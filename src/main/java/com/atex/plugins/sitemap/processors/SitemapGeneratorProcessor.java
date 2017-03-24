package com.atex.plugins.sitemap.processors;

import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.atex.plugins.sitemap.SitemapConfigPolicy;
import com.atex.plugins.sitemap.SitemapGenerator;
import com.polopoly.application.ApplicationComponent;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.search.solr.PostFilteredSolrSearchClient;
import com.polopoly.search.solr.SearchClient;

/**
 * SitemapGeneratorProcessor
 *
 * @author mnova
 */
public class SitemapGeneratorProcessor extends BaseProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(SitemapGeneratorProcessor.class.getName());

    private static final ThreadLocal<SimpleDateFormat> SIMPLEDATE_THLOCAL = new ThreadLocal<SimpleDateFormat>() {
        /**
         * See {@link ThreadLocal#initialValue()}
         * @return a not null value.
         */
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }

    };

    private static boolean processing = false;

    @Override
    public void process(final Exchange exchange) throws Exception {

        if (processing) {
            LOGGER.log(Level.WARNING, "already processing");
            return;
        }

        try {
            processing = true;

            LOG.log(Level.INFO, "start processing sitemap generation");

            final SearchClient searchClient = getApplicationComponent(PostFilteredSolrSearchClient.DEFAULT_COMPOUND_NAME);
            if (!isServiceReady((ApplicationComponent) searchClient)) {
                LOGGER.log(Level.WARNING, "search is not ready yet to serve requests");
            } else {
                login();

                try {
                    final SitemapGenerator sitemapGenerator = new SitemapGenerator(getCMServer(), getContentManager(), searchClient);
                    sitemapGenerator.generateCurrentMonthSitemap();
                } finally {
                    if (isLoggedIn()) {
                        logout();
                    }
                }
            }

        } finally {
            processing = false;
        }

    }

    private void login() throws Exception {
        final SitemapConfigPolicy config = getConfig();
        login(config.getUser(), config.getPassword());
    }

    private SitemapConfigPolicy getConfig() throws CMException {
        return (SitemapConfigPolicy) getCMServer().getPolicy(new ExternalContentId(SitemapConfigPolicy.CONFIG_EXT_ID));
    }

}
