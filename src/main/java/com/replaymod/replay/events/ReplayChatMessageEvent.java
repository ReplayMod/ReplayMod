package com.replaymod.replay.events;

import com.replaymod.replay.camera.CameraEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.Cancelable;
//$$ import net.minecraftforge.fml.common.eventhandler.Event;
//#endif
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
