package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.assets.CustomImageObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class KeyframeSet implements GuiEntryListEntry {
    private String name;
    private Keyframe<AdvancedPosition>[] positionKeyframes;
    private Keyframe<TimestampValue>[] timeKeyframes;
    private CustomImageObject[] customObjects = new CustomImageObject[0];

    public KeyframeSet(String name, Keyframe[] keyframes, List<CustomImageObject> customObjectRepository) {
        this.name = name;
        setCustomObjects(customObjectRepository.toArray(new CustomImageObject[customObjectRepository.size()]));
        setKeyframes(keyframes);
    }

    public KeyframeSet(String name, Keyframe[] keyframes, CustomImageObject[] customObjects) {
        this.name = name;
        setCustomObjects(customObjects);
        setKeyframes(keyframes);
    }

    public void setCustomObjects(CustomImageObject[] customObjects) {
        this.customObjects = new CustomImageObject[customObjects.length];

        int i = 0;
        for(CustomImageObject object : customObjects) {
            try {
                this.customObjects[i] = object.copy();
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
            i++;
        }
    }

    @SuppressWarnings("unchecked")
    public void setKeyframes(Keyframe[] keyframes) {
        List<Keyframe<AdvancedPosition>> posKFList = new ArrayList<Keyframe<AdvancedPosition>>();
        List<Keyframe<TimestampValue>> timeKFList = new ArrayList<Keyframe<TimestampValue>>();

        for(Keyframe kf : keyframes) {
            if(kf.getValue() instanceof AdvancedPosition)
                posKFList.add((Keyframe<AdvancedPosition>)kf);
            else if(kf.getValue() instanceof TimestampValue)
                timeKFList.add((Keyframe<TimestampValue>) kf);
        }

        positionKeyframes = posKFList.toArray(new Keyframe[posKFList.size()]);
        timeKeyframes = timeKFList.toArray(new Keyframe[timeKFList.size()]);
    }

    public Keyframe[] getKeyframes() {
        Keyframe[] keyframes = new Keyframe[positionKeyframes.length + timeKeyframes.length];
        System.arraycopy(positionKeyframes, 0, keyframes, 0, positionKeyframes.length);
        System.arraycopy(timeKeyframes, 0, keyframes, positionKeyframes.length, timeKeyframes.length);
        return keyframes;
    }

    public int getTimeKeyframeCount() {
        return timeKeyframes.length;
    }

    public int getPositionKeyframeCount() {
        return positionKeyframes.length;
    }

    public int getPathDuration() {
        int first = 0;
        int last = 0;

        for(Keyframe k : getKeyframes()) {
            if(k.getRealTimestamp() < first) {
                first = k.getRealTimestamp();
            } else if(k.getRealTimestamp() > last) {
                last = k.getRealTimestamp();
            }
        }

        return last-first;
    }

    public int getCustomObjectCount() {
        return customObjects.length;
    }

    @Override
    public String getDisplayString() {
        return name;
    }
}
