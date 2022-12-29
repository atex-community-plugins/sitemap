package com.atex.plugins.sitemap;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.polopoly.cm.ContentId;
import com.polopoly.cm.ContentReference;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.VersionedContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.ContentRead;
import com.polopoly.cm.collections.ContentList;
import com.polopoly.cm.policy.ContentPolicy;
import com.polopoly.cm.policy.Policy;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.siteengine.structure.Alias;
import com.polopoly.siteengine.structure.Site;

/**
 * PolopolyUtil.
 *
 * @author mnova
 */
public class PolopolyUtil {

    public static String DEFAULT_SITEENGINE_SITES_ID = "p.siteengine.Sites.d";
    public static ExternalContentId DEFAULT_SITEENGINE_SITES_EXTERNAL_ID = new ExternalContentId(DEFAULT_SITEENGINE_SITES_ID);

    public static List<Site> getAllSites(final PolicyCMServer cmServer) throws CMException {
        final List<Site> list = new ArrayList<>();
        ContentRead content = cmServer.getContent(DEFAULT_SITEENGINE_SITES_EXTERNAL_ID);
        ContentList pages = content.getContentList("polopoly.Department");
        ListIterator<ContentReference> iterator = pages.getListIterator();

        while (iterator.hasNext()) {
            ContentId contentId = iterator.next().getReferredContentId();
            Policy pagePolicy = cmServer.getPolicy(contentId);

            if (pagePolicy instanceof Site) {
                list.add((Site) pagePolicy);
            }
        }
        return list;
    }

    public static List<ContentRead> getPageStructure(final ContentId startContentId, final PolicyCMServer cmServer)
            throws CMException {
        List<ContentRead> list = new ArrayList<ContentRead>();
        getPageStructure(startContentId, cmServer, list);
        return list;
    }

    private static void getPageStructure(final ContentId startContentId, final PolicyCMServer cmServer,
                                         final List<ContentRead> list) throws CMException {

        boolean exists = false;

        for (ContentRead cr : list) {
            if (cr.getContentId().getContentId().equals(startContentId)) {
                exists = true;
            }
        }
        if (exists) {
            return;
        }
        ContentRead pageOrSite = cmServer.getContent(startContentId);
        list.add(pageOrSite);
        ContentList pages = pageOrSite.getContentList("polopoly.Department");
        ListIterator<ContentReference> iterator = pages.getListIterator();

        while (iterator.hasNext()) {
            ContentId contentId = iterator.next().getReferredContentId();
            getPageStructure(contentId, cmServer, list);
        }
    }

    public static String getMainAlias(final Site sitePolicy) throws CMException {
        if (sitePolicy == null) {
            return null;
        }
        Alias alias = sitePolicy.getMainAlias();
        if (alias != null) {
            return alias.getDomain();
        }
        return null;
    }

    public static Site getSiteFromContent(VersionedContentId contentId, PolicyCMServer cmServer)
            throws CMException {

        if (contentId == null) {
            return null;
        }
        ContentPolicy policy = (ContentPolicy) cmServer.getPolicy(contentId);
        ContentPolicy site = null;

        while (policy != null) {

            if (policy instanceof Site) {
                site = policy;
                break;
            }
            if (policy.getSecurityParentId() == null) {
                return null;
            }
            policy = (ContentPolicy) cmServer.getPolicy(policy.getSecurityParentId());
        }
        return (Site) site;
    }

    public static Site getSiteFromDomain(final String domain, final PolicyCMServer cmServer) throws CMException {
        if (domain == null) {
            return null;
        }

        final List<Site> sitePolicyList = getAllSites(cmServer);

        for (final Site sitePolicy : sitePolicyList) {
            Alias alias = sitePolicy.getMainAlias();

            if (alias != null && domain.equals(alias.getDomain())) {
                return sitePolicy;
            }
        }

        // if we did not find a match
        // than return the first one.

        if (sitePolicyList.size() > 0) {
            return sitePolicyList.get(0);
        }

        return null;
    }

    public static String getRequestDomain(final HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (Strings.isNullOrEmpty(host)) {
            final StringBuffer sb = new StringBuffer();
            sb.append(request.getScheme());
            sb.append("://");
            sb.append(request.getServerName());

            if (request.getServerPort() != 80) {
                sb.append(":");
                sb.append(request.getServerPort());
            }

            host = sb.toString();
        } else {
            if (!host.toLowerCase().startsWith("http")) {
                final StringBuffer sb = new StringBuffer();
                sb.append(request.getScheme());
                sb.append("://");
                sb.append(host);
                host = sb.toString();
            }
        }
        return host;
    }

}
