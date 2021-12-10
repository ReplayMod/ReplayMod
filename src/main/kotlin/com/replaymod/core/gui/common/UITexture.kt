package com.replaymod.core.gui.common

import gg.essential.elementa.UIComponent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.shader.BlendState
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import java.awt.Color

class UITexture(
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

            val buffer = UGraphics.getFromTessellator()
            buffer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE)
            with(data) {
                with(color) {
                    fun UGraphics.texS(u: Double, v: Double) = tex(u / textureWidth, v / textureHeight)

                    buffer.pos(matrixStack, l, b, 0.0)
                        .color(red, green, blue, alpha)
                        .texS(lt, bt)
                        .endVertex()
                    buffer.pos(matrixStack, r, b, 0.0)
                        .color(red, green, blue, alpha)
                        .texS(rt, bt)
                        .endVertex()
                    buffer.pos(matrixStack, r, t, 0.0)
                        .color(red, green, blue, alpha)
                        .texS(rt, tt)
                        .endVertex()
                    buffer.pos(matrixStack, l, t, 0.0)
                        .color(red, green, blue, alpha)
                        .texS(lt, tt)
                        .endVertex()
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
        val textureWidth: Int = 256,
        val textureHeight: Int = 256,
    ) {
        val lt = leftTexture
        val tt = topTexture
        val rt = rightTexture
        val bt = bottomTexture

        fun offset(x: Int, y: Int) = offset(x.toDouble(), y.toDouble())

        fun offset(x: Double, y: Double) = TextureData(
            leftTexture + x,
            topTexture + y,
            rightTexture + x,
            bottomTexture + y,
            textureWidth,
            textureHeight
        )

        companion object {
            fun full() = ofSize(256, 256)

            fun ofSize(width: Int, height: Int) = ofSize(0, 0, width, height)

            fun ofSize(width: Double, height: Double) = ofSize(0.0, 0.0, width, height)

            fun ofSize(left: Int, top: Int, width: Int, height: Int) =
                ofSize(left.toDouble(), top.toDouble(), width.toDouble(), height.toDouble())

            fun ofSize(left: Double, top: Double, width: Double, height: Double) =
                TextureData(left, top, left + width, top + height)
        }
    }
}