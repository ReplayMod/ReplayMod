package eu.crushedpixel.replaymod.utils;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.KeyframeSet;
import eu.crushedpixel.replaymod.holders.TimestampValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LegacyKeyframeSetAdapter extends TypeAdapter<KeyframeSet[]> {

    public LegacyKeyframeSetAdapter() {
        super();
    }

    @Override
    public KeyframeSet[] read(JsonReader in) throws IOException {
        List<KeyframeSet> sets = new ArrayList<KeyframeSet>();

        in.beginArray();
        while(in.hasNext()) { //iterate over all array entries

            KeyframeSet set = new KeyframeSet();
            List<Keyframe> keyframes = new ArrayList<Keyframe>();

            in.beginObject();
            while(in.hasNext()) { //iterate over all object entries
                String jsonTag = in.nextName();

                if("name".equals(jsonTag)) {
                    set.setName(in.nextString());

                } else if("positionKeyframes".equals(jsonTag)) {
                    in.beginArray();
                    while(in.hasNext()) {
                        Keyframe<AdvancedPosition> newKeyframe = new Keyframe<AdvancedPosition>();
                        Integer spectatedEntityID = null;
                        in.beginObject();
                        while(in.hasNext()) {
                            String jsonKeyframeTag = in.nextName();
                            if("value".equals(jsonKeyframeTag) || "position".equals(jsonKeyframeTag)) {
                                AdvancedPosition position = new Gson().fromJson(in, AdvancedPosition.class);
                                newKeyframe.setValue(position);
                            } else if("realTimestamp".equals(jsonKeyframeTag)) {
                                newKeyframe.setRealTimestamp(in.nextInt());
                            } else if("spectatedEntityID".equals(jsonKeyframeTag)) {
                                spectatedEntityID = in.nextInt();
                            }
                        }
                        newKeyframe.getValue().setSpectatedEntityID(spectatedEntityID);
                        in.endObject();

                        keyframes.add(newKeyframe);
                    }
                    in.endArray();

                } else if("timeKeyframes".equals(jsonTag)) {
                    in.beginArray();
                    while(in.hasNext()) {
                        Keyframe<TimestampValue> newKeyframe = new Keyframe<TimestampValue>();

                        in.beginObject();
                        while(in.hasNext()) {
                            String jsonKeyframeTag = in.nextName();
                            if("timestamp".equals(jsonKeyframeTag)) {
                                TimestampValue timestampValue = new TimestampValue(in.nextInt());
                                newKeyframe.setValue(timestampValue);
                            } else if("value".equals(jsonKeyframeTag)) {
                                TimestampValue timestampValue = new Gson().fromJson(in, TimestampValue.class);
                                newKeyframe.setValue(timestampValue);
                            } else if("realTimestamp".equals(jsonKeyframeTag)) {
                                newKeyframe.setRealTimestamp(in.nextInt());
                            }
                        }
                        in.endObject();

                        keyframes.add(newKeyframe);
                    }
                    in.endArray();
                }
            }
            in.endObject();

            set.setKeyframes(keyframes.toArray(new Keyframe[keyframes.size()]));
            sets.add(set);
        }
        in.endArray();

        return sets.toArray(new KeyframeSet[sets.size()]);
    }

    @Override
    public void write(JsonWriter out, KeyframeSet[] value) throws IOException {}
}
