package eu.crushedpixel.replaymod.replay;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S36PacketSignEditorOpen;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.client.FMLClientHandler;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.events.RecordingHandler;
import eu.crushedpixel.replaymod.holders.PacketData;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.registry.LightingHandler;
import eu.crushedpixel.replaymod.timer.MCTimerHandler;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;

@Sharable
public class ReplaySender extends ChannelInboundHandlerAdapter {

	private long currentTimeStamp;
	private boolean hurryToTimestamp;
	private long desiredTimeStamp;
	private long lastTimeStamp, lastPacketSent;

	private boolean hasRestarted = false;
	
	private File replayFile;
	private boolean active = true;
	private ZipFile archive;
	private DataInputStream dis;
	private ChannelHandlerContext ctx = null;

	private boolean startFromBeginning = true;

	private NetworkManager networkManager;
	private boolean terminate = false;

	private double replaySpeed = 1f;

	private Field joinPacketEntityId, joinPacketWorldType,
	joinPacketDimension, joinPacketDifficulty, joinPacketMaxPlayers;

	private Field effectPacketEntityId;
	private Field metadataPacketEntityId, metadataPacketList;

	private Field animationPacketEntityId;
	private Field entityDataWatcher;

	private Field chatPacketPosition;

	private Minecraft mc = Minecraft.getMinecraft();

	private long now = System.currentTimeMillis();

	private int replayLength = 0;

	private int actualID = -1;

	private EffectRenderer old = mc.effectRenderer;

	private ZipArchiveEntry replayEntry;

	public boolean isHurrying() {
		return hurryToTimestamp;
	}

	public long currentTimeStamp() {
		return currentTimeStamp;
	}

	public int replayLength() {
		return replayLength;
	}
	
	public void stopHurrying() {
		hurryToTimestamp = false;
	}

	public void terminateReplay() {
		terminate = true;
		try {
			channelInactive(ctx);
			ctx.channel().pipeline().close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public long getDesiredTimestamp() {
		return desiredTimeStamp;
	}

	public void jumpToTime(int millis) {
		if(!(ReplayHandler.isInPath() && ReplayProcess.isVideoRecording())) setReplaySpeed(replaySpeed);

		if((millis < currentTimeStamp && !isHurrying())) {
			startFromBeginning = true;
		}

		desiredTimeStamp = millis;
		hurryToTimestamp = true;

	}

	public void setReplaySpeed(final double d) {
		if(d != 0) this.replaySpeed = d;
		MCTimerHandler.setTimerSpeed((float)d);
	}

	public ReplaySender(final File replayFile, NetworkManager nm) {
		try {
			joinPacketEntityId = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149206_a"));
			joinPacketEntityId.setAccessible(true);

			joinPacketDifficulty = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149203_e"));
			joinPacketDifficulty.setAccessible(true);

			joinPacketDimension = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149202_d"));
			joinPacketDimension.setAccessible(true);

			joinPacketMaxPlayers = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149200_f"));
			joinPacketMaxPlayers.setAccessible(true);

			joinPacketWorldType = S01PacketJoinGame.class.getDeclaredField(MCPNames.field("field_149201_g"));
			joinPacketWorldType.setAccessible(true);

			effectPacketEntityId = S1DPacketEntityEffect.class.getDeclaredField(MCPNames.field("field_149434_a"));
			effectPacketEntityId.setAccessible(true);

			metadataPacketEntityId = S1CPacketEntityMetadata.class.getDeclaredField(MCPNames.field("field_149379_a"));
			metadataPacketEntityId.setAccessible(true);

			metadataPacketList = S1CPacketEntityMetadata.class.getDeclaredField(MCPNames.field("field_149378_b"));
			metadataPacketList.setAccessible(true);

			animationPacketEntityId = S0BPacketAnimation.class.getDeclaredField(MCPNames.field("field_148981_a"));
			animationPacketEntityId.setAccessible(true);

			entityDataWatcher = Entity.class.getDeclaredField(MCPNames.field("field_70180_af"));
			entityDataWatcher.setAccessible(true);

			chatPacketPosition = S02PacketChat.class.getDeclaredField(MCPNames.field("field_179842_b"));
			chatPacketPosition.setAccessible(true);

		} catch (Exception e) {
			e.printStackTrace();
		}

		this.replayFile = replayFile;
		this.networkManager = nm;
		if(("."+FilenameUtils.getExtension(replayFile.getAbsolutePath())).equals(ConnectionEventHandler.ZIP_FILE_EXTENSION)) {
			try {
				archive = new ZipFile(replayFile);
				replayEntry = archive.getEntry("recording"+ConnectionEventHandler.TEMP_FILE_EXTENSION);

				ZipArchiveEntry metadata = archive.getEntry("metaData"+ConnectionEventHandler.JSON_FILE_EXTENSION);
				InputStream is = archive.getInputStream(metadata);
				BufferedReader br = new BufferedReader(new InputStreamReader(is));

				String json = br.readLine();

				ReplayMetaData metaData = new Gson().fromJson(json, ReplayMetaData.class);

				this.replayLength = metaData.getDuration();

				sender.start();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Thread sender = new Thread(new Runnable() {

		@Override
		public void run() {
			try {
				dis = new DataInputStream(archive.getInputStream(replayEntry));
			} catch(Exception e) {
				e.printStackTrace();
			}

			try {
				while(ctx == null && !terminate) {
					Thread.sleep(10);
				}
				while(!terminate) {
					if(startFromBeginning) {
						hasRestarted = true;
						currentTimeStamp = 0;
						dis.close();
						dis = new DataInputStream(archive.getInputStream(replayEntry));
						startFromBeginning = false;
						lastPacketSent = System.currentTimeMillis();
						ReplayHandler.restartReplay();
					}

					while(!terminate && !startFromBeginning && (!paused() || FMLClientHandler.instance().isGUIOpen(GuiDownloadTerrain.class))) {
						try {
							/*
							 * LOGIC: 
							 * While behind desired timestamp, only send packets
							 * until desired timestamp is reached,
							 * then increase desired timestamp by 1/20th of a second
							 * 
							 * Desired timestamp is divided through stretch factor.
							 * 
							 * If hurrying, don't wait for correct timing.
							 */


							if(!hurryToTimestamp && ReplayHandler.isInPath()) {
								continue;
							}

							PacketData pd = ReplayFileIO.readPacketData(dis);

							currentTimeStamp = pd.getTimestamp();
							//System.out.println(currentTimeStamp);

							if(!ReplayHandler.isInPath() && !hurryToTimestamp && (mc.theWorld != null && mc.theWorld.getChunkProvider().getLoadedChunkCount() > 0)) {
								//if(!hurryToTimestamp && !FMLClientHandler.instance().isGUIOpen(GuiDownloadTerrain.class)) {
								int timeWait = (int)Math.round((currentTimeStamp - lastTimeStamp)/replaySpeed);
								long timeDiff = System.currentTimeMillis() - lastPacketSent;
								lastPacketSent = System.currentTimeMillis();
								long timeToSleep = Math.max(0, timeWait-timeDiff);
								Thread.sleep(timeToSleep);
							}

							ReplaySender.this.channelRead(ctx, pd.getByteArray());

							lastTimeStamp = currentTimeStamp;

							if(hurryToTimestamp && currentTimeStamp >= desiredTimeStamp && !startFromBeginning) {
								hurryToTimestamp = false;
								if(!ReplayHandler.isInPath() || hasRestarted) {
									MCTimerHandler.advanceRenderPartialTicks(5);
									MCTimerHandler.advancePartialTicks(5);
									MCTimerHandler.advanceTicks(5);
								}
								if(!ReplayHandler.isInPath()) {
									Position pos = ReplayHandler.getLastPosition();
									CameraEntity cam = ReplayHandler.getCameraEntity();
									if(cam != null) {
										if(Math.abs(pos.getX() - cam.posX) < ReplayMod.TP_DISTANCE_LIMIT && Math.abs(pos.getZ() - cam.posZ) < ReplayMod.TP_DISTANCE_LIMIT)
											if(pos != null) {
												cam.moveAbsolute(pos.getX(), pos.getY(), pos.getZ());
												cam.rotationPitch = pos.getPitch();
												cam.rotationYaw = pos.getYaw();
											}
									}
								}
								if(!ReplayHandler.isInPath()) {
									setReplaySpeed(0);
								}
								hasRestarted = false;
							}

						} catch(EOFException eof) {
							System.out.println("End of File encountered!");
							dis = new DataInputStream(archive.getInputStream(replayEntry));
							setReplaySpeed(0);
						} catch(IOException e) {
							e.printStackTrace();
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	});

	private ArrayList<Class> badPackets = new ArrayList<Class>() {
		{
			add(S28PacketEffect.class);
			add(S2BPacketChangeGameState.class);
			add(S06PacketUpdateHealth.class);
			add(S2DPacketOpenWindow.class);
			add(S2EPacketCloseWindow.class);
			add(S2FPacketSetSlot.class);
			add(S30PacketWindowItems.class);
			add(S36PacketSignEditorOpen.class);
			add(S37PacketStatistics.class);
			add(S1FPacketSetExperience.class);
			add(S43PacketCamera.class);
			add(S39PacketPlayerAbilities.class);
		}
	};

	private boolean allowMovement = false;

	private static Field playerUUIDField;
	private static Field gameProfileField;

	//private static Field dataWatcherField;

	private static class ResourcePackCheck extends Thread {

		public ResourcePackCheck(String url, String hash) {
			this.url = url;
			this.hash = hash;
		}

		private String url, hash;

		private static Field serverResourcePackDirectory;
		private static Minecraft mc = Minecraft.getMinecraft();
		private static ResourcePackRepository repo = mc.getResourcePackRepository();

		static {
			try {
				serverResourcePackDirectory = ResourcePackRepository.class.getDeclaredField(MCPNames.field("field_148534_e"));
				serverResourcePackDirectory.setAccessible(true);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		private File getServerResourcePackLocation(String url, String hash) throws IOException, IllegalArgumentException, IllegalAccessException {

			String filename;

			if (hash.matches("^[a-f0-9]{40}$")) {
				filename = hash;
			} else {
				filename = url.substring(url.lastIndexOf("/") + 1);

				if (filename.contains("?"))
				{
					filename = filename.substring(0, filename.indexOf("?"));
				}

				if (!filename.endsWith(".zip"))
				{
					return null;
				}

				filename = "legacy_" + filename.replaceAll("\\W", "");
			}

			File folder = (File)serverResourcePackDirectory.get(repo);
			File rp = new File(folder, filename);

			return rp;
		}

		private boolean downloadServerResourcePack(String url, File file) {
			try {
				FileUtils.copyURLToFile(new URL(url), file);
				return true;
			} catch(Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		public void run() {
			try {
				boolean use = ReplayMod.instance.replaySettings.getUseResourcePacks();
				if(!use) return;

				System.out.println("Looking for downloaded Resource Pack...");
				File rp = getServerResourcePackLocation(url, hash);
				if(rp == null) {
					System.out.println("Invalid Resource Pack provided");
					return;
				}
				if(rp.exists()) {
					System.out.println("Resource Pack found!");
					repo.func_177319_a(rp);

				} else {
					System.out.println("No Resource Pack found.");
					System.out.println("Attempting to download Resource Pack...");
					boolean success = downloadServerResourcePack(url, rp);
					System.out.println(success ? "Resource pack was successfully downloaded!" : "Resource Pack download failed.");
					if(success) {
						repo.func_177319_a(rp);
					}
				}

			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	static {
		try {
			playerUUIDField = S0CPacketSpawnPlayer.class.getDeclaredField(MCPNames.field("field_179820_b"));
			playerUUIDField.setAccessible(true);

			gameProfileField = S38PacketPlayerListItem.AddPlayerData.class.getDeclaredField("field_179964_d");
			gameProfileField.setAccessible(true);

			//dataWatcherField = S0CPacketSpawnPlayer.class.getDeclaredField(MCPNames.field("field_148960_i"));
			//dataWatcherField.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if(terminate) {
			return;
		}

		if(ctx == null) {
			ctx = this.ctx;
		}

		if(msg instanceof Packet) {
			super.channelRead(ctx, msg);
			return;
		}
		byte[] ba = (byte[])msg;

		try {
			Packet p = ReplayFileIO.deserializePacket(ba);

			if(p == null) return;

			//If hurrying, ignore some packets, unless during Replay Path and *not* in initial hurry
			if(hurryToTimestamp && (!ReplayHandler.isInPath() || (desiredTimeStamp-currentTimeStamp > 1000))) {
				if(p instanceof S45PacketTitle ||
						p instanceof S2APacketParticles) return;
			}

			if(p instanceof S29PacketSoundEffect && ReplayHandler.isInPath() && ReplayProcess.isVideoRecording()) {
				return;
			}

			if(p instanceof S03PacketTimeUpdate) {
				p = TimeHandler.getTimePacket((S03PacketTimeUpdate)p);
			}

			if(p instanceof S48PacketResourcePackSend) {
				S48PacketResourcePackSend pa = (S48PacketResourcePackSend)p;
				Thread t = new ResourcePackCheck(pa.func_179783_a(), pa.func_179784_b());
				t.start();

				return;
			}

			if(p instanceof S02PacketChat) {
				byte pos = (Byte)chatPacketPosition.get(p);
				if(pos == 1) { //Ignores command block output sent
					return;
				}
			}

			if(badPackets.contains(p.getClass())) return;

			/*
			if(p instanceof S0EPacketSpawnObject) {
				if(mc.theWorld != null) {
					List<EntityArrow> arrows = mc.theWorld.getEntities(EntityArrow.class, new Predicate<EntityArrow>() {
						@Override
						public boolean apply(EntityArrow input) {
							return true;
						}
					});
 					if(arrows.size() > 20) {
						System.out.println(currentTimeStamp);
					}
				}
			}
			 */

			try {
				if(p instanceof S1CPacketEntityMetadata) {
					if((Integer)metadataPacketEntityId.get(p) == actualID) {
						metadataPacketEntityId.set(p, RecordingHandler.entityID);
					}
				}

				if(p instanceof S01PacketJoinGame) {
					//System.out.println("FOUND JOIN PACKET");
					allowMovement = true;
					int entId = (Integer)joinPacketEntityId.get(p);
					actualID = entId;
					entId = Integer.MIN_VALUE+9002;
					int dimension = (Integer)joinPacketDimension.get(p);
					EnumDifficulty difficulty = (EnumDifficulty)joinPacketDifficulty.get(p);
					int maxPlayers = (Integer)joinPacketMaxPlayers.get(p);
					WorldType worldType = (WorldType)joinPacketWorldType.get(p);

					p = new S01PacketJoinGame(entId, GameType.SPECTATOR, false, dimension, 
							difficulty, maxPlayers, worldType, false);
				}

				if(p instanceof S07PacketRespawn) {
					S07PacketRespawn respawn = (S07PacketRespawn)p;
					p = new S07PacketRespawn(respawn.func_149082_c(), 
							respawn.func_149081_d(), respawn.func_149080_f(), GameType.SPECTATOR);

					allowMovement = true;
				}

				/*
				 * Proof of concept for some nasty player manipulation ;)
				String crPxl = "2cb08a5951f34e98bd0985d9747e80df";
				String johni = "cd3d4be14ffc2f9db432db09e0cd254b";

				if(p instanceof S38PacketPlayerListItem) {
					S38PacketPlayerListItem pp = (S38PacketPlayerListItem)p;
					if(((AddPlayerData)pp.func_179767_a().get(0)).func_179962_a().getId().toString().replace("-", "").equals(crPxl)) {
						GameProfile johniGP = new GameProfile(UUID.fromString(johni.replaceAll(                                            
								"(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",                            
								"$1-$2-$3-$4-$5")), "Johni0702");
						gameProfileField.set(pp.func_179767_a().get(0), johniGP);
						//pp.func_179767_a().set(0, johniGP);
						p = pp;
					}

				}

				if(p instanceof S0CPacketSpawnPlayer) {
					S0CPacketSpawnPlayer sp = (S0CPacketSpawnPlayer)p;

					if(sp.func_179819_c().toString().replace("-", "").equals(crPxl)) {
						playerUUIDField.set(sp, UUID.fromString(johni.replaceAll(                                            
								"(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",                            
								"$1-$2-$3-$4-$5")));
					}

					p = sp;
				}
				 */

				/*
				if(p instanceof S0CPacketSpawnPlayer) {
					System.out.println(dataWatcherField.get(p));
					System.out.println(((S0CPacketSpawnPlayer) p).func_148944_c());
				}
				 */

				if(p instanceof S08PacketPlayerPosLook) {
					final S08PacketPlayerPosLook ppl = (S08PacketPlayerPosLook)p;

					if(ReplayHandler.isInPath() && !hurryToTimestamp) return;

					CameraEntity cent = ReplayHandler.getCameraEntity();

					if(cent != null) {
						if(!allowMovement && !((Math.abs(cent.posX - ppl.func_148932_c()) > ReplayMod.TP_DISTANCE_LIMIT) || 
								(Math.abs(cent.posZ - ppl.func_148933_e()) > ReplayMod.TP_DISTANCE_LIMIT))) {
							return;
						} else {
							allowMovement = false;
						}
					}

					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							while(mc.theWorld == null) {
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}

							Entity ent = ReplayHandler.getCameraEntity();

							if(ent == null || !(ent instanceof CameraEntity)) ent = new CameraEntity(mc.theWorld);
							CameraEntity cent = (CameraEntity)ent;
							cent.moveAbsolute(ppl.func_148932_c(), ppl.func_148928_d(), ppl.func_148933_e());

							ReplayHandler.setCameraEntity(cent);
						}
					});

					t.start();
				}

				if(p instanceof S43PacketCamera) {
					return;
				}

				super.channelRead(ctx, p);
			} catch(Exception e) {
				System.out.println(p.getClass());
				e.printStackTrace();
			}

		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
		networkManager.channel().attr(networkManager.attrKeyConnectionState).set(EnumConnectionState.PLAY);
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		archive.close();
		super.channelInactive(ctx);
	}

	public boolean paused() {
		try {
			return MCTimerHandler.getTimerSpeed() == 0;
		} catch(Exception e) {}
		return true;
	}

	public double getReplaySpeed() {
		if(!paused()) return replaySpeed;
		else return 0;
	}

	public File getReplayFile() {
		return replayFile;
	}

}
