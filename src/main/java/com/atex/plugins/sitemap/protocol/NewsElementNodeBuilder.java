package com.atex.plugins.sitemap.protocol;

import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

import org.jdom.Element;

/**
 * A builder used to build the "url" node for the sitemap protocol (see https://www.sitemaps.org/protocol.html).
 *
 * @author mnova
 */
public class NewsElementNodeBuilder extends UrlElementNodeBuilder<NewsElementNodeBuilder> {

    private String publicationName;
    private String publicationLang;
    private String genres;
    private Date publicationDate;
    private String title;
    private String keywords;
    private String stockTickers;

    public NewsElementNodeBuilder publicationName(final String publicationName) {
        this.publicationName = publicationName;
        return this;
    }

    public NewsElementNodeBuilder publicationLang(final String publicationLang) {
        this.publicationLang = publicationLang;
        return this;
    }

    public NewsElementNodeBuilder genres(final String genres) {
        this.genres = genres;
        return this;
    }

    public NewsElementNodeBuilder publicationDate(final Date publicationDate) {
        this.publicationDate = publicationDate;
        return this;
    }

    public NewsElementNodeBuilder title(final String title) {
        this.title = title;
        return this;
    }

    public NewsElementNodeBuilder keywords(final String keywords) {
        this.keywords = keywords;
        return this;
    }

    public NewsElementNodeBuilder stockTickers(final String stockTickers) {
        this.stockTickers = stockTickers;
        return this;
    }

    @Override
    protected ElementBuilder createElementBuilder() {
        return super.createElementBuilder()
                .addContent(Optional.of(
                        new ElementBuilder(Namespaces.NEWS.ns(), "news")
                                .addContent(Optional.of(
                                        new ElementBuilder(Namespaces.NEWS.ns(), "publication")
                                                .addContent(createNewsElement("name", () -> publicationName))
                                                .addContent(createNewsElement("language", () -> publicationLang))
                                                .build()))
                                .addContent(createNewsElement("genres", () -> genres))
                                .addContent(createNewsElement("publication_date", () ->
                                        (publicationDate != null ? RFCDATE_FMT.get().format(publicationDate) : null)))
                                .addContent(createNewsElement("title", () -> title))
                                .addContent(createNewsElement("keywords", () -> keywords))
                                .addContent(createNewsElement("stock_tickers", () -> stockTickers))
                                .build()));
    }

    protected Optional<Element> createNewsElement(final String name, final Supplier<String> supplier) {
        return createElement(Namespaces.NEWS.ns(), name, supplier);
    }

}
