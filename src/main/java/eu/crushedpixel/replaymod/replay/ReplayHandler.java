package eu.crushedpixel.replaymod.replay;

// For later use in render and path module
public class ReplayHandler {
//
//    private static Minecraft mc = Minecraft.getMinecraft();
//    private static long lastExit;
//    private static NetworkManager networkManager;
//    private static EmbeddedChannel channel;
//    private int realTimelinePosition = 0;
//
//    private static Keyframe selectedKeyframe;
//
//    private static boolean inPath = false;
//
//    private static AdvancedPositionKeyframeList positionKeyframes = new AdvancedPositionKeyframeList();
//    private static KeyframeList<TimestampValue> timeKeyframes = new KeyframeList<TimestampValue>();
//
//    private static boolean inReplay = false;
//    private static AdvancedPosition lastPosition = null;
//
//    private static Set<Marker> markers;
//
//    private static float cameraTilt = 0;
//
//    private static KeyframeSet[] keyframeRepository = new KeyframeSet[]{};
//
//    @Getter @Setter
//    private static AssetRepository assetRepository;
//
//    private static CustomObjectRepository customImageObjects = new CustomObjectRepository();
//
//    /**
//     * The file currently being played.
//     */
//    private static ReplayFile currentReplayFile;
//
//    /**
//     * Currently active replay restrictions.
//     */
//    private static Restrictions restrictions;
//
//    /**
//     * The EntityPositionTracker for the current Replay File.
//     */
//    @Getter
//    private static EntityPositionTracker entityPositionTracker;
//
//    public static KeyframeSet[] getKeyframeRepository() {
//        return keyframeRepository;
//    }
//
//    public static void setKeyframeRepository(KeyframeSet[] repo, boolean write) {
//        keyframeRepository = repo;
//        if(write) {
//            try (OutputStream out = currentReplayFile.write("paths.json")) {
//                out.write(new Gson().toJson(repo).getBytes());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public static Set<Marker> getMarkers() {
//        return markers;
//    }
//
//    public static void setMarkers(Set<Marker> markers, boolean write) {
//        ReplayHandler.markers = markers;
//        if(write) {
//            try {
//                currentReplayFile.writeMarkers(markers);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public static void useKeyframePresetFromRepository(int index) {
//        useKeyframePreset(keyframeRepository[index]);
//    }
//
//    public static void useKeyframePreset(KeyframeSet keyframeSet) {
//        setCustomImageObjects(Arrays.asList(keyframeSet.getCustomObjects()));
//
//        Keyframe[] kfs = keyframeSet.getKeyframes();
//
//        positionKeyframes.clear();
//        timeKeyframes.clear();
//        for(Keyframe kf : kfs) {
//            addKeyframe(kf);
//        }
//
//        selectKeyframe(null);
//
//        fireKeyframesModifyEvent();
//    }
//
//    public static void spectateEntity(Entity e) {
//        getCameraEntity().spectate(e);
//    }
//
//    public static void spectateCamera() {
//        spectateEntity(null);
//    }
//
//    public static boolean isCamera() {
//        return mc.thePlayer instanceof CameraEntity && mc.thePlayer == mc.getRenderViewEntity();
//    }
//
//    public static void startPath(RenderOptions renderOptions, boolean fromStart) {
//        if(!com.replaymod.replay.ReplayHandler.isInPath()) {
//            try {
//                ReplayProcess.startReplayProcess(renderOptions, fromStart);
//            } catch (ReportedException e) {
//                // We have to manually unwrap OOM errors as Minecraft doesn't handle them when they're wrapped
//                Throwable prevCause = null;
//                Throwable cause = e;
//                while (cause != null && cause != prevCause) {
//                    if (cause instanceof OutOfMemoryError) {
//                        // Nevertheless save the crash report in case we actually need it
//                        Minecraft minecraft = Minecraft.getMinecraft();
//                        CrashReport crashReport = e.getCrashReport();
//                        minecraft.addGraphicsAndWorldToCrashReport(crashReport);
//                        Bootstrap.printToSYSOUT(crashReport.getCompleteReport());
//                        File folder = new File(minecraft.mcDataDir, "crash-reports");
//                        File file = new File(folder, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-client.txt");
//                        crashReport.saveToFile(file);
//                        throw (OutOfMemoryError) cause;
//                    }
//                    prevCause = cause;
//                    cause = e.getCause();
//                }
//                throw e;
//            }
//        }
//    }
//
//    public static void interruptReplay() {
//        ReplayProcess.stopReplayProcess(false);
//    }
//
//    public static boolean isInPath() {
//        return inPath;
//    }
//
//    public static void setInPath(boolean replaying) {
//        inPath = replaying;
//    }
//
//    public static CameraEntity getCameraEntity() {
//        return mc.thePlayer instanceof CameraEntity ? (CameraEntity) mc.thePlayer : null;
//    }
//
//    public static float getCameraTilt() {
//        return cameraTilt;
//    }
//
//    public static void setCameraTilt(float tilt) {
//        cameraTilt = tilt;
//    }
//
//    public static void addCameraTilt(float tilt) {
//        cameraTilt += tilt;
//    }
//
//    public static void toggleMarker() {
//        // TODO
//    }
//
//    public static void addTimeKeyframe(Keyframe<TimestampValue> property) {
//        timeKeyframes.add(property);
//        selectKeyframe(property);
//
//        fireKeyframesModifyEvent();
//    }
//
//    public static void addPositionKeyframe(Keyframe<AdvancedPosition> property) {
//        positionKeyframes.add(property);
//        selectKeyframe(property);
//
//        fireKeyframesModifyEvent();
//    }
//
//    @SuppressWarnings("unchecked")
//    public static void addKeyframe(Keyframe property) {
//        if(property.getValue() instanceof AdvancedPosition) {
//            addPositionKeyframe(property);
//        } else if(property.getValue() instanceof TimestampValue) {
//            addTimeKeyframe(property);
//        }
//    }
//
//    public static void removeKeyframe(Keyframe property) {
//        if(property.getValue() instanceof AdvancedPosition) {
//            positionKeyframes.remove(property);
//        } else if(property.getValue() instanceof TimestampValue) {
//            timeKeyframes.remove(property);
//        }
//        // TODO Marker
//
//        if(property == selectedKeyframe) {
//            selectKeyframe(null);
//        }
//
//        fireKeyframesModifyEvent();
//    }
//
//    public static AdvancedPositionKeyframeList getPositionKeyframes() {
//        return positionKeyframes;
//    }
//
//    public static KeyframeList<TimestampValue> getTimeKeyframes() {
//        return timeKeyframes;
//    }
//
//    public static ArrayList<Keyframe> getAllKeyframes() {
//        ArrayList<Keyframe> keyframeList = new ArrayList<Keyframe>();
//        keyframeList.addAll(positionKeyframes);
//        keyframeList.addAll(timeKeyframes);
//
//        return keyframeList;
//    }
//
//    public static void resetKeyframes(final boolean resetMarkers, boolean callback) {
//        if(getPositionKeyframes().isEmpty() && getTimeKeyframes().isEmpty()) return;
//
//        if(!callback) {
//            resetKeyframes(resetMarkers);
//        } else {
//            mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
//                @Override
//                public void confirmClicked(boolean result, int id) {
//                    if(result) {
//                        resetKeyframes(resetMarkers);
//                    }
//
//                    mc.displayGuiScreen(null);
//                }
//            }, I18n.format("replaymod.gui.clearcallback.title"), I18n.format("replaymod.gui.clearcallback.message"), 1));
//        }
//    }
//
//    private static void resetKeyframes(boolean resetMarkers) {
//        timeKeyframes.clear();
//        positionKeyframes.clear();
//        selectKeyframe(null);
//
//        if(resetMarkers) {
//            // TODO Markers
//        }
//
////        setRealTimelineCursor(0); TODO
//
//        fireKeyframesModifyEvent();
//    }
//
//    public static void selectKeyframe(Keyframe kf) {
//        selectedKeyframe = kf;
//    }
//
//    public static boolean isSelected(Keyframe kf) {
//        return kf == selectedKeyframe;
//    }
//
//    public static boolean isInReplay() {
//        return inReplay;
//    }
//
//    public static void startReplay(File file) {
//        try {
//            startReplay(file, true);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void startReplay(File file, boolean asyncMode) throws IOException {
//        entityPositionTracker = new EntityPositionTracker(file);
//        entityPositionTracker.load();
//
//        ReplayMod.chatMessageHandler.initialize();
//        mc.ingameGUI.getChatGUI().clearChatMessages();
//        resetKeyframes(true);
//
//        if(ReplayMod.replaySender != null) {
//            ReplayMod.replaySender.terminateReplay();
//        }
//
//        if(channel != null) {
//            channel.close();
//        }
//
//        setCameraTilt(0);
//
//        restrictions = new Restrictions();
//
//        networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND) {
//            @Override
//            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
//                t.printStackTrace();
//            }
//        };
//        INetHandlerPlayClient pc = new NetHandlerPlayClient(mc, null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
//        networkManager.setNetHandler(pc);
//
//        channel = new EmbeddedChannel(networkManager);
//        channel.attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(networkManager));
//
//        // Open replay
//        currentReplayFile = new ZipReplayFile(new ReplayStudio(), file);
//
//        KeyframeSet[] paths;
//        Optional<InputStream> is = currentReplayFile.get("paths.json");
//        if (!is.isPresent()) {
//            is = currentReplayFile.get("paths");
//        }
//        if (is.isPresent()) {
//            try (Reader reader = new InputStreamReader(is.get())) {
//                paths = new GsonBuilder().registerTypeAdapter(KeyframeSet[].class, new LegacyKeyframeSetAdapter())
//                        .create().fromJson(reader, KeyframeSet[].class);
//            }
//        } else {
//            paths = new KeyframeSet[0];
//        }
//        com.replaymod.replay.ReplayHandler.setKeyframeRepository(paths == null ? new KeyframeSet[0] : paths, false);
//        com.replaymod.replay.ReplayHandler.selectKeyframe(null);
//        com.replaymod.replay.ReplayHandler.setMarkers(currentReplayFile.getMarkers().or(Collections.<Marker>emptySet()), false);
//        PlayerHandler.loadPlayerVisibilityConfiguration(currentReplayFile.getInvisiblePlayers());
//
//        //load assets
//        assetRepository = new AssetRepository();
//        for (ReplayAssetEntry entry : currentReplayFile.getAssets()) {
//            UUID uuid = entry.getUuid();
//            assetRepository.addAsset(entry.getName(), currentReplayFile.getAsset(uuid).get(), uuid);
//        }
//
//        customImageObjects = new CustomObjectRepository();
//
//        ReplayMod.replaySender = new ReplaySender(currentReplayFile, asyncMode);
//        channel.pipeline().addFirst(ReplayMod.replaySender);
//        channel.pipeline().fireChannelActive();
//
//        try {
//            ReplayMod.overlay.resetUI(true);
//        } catch(Exception e) {
//            e.printStackTrace();
//            // TODO: Fix exception
//        }
//
//        //Load lighting and trigger update
//        ReplayMod.replaySettings.setLightingEnabled(ReplayMod.replaySettings.isLightingEnabled());
//
//        inReplay = true;
//    }
//
//    public static void restartReplay() {
//        mc.ingameGUI.getChatGUI().clearChatMessages();
//
//        if(channel != null) {
//            channel.close();
//        }
//
//        restrictions = new Restrictions();
//
//        networkManager = new NetworkManager(EnumPacketDirection.CLIENTBOUND) {
//            @Override
//            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
//                t.printStackTrace();
//            }
//        };
//
//        INetHandlerPlayClient pc = new NetHandlerPlayClient(mc, null, networkManager, new GameProfile(UUID.randomUUID(), "Player"));
//        networkManager.setNetHandler(pc);
//
//        channel = new EmbeddedChannel(networkManager);
//        channel.attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(networkManager));
//
//        channel.pipeline().addFirst(ReplayMod.replaySender);
//        channel.pipeline().fireChannelActive();
//
//        mc.addScheduledTask(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    ReplayMod.overlay.resetUI(false);
//                } catch(Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        inReplay = true;
//    }
//
//    public static void endReplay() {
//        if(ReplayMod.replaySender != null) {
//            ReplayMod.replaySender.terminateReplay();
//        }
//
//        if (currentReplayFile != null) {
//            try {
//                currentReplayFile.save();
//                currentReplayFile.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            currentReplayFile = null;
//        }
//
//        resetKeyframes(true);
//
//        PlayerHandler.resetHiddenPlayers();
//        ReplayGuiRegistry.show();
//        LightingHandler.setLighting(false);
//
//        if (mc.theWorld != null) {
//            mc.theWorld.sendQuittingDisconnectingPacket();
//            mc.loadWorld(null);
//        }
//
//        CameraEntity.spectating = null;
//
//        inReplay = false;
//        lastExit = System.currentTimeMillis();
//
//        FMLCommonHandler.instance().bus().post(new ReplayExitEvent());
//    }
//
//    public static void setInReplay(boolean inReplay1) {
//        inReplay = inReplay1;
//    }
//
//    public static Keyframe getSelectedKeyframe() {
//        return selectedKeyframe;
//    }
//
////    public static int getRealTimelineCursor() {
////        return realTimelinePosition;
////    }
////
////    public static void setRealTimelineCursor(int pos) {
////        realTimelinePosition = pos;
////    }
//
//    public static AdvancedPosition getLastPosition() {
//        return lastPosition;
//    }
//
//    @Getter
//    private static boolean forceLastPosition = false;
//
//    public static void setLastPosition(AdvancedPosition position, boolean force) {
//        lastPosition = position;
//        forceLastPosition = force;
//    }
//
//    public static void moveCameraToLastPosition() {
//        //get the camera position we had before jumping in time
//        AdvancedPosition pos = com.replaymod.replay.ReplayHandler.getLastPosition();
//        CameraEntity cam = com.replaymod.replay.ReplayHandler.getCameraEntity();
//        if (cam != null && pos != null) {
//            // Move camera back in case we have been respawned, unless we're more than ReplayMod.TP_DISTANCE_LIMIT away from that point
//            // this is ignored if we explicitly said to respect this position, e.g. when jumping to marker keyframes.
//            if (com.replaymod.replay.ReplayHandler.isForceLastPosition() ||
//                    (Math.abs(pos.getX() - cam.posX) < ReplayMod.TP_DISTANCE_LIMIT &&
//                            Math.abs(pos.getZ() - cam.posZ) < ReplayMod.TP_DISTANCE_LIMIT)) {
//                cam.moveAbsolute(pos);
//            }
//        }
//    }
//
//    public static ReplayFile getReplayFile() {
//        return currentReplayFile;
//    }
//
//    /**
//     * Synchronizes the cursor on the Keyframe Timeline with the Replay Time
//     * @param ignoreReplaySpeed If true, it always uses 1.0 as the stretch factor
//     */
//    public static void syncTimeCursor(boolean ignoreReplaySpeed) {
//        selectKeyframe(null);
//
//        int curTime = ReplayMod.replaySender.currentTimeStamp();
//
//        int prevTime, prevRealTime;
//
//        Keyframe<TimestampValue> property = timeKeyframes.last();
//
//        if(property == null) {
//            prevTime = 0;
//            prevRealTime = 0;
//        } else {
//            prevTime = (int)property.getValue().value;
//            prevRealTime = property.getRealTimestamp();
//        }
//
//        double speed = ignoreReplaySpeed ? 1 : ReplayMod.overlay.getSpeedSliderValue();
//
//        int newCursorPos = Math.min(GuiReplayOverlay.KEYFRAME_TIMELINE_LENGTH, (int)(prevRealTime+((curTime-prevTime)/speed)));
//
////        setRealTimelineCursor(newCursorPos); TODO
//    }
//
//    public static List<CustomImageObject> getCustomImageObjects() {
//        return customImageObjects.getObjects();
//    }
//
//    public static void setCustomImageObjects(List<CustomImageObject> objects) {
//        customImageObjects.setObjects(new ArrayList<CustomImageObject>(objects));
//    }
//
//    public static void fireKeyframesModifyEvent() {
//        FMLCommonHandler.instance().bus().post(new KeyframesModifyEvent(positionKeyframes, timeKeyframes));
//        positionKeyframes.recalculate(ReplayMod.replaySettings.isLinearMovement());
//        timeKeyframes.recalculate(ReplayMod.replaySettings.isLinearMovement());
//    }
//
//    public static Restrictions getRestrictions() {
//        return restrictions;
//    }
}
