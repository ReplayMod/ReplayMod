package eu.crushedpixel.replaymod.interpolation;

public interface Interpolation<T> {
    void prepare();
    T getPoint(float position);
    void addPoint(T pos);
}
