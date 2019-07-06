package com.replaymod.core;

import com.replaymod.core.events.SettingsChangedCallback;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

//#if MC>=11400
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
//#else
//#if MC>=11300
//$$ import net.minecraftforge.common.ForgeConfigSpec;
//$$ import net.minecraftforge.fml.ModLoadingContext;
//$$ import net.minecraftforge.fml.config.ModConfig;
//$$ import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//#else
//$$ import net.minecraftforge.common.config.Configuration;
//#endif
//#endif

import static com.replaymod.core.versions.MCVer.*;

public class SettingsRegistry {
    private Map<SettingKey<?>, Object> settings = Collections.synchronizedMap(new LinkedHashMap<>());
    //#if MC>=11400
    private final Path configFile = getMinecraft().runDirectory.toPath().resolve("config/replaymod.json");
    //#else
    //$$ private static final Object NULL_OBJECT = new Object();
    //#if MC>=11300
    //$$ private ForgeConfigSpec spec;
    //$$ private ModConfig config;
    //#else
    //$$ private Configuration configuration;
    //#endif
    //#endif

    //#if MC>=11400
    public void register() {
        String config;
        if (Files.exists(configFile)) {
            try {
                config = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            save();
            return;
        }
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(config, JsonObject.class);
        for (Map.Entry<SettingKey<?>, Object> entry : settings.entrySet()) {
            SettingKey<?> key = entry.getKey();
            JsonElement category = root.get(key.getCategory());
            if (category != null && category.isJsonObject()) {
                JsonElement valueElem = category.getAsJsonObject().get(key.getKey());
                if (!valueElem.isJsonPrimitive()) continue;
                JsonPrimitive value = valueElem.getAsJsonPrimitive();
                if (key.getDefault() instanceof Boolean && value.isBoolean()) {
                    entry.setValue(value.getAsBoolean());
                }
                if (key.getDefault() instanceof Integer && value.isNumber()) {
                    entry.setValue(value.getAsNumber().intValue());
                }
                if (key.getDefault() instanceof Double && value.isNumber()) {
                    entry.setValue(value.getAsNumber().doubleValue());
                }
                if (key.getDefault() instanceof String && value.isString()) {
                    entry.setValue(value.getAsString());
                }
            }
        }
    }
    //#else
    //#if MC>=11300
    //$$ public void register() {
    //$$     if (spec == null) {
    //$$         ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    //$$         for (SettingKey<?> key : settings.keySet()) {
    //$$             builder
    //$$                     .translation(key.getDisplayString())
    //$$                     .define(key.getCategory() + "." + key.getKey(), key.getDefault());
    //$$         }
    //$$         spec = builder.build();
    //$$     }
    //$$     FMLJavaModLoadingContext.get().getModEventBus().addListener(this::load);
    //$$     ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, spec);
    //$$ }
    //$$
    //$$ private void load(ModConfig.Loading event) {
    //$$     config = event.getConfig();
    //$$     for (Map.Entry<SettingKey<?>, Object> entry : settings.entrySet()) {
    //$$         SettingKey<?> key = entry.getKey();
    //$$         Object value = config.getConfigData().get(key.getCategory() + "." + key.getKey());
    //$$         entry.setValue(value == null ? key.getDefault() : value);
    //$$     }
    //$$ }
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
        //#if MC>=11400
        settings.put(key, key.getDefault());
        //#else
        //#if MC>=11300
        //$$ if (spec != null) {
        //$$     throw new IllegalStateException("Cannot register more settings are spec has been built.");
        //$$ }
        //$$ settings.put(key, NULL_OBJECT);
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
        //#if MC<11400
        //#if MC>=11300
        //$$ if (config != null) {
        //$$     config.getConfigData().set(key.getCategory() + "." + key.getKey(), value);
        //$$ }
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
        //#endif
        settings.put(key, value);
        SettingsChangedCallback.EVENT.invoker().onSettingsChanged(this, key);
    }

    public void save() {
        //#if MC>=11400
        JsonObject root = new JsonObject();
        for (Map.Entry<SettingKey<?>, Object> entry : settings.entrySet()) {
            SettingKey<?> key = entry.getKey();
            JsonObject category = root.getAsJsonObject(key.getCategory());
            if (category == null) {
                category = new JsonObject();
                root.add(key.getCategory(), category);
            }

            Object value = entry.getValue();
            if (value instanceof Boolean) {
                category.addProperty(key.getKey(), (Boolean) value);
            }
            if (value instanceof Number) {
                category.addProperty(key.getKey(), (Number) value);
            }
            if (value instanceof String) {
                category.addProperty(key.getKey(), (String) value);
            }
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String config = gson.toJson(root);
        try {
            Files.createDirectories(configFile.getParent());
            Files.write(configFile, config.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //#else
        //#if MC>=11300
        //$$ if (config != null) {
        //$$     config.save();
        //$$ }
        //#else
        //$$ configuration.save();
        //#endif
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
