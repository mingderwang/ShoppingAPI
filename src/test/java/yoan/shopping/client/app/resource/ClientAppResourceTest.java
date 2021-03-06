package yoan.shopping.client.app.resource;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static yoan.shopping.client.app.resource.ClientAppResourceErrorMessage.CLIENT_APPS_NOT_FOUND;
import static yoan.shopping.infra.rest.error.Level.INFO;
import static yoan.shopping.infra.util.error.CommonErrorCode.API_RESPONSE;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

import yoan.shopping.client.app.ClientApp;
import yoan.shopping.client.app.repository.ClientAppRepository;
import yoan.shopping.client.app.representation.ClientAppRepresentation;
import yoan.shopping.client.app.representation.ClientAppWriteRepresentation;
import yoan.shopping.infra.rest.Link;
import yoan.shopping.infra.rest.RestRepresentation;
import yoan.shopping.infra.rest.error.WebApiException;
import yoan.shopping.infra.util.error.ApplicationException;
import yoan.shopping.infra.util.error.RepositoryErrorCode;
import yoan.shopping.test.TestHelper;
import yoan.shopping.user.User;

@RunWith(MockitoJUnitRunner.class)
public class ClientAppResourceTest {
	
	@Mock
	ClientAppRepository mockedClientAppRepo;

	private ClientAppResource getClientAppResource(User connectedUser) {
		ClientAppResource testedResource = new ClientAppResource(connectedUser, mockedClientAppRepo);
		return spy(testedResource);
	}
	
	@Test
	public void getRootLinks_should_contains_self_link() {
		//given
		String expectedURL = "http://test";
		UriInfo mockedUriInfo = TestHelper.mockUriInfo(expectedURL);
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		when(testedResource.getUriInfo()).thenReturn(mockedUriInfo);
		
		//when
		List<Link> links = testedResource.getRootLinks();
		
		//then
		assertThat(links).isNotNull();
		assertThat(links).isNotEmpty();
		assertThat(links).contains(Link.self(expectedURL));
	}
	
	@Test
	public void root_should_work() {
		//given
		String expectedURL = "http://test";
		UriInfo mockedUriInfo = TestHelper.mockUriInfo(expectedURL);
		User connectedUser = TestHelper.generateRandomUser();
		ClientAppResource testedResource = getClientAppResource(connectedUser);
		when(testedResource.getUriInfo()).thenReturn(mockedUriInfo);
		
		//when
		Response response = testedResource.root();
		
		//then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
		RestRepresentation representation = (RestRepresentation) response.getEntity();
		assertThat(representation).isNotNull();
		assertThat(representation.getLinks()).contains(Link.self(expectedURL));
	}
	
	@Test
	public void create_should_work_with_valid_input_representation() {
		//given
		String expectedName = "name";
		URI expectedRedirectURI = TestHelper.TEST_URI;
		UUID expectedOwnerId = UUID.randomUUID();
		@SuppressWarnings("deprecation")
		ClientAppWriteRepresentation representation = new ClientAppWriteRepresentation(expectedName, expectedOwnerId, expectedRedirectURI.toString());
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		UriInfo mockedUriInfo = TestHelper.mockUriInfo("http://test");
		when(testedResource.getUriInfo()).thenReturn(mockedUriInfo);
		
		//when
		Response response = testedResource.create(representation);
		
		//then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(CREATED.getStatusCode());
		ClientAppRepresentation clientAppRepresentation = (ClientAppRepresentation) response.getEntity();
		assertThat(clientAppRepresentation).isNotNull();
		assertThat(clientAppRepresentation.getId()).isNotEqualTo(ClientApp.DEFAULT_ID);
		assertThat(clientAppRepresentation.getName()).isEqualTo(expectedName);
		assertThat(clientAppRepresentation.getOwnerId()).isEqualTo(expectedOwnerId);
		assertThat(clientAppRepresentation.getRedirectURI()).isEqualTo(expectedRedirectURI.toString());
	}
	
	@Test(expected = WebApiException.class)
	public void getById_should_return_400_with_invalid_Id() {
		//given
		String invalidId = "invalid ID";
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = "Invalid Param named appId : invalid ID";
		
		//when
		try {
			testedResource.getById(invalidId);
		} catch(WebApiException wae) {
		//then
			TestHelper.assertWebApiException(wae, BAD_REQUEST, INFO, API_RESPONSE, expectedMessage);
			throw wae;
		}
	}
	
	@Test(expected = WebApiException.class)
	public void getById_should_return_404_with_unknown_user_Id() {
		//given
		String unknownId = UUID.randomUUID().toString();
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = "Client application not found";
		
		//when
		try {
			testedResource.getById(unknownId);
		} catch(WebApiException wae) {
		//then
			TestHelper.assertWebApiException(wae, NOT_FOUND, INFO, API_RESPONSE, expectedMessage);
			throw wae;
		}
	}
	
	@Test
	public void getById_should_work_with_existing_user_Id() {
		//given
		UUID existingId = UUID.randomUUID();
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		UriInfo mockedUriInfo = TestHelper.mockUriInfo("http://test");
		when(testedResource.getUriInfo()).thenReturn(mockedUriInfo);
		ClientApp existingClientApp = ClientApp.Builder.createDefault().withId(existingId).build();
		when(mockedClientAppRepo.getById(existingId)).thenReturn(existingClientApp);
		
		//when
		Response response = testedResource.getById(existingId.toString());
		
		//then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
		ClientAppRepresentation clientAppRepresentationRepresentation = (ClientAppRepresentation) response.getEntity();
		assertThat(clientAppRepresentationRepresentation).isNotNull();
		assertThat(clientAppRepresentationRepresentation.getId()).isNotEqualTo(User.DEFAULT_ID);
		assertThat(clientAppRepresentationRepresentation.getName()).isEqualTo(existingClientApp.getName());
		assertThat(clientAppRepresentationRepresentation.getOwnerId()).isEqualTo(existingClientApp.getOwnerId());
		assertThat(clientAppRepresentationRepresentation.getRedirectURI()).isEqualTo(existingClientApp.getRedirectURI().toString());
	}
	
	@Test(expected = WebApiException.class)
	public void getByOwnerId_should_return_404_with_unknown_owner_Id() {
		//given
		UUID unknownId = UUID.randomUUID();
		when(mockedClientAppRepo.getByOwner(unknownId)).thenReturn(ImmutableList.of());
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = CLIENT_APPS_NOT_FOUND.getDevReadableMessage(unknownId.toString());
		
		//when
		try {
			testedResource.getByOwnerId(unknownId.toString());
		} catch(WebApiException wae) {
		//then
			TestHelper.assertWebApiException(wae, NOT_FOUND, INFO, API_RESPONSE, expectedMessage);
			throw wae;
		}
	}
	
	@Test
	public void getByOwnerId_should_work_with_existing_list_Id() {
		//given
		UUID existingOwnerId = UUID.randomUUID();
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		UriInfo mockedUriInfo = TestHelper.mockUriInfo("http://test");
		when(testedResource.getUriInfo()).thenReturn(mockedUriInfo);
		ClientApp existingClientApp = ClientApp.Builder.createDefault().withRandomId().withOwnerId(existingOwnerId).build();
		ClientApp existingClientApp2 = ClientApp.Builder.createDefault().withRandomId().withOwnerId(existingOwnerId).build();
		when(mockedClientAppRepo.getByOwner(existingOwnerId)).thenReturn(ImmutableList.of(existingClientApp, existingClientApp2));
		
		//when
		Response response = testedResource.getByOwnerId(existingOwnerId.toString());
		
		//then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
		List<?> appsRepresentation = (List<?>) response.getEntity();
		assertThat(appsRepresentation).isNotNull();
		assertThat(appsRepresentation).hasSize(2);
		
		ClientAppRepresentation representation = (ClientAppRepresentation) appsRepresentation.get(0);
		assertThat(representation.getId()).isNotEqualTo(ClientApp.DEFAULT_ID);
		assertThat(representation.getName()).isEqualTo(existingClientApp.getName());
		assertThat(representation.getOwnerId()).isEqualTo(existingClientApp.getOwnerId());
		assertThat(representation.getRedirectURI()).isEqualTo(existingClientApp.getRedirectURI().toString());
		
		ClientAppRepresentation representation2 = (ClientAppRepresentation) appsRepresentation.get(1);
		assertThat(representation2.getId()).isNotEqualTo(ClientApp.DEFAULT_ID);
		assertThat(representation2.getName()).isEqualTo(existingClientApp2.getName());
		assertThat(representation2.getOwnerId()).isEqualTo(existingClientApp2.getOwnerId());
		assertThat(representation.getRedirectURI()).isEqualTo(existingClientApp2.getRedirectURI().toString());
	}
	
	@Test
	public void update_should_work_with_existing_client_app() {
		//given
		UUID expectedId = UUID.randomUUID();
		String expectedName = "name";
		URI expectedRedirectURI = TestHelper.TEST_URI;
		@SuppressWarnings("deprecation")
		ClientAppWriteRepresentation representation = new ClientAppWriteRepresentation(expectedName, UUID.randomUUID(), expectedRedirectURI.toString());
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		ClientApp existingApp = ClientApp.Builder.createDefault().withId(expectedId).build();
		when(mockedClientAppRepo.getById(expectedId)).thenReturn(existingApp);
		UriInfo mockedUriInfo = TestHelper.mockUriInfo("http://test");
		when(testedResource.getUriInfo()).thenReturn(mockedUriInfo);
		
		//when
		Response response = testedResource.update(expectedId.toString(), representation);
		
		//then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());
	}
	
	@Test(expected = WebApiException.class)
	public void update_should_return_404_with_invalid_client_app_id() {
		//given
		String invalidAppId = "invalid";
		@SuppressWarnings("deprecation")
		ClientAppWriteRepresentation representation = new ClientAppWriteRepresentation("name", UUID.randomUUID(), TestHelper.TEST_URI.toString());
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = "Invalid Param named appId : invalid";
		
		//when
		try {
			testedResource.update(invalidAppId, representation);
		} catch(WebApiException wae) {
		//then
			TestHelper.assertWebApiException(wae, BAD_REQUEST, INFO, API_RESPONSE, expectedMessage);
			throw wae;
		}
	}
	
	@Test(expected = ApplicationException.class)
	public void update_should_return_404_with_unknown_client_app() {
		//given
		String unknownAppId = UUID.randomUUID().toString();
		@SuppressWarnings("deprecation")
		ClientAppWriteRepresentation representation = new ClientAppWriteRepresentation("name", UUID.randomUUID(), TestHelper.TEST_URI.toString());
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = "Client app not found";
		
		//when
		try {
			testedResource.update(unknownAppId, representation);
		} catch(ApplicationException ae) {
		//then
			TestHelper.assertApplicationException(ae, INFO, RepositoryErrorCode.NOT_FOUND, expectedMessage);
			throw ae;
		}
	}
	
	@Test(expected = WebApiException.class)
	public void changeSecretKey_should_return_400_with_invalid_Id() {
		//given
		String invalidId = "invalid ID";
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = "Invalid Param named appId : invalid ID";
		
		//when
		try {
			testedResource.changeSecretKey(invalidId);
		} catch(WebApiException wae) {
		//then
			TestHelper.assertWebApiException(wae, BAD_REQUEST, INFO, API_RESPONSE, expectedMessage);
			throw wae;
		}
	}
	
	@Test(expected = ApplicationException.class)
	public void changeSecretKey_should_return_404_with_unknown_client_app() {
		//given
		UUID unknownId = UUID.randomUUID();
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = "Client app not found";
		
		//when
		try {
			testedResource.changeSecretKey(unknownId.toString());
		} catch(ApplicationException ae) {
		//then
			TestHelper.assertApplicationException(ae, INFO, RepositoryErrorCode.NOT_FOUND, expectedMessage);
			throw ae;
		}
	}
	
	@Test(expected = WebApiException.class)
	public void deleteById_should_return_400_with_invalid_Id() {
		//given
		String invalidId = "invalid ID";
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = "Invalid Param named appId : invalid ID";
		
		//when
		try {
			testedResource.deleteById(invalidId);
		} catch(WebApiException wae) {
		//then
			TestHelper.assertWebApiException(wae, BAD_REQUEST, INFO, API_RESPONSE, expectedMessage);
			throw wae;
		}
	}
	
	@Test(expected = WebApiException.class)
	public void deleteById_should_return_404_with_unknown_client_app_Id() {
		//given
		String unknownId = UUID.randomUUID().toString();
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		String expectedMessage = "Client application not found";
		
		//when
		try {
			testedResource.deleteById(unknownId);
		} catch(WebApiException wae) {
		//then
			TestHelper.assertWebApiException(wae, NOT_FOUND, INFO, API_RESPONSE, expectedMessage);
			throw wae;
		}
	}
	
	@Test
	public void deleteById_should_work_with_existing_user_Id() {
		//given
		UUID existingId = UUID.randomUUID();
		ClientAppResource testedResource = getClientAppResource(TestHelper.generateRandomUser());
		ClientApp existingApp = ClientApp.Builder.createDefault().withId(existingId).build();
		when(mockedClientAppRepo.getById(existingId)).thenReturn(existingApp);
		
		//when
		Response response = testedResource.deleteById(existingId.toString());
		
		//then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
	}
}
