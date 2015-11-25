package com.atex.plugins.sitemap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.polopoly.application.Application;
import com.polopoly.application.servlet.ApplicationServletUtil;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.CmClientBase;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.search.solr.SearchClient;
import com.polopoly.search.solr.SolrSearchClient;

/**
 * SitemapReadServlet
 *
 * @author mnova
 */
public class SitemapReadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(SitemapReadServlet.class.getName());

    private CmClient cmClient;
    private SearchClient searchClient;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        final ServletContext servletContext = config.getServletContext();

        final Application application = ApplicationServletUtil.getApplication(servletContext);
        cmClient = (CmClient) application.getApplicationComponent(CmClientBase.DEFAULT_COMPOUND_NAME);
        searchClient = (SearchClient) application.getApplicationComponent(SolrSearchClient.DEFAULT_COMPOUND_NAME);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                          IOException {

        final PolicyCMServer cmServer = cmClient.getPolicyCMServer();
        final SitemapUtil sitemapUtil = new SitemapUtil(cmServer, searchClient);

        ServletOutputStream stream = null;
        BufferedInputStream buf = null;

        try {
            stream = response.getOutputStream();
            response.setContentType("text/xml;charset=UTF-8");
            buf = new BufferedInputStream(sitemapUtil.getURLofSitemap(request));
            int readBytes;

            while ((readBytes = buf.read()) != -1) {
                stream.write(readBytes);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not get sitemap - " + request.getRequestURL(), e);
            throw new ServletException(e);

        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(buf);
        }
    }
}
