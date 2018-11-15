package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.EventList;
import android.app.usage.EventStats;
import android.app.usage.UsageEvents.Event;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Flog;
import android.util.HwPCUtils;
import java.util.List;

class IntervalStats {
    public Configuration activeConfiguration;
    public long beginTime;
    public final ArrayMap<Configuration, ConfigurationStats> configurations = new ArrayMap();
    public long endTime;
    public EventList events;
    public final EventTracker interactiveTracker = new EventTracker();
    int intervalType;
    public final EventTracker keyguardHiddenTracker = new EventTracker();
    public final EventTracker keyguardShownTracker = new EventTracker();
    public long lastTimeSaved;
    private final ArraySet<String> mStringCache = new ArraySet();
    public final EventTracker nonInteractiveTracker = new EventTracker();
    private String packageInForeground;
    public final ArrayMap<String, UsageStats> packageStats = new ArrayMap();

    public static final class EventTracker {
        public int count;
        public long curStartTime;
        public long duration;
        public long lastEventTime;

        public void commitTime(long timeStamp) {
            if (this.curStartTime != 0) {
                this.duration += timeStamp - this.duration;
                this.curStartTime = 0;
            }
        }

        public void update(long timeStamp) {
            if (this.curStartTime == 0) {
                this.count++;
            }
            commitTime(timeStamp);
            this.curStartTime = timeStamp;
            this.lastEventTime = timeStamp;
        }

        void addToEventStats(List<EventStats> out, int event, long beginTime, long endTime) {
            if (this.count != 0 || this.duration != 0) {
                EventStats ev = new EventStats();
                ev.mEventType = event;
                ev.mCount = this.count;
                ev.mTotalTime = this.duration;
                ev.mLastEventTime = this.lastEventTime;
                ev.mBeginTimeStamp = beginTime;
                ev.mEndTimeStamp = endTime;
                out.add(ev);
            }
        }
    }

    IntervalStats() {
    }

    UsageStats getOrCreateUsageStats(String packageName) {
        UsageStats usageStats = (UsageStats) this.packageStats.get(packageName);
        if (usageStats != null) {
            return usageStats;
        }
        usageStats = new UsageStats();
        usageStats.mPackageName = getCachedStringRef(packageName);
        usageStats.mBeginTimeStamp = this.beginTime;
        usageStats.mEndTimeStamp = this.endTime;
        this.packageStats.put(usageStats.mPackageName, usageStats);
        return usageStats;
    }

    ConfigurationStats getOrCreateConfigurationStats(Configuration config) {
        ConfigurationStats configStats = (ConfigurationStats) this.configurations.get(config);
        if (configStats != null) {
            return configStats;
        }
        configStats = new ConfigurationStats();
        configStats.mBeginTimeStamp = this.beginTime;
        configStats.mEndTimeStamp = this.endTime;
        configStats.mConfiguration = config;
        this.configurations.put(config, configStats);
        return configStats;
    }

    Event buildEvent(String packageName, String className) {
        Event event = new Event();
        event.mPackage = getCachedStringRef(packageName);
        if (className != null) {
            event.mClass = getCachedStringRef(className);
        }
        return event;
    }

    private boolean isStatefulEvent(int eventType) {
        switch (eventType) {
            case 1:
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    private boolean isUserVisibleEvent(int eventType) {
        return (eventType == 6 || eventType == 11) ? false : true;
    }

    void update(String packageName, long timeStamp, int eventType) {
        update(packageName, timeStamp, eventType, 0);
    }

    void update(String packageName, long timeStamp, int eventType, int displayId) {
        UsageStats usageStats = getOrCreateUsageStats(packageName);
        if ((eventType == 2 || eventType == 3) && (usageStats.mLastEvent == 1 || usageStats.mLastEvent == 4)) {
            if (this.intervalType == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("usagestats update: pkg=");
                stringBuilder.append(packageName);
                stringBuilder.append(", intervalType=");
                stringBuilder.append(this.intervalType);
                stringBuilder.append(" timeStamp=");
                stringBuilder.append(timeStamp);
                stringBuilder.append(",type=");
                stringBuilder.append(eventType);
                stringBuilder.append(",beginTime=");
                stringBuilder.append(usageStats.mBeginTimeStamp);
                stringBuilder.append(",lastTotalTime=");
                stringBuilder.append(usageStats.mTotalTimeInForeground);
                stringBuilder.append(",lastTimeUsed=");
                stringBuilder.append(usageStats.mLastTimeUsed);
                stringBuilder.append(",deltaTime=");
                stringBuilder.append(timeStamp - usageStats.mLastTimeUsed);
                stringBuilder.append(",totalTime=");
                stringBuilder.append((usageStats.mTotalTimeInForeground + timeStamp) - usageStats.mLastTimeUsed);
                stringBuilder.append(",LaunchCount=");
                stringBuilder.append(usageStats.mLaunchCount);
                Flog.i(1701, stringBuilder.toString());
            }
            usageStats.mTotalTimeInForeground += timeStamp - usageStats.mLastTimeUsed;
            if (isLandscape(this.activeConfiguration)) {
                usageStats.mLandTimeInForeground += timeStamp - usageStats.mLastLandTimeUsed;
            }
            if (displayId == HwPCUtils.getPCDisplayID()) {
                if (HwPCUtils.getIsWifiMode()) {
                    usageStats.mTimeInWirelessPCForeground += timeStamp - usageStats.mLastTimeUsedInWirelessPC;
                } else {
                    usageStats.mTimeInPCForeground += timeStamp - usageStats.mLastTimeUsedInPC;
                }
            }
        }
        if (isStatefulEvent(eventType)) {
            usageStats.mLastEvent = eventType;
        }
        if (isUserVisibleEvent(eventType)) {
            usageStats.mLastTimeUsed = timeStamp;
            this.packageInForeground = packageName;
            if (isLandscape(this.activeConfiguration)) {
                usageStats.mLastLandTimeUsed = timeStamp;
            }
            if (displayId == HwPCUtils.getPCDisplayID()) {
                if (HwPCUtils.getIsWifiMode()) {
                    usageStats.mLastTimeUsedInWirelessPC = timeStamp;
                } else {
                    usageStats.mLastTimeUsedInPC = timeStamp;
                }
            }
        }
        usageStats.mEndTimeStamp = timeStamp;
        if (eventType == 1) {
            usageStats.mLaunchCount++;
        }
        this.endTime = timeStamp;
    }

    void updateChooserCounts(String packageName, String category, String action) {
        ArrayMap<String, Integer> chooserCounts;
        UsageStats usageStats = getOrCreateUsageStats(packageName);
        if (usageStats.mChooserCounts == null) {
            usageStats.mChooserCounts = new ArrayMap();
        }
        int idx = usageStats.mChooserCounts.indexOfKey(action);
        if (idx < 0) {
            chooserCounts = new ArrayMap();
            usageStats.mChooserCounts.put(action, chooserCounts);
        } else {
            chooserCounts = (ArrayMap) usageStats.mChooserCounts.valueAt(idx);
        }
        chooserCounts.put(category, Integer.valueOf(((Integer) chooserCounts.getOrDefault(category, Integer.valueOf(0))).intValue() + 1));
    }

    private boolean isLandscape(Configuration config) {
        return config != null && config.orientation == 2;
    }

    void updateConfigurationStats(Configuration config, long timeStamp) {
        ConfigurationStats activeStats;
        if (this.activeConfiguration != null) {
            activeStats = (ConfigurationStats) this.configurations.get(this.activeConfiguration);
            if (activeStats != null) {
                activeStats.mTotalTimeActive += timeStamp - activeStats.mLastTimeActive;
                activeStats.mLastTimeActive = timeStamp - 1;
            }
        }
        if (!(this.packageInForeground == null || this.activeConfiguration == null || config == null || this.activeConfiguration.orientation == config.orientation)) {
            UsageStats usageStats = getOrCreateUsageStats(this.packageInForeground);
            if (usageStats.mLastEvent == 1 || usageStats.mLastEvent == 4) {
                if (!isLandscape(config)) {
                    usageStats.mLandTimeInForeground += timeStamp - usageStats.mLastLandTimeUsed;
                }
                usageStats.mLastLandTimeUsed = timeStamp;
            }
        }
        if (config != null) {
            activeStats = getOrCreateConfigurationStats(config);
            activeStats.mLastTimeActive = timeStamp;
            activeStats.mActivationCount++;
            this.activeConfiguration = activeStats.mConfiguration;
        }
        this.endTime = timeStamp;
    }

    void incrementAppLaunchCount(String packageName) {
        UsageStats usageStats = getOrCreateUsageStats(packageName);
        usageStats.mAppLaunchCount++;
    }

    void commitTime(long timeStamp) {
        this.interactiveTracker.commitTime(timeStamp);
        this.nonInteractiveTracker.commitTime(timeStamp);
        this.keyguardShownTracker.commitTime(timeStamp);
        this.keyguardHiddenTracker.commitTime(timeStamp);
    }

    void updateScreenInteractive(long timeStamp) {
        this.interactiveTracker.update(timeStamp);
        this.nonInteractiveTracker.commitTime(timeStamp);
    }

    void updateScreenNonInteractive(long timeStamp) {
        this.nonInteractiveTracker.update(timeStamp);
        this.interactiveTracker.commitTime(timeStamp);
    }

    void updateKeyguardShown(long timeStamp) {
        this.keyguardShownTracker.update(timeStamp);
        this.keyguardHiddenTracker.commitTime(timeStamp);
    }

    void updateKeyguardHidden(long timeStamp) {
        this.keyguardHiddenTracker.update(timeStamp);
        this.keyguardShownTracker.commitTime(timeStamp);
    }

    void addEventStatsTo(List<EventStats> out) {
        List<EventStats> list = out;
        this.interactiveTracker.addToEventStats(list, 15, this.beginTime, this.endTime);
        List<EventStats> list2 = out;
        this.nonInteractiveTracker.addToEventStats(list2, 16, this.beginTime, this.endTime);
        this.keyguardShownTracker.addToEventStats(list, 17, this.beginTime, this.endTime);
        this.keyguardHiddenTracker.addToEventStats(list2, 18, this.beginTime, this.endTime);
    }

    private String getCachedStringRef(String str) {
        int index = this.mStringCache.indexOf(str);
        if (index >= 0) {
            return (String) this.mStringCache.valueAt(index);
        }
        this.mStringCache.add(str);
        return str;
    }
}
