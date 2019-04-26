package com.replaymod.core.utils;

import com.replaymod.replaystudio.us.myles.ViaVersion.util.ReflectionUtil;
import net.fabricmc.fabric.api.event.Event;

import java.lang.reflect.InvocationTargetException;

public class EventRegistration<T> {
    public static <T> EventRegistration<T> create(Event<T> event, T callback) {
        return new EventRegistration<>(event, callback);
    }

    public static <T> EventRegistration<T> register(Event<T> event, T callback) {
        EventRegistration<T> registration = new EventRegistration<>(event, callback);
        registration.register();
        return registration;
    }

    private final Event<T> event;
    private final T listener;
    private boolean registered;

    private EventRegistration(Event<T> event, T listener) {
        this.event = event;
        this.listener = listener;

        event.register(listener);
    }

    public void register() {
        if (registered) {
            throw new IllegalStateException();
        }

        event.register(listener);
        registered = true;
    }

    @SuppressWarnings("unchecked")
    public void unregister() {
        if (!registered) {
            throw new IllegalStateException();
        }

        try {
            T[] handlers = (T[]) ReflectionUtil.get(event, "handlers", Object[].class);
            T[] copy = (T[]) new Object[handlers.length - 1];
            for (int from = 0, to = 0; from < handlers.length; from++) {
                if (handlers[from] == listener) {
                    continue;
                }
                copy[to++] = handlers[from];
            }
            if (copy.length == 0) {
                copy = null;
            }
            ReflectionUtil.set(event, "handlers", copy);
            ReflectionUtil.invoke(event, "update");
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        registered = false;
    }
}
