package eu.crushedpixel.replaymod.utils;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import eu.crushedpixel.replaymod.assets.CustomImageObject;
import eu.crushedpixel.replaymod.holders.*;

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

                    //TODO: Adapt to new Spectator Keyframe system
                } else if("positionKeyframes".equals(jsonTag)) {
                    in.beginArray();
                    while(in.hasNext()) {
                        Keyframe<AdvancedPosition> newKeyframe = new Keyframe<AdvancedPosition>();
                        Integer spectatedEntityID = null;
                        in.beginObject();
                        while(in.hasNext()) {
                            String jsonKeyframeTag = in.nextName();
                            if("value".equals(jsonKeyframeTag) || "position".equals(jsonKeyframeTag)) {
                                SpectatorData spectatorData = new Gson().fromJson(in, SpectatorData.class);
                                newKeyframe.setValue(spectatorData.normalize());
                            } else if("realTimestamp".equals(jsonKeyframeTag)) {
                                newKeyframe.setRealTimestamp(in.nextInt());
                            } else if("spectatedEntityID".equals(jsonKeyframeTag)) {
                                spectatedEntityID = in.nextInt();
                            }
                        }

                        if(spectatedEntityID != null) {
                            newKeyframe.setValue(newKeyframe.getValue().asSpectatorData(spectatedEntityID));
                        }

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

                } else if("customObjects".equals(jsonTag)) {
                    CustomImageObject[] customObjects = new Gson().fromJson(in, CustomImageObject[].class);

                    set.setCustomObjects(customObjects);
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
