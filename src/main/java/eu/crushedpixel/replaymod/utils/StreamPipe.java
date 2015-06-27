package eu.crushedpixel.replaymod.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamPipe extends Thread {
    private final InputStream in;
    private final OutputStream out;

    public StreamPipe(InputStream in, OutputStream out) {
        super("StreamPipe from " + in + " to " + out);
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            IOUtils.copy(in, out);
        } catch (IOException ignored) {
            // We don't care
            // Note: Once we use this for something important, we should probably care!
        }
    }
}
