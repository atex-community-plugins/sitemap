package com.atex.plugins.sitemap;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.atex.onecms.content.ContentManager;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.search.solr.SearchClient;

/**
 * SitemapGenerator.
 *
 * @author mnova
 */
public class SitemapGenerator {

    private static final Logger LOGGER = Logger.getLogger(SitemapGenerator.class.getName());

    private static Lock lock = new ReentrantLock();

    private final SitemapUtil sitemapUtil;

    public SitemapGenerator(final PolicyCMServer cmServer, final ContentManager contentManager, final SearchClient searchClient) {
        this.sitemapUtil = new SitemapUtil(cmServer, contentManager, searchClient);
    }

    /**
     * Generate normal sitemap for all sites and gather (all normal + department sitemaps) in an index sitemap
     * Generate a new sitemap
     * Generate a video sitemap
     *
     * @return true if the sitemap was updated.
     * @throws CMException if something goes wrong.
     */
    public boolean generateCurrentMonthSitemap()
            throws CMException {

        try {
            if (lock.tryLock(1, TimeUnit.NANOSECONDS)) {

                try {
                    try {
                        //Generate normal sitemap for all sites & gather (all normal + department sitemaps) in an index sitemap
                        sitemapUtil.generateCurrentMonthNormalSitemap();
                    } catch (CMException e) {
                        LOGGER.log(Level.SEVERE, "Could not generate normal sitemap for all.", e);
                        throw e;
                    }

                    return true;
                } finally {
                    lock.unlock();
                }
            }

            return false;
        } catch (InterruptedException e) {
            throw new CMException(e);
        }
    }

    /*
     * Generate normal sitemaps of all sites for a particular month
     *
     * @param date the date to be generate
     * @return true if the sitemap was generated
     * @throws CMException if something goes wrong.
     */
    public boolean generateOtherMonthSitemap(final Date date)
            throws CMException {

        try {
            if (lock.tryLock(1, TimeUnit.NANOSECONDS)) {

                try {
                    sitemapUtil.generateOtherMonthNormalSitemap(date);
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        } catch (InterruptedException e) {
            throw new CMException(e);
        }
    }

}
