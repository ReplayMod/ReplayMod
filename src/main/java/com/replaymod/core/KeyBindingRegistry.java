package com.replaymod.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.replaymod.core.versions.MCVer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import org.lwjgl.input.Keyboard;

//#if MC>=10800
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#else
//$$ import cpw.mods.fml.client.registry.ClientRegistry;
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import cpw.mods.fml.common.gameevent.InputEvent;
//$$ import cpw.mods.fml.common.gameevent.TickEvent;
//#endif

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.replaymod.core.versions.MCVer.*;

public class KeyBindingRegistry {
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
            keyBinding = new KeyBinding(name, keyCode, "replaymod.title");
            keyBindings.put(name, keyBinding);
            ClientRegistry.registerKeyBinding(keyBinding);
        }
        return keyBinding;
    }

    public void registerRaw(int keyCode, Runnable whenPressed) {
        rawHandlers.put(keyCode, whenPressed);
    }

    public Map<String, KeyBinding> getKeyBindings() {
        return Collections.unmodifiableMap(keyBindings);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        handleKeyBindings();
        handleRaw();
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        handleRepeatedKeyBindings();
    }

    public void handleRepeatedKeyBindings() {
        for (Map.Entry<KeyBinding, Collection<Runnable>> entry : repeatedKeyBindingHandlers.asMap().entrySet()) {
            if (isKeyDown(entry.getKey())) {
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
                category.addCrashSection("Key Binding", keyBinding);
                MCVer.addDetail(category, "Handler", runnable::toString);
                throw new ReportedException(crashReport);
            }
        }
    }

    public void handleRaw() {
        int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        for (final Runnable runnable : rawHandlers.get(keyCode)) {
            try {
                runnable.run();
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.makeCrashReport(cause, "Handling Raw Key Binding");
                CrashReportCategory category = crashReport.makeCategory("Key Binding");
                category.addCrashSection("Key Code", keyCode);
                MCVer.addDetail(category, "Handler", runnable::toString);
                throw new ReportedException(crashReport);
            }
        }
    }
}
