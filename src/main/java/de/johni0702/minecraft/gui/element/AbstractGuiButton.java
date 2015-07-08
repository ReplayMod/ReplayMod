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

import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.function.Clickable;
import lombok.Getter;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public abstract class AbstractGuiButton<T extends AbstractGuiButton<T>> extends AbstractGuiClickable<T> implements Clickable, IGuiButton<T> {
    protected static final ResourceLocation BUTTON_SOUND = new ResourceLocation("gui.button.press");
    protected static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation("textures/gui/widgets.png");

    @Getter
    private String label;

    public AbstractGuiButton() {
        setSize(new Dimension(200, 20));
    }

    public AbstractGuiButton(GuiContainer container) {
        super(container);
        setSize(new Dimension(200, 20));
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        renderer.bindTexture(WIDGETS_TEXTURE);
        GlStateManager.color(1, 1, 1, 1);

        byte texture = 1;
        int color = 0xe0e0e0;
        if (!isEnabled()) {
            texture = 0;
            color = 0xa0a0a0;
        } else if (isMouseHovering(new Point(renderInfo.mouseX, renderInfo.mouseY))) {
            texture = 2;
            color = 0xffffa0;
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        int textureY = 46 + texture * 20;
        int halfWidth = size.getWidth() / 2;

        renderer.drawTexturedRect(0, 0, 0, textureY, halfWidth, size.getHeight());
        renderer.drawTexturedRect(halfWidth, 0, 200 - halfWidth, textureY, halfWidth, size.getHeight());
        renderer.drawCenteredString(halfWidth, (size.getHeight() - 8) / 2, color, label);
    }

    @Override
    public ReadableDimension getMinSize() {
        return getPreferredSize();
    }

    @Override
    public void onClick() {
        getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(BUTTON_SOUND, 1.0F));
        super.onClick();
    }

    @Override
    public T setLabel(String label) {
        this.label = label;
        return getThis();
    }

    @Override
    public T setI18nLabel(String label, Object... args) {
        return setLabel(I18n.format(label, args));
    }
}
