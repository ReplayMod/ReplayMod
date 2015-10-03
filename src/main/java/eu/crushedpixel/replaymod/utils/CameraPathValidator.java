package eu.crushedpixel.replaymod.utils;

import com.replaymod.core.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import net.minecraft.client.resources.I18n;

public class CameraPathValidator {

    public static class InvalidCameraPathException extends Exception {
        public InvalidCameraPathException(String message) {
            super(message);
        }

        @Override
        public String getLocalizedMessage() {
            return I18n.format(getMessage());
        }

        public void printToChat() {
            ReplayMod.chatMessageHandler.addLocalizedChatMessage(getMessage(), ChatMessageHandler.ChatMessageType.WARNING);
        }
    }

    public static void validateCameraPath(KeyframeList<AdvancedPosition> positionKeyframes, KeyframeList<TimestampValue> timeKeyframes)
            throws InvalidCameraPathException {

        if(positionKeyframes.size() < 2 || timeKeyframes.size() < 2) {
            throw new InvalidCameraPathException("replaymod.chat.morekeyframes");
        }

        int previousTimestamp = -1;
        for(Keyframe<TimestampValue> timeKeyframe : timeKeyframes) {
            if(timeKeyframe.getValue().asInt() < previousTimestamp) throw new InvalidCameraPathException("replaymod.chat.negativetime");
            previousTimestamp = timeKeyframe.getValue().asInt();
        }
    }
}
