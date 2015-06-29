package eu.crushedpixel.replaymod.api.replay.holders;

import lombok.Data;

@Data
public class FileRating {

    private int file;
    private boolean rating;

    public int getFileID() {
        return file;
    }

    // TODO Will be changed later
    public boolean getRating() {
        return rating;
    }
}
