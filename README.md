sitemap-plugin
============

Plugin for sitemap stored in Polopoly.
The code for the sitemap has been originally developed by Surasin, later modified by Imran and Marco took the code and made a configurable plugin.

## Servlet Configuration

You will need to modify the webapp-dispatcher web.xml:

```
  <servlet>
    <servlet-name>sitemapGenerator</servlet-name>
    <servlet-class>com.atex.plugins.sitemap.SitemapGeneratorServlet</servlet-class>
  </servlet>

  <servlet>
    <servlet-name>sitemapRead</servlet-name>
    <servlet-class>com.atex.plugins.sitemap.SitemapReadServlet</servlet-class>
  </servlet>

  <servlet-mapping>
    <servlet-name>sitemapGenerator</servlet-name>
    <url-pattern>/sitemapGenerator</url-pattern>
  </servlet-mapping>

  <servlet-mapping>
    <servlet-name>sitemapRead</servlet-name>
    <url-pattern>/sitemap.xml</url-pattern>
  </servlet-mapping>

  <servlet-mapping>
    <servlet-name>sitemapRead</servlet-name>
    <url-pattern>/sitemap_news.xml</url-pattern>
  </servlet-mapping>

  <servlet-mapping>
    <servlet-name>sitemapRead</servlet-name>
    <url-pattern>/sitemap_video.xml</url-pattern>
  </servlet-mapping>
```

and the webapp-front web.xml:

```
  <servlet>
    <servlet-name>sitemapRead</servlet-name>
    <servlet-class>com.atex.plugins.sitemap.SitemapReadServlet</servlet-class>
  </servlet>

  <servlet-mapping>
    <servlet-name>sitemapRead</servlet-name>
    <url-pattern>/sitemap.xml</url-pattern>
  </servlet-mapping>

  <servlet-mapping>
    <servlet-name>sitemapRead</servlet-name>
    <url-pattern>/sitemap_news.xml</url-pattern>
  </servlet-mapping>

  <servlet-mapping>
    <servlet-name>sitemapRead</servlet-name>
    <url-pattern>/sitemap_video.xml</url-pattern>
  </servlet-mapping>
```

## Plugin Configuration

In your project you may want to change the default configuration by creating the file plugins.com.atex.plugins.sitemap.Config.content with the following content:

```
id:.
major:appconfig
inputtemplate:com.atex.plugins.sitemap.Configuration
securityparent:p.siteengine.Configuration.d
component:polopoly.Content:name:Sitemap Configuration
component:user:value:sysadmin
component:password:value:sysadmin
#component:sites:value:PolopolyPost.d
component:priority_site:value:1.0
component:priority_page:value:0.5
component:priority_content:value:0.8
component:changefreq_site:value:hourly
component:changefreq_page:value:hourly
component:changefreq_content:value:monthly
component:article_it:value:standard.Article
component:gallery_it:value:com.atex.plugins.image-gallery.MainElement
component:video_it:value:com.atex.plugins.video.MainElement
```

The components article_it, gallery_it and video_it are a comma separated list of inputtemplates.
The component sites is a comma separated list of site ids, if empty all the sites will be processed.

## Generate sitemaps

In order to create sitemaps you have to execute commands like:

curl http://<ip gui>:<port gui>/sitemapGenerator?date=yyyy-mm-dd
curl http://<ip gui>:<port gui>/sitemapGenerator

the first one will generate the article sitemaps (starting from the first day of the month till the end of the month).
the second one will generate the sitemap.xml which will contains links to the article sitemaps.

So if you want to reduce the load on the system, especially when you already have a huge collection of articles, you may need to schedule the generations in off peak hours.

To generate the urls to be invoke you can use the script generateUrls.sh in this way:

```
./generateUrls.sh 'curl http://localhost:8080/sitemapGenerator' 2013-01
curl http://localhost:8080/sitemapGenerator?date=2013-01-01
curl http://localhost:8080/sitemapGenerator?date=2013-02-01
curl http://localhost:8080/sitemapGenerator?date=2013-03-01
curl http://localhost:8080/sitemapGenerator?date=2013-04-01
curl http://localhost:8080/sitemapGenerator?date=2013-05-01
curl http://localhost:8080/sitemapGenerator?date=2013-06-01
curl http://localhost:8080/sitemapGenerator?date=2013-07-01
curl http://localhost:8080/sitemapGenerator?date=2013-08-01
curl http://localhost:8080/sitemapGenerator?date=2013-09-01
curl http://localhost:8080/sitemapGenerator?date=2013-10-01
curl http://localhost:8080/sitemapGenerator?date=2013-11-01
curl http://localhost:8080/sitemapGenerator?date=2013-12-01
curl http://localhost:8080/sitemapGenerator?date=2014-01-01
curl http://localhost:8080/sitemapGenerator?date=2014-02-01
curl http://localhost:8080/sitemapGenerator?date=2014-03-01
curl http://localhost:8080/sitemapGenerator?date=2014-04-01
curl http://localhost:8080/sitemapGenerator?date=2014-05-01
curl http://localhost:8080/sitemapGenerator?date=2014-06-01
curl http://localhost:8080/sitemapGenerator?date=2014-07-01
curl http://localhost:8080/sitemapGenerator?date=2014-08-01
curl http://localhost:8080/sitemapGenerator?date=2014-09-01
curl http://localhost:8080/sitemapGenerator?date=2014-10-01
curl http://localhost:8080/sitemapGenerator?date=2014-11-01
curl http://localhost:8080/sitemapGenerator?date=2014-12-01
curl http://localhost:8080/sitemapGenerator?date=2015-01-01
curl http://localhost:8080/sitemapGenerator?date=2015-02-01
curl http://localhost:8080/sitemapGenerator?date=2015-03-01
curl http://localhost:8080/sitemapGenerator?date=2015-04-01
curl http://localhost:8080/sitemapGenerator?date=2015-05-01
curl http://localhost:8080/sitemapGenerator?date=2015-06-01
curl http://localhost:8080/sitemapGenerator?date=2015-07-01
curl http://localhost:8080/sitemapGenerator?date=2015-08-01
curl http://localhost:8080/sitemapGenerator?date=2015-09-01
curl http://localhost:8080/sitemapGenerator?date=2015-10-01
curl http://localhost:8080/sitemapGenerator?date=2015-11-01
```

You can schedule those operations in a nightly maintenance window, when you have generated all the necessary sitemaps you can ask the system to generate the index sitemap:

```
curl http://localhost:8080/sitemapGenerator
```

Than you can setup a crontab with something like this:

```
20 3,5 * * * /path/to/my/script/dailySitemap.sh 'curl http://localhost:8080/sitemapGenerator' | /bin/bash
```
