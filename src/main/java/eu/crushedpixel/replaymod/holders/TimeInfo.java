package eu.crushedpixel.replaymod.holders;

/**
 * Copyright (c) 2014 Johni0702
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **/
public final class TimeInfo {

	@Override
	public String toString() {
		return start+" | "+speedSince+" | "+speed+" | "+jumpTo;
	}
	
	public static TimeInfo create() {
		long now = System.currentTimeMillis();
		return new TimeInfo(now, now, 1, -1);
	}

	public TimeInfo(long start, long speedSince, double speed, long jumpTo) {
		this.start = start;
		this.speedSince = speedSince;
		this.speed = speed;
		this.jumpTo = jumpTo;
	}

	private final long start;
	private final long speedSince;
	private final double speed;
	private final long jumpTo;

	public long getActualStartTime(long now) {
		long realTimePassed = now - speedSince;
		long ingameTimePassed = (long) (realTimePassed * this.speed);
		return start + realTimePassed - ingameTimePassed;
	}

	public long getInGameTimePassed(long now) {
		long realTimePassed = now - speedSince;
		long ingameTimePassed = (long) (realTimePassed * this.speed);
		return speedSince - start + ingameTimePassed;
	}

	public TimeInfo updateSpeed(long now, double speed) {
		if (isJumping()) {
			return new TimeInfo(now-jumpTo, now, speed, -1);
		} else {
			if (this.speed == speed) {
				return this;
			}
			long start;
			if (this.speed == 1) {
				start = this.start;
			} else {
				start = getActualStartTime(now);
			}
			return new TimeInfo(start, now, speed, -1);
		}
	}

	public boolean isJumping() {
		return jumpTo != -1;
	}

	public TimeInfo jumpTo(long jumpTo) {
		return new TimeInfo(start, speedSince, speed, jumpTo);
	}

	public long getStart() {
		return start;
	}

	public long getSpeedSince() {
		return speedSince;
	}

	public double getSpeed() {
		return speed;
	}

	public long getJumpTo() {
		return jumpTo;
	}
}