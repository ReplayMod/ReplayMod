package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameData;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModContainer> ignoreCaseMap = Loader.instance().getModList().stream()
                .collect(Collectors.toMap(m -> m.getModId().toLowerCase(), Function.identity()));
        return Stream.concat(
                ((Set<String>) GameData.getBlockRegistry().getKeys()).stream(),
                ((Set<String>) GameData.getItemRegistry().getKeys()).stream()
        ).map(name -> {
            if (!name.contains(":")) return null; // Still using old names without namespace, can't do anything, ignore
            return name.split(":", 2)[0];
        }).filter(Objects::nonNull)
                .filter(s -> !s.equals("minecraft")).distinct()
                .map(String::toLowerCase).map(ignoreCaseMap::get).filter(Objects::nonNull)
                .map(mod -> new ModInfo(mod.getModId(), mod.getName(), mod.getVersion()))
                .collect(Collectors.toList());
    }
}
