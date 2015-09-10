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

    private Integer spectatedEntityID;

    private SpectatingMethod spectatingMethod = SpectatingMethod.FIRST_PERSON;

    private SpectatorDataThirdPersonInfo thirdPersonInfo = new SpectatorDataThirdPersonInfo();

    public int getSpectatedEntityID() {
        if(spectatedEntityID == null) throw new IllegalStateException();
        return spectatedEntityID;
    }

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

    /**
     * @return itself if it's a valid SpectatorData object,
     * otherwise a new AdvancedPosition object containing the same position data
     */
    public AdvancedPosition normalize() {
        if(spectatedEntityID != null) return this;
        return new AdvancedPosition(x, y, z, pitch, yaw, roll);
    }

    @Override
    public AdvancedPosition newInstance() {
        return new AdvancedPosition();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new AdvancedPositionSplineInterpolation();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new AdvancedPositionLinearInterpolation();
    }

    public enum SpectatingMethod {
        FIRST_PERSON(0xFF0080FF), SHOULDER_CAM(0xFFFF8000);

        private int color;

        public int getColor() {
            return color;
        }

        SpectatingMethod(int color) {
            this.color = color;
        }
    }
}
