package com.atex.plugins.sitemap.protocol;

/**
 * A builder used to build the "urlset" node for the sitemap protocol (see https://www.sitemaps.org/protocol.html).
 *
 * @author mnova
 */
public class UrlSetElementNodeBuilder extends ElementNodeBuilder<UrlSetElementNodeBuilder> {

    public UrlSetElementNodeBuilder() {
        rootName("urlset");
        namespaces(Namespaces.SITEMAP);
    }

}
