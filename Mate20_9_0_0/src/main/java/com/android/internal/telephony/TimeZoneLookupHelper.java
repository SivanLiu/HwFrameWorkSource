package com.android.internal.telephony;

import android.icu.util.TimeZone;
import android.text.TextUtils;
import java.util.Date;
import libcore.util.CountryTimeZones;
import libcore.util.TimeZoneFinder;

public class TimeZoneLookupHelper {
    private static final int MS_PER_HOUR = 3600000;
    private CountryTimeZones mLastCountryTimeZones;

    public static final class CountryResult {
        public final boolean allZonesHaveSameOffset;
        public final long whenMillis;
        public final String zoneId;

        public CountryResult(String zoneId, boolean allZonesHaveSameOffset, long whenMillis) {
            this.zoneId = zoneId;
            this.allZonesHaveSameOffset = allZonesHaveSameOffset;
            this.whenMillis = whenMillis;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CountryResult that = (CountryResult) o;
            if (this.allZonesHaveSameOffset == that.allZonesHaveSameOffset && this.whenMillis == that.whenMillis) {
                return this.zoneId.equals(that.zoneId);
            }
            return false;
        }

        public int hashCode() {
            return (31 * ((31 * this.zoneId.hashCode()) + this.allZonesHaveSameOffset)) + ((int) (this.whenMillis ^ (this.whenMillis >>> 32)));
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CountryResult{zoneId='");
            stringBuilder.append(this.zoneId);
            stringBuilder.append('\'');
            stringBuilder.append(", allZonesHaveSameOffset=");
            stringBuilder.append(this.allZonesHaveSameOffset);
            stringBuilder.append(", whenMillis=");
            stringBuilder.append(this.whenMillis);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    public static final class OffsetResult {
        public final boolean isOnlyMatch;
        public final String zoneId;

        public OffsetResult(String zoneId, boolean isOnlyMatch) {
            this.zoneId = zoneId;
            this.isOnlyMatch = isOnlyMatch;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OffsetResult result = (OffsetResult) o;
            if (this.isOnlyMatch != result.isOnlyMatch) {
                return false;
            }
            return this.zoneId.equals(result.zoneId);
        }

        public int hashCode() {
            return (31 * this.zoneId.hashCode()) + this.isOnlyMatch;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Result{zoneId='");
            stringBuilder.append(this.zoneId);
            stringBuilder.append('\'');
            stringBuilder.append(", isOnlyMatch=");
            stringBuilder.append(this.isOnlyMatch);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    public OffsetResult lookupByNitzCountry(NitzData nitzData, String isoCountryCode) {
        CountryTimeZones countryTimeZones = getCountryTimeZones(isoCountryCode);
        if (countryTimeZones == null) {
            return null;
        }
        libcore.util.CountryTimeZones.OffsetResult offsetResult = countryTimeZones;
        offsetResult = offsetResult.lookupByOffsetWithBias(nitzData.getLocalOffsetMillis(), nitzData.isDst(), nitzData.getCurrentTimeInMillis(), TimeZone.getDefault());
        if (offsetResult == null) {
            return null;
        }
        return new OffsetResult(offsetResult.mTimeZone.getID(), offsetResult.mOneMatch);
    }

    public OffsetResult lookupByNitz(NitzData nitzData) {
        return lookupByNitzStatic(nitzData);
    }

    public CountryResult lookupByCountry(String isoCountryCode, long whenMillis) {
        CountryTimeZones countryTimeZones = getCountryTimeZones(isoCountryCode);
        if (countryTimeZones == null || countryTimeZones.getDefaultTimeZoneId() == null) {
            return null;
        }
        return new CountryResult(countryTimeZones.getDefaultTimeZoneId(), countryTimeZones.isDefaultOkForCountryTimeZoneDetection(whenMillis), whenMillis);
    }

    static java.util.TimeZone guessZoneByNitzStatic(NitzData nitzData) {
        OffsetResult result = lookupByNitzStatic(nitzData);
        return result != null ? java.util.TimeZone.getTimeZone(result.zoneId) : null;
    }

    private static OffsetResult lookupByNitzStatic(NitzData nitzData) {
        int utcOffsetMillis = nitzData.getLocalOffsetMillis();
        boolean isDst = nitzData.isDst();
        long timeMillis = nitzData.getCurrentTimeInMillis();
        OffsetResult match = lookupByInstantOffsetDst(timeMillis, utcOffsetMillis, isDst);
        if (match == null) {
            return lookupByInstantOffsetDst(timeMillis, utcOffsetMillis, isDst ^ 1);
        }
        return match;
    }

    private static OffsetResult lookupByInstantOffsetDst(long timeMillis, int utcOffsetMillis, boolean isDst) {
        int rawOffset = utcOffsetMillis;
        if (isDst) {
            rawOffset -= MS_PER_HOUR;
        }
        String[] zones = java.util.TimeZone.getAvailableIDs(rawOffset);
        java.util.TimeZone match = null;
        Date d = new Date(timeMillis);
        boolean isOnlyMatch = true;
        for (String zone : zones) {
            java.util.TimeZone tz = java.util.TimeZone.getTimeZone(zone);
            if (tz.getOffset(timeMillis) == utcOffsetMillis && tz.inDaylightTime(d) == isDst) {
                if (match != null) {
                    isOnlyMatch = false;
                    break;
                }
                match = tz;
            }
        }
        if (match == null) {
            return null;
        }
        return new OffsetResult(match.getID(), isOnlyMatch);
    }

    public boolean countryUsesUtc(String isoCountryCode, long whenMillis) {
        boolean z = false;
        if (TextUtils.isEmpty(isoCountryCode)) {
            return false;
        }
        CountryTimeZones countryTimeZones = getCountryTimeZones(isoCountryCode);
        if (countryTimeZones != null && countryTimeZones.hasUtcZone(whenMillis)) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private CountryTimeZones getCountryTimeZones(String isoCountryCode) {
        synchronized (this) {
            CountryTimeZones countryTimeZones;
            if (this.mLastCountryTimeZones == null || !this.mLastCountryTimeZones.isForCountryCode(isoCountryCode)) {
                countryTimeZones = TimeZoneFinder.getInstance().lookupCountryTimeZones(isoCountryCode);
                if (countryTimeZones != null) {
                    this.mLastCountryTimeZones = countryTimeZones;
                }
            } else {
                countryTimeZones = this.mLastCountryTimeZones;
                return countryTimeZones;
            }
        }
    }
}
