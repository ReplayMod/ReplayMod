package eu.crushedpixel.replaymod.registry;

import com.google.common.primitives.Ints;
import com.replaymod.core.ReplayMod;
import eu.crushedpixel.replaymod.api.ApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FavoritedFileHandler {

    private List<Integer> favorited = new ArrayList<Integer>();

    boolean retrieved = false;

    public FavoritedFileHandler() {
        reloadFavorites();
    }

    public List<Integer> getFavorited() { return favorited; }

    public boolean isFavorited(int id) {
        return favorited.contains(id);
    }

    public void addToFavorites(Integer id) throws IOException, ApiException {
        ReplayMod.apiClient.favFile(id, true);
        favorited.remove(id);
        favorited.add(id);
    }

    public void removeFromFavorites(Integer id) throws IOException, ApiException {
        ReplayMod.apiClient.favFile(id, false);
        favorited.remove(id);
    }

    public void reloadFavorites() {
        if(ReplayMod.apiClient.isLoggedIn()) {
            try {
                int[] ids = ReplayMod.apiClient.getFavorites();
                favorited = new ArrayList<Integer>(Ints.asList(ids));
                retrieved = true;
            } catch(Exception e) {
                retrieved = false;
                favorited = new ArrayList<Integer>();
                e.printStackTrace();
            }
        } else {
            retrieved = false;
            favorited = new ArrayList<Integer>();
        }
    }

}
