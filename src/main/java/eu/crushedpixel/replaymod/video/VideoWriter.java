package eu.crushedpixel.replaymod.video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.monte.media.Buffer;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.MovieWriter;
import org.monte.media.Registry;
import org.monte.media.VideoFormatKeys;
import org.monte.media.math.Rational;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;

public class VideoWriter {

	private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
	private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

	private static final String VIDEO_EXTENSION = ".avi";

	private static MovieWriter out;
	private static File file;
	private static boolean isRecording = false;
	private static boolean requestFinish = false;
	private static boolean abort = false;

	private static Buffer buf;
	private static int track;

	public static boolean isRecording() {
		return isRecording;
	}

	public static void startRecording(int width, int height) {
		if(isRecording) {
			IllegalStateException up = new IllegalStateException("VideoWriter is already recording!");
			throw up; //lolololo
		}
		isRecording = true;

		toWrite = new LinkedBlockingQueue<BufferedImage>();

		try {
			File folder = ReplayFileIO.getRenderFolder();

			String fileName = sdf.format(Calendar.getInstance().getTime());

			file = new File(folder, fileName+VIDEO_EXTENSION);
			file.createNewFile();

			out = Registry.getInstance().getWriter(file);
			Format format = new Format(FormatKeys.MediaTypeKey, MediaType.VIDEO,
					FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_MJPG,
					FormatKeys.FrameRateKey, new Rational(ReplayMod.replaySettings.getVideoFramerate(), 1),
					VideoFormatKeys.WidthKey, width,
					VideoFormatKeys.HeightKey, height,
					VideoFormatKeys.DepthKey, 24,
					VideoFormatKeys.QualityKey, (float)ReplayMod.replaySettings.getVideoQuality());


			track = out.addTrack(format);

			buf = new Buffer();
			buf.format = new Format(VideoFormatKeys.DataClassKey, BufferedImage.class); 
			buf.sampleDuration = out.getFormat(track).get(VideoFormatKeys.FrameRateKey).inverse();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				while(true) {
					if(toWrite.isEmpty() || abort) {
						if(requestFinish) {
							requestFinish = false;
							isRecording = false;
							try {
								out.close();
								if(abort) {
									file.delete();
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
							abort = false;
							toWrite = new LinkedBlockingQueue<BufferedImage>();
							return;
						}
						try {
							Thread.sleep(10);
						} catch(Exception e) {
							e.printStackTrace();
						}
					} else {
						write();
					}
				}
			}
		});
		t.start();
	}

	private static Queue<BufferedImage> toWrite = new LinkedBlockingQueue<BufferedImage>();

	public static void writeImage(BufferedImage image) {
		if(requestFinish || !isRecording) {
			IllegalStateException up = new IllegalStateException(
					"The VideoWriter is currently not available. Please try again later.");
			throw up; //lolololo^2
		}

		toWrite.add(image);
	}

	public static void endRecording() {
		if(!isRecording) return;
		requestFinish = true;
	}

	private static void write() {
		try {
			BufferedImage img = toWrite.poll();
			if(img != null) {
				buf.data = img;
				out.write(track, buf);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void abortRecording() {
		requestFinish = true;
		abort = true;
	}
}
