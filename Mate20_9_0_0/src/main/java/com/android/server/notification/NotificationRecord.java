package com.android.server.notification;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationChannel;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.AudioSystem;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.service.notification.Adjustment;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.NotificationStats;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.notification.NotificationUsageStats.SingleNotificationStats;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NotificationRecord {
    static final boolean DBG = Log.isLoggable(TAG, 3);
    private static final int MAX_LOGTAG_LENGTH = 35;
    static final String TAG = "NotificationRecord";
    boolean isCanceled;
    public boolean isUpdate;
    private final List<Adjustment> mAdjustments;
    IActivityManager mAm;
    private AudioAttributes mAttributes;
    private int mAuthoritativeRank;
    private NotificationChannel mChannel;
    private String mChannelIdLogTag;
    private float mContactAffinity;
    private final Context mContext;
    private long mCreationTimeMs;
    private String mGlobalSortKey;
    private ArraySet<Uri> mGrantableUris;
    private String mGroupLogTag;
    private boolean mHasSeenSmartReplies;
    private boolean mHidden;
    private int mImportance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
    private CharSequence mImportanceExplanation = null;
    private boolean mIntercept;
    private boolean mIsAppImportanceLocked;
    private boolean mIsInterruptive;
    private long mLastIntrusive;
    private Light mLight;
    private LogMaker mLogMaker;
    private int mNumberOfSmartRepliesAdded;
    final int mOriginalFlags;
    private int mPackagePriority;
    private int mPackageVisibility;
    private String mPeopleExplanation;
    private ArrayList<String> mPeopleOverride;
    private boolean mPreChannelsNotification = true;
    private long mPushLogPowerTimeMs;
    private long mRankingTimeMs;
    private boolean mRecentlyIntrusive;
    private boolean mRecordedInterruption;
    private boolean mShowBadge;
    private ArrayList<SnoozeCriterion> mSnoozeCriteria;
    private Uri mSound;
    private final NotificationStats mStats;
    private int mSuppressedVisualEffects = 0;
    final int mTargetSdkVersion;
    private boolean mTextChanged;
    private long mUpdateTimeMs;
    private String mUserExplanation;
    private int mUserImportance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
    private int mUserSentiment;
    private long[] mVibration;
    private long mVisibleSinceMs;
    IBinder permissionOwner;
    public final StatusBarNotification sbn;
    SingleNotificationStats stats;

    @VisibleForTesting
    static final class Light {
        public final int color;
        public final int offMs;
        public final int onMs;

        public Light(int color, int onMs, int offMs) {
            this.color = color;
            this.onMs = onMs;
            this.offMs = offMs;
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Light light = (Light) o;
            if (this.color != light.color || this.onMs != light.onMs) {
                return false;
            }
            if (this.offMs != light.offMs) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (31 * ((31 * this.color) + this.onMs)) + this.offMs;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Light{color=");
            stringBuilder.append(this.color);
            stringBuilder.append(", onMs=");
            stringBuilder.append(this.onMs);
            stringBuilder.append(", offMs=");
            stringBuilder.append(this.offMs);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    public NotificationRecord(Context context, StatusBarNotification sbn, NotificationChannel channel) {
        this.sbn = sbn;
        this.mTargetSdkVersion = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageTargetSdkVersion(sbn.getPackageName());
        this.mAm = ActivityManager.getService();
        this.mOriginalFlags = sbn.getNotification().flags;
        this.mRankingTimeMs = calculateRankingTimeMs(0);
        this.mCreationTimeMs = sbn.getPostTime();
        this.mUpdateTimeMs = this.mCreationTimeMs;
        this.mPushLogPowerTimeMs = this.mCreationTimeMs;
        this.mContext = context;
        this.stats = new SingleNotificationStats();
        this.mChannel = channel;
        this.mPreChannelsNotification = isPreChannelsNotification();
        this.mSound = calculateSound();
        this.mVibration = calculateVibration();
        this.mAttributes = calculateAttributes();
        this.mImportance = calculateImportance();
        this.mLight = calculateLights();
        this.mAdjustments = new ArrayList();
        this.mStats = new NotificationStats();
        calculateUserSentiment();
        calculateGrantableUris();
    }

    private boolean isPreChannelsNotification() {
        if (!"miscellaneous".equals(getChannel().getId()) || this.mTargetSdkVersion >= 26) {
            return false;
        }
        return true;
    }

    private Uri calculateSound() {
        Notification n = this.sbn.getNotification();
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.leanback")) {
            return null;
        }
        Uri sound = this.mChannel.getSound();
        if (this.mPreChannelsNotification) {
            boolean z = true;
            if ((n.defaults & 1) == 0) {
                z = false;
            }
            if (z) {
                sound = System.DEFAULT_NOTIFICATION_URI;
            } else {
                sound = n.sound;
            }
        }
        return sound;
    }

    private Light calculateLights() {
        int channelLightColor;
        Light light;
        int defaultLightColor = this.mContext.getResources().getColor(17170533);
        int defaultLightOn = this.mContext.getResources().getInteger(17694770);
        int defaultLightOff = this.mContext.getResources().getInteger(17694769);
        if (getChannel().getLightColor() != 0) {
            channelLightColor = getChannel().getLightColor();
        } else {
            channelLightColor = defaultLightColor;
        }
        if (getChannel().shouldShowLights()) {
            light = new Light(channelLightColor, defaultLightOn, defaultLightOff);
        } else {
            light = null;
        }
        if (!this.mPreChannelsNotification || (getChannel().getUserLockedFields() & 8) != 0) {
            return light;
        }
        Notification notification = this.sbn.getNotification();
        if ((notification.flags & 1) == 0) {
            return null;
        }
        light = new Light(notification.ledARGB, notification.ledOnMS, notification.ledOffMS);
        if ((notification.defaults & 4) != 0) {
            return new Light(defaultLightColor, defaultLightOn, defaultLightOff);
        }
        return light;
    }

    private long[] calculateVibration() {
        long[] defaultVibration = NotificationManagerService.getLongArray(this.mContext.getResources(), 17235999, 17, NotificationManagerService.DEFAULT_VIBRATE_PATTERN);
        long[] vibration = getChannel().shouldVibrate() ? getChannel().getVibrationPattern() == null ? defaultVibration : getChannel().getVibrationPattern() : null;
        if (!this.mPreChannelsNotification || (getChannel().getUserLockedFields() & 16) != 0) {
            return vibration;
        }
        Notification notification = this.sbn.getNotification();
        if ((notification.defaults & 2) != 0) {
            return defaultVibration;
        }
        return notification.vibrate;
    }

    private AudioAttributes calculateAttributes() {
        Notification n = this.sbn.getNotification();
        AudioAttributes attributes = getChannel().getAudioAttributes();
        if (attributes == null) {
            attributes = Notification.AUDIO_ATTRIBUTES_DEFAULT;
        }
        if (!this.mPreChannelsNotification || (getChannel().getUserLockedFields() & 32) != 0) {
            return attributes;
        }
        if (n.audioAttributes != null) {
            return n.audioAttributes;
        }
        if (n.audioStreamType >= 0 && n.audioStreamType < AudioSystem.getNumStreamTypes()) {
            return new Builder().setInternalLegacyStreamType(n.audioStreamType).build();
        }
        if (n.audioStreamType == -1) {
            return attributes;
        }
        Log.w(TAG, String.format("Invalid stream type: %d", new Object[]{Integer.valueOf(n.audioStreamType)}));
        return attributes;
    }

    private int calculateImportance() {
        Notification n = this.sbn.getNotification();
        int importance = getChannel().getImportance();
        int requestedImportance = 3;
        if ((n.flags & 128) != 0) {
            n.priority = 2;
        }
        n.priority = NotificationManagerService.clamp(n.priority, -2, 2);
        switch (n.priority) {
            case -2:
                requestedImportance = 1;
                break;
            case -1:
                requestedImportance = 2;
                break;
            case 0:
                requestedImportance = 3;
                break;
            case 1:
            case 2:
                requestedImportance = 4;
                break;
        }
        this.stats.requestedImportance = requestedImportance;
        SingleNotificationStats singleNotificationStats = this.stats;
        boolean z = (this.mSound == null && this.mVibration == null) ? false : true;
        singleNotificationStats.isNoisy = z;
        Boolean isDefaultChannel = Boolean.valueOf("miscellaneous".equals(getChannel().getId()));
        if (this.mPreChannelsNotification && (importance == JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE || isDefaultChannel.booleanValue() || (getChannel().getUserLockedFields() & 4) == 0)) {
            if (!this.stats.isNoisy && requestedImportance > 2) {
                requestedImportance = 2;
            }
            if (this.stats.isNoisy && requestedImportance < 3) {
                requestedImportance = 3;
            }
            if (n.fullScreenIntent != null) {
                requestedImportance = 4;
            }
            importance = requestedImportance;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Use default channel, The importance is:");
            stringBuilder.append(importance);
            Log.w(str, stringBuilder.toString());
        }
        this.stats.naturalImportance = importance;
        return importance;
    }

    public void copyRankingInformation(NotificationRecord previous) {
        this.mContactAffinity = previous.mContactAffinity;
        this.mRecentlyIntrusive = previous.mRecentlyIntrusive;
        this.mPackagePriority = previous.mPackagePriority;
        this.mPackageVisibility = previous.mPackageVisibility;
        this.mIntercept = previous.mIntercept;
        this.mHidden = previous.mHidden;
        this.mRankingTimeMs = calculateRankingTimeMs(previous.getRankingTimeMs());
        this.mCreationTimeMs = previous.mCreationTimeMs;
        this.mVisibleSinceMs = previous.mVisibleSinceMs;
        this.mPushLogPowerTimeMs = previous.mPushLogPowerTimeMs;
        if (previous.sbn.getOverrideGroupKey() != null && !this.sbn.isAppGroup() && needOverrideGroupKey(previous)) {
            this.sbn.setOverrideGroupKey(previous.sbn.getOverrideGroupKey());
        }
    }

    private boolean needOverrideGroupKey(NotificationRecord previous) {
        Bundle newBundle = this.sbn.getNotification().extras;
        Bundle preBundle = previous.sbn.getNotification().extras;
        if (newBundle == null || !newBundle.containsKey("hw_btw") || preBundle == null || !preBundle.containsKey("hw_btw") || newBundle.getBoolean("hw_btw") == preBundle.getBoolean("hw_btw")) {
            return true;
        }
        return false;
    }

    public Notification getNotification() {
        return this.sbn.getNotification();
    }

    public int getFlags() {
        return this.sbn.getNotification().flags;
    }

    public UserHandle getUser() {
        return this.sbn.getUser();
    }

    public String getKey() {
        return this.sbn.getKey();
    }

    public int getUserId() {
        return this.sbn.getUserId();
    }

    public int getUid() {
        return this.sbn.getUid();
    }

    void dump(ProtoOutputStream proto, long fieldId, boolean redact, int state) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, this.sbn.getKey());
        proto.write(1159641169922L, state);
        if (getChannel() != null) {
            proto.write(1138166333444L, getChannel().getId());
        }
        boolean z = false;
        proto.write(1133871366152L, getLight() != null);
        if (getVibration() != null) {
            z = true;
        }
        proto.write(1133871366151L, z);
        proto.write(1120986464259L, this.sbn.getNotification().flags);
        proto.write(1138166333449L, getGroupKey());
        proto.write(1172526071818L, getImportance());
        if (getSound() != null) {
            proto.write(1138166333445L, getSound().toString());
        }
        if (getAudioAttributes() != null) {
            getAudioAttributes().writeToProto(proto, 1146756268038L);
        }
        proto.end(token);
    }

    String formatRemoteViews(RemoteViews rv) {
        if (rv == null) {
            return "null";
        }
        return String.format("%s/0x%08x (%d bytes): %s", new Object[]{rv.getPackage(), Integer.valueOf(rv.getLayoutId()), Integer.valueOf(rv.estimateMemoryUsage()), rv.toString()});
    }

    void dump(PrintWriter pw, String prefix, Context baseContext, boolean redact) {
        StringBuilder stringBuilder;
        int i;
        StringBuilder stringBuilder2;
        PrintWriter printWriter = pw;
        String str = prefix;
        Notification notification = this.sbn.getNotification();
        Icon icon = notification.getSmallIcon();
        String iconStr = String.valueOf(icon);
        int i2 = 2;
        if (icon == null || icon.getType() != 2) {
            Context context = baseContext;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(iconStr);
            stringBuilder.append(" / ");
            stringBuilder.append(idDebugString(baseContext, icon.getResPackage(), icon.getResId()));
            iconStr = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(this);
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("  ");
        str = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("uid=");
        stringBuilder.append(this.sbn.getUid());
        stringBuilder.append(" userId=");
        stringBuilder.append(this.sbn.getUserId());
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("icon=");
        stringBuilder.append(iconStr);
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("flags=0x");
        stringBuilder.append(Integer.toHexString(notification.flags));
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("pri=");
        stringBuilder.append(notification.priority);
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("key=");
        stringBuilder.append(this.sbn.getKey());
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("seen=");
        stringBuilder.append(this.mStats.hasSeen());
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("groupKey=");
        stringBuilder.append(getGroupKey());
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("fullscreenIntent=");
        stringBuilder.append(notification.fullScreenIntent);
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("contentIntent=");
        stringBuilder.append(notification.contentIntent);
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("deleteIntent=");
        stringBuilder.append(notification.deleteIntent);
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("contentView=");
        stringBuilder.append(formatRemoteViews(notification.contentView));
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("bigContentView=");
        stringBuilder.append(formatRemoteViews(notification.bigContentView));
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("headsUpContentView=");
        stringBuilder.append(formatRemoteViews(notification.headsUpContentView));
        printWriter.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        int i3 = 1;
        stringBuilder.append(String.format("color=0x%08x", new Object[]{Integer.valueOf(notification.color)}));
        printWriter.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("timeout=");
        stringBuilder.append(TimeUtils.formatForLogging(notification.getTimeoutAfter()));
        printWriter.println(stringBuilder.toString());
        if (notification.actions != null && notification.actions.length > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append("actions={");
            printWriter.println(stringBuilder.toString());
            int N = notification.actions.length;
            i = 0;
            while (i < N) {
                Action action = notification.actions[i];
                if (action != null) {
                    String str2 = "%s    [%d] \"%s\" -> %s";
                    Object[] objArr = new Object[4];
                    objArr[0] = str;
                    objArr[i3] = Integer.valueOf(i);
                    objArr[2] = action.title;
                    objArr[3] = action.actionIntent == null ? "null" : action.actionIntent.toString();
                    printWriter.println(String.format(str2, objArr));
                }
                i++;
                i3 = 1;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("  }");
            printWriter.println(stringBuilder3.toString());
        }
        if (notification.extras != null && notification.extras.size() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append("extras={");
            printWriter.println(stringBuilder.toString());
            for (String key : notification.extras.keySet()) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(str);
                stringBuilder4.append("    ");
                stringBuilder4.append(key);
                stringBuilder4.append("=");
                printWriter.print(stringBuilder4.toString());
                Object val = notification.extras.get(key);
                if (val == null) {
                    printWriter.println("null");
                } else {
                    printWriter.print(val.getClass().getSimpleName());
                    if (!(redact && ((val instanceof CharSequence) || (val instanceof String)))) {
                        if (val instanceof Bitmap) {
                            Object[] objArr2 = new Object[i2];
                            objArr2[0] = Integer.valueOf(((Bitmap) val).getWidth());
                            objArr2[1] = Integer.valueOf(((Bitmap) val).getHeight());
                            printWriter.print(String.format(" (%dx%d)", objArr2));
                        } else if (val.getClass().isArray()) {
                            i = Array.getLength(val);
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append(" (");
                            stringBuilder5.append(i);
                            stringBuilder5.append(")");
                            printWriter.print(stringBuilder5.toString());
                            if (!redact) {
                                for (int j = 0; j < i; j++) {
                                    pw.println();
                                    printWriter.print(String.format("%s      [%d] %s", new Object[]{str, Integer.valueOf(j), String.valueOf(Array.get(val, j))}));
                                }
                            }
                        } else {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(" (");
                            stringBuilder2.append(String.valueOf(val));
                            stringBuilder2.append(")");
                            printWriter.print(stringBuilder2.toString());
                        }
                    }
                    pw.println();
                }
                i2 = 2;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append("}");
            printWriter.println(stringBuilder2.toString());
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("stats=");
        stringBuilder2.append(this.stats.toString());
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mContactAffinity=");
        stringBuilder2.append(this.mContactAffinity);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mRecentlyIntrusive=");
        stringBuilder2.append(this.mRecentlyIntrusive);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mPackagePriority=");
        stringBuilder2.append(this.mPackagePriority);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mPackageVisibility=");
        stringBuilder2.append(this.mPackageVisibility);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mUserImportance=");
        stringBuilder2.append(Ranking.importanceToString(this.mUserImportance));
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mImportance=");
        stringBuilder2.append(Ranking.importanceToString(this.mImportance));
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mImportanceExplanation=");
        stringBuilder2.append(this.mImportanceExplanation);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mIsAppImportanceLocked=");
        stringBuilder2.append(this.mIsAppImportanceLocked);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mIntercept=");
        stringBuilder2.append(this.mIntercept);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mHidden==");
        stringBuilder2.append(this.mHidden);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mGlobalSortKey=");
        stringBuilder2.append(this.mGlobalSortKey);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mRankingTimeMs=");
        stringBuilder2.append(this.mRankingTimeMs);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mCreationTimeMs=");
        stringBuilder2.append(this.mCreationTimeMs);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mVisibleSinceMs=");
        stringBuilder2.append(this.mVisibleSinceMs);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mUpdateTimeMs=");
        stringBuilder2.append(this.mUpdateTimeMs);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mSuppressedVisualEffects= ");
        stringBuilder2.append(this.mSuppressedVisualEffects);
        printWriter.println(stringBuilder2.toString());
        if (this.mPreChannelsNotification) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append(String.format("defaults=0x%08x flags=0x%08x", new Object[]{Integer.valueOf(notification.defaults), Integer.valueOf(notification.flags)}));
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append("n.sound=");
            stringBuilder2.append(notification.sound);
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append("n.audioStreamType=");
            stringBuilder2.append(notification.audioStreamType);
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append("n.audioAttributes=");
            stringBuilder2.append(notification.audioAttributes);
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append(String.format("  led=0x%08x onMs=%d offMs=%d", new Object[]{Integer.valueOf(notification.ledARGB), Integer.valueOf(notification.ledOnMS), Integer.valueOf(notification.ledOffMS)}));
            printWriter.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append("vibrate=");
            stringBuilder2.append(Arrays.toString(notification.vibrate));
            printWriter.println(stringBuilder2.toString());
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mSound= ");
        stringBuilder2.append(this.mSound);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mVibration= ");
        stringBuilder2.append(this.mVibration);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mAttributes= ");
        stringBuilder2.append(this.mAttributes);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mLight= ");
        stringBuilder2.append(this.mLight);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mShowBadge=");
        stringBuilder2.append(this.mShowBadge);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mColorized=");
        stringBuilder2.append(notification.isColorized());
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mIsInterruptive=");
        stringBuilder2.append(this.mIsInterruptive);
        printWriter.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("effectiveNotificationChannel=");
        stringBuilder2.append(getChannel());
        printWriter.println(stringBuilder2.toString());
        if (getPeopleOverride() != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append("overridePeople= ");
            stringBuilder2.append(TextUtils.join(",", getPeopleOverride()));
            printWriter.println(stringBuilder2.toString());
        }
        if (getSnoozeCriteria() != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str);
            stringBuilder2.append("snoozeCriteria=");
            stringBuilder2.append(TextUtils.join(",", getSnoozeCriteria()));
            printWriter.println(stringBuilder2.toString());
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("mAdjustments=");
        stringBuilder2.append(this.mAdjustments);
        printWriter.println(stringBuilder2.toString());
    }

    static String idDebugString(Context baseContext, String packageName, int id) {
        Context c;
        if (packageName != null) {
            try {
                c = baseContext.createPackageContext(packageName, null);
            } catch (NameNotFoundException e) {
                c = baseContext;
            }
        } else {
            c = baseContext;
        }
        try {
            return c.getResources().getResourceName(id);
        } catch (NotFoundException e2) {
            return "<name unknown>";
        }
    }

    public final String toString() {
        return String.format("NotificationRecord(0x%08x: pkg=%s user=%s id=%d tag=%s importance=%d key=%sappImportanceLocked=%s: %s)", new Object[]{Integer.valueOf(System.identityHashCode(this)), this.sbn.getPackageName(), this.sbn.getUser(), Integer.valueOf(this.sbn.getId()), this.sbn.getTag(), Integer.valueOf(this.mImportance), this.sbn.getKey(), Boolean.valueOf(this.mIsAppImportanceLocked), this.sbn.getNotification()});
    }

    public void addAdjustment(Adjustment adjustment) {
        synchronized (this.mAdjustments) {
            this.mAdjustments.add(adjustment);
        }
        applyAdjustments();
    }

    public void applyAdjustments() {
        synchronized (this.mAdjustments) {
            for (Adjustment adjustment : this.mAdjustments) {
                Bundle signals = adjustment.getSignals();
                if (signals.containsKey("key_people")) {
                    setPeopleOverride(adjustment.getSignals().getStringArrayList("key_people"));
                }
                if (signals.containsKey("key_snooze_criteria")) {
                    setSnoozeCriteria(adjustment.getSignals().getParcelableArrayList("key_snooze_criteria"));
                }
                if (signals.containsKey("key_group_key")) {
                    setOverrideGroupKey(adjustment.getSignals().getString("key_group_key"));
                }
                if (signals.containsKey("key_user_sentiment") && !this.mIsAppImportanceLocked && (getChannel().getUserLockedFields() & 4) == 0) {
                    setUserSentiment(adjustment.getSignals().getInt("key_user_sentiment", 0));
                }
            }
        }
    }

    public void setIsAppImportanceLocked(boolean isAppImportanceLocked) {
        this.mIsAppImportanceLocked = isAppImportanceLocked;
        calculateUserSentiment();
    }

    public void setContactAffinity(float contactAffinity) {
        this.mContactAffinity = contactAffinity;
        if (this.mImportance < 3 && this.mContactAffinity > 0.5f) {
            setImportance(3, getPeopleExplanation());
        }
    }

    public float getContactAffinity() {
        return this.mContactAffinity;
    }

    public void setRecentlyIntrusive(boolean recentlyIntrusive) {
        this.mRecentlyIntrusive = recentlyIntrusive;
        if (recentlyIntrusive) {
            this.mLastIntrusive = System.currentTimeMillis();
        }
    }

    public boolean isRecentlyIntrusive() {
        return this.mRecentlyIntrusive;
    }

    public long getLastIntrusive() {
        return this.mLastIntrusive;
    }

    public void setPackagePriority(int packagePriority) {
        this.mPackagePriority = packagePriority;
    }

    public int getPackagePriority() {
        return this.mPackagePriority;
    }

    public void setPackageVisibilityOverride(int packageVisibility) {
        this.mPackageVisibility = packageVisibility;
    }

    public int getPackageVisibilityOverride() {
        return this.mPackageVisibility;
    }

    public void setUserImportance(int importance) {
        this.mUserImportance = importance;
        applyUserImportance();
    }

    private String getUserExplanation() {
        if (this.mUserExplanation == null) {
            this.mUserExplanation = this.mContext.getResources().getString(17040217);
        }
        return this.mUserExplanation;
    }

    private String getPeopleExplanation() {
        if (this.mPeopleExplanation == null) {
            this.mPeopleExplanation = this.mContext.getResources().getString(17040216);
        }
        return this.mPeopleExplanation;
    }

    private void applyUserImportance() {
        if (this.mUserImportance != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
            this.mImportance = this.mUserImportance;
            this.mImportanceExplanation = getUserExplanation();
        }
    }

    public int getUserImportance() {
        return this.mUserImportance;
    }

    public void setImportance(int importance, CharSequence explanation) {
        if (importance != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
            this.mImportance = importance;
            this.mImportanceExplanation = explanation;
        }
        applyUserImportance();
    }

    public int getImportance() {
        return this.mImportance;
    }

    public CharSequence getImportanceExplanation() {
        return this.mImportanceExplanation;
    }

    public boolean setIntercepted(boolean intercept) {
        this.mIntercept = intercept;
        return this.mIntercept;
    }

    public boolean isIntercepted() {
        return this.mIntercept;
    }

    public void setHidden(boolean hidden) {
        this.mHidden = hidden;
    }

    public boolean isHidden() {
        return this.mHidden;
    }

    public void setSuppressedVisualEffects(int effects) {
        this.mSuppressedVisualEffects = effects;
    }

    public int getSuppressedVisualEffects() {
        return this.mSuppressedVisualEffects;
    }

    public boolean isCategory(String category) {
        return Objects.equals(getNotification().category, category);
    }

    public boolean isAudioAttributesUsage(int usage) {
        return this.mAttributes != null && this.mAttributes.getUsage() == usage;
    }

    public long getRankingTimeMs() {
        return this.mRankingTimeMs;
    }

    public int getFreshnessMs(long now) {
        return (int) (now - this.mUpdateTimeMs);
    }

    public int getLifespanMs(long now) {
        return (int) (now - this.mCreationTimeMs);
    }

    public int getExposureMs(long now) {
        return this.mVisibleSinceMs == 0 ? 0 : (int) (now - this.mVisibleSinceMs);
    }

    protected long getPushLogPowerTimeMs(long now) {
        return now - this.mPushLogPowerTimeMs;
    }

    protected void setPushLogPowerTimeMs(long timeMs) {
        this.mPushLogPowerTimeMs = timeMs;
    }

    public void setVisibility(boolean visible, int rank, int count) {
        long now = System.currentTimeMillis();
        this.mVisibleSinceMs = visible ? now : this.mVisibleSinceMs;
        this.stats.onVisibilityChanged(visible);
        MetricsLogger.action(getLogMaker(now).setCategory(128).setType(visible ? 1 : 2).addTaggedData(798, Integer.valueOf(rank)).addTaggedData(1395, Integer.valueOf(count)));
        if (visible) {
            setSeen();
            MetricsLogger.histogram(this.mContext, "note_freshness", getFreshnessMs(now));
        }
        EventLogTags.writeNotificationVisibility(getKey(), visible, getLifespanMs(now), getFreshnessMs(now), 0, rank);
    }

    private long calculateRankingTimeMs(long previousRankingTimeMs) {
        Notification n = getNotification();
        if (n.when != 0 && n.when <= this.sbn.getPostTime()) {
            return n.when;
        }
        if (previousRankingTimeMs > 0) {
            return previousRankingTimeMs;
        }
        return this.sbn.getPostTime();
    }

    public void setGlobalSortKey(String globalSortKey) {
        this.mGlobalSortKey = globalSortKey;
    }

    public String getGlobalSortKey() {
        return this.mGlobalSortKey;
    }

    public boolean isSeen() {
        return this.mStats.hasSeen();
    }

    public void setSeen() {
        this.mStats.setSeen();
        if (this.mTextChanged) {
            this.mIsInterruptive = true;
        }
    }

    public void setAuthoritativeRank(int authoritativeRank) {
        this.mAuthoritativeRank = authoritativeRank;
    }

    public int getAuthoritativeRank() {
        return this.mAuthoritativeRank;
    }

    public String getGroupKey() {
        return this.sbn.getGroupKey();
    }

    public void setOverrideGroupKey(String overrideGroupKey) {
        this.sbn.setOverrideGroupKey(overrideGroupKey);
        this.mGroupLogTag = null;
    }

    private String getGroupLogTag() {
        if (this.mGroupLogTag == null) {
            this.mGroupLogTag = shortenTag(this.sbn.getGroup());
        }
        return this.mGroupLogTag;
    }

    private String getChannelIdLogTag() {
        if (this.mChannelIdLogTag == null) {
            this.mChannelIdLogTag = shortenTag(this.mChannel.getId());
        }
        return this.mChannelIdLogTag;
    }

    private String shortenTag(String longTag) {
        if (longTag == null) {
            return null;
        }
        if (longTag.length() < 35) {
            return longTag;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(longTag.substring(0, 27));
        stringBuilder.append("-");
        stringBuilder.append(Integer.toHexString(longTag.hashCode()));
        return stringBuilder.toString();
    }

    public NotificationChannel getChannel() {
        return this.mChannel;
    }

    public boolean getIsAppImportanceLocked() {
        return this.mIsAppImportanceLocked;
    }

    protected void updateNotificationChannel(NotificationChannel channel) {
        if (channel != null) {
            this.mChannel = channel;
            calculateImportance();
            calculateUserSentiment();
        }
    }

    public void setShowBadge(boolean showBadge) {
        this.mShowBadge = showBadge;
    }

    public boolean canShowBadge() {
        return this.mShowBadge;
    }

    public Light getLight() {
        return this.mLight;
    }

    public Uri getSound() {
        return this.mSound;
    }

    public long[] getVibration() {
        return this.mVibration;
    }

    public AudioAttributes getAudioAttributes() {
        return this.mAttributes;
    }

    public ArrayList<String> getPeopleOverride() {
        return this.mPeopleOverride;
    }

    public void setInterruptive(boolean interruptive) {
        this.mIsInterruptive = interruptive;
    }

    public void setTextChanged(boolean textChanged) {
        this.mTextChanged = textChanged;
    }

    public void setRecordedInterruption(boolean recorded) {
        this.mRecordedInterruption = recorded;
    }

    public boolean hasRecordedInterruption() {
        return this.mRecordedInterruption;
    }

    public boolean isInterruptive() {
        return this.mIsInterruptive;
    }

    protected void setPeopleOverride(ArrayList<String> people) {
        this.mPeopleOverride = people;
    }

    public ArrayList<SnoozeCriterion> getSnoozeCriteria() {
        return this.mSnoozeCriteria;
    }

    protected void setSnoozeCriteria(ArrayList<SnoozeCriterion> snoozeCriteria) {
        this.mSnoozeCriteria = snoozeCriteria;
    }

    private void calculateUserSentiment() {
        if ((getChannel().getUserLockedFields() & 4) != 0 || this.mIsAppImportanceLocked) {
            this.mUserSentiment = 1;
        }
    }

    private void setUserSentiment(int userSentiment) {
        this.mUserSentiment = userSentiment;
    }

    public int getUserSentiment() {
        return this.mUserSentiment;
    }

    public NotificationStats getStats() {
        return this.mStats;
    }

    public void recordExpanded() {
        this.mStats.setExpanded();
    }

    public void recordDirectReplied() {
        this.mStats.setDirectReplied();
    }

    public void recordDismissalSurface(int surface) {
        this.mStats.setDismissalSurface(surface);
    }

    public void recordSnoozed() {
        this.mStats.setSnoozed();
    }

    public void recordViewedSettings() {
        this.mStats.setViewedSettings();
    }

    public void setNumSmartRepliesAdded(int noReplies) {
        this.mNumberOfSmartRepliesAdded = noReplies;
    }

    public int getNumSmartRepliesAdded() {
        return this.mNumberOfSmartRepliesAdded;
    }

    public boolean hasSeenSmartReplies() {
        return this.mHasSeenSmartReplies;
    }

    public void setSeenSmartReplies(boolean hasSeenSmartReplies) {
        this.mHasSeenSmartReplies = hasSeenSmartReplies;
    }

    public ArraySet<Uri> getGrantableUris() {
        return this.mGrantableUris;
    }

    protected void calculateGrantableUris() {
        Notification notification = getNotification();
        notification.visitUris(new -$$Lambda$NotificationRecord$XgkrZGcjOHPHem34oE9qLGy3siA(this));
        if (notification.getChannelId() != null) {
            NotificationChannel channel = getChannel();
            if (channel != null) {
                visitGrantableUri(channel.getSound(), (channel.getUserLockedFields() & 32) != 0);
            }
        }
    }

    private void visitGrantableUri(Uri uri, boolean userOverriddenUri) {
        if (uri != null && "content".equals(uri.getScheme())) {
            int sourceUid = this.sbn.getUid();
            if (sourceUid != 1000) {
                long ident = Binder.clearCallingIdentity();
                try {
                    this.mAm.checkGrantUriPermission(sourceUid, null, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)));
                    if (this.mGrantableUris == null) {
                        this.mGrantableUris = new ArraySet();
                    }
                    this.mGrantableUris.add(uri);
                } catch (RemoteException e) {
                } catch (SecurityException e2) {
                    if (!userOverriddenUri) {
                        if (this.mTargetSdkVersion < 28) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Ignoring ");
                            stringBuilder.append(uri);
                            stringBuilder.append(" from ");
                            stringBuilder.append(sourceUid);
                            stringBuilder.append(": ");
                            stringBuilder.append(e2.getMessage());
                            Log.w(str, stringBuilder.toString());
                        } else {
                            throw e2;
                        }
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(ident);
                }
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public LogMaker getLogMaker(long now) {
        if (this.mLogMaker == null) {
            this.mLogMaker = new LogMaker(0).setPackageName(this.sbn.getPackageName()).addTaggedData(796, Integer.valueOf(this.sbn.getId())).addTaggedData(797, this.sbn.getTag()).addTaggedData(857, getChannelIdLogTag());
        }
        return this.mLogMaker.clearCategory().clearType().clearSubtype().clearTaggedData(798).addTaggedData(858, Integer.valueOf(this.mImportance)).addTaggedData(946, getGroupLogTag()).addTaggedData(947, Integer.valueOf(this.sbn.getNotification().isGroupSummary())).addTaggedData(793, Integer.valueOf(getLifespanMs(now))).addTaggedData(795, Integer.valueOf(getFreshnessMs(now))).addTaggedData(794, Integer.valueOf(getExposureMs(now)));
    }

    public LogMaker getLogMaker() {
        return getLogMaker(System.currentTimeMillis());
    }
}
