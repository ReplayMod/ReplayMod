package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.events.handlers.keyboard.StaticKeybinding;
import lombok.Getter;
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
    public static final String KEY_ASSET_MANAGER = "replaymod.input.assetmanager";
    public static final String KEY_OBJECT_MANAGER = "replaymod.input.objectmanager";
    public static final String KEY_TOGGLE_INTERPOLATION = "replaymod.input.interpolation";
    private static Minecraft mc = Minecraft.getMinecraft();

    @Getter
    private static final List<KeyBinding> replayModKeyBindings;

    static {
        replayModKeyBindings = new ArrayList<KeyBinding>();

        replayModKeyBindings.add(new KeyBinding(KEY_PLAY_PAUSE, Keyboard.KEY_P, "replaymod.title"));

        replayModKeyBindings.add(new KeyBinding(KEY_ROLL_CLOCKWISE, Keyboard.KEY_L, "replaymod.title"));
        replayModKeyBindings.add(new KeyBinding(KEY_ROLL_COUNTERCLOCKWISE, Keyboard.KEY_J, "replaymod.title"));
        replayModKeyBindings.add(new KeyBinding(KEY_RESET_TILT, Keyboard.KEY_K, "replaymod.title"));

        replayModKeyBindings.add(new KeyBinding(KEY_CLEAR_KEYFRAMES, Keyboard.KEY_C, "replaymod.title"));
        replayModKeyBindings.add(new KeyBinding(KEY_KEYFRAME_PRESETS, Keyboard.KEY_X, "replaymod.title"));
        replayModKeyBindings.add(new KeyBinding(KEY_PATH_PREVIEW, Keyboard.KEY_H, "replaymod.title"));

        replayModKeyBindings.add(new KeyBinding(KEY_SYNC_TIMELINE, Keyboard.KEY_V, "replaymod.title"));

        replayModKeyBindings.add(new KeyBinding(KEY_THUMBNAIL, Keyboard.KEY_N, "replaymod.title"));
        replayModKeyBindings.add(new KeyBinding(KEY_PLAYER_OVERVIEW, Keyboard.KEY_B, "replaymod.title"));

        replayModKeyBindings.add(new KeyBinding(KEY_LIGHTING, Keyboard.KEY_Z, "replaymod.title"));

        replayModKeyBindings.add(new KeyBinding(KEY_ASSET_MANAGER, Keyboard.KEY_G, "replaymod.title"));
        replayModKeyBindings.add(new KeyBinding(KEY_OBJECT_MANAGER, Keyboard.KEY_F, "replaymod.title"));

        replayModKeyBindings.add(new KeyBinding(KEY_TOGGLE_INTERPOLATION, Keyboard.KEY_O, "replaymod.title"));
    }

    public static void initialize() {
        List<KeyBinding> bindings = new ArrayList<KeyBinding>(Arrays.asList(mc.gameSettings.keyBindings));
        bindings.addAll(replayModKeyBindings);

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

    public static final int STATIC_DELETE_KEYFRAME = 0;

    public static final StaticKeybinding[] staticKeybindings = new StaticKeybinding[]{new StaticKeybinding(STATIC_DELETE_KEYFRAME, Keyboard.KEY_DELETE)};
}
