package com.replaymod.core;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsRegistry {
    private static final Object NULL_OBJECT = new Object();
    private Map<SettingKey<?>, Object> settings = new ConcurrentHashMap<>();
    private Configuration configuration;

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;

        List<SettingKey<?>> keys = new ArrayList<>(settings.keySet());
        settings.clear();
        for (SettingKey key : keys) {
            register(key);
        }
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
        Object value;
        if (configuration != null) {
            if (key.getDefault() instanceof Boolean) {
                value = configuration.get(key.getCategory(), key.getKey(), (Boolean) key.getDefault()).getBoolean();
            } else if (key.getDefault() instanceof Integer) {
                value = configuration.get(key.getCategory(), key.getKey(), (Integer) key.getDefault()).getInt();
            } else if (key.getDefault() instanceof Double) {
                value = configuration.get(key.getCategory(), key.getKey(), (Double) key.getDefault()).getDouble();
            } else if (key.getDefault() instanceof String) {
                value = configuration.get(key.getCategory(), key.getKey(), (String) key.getDefault()).getString();
            } else {
                throw new IllegalArgumentException("Default type " + key.getDefault().getClass() + " not supported.");
            }
        } else {
            value = NULL_OBJECT;
        }
        settings.put(key, value);
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
        if (key.getDefault() instanceof Boolean) {
            configuration.get(key.getCategory(), key.getKey(), (Boolean) key.getDefault()).set((Boolean) value);
        } else if (key.getDefault() instanceof Integer) {
            configuration.get(key.getCategory(), key.getKey(), (Integer) key.getDefault()).set((Integer) value);
        } else if (key.getDefault() instanceof Double) {
            configuration.get(key.getCategory(), key.getKey(), (Double) key.getDefault()).set((Double) value);
        } else if (key.getDefault() instanceof String) {
            configuration.get(key.getCategory(), key.getKey(), (String) key.getDefault()).set((String) value);
        } else {
            throw new IllegalArgumentException("Default type " + key.getDefault().getClass() + " not supported.");
        }
        settings.put(key, value);
    }

    public void save() {
        configuration.save();
    }

    public interface SettingKey<T> {
        String getCategory();
        String getKey();
        String getDisplayString();
        T getDefault();
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
            return I18n.format(displayString);
        }

        @Override
        public T getDefault() {
            return defaultValue;
        }
    }
}
