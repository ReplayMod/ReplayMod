package com.replaymod.online.gui;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.ApiException;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.utils.Consumers;
import de.johni0702.minecraft.gui.utils.Utils;
import com.replaymod.core.utils.Patterns;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableColor;

public class GuiRegister extends AbstractGuiScreen<GuiRegister> {
    public static final int MIN_PW_LENGTH = 5;
    public static final int MAX_PW_LENGTH = 1024;

    private final GuiPanel inputs;
    private final GuiTextField usernameInput, mailInput;
    private final GuiPasswordField passwordInput, passwordConfirmation;
    private final GuiButton registerButton = new GuiButton(this).setI18nLabel("replaymod.gui.register").setSize(150, 20).setDisabled();
    private final GuiButton cancelButton = new GuiButton(this).setI18nLabel("replaymod.gui.cancel").setSize(150, 20);
    private final GuiLabel disclaimerLabel = new GuiLabel(this).setI18nText("replaymod.gui.register.disclaimer");
    private final GuiLabel statusLabel = new GuiLabel(this).setColor(ReadableColor.RED);

    {
        inputs = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(10));
        HorizontalLayout.Data data = new HorizontalLayout.Data(0.5);
        inputs.addElements(new VerticalLayout.Data(1),
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(10))
                        .addElements(data, new GuiLabel().setI18nText("replaymod.gui.username"))
                        .addElements(data, usernameInput = new GuiTextField().setMaxLength(16).setSize(145, 20)),
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(10))
                        .addElements(data, new GuiLabel().setI18nText("replaymod.gui.mail"))
                        .addElements(data, mailInput = new GuiTextField().setSize(145, 20)),
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(10))
                        .addElements(data, new GuiLabel().setI18nText("replaymod.gui.password"))
                        .addElements(data, passwordInput = new GuiPasswordField().setMaxLength(MAX_PW_LENGTH+1).setSize(145, 20)),
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(10))
                        .addElements(data, new GuiLabel().setI18nText("replaymod.gui.register.confirmpw"))
                        .addElements(data, passwordConfirmation = new GuiPasswordField().setMaxLength(MAX_PW_LENGTH+1).setSize(145, 20))
        );
        Utils.link(usernameInput, mailInput, passwordInput, passwordConfirmation);

        setLayout(new CustomLayout<GuiRegister>() {
            @Override
            protected void layout(GuiRegister container, int width, int height) {
                pos(inputs, width / 2 - inputs.getMinSize().getWidth() / 2, 30);
                pos(registerButton, width / 2 - 152, 170);
                pos(cancelButton, width / 2 + 2, 170);
                pos(statusLabel, width / 2 - statusLabel.getMinSize().getWidth() / 2, 152);

                FontRenderer font = getMinecraft().fontRenderer;
                int lineCount = font.listFormattedStringToWidth(disclaimerLabel.getText(), width - 10).size();
                Dimension dim = new Dimension(width - 10, font.FONT_HEIGHT * lineCount);
                disclaimerLabel.setSize(dim);
                pos(disclaimerLabel, 5, height - dim.getHeight() - 5);
            }
        });

        setTitle(new GuiLabel().setI18nText("replaymod.gui.register.title"));

        Runnable doRegister = new Runnable() {
            @Override
            public void run() {
                if (!registerButton.isEnabled()) return;

                inputs.forEach(IGuiTextField.class).setDisabled();
                statusLabel.setI18nText("replaymod.gui.login.logging");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String username = usernameInput.getText().trim();
                            String mail = mailInput.getText().trim();
                            String password = passwordInput.getText();
                            apiClient.register(username, mail, password);

                            getMinecraft().addScheduledTask(new Runnable() {
                                @Override
                                public void run() {
                                    parent.getSuccessScreen().display();
                                }
                            });
                        } catch (ApiException ae) {
                            statusLabel.setText(ae.getLocalizedMessage());
                        } catch (AuthenticationException aue) {
                            aue.printStackTrace();
                            statusLabel.setI18nText("replaymod.gui.register.error.authfailed");
                        } catch (Exception e) {
                            e.printStackTrace();
                            statusLabel.setI18nText("replaymod.gui.login.connectionerror");
                        } finally {
                            inputs.forEach(IGuiTextField.class).setEnabled();
                        }
                    }
                }, "replaymod-register").start();
            }
        };
        inputs.forEach(IGuiTextField.class).onEnter(doRegister);
        registerButton.onClick(doRegister);

        Runnable contentValidation = new Runnable() {
            @Override
            public void run() {
                String status = null;
                if (usernameInput.getText().length() < 5) {
                    status = "replaymod.gui.register.error.shortusername";
                } else if(!Patterns.ALPHANUMERIC_UNDERSCORE.matcher(usernameInput.getText().trim()).matches()) {
                    status = "replaymod.gui.register.error.invalidname";
                } else if (passwordInput.getText().length() < MIN_PW_LENGTH) {
                    status = "replaymod.gui.register.error.shortpw";
                } else if (passwordInput.getText().length() > MAX_PW_LENGTH) {
                    status = "replaymod.gui.register.error.longpw";
                } else if (!com.replaymod.core.utils.Utils.isValidEmailAddress(mailInput.getText())) {
                    status = "replaymod.api.invalidmail";
                } else if (!passwordConfirmation.getText().equals(passwordInput.getText())) {
                    status = "replaymod.gui.register.error.nomatch";
                }

                registerButton.setEnabled(status == null);

                if (status != null
                        && !usernameInput.getText().isEmpty()
                        && !mailInput.getText().isEmpty()
                        && !passwordInput.getText().isEmpty()
                        && !passwordConfirmation.getText().isEmpty()) {
                    statusLabel.setI18nText(status);
                } else {
                    statusLabel.setText("");
                }
            }
        };
        inputs.forEach(IGuiTextField.class).onTextChanged(Consumers.from(contentValidation));

        cancelButton.onClick(new Runnable() {
            @Override
            public void run() {
                parent.display();
            }
        });
    }

    private final ApiClient apiClient;
    private final GuiLoginPrompt parent;

    public GuiRegister(ApiClient apiClient, GuiLoginPrompt parent) {
        this.apiClient = apiClient;
        this.parent = parent;
    }

    @Override
    protected GuiRegister getThis() {
        return this;
    }
}
