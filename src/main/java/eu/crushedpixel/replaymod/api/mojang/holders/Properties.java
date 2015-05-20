package eu.crushedpixel.replaymod.api.mojang.holders;


import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

public class Properties {

    private String name;
    private String value;
    private String signature;

    public String getName() {
        return name;
    }

    public TextureValue getTextureValue() {
        Gson gson = new Gson();
        return gson.fromJson(StringUtils.newStringUtf8(Base64.decodeBase64(value)), TextureValue.class);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
