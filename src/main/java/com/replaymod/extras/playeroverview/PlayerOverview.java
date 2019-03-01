package com.replaymod.extras.playeroverview;

import com.google.common.base.Optional;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.extras.Extra;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderHandEvent;

//#if MC>=11300
//#else
//$$ import org.lwjgl.input.Keyboard;
//#endif

//#if MC>=10800
import com.google.common.base.Predicate;
//#if MC>=11300
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.EventPriority;
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import net.minecraftforge.client.event.RenderPlayerEvent;
//#endif

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.replaymod.core.versions.MCVer.*;

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
                    //#if MC>=10800
                    List<EntityPlayer> players = world(mod.getMinecraft()).getPlayers(EntityPlayer.class, new Predicate() {
                        @Override
                        public boolean apply(Object input) {
                            return !(input instanceof CameraEntity); // Exclude the camera entity
                        }
                    });
                    //#else
                    //$$ List<EntityPlayer> players = mod.getMinecraft().theWorld.playerEntities;
                    //$$ players = players.stream()
                    //$$         .filter(it -> !(it instanceof CameraEntity)) // Exclude the camera entity
                    //$$         .collect(Collectors.toList());
                    //#endif
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

        FML_BUS.register(this);
        FORGE_BUS.register(this);
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

    @SubscribeEvent
    public void oRenderHand(RenderHandEvent event) {
        Entity view = getRenderViewEntity(module.getCore().getMinecraft());
        if (view != null && isHidden(view.getUniqueID())) {
            event.setCanceled(true);
        }
    }

    // See MixinRender for why this is 1.7.10 only
    //#if MC<=10710
    //$$ @SubscribeEvent(priority = EventPriority.HIGHEST)
    //$$ public void preRenderPlayer(RenderPlayerEvent.Pre event) {
    //$$     if (isHidden(event.entityPlayer.getUniqueID())) {
    //$$         event.setCanceled(true);
    //$$     }
    //$$ }
    //#endif

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
