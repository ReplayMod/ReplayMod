package eu.crushedpixel.replaymod.events;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class MouseInputHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean rightDown = false;

    @SubscribeEvent
    public void mouseEvent(MouseEvent event) {
        if(!ReplayHandler.isInReplay() || !Mouse.isButtonDown(1)) {
            rightDown = false;
            return;
        }

        if(Mouse.isButtonDown(1)) {
            if(!rightDown) {
                rightDown = true;
                if(mc.pointedEntity != null && ReplayHandler.isCamera() && mc.currentScreen == null) {
                    ReplayHandler.spectateEntity(mc.pointedEntity);
                }
            }
        } else {
            rightDown = false;
        }
    }
}
