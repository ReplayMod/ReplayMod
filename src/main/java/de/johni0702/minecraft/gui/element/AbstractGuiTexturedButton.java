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
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.*;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public abstract class AbstractGuiTexturedButton<T extends AbstractGuiTexturedButton<T>> extends AbstractGuiClickable<T> implements Clickable, IGuiTexturedButton<T> {
    protected static final ResourceLocation BUTTON_SOUND = new ResourceLocation("gui.button.press");

    @Getter
    private ResourceLocation texture;

    @Getter
    private ReadableDimension textureSize = new ReadableDimension() {
        @Override
        public int getWidth() {
            return getMaxSize().getWidth();
        }

        @Override
        public int getHeight() {
            return getMaxSize().getHeight();
        }

        @Override
        public void getSize(WritableDimension dest) {
            getMaxSize().getSize(dest);
        }
    };

    @Getter
    private ReadableDimension textureTotalSize;

    @Getter
    private ReadablePoint textureNormal;

    @Getter
    private ReadablePoint textureHover;

    @Getter
    private ReadablePoint textureDisabled;

    public AbstractGuiTexturedButton() {
    }

    public AbstractGuiTexturedButton(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        renderer.bindTexture(texture);

        ReadablePoint texture = textureNormal;
        if (!isEnabled()) {
            texture = textureDisabled;
        } else if (isMouseHovering(new Point(renderInfo.mouseX, renderInfo.mouseY))) {
            texture = textureHover;
        }

        if (texture == null) { // Button is disabled but we have no texture for that
            GlStateManager.color(0.5f, 0.5f, 0.5f, 1);
            texture = textureNormal;
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        renderer.drawTexturedRect(0, 0, texture.getX(), texture.getY(), size.getWidth(), size.getHeight(),
                textureSize.getWidth(), textureSize.getHeight(),
                textureTotalSize.getWidth(), textureTotalSize.getHeight());
    }

    @Override
    public ReadableDimension calcMinSize() {
        return new Dimension(0, 0);
    }

    @Override
    public void onClick() {
        getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(BUTTON_SOUND, 1.0F));
        super.onClick();
    }

    @Override
    public T setTexture(ResourceLocation resourceLocation, int size) {
        return setTexture(resourceLocation, size, size);
    }

    @Override
    public T setTexture(ResourceLocation resourceLocation, int width, int height) {
        this.texture = resourceLocation;
        this.textureTotalSize = new Dimension(width, height);
        return getThis();
    }

    @Override
    public T setTextureSize(int size) {
        return setTextureSize(size, size);
    }

    @Override
    public T setTextureSize(int width, int height) {
        this.textureSize = new Dimension(width, height);
        return getThis();
    }

    @Override
    public T setTexturePosH(final int x, final int y) {
        return setTexturePosH(new Point(x, y));
    }

    @Override
    public T setTexturePosV(final int x, final int y) {
        return setTexturePosV(new Point(x, y));
    }

    @Override
    public T setTexturePosH(final ReadablePoint pos) {
        this.textureNormal = pos;
        this.textureHover = new ReadablePoint() {
            @Override
            public int getX() {
                return pos.getX() + textureSize.getWidth();
            }

            @Override
            public int getY() {
                return pos.getY();
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        };
        return getThis();
    }

    @Override
    public T setTexturePosV(final ReadablePoint pos) {
        this.textureNormal = pos;
        this.textureHover = new ReadablePoint() {
            @Override
            public int getX() {
                return pos.getX();
            }

            @Override
            public int getY() {
                return pos.getY() + textureSize.getHeight();
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        };
        return getThis();
    }

    @Override
    public T setTexturePos(int normalX, int normalY, int hoverX, int hoverY) {
        return setTexturePos(new Point(normalX, normalY), new Point(hoverX, hoverY));
    }

    @Override
    public T setTexturePos(ReadablePoint normal, ReadablePoint hover) {
        this.textureNormal = normal;
        this.textureHover = hover;
        return getThis();
    }

    @Override
    public T setTexturePos(int normalX, int normalY, int hoverX, int hoverY, int disabledX, int disabledY) {
        return setTexturePos(new Point(normalX, normalY), new Point(hoverX, hoverY), new Point(disabledX, disabledY));
    }

    @Override
    public T setTexturePos(ReadablePoint normal, ReadablePoint hover, ReadablePoint disabled) {
        this.textureDisabled = disabled;
        return setTexturePos(normal, hover);
    }
}
