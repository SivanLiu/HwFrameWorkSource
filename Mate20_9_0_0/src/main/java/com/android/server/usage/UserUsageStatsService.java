package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.EventList;
import android.app.usage.EventStats;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Flog;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.am.AssistDataRequester;
import com.android.server.audio.AudioService;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.usage.IntervalStats.EventTracker;
import com.android.server.usage.UsageStatsDatabase.CheckinAction;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

class UserUsageStatsService {
    private static final boolean DEBUG = false;
    private static final long[] INTERVAL_LENGTH = new long[]{86400000, UnixCalendar.WEEK_IN_MILLIS, UnixCalendar.MONTH_IN_MILLIS, UnixCalendar.YEAR_IN_MILLIS};
    private static final String TAG = "UsageStatsService";
    private static final StatCombiner<ConfigurationStats> sConfigStatsCombiner = new StatCombiner<ConfigurationStats>() {
        public void combine(IntervalStats stats, boolean mutable, List<ConfigurationStats> accResult) {
            if (mutable) {
                int configCount = stats.configurations.size();
                for (int i = 0; i < configCount; i++) {
                    accResult.add(new ConfigurationStats((ConfigurationStats) stats.configurations.valueAt(i)));
                }
                return;
            }
            accResult.addAll(stats.configurations.values());
        }
    };
    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int sDateFormatFlags = 131093;
    private static final StatCombiner<EventStats> sEventStatsCombiner = new StatCombiner<EventStats>() {
        public void combine(IntervalStats stats, boolean mutable, List<EventStats> accResult) {
            stats.addEventStatsTo(accResult);
        }
    };
    private static final StatCombiner<UsageStats> sUsageStatsCombiner = new StatCombiner<UsageStats>() {
        public void combine(IntervalStats stats, boolean mutable, List<UsageStats> accResult) {
            if (mutable) {
                int statCount = stats.packageStats.size();
                for (int i = 0; i < statCount; i++) {
                    accResult.add(new UsageStats((UsageStats) stats.packageStats.valueAt(i)));
                }
                return;
            }
            accResult.addAll(stats.packageStats.values());
        }
    };
    private final Context mContext;
    private final IntervalStats[] mCurrentStats;
    private final UnixCalendar mDailyExpiryDate;
    private final UsageStatsDatabase mDatabase;
    private String mLastBackgroundedPackage;
    private final StatsUpdatedListener mListener;
    private final String mLogPrefix;
    private boolean mStatsChanged = false;
    private int mTimeZoneOffset = TimeZone.getDefault().getRawOffset();
    private final int mUserId;

    interface StatsUpdatedListener {
        void onNewUpdate(int i);

        void onStatsReloaded();

        void onStatsUpdated();
    }

    UserUsageStatsService(Context context, int userId, File usageStatsDir, StatsUpdatedListener listener) {
        this.mContext = context;
        this.mDailyExpiryDate = new UnixCalendar(0);
        this.mDatabase = new UsageStatsDatabase(usageStatsDir);
        this.mCurrentStats = new IntervalStats[4];
        this.mListener = listener;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User[");
        stringBuilder.append(Integer.toString(userId));
        stringBuilder.append("] ");
        this.mLogPrefix = stringBuilder.toString();
        this.mUserId = userId;
    }

    void init(long currentTimeMillis) {
        this.mDatabase.init(currentTimeMillis);
        int nullCount = 0;
        for (int i = 0; i < this.mCurrentStats.length; i++) {
            this.mCurrentStats[i] = this.mDatabase.getLatestUsageStats(i);
            if (this.mCurrentStats[i] == null) {
                nullCount++;
            } else {
                this.mCurrentStats[i].intervalType = i;
            }
        }
        if (nullCount > 0) {
            if (nullCount != this.mCurrentStats.length) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.mLogPrefix);
                stringBuilder.append("Some stats have no latest available");
                Slog.w(str, stringBuilder.toString());
            }
            loadActiveStats(currentTimeMillis, false);
        } else {
            updateRolloverDeadline(this.mCurrentStats[0].endTime);
        }
        for (IntervalStats stat : this.mCurrentStats) {
            int pkgCount = stat.packageStats.size();
            for (int i2 = 0; i2 < pkgCount; i2++) {
                UsageStats pkgStats = (UsageStats) stat.packageStats.valueAt(i2);
                if (pkgStats.mLastEvent == 1 || pkgStats.mLastEvent == 4) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("init report ev3: pkg=");
                    stringBuilder2.append(pkgStats.mPackageName);
                    stringBuilder2.append(" timeStamp=");
                    stringBuilder2.append(stat.lastTimeSaved);
                    stringBuilder2.append(", lastEvent:");
                    stringBuilder2.append(pkgStats.mLastEvent);
                    Flog.i(1701, stringBuilder2.toString());
                    stat.update(pkgStats.mPackageName, stat.lastTimeSaved, 3);
                    notifyStatsChanged();
                }
            }
            stat.updateConfigurationStats(null, stat.lastTimeSaved);
        }
        if (this.mDatabase.isNewUpdate()) {
            notifyNewUpdate();
        }
    }

    void onTimeChanged(long oldTime, long newTime) {
        persistActiveStats();
        this.mDatabase.onTimeChanged(newTime - oldTime);
        loadActiveStats(newTime, true);
    }

    void reportEvent(Event event) {
        Event event2 = event;
        if (event2.mEventType == 6 && this.mTimeZoneOffset != TimeZone.getDefault().getRawOffset()) {
            Slog.d(TAG, "TimeZone has changed, updateRolloverDeadline!");
            this.mTimeZoneOffset = TimeZone.getDefault().getRawOffset();
            updateRolloverDeadline(event2.mTimeStamp);
        }
        if (event2.mTimeStamp >= this.mDailyExpiryDate.getTimeInMillis()) {
            rolloverStats(event2.mTimeStamp);
        }
        IntervalStats currentDailyStats = this.mCurrentStats[0];
        Configuration newFullConfig = event2.mConfiguration;
        if (event2.mEventType == 5 && currentDailyStats.activeConfiguration != null) {
            event2.mConfiguration = Configuration.generateDelta(currentDailyStats.activeConfiguration, newFullConfig);
        }
        if (currentDailyStats.events == null) {
            currentDailyStats.events = new EventList();
        }
        if (event2.mEventType != 6) {
            currentDailyStats.events.insert(event2);
        }
        boolean incrementAppLaunch = false;
        if (event2.mEventType == 1) {
            if (!(event2.mPackage == null || event2.mPackage.equals(this.mLastBackgroundedPackage))) {
                incrementAppLaunch = true;
            }
        } else if (event2.mEventType == 2 && event2.mPackage != null) {
            this.mLastBackgroundedPackage = event2.mPackage;
        }
        for (IntervalStats stats : this.mCurrentStats) {
            int i = event2.mEventType;
            IntervalStats stats2;
            if (i == 5) {
                stats.updateConfigurationStats(newFullConfig, event2.mTimeStamp);
            } else if (i != 9) {
                switch (i) {
                    case 15:
                        stats.updateScreenInteractive(event2.mTimeStamp);
                        break;
                    case 16:
                        stats.updateScreenNonInteractive(event2.mTimeStamp);
                        break;
                    case 17:
                        stats.updateKeyguardShown(event2.mTimeStamp);
                        break;
                    case 18:
                        stats.updateKeyguardHidden(event2.mTimeStamp);
                        break;
                    default:
                        stats2 = stats;
                        stats.update(event2.mPackage, event2.mTimeStamp, event2.mEventType, event2.mDisplayId);
                        if (!incrementAppLaunch) {
                            break;
                        }
                        stats2.incrementAppLaunchCount(event2.mPackage);
                        break;
                }
            } else {
                stats2 = stats;
                stats2.updateChooserCounts(event2.mPackage, event2.mContentType, event2.mAction);
                String[] annotations = event2.mContentAnnotations;
                if (annotations != null) {
                    for (String annotation : annotations) {
                        stats2.updateChooserCounts(event2.mPackage, annotation, event2.mAction);
                    }
                }
            }
        }
        notifyStatsChanged();
    }

    private <T> List<T> queryStats(int intervalType, long beginTime, long endTime, StatCombiner<T> combiner) {
        int intervalType2;
        long j = beginTime;
        long j2 = endTime;
        int i = intervalType;
        if (i == 4) {
            int intervalType3 = this.mDatabase.findBestFitBucket(j, j2);
            if (intervalType3 < 0) {
                intervalType3 = 0;
            }
            intervalType2 = intervalType3;
        } else {
            intervalType2 = i;
        }
        StatCombiner<T> statCombiner;
        if (intervalType2 < 0 || intervalType2 >= this.mCurrentStats.length) {
            statCombiner = combiner;
            return null;
        }
        IntervalStats currentStats = this.mCurrentStats[intervalType2];
        if (j >= currentStats.endTime) {
            return null;
        }
        List<T> results = this.mDatabase.queryUsageStats(intervalType2, j, Math.min(currentStats.beginTime, j2), combiner);
        if (j >= currentStats.endTime || j2 <= currentStats.beginTime) {
            statCombiner = combiner;
        } else {
            if (results == null) {
                results = new ArrayList();
            }
            combiner.combine(currentStats, true, results);
        }
        return results;
    }

    List<UsageStats> queryUsageStats(int bucketType, long beginTime, long endTime) {
        return queryStats(bucketType, beginTime, endTime, sUsageStatsCombiner);
    }

    List<ConfigurationStats> queryConfigurationStats(int bucketType, long beginTime, long endTime) {
        return queryStats(bucketType, beginTime, endTime, sConfigStatsCombiner);
    }

    List<EventStats> queryEventStats(int bucketType, long beginTime, long endTime) {
        return queryStats(bucketType, beginTime, endTime, sEventStatsCombiner);
    }

    UsageEvents queryEvents(long beginTime, long endTime, boolean obfuscateInstantApps) {
        ArraySet<String> names = new ArraySet();
        final long j = beginTime;
        final long j2 = endTime;
        final boolean z = obfuscateInstantApps;
        final ArraySet<String> arraySet = names;
        List<Event> results = queryStats(0, j, j2, new StatCombiner<Event>() {
            public void combine(IntervalStats stats, boolean mutable, List<Event> accumulatedResult) {
                if (stats.events != null) {
                    int startIndex = stats.events.firstIndexOnOrAfter(j);
                    int size = stats.events.size();
                    int i = startIndex;
                    while (i < size && stats.events.get(i).mTimeStamp < j2) {
                        Event event = stats.events.get(i);
                        if (z) {
                            event = event.getObfuscatedIfInstantApp();
                        }
                        arraySet.add(event.mPackage);
                        if (event.mClass != null) {
                            arraySet.add(event.mClass);
                        }
                        accumulatedResult.add(event);
                        i++;
                    }
                }
            }
        });
        if (results == null || results.isEmpty()) {
            return null;
        }
        String[] table = (String[]) names.toArray(new String[names.size()]);
        Arrays.sort(table);
        return new UsageEvents(results, table);
    }

    UsageEvents queryEventsForPackage(long beginTime, long endTime, String packageName) {
        ArraySet<String> names = new ArraySet();
        names.add(packageName);
        List<Event> results = queryStats(0, beginTime, endTime, new -$$Lambda$UserUsageStatsService$aWxPyFEggMep-oyju6mPXDEUesw(beginTime, endTime, packageName, names));
        if (results == null || results.isEmpty()) {
            return null;
        }
        String[] table = (String[]) names.toArray(new String[names.size()]);
        Arrays.sort(table);
        return new UsageEvents(results, table);
    }

    static /* synthetic */ void lambda$queryEventsForPackage$0(long beginTime, long endTime, String packageName, ArraySet names, IntervalStats stats, boolean mutable, List accumulatedResult) {
        if (stats.events != null) {
            int startIndex = stats.events.firstIndexOnOrAfter(beginTime);
            int size = stats.events.size();
            int i = startIndex;
            while (i < size && stats.events.get(i).mTimeStamp < endTime) {
                Event event = stats.events.get(i);
                if (packageName.equals(event.mPackage)) {
                    if (event.mClass != null) {
                        names.add(event.mClass);
                    }
                    accumulatedResult.add(event);
                }
                i++;
            }
        }
    }

    void persistActiveStats() {
        if (this.mStatsChanged) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mLogPrefix);
            stringBuilder.append("Flushing usage stats to disk");
            Slog.i(str, stringBuilder.toString());
            int i = 0;
            while (i < this.mCurrentStats.length) {
                try {
                    this.mDatabase.putUsageStats(i, this.mCurrentStats[i]);
                    i++;
                } catch (IOException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(this.mLogPrefix);
                    stringBuilder2.append("Failed to persist active stats");
                    Slog.e(str2, stringBuilder2.toString(), e);
                    return;
                }
            }
            this.mStatsChanged = false;
        }
    }

    private void rolloverStats(long currentTimeMillis) {
        int pkgCount;
        int i;
        long j = currentTimeMillis;
        long startTime = SystemClock.elapsedRealtime();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mLogPrefix);
        stringBuilder.append("Rolling over usage stats");
        Slog.i(str, stringBuilder.toString());
        int i2 = 0;
        Configuration previousConfig = this.mCurrentStats[0].activeConfiguration;
        ArraySet<String> continuePreviousDay = new ArraySet();
        IntervalStats[] intervalStatsArr = this.mCurrentStats;
        int length = intervalStatsArr.length;
        int i3 = 0;
        while (i3 < length) {
            IntervalStats[] intervalStatsArr2;
            int i4;
            IntervalStats stat = intervalStatsArr[i3];
            pkgCount = stat.packageStats.size();
            i = i2;
            while (i < pkgCount) {
                UsageStats pkgStats = (UsageStats) stat.packageStats.valueAt(i);
                if (pkgStats.mLastEvent == 1 || pkgStats.mLastEvent == 4) {
                    continuePreviousDay.add(pkgStats.mPackageName);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("rollover report ev3: pkg=");
                    stringBuilder2.append(pkgStats.mPackageName);
                    stringBuilder2.append(" timeStamp=");
                    intervalStatsArr2 = intervalStatsArr;
                    i4 = length;
                    stringBuilder2.append(this.mDailyExpiryDate.getTimeInMillis() - 1);
                    Flog.i(1701, stringBuilder2.toString());
                    stat.update(pkgStats.mPackageName, this.mDailyExpiryDate.getTimeInMillis() - 1, 3);
                    notifyStatsChanged();
                } else {
                    intervalStatsArr2 = intervalStatsArr;
                    i4 = length;
                }
                i++;
                intervalStatsArr = intervalStatsArr2;
                length = i4;
            }
            intervalStatsArr2 = intervalStatsArr;
            i4 = length;
            stat.updateConfigurationStats(null, this.mDailyExpiryDate.getTimeInMillis() - 1);
            stat.commitTime(this.mDailyExpiryDate.getTimeInMillis() - 1);
            i3++;
            intervalStatsArr = intervalStatsArr2;
            length = i4;
            i2 = 0;
        }
        persistActiveStats();
        this.mDatabase.prune(j);
        loadActiveStats(j, false);
        i2 = continuePreviousDay.size();
        int i5 = 0;
        while (i5 < i2) {
            String name = (String) continuePreviousDay.valueAt(i5);
            long beginTime = this.mCurrentStats[0].beginTime;
            IntervalStats[] intervalStatsArr3 = this.mCurrentStats;
            pkgCount = intervalStatsArr3.length;
            i = 0;
            while (i < pkgCount) {
                IntervalStats stat2 = intervalStatsArr3[i];
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("rollover report ev4: pkg=");
                stringBuilder3.append(name);
                stringBuilder3.append(" timeStamp=");
                stringBuilder3.append(beginTime);
                Flog.i(1701, stringBuilder3.toString());
                stat2.update(name, beginTime, 4);
                stat2.updateConfigurationStats(previousConfig, beginTime);
                notifyStatsChanged();
                i++;
                j = currentTimeMillis;
            }
            i5++;
            j = currentTimeMillis;
        }
        persistActiveStats();
        j = SystemClock.elapsedRealtime() - startTime;
        String str2 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append(this.mLogPrefix);
        stringBuilder4.append("Rolling over usage stats complete. Took ");
        stringBuilder4.append(j);
        stringBuilder4.append(" milliseconds");
        Slog.i(str2, stringBuilder4.toString());
    }

    private void notifyStatsChanged() {
        if (!this.mStatsChanged) {
            this.mStatsChanged = true;
            this.mListener.onStatsUpdated();
        }
    }

    private void notifyNewUpdate() {
        this.mListener.onNewUpdate(this.mUserId);
    }

    private void loadActiveStats(long currentTimeMillis, boolean force) {
        UnixCalendar tempCal = new UnixCalendar(0);
        UnixCalendar lastExpiryDate = new UnixCalendar(0);
        int intervalType = 0;
        while (intervalType < this.mCurrentStats.length) {
            tempCal.setTimeInMillis(currentTimeMillis);
            UnixCalendar.truncateTo(tempCal, intervalType);
            if (force || this.mCurrentStats[intervalType] == null || this.mCurrentStats[intervalType].beginTime != tempCal.getTimeInMillis()) {
                String str;
                StringBuilder stringBuilder;
                IntervalStats stats = this.mDatabase.getLatestUsageStats(intervalType);
                this.mCurrentStats[intervalType] = null;
                if (stats != null) {
                    lastExpiryDate.setTimeInMillis(stats.endTime);
                    UnixCalendar.truncateTo(lastExpiryDate, intervalType);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("check currentTime(");
                    stringBuilder.append(currentTimeMillis);
                    stringBuilder.append(") is between endTime(");
                    stringBuilder.append(stats.endTime);
                    stringBuilder.append(") and expireTime(");
                    stringBuilder.append(lastExpiryDate.getTimeInMillis() + INTERVAL_LENGTH[intervalType]);
                    stringBuilder.append(") for interval ");
                    stringBuilder.append(intervalType);
                    Slog.d(str, stringBuilder.toString());
                    if (currentTimeMillis > stats.endTime && currentTimeMillis < lastExpiryDate.getTimeInMillis() + INTERVAL_LENGTH[intervalType]) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(this.mLogPrefix);
                        stringBuilder.append("Loading existing stats ");
                        stringBuilder.append(stats.beginTime);
                        stringBuilder.append(" for interval ");
                        stringBuilder.append(intervalType);
                        Slog.d(str, stringBuilder.toString());
                        this.mCurrentStats[intervalType] = stats;
                    }
                }
                if (this.mCurrentStats[intervalType] == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mLogPrefix);
                    stringBuilder.append("Creating new stats ");
                    stringBuilder.append(tempCal.getTimeInMillis());
                    stringBuilder.append(" for interval ");
                    stringBuilder.append(intervalType);
                    Slog.d(str, stringBuilder.toString());
                    this.mCurrentStats[intervalType] = new IntervalStats();
                    this.mCurrentStats[intervalType].beginTime = tempCal.getTimeInMillis();
                    this.mCurrentStats[intervalType].endTime = currentTimeMillis;
                }
                this.mCurrentStats[intervalType].intervalType = intervalType;
            }
            intervalType++;
        }
        this.mStatsChanged = false;
        updateRolloverDeadline(currentTimeMillis);
        this.mListener.onStatsReloaded();
    }

    private void updateRolloverDeadline(long currentTimeMillis) {
        this.mDailyExpiryDate.setTimeInMillis(currentTimeMillis);
        this.mDailyExpiryDate.addDays(1);
        this.mDailyExpiryDate.truncateToDay();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mLogPrefix);
        stringBuilder.append("Rollover scheduled @ ");
        stringBuilder.append(sDateFormat.format(Long.valueOf(this.mDailyExpiryDate.getTimeInMillis())));
        stringBuilder.append("(");
        stringBuilder.append(this.mDailyExpiryDate.getTimeInMillis());
        stringBuilder.append(")");
        Slog.i(str, stringBuilder.toString());
    }

    void checkin(final IndentingPrintWriter pw) {
        this.mDatabase.checkinDailyFiles(new CheckinAction() {
            public boolean checkin(IntervalStats stats) {
                UserUsageStatsService.this.printIntervalStats(pw, stats, false, false, null);
                return true;
            }
        });
    }

    void dump(IndentingPrintWriter pw, String pkg) {
        dump(pw, pkg, false);
    }

    void dump(IndentingPrintWriter pw, String pkg, boolean compact) {
        printLast24HrEvents(pw, compact ^ 1, pkg);
        for (int interval = 0; interval < this.mCurrentStats.length; interval++) {
            pw.print("In-memory ");
            pw.print(intervalToString(interval));
            pw.println(" stats");
            printIntervalStats(pw, this.mCurrentStats[interval], compact ^ 1, true, pkg);
        }
    }

    private String formatDateTime(long dateTime, boolean pretty) {
        if (!pretty) {
            return Long.toString(dateTime);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"");
        stringBuilder.append(sDateFormat.format(Long.valueOf(dateTime)));
        stringBuilder.append("\"");
        return stringBuilder.toString();
    }

    private String formatElapsedTime(long elapsedTime, boolean pretty) {
        if (!pretty) {
            return Long.toString(elapsedTime);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"");
        stringBuilder.append(DateUtils.formatElapsedTime(elapsedTime / 1000));
        stringBuilder.append("\"");
        return stringBuilder.toString();
    }

    void printEvent(IndentingPrintWriter pw, Event event, boolean prettyDates) {
        pw.printPair("time", formatDateTime(event.mTimeStamp, prettyDates));
        pw.printPair(SoundModelContract.KEY_TYPE, eventToString(event.mEventType));
        pw.printPair("package", event.mPackage);
        if (event.mClass != null) {
            pw.printPair(AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS, event.mClass);
        }
        if (event.mConfiguration != null) {
            pw.printPair("config", Configuration.resourceQualifierString(event.mConfiguration));
        }
        if (event.mShortcutId != null) {
            pw.printPair("shortcutId", event.mShortcutId);
        }
        if (event.mEventType == 11) {
            pw.printPair("standbyBucket", Integer.valueOf(event.getStandbyBucket()));
            pw.printPair(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, UsageStatsManager.reasonToString(event.getStandbyReason()));
        }
        pw.printHexPair("flags", event.mFlags);
        pw.println();
    }

    void printLast24HrEvents(IndentingPrintWriter pw, boolean prettyDates, String pkg) {
        IndentingPrintWriter indentingPrintWriter = pw;
        boolean z = prettyDates;
        long endTime = System.currentTimeMillis();
        UnixCalendar yesterday = new UnixCalendar(endTime);
        yesterday.addDays(-1);
        long beginTime = yesterday.getTimeInMillis();
        final long j = beginTime;
        final long j2 = endTime;
        final String str = pkg;
        List<Event> events = queryStats(0, j, j2, new StatCombiner<Event>() {
            public void combine(IntervalStats stats, boolean mutable, List<Event> accumulatedResult) {
                if (stats.events != null) {
                    int startIndex = stats.events.firstIndexOnOrAfter(j);
                    int size = stats.events.size();
                    int i = startIndex;
                    while (i < size && stats.events.get(i).mTimeStamp < j2) {
                        Event event = stats.events.get(i);
                        if (str == null || str.equals(event.mPackage)) {
                            accumulatedResult.add(event);
                        }
                        i++;
                    }
                }
            }
        });
        indentingPrintWriter.print("Last 24 hour events (");
        if (z) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\"");
            StringBuilder stringBuilder2 = stringBuilder;
            stringBuilder2.append(DateUtils.formatDateRange(this.mContext, beginTime, endTime, sDateFormatFlags));
            stringBuilder2.append("\"");
            indentingPrintWriter.printPair("timeRange", stringBuilder2.toString());
        } else {
            indentingPrintWriter.printPair("beginTime", Long.valueOf(beginTime));
            indentingPrintWriter.printPair("endTime", Long.valueOf(endTime));
        }
        indentingPrintWriter.println(")");
        if (events != null) {
            pw.increaseIndent();
            for (Event event : events) {
                printEvent(indentingPrintWriter, event, z);
            }
            pw.decreaseIndent();
        }
    }

    void printEventAggregation(IndentingPrintWriter pw, String label, EventTracker tracker, boolean prettyDates) {
        if (tracker.count != 0 || tracker.duration != 0) {
            pw.print(label);
            pw.print(": ");
            pw.print(tracker.count);
            pw.print("x for ");
            pw.print(formatElapsedTime(tracker.duration, prettyDates));
            if (tracker.curStartTime != 0) {
                pw.print(" (now running, started at ");
                formatDateTime(tracker.curStartTime, prettyDates);
                pw.print(")");
            }
            pw.println();
        }
    }

    void printIntervalStats(IndentingPrintWriter pw, IntervalStats stats, boolean prettyDates, boolean skipEvents, String pkg) {
        int i;
        UsageStats usageStats;
        int pkgCount;
        int i2;
        IndentingPrintWriter indentingPrintWriter = pw;
        IntervalStats intervalStats = stats;
        boolean z = prettyDates;
        String str = pkg;
        if (z) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\"");
            stringBuilder.append(DateUtils.formatDateRange(this.mContext, intervalStats.beginTime, intervalStats.endTime, sDateFormatFlags));
            stringBuilder.append("\"");
            indentingPrintWriter.printPair("timeRange", stringBuilder.toString());
        } else {
            indentingPrintWriter.printPair("beginTime", Long.valueOf(intervalStats.beginTime));
            indentingPrintWriter.printPair("endTime", Long.valueOf(intervalStats.endTime));
        }
        pw.println();
        pw.increaseIndent();
        indentingPrintWriter.println("packages");
        pw.increaseIndent();
        ArrayMap<String, UsageStats> pkgStats = intervalStats.packageStats;
        int pkgCount2 = pkgStats.size();
        for (i = 0; i < pkgCount2; i++) {
            usageStats = (UsageStats) pkgStats.valueAt(i);
            if (str == null || str.equals(usageStats.mPackageName)) {
                indentingPrintWriter.printPair("package", usageStats.mPackageName);
                indentingPrintWriter.printPair("totalTime", formatElapsedTime(usageStats.mTotalTimeInForeground, z));
                indentingPrintWriter.printPair("lastTime", formatDateTime(usageStats.mLastTimeUsed, z));
                indentingPrintWriter.printPair("appLaunchCount", Integer.valueOf(usageStats.mAppLaunchCount));
                pw.println();
            }
        }
        pw.decreaseIndent();
        pw.println();
        indentingPrintWriter.println("ChooserCounts");
        pw.increaseIndent();
        Iterator it = pkgStats.values().iterator();
        while (it.hasNext()) {
            usageStats = (UsageStats) it.next();
            if (str == null || str.equals(usageStats.mPackageName)) {
                ArrayMap<String, UsageStats> pkgStats2;
                Iterator it2;
                UsageStats usageStats2;
                indentingPrintWriter.printPair("package", usageStats.mPackageName);
                if (usageStats.mChooserCounts != null) {
                    int chooserCountSize = usageStats.mChooserCounts.size();
                    for (int i3 = 0; i3 < chooserCountSize; i3++) {
                        String action = (String) usageStats.mChooserCounts.keyAt(i3);
                        ArrayMap<String, Integer> counts = (ArrayMap) usageStats.mChooserCounts.valueAt(i3);
                        int annotationSize = counts.size();
                        int j = 0;
                        while (j < annotationSize) {
                            String key = (String) counts.keyAt(j);
                            pkgStats2 = pkgStats;
                            pkgStats = ((Integer) counts.valueAt(j)).intValue();
                            if (pkgStats != null) {
                                pkgCount = pkgCount2;
                                it2 = it;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(action);
                                usageStats2 = usageStats;
                                stringBuilder2.append(":");
                                stringBuilder2.append(key);
                                stringBuilder2.append(" is ");
                                stringBuilder2.append(Integer.toString(pkgStats));
                                indentingPrintWriter.printPair("ChooserCounts", stringBuilder2.toString());
                                pw.println();
                            } else {
                                pkgCount = pkgCount2;
                                it2 = it;
                                usageStats2 = usageStats;
                            }
                            j++;
                            pkgStats = pkgStats2;
                            pkgCount2 = pkgCount;
                            it = it2;
                            usageStats = usageStats2;
                        }
                        pkgCount = pkgCount2;
                        it2 = it;
                        usageStats2 = usageStats;
                    }
                }
                pkgStats2 = pkgStats;
                pkgCount = pkgCount2;
                it2 = it;
                usageStats2 = usageStats;
                pw.println();
                pkgStats = pkgStats2;
                pkgCount2 = pkgCount;
                it = it2;
            }
        }
        pkgCount = pkgCount2;
        pw.decreaseIndent();
        if (str == null) {
            indentingPrintWriter.println("configurations");
            pw.increaseIndent();
            ArrayMap<Configuration, ConfigurationStats> configStats = intervalStats.configurations;
            pkgCount2 = configStats.size();
            for (i2 = 0; i2 < pkgCount2; i2++) {
                ConfigurationStats config = (ConfigurationStats) configStats.valueAt(i2);
                indentingPrintWriter.printPair("config", Configuration.resourceQualifierString(config.mConfiguration));
                indentingPrintWriter.printPair("totalTime", formatElapsedTime(config.mTotalTimeActive, z));
                indentingPrintWriter.printPair("lastTime", formatDateTime(config.mLastTimeActive, z));
                indentingPrintWriter.printPair(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.valueOf(config.mActivationCount));
                pw.println();
            }
            pw.decreaseIndent();
            indentingPrintWriter.println("event aggregations");
            pw.increaseIndent();
            printEventAggregation(indentingPrintWriter, "screen-interactive", intervalStats.interactiveTracker, z);
            printEventAggregation(indentingPrintWriter, "screen-non-interactive", intervalStats.nonInteractiveTracker, z);
            printEventAggregation(indentingPrintWriter, "keyguard-shown", intervalStats.keyguardShownTracker, z);
            printEventAggregation(indentingPrintWriter, "keyguard-hidden", intervalStats.keyguardHiddenTracker, z);
            pw.decreaseIndent();
        }
        if (!skipEvents) {
            indentingPrintWriter.println("events");
            pw.increaseIndent();
            EventList events = intervalStats.events;
            i2 = events != null ? events.size() : 0;
            int i4 = 0;
            while (true) {
                i = i4;
                if (i >= i2) {
                    break;
                }
                Event event = events.get(i);
                if (str == null || str.equals(event.mPackage)) {
                    printEvent(indentingPrintWriter, event, z);
                }
                i4 = i + 1;
            }
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    private static String intervalToString(int interval) {
        switch (interval) {
            case 0:
                return "daily";
            case 1:
                return "weekly";
            case 2:
                return "monthly";
            case 3:
                return "yearly";
            default:
                return "?";
        }
    }

    private static String eventToString(int eventType) {
        switch (eventType) {
            case 0:
                return "NONE";
            case 1:
                return "MOVE_TO_FOREGROUND";
            case 2:
                return "MOVE_TO_BACKGROUND";
            case 3:
                return "END_OF_DAY";
            case 4:
                return "CONTINUE_PREVIOUS_DAY";
            case 5:
                return "CONFIGURATION_CHANGE";
            case 6:
                return "SYSTEM_INTERACTION";
            case 7:
                return "USER_INTERACTION";
            case 8:
                return "SHORTCUT_INVOCATION";
            case 9:
                return "CHOOSER_ACTION";
            case 10:
                return "NOTIFICATION_SEEN";
            case 11:
                return "STANDBY_BUCKET_CHANGED";
            case 12:
                return "NOTIFICATION_INTERRUPTION";
            case 13:
                return "SLICE_PINNED_PRIV";
            case 14:
                return "SLICE_PINNED";
            case 15:
                return "SCREEN_INTERACTIVE";
            case 16:
                return "SCREEN_NON_INTERACTIVE";
            default:
                return "UNKNOWN";
        }
    }

    byte[] getBackupPayload(String key) {
        return this.mDatabase.getBackupPayload(key);
    }

    void applyRestoredPayload(String key, byte[] payload) {
        this.mDatabase.applyRestoredPayload(key, payload);
    }
}
