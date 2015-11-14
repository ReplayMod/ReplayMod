package com.replaymod.online.api.replay.holders;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class FileRating {
    private int file;
    @SerializedName("rating") private boolean ratingPositive;
}
