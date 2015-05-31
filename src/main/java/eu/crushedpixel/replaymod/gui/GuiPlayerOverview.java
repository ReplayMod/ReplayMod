package eu.crushedpixel.replaymod.gui;

import com.mojang.realmsclient.util.Pair;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.PlayerVisibility;
import eu.crushedpixel.replaymod.registry.PlayerHandler;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.SkinProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GuiPlayerOverview extends GuiScreen implements GuiReplayOverlay.NoOverlay {

    public static boolean defaultSave = false;

    private List<Pair<EntityPlayer, ResourceLocation>> players;
    private List<GuiCheckBox> checkBoxes;

    private GuiCheckBox hideAllBox, showAllBox;
    private GuiCheckBox rememberHidden;

    private boolean initialized = false;

    private int playerCount;
    private int upperPlayer = 0;

    private int lowerBound;

    private boolean drag = false;
    private int lastY = 0;
    private int fitting = 0;

    private final Minecraft mc = Minecraft.getMinecraft();

    private final String screenTitle = I18n.format("replaymod.input.playeroverview");

    public GuiPlayerOverview(List<EntityPlayer> players) {
        Collections.sort(players, new PlayerComparator());

        this.players = new ArrayList<Pair<EntityPlayer, ResourceLocation>>();
        this.checkBoxes = new ArrayList<GuiCheckBox>();

        for(final EntityPlayer p : players) {
            final ResourceLocation loc = SkinProvider.getResourceLocationForPlayerUUID(p.getUniqueID());
            this.players.add(Pair.of(p, loc));
        }

        playerCount = players.size();

        ReplayMod.replaySender.setReplaySpeed(0);
    }

    private boolean isSpectator(EntityPlayer e) {
        return e.isInvisible() && e.getActivePotionEffect(Potion.invisibility) == null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {

        if(fitting < playerCount) {
            float visiblePerc = (float) fitting / (float) playerCount;

            int h = this.height - 32 - 32;
            int offset = Math.round((upperPlayer / (fitting)) * visiblePerc * h);

            int lower = Math.round(32 + offset + (h * visiblePerc)) - 2;

            int k2 = (int) (this.width * 0.3);

            if(mouseX >= k2 - 16 && mouseX <= k2 - 12 && mouseY >= 32 - 2 + offset && mouseY <= lower) {
                lastY = mouseY;
                drag = true;
                return;
            }
        }
        int k2 = (int) (this.width * 0.3);

        if(mouseX >= k2 && mouseX <= (this.width * 0.6) && mouseY >= upperBound && mouseY <= lowerBound) {
            int off = mouseY - upperBound;
            int p = (off / 21) + upperPlayer;
            ReplayHandler.spectateEntity(players.get(p).first());
            mc.displayGuiScreen(null);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY,
                                  int clickedMouseButton, long timeSinceLastClick) {

        if(drag) {
            float step = 1f / (float) playerCount;

            int diff = mouseY - lastY;
            int h = this.height - 32 - 32;

            float percDiff = (float) diff / (float) h;
            if(Math.abs(percDiff) > Math.abs(step)) {
                int s = (int) (percDiff / step);
                lastY = mouseY;
                upperPlayer += s;
                if(upperPlayer > playerCount - fitting) {
                    upperPlayer = playerCount - fitting;
                } else if(upperPlayer < 0) {
                    upperPlayer = 0;
                }
            }
        }

        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        drag = false;

        for(GuiCheckBox checkBox : checkBoxes) {
            checkBox.mouseReleased(mouseX, mouseY);
        }

        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void initGui() {
        upperPlayer = 0;
        lowerBound = this.height - 10;

        int i = 0;
        for(GuiCheckBox checkBox : checkBoxes) {
            checkBox.xPosition = (int)(this.width*0.7)-5;
            buttonList.add(checkBox);
            i++;
            if(i >= fitting) break;
        }

        if(!initialized) {
            hideAllBox = new GuiCheckBox(GuiConstants.PLAYER_OVERVIEW_HIDE_ALL, 0, 0, "", false);
            showAllBox = new GuiCheckBox(GuiConstants.PLAYER_OVERVIEW_SHOW_ALL, 0, 0, "", true);
            rememberHidden = new GuiCheckBox(GuiConstants.PLAYER_OVERVIEW_REMEMBER, 0, 0, I18n.format("replaymod.gui.playeroverview.remembersettings"), defaultSave);
        }

        hideAllBox.xPosition = (int)(this.width*0.7)-5;
        showAllBox.xPosition = (int)(this.width*0.7)-20;
        hideAllBox.yPosition = showAllBox.yPosition = 45;

        rememberHidden.xPosition = (int)(this.width*0.3);
        rememberHidden.yPosition = 45;

        buttonList.add(hideAllBox);
        buttonList.add(showAllBox);
        buttonList.add(rememberHidden);

        initialized = true;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(button == showAllBox) {
            showAllBox.setIsChecked(true);
            for(Pair<EntityPlayer, ResourceLocation> p : players) {
                PlayerHandler.showPlayer(p.first());
            }
        } else if(button == hideAllBox) {
            hideAllBox.setIsChecked(false);
            for(Pair<EntityPlayer, ResourceLocation> p : players) {
                PlayerHandler.hidePlayer(p.first());
            }
        }

        if(!(button instanceof GuiCheckBox)) return;
        if(button.id >= fitting) return;
        if(!checkBoxes.contains(button)) return;
        PlayerHandler.setIsVisible(players.get(upperPlayer + button.id).first(), ((GuiCheckBox)button).isChecked());
    }

    private static final int upperBound = 65;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawCenteredString(fontRendererObj, screenTitle, this.width / 2, 5, Color.WHITE.getRGB());
        int k2 = (int)(this.width * 0.3);
        int l2 = upperBound;

        drawGradientRect(k2 - 20, 20, (int) (this.width * 0.7) + 20, this.height - 30 - 2 + 10, -1072689136, -804253680);

        drawString(fontRendererObj, I18n.format("replaymod.gui.playeroverview.spectate"), k2 - 10, 30, Color.WHITE.getRGB());

        String visibleString = I18n.format("replaymod.gui.playeroverview.visible");
        drawString(fontRendererObj, visibleString, (int) (this.width * 0.7) + 10 - fontRendererObj.getStringWidth(visibleString), 30, Color.WHITE.getRGB());

        fitting = 0;

        int sk = 0;
        for(Pair<EntityPlayer, ResourceLocation> p : players) {
            if(sk < upperPlayer) {
                sk++;
                continue;
            }
            boolean spec = isSpectator(p.first());

            this.drawString(fontRendererObj, p.first().getName(), k2 + 16 + 5, l2 + 8 - (fontRendererObj.FONT_HEIGHT / 2),
                    spec ? Color.DARK_GRAY.getRGB() : Color.WHITE.getRGB());

            mc.getTextureManager().bindTexture(p.second());

            drawScaledCustomSizeModalRect(k2, l2, 8.0F, 8.0F, 8, 8, 16, 16, 64.0F, 64.0F);
            if(p.first().func_175148_a(EnumPlayerModelParts.HAT))
                Gui.drawScaledCustomSizeModalRect(k2, l2, 40.0F, 8.0F, 8, 8, 16, 16, 64.0F, 64.0F);

            GlStateManager.resetColor();
            if(fitting >= checkBoxes.size()) {
                checkBoxes.add(new GuiCheckBox(checkBoxes.size(), (int)(this.width*0.7)-5, l2+3, "", true));
                buttonList.add(checkBoxes.get(checkBoxes.size() - 1));
            }
            checkBoxes.get(fitting).setIsChecked(!PlayerHandler.isHidden(p.first().getEntityId()));

            l2 += 16 + 5;
            fitting++;
            if(l2 + 32 > lowerBound) {
                break;
            }
        }

        int dw = Mouse.getDWheel();
        if(dw > 0) {
            dw = -1;
        } else if(dw < 0) {
            dw = 1;
        }

        upperPlayer = Math.max(Math.min(upperPlayer + dw, playerCount - fitting), 0);

        if(fitting < playerCount) {
            float visiblePerc = ((float) fitting) / playerCount;
            int barHeight = (int) (visiblePerc * (height - 32 - 32));

            float posPerc = ((float) upperPlayer) / playerCount;
            int barY = (int) (posPerc * (height - 32 - 32));

            drawRect(k2 - 18, upperBound - 2, k2 - 10, this.height - 30 - 2, Color.BLACK.getRGB());
            drawRect(k2 - 16, upperBound+2 - 2 + barY, k2 - 12, 30+2 - 1 + barY + barHeight, Color.LIGHT_GRAY.getRGB());
        }

        int i = 0;
        for(GuiCheckBox checkBox : checkBoxes) {
            checkBox.drawButton(mc, mouseX, mouseY);
            i++;
            if(i >= fitting) break;
        }

        hideAllBox.drawButton(mc, mouseX, mouseY);
        showAllBox.drawButton(mc, mouseX, mouseY);
        rememberHidden.drawButton(mc, mouseX, mouseY);

        if(hideAllBox.isMouseOver()) {
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, I18n.format("replaymod.gui.playeroverview.hideall"), this, Color.WHITE.getRGB());
        }

        if(showAllBox.isMouseOver()) {
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, I18n.format("replaymod.gui.playeroverview.showall"), this, Color.WHITE.getRGB());
        }

        if(rememberHidden.isMouseOver()) {
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, I18n.format("replaymod.gui.playeroverview.remembersettings.description"), this, Color.WHITE.getRGB());
        }

        //this is necessary to reset the GL parameters for further GUI rendering
        GlStateManager.enableBlend();
    }

    private PlayerVisibility getVisibilityInstance() {
        return new PlayerVisibility(PlayerHandler.getHiddenPlayers());
    }


    private void saveOnQuit() {
        if(rememberHidden.isChecked()) {
            try {
                File f = File.createTempFile(ReplayFile.ENTRY_VISIBILITY, "json");
                ReplayFileIO.writePlayerVisibilityToFile(getVisibilityInstance(), f);
                ReplayMod.replayFileAppender.registerModifiedFile(f, ReplayFile.ENTRY_VISIBILITY, ReplayHandler.getReplayFile());
                System.out.println("here");
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            ReplayMod.replayFileAppender.registerModifiedFile(null, ReplayFile.ENTRY_VISIBILITY, ReplayHandler.getReplayFile());
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if(keyCode == Keyboard.KEY_ESCAPE) {
            saveOnQuit();
            super.keyTyped(typedChar, keyCode);
        }
    }

    private class PlayerComparator implements Comparator<EntityPlayer> {

        @Override
        public int compare(EntityPlayer o1, EntityPlayer o2) {
            if(isSpectator(o1) && !isSpectator(o2)) {
                return 1;
            } else if(isSpectator(o2) && !isSpectator(o1)) {
                return -1;
            } else {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        }

    }
}
