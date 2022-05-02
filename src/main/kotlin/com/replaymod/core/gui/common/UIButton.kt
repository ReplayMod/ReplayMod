package com.replaymod.core.gui.common

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import gg.essential.elementa.utils.invisible
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.util.Identifier
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture

class UIButton : UIComponent() {

    private val hovered = BasicState(false)
    private val enabled = BasicState(true)

    private val background = enabled.zip(hovered).map { (enabled, hovered) ->
        val offset = when {
            !enabled -> 0
            hovered -> 2
            else -> 1
        }
        UITexture.TextureData.ofSize(0, 46 + offset * 20, 200, 20)
    }

    init {
        onMouseEnter { hovered.set(true) }
        onMouseLeave { hovered.set(false) }

        onMouseClick {
            playClickSound()
        }

        constrain {
            width = 200.pixels
            height = 20.pixels
            color = Color.WHITE.invisible().toConstraint()
        }
    }

    fun label(text: String) = label {
        bindText(BasicState(text))
    }

    fun label(configure: UIText.() -> Unit) = apply {
        val component = UIText() childOf this
        component.constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            color = enabled.zip(hovered).map { (enabled, hovered) -> when {
                !enabled -> Color(160, 160, 160)
                hovered -> Color(255, 255, 160)
                else -> Color(224, 224, 224)
            } }.toConstraint()
        }
        component.configure()
    }

    fun image(imageFuture: CompletableFuture<BufferedImage>, configure: UIImage.() -> Unit = {}) = apply {
        val component = UIImage(imageFuture) childOf this
        component.constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 100.percent
            height = 100.percent
        }
        component.configure()
    }

    fun texture(
        texture: Identifier,
        data: UITexture.TextureData = UITexture.TextureData.full(),
        configure: UITexture.() -> Unit = {},
    ) = texture(texture, BasicState(data), configure)

    fun texture(texture: Identifier, data: State<UITexture.TextureData>, configure: UITexture.() -> Unit = {}) = apply {
        val component = UITexture(texture, data) childOf this
        component.constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 100.percent
            height = 100.percent
        }
        component.configure()
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)

        val left = getLeft().toDouble()
        val right = getRight().toDouble()
        val top = getTop().toDouble()
        val bottom = getBottom().toDouble()

        UGraphics.bindTexture(0, WIDGETS_TEXTURE)
        UI4Slice.draw(matrixStack, left, right, top, bottom, Color.WHITE, background.get())

        UIBlock.drawBlock(matrixStack, getColor(), left + 1, top + 1, right - 1, bottom - 1)

        super.draw(matrixStack)
    }

    companion object {
        //#if MC>=10900
        private val BUTTON_SOUND = net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK
        //#else
        //$$ private val BUTTON_SOUND = ResourceLocation("gui.button.press")
        //#endif

        val WIDGETS_TEXTURE = Identifier("textures/gui/widgets.png")

        fun playClickSound() {
            val mc = MinecraftClient.getInstance()
            //#if MC>=11400
            mc.soundManager.play(PositionedSoundInstance.master(BUTTON_SOUND, 1.0f))
            //#elseif MC>=10904
            //$$ mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(BUTTON_SOUND, 1.0F));
            //#elseif MC>=10800
            //$$ mc.getSoundHandler().playSound(PositionedSoundRecord.create(BUTTON_SOUND, 1.0F));
            //#else
            //$$ mc.getSoundHandler().playSound(PositionedSoundRecord.createPositionedSoundRecord(BUTTON_SOUND, 1.0F));
            //#endif
        }
    }
}