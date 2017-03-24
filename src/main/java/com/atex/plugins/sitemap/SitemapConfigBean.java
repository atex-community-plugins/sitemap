package com.atex.plugins.sitemap;

/**
 * SiteConfigBean
 *
 * @author mnova
 */
public class SitemapConfigBean {

    private String articleLanguage;
    private String articleGenre;

    public String getArticleLanguage() {
        return articleLanguage;
    }

    public void setArticleLanguage(final String articleLanguage) {
        this.articleLanguage = articleLanguage;
    }

    public String getArticleGenre() {
        return articleGenre;
    }

    public void setArticleGenre(final String articleGenre) {
        this.articleGenre = articleGenre;
    }
}
