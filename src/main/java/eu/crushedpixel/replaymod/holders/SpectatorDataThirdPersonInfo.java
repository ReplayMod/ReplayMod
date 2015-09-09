package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class SpectatorDataThirdPersonInfo implements KeyframeValue {

    /**
     * The shoulder camera's distance (in blocks) to the player
     */
    @Interpolate
    public double shoulderCamDistance = 3;

    /**
     * The shoulder camera's pitch offset in degrees, value between -90 and 90
     */
    @Interpolate
    public double shoulderCamPitchOffset = 0;

    /**
     * The shoulder camera's rotation around the player in degrees
     */
    @Interpolate
    public double shoulderCamYawOffset = 0;

    /**
     * The distance between the automatically calculated position keyframes in seconds
     */
    @Interpolate
    public double shoulderCamSmoothness = 1;

    @Override
    public KeyframeValue newInstance() {
        return new SpectatorDataThirdPersonInfo();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new GenericLinearInterpolation<SpectatorDataThirdPersonInfo>();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new GenericSplineInterpolation<SpectatorDataThirdPersonInfo>();
    }
}
