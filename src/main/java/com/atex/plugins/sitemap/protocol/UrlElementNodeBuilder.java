package com.atex.plugins.sitemap.protocol;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

import org.jdom.Element;
import org.jdom.Namespace;

import com.google.common.base.Strings;

/**
 * A builder used to build the "url" node for the sitemap protocol (see https://www.sitemaps.org/protocol.html).
 *
 * @author mnova
 */
public class UrlElementNodeBuilder<T extends UrlElementNodeBuilder> extends ElementNodeBuilder<T> {

    protected static final ThreadLocal<SimpleDateFormat> RFCDATE_FMT = new ThreadLocal<SimpleDateFormat>() {
        /**
         * See {@link ThreadLocal#initialValue()}
         * @return a not null value.
         */
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        }

    };

    private String url;
    private Date lastMod;
    private ChangeFrequency changeFrequency;
    private Double priority;

    public UrlElementNodeBuilder() {
        super();
        rootName("url");
        namespaces(Namespaces.SITEMAP);
    }

    public T url(final String url) {
        this.url = url;
        return (T) this;
    }

    public T lastModified(final Date lastMod) {
        this.lastMod = lastMod;
        return (T) this;
    }

    public T changeFrequency(final ChangeFrequency changeFrequency) {
        this.changeFrequency = changeFrequency;
        return (T) this;
    }

    public T priority(final Double priority) {
        if (priority != null) {
            this.priority = Math.min(priority, 1.0);
            if (this.priority < 0) {
                this.priority = 0.0;
            }
        } else {
            this.priority = null;
        }
        return (T) this;
    }

    @Override
    protected ElementBuilder createElementBuilder() {
        if (Strings.isNullOrEmpty(url)) {
            throw new RuntimeException("Url is null, cannot create node");
        }

        return super.createElementBuilder()
                .addContent(createElement("loc", () -> url))
                .addContent(createElement("lastmod", () ->
                        (lastMod != null ? RFCDATE_FMT.get().format(lastMod) : null)))
                .addContent(createElement("changefreq", () ->
                        (this.changeFrequency != null) ? this.changeFrequency.name().toLowerCase() : null))
                .addContent(createElement("priority", () ->
                        (this.priority != null) ? Double.toString(this.priority) : null));
    }

    public Element build() {
        return createElementBuilder().build();
    }

    protected Optional<Element> createElement(final String name, final Supplier<String> supplier) {
        return createElement(Namespaces.SITEMAP.ns(), name, supplier);
    }

    protected Optional<Element> createElement(final Namespace ns, final String name, final Supplier<String> supplier) {
        final String text = supplier.get();
        if (text != null) {
            final Element element = new Element(name, ns);
            element.setText(text);
            return Optional.of(element);
        }
        return Optional.empty();
    }

}
