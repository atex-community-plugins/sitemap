package com.atex.plugins.sitemap;

import java.util.List;
import java.util.logging.Level;

import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.ContentResultBuilder;
import com.atex.onecms.content.ContentWrite;
import com.atex.onecms.content.LegacyContentAdapter;
import com.atex.plugins.baseline.policy.BaselinePolicy;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.polopoly.cm.app.policy.SingleValued;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policymvc.PolicyModelDomain;

/**
 * Policy for the plugin configuration.
 *
 * @author mnova
 */
public class SitemapConfigPolicy extends BaselinePolicy implements LegacyContentAdapter<SitemapConfigBean> {

    public static final String CONFIG_EXT_ID = "plugins.com.atex.plugins.sitemap.Config";

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String SITES = "sites";
    private static final String PRIORITY_SITE = "priority_site";
    private static final String PRIORITY_PAGE = "priority_page";
    private static final String PRIORITY_CONTENT = "priority_content";
    private static final String CHANGEFREQ_SITE = "changefreq_site";
    private static final String CHANGEFREQ_PAGE = "changefreq_page";
    private static final String CHANGEFREQ_CONTENT = "changefreq_content";
    private static final String ARTICLE_IT = "article_it";
    private static final String GALLERY_IT = "gallery_it";
    private static final String VIDEO_IT = "video_it";
    private static final String ARTICLE_LANGUAGE = "article_language";
    private static final String ARTICLE_GENRE = "article_genre";

    public String getUser() {
        return Strings.nullToEmpty(getChildValue(USER, ""));
    }

    public String getPassword() {
        return Strings.nullToEmpty(getChildValue(PASSWORD, ""));
    }

    public List<String> getSitesId() {
        return getChildValueSplit(SITES, ",");
    }

    public String getPrioritySite() {
        return Strings.nullToEmpty(getChildValue(PRIORITY_SITE, ""));
    }

    public String getPriorityPage() {
        return Strings.nullToEmpty(getChildValue(PRIORITY_PAGE, ""));
    }

    public String getPriorityContent() {
        return Strings.nullToEmpty(getChildValue(PRIORITY_CONTENT, ""));
    }

    public String getChangeFreqSite() {
        return Strings.nullToEmpty(getChildValue(CHANGEFREQ_SITE, ""));
    }

    public String getChangeFreqPage() {
        return Strings.nullToEmpty(getChildValue(CHANGEFREQ_PAGE, ""));
    }

    public String getChangeFreqContent() {
        return Strings.nullToEmpty(getChildValue(CHANGEFREQ_CONTENT, ""));
    }

    public List<String> getArticleInputTemplates() {
        return getChildValueSplit(ARTICLE_IT, ",");
    }

    public List<String> getGalleryInputTemplates() {
        return getChildValueSplit(GALLERY_IT, ",");
    }

    public List<String> getVideoInputTemplates() {
        return getChildValueSplit(VIDEO_IT, ",");
    }

    private List<String> getChildValueSplit(final String name, final String sep) {
        final String value = Strings.nullToEmpty(getChildValue(name, ""));
        return Lists.newArrayList(Splitter
                .on(sep)
                .omitEmptyStrings()
                .trimResults()
                .split(value)
        );
    }

    public String getArticleLanguage() {
        return getChildValue(ARTICLE_LANGUAGE, null);
    }

    public String getArticleGenre() {
        return getChildValue(ARTICLE_GENRE, null);
    }

    @Override
    public ContentResult<SitemapConfigBean> legacyToNew(final PolicyModelDomain policyModelDomain) throws CMException {

        final SitemapConfigBean bean = new SitemapConfigBean();

        bean.setArticleLanguage(getArticleLanguage());
        bean.setArticleGenre(getArticleGenre());

        return new ContentResultBuilder<SitemapConfigBean>()
                .mainAspectData(bean)
                .build();
    }

    @Override
    public void newToLegacy(final ContentWrite<SitemapConfigBean> contentWrite) throws CMException {
        final SitemapConfigBean bean = contentWrite.getContentData();
        if (bean != null) {
            setChildValue(ARTICLE_LANGUAGE, bean.getArticleLanguage());
            setChildValue(ARTICLE_GENRE, bean.getArticleGenre());
        }
    }
}
