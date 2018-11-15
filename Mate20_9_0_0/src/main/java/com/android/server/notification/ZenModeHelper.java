package com.android.server.notification;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AppOpsManager;
import android.app.AutomaticZenRule;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManagerInternal;
import android.media.VolumePolicy;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.EventInfo;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IHwZenModeFiltering;
import com.android.server.LocalServices;
import com.android.server.notification.ManagedServices.Config;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import huawei.cust.HwCustUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ZenModeHelper {
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final int RULE_INSTANCE_GRACE_PERIOD = 259200000;
    public static final long SUPPRESSED_EFFECT_ALL = 3;
    public static final long SUPPRESSED_EFFECT_CALLS = 2;
    public static final long SUPPRESSED_EFFECT_NOTIFICATIONS = 1;
    static final String TAG = "ZenModeHelper";
    @VisibleForTesting
    protected final AppOpsManager mAppOps;
    @VisibleForTesting
    protected AudioManagerInternal mAudioManager;
    private final ArrayList<Callback> mCallbacks = new ArrayList();
    private final ZenModeConditions mConditions;
    @VisibleForTesting
    protected ZenModeConfig mConfig;
    private final SparseArray<ZenModeConfig> mConfigs = new SparseArray();
    private final Context mContext;
    private HwCustZenModeHelper mCust = ((HwCustZenModeHelper) HwCustUtils.createObj(HwCustZenModeHelper.class, new Object[0]));
    protected ZenModeConfig mDefaultConfig;
    protected String mDefaultRuleEventsName;
    protected String mDefaultRuleEveryNightName;
    private final ZenModeFiltering mFiltering;
    private final H mHandler;
    @VisibleForTesting
    protected boolean mIsBootComplete;
    private final Metrics mMetrics = new Metrics();
    @VisibleForTesting
    protected final NotificationManager mNotificationManager;
    protected PackageManager mPm;
    protected final RingerModeDelegate mRingerModeDelegate = new RingerModeDelegate();
    private final Config mServiceConfig;
    private final SettingsObserver mSettingsObserver;
    private long mSuppressedEffects;
    private int mUser = 0;
    @VisibleForTesting
    protected int mZenMode;

    public static class Callback {
        void onConfigChanged() {
        }

        void onZenModeChanged() {
        }

        void onPolicyChanged() {
        }
    }

    private final class H extends Handler {
        private static final long METRICS_PERIOD_MS = 21600000;
        private static final int MSG_APPLY_CONFIG = 4;
        private static final int MSG_DISPATCH = 1;
        private static final int MSG_METRICS = 2;

        private final class ConfigMessageData {
            public final ZenModeConfig config;
            public final String reason;
            public final boolean setRingerMode;

            ConfigMessageData(ZenModeConfig config, String reason, boolean setRingerMode) {
                this.config = config;
                this.reason = reason;
                this.setRingerMode = setRingerMode;
            }
        }

        private H(Looper looper) {
            super(looper);
        }

        private void postDispatchOnZenModeChanged() {
            removeMessages(1);
            sendEmptyMessage(1);
        }

        private void postMetricsTimer() {
            removeMessages(2);
            sendEmptyMessageDelayed(2, METRICS_PERIOD_MS);
        }

        private void postApplyConfig(ZenModeConfig config, String reason, boolean setRingerMode) {
            sendMessage(obtainMessage(4, new ConfigMessageData(config, reason, setRingerMode)));
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 4) {
                switch (i) {
                    case 1:
                        ZenModeHelper.this.dispatchOnZenModeChanged();
                        return;
                    case 2:
                        ZenModeHelper.this.mMetrics.emit();
                        return;
                    default:
                        return;
                }
            }
            ConfigMessageData applyConfigData = msg.obj;
            ZenModeHelper.this.applyConfig(applyConfigData.config, applyConfigData.reason, applyConfigData.setRingerMode);
        }
    }

    @VisibleForTesting
    protected final class RingerModeDelegate implements android.media.AudioManagerInternal.RingerModeDelegate {
        protected RingerModeDelegate() {
        }

        public String toString() {
            return ZenModeHelper.TAG;
        }

        public int onSetRingerModeInternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeExternal, VolumePolicy policy) {
            boolean isChange = ringerModeOld != ringerModeNew;
            int ringerModeExternalOut = ringerModeNew;
            if (ZenModeHelper.this.mZenMode == 0 || (ZenModeHelper.this.mZenMode == 1 && !ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(ZenModeHelper.this.mConfig))) {
                ZenModeHelper.this.setPreviousRingerModeSetting(Integer.valueOf(ringerModeNew));
            }
            int newZen = -1;
            switch (ringerModeNew) {
                case 0:
                    if (isChange && policy.doNotDisturbWhenSilent) {
                        if (ZenModeHelper.this.mZenMode == 0) {
                            newZen = 1;
                        }
                        ZenModeHelper.this.setPreviousRingerModeSetting(Integer.valueOf(ringerModeOld));
                        break;
                    }
                case 1:
                case 2:
                    if (!isChange || ringerModeOld != 0 || (ZenModeHelper.this.mZenMode != 2 && ZenModeHelper.this.mZenMode != 3 && (ZenModeHelper.this.mZenMode != 1 || !ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(ZenModeHelper.this.mConfig)))) {
                        int i = ZenModeHelper.this.mZenMode;
                        break;
                    }
                    newZen = 0;
                    break;
                    break;
            }
            if (!(!isChange && newZen == -1 && ringerModeExternal == ringerModeExternalOut)) {
                ZenLog.traceSetRingerModeInternal(ringerModeOld, ringerModeNew, caller, ringerModeExternal, ringerModeExternalOut);
            }
            return ringerModeExternalOut;
        }

        public int onSetRingerModeExternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeInternal, VolumePolicy policy) {
            int ringerModeInternalOut = ringerModeNew;
            boolean isChange = ringerModeOld != ringerModeNew;
            if (ringerModeInternal == 1) {
                boolean isVibrate = true;
            }
            switch (ringerModeNew) {
                case 0:
                    if (isChange) {
                        if (ZenModeHelper.this.mZenMode == 0) {
                            break;
                        }
                    }
                    ringerModeInternalOut = ringerModeInternal;
                    break;
                    break;
                case 1:
                case 2:
                    if (ZenModeHelper.this.mZenMode != 0) {
                        break;
                    }
                    break;
            }
            ZenLog.traceSetRingerModeExternal(ringerModeOld, ringerModeNew, caller, ringerModeInternal, ringerModeInternalOut);
            return ringerModeInternalOut;
        }

        public boolean canVolumeDownEnterSilent() {
            return ZenModeHelper.this.mZenMode == 0;
        }

        public int getRingerModeAffectedStreams(int streams) {
            streams |= 38;
            if (ZenModeHelper.this.mZenMode == 2) {
                streams |= 8;
            } else {
                streams &= -9;
            }
            if (ZenModeHelper.this.mZenMode == 1 && ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(ZenModeHelper.this.mConfig)) {
                return streams & -3;
            }
            return streams | 2;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE = Global.getUriFor("zen_mode");

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ZenModeHelper.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE, false, this);
            update(null);
        }

        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (this.ZEN_MODE.equals(uri) && ZenModeHelper.this.mZenMode != ZenModeHelper.this.getZenModeSetting()) {
                if (ZenModeHelper.DEBUG) {
                    Log.d(ZenModeHelper.TAG, "Fixing zen mode setting");
                }
                ZenModeHelper.this.setZenModeSetting(ZenModeHelper.this.mZenMode);
            }
        }
    }

    private final class Metrics extends Callback {
        private static final String COUNTER_PREFIX = "dnd_mode_";
        private static final long MINIMUM_LOG_PERIOD_MS = 60000;
        private long mBeginningMs;
        private int mPreviousZenMode;

        private Metrics() {
            this.mPreviousZenMode = -1;
            this.mBeginningMs = 0;
        }

        void onZenModeChanged() {
            emit();
        }

        private void emit() {
            ZenModeHelper.this.mHandler.postMetricsTimer();
            long now = SystemClock.elapsedRealtime();
            long since = now - this.mBeginningMs;
            if (this.mPreviousZenMode != ZenModeHelper.this.mZenMode || since > 60000) {
                if (this.mPreviousZenMode != -1) {
                    Context access$600 = ZenModeHelper.this.mContext;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(COUNTER_PREFIX);
                    stringBuilder.append(this.mPreviousZenMode);
                    MetricsLogger.count(access$600, stringBuilder.toString(), (int) since);
                }
                this.mPreviousZenMode = ZenModeHelper.this.mZenMode;
                this.mBeginningMs = now;
            }
        }
    }

    public ZenModeHelper(Context context, Looper looper, ConditionProviders conditionProviders) {
        this.mContext = context;
        this.mHandler = new H(looper);
        addCallback(this.mMetrics);
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mNotificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mDefaultConfig = new ZenModeConfig();
        setDefaultZenRules(this.mContext);
        this.mConfig = this.mDefaultConfig;
        this.mConfigs.put(0, this.mConfig);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
        this.mFiltering = new ZenModeFiltering(this.mContext);
        this.mConditions = new ZenModeConditions(this, conditionProviders);
        this.mServiceConfig = conditionProviders.getConfig();
    }

    public Looper getLooper() {
        return this.mHandler.getLooper();
    }

    public String toString() {
        return TAG;
    }

    public boolean matchesCallFilter(UserHandle userHandle, Bundle extras, ValidateNotificationPeople validator, int contactsTimeoutMs, float timeoutAffinity) {
        synchronized (this.mConfig) {
            IHwZenModeFiltering filter = HwServiceFactory.getHwZenModeFiltering();
            boolean matchesCallFilter;
            if (filter != null) {
                matchesCallFilter = filter.matchesCallFilter(this.mContext, this.mZenMode, this.mConfig, userHandle, extras, validator, contactsTimeoutMs, timeoutAffinity);
                return matchesCallFilter;
            }
            matchesCallFilter = ZenModeFiltering.matchesCallFilter(this.mContext, this.mZenMode, this.mConfig, userHandle, extras, validator, contactsTimeoutMs, timeoutAffinity);
            return matchesCallFilter;
        }
    }

    public boolean isCall(NotificationRecord record) {
        return this.mFiltering.isCall(record);
    }

    public void recordCaller(NotificationRecord record) {
        this.mFiltering.recordCall(record);
    }

    public boolean shouldIntercept(NotificationRecord record) {
        boolean shouldIntercept;
        synchronized (this.mConfig) {
            shouldIntercept = this.mFiltering.shouldIntercept(this.mZenMode, this.mConfig, record);
        }
        return shouldIntercept;
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public void initZenMode() {
        if (DEBUG) {
            Log.d(TAG, "initZenMode");
        }
        evaluateZenMode("init", true);
    }

    public void onSystemReady() {
        if (DEBUG) {
            Log.d(TAG, "onSystemReady");
        }
        this.mAudioManager = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        if (this.mAudioManager != null) {
            this.mAudioManager.setRingerModeDelegate(this.mRingerModeDelegate);
        }
        this.mPm = this.mContext.getPackageManager();
        this.mHandler.postMetricsTimer();
        cleanUpZenRules();
        evaluateZenMode("onSystemReady", true);
        this.mIsBootComplete = true;
        showZenUpgradeNotification(this.mZenMode);
    }

    public void onUserSwitched(int user) {
        loadConfigForUser(user, "onUserSwitched");
    }

    public void onUserRemoved(int user) {
        if (user >= 0) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onUserRemoved u=");
                stringBuilder.append(user);
                Log.d(str, stringBuilder.toString());
            }
            this.mConfigs.remove(user);
        }
    }

    public void onUserUnlocked(int user) {
        loadConfigForUser(user, "onUserUnlocked");
    }

    private void loadConfigForUser(int user, String reason) {
        if (this.mUser != user && user >= 0) {
            this.mUser = user;
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(reason);
                stringBuilder.append(" u=");
                stringBuilder.append(user);
                Log.d(str, stringBuilder.toString());
            }
            ZenModeConfig config = (ZenModeConfig) this.mConfigs.get(user);
            if (config == null) {
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(reason);
                    stringBuilder2.append(" generating default config for user ");
                    stringBuilder2.append(user);
                    Log.d(str2, stringBuilder2.toString());
                }
                config = this.mDefaultConfig.copy();
                config.user = user;
            }
            if (user != 0 && "onUserSwitched".equals(reason)) {
                ZenModeConfig config_owner = (ZenModeConfig) this.mConfigs.get(0);
                config = config_owner.copy();
                config.user = user;
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("current user config.user:");
                stringBuilder3.append(config.user);
                stringBuilder3.append(",config_owner:");
                stringBuilder3.append(config_owner);
                Log.d(str3, stringBuilder3.toString());
            }
            synchronized (this.mConfig) {
                setConfigLocked(config, reason);
            }
            cleanUpZenRules();
        }
    }

    public int getZenModeListenerInterruptionFilter() {
        return NotificationManager.zenModeToInterruptionFilter(this.mZenMode);
    }

    public void requestFromListener(ComponentName name, int filter) {
        int newZen = NotificationManager.zenModeFromInterruptionFilter(filter, -1);
        if (newZen != -1) {
            String packageName = name != null ? name.getPackageName() : null;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("listener:");
            stringBuilder.append(name != null ? name.flattenToShortString() : null);
            setManualZenMode(newZen, null, packageName, stringBuilder.toString());
        }
    }

    public void setSuppressedEffects(long suppressedEffects) {
        if (this.mSuppressedEffects != suppressedEffects) {
            this.mSuppressedEffects = suppressedEffects;
            applyRestrictions();
        }
    }

    public long getSuppressedEffects() {
        return this.mSuppressedEffects;
    }

    public int getZenMode() {
        return this.mZenMode;
    }

    public List<ZenRule> getZenRules() {
        List<ZenRule> rules = new ArrayList();
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return rules;
            }
            for (ZenRule rule : this.mConfig.automaticRules.values()) {
                if (canManageAutomaticZenRule(rule)) {
                    rules.add(rule);
                }
            }
            return rules;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0015, code:
            if (r1 != null) goto L_0x0018;
     */
    /* JADX WARNING: Missing block: B:10:0x0017, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:12:0x001c, code:
            if (canManageAutomaticZenRule(r1) == false) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:14:0x0022, code:
            return createAutomaticZenRule(r1);
     */
    /* JADX WARNING: Missing block: B:15:0x0023, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AutomaticZenRule getAutomaticZenRule(String id) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return null;
            }
            ZenRule rule = (ZenRule) this.mConfig.automaticRules.get(id);
        }
    }

    public String addAutomaticZenRule(AutomaticZenRule automaticZenRule, String reason) {
        String str;
        if (!isSystemRule(automaticZenRule)) {
            ServiceInfo owner = getServiceInfo(automaticZenRule.getOwner());
            if (owner != null) {
                int ruleInstanceLimit = -1;
                if (owner.metaData != null) {
                    ruleInstanceLimit = owner.metaData.getInt("android.service.zen.automatic.ruleInstanceLimit", -1);
                }
                if (ruleInstanceLimit > 0 && ruleInstanceLimit < getCurrentInstanceCount(automaticZenRule.getOwner()) + 1) {
                    throw new IllegalArgumentException("Rule instance limit exceeded");
                }
            }
            throw new IllegalArgumentException("Owner is not a condition provider service");
        }
        synchronized (this.mConfig) {
            if (this.mConfig != null) {
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("addAutomaticZenRule rule= ");
                    stringBuilder.append(automaticZenRule);
                    stringBuilder.append(" reason=");
                    stringBuilder.append(reason);
                    Log.d(str2, stringBuilder.toString());
                }
                ZenModeConfig newConfig = this.mConfig.copy();
                ZenRule rule = new ZenRule();
                populateZenRule(automaticZenRule, rule, true);
                newConfig.automaticRules.put(rule.id, rule);
                if (setConfigLocked(newConfig, reason, true)) {
                    str = rule.id;
                } else {
                    throw new AndroidRuntimeException("Could not create rule");
                }
            }
            throw new AndroidRuntimeException("Could not create rule");
        }
        return str;
    }

    public boolean updateAutomaticZenRule(String ruleId, AutomaticZenRule automaticZenRule, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateAutomaticZenRule zenRule=");
                stringBuilder.append(automaticZenRule);
                stringBuilder.append(" reason=");
                stringBuilder.append(reason);
                Log.d(str, stringBuilder.toString());
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            if (ruleId != null) {
                ZenRule rule = (ZenRule) newConfig.automaticRules.get(ruleId);
                if (rule == null || !canManageAutomaticZenRule(rule)) {
                    throw new SecurityException("Cannot update rules not owned by your condition provider");
                }
                populateZenRule(automaticZenRule, rule, false);
                newConfig.automaticRules.put(ruleId, rule);
                boolean configLocked = setConfigLocked(newConfig, reason, true);
                return configLocked;
            }
            throw new IllegalArgumentException("Rule doesn't exist");
        }
    }

    public boolean removeAutomaticZenRule(String id, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            ZenRule rule = (ZenRule) newConfig.automaticRules.get(id);
            if (rule == null) {
                return false;
            } else if (canManageAutomaticZenRule(rule)) {
                newConfig.automaticRules.remove(id);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeZenRule zenRule=");
                    stringBuilder.append(id);
                    stringBuilder.append(" reason=");
                    stringBuilder.append(reason);
                    Log.d(str, stringBuilder.toString());
                }
                boolean configLocked = setConfigLocked(newConfig, reason, true);
                return configLocked;
            } else {
                throw new SecurityException("Cannot delete rules not owned by your condition provider");
            }
        }
    }

    public boolean removeAutomaticZenRules(String packageName, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            for (int i = newConfig.automaticRules.size() - 1; i >= 0; i--) {
                ZenRule rule = (ZenRule) newConfig.automaticRules.get(newConfig.automaticRules.keyAt(i));
                if (rule.component.getPackageName().equals(packageName) && canManageAutomaticZenRule(rule)) {
                    newConfig.automaticRules.removeAt(i);
                }
            }
            boolean configLocked = setConfigLocked(newConfig, reason, true);
            return configLocked;
        }
    }

    public int getCurrentInstanceCount(ComponentName owner) {
        int count = 0;
        synchronized (this.mConfig) {
            for (ZenRule rule : this.mConfig.automaticRules.values()) {
                if (rule.component != null && rule.component.equals(owner)) {
                    count++;
                }
            }
        }
        return count;
    }

    /* JADX WARNING: Missing block: B:16:0x003b, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean canManageAutomaticZenRule(ZenRule rule) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0 || callingUid == 1000 || this.mContext.checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
            return true;
        }
        String[] packages = this.mPm.getPackagesForUid(Binder.getCallingUid());
        if (packages != null) {
            for (String equals : packages) {
                if (equals.equals(rule.component.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setDefaultZenRules(Context context) {
        this.mDefaultConfig = readDefaultConfig(context.getResources());
        appendDefaultRules(this.mDefaultConfig);
    }

    private void appendDefaultRules(ZenModeConfig config) {
        getDefaultRuleNames();
        appendDefaultEveryNightRule(config);
        appendDefaultEventRules(config);
    }

    private boolean ruleValuesEqual(AutomaticZenRule rule, ZenRule defaultRule) {
        boolean z = false;
        if (rule == null || defaultRule == null) {
            return false;
        }
        if (rule.getInterruptionFilter() == NotificationManager.zenModeToInterruptionFilter(defaultRule.zenMode) && rule.getConditionId().equals(defaultRule.conditionId) && rule.getOwner().equals(defaultRule.component)) {
            z = true;
        }
        return z;
    }

    protected void updateDefaultZenRules() {
        ZenModeConfig configDefaultRules = new ZenModeConfig();
        appendDefaultRules(configDefaultRules);
        for (String ruleId : ZenModeConfig.DEFAULT_RULE_IDS) {
            AutomaticZenRule currRule = getAutomaticZenRule(ruleId);
            ZenRule defaultRule = (ZenRule) configDefaultRules.automaticRules.get(ruleId);
            if (ruleValuesEqual(currRule, defaultRule) && !defaultRule.name.equals(currRule.getName()) && canManageAutomaticZenRule(defaultRule)) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Locale change - updating default zen rule name from ");
                    stringBuilder.append(currRule.getName());
                    stringBuilder.append(" to ");
                    stringBuilder.append(defaultRule.name);
                    Slog.d(str, stringBuilder.toString());
                }
                AutomaticZenRule defaultAutoRule = createAutomaticZenRule(defaultRule);
                defaultAutoRule.setEnabled(currRule.isEnabled());
                updateAutomaticZenRule(ruleId, defaultAutoRule, "locale changed");
            }
        }
    }

    private boolean isSystemRule(AutomaticZenRule rule) {
        return PackageManagerService.PLATFORM_PACKAGE_NAME.equals(rule.getOwner().getPackageName());
    }

    private ServiceInfo getServiceInfo(ComponentName owner) {
        Intent queryIntent = new Intent();
        queryIntent.setComponent(owner);
        List<ResolveInfo> installedServices = this.mPm.queryIntentServicesAsUser(queryIntent, 132, UserHandle.getCallingUserId());
        if (installedServices != null) {
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ServiceInfo info = ((ResolveInfo) installedServices.get(i)).serviceInfo;
                if (this.mServiceConfig.bindPermission.equals(info.permission)) {
                    return info;
                }
            }
        }
        return null;
    }

    private void populateZenRule(AutomaticZenRule automaticZenRule, ZenRule rule, boolean isNew) {
        if (isNew) {
            rule.id = ZenModeConfig.newRuleId();
            rule.creationTime = System.currentTimeMillis();
            rule.component = automaticZenRule.getOwner();
        }
        if (rule.enabled != automaticZenRule.isEnabled()) {
            rule.snoozing = false;
        }
        rule.name = automaticZenRule.getName();
        rule.condition = null;
        rule.conditionId = automaticZenRule.getConditionId();
        rule.enabled = automaticZenRule.isEnabled();
        rule.zenMode = NotificationManager.zenModeFromInterruptionFilter(automaticZenRule.getInterruptionFilter(), 0);
    }

    protected AutomaticZenRule createAutomaticZenRule(ZenRule rule) {
        return new AutomaticZenRule(rule.name, rule.component, rule.conditionId, NotificationManager.zenModeToInterruptionFilter(rule.zenMode), rule.enabled, rule.creationTime);
    }

    public void setManualZenMode(int zenMode, Uri conditionId, String caller, String reason) {
        setManualZenMode(zenMode, conditionId, reason, caller, true);
        Global.putInt(this.mContext.getContentResolver(), "show_zen_settings_suggestion", 0);
    }

    /* JADX WARNING: Missing block: B:29:0x0096, code:
            r0 = r1;
     */
    /* JADX WARNING: Missing block: B:30:0x0097, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setManualZenMode(int zenMode, Uri conditionId, String reason, String caller, boolean setRingerMode) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
            } else if (Global.isValidZenMode(zenMode)) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setManualZenMode ");
                    stringBuilder.append(Global.zenModeToString(zenMode));
                    stringBuilder.append(" conditionId=");
                    stringBuilder.append(conditionId);
                    stringBuilder.append(" reason=");
                    stringBuilder.append(reason);
                    stringBuilder.append(" setRingerMode=");
                    stringBuilder.append(setRingerMode);
                    Log.d(str, stringBuilder.toString());
                }
                ZenModeConfig newConfig = this.mConfig.copy();
                if (zenMode == 0) {
                    newConfig.manualRule = null;
                    for (ZenRule automaticRule : newConfig.automaticRules.values()) {
                        if (automaticRule.isAutomaticActive()) {
                            automaticRule.snoozing = true;
                        }
                    }
                } else {
                    ZenRule newRule = new ZenRule();
                    newRule.enabled = true;
                    newRule.zenMode = zenMode;
                    newRule.conditionId = conditionId;
                    newRule.enabler = caller;
                    newConfig.manualRule = newRule;
                }
                if ("restore_default".equals(reason)) {
                    newConfig = this.mDefaultConfig;
                    setConfigLocked(newConfig, reason, true);
                } else {
                    setConfigLocked(newConfig, reason, setRingerMode);
                }
            }
        }
    }

    void dump(ProtoOutputStream proto) {
        proto.write(1159641169921L, this.mZenMode);
        synchronized (this.mConfig) {
            if (this.mConfig.manualRule != null) {
                this.mConfig.manualRule.writeToProto(proto, 2246267895810L);
            }
            for (ZenRule rule : this.mConfig.automaticRules.values()) {
                if (rule.enabled && rule.condition.state == 1 && !rule.snoozing) {
                    rule.writeToProto(proto, 2246267895810L);
                }
            }
            this.mConfig.toNotificationPolicy().writeToProto(proto, 1146756268037L);
            proto.write(1120986464259L, this.mSuppressedEffects);
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mZenMode=");
        pw.println(Global.zenModeToString(this.mZenMode));
        int N = this.mConfigs.size();
        for (int i = 0; i < N; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mConfigs[u=");
            stringBuilder.append(this.mConfigs.keyAt(i));
            stringBuilder.append("]");
            dump(pw, prefix, stringBuilder.toString(), (ZenModeConfig) this.mConfigs.valueAt(i));
        }
        pw.print(prefix);
        pw.print("mUser=");
        pw.println(this.mUser);
        synchronized (this.mConfig) {
            dump(pw, prefix, "mConfig", this.mConfig);
        }
        pw.print(prefix);
        pw.print("mSuppressedEffects=");
        pw.println(this.mSuppressedEffects);
        this.mFiltering.dump(pw, prefix);
        this.mConditions.dump(pw, prefix);
    }

    private static void dump(PrintWriter pw, String prefix, String var, ZenModeConfig config) {
        pw.print(prefix);
        pw.print(var);
        pw.print('=');
        if (config == null) {
            pw.println(config);
            return;
        }
        r1 = new Object[10];
        int i = 0;
        r1[0] = Boolean.valueOf(config.allowAlarms);
        r1[1] = Boolean.valueOf(config.allowMedia);
        r1[2] = Boolean.valueOf(config.allowSystem);
        r1[3] = Boolean.valueOf(config.allowCalls);
        r1[4] = ZenModeConfig.sourceToString(config.allowCallsFrom);
        r1[5] = Boolean.valueOf(config.allowRepeatCallers);
        r1[6] = Boolean.valueOf(config.allowMessages);
        r1[7] = ZenModeConfig.sourceToString(config.allowMessagesFrom);
        r1[8] = Boolean.valueOf(config.allowEvents);
        r1[9] = Boolean.valueOf(config.allowReminders);
        pw.printf("allow(alarms=%b,media=%b,system=%b,calls=%b,callsFrom=%s,repeatCallers=%b,messages=%b,messagesFrom=%s,events=%b,reminders=%b)\n", r1);
        pw.printf(" disallow(visualEffects=%s)\n", new Object[]{Integer.valueOf(config.suppressedVisualEffects)});
        pw.print(prefix);
        pw.print("  manualRule=");
        pw.println(config.manualRule);
        if (!config.automaticRules.isEmpty()) {
            int N = config.automaticRules.size();
            while (true) {
                int i2 = i;
                if (i2 < N) {
                    pw.print(prefix);
                    pw.print(i2 == 0 ? "  automaticRules=" : "                 ");
                    pw.println(config.automaticRules.valueAt(i2));
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    public void readXml(XmlPullParser parser, boolean forRestore) throws XmlPullParserException, IOException {
        ZenModeConfig config = ZenModeConfig.readXml(parser);
        String reason = "readXml";
        if (config != null) {
            if (forRestore) {
                if (config.user == 0) {
                    config.manualRule = null;
                } else {
                    return;
                }
            }
            boolean resetToDefaultRules = true;
            long time = System.currentTimeMillis();
            if (config.automaticRules != null && config.automaticRules.size() > 0) {
                for (ZenRule automaticRule : config.automaticRules.values()) {
                    if (forRestore) {
                        automaticRule.snoozing = false;
                        automaticRule.condition = null;
                        automaticRule.creationTime = time;
                    }
                    resetToDefaultRules &= automaticRule.enabled ^ 1;
                }
            }
            if (config.version < 8) {
                int zenCallWhiteListEnable = Secure.getInt(this.mContext.getContentResolver(), "zen_call_white_list_enabled", 0);
                int zenMsgWhiteListEnable = Secure.getInt(this.mContext.getContentResolver(), "zen_message_white_list_enabled", 0);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("readXml: zenCallWhiteListEnable=");
                stringBuilder.append(zenCallWhiteListEnable);
                stringBuilder.append(";zenMsgWhiteListEnable=");
                stringBuilder.append(zenMsgWhiteListEnable);
                Log.i(str, stringBuilder.toString());
                if (1 == zenCallWhiteListEnable) {
                    config.allowCalls = true;
                    config.allowCallsFrom = 0;
                }
                if (1 == zenMsgWhiteListEnable) {
                    config.allowMessages = true;
                    config.allowMessagesFrom = 0;
                }
            }
            if (config.version < 8 || forRestore) {
                Global.putInt(this.mContext.getContentResolver(), "show_zen_upgrade_notification", 1);
                if (resetToDefaultRules) {
                    config.automaticRules = new ArrayMap();
                    appendDefaultRules(config);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(reason);
                    stringBuilder2.append(", reset to default rules");
                    reason = stringBuilder2.toString();
                }
            } else {
                Global.putInt(this.mContext.getContentResolver(), "zen_settings_updated", 1);
            }
            String reason2 = reason;
            if (DEBUG) {
                Log.d(TAG, reason2);
            }
            synchronized (this.mConfig) {
                setConfigLocked(config, reason2);
            }
            reason = reason2;
        }
    }

    public void writeXml(XmlSerializer out, boolean forBackup, Integer version) throws IOException {
        int N = this.mConfigs.size();
        int i = 0;
        while (i < N) {
            if (!forBackup || this.mConfigs.keyAt(i) == 0) {
                ((ZenModeConfig) this.mConfigs.valueAt(i)).writeXml(out, version);
            }
            i++;
        }
    }

    public Policy getNotificationPolicy() {
        return getNotificationPolicy(this.mConfig);
    }

    private static Policy getNotificationPolicy(ZenModeConfig config) {
        return config == null ? null : config.toNotificationPolicy();
    }

    public void setNotificationPolicy(Policy policy) {
        if (policy != null && this.mConfig != null) {
            synchronized (this.mConfig) {
                ZenModeConfig newConfig = this.mConfig.copy();
                newConfig.applyNotificationPolicy(policy);
                setConfigLocked(newConfig, "setNotificationPolicy");
            }
        }
    }

    private void cleanUpZenRules() {
        long currentTime = System.currentTimeMillis();
        synchronized (this.mConfig) {
            ZenModeConfig newConfig = this.mConfig.copy();
            if (newConfig.automaticRules != null) {
                for (int i = newConfig.automaticRules.size() - 1; i >= 0; i--) {
                    ZenRule rule = (ZenRule) newConfig.automaticRules.get(newConfig.automaticRules.keyAt(i));
                    if (259200000 < currentTime - rule.creationTime) {
                        try {
                            this.mPm.getPackageInfo(rule.component.getPackageName(), DumpState.DUMP_CHANGES);
                        } catch (NameNotFoundException e) {
                            newConfig.automaticRules.removeAt(i);
                        }
                    }
                }
            }
            setConfigLocked(newConfig, "cleanUpZenRules");
        }
    }

    public ZenModeConfig getConfig() {
        ZenModeConfig copy;
        synchronized (this.mConfig) {
            copy = this.mConfig.copy();
        }
        return copy;
    }

    public boolean setConfigLocked(ZenModeConfig config, String reason) {
        if ("restore_default".equals(reason)) {
            config = this.mDefaultConfig;
        }
        return setConfigLocked(config, reason, true);
    }

    public void setConfig(ZenModeConfig config, String reason) {
        synchronized (this.mConfig) {
            setConfigLocked(config, reason);
        }
    }

    private boolean isVDriverMode() {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        if (activityManager == null) {
            return false;
        }
        List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        boolean isVDriveModeFlay = false;
        String packageName = "com.huawei.vdrive";
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (packageName.equals(appProcess.processName)) {
                isVDriveModeFlay = true;
                Log.d(TAG, "HwVDrive is running .");
                break;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isVDriverMode return value isVDriveModeFlay:");
        stringBuilder.append(isVDriveModeFlay);
        Log.d(str, stringBuilder.toString());
        return isVDriveModeFlay;
    }

    private boolean setConfigLocked(ZenModeConfig config, String reason, boolean setRingerMode) {
        StringBuilder stringBuilder;
        long identity = Binder.clearCallingIdentity();
        if (config != null) {
            try {
                if (config.isValid()) {
                    String str;
                    if (config.user != this.mUser) {
                        this.mConfigs.put(config.user, config);
                        if (DEBUG) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("setConfigLocked: store config for user ");
                            stringBuilder.append(config.user);
                            Log.d(str, stringBuilder.toString());
                        }
                        Binder.restoreCallingIdentity(identity);
                        return true;
                    }
                    this.mConditions.evaluateConfig(config, false);
                    this.mConfigs.put(config.user, config);
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("setConfigLocked reason=");
                        stringBuilder.append(reason);
                        Log.d(str, stringBuilder.toString(), new Throwable());
                    }
                    ZenLog.traceConfig(reason, this.mConfig, config);
                    boolean policyChanged = Objects.equals(getNotificationPolicy(this.mConfig), getNotificationPolicy(config)) ^ true;
                    if (!config.equals(this.mConfig)) {
                        dispatchOnConfigChanged();
                    }
                    if (policyChanged) {
                        dispatchOnPolicyChanged();
                    }
                    this.mConfig = config;
                    String val = Integer.toString(config.hashCode());
                    boolean isVDriverMode = "conditionChanged".equals(reason) && isVDriverMode();
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ZenModeHelper isVDriverMode:");
                    stringBuilder2.append(isVDriverMode);
                    stringBuilder2.append(",timecondition:");
                    stringBuilder2.append("conditionChanged".equals(reason));
                    Log.e(str2, stringBuilder2.toString());
                    if (!isVDriverMode) {
                        Global.putString(this.mContext.getContentResolver(), "zen_mode_config_etag", val);
                        if (!evaluateZenMode(reason, setRingerMode)) {
                            applyRestrictions();
                        }
                    }
                    this.mConditions.evaluateConfig(config, true);
                    this.mHandler.postApplyConfig(config, reason, setRingerMode);
                    Binder.restoreCallingIdentity(identity);
                    return true;
                }
            } catch (SecurityException e) {
                Log.wtf(TAG, "Invalid rule in config", e);
                Binder.restoreCallingIdentity(identity);
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }
        SecurityException e2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid config in setConfigLocked; ");
        stringBuilder.append(config);
        Log.w(e2, stringBuilder.toString());
        Binder.restoreCallingIdentity(identity);
        return false;
    }

    private void applyConfig(ZenModeConfig config, String reason, boolean setRingerMode) {
        Global.putString(this.mContext.getContentResolver(), "zen_mode_config_etag", Integer.toString(config.hashCode()));
        if (!evaluateZenMode(reason, setRingerMode)) {
            applyRestrictions();
        }
        this.mConditions.evaluateConfig(config, true);
    }

    private int getZenModeSetting() {
        return Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
    }

    @VisibleForTesting
    protected void setZenModeSetting(int zen) {
        Global.putInt(this.mContext.getContentResolver(), "zen_mode", zen);
        showZenUpgradeNotification(zen);
        SystemProperties.set("persist.sys.zen_mode", Integer.toString(zen));
    }

    private int getPreviousRingerModeSetting() {
        return Global.getInt(this.mContext.getContentResolver(), "zen_mode_ringer_level", 2);
    }

    private void setPreviousRingerModeSetting(Integer previousRingerLevel) {
        Global.putString(this.mContext.getContentResolver(), "zen_mode_ringer_level", previousRingerLevel == null ? null : Integer.toString(previousRingerLevel.intValue()));
    }

    @VisibleForTesting
    protected boolean evaluateZenMode(String reason, boolean setRingerMode) {
        if (DEBUG) {
            Log.d(TAG, "evaluateZenMode");
        }
        int zenBefore = this.mZenMode;
        int zen = computeZenMode();
        ZenLog.traceSetZenMode(zen, reason);
        this.mZenMode = zen;
        if ((this.mZenMode == 2 || this.mZenMode == 1) && "setInterruptionFilter".equals(reason)) {
            Global.putInt(this.mContext.getContentResolver(), "total_silence_mode", 1);
        } else {
            Global.putInt(this.mContext.getContentResolver(), "total_silence_mode", 0);
        }
        setZenModeSetting(this.mZenMode);
        updateRingerModeAffectedStreams();
        if (setRingerMode && zen != zenBefore) {
            applyZenToRingerMode();
        }
        applyRestrictions();
        if (zen != zenBefore) {
            this.mHandler.postDispatchOnZenModeChanged();
        }
        return true;
    }

    private void updateRingerModeAffectedStreams() {
        if (this.mAudioManager != null) {
            this.mAudioManager.updateRingerModeAffectedStreamsInternal();
        }
    }

    private int computeZenMode() {
        if (this.mConfig == null) {
            return 0;
        }
        synchronized (this.mConfig) {
            int i;
            if (this.mConfig.manualRule != null) {
                i = this.mConfig.manualRule.zenMode;
                return i;
            }
            i = 0;
            for (ZenRule automaticRule : this.mConfig.automaticRules.values()) {
                if (automaticRule.isAutomaticActive() && zenSeverity(automaticRule.zenMode) > zenSeverity(i)) {
                    if (Global.getInt(this.mContext.getContentResolver(), "zen_settings_suggestion_viewed", 1) == 0) {
                        Global.putInt(this.mContext.getContentResolver(), "show_zen_settings_suggestion", 1);
                    }
                    i = automaticRule.zenMode;
                }
            }
            return i;
        }
    }

    private void getDefaultRuleNames() {
        this.mDefaultRuleEveryNightName = this.mContext.getResources().getString(17041434);
        this.mDefaultRuleEventsName = this.mContext.getResources().getString(17041433);
    }

    @VisibleForTesting
    protected void applyRestrictions() {
        boolean zenSilence;
        boolean zenPriorityOnly = this.mZenMode == 1;
        boolean zenSilence2 = this.mZenMode == 2;
        boolean zenAlarmsOnly = this.mZenMode == 3;
        boolean muteNotifications = (this.mSuppressedEffects & 1) != 0 || this.mZenMode == 3 || this.mZenMode == 2;
        boolean muteCalls = zenAlarmsOnly || !((!zenPriorityOnly || this.mConfig.allowCalls || this.mConfig.allowRepeatCallers || Secure.getInt(this.mContext.getContentResolver(), "zen_call_white_list_enabled", 0) == 1) && (this.mSuppressedEffects & 2) == 0);
        boolean muteAlarms = zenPriorityOnly && !this.mConfig.allowAlarms;
        boolean muteMedia = zenPriorityOnly && !this.mConfig.allowMedia;
        boolean muteSystem = zenAlarmsOnly || (zenPriorityOnly && !this.mConfig.allowSystem);
        boolean muteEverything = zenSilence2 || (zenPriorityOnly && ZenModeConfig.areAllZenBehaviorSoundsMuted(this.mConfig));
        boolean realMuteEverything = Global.getInt(this.mContext.getContentResolver(), "total_silence_mode", 0) == 1;
        int[] iArr = AudioAttributes.SDK_USAGES;
        int length = iArr.length;
        int i = 0;
        while (i < length) {
            int i2;
            int i3;
            int usage = iArr[i];
            boolean suppressionBehavior = AudioAttributes.SUPPRESSIBLE_USAGES.get(usage);
            boolean zenPriorityOnly2 = zenPriorityOnly;
            if (suppressionBehavior) {
                applyRestrictions(false, usage);
            } else {
                zenPriorityOnly = true;
                if (suppressionBehavior) {
                    if (!(muteNotifications || muteEverything)) {
                        zenPriorityOnly = false;
                    }
                    applyRestrictions(zenPriorityOnly, usage);
                } else if (suppressionBehavior) {
                    zenPriorityOnly = muteCalls || muteEverything;
                    applyRestrictions(zenPriorityOnly, usage);
                } else if (usage == 4) {
                    zenPriorityOnly = muteAlarms || muteEverything;
                    applyRestrictions(zenPriorityOnly, usage);
                } else if (suppressionBehavior) {
                    zenPriorityOnly = muteMedia || (muteEverything && realMuteEverything);
                    applyRestrictions(zenPriorityOnly, usage);
                } else {
                    if (!suppressionBehavior) {
                        zenSilence = zenSilence2;
                        i2 = 3;
                        applyRestrictions(muteEverything, usage);
                    } else if (usage == 13) {
                        zenPriorityOnly = muteSystem || muteEverything;
                        zenSilence = zenSilence2;
                        applyRestrictions(zenPriorityOnly, usage, true);
                        i2 = 3;
                        applyRestrictions(false, usage, 3);
                    } else {
                        zenSilence = zenSilence2;
                        zenPriorityOnly = false;
                        i2 = 3;
                        if (muteSystem || muteEverything) {
                            zenPriorityOnly = true;
                        }
                        applyRestrictions(zenPriorityOnly, usage);
                    }
                    i++;
                    i3 = i2;
                    zenPriorityOnly = zenPriorityOnly2;
                    zenSilence2 = zenSilence;
                }
            }
            zenSilence = zenSilence2;
            i2 = 3;
            i++;
            i3 = i2;
            zenPriorityOnly = zenPriorityOnly2;
            zenSilence2 = zenSilence;
        }
        zenSilence = zenSilence2;
    }

    @VisibleForTesting
    protected void applyRestrictions(boolean mute, int usage, int code) {
        String[] exceptionPackages = getExceptionPackages(usage);
        if (Process.myUid() == 1000) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mAppOps.setRestriction(code, usage, mute, exceptionPackages);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    @VisibleForTesting
    protected void applyRestrictions(boolean mute, int usage) {
        applyRestrictions(mute, usage, 3);
        applyRestrictions(mute, usage, 28);
    }

    private String[] getExceptionPackages(int usage) {
        List<String> listPkgs = new ArrayList();
        if (!(this.mCust == null || this.mCust.getWhiteApps(this.mContext) == null)) {
            listPkgs.addAll(Arrays.asList(this.mCust.getWhiteApps(this.mContext)));
        }
        if (4 == usage && this.mZenMode != 2) {
            listPkgs.add("com.android.deskclock");
        }
        if (listPkgs.size() == 0) {
            return null;
        }
        String[] whiteApps = new String[listPkgs.size()];
        listPkgs.toArray(whiteApps);
        return whiteApps;
    }

    @VisibleForTesting
    protected void applyZenToRingerMode() {
        if (this.mAudioManager != null) {
            int ringerModeInternal = this.mAudioManager.getRingerModeInternal();
            int newRingerModeInternal = ringerModeInternal;
            switch (this.mZenMode) {
                case 0:
                    if (ringerModeInternal == 0) {
                        newRingerModeInternal = getPreviousRingerModeSetting();
                        setPreviousRingerModeSetting(null);
                        break;
                    }
                    break;
                case 2:
                case 3:
                    if (ringerModeInternal != 0) {
                        setPreviousRingerModeSetting(Integer.valueOf(ringerModeInternal));
                        break;
                    }
                    break;
            }
        }
    }

    private void dispatchOnConfigChanged() {
        Iterator it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            ((Callback) it.next()).onConfigChanged();
        }
    }

    private void dispatchOnPolicyChanged() {
        Iterator it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            ((Callback) it.next()).onPolicyChanged();
        }
    }

    private void dispatchOnZenModeChanged() {
        Iterator it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            ((Callback) it.next()).onZenModeChanged();
        }
    }

    private ZenModeConfig readDefaultConfig(Resources resources) {
        XmlResourceParser parser = null;
        try {
            parser = resources.getXml(18284551);
            while (parser.next() != 1) {
                ZenModeConfig config = ZenModeConfig.readXml(parser);
                if (config != null) {
                    IoUtils.closeQuietly(parser);
                    return config;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading default zen mode config from resource", e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
        IoUtils.closeQuietly(parser);
        return new ZenModeConfig();
    }

    private void appendDefaultEveryNightRule(ZenModeConfig config) {
        if (config != null) {
            ScheduleInfo weeknights = new ScheduleInfo();
            weeknights.days = ZenModeConfig.WEEKEND_DAYS;
            weeknights.startHour = 22;
            weeknights.endHour = 7;
            weeknights.exitAtAlarm = false;
            ZenRule rule = new ZenRule();
            rule.enabled = false;
            rule.name = this.mDefaultRuleEveryNightName;
            rule.conditionId = ZenModeConfig.toScheduleConditionId(weeknights);
            rule.zenMode = 1;
            rule.component = ScheduleConditionProvider.COMPONENT;
            rule.id = "EVERY_NIGHT_DEFAULT_RULE";
            rule.creationTime = System.currentTimeMillis();
            config.automaticRules.put(rule.id, rule);
        }
    }

    private void appendDefaultEventRules(ZenModeConfig config) {
        if (config != null) {
            EventInfo events = new EventInfo();
            events.calendar = null;
            events.reply = 1;
            ZenRule rule = new ZenRule();
            rule.enabled = false;
            rule.name = this.mDefaultRuleEventsName;
            rule.conditionId = ZenModeConfig.toEventConditionId(events);
            rule.zenMode = 1;
            rule.component = EventConditionProvider.COMPONENT;
            rule.id = "EVENTS_DEFAULT_RULE";
            rule.creationTime = System.currentTimeMillis();
            config.automaticRules.put(rule.id, rule);
        }
    }

    private static int zenSeverity(int zen) {
        switch (zen) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return 0;
        }
    }

    private void showZenUpgradeNotification(int zen) {
        boolean showNotification = (!this.mIsBootComplete || zen == 0 || Global.getInt(this.mContext.getContentResolver(), "show_zen_upgrade_notification", 0) == 0) ? false : true;
        if (showNotification) {
            Global.putInt(this.mContext.getContentResolver(), "show_zen_upgrade_notification", 0);
        }
    }

    @VisibleForTesting
    protected Notification createZenUpgradeNotification() {
        Bundle extras = new Bundle();
        extras.putString("android.substName", this.mContext.getResources().getString(17040131));
        int title = 17041444;
        int content = 17041443;
        int drawable = 17302797;
        if (Policy.areAllVisualEffectsSuppressed(getNotificationPolicy().suppressedVisualEffects)) {
            title = 17041446;
            content = 17041445;
            drawable = 17302356;
        }
        Intent onboardingIntent = new Intent("android.settings.ZEN_MODE_ONBOARDING");
        onboardingIntent.addFlags(268468224);
        return new Builder(this.mContext, SystemNotificationChannels.DO_NOT_DISTURB).setAutoCancel(true).setSmallIcon(17302750).setLargeIcon(Icon.createWithResource(this.mContext, drawable)).setContentTitle(this.mContext.getResources().getString(title)).setContentText(this.mContext.getResources().getString(content)).setContentIntent(PendingIntent.getActivity(this.mContext, 0, onboardingIntent, 134217728)).setAutoCancel(true).setLocalOnly(true).addExtras(extras).setStyle(new BigTextStyle()).build();
    }
}
