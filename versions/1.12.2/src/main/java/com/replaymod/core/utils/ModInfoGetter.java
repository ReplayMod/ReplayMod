package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModContainer> ignoreCaseMap = Loader.instance().getModList().stream()
                .collect(Collectors.toMap(m -> m.getModId().toLowerCase(), Function.identity()));
        return RegistryManager.ACTIVE.takeSnapshot(false).keySet().stream()
                .map(RegistryManager.ACTIVE::getRegistry)
                .map(ForgeRegistry::getKeys).flatMap(Set::stream)
                .map(ResourceLocation::getResourceDomain).filter(s -> !s.equals("minecraft")).distinct()
                .map(String::toLowerCase).map(ignoreCaseMap::get).filter(Objects::nonNull)
                .map(mod -> new ModInfo(mod.getModId(), mod.getName(), mod.getVersion()))
                .collect(Collectors.toList());
    }
}
