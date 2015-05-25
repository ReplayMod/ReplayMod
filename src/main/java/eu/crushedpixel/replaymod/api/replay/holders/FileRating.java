package eu.crushedpixel.replaymod.api.replay.holders;

public class FileRating {

    private int file;
    private String rating;

    public int getFileID() {
        return file;
    }

    public boolean getRating() {
        return rating.equals("1");
    }
}
