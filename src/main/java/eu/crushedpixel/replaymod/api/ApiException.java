package eu.crushedpixel.replaymod.api;

import eu.crushedpixel.replaymod.api.replay.holders.ApiError;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApiException extends Exception {

    private static final long serialVersionUID = 349073390504232810L;

    private ApiError error;

    public ApiException(ApiError error) {
        super(error.getTranslatedDesc());
        this.error = error;
    }

    public ApiException(String error) {
        super(error);
    }

    public ApiError getError() {
        return error;
    }

}
