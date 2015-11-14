package com.replaymod.online.api.replay.holders;

import lombok.Data;

@Data
public class UserFiles {
    private String user;
    private FileInfo[] files;
    private int total_size;
}
