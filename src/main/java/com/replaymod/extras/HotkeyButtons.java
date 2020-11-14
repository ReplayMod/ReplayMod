package com.replaymod.extras;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.ReplayMod;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.LayoutData;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class HotkeyButtons extends EventRegistrations implements Extra {
    private ReplayMod mod;

    @Override
    public void register(ReplayMod mod) {
        this.mod = mod;

        register();
    }

    { on(ReplayOpenedCallback.EVENT, replayHandler -> new Gui(mod, replayHandler.getOverlay())); }
    public static final class Gui {
        private final GuiButton toggleButton;
        private final GridLayout panelLayout;
        private final GuiPanel panel;

        private boolean open;

        public Gui(ReplayMod mod, GuiReplayOverlay overlay) {
            toggleButton = new GuiButton(overlay).setSize(20, 20)
                    .setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setSpriteUV(0, 120)
                    .onClick(new Runnable() {
                        @Override
                        public void run() {
                            open = !open;
                        }
                    });

            panel = new GuiPanel(overlay) {
                @Override
                public Collection<GuiElement> getChildren() {
                    return open ? super.getChildren() : Collections.<GuiElement>emptyList();
                }

                @Override
                public Map<GuiElement, LayoutData> getElements() {
                    return open ? super.getElements() : Collections.<GuiElement, LayoutData>emptyMap();
                }
            }.setLayout(panelLayout = new GridLayout().setSpacingX(5).setSpacingY(5).setColumns(1));

            final KeyBindingRegistry keyBindingRegistry = mod.getKeyBindingRegistry();
            keyBindingRegistry.getBindings().values().stream()
                    .sorted(Comparator.comparing(it -> I18n.translate(it.name)))
                    .forEachOrdered(keyBinding -> {
                GuiButton button = new GuiButton(){
                    @Override
                    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
                        // There doesn't seem to be an KeyBindingUpdate event, so we'll just update it every time
                        setLabel(keyBinding.isBound() ? keyBinding.getBoundKey() : "");

                        if (keyBinding.supportsAutoActivation()) {
                            setTooltip(new GuiTooltip().setText(new String[]{
                                    I18n.translate("replaymod.gui.ingame.autoactivating"),
                                    I18n.translate("replaymod.gui.ingame.autoactivating."
                                            + (keyBinding.isAutoActivating() ? "disable" : "enable")),
                            }));
                            setLabelColor(keyBinding.isAutoActivating() ? 0x00ff00 : 0xe0e0e0);
                        }

                        super.draw(renderer, size, renderInfo);
                    }
                }.onClick(() -> {
                    if (keyBinding.supportsAutoActivation() && Screen.hasControlDown()) {
                        keyBinding.setAutoActivating(!keyBinding.isAutoActivating());
                    } else {
                        keyBinding.trigger();
                    }
                });
                GuiLabel label = new GuiLabel().setI18nText(keyBinding.name);
                panel.addElements(null, new GuiPanel().setLayout(new CustomLayout<GuiPanel>() {
                    @Override
                    protected void layout(GuiPanel container, int width, int height) {
                        width(button, Math.max(10 /* consistent min width */, width(button)) + 10 /* padding */);
                        height(button, 20);

                        int textWidth = width(label);

                        x(label, width(button) + 4);
                        width(label, width - x(label));

                        if (textWidth > width - x(label)) {
                            height(label, height(label) * 2); // split over two lines
                        }
                        y(label, (height - height(label)) / 2);
                    }
                }).addElements(null, button, label).setSize(150, 20));
            });

            overlay.setLayout(new CustomLayout<GuiReplayOverlay>(overlay.getLayout()) {
                @Override
                protected void layout(GuiReplayOverlay container, int width, int height) {
                    panelLayout.setColumns(Math.max(1, (width - 10) / 155));
                    size(panel, panel.getMinSize());

                    pos(toggleButton, 5, height - 25);
                    pos(panel, 5, y(toggleButton) - 5 - height(panel));
                }
            });
        }
    }
}
