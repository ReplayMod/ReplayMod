package com.replaymod.recording.handler;

import java.nio.ByteBuffer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.ModCompat;
import com.replaymod.core.utils.Utils;
import com.replaymod.recording.Setting;
import com.replaymod.recording.gui.GuiRecordingOverlay;
import com.replaymod.recording.packet.PacketListener;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.replay.StreamReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.codec.binary.Hex;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesisfirehose.model.DeliveryStreamDescription;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;


import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;



//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

import static com.replaymod.core.versions.MCVer.WorldType_DEBUG_ALL_BLOCK_STATES;
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
//#endif

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.io.IOException;
import java.net.UnknownHostException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;

//TODO remove
import java.util.concurrent.TimeUnit;

/**
 * Handles connection events and initiates recording if enabled.
 */
public class ConnectionEventHandler {

    private static final String packetHandlerKey = "packet_handler";
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final Logger logger;
    private final ReplayMod core;

    private RecordingEventHandler recordingEventHandler;
    private PacketListener packetListener;
    private GuiRecordingOverlay guiOverlay;

    public ConnectionEventHandler(Logger logger, ReplayMod core) {
        this.logger = logger;
        this.core = core;
    }

    public void onConnectedToServerEvent(NetworkManager networkManager) {
        try {
            boolean local = networkManager.isLocalChannel();
            if (local) {
                //#if MC>=10800
                if (mc.getIntegratedServer().getEntityWorld().getWorldType() == WorldType_DEBUG_ALL_BLOCK_STATES) {
                    logger.info("Debug World recording is not supported.");
                    return;
                }
                //#endif
                if(!core.getSettingsRegistry().get(Setting.RECORD_SINGLEPLAYER)) {
                    logger.info("Singleplayer Recording is disabled");
                    return;
                }
            } else {
                if(!core.getSettingsRegistry().get(Setting.RECORD_SERVER)) {
                    logger.info("Multiplayer Recording is disabled");
                    return;
                } else {
                    // Get Minecraft Server IP 
                    // TODO Fix this
                    //INetHandler handler = netowrkManager.getNetHandler();

                    // Get Minecraft Username
                    String mcUsername = mc.getSession().getUsername();
                    MessageDigest hashFn = MessageDigest.getInstance("MD5");
                    byte[] uid_raw = hashFn.digest(mcUsername.getBytes("UTF-8"));
                    String uid = Hex.encodeHexString(uid_raw);
                    logger.info(String.format("UID: %s%n", uid));

                    // Create a UDP sockets and connect them to the UserServer and to MinecraftServer
                    DatagramSocket userServerSocket, mcServerSocket;
                    InetAddress userServerAddress, mcServerAddress;
                    try {
                        //Connect to UserServer
                        userServerSocket = new DatagramSocket();
                        userServerAddress = InetAddress.getByName("18.206.147.166"); // TODO use configured IP
                        userServerSocket.connect(userServerAddress, 9999);
                        userServerSocket.setSoTimeout(1000);
                        
                        //Connect to MinecraftServer
                        mcServerSocket = new DatagramSocket();
                        mcServerAddress = InetAddress.getByName("18.188.31.64"); // TODO use configured IP
                        mcServerSocket.connect(mcServerAddress, 8888);
                        mcServerSocket.setSoTimeout(1000);
                        
                    } catch (SocketException | UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return;
                    }
                    
                    ////////////////////////////////////////////
                    //       MC Server Auth Exchange          //
                    ////////////////////////////////////////////
                    
                    // Send Minecraft key request
                    JsonObject mcKeyJson = new JsonObject();
                    mcKeyJson.addProperty("cmd", "get_minecraft_key");
                    mcKeyJson.addProperty("uid", uid);
                    String mcKeyStr = mcKeyJson.toString();
                    DatagramPacket mcKeyRequest = new DatagramPacket(mcKeyStr.getBytes(), mcKeyStr.getBytes().length);
                    try {
                        userServerSocket.send(mcKeyRequest);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        userServerSocket.close();
                        mcServerSocket.close();
                        return;
                    }
                
                    // Get response
                    byte[] buff = new byte[65535];
                    String minecraftKey;
                    DatagramPacket minecraftKeyData = new DatagramPacket(buff, buff.length, userServerAddress, userServerSocket.getLocalPort());
                    try {
                        userServerSocket.receive(minecraftKeyData);
                        JsonObject minecraftKeyJson = new JsonParser().parse(new String(buff, 0, minecraftKeyData.getLength())).getAsJsonObject();
                        minecraftKey = minecraftKeyJson.get("minecraft_key").getAsString();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        userServerSocket.close();
                        mcServerSocket.close();
                        return;
                    }
                    
                    logger.info(String.format("Minecraft Key:    %s%n", minecraftKey));
                    
                    // Send key to Minecraft Server
                    JsonObject authJson = new JsonObject();
                    authJson.addProperty("cmd", "authorize_user");
                    authJson.addProperty("uid", uid);
                    authJson.addProperty("key", minecraftKey);
                    String authStr = authJson.toString();
                    DatagramPacket auth = new DatagramPacket(authStr.getBytes(), authStr.getBytes().length);
                    try {
                        mcServerSocket.send(auth);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        userServerSocket.close();
                        mcServerSocket.close();
                        return;
                    }
                    
                    ////////////////////////////////////////////
                    //       FireHose Key Retrieval           //
                    ////////////////////////////////////////////
                    // Send Firehose key request
                    JsonObject firehoseJson  = new JsonObject();
                    firehoseJson.addProperty("cmd", "get_firehose_key");
                    firehoseJson.addProperty("uid", uid);
                    String firehoseStr = firehoseJson.toString();
                    DatagramPacket firehoseKeyRequest = new DatagramPacket(firehoseStr.getBytes(), firehoseStr.getBytes().length);
                    try {
                        userServerSocket.send(firehoseKeyRequest);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        userServerSocket.close();
                        mcServerSocket.close();
                        return;
                    }
                
                    // Get response
                    byte[] buff1 = new byte[65535];
                    String streamName;
                    String accessKey;
                    String secretKey;
                    String sessionToken;
                    DatagramPacket firehoseKeyData = new DatagramPacket(buff1, buff1.length, userServerAddress, userServerSocket.getLocalPort());
                    try {
                        userServerSocket.receive(firehoseKeyData);
                        JsonObject awsKeys = new JsonParser().parse(new String(buff1, 0, firehoseKeyData.getLength())).getAsJsonObject();
                        streamName   = awsKeys.get("stream_name").getAsString();
                        accessKey    = awsKeys.get("access_key").getAsString();
                        secretKey    = awsKeys.get("secret_key").getAsString();
                        sessionToken = awsKeys.get("session_token").getAsString();
                        
                    
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        userServerSocket.close();
                        mcServerSocket.close();
                        return;
                    }
                    
                    logger.info(String.format("StreamName:    %s%n", streamName));
                    logger.info(String.format("Access Key:    %s%n", accessKey));
                    logger.info(String.format("Secret Key:    %s%n", secretKey));
                    logger.info(String.format("Session Token: %s%n", sessionToken));

                    //Create test client
                    BasicSessionCredentials session_credentials = new BasicSessionCredentials(accessKey, secretKey, sessionToken);
                    AmazonKinesisFirehoseClient firehoseClient = new AmazonKinesisFirehoseClient(session_credentials);

                    //Check if the given stream is open
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + (10 * 60 * 1000);
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            Thread.sleep(1000 * 20);
                        } catch (InterruptedException e) {
                            // Ignore interruption (doesn't impact deliveryStream creation)
                        }

                        DescribeDeliveryStreamRequest describeDeliveryStreamRequest = new DescribeDeliveryStreamRequest();
                        describeDeliveryStreamRequest.withDeliveryStreamName(streamName);
                        DescribeDeliveryStreamResult describeDeliveryStreamResponse =
                        firehoseClient.describeDeliveryStream(describeDeliveryStreamRequest);
                        DeliveryStreamDescription  deliveryStreamDescription = describeDeliveryStreamResponse.getDeliveryStreamDescription();
                        String deliveryStreamStatus = deliveryStreamDescription.getDeliveryStreamStatus();
                        if (deliveryStreamStatus.equals("ACTIVE")) {
                            break;
                        }
                    }


                    // Put records on stream
                    PutRecordRequest putRecordRequest = new PutRecordRequest();
                    putRecordRequest.setDeliveryStreamName(streamName);

                    String data = "This is a test" + "\n";
                    Record record = new Record().withData(ByteBuffer.wrap(data.getBytes()));
                    putRecordRequest.setRecord(record);

                    // Put record into the DeliveryStream
                    firehoseClient.putRecord(putRecordRequest);

                
                    userServerSocket.close();
                    mcServerSocket.close();                    
                }
            }

            String worldName;
            if (local) {
                worldName = mc.getIntegratedServer().getWorldName();
            } else if (Minecraft.getMinecraft().getCurrentServerData() != null) {
                worldName = Minecraft.getMinecraft().getCurrentServerData().serverIP;
            //#if MC>=11100
            } else if (Minecraft.getMinecraft().isConnectedToRealms()) {
                // we can't access the server name without tapping too deep in the Realms Library
                worldName = "A Realms Server";
            //#endif
            } else {
                logger.info("Recording not started as the world is neither local nor remote (probably a replay).");
                return;
            }

            File folder = core.getReplayFolder();

            String name = sdf.format(Calendar.getInstance().getTime());
            File currentFile = new File(folder, Utils.replayNameToFileName(name));
            ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), currentFile);

            replayFile.writeModInfo(ModCompat.getInstalledNetworkMods());

            ReplayMetaData metaData = new ReplayMetaData();
            metaData.setSingleplayer(local);
            metaData.setServerName(worldName);
            metaData.setGenerator("ReplayMod v" + ReplayMod.getContainer().getVersion());
            metaData.setDate(System.currentTimeMillis());
            metaData.setMcVersion(ReplayMod.getMinecraftVersion());
            packetListener = new PacketListener(replayFile, metaData);
            networkManager.channel().pipeline().addBefore(packetHandlerKey, "replay_recorder", packetListener);

            recordingEventHandler = new RecordingEventHandler(packetListener);
            recordingEventHandler.register();

            guiOverlay = new GuiRecordingOverlay(mc, core.getSettingsRegistry());
            guiOverlay.register();

            core.printInfoToChat("replaymod.chat.recordingstarted");
        } catch (Throwable e) {
            e.printStackTrace();
            core.printWarningToChat("replaymod.chat.recordingfailed");
        }
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(ClientDisconnectionFromServerEvent event) {
        if (packetListener != null) {
            guiOverlay.unregister();
            guiOverlay = null;
            recordingEventHandler.unregister();
            recordingEventHandler = null;
            packetListener = null;
        }
    }

    public PacketListener getPacketListener() {
        return packetListener;
    }
}
