//#if MC>=10800
package com.replaymod.render.blend.exporters;

import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import org.lwjgl.opengl.GL11;

//#if MC>=11300
import net.minecraft.client.model.Box;
import net.minecraft.client.model.Cuboid;
//#else
//$$ import net.minecraft.client.model.ModelBox;
//$$ import net.minecraft.client.model.ModelRenderer;
//#endif

import java.io.IOException;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.render.blend.Util.isGlTextureMatrixIdentity;

public class ModelRendererExporter implements Exporter {
    private final RenderState renderState;

    public ModelRendererExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    @Override
    public void setup() throws IOException {
    }

    public void preRenderModel(Cuboid model, float scale) {
        DObject object = getObjectForModel(model, scale);
        renderState.pushObject(object);
        renderState.pushModelView();
    }

    public void onRenderModel() {
        if (!GL11.glIsEnabled(GL11.GL_TEXTURE_2D) || !isGlTextureMatrixIdentity()) {
            return;
        }
        renderState.popModelView();
        renderState.pushModelView();
        renderState.applyLastModelViewTransformToObject();
        DObject object = renderState.peekObject();
        object.setVisible(renderState.getFrame());
        object.keyframeLocRotScale(renderState.getFrame());
    }

    public void postRenderModel() {
        renderState.pop();
    }

    private DObject getObjectForModel(Cuboid model, float scale) {
        int frame = renderState.getFrame();
        DObject parent = renderState.peekObject();
        DObject object = null;
        for (DObject child : parent.getChildren()) {
            if (child.lastFrame < frame
                    && child instanceof ModelBasedDObject
                    && ((ModelBasedDObject) child).isBasedOn(model, scale)) {
                object = child;
                break;
            }
        }
        if (object == null) {
            object = new ModelBasedDObject(model, scale);
            object.id.name = model.name;
            object.setParent(parent);
        }
        object.lastFrame = frame;
        return object;
    }

    private static DMesh generateMesh(Cuboid model, float scale) {
        DMesh mesh = new DMesh();
        BlendMeshBuilder builder = new BlendMeshBuilder(mesh);
        for (Box box : cubeList(model)) {
            box.render(builder, scale);
        }
        builder.maybeFinishDrawing();
        return mesh;
    }

    private static class ModelBasedDObject extends DObject {
        private final Cuboid model;
        private final float scale;
        private boolean valid;

        public ModelBasedDObject(Cuboid model, float scale) {
            super(generateMesh(model, scale));
            this.model = model;
            this.scale = scale;
        }

        public boolean isBasedOn(Cuboid model, float scale) {
            return this.model == model && Math.abs(this.scale - scale) < 1e-4;
        }

        @Override
        public void setVisible(int frame) {
            valid = true;
            super.setVisible(frame);
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }
}
//#endif
