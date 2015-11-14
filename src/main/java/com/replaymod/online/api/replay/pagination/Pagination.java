package com.replaymod.online.api.replay.pagination;

import com.replaymod.online.api.replay.holders.FileInfo;

import java.util.List;

public interface Pagination {
    List<FileInfo> getFiles();

    int getLoadedPages();

    boolean fetchPage();

    int PAGE_SIZE = 30; //defined by the Website API
}
