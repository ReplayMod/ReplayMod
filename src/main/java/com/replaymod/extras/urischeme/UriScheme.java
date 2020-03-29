package com.replaymod.extras.urischeme;

import net.minecraft.util.SystemUtil;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Arrays;

public abstract class UriScheme {
    public static final String PROTOCOL = "replaymod://";
    public static final int PROCESS_PORT = 11754;

    public static void main(String[] args) {
        try {
            System.out.println("Args: " + Arrays.toString(args));

            if (args.length == 1 && args[0].startsWith(PROTOCOL)) {
                int id = Integer.parseInt(args[0].substring(PROTOCOL.length()));
                try {
                    Socket socket = new Socket(InetAddress.getLocalHost(), PROCESS_PORT);
                    socket.getOutputStream().write(String.valueOf(id).getBytes());
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();

                    launchNewInstance(id);
                }
            } else {
                launchNewInstance(null);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to start Minecraft launcher. Saving exception to replaymod-crash.txt");
            try {
                FileOutputStream out = new FileOutputStream("replaymod-crash.txt");
                t.printStackTrace(new PrintStream(out));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static void launchNewInstance(Integer replayId) throws IOException {
        // Determine launcher.jar location
        String osName = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home", ".");
        File dir;
        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            dir = new File(appData != null ? appData : userHome, ".minecraft/");
        } else if (osName.contains("mac")) {
            dir = new File(userHome, "Library/Application Support/minecraft");
        } else if (osName.contains("linux") || osName.contains("unix")) {
            dir = new File(userHome, ".minecraft/");
        } else {
            dir = new File(userHome, "minecraft/");
        }

        // Launch process
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "launcher.jar").directory(dir);
        if (replayId != null) {
            processBuilder.environment().put("replaymod.uri.replayid", String.valueOf(replayId));
        }
        processBuilder.start();
    }

    public static UriScheme create() {
        switch (SystemUtil.getOperatingSystem()) {
            case LINUX:
                return new LinuxUriScheme();
            case WINDOWS:
                return new WindowsUriScheme();
            case OSX:
                return new OSXUriScheme();
            case SOLARIS:
            case UNKNOWN:
            default:
                return null;
        }
    }

    public abstract void install() throws Exception;

    File findJarFile() throws IOException {
        CodeSource codeSource = UriScheme.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            if (location != null) {
                try {
                    return new File(location.toURI());
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
        }
        throw new IOException("No jar file found. Is this a development environment?");
    }
}
