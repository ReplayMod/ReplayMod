package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.ApiException;
import eu.crushedpixel.replaymod.api.replay.holders.FileRating;
import eu.crushedpixel.replaymod.api.replay.holders.Rating;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;

import java.io.IOException;
import java.util.HashMap;

public class RatedFileHandler {

    private HashMap<Integer, Boolean> rated = new HashMap<Integer, Boolean>();

    boolean retrieved = false;

    public RatedFileHandler() {
        reloadRatings();
    }

    public HashMap<Integer, Boolean> getRated() {
        return rated;
    }

    public Rating.RatingType getRating(int id) {
        return Rating.RatingType.fromBoolean(rated.get(id));
    }

    public void rateFile(int id, Rating.RatingType type) throws IOException, ApiException {
        if(type == Rating.RatingType.LIKE || type == Rating.RatingType.DISLIKE) {
            rated.put(id, type == Rating.RatingType.LIKE);
        } else {
            rated.remove(id);
        }
        ReplayMod.apiClient.rateFile(AuthenticationHandler.getKey(), id, type);
    }

    public void reloadRatings() {
        if(AuthenticationHandler.isAuthenticated()) {
            try {
                FileRating[] ratings = ReplayMod.apiClient.getRatedFiles(AuthenticationHandler.getKey());
                rated = new HashMap<Integer, Boolean>();

                for(FileRating fr : ratings) {
                    rated.put(fr.getFileID(), fr.getRating());
                }

                retrieved = true;
            } catch(Exception e) {
                retrieved = false;
                rated = new HashMap<Integer, Boolean>();
                e.printStackTrace();
            }
        } else {
            retrieved = false;
            rated = new HashMap<Integer, Boolean>();
        }
    }

}
