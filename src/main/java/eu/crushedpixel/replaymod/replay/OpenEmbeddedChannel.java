package eu.crushedpixel.replaymod.replay;

import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.NetworkManager;

public class OpenEmbeddedChannel extends EmbeddedChannel {

    private boolean ignoreClose = false;

    public OpenEmbeddedChannel(NetworkManager networkManager) {
        super(networkManager);
    }

    @Override
    public boolean finish() {
        System.out.println("wanted to finish");
        ignoreClose = true;
        return super.finish();
    }

    @Override
    public ChannelFuture close() {
        if(ignoreClose) {
            ignoreClose = false;
            return null;
        }
        return pipeline().close();
    }

    public ChannelFuture manualClose() {
        return pipeline().close();
    }
}
