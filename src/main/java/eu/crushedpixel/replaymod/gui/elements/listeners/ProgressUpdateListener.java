package eu.crushedpixel.replaymod.gui.elements.listeners;

public interface ProgressUpdateListener {

    void onProgressChanged(float progress);

    void onProgressChanged(float progress, String progressString);

}
