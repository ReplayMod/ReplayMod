package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameData;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModCompat {
    @SuppressWarnings("unchecked")
    public static Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModContainer> ignoreCaseMap = Loader.instance().getModList().stream()
                .collect(Collectors.toMap(m -> m.getModId().toLowerCase(), Function.identity()));
        return Stream.concat(
                ((Set<String>) GameData.getBlockRegistry().getKeys()).stream(),
                ((Set<String>) GameData.getItemRegistry().getKeys()).stream()
        ).map(ModCompat::getResourceDomain).filter(s -> !s.equals("minecraft")).distinct()
                .map(String::toLowerCase).map(ignoreCaseMap::get).filter(mod -> mod != null)
                .map(mod -> new ModInfo(mod.getModId(), mod.getName(), mod.getVersion()))
                .collect(Collectors.toList());
    }

    private static String getResourceDomain(String name) {
        if (!name.contains(":")) return null; // Still using old names without namespace, can't do anything, ignore
        return name.split(":", 2)[0];
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
