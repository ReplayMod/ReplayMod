package eu.crushedpixel.replaymod.registry;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileCopyHandler extends Thread {

    private Queue<Pair<File, File>> filesToMove = new ConcurrentLinkedQueue<Pair<File, File>>();
    private boolean shutdown = false;

    public FileCopyHandler() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                FileCopyHandler.this.shutdown();
            }
        }));
    }

    public void registerModifiedFile(File tempFile, File destination) {
        filesToMove.add(Pair.of(tempFile, destination));

        for(Pair<File, File> p : new ArrayList<Pair<File, File>>(filesToMove)) {
            if(p.equals(tempFile)) {
                filesToMove.remove(p);
            }
        }
    }

    public void shutdown() {
        shutdown = true;
    }

    @Override
    public void run() {
        while(!shutdown || !filesToMove.isEmpty()) {
            Pair<File, File> mv = filesToMove.poll();
            if(mv != null) {
                try {
                    Files.move(mv.getLeft().toPath(), mv.getRight().toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch(IOException e) {
                    filesToMove.add(mv);
                }
            }
            try {
                Thread.sleep(1000);
            } catch(Exception e) {
            }
        }
    }

}
