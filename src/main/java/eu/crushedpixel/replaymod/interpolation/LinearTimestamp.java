package eu.crushedpixel.replaymod.interpolation;

import akka.japi.Pair;
import eu.crushedpixel.replaymod.holders.Position;

public class LinearTimestamp extends LinearInterpolation<Integer> {

	@Override
	public Integer getPoint(float position) {
		Pair<Float, Pair<Integer, Integer>> pair = getCurrentPoints(position);
		if(pair == null) return null;
		
		float perc = pair.first();

		int first = pair.second().first();
		int second = pair.second().second();

		int val = (int)getInterpolatedValue(first, second, perc);

		return val;
	}
}
