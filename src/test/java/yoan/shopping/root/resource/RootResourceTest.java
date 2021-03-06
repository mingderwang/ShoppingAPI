package yoan.shopping.root.resource;

import static javax.ws.rs.core.Response.Status.OK;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;

import yoan.shopping.infra.rest.Link;
import yoan.shopping.root.repository.fake.BuildInfoFakeRepository;
import yoan.shopping.root.representation.RootRepresentation;
import yoan.shopping.test.TestHelper;
import yoan.shopping.user.User;

public class RootResourceTest {
	
	@Test
	public void getRootLinks_should_contains_self_link() {
		//given
		String expectedURL = "http://test";
		UriInfo mockedUriInfo = TestHelper.mockUriInfo(expectedURL);
		RootResource testedResource = getRootResource(TestHelper.generateRandomUser());
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
		RootResource testedResource = getRootResource(connectedUser);
		when(testedResource.getUriInfo()).thenReturn(mockedUriInfo);
		
		//when
		Response response = testedResource.root();
		
		//then
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
		RootRepresentation representation = (RootRepresentation) response.getEntity();
		assertThat(representation).isNotNull();
		assertThat(representation.getConnectedUserId()).isEqualTo(connectedUser.getId());
		assertThat(representation.getLinks()).contains(Link.self(expectedURL));
	}
	
	private RootResource getRootResource(User connectedUser) {
		RootResource testedResource = new RootResource(connectedUser, new BuildInfoFakeRepository());
		return spy(testedResource);
	}
}
