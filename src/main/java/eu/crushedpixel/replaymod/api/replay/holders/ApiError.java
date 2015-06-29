package eu.crushedpixel.replaymod.api.replay.holders;

import lombok.Data;
import net.minecraft.client.resources.I18n;

@Data
public class ApiError {

    private int id;
    private String desc;
    private String key;
    private String[] objects;

    public String getTranslatedDesc() {
        try {
            return I18n.format(key, (Object[]) objects);
        } catch(Exception e) {
            e.printStackTrace();
            return desc;
        }
    }
}
