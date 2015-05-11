package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.client.SearchPagination;
import eu.crushedpixel.replaymod.api.client.SearchQuery;
import eu.crushedpixel.replaymod.api.client.holders.FileInfo;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.GuiReplayListEntry;
import eu.crushedpixel.replaymod.gui.replayviewer.GuiReplayViewer;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiReplayCenter extends GuiScreen implements GuiYesNoCallback {

    private static final SearchQuery recentFileSearchQuery = new SearchQuery(false, null, null, null, null, null,
            null, null, null, null);
    private static final SearchQuery bestFileSearchQuery = new SearchQuery(true, null, null, null, null, null,
            null, null, null, null);
    private static final int LOGOUT_CALLBACK_ID = 1;
    private final SearchPagination recentFilePagination = new SearchPagination(recentFileSearchQuery);
    private final SearchPagination bestFilePagination = new SearchPagination(bestFileSearchQuery);
    private ReplayFileList currentList;
    private ReplayFileList recentFileList, bestFileList, myFileList, searchFileList;
    private Tab currentTab = Tab.RECENT_FILES;
    private SearchPagination myFilePagination;
    private GuiButton loadButton, favButton, likeButton, dislikeButton;
    private List<GuiButton> replayButtonBar;

    public static GuiYesNo getYesNoGui(GuiYesNoCallback p_152129_0_, int p_152129_2_) {
        String s1 = I18n.format("replaymod.gui.center.logoutcallback");
        GuiYesNo guiyesno = new GuiYesNo(p_152129_0_, s1, "", I18n.format("replaymod.gui.logout"),
                I18n.format("replaymod.gui.cancel"), p_152129_2_);
        return guiyesno;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        if(AuthenticationHandler.isAuthenticated()) {
            SearchQuery query = new SearchQuery();
            query.auth = AuthenticationHandler.getKey();
            query.order = false;
            myFilePagination = new SearchPagination(query);
        }

        //Top Button Bar
        List<GuiButton> buttonBar = new ArrayList<GuiButton>();

        GuiButton recentButton = new GuiButton(GuiConstants.CENTER_RECENT_BUTTON, 20, 30, I18n.format("replaymod.gui.center.newest"));
        buttonBar.add(recentButton);

        GuiButton bestButton = new GuiButton(GuiConstants.CENTER_BEST_BUTTON, 20, 30, I18n.format("replaymod.gui.center.best"));
        buttonBar.add(bestButton);

        GuiButton ownReplayButton = new GuiButton(GuiConstants.CENTER_MY_REPLAYS_BUTTON, 20, 30, I18n.format("replaymod.gui.center.my"));
        ownReplayButton.enabled = AuthenticationHandler.isAuthenticated();
        buttonBar.add(ownReplayButton);

        GuiButton searchButton = new GuiButton(GuiConstants.CENTER_SEARCH_BUTTON, 20, 30, I18n.format("replaymod.gui.center.search"));
        buttonBar.add(searchButton);

        int i = 0;
        for(GuiButton b : buttonBar) {
            int w = this.width - 30;
            int w2 = w / buttonBar.size();

            int x = 15 + (w2 * i);
            b.xPosition = x + 2;
            b.yPosition = 20;
            b.width = w2 - 4;

            buttonList.add(b);

            i++;
        }

        //Replay specific actions (load, rate, etc)
        replayButtonBar = new ArrayList<GuiButton>();

        loadButton = new GuiButton(GuiConstants.CENTER_LOAD_REPLAY_BUTTON, 20, 30, I18n.format("replaymod.gui.download"));
        replayButtonBar.add(loadButton);

        favButton = new GuiButton(GuiConstants.CENTER_FAV_REPLAY_BUTTON, 20, 30, I18n.format("replaymod.gui.center.favorite"));
        replayButtonBar.add(favButton);

        likeButton = new GuiButton(GuiConstants.CENTER_LIKE_REPLAY_BUTTON, 20, 30, I18n.format("replaymod.gui.like"));
        replayButtonBar.add(likeButton);

        dislikeButton = new GuiButton(GuiConstants.CENTER_DISLIKE_REPLAY_BUTTON, 20, 30, I18n.format("replaymod.gui.dislike"));
        replayButtonBar.add(dislikeButton);

        i = 0;
        for(GuiButton b : replayButtonBar) {
            int w = this.width - 30;
            int w2 = w / replayButtonBar.size();

            int x = 15 + (w2 * i);
            b.xPosition = x + 2;
            b.yPosition = height - 55;
            b.width = w2 - 4;

            b.enabled = false;

            buttonList.add(b);

            i++;
        }

        //Bottom Button Bar (dat alliteration)
        List<GuiButton> bottomBar = new ArrayList<GuiButton>();

        GuiButton exitButton = new GuiButton(GuiConstants.CENTER_BACK_BUTTON, 20, 20, I18n.format("replaymod.gui.mainmenu"));
        bottomBar.add(exitButton);

        GuiButton managerButton = new GuiButton(GuiConstants.CENTER_MANAGER_BUTTON, 20, 20, I18n.format("replaymod.gui.replayviewer"));
        bottomBar.add(managerButton);

        GuiButton logoutButton = new GuiButton(GuiConstants.CENTER_LOGOUT_BUTTON, 20, 20, I18n.format("replaymod.gui.logout"));
        bottomBar.add(logoutButton);

        i = 0;
        for(GuiButton b : bottomBar) {
            int w = this.width - 30;
            int w2 = w / bottomBar.size();

            int x = 15 + (w2 * i);
            b.xPosition = x + 2;
            b.yPosition = height - 30;
            b.width = w2 - 4;

            buttonList.add(b);

            i++;
        }

        showOnlineRecent();
    }

    public void elementSelected(int index) {
        GuiReplayListEntry entry = currentList.getListEntry(index);
        FileInfo info = entry.getFileInfo();
        if(info != null) {
            boolean downloaded = false;
            loadButton.displayString = downloaded ? I18n.format("replaymod.gui.load") : I18n.format("replaymod.gui.download");
            loadButton.enabled = true;
        } else {
            for(GuiButton b : replayButtonBar) {
                b.enabled = false;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws java.io.IOException {
        if(!button.enabled) return;
        if(button.id == GuiConstants.CENTER_BACK_BUTTON) {
            mc.displayGuiScreen(new GuiMainMenu());
        } else if(button.id == GuiConstants.CENTER_LOGOUT_BUTTON) {
            mc.displayGuiScreen(getYesNoGui(this, LOGOUT_CALLBACK_ID));
        } else if(button.id == GuiConstants.CENTER_MANAGER_BUTTON) {
            mc.displayGuiScreen(new GuiReplayViewer());
        } else if(button.id == GuiConstants.CENTER_RECENT_BUTTON) {
            showOnlineRecent();
        } else if(button.id == GuiConstants.CENTER_BEST_BUTTON) {
            showOnlineBest();
        } else if(button.id == GuiConstants.CENTER_MY_REPLAYS_BUTTON) {
            showOnlineOwnFiles();
        } else if(button.id == GuiConstants.CENTER_SEARCH_BUTTON) {

        } else if(button.id == GuiConstants.CENTER_LOAD_REPLAY_BUTTON) {

        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if(id == LOGOUT_CALLBACK_ID) {
            if(result) {
                mc.addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        AuthenticationHandler.logout();
                        mc.displayGuiScreen(new GuiMainMenu());
                    }
                });
            } else {
                mc.displayGuiScreen(this);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.replaycenter"), this.width / 2, 8, Color.WHITE.getRGB());

        if(currentList != null) {
            currentList.drawScreen(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if(currentList != null) {
            this.currentList.handleMouseInput();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if(currentList != null) {
            this.currentList.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if(currentList != null) {
            this.currentList.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void updateCurrentList(ReplayFileList list, SearchPagination pagination) {
        currentList = list;
        if(currentList == null) {
            currentList = new ReplayFileList(mc, width, height, 50, height - 70, 36, this);
        } else {
            currentList.clearEntries();
            currentList.width = width;
            currentList.height = height;
            currentList.top = 50;
            currentList.bottom = height - 70;
        }

        if(pagination.getLoadedPages() < 0) {
            pagination.fetchPage();
        }

        for(FileInfo i : pagination.getFiles()) {
            try {
                File tmp = null;
                if(i.hasThumbnail()) {
                    tmp = File.createTempFile("thumb_online_" + i.getId(), "jpg");
                    ReplayMod.apiClient.downloadThumbnail(i.getId(), tmp);
                }
                currentList.addEntry(i, tmp);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void showOnlineRecent() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(recentFileList, recentFilePagination);
                currentTab = Tab.RECENT_FILES;
            }
        });
        t.start();
    }

    public void showOnlineBest() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(bestFileList, bestFilePagination);
                currentTab = Tab.BEST_FILES;
            }
        });
        t.start();
    }

    public void showOnlineOwnFiles() {
        if(!AuthenticationHandler.isAuthenticated() || myFilePagination == null) return;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(myFileList, myFilePagination);
                currentTab = Tab.MY_FILES;
            }
        });
        t.start();
    }

    private enum Tab {
        RECENT_FILES, BEST_FILES, MY_FILES, SEARCH;
    }
}
