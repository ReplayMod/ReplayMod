package eu.crushedpixel.replaymod.api.client;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.client.holders.FileInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchPagination {

    private final SearchQuery searchQuery;
    private int page;
    private List<FileInfo> files = new ArrayList<FileInfo>();

    public SearchPagination(SearchQuery searchQuery) {
        this.page = -1;
        this.searchQuery = searchQuery;
    }

    public List<FileInfo> getFiles() {
        return new ArrayList<FileInfo>(files);
    }

    public int getLoadedPages() {
        return page;
    }

    public boolean fetchPage() {
        page++;
        searchQuery.offset = page;

        try {
            FileInfo[] fis = ReplayMod.apiClient.searchFiles(searchQuery);
            if(fis.length <= 1) {
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
