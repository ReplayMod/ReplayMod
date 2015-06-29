package eu.crushedpixel.replaymod.api.replay.holders;

import lombok.Data;

@Data
public class UserFiles {
    private String user;
    private FileInfo[] files;
    private int total_size;
}
