package eu.crushedpixel.replaymod.api.replay.holders;

public class AuthKey {

    private String auth;

    public AuthKey(String authkey) {
        this.auth = authkey;
    }

    public String getAuthkey() {
        return auth;
    }
}
