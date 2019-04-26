//#if MC<=10710
//$$ package com.replaymod.render.hooks;
//$$
//$$ import org.lwjgl.opengl.GL11;
//$$ import org.lwjgl.opengl.GL13;
//$$
//$$ /**
//$$  * Tracks OpenGL state for use in the ODS shader.
//$$  * All methods should be called from the GL mixins only.
//$$  */
//$$ public class GLStateTracker {
//$$     private static ThreadLocal<GLStateTracker> stateTracker = ThreadLocal.withInitial(GLStateTracker::new);
//$$     public static GLStateTracker getInstance() {
//$$         return stateTracker.get();
//$$     }
//$$
//$$     public int activeTexture;
//$$
//$$     public void updateActiveTexture(int magic) {
//$$         this.activeTexture = magic - GL13.GL_TEXTURE0;
//$$     }
//$$
//$$     public void updateEnabledState(int magic, boolean enabled) {
//$$         switch (magic) {
//$$             case GL11.GL_FOG: FogStateCallback.EVENT.invoker().fogStateChanged(enabled); break;
//$$             case GL11.GL_TEXTURE_2D: Texture2DStateCallback.EVENT.invoker().texture2DStateChanged(activeTexture, enabled); break;
//$$         }
//$$     }
//$$
//$$     // Called via ASM (see GLStateTrackerTransformer)
//$$     @Deprecated
//$$     @SuppressWarnings("unused")
//$$     public static void hook_glEnable(int magic) {
//$$         GL11.glEnable(magic);
//$$         getInstance().updateEnabledState(magic, true);
//$$     }
//$$
//$$     // Called via ASM (see GLStateTrackerTransformer)
//$$     @Deprecated
//$$     @SuppressWarnings("unused")
//$$     public static void hook_glDisable(int magic) {
//$$         GL11.glDisable(magic);
//$$         getInstance().updateEnabledState(magic, false);
//$$     }
//$$ }
//#endif
