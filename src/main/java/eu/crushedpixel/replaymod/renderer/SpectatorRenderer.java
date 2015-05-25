package eu.crushedpixel.replaymod.renderer;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.SkinProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

public class SpectatorRenderer {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void renderSpectatorHand(RenderHandEvent event) {
        if(!ReplayHandler.isInReplay() || ReplayHandler.isCamera()) {
            return;
        }

        Entity current = ReplayHandler.getCurrentEntity();
        if(!(current instanceof EntityPlayer)) return;
        EntityPlayer entityPlayer = (EntityPlayer)current;

        event.setCanceled(true);

        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        float f1 = 0.07F;

        if (this.mc.gameSettings.anaglyph) {
            GlStateManager.translate((float)(-(event.renderPass * 2 - 1)) * f1, 0.0F, 0.0F);
        }

        Project.gluPerspective(mc.entityRenderer.getFOVModifier(event.partialTicks, false),
                (float)this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, mc.entityRenderer.farPlaneDistance * 2.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();

        if (this.mc.gameSettings.anaglyph)
        {
            GlStateManager.translate((float)(event.renderPass * 2 - 1) * 0.1F, 0.0F, 0.0F);
        }

        GlStateManager.pushMatrix();
        mc.entityRenderer.hurtCameraEffect(event.partialTicks);

        if (this.mc.gameSettings.viewBobbing)
        {
            mc.entityRenderer.setupViewBobbing(event.partialTicks);
        }

        boolean flag = this.mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();

        if (this.mc.gameSettings.thirdPersonView == 0 && !flag && !this.mc.gameSettings.hideGUI)
        {
            mc.entityRenderer.enableLightmap();
            renderItemInFirstPerson(event.partialTicks);
            mc.entityRenderer.disableLightmap();
        }

        GlStateManager.popMatrix();

        if (this.mc.gameSettings.thirdPersonView == 0 && !flag)
        {
            renderOverlays(event.partialTicks, entityPlayer);
            mc.entityRenderer.hurtCameraEffect(event.partialTicks);
        }

        if (this.mc.gameSettings.viewBobbing)
        {
            mc.entityRenderer.setupViewBobbing(event.partialTicks);
        }

    }

    public void renderOverlays(float p_78447_1_, EntityPlayer player) {
        GlStateManager.disableAlpha();

        if(player.isEntityInsideOpaqueBlock()) {
            IBlockState iblockstate = this.mc.theWorld.getBlockState(new BlockPos(player));

            for (int i = 0; i < 8; ++i)
            {
                double d0 = player.posX + (double)(((float)((i >> 0) % 2) - 0.5F) * player.width * 0.8F);
                double d1 = player.posY + (double)(((float)((i >> 1) % 2) - 0.5F) * 0.1F);
                double d2 = player.posZ + (double)(((float)((i >> 2) % 2) - 0.5F) * player.width * 0.8F);
                BlockPos blockpos = new BlockPos(d0, d1 + (double)player.getEyeHeight(), d2);
                IBlockState iblockstate1 = this.mc.theWorld.getBlockState(blockpos);

                if (iblockstate1.getBlock().isVisuallyOpaque())
                {
                    iblockstate = iblockstate1;
                }
            }

            if (iblockstate.getBlock().getRenderType() != -1)
            {
                mc.entityRenderer.itemRenderer.func_178108_a(p_78447_1_, this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(iblockstate));
            }
        }

        if(!player.isSpectator()) {
            if (player.isInsideOfMaterial(Material.water)) {
                renderWaterOverlayTexture(p_78447_1_, player);
            }

            if (this.mc.thePlayer.isBurning()) {
                mc.entityRenderer.itemRenderer.renderFireInFirstPerson(p_78447_1_);
            }
        }

        GlStateManager.enableAlpha();
    }

    public void renderWaterOverlayTexture(float p_78448_1_, EntityPlayer player)
    {
        this.mc.getTextureManager().bindTexture(mc.entityRenderer.itemRenderer.RES_UNDERWATER_OVERLAY);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        float f1 = player.getBrightness(p_78448_1_);
        GlStateManager.color(f1, f1, f1, 0.5F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        float f2 = 4.0F;
        float f3 = -1.0F;
        float f4 = 1.0F;
        float f5 = -1.0F;
        float f6 = 1.0F;
        float f7 = -0.5F;
        float f8 = -player.rotationYaw / 64.0F;
        float f9 = player.rotationPitch / 64.0F;
        worldrenderer.startDrawingQuads();
        worldrenderer.addVertexWithUV((double)f3, (double)f5, (double)f7, (double)(f2 + f8), (double)(f2 + f9));
        worldrenderer.addVertexWithUV((double)f4, (double)f5, (double)f7, (double)(0.0F + f8), (double)(f2 + f9));
        worldrenderer.addVertexWithUV((double)f4, (double)f6, (double)f7, (double)(0.0F + f8), (double)(0.0F + f9));
        worldrenderer.addVertexWithUV((double)f3, (double)f6, (double)f7, (double)(f2 + f8), (double)(0.0F + f9));
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    @SubscribeEvent
    public void tickEvent(TickEvent event) {
        if(event.type != TickEvent.Type.CLIENT) return;
        if(event.phase != TickEvent.Phase.START) return;
        Entity current = ReplayHandler.getCurrentEntity();
        if(!(current instanceof EntityPlayer)) return;
        EntityPlayer entityPlayer = (EntityPlayer)current;
        updateEquippedItem(entityPlayer);
        updateArmYawAndPitch(entityPlayer);
    }

    public void renderItemInFirstPerson(float p_78440_1_) {
        //System.out.println("HERE");
        float f1 = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * p_78440_1_);
        Entity current = ReplayHandler.getCurrentEntity();
        if(!(current instanceof EntityPlayer)) return;
        EntityPlayer entityPlayer = (EntityPlayer)current;
        float f2 = entityPlayer.getSwingProgress(p_78440_1_);
        float f3 = entityPlayer.prevRotationPitch + (entityPlayer.rotationPitch - entityPlayer.prevRotationPitch) * p_78440_1_;
        float f4 = entityPlayer.prevRotationYaw + (entityPlayer.rotationYaw - entityPlayer.prevRotationYaw) * p_78440_1_;
        mc.entityRenderer.itemRenderer.func_178101_a(f3, f4); //does not rely on a player
        setLightmapTextureCoords(entityPlayer);
        rotateHand(entityPlayer, p_78440_1_);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        if (itemToRender != null)
        {
            if (itemToRender.getItem() == Items.filled_map) {
                renderMapInHand(entityPlayer, f3, f1, f2);
            }
            else if (entityPlayer.getItemInUseCount() > 0)
            {
                EnumAction enumaction = itemToRender.getItemUseAction();

                switch (ItemRenderer.SwitchEnumAction.field_178094_a[enumaction.ordinal()])
                {
                    case 1:
                        mc.entityRenderer.itemRenderer.func_178096_b(f1, 0.0F);
                        break;
                    case 2:
                    case 3:
                        renderItem1(entityPlayer, p_78440_1_);
                        mc.entityRenderer.itemRenderer.func_178096_b(f1, 0.0F);
                        break;
                    case 4:
                        mc.entityRenderer.itemRenderer.func_178096_b(f1, 0.0F);
                        mc.entityRenderer.itemRenderer.func_178103_d();
                        break;
                    case 5:
                        mc.entityRenderer.itemRenderer.func_178096_b(f1, 0.0F);
                        renderItem2(p_78440_1_, entityPlayer);
                }
            }
            else {
                //those calls are fine
                mc.entityRenderer.itemRenderer.func_178105_d(f2);
                mc.entityRenderer.itemRenderer.func_178096_b(f1, f2);
            }

            mc.entityRenderer.itemRenderer.renderItem(entityPlayer, itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
        }
        else if (!entityPlayer.isInvisible()) {
            func_178095_a(entityPlayer, f1, f2);
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    public void setLightmapTextureCoords(EntityPlayer p_178109_1_) {
        int i = this.mc.theWorld.getCombinedLight(new BlockPos(p_178109_1_.posX, p_178109_1_.posY + (double)p_178109_1_.getEyeHeight(), p_178109_1_.posZ), 0);
        float f = (float)(i & 65535);
        float f1 = (float)(i >> 16);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, f1);
    }

    private float prevRenderArmPitch, renderArmPitch;
    private float prevRenderArmYaw, renderArmYaw;

    public void updateArmYawAndPitch(EntityPlayer player) {
        this.prevRenderArmYaw = this.renderArmYaw;
        this.prevRenderArmPitch = this.renderArmPitch;
        this.renderArmPitch = (float)((double)this.renderArmPitch + (double)(player.rotationPitch - this.renderArmPitch) * 0.5D);
        this.renderArmYaw = (float)((double)this.renderArmYaw + (double)(player.rotationYaw - this.renderArmYaw) * 0.5D);

    }

    public void rotateHand(EntityPlayer p_178110_1_, float p_178110_2_) {
        float f1 = prevRenderArmPitch + (renderArmPitch - prevRenderArmPitch) * p_178110_2_;
        float f2 = prevRenderArmYaw + (renderArmYaw - prevRenderArmYaw) * p_178110_2_;
        GlStateManager.rotate((p_178110_1_.rotationPitch - f1) * 0.1F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((p_178110_1_.rotationYaw - f2) * 0.1F, 0.0F, 1.0F, 0.0F);
    }

    public void renderMapInHand(EntityPlayer p_178097_1_, float p_178097_2_, float p_178097_3_, float p_178097_4_)
    {
        float f3 = -0.4F * MathHelper.sin(MathHelper.sqrt_float(p_178097_4_) * (float) Math.PI);
        float f4 = 0.2F * MathHelper.sin(MathHelper.sqrt_float(p_178097_4_) * (float)Math.PI * 2.0F);
        float f5 = -0.2F * MathHelper.sin(p_178097_4_ * (float)Math.PI);
        GlStateManager.translate(f3, f4, f5);
        float f6 = mc.entityRenderer.itemRenderer.func_178100_c(p_178097_2_); //player independent
        GlStateManager.translate(0.0F, 0.04F, -0.72F);
        GlStateManager.translate(0.0F, p_178097_3_ * -1.2F, 0.0F);
        GlStateManager.translate(0.0F, f6 * -0.5F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f6 * -85.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);
        renderArmSkin(p_178097_1_);
        float f7 = MathHelper.sin(p_178097_4_ * p_178097_4_ * (float)Math.PI);
        float f8 = MathHelper.sin(MathHelper.sqrt_float(p_178097_4_) * (float)Math.PI);
        GlStateManager.rotate(f7 * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f8 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f8 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.38F, 0.38F, 0.38F);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-1.0F, -1.0F, 0.0F);
        GlStateManager.scale(0.015625F, 0.015625F, 0.015625F);
        this.mc.getTextureManager().bindTexture(mc.entityRenderer.itemRenderer.RES_MAP_BACKGROUND);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GL11.glNormal3f(0.0F, 0.0F, -1.0F);
        worldrenderer.startDrawingQuads();
        worldrenderer.addVertexWithUV(-7.0D, 135.0D, 0.0D, 0.0D, 1.0D);
        worldrenderer.addVertexWithUV(135.0D, 135.0D, 0.0D, 1.0D, 1.0D);
        worldrenderer.addVertexWithUV(135.0D, -7.0D, 0.0D, 1.0D, 0.0D);
        worldrenderer.addVertexWithUV(-7.0D, -7.0D, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        MapData mapdata = Items.filled_map.getMapData(itemToRender, this.mc.theWorld);

        if (mapdata != null)
        {
            this.mc.entityRenderer.getMapItemRenderer().func_148250_a(mapdata, false);
        }
    }

    public void renderArmSkin(EntityPlayer p_178102_1_) {
        bindPlayerTexture(p_178102_1_);
        Render render = mc.entityRenderer.itemRenderer.renderManager.getEntityRenderObject((EntityPlayer)ReplayHandler.getCurrentEntity());
        RenderPlayer renderplayer = (RenderPlayer)render;

        if (!p_178102_1_.isInvisible()) {
            func_180534_a(renderplayer, p_178102_1_);
            func_178106_b(renderplayer, p_178102_1_);
        }
    }

    public void func_180534_a(RenderPlayer p_180534_1_, EntityPlayer player) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(54.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(64.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-62.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.25F, -0.85F, 0.75F);
        func_177138_b(p_180534_1_, player);
        GlStateManager.popMatrix();
    }

    public void func_178106_b(RenderPlayer p_178106_1_, EntityPlayer player) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(92.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(45.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(41.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(-0.3F, -1.1F, 0.45F);
        func_177139_c(p_178106_1_, player);
        GlStateManager.popMatrix();
    }

    public void func_177139_c(RenderPlayer r, EntityPlayer p_177139_1_) {
        float f = 1.0F;
        GlStateManager.color(f, f, f);
        ModelPlayer modelplayer = r.getPlayerModel();
        func_177137_d(r, p_177139_1_);
        modelplayer.isSneak = false;
        modelplayer.swingProgress = 0.0F;
        modelplayer.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, p_177139_1_);
        modelplayer.func_178726_b();
    }

    public void renderItem1(EntityPlayer p_178104_1_, float p_178104_2_) {
        float f1 = (float)p_178104_1_.getItemInUseCount() - p_178104_2_ + 1.0F;
        float f2 = f1 / (float)itemToRender.getMaxItemUseDuration();
        float f3 = MathHelper.abs(MathHelper.cos(f1 / 4.0F * (float)Math.PI) * 0.1F);

        if (f2 >= 0.8F) {
            f3 = 0.0F;
        }

        GlStateManager.translate(0.0F, f3, 0.0F);
        float f4 = 1.0F - (float)Math.pow((double)f2, 27.0D);
        GlStateManager.translate(f4 * 0.6F, f4 * -0.5F, f4 * 0.0F);
        GlStateManager.rotate(f4 * 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f4 * 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f4 * 30.0F, 0.0F, 0.0F, 1.0F);
    }

    public void renderItem2(float p_178098_1_, EntityPlayer p_178098_2_) {
        GlStateManager.rotate(-18.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-12.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-0.9F, 0.2F, 0.0F);
        float f1 = (float)itemToRender.getMaxItemUseDuration() - ((float)p_178098_2_.getItemInUseCount() - p_178098_1_ + 1.0F);
        float f2 = f1 / 20.0F;
        f2 = (f2 * f2 + f2 * 2.0F) / 3.0F;

        if (f2 > 1.0F)
        {
            f2 = 1.0F;
        }

        if (f2 > 0.1F)
        {
            float f3 = MathHelper.sin((f1 - 0.1F) * 1.3F);
            float f4 = f2 - 0.1F;
            float f5 = f3 * f4;
            GlStateManager.translate(f5 * 0.0F, f5 * 0.01F, f5 * 0.0F);
        }

        GlStateManager.translate(f2 * 0.0F, f2 * 0.0F, f2 * 0.1F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F + f2 * 0.2F);
    }

    private void bindPlayerTexture(EntityPlayer player) {
        this.mc.getTextureManager().bindTexture(
                SkinProvider.getResourceLocationForPlayerUUID(player.getUniqueID()));
    }

    public void func_178095_a(EntityPlayer p_178095_1_, float p_178095_2_, float p_178095_3_)
    {
        float f2 = -0.3F * MathHelper.sin(MathHelper.sqrt_float(p_178095_3_) * (float)Math.PI);
        float f3 = 0.4F * MathHelper.sin(MathHelper.sqrt_float(p_178095_3_) * (float)Math.PI * 2.0F);
        float f4 = -0.4F * MathHelper.sin(p_178095_3_ * (float)Math.PI);
        GlStateManager.translate(f2, f3, f4);
        GlStateManager.translate(0.64000005F, -0.6F, -0.71999997F);
        GlStateManager.translate(0.0F, p_178095_2_ * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f5 = MathHelper.sin(p_178095_3_ * p_178095_3_ * (float)Math.PI);
        float f6 = MathHelper.sin(MathHelper.sqrt_float(p_178095_3_) * (float)Math.PI);
        GlStateManager.rotate(f6 * 70.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f5 * -20.0F, 0.0F, 0.0F, 1.0F);
        bindPlayerTexture(p_178095_1_);
        GlStateManager.translate(-1.0F, 3.6F, 3.5F);
        GlStateManager.rotate(120.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(200.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.translate(5.6F, 0.0F, 0.0F);
        Render render = mc.entityRenderer.itemRenderer.renderManager.getEntityRenderObject(p_178095_1_);
        RenderPlayer renderplayer = (RenderPlayer)render;
        func_177138_b(renderplayer, p_178095_1_);
    }

    public void func_177138_b(RenderPlayer r, EntityPlayer p_177138_1_) {
        float f = 1.0F;
        GlStateManager.color(f, f, f);
        ModelPlayer modelplayer = r.getPlayerModel();
        func_177137_d(r, p_177138_1_);
        modelplayer.swingProgress = 0.0F;
        modelplayer.isSneak = false;
        modelplayer.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, p_177138_1_);
        modelplayer.func_178725_a();
    }

    private void func_177137_d(RenderPlayer r, EntityPlayer p_177137_1_) {
        ModelPlayer modelplayer = r.getPlayerModel();

        if (p_177137_1_.isSpectator())
        {
            modelplayer.setInvisible(false);
            modelplayer.bipedHead.showModel = true;
            modelplayer.bipedHeadwear.showModel = true;
        }
        else
        {
            ItemStack itemstack = p_177137_1_.inventory.getCurrentItem();
            modelplayer.setInvisible(true);
            modelplayer.bipedHeadwear.showModel = p_177137_1_.func_175148_a(EnumPlayerModelParts.HAT);
            modelplayer.bipedBodyWear.showModel = p_177137_1_.func_175148_a(EnumPlayerModelParts.JACKET);
            modelplayer.bipedLeftLegwear.showModel = p_177137_1_.func_175148_a(EnumPlayerModelParts.LEFT_PANTS_LEG);
            modelplayer.bipedRightLegwear.showModel = p_177137_1_.func_175148_a(EnumPlayerModelParts.RIGHT_PANTS_LEG);
            modelplayer.bipedLeftArmwear.showModel = p_177137_1_.func_175148_a(EnumPlayerModelParts.LEFT_SLEEVE);
            modelplayer.bipedRightArmwear.showModel = p_177137_1_.func_175148_a(EnumPlayerModelParts.RIGHT_SLEEVE);
            modelplayer.heldItemLeft = 0;
            modelplayer.aimedBow = false;
            modelplayer.isSneak = p_177137_1_.isSneaking();

            if (itemstack == null)
            {
                modelplayer.heldItemRight = 0;
            }
            else
            {
                modelplayer.heldItemRight = 1;

                if (p_177137_1_.getItemInUseCount() > 0)
                {
                    EnumAction enumaction = itemstack.getItemUseAction();

                    if (enumaction == EnumAction.BLOCK)
                    {
                        modelplayer.heldItemRight = 3;
                    }
                    else if (enumaction == EnumAction.BOW)
                    {
                        modelplayer.aimedBow = true;
                    }
                }
            }
        }
    }

    private ItemStack itemToRender;
    private float prevEquippedProgress, equippedProgress;

    public void updateEquippedItem(EntityPlayer player) {
        prevEquippedProgress = equippedProgress;
        ItemStack itemstack = player.inventory.getCurrentItem();
        boolean flag = false;

        if (itemToRender != null && itemstack != null)
        {
            if (!itemToRender.getIsItemStackEqual(itemstack))
            {
                flag = true;
            }
        }
        else if(itemToRender == null && itemstack == null)
        {
            flag = false;
        }
        else
        {
            flag = true;
        }

        float f = 0.4F;
        float f1 = flag ? 0.0F : 1.0F;
        float f2 = MathHelper.clamp_float(f1 - equippedProgress, -f, f);
        equippedProgress += f2;

        if (equippedProgress < 0.1F)
        {
            itemToRender = itemstack;
            mc.entityRenderer.itemRenderer.equippedItemSlot = player.inventory.currentItem;
        }
    }
}
