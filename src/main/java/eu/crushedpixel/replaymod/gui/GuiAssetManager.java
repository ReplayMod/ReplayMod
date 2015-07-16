package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.assets.AssetFileUtils;
import eu.crushedpixel.replaymod.assets.AssetRepository;
import eu.crushedpixel.replaymod.assets.ReplayAsset;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.elements.listeners.FileChooseListener;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Point;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GuiAssetManager extends GuiScreen {

    private String screenTitle;

    private boolean initialized = false;
    private GuiEntryList<ReplayAsset> assetGuiEntryList;
    private GuiAdvancedButton removeButton;
    private GuiAdvancedTextField assetNameInput;
    private GuiFileChooser fileChooser, addButton;

    private ComposedElement inputElements;

    private ComposedElement composedElement;

    private AssetRepository assetRepository;

    private final AssetRepository initialRepository;

    private ReplayAsset currentAsset;

    public GuiAssetManager() {
        this.initialRepository = ReplayHandler.getAssetRepository();
        this.assetRepository = new AssetRepository(ReplayHandler.getAssetRepository());
    }

    @Override
    public void initGui() {
        if(!initialized) {
            screenTitle = I18n.format("replaymod.gui.assets.title");

            assetGuiEntryList = new GuiEntryList<ReplayAsset>(fontRendererObj, 0, 0, 0, 0);
            addButton = new GuiFileChooser(0, 0, 0, I18n.format("replaymod.gui.add"), null, AssetFileUtils.getAllAvailableExtensions()) {
                @Override
                protected void updateDisplayString() {
                    this.displayString = baseString;
                }
            };

            addButton.addFileChooseListener(new FileChooseListener() {
                @Override
                public void onFileChosen(File file) {
                    try {
                        ReplayAsset newAsset = assetRepository.addAsset(file.getName(), new FileInputStream(file));
                        assetGuiEntryList.addElement(newAsset);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            removeButton = new GuiAdvancedButton(0, 0, 0, I18n.format("replaymod.gui.remove")) {
                @Override
                public void performAction() {
                    assetRepository.removeAsset(currentAsset);
                    assetGuiEntryList.removeElement(assetGuiEntryList.getSelectionIndex());
                }
            };
            removeButton.setElementEnabled(false);

            assetNameInput = new GuiAdvancedTextField(fontRendererObj, 0, 0, 150, 20);
            assetNameInput.hint = I18n.format("replaymod.gui.assets.namehint");

            fileChooser = new GuiFileChooser(0, 0, 0, I18n.format("replaymod.gui.assets.changefile"), null, new String[0]) {
                @Override
                protected void updateDisplayString() {
                    this.displayString = baseString;
                }
            };
            fileChooser.addFileChooseListener(new FileChooseListener() {
                @Override
                public void onFileChosen(File file) {
                    if(currentAsset == null) return;
                    try {
                        currentAsset.loadFromStream(new FileInputStream(file));
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            fileChooser.width = 150;

            inputElements = new ComposedElement(assetNameInput, fileChooser);
            composedElement = new ComposedElement(assetGuiEntryList, addButton, removeButton, inputElements);

            inputElements.setElementEnabled(false);

            assetGuiEntryList.addSelectionListener(new SelectionListener() {
                @Override
                public void onSelectionChanged(int selectionIndex) {
                    currentAsset = assetGuiEntryList.getElement(selectionIndex);
                    inputElements.setElementEnabled(currentAsset != null);

                    assetNameInput.setText(currentAsset != null ? currentAsset.getDisplayString() : "");
                    removeButton.setElementEnabled(currentAsset != null);
                    fileChooser.setAllowedExtensions(currentAsset != null ? AssetFileUtils.fileExtensionsForAssetClass(currentAsset.getClass()) : new String[0]);
                }
            });

            for(ReplayAsset asset : assetRepository.getCopyOfReplayAssets()) {
                assetGuiEntryList.addElement(asset);
            }
        }

        int visibleEntries = (int)Math.floor(((double)this.height-(45+20+15+20))/14);

        assetGuiEntryList.width = 150;
        assetGuiEntryList.xPosition = width/2 - assetGuiEntryList.width - 5;
        assetGuiEntryList.setVisibleElements(visibleEntries);

        assetGuiEntryList.yPosition = assetNameInput.yPosition = 45;

        addButton.xPosition = assetGuiEntryList.xPosition-1;
        addButton.width = 77;
        removeButton.width = 76;
        removeButton.xPosition = addButton.xPosition+addButton.width;

        addButton.yPosition = removeButton.yPosition = assetGuiEntryList.yPosition+assetGuiEntryList.height+2;

        assetNameInput.xPosition = fileChooser.xPosition = this.width / 2 + 5;
        fileChooser.yPosition = assetNameInput.yPosition + 20 + 5;

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawCenteredString(fontRendererObj, screenTitle, this.width / 2, 5, Color.WHITE.getRGB());

        int leftBorder = 10;
        int topBorder = 20;

        drawGradientRect(leftBorder, topBorder, width - leftBorder, this.height - 10, -1072689136, -804253680);

        composedElement.draw(mc, mouseX, mouseY);

        if(currentAsset != null) {
            int y = fileChooser.yPosition + 20 + 5;
            int height = (addButton.yPosition+addButton.height)-y;

            GL11.glColor4f(1, 1, 1, 1);
            currentAsset.drawToScreen(fileChooser.xPosition, y, 150, height);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        composedElement.mouseClick(mc, mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        Point mousePos = MouseUtils.getMousePos();
        composedElement.buttonPressed(mc, mousePos.getX(), mousePos.getY(), typedChar, keyCode);

        if(currentAsset != null) {
            currentAsset.setAssetName(assetNameInput.getText());
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        composedElement.tick(mc);
    }

    @Override
    public void onGuiClosed() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(assetRepository.equals(initialRepository)) return;
                ReplayHandler.setAssetRepository(assetRepository);
                assetRepository.saveAssets();
            }
        }, "replaymod-asset-saver").start();
    }
}
