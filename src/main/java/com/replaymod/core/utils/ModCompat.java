package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;

import java.util.*;

public class ModCompat {
    public static Collection<ModInfo> getInstalledNetworkMods() {
        return ModInfoGetter.getInstalledNetworkMods();
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
