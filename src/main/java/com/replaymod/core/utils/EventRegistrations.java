package com.replaymod.core.utils;

import net.fabricmc.fabric.api.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EventRegistrations {
    private List<EventRegistration<?>> registrations = new ArrayList<>();

    public <T> EventRegistrations on(EventRegistration<T> registration) {
        registrations.add(registration);
        return this;
    }

    public <T> EventRegistrations on(Event<T> event, T listener) {
        return on(EventRegistration.create(event, listener));
    }

    public void register() {
        for (EventRegistration<?> registration : registrations) {
            registration.register();
        }
    }

    public void unregister() {
        for (EventRegistration<?> registration : registrations) {
            registration.unregister();
        }
    }
}
