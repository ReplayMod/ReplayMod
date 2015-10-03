package com.replaymod.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class KeyBindingRegistry {
    private Map<String, KeyBinding> keyBindings = new HashMap<String, KeyBinding>();
    private Multimap<KeyBinding, Runnable> keyBindingHandlers = ArrayListMultimap.create();

    public void registerKeyBinding(String name, int keyCode, Runnable whenPressed) {
        KeyBinding keyBinding = keyBindings.get(name);
        if (keyBinding == null) {
            keyBinding = new KeyBinding(name, keyCode, "replaymod.title");
            keyBindings.put(name, keyBinding);
            ClientRegistry.registerKeyBinding(keyBinding);
        }
        keyBindingHandlers.put(keyBinding, whenPressed);
    }

    public Map<String, KeyBinding> getKeyBindings() {
        return Collections.unmodifiableMap(keyBindings);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        for (Map.Entry<KeyBinding, Collection<Runnable>> entry : keyBindingHandlers.asMap().entrySet()) {
            while (entry.getKey().isPressed()) {
                for (final Runnable runnable : entry.getValue()) {
                    try {
                        runnable.run();
                    } catch (Throwable cause) {
                        CrashReport crashReport = CrashReport.makeCrashReport(cause, "Handling Key Binding");
                        CrashReportCategory category = crashReport.makeCategory("Key Binding");
                        category.addCrashSection("Key Binding", entry.getKey());
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
    }
}
