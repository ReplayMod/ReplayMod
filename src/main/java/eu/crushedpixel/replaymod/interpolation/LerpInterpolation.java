package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;

import java.util.ArrayList;
import java.util.List;

public class LerpInterpolation extends GenericLinearInterpolation<AdvancedPosition> {

    private List<Quaternion> quaternions = new ArrayList<Quaternion>();

    @Override
    public void prepare() {
        for(AdvancedPosition position : points) {
            Rotation rot = new Rotation(RotationOrder.XYZ,
                    position.getYaw(), position.getPitch(), position.getRoll());
            quaternions.add(new Quaternion(rot.getQ0(), rot.getQ1(), rot.getQ2(), rot.getQ3()));
        }
        super.prepare();
    }

    @Override
    public void applyPoint(float position, AdvancedPosition toEdit) {
        super.applyPoint(position, toEdit);

        Quaternion quaternion = new Quaternion();

        //first, get previous and next Quaternion for given position
        float relative = position * (quaternions.size()-1);
        int previousIndex = (int)Math.floor(relative);
        int nextIndex = (int)Math.ceil(relative);
        float percentage = relative - previousIndex;

        Quaternion previous = quaternions.get(previousIndex);
        Quaternion next = quaternions.get(nextIndex);

        //interpolate between these Quaternions using the Lerp Algorithm

        double c1 = 1.0f - percentage;
        double c2 = percentage;

        quaternion.setX(c1 * previous.getX() + c2 * next.getX());
        quaternion.setY(c1 * previous.getY() + c2 * next.getY());
        quaternion.setZ(c1 * previous.getZ() + c2 * next.getZ());
        quaternion.setW(c1 * previous.getW() + c2 * next.getW());

        Rotation rotation = new Rotation(quaternion.getW(), quaternion.getX(),
                quaternion.getY(), quaternion.getZ(), false);

        double[] angles = rotation.getAngles(RotationOrder.XYZ);
        toEdit.setYaw(angles[0]);
        toEdit.setPitch(angles[1]);
        toEdit.setRoll(angles[2]);
    }
}
