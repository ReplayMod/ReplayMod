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
import lombok.Getter;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.Color;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;

public abstract class AbstractGuiCheckbox<T extends AbstractGuiCheckbox<T>>
        extends AbstractGuiClickable<T> implements IGuiCheckbox<T> {
    protected static final ResourceLocation BUTTON_SOUND = new ResourceLocation("gui.button.press");
    protected static final ReadableColor BOX_BACKGROUND_COLOR = new Color(46, 46, 46);

    @Getter
    private String label;

    @Getter
    private boolean checked;

    public AbstractGuiCheckbox() {
    }

    public AbstractGuiCheckbox(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        int color = 0xe0e0e0;
        if (!isEnabled()) {
            color = 0xa0a0a0;
        }

        int boxSize = size.getHeight();
        renderer.drawRect(0, 0, boxSize, boxSize, ReadableColor.BLACK);
        renderer.drawRect(1, 1, boxSize - 2, boxSize - 2, BOX_BACKGROUND_COLOR);

        if(isChecked()) {
            renderer.drawCenteredString(boxSize / 2 + 1, 1, color, "x");
        }

        renderer.drawString(boxSize + 2, 2, color, label);
    }

    @Override
    public ReadableDimension getMinSize() {
        FontRenderer fontRenderer = getMinecraft().fontRendererObj;
        int height = fontRenderer.FONT_HEIGHT + 2;
        int width = height + 2 + fontRenderer.getStringWidth(label);
        return new Dimension(width, height);
    }

    @Override
    public void onClick() {
        getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(BUTTON_SOUND, 1.0F));
        checked = !checked;
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

    @Override
    public T setChecked(boolean checked) {
        this.checked = checked;
        return getThis();
    }
}
