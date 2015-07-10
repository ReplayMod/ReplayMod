package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.assets.CustomImageObject;
import eu.crushedpixel.replaymod.assets.ReplayAsset;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.gui.elements.timelines.GuiTimeline;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.GuiEntryListValueEntry;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.Point;

import java.awt.*;
import java.io.IOException;
import java.util.UUID;

public class GuiObjectManager extends GuiScreen {

    private boolean initialized = false;

    private KeyframeList[] keyframeLists;

    private GuiEntryList<CustomImageObject> objectList;
    private GuiAdvancedButton addButton, removeButton;

    private GuiAdvancedTextField nameInput;

    private GuiString dropdownLabel;
    private GuiDropdown<GuiEntryListValueEntry<UUID>> assetDropdown;

    private GuiDraggingNumberInput anchorXInput, anchorYInput, anchorZInput;
    private GuiDraggingNumberInput positionXInput, positionYInput, positionZInput;
    private GuiDraggingNumberInput scaleXInput, scaleYInput, scaleZInput;
    private GuiDraggingNumberInput orientationXInput, orientationYInput, orientationZInput;
    private GuiDraggingNumberInput opacityInput;

    private GuiObjectKeyframeTimeline objectKeyframeTimeline;
    private GuiScrollbar timelineScrollbar;

    private GuiTexturedButton zoomInButton, zoomOutButton;

    private ComposedElement anchorInputs, positionInputs, scaleInputs, orientationInputs, opacityInputs, numberInputs;
    private ComposedElement disableElements, allElements;

    private static final int KEYFRAME_BUTTON_X = 80;
    private static final int KEYFRAME_BUTTON_Y = 40;

    private static final float ZOOM_STEPS = 0.05f;

    private DelegatingElement keyframeButton(final int x, final int y, final int line) {
        return new DelegatingElement() {

            private GuiTexturedButton normal = new GuiTexturedButton(0, x, y, 20, 20,
                    GuiReplayOverlay.replay_gui, KEYFRAME_BUTTON_X, KEYFRAME_BUTTON_Y,
                    GuiReplayOverlay.TEXTURE_SIZE, GuiReplayOverlay.TEXTURE_SIZE, new Runnable() {
                @Override
                public void run() {
                    addKeyframe(line);
                }
            },
                    null);

            private GuiTexturedButton selected = new GuiTexturedButton(0, x, y, 20, 20,
                    GuiReplayOverlay.replay_gui, KEYFRAME_BUTTON_X, KEYFRAME_BUTTON_Y+20,
                    GuiReplayOverlay.TEXTURE_SIZE, GuiReplayOverlay.TEXTURE_SIZE, new Runnable() {
                @Override
                public void run() {
                    removeKeyframe(line);
                }
            },
                    null);

            @Override
            public GuiElement delegate() {
                boolean sel = false; //TODO

                if(sel) {
                    return selected;
                } else {
                    return normal;
                }
            }

            @Override
            public void setEnabled(boolean enabled) {
                selected.setEnabled(enabled);
                normal.setEnabled(enabled);
            }
        };
    }

    private void addKeyframe(int line) {

    }

    private void removeKeyframe(int line) {

    }

    @Override
    public void initGui() {
        if(!initialized) {
            anchorXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            anchorYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            anchorZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);

            positionXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            positionYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            positionZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);

            scaleXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 100d, true, "%");
            scaleYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 100d, true, "%");
            scaleZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 100d, true, "%");

            orientationXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            orientationYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            orientationZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);

            opacityInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, 0d, 100d, 100d, true, "%");

            dropdownLabel = new GuiString(0, 0, Color.WHITE, I18n.format("replaymod.gui.assets.filechooser")+": ");
            assetDropdown = new GuiDropdown<GuiEntryListValueEntry<UUID>>(fontRendererObj, 0, 0, 0, 5);
            if(ReplayHandler.getAssetRepository().getCopyOfReplayAssets().isEmpty()) {
                assetDropdown.addElement(new GuiEntryListValueEntry<UUID>(I18n.format("replaymod.gui.assets.emptylist"), null));
            } else {
                for(ReplayAsset asset : ReplayHandler.getAssetRepository().getCopyOfReplayAssets()) {
                    assetDropdown.addElement(new GuiEntryListValueEntry<UUID>(
                            asset.getDisplayString(), ReplayHandler.getAssetRepository().getUUIDForAsset(asset)));
                }
            }

            objectList = new GuiEntryList<CustomImageObject>(fontRendererObj, 0, 0, 0, 0);
            objectList.setEmptyMessage(I18n.format("replaymod.gui.objects.empty"));

            addButton = new GuiAdvancedButton(0, 0, 0, 20, I18n.format("replaymod.gui.add"), new Runnable() {
                @Override
                public void run() {
                    CustomImageObject customImageObject = new CustomImageObject(I18n.format("replaymod.gui.objects.defaultname"), null);
                    objectList.addElement(customImageObject);
                }
            }, null);

            removeButton = new GuiAdvancedButton(0, 0, 0, 20, I18n.format("replaymod.gui.remove"), new Runnable() {
                @Override
                public void run() {
                    if(objectList.getElement(objectList.getSelectionIndex()) != null)
                        objectList.removeElement(objectList.getSelectionIndex());
                }
            }, null);

            nameInput = new GuiAdvancedTextField(fontRendererObj, 0, 0, 0, 20);
            nameInput.hint = I18n.format("replaymod.gui.objects.properties.name");

            keyframeLists = new KeyframeList[5];

            objectList.addSelectionListener(new SelectionListener() {
                @Override
                public void onSelectionChanged(int selectionIndex) {
                    CustomImageObject selectedObject = objectList.getElement(selectionIndex);
                    if(selectedObject != null) {
                        disableElements.setEnabled(true);
                    } else {
                        disableElements.setEnabled(false);
                    }
                }
            });
        }

        String[] labelStrings = new String[] {
                I18n.format("replaymod.gui.objects.properties.anchor"),
                I18n.format("replaymod.gui.objects.properties.position"),
                I18n.format("replaymod.gui.objects.properties.scale"),
                I18n.format("replaymod.gui.objects.properties.orientation"),
                I18n.format("replaymod.gui.objects.properties.opacity"),
        };

        int maxStringWidth = 0;
        for(String label : labelStrings) {
            int stringWidth = fontRendererObj.getStringWidth(label);
            if(stringWidth > maxStringWidth) {
                maxStringWidth = stringWidth;
            }
        }

        disableElements = new ComposedElement();
        allElements = new ComposedElement();

        int inputWidth = 40;

        anchorInputs = new ComposedElement(anchorXInput, anchorYInput, anchorZInput);
        positionInputs = new ComposedElement(positionXInput, positionYInput, positionZInput);
        scaleInputs = new ComposedElement(scaleXInput, scaleYInput, scaleZInput);
        orientationInputs = new ComposedElement(orientationXInput, orientationYInput, orientationZInput);
        opacityInputs = new ComposedElement(opacityInput);
        numberInputs = new ComposedElement(anchorInputs, positionInputs, scaleInputs, orientationInputs, opacityInputs);

        for(int i = numberInputs.getParts().length-1; i >= 0; i--) {
            int yPos = this.height-5-10-(25*(numberInputs.getParts().length-i));
            DelegatingElement button = keyframeButton(10, yPos, i);
            GuiString label = new GuiString(35, yPos + 6, Color.WHITE, labelStrings[i]);

            ComposedElement child = (ComposedElement)numberInputs.getParts()[i];
            int x = 0;
            for(GuiElement el : child.getParts()) {
                GuiDraggingNumberInput dni = (GuiDraggingNumberInput)el;
                dni.width = inputWidth;
                dni.xPosition = 35+maxStringWidth+5 + (x*(inputWidth+5));
                dni.yPosition = yPos;
                x++;
            }

            child.addPart(button);
            child.addPart(label);

            disableElements.addPart(child);
        }

        int timelineX = anchorZInput.xPosition + anchorZInput.width + 5 - 1;
        int timelineY = anchorZInput.yPosition - 1;

        int timelineWidth = this.width-10-timelineX + 2;
        int timelineHeight = this.height-10-10-timelineY + 2;

        objectKeyframeTimeline = new GuiObjectKeyframeTimeline(timelineX, timelineY, timelineWidth, timelineHeight, keyframeLists);
        disableElements.addPart(objectKeyframeTimeline);

        timelineScrollbar = new GuiScrollbar(timelineX, this.height-15, timelineWidth-21) {
            @Override
            public void dragged() {
                objectKeyframeTimeline.timeStart = (float)sliderPosition;
            }
        };

        timelineScrollbar.size = objectKeyframeTimeline.zoom;
        timelineScrollbar.sliderPosition = objectKeyframeTimeline.timeStart;

        disableElements.addPart(timelineScrollbar);

        zoomInButton = GuiReplayOverlay.texturedButton(width - 28, this.height-15, 40, 20, 9, new Runnable() {
            @Override
            public void run() {
                objectKeyframeTimeline.zoom = Math.max(0.025f, objectKeyframeTimeline.zoom - ZOOM_STEPS);
            }
        }, "replaymod.gui.ingame.menu.zoomin");

        zoomOutButton = GuiReplayOverlay.texturedButton(width - 18, this.height-15, 40, 30, 9, new Runnable() {

            @Override
            public void run() {
                objectKeyframeTimeline.zoom = Math.min(1f, objectKeyframeTimeline.zoom + ZOOM_STEPS);
                objectKeyframeTimeline.timeStart = Math.min(objectKeyframeTimeline.timeStart, 1f - objectKeyframeTimeline.zoom);
            }
        }, "replaymod.gui.ingame.menu.zoomout");

        disableElements.addPart(zoomInButton);
        disableElements.addPart(zoomOutButton);

        objectList.xPosition = 11;
        objectList.yPosition = 11;
        objectList.width = width/2 - 11 - 5;

        int visibleElements = (int)((anchorXInput.yPosition-5 - objectList.yPosition)/14f);
        objectList.setVisibleElements(visibleElements);

        allElements.addPart(objectList);

        nameInput.xPosition = width/2 + 5;
        nameInput.yPosition = objectList.yPosition;
        nameInput.width = objectList.width;

        disableElements.addPart(nameInput);

        dropdownLabel.positionX = (width/2)+5;
        int strWidth = fontRendererObj.getStringWidth(I18n.format("replaymod.gui.assets.filechooser")+": ");

        assetDropdown.xPosition = dropdownLabel.positionX+strWidth+5;
        assetDropdown.yPosition = objectList.yPosition+25;
        dropdownLabel.positionY = assetDropdown.yPosition + 6;
        assetDropdown.width = (objectList.width-strWidth-5);

        disableElements.addPart(dropdownLabel);
        disableElements.addPart(assetDropdown);

        addButton.xPosition = nameInput.xPosition;
        addButton.width = removeButton.width = nameInput.width/2 - 2;
        removeButton.xPosition = addButton.xPosition + nameInput.width/2 + 2;
        addButton.yPosition = removeButton.yPosition = objectList.yPosition+objectList.height-20;

        allElements.addPart(addButton);
        disableElements.addPart(removeButton);

        allElements.addPart(disableElements);

        objectList.setSelectionIndex(objectList.getSelectionIndex()); // trigger an event for the SelectionListener

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        allElements.draw(mc, mouseX, mouseY);

        GlStateManager.enableBlend();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        Point mousePos = MouseUtils.getMousePos();
        allElements.buttonPressed(mc, mousePos.getX(), mousePos.getY(), typedChar, keyCode);

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        allElements.tick(mc);
        super.updateScreen();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        allElements.mouseClick(mc, mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        allElements.mouseDrag(mc, mouseX, mouseY, mouseButton);
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        allElements.mouseRelease(mc, mouseX, mouseY, mouseButton);
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public class GuiObjectKeyframeTimeline extends GuiTimeline {

        private KeyframeList[] keyframeLists;

        public int x, y, width, height;

        private boolean dragging = false;

        public GuiObjectKeyframeTimeline(int x, int y, int width, int height, KeyframeList... keyframeLists) {
            super(x, y, width, height);

            this.keyframeLists = keyframeLists;

            this.zoom = 0.1;
            this.timelineLength = 10 * 60 * 1000;
            this.showMarkers = true;
        }

        @Override
        public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
            if(!enabled) return;
            super.mouseClick(mc, mouseX, mouseY, button);
            int time = (int) getTimeAt(mouseX, mouseY);
            if(time != -1)  {
                cursorPosition = time;
                dragging = true;
            }
        }

        @Override
        public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
            if(!enabled) return;
            super.mouseDrag(mc, mouseX, mouseY, button);
            if(dragging) {
                int time = (int) getTimeAt(mouseX, mouseY);
                if(time != -1) cursorPosition = time;
            }
        }

        @Override
        public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
            if(!enabled) return;
            super.mouseRelease(mc, mouseX, mouseY, button);
            this.dragging = false;
        }
    }
}
