package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.GuiAdvancedTextField;
import eu.crushedpixel.replaymod.gui.elements.GuiEntryList;
import eu.crushedpixel.replaymod.gui.elements.GuiFileChooser;
import eu.crushedpixel.replaymod.gui.elements.GuiNumberInput;
import eu.crushedpixel.replaymod.gui.elements.listeners.FileChooseListener;
import eu.crushedpixel.replaymod.holders.CustomImageObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiCheckBox;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;

public class GuiAssetAdder extends GuiScreen {

    private String screenTitle;

    private boolean initialized = false;

    private GuiEntryList<CustomImageObject> objectList;
    private GuiButton removeButton;
    private GuiFileChooser addButton;
    private GuiFileChooser guiFileChooser;

    private GuiAdvancedTextField nameInput;

    private GuiNumberInput xInput, yInput, zInput, yawInput, pitchInput, rollInput, scaleInput;
    private GuiCheckBox backVisibleCheckBox;

    public GuiAssetAdder() {
        ReplayMod.replaySender.setReplaySpeed(0);
    }

    @Override
    public void initGui() {
        if(!initialized) {
            screenTitle = I18n.format("replaymod.gui.assets.title");

            objectList = new GuiEntryList<CustomImageObject>(0, fontRendererObj, 0, 0, 0, 0);
            removeButton = new GuiButton(GuiConstants.ASSET_ADDER_REMOVE_BUTTON, 0, 0, I18n.format("replaymod.gui.remove"));

            addButton = new GuiFileChooser(1, 0, 0, I18n.format("replaymod.gui.add"), null, ImageIO.getReaderFileSuffixes()) {
                @Override protected void updateDisplayString() {}
            };

            addButton.registerListener(new FileChooseListener() {
                @Override
                public void onFileChosen(File file) {

                }
            });

            guiFileChooser = new GuiFileChooser(2, 0, 0, I18n.format("replaymod.gui.assets.filechooser")+": ", null, ImageIO.getReaderFileSuffixes());
        }

        int h = (int)Math.floor(((double)this.height-(45+20+15+20))/14);

        objectList.width = guiFileChooser.width = 150;
        objectList.xPosition = width/2 - objectList.width - 5;
        objectList.setVisibleElements(h);
        objectList.yPosition = 45;

        addButton.width = removeButton.width = 75;
        addButton.xPosition = objectList.xPosition;
        removeButton.xPosition = addButton.xPosition+addButton.width;
        addButton.yPosition = removeButton.yPosition = objectList.yPosition + objectList.height + 2;

        guiFileChooser.xPosition = this.width/2 + 5;
        guiFileChooser.yPosition = 45;

        buttonList.add(removeButton);
        buttonList.add(addButton);
        buttonList.add(guiFileChooser);

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawCenteredString(fontRendererObj, screenTitle, this.width / 2, 5, Color.WHITE.getRGB());

        int leftBorder = 10;
        int topBorder = 20;

        drawGradientRect(leftBorder, topBorder, width - leftBorder, this.height - 10, -1072689136, -804253680);

        objectList.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }
}
