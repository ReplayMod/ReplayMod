package com.replaymod.core.gui.common.input

import com.replaymod.core.gui.common.elementa.UITextInput
import com.udojava.evalex.Expression
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.toConstraint
import java.awt.Color
import java.math.MathContext

class UIExpressionInput : UITextInput() {
    val value: Double?
        get() = try {
            expression.eval()?.toDouble()
        } catch (e: Expression.ExpressionException) {
            null
        } catch (e: ArithmeticException) {
            null
        } catch (e:  NumberFormatException) {
            null
        }

    val expression: Expression
        get() = Expression(getText(), MathContext.DECIMAL64)

    val valid: State<Boolean> = BasicState(false)

    init {
        constrain {
            color = valid.map { if (it) Color.WHITE else Color.RED }.toConstraint()
        }

        onUpdate {
            valid.set(value != null)
        }
    }

    override fun afterInitialization() {
        // Right-align
        constrain {
            x = 2.pixels(alignOpposite = true)
        }

        super.afterInitialization()
    }

    companion object {
        val expressionChars = "+-*/%".toSet()
    }
}