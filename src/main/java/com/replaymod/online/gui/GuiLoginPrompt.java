package com.replaymod.online.gui;

import com.replaymod.online.api.ApiClient;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiPasswordField;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import eu.crushedpixel.replaymod.gui.GuiConstants;

public class GuiLoginPrompt extends AbstractGuiScreen<GuiLoginPrompt> {

    private final GuiScreen parent, successScreen;
    private final ApiClient apiClient;

    private GuiLabel usernameLabel = new GuiLabel(this).setI18nText("replaymod.gui.username");
    private GuiLabel passwordLabel = new GuiLabel(this).setI18nText("replaymod.gui.password");
    private GuiLabel noAccountLabel = new GuiLabel(this).setI18nText("replaymod.gui.login.noacc");
    private GuiLabel statusLabel = new GuiLabel(this);
    private GuiButton loginButton = new GuiButton(this).setI18nLabel("replaymod.gui.login").setSize(150, 20).setEnabled(false);
    private GuiButton cancelButton = new GuiButton(this).setI18nLabel("replaymod.gui.cancel").setSize(150, 20);
    private GuiButton registerButton = new GuiButton(this).setI18nLabel("replaymod.gui.register").setSize(150, 20);
    private GuiTextField username = new GuiTextField(this).setSize(145, 20).setMaxLength(16).setFocused(true);
    private GuiPasswordField password = new GuiPasswordField(this).setSize(145, 20).setNext(username).setPrevious(username)
            .setMaxLength(GuiConstants.MAX_PW_LENGTH);

    {
        Runnable doLogin = new Runnable() {
            @Override
            public void run() {
                if (!loginButton.isEnabled()) return;
                statusLabel.setI18nText("replaymod.gui.login.logging");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        switch (apiClient.login(username.getText(), password.getText())) {
                            case SUCCESS:
                                statusLabel.setText("");
                                getMinecraft().addScheduledTask(new Runnable() {
                                    @Override
                                    public void run() {
                                        successScreen.display();
                                    }
                                });
                                break;
                            case INVALID_DATA:
                                statusLabel.setI18nText("replaymod.gui.login.incorrect");
                                break;
                            case IO_ERROR:
                                statusLabel.setI18nText("replaymod.gui.login.connectionerror");
                                break;
                        }
                    }
                }, "replaymod-auth").start();
            }
        };
        username.onEnter(doLogin);
        password.onEnter(doLogin);
        loginButton.onClick(doLogin);
        cancelButton.onClick(new Runnable() {
            @Override
            public void run() {
                successScreen.display();
            }
        });
        registerButton.onClick(new Runnable() {
            @Override
            public void run() {
                new GuiRegister(apiClient, GuiLoginPrompt.this).display();
            }
        });
        Runnable contentValidation = new Runnable() {
            @Override
            public void run() {
                loginButton.setEnabled(!username.getText().isEmpty() && !password.getText().isEmpty());
            }
        };
        username.onTextChanged(contentValidation);
        password.onTextChanged(contentValidation);
    }

    public GuiLoginPrompt(ApiClient apiClient, GuiScreen parent, GuiScreen successScreen, boolean manuallyTriggered) {
        this.apiClient = apiClient;
        this.parent = parent;
        this.successScreen = successScreen;

        //if the login prompt was opened automatically (on mod startup), show a "skip" instead of "cancel" button
        if(!manuallyTriggered) {
            cancelButton.setI18nLabel("replaymod.gui.login.skip");
        }

        setLayout(new CustomLayout<GuiLoginPrompt>() {
            @Override
            protected void layout(GuiLoginPrompt container, int width, int height) {
                pos(username, width / 2 - 45, 30);
                pos(password, width / 2 - 45, 60);
                pos(loginButton, width / 2 - 152, 110);
                pos(cancelButton, width / 2 + 2, 110);

                pos(usernameLabel, x(username) - 10 - usernameLabel.getMinSize().getWidth(), y(username) + 7);
                pos(passwordLabel, x(password) - 10 - passwordLabel.getMinSize().getWidth(), y(password) + 7);
                pos(statusLabel, width / 2 - statusLabel.getMinSize().getWidth() / 2, 92);

                int labelWidth = noAccountLabel.getMinSize().getWidth();
                int buttonWidth = registerButton.getMaxSize().getWidth();
                int lineWidth = labelWidth + 5 + buttonWidth;
                int lineStart = width / 2 - lineWidth / 2;
                pos(noAccountLabel, lineStart, height - 22);
                pos(registerButton, lineStart + labelWidth + 5, height - 30);
            }
        });

        setTitle(new GuiLabel().setI18nText("replaymod.gui.login.title"));
    }

    public GuiScreen getSuccessScreen() {
        return successScreen;
    }

    @Override
    protected GuiLoginPrompt getThis() {
        return this;
    }
}
