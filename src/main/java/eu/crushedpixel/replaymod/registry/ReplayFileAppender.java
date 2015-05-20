package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReplayFileAppender extends Thread {

    private Queue<Pair<Pair<File, String>, File>> filesToMove = new ConcurrentLinkedQueue<Pair<Pair<File, String>, File>>();
    private boolean shutdown = false;

    public ReplayFileAppender() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ReplayFileAppender.this.shutdown();
            }
        }));
    }

    public void registerModifiedFile(File toAdd, String name, File replayFile) {
        for(Pair<Pair<File, String>, File> p : new ArrayList<Pair<Pair<File, String>, File>>(filesToMove)) {
            if(p.getLeft().getRight().equals(name) && p.getRight().equals(replayFile)) {
                filesToMove.remove(p);
            }
        }

        filesToMove.add(Pair.of(Pair.of(toAdd, name), replayFile));
    }

    public void shutdown() {
        shutdown = true;
    }

    @Override
    public void run() {
        while(!shutdown || !filesToMove.isEmpty()) {
            Pair<Pair<File, String>, File> mv = filesToMove.poll();
            if(mv != null) {
                if(mv.getRight().canWrite()) {
                    try {
                        if(mv.getLeft().getLeft() == null) {
                            ReplayFileIO.removeFileFromZip(mv.getRight(), mv.getLeft().getRight());
                        } else {
                            ReplayFileIO.addFileToZip(mv.getRight(), mv.getLeft().getLeft(), mv.getLeft().getRight());
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                        filesToMove.add(mv);
                    }
                } else {
                    filesToMove.add(mv);
                }
            }
            try {
                Thread.sleep(1000);
            } catch(Exception e) {}
        }
    }

}
