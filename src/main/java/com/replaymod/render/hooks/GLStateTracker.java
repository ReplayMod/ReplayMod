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
//$$     public BooleanState fog = new BooleanState(GL11.GL_FOG);
//$$     public BooleanState[] texture = new BooleanState[32];
//$$     {
//$$         for (int i = 0; i < texture.length; i++) {
//$$             texture[i] = new BooleanState(GL11.GL_TEXTURE_2D);
//$$         }
//$$     }
//$$
//$$     public void updateActiveTexture(int magic) {
//$$         this.activeTexture = magic - GL13.GL_TEXTURE0;
//$$     }
//$$
//$$     public void updateEnabledState(int magic, boolean enabled) {
//$$         switch (magic) {
//$$             case GL11.GL_FOG: fog.setState(enabled); break;
//$$             case GL11.GL_TEXTURE_2D: texture[activeTexture].setState(enabled); break;
//$$         }
//$$     }
//$$
//$$     public static class BooleanState {
//$$         public int capability;
//$$         public boolean currentState;
//$$
//$$         public BooleanState(int capability) {
//$$             this.capability = capability;
//$$         }
//$$
//$$         public void setState(boolean enabled) {
//$$             this.currentState = enabled;
//$$         }
//$$
//$$         public boolean getState() {
//$$             return currentState;
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
