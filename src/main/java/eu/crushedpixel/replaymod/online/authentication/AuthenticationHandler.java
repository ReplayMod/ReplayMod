package eu.crushedpixel.replaymod.online.authentication;

import com.mojang.authlib.exceptions.AuthenticationException;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.ApiClient;
import eu.crushedpixel.replaymod.api.ApiException;
import eu.crushedpixel.replaymod.api.replay.holders.AuthConfirmation;
import eu.crushedpixel.replaymod.api.replay.holders.AuthKey;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Property;

import java.io.IOException;

public class AuthenticationHandler {

    public static final int SUCCESS = 1;
    public static final int INVALID = 2;
    public static final int NO_CONNECTION = 3;

    private static final ApiClient apiClient = new ApiClient();

    private static String authkey = null;
    private static String username = null;

    public static boolean isAuthenticated() {
        return authkey != null;
    }

    public static String getKey() {
        return authkey;
    }
    public static String getUsername() { return username; }

    @SuppressWarnings("unused")
    public static boolean hasDonated(String uuid) throws IOException, ApiException {
        return apiClient.hasDonated(uuid);
    }

    private static Minecraft mc = Minecraft.getMinecraft();

    public static void register(String usrname, String mail, String password)
            throws IOException, ApiException, AuthenticationException {
        AuthKey auth = apiClient.register(usrname, mail, password,
                mc.getSession().getProfile().getId().toString());
        username = usrname;
        authkey = auth.getAuth();
        saveAuthkey(authkey);
    }

    public static void loadAuthkeyFromConfig() {
        Property p = ReplayMod.config.get("authkey", "authkey", "null");

        String key = null;
        if(!(p.getString().equals("null"))) {
            key = p.getString();
        }

        if(key != null) {
            AuthConfirmation conf = apiClient.checkAuthkey(key);
            if(conf != null) {
                authkey = key;
                username = conf.getUsername();
            } else {
                saveAuthkey("null");
                username = null;
            }
        }
    }

    public static int authenticate(String usrname, String password) {
        try {
            authkey = ReplayMod.apiClient.getLogin(usrname, password).getAuth();
            username = usrname;
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
            username = null;
            saveAuthkey("null");
            return SUCCESS;
        } catch(ApiException e) {
            return INVALID;
        } catch(Exception e) {
            return NO_CONNECTION;
        }
    }

    private static void saveAuthkey(String authkey) {
        ReplayMod.config.removeCategory(ReplayMod.config.getCategory("authkey"));
        ReplayMod.config.get("authkey", "authkey", authkey);
        ReplayMod.config.save();
    }
}
