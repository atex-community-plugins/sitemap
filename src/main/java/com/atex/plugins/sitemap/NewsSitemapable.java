package com.atex.plugins.sitemap;

import org.jdom.Element;

import com.polopoly.cm.client.CMException;

/**
 * NewsSitemapable.
 *
 * @author mnova
 */
public interface NewsSitemapable {

    Element getNewsSitemap() throws CMException;

}
