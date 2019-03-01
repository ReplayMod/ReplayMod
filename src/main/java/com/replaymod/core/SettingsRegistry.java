package com.replaymod.core;

import com.replaymod.core.events.SettingsChangedEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//#if MC>=11300
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//#else
//$$ import net.minecraftforge.common.config.Configuration;
//#endif

import static com.replaymod.core.versions.MCVer.*;

public class SettingsRegistry {
    private static final Object NULL_OBJECT = new Object();
    private Map<SettingKey<?>, Object> settings = new ConcurrentHashMap<>();
    //#if MC>=11300
    private ForgeConfigSpec spec;
    private ModConfig config;
    //#else
    //$$ private Configuration configuration;
    //#endif

    //#if MC>=11300
    public void register() {
        if (spec == null) {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            for (SettingKey<?> key : settings.keySet()) {
                builder
                        .translation(key.getDisplayString())
                        .define(key.getCategory() + "." + key.getKey(), key.getDefault());
            }
            spec = builder.build();
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::load);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, spec);
    }

    private void load(ModConfig.Loading event) {
        config = event.getConfig();
        for (Map.Entry<SettingKey<?>, Object> entry : settings.entrySet()) {
            SettingKey<?> key = entry.getKey();
            Object value = config.getConfigData().get(key.getCategory() + "." + key.getKey());
            entry.setValue(value == null ? key.getDefault() : value);
        }
    }
    //#else
    //$$ public void setConfiguration(Configuration configuration) {
    //$$     this.configuration = configuration;
    //$$
    //$$     List<SettingKey<?>> keys = new ArrayList<>(settings.keySet());
    //$$     settings.clear();
    //$$     for (SettingKey key : keys) {
    //$$         register(key);
    //$$     }
    //$$ }
    //#endif

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
        //#if MC>=11300
        if (spec != null) {
            throw new IllegalStateException("Cannot register more settings are spec has been built.");
        }
        settings.put(key, NULL_OBJECT);
        //#else
        //$$ Object value;
        //$$ if (configuration != null) {
        //$$     if (key.getDefault() instanceof Boolean) {
        //$$         value = configuration.get(key.getCategory(), key.getKey(), (Boolean) key.getDefault()).getBoolean();
        //$$     } else if (key.getDefault() instanceof Integer) {
        //$$         value = configuration.get(key.getCategory(), key.getKey(), (Integer) key.getDefault()).getInt();
        //$$     } else if (key.getDefault() instanceof Double) {
        //$$         value = configuration.get(key.getCategory(), key.getKey(), (Double) key.getDefault()).getDouble();
        //$$     } else if (key.getDefault() instanceof String) {
        //$$         value = configuration.get(key.getCategory(), key.getKey(), (String) key.getDefault()).getString();
        //$$     } else {
        //$$         throw new IllegalArgumentException("Default type " + key.getDefault().getClass() + " not supported.");
        //$$     }
        //$$ } else {
        //$$     value = NULL_OBJECT;
        //$$ }
        //$$ settings.put(key, value);
        //#endif
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
        //#if MC>=11300
        if (config != null) {
            config.getConfigData().set(key.getCategory() + "." + key.getKey(), value);
        }
        //#else
        //$$ if (key.getDefault() instanceof Boolean) {
        //$$     configuration.get(key.getCategory(), key.getKey(), (Boolean) key.getDefault()).set((Boolean) value);
        //$$ } else if (key.getDefault() instanceof Integer) {
        //$$     configuration.get(key.getCategory(), key.getKey(), (Integer) key.getDefault()).set((Integer) value);
        //$$ } else if (key.getDefault() instanceof Double) {
        //$$     configuration.get(key.getCategory(), key.getKey(), (Double) key.getDefault()).set((Double) value);
        //$$ } else if (key.getDefault() instanceof String) {
        //$$     configuration.get(key.getCategory(), key.getKey(), (String) key.getDefault()).set((String) value);
        //$$ } else {
        //$$     throw new IllegalArgumentException("Default type " + key.getDefault().getClass() + " not supported.");
        //$$ }
        //#endif
        settings.put(key, value);
        FML_BUS.post(new SettingsChangedEvent(this, key));
    }

    public void save() {
        //#if MC>=11300
        if (config != null) {
            config.save();
        }
        //#else
        //$$ configuration.save();
        //#endif
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
