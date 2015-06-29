package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.replay.SearchQuery;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;
import eu.crushedpixel.replaymod.api.replay.holders.Rating;
import eu.crushedpixel.replaymod.api.replay.pagination.DownloadedFilePagination;
import eu.crushedpixel.replaymod.api.replay.pagination.FavoritedFilePagination;
import eu.crushedpixel.replaymod.api.replay.pagination.Pagination;
import eu.crushedpixel.replaymod.api.replay.pagination.SearchPagination;
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
    private GuiButton loadButton, favButton, likeButton, dislikeButton;
    private List<GuiButton> replayButtonBar, bottomBar, topBar;

    public static GuiYesNo getYesNoGui(GuiYesNoCallback p_152129_0_, int p_152129_2_) {
        String s1 = I18n.format("replaymod.gui.center.logoutcallback");
        return new GuiYesNo(p_152129_0_, s1, "", I18n.format("replaymod.gui.logout"),
                I18n.format("replaymod.gui.cancel"), p_152129_2_);
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
            disableTopBarButton(GuiConstants.CENTER_RECENT_BUTTON);
        }

        if(currentList != null) {
            currentList.height = height-60;
        }

        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = this.buttonList;

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
            //if not downloaded, disable favorising
            favButton.enabled = favorited || downloaded;

            likeButton.enabled = dislikeButton.enabled = downloaded;
            Rating.RatingType rating = ReplayMod.ratedFileHandler.getRating(info.getId());

            likeButton.displayString = I18n.format("replaymod.gui.like");
            dislikeButton.displayString = I18n.format("replaymod.gui.dislike");

            if(rating == Rating.RatingType.LIKE) {
                likeButton.displayString = I18n.format("replaymod.gui.removelike");
            } else if(rating == Rating.RatingType.DISLIKE) {
                dislikeButton.displayString = I18n.format("replaymod.gui.removedislike");
            }

        } else {
            for(GuiButton b : replayButtonBar) {
                b.enabled = false;
            }
        }
    }

    private void disableTopBarButton(int id) {
        for(GuiButton b : topBar) {
            b.enabled = b.id != id;
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
            disableTopBarButton(button.id);
            showOnlineRecent();
        } else if(button.id == GuiConstants.CENTER_BEST_BUTTON) {
            disableTopBarButton(button.id);
            showOnlineBest();
        } else if(button.id == GuiConstants.CENTER_DOWNLOADED_REPLAYS_BUTTON) {
            disableTopBarButton(button.id);
            showDownloadedFiles();
        } else if(button.id == GuiConstants.CENTER_FAVORITED_REPLAYS_BUTTON) {
            disableTopBarButton(button.id);
            showFavoritedFiles();
        } else if(button.id == GuiConstants.CENTER_SEARCH_BUTTON) {
            disableTopBarButton(button.id);
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
        } else if(button.id == GuiConstants.CENTER_LIKE_REPLAY_BUTTON) {
            GuiReplayListEntry entry = (GuiReplayListEntry)currentList.getListEntry(currentList.selected);
            FileInfo info = entry.getFileInfo();
            if(info != null) {
                try {
                    if(ReplayMod.ratedFileHandler.getRating(info.getId()) == Rating.RatingType.LIKE) {
                        ReplayMod.ratedFileHandler.rateFile(info.getId(), Rating.RatingType.NEUTRAL);
                    } else {
                        ReplayMod.ratedFileHandler.rateFile(info.getId(), Rating.RatingType.LIKE);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                elementSelected(currentList.selected);
            }
        } else if(button.id == GuiConstants.CENTER_DISLIKE_REPLAY_BUTTON) {
            GuiReplayListEntry entry = (GuiReplayListEntry)currentList.getListEntry(currentList.selected);
            FileInfo info = entry.getFileInfo();
            if(info != null) {
                try {
                    if(ReplayMod.ratedFileHandler.getRating(info.getId()) == Rating.RatingType.DISLIKE) {
                        ReplayMod.ratedFileHandler.rateFile(info.getId(), Rating.RatingType.NEUTRAL);
                    } else {
                        ReplayMod.ratedFileHandler.rateFile(info.getId(), Rating.RatingType.DISLIKE);
                    }
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
                elementSelected(currentList.selected);
                initGui();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        if(currentList != null) {
            currentList.drawScreen(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if(((favButton.isMouseOver() && !favButton.enabled) || (likeButton.isMouseOver() && !likeButton.enabled)
                || (dislikeButton.isMouseOver() && !dislikeButton.enabled ))&& currentList.selected != -1) {
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, I18n.format("replaymod.gui.center.downloadrequired"), this, Color.RED);
        }

        this.drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.replaycenter"), this.width / 2, 5, Color.WHITE.getRGB());
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
        currentList = new ReplayFileList(mc, width, height, 50, height - 60, this);
        GuiLoadingListEntry loadingListEntry = new GuiLoadingListEntry();
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
        if(currentListLoader != null && currentListLoader.isAlive()) {
            currentListLoader.interrupt();
            try {
                currentListLoader.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void showOnlineRecent() {
        cancelCurrentListLoader();
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(new SearchPagination(recentFileSearchQuery));
            }
        }, "replaymod-list-loader");
        currentListLoader.start();
    }

    public void showOnlineBest() {
        cancelCurrentListLoader();
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(new SearchPagination(bestFileSearchQuery));
            }
        }, "replaymod-list-loader");
        currentListLoader.start();
    }

    public void showDownloadedFiles() {
        cancelCurrentListLoader();
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(new DownloadedFilePagination());
            }
        }, "replaymod-list-loader");
        currentListLoader.start();
    }

    public void showFavoritedFiles() {
        cancelCurrentListLoader();
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                ReplayMod.favoritedFileHandler.reloadFavorites();
                updateCurrentList(new FavoritedFilePagination());
            }
        }, "replaymod-list-loader");
        currentListLoader.start();
    }
}
