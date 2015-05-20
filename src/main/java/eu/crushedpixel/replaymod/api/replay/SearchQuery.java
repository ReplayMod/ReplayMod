package eu.crushedpixel.replaymod.api.replay;

import java.lang.reflect.Field;
import java.net.URLEncoder;

public class SearchQuery {

    public Boolean order, singleplayer;
    public String player, tag, version, server, name, auth;
    public Integer category, offset;

    public SearchQuery() {
    }

    public SearchQuery(Boolean order, Boolean singleplayer, String player,
                       String tag, String version, String server, String name,
                       String auth, Integer category, Integer offset) {
        this.order = order;
        this.singleplayer = singleplayer;
        this.player = player;
        this.tag = tag;
        this.version = version;
        this.server = server;
        this.name = name;
        this.auth = auth;
        this.category = category;
        this.offset = offset;
    }

    public String buildQuery() {
        String query = "";
        boolean first = true;

        //Please don't slaughter me for this code,
        //even if I deserve it, which I certainly do.
        for(Field f : this.getClass().getDeclaredFields()) {
            try {
                Object value = f.get(this);
                if(value == null) continue;
                query += first ? "?" : "&";
                first = false;
                query += f.getName() + "=";
                query += URLEncoder.encode(String.valueOf(value), "UTF-8");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        return query;
    }

    @Override
    public String toString() {
        return buildQuery();
    }
}
