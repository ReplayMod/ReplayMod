package com.replaymod.core;

import com.replaymod.core.events.SettingsChangedCallback;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class SettingsRegistry {
    private final Map<SettingKey<?>, Object> settings = Collections.synchronizedMap(new LinkedHashMap<>());
    final SettingsRegistryBackend backend = new SettingsRegistryBackend(settings);

    public void register() {
        backend.register();
    }

    public void register(Class<?> settingsClass) {
        for (Field field : settingsClass.getDeclaredFields()) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != 0
                    && SettingKey.class.isAssignableFrom(field.getType())) {
                try {
                    register((SettingKey<?>) field.get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void register(SettingKey<?> key) {
        settings.put(key, key.getDefault());
        backend.register(key);
    }

    public Set<SettingKey<?>> getSettings() {
        return settings.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(SettingKey<T> key) {
        if (!settings.containsKey(key)) {
            throw new IllegalArgumentException("Setting " + key + " unknown.");
        }
        return (T) settings.get(key);
    }

    public <T> void set(SettingKey<T> key, T value) {
        backend.update(key, value);
        settings.put(key, value);
        SettingsChangedCallback.EVENT.invoker().onSettingsChanged(this, key);
    }

    public void save() {
        backend.save();
    }

    public interface SettingKey<T> {
        String getCategory();
        String getKey();
        String getDisplayString();
        T getDefault();
    }

    public interface MultipleChoiceSettingKey<T> extends SettingKey<T> {
        List<T> getChoices();
    }

    public static class SettingKeys<T> implements SettingKey<T> {
        private final String category;
        private final String key;
        private final String displayString;
        private final T defaultValue;

        public SettingKeys(String category, String key, String displayString, T defaultValue) {
            this.category = category;
            this.key = key;
            this.displayString = displayString;
            this.defaultValue = defaultValue;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getDisplayString() {
            return displayString;
        }

        @Override
        public T getDefault() {
            return defaultValue;
        }
    }

    public static class MultipleChoiceSettingKeys<T> extends SettingKeys<T> implements MultipleChoiceSettingKey<T> {
        private List<T> choices = Collections.emptyList();

        public MultipleChoiceSettingKeys(String category, String key, String displayString, T defaultValue) {
            super(category, key, displayString, defaultValue);
        }

        public void setChoices(List<T> choices) {
            this.choices = Collections.unmodifiableList(choices);
        }

        @Override
        public List<T> getChoices() {
            return choices;
        }
    }
}
