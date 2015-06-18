package eu.crushedpixel.replaymod.utils;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static String[] splitStringInMultipleRows(String string, int maxWidth) {
        if(string == null) return new String[0];
        List<String> rows = new ArrayList<String>();
        String remaining = string;
        while(remaining.length() > 0) {
            String[] split = remaining.split(" ");
            String b = "";
            for(String sp : split) {
                b += sp + " ";
                if(mc.fontRendererObj.getStringWidth(b.trim()) > maxWidth) {
                    b = b.substring(0, b.trim().length() - (sp.length()));
                    break;
                }
            }
            String trimmed = b.trim();
            rows.add(trimmed);
            try {
                remaining = remaining.substring(trimmed.length() + 1);
            } catch(Exception e) {
                break;
            }
        }

        return rows.toArray(new String[rows.size()]);
    }
}
