package com.replaymod.replay;

import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraController;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

//#if MC>=11400
import com.replaymod.core.ReplayMod;
//#endif

//#if MC>=11400
import org.lwjgl.glfw.GLFW;
//#else
//$$ import net.minecraft.client.settings.KeyBinding;
//$$ import net.minecraftforge.client.ForgeHooksClient;
//$$ import org.lwjgl.input.Mouse;
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#if MC>=10800
//$$ import java.io.IOException;
//#else
//$$ import com.replaymod.replay.gui.screen.GuiOpeningReplay;
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
    public
    //#if MC>=11600
    //$$ int
    //#else
    void
    //#endif
    beginRenderTick(
            //#if MC>=11400
            long sysClock
            //#endif
    ) {
        //#if MC>=11600
        //$$ int ticksThisFrame =
        //#endif
        super.beginRenderTick(
                //#if MC>=11400
                sysClock
                //#endif
        );

        // 1.7.10: We have to run the scheduled executables (ours only) because MC would only run them every tick
        //#if MC<=10710
        //$$ FML_BUS.post(new RunScheduledTasks());
        //$$
        //$$ // Code below only updates the current screen when a world and player is loaded. This may not be the case for
        //$$ // the GuiOpeningReplay screen resulting in a livelock.
        //$$ // To counteract that, we always update that screen (doesn't matter if we do it twice).
        //$$ if (mc.currentScreen instanceof GuiOpeningReplay) {
        //$$     mc.currentScreen.handleInput();
        //$$ }
        //#endif

        //#if MC>=11400
        ReplayMod.instance.executor.runTasks();
        //#endif

        // If we are in a replay, we have to manually process key and mouse events as the
        // tick speed may vary or there may not be any ticks at all (when the replay is paused)
        if (mod.getReplayHandler() != null && mc.world != null && mc.player != null) {
            //#if MC>=11400
            if (mc.currentScreen == null || mc.currentScreen.passEvents) {
                GLFW.glfwPollEvents();
                MCVer.processKeyBinds();
            }
            mc.keyboard.pollDebugCrash();
            //#else
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
                //#if MC>=10904
                //$$ ((MCVer.MinecraftMethodAccessor) mc).replayModRunTickMouse();
                //$$ ((MCVer.MinecraftMethodAccessor) mc).replayModRunTickKeyboard();
                //#else
                //$$ // 1.8.9 and below has one giant tick function, so we try to only do keyboard & mouse as far as possible
                //$$ ((MCVer.MinecraftMethodAccessor) mc).replayModSetEarlyReturnFromRunTick(true);
                //#if MC>=10800
                //$$ try {
                //$$     mc.runTick();
                //$$ } catch (IOException e) { // *SIGH*
                //$$     e.printStackTrace();
                //$$ }
                //#else
                //$$ mc.runTick();
                //#endif
                //$$ ((MCVer.MinecraftMethodAccessor) mc).replayModSetEarlyReturnFromRunTick(false);
                //#endif
            //$$ }
            //#endif
        }
        //#if MC>=11600
        //$$ return ticksThisFrame;
        //#endif
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
