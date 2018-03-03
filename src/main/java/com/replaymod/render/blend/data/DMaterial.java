package com.replaymod.render.blend.data;

import com.replaymod.render.blend.Util;
import org.blender.dna.MTex;
import org.blender.dna.Material;
import org.blender.dna.Tex;
import org.cakelab.blender.io.block.BlockCodes;
import org.cakelab.blender.nio.CArrayFacade;
import org.cakelab.blender.nio.CPointer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DMaterial {
    public final DId id = new DId(BlockCodes.ID_MA);
    public List<DMTex> textures = new ArrayList<>();

    public CPointer<Material> serialize(Serializer serializer) throws IOException {
        return serializer.maybeMajor(this, id, Material.class, () -> {
            List<Util.IOCallable<CPointer<MTex>>> textures = new ArrayList<>();
            for (DMTex texture : this.textures) {
                textures.add(texture.serialize(serializer));
            }
            return material -> {
                material.setMode(
                        0x10000 /* MA_TRANSP */ | 0x40 /* MA_ZTRANS */ | 0x80 /* MA_VERTEXCOLP */
                );
                material.setAlpha(1);
                material.setRef(1);
                material.setR(1);
                material.setG(1);
                material.setB(1);
                CArrayFacade<CPointer<MTex>> mTexPointers = material.getMtex();
                for (int i = 0; i < textures.size(); i++) {
                    mTexPointers.set(i, textures.get(i).call());
                }
            };
        });
    }

    public static class DMTex {
        public DTexture texture;

        public Util.IOCallable<CPointer<MTex>> serialize(Serializer serializer) throws IOException {
            CPointer<Tex> texture = this.texture.serialize(serializer);
            return () -> {
                MTex mTex = serializer.writeData(MTex.class);
                mTex.setTex(texture);
                mTex.getUvname().fromString("UVMap");
                mTex.setMapto((short) (
                        0x1 /* MAP_COL */ | 0x80 /* MAP_ALPHA */
                ));
                mTex.setBlendtype((short) 0x1 /* MTEX_MUL */);
                mTex.setColfac(1);
                mTex.setAlphafac(1);
                mTex.setDef_var(1);
                mTex.setTexco((short) 16);
                mTex.setProjx((byte) 1);
                mTex.setProjy((byte) 2);
                mTex.setProjz((byte) 3);
                CArrayFacade<Float> size = mTex.getSize();
                size.set(0, 1f);
                size.set(1, 1f);
                size.set(2, 1f);
                return mTex.__io__addressof();
            };
        }
    }
}
