package com.atex.plugins.sitemap;

import org.jdom.Element;
import org.jdom.Namespace;

import com.polopoly.cm.client.CMException;

/**
 * Sitemapable
 * 30/10/15 on 09:38
 *
 * @author mnova
 */
public interface Sitemapable {

    Element getSitemap(Namespace ns) throws CMException;

}
