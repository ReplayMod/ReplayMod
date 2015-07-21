package eu.crushedpixel.replaymod.utils;

public class InterpolationUtils {

    /**
     * Note: I invented the word "Euler break". If there are any better suggestions, let me know.
     * @param first The previous, fixed Rotation value
     * @param second The new Rotation value
     * @param eulerBreak The Euler break, e.g. 180 for Minecraft's Camera Yaw
     * @return The new Rotation value, modified to make the Interpolation algorithms
     * find the closest path between two Euler Rotation values
     */
    public static double fixEulerRotation(double first, double second, int eulerBreak) {
        if(first == second) return first;

        //converting the values to values between 0 and 359,
        //essentially moving the euler break to 0
        double normalizedFirst = (first + eulerBreak) % 360;
        double normalizedSecond = (second + eulerBreak) % 360;

        //the difference between the rotation values
        //if using the "conventional" path
        double pathDifference = Math.abs(normalizedSecond-normalizedFirst);

        int factor = normalizedSecond > normalizedFirst ? 1 : -1;

        //if the "conventional" path takes more than half the rotation,
        //use the path crossing the euler break
        if(pathDifference > 180) {
            //invert the path difference to rotate in the other direction
            pathDifference = -1*(360-pathDifference);
        }

        return first + factor*pathDifference;
    }
}
