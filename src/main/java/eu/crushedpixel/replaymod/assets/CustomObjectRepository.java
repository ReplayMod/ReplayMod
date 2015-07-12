package eu.crushedpixel.replaymod.assets;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;

@Data
@AllArgsConstructor
public class CustomObjectRepository {

    public CustomObjectRepository() {
        this.objects = new ArrayList<CustomImageObject>();
    }

    private ArrayList<CustomImageObject> objects;

}
