package com.replaymod.render.blend;

import com.replaymod.render.blend.data.DMaterial;
import com.replaymod.render.blend.data.DMesh;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class BlendMeshBuilder extends WorldRenderer {
    private final DMesh mesh;
    private final List<DMesh.Vertex> vertices = new ArrayList<>();
    private final List<Vector2f> uvs = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private Vector3f offset = new Vector3f(0, 0, 0);
    private boolean isDrawing;
    private boolean wellBehaved;
    private int mode;
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
    public void startDrawing(int mode) {
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
            Tessellator.getInstance().getWorldRenderer().startDrawingQuads();
        }
    }

    public void maybeFinishDrawing() {
        if (isDrawing) {
            doFinish();
        }
    }

    @Override
    public int finishDrawing() {
        if (!isDrawing) {
            throw new IllegalStateException("Not building!");
        } else {
            if (!wellBehaved) {
                Tessellator.getInstance().getWorldRenderer().finishDrawing();
            }

            doFinish();
            return -1;
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

    @Override
    public void setTextureUV(double u, double v) {
        this.u = (float) u;
        this.v = (float) (1 - v);
    }

    @Override
    public void setColorRGBA(int r, int g, int b, int a) {
        this.color = r | (g << 8) | (b << 16) | (a << 24);
    }

    @Override
    public void addVertex(double x, double y, double z) {
        x -= offset.x;
        y -= offset.y;
        z -= offset.z;
        vertices.add(new DMesh.Vertex((float) x, (float) -z, (float) y));
        uvs.add(new Vector2f(u, v));
        colors.add(color);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addVertexData(int[] ints) {
        ByteBuffer buffer = ByteBuffer.allocate(ints.length * 4).order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(ints);

        VertexFormat vertexFormat = getVertexFormat();
        int posOffset = -1, colorOffset = -1, uvOffset = -1;
        List<VertexFormatElement> elements = vertexFormat.getElements();
        for (VertexFormatElement element : elements) {
            switch (element.getUsage()) {
                case POSITION: posOffset = element.getOffset(); break;
                case COLOR: colorOffset = element.getOffset(); break;
                case UV: if (element.getIndex() == 0) uvOffset = element.getOffset(); break;
            }
        }
        if (posOffset == -1) throw new IllegalStateException("No position element in " + vertexFormat);
        if (uvOffset == -1) throw new IllegalStateException("No uv element in " + vertexFormat);

        for (int offset = 0; offset < buffer.remaining(); offset += vertexFormat.getNextOffset()) {
            // TODO doesn't check buffer element types (works for vanilla because they're always the same)
            if (colorOffset != -1) {
                color = buffer.getInt(offset + colorOffset);
            }
            setTextureUV(
                    buffer.getFloat(offset + uvOffset),
                    buffer.getFloat(offset + uvOffset + 4)
            );
            addVertex(
                    buffer.getFloat(offset + posOffset    ),
                    buffer.getFloat(offset + posOffset + 4),
                    buffer.getFloat(offset + posOffset + 8)
            );
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
