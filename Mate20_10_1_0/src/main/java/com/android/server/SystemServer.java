package com.android.server;

import android.app.ActivityThread;
import android.app.INotificationManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteCompatibilityWalFlags;
import android.database.sqlite.SQLiteGlobal;
import android.hardware.display.DisplayManagerInternal;
import android.hsm.HwSystemManager;
import android.net.IConnectivityManager;
import android.net.INetd;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.NetworkStackClient;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FactoryTest;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.IIncidentManager;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.sysprop.VoldProperties;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.TimingsTraceLog;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodSystemProperty;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BinderInternal;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.widget.ILockSettings;
import com.android.server.BatteryService;
import com.android.server.BinderCallsStatsService;
import com.android.server.HwServiceFactory;
import com.android.server.LooperStatsService;
import com.android.server.NetworkScoreService;
import com.android.server.am.ActivityManagerService;
import com.android.server.appbinding.AppBindingService;
import com.android.server.attention.AttentionManagerService;
import com.android.server.audio.AudioService;
import com.android.server.biometrics.BiometricService;
import com.android.server.biometrics.face.FaceService;
import com.android.server.biometrics.fingerprint.FingerprintService;
import com.android.server.biometrics.iris.IrisService;
import com.android.server.broadcastradio.BroadcastRadioService;
import com.android.server.camera.CameraServiceProxy;
import com.android.server.clipboard.ClipboardService;
import com.android.server.connectivity.IpConnectivityMetrics;
import com.android.server.contentcapture.ContentCaptureManagerInternal;
import com.android.server.coverage.CoverageService;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.color.ColorDisplayService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.emergency.EmergencyAffordanceService;
import com.android.server.gpu.GpuService;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.incident.IncidentCompanionService;
import com.android.server.input.InputManagerService;
import com.android.server.inputmethod.InputMethodManagerService;
import com.android.server.inputmethod.MultiClientInputMethodManagerService;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.lights.LightsService;
import com.android.server.media.MediaResourceMonitorService;
import com.android.server.media.MediaRouterService;
import com.android.server.media.MediaSessionService;
import com.android.server.media.projection.MediaProjectionManagerService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.net.watchlist.NetworkWatchlistService;
import com.android.server.notification.NotificationManagerService;
import com.android.server.oemlock.OemLockService;
import com.android.server.om.OverlayManagerService;
import com.android.server.os.BugreportManagerService;
import com.android.server.os.DeviceIdentifiersPolicyService;
import com.android.server.os.HwBootCheck;
import com.android.server.os.HwBootFail;
import com.android.server.os.SchedulingPolicyService;
import com.android.server.pg.PGManagerService;
import com.android.server.pm.BackgroundDexOptService;
import com.android.server.pm.CrossProfileAppsService;
import com.android.server.pm.DynamicCodeLoggingService;
import com.android.server.pm.Installer;
import com.android.server.pm.LauncherAppsService;
import com.android.server.pm.OtaDexoptService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.UserManagerService;
import com.android.server.policy.HwPolicyFactory;
import com.android.server.policy.PermissionPolicyService;
import com.android.server.policy.role.LegacyRoleResolutionPolicy;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.power.ThermalManagerService;
import com.android.server.restrictions.RestrictionsManagerService;
import com.android.server.role.RoleManagerService;
import com.android.server.rollback.RollbackManagerService;
import com.android.server.security.KeyAttestationApplicationIdProviderService;
import com.android.server.security.KeyChainSystemService;
import com.android.server.signedconfig.SignedConfigService;
import com.android.server.soundtrigger.SoundTriggerService;
import com.android.server.stats.StatsCompanionService;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.telecom.TelecomLoaderService;
import com.android.server.testharness.TestHarnessModeService;
import com.android.server.textclassifier.TextClassificationManagerService;
import com.android.server.textservices.TextServicesManagerService;
import com.android.server.trust.TrustManagerService;
import com.android.server.tv.TvInputManagerService;
import com.android.server.tv.TvRemoteService;
import com.android.server.twilight.TwilightService;
import com.android.server.uri.UriGrantsManagerService;
import com.android.server.usage.UsageStatsService;
import com.android.server.utils.LogBufferUtil;
import com.android.server.vr.VrManagerService;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.IHwRogEx;
import com.android.server.wm.WindowManagerGlobalLock;
import com.android.server.wm.WindowManagerService;
import com.huawei.featurelayer.HwFeatureLoader;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public final class SystemServer {
    private static final String ACCESSIBILITY_MANAGER_SERVICE_CLASS = "com.android.server.accessibility.AccessibilityManagerService$Lifecycle";
    private static final String ACCOUNT_SERVICE_CLASS = "com.android.server.accounts.AccountManagerService$Lifecycle";
    private static final String ADB_SERVICE_CLASS = "com.android.server.adb.AdbService$Lifecycle";
    private static final String APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService";
    private static final String APP_PREDICTION_MANAGER_SERVICE_CLASS = "com.android.server.appprediction.AppPredictionManagerService";
    private static final String AUTO_FILL_MANAGER_SERVICE_CLASS = "com.android.server.autofill.AutofillManagerService";
    private static final String BACKUP_MANAGER_SERVICE_CLASS = "com.android.server.backup.BackupManagerService$Lifecycle";
    private static final String BLOCK_MAP_FILE = "/cache/recovery/block.map";
    private static final TimingsTraceLog BOOT_TIMINGS_TRACE_LOG = new TimingsTraceLog(SYSTEM_SERVER_TIMING_TAG, 524288);
    private static final String CAR_SERVICE_HELPER_SERVICE_CLASS = "com.android.internal.car.CarServiceHelperService";
    private static final String COMPANION_DEVICE_MANAGER_SERVICE_CLASS = "com.android.server.companion.CompanionDeviceManagerService";
    private static final String CONTENT_CAPTURE_MANAGER_SERVICE_CLASS = "com.android.server.contentcapture.ContentCaptureManagerService";
    private static final String CONTENT_SERVICE_CLASS = "com.android.server.content.ContentService$Lifecycle";
    private static final String CONTENT_SUGGESTIONS_SERVICE_CLASS = "com.android.server.contentsuggestions.ContentSuggestionsManagerService";
    private static final int DEFAULT_SYSTEM_THEME = 16974848;
    private static final long EARLIEST_SUPPORTED_TIME = 86400000;
    private static final String ENCRYPTED_STATE = "1";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ETHERNET_SERVICE_CLASS = "com.android.server.ethernet.EthernetService";
    private static final String GSI_RUNNING_PROP = "ro.gsid.image_running";
    private static final String IOT_SERVICE_CLASS = "com.android.things.server.IoTSystemService";
    private static final String JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService";
    private static final String LOCK_SETTINGS_SERVICE_CLASS = "com.android.server.locksettings.LockSettingsService$Lifecycle";
    private static final String LOWPAN_SERVICE_CLASS = "com.android.server.lowpan.LowpanService";
    private static final boolean MAPLE_ENABLE = (SystemProperties.get("ro.maple.enable", "0").equals(ENCRYPTED_STATE) && !SystemProperties.get("persist.mygote.disable", "0").equals(ENCRYPTED_STATE));
    private static final String MIDI_SERVICE_CLASS = "com.android.server.midi.MidiService$Lifecycle";
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String PRINT_MANAGER_SERVICE_CLASS = "com.android.server.print.PrintManagerService";
    private static final String SEARCH_MANAGER_SERVICE_CLASS = "com.android.server.search.SearchManagerService$Lifecycle";
    private static final String SLICE_MANAGER_SERVICE_CLASS = "com.android.server.slice.SliceManagerService$Lifecycle";
    private static final long SLOW_DELIVERY_THRESHOLD_MS = 200;
    private static final long SLOW_DISPATCH_THRESHOLD_MS = 100;
    private static final long SNAPSHOT_INTERVAL = 3600000;
    private static final String START_HIDL_SERVICES = "StartHidlServices";
    private static final String START_SENSOR_SERVICE = "StartSensorService";
    private static final String STORAGE_MANAGER_SERVICE_CLASS = "com.android.server.StorageManagerService$Lifecycle";
    private static final String STORAGE_STATS_SERVICE_CLASS = "com.android.server.usage.StorageStatsService$Lifecycle";
    private static final boolean SUPPORT_HW_ATTESTATION = SystemProperties.getBoolean("ro.config.support_hwattestation", true);
    private static final String SYSPROP_START_COUNT = "sys.system_server.start_count";
    private static final String SYSPROP_START_ELAPSED = "sys.system_server.start_elapsed";
    private static final String SYSPROP_START_UPTIME = "sys.system_server.start_uptime";
    private static final String SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS = "com.android.server.systemcaptions.SystemCaptionsManagerService";
    private static final String SYSTEM_SERVER_TIMING_ASYNC_TAG = "SystemServerTimingAsync";
    private static final String SYSTEM_SERVER_TIMING_TAG = "SystemServerTiming";
    private static final String TAG = "SystemServer";
    private static final String THERMAL_OBSERVER_CLASS = "com.google.android.clockwork.ThermalObserver";
    private static final String TIME_DETECTOR_SERVICE_CLASS = "com.android.server.timedetector.TimeDetectorService$Lifecycle";
    private static final String TIME_ZONE_RULES_MANAGER_SERVICE_CLASS = "com.android.server.timezone.RulesManagerService$Lifecycle";
    private static final String UNCRYPT_PACKAGE_FILE = "/cache/recovery/uncrypt_file";
    private static final String USB_SERVICE_CLASS = "com.android.server.usb.UsbService$Lifecycle";
    private static final String VOICE_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.voiceinteraction.VoiceInteractionManagerService";
    private static final String WALLPAPER_SERVICE_CLASS = "com.android.server.wallpaper.WallpaperManagerService$Lifecycle";
    private static final String WEAR_CONNECTIVITY_SERVICE_CLASS = "com.android.clockwork.connectivity.WearConnectivityService";
    private static final String WEAR_DISPLAY_SERVICE_CLASS = "com.google.android.clockwork.display.WearDisplayService";
    private static final String WEAR_GLOBAL_ACTIONS_SERVICE_CLASS = "com.android.clockwork.globalactions.GlobalActionsService";
    private static final String WEAR_LEFTY_SERVICE_CLASS = "com.google.android.clockwork.lefty.WearLeftyService";
    private static final String WEAR_POWER_SERVICE_CLASS = "com.android.clockwork.power.WearPowerService";
    private static final String WEAR_SIDEKICK_SERVICE_CLASS = "com.google.android.clockwork.sidekick.SidekickService";
    private static final String WEAR_TIME_SERVICE_CLASS = "com.google.android.clockwork.time.WearTimeService";
    private static final String WIFI_AWARE_SERVICE_CLASS = "com.android.server.wifi.aware.WifiAwareService";
    private static final String WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService";
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    private static final int sMaxBinderThreads = 31;
    private Installer installer;
    private ActivityManagerService mActivityManagerService;
    private ContentResolver mContentResolver;
    private DisplayManagerService mDisplayManagerService;
    private EntropyMixer mEntropyMixer;
    private final int mFactoryTestMode = FactoryTest.getMode();
    private boolean mFirstBoot;
    private IHwRogEx mHwRogEx;
    private boolean mOnlyCore;
    private PGManagerService mPGManagerService;
    private PackageManager mPackageManager;
    private PackageManagerService mPackageManagerService;
    private PowerManagerService mPowerManagerService;
    private Future<?> mPrimaryZygotePreload;
    private Timer mProfilerSnapshotTimer;
    private final boolean mRuntimeRestart = ENCRYPTED_STATE.equals(SystemProperties.get("sys.boot_completed"));
    private final long mRuntimeStartElapsedTime = SystemClock.elapsedRealtime();
    private final long mRuntimeStartUptime = SystemClock.uptimeMillis();
    private Future<?> mSensorServiceStart;
    private final int mStartCount = (SystemProperties.getInt(SYSPROP_START_COUNT, 0) + 1);
    private Context mSystemContext;
    private SystemServiceManager mSystemServiceManager;
    private WebViewUpdateService mWebViewUpdateService;
    private WindowManagerGlobalLock mWindowManagerGlobalLock;
    private Future<?> mZygotePreload;

    private static native void initZygoteChildHeapProfiling();

    private static native void startHidlServices();

    private static native void startSensorService();

    private static native void startSysSvcCallRecordService();

    public static void main(String[] args) {
        new SystemServer().run();
    }

    /* JADX INFO: finally extract failed */
    private void run() {
        try {
            traceBeginAndSlog("InitBeforeStartServices");
            SystemProperties.set(SYSPROP_START_COUNT, String.valueOf(this.mStartCount));
            SystemProperties.set(SYSPROP_START_ELAPSED, String.valueOf(this.mRuntimeStartElapsedTime));
            SystemProperties.set(SYSPROP_START_UPTIME, String.valueOf(this.mRuntimeStartUptime));
            EventLog.writeEvent((int) EventLogTags.SYSTEM_SERVER_START, Integer.valueOf(this.mStartCount), Long.valueOf(this.mRuntimeStartUptime), Long.valueOf(this.mRuntimeStartElapsedTime));
            if (System.currentTimeMillis() < 86400000) {
                Slog.w(TAG, "System clock is before 1970; setting to 1970.");
                SystemClock.setCurrentTimeMillis(86400000);
            }
            if (!SystemProperties.get("persist.sys.language").isEmpty()) {
                SystemProperties.set("persist.sys.locale", Locale.getDefault().toLanguageTag());
                SystemProperties.set("persist.sys.language", "");
                SystemProperties.set("persist.sys.country", "");
                SystemProperties.set("persist.sys.localevar", "");
            }
            Binder.setWarnOnBlocking(true);
            PackageItemInfo.forceSafeLabels();
            SQLiteGlobal.sDefaultSyncMode = "FULL";
            SQLiteCompatibilityWalFlags.init((String) null);
            Slog.i(TAG, "Entered the Android system server!");
            int uptimeMillis = (int) SystemClock.elapsedRealtime();
            EventLog.writeEvent((int) EventLogTags.BOOT_PROGRESS_SYSTEM_RUN, uptimeMillis);
            if (!this.mRuntimeRestart) {
                MetricsLogger.histogram((Context) null, "boot_system_server_init", uptimeMillis);
            }
            SystemProperties.set("persist.sys.dalvik.vm.lib.2", VMRuntime.getRuntime().vmLibrary());
            VMRuntime.getRuntime().clearGrowthLimit();
            VMRuntime.getRuntime().setTargetHeapUtilization(0.8f);
            Build.ensureFingerprintProperty();
            Environment.setUserRequired(true);
            BaseBundle.setShouldDefuse(true);
            Parcel.setStackTraceParceling(true);
            BinderInternal.disableBackgroundScheduling(true);
            BinderInternal.setMaxThreads(31);
            Process.setThreadPriority(-2);
            Process.setCanSelfBackground(false);
            Looper.prepareMainLooper();
            Looper.getMainLooper().setSlowLogThresholdMs(SLOW_DISPATCH_THRESHOLD_MS, SLOW_DELIVERY_THRESHOLD_MS);
            System.loadLibrary("android_servers");
            if (Build.IS_DEBUGGABLE) {
                initZygoteChildHeapProfiling();
            }
            performPendingShutdown();
            createSystemContext();
            HwFeatureLoader.SystemServiceFeature.loadFeatureFramework(this.mSystemContext);
            this.mSystemServiceManager = new SystemServiceManager(this.mSystemContext);
            this.mSystemServiceManager.setStartInfo(this.mRuntimeRestart, this.mRuntimeStartElapsedTime, this.mRuntimeStartUptime);
            LocalServices.addService(SystemServiceManager.class, this.mSystemServiceManager);
            SystemServerInitThreadPool.get();
            traceEnd();
            try {
                traceBeginAndSlog("StartServices");
                startBootstrapServices();
                startCoreServices();
                startOtherServices();
                SystemServerInitThreadPool.shutdown();
                Slog.i(TAG, "Finish_StartServices");
                traceEnd();
                StrictMode.initVmDefaults(null);
                if (!this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
                    int uptimeMillis2 = (int) SystemClock.elapsedRealtime();
                    MetricsLogger.histogram((Context) null, "boot_system_server_ready", uptimeMillis2);
                    if (uptimeMillis2 > 60000) {
                        Slog.wtf(SYSTEM_SERVER_TIMING_TAG, "SystemServer init took too long. uptimeMillis=" + uptimeMillis2);
                    }
                }
                LogBufferUtil.closeLogBufferAsNeed(this.mSystemContext);
                if (!VMRuntime.hasBootImageSpaces()) {
                    Slog.wtf(TAG, "Runtime is not running with a boot image!");
                }
                Looper.loop();
                throw new RuntimeException("Main thread loop unexpectedly exited");
            } catch (Throwable th) {
                Slog.i(TAG, "Finish_StartServices");
                traceEnd();
                throw th;
            }
        } catch (Throwable th2) {
            traceEnd();
            throw th2;
        }
    }

    private boolean isFirstBootOrUpgrade() {
        return this.mPackageManagerService.isFirstBoot() || this.mPackageManagerService.isDeviceUpgrading();
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    private void performPendingShutdown() {
        final String reason;
        String shutdownAction = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
        if (shutdownAction != null && shutdownAction.length() > 0) {
            final boolean reboot = shutdownAction.charAt(0) == '1';
            if (shutdownAction.length() > 1) {
                reason = shutdownAction.substring(1, shutdownAction.length());
            } else {
                reason = null;
            }
            if (reason != null && reason.startsWith("recovery-update")) {
                File packageFile = new File(UNCRYPT_PACKAGE_FILE);
                if (packageFile.exists()) {
                    String filename = null;
                    try {
                        filename = FileUtils.readTextFile(packageFile, 0, null);
                    } catch (IOException e) {
                        Slog.e(TAG, "Error reading uncrypt package file", e);
                    }
                    if (filename != null && filename.startsWith("/data") && !new File(BLOCK_MAP_FILE).exists()) {
                        Slog.e(TAG, "Can't find block map file, uncrypt failed or unexpected runtime restart?");
                        return;
                    }
                }
            }
            Message msg = Message.obtain(UiThread.getHandler(), new Runnable() {
                /* class com.android.server.SystemServer.AnonymousClass1 */

                public void run() {
                    synchronized (this) {
                        ShutdownThread.rebootOrShutdown(null, reboot, reason);
                    }
                }
            });
            msg.setAsynchronous(true);
            UiThread.getHandler().sendMessage(msg);
        }
    }

    private void createSystemContext() {
        ActivityThread activityThread = ActivityThread.systemMain();
        this.mSystemContext = activityThread.getSystemContext();
        this.mSystemContext.setTheme(DEFAULT_SYSTEM_THEME);
        activityThread.getSystemUiContext().setTheme(DEFAULT_SYSTEM_THEME);
    }

    /* JADX INFO: finally extract failed */
    private void startBootstrapServices() {
        traceBeginAndSlog("StartWatchdog");
        Watchdog watchdog = Watchdog.getInstance();
        watchdog.start();
        traceEnd();
        if (MAPLE_ENABLE) {
            this.mPrimaryZygotePreload = SystemServerInitThreadPool.get().submit($$Lambda$SystemServer$UyrPns7R814gZEylCbDKhe8It4.INSTANCE, "PrimaryZygotePreload");
        }
        Slog.i(TAG, "Reading configuration...");
        traceBeginAndSlog("ReadingSystemConfig");
        SystemServerInitThreadPool.get().submit($$Lambda$YWiwiKm_Qgqb55C6tTuq_n2JzdY.INSTANCE, "ReadingSystemConfig");
        traceEnd();
        traceBeginAndSlog("StartInstaller");
        this.installer = (Installer) this.mSystemServiceManager.startService(Installer.class);
        traceEnd();
        traceBeginAndSlog("DeviceIdentifiersPolicyService");
        this.mSystemServiceManager.startService(DeviceIdentifiersPolicyService.class);
        traceEnd();
        traceBeginAndSlog("UriGrantsManagerService");
        this.mSystemServiceManager.startService(UriGrantsManagerService.Lifecycle.class);
        traceEnd();
        traceBeginAndSlog("StartActivityManager");
        ActivityTaskManagerService atm = this.mSystemServiceManager.startService(ActivityTaskManagerService.Lifecycle.class).getService();
        this.mActivityManagerService = ActivityManagerService.Lifecycle.startService(this.mSystemServiceManager, atm);
        this.mActivityManagerService.setSystemServiceManager(this.mSystemServiceManager);
        this.mActivityManagerService.setInstaller(this.installer);
        this.mWindowManagerGlobalLock = atm.getGlobalLock();
        traceEnd();
        traceBeginAndSlog("StartPowerManager");
        try {
            this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService("com.android.server.power.HwPowerManagerService");
        } catch (RuntimeException e) {
            Slog.w(TAG, "create HwPowerManagerService failed");
            this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService(PowerManagerService.class);
        }
        traceEnd();
        traceBeginAndSlog("StartThermalManager");
        this.mSystemServiceManager.startService(ThermalManagerService.class);
        traceEnd();
        try {
            Slog.i(TAG, "PG Manager service");
            this.mPGManagerService = PGManagerService.getInstance(this.mSystemContext);
        } catch (Throwable e2) {
            reportWtf("PG Manager service", e2);
        }
        traceBeginAndSlog("InitPowerManagement");
        this.mActivityManagerService.initPowerManagement();
        traceEnd();
        traceBeginAndSlog("StartRecoverySystemService");
        this.mSystemServiceManager.startService(RecoverySystemService.class);
        traceEnd();
        RescueParty.noteBoot(this.mSystemContext);
        traceBeginAndSlog("StartLightsService");
        try {
            this.mSystemServiceManager.startService("com.android.server.lights.HwLightsService");
        } catch (RuntimeException e3) {
            Slog.w(TAG, "create HwLightsService failed");
            this.mSystemServiceManager.startService(LightsService.class);
        }
        traceEnd();
        traceBeginAndSlog("StartSidekickService");
        if (SystemProperties.getBoolean("config.enable_sidekick_graphics", false)) {
            this.mSystemServiceManager.startService(WEAR_SIDEKICK_SERVICE_CLASS);
        }
        traceEnd();
        traceBeginAndSlog("StartDisplayManager");
        this.mDisplayManagerService = (DisplayManagerService) this.mSystemServiceManager.startService(DisplayManagerService.class);
        traceEnd();
        try {
            this.mSystemServiceManager.startService("com.android.server.security.HwSecurityService");
            Slog.i(TAG, "HwSecurityService start success");
        } catch (Exception e4) {
            Slog.e(TAG, "can't start HwSecurityService service");
        }
        traceBeginAndSlog("WaitForDisplay");
        this.mSystemServiceManager.startBootPhase(100);
        traceEnd();
        String cryptState = (String) VoldProperties.decrypt().orElse("");
        boolean z = true;
        if (ENCRYPTING_STATE.equals(cryptState)) {
            Slog.w(TAG, "Detected encryption in progress - only parsing core apps");
            this.mOnlyCore = true;
        } else if (ENCRYPTED_STATE.equals(cryptState)) {
            Slog.w(TAG, "Device encrypted - only parsing core apps");
            this.mOnlyCore = true;
        }
        HwBootCheck.bootSceneEnd(100);
        HwBootFail.setBootStage(HwBootFail.STAGE_FRAMEWORK_JAR_DEXOPT_START);
        HwBootCheck.bootSceneStart(105, 900000);
        if (!this.mRuntimeRestart) {
            MetricsLogger.histogram((Context) null, "boot_package_manager_init_start", (int) SystemClock.elapsedRealtime());
        }
        traceBeginAndSlog("StartPackageManagerService");
        try {
            Watchdog.getInstance().pauseWatchingCurrentThread("packagemanagermain");
            Context context = this.mSystemContext;
            Installer installer2 = this.installer;
            if (this.mFactoryTestMode == 0) {
                z = false;
            }
            this.mPackageManagerService = PackageManagerService.main(context, installer2, z, this.mOnlyCore);
            Watchdog.getInstance().resumeWatchingCurrentThread("packagemanagermain");
            this.mFirstBoot = this.mPackageManagerService.isFirstBoot();
            this.mPackageManager = this.mSystemContext.getPackageManager();
            Slog.i(TAG, "Finish_StartPackageManagerService");
            traceEnd();
            if (!this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
                MetricsLogger.histogram((Context) null, "boot_package_manager_init_ready", (int) SystemClock.elapsedRealtime());
                HwBootCheck.addBootInfo("[bootinfo]\nisFirstBoot: " + this.mFirstBoot + "\nisUpgrade: " + this.mPackageManagerService.isUpgrade());
                HwBootCheck.bootSceneStart(101, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                HwBootFail.setBootStage(HwBootFail.STAGE_FRAMEWORK_JAR_DEXOPT_END);
            }
            HwBootCheck.bootSceneEnd(105);
            if (!this.mOnlyCore && !SystemProperties.getBoolean("config.disable_otadexopt", false)) {
                traceBeginAndSlog("StartOtaDexOptService");
                try {
                    Watchdog.getInstance().pauseWatchingCurrentThread("moveab");
                    OtaDexoptService.main(this.mSystemContext, this.mPackageManagerService);
                } catch (Throwable th) {
                    Watchdog.getInstance().resumeWatchingCurrentThread("moveab");
                    traceEnd();
                    throw th;
                }
                Watchdog.getInstance().resumeWatchingCurrentThread("moveab");
                traceEnd();
            }
            traceBeginAndSlog("StartUserManagerService");
            this.mSystemServiceManager.startService(UserManagerService.LifeCycle.class);
            traceEnd();
            traceBeginAndSlog("InitAttributerCache");
            AttributeCache.init(this.mSystemContext);
            traceEnd();
            traceBeginAndSlog("SetSystemProcess");
            this.mActivityManagerService.setSystemProcess();
            traceEnd();
            traceBeginAndSlog("InitWatchdog");
            watchdog.init(this.mSystemContext, this.mActivityManagerService);
            traceEnd();
            this.mDisplayManagerService.setupSchedulerPolicies();
            traceBeginAndSlog("StartOverlayManagerService");
            this.mSystemServiceManager.startService(new OverlayManagerService(this.mSystemContext, this.installer));
            traceEnd();
            traceBeginAndSlog("StartSensorPrivacyService");
            this.mSystemServiceManager.startService(new SensorPrivacyService(this.mSystemContext));
            traceEnd();
            if (SystemProperties.getInt("persist.sys.displayinset.top", 0) > 0) {
                this.mActivityManagerService.updateSystemUiContext();
                ((DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class)).onOverlayChanged();
            }
            this.mSensorServiceStart = SystemServerInitThreadPool.get().submit($$Lambda$SystemServer$oG4I04QJrkzCGs6IcMTKU2211A.INSTANCE, START_SENSOR_SERVICE);
        } catch (Throwable th2) {
            Watchdog.getInstance().resumeWatchingCurrentThread("packagemanagermain");
            throw th2;
        }
    }

    static /* synthetic */ void lambda$startBootstrapServices$0() {
        try {
            Slog.i(TAG, "PrimaryZygotePreload");
            TimingsTraceLog traceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288);
            traceLog.traceBegin("PrimaryZygotePreload");
            if (!Process.ZYGOTE_PROCESS.preloadDefault(Build.SUPPORTED_64_BIT_ABIS[0])) {
                Slog.e(TAG, "Unable to preload primary zygote default resources");
            }
            Slog.e(TAG, "primary zygote preload default resources");
            traceLog.traceEnd();
        } catch (Exception ex) {
            Slog.e(TAG, "Exception preloading primary default resources", ex);
        }
    }

    static /* synthetic */ void lambda$startBootstrapServices$1() {
        TimingsTraceLog traceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288);
        traceLog.traceBegin(START_SENSOR_SERVICE);
        startSensorService();
        traceLog.traceEnd();
    }

    private void startCoreServices() {
        traceBeginAndSlog("StartBatteryService");
        try {
            this.mSystemServiceManager.startService("com.android.server.HwBatteryService");
        } catch (RuntimeException e) {
            Slog.w(TAG, "create HwBatteryService failed");
            this.mSystemServiceManager.startService(BatteryService.class);
        }
        traceEnd();
        traceBeginAndSlog("StartUsageService");
        this.mSystemServiceManager.startService(UsageStatsService.class);
        this.mActivityManagerService.setUsageStatsManager((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        traceEnd();
        if (this.mPackageManager.hasSystemFeature("android.software.webview")) {
            traceBeginAndSlog("StartWebViewUpdateService");
            this.mWebViewUpdateService = (WebViewUpdateService) this.mSystemServiceManager.startService(WebViewUpdateService.class);
            traceEnd();
        }
        traceBeginAndSlog("StartCachedDeviceStateService");
        this.mSystemServiceManager.startService(CachedDeviceStateService.class);
        traceEnd();
        traceBeginAndSlog("StartBinderCallsStatsService");
        this.mSystemServiceManager.startService(BinderCallsStatsService.LifeCycle.class);
        traceEnd();
        traceBeginAndSlog("StartLooperStatsService");
        this.mSystemServiceManager.startService(LooperStatsService.Lifecycle.class);
        traceEnd();
        traceBeginAndSlog("StartRollbackManagerService");
        this.mSystemServiceManager.startService(RollbackManagerService.class);
        traceEnd();
        traceBeginAndSlog("StartBugreportManagerService");
        this.mSystemServiceManager.startService(BugreportManagerService.class);
        traceEnd();
        traceBeginAndSlog(GpuService.TAG);
        this.mSystemServiceManager.startService(GpuService.class);
        traceEnd();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v204, types: [com.android.server.GraphicsStatsService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r3v44, types: [android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v308, types: [com.android.server.HardwarePropertiesManagerService] */
    /* JADX WARN: Type inference failed for: r0v314, types: [com.android.server.SerialService] */
    /* JADX WARN: Type inference failed for: r1v72, types: [android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r12v10, types: [android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r12v14, types: [com.android.server.TrustedUIService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r1v77, types: [com.android.server.UpdateLockService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r1v79, types: [com.android.server.SystemUpdateManagerService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v365, types: [com.android.server.NsdService] */
    /* JADX WARN: Type inference failed for: r1v95, types: [android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v418, types: [com.android.server.statusbar.StatusBarManagerService] */
    /* JADX WARN: Type inference failed for: r6v31, types: [com.android.server.security.KeyAttestationApplicationIdProviderService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r6v33, types: [com.android.server.os.SchedulingPolicyService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r13v6, types: [android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r9v7, types: [android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v500, types: [com.android.server.DynamicSystemService] */
    /* JADX WARN: Type inference failed for: r13v9, types: [com.android.server.input.InputManagerService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v575, types: [com.android.server.ConsumerIrService] */
    /* JADX WARN: Type inference failed for: r13v10 */
    /* JADX WARN: Type inference failed for: r13v11 */
    /* JADX WARN: Type inference failed for: r13v12 */
    /* JADX WARN: Type inference failed for: r12v34 */
    /* JADX WARN: Type inference failed for: r12v35 */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x0527  */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x0543  */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x060e  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x0669  */
    /* JADX WARNING: Removed duplicated region for block: B:257:0x07b9  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x083a  */
    /* JADX WARNING: Removed duplicated region for block: B:288:0x0864  */
    /* JADX WARNING: Removed duplicated region for block: B:291:0x087f  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x089a  */
    /* JADX WARNING: Removed duplicated region for block: B:297:0x08b5  */
    /* JADX WARNING: Removed duplicated region for block: B:300:0x08d0  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x09cc  */
    /* JADX WARNING: Removed duplicated region for block: B:351:0x09e9 A[Catch:{ all -> 0x0a00 }] */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x09ee A[Catch:{ all -> 0x0a00 }] */
    /* JADX WARNING: Removed duplicated region for block: B:383:0x0a52  */
    /* JADX WARNING: Removed duplicated region for block: B:391:0x0a76  */
    /* JADX WARNING: Removed duplicated region for block: B:394:0x0a8c  */
    /* JADX WARNING: Removed duplicated region for block: B:395:0x0a98  */
    /* JADX WARNING: Removed duplicated region for block: B:412:0x0ae8  */
    /* JADX WARNING: Removed duplicated region for block: B:415:0x0b08  */
    /* JADX WARNING: Removed duplicated region for block: B:423:0x0b39  */
    /* JADX WARNING: Removed duplicated region for block: B:436:0x0b83  */
    /* JADX WARNING: Removed duplicated region for block: B:460:0x0c1b  */
    /* JADX WARNING: Removed duplicated region for block: B:468:0x0c82  */
    /* JADX WARNING: Removed duplicated region for block: B:481:0x0ceb  */
    /* JADX WARNING: Removed duplicated region for block: B:487:0x0d01  */
    /* JADX WARNING: Removed duplicated region for block: B:490:0x0d14  */
    /* JADX WARNING: Removed duplicated region for block: B:519:0x0dc3  */
    /* JADX WARNING: Removed duplicated region for block: B:522:0x0ddf  */
    /* JADX WARNING: Removed duplicated region for block: B:525:0x0df8  */
    /* JADX WARNING: Removed duplicated region for block: B:528:0x0e2f  */
    /* JADX WARNING: Removed duplicated region for block: B:536:0x0e6b  */
    /* JADX WARNING: Removed duplicated region for block: B:539:0x0e84  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0ecf  */
    /* JADX WARNING: Removed duplicated region for block: B:553:0x0ee1  */
    /* JADX WARNING: Removed duplicated region for block: B:555:0x0ee5  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0ef6  */
    /* JADX WARNING: Removed duplicated region for block: B:581:0x0f75  */
    /* JADX WARNING: Removed duplicated region for block: B:588:0x0f8c  */
    /* JADX WARNING: Removed duplicated region for block: B:595:0x0fe3  */
    /* JADX WARNING: Removed duplicated region for block: B:597:0x1005  */
    /* JADX WARNING: Removed duplicated region for block: B:599:0x1016  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1074  */
    /* JADX WARNING: Removed duplicated region for block: B:606:0x1085  */
    /* JADX WARNING: Removed duplicated region for block: B:609:0x10a0  */
    /* JADX WARNING: Removed duplicated region for block: B:612:0x10cf  */
    /* JADX WARNING: Removed duplicated region for block: B:615:0x10f2  */
    /* JADX WARNING: Removed duplicated region for block: B:618:0x1111  */
    /* JADX WARNING: Removed duplicated region for block: B:625:0x1138  */
    /* JADX WARNING: Removed duplicated region for block: B:633:0x1166 A[SYNTHETIC, Splitter:B:633:0x1166] */
    /* JADX WARNING: Removed duplicated region for block: B:644:0x11ab  */
    /* JADX WARNING: Removed duplicated region for block: B:647:0x11db  */
    /* JADX WARNING: Removed duplicated region for block: B:665:0x1257  */
    /* JADX WARNING: Unknown variable types count: 19 */
    private void startOtherServices() {
        boolean z;
        VibratorService vibrator;
        String str;
        TelephonyRegistry telephonyRegistry;
        IpSecService ipSecService;
        String str2;
        boolean tuiEnable;
        String str3;
        InputManagerService inputManager;
        String str4;
        WindowManagerService wm;
        Object obj;
        boolean safeMode;
        ILockSettings lockSettings;
        INetworkPolicyManager iNetworkPolicyManager;
        INetworkManagementService iNetworkManagementService;
        MediaRouterService mediaRouter;
        CountryDetectorService countryDetector;
        NetworkTimeUpdateService networkTimeUpdater;
        IConnectivityManager iConnectivityManager;
        INetworkStatsService iNetworkStatsService;
        LocationManagerService location;
        boolean hasFeatureFingerprint;
        Resources.Theme systemTheme;
        IBinder iBinder;
        INetworkManagementService iNetworkManagementService2;
        IpSecService ipSecService2;
        INetworkStatsService iNetworkStatsService2;
        INetworkPolicyManager iNetworkPolicyManager2;
        CountryDetectorService countryDetector2;
        INetworkStatsService iNetworkStatsService3;
        IConnectivityManager iConnectivityManager2;
        IBinder iBinder2;
        INotificationManager notification;
        INotificationManager notification2;
        LocationManagerService location2;
        CountryDetectorService countryDetector3;
        boolean tuiEnable2;
        IBinder iBinder3;
        MediaRouterService mediaRouter2;
        boolean hasFeatureFace;
        boolean hasFeatureIris;
        boolean hasFeatureFingerprint2;
        MediaRouterService mediaRouter3;
        Class<SystemService> serviceClass;
        MediaRouterService mediaRouter4;
        ?? mediaRouterService;
        NetworkTimeUpdateService networkTimeUpdater2;
        IBinder iBinder4;
        IBinder iBinder5;
        CountryDetectorService countryDetector4;
        ?? countryDetectorService;
        LocationManagerService location3;
        HwServiceFactory.IHwLocationManagerService hwLocation;
        ?? r12;
        IBinder iBinder6;
        IpSecService ipSecService3;
        ?? create;
        IStorageManager storageManager;
        VibratorService vibrator2;
        IBinder iBinder7;
        ?? r13;
        ?? vibratorService;
        AlarmManagerService alarmManager;
        ?? r132;
        Context context = this.mSystemContext;
        WindowManagerService wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        wm2 = null;
        IBinder iBinder8 = null;
        NetworkTimeUpdateService networkTimeUpdater3 = null;
        networkTimeUpdater3 = null;
        InputManagerService inputManager2 = null;
        inputManager2 = null;
        inputManager2 = null;
        inputManager2 = null;
        inputManager2 = null;
        inputManager2 = null;
        inputManager2 = null;
        inputManager2 = null;
        inputManager2 = null;
        IBinder iBinder9 = null;
        this.mHwRogEx = HwServiceFactory.getHwRogEx();
        boolean disableSystemTextClassifier = SystemProperties.getBoolean("config.disable_systemtextclassifier", false);
        boolean disableNetworkTime = SystemProperties.getBoolean("config.disable_networktime", false);
        boolean disableCameraService = SystemProperties.getBoolean("config.disable_cameraservice", false);
        boolean disableSlices = SystemProperties.getBoolean("config.disable_slices", false);
        boolean enableLeftyService = SystemProperties.getBoolean("config.enable_lefty", false);
        boolean tuiEnable3 = SystemProperties.getBoolean("ro.vendor.tui.service", false);
        boolean isEmulator = SystemProperties.get("ro.kernel.qemu").equals(ENCRYPTED_STATE);
        boolean enableRms = SystemProperties.getBoolean("ro.config.enable_rms", false);
        boolean enableIaware = SystemProperties.getBoolean("ro.config.enable_iaware", false);
        boolean isChinaArea = "CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", ""));
        boolean isWatch = context.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        boolean isArc = context.getPackageManager().hasSystemFeature("org.chromium.arc");
        boolean enableVrService = context.getPackageManager().hasSystemFeature("android.hardware.vr.high_performance");
        boolean isStartSysSvcCallRecord = "3".equals(SystemProperties.get("ro.logsystem.usertype", "0")) && "true".equals(SystemProperties.get("ro.syssvccallrecord.enable", "false"));
        if (Build.IS_DEBUGGABLE) {
            z = false;
            if (SystemProperties.getBoolean("debug.crash_system", false)) {
                throw new RuntimeException();
            }
        } else {
            z = false;
        }
        try {
            if (MAPLE_ENABLE) {
                try {
                    Slog.d(TAG, "wait primary zygote preload default resources");
                    ConcurrentUtils.waitForFutureNoInterrupt(this.mPrimaryZygotePreload, "Primary Zygote preload");
                    Slog.d(TAG, "primary zygote preload default resources finished");
                    this.mPrimaryZygotePreload = null;
                } catch (RuntimeException e) {
                    e = e;
                    str2 = "attestation Service";
                    str = "false";
                    telephonyRegistry = null;
                    obj = "";
                    ipSecService = null;
                    vibrator2 = null;
                    tuiEnable = tuiEnable3;
                    iBinder7 = null;
                    str3 = "true";
                    str4 = "0";
                }
            }
            try {
                this.mZygotePreload = SystemServerInitThreadPool.get().submit($$Lambda$SystemServer$NlJmG18aPrQduhRqASIdcn7G0z8.INSTANCE, "SecondaryZygotePreload");
                traceBeginAndSlog("StartKeyAttestationApplicationIdProviderService");
                ServiceManager.addService("sec_key_att_app_id_provider", (IBinder) new KeyAttestationApplicationIdProviderService(context));
                traceEnd();
                traceBeginAndSlog("StartKeyChainSystemService");
                this.mSystemServiceManager.startService(KeyChainSystemService.class);
                traceEnd();
                traceBeginAndSlog("StartSchedulingPolicyService");
                ServiceManager.addService("scheduling_policy", (IBinder) new SchedulingPolicyService());
                traceEnd();
                traceBeginAndSlog("StartTelecomLoaderService");
                this.mSystemServiceManager.startService(TelecomLoaderService.class);
                traceEnd();
                traceBeginAndSlog("StartTelephonyRegistry");
                if (HwSystemManager.mPermissionEnabled == 0) {
                    r13 = new TelephonyRegistry(context);
                } else {
                    HwServiceFactory.IHwTelephonyRegistry itr = HwServiceFactory.getHwTelephonyRegistry();
                    if (itr != null) {
                        r13 = itr.getInstance(context);
                    } else {
                        r13 = new TelephonyRegistry(context);
                    }
                }
            } catch (RuntimeException e2) {
                e = e2;
                str2 = "attestation Service";
                str = "false";
                obj = "";
                ipSecService = null;
                tuiEnable = tuiEnable3;
                str3 = "true";
                str4 = "0";
                telephonyRegistry = null;
                vibrator2 = null;
                iBinder7 = null;
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting core service", e);
                inputManager = inputManager2;
                vibrator = vibrator2;
                wm = wm2;
                safeMode = wm.detectSafeMode();
                if (safeMode) {
                }
                lockSettings = null;
                lockSettings = null;
                if (this.mFactoryTestMode != 1) {
                }
                traceBeginAndSlog("MakeDisplayReady");
                wm.displayReady();
                traceEnd();
                traceBeginAndSlog("StartStorageManagerService");
                try {
                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                    storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                } catch (Throwable e3) {
                    reportWtf("starting StorageManagerService", e3);
                    storageManager = null;
                }
                traceEnd();
                traceBeginAndSlog("StartStorageStatsService");
                try {
                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                } catch (Throwable e4) {
                    reportWtf("starting StorageStatsService", e4);
                }
                traceEnd();
                traceBeginAndSlog("StartUiModeManager");
                this.mSystemServiceManager.startService(UiModeManagerService.class);
                traceEnd();
                HwBootCheck.bootSceneEnd(101);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                if (!this.mOnlyCore) {
                }
                traceBeginAndSlog("PerformFstrimIfNeeded");
                this.mPackageManagerService.performFstrimIfNeeded();
                traceEnd();
                HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                if (this.mFactoryTestMode == 1) {
                }
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config = wm.computeNewConfiguration(0);
                DisplayMetrics metrics = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics);
                context.getResources().updateConfiguration(config, metrics);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            try {
                ServiceManager.addService("telephony.registry", (IBinder) r13);
                traceEnd();
                traceBeginAndSlog("StartEntropyMixer");
                this.mEntropyMixer = new EntropyMixer(context);
                traceEnd();
                this.mContentResolver = context.getContentResolver();
                traceBeginAndSlog("StartAccountManagerService");
                this.mSystemServiceManager.startService(ACCOUNT_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartContentService");
                this.mSystemServiceManager.startService(CONTENT_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("InstallSystemProviders");
                this.mActivityManagerService.installSystemProviders();
                SQLiteCompatibilityWalFlags.reset();
                traceEnd();
                traceBeginAndSlog("StartDropBoxManager");
                this.mSystemServiceManager.startService(DropBoxManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartVibratorService");
                vibratorService = new VibratorService(context);
                try {
                    ServiceManager.addService("vibrator", (IBinder) vibratorService);
                    traceEnd();
                    traceBeginAndSlog("StartDynamicSystemService");
                    iBinder7 = new DynamicSystemService(context);
                } catch (RuntimeException e5) {
                    e = e5;
                    str2 = "attestation Service";
                    str = "false";
                    obj = "";
                    telephonyRegistry = r13;
                    ipSecService = null;
                    tuiEnable = tuiEnable3;
                    str3 = "true";
                    str4 = "0";
                    iBinder7 = null;
                    vibrator2 = vibratorService;
                    Slog.e("System", "******************************************");
                    Slog.e("System", "************ Failure starting core service", e);
                    inputManager = inputManager2;
                    vibrator = vibrator2;
                    wm = wm2;
                    safeMode = wm.detectSafeMode();
                    if (safeMode) {
                    }
                    lockSettings = null;
                    lockSettings = null;
                    if (this.mFactoryTestMode != 1) {
                    }
                    traceBeginAndSlog("MakeDisplayReady");
                    wm.displayReady();
                    traceEnd();
                    traceBeginAndSlog("StartStorageManagerService");
                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                    storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                    traceEnd();
                    traceBeginAndSlog("StartStorageStatsService");
                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartUiModeManager");
                    this.mSystemServiceManager.startService(UiModeManagerService.class);
                    traceEnd();
                    HwBootCheck.bootSceneEnd(101);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                    if (!this.mOnlyCore) {
                    }
                    traceBeginAndSlog("PerformFstrimIfNeeded");
                    this.mPackageManagerService.performFstrimIfNeeded();
                    traceEnd();
                    HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                    if (this.mFactoryTestMode == 1) {
                    }
                    if (!isWatch) {
                    }
                    if (isWatch) {
                    }
                    if (!disableSlices) {
                    }
                    if (!disableCameraService) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartIncidentCompanionService");
                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                    traceEnd();
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    MmsServiceBroker mmsService2 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                    }
                    traceBeginAndSlog("StartClipboardService");
                    this.mSystemServiceManager.startService(ClipboardService.class);
                    traceEnd();
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("AppServiceManager");
                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    vibrator.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings != null) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                    this.mSystemServiceManager.startBootPhase(480);
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                    this.mSystemServiceManager.startBootPhase(500);
                    traceEnd();
                    traceBeginAndSlog("MakeWindowManagerServiceReady");
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                    }
                    Configuration config2 = wm.computeNewConfiguration(0);
                    DisplayMetrics metrics2 = new DisplayMetrics();
                    ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2);
                    context.getResources().updateConfiguration(config2, metrics2);
                    systemTheme = context.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("StartPermissionPolicyService");
                    this.mSystemServiceManager.startService(PermissionPolicyService.class);
                    traceEnd();
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    String[] classes2 = this.mSystemContext.getResources().getStringArray(17236007);
                    while (r14 < r7) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2, enableIaware) {
                        /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                        private final /* synthetic */ Context f$1;
                        private final /* synthetic */ CountryDetectorService f$10;
                        private final /* synthetic */ NetworkTimeUpdateService f$11;
                        private final /* synthetic */ InputManagerService f$12;
                        private final /* synthetic */ TelephonyRegistry f$13;
                        private final /* synthetic */ MediaRouterService f$14;
                        private final /* synthetic */ MmsServiceBroker f$15;
                        private final /* synthetic */ boolean f$16;
                        private final /* synthetic */ WindowManagerService f$2;
                        private final /* synthetic */ boolean f$3;
                        private final /* synthetic */ ConnectivityService f$4;
                        private final /* synthetic */ NetworkManagementService f$5;
                        private final /* synthetic */ NetworkPolicyManagerService f$6;
                        private final /* synthetic */ IpSecService f$7;
                        private final /* synthetic */ NetworkStatsService f$8;
                        private final /* synthetic */ LocationManagerService f$9;

                        {
                            this.f$1 = r4;
                            this.f$2 = r5;
                            this.f$3 = r6;
                            this.f$4 = r7;
                            this.f$5 = r8;
                            this.f$6 = r9;
                            this.f$7 = r10;
                            this.f$8 = r11;
                            this.f$9 = r12;
                            this.f$10 = r13;
                            this.f$11 = r14;
                            this.f$12 = r15;
                            this.f$13 = r16;
                            this.f$14 = r17;
                            this.f$15 = r18;
                            this.f$16 = r19;
                        }

                        public final void run() {
                            SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                        }
                    }, BOOT_TIMINGS_TRACE_LOG);
                }
            } catch (RuntimeException e6) {
                e = e6;
                str2 = "attestation Service";
                str = "false";
                obj = "";
                telephonyRegistry = r13;
                ipSecService = null;
                tuiEnable = tuiEnable3;
                str3 = "true";
                str4 = "0";
                vibrator2 = null;
                iBinder7 = null;
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting core service", e);
                inputManager = inputManager2;
                vibrator = vibrator2;
                wm = wm2;
                safeMode = wm.detectSafeMode();
                if (safeMode) {
                }
                lockSettings = null;
                lockSettings = null;
                if (this.mFactoryTestMode != 1) {
                }
                traceBeginAndSlog("MakeDisplayReady");
                wm.displayReady();
                traceEnd();
                traceBeginAndSlog("StartStorageManagerService");
                this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                traceEnd();
                traceBeginAndSlog("StartStorageStatsService");
                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUiModeManager");
                this.mSystemServiceManager.startService(UiModeManagerService.class);
                traceEnd();
                HwBootCheck.bootSceneEnd(101);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                if (!this.mOnlyCore) {
                }
                traceBeginAndSlog("PerformFstrimIfNeeded");
                this.mPackageManagerService.performFstrimIfNeeded();
                traceEnd();
                HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                if (this.mFactoryTestMode == 1) {
                }
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService22 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config22 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics22 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22);
                context.getResources().updateConfiguration(config22, metrics22);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes22 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            try {
                ServiceManager.addService("dynamic_system", iBinder7);
                traceEnd();
                if (!isWatch) {
                    try {
                        traceBeginAndSlog("StartConsumerIrService");
                        iBinder9 = new ConsumerIrService(context);
                        ServiceManager.addService("consumer_ir", iBinder9);
                        traceEnd();
                    } catch (RuntimeException e7) {
                        e = e7;
                        str2 = "attestation Service";
                        str = "false";
                        obj = "";
                        telephonyRegistry = r13;
                        ipSecService = null;
                        tuiEnable = tuiEnable3;
                        str3 = "true";
                        str4 = "0";
                        vibrator2 = vibratorService;
                        Slog.e("System", "******************************************");
                        Slog.e("System", "************ Failure starting core service", e);
                        inputManager = inputManager2;
                        vibrator = vibrator2;
                        wm = wm2;
                        safeMode = wm.detectSafeMode();
                        if (safeMode) {
                        }
                        lockSettings = null;
                        lockSettings = null;
                        if (this.mFactoryTestMode != 1) {
                        }
                        traceBeginAndSlog("MakeDisplayReady");
                        wm.displayReady();
                        traceEnd();
                        traceBeginAndSlog("StartStorageManagerService");
                        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                        storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                        traceEnd();
                        traceBeginAndSlog("StartStorageStatsService");
                        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartUiModeManager");
                        this.mSystemServiceManager.startService(UiModeManagerService.class);
                        traceEnd();
                        HwBootCheck.bootSceneEnd(101);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                        if (!this.mOnlyCore) {
                        }
                        traceBeginAndSlog("PerformFstrimIfNeeded");
                        this.mPackageManagerService.performFstrimIfNeeded();
                        traceEnd();
                        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                        if (this.mFactoryTestMode == 1) {
                        }
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222);
                        context.getResources().updateConfiguration(config222, metrics222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                }
                try {
                    traceBeginAndSlog("StartAlarmManagerService");
                    AlarmManagerService alarmManager2 = HwServiceFactory.getHwAlarmManagerService(context);
                    if (alarmManager2 == null) {
                        try {
                            alarmManager = new AlarmManagerService(context);
                        } catch (RuntimeException e8) {
                            e = e8;
                            str2 = "attestation Service";
                            str = "false";
                            obj = "";
                            telephonyRegistry = r13;
                            ipSecService = null;
                            tuiEnable = tuiEnable3;
                            str3 = "true";
                            str4 = "0";
                            vibrator2 = vibratorService;
                            Slog.e("System", "******************************************");
                            Slog.e("System", "************ Failure starting core service", e);
                            inputManager = inputManager2;
                            vibrator = vibrator2;
                            wm = wm2;
                            safeMode = wm.detectSafeMode();
                            if (safeMode) {
                            }
                            lockSettings = null;
                            lockSettings = null;
                            if (this.mFactoryTestMode != 1) {
                            }
                            traceBeginAndSlog("MakeDisplayReady");
                            wm.displayReady();
                            traceEnd();
                            traceBeginAndSlog("StartStorageManagerService");
                            this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                            storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                            traceEnd();
                            traceBeginAndSlog("StartStorageStatsService");
                            this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                            traceEnd();
                            traceBeginAndSlog("StartUiModeManager");
                            this.mSystemServiceManager.startService(UiModeManagerService.class);
                            traceEnd();
                            HwBootCheck.bootSceneEnd(101);
                            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                            if (!this.mOnlyCore) {
                            }
                            traceBeginAndSlog("PerformFstrimIfNeeded");
                            this.mPackageManagerService.performFstrimIfNeeded();
                            traceEnd();
                            HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                            if (this.mFactoryTestMode == 1) {
                            }
                            if (!isWatch) {
                            }
                            if (isWatch) {
                            }
                            if (!disableSlices) {
                            }
                            if (!disableCameraService) {
                            }
                            if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartIncidentCompanionService");
                            this.mSystemServiceManager.startService(IncidentCompanionService.class);
                            traceEnd();
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            MmsServiceBroker mmsService2222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                            }
                            traceBeginAndSlog("StartClipboardService");
                            this.mSystemServiceManager.startService(ClipboardService.class);
                            traceEnd();
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("AppServiceManager");
                            this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            vibrator.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings != null) {
                            }
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                            this.mSystemServiceManager.startBootPhase(480);
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                            this.mSystemServiceManager.startBootPhase(500);
                            traceEnd();
                            traceBeginAndSlog("MakeWindowManagerServiceReady");
                            wm.systemReady();
                            traceEnd();
                            if (safeMode) {
                            }
                            Configuration config2222 = wm.computeNewConfiguration(0);
                            DisplayMetrics metrics2222 = new DisplayMetrics();
                            ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222);
                            context.getResources().updateConfiguration(config2222, metrics2222);
                            systemTheme = context.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("StartPermissionPolicyService");
                            this.mSystemServiceManager.startService(PermissionPolicyService.class);
                            traceEnd();
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            String[] classes2222 = this.mSystemContext.getResources().getStringArray(17236007);
                            while (r14 < r7) {
                            }
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222, enableIaware) {
                                /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                                private final /* synthetic */ Context f$1;
                                private final /* synthetic */ CountryDetectorService f$10;
                                private final /* synthetic */ NetworkTimeUpdateService f$11;
                                private final /* synthetic */ InputManagerService f$12;
                                private final /* synthetic */ TelephonyRegistry f$13;
                                private final /* synthetic */ MediaRouterService f$14;
                                private final /* synthetic */ MmsServiceBroker f$15;
                                private final /* synthetic */ boolean f$16;
                                private final /* synthetic */ WindowManagerService f$2;
                                private final /* synthetic */ boolean f$3;
                                private final /* synthetic */ ConnectivityService f$4;
                                private final /* synthetic */ NetworkManagementService f$5;
                                private final /* synthetic */ NetworkPolicyManagerService f$6;
                                private final /* synthetic */ IpSecService f$7;
                                private final /* synthetic */ NetworkStatsService f$8;
                                private final /* synthetic */ LocationManagerService f$9;

                                {
                                    this.f$1 = r4;
                                    this.f$2 = r5;
                                    this.f$3 = r6;
                                    this.f$4 = r7;
                                    this.f$5 = r8;
                                    this.f$6 = r9;
                                    this.f$7 = r10;
                                    this.f$8 = r11;
                                    this.f$9 = r12;
                                    this.f$10 = r13;
                                    this.f$11 = r14;
                                    this.f$12 = r15;
                                    this.f$13 = r16;
                                    this.f$14 = r17;
                                    this.f$15 = r18;
                                    this.f$16 = r19;
                                }

                                public final void run() {
                                    SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                                }
                            }, BOOT_TIMINGS_TRACE_LOG);
                        }
                    } else {
                        alarmManager = alarmManager2;
                    }
                    this.mSystemServiceManager.startService(alarmManager);
                    traceEnd();
                    this.mActivityManagerService.setAlarmManager(alarmManager);
                    traceBeginAndSlog("StartInputManagerService");
                } catch (RuntimeException e9) {
                    e = e9;
                    str2 = "attestation Service";
                    str = "false";
                    obj = "";
                    telephonyRegistry = r13;
                    ipSecService = null;
                    tuiEnable = tuiEnable3;
                    str3 = "true";
                    str4 = "0";
                    vibrator2 = vibratorService;
                    Slog.e("System", "******************************************");
                    Slog.e("System", "************ Failure starting core service", e);
                    inputManager = inputManager2;
                    vibrator = vibrator2;
                    wm = wm2;
                    safeMode = wm.detectSafeMode();
                    if (safeMode) {
                    }
                    lockSettings = null;
                    lockSettings = null;
                    if (this.mFactoryTestMode != 1) {
                    }
                    traceBeginAndSlog("MakeDisplayReady");
                    wm.displayReady();
                    traceEnd();
                    traceBeginAndSlog("StartStorageManagerService");
                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                    storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                    traceEnd();
                    traceBeginAndSlog("StartStorageStatsService");
                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartUiModeManager");
                    this.mSystemServiceManager.startService(UiModeManagerService.class);
                    traceEnd();
                    HwBootCheck.bootSceneEnd(101);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                    if (!this.mOnlyCore) {
                    }
                    traceBeginAndSlog("PerformFstrimIfNeeded");
                    this.mPackageManagerService.performFstrimIfNeeded();
                    traceEnd();
                    HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                    if (this.mFactoryTestMode == 1) {
                    }
                    if (!isWatch) {
                    }
                    if (isWatch) {
                    }
                    if (!disableSlices) {
                    }
                    if (!disableCameraService) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartIncidentCompanionService");
                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                    traceEnd();
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    MmsServiceBroker mmsService22222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                    }
                    traceBeginAndSlog("StartClipboardService");
                    this.mSystemServiceManager.startService(ClipboardService.class);
                    traceEnd();
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("AppServiceManager");
                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    vibrator.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings != null) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                    this.mSystemServiceManager.startBootPhase(480);
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                    this.mSystemServiceManager.startBootPhase(500);
                    traceEnd();
                    traceBeginAndSlog("MakeWindowManagerServiceReady");
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                    }
                    Configuration config22222 = wm.computeNewConfiguration(0);
                    DisplayMetrics metrics22222 = new DisplayMetrics();
                    ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222);
                    context.getResources().updateConfiguration(config22222, metrics22222);
                    systemTheme = context.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("StartPermissionPolicyService");
                    this.mSystemServiceManager.startService(PermissionPolicyService.class);
                    traceEnd();
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    String[] classes22222 = this.mSystemContext.getResources().getStringArray(17236007);
                    while (r14 < r7) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222, enableIaware) {
                        /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                        private final /* synthetic */ Context f$1;
                        private final /* synthetic */ CountryDetectorService f$10;
                        private final /* synthetic */ NetworkTimeUpdateService f$11;
                        private final /* synthetic */ InputManagerService f$12;
                        private final /* synthetic */ TelephonyRegistry f$13;
                        private final /* synthetic */ MediaRouterService f$14;
                        private final /* synthetic */ MmsServiceBroker f$15;
                        private final /* synthetic */ boolean f$16;
                        private final /* synthetic */ WindowManagerService f$2;
                        private final /* synthetic */ boolean f$3;
                        private final /* synthetic */ ConnectivityService f$4;
                        private final /* synthetic */ NetworkManagementService f$5;
                        private final /* synthetic */ NetworkPolicyManagerService f$6;
                        private final /* synthetic */ IpSecService f$7;
                        private final /* synthetic */ NetworkStatsService f$8;
                        private final /* synthetic */ LocationManagerService f$9;

                        {
                            this.f$1 = r4;
                            this.f$2 = r5;
                            this.f$3 = r6;
                            this.f$4 = r7;
                            this.f$5 = r8;
                            this.f$6 = r9;
                            this.f$7 = r10;
                            this.f$8 = r11;
                            this.f$9 = r12;
                            this.f$10 = r13;
                            this.f$11 = r14;
                            this.f$12 = r15;
                            this.f$13 = r16;
                            this.f$14 = r17;
                            this.f$15 = r18;
                            this.f$16 = r19;
                        }

                        public final void run() {
                            SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                        }
                    }, BOOT_TIMINGS_TRACE_LOG);
                }
            } catch (RuntimeException e10) {
                e = e10;
                str2 = "attestation Service";
                str = "false";
                obj = "";
                telephonyRegistry = r13;
                ipSecService = null;
                tuiEnable = tuiEnable3;
                str3 = "true";
                str4 = "0";
                vibrator2 = vibratorService;
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting core service", e);
                inputManager = inputManager2;
                vibrator = vibrator2;
                wm = wm2;
                safeMode = wm.detectSafeMode();
                if (safeMode) {
                }
                lockSettings = null;
                lockSettings = null;
                if (this.mFactoryTestMode != 1) {
                }
                traceBeginAndSlog("MakeDisplayReady");
                wm.displayReady();
                traceEnd();
                traceBeginAndSlog("StartStorageManagerService");
                this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                traceEnd();
                traceBeginAndSlog("StartStorageStatsService");
                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUiModeManager");
                this.mSystemServiceManager.startService(UiModeManagerService.class);
                traceEnd();
                HwBootCheck.bootSceneEnd(101);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                if (!this.mOnlyCore) {
                }
                traceBeginAndSlog("PerformFstrimIfNeeded");
                this.mPackageManagerService.performFstrimIfNeeded();
                traceEnd();
                HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                if (this.mFactoryTestMode == 1) {
                }
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222);
                context.getResources().updateConfiguration(config222222, metrics222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            try {
                inputManager2 = HwServiceFactory.getHwInputManagerService().getInstance(context, null);
                if (inputManager2 == null) {
                    try {
                        inputManager2 = new InputManagerService(context);
                    } catch (RuntimeException e11) {
                        e = e11;
                        str2 = "attestation Service";
                        str = "false";
                        obj = "";
                        telephonyRegistry = r13;
                        ipSecService = null;
                        tuiEnable = tuiEnable3;
                        iBinder7 = iBinder7;
                        str3 = "true";
                        str4 = "0";
                        vibrator2 = vibratorService;
                        Slog.e("System", "******************************************");
                        Slog.e("System", "************ Failure starting core service", e);
                        inputManager = inputManager2;
                        vibrator = vibrator2;
                        wm = wm2;
                        safeMode = wm.detectSafeMode();
                        if (safeMode) {
                        }
                        lockSettings = null;
                        lockSettings = null;
                        if (this.mFactoryTestMode != 1) {
                        }
                        traceBeginAndSlog("MakeDisplayReady");
                        wm.displayReady();
                        traceEnd();
                        traceBeginAndSlog("StartStorageManagerService");
                        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                        storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                        traceEnd();
                        traceBeginAndSlog("StartStorageStatsService");
                        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartUiModeManager");
                        this.mSystemServiceManager.startService(UiModeManagerService.class);
                        traceEnd();
                        HwBootCheck.bootSceneEnd(101);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                        if (!this.mOnlyCore) {
                        }
                        traceBeginAndSlog("PerformFstrimIfNeeded");
                        this.mPackageManagerService.performFstrimIfNeeded();
                        traceEnd();
                        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                        if (this.mFactoryTestMode == 1) {
                        }
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService2222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config2222222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics2222222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222);
                        context.getResources().updateConfiguration(config2222222, metrics2222222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes2222222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                }
                try {
                    traceEnd();
                    traceBeginAndSlog("StartHwSysResManagerService");
                    if (enableRms || enableIaware) {
                        try {
                            this.mSystemServiceManager.startService("com.android.server.rms.HwSysResManagerService");
                        } catch (Throwable e12) {
                            Slog.e(TAG, e12.toString());
                        }
                    }
                    traceEnd();
                    traceBeginAndSlog("StartWindowManagerService");
                    ConcurrentUtils.waitForFutureNoInterrupt(this.mSensorServiceStart, START_SENSOR_SERVICE);
                    this.mSensorServiceStart = null;
                    ipSecService = null;
                    telephonyRegistry = r13;
                    str3 = "true";
                    str4 = "0";
                    str = "false";
                    r132 = inputManager2;
                    vibrator = vibratorService;
                    str2 = "attestation Service";
                    obj = "";
                    tuiEnable = tuiEnable3;
                } catch (RuntimeException e13) {
                    e = e13;
                    str2 = "attestation Service";
                    str = "false";
                    obj = "";
                    telephonyRegistry = r13;
                    ipSecService = null;
                    tuiEnable = tuiEnable3;
                    str3 = "true";
                    str4 = "0";
                    iBinder7 = iBinder7;
                    vibrator2 = vibratorService;
                    Slog.e("System", "******************************************");
                    Slog.e("System", "************ Failure starting core service", e);
                    inputManager = inputManager2;
                    vibrator = vibrator2;
                    wm = wm2;
                    safeMode = wm.detectSafeMode();
                    if (safeMode) {
                    }
                    lockSettings = null;
                    lockSettings = null;
                    if (this.mFactoryTestMode != 1) {
                    }
                    traceBeginAndSlog("MakeDisplayReady");
                    wm.displayReady();
                    traceEnd();
                    traceBeginAndSlog("StartStorageManagerService");
                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                    storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                    traceEnd();
                    traceBeginAndSlog("StartStorageStatsService");
                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartUiModeManager");
                    this.mSystemServiceManager.startService(UiModeManagerService.class);
                    traceEnd();
                    HwBootCheck.bootSceneEnd(101);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                    if (!this.mOnlyCore) {
                    }
                    traceBeginAndSlog("PerformFstrimIfNeeded");
                    this.mPackageManagerService.performFstrimIfNeeded();
                    traceEnd();
                    HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                    if (this.mFactoryTestMode == 1) {
                    }
                    if (!isWatch) {
                    }
                    if (isWatch) {
                    }
                    if (!disableSlices) {
                    }
                    if (!disableCameraService) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartIncidentCompanionService");
                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                    traceEnd();
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    MmsServiceBroker mmsService22222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                    }
                    traceBeginAndSlog("StartClipboardService");
                    this.mSystemServiceManager.startService(ClipboardService.class);
                    traceEnd();
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("AppServiceManager");
                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    vibrator.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings != null) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                    this.mSystemServiceManager.startBootPhase(480);
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                    this.mSystemServiceManager.startBootPhase(500);
                    traceEnd();
                    traceBeginAndSlog("MakeWindowManagerServiceReady");
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                    }
                    Configuration config22222222 = wm.computeNewConfiguration(0);
                    DisplayMetrics metrics22222222 = new DisplayMetrics();
                    ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222);
                    context.getResources().updateConfiguration(config22222222, metrics22222222);
                    systemTheme = context.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("StartPermissionPolicyService");
                    this.mSystemServiceManager.startService(PermissionPolicyService.class);
                    traceEnd();
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    String[] classes22222222 = this.mSystemContext.getResources().getStringArray(17236007);
                    while (r14 < r7) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222, enableIaware) {
                        /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                        private final /* synthetic */ Context f$1;
                        private final /* synthetic */ CountryDetectorService f$10;
                        private final /* synthetic */ NetworkTimeUpdateService f$11;
                        private final /* synthetic */ InputManagerService f$12;
                        private final /* synthetic */ TelephonyRegistry f$13;
                        private final /* synthetic */ MediaRouterService f$14;
                        private final /* synthetic */ MmsServiceBroker f$15;
                        private final /* synthetic */ boolean f$16;
                        private final /* synthetic */ WindowManagerService f$2;
                        private final /* synthetic */ boolean f$3;
                        private final /* synthetic */ ConnectivityService f$4;
                        private final /* synthetic */ NetworkManagementService f$5;
                        private final /* synthetic */ NetworkPolicyManagerService f$6;
                        private final /* synthetic */ IpSecService f$7;
                        private final /* synthetic */ NetworkStatsService f$8;
                        private final /* synthetic */ LocationManagerService f$9;

                        {
                            this.f$1 = r4;
                            this.f$2 = r5;
                            this.f$3 = r6;
                            this.f$4 = r7;
                            this.f$5 = r8;
                            this.f$6 = r9;
                            this.f$7 = r10;
                            this.f$8 = r11;
                            this.f$9 = r12;
                            this.f$10 = r13;
                            this.f$11 = r14;
                            this.f$12 = r15;
                            this.f$13 = r16;
                            this.f$14 = r17;
                            this.f$15 = r18;
                            this.f$16 = r19;
                        }

                        public final void run() {
                            SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                        }
                    }, BOOT_TIMINGS_TRACE_LOG);
                }
                try {
                    WindowManagerService wm3 = WindowManagerService.main(context, inputManager2, !this.mFirstBoot, this.mOnlyCore, HwPolicyFactory.getHwPhoneWindowManager(), this.mActivityManagerService.mActivityTaskManager);
                    try {
                        if (this.mHwRogEx != null) {
                            this.mHwRogEx.initRogModeAndProcessMultiDPI(wm3, context);
                        }
                        ServiceManager.addService("window", wm3, false, 17);
                        ServiceManager.addService("input", (IBinder) r132, false, 1);
                        traceEnd();
                        traceBeginAndSlog("SetWindowManagerService");
                        this.mActivityManagerService.setWindowManager(wm3);
                        traceEnd();
                        traceBeginAndSlog("WindowManagerServiceOnInitReady");
                        wm3.onInitReady();
                        traceEnd();
                        SystemServerInitThreadPool.get().submit($$Lambda$SystemServer$JQH6ND0PqyyiRiz7lXLvUmRhwRM.INSTANCE, START_HIDL_SERVICES);
                        if (!isWatch && enableVrService) {
                            traceBeginAndSlog("StartVrManagerService");
                            this.mSystemServiceManager.startService(VrManagerService.class);
                            traceEnd();
                        }
                        traceBeginAndSlog("StartInputManager");
                        r132.setWindowManagerCallbacks(wm3.getInputManagerCallback());
                        r132.start();
                        traceEnd();
                        traceBeginAndSlog("DisplayManagerWindowManagerAndInputReady");
                        this.mDisplayManagerService.windowManagerAndInputReady();
                        traceEnd();
                        if (this.mFactoryTestMode == 1) {
                            Slog.i(TAG, "No Bluetooth Service (factory test)");
                        } else if (!context.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
                            Slog.i(TAG, "No Bluetooth Service (Bluetooth Hardware Not Present)");
                        } else {
                            traceBeginAndSlog("StartBluetoothService");
                            this.mSystemServiceManager.startService(BluetoothService.class);
                            traceEnd();
                        }
                        traceBeginAndSlog("IpConnectivityMetrics");
                        this.mSystemServiceManager.startService(IpConnectivityMetrics.class);
                        traceEnd();
                        traceBeginAndSlog("NetworkWatchlistService");
                        this.mSystemServiceManager.startService(NetworkWatchlistService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("PinnerService");
                        ((PinnerService) this.mSystemServiceManager.startService(PinnerService.class)).setInstaller(this.installer);
                        traceEnd();
                        traceBeginAndSlog("ZrHungService");
                        try {
                            this.mSystemServiceManager.startService("com.android.server.zrhung.ZRHungService");
                        } catch (Throwable th) {
                            Slog.e(TAG, "Fail to begin and Slog ZRHungService");
                        }
                        traceEnd();
                        traceBeginAndSlog("SignedConfigService");
                        SignedConfigService.registerUpdateReceiver(this.mSystemContext);
                        traceEnd();
                        if (!SystemProperties.get("ro.config.hw_fold_disp").isEmpty() || !SystemProperties.get("persist.sys.fold.disp.size").isEmpty()) {
                            this.mSystemServiceManager.startService("com.android.server.fsm.HwFoldScreenManagerService");
                        }
                        wm = wm3;
                        inputManager = r132;
                    } catch (RuntimeException e14) {
                        e = e14;
                        wm2 = wm3;
                        inputManager2 = r132;
                        iBinder7 = iBinder7;
                        vibrator2 = vibrator;
                        Slog.e("System", "******************************************");
                        Slog.e("System", "************ Failure starting core service", e);
                        inputManager = inputManager2;
                        vibrator = vibrator2;
                        wm = wm2;
                        safeMode = wm.detectSafeMode();
                        if (safeMode) {
                        }
                        lockSettings = null;
                        lockSettings = null;
                        if (this.mFactoryTestMode != 1) {
                        }
                        traceBeginAndSlog("MakeDisplayReady");
                        wm.displayReady();
                        traceEnd();
                        traceBeginAndSlog("StartStorageManagerService");
                        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                        storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                        traceEnd();
                        traceBeginAndSlog("StartStorageStatsService");
                        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartUiModeManager");
                        this.mSystemServiceManager.startService(UiModeManagerService.class);
                        traceEnd();
                        HwBootCheck.bootSceneEnd(101);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                        if (!this.mOnlyCore) {
                        }
                        traceBeginAndSlog("PerformFstrimIfNeeded");
                        this.mPackageManagerService.performFstrimIfNeeded();
                        traceEnd();
                        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                        if (this.mFactoryTestMode == 1) {
                        }
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config222222222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics222222222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222);
                        context.getResources().updateConfiguration(config222222222, metrics222222222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                } catch (RuntimeException e15) {
                    e = e15;
                    inputManager2 = r132;
                    iBinder7 = iBinder7;
                    vibrator2 = vibrator;
                    Slog.e("System", "******************************************");
                    Slog.e("System", "************ Failure starting core service", e);
                    inputManager = inputManager2;
                    vibrator = vibrator2;
                    wm = wm2;
                    safeMode = wm.detectSafeMode();
                    if (safeMode) {
                    }
                    lockSettings = null;
                    lockSettings = null;
                    if (this.mFactoryTestMode != 1) {
                    }
                    traceBeginAndSlog("MakeDisplayReady");
                    wm.displayReady();
                    traceEnd();
                    traceBeginAndSlog("StartStorageManagerService");
                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                    storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                    traceEnd();
                    traceBeginAndSlog("StartStorageStatsService");
                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartUiModeManager");
                    this.mSystemServiceManager.startService(UiModeManagerService.class);
                    traceEnd();
                    HwBootCheck.bootSceneEnd(101);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                    if (!this.mOnlyCore) {
                    }
                    traceBeginAndSlog("PerformFstrimIfNeeded");
                    this.mPackageManagerService.performFstrimIfNeeded();
                    traceEnd();
                    HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                    if (this.mFactoryTestMode == 1) {
                    }
                    if (!isWatch) {
                    }
                    if (isWatch) {
                    }
                    if (!disableSlices) {
                    }
                    if (!disableCameraService) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartIncidentCompanionService");
                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                    traceEnd();
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    MmsServiceBroker mmsService2222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                    }
                    traceBeginAndSlog("StartClipboardService");
                    this.mSystemServiceManager.startService(ClipboardService.class);
                    traceEnd();
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("AppServiceManager");
                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    vibrator.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings != null) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                    this.mSystemServiceManager.startBootPhase(480);
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                    this.mSystemServiceManager.startBootPhase(500);
                    traceEnd();
                    traceBeginAndSlog("MakeWindowManagerServiceReady");
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                    }
                    Configuration config2222222222 = wm.computeNewConfiguration(0);
                    DisplayMetrics metrics2222222222 = new DisplayMetrics();
                    ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222);
                    context.getResources().updateConfiguration(config2222222222, metrics2222222222);
                    systemTheme = context.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("StartPermissionPolicyService");
                    this.mSystemServiceManager.startService(PermissionPolicyService.class);
                    traceEnd();
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    String[] classes2222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                    while (r14 < r7) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222, enableIaware) {
                        /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                        private final /* synthetic */ Context f$1;
                        private final /* synthetic */ CountryDetectorService f$10;
                        private final /* synthetic */ NetworkTimeUpdateService f$11;
                        private final /* synthetic */ InputManagerService f$12;
                        private final /* synthetic */ TelephonyRegistry f$13;
                        private final /* synthetic */ MediaRouterService f$14;
                        private final /* synthetic */ MmsServiceBroker f$15;
                        private final /* synthetic */ boolean f$16;
                        private final /* synthetic */ WindowManagerService f$2;
                        private final /* synthetic */ boolean f$3;
                        private final /* synthetic */ ConnectivityService f$4;
                        private final /* synthetic */ NetworkManagementService f$5;
                        private final /* synthetic */ NetworkPolicyManagerService f$6;
                        private final /* synthetic */ IpSecService f$7;
                        private final /* synthetic */ NetworkStatsService f$8;
                        private final /* synthetic */ LocationManagerService f$9;

                        {
                            this.f$1 = r4;
                            this.f$2 = r5;
                            this.f$3 = r6;
                            this.f$4 = r7;
                            this.f$5 = r8;
                            this.f$6 = r9;
                            this.f$7 = r10;
                            this.f$8 = r11;
                            this.f$9 = r12;
                            this.f$10 = r13;
                            this.f$11 = r14;
                            this.f$12 = r15;
                            this.f$13 = r16;
                            this.f$14 = r17;
                            this.f$15 = r18;
                            this.f$16 = r19;
                        }

                        public final void run() {
                            SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                        }
                    }, BOOT_TIMINGS_TRACE_LOG);
                }
            } catch (RuntimeException e16) {
                e = e16;
                str2 = "attestation Service";
                str = "false";
                obj = "";
                telephonyRegistry = r13;
                ipSecService = null;
                tuiEnable = tuiEnable3;
                str3 = "true";
                str4 = "0";
                iBinder7 = iBinder7;
                vibrator2 = vibratorService;
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting core service", e);
                inputManager = inputManager2;
                vibrator = vibrator2;
                wm = wm2;
                safeMode = wm.detectSafeMode();
                if (safeMode) {
                }
                lockSettings = null;
                lockSettings = null;
                if (this.mFactoryTestMode != 1) {
                }
                traceBeginAndSlog("MakeDisplayReady");
                wm.displayReady();
                traceEnd();
                traceBeginAndSlog("StartStorageManagerService");
                this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                traceEnd();
                traceBeginAndSlog("StartStorageStatsService");
                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUiModeManager");
                this.mSystemServiceManager.startService(UiModeManagerService.class);
                traceEnd();
                HwBootCheck.bootSceneEnd(101);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                if (!this.mOnlyCore) {
                }
                traceBeginAndSlog("PerformFstrimIfNeeded");
                this.mPackageManagerService.performFstrimIfNeeded();
                traceEnd();
                HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                if (this.mFactoryTestMode == 1) {
                }
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService22222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config22222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics22222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222);
                context.getResources().updateConfiguration(config22222222222, metrics22222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes22222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
        } catch (RuntimeException e17) {
            e = e17;
            str2 = "attestation Service";
            str = "false";
            obj = "";
            ipSecService = null;
            tuiEnable = tuiEnable3;
            str3 = "true";
            str4 = "0";
            telephonyRegistry = null;
            vibrator2 = null;
            iBinder7 = null;
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting core service", e);
            inputManager = inputManager2;
            vibrator = vibrator2;
            wm = wm2;
            safeMode = wm.detectSafeMode();
            if (safeMode) {
            }
            lockSettings = null;
            lockSettings = null;
            if (this.mFactoryTestMode != 1) {
            }
            traceBeginAndSlog("MakeDisplayReady");
            wm.displayReady();
            traceEnd();
            traceBeginAndSlog("StartStorageManagerService");
            this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
            storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
            traceEnd();
            traceBeginAndSlog("StartStorageStatsService");
            this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
            traceEnd();
            traceBeginAndSlog("StartUiModeManager");
            this.mSystemServiceManager.startService(UiModeManagerService.class);
            traceEnd();
            HwBootCheck.bootSceneEnd(101);
            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
            if (!this.mOnlyCore) {
            }
            traceBeginAndSlog("PerformFstrimIfNeeded");
            this.mPackageManagerService.performFstrimIfNeeded();
            traceEnd();
            HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
            if (this.mFactoryTestMode == 1) {
            }
            if (!isWatch) {
            }
            if (isWatch) {
            }
            if (!disableSlices) {
            }
            if (!disableCameraService) {
            }
            if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
            }
            traceBeginAndSlog("StartStatsCompanionService");
            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
            traceEnd();
            traceBeginAndSlog("StartIncidentCompanionService");
            this.mSystemServiceManager.startService(IncidentCompanionService.class);
            traceEnd();
            if (safeMode) {
            }
            traceBeginAndSlog("StartMmsService");
            MmsServiceBroker mmsService222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
            traceEnd();
            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
            }
            if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
            }
            traceBeginAndSlog("StartClipboardService");
            this.mSystemServiceManager.startService(ClipboardService.class);
            traceEnd();
            if (isStartSysSvcCallRecord) {
            }
            traceBeginAndSlog("AppServiceManager");
            this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
            traceEnd();
            traceBeginAndSlog("MakeVibratorServiceReady");
            vibrator.systemReady();
            traceEnd();
            traceBeginAndSlog("MakeLockSettingsServiceReady");
            if (lockSettings != null) {
            }
            traceEnd();
            traceBeginAndSlog("StartBootPhaseLockSettingsReady");
            this.mSystemServiceManager.startBootPhase(480);
            traceEnd();
            traceBeginAndSlog("StartBootPhaseSystemServicesReady");
            this.mSystemServiceManager.startBootPhase(500);
            traceEnd();
            traceBeginAndSlog("MakeWindowManagerServiceReady");
            wm.systemReady();
            traceEnd();
            if (safeMode) {
            }
            Configuration config222222222222 = wm.computeNewConfiguration(0);
            DisplayMetrics metrics222222222222 = new DisplayMetrics();
            ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222222);
            context.getResources().updateConfiguration(config222222222222, metrics222222222222);
            systemTheme = context.getTheme();
            if (systemTheme.getChangingConfigurations() != 0) {
            }
            traceBeginAndSlog("MakePowerManagerServiceReady");
            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
            traceEnd();
            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
            traceBeginAndSlog("StartPermissionPolicyService");
            this.mSystemServiceManager.startService(PermissionPolicyService.class);
            traceEnd();
            traceBeginAndSlog("MakePackageManagerServiceReady");
            this.mPackageManagerService.systemReady();
            traceEnd();
            traceBeginAndSlog("MakeDisplayManagerServiceReady");
            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
            traceEnd();
            this.mSystemServiceManager.setSafeMode(safeMode);
            traceBeginAndSlog("StartDeviceSpecificServices");
            String[] classes222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
            while (r14 < r7) {
            }
            traceEnd();
            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
            traceEnd();
            this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222222, enableIaware) {
                /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                private final /* synthetic */ Context f$1;
                private final /* synthetic */ CountryDetectorService f$10;
                private final /* synthetic */ NetworkTimeUpdateService f$11;
                private final /* synthetic */ InputManagerService f$12;
                private final /* synthetic */ TelephonyRegistry f$13;
                private final /* synthetic */ MediaRouterService f$14;
                private final /* synthetic */ MmsServiceBroker f$15;
                private final /* synthetic */ boolean f$16;
                private final /* synthetic */ WindowManagerService f$2;
                private final /* synthetic */ boolean f$3;
                private final /* synthetic */ ConnectivityService f$4;
                private final /* synthetic */ NetworkManagementService f$5;
                private final /* synthetic */ NetworkPolicyManagerService f$6;
                private final /* synthetic */ IpSecService f$7;
                private final /* synthetic */ NetworkStatsService f$8;
                private final /* synthetic */ LocationManagerService f$9;

                {
                    this.f$1 = r4;
                    this.f$2 = r5;
                    this.f$3 = r6;
                    this.f$4 = r7;
                    this.f$5 = r8;
                    this.f$6 = r9;
                    this.f$7 = r10;
                    this.f$8 = r11;
                    this.f$9 = r12;
                    this.f$10 = r13;
                    this.f$11 = r14;
                    this.f$12 = r15;
                    this.f$13 = r16;
                    this.f$14 = r17;
                    this.f$15 = r18;
                    this.f$16 = r19;
                }

                public final void run() {
                    SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                }
            }, BOOT_TIMINGS_TRACE_LOG);
        }
        safeMode = wm.detectSafeMode();
        if (safeMode) {
            this.mActivityManagerService.enterSafeMode();
            Settings.Global.putInt(context.getContentResolver(), "airplane_mode_on", 1);
        }
        lockSettings = null;
        lockSettings = null;
        if (this.mFactoryTestMode != 1) {
            traceBeginAndSlog("StartInputMethodManagerLifecycle");
            if (InputMethodSystemProperty.MULTI_CLIENT_IME_ENABLED) {
                this.mSystemServiceManager.startService(MultiClientInputMethodManagerService.Lifecycle.class);
            } else {
                this.mSystemServiceManager.startService(InputMethodManagerService.Lifecycle.class);
            }
            if (isChinaArea) {
                try {
                    Slog.i(TAG, "Secure Input Method Service");
                    this.mSystemServiceManager.startService("com.android.server.inputmethod.HwSecureInputMethodManagerService$MyLifecycle");
                } catch (Throwable e18) {
                    reportWtf("starting Secure Input Manager Service", e18);
                }
            }
            traceEnd();
            traceBeginAndSlog("StartAccessibilityManagerService");
            try {
                this.mSystemServiceManager.startService(ACCESSIBILITY_MANAGER_SERVICE_CLASS);
            } catch (Throwable e19) {
                reportWtf("starting Accessibility Manager", e19);
            }
            traceEnd();
        }
        traceBeginAndSlog("MakeDisplayReady");
        try {
            wm.displayReady();
        } catch (Throwable e20) {
            reportWtf("making display ready", e20);
        }
        traceEnd();
        if (this.mFactoryTestMode != 1 && !str4.equals(SystemProperties.get("system_init.startmountservice"))) {
            traceBeginAndSlog("StartStorageManagerService");
            this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
            storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
            traceEnd();
            traceBeginAndSlog("StartStorageStatsService");
            this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
            traceEnd();
        }
        traceBeginAndSlog("StartUiModeManager");
        this.mSystemServiceManager.startService(UiModeManagerService.class);
        traceEnd();
        HwBootCheck.bootSceneEnd(101);
        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
        if (!this.mOnlyCore) {
            traceBeginAndSlog("UpdatePackagesIfNeeded");
            try {
                Watchdog.getInstance().pauseWatchingCurrentThread("dexopt");
                this.mPackageManagerService.updatePackagesIfNeeded();
            } catch (Throwable th2) {
                Watchdog.getInstance().resumeWatchingCurrentThread("dexopt");
                throw th2;
            }
            Watchdog.getInstance().resumeWatchingCurrentThread("dexopt");
            traceEnd();
        }
        traceBeginAndSlog("PerformFstrimIfNeeded");
        try {
            this.mPackageManagerService.performFstrimIfNeeded();
        } catch (Throwable e21) {
            reportWtf("performing fstrim", e21);
        }
        traceEnd();
        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
        if (this.mFactoryTestMode == 1) {
            traceBeginAndSlog("StartLockSettingsService");
            try {
                this.mSystemServiceManager.startService(LOCK_SETTINGS_SERVICE_CLASS);
                lockSettings = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
            } catch (Throwable e22) {
                reportWtf("starting LockSettingsService service", e22);
            }
            traceEnd();
            boolean hasPdb = !SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals(obj);
            boolean hasGsi = SystemProperties.getInt(GSI_RUNNING_PROP, 0) > 0;
            if (hasPdb && !hasGsi) {
                traceBeginAndSlog("StartPersistentDataBlock");
                this.mSystemServiceManager.startService(PersistentDataBlockService.class);
                traceEnd();
            }
            traceBeginAndSlog("StartTestHarnessMode");
            this.mSystemServiceManager.startService(TestHarnessModeService.class);
            traceEnd();
            if (hasPdb || OemLockService.isHalPresent()) {
                traceBeginAndSlog("StartOemLockService");
                this.mSystemServiceManager.startService(OemLockService.class);
                traceEnd();
            }
            traceBeginAndSlog("StartDeviceIdleController");
            this.mSystemServiceManager.startService(DeviceIdleController.class);
            traceEnd();
            traceBeginAndSlog("StartDevicePolicyManager");
            this.mSystemServiceManager.startService(DevicePolicyManagerService.Lifecycle.class);
            traceEnd();
            if (!isWatch) {
                traceBeginAndSlog("StartStatusBarManagerService");
                try {
                    iBinder = HwServiceFactory.createHwStatusBarManagerService(context, wm);
                    try {
                        ServiceManager.addService("statusbar", iBinder);
                    } catch (Throwable th3) {
                        e = th3;
                    }
                } catch (Throwable th4) {
                    e = th4;
                    iBinder = null;
                    reportWtf("starting StatusBarManagerService", e);
                    traceEnd();
                    startContentCaptureService(context);
                    startAttentionService(context);
                    startSystemCaptionsManagerService(context);
                    traceBeginAndSlog("StartAppPredictionService");
                    this.mSystemServiceManager.startService(APP_PREDICTION_MANAGER_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartContentSuggestionsService");
                    this.mSystemServiceManager.startService(CONTENT_SUGGESTIONS_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("InitNetworkStackClient");
                    NetworkStackClient.getInstance().init();
                    traceEnd();
                    traceBeginAndSlog("StartNetworkManagementService");
                    iNetworkManagementService2 = NetworkManagementService.create(context);
                    try {
                        ServiceManager.addService("network_management", iNetworkManagementService2);
                    } catch (Throwable th5) {
                        e = th5;
                        reportWtf("starting NetworkManagement Service", e);
                        traceEnd();
                        traceBeginAndSlog("StartIpSecService");
                        create = IpSecService.create(context);
                        ServiceManager.addService(INetd.IPSEC_INTERFACE_PREFIX, (IBinder) create);
                        ipSecService2 = create;
                        traceEnd();
                        traceBeginAndSlog("StartTextServicesManager");
                        this.mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
                        traceEnd();
                        if (!disableSystemTextClassifier) {
                        }
                        traceBeginAndSlog("StartNetworkScoreService");
                        this.mSystemServiceManager.startService(NetworkScoreService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartNetworkStatsService");
                        iNetworkStatsService2 = NetworkStatsService.create(context, iNetworkManagementService2);
                        ServiceManager.addService("netstats", iNetworkStatsService2);
                        traceEnd();
                        traceBeginAndSlog("StartNetworkPolicyManagerService");
                        iNetworkPolicyManager2 = HwServiceFactory.getHwNetworkPolicyManagerService().getInstance(context, this.mActivityManagerService, iNetworkManagementService2);
                        ServiceManager.addService("netpolicy", iNetworkPolicyManager2);
                        traceEnd();
                        if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                        }
                        if (!this.mPackageManager.hasSystemFeature("android.hardware.ethernet")) {
                        }
                        traceBeginAndSlog("StartEthernet");
                        countryDetector2 = null;
                        this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartConnectivityService");
                        iConnectivityManager2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context, iNetworkManagementService2, iNetworkStatsService2, iNetworkPolicyManager2);
                        iNetworkStatsService3 = iNetworkStatsService2;
                        iNetworkManagementService = iNetworkManagementService2;
                        ServiceManager.addService("connectivity", iConnectivityManager2, false, 6);
                        iNetworkPolicyManager2.bindConnectivityManager(iConnectivityManager2);
                        traceEnd();
                        traceBeginAndSlog("StartNsdService");
                        iBinder6 = NsdService.create(context);
                        ServiceManager.addService("servicediscovery", iBinder6);
                        iBinder2 = iBinder6;
                        traceEnd();
                        traceBeginAndSlog("StartSystemUpdateManagerService");
                        ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                        traceEnd();
                        traceBeginAndSlog("StartUpdateLockService");
                        ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                        traceEnd();
                        traceBeginAndSlog("StartNotificationManager");
                        this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                        SystemNotificationChannels.removeDeprecated(context);
                        SystemNotificationChannels.createAll(context);
                        notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                        traceEnd();
                        traceBeginAndSlog("StartDeviceMonitor");
                        this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                        traceEnd();
                        Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                        if (tuiEnable) {
                        }
                        traceBeginAndSlog("StartLocationManagerService");
                        hwLocation = HwServiceFactory.getHwLocationManagerService();
                        if (hwLocation != null) {
                        }
                        ServiceManager.addService("location", (IBinder) r12);
                        notification2 = notification;
                        location2 = r12;
                        traceEnd();
                        traceBeginAndSlog("StartCountryDetectorService");
                        countryDetectorService = new CountryDetectorService(context);
                        ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                        countryDetector3 = countryDetectorService;
                        traceEnd();
                        traceBeginAndSlog("StartTimeDetectorService");
                        countryDetector = countryDetector3;
                        this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (context.getResources().getBoolean(17891450)) {
                        }
                        traceBeginAndSlog("StartAudioService");
                        if (!isArc) {
                        }
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                        }
                        traceBeginAndSlog("StartDockObserver");
                        this.mSystemServiceManager.startService(DockObserver.class);
                        traceEnd();
                        if (isWatch) {
                        }
                        traceBeginAndSlog("StartWiredAccessoryManager");
                        inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                        }
                        traceBeginAndSlog("StartAdbService");
                        this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartUsbService");
                        this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                        traceEnd();
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartHardwarePropertiesManagerService");
                        iBinder4 = new HardwarePropertiesManagerService(context);
                        ServiceManager.addService("hardware_properties", iBinder4);
                        iBinder3 = iBinder4;
                        traceEnd();
                        traceBeginAndSlog("StartTwilightService");
                        this.mSystemServiceManager.startService(TwilightService.class);
                        traceEnd();
                        traceBeginAndSlog("StartColorDisplay");
                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                        traceEnd();
                        traceBeginAndSlog("StartJobScheduler");
                        this.mSystemServiceManager.startService(JobSchedulerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartSoundTrigger");
                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartTrustManager");
                        this.mSystemServiceManager.startService(TrustManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                        }
                        traceBeginAndSlog("StartAppWidgetService");
                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartRoleManagerService");
                        SystemServiceManager systemServiceManager = this.mSystemServiceManager;
                        Context context2 = this.mSystemContext;
                        systemServiceManager.startService(new RoleManagerService(context2, new LegacyRoleResolutionPolicy(context2)));
                        traceEnd();
                        traceBeginAndSlog("StartVoiceRecognitionManager");
                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                        traceEnd();
                        if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                        }
                        traceBeginAndSlog("StartSensorNotification");
                        this.mSystemServiceManager.startService(SensorNotificationService.class);
                        traceEnd();
                        traceBeginAndSlog("StartContextHubSystemService");
                        this.mSystemServiceManager.startService(ContextHubSystemService.class);
                        traceEnd();
                        HwServiceFactory.setupHwServices(context);
                        traceBeginAndSlog("StartDiskStatsService");
                        ServiceManager.addService("diskstats", new DiskStatsService(context));
                        traceEnd();
                        traceBeginAndSlog("RuntimeService");
                        ServiceManager.addService("runtime", new RuntimeService(context));
                        traceEnd();
                        if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                        }
                        if (SUPPORT_HW_ATTESTATION) {
                        }
                        traceBeginAndSlog("StartNetworkTimeUpdateService");
                        networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                        try {
                            Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                            ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                            networkTimeUpdater3 = networkTimeUpdater2;
                        } catch (Throwable th6) {
                            e = th6;
                            reportWtf("starting NetworkTimeUpdate service", e);
                            networkTimeUpdater3 = networkTimeUpdater2;
                            traceEnd();
                            traceBeginAndSlog("CertBlacklister");
                            new CertBlacklister(context);
                            traceEnd();
                            traceBeginAndSlog("StartEmergencyAffordanceService");
                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDreamManager");
                            this.mSystemServiceManager.startService(DreamManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("AddGraphicsStatsService");
                            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                            traceEnd();
                            if (CoverageService.ENABLED) {
                            }
                            if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                            }
                            if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                            }
                            traceBeginAndSlog("StartRestrictionManager");
                            this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("StartMediaSessionService");
                            this.mSystemServiceManager.startService(MediaSessionService.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                            }
                            traceBeginAndSlog("StartTvInputManager");
                            this.mSystemServiceManager.startService(TvInputManagerService.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                            }
                            if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                            }
                            traceBeginAndSlog("StartMediaRouterService");
                            mediaRouterService = new MediaRouterService(context);
                            ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                            mediaRouter2 = mediaRouterService;
                            traceEnd();
                            hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                            hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                            hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                            if (hasFeatureFace) {
                            }
                            if (hasFeatureIris) {
                            }
                            if (hasFeatureFingerprint2) {
                            }
                            traceBeginAndSlog("StartBiometricService");
                            this.mSystemServiceManager.startService(BiometricService.class);
                            traceEnd();
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context);
                            traceEnd();
                            if (!isWatch) {
                            }
                            if (!isWatch) {
                            }
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            location = location2;
                            mediaRouter = mediaRouter3;
                            hasFeatureFingerprint = tuiEnable2;
                            networkTimeUpdater = networkTimeUpdater3;
                            ipSecService = ipSecService2;
                            iNetworkStatsService = iNetworkStatsService3;
                            iConnectivityManager = iConnectivityManager2;
                            if (!isWatch) {
                            }
                            if (isWatch) {
                            }
                            if (!disableSlices) {
                            }
                            if (!disableCameraService) {
                            }
                            if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartIncidentCompanionService");
                            this.mSystemServiceManager.startService(IncidentCompanionService.class);
                            traceEnd();
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            MmsServiceBroker mmsService2222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                            }
                            traceBeginAndSlog("StartClipboardService");
                            this.mSystemServiceManager.startService(ClipboardService.class);
                            traceEnd();
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("AppServiceManager");
                            this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            vibrator.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings != null) {
                            }
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                            this.mSystemServiceManager.startBootPhase(480);
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                            this.mSystemServiceManager.startBootPhase(500);
                            traceEnd();
                            traceBeginAndSlog("MakeWindowManagerServiceReady");
                            wm.systemReady();
                            traceEnd();
                            if (safeMode) {
                            }
                            Configuration config2222222222222 = wm.computeNewConfiguration(0);
                            DisplayMetrics metrics2222222222222 = new DisplayMetrics();
                            ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222222);
                            context.getResources().updateConfiguration(config2222222222222, metrics2222222222222);
                            systemTheme = context.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("StartPermissionPolicyService");
                            this.mSystemServiceManager.startService(PermissionPolicyService.class);
                            traceEnd();
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            String[] classes2222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                            while (r14 < r7) {
                            }
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222222, enableIaware) {
                                /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                                private final /* synthetic */ Context f$1;
                                private final /* synthetic */ CountryDetectorService f$10;
                                private final /* synthetic */ NetworkTimeUpdateService f$11;
                                private final /* synthetic */ InputManagerService f$12;
                                private final /* synthetic */ TelephonyRegistry f$13;
                                private final /* synthetic */ MediaRouterService f$14;
                                private final /* synthetic */ MmsServiceBroker f$15;
                                private final /* synthetic */ boolean f$16;
                                private final /* synthetic */ WindowManagerService f$2;
                                private final /* synthetic */ boolean f$3;
                                private final /* synthetic */ ConnectivityService f$4;
                                private final /* synthetic */ NetworkManagementService f$5;
                                private final /* synthetic */ NetworkPolicyManagerService f$6;
                                private final /* synthetic */ IpSecService f$7;
                                private final /* synthetic */ NetworkStatsService f$8;
                                private final /* synthetic */ LocationManagerService f$9;

                                {
                                    this.f$1 = r4;
                                    this.f$2 = r5;
                                    this.f$3 = r6;
                                    this.f$4 = r7;
                                    this.f$5 = r8;
                                    this.f$6 = r9;
                                    this.f$7 = r10;
                                    this.f$8 = r11;
                                    this.f$9 = r12;
                                    this.f$10 = r13;
                                    this.f$11 = r14;
                                    this.f$12 = r15;
                                    this.f$13 = r16;
                                    this.f$14 = r17;
                                    this.f$15 = r18;
                                    this.f$16 = r19;
                                }

                                public final void run() {
                                    SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                                }
                            }, BOOT_TIMINGS_TRACE_LOG);
                        }
                        traceEnd();
                        traceBeginAndSlog("CertBlacklister");
                        new CertBlacklister(context);
                        traceEnd();
                        traceBeginAndSlog("StartEmergencyAffordanceService");
                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                        traceEnd();
                        traceBeginAndSlog("StartDreamManager");
                        this.mSystemServiceManager.startService(DreamManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("AddGraphicsStatsService");
                        ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                        traceEnd();
                        if (CoverageService.ENABLED) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                        }
                        traceBeginAndSlog("StartRestrictionManager");
                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartMediaSessionService");
                        this.mSystemServiceManager.startService(MediaSessionService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                        }
                        traceBeginAndSlog("StartTvInputManager");
                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        }
                        traceBeginAndSlog("StartMediaRouterService");
                        mediaRouterService = new MediaRouterService(context);
                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                        mediaRouter2 = mediaRouterService;
                        traceEnd();
                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                        hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                        if (hasFeatureFace) {
                        }
                        if (hasFeatureIris) {
                        }
                        if (hasFeatureFingerprint2) {
                        }
                        traceBeginAndSlog("StartBiometricService");
                        this.mSystemServiceManager.startService(BiometricService.class);
                        traceEnd();
                        traceBeginAndSlog("StartBackgroundDexOptService");
                        BackgroundDexOptService.schedule(context);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartLauncherAppsService");
                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                        traceEnd();
                        traceBeginAndSlog("StartCrossProfileAppsService");
                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                        traceEnd();
                        location = location2;
                        mediaRouter = mediaRouter3;
                        hasFeatureFingerprint = tuiEnable2;
                        networkTimeUpdater = networkTimeUpdater3;
                        ipSecService = ipSecService2;
                        iNetworkStatsService = iNetworkStatsService3;
                        iConnectivityManager = iConnectivityManager2;
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService22222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config22222222222222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics22222222222222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222222);
                        context.getResources().updateConfiguration(config22222222222222, metrics22222222222222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes22222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                    traceEnd();
                    traceBeginAndSlog("StartIpSecService");
                    create = IpSecService.create(context);
                    try {
                        ServiceManager.addService(INetd.IPSEC_INTERFACE_PREFIX, (IBinder) create);
                        ipSecService2 = create;
                    } catch (Throwable th7) {
                        e = th7;
                        ipSecService3 = create;
                    }
                    traceEnd();
                    traceBeginAndSlog("StartTextServicesManager");
                    this.mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
                    traceEnd();
                    if (!disableSystemTextClassifier) {
                    }
                    traceBeginAndSlog("StartNetworkScoreService");
                    this.mSystemServiceManager.startService(NetworkScoreService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartNetworkStatsService");
                    iNetworkStatsService2 = NetworkStatsService.create(context, iNetworkManagementService2);
                    try {
                        ServiceManager.addService("netstats", iNetworkStatsService2);
                    } catch (Throwable th8) {
                        e = th8;
                    }
                    traceEnd();
                    traceBeginAndSlog("StartNetworkPolicyManagerService");
                    iNetworkPolicyManager2 = HwServiceFactory.getHwNetworkPolicyManagerService().getInstance(context, this.mActivityManagerService, iNetworkManagementService2);
                    try {
                        ServiceManager.addService("netpolicy", iNetworkPolicyManager2);
                    } catch (Throwable th9) {
                        e = th9;
                    }
                    traceEnd();
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                    }
                    if (!this.mPackageManager.hasSystemFeature("android.hardware.ethernet")) {
                    }
                    traceBeginAndSlog("StartEthernet");
                    countryDetector2 = null;
                    this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartConnectivityService");
                    iConnectivityManager2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context, iNetworkManagementService2, iNetworkStatsService2, iNetworkPolicyManager2);
                    iNetworkStatsService3 = iNetworkStatsService2;
                    iNetworkManagementService = iNetworkManagementService2;
                    ServiceManager.addService("connectivity", iConnectivityManager2, false, 6);
                    iNetworkPolicyManager2.bindConnectivityManager(iConnectivityManager2);
                    traceEnd();
                    traceBeginAndSlog("StartNsdService");
                    iBinder6 = NsdService.create(context);
                    try {
                        ServiceManager.addService("servicediscovery", iBinder6);
                        iBinder2 = iBinder6;
                    } catch (Throwable th10) {
                        e = th10;
                        reportWtf("starting Service Discovery Service", e);
                        iBinder2 = iBinder6;
                        traceEnd();
                        traceBeginAndSlog("StartSystemUpdateManagerService");
                        ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                        traceEnd();
                        traceBeginAndSlog("StartUpdateLockService");
                        ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                        traceEnd();
                        traceBeginAndSlog("StartNotificationManager");
                        this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                        SystemNotificationChannels.removeDeprecated(context);
                        SystemNotificationChannels.createAll(context);
                        notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                        traceEnd();
                        traceBeginAndSlog("StartDeviceMonitor");
                        this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                        traceEnd();
                        Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                        if (tuiEnable) {
                        }
                        traceBeginAndSlog("StartLocationManagerService");
                        hwLocation = HwServiceFactory.getHwLocationManagerService();
                        if (hwLocation != null) {
                        }
                        ServiceManager.addService("location", (IBinder) r12);
                        notification2 = notification;
                        location2 = r12;
                        traceEnd();
                        traceBeginAndSlog("StartCountryDetectorService");
                        countryDetectorService = new CountryDetectorService(context);
                        ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                        countryDetector3 = countryDetectorService;
                        traceEnd();
                        traceBeginAndSlog("StartTimeDetectorService");
                        countryDetector = countryDetector3;
                        this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (context.getResources().getBoolean(17891450)) {
                        }
                        traceBeginAndSlog("StartAudioService");
                        if (!isArc) {
                        }
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                        }
                        traceBeginAndSlog("StartDockObserver");
                        this.mSystemServiceManager.startService(DockObserver.class);
                        traceEnd();
                        if (isWatch) {
                        }
                        traceBeginAndSlog("StartWiredAccessoryManager");
                        inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                        }
                        traceBeginAndSlog("StartAdbService");
                        this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartUsbService");
                        this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                        traceEnd();
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartHardwarePropertiesManagerService");
                        iBinder4 = new HardwarePropertiesManagerService(context);
                        ServiceManager.addService("hardware_properties", iBinder4);
                        iBinder3 = iBinder4;
                        traceEnd();
                        traceBeginAndSlog("StartTwilightService");
                        this.mSystemServiceManager.startService(TwilightService.class);
                        traceEnd();
                        traceBeginAndSlog("StartColorDisplay");
                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                        traceEnd();
                        traceBeginAndSlog("StartJobScheduler");
                        this.mSystemServiceManager.startService(JobSchedulerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartSoundTrigger");
                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartTrustManager");
                        this.mSystemServiceManager.startService(TrustManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                        }
                        traceBeginAndSlog("StartAppWidgetService");
                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartRoleManagerService");
                        SystemServiceManager systemServiceManager2 = this.mSystemServiceManager;
                        Context context22 = this.mSystemContext;
                        systemServiceManager2.startService(new RoleManagerService(context22, new LegacyRoleResolutionPolicy(context22)));
                        traceEnd();
                        traceBeginAndSlog("StartVoiceRecognitionManager");
                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                        traceEnd();
                        if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                        }
                        traceBeginAndSlog("StartSensorNotification");
                        this.mSystemServiceManager.startService(SensorNotificationService.class);
                        traceEnd();
                        traceBeginAndSlog("StartContextHubSystemService");
                        this.mSystemServiceManager.startService(ContextHubSystemService.class);
                        traceEnd();
                        HwServiceFactory.setupHwServices(context);
                        traceBeginAndSlog("StartDiskStatsService");
                        ServiceManager.addService("diskstats", new DiskStatsService(context));
                        traceEnd();
                        traceBeginAndSlog("RuntimeService");
                        ServiceManager.addService("runtime", new RuntimeService(context));
                        traceEnd();
                        if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                        }
                        if (SUPPORT_HW_ATTESTATION) {
                        }
                        traceBeginAndSlog("StartNetworkTimeUpdateService");
                        networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                        Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                        ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                        networkTimeUpdater3 = networkTimeUpdater2;
                        traceEnd();
                        traceBeginAndSlog("CertBlacklister");
                        new CertBlacklister(context);
                        traceEnd();
                        traceBeginAndSlog("StartEmergencyAffordanceService");
                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                        traceEnd();
                        traceBeginAndSlog("StartDreamManager");
                        this.mSystemServiceManager.startService(DreamManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("AddGraphicsStatsService");
                        ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                        traceEnd();
                        if (CoverageService.ENABLED) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                        }
                        traceBeginAndSlog("StartRestrictionManager");
                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartMediaSessionService");
                        this.mSystemServiceManager.startService(MediaSessionService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                        }
                        traceBeginAndSlog("StartTvInputManager");
                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        }
                        traceBeginAndSlog("StartMediaRouterService");
                        mediaRouterService = new MediaRouterService(context);
                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                        mediaRouter2 = mediaRouterService;
                        traceEnd();
                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                        hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                        if (hasFeatureFace) {
                        }
                        if (hasFeatureIris) {
                        }
                        if (hasFeatureFingerprint2) {
                        }
                        traceBeginAndSlog("StartBiometricService");
                        this.mSystemServiceManager.startService(BiometricService.class);
                        traceEnd();
                        traceBeginAndSlog("StartBackgroundDexOptService");
                        BackgroundDexOptService.schedule(context);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartLauncherAppsService");
                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                        traceEnd();
                        traceBeginAndSlog("StartCrossProfileAppsService");
                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                        traceEnd();
                        location = location2;
                        mediaRouter = mediaRouter3;
                        hasFeatureFingerprint = tuiEnable2;
                        networkTimeUpdater = networkTimeUpdater3;
                        ipSecService = ipSecService2;
                        iNetworkStatsService = iNetworkStatsService3;
                        iConnectivityManager = iConnectivityManager2;
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config222222222222222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics222222222222222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222222222);
                        context.getResources().updateConfiguration(config222222222222222, metrics222222222222222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222222222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                    traceEnd();
                    traceBeginAndSlog("StartSystemUpdateManagerService");
                    ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                    traceEnd();
                    traceBeginAndSlog("StartUpdateLockService");
                    ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                    traceEnd();
                    traceBeginAndSlog("StartNotificationManager");
                    this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                    SystemNotificationChannels.removeDeprecated(context);
                    SystemNotificationChannels.createAll(context);
                    notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                    traceEnd();
                    traceBeginAndSlog("StartDeviceMonitor");
                    this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                    traceEnd();
                    Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                    if (tuiEnable) {
                    }
                    traceBeginAndSlog("StartLocationManagerService");
                    hwLocation = HwServiceFactory.getHwLocationManagerService();
                    if (hwLocation != null) {
                    }
                    try {
                        ServiceManager.addService("location", (IBinder) r12);
                        notification2 = notification;
                        location2 = r12;
                    } catch (Throwable th11) {
                        e = th11;
                        location3 = r12;
                        notification2 = notification;
                        reportWtf("starting Location Manager", e);
                        location2 = location3;
                        traceEnd();
                        traceBeginAndSlog("StartCountryDetectorService");
                        countryDetectorService = new CountryDetectorService(context);
                        ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                        countryDetector3 = countryDetectorService;
                        traceEnd();
                        traceBeginAndSlog("StartTimeDetectorService");
                        countryDetector = countryDetector3;
                        this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (context.getResources().getBoolean(17891450)) {
                        }
                        traceBeginAndSlog("StartAudioService");
                        if (!isArc) {
                        }
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                        }
                        traceBeginAndSlog("StartDockObserver");
                        this.mSystemServiceManager.startService(DockObserver.class);
                        traceEnd();
                        if (isWatch) {
                        }
                        traceBeginAndSlog("StartWiredAccessoryManager");
                        inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                        }
                        traceBeginAndSlog("StartAdbService");
                        this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartUsbService");
                        this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                        traceEnd();
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartHardwarePropertiesManagerService");
                        iBinder4 = new HardwarePropertiesManagerService(context);
                        ServiceManager.addService("hardware_properties", iBinder4);
                        iBinder3 = iBinder4;
                        traceEnd();
                        traceBeginAndSlog("StartTwilightService");
                        this.mSystemServiceManager.startService(TwilightService.class);
                        traceEnd();
                        traceBeginAndSlog("StartColorDisplay");
                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                        traceEnd();
                        traceBeginAndSlog("StartJobScheduler");
                        this.mSystemServiceManager.startService(JobSchedulerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartSoundTrigger");
                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartTrustManager");
                        this.mSystemServiceManager.startService(TrustManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                        }
                        traceBeginAndSlog("StartAppWidgetService");
                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartRoleManagerService");
                        SystemServiceManager systemServiceManager22 = this.mSystemServiceManager;
                        Context context222 = this.mSystemContext;
                        systemServiceManager22.startService(new RoleManagerService(context222, new LegacyRoleResolutionPolicy(context222)));
                        traceEnd();
                        traceBeginAndSlog("StartVoiceRecognitionManager");
                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                        traceEnd();
                        if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                        }
                        traceBeginAndSlog("StartSensorNotification");
                        this.mSystemServiceManager.startService(SensorNotificationService.class);
                        traceEnd();
                        traceBeginAndSlog("StartContextHubSystemService");
                        this.mSystemServiceManager.startService(ContextHubSystemService.class);
                        traceEnd();
                        HwServiceFactory.setupHwServices(context);
                        traceBeginAndSlog("StartDiskStatsService");
                        ServiceManager.addService("diskstats", new DiskStatsService(context));
                        traceEnd();
                        traceBeginAndSlog("RuntimeService");
                        ServiceManager.addService("runtime", new RuntimeService(context));
                        traceEnd();
                        if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                        }
                        if (SUPPORT_HW_ATTESTATION) {
                        }
                        traceBeginAndSlog("StartNetworkTimeUpdateService");
                        networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                        Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                        ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                        networkTimeUpdater3 = networkTimeUpdater2;
                        traceEnd();
                        traceBeginAndSlog("CertBlacklister");
                        new CertBlacklister(context);
                        traceEnd();
                        traceBeginAndSlog("StartEmergencyAffordanceService");
                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                        traceEnd();
                        traceBeginAndSlog("StartDreamManager");
                        this.mSystemServiceManager.startService(DreamManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("AddGraphicsStatsService");
                        ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                        traceEnd();
                        if (CoverageService.ENABLED) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                        }
                        traceBeginAndSlog("StartRestrictionManager");
                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartMediaSessionService");
                        this.mSystemServiceManager.startService(MediaSessionService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                        }
                        traceBeginAndSlog("StartTvInputManager");
                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        }
                        traceBeginAndSlog("StartMediaRouterService");
                        mediaRouterService = new MediaRouterService(context);
                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                        mediaRouter2 = mediaRouterService;
                        traceEnd();
                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                        hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                        if (hasFeatureFace) {
                        }
                        if (hasFeatureIris) {
                        }
                        if (hasFeatureFingerprint2) {
                        }
                        traceBeginAndSlog("StartBiometricService");
                        this.mSystemServiceManager.startService(BiometricService.class);
                        traceEnd();
                        traceBeginAndSlog("StartBackgroundDexOptService");
                        BackgroundDexOptService.schedule(context);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartLauncherAppsService");
                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                        traceEnd();
                        traceBeginAndSlog("StartCrossProfileAppsService");
                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                        traceEnd();
                        location = location2;
                        mediaRouter = mediaRouter3;
                        hasFeatureFingerprint = tuiEnable2;
                        networkTimeUpdater = networkTimeUpdater3;
                        ipSecService = ipSecService2;
                        iNetworkStatsService = iNetworkStatsService3;
                        iConnectivityManager = iConnectivityManager2;
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService2222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config2222222222222222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics2222222222222222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222222222);
                        context.getResources().updateConfiguration(config2222222222222222, metrics2222222222222222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes2222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222222222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                    traceEnd();
                    traceBeginAndSlog("StartCountryDetectorService");
                    countryDetectorService = new CountryDetectorService(context);
                    try {
                        ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                        countryDetector3 = countryDetectorService;
                    } catch (Throwable th12) {
                        e = th12;
                        countryDetector4 = countryDetectorService;
                    }
                    traceEnd();
                    traceBeginAndSlog("StartTimeDetectorService");
                    countryDetector = countryDetector3;
                    try {
                        this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                    } catch (Throwable th13) {
                        e = th13;
                    }
                    traceEnd();
                    if (!isWatch) {
                    }
                    if (context.getResources().getBoolean(17891450)) {
                    }
                    traceBeginAndSlog("StartAudioService");
                    if (!isArc) {
                    }
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                    }
                    traceBeginAndSlog("StartDockObserver");
                    this.mSystemServiceManager.startService(DockObserver.class);
                    traceEnd();
                    if (isWatch) {
                    }
                    traceBeginAndSlog("StartWiredAccessoryManager");
                    inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                    }
                    traceBeginAndSlog("StartAdbService");
                    this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartUsbService");
                    this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                    traceEnd();
                    if (!isWatch) {
                    }
                    traceBeginAndSlog("StartHardwarePropertiesManagerService");
                    iBinder4 = new HardwarePropertiesManagerService(context);
                    try {
                        ServiceManager.addService("hardware_properties", iBinder4);
                        iBinder3 = iBinder4;
                    } catch (Throwable th14) {
                        e = th14;
                        Slog.e(TAG, "Failure starting HardwarePropertiesManagerService", e);
                        iBinder3 = iBinder4;
                        traceEnd();
                        traceBeginAndSlog("StartTwilightService");
                        this.mSystemServiceManager.startService(TwilightService.class);
                        traceEnd();
                        traceBeginAndSlog("StartColorDisplay");
                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                        traceEnd();
                        traceBeginAndSlog("StartJobScheduler");
                        this.mSystemServiceManager.startService(JobSchedulerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartSoundTrigger");
                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartTrustManager");
                        this.mSystemServiceManager.startService(TrustManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                        }
                        traceBeginAndSlog("StartAppWidgetService");
                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartRoleManagerService");
                        SystemServiceManager systemServiceManager222 = this.mSystemServiceManager;
                        Context context2222 = this.mSystemContext;
                        systemServiceManager222.startService(new RoleManagerService(context2222, new LegacyRoleResolutionPolicy(context2222)));
                        traceEnd();
                        traceBeginAndSlog("StartVoiceRecognitionManager");
                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                        traceEnd();
                        if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                        }
                        traceBeginAndSlog("StartSensorNotification");
                        this.mSystemServiceManager.startService(SensorNotificationService.class);
                        traceEnd();
                        traceBeginAndSlog("StartContextHubSystemService");
                        this.mSystemServiceManager.startService(ContextHubSystemService.class);
                        traceEnd();
                        HwServiceFactory.setupHwServices(context);
                        traceBeginAndSlog("StartDiskStatsService");
                        ServiceManager.addService("diskstats", new DiskStatsService(context));
                        traceEnd();
                        traceBeginAndSlog("RuntimeService");
                        ServiceManager.addService("runtime", new RuntimeService(context));
                        traceEnd();
                        if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                        }
                        if (SUPPORT_HW_ATTESTATION) {
                        }
                        traceBeginAndSlog("StartNetworkTimeUpdateService");
                        networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                        Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                        ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                        networkTimeUpdater3 = networkTimeUpdater2;
                        traceEnd();
                        traceBeginAndSlog("CertBlacklister");
                        new CertBlacklister(context);
                        traceEnd();
                        traceBeginAndSlog("StartEmergencyAffordanceService");
                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                        traceEnd();
                        traceBeginAndSlog("StartDreamManager");
                        this.mSystemServiceManager.startService(DreamManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("AddGraphicsStatsService");
                        ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                        traceEnd();
                        if (CoverageService.ENABLED) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                        }
                        traceBeginAndSlog("StartRestrictionManager");
                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartMediaSessionService");
                        this.mSystemServiceManager.startService(MediaSessionService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                        }
                        traceBeginAndSlog("StartTvInputManager");
                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        }
                        traceBeginAndSlog("StartMediaRouterService");
                        mediaRouterService = new MediaRouterService(context);
                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                        mediaRouter2 = mediaRouterService;
                        traceEnd();
                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                        hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                        if (hasFeatureFace) {
                        }
                        if (hasFeatureIris) {
                        }
                        if (hasFeatureFingerprint2) {
                        }
                        traceBeginAndSlog("StartBiometricService");
                        this.mSystemServiceManager.startService(BiometricService.class);
                        traceEnd();
                        traceBeginAndSlog("StartBackgroundDexOptService");
                        BackgroundDexOptService.schedule(context);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartLauncherAppsService");
                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                        traceEnd();
                        traceBeginAndSlog("StartCrossProfileAppsService");
                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                        traceEnd();
                        location = location2;
                        mediaRouter = mediaRouter3;
                        hasFeatureFingerprint = tuiEnable2;
                        networkTimeUpdater = networkTimeUpdater3;
                        ipSecService = ipSecService2;
                        iNetworkStatsService = iNetworkStatsService3;
                        iConnectivityManager = iConnectivityManager2;
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService22222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config22222222222222222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics22222222222222222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222222222);
                        context.getResources().updateConfiguration(config22222222222222222, metrics22222222222222222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes22222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222222222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                    traceEnd();
                    traceBeginAndSlog("StartTwilightService");
                    this.mSystemServiceManager.startService(TwilightService.class);
                    traceEnd();
                    traceBeginAndSlog("StartColorDisplay");
                    this.mSystemServiceManager.startService(ColorDisplayService.class);
                    traceEnd();
                    traceBeginAndSlog("StartJobScheduler");
                    this.mSystemServiceManager.startService(JobSchedulerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartSoundTrigger");
                    this.mSystemServiceManager.startService(SoundTriggerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartTrustManager");
                    this.mSystemServiceManager.startService(TrustManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                    }
                    traceBeginAndSlog("StartAppWidgetService");
                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartRoleManagerService");
                    SystemServiceManager systemServiceManager2222 = this.mSystemServiceManager;
                    Context context22222 = this.mSystemContext;
                    systemServiceManager2222.startService(new RoleManagerService(context22222, new LegacyRoleResolutionPolicy(context22222)));
                    traceEnd();
                    traceBeginAndSlog("StartVoiceRecognitionManager");
                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                    traceEnd();
                    if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                    }
                    traceBeginAndSlog("StartSensorNotification");
                    this.mSystemServiceManager.startService(SensorNotificationService.class);
                    traceEnd();
                    traceBeginAndSlog("StartContextHubSystemService");
                    this.mSystemServiceManager.startService(ContextHubSystemService.class);
                    traceEnd();
                    HwServiceFactory.setupHwServices(context);
                    traceBeginAndSlog("StartDiskStatsService");
                    ServiceManager.addService("diskstats", new DiskStatsService(context));
                    traceEnd();
                    traceBeginAndSlog("RuntimeService");
                    ServiceManager.addService("runtime", new RuntimeService(context));
                    traceEnd();
                    if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                    }
                    if (SUPPORT_HW_ATTESTATION) {
                    }
                    traceBeginAndSlog("StartNetworkTimeUpdateService");
                    try {
                        networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                        Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                        ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                        networkTimeUpdater3 = networkTimeUpdater2;
                    } catch (Throwable th15) {
                        e = th15;
                        networkTimeUpdater2 = null;
                        reportWtf("starting NetworkTimeUpdate service", e);
                        networkTimeUpdater3 = networkTimeUpdater2;
                        traceEnd();
                        traceBeginAndSlog("CertBlacklister");
                        new CertBlacklister(context);
                        traceEnd();
                        traceBeginAndSlog("StartEmergencyAffordanceService");
                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                        traceEnd();
                        traceBeginAndSlog("StartDreamManager");
                        this.mSystemServiceManager.startService(DreamManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("AddGraphicsStatsService");
                        ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                        traceEnd();
                        if (CoverageService.ENABLED) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                        }
                        traceBeginAndSlog("StartRestrictionManager");
                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartMediaSessionService");
                        this.mSystemServiceManager.startService(MediaSessionService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                        }
                        traceBeginAndSlog("StartTvInputManager");
                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        }
                        traceBeginAndSlog("StartMediaRouterService");
                        mediaRouterService = new MediaRouterService(context);
                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                        mediaRouter2 = mediaRouterService;
                        traceEnd();
                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                        hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                        if (hasFeatureFace) {
                        }
                        if (hasFeatureIris) {
                        }
                        if (hasFeatureFingerprint2) {
                        }
                        traceBeginAndSlog("StartBiometricService");
                        this.mSystemServiceManager.startService(BiometricService.class);
                        traceEnd();
                        traceBeginAndSlog("StartBackgroundDexOptService");
                        BackgroundDexOptService.schedule(context);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartLauncherAppsService");
                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                        traceEnd();
                        traceBeginAndSlog("StartCrossProfileAppsService");
                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                        traceEnd();
                        location = location2;
                        mediaRouter = mediaRouter3;
                        hasFeatureFingerprint = tuiEnable2;
                        networkTimeUpdater = networkTimeUpdater3;
                        ipSecService = ipSecService2;
                        iNetworkStatsService = iNetworkStatsService3;
                        iConnectivityManager = iConnectivityManager2;
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config222222222222222222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics222222222222222222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222222222222);
                        context.getResources().updateConfiguration(config222222222222222222, metrics222222222222222222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222222222222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                    traceEnd();
                    traceBeginAndSlog("CertBlacklister");
                    new CertBlacklister(context);
                    traceEnd();
                    traceBeginAndSlog("StartEmergencyAffordanceService");
                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                    traceEnd();
                    traceBeginAndSlog("StartDreamManager");
                    this.mSystemServiceManager.startService(DreamManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("AddGraphicsStatsService");
                    ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                    traceEnd();
                    if (CoverageService.ENABLED) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                    }
                    traceBeginAndSlog("StartRestrictionManager");
                    this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartMediaSessionService");
                    this.mSystemServiceManager.startService(MediaSessionService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                    }
                    traceBeginAndSlog("StartTvInputManager");
                    this.mSystemServiceManager.startService(TvInputManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                    }
                    traceBeginAndSlog("StartMediaRouterService");
                    mediaRouterService = new MediaRouterService(context);
                    try {
                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                        mediaRouter2 = mediaRouterService;
                    } catch (Throwable th16) {
                        e = th16;
                        mediaRouter4 = mediaRouterService;
                    }
                    traceEnd();
                    hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                    hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                    hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                    if (hasFeatureFace) {
                    }
                    if (hasFeatureIris) {
                    }
                    if (hasFeatureFingerprint2) {
                    }
                    traceBeginAndSlog("StartBiometricService");
                    this.mSystemServiceManager.startService(BiometricService.class);
                    traceEnd();
                    traceBeginAndSlog("StartBackgroundDexOptService");
                    BackgroundDexOptService.schedule(context);
                    traceEnd();
                    if (!isWatch) {
                    }
                    if (!isWatch) {
                    }
                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartLauncherAppsService");
                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                    traceEnd();
                    traceBeginAndSlog("StartCrossProfileAppsService");
                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                    traceEnd();
                    location = location2;
                    mediaRouter = mediaRouter3;
                    hasFeatureFingerprint = tuiEnable2;
                    networkTimeUpdater = networkTimeUpdater3;
                    ipSecService = ipSecService2;
                    iNetworkStatsService = iNetworkStatsService3;
                    iConnectivityManager = iConnectivityManager2;
                    if (!isWatch) {
                    }
                    if (isWatch) {
                    }
                    if (!disableSlices) {
                    }
                    if (!disableCameraService) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartIncidentCompanionService");
                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                    traceEnd();
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    MmsServiceBroker mmsService2222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                    }
                    traceBeginAndSlog("StartClipboardService");
                    this.mSystemServiceManager.startService(ClipboardService.class);
                    traceEnd();
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("AppServiceManager");
                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    vibrator.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings != null) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                    this.mSystemServiceManager.startBootPhase(480);
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                    this.mSystemServiceManager.startBootPhase(500);
                    traceEnd();
                    traceBeginAndSlog("MakeWindowManagerServiceReady");
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                    }
                    Configuration config2222222222222222222 = wm.computeNewConfiguration(0);
                    DisplayMetrics metrics2222222222222222222 = new DisplayMetrics();
                    ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222222222222);
                    context.getResources().updateConfiguration(config2222222222222222222, metrics2222222222222222222);
                    systemTheme = context.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("StartPermissionPolicyService");
                    this.mSystemServiceManager.startService(PermissionPolicyService.class);
                    traceEnd();
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    String[] classes2222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                    while (r14 < r7) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222222222222, enableIaware) {
                        /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                        private final /* synthetic */ Context f$1;
                        private final /* synthetic */ CountryDetectorService f$10;
                        private final /* synthetic */ NetworkTimeUpdateService f$11;
                        private final /* synthetic */ InputManagerService f$12;
                        private final /* synthetic */ TelephonyRegistry f$13;
                        private final /* synthetic */ MediaRouterService f$14;
                        private final /* synthetic */ MmsServiceBroker f$15;
                        private final /* synthetic */ boolean f$16;
                        private final /* synthetic */ WindowManagerService f$2;
                        private final /* synthetic */ boolean f$3;
                        private final /* synthetic */ ConnectivityService f$4;
                        private final /* synthetic */ NetworkManagementService f$5;
                        private final /* synthetic */ NetworkPolicyManagerService f$6;
                        private final /* synthetic */ IpSecService f$7;
                        private final /* synthetic */ NetworkStatsService f$8;
                        private final /* synthetic */ LocationManagerService f$9;

                        {
                            this.f$1 = r4;
                            this.f$2 = r5;
                            this.f$3 = r6;
                            this.f$4 = r7;
                            this.f$5 = r8;
                            this.f$6 = r9;
                            this.f$7 = r10;
                            this.f$8 = r11;
                            this.f$9 = r12;
                            this.f$10 = r13;
                            this.f$11 = r14;
                            this.f$12 = r15;
                            this.f$13 = r16;
                            this.f$14 = r17;
                            this.f$15 = r18;
                            this.f$16 = r19;
                        }

                        public final void run() {
                            SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                        }
                    }, BOOT_TIMINGS_TRACE_LOG);
                }
                traceEnd();
            } else {
                iBinder = null;
            }
            startContentCaptureService(context);
            startAttentionService(context);
            startSystemCaptionsManagerService(context);
            traceBeginAndSlog("StartAppPredictionService");
            this.mSystemServiceManager.startService(APP_PREDICTION_MANAGER_SERVICE_CLASS);
            traceEnd();
            traceBeginAndSlog("StartContentSuggestionsService");
            this.mSystemServiceManager.startService(CONTENT_SUGGESTIONS_SERVICE_CLASS);
            traceEnd();
            traceBeginAndSlog("InitNetworkStackClient");
            try {
                NetworkStackClient.getInstance().init();
            } catch (Throwable e23) {
                reportWtf("initializing NetworkStackClient", e23);
            }
            traceEnd();
            traceBeginAndSlog("StartNetworkManagementService");
            try {
                iNetworkManagementService2 = NetworkManagementService.create(context);
                ServiceManager.addService("network_management", iNetworkManagementService2);
            } catch (Throwable th17) {
                e = th17;
                iNetworkManagementService2 = null;
                reportWtf("starting NetworkManagement Service", e);
                traceEnd();
                traceBeginAndSlog("StartIpSecService");
                create = IpSecService.create(context);
                ServiceManager.addService(INetd.IPSEC_INTERFACE_PREFIX, (IBinder) create);
                ipSecService2 = create;
                traceEnd();
                traceBeginAndSlog("StartTextServicesManager");
                this.mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
                traceEnd();
                if (!disableSystemTextClassifier) {
                }
                traceBeginAndSlog("StartNetworkScoreService");
                this.mSystemServiceManager.startService(NetworkScoreService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartNetworkStatsService");
                iNetworkStatsService2 = NetworkStatsService.create(context, iNetworkManagementService2);
                ServiceManager.addService("netstats", iNetworkStatsService2);
                traceEnd();
                traceBeginAndSlog("StartNetworkPolicyManagerService");
                iNetworkPolicyManager2 = HwServiceFactory.getHwNetworkPolicyManagerService().getInstance(context, this.mActivityManagerService, iNetworkManagementService2);
                ServiceManager.addService("netpolicy", iNetworkPolicyManager2);
                traceEnd();
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                }
                if (!this.mPackageManager.hasSystemFeature("android.hardware.ethernet")) {
                }
                traceBeginAndSlog("StartEthernet");
                countryDetector2 = null;
                this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartConnectivityService");
                iConnectivityManager2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context, iNetworkManagementService2, iNetworkStatsService2, iNetworkPolicyManager2);
                iNetworkStatsService3 = iNetworkStatsService2;
                iNetworkManagementService = iNetworkManagementService2;
                ServiceManager.addService("connectivity", iConnectivityManager2, false, 6);
                iNetworkPolicyManager2.bindConnectivityManager(iConnectivityManager2);
                traceEnd();
                traceBeginAndSlog("StartNsdService");
                iBinder6 = NsdService.create(context);
                ServiceManager.addService("servicediscovery", iBinder6);
                iBinder2 = iBinder6;
                traceEnd();
                traceBeginAndSlog("StartSystemUpdateManagerService");
                ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                traceEnd();
                traceBeginAndSlog("StartUpdateLockService");
                ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                traceEnd();
                traceBeginAndSlog("StartNotificationManager");
                this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                SystemNotificationChannels.removeDeprecated(context);
                SystemNotificationChannels.createAll(context);
                notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                traceEnd();
                traceBeginAndSlog("StartDeviceMonitor");
                this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                traceEnd();
                Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                if (tuiEnable) {
                }
                traceBeginAndSlog("StartLocationManagerService");
                hwLocation = HwServiceFactory.getHwLocationManagerService();
                if (hwLocation != null) {
                }
                ServiceManager.addService("location", (IBinder) r12);
                notification2 = notification;
                location2 = r12;
                traceEnd();
                traceBeginAndSlog("StartCountryDetectorService");
                countryDetectorService = new CountryDetectorService(context);
                ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                countryDetector3 = countryDetectorService;
                traceEnd();
                traceBeginAndSlog("StartTimeDetectorService");
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager22222 = this.mSystemServiceManager;
                Context context222222 = this.mSystemContext;
                systemServiceManager22222.startService(new RoleManagerService(context222222, new LegacyRoleResolutionPolicy(context222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService22222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config22222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics22222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222222222222);
                context.getResources().updateConfiguration(config22222222222222222222, metrics22222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes22222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            traceBeginAndSlog("StartIpSecService");
            try {
                create = IpSecService.create(context);
                ServiceManager.addService(INetd.IPSEC_INTERFACE_PREFIX, (IBinder) create);
                ipSecService2 = create;
            } catch (Throwable th18) {
                e = th18;
                ipSecService3 = ipSecService;
                reportWtf("starting IpSec Service", e);
                ipSecService2 = ipSecService3;
                traceEnd();
                traceBeginAndSlog("StartTextServicesManager");
                this.mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
                traceEnd();
                if (!disableSystemTextClassifier) {
                }
                traceBeginAndSlog("StartNetworkScoreService");
                this.mSystemServiceManager.startService(NetworkScoreService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartNetworkStatsService");
                iNetworkStatsService2 = NetworkStatsService.create(context, iNetworkManagementService2);
                ServiceManager.addService("netstats", iNetworkStatsService2);
                traceEnd();
                traceBeginAndSlog("StartNetworkPolicyManagerService");
                iNetworkPolicyManager2 = HwServiceFactory.getHwNetworkPolicyManagerService().getInstance(context, this.mActivityManagerService, iNetworkManagementService2);
                ServiceManager.addService("netpolicy", iNetworkPolicyManager2);
                traceEnd();
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                }
                if (!this.mPackageManager.hasSystemFeature("android.hardware.ethernet")) {
                }
                traceBeginAndSlog("StartEthernet");
                countryDetector2 = null;
                this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartConnectivityService");
                iConnectivityManager2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context, iNetworkManagementService2, iNetworkStatsService2, iNetworkPolicyManager2);
                iNetworkStatsService3 = iNetworkStatsService2;
                iNetworkManagementService = iNetworkManagementService2;
                ServiceManager.addService("connectivity", iConnectivityManager2, false, 6);
                iNetworkPolicyManager2.bindConnectivityManager(iConnectivityManager2);
                traceEnd();
                traceBeginAndSlog("StartNsdService");
                iBinder6 = NsdService.create(context);
                ServiceManager.addService("servicediscovery", iBinder6);
                iBinder2 = iBinder6;
                traceEnd();
                traceBeginAndSlog("StartSystemUpdateManagerService");
                ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                traceEnd();
                traceBeginAndSlog("StartUpdateLockService");
                ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                traceEnd();
                traceBeginAndSlog("StartNotificationManager");
                this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                SystemNotificationChannels.removeDeprecated(context);
                SystemNotificationChannels.createAll(context);
                notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                traceEnd();
                traceBeginAndSlog("StartDeviceMonitor");
                this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                traceEnd();
                Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                if (tuiEnable) {
                }
                traceBeginAndSlog("StartLocationManagerService");
                hwLocation = HwServiceFactory.getHwLocationManagerService();
                if (hwLocation != null) {
                }
                ServiceManager.addService("location", (IBinder) r12);
                notification2 = notification;
                location2 = r12;
                traceEnd();
                traceBeginAndSlog("StartCountryDetectorService");
                countryDetectorService = new CountryDetectorService(context);
                ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                countryDetector3 = countryDetectorService;
                traceEnd();
                traceBeginAndSlog("StartTimeDetectorService");
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager222222 = this.mSystemServiceManager;
                Context context2222222 = this.mSystemContext;
                systemServiceManager222222.startService(new RoleManagerService(context2222222, new LegacyRoleResolutionPolicy(context2222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222222222222222);
                context.getResources().updateConfiguration(config222222222222222222222, metrics222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            traceBeginAndSlog("StartTextServicesManager");
            this.mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
            traceEnd();
            if (!disableSystemTextClassifier) {
                traceBeginAndSlog("StartTextClassificationManagerService");
                this.mSystemServiceManager.startService(TextClassificationManagerService.Lifecycle.class);
                traceEnd();
            }
            traceBeginAndSlog("StartNetworkScoreService");
            this.mSystemServiceManager.startService(NetworkScoreService.Lifecycle.class);
            traceEnd();
            traceBeginAndSlog("StartNetworkStatsService");
            try {
                iNetworkStatsService2 = NetworkStatsService.create(context, iNetworkManagementService2);
                ServiceManager.addService("netstats", iNetworkStatsService2);
            } catch (Throwable th19) {
                e = th19;
                iNetworkStatsService2 = null;
                reportWtf("starting NetworkStats Service", e);
                iNetworkStatsService2 = iNetworkStatsService2;
                traceEnd();
                traceBeginAndSlog("StartNetworkPolicyManagerService");
                iNetworkPolicyManager2 = HwServiceFactory.getHwNetworkPolicyManagerService().getInstance(context, this.mActivityManagerService, iNetworkManagementService2);
                ServiceManager.addService("netpolicy", iNetworkPolicyManager2);
                traceEnd();
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                }
                if (!this.mPackageManager.hasSystemFeature("android.hardware.ethernet")) {
                }
                traceBeginAndSlog("StartEthernet");
                countryDetector2 = null;
                this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartConnectivityService");
                iConnectivityManager2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context, iNetworkManagementService2, iNetworkStatsService2, iNetworkPolicyManager2);
                iNetworkStatsService3 = iNetworkStatsService2;
                iNetworkManagementService = iNetworkManagementService2;
                ServiceManager.addService("connectivity", iConnectivityManager2, false, 6);
                iNetworkPolicyManager2.bindConnectivityManager(iConnectivityManager2);
                traceEnd();
                traceBeginAndSlog("StartNsdService");
                iBinder6 = NsdService.create(context);
                ServiceManager.addService("servicediscovery", iBinder6);
                iBinder2 = iBinder6;
                traceEnd();
                traceBeginAndSlog("StartSystemUpdateManagerService");
                ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                traceEnd();
                traceBeginAndSlog("StartUpdateLockService");
                ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                traceEnd();
                traceBeginAndSlog("StartNotificationManager");
                this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                SystemNotificationChannels.removeDeprecated(context);
                SystemNotificationChannels.createAll(context);
                notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                traceEnd();
                traceBeginAndSlog("StartDeviceMonitor");
                this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                traceEnd();
                Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                if (tuiEnable) {
                }
                traceBeginAndSlog("StartLocationManagerService");
                hwLocation = HwServiceFactory.getHwLocationManagerService();
                if (hwLocation != null) {
                }
                ServiceManager.addService("location", (IBinder) r12);
                notification2 = notification;
                location2 = r12;
                traceEnd();
                traceBeginAndSlog("StartCountryDetectorService");
                countryDetectorService = new CountryDetectorService(context);
                ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                countryDetector3 = countryDetectorService;
                traceEnd();
                traceBeginAndSlog("StartTimeDetectorService");
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager2222222 = this.mSystemServiceManager;
                Context context22222222 = this.mSystemContext;
                systemServiceManager2222222.startService(new RoleManagerService(context22222222, new LegacyRoleResolutionPolicy(context22222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService2222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config2222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics2222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222222222222222);
                context.getResources().updateConfiguration(config2222222222222222222222, metrics2222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes2222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            traceBeginAndSlog("StartNetworkPolicyManagerService");
            try {
                try {
                    iNetworkPolicyManager2 = HwServiceFactory.getHwNetworkPolicyManagerService().getInstance(context, this.mActivityManagerService, iNetworkManagementService2);
                    ServiceManager.addService("netpolicy", iNetworkPolicyManager2);
                } catch (Throwable th20) {
                    e = th20;
                    iNetworkPolicyManager2 = null;
                    reportWtf("starting NetworkPolicy Service", e);
                    iNetworkPolicyManager2 = iNetworkPolicyManager2;
                    traceEnd();
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                    }
                    if (!this.mPackageManager.hasSystemFeature("android.hardware.ethernet")) {
                    }
                    traceBeginAndSlog("StartEthernet");
                    countryDetector2 = null;
                    this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartConnectivityService");
                    iConnectivityManager2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context, iNetworkManagementService2, iNetworkStatsService2, iNetworkPolicyManager2);
                    iNetworkStatsService3 = iNetworkStatsService2;
                    iNetworkManagementService = iNetworkManagementService2;
                    ServiceManager.addService("connectivity", iConnectivityManager2, false, 6);
                    iNetworkPolicyManager2.bindConnectivityManager(iConnectivityManager2);
                    traceEnd();
                    traceBeginAndSlog("StartNsdService");
                    iBinder6 = NsdService.create(context);
                    ServiceManager.addService("servicediscovery", iBinder6);
                    iBinder2 = iBinder6;
                    traceEnd();
                    traceBeginAndSlog("StartSystemUpdateManagerService");
                    ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                    traceEnd();
                    traceBeginAndSlog("StartUpdateLockService");
                    ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                    traceEnd();
                    traceBeginAndSlog("StartNotificationManager");
                    this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                    SystemNotificationChannels.removeDeprecated(context);
                    SystemNotificationChannels.createAll(context);
                    notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                    traceEnd();
                    traceBeginAndSlog("StartDeviceMonitor");
                    this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                    traceEnd();
                    Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                    if (tuiEnable) {
                    }
                    traceBeginAndSlog("StartLocationManagerService");
                    hwLocation = HwServiceFactory.getHwLocationManagerService();
                    if (hwLocation != null) {
                    }
                    ServiceManager.addService("location", (IBinder) r12);
                    notification2 = notification;
                    location2 = r12;
                    traceEnd();
                    traceBeginAndSlog("StartCountryDetectorService");
                    countryDetectorService = new CountryDetectorService(context);
                    ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                    countryDetector3 = countryDetectorService;
                    traceEnd();
                    traceBeginAndSlog("StartTimeDetectorService");
                    countryDetector = countryDetector3;
                    this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                    traceEnd();
                    if (!isWatch) {
                    }
                    if (context.getResources().getBoolean(17891450)) {
                    }
                    traceBeginAndSlog("StartAudioService");
                    if (!isArc) {
                    }
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                    }
                    traceBeginAndSlog("StartDockObserver");
                    this.mSystemServiceManager.startService(DockObserver.class);
                    traceEnd();
                    if (isWatch) {
                    }
                    traceBeginAndSlog("StartWiredAccessoryManager");
                    inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                    }
                    traceBeginAndSlog("StartAdbService");
                    this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartUsbService");
                    this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                    traceEnd();
                    if (!isWatch) {
                    }
                    traceBeginAndSlog("StartHardwarePropertiesManagerService");
                    iBinder4 = new HardwarePropertiesManagerService(context);
                    ServiceManager.addService("hardware_properties", iBinder4);
                    iBinder3 = iBinder4;
                    traceEnd();
                    traceBeginAndSlog("StartTwilightService");
                    this.mSystemServiceManager.startService(TwilightService.class);
                    traceEnd();
                    traceBeginAndSlog("StartColorDisplay");
                    this.mSystemServiceManager.startService(ColorDisplayService.class);
                    traceEnd();
                    traceBeginAndSlog("StartJobScheduler");
                    this.mSystemServiceManager.startService(JobSchedulerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartSoundTrigger");
                    this.mSystemServiceManager.startService(SoundTriggerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartTrustManager");
                    this.mSystemServiceManager.startService(TrustManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                    }
                    traceBeginAndSlog("StartAppWidgetService");
                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartRoleManagerService");
                    SystemServiceManager systemServiceManager22222222 = this.mSystemServiceManager;
                    Context context222222222 = this.mSystemContext;
                    systemServiceManager22222222.startService(new RoleManagerService(context222222222, new LegacyRoleResolutionPolicy(context222222222)));
                    traceEnd();
                    traceBeginAndSlog("StartVoiceRecognitionManager");
                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                    traceEnd();
                    if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                    }
                    traceBeginAndSlog("StartSensorNotification");
                    this.mSystemServiceManager.startService(SensorNotificationService.class);
                    traceEnd();
                    traceBeginAndSlog("StartContextHubSystemService");
                    this.mSystemServiceManager.startService(ContextHubSystemService.class);
                    traceEnd();
                    HwServiceFactory.setupHwServices(context);
                    traceBeginAndSlog("StartDiskStatsService");
                    ServiceManager.addService("diskstats", new DiskStatsService(context));
                    traceEnd();
                    traceBeginAndSlog("RuntimeService");
                    ServiceManager.addService("runtime", new RuntimeService(context));
                    traceEnd();
                    if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                    }
                    if (SUPPORT_HW_ATTESTATION) {
                    }
                    traceBeginAndSlog("StartNetworkTimeUpdateService");
                    networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                    Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                    ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                    networkTimeUpdater3 = networkTimeUpdater2;
                    traceEnd();
                    traceBeginAndSlog("CertBlacklister");
                    new CertBlacklister(context);
                    traceEnd();
                    traceBeginAndSlog("StartEmergencyAffordanceService");
                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                    traceEnd();
                    traceBeginAndSlog("StartDreamManager");
                    this.mSystemServiceManager.startService(DreamManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("AddGraphicsStatsService");
                    ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                    traceEnd();
                    if (CoverageService.ENABLED) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                    }
                    traceBeginAndSlog("StartRestrictionManager");
                    this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartMediaSessionService");
                    this.mSystemServiceManager.startService(MediaSessionService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                    }
                    traceBeginAndSlog("StartTvInputManager");
                    this.mSystemServiceManager.startService(TvInputManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                    }
                    traceBeginAndSlog("StartMediaRouterService");
                    mediaRouterService = new MediaRouterService(context);
                    ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                    mediaRouter2 = mediaRouterService;
                    traceEnd();
                    hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                    hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                    hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                    if (hasFeatureFace) {
                    }
                    if (hasFeatureIris) {
                    }
                    if (hasFeatureFingerprint2) {
                    }
                    traceBeginAndSlog("StartBiometricService");
                    this.mSystemServiceManager.startService(BiometricService.class);
                    traceEnd();
                    traceBeginAndSlog("StartBackgroundDexOptService");
                    BackgroundDexOptService.schedule(context);
                    traceEnd();
                    if (!isWatch) {
                    }
                    if (!isWatch) {
                    }
                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartLauncherAppsService");
                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                    traceEnd();
                    traceBeginAndSlog("StartCrossProfileAppsService");
                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                    traceEnd();
                    location = location2;
                    mediaRouter = mediaRouter3;
                    hasFeatureFingerprint = tuiEnable2;
                    networkTimeUpdater = networkTimeUpdater3;
                    ipSecService = ipSecService2;
                    iNetworkStatsService = iNetworkStatsService3;
                    iConnectivityManager = iConnectivityManager2;
                    if (!isWatch) {
                    }
                    if (isWatch) {
                    }
                    if (!disableSlices) {
                    }
                    if (!disableCameraService) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartIncidentCompanionService");
                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                    traceEnd();
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    MmsServiceBroker mmsService22222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                    }
                    traceBeginAndSlog("StartClipboardService");
                    this.mSystemServiceManager.startService(ClipboardService.class);
                    traceEnd();
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("AppServiceManager");
                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    vibrator.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings != null) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                    this.mSystemServiceManager.startBootPhase(480);
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                    this.mSystemServiceManager.startBootPhase(500);
                    traceEnd();
                    traceBeginAndSlog("MakeWindowManagerServiceReady");
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                    }
                    Configuration config22222222222222222222222 = wm.computeNewConfiguration(0);
                    DisplayMetrics metrics22222222222222222222222 = new DisplayMetrics();
                    ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222222222222222);
                    context.getResources().updateConfiguration(config22222222222222222222222, metrics22222222222222222222222);
                    systemTheme = context.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("StartPermissionPolicyService");
                    this.mSystemServiceManager.startService(PermissionPolicyService.class);
                    traceEnd();
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    String[] classes22222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                    while (r14 < r7) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222222222222222, enableIaware) {
                        /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                        private final /* synthetic */ Context f$1;
                        private final /* synthetic */ CountryDetectorService f$10;
                        private final /* synthetic */ NetworkTimeUpdateService f$11;
                        private final /* synthetic */ InputManagerService f$12;
                        private final /* synthetic */ TelephonyRegistry f$13;
                        private final /* synthetic */ MediaRouterService f$14;
                        private final /* synthetic */ MmsServiceBroker f$15;
                        private final /* synthetic */ boolean f$16;
                        private final /* synthetic */ WindowManagerService f$2;
                        private final /* synthetic */ boolean f$3;
                        private final /* synthetic */ ConnectivityService f$4;
                        private final /* synthetic */ NetworkManagementService f$5;
                        private final /* synthetic */ NetworkPolicyManagerService f$6;
                        private final /* synthetic */ IpSecService f$7;
                        private final /* synthetic */ NetworkStatsService f$8;
                        private final /* synthetic */ LocationManagerService f$9;

                        {
                            this.f$1 = r4;
                            this.f$2 = r5;
                            this.f$3 = r6;
                            this.f$4 = r7;
                            this.f$5 = r8;
                            this.f$6 = r9;
                            this.f$7 = r10;
                            this.f$8 = r11;
                            this.f$9 = r12;
                            this.f$10 = r13;
                            this.f$11 = r14;
                            this.f$12 = r15;
                            this.f$13 = r16;
                            this.f$14 = r17;
                            this.f$15 = r18;
                            this.f$16 = r19;
                        }

                        public final void run() {
                            SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                        }
                    }, BOOT_TIMINGS_TRACE_LOG);
                }
            } catch (Throwable th21) {
                e = th21;
                iNetworkPolicyManager2 = null;
                reportWtf("starting NetworkPolicy Service", e);
                iNetworkPolicyManager2 = iNetworkPolicyManager2;
                traceEnd();
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                }
                if (!this.mPackageManager.hasSystemFeature("android.hardware.ethernet")) {
                }
                traceBeginAndSlog("StartEthernet");
                countryDetector2 = null;
                this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartConnectivityService");
                iConnectivityManager2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context, iNetworkManagementService2, iNetworkStatsService2, iNetworkPolicyManager2);
                iNetworkStatsService3 = iNetworkStatsService2;
                iNetworkManagementService = iNetworkManagementService2;
                ServiceManager.addService("connectivity", iConnectivityManager2, false, 6);
                iNetworkPolicyManager2.bindConnectivityManager(iConnectivityManager2);
                traceEnd();
                traceBeginAndSlog("StartNsdService");
                iBinder6 = NsdService.create(context);
                ServiceManager.addService("servicediscovery", iBinder6);
                iBinder2 = iBinder6;
                traceEnd();
                traceBeginAndSlog("StartSystemUpdateManagerService");
                ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                traceEnd();
                traceBeginAndSlog("StartUpdateLockService");
                ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                traceEnd();
                traceBeginAndSlog("StartNotificationManager");
                this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                SystemNotificationChannels.removeDeprecated(context);
                SystemNotificationChannels.createAll(context);
                notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                traceEnd();
                traceBeginAndSlog("StartDeviceMonitor");
                this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                traceEnd();
                Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                if (tuiEnable) {
                }
                traceBeginAndSlog("StartLocationManagerService");
                hwLocation = HwServiceFactory.getHwLocationManagerService();
                if (hwLocation != null) {
                }
                ServiceManager.addService("location", (IBinder) r12);
                notification2 = notification;
                location2 = r12;
                traceEnd();
                traceBeginAndSlog("StartCountryDetectorService");
                countryDetectorService = new CountryDetectorService(context);
                ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                countryDetector3 = countryDetectorService;
                traceEnd();
                traceBeginAndSlog("StartTimeDetectorService");
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager222222222 = this.mSystemServiceManager;
                Context context2222222222 = this.mSystemContext;
                systemServiceManager222222222.startService(new RoleManagerService(context2222222222, new LegacyRoleResolutionPolicy(context2222222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config222222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics222222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222222222222222222);
                context.getResources().updateConfiguration(config222222222222222222222222, metrics222222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                traceBeginAndSlog("StartWifi");
                this.mSystemServiceManager.startService(WIFI_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartWifiScanning");
                this.mSystemServiceManager.startService("com.android.server.wifi.scanner.WifiScanningService");
                traceEnd();
            }
            if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                traceBeginAndSlog("StartRttService");
                this.mSystemServiceManager.startService("com.android.server.wifi.rtt.RttService");
                traceEnd();
            }
            if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                traceBeginAndSlog("StartWifiAware");
                this.mSystemServiceManager.startService(WIFI_AWARE_SERVICE_CLASS);
                traceEnd();
            }
            if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                traceBeginAndSlog("StartWifiP2P");
                this.mSystemServiceManager.startService(WIFI_P2P_SERVICE_CLASS);
                traceEnd();
            }
            if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                traceBeginAndSlog("StartLowpan");
                this.mSystemServiceManager.startService(LOWPAN_SERVICE_CLASS);
                traceEnd();
            }
            if (!this.mPackageManager.hasSystemFeature("android.hardware.ethernet") || this.mPackageManager.hasSystemFeature("android.hardware.usb.host")) {
                traceBeginAndSlog("StartEthernet");
                countryDetector2 = null;
                this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                traceEnd();
            } else {
                countryDetector2 = null;
            }
            traceBeginAndSlog("StartConnectivityService");
            try {
                iConnectivityManager2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context, iNetworkManagementService2, iNetworkStatsService2, iNetworkPolicyManager2);
                iNetworkStatsService3 = iNetworkStatsService2;
                iNetworkManagementService = iNetworkManagementService2;
                try {
                    ServiceManager.addService("connectivity", iConnectivityManager2, false, 6);
                    iNetworkPolicyManager2.bindConnectivityManager(iConnectivityManager2);
                } catch (Throwable th22) {
                    e = th22;
                }
            } catch (Throwable th23) {
                e = th23;
                iNetworkStatsService3 = iNetworkStatsService2;
                iNetworkManagementService = iNetworkManagementService2;
                iConnectivityManager2 = null;
                reportWtf("starting Connectivity Service", e);
                traceEnd();
                traceBeginAndSlog("StartNsdService");
                iBinder6 = NsdService.create(context);
                ServiceManager.addService("servicediscovery", iBinder6);
                iBinder2 = iBinder6;
                traceEnd();
                traceBeginAndSlog("StartSystemUpdateManagerService");
                ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                traceEnd();
                traceBeginAndSlog("StartUpdateLockService");
                ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                traceEnd();
                traceBeginAndSlog("StartNotificationManager");
                this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                SystemNotificationChannels.removeDeprecated(context);
                SystemNotificationChannels.createAll(context);
                notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                traceEnd();
                traceBeginAndSlog("StartDeviceMonitor");
                this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                traceEnd();
                Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                if (tuiEnable) {
                }
                traceBeginAndSlog("StartLocationManagerService");
                hwLocation = HwServiceFactory.getHwLocationManagerService();
                if (hwLocation != null) {
                }
                ServiceManager.addService("location", (IBinder) r12);
                notification2 = notification;
                location2 = r12;
                traceEnd();
                traceBeginAndSlog("StartCountryDetectorService");
                countryDetectorService = new CountryDetectorService(context);
                ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                countryDetector3 = countryDetectorService;
                traceEnd();
                traceBeginAndSlog("StartTimeDetectorService");
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager2222222222 = this.mSystemServiceManager;
                Context context22222222222 = this.mSystemContext;
                systemServiceManager2222222222.startService(new RoleManagerService(context22222222222, new LegacyRoleResolutionPolicy(context22222222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService2222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config2222222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics2222222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222222222222222222);
                context.getResources().updateConfiguration(config2222222222222222222222222, metrics2222222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes2222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            traceBeginAndSlog("StartNsdService");
            try {
                iBinder6 = NsdService.create(context);
                ServiceManager.addService("servicediscovery", iBinder6);
                iBinder2 = iBinder6;
            } catch (Throwable th24) {
                e = th24;
                iBinder6 = null;
                reportWtf("starting Service Discovery Service", e);
                iBinder2 = iBinder6;
                traceEnd();
                traceBeginAndSlog("StartSystemUpdateManagerService");
                ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
                traceEnd();
                traceBeginAndSlog("StartUpdateLockService");
                ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
                traceEnd();
                traceBeginAndSlog("StartNotificationManager");
                this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                SystemNotificationChannels.removeDeprecated(context);
                SystemNotificationChannels.createAll(context);
                notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                traceEnd();
                traceBeginAndSlog("StartDeviceMonitor");
                this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                traceEnd();
                Slog.i(TAG, "TUI Connect enable " + tuiEnable);
                if (tuiEnable) {
                }
                traceBeginAndSlog("StartLocationManagerService");
                hwLocation = HwServiceFactory.getHwLocationManagerService();
                if (hwLocation != null) {
                }
                ServiceManager.addService("location", (IBinder) r12);
                notification2 = notification;
                location2 = r12;
                traceEnd();
                traceBeginAndSlog("StartCountryDetectorService");
                countryDetectorService = new CountryDetectorService(context);
                ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                countryDetector3 = countryDetectorService;
                traceEnd();
                traceBeginAndSlog("StartTimeDetectorService");
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager22222222222 = this.mSystemServiceManager;
                Context context222222222222 = this.mSystemContext;
                systemServiceManager22222222222.startService(new RoleManagerService(context222222222222, new LegacyRoleResolutionPolicy(context222222222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService22222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config22222222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics22222222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222222222222222222);
                context.getResources().updateConfiguration(config22222222222222222222222222, metrics22222222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes22222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            traceBeginAndSlog("StartSystemUpdateManagerService");
            try {
                ServiceManager.addService("system_update", (IBinder) new SystemUpdateManagerService(context));
            } catch (Throwable e24) {
                reportWtf("starting SystemUpdateManagerService", e24);
            }
            traceEnd();
            traceBeginAndSlog("StartUpdateLockService");
            try {
                ServiceManager.addService("updatelock", (IBinder) new UpdateLockService(context));
            } catch (Throwable e25) {
                reportWtf("starting UpdateLockService", e25);
            }
            traceEnd();
            traceBeginAndSlog("StartNotificationManager");
            try {
                this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
            } catch (RuntimeException e26) {
                this.mSystemServiceManager.startService(NotificationManagerService.class);
            }
            SystemNotificationChannels.removeDeprecated(context);
            SystemNotificationChannels.createAll(context);
            notification = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            traceEnd();
            traceBeginAndSlog("StartDeviceMonitor");
            this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
            traceEnd();
            Slog.i(TAG, "TUI Connect enable " + tuiEnable);
            if (tuiEnable) {
                try {
                    ServiceManager.addService("tui", (IBinder) new TrustedUIService(context));
                } catch (Throwable e27) {
                    Slog.e(TAG, "Failure starting TUI Service ", e27);
                }
            }
            traceBeginAndSlog("StartLocationManagerService");
            try {
                hwLocation = HwServiceFactory.getHwLocationManagerService();
                if (hwLocation != null) {
                    r12 = hwLocation.getInstance(context);
                } else {
                    r12 = new LocationManagerService(context);
                }
                ServiceManager.addService("location", (IBinder) r12);
                notification2 = notification;
                location2 = r12;
            } catch (Throwable th25) {
                e = th25;
                location3 = null;
                notification2 = notification;
                reportWtf("starting Location Manager", e);
                location2 = location3;
                traceEnd();
                traceBeginAndSlog("StartCountryDetectorService");
                countryDetectorService = new CountryDetectorService(context);
                ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                countryDetector3 = countryDetectorService;
                traceEnd();
                traceBeginAndSlog("StartTimeDetectorService");
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager222222222222 = this.mSystemServiceManager;
                Context context2222222222222 = this.mSystemContext;
                systemServiceManager222222222222.startService(new RoleManagerService(context2222222222222, new LegacyRoleResolutionPolicy(context2222222222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config222222222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics222222222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222222222222222222222);
                context.getResources().updateConfiguration(config222222222222222222222222222, metrics222222222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            traceBeginAndSlog("StartCountryDetectorService");
            try {
                countryDetectorService = new CountryDetectorService(context);
                ServiceManager.addService("country_detector", (IBinder) countryDetectorService);
                countryDetector3 = countryDetectorService;
            } catch (Throwable th26) {
                e = th26;
                countryDetector4 = countryDetector2;
                reportWtf("starting Country Detector", e);
                countryDetector3 = countryDetector4;
                traceEnd();
                traceBeginAndSlog("StartTimeDetectorService");
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager2222222222222 = this.mSystemServiceManager;
                Context context22222222222222 = this.mSystemContext;
                systemServiceManager2222222222222.startService(new RoleManagerService(context22222222222222, new LegacyRoleResolutionPolicy(context22222222222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService2222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config2222222222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics2222222222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222222222222222222222);
                context.getResources().updateConfiguration(config2222222222222222222222222222, metrics2222222222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes2222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            traceBeginAndSlog("StartTimeDetectorService");
            try {
                countryDetector = countryDetector3;
                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
            } catch (Throwable th27) {
                e = th27;
                countryDetector = countryDetector3;
                reportWtf("starting StartTimeDetectorService service", e);
                traceEnd();
                if (!isWatch) {
                }
                if (context.getResources().getBoolean(17891450)) {
                }
                traceBeginAndSlog("StartAudioService");
                if (!isArc) {
                }
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                }
                traceBeginAndSlog("StartDockObserver");
                this.mSystemServiceManager.startService(DockObserver.class);
                traceEnd();
                if (isWatch) {
                }
                traceBeginAndSlog("StartWiredAccessoryManager");
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                }
                traceBeginAndSlog("StartAdbService");
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
                if (!isWatch) {
                }
                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager22222222222222 = this.mSystemServiceManager;
                Context context222222222222222 = this.mSystemContext;
                systemServiceManager22222222222222.startService(new RoleManagerService(context222222222222222, new LegacyRoleResolutionPolicy(context222222222222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService22222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config22222222222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics22222222222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222222222222222222222);
                context.getResources().updateConfiguration(config22222222222222222222222222222, metrics22222222222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes22222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            if (!isWatch) {
                traceBeginAndSlog("StartSearchManagerService");
                try {
                    this.mSystemServiceManager.startService(SEARCH_MANAGER_SERVICE_CLASS);
                } catch (Throwable e28) {
                    reportWtf("starting Search Service", e28);
                }
                traceEnd();
            }
            if (context.getResources().getBoolean(17891450)) {
                traceBeginAndSlog("StartWallpaperManagerService");
                this.mSystemServiceManager.startService(WALLPAPER_SERVICE_CLASS);
                traceEnd();
            }
            traceBeginAndSlog("StartAudioService");
            if (!isArc) {
                this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                tuiEnable2 = tuiEnable;
                iNetworkPolicyManager = iNetworkPolicyManager2;
            } else {
                String className = context.getResources().getString(17039837);
                try {
                    SystemServiceManager systemServiceManager3 = this.mSystemServiceManager;
                    tuiEnable2 = tuiEnable;
                    try {
                        StringBuilder sb = new StringBuilder();
                        sb.append(className);
                        iNetworkPolicyManager = iNetworkPolicyManager2;
                        try {
                            sb.append("$Lifecycle");
                            systemServiceManager3.startService(sb.toString());
                        } catch (Throwable th28) {
                            e = th28;
                        }
                    } catch (Throwable th29) {
                        e = th29;
                        iNetworkPolicyManager = iNetworkPolicyManager2;
                        reportWtf("starting " + className, e);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                        }
                        traceBeginAndSlog("StartDockObserver");
                        this.mSystemServiceManager.startService(DockObserver.class);
                        traceEnd();
                        if (isWatch) {
                        }
                        traceBeginAndSlog("StartWiredAccessoryManager");
                        inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                        }
                        traceBeginAndSlog("StartAdbService");
                        this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartUsbService");
                        this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                        traceEnd();
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartHardwarePropertiesManagerService");
                        iBinder4 = new HardwarePropertiesManagerService(context);
                        ServiceManager.addService("hardware_properties", iBinder4);
                        iBinder3 = iBinder4;
                        traceEnd();
                        traceBeginAndSlog("StartTwilightService");
                        this.mSystemServiceManager.startService(TwilightService.class);
                        traceEnd();
                        traceBeginAndSlog("StartColorDisplay");
                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                        traceEnd();
                        traceBeginAndSlog("StartJobScheduler");
                        this.mSystemServiceManager.startService(JobSchedulerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartSoundTrigger");
                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartTrustManager");
                        this.mSystemServiceManager.startService(TrustManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                        }
                        traceBeginAndSlog("StartAppWidgetService");
                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartRoleManagerService");
                        SystemServiceManager systemServiceManager222222222222222 = this.mSystemServiceManager;
                        Context context2222222222222222 = this.mSystemContext;
                        systemServiceManager222222222222222.startService(new RoleManagerService(context2222222222222222, new LegacyRoleResolutionPolicy(context2222222222222222)));
                        traceEnd();
                        traceBeginAndSlog("StartVoiceRecognitionManager");
                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                        traceEnd();
                        if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                        }
                        traceBeginAndSlog("StartSensorNotification");
                        this.mSystemServiceManager.startService(SensorNotificationService.class);
                        traceEnd();
                        traceBeginAndSlog("StartContextHubSystemService");
                        this.mSystemServiceManager.startService(ContextHubSystemService.class);
                        traceEnd();
                        HwServiceFactory.setupHwServices(context);
                        traceBeginAndSlog("StartDiskStatsService");
                        ServiceManager.addService("diskstats", new DiskStatsService(context));
                        traceEnd();
                        traceBeginAndSlog("RuntimeService");
                        ServiceManager.addService("runtime", new RuntimeService(context));
                        traceEnd();
                        if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                        }
                        if (SUPPORT_HW_ATTESTATION) {
                        }
                        traceBeginAndSlog("StartNetworkTimeUpdateService");
                        networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                        Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                        ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                        networkTimeUpdater3 = networkTimeUpdater2;
                        traceEnd();
                        traceBeginAndSlog("CertBlacklister");
                        new CertBlacklister(context);
                        traceEnd();
                        traceBeginAndSlog("StartEmergencyAffordanceService");
                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                        traceEnd();
                        traceBeginAndSlog("StartDreamManager");
                        this.mSystemServiceManager.startService(DreamManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("AddGraphicsStatsService");
                        ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                        traceEnd();
                        if (CoverageService.ENABLED) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                        }
                        traceBeginAndSlog("StartRestrictionManager");
                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartMediaSessionService");
                        this.mSystemServiceManager.startService(MediaSessionService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                        }
                        traceBeginAndSlog("StartTvInputManager");
                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        }
                        traceBeginAndSlog("StartMediaRouterService");
                        mediaRouterService = new MediaRouterService(context);
                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                        mediaRouter2 = mediaRouterService;
                        traceEnd();
                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                        hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                        if (hasFeatureFace) {
                        }
                        if (hasFeatureIris) {
                        }
                        if (hasFeatureFingerprint2) {
                        }
                        traceBeginAndSlog("StartBiometricService");
                        this.mSystemServiceManager.startService(BiometricService.class);
                        traceEnd();
                        traceBeginAndSlog("StartBackgroundDexOptService");
                        BackgroundDexOptService.schedule(context);
                        traceEnd();
                        if (!isWatch) {
                        }
                        if (!isWatch) {
                        }
                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartLauncherAppsService");
                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                        traceEnd();
                        traceBeginAndSlog("StartCrossProfileAppsService");
                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                        traceEnd();
                        location = location2;
                        mediaRouter = mediaRouter3;
                        hasFeatureFingerprint = tuiEnable2;
                        networkTimeUpdater = networkTimeUpdater3;
                        ipSecService = ipSecService2;
                        iNetworkStatsService = iNetworkStatsService3;
                        iConnectivityManager = iConnectivityManager2;
                        if (!isWatch) {
                        }
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartIncidentCompanionService");
                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                        traceEnd();
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        MmsServiceBroker mmsService222222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("AppServiceManager");
                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        vibrator.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings != null) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                        this.mSystemServiceManager.startBootPhase(480);
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                        this.mSystemServiceManager.startBootPhase(500);
                        traceEnd();
                        traceBeginAndSlog("MakeWindowManagerServiceReady");
                        wm.systemReady();
                        traceEnd();
                        if (safeMode) {
                        }
                        Configuration config222222222222222222222222222222 = wm.computeNewConfiguration(0);
                        DisplayMetrics metrics222222222222222222222222222222 = new DisplayMetrics();
                        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222222222222222222222222);
                        context.getResources().updateConfiguration(config222222222222222222222222222222, metrics222222222222222222222222222222);
                        systemTheme = context.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("StartPermissionPolicyService");
                        this.mSystemServiceManager.startService(PermissionPolicyService.class);
                        traceEnd();
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        String[] classes222222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                        while (r14 < r7) {
                        }
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222222222222222222222222, enableIaware) {
                            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                            private final /* synthetic */ Context f$1;
                            private final /* synthetic */ CountryDetectorService f$10;
                            private final /* synthetic */ NetworkTimeUpdateService f$11;
                            private final /* synthetic */ InputManagerService f$12;
                            private final /* synthetic */ TelephonyRegistry f$13;
                            private final /* synthetic */ MediaRouterService f$14;
                            private final /* synthetic */ MmsServiceBroker f$15;
                            private final /* synthetic */ boolean f$16;
                            private final /* synthetic */ WindowManagerService f$2;
                            private final /* synthetic */ boolean f$3;
                            private final /* synthetic */ ConnectivityService f$4;
                            private final /* synthetic */ NetworkManagementService f$5;
                            private final /* synthetic */ NetworkPolicyManagerService f$6;
                            private final /* synthetic */ IpSecService f$7;
                            private final /* synthetic */ NetworkStatsService f$8;
                            private final /* synthetic */ LocationManagerService f$9;

                            {
                                this.f$1 = r4;
                                this.f$2 = r5;
                                this.f$3 = r6;
                                this.f$4 = r7;
                                this.f$5 = r8;
                                this.f$6 = r9;
                                this.f$7 = r10;
                                this.f$8 = r11;
                                this.f$9 = r12;
                                this.f$10 = r13;
                                this.f$11 = r14;
                                this.f$12 = r15;
                                this.f$13 = r16;
                                this.f$14 = r17;
                                this.f$15 = r18;
                                this.f$16 = r19;
                            }

                            public final void run() {
                                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                            }
                        }, BOOT_TIMINGS_TRACE_LOG);
                    }
                } catch (Throwable th30) {
                    e = th30;
                    tuiEnable2 = tuiEnable;
                    iNetworkPolicyManager = iNetworkPolicyManager2;
                    reportWtf("starting " + className, e);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                    }
                    traceBeginAndSlog("StartDockObserver");
                    this.mSystemServiceManager.startService(DockObserver.class);
                    traceEnd();
                    if (isWatch) {
                    }
                    traceBeginAndSlog("StartWiredAccessoryManager");
                    inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                    }
                    traceBeginAndSlog("StartAdbService");
                    this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartUsbService");
                    this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                    traceEnd();
                    if (!isWatch) {
                    }
                    traceBeginAndSlog("StartHardwarePropertiesManagerService");
                    iBinder4 = new HardwarePropertiesManagerService(context);
                    ServiceManager.addService("hardware_properties", iBinder4);
                    iBinder3 = iBinder4;
                    traceEnd();
                    traceBeginAndSlog("StartTwilightService");
                    this.mSystemServiceManager.startService(TwilightService.class);
                    traceEnd();
                    traceBeginAndSlog("StartColorDisplay");
                    this.mSystemServiceManager.startService(ColorDisplayService.class);
                    traceEnd();
                    traceBeginAndSlog("StartJobScheduler");
                    this.mSystemServiceManager.startService(JobSchedulerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartSoundTrigger");
                    this.mSystemServiceManager.startService(SoundTriggerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartTrustManager");
                    this.mSystemServiceManager.startService(TrustManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                    }
                    traceBeginAndSlog("StartAppWidgetService");
                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartRoleManagerService");
                    SystemServiceManager systemServiceManager2222222222222222 = this.mSystemServiceManager;
                    Context context22222222222222222 = this.mSystemContext;
                    systemServiceManager2222222222222222.startService(new RoleManagerService(context22222222222222222, new LegacyRoleResolutionPolicy(context22222222222222222)));
                    traceEnd();
                    traceBeginAndSlog("StartVoiceRecognitionManager");
                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                    traceEnd();
                    if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                    }
                    traceBeginAndSlog("StartSensorNotification");
                    this.mSystemServiceManager.startService(SensorNotificationService.class);
                    traceEnd();
                    traceBeginAndSlog("StartContextHubSystemService");
                    this.mSystemServiceManager.startService(ContextHubSystemService.class);
                    traceEnd();
                    HwServiceFactory.setupHwServices(context);
                    traceBeginAndSlog("StartDiskStatsService");
                    ServiceManager.addService("diskstats", new DiskStatsService(context));
                    traceEnd();
                    traceBeginAndSlog("RuntimeService");
                    ServiceManager.addService("runtime", new RuntimeService(context));
                    traceEnd();
                    if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                    }
                    if (SUPPORT_HW_ATTESTATION) {
                    }
                    traceBeginAndSlog("StartNetworkTimeUpdateService");
                    networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                    Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                    ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                    networkTimeUpdater3 = networkTimeUpdater2;
                    traceEnd();
                    traceBeginAndSlog("CertBlacklister");
                    new CertBlacklister(context);
                    traceEnd();
                    traceBeginAndSlog("StartEmergencyAffordanceService");
                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                    traceEnd();
                    traceBeginAndSlog("StartDreamManager");
                    this.mSystemServiceManager.startService(DreamManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("AddGraphicsStatsService");
                    ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                    traceEnd();
                    if (CoverageService.ENABLED) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                    }
                    traceBeginAndSlog("StartRestrictionManager");
                    this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartMediaSessionService");
                    this.mSystemServiceManager.startService(MediaSessionService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                    }
                    traceBeginAndSlog("StartTvInputManager");
                    this.mSystemServiceManager.startService(TvInputManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                    }
                    traceBeginAndSlog("StartMediaRouterService");
                    mediaRouterService = new MediaRouterService(context);
                    ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                    mediaRouter2 = mediaRouterService;
                    traceEnd();
                    hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                    hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                    hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                    if (hasFeatureFace) {
                    }
                    if (hasFeatureIris) {
                    }
                    if (hasFeatureFingerprint2) {
                    }
                    traceBeginAndSlog("StartBiometricService");
                    this.mSystemServiceManager.startService(BiometricService.class);
                    traceEnd();
                    traceBeginAndSlog("StartBackgroundDexOptService");
                    BackgroundDexOptService.schedule(context);
                    traceEnd();
                    if (!isWatch) {
                    }
                    if (!isWatch) {
                    }
                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartLauncherAppsService");
                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                    traceEnd();
                    traceBeginAndSlog("StartCrossProfileAppsService");
                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                    traceEnd();
                    location = location2;
                    mediaRouter = mediaRouter3;
                    hasFeatureFingerprint = tuiEnable2;
                    networkTimeUpdater = networkTimeUpdater3;
                    ipSecService = ipSecService2;
                    iNetworkStatsService = iNetworkStatsService3;
                    iConnectivityManager = iConnectivityManager2;
                    if (!isWatch) {
                    }
                    if (isWatch) {
                    }
                    if (!disableSlices) {
                    }
                    if (!disableCameraService) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartIncidentCompanionService");
                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                    traceEnd();
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    MmsServiceBroker mmsService2222222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                    }
                    traceBeginAndSlog("StartClipboardService");
                    this.mSystemServiceManager.startService(ClipboardService.class);
                    traceEnd();
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("AppServiceManager");
                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    vibrator.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings != null) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                    this.mSystemServiceManager.startBootPhase(480);
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                    this.mSystemServiceManager.startBootPhase(500);
                    traceEnd();
                    traceBeginAndSlog("MakeWindowManagerServiceReady");
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                    }
                    Configuration config2222222222222222222222222222222 = wm.computeNewConfiguration(0);
                    DisplayMetrics metrics2222222222222222222222222222222 = new DisplayMetrics();
                    ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222222222222222222222222);
                    context.getResources().updateConfiguration(config2222222222222222222222222222222, metrics2222222222222222222222222222222);
                    systemTheme = context.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("StartPermissionPolicyService");
                    this.mSystemServiceManager.startService(PermissionPolicyService.class);
                    traceEnd();
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    String[] classes2222222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                    while (r14 < r7) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222222222222222222222222, enableIaware) {
                        /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                        private final /* synthetic */ Context f$1;
                        private final /* synthetic */ CountryDetectorService f$10;
                        private final /* synthetic */ NetworkTimeUpdateService f$11;
                        private final /* synthetic */ InputManagerService f$12;
                        private final /* synthetic */ TelephonyRegistry f$13;
                        private final /* synthetic */ MediaRouterService f$14;
                        private final /* synthetic */ MmsServiceBroker f$15;
                        private final /* synthetic */ boolean f$16;
                        private final /* synthetic */ WindowManagerService f$2;
                        private final /* synthetic */ boolean f$3;
                        private final /* synthetic */ ConnectivityService f$4;
                        private final /* synthetic */ NetworkManagementService f$5;
                        private final /* synthetic */ NetworkPolicyManagerService f$6;
                        private final /* synthetic */ IpSecService f$7;
                        private final /* synthetic */ NetworkStatsService f$8;
                        private final /* synthetic */ LocationManagerService f$9;

                        {
                            this.f$1 = r4;
                            this.f$2 = r5;
                            this.f$3 = r6;
                            this.f$4 = r7;
                            this.f$5 = r8;
                            this.f$6 = r9;
                            this.f$7 = r10;
                            this.f$8 = r11;
                            this.f$9 = r12;
                            this.f$10 = r13;
                            this.f$11 = r14;
                            this.f$12 = r15;
                            this.f$13 = r16;
                            this.f$14 = r17;
                            this.f$15 = r18;
                            this.f$16 = r19;
                        }

                        public final void run() {
                            SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                        }
                    }, BOOT_TIMINGS_TRACE_LOG);
                }
            }
            traceEnd();
            if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                traceBeginAndSlog("StartBroadcastRadioService");
                this.mSystemServiceManager.startService(BroadcastRadioService.class);
                traceEnd();
            }
            traceBeginAndSlog("StartDockObserver");
            this.mSystemServiceManager.startService(DockObserver.class);
            traceEnd();
            if (isWatch) {
                traceBeginAndSlog("StartThermalObserver");
                this.mSystemServiceManager.startService(THERMAL_OBSERVER_CLASS);
                traceEnd();
            }
            traceBeginAndSlog("StartWiredAccessoryManager");
            try {
                inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));
            } catch (Throwable e29) {
                reportWtf("starting WiredAccessoryManager", e29);
            }
            traceEnd();
            if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                traceBeginAndSlog("StartMidiManager");
                this.mSystemServiceManager.startService(MIDI_SERVICE_CLASS);
                traceEnd();
            }
            traceBeginAndSlog("StartAdbService");
            try {
                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
            } catch (Throwable th31) {
                Slog.e(TAG, "Failure starting AdbService");
            }
            traceEnd();
            if (this.mPackageManager.hasSystemFeature("android.hardware.usb.host") || this.mPackageManager.hasSystemFeature("android.hardware.usb.accessory") || isEmulator) {
                traceBeginAndSlog("StartUsbService");
                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                traceEnd();
            }
            if (!isWatch) {
                traceBeginAndSlog("StartSerialService");
                try {
                    iBinder5 = new SerialService(context);
                    try {
                        ServiceManager.addService("serial", iBinder5);
                    } catch (Throwable th32) {
                        e = th32;
                    }
                } catch (Throwable th33) {
                    e = th33;
                    iBinder5 = null;
                    Slog.e(TAG, "Failure starting SerialService", e);
                    traceEnd();
                    iBinder8 = iBinder5;
                    traceBeginAndSlog("StartHardwarePropertiesManagerService");
                    iBinder4 = new HardwarePropertiesManagerService(context);
                    ServiceManager.addService("hardware_properties", iBinder4);
                    iBinder3 = iBinder4;
                    traceEnd();
                    traceBeginAndSlog("StartTwilightService");
                    this.mSystemServiceManager.startService(TwilightService.class);
                    traceEnd();
                    traceBeginAndSlog("StartColorDisplay");
                    this.mSystemServiceManager.startService(ColorDisplayService.class);
                    traceEnd();
                    traceBeginAndSlog("StartJobScheduler");
                    this.mSystemServiceManager.startService(JobSchedulerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartSoundTrigger");
                    this.mSystemServiceManager.startService(SoundTriggerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartTrustManager");
                    this.mSystemServiceManager.startService(TrustManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                    }
                    traceBeginAndSlog("StartAppWidgetService");
                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                    traceEnd();
                    traceBeginAndSlog("StartRoleManagerService");
                    SystemServiceManager systemServiceManager22222222222222222 = this.mSystemServiceManager;
                    Context context222222222222222222 = this.mSystemContext;
                    systemServiceManager22222222222222222.startService(new RoleManagerService(context222222222222222222, new LegacyRoleResolutionPolicy(context222222222222222222)));
                    traceEnd();
                    traceBeginAndSlog("StartVoiceRecognitionManager");
                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                    traceEnd();
                    if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                    }
                    traceBeginAndSlog("StartSensorNotification");
                    this.mSystemServiceManager.startService(SensorNotificationService.class);
                    traceEnd();
                    traceBeginAndSlog("StartContextHubSystemService");
                    this.mSystemServiceManager.startService(ContextHubSystemService.class);
                    traceEnd();
                    HwServiceFactory.setupHwServices(context);
                    traceBeginAndSlog("StartDiskStatsService");
                    ServiceManager.addService("diskstats", new DiskStatsService(context));
                    traceEnd();
                    traceBeginAndSlog("RuntimeService");
                    ServiceManager.addService("runtime", new RuntimeService(context));
                    traceEnd();
                    if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                    }
                    if (SUPPORT_HW_ATTESTATION) {
                    }
                    traceBeginAndSlog("StartNetworkTimeUpdateService");
                    networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                    Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                    ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                    networkTimeUpdater3 = networkTimeUpdater2;
                    traceEnd();
                    traceBeginAndSlog("CertBlacklister");
                    new CertBlacklister(context);
                    traceEnd();
                    traceBeginAndSlog("StartEmergencyAffordanceService");
                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                    traceEnd();
                    traceBeginAndSlog("StartDreamManager");
                    this.mSystemServiceManager.startService(DreamManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("AddGraphicsStatsService");
                    ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                    traceEnd();
                    if (CoverageService.ENABLED) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                    }
                    traceBeginAndSlog("StartRestrictionManager");
                    this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                    traceEnd();
                    traceBeginAndSlog("StartMediaSessionService");
                    this.mSystemServiceManager.startService(MediaSessionService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                    }
                    traceBeginAndSlog("StartTvInputManager");
                    this.mSystemServiceManager.startService(TvInputManagerService.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                    }
                    if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                    }
                    traceBeginAndSlog("StartMediaRouterService");
                    mediaRouterService = new MediaRouterService(context);
                    ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                    mediaRouter2 = mediaRouterService;
                    traceEnd();
                    hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                    hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                    hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                    if (hasFeatureFace) {
                    }
                    if (hasFeatureIris) {
                    }
                    if (hasFeatureFingerprint2) {
                    }
                    traceBeginAndSlog("StartBiometricService");
                    this.mSystemServiceManager.startService(BiometricService.class);
                    traceEnd();
                    traceBeginAndSlog("StartBackgroundDexOptService");
                    BackgroundDexOptService.schedule(context);
                    traceEnd();
                    if (!isWatch) {
                    }
                    if (!isWatch) {
                    }
                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartLauncherAppsService");
                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                    traceEnd();
                    traceBeginAndSlog("StartCrossProfileAppsService");
                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                    traceEnd();
                    location = location2;
                    mediaRouter = mediaRouter3;
                    hasFeatureFingerprint = tuiEnable2;
                    networkTimeUpdater = networkTimeUpdater3;
                    ipSecService = ipSecService2;
                    iNetworkStatsService = iNetworkStatsService3;
                    iConnectivityManager = iConnectivityManager2;
                    if (!isWatch) {
                    }
                    if (isWatch) {
                    }
                    if (!disableSlices) {
                    }
                    if (!disableCameraService) {
                    }
                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("StartIncidentCompanionService");
                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                    traceEnd();
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    MmsServiceBroker mmsService22222222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                    }
                    traceBeginAndSlog("StartClipboardService");
                    this.mSystemServiceManager.startService(ClipboardService.class);
                    traceEnd();
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("AppServiceManager");
                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                    traceEnd();
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    vibrator.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings != null) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                    this.mSystemServiceManager.startBootPhase(480);
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                    this.mSystemServiceManager.startBootPhase(500);
                    traceEnd();
                    traceBeginAndSlog("MakeWindowManagerServiceReady");
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                    }
                    Configuration config22222222222222222222222222222222 = wm.computeNewConfiguration(0);
                    DisplayMetrics metrics22222222222222222222222222222222 = new DisplayMetrics();
                    ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222222222222222222222222);
                    context.getResources().updateConfiguration(config22222222222222222222222222222222, metrics22222222222222222222222222222222);
                    systemTheme = context.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("StartPermissionPolicyService");
                    this.mSystemServiceManager.startService(PermissionPolicyService.class);
                    traceEnd();
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    String[] classes22222222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                    while (r14 < r7) {
                    }
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222222222222222222222222, enableIaware) {
                        /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                        private final /* synthetic */ Context f$1;
                        private final /* synthetic */ CountryDetectorService f$10;
                        private final /* synthetic */ NetworkTimeUpdateService f$11;
                        private final /* synthetic */ InputManagerService f$12;
                        private final /* synthetic */ TelephonyRegistry f$13;
                        private final /* synthetic */ MediaRouterService f$14;
                        private final /* synthetic */ MmsServiceBroker f$15;
                        private final /* synthetic */ boolean f$16;
                        private final /* synthetic */ WindowManagerService f$2;
                        private final /* synthetic */ boolean f$3;
                        private final /* synthetic */ ConnectivityService f$4;
                        private final /* synthetic */ NetworkManagementService f$5;
                        private final /* synthetic */ NetworkPolicyManagerService f$6;
                        private final /* synthetic */ IpSecService f$7;
                        private final /* synthetic */ NetworkStatsService f$8;
                        private final /* synthetic */ LocationManagerService f$9;

                        {
                            this.f$1 = r4;
                            this.f$2 = r5;
                            this.f$3 = r6;
                            this.f$4 = r7;
                            this.f$5 = r8;
                            this.f$6 = r9;
                            this.f$7 = r10;
                            this.f$8 = r11;
                            this.f$9 = r12;
                            this.f$10 = r13;
                            this.f$11 = r14;
                            this.f$12 = r15;
                            this.f$13 = r16;
                            this.f$14 = r17;
                            this.f$15 = r18;
                            this.f$16 = r19;
                        }

                        public final void run() {
                            SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                        }
                    }, BOOT_TIMINGS_TRACE_LOG);
                }
                traceEnd();
                iBinder8 = iBinder5;
            }
            traceBeginAndSlog("StartHardwarePropertiesManagerService");
            try {
                iBinder4 = new HardwarePropertiesManagerService(context);
                ServiceManager.addService("hardware_properties", iBinder4);
                iBinder3 = iBinder4;
            } catch (Throwable th34) {
                e = th34;
                iBinder4 = null;
                Slog.e(TAG, "Failure starting HardwarePropertiesManagerService", e);
                iBinder3 = iBinder4;
                traceEnd();
                traceBeginAndSlog("StartTwilightService");
                this.mSystemServiceManager.startService(TwilightService.class);
                traceEnd();
                traceBeginAndSlog("StartColorDisplay");
                this.mSystemServiceManager.startService(ColorDisplayService.class);
                traceEnd();
                traceBeginAndSlog("StartJobScheduler");
                this.mSystemServiceManager.startService(JobSchedulerService.class);
                traceEnd();
                traceBeginAndSlog("StartSoundTrigger");
                this.mSystemServiceManager.startService(SoundTriggerService.class);
                traceEnd();
                traceBeginAndSlog("StartTrustManager");
                this.mSystemServiceManager.startService(TrustManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                }
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
                traceBeginAndSlog("StartRoleManagerService");
                SystemServiceManager systemServiceManager222222222222222222 = this.mSystemServiceManager;
                Context context2222222222222222222 = this.mSystemContext;
                systemServiceManager222222222222222222.startService(new RoleManagerService(context2222222222222222222, new LegacyRoleResolutionPolicy(context2222222222222222222)));
                traceEnd();
                traceBeginAndSlog("StartVoiceRecognitionManager");
                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                traceEnd();
                if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                }
                traceBeginAndSlog("StartSensorNotification");
                this.mSystemServiceManager.startService(SensorNotificationService.class);
                traceEnd();
                traceBeginAndSlog("StartContextHubSystemService");
                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                traceEnd();
                HwServiceFactory.setupHwServices(context);
                traceBeginAndSlog("StartDiskStatsService");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
                traceEnd();
                traceBeginAndSlog("RuntimeService");
                ServiceManager.addService("runtime", new RuntimeService(context));
                traceEnd();
                if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                }
                if (SUPPORT_HW_ATTESTATION) {
                }
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
                traceBeginAndSlog("CertBlacklister");
                new CertBlacklister(context);
                traceEnd();
                traceBeginAndSlog("StartEmergencyAffordanceService");
                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                traceEnd();
                traceBeginAndSlog("StartDreamManager");
                this.mSystemServiceManager.startService(DreamManagerService.class);
                traceEnd();
                traceBeginAndSlog("AddGraphicsStatsService");
                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
                traceEnd();
                if (CoverageService.ENABLED) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                }
                traceBeginAndSlog("StartRestrictionManager");
                this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                traceEnd();
                traceBeginAndSlog("StartMediaSessionService");
                this.mSystemServiceManager.startService(MediaSessionService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                }
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                }
                if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                }
                traceBeginAndSlog("StartMediaRouterService");
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService222222222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config222222222222222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics222222222222222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics222222222222222222222222222222222);
                context.getResources().updateConfiguration(config222222222222222222222222222222222, metrics222222222222222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes222222222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService222222222222222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            traceBeginAndSlog("StartTwilightService");
            this.mSystemServiceManager.startService(TwilightService.class);
            traceEnd();
            traceBeginAndSlog("StartColorDisplay");
            this.mSystemServiceManager.startService(ColorDisplayService.class);
            traceEnd();
            traceBeginAndSlog("StartJobScheduler");
            this.mSystemServiceManager.startService(JobSchedulerService.class);
            traceEnd();
            traceBeginAndSlog("StartSoundTrigger");
            this.mSystemServiceManager.startService(SoundTriggerService.class);
            traceEnd();
            traceBeginAndSlog("StartTrustManager");
            this.mSystemServiceManager.startService(TrustManagerService.class);
            traceEnd();
            if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                traceBeginAndSlog("StartBackupManager");
                this.mSystemServiceManager.startService(BACKUP_MANAGER_SERVICE_CLASS);
                traceEnd();
            }
            if (this.mPackageManager.hasSystemFeature("android.software.app_widgets") || context.getResources().getBoolean(17891431)) {
                traceBeginAndSlog("StartAppWidgetService");
                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                traceEnd();
            }
            traceBeginAndSlog("StartRoleManagerService");
            SystemServiceManager systemServiceManager2222222222222222222 = this.mSystemServiceManager;
            Context context22222222222222222222 = this.mSystemContext;
            systemServiceManager2222222222222222222.startService(new RoleManagerService(context22222222222222222222, new LegacyRoleResolutionPolicy(context22222222222222222222)));
            traceEnd();
            traceBeginAndSlog("StartVoiceRecognitionManager");
            this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
            traceEnd();
            if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                traceBeginAndSlog("StartGestureLauncher");
                this.mSystemServiceManager.startService(GestureLauncherService.class);
                traceEnd();
            }
            traceBeginAndSlog("StartSensorNotification");
            this.mSystemServiceManager.startService(SensorNotificationService.class);
            traceEnd();
            traceBeginAndSlog("StartContextHubSystemService");
            this.mSystemServiceManager.startService(ContextHubSystemService.class);
            traceEnd();
            HwServiceFactory.setupHwServices(context);
            traceBeginAndSlog("StartDiskStatsService");
            try {
                ServiceManager.addService("diskstats", new DiskStatsService(context));
            } catch (Throwable e30) {
                reportWtf("starting DiskStats Service", e30);
            }
            traceEnd();
            traceBeginAndSlog("RuntimeService");
            try {
                ServiceManager.addService("runtime", new RuntimeService(context));
            } catch (Throwable e31) {
                reportWtf("starting RuntimeService", e31);
            }
            traceEnd();
            if (this.mOnlyCore && context.getResources().getBoolean(17891449)) {
                traceBeginAndSlog("StartTimeZoneRulesManagerService");
                this.mSystemServiceManager.startService(TIME_ZONE_RULES_MANAGER_SERVICE_CLASS);
                traceEnd();
            }
            if (SUPPORT_HW_ATTESTATION) {
                traceBeginAndSlog("StartAttestationService");
                try {
                    Slog.i(TAG, str2);
                    HwServiceFactory.IHwAttestationServiceFactory attestation = HwServiceFactory.getHwAttestationService();
                    if (attestation != null) {
                        ServiceManager.addService("attestation_service", attestation.getInstance(context));
                    }
                } catch (Throwable e32) {
                    Slog.i(TAG, "attestation_service failed");
                    reportWtf(str2, e32);
                }
                traceEnd();
            }
            if (!isWatch && !disableNetworkTime) {
                traceBeginAndSlog("StartNetworkTimeUpdateService");
                networkTimeUpdater2 = new NewNetworkTimeUpdateService(context);
                Slog.d(TAG, "Using networkTimeUpdater class=" + networkTimeUpdater2.getClass());
                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                networkTimeUpdater3 = networkTimeUpdater2;
                traceEnd();
            }
            traceBeginAndSlog("CertBlacklister");
            try {
                new CertBlacklister(context);
            } catch (Throwable e33) {
                reportWtf("starting CertBlacklister", e33);
            }
            traceEnd();
            traceBeginAndSlog("StartEmergencyAffordanceService");
            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
            traceEnd();
            traceBeginAndSlog("StartDreamManager");
            this.mSystemServiceManager.startService(DreamManagerService.class);
            traceEnd();
            traceBeginAndSlog("AddGraphicsStatsService");
            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, (IBinder) new GraphicsStatsService(context));
            traceEnd();
            if (CoverageService.ENABLED) {
                traceBeginAndSlog("AddCoverageService");
                ServiceManager.addService(CoverageService.COVERAGE_SERVICE, new CoverageService());
                traceEnd();
            }
            if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                traceBeginAndSlog("StartPrintManager");
                this.mSystemServiceManager.startService(PRINT_MANAGER_SERVICE_CLASS);
                traceEnd();
            }
            if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                traceBeginAndSlog("StartCompanionDeviceManager");
                this.mSystemServiceManager.startService(COMPANION_DEVICE_MANAGER_SERVICE_CLASS);
                traceEnd();
            }
            traceBeginAndSlog("StartRestrictionManager");
            this.mSystemServiceManager.startService(RestrictionsManagerService.class);
            traceEnd();
            traceBeginAndSlog("StartMediaSessionService");
            this.mSystemServiceManager.startService(MediaSessionService.class);
            traceEnd();
            if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                traceBeginAndSlog("StartHdmiControlService");
                this.mSystemServiceManager.startService(HdmiControlService.class);
                traceEnd();
            }
            if (this.mPackageManager.hasSystemFeature("android.software.live_tv") || this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                traceBeginAndSlog("StartTvInputManager");
                this.mSystemServiceManager.startService(TvInputManagerService.class);
                traceEnd();
            }
            if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                traceBeginAndSlog("StartMediaResourceMonitor");
                this.mSystemServiceManager.startService(MediaResourceMonitorService.class);
                traceEnd();
            }
            if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                traceBeginAndSlog("StartTvRemoteService");
                this.mSystemServiceManager.startService(TvRemoteService.class);
                traceEnd();
            }
            traceBeginAndSlog("StartMediaRouterService");
            try {
                mediaRouterService = new MediaRouterService(context);
                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                mediaRouter2 = mediaRouterService;
            } catch (Throwable th35) {
                e = th35;
                mediaRouter4 = null;
                reportWtf("starting MediaRouterService", e);
                mediaRouter2 = mediaRouter4;
                traceEnd();
                hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                if (hasFeatureFace) {
                }
                if (hasFeatureIris) {
                }
                if (hasFeatureFingerprint2) {
                }
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
                traceBeginAndSlog("StartBackgroundDexOptService");
                BackgroundDexOptService.schedule(context);
                traceEnd();
                if (!isWatch) {
                }
                if (!isWatch) {
                }
                traceBeginAndSlog("StartShortcutServiceLifecycle");
                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartLauncherAppsService");
                this.mSystemServiceManager.startService(LauncherAppsService.class);
                traceEnd();
                traceBeginAndSlog("StartCrossProfileAppsService");
                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                traceEnd();
                location = location2;
                mediaRouter = mediaRouter3;
                hasFeatureFingerprint = tuiEnable2;
                networkTimeUpdater = networkTimeUpdater3;
                ipSecService = ipSecService2;
                iNetworkStatsService = iNetworkStatsService3;
                iConnectivityManager = iConnectivityManager2;
                if (!isWatch) {
                }
                if (isWatch) {
                }
                if (!disableSlices) {
                }
                if (!disableCameraService) {
                }
                if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("StartIncidentCompanionService");
                this.mSystemServiceManager.startService(IncidentCompanionService.class);
                traceEnd();
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                MmsServiceBroker mmsService2222222222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
                }
                traceBeginAndSlog("StartClipboardService");
                this.mSystemServiceManager.startService(ClipboardService.class);
                traceEnd();
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("AppServiceManager");
                this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("MakeVibratorServiceReady");
                vibrator.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings != null) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseLockSettingsReady");
                this.mSystemServiceManager.startBootPhase(480);
                traceEnd();
                traceBeginAndSlog("StartBootPhaseSystemServicesReady");
                this.mSystemServiceManager.startBootPhase(500);
                traceEnd();
                traceBeginAndSlog("MakeWindowManagerServiceReady");
                wm.systemReady();
                traceEnd();
                if (safeMode) {
                }
                Configuration config2222222222222222222222222222222222 = wm.computeNewConfiguration(0);
                DisplayMetrics metrics2222222222222222222222222222222222 = new DisplayMetrics();
                ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics2222222222222222222222222222222222);
                context.getResources().updateConfiguration(config2222222222222222222222222222222222, metrics2222222222222222222222222222222222);
                systemTheme = context.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("StartPermissionPolicyService");
                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                traceEnd();
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                String[] classes2222222222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
                while (r14 < r7) {
                }
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService2222222222222222222222222222222222, enableIaware) {
                    /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
                    private final /* synthetic */ Context f$1;
                    private final /* synthetic */ CountryDetectorService f$10;
                    private final /* synthetic */ NetworkTimeUpdateService f$11;
                    private final /* synthetic */ InputManagerService f$12;
                    private final /* synthetic */ TelephonyRegistry f$13;
                    private final /* synthetic */ MediaRouterService f$14;
                    private final /* synthetic */ MmsServiceBroker f$15;
                    private final /* synthetic */ boolean f$16;
                    private final /* synthetic */ WindowManagerService f$2;
                    private final /* synthetic */ boolean f$3;
                    private final /* synthetic */ ConnectivityService f$4;
                    private final /* synthetic */ NetworkManagementService f$5;
                    private final /* synthetic */ NetworkPolicyManagerService f$6;
                    private final /* synthetic */ IpSecService f$7;
                    private final /* synthetic */ NetworkStatsService f$8;
                    private final /* synthetic */ LocationManagerService f$9;

                    {
                        this.f$1 = r4;
                        this.f$2 = r5;
                        this.f$3 = r6;
                        this.f$4 = r7;
                        this.f$5 = r8;
                        this.f$6 = r9;
                        this.f$7 = r10;
                        this.f$8 = r11;
                        this.f$9 = r12;
                        this.f$10 = r13;
                        this.f$11 = r14;
                        this.f$12 = r15;
                        this.f$13 = r16;
                        this.f$14 = r17;
                        this.f$15 = r18;
                        this.f$16 = r19;
                    }

                    public final void run() {
                        SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
                    }
                }, BOOT_TIMINGS_TRACE_LOG);
            }
            traceEnd();
            hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
            hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
            hasFeatureFingerprint2 = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
            if (hasFeatureFace) {
                traceBeginAndSlog("StartFaceSensor");
                mediaRouter3 = mediaRouter2;
                this.mSystemServiceManager.startService(FaceService.class);
                traceEnd();
            } else {
                mediaRouter3 = mediaRouter2;
            }
            if (hasFeatureIris) {
                traceBeginAndSlog("StartIrisSensor");
                this.mSystemServiceManager.startService(IrisService.class);
                traceEnd();
            }
            if (hasFeatureFingerprint2) {
                traceBeginAndSlog("StartFingerprintSensor");
                try {
                    HwServiceFactory.IHwFingerprintService ifs = HwServiceFactory.getHwFingerprintService();
                    if (ifs != null) {
                        Class<SystemService> serviceClass2 = ifs.createServiceClass();
                        Slog.i(TAG, "serviceClass doesn't null");
                        serviceClass = serviceClass2;
                    } else {
                        Slog.e(TAG, "HwFingerPrintService is null!");
                        serviceClass = null;
                    }
                    if (serviceClass != null) {
                        Slog.i(TAG, "start HwFingerPrintService");
                        this.mSystemServiceManager.startService(serviceClass);
                    } else {
                        this.mSystemServiceManager.startService(FingerprintService.class);
                    }
                    Slog.i(TAG, "FingerPrintService ready");
                } catch (Throwable e34) {
                    Slog.e(TAG, "Start fingerprintservice error", e34);
                }
                traceEnd();
            }
            if (hasFeatureFace || hasFeatureIris || hasFeatureFingerprint2) {
                traceBeginAndSlog("StartBiometricService");
                this.mSystemServiceManager.startService(BiometricService.class);
                traceEnd();
            }
            traceBeginAndSlog("StartBackgroundDexOptService");
            try {
                BackgroundDexOptService.schedule(context);
            } catch (Throwable e35) {
                reportWtf("starting StartBackgroundDexOptService", e35);
            }
            traceEnd();
            if (!isWatch) {
                traceBeginAndSlog("StartDynamicCodeLoggingService");
                try {
                    DynamicCodeLoggingService.schedule(context);
                } catch (Throwable e36) {
                    reportWtf("starting DynamicCodeLoggingService", e36);
                }
                traceEnd();
            }
            if (!isWatch) {
                traceBeginAndSlog("StartPruneInstantAppsJobService");
                try {
                    PruneInstantAppsJobService.schedule(context);
                } catch (Throwable e37) {
                    reportWtf("StartPruneInstantAppsJobService", e37);
                }
                traceEnd();
            }
            traceBeginAndSlog("StartShortcutServiceLifecycle");
            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
            traceEnd();
            traceBeginAndSlog("StartLauncherAppsService");
            this.mSystemServiceManager.startService(LauncherAppsService.class);
            traceEnd();
            traceBeginAndSlog("StartCrossProfileAppsService");
            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
            traceEnd();
            location = location2;
            mediaRouter = mediaRouter3;
            hasFeatureFingerprint = tuiEnable2;
            networkTimeUpdater = networkTimeUpdater3;
            ipSecService = ipSecService2;
            iNetworkStatsService = iNetworkStatsService3;
            iConnectivityManager = iConnectivityManager2;
        } else {
            hasFeatureFingerprint = tuiEnable;
            iNetworkPolicyManager = null;
            mediaRouter = null;
            iNetworkManagementService = null;
            location = null;
            countryDetector = null;
            networkTimeUpdater = null;
            iNetworkStatsService = null;
            iConnectivityManager = null;
        }
        if (!isWatch) {
            traceBeginAndSlog("StartMediaProjectionManager");
            this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
            traceEnd();
        }
        if (isWatch) {
            traceBeginAndSlog("StartWearPowerService");
            this.mSystemServiceManager.startService(WEAR_POWER_SERVICE_CLASS);
            traceEnd();
            traceBeginAndSlog("StartWearConnectivityService");
            this.mSystemServiceManager.startService(WEAR_CONNECTIVITY_SERVICE_CLASS);
            traceEnd();
            traceBeginAndSlog("StartWearDisplayService");
            this.mSystemServiceManager.startService(WEAR_DISPLAY_SERVICE_CLASS);
            traceEnd();
            traceBeginAndSlog("StartWearTimeService");
            this.mSystemServiceManager.startService(WEAR_TIME_SERVICE_CLASS);
            traceEnd();
            if (enableLeftyService) {
                traceBeginAndSlog("StartWearLeftyService");
                this.mSystemServiceManager.startService(WEAR_LEFTY_SERVICE_CLASS);
                traceEnd();
            }
            traceBeginAndSlog("StartWearGlobalActionsService");
            this.mSystemServiceManager.startService(WEAR_GLOBAL_ACTIONS_SERVICE_CLASS);
            traceEnd();
        }
        if (!disableSlices) {
            traceBeginAndSlog("StartSliceManagerService");
            this.mSystemServiceManager.startService(SLICE_MANAGER_SERVICE_CLASS);
            traceEnd();
        }
        if (!disableCameraService) {
            traceBeginAndSlog("StartCameraServiceProxy");
            this.mSystemServiceManager.startService(CameraServiceProxy.class);
            traceEnd();
        }
        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
            traceBeginAndSlog("StartIoTSystemService");
            this.mSystemServiceManager.startService(IOT_SERVICE_CLASS);
            traceEnd();
        }
        traceBeginAndSlog("StartStatsCompanionService");
        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
        traceEnd();
        traceBeginAndSlog("StartIncidentCompanionService");
        this.mSystemServiceManager.startService(IncidentCompanionService.class);
        traceEnd();
        if (safeMode) {
            this.mActivityManagerService.enterSafeMode();
        }
        traceBeginAndSlog("StartMmsService");
        MmsServiceBroker mmsService22222222222222222222222222222222222 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
        traceEnd();
        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
            traceBeginAndSlog("StartAutoFillService");
            this.mSystemServiceManager.startService(AUTO_FILL_MANAGER_SERVICE_CLASS);
            traceEnd();
        }
        if (str3.equals(SystemProperties.get("bastet.service.enable", str))) {
            try {
                traceBeginAndSlog("StartBastetService");
                this.mSystemServiceManager.startService("com.android.server.HwBastetService");
                traceEnd();
            } catch (Throwable th36) {
                Slog.w(TAG, "HwBastetService not exists.");
            }
        }
        traceBeginAndSlog("StartClipboardService");
        this.mSystemServiceManager.startService(ClipboardService.class);
        traceEnd();
        if (isStartSysSvcCallRecord) {
            startSysSvcCallRecordService();
        }
        traceBeginAndSlog("AppServiceManager");
        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
        traceEnd();
        traceBeginAndSlog("MakeVibratorServiceReady");
        try {
            vibrator.systemReady();
        } catch (Throwable e38) {
            reportWtf("making Vibrator Service ready", e38);
        }
        traceEnd();
        traceBeginAndSlog("MakeLockSettingsServiceReady");
        if (lockSettings != null) {
            try {
                lockSettings.systemReady();
            } catch (Throwable e39) {
                reportWtf("making Lock Settings Service ready", e39);
            }
        }
        traceEnd();
        traceBeginAndSlog("StartBootPhaseLockSettingsReady");
        this.mSystemServiceManager.startBootPhase(480);
        traceEnd();
        traceBeginAndSlog("StartBootPhaseSystemServicesReady");
        this.mSystemServiceManager.startBootPhase(500);
        traceEnd();
        traceBeginAndSlog("MakeWindowManagerServiceReady");
        try {
            wm.systemReady();
        } catch (Throwable e40) {
            reportWtf("making Window Manager Service ready", e40);
        }
        traceEnd();
        if (safeMode) {
            this.mActivityManagerService.showSafeModeOverlay();
        }
        Configuration config22222222222222222222222222222222222 = wm.computeNewConfiguration(0);
        DisplayMetrics metrics22222222222222222222222222222222222 = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics22222222222222222222222222222222222);
        context.getResources().updateConfiguration(config22222222222222222222222222222222222, metrics22222222222222222222222222222222222);
        systemTheme = context.getTheme();
        if (systemTheme.getChangingConfigurations() != 0) {
            systemTheme.rebase();
        }
        traceBeginAndSlog("MakePowerManagerServiceReady");
        try {
            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
        } catch (Throwable e41) {
            reportWtf("making Power Manager Service ready", e41);
        }
        traceEnd();
        try {
            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
        } catch (Throwable e42) {
            reportWtf("making PG Manager Service ready", e42);
        }
        traceBeginAndSlog("StartPermissionPolicyService");
        this.mSystemServiceManager.startService(PermissionPolicyService.class);
        traceEnd();
        traceBeginAndSlog("MakePackageManagerServiceReady");
        this.mPackageManagerService.systemReady();
        traceEnd();
        traceBeginAndSlog("MakeDisplayManagerServiceReady");
        try {
            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
        } catch (Throwable e43) {
            reportWtf("making Display Manager Service ready", e43);
        }
        traceEnd();
        this.mSystemServiceManager.setSafeMode(safeMode);
        traceBeginAndSlog("StartDeviceSpecificServices");
        String[] classes22222222222222222222222222222222222 = this.mSystemContext.getResources().getStringArray(17236007);
        for (String className2 : classes22222222222222222222222222222222222) {
            traceBeginAndSlog("StartDeviceSpecificServices " + className2);
            try {
                this.mSystemServiceManager.startService(className2);
            } catch (Throwable e44) {
                reportWtf("starting " + className2, e44);
            }
            traceEnd();
        }
        traceEnd();
        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
        traceEnd();
        this.mActivityManagerService.systemReady(new Runnable(context, wm, safeMode, iConnectivityManager, iNetworkManagementService, iNetworkPolicyManager, ipSecService, iNetworkStatsService, location, countryDetector, networkTimeUpdater, inputManager, telephonyRegistry, mediaRouter, mmsService22222222222222222222222222222222222, enableIaware) {
            /* class com.android.server.$$Lambda$SystemServer$izZXvNBS1sgFBFNX1EVoO0g0o1M */
            private final /* synthetic */ Context f$1;
            private final /* synthetic */ CountryDetectorService f$10;
            private final /* synthetic */ NetworkTimeUpdateService f$11;
            private final /* synthetic */ InputManagerService f$12;
            private final /* synthetic */ TelephonyRegistry f$13;
            private final /* synthetic */ MediaRouterService f$14;
            private final /* synthetic */ MmsServiceBroker f$15;
            private final /* synthetic */ boolean f$16;
            private final /* synthetic */ WindowManagerService f$2;
            private final /* synthetic */ boolean f$3;
            private final /* synthetic */ ConnectivityService f$4;
            private final /* synthetic */ NetworkManagementService f$5;
            private final /* synthetic */ NetworkPolicyManagerService f$6;
            private final /* synthetic */ IpSecService f$7;
            private final /* synthetic */ NetworkStatsService f$8;
            private final /* synthetic */ LocationManagerService f$9;

            {
                this.f$1 = r4;
                this.f$2 = r5;
                this.f$3 = r6;
                this.f$4 = r7;
                this.f$5 = r8;
                this.f$6 = r9;
                this.f$7 = r10;
                this.f$8 = r11;
                this.f$9 = r12;
                this.f$10 = r13;
                this.f$11 = r14;
                this.f$12 = r15;
                this.f$13 = r16;
                this.f$14 = r17;
                this.f$15 = r18;
                this.f$16 = r19;
            }

            public final void run() {
                SystemServer.this.lambda$startOtherServices$5$SystemServer(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
            }
        }, BOOT_TIMINGS_TRACE_LOG);
    }

    static /* synthetic */ void lambda$startOtherServices$2() {
        try {
            Slog.i(TAG, "SecondaryZygotePreload");
            TimingsTraceLog traceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288);
            traceLog.traceBegin("SecondaryZygotePreload");
            if (!Process.ZYGOTE_PROCESS.preloadDefault(Build.SUPPORTED_32_BIT_ABIS[0])) {
                Slog.e(TAG, "Unable to preload default resources");
            }
            traceLog.traceEnd();
        } catch (Exception ex) {
            Slog.e(TAG, "Exception preloading default resources", ex);
        }
    }

    static /* synthetic */ void lambda$startOtherServices$3() {
        TimingsTraceLog traceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288);
        traceLog.traceBegin(START_HIDL_SERVICES);
        startHidlServices();
        traceLog.traceEnd();
    }

    public /* synthetic */ void lambda$startOtherServices$5$SystemServer(Context context, WindowManagerService windowManagerF, boolean safeMode, ConnectivityService connectivityF, NetworkManagementService networkManagementF, NetworkPolicyManagerService networkPolicyF, IpSecService ipSecServiceF, NetworkStatsService networkStatsF, LocationManagerService locationF, CountryDetectorService countryDetectorF, NetworkTimeUpdateService networkTimeUpdaterF, InputManagerService inputManagerF, TelephonyRegistry telephonyRegistryF, MediaRouterService mediaRouterF, MmsServiceBroker mmsServiceF, boolean enableIaware) {
        CountDownLatch networkPolicyInitReadySignal;
        Slog.i(TAG, "Making services ready");
        traceBeginAndSlog("StartActivityManagerReadyPhase");
        this.mSystemServiceManager.startBootPhase(550);
        traceEnd();
        traceBeginAndSlog("StartObservingNativeCrashes");
        try {
            this.mActivityManagerService.startObservingNativeCrashes();
        } catch (Throwable e) {
            reportWtf("observing native crashes", e);
        }
        traceEnd();
        Future<?> webviewPrep = (this.mOnlyCore || this.mWebViewUpdateService == null) ? null : SystemServerInitThreadPool.get().submit(new Runnable() {
            /* class com.android.server.$$Lambda$SystemServer$72PvntN28skIthlRYR9w5EhsdX8 */

            public final void run() {
                SystemServer.this.lambda$startOtherServices$4$SystemServer();
            }
        }, "WebViewFactoryPreparation");
        if (this.mPackageManager.hasSystemFeature("android.hardware.type.automotive")) {
            traceBeginAndSlog("StartCarServiceHelperService");
            this.mSystemServiceManager.startService(CAR_SERVICE_HELPER_SERVICE_CLASS);
            traceEnd();
        }
        traceBeginAndSlog("StartSystemUI");
        try {
            startSystemUi(context, windowManagerF);
        } catch (Throwable e2) {
            reportWtf("starting System UI", e2);
        }
        traceEnd();
        traceBeginAndSlog("StartHwExtDisplayUI");
        try {
            startHwExtDisplayUi(context, windowManagerF);
        } catch (Throwable e3) {
            reportWtf("starting HwExtDisplay UI", e3);
        }
        traceEnd();
        if (safeMode) {
            traceBeginAndSlog("EnableAirplaneModeInSafeMode");
            try {
                connectivityF.setAirplaneMode(true);
            } catch (Throwable e4) {
                reportWtf("enabling Airplane Mode during Safe Mode bootup", e4);
            }
            traceEnd();
        }
        traceBeginAndSlog("MakeNetworkManagementServiceReady");
        if (networkManagementF != null) {
            try {
                networkManagementF.systemReady();
            } catch (Throwable e5) {
                reportWtf("making Network Managment Service ready", e5);
            }
        }
        if (networkPolicyF != null) {
            networkPolicyInitReadySignal = networkPolicyF.networkScoreAndNetworkManagementServiceReady();
        } else {
            networkPolicyInitReadySignal = null;
        }
        traceEnd();
        traceBeginAndSlog("MakeIpSecServiceReady");
        if (ipSecServiceF != null) {
            try {
                ipSecServiceF.systemReady();
            } catch (Throwable e6) {
                reportWtf("making IpSec Service ready", e6);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkStatsServiceReady");
        if (networkStatsF != null) {
            try {
                networkStatsF.systemReady();
            } catch (Throwable e7) {
                reportWtf("making Network Stats Service ready", e7);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeConnectivityServiceReady");
        if (connectivityF != null) {
            try {
                connectivityF.systemReady();
            } catch (Throwable e8) {
                reportWtf("making Connectivity Service ready", e8);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkPolicyServiceReady");
        if (networkPolicyF != null) {
            try {
                networkPolicyF.systemReady(networkPolicyInitReadySignal);
            } catch (Throwable e9) {
                reportWtf("making Network Policy Service ready", e9);
            }
        }
        traceEnd();
        this.mPackageManagerService.waitForAppDataPrepared();
        traceBeginAndSlog("PhaseThirdPartyAppsCanStart");
        if (webviewPrep != null) {
            ConcurrentUtils.waitForFutureNoInterrupt(webviewPrep, "WebViewFactoryPreparation");
        }
        this.mSystemServiceManager.startBootPhase(600);
        traceEnd();
        traceBeginAndSlog("StartNetworkStack");
        try {
            NetworkStackClient.getInstance().start(context);
        } catch (Throwable e10) {
            reportWtf("starting Network Stack", e10);
        }
        traceEnd();
        traceBeginAndSlog("MakeLocationServiceReady");
        if (locationF != null) {
            try {
                locationF.systemRunning();
            } catch (Throwable e11) {
                reportWtf("Notifying Location Service running", e11);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeCountryDetectionServiceReady");
        if (countryDetectorF != null) {
            try {
                countryDetectorF.systemRunning();
            } catch (Throwable e12) {
                reportWtf("Notifying CountryDetectorService running", e12);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkTimeUpdateReady");
        if (networkTimeUpdaterF != null) {
            try {
                networkTimeUpdaterF.systemRunning();
            } catch (Throwable e13) {
                reportWtf("Notifying NetworkTimeService running", e13);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeInputManagerServiceReady");
        if (inputManagerF != null) {
            try {
                inputManagerF.systemRunning();
            } catch (Throwable e14) {
                reportWtf("Notifying InputManagerService running", e14);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeTelephonyRegistryReady");
        if (telephonyRegistryF != null) {
            try {
                telephonyRegistryF.systemRunning();
            } catch (Throwable e15) {
                reportWtf("Notifying TelephonyRegistry running", e15);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeMediaRouterServiceReady");
        if (mediaRouterF != null) {
            try {
                mediaRouterF.systemRunning();
            } catch (Throwable e16) {
                reportWtf("Notifying MediaRouterService running", e16);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeMmsServiceReady");
        if (mmsServiceF != null) {
            try {
                mmsServiceF.systemRunning();
            } catch (Throwable e17) {
                reportWtf("Notifying MmsService running", e17);
            }
        }
        traceEnd();
        traceBeginAndSlog("IncidentDaemonReady");
        try {
            IIncidentManager incident = IIncidentManager.Stub.asInterface(ServiceManager.getService("incident"));
            if (incident != null) {
                incident.systemRunning();
            }
        } catch (Throwable e18) {
            reportWtf("Notifying incident daemon running", e18);
        }
        traceEnd();
        if (enableIaware) {
            try {
                ServiceManager.addService("multi_task", HwServiceFactory.getMultiTaskManagerService().getInstance(context));
            } catch (Throwable e19) {
                reportWtf("starting MultiTaskManagerService", e19);
            }
        } else {
            Slog.e(TAG, "can not start multitask because the prop is false");
        }
        if (HwPCUtils.enabled() && !safeMode) {
            traceBeginAndSlog("StartPCManagerService");
            HwServiceFactory.addHwPCManagerService(context, this.mActivityManagerService);
            traceEnd();
        }
        HwServiceFactory.addHwFmService(context);
        HwServiceFactory.updateLocalesWhenOTAEX(context, this.mPackageManagerService.getSdkVersion());
        if (HwMwUtils.ENABLED) {
            traceBeginAndSlog("StartHwMagicManagerService");
            HwServiceFactory.addHwMagicWindowService(context, this.mActivityManagerService, windowManagerF);
            traceEnd();
        }
    }

    public /* synthetic */ void lambda$startOtherServices$4$SystemServer() {
        Slog.i(TAG, "WebViewFactoryPreparation");
        TimingsTraceLog traceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288);
        traceLog.traceBegin("WebViewFactoryPreparation");
        ConcurrentUtils.waitForFutureNoInterrupt(this.mZygotePreload, "Zygote preload");
        this.mZygotePreload = null;
        this.mWebViewUpdateService.prepareWebViewInSystemServer();
        traceLog.traceEnd();
    }

    private void startSystemCaptionsManagerService(Context context) {
        if (TextUtils.isEmpty(context.getString(17039829))) {
            Slog.d(TAG, "SystemCaptionsManagerService disabled because resource is not overlaid");
            return;
        }
        traceBeginAndSlog("StartSystemCaptionsManagerService");
        this.mSystemServiceManager.startService(SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS);
        traceEnd();
    }

    private void startContentCaptureService(Context context) {
        ActivityManagerService activityManagerService;
        boolean explicitlyEnabled = false;
        String settings = DeviceConfig.getProperty("content_capture", "service_explicitly_enabled");
        if (settings != null && !settings.equalsIgnoreCase(BatteryService.HealthServiceWrapper.INSTANCE_VENDOR)) {
            explicitlyEnabled = Boolean.parseBoolean(settings);
            if (explicitlyEnabled) {
                Slog.d(TAG, "ContentCaptureService explicitly enabled by DeviceConfig");
            } else {
                Slog.d(TAG, "ContentCaptureService explicitly disabled by DeviceConfig");
                return;
            }
        }
        if (explicitlyEnabled || !TextUtils.isEmpty(context.getString(17039820))) {
            traceBeginAndSlog("StartContentCaptureService");
            this.mSystemServiceManager.startService(CONTENT_CAPTURE_MANAGER_SERVICE_CLASS);
            ContentCaptureManagerInternal ccmi = (ContentCaptureManagerInternal) LocalServices.getService(ContentCaptureManagerInternal.class);
            if (!(ccmi == null || (activityManagerService = this.mActivityManagerService) == null)) {
                activityManagerService.setContentCaptureManager(ccmi);
            }
            traceEnd();
            return;
        }
        Slog.d(TAG, "ContentCaptureService disabled because resource is not overlaid");
    }

    private void startAttentionService(Context context) {
        if (!AttentionManagerService.isServiceConfigured(context)) {
            Slog.d(TAG, "AttentionService is not configured on this device");
            return;
        }
        traceBeginAndSlog("StartAttentionManagerService");
        this.mSystemServiceManager.startService(AttentionManagerService.class);
        traceEnd();
    }

    private static void startSystemUi(Context context, WindowManagerService windowManager) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService"));
        intent.addFlags(256);
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
        windowManager.onSystemUiStarted();
    }

    private static void traceBeginAndSlog(String name) {
        Slog.i(TAG, name);
        MetricsLogger.histogram((Context) null, "boot_system_server_" + name, (int) SystemClock.elapsedRealtime());
        BOOT_TIMINGS_TRACE_LOG.traceBegin(name);
    }

    private static void traceEnd() {
        BOOT_TIMINGS_TRACE_LOG.traceEnd();
    }

    static final void startHwExtDisplayUi(Context context, WindowManagerService windowManager) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.huawei.android.extdisplay", "com.huawei.android.extdisplay.HwExtDisplayUIService"));
        intent.addFlags(256);
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
    }
}
