package com.replaymod.pathing.impl;

import com.google.common.base.Optional;
import com.replaymod.pathing.change.Change;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.Timeline;
import com.replaymod.pathing.property.Property;
import com.replaymod.replay.ReplayHandler;

import java.util.*;

public class TimelineImpl implements Timeline {
    private final List<Path> paths = new ArrayList<>();
    private Map<String, Property> properties = new HashMap<>();
    private Deque<Change> undoStack = new ArrayDeque<>();
    private Deque<Change> redoStack = new ArrayDeque<>();

    @Override
    public List<Path> getPaths() {
        return paths;
    }

    @Override
    public Path createPath() {
        Path path = new PathImpl(this);
        paths.add(path);
        return path;
    }

    @Override
    public <T> Optional<T> getValue(Property<T> property, long time) {
        for (Path path : paths) {
            Optional<T> value = path.getValue(property, time);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.absent();
    }

    @Override
    public void applyToGame(long time, ReplayHandler replayHandler) {
        for (Property<?> property : properties.values()) {
            applyToGame(time, replayHandler, property);
        }
    }

    private <T> void applyToGame(long time, ReplayHandler replayHandler, Property<T> property) {
        Optional<T> value = getValue(property, time);
        if (value.isPresent()) {
            property.applyToGame(value.get(), replayHandler);
        }
    }

    @Override
    public void registerProperty(Property property) {
        String id = (property.getGroup() == null ? "" : property.getGroup().getId() + ":") + property.getId();
        properties.put(id, property);
    }

    @Override
    public Property getProperty(String id) {
        return properties.get(id);
    }

    @Override
    public void applyChange(Change change) {
        change.apply(this);
        pushChange(change);
    }

    @Override
    public void pushChange(Change change) {
        undoStack.push(change);
        redoStack.clear();
    }

    @Override
    public void undoLastChange() {
        Change change = undoStack.pop();
        change.undo(this);
        redoStack.push(change);
    }

    @Override
    public void redoLastChange() {
        Change change = redoStack.pop();
        change.apply(this);
        undoStack.push(change);
    }

    @Override
    public Change peekUndoStack() {
        return undoStack.peek();
    }

    @Override
    public Change peekRedoStack() {
        return undoStack.peek();
    }
}
