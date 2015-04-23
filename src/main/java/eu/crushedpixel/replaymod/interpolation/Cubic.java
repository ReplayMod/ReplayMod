package eu.crushedpixel.replaymod.interpolation;

public class Cubic {
    private double a, b, c, d;

    public Cubic(double p0, double d2, double e, double f) {
        this.a = p0;
        this.b = d2;
        this.c = e;
        this.d = f;
    }

    public double eval(double u) {
        return (((d * u) + c) * u + b) * u + a;
    }
}
