package com.atex.plugins.sitemap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.polopoly.cm.ContentId;
import com.polopoly.cm.DefaultMajorNames;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.ContentRead;
import com.polopoly.cm.path.PathSegment;
import com.polopoly.cm.policy.ContentPolicy;
import com.polopoly.cm.policy.Policy;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.common.util.FriendlyUrlConverter;
import com.polopoly.management.ServiceNotAvailableException;
import com.polopoly.search.solr.SearchClient;
import com.polopoly.search.solr.SearchResult;
import com.polopoly.search.solr.SearchResultPage;
import com.polopoly.search.solr.querydecorators.TimeStateFiltered;
import com.polopoly.search.solr.querydecorators.UnderPage;
import com.polopoly.search.solr.querydecorators.VisibleOnline;
import com.polopoly.search.solr.querydecorators.WithDecorators;
import com.polopoly.search.solr.schema.IndexFields;
import com.polopoly.siteengine.structure.Page;
import com.polopoly.siteengine.structure.Site;

/**
 * SitemapUtil
 *
 * @author mnova
 */
public class SitemapUtil {

    public static final ExternalContentId SITEMAP_INPUTTEMPLATE = new ExternalContentId(SitemapPolicy.INPUT_TEMPLATE);
    public static final String PREFIX_EXTERNAL_SITEMAP_INDEX = "com.atex.plugins.sitemap.index.";
    public static final String PREFIX_EXTERNAL_SITEMAP_NORMAL = "com.atex.plugins.sitemap.normal.";
    public static final String PREFIX_EXTERNAL_SITEMAP_DEPARTMENT = "com.atex.plugins.sitemap.department.";
    public static final String PREFIX_EXTERNAL_SITEMAP_NEWS = "com.atex.plugins.sitemap.news.";
    public static final String PREFIX_EXTERNAL_SITEMAP_VIDEO = "com.atex.plugins.sitemap.video.";
    public static final String FILENAME_XML = "file.xml";
    private static final Logger logger = Logger.getLogger(SitemapUtil.class.getName());
    private static final ThreadLocal<SimpleDateFormat> YEARMONTH_FMT = new ThreadLocal<SimpleDateFormat>() {
        /**
         * See {@link ThreadLocal#initialValue()}
         * @return a not null value.
         */
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyyMM");
        }

    };
    private static final ThreadLocal<SimpleDateFormat> RFCDATE_FMT = new ThreadLocal<SimpleDateFormat>() {
        /**
         * See {@link ThreadLocal#initialValue()}
         * @return a not null value.
         */
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssXXX");
        }

    };
    private final PolicyCMServer cmServer;
    private final SearchClient solrClient;
    private SitemapConfigPolicy config;

    public SitemapUtil(final PolicyCMServer cmServer, final SearchClient solrClient) {
        this.cmServer = cmServer;
        this.solrClient = solrClient;
        this.config = null;
    }

    private ContentId generateDepartmentSitemap(final ContentPolicy sitePolicy)
            throws CMException, IOException {

        ExternalContentId externalContentId = new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.Department,
                sitePolicy.getExternalId().getExternalId(), null));

        Document document = new Document();
        Namespace ns = Namespace.getNamespace("http://www.sitemaps.org/schemas/sitemap/0.9");
        Element urlset = new Element("urlset", ns);
        Comment comment = new Comment("Department sitemap of " + externalContentId.getExternalId());
        urlset.addContent(comment);
        document.setRootElement(urlset);

        List<Element> elementList = new ArrayList<>();
        List<ContentRead> pageOrSiteList = PolopolyUtil.getPageStructure(sitePolicy.getContentId().getContentId(), cmServer);

        for (ContentRead content : pageOrSiteList) {
            try {
                elementList.add(getSitemap((ContentPolicy) cmServer.getPolicy(content.getContentId()), ns));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not generate department sitemap fragment for " +
                        content.getContentId().getContentIdString());
            }
        }

        urlset.addContent(elementList);
        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "Department Sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    private String getArticleInputTemplates() throws CMException {
        final SitemapConfigPolicy config = getConfig();
        final List<String> inputTemplates = config.getArticleInputTemplates();
        return getInputTemplatesQuery(inputTemplates);
    }

    private String getGalleryInputTemplates() throws CMException {
        final SitemapConfigPolicy config = getConfig();
        final List<String> inputTemplates = config.getGalleryInputTemplates();
        return getInputTemplatesQuery(inputTemplates);
    }

    private String getVideoInputTemplates() throws CMException {
        final SitemapConfigPolicy config = getConfig();
        final List<String> inputTemplates = config.getVideoInputTemplates();
        return getInputTemplatesQuery(inputTemplates);
    }

    private String getAllInputTemplates() throws CMException {
        final SitemapConfigPolicy config = getConfig();
        final List<String> inputTemplates = Lists.newArrayList();
        inputTemplates.addAll(config.getArticleInputTemplates());
        inputTemplates.addAll(config.getGalleryInputTemplates());
        inputTemplates.addAll(config.getVideoInputTemplates());
        return getInputTemplatesQuery(inputTemplates);
    }

    private String getInputTemplatesQuery(final List<String> inputTemplates) {
        final StringBuilder sb = new StringBuilder();
        if (inputTemplates.size() > 0) {
            if (inputTemplates.size() > 1) {
                sb.append("(");
            }
            for (int idx = 0; idx < inputTemplates.size(); idx++) {
                if (idx > 0) {
                    sb.append(" OR ");
                }
                sb.append("(");
                sb.append(IndexFields.INPUT_TEMPLATE);
                sb.append(":");
                sb.append(inputTemplates.get(idx));
                sb.append(")");
            }
            if (inputTemplates.size() > 1) {
                sb.append(")");
            }
        }
        return sb.toString();
    }

    private ContentId generateNewsSitemap(final ContentPolicy sitePolicy)
            throws CMException, SolrServerException, ServiceNotAvailableException, IOException {

        ExternalContentId externalContentId =
                new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.News,
                        sitePolicy.getExternalId().getExternalId(), null));

        // TODO: mnova, just changed to using decorator.
        //SolrQuery query = new SolrQuery(getArticleInputTemplates() +
        //        " AND ( page:" + sitePolicy.getContentId().getContentId().getContentIdString() + ")");
        SolrQuery query = new SolrQuery(getArticleInputTemplates());
        query = new WithDecorators(
            new UnderPage(sitePolicy.getContentId().getContentId()),
            new TimeStateFiltered(),
            new VisibleOnline())
                .decorate(query);

        Date endDate = DateUtil.getEndDateOfToday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.add(Calendar.DAY_OF_MONTH, -2);
        Date startDate = calendar.getTime();
        String dateCondition = BuilderRangeDate.createQuery(startDate, endDate, "publishingDate");
        query = query.setQuery(query.getQuery() + " AND (" + dateCondition + ")");
        query.addSort("publishingDate", ORDER.desc);
        List<ContentId> contentIdList = getContentId(query, solrClient);

        Document document = new Document();
        Element urlset = new Element("urlset", "http://www.sitemaps.org/schemas/sitemap/0.9");
        Namespace namespace = Namespace.getNamespace("news", "http://www.google.com/schemas/sitemap-news/0.9");
        urlset.addNamespaceDeclaration(namespace);
        Comment comment = new Comment("New sitemap of " + externalContentId.getExternalId());
        urlset.addContent(comment);
        document.setRootElement(urlset);

        // see https://support.google.com/news/publisher/answer/74288

        for (ContentId contentId : contentIdList) {
            Policy policy = cmServer.getPolicy(contentId);

            if (policy instanceof NewsSitemapable) {
                try {
                    urlset.addContent(((NewsSitemapable) policy).getNewsSitemap());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Could not generate news sitemap fragment for " +
                            policy.getContentId().getContentIdString(), e);
                }
            }
        }

        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "News Sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    private ContentId generateNormalSitemap(final ContentPolicy sitePolicy) throws CMException, SolrServerException,
                                                                                   ServiceNotAvailableException,
                                                                                   IOException {
        return generateNormalSitemap(sitePolicy, new Date());
    }

    private ContentId generateNormalSitemap(final ContentPolicy sitePolicy, final Date date) throws CMException,
                                                                                                    SolrServerException,
                                                                                                    ServiceNotAvailableException,
                                                                                                    IOException {

        // TODO: mnova, just changed to using decorator.
        //SolrQuery query = new SolrQuery(getAllInputTemplates() +
        //        " AND ( page:" + sitePolicy.getContentId().getContentId().getContentIdString() + ")");
        SolrQuery query = new SolrQuery(getArticleInputTemplates());
        query = new WithDecorators(
                new UnderPage(sitePolicy.getContentId().getContentId()),
                new TimeStateFiltered(),
                new VisibleOnline())
                .decorate(query);

        Date startDate = DateUtil.getStartDateOfMonth(date);
        Date endDate = DateUtil.getEndDateOfMonth(date);
        String dateCondition = BuilderRangeDate.createQuery(startDate, endDate, "publishingDate");
        query = query.setQuery(query.getQuery() + " AND (" + dateCondition + ")");
        query.addSort("publishingDate", ORDER.desc);
        List<ContentId> contentIdList = getContentId(query, solrClient);

        ExternalContentId externalContentId = new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.Normal,
                sitePolicy.getExternalId().getExternalId(), startDate));

        Document document = new Document();
        Namespace ns = Namespace.getNamespace("http://www.sitemaps.org/schemas/sitemap/0.9");
        Element urlset = new Element("urlset", ns);
        Comment comment = new Comment("Normal sitemap of " + externalContentId.getExternalId());
        urlset.addContent(comment);
        document.setRootElement(urlset);

        for (ContentId contentId : contentIdList) {
            Policy policy = cmServer.getPolicy(contentId);

            if (policy instanceof Sitemapable) {
                try {
                    urlset.addContent(((Sitemapable) policy).getSitemap(ns));

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Could not generate normal sitemap fragment for " +
                            policy.getContentId().getContentIdString(), e);
                }
            } else {
                final Element sitemapElement = getSitemap((ContentPolicy) policy, ns);
                if (sitemapElement != null) {
                    urlset.addContent(sitemapElement);
                }
            }
        }
        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "Normal Sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    public boolean generatedVideoSitemaps() throws CMException {

        final List<Site> sitePolicies = getAllSites();
        boolean success = true;

        for (Site sitePolicy : sitePolicies) {
            try {
                generateVideoSitemap((ContentPolicy) sitePolicy);
            } catch (CMException | SolrServerException | ServiceNotAvailableException | IOException e) {
                success = false;
                logger.log(Level.SEVERE, "Failed to generate a department sitemap for " + sitePolicy.getName(), e);
            }
        }
        return success;
    }

    private List<Site> getAllSites() throws CMException {
        List<Site> sitePolicies;
        final SitemapConfigPolicy configPolicy = getConfig();
        final List<String> sitesId = configPolicy.getSitesId();
        if (sitesId.size() != 0) {
            sitePolicies = Lists.newArrayList();
            for (final String siteId : sitesId) {
                final Policy policy = cmServer.getPolicy(new ExternalContentId(siteId));
                if (policy instanceof Site) {
                    sitePolicies.add((Site) policy);
                }
            }
        } else {
            sitePolicies = PolopolyUtil.getAllSites(cmServer);
        }
        return sitePolicies;
    }

    private ContentId generateVideoSitemap(final ContentPolicy sitePolicy) throws CMException, SolrServerException,
                                                                                  ServiceNotAvailableException,
                                                                                  IOException {
        ExternalContentId externalContentId = new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.Video,
                sitePolicy.getExternalId().getExternalId(), null));

        // TODO: mnova, just changed to using decorator.
        //SolrQuery query = new SolrQuery(getVideoInputTemplates() +
        //        " AND (page:" + sitePolicy.getContentId().getContentId().getContentIdString() + ")");
        SolrQuery query = new SolrQuery(getVideoInputTemplates());
        query = new WithDecorators(
                new UnderPage(sitePolicy.getContentId().getContentId()),
                new TimeStateFiltered(),
                new VisibleOnline())
                .decorate(query);

        Date endDate = DateUtil.getEndDateOfToday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.add(Calendar.DAY_OF_MONTH, -2);
        Date startDate = calendar.getTime();
        String dateCondition = BuilderRangeDate.createQuery(startDate, endDate, "publishingDate");
        query.addFilterQuery(dateCondition);

        query.addSort("publishingDate", ORDER.desc);

        List<ContentId> contentIdList = getContentId(query, solrClient);

        Document document = new Document();
        Element urlset = new Element("urlset", "http://www.sitemaps.org/schemas/sitemap/0.9");
        Namespace namespace = Namespace.getNamespace("video", "http://www.google.com/schemas/sitemap-video/1.1");
        urlset.addNamespaceDeclaration(namespace);
        Comment comment = new Comment("Video sitemap of " + externalContentId.getExternalId());
        urlset.addContent(comment);
        document.setRootElement(urlset);

        for (ContentId contentId : contentIdList) {
            Policy policy = cmServer.getPolicy(contentId);

            if (policy instanceof VideoSitemapable) {
                try {
                    urlset.addContent(((VideoSitemapable) policy).getVideoSitemap());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Could not generate video sitemap fragment for " +
                            policy.getContentId().getContentIdString(), e);
                }
            }
        }

        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "Video sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    private ContentId generateIndexSitemap(final ContentPolicy sitePolicy, List<ContentId> sitemapContentIdList)
            throws CMException, IOException {
        ExternalContentId externalContentId = new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.Index,
                sitePolicy.getExternalId().getExternalId(), null));

        Document document = new Document();
        Namespace ns = Namespace.getNamespace("http://www.sitemaps.org/schemas/sitemap/0.9");
        Element sitemapindex = new Element("sitemapindex", ns);
        Comment comment = new Comment("Index sitemap of " + externalContentId.getExternalId());
        sitemapindex.addContent(comment);
        document.setRootElement(sitemapindex);

        for (ContentId contentId : sitemapContentIdList) {
            Policy policy = cmServer.getPolicy(contentId);

            if (policy instanceof SitemapPolicy) {

                try {
                    Element sitemap = new Element("sitemap", ns);
                    sitemapindex.addContent(sitemap);

                    Comment commentOfSitemap = new Comment("link to " +
                            ((SitemapPolicy) policy).getExternalId().getExternalId());
                    sitemap.addContent(commentOfSitemap);

                    Element loc = new Element("loc", ns);
                    loc.setText(getURLofFile(PolopolyUtil.getMainAlias((Site) sitePolicy), (SitemapPolicy) policy));
                    sitemap.addContent(loc);

                    // using last modified.
                    final long version = policy.getContentId().getVersion();
                    final Date lastModified = new Date(version * 1000);

                    Element lastmod = new Element("lastmod", ns);
                    lastmod.setText(RFCDATE_FMT.get().format(lastModified));
                    sitemap.addContent(lastmod);

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Could not generate index sitemap fragment for " +
                            policy.getContentId().getContentIdString(), e);
                }
            }
        }

        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "Index Sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    private List<ContentId> getContentId(SolrQuery query, SearchClient solrClient)
            throws SolrServerException, ServiceNotAvailableException {
        logger.log(Level.INFO, query.getQuery());
        SearchResult result = solrClient.search(query, 50000);
        SearchResultPage resultPage = result.getPage(0);
        return resultPage.getHits();
    }

    private String getExternalIdStringOfSitemap(SitemapType sitemapType, String siteExternalIdString, Date date) {
        if (SitemapType.Index.equals(sitemapType)) {
            return PREFIX_EXTERNAL_SITEMAP_INDEX + siteExternalIdString;

        } else if (SitemapType.Normal.equals(sitemapType)) {

            if (date != null) {
                return PREFIX_EXTERNAL_SITEMAP_NORMAL + siteExternalIdString + "." + YEARMONTH_FMT.get().format(date);
            } else {
                throw new IllegalArgumentException("Need date for normal sitemap.");
            }
        } else if (SitemapType.Department.equals(sitemapType)) {
            return PREFIX_EXTERNAL_SITEMAP_DEPARTMENT + siteExternalIdString;

        } else if (SitemapType.News.equals(sitemapType)) {
            return PREFIX_EXTERNAL_SITEMAP_NEWS + siteExternalIdString;

        } else if (SitemapType.Video.equals(sitemapType)) {
            // Note: now prefix
            return PREFIX_EXTERNAL_SITEMAP_VIDEO + siteExternalIdString;

        } else {
            throw new IllegalArgumentException("Need date for normal sitemap.");
        }
    }

    public boolean generateCurrentMonthNormalSitemap() throws CMException {

        final List<Site> sitePolicies = getAllSites();
        boolean success = true;

        for (Site sitePolicy : sitePolicies) {
            try {
                List<ContentId> contentIdList = new ArrayList<>();
                //Generate department sitemap
                try {
                    ContentId departmentSitemapContentId = generateDepartmentSitemap((ContentPolicy) sitePolicy);
                    contentIdList.add(departmentSitemapContentId);
                } catch (CMException | IOException e) {
                    success = false;
                    logger.log(Level.SEVERE, "Failed to generate a department sitemap for " + sitePolicy.getName(), e);
                }

                //Generate latest month sitemap
                /*try {
                    generateNormalSitemap(sitePolicy, cmServer, solrClient);
                } catch (CMException | IOException | SolrServerException | ServiceNotAvailableException e) {
                    success = false;
                    logger.log(Level.SEVERE, "Failed to generate a current normal sitemap for " + sitePolicy.getName(), e);
                }
                */

                // Get all other months sitemap
                try {
                    contentIdList.addAll(getAllMonthSitemap((ContentPolicy) sitePolicy));
                } catch (CMException | SolrServerException | ServiceNotAvailableException e) {
                    success = false;
                    logger.log(Level.SEVERE, "Failed to get normal monthly sitemap for " + sitePolicy.getName(), e);
                }

                // Generate index
                generateIndexSitemap((ContentPolicy) sitePolicy, contentIdList);

            } catch (CMException | IOException e) {
                success = false;
                logger.log(Level.SEVERE, "Failed to generate an index sitemap for " + sitePolicy.getName(), e);
            }
        }
        return success;
    }

    public boolean generateCurrentNewsSitemap() throws CMException {

        final List<Site> sitePolicies = getAllSites();
        boolean success = true;

        for (Site sitePolicy : sitePolicies) {
            //Generate news sitemap
            try {
                generateNewsSitemap((ContentPolicy) sitePolicy);

            } catch (CMException | IOException | SolrServerException | ServiceNotAvailableException e) {
                success = false;
                logger.log(Level.SEVERE, "Failed to generate a news sitemap for " + sitePolicy.getName(), e);
            }
        }
        return success;
    }

    public void generateOtherMonthNormalSitemap(final Date date) throws CMException {

        final List<Site> sitePolicies = getAllSites();

        for (Site sitePolicy : sitePolicies) {
            //List<ContentId> contentIdList = new ArrayList<>();
            //Generate latest month sitemap
            try {
                generateNormalSitemap((ContentPolicy) sitePolicy, date);

            } catch (CMException | IOException | SolrServerException | ServiceNotAvailableException e) {
                logger.log(Level.SEVERE, "Failed to generate a normal sitemap for a month " + sitePolicy.getName() +
                        " date: " + date, e);
            }
        }
    }

    public List<ContentId> getAllMonthSitemap(final ContentPolicy sitePolicy) throws SolrServerException,
                                                                                     ServiceNotAvailableException,
                                                                                     CMException {
        SolrQuery query = new SolrQuery(String.format("(%s:com.atex.plugins.sitemap.Sitemap)", IndexFields.INPUT_TEMPLATE));
        List<ContentId> contentIds = getContentId(query, solrClient);
        List<ContentId> temp = new ArrayList<>();
        String targetPrefix = PREFIX_EXTERNAL_SITEMAP_NORMAL + sitePolicy.getExternalId().getExternalId();

        for (ContentId contentId : contentIds) {
            Policy policy = cmServer.getPolicy(contentId);

            // TODO: mnova
            if (policy instanceof SitemapPolicy) {
                SitemapPolicy sitemapPolicy = (SitemapPolicy) policy;

                if (sitemapPolicy.getExternalId() != null) {
                    String externalId = sitemapPolicy.getExternalId().getExternalId();

                    if (externalId.indexOf(targetPrefix) == 0) {
                        temp.add(contentId);
                    }
                }
            }
        }
        return temp;
    }

    /**
     * Generate an xml like this:
     * <p/>
     * <url>
     * <loc>http://www.example.com/</loc>
     * <lastmod>2005-01-01</lastmod>
     * <changefreq>monthly</changefreq>
     * <priority>0.8</priority>
     * </url>
     *
     * @param contentPolicy
     * @param ns
     * @return
     * @throws CMException
     */
    private Element getSitemap(final ContentPolicy contentPolicy, Namespace ns)
            throws CMException {

        final Element url = new Element("url", ns);

        final Element loc = new Element("loc", ns);
        url.addContent(loc);
        loc.setText(getCanonicalUrl(contentPolicy));

        // using last modified instead of getVersionInfo to speed up resolution.
        final long version = contentPolicy.getContentId().getVersion();
        final Date lastModified = new Date(version * 1000);

        Element lastmod = new Element("lastmod", ns);
        url.addContent(lastmod);
        lastmod.setText(RFCDATE_FMT.get().format(lastModified));

        final Element changeFreq = new Element("changefreq", ns);
        url.addContent(changeFreq);

        final Element priority = new Element("priority", ns);
        url.addContent(priority);

        final SitemapConfigPolicy config = getConfig();

        if (contentPolicy instanceof Site) {
            priority.setText(config.getPrioritySite());
            changeFreq.setText(config.getChangeFreqSite());
        } else if (contentPolicy instanceof Page) {
            priority.setText(config.getPriorityPage());
            changeFreq.setText(config.getChangeFreqPage());
        } else {
            priority.setText(config.getPriorityContent());
            changeFreq.setText(config.getChangeFreqContent());
        }

        return url;
    }

    private String getCanonicalUrl(final ContentPolicy contentPolicy) throws CMException {

        String url = "";

        if (contentPolicy instanceof Site) {
            url = PolopolyUtil.getMainAlias((Site) contentPolicy);
        } else if (contentPolicy instanceof Page) {
            Site sitePolicy = PolopolyUtil.getSiteFromContent(contentPolicy.getContentId(), cmServer);
            url = PolopolyUtil.getMainAlias(sitePolicy) + "/" + getUriForPage(contentPolicy, 0);
        } else {
            final ContentId parentId = contentPolicy.getSecurityParentId();
            try {
                final ContentPolicy parentPolicy = (ContentPolicy) cmServer.getPolicy(parentId.getContentId());
                url = getCanonicalUrl(parentPolicy);
            } catch (CMException e) {
                logger.log(Level.SEVERE, "cannot resolve " + parentId.getContentIdString() + ": " + e.getMessage());
            }
            url += String.format("/%s-%s",
                    FriendlyUrlConverter.convert(contentPolicy.getName()),
                    contentPolicy.getContentId().getContentId().getContentIdString());
        }

        return Strings.nullToEmpty(url).toLowerCase();
    }

    private String getUriForPage(final ContentPolicy pagePolicy, int level) throws CMException {
        level++;

        if (level > 10) {
            throw new RuntimeException("Maximum page depth reached");
        }
        String uri = "";
        if (pagePolicy instanceof PathSegment) {
            uri = ((PathSegment) pagePolicy).getPathSegmentString();
        }

        if (pagePolicy.getSecurityParentId() != null) {
            Policy policy = cmServer.getPolicy(pagePolicy.getSecurityParentId());

            if (policy instanceof Page && !(policy instanceof Site)) {
                return getUriForPage((ContentPolicy) policy, level) + "/" + uri;
            }
        }
        return uri;
    }

    public ContentId saveToFile(ExternalContentId externalId, String name, String content, final ContentId securityParentId)
            throws CMException, IOException {

        SitemapPolicy policy = null;

        try {

            final ContentId contentId = cmServer.findContentIdByExternalId(externalId);
            if (contentId == null) {
                int major = cmServer.getMajorByName(DefaultMajorNames.ARTICLE);
                policy = (SitemapPolicy) cmServer.createUnversionedContent(major, securityParentId.getContentId(), SITEMAP_INPUTTEMPLATE);
                policy.setExternalId(externalId.getExternalId());
            } else {
                policy = (SitemapPolicy) cmServer.getPolicy(contentId);
                policy.forcedUnlock();
                policy = (SitemapPolicy) cmServer.createContentVersion(policy.getContentId());
            }

            policy.setName(name);

            final InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            try {
                policy.importFile(FILENAME_XML, is);
            } finally {
                IOUtils.closeQuietly(is);
            }
            cmServer.commitContent(policy);
            logger.log(Level.INFO, "Successfully generated sitemap " + externalId);
        } catch (CMException | IOException e) {
            logger.log(Level.SEVERE, "cannot save sitemap " + name, e);
            if (policy != null) {
                try {
                    cmServer.abortContent(policy);
                } catch (CMException e1) {
                    logger.log(Level.SEVERE, e1.getMessage(), e1);
                }
            }
            throw e;
        }
        return policy.getContentId();
    }

    // TODO: mnova
    private String getURLofFile(String domain, SitemapPolicy fileResourcePolicy) {
        return domain + "/polopoly_fs/" + fileResourcePolicy.getContentId().getContentIdString() + "!/" + FILENAME_XML;
    }

    public InputStream getURLofSitemap(final HttpServletRequest request) throws Exception {
        String domain = PolopolyUtil.getRequestDomain(request);

        // TODO: mnova
        try {
            ContentPolicy sitePolicy = (ContentPolicy) PolopolyUtil.getSiteFromDomain(domain, cmServer);
            String externalIdString;
            String requestUri = request.getRequestURI();

            if (requestUri.indexOf("sitemap_index.xml") >= 0) {
                externalIdString =
                        getExternalIdStringOfSitemap(SitemapType.Index, sitePolicy.getExternalId()
                                                                                  .getExternalId(), null);

            } else if (requestUri.indexOf("sitemap_news.xml") >= 0) {
                externalIdString =
                        getExternalIdStringOfSitemap(SitemapType.News, sitePolicy.getExternalId()
                                                                                 .getExternalId(), null);
            } else if (requestUri.indexOf("sitemap_video.xml") >= 0) {
                externalIdString =
                        getExternalIdStringOfSitemap(SitemapType.Video, sitePolicy.getExternalId()
                                                                                  .getExternalId(), null);
            } else {
                externalIdString =
                        getExternalIdStringOfSitemap(SitemapType.Index, sitePolicy.getExternalId()
                                                                                  .getExternalId(), null);
            }

            SitemapPolicy policy = (SitemapPolicy) cmServer.getPolicy(new ExternalContentId(externalIdString));

            if (policy != null) {
                return policy.getFileStream(FILENAME_XML);
            }

            logger.log(Level.SEVERE, "Could not get sitemap: " + request.getRequestURL());
            throw new IllegalArgumentException("Could not get sitemap: " + request.getRequestURL());
        } catch (CMException | IOException e) {
            logger.log(Level.SEVERE, "Could not get sitemap: " + request.getRequestURL(), e);
            throw e;
        }
    }

    private SitemapConfigPolicy getConfig() throws CMException {
        if (config == null) {
            config = (SitemapConfigPolicy) cmServer.getPolicy(new ExternalContentId(SitemapConfigPolicy.CONFIG_EXT_ID));
        }
        return config;
    }


    enum SitemapType {
        Index,
        Normal,
        Department,
        News,
        Video
    }

}
