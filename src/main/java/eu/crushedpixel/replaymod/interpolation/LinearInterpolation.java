package eu.crushedpixel.replaymod.interpolation;

import java.util.ArrayList;
import java.util.List;

import akka.japi.Pair;

public abstract class LinearInterpolation<K> {

	protected List<K> points = new ArrayList<K>();
	
	public abstract K getPoint(float position);
	
	public void addPoint(K point) {
		points.add(point);
	}
	
	public void clearPoints() {
		points = new ArrayList<K>();
	}
	
	protected Pair<Float, Pair<K, K>> getCurrentPoints(float position) {
		if(points.size() == 0) return null;
		float pos = position * (points.size()-1);
		int      cubicNum = Math.round(pos);
		float    cubicPos = (pos - cubicNum);
		
		if(cubicNum == points.size()-1) {
			cubicNum--;
			cubicPos++;
		}

		if(cubicNum < 0) {
			return new Pair<Float, Pair<K,K>>(cubicPos, new Pair<K,K>(points.get(cubicNum+1), points.get(cubicNum+1)));
		}
		return new Pair<Float, Pair<K,K>>(cubicPos, new Pair<K,K>(points.get(cubicNum), points.get(cubicNum+1)));
	}
	
	protected double getInterpolatedValue(double val1, double val2, float perc) {
		return val1+((val2-val1)*perc);
	}
} 
