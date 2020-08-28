package com.replaymod.recording;

import net.minecraft.client.network.ServerInfo;

/** Extension interface for {@link net.minecraft.client.network.ServerInfo}. */
public interface ServerInfoExt {

    static ServerInfoExt from(ServerInfo base) {
        return (ServerInfoExt) base;
    }

    /** Per-server optional overwrite for {@link Setting#AUTO_START_RECORDING}. */
    Boolean getAutoRecording();

    /** Per-server optional overwrite for {@link Setting#AUTO_START_RECORDING}. */
    void setAutoRecording(Boolean autoRecording);
}
