package com.replaymod.replay.events;

import com.replaymod.replay.camera.CameraEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
//#else
//$$ import cpw.mods.fml.common.eventhandler.Cancelable;
//$$ import cpw.mods.fml.common.eventhandler.Event;
//#endif

@Cancelable
@RequiredArgsConstructor
public class ReplayChatMessageEvent extends Event {
    @Getter
    private final CameraEntity cameraEntity;
}
