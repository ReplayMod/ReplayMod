package com.replaymod.simplepathing.gui.panels

import com.replaymod.core.KeyBindingRegistry
import com.replaymod.core.gui.common.UIButton
import com.replaymod.core.gui.common.input.UIInputOrExpressionField
import com.replaymod.core.gui.utils.*
import com.replaymod.core.utils.i18n
import com.replaymod.replay.gui.overlay.panels.UIToggleablePanel
import com.replaymod.simplepathing.gui.KeyframeState
import com.replaymod.simplepathing.gui.movePosKeyframes
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.toConstraint
import gg.essential.elementa.utils.withAlpha
import java.awt.Color
import java.math.BigDecimal

class UIPositionOffsetPanel(
    val window: Window,
    val state: KeyframeState,
) : UIToggleablePanel() {

    init {
        toggleButton.texture(Resources.icon("pos_offset_panel"))

        constrain {
            x = 0.pixels() boundTo toggleButton
            y = 2.pixels(alignOutside = true) boundTo toggleButton
            width = ChildBasedSizeConstraint() + 10.pixels
            height = ChildBasedSizeConstraint() + 10.pixels
        }

        open.onSetValue { if (!it) keyBindsPanel.open.set(false) }
    }

    private val container: UIContainer by UIContainer().constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        width = basicWidthConstraint { topButtons.getWidth() }
        height = ChildBasedSizeConstraint()
    } childOf this

    private val translateContainer by UIContainer().constrain {
        y = SiblingConstraint(3f)
        width = 100.percent
        height = 20.pixels
    } childOf container

    private val rotateContainer by UIContainer().constrain {
        y = SiblingConstraint(3f)
        width = 100.percent
        height = 20.pixels
    } childOf container

    private val topButtons by UIContainer().constrain {
        y = SiblingConstraint(5f)
        width = ChildBasedSizeConstraint()
        height = 20.pixels
    } childOf container

    private val bottomButtons by UIContainer().constrain {
        y = SiblingConstraint(3f)
        width = 100.percent
        height = 20.pixels
    } childOf container

    private val translateField by UIInputOrExpressionField.forDecimalInput().constrain {
        x = 0.pixels(alignOpposite = true)
        width = 60.pixels
        height = 20.pixels
    }.apply {
        input.value = BigDecimal(1)
        input.onActivate { releaseWindowFocus() }
    } childOf translateContainer

    private val translateLabel by UIText("Translate:").constrain {
        x = 1.pixel(alignOutside = true) boundTo translateField
        y = CenterConstraint()
    } childOf translateContainer

    private val rotateField by UIInputOrExpressionField.forDecimalInput().constrain {
        x = 0.pixels(alignOpposite = true)
        width = 60.pixels
        height = 20.pixels
    }.apply {
        input.value = BigDecimal(15)
        input.onActivate { releaseWindowFocus() }
    } childOf rotateContainer

    private val rotateLabel by UIText("Rotate:").constrain {
        x = 1.pixel(alignOutside = true) boundTo rotateField
        y = CenterConstraint()
    } childOf rotateContainer

    private val rotateButton by UIButton().constrain {
        x = SiblingConstraint(padding = 3f)
        width = ChildBasedSizeConstraint() + 10.pixels
    }.label("R. mode").highlightWhenActive(Color.ORANGE, state.mod.keyRotationMode) childOf topButtons

    val plusXButton by OffsetButton(Axis.X, state.mod.keyPlusX) childOf topButtons
    val plusYButton by OffsetButton(Axis.Y, state.mod.keyPlusY) childOf topButtons
    val plusZButton by OffsetButton(Axis.Z, state.mod.keyPlusZ) childOf topButtons

    val minusXButton by OffsetButton(Axis.X, state.mod.keyMinusX, plusXButton) childOf bottomButtons
    val minusYButton by OffsetButton(Axis.Y, state.mod.keyMinusY, plusYButton) childOf bottomButtons
    val minusZButton by OffsetButton(Axis.Z, state.mod.keyMinusZ, plusZButton) childOf bottomButtons

    inner class OffsetButton(
        private val axis: Axis,
        keyBinding: KeyBindingRegistry.Binding,
        private val inverse: OffsetButton? = null,
    ) : UIContainer() {
        init {
            constrain {
                x = if (inverse != null) {
                    0.pixels boundTo inverse.button
                } else {
                    SiblingConstraint(padding = 3f)
                }
                width = 20.pixels
                height = 20.pixels
            }
        }

        val button by UIButton().constrain {
            width = 100.percent
            height = 100.percent
        }.label((if (inverse == null) "+" else "-") + axis).onLeftClick {
            activate()
        }.highlightWhenActive(axis.color, keyBinding) childOf this

        fun activate() {
            val timeline = state.mod.currentTimeline ?: return
            val vector = axis.toVector3f()
            if (inverse != null) vector.scale(-1f)
            val (pos, rot) = if (state.mod.keyRotationMode.isPressed) {
                Vector3f() to vector
            } else {
                vector to Vector3f()
            }
            pos.scale(translateField.input.value.toFloat())
            rot.scale(rotateField.input.value.toFloat())
            val change = state.movePosKeyframes(state.selection.get(), pos, rot)
            timeline.timeline.pushChange(change)
        }
    }

    private val keyBindsPanel by KeyBindsPanel().constrain {
        x = SiblingConstraint(padding = 2f) boundTo this@UIPositionOffsetPanel
        y = 0.pixels(alignOpposite = true) boundTo this@UIPositionOffsetPanel
    }.apply {
        toggleButton.constrain {
            x = 0.pixels
            y = 0.pixels
        } childOf bottomButtons
    } hiddenChildOf window

    private inner class KeyBindsPanel : UIToggleablePanel() {
        init {
            toggleButton.texture(Resources.icon("settings"))

            constrain {
                width = ChildBasedSizeConstraint() + 10.pixels
                height = ChildBasedSizeConstraint() + 10.pixels
            }
        }

        private val container by UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = basicWidthConstraint { it.children.first().getWidth() }
            height = ChildBasedSizeConstraint()
        } childOf this

        init {
            Row(state.mod.keyRotationMode).constrain { width = ChildBasedSizeConstraint(5f) } childOf container
            Row(state.mod.keyPlusX) childOf container
            Row(state.mod.keyMinusX) childOf container
            Row(state.mod.keyPlusY) childOf container
            Row(state.mod.keyMinusY) childOf container
            Row(state.mod.keyPlusZ) childOf container
            Row(state.mod.keyMinusZ) childOf container
        }

        inner class Row(keyBinding: KeyBindingRegistry.Binding) : UIContainer() {
            init {
                constrain {
                    y = SiblingConstraint()
                    width = 100.percent
                    height = 20.pixels
                }
            }

            private val label by UIText().constrain {
                y = CenterConstraint()
            }.setText(keyBinding.name.i18n()) childOf this

            private val button by UIButton().constrain {
                x = 0.pixels(alignOpposite = true)
                width = 80.pixels
            }.label {
                bindText(pollingState { if (keyBinding.isBound) keyBinding.boundKey else "" })
            }.onLeftClick {
                // TODO need to somehow get our hands on the scan code
                /*
                val inputCatcher by UIContainer().constrain {
                    width = 100.percentOfWindow
                    height = 100.percentOfWindow
                } childOf window

                inputCatcher.setFloating(true)
                 */
            } childOf this
        }
    }

    private fun UIButton.highlightWhenActive(highlightColor: Color, keyBinding: KeyBindingRegistry.Binding) = apply {
        val keyPressed = pollingState { keyBinding.isPressed }
        val buttonPressed = BasicState(false)
        val highlighted = keyPressed.zip(buttonPressed).map { (a, b) -> a || b }

        constrain {
            color = highlighted.map { highlightColor.withAlpha(if (it) 0.5f else 0f) }.toConstraint()
        }
        onLeftClick {
            buttonPressed.set(true)
        }
        onMouseRelease {
            buttonPressed.set(false)
        }
    }
}