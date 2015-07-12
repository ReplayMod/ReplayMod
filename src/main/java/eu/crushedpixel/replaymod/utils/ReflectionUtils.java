package eu.crushedpixel.replaymod.utils;

import eu.crushedpixel.replaymod.interpolation.Interpolate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtils {

    public static List<Field> getFieldsToInterpolate(Class clazz) {
        List<Field> fields = new ArrayList<Field>();
        for(Field f : clazz.getDeclaredFields()) {
            if(f.isAnnotationPresent(Interpolate.class)) fields.add(f);
        }

        if(clazz.getSuperclass() != Object.class) {
            fields.addAll(getFieldsToInterpolate(clazz.getSuperclass()));
        }

        return fields;
    }

}
