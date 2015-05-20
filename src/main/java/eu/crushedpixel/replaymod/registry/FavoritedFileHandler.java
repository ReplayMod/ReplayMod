package eu.crushedpixel.replaymod.registry;

import com.google.common.primitives.Ints;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.ApiException;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;

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
        favorited.remove(id);
        favorited.add(id);
        ReplayMod.apiClient.favFile(AuthenticationHandler.getKey(), id, true);
    }

    public void removeFromFavorites(Integer id) throws IOException, ApiException {
        favorited.remove(id);
        ReplayMod.apiClient.favFile(AuthenticationHandler.getKey(), id, false);
    }

    public void reloadFavorites() {
        if(AuthenticationHandler.isAuthenticated()) {
            try {
                int[] ids = ReplayMod.apiClient.getFavorites(AuthenticationHandler.getKey());
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
