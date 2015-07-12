package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GenericLinearInterpolation<T extends KeyframeValue> implements Interpolation<T> {

    private Field[] fields;

    protected List<T> points = new ArrayList<T>();

    public GenericLinearInterpolation() {
        points = new ArrayList<T>();
    }

    @Override
    public void prepare() {}

    public void addPoint(T point) {
        points.add(point);

        List<Field> fields = ReflectionUtils.getFieldsToInterpolate(point.getClass());
        this.fields = fields.toArray(new Field[fields.size()]);
    }

    @Override
    public void applyPoint(float position, T toEdit) {
        if(points.isEmpty()) {
            throw new IllegalStateException("At least one Value needs to be added for this operation");
        }

        //first, get previous and next T for given position
        float relative = position * (points.size()-1);
        int previousIndex = (int)Math.floor(relative);
        int nextIndex = (int)Math.ceil(relative);
        float percentage = relative - previousIndex;

        T previous = points.get(previousIndex);
        T next = points.get(nextIndex);

        for(Field f : fields) {
            try {
                f.set(toEdit, getInterpolatedValue(f.getDouble(previous), f.getDouble(next), percentage));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private double getInterpolatedValue(double val1, double val2, float perc) {
        return val1 + ((val2 - val1) * perc);
    }
} 
