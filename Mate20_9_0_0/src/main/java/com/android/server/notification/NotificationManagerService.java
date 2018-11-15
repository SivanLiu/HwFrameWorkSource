package com.android.server.notification;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AutomaticZenRule;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.INotificationManager.Stub;
import android.app.ITransientNotification;
import android.app.Notification;
import android.app.Notification.TvExtender;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.backup.BackupManager;
import android.app.usage.UsageStatsManagerInternal;
import android.common.HwFrameworkFactory;
import android.companion.ICompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.hdm.HwDeviceManager;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.media.IRingtonePlayer;
import android.metrics.LogMaker;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.rms.HwSysResource;
import android.service.notification.Adjustment;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.INotificationListener;
import android.service.notification.IStatusBarNotificationHolder;
import android.service.notification.NotificationRankingUpdate;
import android.service.notification.NotificationStats;
import android.service.notification.NotifyingApp;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ZenRule;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SplitNotificationUtils;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import android.view.IWindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.AbsLocationManagerService;
import com.android.server.DeviceIdleController.LocalService;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.audio.AudioService;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.notification.ManagedServices.Config;
import com.android.server.notification.ManagedServices.ManagedServiceInfo;
import com.android.server.notification.ManagedServices.UserProfiles;
import com.android.server.notification.RankingHelper.NotificationSysMgrCfg;
import com.android.server.notification.ZenModeHelper.Callback;
import com.android.server.os.HwBootFail;
import com.android.server.pm.PackageManagerService;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.pgmng.log.LogPower;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class NotificationManagerService extends AbsNotificationManager {
    private static final String ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE = "huawei.intent.action.HWSYSTEMMANAGER_CHANGE_POWERMODE";
    private static final String ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE = "huawei.intent.action.HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE";
    private static final String ACTION_NOTIFICATION_TIMEOUT;
    private static final String ATTR_VERSION = "version";
    private static final int CLOSE_SAVE_POWER = 0;
    static final boolean DBG = Log.isLoggable(TAG, 3);
    private static final int DB_VERSION = 1;
    static final boolean DEBUG_INTERRUPTIVENESS = SystemProperties.getBoolean("debug.notification.interruptiveness", false);
    static final float DEFAULT_MAX_NOTIFICATION_ENQUEUE_RATE = 5.0f;
    static final int DEFAULT_STREAM_TYPE = 5;
    static final long[] DEFAULT_VIBRATE_PATTERN = new long[]{0, 250, 250, 250};
    private static final long DELAY_FOR_ASSISTANT_TIME = 100;
    private static final boolean DISABLE_MULTIWIN = SystemProperties.getBoolean("ro.huawei.disable_multiwindow", false);
    static final boolean ENABLE_BLOCKED_TOASTS = true;
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = SystemProperties.getBoolean("debug.child_notifs", true);
    private static final int EVENTLOG_ENQUEUE_STATUS_IGNORED = 2;
    private static final int EVENTLOG_ENQUEUE_STATUS_NEW = 0;
    private static final int EVENTLOG_ENQUEUE_STATUS_UPDATE = 1;
    public static final List<String> EXPANDEDNTF_PKGS = new ArrayList();
    private static final String EXTRA_KEY = "key";
    static final int FINISH_TOKEN_TIMEOUT = 11000;
    private static final boolean HWFLOW;
    private static final String HWSYSTEMMANAGER_PKG = "com.huawei.systemmanager";
    private static final int IS_TOP_FULL_SCREEN_TOKEN = 206;
    static final int LONG_DELAY = 3500;
    static final int MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS = 3000;
    static final float MATCHES_CALL_FILTER_TIMEOUT_AFFINITY = 1.0f;
    static final int MAX_PACKAGE_NOTIFICATIONS = 50;
    static final int MESSAGE_DURATION_REACHED = 2;
    static final int MESSAGE_FINISH_TOKEN_TIMEOUT = 7;
    static final int MESSAGE_LISTENER_HINTS_CHANGED = 5;
    static final int MESSAGE_LISTENER_NOTIFICATION_FILTER_CHANGED = 6;
    private static final int MESSAGE_RANKING_SORT = 1001;
    private static final int MESSAGE_RECONSIDER_RANKING = 1000;
    static final int MESSAGE_SAVE_POLICY_FILE = 3;
    static final int MESSAGE_SEND_RANKING_UPDATE = 4;
    private static final long MIN_PACKAGE_OVERRATE_LOG_INTERVAL = 5000;
    private static final int MY_PID = Process.myPid();
    private static final int MY_UID = Process.myUid();
    private static final String NOTIFICATION_CENTER_ORIGIN_PKG = "hw_origin_sender_package_name";
    private static final String NOTIFICATION_CENTER_PKG = "com.huawei.android.pushagent";
    private static final long NOTIFICATION_UPDATE_REPORT_INTERVAL = 15000;
    private static final int N_BTW = 0;
    private static final int N_MUSIC = 2;
    private static final int N_NORMAL = 1;
    private static final int OPEN_SAVE_POWER = 3;
    private static final String PERMISSION = "com.huawei.android.launcher.permission.CHANGE_POWERMODE";
    private static final String POWER_MODE = "power_mode";
    private static final String POWER_SAVER_NOTIFICATION_WHITELIST = "super_power_save_notification_whitelist";
    private static final int REQUEST_CODE_TIMEOUT = 1;
    private static final String SCHEME_TIMEOUT = "timeout";
    static final int SHORT_DELAY = 2000;
    private static final String SHUTDOWN_LIMIT_POWERMODE = "shutdomn_limit_powermode";
    static final long SNOOZE_UNTIL_UNSPECIFIED = -1;
    static final String TAG = "NotificationService";
    private static final String TAG_NOTIFICATION_POLICY = "notification-policy";
    private static final String TELECOM_PKG = "com.android.server.telecom";
    static final int VIBRATE_PATTERN_MAXLEN = 17;
    private static final IBinder WHITELIST_TOKEN = new Binder();
    private static final boolean mIsChina = "CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
    private AccessibilityManager mAccessibilityManager;
    private ActivityManager mActivityManager;
    private AlarmManager mAlarmManager;
    private Predicate<String> mAllowedManagedServicePackages;
    private IActivityManager mAm;
    private AppOpsManager mAppOps;
    private UsageStatsManagerInternal mAppUsageStats;
    private Archive mArchive;
    private NotificationAssistants mAssistants;
    Light mAttentionLight;
    AudioManager mAudioManager;
    AudioManagerInternal mAudioManagerInternal;
    @GuardedBy("mNotificationLock")
    final ArrayMap<Integer, ArrayMap<String, String>> mAutobundledSummaries = new ArrayMap();
    private int mCallState;
    private ICompanionDeviceManager mCompanionManager;
    private ConditionProviders mConditionProviders;
    private IDeviceIdleController mDeviceIdleController;
    private boolean mDisableNotificationEffects;
    private DevicePolicyManagerInternal mDpm;
    private List<ComponentName> mEffectsSuppressors = new ArrayList();
    @GuardedBy("mNotificationLock")
    final ArrayList<NotificationRecord> mEnqueuedNotifications = new ArrayList();
    private long[] mFallbackVibrationPattern;
    final IBinder mForegroundToken = new Binder();
    protected boolean mGameDndStatus = false;
    private GroupHelper mGroupHelper;
    private WorkerHandler mHandler;
    protected boolean mInCall = false;
    private final Binder mInCallBinder = new Binder();
    private AudioAttributes mInCallNotificationAudioAttributes;
    private Uri mInCallNotificationUri;
    private float mInCallNotificationVolume;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Context context2 = context;
            Intent intent2 = intent;
            String action = intent.getAction();
            StringBuilder stringBuilder;
            int userHandle;
            if (action.equals("android.intent.action.SCREEN_ON")) {
                NotificationManagerService.this.mScreenOn = true;
                NotificationManagerService.this.mGameDndStatus = NotificationManagerService.this.isGameRunningForeground();
                stringBuilder = new StringBuilder();
                stringBuilder.append("mIntentReceiver-ACTION_SCREEN_ON, mGameDndStatus = ");
                stringBuilder.append(NotificationManagerService.this.mGameDndStatus);
                Flog.i(400, stringBuilder.toString());
                NotificationManagerService.this.updateNotificationPulse();
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                NotificationManagerService.this.mScreenOn = false;
                NotificationManagerService.this.mGameDndStatus = false;
                Flog.i(400, "mIntentReceiver-ACTION_SCREEN_OFF");
                NotificationManagerService.this.updateNotificationPulse();
            } else if (action.equals("android.intent.action.PHONE_STATE")) {
                NotificationManagerService.this.mInCall = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(intent2.getStringExtra(AudioService.CONNECT_INTENT_KEY_STATE));
                Flog.i(400, "mIntentReceiver-ACTION_PHONE_STATE_CHANGED");
                NotificationManagerService.this.updateNotificationPulse();
            } else if (action.equals("android.intent.action.USER_STOPPED")) {
                Flog.i(400, "mIntentReceiver-ACTION_USER_STOPPED");
                userHandle = intent2.getIntExtra("android.intent.extra.user_handle", -1);
                if (userHandle >= 0) {
                    NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, null, 0, 0, true, userHandle, 6, null);
                }
            } else if (action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE")) {
                userHandle = intent2.getIntExtra("android.intent.extra.user_handle", -1);
                if (userHandle >= 0) {
                    NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, null, null, 0, 0, true, userHandle, 15, null);
                }
            } else if (action.equals("android.intent.action.USER_PRESENT")) {
                if (NotificationManagerService.this.mScreenOn) {
                    NotificationManagerService.this.mNotificationLight.turnOff();
                    StatusBarManagerInternal statusBarManagerInternal = NotificationManagerService.this.mStatusBar;
                    NotificationManagerService.this.mGameDndStatus = NotificationManagerService.this.isGameRunningForeground();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("turn off notificationLight due to Receiver-ACTION_USER_PRESENT, mGameDndStatus = ");
                    stringBuilder.append(NotificationManagerService.this.mGameDndStatus);
                    Flog.i(1100, stringBuilder.toString());
                    NotificationManagerService.this.updateLight(false, 0, 0);
                }
            } else if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                if (!NotificationManagerService.this.mScreenOn || NotificationManagerService.this.mGameDndStatus) {
                    NotificationManagerService.this.mNotificationLight.turnOff();
                    NotificationManagerService.this.updateNotificationPulse();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("turn off notificationLight due to Receiver-ACTION_POWER_DISCONNECTED,mScreenOn= ");
                    stringBuilder.append(NotificationManagerService.this.mScreenOn);
                    stringBuilder.append(",mGameDndStatus= ");
                    stringBuilder.append(NotificationManagerService.this.mGameDndStatus);
                    Flog.i(1100, stringBuilder.toString());
                    NotificationManagerService.this.updateLight(false, 0, 0);
                }
            } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                userHandle = intent2.getIntExtra("android.intent.extra.user_handle", -10000);
                Flog.i(400, "mIntentReceiver-ACTION_USER_SWITCHED");
                NotificationManagerService.this.mSettingsObserver.update(null);
                NotificationManagerService.this.mUserProfiles.updateCache(context2);
                NotificationManagerService.this.mConditionProviders.onUserSwitched(userHandle);
                NotificationManagerService.this.mListeners.onUserSwitched(userHandle);
                NotificationManagerService.this.mAssistants.onUserSwitched(userHandle);
                NotificationManagerService.this.mZenModeHelper.onUserSwitched(userHandle);
                NotificationManagerService.this.handleUserSwitchEvents(userHandle);
                NotificationManagerService.this.stopPlaySound();
            } else if (action.equals("android.intent.action.USER_ADDED")) {
                userHandle = intent2.getIntExtra("android.intent.extra.user_handle", -10000);
                if (userHandle != -10000) {
                    NotificationManagerService.this.mUserProfiles.updateCache(context2);
                    if (!NotificationManagerService.this.mUserProfiles.isManagedProfile(userHandle)) {
                        NotificationManagerService.this.readDefaultApprovedServices(userHandle);
                    }
                }
                Flog.i(400, "mIntentReceiver-ACTION_USER_ADDED");
            } else if (action.equals("android.intent.action.USER_REMOVED")) {
                userHandle = intent2.getIntExtra("android.intent.extra.user_handle", -10000);
                NotificationManagerService.this.mUserProfiles.updateCache(context2);
                NotificationManagerService.this.mZenModeHelper.onUserRemoved(userHandle);
                NotificationManagerService.this.mRankingHelper.onUserRemoved(userHandle);
                NotificationManagerService.this.mListeners.onUserRemoved(userHandle);
                NotificationManagerService.this.mConditionProviders.onUserRemoved(userHandle);
                NotificationManagerService.this.mAssistants.onUserRemoved(userHandle);
                NotificationManagerService.this.savePolicyFile();
            } else if (action.equals("android.intent.action.USER_UNLOCKED")) {
                userHandle = intent2.getIntExtra("android.intent.extra.user_handle", -10000);
                NotificationManagerService.this.mConditionProviders.onUserUnlocked(userHandle);
                NotificationManagerService.this.mListeners.onUserUnlocked(userHandle);
                NotificationManagerService.this.mAssistants.onUserUnlocked(userHandle);
                NotificationManagerService.this.mZenModeHelper.onUserUnlocked(userHandle);
            }
        }
    };
    private final NotificationManagerInternal mInternalService = new NotificationManagerInternal() {
        public NotificationChannel getNotificationChannel(String pkg, int uid, String channelId) {
            return NotificationManagerService.this.mRankingHelper.getNotificationChannel(pkg, uid, channelId, false);
        }

        public void enqueueNotification(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, Notification notification, int userId) {
            NotificationManagerService.this.enqueueNotificationInternal(pkg, opPkg, callingUid, callingPid, tag, id, notification, userId);
        }

        public void removeForegroundServiceFlagFromNotification(final String pkg, final int notificationId, final int userId) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mHandler.post(new Runnable() {
                public void run() {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        AnonymousClass12.this.removeForegroundServiceFlagByListLocked(NotificationManagerService.this.mEnqueuedNotifications, pkg, notificationId, userId);
                        AnonymousClass12.this.removeForegroundServiceFlagByListLocked(NotificationManagerService.this.mNotificationList, pkg, notificationId, userId);
                    }
                }
            });
        }

        @GuardedBy("mNotificationLock")
        private void removeForegroundServiceFlagByListLocked(ArrayList<NotificationRecord> notificationList, String pkg, int notificationId, int userId) {
            NotificationRecord r = NotificationManagerService.this.findNotificationByListLocked(notificationList, pkg, null, notificationId, userId);
            if (r != null) {
                r.sbn.getNotification().flags = r.mOriginalFlags & -65;
                NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                NotificationManagerService.this.mListeners.notifyPostedLocked(r, r);
            }
        }
    };
    private int mInterruptionFilter = 0;
    private boolean mIsTelevision;
    private long mLastOverRateLogTime;
    ArrayList<String> mLights = new ArrayList();
    private int mListenerHints;
    private NotificationListeners mListeners;
    private final SparseArray<ArraySet<ManagedServiceInfo>> mListenersDisablingEffects = new SparseArray();
    protected final BroadcastReceiver mLocaleChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                SystemNotificationChannels.createAll(context);
                NotificationManagerService.this.mZenModeHelper.updateDefaultZenRules();
                NotificationManagerService.this.mRankingHelper.onLocaleChanged(context, ActivityManager.getCurrentUser());
            }
        }
    };
    private float mMaxPackageEnqueueRate = 5.0f;
    private MetricsLogger mMetricsLogger;
    @VisibleForTesting
    final NotificationDelegate mNotificationDelegate = new NotificationDelegate() {
        public void onSetDisabled(int status) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.mDisableNotificationEffects = (262144 & status) != 0;
                if (NotificationManagerService.this.disableNotificationEffects(null) != null) {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        IRingtonePlayer player = NotificationManagerService.this.mAudioManager.getRingtonePlayer();
                        if (player != null) {
                            player.stopAsync();
                        }
                    } catch (RemoteException e) {
                    } catch (Throwable th) {
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                    identity = Binder.clearCallingIdentity();
                    NotificationManagerService.this.mVibrator.cancel();
                }
            }
        }

        public void onClearAll(int callingUid, int callingPid, int userId) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.cancelAllLocked(callingUid, callingPid, userId, 3, null, true);
            }
        }

        public void onNotificationClick(int callingUid, int callingPid, String key, NotificationVisibility nv) {
            String str = key;
            NotificationVisibility notificationVisibility = nv;
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                Flog.i(400, "onNotificationClick called");
                NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(str);
                if (r == null) {
                    String str2 = NotificationManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No notification with key: ");
                    stringBuilder.append(str);
                    Log.w(str2, stringBuilder.toString());
                    return;
                }
                long now = System.currentTimeMillis();
                MetricsLogger.action(r.getLogMaker(now).setCategory(128).setType(4).addTaggedData(798, Integer.valueOf(notificationVisibility.rank)).addTaggedData(1395, Integer.valueOf(notificationVisibility.count)));
                EventLogTags.writeNotificationClicked(str, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now), notificationVisibility.rank, notificationVisibility.count);
                StatusBarNotification sbn = r.sbn;
                NotificationManagerService.this.cancelNotification(callingUid, callingPid, sbn.getPackageName(), sbn.getTag(), sbn.getId(), 16, 64, false, r.getUserId(), 1, notificationVisibility.rank, notificationVisibility.count, null);
                nv.recycle();
                NotificationManagerService.this.reportUserInteraction(r);
            }
        }

        public void onNotificationActionClick(int callingUid, int callingPid, String key, int actionIndex, NotificationVisibility nv) {
            Throwable th;
            String str = key;
            NotificationVisibility notificationVisibility = nv;
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                int i;
                try {
                    NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(str);
                    if (r == null) {
                        String str2 = NotificationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("No notification with key: ");
                        stringBuilder.append(str);
                        Log.w(str2, stringBuilder.toString());
                        return;
                    }
                    long now = System.currentTimeMillis();
                    i = actionIndex;
                    MetricsLogger.action(r.getLogMaker(now).setCategory(NetworkConstants.ICMPV6_ECHO_REPLY_TYPE).setType(4).setSubtype(i).addTaggedData(798, Integer.valueOf(notificationVisibility.rank)).addTaggedData(1395, Integer.valueOf(notificationVisibility.count)));
                    EventLogTags.writeNotificationActionClicked(str, i, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now), notificationVisibility.rank, notificationVisibility.count);
                    nv.recycle();
                    NotificationManagerService.this.reportUserInteraction(r);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }

        /* JADX WARNING: Missing block: B:13:0x0020, code:
            r1.this$0.cancelNotification(r21, r22, r23, r24, r25, 0, 66, true, r26, 2, r2.rank, r2.count, null);
            r29.recycle();
     */
        /* JADX WARNING: Missing block: B:14:0x0044, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onNotificationClear(int callingUid, int callingPid, String pkg, String tag, int id, int userId, String key, int dismissalSurface, NotificationVisibility nv) {
            Throwable th;
            NotificationVisibility notificationVisibility = nv;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                int i;
                try {
                    try {
                        NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(key);
                        if (r != null) {
                            try {
                                r.recordDismissalSurface(dismissalSurface);
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                        i = dismissalSurface;
                    } catch (Throwable th3) {
                        th = th3;
                        i = dismissalSurface;
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    String str = key;
                    i = dismissalSurface;
                    throw th;
                }
            }
        }

        public void onPanelRevealed(boolean clearEffects, int items) {
            MetricsLogger.visible(NotificationManagerService.this.getContext(), 127);
            MetricsLogger.histogram(NotificationManagerService.this.getContext(), "note_load", items);
            EventLogTags.writeNotificationPanelRevealed(items);
            if (clearEffects) {
                clearEffects();
            }
        }

        public void onPanelHidden() {
            MetricsLogger.hidden(NotificationManagerService.this.getContext(), 127);
            EventLogTags.writeNotificationPanelHidden();
        }

        public void clearEffects() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                if (NotificationManagerService.DBG) {
                    Slog.d(NotificationManagerService.TAG, "clearEffects");
                }
                NotificationManagerService.this.clearSoundLocked();
                NotificationManagerService.this.clearVibrateLocked();
                NotificationManagerService.this.clearLightsLocked();
            }
        }

        public void onNotificationError(int callingUid, int callingPid, String pkg, String tag, int id, int uid, int initialPid, String message, int userId) {
            NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 0, false, userId, 4, null);
        }

        public void onNotificationVisibilityChanged(NotificationVisibility[] newlyVisibleKeys, NotificationVisibility[] noLongerVisibleKeys) {
            Flog.i(400, "onNotificationVisibilityChanged called");
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r;
                for (NotificationVisibility nv : newlyVisibleKeys) {
                    r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(nv.key);
                    if (r != null) {
                        if (!r.isSeen()) {
                            if (NotificationManagerService.DBG) {
                                String str = NotificationManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Marking notification as visible ");
                                stringBuilder.append(nv.key);
                                Slog.d(str, stringBuilder.toString());
                            }
                            NotificationManagerService.this.reportSeen(r);
                            if (r.getNumSmartRepliesAdded() > 0 && !r.hasSeenSmartReplies()) {
                                r.setSeenSmartReplies(true);
                                NotificationManagerService.this.mMetricsLogger.write(r.getLogMaker().setCategory(1382).addTaggedData(1384, Integer.valueOf(r.getNumSmartRepliesAdded())));
                            }
                        }
                        r.setVisibility(true, nv.rank, nv.count);
                        NotificationManagerService.this.maybeRecordInterruptionLocked(r);
                        nv.recycle();
                    }
                }
                for (NotificationVisibility nv2 : noLongerVisibleKeys) {
                    r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(nv2.key);
                    if (r != null) {
                        r.setVisibility(false, nv2.rank, nv2.count);
                        nv2.recycle();
                    }
                }
            }
        }

        public void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded) {
            Flog.i(400, "onNotificationExpansionChanged called");
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    r.stats.onExpansionChanged(userAction, expanded);
                    long now = System.currentTimeMillis();
                    if (userAction) {
                        int i;
                        LogMaker category = r.getLogMaker(now).setCategory(128);
                        if (expanded) {
                            i = 3;
                        } else {
                            i = 14;
                        }
                        MetricsLogger.action(category.setType(i));
                    }
                    if (expanded && userAction) {
                        r.recordExpanded();
                    }
                    EventLogTags.writeNotificationExpansion(key, userAction, expanded, r.getLifespanMs(now), r.getFreshnessMs(now), r.getExposureMs(now));
                }
            }
        }

        public void onNotificationDirectReplied(String key) {
            NotificationManagerService.this.exitIdle();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    r.recordDirectReplied();
                    NotificationManagerService.this.reportUserInteraction(r);
                }
            }
        }

        public void onNotificationSmartRepliesAdded(String key, int replyCount) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    r.setNumSmartRepliesAdded(replyCount);
                }
            }
        }

        public void onNotificationSmartReplySent(String key, int replyIndex) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    NotificationManagerService.this.mMetricsLogger.write(r.getLogMaker().setCategory(1383).setSubtype(replyIndex));
                    NotificationManagerService.this.reportUserInteraction(r);
                }
            }
        }

        public void onNotificationSettingsViewed(String key) {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(key);
                if (r != null) {
                    r.recordViewedSettings();
                }
            }
        }
    };
    private Light mNotificationLight;
    @GuardedBy("mNotificationLock")
    final ArrayList<NotificationRecord> mNotificationList = new ArrayList();
    final Object mNotificationLock = new Object();
    private boolean mNotificationPulseEnabled;
    private HwSysResource mNotificationResource;
    private final BroadcastReceiver mNotificationTimeoutReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Missing block: B:13:0x0028, code:
            if (r0 == null) goto L_0x0063;
     */
        /* JADX WARNING: Missing block: B:14:0x002a, code:
            r1.this$0.cancelNotification(r0.sbn.getUid(), r0.sbn.getInitialPid(), r0.sbn.getPackageName(), r0.sbn.getTag(), r0.sbn.getId(), 0, 64, true, r0.getUserId(), 19, null);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            Throwable th;
            Intent intent2;
            String action = intent.getAction();
            if (action != null) {
                if (NotificationManagerService.ACTION_NOTIFICATION_TIMEOUT.equals(action)) {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        try {
                            try {
                                NotificationRecord record = NotificationManagerService.this.findNotificationByKeyLocked(intent.getStringExtra(NotificationManagerService.EXTRA_KEY));
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            intent2 = intent;
                            throw th;
                        }
                    }
                }
                intent2 = intent;
            }
        }
    };
    @GuardedBy("mNotificationLock")
    final ArrayMap<String, NotificationRecord> mNotificationsByKey = new ArrayMap();
    private final BroadcastReceiver mPackageIntentReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Removed duplicated region for block: B:73:0x016b  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            Intent intent2 = intent;
            String action = intent.getAction();
            if (action != null) {
                String str;
                boolean z;
                String[] pkgList;
                boolean z2;
                int[] uidList;
                int length;
                int i;
                int changeUserId;
                boolean removingPackage;
                boolean queryRestart = false;
                boolean queryRemove = false;
                boolean packageChanged = false;
                boolean cancelNotifications = true;
                boolean hideNotifications = false;
                boolean unhideNotifications = false;
                if (!action.equals("android.intent.action.PACKAGE_ADDED")) {
                    boolean equals = action.equals("android.intent.action.PACKAGE_REMOVED");
                    queryRemove = equals;
                    if (!(equals || action.equals("android.intent.action.PACKAGE_RESTARTED"))) {
                        equals = action.equals("android.intent.action.PACKAGE_CHANGED");
                        packageChanged = equals;
                        if (!equals) {
                            equals = action.equals("android.intent.action.QUERY_PACKAGE_RESTART");
                            queryRestart = equals;
                            if (!(equals || action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE") || action.equals("android.intent.action.PACKAGES_SUSPENDED") || action.equals("android.intent.action.PACKAGES_UNSUSPENDED"))) {
                                str = action;
                                z = packageChanged;
                            }
                        }
                    }
                }
                z = packageChanged;
                packageChanged = queryRestart;
                int changeUserId2 = intent2.getIntExtra("android.intent.extra.user_handle", -1);
                Flog.i(400, "mIntentReceiver-Package changed");
                int[] uidList2 = null;
                queryRestart = queryRemove && !intent2.getBooleanExtra("android.intent.extra.REPLACING", false);
                boolean removingPackage2 = queryRestart;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("action=");
                stringBuilder.append(action);
                stringBuilder.append(" removing=");
                boolean removingPackage3 = removingPackage2;
                stringBuilder.append(removingPackage3);
                Flog.i(400, stringBuilder.toString());
                if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                    pkgList = intent2.getStringArrayExtra("android.intent.extra.changed_package_list");
                    uidList2 = intent2.getIntArrayExtra("android.intent.extra.changed_uid_list");
                } else if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                    pkgList = intent2.getStringArrayExtra("android.intent.extra.changed_package_list");
                    cancelNotifications = false;
                    hideNotifications = true;
                } else if (action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                    pkgList = intent2.getStringArrayExtra("android.intent.extra.changed_package_list");
                    cancelNotifications = false;
                    unhideNotifications = true;
                } else if (packageChanged) {
                    pkgList = intent2.getStringArrayExtra("android.intent.extra.PACKAGES");
                    uidList2 = new int[]{intent2.getIntExtra("android.intent.extra.UID", -1)};
                } else {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        String pkgName = uri.getSchemeSpecificPart();
                        if (pkgName != null) {
                            int uid;
                            if (queryRemove || z) {
                                uid = intent2.getIntExtra("android.intent.extra.UID", -1);
                                if (uid == -1 || NotificationManagerService.this.mNotificationResource == null) {
                                } else {
                                    str = action;
                                    NotificationManagerService.this.mNotificationResource.clear(uid, pkgName, -1);
                                }
                            } else {
                                str = action;
                            }
                            if (z) {
                                try {
                                    uid = NotificationManagerService.this.mPackageManager.getApplicationEnabledSetting(pkgName, changeUserId2 != -1 ? changeUserId2 : 0);
                                    if (uid == 1 || uid == 0) {
                                        cancelNotifications = false;
                                    }
                                } catch (IllegalArgumentException e) {
                                    Flog.i(400, "Exception trying to look up app enabled setting", e);
                                } catch (RemoteException e2) {
                                }
                            }
                            pkgList = new String[1];
                            z2 = false;
                            pkgList[0] = pkgName;
                            uidList = new int[]{intent2.getIntExtra("android.intent.extra.UID", -1)};
                            if (pkgList != null && pkgList.length > 0) {
                                length = pkgList.length;
                                i = z2;
                                while (i < length) {
                                    int i2;
                                    boolean removingPackage4;
                                    boolean z3;
                                    int i3;
                                    String pkgName2 = pkgList[i];
                                    if (cancelNotifications) {
                                        i2 = i;
                                        removingPackage4 = removingPackage3;
                                        z3 = z2;
                                        i3 = length;
                                        changeUserId = changeUserId2;
                                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkgName2, null, 0, 0, !packageChanged ? true : z2, changeUserId, 5, null);
                                    } else {
                                        i2 = i;
                                        removingPackage4 = removingPackage3;
                                        i3 = length;
                                        changeUserId = changeUserId2;
                                        z3 = z2;
                                        if (hideNotifications) {
                                            NotificationManagerService.this.hideNotificationsForPackages(pkgList);
                                        } else if (unhideNotifications) {
                                            NotificationManagerService.this.unhideNotificationsForPackages(pkgList);
                                        }
                                    }
                                    i = i2 + 1;
                                    z2 = z3;
                                    length = i3;
                                    removingPackage3 = removingPackage4;
                                    changeUserId2 = changeUserId;
                                }
                            }
                            changeUserId = changeUserId2;
                            removingPackage = removingPackage3;
                            NotificationManagerService.this.mListeners.onPackagesChanged(removingPackage, pkgList, uidList);
                            NotificationManagerService.this.mAssistants.onPackagesChanged(removingPackage, pkgList, uidList);
                            NotificationManagerService.this.mConditionProviders.onPackagesChanged(removingPackage, pkgList, uidList);
                            NotificationManagerService.this.mRankingHelper.onPackagesChanged(removingPackage, changeUserId, pkgList, uidList);
                            NotificationManagerService.this.savePolicyFile();
                        }
                        return;
                    }
                    return;
                }
                uidList = uidList2;
                z2 = false;
                length = pkgList.length;
                i = z2;
                while (i < length) {
                }
                changeUserId = changeUserId2;
                removingPackage = removingPackage3;
                NotificationManagerService.this.mListeners.onPackagesChanged(removingPackage, pkgList, uidList);
                NotificationManagerService.this.mAssistants.onPackagesChanged(removingPackage, pkgList, uidList);
                NotificationManagerService.this.mConditionProviders.onPackagesChanged(removingPackage, pkgList, uidList);
                NotificationManagerService.this.mRankingHelper.onPackagesChanged(removingPackage, changeUserId, pkgList, uidList);
                NotificationManagerService.this.savePolicyFile();
            }
        }
    };
    private IPackageManager mPackageManager;
    private PackageManager mPackageManagerClient;
    private AtomicFile mPolicyFile;
    private PowerSaverObserver mPowerSaverObserver;
    private RankingHandler mRankingHandler;
    private RankingHelper mRankingHelper;
    private final HandlerThread mRankingThread = new HandlerThread("ranker", 10);
    final ArrayMap<Integer, ArrayList<NotifyingApp>> mRecentApps = new ArrayMap();
    private final BroadcastReceiver mRestoreReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.os.action.SETTING_RESTORED".equals(intent.getAction())) {
                try {
                    String element = intent.getStringExtra("setting_name");
                    String newValue = intent.getStringExtra("new_value");
                    int restoredFromSdkInt = intent.getIntExtra("restored_from_sdk_int", 0);
                    NotificationManagerService.this.mListeners.onSettingRestored(element, newValue, restoredFromSdkInt, getSendingUserId());
                    NotificationManagerService.this.mConditionProviders.onSettingRestored(element, newValue, restoredFromSdkInt, getSendingUserId());
                } catch (Exception e) {
                    Slog.wtf(NotificationManagerService.TAG, "Cannot restore managed services from settings", e);
                }
            }
        }
    };
    private boolean mScreenOn = true;
    private final IBinder mService = new Stub() {
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (1599293262 == code) {
                try {
                    int event = data.readInt();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("NotificationManagerService.onTransact: got ST_GET_NOTIFICATIONS event ");
                    stringBuilder.append(event);
                    Flog.i(400, stringBuilder.toString());
                    if (event == 1) {
                        NotificationManagerService.this.handleGetNotifications(data, reply);
                        return true;
                    }
                } catch (Exception e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("NotificationManagerService.onTransact: catch exception ");
                    stringBuilder2.append(e.toString());
                    Flog.i(400, stringBuilder2.toString());
                    return false;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        public void enqueueToast(String pkg, ITransientNotification callback, int duration) {
            enqueueToastEx(pkg, callback, duration, 0);
        }

        public void enqueueToastEx(String pkg, ITransientNotification callback, int duration, int displayId) {
            Throwable th;
            long callingId;
            int i;
            ArrayList arrayList;
            long callingId2;
            int callingPid;
            String str = pkg;
            ITransientNotification iTransientNotification = callback;
            int i2 = duration;
            int i3 = displayId;
            IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
            if (manager != null) {
                manager.sendBehavior(BehaviorId.NOTIFICATIONMANAGER_ENQUEUETOAST);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enqueueToast pkg=");
            stringBuilder.append(str);
            stringBuilder.append(" callback=");
            stringBuilder.append(iTransientNotification);
            stringBuilder.append(" duration=");
            stringBuilder.append(i2);
            Flog.i(400, stringBuilder.toString());
            String str2;
            if (str == null || iTransientNotification == null) {
                str2 = NotificationManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Not doing toast. pkg=");
                stringBuilder.append(str);
                stringBuilder.append(" callback=");
                stringBuilder.append(iTransientNotification);
                Slog.e(str2, stringBuilder.toString());
            } else if (!NotificationManagerService.mIsChina || NotificationManagerService.this.isAllowToShow(str, ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getLastResumedActivity())) {
                boolean z = NotificationManagerService.this.isCallerSystemOrPhone() || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str);
                boolean isSystemToast = z;
                boolean isPackageSuspended = NotificationManagerService.this.isPackageSuspendedForUser(str, Binder.getCallingUid());
                if (isSystemToast || (areNotificationsEnabledForPackage(str, Binder.getCallingUid()) && !isPackageSuspended)) {
                    ArrayList arrayList2 = NotificationManagerService.this.mToastQueue;
                    synchronized (arrayList2) {
                        try {
                            int index;
                            int callingPid2 = Binder.getCallingPid();
                            long callingId3 = Binder.clearCallingIdentity();
                            if (isSystemToast) {
                                try {
                                    index = NotificationManagerService.this.indexOfToastLocked(str, iTransientNotification);
                                } catch (Throwable th2) {
                                    th = th2;
                                    callingId = callingId3;
                                    i = callingPid2;
                                    arrayList = arrayList2;
                                    Binder.restoreCallingIdentity(callingId);
                                    throw th;
                                }
                            }
                            try {
                                index = NotificationManagerService.this.indexOfToastPackageLocked(str);
                            } catch (Throwable th3) {
                                th = th3;
                                callingId = callingId3;
                                i = callingPid2;
                                arrayList = arrayList2;
                            }
                            int index2 = index;
                            ToastRecord record;
                            if (index2 >= 0) {
                                record = (ToastRecord) NotificationManagerService.this.mToastQueue.get(index2);
                                record.update(i2);
                                try {
                                    record.callback.hide();
                                } catch (RemoteException e) {
                                }
                                record.update(iTransientNotification);
                                callingId2 = callingId3;
                                callingPid = callingPid2;
                                arrayList = arrayList2;
                            } else {
                                Binder token = new Binder();
                                NotificationManagerService.this.mWindowManagerInternal.addWindowToken(token, 2005, i3);
                                if (i3 == 0) {
                                    try {
                                        record = record;
                                        callingId2 = callingId3;
                                        callingPid = callingPid2;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        callingId = callingId3;
                                        i = callingPid2;
                                        arrayList = arrayList2;
                                        Binder.restoreCallingIdentity(callingId);
                                        throw th;
                                    }
                                    try {
                                        record = new ToastRecord(callingPid2, str, iTransientNotification, i2, token);
                                        record = record;
                                        arrayList = arrayList2;
                                    } catch (Throwable th5) {
                                        th = th5;
                                        arrayList = arrayList2;
                                        callingId = callingId2;
                                        i = callingPid;
                                        Binder.restoreCallingIdentity(callingId);
                                        throw th;
                                    }
                                }
                                int i4 = index2;
                                callingId2 = callingId3;
                                callingPid = callingPid2;
                                try {
                                    record = record;
                                    arrayList = arrayList2;
                                    try {
                                        record = new ToastRecordEx(callingPid, str, iTransientNotification, i2, token, i3);
                                    } catch (Throwable th6) {
                                        th = th6;
                                        callingId = callingId2;
                                        i = callingPid;
                                        Binder.restoreCallingIdentity(callingId);
                                        throw th;
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    arrayList = arrayList2;
                                    callingId = callingId2;
                                    i = callingPid;
                                    Binder.restoreCallingIdentity(callingId);
                                    throw th;
                                }
                                NotificationManagerService.this.mToastQueue.add(record);
                                index2 = NotificationManagerService.this.mToastQueue.size() - 1;
                            }
                            try {
                                NotificationManagerService.this.keepProcessAliveIfNeededLocked(callingPid);
                                if (index2 == 0) {
                                    try {
                                        NotificationManagerService.this.showNextToastLocked();
                                    } catch (Throwable th8) {
                                        th = th8;
                                        callingId = callingId2;
                                    }
                                }
                                Binder.restoreCallingIdentity(callingId2);
                                return;
                            } catch (Throwable th9) {
                                th = th9;
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            arrayList = arrayList2;
                            throw th;
                        }
                    }
                }
                String str3;
                str2 = NotificationManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Suppressing toast from package ");
                stringBuilder.append(str);
                if (isPackageSuspended) {
                    str3 = " due to package suspended by administrator.";
                } else {
                    str3 = " by user request.";
                }
                stringBuilder.append(str3);
                Slog.e(str2, stringBuilder.toString());
            }
        }

        public void cancelToast(String pkg, ITransientNotification callback) {
            String str = NotificationManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cancelToast pkg=");
            stringBuilder.append(pkg);
            stringBuilder.append(" callback=");
            stringBuilder.append(callback);
            Slog.i(str, stringBuilder.toString());
            if (pkg == null || callback == null) {
                str = NotificationManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Not cancelling notification. pkg=");
                stringBuilder.append(pkg);
                stringBuilder.append(" callback=");
                stringBuilder.append(callback);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            synchronized (NotificationManagerService.this.mToastQueue) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    int index = NotificationManagerService.this.indexOfToastLocked(pkg, callback);
                    if (index >= 0) {
                        NotificationManagerService.this.cancelToastLocked(index);
                    } else {
                        String str2 = NotificationManagerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Toast already cancelled. pkg=");
                        stringBuilder2.append(pkg);
                        stringBuilder2.append(" callback=");
                        stringBuilder2.append(callback);
                        Slog.w(str2, stringBuilder2.toString());
                    }
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }

        public void finishToken(String pkg, ITransientNotification callback) {
            synchronized (NotificationManagerService.this.mToastQueue) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    int index = NotificationManagerService.this.indexOfToastLocked(pkg, callback);
                    if (index >= 0) {
                        NotificationManagerService.this.finishTokenLocked(((ToastRecord) NotificationManagerService.this.mToastQueue.get(index)).token);
                    } else {
                        String str = NotificationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Toast already killed. pkg=");
                        stringBuilder.append(pkg);
                        stringBuilder.append(" callback=");
                        stringBuilder.append(callback);
                        Slog.w(str, stringBuilder.toString());
                    }
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }

        public void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id, Notification notification, int userId) throws RemoteException {
            String str = pkg;
            Notification notification2 = notification;
            IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
            if (manager != null) {
                manager.sendBehavior(BehaviorId.NOTIFICATIONMANAGER_ENQUEUENOTIFICATIONWITHTAG);
            }
            NotificationManagerService.this.addHwExtraForNotification(notification2, str, Binder.getCallingPid());
            String str2 = opPkg;
            NotificationManagerService.this.enqueueNotificationInternal(NotificationManagerService.this.getNCTargetAppPkg(str2, str, notification2), str2, Binder.getCallingUid(), Binder.getCallingPid(), tag, id, notification2, userId);
        }

        public void cancelNotificationWithTag(String pkg, String tag, int id, int userId) {
            String str;
            int i;
            int i2;
            String str2 = pkg;
            IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
            if (manager != null) {
                manager.sendBehavior(BehaviorId.NOTIFICATIONMANAGER_CANCELNOTIFICATIONWITHTAG);
            }
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(str2);
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, false, "cancelNotificationWithTag", str2);
            if (NotificationManagerService.HWFLOW) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cancelNotificationWithTag pid ");
                stringBuilder.append(Binder.getCallingPid());
                stringBuilder.append(",uid = ");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(",tag = ");
                str = tag;
                stringBuilder.append(str);
                stringBuilder.append(",pkg =");
                stringBuilder.append(str2);
                stringBuilder.append(",id =");
                i = id;
                stringBuilder.append(i);
                Flog.i(400, stringBuilder.toString());
            } else {
                str = tag;
                i = id;
            }
            if (NotificationManagerService.this.isCallingUidSystem()) {
                i2 = 0;
            } else {
                i2 = 1088;
            }
            NotificationManagerService.this.cancelNotification(Binder.getCallingUid(), Binder.getCallingPid(), str2, str, i, 0, i2, false, userId2, 8, null);
        }

        public void cancelAllNotifications(String pkg, int userId) {
            String str = pkg;
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
            NotificationManagerService.this.cancelAllNotificationsInt(Binder.getCallingUid(), Binder.getCallingPid(), str, null, 0, 64, true, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, false, "cancelAllNotifications", str), 9, null);
        }

        public void setNotificationsEnabledForPackage(String pkg, int uid, boolean enabled) {
            enforceSystemOrSystemUI("setNotificationsEnabledForPackage");
            NotificationManagerService.this.mRankingHelper.setEnabled(pkg, uid, enabled);
            if (!enabled) {
                NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkg, null, 0, 0, true, UserHandle.getUserId(uid), 7, null);
            }
            try {
                NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.APP_BLOCK_STATE_CHANGED").putExtra("android.app.extra.BLOCKED_STATE", enabled ^ 1).addFlags(268435456).setPackage(pkg), UserHandle.of(UserHandle.getUserId(uid)), null);
            } catch (SecurityException e) {
                Slog.w(NotificationManagerService.TAG, "Can't notify app about app block change", e);
            }
            NotificationManagerService.this.savePolicyFile();
        }

        public void setNotificationsEnabledWithImportanceLockForPackage(String pkg, int uid, boolean enabled) {
            setNotificationsEnabledForPackage(pkg, uid, enabled);
            NotificationManagerService.this.mRankingHelper.setAppImportanceLocked(pkg, uid);
        }

        public boolean areNotificationsEnabled(String pkg) {
            return areNotificationsEnabledForPackage(pkg, Binder.getCallingUid());
        }

        public boolean areNotificationsEnabledForPackage(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            return NotificationManagerService.this.mRankingHelper.getImportance(pkg, uid) != 0;
        }

        public int getPackageImportance(String pkg) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            return NotificationManagerService.this.mRankingHelper.getImportance(pkg, Binder.getCallingUid());
        }

        public boolean canShowBadge(String pkg, int uid) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mRankingHelper.canShowBadge(pkg, uid);
        }

        public void setShowBadge(String pkg, int uid, boolean showBadge) {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.mRankingHelper.setShowBadge(pkg, uid, showBadge);
            NotificationManagerService.this.savePolicyFile();
        }

        public void updateNotificationChannelGroupForPackage(String pkg, int uid, NotificationChannelGroup group) throws RemoteException {
            enforceSystemOrSystemUI("Caller not system or systemui");
            NotificationManagerService.this.createNotificationChannelGroup(pkg, uid, group, false, false);
            NotificationManagerService.this.savePolicyFile();
        }

        public void createNotificationChannelGroups(String pkg, ParceledListSlice channelGroupList) throws RemoteException {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            List<NotificationChannelGroup> groups = channelGroupList.getList();
            List<NotificationChannelGroup> groupsCache = new ArrayList(groups);
            int groupSize = groups.size();
            for (int i = 0; i < groupSize; i++) {
                String str = pkg;
                NotificationManagerService.this.createNotificationChannelGroup(str, Binder.getCallingUid(), (NotificationChannelGroup) groupsCache.get(i), true, false);
            }
            NotificationManagerService.this.savePolicyFile();
        }

        private void createNotificationChannelsImpl(String pkg, int uid, ParceledListSlice channelsList) {
            List<NotificationChannel> channels = channelsList.getList();
            int channelsSize = channels.size();
            for (int i = 0; i < channelsSize; i++) {
                NotificationChannel channel = (NotificationChannel) channels.get(i);
                Preconditions.checkNotNull(channel, "channel in list is null");
                NotificationManagerService.this.mRankingHelper.createNotificationChannel(pkg, uid, channel, true, NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(pkg, UserHandle.getUserId(uid)));
                NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(pkg, UserHandle.getUserHandleForUid(uid), NotificationManagerService.this.mRankingHelper.getNotificationChannel(pkg, uid, channel.getId(), false), 1);
            }
            NotificationManagerService.this.savePolicyFile();
        }

        public void createNotificationChannels(String pkg, ParceledListSlice channelsList) throws RemoteException {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            createNotificationChannelsImpl(pkg, Binder.getCallingUid(), channelsList);
        }

        public void createNotificationChannelsForPackage(String pkg, int uid, ParceledListSlice channelsList) throws RemoteException {
            int callingUid = Binder.getCallingUid();
            String callingPkgName = null;
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            if (packageManagerInternal != null) {
                callingPkgName = packageManagerInternal.getNameForUid(callingUid);
            }
            if (!NotificationManagerService.NOTIFICATION_CENTER_PKG.equals(callingPkgName)) {
                NotificationManagerService.this.checkCallerIsSystem();
            }
            createNotificationChannelsImpl(pkg, uid, channelsList);
        }

        public NotificationChannel getNotificationChannel(String pkg, String channelId) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            return NotificationManagerService.this.mRankingHelper.getNotificationChannel(pkg, Binder.getCallingUid(), channelId, false);
        }

        public NotificationChannel getNotificationChannelForPackage(String pkg, int uid, String channelId, boolean includeDeleted) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mRankingHelper.getNotificationChannel(pkg, uid, channelId, includeDeleted);
        }

        public void deleteNotificationChannel(String pkg, String channelId) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            int callingUid = Binder.getCallingUid();
            if ("miscellaneous".equals(channelId)) {
                throw new IllegalArgumentException("Cannot delete default channel");
            }
            NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, pkg, channelId, 0, 0, true, UserHandle.getUserId(callingUid), 17, null);
            NotificationManagerService.this.mRankingHelper.deleteNotificationChannel(pkg, callingUid, channelId);
            NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(pkg, UserHandle.getUserHandleForUid(callingUid), NotificationManagerService.this.mRankingHelper.getNotificationChannel(pkg, callingUid, channelId, true), 3);
            NotificationManagerService.this.savePolicyFile();
        }

        public NotificationChannelGroup getNotificationChannelGroup(String pkg, String groupId) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroupWithChannels(pkg, Binder.getCallingUid(), groupId, false);
        }

        public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroups(String pkg) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroups(pkg, Binder.getCallingUid(), false, false);
        }

        public void deleteNotificationChannelGroup(String pkg, String groupId) {
            String str = pkg;
            String str2 = groupId;
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(str);
            int callingUid = Binder.getCallingUid();
            NotificationChannelGroup groupToDelete = NotificationManagerService.this.mRankingHelper.getNotificationChannelGroup(str2, str, callingUid);
            if (groupToDelete != null) {
                List<NotificationChannel> deletedChannels = NotificationManagerService.this.mRankingHelper.deleteNotificationChannelGroup(str, callingUid, str2);
                int i = 0;
                while (true) {
                    int i2 = i;
                    List<NotificationChannel> deletedChannels2;
                    if (i2 < deletedChannels.size()) {
                        NotificationChannel deletedChannel = (NotificationChannel) deletedChannels.get(i2);
                        NotificationChannel deletedChannel2 = deletedChannel;
                        int i3 = i2;
                        deletedChannels2 = deletedChannels;
                        NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, str, deletedChannel.getId(), 0, 0, true, UserHandle.getUserId(Binder.getCallingUid()), 17, null);
                        NotificationManagerService.this.mListeners.notifyNotificationChannelChanged(str, UserHandle.getUserHandleForUid(callingUid), deletedChannel2, 3);
                        i = i3 + 1;
                        deletedChannels = deletedChannels2;
                    } else {
                        deletedChannels2 = deletedChannels;
                        NotificationManagerService.this.mListeners.notifyNotificationChannelGroupChanged(str, UserHandle.getUserHandleForUid(callingUid), groupToDelete, 3);
                        NotificationManagerService.this.savePolicyFile();
                        return;
                    }
                }
            }
        }

        public void updateNotificationChannelForPackage(String pkg, int uid, NotificationChannel channel) {
            enforceSystemOrSystemUI("Caller not system or systemui");
            Preconditions.checkNotNull(channel);
            String str = NotificationManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateNotificationChannelForPackage, channel: ");
            stringBuilder.append(channel);
            Slog.i(str, stringBuilder.toString());
            NotificationManagerService.this.updateNotificationChannelInt(pkg, uid, channel, false);
        }

        public ParceledListSlice<NotificationChannel> getNotificationChannelsForPackage(String pkg, int uid, boolean includeDeleted) {
            enforceSystemOrSystemUI("getNotificationChannelsForPackage");
            return NotificationManagerService.this.mRankingHelper.getNotificationChannels(pkg, uid, includeDeleted);
        }

        public int getNumNotificationChannelsForPackage(String pkg, int uid, boolean includeDeleted) {
            enforceSystemOrSystemUI("getNumNotificationChannelsForPackage");
            return NotificationManagerService.this.mRankingHelper.getNotificationChannels(pkg, uid, includeDeleted).getList().size();
        }

        public boolean onlyHasDefaultChannel(String pkg, int uid) {
            enforceSystemOrSystemUI("onlyHasDefaultChannel");
            return NotificationManagerService.this.mRankingHelper.onlyHasDefaultChannel(pkg, uid);
        }

        public int getDeletedChannelCount(String pkg, int uid) {
            enforceSystemOrSystemUI("getDeletedChannelCount");
            return NotificationManagerService.this.mRankingHelper.getDeletedChannelCount(pkg, uid);
        }

        public int getBlockedChannelCount(String pkg, int uid) {
            enforceSystemOrSystemUI("getBlockedChannelCount");
            return NotificationManagerService.this.mRankingHelper.getBlockedChannelCount(pkg, uid);
        }

        public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroupsForPackage(String pkg, int uid, boolean includeDeleted) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroups(pkg, uid, includeDeleted, true);
        }

        public NotificationChannelGroup getPopulatedNotificationChannelGroupForPackage(String pkg, int uid, String groupId, boolean includeDeleted) {
            enforceSystemOrSystemUI("getPopulatedNotificationChannelGroupForPackage");
            return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroupWithChannels(pkg, uid, groupId, includeDeleted);
        }

        public NotificationChannelGroup getNotificationChannelGroupForPackage(String groupId, String pkg, int uid) {
            enforceSystemOrSystemUI("getNotificationChannelGroupForPackage");
            return NotificationManagerService.this.mRankingHelper.getNotificationChannelGroup(groupId, pkg, uid);
        }

        public ParceledListSlice<NotificationChannel> getNotificationChannels(String pkg) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            return NotificationManagerService.this.mRankingHelper.getNotificationChannels(pkg, Binder.getCallingUid(), false);
        }

        public ParceledListSlice<NotifyingApp> getRecentNotifyingAppsForUser(int userId) {
            ParceledListSlice<NotifyingApp> parceledListSlice;
            NotificationManagerService.this.checkCallerIsSystem();
            synchronized (NotificationManagerService.this.mNotificationLock) {
                parceledListSlice = new ParceledListSlice(new ArrayList((Collection) NotificationManagerService.this.mRecentApps.getOrDefault(Integer.valueOf(userId), new ArrayList())));
            }
            return parceledListSlice;
        }

        public int getBlockedAppCount(int userId) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mRankingHelper.getBlockedAppCount(userId);
        }

        public boolean areChannelsBypassingDnd() {
            return NotificationManagerService.this.mRankingHelper.areChannelsBypassingDnd();
        }

        public void clearData(String packageName, int uid, boolean fromApp) throws RemoteException {
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.cancelAllNotificationsInt(NotificationManagerService.MY_UID, NotificationManagerService.MY_PID, packageName, null, 0, 0, true, UserHandle.getUserId(Binder.getCallingUid()), 17, null);
            String[] packages = new String[]{packageName};
            int[] uids = new int[]{uid};
            NotificationManagerService.this.mListeners.onPackagesChanged(true, packages, uids);
            NotificationManagerService.this.mAssistants.onPackagesChanged(true, packages, uids);
            NotificationManagerService.this.mConditionProviders.onPackagesChanged(true, packages, uids);
            if (!fromApp) {
                NotificationManagerService.this.mRankingHelper.onPackagesChanged(true, UserHandle.getCallingUserId(), packages, uids);
            }
            NotificationManagerService.this.savePolicyFile();
        }

        public StatusBarNotification[] getActiveNotifications(String callingPkg) {
            NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getActiveNotifications");
            StatusBarNotification[] tmp = null;
            if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, Binder.getCallingUid(), callingPkg) == 0) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    tmp = new StatusBarNotification[NotificationManagerService.this.mNotificationList.size()];
                    int N = NotificationManagerService.this.mNotificationList.size();
                    for (int i = 0; i < N; i++) {
                        tmp[i] = ((NotificationRecord) NotificationManagerService.this.mNotificationList.get(i)).sbn;
                    }
                }
            }
            return tmp;
        }

        public ParceledListSlice<StatusBarNotification> getAppActiveNotifications(String pkg, int incomingUserId) {
            ParceledListSlice<StatusBarNotification> parceledListSlice;
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            int userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), incomingUserId, true, false, "getAppActiveNotifications", pkg);
            synchronized (NotificationManagerService.this.mNotificationLock) {
                int i;
                StatusBarNotification sbn;
                ArrayMap<String, StatusBarNotification> map = new ArrayMap(NotificationManagerService.this.mNotificationList.size() + NotificationManagerService.this.mEnqueuedNotifications.size());
                int N = NotificationManagerService.this.mNotificationList.size();
                int i2 = 0;
                for (i = 0; i < N; i++) {
                    sbn = sanitizeSbn(pkg, userId, ((NotificationRecord) NotificationManagerService.this.mNotificationList.get(i)).sbn);
                    if (sbn != null) {
                        map.put(sbn.getKey(), sbn);
                    }
                }
                for (NotificationRecord snoozed : NotificationManagerService.this.mSnoozeHelper.getSnoozed(userId, pkg)) {
                    StatusBarNotification sbn2 = sanitizeSbn(pkg, userId, snoozed.sbn);
                    if (sbn2 != null) {
                        map.put(sbn2.getKey(), sbn2);
                    }
                }
                i = NotificationManagerService.this.mEnqueuedNotifications.size();
                while (i2 < i) {
                    sbn = sanitizeSbn(pkg, userId, ((NotificationRecord) NotificationManagerService.this.mEnqueuedNotifications.get(i2)).sbn);
                    if (sbn != null) {
                        map.put(sbn.getKey(), sbn);
                    }
                    i2++;
                }
                ArrayList<StatusBarNotification> list = new ArrayList(map.size());
                list.addAll(map.values());
                parceledListSlice = new ParceledListSlice(list);
            }
            return parceledListSlice;
        }

        private StatusBarNotification sanitizeSbn(String pkg, int userId, StatusBarNotification sbn) {
            if (!sbn.getPackageName().equals(pkg)) {
                int i = userId;
            } else if (sbn.getUserId() == userId) {
                return new StatusBarNotification(sbn.getPackageName(), sbn.getOpPkg(), sbn.getId(), sbn.getTag(), sbn.getUid(), sbn.getInitialPid(), sbn.getNotification().clone(), sbn.getUser(), sbn.getOverrideGroupKey(), sbn.getPostTime());
            }
            return null;
        }

        public StatusBarNotification[] getHistoricalNotifications(String callingPkg, int count) {
            NotificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_NOTIFICATIONS", "NotificationManagerService.getHistoricalNotifications");
            StatusBarNotification[] tmp = null;
            if (NotificationManagerService.this.mAppOps.noteOpNoThrow(25, Binder.getCallingUid(), callingPkg) == 0) {
                synchronized (NotificationManagerService.this.mArchive) {
                    tmp = NotificationManagerService.this.mArchive.getArray(count);
                }
            }
            return tmp;
        }

        public void registerListener(INotificationListener listener, ComponentName component, int userid) {
            enforceSystemOrSystemUI("INotificationManager.registerListener");
            NotificationManagerService.this.mListeners.registerService(listener, component, userid);
        }

        public void unregisterListener(INotificationListener token, int userid) {
            NotificationManagerService.this.mListeners.unregisterService((IInterface) token, userid);
        }

        public void cancelNotificationsFromListener(INotificationListener token, String[] keys) {
            String[] strArr = keys;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long identity = Binder.clearCallingIdentity();
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cancelNotificationsFromListener called,callingUid = ");
                stringBuilder.append(callingUid);
                stringBuilder.append(",callingPid = ");
                stringBuilder.append(callingPid);
                Flog.i(400, stringBuilder.toString());
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    if (strArr != null) {
                        int N = strArr.length;
                        int i = 0;
                        while (true) {
                            int i2 = i;
                            if (i2 >= N) {
                                break;
                            }
                            int i3;
                            int N2;
                            NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(strArr[i2]);
                            if (r == null) {
                                i3 = i2;
                                N2 = N;
                            } else {
                                int userId = r.sbn.getUserId();
                                if (userId == info.userid || userId == -1 || NotificationManagerService.this.mUserProfiles.isCurrentProfile(userId)) {
                                    String packageName = r.sbn.getPackageName();
                                    String tag = r.sbn.getTag();
                                    String str = packageName;
                                    i3 = i2;
                                    String str2 = tag;
                                    N2 = N;
                                    cancelNotificationFromListenerLocked(info, callingUid, callingPid, str, str2, r.sbn.getId(), userId);
                                } else {
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Disallowed call from listener: ");
                                    stringBuilder2.append(info.service);
                                    throw new SecurityException(stringBuilder2.toString());
                                }
                            }
                            i = i3 + 1;
                            INotificationListener iNotificationListener = token;
                            N = N2;
                        }
                    } else {
                        NotificationManagerService.this.cancelAllLocked(callingUid, callingPid, info.userid, 11, info, info.supportsProfiles());
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestBindListener(ComponentName component) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(component.getPackageName());
            long identity = Binder.clearCallingIdentity();
            try {
                ManagedServices manager;
                if (NotificationManagerService.this.mAssistants.isComponentEnabledForCurrentProfiles(component)) {
                    manager = NotificationManagerService.this.mAssistants;
                } else {
                    manager = NotificationManagerService.this.mListeners;
                }
                manager.setComponentState(component, true);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestUnbindListener(INotificationListener token) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    info.getOwner().setComponentState(info.component, false);
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setNotificationsShownFromListener(INotificationListener token, String[] keys) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                    if (keys != null) {
                        int N = keys.length;
                        for (int i = 0; i < N; i++) {
                            NotificationRecord r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(keys[i]);
                            if (r != null) {
                                int userId = r.sbn.getUserId();
                                StringBuilder stringBuilder;
                                if (userId != info.userid && userId != -1 && !NotificationManagerService.this.mUserProfiles.isCurrentProfile(userId)) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Disallowed call from listener: ");
                                    stringBuilder.append(info.service);
                                    throw new SecurityException(stringBuilder.toString());
                                } else if (!r.isSeen()) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Marking notification as seen ");
                                    stringBuilder.append(keys[i]);
                                    Flog.i(400, stringBuilder.toString());
                                    NotificationManagerService.this.reportSeen(r);
                                    r.setSeen();
                                    NotificationManagerService.this.maybeRecordInterruptionLocked(r);
                                }
                            }
                        }
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @GuardedBy("mNotificationLock")
        private void cancelNotificationFromListenerLocked(ManagedServiceInfo info, int callingUid, int callingPid, String pkg, String tag, int id, int userId) {
            NotificationManagerService.this.cancelNotification(callingUid, callingPid, pkg, tag, id, 0, 66, true, userId, 10, info);
        }

        public void snoozeNotificationUntilContextFromListener(INotificationListener token, String key, String snoozeCriterionId) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.snoozeNotificationInt(key, -1, snoozeCriterionId, NotificationManagerService.this.mListeners.checkServiceTokenLocked(token));
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void snoozeNotificationUntilFromListener(INotificationListener token, String key, long duration) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.snoozeNotificationInt(key, duration, null, NotificationManagerService.this.mListeners.checkServiceTokenLocked(token));
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void unsnoozeNotificationFromAssistant(INotificationListener token, String key) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.unsnoozeNotificationInt(key, NotificationManagerService.this.mAssistants.checkServiceTokenLocked(token));
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Missing block: B:13:0x0054, code:
            android.os.Binder.restoreCallingIdentity(r12);
     */
        /* JADX WARNING: Missing block: B:14:0x0058, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void cancelNotificationFromListener(INotificationListener token, String pkg, String tag, int id) {
            Throwable th;
            INotificationListener iNotificationListener;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    try {
                        try {
                            ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                            if (info.supportsProfiles()) {
                                String str = NotificationManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Ignoring deprecated cancelNotification(pkg, tag, id) from ");
                                stringBuilder.append(info.component);
                                stringBuilder.append(" use cancelNotification(key) instead.");
                                Log.e(str, stringBuilder.toString());
                            } else {
                                cancelNotificationFromListenerLocked(info, callingUid, callingPid, pkg, tag, id, info.userid);
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            try {
                                throw th;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        iNotificationListener = token;
                        throw th;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
                iNotificationListener = token;
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }

        public ParceledListSlice<StatusBarNotification> getActiveNotificationsFromListener(INotificationListener token, String[] keys, int trim) {
            ParceledListSlice<StatusBarNotification> parceledListSlice;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                int i = 0;
                boolean getKeys = keys != null;
                int N = getKeys ? keys.length : NotificationManagerService.this.mNotificationList.size();
                ArrayList<StatusBarNotification> list = new ArrayList(N);
                while (i < N) {
                    NotificationRecord r;
                    if (getKeys) {
                        r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(keys[i]);
                    } else {
                        r = (NotificationRecord) NotificationManagerService.this.mNotificationList.get(i);
                    }
                    if (r != null) {
                        StatusBarNotification sbn = r.sbn;
                        if (NotificationManagerService.this.isVisibleToListener(sbn, info)) {
                            list.add(trim == 0 ? sbn : sbn.cloneLight());
                        }
                    }
                    i++;
                }
                parceledListSlice = new ParceledListSlice(list);
            }
            return parceledListSlice;
        }

        public ParceledListSlice<StatusBarNotification> getSnoozedNotificationsFromListener(INotificationListener token, int trim) {
            ParceledListSlice<StatusBarNotification> parceledListSlice;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                List<NotificationRecord> snoozedRecords = NotificationManagerService.this.mSnoozeHelper.getSnoozed();
                int N = snoozedRecords.size();
                ArrayList<StatusBarNotification> list = new ArrayList(N);
                for (int i = 0; i < N; i++) {
                    NotificationRecord r = (NotificationRecord) snoozedRecords.get(i);
                    if (r != null) {
                        StatusBarNotification sbn = r.sbn;
                        if (NotificationManagerService.this.isVisibleToListener(sbn, info)) {
                            list.add(trim == 0 ? sbn : sbn.cloneLight());
                        }
                    }
                }
                parceledListSlice = new ParceledListSlice(list);
            }
            return parceledListSlice;
        }

        public void requestHintsFromListener(INotificationListener token, int hints) {
            if (!"com.google.android.projection.gearhead".equals(NotificationManagerService.this.getPackageNameByPid(Binder.getCallingPid()))) {
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                        if ((hints & 7) != 0) {
                            NotificationManagerService.this.addDisabledHints(info, hints);
                        } else {
                            NotificationManagerService.this.removeDisabledHints(info, hints);
                        }
                        NotificationManagerService.this.updateListenerHintsLocked();
                        NotificationManagerService.this.updateEffectsSuppressorLocked();
                    }
                    Binder.restoreCallingIdentity(identity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public int getHintsFromListener(INotificationListener token) {
            int access$4700;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                access$4700 = NotificationManagerService.this.mListenerHints;
            }
            return access$4700;
        }

        public void requestInterruptionFilterFromListener(INotificationListener token, int interruptionFilter) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.mZenModeHelper.requestFromListener(NotificationManagerService.this.mListeners.checkServiceTokenLocked(token).component, interruptionFilter);
                    NotificationManagerService.this.updateInterruptionFilterLocked();
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getInterruptionFilterFromListener(INotificationListener token) throws RemoteException {
            int access$4800;
            synchronized (NotificationManagerService.this.mNotificationLight) {
                access$4800 = NotificationManagerService.this.mInterruptionFilter;
            }
            return access$4800;
        }

        public void setOnNotificationPostedTrimFromListener(INotificationListener token, int trim) throws RemoteException {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                ManagedServiceInfo info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
                if (info == null) {
                    return;
                }
                NotificationManagerService.this.mListeners.setOnNotificationPostedTrimLocked(info, trim);
            }
        }

        public int getZenMode() {
            return NotificationManagerService.this.mZenModeHelper.getZenMode();
        }

        public ZenModeConfig getZenModeConfig() {
            enforceSystemOrSystemUI("INotificationManager.getZenModeConfig");
            return NotificationManagerService.this.mZenModeHelper.getConfig();
        }

        public void setZenMode(int mode, Uri conditionId, String reason) throws RemoteException {
            enforceSystemOrSystemUI("INotificationManager.setZenMode");
            long identity = Binder.clearCallingIdentity();
            try {
                NotificationManagerService.this.mZenModeHelper.setManualZenMode(mode, conditionId, null, reason);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<ZenRule> getZenRules() throws RemoteException {
            enforcePolicyAccess(Binder.getCallingUid(), "getAutomaticZenRules");
            return NotificationManagerService.this.mZenModeHelper.getZenRules();
        }

        public AutomaticZenRule getAutomaticZenRule(String id) throws RemoteException {
            Preconditions.checkNotNull(id, "Id is null");
            enforcePolicyAccess(Binder.getCallingUid(), "getAutomaticZenRule");
            return NotificationManagerService.this.mZenModeHelper.getAutomaticZenRule(id);
        }

        public String addAutomaticZenRule(AutomaticZenRule automaticZenRule) throws RemoteException {
            Preconditions.checkNotNull(automaticZenRule, "automaticZenRule is null");
            Preconditions.checkNotNull(automaticZenRule.getName(), "Name is null");
            Preconditions.checkNotNull(automaticZenRule.getOwner(), "Owner is null");
            Preconditions.checkNotNull(automaticZenRule.getConditionId(), "ConditionId is null");
            enforcePolicyAccess(Binder.getCallingUid(), "addAutomaticZenRule");
            return NotificationManagerService.this.mZenModeHelper.addAutomaticZenRule(automaticZenRule, "addAutomaticZenRule");
        }

        public boolean updateAutomaticZenRule(String id, AutomaticZenRule automaticZenRule) throws RemoteException {
            Preconditions.checkNotNull(automaticZenRule, "automaticZenRule is null");
            Preconditions.checkNotNull(automaticZenRule.getName(), "Name is null");
            Preconditions.checkNotNull(automaticZenRule.getOwner(), "Owner is null");
            Preconditions.checkNotNull(automaticZenRule.getConditionId(), "ConditionId is null");
            enforcePolicyAccess(Binder.getCallingUid(), "updateAutomaticZenRule");
            return NotificationManagerService.this.mZenModeHelper.updateAutomaticZenRule(id, automaticZenRule, "updateAutomaticZenRule");
        }

        public boolean removeAutomaticZenRule(String id) throws RemoteException {
            Preconditions.checkNotNull(id, "Id is null");
            enforcePolicyAccess(Binder.getCallingUid(), "removeAutomaticZenRule");
            return NotificationManagerService.this.mZenModeHelper.removeAutomaticZenRule(id, "removeAutomaticZenRule");
        }

        public boolean removeAutomaticZenRules(String packageName) throws RemoteException {
            Preconditions.checkNotNull(packageName, "Package name is null");
            enforceSystemOrSystemUI("removeAutomaticZenRules");
            return NotificationManagerService.this.mZenModeHelper.removeAutomaticZenRules(packageName, "removeAutomaticZenRules");
        }

        public int getRuleInstanceCount(ComponentName owner) throws RemoteException {
            Preconditions.checkNotNull(owner, "Owner is null");
            enforceSystemOrSystemUI("getRuleInstanceCount");
            return NotificationManagerService.this.mZenModeHelper.getCurrentInstanceCount(owner);
        }

        public void setInterruptionFilter(String pkg, int filter) throws RemoteException {
            enforcePolicyAccess(pkg, "setInterruptionFilter");
            int zen = NotificationManager.zenModeFromInterruptionFilter(filter, -1);
            if (zen != -1) {
                long identity = Binder.clearCallingIdentity();
                try {
                    NotificationManagerService.this.mZenModeHelper.setManualZenMode(zen, null, pkg, "setInterruptionFilter");
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid filter: ");
                stringBuilder.append(filter);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public void notifyConditions(final String pkg, IConditionProvider provider, final Condition[] conditions) {
            final ManagedServiceInfo info = NotificationManagerService.this.mConditionProviders.checkServiceToken(provider);
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            NotificationManagerService.this.mHandler.post(new Runnable() {
                public void run() {
                    NotificationManagerService.this.mConditionProviders.notifyConditions(pkg, info, conditions);
                }
            });
        }

        public void requestUnbindProvider(IConditionProvider provider) {
            long identity = Binder.clearCallingIdentity();
            try {
                ManagedServiceInfo info = NotificationManagerService.this.mConditionProviders.checkServiceToken(provider);
                info.getOwner().setComponentState(info.component, false);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestBindProvider(ComponentName component) {
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(component.getPackageName());
            long identity = Binder.clearCallingIdentity();
            try {
                NotificationManagerService.this.mConditionProviders.setComponentState(component, true);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void enforceSystemOrSystemUI(String message) {
            if (!NotificationManagerService.this.isCallerSystemOrPhone()) {
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
            }
        }

        private void enforceSystemOrSystemUIOrSamePackage(String pkg, String message) {
            try {
                NotificationManagerService.this.checkCallerIsSystemOrSameApp(pkg);
            } catch (SecurityException e) {
                NotificationManagerService.this.getContext().enforceCallingPermission("android.permission.STATUS_BAR_SERVICE", message);
            }
        }

        private void enforcePolicyAccess(int uid, String method) {
            if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") != 0) {
                boolean accessAllowed = false;
                for (String isPackageOrComponentAllowed : NotificationManagerService.this.getContext().getPackageManager().getPackagesForUid(uid)) {
                    if (NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(isPackageOrComponentAllowed, UserHandle.getUserId(uid))) {
                        accessAllowed = true;
                    }
                }
                if (!accessAllowed) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Notification policy access denied calling ");
                    stringBuilder.append(method);
                    Slog.w(NotificationManagerService.TAG, stringBuilder.toString());
                    throw new SecurityException("Notification policy access denied");
                }
            }
        }

        private void enforcePolicyAccess(String pkg, String method) {
            if (NotificationManagerService.this.getContext().checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") != 0) {
                NotificationManagerService.this.checkCallerIsSameApp(pkg);
                if (!checkPolicyAccess(pkg)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Notification policy access denied calling ");
                    stringBuilder.append(method);
                    Slog.w(NotificationManagerService.TAG, stringBuilder.toString());
                    throw new SecurityException("Notification policy access denied");
                }
            }
        }

        private boolean checkPackagePolicyAccess(String pkg) {
            return NotificationManagerService.this.mConditionProviders.isPackageOrComponentAllowed(pkg, AnonymousClass11.getCallingUserHandle().getIdentifier());
        }

        private boolean checkPolicyAccess(String pkg) {
            boolean z = false;
            try {
                if (ActivityManager.checkComponentPermission("android.permission.MANAGE_NOTIFICATIONS", NotificationManagerService.this.getContext().getPackageManager().getPackageUidAsUser(pkg, UserHandle.getCallingUserId()), -1, true) == 0) {
                    return true;
                }
                if (checkPackagePolicyAccess(pkg) || NotificationManagerService.this.mListeners.isComponentEnabledForPackage(pkg) || (NotificationManagerService.this.mDpm != null && NotificationManagerService.this.mDpm.isActiveAdminWithPolicy(Binder.getCallingUid(), -1))) {
                    z = true;
                }
                return z;
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(NotificationManagerService.this.getContext(), NotificationManagerService.TAG, pw)) {
                DumpFilter filter = DumpFilter.parseFromArguments(args);
                if (filter.stats) {
                    NotificationManagerService.this.dumpJson(pw, filter);
                } else if (filter.proto) {
                    NotificationManagerService.this.dumpProto(fd, filter);
                } else if (filter.criticalPriority) {
                    NotificationManagerService.this.dumpNotificationRecords(pw, filter);
                } else {
                    NotificationManagerService.this.dumpImpl(pw, filter);
                }
            }
        }

        public ComponentName getEffectsSuppressor() {
            return !NotificationManagerService.this.mEffectsSuppressors.isEmpty() ? (ComponentName) NotificationManagerService.this.mEffectsSuppressors.get(0) : null;
        }

        public boolean matchesCallFilter(Bundle extras) {
            UserHandle userHandle;
            enforceSystemOrSystemUI("INotificationManager.matchesCallFilter");
            int userId = -10000;
            if (extras != null) {
                userId = extras.getInt("userId", -10000);
            }
            if (userId == -10000) {
                userHandle = Binder.getCallingUserHandle();
            } else {
                userHandle = UserHandle.of(userId);
            }
            return NotificationManagerService.this.mZenModeHelper.matchesCallFilter(userHandle, extras, (ValidateNotificationPeople) NotificationManagerService.this.mRankingHelper.findExtractor(ValidateNotificationPeople.class), NotificationManagerService.MATCHES_CALL_FILTER_CONTACTS_TIMEOUT_MS, 1.0f);
        }

        public boolean isSystemConditionProviderEnabled(String path) {
            enforceSystemOrSystemUI("INotificationManager.isSystemConditionProviderEnabled");
            return NotificationManagerService.this.mConditionProviders.isSystemProviderEnabled(path);
        }

        public byte[] getBackupPayload(int user) {
            NotificationManagerService.this.checkCallerIsSystem();
            if (NotificationManagerService.DBG) {
                String str = NotificationManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getBackupPayload u=");
                stringBuilder.append(user);
                Slog.d(str, stringBuilder.toString());
            }
            if (user != 0) {
                String str2 = NotificationManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getBackupPayload: cannot backup policy for user ");
                stringBuilder2.append(user);
                Slog.w(str2, stringBuilder2.toString());
                return null;
            }
            byte[] toByteArray;
            synchronized (NotificationManagerService.this.mPolicyFile) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    NotificationManagerService.this.writePolicyXml(baos, true);
                    toByteArray = baos.toByteArray();
                } catch (IOException e) {
                    String str3 = NotificationManagerService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("getBackupPayload: error writing payload for user ");
                    stringBuilder3.append(user);
                    Slog.w(str3, stringBuilder3.toString(), e);
                    return null;
                }
            }
            return toByteArray;
        }

        /* JADX WARNING: Removed duplicated region for block: B:19:0x007c A:{Splitter: B:17:0x0070, ExcHandler: java.lang.NumberFormatException (r2_9 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x007c A:{Splitter: B:17:0x0070, ExcHandler: java.lang.NumberFormatException (r2_9 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:19:0x007c, code:
            r2 = move-exception;
     */
        /* JADX WARNING: Missing block: B:21:?, code:
            android.util.Slog.w(com.android.server.notification.NotificationManagerService.TAG, "applyRestore: error reading payload", r2);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void applyRestore(byte[] payload, int user) {
            String str;
            StringBuilder stringBuilder;
            NotificationManagerService.this.checkCallerIsSystem();
            if (NotificationManagerService.DBG) {
                str = NotificationManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("applyRestore u=");
                stringBuilder.append(user);
                stringBuilder.append(" payload=");
                stringBuilder.append(payload != null ? new String(payload, StandardCharsets.UTF_8) : null);
                Slog.d(str, stringBuilder.toString());
            }
            if (payload == null) {
                str = NotificationManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("applyRestore: no payload to restore for user ");
                stringBuilder.append(user);
                Slog.w(str, stringBuilder.toString());
            } else if (user != 0) {
                str = NotificationManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("applyRestore: cannot restore policy for user ");
                stringBuilder.append(user);
                Slog.w(str, stringBuilder.toString());
            } else {
                synchronized (NotificationManagerService.this.mPolicyFile) {
                    try {
                        NotificationManagerService.this.readPolicyXml(new ByteArrayInputStream(payload), true);
                        NotificationManagerService.this.savePolicyFile();
                    } catch (Exception e) {
                    }
                }
            }
        }

        public boolean isNotificationPolicyAccessGranted(String pkg) {
            return checkPolicyAccess(pkg);
        }

        public boolean isNotificationPolicyAccessGrantedForPackage(String pkg) {
            enforceSystemOrSystemUIOrSamePackage(pkg, "request policy access status for another package");
            return checkPolicyAccess(pkg);
        }

        public void setNotificationPolicyAccessGranted(String pkg, boolean granted) throws RemoteException {
            setNotificationPolicyAccessGrantedForUser(pkg, AnonymousClass11.getCallingUserHandle().getIdentifier(), granted);
        }

        public void setNotificationPolicyAccessGrantedForUser(String pkg, int userId, boolean granted) {
            NotificationManagerService.this.checkCallerIsSystemOrShell();
            long identity = Binder.clearCallingIdentity();
            try {
                if (NotificationManagerService.this.mAllowedManagedServicePackages.test(pkg)) {
                    NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(pkg, userId, true, granted);
                    NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(pkg).addFlags(1073741824), UserHandle.of(userId), null);
                    NotificationManagerService.this.savePolicyFile();
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public Policy getNotificationPolicy(String pkg) {
            long identity = Binder.clearCallingIdentity();
            try {
                Policy notificationPolicy = NotificationManagerService.this.mZenModeHelper.getNotificationPolicy();
                return notificationPolicy;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setNotificationPolicy(String pkg, Policy policy) {
            enforcePolicyAccess(pkg, "setNotificationPolicy");
            long identity = Binder.clearCallingIdentity();
            try {
                ApplicationInfo applicationInfo = NotificationManagerService.this.mPackageManager.getApplicationInfo(pkg, 0, UserHandle.getUserId(NotificationManagerService.MY_UID));
                Policy currPolicy = NotificationManagerService.this.mZenModeHelper.getNotificationPolicy();
                if (applicationInfo.targetSdkVersion < 28) {
                    policy = new Policy((((((policy.priorityCategories & -33) & -65) & -129) | (currPolicy.priorityCategories & 32)) | (currPolicy.priorityCategories & 64)) | (currPolicy.priorityCategories & 128), policy.priorityCallSenders, policy.priorityMessageSenders, policy.suppressedVisualEffects);
                }
                policy = new Policy(policy.priorityCategories, policy.priorityCallSenders, policy.priorityMessageSenders, NotificationManagerService.this.calculateSuppressedVisualEffects(policy, currPolicy, applicationInfo.targetSdkVersion));
                ZenLog.traceSetNotificationPolicy(pkg, applicationInfo.targetSdkVersion, policy);
                NotificationManagerService.this.mZenModeHelper.setNotificationPolicy(policy);
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
            Binder.restoreCallingIdentity(identity);
        }

        public List<String> getEnabledNotificationListenerPackages() {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mListeners.getAllowedPackages(AnonymousClass11.getCallingUserHandle().getIdentifier());
        }

        public List<ComponentName> getEnabledNotificationListeners(int userId) {
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mListeners.getAllowedComponents(userId);
        }

        public boolean isNotificationListenerAccessGranted(ComponentName listener) {
            Preconditions.checkNotNull(listener);
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(listener.getPackageName());
            return NotificationManagerService.this.mListeners.isPackageOrComponentAllowed(listener.flattenToString(), AnonymousClass11.getCallingUserHandle().getIdentifier());
        }

        public boolean isNotificationListenerAccessGrantedForUser(ComponentName listener, int userId) {
            Preconditions.checkNotNull(listener);
            NotificationManagerService.this.checkCallerIsSystem();
            return NotificationManagerService.this.mListeners.isPackageOrComponentAllowed(listener.flattenToString(), userId);
        }

        public boolean isNotificationAssistantAccessGranted(ComponentName assistant) {
            Preconditions.checkNotNull(assistant);
            NotificationManagerService.this.checkCallerIsSystemOrSameApp(assistant.getPackageName());
            return NotificationManagerService.this.mAssistants.isPackageOrComponentAllowed(assistant.flattenToString(), AnonymousClass11.getCallingUserHandle().getIdentifier());
        }

        public void setNotificationListenerAccessGranted(ComponentName listener, boolean granted) throws RemoteException {
            setNotificationListenerAccessGrantedForUser(listener, AnonymousClass11.getCallingUserHandle().getIdentifier(), granted);
        }

        public void setNotificationAssistantAccessGranted(ComponentName assistant, boolean granted) throws RemoteException {
            setNotificationAssistantAccessGrantedForUser(assistant, AnonymousClass11.getCallingUserHandle().getIdentifier(), granted);
        }

        public void setNotificationListenerAccessGrantedForUser(ComponentName listener, int userId, boolean granted) throws RemoteException {
            Preconditions.checkNotNull(listener);
            NotificationManagerService.this.checkCallerIsSystemOrShell();
            long identity = Binder.clearCallingIdentity();
            try {
                if (NotificationManagerService.this.mAllowedManagedServicePackages.test(listener.getPackageName())) {
                    NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(listener.flattenToString(), userId, false, granted);
                    NotificationManagerService.this.mListeners.setPackageOrComponentEnabled(listener.flattenToString(), userId, true, granted);
                    NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(listener.getPackageName()).addFlags(1073741824), UserHandle.of(userId), null);
                    NotificationManagerService.this.savePolicyFile();
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setNotificationAssistantAccessGrantedForUser(ComponentName assistant, int userId, boolean granted) throws RemoteException {
            Preconditions.checkNotNull(assistant);
            NotificationManagerService.this.checkCallerIsSystemOrShell();
            long identity = Binder.clearCallingIdentity();
            try {
                if (NotificationManagerService.this.mAllowedManagedServicePackages.test(assistant.getPackageName())) {
                    NotificationManagerService.this.mConditionProviders.setPackageOrComponentEnabled(assistant.flattenToString(), userId, false, granted);
                    NotificationManagerService.this.mAssistants.setPackageOrComponentEnabled(assistant.flattenToString(), userId, true, granted);
                    NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED").setPackage(assistant.getPackageName()).addFlags(1073741824), UserHandle.of(userId), null);
                    NotificationManagerService.this.savePolicyFile();
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void applyEnqueuedAdjustmentFromAssistant(INotificationListener token, Adjustment adjustment) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.mAssistants.checkServiceTokenLocked(token);
                    int N = NotificationManagerService.this.mEnqueuedNotifications.size();
                    for (int i = 0; i < N; i++) {
                        NotificationRecord n = (NotificationRecord) NotificationManagerService.this.mEnqueuedNotifications.get(i);
                        if (Objects.equals(adjustment.getKey(), n.getKey()) && Objects.equals(Integer.valueOf(adjustment.getUser()), Integer.valueOf(n.getUserId()))) {
                            NotificationManagerService.this.applyAdjustment(n, adjustment);
                            break;
                        }
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void applyAdjustmentFromAssistant(INotificationListener token, Adjustment adjustment) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.mAssistants.checkServiceTokenLocked(token);
                    NotificationManagerService.this.applyAdjustment((NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(adjustment.getKey()), adjustment);
                }
                NotificationManagerService.this.mRankingHandler.requestSort();
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void applyAdjustmentsFromAssistant(INotificationListener token, List<Adjustment> adjustments) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.mAssistants.checkServiceTokenLocked(token);
                    for (Adjustment adjustment : adjustments) {
                        NotificationManagerService.this.applyAdjustment((NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(adjustment.getKey()), adjustment);
                    }
                }
                NotificationManagerService.this.mRankingHandler.requestSort();
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void updateNotificationChannelGroupFromPrivilegedListener(INotificationListener token, String pkg, UserHandle user, NotificationChannelGroup group) throws RemoteException {
            Preconditions.checkNotNull(user);
            verifyPrivilegedListener(token, user);
            NotificationManagerService.this.createNotificationChannelGroup(pkg, getUidForPackageAndUser(pkg, user), group, false, true);
            NotificationManagerService.this.savePolicyFile();
        }

        public void updateNotificationChannelFromPrivilegedListener(INotificationListener token, String pkg, UserHandle user, NotificationChannel channel) throws RemoteException {
            Preconditions.checkNotNull(channel);
            Preconditions.checkNotNull(pkg);
            Preconditions.checkNotNull(user);
            verifyPrivilegedListener(token, user);
            NotificationManagerService.this.updateNotificationChannelInt(pkg, getUidForPackageAndUser(pkg, user), channel, true);
        }

        public ParceledListSlice<NotificationChannel> getNotificationChannelsFromPrivilegedListener(INotificationListener token, String pkg, UserHandle user) throws RemoteException {
            Preconditions.checkNotNull(pkg);
            Preconditions.checkNotNull(user);
            verifyPrivilegedListener(token, user);
            return NotificationManagerService.this.mRankingHelper.getNotificationChannels(pkg, getUidForPackageAndUser(pkg, user), false);
        }

        public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroupsFromPrivilegedListener(INotificationListener token, String pkg, UserHandle user) throws RemoteException {
            Preconditions.checkNotNull(pkg);
            Preconditions.checkNotNull(user);
            verifyPrivilegedListener(token, user);
            List<NotificationChannelGroup> groups = new ArrayList();
            groups.addAll(NotificationManagerService.this.mRankingHelper.getNotificationChannelGroups(pkg, getUidForPackageAndUser(pkg, user)));
            return new ParceledListSlice(groups);
        }

        private void verifyPrivilegedListener(INotificationListener token, UserHandle user) {
            ManagedServiceInfo info;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                info = NotificationManagerService.this.mListeners.checkServiceTokenLocked(token);
            }
            StringBuilder stringBuilder;
            if (!NotificationManagerService.this.hasCompanionDevice(info)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(info);
                stringBuilder.append(" does not have access");
                throw new SecurityException(stringBuilder.toString());
            } else if (!info.enabledAndUserMatches(user.getIdentifier())) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(info);
                stringBuilder.append(" does not have access");
                throw new SecurityException(stringBuilder.toString());
            }
        }

        private int getUidForPackageAndUser(String pkg, UserHandle user) throws RemoteException {
            int uid = 0;
            long identity = Binder.clearCallingIdentity();
            try {
                uid = NotificationManagerService.this.mPackageManager.getPackageUid(pkg, 0, user.getIdentifier());
                return uid;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void doBindRecSys() {
            Slog.w(NotificationManagerService.TAG, "do bind recsys service");
            NotificationManagerService.this.checkCallerIsSystem();
            NotificationManagerService.this.bindRecSys();
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new ShellCmd(NotificationManagerService.this, null).exec(this, in, out, err, args, callback, resultReceiver);
        }
    };
    private SettingsObserver mSettingsObserver;
    private SnoozeHelper mSnoozeHelper;
    protected String mSoundNotificationKey;
    StatusBarManagerInternal mStatusBar;
    final ArrayMap<String, NotificationRecord> mSummaryByGroupKey = new ArrayMap();
    boolean mSystemReady;
    final ArrayList<ToastRecord> mToastQueue = new ArrayList();
    private NotificationUsageStats mUsageStats;
    private boolean mUseAttentionLight;
    private final UserProfiles mUserProfiles = new UserProfiles();
    private String mVibrateNotificationKey;
    Vibrator mVibrator;
    private WindowManagerInternal mWindowManagerInternal;
    protected ZenModeHelper mZenModeHelper;
    private String[] noitfication_white_list = new String[]{"com.huawei.message", "com.android.mms", "com.android.contacts", "com.android.phone", "com.android.deskclock", "com.android.calendar", "com.android.systemui", PackageManagerService.PLATFORM_PACKAGE_NAME, "com.android.incallui", "com.android.phone.recorder", "com.android.cellbroadcastreceiver", TELECOM_PKG};
    private String[] plus_notification_white_list = new String[]{"com.android.bluetooth"};
    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (NotificationManagerService.ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE.equals(action)) {
                    if (intent.getIntExtra(NotificationManagerService.POWER_MODE, 0) == 3) {
                        if (NotificationManagerService.this.mPowerSaverObserver == null) {
                            NotificationManagerService.this.mPowerSaverObserver = new PowerSaverObserver(NotificationManagerService.this.mHandler);
                        }
                        NotificationManagerService.this.mPowerSaverObserver.observe();
                        Log.i(NotificationManagerService.TAG, "super power save 2.0 recevier brodcast register sqlite listener");
                    }
                } else if (NotificationManagerService.ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE.equals(action) && intent.getIntExtra(NotificationManagerService.SHUTDOWN_LIMIT_POWERMODE, 0) == 0 && NotificationManagerService.this.mPowerSaverObserver != null) {
                    NotificationManagerService.this.mPowerSaverObserver.unObserve();
                    NotificationManagerService.this.mPowerSaverObserver = null;
                    Log.i(NotificationManagerService.TAG, "super power save 2.0 recevier brodcast unregister sqlite listener");
                }
            }
        }
    };
    private HashSet<String> power_save_whiteSet = new HashSet();

    private static class Archive {
        final ArrayDeque<StatusBarNotification> mBuffer = new ArrayDeque(this.mBufferSize);
        final int mBufferSize;

        public Archive(int size) {
            this.mBufferSize = size;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            int N = this.mBuffer.size();
            sb.append("Archive (");
            sb.append(N);
            sb.append(" notification");
            sb.append(N == 1 ? ")" : "s)");
            return sb.toString();
        }

        public void record(StatusBarNotification nr) {
            if (this.mBuffer.size() == this.mBufferSize) {
                this.mBuffer.removeFirst();
            }
            this.mBuffer.addLast(nr.cloneLight());
        }

        public Iterator<StatusBarNotification> descendingIterator() {
            return this.mBuffer.descendingIterator();
        }

        public StatusBarNotification[] getArray(int count) {
            if (count == 0) {
                count = this.mBufferSize;
            }
            StatusBarNotification[] a = new StatusBarNotification[Math.min(count, this.mBuffer.size())];
            Iterator<StatusBarNotification> iter = descendingIterator();
            int i = 0;
            while (iter.hasNext() && i < count) {
                int i2 = i + 1;
                a[i] = (StatusBarNotification) iter.next();
                i = i2;
            }
            return a;
        }
    }

    public static final class DumpFilter {
        public boolean criticalPriority = false;
        public boolean filtered = false;
        public boolean normalPriority = false;
        public String pkgFilter;
        public boolean proto = false;
        public boolean redact = true;
        public long since;
        public boolean stats;
        public boolean zen;

        public static DumpFilter parseFromArguments(String[] args) {
            DumpFilter filter = new DumpFilter();
            int ai = 0;
            while (ai < args.length) {
                String a = args[ai];
                if (!PriorityDump.PROTO_ARG.equals(a)) {
                    if (!"--noredact".equals(a) && !"--reveal".equals(a)) {
                        if (!"p".equals(a) && !AbsLocationManagerService.DEL_PKG.equals(a) && !"--package".equals(a)) {
                            if (!"--zen".equals(a) && !"zen".equals(a)) {
                                if (!"--stats".equals(a)) {
                                    if (PriorityDump.PRIORITY_ARG.equals(a) && ai < args.length - 1) {
                                        ai++;
                                        String str = args[ai];
                                        boolean z = true;
                                        int hashCode = str.hashCode();
                                        if (hashCode != -1986416409) {
                                            if (hashCode == -1560189025 && str.equals(PriorityDump.PRIORITY_ARG_CRITICAL)) {
                                                z = false;
                                            }
                                        } else if (str.equals(PriorityDump.PRIORITY_ARG_NORMAL)) {
                                            z = true;
                                        }
                                        switch (z) {
                                            case false:
                                                filter.criticalPriority = true;
                                                break;
                                            case true:
                                                filter.normalPriority = true;
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                }
                                filter.stats = true;
                                if (ai < args.length - 1) {
                                    ai++;
                                    filter.since = Long.parseLong(args[ai]);
                                } else {
                                    filter.since = 0;
                                }
                            } else {
                                filter.filtered = true;
                                filter.zen = true;
                            }
                        } else if (ai < args.length - 1) {
                            ai++;
                            filter.pkgFilter = args[ai].trim().toLowerCase();
                            if (filter.pkgFilter.isEmpty()) {
                                filter.pkgFilter = null;
                            } else {
                                filter.filtered = true;
                            }
                        }
                    } else {
                        filter.redact = false;
                    }
                } else {
                    filter.proto = true;
                }
                ai++;
            }
            return filter;
        }

        public boolean matches(StatusBarNotification sbn) {
            boolean z = true;
            if (!this.filtered) {
                return true;
            }
            if (!this.zen && (sbn == null || !(matches(sbn.getPackageName()) || matches(sbn.getOpPkg())))) {
                z = false;
            }
            return z;
        }

        public boolean matches(ComponentName component) {
            boolean z = true;
            if (!this.filtered) {
                return true;
            }
            if (!this.zen && (component == null || !matches(component.getPackageName()))) {
                z = false;
            }
            return z;
        }

        public boolean matches(String pkg) {
            boolean z = true;
            if (!this.filtered) {
                return true;
            }
            if (!this.zen && (pkg == null || !pkg.toLowerCase().contains(this.pkgFilter))) {
                z = false;
            }
            return z;
        }

        public String toString() {
            if (this.stats) {
                return "stats";
            }
            if (this.zen) {
                return "zen";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('\'');
            stringBuilder.append(this.pkgFilter);
            stringBuilder.append('\'');
            return stringBuilder.toString();
        }
    }

    protected class EnqueueNotificationRunnable implements Runnable {
        private final NotificationRecord r;
        private final int userId;

        EnqueueNotificationRunnable(int userId, NotificationRecord r) {
            this.userId = userId;
            this.r = r;
        }

        public void run() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationManagerService.this.mEnqueuedNotifications.add(this.r);
                NotificationManagerService.this.scheduleTimeoutLocked(this.r);
                StatusBarNotification n = this.r.sbn;
                if (NotificationManagerService.DBG) {
                    String str = NotificationManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("EnqueueNotificationRunnable.run for: ");
                    stringBuilder.append(n.getKey());
                    Slog.d(str, stringBuilder.toString());
                }
                NotificationRecord old = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(n.getKey());
                if (old != null) {
                    this.r.copyRankingInformation(old);
                }
                int callingUid = n.getUid();
                int callingPid = n.getInitialPid();
                Notification notification = n.getNotification();
                String pkg = n.getPackageName();
                int id = n.getId();
                String tag = n.getTag();
                NotificationManagerService.this.handleGroupedNotificationLocked(this.r, old, callingUid, callingPid);
                if (n.isGroup() && notification.isGroupChild()) {
                    NotificationManagerService.this.mSnoozeHelper.repostGroupSummary(pkg, this.r.getUserId(), n.getGroupKey());
                }
                if (!pkg.equals("com.android.providers.downloads") || Log.isLoggable("DownloadManager", 2)) {
                    int bigContentViewSize;
                    int enqueueStatus = 0;
                    if (old != null) {
                        enqueueStatus = 1;
                    }
                    int enqueueStatus2 = enqueueStatus;
                    RemoteViews contentView = notification.contentView;
                    RemoteViews bigContentView = notification.bigContentView;
                    enqueueStatus = 0;
                    if (contentView != null) {
                        enqueueStatus = contentView.getCacheSize();
                    }
                    int contentViewSize = enqueueStatus;
                    if (bigContentView != null) {
                        bigContentViewSize = bigContentView.getCacheSize();
                    } else {
                        bigContentViewSize = 0;
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(notification.toString());
                    stringBuilder2.append(" contentViewSize = ");
                    stringBuilder2.append(contentViewSize);
                    stringBuilder2.append(" bigContentViewSize = ");
                    stringBuilder2.append(bigContentViewSize);
                    stringBuilder2.append(" N = ");
                    stringBuilder2.append(NotificationManagerService.this.mNotificationList.size());
                    String key = stringBuilder2.toString();
                    int i = this.userId;
                    EventLogTags.writeNotificationEnqueue(callingUid, callingPid, pkg, id, tag, i, key, enqueueStatus2);
                }
                NotificationManagerService.this.mRankingHelper.extractSignals(this.r);
                if (NotificationManagerService.this.mNotificationResource != null) {
                    NotificationManagerService.this.mNotificationResource.release(callingUid, pkg, -1);
                }
                if (NotificationManagerService.this.mAssistants.isEnabled()) {
                    NotificationManagerService.this.mAssistants.onNotificationEnqueued(this.r);
                    NotificationManagerService.this.mHandler.postDelayed(new PostNotificationRunnable(this.r.getKey()), NotificationManagerService.DELAY_FOR_ASSISTANT_TIME);
                } else {
                    NotificationManagerService.this.mHandler.post(new PostNotificationRunnable(this.r.getKey()));
                }
            }
        }
    }

    private interface FlagChecker {
        boolean apply(int i);
    }

    protected class PostNotificationRunnable implements Runnable {
        private final String key;

        PostNotificationRunnable(String key) {
            this.key = key;
        }

        /* JADX WARNING: Removed duplicated region for block: B:140:0x03e1 A:{SYNTHETIC, EDGE_INSN: B:140:0x03e1->B:127:0x03e1 ?: BREAK  , EDGE_INSN: B:140:0x03e1->B:127:0x03e1 ?: BREAK  } */
        /* JADX WARNING: Removed duplicated region for block: B:123:0x03c0 A:{Catch:{ IllegalArgumentException -> 0x02c1, all -> 0x03ae }} */
        /* JADX WARNING: Removed duplicated region for block: B:123:0x03c0 A:{Catch:{ IllegalArgumentException -> 0x02c1, all -> 0x03ae }} */
        /* JADX WARNING: Removed duplicated region for block: B:140:0x03e1 A:{SYNTHETIC, EDGE_INSN: B:140:0x03e1->B:127:0x03e1 ?: BREAK  , EDGE_INSN: B:140:0x03e1->B:127:0x03e1 ?: BREAK  , EDGE_INSN: B:140:0x03e1->B:127:0x03e1 ?: BREAK  } */
        /* JADX WARNING: Missing block: B:21:0x0077, code:
            return;
     */
        /* JADX WARNING: Missing block: B:70:0x022b, code:
            if (java.util.Objects.equals(r9.getGroup(), r6.getGroup()) == false) goto L_0x022d;
     */
        /* JADX WARNING: Missing block: B:116:0x03ad, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            Throwable th;
            int i;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = null;
                int i2 = 0;
                int N;
                String str;
                StringBuilder stringBuilder;
                try {
                    N = NotificationManagerService.this.mEnqueuedNotifications.size();
                    for (int i3 = 0; i3 < N; i3++) {
                        NotificationRecord enqueued = (NotificationRecord) NotificationManagerService.this.mEnqueuedNotifications.get(i3);
                        if (Objects.equals(this.key, enqueued.getKey())) {
                            r = enqueued;
                            break;
                        }
                    }
                    NotificationRecord r2 = r;
                    String str2;
                    int N2;
                    if (r2 != null) {
                        r2.setHidden(NotificationManagerService.this.isPackageSuspendedLocked(r2));
                        r = (NotificationRecord) NotificationManagerService.this.mNotificationsByKey.get(this.key);
                        final StatusBarNotification n = r2.sbn;
                        Notification notification = n.getNotification();
                        int index = NotificationManagerService.this.indexOfNotificationLocked(n.getKey());
                        StatusBarNotification oldSbn = null;
                        if (index < 0) {
                            NotificationManagerService.this.mNotificationList.add(r2);
                            NotificationManagerService.this.mUsageStats.registerPostedByApp(r2);
                            r2.setInterruptive(NotificationManagerService.this.isVisuallyInterruptive(null, r2));
                        } else {
                            r = (NotificationRecord) NotificationManagerService.this.mNotificationList.get(index);
                            NotificationManagerService.this.mNotificationList.set(index, r2);
                            NotificationManagerService.this.mUsageStats.registerUpdatedByApp(r2, r);
                            notification.flags |= r.getNotification().flags & 64;
                            r2.isUpdate = true;
                            r2.setTextChanged(NotificationManagerService.this.isVisuallyInterruptive(r, r2));
                        }
                        NotificationRecord old = r;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("enqueueNotificationInternal: n.getKey = ");
                        stringBuilder2.append(n.getKey());
                        Flog.i(400, stringBuilder2.toString());
                        NotificationManagerService.this.mNotificationsByKey.put(n.getKey(), r2);
                        if ((notification.flags & 64) != 0) {
                            notification.flags |= 34;
                        }
                        NotificationManagerService.this.applyZenModeLocked(r2);
                        NotificationManagerService.this.mRankingHelper.sort(NotificationManagerService.this.mNotificationList);
                        final int notifyType = NotificationManagerService.this.calculateNotifyType(r2);
                        int i4;
                        if (notification.getSmallIcon() != null) {
                            if (old != null) {
                                oldSbn = old.sbn;
                            }
                            if (!(n.getNotification().extras == null || NotificationManagerService.DISABLE_MULTIWIN)) {
                                n.getNotification().extras.putBoolean("toSingleLine", SplitNotificationUtils.isNotificationAddSplitButton(n.getPackageName()));
                                Bundle bundle = n.getNotification().extras;
                                str = "topFullscreen";
                                boolean z = !NotificationManagerService.this.isExpandedNtfPkg(n.getPackageName()) && (NotificationManagerService.this.isTopFullscreen() || NotificationManagerService.this.isPackageRequestNarrowNotification());
                                bundle.putBoolean(str, z);
                            }
                            if (notification.extras != null) {
                                boolean isGameDndSwitchOn = false;
                                try {
                                    if (NotificationManagerService.this.mGameDndStatus) {
                                        isGameDndSwitchOn = NotificationManagerService.this.isGameDndSwitchOn();
                                        notification.extras.putBoolean("gameDndSwitchOn", isGameDndSwitchOn);
                                    }
                                    str2 = NotificationManagerService.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("mGameDndStatus is:");
                                    stringBuilder.append(NotificationManagerService.this.mGameDndStatus);
                                    stringBuilder.append(" ,isGameDndSwitchOn is:");
                                    stringBuilder.append(isGameDndSwitchOn);
                                    Log.d(str2, stringBuilder.toString());
                                    notification.extras.putBoolean("gameDndOn", NotificationManagerService.this.mGameDndStatus);
                                } catch (ConcurrentModificationException e) {
                                    String str3 = NotificationManagerService.TAG;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("notification.extras:");
                                    stringBuilder3.append(e.toString());
                                    Log.e(str3, stringBuilder3.toString());
                                } catch (Throwable th2) {
                                    th = th2;
                                    i = 0;
                                    i2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                                    while (true) {
                                        N = i;
                                        if (N >= i2) {
                                            break;
                                        }
                                        if (Objects.equals(this.key, ((NotificationRecord) NotificationManagerService.this.mEnqueuedNotifications.get(N)).getKey())) {
                                            NotificationManagerService.this.mEnqueuedNotifications.remove(N);
                                            break;
                                        }
                                        i = N + 1;
                                    }
                                    throw th;
                                }
                            }
                            String packageName;
                            try {
                                NotificationManagerService.this.mListeners.notifyPostedLocked(r2, old);
                                if (oldSbn != null) {
                                }
                                NotificationManagerService.this.mHandler.post(new Runnable() {
                                    public void run() {
                                        NotificationManagerService.this.mGroupHelper.onNotificationPosted(n, NotificationManagerService.this.hasAutoGroupSummaryLocked(n), notifyType);
                                    }
                                });
                                if (oldSbn == null) {
                                    LogPower.push(122, n.getPackageName(), Integer.toString(n.getId()), n.getOpPkg(), new String[]{Integer.toString(r2.getFlags())});
                                    NotificationManagerService.this.reportToIAware(n.getPackageName(), n.getUid(), n.getId(), true);
                                    r2.setPushLogPowerTimeMs(System.currentTimeMillis());
                                    i4 = N;
                                    i = 0;
                                } else {
                                    long now = System.currentTimeMillis();
                                    if (old.getPushLogPowerTimeMs(now) > NotificationManagerService.NOTIFICATION_UPDATE_REPORT_INTERVAL) {
                                        packageName = n.getPackageName();
                                        String num = Integer.toString(n.getId());
                                        String opPkg = n.getOpPkg();
                                        String[] strArr = new String[1];
                                        i = 0;
                                        strArr[0] = Integer.toString(r2.getFlags());
                                        LogPower.push(HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_MINUS, packageName, num, opPkg, strArr);
                                        r2.setPushLogPowerTimeMs(now);
                                    } else {
                                        i = 0;
                                    }
                                }
                            } catch (IllegalArgumentException e2) {
                                i4 = N;
                                i = 0;
                                packageName = NotificationManagerService.TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("notification:");
                                stringBuilder4.append(n);
                                stringBuilder4.append("extras:");
                                stringBuilder4.append(n.getNotification().extras);
                                Log.e(packageName, stringBuilder4.toString());
                                i2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                                while (true) {
                                    N = i;
                                    if (N >= i2) {
                                        break;
                                    }
                                    if (Objects.equals(this.key, ((NotificationRecord) NotificationManagerService.this.mEnqueuedNotifications.get(N)).getKey())) {
                                        NotificationManagerService.this.mEnqueuedNotifications.remove(N);
                                        break;
                                    }
                                    i = N + 1;
                                }
                                return;
                            } catch (Throwable th3) {
                                th = th3;
                                i2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                                while (true) {
                                    N = i;
                                    if (N >= i2) {
                                    }
                                    i = N + 1;
                                }
                                throw th;
                            }
                        }
                        i = 0;
                        i4 = N;
                        str2 = NotificationManagerService.TAG;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Not posting notification without small icon: ");
                        stringBuilder5.append(notification);
                        Slog.e(str2, stringBuilder5.toString());
                        if (!(old == null || old.isCanceled)) {
                            NotificationManagerService.this.mListeners.notifyRemovedLocked(r2, 4, null);
                            NotificationManagerService.this.mHandler.post(new Runnable() {
                                public void run() {
                                    NotificationManagerService.this.mGroupHelper.onNotificationRemoved(n, notifyType);
                                }
                            });
                        }
                        str2 = NotificationManagerService.TAG;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("WARNING: In a future release this will crash the app: ");
                        stringBuilder5.append(n.getPackageName());
                        Slog.e(str2, stringBuilder5.toString());
                        if (!r2.isHidden()) {
                            NotificationManagerService.this.buzzBeepBlinkLocked(r2);
                        }
                        NotificationManagerService.this.maybeRecordInterruptionLocked(r2);
                        N2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                        while (true) {
                            i2 = i;
                            if (i2 >= N2) {
                                break;
                            }
                            if (Objects.equals(this.key, ((NotificationRecord) NotificationManagerService.this.mEnqueuedNotifications.get(i2)).getKey())) {
                                NotificationManagerService.this.mEnqueuedNotifications.remove(i2);
                                break;
                            }
                            i = i2 + 1;
                        }
                    } else {
                        str2 = NotificationManagerService.TAG;
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("Cannot find enqueued record for key: ");
                        stringBuilder6.append(this.key);
                        Slog.i(str2, stringBuilder6.toString());
                        N2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                        while (i2 < N2) {
                            if (Objects.equals(this.key, ((NotificationRecord) NotificationManagerService.this.mEnqueuedNotifications.get(i2)).getKey())) {
                                NotificationManagerService.this.mEnqueuedNotifications.remove(i2);
                                break;
                            }
                            i2++;
                        }
                    }
                } catch (ConcurrentModificationException e3) {
                    str = NotificationManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ConcurrentModificationException is happen, notification.extras.putBoolean:");
                    stringBuilder.append(e3.toString());
                    Log.e(str, stringBuilder.toString());
                } catch (Throwable th4) {
                    th = th4;
                    i = 0;
                    i2 = NotificationManagerService.this.mEnqueuedNotifications.size();
                    while (true) {
                        N = i;
                        if (N >= i2) {
                        }
                        i = N + 1;
                    }
                    throw th;
                }
            }
        }
    }

    private final class PowerSaverObserver extends ContentObserver {
        private final Uri SUPER_POWER_SAVE_NOTIFICATION_URI = Secure.getUriFor(NotificationManagerService.POWER_SAVER_NOTIFICATION_WHITELIST);
        private boolean initObserver = false;

        PowerSaverObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            if (!this.initObserver) {
                this.initObserver = true;
                NotificationManagerService.this.getContext().getContentResolver().registerContentObserver(this.SUPER_POWER_SAVE_NOTIFICATION_URI, false, this, -1);
                update(null);
            }
        }

        void unObserve() {
            this.initObserver = false;
            NotificationManagerService.this.getContext().getContentResolver().unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (uri == null || this.SUPER_POWER_SAVE_NOTIFICATION_URI.equals(uri)) {
                NotificationManagerService.this.setNotificationWhiteList();
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri NOTIFICATION_BADGING_URI = Secure.getUriFor("notification_badging");
        private final Uri NOTIFICATION_LIGHT_PULSE_URI = System.getUriFor("notification_light_pulse");
        private final Uri NOTIFICATION_RATE_LIMIT_URI = Global.getUriFor("max_notification_enqueue_rate");

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = NotificationManagerService.this.getContext().getContentResolver();
            resolver.registerContentObserver(this.NOTIFICATION_BADGING_URI, false, this, -1);
            resolver.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, false, this, -1);
            resolver.registerContentObserver(this.NOTIFICATION_RATE_LIMIT_URI, false, this, -1);
            update(null);
        }

        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            ContentResolver resolver = NotificationManagerService.this.getContext().getContentResolver();
            if (uri == null || this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                boolean z = false;
                if (System.getIntForUser(resolver, "notification_light_pulse", 0, -2) != 0) {
                    z = true;
                }
                boolean pulseEnabled = z;
                if (NotificationManagerService.this.mNotificationPulseEnabled != pulseEnabled) {
                    NotificationManagerService.this.mNotificationPulseEnabled = pulseEnabled;
                    NotificationManagerService.this.updateNotificationPulse();
                }
            }
            if (uri == null || this.NOTIFICATION_RATE_LIMIT_URI.equals(uri)) {
                NotificationManagerService.this.mMaxPackageEnqueueRate = Global.getFloat(resolver, "max_notification_enqueue_rate", NotificationManagerService.this.mMaxPackageEnqueueRate);
            }
            if (uri == null || this.NOTIFICATION_BADGING_URI.equals(uri)) {
                NotificationManagerService.this.mRankingHelper.updateBadgingEnabled();
            }
        }
    }

    private class ShellCmd extends ShellCommand {
        public static final String USAGE = "help\nallow_listener COMPONENT [user_id]\ndisallow_listener COMPONENT [user_id]\nallow_assistant COMPONENT\nremove_assistant COMPONENT\nallow_dnd PACKAGE\ndisallow_dnd PACKAGE\nsuspend_package PACKAGE\nunsuspend_package PACKAGE";

        private ShellCmd() {
        }

        /* synthetic */ ShellCmd(NotificationManagerService x0, AnonymousClass1 x1) {
            this();
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int onCommand(String cmd) {
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            try {
                boolean z;
                switch (cmd.hashCode()) {
                    case -1325770982:
                        if (cmd.equals("disallow_assistant")) {
                            z = true;
                            break;
                        }
                    case -506770550:
                        if (cmd.equals("unsuspend_package")) {
                            z = true;
                            break;
                        }
                    case -432999190:
                        if (cmd.equals("allow_listener")) {
                            z = true;
                            break;
                        }
                    case -429832618:
                        if (cmd.equals("disallow_dnd")) {
                            z = true;
                            break;
                        }
                    case 372345636:
                        if (cmd.equals("allow_dnd")) {
                            z = false;
                            break;
                        }
                    case 393969475:
                        if (cmd.equals("suspend_package")) {
                            z = true;
                            break;
                        }
                    case 1257269496:
                        if (cmd.equals("disallow_listener")) {
                            z = true;
                            break;
                        }
                    case 2110474600:
                        if (cmd.equals("allow_assistant")) {
                            z = true;
                            break;
                        }
                    default:
                        z = true;
                        break;
                }
                ComponentName cn;
                String userId;
                switch (z) {
                    case false:
                        NotificationManagerService.this.getBinderService().setNotificationPolicyAccessGranted(getNextArgRequired(), true);
                        break;
                    case true:
                        NotificationManagerService.this.getBinderService().setNotificationPolicyAccessGranted(getNextArgRequired(), false);
                        break;
                    case true:
                        cn = ComponentName.unflattenFromString(getNextArgRequired());
                        if (cn != null) {
                            userId = getNextArg();
                            if (userId != null) {
                                NotificationManagerService.this.getBinderService().setNotificationListenerAccessGrantedForUser(cn, Integer.parseInt(userId), true);
                                break;
                            }
                            NotificationManagerService.this.getBinderService().setNotificationListenerAccessGranted(cn, true);
                            break;
                        }
                        pw.println("Invalid listener - must be a ComponentName");
                        return -1;
                    case true:
                        cn = ComponentName.unflattenFromString(getNextArgRequired());
                        if (cn != null) {
                            userId = getNextArg();
                            if (userId != null) {
                                NotificationManagerService.this.getBinderService().setNotificationListenerAccessGrantedForUser(cn, Integer.parseInt(userId), false);
                                break;
                            }
                            NotificationManagerService.this.getBinderService().setNotificationListenerAccessGranted(cn, false);
                            break;
                        }
                        pw.println("Invalid listener - must be a ComponentName");
                        return -1;
                    case true:
                        cn = ComponentName.unflattenFromString(getNextArgRequired());
                        if (cn != null) {
                            NotificationManagerService.this.getBinderService().setNotificationAssistantAccessGranted(cn, true);
                            break;
                        }
                        pw.println("Invalid assistant - must be a ComponentName");
                        return -1;
                    case true:
                        cn = ComponentName.unflattenFromString(getNextArgRequired());
                        if (cn != null) {
                            NotificationManagerService.this.getBinderService().setNotificationAssistantAccessGranted(cn, false);
                            break;
                        }
                        pw.println("Invalid assistant - must be a ComponentName");
                        return -1;
                    case true:
                        NotificationManagerService.this.simulatePackageSuspendBroadcast(true, getNextArgRequired());
                        break;
                    case true:
                        NotificationManagerService.this.simulatePackageSuspendBroadcast(false, getNextArgRequired());
                        break;
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error occurred. Check logcat for details. ");
                stringBuilder.append(e.getMessage());
                pw.println(stringBuilder.toString());
                Slog.e(NotificationManagerService.TAG, "Error running shell command", e);
            }
            return 0;
        }

        public void onHelp() {
            getOutPrintWriter().println(USAGE);
        }
    }

    protected class SnoozeNotificationRunnable implements Runnable {
        private final long mDuration;
        private final String mKey;
        private final String mSnoozeCriterionId;

        SnoozeNotificationRunnable(String key, long duration, String snoozeCriterionId) {
            this.mKey = key;
            this.mDuration = duration;
            this.mSnoozeCriterionId = snoozeCriterionId;
        }

        public void run() {
            synchronized (NotificationManagerService.this.mNotificationLock) {
                NotificationRecord r = NotificationManagerService.this.findNotificationByKeyLocked(this.mKey);
                if (r != null) {
                    snoozeLocked(r);
                }
            }
        }

        @GuardedBy("mNotificationLock")
        void snoozeLocked(NotificationRecord r) {
            if (r.sbn.isGroup()) {
                List<NotificationRecord> groupNotifications = NotificationManagerService.this.findGroupNotificationsLocked(r.sbn.getPackageName(), r.sbn.getGroupKey(), r.sbn.getUserId());
                int i = 0;
                int i2;
                if (r.getNotification().isGroupSummary()) {
                    while (true) {
                        i2 = i;
                        if (i2 < groupNotifications.size()) {
                            snoozeNotificationLocked((NotificationRecord) groupNotifications.get(i2));
                            i = i2 + 1;
                        } else {
                            return;
                        }
                    }
                } else if (!NotificationManagerService.this.mSummaryByGroupKey.containsKey(r.sbn.getGroupKey())) {
                    snoozeNotificationLocked(r);
                    return;
                } else if (groupNotifications.size() != 2) {
                    snoozeNotificationLocked(r);
                    return;
                } else {
                    while (true) {
                        i2 = i;
                        if (i2 < groupNotifications.size()) {
                            snoozeNotificationLocked((NotificationRecord) groupNotifications.get(i2));
                            i = i2 + 1;
                        } else {
                            return;
                        }
                    }
                }
            }
            snoozeNotificationLocked(r);
        }

        @GuardedBy("mNotificationLock")
        void snoozeNotificationLocked(NotificationRecord r) {
            MetricsLogger.action(r.getLogMaker().setCategory(831).setType(2).addTaggedData(1139, Long.valueOf(this.mDuration)).addTaggedData(832, Integer.valueOf(this.mSnoozeCriterionId == null ? 0 : 1)));
            NotificationManagerService.this.cancelNotificationLocked(r, false, 18, NotificationManagerService.this.removeFromNotificationListsLocked(r), null);
            NotificationManagerService.this.updateLightsLocked();
            if (this.mSnoozeCriterionId != null) {
                NotificationManagerService.this.mAssistants.notifyAssistantSnoozedLocked(r.sbn, this.mSnoozeCriterionId);
                NotificationManagerService.this.mSnoozeHelper.snooze(r);
            } else {
                NotificationManagerService.this.mSnoozeHelper.snooze(r, this.mDuration);
            }
            r.recordSnoozed();
            NotificationManagerService.this.savePolicyFile();
        }
    }

    private static final class StatusBarNotificationHolder extends IStatusBarNotificationHolder.Stub {
        private StatusBarNotification mValue;

        public StatusBarNotificationHolder(StatusBarNotification value) {
            this.mValue = value;
        }

        public StatusBarNotification get() {
            StatusBarNotification value = this.mValue;
            this.mValue = null;
            return value;
        }
    }

    private static class ToastRecord {
        ITransientNotification callback;
        int duration;
        final int pid;
        final String pkg;
        Binder token;

        ToastRecord(int pid, String pkg, ITransientNotification callback, int duration, Binder token) {
            this.pid = pid;
            this.pkg = pkg;
            this.callback = callback;
            this.duration = duration;
            this.token = token;
        }

        void update(int duration) {
            this.duration = duration;
        }

        void update(ITransientNotification callback) {
            this.callback = callback;
        }

        void dump(PrintWriter pw, String prefix, DumpFilter filter) {
            if (filter == null || filter.matches(this.pkg)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append(this);
                pw.println(stringBuilder.toString());
            }
        }

        public final String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ToastRecord{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" pkg=");
            stringBuilder.append(this.pkg);
            stringBuilder.append(" callback=");
            stringBuilder.append(this.callback);
            stringBuilder.append(" duration=");
            stringBuilder.append(this.duration);
            return stringBuilder.toString();
        }
    }

    private class TrimCache {
        StatusBarNotification heavy;
        StatusBarNotification sbnClone;
        StatusBarNotification sbnCloneLight;

        TrimCache(StatusBarNotification sbn) {
            this.heavy = sbn;
        }

        StatusBarNotification ForListener(ManagedServiceInfo info) {
            if (NotificationManagerService.this.mListeners.getOnNotificationPostedTrim(info) == 1) {
                if (this.sbnCloneLight == null) {
                    this.sbnCloneLight = this.heavy.cloneLight();
                }
                return this.sbnCloneLight;
            }
            if (this.sbnClone == null) {
                this.sbnClone = this.heavy.clone();
            }
            return this.sbnClone;
        }
    }

    protected class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    NotificationManagerService.this.handleDurationReached((ToastRecord) msg.obj);
                    return;
                case 3:
                    new Thread() {
                        public void run() {
                            NotificationManagerService.this.handleSavePolicyFile();
                        }
                    }.start();
                    return;
                case 4:
                    NotificationManagerService.this.handleSendRankingUpdate();
                    return;
                case 5:
                    NotificationManagerService.this.handleListenerHintsChanged(msg.arg1);
                    return;
                case 6:
                    NotificationManagerService.this.handleListenerInterruptionFilterChanged(msg.arg1);
                    return;
                case 7:
                    NotificationManagerService.this.handleKillTokenTimeout((IBinder) msg.obj);
                    return;
                default:
                    return;
            }
        }

        protected void scheduleSendRankingUpdate() {
            if (!hasMessages(4)) {
                sendMessage(Message.obtain(this, 4));
            }
        }
    }

    public class NotificationAssistants extends ManagedServices {
        static final String TAG_ENABLED_NOTIFICATION_ASSISTANTS = "enabled_assistants";

        public NotificationAssistants(Context context, Object lock, UserProfiles up, IPackageManager pm) {
            super(context, lock, up, pm);
        }

        protected Config getConfig() {
            Config c = new Config();
            c.caption = "notification assistant";
            c.serviceInterface = "android.service.notification.NotificationAssistantService";
            c.xmlTag = TAG_ENABLED_NOTIFICATION_ASSISTANTS;
            c.secureSettingName = "enabled_notification_assistant";
            c.bindPermission = "android.permission.BIND_NOTIFICATION_ASSISTANT_SERVICE";
            c.settingsAction = "android.settings.MANAGE_DEFAULT_APPS_SETTINGS";
            c.clientLabel = 17040605;
            return c;
        }

        protected IInterface asInterface(IBinder binder) {
            return INotificationListener.Stub.asInterface(binder);
        }

        protected boolean checkType(IInterface service) {
            return service instanceof INotificationListener;
        }

        protected void onServiceAdded(ManagedServiceInfo info) {
            NotificationManagerService.this.mListeners.registerGuestService(info);
        }

        @GuardedBy("mNotificationLock")
        protected void onServiceRemovedLocked(ManagedServiceInfo removed) {
            NotificationManagerService.this.mListeners.unregisterService(removed.service, removed.userid);
        }

        public void onUserUnlocked(int user) {
            if (this.DEBUG) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onUserUnlocked u=");
                stringBuilder.append(user);
                Slog.d(str, stringBuilder.toString());
            }
            rebindServices(true);
        }

        public void onNotificationEnqueued(NotificationRecord r) {
            StatusBarNotification sbn = r.sbn;
            TrimCache trimCache = new TrimCache(sbn);
            for (final ManagedServiceInfo info : getServices()) {
                if (NotificationManagerService.this.isVisibleToListener(sbn, info)) {
                    final StatusBarNotification sbnToPost = trimCache.ForListener(info);
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        public void run() {
                            NotificationAssistants.this.notifyEnqueued(info, sbnToPost);
                        }
                    });
                }
            }
        }

        private void notifyEnqueued(ManagedServiceInfo info, StatusBarNotification sbn) {
            INotificationListener assistant = info.service;
            try {
                assistant.onNotificationEnqueued(new StatusBarNotificationHolder(sbn));
            } catch (RemoteException ex) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to notify assistant (enqueued): ");
                stringBuilder.append(assistant);
                Log.e(str, stringBuilder.toString(), ex);
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyAssistantSnoozedLocked(StatusBarNotification sbn, final String snoozeCriterionId) {
            TrimCache trimCache = new TrimCache(sbn);
            for (final ManagedServiceInfo info : getServices()) {
                if (NotificationManagerService.this.isVisibleToListener(sbn, info)) {
                    final StatusBarNotification sbnToPost = trimCache.ForListener(info);
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        public void run() {
                            INotificationListener assistant = info.service;
                            try {
                                assistant.onNotificationSnoozedUntilContext(new StatusBarNotificationHolder(sbnToPost), snoozeCriterionId);
                            } catch (RemoteException ex) {
                                String str = NotificationAssistants.this.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("unable to notify assistant (snoozed): ");
                                stringBuilder.append(assistant);
                                Log.e(str, stringBuilder.toString(), ex);
                            }
                        }
                    });
                }
            }
        }

        public boolean isEnabled() {
            return getServices().isEmpty() ^ 1;
        }

        protected void ensureAssistant() {
            for (UserInfo userInfo : this.mUm.getUsers(true)) {
                int userId = userInfo.getUserHandle().getIdentifier();
                if (getAllowedPackages(userId).isEmpty()) {
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Approving default notification assistant for user ");
                    stringBuilder.append(userId);
                    Slog.d(str, stringBuilder.toString());
                    NotificationManagerService.this.readDefaultAssistant(userId);
                }
            }
        }
    }

    public class NotificationListeners extends ManagedServices {
        static final String TAG_ENABLED_NOTIFICATION_LISTENERS = "enabled_listeners";
        private final ArraySet<ManagedServiceInfo> mLightTrimListeners = new ArraySet();

        public NotificationListeners(IPackageManager pm) {
            super(NotificationManagerService.this.getContext(), NotificationManagerService.this.mNotificationLock, NotificationManagerService.this.mUserProfiles, pm);
        }

        protected Config getConfig() {
            Config c = new Config();
            c.caption = "notification listener";
            c.serviceInterface = "android.service.notification.NotificationListenerService";
            c.xmlTag = TAG_ENABLED_NOTIFICATION_LISTENERS;
            c.secureSettingName = "enabled_notification_listeners";
            c.bindPermission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE";
            c.settingsAction = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
            c.clientLabel = 17040603;
            return c;
        }

        protected IInterface asInterface(IBinder binder) {
            return INotificationListener.Stub.asInterface(binder);
        }

        protected boolean checkType(IInterface service) {
            return service instanceof INotificationListener;
        }

        public void onServiceAdded(ManagedServiceInfo info) {
            NotificationRankingUpdate update;
            INotificationListener listener = info.service;
            synchronized (NotificationManagerService.this.mNotificationLock) {
                update = NotificationManagerService.this.makeRankingUpdateLocked(info);
            }
            try {
                listener.onListenerConnected(update);
            } catch (RemoteException e) {
            }
        }

        @GuardedBy("mNotificationLock")
        protected void onServiceRemovedLocked(ManagedServiceInfo removed) {
            if (NotificationManagerService.this.removeDisabledHints(removed)) {
                NotificationManagerService.this.updateListenerHintsLocked();
                NotificationManagerService.this.updateEffectsSuppressorLocked();
            }
            this.mLightTrimListeners.remove(removed);
        }

        @GuardedBy("mNotificationLock")
        public void setOnNotificationPostedTrimLocked(ManagedServiceInfo info, int trim) {
            if (trim == 1) {
                this.mLightTrimListeners.add(info);
            } else {
                this.mLightTrimListeners.remove(info);
            }
        }

        public int getOnNotificationPostedTrim(ManagedServiceInfo info) {
            return this.mLightTrimListeners.contains(info);
        }

        @GuardedBy("mNotificationLock")
        public void notifyPostedLocked(NotificationRecord r, NotificationRecord old) {
            notifyPostedLocked(r, old, true);
        }

        @GuardedBy("mNotificationLock")
        private void notifyPostedLocked(NotificationRecord r, NotificationRecord old, boolean notifyAllListeners) {
            StatusBarNotification sbn = r.sbn;
            StatusBarNotification oldSbn = old != null ? old.sbn : null;
            TrimCache trimCache = new TrimCache(sbn);
            for (final ManagedServiceInfo info : getServices()) {
                boolean sbnVisible = NotificationManagerService.this.isVisibleToListener(sbn, info);
                int targetUserId = 0;
                boolean oldSbnVisible = oldSbn != null ? NotificationManagerService.this.isVisibleToListener(oldSbn, info) : false;
                if (oldSbnVisible || sbnVisible) {
                    if (!r.isHidden() || info.targetSdkVersion >= 28) {
                        if (notifyAllListeners || info.targetSdkVersion < 28) {
                            final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                            if (!oldSbnVisible || sbnVisible) {
                                if (info.userid != -1) {
                                    targetUserId = info.userid;
                                }
                                NotificationManagerService.this.updateUriPermissions(r, old, info.component.getPackageName(), targetUserId);
                                final StatusBarNotification sbnToPost = trimCache.ForListener(info);
                                NotificationManagerService.this.mHandler.post(new Runnable() {
                                    public void run() {
                                        NotificationListeners.this.notifyPosted(info, sbnToPost, update);
                                    }
                                });
                            } else {
                                final StatusBarNotification oldSbnLightClone = oldSbn.cloneLight();
                                NotificationManagerService.this.mHandler.post(new Runnable() {
                                    public void run() {
                                        NotificationListeners.this.notifyRemoved(info, oldSbnLightClone, update, null, 6);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyRemovedLocked(NotificationRecord r, int reason, NotificationStats notificationStats) {
            NotificationRecord notificationRecord = r;
            int i = reason;
            StatusBarNotification sbn = notificationRecord.sbn;
            StatusBarNotification sbnLight = sbn.cloneLight();
            for (ManagedServiceInfo info : getServices()) {
                if (NotificationManagerService.this.isVisibleToListener(sbn, info)) {
                    if (!r.isHidden() || i == 14 || info.targetSdkVersion >= 28) {
                        if (i != 14 || info.targetSdkVersion < 28) {
                            final NotificationStats stats = NotificationManagerService.this.mAssistants.isServiceTokenValidLocked(info.service) ? notificationStats : null;
                            NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(info);
                            WorkerHandler access$2300 = NotificationManagerService.this.mHandler;
                            final ManagedServiceInfo managedServiceInfo = info;
                            final StatusBarNotification statusBarNotification = sbnLight;
                            final NotificationRankingUpdate notificationRankingUpdate = update;
                            StatusBarNotification sbn2 = sbn;
                            AnonymousClass3 anonymousClass3 = r0;
                            final int i2 = i;
                            AnonymousClass3 anonymousClass32 = new Runnable() {
                                public void run() {
                                    NotificationListeners.this.notifyRemoved(managedServiceInfo, statusBarNotification, notificationRankingUpdate, stats, i2);
                                }
                            };
                            access$2300.post(anonymousClass3);
                            sbn = sbn2;
                        }
                    }
                }
            }
            NotificationManagerService.this.mHandler.post(new -$$Lambda$NotificationManagerService$NotificationListeners$Zsu32u2gOsCsEatsnnXTLoY37u0(this, notificationRecord));
        }

        @GuardedBy("mNotificationLock")
        public void notifyRankingUpdateLocked(List<NotificationRecord> changedHiddenNotifications) {
            boolean isHiddenRankingUpdate = changedHiddenNotifications != null && changedHiddenNotifications.size() > 0;
            for (final ManagedServiceInfo serviceInfo : getServices()) {
                if (serviceInfo.isEnabledForCurrentProfiles()) {
                    boolean notifyThisListener = false;
                    if (isHiddenRankingUpdate && serviceInfo.targetSdkVersion >= 28) {
                        for (NotificationRecord rec : changedHiddenNotifications) {
                            if (NotificationManagerService.this.isVisibleToListener(rec.sbn, serviceInfo)) {
                                notifyThisListener = true;
                                break;
                            }
                        }
                    }
                    if (notifyThisListener || !isHiddenRankingUpdate) {
                        final NotificationRankingUpdate update = NotificationManagerService.this.makeRankingUpdateLocked(serviceInfo);
                        NotificationManagerService.this.mHandler.post(new Runnable() {
                            public void run() {
                                NotificationListeners.this.notifyRankingUpdate(serviceInfo, update);
                            }
                        });
                    }
                }
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyListenerHintsChangedLocked(final int hints) {
            for (final ManagedServiceInfo serviceInfo : getServices()) {
                if (serviceInfo.isEnabledForCurrentProfiles()) {
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        public void run() {
                            NotificationListeners.this.notifyListenerHintsChanged(serviceInfo, hints);
                        }
                    });
                }
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyHiddenLocked(List<NotificationRecord> changedNotifications) {
            if (changedNotifications != null && changedNotifications.size() != 0) {
                notifyRankingUpdateLocked(changedNotifications);
                int numChangedNotifications = changedNotifications.size();
                for (int i = 0; i < numChangedNotifications; i++) {
                    NotificationRecord rec = (NotificationRecord) changedNotifications.get(i);
                    NotificationManagerService.this.mListeners.notifyRemovedLocked(rec, 14, rec.getStats());
                }
            }
        }

        @GuardedBy("mNotificationLock")
        public void notifyUnhiddenLocked(List<NotificationRecord> changedNotifications) {
            if (changedNotifications != null && changedNotifications.size() != 0) {
                notifyRankingUpdateLocked(changedNotifications);
                int numChangedNotifications = changedNotifications.size();
                for (int i = 0; i < numChangedNotifications; i++) {
                    NotificationRecord rec = (NotificationRecord) changedNotifications.get(i);
                    NotificationManagerService.this.mListeners.notifyPostedLocked(rec, rec, false);
                }
            }
        }

        public void notifyInterruptionFilterChanged(final int interruptionFilter) {
            for (final ManagedServiceInfo serviceInfo : getServices()) {
                if (serviceInfo.isEnabledForCurrentProfiles()) {
                    NotificationManagerService.this.mHandler.post(new Runnable() {
                        public void run() {
                            NotificationListeners.this.notifyInterruptionFilterChanged(serviceInfo, interruptionFilter);
                        }
                    });
                }
            }
        }

        protected void notifyNotificationChannelChanged(String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
            if (channel != null) {
                for (ManagedServiceInfo serviceInfo : getServices()) {
                    if (serviceInfo.enabledAndUserMatches(UserHandle.getCallingUserId())) {
                        BackgroundThread.getHandler().post(new -$$Lambda$NotificationManagerService$NotificationListeners$E8qsF-PrFYYUtUGked50-pRub20(this, serviceInfo, pkg, user, channel, modificationType));
                    }
                }
            }
        }

        public static /* synthetic */ void lambda$notifyNotificationChannelChanged$1(NotificationListeners notificationListeners, ManagedServiceInfo serviceInfo, String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
            if (NotificationManagerService.this.hasCompanionDevice(serviceInfo) || serviceInfo.component.toString().contains(NotificationManagerService.HWSYSTEMMANAGER_PKG)) {
                notificationListeners.notifyNotificationChannelChanged(serviceInfo, pkg, user, channel, modificationType);
            }
        }

        protected void notifyNotificationChannelGroupChanged(String pkg, UserHandle user, NotificationChannelGroup group, int modificationType) {
            if (group != null) {
                for (ManagedServiceInfo serviceInfo : getServices()) {
                    if (serviceInfo.enabledAndUserMatches(UserHandle.getCallingUserId())) {
                        BackgroundThread.getHandler().post(new -$$Lambda$NotificationManagerService$NotificationListeners$ZpwYxOiDD13VBHvGZVH3p7iGkFI(this, serviceInfo, pkg, user, group, modificationType));
                    }
                }
            }
        }

        public static /* synthetic */ void lambda$notifyNotificationChannelGroupChanged$2(NotificationListeners notificationListeners, ManagedServiceInfo serviceInfo, String pkg, UserHandle user, NotificationChannelGroup group, int modificationType) {
            if (NotificationManagerService.this.hasCompanionDevice(serviceInfo)) {
                notificationListeners.notifyNotificationChannelGroupChanged(serviceInfo, pkg, user, group, modificationType);
            }
        }

        private void notifyPosted(ManagedServiceInfo info, StatusBarNotification sbn, NotificationRankingUpdate rankingUpdate) {
            INotificationListener listener = info.service;
            try {
                listener.onNotificationPosted(new StatusBarNotificationHolder(sbn), rankingUpdate);
            } catch (RemoteException ex) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to notify listener (posted): ");
                stringBuilder.append(listener);
                Log.e(str, stringBuilder.toString(), ex);
            }
        }

        private void notifyRemoved(ManagedServiceInfo info, StatusBarNotification sbn, NotificationRankingUpdate rankingUpdate, NotificationStats stats, int reason) {
            if (info.enabledAndUserMatches(sbn.getUserId())) {
                INotificationListener listener = info.service;
                try {
                    listener.onNotificationRemoved(new StatusBarNotificationHolder(sbn), rankingUpdate, stats, reason);
                    LogPower.push(123, sbn.getPackageName(), Integer.toString(sbn.getId()), sbn.getOpPkg(), new String[]{Integer.toString(sbn.getNotification().flags)});
                    NotificationManagerService.this.reportToIAware(sbn.getPackageName(), sbn.getUid(), sbn.getId(), false);
                } catch (RemoteException ex) {
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to notify listener (removed): ");
                    stringBuilder.append(listener);
                    Log.e(str, stringBuilder.toString(), ex);
                }
            }
        }

        private void notifyRankingUpdate(ManagedServiceInfo info, NotificationRankingUpdate rankingUpdate) {
            INotificationListener listener = info.service;
            try {
                listener.onNotificationRankingUpdate(rankingUpdate);
            } catch (RemoteException ex) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to notify listener (ranking update): ");
                stringBuilder.append(listener);
                Log.e(str, stringBuilder.toString(), ex);
            }
        }

        private void notifyListenerHintsChanged(ManagedServiceInfo info, int hints) {
            INotificationListener listener = info.service;
            try {
                listener.onListenerHintsChanged(hints);
            } catch (RemoteException ex) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to notify listener (listener hints): ");
                stringBuilder.append(listener);
                Log.e(str, stringBuilder.toString(), ex);
            }
        }

        private void notifyInterruptionFilterChanged(ManagedServiceInfo info, int interruptionFilter) {
            INotificationListener listener = info.service;
            try {
                listener.onInterruptionFilterChanged(interruptionFilter);
            } catch (RemoteException ex) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to notify listener (interruption filter): ");
                stringBuilder.append(listener);
                Log.e(str, stringBuilder.toString(), ex);
            }
        }

        void notifyNotificationChannelChanged(ManagedServiceInfo info, String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
            INotificationListener listener = info.service;
            try {
                listener.onNotificationChannelModification(pkg, user, channel, modificationType);
            } catch (RemoteException ex) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to notify listener (channel changed): ");
                stringBuilder.append(listener);
                Log.e(str, stringBuilder.toString(), ex);
            }
        }

        private void notifyNotificationChannelGroupChanged(ManagedServiceInfo info, String pkg, UserHandle user, NotificationChannelGroup group, int modificationType) {
            INotificationListener listener = info.service;
            try {
                listener.onNotificationChannelGroupModification(pkg, user, group, modificationType);
            } catch (RemoteException ex) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to notify listener (channel group changed): ");
                stringBuilder.append(listener);
                Log.e(str, stringBuilder.toString(), ex);
            }
        }

        public boolean isListenerPackage(String packageName) {
            if (packageName == null) {
                return false;
            }
            synchronized (NotificationManagerService.this.mNotificationLock) {
                for (ManagedServiceInfo serviceInfo : getServices()) {
                    if (packageName.equals(serviceInfo.component.getPackageName())) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private final class RankingHandlerWorker extends Handler implements RankingHandler {
        public RankingHandlerWorker(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    NotificationManagerService.this.handleRankingReconsideration(msg);
                    return;
                case 1001:
                    NotificationManagerService.this.handleRankingSort();
                    return;
                default:
                    return;
            }
        }

        public void requestSort() {
            removeMessages(1001);
            Message msg = Message.obtain();
            msg.what = 1001;
            sendMessage(msg);
        }

        public void requestReconsideration(RankingReconsideration recon) {
            sendMessageDelayed(Message.obtain(this, 1000, recon), recon.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    private static class ToastRecordEx extends ToastRecord {
        int displayId;

        ToastRecordEx(int pid, String pkg, ITransientNotification callback, int duration, Binder token, int displayId) {
            super(pid, pkg, callback, duration, token);
            this.displayId = displayId;
        }
    }

    static {
        boolean z = true;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(NotificationManagerService.class.getSimpleName());
        stringBuilder.append(".TIMEOUT");
        ACTION_NOTIFICATION_TIMEOUT = stringBuilder.toString();
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
        EXPANDEDNTF_PKGS.add("com.android.incallui");
        EXPANDEDNTF_PKGS.add("com.android.deskclock");
    }

    protected void readDefaultApprovedServices(int userId) {
        int length;
        String defaultListenerAccess = getContext().getResources().getString(17039786);
        int i = 0;
        if (defaultListenerAccess != null) {
            for (String whitelisted : defaultListenerAccess.split(":")) {
                for (ComponentName cn : this.mListeners.queryPackageForServices(whitelisted, 786432, userId)) {
                    try {
                        getBinderService().setNotificationListenerAccessGrantedForUser(cn, userId, true);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        String defaultDndAccess = getContext().getResources().getString(17039785);
        if (defaultListenerAccess != null) {
            String[] split = defaultDndAccess.split(":");
            length = split.length;
            while (i < length) {
                try {
                    getBinderService().setNotificationPolicyAccessGranted(split[i], true);
                } catch (RemoteException e2) {
                    e2.printStackTrace();
                }
                i++;
            }
        }
        readDefaultAssistant(userId);
    }

    protected void readDefaultAssistant(int userId) {
        String defaultAssistantAccess = getContext().getResources().getString(17039782);
        if (defaultAssistantAccess != null) {
            for (ComponentName cn : this.mAssistants.queryPackageForServices(defaultAssistantAccess, 786432, userId)) {
                try {
                    getBinderService().setNotificationAssistantAccessGrantedForUser(cn, userId, true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void readPolicyXml(InputStream stream, boolean forRestore) throws XmlPullParserException, NumberFormatException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(stream, StandardCharsets.UTF_8.name());
        XmlUtils.beginDocument(parser, TAG_NOTIFICATION_POLICY);
        boolean migratedManagedServices = false;
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("zen".equals(parser.getName())) {
                this.mZenModeHelper.readXml(parser, forRestore);
            } else if ("ranking".equals(parser.getName())) {
                this.mRankingHelper.readXml(parser, forRestore);
            }
            if (this.mListeners.getConfig().xmlTag.equals(parser.getName())) {
                this.mListeners.readXml(parser, this.mAllowedManagedServicePackages);
                migratedManagedServices = true;
            } else if (this.mAssistants.getConfig().xmlTag.equals(parser.getName())) {
                this.mAssistants.readXml(parser, this.mAllowedManagedServicePackages);
                migratedManagedServices = true;
            } else if (this.mConditionProviders.getConfig().xmlTag.equals(parser.getName())) {
                this.mConditionProviders.readXml(parser, this.mAllowedManagedServicePackages);
                migratedManagedServices = true;
            }
        }
        if (!migratedManagedServices) {
            this.mListeners.migrateToXml();
            this.mAssistants.migrateToXml();
            this.mConditionProviders.migrateToXml();
            savePolicyFile();
        }
        this.mAssistants.ensureAssistant();
    }

    private void loadPolicyFile() {
        if (DBG) {
            Slog.d(TAG, "loadPolicyFile");
        }
        synchronized (this.mPolicyFile) {
            InputStream infile = null;
            try {
                infile = this.mPolicyFile.openRead();
                readPolicyXml(infile, false);
            } catch (FileNotFoundException e) {
                readDefaultApprovedServices(0);
            } catch (IOException e2) {
                Log.wtf(TAG, "Unable to read notification policy", e2);
                IoUtils.closeQuietly(infile);
            } catch (NumberFormatException e3) {
                Log.wtf(TAG, "Unable to parse notification policy", e3);
                IoUtils.closeQuietly(infile);
            } catch (XmlPullParserException e4) {
                try {
                    Log.wtf(TAG, "Unable to parse notification policy", e4);
                    IoUtils.closeQuietly(infile);
                } catch (Throwable th) {
                    IoUtils.closeQuietly(infile);
                }
            }
            IoUtils.closeQuietly(infile);
        }
        return;
    }

    public void savePolicyFile() {
        this.mHandler.removeMessages(3);
        this.mHandler.sendEmptyMessage(3);
    }

    private void handleSavePolicyFile() {
        if (DBG) {
            Slog.d(TAG, "handleSavePolicyFile");
        }
        synchronized (this.mPolicyFile) {
            try {
                FileOutputStream stream = this.mPolicyFile.startWrite();
                try {
                    writePolicyXml(stream, false);
                    this.mPolicyFile.finishWrite(stream);
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to save policy file, restoring backup", e);
                    this.mPolicyFile.failWrite(stream);
                } catch (ArrayIndexOutOfBoundsException e2) {
                    Slog.e(TAG, "handleSavePolicyFile has Exception : ArrayIndexOutOfBoundsException");
                } catch (NullPointerException e3) {
                    Slog.e(TAG, "writePolicyXml has Exception : NullPointerException");
                }
            } catch (IOException e4) {
                Slog.w(TAG, "Failed to save policy file", e4);
                return;
            }
        }
        BackupManager.dataChanged(getContext().getPackageName());
    }

    private void writePolicyXml(OutputStream stream, boolean forBackup) throws IOException {
        XmlSerializer out = new FastXmlSerializer();
        out.setOutput(stream, StandardCharsets.UTF_8.name());
        out.startDocument(null, Boolean.valueOf(true));
        out.startTag(null, TAG_NOTIFICATION_POLICY);
        out.attribute(null, ATTR_VERSION, Integer.toString(1));
        this.mZenModeHelper.writeXml(out, forBackup, null);
        this.mRankingHelper.writeXml(out, forBackup);
        this.mListeners.writeXml(out, forBackup);
        this.mAssistants.writeXml(out, forBackup);
        this.mConditionProviders.writeXml(out, forBackup);
        out.endTag(null, TAG_NOTIFICATION_POLICY);
        out.endDocument();
    }

    @GuardedBy("mNotificationLock")
    private void clearSoundLocked() {
        this.mSoundNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
            if (player != null) {
                player.stopAsync();
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
        Binder.restoreCallingIdentity(identity);
    }

    @GuardedBy("mNotificationLock")
    private void clearVibrateLocked() {
        this.mVibrateNotificationKey = null;
        long identity = Binder.clearCallingIdentity();
        try {
            this.mVibrator.cancel();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @GuardedBy("mNotificationLock")
    private void clearLightsLocked() {
        long token = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            int i = this.mLights.size();
            while (i > 0) {
                i--;
                String owner = (String) this.mLights.get(i);
                NotificationRecord ledNotification = (NotificationRecord) this.mNotificationsByKey.get(owner);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("clearEffects :");
                stringBuilder.append(owner);
                Slog.d(str, stringBuilder.toString());
                if (ledNotification == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("LED Notification does not exist: ");
                    stringBuilder.append(owner);
                    Slog.wtfStack(str, stringBuilder.toString());
                    this.mLights.remove(owner);
                } else if (ledNotification.getUser().getIdentifier() == currentUser || ledNotification.getUser().getIdentifier() == -1 || isAFWUserId(ledNotification.getUser().getIdentifier())) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("clearEffects CurrentUser AFWuser or AllUser :");
                    stringBuilder.append(owner);
                    Slog.d(str, stringBuilder.toString());
                    this.mLights.remove(owner);
                }
            }
            updateLightsLocked();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    static long[] getLongArray(Resources r, int resid, int maxlen, long[] def) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return def;
        }
        int len = ar.length > maxlen ? maxlen : ar.length;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) {
            out[i] = (long) ar[i];
        }
        return out;
    }

    public NotificationManagerService(Context context) {
        super(context);
        Notification.processWhitelistToken = WHITELIST_TOKEN;
    }

    @VisibleForTesting
    void setAudioManager(AudioManager audioMananger) {
        this.mAudioManager = audioMananger;
    }

    @VisibleForTesting
    void setVibrator(Vibrator vibrator) {
        this.mVibrator = vibrator;
    }

    @VisibleForTesting
    void setLights(Light light) {
        this.mNotificationLight = light;
        this.mAttentionLight = light;
        this.mNotificationPulseEnabled = true;
    }

    @VisibleForTesting
    void setScreenOn(boolean on) {
        this.mScreenOn = on;
    }

    @VisibleForTesting
    int getNotificationRecordCount() {
        int count;
        synchronized (this.mNotificationLock) {
            count = ((this.mNotificationList.size() + this.mNotificationsByKey.size()) + this.mSummaryByGroupKey.size()) + this.mEnqueuedNotifications.size();
            Iterator it = this.mNotificationList.iterator();
            while (it.hasNext()) {
                NotificationRecord posted = (NotificationRecord) it.next();
                if (this.mNotificationsByKey.containsKey(posted.getKey())) {
                    count--;
                }
                if (posted.sbn.isGroup() && posted.getNotification().isGroupSummary()) {
                    count--;
                }
            }
        }
        return count;
    }

    @VisibleForTesting
    void clearNotifications() {
        this.mEnqueuedNotifications.clear();
        this.mNotificationList.clear();
        this.mNotificationsByKey.clear();
        this.mSummaryByGroupKey.clear();
    }

    @VisibleForTesting
    void addNotification(NotificationRecord r) {
        this.mNotificationList.add(r);
        this.mNotificationsByKey.put(r.sbn.getKey(), r);
        if (r.sbn.isGroup()) {
            this.mSummaryByGroupKey.put(r.getGroupKey(), r);
        }
    }

    @VisibleForTesting
    void addEnqueuedNotification(NotificationRecord r) {
        this.mEnqueuedNotifications.add(r);
    }

    @VisibleForTesting
    NotificationRecord getNotificationRecord(String key) {
        return (NotificationRecord) this.mNotificationsByKey.get(key);
    }

    @VisibleForTesting
    void setSystemReady(boolean systemReady) {
        this.mSystemReady = systemReady;
    }

    @VisibleForTesting
    void setHandler(WorkerHandler handler) {
        this.mHandler = handler;
    }

    @VisibleForTesting
    void setFallbackVibrationPattern(long[] vibrationPattern) {
        this.mFallbackVibrationPattern = vibrationPattern;
    }

    @VisibleForTesting
    void setPackageManager(IPackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    @VisibleForTesting
    void setRankingHelper(RankingHelper rankingHelper) {
        this.mRankingHelper = rankingHelper;
    }

    @VisibleForTesting
    void setRankingHandler(RankingHandler rankingHandler) {
        this.mRankingHandler = rankingHandler;
    }

    @VisibleForTesting
    void setIsTelevision(boolean isTelevision) {
        this.mIsTelevision = isTelevision;
    }

    @VisibleForTesting
    void setUsageStats(NotificationUsageStats us) {
        this.mUsageStats = us;
    }

    @VisibleForTesting
    void setAccessibilityManager(AccessibilityManager am) {
        this.mAccessibilityManager = am;
    }

    @VisibleForTesting
    void init(Looper looper, IPackageManager packageManager, PackageManager packageManagerClient, LightsManager lightsManager, NotificationListeners notificationListeners, NotificationAssistants notificationAssistants, ConditionProviders conditionProviders, ICompanionDeviceManager companionManager, SnoozeHelper snoozeHelper, NotificationUsageStats usageStats, AtomicFile policyFile, ActivityManager activityManager, GroupHelper groupHelper, IActivityManager am, UsageStatsManagerInternal appUsageStats, DevicePolicyManagerInternal dpm) {
        String[] extractorNames;
        LightsManager lightsManager2 = lightsManager;
        Resources resources = getContext().getResources();
        this.mMaxPackageEnqueueRate = Global.getFloat(getContext().getContentResolver(), "max_notification_enqueue_rate", 5.0f);
        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        this.mAm = am;
        this.mPackageManager = packageManager;
        this.mPackageManagerClient = packageManagerClient;
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
        this.mAppUsageStats = appUsageStats;
        this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
        this.mCompanionManager = companionManager;
        this.mActivityManager = activityManager;
        this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        this.mDpm = dpm;
        this.mHandler = new WorkerHandler(looper);
        this.mRankingThread.start();
        try {
            extractorNames = resources.getStringArray(17236026);
        } catch (NotFoundException e) {
            NotFoundException notFoundException = e;
            extractorNames = new String[0];
        }
        String[] extractorNames2 = extractorNames;
        this.mUsageStats = usageStats;
        this.mMetricsLogger = new MetricsLogger();
        this.mRankingHandler = new RankingHandlerWorker(this.mRankingThread.getLooper());
        this.mConditionProviders = conditionProviders;
        this.mZenModeHelper = new ZenModeHelper(getContext(), this.mHandler.getLooper(), this.mConditionProviders);
        this.mZenModeHelper.addCallback(new Callback() {
            public void onConfigChanged() {
                NotificationManagerService.this.savePolicyFile();
            }

            void onZenModeChanged() {
                NotificationManagerService.this.sendRegisteredOnlyBroadcast("android.app.action.INTERRUPTION_FILTER_CHANGED");
                NotificationManagerService.this.getContext().sendBroadcastAsUser(new Intent("android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL").addFlags(67108864), UserHandle.ALL, "android.permission.MANAGE_NOTIFICATIONS");
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.updateInterruptionFilterLocked();
                }
                NotificationManagerService.this.mRankingHandler.requestSort();
            }

            void onPolicyChanged() {
                NotificationManagerService.this.sendRegisteredOnlyBroadcast("android.app.action.NOTIFICATION_POLICY_CHANGED");
                NotificationManagerService.this.mRankingHandler.requestSort();
            }
        });
        Context context = getContext();
        PackageManager packageManager2 = this.mPackageManagerClient;
        Context context2 = context;
        PackageManager packageManager3 = packageManager2;
        this.mRankingHelper = new RankingHelper(context2, packageManager3, this.mRankingHandler, this.mZenModeHelper, this.mUsageStats, extractorNames2);
        this.mSnoozeHelper = snoozeHelper;
        this.mGroupHelper = groupHelper;
        this.mListeners = notificationListeners;
        this.mAssistants = notificationAssistants;
        this.mAllowedManagedServicePackages = new -$$Lambda$ouaYRM5YVYoMkUW8dm6TnIjLfgg(this);
        this.mPolicyFile = policyFile;
        loadPolicyFile();
        this.mStatusBar = (StatusBarManagerInternal) getLocalService(StatusBarManagerInternal.class);
        if (this.mStatusBar != null) {
            this.mStatusBar.setNotificationDelegate(this.mNotificationDelegate);
        }
        this.mNotificationLight = lightsManager2.getLight(4);
        this.mAttentionLight = lightsManager2.getLight(5);
        this.mFallbackVibrationPattern = getLongArray(resources, 17236025, 17, DEFAULT_VIBRATE_PATTERN);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("file://");
        stringBuilder.append(resources.getString(17039822));
        this.mInCallNotificationUri = Uri.parse(stringBuilder.toString());
        this.mInCallNotificationAudioAttributes = new Builder().setContentType(4).setUsage(2).build();
        this.mInCallNotificationVolume = resources.getFloat(17104962);
        this.mUseAttentionLight = resources.getBoolean(17957056);
        boolean z = false;
        if (Global.getInt(getContext().getContentResolver(), "device_provisioned", 0) == 0) {
            this.mDisableNotificationEffects = true;
        }
        this.mZenModeHelper.initZenMode();
        this.mInterruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        this.mUserProfiles.updateCache(getContext());
        listenForCallState();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mArchive = new Archive(resources.getInteger(17694836));
        if (this.mPackageManagerClient.hasSystemFeature("android.software.leanback") || this.mPackageManagerClient.hasSystemFeature("android.hardware.type.television")) {
            z = true;
        }
        this.mIsTelevision = z;
    }

    public void onStart() {
        SnoozeHelper snoozeHelper = new SnoozeHelper(getContext(), new Callback() {
            public void repost(int userId, NotificationRecord r) {
                try {
                    if (NotificationManagerService.DBG) {
                        String str = NotificationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Reposting ");
                        stringBuilder.append(r.getKey());
                        Slog.d(str, stringBuilder.toString());
                    }
                    NotificationManagerService.this.enqueueNotificationInternal(r.sbn.getPackageName(), r.sbn.getOpPkg(), r.sbn.getUid(), r.sbn.getInitialPid(), r.sbn.getTag(), r.sbn.getId(), r.sbn.getNotification(), userId);
                } catch (Exception e) {
                    Slog.e(NotificationManagerService.TAG, "Cannot un-snooze notification", e);
                }
            }
        }, this.mUserProfiles);
        File systemDir = new File(Environment.getDataDirectory(), "system");
        Looper myLooper = Looper.myLooper();
        IPackageManager packageManager = AppGlobals.getPackageManager();
        PackageManager packageManager2 = getContext().getPackageManager();
        LightsManager lightsManager = (LightsManager) getLocalService(LightsManager.class);
        NotificationListeners notificationListeners = new NotificationListeners(AppGlobals.getPackageManager());
        NotificationAssistants notificationAssistants = new NotificationAssistants(getContext(), this.mNotificationLock, this.mUserProfiles, AppGlobals.getPackageManager());
        ConditionProviders conditionProviders = new ConditionProviders(getContext(), this.mUserProfiles, AppGlobals.getPackageManager());
        NotificationUsageStats notificationUsageStats = new NotificationUsageStats(getContext());
        AtomicFile atomicFile = new AtomicFile(new File(systemDir, "notification_policy.xml"), TAG_NOTIFICATION_POLICY);
        ConditionProviders conditionProviders2 = conditionProviders;
        init(myLooper, packageManager, packageManager2, lightsManager, notificationListeners, notificationAssistants, conditionProviders2, null, snoozeHelper, notificationUsageStats, atomicFile, (ActivityManager) getContext().getSystemService("activity"), getGroupHelper(), ActivityManager.getService(), (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class), (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.USER_STOPPED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        getContext().registerReceiver(this.mIntentReceiver, filter);
        IntentFilter powerFilter = new IntentFilter();
        powerFilter.addAction(ACTION_HWSYSTEMMANAGER_CHANGE_POWERMODE);
        powerFilter.addAction(ACTION_HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE);
        getContext().registerReceiver(this.powerReceiver, powerFilter, PERMISSION, null);
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("android.intent.action.PACKAGE_ADDED");
        pkgFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        pkgFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        pkgFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
        pkgFilter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
        pkgFilter.addDataScheme("package");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, pkgFilter, null, null);
        IntentFilter suspendedPkgFilter = new IntentFilter();
        suspendedPkgFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        suspendedPkgFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, suspendedPkgFilter, null, null);
        IntentFilter sdFilter = new IntentFilter("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        getContext().registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.ALL, sdFilter, null, null);
        IntentFilter timeoutFilter = new IntentFilter(ACTION_NOTIFICATION_TIMEOUT);
        timeoutFilter.addDataScheme(SCHEME_TIMEOUT);
        getContext().registerReceiver(this.mNotificationTimeoutReceiver, timeoutFilter);
        getContext().registerReceiver(this.mRestoreReceiver, new IntentFilter("android.os.action.SETTING_RESTORED"));
        getContext().registerReceiver(this.mLocaleChangeReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        publishBinderService("notification", this.mService, false, 5);
        publishLocalService(NotificationManagerInternal.class, this.mInternalService);
    }

    private GroupHelper getGroupHelper() {
        return new GroupHelper(new Callback() {
            public void addAutoGroup(String key) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.addAutogroupKeyLocked(key);
                }
            }

            public void removeAutoGroup(String key) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.removeAutogroupKeyLocked(key);
                }
            }

            public void addAutoGroupSummary(int userId, String pkg, String triggeringKey) {
                NotificationManagerService.this.createAutoGroupSummary(userId, pkg, triggeringKey);
            }

            public void removeAutoGroupSummary(int userId, String pkg) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.clearAutogroupSummaryLocked(userId, pkg, 1);
                }
            }

            public void removeAutoGroupSummary(int userId, String pkg, int notifyType) {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationManagerService.this.clearAutogroupSummaryLocked(userId, pkg, notifyType);
                }
            }
        });
    }

    private void sendRegisteredOnlyBroadcast(String action) {
        getContext().sendBroadcastAsUser(new Intent(action).addFlags(1073741824), UserHandle.ALL, null);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mSystemReady = true;
            this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
            this.mAudioManagerInternal = (AudioManagerInternal) getLocalService(AudioManagerInternal.class);
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            this.mZenModeHelper.onSystemReady();
        } else if (phase == 600) {
            this.mSettingsObserver.observe();
            this.mListeners.onBootPhaseAppsCanStart();
            this.mAssistants.onBootPhaseAppsCanStart();
            this.mConditionProviders.onBootPhaseAppsCanStart();
        }
    }

    @GuardedBy("mNotificationLock")
    private void updateListenerHintsLocked() {
        int hints = calculateHints();
        if (hints != this.mListenerHints) {
            ZenLog.traceListenerHintsChanged(this.mListenerHints, hints, this.mEffectsSuppressors.size());
            this.mListenerHints = hints;
            scheduleListenerHintsChanged(hints);
        }
    }

    @GuardedBy("mNotificationLock")
    private void updateEffectsSuppressorLocked() {
        long updatedSuppressedEffects = calculateSuppressedEffects();
        if (updatedSuppressedEffects != this.mZenModeHelper.getSuppressedEffects()) {
            List<ComponentName> suppressors = getSuppressors();
            ZenLog.traceEffectsSuppressorChanged(this.mEffectsSuppressors, suppressors, updatedSuppressedEffects);
            this.mEffectsSuppressors = suppressors;
            this.mZenModeHelper.setSuppressedEffects(updatedSuppressedEffects);
            sendRegisteredOnlyBroadcast("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
        }
    }

    private void exitIdle() {
        try {
            if (this.mDeviceIdleController != null) {
                this.mDeviceIdleController.exitIdle("notification interaction");
            }
        } catch (RemoteException e) {
        }
    }

    private void updateNotificationChannelInt(String pkg, int uid, NotificationChannel channel, boolean fromListener) {
        String str = pkg;
        int i = uid;
        NotificationChannel notificationChannel = channel;
        if (channel.getImportance() == 0) {
            cancelAllNotificationsInt(MY_UID, MY_PID, str, channel.getId(), 0, 0, true, UserHandle.getUserId(uid), 17, null);
            if (isUidSystemOrPhone(i)) {
                int[] profileIds = this.mUserProfiles.getCurrentProfileIds();
                int N = profileIds.length;
                int i2 = 0;
                while (true) {
                    int i3 = i2;
                    if (i3 >= N) {
                        break;
                    }
                    String str2 = str;
                    int i4 = i3;
                    int N2 = N;
                    int[] profileIds2 = profileIds;
                    cancelAllNotificationsInt(MY_UID, MY_PID, str2, channel.getId(), 0, 0, true, profileIds[i3], 17, null);
                    i2 = i4 + 1;
                    profileIds = profileIds2;
                    N = N2;
                }
            }
        }
        NotificationChannel preUpdate = this.mRankingHelper.getNotificationChannel(str, i, channel.getId(), true);
        this.mRankingHelper.updateNotificationChannel(str, i, notificationChannel, true);
        maybeNotifyChannelOwner(str, i, preUpdate, notificationChannel);
        if (!fromListener) {
            this.mListeners.notifyNotificationChannelChanged(str, UserHandle.getUserHandleForUid(uid), this.mRankingHelper.getNotificationChannel(str, i, channel.getId(), false), 2);
        }
        savePolicyFile();
    }

    private void maybeNotifyChannelOwner(String pkg, int uid, NotificationChannel preUpdate, NotificationChannel update) {
        try {
            if ((preUpdate.getImportance() == 0 && update.getImportance() != 0) || (preUpdate.getImportance() != 0 && update.getImportance() == 0)) {
                getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED").putExtra("android.app.extra.NOTIFICATION_CHANNEL_ID", update.getId()).putExtra("android.app.extra.BLOCKED_STATE", update.getImportance() == 0).addFlags(268435456).setPackage(pkg), UserHandle.of(UserHandle.getUserId(uid)), null);
            }
        } catch (SecurityException e) {
            Slog.w(TAG, "Can't notify app about channel change", e);
        }
    }

    private void createNotificationChannelGroup(String pkg, int uid, NotificationChannelGroup group, boolean fromApp, boolean fromListener) {
        Preconditions.checkNotNull(group);
        Preconditions.checkNotNull(pkg);
        NotificationChannelGroup preUpdate = this.mRankingHelper.getNotificationChannelGroup(group.getId(), pkg, uid);
        this.mRankingHelper.createNotificationChannelGroup(pkg, uid, group, fromApp);
        if (!fromApp) {
            maybeNotifyChannelGroupOwner(pkg, uid, preUpdate, group);
        }
        if (!fromListener) {
            this.mListeners.notifyNotificationChannelGroupChanged(pkg, UserHandle.of(UserHandle.getCallingUserId()), group, 1);
        }
    }

    private void maybeNotifyChannelGroupOwner(String pkg, int uid, NotificationChannelGroup preUpdate, NotificationChannelGroup update) {
        try {
            if (preUpdate.isBlocked() != update.isBlocked()) {
                getContext().sendBroadcastAsUser(new Intent("android.app.action.NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED").putExtra("android.app.extra.NOTIFICATION_CHANNEL_GROUP_ID", update.getId()).putExtra("android.app.extra.BLOCKED_STATE", update.isBlocked()).addFlags(268435456).setPackage(pkg), UserHandle.of(UserHandle.getUserId(uid)), null);
            }
        } catch (SecurityException e) {
            Slog.w(TAG, "Can't notify app about group change", e);
        }
    }

    private ArrayList<ComponentName> getSuppressors() {
        ArrayList<ComponentName> names = new ArrayList();
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            Iterator it = ((ArraySet) this.mListenersDisablingEffects.valueAt(i)).iterator();
            while (it.hasNext()) {
                names.add(((ManagedServiceInfo) it.next()).component);
            }
        }
        return names;
    }

    private boolean removeDisabledHints(ManagedServiceInfo info) {
        return removeDisabledHints(info, 0);
    }

    private boolean removeDisabledHints(ManagedServiceInfo info, int hints) {
        boolean removed = false;
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            int hint = this.mListenersDisablingEffects.keyAt(i);
            ArraySet<ManagedServiceInfo> listeners = (ArraySet) this.mListenersDisablingEffects.valueAt(i);
            if (hints == 0 || (hint & hints) == hint) {
                boolean z = removed || listeners.remove(info);
                removed = z;
            }
        }
        return removed;
    }

    private void addDisabledHints(ManagedServiceInfo info, int hints) {
        if ((hints & 1) != 0) {
            addDisabledHint(info, 1);
        }
        if ((hints & 2) != 0) {
            addDisabledHint(info, 2);
        }
        if ((hints & 4) != 0) {
            addDisabledHint(info, 4);
        }
    }

    private void addDisabledHint(ManagedServiceInfo info, int hint) {
        if (this.mListenersDisablingEffects.indexOfKey(hint) < 0) {
            this.mListenersDisablingEffects.put(hint, new ArraySet());
        }
        ((ArraySet) this.mListenersDisablingEffects.get(hint)).add(info);
    }

    private int calculateHints() {
        int hints = 0;
        for (int i = this.mListenersDisablingEffects.size() - 1; i >= 0; i--) {
            int hint = this.mListenersDisablingEffects.keyAt(i);
            if (!((ArraySet) this.mListenersDisablingEffects.valueAt(i)).isEmpty()) {
                hints |= hint;
            }
        }
        return hints;
    }

    private long calculateSuppressedEffects() {
        int hints = calculateHints();
        long suppressedEffects = 0;
        if ((hints & 1) != 0) {
            suppressedEffects = 0 | 3;
        }
        if ((hints & 2) != 0) {
            suppressedEffects |= 1;
        }
        if ((hints & 4) != 0) {
            return suppressedEffects | 2;
        }
        return suppressedEffects;
    }

    @GuardedBy("mNotificationLock")
    private void updateInterruptionFilterLocked() {
        int interruptionFilter = this.mZenModeHelper.getZenModeListenerInterruptionFilter();
        if (interruptionFilter != this.mInterruptionFilter) {
            this.mInterruptionFilter = interruptionFilter;
            scheduleInterruptionFilterChanged(interruptionFilter);
        }
    }

    @VisibleForTesting
    INotificationManager getBinderService() {
        return Stub.asInterface(this.mService);
    }

    @GuardedBy("mNotificationLock")
    protected void reportSeen(NotificationRecord r) {
        this.mAppUsageStats.reportEvent(r.sbn.getPackageName(), getRealUserId(r.sbn.getUserId()), 10);
    }

    protected int calculateSuppressedVisualEffects(Policy incomingPolicy, Policy currPolicy, int targetSdkVersion) {
        if (incomingPolicy.suppressedVisualEffects == -1) {
            return incomingPolicy.suppressedVisualEffects;
        }
        int[] effectsIntroducedInP = new int[]{4, 8, 16, 32, 64, 128, 256};
        int newSuppressedVisualEffects = incomingPolicy.suppressedVisualEffects;
        boolean i = false;
        if (targetSdkVersion < 28) {
            while (true) {
                int i2;
                int i3 = i2;
                if (i3 >= effectsIntroducedInP.length) {
                    break;
                }
                newSuppressedVisualEffects = (newSuppressedVisualEffects & (~effectsIntroducedInP[i3])) | (currPolicy.suppressedVisualEffects & effectsIntroducedInP[i3]);
                i2 = i3 + 1;
            }
            if ((newSuppressedVisualEffects & 1) != 0) {
                newSuppressedVisualEffects = (newSuppressedVisualEffects | 8) | 4;
            }
            if ((newSuppressedVisualEffects & 2) != 0) {
                newSuppressedVisualEffects |= 16;
            }
        } else {
            if ((newSuppressedVisualEffects - 2) - 1 > 0) {
                i = true;
            }
            if (i) {
                newSuppressedVisualEffects &= -4;
                if ((newSuppressedVisualEffects & 16) != 0) {
                    newSuppressedVisualEffects |= 2;
                }
                if (!((newSuppressedVisualEffects & 8) == 0 || (newSuppressedVisualEffects & 4) == 0 || (newSuppressedVisualEffects & 128) == 0)) {
                    newSuppressedVisualEffects |= 1;
                }
            } else {
                if ((newSuppressedVisualEffects & 1) != 0) {
                    newSuppressedVisualEffects = ((newSuppressedVisualEffects | 8) | 4) | 128;
                }
                if ((newSuppressedVisualEffects & 2) != 0) {
                    newSuppressedVisualEffects |= 16;
                }
            }
        }
        return newSuppressedVisualEffects;
    }

    @GuardedBy("mNotificationLock")
    protected void maybeRecordInterruptionLocked(NotificationRecord r) {
        if (r.isInterruptive() && !r.hasRecordedInterruption()) {
            this.mAppUsageStats.reportInterruptiveNotification(r.sbn.getPackageName(), r.getChannel().getId(), getRealUserId(r.sbn.getUserId()));
            logRecentLocked(r);
            r.setRecordedInterruption(true);
        }
    }

    protected void reportUserInteraction(NotificationRecord r) {
        this.mAppUsageStats.reportEvent(r.sbn.getPackageName(), getRealUserId(r.sbn.getUserId()), 7);
    }

    private int getRealUserId(int userId) {
        return userId == -1 ? 0 : userId;
    }

    @VisibleForTesting
    NotificationManagerInternal getInternalService() {
        return this.mInternalService;
    }

    private void applyAdjustment(NotificationRecord r, Adjustment adjustment) {
        if (!(r == null || adjustment.getSignals() == null)) {
            Bundle.setDefusable(adjustment.getSignals(), true);
            r.addAdjustment(adjustment);
        }
    }

    @GuardedBy("mNotificationLock")
    void addAutogroupKeyLocked(String key) {
        NotificationRecord r = (NotificationRecord) this.mNotificationsByKey.get(key);
        if (r != null) {
            String groupKey = this.mGroupHelper.getAutoGroupKey(calculateNotifyType(r), r.sbn.getId());
            if (r.sbn.getOverrideGroupKey() == null) {
                addAutoGroupAdjustment(r, groupKey);
                EventLogTags.writeNotificationAutogrouped(key);
                this.mRankingHandler.requestSort();
            }
        }
    }

    @GuardedBy("mNotificationLock")
    void removeAutogroupKeyLocked(String key) {
        NotificationRecord r = (NotificationRecord) this.mNotificationsByKey.get(key);
        if (!(r == null || r.sbn.getOverrideGroupKey() == null)) {
            addAutoGroupAdjustment(r, null);
            EventLogTags.writeNotificationUnautogrouped(key);
            this.mRankingHandler.requestSort();
        }
    }

    private void addAutoGroupAdjustment(NotificationRecord r, String overrideGroupKey) {
        Bundle signals = new Bundle();
        signals.putString("key_group_key", overrideGroupKey);
        r.addAdjustment(new Adjustment(r.sbn.getPackageName(), r.getKey(), signals, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, r.sbn.getUserId()));
    }

    @GuardedBy("mNotificationLock")
    private void clearAutogroupSummaryLocked(int userId, String pkg, int notifyType) {
        ArrayMap<String, String> summaries = (ArrayMap) this.mAutobundledSummaries.get(Integer.valueOf(userId));
        String notifyKey = this.mGroupHelper.getUnGroupKey(pkg, notifyType);
        if (summaries != null && summaries.containsKey(notifyKey)) {
            NotificationRecord removed = findNotificationByKeyLocked((String) summaries.remove(notifyKey));
            if (removed != null) {
                cancelNotificationLocked(removed, false, 16, removeFromNotificationListsLocked(removed), null);
            }
        }
    }

    @GuardedBy("mNotificationLock")
    private boolean hasAutoGroupSummaryLocked(StatusBarNotification sbn) {
        ArrayMap<String, String> summaries = (ArrayMap) this.mAutobundledSummaries.get(Integer.valueOf(sbn.getUserId()));
        return summaries != null && summaries.containsKey(sbn.getPackageName());
    }

    /* JADX WARNING: Missing block: B:59:0x01c3, code:
            if (r9 == null) goto L_0x01e7;
     */
    /* JADX WARNING: Missing block: B:61:0x01db, code:
            if (checkDisqualifyingFeatures(r11, MY_UID, r9.sbn.getId(), r9.sbn.getTag(), r9, true) == false) goto L_0x01e7;
     */
    /* JADX WARNING: Missing block: B:62:0x01dd, code:
            r8.mHandler.post(new com.android.server.notification.NotificationManagerService.EnqueueNotificationRunnable(r8, r11, r9));
     */
    /* JADX WARNING: Missing block: B:63:0x01e7, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void createAutoGroupSummary(int userId, String pkg, String triggeringKey) {
        Throwable th;
        NotificationRecord summaryRecord;
        NotificationRecord notificationRecord;
        String str = pkg;
        synchronized (this.mNotificationLock) {
            try {
                try {
                    NotificationRecord notificationRecord2 = (NotificationRecord) this.mNotificationsByKey.get(triggeringKey);
                    int i;
                    if (notificationRecord2 == null) {
                        try {
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            i = userId;
                            throw th;
                        }
                    }
                    NotificationRecord summaryRecord2;
                    int notifyType = calculateNotifyType(notificationRecord2);
                    String notifyKey = this.mGroupHelper.getUnGroupKey(str, notifyType);
                    StatusBarNotification adjustedSbn = notificationRecord2.sbn;
                    i = adjustedSbn.getUser().getIdentifier();
                    ArrayMap<String, String> summaries = (ArrayMap) this.mAutobundledSummaries.get(Integer.valueOf(i));
                    if (summaries == null) {
                        try {
                            summaries = new ArrayMap();
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                    try {
                        this.mAutobundledSummaries.put(Integer.valueOf(i), summaries);
                        if (notificationRecord2.getNotification().getGroup() != null) {
                            if (notificationRecord2.getNotification().getGroup().contains("ranker_group")) {
                                return;
                            }
                        }
                        if (summaries.containsKey(notifyKey)) {
                            summaryRecord = null;
                        } else {
                            ApplicationInfo appInfo = (ApplicationInfo) adjustedSbn.getNotification().extras.getParcelable("android.appInfo");
                            Bundle extras = new Bundle();
                            extras.putParcelable("android.appInfo", appInfo);
                            String channelId = notificationRecord2.getChannel().getId();
                            String groupKey = this.mGroupHelper.getAutoGroupKey(notifyType, notificationRecord2.sbn.getId());
                            try {
                                Notification summaryNotification = new Notification.Builder(getContext(), channelId).setSmallIcon(adjustedSbn.getNotification().getSmallIcon()).setGroupSummary(true).setGroupAlertBehavior(2).setGroup(groupKey).setFlag(1024, true).setFlag(512, true).setColor(adjustedSbn.getNotification().color).setLocalOnly(true).build();
                                summaryNotification.extras.putAll(extras);
                                Intent appIntent = getContext().getPackageManager().getLaunchIntentForPackage(str);
                                if (appIntent != null) {
                                    summaryNotification.contentIntent = PendingIntent.getActivityAsUser(getContext(), 0, appIntent, 0, null, UserHandle.of(i));
                                }
                                StatusBarNotification summarySbn = new StatusBarNotification(adjustedSbn.getPackageName(), adjustedSbn.getOpPkg(), HwBootFail.STAGE_BOOT_SUCCESS, groupKey, adjustedSbn.getUid(), adjustedSbn.getInitialPid(), summaryNotification, adjustedSbn.getUser(), groupKey, System.currentTimeMillis());
                                summaryRecord2 = new NotificationRecord(getContext(), summarySbn, notificationRecord2.getChannel());
                                if (notificationRecord2.getNotification().extras == null || !notificationRecord2.getNotification().extras.containsKey("hw_btw")) {
                                    if (notificationRecord2.getImportance() < 2) {
                                        summaryRecord2.setImportance(1, "for user");
                                    }
                                } else {
                                    summaryRecord2.setImportance(1, "for user");
                                    Bundle summaryExtras = summaryRecord2.getNotification().extras;
                                    boolean isBtw = notificationRecord2.getNotification().extras.getBoolean("hw_btw");
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Autogroup summary Notification is btw : ");
                                    stringBuilder.append(isBtw);
                                    Flog.i(400, stringBuilder.toString());
                                    if (summaryExtras != null) {
                                        summaryExtras.putBoolean("hw_btw", isBtw);
                                    }
                                }
                                summaryRecord2.setIsAppImportanceLocked(notificationRecord2.getIsAppImportanceLocked());
                                summaries.put(notifyKey, summarySbn.getKey());
                                summaryRecord = summaryRecord2;
                            } catch (Throwable th4) {
                                th = th4;
                                summaryRecord2 = null;
                                throw th;
                            }
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        notificationRecord = null;
                        throw th;
                    }
                    try {
                    } catch (Throwable th6) {
                        th = th6;
                        summaryRecord2 = summaryRecord;
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    notificationRecord = null;
                    throw th;
                }
            } catch (Throwable th8) {
                th = th8;
                String str2 = triggeringKey;
                notificationRecord = null;
                throw th;
            }
        }
    }

    private String disableNotificationEffects(NotificationRecord record) {
        if (this.mDisableNotificationEffects) {
            return "booleanState";
        }
        if ((this.mListenerHints & 1) != 0) {
            return "listenerHints";
        }
        if (this.mCallState == 0 || this.mZenModeHelper.isCall(record)) {
            return null;
        }
        return "callState";
    }

    private void dumpJson(PrintWriter pw, DumpFilter filter) {
        JSONObject dump = new JSONObject();
        try {
            dump.put("service", "Notification Manager");
            dump.put("bans", this.mRankingHelper.dumpBansJson(filter));
            dump.put("ranking", this.mRankingHelper.dumpJson(filter));
            dump.put("stats", this.mUsageStats.dumpJson(filter));
            dump.put("channels", this.mRankingHelper.dumpChannelsJson(filter));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        pw.println(dump);
    }

    private boolean allowNotificationsInCall(String disableEffects, NotificationRecord record) {
        boolean z = false;
        if (record == null || record.sbn == null) {
            return false;
        }
        boolean isMmsEnabled = isMmsNotificationEnable(record.sbn.getPackageName());
        boolean isCallEnabled = "callState".equals(disableEffects) && "com.android.systemui".equals(record.sbn.getPackageName()) && "low_battery".equals(record.sbn.getTag());
        if (isMmsEnabled || isCallEnabled) {
            z = true;
        }
        return z;
    }

    private void dumpProto(FileDescriptor fd, DumpFilter filter) {
        DumpFilter dumpFilter = filter;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mNotificationLock) {
            int i;
            NotificationRecord nr;
            List<NotificationRecord> snoozed;
            int N = this.mNotificationList.size();
            int i2 = 0;
            while (true) {
                i = i2;
                if (i >= N) {
                    break;
                }
                nr = (NotificationRecord) this.mNotificationList.get(i);
                if (!dumpFilter.filtered || dumpFilter.matches(nr.sbn)) {
                    nr.dump(proto, 2246267895809L, dumpFilter.redact, 1);
                }
                i2 = i + 1;
            }
            N = this.mEnqueuedNotifications.size();
            i2 = 0;
            while (true) {
                i = i2;
                if (i >= N) {
                    break;
                }
                nr = (NotificationRecord) this.mEnqueuedNotifications.get(i);
                if (!dumpFilter.filtered || dumpFilter.matches(nr.sbn)) {
                    nr.dump(proto, 2246267895809L, dumpFilter.redact, 0);
                }
                i2 = i + 1;
            }
            List<NotificationRecord> snoozed2 = this.mSnoozeHelper.getSnoozed();
            N = snoozed2.size();
            i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 >= N) {
                    break;
                }
                NotificationRecord nr2 = (NotificationRecord) snoozed2.get(i3);
                if (!dumpFilter.filtered || dumpFilter.matches(nr2.sbn)) {
                    nr2.dump(proto, 2246267895809L, dumpFilter.redact, 2);
                }
                i2 = i3 + 1;
            }
            long zenLog = proto.start(1146756268034L);
            this.mZenModeHelper.dump(proto);
            for (ComponentName suppressor : this.mEffectsSuppressors) {
                suppressor.writeToProto(proto, 2246267895812L);
            }
            proto.end(zenLog);
            long listenersToken = proto.start(1146756268035L);
            this.mListeners.dump(proto, dumpFilter);
            proto.end(listenersToken);
            proto.write(1120986464260L, this.mListenerHints);
            int i4 = 0;
            while (i4 < this.mListenersDisablingEffects.size()) {
                long zenLog2;
                FileDescriptor fileDescriptor;
                long effectsToken = proto.start(2246267895813L);
                snoozed = snoozed2;
                proto.write(1120986464257L, this.mListenersDisablingEffects.keyAt(i4));
                ArraySet<ManagedServiceInfo> listeners = (ArraySet) this.mListenersDisablingEffects.valueAt(i4);
                int j = 0;
                while (j < listeners.size()) {
                    zenLog2 = zenLog;
                    ((ManagedServiceInfo) listeners.valueAt(i4)).writeToProto(proto, 2246267895810L, null);
                    j++;
                    zenLog = zenLog2;
                    fileDescriptor = fd;
                }
                zenLog2 = zenLog;
                proto.end(effectsToken);
                i4++;
                snoozed2 = snoozed;
                zenLog = zenLog2;
                fileDescriptor = fd;
            }
            snoozed = snoozed2;
            long assistantsToken = proto.start(1146756268038L);
            this.mAssistants.dump(proto, dumpFilter);
            proto.end(assistantsToken);
            long conditionsToken = proto.start(1146756268039L);
            this.mConditionProviders.dump(proto, dumpFilter);
            proto.end(conditionsToken);
            long rankingToken = proto.start(1146756268040L);
            this.mRankingHelper.dump(proto, dumpFilter);
            proto.end(rankingToken);
        }
        proto.flush();
    }

    private void dumpNotificationRecords(PrintWriter pw, DumpFilter filter) {
        synchronized (this.mNotificationLock) {
            int N = this.mNotificationList.size();
            if (N > 0) {
                pw.println("  Notification List:");
                for (int i = 0; i < N; i++) {
                    NotificationRecord nr = (NotificationRecord) this.mNotificationList.get(i);
                    if (!filter.filtered || filter.matches(nr.sbn)) {
                        nr.dump(pw, "    ", getContext(), filter.redact);
                    }
                }
                pw.println("  ");
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x01aa  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void dumpImpl(PrintWriter pw, DumpFilter filter) {
        int N;
        int i;
        pw.print("Current Notification Manager state");
        if (filter.filtered) {
            pw.print(" (filtered to ");
            pw.print(filter);
            pw.print(")");
        }
        pw.println(':');
        boolean zenOnly = filter.filtered && filter.zen;
        if (!zenOnly) {
            synchronized (this.mToastQueue) {
                N = this.mToastQueue.size();
                if (N > 0) {
                    pw.println("  Toast Queue:");
                    for (i = 0; i < N; i++) {
                        ((ToastRecord) this.mToastQueue.get(i)).dump(pw, "    ", filter);
                    }
                    pw.println("  ");
                }
            }
        }
        synchronized (this.mNotificationLock) {
            StringBuilder stringBuilder;
            int N2;
            if (!zenOnly) {
                if (!filter.normalPriority) {
                    dumpNotificationRecords(pw, filter);
                }
                if (!filter.filtered) {
                    N = this.mLights.size();
                    if (N > 0) {
                        pw.println("  Lights List:");
                        for (i = 0; i < N; i++) {
                            if (i == N - 1) {
                                pw.print("  > ");
                            } else {
                                pw.print("    ");
                            }
                            pw.println((String) this.mLights.get(i));
                        }
                        pw.println("  ");
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  mUseAttentionLight=");
                    stringBuilder2.append(this.mUseAttentionLight);
                    pw.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  mNotificationPulseEnabled=");
                    stringBuilder2.append(this.mNotificationPulseEnabled);
                    pw.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  mSoundNotificationKey=");
                    stringBuilder2.append(this.mSoundNotificationKey);
                    pw.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  mVibrateNotificationKey=");
                    stringBuilder2.append(this.mVibrateNotificationKey);
                    pw.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  mDisableNotificationEffects=");
                    stringBuilder2.append(this.mDisableNotificationEffects);
                    pw.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  mCallState=");
                    stringBuilder2.append(callStateToString(this.mCallState));
                    pw.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  mSystemReady=");
                    stringBuilder2.append(this.mSystemReady);
                    pw.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  mMaxPackageEnqueueRate=");
                    stringBuilder2.append(this.mMaxPackageEnqueueRate);
                    pw.println(stringBuilder2.toString());
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("  mArchive=");
                stringBuilder3.append(this.mArchive.toString());
                pw.println(stringBuilder3.toString());
                Iterator<StatusBarNotification> iter = this.mArchive.descendingIterator();
                i = 0;
                while (iter.hasNext()) {
                    StatusBarNotification sbn = (StatusBarNotification) iter.next();
                    if (filter == null || filter.matches(sbn)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("    ");
                        stringBuilder.append(sbn);
                        pw.println(stringBuilder.toString());
                        i++;
                        if (i >= 5) {
                            if (iter.hasNext()) {
                                pw.println("    ...");
                            }
                            if (!zenOnly) {
                                N2 = this.mEnqueuedNotifications.size();
                                if (N2 > 0) {
                                    pw.println("  Enqueued Notification List:");
                                    for (int i2 = 0; i2 < N2; i2++) {
                                        NotificationRecord nr = (NotificationRecord) this.mEnqueuedNotifications.get(i2);
                                        if (!filter.filtered || filter.matches(nr.sbn)) {
                                            nr.dump(pw, "    ", getContext(), filter.redact);
                                        }
                                    }
                                    pw.println("  ");
                                }
                                this.mSnoozeHelper.dump(pw, filter);
                            }
                        }
                    }
                }
                if (zenOnly) {
                }
            }
            if (!zenOnly) {
                pw.println("\n  Ranking Config:");
                this.mRankingHelper.dump(pw, "    ", filter);
                pw.println("\n  Notification listeners:");
                this.mListeners.dump(pw, filter);
                pw.print("    mListenerHints: ");
                pw.println(this.mListenerHints);
                pw.print("    mListenersDisablingEffects: (");
                N = this.mListenersDisablingEffects.size();
                for (i = 0; i < N; i++) {
                    N2 = this.mListenersDisablingEffects.keyAt(i);
                    if (i > 0) {
                        pw.print(';');
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("hint[");
                    stringBuilder.append(N2);
                    stringBuilder.append("]:");
                    pw.print(stringBuilder.toString());
                    ArraySet<ManagedServiceInfo> listeners = (ArraySet) this.mListenersDisablingEffects.valueAt(i);
                    int listenerSize = listeners.size();
                    for (int j = 0; j < listenerSize; j++) {
                        if (i > 0) {
                            pw.print(',');
                        }
                        ManagedServiceInfo listener = (ManagedServiceInfo) listeners.valueAt(i);
                        if (listener != null) {
                            pw.print(listener.component);
                        }
                    }
                }
                pw.println(')');
                pw.println("\n  Notification assistant services:");
                this.mAssistants.dump(pw, filter);
            }
            if (!filter.filtered || zenOnly) {
                pw.println("\n  Zen Mode:");
                pw.print("    mInterruptionFilter=");
                pw.println(this.mInterruptionFilter);
                this.mZenModeHelper.dump(pw, "    ");
                pw.println("\n  Zen Log:");
                ZenLog.dump(pw, "    ");
            }
            pw.println("\n  Condition providers:");
            this.mConditionProviders.dump(pw, filter);
            pw.println("\n  Group summaries:");
            for (Entry<String, NotificationRecord> entry : this.mSummaryByGroupKey.entrySet()) {
                NotificationRecord r = (NotificationRecord) entry.getValue();
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("    ");
                stringBuilder4.append((String) entry.getKey());
                stringBuilder4.append(" -> ");
                stringBuilder4.append(r.getKey());
                pw.println(stringBuilder4.toString());
                if (this.mNotificationsByKey.get(r.getKey()) != r) {
                    pw.println("!!!!!!LEAK: Record not found in mNotificationsByKey.");
                    r.dump(pw, "      ", getContext(), filter.redact);
                }
            }
            if (!zenOnly) {
                pw.println("\n  Usage Stats:");
                this.mUsageStats.dump(pw, "    ", filter);
            }
        }
    }

    private void setNotificationWhiteList() {
        int length;
        String apps_plus = Secure.getString(getContext().getContentResolver(), POWER_SAVER_NOTIFICATION_WHITELIST);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getNotificationWhiteList from db: ");
        stringBuilder.append(apps_plus);
        Log.i(str, stringBuilder.toString());
        this.power_save_whiteSet.clear();
        int i = 0;
        for (String s : this.plus_notification_white_list) {
            this.power_save_whiteSet.add(s);
        }
        for (String s2 : this.noitfication_white_list) {
            this.power_save_whiteSet.add(s2);
        }
        if (!TextUtils.isEmpty(apps_plus)) {
            String[] split = apps_plus.split(";");
            length = split.length;
            while (i < length) {
                this.power_save_whiteSet.add(split[i]);
                i++;
            }
        }
    }

    private boolean isNoitficationWhiteApp(String pkg) {
        return this.power_save_whiteSet.contains(pkg);
    }

    void enqueueNotificationInternal(String pkg, String opPkg, int callingUid, int callingPid, String tag, int id, Notification notification, int incomingUserId) {
        NameNotFoundException e;
        String str = pkg;
        String str2 = opPkg;
        int i = callingUid;
        int i2 = id;
        Notification notification2 = notification;
        StringBuilder stringBuilder;
        if (SystemProperties.getBoolean("sys.super_power_save", false) && !isNoitficationWhiteApp(pkg)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("enqueueNotificationInternal  !isNoitficationWhiteApp package=");
            stringBuilder.append(str);
            Flog.i(400, stringBuilder.toString());
        } else if (isBlockRideModeNotification(pkg)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("enqueueNotificationInternal  !isBlockModeNotification package=");
            stringBuilder.append(str);
            Flog.i(400, stringBuilder.toString());
        } else if (isNotificationDisable()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("MDM policy is on , enqueueNotificationInternal  !isNotificationDisable package=");
            stringBuilder.append(str);
            Flog.i(400, stringBuilder.toString());
        } else {
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enqueueNotificationInternal: pkg=");
            stringBuilder2.append(str);
            stringBuilder2.append(" id=");
            stringBuilder2.append(i2);
            stringBuilder2.append(" notification=");
            stringBuilder2.append(notification2);
            Slog.i(str3, stringBuilder2.toString());
            int targetUid = getNCTargetAppUid(str2, str, i, notification2);
            if (targetUid == i) {
                checkCallerIsSystemOrSameApp(pkg);
            } else {
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("NC ");
                stringBuilder2.append(i);
                stringBuilder2.append(" calling ");
                stringBuilder2.append(targetUid);
                Slog.i(str3, stringBuilder2.toString());
            }
            boolean z = isUidSystemOrPhone(targetUid) || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str) || isCustDialer(pkg);
            if (z || !HwDeviceManager.disallowOp(33)) {
                int targetUid2 = targetUid;
                targetUid = ActivityManager.handleIncomingUser(callingPid, i, incomingUserId, true, false, "enqueueNotification", str);
                UserHandle user = new UserHandle(targetUid);
                String str4;
                int i3;
                if (str == null || notification2 == null) {
                    str4 = str;
                    i3 = targetUid2;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("null not allowed: pkg=");
                    stringBuilder2.append(str4);
                    stringBuilder2.append(" id=");
                    stringBuilder2.append(id);
                    stringBuilder2.append(" notification=");
                    stringBuilder2.append(notification2);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
                i2 = targetUid;
                int i4 = 400;
                recognize(tag, i2, notification2, user, str, i, callingPid);
                if (notification2.extras != null && notification2.extras.containsKey("hw_btw")) {
                    if (notification.isGroupSummary()) {
                        CharSequence title = notification2.extras.getCharSequence("android.title");
                        CharSequence content = notification2.extras.getCharSequence("android.text");
                        RemoteViews remoteV = notification2.contentView;
                        RemoteViews bigContentView = notification2.bigContentView;
                        RemoteViews headsUpContentView = notification2.headsUpContentView;
                        if (remoteV == null && bigContentView == null && headsUpContentView == null && title == null && content == null) {
                            Flog.i(i4, "epmty GroupSummary");
                            return;
                        }
                        notification2.flags ^= 512;
                    }
                    notification2.setGroup(null);
                }
                int notificationUid = resolveNotificationUid(str2, targetUid2, i2);
                int i5;
                try {
                    Notification.addFieldsFromContext(this.mPackageManagerClient.getApplicationInfoAsUser(str, 268435456, i2 == -1 ? 0 : i2), notification2);
                    if (this.mPackageManagerClient.checkPermission("android.permission.USE_COLORIZED_NOTIFICATIONS", str) == 0) {
                        try {
                            notification2.flags |= 2048;
                        } catch (NameNotFoundException e2) {
                            e = e2;
                            i5 = i2;
                            str4 = str;
                            i3 = targetUid2;
                        }
                    } else {
                        notification2.flags &= -2049;
                    }
                    this.mUsageStats.registerEnqueuedByApp(str);
                    str3 = notification.getChannelId();
                    if (this.mIsTelevision && new TvExtender(notification2).getChannelId() != null) {
                        str3 = new TvExtender(notification2).getChannelId();
                    }
                    NotificationChannel channel = this.mRankingHelper.getNotificationChannel(str, notificationUid, str3, false);
                    int userId;
                    boolean appNotificationsOff;
                    if (channel == null) {
                        str4 = new StringBuilder();
                        str4.append("No Channel found for pkg=");
                        str4.append(str);
                        str4.append(", channelId=");
                        str4.append(str3);
                        str4.append(", id=");
                        userId = i2;
                        str4.append(id);
                        str4.append(", tag=");
                        str4.append(tag);
                        str4.append(", opPkg=");
                        str4.append(str2);
                        str4.append(", callingUid=");
                        str4.append(i);
                        str4.append(", userId=");
                        str4.append(userId);
                        str4.append(", incomingUserId=");
                        str4.append(incomingUserId);
                        str4.append(", notificationUid=");
                        str4.append(notificationUid);
                        str4.append(", notification=");
                        str4.append(notification2);
                        str4 = str4.toString();
                        Log.e(TAG, str4);
                        appNotificationsOff = this.mRankingHelper.getImportance(str, notificationUid) == 0;
                        if (appNotificationsOff) {
                            boolean z2 = appNotificationsOff;
                        } else {
                            Log.e(TAG, "appNotificationsOff is false ");
                        }
                        return;
                    }
                    targetUid = tag;
                    int i6 = incomingUserId;
                    userId = i2;
                    i2 = id;
                    String str5 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("enqueueNotificationInternal Channel Info : pkg=");
                    stringBuilder3.append(str);
                    stringBuilder3.append(" id=");
                    stringBuilder3.append(i2);
                    stringBuilder3.append(" importance =");
                    stringBuilder3.append(channel.getImportance());
                    Slog.i(str5, stringBuilder3.toString());
                    i5 = targetUid2;
                    StatusBarNotification statusBarNotification = new StatusBarNotification(str, str2, i2, targetUid, notificationUid, callingPid, notification, user, null, System.currentTimeMillis());
                    targetUid2 = new NotificationRecord(getContext(), statusBarNotification, channel);
                    targetUid2.setIsAppImportanceLocked(this.mRankingHelper.getIsAppImportanceLocked(str, i));
                    notification2 = notification;
                    StatusBarNotification n = statusBarNotification;
                    if ((notification2.flags & 64) != 0) {
                        boolean fgServiceShown = channel.isFgServiceShown();
                        if ((channel.getUserLockedFields() & 4) == 0 || !fgServiceShown) {
                            appNotificationsOff = true;
                            if (targetUid2.getImportance() == 1 || targetUid2.getImportance() == 0) {
                                if (TextUtils.isEmpty(str3) || "miscellaneous".equals(str3)) {
                                    targetUid2.setImportance(2, "Bumped for foreground service");
                                } else {
                                    channel.setImportance(2);
                                    if (!fgServiceShown) {
                                        channel.unlockFields(4);
                                        channel.setFgServiceShown(true);
                                    }
                                    this.mRankingHelper.updateNotificationChannel(str, notificationUid, channel, false);
                                    targetUid2.updateNotificationChannel(channel);
                                }
                            }
                        } else {
                            i3 = i5;
                            appNotificationsOff = true;
                        }
                        if (!(fgServiceShown || TextUtils.isEmpty(str3) || "miscellaneous".equals(str3))) {
                            channel.setFgServiceShown(appNotificationsOff);
                            targetUid2.updateNotificationChannel(channel);
                        }
                    }
                    i5 = userId;
                    str4 = str;
                    if (checkDisqualifyingFeatures(userId, notificationUid, id, tag, targetUid2, targetUid2.sbn.getOverrideGroupKey() != null)) {
                        if (notification2.allPendingIntents != null) {
                            i6 = notification2.allPendingIntents.size();
                            if (i6 > 0) {
                                ActivityManagerInternal userId2 = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
                                long duration = ((LocalService) LocalServices.getService(LocalService.class)).getNotificationWhitelistDuration();
                                int i7 = 0;
                                while (true) {
                                    i = i7;
                                    if (i >= i6) {
                                        break;
                                    }
                                    String channelId;
                                    PendingIntent pendingIntent = (PendingIntent) notification2.allPendingIntents.valueAt(i);
                                    if (pendingIntent != null) {
                                        channelId = str3;
                                        userId2.setPendingIntentWhitelistDuration(pendingIntent.getTarget(), WHITELIST_TOKEN, duration);
                                    } else {
                                        channelId = str3;
                                    }
                                    i7 = i + 1;
                                    str3 = channelId;
                                }
                            }
                        }
                        if (this.mNotificationResource == null) {
                            if (DBG || Log.HWINFO) {
                                Log.i(TAG, " init notification resource");
                            }
                            this.mNotificationResource = HwFrameworkFactory.getHwResource(10);
                        }
                        if (this.mNotificationResource == null || 2 != this.mNotificationResource.acquire(notificationUid, str4, -1)) {
                            this.mHandler.post(new EnqueueNotificationRunnable(i5, targetUid2));
                            return;
                        }
                        if (DBG || Log.HWINFO) {
                            Log.i(TAG, " enqueueNotificationInternal dont acquire resource");
                        }
                        return;
                    }
                    return;
                } catch (NameNotFoundException e3) {
                    e = e3;
                    i5 = i2;
                    str4 = str;
                    i3 = targetUid2;
                    Slog.e(TAG, "Cannot create a context for sending app", e);
                    return;
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("MDM policy forbid (targetUid : ");
            stringBuilder.append(targetUid);
            stringBuilder.append(", pkg : ");
            stringBuilder.append(str);
            stringBuilder.append(") send notification");
            Flog.i(400, stringBuilder.toString());
        }
    }

    private void doChannelWarningToast(CharSequence toastText) {
        if (Global.getInt(getContext().getContentResolver(), "show_notification_channel_warnings", Build.IS_DEBUGGABLE) != 0) {
            Toast.makeText(getContext(), this.mHandler.getLooper(), toastText, 0).show();
        }
    }

    private int resolveNotificationUid(String opPackageName, int callingUid, int userId) {
        if (!(!isCallerSystemOrPhone() || opPackageName == null || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(opPackageName) || TELECOM_PKG.equals(opPackageName))) {
            try {
                return getContext().getPackageManager().getPackageUidAsUser(opPackageName, userId);
            } catch (NameNotFoundException e) {
            }
        }
        return callingUid;
    }

    /* JADX WARNING: Missing block: B:31:0x00dc, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean checkDisqualifyingFeatures(int userId, int callingUid, int id, String tag, NotificationRecord r, boolean isAutogroup) {
        StringBuilder stringBuilder;
        Throwable th;
        int i = userId;
        NotificationRecord notificationRecord = r;
        String pkg = notificationRecord.sbn.getPackageName();
        boolean z = isUidSystemOrPhone(callingUid) || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg);
        boolean isSystemNotification = z;
        boolean isNotificationFromListener = this.mListeners.isListenerPackage(pkg);
        int i2;
        String str;
        if (isSystemNotification || isNotificationFromListener) {
            i2 = id;
            str = tag;
        } else {
            synchronized (this.mNotificationLock) {
                try {
                    if (this.mNotificationsByKey.get(notificationRecord.sbn.getKey()) == null && !isPushNotification(notificationRecord.sbn.getOpPkg(), pkg, notificationRecord.sbn.getNotification()) && isCallerInstantApp(pkg)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Instant app ");
                        stringBuilder.append(pkg);
                        stringBuilder.append(" cannot create notifications");
                        throw new SecurityException(stringBuilder.toString());
                    }
                    String str2;
                    StringBuilder stringBuilder2;
                    if (!(this.mNotificationsByKey.get(notificationRecord.sbn.getKey()) == null || r.getNotification().hasCompletedProgress() || isAutogroup)) {
                        float appEnqueueRate = this.mUsageStats.getAppEnqueueRate(pkg);
                        if (appEnqueueRate > this.mMaxPackageEnqueueRate) {
                            this.mUsageStats.registerOverRateQuota(pkg);
                            long now = SystemClock.elapsedRealtime();
                            if (now - this.mLastOverRateLogTime > MIN_PACKAGE_OVERRATE_LOG_INTERVAL) {
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Package enqueue rate is ");
                                stringBuilder2.append(appEnqueueRate);
                                stringBuilder2.append(". Shedding ");
                                stringBuilder2.append(notificationRecord.sbn.getKey());
                                stringBuilder2.append(". package=");
                                stringBuilder2.append(pkg);
                                Slog.e(str2, stringBuilder2.toString());
                                this.mLastOverRateLogTime = now;
                            }
                        }
                    }
                    int count = getNotificationCountLocked(pkg, i, id, tag);
                    if (count >= 50) {
                        this.mUsageStats.registerOverCountQuota(pkg);
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Package has already posted or enqueued ");
                        stringBuilder2.append(count);
                        stringBuilder2.append(" notifications.  Not showing more.  package=");
                        stringBuilder2.append(pkg);
                        Slog.e(str2, stringBuilder2.toString());
                        return false;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        if (!this.mSnoozeHelper.isSnoozed(i, pkg, r.getKey())) {
            return !isBlocked(notificationRecord, this.mUsageStats);
        } else {
            MetricsLogger.action(r.getLogMaker().setType(6).setCategory(831));
            if (DBG) {
                String str3 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Ignored enqueue for snoozed notification ");
                stringBuilder.append(r.getKey());
                Slog.d(str3, stringBuilder.toString());
            }
            this.mSnoozeHelper.update(i, notificationRecord);
            savePolicyFile();
            return false;
        }
    }

    @GuardedBy("mNotificationLock")
    protected int getNotificationCountLocked(String pkg, int userId, int excludedId, String excludedTag) {
        int i;
        NotificationRecord existing;
        int N = this.mNotificationList.size();
        int i2 = 0;
        int count = 0;
        for (i = 0; i < N; i++) {
            existing = (NotificationRecord) this.mNotificationList.get(i);
            if (existing.sbn.getPackageName().equals(pkg) && existing.sbn.getUserId() == userId && !(existing.sbn.getId() == excludedId && TextUtils.equals(existing.sbn.getTag(), excludedTag))) {
                count++;
            }
        }
        i = this.mEnqueuedNotifications.size();
        while (i2 < i) {
            existing = (NotificationRecord) this.mEnqueuedNotifications.get(i2);
            if (existing.sbn.getPackageName().equals(pkg) && existing.sbn.getUserId() == userId) {
                count++;
            }
            i2++;
        }
        return count;
    }

    protected boolean isBlocked(NotificationRecord r, NotificationUsageStats usageStats) {
        String pkg = r.sbn.getPackageName();
        int callingUid = r.sbn.getUid();
        boolean isPackageSuspended = isPackageSuspendedForUser(pkg, callingUid);
        if (isPackageSuspended) {
            Slog.e(TAG, "Suppressing notification from package due to package suspended by administrator.");
            usageStats.registerSuspendedByAdmin(r);
            return isPackageSuspended;
        }
        boolean isFromPinNf = isFromPinNotification(r.getNotification(), pkg);
        boolean isRankGroupBlocked = this.mRankingHelper.isGroupBlocked(pkg, callingUid, r.getChannel().getGroup());
        int rankImportance = this.mRankingHelper.getImportance(pkg, callingUid);
        int channelImportance = r.getChannel().getImportance();
        boolean isBlocked = !isFromPinNf && (isRankGroupBlocked || rankImportance == 0 || channelImportance == 0);
        if (isBlocked) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Suppressing notification from package by user request. isFromPinNotification = ");
            stringBuilder.append(isFromPinNf);
            stringBuilder.append(", isGroupBlocked = ");
            stringBuilder.append(isRankGroupBlocked);
            stringBuilder.append(", RankImportance = ");
            stringBuilder.append(rankImportance);
            stringBuilder.append(", ChannelImportance = ");
            stringBuilder.append(channelImportance);
            Slog.e(str, stringBuilder.toString());
            usageStats.registerBlocked(r);
        }
        return isBlocked;
    }

    @GuardedBy("mNotificationLock")
    private boolean isPackageSuspendedLocked(NotificationRecord r) {
        return isPackageSuspendedForUser(r.sbn.getPackageName(), r.sbn.getUid());
    }

    private boolean isExpandedNtfPkg(String pkgName) {
        return EXPANDEDNTF_PKGS.contains(pkgName);
    }

    private boolean isTopFullscreen() {
        boolean z = false;
        int ret = 0;
        try {
            IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            if (wm == null) {
                return false;
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.view.IWindowManager");
            wm.asBinder().transact(IS_TOP_FULL_SCREEN_TOKEN, data, reply, 0);
            ret = reply.readInt();
            if (ret > 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            Log.e(TAG, "isTopIsFullscreen", e);
        }
    }

    @GuardedBy("mNotificationLock")
    @VisibleForTesting
    protected boolean isVisuallyInterruptive(NotificationRecord old, NotificationRecord r) {
        NotificationRecord notificationRecord = old;
        NotificationRecord notificationRecord2 = r;
        StringBuilder stringBuilder;
        String str;
        if (notificationRecord == null) {
            if (DEBUG_INTERRUPTIVENESS) {
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("INTERRUPTIVENESS: ");
                stringBuilder.append(r.getKey());
                stringBuilder.append(" is interruptive: new notification");
                Log.v(str2, stringBuilder.toString());
            }
            return true;
        } else if (notificationRecord2 == null) {
            if (DEBUG_INTERRUPTIVENESS) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("INTERRUPTIVENESS: ");
                stringBuilder.append(r.getKey());
                stringBuilder.append(" is not interruptive: null");
                Log.v(str, stringBuilder.toString());
            }
            return false;
        } else {
            Notification oldN = notificationRecord.sbn.getNotification();
            Notification newN = notificationRecord2.sbn.getNotification();
            StringBuilder stringBuilder2;
            if (oldN.extras == null || newN.extras == null) {
                if (DEBUG_INTERRUPTIVENESS) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("INTERRUPTIVENESS: ");
                    stringBuilder2.append(r.getKey());
                    stringBuilder2.append(" is not interruptive: no extras");
                    Log.v(str, stringBuilder2.toString());
                }
                return false;
            } else if ((notificationRecord2.sbn.getNotification().flags & 64) != 0) {
                if (DEBUG_INTERRUPTIVENESS) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("INTERRUPTIVENESS: ");
                    stringBuilder2.append(r.getKey());
                    stringBuilder2.append(" is not interruptive: foreground service");
                    Log.v(str, stringBuilder2.toString());
                }
                return false;
            } else if (notificationRecord2.sbn.isGroup() && notificationRecord2.sbn.getNotification().isGroupSummary()) {
                if (DEBUG_INTERRUPTIVENESS) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("INTERRUPTIVENESS: ");
                    stringBuilder2.append(r.getKey());
                    stringBuilder2.append(" is not interruptive: summary");
                    Log.v(str, stringBuilder2.toString());
                }
                return false;
            } else {
                String oldText;
                if (Objects.equals(String.valueOf(oldN.extras.get("android.title")), String.valueOf(newN.extras.get("android.title")))) {
                    StringBuilder stringBuilder3;
                    StringBuilder stringBuilder4;
                    if (!Objects.equals(String.valueOf(oldN.extras.get("android.text")), String.valueOf(newN.extras.get("android.text")))) {
                        if (DEBUG_INTERRUPTIVENESS) {
                            String str3 = TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("INTERRUPTIVENESS: ");
                            stringBuilder5.append(r.getKey());
                            stringBuilder5.append(" is interruptive: changed text");
                            Log.v(str3, stringBuilder5.toString());
                            str3 = TAG;
                            stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("INTERRUPTIVENESS: ");
                            stringBuilder5.append(String.format("   old text: %s (%s@0x%08x)", new Object[]{oldText, oldText.getClass(), Integer.valueOf(oldText.hashCode())}));
                            Log.v(str3, stringBuilder5.toString());
                            str = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("INTERRUPTIVENESS: ");
                            stringBuilder3.append(String.format("   new text: %s (%s@0x%08x)", new Object[]{newText, newText.getClass(), Integer.valueOf(newText.hashCode())}));
                            Log.v(str, stringBuilder3.toString());
                        }
                        return true;
                    } else if (oldN.hasCompletedProgress() != newN.hasCompletedProgress()) {
                        if (DEBUG_INTERRUPTIVENESS) {
                            str = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("INTERRUPTIVENESS: ");
                            stringBuilder4.append(r.getKey());
                            stringBuilder4.append(" is interruptive: completed progress");
                            Log.v(str, stringBuilder4.toString());
                        }
                        return true;
                    } else if (Notification.areActionsVisiblyDifferent(oldN, newN)) {
                        if (DEBUG_INTERRUPTIVENESS) {
                            str = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("INTERRUPTIVENESS: ");
                            stringBuilder4.append(r.getKey());
                            stringBuilder4.append(" is interruptive: changed actions");
                            Log.v(str, stringBuilder4.toString());
                        }
                        return true;
                    } else {
                        try {
                            Notification.Builder oldB = Notification.Builder.recoverBuilder(getContext(), oldN);
                            Notification.Builder newB = Notification.Builder.recoverBuilder(getContext(), newN);
                            String str4;
                            if (Notification.areStyledNotificationsVisiblyDifferent(oldB, newB)) {
                                if (DEBUG_INTERRUPTIVENESS) {
                                    str4 = TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("INTERRUPTIVENESS: ");
                                    stringBuilder3.append(r.getKey());
                                    stringBuilder3.append(" is interruptive: styles differ");
                                    Log.v(str4, stringBuilder3.toString());
                                }
                                return true;
                            }
                            if (Notification.areRemoteViewsChanged(oldB, newB)) {
                                if (DEBUG_INTERRUPTIVENESS) {
                                    str4 = TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("INTERRUPTIVENESS: ");
                                    stringBuilder3.append(r.getKey());
                                    stringBuilder3.append(" is interruptive: remoteviews differ");
                                    Log.v(str4, stringBuilder3.toString());
                                }
                                return true;
                            }
                            return false;
                        } catch (Exception e) {
                            Slog.w(TAG, "error recovering builder", e);
                        }
                    }
                } else {
                    if (DEBUG_INTERRUPTIVENESS) {
                        oldText = TAG;
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("INTERRUPTIVENESS: ");
                        stringBuilder6.append(r.getKey());
                        stringBuilder6.append(" is interruptive: changed title");
                        Log.v(oldText, stringBuilder6.toString());
                        oldText = TAG;
                        stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("INTERRUPTIVENESS: ");
                        stringBuilder6.append(String.format("   old title: %s (%s@0x%08x)", new Object[]{oldTitle, oldTitle.getClass(), Integer.valueOf(oldTitle.hashCode())}));
                        Log.v(oldText, stringBuilder6.toString());
                        oldText = TAG;
                        stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("INTERRUPTIVENESS: ");
                        stringBuilder6.append(String.format("   new title: %s (%s@0x%08x)", new Object[]{newTitle, newTitle.getClass(), Integer.valueOf(newTitle.hashCode())}));
                        Log.v(oldText, stringBuilder6.toString());
                    }
                    return true;
                }
            }
        }
    }

    @GuardedBy("mNotificationLock")
    @VisibleForTesting
    protected void logRecentLocked(NotificationRecord r) {
        if (!r.isUpdate) {
            ArrayList<NotifyingApp> recentAppsForUser = (ArrayList) this.mRecentApps.getOrDefault(Integer.valueOf(r.getUser().getIdentifier()), new ArrayList(6));
            NotifyingApp na = new NotifyingApp().setPackage(r.sbn.getPackageName()).setUid(r.sbn.getUid()).setLastNotified(r.sbn.getPostTime());
            for (int i = recentAppsForUser.size() - 1; i >= 0; i--) {
                NotifyingApp naExisting = (NotifyingApp) recentAppsForUser.get(i);
                if (na.getPackage().equals(naExisting.getPackage()) && na.getUid() == naExisting.getUid()) {
                    recentAppsForUser.remove(i);
                    break;
                }
            }
            recentAppsForUser.add(0, na);
            if (recentAppsForUser.size() > 5) {
                recentAppsForUser.remove(recentAppsForUser.size() - 1);
            }
            this.mRecentApps.put(Integer.valueOf(r.getUser().getIdentifier()), recentAppsForUser);
        }
    }

    @GuardedBy("mNotificationLock")
    private void handleGroupedNotificationLocked(NotificationRecord r, NotificationRecord old, int callingUid, int callingPid) {
        NotificationRecord notificationRecord = r;
        NotificationRecord notificationRecord2 = old;
        StatusBarNotification sbn = notificationRecord.sbn;
        Notification n = sbn.getNotification();
        if (n.isGroupSummary() && !sbn.isAppGroup()) {
            n.flags &= -513;
        }
        String group = sbn.getGroupKey();
        boolean isSummary = n.isGroupSummary();
        String str = null;
        Notification oldN = notificationRecord2 != null ? notificationRecord2.sbn.getNotification() : null;
        if (notificationRecord2 != null) {
            str = notificationRecord2.sbn.getGroupKey();
        }
        String oldGroup = str;
        boolean z = notificationRecord2 != null && oldN.isGroupSummary();
        boolean oldIsSummary = z;
        if (oldIsSummary) {
            NotificationRecord removedSummary = (NotificationRecord) this.mSummaryByGroupKey.remove(oldGroup);
            if (removedSummary != notificationRecord2) {
                String removedKey = removedSummary != null ? removedSummary.getKey() : "<null>";
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Removed summary didn't match old notification: old=");
                stringBuilder.append(old.getKey());
                stringBuilder.append(", removed=");
                stringBuilder.append(removedKey);
                Slog.w(str2, stringBuilder.toString());
            }
        }
        if (isSummary) {
            this.mSummaryByGroupKey.put(group, notificationRecord);
        }
        if (!oldIsSummary) {
            return;
        }
        if (!isSummary || !oldGroup.equals(group)) {
            cancelGroupChildrenLocked(notificationRecord2, callingUid, callingPid, null, false, null);
        }
    }

    @GuardedBy("mNotificationLock")
    @VisibleForTesting
    void scheduleTimeoutLocked(NotificationRecord record) {
        if (record.getNotification().getTimeoutAfter() > 0) {
            this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + record.getNotification().getTimeoutAfter(), PendingIntent.getBroadcast(getContext(), 1, new Intent(ACTION_NOTIFICATION_TIMEOUT).setData(new Uri.Builder().scheme(SCHEME_TIMEOUT).appendPath(record.getKey()).build()).addFlags(268435456).putExtra(EXTRA_KEY, record.getKey()), 134217728));
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:143:0x024f  */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x026e  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x026c  */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x0274  */
    /* JADX WARNING: Removed duplicated region for block: B:152:0x0271  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x027d  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x027a  */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x028c  */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x028a  */
    /* JADX WARNING: Removed duplicated region for block: B:164:0x0291  */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x028f  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x0295  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0134  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x015d  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x013f  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0165 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x016d  */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x022c A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x024f  */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x0254 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x026c  */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x026e  */
    /* JADX WARNING: Removed duplicated region for block: B:152:0x0271  */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x0274  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x027a  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x027d  */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x028a  */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x028c  */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x028f  */
    /* JADX WARNING: Removed duplicated region for block: B:164:0x0291  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x0295  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0134  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0138 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x013f  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x015d  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0165 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x016d  */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x022c A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x024f  */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x0254 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x026e  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x026c  */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x0274  */
    /* JADX WARNING: Removed duplicated region for block: B:152:0x0271  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x027d  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x027a  */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x028c  */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x028a  */
    /* JADX WARNING: Removed duplicated region for block: B:164:0x0291  */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x028f  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x0295  */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x022c A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x024f  */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x0254 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x026c  */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x026e  */
    /* JADX WARNING: Removed duplicated region for block: B:152:0x0271  */
    /* JADX WARNING: Removed duplicated region for block: B:153:0x0274  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x027a  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x027d  */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x028a  */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x028c  */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x028f  */
    /* JADX WARNING: Removed duplicated region for block: B:164:0x0291  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x0295  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mNotificationLock")
    @VisibleForTesting
    void buzzBeepBlinkLocked(NotificationRecord record) {
        String str;
        boolean buzz;
        boolean beep;
        boolean blink;
        boolean isHwFallBackToVibration;
        boolean blink2;
        int i;
        int i2;
        int i3;
        NotificationRecord notificationRecord = record;
        Notification notification = notificationRecord.sbn.getNotification();
        String key = record.getKey();
        boolean aboveThreshold = record.getImportance() >= 3;
        boolean canInterrupt = (isFromPinNotification(notification, notificationRecord.sbn.getPackageName()) || disallowInterrupt(notification)) ? false : true;
        if (DBG || record.isIntercepted()) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pkg=");
            stringBuilder.append(notificationRecord.sbn.getPackageName());
            stringBuilder.append(" canInterrupt=");
            stringBuilder.append(canInterrupt);
            stringBuilder.append(" record.getImportance()=");
            stringBuilder.append(record.getImportance());
            Slog.v(str, stringBuilder.toString());
        }
        str = disableNotificationEffects(record);
        boolean wasBeep = key != null && key.equals(this.mSoundNotificationKey);
        boolean wasBuzz = key != null && key.equals(this.mVibrateNotificationKey);
        boolean hasValidVibrate = false;
        boolean hasValidSound = false;
        boolean sentAccessibilityEvent = false;
        if (!notificationRecord.isUpdate && record.getImportance() > 1) {
            sendAccessibilityEvent(notification, notificationRecord.sbn.getPackageName());
            sentAccessibilityEvent = true;
        }
        boolean z;
        if (!aboveThreshold || !isNotificationForCurrentUser(record)) {
            buzz = false;
            beep = false;
            blink = false;
            if (DBG || Log.HWINFO) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("disableEffects=");
                stringBuilder2.append(str);
                stringBuilder2.append(" canInterrupt=");
                stringBuilder2.append(canInterrupt);
                stringBuilder2.append(" once update: ");
                z = notificationRecord.isUpdate && (notification.flags & 8) != 0;
                stringBuilder2.append(z);
                Slog.v(str2, stringBuilder2.toString());
            }
        } else if (!canInterrupt || !this.mSystemReady || this.mAudioManager == null) {
            buzz = false;
            beep = false;
            blink = false;
        } else if (this.mGameDndStatus && isGameDndSwitchOn()) {
            buzz = false;
            beep = false;
            blink = false;
        } else {
            long[] vibration;
            Uri soundUri = record.getSound();
            z = (soundUri == null || Uri.EMPTY.equals(soundUri)) ? false : true;
            buzz = false;
            beep = false;
            boolean isHwSoundAllow = isHwSoundAllow(notificationRecord.sbn.getPackageName(), record.getChannel().getId(), record.getUserId());
            if (!isHwSoundAllow) {
                if (this.mAudioManager.getRingerModeInternal() == 2) {
                    isHwFallBackToVibration = true;
                    vibration = record.getVibration();
                    if (vibration == null || !z) {
                        blink = false;
                    } else {
                        blink = false;
                        if ((this.mAudioManager.getRingerModeInternal() || isHwFallBackToVibration) && !this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(record.getAudioAttributes()))) {
                            blink2 = this.mFallbackVibrationPattern;
                            hasValidVibrate = blink2;
                            hasValidSound = z && isHwSoundAllow;
                            if (hasValidVibrate) {
                                if (isHwVibrateAllow(notificationRecord.sbn.getPackageName(), record.getChannel().getId(), record.getUserId())) {
                                    isHwSoundAllow = true;
                                    hasValidVibrate = isHwSoundAllow;
                                    isHwSoundAllow = hasValidSound || hasValidVibrate;
                                    if (isHwSoundAllow || shouldMuteNotificationLocked(record)) {
                                        isHwSoundAllow = buzz;
                                        isHwFallBackToVibration = beep;
                                    } else {
                                        if (!sentAccessibilityEvent) {
                                            sendAccessibilityEvent(notification, notificationRecord.sbn.getPackageName());
                                        }
                                        if (DBG) {
                                            Slog.v(TAG, "Interrupting!");
                                        }
                                        if (hasValidSound) {
                                            this.mSoundNotificationKey = key;
                                            if (this.mInCall) {
                                                playInCallNotification();
                                                isHwFallBackToVibration = true;
                                            } else {
                                                isHwFallBackToVibration = playSound(notificationRecord, soundUri);
                                            }
                                        } else {
                                            isHwFallBackToVibration = beep;
                                        }
                                        z = this.mAudioManager.getRingerModeInternal() == 0;
                                        if (this.mInCall || !hasValidVibrate || z) {
                                            isHwSoundAllow = buzz;
                                        } else {
                                            this.mVibrateNotificationKey = key;
                                            isHwSoundAllow = playVibration(notificationRecord, blink2, hasValidSound);
                                        }
                                    }
                                    buzz = isHwSoundAllow;
                                    if (wasBeep && !hasValidSound) {
                                        clearSoundLocked();
                                    }
                                    if (wasBuzz && !hasValidVibrate) {
                                        clearVibrateLocked();
                                    }
                                    blink2 = this.mLights.remove(key);
                                    if (record.getLight() == null && aboveThreshold && canInterrupt && (record.getSuppressedVisualEffects() & 8) == 0) {
                                        this.mLights.add(key);
                                        updateLightsLocked();
                                        if (this.mUseAttentionLight) {
                                            this.mAttentionLight.pulse();
                                        }
                                        blink = true;
                                    } else if (blink2) {
                                        updateLightsLocked();
                                    }
                                    if (!buzz || isHwFallBackToVibration || blink) {
                                        i = 1;
                                        notificationRecord.setInterruptive(true);
                                        MetricsLogger.action(record.getLogMaker().setCategory(199).setType(1).setSubtype(((buzz ? 1 : 0) | (isHwFallBackToVibration ? 2 : 0)) | (blink ? 4 : 0)));
                                        i2 = buzz ? 1 : 0;
                                        i3 = isHwFallBackToVibration ? 1 : 0;
                                        if (!blink) {
                                            i = 0;
                                        }
                                        EventLogTags.writeNotificationAlert(key, i2, i3, i);
                                    }
                                    return;
                                }
                            }
                            boolean z2 = isHwFallBackToVibration;
                            isHwSoundAllow = false;
                            hasValidVibrate = isHwSoundAllow;
                            if (!hasValidSound) {
                            }
                            if (isHwSoundAllow) {
                            }
                            isHwSoundAllow = buzz;
                            isHwFallBackToVibration = beep;
                            buzz = isHwSoundAllow;
                            clearSoundLocked();
                            clearVibrateLocked();
                            blink2 = this.mLights.remove(key);
                            if (record.getLight() == null) {
                            }
                            if (blink2) {
                            }
                            if (buzz) {
                            }
                            i = 1;
                            notificationRecord.setInterruptive(true);
                            if (buzz) {
                            }
                            if (isHwFallBackToVibration) {
                            }
                            if (blink) {
                            }
                            MetricsLogger.action(record.getLogMaker().setCategory(199).setType(1).setSubtype(((buzz ? 1 : 0) | (isHwFallBackToVibration ? 2 : 0)) | (blink ? 4 : 0)));
                            if (buzz) {
                            }
                            if (isHwFallBackToVibration) {
                            }
                            if (blink) {
                            }
                            EventLogTags.writeNotificationAlert(key, i2, i3, i);
                        }
                    }
                    blink2 = vibration;
                    if (blink2) {
                    }
                    hasValidVibrate = blink2;
                    if (!z) {
                    }
                    if (hasValidVibrate) {
                    }
                    isHwSoundAllow = false;
                    hasValidVibrate = isHwSoundAllow;
                    if (hasValidSound) {
                    }
                    if (isHwSoundAllow) {
                    }
                    isHwSoundAllow = buzz;
                    isHwFallBackToVibration = beep;
                    buzz = isHwSoundAllow;
                    clearSoundLocked();
                    clearVibrateLocked();
                    blink2 = this.mLights.remove(key);
                    if (record.getLight() == null) {
                    }
                    if (blink2) {
                    }
                    if (buzz) {
                    }
                    i = 1;
                    notificationRecord.setInterruptive(true);
                    if (buzz) {
                    }
                    if (isHwFallBackToVibration) {
                    }
                    if (blink) {
                    }
                    MetricsLogger.action(record.getLogMaker().setCategory(199).setType(1).setSubtype(((buzz ? 1 : 0) | (isHwFallBackToVibration ? 2 : 0)) | (blink ? 4 : 0)));
                    if (buzz) {
                    }
                    if (isHwFallBackToVibration) {
                    }
                    if (blink) {
                    }
                    EventLogTags.writeNotificationAlert(key, i2, i3, i);
                }
            }
            isHwFallBackToVibration = false;
            vibration = record.getVibration();
            if (vibration == null) {
            }
            blink = false;
            blink2 = vibration;
            if (blink2) {
            }
            hasValidVibrate = blink2;
            if (z) {
            }
            if (hasValidVibrate) {
            }
            isHwSoundAllow = false;
            hasValidVibrate = isHwSoundAllow;
            if (hasValidSound) {
            }
            if (isHwSoundAllow) {
            }
            isHwSoundAllow = buzz;
            isHwFallBackToVibration = beep;
            buzz = isHwSoundAllow;
            clearSoundLocked();
            clearVibrateLocked();
            blink2 = this.mLights.remove(key);
            if (record.getLight() == null) {
            }
            if (blink2) {
            }
            if (buzz) {
            }
            i = 1;
            notificationRecord.setInterruptive(true);
            if (buzz) {
            }
            if (isHwFallBackToVibration) {
            }
            if (blink) {
            }
            MetricsLogger.action(record.getLogMaker().setCategory(199).setType(1).setSubtype(((buzz ? 1 : 0) | (isHwFallBackToVibration ? 2 : 0)) | (blink ? 4 : 0)));
            if (buzz) {
            }
            if (isHwFallBackToVibration) {
            }
            if (blink) {
            }
            EventLogTags.writeNotificationAlert(key, i2, i3, i);
        }
        isHwFallBackToVibration = beep;
        clearSoundLocked();
        clearVibrateLocked();
        blink2 = this.mLights.remove(key);
        if (record.getLight() == null) {
        }
        if (blink2) {
        }
        if (buzz) {
        }
        i = 1;
        notificationRecord.setInterruptive(true);
        if (buzz) {
        }
        if (isHwFallBackToVibration) {
        }
        if (blink) {
        }
        MetricsLogger.action(record.getLogMaker().setCategory(199).setType(1).setSubtype(((buzz ? 1 : 0) | (isHwFallBackToVibration ? 2 : 0)) | (blink ? 4 : 0)));
        if (buzz) {
        }
        if (isHwFallBackToVibration) {
        }
        if (blink) {
        }
        EventLogTags.writeNotificationAlert(key, i2, i3, i);
    }

    @GuardedBy("mNotificationLock")
    boolean shouldMuteNotificationLocked(NotificationRecord record) {
        Notification notification = record.getNotification();
        if (record.isUpdate && (notification.flags & 8) != 0) {
            return true;
        }
        String disableEffects = disableNotificationEffects(record);
        if (disableEffects != null && !allowNotificationsInCall(disableEffects, record)) {
            ZenLog.traceDisableEffects(record, disableEffects);
            return true;
        } else if (record.isIntercepted() && !inNonDisturbMode(record.sbn.getPackageName())) {
            return true;
        } else {
            if (record.sbn.isGroup() && notification.suppressAlertingDueToGrouping()) {
                return true;
            }
            if (!this.mUsageStats.isAlertRateLimited(record.sbn.getPackageName())) {
                return false;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Muting recently noisy ");
            stringBuilder.append(record.getKey());
            Slog.e(str, stringBuilder.toString());
            return true;
        }
    }

    private boolean playSound(NotificationRecord record, Uri soundUri) {
        boolean looping = (record.getNotification().flags & 4) != 0;
        if (!(this.mAudioManager.isAudioFocusExclusive() || this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(record.getAudioAttributes())) == 0)) {
            long identity = Binder.clearCallingIdentity();
            try {
                IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
                if (player != null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Playing sound ");
                    stringBuilder.append(soundUri);
                    stringBuilder.append(" with attributes ");
                    stringBuilder.append(record.getAudioAttributes());
                    Slog.v(str, stringBuilder.toString());
                    player.playAsync(soundUri, record.sbn.getUser(), looping, record.getAudioAttributes());
                    Binder.restoreCallingIdentity(identity);
                    return true;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
            Binder.restoreCallingIdentity(identity);
        }
        return false;
    }

    private boolean playVibration(NotificationRecord record, long[] vibration, boolean delayVibForSound) {
        long identity = Binder.clearCallingIdentity();
        try {
            int i;
            if ((record.getNotification().flags & 4) != 0) {
                i = 0;
            } else {
                i = -1;
            }
            VibrationEffect effect = VibrationEffect.createWaveform(vibration, i);
            if (delayVibForSound) {
                new Thread(new -$$Lambda$NotificationManagerService$VixLwsYU0BcPccdsEjIXUFUzzx4(this, record, effect)).start();
            } else {
                this.mVibrator.vibrate(record.sbn.getUid(), record.sbn.getOpPkg(), effect, record.getAudioAttributes());
            }
            Binder.restoreCallingIdentity(identity);
            return true;
        } catch (IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error creating vibration waveform with pattern: ");
            stringBuilder.append(Arrays.toString(vibration));
            Slog.e(str, stringBuilder.toString());
            Binder.restoreCallingIdentity(identity);
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public static /* synthetic */ void lambda$playVibration$0(NotificationManagerService notificationManagerService, NotificationRecord record, VibrationEffect effect) {
        int waitMs = notificationManagerService.mAudioManager.getFocusRampTimeMs(3, record.getAudioAttributes());
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Delaying vibration by ");
            stringBuilder.append(waitMs);
            stringBuilder.append("ms");
            Slog.v(str, stringBuilder.toString());
        }
        try {
            Thread.sleep((long) waitMs);
        } catch (InterruptedException e) {
        }
        notificationManagerService.mVibrator.vibrate(record.sbn.getUid(), record.sbn.getOpPkg(), effect, record.getAudioAttributes());
    }

    private boolean isNotificationForCurrentUser(NotificationRecord record) {
        long token = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            return record.getUserId() == -1 || record.getUserId() == currentUser || this.mUserProfiles.isCurrentProfile(record.getUserId());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    protected void playInCallNotification() {
        new Thread() {
            public void run() {
                long identity = Binder.clearCallingIdentity();
                try {
                    IRingtonePlayer player = NotificationManagerService.this.mAudioManager.getRingtonePlayer();
                    if (player != null) {
                        String str = NotificationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("playInCallNotification sound ");
                        stringBuilder.append(NotificationManagerService.this.mInCallNotificationUri);
                        stringBuilder.append(" with attributes ");
                        stringBuilder.append(NotificationManagerService.this.mInCallNotificationAudioAttributes);
                        stringBuilder.append("mInCallBinder : ");
                        stringBuilder.append(NotificationManagerService.this.mInCallBinder);
                        Slog.v(str, stringBuilder.toString());
                        player.play(NotificationManagerService.this.mInCallBinder, NotificationManagerService.this.mInCallNotificationUri, NotificationManagerService.this.mInCallNotificationAudioAttributes, NotificationManagerService.this.mInCallNotificationVolume, false);
                    }
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                }
                Binder.restoreCallingIdentity(identity);
            }
        }.start();
    }

    @GuardedBy("mToastQueue")
    void showNextToastLocked() {
        ToastRecord record = (ToastRecord) this.mToastQueue.get(0);
        while (record != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Show pkg=");
            stringBuilder.append(record.pkg);
            stringBuilder.append(" callback=");
            stringBuilder.append(record.callback);
            Flog.i(400, stringBuilder.toString());
            try {
                record.callback.show(record.token);
                scheduleDurationReachedLocked(record);
                return;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Object died trying to show notification ");
                stringBuilder2.append(record.callback);
                stringBuilder2.append(" in package ");
                stringBuilder2.append(record.pkg);
                Slog.w(str, stringBuilder2.toString());
                int index = this.mToastQueue.indexOf(record);
                if (index >= 0) {
                    this.mToastQueue.remove(index);
                }
                keepProcessAliveIfNeededLocked(record.pid);
                if (this.mToastQueue.size() > 0) {
                    record = (ToastRecord) this.mToastQueue.get(0);
                } else {
                    record = null;
                }
            }
        }
    }

    @GuardedBy("mToastQueue")
    void cancelToastLocked(int index) {
        ToastRecord record = (ToastRecord) this.mToastQueue.get(index);
        try {
            record.callback.hide();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Object died trying to hide notification ");
            stringBuilder.append(record.callback);
            stringBuilder.append(" in package ");
            stringBuilder.append(record.pkg);
            Slog.w(str, stringBuilder.toString());
        }
        ToastRecord lastToast = (ToastRecord) this.mToastQueue.remove(index);
        if (HwPCUtils.isPcCastModeInServer() && (lastToast instanceof ToastRecordEx)) {
            this.mWindowManagerInternal.removeWindowToken(lastToast.token, true, ((ToastRecordEx) lastToast).displayId);
        } else {
            this.mWindowManagerInternal.removeWindowToken(lastToast.token, false, 0);
            scheduleKillTokenTimeout(lastToast.token);
        }
        keepProcessAliveIfNeededLocked(record.pid);
        if (this.mToastQueue.size() > 0) {
            showNextToastLocked();
        }
    }

    void finishTokenLocked(IBinder t) {
        this.mHandler.removeCallbacksAndMessages(t);
        this.mWindowManagerInternal.removeWindowToken(t, true, 0);
    }

    @GuardedBy("mToastQueue")
    private void scheduleDurationReachedLocked(ToastRecord r) {
        this.mHandler.removeCallbacksAndMessages(r);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 2, r), r.duration == 1 ? 3500 : 2000);
    }

    private void handleDurationReached(ToastRecord record) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Timeout pkg=");
        stringBuilder.append(record.pkg);
        stringBuilder.append(" callback=");
        stringBuilder.append(record.callback);
        Flog.i(400, stringBuilder.toString());
        synchronized (this.mToastQueue) {
            int index = indexOfToastLocked(record.pkg, record.callback);
            if (index >= 0) {
                cancelToastLocked(index);
            }
        }
    }

    @GuardedBy("mToastQueue")
    private void scheduleKillTokenTimeout(IBinder token) {
        this.mHandler.removeCallbacksAndMessages(token);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 7, token), 11000);
    }

    private void handleKillTokenTimeout(IBinder token) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Kill Token Timeout token=");
            stringBuilder.append(token);
            Slog.d(str, stringBuilder.toString());
        }
        synchronized (this.mToastQueue) {
            finishTokenLocked(token);
        }
    }

    @GuardedBy("mToastQueue")
    int indexOfToastLocked(String pkg, ITransientNotification callback) {
        IBinder cbak = callback.asBinder();
        ArrayList<ToastRecord> list = this.mToastQueue;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            ToastRecord r = (ToastRecord) list.get(i);
            if (r.pkg.equals(pkg) && r.callback.asBinder().equals(cbak)) {
                return i;
            }
        }
        return -1;
    }

    @GuardedBy("mToastQueue")
    int indexOfToastPackageLocked(String pkg) {
        ArrayList<ToastRecord> list = this.mToastQueue;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            if (((ToastRecord) list.get(i)).pkg.equals(pkg)) {
                return i;
            }
        }
        return -1;
    }

    @GuardedBy("mToastQueue")
    void keepProcessAliveIfNeededLocked(int pid) {
        ArrayList<ToastRecord> list = this.mToastQueue;
        int N = list.size();
        boolean z = false;
        int toastCount = 0;
        for (int i = 0; i < N; i++) {
            if (((ToastRecord) list.get(i)).pid == pid) {
                toastCount++;
            }
        }
        try {
            IActivityManager iActivityManager = this.mAm;
            IBinder iBinder = this.mForegroundToken;
            if (toastCount > 0) {
                z = true;
            }
            iActivityManager.setProcessImportant(iBinder, pid, z, "toast");
        } catch (RemoteException e) {
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0067, code:
            if (r11 == false) goto L_0x006e;
     */
    /* JADX WARNING: Missing block: B:24:0x0069, code:
            r13.mHandler.scheduleSendRankingUpdate();
     */
    /* JADX WARNING: Missing block: B:25:0x006e, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleRankingReconsideration(Message message) {
        if (message.obj instanceof RankingReconsideration) {
            RankingReconsideration recon = message.obj;
            recon.run();
            synchronized (this.mNotificationLock) {
                NotificationRecord record = (NotificationRecord) this.mNotificationsByKey.get(recon.getKey());
                if (record == null) {
                    return;
                }
                int indexBefore = findNotificationRecordIndexLocked(record);
                boolean interceptBefore = record.isIntercepted();
                float contactAffinityBefore = record.getContactAffinity();
                int visibilityBefore = record.getPackageVisibilityOverride();
                recon.applyChangesLocked(record);
                applyZenModeLocked(record);
                this.mRankingHelper.sort(this.mNotificationList);
                int indexAfter = findNotificationRecordIndexLocked(record);
                boolean interceptAfter = record.isIntercepted();
                float contactAffinityAfter = record.getContactAffinity();
                boolean changed = (indexBefore == indexAfter && interceptBefore == interceptAfter && visibilityBefore == record.getPackageVisibilityOverride()) ? false : true;
                if (!(!interceptBefore || interceptAfter || Float.compare(contactAffinityBefore, contactAffinityAfter) == 0)) {
                    buzzBeepBlinkLocked(record);
                }
            }
        }
    }

    void handleRankingSort() {
        if (this.mRankingHelper != null) {
            synchronized (this.mNotificationLock) {
                int N = this.mNotificationList.size();
                ArrayList<String> orderBefore = new ArrayList(N);
                int[] visibilities = new int[N];
                boolean[] showBadges = new boolean[N];
                ArrayList<NotificationChannel> channelBefore = new ArrayList(N);
                ArrayList<String> groupKeyBefore = new ArrayList(N);
                ArrayList<ArrayList<String>> overridePeopleBefore = new ArrayList(N);
                ArrayList<ArrayList<SnoozeCriterion>> snoozeCriteriaBefore = new ArrayList(N);
                ArrayList<Integer> userSentimentBefore = new ArrayList(N);
                ArrayList<Integer> suppressVisuallyBefore = new ArrayList(N);
                int i = 0;
                for (int i2 = 0; i2 < N; i2++) {
                    NotificationRecord r = (NotificationRecord) this.mNotificationList.get(i2);
                    orderBefore.add(r.getKey());
                    visibilities[i2] = r.getPackageVisibilityOverride();
                    showBadges[i2] = r.canShowBadge();
                    channelBefore.add(r.getChannel());
                    groupKeyBefore.add(r.getGroupKey());
                    overridePeopleBefore.add(r.getPeopleOverride());
                    snoozeCriteriaBefore.add(r.getSnoozeCriteria());
                    userSentimentBefore.add(Integer.valueOf(r.getUserSentiment()));
                    suppressVisuallyBefore.add(Integer.valueOf(r.getSuppressedVisualEffects()));
                    this.mRankingHelper.extractSignals(r);
                }
                this.mRankingHelper.sort(this.mNotificationList);
                while (i < N) {
                    NotificationRecord r2 = (NotificationRecord) this.mNotificationList.get(i);
                    if (((String) orderBefore.get(i)).equals(r2.getKey()) && visibilities[i] == r2.getPackageVisibilityOverride() && showBadges[i] == r2.canShowBadge() && Objects.equals(channelBefore.get(i), r2.getChannel()) && Objects.equals(groupKeyBefore.get(i), r2.getGroupKey()) && Objects.equals(overridePeopleBefore.get(i), r2.getPeopleOverride()) && Objects.equals(snoozeCriteriaBefore.get(i), r2.getSnoozeCriteria()) && Objects.equals(userSentimentBefore.get(i), Integer.valueOf(r2.getUserSentiment())) && Objects.equals(suppressVisuallyBefore.get(i), Integer.valueOf(r2.getSuppressedVisualEffects()))) {
                        i++;
                    } else {
                        this.mHandler.scheduleSendRankingUpdate();
                        return;
                    }
                }
            }
        }
    }

    @GuardedBy("mNotificationLock")
    private void recordCallerLocked(NotificationRecord record) {
        if (this.mZenModeHelper.isCall(record)) {
            this.mZenModeHelper.recordCaller(record);
        }
    }

    @GuardedBy("mNotificationLock")
    private void applyZenModeLocked(NotificationRecord record) {
        record.setIntercepted(this.mZenModeHelper.shouldIntercept(record));
        if (record.isIntercepted()) {
            record.setSuppressedVisualEffects(this.mZenModeHelper.getNotificationPolicy().suppressedVisualEffects);
        } else {
            record.setSuppressedVisualEffects(0);
        }
    }

    @GuardedBy("mNotificationLock")
    private int findNotificationRecordIndexLocked(NotificationRecord target) {
        return this.mRankingHelper.indexOf(this.mNotificationList, target);
    }

    private void handleSendRankingUpdate() {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyRankingUpdateLocked(null);
        }
    }

    private void scheduleListenerHintsChanged(int state) {
        this.mHandler.removeMessages(5);
        this.mHandler.obtainMessage(5, state, 0).sendToTarget();
    }

    private void scheduleInterruptionFilterChanged(int listenerInterruptionFilter) {
        this.mHandler.removeMessages(6);
        this.mHandler.obtainMessage(6, listenerInterruptionFilter, 0).sendToTarget();
    }

    private void handleListenerHintsChanged(int hints) {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyListenerHintsChangedLocked(hints);
        }
    }

    private void handleListenerInterruptionFilterChanged(int interruptionFilter) {
        synchronized (this.mNotificationLock) {
            this.mListeners.notifyInterruptionFilterChanged(interruptionFilter);
        }
    }

    static int clamp(int x, int low, int high) {
        if (x < low) {
            return low;
        }
        return x > high ? high : x;
    }

    void sendAccessibilityEvent(Notification notification, CharSequence packageName) {
        if (this.mAccessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(64);
            event.setPackageName(packageName);
            event.setClassName(Notification.class.getName());
            event.setParcelableData(notification);
            CharSequence tickerText = notification.tickerText;
            if (!TextUtils.isEmpty(tickerText)) {
                event.getText().add(tickerText);
            }
            this.mAccessibilityManager.sendAccessibilityEvent(event);
        }
    }

    @GuardedBy("mNotificationLock")
    private boolean removeFromNotificationListsLocked(NotificationRecord r) {
        boolean wasPosted = false;
        NotificationRecord findNotificationByListLocked = findNotificationByListLocked(this.mNotificationList, r.getKey());
        NotificationRecord recordInList = findNotificationByListLocked;
        if (findNotificationByListLocked != null) {
            this.mNotificationList.remove(recordInList);
            this.mNotificationsByKey.remove(recordInList.sbn.getKey());
            wasPosted = true;
        }
        while (true) {
            findNotificationByListLocked = findNotificationByListLocked(this.mEnqueuedNotifications, r.getKey());
            recordInList = findNotificationByListLocked;
            if (findNotificationByListLocked == null) {
                return wasPosted;
            }
            this.mEnqueuedNotifications.remove(recordInList);
        }
    }

    @GuardedBy("mNotificationLock")
    private void cancelNotificationLocked(NotificationRecord r, boolean sendDelete, int reason, boolean wasPosted, String listenerName) {
        cancelNotificationLocked(r, sendDelete, reason, -1, -1, wasPosted, listenerName);
    }

    @GuardedBy("mNotificationLock")
    private void cancelNotificationLocked(NotificationRecord r, boolean sendDelete, int reason, int rank, int count, boolean wasPosted, String listenerName) {
        long identity;
        int i;
        final NotificationRecord notificationRecord = r;
        int i2 = reason;
        String canceledKey = r.getKey();
        recordCallerLocked(r);
        if (r.getStats().getDismissalSurface() == -1) {
            notificationRecord.recordDismissalSurface(0);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cancelNotificationLocked called,tell the app,reason = ");
        stringBuilder.append(i2);
        Flog.i(400, stringBuilder.toString());
        if (sendDelete && r.getNotification().deleteIntent != null) {
            try {
                r.getNotification().deleteIntent.send();
            } catch (CanceledException ex) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("canceled PendingIntent for ");
                stringBuilder2.append(notificationRecord.sbn.getPackageName());
                Slog.w(str, stringBuilder2.toString(), ex);
            }
        }
        final int notifyType = calculateNotifyType(r);
        if (wasPosted) {
            if (r.getNotification().getSmallIcon() != null) {
                if (i2 != 18) {
                    notificationRecord.isCanceled = true;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("cancelNotificationLocked:");
                stringBuilder.append(notificationRecord.sbn.getKey());
                Flog.i(400, stringBuilder.toString());
                this.mListeners.notifyRemovedLocked(notificationRecord, i2, r.getStats());
                this.mHandler.post(new Runnable() {
                    public void run() {
                        NotificationManagerService.this.mGroupHelper.onNotificationRemoved(notificationRecord.sbn, notifyType);
                    }
                });
            }
            if (canceledKey.equals(this.mSoundNotificationKey)) {
                this.mSoundNotificationKey = null;
                identity = Binder.clearCallingIdentity();
                try {
                    IRingtonePlayer player = this.mAudioManager.getRingtonePlayer();
                    if (player != null) {
                        player.stopAsync();
                    }
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                }
                Binder.restoreCallingIdentity(identity);
            }
            if (canceledKey.equals(this.mVibrateNotificationKey)) {
                this.mVibrateNotificationKey = null;
                long identity2 = Binder.clearCallingIdentity();
                try {
                    this.mVibrator.cancel();
                } finally {
                    Binder.restoreCallingIdentity(identity2);
                }
            }
            this.mLights.remove(canceledKey);
        }
        switch (i2) {
            case 2:
            case 3:
                this.mUsageStats.registerDismissedByUser(notificationRecord);
                break;
            default:
                switch (i2) {
                    case 8:
                    case 9:
                        this.mUsageStats.registerRemovedByApp(notificationRecord);
                        break;
                    case 10:
                    case 11:
                        break;
                }
                this.mUsageStats.registerDismissedByUser(notificationRecord);
                break;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("cancelNotificationLocked,remove =");
        stringBuilder.append(notificationRecord.sbn.getPackageName());
        stringBuilder.append(", key:");
        stringBuilder.append(r.getKey());
        Flog.i(400, stringBuilder.toString());
        String groupKey = r.getGroupKey();
        NotificationRecord groupSummary = (NotificationRecord) this.mSummaryByGroupKey.get(groupKey);
        if (groupSummary != null && groupSummary.getKey().equals(canceledKey)) {
            this.mSummaryByGroupKey.remove(groupKey);
        }
        ArrayMap<String, String> summaries = (ArrayMap) this.mAutobundledSummaries.get(Integer.valueOf(notificationRecord.sbn.getUserId()));
        String notifyKey = this.mGroupHelper.getUnGroupKey(notificationRecord.sbn.getPackageName(), notifyType);
        if (summaries != null && notificationRecord.sbn.getKey().equals(summaries.get(notifyKey))) {
            summaries.remove(notifyKey);
        }
        this.mArchive.record(notificationRecord.sbn);
        identity = System.currentTimeMillis();
        LogMaker logMaker = notificationRecord.getLogMaker(identity).setCategory(128).setType(5).setSubtype(i2);
        if (rank != -1) {
            i = count;
            if (i != -1) {
                logMaker.addTaggedData(798, Integer.valueOf(rank)).addTaggedData(1395, Integer.valueOf(count));
                MetricsLogger.action(logMaker);
                EventLogTags.writeNotificationCanceled(canceledKey, i2, notificationRecord.getLifespanMs(identity), notificationRecord.getFreshnessMs(identity), notificationRecord.getExposureMs(identity), rank, i, listenerName);
            }
        }
        i = count;
        String str2 = groupKey;
        MetricsLogger.action(logMaker);
        EventLogTags.writeNotificationCanceled(canceledKey, i2, notificationRecord.getLifespanMs(identity), notificationRecord.getFreshnessMs(identity), notificationRecord.getExposureMs(identity), rank, i, listenerName);
    }

    /* JADX WARNING: Removed duplicated region for block: B:80:0x0114 A:{SYNTHETIC, EDGE_INSN: B:80:0x0114->B:64:0x0114 ?: BREAK  , EDGE_INSN: B:80:0x0114->B:64:0x0114 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d7  */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x0155 A:{SYNTHETIC, EDGE_INSN: B:83:0x0155->B:77:0x0155 ?: BREAK  , EDGE_INSN: B:83:0x0155->B:77:0x0155 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x0120  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0157  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    void updateUriPermissions(NotificationRecord newRecord, NotificationRecord oldRecord, String targetPkg, int targetUserId) {
        String str;
        NotificationRecord notificationRecord = newRecord;
        NotificationRecord notificationRecord2 = oldRecord;
        String key = notificationRecord != null ? newRecord.getKey() : oldRecord.getKey();
        if (DBG) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(": updating permissions");
            Slog.d(str, stringBuilder.toString());
        }
        ArraySet<Uri> newUris = notificationRecord != null ? newRecord.getGrantableUris() : null;
        ArraySet<Uri> oldUris = notificationRecord2 != null ? oldRecord.getGrantableUris() : null;
        if (newUris != null || oldUris != null) {
            int i;
            int i2;
            int i3;
            IBinder permissionOwner = null;
            if (notificationRecord != null && null == null) {
                permissionOwner = notificationRecord.permissionOwner;
            }
            if (notificationRecord2 != null && permissionOwner == null) {
                permissionOwner = notificationRecord2.permissionOwner;
            }
            IBinder permissionOwner2 = permissionOwner;
            if (newUris != null && permissionOwner2 == null) {
                try {
                    StringBuilder stringBuilder2;
                    if (DBG) {
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(key);
                        stringBuilder2.append(": creating owner");
                        Slog.d(str, stringBuilder2.toString());
                    }
                    IActivityManager iActivityManager = this.mAm;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("NOTIF:");
                    stringBuilder2.append(key);
                    permissionOwner2 = iActivityManager.newUriPermissionOwner(stringBuilder2.toString());
                } catch (RemoteException e) {
                }
            }
            if (newUris == null && permissionOwner2 != null) {
                long ident = Binder.clearCallingIdentity();
                try {
                    if (DBG) {
                        str = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(key);
                        stringBuilder3.append(": destroying owner");
                        Slog.d(str, stringBuilder3.toString());
                    }
                    this.mAm.revokeUriPermissionFromOwner(permissionOwner2, null, -1, UserHandle.getUserId(oldRecord.getUid()));
                    permissionOwner = null;
                } catch (RemoteException e2) {
                    permissionOwner = e2;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
                i = 0;
                if (newUris != null && permissionOwner != null) {
                    i2 = 0;
                    while (true) {
                        i3 = i2;
                        if (i3 < newUris.size()) {
                            break;
                        }
                        Uri uri = (Uri) newUris.valueAt(i3);
                        if (oldUris == null || !oldUris.contains(uri)) {
                            if (DBG) {
                                String str2 = TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append(key);
                                stringBuilder4.append(": granting ");
                                stringBuilder4.append(uri);
                                Slog.d(str2, stringBuilder4.toString());
                            }
                            grantUriPermission(permissionOwner, uri, newRecord.getUid(), targetPkg, targetUserId);
                        }
                        i2 = i3 + 1;
                    }
                }
                if (oldUris != null && permissionOwner != null) {
                    while (true) {
                        i2 = i;
                        if (i2 < oldUris.size()) {
                            break;
                        }
                        Uri uri2 = (Uri) oldUris.valueAt(i2);
                        if (newUris == null || !newUris.contains(uri2)) {
                            if (DBG) {
                                String str3 = TAG;
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append(key);
                                stringBuilder5.append(": revoking ");
                                stringBuilder5.append(uri2);
                                Slog.d(str3, stringBuilder5.toString());
                            }
                            revokeUriPermission(permissionOwner, uri2, oldRecord.getUid());
                        }
                        i = i2 + 1;
                    }
                }
                if (notificationRecord != null) {
                    notificationRecord.permissionOwner = permissionOwner;
                }
            }
            permissionOwner = permissionOwner2;
            i = 0;
            i2 = 0;
            while (true) {
                i3 = i2;
                if (i3 < newUris.size()) {
                }
                i2 = i3 + 1;
            }
            while (true) {
                i2 = i;
                if (i2 < oldUris.size()) {
                }
                i = i2 + 1;
            }
            if (notificationRecord != null) {
            }
        }
    }

    private void grantUriPermission(IBinder owner, Uri uri, int sourceUid, String targetPkg, int targetUserId) {
        if (uri != null && "content".equals(uri.getScheme())) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mAm.grantUriPermissionFromOwner(owner, sourceUid, targetPkg, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)), targetUserId);
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void revokeUriPermission(IBinder owner, Uri uri, int sourceUid) {
        if (uri != null && "content".equals(uri.getScheme())) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mAm.revokeUriPermissionFromOwner(owner, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)));
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
            Binder.restoreCallingIdentity(ident);
        }
    }

    void cancelNotification(int callingUid, int callingPid, String pkg, String tag, int id, int mustHaveFlags, int mustNotHaveFlags, boolean sendDelete, int userId, int reason, ManagedServiceInfo listener) {
        cancelNotification(callingUid, callingPid, pkg, tag, id, mustHaveFlags, mustNotHaveFlags, sendDelete, userId, reason, -1, -1, listener);
    }

    void cancelNotification(int callingUid, int callingPid, String pkg, String tag, int id, int mustHaveFlags, int mustNotHaveFlags, boolean sendDelete, int userId, int reason, int rank, int count, ManagedServiceInfo listener) {
        final ManagedServiceInfo managedServiceInfo = listener;
        final int i = callingUid;
        final int i2 = callingPid;
        final String str = pkg;
        final int i3 = id;
        final String str2 = tag;
        final int i4 = userId;
        final int i5 = mustHaveFlags;
        final int i6 = mustNotHaveFlags;
        final int i7 = reason;
        final boolean z = sendDelete;
        final int i8 = rank;
        AnonymousClass15 anonymousClass15 = r0;
        WorkerHandler workerHandler = this.mHandler;
        final int i9 = count;
        AnonymousClass15 anonymousClass152 = new Runnable() {
            /* JADX WARNING: Missing block: B:28:0x00da, code:
            return;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                String listenerName = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
                EventLogTags.writeNotificationCancel(i, i2, str, i3, str2, i4, i5, i6, i7, listenerName);
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    NotificationRecord r = NotificationManagerService.this.findNotificationLocked(str, str2, i3, i4);
                    if (r != null) {
                        if (i7 == 1) {
                            NotificationManagerService.this.mUsageStats.registerClickedByUser(r);
                        }
                        if ((r.getNotification().flags & i5) != i5) {
                        } else if ((r.getNotification().flags & i6) != 0) {
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("cancelNotification,cancelNotificationLocked,callingUid = ");
                            stringBuilder.append(i);
                            stringBuilder.append(",callingPid = ");
                            stringBuilder.append(i2);
                            Flog.i(400, stringBuilder.toString());
                            NotificationManagerService.this.cancelNotificationLocked(r, z, i7, i8, i9, NotificationManagerService.this.removeFromNotificationListsLocked(r), listenerName);
                            NotificationManagerService.this.cancelGroupChildrenLocked(r, i, i2, listenerName, z, null);
                            NotificationManagerService.this.updateLightsLocked();
                        }
                    } else {
                        Flog.i(400, "cancelNotification,r: null");
                        if (i7 != 18 && NotificationManagerService.this.mSnoozeHelper.cancel(i4, str, str2, i3)) {
                            NotificationManagerService.this.savePolicyFile();
                        }
                    }
                }
            }
        };
        workerHandler.post(anonymousClass15);
    }

    private boolean notificationMatchesUserId(NotificationRecord r, int userId) {
        return userId == -1 || r.getUserId() == -1 || r.getUserId() == userId;
    }

    private boolean notificationMatchesCurrentProfiles(NotificationRecord r, int userId) {
        return notificationMatchesUserId(r, userId) || this.mUserProfiles.isCurrentProfile(r.getUserId());
    }

    void cancelAllNotificationsInt(int callingUid, int callingPid, String pkg, String channelId, int mustHaveFlags, int mustNotHaveFlags, boolean doit, int userId, int reason, ManagedServiceInfo listener) {
        final ManagedServiceInfo managedServiceInfo = listener;
        final int i = callingUid;
        final int i2 = callingPid;
        final String str = pkg;
        final int i3 = userId;
        final int i4 = mustHaveFlags;
        final int i5 = mustNotHaveFlags;
        final int i6 = reason;
        final boolean z = doit;
        final String str2 = channelId;
        this.mHandler.post(new Runnable() {
            public void run() {
                String listenerName = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
                EventLogTags.writeNotificationCancelAll(i, i2, str, i3, i4, i5, i6, listenerName);
                if (z) {
                    synchronized (NotificationManagerService.this.mNotificationLock) {
                        Flog.i(400, "cancelAllNotificationsInt called");
                        FlagChecker flagChecker = new -$$Lambda$NotificationManagerService$16$gM2kaMJlOTOJvTbVDftec0GMGcc(i4, i5);
                        NotificationManagerService.this.cancelAllNotificationsByListLocked(NotificationManagerService.this.mNotificationList, i, i2, str, true, str2, flagChecker, false, i3, false, i6, listenerName, true);
                        NotificationManagerService.this.cancelAllNotificationsByListLocked(NotificationManagerService.this.mEnqueuedNotifications, i, i2, str, true, str2, flagChecker, false, i3, false, i6, listenerName, false);
                        NotificationManagerService.this.mSnoozeHelper.cancel(i3, str);
                    }
                }
            }

            static /* synthetic */ boolean lambda$run$0(int mustHaveFlags, int mustNotHaveFlags, int flags) {
                if ((flags & mustHaveFlags) == mustHaveFlags && (flags & mustNotHaveFlags) == 0) {
                    return true;
                }
                return false;
            }
        });
    }

    @GuardedBy("mNotificationLock")
    private void cancelAllNotificationsByListLocked(ArrayList<NotificationRecord> notificationList, int callingUid, int callingPid, String pkg, boolean nullPkgIndicatesUserSwitch, String channelId, FlagChecker flagChecker, boolean includeCurrentProfiles, int userId, boolean sendDelete, int reason, String listenerName, boolean wasPosted) {
        int i;
        ArrayList<NotificationRecord> arrayList = notificationList;
        String str = pkg;
        String str2 = channelId;
        int i2 = userId;
        int i3 = notificationList.size() - 1;
        ArrayList<NotificationRecord> canceledNotifications = null;
        while (true) {
            i = i3;
            if (i < 0) {
                break;
            }
            NotificationRecord r = (NotificationRecord) arrayList.get(i);
            if (includeCurrentProfiles ? notificationMatchesCurrentProfiles(r, i2) : notificationMatchesUserId(r, i2)) {
                if (!(nullPkgIndicatesUserSwitch && str == null && r.getUserId() == -1)) {
                    if (flagChecker.apply(r.getFlags()) && ((str == null || r.sbn.getPackageName().equals(str)) && (str2 == null || str2.equals(r.getChannel().getId())))) {
                        if (canceledNotifications == null) {
                            canceledNotifications = new ArrayList();
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("cancelAllNotificationsInt:");
                        stringBuilder.append(r.sbn.getKey());
                        Flog.i(400, stringBuilder.toString());
                        arrayList.remove(i);
                        this.mNotificationsByKey.remove(r.getKey());
                        canceledNotifications.add(r);
                        cancelNotificationLocked(r, sendDelete, reason, wasPosted, listenerName);
                    }
                    i3 = i - 1;
                }
            }
            FlagChecker flagChecker2 = flagChecker;
            i3 = i - 1;
        }
        if (canceledNotifications != null) {
            int M = canceledNotifications.size();
            int i4 = 0;
            while (true) {
                i = i4;
                if (i < M) {
                    int i5 = i;
                    cancelGroupChildrenLocked((NotificationRecord) canceledNotifications.get(i), callingUid, callingPid, listenerName, false, flagChecker);
                    i4 = i5 + 1;
                } else {
                    updateLightsLocked();
                    return;
                }
            }
        }
    }

    void snoozeNotificationInt(String key, long duration, String snoozeCriterionId, ManagedServiceInfo listener) {
        String listenerName = listener == null ? null : listener.component.toShortString();
        if ((duration > 0 || snoozeCriterionId != null) && key != null) {
            if (DBG) {
                Slog.d(TAG, String.format("snooze event(%s, %d, %s, %s)", new Object[]{key, Long.valueOf(duration), snoozeCriterionId, listenerName}));
            }
            this.mHandler.post(new SnoozeNotificationRunnable(key, duration, snoozeCriterionId));
        }
    }

    void unsnoozeNotificationInt(String key, ManagedServiceInfo listener) {
        String listenerName = listener == null ? null : listener.component.toShortString();
        if (DBG) {
            Slog.d(TAG, String.format("unsnooze event(%s, %s)", new Object[]{key, listenerName}));
        }
        this.mSnoozeHelper.repost(key);
        savePolicyFile();
    }

    @GuardedBy("mNotificationLock")
    void cancelAllLocked(int callingUid, int callingPid, int userId, int reason, ManagedServiceInfo listener, boolean includeCurrentProfiles) {
        final ManagedServiceInfo managedServiceInfo = listener;
        final int i = callingUid;
        final int i2 = callingPid;
        final int i3 = userId;
        final int i4 = reason;
        final boolean z = includeCurrentProfiles;
        this.mHandler.post(new Runnable() {
            public void run() {
                synchronized (NotificationManagerService.this.mNotificationLock) {
                    String listenerName = managedServiceInfo == null ? null : managedServiceInfo.component.toShortString();
                    EventLogTags.writeNotificationCancelAll(i, i2, null, i3, 0, 0, i4, listenerName);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cancelAllLocked called,callingUid = ");
                    stringBuilder.append(i);
                    stringBuilder.append(",callingPid = ");
                    stringBuilder.append(i2);
                    Flog.i(400, stringBuilder.toString());
                    FlagChecker flagChecker = -$$Lambda$NotificationManagerService$17$Hl56UaJa0DLooMsM68-or-QNF1Y.INSTANCE;
                    NotificationManagerService.this.cancelAllNotificationsByListLocked(NotificationManagerService.this.mNotificationList, i, i2, null, false, null, flagChecker, z, i3, true, i4, listenerName, true);
                    NotificationManagerService.this.cancelAllNotificationsByListLocked(NotificationManagerService.this.mEnqueuedNotifications, i, i2, null, false, null, flagChecker, z, i3, true, i4, listenerName, false);
                    NotificationManagerService.this.mSnoozeHelper.cancel(i3, z);
                }
            }

            static /* synthetic */ boolean lambda$run$0(int flags) {
                if ((flags & 34) != 0) {
                    return false;
                }
                return true;
            }
        });
    }

    @GuardedBy("mNotificationLock")
    private void cancelGroupChildrenLocked(NotificationRecord r, int callingUid, int callingPid, String listenerName, boolean sendDelete, FlagChecker flagChecker) {
        if (r.getNotification().isGroupSummary()) {
            NotificationRecord notificationRecord = r;
            if (notificationRecord.sbn.getPackageName() == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No package for group summary: ");
                stringBuilder.append(notificationRecord.getKey());
                Flog.e(400, stringBuilder.toString());
                return;
            }
            NotificationRecord notificationRecord2 = notificationRecord;
            int i = callingUid;
            int i2 = callingPid;
            String str = listenerName;
            boolean z = sendDelete;
            FlagChecker flagChecker2 = flagChecker;
            cancelGroupChildrenByListLocked(this.mNotificationList, notificationRecord2, i, i2, str, z, true, flagChecker2);
            cancelGroupChildrenByListLocked(this.mEnqueuedNotifications, notificationRecord2, i, i2, str, z, false, flagChecker2);
        }
    }

    @GuardedBy("mNotificationLock")
    private void cancelGroupChildrenByListLocked(ArrayList<NotificationRecord> notificationList, NotificationRecord parentNotification, int callingUid, int callingPid, String listenerName, boolean sendDelete, boolean wasPosted, FlagChecker flagChecker) {
        ArrayList<NotificationRecord> arrayList = notificationList;
        FlagChecker i = flagChecker;
        String pkg = parentNotification.sbn.getPackageName();
        int userId = parentNotification.getUserId();
        int i2 = notificationList.size() - 1;
        while (true) {
            int i3 = i2;
            if (i3 >= 0) {
                int i4;
                NotificationRecord childR = (NotificationRecord) arrayList.get(i3);
                StatusBarNotification childSbn = childR.sbn;
                if (!childSbn.isGroup() || childSbn.getNotification().isGroupSummary() || !childR.getGroupKey().equals(parentNotification.getGroupKey()) || (childR.getFlags() & 98) != 0) {
                    i4 = i3;
                } else if (i == null || i.apply(childR.getFlags())) {
                    NotificationRecord childR2 = childR;
                    i4 = i3;
                    EventLogTags.writeNotificationCancel(callingUid, callingPid, pkg, childSbn.getId(), childSbn.getTag(), userId, 0, 0, 12, listenerName);
                    arrayList.remove(i4);
                    NotificationRecord childR3 = childR2;
                    this.mNotificationsByKey.remove(childR3.getKey());
                    cancelNotificationLocked(childR3, sendDelete, 12, wasPosted, listenerName);
                } else {
                    i4 = i3;
                }
                i2 = i4 - 1;
                i = flagChecker;
            } else {
                return;
            }
        }
    }

    @GuardedBy("mNotificationLock")
    void updateLightsLocked() {
        NotificationRecord ledNotification = null;
        int i = this.mLights.size();
        long token = Binder.clearCallingIdentity();
        try {
            int currentUser = ActivityManager.getCurrentUser();
            while (ledNotification == null && i > 0) {
                i--;
                String owner = (String) this.mLights.get(i);
                ledNotification = (NotificationRecord) this.mNotificationsByKey.get(owner);
                String str;
                StringBuilder stringBuilder;
                if (ledNotification == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("LED Notification does not exist: ");
                    stringBuilder.append(owner);
                    Slog.wtfStack(str, stringBuilder.toString());
                    this.mLights.remove(owner);
                } else if (!(ledNotification.getUser().getIdentifier() == currentUser || ledNotification.getUser().getIdentifier() == -1 || isAFWUserId(ledNotification.getUser().getIdentifier()))) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ledNotification is not CurrentUser,AFWuser and AllUser:");
                    stringBuilder.append(owner);
                    Slog.d(str, stringBuilder.toString());
                    ledNotification = null;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateLightsLocked,mInCall =");
            stringBuilder2.append(this.mInCall);
            stringBuilder2.append(",mScreenOn = ");
            stringBuilder2.append(this.mScreenOn);
            stringBuilder2.append(",mGameDndStatus = ");
            stringBuilder2.append(this.mGameDndStatus);
            stringBuilder2.append(",ledNotification == null?");
            stringBuilder2.append(ledNotification == null);
            Flog.i(400, stringBuilder2.toString());
            if (ledNotification == null || this.mInCall || (this.mScreenOn && !this.mGameDndStatus)) {
                Flog.i(400, "updateLightsLocked,turn off notificationLight");
                this.mNotificationLight.turnOff();
                Flog.i(1100, "turn off notificationLight due to incall or screenon");
                updateLight(false, 0, 0);
                return;
            }
            Light light = ledNotification.getLight();
            if (light != null && this.mNotificationPulseEnabled) {
                this.mNotificationLight.setFlashing(light.color, 1, light.onMs, light.offMs);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("set flash led color=0x");
                stringBuilder3.append(Integer.toHexString(light.color));
                stringBuilder3.append(", ledOn:");
                stringBuilder3.append(light.onMs);
                stringBuilder3.append(", ledOff:");
                stringBuilder3.append(light.offMs);
                Flog.i(1100, stringBuilder3.toString());
                updateLight(true, light.onMs, light.offMs);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    protected int indexOfNotificationLocked(String pkg, String tag, int id, int userId) {
        ArrayList<NotificationRecord> list = this.mNotificationList;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            NotificationRecord r = (NotificationRecord) list.get(i);
            if (notificationMatchesUserId(r, userId) && r.sbn.getId() == id && TextUtils.equals(r.sbn.getTag(), tag) && r.sbn.getPackageName().equals(pkg)) {
                return i;
            }
        }
        return -1;
    }

    @GuardedBy("mNotificationLock")
    List<NotificationRecord> findGroupNotificationsLocked(String pkg, String groupKey, int userId) {
        List<NotificationRecord> records = new ArrayList();
        records.addAll(findGroupNotificationByListLocked(this.mNotificationList, pkg, groupKey, userId));
        records.addAll(findGroupNotificationByListLocked(this.mEnqueuedNotifications, pkg, groupKey, userId));
        return records;
    }

    @GuardedBy("mNotificationLock")
    private List<NotificationRecord> findGroupNotificationByListLocked(ArrayList<NotificationRecord> list, String pkg, String groupKey, int userId) {
        List<NotificationRecord> records = new ArrayList();
        int len = list.size();
        for (int i = 0; i < len; i++) {
            NotificationRecord r = (NotificationRecord) list.get(i);
            if (notificationMatchesUserId(r, userId) && r.getGroupKey().equals(groupKey) && r.sbn.getPackageName().equals(pkg)) {
                records.add(r);
            }
        }
        return records;
    }

    @GuardedBy("mNotificationLock")
    private NotificationRecord findNotificationByKeyLocked(String key) {
        NotificationRecord findNotificationByListLocked = findNotificationByListLocked(this.mNotificationList, key);
        NotificationRecord r = findNotificationByListLocked;
        if (findNotificationByListLocked != null) {
            return r;
        }
        findNotificationByListLocked = findNotificationByListLocked(this.mEnqueuedNotifications, key);
        r = findNotificationByListLocked;
        if (findNotificationByListLocked != null) {
            return r;
        }
        return null;
    }

    @GuardedBy("mNotificationLock")
    NotificationRecord findNotificationLocked(String pkg, String tag, int id, int userId) {
        NotificationRecord findNotificationByListLocked = findNotificationByListLocked(this.mNotificationList, pkg, tag, id, userId);
        NotificationRecord r = findNotificationByListLocked;
        if (findNotificationByListLocked != null) {
            return r;
        }
        findNotificationByListLocked = findNotificationByListLocked(this.mEnqueuedNotifications, pkg, tag, id, userId);
        r = findNotificationByListLocked;
        if (findNotificationByListLocked != null) {
            return r;
        }
        return null;
    }

    @GuardedBy("mNotificationLock")
    public NotificationRecord findNotificationByListLocked(ArrayList<NotificationRecord> list, String pkg, String tag, int id, int userId) {
        int len = list.size();
        for (int i = 0; i < len; i++) {
            NotificationRecord r = (NotificationRecord) list.get(i);
            if (notificationMatchesUserId(r, userId) && r.sbn.getId() == id && TextUtils.equals(r.sbn.getTag(), tag) && r.sbn.getPackageName().equals(pkg)) {
                return r;
            }
        }
        return null;
    }

    @GuardedBy("mNotificationLock")
    public NotificationRecord findNotificationByListLocked(ArrayList<NotificationRecord> list, String key) {
        int N = list.size();
        for (int i = 0; i < N; i++) {
            if (key.equals(((NotificationRecord) list.get(i)).getKey())) {
                return (NotificationRecord) list.get(i);
            }
        }
        return null;
    }

    @GuardedBy("mNotificationLock")
    int indexOfNotificationLocked(String key) {
        int N = this.mNotificationList.size();
        for (int i = 0; i < N; i++) {
            if (key.equals(((NotificationRecord) this.mNotificationList.get(i)).getKey())) {
                return i;
            }
        }
        return -1;
    }

    @VisibleForTesting
    protected void hideNotificationsForPackages(String[] pkgs) {
        synchronized (this.mNotificationLock) {
            List<String> pkgList = Arrays.asList(pkgs);
            List<NotificationRecord> changedNotifications = new ArrayList();
            int numNotifications = this.mNotificationList.size();
            for (int i = 0; i < numNotifications; i++) {
                NotificationRecord rec = (NotificationRecord) this.mNotificationList.get(i);
                if (pkgList.contains(rec.sbn.getPackageName())) {
                    rec.setHidden(true);
                    changedNotifications.add(rec);
                }
            }
            this.mListeners.notifyHiddenLocked(changedNotifications);
        }
    }

    @VisibleForTesting
    protected void unhideNotificationsForPackages(String[] pkgs) {
        synchronized (this.mNotificationLock) {
            List<String> pkgList = Arrays.asList(pkgs);
            List<NotificationRecord> changedNotifications = new ArrayList();
            int numNotifications = this.mNotificationList.size();
            for (int i = 0; i < numNotifications; i++) {
                NotificationRecord rec = (NotificationRecord) this.mNotificationList.get(i);
                if (pkgList.contains(rec.sbn.getPackageName())) {
                    rec.setHidden(false);
                    changedNotifications.add(rec);
                }
            }
            this.mListeners.notifyUnhiddenLocked(changedNotifications);
        }
    }

    private void updateNotificationPulse() {
        synchronized (this.mNotificationLock) {
            updateLightsLocked();
        }
    }

    protected boolean isCallingUidSystem() {
        return Binder.getCallingUid() == 1000;
    }

    protected boolean isUidSystemOrPhone(int uid) {
        int appid = UserHandle.getAppId(uid);
        return appid == 1000 || appid == 1001 || uid == 0;
    }

    protected boolean isCallerSystemOrPhone() {
        return isUidSystemOrPhone(Binder.getCallingUid());
    }

    private void checkCallerIsSystemOrShell() {
        if (Binder.getCallingUid() != 2000) {
            checkCallerIsSystem();
        }
    }

    private void checkCallerIsSystem() {
        if (!isCallerSystemOrPhone()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Disallowed call for uid ");
            stringBuilder.append(Binder.getCallingUid());
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void checkCallerIsSystemOrSameApp(String pkg) {
        if (!isCallerSystemOrPhone()) {
            checkCallerIsSameApp(pkg);
        }
    }

    private boolean isCallerInstantApp(String pkg) {
        if (isCallerSystemOrPhone()) {
            return false;
        }
        this.mAppOps.checkPackage(Binder.getCallingUid(), pkg);
        StringBuilder stringBuilder;
        try {
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(pkg, 0, UserHandle.getCallingUserId());
            if (ai != null) {
                return ai.isInstantApp();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown package ");
            stringBuilder.append(pkg);
            throw new SecurityException(stringBuilder.toString());
        } catch (RemoteException re) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown package ");
            stringBuilder.append(pkg);
            throw new SecurityException(stringBuilder.toString(), re);
        }
    }

    private void checkCallerIsSameApp(String pkg) {
        int uid = Binder.getCallingUid();
        StringBuilder stringBuilder;
        try {
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(pkg, 0, UserHandle.getCallingUserId());
            if (ai == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown package ");
                stringBuilder.append(pkg);
                throw new SecurityException(stringBuilder.toString());
            } else if (!UserHandle.isSameApp(ai.uid, uid)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Calling uid ");
                stringBuilder.append(uid);
                stringBuilder.append(" gave package ");
                stringBuilder.append(pkg);
                stringBuilder.append(" which is owned by uid ");
                stringBuilder.append(ai.uid);
                throw new SecurityException(stringBuilder.toString());
            }
        } catch (RemoteException re) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown package ");
            stringBuilder.append(pkg);
            stringBuilder.append("\n");
            stringBuilder.append(re);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private static String callStateToString(int state) {
        switch (state) {
            case 0:
                return "CALL_STATE_IDLE";
            case 1:
                return "CALL_STATE_RINGING";
            case 2:
                return "CALL_STATE_OFFHOOK";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CALL_STATE_UNKNOWN_");
                stringBuilder.append(state);
                return stringBuilder.toString();
        }
    }

    private void listenForCallState() {
        TelephonyManager.from(getContext()).listen(new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                if (NotificationManagerService.this.mCallState != state) {
                    if (NotificationManagerService.DBG) {
                        String str = NotificationManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Call state changed: ");
                        stringBuilder.append(NotificationManagerService.callStateToString(state));
                        Slog.d(str, stringBuilder.toString());
                    }
                    NotificationManagerService.this.mCallState = state;
                }
            }
        }, 32);
    }

    @GuardedBy("mNotificationLock")
    private NotificationRankingUpdate makeRankingUpdateLocked(ManagedServiceInfo info) {
        NotificationManagerService notificationManagerService = this;
        int N = notificationManagerService.mNotificationList.size();
        ArrayList<String> keys = new ArrayList(N);
        ArrayList<String> interceptedKeys = new ArrayList(N);
        ArrayList<Integer> importance = new ArrayList(N);
        Bundle overrideGroupKeys = new Bundle();
        Bundle visibilityOverrides = new Bundle();
        Bundle suppressedVisualEffects = new Bundle();
        Bundle explanation = new Bundle();
        Bundle channels = new Bundle();
        Bundle overridePeople = new Bundle();
        Bundle snoozeCriteria = new Bundle();
        Bundle showBadge = new Bundle();
        Bundle userSentiment = new Bundle();
        Bundle hidden = new Bundle();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= N) {
                break;
            }
            int N2 = N;
            NotificationRecord N3 = (NotificationRecord) notificationManagerService.mNotificationList.get(i2);
            int i3 = i2;
            Bundle hidden2 = hidden;
            if (notificationManagerService.isVisibleToListener(N3.sbn, info)) {
                String key = N3.sbn.getKey();
                keys.add(key);
                importance.add(Integer.valueOf(N3.getImportance()));
                if (N3.getImportanceExplanation() != null) {
                    explanation.putCharSequence(key, N3.getImportanceExplanation());
                }
                if (N3.isIntercepted()) {
                    interceptedKeys.add(key);
                }
                suppressedVisualEffects.putInt(key, N3.getSuppressedVisualEffects());
                if (N3.getPackageVisibilityOverride() != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                    visibilityOverrides.putInt(key, N3.getPackageVisibilityOverride());
                }
                overrideGroupKeys.putString(key, N3.sbn.getOverrideGroupKey());
                channels.putParcelable(key, N3.getChannel());
                overridePeople.putStringArrayList(key, N3.getPeopleOverride());
                snoozeCriteria.putParcelableArrayList(key, N3.getSnoozeCriteria());
                showBadge.putBoolean(key, N3.canShowBadge());
                userSentiment.putInt(key, N3.getUserSentiment());
                hidden = hidden2;
                hidden.putBoolean(key, N3.isHidden());
            } else {
                hidden = hidden2;
            }
            i = i3 + 1;
            N = N2;
            notificationManagerService = this;
        }
        int M = keys.size();
        String[] keysAr = (String[]) keys.toArray(new String[M]);
        String[] interceptedKeysAr = (String[]) interceptedKeys.toArray(new String[interceptedKeys.size()]);
        int[] importanceAr = new int[M];
        int i4 = 0;
        while (true) {
            ArrayList<String> keys2 = keys;
            int i5 = i4;
            if (i5 < M) {
                int M2 = M;
                importanceAr[i5] = ((Integer) importance.get(i5)).intValue();
                i4 = i5 + 1;
                keys = keys2;
                M = M2;
            } else {
                return new NotificationRankingUpdate(keysAr, interceptedKeysAr, visibilityOverrides, suppressedVisualEffects, importanceAr, explanation, overrideGroupKeys, channels, overridePeople, snoozeCriteria, showBadge, userSentiment, hidden);
            }
        }
    }

    boolean hasCompanionDevice(ManagedServiceInfo info) {
        if (this.mCompanionManager == null) {
            this.mCompanionManager = getCompanionManager();
        }
        if (this.mCompanionManager == null) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            if (!ArrayUtils.isEmpty(this.mCompanionManager.getAssociations(info.component.getPackageName(), info.userid))) {
                Binder.restoreCallingIdentity(identity);
                return true;
            }
        } catch (SecurityException e) {
        } catch (RemoteException re) {
            Slog.e(TAG, "Cannot reach companion device service", re);
        } catch (Exception e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot verify listener ");
            stringBuilder.append(info);
            Slog.e(str, stringBuilder.toString(), e2);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
        Binder.restoreCallingIdentity(identity);
        return false;
    }

    protected ICompanionDeviceManager getCompanionManager() {
        return ICompanionDeviceManager.Stub.asInterface(ServiceManager.getService("companiondevice"));
    }

    private boolean isVisibleToListener(StatusBarNotification sbn, ManagedServiceInfo listener) {
        if (listener.enabledAndUserMatches(sbn.getUserId())) {
            return true;
        }
        return false;
    }

    private boolean isPackageSuspendedForUser(String pkg, int uid) {
        long identity = Binder.clearCallingIdentity();
        try {
            boolean isPackageSuspendedForUser = this.mPackageManager.isPackageSuspendedForUser(pkg, UserHandle.getUserId(uid));
            Binder.restoreCallingIdentity(identity);
            return isPackageSuspendedForUser;
        } catch (RemoteException e) {
            throw new SecurityException("Could not talk to package manager service");
        } catch (IllegalArgumentException e2) {
            Binder.restoreCallingIdentity(identity);
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @VisibleForTesting
    boolean canUseManagedServices(String pkg) {
        int i = 0;
        boolean canUseManagedServices = !this.mActivityManager.isLowRamDevice() || this.mPackageManagerClient.hasSystemFeature("android.hardware.type.watch");
        String[] stringArray = getContext().getResources().getStringArray(17235977);
        int length = stringArray.length;
        while (i < length) {
            if (stringArray[i].equals(pkg)) {
                canUseManagedServices = true;
            }
            i++;
        }
        return canUseManagedServices;
    }

    @VisibleForTesting
    protected void simulatePackageSuspendBroadcast(boolean suspend, String pkg) {
        String action;
        Bundle extras = new Bundle();
        extras.putStringArray("android.intent.extra.changed_package_list", new String[]{pkg});
        if (suspend) {
            action = "android.intent.action.PACKAGES_SUSPENDED";
        } else {
            action = "android.intent.action.PACKAGES_UNSUSPENDED";
        }
        Intent intent = new Intent(action);
        intent.putExtras(extras);
        this.mPackageIntentReceiver.onReceive(getContext(), intent);
    }

    private boolean isPushNotification(String opPkg, String pkg, Notification notification) {
        if (notification != null && NOTIFICATION_CENTER_PKG.equals(opPkg)) {
            Bundle bundle = notification.extras;
            if (bundle != null) {
                String targetPkg = bundle.getString(NOTIFICATION_CENTER_ORIGIN_PKG);
                if (targetPkg != null && targetPkg.equals(pkg)) {
                    try {
                        if (AppGlobals.getPackageManager().getApplicationInfo(targetPkg, 0, UserHandle.getCallingUserId()) != null) {
                            return true;
                        }
                    } catch (Exception e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown package pkg:");
                        stringBuilder.append(targetPkg);
                        Slog.w(str, stringBuilder.toString());
                    }
                }
            }
        }
        return false;
    }

    private boolean disallowInterrupt(Notification notification) {
        if (notification == null || notification.extras == null) {
            return false;
        }
        return notification.extras.getBoolean("hw_btw", false);
    }

    private int calculateNotifyType(NotificationRecord r) {
        int notifyType = r.getImportance() < 2 ? 0 : 1;
        if (r.getNotification().extras == null) {
            return notifyType;
        }
        if (r.getNotification().extras.containsKey("hw_btw")) {
            notifyType = 1 ^ disallowInterrupt(r.getNotification());
        }
        String hwType = r.getNotification().extras.getString("hw_type");
        if (hwType == null || !hwType.equals("type_music")) {
            return notifyType;
        }
        return 2;
    }

    public void setSysMgrCfgMap(ArrayList<NotificationSysMgrCfg> tempSysMgrCfgList) {
        if (this.mRankingHelper == null) {
            Log.w(TAG, "setSysMgrCfgMap: mRankingHelper is null");
        } else {
            this.mRankingHelper.setSysMgrCfgMap(tempSysMgrCfgList);
        }
    }

    protected boolean isCustDialer(String packageName) {
        return false;
    }

    protected String getPackageNameByPid(int pid) {
        if (pid <= 0) {
            return null;
        }
        ActivityManager activityManager = (ActivityManager) getContext().getSystemService("activity");
        if (activityManager == null) {
            return null;
        }
        List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return null;
        }
        String packageName = null;
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.pid == pid) {
                packageName = appProcess.processName;
                break;
            }
        }
        int indexProcessFlag = -1;
        if (packageName != null) {
            indexProcessFlag = packageName.indexOf(58);
        }
        return indexProcessFlag > 0 ? packageName.substring(0, indexProcessFlag) : packageName;
    }
}
