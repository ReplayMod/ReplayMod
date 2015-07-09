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

import de.johni0702.minecraft.gui.container.GuiContainer;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;

public abstract class AbstractComposedGuiElement<T extends AbstractComposedGuiElement<T>>
        extends AbstractGuiElement<T> implements ComposedGuiElement<T> {
    public AbstractComposedGuiElement() {
    }

    public AbstractComposedGuiElement(GuiContainer container) {
        super(container);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C forEach(final Class<C> ofType) {
        return (C) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ofType}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                boolean isGetter = method.getName().startsWith("get");
                Object handled = null;
                for (final GuiElement element : getChildren()) {
                    try {
                        if (element instanceof ComposedGuiElement) {
                            handled = method.invoke(((ComposedGuiElement) element).forEach(ofType), args);
                        } else if (ofType.isInstance(element)) {
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
