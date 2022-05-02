package com.replaymod.core.gui.common

import gg.essential.elementa.UIComponent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.shader.BlendState
import net.minecraft.util.Identifier
import java.awt.Color

class UI9Slice(
    private val texture: Identifier,
    private var textureData: State<TextureData>,
) : UIComponent() {

    constructor(texture: Identifier, textureData: TextureData) : this(texture, BasicState(textureData))

    fun bindTextureData(textureData: State<TextureData>) {
        this.textureData = textureData
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val left = getLeft().toDouble()
        val right = getRight().toDouble()
        val top = getTop().toDouble()
        val bottom = getBottom().toDouble()

        UGraphics.bindTexture(0, texture)
        draw(matrixStack, left, right, top, bottom, getColor(), textureData.get())

        super.draw(matrixStack)
    }

    companion object {
        fun draw(
            matrixStack: UMatrixStack,
            l: Double,
            r: Double,
            t: Double,
            b: Double,
            color: Color,
            data: TextureData,
        ) {
            val oldBlendState = BlendState.active()
            BlendState.NORMAL.activate()

            UGraphics.enableAlpha()

            val red = color.red.toFloat() / 255f
            val green = color.green.toFloat() / 255f
            val blue = color.blue.toFloat() / 255f
            val alpha = color.alpha.toFloat() / 255f
            val buffer = UGraphics.getFromTessellator()

            buffer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR)

            fun UGraphics.texS(u: Double, v: Double) = tex(u / data.textureWidth, v / data.textureHeight)

            fun drawTexturedRect(x: Double, y: Double, u: Double, v: Double, width: Double, height: Double) {
                buffer.pos(matrixStack, x, y + height, 0.0)
                    .texS(u, v + height)
                    .color(red, green, blue, alpha)
                    .endVertex()
                buffer.pos(matrixStack, x + width, y + height, 0.0)
                    .texS(u + width, v + height)
                    .color(red, green, blue, alpha)
                    .endVertex()
                buffer.pos(matrixStack, x + width, y, 0.0)
                    .texS(u + width, v)
                    .color(red, green, blue, alpha)
                    .endVertex()
                buffer.pos(matrixStack, x, y, 0.0)
                    .texS(u, v)
                    .color(red, green, blue, alpha)
                    .endVertex()
            }

            with(data) {
                // Corners
                run {
                    drawTexturedRect(l, t, lt, tt, ls, ts) // Top left
                    drawTexturedRect(r - rs, t, rt - rs, tt, rs, ts) // Top right
                    drawTexturedRect(l, b - bs, lt, bt - bs, ls, bs) // Bottom left
                    drawTexturedRect(r - rs, b - bs, rt - rs, bt - bs, rs, bs) // Bottom right
                }

                // Top and bottom edge
                run {
                    var x0 = l + ls
                    val xMax = r - rs
                    while (x0 < xMax) {
                        val w = (xMax - x0).coerceAtMost(ws)
                        drawTexturedRect(x0, t, lt + ls, tt, w, ts) // Top
                        drawTexturedRect(x0, b - bs, lt + ls, bt - bs, w, bs) // Bottom
                        x0 += w
                    }
                }

                // Left and right edge
                run {
                    var y0 = t + ts
                    val yMax = b - bs
                    while (y0 < yMax) {
                        val h = (yMax - y0).coerceAtMost(hs)
                        drawTexturedRect(l, y0, lt, tt + ts, ls, h) // Left
                        drawTexturedRect(r - rs, y0, rt - rs, tt + ts, rs, h) // Right
                        y0 += h
                    }
                }

                // Center
                run {
                    var x0 = l + ls
                    val xMax = r - rs
                    while (x0 < xMax) {
                        val w = (xMax - x0).coerceAtMost(ws)

                        var y0 = t + ts
                        val yMax = b - bs
                        while (y0 < yMax) {
                            val h = (yMax - y0).coerceAtMost(hs)
                            drawTexturedRect(x0, y0, lt + ls, tt + ts, w, h)
                            y0 += h
                        }

                        x0 += w
                    }
                }
            }

            buffer.drawDirect()

            oldBlendState.activate()
        }
    }

    data class TextureData(
        val leftTexture: Double,
        val topTexture: Double,
        val rightTexture: Double,
        val bottomTexture: Double,
        val leftSlice: Double,
        val topSlice: Double,
        val rightSlice: Double,
        val bottomSlice: Double,
        val textureWidth: Int = 256,
        val textureHeight: Int = 256,
    ) {
        val lt = leftTexture
        val tt = topTexture
        val rt = rightTexture
        val bt = bottomTexture
        val wt = rightTexture - leftTexture
        val ht = bottomTexture - topTexture
        val ls = leftSlice
        val ts = topSlice
        val rs = rightSlice
        val bs = bottomSlice
        val ws = wt - leftSlice - rightSlice
        val hs = ht - leftSlice - bottomSlice

        fun offset(x: Int, y: Int) = offset(x.toDouble(), y.toDouble())

        fun offset(x: Double, y: Double) = TextureData(
            leftTexture + x,
            topTexture + y,
            rightTexture + x,
            bottomTexture + y,
            leftSlice,
            topSlice,
            rightSlice,
            bottomSlice,
            textureWidth,
            textureHeight
        )

        companion object {
            fun ofSize(
                width: Int,
                height: Int,
                border: Int,
            ) = ofSize(width, height, border, border, border, border)

            fun ofSize(
                width: Int,
                height: Int,
                leftSlice: Int,
                topSlice: Int,
                rightSlice: Int,
                bottomSlice: Int,
            ) = ofSize(
                width.toDouble(),
                height.toDouble(),
                leftSlice.toDouble(),
                topSlice.toDouble(),
                rightSlice.toDouble(),
                bottomSlice.toDouble()
            )

            fun ofSize(
                width: Double,
                height: Double,
                leftSlice: Double,
                topSlice: Double,
                rightSlice: Double,
                bottomSlice: Double,
            ) = TextureData(
                0.0,
                0.0,
                width,
                height,
                leftSlice,
                topSlice,
                rightSlice,
                bottomSlice
            )
        }
    }
}