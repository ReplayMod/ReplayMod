package com.replaymod.core.versions;

import com.replaymod.core.mixin.GuiScreenAccessor;
import com.replaymod.core.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.model.ModelBox;
import net.minecraft.client.renderer.entity.model.ModelRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

//#if MC>=11400
//$$ import com.replaymod.core.mixin.AbstractButtonWidgetAccessor;
//$$ import net.minecraft.client.gui.widget.AbstractButtonWidget;
//$$ import java.util.concurrent.CompletableFuture;
//#else
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
//#endif

//#if MC>=11300
import net.minecraft.client.MainWindow;
import net.minecraft.client.util.InputMappings;
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
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
//#else
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif

//#if MC>=10800
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.world.WorldType;
//#else
//$$ import com.google.common.util.concurrent.Futures;
//$$ import io.netty.handler.codec.DecoderException;
//$$ import net.minecraft.client.resources.FileResourcePack;
//$$
//$$ import static org.lwjgl.opengl.GL11.*;
//#endif

//#if MC>=11400
//$$ import net.fabricmc.loader.api.FabricLoader;
//#else
//#if MC>=11300
import net.minecraftforge.fml.ModList;
//#else
//$$ import net.minecraftforge.fml.common.Loader;
//#endif
//#endif

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    //#if MC<11400
    public static IEventBus FORGE_BUS = MinecraftForge.EVENT_BUS;
    //#if MC>=10809
    public static IEventBus FML_BUS = FORGE_BUS;
    //#else
    //$$ public static EventBus FML_BUS = FMLCommonHandler.instance().bus();
    //#endif
    //#endif

    public static boolean isModLoaded(String id) {
        //#if MC>=11400
        //$$ return FabricLoader.getInstance().isModLoaded(id.toLowerCase());
        //#else
        //#if MC>=11300
        return ModList.get().isLoaded(id.toLowerCase());
        //#else
        //$$ return Loader.isModLoaded(id);
        //#endif
        //#endif
    }

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

    //#if MC>=11400
    //$$ public static void width(AbstractButtonWidget button, int value) {
    //$$     button.setWidth(value);
    //$$ }
    //$$
    //$$ public static int width(AbstractButtonWidget button) {
    //$$     return button.getWidth();
    //$$ }
    //$$
    //$$ public static int height(AbstractButtonWidget button) {
    //$$     return ((AbstractButtonWidgetAccessor) button).getHeight();
    //$$ }
    //#else
    public static void width(GuiButton button, int value) {
        button.width = value;
    }

    public static int width(GuiButton button) {
        return button.width;
    }

    public static int height(GuiButton button) {
        return button.height;
    }
    //#endif

    //#if MC<11400
    public static void addButton(GuiScreenEvent.InitGuiEvent event, GuiButton button) {
        //#if MC>=11300
        event.addButton(button);
        //#else
        //$$ getButtonList(event).add(button);
        //#endif
    }

    public static void removeButton(GuiScreenEvent.InitGuiEvent event, GuiButton button) {
        //#if MC>=11300
        event.removeButton(button);
        //#else
        //$$ getButtonList(event).remove(button);
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

    public static EntityLivingBase getEntity(RenderLivingEvent event) {
        //#if MC>=10904
        return event.getEntity();
        //#else
        //$$ return event.entity;
        //#endif
    }
    //#endif

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

    //#if MC<11400
    public static RenderGameOverlayEvent.ElementType getType(RenderGameOverlayEvent event) {
        //#if MC>=10904
        return event.getType();
        //#else
        //$$ return event.type;
        //#endif
    }
    //#endif

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

    public static ResourceLocation LOCATION_BLOCKS_TEXTURE
            //#if MC>=10904
            = TextureMap.LOCATION_BLOCKS_TEXTURE;
            //#else
            //$$ = TextureMap.locationBlocksTexture;
            //#endif

    public static Entity getRiddenEntity(Entity ridden) {
        //#if MC>=10904
        return ridden.getRidingEntity();
        //#else
        //$$ return ridden.ridingEntity;
        //#endif
    }

    public static Iterable<Entity> loadedEntityList(WorldClient world) {
        //#if MC>=11400
        //$$ return world.getEntities();
        //#else
        return world.loadedEntityList;
        //#endif
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
    public static List<ModelBox> cubeList(ModelRenderer modelRenderer) {
        return modelRenderer.cubeList;
    }

    @SuppressWarnings("unchecked")
    public static List<EntityPlayer> playerEntities(World world) {
        //#if MC>=11400
        //$$ return (List) world.getPlayers();
        //#else
        return world.playerEntities;
        //#endif
    }

    public static boolean isOnMainThread() {
        //#if MC>=11400
        //$$ return getMinecraft().isOnThread();
        //#else
        return getMinecraft().isCallingFromMinecraftThread();
        //#endif
    }

    //#if MC>=11300
    public static MainWindow newScaledResolution(Minecraft mc) {
        return mc.mainWindow;
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
    //$$ CompletableFuture<?>
    //#else
    ListenableFuture<?>
    //#endif
    setServerResourcePack(File file) {
        //#if MC>=11300
        return getMinecraft().getPackFinder().func_195741_a(file);
        //#else
        //$$ ResourcePackRepository repo = getMinecraft().getResourcePackRepository();
        //#if MC>=10809
        //#if MC>=11200
        //$$ return repo.setServerResourcePack(file);
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
        //#endif
    }

    public static <T> void addCallback(
            //#if MC>=11400
            //$$ CompletableFuture<T> future,
            //#else
            ListenableFuture<T> future,
            //#endif
            Consumer<T> success,
            Consumer<Throwable> failure
    ) {
        //#if MC>=11400
        //$$ future.thenAccept(success).exceptionally(throwable -> {
        //$$     failure.accept(throwable);
        //$$     return null;
        //$$ });
        //#else
        Futures.addCallback(future, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                success.accept(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                failure.accept(throwable);
            }
        });
        //#endif
    }

    public static void BufferBuilder_setTranslation(double x, double y, double z) {
        //#if MC>=10800
        Tessellator.getInstance().getBuffer().setTranslation(x, y, z);
        //#else
        //$$ Tessellator.instance.setTranslation(x, y, z);
        //#endif
    }

    public static void BufferBuilder_beginPosCol(int mode) {
        //#if MC>=10800
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        //#if MC>=10809
        bufferBuilder.begin(mode, DefaultVertexFormats.POSITION_COLOR);
        //#else
        //$$ bufferBuilder.startDrawing(mode);
        //#endif
        //#else
        //$$ Tessellator.instance.startDrawing(mode);
        //#endif
    }

    public static void BufferBuilder_addPosCol(double x, double y, double z, int r, int g, int b, int a) {
        //#if MC>=10800
        BufferBuilder worldRenderer = Tessellator.getInstance().getBuffer();
        //#else
        //$$ Tessellator worldRenderer = Tessellator.instance;
        //#endif
        //#if MC>=10809
        worldRenderer.pos(x, y, z).color(r, g, b, a).endVertex();
        //#else
        //$$ worldRenderer.setColorRGBA(r, g, b, a);
        //$$ worldRenderer.addVertex(x, y, z);
        //#endif
    }

    public static void BufferBuilder_beginPosTex(int mode) {
        //#if MC>=10800
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        //#if MC>=10809
        bufferBuilder.begin(mode, DefaultVertexFormats.POSITION_TEX);
        //#else
        //$$ bufferBuilder.startDrawing(mode);
        //#endif
        //#else
        //$$ Tessellator.instance.startDrawing(mode);
        //#endif
    }

    public static void BufferBuilder_addPosTex(double x, double y, double z, double u, double v) {
        //#if MC>=10800
        BufferBuilder worldRenderer = Tessellator.getInstance().getBuffer();
        //#else
        //$$ Tessellator worldRenderer = Tessellator.instance;
        //#endif
        //#if MC>=10809
        worldRenderer.pos(x, y, z).tex(u, v).endVertex();
        //#else
        //$$ worldRenderer.addVertexWithUV(x, y, z, u, v);
        //#endif
    }

    public static void BufferBuilder_beginPosTexCol(int mode) {
        //#if MC>=10800
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        //#if MC>=10809
        bufferBuilder.begin(mode, DefaultVertexFormats.POSITION_TEX_COLOR);
        //#else
        //$$ bufferBuilder.startDrawing(mode);
        //#endif
        //#else
        //$$ Tessellator.instance.startDrawing(mode);
        //#endif
    }

    public static void BufferBuilder_addPosTexCol(double x, double y, double z, double u, double v, int r, int g, int b, int a) {
        //#if MC>=10800
        BufferBuilder worldRenderer = Tessellator.getInstance().getBuffer();
        //#else
        //$$ Tessellator worldRenderer = Tessellator.instance;
        //#endif
        //#if MC>=10809
        worldRenderer.pos(x, y, z).tex(u, v).color(r, g, b, a).endVertex();
        //#else
        //$$ worldRenderer.setColorRGBA(r, g, b, a);
        //$$ worldRenderer.addVertexWithUV(x, y, z, u, v);
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

    public static RenderManager getRenderManager() {
        //#if MC>=10800
        return getMinecraft().getRenderManager();
        //#else
        //$$ return RenderManager.instance;
        //#endif
    }

    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    public static float getRenderPartialTicks() {
        return ((MinecraftAccessor) getMinecraft()).getTimer().renderPartialTicks;
    }

    public static void addButton(GuiScreen screen, GuiButton button) {
        GuiScreenAccessor acc = (GuiScreenAccessor) screen;
        acc.getButtons().add(button);
        //#if MC>=11300
        acc.getChildren().add(button);
        //#endif
    }

    //#if MC>=11300
    public static void processKeyBinds() {
        ((MinecraftMethodAccessor) getMinecraft()).replayModProcessKeyBinds();
    }
    public interface MinecraftMethodAccessor {
        void replayModProcessKeyBinds();
        //#if MC>=11400
        //$$ void replayModExecuteTaskQueue();
        //#endif
    }
    //#endif

    public static long milliTime() {
        //#if MC>=11300
        return Util.milliTime();
        //#else
        //$$ return Minecraft.getSystemTime();
        //#endif
    }

    public static void bindTexture(ResourceLocation texture) {
        //#if MC>=11300
        getMinecraft().getTextureManager().bindTexture(texture);
        //#else
        //$$ getMinecraft().renderEngine.bindTexture(texture);
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
        //#if MC>=11300
        Util.getOSType().openFile(file);
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

    //#if MC>=11300
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

    //#if MC>=11300
    public static void color(float r, float g, float b, float a) { GlStateManager.color4f(r, g, b, a); }
    public static void enableAlpha() { GlStateManager.enableAlphaTest(); }
    public static void disableAlpha() { GlStateManager.disableAlphaTest(); }
    public static void tryBlendFuncSeparate(int l, int r, int vl, int vr) { GlStateManager.blendFuncSeparate(l, r, vl, vr); }
    public static void colorLogicOp(int op) { GlStateManager.logicOp(op); }

    public static abstract class Keyboard {
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

        public static boolean isKeyDown(int keyCode) {
            //#if MC>=11400
            //$$ return InputUtil.isKeyPressed(getMinecraft().window.getHandle(), keyCode);
            //#else
            return InputMappings.isKeyDown(keyCode);
            //#endif
        }
    }
    //#endif
}
