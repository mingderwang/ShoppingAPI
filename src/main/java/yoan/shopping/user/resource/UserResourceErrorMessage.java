package yoan.shopping.user.resource;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import yoan.shopping.infra.util.error.ErrorMessage;

/**
 * Error messages specific to User API
 * @author yoan
 */
public enum UserResourceErrorMessage  implements ErrorMessage {
	/** User not found */
	USER_NOT_FOUND("User not found"),
	/** User with email : %s aready exists */
	ALREADY_EXISTING_USER_WITH_EMAIL("User with email : %s already exists");

	private String message;
	
	private UserResourceErrorMessage(String message) {
		checkArgument(isNotBlank(message), "An error message should not be empty");
		this.message = message;
	}
	
	@Override
	public String getDevReadableMessage() {
		return message;
	}

	@Override
	public String getDevReadableMessage(Object... params) {
		return String.format(message, params);
	}

}