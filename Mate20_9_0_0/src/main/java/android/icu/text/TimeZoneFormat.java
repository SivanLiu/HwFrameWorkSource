package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SoftCache;
import android.icu.impl.TZDBTimeZoneNames;
import android.icu.impl.TextTrieMap;
import android.icu.impl.TimeZoneGenericNames;
import android.icu.impl.TimeZoneGenericNames.GenericMatchInfo;
import android.icu.impl.TimeZoneGenericNames.GenericNameType;
import android.icu.impl.TimeZoneNamesImpl;
import android.icu.impl.ZoneMeta;
import android.icu.lang.UCharacter;
import android.icu.text.DateFormat.Field;
import android.icu.text.TimeZoneNames.MatchInfo;
import android.icu.text.TimeZoneNames.NameType;
import android.icu.util.Calendar;
import android.icu.util.Freezable;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.TimeZone.SystemTimeZoneType;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

public class TimeZoneFormat extends UFormat implements Freezable<TimeZoneFormat>, Serializable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final EnumSet<GenericNameType> ALL_GENERIC_NAME_TYPES = EnumSet.of(GenericNameType.LOCATION, GenericNameType.LONG, GenericNameType.SHORT);
    private static final EnumSet<NameType> ALL_SIMPLE_NAME_TYPES = EnumSet.of(NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT, NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT, NameType.EXEMPLAR_LOCATION);
    private static final String[] ALT_GMT_STRINGS = new String[]{DEFAULT_GMT_ZERO, "UTC", "UT"};
    private static final String ASCII_DIGITS = "0123456789";
    private static final String[] DEFAULT_GMT_DIGITS = new String[]{AndroidHardcodedSystemProperties.JAVA_VERSION, "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final char DEFAULT_GMT_OFFSET_SEP = ':';
    private static final String DEFAULT_GMT_PATTERN = "GMT{0}";
    private static final String DEFAULT_GMT_ZERO = "GMT";
    private static final String ISO8601_UTC = "Z";
    private static final int ISO_LOCAL_STYLE_FLAG = 256;
    private static final int ISO_Z_STYLE_FLAG = 128;
    private static final int MAX_OFFSET = 86400000;
    private static final int MAX_OFFSET_HOUR = 23;
    private static final int MAX_OFFSET_MINUTE = 59;
    private static final int MAX_OFFSET_SECOND = 59;
    private static final int MILLIS_PER_HOUR = 3600000;
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int MILLIS_PER_SECOND = 1000;
    private static final GMTOffsetPatternType[] PARSE_GMT_OFFSET_TYPES = new GMTOffsetPatternType[]{GMTOffsetPatternType.POSITIVE_HMS, GMTOffsetPatternType.NEGATIVE_HMS, GMTOffsetPatternType.POSITIVE_HM, GMTOffsetPatternType.NEGATIVE_HM, GMTOffsetPatternType.POSITIVE_H, GMTOffsetPatternType.NEGATIVE_H};
    private static volatile TextTrieMap<String> SHORT_ZONE_ID_TRIE = null;
    private static final String TZID_GMT = "Etc/GMT";
    private static final String UNKNOWN_LOCATION = "Unknown";
    private static final int UNKNOWN_OFFSET = Integer.MAX_VALUE;
    private static final String UNKNOWN_SHORT_ZONE_ID = "unk";
    private static final String UNKNOWN_ZONE_ID = "Etc/Unknown";
    private static volatile TextTrieMap<String> ZONE_ID_TRIE = null;
    private static TimeZoneFormatCache _tzfCache = new TimeZoneFormatCache();
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("_locale", ULocale.class), new ObjectStreamField("_tznames", TimeZoneNames.class), new ObjectStreamField("_gmtPattern", String.class), new ObjectStreamField("_gmtOffsetPatterns", String[].class), new ObjectStreamField("_gmtOffsetDigits", String[].class), new ObjectStreamField("_gmtZeroFormat", String.class), new ObjectStreamField("_parseAllStyles", Boolean.TYPE)};
    private static final long serialVersionUID = 2281246852693575022L;
    private transient boolean _abuttingOffsetHoursAndMinutes;
    private volatile transient boolean _frozen;
    private String[] _gmtOffsetDigits;
    private transient Object[][] _gmtOffsetPatternItems;
    private String[] _gmtOffsetPatterns;
    private String _gmtPattern;
    private transient String _gmtPatternPrefix;
    private transient String _gmtPatternSuffix;
    private String _gmtZeroFormat = DEFAULT_GMT_ZERO;
    private volatile transient TimeZoneGenericNames _gnames;
    private ULocale _locale;
    private boolean _parseAllStyles;
    private boolean _parseTZDBNames;
    private transient String _region;
    private volatile transient TimeZoneNames _tzdbNames;
    private TimeZoneNames _tznames;

    private static class GMTOffsetField {
        final char _type;
        final int _width;

        GMTOffsetField(char type, int width) {
            this._type = type;
            this._width = width;
        }

        char getType() {
            return this._type;
        }

        int getWidth() {
            return this._width;
        }

        static boolean isValid(char type, int width) {
            return width == 1 || width == 2;
        }
    }

    public enum GMTOffsetPatternType {
        POSITIVE_HM("+H:mm", DateFormat.HOUR24_MINUTE, true),
        POSITIVE_HMS("+H:mm:ss", DateFormat.HOUR24_MINUTE_SECOND, true),
        NEGATIVE_HM("-H:mm", DateFormat.HOUR24_MINUTE, false),
        NEGATIVE_HMS("-H:mm:ss", DateFormat.HOUR24_MINUTE_SECOND, false),
        POSITIVE_H("+H", DateFormat.HOUR24, true),
        NEGATIVE_H("-H", DateFormat.HOUR24, false);
        
        private String _defaultPattern;
        private boolean _isPositive;
        private String _required;

        private GMTOffsetPatternType(String defaultPattern, String required, boolean isPositive) {
            this._defaultPattern = defaultPattern;
            this._required = required;
            this._isPositive = isPositive;
        }

        private String defaultPattern() {
            return this._defaultPattern;
        }

        private String required() {
            return this._required;
        }

        private boolean isPositive() {
            return this._isPositive;
        }
    }

    private enum OffsetFields {
        H,
        HM,
        HMS
    }

    public enum ParseOption {
        ALL_STYLES,
        TZ_DATABASE_ABBREVIATIONS
    }

    public enum Style {
        GENERIC_LOCATION(1),
        GENERIC_LONG(2),
        GENERIC_SHORT(4),
        SPECIFIC_LONG(8),
        SPECIFIC_SHORT(16),
        LOCALIZED_GMT(32),
        LOCALIZED_GMT_SHORT(64),
        ISO_BASIC_SHORT(128),
        ISO_BASIC_LOCAL_SHORT(256),
        ISO_BASIC_FIXED(128),
        ISO_BASIC_LOCAL_FIXED(256),
        ISO_BASIC_FULL(128),
        ISO_BASIC_LOCAL_FULL(256),
        ISO_EXTENDED_FIXED(128),
        ISO_EXTENDED_LOCAL_FIXED(256),
        ISO_EXTENDED_FULL(128),
        ISO_EXTENDED_LOCAL_FULL(256),
        ZONE_ID(512),
        ZONE_ID_SHORT(1024),
        EXEMPLAR_LOCATION(2048);
        
        final int flag;

        private Style(int flag) {
            this.flag = flag;
        }
    }

    public enum TimeType {
        UNKNOWN,
        STANDARD,
        DAYLIGHT
    }

    private static class TimeZoneFormatCache extends SoftCache<ULocale, TimeZoneFormat, ULocale> {
        private TimeZoneFormatCache() {
        }

        protected TimeZoneFormat createInstance(ULocale key, ULocale data) {
            TimeZoneFormat fmt = new TimeZoneFormat(data);
            fmt.freeze();
            return fmt;
        }
    }

    protected TimeZoneFormat(ULocale locale) {
        this._locale = locale;
        this._tznames = TimeZoneNames.getInstance(locale);
        String gmtPattern = null;
        String hourFormats = null;
        try {
            ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_ZONE_BASE_NAME, locale);
            try {
                gmtPattern = bundle.getStringWithFallback("zoneStrings/gmtFormat");
            } catch (MissingResourceException e) {
            }
            try {
                hourFormats = bundle.getStringWithFallback("zoneStrings/hourFormat");
            } catch (MissingResourceException e2) {
            }
            try {
                this._gmtZeroFormat = bundle.getStringWithFallback("zoneStrings/gmtZeroFormat");
            } catch (MissingResourceException e3) {
            }
        } catch (MissingResourceException e4) {
        }
        if (gmtPattern == null) {
            gmtPattern = DEFAULT_GMT_PATTERN;
        }
        initGMTPattern(gmtPattern);
        String[] gmtOffsetPatterns = new String[GMTOffsetPatternType.values().length];
        int i = 0;
        if (hourFormats != null) {
            String[] hourPatterns = hourFormats.split(";", 2);
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_H.ordinal()] = truncateOffsetPattern(hourPatterns[0]);
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HM.ordinal()] = hourPatterns[0];
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HMS.ordinal()] = expandOffsetPattern(hourPatterns[0]);
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_H.ordinal()] = truncateOffsetPattern(hourPatterns[1]);
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HM.ordinal()] = hourPatterns[1];
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()] = expandOffsetPattern(hourPatterns[1]);
        } else {
            GMTOffsetPatternType[] values = GMTOffsetPatternType.values();
            int length = values.length;
            while (i < length) {
                GMTOffsetPatternType patType = values[i];
                gmtOffsetPatterns[patType.ordinal()] = patType.defaultPattern();
                i++;
            }
        }
        initGMTOffsetPatterns(gmtOffsetPatterns);
        this._gmtOffsetDigits = DEFAULT_GMT_DIGITS;
        NumberingSystem ns = NumberingSystem.getInstance(locale);
        if (!ns.isAlgorithmic()) {
            this._gmtOffsetDigits = toCodePoints(ns.getDescription());
        }
    }

    public static TimeZoneFormat getInstance(ULocale locale) {
        if (locale != null) {
            return (TimeZoneFormat) _tzfCache.getInstance(locale, locale);
        }
        throw new NullPointerException("locale is null");
    }

    public static TimeZoneFormat getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    public TimeZoneNames getTimeZoneNames() {
        return this._tznames;
    }

    private TimeZoneGenericNames getTimeZoneGenericNames() {
        if (this._gnames == null) {
            synchronized (this) {
                if (this._gnames == null) {
                    this._gnames = TimeZoneGenericNames.getInstance(this._locale);
                }
            }
        }
        return this._gnames;
    }

    private TimeZoneNames getTZDBTimeZoneNames() {
        if (this._tzdbNames == null) {
            synchronized (this) {
                if (this._tzdbNames == null) {
                    this._tzdbNames = new TZDBTimeZoneNames(this._locale);
                }
            }
        }
        return this._tzdbNames;
    }

    public TimeZoneFormat setTimeZoneNames(TimeZoneNames tznames) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        this._tznames = tznames;
        this._gnames = new TimeZoneGenericNames(this._locale, this._tznames);
        return this;
    }

    public String getGMTPattern() {
        return this._gmtPattern;
    }

    public TimeZoneFormat setGMTPattern(String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        initGMTPattern(pattern);
        return this;
    }

    public String getGMTOffsetPattern(GMTOffsetPatternType type) {
        return this._gmtOffsetPatterns[type.ordinal()];
    }

    public TimeZoneFormat setGMTOffsetPattern(GMTOffsetPatternType type, String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        } else if (pattern != null) {
            Object[] parsedItems = parseOffsetPattern(pattern, type.required());
            this._gmtOffsetPatterns[type.ordinal()] = pattern;
            this._gmtOffsetPatternItems[type.ordinal()] = parsedItems;
            checkAbuttingHoursAndMinutes();
            return this;
        } else {
            throw new NullPointerException("Null GMT offset pattern");
        }
    }

    public String getGMTOffsetDigits() {
        StringBuilder buf = new StringBuilder(this._gmtOffsetDigits.length);
        for (String digit : this._gmtOffsetDigits) {
            buf.append(digit);
        }
        return buf.toString();
    }

    public TimeZoneFormat setGMTOffsetDigits(String digits) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        } else if (digits != null) {
            String[] digitArray = toCodePoints(digits);
            if (digitArray.length == 10) {
                this._gmtOffsetDigits = digitArray;
                return this;
            }
            throw new IllegalArgumentException("Length of digits must be 10");
        } else {
            throw new NullPointerException("Null GMT offset digits");
        }
    }

    public String getGMTZeroFormat() {
        return this._gmtZeroFormat;
    }

    public TimeZoneFormat setGMTZeroFormat(String gmtZeroFormat) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        } else if (gmtZeroFormat == null) {
            throw new NullPointerException("Null GMT zero format");
        } else if (gmtZeroFormat.length() != 0) {
            this._gmtZeroFormat = gmtZeroFormat;
            return this;
        } else {
            throw new IllegalArgumentException("Empty GMT zero format");
        }
    }

    public TimeZoneFormat setDefaultParseOptions(EnumSet<ParseOption> options) {
        this._parseAllStyles = options.contains(ParseOption.ALL_STYLES);
        this._parseTZDBNames = options.contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        return this;
    }

    public EnumSet<ParseOption> getDefaultParseOptions() {
        if (this._parseAllStyles && this._parseTZDBNames) {
            return EnumSet.of(ParseOption.ALL_STYLES, ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        if (this._parseAllStyles) {
            return EnumSet.of(ParseOption.ALL_STYLES);
        }
        if (this._parseTZDBNames) {
            return EnumSet.of(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        return EnumSet.noneOf(ParseOption.class);
    }

    public final String formatOffsetISO8601Basic(int offset, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        return formatOffsetISO8601(offset, true, useUtcIndicator, isShort, ignoreSeconds);
    }

    public final String formatOffsetISO8601Extended(int offset, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        return formatOffsetISO8601(offset, false, useUtcIndicator, isShort, ignoreSeconds);
    }

    public String formatOffsetLocalizedGMT(int offset) {
        return formatOffsetLocalizedGMT(offset, false);
    }

    public String formatOffsetShortLocalizedGMT(int offset) {
        return formatOffsetLocalizedGMT(offset, true);
    }

    public final String format(Style style, TimeZone tz, long date) {
        return format(style, tz, date, null);
    }

    public String format(Style style, TimeZone tz, long date, Output<TimeType> timeType) {
        String result = null;
        if (timeType != null) {
            timeType.value = TimeType.UNKNOWN;
        }
        boolean noOffsetFormatFallback = false;
        switch (style) {
            case GENERIC_LOCATION:
                result = getTimeZoneGenericNames().getGenericLocationName(ZoneMeta.getCanonicalCLDRID(tz));
                break;
            case GENERIC_LONG:
                result = getTimeZoneGenericNames().getDisplayName(tz, GenericNameType.LONG, date);
                break;
            case GENERIC_SHORT:
                result = getTimeZoneGenericNames().getDisplayName(tz, GenericNameType.SHORT, date);
                break;
            case SPECIFIC_LONG:
                result = formatSpecific(tz, NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT, date, timeType);
                break;
            case SPECIFIC_SHORT:
                result = formatSpecific(tz, NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT, date, timeType);
                break;
            case ZONE_ID:
                result = tz.getID();
                noOffsetFormatFallback = true;
                break;
            case ZONE_ID_SHORT:
                result = ZoneMeta.getShortID(tz);
                if (result == null) {
                    result = UNKNOWN_SHORT_ZONE_ID;
                }
                noOffsetFormatFallback = true;
                break;
            case EXEMPLAR_LOCATION:
                result = formatExemplarLocation(tz);
                noOffsetFormatFallback = true;
                break;
        }
        if (result == null && !noOffsetFormatFallback) {
            int[] offsets = new int[]{0, 0};
            tz.getOffset(date, false, offsets);
            int offset = offsets[0] + offsets[1];
            switch (style) {
                case GENERIC_LOCATION:
                case GENERIC_LONG:
                case SPECIFIC_LONG:
                case LOCALIZED_GMT:
                    result = formatOffsetLocalizedGMT(offset);
                    break;
                case GENERIC_SHORT:
                case SPECIFIC_SHORT:
                case LOCALIZED_GMT_SHORT:
                    result = formatOffsetShortLocalizedGMT(offset);
                    break;
                case ISO_BASIC_SHORT:
                    result = formatOffsetISO8601Basic(offset, true, true, true);
                    break;
                case ISO_BASIC_LOCAL_SHORT:
                    result = formatOffsetISO8601Basic(offset, false, true, true);
                    break;
                case ISO_BASIC_FIXED:
                    result = formatOffsetISO8601Basic(offset, true, false, true);
                    break;
                case ISO_BASIC_LOCAL_FIXED:
                    result = formatOffsetISO8601Basic(offset, false, false, true);
                    break;
                case ISO_BASIC_FULL:
                    result = formatOffsetISO8601Basic(offset, true, false, false);
                    break;
                case ISO_BASIC_LOCAL_FULL:
                    result = formatOffsetISO8601Basic(offset, false, false, false);
                    break;
                case ISO_EXTENDED_FIXED:
                    result = formatOffsetISO8601Extended(offset, true, false, true);
                    break;
                case ISO_EXTENDED_LOCAL_FIXED:
                    result = formatOffsetISO8601Extended(offset, false, false, true);
                    break;
                case ISO_EXTENDED_FULL:
                    result = formatOffsetISO8601Extended(offset, true, false, false);
                    break;
                case ISO_EXTENDED_LOCAL_FULL:
                    result = formatOffsetISO8601Extended(offset, false, false, false);
                    break;
            }
            if (timeType != null) {
                timeType.value = offsets[1] != 0 ? TimeType.DAYLIGHT : TimeType.STANDARD;
            }
        }
        return result;
    }

    public final int parseOffsetISO8601(String text, ParsePosition pos) {
        return parseOffsetISO8601(text, pos, false, null);
    }

    public int parseOffsetLocalizedGMT(String text, ParsePosition pos) {
        return parseOffsetLocalizedGMT(text, pos, false, null);
    }

    public int parseOffsetShortLocalizedGMT(String text, ParsePosition pos) {
        return parseOffsetLocalizedGMT(text, pos, true, null);
    }

    /* JADX WARNING: Removed duplicated region for block: B:210:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:198:0x048c  */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x04e2  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x03fb  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x03f0  */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x0560  */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x0403  */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x057b  */
    /* JADX WARNING: Removed duplicated region for block: B:234:0x0568  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x03f0  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x03fb  */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x0403  */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x0560  */
    /* JADX WARNING: Removed duplicated region for block: B:234:0x0568  */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x057b  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x0397  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x03fb  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x03f0  */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x0560  */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x0403  */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x057b  */
    /* JADX WARNING: Removed duplicated region for block: B:234:0x0568  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public TimeZone parse(Style style, String text, ParsePosition pos, EnumSet<ParseOption> options, Output<TimeType> timeType) {
        int offset;
        MatchInfo specificMatches;
        String id;
        Style style2 = style;
        String str = text;
        ParsePosition parsePosition = pos;
        EnumSet<ParseOption> enumSet = options;
        Output<TimeType> timeType2 = timeType;
        if (timeType2 == null) {
            timeType2 = new Output(TimeType.UNKNOWN);
        } else {
            timeType2.value = TimeType.UNKNOWN;
        }
        int startIdx = pos.getIndex();
        int maxPos = text.length();
        boolean fallbackLocalizedGMT = style2 == Style.SPECIFIC_LONG || style2 == Style.GENERIC_LONG || style2 == Style.GENERIC_LOCATION;
        boolean fallbackShortLocalizedGMT = style2 == Style.SPECIFIC_SHORT || style2 == Style.GENERIC_SHORT;
        int evaluated = 0;
        ParsePosition tmpPos = new ParsePosition(startIdx);
        int parsedOffset = Integer.MAX_VALUE;
        int parsedPos = -1;
        if (fallbackLocalizedGMT || fallbackShortLocalizedGMT) {
            Output<Boolean> hasDigitOffset = new Output(Boolean.valueOf(false));
            offset = parseOffsetLocalizedGMT(str, tmpPos, fallbackShortLocalizedGMT, hasDigitOffset);
            if (tmpPos.getErrorIndex()) {
                if (tmpPos.getIndex() == maxPos || ((Boolean) hasDigitOffset.value).booleanValue()) {
                    parsePosition.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                parsedOffset = offset;
                parsedPos = tmpPos.getIndex();
            }
            evaluated = 0 | (Style.LOCALIZED_GMT.flag | Style.LOCALIZED_GMT_SHORT.flag);
        } else {
            boolean z = fallbackLocalizedGMT;
            boolean z2 = fallbackShortLocalizedGMT;
        }
        if (enumSet == null) {
            fallbackLocalizedGMT = getDefaultParseOptions().contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        } else {
            fallbackLocalizedGMT = enumSet.contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        switch (style) {
            case GENERIC_LOCATION:
            case GENERIC_LONG:
            case GENERIC_SHORT:
                EnumSet<GenericNameType> genericNameTypes = null;
                switch (style) {
                    case GENERIC_LOCATION:
                        genericNameTypes = EnumSet.of(GenericNameType.LOCATION);
                        break;
                    case GENERIC_LONG:
                        genericNameTypes = EnumSet.of(GenericNameType.LONG, GenericNameType.LOCATION);
                        break;
                    case GENERIC_SHORT:
                        genericNameTypes = EnumSet.of(GenericNameType.SHORT, GenericNameType.LOCATION);
                        break;
                }
                GenericMatchInfo bestGeneric = getTimeZoneGenericNames().findBestMatch(str, startIdx, genericNameTypes);
                if (bestGeneric != null && bestGeneric.matchLength() + startIdx > parsedPos) {
                    timeType2.value = bestGeneric.timeType();
                    parsePosition.setIndex(bestGeneric.matchLength() + startIdx);
                    return TimeZone.getTimeZone(bestGeneric.tzID());
                }
            case SPECIFIC_LONG:
            case SPECIFIC_SHORT:
                EnumSet<NameType> nameTypes;
                MatchInfo specificMatch;
                Iterator it;
                if (style2 == Style.SPECIFIC_LONG) {
                    nameTypes = EnumSet.of(NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT);
                } else {
                    nameTypes = EnumSet.of(NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT);
                }
                Collection<MatchInfo> specificMatches2 = this._tznames.find(str, startIdx, nameTypes);
                if (specificMatches2 != null) {
                    specificMatch = null;
                    it = specificMatches2.iterator();
                    while (it.hasNext()) {
                        Collection<MatchInfo> specificMatches3 = specificMatches2;
                        specificMatches = (MatchInfo) it.next();
                        Iterator it2 = it;
                        if (startIdx + specificMatches.matchLength() > parsedPos) {
                            specificMatch = specificMatches;
                            parsedPos = specificMatches.matchLength() + startIdx;
                        }
                        specificMatches2 = specificMatches3;
                        it = it2;
                    }
                    if (specificMatch != null) {
                        timeType2.value = getTimeType(specificMatch.nameType());
                        parsePosition.setIndex(parsedPos);
                        return TimeZone.getTimeZone(getTimeZoneID(specificMatch.tzID(), specificMatch.mzID()));
                    }
                }
                if (fallbackLocalizedGMT && style2 == Style.SPECIFIC_SHORT) {
                    specificMatches2 = getTZDBTimeZoneNames().find(str, startIdx, nameTypes);
                    if (specificMatches2 == null) {
                        break;
                    }
                    Collection<MatchInfo> tzdbNameMatches;
                    specificMatch = null;
                    for (MatchInfo nameTypes2 : specificMatches2) {
                        EnumSet<NameType> nameTypes3 = nameTypes;
                        tzdbNameMatches = specificMatches2;
                        if (startIdx + nameTypes2.matchLength() > parsedPos) {
                            parsedPos = nameTypes2.matchLength() + startIdx;
                            specificMatch = nameTypes2;
                        }
                        nameTypes = nameTypes3;
                        specificMatches2 = tzdbNameMatches;
                    }
                    tzdbNameMatches = specificMatches2;
                    if (specificMatch != null) {
                        timeType2.value = getTimeType(specificMatch.nameType());
                        parsePosition.setIndex(parsedPos);
                        return TimeZone.getTimeZone(getTimeZoneID(specificMatch.tzID(), specificMatch.mzID()));
                    }
                }
                break;
            case ZONE_ID:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                id = parseZoneID(str, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    parsePosition.setIndex(tmpPos.getIndex());
                    return TimeZone.getTimeZone(id);
                }
                break;
            case ZONE_ID_SHORT:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                id = parseShortZoneID(str, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    parsePosition.setIndex(tmpPos.getIndex());
                    return TimeZone.getTimeZone(id);
                }
                break;
            case EXEMPLAR_LOCATION:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                id = parseExemplarLocation(str, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    parsePosition.setIndex(tmpPos.getIndex());
                    return TimeZone.getTimeZone(id);
                }
                break;
            case LOCALIZED_GMT:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                offset = parseOffsetLocalizedGMT(str, tmpPos);
                if (tmpPos.getErrorIndex() != -1) {
                    evaluated |= Style.LOCALIZED_GMT_SHORT.flag;
                    break;
                }
                parsePosition.setIndex(tmpPos.getIndex());
                return getTimeZoneForOffset(offset);
            case LOCALIZED_GMT_SHORT:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                offset = parseOffsetShortLocalizedGMT(str, tmpPos);
                if (tmpPos.getErrorIndex() != -1) {
                    evaluated |= Style.LOCALIZED_GMT.flag;
                    break;
                }
                parsePosition.setIndex(tmpPos.getIndex());
                return getTimeZoneForOffset(offset);
            case ISO_BASIC_SHORT:
            case ISO_BASIC_FIXED:
            case ISO_BASIC_FULL:
            case ISO_EXTENDED_FIXED:
            case ISO_EXTENDED_FULL:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                offset = parseOffsetISO8601(str, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    parsePosition.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                break;
            case ISO_BASIC_LOCAL_SHORT:
            case ISO_BASIC_LOCAL_FIXED:
            case ISO_BASIC_LOCAL_FULL:
            case ISO_EXTENDED_LOCAL_FIXED:
            case ISO_EXTENDED_LOCAL_FULL:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                Output<Boolean> hasDigitOffset2 = new Output(Boolean.valueOf(false));
                int offset2 = parseOffsetISO8601(str, tmpPos, false, hasDigitOffset2);
                if (tmpPos.getErrorIndex() == -1 && ((Boolean) hasDigitOffset2.value).booleanValue()) {
                    parsePosition.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset2);
                }
        }
        int evaluated2 = style2.flag | evaluated;
        if (parsedPos > startIdx) {
            parsePosition.setIndex(parsedPos);
            return getTimeZoneForOffset(parsedOffset);
        }
        String parsedID;
        String parsedID2;
        String parsedID3;
        EnumSet<ParseOption> enumSet2;
        TimeType parsedTimeType = TimeType.UNKNOWN;
        if (parsedPos >= maxPos) {
            parsedID = null;
        } else if ((evaluated2 & 128) == 0 || (evaluated2 & 256) == 0) {
            tmpPos.setIndex(startIdx);
            tmpPos.setErrorIndex(-1);
            Output<Boolean> hasDigitOffset3 = new Output(Boolean.valueOf(false));
            evaluated = parseOffsetISO8601(str, tmpPos, false, hasDigitOffset3);
            parsedID = null;
            if (tmpPos.getErrorIndex() == -1) {
                if (tmpPos.getIndex() == maxPos || ((Boolean) hasDigitOffset3.value).booleanValue()) {
                    parsePosition.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(evaluated);
                } else if (parsedPos < tmpPos.getIndex()) {
                    parsedOffset = evaluated;
                    id = null;
                    parsedTimeType = TimeType.UNKNOWN;
                    parsedPos = tmpPos.getIndex();
                    if (parsedPos < maxPos || (Style.LOCALIZED_GMT.flag & evaluated2) != 0) {
                        parsedID2 = id;
                    } else {
                        tmpPos.setIndex(startIdx);
                        tmpPos.setErrorIndex(-1);
                        hasDigitOffset3 = new Output(Boolean.valueOf(false));
                        int offset3 = parseOffsetLocalizedGMT(str, tmpPos, false, hasDigitOffset3);
                        parsedID2 = id;
                        if (tmpPos.getErrorIndex() == -1) {
                            if (tmpPos.getIndex() == maxPos || ((Boolean) hasDigitOffset3.value).booleanValue()) {
                                parsePosition.setIndex(tmpPos.getIndex());
                                return getTimeZoneForOffset(offset3);
                            } else if (parsedPos < tmpPos.getIndex()) {
                                parsedOffset = offset3;
                                id = null;
                                parsedTimeType = TimeType.UNKNOWN;
                                parsedPos = tmpPos.getIndex();
                                if (parsedPos < maxPos || (Style.LOCALIZED_GMT_SHORT.flag & evaluated2) != 0) {
                                    parsedID3 = id;
                                } else {
                                    tmpPos.setIndex(startIdx);
                                    tmpPos.setErrorIndex(-1);
                                    Output<Boolean> hasDigitOffset4 = new Output(Boolean.valueOf(false));
                                    evaluated = parseOffsetLocalizedGMT(str, tmpPos, true, hasDigitOffset4);
                                    parsedID3 = id;
                                    if (tmpPos.getErrorIndex() == -1) {
                                        if (tmpPos.getIndex() == maxPos || ((Boolean) hasDigitOffset4.value).booleanValue()) {
                                            parsePosition.setIndex(tmpPos.getIndex());
                                            return getTimeZoneForOffset(evaluated);
                                        } else if (parsedPos < tmpPos.getIndex()) {
                                            parsedOffset = evaluated;
                                            id = null;
                                            parsedTimeType = TimeType.UNKNOWN;
                                            parsedPos = tmpPos.getIndex();
                                            enumSet2 = options;
                                            if (enumSet2 != null) {
                                                fallbackShortLocalizedGMT = getDefaultParseOptions().contains(ParseOption.ALL_STYLES);
                                            } else {
                                                fallbackShortLocalizedGMT = enumSet2.contains(ParseOption.ALL_STYLES);
                                            }
                                            String str2;
                                            TimeType timeType3;
                                            if (fallbackShortLocalizedGMT) {
                                                str2 = id;
                                                timeType3 = parsedTimeType;
                                            } else {
                                                Collection<MatchInfo> specificMatches4;
                                                int matchPos;
                                                String parsedID4;
                                                if (parsedPos < maxPos) {
                                                    specificMatches4 = this._tznames.find(str, startIdx, ALL_SIMPLE_NAME_TYPES);
                                                    MatchInfo specificMatch2 = null;
                                                    if (specificMatches4 != null) {
                                                        str2 = id;
                                                        Iterator parsedID5 = specificMatches4.iterator();
                                                        matchPos = -1;
                                                        while (parsedID5.hasNext()) {
                                                            Iterator it3 = parsedID5;
                                                            specificMatches = (MatchInfo) parsedID5.next();
                                                            timeType3 = parsedTimeType;
                                                            if (startIdx + specificMatches.matchLength() > matchPos) {
                                                                specificMatch2 = specificMatches;
                                                                matchPos = startIdx + specificMatches.matchLength();
                                                            }
                                                            parsedID5 = it3;
                                                            parsedTimeType = timeType3;
                                                        }
                                                        timeType3 = parsedTimeType;
                                                    } else {
                                                        str2 = id;
                                                        timeType3 = parsedTimeType;
                                                        matchPos = -1;
                                                    }
                                                    if (parsedPos < matchPos) {
                                                        parsedPos = matchPos;
                                                        id = getTimeZoneID(specificMatch2.tzID(), specificMatch2.mzID());
                                                        parsedTimeType = getTimeType(specificMatch2.nameType());
                                                        parsedOffset = Integer.MAX_VALUE;
                                                        if (fallbackLocalizedGMT && parsedPos < maxPos && (Style.SPECIFIC_SHORT.flag & evaluated2) == 0) {
                                                            specificMatches4 = getTZDBTimeZoneNames().find(str, startIdx, ALL_SIMPLE_NAME_TYPES);
                                                            if (specificMatches4 != null) {
                                                                fallbackLocalizedGMT = specificMatches4.iterator();
                                                                MatchInfo tzdbNameMatch = null;
                                                                evaluated = -1;
                                                                while (fallbackLocalizedGMT.hasNext()) {
                                                                    Object obj = fallbackLocalizedGMT;
                                                                    MatchInfo parseTZDBAbbrev = (MatchInfo) fallbackLocalizedGMT.next();
                                                                    parsedID4 = id;
                                                                    if (startIdx + parseTZDBAbbrev.matchLength() > evaluated) {
                                                                        tzdbNameMatch = parseTZDBAbbrev;
                                                                        evaluated = parseTZDBAbbrev.matchLength() + startIdx;
                                                                    }
                                                                    fallbackLocalizedGMT = obj;
                                                                    id = parsedID4;
                                                                }
                                                                parsedID4 = id;
                                                                if (parsedPos < evaluated) {
                                                                    parsedPos = evaluated;
                                                                    id = getTimeZoneID(tzdbNameMatch.tzID(), tzdbNameMatch.mzID());
                                                                    parsedTimeType = getTimeType(tzdbNameMatch.nameType());
                                                                    parsedOffset = Integer.MAX_VALUE;
                                                                    parsedID4 = id;
                                                                }
                                                                if (parsedPos < maxPos) {
                                                                    GenericMatchInfo genericMatch = getTimeZoneGenericNames().findBestMatch(str, startIdx, ALL_GENERIC_NAME_TYPES);
                                                                    if (genericMatch != null && parsedPos < genericMatch.matchLength() + startIdx) {
                                                                        parsedPos = startIdx + genericMatch.matchLength();
                                                                        parsedID4 = genericMatch.tzID();
                                                                        parsedTimeType = genericMatch.timeType();
                                                                        parsedOffset = Integer.MAX_VALUE;
                                                                    }
                                                                }
                                                                if (parsedPos < maxPos && (Style.ZONE_ID.flag & evaluated2) == 0) {
                                                                    tmpPos.setIndex(startIdx);
                                                                    tmpPos.setErrorIndex(-1);
                                                                    fallbackLocalizedGMT = parseZoneID(str, tmpPos);
                                                                    if (tmpPos.getErrorIndex() == -1 && parsedPos < tmpPos.getIndex()) {
                                                                        matchPos = tmpPos.getIndex();
                                                                        id = fallbackLocalizedGMT;
                                                                        parsedTimeType = TimeType.UNKNOWN;
                                                                        parsedPos = matchPos;
                                                                        parsedOffset = true;
                                                                        if (parsedPos < maxPos && (Style.ZONE_ID_SHORT.flag & evaluated2) == 0) {
                                                                            tmpPos.setIndex(startIdx);
                                                                            tmpPos.setErrorIndex(-1);
                                                                            fallbackLocalizedGMT = parseShortZoneID(str, tmpPos);
                                                                            if (tmpPos.getErrorIndex() == -1 && parsedPos < tmpPos.getIndex()) {
                                                                                parsedPos = tmpPos.getIndex();
                                                                                id = fallbackLocalizedGMT;
                                                                                parsedTimeType = TimeType.UNKNOWN;
                                                                                parsedOffset = Integer.MAX_VALUE;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                id = parsedID4;
                                                                tmpPos.setIndex(startIdx);
                                                                tmpPos.setErrorIndex(-1);
                                                                fallbackLocalizedGMT = parseShortZoneID(str, tmpPos);
                                                                parsedPos = tmpPos.getIndex();
                                                                id = fallbackLocalizedGMT;
                                                                parsedTimeType = TimeType.UNKNOWN;
                                                                parsedOffset = Integer.MAX_VALUE;
                                                            }
                                                        }
                                                        parsedID4 = id;
                                                        if (parsedPos < maxPos) {
                                                        }
                                                        tmpPos.setIndex(startIdx);
                                                        tmpPos.setErrorIndex(-1);
                                                        fallbackLocalizedGMT = parseZoneID(str, tmpPos);
                                                        matchPos = tmpPos.getIndex();
                                                        id = fallbackLocalizedGMT;
                                                        parsedTimeType = TimeType.UNKNOWN;
                                                        parsedPos = matchPos;
                                                        parsedOffset = true;
                                                        tmpPos.setIndex(startIdx);
                                                        tmpPos.setErrorIndex(-1);
                                                        fallbackLocalizedGMT = parseShortZoneID(str, tmpPos);
                                                        parsedPos = tmpPos.getIndex();
                                                        id = fallbackLocalizedGMT;
                                                        parsedTimeType = TimeType.UNKNOWN;
                                                        parsedOffset = Integer.MAX_VALUE;
                                                    }
                                                } else {
                                                    str2 = id;
                                                    timeType3 = parsedTimeType;
                                                }
                                                id = str2;
                                                parsedTimeType = timeType3;
                                                specificMatches4 = getTZDBTimeZoneNames().find(str, startIdx, ALL_SIMPLE_NAME_TYPES);
                                                if (specificMatches4 != null) {
                                                }
                                                parsedID4 = id;
                                                if (parsedPos < maxPos) {
                                                }
                                                tmpPos.setIndex(startIdx);
                                                tmpPos.setErrorIndex(-1);
                                                fallbackLocalizedGMT = parseZoneID(str, tmpPos);
                                                matchPos = tmpPos.getIndex();
                                                id = fallbackLocalizedGMT;
                                                parsedTimeType = TimeType.UNKNOWN;
                                                parsedPos = matchPos;
                                                parsedOffset = true;
                                                tmpPos.setIndex(startIdx);
                                                tmpPos.setErrorIndex(-1);
                                                fallbackLocalizedGMT = parseShortZoneID(str, tmpPos);
                                                parsedPos = tmpPos.getIndex();
                                                id = fallbackLocalizedGMT;
                                                parsedTimeType = TimeType.UNKNOWN;
                                                parsedOffset = Integer.MAX_VALUE;
                                            }
                                            if (parsedPos <= startIdx) {
                                                TimeZone parsedTZ;
                                                if (id != null) {
                                                    parsedTZ = TimeZone.getTimeZone(id);
                                                } else {
                                                    parsedTZ = getTimeZoneForOffset(parsedOffset);
                                                }
                                                timeType2.value = parsedTimeType;
                                                parsePosition.setIndex(parsedPos);
                                                return parsedTZ;
                                            }
                                            parsePosition.setErrorIndex(startIdx);
                                            return null;
                                        }
                                    }
                                }
                                id = parsedID3;
                                enumSet2 = options;
                                if (enumSet2 != null) {
                                }
                                if (fallbackShortLocalizedGMT) {
                                }
                                if (parsedPos <= startIdx) {
                                }
                            }
                        }
                    }
                    id = parsedID2;
                    if (parsedPos < maxPos) {
                    }
                    parsedID3 = id;
                    id = parsedID3;
                    enumSet2 = options;
                    if (enumSet2 != null) {
                    }
                    if (fallbackShortLocalizedGMT) {
                    }
                    if (parsedPos <= startIdx) {
                    }
                }
            }
        } else {
            parsedID = null;
        }
        id = parsedID;
        if (parsedPos < maxPos) {
        }
        parsedID2 = id;
        id = parsedID2;
        if (parsedPos < maxPos) {
        }
        parsedID3 = id;
        id = parsedID3;
        enumSet2 = options;
        if (enumSet2 != null) {
        }
        if (fallbackShortLocalizedGMT) {
        }
        if (parsedPos <= startIdx) {
        }
    }

    public TimeZone parse(Style style, String text, ParsePosition pos, Output<TimeType> timeType) {
        return parse(style, text, pos, null, timeType);
    }

    public final TimeZone parse(String text, ParsePosition pos) {
        return parse(Style.GENERIC_LOCATION, text, pos, EnumSet.of(ParseOption.ALL_STYLES), null);
    }

    public final TimeZone parse(String text) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        TimeZone tz = parse(text, pos);
        if (pos.getErrorIndex() < 0) {
            return tz;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unparseable time zone: \"");
        stringBuilder.append(text);
        stringBuilder.append("\"");
        throw new ParseException(stringBuilder.toString(), 0);
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        TimeZone tz;
        long date = System.currentTimeMillis();
        if (obj instanceof TimeZone) {
            tz = (TimeZone) obj;
        } else if (obj instanceof Calendar) {
            tz = ((Calendar) obj).getTimeZone();
            date = ((Calendar) obj).getTimeInMillis();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot format given Object (");
            stringBuilder.append(obj.getClass().getName());
            stringBuilder.append(") as a time zone");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        String result = formatOffsetLocalizedGMT(tz.getOffset(date));
        toAppendTo.append(result);
        if (pos.getFieldAttribute() == Field.TIME_ZONE || pos.getField() == 17) {
            pos.setBeginIndex(0);
            pos.setEndIndex(result.length());
        }
        return toAppendTo;
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        AttributedString as = new AttributedString(format(obj, new StringBuffer(), new FieldPosition(0)).toString());
        as.addAttribute(Field.TIME_ZONE, Field.TIME_ZONE);
        return as.getIterator();
    }

    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    private String formatOffsetLocalizedGMT(int offset, boolean isShort) {
        if (offset == 0) {
            return this._gmtZeroFormat;
        }
        StringBuilder buf = new StringBuilder();
        boolean positive = true;
        if (offset < 0) {
            offset = -offset;
            positive = false;
        }
        int offsetH = offset / 3600000;
        offset %= 3600000;
        int offsetM = offset / 60000;
        offset %= 60000;
        int offsetS = offset / 1000;
        if (offsetH > 23 || offsetM > 59 || offsetS > 59) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Offset out of range :");
            stringBuilder.append(offset);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        Object[] offsetPatternItems;
        if (positive) {
            if (offsetS != 0) {
                offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_HMS.ordinal()];
            } else if (offsetM == 0 && isShort) {
                offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_H.ordinal()];
            } else {
                offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_HM.ordinal()];
            }
        } else if (offsetS != 0) {
            offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()];
        } else if (offsetM == 0 && isShort) {
            offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_H.ordinal()];
        } else {
            offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_HM.ordinal()];
        }
        buf.append(this._gmtPatternPrefix);
        for (GMTOffsetField item : offsetPatternItems) {
            if (item instanceof String) {
                buf.append((String) item);
            } else if (item instanceof GMTOffsetField) {
                char type = item.getType();
                int i = 2;
                if (type == 'H') {
                    if (isShort) {
                        i = 1;
                    }
                    appendOffsetDigits(buf, offsetH, i);
                } else if (type == 'm') {
                    appendOffsetDigits(buf, offsetM, 2);
                } else if (type == 's') {
                    appendOffsetDigits(buf, offsetS, 2);
                }
            }
        }
        buf.append(this._gmtPatternSuffix);
        return buf.toString();
    }

    private String formatOffsetISO8601(int offset, boolean isBasic, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        int i = offset;
        int absOffset = i < 0 ? -i : i;
        if (useUtcIndicator && (absOffset < 1000 || (ignoreSeconds && absOffset < 60000))) {
            return ISO8601_UTC;
        }
        OffsetFields minFields = isShort ? OffsetFields.H : OffsetFields.HM;
        OffsetFields maxFields = ignoreSeconds ? OffsetFields.HM : OffsetFields.HMS;
        Character sep = isBasic ? null : Character.valueOf(DEFAULT_GMT_OFFSET_SEP);
        if (absOffset < 86400000) {
            int[] fields = new int[3];
            int idx = 0;
            fields[0] = absOffset / 3600000;
            absOffset %= 3600000;
            fields[1] = absOffset / 60000;
            fields[2] = (absOffset % 60000) / 1000;
            int lastIdx = maxFields.ordinal();
            while (lastIdx > minFields.ordinal() && fields[lastIdx] == 0) {
                lastIdx--;
            }
            StringBuilder buf = new StringBuilder();
            char sign = '+';
            if (i < 0) {
                for (int idx2 = 0; idx2 <= lastIdx; idx2++) {
                    if (fields[idx2] != 0) {
                        sign = '-';
                        break;
                    }
                }
            }
            buf.append(sign);
            while (idx <= lastIdx) {
                if (!(sep == null || idx == 0)) {
                    buf.append(sep);
                }
                if (fields[idx] < 10) {
                    buf.append('0');
                }
                buf.append(fields[idx]);
                idx++;
            }
            return buf.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Offset out of range :");
        stringBuilder.append(i);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private String formatSpecific(TimeZone tz, NameType stdType, NameType dstType, long date, Output<TimeType> timeType) {
        String name;
        boolean isDaylight = tz.inDaylightTime(new Date(date));
        if (isDaylight) {
            name = getTimeZoneNames().getDisplayName(ZoneMeta.getCanonicalCLDRID(tz), dstType, date);
        } else {
            name = getTimeZoneNames().getDisplayName(ZoneMeta.getCanonicalCLDRID(tz), stdType, date);
        }
        if (!(name == null || timeType == null)) {
            timeType.value = isDaylight ? TimeType.DAYLIGHT : TimeType.STANDARD;
        }
        return name;
    }

    private String formatExemplarLocation(TimeZone tz) {
        String location = getTimeZoneNames().getExemplarLocationName(ZoneMeta.getCanonicalCLDRID(tz));
        if (location != null) {
            return location;
        }
        location = getTimeZoneNames().getExemplarLocationName("Etc/Unknown");
        if (location == null) {
            return UNKNOWN_LOCATION;
        }
        return location;
    }

    private String getTimeZoneID(String tzID, String mzID) {
        String id = tzID;
        if (id == null) {
            id = this._tznames.getReferenceZoneID(mzID, getTargetRegion());
            if (id == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid mzID: ");
                stringBuilder.append(mzID);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return id;
    }

    private synchronized String getTargetRegion() {
        if (this._region == null) {
            this._region = this._locale.getCountry();
            if (this._region.length() == 0) {
                this._region = ULocale.addLikelySubtags(this._locale).getCountry();
                if (this._region.length() == 0) {
                    this._region = "001";
                }
            }
        }
        return this._region;
    }

    private TimeType getTimeType(NameType nameType) {
        switch (nameType) {
            case LONG_STANDARD:
            case SHORT_STANDARD:
                return TimeType.STANDARD;
            case LONG_DAYLIGHT:
            case SHORT_DAYLIGHT:
                return TimeType.DAYLIGHT;
            default:
                return TimeType.UNKNOWN;
        }
    }

    private void initGMTPattern(String gmtPattern) {
        int idx = gmtPattern.indexOf("{0}");
        if (idx >= 0) {
            this._gmtPattern = gmtPattern;
            this._gmtPatternPrefix = unquote(gmtPattern.substring(0, idx));
            this._gmtPatternSuffix = unquote(gmtPattern.substring(idx + 3));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad localized GMT pattern: ");
        stringBuilder.append(gmtPattern);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static String unquote(String s) {
        if (s.indexOf(39) < 0) {
            return s;
        }
        StringBuilder buf = new StringBuilder();
        boolean inQuote = false;
        boolean isPrevQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == PatternTokenizer.SINGLE_QUOTE) {
                if (isPrevQuote) {
                    buf.append(c);
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                }
                inQuote = !inQuote;
            } else {
                isPrevQuote = false;
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private void initGMTOffsetPatterns(String[] gmtOffsetPatterns) {
        int size = GMTOffsetPatternType.values().length;
        if (gmtOffsetPatterns.length >= size) {
            Object[][] gmtOffsetPatternItems = new Object[size][];
            for (GMTOffsetPatternType t : GMTOffsetPatternType.values()) {
                int idx = t.ordinal();
                gmtOffsetPatternItems[idx] = parseOffsetPattern(gmtOffsetPatterns[idx], t.required());
            }
            this._gmtOffsetPatterns = new String[size];
            System.arraycopy(gmtOffsetPatterns, 0, this._gmtOffsetPatterns, 0, size);
            this._gmtOffsetPatternItems = gmtOffsetPatternItems;
            checkAbuttingHoursAndMinutes();
            return;
        }
        throw new IllegalArgumentException("Insufficient number of elements in gmtOffsetPatterns");
    }

    private void checkAbuttingHoursAndMinutes() {
        this._abuttingOffsetHoursAndMinutes = false;
        for (Object[] items : this._gmtOffsetPatternItems) {
            boolean afterH = false;
            for (GMTOffsetField item : r1[r3]) {
                if (item instanceof GMTOffsetField) {
                    GMTOffsetField fld = item;
                    if (afterH) {
                        this._abuttingOffsetHoursAndMinutes = true;
                    } else if (fld.getType() == 'H') {
                        afterH = true;
                    }
                } else if (afterH) {
                    break;
                }
            }
        }
    }

    private static Object[] parseOffsetPattern(String pattern, String letters) {
        StringBuilder text = new StringBuilder();
        boolean invalidPattern = false;
        List<Object> items = new ArrayList();
        BitSet checkBits = new BitSet(letters.length());
        int itemLength = 1;
        char itemType = 0;
        boolean inQuote = false;
        boolean isPrevQuote = false;
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == PatternTokenizer.SINGLE_QUOTE) {
                if (!isPrevQuote) {
                    isPrevQuote = true;
                    if (itemType != 0) {
                        if (!GMTOffsetField.isValid(itemType, itemLength)) {
                            invalidPattern = true;
                            break;
                        }
                        items.add(new GMTOffsetField(itemType, itemLength));
                        itemType = 0;
                    }
                } else {
                    text.append(PatternTokenizer.SINGLE_QUOTE);
                    isPrevQuote = false;
                }
                inQuote = !inQuote;
            } else {
                isPrevQuote = false;
                if (inQuote) {
                    text.append(ch);
                } else {
                    int patFieldIdx = letters.indexOf(ch);
                    if (patFieldIdx < 0) {
                        if (itemType != 0) {
                            if (!GMTOffsetField.isValid(itemType, itemLength)) {
                                invalidPattern = true;
                                break;
                            }
                            items.add(new GMTOffsetField(itemType, itemLength));
                            itemType = 0;
                        }
                        text.append(ch);
                    } else if (ch == itemType) {
                        itemLength++;
                    } else {
                        if (itemType != 0) {
                            if (!GMTOffsetField.isValid(itemType, itemLength)) {
                                invalidPattern = true;
                                break;
                            }
                            items.add(new GMTOffsetField(itemType, itemLength));
                        } else if (text.length() > 0) {
                            items.add(text.toString());
                            text.setLength(0);
                        }
                        itemType = ch;
                        itemLength = 1;
                        checkBits.set(patFieldIdx);
                    }
                }
            }
        }
        if (!invalidPattern) {
            if (itemType == 0) {
                if (text.length() > 0) {
                    items.add(text.toString());
                    text.setLength(0);
                }
            } else if (GMTOffsetField.isValid(itemType, itemLength)) {
                items.add(new GMTOffsetField(itemType, itemLength));
            } else {
                invalidPattern = true;
            }
        }
        if (!invalidPattern && checkBits.cardinality() == letters.length()) {
            return items.toArray(new Object[items.size()]);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad localized GMT offset pattern: ");
        stringBuilder.append(pattern);
        throw new IllegalStateException(stringBuilder.toString());
    }

    private static String expandOffsetPattern(String offsetHM) {
        int idx_mm = offsetHM.indexOf("mm");
        if (idx_mm >= 0) {
            String sep = ":";
            int idx_H = offsetHM.substring(0, idx_mm).lastIndexOf(DateFormat.HOUR24);
            if (idx_H >= 0) {
                sep = offsetHM.substring(idx_H + 1, idx_mm);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(offsetHM.substring(0, idx_mm + 2));
            stringBuilder.append(sep);
            stringBuilder.append("ss");
            stringBuilder.append(offsetHM.substring(idx_mm + 2));
            return stringBuilder.toString();
        }
        throw new RuntimeException("Bad time zone hour pattern data");
    }

    private static String truncateOffsetPattern(String offsetHM) {
        int idx_mm = offsetHM.indexOf("mm");
        if (idx_mm >= 0) {
            int idx_HH = offsetHM.substring(0, idx_mm).lastIndexOf("HH");
            if (idx_HH >= 0) {
                return offsetHM.substring(0, idx_HH + 2);
            }
            int idx_H = offsetHM.substring(0, idx_mm).lastIndexOf(DateFormat.HOUR24);
            if (idx_H >= 0) {
                return offsetHM.substring(0, idx_H + 1);
            }
            throw new RuntimeException("Bad time zone hour pattern data");
        }
        throw new RuntimeException("Bad time zone hour pattern data");
    }

    private void appendOffsetDigits(StringBuilder buf, int n, int minDigits) {
        int numDigits = n >= 10 ? 2 : 1;
        for (int i = 0; i < minDigits - numDigits; i++) {
            buf.append(this._gmtOffsetDigits[0]);
        }
        if (numDigits == 2) {
            buf.append(this._gmtOffsetDigits[n / 10]);
        }
        buf.append(this._gmtOffsetDigits[n % 10]);
    }

    private TimeZone getTimeZoneForOffset(int offset) {
        if (offset == 0) {
            return TimeZone.getTimeZone(TZID_GMT);
        }
        return ZoneMeta.getCustomTimeZone(offset);
    }

    private int parseOffsetLocalizedGMT(String text, ParsePosition pos, boolean isShort, Output<Boolean> hasDigitOffset) {
        String str = text;
        ParsePosition parsePosition = pos;
        Output<Boolean> output = hasDigitOffset;
        int start = pos.getIndex();
        int[] parsedLength = new int[]{0};
        if (output != null) {
            output.value = Boolean.valueOf(false);
        }
        int offset = parseOffsetLocalizedGMTPattern(str, start, isShort, parsedLength);
        if (parsedLength[0] > 0) {
            if (output != null) {
                output.value = Boolean.valueOf(true);
            }
            parsePosition.setIndex(parsedLength[0] + start);
            return offset;
        }
        int offset2 = parseOffsetDefaultLocalizedGMT(str, start, parsedLength);
        if (parsedLength[0] > 0) {
            if (output != null) {
                output.value = Boolean.valueOf(true);
            }
            parsePosition.setIndex(parsedLength[0] + start);
            return offset2;
        }
        if (str.regionMatches(true, start, this._gmtZeroFormat, 0, this._gmtZeroFormat.length())) {
            parsePosition.setIndex(this._gmtZeroFormat.length() + start);
            return 0;
        }
        String[] strArr = ALT_GMT_STRINGS;
        int length = strArr.length;
        int i = 0;
        while (i < length) {
            String str2 = strArr[i];
            String defGMTZero = str2;
            int i2 = i;
            int i3 = length;
            if (str.regionMatches(true, start, str2, 0, str2.length())) {
                parsePosition.setIndex(defGMTZero.length() + start);
                return 0;
            }
            i = i2 + 1;
            length = i3;
        }
        parsePosition.setErrorIndex(start);
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0052  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int parseOffsetLocalizedGMTPattern(String text, int start, boolean isShort, int[] parsedLen) {
        String str;
        int idx = start;
        int offset = 0;
        boolean parsed = false;
        int len = this._gmtPatternPrefix.length();
        if (len > 0) {
            if (!text.regionMatches(true, idx, this._gmtPatternPrefix, 0, len)) {
                str = text;
                parsedLen[0] = parsed ? idx - start : 0;
                return offset;
            }
        }
        idx += len;
        int[] offsetLen = new int[1];
        str = text;
        offset = parseOffsetFields(str, idx, false, offsetLen);
        if (offsetLen[0] != 0) {
            int idx2 = offsetLen[0] + idx;
            int len2 = this._gmtPatternSuffix.length();
            if (len2 > 0) {
                if (!str.regionMatches(true, idx2, this._gmtPatternSuffix, 0, len2)) {
                    idx = idx2;
                }
            }
            idx = idx2 + len2;
            parsed = true;
        }
        if (parsed) {
        }
        parsedLen[0] = parsed ? idx - start : 0;
        return offset;
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x00b5  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int parseOffsetFields(String text, int start, boolean isShort, int[] parsedLen) {
        int i;
        GMTOffsetPatternType gmtPatType;
        int i2;
        int[] iArr = parsedLen;
        int outLen = 1;
        if (iArr != null && iArr.length >= 1) {
            iArr[0] = 0;
        }
        int offsetS = 0;
        int offsetM = 0;
        int offsetH = 0;
        int[] fields = new int[]{0, 0, 0};
        GMTOffsetPatternType[] gMTOffsetPatternTypeArr = PARSE_GMT_OFFSET_TYPES;
        int length = gMTOffsetPatternTypeArr.length;
        int outLen2 = 0;
        int i3 = 0;
        while (true) {
            i = -1;
            if (i3 >= length) {
                break;
            }
            gmtPatType = gMTOffsetPatternTypeArr[i3];
            GMTOffsetPatternType gmtPatType2 = gmtPatType;
            int i4 = i3;
            i2 = length;
            GMTOffsetPatternType[] gMTOffsetPatternTypeArr2 = gMTOffsetPatternTypeArr;
            outLen2 = parseOffsetFieldsWithPattern(text, start, this._gmtOffsetPatternItems[gmtPatType.ordinal()], false, fields);
            if (outLen2 > 0) {
                outLen = gmtPatType2.isPositive() ? 1 : -1;
                offsetH = fields[0];
                offsetM = fields[1];
                offsetS = fields[2];
            } else {
                i3 = i4 + 1;
                gMTOffsetPatternTypeArr = gMTOffsetPatternTypeArr2;
                length = i2;
            }
        }
        int sign = outLen;
        outLen = outLen2;
        if (outLen > 0 && this._abuttingOffsetHoursAndMinutes) {
            int tmpLen;
            outLen2 = 1;
            gMTOffsetPatternTypeArr = PARSE_GMT_OFFSET_TYPES;
            length = gMTOffsetPatternTypeArr.length;
            int tmpLen2 = 0;
            i3 = 0;
            while (i3 < length) {
                gmtPatType = gMTOffsetPatternTypeArr[i3];
                GMTOffsetPatternType gmtPatType3 = gmtPatType;
                i2 = i3;
                int i5 = length;
                GMTOffsetPatternType[] gMTOffsetPatternTypeArr3 = gMTOffsetPatternTypeArr;
                tmpLen2 = parseOffsetFieldsWithPattern(text, start, this._gmtOffsetPatternItems[gmtPatType.ordinal()], true, fields);
                if (tmpLen2 > 0) {
                    if (gmtPatType3.isPositive()) {
                        i = 1;
                    }
                    outLen2 = i;
                    tmpLen = tmpLen2;
                    if (tmpLen > outLen) {
                        outLen = tmpLen;
                        sign = outLen2;
                        offsetH = fields[0];
                        offsetM = fields[1];
                        offsetS = fields[2];
                    }
                } else {
                    i3 = i2 + 1;
                    gMTOffsetPatternTypeArr = gMTOffsetPatternTypeArr3;
                    length = i5;
                }
            }
            tmpLen = tmpLen2;
            if (tmpLen > outLen) {
            }
        }
        if (iArr != null && iArr.length >= 1) {
            iArr[0] = outLen;
        }
        if (outLen > 0) {
            return (((((offsetH * 60) + offsetM) * 60) + offsetS) * 1000) * sign;
        }
        return 0;
    }

    private int parseOffsetFieldsWithPattern(String text, int start, Object[] patternItems, boolean forceSingleHourDigit, int[] fields) {
        Object[] objArr = patternItems;
        int i = 2;
        fields[2] = 0;
        fields[1] = 0;
        fields[0] = 0;
        boolean failed = false;
        int i2 = 0;
        int offsetM = 0;
        int offsetH = 0;
        int idx = start;
        int[] tmpParsedLen = new int[]{0};
        int offsetS = i2;
        while (i2 < objArr.length) {
            if (objArr[i2] instanceof String) {
                String str;
                String patStr = objArr[i2];
                int len = patStr.length();
                int patIdx = 0;
                if (i2 != 0 || idx >= text.length()) {
                    str = text;
                } else {
                    str = text;
                    if (!PatternProps.isWhiteSpace(str.codePointAt(idx))) {
                        while (len > 0) {
                            int cp = patStr.codePointAt(patIdx);
                            if (!PatternProps.isWhiteSpace(cp)) {
                                break;
                            }
                            int cpLen = Character.charCount(cp);
                            len -= cpLen;
                            patIdx += cpLen;
                        }
                    }
                }
                int len2 = len;
                if (!str.regionMatches(true, idx, patStr, patIdx, len2)) {
                    failed = true;
                    break;
                }
                idx += len2;
            } else {
                GMTOffsetField field = objArr[i2];
                char fieldType = field.getType();
                if (fieldType == 'H') {
                    int maxDigits = forceSingleHourDigit ? 1 : i;
                    offsetH = parseOffsetFieldWithLocalizedDigits(text, idx, 1, maxDigits, 0, 23, tmpParsedLen);
                } else {
                    char fieldType2 = fieldType;
                    GMTOffsetField gMTOffsetField = field;
                    if (fieldType2 == 'm') {
                        offsetM = parseOffsetFieldWithLocalizedDigits(text, idx, 2, 2, 0, 59, tmpParsedLen);
                    } else if (fieldType2 == 's') {
                        offsetS = parseOffsetFieldWithLocalizedDigits(text, idx, 2, 2, 0, 59, tmpParsedLen);
                    }
                }
                if (tmpParsedLen[0] == 0) {
                    failed = true;
                    break;
                }
                idx += tmpParsedLen[0];
            }
            i2++;
            i = 2;
        }
        if (failed) {
            return 0;
        }
        fields[0] = offsetH;
        fields[1] = offsetM;
        fields[2] = offsetS;
        return idx - start;
    }

    private int parseOffsetDefaultLocalizedGMT(String text, int start, int[] parsedLen) {
        String str = text;
        int idx = start;
        int offset = 0;
        int parsed = 0;
        int gmtLen = 0;
        for (String gmt : ALT_GMT_STRINGS) {
            int len = gmt.length();
            if (str.regionMatches(true, idx, gmt, 0, len)) {
                gmtLen = len;
                break;
            }
        }
        if (gmtLen != 0) {
            idx += gmtLen;
            if (idx + 1 < text.length()) {
                int sign;
                char c = str.charAt(idx);
                if (c == '+') {
                    sign = 1;
                } else if (c == '-') {
                    sign = -1;
                }
                idx++;
                int[] lenWithSep = new int[]{0};
                int offsetWithSep = parseDefaultOffsetFields(str, idx, 58, lenWithSep);
                if (lenWithSep[0] == text.length() - idx) {
                    idx += lenWithSep[0];
                    offset = offsetWithSep * sign;
                } else {
                    int[] lenAbut = new int[]{0};
                    int offsetAbut = parseAbuttingOffsetFields(str, idx, lenAbut);
                    if (lenWithSep[0] > lenAbut[0]) {
                        offset = offsetWithSep * sign;
                        idx += lenWithSep[0];
                    } else {
                        offset = offsetAbut * sign;
                        idx += lenAbut[0];
                    }
                }
                parsed = idx - start;
            }
        }
        parsedLen[0] = parsed;
        return offset;
    }

    private int parseDefaultOffsetFields(String text, int start, char separator, int[] parsedLen) {
        String str = text;
        int i = start;
        char c = separator;
        int max = text.length();
        int idx = i;
        int[] len = new int[]{0};
        int min = 0;
        int sec = 0;
        int hour = parseOffsetFieldWithLocalizedDigits(str, idx, 1, 2, 0, 23, len);
        if (len[0] != 0) {
            idx += len[0];
            if (idx + 1 < max && str.charAt(idx) == c) {
                min = parseOffsetFieldWithLocalizedDigits(str, idx + 1, 2, 2, 0, 59, len);
                if (len[0] != 0) {
                    idx += len[0] + 1;
                    if (idx + 1 < max && str.charAt(idx) == c) {
                        sec = parseOffsetFieldWithLocalizedDigits(str, idx + 1, 2, 2, 0, 59, len);
                        if (len[0] != 0) {
                            idx += 1 + len[0];
                        }
                    }
                }
            }
        }
        int sec2 = sec;
        if (idx == i) {
            parsedLen[0] = 0;
            return 0;
        }
        parsedLen[0] = idx - i;
        return ((3600000 * hour) + (60000 * min)) + (sec2 * 1000);
    }

    private int parseAbuttingOffsetFields(String text, int start, int[] parsedLen) {
        int i;
        int[] digits = new int[6];
        int[] parsed = new int[6];
        int i2 = 1;
        int[] len = new int[]{0};
        int numDigits = 0;
        int idx = start;
        for (i = 0; i < 6; i++) {
            digits[i] = parseSingleLocalizedDigit(text, idx, len);
            if (digits[i] < 0) {
                break;
            }
            idx += len[0];
            parsed[i] = idx - start;
            numDigits++;
        }
        String str = text;
        if (numDigits == 0) {
            parsedLen[0] = 0;
            return 0;
        }
        int offset = 0;
        while (numDigits > 0) {
            i = 0;
            int min = 0;
            int sec = 0;
            switch (numDigits) {
                case 1:
                    i = digits[0];
                    break;
                case 2:
                    i = (digits[0] * 10) + digits[i2];
                    break;
                case 3:
                    i = digits[0];
                    min = (digits[i2] * 10) + digits[2];
                    break;
                case 4:
                    i = (digits[0] * 10) + digits[i2];
                    min = (digits[2] * 10) + digits[3];
                    break;
                case 5:
                    i = digits[0];
                    min = (digits[i2] * 10) + digits[2];
                    sec = (digits[3] * 10) + digits[4];
                    break;
                case 6:
                    i = (digits[0] * 10) + digits[i2];
                    min = (digits[2] * 10) + digits[3];
                    sec = (digits[4] * 10) + digits[5];
                    break;
            }
            if (i > 23 || min > 59 || sec > 59) {
                numDigits--;
                i2 = 1;
            } else {
                offset = ((3600000 * i) + (60000 * min)) + (sec * 1000);
                parsedLen[0] = parsed[numDigits - 1];
                return offset;
            }
        }
        return offset;
    }

    private int parseOffsetFieldWithLocalizedDigits(String text, int start, int minDigits, int maxDigits, int minVal, int maxVal, int[] parsedLen) {
        parsedLen[0] = 0;
        int decVal = 0;
        int numDigits = 0;
        int idx = start;
        int[] digitLen = new int[]{0};
        while (idx < text.length() && numDigits < maxDigits) {
            int digit = parseSingleLocalizedDigit(text, idx, digitLen);
            if (digit < 0) {
                break;
            }
            int tmpVal = (decVal * 10) + digit;
            if (tmpVal > maxVal) {
                break;
            }
            decVal = tmpVal;
            numDigits++;
            idx += digitLen[0];
        }
        if (numDigits < minDigits || decVal < minVal) {
            return -1;
        }
        parsedLen[0] = idx - start;
        return decVal;
    }

    private int parseSingleLocalizedDigit(String text, int start, int[] len) {
        int digit = -1;
        len[0] = 0;
        if (start < text.length()) {
            int cp = Character.codePointAt(text, start);
            for (int i = 0; i < this._gmtOffsetDigits.length; i++) {
                if (cp == this._gmtOffsetDigits[i].codePointAt(0)) {
                    digit = i;
                    break;
                }
            }
            if (digit < 0) {
                digit = UCharacter.digit(cp);
            }
            if (digit >= 0) {
                len[0] = Character.charCount(cp);
            }
        }
        return digit;
    }

    private static String[] toCodePoints(String str) {
        int offset = 0;
        int len = str.codePointCount(0, str.length());
        String[] codePoints = new String[len];
        for (int i = 0; i < len; i++) {
            int codeLen = Character.charCount(str.codePointAt(offset));
            codePoints[i] = str.substring(offset, offset + codeLen);
            offset += codeLen;
        }
        return codePoints;
    }

    private static int parseOffsetISO8601(String text, ParsePosition pos, boolean extendedOnly, Output<Boolean> hasDigitOffset) {
        if (hasDigitOffset != null) {
            hasDigitOffset.value = Boolean.valueOf(false);
        }
        int start = pos.getIndex();
        if (start >= text.length()) {
            pos.setErrorIndex(start);
            return 0;
        }
        char firstChar = text.charAt(start);
        if (Character.toUpperCase(firstChar) == ISO8601_UTC.charAt(0)) {
            pos.setIndex(start + 1);
            return 0;
        }
        int sign;
        if (firstChar == '+') {
            sign = 1;
        } else if (firstChar == '-') {
            sign = -1;
        } else {
            pos.setErrorIndex(start);
            return 0;
        }
        ParsePosition posOffset = new ParsePosition(start + 1);
        int offset = parseAsciiOffsetFields(text, posOffset, 58, OffsetFields.H, OffsetFields.HMS);
        if (posOffset.getErrorIndex() == -1 && !extendedOnly && posOffset.getIndex() - start <= 3) {
            ParsePosition posBasic = new ParsePosition(start + 1);
            int tmpOffset = parseAbuttingAsciiOffsetFields(text, posBasic, OffsetFields.H, OffsetFields.HMS, false);
            if (posBasic.getErrorIndex() == -1 && posBasic.getIndex() > posOffset.getIndex()) {
                offset = tmpOffset;
                posOffset.setIndex(posBasic.getIndex());
            }
        }
        if (posOffset.getErrorIndex() != -1) {
            pos.setErrorIndex(start);
            return 0;
        }
        pos.setIndex(posOffset.getIndex());
        if (hasDigitOffset != null) {
            hasDigitOffset.value = Boolean.valueOf(true);
        }
        return sign * offset;
    }

    /* JADX WARNING: Removed duplicated region for block: B:40:0x00d0  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00cc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int parseAbuttingAsciiOffsetFields(String text, ParsePosition pos, OffsetFields minFields, OffsetFields maxFields, boolean fixedHourWidth) {
        ParsePosition parsePosition = pos;
        int start = pos.getIndex();
        int i = 1;
        int minDigits = ((minFields.ordinal() + 1) * 2) - (fixedHourWidth ^ 1);
        int[] digits = new int[((maxFields.ordinal() + 1) * 2)];
        int numDigits = 0;
        int idx = start;
        while (numDigits < digits.length && idx < text.length()) {
            int digit = ASCII_DIGITS.indexOf(text.charAt(idx));
            if (digit < 0) {
                break;
            }
            digits[numDigits] = digit;
            numDigits++;
            idx++;
        }
        String str = text;
        if (fixedHourWidth && (numDigits & 1) != 0) {
            numDigits--;
        }
        if (numDigits < minDigits) {
            parsePosition.setErrorIndex(start);
            return 0;
        }
        int sec = 0;
        int min = 0;
        int hour = 0;
        int numDigits2 = numDigits;
        boolean bParsed = false;
        while (numDigits2 >= minDigits) {
            switch (numDigits2) {
                case 1:
                    hour = digits[0];
                    break;
                case 2:
                    hour = (digits[0] * 10) + digits[i];
                    break;
                case 3:
                    hour = digits[0];
                    min = (digits[i] * 10) + digits[2];
                    break;
                case 4:
                    hour = (digits[0] * 10) + digits[i];
                    min = (digits[2] * 10) + digits[3];
                    break;
                case 5:
                    hour = digits[0];
                    min = (digits[i] * 10) + digits[2];
                    sec = (digits[3] * 10) + digits[4];
                    break;
                case 6:
                    hour = (digits[0] * 10) + digits[i];
                    min = (digits[2] * 10) + digits[3];
                    sec = (digits[4] * 10) + digits[5];
                    break;
            }
            if (hour > 23 || min > 59 || sec > 59) {
                numDigits2 -= fixedHourWidth ? 2 : 1;
                sec = 0;
                min = 0;
                hour = 0;
                i = 1;
            } else {
                bParsed = true;
                if (bParsed) {
                    parsePosition.setErrorIndex(start);
                    return 0;
                }
                parsePosition.setIndex(start + numDigits2);
                return ((((hour * 60) + min) * 60) + sec) * 1000;
            }
        }
        if (bParsed) {
        }
    }

    private static int parseAsciiOffsetFields(String text, ParsePosition pos, char sep, OffsetFields minFields, OffsetFields maxFields) {
        int idx;
        ParsePosition parsePosition = pos;
        int start = pos.getIndex();
        int[] fieldVal = new int[]{0, 0, 0};
        int[] fieldLen = new int[]{0, -1, -1};
        int fieldIdx = 0;
        for (idx = start; idx < text.length() && fieldIdx <= maxFields.ordinal(); idx++) {
            char c = text.charAt(idx);
            if (c == sep) {
                if (fieldIdx == 0) {
                    if (fieldLen[0] == 0) {
                        break;
                    }
                    fieldIdx++;
                } else if (fieldLen[fieldIdx] != -1) {
                    break;
                } else {
                    fieldLen[fieldIdx] = 0;
                }
            } else if (fieldLen[fieldIdx] == -1) {
                break;
            } else {
                int digit = ASCII_DIGITS.indexOf(c);
                if (digit < 0) {
                    break;
                }
                fieldVal[fieldIdx] = (fieldVal[fieldIdx] * 10) + digit;
                fieldLen[fieldIdx] = fieldLen[fieldIdx] + 1;
                if (fieldLen[fieldIdx] >= 2) {
                    fieldIdx++;
                }
            }
        }
        String str = text;
        char c2 = sep;
        fieldIdx = 0;
        idx = 0;
        OffsetFields parsedFields = null;
        if (fieldLen[0] != 0) {
            if (fieldVal[0] > 23) {
                fieldIdx = (fieldVal[0] / 10) * 3600000;
                parsedFields = OffsetFields.H;
                idx = 1;
            } else {
                fieldIdx = fieldVal[0] * 3600000;
                idx = fieldLen[0];
                parsedFields = OffsetFields.H;
                if (fieldLen[1] == 2 && fieldVal[1] <= 59) {
                    fieldIdx += fieldVal[1] * 60000;
                    idx += fieldLen[1] + 1;
                    parsedFields = OffsetFields.HM;
                    if (fieldLen[2] == 2 && fieldVal[2] <= 59) {
                        fieldIdx += fieldVal[2] * 1000;
                        idx += 1 + fieldLen[2];
                        parsedFields = OffsetFields.HMS;
                    }
                }
            }
        }
        if (parsedFields == null || parsedFields.ordinal() < minFields.ordinal()) {
            parsePosition.setErrorIndex(start);
            return 0;
        }
        parsePosition.setIndex(start + idx);
        return fieldIdx;
    }

    private static String parseZoneID(String text, ParsePosition pos) {
        if (ZONE_ID_TRIE == null) {
            synchronized (TimeZoneFormat.class) {
                if (ZONE_ID_TRIE == null) {
                    TextTrieMap<String> trie = new TextTrieMap(true);
                    for (String id : TimeZone.getAvailableIDs()) {
                        trie.put(id, id);
                    }
                    ZONE_ID_TRIE = trie;
                }
            }
        }
        int[] matchLen = new int[]{0};
        Iterator<String> itr = ZONE_ID_TRIE.get(text, pos.getIndex(), matchLen);
        if (itr != null) {
            String resolvedID = (String) itr.next();
            pos.setIndex(pos.getIndex() + matchLen[0]);
            return resolvedID;
        }
        pos.setErrorIndex(pos.getIndex());
        return null;
    }

    private static String parseShortZoneID(String text, ParsePosition pos) {
        if (SHORT_ZONE_ID_TRIE == null) {
            synchronized (TimeZoneFormat.class) {
                if (SHORT_ZONE_ID_TRIE == null) {
                    TextTrieMap<String> trie = new TextTrieMap(true);
                    for (String id : TimeZone.getAvailableIDs(SystemTimeZoneType.CANONICAL, null, null)) {
                        String shortID = ZoneMeta.getShortID(id);
                        if (shortID != null) {
                            trie.put(shortID, id);
                        }
                    }
                    trie.put(UNKNOWN_SHORT_ZONE_ID, "Etc/Unknown");
                    SHORT_ZONE_ID_TRIE = trie;
                }
            }
        }
        int[] matchLen = new int[]{0};
        Iterator<String> itr = SHORT_ZONE_ID_TRIE.get(text, pos.getIndex(), matchLen);
        if (itr != null) {
            String resolvedID = (String) itr.next();
            pos.setIndex(pos.getIndex() + matchLen[0]);
            return resolvedID;
        }
        pos.setErrorIndex(pos.getIndex());
        return null;
    }

    private String parseExemplarLocation(String text, ParsePosition pos) {
        int startIdx = pos.getIndex();
        int parsedPos = -1;
        String tzID = null;
        Collection<MatchInfo> exemplarMatches = this._tznames.find(text, startIdx, EnumSet.of(NameType.EXEMPLAR_LOCATION));
        if (exemplarMatches != null) {
            MatchInfo exemplarMatch = null;
            for (MatchInfo match : exemplarMatches) {
                if (match.matchLength() + startIdx > parsedPos) {
                    exemplarMatch = match;
                    parsedPos = match.matchLength() + startIdx;
                }
            }
            if (exemplarMatch != null) {
                tzID = getTimeZoneID(exemplarMatch.tzID(), exemplarMatch.mzID());
                pos.setIndex(parsedPos);
            }
        }
        if (tzID == null) {
            pos.setErrorIndex(startIdx);
        }
        return tzID;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        PutField fields = oos.putFields();
        fields.put("_locale", this._locale);
        fields.put("_tznames", this._tznames);
        fields.put("_gmtPattern", this._gmtPattern);
        fields.put("_gmtOffsetPatterns", this._gmtOffsetPatterns);
        fields.put("_gmtOffsetDigits", this._gmtOffsetDigits);
        fields.put("_gmtZeroFormat", this._gmtZeroFormat);
        fields.put("_parseAllStyles", this._parseAllStyles);
        oos.writeFields();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        GetField fields = ois.readFields();
        this._locale = (ULocale) fields.get("_locale", null);
        if (this._locale != null) {
            this._tznames = (TimeZoneNames) fields.get("_tznames", null);
            if (this._tznames != null) {
                this._gmtPattern = (String) fields.get("_gmtPattern", null);
                if (this._gmtPattern != null) {
                    String[] tmpGmtOffsetPatterns = (String[]) fields.get("_gmtOffsetPatterns", null);
                    if (tmpGmtOffsetPatterns == null) {
                        throw new InvalidObjectException("Missing field: gmtOffsetPatterns");
                    } else if (tmpGmtOffsetPatterns.length >= 4) {
                        this._gmtOffsetPatterns = new String[6];
                        if (tmpGmtOffsetPatterns.length == 4) {
                            for (int i = 0; i < 4; i++) {
                                this._gmtOffsetPatterns[i] = tmpGmtOffsetPatterns[i];
                            }
                            this._gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_H.ordinal()] = truncateOffsetPattern(this._gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HM.ordinal()]);
                            this._gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_H.ordinal()] = truncateOffsetPattern(this._gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HM.ordinal()]);
                        } else {
                            this._gmtOffsetPatterns = tmpGmtOffsetPatterns;
                        }
                        this._gmtOffsetDigits = (String[]) fields.get("_gmtOffsetDigits", null);
                        if (this._gmtOffsetDigits == null) {
                            throw new InvalidObjectException("Missing field: gmtOffsetDigits");
                        } else if (this._gmtOffsetDigits.length == 10) {
                            this._gmtZeroFormat = (String) fields.get("_gmtZeroFormat", null);
                            if (this._gmtZeroFormat != null) {
                                this._parseAllStyles = fields.get("_parseAllStyles", false);
                                if (fields.defaulted("_parseAllStyles")) {
                                    throw new InvalidObjectException("Missing field: parseAllStyles");
                                }
                                if (this._tznames instanceof TimeZoneNamesImpl) {
                                    this._tznames = TimeZoneNames.getInstance(this._locale);
                                    this._gnames = null;
                                } else {
                                    this._gnames = new TimeZoneGenericNames(this._locale, this._tznames);
                                }
                                initGMTPattern(this._gmtPattern);
                                initGMTOffsetPatterns(this._gmtOffsetPatterns);
                                return;
                            }
                            throw new InvalidObjectException("Missing field: gmtZeroFormat");
                        } else {
                            throw new InvalidObjectException("Incompatible field: gmtOffsetDigits");
                        }
                    } else {
                        throw new InvalidObjectException("Incompatible field: gmtOffsetPatterns");
                    }
                }
                throw new InvalidObjectException("Missing field: gmtPattern");
            }
            throw new InvalidObjectException("Missing field: tznames");
        }
        throw new InvalidObjectException("Missing field: locale");
    }

    public boolean isFrozen() {
        return this._frozen;
    }

    public TimeZoneFormat freeze() {
        this._frozen = true;
        return this;
    }

    public TimeZoneFormat cloneAsThawed() {
        TimeZoneFormat copy = (TimeZoneFormat) super.clone();
        copy._frozen = false;
        return copy;
    }
}
