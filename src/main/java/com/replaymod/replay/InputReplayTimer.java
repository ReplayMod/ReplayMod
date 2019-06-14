package com.replaymod.replay;

import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraController;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

//#if MC>=11300
import com.replaymod.core.versions.MCVer;
import org.lwjgl.glfw.GLFW;
//#else
//$$ import net.minecraft.client.settings.KeyBinding;
//$$ import net.minecraftforge.client.ForgeHooksClient;
//$$ import org.lwjgl.input.Mouse;
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#if MC>=10800
//$$ import java.io.IOException;
//#else
//$$ import cpw.mods.fml.common.eventhandler.Event;
//$$ import net.minecraft.client.renderer.entity.RenderManager;
//$$
//$$ import static com.replaymod.core.versions.MCVer.FML_BUS;
//#endif
//#endif

//#if MC>=10904
//#else
//$$ import net.minecraft.client.multiplayer.WorldClient;
//#endif

public class InputReplayTimer extends WrappedTimer {
    private final ReplayModReplay mod;
    private final MinecraftClient mc;
    
    public InputReplayTimer(RenderTickCounter wrapped, ReplayModReplay mod) {
        super(wrapped);
        this.mod = mod;
        this.mc = mod.getCore().getMinecraft();
    }

    @Override
    public void beginRenderTick(
            //#if MC>=11300
            long sysClock
            //#endif
    ) {
        super.beginRenderTick(
                //#if MC>=11300
                sysClock
                //#endif
        );

        // 1.7.10: We have to run the scheduled executables (ours only) because MC would only run them every tick
        //#if MC<=10710
        //$$ FML_BUS.post(new RunScheduledTasks());
        //#endif

        // If we are in a replay, we have to manually process key and mouse events as the
        // tick speed may vary or there may not be any ticks at all (when the replay is paused)
        if (mod.getReplayHandler() != null) {
            //#if MC>=11300
            if (mc.currentScreen == null || mc.currentScreen.passEvents) {
                GLFW.glfwPollEvents();
                MCVer.processKeyBinds();
            }
            mc.keyboard.pollDebugCrash();
            //#else
            //#if MC>=10904
            //$$ if (mc.currentScreen != null) {
                //#if MC>=10800
                //$$ try {
                //$$     mc.currentScreen.handleInput();
                //$$ } catch (IOException e) { // *SIGH*
                //$$     e.printStackTrace();
                //$$ }
                //#else
                //$$ mc.currentScreen.handleInput();
                //#endif
            //$$ }
            //$$ if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
            //$$     ((MCVer.MinecraftMethodAccessor) mc).replayModRunTickMouse();
            //$$     ((MCVer.MinecraftMethodAccessor) mc).replayModRunTickKeyboard();
            //$$ }
            //#else
            //$$ // 1.8.9 and below has one giant tick function, so we try to only do keyboard & mouse as far as possible
            //$$ WorldClient world = mc.theWorld;
            //$$ mc.theWorld = null;
            //#if MC>=10800
            //$$ try {
            //$$     mc.runTick();
            //$$ } catch (IOException e) { // *SIGH*
            //$$     e.printStackTrace();
            //$$ }
            //#else
            //$$ mc.runTick();
            //#endif
            //$$ if (mc.theWorld == null) {
            //$$     mc.theWorld = world;
            //$$ }
            //#endif
            //#endif
        }
    }

    public static void handleScroll(int wheel) {
        if (wheel != 0) {
            ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
            if (replayHandler != null) {
                CameraEntity cameraEntity = replayHandler.getCameraEntity();
                if (cameraEntity != null) {
                    CameraController controller = cameraEntity.getCameraController();
                    while (wheel > 0) {
                        controller.increaseSpeed();
                        wheel--;
                    }
                    while (wheel < 0) {
                        controller.decreaseSpeed();
                        wheel++;
                    }
                }
            }
        }
    }

    //#if MC<=10710
    //$$ public static class RunScheduledTasks extends Event {}
    //#endif
}
