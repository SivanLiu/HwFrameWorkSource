package com.android.internal.telephony;

import android.telephony.Rlog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import java.util.Calendar;
import java.util.TimeZone;

@VisibleForTesting(visibility = Visibility.PACKAGE)
public final class NitzData {
    private static final String LOG_TAG = "NitzData";
    private static final int MAX_NITZ_YEAR = 2037;
    private static final int MS_PER_QUARTER_HOUR = 900000;
    private final long mCurrentTimeMillis;
    private final Integer mDstOffset;
    private final TimeZone mEmulatorHostTimeZone;
    private final String mOriginalString;
    private final int mZoneOffset;

    private NitzData(String originalString, int zoneOffsetMillis, Integer dstOffsetMillis, long utcTimeMillis, TimeZone emulatorHostTimeZone) {
        if (originalString != null) {
            this.mOriginalString = originalString;
            this.mZoneOffset = zoneOffsetMillis;
            this.mDstOffset = dstOffsetMillis;
            this.mCurrentTimeMillis = utcTimeMillis;
            this.mEmulatorHostTimeZone = emulatorHostTimeZone;
            return;
        }
        throw new NullPointerException("originalString==null");
    }

    public static NitzData parse(String nitz) {
        String str = nitz;
        String str2;
        StringBuilder stringBuilder;
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.clear();
            boolean z = false;
            c.set(16, 0);
            String[] nitzSubs = str.split("[/:,+-]");
            int year = 2000 + Integer.parseInt(nitzSubs[0]);
            if (year > 2037) {
                str2 = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NITZ year: ");
                stringBuilder.append(year);
                stringBuilder.append(" exceeds limit, skip NITZ time update");
                Rlog.e(str2, stringBuilder.toString());
                return null;
            }
            int i = 1;
            c.set(1, year);
            c.set(2, Integer.parseInt(nitzSubs[1]) - 1);
            c.set(5, Integer.parseInt(nitzSubs[2]));
            c.set(10, Integer.parseInt(nitzSubs[3]));
            c.set(12, Integer.parseInt(nitzSubs[4]));
            int second = Integer.parseInt(nitzSubs[5]);
            c.set(13, second);
            if (str.indexOf(45) == -1) {
                z = true;
            }
            boolean sign = z;
            int totalUtcOffsetQuarterHours = Integer.parseInt(nitzSubs[6]);
            if (!sign) {
                i = -1;
            }
            int totalUtcOffsetMillis = (i * totalUtcOffsetQuarterHours) * MS_PER_QUARTER_HOUR;
            Integer dstAdjustmentQuarterHours = nitzSubs.length >= 8 ? Integer.valueOf(Integer.parseInt(nitzSubs[7])) : null;
            Integer dstAdjustmentMillis = null;
            if (dstAdjustmentQuarterHours != null) {
                dstAdjustmentMillis = Integer.valueOf(dstAdjustmentQuarterHours.intValue() * MS_PER_QUARTER_HOUR);
            }
            Integer dstAdjustmentMillis2 = dstAdjustmentMillis;
            TimeZone zone = null;
            if (nitzSubs.length >= 9) {
                zone = TimeZone.getTimeZone(nitzSubs[8].replace('!', '/'));
            }
            return new NitzData(str, totalUtcOffsetMillis, dstAdjustmentMillis2, c.getTimeInMillis(), zone);
        } catch (RuntimeException ex) {
            str2 = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NITZ: Parsing NITZ time ");
            stringBuilder.append(str);
            stringBuilder.append(" ex=");
            stringBuilder.append(ex);
            Rlog.e(str2, stringBuilder.toString());
            return null;
        }
    }

    public static NitzData createForTests(int zoneOffsetMillis, Integer dstOffsetMillis, long utcTimeMillis, TimeZone emulatorHostTimeZone) {
        return new NitzData("Test data", zoneOffsetMillis, dstOffsetMillis, utcTimeMillis, emulatorHostTimeZone);
    }

    public long getCurrentTimeInMillis() {
        return this.mCurrentTimeMillis;
    }

    public int getLocalOffsetMillis() {
        return this.mZoneOffset;
    }

    public Integer getDstAdjustmentMillis() {
        return this.mDstOffset;
    }

    public boolean isDst() {
        return (this.mDstOffset == null || this.mDstOffset.intValue() == 0) ? false : true;
    }

    public TimeZone getEmulatorHostTimeZone() {
        return this.mEmulatorHostTimeZone;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NitzData nitzData = (NitzData) o;
        if (this.mZoneOffset != nitzData.mZoneOffset || this.mCurrentTimeMillis != nitzData.mCurrentTimeMillis || !this.mOriginalString.equals(nitzData.mOriginalString)) {
            return false;
        }
        if (!this.mDstOffset == null ? this.mDstOffset.equals(nitzData.mDstOffset) : nitzData.mDstOffset == null) {
            return false;
        }
        if (this.mEmulatorHostTimeZone != null) {
            z = this.mEmulatorHostTimeZone.equals(nitzData.mEmulatorHostTimeZone);
        } else if (nitzData.mEmulatorHostTimeZone != null) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * ((31 * ((31 * ((31 * this.mOriginalString.hashCode()) + this.mZoneOffset)) + (this.mDstOffset != null ? this.mDstOffset.hashCode() : 0))) + ((int) (this.mCurrentTimeMillis ^ (this.mCurrentTimeMillis >>> 32))));
        if (this.mEmulatorHostTimeZone != null) {
            i = this.mEmulatorHostTimeZone.hashCode();
        }
        return result + i;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NitzData{mOriginalString=");
        stringBuilder.append(this.mOriginalString);
        stringBuilder.append(", mZoneOffset=");
        stringBuilder.append(this.mZoneOffset);
        stringBuilder.append(", mDstOffset=");
        stringBuilder.append(this.mDstOffset);
        stringBuilder.append(", mCurrentTimeMillis=");
        stringBuilder.append(this.mCurrentTimeMillis);
        stringBuilder.append(", mEmulatorHostTimeZone=");
        stringBuilder.append(this.mEmulatorHostTimeZone);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
