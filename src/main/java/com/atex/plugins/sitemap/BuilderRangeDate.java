package com.atex.plugins.sitemap;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * BuilderRangeDate.
 *
 * @author mnova
 */
public class BuilderRangeDate {

    private static final ThreadLocal<SimpleDateFormat> FROM_DATE_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        /**
         * See {@link ThreadLocal#initialValue()}
         * @return a not null value.
         */
        @Override
        protected SimpleDateFormat initialValue() {
            final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T00:00:00Z'", Locale.getDefault());
            fmt.setTimeZone(TimeZone.getDefault());
            return fmt;
        }

    };

    private static final ThreadLocal<SimpleDateFormat> TO_DATE_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        /**
         * See {@link ThreadLocal#initialValue()}
         * @return a not null value.
         */
        @Override
        protected SimpleDateFormat initialValue() {
            final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T23:59:59Z'", Locale.getDefault());
            fmt.setTimeZone(TimeZone.getDefault());
            return fmt;
        }

    };

    public static String createQuery(Date date, String field_date) {
        return createQuery(date, date, field_date);
    }

    public static String createQuery(Date fromDate, Date toDate, String field_date) {
        String fromDateString = "";
        String toDateString = "";
        if (fromDate != null && toDate != null) {
            fromDateString = FROM_DATE_FORMATTER.get().format(fromDate);
            toDateString = TO_DATE_FORMATTER.get().format(toDate);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            fromDateString = FROM_DATE_FORMATTER.get().format(cal.getTime());
            toDateString = "*";
        }

        return "(" + getPartialQuery(fromDateString, toDateString, field_date) + ")";
    }

    protected static String getPartialQuery(String fromDateString, String toDateString, String indexField) {
        StringBuilder sb = new StringBuilder();
        sb.append(indexField).append(":");
        sb.append("[").append(fromDateString).append(" TO ").append(toDateString).append("]");

        return sb.toString();
    }

}
