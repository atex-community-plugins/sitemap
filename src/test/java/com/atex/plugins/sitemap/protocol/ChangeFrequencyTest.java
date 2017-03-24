package com.atex.plugins.sitemap.protocol;

import java.util.Date;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * ChangeFrequencyTest
 *
 * @author mnova
 */
public class ChangeFrequencyTest {

    @Test
    public void testValidValues() {
        for (final ChangeFrequency cf : ChangeFrequency.values()) {
            Assert.assertEquals(cf, ChangeFrequency.from(cf.name()));
        }
    }

    @Test
    public void testValidValuesLowercase() {
        for (final ChangeFrequency cf : ChangeFrequency.values()) {
            Assert.assertEquals(cf, ChangeFrequency.from(cf.name().toLowerCase()));
        }
    }

    @Test
    public void testValidValuesCustom() {
        Assert.assertEquals(ChangeFrequency.HOURLY, ChangeFrequency.from("HOURLY"));
        Assert.assertEquals(ChangeFrequency.ALWAYS, ChangeFrequency.from("ALWAYS"));
        Assert.assertEquals(ChangeFrequency.NEVER, ChangeFrequency.from("NEVER"));
        Assert.assertEquals(ChangeFrequency.MONTHLY, ChangeFrequency.from("MONTHLY"));
    }

    @Test
    public void testInvalidValidValues() {
        Random rnd = new Random(new Date().getTime());
        final int howmuch = (rnd.nextInt(10) + 5);
        for (int idx = 0; idx < howmuch; idx++) {
            final String value = RandomStringUtils.random(10);
            Assert.assertEquals(null, ChangeFrequency.from(value));
        }
    }

}