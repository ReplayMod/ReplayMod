package com.replaymod.core.files;

import com.replaymod.replaystudio.replay.ReplayFile;

import java.io.IOException;

public class ManagedReplayFile extends DelegatingReplayFile {
    private Runnable onClose;

    public ManagedReplayFile(ReplayFile delegate, Runnable onClose) {
        super(delegate);

        this.onClose = onClose;
    }

    @Override
    public void close() throws IOException {
        super.close();

        onClose.run();
        onClose = () -> {};
    }
}
