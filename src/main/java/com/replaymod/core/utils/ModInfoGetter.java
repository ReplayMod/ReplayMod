package com.replaymod.core.utils;
import com.replaymod.replaystudio.data.ModInfo;
import net.minecraft.util.Identifier;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModInfo> modInfoMap = FabricLoader.getInstance().getAllMods().stream()
                .map(ModContainer::getMetadata)
                .map(m -> new ModInfo(m.getId(), m.getName(), m.getVersion().toString()))
                .collect(Collectors.toMap(ModInfo::getId, Function.identity()));
        return Registry.REGISTRIES.stream()
                .map(Registry::getIds).flatMap(Set::stream)
                .map(Identifier::getNamespace).filter(s -> !s.equals("minecraft")).distinct()
                .map(modInfoMap::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
