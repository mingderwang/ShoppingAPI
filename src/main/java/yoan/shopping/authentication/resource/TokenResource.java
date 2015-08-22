package yoan.shopping.authentication.resource;

import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static yoan.shopping.authentication.resource.OAuthResourceErrorMessage.GRANT_TYPE_NOT_IMPLEMENTED;
import static yoan.shopping.authentication.resource.OAuthResourceErrorMessage.INVALID_AUTHZ_CODE;
import static yoan.shopping.authentication.resource.OAuthResourceErrorMessage.INVALID_CLIENT_SECRET;
import static yoan.shopping.authentication.resource.OAuthResourceErrorMessage.MISSING_CLIENT_SECRET;
import static yoan.shopping.authentication.resource.OAuthResourceErrorMessage.UNKNOWN_CLIENT;
import static yoan.shopping.infra.config.guice.ShoppingWebModule.CONNECTED_USER;
import static yoan.shopping.infra.rest.error.Level.INFO;
import static yoan.shopping.infra.rest.error.Level.WARNING;
import static yoan.shopping.infra.util.error.CommonErrorCode.API_RESPONSE;

import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;

import com.google.inject.name.Named;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import yoan.shopping.authentication.repository.OAuth2AccessTokenRepository;
import yoan.shopping.authentication.repository.OAuth2AuthorizationCodeRepository;
import yoan.shopping.client.app.ClientApp;
import yoan.shopping.client.app.repository.ClientAppRepository;
import yoan.shopping.infra.rest.error.WebApiException;
import yoan.shopping.infra.util.ResourceUtil;
import yoan.shopping.user.User;

@Path("/auth/token")
@Api(value = "/auth/token", description = "OAuth2 Token endpoint")
public class TokenResource {

	/** Currently connected user */
	private final User authenticatedUser;
	private final OAuth2AuthorizationCodeRepository authzCodeRepository;
	private final OAuth2AccessTokenRepository accessTokenRepository;
	private final ClientAppRepository clientAppRepository;

	public static final String INVALID_CLIENT_DESCRIPTION = "Client authentication failed (e.g., unknown client, no client authentication included, or unsupported authentication method).";
	
	@Inject
	public TokenResource(@Named(CONNECTED_USER) User authenticatedUser, OAuth2AuthorizationCodeRepository authzCodeRepository, OAuth2AccessTokenRepository accessTokenRepository, ClientAppRepository clientAppRepository) {
		this.authenticatedUser = requireNonNull(authenticatedUser);
		this.authzCodeRepository = requireNonNull(authzCodeRepository);
		this.accessTokenRepository = requireNonNull(accessTokenRepository);
		this.clientAppRepository = requireNonNull(clientAppRepository);
	}

	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json")
	@ApiOperation(value = "Get Oauth2 access token", notes = "This will can only be done by an authenticated client")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Response with access token in payload"), @ApiResponse(code = 401, message = "Not authenticated") })
	public Response authorize(@Context HttpServletRequest request) throws OAuthSystemException {
		try {
			OAuthTokenRequest oauthRequest = new OAuthTokenRequest(request);
			OAuthResponse response = handleTokenRequest(oauthRequest);
			return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
		} catch(OAuthProblemException problem) {
			return handleOAuthProblem(problem);
		} catch(OAuthException oae) {
			return handleOAuthProblemResponse(oae.getResponse());
		}
	}

	protected OAuthResponse handleTokenRequest(OAuthTokenRequest oauthRequest) throws OAuthSystemException {
		ensureTrustedClient(oauthRequest);

		GrantType grantType = extractGrantType(oauthRequest);
		switch (grantType) {
			case AUTHORIZATION_CODE :
				authorizeWithCode(oauthRequest);
				break;
			case PASSWORD :
				authorizeWithPassword(oauthRequest);
				break;
			case REFRESH_TOKEN :
				//TODO implement OAuth2 refresh token grant
			case CLIENT_CREDENTIALS :
				//TODO implement OAuth2 client credentials grant
			default:
				throw new WebApiException(BAD_REQUEST, WARNING, API_RESPONSE, GRANT_TYPE_NOT_IMPLEMENTED.getDevReadableMessage(grantType.toString()));
		}
		
		String accessToken = generateAccessToken();

		OAuthResponse response = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK).setAccessToken(accessToken).setExpiresIn("3600").buildJSONMessage();
		return response;
	}

	private GrantType extractGrantType(OAuthTokenRequest oauthRequest) {
		String grantTypeParam = oauthRequest.getGrantType();
		return GrantType.valueOf(grantTypeParam.toUpperCase());
	}
	
	private void ensureTrustedClient(OAuthTokenRequest oauthRequest) throws OAuthSystemException {
		UUID clientId = ResourceUtil.getIdfromParam(OAuth.OAUTH_CLIENT_ID, oauthRequest.getParam(OAuth.OAUTH_CLIENT_ID));
		ClientApp clientApp = clientAppRepository.getById(clientId);
		if (!checkClientExists(clientApp)) {
			throw new WebApiException(BAD_REQUEST, WARNING, API_RESPONSE, UNKNOWN_CLIENT.getDevReadableMessage(clientId.toString()));
		}
		
		String clientSecret = oauthRequest.getParam(OAuth.OAUTH_CLIENT_SECRET);
		if (!checkClientSecret(clientApp, clientSecret)) {
			throw new WebApiException(BAD_REQUEST, WARNING, API_RESPONSE, INVALID_CLIENT_SECRET.getDevReadableMessage(clientId.toString()));
		}
	}
	
	private boolean checkClientExists(ClientApp clientApp) {
		return clientApp != null;
	}

	private boolean checkClientSecret(ClientApp clientApp, String secret) {
		if (StringUtils.isBlank(secret)) {
			throw new WebApiException(BAD_REQUEST, INFO, API_RESPONSE, MISSING_CLIENT_SECRET);
		}
		String hashedSecret  = clientAppRepository.hashSecret(secret, clientApp.getSalt());
		return clientApp.getSecret().equals(hashedSecret);
	}

	private void authorizeWithCode(OAuthTokenRequest oauthRequest) throws OAuthSystemException {
		String authzCode = oauthRequest.getCode();
		if (!checkAuthCode(authzCode)) {
			throw new OAuthException(buildBadAuthCodeResponse(authzCode));
		}
		authzCodeRepository.deleteByCode(authzCode);
	}
	
	private boolean checkAuthCode(String authCode) {
		return authzCodeRepository.getUserIdByAuthorizationCode(authCode) != null;
	}

	private void authorizeWithPassword(OAuthTokenRequest oauthRequest) throws OAuthSystemException {
		if (!checkUserPassword(oauthRequest.getUsername(), oauthRequest.getPassword())) {
			throw new OAuthException(buildInvalidUserPassResponse());
		}
	}
	
	private boolean checkUserPassword(String user, String password) {
		//TODO implement check on user and password
		return false;
	}
	
	protected String generateAccessToken() throws OAuthSystemException {
		OAuthIssuer oauthIssuer = new OAuthIssuerImpl(new MD5Generator());
		String accessToken = oauthIssuer.accessToken();
		accessTokenRepository.insertAccessToken(accessToken, authenticatedUser.getId());
		return accessToken;
	}
	
	private Response handleOAuthProblem(OAuthProblemException problem) throws OAuthSystemException {
		OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(problem).buildJSONMessage();
		return handleOAuthProblemResponse(response);
	}

	private Response handleOAuthProblemResponse(OAuthResponse response) throws OAuthSystemException {
		return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
	}
	
	private OAuthResponse buildBadAuthCodeResponse(String authzCode) throws OAuthSystemException {
		return OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
							  .setError(OAuthError.TokenResponse.INVALID_GRANT).setErrorDescription(INVALID_AUTHZ_CODE.getDevReadableMessage(authzCode))
							  .buildJSONMessage();
	}

	private OAuthResponse buildInvalidUserPassResponse() throws OAuthSystemException {
		return OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
							  .setError(OAuthError.TokenResponse.INVALID_GRANT).setErrorDescription("invalid username or password")
							  .buildJSONMessage();
	}
}