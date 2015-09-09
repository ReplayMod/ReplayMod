package eu.crushedpixel.replaymod.interpolation;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.SpectatorData;
import eu.crushedpixel.replaymod.holders.SpectatorDataThirdPersonInfo;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A SpectatorDataInterpolation object calculates Camera Paths between multiple Spectator Keyframes.
 * It does not matter which SpectatingMethod these Keyframes have, the SpectatorDataInterpolation treats them specifically.
 */
public class SpectatorDataInterpolation {

    private Integer entityID = null;

    private List<Pair<Integer, SpectatorData>> points = new ArrayList<Pair<Integer, SpectatorData>>();
    private KeyframeList<AdvancedPosition> underlyingKeyframes;

    private final boolean linear;

    public SpectatorDataInterpolation(boolean linear) {
        this.linear = linear;
        underlyingKeyframes = new KeyframeList<AdvancedPosition>();
    }

    public int size() {
        return points.size();
    }

    public boolean contains(SpectatorData spectatorData) {
        return indexOf(spectatorData) != -1;
    }

    public int indexOf(SpectatorData spectatorData) {
        int i = 0;
        for(Pair<Integer, SpectatorData> pair : points) {
            if(pair.getValue().equals(spectatorData)) return i;
            i++;
        }

        return -1;
    }

    public void prepare() {
        //first, create an Interpolation instance to retreive a SpectatorDataThirdPersonInfo object for every
        //position on the Spectator Path
        Interpolation<SpectatorDataThirdPersonInfo> thirdPersonInfoInterpolation =
                linear ? new GenericLinearInterpolation<SpectatorDataThirdPersonInfo>() :
                        new GenericSplineInterpolation<SpectatorDataThirdPersonInfo>();

        double previousSmoothness = 0.5;
        for(Pair<Integer, SpectatorData> pair : points) {
            if(pair.getValue().getSpectatingMethod() == SpectatorData.SpectatingMethod.FIRST_PERSON) {
                thirdPersonInfoInterpolation.addPoint(new SpectatorDataThirdPersonInfo(0, 0, 0, previousSmoothness));
            } else {
                thirdPersonInfoInterpolation.addPoint(pair.getValue().getThirdPersonInfo());
                previousSmoothness = pair.getValue().getThirdPersonInfo().shoulderCamSmoothness;
            }
        }
        thirdPersonInfoInterpolation.prepare();

        //feed the underlying interpolator with AdvancedPosition Keyframes that are derived from the Spectator Keyframes
        int i = 0;
        int size = points.size();
        for(Pair<Integer, SpectatorData> pair : points) {
            int timestamp = pair.getKey();
            int currentTimestamp = timestamp;
            int nextTimestamp = timestamp;

            if(i+1 < points.size()) {
                nextTimestamp = points.get(i+1).getKey();
            }

            int difference = nextTimestamp - timestamp;

            int smoothness = 0;
            while(currentTimestamp + smoothness < nextTimestamp) {
                currentTimestamp += smoothness;

                SpectatorDataThirdPersonInfo thirdPersonInfo = new SpectatorDataThirdPersonInfo();
                float percentage = (float)i/size;
                percentage += ((currentTimestamp-timestamp)/(float)difference) * 1f/size;

                thirdPersonInfoInterpolation.applyPoint(percentage, thirdPersonInfo);

                smoothness = (int)(thirdPersonInfo.shoulderCamSmoothness*1000);

                //calculate the Position relative to the Entity
                AdvancedPosition entityPosition = ReplayHandler.getEntityPositionTracker().getEntityPositionAtTimestamp(entityID, currentTimestamp);
                if(entityPosition == null) continue;

                //first, rotate the camera pitch and yaw according to the settings
                entityPosition.setYaw(entityPosition.getYaw() + thirdPersonInfo.shoulderCamYawOffset);
                entityPosition.setPitch(entityPosition.getPitch() + thirdPersonInfo.shoulderCamPitchOffset);

                //next, move the camera point to fulfill the specified distance to the entity
                entityPosition = entityPosition.getDestination(-1 * thirdPersonInfo.shoulderCamDistance);
                underlyingKeyframes.add(new Keyframe<AdvancedPosition>(currentTimestamp, entityPosition));
            }

            i++;
        }

        underlyingKeyframes.recalculate(linear);
    }

    public void applyPoint(float position, AdvancedPosition toEdit) {
        int keyframeIndex = (int)position*points.size();

        //the progress between this and the next keyframe (between 0 and 1)
        float partial = (position - ((float)keyframeIndex/points.size())) / (1f/points.size());

        Pair<Integer, SpectatorData> pair = points.get(keyframeIndex);
        Pair<Integer, SpectatorData> next = keyframeIndex < points.size()-1 ? points.get(keyframeIndex+1) : null;

        SpectatorData spectatorData = pair.getValue();

        //decide whether the Entity is spectated in First Person Mode or not
        boolean firstPerson = true;

        //firstPerson is false if either the SpectatorKeyframe
        //or the following SpectatorKeyframe are no FIRST_PERSON Keyframes
        //it's only true if the position value is between two FIRST_PERSON Keyframes
        if(spectatorData.getSpectatingMethod() != SpectatorData.SpectatingMethod.FIRST_PERSON) {
            firstPerson = false;
        } else if(next != null) {
            if(next.getValue().getSpectatingMethod() != SpectatorData.SpectatingMethod.FIRST_PERSON) firstPerson = false;
        }

        if(firstPerson) {
            int firstTimestamp = pair.getKey();
            int nextTimestamp = next == null ? pair.getKey() : next.getKey();

            int diff = nextTimestamp - firstTimestamp;
            int interpolatedTimestamp = firstTimestamp+(int)(partial*diff);

            AdvancedPosition pos = ReplayHandler.getEntityPositionTracker().getEntityPositionAtTimestamp(entityID, interpolatedTimestamp);
            if(pos != null)
                toEdit.apply(pos);
        } else {
            toEdit.apply(underlyingKeyframes.getInterpolatedValueForPathPosition(position, linear));
        }
    }

    public void addPoint(SpectatorData spectatorData, int realTimestamp) {
        if(entityID == null) entityID = spectatorData.getSpectatedEntityID();
        else if(entityID != spectatorData.getSpectatedEntityID()) throw new IllegalArgumentException();
        points.add(Pair.of(realTimestamp, spectatorData));
    }
}