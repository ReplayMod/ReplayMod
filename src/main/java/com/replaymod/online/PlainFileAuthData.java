package com.replaymod.online;

import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.AuthData;
import com.replaymod.online.api.replay.holders.AuthConfirmation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Auth data stored in a plain text file.
 */
public class PlainFileAuthData implements AuthData {
    private final Path path;
    private String userName;
    private String authKey;

    public PlainFileAuthData(Path path) {
        this.path = path;
    }

    /**
     * Loads the data from the JSON file and checks for its validity.
     * If the data is invalid, the JSON file is removed.
     * @param apiClient Api client used for validating the auth data
     */
    public void load(ApiClient apiClient) throws IOException {
        String authKey;
        try {
            authKey = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (FileNotFoundException ignored) {
            return;
        }
        AuthConfirmation result = apiClient.checkAuthkey(authKey);
        if (result != null) {
            this.authKey = authKey;
            this.userName = result.getUsername();
        } else {
            Files.deleteIfExists(path);
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

        try {
            Files.write(path, authKey.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            ReplayModOnline.LOGGER.error("Saving auth data:", e);
        }
    }
}
