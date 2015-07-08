package eu.crushedpixel.replaymod.interpolation;

/**
 * An abstract class that GenericSplineInterpolation can process.
 * Subclasses simply have to annotate at least one field with @Interpolate,
 * and the GenericSplineInterpolation will interpolate these.
 * <br><br>
 * It is recommended for KeyframeValue subclasses to have a @NoArgsConstructor annotation.
 */
public interface KeyframeValue {

    public KeyframeValue newInstance();

}
