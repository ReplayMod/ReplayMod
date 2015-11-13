package com.replaymod.extras.urischeme;

import com.replaymod.core.ReplayMod;
import com.replaymod.extras.Extra;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class UriSchemeExtra implements Extra {
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
        // TODO
//        File file = ReplayMod.downloadedFileHandler.getFileForID(id);
//        if (file == null) {
//            FileInfo info = new FileInfo(id, null, null, null, 0, 0, 0, String.valueOf(id), false, 0);
//            new GuiReplayDownloading(info).display();
//        } else {
//            try {
//                ReplayHandler.startReplay(file);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }
}
