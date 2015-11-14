package com.replaymod.extras.urischeme;

import com.replaymod.core.ReplayMod;
import com.replaymod.extras.Extra;
import com.replaymod.online.ReplayModOnline;
import de.johni0702.minecraft.gui.container.GuiScreen;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class UriSchemeExtra implements Extra {
    @Mod.Instance(ReplayModOnline.MOD_ID)
    private static ReplayModOnline module;

    private ReplayMod mod;

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.mod = mod;

        UriScheme uriScheme = UriScheme.create();
        if (uriScheme == null) {
            throw new UnsupportedOperationException("OS not supported.");
        }
        uriScheme.install();

        mod.runLater(new Runnable() {
            @Override
            public void run() {
                // Start listening for future requests
                startListener();

                // Handle initial request
                String replayId = System.getenv("replaymod.uri.replayid");
                if (replayId != null) {
                    loadReplay(Integer.parseInt(replayId));
                }
            }
        });
    }

    private void startListener() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                try {
                    serverSocket = new ServerSocket(UriScheme.PROCESS_PORT);
                    while (!Thread.interrupted()) {
                        Socket clientSocket = serverSocket.accept();
                        try {
                            InputStream inputStream = clientSocket.getInputStream();
                            String replayId = IOUtils.toString(inputStream);
                            final int id = Integer.parseInt(replayId);
                            mod.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    loadReplay(id);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            IOUtils.closeQuietly(clientSocket);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(serverSocket);
                }
            }
        }, "UriSchemeHandler").start();
    }

    private void loadReplay(int id) {
        try {
            module.startReplay(id, "Replay #" + id, GuiScreen.wrap(null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
