package com.atex.plugins.sitemap.processors;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.RepositoryClient;
import com.polopoly.application.Application;
import com.polopoly.application.ApplicationComponent;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.CmClientBase;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.integration.IntegrationServerApplication;
import com.polopoly.management.ServiceControl;
import com.polopoly.user.server.Caller;
import com.polopoly.user.server.User;
import com.polopoly.user.server.UserServer;

/**
 * Base processor with useful methods.
 *
 * @author mnova
 */
public abstract class BaseProcessor {

    protected final Logger LOG = Logger.getLogger(getClass().getName());

    private Application application = null;
    private Caller oldCaller = null;
    private Caller loggedIn = null;

    protected boolean login(final String userName, final String userPwd) throws Exception {
        if (loggedIn == null) {
            final PolicyCMServer cmServer = getCMServer();
            oldCaller = cmServer.getCurrentCaller();
            final UserServer userServer = getCmClient().getUserServer();
            loggedIn = userServer.loginAndMerge(userName, userPwd, oldCaller);
            cmServer.setCurrentCaller(loggedIn);
            return true;
        }
        return false;
    }

    protected boolean isServiceReady(final ApplicationComponent component) {
        return isServiceReady(component.getServiceControl());
    }

    protected boolean isServiceReady(final ServiceControl service) {
        return service.isConnected() && service.isServing();
    }

    protected CmClient getCmClient() {
        return getApplicationComponent(CmClientBase.DEFAULT_COMPOUND_NAME);
    }

    protected PolicyCMServer getCMServer() {
        return getCmClient().getPolicyCMServer();
    }

    protected ContentManager getContentManager() {
        try {
            final RepositoryClient repositoryClient = getApplication().getPreferredApplicationComponent(RepositoryClient.class);
            return repositoryClient.getContentManager();
        } catch (IllegalApplicationStateException e) {
            throw new RuntimeException(e);
        }
    }

    protected Application getApplication() {
        if (application == null) {
            application = IntegrationServerApplication.getPolopolyApplication();
        }
        return application;
    }

    protected <T> T getApplicationComponent(final String name) {
        final Application application = getApplication();
        return (T) application.getApplicationComponent(name);
    }

    protected void logout() {
        if (loggedIn != null) {
            try {
                final CmClient cmClient = getCmClient();
                final PolicyCMServer cmServer = cmClient.getPolicyCMServer();
                final UserServer userServer = cmClient.getUserServer();
                final User user = userServer.getUserByUserId(loggedIn.getUserId());
                user.logout(loggedIn);
                cmServer.setCurrentCaller(oldCaller);
                loggedIn = null;
                oldCaller = null;
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "cannot logout " + loggedIn.getLoginName() + ": " + e.getMessage(), e);
            }
        }
    }

    protected boolean isLoggedIn() {
        return (loggedIn != null);
    }

}
