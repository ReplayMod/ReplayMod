package com.replaymod.core.versions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.gradle.remap.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

//#if MC>=11700
//#else
import org.lwjgl.opengl.GL11;
//#endif

//#if MC>=11600
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
//#else
//#endif

//#if MC>=11400
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.util.Window;
//#else
//$$ import net.minecraft.client.gui.GuiButton;
//#endif

//#if MC>=10904
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.crash.CrashCallable;
//#else
//$$ import java.util.concurrent.Callable;
//#endif

//#if MC>=10809
import net.minecraft.client.render.VertexFormats;
//#else
//#endif

//#if MC>=10800
import net.minecraft.client.render.BufferBuilder;
//#else
//$$ import net.minecraft.entity.EntityLivingBase;
//#endif

import java.util.Collection;
import java.util.List;

class Patterns {
    //#if MC>=10904
    @Pattern
    private static void addCrashCallable(CrashReportSection category, String name, CrashCallable<String> callable) {
        //#if MC>=11200
        category.add(name, callable);
        //#else
        //$$ category.setDetail(name, callable);
        //#endif
    }
    //#else
    //$$ @Pattern
    //$$ private static void addCrashCallable(CrashReportCategory category, String name, Callable<String> callable) {
    //$$     category.addCrashSectionCallable(name, callable);
    //$$ }
    //#endif

    @Pattern
    private static double Entity_getX(Entity entity) {
        //#if MC>=11500
        return entity.getX();
        //#else
        //$$ return entity.x;
        //#endif
    }

    @Pattern
    private static double Entity_getY(Entity entity) {
        //#if MC>=11500
        return entity.getY();
        //#else
        //$$ return entity.y;
        //#endif
    }

    @Pattern
    private static double Entity_getZ(Entity entity) {
        //#if MC>=11500
        return entity.getZ();
        //#else
        //$$ return entity.z;
        //#endif
    }

    @Pattern
    private static void Entity_setYaw(Entity entity, float value) {
        //#if MC>=11700
        //$$ entity.setYaw(value);
        //#else
        entity.yaw = value;
        //#endif
    }

    @Pattern
    private static float Entity_getYaw(Entity entity) {
        //#if MC>=11700
        //$$ return entity.getYaw();
        //#else
        return entity.yaw;
        //#endif
    }

    @Pattern
    private static void Entity_setPitch(Entity entity, float value) {
        //#if MC>=11700
        //$$ entity.setPitch(value);
        //#else
        entity.pitch = value;
        //#endif
    }

    @Pattern
    private static float Entity_getPitch(Entity entity) {
        //#if MC>=11700
        //$$ return entity.getPitch();
        //#else
        return entity.pitch;
        //#endif
    }

    @Pattern
    private static void Entity_setPos(Entity entity, double x, double y, double z) {
        //#if MC>=11500
        entity.setPos(x, y, z);
        //#else
        //$$ { net.minecraft.entity.Entity self = entity; self.x = x; self.y = y; self.z = z; }
        //#endif
    }

    //#if MC>=11400
    @Pattern
    private static void setWidth(AbstractButtonWidget button, int value) {
        button.setWidth(value);
    }

    @Pattern
    private static int getWidth(AbstractButtonWidget button) {
        return button.getWidth();
    }

    @Pattern
    private static int getHeight(AbstractButtonWidget button) {
        //#if MC>=11600
        return button.getHeight();
        //#else
        //$$ return ((com.replaymod.core.mixin.AbstractButtonWidgetAccessor) button).getHeight();
        //#endif
    }
    //#else
    //$$ @Pattern
    //$$ private static void setWidth(GuiButton button, int value) {
    //$$     button.width = value;
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static int getWidth(GuiButton button) {
    //$$     return button.width;
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static int getHeight(GuiButton button) {
    //$$     return button.height;
    //$$ }
    //#endif

    @Pattern
    private static String readString(PacketByteBuf buffer, int max) {
        //#if MC>=10800
        return buffer.readString(max);
        //#else
        //$$ return com.replaymod.core.versions.MCVer.tryReadString(buffer, max);
        //#endif
    }

    @Pattern
    //#if MC>=10800
    private static Entity getRenderViewEntity(MinecraftClient mc) {
        return mc.getCameraEntity();
    }
    //#else
    //$$ private static EntityLivingBase getRenderViewEntity(Minecraft mc) {
    //$$     return mc.renderViewEntity;
    //$$ }
    //#endif

    @Pattern
    //#if MC>=10800
    private static void setRenderViewEntity(MinecraftClient mc, Entity entity) {
        mc.setCameraEntity(entity);
    }
    //#else
    //$$ private static void setRenderViewEntity(Minecraft mc, EntityLivingBase entity) {
    //$$     mc.renderViewEntity = entity;
    //$$ }
    //#endif

    @Pattern
    private static Entity getVehicle(Entity passenger) {
        //#if MC>=10904
        return passenger.getVehicle();
        //#else
        //$$ return passenger.ridingEntity;
        //#endif
    }

    @Pattern
    private static PlayerInventory getInventory(PlayerEntity entity) {
        //#if MC>=11700
        //$$ return entity.getInventory();
        //#else
        return entity.inventory;
        //#endif
    }

    @Pattern
    private static Iterable<Entity> loadedEntityList(ClientWorld world) {
        //#if MC>=11400
        return world.getEntities();
        //#else
        //#if MC>=10809
        //$$ return world.loadedEntityList;
        //#else
        //$$ return ((java.util.List<net.minecraft.entity.Entity>) world.loadedEntityList);
        //#endif
        //#endif
    }

    @Pattern
    //#if MC>=11700
    //$$ private static void getEntitySectionArray() {}
    //#else
    private static Collection<Entity>[] getEntitySectionArray(WorldChunk chunk) {
        //#if MC>=11700
        //$$ return obsolete(chunk);
        //#elseif MC>=10800
        return chunk.getEntitySectionArray();
        //#else
        //$$ return chunk.entityLists;
        //#endif
    }
    //#endif

    @Pattern
    private static List<? extends PlayerEntity> playerEntities(World world) {
        //#if MC>=11400
        return world.getPlayers();
        //#elseif MC>=10809
        //$$ return world.playerEntities;
        //#else
        //$$ return ((List<? extends net.minecraft.entity.player.EntityPlayer>) world.playerEntities);
        //#endif
    }

    @Pattern
    private static boolean isOnMainThread(MinecraftClient mc) {
        //#if MC>=11400
        return mc.isOnThread();
        //#else
        //$$ return mc.isCallingFromMinecraftThread();
        //#endif
    }

    @Pattern
    private static void scheduleOnMainThread(MinecraftClient mc, Runnable runnable) {
        //#if MC>=11400
        mc.send(runnable);
        //#else
        //$$ mc.addScheduledTask(runnable);
        //#endif
    }

    @Pattern
    private static Window getWindow(MinecraftClient mc) {
        //#if MC>=11500
        return mc.getWindow();
        //#elseif MC>=11400
        //$$ return mc.window;
        //#else
        //$$ return new com.replaymod.core.versions.Window(mc);
        //#endif
    }

    @Pattern
    private static BufferBuilder Tessellator_getBuffer(Tessellator tessellator) {
        //#if MC>=10800
        return tessellator.getBuffer();
        //#else
        //$$ return new BufferBuilder(tessellator);
        //#endif
    }

    //#if MC<11700
    @Pattern
    private static void BufferBuilder_beginPosCol(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        buffer.begin(mode, VertexFormats.POSITION_COLOR);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_COLOR */);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_addPosCol(BufferBuilder buffer, double x, double y, double z, int r, int g, int b, int a) {
        //#if MC>=10809
        buffer.vertex(x, y, z).color(r, g, b, a).next();
        //#else
        //$$ { WorldRenderer $buffer = buffer; double $x = x; double $y = y; double $z = z; $buffer.setColorRGBA(r, g, b, a); $buffer.addVertex($x, $y, $z); }
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginPosTex(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        buffer.begin(mode, VertexFormats.POSITION_TEXTURE);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_TEXTURE */);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_addPosTex(BufferBuilder buffer, double x, double y, double z, float u, float v) {
        //#if MC>=10809
        buffer.vertex(x, y, z).texture(u, v).next();
        //#else
        //$$ buffer.addVertexWithUV(x, y, z, u, v);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginPosTexCol(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        buffer.begin(mode, VertexFormats.POSITION_TEXTURE_COLOR);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_TEXTURE_COLOR */);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_addPosTexCol(BufferBuilder buffer, double x, double y, double z, float u, float v, int r, int g, int b, int a) {
        //#if MC>=10809
        buffer.vertex(x, y, z).texture(u, v).color(r, g, b, a).next();
        //#else
        //$$ { WorldRenderer $buffer = buffer; double $x = x; double $y = y; double $z = z; float $u = u; float $v = v; $buffer.setColorRGBA(r, g, b, a); $buffer.addVertexWithUV($x, $y, $z, $u, $v); }
        //#endif
    }
    //#else
    //$$ @Pattern private static void BufferBuilder_beginPosCol() {}
    //$$ @Pattern private static void BufferBuilder_addPosCol() {}
    //$$ @Pattern private static void BufferBuilder_beginPosTex() {}
    //$$ @Pattern private static void BufferBuilder_addPosTex() {}
    //$$ @Pattern private static void BufferBuilder_beginPosTexCol() {}
    //$$ @Pattern private static void BufferBuilder_addPosTexCol() {}
    //#endif

    @Pattern
    private static Tessellator Tessellator_getInstance() {
        //#if MC>=10800
        return Tessellator.getInstance();
        //#else
        //$$ return Tessellator.instance;
        //#endif
    }

    @Pattern
    private static EntityRenderDispatcher getEntityRenderDispatcher(MinecraftClient mc) {
        //#if MC>=10800
        return mc.getEntityRenderDispatcher();
        //#else
        //$$ return com.replaymod.core.versions.MCVer.getRenderManager(mc);
        //#endif
    }

    @Pattern
    private static float getCameraYaw(EntityRenderDispatcher dispatcher) {
        //#if MC>=11500
        return dispatcher.camera.getYaw();
        //#else
        //$$ return dispatcher.cameraYaw;
        //#endif
    }

    @Pattern
    private static float getCameraPitch(EntityRenderDispatcher dispatcher) {
        //#if MC>=11500
        return dispatcher.camera.getPitch();
        //#else
        //$$ return dispatcher.cameraPitch;
        //#endif
    }

    @Pattern
    private static float getRenderPartialTicks(MinecraftClient mc) {
        //#if MC>=10900
        return mc.getTickDelta();
        //#else
        //$$ return ((com.replaymod.core.mixin.MinecraftAccessor) mc).getTimer().renderPartialTicks;
        //#endif
    }

    @Pattern
    private static TextureManager getTextureManager(MinecraftClient mc) {
        //#if MC>=11400
        return mc.getTextureManager();
        //#else
        //$$ return mc.renderEngine;
        //#endif
    }

    @Pattern
    private static String getBoundKeyName(KeyBinding keyBinding) {
        //#if MC>=11600
        return keyBinding.getBoundKeyLocalizedText().getString();
        //#elseif MC>=11400
        //$$ return keyBinding.getLocalizedName();
        //#else
        //$$ return org.lwjgl.input.Keyboard.getKeyName(keyBinding.getKeyCode());
        //#endif
    }

    @Pattern
    private static PositionedSoundInstance master(Identifier sound, float pitch) {
        //#if MC>=10900
        return PositionedSoundInstance.master(new SoundEvent(sound), pitch);
        //#elseif MC>=10800
        //$$ return PositionedSoundRecord.create(sound, pitch);
        //#else
        //$$ return PositionedSoundRecord.createPositionedSoundRecord(sound, pitch);
        //#endif
    }

    @Pattern
    private static boolean isKeyBindingConflicting(KeyBinding a, KeyBinding b) {
        //#if MC>=10900
        return a.equals(b);
        //#else
        //$$ return (a.getKeyCode() == b.getKeyCode());
        //#endif
    }

    //#if MC>=11600
    @Pattern
    private static void BufferBuilder_beginLineStrip(BufferBuilder buffer, VertexFormat vertexFormat) {
        //#if MC>=11700
        //$$ buffer.begin(net.minecraft.client.render.VertexFormat.DrawMode.LINE_STRIP, VertexFormats.LINES);
        //#else
        buffer.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginLines(BufferBuilder buffer) {
        //#if MC>=11700
        //$$ buffer.begin(net.minecraft.client.render.VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        //#else
        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginQuads(BufferBuilder buffer, VertexFormat vertexFormat) {
        //#if MC>=11700
        //$$ buffer.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, vertexFormat);
        //#else
        buffer.begin(GL11.GL_QUADS, vertexFormat);
        //#endif
    }
    //#else
    //$$ @Pattern private static void BufferBuilder_beginLineStrip() {}
    //$$ @Pattern private static void BufferBuilder_beginLines() {}
    //$$ @Pattern private static void BufferBuilder_beginQuads() {}
    //#endif

    @Pattern
    private static void GL11_glLineWidth(float width) {
        //#if MC>=11700
        //$$ com.mojang.blaze3d.systems.RenderSystem.lineWidth(width);
        //#else
        GL11.glLineWidth(width);
        //#endif
    }

    @Pattern
    private static void GL11_glTranslatef(float x, float y, float z) {
        //#if MC>=11700
        //$$ com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().translate(x, y, z);
        //#else
        GL11.glTranslatef(x, y, z);
        //#endif
    }

    @Pattern
    private static void GL11_glRotatef(float angle, float x, float y, float z) {
        //#if MC>=11700
        //$$ { float $angle = angle; com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().multiply(new net.minecraft.util.math.Quaternion(new net.minecraft.util.math.Vec3f(x, y, z), $angle, true)); }
        //#else
        GL11.glRotatef(angle, x, y, z);
        //#endif
    }

    // FIXME preprocessor bug: there are mappings for this, not sure why it doesn't remap by itself
    //#if MC>=11600
    @Pattern
    private static Matrix4f getPositionMatrix(MatrixStack.Entry stack) {
        //#if MC>=11800
        //$$ return stack.getPositionMatrix();
        //#else
        return stack.getModel();
        //#endif
    }
    //#else
    //$$ private static void getPositionMatrix() {}
    //#endif

    @SuppressWarnings("rawtypes") // preprocessor bug: doesn't work with generics
    @Pattern
    private static void Futures_addCallback(ListenableFuture future, FutureCallback callback) {
        //#if MC>=11800
        //$$ Futures.addCallback(future, callback, Runnable::run);
        //#else
        Futures.addCallback(future, callback);
        //#endif
    }

    @Pattern
    private static void setCrashReport(MinecraftClient mc, CrashReport report) {
        //#if MC>=11800
        //$$ mc.setCrashReportSupplier(() -> report);
        //#else
        mc.setCrashReport(report);
        //#endif
    }

    @Pattern
    private static CrashException crashReportToException(MinecraftClient mc) {
        //#if MC>=11800
        //$$ return new CrashException(((MinecraftAccessor) mc).getCrashReporter().get());
        //#else
        return new CrashException(((MinecraftAccessor) mc).getCrashReporter());
        //#endif
    }
}
