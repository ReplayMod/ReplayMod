package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.assets.CustomImageObject;
import eu.crushedpixel.replaymod.assets.CustomObjectRepository;
import eu.crushedpixel.replaymod.assets.ReplayAsset;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.elements.listeners.NumberValueChangeListener;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.gui.elements.timelines.GuiTimeline;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.*;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class GuiObjectManager extends GuiScreen implements GuiReplayOverlay.NoOverlay {

    private boolean initialized = false;

    private GuiEntryList<CustomImageObject> objectList;
    private GuiAdvancedButton addButton, removeButton;

    private GuiAdvancedTextField nameInput;

    private GuiString dropdownLabel;
    private GuiDropdown<GuiEntryListValueEntry<UUID>> assetDropdown;

    private final List<CustomImageObject> initialObjects = ReplayHandler.getCustomImageObjects();

    private GuiDraggingNumberInput anchorXInput, anchorYInput, anchorZInput;
    private GuiDraggingNumberInput positionXInput, positionYInput, positionZInput;
    private GuiDraggingNumberInput orientationXInput, orientationYInput, orientationZInput;
    private GuiDraggingNumberInput scaleXInput, scaleYInput, scaleZInput;
    private GuiDraggingNumberInput opacityInput;
    
    private NumberInputGroup anchorNumberInputs, positionNumberInputs, orientationNumberInputs, scaleNumberInputs, opacityNumberInputs;

    @Getter
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
                    objectKeyframeTimeline.addKeyframe(line);
                }
            },
                    null);

            private GuiTexturedButton selected = new GuiTexturedButton(0, x, y, 20, 20,
                    GuiReplayOverlay.replay_gui, KEYFRAME_BUTTON_X, KEYFRAME_BUTTON_Y+20,
                    GuiReplayOverlay.TEXTURE_SIZE, GuiReplayOverlay.TEXTURE_SIZE, new Runnable() {
                @Override
                public void run() {
                    objectKeyframeTimeline.removeKeyframe(line);
                }
            },
                    null);

            @Override
            public GuiElement delegate() {
                if(objectKeyframeTimeline.getSelectedKeyframeRow() == line) {
                    return selected;
                } else {
                    return normal;
                }
            }

            @Override
            public void setElementEnabled(boolean enabled) {
                selected.setElementEnabled(enabled);
                normal.setElementEnabled(enabled);
            }
        };
    }

    @Override
    public void initGui() {
        if(!initialized) {
            anchorXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true, "", 0.1);
            anchorYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true, "", 0.1);
            anchorZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true, "", 0.1);
            anchorNumberInputs = new NumberPositionInputGroup(null, anchorXInput, anchorYInput, anchorZInput);

            positionXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            positionYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            positionZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            positionNumberInputs = new NumberPositionInputGroup(null, positionXInput, positionYInput, positionZInput);

            orientationXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            orientationYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            orientationZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 0d, true);
            orientationNumberInputs = new NumberPositionInputGroup(null, orientationXInput, orientationYInput, orientationZInput);

            scaleXInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 100d, true, "%", 0.5);
            scaleYInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 100d, true, "%", 0.5);
            scaleZInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, null, null, 100d, true, "%", 0.5);
            scaleNumberInputs = new NumberPositionInputGroup(null, scaleXInput, scaleYInput, scaleZInput);

            opacityInput = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 50, 0d, 100d, 100d, true, "%", 0.5);
            opacityNumberInputs = new NumberValueInputGroup(null, opacityInput);

            dropdownLabel = new GuiString(0, 0, Color.WHITE, I18n.format("replaymod.gui.assets.filechooser")+": ");
            assetDropdown = new GuiDropdown<GuiEntryListValueEntry<UUID>>(fontRendererObj, 0, 0, 0, 5);
            if(ReplayHandler.getAssetRepository().getCopyOfReplayAssets().isEmpty()) {
                assetDropdown.addElement(new GuiEntryListValueEntry<UUID>(I18n.format("replaymod.gui.assets.emptylist"), null));
            } else {
                assetDropdown.addElement(new GuiEntryListValueEntry<UUID>(I18n.format("replaymod.gui.assets.noselection"), null));
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
                    try {
                        CustomImageObject customImageObject = new CustomImageObject(I18n.format("replaymod.gui.objects.defaultname"), null);

                        Position defaultPosition = new Position(mc.getRenderViewEntity().getPosition());
                        customImageObject.getTransformations().setDefaultPosition(defaultPosition);

                        objectList.addElement(customImageObject);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
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

            for(CustomImageObject customImageObject : initialObjects) {
                objectList.addElement(customImageObject);
            }

            objectList.addSelectionListener(new SelectionListener() {
                @Override
                public void onSelectionChanged(int selectionIndex) {
                    CustomImageObject selectedObject = objectList.getElement(selectionIndex);
                    if(selectedObject != null) {
                        disableElements.setElementEnabled(true);

                        nameInput.setText(selectedObject.getName());

                        //setting the dropdown value
                        int sel = 0;
                        if(selectedObject.getLinkedAsset() != null) {
                            int i = 0;
                            for(GuiEntryListValueEntry<UUID> entry : assetDropdown.getAllElements()) {
                                if(selectedObject.getLinkedAsset().equals(entry.getValue())) {
                                    sel = i;
                                    break;
                                }
                                i++;
                            }
                        }

                        assetDropdown.setSelectionIndexQuietly(sel);

                        Transformations transformations = selectedObject.getTransformations();
                        
                        objectKeyframeTimeline.setTransformations(transformations);
                        
                        anchorNumberInputs.setUnderlyingKeyframeList(transformations.getAnchorKeyframes());
                        positionNumberInputs.setUnderlyingKeyframeList(transformations.getPositionKeyframes());
                        orientationNumberInputs.setUnderlyingKeyframeList(transformations.getOrientationKeyframes());
                        scaleNumberInputs.setUnderlyingKeyframeList(transformations.getScaleKeyframes());
                        opacityNumberInputs.setUnderlyingKeyframeList(transformations.getOpacityKeyframes());

                        updateValuesForTransformation(objectKeyframeTimeline.getTransformations().getTransformationForTimestamp(objectKeyframeTimeline.cursorPosition));
                    } else {
                        disableElements.setElementEnabled(false);
                    }
                }
            });

            assetDropdown.addSelectionListener(new SelectionListener() {
                @Override
                public void onSelectionChanged(int selectionIndex) {
                    CustomImageObject selectedObject = objectList.getElement(objectList.getSelectionIndex());
                    GuiEntryListValueEntry<UUID> entry = assetDropdown.getElement(selectionIndex);
                    if(selectedObject != null && entry != null) {
                        try {
                            selectedObject.setLinkedAsset(entry.getValue());
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        String[] labelStrings = new String[] {
                I18n.format("replaymod.gui.objects.properties.anchor"),
                I18n.format("replaymod.gui.objects.properties.position"),
                I18n.format("replaymod.gui.objects.properties.orientation"),
                I18n.format("replaymod.gui.objects.properties.scale"),
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
        orientationInputs = new ComposedElement(orientationXInput, orientationYInput, orientationZInput);
        scaleInputs = new ComposedElement(scaleXInput, scaleYInput, scaleZInput);
        opacityInputs = new ComposedElement(opacityInput);
        numberInputs = new ComposedElement(anchorInputs, positionInputs, orientationInputs, scaleInputs, opacityInputs);

        for(int i = numberInputs.getParts().size()-1; i >= 0; i--) {
            int yPos = this.height-5-10-(25*(numberInputs.getParts().size()-i));
            DelegatingElement button = keyframeButton(10, yPos, i);
            GuiString label = new GuiString(35, yPos + 6, Color.WHITE, labelStrings[i]);

            ComposedElement child = (ComposedElement)numberInputs.getParts().get(i);
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

        objectKeyframeTimeline = new GuiObjectKeyframeTimeline(timelineX, timelineY, timelineWidth, timelineHeight);
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
        addButton.yPosition = removeButton.yPosition = objectList.yPosition+objectList.height-25;

        allElements.addPart(addButton);
        disableElements.addPart(removeButton);

        allElements.addPart(disableElements);

        objectList.setSelectionIndex(objectList.getSelectionIndex()); // trigger an event for the SelectionListener

        initialized = true;
    }

    private void saveOnQuit() {
        List<CustomImageObject> objects = objectList.getCopyOfElements();

        if(objects.equals(initialObjects)) {
            return;
        }

        ReplayHandler.setCustomImageObjects(objects);

        if(objects.size() > 0) {
            try {
                File f = File.createTempFile(ReplayFile.ENTRY_CUSTOM_OBJECTS, "json");
                ReplayFileIO.write(new CustomObjectRepository(objects), f);
                ReplayMod.replayFileAppender.registerModifiedFile(f, ReplayFile.ENTRY_CUSTOM_OBJECTS, ReplayHandler.getReplayFile());
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            ReplayMod.replayFileAppender.registerModifiedFile(null, ReplayFile.ENTRY_CUSTOM_OBJECTS, ReplayHandler.getReplayFile());
        }
    }

    private void updateValuesForTransformation(Transformation transformation) {
        anchorXInput.setValueQuietly(transformation.getAnchor().getX());
        anchorYInput.setValueQuietly(transformation.getAnchor().getY());
        anchorZInput.setValueQuietly(transformation.getAnchor().getZ());

        positionXInput.setValueQuietly(transformation.getPosition().getX());
        positionYInput.setValueQuietly(transformation.getPosition().getY());
        positionZInput.setValueQuietly(transformation.getPosition().getZ());
        
        orientationXInput.setValueQuietly(transformation.getOrientation().getX());
        orientationYInput.setValueQuietly(transformation.getOrientation().getY());
        orientationZInput.setValueQuietly(transformation.getOrientation().getZ());

        scaleXInput.setValueQuietly(transformation.getScale().getX());
        scaleYInput.setValueQuietly(transformation.getScale().getY());
        scaleZInput.setValueQuietly(transformation.getScale().getZ());

        opacityInput.setValueQuietly(transformation.getOpacity());
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

        CustomImageObject selectedObject = objectList.getElement(objectList.getSelectionIndex());
        if(selectedObject != null) {
            selectedObject.setName(nameInput.getText().trim());
        }

        if(keyCode == Keyboard.KEY_DELETE) {
            if(objectKeyframeTimeline.getSelectedKeyframe() != null) {
                objectKeyframeTimeline.removeKeyframe(objectKeyframeTimeline.getSelectedKeyframeRow());
            }
        }

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

    @Override
    public void onGuiClosed() {
        saveOnQuit();
    }

    public abstract class NumberInputGroup implements NumberValueChangeListener {

        KeyframeList toModify;

        public void setUnderlyingKeyframeList(KeyframeList toModify) {
            this.toModify = toModify;
        }

    }

    public class NumberValueInputGroup extends NumberInputGroup {

        public NumberValueInputGroup(KeyframeList<NumberValue> toModify, GuiNumberInput input) {
            this.toModify = toModify;

            input.addValueChangeListener(this);
        }

        @Override
        public void onValueChange(double value) {
            NumberValue numberValue = new NumberValue(value);

            //if keyframe selected, overwrite its value
            if(toModify.contains(objectKeyframeTimeline.getSelectedKeyframe())) {
                objectKeyframeTimeline.getSelectedKeyframe().setValue(numberValue);
            } else {
                toModify.add(new Keyframe<NumberValue>(objectKeyframeTimeline.cursorPosition, numberValue));
            }
        }
    }

    public class NumberPositionInputGroup extends NumberInputGroup {

        public NumberPositionInputGroup(KeyframeList<Position> toModify, GuiNumberInput xInput, GuiNumberInput yInput, GuiNumberInput zInput) {
            this.toModify = toModify;
            this.xInput = xInput;
            this.yInput = yInput;
            this.zInput = zInput;

            xInput.addValueChangeListener(this);
            yInput.addValueChangeListener(this);
            zInput.addValueChangeListener(this);
        }

        private GuiNumberInput xInput, yInput, zInput;

        @Override
        public void onValueChange(double value) {
            Position position = new Position(xInput.getPreciseValue(), yInput.getPreciseValue(), zInput.getPreciseValue());

            //if keyframe selected, overwrite its value
            if(toModify.contains(objectKeyframeTimeline.getSelectedKeyframe())) {
                objectKeyframeTimeline.getSelectedKeyframe().setValue(position);
            } else {
                toModify.add(new Keyframe<Position>(objectKeyframeTimeline.cursorPosition, position));
            }
        }
    }

    public class GuiObjectKeyframeTimeline extends GuiTimeline {

        private static final int KEYFRAME_X = 74;
        private static final int KEYFRAME_Y = 35;
        private static final int KEYFRAME_SIZE = 5;

        @Getter @Setter
        private Transformations transformations;

        @Getter private int selectedKeyframeRow = -1;
        @Getter private Keyframe selectedKeyframe = null;

        private boolean dragging = false;

        public GuiObjectKeyframeTimeline(int x, int y, int width, int height) {
            super(x, y, width, height);

            this.zoom = 0.1;
            this.timelineLength = 10 * 60 * 1000;
            this.showMarkers = true;
        }

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY) {
            super.draw(mc, mouseX, mouseY);

            int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;

            long leftTime = Math.round(timeStart * timelineLength);
            long rightTime = Math.round((timeStart + zoom) * timelineLength);

            double segmentLength = timelineLength * zoom;

            if(transformations != null) {
                for(int i = 0; i < 5; i++) {
                    KeyframeList keyframes = transformations.getKeyframeListByID(i);

                    //Draw Keyframe logos
                    for(Keyframe kf : (List<Keyframe>) keyframes) {
                        drawKeyframe(kf, (int)(i * (height / 5f)) + 10, bodyWidth, leftTime, rightTime, segmentLength);
                    }
                }
            }
        }

        private int getKeyframeX(int timestamp, long leftTime, int bodyWidth, double segmentLength) {
            long positionInSegment = timestamp - leftTime;
            double fractionOfSegment = positionInSegment / segmentLength;
            return (int) (positionX + BORDER_LEFT + fractionOfSegment * bodyWidth);
        }

        private void drawKeyframe(Keyframe kf, int y, int bodyWidth, long leftTime, long rightTime, double segmentLength) {
            if (kf.getRealTimestamp() <= rightTime && kf.getRealTimestamp() >= leftTime) {
                int textureX = KEYFRAME_X;
                int textureY = KEYFRAME_Y;
                y = positionY+y;

                int keyframeX = getKeyframeX(kf.getRealTimestamp(), leftTime, bodyWidth, segmentLength);

                if (kf == selectedKeyframe) {
                    textureX += KEYFRAME_SIZE;
                }

                rect(keyframeX - 2, y, textureX, textureY, 5, 5);
            }
        }

        @Override
        public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
            if(!enabled) return false;
            super.mouseClick(mc, mouseX, mouseY, button);
            int time = (int) getTimeAt(mouseX, mouseY);
            if(time != -1)  {
                boolean clicked = false;
                
                int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));

                if(transformations != null) {
                    for(int i = 0; i < 5; i++) {
                        int upper = positionY + (int)(i * (height / 5f)) + 10;
                        int lower = upper + KEYFRAME_SIZE;
                        if(mouseY >= upper && mouseY <= lower) {
                            KeyframeList keyframes = transformations.getKeyframeListByID(i);
                            Keyframe closest = keyframes.getClosestKeyframeForTimestamp(time, tolerance);

                            selectedKeyframe = closest;

                            if(selectedKeyframe != null) {
                                selectedKeyframeRow = i;
                            } else {
                                selectedKeyframeRow = -1;
                            }

                            clicked = true;
                        }
                    }
                }

                if(!clicked) {
                    selectedKeyframe = null;
                    selectedKeyframeRow = -1;
                }

                cursorPosition = time;
                updateValuesForTransformation(getTransformations().getTransformationForTimestamp(cursorPosition));
                
                dragging = true;
                return true;
            }

            return false;
        }

        @Override
        public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
            if(!enabled) return;
            super.mouseDrag(mc, mouseX, mouseY, button);
            if(dragging) {
                int time = (int) getTimeAt(mouseX, mouseY);
                if(time != -1) {
                    if(selectedKeyframe != null) {
                        selectedKeyframe.setRealTimestamp(time);
                    }
                    cursorPosition = time;
                    updateValuesForTransformation(getTransformations().getTransformationForTimestamp(cursorPosition));
                }
            }
        }

        @Override
        public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
            if(!enabled) return;
            super.mouseRelease(mc, mouseX, mouseY, button);
            this.dragging = false;
        }

        public void addKeyframe(int line) {
            CustomImageObject currentObject = objectList.getElement(objectList.getSelectionIndex());
            if(currentObject != null) {
                KeyframeList list = currentObject.getTransformations().getKeyframeListByID(line);
                int timestamp = objectKeyframeTimeline.cursorPosition;

                Keyframe kf = null;
                
                switch(line) {
                    case 0:
                        
                        kf = new Keyframe(timestamp, new Position(
                                anchorXInput.getPreciseValue(),
                                anchorYInput.getPreciseValue(),
                                anchorZInput.getPreciseValue()));
                        break;
                    case 1:
                        kf = new Keyframe(timestamp, new Position(
                                positionXInput.getPreciseValue(),
                                positionYInput.getPreciseValue(),
                                positionZInput.getPreciseValue()));
                        break;
                    case 2:
                        kf = new Keyframe(timestamp, new Position(
                                orientationXInput.getPreciseValue(),
                                orientationYInput.getPreciseValue(),
                                orientationZInput.getPreciseValue()));
                        break;
                    case 3:
                        kf = new Keyframe(timestamp, new Position(
                                scaleXInput.getPreciseValue(),
                                scaleYInput.getPreciseValue(),
                                scaleZInput.getPreciseValue()));
                        break;
                    case 4:
                        kf = new Keyframe(timestamp, new NumberValue(
                                opacityInput.getPreciseValue()
                        ));
                        break;
                }

                list.add(kf);

                selectedKeyframe = kf;
                selectedKeyframeRow = line;
            }
        }

        public void removeKeyframe(int line) {
            if(selectedKeyframeRow == line) {
                CustomImageObject currentObject = objectList.getElement(objectList.getSelectionIndex());
                if(currentObject != null) {
                    KeyframeList list = currentObject.getTransformations().getKeyframeListByID(line);
                    list.remove(selectedKeyframe);
                    selectedKeyframe = null;
                    selectedKeyframeRow = -1;
                }
            }
        }
    }
}
