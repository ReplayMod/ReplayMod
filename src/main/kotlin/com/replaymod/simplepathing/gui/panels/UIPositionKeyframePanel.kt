package com.replaymod.simplepathing.gui.panels

import com.replaymod.core.gui.common.input.*
import com.replaymod.core.gui.utils.Axis
import com.replaymod.core.gui.utils.Resources
import com.replaymod.core.gui.utils.hiddenChildOf
import com.replaymod.core.gui.utils.onSetValueAndNow
import com.replaymod.core.utils.i18n
import com.replaymod.core.utils.transpose
import com.replaymod.replay.gui.overlay.panels.UIToggleablePanel
import com.replaymod.replaystudio.pathing.change.Change
import com.replaymod.replaystudio.pathing.change.CombinedChange
import com.replaymod.simplepathing.SPTimeline
import com.replaymod.simplepathing.gui.KeyframeState
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import java.util.*
import kotlin.math.*
import kotlin.time.Duration

class UIPositionKeyframePanel(
    val state: KeyframeState,
) : UIToggleablePanel() {
    init {
        toggleButton.texture(Resources.icon("pos_keyframe_panel"))

        constrain {
            width = 230.pixels
            height = 114.pixels
        }
    }

    val translateContainer by UIContainer().constrain {
        width = 128.pixels
        height = ChildBasedSizeConstraint()
    } childOf this

    val translateLabel by UIContainer().constrain {
        width = 100.percent
        height = 20.pixels
    }.addChild(UIText("Translate".i18n()).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
    }) childOf translateContainer

    val translateXYZ by UIContainer().constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = ChildBasedSizeConstraint()
    } childOf translateContainer

    val translateX by FieldComponent(Axis.X) childOf translateXYZ
    val translateY by FieldComponent(Axis.Y) childOf translateXYZ
    val translateZ by FieldComponent(Axis.Z) childOf translateXYZ

    val rotateContainer by UIContainer().constrain {
        x = SiblingConstraint()
        width = 102.pixels
        height = ChildBasedSizeConstraint()
    } childOf this

    val rotateLabel by UIContainer().constrain {
        width = 100.percent
        height = 20.pixels
    }.addChild(UIText("Rotate".i18n()).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
    }) childOf rotateContainer

    val rotateXYZ by UIContainer().constrain {
        y = SiblingConstraint()
        width = 100.percent
        height = ChildBasedSizeConstraint()
    } childOf rotateContainer

    val rotateX by FieldComponent(Axis.X) childOf rotateXYZ
    val rotateY by FieldComponent(Axis.Y) childOf rotateXYZ
    val rotateZ by FieldComponent(Axis.Z) childOf rotateXYZ

    val timeField by UIInputOrExpressionField.forTimeInput().constrain {
        x = 0.pixels boundTo rotateZ.input.integer
        y = SiblingConstraint(4f)
        width = 60.pixels
        height = 20.pixels
    }.apply {
        input.onActivate { apply() }
    } childOf this

    val timeLabel by UIContainer().constrain {
        x = 5.pixels(alignOutside = true) boundTo timeField
        y = 0.pixels boundTo timeField
        width = ChildBasedSizeConstraint()
        height = CopyConstraintFloat() boundTo timeField
    }.addChild(UIText("Video Time (min:sec:ms)".i18n()).constrain {
        y = CenterConstraint()
    }) childOf this

    init {
        state.selectedPositionKeyframes.map { it.entries.firstOrNull() }.onSetValueAndNow { selection ->
            val (time, keyframe) = selection?.toPair().transpose()

            timeField.input.value = time ?: Duration.ZERO
            (keyframe?.position ?: Triple(0.0, 0.0, 0.0)).let { (x, y, z) ->
                translateX.value = x
                translateY.value = y
                translateZ.value = z
            }
            (keyframe?.rotation ?: Triple(0.0, 0.0, 0.0)).let { (x, y, z) ->
                rotateX.value = x
                rotateY.value = y
                rotateZ.value = z
            }
        }
    }

    private fun apply() {
        val time = state.selectedPositionKeyframes.get().keys.firstOrNull() ?: return

        val timeline = state.mod.currentTimeline
        var change: Change = timeline.updatePositionKeyframe(
            time.inWholeMilliseconds,
            translateX.value, translateY.value, translateZ.value,
            rotateX.value.toFloat(), rotateY.value.toFloat(), rotateZ.value.toFloat(),
        )

        val newTime = timeField.input.value
        if (newTime != time) {
            change = CombinedChange.createFromApplied(
                change,
                timeline.moveKeyframe(SPTimeline.SPPath.POSITION, time.inWholeMilliseconds, newTime.inWholeMilliseconds)
            )
            state.mod.setSelected(SPTimeline.SPPath.POSITION, newTime.inWholeMilliseconds)
        }
        timeline.timeline.pushChange(change)
    }

    inner class FieldComponent(axis: Axis) : UIContainer() {
        init {
            constrain {
                y = SiblingConstraint(4f)
                width = 100.percent
                height = 20.pixels
            }
        }

        val label by UIContainer().constrain {
            width = 16.pixels
            height = 100.percent
        }.addChild(UIText(axis.toString()).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            color = axis.color.toConstraint()
        }) childOf this

        val input by DecimalInputField().constrain {
            x = SiblingConstraint()
            width = FillConstraint() - 6.pixels
        } childOf this

        init {
            input.onActivate { apply() }
        }

        var value by input::value
    }

    class DecimalInputField(
        fractionalDigits: Int = 5,
    ) : UIContainer() {
        private val fractionMultiplier = (10.0).pow(fractionalDigits)

        init {
            constrain {
                width = 100.percent
                height = 100.percent
            }
        }

        val expression by UIInputField(UIExpressionInput()).constrain {
            width = 100.percent
            height = 100.percent
        } hiddenChildOf this

        val decimal by UIContainer().constrain {
            width = 100.percent
            height = 100.percent
        } childOf this

        val integer by UIInputField(UIIntegerInput()).constrain {
            width = FillConstraint()
        } childOf decimal

        val dot by UIContainer().constrain {
            x = SiblingConstraint()
            width = 5.pixels
            height = 100.percent
        }.addChild(UIText(".", shadow = false).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        }) childOf decimal

        val fraction by UIInputField(UIIntegerInput(fixedDigits = fractionalDigits)).constrain {
            x = SiblingConstraint()
            width = 37.pixels
        } childOf decimal

        init {
            val maybeSwitchToExpression: UIComponent.(typedChar: Char, keyCode: Int) -> Unit = { typedChar, keyCode ->
                this as UIIntegerInput
                if (typedChar in UIExpressionInput.expressionChars && (typedChar != '-' || !isCursorAtAbsoluteStart)) {
                    decimal.hide(instantly = true)
                    expression.unhide()

                    with(expression.input) {
                        setText(integer.input.getText() + "." + fraction.input.getText())
                        grabWindowFocus()
                        setActive(true)
                        keyTypedListeners.forEach { it(typedChar, keyCode) }
                    }
                }
            }
            integer.input.keyTypedListeners.add(0, maybeSwitchToExpression)
            fraction.input.keyTypedListeners.add(0, maybeSwitchToExpression)
            
            expression.input.onActivate {
                // Re-assigning the value will switch back to integer+fraction fields
                value = expression.input.value ?: return@onActivate

                // Forward the activation
                integer.input.activateAction("")
            }
            expression.input.onFocusLost {
                // Re-assigning the value will switch back to integer+fraction fields
                value = value
            }
        }

        var value: Double
            get() {
                val int = integer.input.value
                return int + (int.sign * fraction.input.value / fractionMultiplier)
            }
            set(value) {
                integer.input.value = value.toInt()
                fraction.input.value = (abs(value - value.toInt()) * fractionMultiplier).roundToInt()

                expression.hide(instantly = true)
                decimal.unhide()
                if (Window.ofOrNull(expression) != null && expression.input.hasFocus()) {
                    integer.input.grabWindowFocus()
                }
            }

        fun onActivate(listener: () -> Unit) {
            integer.input.onActivate { listener() }
            fraction.input.onActivate { listener() }
        }
    }
}