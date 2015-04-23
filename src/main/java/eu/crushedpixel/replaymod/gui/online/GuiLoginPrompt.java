package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.PasswordTextField;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
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
    private int lastMouseX, lastMouseY;
    private float lastPartialTicks;

    public GuiLoginPrompt(GuiScreen parent, GuiScreen successScreen) {
        this.parent = parent;
        this.successScreen = successScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        username = new GuiTextField(GuiConstants.REPLAY_CENTER_LOGIN_TEXT_ID, fontRendererObj, this.width / 2 - 45, 30, 145, 20);
        username.setEnabled(true);
        username.setFocused(true);

        password = new PasswordTextField(GuiConstants.REPLAY_CENTER_PASSWORD_TEXT_ID, fontRendererObj, this.width / 2 - 45, 60, 145, 20);
        password.setEnabled(true);
        password.setFocused(false);

        loginButton = new GuiButton(GuiConstants.LOGIN_OKAY_BUTTON, this.width / 2 - 150 - 2, 110, "Login");
        loginButton.width = 150;
        loginButton.enabled = false;
        buttonList.add(loginButton);

        cancelButton = new GuiButton(GuiConstants.LOGIN_CANCEL_BUTTON, this.width / 2 + 2, 110, "Cancel");
        cancelButton.width = 150;
        buttonList.add(cancelButton);
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
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastPartialTicks = partialTicks;

        this.drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Login to ReplayMod.com", this.width / 2, 10, Color.WHITE.getRGB());

        drawString(fontRendererObj, "Username", this.width / 2 - 100, 37, Color.WHITE.getRGB());
        username.drawTextBox();

        drawString(fontRendererObj, "Password", this.width / 2 - 100, 67, Color.WHITE.getRGB());
        password.drawTextBox();

        switch(textState) {
            case INVALID_LOGIN:
                drawCenteredString(fontRendererObj, "Incorrect username or password.", this.width / 2, 92, Color.RED.getRGB());
                break;
            case LOGGING_IN:
                drawCenteredString(fontRendererObj, "Logging in...", this.width / 2, 92, Color.WHITE.getRGB());
                break;
            case NO_CONNECTION:
                drawCenteredString(fontRendererObj, "Could not connect to ReplayMod.com", this.width / 2, 92, Color.RED.getRGB());
                break;
        }
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
        }
        if(keyCode == 28) { //Enter key
            actionPerformed(loginButton);
            return;
        }
        if(username.isFocused()) {
            username.textboxKeyTyped(typedChar, keyCode);
        } else if(password.isFocused()) {
            password.textboxKeyTyped(typedChar, keyCode);
        }
        loginButton.enabled = username.getText().length() > 0 && password.getText().length() > 0;
    }
}
