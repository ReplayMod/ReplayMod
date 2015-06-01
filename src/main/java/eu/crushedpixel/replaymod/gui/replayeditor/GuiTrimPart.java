package eu.crushedpixel.replaymod.gui.replayeditor;

import eu.crushedpixel.replaymod.gui.elements.GuiNumberInput;
import eu.crushedpixel.replaymod.studio.StudioImplementation;
import eu.crushedpixel.replaymod.utils.TimestampUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GuiTrimPart extends GuiStudioPart {

    private static final String DESCRIPTION = I18n.format("replaymod.gui.editor.trim.description");
    private final String TITLE = I18n.format("replaymod.gui.editor.trim.title");
    private Minecraft mc = Minecraft.getMinecraft();
    private boolean initialized = false;

    private GuiNumberInput startMinInput, startSecInput, startMsInput;
    private GuiNumberInput endMinInput, endSecInput, endMsInput;

    private List<GuiNumberInput> inputOrder = new ArrayList<GuiNumberInput>();

    public GuiTrimPart(int yPos) {
        super(yPos);
        fontRendererObj = mc.fontRendererObj;
    }

    @Override
    public void applyFilters(File replayFile, File outputFile) {
        try {
            StudioImplementation.trimReplay(replayFile, getStartTimestamp(), getEndTimestamp(), outputFile);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private int valueOf(String text) {
        try {
            return Integer.valueOf(text);
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    private int getStartTimestamp() {
        int mins = valueOf(startMinInput.getText());
        int secs = valueOf(startSecInput.getText());
        int ms = valueOf(startMsInput.getText());

        return TimestampUtils.calculateTimestamp(mins, secs, ms);
    }

    private int getEndTimestamp() {
        int mins = valueOf(endMinInput.getText());
        int secs = valueOf(endSecInput.getText());
        int ms = valueOf(endMsInput.getText());

        return TimestampUtils.calculateTimestamp(mins, secs, ms);
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void initGui() {
        if(!initialized) {
            startMinInput = new GuiNumberInput(1, fontRendererObj, 70, yPos, 30, null, 999d, null, false);
            startSecInput = new GuiNumberInput(1, fontRendererObj, 120, yPos, 25, null, 99d, null, false);
            startMsInput = new GuiNumberInput(1, fontRendererObj, 165, yPos, 30, null, 999d, null, false);

            endMinInput = new GuiNumberInput(1, fontRendererObj, 70, yPos + 30, 30, null, 999d, null, false);
            endSecInput = new GuiNumberInput(1, fontRendererObj, 120, yPos + 30, 25, null, 99d, null, false);
            endMsInput = new GuiNumberInput(1, fontRendererObj, 165, yPos + 30, 30, null, 999d, null, false);

            inputOrder.clear();

            inputOrder.add(startMinInput);
            inputOrder.add(startSecInput);
            inputOrder.add(startMsInput);
            inputOrder.add(endMinInput);
            inputOrder.add(endSecInput);
            inputOrder.add(endMsInput);
        }

        initialized = true;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for(GuiNumberInput input : inputOrder) {
            input.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawString(mc.fontRendererObj, I18n.format("replaymod.gui.start")+":", 30, yPos + 7, Color.WHITE.getRGB());
        drawString(mc.fontRendererObj, I18n.format("replaymod.gui.end")+":", 30, yPos + 7 + 30, Color.WHITE.getRGB());
        drawString(mc.fontRendererObj, I18n.format("replaymod.gui.minutes"), 105, yPos + 7, Color.WHITE.getRGB());
        drawString(mc.fontRendererObj, I18n.format("replaymod.gui.minutes"), 105, yPos + 7 + 30, Color.WHITE.getRGB());
        drawString(mc.fontRendererObj, I18n.format("replaymod.gui.seconds"), 150, yPos + 7, Color.WHITE.getRGB());
        drawString(mc.fontRendererObj, I18n.format("replaymod.gui.seconds"), 150, yPos + 7 + 30, Color.WHITE.getRGB());
        drawString(mc.fontRendererObj, I18n.format("replaymod.gui.milliseconds"), 200, yPos + 7, Color.WHITE.getRGB());
        drawString(mc.fontRendererObj, I18n.format("replaymod.gui.milliseconds"), 200, yPos + 7 + 30, Color.WHITE.getRGB());

        drawString(mc.fontRendererObj, "Timestamp: " + getStartTimestamp(), 230, yPos + 7, Color.WHITE.getRGB());
        drawString(mc.fontRendererObj, "Timestamp: " + getEndTimestamp(), 230, yPos + 30 + 7, Color.WHITE.getRGB());

        for(GuiNumberInput input : inputOrder) {
            input.drawTextBox();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        if(!initialized) {
            initGui();
        } else {
            for(GuiNumberInput input : inputOrder) {
                input.updateCursorCounter();
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(keyCode == Keyboard.KEY_TAB) { //Tab handling
            int i = 0;
            for(GuiNumberInput input : inputOrder) {
                if(input.isFocused()) {
                    input.setFocused(false);
                    i++;
                    if(i >= inputOrder.size()) i = 0;
                    inputOrder.get(i).setFocused(true);
                    break;
                }
                i++;
            }
        } else {
            for(GuiNumberInput input : inputOrder) {
                input.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }
}
