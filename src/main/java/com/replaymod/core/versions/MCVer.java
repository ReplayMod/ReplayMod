package com.replaymod.core.versions;

import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventBus;

//#if MC>=10904
//#if MC>=11200
import net.minecraft.client.renderer.BufferBuilder;
//#else
//$$ import net.minecraft.client.renderer.VertexBuffer;
//#endif
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.MathHelper;
//#else
//$$ import net.minecraft.client.particle.EntityFX;
//$$ import net.minecraft.client.renderer.WorldRenderer;
//$$ import net.minecraft.util.MathHelper;
//#endif
//#if MC>=10809
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
//#else
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif

import java.io.File;
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
        //$$ return buffer.readStringFromBuffer(max);
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

    public static WorldType WorldType_DEBUG_ALL_BLOCK_STATES
            //#if MC>=11200
            = WorldType.DEBUG_ALL_BLOCK_STATES;
            //#else
            //$$ = WorldType.DEBUG_WORLD;
            //#endif

    public static ResourceLocation LOCATION_BLOCKS_TEXTURE
            //#if MC>=10904
            = TextureMap.LOCATION_BLOCKS_TEXTURE;
            //#else
            //$$ = TextureMap.locationBlocksTexture;
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
        return chunk.getEntityLists();
    }

    @SuppressWarnings("unchecked")
    public static List<ModelBox> cubeList(ModelRenderer modelRenderer) {
        return modelRenderer.cubeList;
    }

    @SuppressWarnings("unchecked")
    public static List<EntityPlayer> playerEntities(World world) {
        return world.playerEntities;
    }

    public static GlStateManager.BooleanState fog(GlStateManager.FogState fogState) {
        //#if MC>=10809
        return fogState.fog;
        //#else
        //$$ return fogState.field_179049_a;
        //#endif
    }

    public static void fog(GlStateManager.FogState fogState, GlStateManager.BooleanState fog) {
        //#if MC>=10809
        fogState.fog = fog;
        //#else
        //$$ fogState.field_179049_a = fog;
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
        //$$ return repo.func_177319_a(file);
        //#endif
    }

    public static void BufferBuilder_setTranslation(double x, double y, double z) {
        //#if MC>=10904
        Tessellator.getInstance().getBuffer().setTranslation(x, y, z);
        //#else
        //$$ Tessellator.getInstance().getWorldRenderer().setTranslation(x, y, z);
        //#endif
    }

    public static void BufferBuilder_beginPosCol(int mode) {
        //#if MC>=10904
        Tessellator.getInstance().getBuffer().begin(mode, DefaultVertexFormats.POSITION_COLOR);
        //#else
        //#if MC>=10809
        //$$ Tessellator.getInstance().getWorldRenderer().begin(mode, DefaultVertexFormats.POSITION_COLOR);
        //#else
        //$$ Tessellator.getInstance().getWorldRenderer().startDrawing(mode);
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
        //$$ WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
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
        //$$ Tessellator.getInstance().getWorldRenderer().startDrawing(mode);
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
        //$$ WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
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
        //$$ Tessellator.getInstance().getWorldRenderer().startDrawing(mode);
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
        //$$ WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        //#endif
        //#if MC>=10809
        worldRenderer.pos(x, y, z).tex(u, v).color(r, g, b, a).endVertex();
        //#else
        //$$ worldRenderer.setColorRGBA(r, g, b, a);
        //$$ worldRenderer.addVertexWithUV(x, y, z, u, v);
        //#endif
    }

    //#if MC>=10904
    //#if MC>=11200
    public static BufferBuilder getBuffer(Tessellator tessellator) {
    //#else
    //$$ public static VertexBuffer getBuffer(Tessellator tessellator) {
    //#endif
    //#else
    //$$ public static WorldRenderer getBuffer(Tessellator tessellator) {
    //#endif
        //#if MC>=10904
        return Tessellator.getInstance().getBuffer();
        //#else
        //$$ return Tessellator.getInstance().getWorldRenderer();
        //#endif
    }

    @SuppressWarnings("unchecked")
    public static List<VertexFormatElement> getElements(VertexFormat vertexFormat) {
        return vertexFormat.getElements();
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

    public static double interpPosX() {
        //#if MC>=10904
        return Particle.interpPosX;
        //#else
        //$$ return EntityFX.interpPosX;
        //#endif
    }

    public static double interpPosY() {
        //#if MC>=10904
        return Particle.interpPosY;
        //#else
        //$$ return EntityFX.interpPosY;
        //#endif
    }

    public static double interpPosZ() {
        //#if MC>=10904
        return Particle.interpPosZ;
        //#else
        //$$ return EntityFX.interpPosZ;
        //#endif
    }
}
