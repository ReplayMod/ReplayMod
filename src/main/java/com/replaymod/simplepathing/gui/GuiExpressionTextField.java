package com.replaymod.simplepathing.gui;

import com.udojava.evalex.Expression;
import de.johni0702.minecraft.gui.element.GuiTextField;

import java.math.BigDecimal;

public class GuiExpressionTextField extends GuiTextField {

    private Boolean verified = null;
    private int precision = 20;

    @Override
    protected void onTextChanged(String from) {
        super.onTextChanged(from);

        verify();

    }

    private void verify(){
        try{
            getExpression().setPrecision(precision).eval();
            verified = true;
        } catch (Expression.ExpressionException | ArithmeticException e){
            verified = false;
        }
    }

    public boolean isVerified(){
        if(verified == null){
            verify();
        }
        return verified;
    }

    public GuiExpressionTextField setPrecision(int precision) {
        this.precision = precision;
        return this;
    }

    public Expression getExpression() {
        return new Expression(getText());
    }

    public BigDecimal getBigDecimal() throws Expression.ExpressionException, ArithmeticException {
        return getExpression().setPrecision(precision).eval();
    }

    public long getLong() throws Expression.ExpressionException, ArithmeticException {
        return  getBigDecimal().longValueExact();
    }

    public double getDouble() throws Expression.ExpressionException, ArithmeticException {
        return  getBigDecimal().doubleValue();
    }

    public float getFloat() throws Expression.ExpressionException, ArithmeticException {
        return  getBigDecimal().floatValue();
    }

    public int getInt() throws Expression.ExpressionException, ArithmeticException {
        return  getBigDecimal().intValue();
    }

    @Override
    public GuiExpressionTextField setSize(int width, int height){
        return (GuiExpressionTextField) super.setSize(width,height);
    }
}
