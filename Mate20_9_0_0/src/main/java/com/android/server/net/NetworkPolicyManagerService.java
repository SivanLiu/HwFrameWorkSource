package com.android.server.net;

import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStatsManagerInternal;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.IConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.INetworkPolicyListener;
import android.net.INetworkPolicyManager.Stub;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkIdentity;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkQuotaInfo;
import android.net.NetworkRequest.Builder;
import android.net.NetworkSpecifier;
import android.net.NetworkState;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.net.NetworkTemplate;
import android.net.StringNetworkSpecifier;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BestClock;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IDeviceIdleController;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.os.PersistableBundle;
import android.os.PowerManagerInternal;
import android.os.PowerManagerInternal.LowPowerModeListener;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.DataUnit;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.RecurrenceRule;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.NetPluginDelegate;
import com.android.server.NetworkManagementService;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class NetworkPolicyManagerService extends Stub {
    private static final String ACTION_ALLOW_BACKGROUND = "com.android.server.net.action.ALLOW_BACKGROUND";
    private static final String ACTION_SNOOZE_RAPID = "com.android.server.net.action.SNOOZE_RAPID";
    private static final String ACTION_SNOOZE_WARNING = "com.android.server.net.action.SNOOZE_WARNING";
    private static final String ATTR_APP_ID = "appId";
    @Deprecated
    private static final String ATTR_CYCLE_DAY = "cycleDay";
    private static final String ATTR_CYCLE_END = "cycleEnd";
    private static final String ATTR_CYCLE_PERIOD = "cyclePeriod";
    private static final String ATTR_CYCLE_START = "cycleStart";
    @Deprecated
    private static final String ATTR_CYCLE_TIMEZONE = "cycleTimezone";
    private static final String ATTR_INFERRED = "inferred";
    private static final String ATTR_LAST_LIMIT_SNOOZE = "lastLimitSnooze";
    private static final String ATTR_LAST_SNOOZE = "lastSnooze";
    private static final String ATTR_LAST_WARNING_SNOOZE = "lastWarningSnooze";
    private static final String ATTR_LIMIT_BEHAVIOR = "limitBehavior";
    private static final String ATTR_LIMIT_BYTES = "limitBytes";
    private static final String ATTR_METERED = "metered";
    private static final String ATTR_NETWORK_ID = "networkId";
    private static final String ATTR_NETWORK_TEMPLATE = "networkTemplate";
    private static final String ATTR_OWNER_PACKAGE = "ownerPackage";
    private static final String ATTR_POLICY = "policy";
    private static final String ATTR_RESTRICT_BACKGROUND = "restrictBackground";
    private static final String ATTR_SUBSCRIBER_ID = "subscriberId";
    private static final String ATTR_SUB_ID = "subId";
    private static final String ATTR_SUMMARY = "summary";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_UID = "uid";
    private static final String ATTR_USAGE_BYTES = "usageBytes";
    private static final String ATTR_USAGE_TIME = "usageTime";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_WARNING_BYTES = "warningBytes";
    private static final int CHAIN_TOGGLE_DISABLE = 2;
    private static final int CHAIN_TOGGLE_ENABLE = 1;
    private static final int CHAIN_TOGGLE_NONE = 0;
    private static final boolean GOOGLE_WARNING_DISABLED = true;
    private static final boolean IS_DOCOMO = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    private static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", HealthServiceWrapper.INSTANCE_VENDOR));
    private static final boolean LOGD = NetworkPolicyLogger.LOGD;
    private static final boolean LOGV = NetworkPolicyLogger.LOGV;
    private static final int MSG_ADVISE_PERSIST_THRESHOLD = 7;
    private static final int MSG_LIMIT_REACHED = 5;
    private static final int MSG_METERED_IFACES_CHANGED = 2;
    private static final int MSG_METERED_RESTRICTED_PACKAGES_CHANGED = 17;
    private static final int MSG_POLICIES_CHANGED = 13;
    private static final int MSG_REMOVE_INTERFACE_QUOTA = 11;
    private static final int MSG_RESET_FIREWALL_RULES_BY_UID = 15;
    private static final int MSG_RESTRICT_BACKGROUND_CHANGED = 6;
    private static final int MSG_RULES_CHANGED = 1;
    private static final int MSG_SET_NETWORK_TEMPLATE_ENABLED = 18;
    private static final int MSG_SUBSCRIPTION_OVERRIDE = 16;
    private static final int MSG_UPDATE_INTERFACE_QUOTA = 10;
    public static final int OPPORTUNISTIC_QUOTA_UNKNOWN = -1;
    private static final String PROP_SUB_PLAN_OWNER = "persist.sys.sub_plan_owner";
    private static final float QUOTA_FRAC_JOBS_DEFAULT = 0.5f;
    private static final float QUOTA_FRAC_MULTIPATH_DEFAULT = 0.5f;
    private static final float QUOTA_LIMITED_DEFAULT = 0.1f;
    private static final long QUOTA_UNLIMITED_DEFAULT = DataUnit.MEBIBYTES.toBytes(20);
    static final String TAG = "NetworkPolicy";
    private static final String TAG_APP_POLICY = "app-policy";
    private static final String TAG_NETWORK_POLICY = "network-policy";
    private static final String TAG_POLICY_LIST = "policy-list";
    private static final String TAG_RESTRICT_BACKGROUND = "restrict-background";
    private static final String TAG_REVOKED_RESTRICT_BACKGROUND = "revoked-restrict-background";
    private static final String TAG_SUBSCRIPTION_PLAN = "subscription-plan";
    private static final String TAG_UID_POLICY = "uid-policy";
    private static final String TAG_WHITELIST = "whitelist";
    @VisibleForTesting
    public static final int TYPE_LIMIT = 35;
    @VisibleForTesting
    public static final int TYPE_LIMIT_SNOOZED = 36;
    @VisibleForTesting
    public static final int TYPE_RAPID = 45;
    private static final int TYPE_RESTRICT_BACKGROUND = 1;
    private static final int TYPE_RESTRICT_POWER = 2;
    @VisibleForTesting
    public static final int TYPE_WARNING = 34;
    private static final int UID_MSG_GONE = 101;
    private static final int UID_MSG_STATE_CHANGED = 100;
    private static final int VERSION_ADDED_CYCLE = 11;
    private static final int VERSION_ADDED_INFERRED = 7;
    private static final int VERSION_ADDED_METERED = 4;
    private static final int VERSION_ADDED_NETWORK_ID = 9;
    private static final int VERSION_ADDED_RESTRICT_BACKGROUND = 3;
    private static final int VERSION_ADDED_SNOOZE = 2;
    private static final int VERSION_ADDED_TIMEZONE = 6;
    private static final int VERSION_INIT = 1;
    private static final int VERSION_LATEST = 11;
    private static final int VERSION_SPLIT_SNOOZE = 5;
    private static final int VERSION_SWITCH_APP_ID = 8;
    private static final int VERSION_SWITCH_UID = 10;
    private static final long WAIT_FOR_ADMIN_DATA_TIMEOUT_MS = 10000;
    @GuardedBy("mNetworkPoliciesSecondLock")
    private final ArraySet<NotificationId> mActiveNotifs;
    private final IActivityManager mActivityManager;
    private ActivityManagerInternal mActivityManagerInternal;
    private final CountDownLatch mAdminDataAvailableLatch;
    private final INetworkManagementEventObserver mAlertObserver;
    private final BroadcastReceiver mAllowReceiver;
    private final AppOpsManager mAppOps;
    private final CarrierConfigManager mCarrierConfigManager;
    private BroadcastReceiver mCarrierConfigReceiver;
    private final Clock mClock;
    private IConnectivityManager mConnManager;
    private BroadcastReceiver mConnReceiver;
    private final Context mContext;
    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mDefaultRestrictBackgroundWhitelistUids;
    private IDeviceIdleController mDeviceIdleController;
    @GuardedBy("mUidRulesFirstLock")
    volatile boolean mDeviceIdleMode;
    @GuardedBy("mUidRulesFirstLock")
    final SparseBooleanArray mFirewallChainStates;
    final Handler mHandler;
    private final Callback mHandlerCallback;
    private IHwBehaviorCollectManager mHwBehaviorManager;
    private final IPackageManager mIPm;
    private final RemoteCallbackList<INetworkPolicyListener> mListeners;
    private boolean mLoadedRestrictBackground;
    private final NetworkPolicyLogger mLogger;
    @GuardedBy("mNetworkPoliciesSecondLock")
    private String[] mMergedSubscriberIds;
    @GuardedBy("mNetworkPoliciesSecondLock")
    private ArraySet<String> mMeteredIfaces;
    @GuardedBy("mUidRulesFirstLock")
    private final SparseArray<Set<Integer>> mMeteredRestrictedUids;
    @GuardedBy("mNetworkPoliciesSecondLock")
    private final SparseIntArray mNetIdToSubId;
    private final NetworkCallback mNetworkCallback;
    private final INetworkManagementService mNetworkManager;
    @GuardedBy("mNetworkPoliciesSecondLock")
    private final SparseBooleanArray mNetworkMetered;
    final Object mNetworkPoliciesSecondLock;
    @GuardedBy("mNetworkPoliciesSecondLock")
    final ArrayMap<NetworkTemplate, NetworkPolicy> mNetworkPolicy;
    @GuardedBy("mNetworkPoliciesSecondLock")
    private final SparseBooleanArray mNetworkRoaming;
    private NetworkStatsManagerInternal mNetworkStats;
    @GuardedBy("mNetworkPoliciesSecondLock")
    private final ArraySet<NetworkTemplate> mOverLimitNotified;
    private final BroadcastReceiver mPackageReceiver;
    @GuardedBy("allLocks")
    private final AtomicFile mPolicyFile;
    private PowerManagerInternal mPowerManagerInternal;
    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mPowerSaveTempWhitelistAppIds;
    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mPowerSaveWhitelistAppIds;
    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mPowerSaveWhitelistExceptIdleAppIds;
    private final BroadcastReceiver mPowerSaveWhitelistReceiver;
    @GuardedBy("mUidRulesFirstLock")
    volatile boolean mRestrictBackground;
    private boolean mRestrictBackgroundBeforeBsm;
    @GuardedBy("mUidRulesFirstLock")
    volatile boolean mRestrictBackgroundChangedInBsm;
    @GuardedBy("mUidRulesFirstLock")
    private PowerSaveState mRestrictBackgroundPowerState;
    @GuardedBy("mUidRulesFirstLock")
    private final SparseBooleanArray mRestrictBackgroundWhitelistRevokedUids;
    @GuardedBy("mUidRulesFirstLock")
    volatile boolean mRestrictPower;
    private final BroadcastReceiver mSnoozeReceiver;
    public final StatLogger mStatLogger;
    private final BroadcastReceiver mStatsReceiver;
    @GuardedBy("mNetworkPoliciesSecondLock")
    private final SparseArray<String> mSubIdToSubscriberId;
    @GuardedBy("mNetworkPoliciesSecondLock")
    final SparseLongArray mSubscriptionOpportunisticQuota;
    @GuardedBy("mNetworkPoliciesSecondLock")
    final SparseArray<SubscriptionPlan[]> mSubscriptionPlans;
    @GuardedBy("mNetworkPoliciesSecondLock")
    final SparseArray<String> mSubscriptionPlansOwner;
    private final boolean mSuppressDefaultPolicy;
    @GuardedBy("allLocks")
    volatile boolean mSystemReady;
    private long mTimeRefreshRealtime;
    @VisibleForTesting
    public final Handler mUidEventHandler;
    private final Callback mUidEventHandlerCallback;
    private final ServiceThread mUidEventThread;
    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidFirewallDozableRules;
    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidFirewallPowerSaveRules;
    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidFirewallStandbyRules;
    private final IUidObserver mUidObserver;
    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidPolicy;
    private final BroadcastReceiver mUidRemovedReceiver;
    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidRules;
    final Object mUidRulesFirstLock;
    @GuardedBy("mUidRulesFirstLock")
    final SparseIntArray mUidState;
    private UsageStatsManagerInternal mUsageStats;
    private final UserManager mUserManager;
    private final BroadcastReceiver mUserReceiver;
    private final BroadcastReceiver mWifiReceiver;

    private class AppIdleStateChangeListener extends android.app.usage.UsageStatsManagerInternal.AppIdleStateChangeListener {
        private AppIdleStateChangeListener() {
        }

        /* synthetic */ AppIdleStateChangeListener(NetworkPolicyManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            try {
                int uid = NetworkPolicyManagerService.this.mContext.getPackageManager().getPackageUidAsUser(packageName, 8192, userId);
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    NetworkPolicyManagerService.this.mLogger.appIdleStateChanged(uid, idle);
                    NetworkPolicyManagerService.this.updateRuleForAppIdleUL(uid);
                    NetworkPolicyManagerService.this.updateRulesForPowerRestrictionsUL(uid);
                }
            } catch (NameNotFoundException e) {
            }
        }

        public void onParoleStateChanged(boolean isParoleOn) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                NetworkPolicyManagerService.this.mLogger.paroleStateChanged(isParoleOn);
                NetworkPolicyManagerService.this.updateRulesForAppIdleParoleUL();
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ChainToggleType {
    }

    private class NotificationId {
        private final int mId;
        private final String mTag;

        NotificationId(NetworkPolicy policy, int type) {
            this.mTag = buildNotificationTag(policy, type);
            this.mId = type;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NotificationId)) {
                return false;
            }
            return Objects.equals(this.mTag, ((NotificationId) o).mTag);
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.mTag});
        }

        private String buildNotificationTag(NetworkPolicy policy, int type) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NetworkPolicy:");
            stringBuilder.append(policy.template.hashCode());
            stringBuilder.append(":");
            stringBuilder.append(type);
            return stringBuilder.toString();
        }

        public String getTag() {
            return this.mTag;
        }

        public int getId() {
            return this.mId;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RestrictType {
    }

    interface Stats {
        public static final int COUNT = 2;
        public static final int IS_UID_NETWORKING_BLOCKED = 1;
        public static final int UPDATE_NETWORK_ENABLED = 0;
    }

    private class NetworkPolicyManagerInternalImpl extends NetworkPolicyManagerInternal {
        private NetworkPolicyManagerInternalImpl() {
        }

        /* synthetic */ NetworkPolicyManagerInternalImpl(NetworkPolicyManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void resetUserState(int userId) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                boolean z = false;
                boolean changed = NetworkPolicyManagerService.this.removeUserStateUL(userId, false);
                if (NetworkPolicyManagerService.this.addDefaultRestrictBackgroundWhitelistUidsUL(userId) || changed) {
                    z = true;
                }
                if (z) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        NetworkPolicyManagerService.this.writePolicyAL();
                    }
                }
            }
        }

        public boolean isUidRestrictedOnMeteredNetworks(int uid) {
            boolean isBackgroundRestricted;
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                int uidRules = NetworkPolicyManagerService.this.mUidRules.get(uid, 32);
                isBackgroundRestricted = NetworkPolicyManagerService.this.mRestrictBackground;
            }
            if (!isBackgroundRestricted || NetworkPolicyManagerService.hasRule(uidRules, 1) || NetworkPolicyManagerService.hasRule(uidRules, 2)) {
                return false;
            }
            return true;
        }

        public boolean isUidNetworkingBlocked(int uid, String ifname) {
            boolean isNetworkMetered;
            long startTime = NetworkPolicyManagerService.this.mStatLogger.getTime();
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                isNetworkMetered = NetworkPolicyManagerService.this.mMeteredIfaces.contains(ifname);
            }
            boolean ret = NetworkPolicyManagerService.this.isUidNetworkingBlockedInternal(uid, isNetworkMetered);
            NetworkPolicyManagerService.this.mStatLogger.logDurationStat(1, startTime);
            return ret;
        }

        public void onTempPowerSaveWhitelistChange(int appId, boolean added) {
            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                NetworkPolicyManagerService.this.mLogger.tempPowerSaveWlChanged(appId, added);
                if (added) {
                    NetworkPolicyManagerService.this.mPowerSaveTempWhitelistAppIds.put(appId, true);
                } else {
                    NetworkPolicyManagerService.this.mPowerSaveTempWhitelistAppIds.delete(appId);
                }
                NetworkPolicyManagerService.this.updateRulesForTempWhitelistChangeUL(appId);
            }
        }

        public SubscriptionPlan getSubscriptionPlan(Network network) {
            SubscriptionPlan access$4000;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                access$4000 = NetworkPolicyManagerService.this.getPrimarySubscriptionPlanLocked(NetworkPolicyManagerService.this.getSubIdLocked(network));
            }
            return access$4000;
        }

        public SubscriptionPlan getSubscriptionPlan(NetworkTemplate template) {
            SubscriptionPlan access$4000;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                access$4000 = NetworkPolicyManagerService.this.getPrimarySubscriptionPlanLocked(NetworkPolicyManagerService.this.findRelevantSubIdNL(template));
            }
            return access$4000;
        }

        public long getSubscriptionOpportunisticQuota(Network network, int quotaType) {
            long quotaBytes;
            synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                quotaBytes = NetworkPolicyManagerService.this.mSubscriptionOpportunisticQuota.get(NetworkPolicyManagerService.this.getSubIdLocked(network), -1);
            }
            if (quotaBytes == -1) {
                return -1;
            }
            if (quotaType == 1) {
                return (long) (((float) quotaBytes) * Global.getFloat(NetworkPolicyManagerService.this.mContext.getContentResolver(), "netpolicy_quota_frac_jobs", 0.5f));
            }
            if (quotaType == 2) {
                return (long) (((float) quotaBytes) * Global.getFloat(NetworkPolicyManagerService.this.mContext.getContentResolver(), "netpolicy_quota_frac_multipath", 0.5f));
            }
            return -1;
        }

        public void onAdminDataAvailable() {
            NetworkPolicyManagerService.this.mAdminDataAvailableLatch.countDown();
        }

        public void setMeteredRestrictedPackages(Set<String> packageNames, int userId) {
            NetworkPolicyManagerService.this.setMeteredRestrictedPackagesInternal(packageNames, userId);
        }

        public void setMeteredRestrictedPackagesAsync(Set<String> packageNames, int userId) {
            NetworkPolicyManagerService.this.mHandler.obtainMessage(17, userId, 0, packageNames).sendToTarget();
        }
    }

    public NetworkPolicyManagerService(Context context, IActivityManager activityManager, INetworkManagementService networkManagement) {
        this(context, activityManager, networkManagement, AppGlobals.getPackageManager(), getDefaultClock(), getDefaultSystemDir(), false);
    }

    private static File getDefaultSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    private static Clock getDefaultClock() {
        return new BestClock(ZoneOffset.UTC, new Clock[]{SystemClock.currentNetworkTimeClock(), Clock.systemUTC()});
    }

    private static boolean isNeedShowWarning() {
        return IS_DOCOMO && IS_TABLET;
    }

    public NetworkPolicyManagerService(Context context, IActivityManager activityManager, INetworkManagementService networkManagement, IPackageManager pm, Clock clock, File systemDir, boolean suppressDefaultPolicy) {
        this.mUidRulesFirstLock = new Object();
        this.mNetworkPoliciesSecondLock = new Object();
        this.mAdminDataAvailableLatch = new CountDownLatch(1);
        this.mNetworkPolicy = new ArrayMap();
        this.mSubscriptionPlans = new SparseArray();
        this.mSubscriptionPlansOwner = new SparseArray();
        this.mSubscriptionOpportunisticQuota = new SparseLongArray();
        this.mUidPolicy = new SparseIntArray();
        this.mUidRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mPowerSaveWhitelistExceptIdleAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistAppIds = new SparseBooleanArray();
        this.mPowerSaveTempWhitelistAppIds = new SparseBooleanArray();
        this.mDefaultRestrictBackgroundWhitelistUids = new SparseBooleanArray();
        this.mRestrictBackgroundWhitelistRevokedUids = new SparseBooleanArray();
        this.mMeteredIfaces = new ArraySet();
        this.mOverLimitNotified = new ArraySet();
        this.mActiveNotifs = new ArraySet();
        this.mUidState = new SparseIntArray();
        this.mNetworkMetered = new SparseBooleanArray();
        this.mNetworkRoaming = new SparseBooleanArray();
        this.mNetIdToSubId = new SparseIntArray();
        this.mSubIdToSubscriberId = new SparseArray();
        this.mMergedSubscriberIds = EmptyArray.STRING;
        this.mMeteredRestrictedUids = new SparseArray();
        this.mListeners = new RemoteCallbackList();
        this.mLogger = new NetworkPolicyLogger();
        this.mStatLogger = new StatLogger(new String[]{"updateNetworkEnabledNL()", "isUidNetworkingBlocked()"});
        this.mUidObserver = new IUidObserver.Stub() {
            public void onUidStateChanged(int uid, int procState, long procStateSeq) {
                NetworkPolicyManagerService.this.mUidEventHandler.obtainMessage(100, uid, procState, Long.valueOf(procStateSeq)).sendToTarget();
            }

            public void onUidGone(int uid, boolean disabled) {
                NetworkPolicyManagerService.this.mUidEventHandler.obtainMessage(101, uid, 0).sendToTarget();
            }

            public void onUidActive(int uid) {
            }

            public void onUidIdle(int uid, boolean disabled) {
            }

            public void onUidCachedChanged(int uid, boolean cached) {
            }
        };
        this.mPowerSaveWhitelistReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    NetworkPolicyManagerService.this.updatePowerSaveWhitelistUL();
                    NetworkPolicyManagerService.this.updateRulesForRestrictPowerUL();
                    NetworkPolicyManagerService.this.updateRulesForAppIdleUL();
                }
            }
        };
        this.mPackageReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (uid != -1 && "android.intent.action.PACKAGE_ADDED".equals(action)) {
                    if (NetworkPolicyManagerService.LOGV) {
                        String str = NetworkPolicyManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ACTION_PACKAGE_ADDED for uid=");
                        stringBuilder.append(uid);
                        Slog.v(str, stringBuilder.toString());
                    }
                    synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                        NetworkPolicyManagerService.this.updateRestrictionRulesForUidUL(uid);
                    }
                }
            }
        };
        this.mUidRemovedReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (uid != -1) {
                    if (NetworkPolicyManagerService.LOGV) {
                        String str = NetworkPolicyManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ACTION_UID_REMOVED for uid=");
                        stringBuilder.append(uid);
                        Slog.v(str, stringBuilder.toString());
                    }
                    synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                        NetworkPolicyManagerService.this.onUidDeletedUL(uid);
                        synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                            NetworkPolicyManagerService.this.writePolicyAL();
                        }
                    }
                }
            }
        };
        this.mUserReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int i = -1;
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId != -1) {
                    int hashCode = action.hashCode();
                    if (hashCode != -2061058799) {
                        if (hashCode == 1121780209 && action.equals("android.intent.action.USER_ADDED")) {
                            i = 1;
                        }
                    } else if (action.equals("android.intent.action.USER_REMOVED")) {
                        i = 0;
                    }
                    switch (i) {
                        case 0:
                        case 1:
                            synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                                NetworkPolicyManagerService.this.removeUserStateUL(userId, true);
                                NetworkPolicyManagerService.this.mMeteredRestrictedUids.remove(userId);
                                if (action == "android.intent.action.USER_ADDED") {
                                    NetworkPolicyManagerService.this.addDefaultRestrictBackgroundWhitelistUidsUL(userId);
                                }
                                synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                                    NetworkPolicyManagerService.this.updateRulesForGlobalChangeAL(true);
                                }
                            }
                    }
                }
            }
        };
        this.mStatsReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                    NetworkPolicyManagerService.this.updateNetworkEnabledNL();
                    NetworkPolicyManagerService.this.updateNotificationsNL();
                }
            }
        };
        this.mAllowReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                NetworkPolicyManagerService.this.setRestrictBackground(false);
            }
        };
        this.mSnoozeReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                NetworkTemplate template = (NetworkTemplate) intent.getParcelableExtra("android.net.NETWORK_TEMPLATE");
                if (NetworkPolicyManagerService.ACTION_SNOOZE_WARNING.equals(intent.getAction())) {
                    NetworkPolicyManagerService.this.performSnooze(template, 34);
                } else if (NetworkPolicyManagerService.ACTION_SNOOZE_RAPID.equals(intent.getAction())) {
                    NetworkPolicyManagerService.this.performSnooze(template, 45);
                }
            }
        };
        this.mWifiReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        NetworkPolicyManagerService.this.upgradeWifiMeteredOverrideAL();
                    }
                }
                NetworkPolicyManagerService.this.mContext.unregisterReceiver(this);
            }
        };
        this.mNetworkCallback = new NetworkCallback() {
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                if (network != null && networkCapabilities != null) {
                    synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                        boolean newMetered = networkCapabilities.hasCapability(11) ^ 1;
                        boolean meteredChanged = NetworkPolicyManagerService.updateCapabilityChange(NetworkPolicyManagerService.this.mNetworkMetered, newMetered, network);
                        boolean roamingChanged = NetworkPolicyManagerService.updateCapabilityChange(NetworkPolicyManagerService.this.mNetworkRoaming, networkCapabilities.hasCapability(18) ^ 1, network);
                        if (meteredChanged || roamingChanged) {
                            NetworkPolicyManagerService.this.mLogger.meterednessChanged(network.netId, newMetered);
                            NetworkPolicyManagerService.this.updateNetworkRulesNL();
                        }
                    }
                }
            }
        };
        this.mAlertObserver = new BaseNetworkObserver() {
            public void limitReached(String limitName, String iface) {
                NetworkPolicyManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", NetworkPolicyManagerService.TAG);
                if (!NetworkManagementService.LIMIT_GLOBAL_ALERT.equals(limitName)) {
                    NetworkPolicyManagerService.this.mHandler.obtainMessage(5, iface).sendToTarget();
                }
            }
        };
        this.mConnReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                NetworkPolicyManagerService.this.updateNetworksInternal();
            }
        };
        this.mCarrierConfigReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra("subscription")) {
                    int subId = intent.getIntExtra("subscription", -1);
                    NetworkPolicyManagerService.this.updateSubscriptions();
                    synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                        synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                            String subscriberId = (String) NetworkPolicyManagerService.this.mSubIdToSubscriberId.get(subId, null);
                            if (subscriberId != null) {
                                NetworkPolicyManagerService.this.ensureActiveMobilePolicyAL(subId, subscriberId);
                                NetworkPolicyManagerService.this.maybeUpdateMobilePolicyCycleAL(subId, subscriberId);
                            } else {
                                String str = NetworkPolicyManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Missing subscriberId for subId ");
                                stringBuilder.append(subId);
                                Slog.wtf(str, stringBuilder.toString());
                            }
                            NetworkPolicyManagerService.this.handleNetworkPoliciesUpdateAL(true);
                        }
                    }
                }
            }
        };
        this.mHandlerCallback = new Callback() {
            public boolean handleMessage(Message msg) {
                int i = 0;
                int uid;
                int uidRules;
                int length;
                int length2;
                switch (msg.what) {
                    case 1:
                        uid = msg.arg1;
                        uidRules = msg.arg2;
                        length = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        while (i < length) {
                            NetworkPolicyManagerService.this.dispatchUidRulesChanged((INetworkPolicyListener) NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i), uid, uidRules);
                            i++;
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 2:
                        String[] meteredIfaces = msg.obj;
                        uidRules = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        while (i < uidRules) {
                            NetworkPolicyManagerService.this.dispatchMeteredIfacesChanged((INetworkPolicyListener) NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i), meteredIfaces);
                            i++;
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 5:
                        String iface = msg.obj;
                        synchronized (NetworkPolicyManagerService.this.mNetworkPoliciesSecondLock) {
                            if (NetworkPolicyManagerService.this.mMeteredIfaces.contains(iface)) {
                                NetworkPolicyManagerService.this.mNetworkStats.forceUpdate();
                                NetworkPolicyManagerService.this.updateNetworkEnabledNL();
                                NetworkPolicyManagerService.this.updateNotificationsNL();
                            }
                        }
                        return true;
                    case 6:
                        boolean restrictBackground = msg.arg1 != 0;
                        uidRules = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        while (i < uidRules) {
                            NetworkPolicyManagerService.this.dispatchRestrictBackgroundChanged((INetworkPolicyListener) NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i), restrictBackground);
                            i++;
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        Intent intent = new Intent("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
                        intent.setFlags(1073741824);
                        NetworkPolicyManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                        return true;
                    case 7:
                        NetworkPolicyManagerService.this.mNetworkStats.advisePersistThreshold(((Long) msg.obj).longValue() / 1000);
                        return true;
                    case 10:
                        NetworkPolicyManagerService.this.removeInterfaceQuota((String) msg.obj);
                        NetworkPolicyManagerService.this.setInterfaceQuota((String) msg.obj, (((long) msg.arg1) << 32) | (((long) msg.arg2) & 4294967295L));
                        return true;
                    case 11:
                        NetworkPolicyManagerService.this.removeInterfaceQuota((String) msg.obj);
                        return true;
                    case 13:
                        uid = msg.arg1;
                        uidRules = msg.arg2;
                        Boolean notifyApp = msg.obj;
                        length2 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        while (i < length2) {
                            NetworkPolicyManagerService.this.dispatchUidPoliciesChanged((INetworkPolicyListener) NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i), uid, uidRules);
                            i++;
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        if (notifyApp.booleanValue()) {
                            NetworkPolicyManagerService.this.broadcastRestrictBackgroundChanged(uid, notifyApp);
                        }
                        return true;
                    case 15:
                        NetworkPolicyManagerService.this.resetUidFirewallRules(msg.arg1);
                        return true;
                    case 16:
                        uid = msg.arg1;
                        uidRules = msg.arg2;
                        length = ((Integer) msg.obj).intValue();
                        length2 = NetworkPolicyManagerService.this.mListeners.beginBroadcast();
                        while (i < length2) {
                            NetworkPolicyManagerService.this.dispatchSubscriptionOverride((INetworkPolicyListener) NetworkPolicyManagerService.this.mListeners.getBroadcastItem(i), length, uid, uidRules);
                            i++;
                        }
                        NetworkPolicyManagerService.this.mListeners.finishBroadcast();
                        return true;
                    case 17:
                        NetworkPolicyManagerService.this.setMeteredRestrictedPackagesInternal(msg.obj, msg.arg1);
                        return true;
                    case 18:
                        boolean enabled;
                        NetworkTemplate template = msg.obj;
                        if (msg.arg1 != 0) {
                            enabled = true;
                        }
                        NetworkPolicyManagerService.this.setNetworkTemplateEnabledInner(template, enabled);
                        return true;
                    default:
                        return false;
                }
            }
        };
        this.mUidEventHandlerCallback = new Callback() {
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 100:
                        NetworkPolicyManagerService.this.handleUidChanged(msg.arg1, msg.arg2, ((Long) msg.obj).longValue());
                        return true;
                    case 101:
                        NetworkPolicyManagerService.this.handleUidGone(msg.arg1);
                        return true;
                    default:
                        return false;
                }
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing context");
        this.mActivityManager = (IActivityManager) Preconditions.checkNotNull(activityManager, "missing activityManager");
        this.mNetworkManager = (INetworkManagementService) Preconditions.checkNotNull(networkManagement, "missing networkManagement");
        this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        this.mClock = (Clock) Preconditions.checkNotNull(clock, "missing Clock");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mCarrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService(CarrierConfigManager.class);
        this.mIPm = pm;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new Handler(thread.getLooper(), this.mHandlerCallback);
        this.mUidEventThread = new ServiceThread("NetworkPolicy.uid", -2, false);
        this.mUidEventThread.start();
        this.mUidEventHandler = new Handler(this.mUidEventThread.getLooper(), this.mUidEventHandlerCallback);
        this.mSuppressDefaultPolicy = suppressDefaultPolicy;
        this.mPolicyFile = new AtomicFile(new File(systemDir, "netpolicy.xml"), "net-policy");
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        LocalServices.addService(NetworkPolicyManagerInternal.class, new NetworkPolicyManagerInternalImpl(this, null));
    }

    public void bindConnectivityManager(IConnectivityManager connManager) {
        this.mConnManager = (IConnectivityManager) Preconditions.checkNotNull(connManager, "missing IConnectivityManager");
    }

    void updatePowerSaveWhitelistUL() {
        try {
            int length;
            int[] whitelist = this.mDeviceIdleController.getAppIdWhitelistExceptIdle();
            this.mPowerSaveWhitelistExceptIdleAppIds.clear();
            int i = 0;
            if (whitelist != null) {
                for (int uid : whitelist) {
                    this.mPowerSaveWhitelistExceptIdleAppIds.put(uid, true);
                }
            }
            whitelist = this.mDeviceIdleController.getAppIdWhitelist();
            this.mPowerSaveWhitelistAppIds.clear();
            if (whitelist != null) {
                length = whitelist.length;
                while (i < length) {
                    this.mPowerSaveWhitelistAppIds.put(whitelist[i], true);
                    i++;
                }
            }
        } catch (RemoteException e) {
        }
    }

    boolean addDefaultRestrictBackgroundWhitelistUidsUL() {
        List<UserInfo> users = this.mUserManager.getUsers();
        int numberUsers = users.size();
        boolean changed = false;
        for (int i = 0; i < numberUsers; i++) {
            boolean z = addDefaultRestrictBackgroundWhitelistUidsUL(((UserInfo) users.get(i)).id) || changed;
            changed = z;
        }
        return changed;
    }

    private boolean addDefaultRestrictBackgroundWhitelistUidsUL(int userId) {
        SystemConfig sysConfig = SystemConfig.getInstance();
        PackageManager pm = this.mContext.getPackageManager();
        ArraySet<String> allowDataUsage = sysConfig.getAllowInDataUsageSave();
        boolean changed = false;
        for (int i = 0; i < allowDataUsage.size(); i++) {
            String pkg = (String) allowDataUsage.valueAt(i);
            if (LOGD) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checking restricted background whitelisting for package ");
                stringBuilder.append(pkg);
                stringBuilder.append(" and user ");
                stringBuilder.append(userId);
                Slog.d(str, stringBuilder.toString());
            }
            String str2;
            StringBuilder stringBuilder2;
            try {
                ApplicationInfo app = pm.getApplicationInfoAsUser(pkg, DumpState.DUMP_DEXOPT, userId);
                if (app.isPrivilegedApp()) {
                    String str3;
                    StringBuilder stringBuilder3;
                    int uid = UserHandle.getUid(userId, app.uid);
                    this.mDefaultRestrictBackgroundWhitelistUids.append(uid, true);
                    if (LOGD) {
                        str3 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Adding uid ");
                        stringBuilder3.append(uid);
                        stringBuilder3.append(" (user ");
                        stringBuilder3.append(userId);
                        stringBuilder3.append(") to default restricted background whitelist. Revoked status: ");
                        stringBuilder3.append(this.mRestrictBackgroundWhitelistRevokedUids.get(uid));
                        Slog.d(str3, stringBuilder3.toString());
                    }
                    if (!this.mRestrictBackgroundWhitelistRevokedUids.get(uid)) {
                        if (LOGD) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("adding default package ");
                            stringBuilder3.append(pkg);
                            stringBuilder3.append(" (uid ");
                            stringBuilder3.append(uid);
                            stringBuilder3.append(" for user ");
                            stringBuilder3.append(userId);
                            stringBuilder3.append(") to restrict background whitelist");
                            Slog.d(str3, stringBuilder3.toString());
                        }
                        setUidPolicyUncheckedUL(uid, 4, false);
                        changed = true;
                    }
                } else {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("addDefaultRestrictBackgroundWhitelistUidsUL(): skipping non-privileged app  ");
                    stringBuilder2.append(pkg);
                    Slog.e(str2, stringBuilder2.toString());
                }
            } catch (NameNotFoundException e) {
                if (LOGD) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("No ApplicationInfo for package ");
                    stringBuilder2.append(pkg);
                    Slog.d(str2, stringBuilder2.toString());
                }
            }
        }
        return changed;
    }

    private void initService(CountDownLatch initCompleteSignal) {
        Trace.traceBegin(2097152, "systemReady");
        int oldPriority = Process.getThreadPriority(Process.myTid());
        try {
            Process.setThreadPriority(-2);
            if (isBandwidthControlEnabled()) {
                this.mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
                this.mNetworkStats = (NetworkStatsManagerInternal) LocalServices.getService(NetworkStatsManagerInternal.class);
                synchronized (this.mUidRulesFirstLock) {
                    synchronized (this.mNetworkPoliciesSecondLock) {
                        updatePowerSaveWhitelistUL();
                        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
                        this.mPowerManagerInternal.registerLowPowerModeObserver(new LowPowerModeListener() {
                            public int getServiceType() {
                                return 6;
                            }

                            public void onLowPowerModeChanged(PowerSaveState result) {
                                boolean enabled = result.batterySaverEnabled;
                                if (NetworkPolicyManagerService.LOGD) {
                                    String str = NetworkPolicyManagerService.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("onLowPowerModeChanged(");
                                    stringBuilder.append(enabled);
                                    stringBuilder.append(")");
                                    Slog.d(str, stringBuilder.toString());
                                }
                                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                                    if (NetworkPolicyManagerService.this.mRestrictPower != enabled) {
                                        NetworkPolicyManagerService.this.mRestrictPower = enabled;
                                        NetworkPolicyManagerService.this.updateRulesForRestrictPowerUL();
                                    }
                                }
                            }
                        });
                        this.mRestrictPower = this.mPowerManagerInternal.getLowPowerState(6).batterySaverEnabled;
                        this.mSystemReady = true;
                        waitForAdminData();
                        readPolicyAL();
                        this.mRestrictBackgroundBeforeBsm = this.mLoadedRestrictBackground;
                        this.mRestrictBackgroundPowerState = this.mPowerManagerInternal.getLowPowerState(10);
                        if (this.mRestrictBackgroundPowerState.batterySaverEnabled && !this.mLoadedRestrictBackground) {
                            this.mLoadedRestrictBackground = true;
                        }
                        this.mPowerManagerInternal.registerLowPowerModeObserver(new LowPowerModeListener() {
                            public int getServiceType() {
                                return 10;
                            }

                            public void onLowPowerModeChanged(PowerSaveState result) {
                                synchronized (NetworkPolicyManagerService.this.mUidRulesFirstLock) {
                                    NetworkPolicyManagerService.this.updateRestrictBackgroundByLowPowerModeUL(result);
                                }
                            }
                        });
                        if (addDefaultRestrictBackgroundWhitelistUidsUL()) {
                            writePolicyAL();
                        }
                        setRestrictBackgroundUL(this.mLoadedRestrictBackground);
                        updateRulesForGlobalChangeAL(false);
                        updateNotificationsNL();
                    }
                }
                this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
                try {
                    this.mActivityManager.registerUidObserver(this.mUidObserver, 3, -1, null);
                    this.mNetworkManager.registerObserver(this.mAlertObserver);
                } catch (RemoteException e) {
                }
                this.mContext.registerReceiver(this.mPowerSaveWhitelistReceiver, new IntentFilter("android.os.action.POWER_SAVE_WHITELIST_CHANGED"), null, this.mHandler);
                this.mContext.registerReceiver(this.mConnReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"), "android.permission.CONNECTIVITY_INTERNAL", this.mHandler);
                IntentFilter packageFilter = new IntentFilter();
                packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
                packageFilter.addDataScheme("package");
                this.mContext.registerReceiver(this.mPackageReceiver, packageFilter, null, this.mHandler);
                this.mContext.registerReceiver(this.mUidRemovedReceiver, new IntentFilter("android.intent.action.UID_REMOVED"), null, this.mHandler);
                IntentFilter userFilter = new IntentFilter();
                userFilter.addAction("android.intent.action.USER_ADDED");
                userFilter.addAction("android.intent.action.USER_REMOVED");
                this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, this.mHandler);
                this.mContext.registerReceiver(this.mStatsReceiver, new IntentFilter(NetworkStatsService.ACTION_NETWORK_STATS_UPDATED), "android.permission.READ_NETWORK_USAGE_HISTORY", this.mHandler);
                this.mContext.registerReceiver(this.mAllowReceiver, new IntentFilter(ACTION_ALLOW_BACKGROUND), "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
                this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter(ACTION_SNOOZE_WARNING), "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
                this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter(ACTION_SNOOZE_RAPID), "android.permission.MANAGE_NETWORK_POLICY", this.mHandler);
                this.mContext.registerReceiver(this.mWifiReceiver, new IntentFilter("android.net.wifi.CONFIGURED_NETWORKS_CHANGE"), null, this.mHandler);
                this.mContext.registerReceiver(this.mCarrierConfigReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"), null, this.mHandler);
                ((ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class)).registerNetworkCallback(new Builder().build(), this.mNetworkCallback);
                this.mUsageStats.addAppIdleStateChangeListener(new AppIdleStateChangeListener(this, null));
                ((SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class)).addOnSubscriptionsChangedListener(new OnSubscriptionsChangedListener(this.mHandler.getLooper()) {
                    public void onSubscriptionsChanged() {
                        NetworkPolicyManagerService.this.updateNetworksInternal();
                    }
                });
                initCompleteSignal.countDown();
                Process.setThreadPriority(oldPriority);
                Trace.traceEnd(2097152);
                return;
            }
            Slog.w(TAG, "bandwidth controls disabled, unable to enforce policy");
        } finally {
            Process.setThreadPriority(oldPriority);
            Trace.traceEnd(2097152);
        }
    }

    public CountDownLatch networkScoreAndNetworkManagementServiceReady() {
        CountDownLatch initCompleteSignal = new CountDownLatch(1);
        this.mHandler.post(new -$$Lambda$NetworkPolicyManagerService$HDTUqowtgL-W_V0Kq6psXLWC9ws(this, initCompleteSignal));
        return initCompleteSignal;
    }

    public void systemReady(CountDownLatch initCompleteSignal) {
        try {
            if (!initCompleteSignal.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Service NetworkPolicy init timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Service NetworkPolicy init interrupted", e);
        }
    }

    private static boolean updateCapabilityChange(SparseBooleanArray lastValues, boolean newValue, Network network) {
        boolean changed = false;
        if (lastValues.get(network.netId, false) != newValue || lastValues.indexOfKey(network.netId) < 0) {
            changed = true;
        }
        if (changed) {
            lastValues.put(network.netId, newValue);
        }
        return changed;
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x00ce  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void updateNotificationsNL() {
        boolean z;
        if (LOGV) {
            Slog.v(TAG, "updateNotificationsNL()");
        }
        Trace.traceBegin(2097152, "updateNotificationsNL");
        ArraySet<NotificationId> beforeNotifs = new ArraySet(this.mActiveNotifs);
        this.mActiveNotifs.clear();
        long now = this.mClock.millis();
        boolean z2 = true;
        int i = this.mNetworkPolicy.size() - 1;
        while (true) {
            int i2 = i;
            if (i2 < 0) {
                break;
            }
            int i3;
            NetworkPolicy policy = (NetworkPolicy) this.mNetworkPolicy.valueAt(i2);
            int subId = findRelevantSubIdNL(policy.template);
            if (subId != -1 && policy.hasCycle()) {
                long totalBytes;
                Pair<ZonedDateTime, ZonedDateTime> cycle = (Pair) NetworkPolicyManager.cycleIterator(policy).next();
                long cycleStart = ((ZonedDateTime) cycle.first).toInstant().toEpochMilli();
                long cycleEnd = ((ZonedDateTime) cycle.second).toInstant().toEpochMilli();
                long totalBytes2 = getTotalBytes(policy.template, cycleStart, cycleEnd);
                PersistableBundle config = this.mCarrierConfigManager.getConfigForSubId(subId);
                boolean notifyWarning = getBooleanDefeatingNullable(config, "data_warning_notification_bool", z2);
                boolean notifyLimit = getBooleanDefeatingNullable(config, "data_limit_notification_bool", z2);
                boolean notifyRapid = getBooleanDefeatingNullable(config, "data_rapid_notification_bool", z2);
                boolean snoozedRecently = false;
                if (notifyWarning && policy.isOverWarning(totalBytes2) && !policy.isOverLimit(totalBytes2)) {
                    if (!(policy.lastWarningSnooze >= cycleStart ? z2 : false)) {
                        totalBytes = totalBytes2;
                        enqueueNotification(policy, 34, totalBytes2, null);
                        if (notifyLimit) {
                            if (policy.isOverLimit(totalBytes)) {
                                if (policy.lastLimitSnooze >= cycleStart ? z2 : false) {
                                    enqueueNotification(policy, 36, totalBytes, null);
                                } else {
                                    enqueueNotification(policy, 35, totalBytes, null);
                                    notifyOverLimitNL(policy.template);
                                }
                            } else {
                                notifyUnderLimitNL(policy.template);
                            }
                        }
                        if (notifyRapid || policy.limitBytes == -1) {
                            z = z2;
                            i3 = i2;
                        } else {
                            long recentDuration = TimeUnit.DAYS.toMillis(4);
                            long recentStart = now - recentDuration;
                            long recentEnd = now;
                            long recentBytes = getTotalBytes(policy.template, recentStart, recentEnd);
                            long cycleDuration = cycleEnd - cycleStart;
                            cycleDuration = (recentBytes * cycleDuration) / recentDuration;
                            int i4 = i2;
                            long alertBytes = (policy.limitBytes * 3) / 2;
                            Pair<ZonedDateTime, ZonedDateTime> cycle2;
                            if (LOGD) {
                                String str = TAG;
                                cycle2 = cycle;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Rapid usage considering recent ");
                                stringBuilder.append(recentBytes);
                                stringBuilder.append(" projected ");
                                stringBuilder.append(cycleDuration);
                                stringBuilder.append(" alert ");
                                stringBuilder.append(alertBytes);
                                Slog.d(str, stringBuilder.toString());
                            } else {
                                cycle2 = cycle;
                                int i5 = subId;
                            }
                            if (policy.lastRapidSnooze >= now - 86400000) {
                                snoozedRecently = true;
                            }
                            if (cycleDuration <= alertBytes || snoozedRecently) {
                                i3 = i4;
                                z = true;
                            } else {
                                i3 = i4;
                                z = true;
                                enqueueNotification(policy, 45, 0, findRapidBlame(policy.template, recentStart, recentEnd));
                            }
                        }
                    }
                }
                totalBytes = totalBytes2;
                if (notifyLimit) {
                }
                if (notifyRapid) {
                }
                z = z2;
                i3 = i2;
            } else {
                z = z2;
                i3 = i2;
            }
            i = i3 - 1;
            z2 = z;
        }
        z = z2;
        for (i = beforeNotifs.size() - 1; i >= 0; i--) {
            NotificationId notificationId = (NotificationId) beforeNotifs.valueAt(i);
            if (!this.mActiveNotifs.contains(notificationId)) {
                cancelNotification(notificationId);
            }
        }
        Trace.traceEnd(2097152);
    }

    private ApplicationInfo findRapidBlame(NetworkTemplate template, long start, long end) {
        long totalBytes = 0;
        long maxBytes = 0;
        NetworkStats stats = getNetworkUidBytes(template, start, end);
        Entry entry = null;
        int maxUid = 0;
        for (int i = 0; i < stats.size(); i++) {
            entry = stats.getValues(i, entry);
            long bytes = entry.rxBytes + entry.txBytes;
            totalBytes += bytes;
            if (bytes > maxBytes) {
                maxBytes = bytes;
                maxUid = entry.uid;
            }
        }
        if (maxBytes > 0 && maxBytes > totalBytes / 2) {
            String[] packageNames = this.mContext.getPackageManager().getPackagesForUid(maxUid);
            if (packageNames != null && packageNames.length == 1) {
                try {
                    return this.mContext.getPackageManager().getApplicationInfo(packageNames[0], 4989440);
                } catch (NameNotFoundException e) {
                }
            }
        }
        return null;
    }

    private int findRelevantSubIdNL(NetworkTemplate template) {
        for (int i = 0; i < this.mSubIdToSubscriberId.size(); i++) {
            int subId = this.mSubIdToSubscriberId.keyAt(i);
            if (template.matches(new NetworkIdentity(0, 0, (String) this.mSubIdToSubscriberId.valueAt(i), null, false, true, true))) {
                return subId;
            }
        }
        return -1;
    }

    private void notifyOverLimitNL(NetworkTemplate template) {
        if (!this.mOverLimitNotified.contains(template)) {
            this.mContext.startActivity(buildNetworkOverLimitIntent(this.mContext.getResources(), template));
            this.mOverLimitNotified.add(template);
        }
    }

    private void notifyUnderLimitNL(NetworkTemplate template) {
        this.mOverLimitNotified.remove(template);
    }

    private void enqueueNotification(NetworkPolicy policy, int type, long totalBytes, ApplicationInfo rapidBlame) {
        CharSequence title;
        CharSequence body;
        CharSequence title2;
        NetworkPolicy networkPolicy = policy;
        int i = type;
        long j = totalBytes;
        ApplicationInfo applicationInfo = rapidBlame;
        NotificationId notificationId = new NotificationId(networkPolicy, i);
        Notification.Builder builder = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_ALERTS);
        builder.setOnlyAlertOnce(true);
        builder.setWhen(0);
        builder.setColor(this.mContext.getColor(17170784));
        Resources res = this.mContext.getResources();
        if (i != 45) {
            Bitmap bmp1;
            switch (i) {
                case 34:
                    if (isNeedShowWarning()) {
                        int i2;
                        title = res.getText(17039886);
                        body = res.getString(17039885, new Object[]{Formatter.formatFileSize(this.mContext, j)});
                        builder.setSmallIcon(17301624);
                        bmp1 = BitmapFactory.decodeResource(res, 33751681);
                        if (bmp1 != null) {
                            builder.setLargeIcon(bmp1);
                        }
                        builder.setTicker(title);
                        builder.setContentTitle(title);
                        builder.setContentText(body);
                        builder.setDefaults(-1);
                        builder.setChannelId(SystemNotificationChannels.NETWORK_ALERTS);
                        if (isNeedShowWarning()) {
                            i2 = 134217728;
                            builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, buildSnoozeWarningIntent(networkPolicy.template), 134217728));
                        } else {
                            i2 = 134217728;
                        }
                        builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, buildViewDataUsageIntent(res, networkPolicy.template), i2));
                        if (!isNeedShowWarning()) {
                            return;
                        }
                    }
                    if (LOGV) {
                        Slog.v(TAG, "google warning disabled ,don't show notification");
                    }
                    return;
                    break;
                case 35:
                    i = networkPolicy.template.getMatchRule();
                    if (i == 1) {
                        title2 = res.getText(17039879);
                    } else if (i == 4) {
                        title2 = res.getText(17039888);
                    } else {
                        return;
                    }
                    title = title2;
                    body = res.getText(17039876);
                    builder.setOngoing(true);
                    bmp1 = BitmapFactory.decodeResource(res, 33751679);
                    if (bmp1 != null) {
                        builder.setLargeIcon(bmp1);
                    }
                    builder.setSmallIcon(17303472);
                    builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, buildNetworkOverLimitIntent(res, networkPolicy.template), 134217728));
                    break;
                case 36:
                    int matchRule = networkPolicy.template.getMatchRule();
                    if (matchRule == 1) {
                        title = res.getText(17039878);
                    } else if (matchRule == 4) {
                        title = res.getText(17039887);
                    } else {
                        return;
                    }
                    long overBytes = j - networkPolicy.limitBytes;
                    body = res.getString(17039877, new Object[]{Formatter.formatFileSize(this.mContext, overBytes)});
                    builder.setOngoing(true);
                    builder.setSmallIcon(17301624);
                    Bitmap bmp3 = BitmapFactory.decodeResource(res, 33751681);
                    if (bmp3 != null) {
                        builder.setLargeIcon(bmp3);
                    }
                    builder.setChannelId(SystemNotificationChannels.NETWORK_STATUS);
                    CharSequence body2 = body;
                    builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, buildViewDataUsageIntent(res, networkPolicy.template), 134217728));
                    if (isNeedShowWarning()) {
                        body = body2;
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
        title = res.getText(17039882);
        if (applicationInfo != null) {
            title2 = res.getString(17039880, new Object[]{applicationInfo.loadLabel(this.mContext.getPackageManager())});
        } else {
            title2 = res.getString(17039881);
        }
        body = title2;
        builder.setSmallIcon(17301624);
        builder.setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, buildSnoozeRapidIntent(networkPolicy.template), 134217728));
        builder.setContentIntent(PendingIntent.getActivity(this.mContext, 0, buildViewDataUsageIntent(res, networkPolicy.template), 134217728));
        title2 = title;
        builder.setTicker(title2);
        builder.setContentTitle(title2);
        builder.setContentText(body);
        builder.setStyle(new BigTextStyle().bigText(body));
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notifyAsUser(notificationId.getTag(), notificationId.getId(), builder.build(), UserHandle.ALL);
        this.mActiveNotifs.add(notificationId);
    }

    private void cancelNotification(NotificationId notificationId) {
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancel(notificationId.getTag(), notificationId.getId());
    }

    private void updateNetworksInternal() {
        updateSubscriptions();
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                ensureActiveMobilePolicyAL();
                normalizePoliciesNL();
                updateNetworkEnabledNL();
                updateNetworkRulesNL();
                updateNotificationsNL();
            }
        }
    }

    @VisibleForTesting
    public void updateNetworks() throws InterruptedException {
        updateNetworksInternal();
        CountDownLatch latch = new CountDownLatch(1);
        this.mHandler.post(new -$$Lambda$NetworkPolicyManagerService$lv2qqWetKVoJzbe7z3LT5idTu54(latch));
        latch.await(5, TimeUnit.SECONDS);
    }

    private boolean maybeUpdateMobilePolicyCycleAL(int subId, String subscriberId) {
        if (LOGV) {
            Slog.v(TAG, "maybeUpdateMobilePolicyCycleAL()");
        }
        boolean policyUpdated = false;
        NetworkIdentity probeIdent = new NetworkIdentity(0, 0, subscriberId, null, false, true, true);
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            if (((NetworkTemplate) this.mNetworkPolicy.keyAt(i)).matches(probeIdent)) {
                policyUpdated |= updateDefaultMobilePolicyAL(subId, (NetworkPolicy) this.mNetworkPolicy.valueAt(i));
            }
        }
        return policyUpdated;
    }

    @VisibleForTesting
    public int getCycleDayFromCarrierConfig(PersistableBundle config, int fallbackCycleDay) {
        if (config == null) {
            return fallbackCycleDay;
        }
        int cycleDay = config.getInt("monthly_data_cycle_day_int");
        if (cycleDay == -1) {
            return fallbackCycleDay;
        }
        Calendar cal = Calendar.getInstance();
        if (cycleDay >= cal.getMinimum(5) && cycleDay <= cal.getMaximum(5)) {
            return cycleDay;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid date in CarrierConfigManager.KEY_MONTHLY_DATA_CYCLE_DAY_INT: ");
        stringBuilder.append(cycleDay);
        Slog.e(str, stringBuilder.toString());
        return fallbackCycleDay;
    }

    @VisibleForTesting
    public long getWarningBytesFromCarrierConfig(PersistableBundle config, long fallbackWarningBytes) {
        if (config == null) {
            return fallbackWarningBytes;
        }
        long warningBytes = config.getLong("data_warning_threshold_bytes_long");
        if (warningBytes == -2) {
            return -1;
        }
        if (warningBytes == -1) {
            return getPlatformDefaultWarningBytes();
        }
        if (warningBytes >= 0) {
            return warningBytes;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid value in CarrierConfigManager.KEY_DATA_WARNING_THRESHOLD_BYTES_LONG; expected a non-negative value but got: ");
        stringBuilder.append(warningBytes);
        Slog.e(str, stringBuilder.toString());
        return fallbackWarningBytes;
    }

    @VisibleForTesting
    public long getLimitBytesFromCarrierConfig(PersistableBundle config, long fallbackLimitBytes) {
        if (config == null) {
            return fallbackLimitBytes;
        }
        long limitBytes = config.getLong("data_limit_threshold_bytes_long");
        if (limitBytes == -2) {
            return -1;
        }
        if (limitBytes == -1) {
            return getPlatformDefaultLimitBytes();
        }
        if (limitBytes >= 0) {
            return limitBytes;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid value in CarrierConfigManager.KEY_DATA_LIMIT_THRESHOLD_BYTES_LONG; expected a non-negative value but got: ");
        stringBuilder.append(limitBytes);
        Slog.e(str, stringBuilder.toString());
        return fallbackLimitBytes;
    }

    void handleNetworkPoliciesUpdateAL(boolean shouldNormalizePolicies) {
        if (shouldNormalizePolicies) {
            normalizePoliciesNL();
        }
        updateNetworkEnabledNL();
        updateNetworkRulesNL();
        updateNotificationsNL();
        writePolicyAL();
    }

    void updateNetworkEnabledNL() {
        if (LOGV) {
            Slog.v(TAG, "updateNetworkEnabledNL()");
        }
        Trace.traceBegin(2097152, "updateNetworkEnabledNL");
        long startTime = this.mStatLogger.getTime();
        int i = this.mNetworkPolicy.size() - 1;
        while (true) {
            int i2 = i;
            boolean z = false;
            if (i2 >= 0) {
                NetworkPolicy policy = (NetworkPolicy) this.mNetworkPolicy.valueAt(i2);
                if (policy.limitBytes == -1 || !policy.hasCycle()) {
                    setNetworkTemplateEnabled(policy.template, true);
                } else {
                    Pair<ZonedDateTime, ZonedDateTime> cycle = (Pair) NetworkPolicyManager.cycleIterator(policy).next();
                    long start = ((ZonedDateTime) cycle.first).toInstant().toEpochMilli();
                    boolean overLimitWithoutSnooze = policy.isOverLimit(getTotalBytes(policy.template, start, ((ZonedDateTime) cycle.second).toInstant().toEpochMilli())) && policy.lastLimitSnooze < start;
                    if (!overLimitWithoutSnooze) {
                        z = true;
                    }
                    setNetworkTemplateEnabled(policy.template, z);
                }
                i = i2 - 1;
            } else {
                this.mStatLogger.logDurationStat(0, startTime);
                Trace.traceEnd(2097152);
                return;
            }
        }
    }

    private void setNetworkTemplateEnabled(NetworkTemplate template, boolean enabled) {
        this.mHandler.obtainMessage(18, enabled, 0, template).sendToTarget();
    }

    /* JADX WARNING: Missing block: B:17:0x0047, code:
            r3 = (android.telephony.TelephonyManager) r1.mContext.getSystemService(android.telephony.TelephonyManager.class);
     */
    /* JADX WARNING: Missing block: B:19:0x0056, code:
            if (r0 >= r2.size()) goto L_0x0071;
     */
    /* JADX WARNING: Missing block: B:20:0x0058, code:
            r3.setPolicyDataEnabled(r17, r2.get(r0));
            r0 = r0 + 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setNetworkTemplateEnabledInner(NetworkTemplate template, boolean enabled) {
        Throwable th;
        boolean z;
        NetworkTemplate networkTemplate;
        if (template.getMatchRule() == 1) {
            IntArray matchingSubIds = new IntArray();
            synchronized (this.mNetworkPoliciesSecondLock) {
                int i = 0;
                int i2 = 0;
                while (i2 < this.mSubIdToSubscriberId.size()) {
                    try {
                        int subId = this.mSubIdToSubscriberId.keyAt(i2);
                        try {
                            if (template.matches(new NetworkIdentity(0, 0, (String) this.mSubIdToSubscriberId.valueAt(i2), null, false, true, true))) {
                                matchingSubIds.add(subId);
                            }
                            i2++;
                        } catch (Throwable th2) {
                            th = th2;
                            z = enabled;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        networkTemplate = template;
                        z = enabled;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
                networkTemplate = template;
            }
        } else {
            networkTemplate = template;
        }
        z = enabled;
    }

    private static void collectIfaces(ArraySet<String> ifaces, NetworkState state) {
        String baseIface = state.linkProperties.getInterfaceName();
        if (baseIface != null) {
            ifaces.add(baseIface);
        }
        for (LinkProperties stackedLink : state.linkProperties.getStackedLinks()) {
            String stackedIface = stackedLink.getInterfaceName();
            if (stackedIface != null) {
                ifaces.add(stackedIface);
            }
        }
    }

    void updateSubscriptions() {
        if (LOGV) {
            Slog.v(TAG, "updateSubscriptions()");
        }
        Trace.traceBegin(2097152, "updateSubscriptions");
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        int[] subIds = ArrayUtils.defeatNullable(((SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class)).getActiveSubscriptionIdList());
        String[] mergedSubscriberIds = ArrayUtils.defeatNullable(tm.getMergedSubscriberIds());
        SparseArray<String> subIdToSubscriberId = new SparseArray(subIds.length);
        int i = 0;
        for (int subId : subIds) {
            String subscriberId = tm.getSubscriberId(subId);
            if (TextUtils.isEmpty(subscriberId)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Missing subscriberId for subId ");
                stringBuilder.append(subId);
                Slog.wtf(str, stringBuilder.toString());
            } else {
                subIdToSubscriberId.put(subId, subscriberId);
            }
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            this.mSubIdToSubscriberId.clear();
            while (i < subIdToSubscriberId.size()) {
                this.mSubIdToSubscriberId.put(subIdToSubscriberId.keyAt(i), (String) subIdToSubscriberId.valueAt(i));
                i++;
            }
            this.mMergedSubscriberIds = mergedSubscriberIds;
        }
        Trace.traceEnd(2097152);
    }

    void updateNetworkRulesNL() {
        if (LOGV) {
            Slog.v(TAG, "updateNetworkRulesNL()");
        }
        Trace.traceBegin(2097152, "updateNetworkRulesNL");
        try {
            boolean z;
            NetworkState state;
            int i;
            boolean hasLimit;
            long totalBytes;
            long quotaBytes;
            ContentResolver cr;
            NetworkState[] states = defeatNullable(this.mConnManager.getAllNetworkState());
            this.mNetIdToSubId.clear();
            ArrayMap<NetworkState, NetworkIdentity> identified = new ArrayMap();
            int length = states.length;
            int i2 = 0;
            while (true) {
                z = true;
                if (i2 >= length) {
                    break;
                }
                state = states[i2];
                if (state.network != null) {
                    this.mNetIdToSubId.put(state.network.netId, parseSubId(state));
                }
                if (state.networkInfo != null && state.networkInfo.isConnected()) {
                    identified.put(state, NetworkIdentity.buildNetworkIdentity(this.mContext, state, true));
                }
                i2++;
            }
            ArraySet<String> newMeteredIfaces = new ArraySet();
            ArraySet<String> matchingIfaces = new ArraySet();
            int i3 = this.mNetworkPolicy.size() - 1;
            long lowestRule = JobStatus.NO_LATEST_RUNTIME;
            while (true) {
                i = i3;
                if (i < 0) {
                    break;
                }
                NetworkPolicy policy;
                ArrayMap<NetworkState, NetworkIdentity> identified2;
                NetworkPolicy policy2 = (NetworkPolicy) this.mNetworkPolicy.valueAt(i);
                matchingIfaces.clear();
                for (length = identified.size() - z; length >= 0; length--) {
                    if (policy2.template.matches((NetworkIdentity) identified.valueAt(length))) {
                        collectIfaces(matchingIfaces, (NetworkState) identified.keyAt(length));
                    }
                }
                if (LOGD) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Applying ");
                    stringBuilder.append(policy2);
                    stringBuilder.append(" to ifaces ");
                    stringBuilder.append(matchingIfaces);
                    Slog.d(str, stringBuilder.toString());
                }
                boolean hasWarning = policy2.warningBytes != -1 ? z : false;
                hasLimit = policy2.limitBytes != -1 ? z : false;
                if (hasLimit || policy2.metered) {
                    ArraySet<String> newMeteredIfaces2;
                    if (hasLimit && policy2.hasCycle()) {
                        Pair<ZonedDateTime, ZonedDateTime> cycle = (Pair) NetworkPolicyManager.cycleIterator(policy2).next();
                        long start = ((ZonedDateTime) cycle.first).toInstant().toEpochMilli();
                        newMeteredIfaces2 = newMeteredIfaces;
                        policy = policy2;
                        identified2 = identified;
                        identified = lowestRule;
                        totalBytes = getTotalBytes(policy2.template, start, ((ZonedDateTime) cycle.second).toInstant().toEpochMilli());
                        if (policy.lastLimitSnooze >= start) {
                            quotaBytes = JobStatus.NO_LATEST_RUNTIME;
                        } else {
                            quotaBytes = Math.max(1, policy.limitBytes - totalBytes);
                        }
                    } else {
                        policy = policy2;
                        identified2 = identified;
                        newMeteredIfaces2 = newMeteredIfaces;
                        identified = lowestRule;
                        quotaBytes = JobStatus.NO_LATEST_RUNTIME;
                    }
                    totalBytes = quotaBytes;
                    if (matchingIfaces.size() > 1) {
                        Slog.w(TAG, "shared quota unsupported; generating rule for each iface");
                    }
                    for (i3 = matchingIfaces.size() - 1; i3 >= 0; i3--) {
                        String iface = (String) matchingIfaces.valueAt(i3);
                        setInterfaceQuotaAsync(iface, totalBytes);
                        newMeteredIfaces2.add(iface);
                    }
                    newMeteredIfaces = newMeteredIfaces2;
                } else {
                    policy = policy2;
                    identified2 = identified;
                    identified = lowestRule;
                }
                if (!hasWarning || policy.warningBytes >= identified) {
                    lowestRule = identified;
                } else {
                    lowestRule = policy.warningBytes;
                }
                if (hasLimit && policy.limitBytes < lowestRule) {
                    lowestRule = policy.limitBytes;
                }
                i3 = i - 1;
                identified = identified2;
                z = true;
            }
            long lowestRule2 = lowestRule;
            for (NetworkState state2 : states) {
                if (!(state2.networkInfo == null || !state2.networkInfo.isConnected() || state2.networkCapabilities.hasCapability(11))) {
                    matchingIfaces.clear();
                    collectIfaces(matchingIfaces, state2);
                    for (int j = matchingIfaces.size() - 1; j >= 0; j--) {
                        String iface2 = (String) matchingIfaces.valueAt(j);
                        if (!newMeteredIfaces.contains(iface2)) {
                            setInterfaceQuotaAsync(iface2, JobStatus.NO_LATEST_RUNTIME);
                            newMeteredIfaces.add(iface2);
                        }
                    }
                }
            }
            for (length = this.mMeteredIfaces.size() - 1; length >= 0; length--) {
                String iface3 = (String) this.mMeteredIfaces.valueAt(length);
                if (!newMeteredIfaces.contains(iface3)) {
                    removeInterfaceQuotaAsync(iface3);
                }
            }
            this.mMeteredIfaces = newMeteredIfaces;
            ContentResolver cr2 = this.mContext.getContentResolver();
            boolean z2 = true;
            if (Global.getInt(cr2, "netpolicy_quota_enabled", 1) == 0) {
                z2 = false;
            }
            hasLimit = z2;
            long quotaUnlimited = Global.getLong(cr2, "netpolicy_quota_unlimited", QUOTA_UNLIMITED_DEFAULT);
            float quotaLimited = Global.getFloat(cr2, "netpolicy_quota_limited", QUOTA_LIMITED_DEFAULT);
            this.mSubscriptionOpportunisticQuota.clear();
            i = states.length;
            int i4 = 0;
            while (i4 < i) {
                NetworkState[] states2;
                int i5;
                NetworkState state3 = states[i4];
                if (hasLimit && state3.network != null) {
                    i3 = getSubIdLocked(state3.network);
                    SubscriptionPlan plan = getPrimarySubscriptionPlanLocked(i3);
                    if (plan != null) {
                        long limitBytes = plan.getDataLimitBytes();
                        if (!state3.networkCapabilities.hasCapability(18)) {
                            totalBytes = 0;
                        } else if (limitBytes == -1) {
                            totalBytes = -1;
                        } else {
                            long j2;
                            SubscriptionPlan subscriptionPlan;
                            if (limitBytes == JobStatus.NO_LATEST_RUNTIME) {
                                totalBytes = quotaUnlimited;
                                states2 = states;
                                states = i3;
                                NetworkState networkState = state3;
                                cr = cr2;
                                j2 = JobStatus.NO_LATEST_RUNTIME;
                                subscriptionPlan = plan;
                                i5 = i4;
                            } else {
                                Range<ZonedDateTime> cycle2 = (Range) plan.cycleIterator().next();
                                long start2 = ((ZonedDateTime) cycle2.getLower()).toInstant().toEpochMilli();
                                long end = ((ZonedDateTime) cycle2.getUpper()).toInstant().toEpochMilli();
                                Instant now = this.mClock.instant();
                                states2 = states;
                                Instant now2 = now;
                                long startOfDay = ZonedDateTime.ofInstant(now, ((ZonedDateTime) cycle2.getLower()).getZone()).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
                                cr = cr2;
                                Instant now3 = now2;
                                NetworkTemplate buildTemplateMobileAll = NetworkTemplate.buildTemplateMobileAll(state3.subscriberId);
                                states = i3;
                                j2 = JobStatus.NO_LATEST_RUNTIME;
                                subscriptionPlan = plan;
                                i5 = i4;
                                totalBytes = getTotalBytes(buildTemplateMobileAll, start2, startOfDay);
                                quotaBytes = limitBytes - totalBytes;
                                long j3 = quotaBytes;
                                totalBytes = Math.max(0, (long) (((float) (quotaBytes / ((((end - now3.toEpochMilli()) - 1) / TimeUnit.DAYS.toMillis(1)) + 1))) * quotaLimited));
                            }
                            this.mSubscriptionOpportunisticQuota.put(states, totalBytes);
                            i4 = i5 + 1;
                            states = states2;
                            cr2 = cr;
                        }
                        states2 = states;
                        states = i3;
                        i5 = i4;
                        cr = cr2;
                        this.mSubscriptionOpportunisticQuota.put(states, totalBytes);
                        i4 = i5 + 1;
                        states = states2;
                        cr2 = cr;
                    }
                }
                states2 = states;
                i5 = i4;
                cr = cr2;
                i4 = i5 + 1;
                states = states2;
                cr2 = cr;
            }
            cr = cr2;
            this.mHandler.obtainMessage(2, (String[]) this.mMeteredIfaces.toArray(new String[this.mMeteredIfaces.size()])).sendToTarget();
            this.mHandler.obtainMessage(7, Long.valueOf(lowestRule2)).sendToTarget();
            Trace.traceEnd(2097152);
        } catch (RemoteException e) {
        }
    }

    private void ensureActiveMobilePolicyAL() {
        if (LOGV) {
            Slog.v(TAG, "ensureActiveMobilePolicyAL()");
        }
        if (!this.mSuppressDefaultPolicy) {
            for (int i = 0; i < this.mSubIdToSubscriberId.size(); i++) {
                ensureActiveMobilePolicyAL(this.mSubIdToSubscriberId.keyAt(i), (String) this.mSubIdToSubscriberId.valueAt(i));
            }
        }
    }

    private boolean ensureActiveMobilePolicyAL(int subId, String subscriberId) {
        NetworkIdentity probeIdent = new NetworkIdentity(0, 0, subscriberId, null, false, true, true);
        for (int i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
            NetworkTemplate template = (NetworkTemplate) this.mNetworkPolicy.keyAt(i);
            if (template.matches(probeIdent)) {
                if (LOGD) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Found template ");
                    stringBuilder.append(template);
                    stringBuilder.append(" which matches subscriber ");
                    stringBuilder.append(NetworkIdentity.scrubSubscriberId(subscriberId));
                    Slog.d(str, stringBuilder.toString());
                }
                return false;
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("No policy for subscriber ");
        stringBuilder2.append(NetworkIdentity.scrubSubscriberId(subscriberId));
        stringBuilder2.append("; generating default policy");
        Slog.i(str2, stringBuilder2.toString());
        addNetworkPolicyAL(buildDefaultMobilePolicy(subId, subscriberId));
        return true;
    }

    private long getPlatformDefaultWarningBytes() {
        int dataWarningConfig = this.mContext.getResources().getInteger(17694829);
        if (((long) dataWarningConfig) == -1) {
            return -1;
        }
        return ((long) dataWarningConfig) * 1048576;
    }

    private long getPlatformDefaultLimitBytes() {
        return -1;
    }

    @VisibleForTesting
    public NetworkPolicy buildDefaultMobilePolicy(int subId, String subscriberId) {
        Throwable th;
        NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(subscriberId);
        NetworkPolicy policy = new NetworkPolicy(template, NetworkPolicy.buildRule(ZonedDateTime.now().getDayOfMonth(), ZoneId.systemDefault()), getPlatformDefaultWarningBytes(), getPlatformDefaultLimitBytes(), -1, -1, true, true);
        synchronized (this.mUidRulesFirstLock) {
            try {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    updateDefaultMobilePolicyAL(subId, policy);
                }
                try {
                    return policy;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                int i = subId;
                throw th;
            }
        }
    }

    private boolean updateDefaultMobilePolicyAL(int subId, NetworkPolicy policy) {
        int i = subId;
        NetworkPolicy networkPolicy = policy;
        if (networkPolicy.inferred) {
            NetworkTemplate networkTemplate = networkPolicy.template;
            RecurrenceRule recurrenceRule = networkPolicy.cycleRule;
            long j = networkPolicy.warningBytes;
            long j2 = networkPolicy.limitBytes;
            long j3 = networkPolicy.lastWarningSnooze;
            long j4 = networkPolicy.lastLimitSnooze;
            NetworkPolicy networkPolicy2 = new NetworkPolicy(networkTemplate, recurrenceRule, j, j2, j3, j4, networkPolicy.metered, networkPolicy.inferred);
            SubscriptionPlan[] plans = (SubscriptionPlan[]) this.mSubscriptionPlans.get(i);
            if (!ArrayUtils.isEmpty(plans)) {
                SubscriptionPlan plan = plans[0];
                networkPolicy.cycleRule = plan.getCycleRule();
                long planLimitBytes = plan.getDataLimitBytes();
                if (planLimitBytes != -1) {
                    if (planLimitBytes != JobStatus.NO_LATEST_RUNTIME) {
                        networkPolicy.warningBytes = (9 * planLimitBytes) / 10;
                        switch (plan.getDataLimitBehavior()) {
                            case 0:
                            case 1:
                                networkPolicy.limitBytes = planLimitBytes;
                                break;
                            default:
                                networkPolicy.limitBytes = -1;
                                break;
                        }
                    }
                    networkPolicy.warningBytes = -1;
                    networkPolicy.limitBytes = -1;
                } else {
                    networkPolicy.warningBytes = getPlatformDefaultWarningBytes();
                    networkPolicy.limitBytes = getPlatformDefaultLimitBytes();
                }
            } else {
                int currentCycleDay;
                PersistableBundle config = this.mCarrierConfigManager.getConfigForSubId(i);
                if (networkPolicy.cycleRule.isMonthly()) {
                    currentCycleDay = networkPolicy.cycleRule.start.getDayOfMonth();
                } else {
                    currentCycleDay = -1;
                }
                networkPolicy.cycleRule = NetworkPolicy.buildRule(getCycleDayFromCarrierConfig(config, currentCycleDay), ZoneId.systemDefault());
                networkPolicy.warningBytes = getWarningBytesFromCarrierConfig(config, networkPolicy.warningBytes);
                networkPolicy.limitBytes = getLimitBytesFromCarrierConfig(config, networkPolicy.limitBytes);
            }
            if (networkPolicy.equals(networkPolicy2)) {
                return false;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Updated ");
            stringBuilder.append(networkPolicy2);
            stringBuilder.append(" to ");
            stringBuilder.append(networkPolicy);
            Slog.d(str, stringBuilder.toString());
            return true;
        }
        if (LOGD) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Ignoring user-defined policy ");
            stringBuilder2.append(networkPolicy);
            Slog.d(str2, stringBuilder2.toString());
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:50:0x0130 A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x0126 A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x013e A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0135 A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x014b A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x011a A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0112 A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x0126 A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x0130 A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0135 A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x013e A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x014b A:{Catch:{ FileNotFoundException -> 0x038c, Exception -> 0x0382, all -> 0x0380 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void readPolicyAL() {
        if (LOGV) {
            Slog.v(TAG, "readPolicyAL()");
        }
        this.mNetworkPolicy.clear();
        this.mSubscriptionPlans.clear();
        this.mSubscriptionPlansOwner.clear();
        this.mUidPolicy.clear();
        String str = null;
        FileInputStream fis = null;
        try {
            int next;
            int i;
            int cycleDay;
            String cycleTimezone;
            int subId;
            int uid;
            StringBuilder stringBuilder;
            fis = this.mPolicyFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, StandardCharsets.UTF_8.name());
            SparseBooleanArray whitelistedRestrictBackground = new SparseBooleanArray();
            int version = 1;
            while (true) {
                next = in.next();
                int type = next;
                boolean z = true;
                if (next == 1) {
                    break;
                }
                String tag = in.getName();
                if (type != 2) {
                    i = version;
                    if (type == 3) {
                        str = TAG_WHITELIST.equals(tag);
                        if (str != null) {
                            str = null;
                        }
                    }
                    version = i;
                } else if (TAG_POLICY_LIST.equals(tag)) {
                    boolean oldValue = this.mRestrictBackground;
                    version = XmlUtils.readIntAttribute(in, ATTR_VERSION);
                    if (version < 3 || !XmlUtils.readBooleanAttribute(in, ATTR_RESTRICT_BACKGROUND)) {
                        z = false;
                    }
                    this.mLoadedRestrictBackground = z;
                } else {
                    String networkId;
                    long lastLimitSnooze;
                    if (TAG_NETWORK_POLICY.equals(tag)) {
                        RecurrenceRule cycleRule;
                        long lastLimitSnooze2;
                        boolean metered;
                        int networkTemplate = XmlUtils.readIntAttribute(in, ATTR_NETWORK_TEMPLATE);
                        String subscriberId = in.getAttributeValue(str, ATTR_SUBSCRIBER_ID);
                        if (version >= 9) {
                            networkId = in.getAttributeValue(str, ATTR_NETWORK_ID);
                        } else {
                            networkId = str;
                        }
                        if (version >= 11) {
                            String start = XmlUtils.readStringAttribute(in, ATTR_CYCLE_START);
                            String end = XmlUtils.readStringAttribute(in, ATTR_CYCLE_END);
                            cycleRule = new RecurrenceRule(RecurrenceRule.convertZonedDateTime(start), RecurrenceRule.convertZonedDateTime(end), RecurrenceRule.convertPeriod(XmlUtils.readStringAttribute(in, ATTR_CYCLE_PERIOD)));
                        } else {
                            cycleDay = XmlUtils.readIntAttribute(in, ATTR_CYCLE_DAY);
                            if (version >= 6) {
                                cycleTimezone = in.getAttributeValue(null, ATTR_CYCLE_TIMEZONE);
                            } else {
                                cycleTimezone = "UTC";
                            }
                            cycleRule = NetworkPolicy.buildRule(cycleDay, ZoneId.of(cycleTimezone));
                        }
                        long warningBytes = XmlUtils.readLongAttribute(in, ATTR_WARNING_BYTES);
                        long limitBytes = XmlUtils.readLongAttribute(in, ATTR_LIMIT_BYTES);
                        if (version >= 5) {
                            lastLimitSnooze = XmlUtils.readLongAttribute(in, ATTR_LAST_LIMIT_SNOOZE);
                        } else if (version >= 2) {
                            lastLimitSnooze = XmlUtils.readLongAttribute(in, ATTR_LAST_SNOOZE);
                        } else {
                            lastLimitSnooze2 = -1;
                            if (version < 4) {
                                z = XmlUtils.readBooleanAttribute(in, ATTR_METERED);
                            } else if (networkTemplate != 1) {
                                long lastWarningSnooze;
                                boolean inferred;
                                metered = false;
                                if (version >= 5) {
                                    lastWarningSnooze = XmlUtils.readLongAttribute(in, ATTR_LAST_WARNING_SNOOZE);
                                } else {
                                    lastWarningSnooze = -1;
                                }
                                if (version >= 7) {
                                    inferred = XmlUtils.readBooleanAttribute(in, ATTR_INFERRED);
                                } else {
                                    inferred = false;
                                }
                                str = new NetworkTemplate(networkTemplate, subscriberId, networkId);
                                if (str.isPersistable()) {
                                    this.mNetworkPolicy.put(str, new NetworkPolicy(str, cycleRule, warningBytes, limitBytes, lastWarningSnooze, lastLimitSnooze2, metered, inferred));
                                }
                                i = version;
                            } else {
                                z = true;
                            }
                            metered = z;
                            if (version >= 5) {
                            }
                            if (version >= 7) {
                            }
                            str = new NetworkTemplate(networkTemplate, subscriberId, networkId);
                            if (str.isPersistable()) {
                            }
                            i = version;
                        }
                        lastLimitSnooze2 = lastLimitSnooze;
                        if (version < 4) {
                        }
                        metered = z;
                        if (version >= 5) {
                        }
                        if (version >= 7) {
                        }
                        str = new NetworkTemplate(networkTemplate, subscriberId, networkId);
                        if (str.isPersistable()) {
                        }
                        i = version;
                    } else {
                        String str2 = str;
                        if (TAG_SUBSCRIPTION_PLAN.equals(tag)) {
                            str = XmlUtils.readStringAttribute(in, ATTR_CYCLE_START);
                            networkId = XmlUtils.readStringAttribute(in, ATTR_CYCLE_END);
                            cycleTimezone = XmlUtils.readStringAttribute(in, ATTR_CYCLE_PERIOD);
                            SubscriptionPlan.Builder builder = new SubscriptionPlan.Builder(RecurrenceRule.convertZonedDateTime(str), RecurrenceRule.convertZonedDateTime(networkId), RecurrenceRule.convertPeriod(cycleTimezone));
                            builder.setTitle(XmlUtils.readStringAttribute(in, ATTR_TITLE));
                            builder.setSummary(XmlUtils.readStringAttribute(in, ATTR_SUMMARY));
                            lastLimitSnooze = XmlUtils.readLongAttribute(in, ATTR_LIMIT_BYTES, -1);
                            int limitBehavior = XmlUtils.readIntAttribute(in, ATTR_LIMIT_BEHAVIOR, -1);
                            if (!(lastLimitSnooze == -1 || limitBehavior == -1)) {
                                builder.setDataLimit(lastLimitSnooze, limitBehavior);
                            }
                            i = version;
                            long usageBytes = XmlUtils.readLongAttribute(in, ATTR_USAGE_BYTES, -1);
                            long usageTime = XmlUtils.readLongAttribute(in, ATTR_USAGE_TIME, -1);
                            long usageBytes2 = usageBytes;
                            long usageTime2;
                            if (usageBytes2 != -1) {
                                usageTime2 = usageTime;
                                if (usageTime2 != -1) {
                                    builder.setDataUsage(usageBytes2, usageTime2);
                                }
                            } else {
                                usageTime2 = usageTime;
                            }
                            subId = XmlUtils.readIntAttribute(in, ATTR_SUB_ID);
                            this.mSubscriptionPlans.put(subId, (SubscriptionPlan[]) ArrayUtils.appendElement(SubscriptionPlan.class, (SubscriptionPlan[]) this.mSubscriptionPlans.get(subId), builder.build()));
                            str = XmlUtils.readStringAttribute(in, ATTR_OWNER_PACKAGE);
                            this.mSubscriptionPlansOwner.put(subId, str);
                        } else {
                            i = version;
                            String str3;
                            if (TAG_UID_POLICY.equals(tag)) {
                                str = XmlUtils.readIntAttribute(in, "uid");
                                subId = XmlUtils.readIntAttribute(in, ATTR_POLICY);
                                if (UserHandle.isApp(str)) {
                                    setUidPolicyUncheckedUL(str, subId, false);
                                } else {
                                    str3 = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("unable to apply policy to UID ");
                                    stringBuilder2.append(str);
                                    stringBuilder2.append("; ignoring");
                                    Slog.w(str3, stringBuilder2.toString());
                                }
                            } else if (TAG_APP_POLICY.equals(tag)) {
                                str = XmlUtils.readIntAttribute(in, ATTR_APP_ID);
                                subId = XmlUtils.readIntAttribute(in, ATTR_POLICY);
                                uid = UserHandle.getUid(0, str);
                                if (UserHandle.isApp(uid)) {
                                    setUidPolicyUncheckedUL(uid, subId, false);
                                } else {
                                    str3 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("unable to apply policy to UID ");
                                    stringBuilder.append(uid);
                                    stringBuilder.append("; ignoring");
                                    Slog.w(str3, stringBuilder.toString());
                                }
                            } else if (TAG_WHITELIST.equals(tag)) {
                                str = true;
                            } else if (!TAG_RESTRICT_BACKGROUND.equals(tag) || null == null) {
                                str = TAG_REVOKED_RESTRICT_BACKGROUND.equals(tag);
                                if (!(str == null || null == null)) {
                                    str = XmlUtils.readIntAttribute(in, "uid");
                                    this.mRestrictBackgroundWhitelistRevokedUids.put(str, true);
                                }
                            } else {
                                str = XmlUtils.readIntAttribute(in, "uid");
                                whitelistedRestrictBackground.append(str, true);
                            }
                        }
                    }
                    version = i;
                }
                boolean insideWhitelist = str;
                version = i;
            }
            i = version;
            cycleDay = whitelistedRestrictBackground.size();
            for (subId = 0; subId < cycleDay; subId++) {
                version = whitelistedRestrictBackground.keyAt(subId);
                next = this.mUidPolicy.get(version, 0);
                StringBuilder stringBuilder3;
                if ((next & 1) != 0) {
                    cycleTimezone = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ignoring restrict-background-whitelist for ");
                    stringBuilder.append(version);
                    stringBuilder.append(" because its policy is ");
                    stringBuilder.append(NetworkPolicyManager.uidPoliciesToString(next));
                    Slog.w(cycleTimezone, stringBuilder.toString());
                } else if (UserHandle.isApp(version)) {
                    uid = next | 4;
                    if (LOGV) {
                        String str4 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("new policy for ");
                        stringBuilder3.append(version);
                        stringBuilder3.append(": ");
                        stringBuilder3.append(NetworkPolicyManager.uidPoliciesToString(uid));
                        Log.v(str4, stringBuilder3.toString());
                    }
                    setUidPolicyUncheckedUL(version, uid, false);
                } else {
                    cycleTimezone = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("unable to update policy on UID ");
                    stringBuilder3.append(version);
                    Slog.w(cycleTimezone, stringBuilder3.toString());
                }
            }
        } catch (FileNotFoundException e) {
            upgradeDefaultBackgroundDataUL();
        } catch (Exception e2) {
            Log.wtf(TAG, "problem reading network policy", e2);
        } catch (Throwable th) {
            IoUtils.closeQuietly(fis);
        }
        IoUtils.closeQuietly(fis);
    }

    private void upgradeDefaultBackgroundDataUL() {
        boolean z = true;
        if (Global.getInt(this.mContext.getContentResolver(), "default_restrict_background_data", 0) != 1) {
            z = false;
        }
        this.mLoadedRestrictBackground = z;
    }

    private void upgradeWifiMeteredOverrideAL() {
        boolean modified = false;
        WifiManager wm = (WifiManager) this.mContext.getSystemService(WifiManager.class);
        List<WifiConfiguration> configs = wm.getConfiguredNetworks();
        int i = 0;
        while (i < this.mNetworkPolicy.size()) {
            NetworkPolicy policy = (NetworkPolicy) this.mNetworkPolicy.valueAt(i);
            if (policy.template.getMatchRule() != 4 || policy.inferred) {
                i++;
            } else {
                this.mNetworkPolicy.removeAt(i);
                modified = true;
                String networkId = NetworkPolicyManager.resolveNetworkId(policy.template.getNetworkId());
                for (WifiConfiguration config : configs) {
                    if (Objects.equals(NetworkPolicyManager.resolveNetworkId(config), networkId)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Found network ");
                        stringBuilder.append(networkId);
                        stringBuilder.append("; upgrading metered hint");
                        Slog.d(str, stringBuilder.toString());
                        config.meteredOverride = policy.metered ? 1 : 2;
                        wm.updateNetwork(config);
                    }
                }
            }
        }
        if (modified) {
            writePolicyAL();
        }
    }

    void writePolicyAL() {
        if (LOGV) {
            Slog.v(TAG, "writePolicyAL()");
        }
        FileOutputStream fos = null;
        try {
            int i;
            int subId;
            fos = this.mPolicyFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, TAG_POLICY_LIST);
            XmlUtils.writeIntAttribute(out, ATTR_VERSION, 11);
            XmlUtils.writeBooleanAttribute(out, ATTR_RESTRICT_BACKGROUND, this.mRestrictBackground);
            int i2 = 0;
            for (i = 0; i < this.mNetworkPolicy.size(); i++) {
                NetworkPolicy policy = (NetworkPolicy) this.mNetworkPolicy.valueAt(i);
                NetworkTemplate template = policy.template;
                if (template.isPersistable()) {
                    out.startTag(null, TAG_NETWORK_POLICY);
                    XmlUtils.writeIntAttribute(out, ATTR_NETWORK_TEMPLATE, template.getMatchRule());
                    String subscriberId = template.getSubscriberId();
                    if (subscriberId != null) {
                        out.attribute(null, ATTR_SUBSCRIBER_ID, subscriberId);
                    }
                    String networkId = template.getNetworkId();
                    if (networkId != null) {
                        out.attribute(null, ATTR_NETWORK_ID, networkId);
                    }
                    XmlUtils.writeStringAttribute(out, ATTR_CYCLE_START, RecurrenceRule.convertZonedDateTime(policy.cycleRule.start));
                    XmlUtils.writeStringAttribute(out, ATTR_CYCLE_END, RecurrenceRule.convertZonedDateTime(policy.cycleRule.end));
                    XmlUtils.writeStringAttribute(out, ATTR_CYCLE_PERIOD, RecurrenceRule.convertPeriod(policy.cycleRule.period));
                    XmlUtils.writeLongAttribute(out, ATTR_WARNING_BYTES, policy.warningBytes);
                    XmlUtils.writeLongAttribute(out, ATTR_LIMIT_BYTES, policy.limitBytes);
                    XmlUtils.writeLongAttribute(out, ATTR_LAST_WARNING_SNOOZE, policy.lastWarningSnooze);
                    XmlUtils.writeLongAttribute(out, ATTR_LAST_LIMIT_SNOOZE, policy.lastLimitSnooze);
                    XmlUtils.writeBooleanAttribute(out, ATTR_METERED, policy.metered);
                    XmlUtils.writeBooleanAttribute(out, ATTR_INFERRED, policy.inferred);
                    out.endTag(null, TAG_NETWORK_POLICY);
                }
            }
            for (i = 0; i < this.mSubscriptionPlans.size(); i++) {
                subId = this.mSubscriptionPlans.keyAt(i);
                String ownerPackage = (String) this.mSubscriptionPlansOwner.get(subId);
                SubscriptionPlan[] plans = (SubscriptionPlan[]) this.mSubscriptionPlans.valueAt(i);
                if (!ArrayUtils.isEmpty(plans)) {
                    for (SubscriptionPlan plan : plans) {
                        out.startTag(null, TAG_SUBSCRIPTION_PLAN);
                        XmlUtils.writeIntAttribute(out, ATTR_SUB_ID, subId);
                        XmlUtils.writeStringAttribute(out, ATTR_OWNER_PACKAGE, ownerPackage);
                        RecurrenceRule cycleRule = plan.getCycleRule();
                        XmlUtils.writeStringAttribute(out, ATTR_CYCLE_START, RecurrenceRule.convertZonedDateTime(cycleRule.start));
                        XmlUtils.writeStringAttribute(out, ATTR_CYCLE_END, RecurrenceRule.convertZonedDateTime(cycleRule.end));
                        XmlUtils.writeStringAttribute(out, ATTR_CYCLE_PERIOD, RecurrenceRule.convertPeriod(cycleRule.period));
                        XmlUtils.writeStringAttribute(out, ATTR_TITLE, plan.getTitle());
                        XmlUtils.writeStringAttribute(out, ATTR_SUMMARY, plan.getSummary());
                        XmlUtils.writeLongAttribute(out, ATTR_LIMIT_BYTES, plan.getDataLimitBytes());
                        XmlUtils.writeIntAttribute(out, ATTR_LIMIT_BEHAVIOR, plan.getDataLimitBehavior());
                        XmlUtils.writeLongAttribute(out, ATTR_USAGE_BYTES, plan.getDataUsageBytes());
                        XmlUtils.writeLongAttribute(out, ATTR_USAGE_TIME, plan.getDataUsageTime());
                        out.endTag(null, TAG_SUBSCRIPTION_PLAN);
                    }
                }
            }
            for (i = 0; i < this.mUidPolicy.size(); i++) {
                subId = this.mUidPolicy.keyAt(i);
                int policy2 = this.mUidPolicy.valueAt(i);
                if (policy2 != 0) {
                    out.startTag(null, TAG_UID_POLICY);
                    XmlUtils.writeIntAttribute(out, "uid", subId);
                    XmlUtils.writeIntAttribute(out, ATTR_POLICY, policy2);
                    out.endTag(null, TAG_UID_POLICY);
                }
            }
            out.endTag(null, TAG_POLICY_LIST);
            out.startTag(null, TAG_WHITELIST);
            i = this.mRestrictBackgroundWhitelistRevokedUids.size();
            while (i2 < i) {
                subId = this.mRestrictBackgroundWhitelistRevokedUids.keyAt(i2);
                out.startTag(null, TAG_REVOKED_RESTRICT_BACKGROUND);
                XmlUtils.writeIntAttribute(out, "uid", subId);
                out.endTag(null, TAG_REVOKED_RESTRICT_BACKGROUND);
                i2++;
            }
            out.endTag(null, TAG_WHITELIST);
            out.endDocument();
            this.mPolicyFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mPolicyFile.failWrite(fos);
            }
        }
    }

    public void setUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (UserHandle.isApp(uid)) {
            synchronized (this.mUidRulesFirstLock) {
                long token = Binder.clearCallingIdentity();
                try {
                    int oldPolicy = this.mUidPolicy.get(uid, 0);
                    if (oldPolicy != policy) {
                        setUidPolicyUncheckedUL(uid, oldPolicy, policy, true);
                        this.mLogger.uidPolicyChanged(uid, oldPolicy, policy);
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cannot apply policy to UID ");
        stringBuilder.append(uid);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void addUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (UserHandle.isApp(uid)) {
            synchronized (this.mUidRulesFirstLock) {
                int oldPolicy = this.mUidPolicy.get(uid, 0);
                policy |= oldPolicy;
                if (oldPolicy != policy) {
                    setUidPolicyUncheckedUL(uid, oldPolicy, policy, true);
                    this.mLogger.uidPolicyChanged(uid, oldPolicy, policy);
                }
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cannot apply policy to UID ");
        stringBuilder.append(uid);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void removeUidPolicy(int uid, int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        if (UserHandle.isApp(uid)) {
            synchronized (this.mUidRulesFirstLock) {
                int oldPolicy = this.mUidPolicy.get(uid, 0);
                policy = oldPolicy & (~policy);
                if (oldPolicy != policy) {
                    setUidPolicyUncheckedUL(uid, oldPolicy, policy, true);
                    this.mLogger.uidPolicyChanged(uid, oldPolicy, policy);
                }
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cannot apply policy to UID ");
        stringBuilder.append(uid);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void setUidPolicyUncheckedUL(int uid, int oldPolicy, int policy, boolean persist) {
        boolean notifyApp = false;
        setUidPolicyUncheckedUL(uid, policy, false);
        if (isUidValidForWhitelistRules(uid)) {
            boolean wasBlacklisted = oldPolicy == 1;
            boolean isBlacklisted = policy == 1;
            boolean wasWhitelisted = oldPolicy == 4;
            boolean isWhitelisted = policy == 4;
            boolean wasBlocked = wasBlacklisted || (this.mRestrictBackground && !wasWhitelisted);
            boolean isBlocked = isBlacklisted || (this.mRestrictBackground && !isWhitelisted);
            if (wasWhitelisted && ((!isWhitelisted || isBlacklisted) && this.mDefaultRestrictBackgroundWhitelistUids.get(uid) && !this.mRestrictBackgroundWhitelistRevokedUids.get(uid))) {
                if (LOGD) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Adding uid ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" to revoked restrict background whitelist");
                    Slog.d(str, stringBuilder.toString());
                }
                this.mRestrictBackgroundWhitelistRevokedUids.append(uid, true);
            }
            if (wasBlocked != isBlocked) {
                notifyApp = true;
            }
        } else {
            notifyApp = false;
        }
        this.mHandler.obtainMessage(13, uid, policy, Boolean.valueOf(notifyApp)).sendToTarget();
        if (persist) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                writePolicyAL();
            }
        }
    }

    private void setUidPolicyUncheckedUL(int uid, int policy, boolean persist) {
        if (policy == 0) {
            this.mUidPolicy.delete(uid);
        } else {
            this.mUidPolicy.put(uid, policy);
        }
        updateRulesForDataUsageRestrictionsUL(uid);
        if (persist) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                writePolicyAL();
            }
        }
    }

    public int getUidPolicy(int uid) {
        int i;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesFirstLock) {
            i = this.mUidPolicy.get(uid, 0);
        }
        return i;
    }

    public int[] getUidsWithPolicy(int policy) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        int i = 0;
        int[] uids = new int[0];
        synchronized (this.mUidRulesFirstLock) {
            while (i < this.mUidPolicy.size()) {
                int uid = this.mUidPolicy.keyAt(i);
                int uidPolicy = this.mUidPolicy.valueAt(i);
                if ((policy == 0 && uidPolicy == 0) || (uidPolicy & policy) != 0) {
                    uids = ArrayUtils.appendInt(uids, uid);
                }
                i++;
            }
        }
        return uids;
    }

    boolean removeUserStateUL(int userId, boolean writePolicy) {
        int i;
        int i2;
        this.mLogger.removingUserState(userId);
        boolean changed = false;
        for (i = this.mRestrictBackgroundWhitelistRevokedUids.size() - 1; i >= 0; i--) {
            if (UserHandle.getUserId(this.mRestrictBackgroundWhitelistRevokedUids.keyAt(i)) == userId) {
                this.mRestrictBackgroundWhitelistRevokedUids.removeAt(i);
                changed = true;
            }
        }
        i = 0;
        int[] uids = new int[0];
        for (i2 = 0; i2 < this.mUidPolicy.size(); i2++) {
            int uid = this.mUidPolicy.keyAt(i2);
            if (UserHandle.getUserId(uid) == userId) {
                uids = ArrayUtils.appendInt(uids, uid);
            }
        }
        if (uids.length > 0) {
            i2 = uids.length;
            while (i < i2) {
                this.mUidPolicy.delete(uids[i]);
                i++;
            }
            changed = true;
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            updateRulesForGlobalChangeAL(true);
            if (writePolicy && changed) {
                writePolicyAL();
            }
        }
        return changed;
    }

    public void registerListener(INetworkPolicyListener listener) {
        sendBehavior(BehaviorId.NETWORKPOLICYMANAGER_REGISTERLISTENER);
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mListeners.register(listener);
    }

    public void unregisterListener(INetworkPolicyListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mListeners.unregister(listener);
    }

    public void setNetworkPolicies(NetworkPolicy[] policies) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    normalizePoliciesNL(policies);
                    handleNetworkPoliciesUpdateAL(false);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void addNetworkPolicyAL(NetworkPolicy policy) {
        setNetworkPolicies((NetworkPolicy[]) ArrayUtils.appendElement(NetworkPolicy.class, getNetworkPolicies(this.mContext.getOpPackageName()), policy));
    }

    public NetworkPolicy[] getNetworkPolicies(String callingPackage) {
        NetworkPolicy[] policies;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        int i = 0;
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", TAG);
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", TAG);
            if (this.mAppOps.noteOp(51, Binder.getCallingUid(), callingPackage) != 0) {
                return new NetworkPolicy[0];
            }
        }
        synchronized (this.mNetworkPoliciesSecondLock) {
            int size = this.mNetworkPolicy.size();
            policies = new NetworkPolicy[size];
            while (i < size) {
                policies[i] = (NetworkPolicy) this.mNetworkPolicy.valueAt(i);
                i++;
            }
        }
        return policies;
    }

    private void normalizePoliciesNL() {
        normalizePoliciesNL(getNetworkPolicies(this.mContext.getOpPackageName()));
    }

    private void normalizePoliciesNL(NetworkPolicy[] policies) {
        this.mNetworkPolicy.clear();
        for (NetworkPolicy policy : policies) {
            if (policy != null) {
                policy.template = NetworkTemplate.normalize(policy.template, this.mMergedSubscriberIds);
                NetworkPolicy existing = (NetworkPolicy) this.mNetworkPolicy.get(policy.template);
                if (existing == null || existing.compareTo(policy) > 0) {
                    if (existing != null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Normalization replaced ");
                        stringBuilder.append(existing);
                        stringBuilder.append(" with ");
                        stringBuilder.append(policy);
                        Slog.d(str, stringBuilder.toString());
                    }
                    this.mNetworkPolicy.put(policy.template, policy);
                }
            }
        }
    }

    public void snoozeLimit(NetworkTemplate template) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            performSnooze(template, 35);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void performSnooze(NetworkTemplate template, int type) {
        long currentTime = this.mClock.millis();
        synchronized (this.mUidRulesFirstLock) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                NetworkPolicy policy = (NetworkPolicy) this.mNetworkPolicy.get(template);
                if (policy != null) {
                    if (type != 45) {
                        switch (type) {
                            case 34:
                                policy.lastWarningSnooze = currentTime;
                                break;
                            case 35:
                                policy.lastLimitSnooze = currentTime;
                                break;
                            default:
                                throw new IllegalArgumentException("unexpected type");
                        }
                    }
                    policy.lastRapidSnooze = currentTime;
                    handleNetworkPoliciesUpdateAL(true);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to find policy for ");
                    stringBuilder.append(template);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
    }

    public void onTetheringChanged(String iface, boolean tethering) {
        synchronized (this.mUidRulesFirstLock) {
            if (this.mRestrictBackground && tethering) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Tethering on (");
                stringBuilder.append(iface);
                stringBuilder.append("); disable Data Saver");
                Log.d(str, stringBuilder.toString());
                setRestrictBackground(false);
            }
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        Trace.traceBegin(2097152, "setRestrictBackground");
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mUidRulesFirstLock) {
                    setRestrictBackgroundUL(restrictBackground);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    private void setRestrictBackgroundUL(boolean restrictBackground) {
        Trace.traceBegin(2097152, "setRestrictBackgroundUL");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mRestrictBackground=");
        stringBuilder.append(this.mRestrictBackground);
        stringBuilder.append(",restrictBackground=");
        stringBuilder.append(restrictBackground);
        Log.i(str, stringBuilder.toString());
        try {
            if (restrictBackground == this.mRestrictBackground) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("setRestrictBackgroundUL: already ");
                stringBuilder.append(restrictBackground);
                Slog.w(str, stringBuilder.toString());
                Trace.traceEnd(2097152);
                return;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setRestrictBackgroundUL(): ");
            stringBuilder.append(restrictBackground);
            Slog.d(str, stringBuilder.toString());
            boolean oldRestrictBackground = this.mRestrictBackground;
            this.mRestrictBackground = restrictBackground;
            updateRulesForRestrictBackgroundUL();
            try {
                if (!this.mNetworkManager.setDataSaverModeEnabled(this.mRestrictBackground)) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Could not change Data Saver Mode on NMS to ");
                    stringBuilder2.append(this.mRestrictBackground);
                    Slog.e(str2, stringBuilder2.toString());
                    this.mRestrictBackground = oldRestrictBackground;
                    Trace.traceEnd(2097152);
                    return;
                }
            } catch (RemoteException e) {
            }
            sendRestrictBackgroundChangedMsg();
            this.mLogger.restrictBackgroundChanged(oldRestrictBackground, this.mRestrictBackground);
            if (this.mRestrictBackgroundPowerState.globalBatterySaverEnabled) {
                this.mRestrictBackgroundChangedInBsm = true;
            }
            synchronized (this.mNetworkPoliciesSecondLock) {
                updateNotificationsNL();
                writePolicyAL();
            }
            Trace.traceEnd(2097152);
        } catch (Throwable th) {
            Trace.traceEnd(2097152);
        }
    }

    private void sendRestrictBackgroundChangedMsg() {
        this.mHandler.removeMessages(6);
        this.mHandler.obtainMessage(6, this.mRestrictBackground, 0).sendToTarget();
    }

    /* JADX WARNING: Missing block: B:18:0x0036, code:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getRestrictBackgroundByCaller() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
        int uid = Binder.getCallingUid();
        synchronized (this.mUidRulesFirstLock) {
            long token = Binder.clearCallingIdentity();
            try {
                int policy = getUidPolicy(uid);
                int i = 3;
                if (policy == 1) {
                    return 3;
                } else if (!this.mRestrictBackground) {
                    return 1;
                } else if ((this.mUidPolicy.get(uid) & 4) != 0) {
                    i = 2;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public boolean getRestrictBackground() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesFirstLock) {
            z = this.mRestrictBackground;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:15:0x002d, code:
            if (r5 == false) goto L_0x0036;
     */
    /* JADX WARNING: Missing block: B:17:?, code:
            com.android.server.EventLogTags.writeDeviceIdleOnPhase("net");
     */
    /* JADX WARNING: Missing block: B:18:0x0036, code:
            com.android.server.EventLogTags.writeDeviceIdleOffPhase("net");
     */
    /* JADX WARNING: Missing block: B:19:0x003c, code:
            android.os.Trace.traceEnd(2097152);
     */
    /* JADX WARNING: Missing block: B:20:0x0040, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setDeviceIdleMode(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        Trace.traceBegin(2097152, "setDeviceIdleMode");
        try {
            synchronized (this.mUidRulesFirstLock) {
                if (this.mDeviceIdleMode == enabled) {
                    Trace.traceEnd(2097152);
                    return;
                }
                this.mDeviceIdleMode = enabled;
                this.mLogger.deviceIdleModeEnabled(enabled);
                if (this.mSystemReady) {
                    updateRulesForRestrictPowerUL();
                }
            }
        } catch (Throwable th) {
            Trace.traceEnd(2097152);
        }
    }

    public void setWifiMeteredOverride(String networkId, int meteredOverride) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        long token = Binder.clearCallingIdentity();
        try {
            WifiManager wm = (WifiManager) this.mContext.getSystemService(WifiManager.class);
            for (WifiConfiguration config : wm.getConfiguredNetworks()) {
                if (Objects.equals(NetworkPolicyManager.resolveNetworkId(config), networkId)) {
                    config.meteredOverride = meteredOverride;
                    wm.updateNetwork(config);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Deprecated
    public NetworkQuotaInfo getNetworkQuotaInfo(NetworkState state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Shame on UID ");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(" for calling the hidden API getNetworkQuotaInfo(). Shame!");
        Log.w(str, stringBuilder.toString());
        return new NetworkQuotaInfo();
    }

    private void enforceSubscriptionPlanAccess(int subId, int callingUid, String callingPackage) {
        this.mAppOps.checkPackage(callingUid, callingPackage);
        long token = Binder.clearCallingIdentity();
        try {
            SubscriptionInfo si = ((SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class)).getActiveSubscriptionInfo(subId);
            PersistableBundle config = this.mCarrierConfigManager.getConfigForSubId(subId);
            if (si == null || !si.isEmbedded() || !si.canManageSubscription(this.mContext, callingPackage)) {
                String overridePackage;
                if (config != null) {
                    overridePackage = config.getString("config_plans_package_override_string", null);
                    if (!TextUtils.isEmpty(overridePackage) && Objects.equals(overridePackage, callingPackage)) {
                        return;
                    }
                }
                overridePackage = this.mCarrierConfigManager.getDefaultCarrierServicePackageName();
                if (TextUtils.isEmpty(overridePackage) || !Objects.equals(overridePackage, callingPackage)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("persist.sys.sub_plan_owner.");
                    stringBuilder.append(subId);
                    String testPackage = SystemProperties.get(stringBuilder.toString(), null);
                    if (TextUtils.isEmpty(testPackage) || !Objects.equals(testPackage, callingPackage)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("fw.sub_plan_owner.");
                        stringBuilder2.append(subId);
                        String legacyTestPackage = SystemProperties.get(stringBuilder2.toString(), null);
                        if (TextUtils.isEmpty(legacyTestPackage) || !Objects.equals(legacyTestPackage, callingPackage)) {
                            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_SUBSCRIPTION_PLANS", TAG);
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public SubscriptionPlan[] getSubscriptionPlans(int subId, String callingPackage) {
        int i = subId;
        String str = callingPackage;
        enforceSubscriptionPlanAccess(i, Binder.getCallingUid(), str);
        String fake = SystemProperties.get("fw.fake_plan");
        if (TextUtils.isEmpty(fake)) {
            synchronized (this.mNetworkPoliciesSecondLock) {
                String ownerPackage = (String) this.mSubscriptionPlansOwner.get(i);
                if (Objects.equals(ownerPackage, str) || UserHandle.getCallingAppId() == 1000) {
                    SubscriptionPlan[] subscriptionPlanArr = (SubscriptionPlan[]) this.mSubscriptionPlans.get(i);
                    return subscriptionPlanArr;
                }
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not returning plans because caller ");
                stringBuilder.append(str);
                stringBuilder.append(" doesn't match owner ");
                stringBuilder.append(ownerPackage);
                Log.w(str2, stringBuilder.toString());
                return null;
            }
        }
        List<SubscriptionPlan> plans = new ArrayList();
        if ("month_hard".equals(fake)) {
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile").setDataLimit(5368709120L, 1).setDataUsage(1073741824, ZonedDateTime.now().minusHours(36).toInstant().toEpochMilli()).build());
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile Happy").setDataLimit(JobStatus.NO_LATEST_RUNTIME, 1).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(36).toInstant().toEpochMilli()).build());
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Charged after limit").setDataLimit(5368709120L, 1).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(36).toInstant().toEpochMilli()).build());
        } else if ("month_soft".equals(fake)) {
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile is the carriers name who this plan belongs to").setSummary("Crazy unlimited bandwidth plan with incredibly long title that should be cut off to prevent UI from looking terrible").setDataLimit(5368709120L, 2).setDataUsage(1073741824, ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli()).build());
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Throttled after limit").setDataLimit(5368709120L, 2).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli()).build());
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, No data connection after limit").setDataLimit(5368709120L, 0).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli()).build());
        } else if ("month_over".equals(fake)) {
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile is the carriers name who this plan belongs to").setDataLimit(5368709120L, 2).setDataUsage(6442450944L, ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli()).build());
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, Throttled after limit").setDataLimit(5368709120L, 2).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli()).build());
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2017-03-14T00:00:00.000Z")).setTitle("G-Mobile, No data connection after limit").setDataLimit(5368709120L, 0).setDataUsage(5368709120L, ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli()).build());
        } else if ("month_none".equals(fake)) {
            plans.add(SubscriptionPlan.Builder.createRecurringMonthly(ZonedDateTime.parse("2007-03-14T00:00:00.000Z")).setTitle("G-Mobile").build());
        } else if ("prepaid".equals(fake)) {
            plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20), ZonedDateTime.now().plusDays(10)).setTitle("G-Mobile").setDataLimit(536870912, 0).setDataUsage(104857600, ZonedDateTime.now().minusHours(3).toInstant().toEpochMilli()).build());
        } else if ("prepaid_crazy".equals(fake)) {
            plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20), ZonedDateTime.now().plusDays(10)).setTitle("G-Mobile Anytime").setDataLimit(536870912, 0).setDataUsage(104857600, ZonedDateTime.now().minusHours(3).toInstant().toEpochMilli()).build());
            plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(10), ZonedDateTime.now().plusDays(20)).setTitle("G-Mobile Nickel Nights").setSummary("5¢/GB between 1-5AM").setDataLimit(5368709120L, 2).setDataUsage(15728640, ZonedDateTime.now().minusHours(30).toInstant().toEpochMilli()).build());
            plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(10), ZonedDateTime.now().plusDays(20)).setTitle("G-Mobile Bonus 3G").setSummary("Unlimited 3G data").setDataLimit(1073741824, 2).setDataUsage(314572800, ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli()).build());
        } else if ("unlimited".equals(fake)) {
            plans.add(SubscriptionPlan.Builder.createNonrecurring(ZonedDateTime.now().minusDays(20), ZonedDateTime.now().plusDays(10)).setTitle("G-Mobile Awesome").setDataLimit(JobStatus.NO_LATEST_RUNTIME, 2).setDataUsage(52428800, ZonedDateTime.now().minusHours(3).toInstant().toEpochMilli()).build());
        }
        return (SubscriptionPlan[]) plans.toArray(new SubscriptionPlan[plans.size()]);
    }

    public void setSubscriptionPlans(int subId, SubscriptionPlan[] plans, String callingPackage) {
        enforceSubscriptionPlanAccess(subId, Binder.getCallingUid(), callingPackage);
        for (SubscriptionPlan plan : plans) {
            Preconditions.checkNotNull(plan);
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    this.mSubscriptionPlans.put(subId, plans);
                    this.mSubscriptionPlansOwner.put(subId, callingPackage);
                    String subscriberId = (String) this.mSubIdToSubscriberId.get(subId, null);
                    if (subscriberId != null) {
                        ensureActiveMobilePolicyAL(subId, subscriberId);
                        maybeUpdateMobilePolicyCycleAL(subId, subscriberId);
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Missing subscriberId for subId ");
                        stringBuilder.append(subId);
                        Slog.wtf(str, stringBuilder.toString());
                    }
                    handleNetworkPoliciesUpdateAL(true);
                }
            }
            Intent intent = new Intent("android.telephony.action.SUBSCRIPTION_PLANS_CHANGED");
            intent.addFlags(1073741824);
            intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", subId);
            this.mContext.sendBroadcast(intent, "android.permission.MANAGE_SUBSCRIPTION_PLANS");
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void setSubscriptionPlansOwner(int subId, String packageName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("persist.sys.sub_plan_owner.");
        stringBuilder.append(subId);
        SystemProperties.set(stringBuilder.toString(), packageName);
    }

    public String getSubscriptionPlansOwner(int subId) {
        if (UserHandle.getCallingAppId() == 1000) {
            String str;
            synchronized (this.mNetworkPoliciesSecondLock) {
                str = (String) this.mSubscriptionPlansOwner.get(subId);
            }
            return str;
        }
        throw new SecurityException();
    }

    public void setSubscriptionOverride(int subId, int overrideMask, int overrideValue, long timeoutMillis, String callingPackage) {
        enforceSubscriptionPlanAccess(subId, Binder.getCallingUid(), callingPackage);
        synchronized (this.mNetworkPoliciesSecondLock) {
            SubscriptionPlan plan = getPrimarySubscriptionPlanLocked(subId);
            if (plan == null || plan.getDataLimitBehavior() == -1) {
                throw new IllegalStateException("Must provide valid SubscriptionPlan to enable overriding");
            }
        }
        boolean z = true;
        if (Global.getInt(this.mContext.getContentResolver(), "netpolicy_override_enabled", 1) == 0) {
            z = false;
        }
        if (z || overrideValue == 0) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(16, overrideMask, overrideValue, Integer.valueOf(subId)));
            if (timeoutMillis > 0) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(16, overrideMask, 0, Integer.valueOf(subId)), timeoutMillis);
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            IndentingPrintWriter fout = new IndentingPrintWriter(writer, "  ");
            ArraySet<String> argSet = new ArraySet(args.length);
            int i = 0;
            for (String arg : args) {
                argSet.add(arg);
            }
            synchronized (this.mUidRulesFirstLock) {
                synchronized (this.mNetworkPoliciesSecondLock) {
                    if (argSet.contains("--unsnooze")) {
                        for (i = this.mNetworkPolicy.size() - 1; i >= 0; i--) {
                            ((NetworkPolicy) this.mNetworkPolicy.valueAt(i)).clearSnooze();
                        }
                        handleNetworkPoliciesUpdateAL(true);
                        fout.println("Cleared snooze timestamps");
                        return;
                    }
                    int i2;
                    int subId;
                    StringBuilder stringBuilder;
                    int i3;
                    int valueAt;
                    int uid;
                    fout.print("System ready: ");
                    fout.println(this.mSystemReady);
                    fout.print("Restrict background: ");
                    fout.println(this.mRestrictBackground);
                    fout.print("Restrict power: ");
                    fout.println(this.mRestrictPower);
                    fout.print("Device idle: ");
                    fout.println(this.mDeviceIdleMode);
                    fout.print("Metered ifaces: ");
                    fout.println(String.valueOf(this.mMeteredIfaces));
                    fout.println();
                    fout.println("Network policies:");
                    fout.increaseIndent();
                    for (i2 = 0; i2 < this.mNetworkPolicy.size(); i2++) {
                        fout.println(((NetworkPolicy) this.mNetworkPolicy.valueAt(i2)).toString());
                    }
                    fout.decreaseIndent();
                    fout.println();
                    fout.println("Subscription plans:");
                    fout.increaseIndent();
                    for (i2 = 0; i2 < this.mSubscriptionPlans.size(); i2++) {
                        subId = this.mSubscriptionPlans.keyAt(i2);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Subscriber ID ");
                        stringBuilder.append(subId);
                        stringBuilder.append(":");
                        fout.println(stringBuilder.toString());
                        fout.increaseIndent();
                        SubscriptionPlan[] plans = (SubscriptionPlan[]) this.mSubscriptionPlans.valueAt(i2);
                        if (!ArrayUtils.isEmpty(plans)) {
                            for (SubscriptionPlan plan : plans) {
                                fout.println(plan);
                            }
                        }
                        fout.decreaseIndent();
                    }
                    fout.decreaseIndent();
                    fout.println();
                    fout.println("Active subscriptions:");
                    fout.increaseIndent();
                    for (i2 = 0; i2 < this.mSubIdToSubscriberId.size(); i2++) {
                        subId = this.mSubIdToSubscriberId.keyAt(i2);
                        String subscriberId = (String) this.mSubIdToSubscriberId.valueAt(i2);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(subId);
                        stringBuilder2.append("=");
                        stringBuilder2.append(NetworkIdentity.scrubSubscriberId(subscriberId));
                        fout.println(stringBuilder2.toString());
                    }
                    fout.decreaseIndent();
                    fout.println();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Merged subscriptions: ");
                    stringBuilder3.append(Arrays.toString(NetworkIdentity.scrubSubscriberId(this.mMergedSubscriberIds)));
                    fout.println(stringBuilder3.toString());
                    fout.println();
                    fout.println("Policy for UIDs:");
                    fout.increaseIndent();
                    i2 = this.mUidPolicy.size();
                    for (subId = 0; subId < i2; subId++) {
                        uid = this.mUidPolicy.keyAt(subId);
                        valueAt = this.mUidPolicy.valueAt(subId);
                        fout.print("UID=");
                        fout.print(uid);
                        fout.print(" policy=");
                        fout.print(NetworkPolicyManager.uidPoliciesToString(valueAt));
                        fout.println();
                    }
                    fout.decreaseIndent();
                    i2 = this.mPowerSaveWhitelistExceptIdleAppIds.size();
                    if (i2 > 0) {
                        fout.println("Power save whitelist (except idle) app ids:");
                        fout.increaseIndent();
                        for (subId = 0; subId < i2; subId++) {
                            fout.print("UID=");
                            fout.print(this.mPowerSaveWhitelistExceptIdleAppIds.keyAt(subId));
                            fout.print(": ");
                            fout.print(this.mPowerSaveWhitelistExceptIdleAppIds.valueAt(subId));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    i2 = this.mPowerSaveWhitelistAppIds.size();
                    if (i2 > 0) {
                        fout.println("Power save whitelist app ids:");
                        fout.increaseIndent();
                        for (subId = 0; subId < i2; subId++) {
                            fout.print("UID=");
                            fout.print(this.mPowerSaveWhitelistAppIds.keyAt(subId));
                            fout.print(": ");
                            fout.print(this.mPowerSaveWhitelistAppIds.valueAt(subId));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    i2 = this.mDefaultRestrictBackgroundWhitelistUids.size();
                    if (i2 > 0) {
                        fout.println("Default restrict background whitelist uids:");
                        fout.increaseIndent();
                        for (subId = 0; subId < i2; subId++) {
                            fout.print("UID=");
                            fout.print(this.mDefaultRestrictBackgroundWhitelistUids.keyAt(subId));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    i2 = this.mRestrictBackgroundWhitelistRevokedUids.size();
                    if (i2 > 0) {
                        fout.println("Default restrict background whitelist uids revoked by users:");
                        fout.increaseIndent();
                        for (subId = 0; subId < i2; subId++) {
                            fout.print("UID=");
                            fout.print(this.mRestrictBackgroundWhitelistRevokedUids.keyAt(subId));
                            fout.println();
                        }
                        fout.decreaseIndent();
                    }
                    SparseBooleanArray knownUids = new SparseBooleanArray();
                    collectKeys(this.mUidState, knownUids);
                    collectKeys(this.mUidRules, knownUids);
                    fout.println("Status for all known UIDs:");
                    fout.increaseIndent();
                    i2 = knownUids.size();
                    for (uid = 0; uid < i2; uid++) {
                        valueAt = knownUids.keyAt(uid);
                        fout.print("UID=");
                        fout.print(valueAt);
                        i3 = this.mUidState.get(valueAt, 18);
                        fout.print(" state=");
                        fout.print(i3);
                        if (i3 <= 2) {
                            fout.print(" (fg)");
                        } else {
                            fout.print(i3 <= 4 ? " (fg svc)" : " (bg)");
                        }
                        int uidRules = this.mUidRules.get(valueAt, 0);
                        fout.print(" rules=");
                        fout.print(NetworkPolicyManager.uidRulesToString(uidRules));
                        fout.println();
                    }
                    fout.decreaseIndent();
                    fout.println("Status for just UIDs with rules:");
                    fout.increaseIndent();
                    i2 = this.mUidRules.size();
                    for (uid = 0; uid < i2; uid++) {
                        valueAt = this.mUidRules.keyAt(uid);
                        fout.print("UID=");
                        fout.print(valueAt);
                        i3 = this.mUidRules.get(valueAt, 0);
                        fout.print(" rules=");
                        fout.print(NetworkPolicyManager.uidRulesToString(i3));
                        fout.println();
                    }
                    fout.decreaseIndent();
                    fout.println("Admin restricted uids for metered data:");
                    fout.increaseIndent();
                    i2 = this.mMeteredRestrictedUids.size();
                    while (i < i2) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("u");
                        stringBuilder.append(this.mMeteredRestrictedUids.keyAt(i));
                        stringBuilder.append(": ");
                        fout.print(stringBuilder.toString());
                        fout.println(this.mMeteredRestrictedUids.valueAt(i));
                        i++;
                    }
                    fout.decreaseIndent();
                    fout.println();
                    this.mStatLogger.dump(fout);
                    this.mLogger.dumpLogs(fout);
                }
            }
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new NetworkPolicyManagerShellCommand(this.mContext, this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    @VisibleForTesting
    public boolean isUidForeground(int uid) {
        boolean isUidStateForeground;
        synchronized (this.mUidRulesFirstLock) {
            isUidStateForeground = isUidStateForeground(this.mUidState.get(uid, 18));
        }
        return isUidStateForeground;
    }

    private boolean isUidForegroundOnRestrictBackgroundUL(int uid) {
        return NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(this.mUidState.get(uid, 18));
    }

    private boolean isUidForegroundOnRestrictPowerUL(int uid) {
        return NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(this.mUidState.get(uid, 18));
    }

    private boolean isUidStateForeground(int state) {
        return state <= 4;
    }

    private void updateUidStateUL(int uid, int uidState) {
        Trace.traceBegin(2097152, "updateUidStateUL");
        try {
            int oldUidState = this.mUidState.get(uid, 18);
            if (oldUidState != uidState) {
                this.mUidState.put(uid, uidState);
                updateRestrictBackgroundRulesOnUidStatusChangedUL(uid, oldUidState, uidState);
                if (NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(oldUidState) != NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(uidState)) {
                    updateRuleForAppIdleUL(uid);
                    if (this.mDeviceIdleMode) {
                        updateRuleForDeviceIdleUL(uid);
                    }
                    if (this.mRestrictPower) {
                        updateRuleForRestrictPowerUL(uid);
                    }
                    updateRulesForPowerRestrictionsUL(uid);
                }
                updateNetworkStats(uid, isUidStateForeground(uidState));
            }
            Trace.traceEnd(2097152);
        } catch (Throwable th) {
            Trace.traceEnd(2097152);
        }
    }

    private void removeUidStateUL(int uid) {
        int index = this.mUidState.indexOfKey(uid);
        if (index >= 0) {
            int oldUidState = this.mUidState.valueAt(index);
            this.mUidState.removeAt(index);
            if (oldUidState != 18) {
                updateRestrictBackgroundRulesOnUidStatusChangedUL(uid, oldUidState, 18);
                if (this.mDeviceIdleMode) {
                    updateRuleForDeviceIdleUL(uid);
                }
                if (this.mRestrictPower) {
                    updateRuleForRestrictPowerUL(uid);
                }
                updateRulesForPowerRestrictionsUL(uid);
                updateNetworkStats(uid, false);
            }
        }
    }

    private void updateNetworkStats(int uid, boolean uidForeground) {
        if (Trace.isTagEnabled(2097152)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateNetworkStats: ");
            stringBuilder.append(uid);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(uidForeground ? "F" : "B");
            Trace.traceBegin(2097152, stringBuilder.toString());
        }
        try {
            this.mNetworkStats.setUidForeground(uid, uidForeground);
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    private void updateRestrictBackgroundRulesOnUidStatusChangedUL(int uid, int oldUidState, int newUidState) {
        if (NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(oldUidState) != NetworkPolicyManager.isProcStateAllowedWhileOnRestrictBackground(newUidState)) {
            updateRulesForDataUsageRestrictionsUL(uid);
        }
    }

    void updateRulesForPowerSaveUL() {
        Trace.traceBegin(2097152, "updateRulesForPowerSaveUL");
        try {
            updateRulesForWhitelistedPowerSaveUL(this.mRestrictPower, 3, this.mUidFirewallPowerSaveRules);
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    void updateRuleForRestrictPowerUL(int uid) {
        updateRulesForWhitelistedPowerSaveUL(uid, this.mRestrictPower, 3);
    }

    void updateRulesForDeviceIdleUL() {
        Trace.traceBegin(2097152, "updateRulesForDeviceIdleUL");
        try {
            updateRulesForWhitelistedPowerSaveUL(this.mDeviceIdleMode, 1, this.mUidFirewallDozableRules);
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    void updateRuleForDeviceIdleUL(int uid) {
        updateRulesForWhitelistedPowerSaveUL(uid, this.mDeviceIdleMode, 1);
    }

    private void updateRulesForWhitelistedPowerSaveUL(boolean enabled, int chain, SparseIntArray rules) {
        if (enabled) {
            int ui;
            SparseIntArray uidRules = rules;
            uidRules.clear();
            List<UserInfo> users = this.mUserManager.getUsers();
            for (ui = users.size() - 1; ui >= 0; ui--) {
                UserInfo user = (UserInfo) users.get(ui);
                updateRulesForWhitelistedAppIds(uidRules, this.mPowerSaveTempWhitelistAppIds, user.id);
                updateRulesForWhitelistedAppIds(uidRules, this.mPowerSaveWhitelistAppIds, user.id);
                if (chain == 3) {
                    updateRulesForWhitelistedAppIds(uidRules, this.mPowerSaveWhitelistExceptIdleAppIds, user.id);
                }
            }
            for (ui = this.mUidState.size() - 1; ui >= 0; ui--) {
                if (NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode(this.mUidState.valueAt(ui))) {
                    uidRules.put(this.mUidState.keyAt(ui), 1);
                }
            }
            setUidFirewallRulesUL(chain, uidRules, 1);
            return;
        }
        setUidFirewallRulesUL(chain, null, 2);
    }

    private void updateRulesForWhitelistedAppIds(SparseIntArray uidRules, SparseBooleanArray whitelistedAppIds, int userId) {
        for (int i = whitelistedAppIds.size() - 1; i >= 0; i--) {
            if (whitelistedAppIds.valueAt(i)) {
                uidRules.put(UserHandle.getUid(userId, whitelistedAppIds.keyAt(i)), 1);
            }
        }
    }

    private boolean isWhitelistedBatterySaverUL(int uid, boolean deviceIdleMode) {
        int appId = UserHandle.getAppId(uid);
        boolean z = true;
        boolean isWhitelisted = this.mPowerSaveTempWhitelistAppIds.get(appId) || this.mPowerSaveWhitelistAppIds.get(appId);
        if (deviceIdleMode) {
            return isWhitelisted;
        }
        if (!(isWhitelisted || this.mPowerSaveWhitelistExceptIdleAppIds.get(appId))) {
            z = false;
        }
        return z;
    }

    private void updateRulesForWhitelistedPowerSaveUL(int uid, boolean enabled, int chain) {
        if (enabled) {
            if (isWhitelistedBatterySaverUL(uid, chain == 1) || isUidForegroundOnRestrictPowerUL(uid)) {
                setUidFirewallRule(chain, uid, 1);
            } else {
                setUidFirewallRule(chain, uid, 0);
            }
        }
    }

    void updateRulesForAppIdleUL() {
        Trace.traceBegin(2097152, "updateRulesForAppIdleUL");
        try {
            SparseIntArray uidRules = this.mUidFirewallStandbyRules;
            uidRules.clear();
            List<UserInfo> users = this.mUserManager.getUsers();
            int ui = users.size();
            while (true) {
                ui--;
                if (ui < 0) {
                    break;
                }
                for (int uid : this.mUsageStats.getIdleUidsForUser(((UserInfo) users.get(ui)).id)) {
                    if (!this.mPowerSaveTempWhitelistAppIds.get(UserHandle.getAppId(uid), false) && hasInternetPermissions(uid)) {
                        uidRules.put(uid, 2);
                    }
                }
            }
            setUidFirewallRulesUL(2, uidRules, 0);
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    void updateRuleForAppIdleUL(int uid) {
        if (isUidValidForBlacklistRules(uid)) {
            if (Trace.isTagEnabled(2097152)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateRuleForAppIdleUL: ");
                stringBuilder.append(uid);
                Trace.traceBegin(2097152, stringBuilder.toString());
            }
            try {
                if (this.mPowerSaveTempWhitelistAppIds.get(UserHandle.getAppId(uid)) || !isUidIdle(uid) || isUidForegroundOnRestrictPowerUL(uid)) {
                    setUidFirewallRule(2, uid, 0);
                } else {
                    setUidFirewallRule(2, uid, 2);
                }
                Trace.traceEnd(2097152);
            } catch (Throwable th) {
                Trace.traceEnd(2097152);
            }
        }
    }

    void updateRulesForAppIdleParoleUL() {
        boolean paroled = this.mUsageStats.isAppIdleParoleOn();
        boolean enableChain = paroled ^ 1;
        enableFirewallChainUL(2, enableChain);
        int ruleCount = this.mUidFirewallStandbyRules.size();
        for (int i = 0; i < ruleCount; i++) {
            int uid = this.mUidFirewallStandbyRules.keyAt(i);
            int oldRules = this.mUidRules.get(uid);
            if (enableChain) {
                oldRules &= 15;
            } else if ((oldRules & 240) == 0) {
            }
            int newUidRules = updateRulesForPowerRestrictionsUL(uid, oldRules, paroled);
            if (newUidRules == 0) {
                this.mUidRules.delete(uid);
            } else {
                this.mUidRules.put(uid, newUidRules);
            }
        }
    }

    private void updateRulesForGlobalChangeAL(boolean restrictedNetworksChanged) {
        if (Trace.isTagEnabled(2097152)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateRulesForGlobalChangeAL: ");
            stringBuilder.append(restrictedNetworksChanged ? "R" : "-");
            Trace.traceBegin(2097152, stringBuilder.toString());
        }
        try {
            updateRulesForAppIdleUL();
            updateRulesForRestrictPowerUL();
            updateRulesForRestrictBackgroundUL();
            if (restrictedNetworksChanged) {
                normalizePoliciesNL();
                updateNetworkRulesNL();
            }
            Trace.traceEnd(2097152);
        } catch (Throwable th) {
            Trace.traceEnd(2097152);
        }
    }

    private void updateRulesForRestrictPowerUL() {
        Trace.traceBegin(2097152, "updateRulesForRestrictPowerUL");
        try {
            updateRulesForDeviceIdleUL();
            updateRulesForPowerSaveUL();
            updateRulesForAllAppsUL(2);
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    private void updateRulesForRestrictBackgroundUL() {
        Trace.traceBegin(2097152, "updateRulesForRestrictBackgroundUL");
        try {
            updateRulesForAllAppsUL(1);
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    private void updateRulesForAllAppsUL(int type) {
        PackageManager pm;
        int i = type;
        long j = 2097152;
        if (Trace.isTagEnabled(2097152)) {
            StringBuilder stringBuilder = new StringBuilder();
            pm = "updateRulesForRestrictPowerUL-";
            stringBuilder.append(pm);
            stringBuilder.append(i);
            Trace.traceBegin(2097152, stringBuilder.toString());
        }
        List<ApplicationInfo> apps;
        try {
            pm = this.mContext.getPackageManager();
            Trace.traceBegin(2097152, "list-users");
            List<UserInfo> users = this.mUserManager.getUsers();
            Trace.traceEnd(2097152);
            apps = "list-uids";
            Trace.traceBegin(2097152, apps);
            apps = pm.getInstalledApplications(apps);
            Trace.traceEnd(j);
            int usersSize = users.size();
            int appsSize = apps.size();
            int i2;
            while (i2 < usersSize) {
                UserInfo user = (UserInfo) users.get(i2);
                for (int j2 = 0; j2 < appsSize; j2++) {
                    int uid = UserHandle.getUid(user.id, ((ApplicationInfo) apps.get(j2)).uid);
                    switch (i) {
                        case 1:
                            updateRulesForDataUsageRestrictionsUL(uid);
                            break;
                        case 2:
                            updateRulesForPowerRestrictionsUL(uid);
                            break;
                        default:
                            String str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Invalid type for updateRulesForAllApps: ");
                            stringBuilder2.append(i);
                            Slog.w(str, stringBuilder2.toString());
                            break;
                    }
                }
                i2++;
            }
            Trace.traceEnd(2097152);
        } catch (Throwable th) {
            apps = th;
            Trace.traceEnd(j);
        } finally {
            j = 2097152;
            Trace.traceEnd(2097152);
            List<ApplicationInfo> list = apps;
        }
    }

    private void updateRulesForTempWhitelistChangeUL(int appId) {
        List<UserInfo> users = this.mUserManager.getUsers();
        int numUsers = users.size();
        for (int i = 0; i < numUsers; i++) {
            int uid = UserHandle.getUid(((UserInfo) users.get(i)).id, appId);
            updateRuleForAppIdleUL(uid);
            updateRuleForDeviceIdleUL(uid);
            updateRuleForRestrictPowerUL(uid);
            updateRulesForPowerRestrictionsUL(uid);
        }
    }

    private boolean isUidValidForBlacklistRules(int uid) {
        if (uid == 1013 || uid == 1019 || (UserHandle.isApp(uid) && hasInternetPermissions(uid))) {
            return true;
        }
        return false;
    }

    private boolean isUidValidForWhitelistRules(int uid) {
        return UserHandle.isApp(uid) && hasInternetPermissions(uid);
    }

    private boolean isUidIdle(int uid) {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        int userId = UserHandle.getUserId(uid);
        if (packages != null) {
            for (String packageName : packages) {
                if (!this.mUsageStats.isAppIdle(packageName, uid, userId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasInternetPermissions(int uid) {
        try {
            if (this.mIPm.checkUidPermission("android.permission.INTERNET", uid) != 0) {
                return false;
            }
        } catch (RemoteException e) {
        }
        return true;
    }

    private void onUidDeletedUL(int uid) {
        this.mUidRules.delete(uid);
        this.mUidPolicy.delete(uid);
        this.mUidFirewallStandbyRules.delete(uid);
        this.mUidFirewallDozableRules.delete(uid);
        this.mUidFirewallPowerSaveRules.delete(uid);
        this.mPowerSaveWhitelistExceptIdleAppIds.delete(uid);
        this.mPowerSaveWhitelistAppIds.delete(uid);
        this.mPowerSaveTempWhitelistAppIds.delete(uid);
        this.mHandler.obtainMessage(15, uid, 0).sendToTarget();
    }

    private void updateRestrictionRulesForUidUL(int uid) {
        updateRuleForDeviceIdleUL(uid);
        updateRuleForAppIdleUL(uid);
        updateRuleForRestrictPowerUL(uid);
        updateRulesForPowerRestrictionsUL(uid);
        updateRulesForDataUsageRestrictionsUL(uid);
    }

    private void updateRulesForDataUsageRestrictionsUL(int uid) {
        if (Trace.isTagEnabled(2097152)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateRulesForDataUsageRestrictionsUL: ");
            stringBuilder.append(uid);
            Trace.traceBegin(2097152, stringBuilder.toString());
        }
        try {
            updateRulesForDataUsageRestrictionsULInner(uid);
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    private void updateRulesForDataUsageRestrictionsULInner(int uid) {
        if (isUidValidForWhitelistRules(uid)) {
            String str;
            StringBuilder stringBuilder;
            boolean z = false;
            int uidPolicy = this.mUidPolicy.get(uid, 0);
            int oldUidRules = this.mUidRules.get(uid, 0);
            boolean isForeground = isUidForegroundOnRestrictBackgroundUL(uid);
            boolean isRestrictedByAdmin = isRestrictedByAdminUL(uid);
            boolean isBlacklisted = (uidPolicy & 1) != 0;
            boolean isWhitelisted = (uidPolicy & 4) != 0;
            int oldRule = oldUidRules & 15;
            int newRule = 0;
            if (isRestrictedByAdmin) {
                newRule = 4;
            } else if (isForeground) {
                if (isBlacklisted || (this.mRestrictBackground && !isWhitelisted)) {
                    newRule = 2;
                } else if (isWhitelisted) {
                    newRule = 1;
                }
            } else if (isBlacklisted) {
                newRule = 4;
            } else if (this.mRestrictBackground && isWhitelisted) {
                newRule = 1;
            }
            int newUidRules = (oldUidRules & 240) | newRule;
            if (LOGV) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateRuleForRestrictBackgroundUL(");
                stringBuilder.append(uid);
                stringBuilder.append("): isForeground=");
                stringBuilder.append(isForeground);
                stringBuilder.append(", isBlacklisted=");
                stringBuilder.append(isBlacklisted);
                stringBuilder.append(", isWhitelisted=");
                stringBuilder.append(isWhitelisted);
                stringBuilder.append(", isRestrictedByAdmin=");
                stringBuilder.append(isRestrictedByAdmin);
                stringBuilder.append(", oldRule=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(oldRule));
                stringBuilder.append(", newRule=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(newRule));
                stringBuilder.append(", newUidRules=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(newUidRules));
                stringBuilder.append(", oldUidRules=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(oldUidRules));
                Log.v(str, stringBuilder.toString());
            }
            if (newUidRules == 0) {
                this.mUidRules.delete(uid);
            } else {
                this.mUidRules.put(uid, newUidRules);
            }
            if (newRule != oldRule) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateRuleForRestrictBackgroundUL(");
                stringBuilder.append(uid);
                stringBuilder.append("): isForeground=");
                stringBuilder.append(isForeground);
                stringBuilder.append(", isBlacklisted=");
                stringBuilder.append(isBlacklisted);
                stringBuilder.append(", isWhitelisted=");
                stringBuilder.append(isWhitelisted);
                stringBuilder.append(", isRestrictedByAdmin=");
                stringBuilder.append(isRestrictedByAdmin);
                stringBuilder.append(", oldRule=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(oldRule));
                stringBuilder.append(", newRule=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(newRule));
                stringBuilder.append(", newUidRules=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(newUidRules));
                stringBuilder.append(", oldUidRules=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(oldUidRules));
                stringBuilder.append(", mRestrictBackground=");
                stringBuilder.append(this.mRestrictBackground);
                Log.i(str, stringBuilder.toString());
                if (hasRule(newRule, 2)) {
                    setMeteredNetworkWhitelist(uid, true);
                    if (isBlacklisted) {
                        setMeteredNetworkBlacklist(uid, false);
                    }
                } else if (hasRule(oldRule, 2)) {
                    if (!isWhitelisted) {
                        setMeteredNetworkWhitelist(uid, false);
                    }
                    if (isBlacklisted || isRestrictedByAdmin) {
                        setMeteredNetworkBlacklist(uid, true);
                    }
                } else if (hasRule(newRule, 4) || hasRule(oldRule, 4)) {
                    if (isBlacklisted || isRestrictedByAdmin) {
                        z = true;
                    }
                    setMeteredNetworkBlacklist(uid, z);
                    if (hasRule(oldRule, 4) && isWhitelisted) {
                        setMeteredNetworkWhitelist(uid, isWhitelisted);
                    }
                } else if (hasRule(newRule, 1) || hasRule(oldRule, 1)) {
                    setMeteredNetworkWhitelist(uid, isWhitelisted);
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unexpected change of metered UID state for ");
                    stringBuilder2.append(uid);
                    stringBuilder2.append(": foreground=");
                    stringBuilder2.append(isForeground);
                    stringBuilder2.append(", whitelisted=");
                    stringBuilder2.append(isWhitelisted);
                    stringBuilder2.append(", blacklisted=");
                    stringBuilder2.append(isBlacklisted);
                    stringBuilder2.append(", isRestrictedByAdmin=");
                    stringBuilder2.append(isRestrictedByAdmin);
                    stringBuilder2.append(", newRule=");
                    stringBuilder2.append(NetworkPolicyManager.uidRulesToString(newUidRules));
                    stringBuilder2.append(", oldRule=");
                    stringBuilder2.append(NetworkPolicyManager.uidRulesToString(oldUidRules));
                    Log.wtf(str2, stringBuilder2.toString());
                }
                this.mHandler.obtainMessage(1, uid, newUidRules).sendToTarget();
            }
            return;
        }
        if (LOGD) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("no need to update restrict data rules for uid ");
            stringBuilder3.append(uid);
            Slog.d(str3, stringBuilder3.toString());
        }
    }

    private void updateRulesForPowerRestrictionsUL(int uid) {
        int newUidRules = updateRulesForPowerRestrictionsUL(uid, this.mUidRules.get(uid, 0), false);
        if (newUidRules == 0) {
            this.mUidRules.delete(uid);
        } else {
            this.mUidRules.put(uid, newUidRules);
        }
    }

    private int updateRulesForPowerRestrictionsUL(int uid, int oldUidRules, boolean paroled) {
        if (Trace.isTagEnabled(2097152)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateRulesForPowerRestrictionsUL: ");
            stringBuilder.append(uid);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(oldUidRules);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(paroled ? "P" : "-");
            Trace.traceBegin(2097152, stringBuilder.toString());
        }
        try {
            int updateRulesForPowerRestrictionsULInner = updateRulesForPowerRestrictionsULInner(uid, oldUidRules, paroled);
            return updateRulesForPowerRestrictionsULInner;
        } finally {
            Trace.traceEnd(2097152);
        }
    }

    private int updateRulesForPowerRestrictionsULInner(int uid, int oldUidRules, boolean paroled) {
        int i = uid;
        int i2 = oldUidRules;
        boolean restrictMode = false;
        String str;
        if (isUidValidForBlacklistRules(uid)) {
            boolean isIdle = !paroled && isUidIdle(uid);
            if (isIdle || this.mRestrictPower || this.mDeviceIdleMode) {
                restrictMode = true;
            }
            boolean isForeground = isUidForegroundOnRestrictPowerUL(uid);
            boolean isWhitelisted = isWhitelistedBatterySaverUL(i, this.mDeviceIdleMode);
            int oldRule = i2 & 240;
            int newRule = 0;
            if (isForeground) {
                if (restrictMode) {
                    newRule = 32;
                }
            } else if (restrictMode) {
                newRule = isWhitelisted ? 32 : 64;
            }
            int newUidRules = (i2 & 15) | newRule;
            if (LOGV) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateRulesForPowerRestrictionsUL(");
                stringBuilder.append(i);
                stringBuilder.append("), isIdle: ");
                stringBuilder.append(isIdle);
                stringBuilder.append(", mRestrictPower: ");
                stringBuilder.append(this.mRestrictPower);
                stringBuilder.append(", mDeviceIdleMode: ");
                stringBuilder.append(this.mDeviceIdleMode);
                stringBuilder.append(", isForeground=");
                stringBuilder.append(isForeground);
                stringBuilder.append(", isWhitelisted=");
                stringBuilder.append(isWhitelisted);
                stringBuilder.append(", oldRule=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(oldRule));
                stringBuilder.append(", newRule=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(newRule));
                stringBuilder.append(", newUidRules=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(newUidRules));
                stringBuilder.append(", oldUidRules=");
                stringBuilder.append(NetworkPolicyManager.uidRulesToString(oldUidRules));
                Log.v(str2, stringBuilder.toString());
            }
            if (newRule != oldRule) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateRulesForPowerRestrictionsUL(");
                stringBuilder2.append(i);
                stringBuilder2.append("), isIdle: ");
                stringBuilder2.append(isIdle);
                stringBuilder2.append(", mRestrictPower: ");
                stringBuilder2.append(this.mRestrictPower);
                stringBuilder2.append(", mDeviceIdleMode: ");
                stringBuilder2.append(this.mDeviceIdleMode);
                stringBuilder2.append(", isForeground=");
                stringBuilder2.append(isForeground);
                stringBuilder2.append(", isWhitelisted=");
                stringBuilder2.append(isWhitelisted);
                stringBuilder2.append(", oldRule=");
                stringBuilder2.append(NetworkPolicyManager.uidRulesToString(oldRule));
                stringBuilder2.append(", newRule=");
                stringBuilder2.append(NetworkPolicyManager.uidRulesToString(newRule));
                stringBuilder2.append(", newUidRules=");
                stringBuilder2.append(NetworkPolicyManager.uidRulesToString(newUidRules));
                stringBuilder2.append(", oldUidRules=");
                stringBuilder2.append(NetworkPolicyManager.uidRulesToString(oldUidRules));
                stringBuilder2.append(", mRestrictBackground=");
                stringBuilder2.append(this.mRestrictBackground);
                Log.i(str, stringBuilder2.toString());
                StringBuilder stringBuilder3;
                if (newRule == 0 || hasRule(newRule, 32)) {
                    if (LOGV) {
                        str = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Allowing non-metered access for UID ");
                        stringBuilder3.append(i);
                        Log.v(str, stringBuilder3.toString());
                    }
                } else if (!hasRule(newRule, 64)) {
                    str = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Unexpected change of non-metered UID state for ");
                    stringBuilder3.append(i);
                    stringBuilder3.append(": foreground=");
                    stringBuilder3.append(isForeground);
                    stringBuilder3.append(", whitelisted=");
                    stringBuilder3.append(isWhitelisted);
                    stringBuilder3.append(", newRule=");
                    stringBuilder3.append(NetworkPolicyManager.uidRulesToString(newUidRules));
                    stringBuilder3.append(", oldRule=");
                    stringBuilder3.append(NetworkPolicyManager.uidRulesToString(oldUidRules));
                    Log.wtf(str, stringBuilder3.toString());
                } else if (LOGV) {
                    str = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Rejecting non-metered access for UID ");
                    stringBuilder3.append(i);
                    Log.v(str, stringBuilder3.toString());
                }
                this.mHandler.obtainMessage(1, i, newUidRules).sendToTarget();
            }
            return newUidRules;
        }
        if (LOGD) {
            str = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("no need to update restrict power rules for uid ");
            stringBuilder4.append(i);
            Slog.d(str, stringBuilder4.toString());
        }
        return 0;
    }

    private void dispatchUidRulesChanged(INetworkPolicyListener listener, int uid, int uidRules) {
        if (listener != null) {
            try {
                listener.onUidRulesChanged(uid, uidRules);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchMeteredIfacesChanged(INetworkPolicyListener listener, String[] meteredIfaces) {
        if (listener != null) {
            try {
                listener.onMeteredIfacesChanged(meteredIfaces);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchRestrictBackgroundChanged(INetworkPolicyListener listener, boolean restrictBackground) {
        if (listener != null) {
            try {
                listener.onRestrictBackgroundChanged(restrictBackground);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchUidPoliciesChanged(INetworkPolicyListener listener, int uid, int uidPolicies) {
        if (listener != null) {
            try {
                listener.onUidPoliciesChanged(uid, uidPolicies);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchSubscriptionOverride(INetworkPolicyListener listener, int subId, int overrideMask, int overrideValue) {
        if (listener != null) {
            try {
                listener.onSubscriptionOverride(subId, overrideMask, overrideValue);
            } catch (RemoteException e) {
            }
        }
    }

    void handleUidChanged(int uid, int procState, long procStateSeq) {
        Trace.traceBegin(2097152, "onUidStateChanged");
        try {
            synchronized (this.mUidRulesFirstLock) {
                this.mLogger.uidStateChanged(uid, procState, procStateSeq);
                updateUidStateUL(uid, procState);
                this.mActivityManagerInternal.notifyNetworkPolicyRulesUpdated(uid, procStateSeq);
            }
            Trace.traceEnd(2097152);
        } catch (Throwable th) {
            Trace.traceEnd(2097152);
        }
    }

    void handleUidGone(int uid) {
        Trace.traceBegin(2097152, "onUidGone");
        try {
            synchronized (this.mUidRulesFirstLock) {
                removeUidStateUL(uid);
            }
            Trace.traceEnd(2097152);
        } catch (Throwable th) {
            Trace.traceEnd(2097152);
        }
    }

    private void broadcastRestrictBackgroundChanged(int uid, Boolean changed) {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (packages != null) {
            int userId = UserHandle.getUserId(uid);
            for (String packageName : packages) {
                Intent intent = new Intent("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
                intent.setPackage(packageName);
                intent.setFlags(1073741824);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
            }
        }
    }

    private void setInterfaceQuotaAsync(String iface, long quotaBytes) {
        this.mHandler.obtainMessage(10, (int) (quotaBytes >> 32), (int) (-1 & quotaBytes), iface).sendToTarget();
    }

    private void setInterfaceQuota(String iface, long quotaBytes) {
        try {
            this.mNetworkManager.setInterfaceQuota(iface, quotaBytes);
            if (HuaweiTelephonyConfigs.isQcomPlatform()) {
                NetPluginDelegate.setQuota(iface, quotaBytes);
            }
        } catch (IllegalStateException e) {
            Log.wtf(TAG, "problem setting interface quota", e);
        } catch (RemoteException e2) {
        }
    }

    private void removeInterfaceQuotaAsync(String iface) {
        this.mHandler.obtainMessage(11, iface).sendToTarget();
    }

    private void removeInterfaceQuota(String iface) {
        try {
            this.mNetworkManager.removeInterfaceQuota(iface);
        } catch (IllegalStateException e) {
            Log.wtf(TAG, "problem removing interface quota", e);
        } catch (RemoteException e2) {
        }
    }

    private void setMeteredNetworkBlacklist(int uid, boolean enable) {
        if (LOGV) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setMeteredNetworkBlacklist ");
            stringBuilder.append(uid);
            stringBuilder.append(": ");
            stringBuilder.append(enable);
            Slog.v(str, stringBuilder.toString());
        }
        try {
            this.mNetworkManager.setUidMeteredNetworkBlacklist(uid, enable);
        } catch (IllegalStateException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("problem setting blacklist (");
            stringBuilder2.append(enable);
            stringBuilder2.append(") rules for ");
            stringBuilder2.append(uid);
            Log.wtf(str2, stringBuilder2.toString(), e);
        } catch (RemoteException e2) {
        }
    }

    private void setMeteredNetworkWhitelist(int uid, boolean enable) {
        if (LOGV) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setMeteredNetworkWhitelist ");
            stringBuilder.append(uid);
            stringBuilder.append(": ");
            stringBuilder.append(enable);
            Slog.v(str, stringBuilder.toString());
        }
        try {
            this.mNetworkManager.setUidMeteredNetworkWhitelist(uid, enable);
        } catch (IllegalStateException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("problem setting whitelist (");
            stringBuilder2.append(enable);
            stringBuilder2.append(") rules for ");
            stringBuilder2.append(uid);
            Log.wtf(str2, stringBuilder2.toString(), e);
        } catch (RemoteException e2) {
        }
    }

    private void setUidFirewallRulesUL(int chain, SparseIntArray uidRules, int toggle) {
        if (uidRules != null) {
            setUidFirewallRulesUL(chain, uidRules);
        }
        if (toggle != 0) {
            boolean z = true;
            if (toggle != 1) {
                z = false;
            }
            enableFirewallChainUL(chain, z);
        }
    }

    private void setUidFirewallRulesUL(int chain, SparseIntArray uidRules) {
        try {
            int size = uidRules.size();
            int[] uids = new int[size];
            int[] rules = new int[size];
            for (int index = size - 1; index >= 0; index--) {
                uids[index] = uidRules.keyAt(index);
                rules[index] = uidRules.valueAt(index);
            }
            this.mNetworkManager.setFirewallUidRules(chain, uids, rules);
            this.mLogger.firewallRulesChanged(chain, uids, rules);
        } catch (IllegalStateException e) {
            Log.wtf(TAG, "problem setting firewall uid rules", e);
        } catch (RemoteException e2) {
        }
    }

    private void setUidFirewallRule(int chain, int uid, int rule) {
        if (Trace.isTagEnabled(2097152)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setUidFirewallRule: ");
            stringBuilder.append(chain);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(uid);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(rule);
            Trace.traceBegin(2097152, stringBuilder.toString());
        }
        if (chain == 1) {
            try {
                this.mUidFirewallDozableRules.put(uid, rule);
            } catch (IllegalStateException e) {
                Log.wtf(TAG, "problem setting firewall uid rules", e);
            } catch (RemoteException e2) {
            } catch (Throwable th) {
                Trace.traceEnd(2097152);
            }
        } else if (chain == 2) {
            this.mUidFirewallStandbyRules.put(uid, rule);
        } else if (chain == 3) {
            this.mUidFirewallPowerSaveRules.put(uid, rule);
        }
        this.mNetworkManager.setFirewallUidRule(chain, uid, rule);
        this.mLogger.uidFirewallRuleChanged(chain, uid, rule);
        Trace.traceEnd(2097152);
    }

    private void enableFirewallChainUL(int chain, boolean enable) {
        if (this.mFirewallChainStates.indexOfKey(chain) < 0 || this.mFirewallChainStates.get(chain) != enable) {
            this.mFirewallChainStates.put(chain, enable);
            try {
                this.mNetworkManager.setFirewallChainEnabled(chain, enable);
                this.mLogger.firewallChainEnabled(chain, enable);
            } catch (IllegalStateException e) {
                Log.wtf(TAG, "problem enable firewall chain", e);
            } catch (RemoteException e2) {
            }
        }
    }

    private void resetUidFirewallRules(int uid) {
        try {
            this.mNetworkManager.setFirewallUidRule(1, uid, 0);
            this.mNetworkManager.setFirewallUidRule(2, uid, 0);
            this.mNetworkManager.setFirewallUidRule(3, uid, 0);
            this.mNetworkManager.setUidMeteredNetworkWhitelist(uid, false);
            this.mNetworkManager.setUidMeteredNetworkBlacklist(uid, false);
        } catch (IllegalStateException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("problem resetting firewall uid rules for ");
            stringBuilder.append(uid);
            Log.wtf(str, stringBuilder.toString(), e);
        } catch (RemoteException e2) {
        }
    }

    @Deprecated
    private long getTotalBytes(NetworkTemplate template, long start, long end) {
        return getNetworkTotalBytes(template, start, end);
    }

    private long getNetworkTotalBytes(NetworkTemplate template, long start, long end) {
        try {
            return this.mNetworkStats.getNetworkTotalBytes(template, start, end);
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read network stats: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
            return 0;
        }
    }

    private NetworkStats getNetworkUidBytes(NetworkTemplate template, long start, long end) {
        try {
            return this.mNetworkStats.getNetworkUidBytes(template, start, end);
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read network stats: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
            return new NetworkStats(SystemClock.elapsedRealtime(), 0);
        }
    }

    private boolean isBandwidthControlEnabled() {
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = this.mNetworkManager.isBandwidthControlEnabled();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static Intent buildAllowBackgroundDataIntent() {
        return new Intent(ACTION_ALLOW_BACKGROUND);
    }

    private static Intent buildSnoozeWarningIntent(NetworkTemplate template) {
        Intent intent = new Intent(ACTION_SNOOZE_WARNING);
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", template);
        return intent;
    }

    private static Intent buildSnoozeRapidIntent(NetworkTemplate template) {
        Intent intent = new Intent(ACTION_SNOOZE_RAPID);
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", template);
        return intent;
    }

    private static Intent buildNetworkOverLimitIntent(Resources res, NetworkTemplate template) {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(res.getString(17039833)));
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", template);
        return intent;
    }

    private static Intent buildViewDataUsageIntent(Resources res, NetworkTemplate template) {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(res.getString(17039779)));
        intent.addFlags(268435456);
        intent.putExtra("android.net.NETWORK_TEMPLATE", template);
        return intent;
    }

    @VisibleForTesting
    public void addIdleHandler(IdleHandler handler) {
        this.mHandler.getLooper().getQueue().addIdleHandler(handler);
    }

    @VisibleForTesting
    public void updateRestrictBackgroundByLowPowerModeUL(PowerSaveState result) {
        boolean shouldInvokeRestrictBackground;
        this.mRestrictBackgroundPowerState = result;
        boolean restrictBackground = result.batterySaverEnabled;
        boolean localRestrictBgChangedInBsm = this.mRestrictBackgroundChangedInBsm;
        boolean z = true;
        if (result.globalBatterySaverEnabled) {
            if (this.mRestrictBackground || !result.batterySaverEnabled) {
                z = false;
            }
            shouldInvokeRestrictBackground = z;
            this.mRestrictBackgroundBeforeBsm = this.mRestrictBackground;
            localRestrictBgChangedInBsm = false;
        } else {
            shouldInvokeRestrictBackground = this.mRestrictBackgroundChangedInBsm ^ true;
            restrictBackground = this.mRestrictBackgroundBeforeBsm;
        }
        if (shouldInvokeRestrictBackground) {
            setRestrictBackgroundUL(restrictBackground);
        }
        this.mRestrictBackgroundChangedInBsm = localRestrictBgChangedInBsm;
    }

    private static void collectKeys(SparseIntArray source, SparseBooleanArray target) {
        int size = source.size();
        for (int i = 0; i < size; i++) {
            target.put(source.keyAt(i), true);
        }
    }

    public void factoryReset(String subscriber) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (!this.mUserManager.hasUserRestriction("no_network_reset")) {
            NetworkPolicy[] policies = getNetworkPolicies(this.mContext.getOpPackageName());
            NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(subscriber);
            for (NetworkPolicy policy : policies) {
                if (policy.template.equals(template)) {
                    policy.limitBytes = -1;
                    policy.inferred = false;
                    policy.clearSnooze();
                }
            }
            setNetworkPolicies(policies);
            setRestrictBackground(false);
            if (!this.mUserManager.hasUserRestriction("no_control_apps")) {
                for (int uid : getUidsWithPolicy(1)) {
                    setUidPolicy(uid, 0);
                }
            }
        }
    }

    public boolean isUidNetworkingBlocked(int uid, boolean isNetworkMetered) {
        long startTime = this.mStatLogger.getTime();
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        boolean ret = isUidNetworkingBlockedInternal(uid, isNetworkMetered);
        this.mStatLogger.logDurationStat(1, startTime);
        return ret;
    }

    private boolean isUidNetworkingBlockedInternal(int uid, boolean isNetworkMetered) {
        int uidRules;
        boolean isBackgroundRestricted;
        synchronized (this.mUidRulesFirstLock) {
            uidRules = this.mUidRules.get(uid, 0);
            isBackgroundRestricted = this.mRestrictBackground;
        }
        if (hasRule(uidRules, 64)) {
            this.mLogger.networkBlocked(uid, 0);
            return true;
        } else if (!isNetworkMetered) {
            this.mLogger.networkBlocked(uid, 1);
            return false;
        } else if (hasRule(uidRules, 4)) {
            this.mLogger.networkBlocked(uid, 2);
            return true;
        } else if (hasRule(uidRules, 1)) {
            this.mLogger.networkBlocked(uid, 3);
            return false;
        } else if (hasRule(uidRules, 2)) {
            this.mLogger.networkBlocked(uid, 4);
            return false;
        } else if (isBackgroundRestricted) {
            this.mLogger.networkBlocked(uid, 5);
            return true;
        } else {
            this.mLogger.networkBlocked(uid, 6);
            return false;
        }
    }

    private void setMeteredRestrictedPackagesInternal(Set<String> packageNames, int userId) {
        synchronized (this.mUidRulesFirstLock) {
            Set<Integer> newRestrictedUids = new ArraySet();
            for (String packageName : packageNames) {
                int uid = getUidForPackage(packageName, userId);
                if (uid >= 0) {
                    newRestrictedUids.add(Integer.valueOf(uid));
                }
            }
            Set<Integer> oldRestrictedUids = (Set) this.mMeteredRestrictedUids.get(userId);
            this.mMeteredRestrictedUids.put(userId, newRestrictedUids);
            handleRestrictedPackagesChangeUL(oldRestrictedUids, newRestrictedUids);
            this.mLogger.meteredRestrictedPkgsChanged(newRestrictedUids);
        }
    }

    private int getUidForPackage(String packageName, int userId) {
        try {
            return this.mContext.getPackageManager().getPackageUidAsUser(packageName, 4202496, userId);
        } catch (NameNotFoundException e) {
            return -1;
        }
    }

    private int parseSubId(NetworkState state) {
        if (state == null || state.networkCapabilities == null || !state.networkCapabilities.hasTransport(0)) {
            return -1;
        }
        NetworkSpecifier spec = state.networkCapabilities.getNetworkSpecifier();
        if (!(spec instanceof StringNetworkSpecifier)) {
            return -1;
        }
        try {
            return Integer.parseInt(((StringNetworkSpecifier) spec).specifier);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @GuardedBy("mNetworkPoliciesSecondLock")
    private int getSubIdLocked(Network network) {
        return this.mNetIdToSubId.get(network.netId, -1);
    }

    @GuardedBy("mNetworkPoliciesSecondLock")
    private SubscriptionPlan getPrimarySubscriptionPlanLocked(int subId) {
        SubscriptionPlan[] plans = (SubscriptionPlan[]) this.mSubscriptionPlans.get(subId);
        if (!ArrayUtils.isEmpty(plans)) {
            for (SubscriptionPlan plan : plans) {
                if (plan.getCycleRule().isRecurring() || ((Range) plan.cycleIterator().next()).contains(ZonedDateTime.now(this.mClock))) {
                    return plan;
                }
            }
        }
        return null;
    }

    private void waitForAdminData() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin")) {
            ConcurrentUtils.waitForCountDownNoInterrupt(this.mAdminDataAvailableLatch, 10000, "Wait for admin data");
        }
    }

    private void handleRestrictedPackagesChangeUL(Set<Integer> oldRestrictedUids, Set<Integer> newRestrictedUids) {
        if (oldRestrictedUids == null) {
            for (Integer uid : newRestrictedUids) {
                updateRulesForDataUsageRestrictionsUL(uid.intValue());
            }
            return;
        }
        int uid2;
        for (Integer uid3 : oldRestrictedUids) {
            uid2 = uid3.intValue();
            if (!newRestrictedUids.contains(Integer.valueOf(uid2))) {
                updateRulesForDataUsageRestrictionsUL(uid2);
            }
        }
        for (Integer uid32 : newRestrictedUids) {
            uid2 = uid32.intValue();
            if (!oldRestrictedUids.contains(Integer.valueOf(uid2))) {
                updateRulesForDataUsageRestrictionsUL(uid2);
            }
        }
    }

    private boolean isRestrictedByAdminUL(int uid) {
        Set<Integer> restrictedUids = (Set) this.mMeteredRestrictedUids.get(UserHandle.getUserId(uid));
        return restrictedUids != null && restrictedUids.contains(Integer.valueOf(uid));
    }

    private static boolean hasRule(int uidRules, int rule) {
        return (uidRules & rule) != 0;
    }

    public long getTimeRefreshElapsedRealtime() {
        if (this.mTimeRefreshRealtime != -1) {
            return SystemClock.elapsedRealtime() - this.mTimeRefreshRealtime;
        }
        return JobStatus.NO_LATEST_RUNTIME;
    }

    private static NetworkState[] defeatNullable(NetworkState[] val) {
        return val != null ? val : new NetworkState[0];
    }

    private static boolean getBooleanDefeatingNullable(PersistableBundle bundle, String key, boolean defaultValue) {
        return bundle != null ? bundle.getBoolean(key, defaultValue) : defaultValue;
    }

    private void sendBehavior(BehaviorId bid) {
        if (this.mHwBehaviorManager == null) {
            this.mHwBehaviorManager = HwFrameworkFactory.getHwBehaviorCollectManager();
        }
        if (this.mHwBehaviorManager != null) {
            try {
                this.mHwBehaviorManager.sendBehavior(Binder.getCallingUid(), Binder.getCallingPid(), bid);
                return;
            } catch (Exception e) {
                Log.e(TAG, "sendBehavior:");
                return;
            }
        }
        Log.w(TAG, "HwBehaviorCollectManager is null");
    }
}
