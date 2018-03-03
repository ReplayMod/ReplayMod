package com.replaymod.render.blend.data;

import org.cakelab.blender.io.util.Identifier;

public class DId {
    public final Identifier code;
    public String name;

    public DId(Identifier code) {
        this.code = code;
    }

    public DId(Identifier code, String name) {
        this.code = code;
        this.name = name;
    }
}
