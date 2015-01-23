package eu.crushedpixel.replaymod.api.client.holders;

import eu.crushedpixel.replaymod.recording.ReplayMetaData;

public class FileInfo {

	private int id;
	private ReplayMetaData metadata;
	private String owner;
	private Rating ratings;
	private int size;
	private int category;
	
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
	
}
