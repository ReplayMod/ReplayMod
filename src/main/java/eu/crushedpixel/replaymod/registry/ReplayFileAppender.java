package eu.crushedpixel.replaymod.registry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.replaymod.replay.events.ReplayCloseEvent;
import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReplayFileAppender {

    private Multimap<File, Pair<File, String>> filesToMove = ArrayListMultimap.create();
    private Queue<File> filesToRewrite = new ConcurrentLinkedQueue<File>();

    private List<GuiReplaySaving> listeners = new ArrayList<GuiReplaySaving>();

    //this is true if the DataListener is currently busy saving a newly recorded Replay File
    private boolean newReplayFileWriting = false;

    public void startNewReplayFileWriting() {
        newReplayFileWriting = true;

        openGuiSavingScreen();
    }

    private void openGuiSavingScreen() {
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

    public void registerModifiedFile(File toAdd, String name, File replayFile) {
        //first, remove any files with the same name assigned to this Replay File
        for (Iterator<Pair<File, String>> iter = filesToMove.get(replayFile).iterator(); iter.hasNext(); ) {
            if (iter.next().getRight().equals(name)) {
                iter.remove();
            }
        }

        //then, assign the file/name pair to the Replay File
        filesToMove.put(replayFile, Pair.of(toAdd, name));

        //finally, add the Replay File to the Queue if not already contained
        if(!filesToRewrite.contains(replayFile)) {
            filesToRewrite.add(replayFile);
        }
    }

    public void deleteAllFilesByFolder(String folderName, File replayFile) {
        Iterator<Pair<File, String>> iter = filesToMove.get(replayFile).iterator();
        List<String> toDelete = new ArrayList<String>();
        while(iter.hasNext()) {
            Pair<File, String> pair = iter.next();
            if (pair.getRight().startsWith(folderName)) {
                toDelete.add(pair.getRight());
            }
        }

        for(String del : toDelete) {
            registerModifiedFile(null, del, replayFile);
        }

        if(!filesToRewrite.contains(replayFile)) {
            filesToRewrite.add(replayFile);
        }
    }

    public void addFinishListener(GuiReplaySaving gui) {
        listeners.add(gui);
    }

    @SubscribeEvent
    public void onReplayExit(ReplayCloseEvent.Post event) {
        if(!filesToRewrite.isEmpty()) {
            openGuiSavingScreen();
            writeFiles();
        }
    }

    private void writeFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!filesToRewrite.isEmpty()) {
                    File replayFile = filesToRewrite.poll();
                    if(replayFile != null) {
                        if(replayFile.canWrite()) {
                            try {
                                Collection<Pair<File, String>> ftm = filesToMove.get(replayFile);

                                File replayFile2 = replayFile;

                                if(replayFile.getParentFile().equals(ReplayFileIO.getReplayDownloadFolder())) {
                                    String fileName = FilenameUtils.getName(replayFile.getAbsolutePath());
                                    File copyTo = new File(ReplayFileIO.getReplayFolder(), fileName);
                                    copyTo = ReplayFileIO.getNextFreeFile(copyTo);

                                    FileUtils.copyFile(replayFile, copyTo);
                                    replayFile = copyTo;
                                }

                                HashMap<String, File> toAdd = new HashMap<String, File>();
                                for(Pair<File, String> p : ftm) {
                                    if(p.getLeft() == null || p.getLeft().exists()) {
                                        toAdd.put(p.getRight(), p.getLeft());
                                    }
                                }
                                ReplayFileIO.addFilesToZip(replayFile, toAdd);

                                //delete all written files
                                for(Pair<File, String> p : ftm) {
                                    if(p.getLeft() != null) {
                                        try {
                                            FileUtils.forceDelete(p.getLeft());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                filesToMove.removeAll(replayFile2);
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
                }
                callListeners();
            }
        }, "replay-file-appender").start();
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
