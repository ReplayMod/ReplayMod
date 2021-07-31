package com.replaymod.core;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SettingsRegistryBackend {
    private final Map<SettingsRegistry.SettingKey<?>, Object> settings;

    private static final Object NULL_OBJECT = new Object();
    private Configuration configuration;

    SettingsRegistryBackend(Map<SettingsRegistry.SettingKey<?>, Object> settings) {
        this.settings = settings;
    }

    public void register() {
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;

        for (SettingsRegistry.SettingKey<?> key : new ArrayList<>(settings.keySet())) {
            register(key);
        }
    }

    public void register(SettingsRegistry.SettingKey<?> key) {
        Object value;
        if (configuration != null) {
            if (key.getDefault() instanceof Boolean) {
                value = configuration.get(key.getCategory(), key.getKey(), (Boolean) key.getDefault()).getBoolean();
            } else if (key.getDefault() instanceof Integer) {
                value = configuration.get(key.getCategory(), key.getKey(), (Integer) key.getDefault()).getInt();
            } else if (key.getDefault() instanceof Double) {
                value = configuration.get(key.getCategory(), key.getKey(), (Double) key.getDefault()).getDouble();
            } else if (key.getDefault() instanceof String) {
                Property property = configuration.get(key.getCategory(), key.getKey(), (String) key.getDefault());
                value = property.getString();
                if (key instanceof SettingsRegistry.MultipleChoiceSettingKey) {
                    @SuppressWarnings("unchecked")
                    List<String> choices = ((SettingsRegistry.MultipleChoiceSettingKey<String>) key).getChoices();
                    property.setValidValues(choices.toArray(new String[0]));
                    property.setComment("Valid values: " + String.join(", ", choices));
                }
            } else {
                throw new IllegalArgumentException("Default type " + key.getDefault().getClass() + " not supported.");
            }
        } else {
            value = NULL_OBJECT;
        }
        settings.put(key, value);
    }

    public <T> void update(SettingsRegistry.SettingKey<T> key, T value) {
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
    }

    public void save() {
        configuration.save();
    }
}
