package eu.crushedpixel.replaymod.gui;

import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;

import eu.crushedpixel.replaymod.reflection.MCPNames;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.Packet;

public class GuiCustomMainMenu extends GuiMainMenu {

	private static final int REPLAY_MANAGER_ID = 9001;
	
	@Override
	public void initGui() {
		Class<? extends GuiMainMenu> clazz = GuiMainMenu.class;
		
		int i1 = this.height / 4 + 48;
		this.buttonList.add(new GuiButton(REPLAY_MANAGER_ID, this.width / 2 - 100, i1 + 2*24, I18n.format("Replay Manager", new Object[0])));
		
		try {
			Field viewportTexture = clazz.getDeclaredField(MCPNames.field("field_73977_n"));
			viewportTexture.setAccessible(true);
			viewportTexture.set(this, new DynamicTexture(256, 256));

			Field field_110351_G = clazz.getDeclaredField(MCPNames.field("field_110351_G"));
			field_110351_G.setAccessible(true);
			field_110351_G.set(this, this.mc.getTextureManager().getDynamicTextureLocation("background", (DynamicTexture)viewportTexture.get(this)));

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());

			Field splashText = clazz.getDeclaredField(MCPNames.field("field_73975_c"));
			splashText.setAccessible(true);

			if (calendar.get(2) + 1 == 11 && calendar.get(5) == 9)
			{
				splashText.set(this, "Happy birthday, ez!");
			}
			else if (calendar.get(2) + 1 == 6 && calendar.get(5) == 1)
			{
				splashText.set(this, "Happy birthday, Notch!");
			}
			else if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24)
			{
				splashText.set(this, "Merry X-Mas!");
			}
			else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1)
			{
				splashText.set(this, "Happy new year!");
			}
			else if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31)
			{
				splashText.set(this, "OOoooOOOoooo! Spooky!");
			}

			boolean flag = true;
			int i = this.height / 4 + 48;

			if (this.mc.isDemo())
			{
				Method addDemoButtons = clazz.getDeclaredMethod(MCPNames.method("func_73972_b"), int.class, int.class);
				addDemoButtons.setAccessible(true);
				addDemoButtons.invoke(this, i, 24);
			}
			else
			{
				Method addSingleplayerMultiplayerButtons = clazz.getDeclaredMethod(MCPNames.method("func_73969_a"), int.class, int.class);
				addSingleplayerMultiplayerButtons.setAccessible(true);
				addSingleplayerMultiplayerButtons.invoke(this, i - 24, 24);
			}

			this.buttonList.add(new GuiButton(0, this.width / 2 - 100, i + 72 + 12, 98, 20, I18n.format("menu.options", new Object[0])));
			this.buttonList.add(new GuiButton(4, this.width / 2 + 2, i + 72 + 12, 98, 20, I18n.format("menu.quit", new Object[0])));
			this.buttonList.add(new GuiButtonLanguage(5, this.width / 2 - 124, i + 72 + 12));

			Field threadLock = clazz.getDeclaredField(MCPNames.field("field_104025_t"));
			threadLock.setAccessible(true);
			Object object = threadLock.get(this);

			Field field_92023_s = clazz.getDeclaredField(MCPNames.field("field_92023_s"));
			field_92023_s.setAccessible(true);

			Field openGLWarning1 = clazz.getDeclaredField(MCPNames.field("openGLWarning1"));
			openGLWarning1.setAccessible(true);

			Field openGLWarning2 = clazz.getDeclaredField(MCPNames.field("openGLWarning2"));
			openGLWarning2.setAccessible(true);

			Field field_92024_r = clazz.getDeclaredField(MCPNames.field("field_92024_r"));
			field_92024_r.setAccessible(true);

			Field field_92022_t = clazz.getDeclaredField(MCPNames.field("field_92022_t"));
			field_92022_t.setAccessible(true);

			Field field_92021_u = clazz.getDeclaredField(MCPNames.field("field_92021_u"));
			field_92021_u.setAccessible(true);

			Field field_92020_v = clazz.getDeclaredField(MCPNames.field("field_92020_v"));
			field_92020_v.setAccessible(true);

			Field field_92019_w = clazz.getDeclaredField(MCPNames.field("field_92019_w"));
			field_92019_w.setAccessible(true);

			synchronized (object)
			{
				field_92023_s.set(this, this.fontRendererObj.getStringWidth((String)openGLWarning1.get(this)));
				field_92024_r.set(this, this.fontRendererObj.getStringWidth((String)openGLWarning2.get(this)));
				int j = Math.max((Integer)(field_92023_s.get(this)), (Integer)(field_92024_r.get(this)));
				field_92022_t.set(this, (this.width - j) / 2);
				field_92021_u.set(this, ((GuiButton)this.buttonList.get(0)).yPosition - 24);
				field_92020_v.set(this, (Integer)field_92022_t.get(this) + j);
				field_92019_w.set(this, (Integer)field_92021_u.get(this) + 24);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(button.id == REPLAY_MANAGER_ID) {
			this.mc.displayGuiScreen(new GuiReplayManager());
		}
		super.actionPerformed(button);
    }
    
}
