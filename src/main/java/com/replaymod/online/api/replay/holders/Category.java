package com.replaymod.online.api.replay.holders;

import net.minecraft.client.resource.language.I18n;

public enum Category {

    SURVIVAL(0, "replaymod.category.survival"), MINIGAME(1, "replaymod.category.minigame"),
    BUILD(2, "replaymod.category.build"), MISCELLANEOUS(3, "replaymod.category.misc");

    private int id;
    private String name;

    Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public static Category fromId(int id) {
        for(Category c : values()) {
            if(c.id == id) return c;
        }
        return null;
    }

    public String toNiceString() {
        return I18n.translate(this.name);
    }

    public Category next() {
        for(int i = 0; i < values().length; i++) {
            if(values()[i] == this) {
                if(i == values().length - 1) {
                    i = -1;
                }
                return values()[i + 1];
            }
        }
        return this;
    }

    public static String[] stringValues() {
        String[] values = new String[Category.values().length];
        int i = 0;
        for (Category c : Category.values()) {
            values[i++] = c.toNiceString();
        }
        return values;
    }
}
