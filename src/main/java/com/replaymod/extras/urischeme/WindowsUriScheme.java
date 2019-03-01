package com.replaymod.extras.urischeme;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.IOException;

public class WindowsUriScheme extends UriScheme {
    @Override
    public void install() throws IOException, InterruptedException {
        String path = findJarFile().getAbsolutePath().replace("\\", "\\\\").replace("\"", "\\\"");
        regAdd("\\replaymod /f /ve /d \"URL:replaymod Protocol\"");
        regAdd("\\replaymod /f /v \"URL Protocol\" /d \"\"");
        regAdd("\\replaymod\\shell\\open\\command /f /ve /d \"java -cp \\\"" + path + "\\\" " + UriScheme.class.getName() + " \\\"%1\\\"\"");
    }

    private void regAdd(String args) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("REG ADD HKCU\\Software\\Classes" + args);
        if (process.waitFor() != 0) {
            StringBuilderWriter writer = new StringBuilderWriter();
            IOUtils.copy(process.getInputStream(), writer);
            IOUtils.copy(process.getErrorStream(), writer);
            throw new IOException(writer.toString());
        }
    }
}
