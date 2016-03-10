package com.replaymod.pathing.property;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.replay.ReplayHandler;
import lombok.NonNull;

import java.io.IOException;
import java.util.Collection;

/**
 * Represents a property of a property.
 * Such properties may or may not be interpolated between keyframes.
 * <br>
 * If a property cannot be interpolated between keyframes, it is only active between two keyframes having the same
 * value for that property.
 *
 * @param <T> The type of the property, must be immutable
 */
public interface Property<T> {
    /**
     * Returns the localized name of this property.
     *
     * @return Localized name.
     */
    @NonNull
    String getLocalizedName();

    /**
     * Returns the group this property belongs to.
     *
     * @return The group
     */
    PropertyGroup getGroup();

    /**
     * Returns an ID unique for this property.
     * There may be multiple instances for the same property ID
     * if they all represent the same concept (all x-Coordinate).
     *
     * @return Unique ID
     */
    @NonNull
    String getId();

    /**
     * Returns a new value for this property.
     * @return New value, may be {@code null}
     */
    T getNewValue();

    /**
     * Returns (optionally) interpolatable parts of this property.
     * @return Collection of parts
     */
    Collection<PropertyPart<T>> getParts();

    /**
     * Appy the specified value of this property to the game.
     * @param value The value of this property
     * @param replayHandler The ReplayHandler instance
     */
    void applyToGame(T value, ReplayHandler replayHandler);

    /**
     * Writes the specified value of this property to JSON.
     * @param writer The json writer
     * @param value The value
     */
    void toJson(JsonWriter writer, T value) throws IOException;

    /**
     * Reads the value of this property from JSON.
     * @param reader The json reader
     * @return The value
     */
    T fromJson(JsonReader reader) throws IOException;
}
