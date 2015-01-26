package eu.crushedpixel.replaymod.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

public class KeybindRegistry {

	private static Minecraft mc = Minecraft.getMinecraft();
	
	public static void initialize() {
		List<KeyBinding> bindings = new ArrayList<KeyBinding>(Arrays.asList(mc.gameSettings.keyBindings));
		
		bindings.add(new KeyBinding("key.lighting", Keyboard.KEY_V, "category.replaymod"));
		bindings.add(new KeyBinding("key.thumbnail", Keyboard.KEY_B, "category.replaymod"));
		
		mc.gameSettings.keyBindings = bindings.toArray(new KeyBinding[bindings.size()]);
	}
}
