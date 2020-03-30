package com.replaymod.online.api.replay.holders;

import com.replaymod.replaystudio.replay.ReplayMetaData;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class FileInfo {
    private int id;
    private ReplayMetaData metadata;
    private String owner;
    private Rating ratings;
    private int size;
    private int category;
    private int downloads;
    private String name;
    @Getter(AccessLevel.NONE)
    private boolean thumbnail;
    private int favorites;

    public boolean hasThumbnail() {
        return thumbnail;
    }
}
