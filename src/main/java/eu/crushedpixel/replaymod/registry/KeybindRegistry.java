package eu.crushedpixel.replaymod.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeybindRegistry {

    public static final String KEY_LIGHTING = "replaymod.input.lighting";
    public static final String KEY_THUMBNAIL = "replaymod.input.thumbnail";
    public static final String KEY_SPECTATE = "replaymod.input.spectate";
    public static final String KEY_CLEAR_KEYFRAMES = "replaymod.input.clearkeyframes";
    public static final String KEY_SYNC_TIMELINE = "replaymod.input.synctimeline";
    public static final String KEY_KEYFRAME_PRESETS = "replaymod.input.keyframerepository";
    public static final String KEY_ROTATE_CLOCKWISE = "replaymod.input.rotateclockwise";
    public static final String KEY_ROTATE_COUNTERCLOCKWISE = "replaymod.input.rotatecounterclockwise";
    public static final String KEY_RESET_TILT = "replaymod.input.resettilt";
    private static Minecraft mc = Minecraft.getMinecraft();

    public static void initialize() {
        List<KeyBinding> bindings = new ArrayList<KeyBinding>(Arrays.asList(mc.gameSettings.keyBindings));

        bindings.add(new KeyBinding(KEY_LIGHTING, Keyboard.KEY_M, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_THUMBNAIL, Keyboard.KEY_N, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_SPECTATE, Keyboard.KEY_B, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_SYNC_TIMELINE, Keyboard.KEY_V, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_CLEAR_KEYFRAMES, Keyboard.KEY_C, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_KEYFRAME_PRESETS, Keyboard.KEY_X, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_ROTATE_CLOCKWISE, Keyboard.KEY_L, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_RESET_TILT, Keyboard.KEY_K, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_ROTATE_COUNTERCLOCKWISE, Keyboard.KEY_J, "replaymod.title"));

        mc.gameSettings.keyBindings = bindings.toArray(new KeyBinding[bindings.size()]);

        mc.gameSettings.loadOptions();
    }
}
