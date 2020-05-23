//#if FABRIC>=1
// Fabric equivalent is in ReplayModMMLauncher
//#else
//#if MC>=11400
//$$ // Generally not supported by RM
//#else
//$$ package com.replaymod.core.tweaker;
//$$
//$$ import com.replaymod.extras.modcore.ModCoreInstaller;
//$$ import net.minecraft.launchwrapper.ITweaker;
//$$ import net.minecraft.launchwrapper.Launch;
//$$ import net.minecraft.launchwrapper.LaunchClassLoader;
//$$
//$$ import java.io.File;
//$$ import java.util.List;
//$$
//$$ public class ReplayModTweaker implements ITweaker {
//$$     @Override
//$$     public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
//$$     }
//$$
//$$     @Override
//$$     public void injectIntoClassLoader(LaunchClassLoader classLoader) {
//$$         // Inject Mixin's tweaker (cause you can only specify one tweaker in the manifest)
//$$         @SuppressWarnings("unchecked")
//$$         List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
//$$         tweakClasses.add("org.spongepowered.asm.launch.MixinTweaker");
//$$
        //#if MC>=11202 && MC<=11202
        //$$ initModCore("1.12.2");
        //#endif
        //#if MC>=10809 && MC<=10809
        //$$ initModCore("1.8.9");
        //#endif
//$$     }
//$$
//$$     @Override
//$$     public String getLaunchTarget() {
//$$         return null;
//$$     }
//$$
//$$     @Override
//$$     public String[] getLaunchArguments() {
//$$         return new String[0];
//$$     }
//$$
//$$     private void initModCore(String mcVer) {
//$$         try {
//$$             if (System.getProperty("REPLAYMOD_SKIP_MODCORE", "false").equalsIgnoreCase("true")) {
//$$                 System.out.println("ReplayMod not initializing ModCore because REPLAYMOD_SKIP_MODCORE is true.");
//$$                 return;
//$$             }
//$$
//$$             if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
//$$                 System.out.println("ReplayMod not initializing ModCore because we're in a development environment.");
//$$                 return;
//$$             }
//$$
//$$             int result = ModCoreInstaller.initialize(Launch.minecraftHome, mcVer + "_forge");
//$$             if (result != -2) { // Don't even bother logging the result if there's no ModCore for this version.
//$$                 System.out.println("ReplayMod ModCore init result: " + result);
//$$             }
//$$             if (ModCoreInstaller.isErrored()) {
//$$                 System.err.println(ModCoreInstaller.getError());
//$$             }
//$$         } catch (Throwable t) {
//$$             System.err.println("ReplayMod caught error during ModCore init:");
//$$             t.printStackTrace();
//$$         }
//$$     }
//$$ }
//#endif
//#endif
