package eu.crushedpixel.replaymod.interpolation;

public interface Interpolation<T extends KeyframeValue> {
    void prepare();
    void applyPoint(float position, T toEdit);
    void addPoint(T pos);
}
