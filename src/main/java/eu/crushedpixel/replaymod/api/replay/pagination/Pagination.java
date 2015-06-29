package eu.crushedpixel.replaymod.api.replay.pagination;

import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;

import java.util.List;

public interface Pagination {
    List<FileInfo> getFiles();

    int getLoadedPages();

    boolean fetchPage();

    int PAGE_SIZE = 30; //defined by the Website API
}
