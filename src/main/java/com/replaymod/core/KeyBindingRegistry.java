package com.replaymod.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import org.lwjgl.input.Keyboard;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

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
            if (entry.getKey().getIsKeyPressed()) {
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
                category.addCrashSectionCallable("Handler", runnable::toString);
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
                category.addCrashSectionCallable("Handler", new Callable() {
                    @Override
                    public Object call() throws Exception {
                        return runnable;
                    }
                });
                throw new ReportedException(crashReport);
            }
        }
    }
}
