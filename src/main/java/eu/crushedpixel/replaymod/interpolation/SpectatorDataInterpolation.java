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

    private List<Pair<Integer, Keyframe<AdvancedPosition>>> points = new ArrayList<Pair<Integer, Keyframe<AdvancedPosition>>>();
    private KeyframeList<AdvancedPosition> underlyingKeyframes;

    private final boolean linear;

    public SpectatorDataInterpolation(boolean linear) {
        this.linear = linear;
        underlyingKeyframes = new KeyframeList<AdvancedPosition>();
    }

    public int size() {
        return points.size();
    }

    public List<Keyframe<AdvancedPosition>> elements() {
        return new ArrayList<Keyframe<AdvancedPosition>>(underlyingKeyframes);
    }

    public void prepare() {
        //first, create an Interpolation instance to retreive a SpectatorDataThirdPersonInfo object for every
        //position on the Spectator Path
        Interpolation<SpectatorDataThirdPersonInfo> thirdPersonInfoInterpolation =
                linear ? new GenericLinearInterpolation<SpectatorDataThirdPersonInfo>() :
                        new GenericSplineInterpolation<SpectatorDataThirdPersonInfo>();

        double previousSmoothness = 0.5;
        for(Pair<Integer, Keyframe<AdvancedPosition>> pair : points) {
            if(((SpectatorData)pair.getValue().getValue()).getSpectatingMethod() == SpectatorData.SpectatingMethod.FIRST_PERSON) {
                thirdPersonInfoInterpolation.addPoint(new SpectatorDataThirdPersonInfo(0, 0, 0, previousSmoothness));
            } else {
                SpectatorDataThirdPersonInfo thirdPersonInfo = ((SpectatorData)pair.getValue().getValue()).getThirdPersonInfo();
                thirdPersonInfoInterpolation.addPoint(thirdPersonInfo);
                previousSmoothness = thirdPersonInfo.shoulderCamSmoothness;
            }
        }
        thirdPersonInfoInterpolation.prepare();

        //updating the spectator keyframe's position in the world to smoothly continue the path
        //with non-spectator position keyframes
        for(Pair<Integer, Keyframe<AdvancedPosition>> pair : points) {
            AdvancedPosition entityPosition = ReplayHandler.getEntityPositionTracker().getEntityPositionAtTimestamp(entityID, pair.getKey());
            if(entityPosition == null) continue;

            //transform the entity position (sry for no dry code :x )
            SpectatorDataThirdPersonInfo thirdPersonInfo = ((SpectatorData)pair.getValue().getValue()).getThirdPersonInfo();

            //first, rotate the camera pitch and yaw according to the settings
            entityPosition.setYaw(entityPosition.getYaw() + thirdPersonInfo.shoulderCamYawOffset);
            entityPosition.setPitch(entityPosition.getPitch() + thirdPersonInfo.shoulderCamPitchOffset);

            //next, move the camera point to fulfill the specified distance to the entity
            entityPosition = entityPosition.getDestination(-1 * thirdPersonInfo.shoulderCamDistance);

            pair.getValue().getValue().apply(entityPosition);
        }

        //feed the underlying keyframe list with AdvancedPosition Keyframes that are derived from the Spectator Keyframes
        underlyingKeyframes = new KeyframeList<AdvancedPosition>();
        int i = 0;
        int firstTimestamp = -1;
        int size = points.size()-1;
        for(Pair<Integer, Keyframe<AdvancedPosition>> pair : points) {
            underlyingKeyframes.add(new Keyframe<AdvancedPosition>(pair.getValue().getRealTimestamp(), pair.getValue().getValue()));

            int timestamp = pair.getKey();
            if(firstTimestamp == -1) firstTimestamp = timestamp;

            int realTimestamp = pair.getValue().getRealTimestamp();
            int nextRealTimestamp = realTimestamp;

            int currentTimestamp = timestamp;
            int nextTimestamp = timestamp;

            if(i+1 < points.size()) {
                Pair<Integer, Keyframe<AdvancedPosition>> nextPair = points.get(i+1);
                nextTimestamp = nextPair.getKey();
                nextRealTimestamp = nextPair.getValue().getRealTimestamp();
            }

            int difference = nextTimestamp - timestamp;
            int realTimestampDifference = nextRealTimestamp - realTimestamp;

            int smoothness = 0;
            while(currentTimestamp + smoothness < nextTimestamp) {
                currentTimestamp += smoothness;

                SpectatorDataThirdPersonInfo thirdPersonInfo = new SpectatorDataThirdPersonInfo();
                float percentage = (float)i/size;
                percentage += ((currentTimestamp-timestamp)/(float)difference) * 1f/size;

                float progress = ((currentTimestamp-timestamp)/(float)difference);
                int interpolatedRealTimestamp = (int)(realTimestamp + (progress*realTimestampDifference));

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

                underlyingKeyframes.add(new Keyframe<AdvancedPosition>(interpolatedRealTimestamp, entityPosition));
            }

            i++;
        }
    }

    public void addPoint(Keyframe<AdvancedPosition> keyframe, int realTimestamp) {
        if(!(keyframe.getValue() instanceof SpectatorData)) throw new IllegalArgumentException();
        SpectatorData spectatorData = (SpectatorData)keyframe.getValue();
        if(entityID == null) entityID = spectatorData.getSpectatedEntityID();
        else if(entityID != spectatorData.getSpectatedEntityID()) throw new IllegalArgumentException();
        points.add(Pair.of(realTimestamp, keyframe));
    }
}