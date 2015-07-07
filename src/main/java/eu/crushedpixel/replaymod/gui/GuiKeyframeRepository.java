package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.GuiEntryList;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.KeyframeSet;
import eu.crushedpixel.replaymod.holders.MarkerKeyframe;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiKeyframeRepository extends GuiScreen implements GuiReplayOverlay.NoOverlay {

    private boolean initialized = false;

    private String screenTitle;
    private final Minecraft mc = Minecraft.getMinecraft();

    private GuiEntryList<KeyframeSet> keyframeSetList;
    private KeyframeSet[] keyframeRepository;

    private KeyframeSet currentKeyframeSet = null;

    private GuiTextField nameInput;
    private GuiButton removeButton, loadButton, saveButton;

    private String message = null;

    private int currentSetTimeKeyframeCount, currentSetPositionKeyframeCount, currentSetDuration;

    public GuiKeyframeRepository(KeyframeSet[] keyframeRepository) {
        this.keyframeRepository = keyframeRepository;
        ReplayMod.replaySender.setReplaySpeed(0);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int h = (int)Math.floor(((double)this.height-(45+20+15))/14);

        if(!initialized) {
            screenTitle = I18n.format("replaymod.gui.keyframerepository.title");

            keyframeSetList = new GuiEntryList<KeyframeSet>(mc.fontRendererObj,
                    0, 45, 0, h);

            for(KeyframeSet set : keyframeRepository)
                keyframeSetList.addElement(set);

            keyframeSetList.setSelectionIndex(-1);

            keyframeSetList.setEmptyMessage(I18n.format("replaymod.gui.keyframerepository.noentries"));

            keyframeSetList.addSelectionListener(new SelectionListener() {
                @Override
                public void onSelectionChanged(int selectionIndex) {
                    removeButton.enabled = selectionIndex >= 0;
                    loadButton.enabled = selectionIndex >= 0;
                    nameInput.setEnabled(selectionIndex >= 0);

                    if(selectionIndex >= 0) {
                        currentKeyframeSet = keyframeSetList.getElement(selectionIndex);
                        nameInput.setText(currentKeyframeSet.getName());

                        currentSetPositionKeyframeCount = currentKeyframeSet.getPositionKeyframeCount();
                        currentSetTimeKeyframeCount = currentKeyframeSet.getTimeKeyframeCount();
                        currentSetDuration = currentKeyframeSet.getPathDuration();
                    } else {
                        nameInput.setText("");
                        currentKeyframeSet = null;
                    }

                    message = null;
                }
            });

            nameInput = new GuiTextField(GuiConstants.KEYFRAME_REPOSTORY_NAME_INPUT, mc.fontRendererObj,
                    0, 45, 0, 20);
            nameInput.setEnabled(false);

            removeButton = new GuiButton(GuiConstants.KEYFRAME_REPOSITORY_REMOVE_BUTTON, 0, 75,
                    I18n.format("replaymod.gui.remove"));
            loadButton = new GuiButton(GuiConstants.KEYFRAME_REPOSITORY_LOAD_BUTTON, 0, 75,
                    I18n.format("replaymod.gui.load"));

            saveButton = new GuiButton(GuiConstants.KEYFRAME_REPOSITORY_ADD_BUTTON, 30 + (this.width / 2),
                    keyframeSetList.yPosition+keyframeSetList.height-20,
                    I18n.format("replaymod.gui.keyframerepository.savecurrent"));

            removeButton.enabled = loadButton.enabled = false;

        }

        keyframeSetList.width = nameInput.width = saveButton.width = 150;

        keyframeSetList.xPosition = width / 2 - keyframeSetList.width - 5;
        keyframeSetList.setVisibleElements(h);

        removeButton.width = loadButton.width = 73;

        nameInput.xPosition = saveButton.xPosition = width / 2 + 5;
        loadButton.xPosition = width / 2 + 5;
        removeButton.xPosition = width / 2 + 5 + loadButton.width + 3 + 4;

        saveButton.yPosition = keyframeSetList.yPosition+keyframeSetList.height-20;

        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = this.buttonList;

        buttonList.add(removeButton);
        buttonList.add(loadButton);
        buttonList.add(saveButton);

        initialized = true;
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if(!button.enabled) return;
        switch(button.id) {
            case GuiConstants.KEYFRAME_REPOSITORY_ADD_BUTTON:
                List<Keyframe> kfs = new ArrayList<Keyframe>(ReplayHandler.getKeyframes());
                for(Keyframe kf : new ArrayList<Keyframe>(kfs)) {
                    if(kf instanceof MarkerKeyframe) kfs.remove(kf);
                }

                Keyframe[] keyframes = kfs.toArray(new Keyframe[ReplayHandler.getKeyframes().size()]);
                KeyframeSet newSet = new KeyframeSet(I18n.format("replaymod.gui.keyframerepository.preset.defaultname"), keyframes);
                if(newSet.getPositionKeyframeCount() < 2 || newSet.getTimeKeyframeCount() < 1) {
                    message = I18n.format("replaymod.chat.morekeyframes");
                    break;
                } else if(keyframeSetList.getCopyOfElements().contains(newSet)) {
                    message = I18n.format("replaymod.gui.keyframerepository.duplicate");
                    break;
                }
                message = null;

                keyframeSetList.addElement(newSet);
                keyframeSetList.setSelectionIndex(keyframeSetList.getEntryCount()-1);
                break;
            case GuiConstants.KEYFRAME_REPOSITORY_REMOVE_BUTTON:
                keyframeSetList.removeElement(keyframeSetList.getSelectionIndex());
                break;
            case GuiConstants.KEYFRAME_REPOSITORY_LOAD_BUTTON:
                ReplayHandler.useKeyframePreset(keyframeRepository[keyframeSetList.getSelectionIndex()].getKeyframes());
                saveOnQuit();
                mc.displayGuiScreen(null);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawCenteredString(fontRendererObj, screenTitle, this.width / 2, 5, Color.WHITE.getRGB());

        int leftBorder = 10;
        int topBorder = 20;

        drawGradientRect(leftBorder, topBorder, width - leftBorder, this.height - 10, -1072689136, -804253680);

        this.drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.keyframerepository.presets"), keyframeSetList.xPosition + (keyframeSetList.width/2), 30, Color.WHITE.getRGB());

        keyframeSetList.drawTextBox();

        nameInput.drawTextBox();

        KeyframeSet currentSet = null;

        if(keyframeSetList.getSelectionIndex() >= 0) {
            currentSet = keyframeSetList.getElement(keyframeSetList.getSelectionIndex());
        }

        if(currentSet != null) {
            this.drawString(fontRendererObj, I18n.format("replaymod.gui.keyframerepository.positionkeyframes") + ": " +currentSetPositionKeyframeCount,
                    loadButton.xPosition+2, removeButton.yPosition + 30, Color.WHITE.getRGB());

            this.drawString(fontRendererObj, I18n.format("replaymod.gui.keyframerepository.timekeyframes") + ": " +currentSetTimeKeyframeCount,
                    loadButton.xPosition+2, removeButton.yPosition + 50, Color.WHITE.getRGB());

            this.drawString(fontRendererObj, I18n.format("replaymod.gui.duration")+": "+ DurationFormatUtils.formatDurationHMS(currentSetDuration),
                    loadButton.xPosition+2, removeButton.yPosition + 70, Color.WHITE.getRGB());
        }

        if(message != null) {
            this.drawCenteredString(fontRendererObj, message, this.width/2, this.height-25, Color.RED.getRGB());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        keyframeSetList.mouseClicked(mouseX, mouseY, mouseButton);
        nameInput.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        nameInput.updateCursorCounter();
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        nameInput.textboxKeyTyped(typedChar, keyCode);
        if(keyCode == Keyboard.KEY_ESCAPE) {
            saveOnQuit();
            super.keyTyped(typedChar, keyCode);
        }
        if(currentKeyframeSet != null) {
            currentKeyframeSet.setName(nameInput.getText());
            keyframeSetList.replaceElement(keyframeSetList.getSelectionIndex(), currentKeyframeSet);
        }
    }

    private void saveOnQuit() {
        ArrayList<KeyframeSet> copy = new ArrayList<KeyframeSet>(keyframeSetList.getCopyOfElements());
        this.keyframeRepository = copy.toArray(new KeyframeSet[copy.size()]);
        ReplayHandler.setKeyframeRepository(keyframeRepository, true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}