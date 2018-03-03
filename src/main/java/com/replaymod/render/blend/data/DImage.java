package com.replaymod.render.blend.data;

import org.apache.commons.lang3.tuple.Pair;
import org.blender.dna.Image;
import org.blender.dna.ImagePackedFile;
import org.cakelab.blender.io.block.BlockCodes;
import org.cakelab.blender.nio.CPointer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DImage {
    public final DId id = new DId(BlockCodes.ID_IM);
    public String filePath;
    public List<Pair<String, DPackedFile>> packedFiles = new ArrayList<>();

    public CPointer<Image> serialize(Serializer serializer) throws IOException {
        return serializer.maybeMajor(this, id, Image.class, () -> {
            return image -> {
                image.getName().fromString(String.valueOf(filePath));
                image.setSource((short) 1);
                image.getColorspace_settings().getName().fromString("sRGB");
                image.setAspx(1);
                image.setAspy(1);
                serializer.writeDataList(ImagePackedFile.class, image.getPackedfiles(), packedFiles.size(), (i, pf) -> {
                    Pair<String, DPackedFile> pair = packedFiles.get(i);
                    pf.getFilepath().fromString(pair.getLeft());
                    pf.setPackedfile(pair.getRight().serialize(serializer));
                });
            };
        });
    }
}
