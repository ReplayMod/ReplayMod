package com.replaymod.compat.shaders;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ShaderReflection {

    // Shaders.frameTimeCounter
    public static Field shaders_frameTimeCounter;

    // Shaders.isShadowPass
    public static Field shaders_isShadowPass;

    // Shaders.beginRender()
    public static Method shaders_beginRender;

    // RenderGlobal.chunksToUpdateForced (Optifine only)
    public static Field renderGlobal_chunksToUpdateForced;

    // Config.isShaders() (Optifine only)
    public static Method config_isShaders;

    static {
        try {
            shaders_frameTimeCounter = Class.forName("shadersmod.client.Shaders")
                    .getDeclaredField("frameTimeCounter");
            shaders_frameTimeCounter.setAccessible(true);

            shaders_isShadowPass = Class.forName("shadersmod.client.Shaders")
                    .getDeclaredField("isShadowPass");
            shaders_isShadowPass.setAccessible(true);

            shaders_beginRender = Class.forName("shadersmod.client.Shaders")
                    .getDeclaredMethod("beginRender", Minecraft.class, float.class, long.class);
            shaders_beginRender.setAccessible(true);

            renderGlobal_chunksToUpdateForced = Class.forName("net.minecraft.client.renderer.RenderGlobal")
                    .getDeclaredField("chunksToUpdateForced");
            renderGlobal_chunksToUpdateForced.setAccessible(true);

            config_isShaders = Class.forName("Config")
                    .getDeclaredMethod("isShaders");
            config_isShaders.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
            // no shaders mod installed
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            // the method wasn't found. Has it been renamed?
            e.printStackTrace();
        }
    }
}
