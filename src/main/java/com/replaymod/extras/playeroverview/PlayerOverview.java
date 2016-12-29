package com.replaymod.extras.playeroverview;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.extras.Extra;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.*;

public class PlayerOverview implements Extra {
    private ReplayModReplay module;

    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private boolean savingEnabled;

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.module = ReplayModReplay.instance;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.playeroverview", Keyboard.KEY_B, new Runnable() {
            @Override
            public void run() {
                if (module.getReplayHandler() != null) {
                    @SuppressWarnings("unchecked")
                    List<EntityPlayer> players = mod.getMinecraft().theWorld.getPlayers(EntityPlayer.class, new Predicate() {
                        @Override
                        public boolean apply(Object input) {
                            return !(input instanceof CameraEntity); // Exclude the camera entity
                        }
                    });
                    if (!Utils.isCtrlDown()) {
                        // Hide all players that have an UUID v2 (commonly used for NPCs)
                        Iterator<EntityPlayer> iter = players.iterator();
                        while (iter.hasNext()) {
                            UUID uuid = iter.next().getGameProfile().getId();
                            if (uuid != null && uuid.version() == 2) {
                                iter.remove();
                            }
                        }
                    }
                    new PlayerOverviewGui(PlayerOverview.this, players).display();
                }
            }
        });

        FMLCommonHandler.instance().bus().register(this);

        RenderManager renderManager = mod.getMinecraft().getRenderManager();
        @SuppressWarnings("unchecked")
        Map<String, RenderPlayer> skinMap = renderManager.skinMap;
        skinMap.put("default", new PlayerRenderHook(this, renderManager, false));
        skinMap.put("slim", new PlayerRenderHook(this, renderManager, true));
    }

    public boolean isHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }

    public void setHidden(UUID uuid, boolean hidden) {
        if (hidden) {
            hiddenPlayers.add(uuid);
        } else {
            hiddenPlayers.remove(uuid);
        }
    }

    @SubscribeEvent
    public void onReplayOpen(ReplayOpenEvent.Pre event) throws IOException {
        Optional<Set<UUID>> savedData = event.getReplayHandler().getReplayFile().getInvisiblePlayers();
        if (savedData.isPresent()) {
            hiddenPlayers.addAll(savedData.get());
            savingEnabled = true;
        } else {
            savingEnabled = false;
        }
    }

    @SubscribeEvent
    public void onReplayClose(ReplayCloseEvent.Pre event) throws IOException {
        hiddenPlayers.clear();
    }

    public boolean isSavingEnabled() {
        return savingEnabled;
    }

    public void setSavingEnabled(boolean savingEnabled) {
        this.savingEnabled = savingEnabled;
    }

    public void saveHiddenPlayers() {
        if (savingEnabled) {
            try {
                module.getReplayHandler().getReplayFile().writeInvisiblePlayers(hiddenPlayers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
