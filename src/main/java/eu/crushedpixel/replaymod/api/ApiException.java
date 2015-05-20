package eu.crushedpixel.replaymod.api;

import eu.crushedpixel.replaymod.api.replay.holders.ApiError;

public class ApiException extends Exception {

    private static final long serialVersionUID = 349073390504232810L;

    private ApiError error;
    private String errorMsg;

    public ApiException(ApiError error) {
        super(error.getTranslatedDesc());
        this.error = error;
        this.errorMsg = error.getTranslatedDesc();
    }

    public ApiException(String error) {
        super(error);
        this.errorMsg = error;
    }

    public ApiError getError() {
        return error;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

}
