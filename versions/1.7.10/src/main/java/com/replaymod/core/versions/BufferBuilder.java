package com.replaymod.core.versions;

import net.minecraft.client.renderer.Tessellator;

public class BufferBuilder {

    private final Tessellator inner;

    public BufferBuilder(Tessellator inner) {
        this.inner = inner;
    }

    public void startDrawing(int glQuads) {
        inner.startDrawing(glQuads);
    }

    public void addVertexWithUV(double x, double y, double z, float u, float v) {
        inner.addVertexWithUV(x, y, z, u, v);
    }

    public void setColorRGBA(int r, int g, int b, int a) {
        inner.setColorRGBA(r, g, b, a);
    }

    public void addVertex(double x, double y, double z) {
        inner.addVertex(x, y, z);
    }

    public static class VertexFormats {}
}
