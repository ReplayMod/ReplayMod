package com.replaymod.render.blend.exporters;

import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;

//#if MC>=10904
import net.minecraft.client.renderer.block.model.IBakedModel;
import org.lwjgl.opengl.GL11;
//#else
//$$ import net.minecraft.client.resources.model.IBakedModel;
//#endif

import java.util.List;

public class ItemExporter implements Exporter {
    private final RenderState renderState;

    public ItemExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    public void onRender(RenderItem renderItem, IBakedModel model, ItemStack stack) {
        DObject object = getObjectForItemStack(renderItem, model, stack);

        renderState.pushObject(object);
        renderState.pushModelView();
        renderState.applyLastModelViewTransformToObject();

        object.setVisible(renderState.getFrame());
        object.keyframeLocRotScale(renderState.getFrame());

        renderState.pop();
    }

    private DObject getObjectForItemStack(RenderItem renderItem, IBakedModel model, ItemStack stack) {
        int frame = renderState.getFrame();
        DObject parent = renderState.peekObject();
        DObject object = null;
        for (DObject child : parent.getChildren()) {
            if (child.lastFrame < frame
                    && child instanceof ItemBasedDObject
                    && ((ItemBasedDObject) child).isBasedOn(renderItem, model, stack)) {
                object = child;
                break;
            }
        }
        if (object == null) {
            object = new ItemBasedDObject(renderItem, model, stack);
            object.id.name = stack.getDisplayName();
            object.setParent(parent);
        }
        object.lastFrame = frame;
        return object;
    }

    @SuppressWarnings("unchecked")
    private static DMesh generateMeshForItemStack(RenderItem renderItem, IBakedModel model, ItemStack stack) {
        DMesh mesh = new DMesh();
        BlendMeshBuilder builder = new BlendMeshBuilder(mesh);
        builder.setWellBehaved(true);
        //#if MC>=10809
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        //#else
        //$$ builder.startDrawingQuads();
        //$$ builder.setVertexFormat(DefaultVertexFormats.ITEM);
        //#endif

        //#if MC>=10904
        for (EnumFacing face : EnumFacing.values()) {
            renderQuads(renderItem, builder, model.getQuads(null, face, 0), stack);
        }
        renderQuads(renderItem, builder, model.getQuads(null, null, 0), stack);
        //#else
        //$$ for (EnumFacing face : EnumFacing.values()) {
        //$$     renderQuads(renderItem, builder, model.getFaceQuads(face), stack);
        //$$ }
        //$$ renderQuads(renderItem, builder, model.getGeneralQuads(), stack);
        //#endif

        builder.finishDrawing();
        return mesh;
    }

    private static void renderQuads(RenderItem renderItem, BlendMeshBuilder buffer, List<BakedQuad> quads, ItemStack stack) {
        for (BakedQuad quad : quads) {
            int color = stack != null && quad.hasTintIndex()
                    //#if MC>=11200
                    ? renderItem.itemColors.getColorFromItemstack(stack, quad.getTintIndex()) | 0xff000000
                    //#else
                    //$$ ? stack.getItem().getColorFromItemStack(stack, quad.getTintIndex()) | 0xff000000
                    //#endif
                    : 0xffffffff;
            LightUtil.renderQuadColor(buffer, quad, color);
        }

    }

    private static class ItemBasedDObject extends DObject {
        private final RenderItem renderItem;
        private final IBakedModel model;
        private final ItemStack stack;
        private boolean valid;

        public ItemBasedDObject(RenderItem renderItem, IBakedModel model, ItemStack stack) {
            super(generateMeshForItemStack(renderItem, model, stack));
            this.renderItem = renderItem;
            this.model = model;
            this.stack = stack;
        }

        public boolean isBasedOn(RenderItem renderItem, IBakedModel model, ItemStack stack) {
            return this.renderItem == renderItem && this.model == model && this.stack == stack;
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
