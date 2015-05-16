package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.client.SearchQuery;
import eu.crushedpixel.replaymod.api.client.holders.FileInfo;
import eu.crushedpixel.replaymod.api.client.pagination.DownloadedFilePagination;
import eu.crushedpixel.replaymod.api.client.pagination.FavoritedFilePagination;
import eu.crushedpixel.replaymod.api.client.pagination.Pagination;
import eu.crushedpixel.replaymod.api.client.pagination.SearchPagination;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.GuiLoadingListEntry;
import eu.crushedpixel.replaymod.gui.elements.GuiReplayListEntry;
import eu.crushedpixel.replaymod.gui.replayviewer.GuiReplayViewer;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
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
    private ReplayFileList currentList;
    private Tab currentTab = Tab.RECENT_FILES;
    private GuiButton loadButton, favButton, likeButton, dislikeButton;
    private List<GuiButton> replayButtonBar, bottomBar, topBar;
    private GuiLoadingListEntry loadingListEntry;

    public static GuiYesNo getYesNoGui(GuiYesNoCallback p_152129_0_, int p_152129_2_) {
        String s1 = I18n.format("replaymod.gui.center.logoutcallback");
        GuiYesNo guiyesno = new GuiYesNo(p_152129_0_, s1, "", I18n.format("replaymod.gui.logout"),
                I18n.format("replaymod.gui.cancel"), p_152129_2_);
        return guiyesno;
    }

    private boolean initialized = false;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        if(!initialized) {
            if(!AuthenticationHandler.isAuthenticated()) {
                mc.displayGuiScreen(new GuiLoginPrompt(new GuiMainMenu(), this));
            }

            //Top Button Bar
            topBar = new ArrayList<GuiButton>();

            GuiButton recentButton = new GuiButton(GuiConstants.CENTER_RECENT_BUTTON, 20, 30, I18n.format("replaymod.gui.center.top.recent"));
            topBar.add(recentButton);

            GuiButton bestButton = new GuiButton(GuiConstants.CENTER_BEST_BUTTON, 20, 30, I18n.format("replaymod.gui.center.top.best"));
            topBar.add(bestButton);

            GuiButton downloadedReplayButton = new GuiButton(GuiConstants.CENTER_DOWNLOADED_REPLAYS_BUTTON, 20, 30, I18n.format("replaymod.gui.center.top.downloaded"));
            downloadedReplayButton.enabled = AuthenticationHandler.isAuthenticated();
            topBar.add(downloadedReplayButton);

            GuiButton favoritedReplayButton = new GuiButton(GuiConstants.CENTER_FAVORITED_REPLAYS_BUTTON, 20, 30, I18n.format("replaymod.gui.center.top.favorited"));
            favoritedReplayButton.enabled = AuthenticationHandler.isAuthenticated();
            topBar.add(favoritedReplayButton);

            GuiButton searchButton = new GuiButton(GuiConstants.CENTER_SEARCH_BUTTON, 20, 30, I18n.format("replaymod.gui.center.top.search"));
            topBar.add(searchButton);

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

            //Bottom Button Bar (dat alliteration)
            bottomBar = new ArrayList<GuiButton>();

            GuiButton exitButton = new GuiButton(GuiConstants.CENTER_BACK_BUTTON, 20, 20, I18n.format("replaymod.gui.mainmenu"));
            bottomBar.add(exitButton);

            GuiButton managerButton = new GuiButton(GuiConstants.CENTER_MANAGER_BUTTON, 20, 20, I18n.format("replaymod.gui.replayviewer"));
            bottomBar.add(managerButton);

            GuiButton logoutButton = new GuiButton(GuiConstants.CENTER_LOGOUT_BUTTON, 20, 20, I18n.format("replaymod.gui.logout"));
            bottomBar.add(logoutButton);

            showOnlineRecent();
        }

        int i = 0;
        for(GuiButton b : topBar) {
            int w = this.width - 30;
            int w2 = w / topBar.size();

            int x = 15 + (w2 * i);
            b.xPosition = x + 2;
            b.yPosition = 20;
            b.width = w2 - 4;

            buttonList.add(b);

            i++;
        }

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

        initialized = true;
    }

    public void elementSelected(int index) {
        if(index < 0) {
            for(GuiButton b : replayButtonBar) {
                b.enabled = false;
            }
            return;
        }
        GuiReplayListEntry entry = (GuiReplayListEntry)currentList.getListEntry(index);
        FileInfo info = entry.getFileInfo();
        if(info != null) {
            boolean downloaded = ReplayMod.downloadedFileHandler.getFileForID(info.getId()) != null;
            loadButton.displayString = downloaded ? I18n.format("replaymod.gui.load") : I18n.format("replaymod.gui.download");
            loadButton.enabled = true;

            boolean favorited = ReplayMod.favoritedFileHandler.isFavorited(info.getId());
            favButton.displayString = favorited ? I18n.format("replaymod.gui.center.unfavorite") : I18n.format("replaymod.gui.center.favorite");
            favButton.enabled = true;
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
        } else if(button.id == GuiConstants.CENTER_DOWNLOADED_REPLAYS_BUTTON) {
            showDownloadedFiles();
        } else if(button.id == GuiConstants.CENTER_FAVORITED_REPLAYS_BUTTON) {
            showFavoritedFiles();
        } else if(button.id == GuiConstants.CENTER_SEARCH_BUTTON) {

        } else if(button.id == GuiConstants.CENTER_LOAD_REPLAY_BUTTON) {
            GuiReplayListEntry entry = (GuiReplayListEntry)currentList.getListEntry(currentList.selected);
            FileInfo info = entry.getFileInfo();
            if(info != null) {
                File f = ReplayMod.downloadedFileHandler.getFileForID(info.getId());
                if(f == null) {
                    f = ReplayMod.downloadedFileHandler.downloadFileForID(info.getId());
                }
                try {
                    ReplayHandler.startReplay(f);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        } else if(button.id == GuiConstants.CENTER_FAV_REPLAY_BUTTON) {
            GuiReplayListEntry entry = (GuiReplayListEntry)currentList.getListEntry(currentList.selected);
            FileInfo info = entry.getFileInfo();
            if(info != null) {
                boolean favorited = ReplayMod.favoritedFileHandler.isFavorited(info.getId());
                try {
                    if(favorited) ReplayMod.favoritedFileHandler.removeFromFavorites(info.getId());
                    else ReplayMod.favoritedFileHandler.addToFavorites(info.getId());
                } catch(Exception e) {
                    e.printStackTrace();
                }
                elementSelected(currentList.selected);
            }
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

    private void updateCurrentList(Pagination pagination) {
        elementSelected(-1);
        currentList = new ReplayFileList(mc, width, height, 50, height - 70, 36, this);
        loadingListEntry = new GuiLoadingListEntry(currentList);
        currentList.addEntry(loadingListEntry);

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
                currentList.addEntry(currentList.getEntries().size()-1, new GuiReplayListEntry(currentList, i, tmp));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        currentList.removeEntry(loadingListEntry);
    }

    private Thread currentListLoader;

    private void cancelCurrentListLoader() {
        if(currentListLoader != null && currentListLoader.isAlive()) currentListLoader.stop();
    }

    public void showOnlineRecent() {
        cancelCurrentListLoader();
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(new SearchPagination(recentFileSearchQuery));
                currentTab = Tab.RECENT_FILES;
            }
        });
        currentListLoader.start();
    }

    public void showOnlineBest() {
        cancelCurrentListLoader();
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(new SearchPagination(bestFileSearchQuery));
                currentTab = Tab.BEST_FILES;
            }
        });
        currentListLoader.start();
    }

    public void showDownloadedFiles() {
        cancelCurrentListLoader();
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(new DownloadedFilePagination());
                currentTab = Tab.DOWNLOADED_FILES;
            }
        });
        currentListLoader.start();
    }

    public void showFavoritedFiles() {
        cancelCurrentListLoader();
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                ReplayMod.favoritedFileHandler.reloadFavorites();
                updateCurrentList(new FavoritedFilePagination());
                currentTab = Tab.FAVORITED_FILES;
            }
        });
        currentListLoader.start();
    }

    private enum Tab {
        RECENT_FILES, BEST_FILES, DOWNLOADED_FILES, FAVORITED_FILES, SEARCH;
    }
}
