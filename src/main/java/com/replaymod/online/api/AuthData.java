package com.replaymod.online.api;

/**
 * Represents a set of persistent authentication data.
 */
public interface AuthData {
    /**
     * Returns the user name of the authenticated user.
     * @return user name or {@code null} if not logged in
     */
    String getUserName();

    /**
     * Returns the authentication key of the authenticated user.
     * @return auth key or {@code null} if not logged in
     */
    String getAuthKey();

    /**
     * Store the authentication data after login.
     * @param userName The user name
     * @param authKey The authentication key
     */
    void setData(String userName, String authKey);

    /**
     * Result of authentication operations.
     */
    enum AuthResult {
        /**
         * The operation succeeded without errors.
         */
        SUCCESS,

        /**
         * The data provided got rejected due to it being invalid.
         */
        INVALID_DATA,

        /**
         * Operation could not be performed due to connectivity problems.
         */
        IO_ERROR,
    }
}
