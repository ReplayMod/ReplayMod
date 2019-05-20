package com.replaymod.core.gui;

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.container.VanillaGuiScreen;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

//#if MC>=11400
import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
import net.minecraft.client.gui.screen.Screen;
//#else
//$$ import net.minecraftforge.client.event.GuiScreenEvent;
//$$ import net.minecraftforge.common.MinecraftForge;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import static com.replaymod.core.versions.MCVer.getGui;
//#endif

import static com.replaymod.core.versions.MCVer.getMinecraft;

public class GuiBackgroundProcesses extends EventRegistrations {
    private GuiPanel panel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(10));

    //#if MC>=11400
    { on(InitScreenCallback.EVENT, (screen, buttons) -> onGuiInit(screen)); }
    private void onGuiInit(Screen guiScreen) {
    //#else
    //$$ @SubscribeEvent
    //$$ public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
    //$$     net.minecraft.client.gui.GuiScreen guiScreen = getGui(event);
    //#endif
        if (guiScreen != getMinecraft().currentScreen) return; // people tend to construct GuiScreens without opening them

        VanillaGuiScreen.setup(guiScreen).setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(panel, width - 5 - width(panel), 5);
            }
        }).addElements(null, panel);
    }

    public void addProcess(GuiElement<?> element) {
        panel.addElements(new VerticalLayout.Data(1.0),
                new Element(element));
    }

    public void removeProcess(GuiElement<?> element) {
        for (GuiElement child : panel.getChildren()) {
            if (((Element) child).inner == element) {
                panel.removeElement(child);
                return;
            }
        }
    }

    private static class Element extends AbstractGuiContainer<Element> {
        private GuiElement inner;

        Element(GuiElement inner) {
            this.inner = inner;
            addElements(null, inner);

            setLayout(new CustomLayout<Element>() {
                @Override
                protected void layout(Element container, int width, int height) {
                    pos(inner, 10, 10);
                    size(inner, width - 20, height - 20);
                }
            });
        }

        @Override
        public ReadableDimension getMinSize() {
            ReadableDimension minSize = inner.getMinSize();
            return new Dimension(minSize.getWidth() + 20, minSize.getHeight() + 20);
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            final int u0 = 0;
            final int v0 = 39;
            renderer.bindTexture(TEXTURE);

            int w = size.getWidth();
            int h = size.getHeight();

            // Corners
            renderer.drawTexturedRect(0, 0, u0, v0, 5, 5); // Top left
            renderer.drawTexturedRect(w - 5, 0, u0 + 12, v0, 5, 5); // Top right
            renderer.drawTexturedRect(0, h - 5, u0, v0 + 12, 5, 5); // Bottom left
            renderer.drawTexturedRect(w - 5, h - 5, u0 + 12, v0 + 12, 5, 5); // Bottom right

            // Top and bottom edge
            for (int x = 5; x < w - 5; x += 5) {
                int rx = Math.min(5, w - 5 - x);
                renderer.drawTexturedRect(x, 0, u0 + 6, v0, rx, 5); // Top
                renderer.drawTexturedRect(x, h - 5, u0 + 6, v0 + 12, rx, 5); // Bottom
            }

            // Left and right edge
            for (int y = 5; y < h - 5; y += 5) {
                int ry = Math.min(5, h - 5 - y);
                renderer.drawTexturedRect(0, y, u0, v0 + 6, 5, ry); // Left
                renderer.drawTexturedRect(w - 5, y, u0 + 12, v0 + 6, 5, ry); // Right
            }

            // Center
            for (int x = 5; x < w - 5; x += 5) {
                for (int y = 5; y < h - 5; y += 5) {
                    int rx = Math.min(5, w - 5 - x);
                    int ry = Math.min(5, h - 5 - y);
                    renderer.drawTexturedRect(x, y, u0 + 6, v0 + 6, rx, ry);
                }
            }

            super.draw(renderer, size, renderInfo);
        }

        @Override
        protected Element getThis() {
            return this;
        }
    }
}
