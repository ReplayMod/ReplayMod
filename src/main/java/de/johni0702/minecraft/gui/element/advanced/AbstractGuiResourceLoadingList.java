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

package de.johni0702.minecraft.gui.element.advanced;

import com.google.common.base.Supplier;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiVerticalList;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.function.Clickable;
import de.johni0702.minecraft.gui.function.Closeable;
import de.johni0702.minecraft.gui.function.Loadable;
import de.johni0702.minecraft.gui.function.Tickable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractGuiResourceLoadingList
        <T extends AbstractGuiResourceLoadingList<T, U>, U extends GuiElement<U> & Comparable<U>>
        extends AbstractGuiVerticalList<T> implements Tickable, Loadable, Closeable {
    private static final String[] LOADING_TEXT = {"Ooo", "oOo", "ooO", "oOo"};
    private final GuiLabel loadingElement = new GuiLabel();
    private final GuiPanel resourcesPanel = new GuiPanel(getListPanel()).setLayout(new VerticalLayout());

    private Consumer<Consumer<Supplier<U>>> onLoad;
    private Runnable onSelectionChanged;
    private Thread loaderThread;
    private Queue<Runnable> resourcesQueue = new ConcurrentLinkedQueue<>();
    private int tick;

    private Element selected;

    public AbstractGuiResourceLoadingList() {
    }

    public AbstractGuiResourceLoadingList(GuiContainer container) {
        super(container);
    }

    @Override
    public void tick() {
        loadingElement.setText(LOADING_TEXT[tick++ / 5 % LOADING_TEXT.length]);
        Runnable resource;
        while ((resource = resourcesQueue.poll()) != null) {
            resource.run();
        }
    }

    @Override
    public void load() {
        // Stop current loading
        if (loaderThread != null) {
            loaderThread.interrupt();
            try {
                loaderThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Clear list
        resourcesQueue.clear();
        for (GuiElement element : new ArrayList<>(resourcesPanel.getChildren())) {
            resourcesPanel.removeElement(element);
        }
        selected = null;
        onSelectionChanged();

        // Load new data
        loaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                getListPanel().addElements(new VerticalLayout.Data(0.5), loadingElement);
                try {
                    onLoad.consume(new Consumer<Supplier<U>>() {
                        @Override
                        public void consume(final Supplier<U> obj) {
                            resourcesQueue.offer(new Runnable() {
                                @Override
                                public void run() {
                                    resourcesPanel.addElements(null, new Element(obj.get()));
                                }
                            });
                        }
                    });
                } finally {
                    resourcesQueue.offer(new Runnable() {
                        @Override
                        public void run() {
                            getListPanel().removeElement(loadingElement);
                        }
                    });
                }
            }
        });
        getListPanel().addElements(new VerticalLayout.Data(0.5), loadingElement);
        loaderThread.start();
    }

    @Override
    public void close() {
        loaderThread.interrupt();
    }

    public T onLoad(Consumer<Consumer<Supplier<U>>> function) {
        this.onLoad = function;
        return getThis();
    }

    public void onSelectionChanged() {
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    public T onSelectionChanged(Runnable onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
        return getThis();
    }

    public U getSelected() {
        return selected == null ? null : selected.resource;
    }

    private class Element extends GuiPanel implements Clickable, Comparable<Element> {
        private final U resource;
        private ReadableDimension size;

        public Element(final U resource) {
            this.resource = resource;
            addElements(null, resource);
            setLayout(new CustomLayout<GuiPanel>() {
                @Override
                protected void layout(GuiPanel container, int width, int height) {
                    pos(resource, 2, 2);
                }

                @Override
                public ReadableDimension calcMinSize(GuiContainer<?> container) {
                    ReadableDimension size = resource.getMinSize();
                    return new Dimension(size.getWidth() + 4, size.getHeight() + 4);
                }
            });
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            this.size = size;
            if (selected == this) {
                // Draw selection
                int w = size.getWidth();
                int h = size.getHeight();
                // Black background
                renderer.drawRect(0, 0, w, h, Colors.BLACK);
                // Light gray border
                renderer.drawRect(0, 0, w, 1, Colors.LIGHT_GRAY); // Top
                renderer.drawRect(0, h - 1, w, 1, Colors.LIGHT_GRAY); // Bottom
                renderer.drawRect(0, 0, 1, h, Colors.LIGHT_GRAY); // Left
                renderer.drawRect(w - 1, 0, 1, h, Colors.LIGHT_GRAY); // Right
            }
            super.draw(renderer, size, renderInfo);
        }

        @Override
        public boolean mouseClick(ReadablePoint position, int button) {
            Point point = new Point(position);
            getContainer().convertFor(this, point);
            if (point.getX() > 0 && point.getX() < size.getWidth()
                    && point.getY() > 0 && point.getY() < size.getHeight()) {
                if (selected != this) {
                    selected = this;
                    onSelectionChanged();
                }
                return true;
            }
            return false;
        }

        @Override
        public int compareTo(Element o) {
            return resource.compareTo(o.resource);
        }
    }
}
