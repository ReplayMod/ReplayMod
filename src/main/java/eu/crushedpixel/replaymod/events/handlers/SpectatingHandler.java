package eu.crushedpixel.replaymod.events.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;

public class SpectatingHandler {
    
    private static Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean canSpectate(Entity e) {
        return ((e instanceof EntityPlayer || e instanceof EntityLiving || e instanceof EntityItemFrame) && e != mc.thePlayer);
    } 
}
