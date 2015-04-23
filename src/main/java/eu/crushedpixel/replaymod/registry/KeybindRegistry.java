package eu.crushedpixel.replaymod.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeybindRegistry {

    public static final String KEY_LIGHTING = "Toggle Lighting";
    public static final String KEY_THUMBNAIL = "Create Thumbnail";
    public static final String KEY_SPECTATE = "Spectate Entity";
    private static Minecraft mc = Minecraft.getMinecraft();

    public static void initialize() {
        List<KeyBinding> bindings = new ArrayList<KeyBinding>(Arrays.asList(mc.gameSettings.keyBindings));

        bindings.add(new KeyBinding(KEY_LIGHTING, Keyboard.KEY_V, "Replay Mod"));
        bindings.add(new KeyBinding(KEY_THUMBNAIL, Keyboard.KEY_B, "Replay Mod"));
        bindings.add(new KeyBinding(KEY_SPECTATE, Keyboard.KEY_C, "Replay Mod"));

        mc.gameSettings.keyBindings = bindings.toArray(new KeyBinding[bindings.size()]);
    }
}
