package eu.crushedpixel.replaymod.gui.replayeditor;

import eu.crushedpixel.replaymod.gui.elements.listeners.ProgressUpdateListener;
import net.minecraft.client.gui.GuiScreen;

import java.io.File;
import java.io.IOException;

public abstract class GuiStudioPart extends GuiScreen implements ProgressUpdateListener {

    protected int yPos = 0;

    public GuiStudioPart(int yPos) {
        this.yPos = yPos;
    }

    public abstract void applyFilters(File replayFile, File outputFile);

    public abstract void cancelFilters();

    public abstract String getDescription();

    public abstract String getTitle();

    @Override
    public abstract void keyTyped(char typedChar, int keyCode);

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private float progress;
    private String progressString;

    public float getProgress() {
        return progress;
    }

    public String getFilterProgressString() {
        return progressString;
    }

    @Override
    public void onProgressChanged(float progress, String progressString) {
        this.progress = progress;
        this.progressString = progressString;
    }

    @Override
    public void onProgressChanged(float progress) {
        this.progress = progress;
    }

    public boolean validateInputs() {
        return true;
    }

}
