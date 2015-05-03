package eu.crushedpixel.replaymod.api.client.holders;

import net.minecraft.client.resources.I18n;

public class ApiError {

    private int id;
    private String desc;
    private String key;
    private String[] objects;

    public ApiError(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getTranslatedDesc() {
        return I18n.format(key, objects);
    }
}
