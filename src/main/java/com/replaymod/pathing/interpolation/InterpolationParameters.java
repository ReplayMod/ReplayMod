package com.replaymod.pathing.interpolation;

import lombok.Data;

/**
 * Parameters which can be used to make an interpolation more fitting to the previous one.
 */
@Data
public class InterpolationParameters {
    /**
     * The final value.
     * Should be the same as for the final property.
     * If it differs though, this value should be preferred.
     */
    private final double value;

    /**
     * Velocity at the end of the interpolation.
     * Normally this is the first derivative of the interpolating function at the end of the interpolation.
     */
    private final double velocity;

    /**
     * Acceleration at the end of the interpolation.
     * Normally this is the second derivative of the interpolating function at the end of the interpolation.
     */
    private final double acceleration;
}
