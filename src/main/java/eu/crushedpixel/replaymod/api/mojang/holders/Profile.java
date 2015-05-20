package eu.crushedpixel.replaymod.api.mojang.holders;

public class Profile {

    private String id;
    private String name;
    private Properties[] properties;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Properties[] getProperties() {
        return properties;
    }

    public void setProperties(Properties[] properties) {
        this.properties = properties;
    }
}
