package com.replaymod.online.api.replay.pagination;

import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.replay.SearchQuery;
import com.replaymod.online.api.replay.holders.FileInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchPagination implements Pagination {

    private final ApiClient apiClient;
    private final SearchQuery searchQuery;
    private int page;
    private List<FileInfo> files = new ArrayList<FileInfo>();

    public SearchPagination(ApiClient apiClient, SearchQuery searchQuery) {
        this.apiClient = apiClient;
        this.page = -1;
        this.searchQuery = searchQuery;
    }

    @Override
    public List<FileInfo> getFiles() {
        return Collections.unmodifiableList(files);
    }

    @Override
    public int getLoadedPages() {
        return page;
    }

    @Override
    public boolean fetchPage() {
        page++;
        searchQuery.offset = page;

        try {
            FileInfo[] fis = apiClient.searchFiles(searchQuery);
            if(fis.length < 1) {
                page--;
                return false;
            }

            files.addAll(Arrays.asList(fis));

            return true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        page--;
        return false;
    }
}
