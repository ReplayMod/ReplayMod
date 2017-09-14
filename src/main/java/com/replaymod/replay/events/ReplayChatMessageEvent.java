package com.replaymod.replay.events;

import com.replaymod.replay.camera.CameraEntity;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Cancelable
@RequiredArgsConstructor
public class ReplayChatMessageEvent extends Event {
    @Getter
    private final CameraEntity cameraEntity;
}
