package com.replaymod.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.replaymod.core.mixin.KeyBindingAccessor;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.core.versions.MCVer;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.crash.CrashException;

//#if FABRIC>=1
import com.replaymod.core.versions.LangResourcePack;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import static com.replaymod.core.ReplayMod.MOD_ID;
//#else
//$$ import net.minecraftforge.fml.client.registry.ClientRegistry;
//#endif

//#if MC>=11400
import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.KeyEventCallback;
import com.replaymod.core.events.PreRenderCallback;
import org.lwjgl.glfw.GLFW;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//$$ import net.minecraftforge.fml.common.gameevent.InputEvent;
//$$ import net.minecraftforge.fml.common.gameevent.TickEvent;
//$$ import org.lwjgl.input.Keyboard;
//$$ import static com.replaymod.core.versions.MCVer.FML_BUS;
//#endif

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class KeyBindingRegistry extends EventRegistrations {
    private static final String CATEGORY = "replaymod.title";
    //#if FABRIC>=1
    static { net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry.INSTANCE.addCategory(CATEGORY); }
    //#endif

    private final Map<String, Binding> bindings = new HashMap<>();
    private Set<KeyBinding> onlyInReplay = new HashSet<>();
    private Multimap<Integer, Runnable> rawHandlers = ArrayListMultimap.create();

    public Binding registerKeyBinding(String name, int keyCode, Runnable whenPressed, boolean onlyInRepay) {
        Binding binding = registerKeyBinding(name, keyCode, onlyInRepay);
        binding.handlers.add(whenPressed);
        return binding;
    }

    public Binding registerRepeatedKeyBinding(String name, int keyCode, Runnable whenPressed, boolean onlyInRepay) {
        Binding binding = registerKeyBinding(name, keyCode, onlyInRepay);
        binding.repeatedHandlers.add(whenPressed);
        return binding;
    }

    private Binding registerKeyBinding(String name, int keyCode, boolean onlyInRepay) {
        Binding binding = bindings.get(name);
        if (binding == null) {
            //#if FABRIC>=1
            if (keyCode == 0) {
                keyCode = -1;
            }
            Identifier id = new Identifier(MOD_ID, name.substring(LangResourcePack.LEGACY_KEY_PREFIX.length()));
            FabricKeyBinding fabricKeyBinding = FabricKeyBinding.Builder.create(id, InputUtil.Type.KEYSYM, keyCode, CATEGORY).build();
            net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry.INSTANCE.register(fabricKeyBinding);
            KeyBinding keyBinding = fabricKeyBinding;
            //#else
            //$$ KeyBinding keyBinding = new KeyBinding(name, keyCode, CATEGORY);
            //$$ ClientRegistry.registerKeyBinding(keyBinding);
            //#endif
            binding = new Binding(name, keyBinding);
            bindings.put(name, binding);
            if (onlyInRepay) {
                this.onlyInReplay.add(keyBinding);
            }
        } else if (!onlyInRepay) {
            this.onlyInReplay.remove(binding.keyBinding);
        }
        return binding;
    }

    public void registerRaw(int keyCode, Runnable whenPressed) {
        rawHandlers.put(keyCode, whenPressed);
    }

    public Map<String, Binding> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public Set<KeyBinding> getOnlyInReplay() {
        return Collections.unmodifiableSet(onlyInReplay);
    }

    //#if MC>=11400
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
        for (Binding binding : bindings.values()) {
            if (binding.keyBinding.isPressed()) {
                invokeKeyBindingHandlers(binding, binding.repeatedHandlers);
            }
        }
    }

    private void handleKeyBindings() {
        for (Binding binding : bindings.values()) {
            while (binding.keyBinding.wasPressed()) {
                invokeKeyBindingHandlers(binding, binding.handlers);
                invokeKeyBindingHandlers(binding, binding.repeatedHandlers);
            }
        }
    }

    private void invokeKeyBindingHandlers(Binding binding, Collection<Runnable> handlers) {
        for (final Runnable runnable : handlers) {
            try {
                runnable.run();
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.create(cause, "Handling Key Binding");
                CrashReportSection category = crashReport.addElement("Key Binding");
                MCVer.addDetail(category, "Key Binding", () -> binding.name);
                MCVer.addDetail(category, "Handler", runnable::toString);
                throw new CrashException(crashReport);
            }
        }
    }

    //#if MC>=11400
    private void handleRaw(int keyCode, int action) {
        if (action != GLFW.GLFW_PRESS) return;
    //#else
    //$$ private void handleRaw() {
    //$$     int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
    //#endif
        for (final Runnable runnable : rawHandlers.get(keyCode)) {
            try {
                runnable.run();
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.create(cause, "Handling Raw Key Binding");
                CrashReportSection category = crashReport.addElement("Key Binding");
                MCVer.addDetail(category, "Key Code", () -> "" + keyCode);
                MCVer.addDetail(category, "Handler", runnable::toString);
                throw new CrashException(crashReport);
            }
        }
    }

    public class Binding {
        public final String name;
        public final KeyBinding keyBinding;
        private final List<Runnable> handlers = new ArrayList<>();
        private final List<Runnable> repeatedHandlers = new ArrayList<>();
        private boolean autoActivation;
        private Consumer<Boolean> autoActivationUpdate;

        public Binding(String name, KeyBinding keyBinding) {
            this.name = name;
            this.keyBinding = keyBinding;
        }

        public String getBoundKey() {
            return MCVer.getBoundKey(keyBinding);
        }

        public boolean isBound() {
            //#if MC>=11400
            return !keyBinding.isUnbound();
            //#else
            //$$ return keyBinding.getKeyCode() != 0;
            //#endif
        }

        public void trigger() {
            KeyBindingAccessor acc = (KeyBindingAccessor) keyBinding;
            acc.setPressTime(acc.getPressTime() + 1);
            handleKeyBindings();
        }

        public void registerAutoActivationSupport(boolean active, Consumer<Boolean> update) {
            this.autoActivation = active;
            this.autoActivationUpdate = update;
        }

        public boolean supportsAutoActivation() {
            return autoActivationUpdate != null;
        }

        public boolean isAutoActivating() {
            return supportsAutoActivation() && autoActivation;
        }

        public void setAutoActivating(boolean active) {
            if (this.autoActivation == active) {
                return;
            }
            this.autoActivation = active;
            this.autoActivationUpdate.accept(active);
        }
    }
}
