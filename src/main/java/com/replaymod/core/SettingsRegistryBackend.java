package com.replaymod.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.replaymod.core.events.SettingsChangedCallback;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;

import static com.replaymod.core.versions.MCVer.getMinecraft;

class SettingsRegistryBackend {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<SettingsRegistry.SettingKey<?>, Object> settings;

    private final Path configFile = getMinecraft().runDirectory.toPath().resolve("config/replaymod.json");

    SettingsRegistryBackend(Map<SettingsRegistry.SettingKey<?>, Object> settings) {
        this.settings = settings;
    }

    public void register() {
        load(true);
        try {
            registerWatcher();
        } catch (IOException e) {
            LOGGER.warn("Failed to setup file watcher for {}, live-reloading is disabled. Cause: {}", configFile, e);
        }
    }

    private void load(boolean createIfMissingOrBroken) {
        String config;
        if (Files.exists(configFile)) {
            try {
                config = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            if (createIfMissingOrBroken) {
                save();
            }
            return;
        }
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(config, JsonObject.class);
        if (root == null) {
            LOGGER.error("Config file {} appears corrupted: {}", configFile, config);
            if (createIfMissingOrBroken) {
                save();
            }
            return;
        }
        for (Map.Entry<SettingsRegistry.SettingKey<?>, Object> entry : settings.entrySet()) {
            SettingsRegistry.SettingKey<?> key = entry.getKey();
            JsonElement category = root.get(key.getCategory());
            if (category != null && category.isJsonObject()) {
                JsonElement valueElem = category.getAsJsonObject().get(key.getKey());
                if (valueElem == null || !valueElem.isJsonPrimitive()) continue;
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
                    String valueStr = value.getAsString();
                    if (entry instanceof SettingsRegistry.MultipleChoiceSettingKey) {
                        @SuppressWarnings("unchecked")
                        List<String> choices = ((SettingsRegistry.MultipleChoiceSettingKey<String>) entry).getChoices();
                        if (choices.stream().noneMatch(valueStr::equals)) {
                            continue;
                        }
                    }
                    entry.setValue(valueStr);
                }
            }
        }
    }

    private void registerWatcher() throws IOException {
        WatchService watchService = configFile.getFileSystem().newWatchService();
        configFile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        Thread thread = new Thread(() -> {
            while (true) {
                WatchKey nextKey;
                try {
                    nextKey = watchService.take();
                } catch (InterruptedException e) {
                    return;
                }
                for (WatchEvent<?> event : nextKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Path fileName = ((Path) event.context());
                    if (fileName.equals(configFile.getFileName())) {
                        MinecraftClient.getInstance().send(this::reload);
                    }
                }
                if (!nextKey.reset()) {
                    return;
                }
            }
        });
        thread.setName("replaymod-config-watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private void reload() {
        load(false);

        SettingsRegistry settingsRegistry = ReplayMod.instance.getSettingsRegistry();
        for (SettingsRegistry.SettingKey<?> key : settings.keySet()) {
            SettingsChangedCallback.EVENT.invoker().onSettingsChanged(settingsRegistry, key);
        }
    }

    @SuppressWarnings("unused")
    public void register(SettingsRegistry.SettingKey<?> key) {
    }

    @SuppressWarnings("unused")
    public <T> void update(SettingsRegistry.SettingKey<T> key, T value) {
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<SettingsRegistry.SettingKey<?>, Object> entry : settings.entrySet()) {
            SettingsRegistry.SettingKey<?> key = entry.getKey();
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
                if (key instanceof SettingsRegistry.MultipleChoiceSettingKey) {
                    @SuppressWarnings("unchecked")
                    List<String> choices = ((SettingsRegistry.MultipleChoiceSettingKey<String>) key).getChoices();
                    JsonArray array = new JsonArray();
                    for (String choice : choices) {
                        array.add(choice);
                    }
                    category.add(key.getKey() + "_valid_values", array);
                }
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
    }
}
