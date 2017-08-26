package com.replaymod.replay.events;

import com.replaymod.replay.camera.CameraEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
@RequiredArgsConstructor
public class ReplayChatMessageEvent extends Event {
    @Getter
    private final CameraEntity cameraEntity;
}
