package com.replaymod.pathing.property;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for most property groups.
 */
@RequiredArgsConstructor
public abstract class AbstractPropertyGroup implements PropertyGroup {
    private final String id, localizationKey;
    private final List<Property> properties = new ArrayList<>();

    @Override
    public String getLocalizedName() {
        return I18n.format(localizationKey);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Property> getProperties() {
        return properties;
    }
}
