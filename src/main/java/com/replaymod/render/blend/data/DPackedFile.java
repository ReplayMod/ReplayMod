package com.replaymod.render.blend.data;

import org.blender.dna.PackedFile;
import org.cakelab.blender.nio.CPointer;

import java.io.IOException;

public class DPackedFile {
    public byte[] data;

    public DPackedFile() {}

    public DPackedFile(byte[] data) {
        this.data = data;
    }

    public CPointer<PackedFile> serialize(Serializer serializer) throws IOException {
        PackedFile packedFile = serializer.writeData(PackedFile.class);
        packedFile.setSize(data.length);
        packedFile.setSeek(0);
        packedFile.setData(serializer.writeBytes(data).cast(Object.class));
        return packedFile.__io__addressof();
    }
}
