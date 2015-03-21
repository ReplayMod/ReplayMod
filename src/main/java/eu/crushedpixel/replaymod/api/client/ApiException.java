package eu.crushedpixel.replaymod.api.client;

import eu.crushedpixel.replaymod.api.client.holders.ApiError;

public class ApiException extends Exception {

	private static final long serialVersionUID = 349073390504232810L;

	private ApiError error;
	
	public ApiException(ApiError error) {
		super(error.getDesc());
		this.error = error;
	}
	
	public ApiError getError() {
		return error;
	}

}
