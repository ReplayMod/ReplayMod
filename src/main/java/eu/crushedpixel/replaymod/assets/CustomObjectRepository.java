package eu.crushedpixel.replaymod.assets;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class CustomObjectRepository {

    public CustomObjectRepository() {
        this.objects = new ArrayList<CustomImageObject>();
    }

    public void setObjects(List<CustomImageObject> objects) {
        this.objects = new ArrayList<CustomImageObject>(objects);
    }

    @Getter
    private ArrayList<CustomImageObject> objects;

}
