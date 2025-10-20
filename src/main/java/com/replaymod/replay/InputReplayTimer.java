package com.replaymod.replay;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraController;
import com.replaymod.replay.camera.CameraEntity;
import de.johni0702.minecraft.gui.versions.ScreenExt;
import net.minecraft.client.MinecraftClient;

//#if MC>=12109
//$$ import net.minecraft.client.gui.screen.Overlay;
//#endif

//#if MC>=12109
//$$ import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
//#else
//#if MC>=11802
//$$ import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
//#endif
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
//$$ import net.minecraft.client.renderer.entity.RenderManager;
//#endif
//#endif

//#if MC>=10904
//#else
//$$ import net.minecraft.client.multiplayer.WorldClient;
//#endif

public class InputReplayTimer {
    public static void updateInReplay() {
        ReplayModReplay mod = ReplayModReplay.instance;
        MinecraftClient mc = mod.getCore().getMinecraft();

        ReplayMod.instance.runTasks();

        //#if MC<=10710
        //$$ // Code below only updates the current screen when a world and player is loaded. This may not be the case for
        //$$ // the GuiOpeningReplay screen resulting in a livelock.
        //$$ // To counteract that, we always update that screen (doesn't matter if we do it twice).
        //$$ if (mc.currentScreen instanceof GuiOpeningReplay) {
        //$$     mc.currentScreen.handleInput();
        //$$ }
        //#endif

        // If we are in a replay, we have to manually process key and mouse events as the
        // tick speed may vary or there may not be any ticks at all (when the replay is paused)
        if (mod.getReplayHandler() != null && mc.world != null && mc.player != null) {
            //#if MC>=11400
            if (mc.currentScreen == null || ((ScreenExt) mc.currentScreen).doesPassEvents()) {
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

            //#if MC>=11802
            //$$ // As of 1.18.2, this screen always stays open for at least two seconds, and requires ticking to close.
            //$$ // Thanks, but we'll have none of that (at least while in a replay).
            //#if MC>=12109
            //$$ if (mc.currentScreen instanceof LevelLoadingScreen) {
            //#else
            //$$ if (mc.currentScreen instanceof DownloadingTerrainScreen) {
            //#endif
            //$$     mc.currentScreen.close();
            //$$ }
            //#endif

            //#if MC>=12109
            //$$ // The SplashOverlay now only closes on `tick`, but there are no ticks while the replay is paused.
            //$$ // so we need to manually tick it to not get stuck.
            //$$ Overlay overlay = mc.getOverlay();
            //$$ if (overlay != null) {
            //$$     overlay.tick();
            //$$ }
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
}
