package eu.crushedpixel.replaymod.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity.MoveDirection;
import eu.crushedpixel.replaymod.gui.GuiMouseInput;
import eu.crushedpixel.replaymod.registry.KeybindRegistry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.screenshot.ReplayScreenshot;
import eu.crushedpixel.replaymod.replay.spectate.SpectateHandler;

public class KeyInputHandler {

	private Minecraft mc = Minecraft.getMinecraft();

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {

		if(!ReplayHandler.replayActive()) return;
		if(mc.currentScreen != null) {
			return;
		}

		KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;
		for(KeyBinding kb : keyBindings) {
			if(!kb.isKeyDown()) {
				continue;
			}
			try {

				if(ReplayHandler.isCamera()) {
					if(kb.getKeyDescription().equals("key.forward")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.FORWARD);
						continue;
					}

					if(kb.getKeyDescription().equals("key.back")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.BACKWARD);
						continue;
					}

					if(kb.getKeyDescription().equals("key.jump")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.UP);
						continue;
					}

					if(kb.getKeyDescription().equals("key.left")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.LEFT);
						continue;
					}

					if(kb.getKeyDescription().equals("key.right")) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.RIGHT);
						continue;
					}
				}
				if(kb.getKeyDescription().equals("key.sneak")) {
					if(ReplayHandler.isCamera()) {
						ReplayHandler.getCameraEntity().setMovement(MoveDirection.DOWN);
					} else {
						ReplayHandler.spectateCamera();
					}
					continue;
				}

				if(kb.getKeyDescription().equals("key.chat")) {
					mc.displayGuiScreen(new GuiMouseInput());
					continue;
				}

				//Custom registered handlers
				if(kb.getKeyDescription().equals(KeybindRegistry.KEY_THUMBNAIL) && kb.isPressed()) {
					ReplayScreenshot.prepareScreenshot();
					GuiReplayOverlay.requestScreenshot();
					continue;
				}

				if(kb.getKeyDescription().equals(KeybindRegistry.KEY_SPECTATE) && kb.isPressed()) {
					SpectateHandler.openSpectateSelection();
					continue;
				}

				if(kb.getKeyDescription().equals(KeybindRegistry.KEY_LIGHTING) && kb.isPressed()) {
					ReplayMod.replaySettings.setLightingEnabled(!ReplayMod.replaySettings.isLightingEnabled());			
					continue;
				}

			} catch(Exception e) {
				e.printStackTrace();
			}

		}
	}
}
