package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.function.Tickable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import org.apache.commons.io.FileUtils;

//#if MC>=11300
import net.minecraftforge.fml.ModList;
//#else
//#if MC>=10800
//$$ import net.minecraftforge.fml.common.Loader;
//#else
//$$ import cpw.mods.fml.common.Loader;
//#endif
//#endif

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import static com.replaymod.core.utils.Utils.SSL_SOCKET_FACTORY;
import static com.replaymod.extras.ReplayModExtras.LOGGER;

public class OpenEyeExtra implements Extra {
    private static final String DOWNLOAD_URL = "https://www.replaymod.com/dl/openeye/" + ReplayMod.getMinecraftVersion();

    private ReplayMod mod;

    @Override
    public void register(ReplayMod mod) throws Exception {
        this.mod = mod;

        //#if MC>=11300
        boolean isOpenEyeLoaded = ModList.get().isLoaded("openeye");
        //#else
        //$$ boolean isOpenEyeLoaded = Loader.isModLoaded("OpenEye");
        //#endif
        if (!isOpenEyeLoaded && mod.getSettingsRegistry().get(Setting.ASK_FOR_OPEN_EYE)) {
            new Thread(() -> {
                try {
                    LOGGER.trace("Checking for OpenEye availability");
                    HttpsURLConnection connection = (HttpsURLConnection) new URL(DOWNLOAD_URL).openConnection();
                    connection.setSSLSocketFactory(SSL_SOCKET_FACTORY);
                    connection.setRequestMethod("HEAD");
                    connection.connect();
                    LOGGER.trace("Got response code: {}", connection.getResponseCode());
                    if (connection.getResponseCode() == 200) {
                        mod.runLater(() -> new OfferGui(GuiScreen.wrap(mod.getMinecraft().currentScreen)).display());
                    } else {
                        LOGGER.info("Cannot offer OpenEye, server returned: {} {}",
                                connection.getResponseCode(), connection.getResponseMessage());
                    }
                    connection.disconnect();
                } catch (Throwable e) {
                    LOGGER.error("Failed to check for OpenEye availability:", e);
                }
            }).start();
        }
    }

    public class OfferGui extends AbstractGuiScreen<OfferGui> {
        public final GuiScreen parent;
        public final GuiPanel textPanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(3))
                .addElements(new VerticalLayout.Data(0.5),
                        new GuiLabel().setI18nText("replaymod.gui.offeropeneye1"),
                        new GuiLabel().setI18nText("replaymod.gui.offeropeneye2"),
                        new GuiLabel().setI18nText("replaymod.gui.offeropeneye3"),
                        new GuiLabel().setI18nText("replaymod.gui.offeropeneye4"),
                        new GuiLabel().setI18nText("replaymod.gui.offeropeneye5"),
                        new GuiLabel().setI18nText("replaymod.gui.offeropeneye6"),
                        new GuiLabel().setI18nText("replaymod.gui.offeropeneye7"),
                        new GuiLabel().setI18nText("replaymod.gui.offeropeneye8"));
        public final GuiPanel buttonPanel = new GuiPanel()
                .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(5));
        public final GuiPanel contentPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(20))
                .addElements(new VerticalLayout.Data(0.5), textPanel, buttonPanel);
        public final GuiButton yesButton = new GuiButton(buttonPanel).setSize(150, 20).setI18nLabel("gui.yes");
        public final GuiButton noButton = new GuiButton(buttonPanel).setSize(150, 20).setI18nLabel("gui.no");

        public OfferGui(GuiScreen parent) {
            this.parent = parent;

            yesButton.onClick(() -> {
                GuiPopup popup = new GuiPopup(OfferGui.this);
                new Thread(() -> {
                    try {
                        File targetFile = new File(mod.getMinecraft().gameDir, "mods/" + ReplayMod.getMinecraftVersion() + "/OpenEye.jar");
                        FileUtils.forceMkdir(targetFile.getParentFile());

                        HttpsURLConnection connection = (HttpsURLConnection) new URL(DOWNLOAD_URL).openConnection();
                        connection.setSSLSocketFactory(SSL_SOCKET_FACTORY);
                        ReadableByteChannel in = Channels.newChannel(connection.getInputStream());
                        FileChannel out = new FileOutputStream(targetFile).getChannel();
                        out.transferFrom(in, 0, Long.MAX_VALUE);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        mod.runLater(() -> {
                            popup.close();
                            parent.display();
                        });
                    }
                }).start();
            });
            noButton.onClick(() -> {
                mod.getSettingsRegistry().set(Setting.ASK_FOR_OPEN_EYE, false);
                mod.getSettingsRegistry().save();
                parent.display();
            });

            setLayout(new CustomLayout<OfferGui>() {
                @Override
                protected void layout(OfferGui container, int width, int height) {
                    pos(contentPanel, width / 2 - width(contentPanel) / 2, height / 2 - height(contentPanel) / 2);
                }
            });
        }

        @Override
        protected OfferGui getThis() {
            return this;
        }
    }

    public static final class GuiPopup extends AbstractGuiPopup<GuiPopup> {
        GuiPopup(GuiContainer container) {
            super(container);
            popup.addElements(null, new GuiIndicator().setColor(Colors.BLACK));
            setBackgroundColor(Colors.DARK_TRANSPARENT);
            open();
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        protected GuiPopup getThis() {
            return this;
        }

        private static final class GuiIndicator extends GuiLabel implements Tickable {
            private int tick;

            @Override
            public void tick() {
                tick++;
                setText(new String[]{
                        "Ooooo",
                        "oOooo",
                        "ooOoo",
                        "oooOo",
                        "ooooO",
                        "oooOo",
                        "ooOoo",
                        "oOooo",
                }[tick / 5 % 8]);
            }
        }
    }
}
