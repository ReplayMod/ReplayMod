package com.replaymod.simplepathing.gui;

import com.udojava.evalex.Expression;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;

import java.math.BigDecimal;

public class GuiExpressionField extends GuiTextField {

    private Boolean expressionValid = null;
    private int precision = 20;

    @Override
    protected void onTextChanged(String from) {
        verify();
        super.onTextChanged(from);

    }

    private boolean verify(){
        try{
            getExpression().setPrecision(precision).eval();
            setTextColor(ReadableColor.WHITE);
            return expressionValid = true;
        } catch (Expression.ExpressionException | ArithmeticException | NumberFormatException e){
            setTextColor(ReadableColor.RED);
            return expressionValid = false;
        }
    }

    public boolean isExpressionValid(){
        if(expressionValid == null){
            verify();
        }
        return expressionValid;
    }

    public GuiExpressionField setPrecision(int precision) {
        this.precision = precision;
        return this;
    }

    public Expression getExpression() {
        return new Expression(getText());
    }

    public BigDecimal getBigDecimal() throws Expression.ExpressionException, ArithmeticException, NumberFormatException {
        return getExpression().setPrecision(precision).eval();
    }

    public long getLong() throws Expression.ExpressionException, ArithmeticException, NumberFormatException  {
        return  getBigDecimal().longValueExact();
    }

    public double getDouble() throws Expression.ExpressionException, ArithmeticException, NumberFormatException  {
        return  getBigDecimal().doubleValue();
    }

    public float getFloat() throws Expression.ExpressionException, ArithmeticException, NumberFormatException  {
        return  getBigDecimal().floatValue();
    }

    public int getInt() throws Expression.ExpressionException, ArithmeticException, NumberFormatException  {
        return  getBigDecimal().intValue();
    }

    @Override
    public GuiExpressionField setSize(int width, int height){
        return (GuiExpressionField) super.setSize(width,height);
    }

    @Override
    public GuiExpressionField onTextChanged(Consumer<String> textChanged) {
        return (GuiExpressionField) super.onTextChanged(textChanged);
    }
}
