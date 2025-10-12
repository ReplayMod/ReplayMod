package com.replaymod.core.versions;

import com.mojang.blaze3d.platform.GlStateManager;
import com.replaymod.core.mixin.GuiScreenAccessor;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector2f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.NetworkState;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

//#if MC>=12105
//$$ import net.minecraft.client.render.VertexConsumer;
//#endif

//#if MC>=11700
//$$ import net.minecraft.util.math.Matrix4f;
//#endif

//#if MC>=11604
//#else
//$$ import net.minecraft.entity.Entity;
//#endif

//#if MC>=11600
import net.minecraft.resource.ResourcePackSource;
//#endif

//#if MC>=11400
import com.replaymod.render.mixin.MainWindowAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.util.Window;

import java.util.concurrent.CompletableFuture;

//#if MC>=11600
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
//#else
//$$ import net.minecraft.client.resource.language.I18n;
//#endif
//#else
//$$ import com.google.common.util.concurrent.FutureCallback;
//$$ import com.google.common.util.concurrent.Futures;
//$$ import com.google.common.util.concurrent.ListenableFuture;
//$$ import net.minecraft.client.gui.GuiButton;
//$$ import net.minecraft.realms.RealmsSharedConstants;
//#endif

//#if MC>=11400
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
//#else
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
//#endif

//#if MC>=10800
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
//#if MC<11500
//$$ import net.minecraft.client.render.chunk.ChunkRenderTask;
//#endif
//#else
//$$ import com.replaymod.core.mixin.ResourcePackRepositoryAccessor;
//$$ import io.netty.handler.codec.DecoderException;
//$$ import net.minecraft.client.renderer.entity.RenderManager;
//$$ import net.minecraft.client.resources.FileResourcePack;
//$$ import net.minecraft.network.PacketBuffer;
//$$
//$$ import static org.lwjgl.opengl.GL11.*;
//#endif

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.Optional;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    public static int getProtocolVersion() {
        //#if MC>=12106
        //$$ return SharedConstants.getProtocolVersion();
        //#elseif MC>=11400
        return SharedConstants.getGameVersion().getProtocolVersion();
        //#else
        //$$ return RealmsSharedConstants.NETWORK_PROTOCOL_VERSION;
        //#endif
    }

    public static NetworkState asMc(State state) {
        switch (state) {
            case HANDSHAKE: return NetworkState.HANDSHAKING;
            case STATUS: return NetworkState.STATUS;
            case LOGIN: return NetworkState.LOGIN;
            //#if MC>=12002
            //$$ case CONFIGURATION: return NetworkState.CONFIGURATION;
            //#endif
            case PLAY: return NetworkState.PLAY;
        }
        throw new IllegalArgumentException("Unexpected value: " + state);
    }

    public static State fromMc(NetworkState mcState) {
        switch (mcState) {
            case HANDSHAKING: return State.HANDSHAKE;
            case STATUS: return State.STATUS;
            case LOGIN: return State.LOGIN;
            //#if MC>=12002
            //$$ case CONFIGURATION: return State.CONFIGURATION;
            //#endif
            case PLAY: return State.PLAY;
        }
        throw new IllegalArgumentException("Unexpected value: " + mcState);
    }

    public static PacketTypeRegistry getPacketTypeRegistry(NetworkState state) {
        return getPacketTypeRegistry(fromMc(state));
    }

    public static PacketTypeRegistry getPacketTypeRegistry(State state) {
        return PacketTypeRegistry.get(ProtocolVersion.getProtocol(getProtocolVersion()), state);
    }

    public static PacketTypeRegistry getPacketTypeRegistry(boolean loginPhase) {
        return PacketTypeRegistry.get(
                ProtocolVersion.getProtocol(getProtocolVersion()),
                loginPhase ? State.LOGIN : State.PLAY
        );
    }

    public static void resizeMainWindow(MinecraftClient mc, int width, int height) {
        //#if MC>=11400
        Window window = mc.getWindow();
        MainWindowAccessor mainWindow = (MainWindowAccessor) (Object) window;
        //noinspection ConstantConditions
        mainWindow.invokeOnFramebufferSizeChanged(window.getHandle(), width, height);
        //#else
        //$$ if (width != mc.displayWidth || height != mc.displayHeight) {
        //$$     mc.resize(width, height);
        //$$ }
        //#endif
    }

    //#if MC<10800
    //$$ public static String tryReadString(PacketBuffer buffer, int max) {
    //$$     try {
    //$$         return buffer.readStringFromBuffer(max);
    //$$     } catch (IOException e) {
    //$$         throw new DecoderException(e);
    //$$     }
    //$$ }
    //#endif

    //#if MC<12003
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
                , ResourcePackSource.PACK_SOURCE_SERVER
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
    //#endif

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
    @SuppressWarnings("unchecked")
    public static List<VertexFormatElement> getElements(VertexFormat vertexFormat) {
        return vertexFormat.getElements();
    }
    //#endif

    //#if MC<10800
    //$$ public static RenderManager getRenderManager(@SuppressWarnings("unused") Minecraft mc) {
    //$$     return RenderManager.instance;
    //$$ }
    //#endif

    public static MinecraftClient getMinecraft() {
        return MinecraftClient.getInstance();
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
        acc.invokeAddButton(button);
        //#else
        //$$ acc.getButtons().add(button);
        //#endif
    }

    //#if MC>=11400
    public static Optional<AbstractButtonWidget> findButton(Iterable<? extends Element> buttonList, @SuppressWarnings("unused") String text, @SuppressWarnings("unused") int id) {
        //#if MC>=11600
        final Text message = new TranslatableText(text);
        //#else
        //$$ final String message = I18n.translate(text);
        //#endif
        for (Element e : buttonList) {
            if (e instanceof ParentElement) {
                Optional<AbstractButtonWidget> button = findButton(((ParentElement) e).children(), text, id);
                if (button.isPresent()) {
                    return button;
                }
            }
            if (!(e instanceof AbstractButtonWidget)) {
                continue;
            }
            AbstractButtonWidget b = (AbstractButtonWidget) e;
            if (message.equals(b.getMessage())) {
                return Optional.of(b);
            }
            //#if MC>=11600
            // Fuzzy match (copy does not include children)
            if (b.getMessage() != null && b.getMessage().copy().equals(message)) {
                return Optional.of(b);
            }
            //#endif
        }
        return Optional.empty();
    }
    //#else
    //$$ public static Optional<GuiButton> findButton(Iterable<GuiButton> buttonList, @SuppressWarnings("unused") String text, int id) {
    //$$     for (GuiButton b : buttonList) {
    //$$         if (b.id == id) {
    //$$             return Optional.of(b);
    //$$         }
    //$$     }
    //$$     return Optional.empty();
    //$$ }
    //#endif

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

    //#if MC<=11601
    //$$ public static Vec3d getTrackedPosition(Entity entity) {
    //$$     return new Vec3d(entity.trackedX / 4096.0, entity.trackedY / 4096.0, entity.trackedZ / 4096.0);
    //$$ }
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
        Util.getOperatingSystem().open(url);
        //#else
        //$$ try {
        //$$     Desktop.getDesktop().browse(url);
        //$$ } catch (Throwable e) {
        //$$     LogManager.getLogger().error("Failed to open URL: ", e);
        //$$ }
        //#endif
    }

    public static void pushMatrix() {
        //#if MC>=12006
        //$$ RenderSystem.getModelViewStack().pushMatrix();
        //#elseif MC>=11700
        //$$ RenderSystem.getModelViewStack().push();
        //#else
        GlStateManager.pushMatrix();
        //#endif
    }

    public static void popMatrix() {
        //#if MC>=11700
        //#if MC>=12006
        //$$ RenderSystem.getModelViewStack().popMatrix();
        //#else
        //$$ RenderSystem.getModelViewStack().pop();
        //#endif
        //#if MC<12102
        //$$ RenderSystem.applyModelViewMatrix();
        //#endif
        //#else
        GlStateManager.popMatrix();
        //#endif
    }

    //#if MC>=11700
    //$$ public static net.minecraft.util.math.Quaternion quaternion(float angle, net.minecraft.util.math.Vec3f axis) {
        //#if MC>=11903
        //$$ return new org.joml.Quaternionf().fromAxisAngleDeg(axis.x, axis.y, axis.z, angle);
        //#else
        //$$ return new net.minecraft.util.math.Quaternion(axis, angle, true);
        //#endif
    //$$ }
    //$$
    //$$ public static Matrix4f ortho(float left, float right, float top, float bottom, float zNear, float zFar) {
        //#if MC>=11903
        //$$ return new Matrix4f().ortho(left, right, bottom, top, zNear, zFar);
        //#else
        //$$ return Matrix4f.projectionMatrix(left, right, top, bottom, zNear, zFar);
        //#endif
    //$$ }
    //#endif

    //#if MC>=12105
    //$$ public static void emitLine(MatrixStack matrixStack, VertexConsumer buffer, Vector2f p1, Vector2f p2, int color) {
    //#else
    public static void emitLine(MatrixStack matrixStack, BufferBuilder buffer, Vector2f p1, Vector2f p2, int color) {
    //#endif
        emitLine(matrixStack, buffer, new Vector3f(p1.x, p1.y, 0), new Vector3f(p2.x, p2.y, 0), color);
    }

    //#if MC>=12105
    //$$ public static void emitLine(MatrixStack matrixStack, VertexConsumer buffer, Vector3f p1, Vector3f p2, int color) {
    //#else
    public static void emitLine(MatrixStack matrixStack, BufferBuilder buffer, Vector3f p1, Vector3f p2, int color) {
    //#endif
        int r = color >> 24 & 0xff;
        int g = color >> 16 & 0xff;
        int b = color >> 8 & 0xff;
        int a = color & 0xff;
        //#if MC>=11700
        //$$ Vector3f n = Vector3f.sub(p2, p1, null);
        //#endif
        //#if MC>=11600
        buffer.vertex(matrixStack.peek().getModel(), p1.x, p1.y, p1.z)
        //#else
        //$$ buffer.vertex(p1.x, p1.y, p1.z)
        //#endif
                .color(r, g, b, a)
                //#if MC>=11700
                //$$ .normal(n.x, n.y, n.z)
                //#endif
                ;
        buffer.next();
        //#if MC>=11600
        buffer.vertex(matrixStack.peek().getModel(), p2.x, p2.y, p2.z)
        //#else
        //$$ buffer.vertex(p2.x, p2.y, p2.z)
        //#endif
                .color(r, g, b, a)
                //#if MC>=11700
                //$$ .normal(n.x, n.y, n.z)
                //#endif
                ;
        buffer.next();
    }

    //#if MC<12105
    public static void bindTexture(Identifier id) {
        de.johni0702.minecraft.gui.versions.MCVer.bindTexture(id);
    }
    //#endif

    //#if MC<10900
    //$$ public static class SoundEvent {}
    //#endif

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
        //$$
        //$$ public static String getKeyName(int code) {
        //$$     return org.lwjgl.input.Keyboard.getKeyName(code);
        //$$ }
        //#endif
    }
}
