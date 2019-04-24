package com.replaymod.replay;

import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.replay.camera.CameraController;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

//#if MC>=11300
import org.lwjgl.glfw.GLFW;
//#else
//$$ import com.replaymod.replay.events.ReplayDispatchKeypressesEvent;
//$$ import net.minecraft.client.gui.GuiScreen;
//$$ import net.minecraft.client.settings.GameSettings;
//$$ import net.minecraft.client.settings.KeyBinding;
//$$ import net.minecraft.crash.CrashReport;
//$$ import net.minecraft.util.ReportedException;
//$$ import net.minecraftforge.client.ForgeHooksClient;
//$$ import net.minecraftforge.common.MinecraftForge;
//$$ import org.lwjgl.input.Keyboard;
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

public class InputReplayTimer extends WrappedTimer {
    private final ReplayModReplay mod;
    private final Minecraft mc;
    
    public InputReplayTimer(Timer wrapped, ReplayModReplay mod) {
        super(wrapped);
        this.mod = mod;
        this.mc = mod.getCore().getMinecraft();
    }

    @Override
    //#if MC>=11300
    public void updateTimer(long sysClock) {
        super.updateTimer(sysClock);
    //#else
    //$$ public void updateTimer() {
    //$$     super.updateTimer();
    //#endif

        // 1.7.10: We have to run the scheduled executables (ours only) because MC would only run them every tick
        //#if MC<=10710
        //$$ FML_BUS.post(new RunScheduledTasks());
        //#endif

        // If we are in a replay, we have to manually process key and mouse events as the
        // tick speed may vary or there may not be any ticks at all (when the replay is paused)
        if (mod.getReplayHandler() != null) {
            //#if MC>=11300
            if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
                GLFW.glfwPollEvents();
                mc.processKeyBinds();
            }
            mc.keyboardListener.tick();
            //#else
            //$$ if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
            //$$     while (Mouse.next()) {
            //$$         handleMouseEvent();
            //$$     }
            //$$
            //$$     while (Keyboard.next()) {
            //$$         handleKeyEvent();
            //$$     }
            //$$ } else {
                //#if MC<11300
                //#if MC>=10800
                //$$ try {
                //$$     mc.currentScreen.handleInput();
                //$$ } catch (IOException e) { // *SIGH*
                //$$     e.printStackTrace();
                //$$ }
                //#else
                //$$ mc.currentScreen.handleInput();
                //#endif
                //#endif
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

    //#if MC<11300
    //$$ protected void handleMouseEvent() {
    //$$     if (ForgeHooksClient.postMouseEvent()) return;
    //$$
    //$$     int button = Mouse.getEventButton() - 100;
    //$$     boolean pressed = Mouse.getEventButtonState();
    //$$
    //$$     // Update key binding states
    //$$     KeyBinding.setKeyBindState(button, pressed);
    //$$     if (pressed) {
    //$$         KeyBinding.onTick(button);
    //$$     }
    //$$
    //$$     int wheel = Mouse.getEventDWheel();
    //$$     handleScroll(wheel);
    //$$
    //$$     if (mc.currentScreen == null) {
    //$$         if (!mc.inGameHasFocus && Mouse.getEventButtonState()) {
    //$$             // Regrab mouse if the user clicks into the window
    //$$             mc.setIngameFocus();
    //$$         }
    //$$     } else {
            //#if MC>=10800
            //$$ try {
            //$$     mc.currentScreen.handleMouseInput();
            //$$ } catch (IOException e) { // WHO IS RESPONSIBLE FOR THIS MESS?!?
            //$$     e.printStackTrace();
            //$$ }
            //#else
            //$$ mc.currentScreen.handleMouseInput();
            //#endif
    //$$     }
    //$$
    //$$     FMLCommonHandler.instance().fireMouseInput();
    //$$ }
    //$$
    //$$ protected void handleKeyEvent() {
    //$$     // TODO 1.7.10: This might be missing some 1.7.10-only key bindings or implement some of them incorrectly
    //$$     int key = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
    //$$     boolean pressed = Keyboard.getEventKeyState();
    //$$
    //$$     KeyBinding.setKeyBindState(key, pressed);
    //$$     if (pressed) {
    //$$         KeyBinding.onTick(key);
    //$$     }
    //$$
    //$$     // Still want to be able to create debug crashes ]:D
    //$$     if (mc.debugCrashKeyPressTime > 0) {
    //$$         if (Minecraft.getSystemTime() - mc.debugCrashKeyPressTime >= 6000L) {
    //$$             throw new ReportedException(new CrashReport("Manually triggered debug crash", new Throwable()));
    //$$         }
    //$$
    //$$         if (!Keyboard.isKeyDown(Keyboard.KEY_F3) || !Keyboard.isKeyDown(Keyboard.KEY_C)) {
    //$$             mc.debugCrashKeyPressTime = -1;
    //$$         }
    //$$     } else if (Keyboard.isKeyDown(Keyboard.KEY_F3) && Keyboard.isKeyDown(Keyboard.KEY_C)) {
    //$$         mc.debugCrashKeyPressTime = Minecraft.getSystemTime();
    //$$     }
    //$$
    //$$     // Twitch, screenshot, fullscreen, etc. (stuff that works everywhere)
    //$$     if (!MinecraftForge.EVENT_BUS.post(new ReplayDispatchKeypressesEvent.Pre())) {
    //$$         mc.dispatchKeypresses();
    //$$     }
    //$$
    //$$     if (pressed) {
    //$$         // This might be subject to change as vanilla shaders are still kinda unused in 1.8
    //$$         if (key == Keyboard.KEY_F4 && mc.entityRenderer != null) {
                //#if MC>=10800
                //$$ mc.entityRenderer.switchUseShader();
                //#else
                //$$ mc.entityRenderer.activateNextShader();
                //#endif
    //$$         }
    //$$
    //$$         if (mc.currentScreen != null) {
                //#if MC>=10800
                //$$ try {
                //$$     mc.currentScreen.handleKeyboardInput();
                //$$ } catch (IOException e) { // AND WHO THOUGHT THIS WAS A GREAT IDEA?
                //$$     e.printStackTrace();
                //$$ }
                //#else
                //$$ mc.currentScreen.handleKeyboardInput();
                //#endif
    //$$         } else {
    //$$             if (key == Keyboard.KEY_ESCAPE) {
    //$$                 mc.displayInGameMenu();
    //$$             }
    //$$
    //$$             // Following are a ton of vanilla keyboard shortcuts, some are removed as they're useless in the
    //$$             // replay viewer as of now
    //$$             // TODO Update maybe add new key bindings
    //$$             // TODO: Translate magic values to Keyboard.KEY_ constants
    //$$
    //$$             if (key == 32 && Keyboard.isKeyDown(61) && mc.ingameGUI != null) {
                    //#if MC>=11100
                    //$$ mc.ingameGUI.getChatGUI().clearChatMessages(false);
                    //#else
                    //$$ mc.ingameGUI.getChatGUI().clearChatMessages();
                    //#endif
    //$$             }
    //$$
    //$$             if (key == 31 && Keyboard.isKeyDown(61)) {
    //$$                 mc.refreshResources();
    //$$             }
    //$$
    //$$             if (key == 20 && Keyboard.isKeyDown(61)) {
    //$$                 mc.refreshResources();
    //$$             }
    //$$
    //$$             if (key == 33 && Keyboard.isKeyDown(61)) {
    //$$                 boolean flag1 = Keyboard.isKeyDown(42) | Keyboard.isKeyDown(54);
    //$$                 mc.gameSettings.setOptionValue(GameSettings.Options.RENDER_DISTANCE, flag1 ? -1 : 1);
    //$$             }
    //$$
    //$$             if (key == 30 && Keyboard.isKeyDown(61)) {
    //$$                 mc.renderGlobal.loadRenderers();
    //$$             }
    //$$
    //$$             if (key == 48 && Keyboard.isKeyDown(61)) {
                    //#if MC>=10800
                    //$$ mc.getRenderManager().setDebugBoundingBox(!mc.getRenderManager().isDebugBoundingBox());
                    //#else
                    //$$ RenderManager.debugBoundingBox = !RenderManager.debugBoundingBox;
                    //#endif
    //$$             }
    //$$
    //$$             if (key == 25 && Keyboard.isKeyDown(61)) {
    //$$                 mc.gameSettings.pauseOnLostFocus = !mc.gameSettings.pauseOnLostFocus;
    //$$                 mc.gameSettings.saveOptions();
    //$$             }
    //$$
    //$$             if (key == 59) {
    //$$                 mc.gameSettings.hideGUI = !mc.gameSettings.hideGUI;
    //$$             }
    //$$
    //$$             if (key == 61) {
    //$$                 mc.gameSettings.showDebugInfo = !mc.gameSettings.showDebugInfo;
    //$$                 mc.gameSettings.showDebugProfilerChart = GuiScreen.isShiftKeyDown();
    //$$             }
    //$$
    //$$             if (mc.gameSettings.keyBindTogglePerspective.isPressed()) {
    //$$                 mc.gameSettings.thirdPersonView = (mc.gameSettings.thirdPersonView + 1) % 3;
    //$$
                    //#if MC>=10800
                    //$$ if (mc.entityRenderer != null) { // Extra check, not in vanilla code
                    //$$     if (mc.gameSettings.thirdPersonView == 0) {
                    //$$         mc.entityRenderer.loadEntityShader(mc.getRenderViewEntity());
                    //$$     } else if (mc.gameSettings.thirdPersonView == 1) {
                    //$$         mc.entityRenderer.loadEntityShader(null);
                    //$$     }
                    //$$ }
                    //#endif
    //$$             }
    //$$         }
    //$$
    //$$         // Navigation in the debug chart
    //$$         if (mc.gameSettings.showDebugInfo && mc.gameSettings.showDebugProfilerChart) {
    //$$             if (key == Keyboard.KEY_0) {
    //$$                 mc.updateDebugProfilerName(0);
    //$$             }
    //$$
    //$$             for (int i = 0; i < 9; ++i) {
    //$$                 if (key == 2 + i) {
    //$$                     mc.updateDebugProfilerName(i + 1);
    //$$                 }
    //$$             }
    //$$         }
    //$$     }
    //$$
    //$$     FMLCommonHandler.instance().fireKeyInput();
    //$$ }
    //#endif

    //#if MC<=10710
    //$$ public static class RunScheduledTasks extends Event {}
    //#endif
}
