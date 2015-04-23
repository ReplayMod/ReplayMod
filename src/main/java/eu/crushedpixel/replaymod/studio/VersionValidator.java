package eu.crushedpixel.replaymod.studio;

public class VersionValidator {

    public static final boolean isValid;

    static {
        String version = Runtime.class.getPackage().getSpecificationVersion();
        if(version != null) {
            String[] split = version.split("\\.");
            if(split.length > 1) {
                isValid = Integer.valueOf(split[1]) >= 7;
            } else {
                isValid = false;
            }
        } else {
            isValid = false;
        }
    }
}
