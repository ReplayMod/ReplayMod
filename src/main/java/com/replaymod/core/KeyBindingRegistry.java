package com.replaymod.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.core.versions.MCVer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;

//#if MC>=11400
//$$ import com.replaymod.core.versions.LangResourcePack;
//$$ import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
//$$ import net.minecraft.client.util.InputUtil;
//$$ import net.minecraft.util.Identifier;
//$$ import static com.replaymod.core.ReplayMod.MOD_ID;
//#else
import net.minecraftforge.fml.client.registry.ClientRegistry;
//#endif

//#if MC>=11300
import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.KeyEventCallback;
import com.replaymod.core.events.PreRenderCallback;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//$$ import net.minecraftforge.fml.common.gameevent.InputEvent;
//$$ import net.minecraftforge.fml.common.gameevent.TickEvent;
//$$ import org.lwjgl.input.Keyboard;
//$$ import static com.replaymod.core.versions.MCVer.FML_BUS;
//#endif

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KeyBindingRegistry extends EventRegistrations {
    private static final String CATEGORY = "replaymod.title";
    //#if MC>=11400
    //$$ static { net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry.INSTANCE.addCategory(CATEGORY); }
    //#endif

    private Map<String, KeyBinding> keyBindings = new HashMap<String, KeyBinding>();
    private Multimap<KeyBinding, Runnable> keyBindingHandlers = ArrayListMultimap.create();
    private Multimap<KeyBinding, Runnable> repeatedKeyBindingHandlers = ArrayListMultimap.create();
    private Multimap<Integer, Runnable> rawHandlers = ArrayListMultimap.create();

    public void registerKeyBinding(String name, int keyCode, Runnable whenPressed) {
        keyBindingHandlers.put(registerKeyBinding(name, keyCode), whenPressed);
    }

    public void registerRepeatedKeyBinding(String name, int keyCode, Runnable whenPressed) {
        repeatedKeyBindingHandlers.put(registerKeyBinding(name, keyCode), whenPressed);
    }

    private KeyBinding registerKeyBinding(String name, int keyCode) {
        KeyBinding keyBinding = keyBindings.get(name);
        if (keyBinding == null) {
            //#if MC>=11400
            //$$ if (keyCode == 0) {
            //$$     keyCode = -1;
            //$$ }
            //$$ Identifier id = new Identifier(MOD_ID, name.substring(LangResourcePack.LEGACY_KEY_PREFIX.length()));
            //$$ FabricKeyBinding fabricKeyBinding = FabricKeyBinding.Builder.create(id, InputUtil.Type.KEYSYM, keyCode, CATEGORY).build();
            //$$ net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry.INSTANCE.register(fabricKeyBinding);
            //$$ keyBinding = fabricKeyBinding;
            //#else
            keyBinding = new KeyBinding(name, keyCode, CATEGORY);
            ClientRegistry.registerKeyBinding(keyBinding);
            //#endif
            keyBindings.put(name, keyBinding);
        }
        return keyBinding;
    }

    public void registerRaw(int keyCode, Runnable whenPressed) {
        rawHandlers.put(keyCode, whenPressed);
    }

    public Map<String, KeyBinding> getKeyBindings() {
        return Collections.unmodifiableMap(keyBindings);
    }

    //#if MC>=11300
    { on(KeyBindingEventCallback.EVENT, this::handleKeyBindings); }
    { on(KeyEventCallback.EVENT, (keyCode, scanCode, action, modifiers) -> handleRaw(keyCode, action)); }
    { on(PreRenderCallback.EVENT, this::handleRepeatedKeyBindings); }
    //#else
    //$$ @SubscribeEvent
    //$$ public void onKeyInput(InputEvent.KeyInputEvent event) {
    //$$     handleKeyBindings();
    //$$     handleRaw();
    //$$ }
    //$$
    //$$ @SubscribeEvent
    //$$ public void onTick(TickEvent.RenderTickEvent event) {
    //$$     if (event.phase != TickEvent.Phase.START) return;
    //$$     handleRepeatedKeyBindings();
    //$$ }
    //#endif

    public void handleRepeatedKeyBindings() {
        for (Map.Entry<KeyBinding, Collection<Runnable>> entry : repeatedKeyBindingHandlers.asMap().entrySet()) {
            if (entry.getKey().isKeyDown()) {
                invokeKeyBindingHandlers(entry.getKey(), entry.getValue());
            }
        }
    }

    public void handleKeyBindings() {
        for (Map.Entry<KeyBinding, Collection<Runnable>> entry : keyBindingHandlers.asMap().entrySet()) {
            while (entry.getKey().isPressed()) {
                invokeKeyBindingHandlers(entry.getKey(), entry.getValue());
            }
        }
    }

    private void invokeKeyBindingHandlers(KeyBinding keyBinding, Collection<Runnable> handlers) {
        for (final Runnable runnable : handlers) {
            try {
                runnable.run();
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.makeCrashReport(cause, "Handling Key Binding");
                CrashReportCategory category = crashReport.makeCategory("Key Binding");
                MCVer.addDetail(category, "Key Binding", keyBinding::toString);
                MCVer.addDetail(category, "Handler", runnable::toString);
                throw new ReportedException(crashReport);
            }
        }
    }

    //#if MC>=11300
    private void handleRaw(int keyCode, int action) {
        if (action != 0) return;
    //#else
    //$$ private void handleRaw() {
    //$$     int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
    //#endif
        for (final Runnable runnable : rawHandlers.get(keyCode)) {
            try {
                runnable.run();
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.makeCrashReport(cause, "Handling Raw Key Binding");
                CrashReportCategory category = crashReport.makeCategory("Key Binding");
                MCVer.addDetail(category, "Key Code", () -> "" + keyCode);
                MCVer.addDetail(category, "Handler", runnable::toString);
                throw new ReportedException(crashReport);
            }
        }
    }
}
