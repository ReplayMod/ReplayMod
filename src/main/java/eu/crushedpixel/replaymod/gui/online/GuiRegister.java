package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.api.client.ApiException;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.PasswordTextField;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.utils.EmailAddressUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiRegister extends GuiScreen {
    private String message = null;

    private GuiTextField usernameInput, mailInput;
    private PasswordTextField passwordInput, passwordConfirmation;
    private GuiButton registerButton, cancelButton;

    private boolean initialized = false;

    private int strwidth = 0;

    private String[] labels = new String[]{I18n.format("replaymod.gui.username"), I18n.format("replaymod.gui.mail"), I18n.format("replaymod.gui.password"),
            I18n.format("replaymod.gui.register.confirmpw")};

    private List<GuiTextField> inputFields;

    private GuiLoginPrompt parent = null;

    public GuiRegister(GuiLoginPrompt parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        if(!initialized) {
            usernameInput = new GuiTextField(GuiConstants.REGISTER_USERNAME_FIELD, fontRendererObj, this.width / 2 - 45, 30, 145, 20);
            mailInput = new GuiTextField(GuiConstants.REGISTER_MAIL_FIELD, fontRendererObj, this.width / 2 - 45, 60, 145, 20);
            passwordInput = new PasswordTextField(GuiConstants.REGISTER_PASSWORD_FIELD, fontRendererObj, this.width / 2 - 45, 90, 145, 20);
            passwordConfirmation = new PasswordTextField(GuiConstants.REGISTER_PASSWORD_CONFIRM_FIELD, fontRendererObj, this.width / 2 - 45, 120, 145, 20);

            registerButton = new GuiButton(GuiConstants.REGISTER_OKAY_BUTTON, this.width / 2 - 150 - 2, 170, I18n.format("replaymod.gui.register"));
            registerButton.enabled = false;
            registerButton.width = 150;

            cancelButton = new GuiButton(GuiConstants.REGISTER_CANCEL_BUTTON, this.width / 2 + 2, 170, I18n.format("replaymod.gui.cancel"));
            cancelButton.width = 150;
        } else {
            usernameInput.xPosition = mailInput.xPosition = passwordInput.xPosition = passwordConfirmation.xPosition = this.width / 2 - 45;

            registerButton.xPosition = this.width / 2 - 150 - 2;
            cancelButton.xPosition = this.width / 2 + 2;
        }

        inputFields = new ArrayList<GuiTextField>();
        inputFields.add(usernameInput);
        inputFields.add(mailInput);
        inputFields.add(passwordInput);
        inputFields.add(passwordConfirmation);

        buttonList.add(registerButton);
        buttonList.add(cancelButton);

        strwidth = Math.max(Math.max(fontRendererObj.getStringWidth(labels[0]), fontRendererObj.getStringWidth(labels[1])),
                Math.max(fontRendererObj.getStringWidth(labels[2]), fontRendererObj.getStringWidth(labels[3])));

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        for(GuiTextField f : inputFields) {
            f.drawTextBox();
        }

        int i = 0;
        for(String label : labels) {
            drawString(fontRendererObj, label, this.width / 2 - (45 + 10 + strwidth), 37+(i*30), Color.WHITE.getRGB());
            i++;
        }

        if(message != null) {
            drawCenteredString(fontRendererObj, message, this.width / 2, 152, Color.RED.getRGB());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for(GuiTextField f : inputFields) {
            f.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        for(GuiTextField f : inputFields) {
            f.updateCursorCounter();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(keyCode == Keyboard.KEY_TAB) {
            int i = 0;
            for(GuiTextField f : inputFields) {
                if(f.isFocused()) {
                    i = inputFields.indexOf(f);
                    f.setFocused(false);
                }
            }

            if(i == inputFields.size()-1) {
                i = 0;
            } else {
                i++;
            }

            inputFields.get(i).setFocused(true);
        } else if(keyCode == Keyboard.KEY_RETURN) {
            actionPerformed(registerButton);
            return;
        }

        for(GuiTextField f : inputFields) {
            if(f.isFocused()) f.textboxKeyTyped(typedChar, keyCode);
        }

        registerButton.enabled = usernameInput.getText().length() >= 5 && EmailAddressUtils.isValidEmailAddress(mailInput.getText())
                && passwordInput.getText().length() >= 5 && passwordConfirmation.getText().equals(passwordInput.getText());

        //only show message if user has filled out every text field
        if(!registerButton.enabled && usernameInput.getText().length() > 0 && mailInput.getText().length() > 0
                && passwordInput.getText().length() > 0 && passwordConfirmation.getText().length() > 0) {
            if(usernameInput.getText().length() < 5) {
                message = I18n.format("replaymod.gui.register.error.shortusername");
            } else if(!EmailAddressUtils.isValidEmailAddress(mailInput.getText())) {
                message = I18n.format("replaymod.gui.register.error.invalidmail");
            } else if(!(passwordInput.getText().length() >= 5)) {
                message = I18n.format("replaymod.gui.register.error.shortpw");
            } else if(!passwordConfirmation.getText().equals(passwordInput.getText())) {
                message = I18n.format("replaymod.gui.register.error.nomatch");
            }
        } else {
            message = null;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(!button.enabled) return;
        switch(button.id) {
            case GuiConstants.REGISTER_CANCEL_BUTTON:
                mc.displayGuiScreen(parent);
                break;
            case GuiConstants.REGISTER_OKAY_BUTTON:
                message = I18n.format("replaymod.gui.login.logging");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AuthenticationHandler.register(usernameInput.getText().trim(),
                                    mailInput.getText().trim(), passwordInput.getText());

                            mc.addScheduledTask(new Runnable() {
                                @Override
                                public void run() {
                                    mc.displayGuiScreen(parent.getSuccessScreen());
                                }
                            });
                        } catch(ApiException ae) {
                            message = ae.getLocalizedMessage();
                        } catch(Exception e) {
                            message = I18n.format("replaymod.gui.login.connectionerror");
                        }
                    }
                }).start();
                break;
        }
    }
}
