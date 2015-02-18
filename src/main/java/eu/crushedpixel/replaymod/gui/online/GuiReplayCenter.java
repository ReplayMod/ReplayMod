package eu.crushedpixel.replaymod.gui.online;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.client.ApiException;
import eu.crushedpixel.replaymod.api.client.SearchPagination;
import eu.crushedpixel.replaymod.api.client.SearchQuery;
import eu.crushedpixel.replaymod.api.client.holders.FileInfo;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.GuiReplayListExtended;
import eu.crushedpixel.replaymod.gui.replaymanager.GuiReplayManager;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;

public class GuiReplayCenter extends GuiScreen implements GuiYesNoCallback {

	private enum Tab {
		RECENT_FILES, BEST_FILES, MY_FILES, SEARCH;
	}

	private GuiReplayListExtended currentList;

	private ReplayFileList recentFileList, bestFileList, myFileList, searchFileList;

	private Tab currentTab = Tab.RECENT_FILES;

	private static final SearchQuery recentFileSearchQuery = new SearchQuery(false, null, null, null, null, null,
			null, null, null, null);

	private static final SearchQuery bestFileSearchQuery = new SearchQuery(true, null, null, null, null, null,
			null, null, null, null);

	private final SearchPagination recentFilePagination = new SearchPagination(recentFileSearchQuery);
	private final SearchPagination bestFilePagination = new SearchPagination(bestFileSearchQuery);
	private SearchPagination myFilePagination;

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

		GuiButton recentButton = new GuiButton(GuiConstants.CENTER_RECENT_BUTTON, 20, 30, "Newest Replays");
		buttonBar.add(recentButton);

		GuiButton bestButton = new GuiButton(GuiConstants.CENTER_BEST_BUTTON, 20, 30, "Best Replays");
		buttonBar.add(bestButton);

		GuiButton ownReplayButton = new GuiButton(GuiConstants.CENTER_MY_REPLAYS_BUTTON, 20, 30, "My Replays");
		ownReplayButton.enabled = AuthenticationHandler.isAuthenticated();
		buttonBar.add(ownReplayButton);

		GuiButton searchButton = new GuiButton(GuiConstants.CENTER_SEARCH_BUTTON, 20, 30, "Search");
		buttonBar.add(searchButton);

		int i = 0;
		for(GuiButton b : buttonBar) {
			int w = this.width - 30;
			int w2 = w/buttonBar.size();

			int x = 15+(w2*i);
			b.xPosition = x+2;
			b.yPosition = 20;
			b.width = w2-4;

			buttonList.add(b);

			i++;
		}

		//Bottom Button Bar (dat alliteration)
		List<GuiButton> bottomBar = new ArrayList<GuiButton>();

		GuiButton exitButton = new GuiButton(GuiConstants.CENTER_BACK_BUTTON, 20, 20, "Main Menu");
		bottomBar.add(exitButton);

		GuiButton managerButton = new GuiButton(GuiConstants.CENTER_MANAGER_BUTTON, 20, 20, "Replay Manager");
		bottomBar.add(managerButton);

		GuiButton logoutButton = new GuiButton(GuiConstants.CENTER_LOGOUT_BUTTON, 20, 20, "Logout");
		bottomBar.add(logoutButton);

		i = 0;
		for(GuiButton b : bottomBar) {
			int w = this.width - 30;
			int w2 = w/bottomBar.size();

			int x = 15+(w2*i);
			b.xPosition = x+2;
			b.yPosition = height-30;
			b.width = w2-4;

			buttonList.add(b);

			i++;
		}

		showOnlineRecent();
	}

	@Override
	protected void actionPerformed(GuiButton button) throws java.io.IOException {
		if(!button.enabled) return;
		if(button.id == GuiConstants.CENTER_BACK_BUTTON) {
			mc.displayGuiScreen(new GuiMainMenu());
		} else if(button.id == GuiConstants.CENTER_LOGOUT_BUTTON) {
			mc.displayGuiScreen(getYesNoGui(this, LOGOUT_CALLBACK_ID));
		} else if(button.id == GuiConstants.CENTER_MANAGER_BUTTON) {
			mc.displayGuiScreen(new GuiReplayManager());
		} else if(button.id == GuiConstants.CENTER_RECENT_BUTTON) {
			showOnlineRecent();
		} else if(button.id == GuiConstants.CENTER_BEST_BUTTON) {
			showOnlineBest();
		} else if(button.id == GuiConstants.CENTER_MY_REPLAYS_BUTTON) {
			showOnlineOwnFiles();
		} else if(button.id == GuiConstants.CENTER_SEARCH_BUTTON) {

		}
	}

	private static final int LOGOUT_CALLBACK_ID = 1;
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

	public static GuiYesNo getYesNoGui(GuiYesNoCallback p_152129_0_, int p_152129_2_) {
		String s1 = I18n.format("Do you really want to log out?", new Object[0]);
		GuiYesNo guiyesno = new GuiYesNo(p_152129_0_, s1, "", "Logout", "Cancel", p_152129_2_);
		return guiyesno;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(fontRendererObj, "Replay Center", this.width/2, 8, Color.WHITE.getRGB());

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
			currentList = new ReplayFileList(mc, width, height, 50, height-40, 36);
		} else {
			currentList.clearEntries();
			currentList.width = width;
			currentList.height = height;
			currentList.top = 50;
			currentList.bottom = height-40;
		}

		if(pagination.getLoadedPages() < 0) {
			pagination.fetchPage();
		}

		for(FileInfo i : pagination.getFiles()) {
			try {
				File tmp = null;
				if(i.hasThumbnail()) {
					tmp = File.createTempFile("thumb_online_"+i.getId(), "jpg");
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
}
