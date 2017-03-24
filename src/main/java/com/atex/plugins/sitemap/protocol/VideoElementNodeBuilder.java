package com.atex.plugins.sitemap.protocol;

import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

import org.jdom.Element;

/**
 * A builder used to build the "url" node for the video sitemap protocol (see https://developers.google.com/webmasters/videosearch/sitemaps).
 *
 * @author mnova
 */
public class VideoElementNodeBuilder extends UrlElementNodeBuilder<VideoElementNodeBuilder> {

    private String title;
    private String description;
    private String mediaUrl;
    private String mediaPlayerUrl;
    private String thumbnailUrl;
    private String duration;
    private Date expirationDate;
    private Double rating;
    private Date publicationDate;
    private Boolean familyFriendly;
    private String tag;
    private String category;
    private String countryRestriction;

    public VideoElementNodeBuilder title(final String title) {
        this.title = title;
        return this;
    }

    public VideoElementNodeBuilder description(final String description) {
        this.description = description;
        return this;
    }

    public VideoElementNodeBuilder mediaUrl(final String mediaUrl) {
        this.mediaUrl = mediaUrl;
        return this;
    }

    public VideoElementNodeBuilder mediaPlayerUrl(final String mediaPlayerUrl) {
        this.mediaPlayerUrl = mediaPlayerUrl;
        return this;
    }

    public VideoElementNodeBuilder thumbnailUrl(final String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }

    public VideoElementNodeBuilder duration(final String duration) {
        this.duration = duration;
        return this;
    }

    public VideoElementNodeBuilder expirationDate(final Date expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public VideoElementNodeBuilder rating(final Double rating) {
        this.rating = rating;
        if (this.rating != null) {
            this.rating = Math.min(this.rating, 5.0);
            if (this.rating < 0) {
                this.rating = 0.0;
            }
        }
        return this;
    }

    public VideoElementNodeBuilder publicationDate(final Date publicationDate) {
        this.publicationDate = publicationDate;
        return this;
    }

    public VideoElementNodeBuilder familyFriendly(final Boolean familyFriendly) {
        this.familyFriendly = familyFriendly;
        return this;
    }

    public VideoElementNodeBuilder tag(final String tag) {
        this.tag = tag;
        return this;
    }

    public VideoElementNodeBuilder category(final String category) {
        this.category = category;
        return this;
    }

    public VideoElementNodeBuilder countryRestriction(final String countryRestriction) {
        this.countryRestriction = countryRestriction;
        return this;
    }

    @Override
    protected ElementBuilder createElementBuilder() {
        return super.createElementBuilder()
                .addContent(Optional.of(
                        new ElementBuilder(Namespaces.VIDEO.ns(), "video")
                                .addContent(createVideoElement("thumbnail_loc", () -> thumbnailUrl))
                                .addContent(createVideoElement("title", () -> title))
                                .addContent(createVideoElement("description", () -> description))
                                .addContent(createVideoElement("content_loc", () -> mediaUrl))
                                .addContent(createVideoElement("player_loc", () -> mediaPlayerUrl))
                                .addContent(createVideoElement("duration", () -> duration))
                                .addContent(createVideoElement("publication_date", () ->
                                        (publicationDate != null ? RFCDATE_FMT.get().format(publicationDate) : null)))
                                .addContent(createVideoElement("expiration_date", () ->
                                        (expirationDate != null ? RFCDATE_FMT.get().format(expirationDate) : null)))
                                .addContent(createVideoElement("rating", () ->
                                        (this.rating != null) ? Double.toString(this.rating) : null))
                                .addContent(createVideoElement("family_friendly", () ->
                                        (this.familyFriendly != null) ? (this.familyFriendly ? "yes" : "no") : null))
                                .addContent(createVideoElement("restriction", () -> countryRestriction))
                                .addContent(createVideoElement("tag", () -> tag))
                                .addContent(createVideoElement("category", () -> category))
                                .build()));
    }

    protected Optional<Element> createVideoElement(final String name, final Supplier<String> supplier) {
        return createElement(Namespaces.VIDEO.ns(), name, supplier);
    }

}
