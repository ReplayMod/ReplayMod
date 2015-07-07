package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.elements.listeners.FileChooseListener;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.holders.CustomImageObject;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiAssetAdder extends GuiScreen {

    private String screenTitle;

    private boolean initialized = false;

    private GuiEntryList<CustomImageObject> objectList;
    private GuiButton removeButton;
    private GuiFileChooser addButton;
    private GuiFileChooser guiFileChooser;

    private GuiAdvancedTextField nameInput;

    private GuiNumberInput xInput, yInput, zInput, yawInput, pitchInput, rollInput, scaleInput, opacityInput;

    private List<GuiTextField> textFields = new ArrayList<GuiTextField>();

    public GuiAssetAdder() {
        ReplayMod.replaySender.setReplaySpeed(0);
    }

    @Override
    public void initGui() {
        if(!initialized) {
            screenTitle = I18n.format("replaymod.gui.assets.title");

            objectList = new GuiEntryList<CustomImageObject>(fontRendererObj, 0, 0, 0, 0);
            objectList.setEmptyMessage(I18n.format("replaymod.gui.assets.emptylist"));

            for(CustomImageObject object : ReplayHandler.getCustomImageObjects()) {
                objectList.addElement(object);
            }

            removeButton = new GuiButton(GuiConstants.ASSET_ADDER_REMOVE_BUTTON, 0, 0, I18n.format("replaymod.gui.remove"));

            addButton = new GuiFileChooser(1, 0, 0, I18n.format("replaymod.gui.add"), null, ImageIO.getReaderFileSuffixes()) {
                @Override protected void updateDisplayString() {}
            };

            addButton.addFileChooseListener(new FileChooseListener() {
                @Override
                public void onFileChosen(File file) {
                    try {
                        objectList.addElement(new CustomImageObject(new Position(mc.getRenderViewEntity()),
                                I18n.format("replaymod.gui.assets.defaultname"), file));
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            nameInput = new GuiAdvancedTextField(fontRendererObj, 2, 0, 0, 20);
            nameInput.hint = I18n.format("replaymod.gui.assets.namehint");

            xInput = new GuiNumberInput(3, fontRendererObj, 0, 0, 0, null, null, 0d, true);
            yInput = new GuiNumberInput(4, fontRendererObj, 0, 0, 0, null, null, 0d, true);
            zInput = new GuiNumberInput(5, fontRendererObj, 0, 0, 0, null, null, 0d, true);

            yawInput = new GuiNumberInput(6, fontRendererObj, 0, 0, 0, -360d, 360d, 0d, true);
            pitchInput = new GuiNumberInput(7, fontRendererObj, 0, 0, 0, -360d, 360d, 0d, true);
            rollInput = new GuiNumberInput(8, fontRendererObj, 0, 0, 0, -360d, 360d, 0d, true);

            scaleInput = new GuiNumberInputWithText(9, fontRendererObj, 0, 0, 0, 0d, 10000d, 100d, true, "%");
            opacityInput = new GuiNumberInputWithText(10, fontRendererObj, 0, 0, 0, 0d, 100d, 100d, true, "%");

            guiFileChooser = new GuiFileChooser(3, 0, 0, I18n.format("replaymod.gui.assets.filechooser")+": ", null, ImageIO.getReaderFileSuffixes());
            guiFileChooser.addFileChooseListener(new FileChooseListener() {
                @Override
                public void onFileChosen(File file) {
                    try {
                        objectList.getElement(objectList.getSelectionIndex()).setImageFile(file);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            guiFileChooser.enabled = false;

            textFields.add(objectList);
            textFields.add(nameInput);
            textFields.add(xInput);
            textFields.add(yInput);
            textFields.add(zInput);
            textFields.add(pitchInput);
            textFields.add(yawInput);
            textFields.add(rollInput);
            textFields.add(scaleInput);
            textFields.add(opacityInput);

            objectList.addSelectionListener(new SelectionListener() {
                @Override
                public void onSelectionChanged(int selectionIndex) {
                    CustomImageObject object = objectList.getElement(selectionIndex);
                    if(object == null) {
                        guiFileChooser.enabled = false;
                        for(GuiTextField tf : textFields) {
                            tf.setEnabled(false);
                        }
                        return;
                    }
                    guiFileChooser.enabled = true;
                    for(GuiTextField tf : textFields) {
                        tf.setEnabled(true);
                    }

                    nameInput.setText(object.getName());
                    guiFileChooser.setSelectedFile(object.getImageFile());
                    xInput.setValue(object.getPosition().getX());
                    yInput.setValue(object.getPosition().getY());
                    zInput.setValue(object.getPosition().getZ());
                    pitchInput.setValue(object.getPosition().getPitch());
                    yawInput.setValue(object.getPosition().getYaw());
                    rollInput.setValue(object.getPosition().getRoll());
                    scaleInput.setValue(object.getPosition().getScale()*100);
                    opacityInput.setValue(object.getPosition().getOpacity()*100);
                }
            });

            if(objectList.getEntryCount() > 0) {
                objectList.setSelectionIndex(0);
            }
        }

        int h = (int)Math.floor(((double)this.height-(45+20+15+20))/14);

        objectList.width = guiFileChooser.width = nameInput.width = 150;
        objectList.xPosition = width/2 - objectList.width - 5;
        objectList.setVisibleElements(h);
        objectList.yPosition = 45;

        addButton.width = removeButton.width = 75;
        addButton.xPosition = objectList.xPosition;
        removeButton.xPosition = addButton.xPosition+addButton.width;
        addButton.yPosition = removeButton.yPosition = objectList.yPosition + objectList.height + 2;

        nameInput.xPosition = this.width/2 + 5;
        nameInput.yPosition = 45;

        guiFileChooser.xPosition = this.width/2 + 5;
        guiFileChooser.yPosition = nameInput.yPosition + 20 + 5;

        xInput.yPosition = yInput.yPosition = zInput.yPosition = guiFileChooser.yPosition + 20 + 5 + 15;
        yawInput.yPosition = pitchInput.yPosition = rollInput.yPosition = xInput.yPosition + 20 + 5 + 15;
        scaleInput.yPosition = opacityInput.yPosition = yawInput.yPosition + 20 + 5 + 15;

        xInput.xPosition = scaleInput.xPosition = pitchInput.xPosition = this.width/2 + 8;
        yInput.xPosition = yawInput.xPosition = xInput.xPosition + 50;
        zInput.xPosition = rollInput.xPosition = yInput.xPosition + 50;
        opacityInput.xPosition = scaleInput.xPosition + 75;

        xInput.width = yInput.width = zInput.width = yawInput.width = pitchInput.width = rollInput.width = 43;
        scaleInput.width = opacityInput.width = 68;

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

        for(GuiTextField textField : textFields) {
            textField.drawTextBox();
        }

        drawCenteredString(fontRendererObj, "X", xInput.xPosition + (xInput.width/2), xInput.yPosition - 12, Color.WHITE.getRGB());
        drawCenteredString(fontRendererObj, "Y", yInput.xPosition + (yInput.width/2), yInput.yPosition - 12, Color.WHITE.getRGB());
        drawCenteredString(fontRendererObj, "Z", zInput.xPosition + (zInput.width/2), zInput.yPosition - 12, Color.WHITE.getRGB());

        drawCenteredString(fontRendererObj, "Yaw", yawInput.xPosition + (yawInput.width/2), yawInput.yPosition - 12, Color.WHITE.getRGB());
        drawCenteredString(fontRendererObj, "Pitch", pitchInput.xPosition + (pitchInput.width/2), pitchInput.yPosition - 12, Color.WHITE.getRGB());
        drawCenteredString(fontRendererObj, "Roll", rollInput.xPosition + (rollInput.width/2), rollInput.yPosition - 12, Color.WHITE.getRGB());

        drawCenteredString(fontRendererObj, "Scale", scaleInput.xPosition + (scaleInput.width/2), scaleInput.yPosition - 12, Color.WHITE.getRGB());
        drawCenteredString(fontRendererObj, "Opacity", opacityInput.xPosition + (opacityInput.width/2), opacityInput.yPosition - 12, Color.WHITE.getRGB());
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        for(GuiTextField textField : textFields) {
            textField.updateCursorCounter();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for(GuiTextField textField : textFields) {
            textField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for(GuiTextField textField : textFields) {
            textField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);

        if(guiFileChooser.enabled) {
            CustomImageObject current = objectList.getElement(objectList.getSelectionIndex());
            if(current == null) return;

            current.setName(nameInput.getText().trim());
            current.getPosition().setX(xInput.getPreciseValue());
            current.getPosition().setY(yInput.getPreciseValue());
            current.getPosition().setZ(zInput.getPreciseValue());
            current.getPosition().setPitch((float) pitchInput.getPreciseValue());
            current.getPosition().setYaw((float) yawInput.getPreciseValue());
            current.getPosition().setRoll((float) rollInput.getPreciseValue());
            current.getPosition().setScale((float) scaleInput.getPreciseValue() / 100f);
            current.getPosition().setOpacity((float) opacityInput.getPreciseValue() / 100f);
        }

    }

    @Override
    public void onGuiClosed() {
        ReplayHandler.setCustomImageObjects(objectList.getCopyOfElements());
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;

        if(button.id == GuiConstants.ASSET_ADDER_REMOVE_BUTTON) {
            CustomImageObject current = objectList.getElement(objectList.getSelectionIndex());
            if(current == null) return;

            objectList.removeElement(objectList.getSelectionIndex());
        }
    }
}
