package com.replaymod.core.versions;

import com.replaymod.core.mixin.GuiScreenAccessor;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.model.ModelPart;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11500
import net.minecraft.client.model.ModelPart.Cuboid;
import java.util.ArrayList;
//#else
//$$ import net.minecraft.client.model.Box;
//#endif

//#if MC>=11400
import com.replaymod.core.mixin.AbstractButtonWidgetAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.AbstractButtonWidget;

import java.util.concurrent.CompletableFuture;
//#else
//$$ import com.google.common.util.concurrent.FutureCallback;
//$$ import com.google.common.util.concurrent.Futures;
//$$ import com.google.common.util.concurrent.ListenableFuture;
//$$ import net.minecraft.client.gui.GuiButton;
//$$ import net.minecraft.realms.RealmsSharedConstants;
//#endif

//#if FABRIC>=1
//#else
//$$ import net.minecraft.entity.LivingEntity;
//$$ import net.minecraftforge.client.event.GuiScreenEvent;
//$$ import net.minecraftforge.client.event.RenderGameOverlayEvent;
//$$ import net.minecraftforge.client.event.RenderLivingEvent;
//$$ import net.minecraftforge.common.MinecraftForge;
//$$ import net.minecraftforge.eventbus.api.IEventBus;
//#endif

//#if MC>=11400
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
//#else
//$$ import net.minecraft.client.gui.ScaledResolution;
//$$ import net.minecraft.client.resources.ResourcePackRepository;
//$$ import net.minecraftforge.fml.client.FMLClientHandler;
//$$ import org.apache.logging.log4j.LogManager;
//$$ import org.lwjgl.Sys;
//$$ import java.awt.Desktop;
//$$ import java.io.IOException;
//#endif

//#if MC>=10904
import com.replaymod.render.blend.mixin.ParticleAccessor;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.Vec3d;
//#endif

//#if MC>=10809
import net.minecraft.client.render.VertexFormats;
//#else
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif

//#if MC>=10800
import net.minecraft.client.render.BufferBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
//#if MC<11500
//$$ import net.minecraft.client.render.chunk.ChunkRenderTask;
//#endif
//#else
//$$ import com.replaymod.core.mixin.ResourcePackRepositoryAccessor;
//$$ import com.google.common.util.concurrent.Futures;
//$$ import io.netty.handler.codec.DecoderException;
//$$ import net.minecraft.client.resources.FileResourcePack;
//$$
//$$ import static org.lwjgl.opengl.GL11.*;
//#endif

//#if FABRIC>=1
import net.fabricmc.loader.api.FabricLoader;
//#else
//#if MC>=11400
//$$ import net.minecraftforge.fml.ModList;
//#else
//$$ import net.minecraftforge.fml.common.Loader;
//#endif
//#endif

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    private static Logger LOGGER = LogManager.getLogger();

    //#if FABRIC<=0
    //$$ public static IEventBus FORGE_BUS = MinecraftForge.EVENT_BUS;
    //#if MC>=10809
    //$$ public static IEventBus FML_BUS = FORGE_BUS;
    //#else
    //$$ public static EventBus FML_BUS = FMLCommonHandler.instance().bus();
    //#endif
    //#endif

    public static boolean isModLoaded(String id) {
        //#if FABRIC>=1
        return FabricLoader.getInstance().isModLoaded(id.toLowerCase());
        //#else
        //#if MC>=11400
        //$$ return ModList.get().isLoaded(id.toLowerCase());
        //#else
        //$$ return Loader.isModLoaded(id);
        //#endif
        //#endif
    }

    public static int getProtocolVersion() {
        //#if MC>=11400
        return SharedConstants.getGameVersion().getProtocolVersion();
        //#else
        //$$ return RealmsSharedConstants.NETWORK_PROTOCOL_VERSION;
        //#endif
    }

    public static PacketTypeRegistry getPacketTypeRegistry(boolean loginPhase) {
        return PacketTypeRegistry.get(
                ProtocolVersion.getProtocol(getProtocolVersion()),
                loginPhase ? State.LOGIN : State.PLAY
        );
    }

    public static void addDetail(CrashReportSection category, String name, Callable<String> callable) {
        //#if MC>=10904
        //#if MC>=11200
        category.add(name, callable::call);
        //#else
        //$$ category.setDetail(name, callable::call);
        //#endif
        //#else
        //$$ category.addCrashSectionCallable(name, callable);
        //#endif
    }

    public static double Entity_getX(Entity entity) {
        //#if MC>=11500
        return entity.getX();
        //#else
        //$$ return entity.x;
        //#endif
    }

    public static double Entity_getY(Entity entity) {
        //#if MC>=11500
        return entity.getY();
        //#else
        //$$ return entity.y;
        //#endif
    }

    public static double Entity_getZ(Entity entity) {
        //#if MC>=11500
        return entity.getZ();
        //#else
        //$$ return entity.z;
        //#endif
    }

    public static void Entity_setPos(Entity entity, double x, double y, double z) {
        //#if MC>=11500
        entity.setPos(x, y, z);
        //#else
        //$$ entity.x = x;
        //$$ entity.y = y;
        //$$ entity.z = z;
        //#endif
    }

    //#if MC>=11400
    public static void width(AbstractButtonWidget button, int value) {
        button.setWidth(value);
    }

    public static int width(AbstractButtonWidget button) {
        return button.getWidth();
    }

    public static int height(AbstractButtonWidget button) {
        return ((AbstractButtonWidgetAccessor) button).getHeight();
    }
    //#else
    //$$ public static void width(GuiButton button, int value) {
    //$$     button.width = value;
    //$$ }
    //$$
    //$$ public static int width(GuiButton button) {
    //$$     return button.width;
    //$$ }
    //$$
    //$$ public static int height(GuiButton button) {
    //$$     return button.height;
    //$$ }
    //#endif

    //#if FABRIC<=0
    //#if MC>=11400
    //$$ public static void addButton(GuiScreenEvent.InitGuiEvent event, Widget button) {
    //$$     event.addWidget(button);
    //$$ }
    //$$
    //$$ public static void removeButton(GuiScreenEvent.InitGuiEvent event, Widget button) {
    //$$     event.removeWidget(button);
    //$$ }
    //$$
    //$$ public static List<Widget> getButtonList(GuiScreenEvent.InitGuiEvent event) {
    //$$     return event.getWidgetList();
    //$$ }
    //$$
    //$$ public static Widget getButton(GuiScreenEvent.ActionPerformedEvent event) {
    //$$     return event.getButton();
    //$$ }
    //#else
    //$$ public static void addButton(GuiScreenEvent.InitGuiEvent event, GuiButton button) {
        //#if MC>=11400
        //$$ event.addButton(button);
        //#else
        //$$ getButtonList(event).add(button);
        //#endif
    //$$ }
    //$$
    //$$ public static void removeButton(GuiScreenEvent.InitGuiEvent event, GuiButton button) {
        //#if MC>=11400
        //$$ event.removeButton(button);
        //#else
        //$$ getButtonList(event).remove(button);
        //#endif
    //$$ }
    //$$
    //$$ @SuppressWarnings("unchecked")
    //$$ public static List<GuiButton> getButtonList(GuiScreenEvent.InitGuiEvent event) {
        //#if MC>=10904
        //$$ return event.getButtonList();
        //#else
        //$$ return event.buttonList;
        //#endif
    //$$ }
    //$$
    //$$ public static GuiButton getButton(GuiScreenEvent.ActionPerformedEvent event) {
        //#if MC>=10904
        //$$ return event.getButton();
        //#else
        //$$ return event.button;
        //#endif
    //$$ }
    //#endif
    //$$
    //$$ public static Screen getGui(GuiScreenEvent event) {
        //#if MC>=10904
        //$$ return event.getGui();
        //#else
        //$$ return event.gui;
        //#endif
    //$$ }
    //$$
    //$$ public static LivingEntity getEntity(RenderLivingEvent event) {
        //#if MC>=10904
        //$$ return event.getEntity();
        //#else
        //$$ return event.entity;
        //#endif
    //$$ }
    //#endif

    public static String readString(PacketByteBuf buffer, int max) {
        //#if MC>=10800
        return buffer.readString(max);
        //#else
        //$$ try {
        //$$     return buffer.readStringFromBuffer(max);
        //$$ } catch (IOException e) {
        //$$     throw new DecoderException(e);
        //$$ }
        //#endif
    }

    //#if FABRIC<=0
    //$$ public static RenderGameOverlayEvent.ElementType getType(RenderGameOverlayEvent event) {
        //#if MC>=10904
        //$$ return event.getType();
        //#else
        //$$ return event.type;
        //#endif
    //$$ }
    //#endif

    //#if MC>=10800
    public static Entity getRenderViewEntity(MinecraftClient mc) {
        return mc.getCameraEntity();
    }
    //#else
    //$$ public static EntityLivingBase getRenderViewEntity(Minecraft mc) {
    //$$     return mc.renderViewEntity;
    //$$ }
    //#endif

    //#if MC>=10800
    public static void setRenderViewEntity(MinecraftClient mc, Entity entity) {
        mc.setCameraEntity(entity);
    }
    //#else
    //$$ public static void setRenderViewEntity(Minecraft mc, EntityLivingBase entity) {
    //$$     mc.renderViewEntity = entity;
    //$$ }
    //#endif

    public static Entity getRiddenEntity(Entity ridden) {
        //#if MC>=10904
        return ridden.getVehicle();
        //#else
        //$$ return ridden.ridingEntity;
        //#endif
    }

    public static Iterable<Entity> loadedEntityList(ClientWorld world) {
        //#if MC>=11400
        return world.getEntities();
        //#else
        //$$ return world.loadedEntityList;
        //#endif
    }

    @SuppressWarnings("unchecked")
    public static Collection<Entity>[] getEntityLists(WorldChunk chunk) {
        //#if MC>=10800
        return chunk.getEntitySectionArray();
        //#else
        //$$ return chunk.entityLists;
        //#endif
    }

    @SuppressWarnings("unchecked")
    //#if MC>=11500
    public static List<Cuboid> cubeList(ModelPart modelRenderer) {
        return new ArrayList<>(); // FIXME 1.15
    }
    //#else
    //$$ public static List<Box> cubeList(ModelPart modelRenderer) {
    //$$     return modelRenderer.boxes;
    //$$ }
    //#endif

    @SuppressWarnings("unchecked")
    public static List<PlayerEntity> playerEntities(World world) {
        //#if MC>=11400
        return (List) world.getPlayers();
        //#else
        //$$ return world.playerEntities;
        //#endif
    }

    public static boolean isOnMainThread() {
        //#if MC>=11400
        return getMinecraft().isOnThread();
        //#else
        //$$ return getMinecraft().isCallingFromMinecraftThread();
        //#endif
    }

    public static void scheduleOnMainThread(Runnable runnable) {
        //#if MC>=11400
        getMinecraft().send(runnable);
        //#else
        //$$ getMinecraft().addScheduledTask(runnable);
        //#endif
    }

    //#if MC>=11400
    public static Window getWindow(MinecraftClient mc) {
        //#if MC>=11500
        return mc.getWindow();
        //#else
        //$$ return mc.window;
        //#endif
    }
    //#endif

    //#if MC>=11400
    public static Window newScaledResolution(MinecraftClient mc) {
        return getWindow(mc);
    }
    //#else
    //$$ public static ScaledResolution newScaledResolution(Minecraft mc) {
    //#if MC>=10809
    //$$ return new ScaledResolution(mc);
    //#else
    //$$ return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
    //#endif
    //$$ }
    //#endif

    public static
    //#if MC>=11400
    CompletableFuture<?>
    //#else
    //$$ ListenableFuture<?>
    //#endif
    setServerResourcePack(File file) {
        //#if MC>=11400
        return getMinecraft().getResourcePackDownloader().loadServerPack(
                file
                //#if MC>=11600
                //$$ , net.minecraft.class_5352.field_25350
                //#endif
        );
        //#else
        //$$ ResourcePackRepository repo = getMinecraft().getResourcePackRepository();
        //#if MC>=10800
        //$$ return repo.setServerResourcePack(file);
        //#else
        //$$ ResourcePackRepositoryAccessor acc = (ResourcePackRepositoryAccessor) repo;
        //$$ acc.setActive(false);
        //$$ acc.setPack(new FileResourcePack(file));
        //$$ Minecraft.getMinecraft().scheduleResourcesRefresh();
        //$$ return Futures.immediateFuture(null);
        //#endif
        //#endif
    }

    public static <T> void addCallback(
            //#if MC>=11400
            CompletableFuture<T> future,
            //#else
            //$$ ListenableFuture<T> future,
            //#endif
            Consumer<T> success,
            Consumer<Throwable> failure
    ) {
        //#if MC>=11400
        future.thenAccept(success).exceptionally(throwable -> {
            failure.accept(throwable);
            return null;
        });
        //#else
        //$$ Futures.addCallback(future, new FutureCallback<T>() {
        //$$     @Override
        //$$     public void onSuccess(T result) {
        //$$         success.accept(result);
        //$$     }
        //$$
        //$$     @Override
        //$$     public void onFailure(Throwable throwable) {
        //$$         failure.accept(throwable);
        //$$     }
        //$$ });
        //#endif
    }

    //#if MC>=10800
    public static BufferBuilder Tessellator_getBufferBuilder() {
        return Tessellator.getInstance().getBuffer();
    }
    //#else
    //$$ public static Tessellator Tessellator_getBufferBuilder() {
    //$$     return Tessellator.instance;
    //$$ }
    //#endif

    public static void BufferBuilder_beginPosCol(int mode) {
        Tessellator_getBufferBuilder().begin(
                mode
                //#if MC>=10809
                , VertexFormats.POSITION_COLOR
                //#endif
        );
    }

    public static void BufferBuilder_addPosCol(double x, double y, double z, int r, int g, int b, int a) {
        //#if MC>=10809
        Tessellator_getBufferBuilder().vertex(x, y, z).color(r, g, b, a).next();
        //#else
        //$$ Tessellator_getBufferBuilder().setColorRGBA(r, g, b, a);
        //$$ Tessellator_getBufferBuilder().addVertex(x, y, z);
        //#endif
    }

    public static void BufferBuilder_beginPosTex(int mode) {
        Tessellator_getBufferBuilder().begin(
                mode
                //#if MC>=10809
                , VertexFormats.POSITION_TEXTURE
                //#endif
        );
    }

    public static void BufferBuilder_addPosTex(double x, double y, double z, float u, float v) {
        //#if MC>=10809
        Tessellator_getBufferBuilder().vertex(x, y, z).texture(u, v).next();
        //#else
        //$$ Tessellator_getBufferBuilder().addVertexWithUV(x, y, z, u, v);
        //#endif
    }

    public static void BufferBuilder_beginPosTexCol(int mode) {
        Tessellator_getBufferBuilder().begin(
                mode
                //#if MC>=10809
                , VertexFormats.POSITION_TEXTURE_COLOR
                //#endif
        );
    }

    public static void BufferBuilder_addPosTexCol(double x, double y, double z, float u, float v, int r, int g, int b, int a) {
        //#if MC>=10809
        Tessellator_getBufferBuilder().vertex(x, y, z).texture(u, v).color(r, g, b, a).next();
        //#else
        //$$ Tessellator_getBufferBuilder().setColorRGBA(r, g, b, a);
        //$$ Tessellator_getBufferBuilder().addVertexWithUV(x, y, z, u, v);
        //#endif
    }

    //#if MC>=10800
    @SuppressWarnings("unchecked")
    public static List<VertexFormatElement> getElements(VertexFormat vertexFormat) {
        return vertexFormat.getElements();
    }
    //#endif

    public static Tessellator Tessellator_getInstance() {
        //#if MC>=10800
        return Tessellator.getInstance();
        //#else
        //$$ return Tessellator.instance;
        //#endif
    }

    public static EntityRenderDispatcher getRenderManager() {
        //#if MC>=10800
        return getMinecraft().getEntityRenderManager();
        //#else
        //$$ return RenderManager.instance;
        //#endif
    }

    public static MinecraftClient getMinecraft() {
        return MinecraftClient.getInstance();
    }

    public static float getRenderPartialTicks() {
        return ((MinecraftAccessor) getMinecraft()).getTimer().tickDelta;
    }

    public static void addButton(
            Screen screen,
            //#if MC>=11400
            ButtonWidget button
            //#else
            //$$ GuiButton button
            //#endif
    ) {
        GuiScreenAccessor acc = (GuiScreenAccessor) screen;
        //#if MC>=11400
        if (screen instanceof TitleScreen && isModLoaded("modmenu")) {
            // Since we bypass the usual addButton, we need to manually move our buttons down
            // https://github.com/Prospector/ModMenu/blob/eea70ec37581a7229142cf21df795fdacc6a7b4c/src/main/java/io/github/prospector/modmenu/mixin/TitleScreenMixin.java#L42
            button.y += 12;
        }
        //#endif
        acc.getButtons().add(button);
        //#if MC>=11400
        acc.getChildren().add(button);
        //#endif
    }

    //#if MC>=11400
    public static void processKeyBinds() {
        ((MinecraftMethodAccessor) getMinecraft()).replayModProcessKeyBinds();
    }
    //#endif

    public interface MinecraftMethodAccessor {
        //#if MC>=11400
        void replayModProcessKeyBinds();
        //#else
        //#if MC>=10904
        //$$ void replayModRunTickMouse();
        //$$ void replayModRunTickKeyboard();
        //#else
        //$$ void replayModSetEarlyReturnFromRunTick(boolean earlyReturn);
        //#endif
        //#endif
        //#if MC>=11400
        void replayModExecuteTaskQueue();
        //#endif
    }

    //#if MC>=10800 && MC<11500
    //$$ public interface ChunkRenderWorkerAccessor {
    //$$     void doRunTask(ChunkRenderTask task) throws InterruptedException;
    //$$ }
    //#endif

    public static long milliTime() {
        //#if MC>=11400
        return Util.getMeasuringTimeMs();
        //#else
        //$$ return Minecraft.getSystemTime();
        //#endif
    }

    public static void bindTexture(Identifier texture) {
        //#if MC>=11500
        getMinecraft().getTextureManager().bindTexture(texture);
        //#else
        //#if MC>=11400
        //$$ getMinecraft().getTextureManager().bindTexture(texture);
        //#else
        //$$ getMinecraft().renderEngine.bindTexture(texture);
        //#endif
        //#endif
    }

    public static float cos(float val) {
        return MathHelper.cos(val);
    }

    public static float sin(float val) {
        return MathHelper.sin(val);
    }

    //#if MC>=10904
    // TODO: this can be inlined once https://github.com/SpongePowered/Mixin/issues/305 is fixed
    public static Vec3d getPosition(Particle particle, float partialTicks) {
        ParticleAccessor acc = (ParticleAccessor) particle;
        double x = acc.getPrevPosX() + (acc.getPosX() - acc.getPrevPosX()) * partialTicks;
        double y = acc.getPrevPosY() + (acc.getPosY() - acc.getPrevPosY()) * partialTicks;
        double z = acc.getPrevPosZ() + (acc.getPosZ() - acc.getPrevPosZ()) * partialTicks;
        return new Vec3d(x, y, z);
    }
    //#endif

    public static void openFile(File file) {
        //#if MC>=11400
        Util.getOperatingSystem().open(file);
        //#else
        //$$ String path = file.getAbsolutePath();
        //$$
        //$$ // First try OS specific methods
        //$$ try {
        //$$     switch (Util.getOSType()) {
        //$$         case WINDOWS:
        //$$             Runtime.getRuntime().exec(String.format("cmd.exe /C start \"Open file\" \"%s\"", path));
        //$$             return;
        //$$         case OSX:
        //$$             Runtime.getRuntime().exec(new String[]{"/usr/bin/open", path});
        //$$             return;
        //$$     }
        //$$ } catch (IOException e) {
        //$$     LogManager.getLogger().error("Cannot open file", e);
        //$$ }
        //$$
        //$$ // Otherwise try to java way
        //$$ try {
        //$$     Desktop.getDesktop().browse(file.toURI());
        //$$ } catch (Throwable throwable) {
        //$$     // And if all fails, lwjgl
        //$$     Sys.openURL("file://" + path);
        //$$ }
        //#endif
    }

    public static void openURL(URI url) {
        //#if MC>=11400
        //#if MC>=11400
        Util.getOperatingSystem().open(url);
        //#else
        //$$ Util.getOSType().openURI(url);
        //#endif
        //#else
        //$$ try {
        //$$     Desktop.getDesktop().browse(url);
        //$$ } catch (Throwable e) {
        //$$     LOGGER.error("Failed to open URL: ", e);
        //$$ }
        //#endif
    }

    //#if MC>=11400
    private static Boolean hasOptifine;
    public static boolean hasOptifine() {
        if (hasOptifine == null) {
            try {
                Class.forName("Config");
                hasOptifine = true;
            } catch (ClassNotFoundException e) {
                hasOptifine = false;
            }
        }
        return hasOptifine;
    }
    //#else
    //$$ public static boolean hasOptifine() {
    //$$     return FMLClientHandler.instance().hasOptifine();
    //$$ }
    //#endif

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

    public static abstract class Keyboard {
        //#if MC>=11400
        public static final int KEY_LCONTROL = GLFW.GLFW_KEY_LEFT_CONTROL;
        public static final int KEY_LSHIFT = GLFW.GLFW_KEY_LEFT_SHIFT;
        public static final int KEY_ESCAPE = GLFW.GLFW_KEY_ESCAPE;
        public static final int KEY_HOME = GLFW.GLFW_KEY_HOME;
        public static final int KEY_END = GLFW.GLFW_KEY_END;
        public static final int KEY_UP = GLFW.GLFW_KEY_UP;
        public static final int KEY_DOWN = GLFW.GLFW_KEY_DOWN;
        public static final int KEY_LEFT = GLFW.GLFW_KEY_LEFT;
        public static final int KEY_RIGHT = GLFW.GLFW_KEY_RIGHT;
        public static final int KEY_BACK = GLFW.GLFW_KEY_BACKSPACE;
        public static final int KEY_DELETE = GLFW.GLFW_KEY_DELETE;
        public static final int KEY_RETURN = GLFW.GLFW_KEY_ENTER;
        public static final int KEY_TAB = GLFW.GLFW_KEY_TAB;
        public static final int KEY_F1 = GLFW.GLFW_KEY_F1;
        public static final int KEY_A = GLFW.GLFW_KEY_A;
        public static final int KEY_B = GLFW.GLFW_KEY_B;
        public static final int KEY_C = GLFW.GLFW_KEY_C;
        public static final int KEY_D = GLFW.GLFW_KEY_D;
        public static final int KEY_E = GLFW.GLFW_KEY_E;
        public static final int KEY_F = GLFW.GLFW_KEY_F;
        public static final int KEY_G = GLFW.GLFW_KEY_G;
        public static final int KEY_H = GLFW.GLFW_KEY_H;
        public static final int KEY_I = GLFW.GLFW_KEY_I;
        public static final int KEY_J = GLFW.GLFW_KEY_J;
        public static final int KEY_K = GLFW.GLFW_KEY_K;
        public static final int KEY_L = GLFW.GLFW_KEY_L;
        public static final int KEY_M = GLFW.GLFW_KEY_M;
        public static final int KEY_N = GLFW.GLFW_KEY_N;
        public static final int KEY_O = GLFW.GLFW_KEY_O;
        public static final int KEY_P = GLFW.GLFW_KEY_P;
        public static final int KEY_Q = GLFW.GLFW_KEY_Q;
        public static final int KEY_R = GLFW.GLFW_KEY_R;
        public static final int KEY_S = GLFW.GLFW_KEY_S;
        public static final int KEY_T = GLFW.GLFW_KEY_T;
        public static final int KEY_U = GLFW.GLFW_KEY_U;
        public static final int KEY_V = GLFW.GLFW_KEY_V;
        public static final int KEY_W = GLFW.GLFW_KEY_W;
        public static final int KEY_X = GLFW.GLFW_KEY_X;
        public static final int KEY_Y = GLFW.GLFW_KEY_Y;
        public static final int KEY_Z = GLFW.GLFW_KEY_Z;
        //#else
        //$$ public static final int KEY_LCONTROL = org.lwjgl.input.Keyboard.KEY_LCONTROL;
        //$$ public static final int KEY_LSHIFT = org.lwjgl.input.Keyboard.KEY_LSHIFT;
        //$$ public static final int KEY_ESCAPE = org.lwjgl.input.Keyboard.KEY_ESCAPE;
        //$$ public static final int KEY_HOME = org.lwjgl.input.Keyboard.KEY_HOME;
        //$$ public static final int KEY_END = org.lwjgl.input.Keyboard.KEY_END;
        //$$ public static final int KEY_UP = org.lwjgl.input.Keyboard.KEY_UP;
        //$$ public static final int KEY_DOWN = org.lwjgl.input.Keyboard.KEY_DOWN;
        //$$ public static final int KEY_LEFT = org.lwjgl.input.Keyboard.KEY_LEFT;
        //$$ public static final int KEY_RIGHT = org.lwjgl.input.Keyboard.KEY_RIGHT;
        //$$ public static final int KEY_BACK = org.lwjgl.input.Keyboard.KEY_BACK;
        //$$ public static final int KEY_DELETE = org.lwjgl.input.Keyboard.KEY_DELETE;
        //$$ public static final int KEY_RETURN = org.lwjgl.input.Keyboard.KEY_RETURN;
        //$$ public static final int KEY_TAB = org.lwjgl.input.Keyboard.KEY_TAB;
        //$$ public static final int KEY_F1 = org.lwjgl.input.Keyboard.KEY_F1;
        //$$ public static final int KEY_A = org.lwjgl.input.Keyboard.KEY_A;
        //$$ public static final int KEY_B = org.lwjgl.input.Keyboard.KEY_B;
        //$$ public static final int KEY_C = org.lwjgl.input.Keyboard.KEY_C;
        //$$ public static final int KEY_D = org.lwjgl.input.Keyboard.KEY_D;
        //$$ public static final int KEY_E = org.lwjgl.input.Keyboard.KEY_E;
        //$$ public static final int KEY_F = org.lwjgl.input.Keyboard.KEY_F;
        //$$ public static final int KEY_G = org.lwjgl.input.Keyboard.KEY_G;
        //$$ public static final int KEY_H = org.lwjgl.input.Keyboard.KEY_H;
        //$$ public static final int KEY_I = org.lwjgl.input.Keyboard.KEY_I;
        //$$ public static final int KEY_J = org.lwjgl.input.Keyboard.KEY_J;
        //$$ public static final int KEY_K = org.lwjgl.input.Keyboard.KEY_K;
        //$$ public static final int KEY_L = org.lwjgl.input.Keyboard.KEY_L;
        //$$ public static final int KEY_M = org.lwjgl.input.Keyboard.KEY_M;
        //$$ public static final int KEY_N = org.lwjgl.input.Keyboard.KEY_N;
        //$$ public static final int KEY_O = org.lwjgl.input.Keyboard.KEY_O;
        //$$ public static final int KEY_P = org.lwjgl.input.Keyboard.KEY_P;
        //$$ public static final int KEY_Q = org.lwjgl.input.Keyboard.KEY_Q;
        //$$ public static final int KEY_R = org.lwjgl.input.Keyboard.KEY_R;
        //$$ public static final int KEY_S = org.lwjgl.input.Keyboard.KEY_S;
        //$$ public static final int KEY_T = org.lwjgl.input.Keyboard.KEY_T;
        //$$ public static final int KEY_U = org.lwjgl.input.Keyboard.KEY_U;
        //$$ public static final int KEY_V = org.lwjgl.input.Keyboard.KEY_V;
        //$$ public static final int KEY_W = org.lwjgl.input.Keyboard.KEY_W;
        //$$ public static final int KEY_X = org.lwjgl.input.Keyboard.KEY_X;
        //$$ public static final int KEY_Y = org.lwjgl.input.Keyboard.KEY_Y;
        //$$ public static final int KEY_Z = org.lwjgl.input.Keyboard.KEY_Z;
        //#endif

        public static boolean isKeyDown(int keyCode) {
            //#if MC>=11500
            return InputUtil.isKeyPressed(getMinecraft().getWindow().getHandle(), keyCode);
            //#else
            //#if MC>=11400
            //$$ return InputUtil.isKeyPressed(getMinecraft().window.getHandle(), keyCode);
            //#else
            //#if MC>=11400
            //$$ return InputMappings.isKeyDown(keyCode);
            //#else
            //$$ return org.lwjgl.input.Keyboard.isKeyDown(keyCode);
            //#endif
            //#endif
            //#endif
        }

        //#if MC<11400
        //$$ public static int getEventKey() {
        //$$     return org.lwjgl.input.Keyboard.getEventKey();
        //$$ }
        //$$
        //$$ public static boolean getEventKeyState() {
        //$$     return org.lwjgl.input.Keyboard.getEventKeyState();
        //$$ }
        //#endif
    }
}
