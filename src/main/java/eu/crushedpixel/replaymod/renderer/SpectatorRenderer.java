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
    private EntityPlayer currentPlayer;

    private EntityPlayer getSpectatedPlayer() {
        Entity current = ReplayHandler.getCurrentEntity();
        if(!(current instanceof EntityPlayer)) return null;
        return (EntityPlayer)current;
    }

    protected void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        Project.gluPerspective(fovY, aspect, zNear, zFar);
    }

    public void renderSpectatorHand(EntityPlayer entityPlayer, float partialTicks, int renderPass) {
        if (entityPlayer != currentPlayer) {
            updateNow(entityPlayer);
        }

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();

        float f1 = 0.07F;

        if(this.mc.gameSettings.anaglyph) {
            GlStateManager.translate((float)(-(renderPass * 2 - 1)) * f1, 0.0F, 0.0F);
        }

        gluPerspective(mc.entityRenderer.getFOVModifier(partialTicks, false),
                (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, mc.entityRenderer.farPlaneDistance * 2.0F);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();

        if(this.mc.gameSettings.anaglyph) {
            GlStateManager.translate((float)(renderPass * 2 - 1) * 0.1F, 0.0F, 0.0F);
        }

        GlStateManager.pushMatrix();
        mc.entityRenderer.hurtCameraEffect(partialTicks);

        if(this.mc.gameSettings.viewBobbing) {
            mc.entityRenderer.setupViewBobbing(partialTicks);
        }

        boolean sleeping = this.mc.getRenderViewEntity() instanceof EntityLivingBase &&
                ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();

        if(this.mc.gameSettings.thirdPersonView == 0 && !sleeping && !this.mc.gameSettings.hideGUI) {
            mc.entityRenderer.enableLightmap();
            renderItemInFirstPerson(partialTicks);
            mc.entityRenderer.disableLightmap();
        }

        GlStateManager.popMatrix();

        if(this.mc.gameSettings.thirdPersonView == 0 && !sleeping) {
            renderOverlays(partialTicks, entityPlayer);
            mc.entityRenderer.hurtCameraEffect(partialTicks);
        }

        if(this.mc.gameSettings.viewBobbing) {
            mc.entityRenderer.setupViewBobbing(partialTicks);
        }
    }

    @SubscribeEvent
    public void renderHandEvent(RenderHandEvent event) {
        if(!ReplayHandler.isInReplay() || ReplayHandler.isCamera()) {
            return;
        }

        EntityPlayer entityPlayer = getSpectatedPlayer();
        if(entityPlayer == null) return;

        renderSpectatorHand(entityPlayer, event.partialTicks, event.renderPass);
        event.setCanceled(true);
    }

    public void renderItemInFirstPerson(float partialTicks) {
        EntityPlayer entityPlayer = getSpectatedPlayer();

        float equippedProgressState = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
        float swingProgress = entityPlayer.getSwingProgress(partialTicks);
        float rotationPitchState = entityPlayer.prevRotationPitch + (entityPlayer.rotationPitch - entityPlayer.prevRotationPitch) * partialTicks;
        float rotationYawState = entityPlayer.prevRotationYaw + (entityPlayer.rotationYaw - entityPlayer.prevRotationYaw) * partialTicks;

        //lights the hand
        mc.entityRenderer.itemRenderer.func_178101_a(rotationPitchState, rotationYawState);
        setLightmapTextureCoords(entityPlayer);

        //sets the rotation of the hand
        rotateHandPosition(entityPlayer, partialTicks);

        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        if (itemToRender != null) {

            //treat map rendering diffently
            if(itemToRender.getItem() == Items.filled_map) {
                renderMapInHand(entityPlayer, rotationPitchState, equippedProgressState, swingProgress);
            }

            else if(entityPlayer.getItemInUseCount() > 0) {
                EnumAction action = itemToRender.getItemUseAction();

                //places the arm according to the current action
                switch(action) {
                    case NONE:
                        mc.entityRenderer.itemRenderer.func_178096_b(equippedProgressState, 0.0F);
                        break;
                    case EAT:
                    case DRINK:
                        prepareFoodItem(entityPlayer, partialTicks);
                        mc.entityRenderer.itemRenderer.func_178096_b(equippedProgressState, 0.0F);
                        break;
                    case BLOCK:
                        mc.entityRenderer.itemRenderer.func_178096_b(equippedProgressState, 0.0F);
                        //rotate into blocking animation position
                        mc.entityRenderer.itemRenderer.func_178103_d();
                        break;
                    case BOW:
                        mc.entityRenderer.itemRenderer.func_178096_b(equippedProgressState, 0.0F);
                        prepareBowItem(partialTicks, entityPlayer);
                }
            }
            else {
                //swings the arm
                mc.entityRenderer.itemRenderer.func_178105_d(swingProgress);
                mc.entityRenderer.itemRenderer.func_178096_b(equippedProgressState, swingProgress);
            }

            mc.entityRenderer.itemRenderer.renderItem(entityPlayer, itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
        }
        else if (!entityPlayer.isInvisible()) {
            renderPlayerHand(entityPlayer, equippedProgressState, swingProgress);
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    /**
     * Rendering a Map Item
     */

    public void renderMapInHand(EntityPlayer p_178097_1_, float p_178097_2_, float p_178097_3_, float p_178097_4_) {
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
        renderMapArms(p_178097_1_);
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

        if(mapdata != null) {
            this.mc.entityRenderer.getMapItemRenderer().func_148250_a(mapdata, false);
        }
    }

    public void renderMapArms(EntityPlayer entityPlayer) {
        bindPlayerTexture(entityPlayer);
        Render render = mc.entityRenderer.itemRenderer.renderManager.getEntityRenderObject((EntityPlayer)ReplayHandler.getCurrentEntity());
        RenderPlayer renderplayer = (RenderPlayer)render;

        if(!entityPlayer.isInvisible()) {
            renderRightMapArm(renderplayer, entityPlayer);
            renderLeftMapArm(renderplayer, entityPlayer);
        }
    }

    public void renderRightMapArm(RenderPlayer renderPlayer, EntityPlayer entityPlayer) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(54.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(64.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-62.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.25F, -0.85F, 0.75F);
        renderRightArm(renderPlayer, entityPlayer);
        GlStateManager.popMatrix();
    }

    public void renderLeftMapArm(RenderPlayer renderPlayer, EntityPlayer entityPlayer) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(92.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(45.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(41.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(-0.3F, -1.1F, 0.45F);
        renderRightArm(renderPlayer, entityPlayer);
        GlStateManager.popMatrix();
    }

    /**
     * Basic Rendering Preparations
     */

    public void setLightmapTextureCoords(EntityPlayer p_178109_1_) {
        int i = this.mc.theWorld.getCombinedLight(new BlockPos(p_178109_1_.posX, p_178109_1_.posY + (double)p_178109_1_.getEyeHeight(), p_178109_1_.posZ), 0);
        float f = (float)(i & 65535);
        float f1 = (float)(i >> 16);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, f1);
    }

    public void rotateHandPosition(EntityPlayer p_178110_1_, float p_178110_2_) {
        float f1 = prevRenderArmPitch + (renderArmPitch - prevRenderArmPitch) * p_178110_2_;
        float f2 = prevRenderArmYaw + (renderArmYaw - prevRenderArmYaw) * p_178110_2_;
        GlStateManager.rotate((p_178110_1_.rotationPitch - f1) * 0.1F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((p_178110_1_.rotationYaw - f2) * 0.1F, 0.0F, 1.0F, 0.0F);
    }

    private void bindPlayerTexture(EntityPlayer player) {
        this.mc.getTextureManager().bindTexture(
                SkinProvider.getResourceLocationForPlayerUUID(player.getUniqueID()));
    }

    public void renderPlayerHand(EntityPlayer p_178095_1_, float equippedProgressState, float swingProgress) {
        float f2 = -0.3F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        float f3 = 0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI * 2.0F);
        float f4 = -0.4F * MathHelper.sin(swingProgress * (float)Math.PI);

        GlStateManager.translate(f2, f3, f4);
        GlStateManager.translate(0.64000005F, -0.6F, -0.71999997F);
        GlStateManager.translate(0.0F, equippedProgressState * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);

        float f5 = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        float f6 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);

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

        renderRightArm(renderplayer, p_178095_1_);
    }

    public void renderRightArm(RenderPlayer renderPlayer, EntityPlayer entityPlayer) {
        float f = 1.0F;
        GlStateManager.color(f, f, f);
        ModelPlayer modelplayer = renderPlayer.getPlayerModel();
        prepareModelPlayer(renderPlayer, entityPlayer);
        modelplayer.swingProgress = 0.0F;
        modelplayer.isSneak = false;
        modelplayer.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, entityPlayer);
        modelplayer.func_178725_a();
    }

    private void prepareModelPlayer(RenderPlayer renderPlayer, EntityPlayer entityPlayer) {
        ModelPlayer modelplayer = renderPlayer.getPlayerModel();

        if (entityPlayer.isSpectator()) {
            modelplayer.setInvisible(false);
            modelplayer.bipedHead.showModel = true;
            modelplayer.bipedHeadwear.showModel = true;
        } else {
            ItemStack itemstack = entityPlayer.inventory.getCurrentItem();
            modelplayer.setInvisible(true);
            modelplayer.bipedHeadwear.showModel = entityPlayer.func_175148_a(EnumPlayerModelParts.HAT);
            modelplayer.bipedBodyWear.showModel = entityPlayer.func_175148_a(EnumPlayerModelParts.JACKET);
            modelplayer.bipedLeftLegwear.showModel = entityPlayer.func_175148_a(EnumPlayerModelParts.LEFT_PANTS_LEG);
            modelplayer.bipedRightLegwear.showModel = entityPlayer.func_175148_a(EnumPlayerModelParts.RIGHT_PANTS_LEG);
            modelplayer.bipedLeftArmwear.showModel = entityPlayer.func_175148_a(EnumPlayerModelParts.LEFT_SLEEVE);
            modelplayer.bipedRightArmwear.showModel = entityPlayer.func_175148_a(EnumPlayerModelParts.RIGHT_SLEEVE);
            modelplayer.heldItemLeft = 0;
            modelplayer.aimedBow = false;
            modelplayer.isSneak = entityPlayer.isSneaking();

            if (itemstack == null) {
                modelplayer.heldItemRight = 0;
            } else {
                modelplayer.heldItemRight = 1;

                if(entityPlayer.getItemInUseCount() > 0) {
                    EnumAction enumaction = itemstack.getItemUseAction();

                    if (enumaction == EnumAction.BLOCK) {
                        modelplayer.heldItemRight = 3;
                    } else if (enumaction == EnumAction.BOW) {
                        modelplayer.aimedBow = true;
                    }
                }
            }
        }
    }

    /**
     * Preparing the Rendering of drawn Bows or Food/Potions which are being consumed
     */

    public void prepareFoodItem(EntityPlayer entityPlayer, float partialTicks) {
        float f1 = (float)entityPlayer.getItemInUseCount() - partialTicks + 1.0F;
        float f2 = f1 / (float)itemToRender.getMaxItemUseDuration();
        float f3 = MathHelper.abs(MathHelper.cos(f1 / 4.0F * (float) Math.PI) * 0.1F);

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

    public void prepareBowItem(float partialTicks, EntityPlayer player) {
        GlStateManager.rotate(-18.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-12.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-0.9F, 0.2F, 0.0F);
        float drawState = (float)itemToRender.getMaxItemUseDuration() -
                ((float)player.getItemInUseCount() - partialTicks + 1.0F);
        float drawTicks = drawState / 20.0F;
        drawTicks = (drawTicks * drawTicks + drawTicks * 2.0F) / 3.0F;

        if (drawTicks > 1.0F) {
            drawTicks = 1.0F;
        }

        if (drawTicks > 0.1F) {
            float f3 = MathHelper.sin((drawState - 0.1F) * 1.3F);
            float f4 = drawTicks - 0.1F;
            float f5 = f3 * f4;
            GlStateManager.translate(f5 * 0.0F, f5 * 0.01F, f5 * 0.0F);
        }

        GlStateManager.translate(drawTicks * 0.0F, drawTicks * 0.0F, drawTicks * 0.1F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F + drawTicks * 0.2F);
    }

    /**
     * Handling the spectated Player's Arm Item, Hand Pitch and Yaw, and Equipment process
     */

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

    private float prevRenderArmPitch, renderArmPitch;
    private float prevRenderArmYaw, renderArmYaw;

    public void updateArmYawAndPitch(EntityPlayer player) {
        this.prevRenderArmYaw = this.renderArmYaw;
        this.prevRenderArmPitch = this.renderArmPitch;
        this.renderArmPitch = (float)((double)this.renderArmPitch + (double)(player.rotationPitch - this.renderArmPitch) * 0.5D);
        this.renderArmYaw = (float)((double)this.renderArmYaw + (double)(player.rotationYaw - this.renderArmYaw) * 0.5D);
    }

    private ItemStack itemToRender;
    private float prevEquippedProgress, equippedProgress;

    public void updateEquippedItem(EntityPlayer player) {
        prevEquippedProgress = equippedProgress;
        ItemStack itemstack = player.inventory.getCurrentItem();
        boolean flag = false;

        if(itemToRender != null && itemstack != null) {
            if(!itemToRender.getIsItemStackEqual(itemstack)) {
                flag = true;
            }
        } else if(itemToRender == null && itemstack == null) {
            flag = false;
        } else {
            flag = true;
        }

        float f = 0.4F;
        float f1 = flag ? 0.0F : 1.0F;
        float f2 = MathHelper.clamp_float(f1 - equippedProgress, -f, f);
        equippedProgress += f2;

        if(equippedProgress < 0.1F) {
            itemToRender = itemstack;
            mc.entityRenderer.itemRenderer.equippedItemSlot = player.inventory.currentItem;
        }
    }

    public void updateNow(EntityPlayer player) {
        this.currentPlayer = player;

        prevEquippedProgress = equippedProgress = 1;
        itemToRender = player.inventory.getCurrentItem();
        mc.entityRenderer.itemRenderer.equippedItemSlot = player.inventory.currentItem;

        renderArmYaw = prevRenderArmYaw = player.rotationYaw;
        renderArmPitch = prevRenderArmPitch = player.rotationPitch;
    }
    
    /**
     * The GUI Overlays (Fire, Water)
     */

    public void renderOverlays(float partialTicks, EntityPlayer player) {
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
                mc.entityRenderer.itemRenderer.func_178108_a(partialTicks, this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(iblockstate));
            }
        }

        if(!player.isSpectator()) {
            if (player.isInsideOfMaterial(Material.water)) {
                renderWaterOverlayTexture(partialTicks, player);
            }

            if (this.mc.thePlayer.isBurning()) {
                mc.entityRenderer.itemRenderer.renderFireInFirstPerson(partialTicks);
            }
        }

        GlStateManager.enableAlpha();
    }

    public void renderWaterOverlayTexture(float partialTicks, EntityPlayer player) {
        this.mc.getTextureManager().bindTexture(mc.entityRenderer.itemRenderer.RES_UNDERWATER_OVERLAY);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        float f1 = player.getBrightness(partialTicks);
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
}
