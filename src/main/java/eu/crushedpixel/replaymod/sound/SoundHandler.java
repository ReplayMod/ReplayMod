package eu.crushedpixel.replaymod.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

public class SoundHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ResourceLocation successSoundLocation = new ResourceLocation("replaymod:renderSuccess");

    public void playRenderSuccessSound() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(successSoundLocation, 1.0F));
    }
}
