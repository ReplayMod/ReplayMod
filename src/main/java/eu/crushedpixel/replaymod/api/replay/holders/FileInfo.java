package eu.crushedpixel.replaymod.api.replay.holders;

import eu.crushedpixel.replaymod.recording.ReplayMetaData;

public class FileInfo {

    private int id;
    private ReplayMetaData metadata;
    private String owner;
    private Rating ratings;
    private int size;
    private int category;
    private int downloads;
    private int favorites;
    private String name;
    private boolean thumbnail;


    public FileInfo(int id, ReplayMetaData metadata, String owner,
                    Rating ratings, int size, int category, int downloads, String name,
                    boolean thumbnail, int favorites) {
        this.id = id;
        this.metadata = metadata;
        this.owner = owner;
        this.ratings = ratings;
        this.size = size;
        this.category = category;
        this.downloads = downloads;
        this.name = name;
        this.thumbnail = thumbnail;
        this.favorites = favorites;
    }

    public int getId() {
        return id;
    }

    public ReplayMetaData getMetadata() {
        return metadata;
    }

    public String getOwner() {
        return owner;
    }

    public Rating getRatings() {
        return ratings;
    }

    public int getSize() {
        return size;
    }

    public int getCategory() {
        return category;
    }

    public int getDownloads() {
        return downloads;
    }

    public int getFavorites() { return favorites; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasThumbnail() {
        return thumbnail;
    }


}
