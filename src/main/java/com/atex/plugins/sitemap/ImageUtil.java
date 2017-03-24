package com.atex.plugins.sitemap;

import java.util.logging.Logger;

import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.ContentVersionId;
import com.atex.onecms.content.Subject;
import com.atex.onecms.content.aspects.Aspect;
import com.atex.onecms.image.ImageInfoAspectBean;
import com.atex.onecms.ws.image.ImageServiceConfigurationProvider;
import com.atex.onecms.ws.image.ImageServiceUrlBuilder;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.util.StringUtil;

/**
 * ImageUtil
 *
 * @author mnova
 */
public class ImageUtil {

    private static final Logger LOGGER = Logger.getLogger(ImageUtil.class.getName());

    private final PolicyCMServer cmServer;
    private final ContentManager contentManager;

    public ImageUtil(final PolicyCMServer cmServer,
                     final ContentManager contentManager) {
        this.cmServer = cmServer;
        this.contentManager = contentManager;
    }

    public ImageServiceUrlBuilder getUrlBuilder(final ContentId contentId) {
        final ContentVersionId versionId = contentManager.resolve(contentId, Subject.NOBODY_CALLER);
        final ContentResult<ImageInfoAspectBean> result = contentManager.get(
                versionId,
                null,
                ImageInfoAspectBean.class,
                null,
                Subject.NOBODY_CALLER);
        if (result != null && result.getStatus().isSuccess()) {
            final Aspect<ImageInfoAspectBean> imageInfo = result.getContent().getAspect(ImageInfoAspectBean.ASPECT_NAME);
            if (imageInfo != null && imageInfo.getData() != null && !StringUtil.isEmpty(imageInfo.getData().getFilePath())) {
                return getUrlBuilder(result);
            }
        }
        return null;
    }

    public <T> ImageServiceUrlBuilder getUrlBuilder(final ContentResult<T> result) {
        return new ImageServiceUrlBuilder(result, getSecret());
    }

    private String getSecret() {
        try {
            return new ImageServiceConfigurationProvider(cmServer)
                    .getImageServiceConfiguration()
                    .getSecret();
        } catch (CMException e) {
            throw new RuntimeException("Secret not found", e);
        }
    }
}
