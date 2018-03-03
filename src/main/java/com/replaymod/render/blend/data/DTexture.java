package com.replaymod.render.blend.data;

import org.blender.dna.Image;
import org.blender.dna.Tex;
import org.cakelab.blender.io.block.BlockCodes;
import org.cakelab.blender.nio.CPointer;

import java.io.IOException;

public class DTexture {
    public final DId id = new DId(BlockCodes.ID_TE);
    public DImage image;

    public CPointer<Tex> serialize(Serializer serializer) throws IOException {
        return serializer.maybeMajor(this, id, Tex.class, () -> {
            CPointer<Image> image = this.image.serialize(serializer);
            return tex -> {
                tex.setIma(image);
                tex.setType((short) 8 /* TEX_IMAGE */);
                tex.setImaflag((short) 2 /* TEX_USEALPHA */);
                tex.setCropxmax(1);
                tex.setCropymax(1);
                tex.setRfac(1);
                tex.setGfac(1);
                tex.setBfac(1);
                tex.setBright(1);
                tex.setContrast(1);
                tex.setSaturation(1);
            };
        });
    }
}
