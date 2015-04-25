package eu.crushedpixel.replaymod.events;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class MouseInputHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean leftDown = false;
    private boolean rightDown = false;

    @SubscribeEvent
    public void mouseEvent(MouseEvent event) {
        if(Mouse.isButtonDown(0)) {
            if(!leftDown) {
                leftDown = true;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS,
                        new Vec3(0, 0, 0), EnumFacing.NORTH, new BlockPos(0, 0, 0));
            }
        } else {
            leftDown = false;
        }

        if(Mouse.isButtonDown(1)) {
            if(!rightDown) {
                rightDown = true;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS,
                        new Vec3(0, 0, 0), EnumFacing.NORTH, new BlockPos(0, 0, 0));
            }
        } else {
            rightDown = false;
        }
    }
}
