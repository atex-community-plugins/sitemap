package com.atex.plugins.sitemap.protocol;

import org.jdom.Namespace;

/**
 * Namespaces
 *
 * @author mnova
 */
public enum Namespaces {

    SITEMAP(Namespace.getNamespace("http://www.sitemaps.org/schemas/sitemap/0.9")),

    NEWS(Namespace.getNamespace("news","http://www.google.com/schemas/sitemap-news/0.9")),

    VIDEO(Namespace.getNamespace("video", "http://www.google.com/schemas/sitemap-video/1.1")),

    IMAGE(Namespace.getNamespace("image", "http://www.google.com/schemas/sitemap-image/1.1"));

    private final Namespace ns;

    Namespaces(final Namespace ns) {
        this.ns = ns;
    }

    public Namespace ns() {
        return ns;
    }

}
