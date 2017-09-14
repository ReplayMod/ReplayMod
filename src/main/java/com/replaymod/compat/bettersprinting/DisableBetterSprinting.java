package com.replaymod.compat.bettersprinting;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayChatMessageEvent;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.Restriction;
import cpw.mods.fml.common.versioning.VersionRange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IWorldAccess;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;

/**
 * Old Better Sprinting versions replace the vanilla player with their own, overridden instance (replacing the camera entity).
 *
 * See: https://github.com/chylex/Better-Sprinting/blob/1.8/src/main/java/chylex/bettersprinting/client/player/impl/LogicImplOverride.java
 */
public class DisableBetterSprinting {
    private static final VersionRange OLD_VERSION = VersionRange.newRange(null,
            Collections.singletonList(new Restriction(null, false, new DefaultArtifactVersion("2.0.0"), false)));
    private static final String LOGIC_CLASS_NAME = "chylex.bettersprinting.client.player.impl.LogicImplOverride";
    private static final String CONTROLLER_OVERRIDE_CLASS_NAME = LOGIC_CLASS_NAME + ".PlayerControllerMPOverride";

    public static void register() {
        Loader.instance().getModList().stream()
                .filter(mod -> mod.getModId().equalsIgnoreCase("bettersprinting"))
                .findFirst()
                .map(ModContainer::getProcessedVersion).filter(OLD_VERSION::containsVersion)
                .ifPresent($_ -> MinecraftForge.EVENT_BUS.register(new DisableBetterSprinting()));
    }

    private DisableBetterSprinting() {}

    private final Minecraft mc = Minecraft.getMinecraft();
    private PlayerControllerMP originalController;
    private BetterSprintingWorldAccess worldAccessHook = new BetterSprintingWorldAccess();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void beforeGuiOpenEvent(GuiOpenEvent event) {
        if (ReplayModReplay.instance.getReplayHandler() != null && mc.theWorld != null) {
            // During replay, get ready to revert BetterSprinting's overwritten playerController
            originalController = mc.playerController;
            mc.theWorld.addWorldAccess(worldAccessHook);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void afterGuiOpenEvent(GuiOpenEvent event) {
        if (ReplayModReplay.instance.getReplayHandler() != null && mc.theWorld != null) {
            mc.theWorld.removeWorldAccess(worldAccessHook);
        }
    }

    @SubscribeEvent
    public void onReplayChatMessage(ReplayChatMessageEvent event) {
        // Suppress this message if it's the Better Sprinting warning message
        for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
            if (LOGIC_CLASS_NAME.equals(elem.getClassName())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private class BetterSprintingWorldAccess implements IWorldAccess {
        @Override
        public void onEntityDestroy(Entity entityIn) {
            if (mc.playerController != null && mc.playerController.getClass().getName().equals(CONTROLLER_OVERRIDE_CLASS_NAME)) {
                // Someone has secretly swapped out the player controller and is about to substitute their own player entity.
                // This is the right time to destroy their plan.
                mc.playerController = originalController;
            }
        }

        @Override public void markBlockForUpdate(int x, int y, int z) {}
        @Override public void markBlockForRenderUpdate(int p_147588_1_, int p_147588_2_, int p_147588_3_) {}
        @Override public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}
        @Override public void playSound(String soundName, double x, double y, double z, float volume, float pitch) {}
        @Override public void playSoundToNearExcept(EntityPlayer except, String soundName, double x, double y, double z, float volume, float pitch) {}
        @Override public void spawnParticle(String p_180442_1_, double p_180442_2_, double p_180442_3_, double p_180442_5_, double p_180442_7_, double p_180442_9_, double p_180442_11_) {}
        @Override public void onEntityCreate(Entity p_72703_1_) {}
        @Override public void playRecord(String recordName, int x, int y, int z) {}
        @Override public void broadcastSound(int p_180440_1_, int x, int y, int z, int p_180440_3_) {}
        @Override public void playAuxSFX(EntityPlayer p_72706_1_, int p_72706_2_, int p_72706_3_, int p_72706_4_, int p_72706_5_, int p_72706_6_) {}
        @Override public void destroyBlockPartially(int p_147587_1_, int p_147587_2_, int p_147587_3_, int p_147587_4_, int p_147587_5_) {}
        @Override public void onStaticEntitiesChanged() {}
    }
}
