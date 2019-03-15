package com.replaymod.render.blend.data;

import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector2f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import org.blender.dna.CustomData;
import org.blender.dna.CustomDataLayer;
import org.blender.dna.Image;
import org.blender.dna.MEdge;
import org.blender.dna.MFace;
import org.blender.dna.MLoop;
import org.blender.dna.MLoopCol;
import org.blender.dna.MLoopUV;
import org.blender.dna.MPoly;
import org.blender.dna.MTexPoly;
import org.blender.dna.MVert;
import org.blender.dna.Material;
import org.blender.dna.Mesh;
import org.cakelab.blender.io.block.BlockCodes;
import org.cakelab.blender.nio.CArrayFacade;
import org.cakelab.blender.nio.CPointer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DMesh {
    public final DId id = new DId(BlockCodes.ID_ME);
    public final List<Vertex> vertices = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();
    public final List<Face> faces = new ArrayList<>();
    public final List<Loop> loops = new ArrayList<>();
    public final List<Poly> polys = new ArrayList<>();
    public final List<DMaterial> materials = new ArrayList<>();

    public CPointer<Mesh> serialize(Serializer serializer) throws IOException {
        return serializer.maybeMajor(this, id, Mesh.class, () -> {
            List<CPointer<Material>> materials = new ArrayList<>();
            List<CPointer<Image>> images = new ArrayList<>();
            for (DMaterial material : this.materials) {
                materials.add(material.serialize(serializer));
                images.add(material.textures.isEmpty() ? null : material.textures.get(0).texture.image.serialize(serializer));
            }
            return mesh -> {
                mesh.setDrawflag(3 /* Edges & Faces */);
                CArrayFacade<CPointer<Material>> mat = serializer.writeDataPArray(Material.class, materials.size(), materials::get);
                mesh.setMat(mat);
                mesh.setTotcol((short) materials.size());

                if (!vertices.isEmpty()) {
                    CArrayFacade<MVert> mVerts = serializer.writeData(MVert.class, vertices.size(),
                            (i, mVert) -> vertices.get(i).serialize(mVert));

                    CustomDataLayer vDataLayer = serializer.writeData(CustomDataLayer.class);
                    vDataLayer.setType(0 /* CD_MVERT */);
                    vDataLayer.setData(mVerts.cast(Object.class));

                    CustomData vData = mesh.getVdata();
                    vData.setMaxlayer(1);
                    vData.setTotlayer(1);
                    vData.setLayers(vDataLayer.__io__addressof());

                    mesh.setMvert(mVerts);
                    mesh.setTotvert(vertices.size());
                }
                if (!edges.isEmpty()) {
                    CArrayFacade<MEdge> mEdges = serializer.writeData(MEdge.class, edges.size(),
                            (i, mEdge) -> edges.get(i).serialize(mEdge));

                    CustomDataLayer eDataLayer = serializer.writeData(CustomDataLayer.class);
                    eDataLayer.setType(3 /* CD_MEDGE */);
                    eDataLayer.setData(mEdges.cast(Object.class));

                    CustomData eData = mesh.getEdata();
                    eData.setMaxlayer(1);
                    eData.setTotlayer(1);
                    eData.setLayers(eDataLayer.__io__addressof());

                    mesh.setMedge(mEdges);
                    mesh.setTotedge(edges.size());


                    mesh.setMface(serializer.writeData(MFace.class, faces.size(),
                            (i, mFace) -> faces.get(i).serialize(mFace)));
                    mesh.setTotface(faces.size());
                }
                if (!loops.isEmpty()) {
                    CArrayFacade<MLoop> mLoops = serializer.writeData(MLoop.class, loops.size(),
                            (i, mLoop) -> loops.get(i).serialize(mLoop));
                    CArrayFacade<MLoopUV> mLoopUVs = serializer.writeData(MLoopUV.class, loops.size(),
                            (i, mLoopUV) -> loops.get(i).serialize(mLoopUV));
                    CArrayFacade<MLoopCol> mLoopCols = serializer.writeData(MLoopCol.class, loops.size(),
                            (i, mLoopCol) -> loops.get(i).serialize(mLoopCol));

                    CArrayFacade<CustomDataLayer> dataLayers = serializer.writeData(CustomDataLayer.class, 3);
                    int i = 0;

                    CustomDataLayer luvDataLayer = dataLayers.get(i++);
                    luvDataLayer.setType(16 /* CD_MLOOPUV */);
                    luvDataLayer.setData(mLoopUVs.cast(Object.class));

                    CustomDataLayer lcolDataLayer = dataLayers.get(i++);
                    lcolDataLayer.setType(17 /* CD_MLOOPCOL */);
                    lcolDataLayer.setData(mLoopCols.cast(Object.class));

                    CustomDataLayer lDataLayer = dataLayers.get(i++);
                    lDataLayer.setType(26 /* CD_MLOOP */);
                    lDataLayer.setData(mLoops.cast(Object.class));

                    CustomData lData = mesh.getLdata();
                    lData.setMaxlayer(i);
                    lData.setTotlayer(i);
                    lData.setLayers(dataLayers);

                    mesh.setMloop(mLoops);
                    mesh.setMloopuv(mLoopUVs);
                    mesh.setTotloop(loops.size());
                }
                if (!polys.isEmpty()) {
                    CArrayFacade<MPoly> mPolys = serializer.writeData(MPoly.class, polys.size(),
                            (i, mPoly) -> polys.get(i).serialize(mPoly));
                    CArrayFacade<MTexPoly> mTexPolys = serializer.writeData(MTexPoly.class, polys.size(),
                            (i, mTexPoly) -> mTexPoly.setTpage(images.get(polys.get(i).materialSlot)));

                    CArrayFacade<CustomDataLayer> dataLayers = serializer.writeData(CustomDataLayer.class, mTexPolys != null ? 2 : 1);
                    int i = 0;

                    if (mTexPolys != null) {
                        CustomDataLayer pTexDataLayer = dataLayers.get(i++);
                        pTexDataLayer.getName().fromString("UVMap");
                        pTexDataLayer.setType(15 /* CD_MTEXPOLY */);
                        pTexDataLayer.setData(mTexPolys.cast(Object.class));
                    }

                    CustomDataLayer pDataLayer = dataLayers.get(i++);
                    pDataLayer.setType(25 /* CD_MPOLY */);
                    pDataLayer.setData(mPolys.cast(Object.class));

                    CustomData pData = mesh.getPdata();
                    pData.setMaxlayer(i);
                    pData.setTotlayer(i);
                    pData.setLayers(dataLayers);

                    mesh.setMpoly(mPolys);
                    mesh.setMtpoly(mTexPolys);
                    mesh.setTotpoly(polys.size());
                }
            };
        });
    }

    public void addTriangle(DMesh.Vertex v1, DMesh.Vertex v2, DMesh.Vertex v3,
                            Vector2f uv1, Vector2f uv2, Vector2f uv3,
                            int c1, int c2, int c3,
                            int materialSlot) {
        int vOffset = vertices.size();
        int eOffset = edges.size();
        int lOffset = loops.size();
        vertices.add(v1);
        vertices.add(v2);
        vertices.add(v3);
        edges.add(new Edge(vOffset    , vOffset + 1));
        edges.add(new Edge(vOffset + 1, vOffset + 2));
        edges.add(new Edge(vOffset + 2, vOffset    ));
        loops.add(new Loop(vOffset    , eOffset    , uv1.x, uv1.y, c1));
        loops.add(new Loop(vOffset + 1, eOffset + 1, uv2.x, uv2.y, c2));
        loops.add(new Loop(vOffset + 2, eOffset + 2, uv3.x, uv3.y, c3));
        polys.add(new Poly(lOffset, 3, materialSlot));
    }

    public void addQuad(DMesh.Vertex v1, DMesh.Vertex v2, DMesh.Vertex v3, DMesh.Vertex v4,
                        Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4,
                        int c1, int c2, int c3, int c4,
                        int materialSlot) {
        int vOffset = vertices.size();
        int eOffset = edges.size();
        int lOffset = loops.size();
        vertices.add(v1);
        vertices.add(v2);
        vertices.add(v3);
        vertices.add(v4);
        edges.add(new Edge(vOffset    , vOffset + 1));
        edges.add(new Edge(vOffset + 1, vOffset + 2));
        edges.add(new Edge(vOffset + 2, vOffset + 3));
        edges.add(new Edge(vOffset + 3, vOffset    ));
        loops.add(new Loop(vOffset    , eOffset    , uv1.x, uv1.y, c1));
        loops.add(new Loop(vOffset + 1, eOffset + 1, uv2.x, uv2.y, c2));
        loops.add(new Loop(vOffset + 2, eOffset + 2, uv3.x, uv3.y, c3));
        loops.add(new Loop(vOffset + 3, eOffset + 3, uv4.x, uv4.y, c4));
        polys.add(new Poly(lOffset, 4, materialSlot));
    }

    public float getSizeX() {
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        for (Vertex vertex : vertices) {
            if (vertex.pos.x < minX) {
                minX = vertex.pos.x;
            }
            if (vertex.pos.x > maxX) {
                maxX = vertex.pos.x;
            }
        }
        return maxX - minX;
    }

    public boolean hasZeroLengthEdge(float delta) {
        float deltaSquared = delta * delta;
        Vector3f dst = new Vector3f();
        for (Edge edge : edges) {
            Vertex v1 = vertices.get(edge.v1);
            Vertex v2 = vertices.get(edge.v2);
            if (Vector3f.sub(v1.pos, v2.pos, dst).lengthSquared() < deltaSquared) {
                return true;
            }
        }
        return false;
    }

    public static class Vertex {
        public Vector3f pos;
        public short normX, normY, normZ;

        public Vertex(float x, float y, float z) {
            this.pos = new Vector3f(x, y, z);
        }

        public void serialize(MVert mVert) throws IOException {
            CArrayFacade<Float> pos = mVert.getCo();
            pos.set(0, this.pos.getX());
            pos.set(1, this.pos.getY());
            pos.set(2, this.pos.getZ());
            CArrayFacade<Short> norm = mVert.getNo();
            norm.set(0, this.normX);
            norm.set(1, this.normY);
            norm.set(2, this.normZ);
        }
    }

    public static class Edge {
        public int v1, v2;

        public Edge(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        public void serialize(MEdge mEdge) throws IOException {
            mEdge.setV1(v1);
            mEdge.setV2(v2);
        }
    }

    public static class Loop {
        public int vertex, edge;
        public float u, v;
        public int col;

        public Loop(int vertex, int edge,
                    float u, float v,
                    int col) {
            this.vertex = vertex;
            this.edge = edge;
            this.u = u;
            this.v = v;
            this.col = col;
        }

        public void serialize(MLoop mLoop) throws IOException {
            mLoop.setV(vertex);
            mLoop.setE(edge);
        }

        public void serialize(MLoopUV mLoop) throws IOException {
            CArrayFacade<Float> uv = mLoop.getUv();
            uv.set(0, u);
            uv.set(1, v);
        }

        public void serialize(MLoopCol mLoop) throws IOException {
            mLoop.setR((byte) ((col      ) & 0xff));
            mLoop.setG((byte) ((col >>  8) & 0xff));
            mLoop.setB((byte) ((col >> 16) & 0xff));
            mLoop.setA((byte) ((col >> 24) & 0xff));
        }
    }

    public static class Poly {
        public int loopStart, size;
        public short materialSlot;

        public Poly(int loopStart, int size, int materialSlot) {
            this.loopStart = loopStart;
            this.size = size;
            this.materialSlot = (short) materialSlot;
        }

        public void serialize(MPoly mPoly) throws IOException {
            mPoly.setLoopstart(loopStart);
            mPoly.setTotloop(size);
            mPoly.setMat_nr(materialSlot);
        }
    }

    public static class Face {
        public int v1, v2, v3, v4;

        public Face(int v1, int v2, int v3, int v4) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
        }

        public void serialize(MFace mFace) throws IOException {
            mFace.setV1(v1);
            mFace.setV2(v2);
            mFace.setV3(v3);
            mFace.setV4(v4);
        }
    }
}
