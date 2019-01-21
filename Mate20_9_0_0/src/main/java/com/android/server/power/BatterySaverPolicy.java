package com.android.server.power;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerSaveState;
import android.os.PowerSaveState.Builder;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.power.batterysaver.BatterySavingStats;
import com.android.server.power.batterysaver.CpuFrequencies;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BatterySaverPolicy extends ContentObserver {
    public static final boolean DEBUG = false;
    private static final String KEY_ADJUST_BRIGHTNESS_DISABLED = "adjust_brightness_disabled";
    private static final String KEY_ADJUST_BRIGHTNESS_FACTOR = "adjust_brightness_factor";
    private static final String KEY_ANIMATION_DISABLED = "animation_disabled";
    private static final String KEY_AOD_DISABLED = "aod_disabled";
    private static final String KEY_CPU_FREQ_INTERACTIVE = "cpufreq-i";
    private static final String KEY_CPU_FREQ_NONINTERACTIVE = "cpufreq-n";
    private static final String KEY_DATASAVER_DISABLED = "datasaver_disabled";
    private static final String KEY_FIREWALL_DISABLED = "firewall_disabled";
    private static final String KEY_FORCE_ALL_APPS_STANDBY = "force_all_apps_standby";
    private static final String KEY_FORCE_BACKGROUND_CHECK = "force_background_check";
    private static final String KEY_FULLBACKUP_DEFERRED = "fullbackup_deferred";
    private static final String KEY_GPS_MODE = "gps_mode";
    private static final String KEY_KEYVALUE_DEFERRED = "keyvaluebackup_deferred";
    private static final String KEY_LAUNCH_BOOST_DISABLED = "launch_boost_disabled";
    private static final String KEY_OPTIONAL_SENSORS_DISABLED = "optional_sensors_disabled";
    private static final String KEY_SEND_TRON_LOG = "send_tron_log";
    private static final String KEY_SOUNDTRIGGER_DISABLED = "soundtrigger_disabled";
    private static final String KEY_VIBRATION_DISABLED = "vibration_disabled";
    public static final String SECURE_KEY_GPS_MODE = "batterySaverGpsMode";
    private static final String TAG = "BatterySaverPolicy";
    @GuardedBy("mLock")
    private boolean mAccessibilityEnabled;
    @GuardedBy("mLock")
    private boolean mAdjustBrightnessDisabled;
    @GuardedBy("mLock")
    private float mAdjustBrightnessFactor;
    @GuardedBy("mLock")
    private boolean mAnimationDisabled;
    @GuardedBy("mLock")
    private boolean mAodDisabled;
    private final BatterySavingStats mBatterySavingStats;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    @GuardedBy("mLock")
    private boolean mDataSaverDisabled;
    @GuardedBy("mLock")
    private String mDeviceSpecificSettings;
    @GuardedBy("mLock")
    private String mDeviceSpecificSettingsSource;
    @GuardedBy("mLock")
    private String mEventLogKeys;
    @GuardedBy("mLock")
    private ArrayMap<String, String> mFilesForInteractive;
    @GuardedBy("mLock")
    private ArrayMap<String, String> mFilesForNoninteractive;
    @GuardedBy("mLock")
    private boolean mFireWallDisabled;
    @GuardedBy("mLock")
    private boolean mForceAllAppsStandby;
    @GuardedBy("mLock")
    private boolean mForceBackgroundCheck;
    @GuardedBy("mLock")
    private boolean mFullBackupDeferred;
    @GuardedBy("mLock")
    private int mGpsMode;
    private final Handler mHandler;
    @GuardedBy("mLock")
    private boolean mKeyValueBackupDeferred;
    @GuardedBy("mLock")
    private boolean mLaunchBoostDisabled;
    @GuardedBy("mLock")
    private final List<BatterySaverPolicyListener> mListeners = new ArrayList();
    private final Object mLock;
    @GuardedBy("mLock")
    private boolean mOptionalSensorsDisabled;
    @GuardedBy("mLock")
    private boolean mSendTronLog;
    @GuardedBy("mLock")
    private String mSettings;
    @GuardedBy("mLock")
    private boolean mSoundTriggerDisabled;
    @GuardedBy("mLock")
    private boolean mVibrationDisabledConfig;
    @GuardedBy("mLock")
    private boolean mVibrationDisabledEffective;

    public interface BatterySaverPolicyListener {
        void onBatterySaverPolicyChanged(BatterySaverPolicy batterySaverPolicy);
    }

    public BatterySaverPolicy(Object lock, Context context, BatterySavingStats batterySavingStats) {
        super(BackgroundThread.getHandler());
        this.mLock = lock;
        this.mHandler = BackgroundThread.getHandler();
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mBatterySavingStats = batterySavingStats;
    }

    public void systemReady() {
        ConcurrentUtils.wtfIfLockHeld(TAG, this.mLock);
        this.mContentResolver.registerContentObserver(Global.getUriFor("battery_saver_constants"), false, this);
        this.mContentResolver.registerContentObserver(Global.getUriFor("battery_saver_device_specific_constants"), false, this);
        AccessibilityManager acm = (AccessibilityManager) this.mContext.getSystemService(AccessibilityManager.class);
        acm.addAccessibilityStateChangeListener(new -$$Lambda$BatterySaverPolicy$DPeh8xGdH0ye3BQJ8Ozaqeu6Y30(this));
        boolean enabled = acm.isEnabled();
        synchronized (this.mLock) {
            this.mAccessibilityEnabled = enabled;
        }
        onChange(true, null);
    }

    public static /* synthetic */ void lambda$systemReady$0(BatterySaverPolicy batterySaverPolicy, boolean enabled) {
        synchronized (batterySaverPolicy.mLock) {
            batterySaverPolicy.mAccessibilityEnabled = enabled;
        }
        batterySaverPolicy.refreshSettings();
    }

    public void addListener(BatterySaverPolicyListener listener) {
        synchronized (this.mLock) {
            this.mListeners.add(listener);
        }
    }

    @VisibleForTesting
    String getGlobalSetting(String key) {
        return Global.getString(this.mContentResolver, key);
    }

    @VisibleForTesting
    int getDeviceSpecificConfigResId() {
        return 17039768;
    }

    public void onChange(boolean selfChange, Uri uri) {
        refreshSettings();
    }

    private void refreshSettings() {
        BatterySaverPolicyListener[] listeners;
        synchronized (this.mLock) {
            String setting = getGlobalSetting("battery_saver_constants");
            String deviceSpecificSetting = getGlobalSetting("battery_saver_device_specific_constants");
            this.mDeviceSpecificSettingsSource = "battery_saver_device_specific_constants";
            if (TextUtils.isEmpty(deviceSpecificSetting) || "null".equals(deviceSpecificSetting)) {
                deviceSpecificSetting = this.mContext.getString(getDeviceSpecificConfigResId());
                this.mDeviceSpecificSettingsSource = "(overlay)";
            }
            updateConstantsLocked(setting, deviceSpecificSetting);
            listeners = (BatterySaverPolicyListener[]) this.mListeners.toArray(new BatterySaverPolicyListener[this.mListeners.size()]);
        }
        this.mHandler.post(new -$$Lambda$BatterySaverPolicy$9q6hxnTofoZqK_ebwl_HDCH8A4A(this, listeners));
    }

    public static /* synthetic */ void lambda$refreshSettings$1(BatterySaverPolicy batterySaverPolicy, BatterySaverPolicyListener[] listeners) {
        for (BatterySaverPolicyListener listener : listeners) {
            listener.onBatterySaverPolicyChanged(batterySaverPolicy);
        }
    }

    @GuardedBy("mLock")
    @VisibleForTesting
    void updateConstantsLocked(String setting, String deviceSpecificSetting) {
        this.mSettings = setting;
        this.mDeviceSpecificSettings = deviceSpecificSetting;
        KeyValueListParser parser = new KeyValueListParser(',');
        try {
            parser.setString(setting);
        } catch (IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad battery saver constants: ");
            stringBuilder.append(setting);
            Slog.wtf(str, stringBuilder.toString());
        }
        boolean z = true;
        this.mVibrationDisabledConfig = parser.getBoolean(KEY_VIBRATION_DISABLED, true);
        this.mAnimationDisabled = parser.getBoolean(KEY_ANIMATION_DISABLED, false);
        this.mSoundTriggerDisabled = parser.getBoolean(KEY_SOUNDTRIGGER_DISABLED, true);
        this.mFullBackupDeferred = parser.getBoolean(KEY_FULLBACKUP_DEFERRED, true);
        this.mKeyValueBackupDeferred = parser.getBoolean(KEY_KEYVALUE_DEFERRED, true);
        this.mFireWallDisabled = parser.getBoolean(KEY_FIREWALL_DISABLED, false);
        this.mAdjustBrightnessDisabled = parser.getBoolean(KEY_ADJUST_BRIGHTNESS_DISABLED, true);
        this.mAdjustBrightnessFactor = parser.getFloat(KEY_ADJUST_BRIGHTNESS_FACTOR, 0.5f);
        this.mDataSaverDisabled = parser.getBoolean(KEY_DATASAVER_DISABLED, true);
        this.mLaunchBoostDisabled = parser.getBoolean(KEY_LAUNCH_BOOST_DISABLED, true);
        this.mForceAllAppsStandby = parser.getBoolean(KEY_FORCE_ALL_APPS_STANDBY, true);
        this.mForceBackgroundCheck = parser.getBoolean(KEY_FORCE_BACKGROUND_CHECK, true);
        this.mOptionalSensorsDisabled = parser.getBoolean(KEY_OPTIONAL_SENSORS_DISABLED, true);
        this.mAodDisabled = parser.getBoolean(KEY_AOD_DISABLED, true);
        this.mSendTronLog = parser.getBoolean(KEY_SEND_TRON_LOG, false);
        this.mGpsMode = parser.getInt(KEY_GPS_MODE, Secure.getInt(this.mContentResolver, SECURE_KEY_GPS_MODE, 2));
        try {
            parser.setString(deviceSpecificSetting);
        } catch (IllegalArgumentException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Bad device specific battery saver constants: ");
            stringBuilder2.append(deviceSpecificSetting);
            Slog.wtf(str2, stringBuilder2.toString());
        }
        this.mFilesForInteractive = new CpuFrequencies().parseString(parser.getString(KEY_CPU_FREQ_INTERACTIVE, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)).toSysFileMap();
        this.mFilesForNoninteractive = new CpuFrequencies().parseString(parser.getString(KEY_CPU_FREQ_NONINTERACTIVE, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)).toSysFileMap();
        if (!this.mVibrationDisabledConfig || this.mAccessibilityEnabled) {
            z = false;
        }
        this.mVibrationDisabledEffective = z;
        StringBuilder sb = new StringBuilder();
        if (this.mForceAllAppsStandby) {
            sb.append("A");
        }
        if (this.mForceBackgroundCheck) {
            sb.append("B");
        }
        if (this.mVibrationDisabledEffective) {
            sb.append("v");
        }
        if (this.mAnimationDisabled) {
            sb.append("a");
        }
        if (this.mSoundTriggerDisabled) {
            sb.append("s");
        }
        if (this.mFullBackupDeferred) {
            sb.append("F");
        }
        if (this.mKeyValueBackupDeferred) {
            sb.append("K");
        }
        if (!this.mFireWallDisabled) {
            sb.append("f");
        }
        if (!this.mDataSaverDisabled) {
            sb.append("d");
        }
        if (!this.mAdjustBrightnessDisabled) {
            sb.append("b");
        }
        if (this.mLaunchBoostDisabled) {
            sb.append("l");
        }
        if (this.mOptionalSensorsDisabled) {
            sb.append("S");
        }
        if (this.mAodDisabled) {
            sb.append("o");
        }
        if (this.mSendTronLog) {
            sb.append("t");
        }
        sb.append(this.mGpsMode);
        this.mEventLogKeys = sb.toString();
        this.mBatterySavingStats.setSendTronLog(this.mSendTronLog);
    }

    public PowerSaveState getBatterySaverPolicy(int type, boolean realMode) {
        synchronized (this.mLock) {
            Builder builder = new Builder().setGlobalBatterySaverEnabled(realMode);
            PowerSaveState build;
            if (realMode) {
                switch (type) {
                    case 1:
                        build = builder.setBatterySaverEnabled(realMode).setGpsMode(this.mGpsMode).build();
                        return build;
                    case 2:
                        build = builder.setBatterySaverEnabled(this.mVibrationDisabledEffective).build();
                        return build;
                    case 3:
                        build = builder.setBatterySaverEnabled(this.mAnimationDisabled).build();
                        return build;
                    case 4:
                        build = builder.setBatterySaverEnabled(this.mFullBackupDeferred).build();
                        return build;
                    case 5:
                        build = builder.setBatterySaverEnabled(this.mKeyValueBackupDeferred).build();
                        return build;
                    case 6:
                        build = builder.setBatterySaverEnabled(this.mFireWallDisabled ^ 1).build();
                        return build;
                    case 7:
                        build = builder.setBatterySaverEnabled(this.mAdjustBrightnessDisabled ^ 1).setBrightnessFactor(this.mAdjustBrightnessFactor).build();
                        return build;
                    case 8:
                        build = builder.setBatterySaverEnabled(this.mSoundTriggerDisabled).build();
                        return build;
                    case 10:
                        build = builder.setBatterySaverEnabled(this.mDataSaverDisabled ^ 1).build();
                        return build;
                    case 11:
                        build = builder.setBatterySaverEnabled(this.mForceAllAppsStandby).build();
                        return build;
                    case 12:
                        build = builder.setBatterySaverEnabled(this.mForceBackgroundCheck).build();
                        return build;
                    case 13:
                        build = builder.setBatterySaverEnabled(this.mOptionalSensorsDisabled).build();
                        return build;
                    case 14:
                        build = builder.setBatterySaverEnabled(this.mAodDisabled).build();
                        return build;
                    default:
                        build = builder.setBatterySaverEnabled(realMode).build();
                        return build;
                }
            }
            build = builder.setBatterySaverEnabled(realMode).build();
            return build;
        }
    }

    public int getGpsMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mGpsMode;
        }
        return i;
    }

    public ArrayMap<String, String> getFileValues(boolean interactive) {
        ArrayMap<String, String> arrayMap;
        synchronized (this.mLock) {
            if (interactive) {
                try {
                    arrayMap = this.mFilesForInteractive;
                } catch (Throwable th) {
                }
            } else {
                arrayMap = this.mFilesForNoninteractive;
            }
        }
        return arrayMap;
    }

    public boolean isLaunchBoostDisabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mLaunchBoostDisabled;
        }
        return z;
    }

    public String toEventLogString() {
        String str;
        synchronized (this.mLock) {
            str = this.mEventLogKeys;
        }
        return str;
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println();
            this.mBatterySavingStats.dump(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            pw.println();
            pw.println("Battery saver policy (*NOTE* they only apply when battery saver is ON):");
            pw.println("  Settings: battery_saver_constants");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    value: ");
            stringBuilder.append(this.mSettings);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Settings: ");
            stringBuilder.append(this.mDeviceSpecificSettingsSource);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    value: ");
            stringBuilder.append(this.mDeviceSpecificSettings);
            pw.println(stringBuilder.toString());
            pw.println();
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mAccessibilityEnabled=");
            stringBuilder.append(this.mAccessibilityEnabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  vibration_disabled:config=");
            stringBuilder.append(this.mVibrationDisabledConfig);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  vibration_disabled:effective=");
            stringBuilder.append(this.mVibrationDisabledEffective);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  animation_disabled=");
            stringBuilder.append(this.mAnimationDisabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  fullbackup_deferred=");
            stringBuilder.append(this.mFullBackupDeferred);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  keyvaluebackup_deferred=");
            stringBuilder.append(this.mKeyValueBackupDeferred);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  firewall_disabled=");
            stringBuilder.append(this.mFireWallDisabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  datasaver_disabled=");
            stringBuilder.append(this.mDataSaverDisabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  launch_boost_disabled=");
            stringBuilder.append(this.mLaunchBoostDisabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  adjust_brightness_disabled=");
            stringBuilder.append(this.mAdjustBrightnessDisabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  adjust_brightness_factor=");
            stringBuilder.append(this.mAdjustBrightnessFactor);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  gps_mode=");
            stringBuilder.append(this.mGpsMode);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  force_all_apps_standby=");
            stringBuilder.append(this.mForceAllAppsStandby);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  force_background_check=");
            stringBuilder.append(this.mForceBackgroundCheck);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  optional_sensors_disabled=");
            stringBuilder.append(this.mOptionalSensorsDisabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  aod_disabled=");
            stringBuilder.append(this.mAodDisabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  send_tron_log=");
            stringBuilder.append(this.mSendTronLog);
            pw.println(stringBuilder.toString());
            pw.println();
            pw.print("  Interactive File values:\n");
            dumpMap(pw, "    ", this.mFilesForInteractive);
            pw.println();
            pw.print("  Noninteractive File values:\n");
            dumpMap(pw, "    ", this.mFilesForNoninteractive);
        }
    }

    private void dumpMap(PrintWriter pw, String prefix, ArrayMap<String, String> map) {
        if (map != null) {
            int size = map.size();
            for (int i = 0; i < size; i++) {
                pw.print(prefix);
                pw.print((String) map.keyAt(i));
                pw.print(": '");
                pw.print((String) map.valueAt(i));
                pw.println("'");
            }
        }
    }

    @VisibleForTesting
    public void setAccessibilityEnabledForTest(boolean enabled) {
        synchronized (this.mLock) {
            this.mAccessibilityEnabled = enabled;
        }
    }
}
