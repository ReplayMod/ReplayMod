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
        initFrameTimeCounter();

        initIsShadowPass();

        initBeginRender();

        initChunksToUpdateForced();

        initConfigIsShaders();
    }

    private static void initFrameTimeCounter() {
        try {
            shaders_frameTimeCounter = Class.forName("shadersmod.client.Shaders")
                    .getDeclaredField("frameTimeCounter");
            shaders_frameTimeCounter.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
            // no shaders mod installed
        } catch (NoSuchFieldException e) {
            // the field wasn't found. Has it been renamed?
            e.printStackTrace();
        }
    }

    private static void initIsShadowPass() {
        try {
            shaders_isShadowPass = Class.forName("shadersmod.client.Shaders")
                    .getDeclaredField("isShadowPass");
            shaders_isShadowPass.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
            // no shaders mod installed
        } catch (NoSuchFieldException e) {
            // the field wasn't found. Has it been renamed?
            e.printStackTrace();
        }
    }

    private static void initBeginRender() {
        try {
            shaders_beginRender = Class.forName("shadersmod.client.Shaders")
                    .getDeclaredMethod("beginRender", Minecraft.class, float.class, long.class);
            shaders_frameTimeCounter.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
            // no shaders mod installed
        } catch (NoSuchMethodException e) {
            // the method wasn't found. Has it been renamed?
            e.printStackTrace();
        }
    }

    private static void initChunksToUpdateForced() {
        try {
            renderGlobal_chunksToUpdateForced = Class.forName("net.minecraft.client.renderer.RenderGlobal")
                    .getDeclaredField("chunksToUpdateForced");
            renderGlobal_chunksToUpdateForced.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
            // no shaders mod installed
        } catch (NoSuchFieldException e) {
            // the field wasn't found. Has it been renamed?
            e.printStackTrace();
        }
    }

    private static void initConfigIsShaders() {
        try {
            config_isShaders = Class.forName("Config")
                    .getDeclaredMethod("isShaders");
            config_isShaders.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
            // no shaders mod installed
        } catch (NoSuchMethodException e) {
            // the method wasn't found. Has it been renamed?
            e.printStackTrace();
        }
    }

}
