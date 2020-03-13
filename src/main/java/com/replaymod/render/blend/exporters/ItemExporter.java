//#if MC>=10800
package com.replaymod.render.blend.exporters;

import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.lwjgl.opengl.GL11;

//#if MC>=11400
import net.minecraft.util.math.Vec3i;
//#else
//$$ import net.minecraftforge.client.model.pipeline.LightUtil;
//#endif

//#if MC>=11300
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BakedModel;
//#else
//$$ import net.minecraft.client.renderer.block.model.BakedQuad;
//#if MC>=10904
//$$ import net.minecraft.client.renderer.block.model.IBakedModel;
//#else
//$$ import net.minecraft.client.resources.model.IBakedModel;
//#endif
//#endif

//#if MC>=10904
import com.replaymod.render.blend.mixin.ItemRendererAccessor;
//#endif

import java.util.List;
import java.util.Random;

public class ItemExporter implements Exporter {
    private final RenderState renderState;

    public ItemExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    public void onRender(Object renderItem, BakedModel model, ItemStack stack) {
        DObject object = getObjectForItemStack(renderItem, model, stack);

        renderState.pushObject(object);
        renderState.pushModelView();
        renderState.applyLastModelViewTransformToObject();

        object.setVisible(renderState.getFrame());
        object.keyframeLocRotScale(renderState.getFrame());

        renderState.pop();
    }

    private DObject getObjectForItemStack(Object renderItem, BakedModel model, ItemStack stack) {
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
            //#if MC>=11300
            object.id.name = stack.getName().getString();
            //#else
            //$$ object.id.name = stack.getDisplayName();
            //#endif
            object.setParent(parent);
        }
        object.lastFrame = frame;
        return object;
    }

    @SuppressWarnings("unchecked")
    private static DMesh generateMeshForItemStack(Object renderItem, BakedModel model, ItemStack stack) {
        DMesh mesh = new DMesh();
        BlendMeshBuilder builder = new BlendMeshBuilder(mesh);
        builder.setWellBehaved(true);
        //#if MC>=10809
        builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_UV_NORMAL);
        //#else
        //$$ builder.startDrawingQuads();
        //$$ builder.setVertexFormat(DefaultVertexFormats.ITEM);
        //#endif

        //#if MC>=11300
        for (Direction face : Direction.values()) {
            renderQuads(renderItem, builder, model.getQuads(null, face, new Random()), stack);
        }
        renderQuads(renderItem, builder, model.getQuads(null, null, new Random()), stack);
        //#else
        //#if MC>=10904
        //$$ for (EnumFacing face : EnumFacing.values()) {
        //$$     renderQuads(renderItem, builder, model.getQuads(null, face, 0), stack);
        //$$ }
        //$$ renderQuads(renderItem, builder, model.getQuads(null, null, 0), stack);
        //#else
        //$$ for (EnumFacing face : EnumFacing.values()) {
        //$$     renderQuads(renderItem, builder, model.getFaceQuads(face), stack);
        //$$ }
        //$$ renderQuads(renderItem, builder, model.getGeneralQuads(), stack);
        //#endif
        //#endif

        builder.end();
        return mesh;
    }

    private static void renderQuads(Object renderItem, BlendMeshBuilder buffer, List<BakedQuad> quads, ItemStack stack) {
        for (BakedQuad quad : quads) {
            int color = stack != null && quad.hasColor()
                    //#if MC>=10904
                    ? ((ItemRendererAccessor) renderItem).getItemColors().getColorMultiplier(stack, quad.getColorIndex()) | 0xff000000
                    //#else
                    //$$ ? stack.getItem().getColorFromItemStack(stack, quad.getTintIndex()) | 0xff000000
                    //#endif
                    : 0xffffffff;
            //#if MC>=11500
            //$$ // FIXME 1.15
            //#else
            //#if MC>=11400
            buffer.putVertexData(quad.getVertexData());
            buffer.setQuadColor(color);
            Vec3i vec3i_1 = quad.getFace().getVector();
            buffer.postNormal(vec3i_1.getX(), vec3i_1.getY(), vec3i_1.getZ());
            //#else
            //$$ LightUtil.renderQuadColor(buffer, quad, color);
            //#endif
            //#endif
        }

    }

    private static class ItemBasedDObject extends DObject {
        private final Object renderItem;
        private final BakedModel model;
        private final ItemStack stack;
        private boolean valid;

        public ItemBasedDObject(Object renderItem, BakedModel model, ItemStack stack) {
            super(generateMeshForItemStack(renderItem, model, stack));
            this.renderItem = renderItem;
            this.model = model;
            this.stack = stack;
        }

        public boolean isBasedOn(Object renderItem, BakedModel model, ItemStack stack) {
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
//#endif
