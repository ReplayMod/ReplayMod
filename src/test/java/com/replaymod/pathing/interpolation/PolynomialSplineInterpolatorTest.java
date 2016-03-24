package com.replaymod.pathing.interpolation;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class PolynomialSplineInterpolatorTest {

    @Test
    public void testSolveMatrix() throws Exception {
        double[][] matrix;
        PolynomialSplineInterpolator.solveMatrix(matrix = new double[][]{
                {1, 0, 0, 1},
                {0, 1, 0, 2},
                {0, 0, 1, 3},
        });
        assertEquals(solvedMatrix(1, 2, 3), matrix);
        PolynomialSplineInterpolator.solveMatrix(matrix = new double[][]{
                {0, 0, 1, 3},
                {0, 1, 0, 2},
                {1, 0, 0, 1},
        });
        assertEquals(solvedMatrix(1, 2, 3), matrix);
        PolynomialSplineInterpolator.solveMatrix(matrix = new double[][]{
                {1, 0, 0, 1},
                {0, 2, 0, 4},
                {0, 0, 3, 9},
        });
        assertEquals(solvedMatrix(1, 2, 3), matrix);
        PolynomialSplineInterpolator.solveMatrix(matrix = new double[][]{
                {3, 3,  4, 1},
                {3, 5,  9, 2},
                {5, 9, 17, 4},
        });
        assertEquals(solvedMatrix(1, -2, 1), matrix);
    }

    private double[][] solvedMatrix(int...results) {
        double[][] matrix = new double[results.length][results.length + 1];
        for (int i = 0; i < results.length; i++) {
            matrix[i][i] = 1;
            matrix[i][results.length] = results[i];
        }
        return matrix;
    }

    private void assertEquals(double[][] expected, double[][] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], 1.0E-10);
        }
    }

    @Test
    public void testDerivative() throws Exception {
        assertArrayEquals(new double[]{}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{42}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{3, 2, 1}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{1, 1, 1, 1}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{15, 8, 3}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{5, 4, 3, 2}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{0, 0, 0}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{0, 0, 0, 0}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{0, 0, 0}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{0, 0, 0, 1}).derivative().coefficients, Double.MIN_VALUE);
    }
}