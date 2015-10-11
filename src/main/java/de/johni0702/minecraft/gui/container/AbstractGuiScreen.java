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
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.function.*;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

import java.io.IOException;
import java.util.concurrent.Callable;

public abstract class AbstractGuiScreen<T extends AbstractGuiScreen<T>> extends AbstractGuiContainer<T> {

    private final MinecraftGuiScreen wrapped = new MinecraftGuiScreen();

    private Dimension screenSize;

    @Getter
    @Setter
    private boolean drawBackground = true;

    @Getter
    private boolean enabledRepeatedKeyEvents = true;

    @Getter
    @Setter
    private GuiLabel title;

    public net.minecraft.client.gui.GuiScreen toMinecraft() {
        return wrapped;
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        if (renderInfo.layer == 0) {
            if (drawBackground) {
                wrapped.drawDefaultBackground();
            }
            if (title != null) {
                ReadableDimension titleSize = title.getMinSize();
                int x = screenSize.getWidth() / 2 - titleSize.getWidth() / 2;
                OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, new Point(x, 10), new Dimension(0, 0));
                title.draw(eRenderer, titleSize, null);
            }
        }
        super.draw(renderer, size, renderInfo);
        if (renderInfo.layer == getMaxLayer()) {
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

    public void setEnabledRepeatedKeyEvents(boolean enableRepeatKeyEvents) {
        this.enabledRepeatedKeyEvents = enableRepeatKeyEvents;
        if (wrapped.active) {
            Keyboard.enableRepeatEvents(enableRepeatKeyEvents);
        }
    }

    public void display() {
        getMinecraft().displayGuiScreen(toMinecraft());
    }

    protected class MinecraftGuiScreen extends net.minecraft.client.gui.GuiScreen {
        private MinecraftGuiRenderer renderer;
        private boolean active;

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            int layers = getMaxLayer();
            for (int layer = 0; layer <= layers; layer++) {
                draw(renderer, screenSize, new RenderInfo(partialTicks, mouseX, mouseY, layer));
            }
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
            forEach(Closeable.class).close();
            active = false;
            if (enabledRepeatedKeyEvents) {
                Keyboard.enableRepeatEvents(false);
            }
        }

        @Override
        public void initGui() {
            active = false;
            if (enabledRepeatedKeyEvents) {
                Keyboard.enableRepeatEvents(true);
            }
            screenSize = new Dimension(width, height);
            renderer = new MinecraftGuiRenderer(screenSize);
        }

        public T getWrapper() {
            return AbstractGuiScreen.this.getThis();
        }
    }
}
