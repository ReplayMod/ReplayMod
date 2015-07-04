package eu.crushedpixel.replaymod.api.replay.holders;

public enum MinecraftVersion {

    MC_1_8("Minecraft 1.8", "1.8");

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
