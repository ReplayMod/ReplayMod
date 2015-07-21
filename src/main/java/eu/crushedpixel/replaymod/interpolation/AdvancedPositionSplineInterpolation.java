package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.utils.InterpolationUtils;

public class AdvancedPositionSplineInterpolation extends GenericSplineInterpolation<AdvancedPosition> {

    @Override
    public void addPoint(AdvancedPosition point) {
        double normalizedYaw = (point.getYaw() + 180) % 360;
        double normalizedRoll = (point.getRoll()) % 360;

        if(!points.isEmpty()) {
            AdvancedPosition last = points.get(points.size()-1);

            double yaw = InterpolationUtils.fixEulerRotation(last.getYaw(), point.getYaw(), 180);
            double roll = InterpolationUtils.fixEulerRotation(last.getRoll(), point.getRoll(), 0);

            point.setYaw(yaw);
            point.setRoll(roll);
        } else {
            point.setYaw(normalizedYaw-180);
            point.setRoll(normalizedRoll);
        }

        super.addPoint(point);
    }
}
