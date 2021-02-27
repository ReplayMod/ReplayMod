package com.replaymod.core;

import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.versions.forge.EventsAdapter;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.List;

import static com.replaymod.core.ReplayMod.MOD_ID;
import static com.replaymod.core.ReplayMod.jGuiResourcePack;
import static com.replaymod.core.versions.MCVer.getMinecraft;

//#if MC<=10710
//$$ import net.minecraft.client.resources.FolderResourcePack;
//$$ import java.io.ByteArrayInputStream;
//$$ import java.io.File;
//$$ import java.io.IOException;
//$$ import java.io.InputStream;
//$$ import java.nio.charset.StandardCharsets;
//#endif

@Mod(modid = ReplayMod.MOD_ID,
        useMetadata = true,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        //#if MC>=10800
        clientSideOnly = true,
        updateJSON = "https://raw.githubusercontent.com/ReplayMod/ReplayMod/master/versions.json",
        //#endif
        guiFactory = "com.replaymod.core.gui.GuiFactory")
public class ReplayModBackend {
    private final ReplayMod mod = new ReplayMod(this);
    private final EventsAdapter eventsAdapter = new EventsAdapter();

    @Deprecated
    public static Configuration config;

    @EventHandler
    public void init(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
        settingsRegistry.backend.setConfiguration(config);
        settingsRegistry.save(); // Save default values to disk
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        mod.initModules();
        eventsAdapter.register();
    }

    public String getVersion() {
        return Loader.instance().getIndexedModList().get(MOD_ID).getVersion();
    }

    public String getMinecraftVersion() {
        return Loader.MC_VERSION;
    }

    public boolean isModLoaded(String id) {
        return Loader.isModLoaded(id);
    }

    static { // Note: even preInit is too late and we'd have to issue another resource reload
        List<IResourcePack> defaultResourcePacks = ((MinecraftAccessor) getMinecraft()).getDefaultResourcePacks();

        if (jGuiResourcePack != null) {
            defaultResourcePacks.add(jGuiResourcePack);
        }

        //#if MC<=10710
        //$$ FolderResourcePack mainResourcePack = new FolderResourcePack(new File("../src/main/resources")) {
        //$$     @Override
        //$$     protected InputStream getInputStreamByName(String resourceName) throws IOException {
        //$$         try {
        //$$             return super.getInputStreamByName(resourceName);
        //$$         } catch (IOException e) {
        //$$             if ("pack.mcmeta".equals(resourceName)) {
        //$$                 return new ByteArrayInputStream(("{\"pack\": {\"description\": \"dummy pack for mod resources in dev-env\", \"pack_format\": 1}}").getBytes(StandardCharsets.UTF_8));
        //$$             }
        //$$             throw e;
        //$$         }
        //$$     }
        //$$ };
        //$$ defaultResourcePacks.add(mainResourcePack);
        //#endif
    }
}
