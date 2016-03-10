package com.replaymod.pathing.property;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.pathing.change.Change;
import lombok.NonNull;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Represents a group of property properties.
 * These groups are displayed and stored together.
 * The same property id may exist in multiple groups.<br>
 * Groups may also define a way of setting their properties in one simple way (e.g. "set to current position").
 */
public interface PropertyGroup {
    /**
     * Returns the localized name of this group.
     *
     * @return Localized name.
     */
    @NonNull
    String getLocalizedName();

    /**
     * Returns an ID unique for this group.
     *
     * @return Unique ID
     */
    @NonNull
    String getId();

    /**
     * Return a list of all properties in this group.
     *
     * @return List of properties or empty list if none.
     */
    @NonNull
    List<Property> getProperties();

    /**
     * Return a Callable which can be used to set all properties in this group at once.
     * The Callable returns a future which is completed once all properties have been updated.
     * It is up to the caller to apply those changes to the path. If those changes are not applied
     * immediately, their content may become invalid and applying them results in undefined behavior.
     *
     * @return Optional callable
     */
    @NonNull
    Optional<Callable<ListenableFuture<Change>>> getSetter();
}
