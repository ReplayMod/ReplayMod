package com.replaymod.recording.handler;

import java.nio.ByteBuffer;

import com.google.api.client.json.Json;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.ModCompat;
import com.replaymod.core.utils.Utils;
import com.replaymod.recording.Setting;
import com.replaymod.recording.gui.GuiRecordingOverlay;
import com.replaymod.recording.packet.PacketListener;
import com.replaymod.replaystudio.replay.Replay;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
//import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.replay.StreamReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesisfirehose.model.DeliveryStreamDescription;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;


import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.codec.binary.Hex;

import com.amazonaws.auth.BasicSessionCredentials;


import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;



//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import scala.xml.dtd.PublicID;

import static com.replaymod.core.versions.MCVer.WorldType_DEBUG_ALL_BLOCK_STATES;
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
//#endif

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.net.Socket;
import java.net.DatagramSocket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
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

    private NetworkManager networkManager;
    private DatagramSocket userServerSocket;
    private Socket         mcServerSocket;

    private Thread recordingManager;

    private String uid;

    public ConnectionEventHandler(Logger logger, ReplayMod core) {
        this.logger = logger;
        this.core = core;

        //Set prefered network stack to ipv4
        System.setProperty("java.net.preferIPv4Stack" , "true");
    }

    private void manageRecording(){
        try{
            logger.error("Recording Service Started");
            BufferedReader in = new BufferedReader(new InputStreamReader(mcServerSocket.getInputStream()));
            String jsonStr = "";
            int connectionAttempts = 10;
            while (true){
                try{
                    jsonStr = in.readLine();
                    
                    if( jsonStr != null){
                        
                        JsonObject recordObject = new JsonParser().parse(jsonStr).getAsJsonObject();
                        if (recordObject.has("record")){
                            boolean recordFlag = recordObject.get("record").getAsBoolean();

                            if (recordObject.has("experiment")) {
                                JsonObject experimentMetaData = recordObject.get("experiment").getAsJsonObject();
                                logger.error("I parsed metadata :" +  experimentMetaData.toString());

                                if(recordFlag){
                                    markStartRecording(experimentMetaData.toString());}
                                else{
                                    markStopRecording(experimentMetaData.toString());}
                            }
                            else {
                                logger.error("Experiment field not present! Not recording!");
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    logger.error(e.toString());
                    logger.error(jsonStr);
                    logger.error("JsonSyntaxException in parsing string!");
                    markStopRecording("{}");
                } catch (IOException e)  {
                    logger.error("Issue connecting to minecraft server - TCP connection error");
                    if (connectionAttempts-- < 1) {
                        logger.error("Giving up on connecting to serever!");
                        logger.error("Trying to quit...");
                        onDisconnectedFromServerEvent(null);
                    } else {
                        logger.info("Opening new connection to server");
                        mcServerSocket.connect(mcServerSocket.getRemoteSocketAddress(), 500);
                        in = new BufferedReader(new InputStreamReader(mcServerSocket.getInputStream()));
                    }
                    
                }

                Thread.sleep(50);
            }
        }
        catch (IOException e) {
            logger.error("Could not create reader! Exiting recording manager");
            return;
        }
        catch (InterruptedException e) {
            logger.error("Recording manager stopped");
            return;
        }
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

            //Excnage key with minecraft server
            if (!authenticateWithServer()) {
                return;
            }

            //File folder = core.getReplayFolder();
            //String name = sdf.format(Calendar.getInstance().getTime());
            //File currentFile = new File(folder, Utils.replayNameToFileName(name));
            //ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), currentFile);
            
            ReplayFile replayFile = new StreamReplayFile(new ReplayStudio(), uid, logger);

            replayFile.writeModInfo(ModCompat.getInstalledNetworkMods());

            ReplayMetaData metaData = new ReplayMetaData();
            metaData.setSingleplayer(local);
            metaData.setServerName(worldName);
            metaData.setGenerator("ReplayMod v" + ReplayMod.getContainer().getVersion());
            metaData.setDate(System.currentTimeMillis());
            metaData.setMcVersion(ReplayMod.getMinecraftVersion());
            packetListener = new PacketListener(replayFile, metaData);
            networkManager.channel().pipeline().addBefore(packetHandlerKey, "replay_recorder", packetListener);

            recordingEventHandler = new RecordingEventHandler(packetListener, logger);
            recordingEventHandler.register();

            guiOverlay = new GuiRecordingOverlay(mc, core.getSettingsRegistry());
            guiOverlay.register();

            core.printInfoToChat("replaymod.chat.recordingstarted");
            
            ////////////////////////////////////////////
            //        Recording Manager Thread        //
            ////////////////////////////////////////////

            Runnable recordingService = new Runnable(){
                public void run(){
                    manageRecording();
                }
            };

            recordingManager = new Thread(recordingService);
            recordingManager.start();

            ////////////////////////////////////////////
            //           User Notification            //
            ////////////////////////////////////////////

            if(replayFile instanceof StreamReplayFile){
                core.printInfoToChat("Stream info: ");
                core.printInfoToChat(((StreamReplayFile)replayFile).getStreamName());
                core.printInfoToChat("Version " + ((StreamReplayFile)replayFile).getStreamVersion());
            }
            
        } catch (Throwable e) {
            e.printStackTrace();
            core.printWarningToChat("replaymod.chat.recordingfailed");
            if (recordingManager != null) {
                recordingManager.interrupt(); 
            }
            // Unregister existing handlers
            if (packetListener != null) {
                logger.info("Trying to unregister guiOverlay");
                guiOverlay.unregister();
                guiOverlay = null;
                logger.info("Trying to unregister the event handler");
                recordingEventHandler.unregister();
                recordingEventHandler = null;
                packetListener = null;
            }
        }
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(ClientDisconnectionFromServerEvent event) {
        // Unregister existing handlers
        if (packetListener != null) {
  
            recordingManager.interrupt(); 
            markStopRecording("{}");
            core.printInfoToChat("Recording Stoped");

            logger.info("Trying to unregister guiOverlay");
            guiOverlay.unregister();
            guiOverlay = null;
            logger.info("Trying to unregister the event handler");
            recordingEventHandler.unregister();

            recordingEventHandler = null;
            packetListener = null;
        }
    }

    private void markStartRecording(String experimentMetadata){
        if (packetListener != null) {
            logger.info("Recording experment metadata - Start Recording!");
            packetListener.setExperementMetadata(experimentMetadata);
            packetListener.addMarker(true);
        }
        
    }

    private void markStopRecording(String experimentMetadata){
        if (packetListener != null) {
            logger.info("Recording experment metadata");
            packetListener.setExperementMetadata(experimentMetadata);
            packetListener.addMarker(false);
        }
        
    }

    /** Event Handler
     * 
     * Returns true if authentication was sucessfull
    * 
    * configures userServerSocket -> userServer
    * configures mcServerSocket -> current MinecraftServer
    * gets a Minecraft play key
    * sends authorization to minecraft server
    * establishes thread to listen for recording start/stop events
    */
    public boolean authenticateWithServer() {
        try{
            String mcUsername = mc.getSession().getUsername();
            String mcUUID = mc.getSession().getPlayerID();
            MessageDigest hashFn = MessageDigest.getInstance("MD5");
            byte[] uid_raw = hashFn.digest(mcUUID.getBytes("UTF-8"));
            this.uid = Hex.encodeHexString(uid_raw);
            logger.info(String.format("UID: %s%n", uid));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e){
            e.printStackTrace();
        }

        // Create a UDP sockets and connect them to the UserServer and to MinecraftServer
        InetAddress userServerAddress;
        try {
            //Connect to UserServer
            userServerSocket = new DatagramSocket();
            userServerAddress = InetAddress.getByName("user.herobraine.stream");
            userServerSocket.connect(userServerAddress, 9999);
            userServerSocket.setSoTimeout(1000);                        
        } catch (SocketException | UnknownHostException e) {
            logger.info("Error establishing connection to user server");
            e.printStackTrace();
            logger.error("Error establishing connection to user server");
            return false;
        }
        InetAddress mcServerAddress;
        PrintWriter mcServerOut = null;
        try {       
            //Connect to MinecraftServer
            String minecraft_ip = Minecraft.getMinecraft().getCurrentServerData().serverIP;
            
            int portIdx = minecraft_ip.indexOf(':'); // If port is part of the Minecraft server IP remove it
            if (portIdx != -1){ minecraft_ip = minecraft_ip.substring(0, portIdx); }

            logger.info("Establishing connection to minecraft server: " + minecraft_ip);
            mcServerAddress = InetAddress.getByName(minecraft_ip); 
            mcServerSocket = new Socket();
            mcServerSocket.setKeepAlive(true);
            mcServerSocket.connect(new InetSocketAddress(mcServerAddress, 8888), 500);
            mcServerOut = new PrintWriter(new DataOutputStream(mcServerSocket.getOutputStream()), true);
        } catch (IOException e) {
            logger.error("Error establishing connection to minecraft server");
            return false;
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
            return false;
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
            e.printStackTrace();
            userServerSocket.close();
            return false;
        }
        
        logger.info(String.format("Minecraft Key:    %s%n", minecraftKey));

        // Send key to Minecraft Server
        JsonObject authJson = new JsonObject();
        authJson.addProperty("cmd", "authorize_user");
        authJson.addProperty("uid", uid);
        authJson.addProperty("minecraft_key", minecraftKey);
        String authStr = authJson.toString();      
        if (mcServerOut != null){
            mcServerOut.write(authStr);
            mcServerOut.append('\n');
            mcServerOut.flush();
        } else {
            logger.error("Minecraft server connection is not complete - not sending play key");
        } 
        
        // Get response
        boolean authenicated = false;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(mcServerSocket.getInputStream()));
            String jsonStr = "";
            jsonStr = in.readLine();
            JsonObject authResponse = new JsonParser().parse(jsonStr).getAsJsonObject();
            authenicated = authResponse.get("authorized").getAsBoolean();
        } catch (JsonSyntaxException | IOException e) {
            e.printStackTrace();
            userServerSocket.close();
            logger.error("Error recieving response from server");
            return false;
        }
        
        logger.info(String.format("Minecraft Key:    %s%n", minecraftKey));
        logger.info(String.format("Server response: %s%n", Boolean.toString(authenicated)));
        return authenicated;
    }

    public PacketListener getPacketListener() {
        return packetListener;
    }
}
