package com.replaymod.pathing.property;

import net.minecraft.client.resources.I18n;

/**
 * Abstract base class for most properties.
 */
public abstract class AbstractProperty<T> implements Property<T> {
    private final String id, localizationKey;
    private final PropertyGroup propertyGroup;
    private final T initialValue;

    public AbstractProperty(String id, String localizationKey, PropertyGroup propertyGroup, T initialValue) {
        this.id = id;
        this.localizationKey = localizationKey;
        this.propertyGroup = propertyGroup;
        this.initialValue = initialValue;

        if (propertyGroup != null) {
            propertyGroup.getProperties().add(this);
        }
    }

    @Override
    public String getLocalizedName() {
        return I18n.format(localizationKey);
    }

    @Override
    public PropertyGroup getGroup() {
        return propertyGroup;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public T getNewValue() {
        return initialValue;
    }
}
