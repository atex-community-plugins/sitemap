package com.atex.plugins.sitemap.variant;

import java.util.Date;

import com.atex.onecms.content.ContentId;

/**
 * VideoSitemapBean variant
 *
 * @author mnova
 */
public class VideoSitemapBean {

    public static final String VARIANT_NAME = "com.atex.plugins.sitemap.video";

    private ContentId contentId;
    private ContentId imageContentId;
    private String title;
    private String description;
    private String mediaUrl;
    private String mediaPlayerUrl;
    private String duration;
    private Date expirationDate;
    private Double rating;
    private Date publicationDate;
    private Boolean familyFriendly;
    private String tag;
    private String category;
    private String countryRestriction;

    public ContentId getContentId() {
        return contentId;
    }

    public void setContentId(final ContentId contentId) {
        this.contentId = contentId;
    }

    public ContentId getImageContentId() {
        return imageContentId;
    }

    public void setImageContentId(final ContentId imageContentId) {
        this.imageContentId = imageContentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(final String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaPlayerUrl() {
        return mediaPlayerUrl;
    }

    public void setMediaPlayerUrl(final String mediaPlayerUrl) {
        this.mediaPlayerUrl = mediaPlayerUrl;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(final Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(final Double rating) {
        this.rating = rating;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(final Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Boolean getFamilyFriendly() {
        return familyFriendly;
    }

    public void setFamilyFriendly(final Boolean familyFriendly) {
        this.familyFriendly = familyFriendly;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(final String tag) {
        this.tag = tag;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public String getCountryRestriction() {
        return countryRestriction;
    }

    public void setCountryRestriction(final String countryRestriction) {
        this.countryRestriction = countryRestriction;
    }
}
