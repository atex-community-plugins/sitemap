package com.atex.plugins.sitemap;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.RepositoryClient;
import com.google.common.base.Strings;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.application.servlet.ApplicationServletUtil;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.CmClientBase;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.search.solr.SearchClient;
import com.polopoly.search.solr.SolrSearchClient;
import com.polopoly.user.server.Caller;
import com.polopoly.user.server.User;
import com.polopoly.user.server.UserServer;

/**
 * SitemapGeneratorServlet.
 *
 * @author mnova
 */
public class SitemapGeneratorServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SitemapGeneratorServlet.class.getName());

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

    private static final long serialVersionUID = 1L;
    private static String REQ_PARAM_DATE = "date";

    private ServletContext servletContext;
    private CmClient cmClient;
    private ContentManager contentManager;
    private SearchClient searchClient;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servletContext = config.getServletContext();

        final Application application = ApplicationServletUtil.getApplication(servletContext);
        cmClient = (CmClient) application.getApplicationComponent(CmClientBase.DEFAULT_COMPOUND_NAME);
        searchClient = (SearchClient) application.getApplicationComponent(SolrSearchClient.DEFAULT_COMPOUND_NAME);

        try {
            final RepositoryClient repositoryClient = application.getPreferredApplicationComponent(RepositoryClient.class);
            contentManager = repositoryClient.getContentManager();
        } catch (IllegalApplicationStateException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                          IOException {
        Caller caller = null;

        try {
            caller = login();

            final String dateParam = Strings.nullToEmpty(request.getParameter(REQ_PARAM_DATE)).trim();
            final SitemapGenerator sitemapGenerator = new SitemapGenerator(getCMServer(), contentManager, searchClient);

            if (!Strings.isNullOrEmpty(dateParam)) {
                try {
                    final Date date = SIMPLEDATE_THLOCAL.get().parse(dateParam);
                    sitemapGenerator.generateOtherMonthSitemap(date);
                } catch (ParseException e) {
                    LOGGER.log(Level.WARNING, e.getMessage());
                }
            } else {
                sitemapGenerator.generateCurrentMonthSitemap();
            }

            final PrintWriter pw = response.getWriter();
            pw.print("OK");
        } catch (CMException e) {
            LOGGER.log(Level.SEVERE, "Could not generate normal sitmap for all.", e);
            throw new IOException(e);
        } finally {
            logout(caller);
        }
    }

    private Caller login() throws CMException {

        try {
            final UserServer userServer = cmClient.getUserServer();

            final SitemapConfigPolicy config = getConfig();

            final String userName = config.getUser();

            LOGGER.log(Level.INFO, "Sitemap user: " + userName);

            final Caller caller = userServer.loginAndMerge(userName, config.getPassword(), null);
            getCMServer().setCurrentCaller(caller);

            return caller;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new CMException(e);
        }
    }

    private void logout(final Caller caller) {
        if (caller != null) {
            LOGGER.log(Level.INFO, "Logging out user.");
            try {
                final UserServer userServer = cmClient.getUserServer();
                final User user = userServer.getUserByUserId(caller.getUserId());
                if (user != null) {
                    user.logout(caller);
                }
                getCMServer().setCurrentCaller(null);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    private SitemapConfigPolicy getConfig() throws CMException {
        return (SitemapConfigPolicy) getCMServer().getPolicy(new ExternalContentId(SitemapConfigPolicy.CONFIG_EXT_ID));
    }

    private PolicyCMServer getCMServer() {
        return cmClient.getPolicyCMServer();
    }

}
