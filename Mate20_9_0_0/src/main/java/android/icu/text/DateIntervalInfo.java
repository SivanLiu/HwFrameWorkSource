package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SimpleCache;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.impl.UResource.Table;
import android.icu.impl.UResource.Value;
import android.icu.impl.Utility;
import android.icu.impl.number.Padder;
import android.icu.util.Calendar;
import android.icu.util.Freezable;
import android.icu.util.ICUCloneNotSupportedException;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Set;

public class DateIntervalInfo implements Cloneable, Freezable<DateIntervalInfo>, Serializable {
    static final String[] CALENDAR_FIELD_TO_PATTERN_LETTER = new String[]{"G", DateFormat.YEAR, DateFormat.NUM_MONTH, "w", "W", DateFormat.DAY, "D", DateFormat.ABBR_WEEKDAY, "F", "a", "h", DateFormat.HOUR24, DateFormat.MINUTE, DateFormat.SECOND, "S", DateFormat.ABBR_SPECIFIC_TZ, Padder.FALLBACK_PADDING_STRING, "Y", "e", "u", "g", "A", Padder.FALLBACK_PADDING_STRING, Padder.FALLBACK_PADDING_STRING};
    private static String CALENDAR_KEY = "calendar";
    private static final ICUCache<String, DateIntervalInfo> DIICACHE = new SimpleCache();
    private static String EARLIEST_FIRST_PREFIX = "earliestFirst:";
    private static String FALLBACK_STRING = "fallback";
    private static String INTERVAL_FORMATS_KEY = "intervalFormats";
    private static String LATEST_FIRST_PREFIX = "latestFirst:";
    private static final int MINIMUM_SUPPORTED_CALENDAR_FIELD = 13;
    static final int currentSerialVersion = 1;
    private static final long serialVersionUID = 1;
    private String fFallbackIntervalPattern;
    private boolean fFirstDateInPtnIsLaterDate;
    private Map<String, Map<String, PatternInfo>> fIntervalPatterns;
    private transient boolean fIntervalPatternsReadOnly;
    private volatile transient boolean frozen;

    public static final class PatternInfo implements Cloneable, Serializable {
        static final int currentSerialVersion = 1;
        private static final long serialVersionUID = 1;
        private final boolean fFirstDateInPtnIsLaterDate;
        private final String fIntervalPatternFirstPart;
        private final String fIntervalPatternSecondPart;

        public PatternInfo(String firstPart, String secondPart, boolean firstDateInPtnIsLaterDate) {
            this.fIntervalPatternFirstPart = firstPart;
            this.fIntervalPatternSecondPart = secondPart;
            this.fFirstDateInPtnIsLaterDate = firstDateInPtnIsLaterDate;
        }

        public String getFirstPart() {
            return this.fIntervalPatternFirstPart;
        }

        public String getSecondPart() {
            return this.fIntervalPatternSecondPart;
        }

        public boolean firstDateInPtnIsLaterDate() {
            return this.fFirstDateInPtnIsLaterDate;
        }

        public boolean equals(Object a) {
            boolean z = false;
            if (!(a instanceof PatternInfo)) {
                return false;
            }
            PatternInfo patternInfo = (PatternInfo) a;
            if (Utility.objectEquals(this.fIntervalPatternFirstPart, patternInfo.fIntervalPatternFirstPart) && Utility.objectEquals(this.fIntervalPatternSecondPart, patternInfo.fIntervalPatternSecondPart) && this.fFirstDateInPtnIsLaterDate == patternInfo.fFirstDateInPtnIsLaterDate) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            int hash = this.fIntervalPatternFirstPart != null ? this.fIntervalPatternFirstPart.hashCode() : 0;
            if (this.fIntervalPatternSecondPart != null) {
                hash ^= this.fIntervalPatternSecondPart.hashCode();
            }
            if (this.fFirstDateInPtnIsLaterDate) {
                return ~hash;
            }
            return hash;
        }

        @Deprecated
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{first=«");
            stringBuilder.append(this.fIntervalPatternFirstPart);
            stringBuilder.append("», second=«");
            stringBuilder.append(this.fIntervalPatternSecondPart);
            stringBuilder.append("», reversed:");
            stringBuilder.append(this.fFirstDateInPtnIsLaterDate);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private static final class DateIntervalSink extends Sink {
        private static final String ACCEPTED_PATTERN_LETTERS = "yMdahHms";
        private static final String DATE_INTERVAL_PATH_PREFIX;
        private static final String DATE_INTERVAL_PATH_SUFFIX;
        DateIntervalInfo dateIntervalInfo;
        String nextCalendarType;

        public DateIntervalSink(DateIntervalInfo dateIntervalInfo) {
            this.dateIntervalInfo = dateIntervalInfo;
        }

        public void put(Key key, Value value, boolean noFallback) {
            Table dateIntervalData = value.getTable();
            int j = 0;
            for (int i = 0; dateIntervalData.getKeyAndValue(i, key, value); i++) {
                if (key.contentEquals(DateIntervalInfo.INTERVAL_FORMATS_KEY)) {
                    if (value.getType() == 3) {
                        this.nextCalendarType = getCalendarTypeFromPath(value.getAliasString());
                        return;
                    } else if (value.getType() == 2) {
                        Table skeletonData = value.getTable();
                        while (skeletonData.getKeyAndValue(j, key, value)) {
                            if (value.getType() == 2) {
                                processSkeletonTable(key, value);
                            }
                            j++;
                        }
                        return;
                    }
                }
            }
        }

        public void processSkeletonTable(Key key, Value value) {
            String currentSkeleton = key.toString();
            Table patternData = value.getTable();
            for (int k = 0; patternData.getKeyAndValue(k, key, value); k++) {
                if (value.getType() == 0) {
                    CharSequence patternLetter = validateAndProcessPatternLetter(key);
                    if (patternLetter != null) {
                        setIntervalPatternIfAbsent(currentSkeleton, patternLetter.toString(), value);
                    }
                }
            }
        }

        public String getAndResetNextCalendarType() {
            String tmpCalendarType = this.nextCalendarType;
            this.nextCalendarType = null;
            return tmpCalendarType;
        }

        static {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/LOCALE/");
            stringBuilder.append(DateIntervalInfo.CALENDAR_KEY);
            stringBuilder.append("/");
            DATE_INTERVAL_PATH_PREFIX = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append("/");
            stringBuilder.append(DateIntervalInfo.INTERVAL_FORMATS_KEY);
            DATE_INTERVAL_PATH_SUFFIX = stringBuilder.toString();
        }

        private String getCalendarTypeFromPath(String path) {
            if (path.startsWith(DATE_INTERVAL_PATH_PREFIX) && path.endsWith(DATE_INTERVAL_PATH_SUFFIX)) {
                return path.substring(DATE_INTERVAL_PATH_PREFIX.length(), path.length() - DATE_INTERVAL_PATH_SUFFIX.length());
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Malformed 'intervalFormat' alias path: ");
            stringBuilder.append(path);
            throw new ICUException(stringBuilder.toString());
        }

        private CharSequence validateAndProcessPatternLetter(CharSequence patternLetter) {
            if (patternLetter.length() != 1) {
                return null;
            }
            char letter = patternLetter.charAt(0);
            if (ACCEPTED_PATTERN_LETTERS.indexOf(letter) < 0) {
                return null;
            }
            if (letter == DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[11].charAt(0)) {
                patternLetter = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[10];
            }
            return patternLetter;
        }

        private void setIntervalPatternIfAbsent(String currentSkeleton, String lrgDiffCalUnit, Value intervalPattern) {
            Map<String, PatternInfo> patternsOfOneSkeleton = (Map) this.dateIntervalInfo.fIntervalPatterns.get(currentSkeleton);
            if (patternsOfOneSkeleton == null || !patternsOfOneSkeleton.containsKey(lrgDiffCalUnit)) {
                this.dateIntervalInfo.setIntervalPatternInternally(currentSkeleton, lrgDiffCalUnit, intervalPattern.toString());
            }
        }
    }

    @Deprecated
    public DateIntervalInfo() {
        this.fFirstDateInPtnIsLaterDate = false;
        this.fIntervalPatterns = null;
        this.frozen = false;
        this.fIntervalPatternsReadOnly = false;
        this.fIntervalPatterns = new HashMap();
        this.fFallbackIntervalPattern = "{0} – {1}";
    }

    public DateIntervalInfo(ULocale locale) {
        this.fFirstDateInPtnIsLaterDate = false;
        this.fIntervalPatterns = null;
        this.frozen = false;
        this.fIntervalPatternsReadOnly = false;
        initializeData(locale);
    }

    public DateIntervalInfo(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    private void initializeData(ULocale locale) {
        String key = locale.toString();
        DateIntervalInfo dii = (DateIntervalInfo) DIICACHE.get(key);
        if (dii == null) {
            setup(locale);
            this.fIntervalPatternsReadOnly = true;
            DIICACHE.put(key, ((DateIntervalInfo) clone()).freeze());
            return;
        }
        initializeFromReadOnlyPatterns(dii);
    }

    private void initializeFromReadOnlyPatterns(DateIntervalInfo dii) {
        this.fFallbackIntervalPattern = dii.fFallbackIntervalPattern;
        this.fFirstDateInPtnIsLaterDate = dii.fFirstDateInPtnIsLaterDate;
        this.fIntervalPatterns = dii.fIntervalPatterns;
        this.fIntervalPatternsReadOnly = true;
    }

    private void setup(ULocale locale) {
        this.fIntervalPatterns = new HashMap(19);
        this.fFallbackIntervalPattern = "{0} – {1}";
        try {
            String calendarTypeToUse = locale.getKeywordValue("calendar");
            if (calendarTypeToUse == null) {
                calendarTypeToUse = Calendar.getKeywordValuesForLocale("calendar", locale, true)[0];
            }
            if (calendarTypeToUse == null) {
                calendarTypeToUse = "gregorian";
            }
            DateIntervalSink sink = new DateIntervalSink(this);
            ICUResourceBundle resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(CALENDAR_KEY);
            stringBuilder.append("/");
            stringBuilder.append(calendarTypeToUse);
            stringBuilder.append("/");
            stringBuilder.append(INTERVAL_FORMATS_KEY);
            stringBuilder.append("/");
            stringBuilder.append(FALLBACK_STRING);
            setFallbackIntervalPattern(resource.getStringWithFallback(stringBuilder.toString()));
            Set<String> loadedCalendarTypes = new HashSet();
            while (calendarTypeToUse != null) {
                if (loadedCalendarTypes.contains(calendarTypeToUse)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Loop in calendar type fallback: ");
                    stringBuilder2.append(calendarTypeToUse);
                    throw new ICUException(stringBuilder2.toString());
                }
                loadedCalendarTypes.add(calendarTypeToUse);
                String pathToIntervalFormats = new StringBuilder();
                pathToIntervalFormats.append(CALENDAR_KEY);
                pathToIntervalFormats.append("/");
                pathToIntervalFormats.append(calendarTypeToUse);
                resource.getAllItemsWithFallback(pathToIntervalFormats.toString(), sink);
                calendarTypeToUse = sink.getAndResetNextCalendarType();
            }
        } catch (MissingResourceException e) {
        }
    }

    private static int splitPatternInto2Part(String intervalPattern) {
        char prevCh = 0;
        int count = 0;
        int[] patternRepeated = new int[58];
        boolean foundRepetition = false;
        boolean inQuote = false;
        int i = 0;
        while (i < intervalPattern.length()) {
            char ch = intervalPattern.charAt(i);
            if (ch != prevCh && count > 0) {
                if (patternRepeated[prevCh - 65] != 0) {
                    foundRepetition = true;
                    break;
                }
                patternRepeated[prevCh - 65] = 1;
                count = 0;
            }
            if (ch == PatternTokenizer.SINGLE_QUOTE) {
                if (i + 1 >= intervalPattern.length() || intervalPattern.charAt(i + 1) != PatternTokenizer.SINGLE_QUOTE) {
                    inQuote = !inQuote;
                } else {
                    i++;
                }
            } else if (!inQuote && ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))) {
                prevCh = ch;
                count++;
            }
            i++;
        }
        if (count > 0 && !foundRepetition && patternRepeated[prevCh - 65] == 0) {
            count = 0;
        }
        return i - count;
    }

    public void setIntervalPattern(String skeleton, int lrgDiffCalUnit, String intervalPattern) {
        if (this.frozen) {
            throw new UnsupportedOperationException("no modification is allowed after DII is frozen");
        } else if (lrgDiffCalUnit <= 13) {
            if (this.fIntervalPatternsReadOnly) {
                this.fIntervalPatterns = cloneIntervalPatterns(this.fIntervalPatterns);
                this.fIntervalPatternsReadOnly = false;
            }
            PatternInfo ptnInfo = setIntervalPatternInternally(skeleton, CALENDAR_FIELD_TO_PATTERN_LETTER[lrgDiffCalUnit], intervalPattern);
            if (lrgDiffCalUnit == 11) {
                setIntervalPattern(skeleton, CALENDAR_FIELD_TO_PATTERN_LETTER[9], ptnInfo);
                setIntervalPattern(skeleton, CALENDAR_FIELD_TO_PATTERN_LETTER[10], ptnInfo);
            } else if (lrgDiffCalUnit == 5 || lrgDiffCalUnit == 7) {
                setIntervalPattern(skeleton, CALENDAR_FIELD_TO_PATTERN_LETTER[5], ptnInfo);
            }
        } else {
            throw new IllegalArgumentException("calendar field is larger than MINIMUM_SUPPORTED_CALENDAR_FIELD");
        }
    }

    private PatternInfo setIntervalPatternInternally(String skeleton, String lrgDiffCalUnit, String intervalPattern) {
        Map<String, PatternInfo> patternsOfOneSkeleton = (Map) this.fIntervalPatterns.get(skeleton);
        boolean emptyHash = false;
        if (patternsOfOneSkeleton == null) {
            patternsOfOneSkeleton = new HashMap();
            emptyHash = true;
        }
        boolean order = this.fFirstDateInPtnIsLaterDate;
        if (intervalPattern.startsWith(LATEST_FIRST_PREFIX)) {
            order = true;
            intervalPattern = intervalPattern.substring(LATEST_FIRST_PREFIX.length(), intervalPattern.length());
        } else if (intervalPattern.startsWith(EARLIEST_FIRST_PREFIX)) {
            order = false;
            intervalPattern = intervalPattern.substring(EARLIEST_FIRST_PREFIX.length(), intervalPattern.length());
        }
        PatternInfo itvPtnInfo = genPatternInfo(intervalPattern, order);
        patternsOfOneSkeleton.put(lrgDiffCalUnit, itvPtnInfo);
        if (emptyHash) {
            this.fIntervalPatterns.put(skeleton, patternsOfOneSkeleton);
        }
        return itvPtnInfo;
    }

    private void setIntervalPattern(String skeleton, String lrgDiffCalUnit, PatternInfo ptnInfo) {
        ((Map) this.fIntervalPatterns.get(skeleton)).put(lrgDiffCalUnit, ptnInfo);
    }

    @Deprecated
    public static PatternInfo genPatternInfo(String intervalPattern, boolean laterDateFirst) {
        int splitPoint = splitPatternInto2Part(intervalPattern);
        String firstPart = intervalPattern.substring(null, splitPoint);
        String secondPart = null;
        if (splitPoint < intervalPattern.length()) {
            secondPart = intervalPattern.substring(splitPoint, intervalPattern.length());
        }
        return new PatternInfo(firstPart, secondPart, laterDateFirst);
    }

    public PatternInfo getIntervalPattern(String skeleton, int field) {
        if (field <= 13) {
            Map<String, PatternInfo> patternsOfOneSkeleton = (Map) this.fIntervalPatterns.get(skeleton);
            if (patternsOfOneSkeleton != null) {
                PatternInfo intervalPattern = (PatternInfo) patternsOfOneSkeleton.get(CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
                if (intervalPattern != null) {
                    return intervalPattern;
                }
            }
            return null;
        }
        throw new IllegalArgumentException("no support for field less than SECOND");
    }

    public String getFallbackIntervalPattern() {
        return this.fFallbackIntervalPattern;
    }

    public void setFallbackIntervalPattern(String fallbackPattern) {
        if (this.frozen) {
            throw new UnsupportedOperationException("no modification is allowed after DII is frozen");
        }
        int firstPatternIndex = fallbackPattern.indexOf("{0}");
        int secondPatternIndex = fallbackPattern.indexOf("{1}");
        if (firstPatternIndex == -1 || secondPatternIndex == -1) {
            throw new IllegalArgumentException("no pattern {0} or pattern {1} in fallbackPattern");
        }
        if (firstPatternIndex > secondPatternIndex) {
            this.fFirstDateInPtnIsLaterDate = true;
        }
        this.fFallbackIntervalPattern = fallbackPattern;
    }

    public boolean getDefaultOrder() {
        return this.fFirstDateInPtnIsLaterDate;
    }

    public Object clone() {
        if (this.frozen) {
            return this;
        }
        return cloneUnfrozenDII();
    }

    private Object cloneUnfrozenDII() {
        try {
            DateIntervalInfo other = (DateIntervalInfo) super.clone();
            other.fFallbackIntervalPattern = this.fFallbackIntervalPattern;
            other.fFirstDateInPtnIsLaterDate = this.fFirstDateInPtnIsLaterDate;
            if (this.fIntervalPatternsReadOnly) {
                other.fIntervalPatterns = this.fIntervalPatterns;
                other.fIntervalPatternsReadOnly = true;
            } else {
                other.fIntervalPatterns = cloneIntervalPatterns(this.fIntervalPatterns);
                other.fIntervalPatternsReadOnly = false;
            }
            other.frozen = false;
            return other;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException("clone is not supported", e);
        }
    }

    private static Map<String, Map<String, PatternInfo>> cloneIntervalPatterns(Map<String, Map<String, PatternInfo>> patterns) {
        Map<String, Map<String, PatternInfo>> result = new HashMap();
        for (Entry<String, Map<String, PatternInfo>> skeletonEntry : patterns.entrySet()) {
            String skeleton = (String) skeletonEntry.getKey();
            Map<String, PatternInfo> patternsOfOneSkeleton = (Map) skeletonEntry.getValue();
            Map<String, PatternInfo> oneSetPtn = new HashMap();
            for (Entry<String, PatternInfo> calEntry : patternsOfOneSkeleton.entrySet()) {
                oneSetPtn.put((String) calEntry.getKey(), (PatternInfo) calEntry.getValue());
            }
            result.put(skeleton, oneSetPtn);
        }
        return result;
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    public DateIntervalInfo freeze() {
        this.fIntervalPatternsReadOnly = true;
        this.frozen = true;
        return this;
    }

    public DateIntervalInfo cloneAsThawed() {
        return (DateIntervalInfo) cloneUnfrozenDII();
    }

    static void parseSkeleton(String skeleton, int[] skeletonFieldWidth) {
        for (int i = 0; i < skeleton.length(); i++) {
            int charAt = skeleton.charAt(i) - 65;
            skeletonFieldWidth[charAt] = skeletonFieldWidth[charAt] + 1;
        }
    }

    private static boolean stringNumeric(int fieldWidth, int anotherFieldWidth, char patternLetter) {
        if (patternLetter != 'M' || ((fieldWidth > 2 || anotherFieldWidth <= 2) && (fieldWidth <= 2 || anotherFieldWidth > 2))) {
            return false;
        }
        return true;
    }

    BestMatchInfo getBestSkeleton(String inputSkeleton) {
        String bestSkeleton;
        int[] skeletonFieldWidth;
        String inputSkeleton2 = inputSkeleton;
        String bestSkeleton2 = inputSkeleton2;
        int[] inputSkeletonFieldWidth = new int[58];
        int[] skeletonFieldWidth2 = new int[58];
        boolean replaceZWithV = false;
        if (inputSkeleton2.indexOf(122) != -1) {
            inputSkeleton2 = inputSkeleton2.replace('z', 'v');
            replaceZWithV = true;
        }
        parseSkeleton(inputSkeleton2, inputSkeletonFieldWidth);
        int bestDistance = Integer.MAX_VALUE;
        int bestFieldDifference = 0;
        for (String skeleton : this.fIntervalPatterns.keySet()) {
            int i;
            String inputSkeleton3;
            int i2 = 0;
            for (i = 0; i < skeletonFieldWidth2.length; i++) {
                skeletonFieldWidth2[i] = 0;
            }
            parseSkeleton(skeleton, skeletonFieldWidth2);
            int distance = 0;
            i = 1;
            while (true) {
                inputSkeleton3 = inputSkeleton2;
                if (i2 >= inputSkeletonFieldWidth.length) {
                    break;
                }
                inputSkeleton2 = inputSkeletonFieldWidth[i2];
                bestSkeleton = bestSkeleton2;
                bestSkeleton2 = skeletonFieldWidth2[i2];
                if (inputSkeleton2 != bestSkeleton2) {
                    if (inputSkeleton2 == null) {
                        i = -1;
                        distance += 4096;
                    } else if (bestSkeleton2 == null) {
                        i = -1;
                        distance += 4096;
                    } else {
                        skeletonFieldWidth = skeletonFieldWidth2;
                        if (stringNumeric(inputSkeleton2, bestSkeleton2, (char) (i2 + 65))) {
                            distance += 256;
                        } else {
                            distance += Math.abs(inputSkeleton2 - bestSkeleton2);
                        }
                        i2++;
                        inputSkeleton2 = inputSkeleton3;
                        bestSkeleton2 = bestSkeleton;
                        skeletonFieldWidth2 = skeletonFieldWidth;
                    }
                }
                skeletonFieldWidth = skeletonFieldWidth2;
                i2++;
                inputSkeleton2 = inputSkeleton3;
                bestSkeleton2 = bestSkeleton;
                skeletonFieldWidth2 = skeletonFieldWidth;
            }
            bestSkeleton = bestSkeleton2;
            skeletonFieldWidth = skeletonFieldWidth2;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestFieldDifference = i;
                bestSkeleton2 = skeleton;
            } else {
                bestSkeleton2 = bestSkeleton;
            }
            if (distance == 0) {
                bestFieldDifference = 0;
                break;
            }
            inputSkeleton2 = inputSkeleton3;
            skeletonFieldWidth2 = skeletonFieldWidth;
        }
        bestSkeleton = bestSkeleton2;
        skeletonFieldWidth = skeletonFieldWidth2;
        if (replaceZWithV && bestFieldDifference != -1) {
            bestFieldDifference = 2;
        }
        return new BestMatchInfo(bestSkeleton2, bestFieldDifference);
    }

    public boolean equals(Object a) {
        if (!(a instanceof DateIntervalInfo)) {
            return false;
        }
        return this.fIntervalPatterns.equals(((DateIntervalInfo) a).fIntervalPatterns);
    }

    public int hashCode() {
        return this.fIntervalPatterns.hashCode();
    }

    @Deprecated
    public Map<String, Set<String>> getPatterns() {
        LinkedHashMap<String, Set<String>> result = new LinkedHashMap();
        for (Entry<String, Map<String, PatternInfo>> entry : this.fIntervalPatterns.entrySet()) {
            result.put((String) entry.getKey(), new LinkedHashSet(((Map) entry.getValue()).keySet()));
        }
        return result;
    }

    @Deprecated
    public Map<String, Map<String, PatternInfo>> getRawPatterns() {
        LinkedHashMap<String, Map<String, PatternInfo>> result = new LinkedHashMap();
        for (Entry<String, Map<String, PatternInfo>> entry : this.fIntervalPatterns.entrySet()) {
            result.put((String) entry.getKey(), new LinkedHashMap((Map) entry.getValue()));
        }
        return result;
    }
}
