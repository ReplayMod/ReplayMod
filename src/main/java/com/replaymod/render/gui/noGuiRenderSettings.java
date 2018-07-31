package com.replaymod.render.gui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.common.io.Files; // RAh
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.VideoWriter;
import com.replaymod.render.rendering.VideoRenderer;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Timeline;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.container.GuiVerticalList;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.element.advanced.GuiColorPicker;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.function.Closeable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.GuiFileChooserPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.Utils;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.lwjgl.util.Color;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.ReadableDimension;

import javax.annotation.Nullable;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static com.replaymod.core.utils.Utils.error;
import static com.replaymod.render.ReplayModRender.LOGGER;

public class noGuiRenderSettings  {
	/** RAH
	* Attempt to remove the GUI
	*
	**/
	public void doRender ()
	{
            // Closing this GUI ensures that settings are saved
            //getMinecraft().displayGuiScreen(null);
            try {
                //VideoRenderer videoRenderer = new VideoRenderer(save(false), replayHandler, timeline);
				VideoRenderer videoRenderer = new VideoRenderer(getDefaultRenderSettings(), replayHandler, timeline);
                videoRenderer.renderVideo();
            } catch (VideoWriter.NoFFmpegException e) {
                LOGGER.error("Rendering video:", e);
                //getMinecraft().displayGuiScreen(errorScreen);
            } catch (VideoWriter.FFmpegStartupException e) {
				e.printStackTrace();
            } catch (Throwable t) {
                //error(LOGGER, noGuiRenderSettings.this, CrashReport.makeCrashReport(t, "Rendering video"), () -> {});
				LOGGER.error("Rendering video:", t);
            }
			LOGGER.debug("Done with Render");
			
    }
	public void doRender (int starttime_ms, int endtime_ms)
	{
		startTime_ms = starttime_ms;
		endTime_ms = endtime_ms;
        try {
            //VideoRenderer videoRenderer = new VideoRenderer(save(false), replayHandler, timeline);
			VideoRenderer videoRenderer = new VideoRenderer(getDefaultRenderSettings(), replayHandler, timeline);
            videoRenderer.renderVideo();
        } catch (VideoWriter.NoFFmpegException e) {
            LOGGER.error("Rendering video:", e);
            //getMinecraft().displayGuiScreen(errorScreen);
        } catch (VideoWriter.FFmpegStartupException e) {
			e.printStackTrace();
        } catch (Throwable t) {
            //error(LOGGER, noGuiRenderSettings.this, CrashReport.makeCrashReport(t, "Rendering video"), () -> {});
			LOGGER.error("Rendering video:", t);
        }
		LOGGER.debug("Done with Render, writing text file");

		// RAH - Create an output file to communicate we are done rendering
		File file = new File("finished.txt");
		if ( ! file.exists( ) )
			try {
				file.createNewFile( );
			} catch (Exception e) {
				e.printStackTrace();
			}
		//System.exit(0);
		//mod.getReplayHandler().endReplay();
		try {
			this.replayHandler.endReplay(); // RAH - after done with rendering, return to the main MC screen
		} catch (Exception e) {
				e.printStackTrace();
		}
    }



    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private File outputFile;
    private boolean outputFileManuallySet;
	private int startTime_ms = 0; // Given a replay file, this is our start point for rendering
	private int endTime_ms = 0;   // The final point of the render file

    public noGuiRenderSettings(ReplayHandler replayHandler, Timeline timeline) {
        this.replayHandler = replayHandler;
        this.timeline = timeline;

        String json = getConfigProperty(ReplayModRender.instance.getConfiguration()).getString();
        RenderSettings settings = new GsonBuilder()
                .registerTypeAdapter(RenderSettings.class, (InstanceCreator<RenderSettings>) type -> getDefaultRenderSettings())
                .registerTypeAdapter(ReadableColor.class, new Gson().getAdapter(Color.class))
                .create().fromJson(json, RenderSettings.class);
        load(settings);
    }




    public void load(RenderSettings settings) {
		return;
    }

    public RenderSettings save(boolean serialize) {
        return null;

    }

    protected File generateOutputFile(RenderSettings.EncodingPreset encodingPreset) {
        String fileName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());

        File folder = ReplayModRender.instance.getVideoFolder();
        return new File(folder, fileName + "." + encodingPreset.getFileExtension());
    }

	// RAH - when removing gui, I wiped out encodingPresets BC it is part of the GUI framework - use hardcoded values
	protected File generateOutputFile() {
		String dateStr = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
		String fileName = String.format("CMURL_%010d_%010d-",startTime_ms,endTime_ms) + dateStr;
        File folder = ReplayModRender.instance.getVideoFolder();
		return new File(folder, fileName + ".MP4");  // No longer have encodingPreset, hardcoding to MP4
    }

    private RenderSettings getDefaultRenderSettings() {
		// RAh - the null was for serialized - we are going to file only - replaced null with generateOutputFile()
		int width = 320; // RAH - made these variables to be more easily readable
		int height = 240;
		int frameRate = 5;
		// N.B. When bitRate was set to 10, a 1920x1080 at 60 fps resulted in a final bitRate in the file of 19,930 kbps - that is a crazy amount of data
		int bitRate = 8; // was 10 - Do know what this is, mbps, kbps - some other value? There is a shift happening - be careful
		return new RenderSettings(RenderSettings.RenderMethod.DEFAULT, RenderSettings.EncodingPreset.MP4_DEFAULT, width, height, frameRate, bitRate << 20, generateOutputFile(),
                true, false, false, false, null, false, RenderSettings.AntiAliasing.NONE, "", RenderSettings.EncodingPreset.MP4_DEFAULT.getValue(), false);
    }


    public void close() {
        RenderSettings settings = save(true);
        String json = new Gson().toJson(settings);
        Configuration config = ReplayModRender.instance.getConfiguration();
        getConfigProperty(config).set(json);
        config.save();
    }

    protected Property getConfigProperty(Configuration configuration) {
        return configuration.get("rendersettings", "settings", "{}",
                "Last state of the render settings GUI. Internal use only.");
    }

    public ReplayHandler getReplayHandler() {
        return replayHandler;
    }
}
