package eu.crushedpixel.replaymod.api.replay;

import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.net.URLEncoder;

@AllArgsConstructor
public class SearchQuery {

    public SearchQuery() {} //empty constructor which allows us to define values later

    public Boolean order, singleplayer;
    public String player, tag, version, server, name, auth;
    public Integer category, offset;

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
