package eu.crushedpixel.replaymod.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Util;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;

import com.google.gson.Gson;
import com.mojang.realmsclient.util.Pair;

import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class GuiReplayManager extends GuiScreen implements GuiYesNoCallback {

	private GuiScreen parentScreen;
	private GuiButton btnEditServer;
	private GuiButton btnSelectServer;
	private GuiButton btnDeleteServer;
	private String hoveringText;
	private boolean initialized;
	private GuiReplayListExtended replayGuiList;
	private List<Pair<File, ReplayMetaData>> replayFileList = new ArrayList<Pair<File, ReplayMetaData>>();
	private GuiButton loadButton, folderButton, renameButton, deleteButton, cancelButton, settingsButton;

	private static Gson gson = new Gson();
	private boolean replaying = false;
	
	private static final int LOAD_BUTTON_ID = 9001;
	private static final int FOLDER_BUTTON_ID = 9002;
	private static final int RENAME_BUTTON_ID = 9003;
	private static final int DELETE_BUTTON_ID = 9004;
	private static final int SETTINGS_BUTTON_ID = 9005;
	private static final int CANCEL_BUTTON_ID = 9006;

	private boolean delete_file = false;

	private void reloadFiles() {
		replayGuiList.clearEntries();
		replayFileList = new ArrayList<Pair<File, ReplayMetaData>>();

		File folder = new File("./replay_recordings/");
		folder.mkdirs();
		
		for(File file : folder.listFiles()) {
			if(("."+FilenameUtils.getExtension(file.getAbsolutePath())).equals(ConnectionEventHandler.ZIP_FILE_EXTENSION)) {
				try {
					ZipFile archive = new ZipFile(file);
					ZipArchiveEntry recfile = archive.getEntry("recording"+ConnectionEventHandler.TEMP_FILE_EXTENSION);
					ZipArchiveEntry metadata = archive.getEntry("metaData"+ConnectionEventHandler.JSON_FILE_EXTENSION);

					InputStream is = archive.getInputStream(metadata);
					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					
					String json = br.readLine();
					
					ReplayMetaData metaData = gson.fromJson(json, ReplayMetaData.class);
					
					replayFileList.add(Pair.of(file, metaData));

					archive.close();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		Collections.sort(replayFileList, new FileAgeComparator());
		
		for(Pair<File, ReplayMetaData> p : replayFileList) {
			replayGuiList.addEntry(FilenameUtils.getBaseName(p.first().getName()), p.second());
		}
	}
	
	public class FileAgeComparator implements Comparator<Pair<File, ReplayMetaData>> {

		@Override
		public int compare(Pair<File, ReplayMetaData> o1, Pair<File, ReplayMetaData> o2) {
			return (int)(new Date(o2.second().getDate()).compareTo(new Date(o1.second().getDate())));
		}
		
	}

	public void initGui()
	{
		replayGuiList = new GuiReplayListExtended(this, this.mc, this.width, this.height, 32, this.height - 64, 36);
		Keyboard.enableRepeatEvents(true);
		this.buttonList.clear();

		if (!this.initialized) {
			this.initialized = true;
		}
		else {
			this.replayGuiList.setDimensions(this.width, this.height, 32, this.height - 64);
		}

		reloadFiles();
		this.createButtons();
	}

	private void createButtons() {
		this.buttonList.add(loadButton = new GuiButton(LOAD_BUTTON_ID, this.width / 2 - 154, this.height - 52, 150, 20, I18n.format("Load Replay", new Object[0])));
		this.buttonList.add(folderButton =  new GuiButton(FOLDER_BUTTON_ID, this.width / 2 + 4, this.height - 52, 150, 20, I18n.format("Open Replay Folder...", new Object[0])));
		this.buttonList.add(renameButton = new GuiButton(RENAME_BUTTON_ID, this.width / 2 - 154, this.height - 28, 72, 20, I18n.format("Rename", new Object[0])));
		this.buttonList.add(deleteButton = new GuiButton(DELETE_BUTTON_ID, this.width / 2 - 76, this.height - 28, 72, 20, I18n.format("Delete", new Object[0])));
		this.buttonList.add(settingsButton = new GuiButton(SETTINGS_BUTTON_ID, this.width / 2 + 4, this.height - 28, 72, 20, I18n.format("Settings", new Object[0])));
		this.buttonList.add(cancelButton = new GuiButton(CANCEL_BUTTON_ID, this.width / 2 + 4 + 78, this.height - 28, 72, 20, I18n.format("Cancel", new Object[0])));
		setButtonsEnabled(false);
	}

	public void handleMouseInput() throws IOException
	{
		super.handleMouseInput();
		this.replayGuiList.handleMouseInput();
	}

	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
	{
		super.mouseClicked(mouseX, mouseY, mouseButton);
		this.replayGuiList.mouseClicked(mouseX, mouseY, mouseButton);
	}

	protected void mouseReleased(int mouseX, int mouseY, int state)
	{
		super.mouseReleased(mouseX, mouseY, state);
		this.replayGuiList.mouseReleased(mouseX, mouseY, state);
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		this.hoveringText = null;
		this.drawDefaultBackground();
		this.replayGuiList.drawScreen(mouseX, mouseY, partialTicks);
		this.drawCenteredString(this.fontRendererObj, I18n.format("Replay Manager", new Object[0]), this.width / 2, 20, 16777215);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException
	{
		if (button.enabled) {
			if(button.id == LOAD_BUTTON_ID) {
				loadReplay(replayGuiList.selected);
			}
			else if (button.id == CANCEL_BUTTON_ID)
			{
				mc.displayGuiScreen(parentScreen);
			}
			else if (button.id == DELETE_BUTTON_ID)
			{
				String s = replayGuiList.getListEntry(replayGuiList.selected).getFileName();

				if (s != null)
				{
					delete_file = true;
					GuiYesNo guiyesno = getYesNoGui(this, s, 1);
					this.mc.displayGuiScreen(guiyesno);
				}
			}
			else if(button.id == SETTINGS_BUTTON_ID) {
				this.mc.displayGuiScreen(new GuiReplaySettings(this));
			}
			else if(button.id == RENAME_BUTTON_ID) 
			{
				File file = replayFileList.get(replayGuiList.selected).first();
				this.mc.displayGuiScreen(new GuiRenameReplay(this, file));
			}
			else if(button.id == FOLDER_BUTTON_ID)
			{
				File file1 = new File("./replay_recordings/");
				file1.mkdirs();
				String s = file1.getAbsolutePath();

				if (Util.getOSType() == Util.EnumOS.OSX)
				{
					try
					{
						Runtime.getRuntime().exec(new String[] {"/usr/bin/open", s});
						return;
					}
					catch (IOException ioexception1) {}
				}
				else if (Util.getOSType() == Util.EnumOS.WINDOWS)
				{
					String s1 = String.format("cmd.exe /C start \"Open file\" \"%s\"", new Object[] {s});

					try
					{
						Runtime.getRuntime().exec(s1);
						return;
					}
					catch (IOException ioexception) {}
				}

				boolean flag = false;

				try
				{
					Class oclass = Class.forName("java.awt.Desktop");
					Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object)null, new Object[0]);
					oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, new Object[] {file1.toURI()});
				}
				catch (Throwable throwable)
				{
					flag = true;
				}

				if (flag)
				{
					Sys.openURL("file://" + s);
				}
			}
		}
	}

	public void confirmClicked(boolean result, int id) {
		if (this.delete_file)
		{
			this.delete_file = false;

			if (result)
			{
				replayFileList.get(replayGuiList.selected).first().delete();
				replayFileList.remove(replayGuiList.selected);
			}

			this.mc.displayGuiScreen(this);
		}
	}

	public static GuiYesNo getYesNoGui(GuiYesNoCallback p_152129_0_, String file, int p_152129_2_)
	{
		String s1 = I18n.format("Are you sure you want to delete this replay?", new Object[0]);
		String s2 = "\'" + file + "\' " + I18n.format("will be lost forever! (A long time!)", new Object[0]);
		String s3 = I18n.format("Delete", new Object[0]);
		String s4 = I18n.format("Cancel", new Object[0]);
		GuiYesNo guiyesno = new GuiYesNo(p_152129_0_, s1, s2, s3, s4, p_152129_2_);
		return guiyesno;
	}

	public void setButtonsEnabled(boolean b) {
		loadButton.enabled = b;
		renameButton.enabled = b;
		deleteButton.enabled = b;
	}

	public void loadReplay(int id)
    {
        mc.displayGuiScreen((GuiScreen)null);

        try {
			ReplayHandler.startReplay(replayFileList.get(id).first());
		} catch(Exception e) {
			e.printStackTrace();
		}

    }

}
