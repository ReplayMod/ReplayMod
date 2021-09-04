package com.replaymod.core.gui.common.input

import com.replaymod.core.gui.common.elementa.UITextInput
import com.replaymod.core.gui.utils.hiddenChildOf
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.dsl.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class UIInputOrExpressionField<T: UITextInput>(
    field: UIInputField<T>,
    getValue: T.() -> String,
    setValue: T.(value: Double) -> Unit,
    forwardActivation: Boolean = true,
) : UIContainer() {

    val input: T by field::input

    val inputField: UIInputField<T> = field.constrain {
        width = 100.percent
        height = 100.percent
    } childOf this
    
    val expressionField by UIInputField(UIExpressionInput()).constrain {
        width = 100.percent
        height = 100.percent
    } hiddenChildOf this

    init {
        // Switch from regular input to expression when certain keys are pressed (special case negative numbers)
        input.keyTypedListeners.add(0) { typedChar, keyCode ->
            if (typedChar in UIExpressionInput.expressionChars && (typedChar != '-' || !input.isCursorAtAbsoluteStart)) {
                inputField.hide(instantly = true)
                expressionField.unhide()

                with (expressionField.input) {
                    setText(inputField.input.getValue())
                    grabWindowFocus()
                    setActive(true)
                    keyTypedListeners.forEach { it(typedChar, keyCode) }
                }
            }
        }
        
        // Switch from expression to regular input when Enter is pressed (and the expression is valid)
        expressionField.input.onActivate {
            val value = expressionField.input.value ?: return@onActivate
            
            expressionField.hide(instantly = true)
            inputField.unhide()

            with(inputField.input) {
                setValue(value)
                grabWindowFocus()
                if (forwardActivation) {
                    activateAction(getText())
                }
            }
        }

        // Also switch back when the expression field looses focus, but do not apply the result in that case
        expressionField.input.onFocusLost {
            expressionField.hide(instantly = true)
            inputField.unhide()
        }
    }
    
    companion object {
        fun forTimeInput(input: UITimeInput = UITimeInput()) = UIInputOrExpressionField(
            UIInputField(input),
            { "%.3f".format(Locale.ROOT, value.toDouble(TimeUnit.SECONDS)) },
            { value = Duration.seconds(it) },
        )
    }
}