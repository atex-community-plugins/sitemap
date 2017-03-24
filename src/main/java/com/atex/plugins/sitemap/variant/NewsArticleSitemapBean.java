package com.atex.plugins.sitemap.variant;


import java.util.List;

import com.atex.onecms.content.ContentId;

/**
 * NewsArticleSitemapBean variant
 *
 * @author mnova
 */
public class NewsArticleSitemapBean {

    public static final String VARIANT_NAME = "com.atex.plugins.sitemap.newsarticle";

    private ContentId contentId;
    private String name;
    private long publishingDateTime;
    private String keywords;
    private String language;
    private String genres;
    private String stockTickers;
    private List<ContentId> linkPath;
    private ContentId imageContentId;

    public void setContentId(final ContentId contentId) {
        this.contentId = contentId;
    }

    public ContentId getContentId() {
        return contentId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getPublishingDateTime() {
        return publishingDateTime;
    }

    public void setPublishingDateTime(final long publishingDateTime) {
        this.publishingDateTime = publishingDateTime;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(final String keywords) {
        this.keywords = keywords;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(final String genres) {
        this.genres = genres;
    }

    public String getStockTickers() {
        return stockTickers;
    }

    public void setStockTickers(final String stockTickers) {
        this.stockTickers = stockTickers;
    }

    public List<ContentId> getLinkPath() {
        return linkPath;
    }

    public void setLinkPath(final List<ContentId> linkPath) {
        this.linkPath = linkPath;
    }

    public ContentId getImageContentId() {
        return imageContentId;
    }

    public void setImageContentId(final ContentId imageContentId) {
        this.imageContentId = imageContentId;
    }
}
