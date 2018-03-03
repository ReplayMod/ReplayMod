package com.replaymod.render.blend;

import com.replaymod.render.blend.data.DMaterial;
import com.replaymod.render.blend.data.DMesh;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

//#if MC>=11200
import net.minecraft.client.renderer.BufferBuilder;
//#else
//$$ import net.minecraft.client.renderer.WorldRenderer;
//#endif

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.replaymod.core.versions.MCVer.*;

public class BlendMeshBuilder
        //#if MC>=11200
        extends BufferBuilder
        //#else
        //$$ extends WorldRenderer
        //#endif
{
    private final DMesh mesh;
    private final List<DMesh.Vertex> vertices = new ArrayList<>();
    private final List<Vector2f> uvs = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private Vector3f offset = new Vector3f(0, 0, 0);
    private boolean isDrawing;
    private boolean wellBehaved;
    private int mode;
    //#if MC>=11200
    private double x, y, z;
    //#endif
    private float u, v;
    private int color = 0xffffffff; // Default not in parent class but probably set implicitly somewhere due to re-use

    public BlendMeshBuilder(DMesh mesh) {
        super(0);
        this.mesh = mesh;
    }

    public void setWellBehaved(boolean wellBehaved) {
        this.wellBehaved = wellBehaved;
    }

    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

    @Override
    //#if MC>=11200
    public void begin(int mode, VertexFormat vertexFormat) {
    //#else
    //$$ public void startDrawing(int mode) {
    //#endif
        if (isDrawing) {
            if (!wellBehaved) {
                // Someone probably finished drawing with the global instance instead of this one,
                // let's just assume that what's happened and finish our last draw by ourselves
                // (might miss correct texture though)
                doFinish();
            } else {
                throw new IllegalStateException("Already drawing!");
            }
        }
        this.isDrawing = true;
        this.mode = mode;

        if (!wellBehaved) {
            // In case the calling code finishes with Tessellator.getInstance().draw()
            BufferBuilder_beginPosTexCol(mode);
        }
    }

    public void maybeFinishDrawing() {
        if (isDrawing) {
            doFinish();
        }
    }

    @Override
    //#if MC>=11200
    public void finishDrawing() {
    //#else
    //$$ public int finishDrawing() {
    //#endif
        if (!isDrawing) {
            throw new IllegalStateException("Not building!");
        } else {
            if (!wellBehaved) {
                getBuffer(Tessellator.getInstance()).finishDrawing();
            }

            doFinish();
            //#if MC<11200
            //$$ return -1;
            //#endif
        }
    }

    private void doFinish() {
        this.isDrawing = false;

        DMaterial activeMaterial = BlendState.getState().getMaterials().getActiveMaterial();
        int materialSlot = mesh.materials.indexOf(activeMaterial);
        if (materialSlot < 0) {
            materialSlot = mesh.materials.size();
            mesh.materials.add(activeMaterial);
        }

        switch (mode) {
            case GL11.GL_TRIANGLES:
                for (int i = 0; i < vertices.size(); i+=3) {
                    mesh.addTriangle(
                            vertices.get(i    ),
                            vertices.get(i + 1),
                            vertices.get(i + 2),
                            uvs.get(i    ),
                            uvs.get(i + 1),
                            uvs.get(i + 2),
                            colors.get(i    ),
                            colors.get(i + 1),
                            colors.get(i + 2),
                            materialSlot
                    );
                }
                break;
            case GL11.GL_QUADS:
                for (int i = 0; i < vertices.size(); i+=4) {
                    mesh.addQuad(
                            vertices.get(i    ),
                            vertices.get(i + 1),
                            vertices.get(i + 2),
                            vertices.get(i + 3),
                            uvs.get(i    ),
                            uvs.get(i + 1),
                            uvs.get(i + 2),
                            uvs.get(i + 3),
                            colors.get(i    ),
                            colors.get(i + 1),
                            colors.get(i + 2),
                            colors.get(i + 3),
                            materialSlot
                    );
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported mode: " + mode);
        }
    }

    //#if MC>=11200
    // TODO these should all behave differently depending on the order in which they're called
    // e.g. tex may be called multiple times, if there are multiple active texture units
    @Override
    public BlendMeshBuilder tex(double u, double v) {
        this.u = (float) u;
        this.v = (float) (1 - v);
        return this;
    }

    @Override
    public BufferBuilder lightmap(int p_187314_1_, int p_187314_2_) {
        // TODO we probably do care about this
        return this;
    }

    @Override
    public BlendMeshBuilder color(int r, int g, int b, int a) {
        this.color = r | (g << 8) | (b << 16) | (a << 24);
        return this;
    }

    @Override
    public BufferBuilder normal(float x, float y, float z) {
        // TODO do we care about normals?
        return this;
    }

    @Override
    public BlendMeshBuilder pos(double x, double y, double z) {
        this.x = x - offset.x;
        this.y = y - offset.y;
        this.z = z - offset.z;
        return this;
    }

    @Override
    public void endVertex() {
        vertices.add(new DMesh.Vertex((float) x, (float) -z, (float) y));
        uvs.add(new Vector2f(u, v));
        colors.add(color);
    }
    //#else
    //$$ @Override
    //$$ public void setTextureUV(double u, double v) {
    //$$     this.u = (float) u;
    //$$     this.v = (float) (1 - v);
    //$$ }
    //$$
    //$$ @Override
    //$$ public void setColorRGBA(int r, int g, int b, int a) {
    //$$     this.color = r | (g << 8) | (b << 16) | (a << 24);
    //$$ }
    //$$
    //$$ @Override
    //$$ public void addVertex(double x, double y, double z) {
    //$$     x -= offset.x;
    //$$     y -= offset.y;
    //$$     z -= offset.z;
    //$$     vertices.add(new DMesh.Vertex((float) x, (float) -z, (float) y));
    //$$     uvs.add(new Vector2f(u, v));
    //$$     colors.add(color);
    //$$ }
    //#endif

    @Override
    @SuppressWarnings("unchecked")
    public void addVertexData(int[] ints) {
        ByteBuffer buffer = ByteBuffer.allocate(ints.length * 4).order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(ints);

        VertexFormat vertexFormat = getVertexFormat();
        int posOffset = -1, colorOffset = -1, uvOffset = -1;
        List<VertexFormatElement> elements = vertexFormat.getElements();
        int index = 0;
        for (VertexFormatElement element : elements) {
            //#if MC>11200
            int offset = vertexFormat.getOffset(index);
            //#else
            //$$ int offset = element.getOffset();
            //#endif
            switch (element.getUsage()) {
                case POSITION: posOffset = offset; break;
                case COLOR: colorOffset = offset; break;
                case UV: if (element.getIndex() == 0) uvOffset = offset; break;
            }
            index++;
        }
        if (posOffset == -1) throw new IllegalStateException("No position element in " + vertexFormat);
        if (uvOffset == -1) throw new IllegalStateException("No uv element in " + vertexFormat);

        for (int offset = 0; offset < buffer.remaining(); offset += vertexFormat.getNextOffset()) {
            // TODO doesn't check buffer element types (works for vanilla because they're always the same)
            if (colorOffset != -1) {
                color = buffer.getInt(offset + colorOffset);
            }
            //#if MC>11200
            tex(
            //#else
            //$$ setTextureUV(
            //#endif
                    buffer.getFloat(offset + uvOffset),
                    buffer.getFloat(offset + uvOffset + 4)
            );
            //#if MC>11200
            pos(
            //#else
            //$$ addVertex(
            //#endif
                    buffer.getFloat(offset + posOffset    ),
                    buffer.getFloat(offset + posOffset + 4),
                    buffer.getFloat(offset + posOffset + 8)
            );

            //#if MC>11200
            endVertex();
            //#endif
        }
    }

    @Override
    public int getVertexCount() {
        return vertices.size();
    }

    public void putColorRGBA(int byteIndex, int r, int g, int b, int a) {
        int color = r | (g << 8) | (b << 16) | (a << 24);
        putColor(color, -1); // FIXME: reverse index calculation
    }

    private void putColor(int color, int reverseIndex) {
        colors.set(colors.size() - reverseIndex, color);
    }

    public void putColor4(int color) {
        for (int i = 0; i < 4; i++) {
            putColor(color, i + 1);
        }
    }

    // FIXME overwrite putX and more
}
