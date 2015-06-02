package eu.crushedpixel.replaymod.registry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReplayFileAppender extends Thread {

    private Multimap<File, Pair<File, String>> filesToMove = ArrayListMultimap.create();
    private Queue<File> filesToRewrite = new ConcurrentLinkedQueue<File>();

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
        //first, remove any files with the same name assigned to this Replay File
        if(filesToMove.get(replayFile) != null) {
            for(Pair<File, String> p : new ArrayList<Pair<File, String>>((Collection<Pair<File, String>>)filesToMove.get(replayFile))) {
                if(p.getRight().equals(name)) {
                    filesToMove.remove(replayFile, p);
                }
            }
        }

        //then, assign the file/name pair to the Replay File
        filesToMove.put(replayFile, Pair.of(toAdd, name));

        //finally, add the Replay File to the Queue if not already contained
        if(!filesToRewrite.contains(replayFile)) {
            filesToRewrite.add(replayFile);
        }
    }

    public void shutdown() {
        shutdown = true;
    }

    public void addFinishListener(GuiReplaySaving gui) {
        listeners.add(gui);
    }

    @Override
    public void run() {
        while(!shutdown || !filesToRewrite.isEmpty()) {
            File replayFile = filesToRewrite.poll();
            if(replayFile != null) {
                if(replayFile.canWrite()) {
                    try {
                        HashMap<String, File> toAdd = new HashMap<String, File>();
                        for(Pair<File, String> p : filesToMove.get(replayFile)) {
                            toAdd.put(p.getRight(), p.getLeft());
                        }
                        ReplayFileIO.addFilesToZip(replayFile, toAdd);

                        //delete all written files
                        for(Pair<File, String> p : filesToMove.get(replayFile)) {
                            p.getLeft().delete();
                        }
                        filesToMove.removeAll(replayFile);
                    } catch(Exception e) {
                        e.printStackTrace();
                        filesToRewrite.add(replayFile);
                    } finally {
                        callListeners();
                    }

                } else {
                    filesToRewrite.add(replayFile);
                }
            }
            try {
                Thread.sleep(1000);
            } catch(Exception e) {}
        }
    }

    public void callListeners() {
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
