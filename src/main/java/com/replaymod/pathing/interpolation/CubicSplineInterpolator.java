package com.replaymod.pathing.interpolation;

public class CubicSplineInterpolator extends PolynomialSplineInterpolator {
    public CubicSplineInterpolator() {
        super(3);
    }

    @Override
    protected void fillMatrix(double[][] matrix, double[] xs, double[] ys, int num, InterpolationParameters params) {
        int row = 0;
        double x;

        if (params != null) {
            // Apply previous values
            ys[0] = params.getValue();
            x = xs[0];
            matrix[row][0] = 3 * x * x;
            matrix[row][1] = 2 * x;
            matrix[row][2] = 1;
            matrix[row][num * 4] = params.getVelocity();
            row++;
            matrix[row][0] = 6 * x;
            matrix[row][1] = 2;
            matrix[row][num * 4] = params.getAcceleration();
            row++;
        } else {
            // Set second derivative at the first and the last knot to 0
            matrix[row][0] = 6 * xs[0];
            matrix[row][1] = 2;
            row++;
            matrix[row][(num - 1) * 4] = 6 * xs[xs.length - 1];
            matrix[row][(num - 1) * 4 + 1] = 2;
            row++;
        }

        for (int i = 0; i < num; i++) {
            // Each cubic i must produce the correct result at x[i] and x[i+1], that is y[i] and y[i+1]
            // Resulting in these linear equations for every value
            //       a[i]    b[i]     c[i]    d[i]  ...  y
            // (... x[i]³    x[i]²    x[i]     1    ...  y[i]     ...)  or  f[i](x[i]) = y[i]
            // (... x[i+1]³  x[i+1]²  x[i+1]   1    ...  y[i+1]   ...)  or  f[i](x[i+1]) = y[i+1]
            x = xs[i];
            matrix[row][i * 4    ] = x * x * x;
            matrix[row][i * 4 + 1] = x * x;
            matrix[row][i * 4 + 2] = x;
            matrix[row][i * 4 + 3] = 1;
            matrix[row][num * 4] = ys[i];
            row++;
            x = xs[i + 1];
            matrix[row][i * 4    ] = x * x * x;
            matrix[row][i * 4 + 1] = x * x;
            matrix[row][i * 4 + 2] = x;
            matrix[row][i * 4 + 3] = 1;
            matrix[row][num * 4] = ys[i + 1];
            row++;

            // The first derivative should be defined at all knots
            // Therefore two adjacent cubics have to have the same derivative at their common knot
            // Linear equation for every value (except the last one)
            //       a[i]      b[i]    c[i]  d[i]  a[i+1]    b[i+1]   c[i+1]  d[i+1]
            // (... 3x[i+1]²  2x[i+1]   1     0    3x[i+1]²  2x[i+1]    1       0     ...)
            if (i < num - 1) {
                x = xs[i + 1];
                matrix[row][i * 4] = -(matrix[row][i * 4 + 4] = 3 * x * x);
                matrix[row][i * 4 + 1] = -(matrix[row][i * 4 + 5] = 2 * x);
                matrix[row][i * 4 + 2] = -(matrix[row][i * 4 + 6] = 1);
                row++;
            }

            // Same for the second derivative
            if (i < num - 1) {
                x = xs[i + 1];
                matrix[row][i * 4] = -(matrix[row][i * 4 + 4] = 6 * x);
                matrix[row][i * 4 + 1] = -(matrix[row][i * 4 + 5] = 2);
                row++;
            }
        }
    }
}
