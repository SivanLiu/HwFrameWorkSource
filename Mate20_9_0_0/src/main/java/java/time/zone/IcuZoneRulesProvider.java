package java.time.zone;

import android.icu.util.AnnualTimeZoneRule;
import android.icu.util.BasicTimeZone;
import android.icu.util.DateTimeRule;
import android.icu.util.InitialTimeZoneRule;
import android.icu.util.TimeZone;
import android.icu.util.TimeZone.SystemTimeZoneType;
import android.icu.util.TimeZoneRule;
import android.icu.util.TimeZoneTransition;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import libcore.util.BasicLruCache;

public class IcuZoneRulesProvider extends ZoneRulesProvider {
    private static final int MAX_TRANSITIONS = 10000;
    private static final int SECONDS_IN_DAY = 86400;
    private final BasicLruCache<String, ZoneRules> cache = new ZoneRulesCache(8);

    private static class ZoneRulesCache extends BasicLruCache<String, ZoneRules> {
        ZoneRulesCache(int maxSize) {
            super(maxSize);
        }

        protected ZoneRules create(String zoneId) {
            String canonicalId = TimeZone.getCanonicalID(zoneId);
            if (canonicalId.equals(zoneId)) {
                return IcuZoneRulesProvider.generateZoneRules(zoneId);
            }
            return (ZoneRules) get(canonicalId);
        }
    }

    protected Set<String> provideZoneIds() {
        HashSet zoneIds = new HashSet(TimeZone.getAvailableIDs(SystemTimeZoneType.ANY, null, null));
        zoneIds.remove("GMT+0");
        zoneIds.remove("GMT-0");
        return zoneIds;
    }

    protected ZoneRules provideRules(String zoneId, boolean forCaching) {
        return (ZoneRules) this.cache.get(zoneId);
    }

    protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
        return new TreeMap(Collections.singletonMap(TimeZone.getTZDataVersion(), provideRules(zoneId, false)));
    }

    /* JADX WARNING: Removed duplicated region for block: B:42:0x01a2  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x0184  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static ZoneRules generateZoneRules(String zoneId) {
        TimeZoneRule[] rules;
        InitialTimeZoneRule initial;
        ZoneOffset baseStandardOffset;
        ZoneOffset baseWallOffset;
        AnnualTimeZoneRule last2;
        List<ZoneOffsetTransitionRule> lastRules;
        boolean baseWallOffset2;
        List<ZoneOffsetTransitionRule> lastRules2;
        String str = zoneId;
        TimeZone timeZone = TimeZone.getFrozenTimeZone(zoneId);
        boolean z = timeZone instanceof BasicTimeZone;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected time zone class ");
        stringBuilder.append(timeZone.getClass());
        verify(z, str, stringBuilder.toString());
        BasicTimeZone tz = (BasicTimeZone) timeZone;
        TimeZoneRule[] rules2 = tz.getTimeZoneRules();
        boolean z2 = false;
        InitialTimeZoneRule initial2 = rules2[0];
        ZoneOffset baseStandardOffset2 = millisToOffset(initial2.getRawOffset());
        ZoneOffset baseWallOffset3 = millisToOffset(initial2.getRawOffset() + initial2.getDSTSavings());
        List<ZoneOffsetTransition> standardOffsetTransitionList = new ArrayList();
        List<ZoneOffsetTransition> transitionList = new ArrayList();
        List<ZoneOffsetTransitionRule> lastRules3 = new ArrayList();
        AnnualTimeZoneRule last1 = null;
        AnnualTimeZoneRule last22 = null;
        TimeZoneTransition transition = tz.getNextTransition(0, false);
        int preLastDstSavings = 0;
        int transitionCount = 1;
        while (transition != null) {
            int transitionCount2;
            boolean hadEffect;
            int preLastDstSavings2;
            TimeZoneRule from = transition.getFrom();
            TimeZone timeZone2 = timeZone;
            timeZone = transition.getTo();
            boolean hadEffect2 = false;
            rules = rules2;
            initial = initial2;
            if (from.getRawOffset() != timeZone.getRawOffset()) {
                baseStandardOffset = baseStandardOffset2;
                baseWallOffset = baseWallOffset3;
                last2 = last22;
                standardOffsetTransitionList.add(new ZoneOffsetTransition(TimeUnit.MILLISECONDS.toSeconds(transition.getTime()), millisToOffset(from.getRawOffset()), millisToOffset(timeZone.getRawOffset())));
                hadEffect2 = true;
            } else {
                baseStandardOffset = baseStandardOffset2;
                baseWallOffset = baseWallOffset3;
                last2 = last22;
            }
            int fromTotalOffset = from.getRawOffset() + from.getDSTSavings();
            int toTotalOffset = timeZone.getRawOffset() + timeZone.getDSTSavings();
            if (fromTotalOffset != toTotalOffset) {
                lastRules = lastRules3;
                transitionCount2 = transitionCount;
                transitionList.add(new ZoneOffsetTransition(TimeUnit.MILLISECONDS.toSeconds(transition.getTime()), millisToOffset(fromTotalOffset), millisToOffset(toTotalOffset)));
                hadEffect = true;
            } else {
                lastRules = lastRules3;
                transitionCount2 = transitionCount;
                hadEffect = hadEffect2;
            }
            verify(hadEffect, str, "Transition changed neither total nor raw offset.");
            if (!(timeZone instanceof AnnualTimeZoneRule)) {
                verify(last1 == null, str, "Unexpected rule after AnnualTimeZoneRule.");
            } else if (last1 == null) {
                preLastDstSavings2 = from.getDSTSavings();
                AnnualTimeZoneRule last12 = (AnnualTimeZoneRule) timeZone;
                verify(last12.getEndYear() == Integer.MAX_VALUE, str, "AnnualTimeZoneRule is not permanent.");
                preLastDstSavings = preLastDstSavings2;
                last1 = last12;
            } else {
                last22 = (AnnualTimeZoneRule) timeZone;
                verify(last22.getEndYear() == Integer.MAX_VALUE, str, "AnnualTimeZoneRule is not permanent.");
                verify(tz.getNextTransition(transition.getTime(), false).getTo() == last1, str, "Unexpected rule after 2 AnnualTimeZoneRules.");
                preLastDstSavings2 = transitionCount2;
                baseWallOffset2 = false;
                if (last1 == null) {
                    if (last22 != null) {
                        baseWallOffset2 = true;
                    }
                    verify(baseWallOffset2, str, "Only one AnnualTimeZoneRule.");
                    lastRules2 = lastRules;
                    lastRules2.add(toZoneOffsetTransitionRule(last1, preLastDstSavings));
                    lastRules2.add(toZoneOffsetTransitionRule(last22, last1.getDSTSavings()));
                } else {
                    lastRules2 = lastRules;
                }
                return ZoneRules.of(baseStandardOffset, baseWallOffset, standardOffsetTransitionList, transitionList, lastRules2);
            }
            preLastDstSavings2 = transitionCount2;
            verify(preLastDstSavings2 <= MAX_TRANSITIONS, str, "More than 10000 transitions.");
            transition = tz.getNextTransition(transition.getTime(), false);
            transitionCount = preLastDstSavings2 + 1;
            z2 = false;
            timeZone = timeZone2;
            rules2 = rules;
            initial2 = initial;
            baseStandardOffset2 = baseStandardOffset;
            baseWallOffset3 = baseWallOffset;
            last22 = last2;
            lastRules3 = lastRules;
        }
        rules = rules2;
        initial = initial2;
        baseStandardOffset = baseStandardOffset2;
        baseWallOffset = baseWallOffset3;
        lastRules = lastRules3;
        last2 = last22;
        baseWallOffset2 = z2;
        if (last1 == null) {
        }
        return ZoneRules.of(baseStandardOffset, baseWallOffset, standardOffsetTransitionList, transitionList, lastRules2);
    }

    private static void verify(boolean check, String zoneId, String message) {
        if (!check) {
            throw new ZoneRulesException(String.format("Failed verification of zone %s: %s", zoneId, message));
        }
    }

    private static ZoneOffsetTransitionRule toZoneOffsetTransitionRule(AnnualTimeZoneRule rule, int dstSavingMillisBefore) {
        int dayOfMonthIndicator;
        LocalTime time;
        boolean z;
        TimeDefinition timeDefinition;
        DateTimeRule dateTimeRule = rule.getRule();
        Month month = Month.JANUARY.plus((long) dateTimeRule.getRuleMonth());
        DayOfWeek dayOfWeek = DayOfWeek.SATURDAY.plus((long) dateTimeRule.getRuleDayOfWeek());
        switch (dateTimeRule.getDateRuleType()) {
            case 0:
                dayOfMonthIndicator = dateTimeRule.getRuleDayOfMonth();
                dayOfWeek = null;
                break;
            case 1:
                throw new ZoneRulesException("Date rule type DOW is unsupported");
            case 2:
                dayOfMonthIndicator = dateTimeRule.getRuleDayOfMonth();
                break;
            case 3:
                dayOfMonthIndicator = ((-month.maxLength()) + dateTimeRule.getRuleDayOfMonth()) - 1;
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected date rule type: ");
                stringBuilder.append(dateTimeRule.getDateRuleType());
                throw new ZoneRulesException(stringBuilder.toString());
        }
        int dayOfMonthIndicator2 = dayOfMonthIndicator;
        dayOfMonthIndicator = (int) TimeUnit.MILLISECONDS.toSeconds((long) dateTimeRule.getRuleMillisInDay());
        if (dayOfMonthIndicator == SECONDS_IN_DAY) {
            time = LocalTime.MIDNIGHT;
            z = true;
        } else {
            time = LocalTime.ofSecondOfDay((long) dayOfMonthIndicator);
            z = false;
        }
        LocalTime time2 = time;
        boolean timeEndOfDay = z;
        switch (dateTimeRule.getTimeRuleType()) {
            case 0:
                timeDefinition = TimeDefinition.WALL;
                break;
            case 1:
                timeDefinition = TimeDefinition.STANDARD;
                break;
            case 2:
                timeDefinition = TimeDefinition.UTC;
                break;
            default:
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unexpected time rule type ");
                stringBuilder2.append(dateTimeRule.getTimeRuleType());
                throw new ZoneRulesException(stringBuilder2.toString());
        }
        return ZoneOffsetTransitionRule.of(month, dayOfMonthIndicator2, dayOfWeek, time2, timeEndOfDay, timeDefinition, millisToOffset(rule.getRawOffset()), millisToOffset(rule.getRawOffset() + dstSavingMillisBefore), millisToOffset(rule.getRawOffset() + rule.getDSTSavings()));
    }

    private static ZoneOffset millisToOffset(int offset) {
        return ZoneOffset.ofTotalSeconds((int) TimeUnit.MILLISECONDS.toSeconds((long) offset));
    }
}
