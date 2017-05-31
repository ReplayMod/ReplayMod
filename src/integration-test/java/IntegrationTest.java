import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for running integration tests from within the IDE.
 * This is <b>not</b> called when running integration tests directly from gradle.
 */
public class IntegrationTest {
    public static void main(String[] args) throws Throwable {
        // Prevent MC from grabbing our mouse during the test (it's not needed)
        System.setProperty("fml.noGrab", "true");

        // Make sure the test folder exists and is fresh
        File gameDir = new File(System.getProperty("user.dir"), "integration-test");
        if (gameDir.exists()) {
            FileUtils.forceDelete(gameDir);
        }
        FileUtils.forceMkdir(gameDir);

        // Set game dir to test folder and call regular entry point
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        argsList.add("--gameDir");
        argsList.add(gameDir.getCanonicalPath());
        GradleStart.main(argsList.toArray(new String[argsList.size()]));
    }
}
