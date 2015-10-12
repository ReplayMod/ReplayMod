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

package de.johni0702.minecraft.gui.element;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import de.johni0702.minecraft.gui.container.GuiContainer;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class AbstractComposedGuiElement<T extends AbstractComposedGuiElement<T>>
        extends AbstractGuiElement<T> implements ComposedGuiElement<T> {
    public AbstractComposedGuiElement() {
    }

    public AbstractComposedGuiElement(GuiContainer container) {
        super(container);
    }

    @Override
    public int getMaxLayer() {
        return getLayer() + Ordering.natural().max(Iterables.concat(Collections.singleton(0),
                Iterables.transform(getChildren(), new Function<GuiElement, Integer>() {

                    @Nullable
                    @Override
                    public Integer apply(GuiElement e) {
                        return e instanceof ComposedGuiElement ? ((ComposedGuiElement) e).getMaxLayer() : e.getLayer();
                    }
                })));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C forEach(final Class<C> ofType) {
        int maxLayer = getMaxLayer();
        final List<C> layers = new ArrayList<C>(maxLayer + 1);
        for (int i = maxLayer; i >= 0; i--) {
            layers.add(forEach(i, ofType));
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

    @Override
    @SuppressWarnings("unchecked")
    public <C> C forEach(final int layer, final Class<C> ofType) {
        return (C) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ofType}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                boolean isGetter = method.getName().startsWith("get");
                Object handled = method.getReturnType().equals(boolean.class) ? false : null;
                final AbstractComposedGuiElement self = AbstractComposedGuiElement.this;
                if (ofType.isInstance(self) && self.getLayer() == layer) {
                    try {
                        handled = method.invoke(self, args);
                    } catch (Exception e) {
                        CrashReport crash = CrashReport.makeCrashReport(e, "Calling Gui method");
                        CrashReportCategory category = crash.makeCategory("Gui");
                        category.addCrashSection("Method", method);
                        category.addCrashSectionCallable("ComposedElement", new Callable() {
                            @Override
                            public Object call() throws Exception {
                                return self;
                            }
                        });
                        category.addCrashSectionCallable("Element", new Callable() {
                            @Override
                            public Object call() throws Exception {
                                return self;
                            }
                        });
                        throw new ReportedException(crash);
                    }
                    if (handled != null) {
                        if (handled instanceof Boolean) {
                            if (Boolean.TRUE.equals(handled)) {
                                return true;
                            }
                        } else if (isGetter) {
                            return handled;
                        }
                    }
                }
                for (final GuiElement element : getChildren()) {
                    try {
                        if (element instanceof ComposedGuiElement) {
                            ComposedGuiElement composed = (ComposedGuiElement) element;
                            if (layer <= composed.getMaxLayer()) {
                                Object elementProxy = composed.forEach(layer - composed.getLayer(), ofType);
                                handled = method.invoke(elementProxy, args);
                            }
                        } else if (ofType.isInstance(element) && element.getLayer() == layer) {
                            handled = method.invoke(element, args);
                        }
                        if (handled != null) {
                            if (handled instanceof Boolean) {
                                if (Boolean.TRUE.equals(handled)) {
                                    break;
                                }
                            } else if (isGetter) {
                                return handled;
                            }
                        }
                    } catch (Exception e) {
                        CrashReport crash = CrashReport.makeCrashReport(e, "Calling Gui method");
                        CrashReportCategory category = crash.makeCategory("Gui");
                        category.addCrashSection("Method", method);
                        category.addCrashSectionCallable("ComposedElement", new Callable() {
                            @Override
                            public Object call() throws Exception {
                                return element;
                            }
                        });
                        category.addCrashSectionCallable("Element", new Callable() {
                            @Override
                            public Object call() throws Exception {
                                return element;
                            }
                        });
                        throw new ReportedException(crash);
                    }
                }
                return handled;
            }
        });
    }
}
