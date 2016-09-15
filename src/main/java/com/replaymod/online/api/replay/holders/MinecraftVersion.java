package com.replaymod.online.api.replay.holders;

public enum MinecraftVersion {

    MC_1_8("Minecraft 1.8", "1.8"),
    MC_1_9_4("Minecraft 1.9.4", "1.9.4");

    private String niceName, apiName;

    MinecraftVersion(String niceName, String apiName) {
        this.niceName = niceName;
        this.apiName = apiName;
    }

    public String toNiceName() {
        return niceName;
    }

    public String getApiName() {
        return apiName;
    }
}
