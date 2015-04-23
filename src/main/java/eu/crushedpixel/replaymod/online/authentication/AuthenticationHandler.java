package eu.crushedpixel.replaymod.online.authentication;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.client.ApiClient;
import eu.crushedpixel.replaymod.api.client.ApiException;

import java.io.IOException;

public class AuthenticationHandler {

    public static final int SUCCESS = 1;
    public static final int INVALID = 2;
    public static final int NO_CONNECTION = 3;

    private static final ApiClient apiClient = new ApiClient();

    private static String authkey = null;

    public static boolean isAuthenticated() {
        return authkey != null;
    }

    public static String getKey() {
        return authkey;
    }

    public static boolean hasDonated(String uuid) throws IOException, ApiException {
        return apiClient.hasDonated(uuid);
    }

    public static int authenticate(String username, String password) {
        try {
            authkey = ReplayMod.apiClient.getLogin(username, password).getAuthkey();
            return SUCCESS;
        } catch(ApiException e) {
            return INVALID;
        } catch(Exception e) {
            return NO_CONNECTION;
        }
    }

    public static int logout() {
        try {
            ReplayMod.apiClient.logout(authkey);
            authkey = null;
            return SUCCESS;
        } catch(ApiException e) {
            return INVALID;
        } catch(Exception e) {
            return NO_CONNECTION;
        }
    }
}
