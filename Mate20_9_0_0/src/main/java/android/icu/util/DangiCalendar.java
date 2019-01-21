package android.icu.util;

import android.icu.util.ULocale.Category;
import java.util.Date;

@Deprecated
public class DangiCalendar extends ChineseCalendar {
    private static final int DANGI_EPOCH_YEAR = -2332;
    private static final TimeZone KOREA_ZONE;
    private static final long serialVersionUID = 8156297445349501985L;

    static {
        InitialTimeZoneRule initialTimeZone = new InitialTimeZoneRule("GMT+8", 28800000, 0);
        long[] millis1898 = new long[]{-2270592000000L};
        long[] millis1912 = new long[]{-1829088000000L};
        TimeZoneRule timeArrayTimeZoneRule = new TimeArrayTimeZoneRule("Korean 1897", 25200000, 0, new long[]{-2302128000000L}, 1);
        TimeZoneRule timeArrayTimeZoneRule2 = new TimeArrayTimeZoneRule("Korean 1898-1911", 28800000, 0, millis1898, 1);
        TimeZoneRule timeArrayTimeZoneRule3 = new TimeArrayTimeZoneRule("Korean 1912-", 32400000, 0, millis1912, 1);
        RuleBasedTimeZone tz = new RuleBasedTimeZone("KOREA_ZONE", initialTimeZone);
        tz.addTransitionRule(timeArrayTimeZoneRule);
        tz.addTransitionRule(timeArrayTimeZoneRule2);
        tz.addTransitionRule(timeArrayTimeZoneRule3);
        tz.freeze();
        KOREA_ZONE = tz;
    }

    @Deprecated
    public DangiCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(Category.FORMAT));
    }

    @Deprecated
    public DangiCalendar(Date date) {
        this(TimeZone.getDefault(), ULocale.getDefault(Category.FORMAT));
        setTime(date);
    }

    @Deprecated
    public DangiCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale, (int) DANGI_EPOCH_YEAR, KOREA_ZONE);
    }

    @Deprecated
    public String getType() {
        return "dangi";
    }
}
