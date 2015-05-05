package eu.crushedpixel.replaymod.online.authentication;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.client.ApiClient;
import eu.crushedpixel.replaymod.api.client.ApiException;
import eu.crushedpixel.replaymod.api.client.holders.AuthKey;
import net.minecraftforge.common.config.Property;

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

    public static void register(String username, String mail, String password) throws IOException, ApiException {
        AuthKey auth = apiClient.register(username, mail, password);
        authkey = auth.getAuthkey();
        saveAuthkey(authkey);
    }

    public static boolean loadAuthkeyFromConfig() {
        Property p = ReplayMod.instance.config.get("authkey", "authkey", "null");

        String key = null;
        if(!(p.getString().equals("null"))) {
            key = p.getString();
        }

        if(key != null) {
            boolean succ = apiClient.checkAuthkey(key);
            if(succ) {
                authkey = key;
            } else {
                saveAuthkey("null");
            }
            return succ;
        }
        return false;
    }

    public static int authenticate(String username, String password) {
        try {
            authkey = ReplayMod.apiClient.getLogin(username, password).getAuthkey();
            saveAuthkey(authkey);
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
            saveAuthkey("null");
            return SUCCESS;
        } catch(ApiException e) {
            return INVALID;
        } catch(Exception e) {
            return NO_CONNECTION;
        }
    }

    private static void saveAuthkey(String authkey) {
        ReplayMod.instance.config.removeCategory(ReplayMod.config.getCategory("authkey"));
        ReplayMod.instance.config.get("authkey", "authkey", authkey);
        ReplayMod.instance.config.save();
    }
}
