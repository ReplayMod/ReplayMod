package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.replay.SearchQuery;
import eu.crushedpixel.replaymod.api.replay.holders.Category;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;
import eu.crushedpixel.replaymod.api.replay.holders.MinecraftVersion;
import eu.crushedpixel.replaymod.api.replay.holders.Rating;
import eu.crushedpixel.replaymod.api.replay.pagination.DownloadedFilePagination;
import eu.crushedpixel.replaymod.api.replay.pagination.FavoritedFilePagination;
import eu.crushedpixel.replaymod.api.replay.pagination.Pagination;
import eu.crushedpixel.replaymod.api.replay.pagination.SearchPagination;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.replayviewer.GuiReplayViewer;
import eu.crushedpixel.replaymod.holders.GuiEntryListStringEntry;
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
    private ReplayFileList currentList;
    private GuiButton loadButton, favButton, likeButton, dislikeButton;
    private List<GuiButton> replayButtonBar, bottomBar, topBar;

    private GuiToggleButton searchGametypeToggle, searchSortToggle;
    private GuiDropdown<GuiEntryListStringEntry> searchCategoryDropdown, searchVersionDropdown;
    private GuiAdvancedTextField searchNameInput, searchServerInput;
    private GuiButton searchActionButton;

    private boolean showSearchFields = false;

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
                mc.displayGuiScreen(new GuiLoginPrompt(new GuiMainMenu(), this, true).toMinecraft());
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

            GuiButton logoutButton = new GuiButton(GuiConstants.CENTER_LOGOUT_BUTTON, 20, 20, I18n.format("replaymod.gui.logout"));
            bottomBar.add(logoutButton);

            GuiButton managerButton = new GuiButton(GuiConstants.CENTER_MANAGER_BUTTON, 20, 20, I18n.format("replaymod.gui.replayviewer"));
            bottomBar.add(managerButton);

            GuiButton exitButton = new GuiButton(GuiConstants.CENTER_BACK_BUTTON, 20, 20, I18n.format("replaymod.gui.mainmenu"));
            bottomBar.add(exitButton);

            showOnlineRecent();
            disableTopBarButton(GuiConstants.CENTER_RECENT_BUTTON);

            //Search GUI
            searchActionButton = new GuiButton(GuiConstants.CENTER_SEARCH_ACTION_BUTTON, 20, 20, I18n.format("replaymod.gui.center.top.search"));
            searchGametypeToggle = new GuiToggleButton(GuiConstants.CENTER_SEARCH_GAMETYPE_TOGGLE, 0, 0, I18n.format("replaymod.gui.center.search.gametype")+": ",
                    new String[]{I18n.format("options.particles.all"), I18n.format("menu.singleplayer"), I18n.format("menu.multiplayer")});
            searchSortToggle = new GuiToggleButton(GuiConstants.CENTER_SEARCH_ORDER_TOGGLE, 0, 0, I18n.format("replaymod.gui.center.search.order")+": ",
                    new String[]{I18n.format("replaymod.gui.center.search.order.best"), I18n.format("replaymod.gui.center.search.order.recent")});
            searchCategoryDropdown = new GuiDropdown<GuiEntryListStringEntry>(fontRendererObj, 0, 0, 0, Category.values().length+1);
            searchVersionDropdown = new GuiDropdown<GuiEntryListStringEntry>(fontRendererObj, 0, 0, 0, 5);
            searchNameInput = new GuiAdvancedTextField(fontRendererObj, 0, 0, 50, 20);
            searchServerInput = new GuiAdvancedTextField(fontRendererObj, 0, 0, 50, 20);

            searchNameInput.hint = I18n.format("replaymod.gui.center.search.name");
            searchServerInput.hint = I18n.format("replaymod.gui.center.search.server");

            searchCategoryDropdown.addElement(new GuiEntryListStringEntry(I18n.format("replaymod.gui.center.search.category")));
            for(Category c : Category.values()) {
                searchCategoryDropdown.addElement(new GuiEntryListStringEntry(c.toNiceString()));
            }

            searchVersionDropdown.addElement(new GuiEntryListStringEntry(I18n.format("replaymod.gui.center.search.version")));
            for(MinecraftVersion v : MinecraftVersion.values()) {
                searchVersionDropdown.addElement(new GuiEntryListStringEntry(v.toNiceName()));
            }
        }

        int wd = this.width - 40;
        int sw = wd/3 + 4;

        searchNameInput.xPosition = searchServerInput.xPosition = searchActionButton.xPosition = 20;
        searchCategoryDropdown.xPosition = searchVersionDropdown.xPosition = 20 + sw;
        searchGametypeToggle.xPosition = searchSortToggle.xPosition = 20 + 2*sw;

        searchNameInput.width = searchCategoryDropdown.width = searchGametypeToggle.width = searchActionButton.width =
                searchServerInput.width = searchVersionDropdown.width = searchSortToggle.width = sw-7;

        searchNameInput.yPosition = searchCategoryDropdown.yPosition = searchGametypeToggle.yPosition = 70;
        searchServerInput.yPosition = searchVersionDropdown.yPosition = searchSortToggle.yPosition = 100;
        searchActionButton.yPosition = 130;

        if(showSearchFields) {
            showSearchFields();
        }

        if(currentList != null) {
            currentList.setDimensions(this.width, this.height, 50, this.height - 60);
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
            showReplaySearch();
        } else if(button.id == GuiConstants.CENTER_LOAD_REPLAY_BUTTON) {
            GuiReplayListEntry entry = (GuiReplayListEntry)currentList.getListEntry(currentList.selected);
            FileInfo info = entry.getFileInfo();
            if(info != null) {
                File f = ReplayMod.downloadedFileHandler.getFileForID(info.getId());
                if(f == null) {
                    new GuiReplayDownloading(info).display();
                } else {
                    mc.displayGuiScreen(new GuiReplayInstanceChooser(info, f));
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

        //the search action button
        else if(button.id == GuiConstants.CENTER_SEARCH_ACTION_BUTTON) {
            SearchQuery searchQuery = new SearchQuery();

            Boolean gameType = null;
            if(searchGametypeToggle.getValue() > 0) {
                gameType = searchGametypeToggle.getValue() == 1;
            }
            searchQuery.singleplayer = gameType;

            Integer category = null;
            if(searchCategoryDropdown.getSelectionIndex() > 0) {
                category = searchCategoryDropdown.getSelectionIndex()-1;
            }
            searchQuery.category = category;

            String mcversion = null;
            if(searchVersionDropdown.getSelectionIndex() > 0) {
                mcversion = MinecraftVersion.values()[searchVersionDropdown.getSelectionIndex()-1].getApiName();
            }
            searchQuery.version = mcversion;

            if(searchNameInput.getText().trim().length() > 0) {
                searchQuery.name = searchNameInput.getText().trim();
            }

            if(searchServerInput.getText().trim().length() > 0) {
                searchQuery.server = searchServerInput.getText().trim();
            }

            Boolean order = searchSortToggle.getValue() == 0;
            searchQuery.order = order;

            SearchPagination searchPagination = new SearchPagination(searchQuery);
            showReplaySearch(searchPagination);
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
                || (dislikeButton.isMouseOver() && !dislikeButton.enabled )) && (currentList != null && currentList.selected != -1)) {
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, I18n.format("replaymod.gui.center.downloadrequired"), this, Color.RED);
        }

        this.drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.replaycenter"), this.width / 2, 5, Color.WHITE.getRGB());

        //search fields
        if(showSearchFields) {
            this.drawString(fontRendererObj, I18n.format("replaymod.gui.center.search.filters"), searchActionButton.xPosition, 50, Color.WHITE.getRGB());
            searchActionButton.drawButton(mc, mouseX, mouseY);
            searchGametypeToggle.drawButton(mc, mouseX, mouseY);
            searchSortToggle.drawButton(mc, mouseX, mouseY);
            searchVersionDropdown.drawTextBox();
            searchCategoryDropdown.drawTextBox();
            searchNameInput.drawTextBox();
            searchServerInput.drawTextBox();
        }
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

        if(showSearchFields) {
            if(!searchCategoryDropdown.mouseClickedResult(mouseX, mouseY))
                searchVersionDropdown.mouseClicked(mouseX, mouseY, mouseButton);
            searchNameInput.mouseClicked(mouseX, mouseY, mouseButton);
            searchServerInput.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        if(showSearchFields) {
            searchNameInput.updateCursorCounter();
            searchServerInput.updateCursorCounter();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(showSearchFields) {
            searchNameInput.textboxKeyTyped(typedChar, keyCode);
            searchServerInput.textboxKeyTyped(typedChar, keyCode);
        }

        super.keyTyped(typedChar, keyCode);
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
        hideSearchFields();

        elementSelected(-1);
        currentList = new ReplayFileList(mc, width, height, 50, height - 60, this);
        GuiLoadingListEntry loadingListEntry = new GuiLoadingListEntry();
        currentList.addEntry(loadingListEntry);

        if(pagination.getLoadedPages() < 0) {
            pagination.fetchPage();
        }

        for(FileInfo i : pagination.getFiles()) {
            if(Thread.interrupted()) break;
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

    public void showReplaySearch() {
        cancelCurrentListLoader();
        currentList = null;
        showSearchFields();
    }

    public void showReplaySearch(final SearchPagination searchPagination) {
        disableTopBarButton(-1);
        currentListLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                updateCurrentList(searchPagination);
            }
        }, "replaymod-list-loader");
        currentListLoader.start();
    }

    @SuppressWarnings("unchecked")
    private void showSearchFields() {
        showSearchFields = true;
        buttonList.add(searchActionButton);
        buttonList.add(searchGametypeToggle);
        buttonList.add(searchSortToggle);
    }

    private void hideSearchFields() {
        showSearchFields = false;
        buttonList.remove(searchActionButton);
        buttonList.remove(searchGametypeToggle);
        buttonList.remove(searchSortToggle);
    }
}
