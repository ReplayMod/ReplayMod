package com.replaymod.core;

public interface Module {
    // FMLCommonSetupEvent for 1.13+, FMLInitializationEvent below
    default void initCommon() {}
    // FMLClientSetupEvent for 1.13+, FMLInitializationEvent (if client) below
    default void initClient() {}
    // FMLClientSetupEvent for 1.13+, FMLInitializationEvent below
    default void registerKeyBindings(KeyBindingRegistry registry) {}
}
