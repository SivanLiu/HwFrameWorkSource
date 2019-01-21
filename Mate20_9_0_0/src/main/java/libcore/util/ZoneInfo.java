package libcore.util;

import android.icu.lang.UCharacter.UnicodeBlock;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import libcore.io.BufferIterator;

public final class ZoneInfo extends TimeZone {
    private static final int[] LEAP = new int[]{0, 31, 60, 91, 121, 152, 182, 213, 244, UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F_ID, 305, 335};
    private static final long MILLISECONDS_PER_400_YEARS = 12622780800000L;
    private static final long MILLISECONDS_PER_DAY = 86400000;
    private static final int[] NORMAL = new int[]{0, 31, 59, 90, 120, 151, 181, 212, 243, UnicodeBlock.TANGUT_COMPONENTS_ID, 304, 334};
    private static final long UNIX_OFFSET = 62167219200000L;
    static final long serialVersionUID = -4598738130123921552L;
    private int mDstSavings;
    private final int mEarliestRawOffset;
    private final byte[] mIsDsts;
    private final int[] mOffsets;
    private int mRawOffset;
    private final long[] mTransitions;
    private final byte[] mTypes;
    private final boolean mUseDst;

    private static class CheckedArithmeticException extends Exception {
        private CheckedArithmeticException() {
        }
    }

    static class OffsetInterval {
        private final int endWallTimeSeconds;
        private final int isDst;
        private final int startWallTimeSeconds;
        private final int totalOffsetSeconds;

        public static OffsetInterval create(ZoneInfo timeZone, int transitionIndex) throws CheckedArithmeticException {
            if (transitionIndex < -1 || transitionIndex >= timeZone.mTransitions.length) {
                return null;
            }
            int rawOffsetSeconds = timeZone.mRawOffset / 1000;
            if (transitionIndex == -1) {
                return new OffsetInterval(Integer.MIN_VALUE, ZoneInfo.checkedAdd(timeZone.mTransitions[0], rawOffsetSeconds), 0, rawOffsetSeconds);
            }
            int endWallTimeSeconds;
            int type = timeZone.mTypes[transitionIndex] & 255;
            int totalOffsetSeconds = timeZone.mOffsets[type] + rawOffsetSeconds;
            if (transitionIndex == timeZone.mTransitions.length - 1) {
                endWallTimeSeconds = Integer.MAX_VALUE;
            } else {
                endWallTimeSeconds = ZoneInfo.checkedAdd(timeZone.mTransitions[transitionIndex + 1], totalOffsetSeconds);
            }
            return new OffsetInterval(ZoneInfo.checkedAdd(timeZone.mTransitions[transitionIndex], totalOffsetSeconds), endWallTimeSeconds, timeZone.mIsDsts[type], totalOffsetSeconds);
        }

        private OffsetInterval(int startWallTimeSeconds, int endWallTimeSeconds, int isDst, int totalOffsetSeconds) {
            this.startWallTimeSeconds = startWallTimeSeconds;
            this.endWallTimeSeconds = endWallTimeSeconds;
            this.isDst = isDst;
            this.totalOffsetSeconds = totalOffsetSeconds;
        }

        public boolean containsWallTime(long wallTimeSeconds) {
            return wallTimeSeconds >= ((long) this.startWallTimeSeconds) && wallTimeSeconds < ((long) this.endWallTimeSeconds);
        }

        public int getIsDst() {
            return this.isDst;
        }

        public int getTotalOffsetSeconds() {
            return this.totalOffsetSeconds;
        }

        public long getEndWallTimeSeconds() {
            return (long) this.endWallTimeSeconds;
        }

        public long getStartWallTimeSeconds() {
            return (long) this.startWallTimeSeconds;
        }
    }

    public static class WallTime {
        private final GregorianCalendar calendar = new GregorianCalendar(0, 0, 0, 0, 0, 0);
        private int gmtOffsetSeconds;
        private int hour;
        private int isDst;
        private int minute;
        private int month;
        private int monthDay;
        private int second;
        private int weekDay;
        private int year;
        private int yearDay;

        public WallTime() {
            this.calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public void localtime(int timeSeconds, ZoneInfo zoneInfo) {
            try {
                byte isDst;
                int offsetSeconds = zoneInfo.mRawOffset / 1000;
                if (zoneInfo.mTransitions.length == 0) {
                    isDst = (byte) 0;
                } else {
                    int offsetIndex = zoneInfo.findOffsetIndexForTimeInSeconds((long) timeSeconds);
                    if (offsetIndex == -1) {
                        isDst = (byte) 0;
                    } else {
                        offsetSeconds += zoneInfo.mOffsets[offsetIndex];
                        isDst = zoneInfo.mIsDsts[offsetIndex];
                    }
                }
                byte isDst2 = isDst;
                this.calendar.setTimeInMillis(((long) ZoneInfo.checkedAdd((long) timeSeconds, offsetSeconds)) * 1000);
                copyFieldsFromCalendar();
                this.isDst = isDst2;
                this.gmtOffsetSeconds = offsetSeconds;
            } catch (CheckedArithmeticException e) {
            }
        }

        public int mktime(ZoneInfo zoneInfo) {
            int i;
            if (this.isDst > 0) {
                this.isDst = 1;
                i = 1;
            } else if (this.isDst < 0) {
                this.isDst = -1;
                i = -1;
            } else {
                i = 0;
            }
            this.isDst = i;
            copyFieldsToCalendar();
            long longWallTimeSeconds = this.calendar.getTimeInMillis() / 1000;
            if (-2147483648L > longWallTimeSeconds || longWallTimeSeconds > 2147483647L) {
                return -1;
            }
            i = (int) longWallTimeSeconds;
            try {
                int rawOffsetSeconds = zoneInfo.mRawOffset / 1000;
                int rawTimeSeconds = ZoneInfo.checkedSubtract(i, rawOffsetSeconds);
                if (zoneInfo.mTransitions.length != 0) {
                    int initialTransitionIndex = zoneInfo.findTransitionIndex((long) rawTimeSeconds);
                    if (this.isDst < 0) {
                        Integer result = doWallTimeSearch(zoneInfo, initialTransitionIndex, i, true);
                        return result == null ? -1 : result.intValue();
                    }
                    Integer result2 = doWallTimeSearch(zoneInfo, initialTransitionIndex, i, true);
                    if (result2 == null) {
                        result2 = doWallTimeSearch(zoneInfo, initialTransitionIndex, i, false);
                    }
                    if (result2 == null) {
                        result2 = Integer.valueOf(-1);
                    }
                    return result2.intValue();
                } else if (this.isDst > 0) {
                    return -1;
                } else {
                    copyFieldsFromCalendar();
                    this.isDst = 0;
                    this.gmtOffsetSeconds = rawOffsetSeconds;
                    return rawTimeSeconds;
                }
            } catch (CheckedArithmeticException e) {
                return -1;
            }
        }

        private Integer tryOffsetAdjustments(ZoneInfo zoneInfo, int oldWallTimeSeconds, OffsetInterval targetInterval, int transitionIndex, int isDstToFind) throws CheckedArithmeticException {
            int[] offsetsToTry = getOffsetsOfType(zoneInfo, transitionIndex, isDstToFind);
            int j = 0;
            while (j < offsetsToTry.length) {
                int jOffsetSeconds = offsetsToTry[j] + (zoneInfo.mRawOffset / 1000);
                int targetIntervalOffsetSeconds = targetInterval.getTotalOffsetSeconds();
                int adjustedWallTimeSeconds = ZoneInfo.checkedAdd((long) oldWallTimeSeconds, targetIntervalOffsetSeconds - jOffsetSeconds);
                if (targetInterval.containsWallTime((long) adjustedWallTimeSeconds)) {
                    int returnValue = ZoneInfo.checkedSubtract(adjustedWallTimeSeconds, targetIntervalOffsetSeconds);
                    this.calendar.setTimeInMillis(((long) adjustedWallTimeSeconds) * 1000);
                    copyFieldsFromCalendar();
                    this.isDst = targetInterval.getIsDst();
                    this.gmtOffsetSeconds = targetIntervalOffsetSeconds;
                    return Integer.valueOf(returnValue);
                }
                j++;
                ZoneInfo zoneInfo2 = zoneInfo;
                int i = transitionIndex;
            }
            int i2 = oldWallTimeSeconds;
            OffsetInterval offsetInterval = targetInterval;
            return null;
        }

        private static int[] getOffsetsOfType(ZoneInfo zoneInfo, int startIndex, int isDst) {
            int[] offsets = new int[(zoneInfo.mOffsets.length + 1)];
            boolean[] seen = new boolean[zoneInfo.mOffsets.length];
            int delta = 0;
            boolean clampTop = false;
            int numFound = 0;
            boolean clampBottom = false;
            while (true) {
                delta *= -1;
                if (delta >= 0) {
                    delta++;
                }
                int transitionIndex = startIndex + delta;
                int type;
                if (delta < 0 && transitionIndex < -1) {
                    clampBottom = true;
                } else if (delta > 0 && transitionIndex >= zoneInfo.mTypes.length) {
                    clampTop = true;
                } else if (transitionIndex != -1) {
                    type = zoneInfo.mTypes[transitionIndex] & 255;
                    if (!seen[type]) {
                        if (zoneInfo.mIsDsts[type] == isDst) {
                            int numFound2 = numFound + 1;
                            offsets[numFound] = zoneInfo.mOffsets[type];
                            numFound = numFound2;
                        }
                        seen[type] = true;
                    }
                } else if (isDst == 0) {
                    type = numFound + 1;
                    offsets[numFound] = 0;
                    numFound = type;
                }
                if (clampTop && clampBottom) {
                    int[] toReturn = new int[numFound];
                    System.arraycopy(offsets, 0, toReturn, 0, numFound);
                    return toReturn;
                }
            }
        }

        private Integer doWallTimeSearch(ZoneInfo zoneInfo, int initialTransitionIndex, int wallTimeSeconds, boolean mustMatchDst) throws CheckedArithmeticException {
            int transitionIndexDelta;
            OffsetInterval offsetInterval;
            int i = wallTimeSeconds;
            boolean clampTop = false;
            int clampBottom = false;
            int loop = 0;
            while (true) {
                boolean clampTop2;
                transitionIndexDelta = (loop + 1) / 2;
                boolean z = true;
                if (loop % 2 == 1) {
                    transitionIndexDelta *= -1;
                }
                int transitionIndexDelta2 = transitionIndexDelta;
                int loop2 = loop + 1;
                if ((transitionIndexDelta2 <= 0 || !clampTop) && (transitionIndexDelta2 >= 0 || clampBottom == 0)) {
                    int currentTransitionIndex = initialTransitionIndex + transitionIndexDelta2;
                    ZoneInfo zoneInfo2 = zoneInfo;
                    offsetInterval = OffsetInterval.create(zoneInfo2, currentTransitionIndex);
                    if (offsetInterval == null) {
                        int i2;
                        boolean clampTop3 = (transitionIndexDelta2 > 0) | clampTop;
                        if (transitionIndexDelta2 >= 0) {
                            i2 = 0;
                        }
                        clampTop = clampTop3;
                        clampBottom |= i2;
                    } else {
                        OffsetInterval offsetInterval2;
                        int i3;
                        if (mustMatchDst) {
                            if (!(offsetInterval.containsWallTime((long) i) && (this.isDst == -1 || offsetInterval.getIsDst() == this.isDst))) {
                                offsetInterval2 = offsetInterval;
                                i3 = currentTransitionIndex;
                            }
                        } else if (this.isDst != offsetInterval.getIsDst()) {
                            offsetInterval2 = offsetInterval;
                            Integer returnValue = tryOffsetAdjustments(zoneInfo2, i, offsetInterval, currentTransitionIndex, this.isDst);
                            if (returnValue != null) {
                                return returnValue;
                            }
                        } else {
                            offsetInterval2 = offsetInterval;
                            i3 = currentTransitionIndex;
                        }
                        if (transitionIndexDelta2 > 0) {
                            clampTop2 = clampTop;
                            if (offsetInterval2.getEndWallTimeSeconds() - ((long) i) <= 86400) {
                                z = false;
                            }
                            clampTop = z ? true : clampTop2;
                        } else {
                            clampTop2 = clampTop;
                            OffsetInterval offsetInterval3 = offsetInterval2;
                            if (transitionIndexDelta2 < 0) {
                                if (((long) i) - offsetInterval3.getStartWallTimeSeconds() < 86400) {
                                    z = false;
                                }
                                if (z) {
                                    clampBottom = true;
                                    clampTop = clampTop2;
                                }
                            }
                        }
                    }
                    if (!clampTop && clampBottom != 0) {
                        return null;
                    }
                    loop = loop2;
                } else {
                    clampTop2 = clampTop;
                }
                clampTop = clampTop2;
                if (!clampTop) {
                }
                loop = loop2;
            }
            loop = offsetInterval.getTotalOffsetSeconds();
            transitionIndexDelta = ZoneInfo.checkedSubtract(i, loop);
            copyFieldsFromCalendar();
            this.isDst = offsetInterval.getIsDst();
            this.gmtOffsetSeconds = loop;
            return Integer.valueOf(transitionIndexDelta);
        }

        public void setYear(int year) {
            this.year = year;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public void setMonthDay(int monthDay) {
            this.monthDay = monthDay;
        }

        public void setHour(int hour) {
            this.hour = hour;
        }

        public void setMinute(int minute) {
            this.minute = minute;
        }

        public void setSecond(int second) {
            this.second = second;
        }

        public void setWeekDay(int weekDay) {
            this.weekDay = weekDay;
        }

        public void setYearDay(int yearDay) {
            this.yearDay = yearDay;
        }

        public void setIsDst(int isDst) {
            this.isDst = isDst;
        }

        public void setGmtOffset(int gmtoff) {
            this.gmtOffsetSeconds = gmtoff;
        }

        public int getYear() {
            return this.year;
        }

        public int getMonth() {
            return this.month;
        }

        public int getMonthDay() {
            return this.monthDay;
        }

        public int getHour() {
            return this.hour;
        }

        public int getMinute() {
            return this.minute;
        }

        public int getSecond() {
            return this.second;
        }

        public int getWeekDay() {
            return this.weekDay;
        }

        public int getYearDay() {
            return this.yearDay;
        }

        public int getGmtOffset() {
            return this.gmtOffsetSeconds;
        }

        public int getIsDst() {
            return this.isDst;
        }

        private void copyFieldsToCalendar() {
            this.calendar.set(1, this.year);
            this.calendar.set(2, this.month);
            this.calendar.set(5, this.monthDay);
            this.calendar.set(11, this.hour);
            this.calendar.set(12, this.minute);
            this.calendar.set(13, this.second);
            this.calendar.set(14, 0);
        }

        private void copyFieldsFromCalendar() {
            this.year = this.calendar.get(1);
            this.month = this.calendar.get(2);
            this.monthDay = this.calendar.get(5);
            this.hour = this.calendar.get(11);
            this.minute = this.calendar.get(12);
            this.second = this.calendar.get(13);
            this.weekDay = this.calendar.get(7) - 1;
            this.yearDay = this.calendar.get(6) - 1;
        }
    }

    public static ZoneInfo readTimeZone(String id, BufferIterator it, long currentTimeMillis) throws IOException {
        String str = id;
        BufferIterator bufferIterator = it;
        int tzh_magic = it.readInt();
        StringBuilder stringBuilder;
        if (tzh_magic == 1415211366) {
            bufferIterator.skip(28);
            int tzh_timecnt = it.readInt();
            int MAX_TRANSITIONS = 2000;
            int MAX_TRANSITIONS2;
            if (tzh_timecnt < 0 || tzh_timecnt > 2000) {
                MAX_TRANSITIONS2 = 2000;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Timezone id=");
                stringBuilder.append(str);
                stringBuilder.append(" has an invalid number of transitions=");
                stringBuilder.append(tzh_timecnt);
                throw new IOException(stringBuilder.toString());
            }
            int tzh_typecnt = it.readInt();
            if (tzh_typecnt < 1) {
                MAX_TRANSITIONS2 = 2000;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ZoneInfo requires at least one type to be provided for each timezone but could not find one for '");
                stringBuilder.append(str);
                stringBuilder.append("'");
                throw new IOException(stringBuilder.toString());
            } else if (tzh_typecnt <= 256) {
                StringBuilder stringBuilder2;
                bufferIterator.skip(4);
                int[] transitions32 = new int[tzh_timecnt];
                int i = 0;
                bufferIterator.readIntArray(transitions32, 0, transitions32.length);
                long[] transitions64 = new long[tzh_timecnt];
                int i2 = 0;
                while (i2 < tzh_timecnt) {
                    transitions64[i2] = (long) transitions32[i2];
                    if (i2 <= 0 || transitions64[i2] > transitions64[i2 - 1]) {
                        i2++;
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(str);
                        stringBuilder2.append(" transition at ");
                        stringBuilder2.append(i2);
                        stringBuilder2.append(" is not sorted correctly, is ");
                        stringBuilder2.append(transitions64[i2]);
                        stringBuilder2.append(", previous is ");
                        stringBuilder2.append(transitions64[i2 - 1]);
                        throw new IOException(stringBuilder2.toString());
                    }
                }
                byte[] type = new byte[tzh_timecnt];
                bufferIterator.readByteArray(type, 0, type.length);
                i2 = 0;
                while (i2 < type.length) {
                    int typeIndex = type[i2] & 255;
                    if (typeIndex < tzh_typecnt) {
                        i2++;
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(str);
                        stringBuilder2.append(" type at ");
                        stringBuilder2.append(i2);
                        stringBuilder2.append(" is not < ");
                        stringBuilder2.append(tzh_typecnt);
                        stringBuilder2.append(", is ");
                        stringBuilder2.append(typeIndex);
                        throw new IOException(stringBuilder2.toString());
                    }
                }
                int[] gmtOffsets = new int[tzh_typecnt];
                byte[] isDsts = new byte[tzh_typecnt];
                while (true) {
                    i2 = i;
                    if (i2 < tzh_typecnt) {
                        gmtOffsets[i2] = it.readInt();
                        byte isDst = it.readByte();
                        if (isDst == (byte) 0 || isDst == (byte) 1) {
                            MAX_TRANSITIONS2 = MAX_TRANSITIONS;
                            isDsts[i2] = isDst;
                            bufferIterator.skip(1);
                            i = i2 + 1;
                            MAX_TRANSITIONS = MAX_TRANSITIONS2;
                        } else {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(str);
                            stringBuilder3.append(" dst at ");
                            stringBuilder3.append(i2);
                            stringBuilder3.append(" is not 0 or 1, is ");
                            stringBuilder3.append(isDst);
                            throw new IOException(stringBuilder3.toString());
                        }
                    }
                    return new ZoneInfo(str, transitions64, type, gmtOffsets, isDsts, currentTimeMillis);
                }
            } else {
                MAX_TRANSITIONS2 = 2000;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Timezone with id ");
                stringBuilder.append(str);
                stringBuilder.append(" has too many types=");
                stringBuilder.append(tzh_typecnt);
                throw new IOException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Timezone id=");
        stringBuilder.append(str);
        stringBuilder.append(" has an invalid header=");
        stringBuilder.append(tzh_magic);
        throw new IOException(stringBuilder.toString());
    }

    private ZoneInfo(String name, long[] transitions, byte[] types, int[] gmtOffsets, byte[] isDsts, long currentTimeMillis) {
        String str = name;
        int[] iArr = gmtOffsets;
        if (iArr.length != 0) {
            int type;
            int i;
            this.mTransitions = transitions;
            this.mTypes = types;
            this.mIsDsts = isDsts;
            setID(name);
            int lastStd = -1;
            int lastDst = -1;
            int i2 = this.mTransitions.length - 1;
            while (true) {
                if ((lastStd == -1 || lastDst == -1) && i2 >= 0) {
                    type = this.mTypes[i2] & 255;
                    if (lastStd == -1 && this.mIsDsts[type] == (byte) 0) {
                        lastStd = i2;
                    }
                    if (lastDst == -1 && this.mIsDsts[type] != (byte) 0) {
                        lastDst = i2;
                    }
                    i2--;
                }
            }
            type = 0;
            if (this.mTransitions.length == 0) {
                this.mRawOffset = iArr[0];
            } else if (lastStd != -1) {
                this.mRawOffset = iArr[this.mTypes[lastStd] & 255];
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ZoneInfo requires at least one non-DST transition to be provided for each timezone that has at least one transition but could not find one for '");
                stringBuilder.append(str);
                stringBuilder.append("'");
                throw new IllegalStateException(stringBuilder.toString());
            }
            if (lastDst != -1 && this.mTransitions[lastDst] < roundUpMillisToSeconds(currentTimeMillis)) {
                lastDst = -1;
            }
            if (lastDst == -1) {
                this.mDstSavings = 0;
                this.mUseDst = false;
            } else {
                this.mDstSavings = (iArr[this.mTypes[lastDst] & 255] - iArr[this.mTypes[lastStd] & 255]) * 1000;
                this.mUseDst = true;
            }
            i2 = -1;
            for (i = 0; i < this.mTransitions.length; i++) {
                if (this.mIsDsts[this.mTypes[i] & 255] == (byte) 0) {
                    i2 = i;
                    break;
                }
            }
            i = i2 != -1 ? iArr[this.mTypes[i2] & 255] : this.mRawOffset;
            this.mOffsets = iArr;
            while (true) {
                int i3 = type;
                if (i3 < this.mOffsets.length) {
                    int[] iArr2 = this.mOffsets;
                    iArr2[i3] = iArr2[i3] - this.mRawOffset;
                    type = i3 + 1;
                } else {
                    this.mRawOffset *= 1000;
                    this.mEarliestRawOffset = i * 1000;
                    return;
                }
            }
        }
        long[] jArr = transitions;
        byte[] bArr = types;
        byte[] bArr2 = isDsts;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("ZoneInfo requires at least one offset to be provided for each timezone but could not find one for '");
        stringBuilder2.append(str);
        stringBuilder2.append("'");
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (!this.mUseDst && this.mDstSavings != 0) {
            this.mDstSavings = 0;
        }
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis) {
        year %= 400;
        long calc = ((((long) (year / 400)) * MILLISECONDS_PER_400_YEARS) + (((long) year) * 31536000000L)) + (((long) ((year + 3) / 4)) * 86400000);
        if (year > 0) {
            calc -= ((long) ((year - 1) / 100)) * 86400000;
        }
        boolean isLeap = year == 0 || (year % 4 == 0 && year % 100 != 0);
        return getOffset(((((calc + (((long) (isLeap ? LEAP : NORMAL)[month]) * 86400000)) + (((long) (day - 1)) * 86400000)) + ((long) millis)) - ((long) this.mRawOffset)) - UNIX_OFFSET);
    }

    public int findTransitionIndex(long seconds) {
        int transition = Arrays.binarySearch(this.mTransitions, seconds);
        if (transition < 0) {
            transition = (~transition) - 1;
            if (transition < 0) {
                return -1;
            }
        }
        return transition;
    }

    int findOffsetIndexForTimeInSeconds(long seconds) {
        int transition = findTransitionIndex(seconds);
        if (transition < 0) {
            return -1;
        }
        return this.mTypes[transition] & 255;
    }

    int findOffsetIndexForTimeInMilliseconds(long millis) {
        return findOffsetIndexForTimeInSeconds(roundDownMillisToSeconds(millis));
    }

    static long roundDownMillisToSeconds(long millis) {
        if (millis < 0) {
            return (millis - 999) / 1000;
        }
        return millis / 1000;
    }

    static long roundUpMillisToSeconds(long millis) {
        if (millis > 0) {
            return (999 + millis) / 1000;
        }
        return millis / 1000;
    }

    public int getOffsetsByUtcTime(long utcTimeInMillis, int[] offsets) {
        int rawOffset;
        int dstOffset;
        int totalOffset;
        int transitionIndex = findTransitionIndex(roundDownMillisToSeconds(utcTimeInMillis));
        if (transitionIndex == -1) {
            rawOffset = this.mEarliestRawOffset;
            dstOffset = 0;
            totalOffset = rawOffset;
        } else {
            dstOffset = this.mTypes[transitionIndex] & 255;
            totalOffset = this.mRawOffset + (this.mOffsets[dstOffset] * 1000);
            if (this.mIsDsts[dstOffset] == (byte) 0) {
                rawOffset = totalOffset;
                dstOffset = 0;
            } else {
                int rawOffset2 = -1;
                do {
                    transitionIndex--;
                    if (transitionIndex < 0) {
                        break;
                    }
                    dstOffset = this.mTypes[transitionIndex] & 255;
                } while (this.mIsDsts[dstOffset] != (byte) 0);
                rawOffset2 = this.mRawOffset + (this.mOffsets[dstOffset] * 1000);
                if (rawOffset2 == -1) {
                    rawOffset = this.mEarliestRawOffset;
                } else {
                    rawOffset = rawOffset2;
                }
                dstOffset = totalOffset - rawOffset;
            }
        }
        offsets[0] = rawOffset;
        offsets[1] = dstOffset;
        return totalOffset;
    }

    public int getOffset(long when) {
        int offsetIndex = findOffsetIndexForTimeInMilliseconds(when);
        if (offsetIndex == -1) {
            return this.mEarliestRawOffset;
        }
        return this.mRawOffset + (this.mOffsets[offsetIndex] * 1000);
    }

    public boolean inDaylightTime(Date time) {
        int offsetIndex = findOffsetIndexForTimeInMilliseconds(time.getTime());
        boolean z = false;
        if (offsetIndex == -1) {
            return false;
        }
        if (this.mIsDsts[offsetIndex] == (byte) 1) {
            z = true;
        }
        return z;
    }

    public int getRawOffset() {
        return this.mRawOffset;
    }

    public void setRawOffset(int off) {
        this.mRawOffset = off;
    }

    public int getDSTSavings() {
        return this.mDstSavings;
    }

    public boolean useDaylightTime() {
        return this.mUseDst;
    }

    public boolean hasSameRules(TimeZone timeZone) {
        boolean z = false;
        if (!(timeZone instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo other = (ZoneInfo) timeZone;
        if (this.mUseDst != other.mUseDst) {
            return false;
        }
        if (this.mUseDst) {
            if (this.mRawOffset == other.mRawOffset && Arrays.equals(this.mOffsets, other.mOffsets) && Arrays.equals(this.mIsDsts, other.mIsDsts) && Arrays.equals(this.mTypes, other.mTypes) && Arrays.equals(this.mTransitions, other.mTransitions)) {
                z = true;
            }
            return z;
        }
        if (this.mRawOffset == other.mRawOffset) {
            z = true;
        }
        return z;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo other = (ZoneInfo) obj;
        if (getID().equals(other.getID()) && hasSameRules(other)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * 1) + getID().hashCode())) + Arrays.hashCode(this.mOffsets))) + Arrays.hashCode(this.mIsDsts))) + this.mRawOffset)) + Arrays.hashCode(this.mTransitions))) + Arrays.hashCode(this.mTypes))) + (this.mUseDst ? 1231 : 1237);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("[id=\"");
        stringBuilder.append(getID());
        stringBuilder.append("\",mRawOffset=");
        stringBuilder.append(this.mRawOffset);
        stringBuilder.append(",mEarliestRawOffset=");
        stringBuilder.append(this.mEarliestRawOffset);
        stringBuilder.append(",mUseDst=");
        stringBuilder.append(this.mUseDst);
        stringBuilder.append(",mDstSavings=");
        stringBuilder.append(this.mDstSavings);
        stringBuilder.append(",transitions=");
        stringBuilder.append(this.mTransitions.length);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public Object clone() {
        return super.clone();
    }

    private static int checkedAdd(long a, int b) throws CheckedArithmeticException {
        long result = ((long) b) + a;
        if (result == ((long) ((int) result))) {
            return (int) result;
        }
        throw new CheckedArithmeticException();
    }

    private static int checkedSubtract(int a, int b) throws CheckedArithmeticException {
        long result = ((long) a) - ((long) b);
        if (result == ((long) ((int) result))) {
            return (int) result;
        }
        throw new CheckedArithmeticException();
    }
}
