//#if MC>=10800
package com.replaymod.compat.shaders;

import net.minecraft.client.MinecraftClient;

//#if MC>=11400
import net.minecraft.client.render.Camera;
//#endif

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
            Class<?> shadersClass;
            try {
                shadersClass = Class.forName("shadersmod.client.Shaders"); // Pre Optifine 1.12.2 E1
            } catch (ClassNotFoundException ignore) {
                shadersClass = Class.forName("net.optifine.shaders.Shaders"); // Post Optifine 1.12.2 E1
            }
            shaders_frameTimeCounter = shadersClass.getDeclaredField("frameTimeCounter");
            shaders_frameTimeCounter.setAccessible(true);

            shaders_isShadowPass = shadersClass.getDeclaredField("isShadowPass");
            shaders_isShadowPass.setAccessible(true);

            shaders_beginRender = shadersClass.getDeclaredMethod("beginRender", MinecraftClient.class,
                    //#if MC>=11400
                    Camera.class,
                    //#endif
                    float.class, long.class);
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
//#endif
