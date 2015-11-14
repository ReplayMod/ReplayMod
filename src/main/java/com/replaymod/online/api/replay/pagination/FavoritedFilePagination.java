package com.replaymod.online.api.replay.pagination;

import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.replay.holders.FileInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FavoritedFilePagination implements Pagination {

    private final ApiClient apiClient;
    private int page;
    private HashMap<Integer, FileInfo> files = new HashMap<>();

    public FavoritedFilePagination(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.page = -1;
    }

    @Override
    public List<FileInfo> getFiles() {
        return new ArrayList<>(files.values());
    }

    @Override
    public int getLoadedPages() {
        return page;
    }

    @Override
    public boolean fetchPage() {
        page++;

        try {
            int[] f = apiClient.getFavorites();
            List<Integer> toAdd = new ArrayList<>();
            for(int i : f) {
                if(!files.containsKey(i)) {
                    toAdd.add(i);
                    if(toAdd.size() >= Pagination.PAGE_SIZE) break;
                }
            }

            FileInfo[] fis = apiClient.getFileInfo(toAdd);
            if(fis.length < 1) {
                page--;
                return false;
            }

            for(FileInfo info : fis) {
                files.put(info.getId(), info);
            }

            return true;
        } catch(Exception e) {
            e.printStackTrace();
        }
        page--;
        return false;
    }
}
