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
import de.johni0702.minecraft.gui.OffsetGuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.element.AbstractComposedGuiElement;
import de.johni0702.minecraft.gui.element.ComposedGuiElement;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.layout.Layout;
import de.johni0702.minecraft.gui.layout.LayoutData;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkState;

public abstract class AbstractGuiContainer<T extends AbstractGuiContainer<T>>
        extends AbstractComposedGuiElement<T> implements GuiContainer<T> {

    private final Map<GuiElement, LayoutData> elements = new LinkedHashMap<GuiElement, LayoutData>();

    private Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> layedOutElements;

    private Layout layout;

    public AbstractGuiContainer() {
    }

    public AbstractGuiContainer(GuiContainer container) {
        super(container);
    }

    @Override
    public T setLayout(Layout layout) {
        this.layout = layout;
        return getThis();
    }

    @Override
    public Layout getLayout() {
        return layout;
    }

    @Override
    public void convertFor(GuiElement element, Point point) {
        checkState(layedOutElements != null, "Cannot convert position unless rendered at least once.");
        Pair<ReadablePoint, ReadableDimension> pair = layedOutElements.get(element);
        checkState(pair != null, "Element " + element + " not part of " + this);
        ReadablePoint pos = pair.getKey();
        point.translate(-pos.getX(), -pos.getY());
        if (getContainer() != null) {
            getContainer().convertFor(this, point);
        }
    }

    @Override
    public Collection<GuiElement> getChildren() {
        return Collections.unmodifiableCollection(elements.keySet());
    }

    @Override
    public Map<GuiElement, LayoutData> getElements() {
        return Collections.unmodifiableMap(elements);
    }

    @Override
    public T addElements(LayoutData layoutData, GuiElement... elements) {
        if (layoutData == null) {
            layoutData = LayoutData.NONE;
        }
        for (GuiElement element : elements) {
            this.elements.put(element, layoutData);
            element.setContainer(this);
        }
        return getThis();
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        try {
            layedOutElements = layout.layOut(this, size);
        } catch (Exception ex) {
            CrashReport crashReport = CrashReport.makeCrashReport(ex, "Gui Layout");
            renderInfo.addTo(crashReport);
            CrashReportCategory category = crashReport.makeCategory("Gui container details");
            category.addCrashSectionCallable("Container", new Callable() {
                @Override
                public Object call() throws Exception {
                    return this;
                }
            });
            category.addCrashSectionCallable("Layout", new Callable() {
                @Override
                public Object call() throws Exception {
                    return layout;
                }
            });
            throw new ReportedException(crashReport);
        }
        for (final Map.Entry<GuiElement, Pair<ReadablePoint, ReadableDimension>> e : layedOutElements.entrySet()) {
            GuiElement element = e.getKey();
            if (element instanceof ComposedGuiElement) {
                if (((ComposedGuiElement) element).getMaxLayer() < renderInfo.layer) {
                    continue;
                }
            } else {
                if (element.getLayer() != renderInfo.layer) {
                    continue;
                }
            }
            final ReadablePoint ePosition = e.getValue().getLeft();
            final ReadableDimension eSize = e.getValue().getRight();
            try {
                OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, ePosition, eSize);
                e.getKey().draw(eRenderer, eSize, renderInfo.offsetMouse(ePosition.getX(), ePosition.getY()));
            } catch (Exception ex) {
                CrashReport crashReport = CrashReport.makeCrashReport(ex, "Rendering Gui");
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
                category.addCrashSectionCallable("Layout", new Callable() {
                    @Override
                    public Object call() throws Exception {
                        return layout;
                    }
                });
                category = crashReport.makeCategory("Gui element details");
                category.addCrashSectionCallable("Element", new Callable() {
                    @Override
                    public Object call() throws Exception {
                        return e.getKey();
                    }
                });
                category.addCrashSectionCallable("Position", new Callable() {
                    @Override
                    public Object call() throws Exception {
                        return ePosition;
                    }
                });
                category.addCrashSectionCallable("Size", new Callable() {
                    @Override
                    public Object call() throws Exception {
                        return eSize;
                    }
                });
                if (e.getKey() instanceof GuiContainer) {
                    category.addCrashSectionCallable("Layout", new Callable() {
                        @Override
                        public Object call() throws Exception {
                            return ((GuiContainer) e.getKey()).getLayout();
                        }
                    });
                }
                throw new ReportedException(crashReport);
            }
        }
    }

    @Override
    public ReadableDimension getMinSize() {
        return layout.calcMinSize(this);
    }
}
