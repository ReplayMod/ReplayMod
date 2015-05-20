package eu.crushedpixel.replaymod.registry;

import com.google.common.base.Predicate;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.gui.GuiPlayerOverview;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerHandler {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static Predicate<EntityPlayer> playerPredicate = new Predicate<EntityPlayer>() {
        @Override
        public boolean apply(EntityPlayer input) {
            if(input instanceof CameraEntity || input == mc.thePlayer) return false;
            return true;
        }
    };

    private static List<Integer> hidden = new ArrayList<Integer>();

    public static void hidePlayer(EntityPlayer player) {
        hidden.remove((Integer) player.getEntityId());
        hidden.add(player.getEntityId());
    }

    public static void showPlayer(EntityPlayer player) {
        hidden.remove((Integer) player.getEntityId());
    }

    public static void setIsVisible(EntityPlayer player, boolean visible) {
        if(visible) showPlayer(player);
        else hidePlayer(player);
    }

    public static List<Integer> getHiddenPlayers() {
        return hidden;
    }

    public static boolean isHidden(int id) {
        return hidden.contains(id);
    }

    public static void resetHiddenPlayers() {
        for(int i : hidden) {
            Entity ent = mc.theWorld.getEntityByID(i);
            if(ent != null) {
                ent.setInvisible(false);
            }
        }
    }

    public static void openPlayerOverview() {
        if(!ReplayHandler.isInReplay()) {
            return;
        }

        List<EntityPlayer> players = mc.theWorld.getEntities(EntityPlayer.class, playerPredicate);
        mc.displayGuiScreen(new GuiPlayerOverview(players));
    }
}
