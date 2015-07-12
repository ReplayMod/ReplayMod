package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Vector;

public class GenericSplineInterpolation<T extends KeyframeValue> extends BasicSpline implements Interpolation<T> {

    private Field[] fields;

    private Vector<T> points;
    private Vector<Cubic>[] cubics = new Vector[0];

    public GenericSplineInterpolation() {
        this.points = new Vector<T>();
    }

    public void addPoint(T point) {
        this.points.add(point);

        List<Field> fields = ReflectionUtils.getFieldsToInterpolate(point.getClass());
        this.fields = fields.toArray(new Field[fields.size()]);
    }

    public Vector<T> getPoints() {
        return points;
    }

    @Override
    public void prepare() {
        if(fields.length <= 0) {
            throw new IllegalStateException("The passed KeyframeValue class" +
                    " has to contain at least one Field");
        }
        if(!points.isEmpty()) {
            cubics = new Vector[fields.length];
            for(int i=0; i<fields.length; i++) {
                cubics[i] = new Vector<Cubic>();
                try {
                    calcNaturalCubic(points, fields[i], cubics[i]);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            throw new IllegalStateException("At least one Value needs to be added" +
                    " before preparing this Spline");
        }
    }

    public void applyPoint(float position, T toEdit) {
        Vector<Cubic> first = cubics[0];
        position = position * first.size();
        int cubicNum = (int) Math.min(cubics[0].size() - 1, position);
        float cubicPos = (position - cubicNum);

        int i = 0;
        for(Field f : fields) {
            try {
                f.set(toEdit, cubics[i].get(cubicNum).eval(cubicPos));
            } catch(Exception e) {
                e.printStackTrace();
            }
            i++;
        }
    }

}
