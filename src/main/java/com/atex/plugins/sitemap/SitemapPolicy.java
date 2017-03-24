package com.atex.plugins.sitemap;

import com.atex.onecms.content.AspectedPolicy;
import com.atex.onecms.content.metadata.MetadataInfo;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CMRuntimeException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.metadata.Metadata;
import com.polopoly.metadata.MetadataAware;
import com.polopoly.metadata.util.MetadataUtil;


public class SitemapPolicy extends AspectedPolicy<SitemapContentDataBean> implements MetadataAware {

    public static final String INPUT_TEMPLATE = "com.atex.plugins.sitemap.Sitemap";

    public SitemapPolicy(final CmClient cmClient, final Application application) throws IllegalApplicationStateException {
        super(cmClient, application);
    }

    @Override
    public Metadata getMetadata()  {
      try {
        MetadataInfo metadata = (MetadataInfo) getAspect("atex.Metadata");
        if (metadata != null) {
          return metadata.getMetadata();
        }
      } catch (CMException e) {
      }

      return new Metadata();
    }

    @Override
    public void setMetadata(final Metadata metadata) {
      try {
        MetadataInfo metadataInfo= new MetadataInfo();
        metadataInfo.setMetadata(metadata);
        metadataInfo.setTaxonomyIds(MetadataUtil.getTaxonomyIds(this));
        setAspect("atex.Metadata", metadataInfo);
      } catch (CMException e) {
        throw new CMRuntimeException("Unable to set metadata", e);
      }
    }

}
