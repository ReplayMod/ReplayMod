package com.replaymod.core.gui.common

import com.replaymod.core.gui.common.UITexture.TextureData
import gg.essential.elementa.UIComponent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.shader.BlendState
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import java.awt.Color

class UI4Slice(
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

            buffer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE)

            fun UGraphics.texS(u: Double, v: Double) = tex(u / data.textureWidth, v / data.textureHeight)

            fun drawTexturedRect(x: Double, y: Double, u: Double, v: Double, width: Double, height: Double) {
                buffer.pos(matrixStack, x, y + height, 0.0)
                    .color(red, green, blue, alpha)
                    .texS(u, v + height)
                    .endVertex()
                buffer.pos(matrixStack, x + width, y + height, 0.0)
                    .color(red, green, blue, alpha)
                    .texS(u + width, v + height)
                    .endVertex()
                buffer.pos(matrixStack, x + width, y, 0.0)
                    .color(red, green, blue, alpha)
                    .texS(u + width, v)
                    .endVertex()
                buffer.pos(matrixStack, x, y, 0.0)
                    .color(red, green, blue, alpha)
                    .texS(u, v)
                    .endVertex()
            }

            with(data) {
                val wh = (r - l) / 2
                val hh = (b - t) / 2

                drawTexturedRect(l, t, lt, tt, wh, hh)
                drawTexturedRect(l, t + hh, lt, bt - hh, wh, hh)
                drawTexturedRect(l + wh, t, rt - wh, tt, wh, hh)
                drawTexturedRect(l + wh, t + hh, rt - wh, bt - hh, wh, hh)
            }

            buffer.drawDirect()

            oldBlendState.activate()
        }
    }
}