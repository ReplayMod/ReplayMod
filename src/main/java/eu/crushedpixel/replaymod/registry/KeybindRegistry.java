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
    public static final String KEY_PLAYER_OVERVIEW = "replaymod.input.playeroverview";
    public static final String KEY_CLEAR_KEYFRAMES = "replaymod.input.clearkeyframes";
    public static final String KEY_SYNC_TIMELINE = "replaymod.input.synctimeline";
    public static final String KEY_KEYFRAME_PRESETS = "replaymod.input.keyframerepository";
    public static final String KEY_ROLL_CLOCKWISE = "replaymod.input.rollclockwise";
    public static final String KEY_ROLL_COUNTERCLOCKWISE = "replaymod.input.rollcounterclockwise";
    public static final String KEY_RESET_TILT = "replaymod.input.resettilt";
    public static final String KEY_PLAY_PAUSE = "replaymod.input.playpause";
    public static final String KEY_ADD_MARKER = "replaymod.input.marker";
    public static final String KEY_PATH_PREVIEW = "replaymod.input.pathpreview";
    private static Minecraft mc = Minecraft.getMinecraft();

    public static void initialize() {
        List<KeyBinding> bindings = new ArrayList<KeyBinding>(Arrays.asList(mc.gameSettings.keyBindings));

        bindings.add(new KeyBinding(KEY_ADD_MARKER, Keyboard.KEY_M, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_THUMBNAIL, Keyboard.KEY_N, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_PLAYER_OVERVIEW, Keyboard.KEY_B, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_SYNC_TIMELINE, Keyboard.KEY_V, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_CLEAR_KEYFRAMES, Keyboard.KEY_C, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_KEYFRAME_PRESETS, Keyboard.KEY_X, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_LIGHTING, Keyboard.KEY_Z, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_ROLL_CLOCKWISE, Keyboard.KEY_L, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_RESET_TILT, Keyboard.KEY_K, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_ROLL_COUNTERCLOCKWISE, Keyboard.KEY_J, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_PLAY_PAUSE, Keyboard.KEY_P, "replaymod.title"));
        bindings.add(new KeyBinding(KEY_PATH_PREVIEW, Keyboard.KEY_H, "replaymod.title"));

        mc.gameSettings.keyBindings = bindings.toArray(new KeyBinding[bindings.size()]);

        mc.gameSettings.loadOptions();
    }

    public static KeyBinding getKeyBinding(String binding) {
        for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
            if (binding.equals(keyBinding.getKeyDescription())) {
                return keyBinding;
            }
        }
        return null;
    }
}
