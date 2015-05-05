package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.Position;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

public class SplinePoint extends BasicSpline {
    private static final Object[] EMPTYOBJ = new Object[]{};
    private Vector<Position> points;
    private Vector<Cubic> xCubics;
    private Vector<Cubic> yCubics;
    private Vector<Cubic> zCubics;
    private Vector<Cubic> pitchCubics;
    private Vector<Cubic> yawCubics;
    private Vector<Cubic> rotCubics;
    private Field vectorX;
    private Field vectorY;
    private Field vectorZ;
    private Field vectorPitch;
    private Field vectorYaw;
    private Field vectorRot;

    public SplinePoint() {
        this.points = new Vector<Position>();

        this.xCubics = new Vector<Cubic>();
        this.yCubics = new Vector<Cubic>();
        this.zCubics = new Vector<Cubic>();
        this.pitchCubics = new Vector<Cubic>();
        this.yawCubics = new Vector<Cubic>();
        this.rotCubics = new Vector<Cubic>();

        try {
            vectorX = Position.class.getDeclaredField("x");
            vectorY = Position.class.getDeclaredField("y");
            vectorZ = Position.class.getDeclaredField("z");
            vectorPitch = Position.class.getDeclaredField("pitch");
            vectorYaw = Position.class.getDeclaredField("yaw");
            vectorRot = Position.class.getDeclaredField("rotation");
            vectorX.setAccessible(true);
            vectorY.setAccessible(true);
            vectorZ.setAccessible(true);
            vectorPitch.setAccessible(true);
            vectorYaw.setAccessible(true);
            vectorRot.setAccessible(true);
        } catch(SecurityException e) {
            e.printStackTrace();
        } catch(NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public void addPoint(Position point) {
        this.points.add(point);
    }

    public Vector<Position> getPoints() {
        return points;
    }

    public void calcSpline() {
        try {
            calcNaturalCubic(points, vectorX, xCubics);
            calcNaturalCubic(points, vectorY, yCubics);
            calcNaturalCubic(points, vectorZ, zCubics);
            calcNaturalCubic(points, vectorPitch, pitchCubics);
            calcNaturalCubic(points, vectorYaw, yawCubics);
            calcNaturalCubic(points, vectorRot, rotCubics);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        } catch(InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public Position getPoint(float position) {
        position = position * xCubics.size();
        int cubicNum = (int) Math.min(xCubics.size() - 1, position);
        float cubicPos = (position - cubicNum);

        return new Position(xCubics.get(cubicNum).eval(cubicPos),
                yCubics.get(cubicNum).eval(cubicPos),
                zCubics.get(cubicNum).eval(cubicPos),
                (float) pitchCubics.get(cubicNum).eval(cubicPos),
                (float) yawCubics.get(cubicNum).eval(cubicPos),
                (float) rotCubics.get(cubicNum).eval(cubicPos));
    }

}
