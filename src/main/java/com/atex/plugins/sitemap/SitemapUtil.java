package com.atex.plugins.sitemap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.polopoly.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.ContentVersionId;
import com.atex.onecms.content.IdUtil;
import com.atex.onecms.content.Subject;
import com.atex.onecms.ws.image.ImageServiceUrlBuilder;
import com.atex.plugins.baseline.url.URLBuilder;
import com.atex.plugins.baseline.url.URLBuilderCapabilities;
import com.atex.plugins.baseline.url.URLBuilderLoader;
import com.atex.plugins.sitemap.protocol.ChangeFrequency;
import com.atex.plugins.sitemap.protocol.ElementNodeBuilder;
import com.atex.plugins.sitemap.protocol.Namespaces;
import com.atex.plugins.sitemap.protocol.NewsElementNodeBuilder;
import com.atex.plugins.sitemap.protocol.UrlElementNodeBuilder;
import com.atex.plugins.sitemap.protocol.UrlSetElementNodeBuilder;
import com.atex.plugins.sitemap.protocol.VideoElementNodeBuilder;
import com.atex.plugins.sitemap.variant.NewsArticleSitemapBean;
import com.atex.plugins.sitemap.variant.VideoSitemapBean;
import com.google.common.collect.Lists;
import com.polopoly.cm.ContentId;
import com.polopoly.cm.DefaultMajorNames;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.VersionedContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.ContentRead;
import com.polopoly.cm.policy.ContentPolicy;
import com.polopoly.cm.policy.Policy;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.management.ServiceNotAvailableException;
import com.polopoly.search.solr.SearchClient;
import com.polopoly.search.solr.SearchResult;
import com.polopoly.search.solr.SearchResultPage;
import com.polopoly.search.solr.querydecorators.TimeStateFiltered;
import com.polopoly.search.solr.querydecorators.UnderPage;
import com.polopoly.search.solr.querydecorators.VisibleOnline;
import com.polopoly.search.solr.querydecorators.WithDecorators;
import com.polopoly.search.solr.querydecorators.WithInputTemplate;
import com.polopoly.search.solr.schema.IndexFields;
import com.polopoly.siteengine.structure.Page;
import com.polopoly.siteengine.structure.Site;

/**
 * SitemapUtil
 *
 * @author mnova
 */
public class SitemapUtil {

    private static final Logger LOGGER = Logger.getLogger(SitemapUtil.class.getName());
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

    private static final ExternalContentId SITEMAP_INPUTTEMPLATE = new ExternalContentId(SitemapPolicy.INPUT_TEMPLATE);
    private static final String PREFIX_EXTERNAL_SITEMAP_INDEX = "com.atex.plugins.sitemap.index.";
    private static final String PREFIX_EXTERNAL_SITEMAP_NORMAL = "com.atex.plugins.sitemap.normal.";
    private static final String PREFIX_EXTERNAL_SITEMAP_DEPARTMENT = "com.atex.plugins.sitemap.department.";
    private static final String PREFIX_EXTERNAL_SITEMAP_NEWS = "com.atex.plugins.sitemap.news.";
    private static final String PREFIX_EXTERNAL_SITEMAP_VIDEO = "com.atex.plugins.sitemap.video.";
    private static final String FILENAME_XML = "file.xml";

    private final PolicyCMServer cmServer;
    private final ContentManager contentManager;
    private final SearchClient solrClient;
    private final URLBuilder urlBuilder;
    private final ImageUtil imageUtil;
    private SitemapConfigPolicy config;

    public SitemapUtil(final PolicyCMServer cmServer, final ContentManager contentManager, final SearchClient solrClient) {
        this.cmServer = cmServer;
        this.contentManager = contentManager;
        this.solrClient = solrClient;
        this.config = null;
        this.urlBuilder = new URLBuilderLoader(cmServer, contentManager)
                .create(URLBuilderCapabilities.WWW);
        this.imageUtil = new ImageUtil(cmServer, contentManager);
    }

    private ContentId generateDepartmentSitemap(final ContentPolicy sitePolicy)
            throws CMException, IOException {

        LOGGER.log(Level.INFO, "Generating department sitemap for " + sitePolicy.getContentId().getContentIdString());

        ExternalContentId externalContentId = new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.Department,
                sitePolicy.getExternalId().getExternalId(), null));

        final Element urlset = new UrlSetElementNodeBuilder()
                .comment("Department sitemap of " + externalContentId.getExternalId())
                .build();
        final Document document = new Document();
        document.setRootElement(urlset);

        List<Element> elementList = new ArrayList<>();
        List<ContentRead> pageOrSiteList = PolopolyUtil.getPageStructure(sitePolicy.getContentId().getContentId(), cmServer);

        for (ContentRead content : pageOrSiteList) {
            try {
                elementList.add(getSitemap((ContentPolicy) cmServer.getPolicy(content.getContentId())));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not generate department sitemap fragment for " +
                        content.getContentId().getContentIdString());
            }
        }

        urlset.addContent(elementList);
        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "Department Sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    private WithInputTemplate withArticleInputTemplates() throws CMException {
        final SitemapConfigPolicy config = getConfig();
        final List<String> inputTemplates = config.getArticleInputTemplates();
        return getInputTemplatesQuery(inputTemplates);
    }

    private WithInputTemplate withGalleryInputTemplates() throws CMException {
        final SitemapConfigPolicy config = getConfig();
        final List<String> inputTemplates = config.getGalleryInputTemplates();
        return getInputTemplatesQuery(inputTemplates);
    }

    private WithInputTemplate withVideoInputTemplates() throws CMException {
        final SitemapConfigPolicy config = getConfig();
        final List<String> inputTemplates = config.getVideoInputTemplates();
        return getInputTemplatesQuery(inputTemplates);
    }

    private WithInputTemplate getInputTemplatesQuery(final List<String> inputTemplates) {
        return new WithInputTemplate(inputTemplates.toArray(new String[inputTemplates.size()]));
    }

    private ContentId generateNewsSitemap(final ContentPolicy sitePolicy) throws CMException {

        ExternalContentId externalContentId =
                new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.News,
                        sitePolicy.getExternalId().getExternalId(), null));

        SolrQuery query = new WithDecorators(
                withArticleInputTemplates(),
                new UnderPage(sitePolicy.getContentId().getContentId()),
                new TimeStateFiltered(),
                new VisibleOnline())
                .decorate(new SolrQuery("*:*"));

        Date endDate = DateUtil.getEndDateOfToday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.add(Calendar.DAY_OF_MONTH, -2);
        Date startDate = calendar.getTime();
        String dateCondition = BuilderRangeDate.createQuery(startDate, endDate, "publishingDate");
        query = query.setQuery(query.getQuery() + " AND (" + dateCondition + ")");
        query.addSort("publishingDate", ORDER.desc);
        List<ContentId> contentIdList = getContentId(query, solrClient);

        final Element urlset = new UrlSetElementNodeBuilder()
                .namespaces(Namespaces.NEWS)
                .comment("News sitemap of " + externalContentId.getExternalId())
                .build();
        final Document document = new Document();
        document.setRootElement(urlset);

        // see https://support.google.com/news/publisher/answer/74288

        for (ContentId contentId : contentIdList) {
            Policy policy = cmServer.getPolicy(contentId);

            if (policy instanceof NewsSitemapable) {
                try {
                    urlset.addContent(((NewsSitemapable) policy).getNewsSitemap());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Could not generate news sitemap fragment for " +
                            policy.getContentId().getContentIdString(), e);
                }
            } else {
                final NewsArticleSitemapBean article = fetchNewsArticle(contentId, Subject.NOBODY_CALLER);
                if (article != null && policy instanceof ContentPolicy) {
                    urlset.addContent(createSitemapNodeFrom((ContentPolicy) policy, article));
                }
            }
        }

        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "News Sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    private Element createSitemapNodeFrom(final ContentPolicy policy, final NewsArticleSitemapBean article)
            throws CMException {

        return ((NewsElementNodeBuilder) setupSitemapNodeBuilder(new NewsElementNodeBuilder(), policy))
                .publicationName(getSiteNameForSitemap(policy.getContentId()))
                .publicationLang(article.getLanguage())
                .genres(article.getGenres())
                .publicationDate(new Date(article.getPublishingDateTime()))
                .title(article.getName())
                .keywords(article.getKeywords())
                .stockTickers(article.getStockTickers())
                .build();
    }

    private String getSiteNameForSitemap(VersionedContentId versionedContentId) throws CMException{
        Site sitePolicy = PolopolyUtil.getSiteFromContent(versionedContentId, getCMServer());
        if (sitePolicy != null) {
            return sitePolicy.getName();
        }
        return null;
    }

    private ContentId generateNormalSitemap(final ContentPolicy sitePolicy) throws CMException {
        return generateNormalSitemap(sitePolicy, new Date());
    }

    private ContentId generateNormalSitemap(final ContentPolicy sitePolicy, final Date date) throws CMException {

        LOGGER.log(Level.INFO, "Generating article sitemap for " + sitePolicy.getContentId().getContentIdString());

        SolrQuery query = new WithDecorators(
                withArticleInputTemplates(),
                new UnderPage(sitePolicy.getContentId().getContentId()),
                new TimeStateFiltered(),
                new VisibleOnline())
                .decorate(new SolrQuery("*:*"));

        Date startDate = DateUtil.getStartDateOfMonth(date);
        Date endDate = DateUtil.getEndDateOfMonth(date);
        String dateCondition = BuilderRangeDate.createQuery(startDate, endDate, "publishingDate");
        query = query.setQuery(query.getQuery() + " AND (" + dateCondition + ")");
        query.addSort("publishingDate", ORDER.desc);
        List<ContentId> contentIdList = getContentId(query, solrClient);

        ExternalContentId externalContentId = new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.Normal,
                sitePolicy.getExternalId().getExternalId(), startDate));

        final Element urlset = new UrlSetElementNodeBuilder()
                .comment("Normal sitemap of " + externalContentId.getExternalId())
                .build();
        final Document document = new Document();
        document.setRootElement(urlset);

        for (ContentId contentId : contentIdList) {
            Policy policy = cmServer.getPolicy(contentId);

            if (policy instanceof Sitemapable) {
                try {
                    urlset.addContent(((Sitemapable) policy).getSitemap(Namespaces.SITEMAP.ns()));

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Could not generate normal sitemap fragment for " +
                            policy.getContentId().getContentIdString(), e);
                }
            } else {
                final Element sitemapElement = getSitemap((ContentPolicy) policy);
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
            } catch (CMException e) {
                success = false;
                LOGGER.log(Level.SEVERE, "Failed to generate a video sitemap for " + sitePolicy.getName(), e);
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

    private ContentId generateVideoSitemap(final ContentPolicy sitePolicy) throws CMException {

        LOGGER.log(Level.INFO, "Generating video sitemap for " + sitePolicy.getContentId().getContentIdString());

        ExternalContentId externalContentId = new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.Video,
                sitePolicy.getExternalId().getExternalId(), null));

        SolrQuery query = new WithDecorators(
                withVideoInputTemplates(),
                new UnderPage(sitePolicy.getContentId().getContentId()),
                new TimeStateFiltered(),
                new VisibleOnline())
                .decorate(new SolrQuery("*:*"));

        Date endDate = DateUtil.getEndDateOfToday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.add(Calendar.DAY_OF_MONTH, -2);
        Date startDate = calendar.getTime();
        String dateCondition = BuilderRangeDate.createQuery(startDate, endDate, "publishingDate");
        query.addFilterQuery(dateCondition);

        query.addSort("publishingDate", ORDER.desc);

        List<ContentId> contentIdList = getContentId(query, solrClient);

        final Element urlset = new UrlSetElementNodeBuilder()
                .namespaces(Namespaces.VIDEO)
                .comment("Video sitemap of " + externalContentId.getExternalId())
                .build();
        final Document document = new Document();
        document.setRootElement(urlset);

        for (ContentId contentId : contentIdList) {
            Policy policy = cmServer.getPolicy(contentId);

            if (policy instanceof VideoSitemapable) {
                try {
                    urlset.addContent(((VideoSitemapable) policy).getVideoSitemap());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Could not generate video sitemap fragment for " +
                            policy.getContentId().getContentIdString(), e);
                }
            } else {
                final VideoSitemapBean video = fetchVideo(contentId, Subject.NOBODY_CALLER);
                if (video != null && policy instanceof ContentPolicy) {
                    urlset.addContent(createSitemapNodeFrom((ContentPolicy) policy, video));
                }
            }
        }

        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "Video sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    private Element createSitemapNodeFrom(final ContentPolicy policy, final VideoSitemapBean video)
            throws CMException {

        final Site sitePolicy = PolopolyUtil.getSiteFromContent(policy.getContentId(), getCMServer());
        final String siteUrl = PolopolyUtil.getMainAlias(sitePolicy);

        String mediaUrl = video.getMediaUrl();
        if (mediaUrl != null) {
            if (mediaUrl.startsWith("/") || mediaUrl.toLowerCase().startsWith("http://localhost:8080")) {
                String url = siteUrl;
                if (url != null) {
                    if (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    if (mediaUrl.toLowerCase().startsWith("http://localhost:8080/")) {
                        mediaUrl = url + mediaUrl.substring(21);
                    } else {
                        mediaUrl = url + mediaUrl;
                    }
                }
            }
        }

        String thumbnailUrl = null;
        if (video.getImageContentId() != null) {
            final ImageServiceUrlBuilder imageUrlBuilder = imageUtil.getUrlBuilder(video.getImageContentId());
            if (imageUrlBuilder != null) {
                thumbnailUrl = imageUrlBuilder.buildUrl();
                if (thumbnailUrl.startsWith("/")) {
                    if (siteUrl != null) {
                        if (siteUrl.endsWith("/")) {
                            thumbnailUrl = siteUrl.substring(0, siteUrl.length() - 1) + thumbnailUrl;
                        } else {
                            thumbnailUrl = siteUrl + thumbnailUrl;
                        }
                    }
                }
            }
        }

        return ((VideoElementNodeBuilder) setupSitemapNodeBuilder(new VideoElementNodeBuilder(), policy))
                .title(video.getTitle())
                .description(video.getDescription())
                .thumbnailUrl(thumbnailUrl)
                .mediaUrl(mediaUrl)
                .mediaPlayerUrl(video.getMediaPlayerUrl())
                .duration(video.getDuration())
                .rating(video.getRating())
                .tag(video.getTag())
                .category(video.getCategory())
                .countryRestriction(video.getCountryRestriction())
                .publicationDate(video.getPublicationDate())
                .expirationDate(video.getExpirationDate())
                .build();
    }

    private ContentId generateIndexSitemap(final ContentPolicy sitePolicy, List<ContentId> sitemapContentIdList)
            throws CMException, IOException {

        LOGGER.log(Level.INFO, "Generating index sitemap for " + sitePolicy.getContentId().getContentIdString());

        final ExternalContentId externalContentId = new ExternalContentId(getExternalIdStringOfSitemap(SitemapType.Index,
                sitePolicy.getExternalId().getExternalId(), null));

        final Document document = new Document();
        final Element sitemapindex = new ElementNodeBuilder()
                .rootName("sitemapindex")
                .namespaces(Namespaces.SITEMAP)
                .comment("Index sitemap of " + externalContentId.getExternalId())
                .build();
        document.setRootElement(sitemapindex);

        for (ContentId contentId : sitemapContentIdList) {
            Policy policy = cmServer.getPolicy(contentId);

            if (policy instanceof SitemapPolicy) {

                try {
                    final Element sitemap = new UrlElementNodeBuilder()
                            .url(getURLofFile(PolopolyUtil.getMainAlias((Site) sitePolicy), (SitemapPolicy) policy))
                            .lastModified(((ContentPolicy) policy).getVersionInfo().getVersionCommitDate())
                            .rootName("sitemap")
                            .comment("link to " +
                                    ((SitemapPolicy) policy).getExternalId().getExternalId())
                            .build();
                    sitemapindex.addContent(sitemap);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Could not generate index sitemap fragment for " +
                            policy.getContentId().getContentIdString(), e);
                }
            }
        }

        String content = new XMLOutputter().outputString(document);

        return saveToFile(externalContentId, "Index Sitemap for " + sitePolicy.getName(), content, sitePolicy.getContentId());
    }

    private List<ContentId> getContentId(SolrQuery query, SearchClient solrClient) throws CMException {

        LOGGER.log(Level.FINE, query.getQuery());

        try {
            SearchResult result = solrClient.search(query, 50000);
            SearchResultPage resultPage = result.getPage(0);
            return resultPage.getHits();
        } catch (SolrServerException | ServiceNotAvailableException e) {
            throw new CMException(e);
        }
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
                    LOGGER.log(Level.SEVERE, "Failed to generate a department sitemap for " + sitePolicy.getName(), e);
                }

                //Generate latest month sitemap
                try {
                    generateNormalSitemap((ContentPolicy) sitePolicy);
                } catch (CMException e) {
                    success = false;
                    LOGGER.log(Level.SEVERE, "Failed to generate a current normal sitemap for " + sitePolicy.getName(), e);
                }

                try {
                    generateNewsSitemap((ContentPolicy) sitePolicy);
                } catch (CMException e) {
                    success = false;
                    LOGGER.log(Level.SEVERE, "Failed to generate a current news sitemap for " + sitePolicy.getName(), e);
                }

                try {
                    generateVideoSitemap((ContentPolicy) sitePolicy);
                } catch (CMException e) {
                    success = false;
                    LOGGER.log(Level.SEVERE, "Failed to generate a current video sitemap for " + sitePolicy.getName(), e);
                }

                // Get all other months sitemap
                try {
                    contentIdList.addAll(getAllMonthSitemap((ContentPolicy) sitePolicy));
                } catch (CMException e) {
                    success = false;
                    LOGGER.log(Level.SEVERE, "Failed to get normal monthly sitemap for " + sitePolicy.getName(), e);
                }

                // Generate index
                generateIndexSitemap((ContentPolicy) sitePolicy, contentIdList);

            } catch (CMException | IOException e) {
                success = false;
                LOGGER.log(Level.SEVERE, "Failed to generate an index sitemap for " + sitePolicy.getName(), e);
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

            } catch (CMException e) {
                success = false;
                LOGGER.log(Level.SEVERE, "Failed to generate a news sitemap for " + sitePolicy.getName(), e);
            }
        }
        return success;
    }

    public void generateOtherMonthNormalSitemap(final Date date) throws CMException {

        final List<Site> sitePolicies = getAllSites();

        for (Site sitePolicy : sitePolicies) {
            // Generate latest month sitemap
            try {
                generateNormalSitemap((ContentPolicy) sitePolicy, date);
            } catch (CMException e) {
                LOGGER.log(Level.SEVERE, "Failed to generate a normal sitemap for a month " + sitePolicy.getName() +
                        " date: " + date, e);
            }
        }
    }

    public List<ContentId> getAllMonthSitemap(final ContentPolicy sitePolicy) throws CMException {

        LOGGER.log(Level.INFO, "Generating all months sitemap for " + sitePolicy.getContentId().getContentIdString());

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

    private Element getSitemap(final ContentPolicy contentPolicy) throws CMException {
        return setupSitemapNodeBuilder(new UrlElementNodeBuilder(), contentPolicy).build();
    }

    private UrlElementNodeBuilder setupSitemapNodeBuilder(final UrlElementNodeBuilder sourceNodeBuilder, final ContentPolicy contentPolicy) throws CMException {
        final UrlElementNodeBuilder nodeBuilder = sourceNodeBuilder
                .url(getCanonicalUrl(contentPolicy))
                .lastModified(contentPolicy.getVersionInfo().getVersionCommitDate());
        final SitemapConfigPolicy config = getConfig();

        if (contentPolicy instanceof Site) {
            return nodeBuilder.priority(Double.parseDouble(config.getPrioritySite()))
                              .changeFrequency(ChangeFrequency.from(config.getChangeFreqSite()));
        } else if (contentPolicy instanceof Page) {
            return nodeBuilder.priority(Double.parseDouble(config.getPriorityPage()))
                              .changeFrequency(ChangeFrequency.from(config.getChangeFreqPage()));
        } else {
            return nodeBuilder.priority(Double.parseDouble(config.getPriorityContent()))
                              .changeFrequency(ChangeFrequency.from(config.getChangeFreqContent()));
        }
    }

    private String getCanonicalUrl(final ContentPolicy contentPolicy) throws CMException {
        return urlBuilder.buildUrl(contentPolicy.getContentId());
    }

    private ContentId saveToFile(final ExternalContentId externalId,
                                 final String name,
                                 final String content,
                                 final ContentId securityParentId) throws CMException {

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
            LOGGER.log(Level.INFO, "Successfully generated sitemap " + externalId);
        } catch (CMException | IOException e) {
            LOGGER.log(Level.SEVERE, "cannot save sitemap " + name, e);
            if (policy != null) {
                try {
                    cmServer.abortContent(policy);
                } catch (CMException e1) {
                    LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
                }
            }
            throw new CMException(e);
        }
        return policy.getContentId();
    }

    // TODO: mnova review this code.
    private String getURLofFile(String domain, SitemapPolicy fileResourcePolicy) {
        if (StringUtil.isEmpty(domain)) {
            domain = "http://localhost:8080";
        }
        return domain + "/polopoly_fs/" + fileResourcePolicy.getContentId().getContentIdString() + "!/" + FILENAME_XML;
    }

    public InputStream getURLofSitemap(final HttpServletRequest request) throws Exception {
        String domain = PolopolyUtil.getRequestDomain(request);

        // TODO: mnova review this code.
        try {
            final ContentPolicy sitePolicy = (ContentPolicy) PolopolyUtil.getSiteFromDomain(domain, cmServer);
            final String requestUri = request.getRequestURI();

            final SitemapType sitemapType;

            if (requestUri.contains("sitemap_index.xml")) {
                sitemapType = SitemapType.Index;
            } else if (requestUri.contains("sitemap_news.xml")) {
                sitemapType = SitemapType.News;
            } else if (requestUri.contains("sitemap_video.xml")) {
                sitemapType = SitemapType.Video;
            } else {
                sitemapType = SitemapType.Index;
            }
            final String externalIdString = getExternalIdStringOfSitemap(
                    sitemapType,
                    sitePolicy.getExternalId()
                              .getExternalId(),
                    null);

            SitemapPolicy policy = (SitemapPolicy) cmServer.getPolicy(new ExternalContentId(externalIdString));

            if (policy != null) {
                return policy.getFileStream(FILENAME_XML);
            }

            LOGGER.log(Level.SEVERE, "Could not get sitemap: " + request.getRequestURL());
            throw new IllegalArgumentException("Could not get sitemap: " + request.getRequestURL());
        } catch (CMException | IOException e) {
            LOGGER.log(Level.SEVERE, "Could not get sitemap: " + request.getRequestURL(), e);
            throw e;
        }
    }

    private SitemapConfigPolicy getConfig() throws CMException {
        if (config == null) {
            config = (SitemapConfigPolicy) cmServer.getPolicy(new ExternalContentId(SitemapConfigPolicy.CONFIG_EXT_ID));
        }
        return config;
    }

    private PolicyCMServer getCMServer() {
        return cmServer;
    }

    private ContentManager getContentManager() {
        return contentManager;
    }

    private SearchClient getSolrClient() {
        return solrClient;
    }

    private NewsArticleSitemapBean fetchNewsArticle(final ContentId contentId, final Subject subject) {
        return fetchNewsArticle(IdUtil.fromPolicyContentId(contentId), subject);
    }

    private NewsArticleSitemapBean fetchNewsArticle(final com.atex.onecms.content.ContentId contentId,
                                                    final Subject subject) {
        return fetch(contentId, NewsArticleSitemapBean.VARIANT_NAME, NewsArticleSitemapBean.class, subject);
        /*
        final ContentManager contentManager = getContentManager();
        ContentVersionId versionedId = contentManager.resolve(contentId, subject);
        NewsArticleSitemapBean info = null;
        if (versionedId != null) {
            ContentResult<NewsArticleSitemapBean> result = contentManager.get(versionedId,
                    NewsArticleSitemapBean.VARIANT_NAME,
                    NewsArticleSitemapBean.class,
                    Collections.emptyMap(),
                    subject);
            if (result.getStatus().isSuccess()) {
                info = result.getContent().getContentData();
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                            String.format(
                                    "Tried to fetch %s with variant %s but ContentManager result was %d.",
                                    IdUtil.toVersionedIdString(versionedId),
                                    NewsArticleSitemapBean.VARIANT_NAME,
                                    result.getStatus().getDetailCode()));
                }
            }
        } else {
            LOGGER.log(Level.WARNING, "Could not resolve to versioned id: "
                    + IdUtil.toIdString(contentId));
        }
        return info;
        */
    }

    private VideoSitemapBean fetchVideo(final ContentId contentId, final Subject subject) {
        return fetchVideo(IdUtil.fromPolicyContentId(contentId), subject);
    }

    private VideoSitemapBean fetchVideo(final com.atex.onecms.content.ContentId contentId,
                                        final Subject subject) {
        return fetch(contentId, VideoSitemapBean.VARIANT_NAME, VideoSitemapBean.class, subject);
    }

    private <T> T fetch(final com.atex.onecms.content.ContentId contentId,
                        final String variantName,
                        final Class<T> variantClass,
                        final Subject subject) {

        final ContentManager contentManager = getContentManager();
        final ContentVersionId versionedId = contentManager.resolve(contentId, subject);
        if (versionedId != null) {
            final ContentResult<T> result = contentManager.get(versionedId,
                    variantName,
                    variantClass,
                    Collections.emptyMap(),
                    subject);
            if (result.getStatus().isSuccess()) {
                return result.getContent().getContentData();
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                            String.format(
                                    "Tried to fetch %s with variant %s but ContentManager result was %d.",
                                    IdUtil.toVersionedIdString(versionedId),
                                    variantName,
                                    result.getStatus().getDetailCode()));
                }
            }
        } else {
            LOGGER.log(Level.WARNING, "Could not resolve to versioned id: "
                    + IdUtil.toIdString(contentId));
        }
        return null;

    }

    enum SitemapType {
        Index,
        Normal,
        Department,
        News,
        Video
    }

}
