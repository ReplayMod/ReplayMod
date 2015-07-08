package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.assets.CustomImageObject;
import eu.crushedpixel.replaymod.assets.ReplayAsset;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.Point;

import java.awt.*;
import java.io.IOException;

public class GuiObjectManager extends GuiScreen {

    private boolean initialized = false;

    private KeyframeList[] keyframeLists;

    private GuiEntryList<CustomImageObject> objectList;
    private GuiAdvancedButton addButton, removeButton;

    private GuiAdvancedTextField nameInput;

    private GuiString dropdownLabel;
    private GuiDropdown<String> assetDropdown;

    private GuiDraggingNumberInput anchorXInput, anchorYInput, anchorZInput;
    private GuiDraggingNumberInput positionXInput, positionYInput, positionZInput;
    private GuiDraggingNumberInput scaleXInput, scaleYInput, scaleZInput;
    private GuiDraggingNumberInput orientationXInput, orientationYInput, orientationZInput;
    private GuiDraggingNumberInput opacityInput;

    private GuiObjectKeyframeTimeline objectKeyframeTimeline;
    private GuiScrollbar timelineScrollbar;

    private GuiTexturedButton zoomInButton, zoomOutButton;

    private ComposedElement anchorInputs, positionInputs, scaleInputs, orientationInputs, opacityInputs, numberInputs;
    private ComposedElement allElements;

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
            assetDropdown = new GuiDropdown<String>(fontRendererObj, 0, 0, 0, 5);
            if(ReplayHandler.getAssetRepository().getCopyOfReplayAssets().isEmpty()) {
                assetDropdown.addElement(I18n.format("replaymod.gui.assets.emptylist"));
            } else {
                for(ReplayAsset asset : ReplayHandler.getAssetRepository().getCopyOfReplayAssets()) {
                    assetDropdown.addElement(asset.getDisplayString());
                }
            }

            objectList = new GuiEntryList<CustomImageObject>(fontRendererObj, 0, 0, 0, 0);
            objectList.setEmptyMessage(I18n.format("replaymod.gui.objects.empty"));
            
            addButton = new GuiAdvancedButton(0, 0, 0, 20, I18n.format("replaymod.gui.add"), new Runnable() {
                @Override
                public void run() {

                }
            }, null);

            removeButton = new GuiAdvancedButton(0, 0, 0, 20, I18n.format("replaymod.gui.remove"), new Runnable() {
                @Override
                public void run() {

                }
            }, null);

            nameInput = new GuiAdvancedTextField(fontRendererObj, 0, 0, 0, 20);
            nameInput.hint = I18n.format("replaymod.gui.objects.properties.name");

            keyframeLists = new KeyframeList[5];
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

            allElements.addPart(child);
        }

        int timelineX = anchorZInput.xPosition + anchorZInput.width + 5;
        int timelineY = anchorZInput.yPosition;

        int timelineWidth = this.width-10-timelineX;
        int timelineHeight = this.height-10-10-timelineY;

        objectKeyframeTimeline = new GuiObjectKeyframeTimeline(timelineX, timelineY, timelineWidth, timelineHeight, keyframeLists);
        allElements.addPart(objectKeyframeTimeline);

        timelineScrollbar = new GuiScrollbar(timelineX, this.height-15, timelineWidth-19) {
            @Override
            public void dragged() {
                objectKeyframeTimeline.setLeftPosition((float)sliderPosition);
            }
        };
        allElements.addPart(timelineScrollbar);

        zoomInButton = GuiReplayOverlay.texturedButton(width - 28, this.height-15, 40, 20, 9, new Runnable() {
            @Override
            public void run() {
                objectKeyframeTimeline.setZoomScale(Math.max(0.025f, objectKeyframeTimeline.getZoomScale() - ZOOM_STEPS));
            }
        }, "replaymod.gui.ingame.menu.zoomin");

        zoomOutButton = GuiReplayOverlay.texturedButton(width - 18, this.height-15, 40, 30, 9, new Runnable() {

            @Override
            public void run() {
                objectKeyframeTimeline.setZoomScale(Math.min(1f, objectKeyframeTimeline.getZoomScale() + ZOOM_STEPS));
                objectKeyframeTimeline.setLeftPosition(Math.min(objectKeyframeTimeline.getLeftPos(), 1f - objectKeyframeTimeline.getZoomScale()));
            }
        }, "replaymod.gui.ingame.menu.zoomout");

        allElements.addPart(zoomInButton);
        allElements.addPart(zoomOutButton);

        objectList.xPosition = 11;
        objectList.yPosition = 11;
        objectList.width = width/2 - 11 - 5;

        int visibleElements = (int)((anchorXInput.yPosition-5 - objectList.yPosition)/14f);
        objectList.setVisibleElements(visibleElements);

        allElements.addPart(objectList);

        nameInput.xPosition = width/2 + 5;
        nameInput.yPosition = objectList.yPosition;
        nameInput.width = objectList.width;

        allElements.addPart(nameInput);

        dropdownLabel.positionX = (width/2)+5;
        int strWidth = fontRendererObj.getStringWidth(I18n.format("replaymod.gui.assets.filechooser")+": ");

        assetDropdown.xPosition = dropdownLabel.positionX+strWidth+5;
        assetDropdown.yPosition = objectList.yPosition+25;
        dropdownLabel.positionY = assetDropdown.yPosition + 6;
        assetDropdown.width = (objectList.width-strWidth-5);

        allElements.addPart(dropdownLabel);
        allElements.addPart(assetDropdown);

        addButton.xPosition = nameInput.xPosition;
        addButton.width = removeButton.width = nameInput.width/2 - 2;
        removeButton.xPosition = addButton.xPosition + nameInput.width/2 + 2;
        addButton.yPosition = removeButton.yPosition = objectList.yPosition+objectList.height-20;

        allElements.addPart(addButton);
        allElements.addPart(removeButton);

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

    public class GuiObjectKeyframeTimeline implements GuiElement {

        private KeyframeList[] keyframeLists;

        public int x, y, width, height;

        @Getter @Setter
        private int timestamp;

        @Getter @Setter
        private float zoomScale;

        @Getter @Setter
        private float leftPos;

        private int timelineLength = 10 * 60 * 1000;

        public GuiObjectKeyframeTimeline(int x, int y, int width, int height, KeyframeList... keyframeLists) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;

            this.keyframeLists = keyframeLists;
        }

        public void setLeftPosition(float leftPos) {
            this.leftPos = leftPos;
        }

        private boolean enabled = true;

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY) {
            drawRect(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, -6250336);
            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, -16777216);

            int count = keyframeLists.length;
            int i = 0;
            for(KeyframeList list : keyframeLists) {
                int yPos = (int)(((float)i/count)*(height+2));
                int h = (int)(1f/count)*(height+2);

                if(i < count)
                    drawHorizontalLine(x, x+width, y+yPos+h-1, -6250336);
                i++;
            }
        }

        @Override
        public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {

        }

        @Override
        public boolean isHovering(int mouseX, int mouseY) {
            return false;
        }

        @Override
        public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {

        }

        @Override
        public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {

        }

        @Override
        public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {

        }

        @Override
        public void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode) {

        }

        @Override
        public void tick(Minecraft mc) {

        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
