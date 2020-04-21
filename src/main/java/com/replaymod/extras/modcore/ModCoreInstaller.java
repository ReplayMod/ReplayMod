/* Based on https://github.com/Sk1erLLC/ModCore-Example-Mod/blob/4a362fa1c67cee28bdfc82bd5a99a30896b081fb/src/main/java/club/sk1er/mods/modcoreexample/ModCoreInstaller.java
 *
 * MIT License
 *
 * Copyright (c) 2020 Sk1er LLC
 * Copyright (c) 2020 ModCore Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.replaymod.extras.modcore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

//#if FABRIC>=1
import net.fabricmc.loader.api.FabricLoader;
//#else
//#if MC>=11400
//#else
//$$ import net.minecraft.launchwrapper.Launch;
//$$ import java.net.URLClassLoader;
//$$ import java.util.LinkedHashSet;
//#endif
//#endif

public class ModCoreInstaller {

    private static final String VERSION_URL = "https://api.sk1er.club/modcore_versions";
    private static final String className = "club.sk1er.mods.core.tweaker.ModCoreTweaker";
    private static boolean errored = false;
    private static String error;
    private static File dataDir = null;


    private static boolean isInitalized() {
        try {
            //#if MC<11400
            //$$ LinkedHashSet<String> objects = new LinkedHashSet<>();
            //$$ objects.add(className);
            //$$ Launch.classLoader.clearNegativeEntries(objects);
            //#endif
            return Class.forName(className) != null;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isErrored() {
        return errored;
    }

    public static String getError() {
        return error;
    }

    private static void bail(String error) {
        errored = true;
        ModCoreInstaller.error = error;
    }

    private static JsonHolder readFile(File in) {
        try {
            return new JsonHolder(FileUtils.readFileToString(in));
        } catch (IOException ignored) {

        }
        return new JsonHolder();
    }

    private static boolean initializeModCore(File gameDir) {
        try {
            Class<?> modCore = Class.forName("club.sk1er.mods.core.tweaker.ModCoreTweaker");
            Method initialize = modCore.getMethod("initialize", File.class);
            initialize.invoke(null, gameDir);
            System.out.println("Loaded ModCore Successfully");
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        System.out.println("Did NOT initialize ModCore Successfully");
        return false;
    }

    public static int initialize(File gameDir, String minecraftVersion) {
        if (isInitalized()) return -1;
        dataDir = new File(gameDir, "modcore");
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                bail("Unable to create necessary files");
                return 1;
            }
        }
        JsonHolder jsonHolder = fetchJSON(VERSION_URL);
        if (!jsonHolder.has(minecraftVersion)) {
            System.out.println("No ModCore target for " + minecraftVersion + ". Aborting install");
            return -2;
        }
        String latestRemote = jsonHolder.optString(minecraftVersion);
        boolean failed = jsonHolder.getKeys().size() == 0 || (jsonHolder.has("success") && !jsonHolder.optBoolean("success"));

        File metadataFile = new File(dataDir, "metadata.json");
        JsonHolder localMetadata = readFile(metadataFile);
        if (failed) latestRemote = localMetadata.optString(minecraftVersion);
        File modcoreFile = new File(dataDir, "Sk1er Modcore-" + latestRemote + " (" + minecraftVersion + ").jar");

        if (!modcoreFile.exists() || !localMetadata.optString(minecraftVersion).equalsIgnoreCase(latestRemote) && !failed) {
            //File does not exist, or is out of date, download it
            File old = new File(dataDir, "Sk1er Modcore-" + localMetadata.optString(minecraftVersion) + " (" + minecraftVersion + ").jar");
            if (old.exists()) old.delete();

            if (!download("https://static.sk1er.club/repo/mods/modcore/" + latestRemote + "/" + minecraftVersion + "/ModCore-" + latestRemote + " (" + minecraftVersion + ").jar", latestRemote, modcoreFile, minecraftVersion, localMetadata)) {
                bail("Unable to download");
                return 2;
            }

        }

        addToClasspath(modcoreFile);

        if (!isInitalized()) {
            bail("Something went wrong and it did not add the jar to the class path. Local file exists? " + modcoreFile.exists());
            return 3;
        }
        return initializeModCore(gameDir) ? 0 : 4;

    }


    public static void addToClasspath(File file) {
        try {
            URL url = file.toURI().toURL();
            ClassLoader classLoader = ModCoreInstaller.class.getClassLoader();
            //#if MC>=11400
            Method method = classLoader.getClass().getDeclaredMethod("addURL", URL.class);
            //#else
            //$$ Launch.classLoader.addURL(url);
            //$$ Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            //#endif
            method.setAccessible(true);
            method.invoke(classLoader, url);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    private static boolean download(String url, String version, File file, String mcver, JsonHolder versionData) {
        url = url.replace(" ", "%20");
        System.out.println("Downloading ModCore " + " version " + version + " from: " + url);
        JFrame frame = new JFrame("ModCore Initializer");
        JProgressBar bar = new JProgressBar();
        TextArea comp = new TextArea("", 1, 1, TextArea.SCROLLBARS_NONE);
        frame.getContentPane().add(comp);
        frame.getContentPane().add(bar);
        GridLayout manager = new GridLayout();
        frame.setLayout(manager);
        manager.setColumns(1);
        manager.setRows(2);
        comp.setText("Downloading Sk1er ModCore Library Version " + version + " for Minecraft " + mcver);
        comp.setSize(399, 80);
        comp.setEditable(false);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();


        Dimension preferredSize = new Dimension(400, 225);
        bar.setSize(preferredSize);
        frame.setSize(preferredSize);
        frame.setResizable(false);
        bar.setBorderPainted(true);
        bar.setMinimum(0);
        bar.setStringPainted(true);
        frame.setVisible(true);
        frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);
        Font font = bar.getFont();
        bar.setFont(new Font(font.getName(), font.getStyle(), font.getSize() * 4));
        comp.setFont(new Font(font.getName(), font.getStyle(), font.getSize() * 2));

        try {

            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(true);
            connection.addRequestProperty("User-Agent", "Mozilla/4.76 (Sk1er Modcore Initializer)");
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDoOutput(true);
            InputStream is = connection.getInputStream();
            int contentLength = connection.getContentLength();
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            System.out.println("MAX: " + contentLength);
            bar.setMaximum(contentLength);
            int read;
            bar.setValue(0);
            while ((read = is.read(buffer)) > 0) {
                outputStream.write(buffer, 0, read);
                bar.setValue(bar.getValue() + 1024);
            }
            outputStream.close();
            FileUtils.write(new File(dataDir, "metadata.json"), versionData.put(mcver, version).toString());
        } catch (Exception e) {
            e.printStackTrace();
            frame.dispose();
            return false;
        }
        frame.dispose();
        return true;
    }

    public static final JsonHolder fetchJSON(String url) {
        return new JsonHolder(fetchString(url));
    }

    public static final String fetchString(String url) {
        url = url.replace(" ", "%20");
        System.out.println("Fetching " + url);
        try {
            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(true);
            connection.addRequestProperty("User-Agent", "Mozilla/4.76 (Sk1er ModCore)");
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDoOutput(true);
            InputStream is = connection.getInputStream();
            return IOUtils.toString(is, Charset.defaultCharset());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Failed to fetch";
    }


    //Added because we need to use before ModCore is loaded
    static class JsonHolder {
        private JsonObject object;

        public JsonHolder(JsonObject object) {
            this.object = object;
        }

        public JsonHolder(String raw) {
            if (raw == null)
                object = new JsonObject();
            else
                try {
                    this.object = new JsonParser().parse(raw).getAsJsonObject();
                } catch (Exception e) {
                    this.object = new JsonObject();
                    e.printStackTrace();
                }
        }

        public JsonHolder() {
            this(new JsonObject());
        }

        @Override
        public String toString() {
            if (object != null)
                return object.toString();
            return "{}";
        }

        public JsonHolder put(String key, boolean value) {
            object.addProperty(key, value);
            return this;
        }

        public void mergeNotOverride(JsonHolder merge) {
            merge(merge, false);
        }

        public void mergeOverride(JsonHolder merge) {
            merge(merge, true);
        }

        public void merge(JsonHolder merge, boolean override) {
            JsonObject object = merge.getObject();
            for (String s : merge.getKeys()) {
                if (override || !this.has(s))
                    put(s, object.get(s));
            }
        }

        private void put(String s, JsonElement element) {
            this.object.add(s, element);
        }

        public JsonHolder put(String key, String value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonHolder put(String key, int value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonHolder put(String key, double value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonHolder put(String key, long value) {
            object.addProperty(key, value);
            return this;
        }

        private JsonHolder defaultOptJSONObject(String key, JsonObject fallBack) {
            try {
                return new JsonHolder(object.get(key).getAsJsonObject());
            } catch (Exception e) {
                return new JsonHolder(fallBack);
            }
        }

        public JsonArray defaultOptJSONArray(String key, JsonArray fallback) {
            try {
                return object.get(key).getAsJsonArray();
            } catch (Exception e) {
                return fallback;
            }
        }

        public JsonArray optJSONArray(String key) {
            return defaultOptJSONArray(key, new JsonArray());
        }


        public boolean has(String key) {
            return object.has(key);
        }

        public long optLong(String key, long fallback) {
            try {
                return object.get(key).getAsLong();
            } catch (Exception e) {
                return fallback;
            }
        }

        public long optLong(String key) {
            return optLong(key, 0);
        }

        public boolean optBoolean(String key, boolean fallback) {
            try {
                return object.get(key).getAsBoolean();
            } catch (Exception e) {
                return fallback;
            }
        }

        public boolean optBoolean(String key) {
            return optBoolean(key, false);
        }

        public JsonObject optActualJSONObject(String key) {
            try {
                return object.get(key).getAsJsonObject();
            } catch (Exception e) {
                return new JsonObject();
            }
        }

        public JsonHolder optJSONObject(String key) {
            return defaultOptJSONObject(key, new JsonObject());
        }


        public int optInt(String key, int fallBack) {
            try {
                return object.get(key).getAsInt();
            } catch (Exception e) {
                return fallBack;
            }
        }

        public int optInt(String key) {
            return optInt(key, 0);
        }


        public String defaultOptString(String key, String fallBack) {
            try {
                return object.get(key).getAsString();
            } catch (Exception e) {
                return fallBack;
            }
        }

        public String optString(String key) {
            return defaultOptString(key, "");
        }


        public double optDouble(String key, double fallBack) {
            try {
                return object.get(key).getAsDouble();
            } catch (Exception e) {
                return fallBack;
            }
        }

        public List<String> getKeys() {
            List<String> tmp = new ArrayList<>();
            object.entrySet().forEach(e -> tmp.add(e.getKey()));
            return tmp;
        }

        public double optDouble(String key) {
            return optDouble(key, 0.0);
        }


        public JsonObject getObject() {
            return object;
        }

        public boolean isNull(String key) {
            return object.has(key) && object.get(key).isJsonNull();
        }

        public JsonHolder put(String values, JsonHolder values1) {
            return put(values, values1.getObject());
        }

        public JsonHolder put(String values, JsonObject object) {
            this.object.add(values, object);
            return this;
        }

        public void put(String blacklisted, JsonArray jsonElements) {
            this.object.add(blacklisted, jsonElements);
        }

        public void remove(String header) {
            object.remove(header);
        }
    }


}

