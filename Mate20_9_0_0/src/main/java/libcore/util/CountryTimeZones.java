package libcore.util;

import android.icu.impl.PatternTokenizer;
import android.icu.util.TimeZone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CountryTimeZones {
    private final String countryIso;
    private final String defaultTimeZoneId;
    private final boolean everUsesUtc;
    private TimeZone icuDefaultTimeZone;
    private List<TimeZone> icuTimeZones;
    private final List<TimeZoneMapping> timeZoneMappings;

    public static final class OffsetResult {
        public final boolean mOneMatch;
        public final TimeZone mTimeZone;

        public OffsetResult(TimeZone timeZone, boolean oneMatch) {
            this.mTimeZone = (TimeZone) Objects.requireNonNull(timeZone);
            this.mOneMatch = oneMatch;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Result{mTimeZone='");
            stringBuilder.append(this.mTimeZone);
            stringBuilder.append(PatternTokenizer.SINGLE_QUOTE);
            stringBuilder.append(", mOneMatch=");
            stringBuilder.append(this.mOneMatch);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    public static final class TimeZoneMapping {
        public final Long notUsedAfter;
        public final boolean showInPicker;
        public final String timeZoneId;

        TimeZoneMapping(String timeZoneId, boolean showInPicker, Long notUsedAfter) {
            this.timeZoneId = timeZoneId;
            this.showInPicker = showInPicker;
            this.notUsedAfter = notUsedAfter;
        }

        public static TimeZoneMapping createForTests(String timeZoneId, boolean showInPicker, Long notUsedAfter) {
            return new TimeZoneMapping(timeZoneId, showInPicker, notUsedAfter);
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TimeZoneMapping that = (TimeZoneMapping) o;
            if (!(this.showInPicker == that.showInPicker && Objects.equals(this.timeZoneId, that.timeZoneId) && Objects.equals(this.notUsedAfter, that.notUsedAfter))) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.timeZoneId, Boolean.valueOf(this.showInPicker), this.notUsedAfter});
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TimeZoneMapping{timeZoneId='");
            stringBuilder.append(this.timeZoneId);
            stringBuilder.append(PatternTokenizer.SINGLE_QUOTE);
            stringBuilder.append(", showInPicker=");
            stringBuilder.append(this.showInPicker);
            stringBuilder.append(", notUsedAfter=");
            stringBuilder.append(this.notUsedAfter);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }

        public static boolean containsTimeZoneId(List<TimeZoneMapping> timeZoneMappings, String timeZoneId) {
            for (TimeZoneMapping timeZoneMapping : timeZoneMappings) {
                if (timeZoneMapping.timeZoneId.equals(timeZoneId)) {
                    return true;
                }
            }
            return false;
        }
    }

    private CountryTimeZones(String countryIso, String defaultTimeZoneId, boolean everUsesUtc, List<TimeZoneMapping> timeZoneMappings) {
        this.countryIso = (String) Objects.requireNonNull(countryIso);
        this.defaultTimeZoneId = defaultTimeZoneId;
        this.everUsesUtc = everUsesUtc;
        this.timeZoneMappings = Collections.unmodifiableList(new ArrayList(timeZoneMappings));
    }

    public static CountryTimeZones createValidated(String countryIso, String defaultTimeZoneId, boolean everUsesUtc, List<TimeZoneMapping> timeZoneMappings, String debugInfo) {
        HashSet<String> validTimeZoneIdsSet = new HashSet(Arrays.asList(ZoneInfoDB.getInstance().getAvailableIDs()));
        List<TimeZoneMapping> validCountryTimeZoneMappings = new ArrayList();
        for (TimeZoneMapping timeZoneMapping : timeZoneMappings) {
            String timeZoneId = timeZoneMapping.timeZoneId;
            if (validTimeZoneIdsSet.contains(timeZoneId)) {
                validCountryTimeZoneMappings.add(timeZoneMapping);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping invalid zone: ");
                stringBuilder.append(timeZoneId);
                stringBuilder.append(" at ");
                stringBuilder.append(debugInfo);
                System.logW(stringBuilder.toString());
            }
        }
        if (!validTimeZoneIdsSet.contains(defaultTimeZoneId)) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid default time zone ID: ");
            stringBuilder2.append(defaultTimeZoneId);
            stringBuilder2.append(" at ");
            stringBuilder2.append(debugInfo);
            System.logW(stringBuilder2.toString());
            defaultTimeZoneId = null;
        }
        return new CountryTimeZones(normalizeCountryIso(countryIso), defaultTimeZoneId, everUsesUtc, validCountryTimeZoneMappings);
    }

    public String getCountryIso() {
        return this.countryIso;
    }

    public boolean isForCountryCode(String countryIso) {
        return this.countryIso.equals(normalizeCountryIso(countryIso));
    }

    public synchronized TimeZone getDefaultTimeZone() {
        if (this.icuDefaultTimeZone == null) {
            TimeZone defaultTimeZone;
            if (this.defaultTimeZoneId == null) {
                defaultTimeZone = null;
            } else {
                defaultTimeZone = getValidFrozenTimeZoneOrNull(this.defaultTimeZoneId);
            }
            this.icuDefaultTimeZone = defaultTimeZone;
        }
        return this.icuDefaultTimeZone;
    }

    public String getDefaultTimeZoneId() {
        return this.defaultTimeZoneId;
    }

    public List<TimeZoneMapping> getTimeZoneMappings() {
        return this.timeZoneMappings;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CountryTimeZones that = (CountryTimeZones) o;
        if (this.everUsesUtc != that.everUsesUtc || !this.countryIso.equals(that.countryIso)) {
            return false;
        }
        if (this.defaultTimeZoneId == null ? that.defaultTimeZoneId == null : this.defaultTimeZoneId.equals(that.defaultTimeZoneId)) {
            return this.timeZoneMappings.equals(that.timeZoneMappings);
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * this.countryIso.hashCode()) + (this.defaultTimeZoneId != null ? this.defaultTimeZoneId.hashCode() : 0))) + this.timeZoneMappings.hashCode())) + this.everUsesUtc;
    }

    public synchronized List<TimeZone> getIcuTimeZones() {
        if (this.icuTimeZones == null) {
            ArrayList<TimeZone> mutableList = new ArrayList(this.timeZoneMappings.size());
            for (TimeZoneMapping timeZoneMapping : this.timeZoneMappings) {
                TimeZone timeZone;
                String timeZoneId = timeZoneMapping.timeZoneId;
                if (timeZoneId.equals(this.defaultTimeZoneId)) {
                    timeZone = getDefaultTimeZone();
                } else {
                    timeZone = getValidFrozenTimeZoneOrNull(timeZoneId);
                }
                if (timeZone == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Skipping invalid zone: ");
                    stringBuilder.append(timeZoneId);
                    System.logW(stringBuilder.toString());
                } else {
                    mutableList.add(timeZone);
                }
            }
            this.icuTimeZones = Collections.unmodifiableList(mutableList);
        }
        return this.icuTimeZones;
    }

    public boolean hasUtcZone(long whenMillis) {
        if (!this.everUsesUtc) {
            return false;
        }
        for (TimeZone zone : getIcuTimeZones()) {
            if (zone.getOffset(whenMillis) == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isDefaultOkForCountryTimeZoneDetection(long whenMillis) {
        if (this.timeZoneMappings.isEmpty()) {
            return false;
        }
        if (this.timeZoneMappings.size() == 1) {
            return true;
        }
        TimeZone countryDefault = getDefaultTimeZone();
        if (countryDefault == null) {
            return false;
        }
        int countryDefaultOffset = countryDefault.getOffset(whenMillis);
        for (TimeZone candidate : getIcuTimeZones()) {
            if (candidate != countryDefault) {
                if (countryDefaultOffset != candidate.getOffset(whenMillis)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Deprecated
    public OffsetResult lookupByOffsetWithBias(int offsetMillis, boolean isDst, long whenMillis, TimeZone bias) {
        if (this.timeZoneMappings == null || this.timeZoneMappings.isEmpty()) {
            return null;
        }
        TimeZone firstMatch = null;
        boolean biasMatched = false;
        boolean oneMatch = true;
        for (TimeZone match : getIcuTimeZones()) {
            if (offsetMatchesAtTime(match, offsetMillis, isDst, whenMillis)) {
                if (firstMatch == null) {
                    firstMatch = match;
                } else {
                    oneMatch = false;
                }
                if (bias != null && match.getID().equals(bias.getID())) {
                    biasMatched = true;
                }
                if (firstMatch != null && !oneMatch && (bias == null || biasMatched)) {
                    break;
                }
            }
        }
        if (firstMatch == null) {
            return null;
        }
        return new OffsetResult(biasMatched ? bias : firstMatch, oneMatch);
    }

    private static boolean offsetMatchesAtTime(TimeZone timeZone, int offsetMillis, boolean isDst, long whenMillis) {
        int[] offsets = new int[2];
        boolean z = false;
        timeZone.getOffset(whenMillis, false, offsets);
        if (isDst != (offsets[1] != 0)) {
            return false;
        }
        if (offsetMillis == offsets[0] + offsets[1]) {
            z = true;
        }
        return z;
    }

    public OffsetResult lookupByOffsetWithBias(int offsetMillis, Integer dstOffsetMillis, long whenMillis, TimeZone bias) {
        if (this.timeZoneMappings == null || this.timeZoneMappings.isEmpty()) {
            return null;
        }
        TimeZone firstMatch = null;
        boolean biasMatched = false;
        boolean oneMatch = true;
        for (TimeZone match : getIcuTimeZones()) {
            if (offsetMatchesAtTime(match, offsetMillis, dstOffsetMillis, whenMillis)) {
                if (firstMatch == null) {
                    firstMatch = match;
                } else {
                    oneMatch = false;
                }
                if (bias != null && match.getID().equals(bias.getID())) {
                    biasMatched = true;
                }
                if (firstMatch != null && !oneMatch && (bias == null || biasMatched)) {
                    break;
                }
            }
        }
        if (firstMatch == null) {
            return null;
        }
        return new OffsetResult(biasMatched ? bias : firstMatch, oneMatch);
    }

    private static boolean offsetMatchesAtTime(TimeZone timeZone, int offsetMillis, Integer dstOffsetMillis, long whenMillis) {
        int[] offsets = new int[2];
        boolean z = false;
        timeZone.getOffset(whenMillis, false, offsets);
        if (dstOffsetMillis != null && dstOffsetMillis.intValue() != offsets[1]) {
            return false;
        }
        if (offsetMillis == offsets[0] + offsets[1]) {
            z = true;
        }
        return z;
    }

    private static TimeZone getValidFrozenTimeZoneOrNull(String timeZoneId) {
        TimeZone timeZone = TimeZone.getFrozenTimeZone(timeZoneId);
        if (timeZone.getID().equals(TimeZone.UNKNOWN_ZONE_ID)) {
            return null;
        }
        return timeZone;
    }

    private static String normalizeCountryIso(String countryIso) {
        return countryIso.toLowerCase(Locale.US);
    }
}
