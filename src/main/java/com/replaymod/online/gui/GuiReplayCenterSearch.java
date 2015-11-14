package com.replaymod.online.gui;

import com.google.common.base.Strings;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.replay.SearchQuery;
import com.replaymod.online.api.replay.holders.Category;
import com.replaymod.online.api.replay.holders.MinecraftVersion;
import com.replaymod.online.api.replay.pagination.SearchPagination;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.element.GuiToggleButton;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

public class GuiReplayCenterSearch extends AbstractGuiPopup<GuiReplayCenterSearch> {
    private final GuiReplayCenter replayCenter;
    private final ApiClient apiClient;
    public final GuiLabel title = new GuiLabel().setI18nText("replaymod.gui.center.search.filters").setColor(Colors.BLACK);
    public final GuiTextField name = new GuiTextField().setSize(120, 20).setI18nHint("replaymod.gui.center.search.name");
    public final GuiTextField server = new GuiTextField().setSize(120, 20).setI18nHint("replaymod.gui.center.search.server");
    public final GuiDropdownMenu<String> category = new GuiDropdownMenu<String>().setSize(120, 20);
    public final GuiDropdownMenu<String> version = new GuiDropdownMenu<String>().setSize(120, 20);
    public final GuiToggleButton<String> gameType = new GuiToggleButton<String>().setSize(120, 20);
    public final GuiToggleButton<String> sort = new GuiToggleButton<String>().setSize(120, 20);
    public final GuiButton cancelButton = new GuiButton().setSize(120, 20).onClick(new Runnable() {
        @Override
        public void run() {
            close();
        }
    }).setI18nLabel("replaymod.gui.cancel");
    public final GuiButton searchButton = new GuiButton().setSize(120, 20).onClick(new Runnable() {
        @Override
        public void run() {
            SearchQuery searchQuery = new SearchQuery();
            searchQuery.singleplayer = new Boolean[]{null,true, false}[gameType.getSelected()];
            searchQuery.category = category.getSelected() == 0 ? null : category.getSelected() - 1;
            searchQuery.version = version.getSelected() == 0
                    ? null : MinecraftVersion.values()[version.getSelected() - 1].getApiName();
            searchQuery.name = Strings.emptyToNull(name.getText().trim());
            searchQuery.server = Strings.emptyToNull(server.getText().trim());
            searchQuery.order = sort.getSelected() == 0;

            close();
            replayCenter.show(new SearchPagination(apiClient, searchQuery));
        }
    }).setI18nLabel("replaymod.gui.center.top.search");
    {
        List<String> categories = new ArrayList<>();
        categories.add(I18n.format("replaymod.gui.center.search.category"));
        for (Category c : Category.values()) {
            categories.add(c.toNiceString());
        }
        category.setValues(categories.toArray(new String[categories.size()]));

        List<String> versions = new ArrayList<>();
        versions.add(I18n.format("replaymod.gui.center.search.version"));
        for (MinecraftVersion v : MinecraftVersion.values()) {
            versions.add(v.toNiceName());
        }
        version.setValues(versions.toArray(new String[versions.size()]));

        gameType.setI18nLabel("replaymod.gui.center.search.gametype")
                .setValues(I18n.format("options.particles.all"),
                        I18n.format("menu.singleplayer"),
                        I18n.format("menu.multiplayer"));
        sort.setI18nLabel("replaymod.gui.center.search.order")
                .setValues(I18n.format("replaymod.gui.center.search.order.best"),
                        I18n.format("replaymod.gui.center.search.order.recent"));

        popup.setLayout(new GridLayout().setColumns(3).setSpacingX(5).setSpacingY(5));
        popup.addElements(null,
                title, new GuiLabel(), new GuiLabel(),
                name, category, gameType,
                server, version, sort,
                new GuiLabel(), new GuiLabel(), new GuiLabel(),
                new GuiLabel(), cancelButton, searchButton);

        setBackgroundColor(Colors.DARK_TRANSPARENT);
    }

    public GuiReplayCenterSearch(GuiReplayCenter replayCenter, ApiClient apiClient) {
        super(replayCenter);
        this.replayCenter = replayCenter;
        this.apiClient = apiClient;
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    protected GuiReplayCenterSearch getThis() {
        return this;
    }
}
