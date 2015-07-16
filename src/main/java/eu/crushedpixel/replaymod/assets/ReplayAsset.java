package eu.crushedpixel.replaymod.assets;

import eu.crushedpixel.replaymod.holders.GuiEntryListEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ReplayAsset<T> extends GuiEntryListEntry {

    String getSavedFileExtension();

    void loadFromStream(InputStream inputStream) throws IOException;
    void writeToStream(OutputStream outputStream) throws IOException;

    void drawToScreen(int x, int y, int maxWidth, int maxHeight);

    void setAssetName(String name);

    ReplayAsset<T> copy();

    T getObject();

}
