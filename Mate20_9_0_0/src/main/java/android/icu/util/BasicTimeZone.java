package android.icu.util;

import android.icu.impl.Grego;
import java.util.BitSet;
import java.util.Date;
import java.util.LinkedList;

public abstract class BasicTimeZone extends TimeZone {
    @Deprecated
    protected static final int FORMER_LATTER_MASK = 12;
    @Deprecated
    public static final int LOCAL_DST = 3;
    @Deprecated
    public static final int LOCAL_FORMER = 4;
    @Deprecated
    public static final int LOCAL_LATTER = 12;
    @Deprecated
    public static final int LOCAL_STD = 1;
    private static final long MILLIS_PER_YEAR = 31536000000L;
    @Deprecated
    protected static final int STD_DST_MASK = 3;
    private static final long serialVersionUID = -3204278532246180932L;

    public abstract TimeZoneTransition getNextTransition(long j, boolean z);

    public abstract TimeZoneTransition getPreviousTransition(long j, boolean z);

    public abstract TimeZoneRule[] getTimeZoneRules();

    public boolean hasEquivalentTransitions(TimeZone tz, long start, long end) {
        return hasEquivalentTransitions(tz, start, end, false);
    }

    /* JADX WARNING: Missing block: B:85:0x01b2, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:87:0x01b4, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean hasEquivalentTransitions(TimeZone tz, long start, long end, boolean ignoreDstAmount) {
        BasicTimeZone basicTimeZone = this;
        TimeZone timeZone = tz;
        long j = start;
        if (basicTimeZone == timeZone) {
            return true;
        }
        boolean z = false;
        if (!(timeZone instanceof BasicTimeZone)) {
            return false;
        }
        int[] offsets1 = new int[2];
        int[] offsets2 = new int[2];
        basicTimeZone.getOffset(j, false, offsets1);
        timeZone.getOffset(j, false, offsets2);
        if (ignoreDstAmount) {
            if (offsets1[0] + offsets1[1] != offsets2[0] + offsets2[1] || ((offsets1[1] != 0 && offsets2[1] == 0) || (offsets1[1] == 0 && offsets2[1] != 0))) {
                return false;
            }
        } else if (!(offsets1[0] == offsets2[0] && offsets1[1] == offsets2[1])) {
            return false;
        }
        long time = j;
        while (true) {
            TimeZoneTransition tr1 = basicTimeZone.getNextTransition(time, z);
            TimeZoneTransition tr2 = ((BasicTimeZone) timeZone).getNextTransition(time, z);
            if (ignoreDstAmount) {
                while (tr1 != null && tr1.getTime() <= end && tr1.getFrom().getRawOffset() + tr1.getFrom().getDSTSavings() == tr1.getTo().getRawOffset() + tr1.getTo().getDSTSavings() && tr1.getFrom().getDSTSavings() != 0 && tr1.getTo().getDSTSavings() != 0) {
                    tr1 = basicTimeZone.getNextTransition(tr1.getTime(), false);
                    z = false;
                    j = start;
                }
                while (tr2 != null && tr2.getTime() <= end && tr2.getFrom().getRawOffset() + tr2.getFrom().getDSTSavings() == tr2.getTo().getRawOffset() + tr2.getTo().getDSTSavings() && tr2.getFrom().getDSTSavings() != 0 && tr2.getTo().getDSTSavings() != 0) {
                    tr2 = ((BasicTimeZone) timeZone).getNextTransition(tr2.getTime(), false);
                    timeZone = tz;
                }
            }
            boolean inRange1 = false;
            boolean inRange2 = false;
            if (tr1 != null && tr1.getTime() <= end) {
                inRange1 = true;
            }
            if (tr2 != null && tr2.getTime() <= end) {
                inRange2 = true;
            }
            if (!inRange1 && !inRange2) {
                return true;
            }
            if (inRange1 && inRange2) {
                if (tr1.getTime() != tr2.getTime()) {
                    return false;
                }
                if (ignoreDstAmount) {
                    if (tr1.getTo().getRawOffset() + tr1.getTo().getDSTSavings() != tr2.getTo().getRawOffset() + tr2.getTo().getDSTSavings() || ((tr1.getTo().getDSTSavings() != 0 && tr2.getTo().getDSTSavings() == 0) || (tr1.getTo().getDSTSavings() == 0 && tr2.getTo().getDSTSavings() != 0))) {
                    }
                } else if (tr1.getTo().getRawOffset() == tr2.getTo().getRawOffset() && tr1.getTo().getDSTSavings() == tr2.getTo().getDSTSavings()) {
                }
                time = tr1.getTime();
                int i = 1;
                basicTimeZone = this;
                timeZone = tz;
                j = start;
                z = false;
            }
        }
        return false;
    }

    public TimeZoneRule[] getTimeZoneRules(long start) {
        BasicTimeZone basicTimeZone = this;
        long j = start;
        TimeZoneRule[] all = getTimeZoneRules();
        TimeZoneTransition tzt = basicTimeZone.getPreviousTransition(j, true);
        if (tzt == null) {
            return all;
        }
        int i;
        BitSet isProcessed = new BitSet(all.length);
        LinkedList filteredRules = new LinkedList();
        TimeZoneRule initial = new InitialTimeZoneRule(tzt.getTo().getName(), tzt.getTo().getRawOffset(), tzt.getTo().getDSTSavings());
        filteredRules.add(initial);
        boolean z = false;
        isProcessed.set(0);
        int i2 = 1;
        while (true) {
            i = i2;
            if (i >= all.length) {
                break;
            }
            int i3 = i;
            if (all[i].getNextStart(j, initial.getRawOffset(), initial.getDSTSavings(), 0) == null) {
                isProcessed.set(i3);
            }
            i2 = i3 + 1;
        }
        boolean bFinalStd = false;
        long time = j;
        boolean bFinalDst = false;
        while (true) {
            if (bFinalStd && bFinalDst) {
                break;
            }
            tzt = basicTimeZone.getNextTransition(time, z);
            if (tzt == null) {
                break;
            }
            time = tzt.getTime();
            TimeZoneRule toRule = tzt.getTo();
            i = 1;
            while (i < all.length && !all[i].equals(toRule)) {
                i++;
            }
            boolean z2;
            long j2;
            if (i >= all.length) {
                z2 = bFinalStd;
                j2 = time;
                throw new IllegalStateException("The rule was not found");
            } else if (!isProcessed.get(i)) {
                boolean bFinalDst2;
                if (toRule instanceof TimeArrayTimeZoneRule) {
                    TimeArrayTimeZoneRule tar = (TimeArrayTimeZoneRule) toRule;
                    long t = j;
                    while (true) {
                        z2 = bFinalStd;
                        j2 = time;
                        bFinalStd = t;
                        tzt = basicTimeZone.getNextTransition(bFinalStd, z);
                        if (!(tzt == null || tzt.getTo().equals(tar))) {
                            t = tzt.getTime();
                            bFinalStd = z2;
                            time = j2;
                            basicTimeZone = this;
                            z = false;
                        }
                    }
                    if (tzt != null) {
                        Date firstStart = tar.getFirstStart(tzt.getFrom().getRawOffset(), tzt.getFrom().getDSTSavings());
                        if (firstStart.getTime() > j) {
                            filteredRules.add(tar);
                        } else {
                            long[] times = tar.getStartTimes();
                            int timeType = tar.getTimeType();
                            t = bFinalStd;
                            bFinalStd = false;
                            while (bFinalStd < times.length) {
                                Date firstStart2;
                                t = times[bFinalStd];
                                if (timeType == 1) {
                                    firstStart2 = firstStart;
                                    t -= (long) tzt.getFrom().getRawOffset();
                                } else {
                                    firstStart2 = firstStart;
                                }
                                if (timeType == 0) {
                                    t -= (long) tzt.getFrom().getDSTSavings();
                                }
                                if (t > j) {
                                    break;
                                }
                                bFinalStd++;
                                firstStart = firstStart2;
                            }
                            int asize = times.length - bFinalStd;
                            if (asize > 0) {
                                long[] newtimes = new long[asize];
                                System.arraycopy(times, bFinalStd, newtimes, 0, asize);
                                filteredRules.add(new TimeArrayTimeZoneRule(tar.getName(), tar.getRawOffset(), tar.getDSTSavings(), newtimes, tar.getTimeType()));
                            }
                        }
                    }
                    bFinalDst2 = bFinalDst;
                    bFinalStd = z2;
                    bFinalDst = false;
                } else {
                    z2 = bFinalStd;
                    j2 = time;
                    if (toRule instanceof AnnualTimeZoneRule) {
                        AnnualTimeZoneRule ar = (AnnualTimeZoneRule) toRule;
                        if (ar.getFirstStart(tzt.getFrom().getRawOffset(), tzt.getFrom().getDSTSavings()).getTime() == tzt.getTime()) {
                            filteredRules.add(ar);
                            bFinalDst2 = bFinalDst;
                            bFinalDst = false;
                        } else {
                            int[] dfields = new int[6];
                            bFinalDst2 = bFinalDst;
                            Grego.timeToFields(tzt.getTime(), dfields);
                            bFinalDst = false;
                            filteredRules.add(new AnnualTimeZoneRule(ar.getName(), ar.getRawOffset(), ar.getDSTSavings(), ar.getRule(), dfields[0], ar.getEndYear()));
                        }
                        if (ar.getEndYear() == Integer.MAX_VALUE) {
                            if (ar.getDSTSavings() == 0) {
                                bFinalStd = true;
                            } else {
                                bFinalDst2 = true;
                            }
                        }
                    } else {
                        bFinalDst2 = bFinalDst;
                        bFinalDst = false;
                    }
                    bFinalStd = z2;
                }
                isProcessed.set(i);
                z = bFinalDst;
                time = j2;
                bFinalDst = bFinalDst2;
                basicTimeZone = this;
            }
        }
        return (TimeZoneRule[]) filteredRules.toArray(new TimeZoneRule[filteredRules.size()]);
    }

    /* JADX WARNING: Removed duplicated region for block: B:71:0x02f3  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x02ed  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public TimeZoneRule[] getSimpleTimeZoneRulesNear(long date) {
        int i;
        long j = date;
        AnnualTimeZoneRule[] annualRules = null;
        String initialName = getNextTransition(j, false);
        TimeZoneTransition tr;
        TimeZoneRule initialRule;
        if (initialName != null) {
            int initialDst;
            int initialRaw;
            String initialName2;
            TimeZoneRule tr2;
            String initialName3 = initialName.getFrom().getName();
            int initialRaw2 = initialName.getFrom().getRawOffset();
            int initialDst2 = initialName.getFrom().getDSTSavings();
            long nextTransitionTime = initialName.getTime();
            if (((initialName.getFrom().getDSTSavings() != 0 || initialName.getTo().getDSTSavings() == 0) && (initialName.getFrom().getDSTSavings() == 0 || initialName.getTo().getDSTSavings() != 0)) || j + MILLIS_PER_YEAR <= nextTransitionTime) {
                initialDst = initialDst2;
                initialRaw = initialRaw2;
                initialName2 = initialName3;
                initialName3 = initialName;
            } else {
                AnnualTimeZoneRule[] annualRules2;
                long nextTransitionTime2;
                int initialRaw3;
                AnnualTimeZoneRule secondRule;
                annualRules = new AnnualTimeZoneRule[2];
                initialName2 = initialName3;
                int[] dtfields = Grego.timeToFields((((long) initialName.getFrom().getRawOffset()) + nextTransitionTime) + ((long) initialName.getFrom().getDSTSavings()), null);
                initialDst = 0;
                int i2 = initialRaw2;
                annualRules[initialDst] = new AnnualTimeZoneRule(initialName.getTo().getName(), i2, initialName.getTo().getDSTSavings(), new DateTimeRule(dtfields[1], Grego.getDayOfWeekInMonth(dtfields[0], dtfields[1], dtfields[2]), dtfields[3], dtfields[5], 0), dtfields[0], Integer.MAX_VALUE);
                if (initialName.getTo().getRawOffset() == initialRaw2) {
                    tr = getNextTransition(nextTransitionTime, initialDst);
                    if (tr == null || (((tr.getFrom().getDSTSavings() != null || tr.getTo().getDSTSavings() == null) && (tr.getFrom().getDSTSavings() == null || tr.getTo().getDSTSavings() != null)) || nextTransitionTime + MILLIS_PER_YEAR <= tr.getTime())) {
                        annualRules2 = annualRules;
                        nextTransitionTime2 = nextTransitionTime;
                        initialDst = initialDst2;
                        initialRaw3 = initialRaw2;
                    } else {
                        AnnualTimeZoneRule[] annualRules3 = annualRules;
                        dtfields = Grego.timeToFields(((long) tr.getFrom().getDSTSavings()) + (tr.getTime() + ((long) tr.getFrom().getRawOffset())), dtfields);
                        DateTimeRule dtr = new DateTimeRule(dtfields[1], Grego.getDayOfWeekInMonth(dtfields[null], dtfields[1], dtfields[2]), dtfields[3], dtfields[5], 0);
                        secondRule = new AnnualTimeZoneRule(tr.getTo().getName(), tr.getTo().getRawOffset(), tr.getTo().getDSTSavings(), dtr, dtfields[null] - 1, Integer.MAX_VALUE);
                        initialDst = tr.getFrom().getRawOffset();
                        annualRules2 = annualRules3;
                        nextTransitionTime2 = nextTransitionTime;
                        int i3 = initialDst;
                        initialDst = initialDst2;
                        initialDst2 = tr.getFrom().getDSTSavings();
                        initialRaw3 = initialRaw2;
                        annualRules = secondRule.getPreviousStart(j, i3, initialDst2, 1);
                        if (annualRules != null && annualRules.getTime() <= j && initialRaw3 == tr.getTo().getRawOffset() && initialDst == tr.getTo().getDSTSavings()) {
                            annualRules2[1] = secondRule;
                        }
                        DateTimeRule dateTimeRule = dtr;
                    }
                } else {
                    annualRules2 = annualRules;
                    nextTransitionTime2 = nextTransitionTime;
                    initialDst = initialDst2;
                    initialRaw3 = initialRaw2;
                    initialName3 = initialName;
                }
                if (annualRules2[1] == null) {
                    initialName3 = getPreviousTransition(j, true);
                    if (initialName3 == null || ((initialName3.getFrom().getDSTSavings() != 0 || initialName3.getTo().getDSTSavings() == 0) && (initialName3.getFrom().getDSTSavings() == 0 || initialName3.getTo().getDSTSavings() != 0))) {
                        initialRaw = initialRaw3;
                        initialName = true;
                    } else {
                        dtfields = Grego.timeToFields((initialName3.getTime() + ((long) initialName3.getFrom().getRawOffset())) + ((long) initialName3.getFrom().getDSTSavings()), dtfields);
                        initialRaw = initialRaw3;
                        i2 = initialRaw;
                        int i4 = initialDst;
                        secondRule = new AnnualTimeZoneRule(initialName3.getTo().getName(), i2, i4, new DateTimeRule(dtfields[1], Grego.getDayOfWeekInMonth(dtfields[0], dtfields[1], dtfields[2]), dtfields[3], dtfields[5], 0), annualRules2[null].getStartYear() - 1, Integer.MAX_VALUE);
                        if (secondRule.getNextStart(j, initialName3.getFrom().getRawOffset(), initialName3.getFrom().getDSTSavings(), false).getTime() > nextTransitionTime2) {
                            initialName = true;
                            annualRules2[1] = secondRule;
                        } else {
                            initialName = true;
                        }
                    }
                } else {
                    initialName = 1;
                    initialRaw = initialRaw3;
                }
                if (annualRules2[initialName] == null) {
                    annualRules = null;
                } else {
                    initialName = annualRules2[0].getName();
                    initialRaw2 = annualRules2[0].getRawOffset();
                    initialDst = annualRules2[0].getDSTSavings();
                    annualRules = annualRules2;
                    initialRule = new InitialTimeZoneRule(initialName, initialRaw2, initialDst);
                    tr2 = initialRule;
                }
            }
            initialRaw2 = initialRaw;
            initialName = initialName2;
            initialRule = new InitialTimeZoneRule(initialName, initialRaw2, initialDst);
            tr2 = initialRule;
        } else {
            tr = getPreviousTransition(j, true);
            if (tr != null) {
                initialRule = new InitialTimeZoneRule(tr.getTo().getName(), tr.getTo().getRawOffset(), tr.getTo().getDSTSavings());
            } else {
                int[] offsets = new int[2];
                getOffset(j, false, offsets);
                i = 1;
                initialRule = new InitialTimeZoneRule(getID(), offsets[0], offsets[1]);
                if (annualRules != null) {
                    TimeZoneRule[] result = new TimeZoneRule[i];
                    result[0] = initialRule;
                    return result;
                }
                return new TimeZoneRule[]{initialRule, annualRules[0], annualRules[i]};
            }
        }
        i = 1;
        if (annualRules != null) {
        }
    }

    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        throw new IllegalStateException("Not implemented");
    }

    protected BasicTimeZone() {
    }

    @Deprecated
    protected BasicTimeZone(String ID) {
        super(ID);
    }
}
