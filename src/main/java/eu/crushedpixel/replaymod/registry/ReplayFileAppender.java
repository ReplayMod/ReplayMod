package eu.crushedpixel.replaymod.registry;

import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReplayFileAppender extends Thread {

    private Queue<Pair<Pair<File, String>, File>> filesToMove = new ConcurrentLinkedQueue<Pair<Pair<File, String>, File>>();
    private boolean shutdown = false;
    private List<GuiReplaySaving> listeners = new ArrayList<GuiReplaySaving>();

    //this is true if the DataListener is currently busy saving a newly recorded Replay File
    private boolean newReplayFileWriting = false;

    public void startNewReplayFileWriting() {
        newReplayFileWriting = true;

        if(!FMLClientHandler.instance().isGUIOpen(GuiReplaySaving.class)) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    final GuiReplaySaving savingScreen = new GuiReplaySaving(null);

                    Minecraft.getMinecraft().displayGuiScreen(savingScreen);
                }
            });
        }
    }

    public void replayFileWritingFinished() {
        newReplayFileWriting = false;
        callListeners();
    }

    public ReplayFileAppender() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ReplayFileAppender.this.shutdown();
            }
        }, "replaymod-file-appender-shutdown"));
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

    public boolean isBusy() {
        return !filesToMove.isEmpty() && !newReplayFileWriting;
    }

    public void addFinishListener(GuiReplaySaving gui) {
        listeners.add(gui);
    }

    @Override
    public void run() {
        while(!shutdown || !filesToMove.isEmpty()) {
            Pair<Pair<File, String>, File> mv = filesToMove.poll();
            if(mv != null) {
                if(mv.getRight().canWrite()) {
                    try {
                        ReplayFileIO.addFileToZip(mv.getRight(), mv.getLeft().getLeft(), mv.getLeft().getRight());
                    } catch(Exception e) {
                        e.printStackTrace();
                        filesToMove.add(mv);
                    } finally {
                        callListeners();
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

    private void callListeners() {
        if(filesToMove.isEmpty() && !newReplayFileWriting) {
            for(final GuiReplaySaving gui : listeners) {
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        gui.dispatch();
                    }
                });
            }

            listeners = new ArrayList<GuiReplaySaving>();
        }
    }

}
