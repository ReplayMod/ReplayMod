package eu.crushedpixel.replaymod.interpolation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public abstract class BasicSpline {
	public void calcNaturalCubic(List valueCollection, Field val, Collection<Cubic> cubicCollection) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		int num = valueCollection.size()-1;

		double[] gamma = new double[num+1];
		double[] delta = new double[num+1];
		double[] D = new double[num+1];

		int i;
		/*
	           We solve the equation
	          [2 1       ] [D[0]]   [3(x[1] - x[0])  ]
	          |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
	          |  1 4 1   | | .  | = |      .         |
	          |    ..... | | .  |   |      .         |
	          |     1 4 1| | .  |   |3(x[n] - x[n-2])|
	          [       1 2] [D[n]]   [3(x[n] - x[n-1])]

	          by using row operations to convert the matrix to upper triangular
	          and then back sustitution.  The D[i] are the derivatives at the knots.
		 */
		gamma[0] = 1.0f / 2.0f;
		for(i=1; i< num; i++) {
			gamma[i] = 1.0f/(4.0f - gamma[i-1]);
		}
		gamma[num] = 1.0f/(2.0f - gamma[num-1]);

		Double p0 = val.getDouble(valueCollection.get(0));
		Double p1 = val.getDouble(valueCollection.get(1));

		delta[0] = 3.0f * (p1 - p0) * gamma[0];
		for(i=1; i< num; i++) {
			p0 = val.getDouble(valueCollection.get(i-1));
			p1 = val.getDouble(valueCollection.get(i+1));
			delta[i] = (3.0f * (p1 - p0) - delta[i - 1]) * gamma[i];
		}
		p0 = val.getDouble(valueCollection.get(num-1));
		p1 = val.getDouble(valueCollection.get(num));

		delta[num] = (3.0f * (p1 - p0) - delta[num - 1]) * gamma[num];

		D[num] = delta[num];
		for(i=num-1; i >= 0; i--) {
			D[i] = delta[i] - gamma[i] * D[i+1];
		}

	    //now compute the coefficients of the cubics
		cubicCollection.clear();

		for(i=0; i<num; i++) {
			p0 = val.getDouble(valueCollection.get(i));
			p1 = val.getDouble(valueCollection.get(i+1));

			cubicCollection.add(new Cubic(
					p0, 
					D[i], 
					3*(p1 - p0) - 2*D[i] - D[i+1],
					2*(p0 - p1) +   D[i] + D[i+1]
					)
					);
		}
	}
}