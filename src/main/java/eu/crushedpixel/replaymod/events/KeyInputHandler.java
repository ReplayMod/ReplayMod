package eu.crushedpixel.replaymod.events;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity.MoveDirection;
import eu.crushedpixel.replaymod.gui.GuiCancelRender;
import eu.crushedpixel.replaymod.gui.GuiMouseInput;
import eu.crushedpixel.replaymod.registry.KeybindRegistry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.replay.spectate.SpectateHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;

public class KeyInputHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    private boolean escDown = false;

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) throws LWJGLException {

        if(!ReplayHandler.isInReplay()) return;
        if(mc.currentScreen != null && !(mc.currentScreen instanceof GuiMouseInput)) {
            return;
        }

        if(Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent() && Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)
                && ReplayHandler.isInPath() && ReplayProcess.isVideoRecording()
                && mc.currentScreen == null && !escDown) {
            mc.displayGuiScreen(new GuiCancelRender());
        }

        if(!Keyboard.isCreated()) Keyboard.create();
        escDown = Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && Keyboard.getEventKeyState();

        boolean found = false;

        KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;
        for(KeyBinding kb : keyBindings) {
            if(!kb.isKeyDown()) {
                continue;
            }
            try {
                boolean speedup = false;

                if(ReplayHandler.isCamera()) {
                    if(kb.getKeyDescription().equals("key.forward")) {
                        ReplayHandler.getCameraEntity().setMovement(MoveDirection.FORWARD);
                        speedup = true;
                    }

                    if(kb.getKeyDescription().equals("key.back")) {
                        ReplayHandler.getCameraEntity().setMovement(MoveDirection.BACKWARD);
                        speedup = true;
                    }

                    if(kb.getKeyDescription().equals("key.jump")) {
                        ReplayHandler.getCameraEntity().setMovement(MoveDirection.UP);
                        speedup = true;
                    }

                    if(kb.getKeyDescription().equals("key.left")) {
                        ReplayHandler.getCameraEntity().setMovement(MoveDirection.LEFT);
                        speedup = true;
                    }

                    if(kb.getKeyDescription().equals("key.right")) {
                        ReplayHandler.getCameraEntity().setMovement(MoveDirection.RIGHT);
                        speedup = true;
                    }
                }
                if(kb.getKeyDescription().equals("key.sneak")) {
                    if(ReplayHandler.isCamera()) {
                        ReplayHandler.getCameraEntity().setMovement(MoveDirection.DOWN);
                        speedup = true;
                    } else {
                        ReplayHandler.spectateCamera();
                    }
                }

                if(speedup) {
                    ReplayHandler.getCameraEntity().speedUp();
                }

                if(kb.getKeyDescription().equals("key.chat")) {
                    mc.displayGuiScreen(new GuiMouseInput());
                    break;
                }

                handleCustomKeybindings(kb, found, null);

            } catch(Exception e) {
                e.printStackTrace();
            }
            found = true;
        }
    }

    public void handleCustomKeybindings(KeyBinding kb, boolean found, Integer keyCode) {
        //Custom registered handlers
        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_THUMBNAIL) && (kb.isPressed() || kb.getKeyCode() == keyCode) && !found) {
            TickAndRenderListener.requestScreenshot();
            //TODO: Make this properly work
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_SPECTATE) && (kb.isPressed() || kb.getKeyCode() == keyCode) && !found) {
            SpectateHandler.openSpectateSelection();
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_LIGHTING) && (kb.isPressed() || kb.getKeyCode() == keyCode)) {
            ReplayMod.replaySettings.setLightingEnabled(!ReplayMod.replaySettings.isLightingEnabled());
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_CLEAR_KEYFRAMES) && (kb.isPressed() || kb.getKeyCode() == keyCode)) {
            ReplayHandler.resetKeyframes();
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_SYNC_TIMELINE) && (kb.isPressed() || kb.getKeyCode() == keyCode)) {
            ReplayHandler.syncTimeCursor(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        }
    }
}
