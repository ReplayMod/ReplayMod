package eu.crushedpixel.replaymod.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class PasswordTextField extends GuiTextField {

    public PasswordTextField(int p_i45542_1_, FontRenderer p_i45542_2_,
                             int p_i45542_3_, int p_i45542_4_, int p_i45542_5_, int p_i45542_6_) {
        super(p_i45542_1_, p_i45542_2_, p_i45542_3_, p_i45542_4_, p_i45542_5_,
                p_i45542_6_);
    }

    @Override
    public void drawTextBox() {
        String prev = getText();

        String pw = "";
        for(int i = 0; i < prev.length(); i++) {
            pw += "*";
        }

        text = pw;
        super.drawTextBox();
        text = prev;
    }


}
