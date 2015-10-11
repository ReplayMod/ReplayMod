/*
 * Copyright (c) 2015 johni0702
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package de.johni0702.minecraft.gui.container;

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.MinecraftGuiRenderer;
import de.johni0702.minecraft.gui.OffsetGuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.function.*;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class AbstractGuiOverlay<T extends AbstractGuiOverlay<T>> extends AbstractGuiContainer<T> {

    private final UserInputGuiScreen userInputGuiScreen = new UserInputGuiScreen();
    private final EventHandler eventHandler = new EventHandler();
    private boolean visible;
    private Dimension screenSize;
    private boolean mouseVisible;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            if (visible) {
                MinecraftForge.EVENT_BUS.register(eventHandler);
            } else {
                forEach(Closeable.class).close();
                MinecraftForge.EVENT_BUS.unregister(eventHandler);
            }
            updateUserInputGui();
        }
        this.visible = visible;
    }

    public boolean isMouseVisible() {
        return mouseVisible;
    }

    public void setMouseVisible(boolean mouseVisible) {
        this.mouseVisible = mouseVisible;
        updateUserInputGui();
    }

    private void updateUserInputGui() {
        Minecraft mc = getMinecraft();
        if (visible) {
            if (mouseVisible) {
                if (mc.currentScreen != userInputGuiScreen) {
                    mc.displayGuiScreen(userInputGuiScreen);
                }
            } else {
                if (mc.currentScreen == userInputGuiScreen) {
                    mc.displayGuiScreen(null);
                }
            }
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);
        if (mouseVisible && renderInfo.layer == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class).getTooltip(renderInfo);
            if (tooltip != null) {
                final ReadableDimension tooltipSize = tooltip.getMinSize();
                int x, y;
                if (renderInfo.mouseX + 8 + tooltipSize.getWidth() < screenSize.getWidth()) {
                    x = renderInfo.mouseX + 8;
                } else {
                    x = screenSize.getWidth() - tooltipSize.getWidth() - 1;
                }
                if (renderInfo.mouseY + 8 + tooltipSize.getHeight() < screenSize.getHeight()) {
                    y = renderInfo.mouseY + 8;
                } else {
                    y = screenSize.getHeight() - tooltipSize.getHeight() - 1;
                }
                final ReadablePoint position = new Point(x, y);
                try {
                    OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, position, tooltipSize);
                    tooltip.draw(eRenderer, tooltipSize, renderInfo);
                } catch (Exception ex) {
                    CrashReport crashReport = CrashReport.makeCrashReport(ex, "Rendering Gui Tooltip");
                    renderInfo.addTo(crashReport);
                    CrashReportCategory category = crashReport.makeCategory("Gui container details");
                    category.addCrashSectionCallable("Container", new Callable() {
                        @Override
                        public Object call() throws Exception {
                            return this;
                        }
                    });
                    category.addCrashSection("Width", size.getWidth());
                    category.addCrashSection("Height", size.getHeight());
                    category = crashReport.makeCategory("Tooltip details");
                    category.addCrashSectionCallable("Element", new Callable() {
                        @Override
                        public Object call() throws Exception {
                            return tooltip;
                        }
                    });
                    category.addCrashSectionCallable("Position", new Callable() {
                        @Override
                        public Object call() throws Exception {
                            return position;
                        }
                    });
                    category.addCrashSectionCallable("Size", new Callable() {
                        @Override
                        public Object call() throws Exception {
                            return tooltipSize;
                        }
                    });
                    throw new ReportedException(crashReport);
                }
            }
        }
    }

    @Override
    public ReadableDimension getMinSize() {
        return screenSize;
    }

    @Override
    public ReadableDimension getMaxSize() {
        return screenSize;
    }

    private class EventHandler {
        private MinecraftGuiRenderer renderer;

        @SubscribeEvent
        public void renderOverlay(RenderGameOverlayEvent.Post event) {
            if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
                updateRenderer();
                int layers = getMaxLayer();
                int mouseX = -1, mouseY = -1;
                if (mouseVisible) {
                    Point mouse = MouseUtils.getMousePos();
                    mouseX = mouse.getX();
                    mouseY = mouse.getY();
                }
                for (int layer = 0; layer <= layers; layer++) {
                    draw(renderer, screenSize, new RenderInfo(event.partialTicks, mouseX, mouseY, layer));
                }
            }
        }

        @SubscribeEvent
        public void tickOverlay(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                forEach(Tickable.class).tick();
            }
        }

        private void updateRenderer() {
            Minecraft mc = getMinecraft();
            ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            if (screenSize == null
                    || screenSize.getWidth() != res.getScaledWidth()
                    || screenSize.getHeight() != res.getScaledHeight()) {
                screenSize = new Dimension(res.getScaledWidth(), res.getScaledHeight());
                renderer = new MinecraftGuiRenderer(screenSize);
            }
        }
    }

    protected class UserInputGuiScreen extends net.minecraft.client.gui.GuiScreen {

        {
            allowUserInput = true;
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            forEach(Typeable.class).typeKey(MouseUtils.getMousePos(), keyCode, typedChar, isCtrlKeyDown(), isShiftKeyDown());
            super.keyTyped(typedChar, keyCode);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            forEach(Clickable.class).mouseClick(new Point(mouseX, mouseY), mouseButton);
        }

        @Override
        protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
            forEach(Draggable.class).mouseRelease(new Point(mouseX, mouseY), mouseButton);
        }

        @Override
        protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
            forEach(Draggable.class).mouseDrag(new Point(mouseX, mouseY), mouseButton, timeSinceLastClick);
        }

        @Override
        public void updateScreen() {
            forEach(Tickable.class).tick();
        }

        @Override
        public void onGuiClosed() {
            mouseVisible = false;
        }
    }
}
