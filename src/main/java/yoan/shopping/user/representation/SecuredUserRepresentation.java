package yoan.shopping.user.representation;

import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlElement;

import io.swagger.annotations.ApiModel;
import yoan.shopping.infra.rest.Link;

/**
 * User with security infos Rest Representation
 * @author yoan
 */
@ApiModel(value = "Secured user")
public class SecuredUserRepresentation extends UserRepresentation {
	/** User password */
	private String password;
	
	public SecuredUserRepresentation() {
		super();
	}
	
	/** Test Purpose only */
	@Deprecated 
	public SecuredUserRepresentation(UUID id, String name, String email, List<Link> links, String password) {
		super(id, name, email, links);
		this.password = password;
	}
	
	@XmlElement(name = "password")
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
}
