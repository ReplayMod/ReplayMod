package com.replaymod.online.api.replay.holders;

import lombok.Data;
import net.minecraft.client.resource.language.I18n;

@Data
public class ApiError {

    private int id;
    private String desc;
    private String key;
    private String[] objects;

    public String getTranslatedDesc() {
        try {
            return I18n.translate(key, (Object[]) objects);
        } catch(Exception e) {
            e.printStackTrace();
            return desc;
        }
    }
}
