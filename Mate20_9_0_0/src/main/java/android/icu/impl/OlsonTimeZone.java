package android.icu.impl;

import android.icu.util.AnnualTimeZoneRule;
import android.icu.util.BasicTimeZone;
import android.icu.util.DateTimeRule;
import android.icu.util.InitialTimeZoneRule;
import android.icu.util.SimpleTimeZone;
import android.icu.util.TimeArrayTimeZoneRule;
import android.icu.util.TimeZone;
import android.icu.util.TimeZoneRule;
import android.icu.util.TimeZoneTransition;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.MissingResourceException;

public class OlsonTimeZone extends BasicTimeZone {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final boolean DEBUG = ICUDebug.enabled("olson");
    private static final int MAX_OFFSET_SECONDS = 86400;
    private static final int SECONDS_PER_DAY = 86400;
    private static final String ZONEINFORES = "zoneinfo64";
    private static final int currentSerialVersion = 1;
    static final long serialVersionUID = -6281977362477515376L;
    private volatile String canonicalID = null;
    private double finalStartMillis = Double.MAX_VALUE;
    private int finalStartYear = Integer.MAX_VALUE;
    private SimpleTimeZone finalZone = null;
    private transient SimpleTimeZone finalZoneWithStartYear;
    private transient TimeZoneTransition firstFinalTZTransition;
    private transient TimeZoneTransition firstTZTransition;
    private transient int firstTZTransitionIdx;
    private transient TimeArrayTimeZoneRule[] historicRules;
    private transient InitialTimeZoneRule initialRule;
    private volatile transient boolean isFrozen = false;
    private int serialVersionOnStream = 1;
    private int transitionCount;
    private transient boolean transitionRulesInitialized;
    private long[] transitionTimes64;
    private int typeCount;
    private byte[] typeMapData;
    private int[] typeOffsets;

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        if (month < 0 || month > 11) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Month is not in the legal range: ");
            stringBuilder.append(month);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        return getOffset(era, year, month, day, dayOfWeek, milliseconds, Grego.monthLength(year, month));
    }

    public int getOffset(int era, int year, int month, int dom, int dow, int millis, int monthLength) {
        int i = era;
        int i2 = month;
        int i3 = dom;
        int i4 = dow;
        int i5 = millis;
        int i6 = monthLength;
        int i7;
        if ((i == 1 || i == 0) && i2 >= 0 && i2 <= 11 && i3 >= 1 && i3 <= i6 && i4 >= 1 && i4 <= 7 && i5 >= 0 && i5 < Grego.MILLIS_PER_DAY && i6 >= 28 && i6 <= 31) {
            if (i == 0) {
                i7 = -year;
            } else {
                i7 = year;
            }
            int year2 = i7;
            if (this.finalZone != null && year2 >= this.finalStartYear) {
                return this.finalZone.getOffset(i, year2, i2, i3, i4, i5);
            }
            int[] offsets = new int[2];
            int[] offsets2 = offsets;
            getHistoricalOffset((Grego.fieldsToDay(year2, i2, i3) * 86400000) + ((long) i5), true, 3, 1, offsets);
            return offsets2[0] + offsets2[1];
        }
        i7 = year;
        throw new IllegalArgumentException();
    }

    public void setRawOffset(int offsetMillis) {
        int i = offsetMillis;
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen OlsonTimeZone instance.");
        } else if (getRawOffset() != i) {
            long current = System.currentTimeMillis();
            if (((double) current) < this.finalStartMillis) {
                SimpleTimeZone stz = new SimpleTimeZone(i, getID());
                boolean bDst = useDaylightTime();
                if (bDst) {
                    TimeZoneRule[] currentRules = getSimpleTimeZoneRulesNear(current);
                    if (currentRules.length != 3) {
                        TimeZoneTransition tzt = getPreviousTransition(current, false);
                        if (tzt != null) {
                            currentRules = getSimpleTimeZoneRulesNear(tzt.getTime() - 1);
                        }
                    }
                    if (currentRules.length == 3 && (currentRules[1] instanceof AnnualTimeZoneRule) && (currentRules[2] instanceof AnnualTimeZoneRule)) {
                        DateTimeRule start;
                        DateTimeRule end;
                        int sav;
                        AnnualTimeZoneRule r1 = currentRules[1];
                        AnnualTimeZoneRule r2 = currentRules[2];
                        int offset1 = r1.getRawOffset() + r1.getDSTSavings();
                        int offset2 = r2.getRawOffset() + r2.getDSTSavings();
                        if (offset1 > offset2) {
                            start = r1.getRule();
                            end = r2.getRule();
                            sav = offset1 - offset2;
                        } else {
                            start = r2.getRule();
                            end = r1.getRule();
                            sav = offset2 - offset1;
                        }
                        stz.setStartRule(start.getRuleMonth(), start.getRuleWeekInMonth(), start.getRuleDayOfWeek(), start.getRuleMillisInDay());
                        stz.setEndRule(end.getRuleMonth(), end.getRuleWeekInMonth(), end.getRuleDayOfWeek(), end.getRuleMillisInDay());
                        stz.setDSTSavings(sav);
                    } else {
                        stz.setStartRule(0, 1, 0);
                        stz.setEndRule(11, 31, 86399999);
                    }
                }
                int[] fields = Grego.timeToFields(current, null);
                this.finalStartYear = fields[0];
                this.finalStartMillis = (double) Grego.fieldsToDay(fields[0], 0, 1);
                if (bDst) {
                    stz.setStartYear(this.finalStartYear);
                }
                this.finalZone = stz;
            } else {
                this.finalZone.setRawOffset(i);
            }
            this.transitionRulesInitialized = false;
        }
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    public void getOffset(long date, boolean local, int[] offsets) {
        if (this.finalZone == null || ((double) date) < this.finalStartMillis) {
            getHistoricalOffset(date, local, 4, 12, offsets);
        } else {
            this.finalZone.getOffset(date, local, offsets);
        }
    }

    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        if (this.finalZone == null || ((double) date) < this.finalStartMillis) {
            getHistoricalOffset(date, true, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
        } else {
            this.finalZone.getOffsetFromLocal(date, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
        }
    }

    public int getRawOffset() {
        int[] ret = new int[2];
        getOffset(System.currentTimeMillis(), false, ret);
        return ret[0];
    }

    public boolean useDaylightTime() {
        long current = System.currentTimeMillis();
        boolean z = false;
        if (this.finalZone == null || ((double) current) < this.finalStartMillis) {
            int[] fields = Grego.timeToFields(current, null);
            long start = Grego.fieldsToDay(fields[0], 0, 1) * 86400;
            long limit = Grego.fieldsToDay(fields[0] + 1, 0, 1) * 86400;
            int i = 0;
            while (i < this.transitionCount && this.transitionTimes64[i] < limit) {
                if ((this.transitionTimes64[i] >= start && dstOffsetAt(i) != 0) || (this.transitionTimes64[i] > start && i > 0 && dstOffsetAt(i - 1) != 0)) {
                    return true;
                }
                i++;
            }
            return false;
        }
        if (this.finalZone != null && this.finalZone.useDaylightTime()) {
            z = true;
        }
        return z;
    }

    public boolean observesDaylightTime() {
        long current = System.currentTimeMillis();
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                return true;
            }
            if (((double) current) >= this.finalStartMillis) {
                return false;
            }
        }
        long currentSec = Grego.floorDivide(current, 1000);
        int trsIdx = this.transitionCount - 1;
        if (dstOffsetAt(trsIdx) != 0) {
            return true;
        }
        while (trsIdx >= 0 && this.transitionTimes64[trsIdx] > currentSec) {
            if (dstOffsetAt(trsIdx - 1) != 0) {
                return true;
            }
            trsIdx--;
        }
        return false;
    }

    public int getDSTSavings() {
        if (this.finalZone != null) {
            return this.finalZone.getDSTSavings();
        }
        return super.getDSTSavings();
    }

    public boolean inDaylightTime(Date date) {
        int[] temp = new int[2];
        getOffset(date.getTime(), false, temp);
        return temp[1] != 0;
    }

    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (!super.hasSameRules(other) || !(other instanceof OlsonTimeZone)) {
            return false;
        }
        OlsonTimeZone o = (OlsonTimeZone) other;
        if (this.finalZone == null) {
            if (o.finalZone != null) {
                return false;
            }
        } else if (!(o.finalZone != null && this.finalStartYear == o.finalStartYear && this.finalZone.hasSameRules(o.finalZone))) {
            return false;
        }
        if (this.transitionCount == o.transitionCount && Arrays.equals(this.transitionTimes64, o.transitionTimes64) && this.typeCount == o.typeCount && Arrays.equals(this.typeMapData, o.typeMapData) && Arrays.equals(this.typeOffsets, o.typeOffsets)) {
            return true;
        }
        return false;
    }

    public String getCanonicalID() {
        if (this.canonicalID == null) {
            synchronized (this) {
                if (this.canonicalID == null) {
                    this.canonicalID = TimeZone.getCanonicalID(getID());
                    if (this.canonicalID == null) {
                        this.canonicalID = getID();
                    }
                }
            }
        }
        return this.canonicalID;
    }

    private void constructEmpty() {
        this.transitionCount = 0;
        this.transitionTimes64 = null;
        this.typeMapData = null;
        this.typeCount = 1;
        this.typeOffsets = new int[]{0, 0};
        this.finalZone = null;
        this.finalStartYear = Integer.MAX_VALUE;
        this.finalStartMillis = Double.MAX_VALUE;
        this.transitionRulesInitialized = false;
    }

    public OlsonTimeZone(UResourceBundle top, UResourceBundle res, String id) {
        super(id);
        construct(top, res);
    }

    private void construct(UResourceBundle top, UResourceBundle res) {
        UResourceBundle uResourceBundle = top;
        UResourceBundle uResourceBundle2 = res;
        if (uResourceBundle == null || uResourceBundle2 == null) {
            throw new IllegalArgumentException();
        }
        if (DEBUG) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("OlsonTimeZone(");
            stringBuilder.append(res.getKey());
            stringBuilder.append(")");
            printStream.println(stringBuilder.toString());
        }
        int[] transPost32 = null;
        int[] trans32 = null;
        int[] transPre32 = null;
        this.transitionCount = 0;
        int i = 2;
        try {
            transPre32 = uResourceBundle2.get("transPre32").getIntVector();
            if (transPre32.length % 2 == 0) {
                this.transitionCount += transPre32.length / 2;
                try {
                    trans32 = uResourceBundle2.get("trans").getIntVector();
                    this.transitionCount += trans32.length;
                } catch (MissingResourceException e) {
                }
                try {
                    transPost32 = uResourceBundle2.get("transPost32").getIntVector();
                    if (transPost32.length % 2 == 0) {
                        int idx;
                        this.transitionCount += transPost32.length / 2;
                        int i2 = 1;
                        if (this.transitionCount > 0) {
                            int idx2;
                            int[] transPost322;
                            this.transitionTimes64 = new long[this.transitionCount];
                            long j = 32;
                            if (transPre32 != null) {
                                idx2 = 0;
                                idx = 0;
                                while (idx < transPre32.length / 2) {
                                    transPost322 = transPost32;
                                    this.transitionTimes64[idx2] = ((((long) transPre32[idx * 2]) & 4294967295L) << j) | (((long) transPre32[(idx * 2) + i2]) & 4294967295L);
                                    idx++;
                                    idx2++;
                                    transPost32 = transPost322;
                                    i2 = 1;
                                    j = 32;
                                }
                                transPost322 = transPost32;
                            } else {
                                transPost322 = transPost32;
                                idx2 = 0;
                            }
                            if (trans32 != null) {
                                idx = 0;
                                while (idx < trans32.length) {
                                    this.transitionTimes64[idx2] = (long) trans32[idx];
                                    idx++;
                                    idx2++;
                                }
                            }
                            if (transPost322 != null) {
                                idx = 0;
                                while (true) {
                                    transPost32 = transPost322;
                                    if (idx >= transPost32.length / i) {
                                        break;
                                    }
                                    this.transitionTimes64[idx2] = (((long) transPost32[(idx * 2) + 1]) & 4294967295L) | ((((long) transPost32[idx * 2]) & 4294967295L) << 32);
                                    idx++;
                                    idx2++;
                                    transPost322 = transPost32;
                                    i = 2;
                                }
                            }
                        } else {
                            this.transitionTimes64 = null;
                        }
                        UResourceBundle r = uResourceBundle2.get("typeOffsets");
                        this.typeOffsets = r.getIntVector();
                        if (this.typeOffsets.length < 2 || this.typeOffsets.length > Normalizer2Impl.COMP_1_TRAIL_MASK || this.typeOffsets.length % 2 != 0) {
                            throw new IllegalArgumentException("Invalid Format");
                        }
                        String ruleID;
                        this.typeCount = this.typeOffsets.length / 2;
                        if (this.transitionCount > 0) {
                            r = uResourceBundle2.get("typeMap");
                            this.typeMapData = r.getBinary(null);
                            if (this.typeMapData == null || this.typeMapData.length != this.transitionCount) {
                                throw new IllegalArgumentException("Invalid Format");
                            }
                            UResourceBundle uResourceBundle3 = r;
                            ruleID = null;
                        } else {
                            ruleID = null;
                            this.typeMapData = null;
                        }
                        this.finalZone = ruleID;
                        this.finalStartYear = Integer.MAX_VALUE;
                        this.finalStartMillis = Double.MAX_VALUE;
                        try {
                            idx = uResourceBundle2.get("finalRaw").getInt() * 1000;
                            int[] ruleData = loadRule(uResourceBundle, uResourceBundle2.getString("finalRule")).getIntVector();
                            if (ruleData == null || ruleData.length != 11) {
                                throw new IllegalArgumentException("Invalid Format");
                            }
                            int i3 = ruleData[3] * 1000;
                            int i4 = idx;
                            int i5 = i3;
                            this.finalZone = new SimpleTimeZone(i4, "", ruleData[0], ruleData[1], ruleData[2], i5, ruleData[4], ruleData[5], ruleData[6], ruleData[7], ruleData[8] * 1000, ruleData[9], ruleData[10] * 1000);
                            this.finalStartYear = uResourceBundle2.get("finalYear").getInt();
                            this.finalStartMillis = (double) (Grego.fieldsToDay(this.finalStartYear, 0, 1) * 86400000);
                            return;
                        } catch (MissingResourceException e2) {
                            if (ruleID != null) {
                                throw new IllegalArgumentException("Invalid Format");
                            }
                            return;
                        }
                    }
                    throw new IllegalArgumentException("Invalid Format");
                } catch (MissingResourceException e3) {
                }
            } else {
                throw new IllegalArgumentException("Invalid Format");
            }
        } catch (MissingResourceException e4) {
        }
    }

    public OlsonTimeZone(String id) {
        super(id);
        UResourceBundle top = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZONEINFORES, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        construct(top, ZoneMeta.openOlsonResource(top, id));
        if (this.finalZone != null) {
            this.finalZone.setID(id);
        }
    }

    public void setID(String id) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen OlsonTimeZone instance.");
        }
        if (this.canonicalID == null) {
            this.canonicalID = TimeZone.getCanonicalID(getID());
            if (this.canonicalID == null) {
                this.canonicalID = getID();
            }
        }
        if (this.finalZone != null) {
            this.finalZone.setID(id);
        }
        super.setID(id);
        this.transitionRulesInitialized = false;
    }

    private void getHistoricalOffset(long date, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt, int[] offsets) {
        boolean z = false;
        boolean z2 = true;
        if (this.transitionCount != 0) {
            long sec = Grego.floorDivide(date, 1000);
            if (local || sec >= this.transitionTimes64[0]) {
                int transIdx = this.transitionCount - 1;
                while (transIdx >= 0) {
                    long transition = this.transitionTimes64[transIdx];
                    if (local && sec >= transition - 86400) {
                        int offsetBefore = zoneOffsetAt(transIdx - 1);
                        boolean dstBefore = dstOffsetAt(transIdx + -1) != 0 ? z2 : z;
                        int offsetAfter = zoneOffsetAt(transIdx);
                        boolean dstAfter = dstOffsetAt(transIdx) != 0 ? z2 : false;
                        boolean dstToStd = (!dstBefore || dstAfter) ? false : z2;
                        boolean stdToDst = (dstBefore || !dstAfter) ? false : z2;
                        transition = offsetAfter - offsetBefore >= 0 ? (((NonExistingTimeOpt & 3) == 1 && dstToStd) || ((NonExistingTimeOpt & 3) == 3 && stdToDst)) ? transition + ((long) offsetBefore) : (((NonExistingTimeOpt & 3) == 1 && stdToDst) || ((NonExistingTimeOpt & 3) == 3 && dstToStd)) ? transition + ((long) offsetAfter) : (NonExistingTimeOpt & 12) == 12 ? transition + ((long) offsetBefore) : transition + ((long) offsetAfter) : (((DuplicatedTimeOpt & 3) == 1 && dstToStd) || ((DuplicatedTimeOpt & 3) == 3 && stdToDst)) ? transition + ((long) offsetAfter) : (((DuplicatedTimeOpt & 3) == 1 && stdToDst) || ((DuplicatedTimeOpt & 3) == 3 && dstToStd)) ? transition + ((long) offsetBefore) : (DuplicatedTimeOpt & 12) == 4 ? transition + ((long) offsetBefore) : transition + ((long) offsetAfter);
                    }
                    if (sec >= transition) {
                        break;
                    }
                    transIdx--;
                    z = false;
                    z2 = true;
                    long j = date;
                }
                offsets[0] = rawOffsetAt(transIdx) * 1000;
                offsets[1] = dstOffsetAt(transIdx) * 1000;
                return;
            }
            offsets[0] = initialRawOffset() * 1000;
            offsets[1] = initialDstOffset() * 1000;
            return;
        }
        offsets[0] = initialRawOffset() * 1000;
        offsets[1] = initialDstOffset() * 1000;
    }

    private int getInt(byte val) {
        return val & 255;
    }

    private int zoneOffsetAt(int transIdx) {
        int typeIdx = transIdx >= 0 ? getInt(this.typeMapData[transIdx]) * 2 : 0;
        return this.typeOffsets[typeIdx] + this.typeOffsets[typeIdx + 1];
    }

    private int rawOffsetAt(int transIdx) {
        return this.typeOffsets[transIdx >= 0 ? getInt(this.typeMapData[transIdx]) * 2 : 0];
    }

    private int dstOffsetAt(int transIdx) {
        return this.typeOffsets[(transIdx >= 0 ? getInt(this.typeMapData[transIdx]) * 2 : 0) + 1];
    }

    private int initialRawOffset() {
        return this.typeOffsets[0];
    }

    private int initialDstOffset() {
        return this.typeOffsets[1];
    }

    public String toString() {
        int i;
        StringBuilder buf = new StringBuilder();
        buf.append(super.toString());
        buf.append('[');
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("transitionCount=");
        stringBuilder.append(this.transitionCount);
        buf.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(",typeCount=");
        stringBuilder.append(this.typeCount);
        buf.append(stringBuilder.toString());
        buf.append(",transitionTimes=");
        int i2 = 0;
        if (this.transitionTimes64 != null) {
            buf.append('[');
            for (i = 0; i < this.transitionTimes64.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(Long.toString(this.transitionTimes64[i]));
            }
            buf.append(']');
        } else {
            buf.append("null");
        }
        buf.append(",typeOffsets=");
        if (this.typeOffsets != null) {
            buf.append('[');
            for (i = 0; i < this.typeOffsets.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(Integer.toString(this.typeOffsets[i]));
            }
            buf.append(']');
        } else {
            buf.append("null");
        }
        buf.append(",typeMapData=");
        if (this.typeMapData != null) {
            buf.append('[');
            while (true) {
                int i3 = i2;
                if (i3 >= this.typeMapData.length) {
                    break;
                }
                if (i3 > 0) {
                    buf.append(',');
                }
                buf.append(Byte.toString(this.typeMapData[i3]));
                i2 = i3 + 1;
            }
        } else {
            buf.append("null");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(",finalStartYear=");
        stringBuilder2.append(this.finalStartYear);
        buf.append(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(",finalStartMillis=");
        stringBuilder2.append(this.finalStartMillis);
        buf.append(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(",finalZone=");
        stringBuilder2.append(this.finalZone);
        buf.append(stringBuilder2.toString());
        buf.append(']');
        return buf.toString();
    }

    private static UResourceBundle loadRule(UResourceBundle top, String ruleid) {
        return top.get("Rules").get(ruleid);
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!super.equals(obj)) {
            return false;
        }
        OlsonTimeZone z2 = (OlsonTimeZone) obj;
        if (Utility.arrayEquals(this.typeMapData, z2.typeMapData) || (this.finalStartYear == z2.finalStartYear && ((this.finalZone == null && z2.finalZone == null) || (this.finalZone != null && z2.finalZone != null && this.finalZone.equals(z2.finalZone) && this.transitionCount == z2.transitionCount && this.typeCount == z2.typeCount && Utility.arrayEquals(this.transitionTimes64, z2.transitionTimes64) && Utility.arrayEquals(this.typeOffsets, z2.typeOffsets) && Utility.arrayEquals(this.typeMapData, z2.typeMapData))))) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        int ret;
        int i = 0;
        int ret2 = (int) (((long) ((this.finalStartYear ^ ((this.finalStartYear >>> 4) + this.transitionCount)) ^ ((this.transitionCount >>> 6) + this.typeCount))) ^ (((((long) (this.typeCount >>> 8)) + Double.doubleToLongBits(this.finalStartMillis)) + ((long) (this.finalZone == null ? 0 : this.finalZone.hashCode()))) + ((long) super.hashCode())));
        if (this.transitionTimes64 != null) {
            ret = ret2;
            for (ret2 = 0; ret2 < this.transitionTimes64.length; ret2++) {
                ret = (int) (((long) ret) + (this.transitionTimes64[ret2] ^ (this.transitionTimes64[ret2] >>> 8)));
            }
            ret2 = ret;
        }
        ret = ret2;
        for (ret2 = 0; ret2 < this.typeOffsets.length; ret2++) {
            ret += this.typeOffsets[ret2] ^ (this.typeOffsets[ret2] >>> 8);
        }
        if (this.typeMapData != null) {
            while (true) {
                ret2 = i;
                if (ret2 >= this.typeMapData.length) {
                    break;
                }
                ret += this.typeMapData[ret2] & 255;
                i = ret2 + 1;
            }
        }
        return ret;
    }

    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        initTransitionRules();
        if (this.finalZone != null) {
            if (inclusive && base == this.firstFinalTZTransition.getTime()) {
                return this.firstFinalTZTransition;
            }
            if (base >= this.firstFinalTZTransition.getTime()) {
                if (this.finalZone.useDaylightTime()) {
                    return this.finalZoneWithStartYear.getNextTransition(base, inclusive);
                }
                return null;
            }
        }
        if (this.historicRules == null) {
            return null;
        }
        int ttidx = this.transitionCount;
        while (true) {
            ttidx--;
            if (ttidx < this.firstTZTransitionIdx) {
                break;
            }
            long t = this.transitionTimes64[ttidx] * 1000;
            if (base > t || (!inclusive && base == t)) {
                break;
            }
        }
        if (ttidx == this.transitionCount - 1) {
            return this.firstFinalTZTransition;
        }
        if (ttidx < this.firstTZTransitionIdx) {
            return this.firstTZTransition;
        }
        TimeZoneRule to = this.historicRules[getInt(this.typeMapData[ttidx + 1])];
        TimeZoneRule from = this.historicRules[getInt(this.typeMapData[ttidx])];
        long startTime = this.transitionTimes64[ttidx + 1] * 1000;
        if (from.getName().equals(to.getName()) && from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
            return getNextTransition(startTime, false);
        }
        return new TimeZoneTransition(startTime, from, to);
    }

    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        initTransitionRules();
        if (this.finalZone != null) {
            if (inclusive && base == this.firstFinalTZTransition.getTime()) {
                return this.firstFinalTZTransition;
            }
            if (base > this.firstFinalTZTransition.getTime()) {
                if (this.finalZone.useDaylightTime()) {
                    return this.finalZoneWithStartYear.getPreviousTransition(base, inclusive);
                }
                return this.firstFinalTZTransition;
            }
        }
        if (this.historicRules == null) {
            return null;
        }
        long t;
        int ttidx = this.transitionCount;
        while (true) {
            ttidx--;
            if (ttidx < this.firstTZTransitionIdx) {
                break;
            }
            t = this.transitionTimes64[ttidx] * 1000;
            if (base > t || (inclusive && base == t)) {
                break;
            }
        }
        if (ttidx < this.firstTZTransitionIdx) {
            return null;
        }
        if (ttidx == this.firstTZTransitionIdx) {
            return this.firstTZTransition;
        }
        TimeZoneRule to = this.historicRules[getInt(this.typeMapData[ttidx])];
        TimeZoneRule from = this.historicRules[getInt(this.typeMapData[ttidx - 1])];
        t = this.transitionTimes64[ttidx] * 1000;
        if (from.getName().equals(to.getName()) && from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
            return getPreviousTransition(t, false);
        }
        return new TimeZoneTransition(t, from, to);
    }

    public TimeZoneRule[] getTimeZoneRules() {
        int i;
        initTransitionRules();
        int size = 1;
        if (this.historicRules != null) {
            int size2 = 1;
            for (TimeArrayTimeZoneRule timeArrayTimeZoneRule : this.historicRules) {
                if (timeArrayTimeZoneRule != null) {
                    size2++;
                }
            }
            size = size2;
        }
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                size += 2;
            } else {
                size++;
            }
        }
        TimeZoneRule[] rules = new TimeZoneRule[size];
        int idx = 0 + 1;
        rules[0] = this.initialRule;
        if (this.historicRules != null) {
            for (i = 0; i < this.historicRules.length; i++) {
                if (this.historicRules[i] != null) {
                    int idx2 = idx + 1;
                    rules[idx] = this.historicRules[i];
                    idx = idx2;
                }
            }
        }
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                TimeZoneRule[] stzr = this.finalZoneWithStartYear.getTimeZoneRules();
                i = idx + 1;
                rules[idx] = stzr[1];
                idx = i + 1;
                rules[i] = stzr[2];
            } else {
                i = idx + 1;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getID());
                stringBuilder.append("(STD)");
                rules[idx] = new TimeArrayTimeZoneRule(stringBuilder.toString(), this.finalZone.getRawOffset(), 0, new long[]{(long) this.finalStartMillis}, 2);
            }
        }
        return rules;
    }

    private synchronized void initTransitionRules() {
        synchronized (this) {
            if (this.transitionRulesInitialized) {
                return;
            }
            this.initialRule = null;
            this.firstTZTransition = null;
            this.firstFinalTZTransition = null;
            this.historicRules = null;
            this.firstTZTransitionIdx = 0;
            this.finalZoneWithStartYear = null;
            String stdName = new StringBuilder();
            stdName.append(getID());
            stdName.append("(STD)");
            stdName = stdName.toString();
            String dstName = new StringBuilder();
            dstName.append(getID());
            dstName.append("(DST)");
            dstName = dstName.toString();
            int raw = initialRawOffset() * 1000;
            int dst = initialDstOffset() * 1000;
            this.initialRule = new InitialTimeZoneRule(dst == 0 ? stdName : dstName, raw, dst);
            if (this.transitionCount > 0) {
                int transitionIdx = 0;
                while (transitionIdx < this.transitionCount) {
                    if (getInt(this.typeMapData[transitionIdx]) != 0) {
                        break;
                    }
                    this.firstTZTransitionIdx++;
                    transitionIdx++;
                }
                String str;
                if (transitionIdx == this.transitionCount) {
                    str = dstName;
                } else {
                    long[] times = new long[this.transitionCount];
                    transitionIdx = dst;
                    dst = raw;
                    raw = 0;
                    while (true) {
                        long j = 1000;
                        if (raw >= this.typeCount) {
                            break;
                        }
                        int nTimes = 0;
                        int transitionIdx2 = this.firstTZTransitionIdx;
                        while (transitionIdx2 < this.transitionCount) {
                            if (raw == getInt(this.typeMapData[transitionIdx2])) {
                                long tt = this.transitionTimes64[transitionIdx2] * j;
                                str = dstName;
                                if (((double) tt) < this.finalStartMillis) {
                                    int nTimes2 = nTimes + 1;
                                    times[nTimes] = tt;
                                    nTimes = nTimes2;
                                }
                            } else {
                                str = dstName;
                            }
                            transitionIdx2++;
                            dstName = str;
                            j = 1000;
                        }
                        str = dstName;
                        if (nTimes > 0) {
                            long[] startTimes = new long[nTimes];
                            System.arraycopy(times, 0, startTimes, 0, nTimes);
                            dstName = this.typeOffsets[raw * 2] * 1000;
                            dst = this.typeOffsets[(raw * 2) + 1] * 1000;
                            if (this.historicRules == null) {
                                this.historicRules = new TimeArrayTimeZoneRule[this.typeCount];
                            }
                            this.historicRules[raw] = new TimeArrayTimeZoneRule(dst == 0 ? stdName : str, dstName, dst, startTimes, 2);
                            transitionIdx = dst;
                            dst = dstName;
                        }
                        raw++;
                        dstName = str;
                    }
                    this.firstTZTransition = new TimeZoneTransition(this.transitionTimes64[this.firstTZTransitionIdx] * 1000, this.initialRule, this.historicRules[getInt(this.typeMapData[this.firstTZTransitionIdx])]);
                }
            }
            if (this.finalZone != null) {
                TimeZoneRule firstFinalRule;
                TimeZoneRule firstFinalRule2;
                long startTime = (long) this.finalStartMillis;
                if (this.finalZone.useDaylightTime()) {
                    this.finalZoneWithStartYear = (SimpleTimeZone) this.finalZone.clone();
                    this.finalZoneWithStartYear.setStartYear(this.finalStartYear);
                    TimeZoneTransition tzt = this.finalZoneWithStartYear.getNextTransition(startTime, false);
                    firstFinalRule = tzt.getTo();
                    startTime = tzt.getTime();
                    firstFinalRule2 = firstFinalRule;
                } else {
                    this.finalZoneWithStartYear = this.finalZone;
                    firstFinalRule = new TimeArrayTimeZoneRule(this.finalZone.getID(), this.finalZone.getRawOffset(), 0, new long[]{startTime}, 2);
                }
                firstFinalRule = null;
                if (this.transitionCount > 0) {
                    firstFinalRule = this.historicRules[getInt(this.typeMapData[this.transitionCount - 1])];
                }
                if (firstFinalRule == null) {
                    firstFinalRule = this.initialRule;
                }
                this.firstFinalTZTransition = new TimeZoneTransition(startTime, firstFinalRule, firstFinalRule2);
            }
            this.transitionRulesInitialized = true;
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            boolean initialized = false;
            String tzid = getID();
            if (tzid != null) {
                try {
                    UResourceBundle top = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZONEINFORES, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                    construct(top, ZoneMeta.openOlsonResource(top, tzid));
                    if (this.finalZone != null) {
                        this.finalZone.setID(tzid);
                    }
                    initialized = true;
                } catch (Exception e) {
                }
            }
            if (!initialized) {
                constructEmpty();
            }
        }
        this.transitionRulesInitialized = false;
    }

    public boolean isFrozen() {
        return this.isFrozen;
    }

    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    public TimeZone cloneAsThawed() {
        OlsonTimeZone tz = (OlsonTimeZone) super.cloneAsThawed();
        if (this.finalZone != null) {
            this.finalZone.setID(getID());
            tz.finalZone = (SimpleTimeZone) this.finalZone.clone();
        }
        tz.isFrozen = false;
        return tz;
    }
}
