package eu.crushedpixel.replaymod.studio;

public class VersionValidator {

    public static final boolean isValid;

    static {
        String version = Runtime.class.getPackage().getSpecificationVersion();
        if(version != null) {
            String[] split = version.split("\\.");
            isValid = split.length > 1 && Integer.valueOf(split[1]) >= 7;
        } else {
            isValid = false;
        }
    }
}
