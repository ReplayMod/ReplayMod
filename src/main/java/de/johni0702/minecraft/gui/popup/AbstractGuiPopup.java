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

package de.johni0702.minecraft.gui.popup;

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiContainer;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.Layout;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGuiPopup<T extends AbstractGuiPopup<T>> extends AbstractGuiContainer<T> {
    private final GuiPanel popupContainer = new GuiPanel(this){
        private final int u0 = 0;
        private final int v0 = 39;

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
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
            for (int y = 5; y < w - 5; y += 5) {
                int ry = Math.min(5, h - 5 - y);
                renderer.drawTexturedRect(0, y, u0, v0 + 6, 5, ry); // Left
                renderer.drawTexturedRect(w - 5, y, u0 + 12, v0 + 6, 5, ry); // Right
            }

            // Center
            for (int x = 5; x < w - 5; x += 5) {
                for (int y = 5; y < w - 5; y += 5) {
                    int rx = Math.min(5, w - 5 - x);
                    int ry = Math.min(5, h - 5 - y);
                    renderer.drawTexturedRect(x, y, u0 + 6, v0 + 6, rx, ry);
                }
            }

            super.draw(renderer, size, renderInfo);
        }
    }.setLayout(new CustomLayout<GuiPanel>() {
        @Override
        protected void layout(GuiPanel container, int width, int height) {
            pos(popup, 10, 10);
        }

        @Override
        public ReadableDimension calcMinSize(GuiContainer<?> container) {
            ReadableDimension size = popup.calcMinSize();
            return new Dimension(size.getWidth() + 20, size.getHeight() + 20);
        }
    });
    protected final GuiPanel popup = new GuiPanel(popupContainer);

    private int layer;

    {
        setLayout(new CustomLayout<T>() {
            @Override
            protected void layout(T container, int width, int height) {
                pos(popupContainer, width / 2 - width(popupContainer) / 2, height / 2 - height(popupContainer) / 2);
            }
        });
    }

    private final Layout originalLayout;

    public AbstractGuiPopup(GuiContainer container) {
        super(container);
        container.setLayout(new CustomLayout(originalLayout = container.getLayout()) {
            @Override
            protected void layout(GuiContainer container, int width, int height) {
                pos(AbstractGuiPopup.this, 0, 0);
                size(AbstractGuiPopup.this, width, height);
            }
        });
    }

    protected void close() {
        getContainer().setLayout(originalLayout);
        getContainer().removeElement(this);
    }

    public T setLayer(int layer) {
        this.layer = layer;
        return getThis();
    }

    @Override
    public int getLayer() {
        return layer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C forEach(int layer, final Class<C> ofType) {
        final C realProxy = super.forEach(layer, ofType);
        if (layer >= 0) {
            return (C) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ofType}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getReturnType().equals(boolean.class)) {
                        method.invoke(forEachChild(ofType), args);
                        return true;
                    }
                    return method.invoke(realProxy, args);
                }
            });
        } else {
            return realProxy;
        }
    }

    @SuppressWarnings("unchecked")
    private <C> C forEachChild(final Class<C> ofType) {
        int maxLayer = getMaxLayer();
        final List<C> layers = new ArrayList<C>(maxLayer + 1);
        for (int i = maxLayer; i >= 0; i--) {
            layers.add(super.forEach(i, ofType));
        }
        return (C) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ofType}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                boolean isGetter = method.getName().startsWith("get");
                Object handled = method.getReturnType().equals(boolean.class) ? false : null;
                for (final C layer : layers) {
                    handled = method.invoke(layer, args);
                    if (handled != null) {
                        if (handled instanceof Boolean) {
                            if (Boolean.TRUE.equals(handled)) {
                                break;
                            }
                        } else if (isGetter) {
                            return handled;
                        }
                    }
                }
                return handled;
            }
        });
    }
}
