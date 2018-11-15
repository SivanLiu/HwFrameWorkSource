package com.android.server.connectivity;

import android.app.usage.NetworkStatsManager;
import android.app.usage.NetworkStatsManager.UsageCallback;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkIdentity;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkPolicyManager.Listener;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.net.NetworkTemplate;
import android.net.StringNetworkSpecifier;
import android.net.Uri;
import android.os.BestClock;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;
import android.util.DebugUtils;
import android.util.Range;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.net.NetworkStatsManagerInternal;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MultipathPolicyTracker {
    private static final boolean DBG = false;
    private static final int OPQUOTA_USER_SETTING_DIVIDER = 20;
    private static String TAG = MultipathPolicyTracker.class.getSimpleName();
    private ConnectivityManager mCM;
    private final Clock mClock;
    private final ConfigChangeReceiver mConfigChangeReceiver;
    private final Context mContext;
    private final Dependencies mDeps;
    private final Handler mHandler;
    private NetworkCallback mMobileNetworkCallback;
    private final ConcurrentHashMap<Network, MultipathTracker> mMultipathTrackers;
    private NetworkPolicyManager mNPM;
    private Listener mPolicyListener;
    private final ContentResolver mResolver;
    @VisibleForTesting
    final ContentObserver mSettingsObserver;
    private NetworkStatsManager mStatsManager;

    private final class ConfigChangeReceiver extends BroadcastReceiver {
        private ConfigChangeReceiver() {
        }

        /* synthetic */ ConfigChangeReceiver(MultipathPolicyTracker x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            MultipathPolicyTracker.this.updateAllMultipathBudgets();
        }
    }

    public static class Dependencies {
        public Clock getClock() {
            return new BestClock(ZoneOffset.UTC, new Clock[]{SystemClock.currentNetworkTimeClock(), Clock.systemUTC()});
        }
    }

    class MultipathTracker {
        private long mMultipathBudget;
        private NetworkCapabilities mNetworkCapabilities;
        private final NetworkTemplate mNetworkTemplate;
        private long mQuota;
        private final UsageCallback mUsageCallback;
        final Network network;
        final int subId;
        final String subscriberId;

        /* JADX WARNING: Removed duplicated region for block: B:13:0x0082 A:{Splitter: B:1:0x0011, ExcHandler: java.lang.ClassCastException (r2_8 'e' java.lang.RuntimeException)} */
        /* JADX WARNING: Removed duplicated region for block: B:13:0x0082 A:{Splitter: B:1:0x0011, ExcHandler: java.lang.ClassCastException (r2_8 'e' java.lang.RuntimeException)} */
        /* JADX WARNING: Missing block: B:13:0x0082, code:
            r2 = move-exception;
     */
        /* JADX WARNING: Missing block: B:15:0x009c, code:
            throw new java.lang.IllegalStateException(java.lang.String.format("Can't get subId from mobile network %s (%s): %s", new java.lang.Object[]{r14, r15, r2.getMessage()}));
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public MultipathTracker(final Network network, NetworkCapabilities nc) {
            this.network = network;
            this.mNetworkCapabilities = new NetworkCapabilities(nc);
            try {
                this.subId = Integer.parseInt(((StringNetworkSpecifier) nc.getNetworkSpecifier()).toString());
                TelephonyManager tele = (TelephonyManager) MultipathPolicyTracker.this.mContext.getSystemService(TelephonyManager.class);
                if (tele != null) {
                    tele = tele.createForSubscriptionId(this.subId);
                    if (tele != null) {
                        this.subscriberId = tele.getSubscriberId();
                        this.mNetworkTemplate = new NetworkTemplate(1, this.subscriberId, new String[]{this.subscriberId}, null, -1, -1, 0);
                        this.mUsageCallback = new UsageCallback(MultipathPolicyTracker.this) {
                            public void onThresholdReached(int networkType, String subscriberId) {
                                MultipathTracker.this.mMultipathBudget = 0;
                                MultipathTracker.this.updateMultipathBudget();
                            }
                        };
                        updateMultipathBudget();
                        return;
                    }
                    throw new IllegalStateException(String.format("Can't get TelephonyManager for subId %d", new Object[]{Integer.valueOf(this.subId)}));
                }
                throw new IllegalStateException(String.format("Missing TelephonyManager", new Object[0]));
            } catch (RuntimeException e) {
            }
        }

        public void setNetworkCapabilities(NetworkCapabilities nc) {
            this.mNetworkCapabilities = new NetworkCapabilities(nc);
        }

        private long getDailyNonDefaultDataUsage() {
            ZonedDateTime end = ZonedDateTime.ofInstant(MultipathPolicyTracker.this.mClock.instant(), ZoneId.systemDefault());
            return getNetworkTotalBytes(end.truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
        }

        private long getNetworkTotalBytes(long start, long end) {
            try {
                return ((NetworkStatsManagerInternal) LocalServices.getService(NetworkStatsManagerInternal.class)).getNetworkTotalBytes(this.mNetworkTemplate, start, end);
            } catch (RuntimeException e) {
                String access$400 = MultipathPolicyTracker.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to get data usage: ");
                stringBuilder.append(e);
                Slog.w(access$400, stringBuilder.toString());
                return -1;
            }
        }

        private NetworkIdentity getTemplateMatchingNetworkIdentity(NetworkCapabilities nc) {
            return new NetworkIdentity(0, 0, this.subscriberId, null, nc.hasCapability(18) ^ 1, nc.hasCapability(11) ^ 1, false);
        }

        private long getRemainingDailyBudget(long limitBytes, Range<ZonedDateTime> cycle) {
            long start = ((ZonedDateTime) cycle.getLower()).toInstant().toEpochMilli();
            long end = ((ZonedDateTime) cycle.getUpper()).toInstant().toEpochMilli();
            long totalBytes = getNetworkTotalBytes(start, end);
            long j = 0;
            if (totalBytes != -1) {
                j = Math.max(0, limitBytes - totalBytes);
            }
            return j / Math.max(1, (((end - MultipathPolicyTracker.this.mClock.millis()) - 1) / TimeUnit.DAYS.toMillis(1)) + 1);
        }

        private long getUserPolicyOpportunisticQuotaBytes() {
            long minQuota = JobStatus.NO_LATEST_RUNTIME;
            NetworkIdentity identity = getTemplateMatchingNetworkIdentity(this.mNetworkCapabilities);
            for (NetworkPolicy policy : MultipathPolicyTracker.this.mNPM.getNetworkPolicies()) {
                if (policy.hasCycle() && policy.template.matches(identity)) {
                    long policyBytes;
                    long cycleStart = ((ZonedDateTime) ((Range) policy.cycleIterator().next()).getLower()).toInstant().toEpochMilli();
                    long activeWarning = MultipathPolicyTracker.getActiveWarning(policy, cycleStart);
                    if (activeWarning == -1) {
                        policyBytes = MultipathPolicyTracker.getActiveLimit(policy, cycleStart);
                    } else {
                        policyBytes = activeWarning;
                    }
                    if (!(policyBytes == -1 || policyBytes == -1)) {
                        minQuota = Math.min(minQuota, getRemainingDailyBudget(policyBytes, (Range) policy.cycleIterator().next()));
                    }
                }
            }
            if (minQuota == JobStatus.NO_LATEST_RUNTIME) {
                return -1;
            }
            return minQuota / 20;
        }

        void updateMultipathBudget() {
            long quota = ((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class)).getSubscriptionOpportunisticQuota(this.network, 2);
            if (quota == -1) {
                quota = getUserPolicyOpportunisticQuotaBytes();
            }
            if (quota == -1) {
                quota = MultipathPolicyTracker.this.getDefaultDailyMultipathQuotaBytes();
            }
            if (!haveMultipathBudget() || quota != this.mQuota) {
                this.mQuota = quota;
                long usage = getDailyNonDefaultDataUsage();
                long j = 0;
                if (usage != -1) {
                    j = Math.max(0, quota - usage);
                }
                long budget = j;
                if (budget > NetworkStatsManager.MIN_THRESHOLD_BYTES) {
                    registerUsageCallback(budget);
                } else {
                    maybeUnregisterUsageCallback();
                }
            }
        }

        public int getMultipathPreference() {
            if (haveMultipathBudget()) {
                return 3;
            }
            return 0;
        }

        public long getQuota() {
            return this.mQuota;
        }

        public long getMultipathBudget() {
            return this.mMultipathBudget;
        }

        private boolean haveMultipathBudget() {
            return this.mMultipathBudget > 0;
        }

        private void registerUsageCallback(long budget) {
            maybeUnregisterUsageCallback();
            MultipathPolicyTracker.this.mStatsManager.registerUsageCallback(this.mNetworkTemplate, 0, budget, this.mUsageCallback, MultipathPolicyTracker.this.mHandler);
            this.mMultipathBudget = budget;
        }

        private void maybeUnregisterUsageCallback() {
            if (haveMultipathBudget()) {
                MultipathPolicyTracker.this.mStatsManager.unregisterUsageCallback(this.mUsageCallback);
                this.mMultipathBudget = 0;
            }
        }

        void shutdown() {
            maybeUnregisterUsageCallback();
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            Slog.wtf(MultipathPolicyTracker.TAG, "Should never be reached.");
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (!Global.getUriFor("network_default_daily_multipath_quota_bytes").equals(uri)) {
                String access$400 = MultipathPolicyTracker.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected settings observation: ");
                stringBuilder.append(uri);
                Slog.wtf(access$400, stringBuilder.toString());
            }
            MultipathPolicyTracker.this.updateAllMultipathBudgets();
        }
    }

    public MultipathPolicyTracker(Context ctx, Handler handler) {
        this(ctx, handler, new Dependencies());
    }

    public MultipathPolicyTracker(Context ctx, Handler handler, Dependencies deps) {
        this.mMultipathTrackers = new ConcurrentHashMap();
        this.mContext = ctx;
        this.mHandler = handler;
        this.mClock = deps.getClock();
        this.mDeps = deps;
        this.mResolver = this.mContext.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mConfigChangeReceiver = new ConfigChangeReceiver(this, null);
    }

    public void start() {
        this.mCM = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mNPM = (NetworkPolicyManager) this.mContext.getSystemService(NetworkPolicyManager.class);
        this.mStatsManager = (NetworkStatsManager) this.mContext.getSystemService(NetworkStatsManager.class);
        registerTrackMobileCallback();
        registerNetworkPolicyListener();
        this.mResolver.registerContentObserver(Global.getUriFor("network_default_daily_multipath_quota_bytes"), false, this.mSettingsObserver);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiverAsUser(this.mConfigChangeReceiver, UserHandle.ALL, intentFilter, null, this.mHandler);
    }

    public void shutdown() {
        maybeUnregisterTrackMobileCallback();
        unregisterNetworkPolicyListener();
        for (MultipathTracker t : this.mMultipathTrackers.values()) {
            t.shutdown();
        }
        this.mMultipathTrackers.clear();
        this.mResolver.unregisterContentObserver(this.mSettingsObserver);
        this.mContext.unregisterReceiver(this.mConfigChangeReceiver);
    }

    public Integer getMultipathPreference(Network network) {
        if (network == null) {
            return null;
        }
        MultipathTracker t = (MultipathTracker) this.mMultipathTrackers.get(network);
        if (t != null) {
            return Integer.valueOf(t.getMultipathPreference());
        }
        return null;
    }

    private static long getActiveWarning(NetworkPolicy policy, long cycleStart) {
        if (policy.lastWarningSnooze < cycleStart) {
            return policy.warningBytes;
        }
        return -1;
    }

    private static long getActiveLimit(NetworkPolicy policy, long cycleStart) {
        if (policy.lastLimitSnooze < cycleStart) {
            return policy.limitBytes;
        }
        return -1;
    }

    private long getDefaultDailyMultipathQuotaBytes() {
        String setting = Global.getString(this.mContext.getContentResolver(), "network_default_daily_multipath_quota_bytes");
        if (setting != null) {
            try {
                return Long.parseLong(setting);
            } catch (NumberFormatException e) {
            }
        }
        return (long) this.mContext.getResources().getInteger(17694826);
    }

    private void registerTrackMobileCallback() {
        NetworkRequest request = new Builder().addCapability(12).addTransportType(0).build();
        this.mMobileNetworkCallback = new NetworkCallback() {
            public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                MultipathTracker existing = (MultipathTracker) MultipathPolicyTracker.this.mMultipathTrackers.get(network);
                if (existing != null) {
                    existing.setNetworkCapabilities(nc);
                    existing.updateMultipathBudget();
                    return;
                }
                try {
                    MultipathPolicyTracker.this.mMultipathTrackers.put(network, new MultipathTracker(network, nc));
                } catch (IllegalStateException e) {
                    String access$400 = MultipathPolicyTracker.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't track mobile network ");
                    stringBuilder.append(network);
                    stringBuilder.append(": ");
                    stringBuilder.append(e.getMessage());
                    Slog.e(access$400, stringBuilder.toString());
                }
            }

            public void onLost(Network network) {
                MultipathTracker existing = (MultipathTracker) MultipathPolicyTracker.this.mMultipathTrackers.get(network);
                if (existing != null) {
                    existing.shutdown();
                    MultipathPolicyTracker.this.mMultipathTrackers.remove(network);
                }
            }
        };
        this.mCM.registerNetworkCallback(request, this.mMobileNetworkCallback, this.mHandler);
    }

    private void updateAllMultipathBudgets() {
        for (MultipathTracker t : this.mMultipathTrackers.values()) {
            t.updateMultipathBudget();
        }
    }

    private void maybeUnregisterTrackMobileCallback() {
        if (this.mMobileNetworkCallback != null) {
            this.mCM.unregisterNetworkCallback(this.mMobileNetworkCallback);
        }
        this.mMobileNetworkCallback = null;
    }

    private void registerNetworkPolicyListener() {
        this.mPolicyListener = new Listener() {
            public void onMeteredIfacesChanged(String[] meteredIfaces) {
                MultipathPolicyTracker.this.mHandler.post(new -$$Lambda$MultipathPolicyTracker$2$dvyDLfu9d6g2XoEdL3QMHx7ut6k(this));
            }
        };
        this.mNPM.registerListener(this.mPolicyListener);
    }

    private void unregisterNetworkPolicyListener() {
        this.mNPM.unregisterListener(this.mPolicyListener);
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("MultipathPolicyTracker:");
        pw.increaseIndent();
        for (MultipathTracker t : this.mMultipathTrackers.values()) {
            pw.println(String.format("Network %s: quota %d, budget %d. Preference: %s", new Object[]{t.network, Long.valueOf(t.getQuota()), Long.valueOf(t.getMultipathBudget()), DebugUtils.flagsToString(ConnectivityManager.class, "MULTIPATH_PREFERENCE_", t.getMultipathPreference())}));
        }
        pw.decreaseIndent();
    }
}
