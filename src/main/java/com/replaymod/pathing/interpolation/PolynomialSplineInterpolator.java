package com.replaymod.pathing.interpolation;

import com.google.common.base.Optional;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.PathSegment;
import com.replaymod.pathing.property.Property;
import com.replaymod.pathing.property.PropertyPart;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class PolynomialSplineInterpolator extends AbstractInterpolator {
    private final int degree;
    private Map<Property<?>, Set<Keyframe>> framesToProperty = new HashMap<>();
    private Map<PropertyPart, Polynomials> polynomials = new HashMap<>();

    protected PolynomialSplineInterpolator(int degree) {
        this.degree = degree;
    }

    @Override
    protected Map<PropertyPart, InterpolationParameters> bakeInterpolation(Map<PropertyPart, InterpolationParameters> parameters) {
        framesToProperty.clear();
        for (PathSegment segment : getSegments()) {
            for (Property property : getKeyframeProperties()) {
                if (segment.getStartKeyframe().getValue(property).isPresent()) {
                    addToMap(framesToProperty, property, segment.getStartKeyframe());
                }
                if (segment.getEndKeyframe().getValue(property).isPresent()) {
                    addToMap(framesToProperty, property, segment.getEndKeyframe());
                }
            }
        }

        polynomials.clear();
        parameters = new HashMap<>(parameters);
        for (Map.Entry<Property<?>, Set<Keyframe>> entry : framesToProperty.entrySet()) {
            prepareProperty(entry.getKey(), entry.getValue(), parameters);
        }

        return parameters;
    }

    private <U> void prepareProperty(Property<U> property, Set<Keyframe> keyframes, Map<PropertyPart, InterpolationParameters> parameters) {
        for (PropertyPart<U> part : property.getParts()) {
            if (part.isInterpolatable()) {
                double[] time = new double[keyframes.size()];
                double[] values = new double[keyframes.size()];
                int i = 0;
                for (Keyframe keyframe : keyframes) {
                    time[i] = keyframe.getTime();
                    values[i++] = part.toDouble(keyframe.getValue(property).get());
                }
                Polynomials polynomials = calcPolynomials(time, values, parameters.get(part));

                double lastTime = time[time.length - 1];
                Polynomial lastPolynomial = polynomials.polynomials[polynomials.polynomials.length - 1];
                double lastValue = lastPolynomial.eval(lastTime) + polynomials.yOffset;
                double lastVelocity = (lastPolynomial = lastPolynomial.derivative()).eval(lastTime);
                double lastAcceleration = lastPolynomial.derivative().eval(lastTime);
                parameters.put(part, new InterpolationParameters(lastValue, lastVelocity, lastAcceleration));
                this.polynomials.put(part, polynomials);
            }
        }
    }

    private void addToMap(Map<Property<?>, Set<Keyframe>> map, Property property, Keyframe keyframe) {
        Set<Keyframe> set = map.get(property);
        if (set == null) {
            map.put(property, set = new LinkedHashSet<>());
        }
        set.add(keyframe);
    }

    protected Polynomials calcPolynomials(double[] xs, double[] ys, InterpolationParameters params) {
        int unknowns = degree + 1;
        int num = xs.length - 1;
        if (num == 0) {
            return new Polynomials(0, new Polynomial[]{new Polynomial(new double[]{ys[0]})});
        }

        double yOffset;
        {
            double total = 0;
            for (double y : ys) {
                total += y;
            }
            yOffset = total / ys.length;
            for (int i = 0; i < ys.length; i++) {
                ys[i] -= yOffset;
            }
            for (int i = 0; i < xs.length; i++) {
                xs[i] /= 1000;
            }
        }

        // We want to find cubic equations y = ax³ + bx² + cx + d, one for each pair of values
        double[][] matrix = new double[num * unknowns][num * unknowns + 1];

        fillMatrix(matrix, xs, ys, num, params);

        solveMatrix(matrix);

        Polynomial[] polynomials = new Polynomial[num];
        for (int i = 0; i < num; i++) {
            double[] coefficients = new double[degree + 1];
            for (int j = 0; j <= degree; j++) {
                coefficients[j] = matrix[i * unknowns + j][num * unknowns];
            }
            polynomials[i] = new Polynomial(coefficients);
        }
        return new Polynomials(yOffset, polynomials);
    }

    protected abstract void fillMatrix(double[][] matrix, double[] xs, double[] ys, int num, InterpolationParameters params);

    protected static void solveMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i][i] == 0) {
                for (int j = i + 1; j < matrix.length; j++) {
                    if (matrix[j][i] != 0) {
                        double[] s = matrix[j];
                        matrix[j] = matrix[i];
                        matrix[i] = s;
                        break;
                    }
                }
            }
            double factor = matrix[i][i];
            if (factor != 1) {
                matrix[i][i] = 1;
                for (int j = i + 1; j < matrix[i].length; j++) {
                    matrix[i][j] /= factor;
                }
            }
            for (int j = i + 1; j < matrix.length; j++) {
                factor = matrix[j][i];
                if (factor != 0) {
                    matrix[j][i] = 0;
                    for (int k = i + 1; k < matrix[j].length; k++) {
                        matrix[j][k] = matrix[j][k] - matrix[i][k] * factor;
                    }
                }
            }
        }
        for (int i = matrix.length - 1; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (matrix[j][i] != 0) {
                    int k = matrix[j].length - 1;
                    matrix[j][k] -= matrix[j][i] / matrix[i][i] * matrix[i][k];
                    matrix[j][i] = 0;
                }
            }
        }
    }

    @Override
    public <T> Optional<T> getValue(Property<T> property, long time) {
        Set<Keyframe> kfSet = framesToProperty.get(property);
        if (kfSet == null) {
            return Optional.absent();
        }
        Keyframe kfBefore = null, kfAfter = null;
        int index = 0;
        for (Keyframe keyframe : kfSet) {
            if (keyframe.getTime() == time) {
                return keyframe.getValue(property);
            } else if (keyframe.getTime() < time) {
                kfBefore = keyframe;
            } else if (keyframe.getTime() > time) {
                kfAfter = keyframe;
                index--;
                break;
            }
            index++;
        }
        if (kfBefore == null || kfAfter == null) {
            return Optional.absent();
        }

        T interpolated = kfBefore.getValue(property).get();
        for (PropertyPart<T> part : property.getParts()) {
            if (part.isInterpolatable()) {
                interpolated = part.fromDouble(interpolated, polynomials.get(part).eval(time, index));
            }
        }
        return Optional.of(interpolated);
    }

    private static class Polynomials {
        private final double yOffset;
        private final Polynomial[] polynomials;

        private Polynomials(double yOffset, Polynomial[] polynomials) {
            this.yOffset = yOffset;
            this.polynomials = polynomials;
        }

        public double eval(double time, int index) {
            return polynomials[index].eval(time / 1000) + yOffset;
        }
    }

    public static class Polynomial {
        public final double[] coefficients;

        public Polynomial(double[] coefficients) {
            this.coefficients = coefficients;
        }

        public double eval(double at) {
            double val = 0;
            for (double coefficient : coefficients) {
                val = val * at + coefficient;
            }
            return val;
        }

        public Polynomial derivative() {
            if (coefficients.length == 0) {
                return this;
            }
            Polynomial derived = new Polynomial(new double[coefficients.length - 1]);
            for (int i = 0; i < coefficients.length - 1; i++) {
                derived.coefficients[i] = coefficients[i] * (coefficients.length - 1 - i);
            }
            return derived;
        }
    }
}
