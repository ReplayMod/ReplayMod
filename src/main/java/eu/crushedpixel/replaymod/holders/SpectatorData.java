package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.AdvancedPositionLinearInterpolation;
import eu.crushedpixel.replaymod.interpolation.AdvancedPositionSplineInterpolation;
import eu.crushedpixel.replaymod.interpolation.Interpolation;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class SpectatorData extends AdvancedPosition {

    private int spectatedEntityID;

    public SpectatorData(double x, double y, double z, double pitch, double yaw, double roll, int entityID) {
        super(x, y, z, pitch, yaw, roll);
        this.spectatedEntityID = entityID;
    }

    public SpectatorData(int entityID) {
        this(Minecraft.getMinecraft().theWorld.getEntityByID(entityID));
    }

    public SpectatorData(Entity e) {
        super(e.posX, e.posY, e.posZ, e.rotationPitch, e.rotationYaw);
        this.spectatedEntityID = e.getEntityId();
    }

    @Override
    public SpectatorData newInstance() {
        return new SpectatorData();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new AdvancedPositionSplineInterpolation();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new AdvancedPositionLinearInterpolation();
    }
}
