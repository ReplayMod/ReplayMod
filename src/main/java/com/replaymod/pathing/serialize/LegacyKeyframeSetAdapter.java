package com.replaymod.pathing.serialize;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.replaymod.pathing.serialize.LegacyTimelineConverter.*;

public class LegacyKeyframeSetAdapter extends TypeAdapter<KeyframeSet[]> {

    public LegacyKeyframeSetAdapter() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Override
    public KeyframeSet[] read(JsonReader in) throws IOException {
        List<KeyframeSet> sets = new ArrayList<>();

        in.beginArray();
        while(in.hasNext()) { //iterate over all array entries

            KeyframeSet set = new KeyframeSet();
            List<Keyframe> positionKeyframes = new ArrayList<>();
            List<Keyframe> timeKeyframes = new ArrayList<>();

            in.beginObject();
            while(in.hasNext()) { //iterate over all object entries
                String jsonTag = in.nextName();

                if("name".equals(jsonTag)) {
                    set.name = in.nextString();

                    //TODO: Adapt to new Spectator Keyframe system
                } else if("positionKeyframes".equals(jsonTag)) {
                    in.beginArray();
                    while(in.hasNext()) {
                        Keyframe<AdvancedPosition> newKeyframe = new Keyframe<>();
                        Integer spectatedEntityID = null;
                        in.beginObject();
                        while(in.hasNext()) {
                            String jsonKeyframeTag = in.nextName();
                            if("value".equals(jsonKeyframeTag) || "position".equals(jsonKeyframeTag)) {
                                SpectatorData spectatorData = new Gson().fromJson(in, SpectatorData.class);
                                if (spectatorData.spectatedEntityID != null) {
                                    newKeyframe.value = spectatorData;
                                } else {
                                    newKeyframe.value = new AdvancedPosition();
                                    newKeyframe.value.x = spectatorData.x;
                                    newKeyframe.value.y = spectatorData.y;
                                    newKeyframe.value.z = spectatorData.z;
                                    newKeyframe.value.yaw = spectatorData.yaw;
                                    newKeyframe.value.pitch = spectatorData.pitch;
                                    newKeyframe.value.roll = spectatorData.roll;
                                }
                            } else if("realTimestamp".equals(jsonKeyframeTag)) {
                                newKeyframe.realTimestamp = in.nextInt();
                            } else if("spectatedEntityID".equals(jsonKeyframeTag)) {
                                spectatedEntityID = in.nextInt();
                            }
                        }

                        if(spectatedEntityID != null) {
                            AdvancedPosition pos = newKeyframe.value;
                            SpectatorData spectatorData = new SpectatorData();
                            spectatorData.spectatedEntityID = spectatedEntityID;
                            newKeyframe.value = spectatorData;
                            newKeyframe.value.x = pos.x;
                            newKeyframe.value.y = pos.y;
                            newKeyframe.value.z = pos.z;
                            newKeyframe.value.yaw = pos.yaw;
                            newKeyframe.value.pitch = pos.pitch;
                            newKeyframe.value.roll = pos.roll;
                        }

                        in.endObject();

                        positionKeyframes.add(newKeyframe);
                    }
                    in.endArray();

                } else if("timeKeyframes".equals(jsonTag)) {
                    in.beginArray();
                    while(in.hasNext()) {
                        Keyframe<TimestampValue> newKeyframe = new Keyframe<>();

                        in.beginObject();
                        while(in.hasNext()) {
                            String jsonKeyframeTag = in.nextName();
                            if("timestamp".equals(jsonKeyframeTag)) {
                                TimestampValue timestampValue = new TimestampValue();
                                timestampValue.value = in.nextInt();
                                newKeyframe.value = timestampValue;
                            } else if("value".equals(jsonKeyframeTag)) {
                                newKeyframe.value = new Gson().fromJson(in, TimestampValue.class);
                            } else if("realTimestamp".equals(jsonKeyframeTag)) {
                                newKeyframe.realTimestamp = in.nextInt();
                            }
                        }
                        in.endObject();

                        timeKeyframes.add(newKeyframe);
                    }
                    in.endArray();

                } else if("customObjects".equals(jsonTag)) {
                    set.customObjects = new Gson().fromJson(in, CustomImageObject[].class);
                }
            }
            in.endObject();

            set.positionKeyframes = positionKeyframes.toArray(new Keyframe[positionKeyframes.size()]);
            set.timeKeyframes = timeKeyframes.toArray(new Keyframe[timeKeyframes.size()]);
            sets.add(set);
        }
        in.endArray();

        return sets.toArray(new KeyframeSet[sets.size()]);
    }

    @Override
    public void write(JsonWriter out, KeyframeSet[] value) throws IOException {}
}
