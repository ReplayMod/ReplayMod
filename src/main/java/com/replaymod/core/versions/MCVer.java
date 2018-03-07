package com.replaymod.core.versions;

import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

//#if MC>=10904
//#if MC>=11200
import net.minecraft.client.renderer.BufferBuilder;
//#else
//$$ import net.minecraft.client.renderer.VertexBuffer;
//#endif
import net.minecraft.util.math.MathHelper;
//#else
//#if MC>=10800
//$$ import net.minecraft.client.renderer.WorldRenderer;
//#endif
//$$ import net.minecraft.util.MathHelper;
//#endif
//#if MC>=10809
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
//#else
//#if MC>=10800
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#else
//$$ import cpw.mods.fml.common.FMLCommonHandler;
//#endif
//#endif

//#if MC>=10800
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.BooleanState;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.eventhandler.EventBus;
//#else
//$$ import com.google.common.util.concurrent.Futures;
//$$ import com.replaymod.render.hooks.GLStateTracker;
//$$ import com.replaymod.render.hooks.GLStateTracker.BooleanState;
//$$ import cpw.mods.fml.common.eventhandler.EventBus;
//$$ import io.netty.handler.codec.DecoderException;
//$$ import net.minecraft.client.resources.FileResourcePack;
//$$
//$$ import static org.lwjgl.opengl.GL11.*;
//#endif

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    public static EventBus FORGE_BUS = MinecraftForge.EVENT_BUS;
    //#if MC>=10809
    public static EventBus FML_BUS = FORGE_BUS;
    //#else
    //$$ public static EventBus FML_BUS = FMLCommonHandler.instance().bus();
    //#endif

    public static void addDetail(CrashReportCategory category, String name, Callable<String> callable) {
        //#if MC>=10904
        //#if MC>=11200
        category.addDetail(name, callable::call);
        //#else
        //$$ category.setDetail(name, callable::call);
        //#endif
        //#else
        //$$ category.addCrashSectionCallable(name, callable);
        //#endif
    }

    @SuppressWarnings("unchecked")
    public static List<GuiButton> getButtonList(GuiScreenEvent.InitGuiEvent event) {
        //#if MC>=10904
        return event.getButtonList();
        //#else
        //$$ return event.buttonList;
        //#endif
    }

    public static GuiButton getButton(GuiScreenEvent.ActionPerformedEvent event) {
        //#if MC>=10904
        return event.getButton();
        //#else
        //$$ return event.button;
        //#endif
    }

    public static GuiScreen getGui(GuiScreenEvent event) {
        //#if MC>=10904
        return event.getGui();
        //#else
        //$$ return event.gui;
        //#endif
    }

    public static int x(GuiButton button) {
        //#if MC>=11200
        return button.x;
        //#else
        //$$ return button.xPosition;
        //#endif
    }

    public static int y(GuiButton button) {
        //#if MC>=11200
        return button.y;
        //#else
        //$$ return button.yPosition;
        //#endif
    }

    public static void y(GuiButton button, int val) {
        //#if MC>=11200
        button.y = val;
        //#else
        //$$ button.yPosition = val;
        //#endif
    }

    public static EntityLivingBase getEntity(RenderLivingEvent event) {
        //#if MC>=10904
        return event.getEntity();
        //#else
        //$$ return event.entity;
        //#endif
    }

    public static NetHandlerPlayClient getConnection(Minecraft mc) {
        //#if MC>=10904
        return mc.getConnection();
        //#else
        //$$ return mc.getNetHandler();
        //#endif
    }

    public static String readString(PacketBuffer buffer, int max) {
        //#if MC>=11102
        return buffer.readString(max);
        //#else
        //#if MC>=10800
        //$$ return buffer.readStringFromBuffer(max);
        //#else
        //$$ try {
        //$$     return buffer.readStringFromBuffer(max);
        //$$ } catch (IOException e) {
        //$$     throw new DecoderException(e);
        //$$ }
        //#endif
        //#endif
    }

    public static int readVarInt(PacketBuffer buffer) {
        //#if MC>=11102
        return buffer.readVarInt();
        //#else
        //$$ return buffer.readVarIntFromBuffer();
        //#endif
    }

    public static void writeVarInt(PacketBuffer buffer, int val) {
        //#if MC>=11102
        buffer.writeVarInt(val);
        //#else
        //$$ buffer.writeVarIntToBuffer(val);
        //#endif
    }

    public static FontRenderer getFontRenderer(Minecraft mc) {
        //#if MC>=11200
        return mc.fontRenderer;
        //#else
        //$$ return mc.fontRendererObj;
        //#endif
    }

    public static RenderGameOverlayEvent.ElementType getType(RenderGameOverlayEvent event) {
        //#if MC>=10904
        return event.getType();
        //#else
        //$$ return event.type;
        //#endif
    }

    //#if MC>=10800
    public static WorldType WorldType_DEBUG_ALL_BLOCK_STATES
            //#if MC>=11200
            = WorldType.DEBUG_ALL_BLOCK_STATES;
            //#else
            //$$ = WorldType.DEBUG_WORLD;
            //#endif
    //#endif

    //#if MC>=10800
    public static Entity getRenderViewEntity(Minecraft mc) {
        return mc.getRenderViewEntity();
    }
    //#else
    //$$ public static EntityLivingBase getRenderViewEntity(Minecraft mc) {
    //$$     return mc.renderViewEntity;
    //$$ }
    //#endif

    //#if MC>=10800
    public static void setRenderViewEntity(Minecraft mc, Entity entity) {
        mc.setRenderViewEntity(entity);
    }
    //#else
    //$$ public static void setRenderViewEntity(Minecraft mc, EntityLivingBase entity) {
    //$$     mc.renderViewEntity = entity;
    //$$ }
    //#endif

    public static EntityPlayerSP player(Minecraft mc) {
        //#if MC>=11102
        return mc.player;
        //#else
        //$$ return mc.thePlayer;
        //#endif
    }

    public static WorldClient world(Minecraft mc) {
        //#if MC>=11102
        return mc.world;
        //#else
        //$$ return mc.theWorld;
        //#endif
    }

    public static World world(Entity entity) {
        //#if MC>=11102
        return entity.world;
        //#else
        //$$ return entity.worldObj;
        //#endif
    }

    public static void world(Entity entity, World world) {
        //#if MC>=11102
        entity.world = world;
        //#else
        //$$ entity.worldObj = world;
        //#endif
    }

    public static Entity getRidingEntity(Entity ridden) {
        //#if MC>=10904
        return ridden.getRidingEntity();
        //#else
        //$$ return ridden.ridingEntity;
        //#endif
    }

    @SuppressWarnings("unchecked")
    public static List<Entity> loadedEntityList(World world) {
        return world.loadedEntityList;
    }

    @SuppressWarnings("unchecked")
    public static Collection<Entity>[] getEntityLists(Chunk chunk) {
        //#if MC>=10800
        return chunk.getEntityLists();
        //#else
        //$$ return chunk.entityLists;
        //#endif
    }

    @SuppressWarnings("unchecked")
    public static List<EntityPlayer> playerEntities(World world) {
        return world.playerEntities;
    }

    public static BooleanState fog() {
        //#if MC>=10809
        return GlStateManager.fogState.fog;
        //#else
        //#if MC>=10800
        //$$ return GlStateManager.fogState.field_179049_a;
        //#else
        //$$ return GLStateTracker.getInstance().fog;
        //#endif
        //#endif
    }

    public static void fog(BooleanState fog) {
        //#if MC>=10809
        GlStateManager.fogState.fog = fog;
        //#else
        //#if MC>=10800
        //$$ GlStateManager.fogState.field_179049_a = fog;
        //#else
        //$$ GLStateTracker.getInstance().fog = fog;
        //#endif
        //#endif
    }

    public static BooleanState texture2DState(int index) {
        //#if MC>=10800
        return GlStateManager.textureState[index].texture2DState;
        //#else
        //$$ return GLStateTracker.getInstance().texture[index];
        //#endif
    }

    public static void texture2DState(int index, BooleanState texture2DState) {
        //#if MC>=10800
        GlStateManager.textureState[index].texture2DState = texture2DState;
        //#else
        //$$ GLStateTracker.getInstance().texture[index] = texture2DState;
        //#endif
    }

    public static void ServerList_saveSingleServer(ServerData serverData) {
        //#if MC>=10904
        ServerList.saveSingleServer(serverData);
        //#else
        //$$ ServerList.func_147414_b(serverData);
        //#endif
    }

    public static void sendPacket(NetHandlerPlayClient netHandler, Packet packet) {
        //#if MC>=10904
        netHandler.sendPacket(packet);
        //#else
        //$$ netHandler.addToSendQueue(packet);
        //#endif
    }

    public static ScaledResolution newScaledResolution(Minecraft mc) {
        //#if MC>=10809
        return new ScaledResolution(mc);
        //#else
        //$$ return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        //#endif
    }

    public static ListenableFuture setServerResourcePack(ResourcePackRepository repo, File file) {
        //#if MC>=10809
        //#if MC>=11200
        return repo.setServerResourcePack(file);
        //#else
        //$$ return repo.setResourcePackInstance(file);
        //#endif
        //#else
        //#if MC>=10800
        //$$ return repo.func_177319_a(file);
        //#else
        //$$ repo.field_148533_g = false;
        //$$ repo.field_148532_f = new FileResourcePack(file);
        //$$ Minecraft.getMinecraft().scheduleResourcesRefresh();
        //$$ return Futures.immediateFuture(null);
        //#endif
        //#endif
    }

    public static boolean isKeyDown(KeyBinding keyBinding) {
        //#if MC>=10800
        return keyBinding.isKeyDown();
        //#else
        //$$ return keyBinding.getIsKeyPressed();
        //#endif
    }

    public static void BufferBuilder_setTranslation(double x, double y, double z) {
        //#if MC>=10904
        Tessellator.getInstance().getBuffer().setTranslation(x, y, z);
        //#else
        //#if MC>=10800
        //$$ Tessellator.getInstance().getWorldRenderer().setTranslation(x, y, z);
        //#else
        //$$ Tessellator.instance.setTranslation(x, y, z);
        //#endif
        //#endif
    }

    public static void BufferBuilder_beginPosCol(int mode) {
        //#if MC>=10904
        Tessellator.getInstance().getBuffer().begin(mode, DefaultVertexFormats.POSITION_COLOR);
        //#else
        //#if MC>=10809
        //$$ Tessellator.getInstance().getWorldRenderer().begin(mode, DefaultVertexFormats.POSITION_COLOR);
        //#else
        //#if MC>=10800
        //$$ Tessellator.getInstance().getWorldRenderer().startDrawing(mode);
        //#else
        //$$ Tessellator.instance.startDrawing(mode);
        //#endif
        //#endif
        //#endif
    }

    public static void BufferBuilder_addPosCol(double x, double y, double z, int r, int g, int b, int a) {
        //#if MC>=10904
        //#if MC>=11200
        BufferBuilder worldRenderer = Tessellator.getInstance().getBuffer();
        //#else
        //$$ VertexBuffer worldRenderer = Tessellator.getInstance().getBuffer();
        //#endif
        //#else
        //#if MC>=10800
        //$$ WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        //#else
        //$$ Tessellator worldRenderer = Tessellator.instance;
        //#endif
        //#endif
        //#if MC>=10809
        worldRenderer.pos(x, y, z).color(r, g, b, a).endVertex();
        //#else
        //$$ worldRenderer.setColorRGBA(r, g, b, a);
        //$$ worldRenderer.addVertex(x, y, z);
        //#endif
    }

    public static void BufferBuilder_beginPosTex(int mode) {
        //#if MC>=10904
        Tessellator.getInstance().getBuffer().begin(mode, DefaultVertexFormats.POSITION_TEX);
        //#else
        //#if MC>=10809
        //$$ Tessellator.getInstance().getWorldRenderer().begin(mode, DefaultVertexFormats.POSITION_TEX);
        //#else
        //#if MC>=10800
        //$$ Tessellator.getInstance().getWorldRenderer().startDrawing(mode);
        //#else
        //$$ Tessellator.instance.startDrawing(mode);
        //#endif
        //#endif
        //#endif
    }

    public static void BufferBuilder_addPosTex(double x, double y, double z, double u, double v) {
        //#if MC>=10904
        //#if MC>=11200
        BufferBuilder worldRenderer = Tessellator.getInstance().getBuffer();
        //#else
        //$$ VertexBuffer worldRenderer = Tessellator.getInstance().getBuffer();
        //#endif
        //#else
        //#if MC>=10800
        //$$ WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        //#else
        //$$ Tessellator worldRenderer = Tessellator.instance;
        //#endif
        //#endif
        //#if MC>=10809
        worldRenderer.pos(x, y, z).tex(u, v).endVertex();
        //#else
        //$$ worldRenderer.addVertexWithUV(x, y, z, u, v);
        //#endif
    }

    public static void BufferBuilder_beginPosTexCol(int mode) {
        //#if MC>=10904
        Tessellator.getInstance().getBuffer().begin(mode, DefaultVertexFormats.POSITION_TEX_COLOR);
        //#else
        //#if MC>=10809
        //$$ Tessellator.getInstance().getWorldRenderer().begin(mode, DefaultVertexFormats.POSITION_TEX_COLOR);
        //#else
        //#if MC>=10800
        //$$ Tessellator.getInstance().getWorldRenderer().startDrawing(mode);
        //#else
        //$$ Tessellator.instance.startDrawing(mode);
        //#endif
        //#endif
        //#endif
    }

    public static void BufferBuilder_addPosTexCol(double x, double y, double z, double u, double v, int r, int g, int b, int a) {
        //#if MC>=10904
        //#if MC>=11200
        BufferBuilder worldRenderer = Tessellator.getInstance().getBuffer();
        //#else
        //$$ VertexBuffer worldRenderer = Tessellator.getInstance().getBuffer();
        //#endif
        //#else
        //#if MC>=10800
        //$$ WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        //#else
        //$$ Tessellator worldRenderer = Tessellator.instance;
        //#endif
        //#endif
        //#if MC>=10809
        worldRenderer.pos(x, y, z).tex(u, v).color(r, g, b, a).endVertex();
        //#else
        //$$ worldRenderer.setColorRGBA(r, g, b, a);
        //$$ worldRenderer.addVertexWithUV(x, y, z, u, v);
        //#endif
    }

    public static Tessellator Tessellator_getInstance() {
        //#if MC>=10800
        return Tessellator.getInstance();
        //#else
        //$$ return Tessellator.instance;
        //#endif
    }

    public static RenderManager getRenderManager() {
        //#if MC>=10800
        return Minecraft.getMinecraft().getRenderManager();
        //#else
        //$$ return RenderManager.instance;
        //#endif
    }

    public static int floor(double val) {
        //#if MC>=11102
        return MathHelper.floor(val);
        //#else
        //$$ return MathHelper.floor_double(val);
        //#endif
    }

    public static float cos(float val) {
        return MathHelper.cos(val);
    }

    public static float sin(float val) {
        return MathHelper.sin(val);
    }

    //#if MC<=10710
    //$$ public static class GlStateManager {
    //$$     public static void resetColor() { /* nop */ }
    //$$     public static void clearColor(float r, float g, float b, float a) { glClearColor(r, g, b, a); }
    //$$     public static void enableTexture2D() { glEnable(GL_TEXTURE_2D); }
    //$$     public static void enableAlpha() { glEnable(GL_ALPHA_TEST); }
    //$$     public static void alphaFunc(int func, float ref) { glAlphaFunc(func, ref); }
    //$$     public static void enableDepth() { glEnable(GL_DEPTH_TEST); }
    //$$     public static void pushMatrix() { glPushMatrix(); }
    //$$     public static void popAttrib() { glPopAttrib(); }
    //$$     public static void popMatrix() { glPopMatrix(); }
    //$$     public static void clear(int mask) { glClear(mask); }
    //$$     public static void translate(double x, double y, double z) { glTranslated(x, y, z); }
    //$$     public static void rotate(float angle, float x, float y, float z) { glRotatef(angle, x, y, z); }
    //$$ }
    //#endif
}
