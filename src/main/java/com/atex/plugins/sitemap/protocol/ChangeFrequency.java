package com.atex.plugins.sitemap.protocol;

import com.polopoly.util.StringUtil;

/**
 * ChangeFrequency, see https://www.sitemaps.org/protocol.html for details on these values.
 *
 * @author mnova
 */
public enum ChangeFrequency {

    ALWAYS,

    HOURLY,

    DAILY,

    WEEKLY,

    MONTHLY,

    YEARLY,

    NEVER;

    public static ChangeFrequency from(final String value) {
        for (final ChangeFrequency cf : ChangeFrequency.values()) {
            if (StringUtil.equalsIgnoreCase(cf.name(), value)) {
                return cf;
            }
        }
        return null;
    }
}
