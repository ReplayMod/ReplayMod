package com.replaymod.pathing.properties;

import com.replaymod.pathing.property.AbstractPropertyPart;
import com.replaymod.pathing.property.Property;
import org.apache.commons.lang3.tuple.Triple;

public class PropertyParts {
    private PropertyParts(){}

    public static class ForInteger extends AbstractPropertyPart<Integer> {
        public ForInteger(Property<Integer> property, boolean interpolatable) {
            super(property, interpolatable);
        }

        @Override
        public double toDouble(Integer value) {
            return value;
        }

        @Override
        public Integer fromDouble(Integer value, double d) {
            return (int) Math.round(d);
        }
    }

    public static class ForDoubleTriple extends AbstractPropertyPart<Triple<Double, Double, Double>> {
        private final TripleElement element;
        public ForDoubleTriple(Property<Triple<Double, Double, Double>> property, boolean interpolatable, TripleElement element) {
            super(property, interpolatable);
            this.element = element;
        }

        @Override
        public double toDouble(Triple<Double, Double, Double> value) {
            switch (element) {
                case LEFT: return value.getLeft();
                case MIDDLE: return value.getMiddle();
                case RIGHT: return value.getRight();
            }
            throw new AssertionError(element);
        }

        @Override
        public Triple<Double, Double, Double> fromDouble(Triple<Double, Double, Double> value, double d) {
            switch (element) {
                case LEFT: return Triple.of(d, value.getMiddle(), value.getRight());
                case MIDDLE: return Triple.of(value.getLeft(), d, value.getRight());
                case RIGHT: return Triple.of(value.getLeft(), value.getMiddle(), d);
            }
            throw new AssertionError(element);
        }
    }

    public static class ForFloatTriple extends AbstractPropertyPart<Triple<Float, Float, Float>> {
        private final TripleElement element;
        public ForFloatTriple(Property<Triple<Float, Float, Float>> property, boolean interpolatable, TripleElement element) {
            super(property, interpolatable);
            this.element = element;
        }

        @Override
        public double toDouble(Triple<Float, Float, Float> value) {
            switch (element) {
                case LEFT: return value.getLeft();
                case MIDDLE: return value.getMiddle();
                case RIGHT: return value.getRight();
            }
            throw new AssertionError(element);
        }

        @Override
        public Triple<Float, Float, Float> fromDouble(Triple<Float, Float, Float> value, double d) {
            switch (element) {
                case LEFT: return Triple.of((float) d, value.getMiddle(), value.getRight());
                case MIDDLE: return Triple.of(value.getLeft(), (float) d, value.getRight());
                case RIGHT: return Triple.of(value.getLeft(), value.getMiddle(), (float) d);
            }
            throw new AssertionError(element);
        }
    }

    public enum TripleElement {
        LEFT, MIDDLE, RIGHT;
    }
}
