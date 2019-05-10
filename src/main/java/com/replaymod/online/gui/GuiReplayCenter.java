package com.replaymod.online.gui;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.replaymod.core.utils.Utils;
import com.replaymod.online.ReplayModOnline;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.ApiException;
import com.replaymod.online.api.replay.SearchQuery;
import com.replaymod.online.api.replay.holders.Category;
import com.replaymod.online.api.replay.holders.FileInfo;
import com.replaymod.online.api.replay.holders.FileRating;
import com.replaymod.online.api.replay.holders.Rating;
import com.replaymod.online.api.replay.pagination.DownloadedFilePagination;
import com.replaymod.online.api.replay.pagination.FavoritedFilePagination;
import com.replaymod.online.api.replay.pagination.Pagination;
import com.replaymod.online.api.replay.pagination.SearchPagination;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.studio.ReplayStudio;
import de.johni0702.minecraft.gui.container.AbstractGuiContainer;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiImage;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.IGuiButton;
import de.johni0702.minecraft.gui.element.advanced.GuiResourceLoadingList;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.text.TextFormat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.replaymod.online.ReplayModOnline.LOGGER;

public class GuiReplayCenter extends GuiScreen {
    private final ReplayModOnline mod;
    private final ApiClient apiClient;

    private GuiButton makeCategory(String name) {
        return new GuiButton().setI18nLabel("replaymod.gui.center.top." + name).setSize(75, 20);
    }

    public final GuiButton categoryRecent = makeCategory("recent").onClick(new Runnable() {
        @Override
        public void run() {
            show(new SearchPagination(apiClient,
                    new SearchQuery(false, null, null, null, null, null, null, null, null, null)));
            categoryRecent.setDisabled();
        }
    });

    public final GuiButton categoryBest = makeCategory("best").onClick(new Runnable() {
        @Override
        public void run() {
            show(new SearchPagination(apiClient,
                    new SearchQuery(true, null, null, null, null, null, null, null, null, null)));
            categoryBest.setDisabled();
        }
    });

    public final GuiButton categoryDownloaded = makeCategory("downloaded").onClick(new Runnable()

    {
        @Override
        public void run() {
            show(new DownloadedFilePagination(mod));
            categoryDownloaded.setDisabled();
        }
    });

    public final GuiButton categoryFavorited = makeCategory("favorited").onClick(new Runnable() {
        @Override
        public void run() {
            show(new FavoritedFilePagination(apiClient));
            categoryFavorited.setDisabled();
        }
    });

    public final GuiButton categorySearch = makeCategory("search").onClick(new Runnable() {
        @Override
        public void run() {
            searchPopup.open();
        }
    });

    public final GuiPanel categories = new GuiPanel(this).setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, categoryRecent, categoryBest, categoryDownloaded, categoryFavorited, categorySearch);

    public final GuiResourceLoadingList<GuiReplayEntry> list = new GuiResourceLoadingList<GuiReplayEntry>(this).onSelectionChanged(new Runnable() {
        @Override
        public void run() {
            GuiReplayEntry selected = list.getSelected();
            replayButtonPanel.forEach(IGuiButton.class).setEnabled(selected != null);
            replayButtonPanel.forEach(IGuiButton.class).setTooltip(null);
            if (selected != null) {
                int replayId = selected.fileInfo.getId();
                boolean favorited = favoritedReplays.contains(replayId);
                boolean liked = likedReplays.contains(replayId);
                boolean disliked = dislikedReplays.contains(replayId);

                loadButton.setI18nLabel(selected.downloaded ? "replaymod.gui.load" : "replaymod.gui.download");
                if (selected.incompatible) {
                    loadButton.setDisabled();
                }

                favoriteButton.setI18nLabel("replaymod.gui.center." + (favorited ? "unfavorite" : "favorite"));
                // Only allow button usage for either unfavorite or favorite after they've actually downloaded it
                favoriteButton.setEnabled(favorited || selected.downloaded);
                if (favoriteButton.isEnabled()) {
                    favoriteButton.setTooltip(null);
                } else {
                    favoriteButton.setTooltip(new GuiTooltip().setI18nText("replaymod.gui.center.downloadrequired"));
                }

                // Similar for like/dislike buttons
                likeButton.setEnabled(selected.downloaded);
                dislikeButton.setEnabled(selected.downloaded);
                if (likeButton.isEnabled()) {
                    likeButton.setTooltip(null);
                    dislikeButton.setTooltip(null);
                } else {
                    likeButton.setTooltip(new GuiTooltip().setI18nText("replaymod.gui.center.downloadrequired"));
                    dislikeButton.setTooltip(new GuiTooltip().setI18nText("replaymod.gui.center.downloadrequired"));
                }
                likeButton.setI18nLabel("replaymod.gui." + (liked ? "removelike" : "like"));
                dislikeButton.setI18nLabel("replaymod.gui." + (disliked ? "removedislike" : "dislike"));
            }
        }
    }).onLoad(new Consumer<Consumer<Supplier<GuiReplayEntry>>>() {
        @Override
        public void consume(Consumer<Supplier<GuiReplayEntry>> obj) {
            // Do not load any replays until the user has selected the category they'd like to view
        }
    }).setDrawShadow(true).setDrawSlider(true);

    public final GuiButton loadButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            try {
                FileInfo fileInfo = list.getSelected().fileInfo;
                mod.startReplay(fileInfo.getId(), fileInfo.getName(), GuiReplayCenter.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }).setSize(95, 20).setI18nLabel("replaymod.gui.download").setDisabled();

    public final GuiButton favoriteButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            try {
                int replayId = list.getSelected().fileInfo.getId();
                boolean favorited = favoritedReplays.contains(replayId);
                apiClient.favFile(replayId, !favorited);
                reloadFavorited();
                list.onSelectionChanged();
            } catch (IOException | ApiException e) {
                e.printStackTrace();
            }
        }
    }).setSize(95, 20).setI18nLabel("replaymod.gui.center.favorite").setDisabled();

    public final GuiButton likeButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            try {
                int replayId = list.getSelected().fileInfo.getId();
                boolean liked = likedReplays.contains(replayId);
                apiClient.rateFile(replayId, liked ? Rating.RatingType.NEUTRAL : Rating.RatingType.LIKE);
                reloadRated();
                list.onSelectionChanged();
            } catch (IOException | ApiException e) {
                e.printStackTrace();
            }
        }
    }).setSize(95, 20).setI18nLabel("replaymod.gui.like").setDisabled();

    public final GuiButton dislikeButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            try {
                int replayId = list.getSelected().fileInfo.getId();
                boolean disliked = dislikedReplays.contains(replayId);
                apiClient.rateFile(replayId, disliked ? Rating.RatingType.NEUTRAL : Rating.RatingType.DISLIKE);
                reloadRated();
                list.onSelectionChanged();
            } catch (IOException | ApiException e) {
                e.printStackTrace();
            }
        }
    }).setSize(95, 20).setI18nLabel("replaymod.gui.dislike").setDisabled();

    public final GuiButton logoutButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            Futures.addCallback(GuiYesNoPopup.open(GuiReplayCenter.this,
                    new GuiLabel().setI18nText("replaymod.gui.center.logoutcallback").setColor(Colors.BLACK))
                    .setYesI18nLabel("replaymod.gui.logout").setNoI18nLabel("replaymod.gui.cancel")
                    .getFuture(), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        apiClient.logout();
                        getMinecraft().openScreen(null);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }).setSize(195, 20).setI18nLabel("replaymod.gui.logout");

    public final GuiButton mainMenuButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            getMinecraft().openScreen(null);
        }
    }).setSize(195, 20).setI18nLabel("replaymod.gui.mainmenu");

    public final GuiReplayCenterSearch searchPopup;

    public final GuiPanel replayButtonPanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, loadButton, favoriteButton, likeButton, dislikeButton);
    public final GuiPanel generalButtonPanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, logoutButton, mainMenuButton);
    public final GuiPanel buttonPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(5))
            .addElements(null, replayButtonPanel, generalButtonPanel);

    private volatile Set<Integer> favoritedReplays = Collections.emptySet();
    private volatile Set<Integer> likedReplays = Collections.emptySet();
    private volatile Set<Integer> dislikedReplays = Collections.emptySet();

    public GuiReplayCenter(ReplayModOnline mod) {
        this.mod = mod;
        this.apiClient = mod.getApiClient();
        this.searchPopup = new GuiReplayCenterSearch(this, apiClient);

        setTitle(new GuiLabel().setI18nText("replaymod.gui.replaycenter"));

        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, height - 10 - height(buttonPanel));

                pos(list, 0, 50);
                size(list, width, y(buttonPanel) - 10 - y(list));

                pos(categories, width / 2 - width(categories) / 2, y(list) - 7 - height(categories));
            }
        });
    }

    private void reloadFavorited() {
        try {
            favoritedReplays = new HashSet<>(Ints.asList(apiClient.getFavorites()));
        } catch (IOException | ApiException e) {
            e.printStackTrace();
        }
    }

    private void reloadRated() {
        try {
            Set<Integer> liked = new HashSet<>(), disliked = new HashSet<>();
            for (FileRating rating : apiClient.getRatedFiles()) {
                (rating.isRatingPositive() ? liked : disliked).add(rating.getFile());
            }
            this.likedReplays = liked;
            this.dislikedReplays = disliked;
        } catch (IOException | ApiException e) {
            e.printStackTrace();
        }
    }

    public void show(final Pagination pagination) {
        list.setOffsetY(0);
        list.onLoad(new Consumer<Consumer<Supplier<GuiReplayEntry>>>() {
            @Override
            public void consume(Consumer<Supplier<GuiReplayEntry>> obj) {
                if (pagination.getLoadedPages() < 0) {
                    pagination.fetchPage();
                }

                reloadFavorited();
                reloadRated();

                int i = 0;
                for (final FileInfo fileInfo : pagination.getFiles()) {
                    if (Thread.interrupted()) return;
                    try {
                        // Make sure that to int[] conversion doesn't have to occur in main thread
                        final BufferedImage theThumb;
                        if (fileInfo.hasThumbnail()) {
                            BufferedImage buf = apiClient.downloadThumbnail(fileInfo.getId());
                            // This is the same way minecraft calls this method, we cache the result and hand
                            // minecraft a BufferedImage with way simpler logic using the precomputed values
                            final int[] theIntArray = buf.getRGB(0, 0, buf.getWidth(), buf.getHeight(), null, 0, buf.getWidth());
                            theThumb = new BufferedImage(buf.getWidth(), buf.getHeight(), BufferedImage.TYPE_INT_ARGB) {
                                @Override
                                public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
                                    System.arraycopy(theIntArray, 0, rgbArray, 0, theIntArray.length);
                                    return null; // Minecraft doesn't use the return value
                                }
                            };
                        } else {
                            theThumb = null;
                        }

                        final int sortId = i++;
                        final boolean downloaded = mod.hasDownloaded(fileInfo.getId());
                        obj.consume(new Supplier<GuiReplayEntry>() {
                            @Override
                            public GuiReplayEntry get() {
                                return new GuiReplayEntry(fileInfo, theThumb, sortId, downloaded);
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.error("Could not load Replay File {}", fileInfo.getId(), e);
                    }
                }
            }
        }).load();

        categories.forEach(IGuiButton.class).setEnabled();
    }

    @Override
    protected GuiReplayCenter getThis() {
        return this;
    }

    private final GuiImage defaultThumbnail = new GuiImage().setTexture(Utils.DEFAULT_THUMBNAIL);
    public class GuiReplayEntry extends AbstractGuiContainer<GuiReplayEntry> implements Comparable<GuiReplayEntry> {
        public final FileInfo fileInfo;
        public final GuiLabel name = new GuiLabel();
        public final GuiLabel author = new GuiLabel();
        public final GuiLabel date = new GuiLabel().setColor(Colors.LIGHT_GRAY);
        public final GuiLabel server = new GuiLabel().setColor(Colors.LIGHT_GRAY);
        public final GuiLabel version = new GuiLabel().setColor(Colors.RED);
        public final GuiLabel category = new GuiLabel().setColor(Colors.GREY);
        public final GuiPanel stats = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(3));
        public final GuiLabel favorites = new GuiLabel(stats).setColor(Colors.ORANGE);
        public final GuiLabel likes = new GuiLabel(stats).setColor(Colors.GREEN);
        public final GuiLabel dislikes = new GuiLabel(stats).setColor(Colors.RED);
        public final GuiPanel infoPanel = new GuiPanel(this).setLayout(new CustomLayout<GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                pos(name, 0, 0);
                pos(author, 0, height(name) + 2);
                pos(server, 0, y(author) + height(author) + 3);
                pos(category, 0, y(server) + height(server) + 3);

                pos(date, width - width(date), y(author));
                pos(version, width - width(version), y(server));
                pos(stats, width - width(stats), y(category));
            }
        }).addElements(null, name, author, date, server, version, category, stats);
        public final GuiImage thumbnail;
        public final GuiLabel duration = new GuiLabel();
        public final GuiPanel durationPanel = new GuiPanel().setBackgroundColor(Colors.HALF_TRANSPARENT)
                .addElements(null, duration).setLayout(new CustomLayout<GuiPanel>() {
                    @Override
                    protected void layout(GuiPanel container, int width, int height) {
                        pos(duration, 2, 2);
                    }

                    @Override
                    public ReadableDimension calcMinSize(GuiContainer<?> container) {
                        ReadableDimension dimension = duration.calcMinSize();
                        return new Dimension(dimension.getWidth() + 2, dimension.getHeight() + 2);
                    }
                });
        public final GuiLabel downloads = new GuiLabel();
        public final GuiPanel downloadsPanel = new GuiPanel().setBackgroundColor(Colors.HALF_TRANSPARENT)
                .addElements(null, downloads).setLayout(new CustomLayout<GuiPanel>() {
                    @Override
                    protected void layout(GuiPanel container, int width, int height) {
                        pos(downloads, 2, 2);
                    }

                    @Override
                    public ReadableDimension calcMinSize(GuiContainer<?> container) {
                        ReadableDimension dimension = downloads.calcMinSize();
                        return new Dimension(dimension.getWidth() + 2, dimension.getHeight() + 2);
                    }
                });

        private final long dateMillis;
        private final int sortId;
        private final boolean downloaded;
        private final boolean incompatible;

        public GuiReplayEntry(FileInfo fileInfo, BufferedImage thumbImage, int sortId, boolean downloaded) {
            this.fileInfo = fileInfo;
            this.sortId = sortId;
            this.downloaded = downloaded;
            ReplayMetaData metaData = fileInfo.getMetadata();

            name.setText(TextFormat.UNDERLINE + Utils.fileNameToReplayName(fileInfo.getName()));
            author.setI18nText("replaymod.gui.center.author",
                    "" + TextFormat.GRAY + TextFormat.ITALIC, fileInfo.getOwner());
            if (StringUtils.isEmpty(metaData.getServerName())) {
                server.setI18nText("replaymod.gui.iphidden").setColor(Colors.DARK_RED);
            } else {
                server.setText(metaData.getServerName());
            }
            incompatible = !new ReplayStudio().isCompatible(fileInfo.getMetadata().getFileFormatVersion());
            if (incompatible) {
                version.setText("Minecraft " + fileInfo.getMetadata().getMcVersion());
            }
            dateMillis = metaData.getDate();
            date.setText(new SimpleDateFormat().format(new Date(dateMillis)));
            if (thumbImage == null) {
                thumbnail = new GuiImage(defaultThumbnail);
                addElements(null, thumbnail);
            } else {
                thumbnail = new GuiImage(this).setTexture(thumbImage);
            }
            thumbnail.setSize(45 * 16 / 9, 45);
            duration.setText(Utils.convertSecondsToShortString(metaData.getDuration() / 1000));
            downloads.setText(fileInfo.getDownloads() + " ⬇");
            favorites.setText("⭑" + fileInfo.getFavorites());
            likes.setText("⬆" + fileInfo.getRatings().getPositive());
            dislikes.setText("⬇" + fileInfo.getRatings().getNegative());
            category.setText(TextFormat.ITALIC + Optional.fromNullable(Category.fromId(fileInfo.getCategory()))
                    .or(Category.MISCELLANEOUS).toNiceString());
            addElements(null, durationPanel, downloadsPanel);

            setLayout(new CustomLayout<GuiReplayEntry>() {
                @Override
                protected void layout(GuiReplayEntry container, int width, int height) {
                    pos(thumbnail, 0, 0);
                    x(durationPanel, width(thumbnail) - width(durationPanel));
                    y(durationPanel, height(thumbnail) - height(durationPanel));
                    x(downloadsPanel, width(thumbnail) - width(downloadsPanel));
                    y(downloadsPanel, height(thumbnail) - height(durationPanel) - height(downloadsPanel));

                    pos(infoPanel, width(thumbnail) + 5, 0);
                    size(infoPanel, width - width(thumbnail) - 5, height(thumbnail));
                }

                @Override
                public ReadableDimension calcMinSize(GuiContainer<?> container) {
                    return new Dimension(300, thumbnail.getMinSize().getHeight());
                }
            });
        }

        @Override
        protected GuiReplayEntry getThis() {
            return this;
        }

        @Override
        public int compareTo(GuiReplayEntry o) {
            return Integer.compare(sortId, o.sortId);
        }
    }
}
