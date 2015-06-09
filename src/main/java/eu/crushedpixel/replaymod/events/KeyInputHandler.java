package eu.crushedpixel.replaymod.events;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity.MoveDirection;
import eu.crushedpixel.replaymod.gui.GuiKeyframeRepository;
import eu.crushedpixel.replaymod.gui.GuiMouseInput;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.registry.KeybindRegistry;
import eu.crushedpixel.replaymod.registry.PlayerHandler;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;

public class KeyInputHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    private long prevKeysDown = Sys.getTime();

    public void onKeyInput() throws Exception {
        if(!ReplayHandler.isInReplay()) return;

        if(!Keyboard.isCreated()) Keyboard.create();

        Keyboard.poll();

        KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;

        boolean speedup = false;

        if(mc.currentScreen == null) {
            for(KeyBinding kb : keyBindings) {
                //don't act on Mouse inputs
                if(kb.getKeyCode() < 0) continue;

                if(!ReplayMod.replaySender.paused() && !kb.isKeyDown()) continue;

                if(ReplayMod.replaySender.paused() && !Keyboard.isKeyDown(kb.getKeyCode()))
                    continue;
                try {

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

                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if(ReplayHandler.getCameraEntity() != null) {
            if(speedup) {
                ReplayHandler.getCameraEntity().speedUp();
                prevKeysDown = Sys.getTime();
            } else {
                if(Sys.getTime() - prevKeysDown > 100) {
                    ReplayHandler.getCameraEntity().stopSpeedUp();
                }
            }
        }
    }

    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        try {
            onKeyInput();
        } catch(Exception e) {
            e.printStackTrace();
        }

        KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;

        boolean found = false;

        for(KeyBinding kb : keyBindings) {
            if(!kb.isKeyDown()) continue;

            if(kb.getKeyDescription().equals("key.chat") && kb.isPressed()) {
                mc.displayGuiScreen(new GuiMouseInput(ReplayMod.overlay));
                break;
            }

            handleCustomKeybindings(kb, found, -1);
            found = true;
        }

    }

    public void handleCustomKeybindings(KeyBinding kb, boolean found, int keyCode) {
        //Custom registered handlers
        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_ADD_MARKER) && (kb.isPressed() || kb.getKeyCode() == keyCode)) {
            if(ReplayHandler.isInReplay()) {
                ReplayHandler.addMarker();
            } else if(ConnectionEventHandler.isRecording()) {
                ConnectionEventHandler.addMarker();
            }
        }

        if(!ReplayHandler.isInReplay()) return;

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_PLAY_PAUSE) && (kb.isPressed() || kb.getKeyCode() == keyCode) && !ReplayHandler.isInPath()) {
            ReplayMod.overlay.togglePlayPause();
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_ROLL_CLOCKWISE) && (kb.isKeyDown() || kb.getKeyCode() == keyCode) && !ReplayHandler.isInPath()) {
            ReplayHandler.addCameraTilt(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) ? 0.2f : 1);
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_ROLL_COUNTERCLOCKWISE) && (kb.isKeyDown() || kb.getKeyCode() == keyCode) && !ReplayHandler.isInPath()) {
            ReplayHandler.addCameraTilt(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) ? -0.2f : -1);
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_RESET_TILT) && (kb.isKeyDown() || kb.getKeyCode() == keyCode) && !found && !ReplayHandler.isInPath()) {
            ReplayHandler.setCameraTilt(0);
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_THUMBNAIL) && (kb.isPressed() || kb.getKeyCode() == keyCode) && !found) {
            TickAndRenderListener.requestScreenshot();
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_PLAYER_OVERVIEW) && (kb.isPressed() || kb.getKeyCode() == keyCode) && !found) {
            PlayerHandler.openPlayerOverview();
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_LIGHTING) && (kb.isPressed() || kb.getKeyCode() == keyCode)) {
            ReplayMod.replaySettings.setLightingEnabled(!ReplayMod.replaySettings.isLightingEnabled());
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_CLEAR_KEYFRAMES) && (kb.isPressed() || kb.getKeyCode() == keyCode)) {
            ReplayHandler.resetKeyframes(false);
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_SYNC_TIMELINE) && (kb.isPressed() || kb.getKeyCode() == keyCode)) {
            ReplayHandler.syncTimeCursor(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        }

        if(kb.getKeyDescription().equals(KeybindRegistry.KEY_KEYFRAME_PRESETS) && (kb.isPressed() || kb.getKeyCode() == keyCode)) {
            mc.displayGuiScreen(new GuiKeyframeRepository(ReplayHandler.getKeyframeRepository()));
        }
    }
}
