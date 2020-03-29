package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;
import net.minecraft.util.Identifier;

//#if FABRIC>=1
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.registry.Registry;
//#else
//#if MC>=11200
//$$ import net.minecraftforge.registries.ForgeRegistry;
//$$ import net.minecraftforge.registries.RegistryManager;
//#else
//$$ import net.minecraftforge.fml.common.registry.GameData;
//$$ import java.util.stream.Stream;
//#endif
//$$
//#if MC>=11400
//$$ import net.minecraftforge.fml.ModList;
//#else
//$$ import net.minecraftforge.fml.common.Loader;
//$$ import net.minecraftforge.fml.common.ModContainer;
//#endif
//#endif

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModCompat {
    @SuppressWarnings("unchecked")
    public static Collection<ModInfo> getInstalledNetworkMods() {
        //#if MC>=11400
        //#if FABRIC>=1
        Map<String, ModInfo> modInfoMap = FabricLoader.getInstance().getAllMods().stream()
                .map(ModContainer::getMetadata)
                .map(m -> new ModInfo(m.getId(), m.getName(), m.getVersion().toString()))
                .collect(Collectors.toMap(ModInfo::getId, Function.identity()));
        return Registry.REGISTRIES.stream()
                .map(Registry::getIds).flatMap(Set::stream)
        //#else
        //$$ Map<String, ModInfo> modInfoMap = ModList.get().getMods().stream()
        //$$         .map(m -> new ModInfo(m.getModId(), m.getDisplayName(), m.getVersion().toString()))
        //$$         .collect(Collectors.toMap(ModInfo::getId, Function.identity()));
        //$$ return RegistryManager.ACTIVE.takeSnapshot(false).keySet().stream()
        //$$         .map(RegistryManager.ACTIVE::getRegistry)
        //$$         .map(ForgeRegistry::getKeys).flatMap(Set::stream)
        //#endif
                .map(Identifier::getNamespace).filter(s -> !s.equals("minecraft")).distinct()
                .map(modInfoMap::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
        //#else
        //$$ Map<String, ModContainer> ignoreCaseMap = Loader.instance().getModList().stream()
        //$$         .collect(Collectors.toMap(m -> m.getModId().toLowerCase(), Function.identity()));
        //#if MC>=11200
        //$$ return RegistryManager.ACTIVE.takeSnapshot(false).keySet().stream()
        //$$         .map(RegistryManager.ACTIVE::getRegistry)
        //$$         .map(ForgeRegistry::getKeys).flatMap(Set::stream)
        //$$         .map(ResourceLocation::getResourceDomain).filter(s -> !s.equals("minecraft")).distinct()
        //$$         .map(String::toLowerCase).map(ignoreCaseMap::get).filter(mod -> mod != null)
        //$$         .map(mod -> new ModInfo(mod.getModId(), mod.getName(), mod.getVersion()))
        //$$         .collect(Collectors.toList());
        //#else
        //#if MC>=10800
        //$$ return Stream.concat(
        //$$         ((Set<ResourceLocation>) GameData.getBlockRegistry().getKeys()).stream(),
        //$$         ((Set<ResourceLocation>) GameData.getItemRegistry().getKeys()).stream()
        //$$ ).map(ResourceLocation::getResourceDomain).filter(s -> !s.equals("minecraft")).distinct()
        //$$         .map(String::toLowerCase).map(ignoreCaseMap::get).filter(mod -> mod != null)
        //$$         .map(mod -> new ModInfo(mod.getModId(), mod.getName(), mod.getVersion()))
        //$$         .collect(Collectors.toList());
        //#else
        //$$ return Stream.concat(
        //$$         ((Set<String>) GameData.getBlockRegistry().getKeys()).stream(),
        //$$         ((Set<String>) GameData.getItemRegistry().getKeys()).stream()
        //$$ ).map(name -> {
        //$$     if (!name.contains(":")) return null; // Still using old names without namespace, can't do anything, ignore
        //$$     return name.split(":", 2)[0];
        //$$ }).filter(s -> !s.equals("minecraft")).distinct()
        //$$         .map(String::toLowerCase).map(ignoreCaseMap::get).filter(mod -> mod != null)
        //$$         .map(mod -> new ModInfo(mod.getModId(), mod.getName(), mod.getVersion()))
        //$$         .collect(Collectors.toList());
        //#endif
        //#endif
        //#endif
    }

    public static final class ModInfoDifference {
        private final Set<ModInfo> missing = new HashSet<>();
        private final Map<ModInfo, String> differing = new HashMap<>();
        public ModInfoDifference(Collection<ModInfo> requiredList) {
            Collection<ModInfo> installedList = getInstalledNetworkMods();
            REQUIRED:
            for (ModInfo required : requiredList) {
                for (ModInfo installed : installedList) {
                    if (required.getId().equals(installed.getId())) {
                        // Mod is installed, check if versions match
                        if (Objects.equals(required.getVersion(), installed.getVersion())) {
                            // Mod found and version match
                            continue REQUIRED;
                        } else {
                            // Mod found but versions don't match
                            differing.put(required, installed.getVersion());
                            continue REQUIRED;
                        }
                    }
                }
                // Mod no longer installed
                missing.add(required);
            }
        }

        public Set<ModInfo> getMissing() {
            return Collections.unmodifiableSet(missing);
        }

        public Map<ModInfo, String> getDiffering() {
            return Collections.unmodifiableMap(differing);
        }
    }
}
