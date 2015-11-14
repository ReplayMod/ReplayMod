package com.replaymod.online;

import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.AuthData;
import com.replaymod.online.api.replay.holders.AuthConfirmation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Auth data stored in a {@link Configuration}.
 */
public class ConfigurationAuthData implements AuthData {

    private final Configuration config;
    private String userName;
    private String authKey;

    public ConfigurationAuthData(Configuration config) {
        this.config = config;
    }

    /**
     * Loads the data from the configuration and checks for its validity.
     * If the data is invalid, it is removed from the config.
     * @param apiClient Api client used for validating the auth data
     */
    public void load(ApiClient apiClient) {
        Property property = config.get("authkey", "authkey", (String) null);
        if (property != null) {
            String authKey = property.getString();
            AuthConfirmation result = apiClient.checkAuthkey(authKey);
            if (result != null) {
                this.authKey = authKey;
                this.userName = result.getUsername();
            } else {
                setData(null, null);
            }
        }
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getAuthKey() {
        return authKey;
    }

    @Override
    public void setData(String userName, String authKey) {
        this.userName = userName;
        this.authKey = authKey;

        config.removeCategory(config.getCategory("authkey"));
        if (authKey != null) {
            // Note: .get() actually creates the entry with the default value if it doesn't exist
            config.get("authkey", "authkey", authKey);
        }
        config.save();
    }
}
