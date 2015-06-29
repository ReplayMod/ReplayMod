package eu.crushedpixel.replaymod.interpolation;

import akka.japi.Pair;

import java.util.ArrayList;
import java.util.List;

public abstract class LinearInterpolation<K> implements Interpolation<K> {

    protected List<K> points = new ArrayList<K>();

    public LinearInterpolation() {
        points = new ArrayList<K>();
    }

    @Override
    public void prepare() {

    }

    public abstract K getPoint(float position);

    public void addPoint(K point) {
        points.add(point);
    }

    protected Pair<Float, Pair<K, K>> getCurrentPoints(float position) {
        if(points.size() == 0) return null;
        position = position * (points.size() - 1);
        int cubicNum = (int) Math.min(points.size() - 1, position);
        float cubicPos = (position - cubicNum);

        if(cubicNum == points.size() - 1) {
            cubicNum--;
            cubicPos++;
        }

        if(cubicNum < 0) {
            return new Pair<Float, Pair<K, K>>(cubicPos, new Pair<K, K>(points.get(cubicNum + 1), points.get(cubicNum + 1)));
        }
        return new Pair<Float, Pair<K, K>>(cubicPos, new Pair<K, K>(points.get(cubicNum), points.get(cubicNum + 1)));
    }

    protected double getInterpolatedValue(double val1, double val2, float perc) {
        return val1 + ((val2 - val1) * perc);
    }
} 
