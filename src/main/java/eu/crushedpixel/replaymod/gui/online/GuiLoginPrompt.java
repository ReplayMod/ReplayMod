package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.PasswordTextField;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;

public class GuiLoginPrompt extends GuiScreen {

    private static final int EMPTY = 0;
    private static final int LOGGING_IN = 1;
    private static final int INVALID_LOGIN = 2;
    private static final int NO_CONNECTION = 3;
    private static Minecraft mc = Minecraft.getMinecraft();
    private GuiScreen parent, successScreen;
    private int textState = 0;
    private GuiTextField username;
    private PasswordTextField password;
    private GuiButton loginButton;
    private GuiButton cancelButton;
    private GuiButton registerButton;

    private String noacc;
    private int strwidth;

    private boolean initialized = false;

    public GuiScreen getSuccessScreen() {
        return successScreen;
    }

    public GuiLoginPrompt(GuiScreen parent, GuiScreen successScreen) {
        this.parent = parent;
        this.successScreen = successScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        if(!initialized) {
            username = new GuiTextField(GuiConstants.LOGIN_USERNAME_FIELD, fontRendererObj, this.width / 2 - 45, 30, 145, 20);
            username.setEnabled(true);
            username.setFocused(true);

            password = new PasswordTextField(GuiConstants.LOGIN_PASSWORD_FIELD, fontRendererObj, this.width / 2 - 45, 60, 145, 20);
            password.setEnabled(true);
            password.setFocused(false);

            loginButton = new GuiButton(GuiConstants.LOGIN_OKAY_BUTTON, this.width / 2 - 150 - 2, 110, I18n.format("replaymod.gui.login"));
            loginButton.enabled = false;
            loginButton.width = 150;

            cancelButton = new GuiButton(GuiConstants.LOGIN_CANCEL_BUTTON, this.width / 2 + 2, 110, I18n.format("replaymod.gui.cancel"));
            cancelButton.width = 150;

            registerButton = new GuiButton(GuiConstants.LOGIN_REGISTER_BUTTON, 0, this.height-30, I18n.format("replaymod.gui.register"));
            registerButton.width = 150;
        } else {
            username.xPosition = password.xPosition = this.width / 2 - 45;
            loginButton.xPosition = this.width / 2 - 150 - 2;
            cancelButton.xPosition = this.width / 2 + 2;
            registerButton.yPosition = this.height-30;
        }

        noacc = I18n.format("replaymod.gui.login.noacc");
        strwidth = fontRendererObj.getStringWidth(noacc);

        int tw = 150+5+strwidth;
        registerButton.xPosition = (width/2) - (tw/2) + strwidth+5;

        buttonList.add(loginButton);
        buttonList.add(cancelButton);
        buttonList.add(registerButton);

        strwidth2 = Math.max(fontRendererObj.getStringWidth(usernameLabel), fontRendererObj.getStringWidth(passwordLabel));

        initialized = true;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(button.id == GuiConstants.LOGIN_OKAY_BUTTON) {
            if(button.enabled) {
                //Authenticate
                textState = LOGGING_IN;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        switch(AuthenticationHandler.authenticate(username.getText(), password.getText())) {
                            case AuthenticationHandler.SUCCESS:
                                textState = EMPTY;
                                mc.addScheduledTask(new Runnable() {
                                    @Override
                                    public void run() {
                                        mc.displayGuiScreen(successScreen);
                                    }
                                });
                                break;
                            case AuthenticationHandler.INVALID:
                                textState = INVALID_LOGIN;
                                break;
                            case AuthenticationHandler.NO_CONNECTION:
                                textState = NO_CONNECTION;
                                break;
                        }
                    }
                }).start();
            }
        } else if(button.id == GuiConstants.LOGIN_CANCEL_BUTTON) {
            mc.displayGuiScreen(parent);
        } else if(button.id == GuiConstants.LOGIN_REGISTER_BUTTON) {
            mc.displayGuiScreen(new GuiRegister(this));
        }
    }

    private String usernameLabel = I18n.format("replaymod.gui.username");
    private String passwordLabel = I18n.format("replaymod.gui.password");
    private int strwidth2 = 100;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.login.title"), this.width / 2, 10, Color.WHITE.getRGB());

        drawString(fontRendererObj, usernameLabel, this.width / 2 - (45+10+strwidth2), 37, Color.WHITE.getRGB());
        username.drawTextBox();

        drawString(fontRendererObj, passwordLabel, this.width / 2 - (45+10+strwidth2), 67, Color.WHITE.getRGB());
        password.drawTextBox();

        switch(textState) {
            case INVALID_LOGIN:
                drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.login.incorrect"), this.width / 2, 92, Color.RED.getRGB());
                break;
            case LOGGING_IN:
                drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.login.logging"), this.width / 2, 92, Color.WHITE.getRGB());
                break;
            case NO_CONNECTION:
                drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.login.connectionerror"), this.width / 2, 92, Color.RED.getRGB());
                break;
        }

        int tw = 150+5+strwidth;
        drawString(fontRendererObj, noacc, this.width / 2 - (tw/2), this.height-22, Color.WHITE.getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        username.updateCursorCounter();
        password.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(keyCode == Keyboard.KEY_TAB) {
            if(password.isFocused()) {
                password.setFocused(false);
                username.setFocused(true);
            } else {
                username.setFocused(false);
                password.setFocused(true);
            }
            return;
        } else if(keyCode == Keyboard.KEY_RETURN) {
            actionPerformed(loginButton);
            return;
        }
        if(username.isFocused()) {
            username.textboxKeyTyped(typedChar, keyCode);
        } else if(password.isFocused()) {
            password.textboxKeyTyped(typedChar, keyCode);
        }
        loginButton.enabled = username.getText().length() > 0 && password.getText().length() > 0;

        super.keyTyped(typedChar, keyCode);
    }
}
