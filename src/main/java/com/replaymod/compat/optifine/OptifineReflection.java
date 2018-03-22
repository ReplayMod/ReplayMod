package com.replaymod.compat.optifine;

import net.minecraft.client.settings.GameSettings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class OptifineReflection {

    // GameSettings.ofFastRender
    public static Field gameSettings_ofFastRender;

    static {
        try {
            // this throws an ignored ClassNotFoundException if Optifine isn't installed
            Class.forName("Config");

            gameSettings_ofFastRender = GameSettings.class.getDeclaredField("ofFastRender");
            gameSettings_ofFastRender.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
            // no optifine installed
        } catch (NoSuchFieldException e) {
            // the field wasn't found. Has it been renamed?
            e.printStackTrace();
        }
    }

    public static void reloadLang() {
        try {
            Class.forName("Lang").getDeclaredMethod("resourcesReloaded").invoke(null);
        } catch (ClassNotFoundException ignore) {
            // no optifine installed
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
