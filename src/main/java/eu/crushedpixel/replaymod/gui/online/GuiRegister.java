package eu.crushedpixel.replaymod.gui.online;

import com.mojang.authlib.exceptions.AuthenticationException;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import eu.crushedpixel.replaymod.api.ApiException;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.utils.EmailAddressUtils;
import org.lwjgl.util.ReadableColor;

public class GuiRegister extends AbstractGuiScreen<GuiRegister> {
    private final GuiPanel inputs;
    private final GuiTextField usernameInput, mailInput;
    private final GuiPasswordField passwordInput, passwordConfirmation;
    private final GuiButton registerButton = new GuiButton(this).setI18nLabel("replaymod.gui.register").setWidth(150).setDisabled();
    private final GuiButton cancelButton = new GuiButton(this).setI18nLabel("replaymod.gui.cancel").setWidth(150);
    private final GuiLabel statusLabel = new GuiLabel(this).setColor(ReadableColor.RED);

    {
        inputs = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(10));
        HorizontalLayout.Data data = new HorizontalLayout.Data(0, VerticalLayout.Alignment.CENTER);
        inputs.addElements(new VerticalLayout.Data(0, HorizontalLayout.Alignment.RIGHT),
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(10))
                        .addElements(data, new GuiLabel().setI18nText("replaymod.gui.username"))
                        .addElements(data, usernameInput = new GuiTextField().setMaxLength(16).setSize(145, 20)),
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(10))
                        .addElements(data, new GuiLabel().setI18nText("replaymod.gui.mail"))
                        .addElements(data, mailInput = new GuiTextField().setSize(145, 20)),
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(10))
                        .addElements(data, new GuiLabel().setI18nText("replaymod.gui.password"))
                        .addElements(data, passwordInput = new GuiPasswordField().setMaxLength(32).setSize(145, 20)),
                new GuiPanel().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(10))
                        .addElements(data, new GuiLabel().setI18nText("replaymod.gui.register.confirmpw"))
                        .addElements(data, passwordConfirmation = new GuiPasswordField().setMaxLength(32).setSize(145, 20))
        );
        usernameInput.setNext(mailInput)
                .getNext().setNext(passwordInput)
                .getNext().setNext(passwordConfirmation)
                .getNext().setNext(usernameInput);

        setLayout(new CustomLayout<GuiRegister>() {
            @Override
            protected void layout(GuiRegister container, int width, int height) {
                pos(inputs, width / 2 - inputs.getMinSize().getWidth() / 2, 30);
                pos(registerButton, width / 2 - 152, 170);
                pos(cancelButton, width / 2 + 2, 170);
                pos(statusLabel, width / 2 - statusLabel.getMinSize().getWidth() / 2, 152);
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
                            AuthenticationHandler.register(username, mail, password);

                            getMinecraft().addScheduledTask(new Runnable() {
                                @Override
                                public void run() {
                                    getMinecraft().displayGuiScreen(parent.getSuccessScreen());
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
                } else if (passwordInput.getText().length() < 5) {
                    status = "replaymod.gui.register.error.shortpw";
                } else if (!EmailAddressUtils.isValidEmailAddress(mailInput.getText())) {
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
        inputs.forEach(IGuiTextField.class).onTextChanged(contentValidation);

        cancelButton.onClick(new Runnable() {
            @Override
            public void run() {
                parent.display();
            }
        });
    }

    private GuiLoginPrompt parent;

    public GuiRegister(GuiLoginPrompt parent) {
        this.parent = parent;
    }

    @Override
    protected GuiRegister getThis() {
        return this;
    }
}
