package com.atex.plugins.sitemap;

import org.jdom.Element;

import com.polopoly.cm.client.CMException;

/**
 * VideoSitemapable.
 *
 * @author mnova
 */
public interface VideoSitemapable {

    Element getVideoSitemap() throws CMException;

}
