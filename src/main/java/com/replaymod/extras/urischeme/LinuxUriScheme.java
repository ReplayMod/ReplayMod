package com.replaymod.extras.urischeme;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class LinuxUriScheme extends UriScheme {

    @Override
    public void install() throws URISyntaxException, IOException {
        File file = new File("replaymod.desktop");
        File iconFile = new File("replaymod-icon.jpg");
        String path = findJarFile().getAbsolutePath().replace("\\", "\\\\").replace("\"", "\\\"");
        String content =
                "[Desktop Entry]\n" +
                "Name=ReplayMod\n" +
                "Exec=java -cp \"" + path + "\" " + UriScheme.class.getName() + " %u\n" +
                "Icon=" + iconFile.getAbsolutePath().replace("\\", "\\\\").replace("\"", "\\\"") + "\n" +
                "Type=Application\n" +
                "Terminal=false\n" +
                "NoDisplay=true\n" +
                "MimeType=x-scheme-handler/replaymod;";

        FileOutputStream out = new FileOutputStream(file);
        try {
            IOUtils.write(content, out);
        } finally {
            out.close();
        }

        InputStream in = LinuxUriScheme.class.getResourceAsStream("/assets/replaymod/logo.jpg");
        try {
            out = new FileOutputStream(iconFile);
            try {
                IOUtils.copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }

        String[] command = {"xdg-desktop-menu", "install", "--novendor", "replaymod.desktop"};
        Process process = new ProcessBuilder().command(command).start();
        try {
            if (process.waitFor() != 0) {
                StringBuilderWriter writer = new StringBuilderWriter();
                IOUtils.copy(process.getInputStream(), writer);
                IOUtils.copy(process.getErrorStream(), writer);
                throw new IOException(writer.toString());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        FileUtils.deleteQuietly(file);
    }
}
