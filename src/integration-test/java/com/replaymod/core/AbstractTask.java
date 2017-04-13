package com.replaymod.core;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import de.johni0702.minecraft.gui.container.AbstractGuiOverlay;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiOverlay;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiTexturedButton;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.utils.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import static com.replaymod.core.ReplayModIntegrationTest.LOGGER;
import static com.replaymod.core.Utils.addCallback;

public abstract class AbstractTask implements Task {
    public static Task create(Consumer<AbstractTask> init) {
        return new AbstractTask() {
            @Override
            protected void init() {
                init.consume(this);
            }
        };
    }

    public static final Minecraft mc = Minecraft.getMinecraft();
    public final ReplayMod core = ReplayMod.instance;
    public SettableFuture<Void> future;

    @Override
    public ListenableFuture<Void> execute() {
        future = SettableFuture.create();

        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        addCallback(future, success -> {
            FMLCommonHandler.instance().bus().unregister(this);
            MinecraftForge.EVENT_BUS.unregister(this);
        }, error -> {});

        init();

        return future;
    }

    protected void init() {}

    protected void runLater(Runnable runnable) {
        core.runLater(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                future.setException(t);
            }
        });
    }

    public void expectGuiClosed(Runnable onClosed) {
        expectGuiClosed0(10, onClosed);
    }

    public void expectGuiClosed(int timeout, Runnable onClosed) {
        expectGuiClosed0(timeout, onClosed);
    }

    private void expectGuiClosed0(int timeout, Runnable onClosed) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        class EventHandler {
            final net.minecraft.client.gui.GuiScreen currentScreen = mc.currentScreen;
            int framesPassed;

            @SubscribeEvent
            public void onGuiOpen(TickEvent.RenderTickEvent event) {
                if (event.phase != TickEvent.Phase.START) return;
                if (currentScreen != mc.currentScreen) {
                    FMLCommonHandler.instance().bus().unregister(this);
                    onClosed.run();
                } else {
                    if (framesPassed < timeout) {
                        framesPassed++;
                    } else {
                        Object gui = (gui = GuiScreen.from(currentScreen)) == null ? currentScreen : gui;
                        Exception e = new TimeoutException("Timeout while waiting for " + gui + " to be closed.");
                        e.setStackTrace(Arrays.copyOfRange(stackTrace, 3, stackTrace.length));
                        future.setException(e);
                    }
                }
            }
        }
        FMLCommonHandler.instance().bus().register(new EventHandler());
    }

    public void expectPopupClosed(Runnable onClosed) {
        expectPopupClosed0(10, onClosed);
    }

    public void expectPopupClosed(int timeout, Runnable onClosed) {
        expectPopupClosed0(timeout, onClosed);
    }

    private void expectPopupClosed0(int timeout, Runnable onClosed) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        AbstractGuiPopup popup = getPopup(mc.currentScreen);
        if (popup == null) {
            throw new IllegalStateException("No popup found.");
        }
        class EventHandler {
            int framesPassed;

            @SubscribeEvent
            public void onGuiOpen(TickEvent.RenderTickEvent event) {
                if (event.phase != TickEvent.Phase.START) return;
                if (getPopup(mc.currentScreen) != popup) {
                    FMLCommonHandler.instance().bus().unregister(this);
                    onClosed.run();
                } else {
                    if (framesPassed < timeout) {
                        framesPassed++;
                    } else {
                        Exception e = new TimeoutException("Timeout while waiting for " + popup + " to be closed.");
                        e.setStackTrace(Arrays.copyOfRange(stackTrace, 3, stackTrace.length));
                        future.setException(e);
                    }
                }
            }
        }
        FMLCommonHandler.instance().bus().register(new EventHandler());
    }

    private AbstractGuiPopup getPopup(net.minecraft.client.gui.GuiScreen minecraft) {
        GuiContainer<?> container = GuiOverlay.from(minecraft);
        if (container == null) {
            container = GuiScreen.from(minecraft);
        }
        if (container != null) {
            while (container.getContainer() != null) {
                container = container.getContainer();
            }
            GuiElement popup = Iterables.getLast(container.getChildren());
            if (popup instanceof AbstractGuiPopup) {
                return (AbstractGuiPopup) popup;
            }
        }
        return null;
    }

    public <T> void expectGui(Class<T> guiClass, Consumer<T> onOpen) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        class EventHandler {
            net.minecraft.client.gui.GuiScreen currentScreen;
            int framesPassed;

            @SubscribeEvent
            public void onGuiOpen(TickEvent.RenderTickEvent event) {
                if (event.phase != TickEvent.Phase.START) return;
                if (currentScreen != mc.currentScreen) {
                    currentScreen = mc.currentScreen;
                    framesPassed = 0;
                }
                if (framesPassed < 10) {
                    framesPassed++;
                    return;
                }

                FMLCommonHandler.instance().bus().unregister(this);

                Object foundGui = null;
                if (AbstractGuiScreen.class.isAssignableFrom(guiClass)) {
                    AbstractGuiScreen guiScreen = GuiScreen.from(currentScreen);
                    if (guiClass.isInstance(guiScreen)) {
                        onOpen.consume(guiClass.cast(guiScreen));
                        return;
                    }
                    foundGui = guiScreen;
                } else if (AbstractGuiOverlay.class.isAssignableFrom(guiClass)) {
                    AbstractGuiOverlay guiScreen = GuiOverlay.from(currentScreen);
                    if (guiClass.isInstance(guiScreen)) {
                        onOpen.consume(guiClass.cast(guiScreen));
                        return;
                    }
                    foundGui = guiScreen;
                } else if (AbstractGuiPopup.class.isAssignableFrom(guiClass)) {
                    AbstractGuiPopup popup = getPopup(currentScreen);
                    if (guiClass.isInstance(popup)) {
                        onOpen.consume(guiClass.cast(popup));
                        return;
                    }
                } else {
                    if (guiClass.isInstance(currentScreen)) {
                        onOpen.consume(guiClass.cast(currentScreen));
                        return;
                    }
                }
                class UnexpectedGuiException extends Exception {
                    UnexpectedGuiException(Object foundGui) {
                        super("Expected instance of " + guiClass + " but found " + foundGui);
                        setStackTrace(Arrays.copyOfRange(stackTrace, 2, stackTrace.length));
                    }
                }
                future.setException(new UnexpectedGuiException(foundGui == null ? currentScreen : foundGui));
            }
        }
        FMLCommonHandler.instance().bus().register(new EventHandler());
    }

    private void clickNow(int x, int y) {
        try {
            Method method = net.minecraft.client.gui.GuiScreen.class
                    .getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
            method.setAccessible(true);
            method.invoke(mc.currentScreen, x, y, 0);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            future.setException(e);
        }
    }

    public void click(int x, int y) {
        runLater(() -> {
            LOGGER.info("Clicking at {}/{}", x, y);
            clickNow(x, y);
        });
    }

    private void dragNow(int x, int y) {
        try {
            Method method = net.minecraft.client.gui.GuiScreen.class
                    .getDeclaredMethod("mouseClickMove", int.class, int.class, int.class, long.class);
            method.setAccessible(true);
            method.invoke(mc.currentScreen, x, y, 0, 0);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            future.setException(e);
        }
    }

    public void drag(int x, int y) {
        runLater(() -> {
            LOGGER.info("Dragging to {}/{}", x, y);
            dragNow(x, y);
        });
    }

    public void click(GuiButton button) {
        runLater(() -> {
            if (!button.isEnabled()) {
                future.setException(new IllegalStateException("Button is disabled: " + button.getLabel()));
                return;
            }
            LOGGER.info("Clicking button {}", button.getLabel());
            button.onClick();
        });
    }

    public void click(GuiTexturedButton button) {
        runLater(() -> {
            if (!button.isEnabled()) {
                future.setException(new IllegalStateException("Button is disabled: " + button.getTexture()));
                return;
            }
            LOGGER.info("Clicking textured button {}", button.getTexture());
            button.onClick();
        });
    }

    public void click(String buttonText) {
        runLater(() -> {
            LOGGER.info("Clicking button {}", buttonText);
            try {
                Field field = net.minecraft.client.gui.GuiScreen.class.getDeclaredField("buttonList");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<net.minecraft.client.gui.GuiButton> buttonList = (List)
                        field.get(mc.currentScreen);

                net.minecraft.client.gui.GuiButton button = null;
                for (net.minecraft.client.gui.GuiButton guiButton : buttonList) {
                    if (guiButton.displayString.equals(buttonText)) {
                        button = guiButton;
                    }
                }
                if (button == null) {
                    future.setException(new NoSuchElementException("No button with label: " + buttonText));
                    return;
                }

                clickNow(button.xPosition + 5, button.yPosition + 5);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                future.setException(e);
            }
        });
    }

    public void type(String string) {
        for (char c : string.toCharArray()) {
            press(c, Keyboard.getKeyIndex(String.valueOf(c).toUpperCase()));
        }
    }

    public void press(int keyCode) {
        String keyName = Keyboard.getKeyName(keyCode);
        char character = keyName.length() == 1 ? keyName.charAt(0) : '\0';
        press(character, keyCode);
    }

    public void press(char character, int keyCode) {
        runLater(() -> {
            LOGGER.info("Pressing key {}", Keyboard.getKeyName(keyCode));
            if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
                KeyBinding.onTick(keyCode);
                FMLCommonHandler.instance().fireKeyInput();
            }
            if (mc.currentScreen != null) {
                try {
                    Method method = net.minecraft.client.gui.GuiScreen.class
                            .getDeclaredMethod("keyTyped", char.class, int.class);
                    method.setAccessible(true);
                    method.invoke(mc.currentScreen, character, keyCode);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    future.setException(e);
                }
            }
        });
    }
}
