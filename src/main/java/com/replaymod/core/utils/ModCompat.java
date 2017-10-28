package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModCompat {
    @SuppressWarnings("unchecked")
    public static Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModContainer> ignoreCaseMap = Loader.instance().getModList().stream()
                .collect(Collectors.toMap(m -> m.getModId().toLowerCase(), Function.identity()));
        return RegistryManager.ACTIVE.takeSnapshot(false).keySet().stream()
                .map(RegistryManager.ACTIVE::getRegistry)
                .map(ForgeRegistry::getKeys).flatMap(Set::stream)
                .map(ResourceLocation::getResourceDomain).filter(s -> !s.equals("minecraft")).distinct()
                .map(String::toLowerCase).map(ignoreCaseMap::get).filter(mod -> mod != null)
                .map(mod -> new ModInfo(mod.getModId(), mod.getName(), mod.getVersion()))
                .collect(Collectors.toList());
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
