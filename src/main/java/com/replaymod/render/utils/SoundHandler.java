package com.replaymod.render.utils;

import com.replaymod.core.versions.MCVer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class SoundHandler {

    private final ResourceLocation successSoundLocation = new ResourceLocation("replaymod", "render_success.wav");

    public void playRenderSuccessSound() {
        playSound(successSoundLocation);
    }

    /**
     * Plays a <b>.wav</b> Sound from a ResourceLocation. This method does <b>not</b> respect Game Settings like Audio Volume.
     * @param loc The Sound File's ResourceLocation
     */
    public void playSound(ResourceLocation loc) {
        try {
            InputStream is = MCVer.getMinecraft().getResourceManager().getResource(loc).getInputStream();
            byte[] bytes = IOUtils.toByteArray(is);
            is.close();
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));

            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
