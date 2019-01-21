package java.util;

import android.icu.text.TimeZoneNames;
import android.icu.text.TimeZoneNames.NameType;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.Locale.Category;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import libcore.util.ZoneInfoDB;
import org.apache.harmony.luni.internal.util.TimezoneGetter;

public abstract class TimeZone implements Serializable, Cloneable {
    private static final TimeZone GMT = new SimpleTimeZone(0, "GMT");
    public static final int LONG = 1;
    static final TimeZone NO_TIMEZONE = null;
    public static final int SHORT = 0;
    private static final TimeZone UTC = new SimpleTimeZone(0, "UTC");
    private static volatile TimeZone defaultTimeZone = null;
    static final long serialVersionUID = 3581463369166924961L;
    private String ID;

    private static class NoImagePreloadHolder {
        public static final Pattern CUSTOM_ZONE_ID_PATTERN = Pattern.compile("^GMT[-+](\\d{1,2})(:?(\\d\\d))?$");

        private NoImagePreloadHolder() {
        }
    }

    private static native String getSystemGMTOffsetID();

    private static native String getSystemTimeZoneID(String str, String str2);

    public abstract int getOffset(int i, int i2, int i3, int i4, int i5, int i6);

    public abstract int getRawOffset();

    public abstract boolean inDaylightTime(Date date);

    public abstract void setRawOffset(int i);

    public abstract boolean useDaylightTime();

    public int getOffset(long date) {
        if (inDaylightTime(new Date(date))) {
            return getRawOffset() + getDSTSavings();
        }
        return getRawOffset();
    }

    int getOffsets(long date, int[] offsets) {
        int rawoffset = getRawOffset();
        int dstoffset = 0;
        if (inDaylightTime(new Date(date))) {
            dstoffset = getDSTSavings();
        }
        if (offsets != null) {
            offsets[0] = rawoffset;
            offsets[1] = dstoffset;
        }
        return rawoffset + dstoffset;
    }

    public String getID() {
        return this.ID;
    }

    public void setID(String ID) {
        if (ID != null) {
            this.ID = ID;
            return;
        }
        throw new NullPointerException();
    }

    public final String getDisplayName() {
        return getDisplayName(false, 1, Locale.getDefault(Category.DISPLAY));
    }

    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, 1, locale);
    }

    public final String getDisplayName(boolean daylight, int style) {
        return getDisplayName(daylight, style, Locale.getDefault(Category.DISPLAY));
    }

    public String getDisplayName(boolean daylightTime, int style, Locale locale) {
        NameType nameType;
        switch (style) {
            case 0:
                if (!daylightTime) {
                    nameType = NameType.SHORT_STANDARD;
                    break;
                }
                nameType = NameType.SHORT_DAYLIGHT;
                break;
            case 1:
                if (!daylightTime) {
                    nameType = NameType.LONG_STANDARD;
                    break;
                }
                nameType = NameType.LONG_DAYLIGHT;
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal style: ");
                stringBuilder.append(style);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
        String canonicalID = android.icu.util.TimeZone.getCanonicalID(getID());
        if (canonicalID != null) {
            String displayName = TimeZoneNames.getInstance(locale).getDisplayName(canonicalID, nameType, System.currentTimeMillis());
            if (displayName != null) {
                return displayName;
            }
        }
        int offsetMillis = getRawOffset();
        if (daylightTime) {
            offsetMillis += getDSTSavings();
        }
        return createGmtOffsetString(true, true, offsetMillis);
    }

    public static String createGmtOffsetString(boolean includeGmt, boolean includeMinuteSeparator, int offsetMillis) {
        int offsetMinutes = offsetMillis / 60000;
        char sign = '+';
        if (offsetMinutes < 0) {
            sign = '-';
            offsetMinutes = -offsetMinutes;
        }
        StringBuilder builder = new StringBuilder(9);
        if (includeGmt) {
            builder.append("GMT");
        }
        builder.append(sign);
        appendNumber(builder, 2, offsetMinutes / 60);
        if (includeMinuteSeparator) {
            builder.append(':');
        }
        appendNumber(builder, 2, offsetMinutes % 60);
        return builder.toString();
    }

    private static void appendNumber(StringBuilder builder, int count, int value) {
        String string = Integer.toString(value);
        for (int i = 0; i < count - string.length(); i++) {
            builder.append('0');
        }
        builder.append(string);
    }

    public int getDSTSavings() {
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }

    public boolean observesDaylightTime() {
        return useDaylightTime() || inDaylightTime(new Date());
    }

    /* JADX WARNING: Missing block: B:33:0x005e, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized TimeZone getTimeZone(String id) {
        synchronized (TimeZone.class) {
            if (id != null) {
                TimeZone timeZone;
                if (id.length() == 3) {
                    if (id.equals("GMT")) {
                        timeZone = (TimeZone) GMT.clone();
                        return timeZone;
                    } else if (id.equals("UTC")) {
                        timeZone = (TimeZone) UTC.clone();
                        return timeZone;
                    }
                }
                timeZone = null;
                try {
                    timeZone = ZoneInfoDB.getInstance().makeTimeZone(id);
                } catch (IOException e) {
                }
                if (timeZone == null) {
                    if (id.length() > 3 && id.startsWith("GMT")) {
                        timeZone = getCustomTimeZone(id);
                    }
                }
                TimeZone timeZone2 = timeZone != null ? timeZone : (TimeZone) GMT.clone();
            } else {
                throw new NullPointerException("id == null");
            }
        }
    }

    public static TimeZone getTimeZone(ZoneId zoneId) {
        String tzid = zoneId.getId();
        char c = tzid.charAt(0);
        if (c == '+' || c == '-') {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GMT");
            stringBuilder.append(tzid);
            tzid = stringBuilder.toString();
        } else if (c == 'Z' && tzid.length() == 1) {
            tzid = "UTC";
        }
        return getTimeZone(tzid);
    }

    public ZoneId toZoneId() {
        return ZoneId.of(getID(), ZoneId.SHORT_IDS);
    }

    private static TimeZone getCustomTimeZone(String id) {
        Matcher m = NoImagePreloadHolder.CUSTOM_ZONE_ID_PATTERN.matcher(id);
        if (!m.matches()) {
            return null;
        }
        int minute = 0;
        try {
            int hour = Integer.parseInt(m.group(1));
            if (m.group(3) != null) {
                minute = Integer.parseInt(m.group(3));
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            int raw = (3600000 * hour) + (60000 * minute);
            if (id.charAt(3) == '-') {
                raw = -raw;
            }
            return new SimpleTimeZone(raw, String.format(Locale.ROOT, "GMT%c%02d:%02d", Character.valueOf(id.charAt(3)), Integer.valueOf(hour), Integer.valueOf(minute)));
        } catch (NumberFormatException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static synchronized String[] getAvailableIDs(int rawOffset) {
        String[] availableIDs;
        synchronized (TimeZone.class) {
            availableIDs = ZoneInfoDB.getInstance().getAvailableIDs(rawOffset);
        }
        return availableIDs;
    }

    public static synchronized String[] getAvailableIDs() {
        String[] availableIDs;
        synchronized (TimeZone.class) {
            availableIDs = ZoneInfoDB.getInstance().getAvailableIDs();
        }
        return availableIDs;
    }

    public static TimeZone getDefault() {
        return (TimeZone) getDefaultRef().clone();
    }

    static synchronized TimeZone getDefaultRef() {
        TimeZone timeZone;
        synchronized (TimeZone.class) {
            if (defaultTimeZone == null) {
                TimezoneGetter tzGetter = TimezoneGetter.getInstance();
                String zoneName = tzGetter != null ? tzGetter.getId() : null;
                if (zoneName != null) {
                    zoneName = zoneName.trim();
                }
                if (zoneName == null || zoneName.isEmpty()) {
                    try {
                        zoneName = IoUtils.readFileAsString("/etc/timezone");
                    } catch (IOException e) {
                        zoneName = "GMT";
                    }
                }
                defaultTimeZone = getTimeZone(zoneName);
            }
            timeZone = defaultTimeZone;
        }
        return timeZone;
    }

    public static synchronized void setDefault(TimeZone timeZone) {
        synchronized (TimeZone.class) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new PropertyPermission("user.timezone", "write"));
            }
            defaultTimeZone = timeZone != null ? (TimeZone) timeZone.clone() : null;
            android.icu.util.TimeZone.setICUDefault(null);
        }
    }

    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset() && useDaylightTime() == other.useDaylightTime();
    }

    public Object clone() {
        try {
            TimeZone other = (TimeZone) super.clone();
            other.ID = this.ID;
            return other;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
