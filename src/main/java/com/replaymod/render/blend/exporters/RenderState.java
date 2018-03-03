package com.replaymod.render.blend.exporters;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMaterial;
import com.replaymod.render.blend.data.DObject;
import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import static com.replaymod.render.ReplayModRender.LOGGER;
import static com.replaymod.render.blend.Util.getGlModelViewMatrix;
import static com.replaymod.render.blend.Util.glScaleToBlend;
import static com.replaymod.render.blend.Util.glToBlend;
import static com.replaymod.render.blend.Util.posFromMat;
import static com.replaymod.render.blend.Util.rotFromMat;
import static com.replaymod.render.blend.Util.scaleFromMat;
import static com.replaymod.render.blend.Util.scaleMat3;

public class RenderState implements Exporter {

    private final BlendState blendState;
    private final ModelRendererExporter modelExporter = new ModelRendererExporter(this);
    private final Deque<DObject> objectStack = new LinkedList<>();
    private final Deque<Matrix4f> modelViewStack = new LinkedList<>();

    private int frame;

    public RenderState(BlendState blendState) {
        this.blendState = blendState;

        blendState.register(modelExporter);
    }

    public int getFrame() {
        return frame;
    }

    public DMaterial getCurrentMaterial() {
        return blendState.getMaterials().getActiveMaterial();
    }

    public ModelRendererExporter getModelExporter() {
        return modelExporter;
    }

    public Deque<DObject> getObjectStack() {
        return objectStack;
    }

    public void push(DObject object, Matrix4f modelView) {
        pushObject(object);
        pushModelView(modelView);
    }

    public void pushObject(DObject object) {
        objectStack.push(object);
    }

    public void pushModelView() {
        pushModelView(getGlModelViewMatrix());
    }

    public void pushModelView(Matrix4f modelView) {
        modelViewStack.push(modelView);
    }

    public void pop() {
        popObject();
        popModelView();
    }

    public DObject popObject() {
        return objectStack.pop();
    }

    public Matrix4f popModelView() {
        return modelViewStack.pop();
    }

    public DObject peekObject() {
        return objectStack.peek();
    }

    public Matrix4f peekModelView() {
        return modelViewStack.peek();
    }

    public void applyLastModelViewTransformToObject() {
        applyLastModelViewTransformToObject(peekObject());
    }

    public void applyLastModelViewTransformToObject(DObject object) {
        Matrix4f mat = calcLastModelViewTransform();

        posFromMat(mat, object.loc);
        scaleFromMat(mat, object.scale);
        scaleMat3(mat, object.scale);
        rotFromMat(mat, object.rot);

        glToBlend(object.loc);
        glScaleToBlend(object.scale);
        glToBlend(object.rot);
    }

    public Matrix4f calcLastModelViewTransform() {
        Iterator<Matrix4f> iter = modelViewStack.iterator();
        Matrix4f newModelView = iter.next();
        Matrix4f prevModelView = iter.next();
        // We need to apply the same transformation T to the object that has transformed the old into the new matrix:
        // O * T = N   <=>   T = O^{-1} * N
        return Matrix4f.mul(Matrix4f.invert(prevModelView, null), newModelView, null);
    }

    @Override
    public void setup() throws IOException {
    }

    @Override
    public void preFrame(int frame) throws IOException {
        this.frame = frame;
    }

    @Override
    public void postFrame(int frame) throws IOException {
        if (!objectStack.isEmpty()) {
            LOGGER.warn("Post frame with non-empty object stack! ({} remaining)", objectStack.size());
            objectStack.clear();
        }
        if (!modelViewStack.isEmpty()) {
            LOGGER.warn("Post frame with non-empty model-view stack! ({} remaining)", modelViewStack.size());
            modelViewStack.clear();
        }
    }

    @Override
    public void tearDown() throws IOException {

    }
}
