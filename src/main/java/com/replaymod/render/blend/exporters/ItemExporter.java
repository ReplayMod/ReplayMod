package com.replaymod.render.blend.exporters;

import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;

import java.util.List;

public class ItemExporter implements Exporter {
    private final RenderState renderState;

    public ItemExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    public void onRender(IBakedModel model, ItemStack stack) {
        DObject object = getObjectForItemStack(model, stack);

        renderState.pushObject(object);
        renderState.pushModelView();
        renderState.applyLastModelViewTransformToObject();

        object.setVisible(renderState.getFrame());
        object.keyframeLocRotScale(renderState.getFrame());

        renderState.pop();
    }

    private DObject getObjectForItemStack(IBakedModel model, ItemStack stack) {
        int frame = renderState.getFrame();
        DObject parent = renderState.peekObject();
        DObject object = null;
        for (DObject child : parent.getChildren()) {
            if (child.lastFrame < frame
                    && child instanceof ItemBasedDObject
                    && ((ItemBasedDObject) child).isBasedOn(model, stack)) {
                object = child;
                break;
            }
        }
        if (object == null) {
            object = new ItemBasedDObject(model, stack);
            object.id.name = stack.getDisplayName();
            object.setParent(parent);
        }
        object.lastFrame = frame;
        return object;
    }

    @SuppressWarnings("unchecked")
    private static DMesh generateMeshForItemStack(IBakedModel model, ItemStack stack) {
        DMesh mesh = new DMesh();
        BlendMeshBuilder builder = new BlendMeshBuilder(mesh);
        builder.setWellBehaved(true);
        builder.startDrawingQuads();
        builder.setVertexFormat(DefaultVertexFormats.ITEM);

        for (EnumFacing face : EnumFacing.values()) {
            renderQuads(builder, model.getFaceQuads(face), stack);
        }
        renderQuads(builder, model.getGeneralQuads(), stack);

        builder.finishDrawing();
        return mesh;
    }

    private static void renderQuads(WorldRenderer buffer, List<BakedQuad> quads, ItemStack stack) {
        for (BakedQuad quad : quads) {
            int color = stack != null && quad.hasTintIndex()
                    ? stack.getItem().getColorFromItemStack(stack, quad.getTintIndex()) | 0xff000000
                    : 0xffffffff;
            LightUtil.renderQuadColor(buffer, quad, color);
        }

    }

    private static class ItemBasedDObject extends DObject {
        private final IBakedModel model;
        private final ItemStack stack;
        private boolean valid;

        public ItemBasedDObject(IBakedModel model, ItemStack stack) {
            super(generateMeshForItemStack(model, stack));
            this.model = model;
            this.stack = stack;
        }

        public boolean isBasedOn(IBakedModel model, ItemStack scale) {
            return this.model == model; // FIXME ignores color of stack
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
