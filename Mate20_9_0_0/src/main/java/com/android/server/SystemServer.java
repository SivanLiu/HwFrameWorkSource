package com.android.server;

import android.app.ActivityThread;
import android.app.INotificationManager;
import android.app.usage.UsageStatsManagerInternal;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.database.sqlite.SQLiteCompatibilityWalFlags;
import android.hsm.HwSystemManager;
import android.net.INetd;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FactoryTest;
import android.os.FileUtils;
import android.os.IIncidentManager;
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
import android.os.storage.IStorageManager.Stub;
import android.provider.Settings.Global;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.HwPCUtils;
import android.util.Jlog;
import android.util.Slog;
import android.util.TimingsTraceLog;
import android.view.WindowManager;
import android.vr.VRManagerService;
import com.android.internal.app.ColorDisplayController;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BinderInternal;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.widget.ILockSettings;
import com.android.server.HwServiceFactory.IHwAttestationServiceFactory;
import com.android.server.HwServiceFactory.IHwFingerprintService;
import com.android.server.HwServiceFactory.IHwForceRotationManagerServiceWrapper;
import com.android.server.HwServiceFactory.IHwLocationManagerService;
import com.android.server.HwServiceFactory.IHwTelephonyRegistry;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityManagerService.Lifecycle;
import com.android.server.audio.AudioService;
import com.android.server.camera.CameraServiceProxy;
import com.android.server.clipboard.ClipboardService;
import com.android.server.connectivity.IpConnectivityMetrics;
import com.android.server.coverage.CoverageService;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.display.ColorDisplayService;
import com.android.server.display.DisplayManagerService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.emergency.EmergencyAffordanceService;
import com.android.server.fingerprint.FingerprintService;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.input.InputManagerService;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.lights.LightsService;
import com.android.server.media.MediaResourceMonitorService;
import com.android.server.media.MediaRouterService;
import com.android.server.media.MediaSessionService;
import com.android.server.media.MediaUpdateService;
import com.android.server.media.dtv.DTVService;
import com.android.server.media.projection.MediaProjectionManagerService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.net.watchlist.NetworkWatchlistService;
import com.android.server.notification.NotificationManagerService;
import com.android.server.oemlock.OemLockService;
import com.android.server.om.OverlayManagerService;
import com.android.server.os.DeviceIdentifiersPolicyService;
import com.android.server.os.HwBootCheck;
import com.android.server.os.HwBootFail;
import com.android.server.os.SchedulingPolicyService;
import com.android.server.pg.PGManagerService;
import com.android.server.pm.BackgroundDexOptService;
import com.android.server.pm.CrossProfileAppsService;
import com.android.server.pm.Installer;
import com.android.server.pm.LauncherAppsService;
import com.android.server.pm.OtaDexoptService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.UserManagerService.LifeCycle;
import com.android.server.policy.HwPolicyFactory;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.restrictions.RestrictionsManagerService;
import com.android.server.security.KeyAttestationApplicationIdProviderService;
import com.android.server.security.KeyChainSystemService;
import com.android.server.soundtrigger.SoundTriggerService;
import com.android.server.stats.StatsCompanionService;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.telecom.TelecomLoaderService;
import com.android.server.textclassifier.TextClassificationManagerService;
import com.android.server.trust.TrustManagerService;
import com.android.server.tv.TvInputManagerService;
import com.android.server.tv.TvRemoteService;
import com.android.server.twilight.TwilightService;
import com.android.server.usage.UsageStatsService;
import com.android.server.utils.LogBufferUtil;
import com.android.server.vr.VrManagerService;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.wm.WindowManagerService;
import com.huawei.featurelayer.HwFeatureLoader.SystemServiceFeature;
import dalvik.system.VMRuntime;
import huawei.android.app.HwCustEmergDataManager;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public final class SystemServer {
    private static final String ACCOUNT_SERVICE_CLASS = "com.android.server.accounts.AccountManagerService$Lifecycle";
    private static final String APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService";
    private static final String AUTO_FILL_MANAGER_SERVICE_CLASS = "com.android.server.autofill.AutofillManagerService";
    private static final String BACKUP_MANAGER_SERVICE_CLASS = "com.android.server.backup.BackupManagerService$Lifecycle";
    private static final String BLOCK_MAP_FILE = "/cache/recovery/block.map";
    private static final TimingsTraceLog BOOT_TIMINGS_TRACE_LOG = new TimingsTraceLog(SYSTEM_SERVER_TIMING_TAG, 524288);
    private static final String CAR_SERVICE_HELPER_SERVICE_CLASS = "com.android.internal.car.CarServiceHelperService";
    private static final String COMPANION_DEVICE_MANAGER_SERVICE_CLASS = "com.android.server.companion.CompanionDeviceManagerService";
    private static final String CONTENT_SERVICE_CLASS = "com.android.server.content.ContentService$Lifecycle";
    private static final int DEFAULT_SYSTEM_THEME = 16974803;
    private static final long EARLIEST_SUPPORTED_TIME = 86400000;
    private static final String ENCRYPTED_STATE = "1";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ETHERNET_SERVICE_CLASS = "com.android.server.ethernet.EthernetService";
    public static final int FIRST_ON_SMART_FHD = 1;
    private static final String IOT_SERVICE_CLASS = "com.google.android.things.services.IoTSystemService";
    private static final String JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService";
    private static final boolean LOCAL_LOGV = true;
    private static final String LOCK_SETTINGS_SERVICE_CLASS = "com.android.server.locksettings.LockSettingsService$Lifecycle";
    private static final String LOWPAN_SERVICE_CLASS = "com.android.server.lowpan.LowpanService";
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
    private static final String SYSTEM_SERVER_TIMING_ASYNC_TAG = "SystemServerTimingAsync";
    private static final String SYSTEM_SERVER_TIMING_TAG = "SystemServerTiming";
    private static final String TAG = "SystemServer";
    private static final String THERMAL_OBSERVER_CLASS = "com.google.android.clockwork.ThermalObserver";
    private static final String TIME_ZONE_RULES_MANAGER_SERVICE_CLASS = "com.android.server.timezone.RulesManagerService$Lifecycle";
    private static final String UNCRYPT_PACKAGE_FILE = "/cache/recovery/uncrypt_file";
    private static final String USB_SERVICE_CLASS = "com.android.server.usb.UsbService$Lifecycle";
    private static final String VOICE_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.voiceinteraction.VoiceInteractionManagerService";
    private static final String WEAR_CONFIG_SERVICE_CLASS = "com.google.android.clockwork.WearConfigManagerService";
    private static final String WEAR_CONNECTIVITY_SERVICE_CLASS = "com.android.clockwork.connectivity.WearConnectivityService";
    private static final String WEAR_DISPLAY_SERVICE_CLASS = "com.google.android.clockwork.display.WearDisplayService";
    private static final String WEAR_GLOBAL_ACTIONS_SERVICE_CLASS = "com.android.clockwork.globalactions.GlobalActionsService";
    private static final String WEAR_LEFTY_SERVICE_CLASS = "com.google.android.clockwork.lefty.WearLeftyService";
    private static final String WEAR_SIDEKICK_SERVICE_CLASS = "com.google.android.clockwork.sidekick.SidekickService";
    private static final String WEAR_TIME_SERVICE_CLASS = "com.google.android.clockwork.time.WearTimeService";
    private static final String WIFI_AWARE_SERVICE_CLASS = "com.android.server.wifi.aware.WifiAwareService";
    private static final String WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService";
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    private static final int sMaxBinderThreads = 31;
    private ActivityManagerService mActivityManagerService;
    private ContentResolver mContentResolver;
    private DisplayManagerService mDisplayManagerService;
    private EntropyMixer mEntropyMixer;
    private final int mFactoryTestMode = FactoryTest.getMode();
    private boolean mFirstBoot;
    private boolean mOnlyCore;
    private PGManagerService mPGManagerService;
    private PackageManager mPackageManager;
    private PackageManagerService mPackageManagerService;
    private PowerManagerService mPowerManagerService;
    private Timer mProfilerSnapshotTimer;
    private final boolean mRuntimeRestart;
    private final long mRuntimeStartElapsedTime;
    private final long mRuntimeStartUptime;
    private Future<?> mSensorServiceStart;
    private Context mSystemContext;
    private SystemServiceManager mSystemServiceManager;
    private WebViewUpdateService mWebViewUpdateService;
    private Future<?> mZygotePreload;

    private static native void startHidlServices();

    private static native void startSensorService();

    private static native void startSysSvcCallRecordService();

    public static void main(String[] args) {
        new SystemServer().run();
    }

    public SystemServer() {
        if (this.mFactoryTestMode != 0) {
            Jlog.d(26, "JL_FIRST_BOOT");
        }
        this.mRuntimeRestart = ENCRYPTED_STATE.equals(SystemProperties.get("sys.boot_completed"));
        this.mRuntimeStartElapsedTime = SystemClock.elapsedRealtime();
        this.mRuntimeStartUptime = SystemClock.uptimeMillis();
    }

    private void run() {
        try {
            traceBeginAndSlog("InitBeforeStartServices");
            if (System.currentTimeMillis() < 86400000) {
                Slog.w(TAG, "System clock is before 1970; setting to 1970.");
                SystemClock.setCurrentTimeMillis(86400000);
            }
            if (!SystemProperties.get("persist.sys.language").isEmpty()) {
                SystemProperties.set("persist.sys.locale", Locale.getDefault().toLanguageTag());
                SystemProperties.set("persist.sys.language", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                SystemProperties.set("persist.sys.country", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                SystemProperties.set("persist.sys.localevar", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            Binder.setWarnOnBlocking(true);
            PackageItemInfo.setForceSafeLabels(true);
            SQLiteCompatibilityWalFlags.init(null);
            Slog.i(TAG, "Entered the Android system server!");
            int uptimeMillis = (int) SystemClock.elapsedRealtime();
            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_SYSTEM_RUN, uptimeMillis);
            if (!this.mRuntimeRestart) {
                MetricsLogger.histogram(null, "boot_system_server_init", uptimeMillis);
                Jlog.d(30, "JL_BOOT_PROGRESS_SYSTEM_RUN");
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
            performPendingShutdown();
            createSystemContext();
            SystemServiceFeature.loadFeatureFramework(this.mSystemContext);
            this.mSystemServiceManager = new SystemServiceManager(this.mSystemContext);
            this.mSystemServiceManager.setStartInfo(this.mRuntimeRestart, this.mRuntimeStartElapsedTime, this.mRuntimeStartUptime);
            LocalServices.addService(SystemServiceManager.class, this.mSystemServiceManager);
            SystemServerInitThreadPool.get();
            try {
                traceBeginAndSlog("StartServices");
                startBootstrapServices();
                startCoreServices();
                startOtherServices();
                SystemServerInitThreadPool.shutdown();
                Slog.i(TAG, "Finish_StartServices");
                traceEnd();
                this.mPackageManagerService.onSystemServiceStartComplete();
                StrictMode.initVmDefaults(null);
                if (!(this.mRuntimeRestart || isFirstBootOrUpgrade())) {
                    int uptimeMillis2 = (int) SystemClock.elapsedRealtime();
                    MetricsLogger.histogram(null, "boot_system_server_ready", uptimeMillis2);
                    if (uptimeMillis2 > 60000) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("SystemServer init took too long. uptimeMillis=");
                        stringBuilder.append(uptimeMillis2);
                        Slog.wtf(SYSTEM_SERVER_TIMING_TAG, stringBuilder.toString());
                    }
                }
                LogBufferUtil.closeLogBufferAsNeed(this.mSystemContext);
                SmartShrinker.reclaim(Process.myPid(), 3);
                Looper.loop();
                throw new RuntimeException("Main thread loop unexpectedly exited");
            } catch (Throwable th) {
                Slog.i(TAG, "Finish_StartServices");
                traceEnd();
            }
        } finally {
            traceEnd();
        }
    }

    private boolean isFirstBootOrUpgrade() {
        return this.mPackageManagerService.isFirstBoot() || this.mPackageManagerService.isUpgrade();
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BOOT FAILURE ");
        stringBuilder.append(msg);
        Slog.wtf(str, stringBuilder.toString(), e);
    }

    private void performPendingShutdown() {
        String shutdownAction = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        if (shutdownAction != null && shutdownAction.length() > 0) {
            String reason;
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
                    if (!(filename == null || !filename.startsWith("/data") || new File(BLOCK_MAP_FILE).exists())) {
                        Slog.e(TAG, "Can't find block map file, uncrypt failed or unexpected runtime restart?");
                        return;
                    }
                }
            }
            Message msg = Message.obtain(UiThread.getHandler(), new Runnable() {
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

    private void startBootstrapServices() {
        Slog.i(TAG, "Reading configuration...");
        String TAG_SYSTEM_CONFIG = "ReadingSystemConfig";
        traceBeginAndSlog("ReadingSystemConfig");
        SystemServerInitThreadPool.get().submit(-$$Lambda$YWiwiKm_Qgqb55C6tTuq_n2JzdY.INSTANCE, "ReadingSystemConfig");
        traceEnd();
        traceBeginAndSlog("StartInstaller");
        Installer installer = (Installer) this.mSystemServiceManager.startService(Installer.class);
        traceEnd();
        traceBeginAndSlog("DeviceIdentifiersPolicyService");
        this.mSystemServiceManager.startService(DeviceIdentifiersPolicyService.class);
        traceEnd();
        traceBeginAndSlog("StartActivityManager");
        this.mActivityManagerService = ((Lifecycle) this.mSystemServiceManager.startService(Lifecycle.class)).getService();
        this.mActivityManagerService.setSystemServiceManager(this.mSystemServiceManager);
        this.mActivityManagerService.setInstaller(installer);
        traceEnd();
        traceBeginAndSlog("StartPowerManager");
        try {
            this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService("com.android.server.power.HwPowerManagerService");
        } catch (RuntimeException e) {
            Slog.w(TAG, "create HwPowerManagerService failed");
            this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService(PowerManagerService.class);
        }
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
        String cryptState = SystemProperties.get("vold.decrypt");
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
            MetricsLogger.histogram(null, "boot_package_manager_init_start", (int) SystemClock.elapsedRealtime());
        }
        HwCustEmergDataManager emergDataManager = HwCustEmergDataManager.getDefault();
        if (emergDataManager != null && emergDataManager.isEmergencyState()) {
            this.mOnlyCore = true;
            if (emergDataManager.isEmergencyMountState()) {
                emergDataManager.backupEmergencyDataFile();
            }
        }
        traceBeginAndSlog("StartPackageManagerService");
        Slog.i(TAG, "Package Manager");
        Context context = this.mSystemContext;
        if (this.mFactoryTestMode == 0) {
            z = false;
        }
        this.mPackageManagerService = PackageManagerService.main(context, installer, z, this.mOnlyCore);
        this.mFirstBoot = this.mPackageManagerService.isFirstBoot();
        this.mPackageManager = this.mSystemContext.getPackageManager();
        Slog.i(TAG, "Finish_StartPackageManagerService");
        traceEnd();
        if (!(this.mRuntimeRestart || isFirstBootOrUpgrade())) {
            MetricsLogger.histogram(null, "boot_package_manager_init_ready", (int) SystemClock.elapsedRealtime());
        }
        HwBootFail.setBootStage(HwBootFail.STAGE_FRAMEWORK_JAR_DEXOPT_END);
        HwBootCheck.bootSceneEnd(105);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[bootinfo]\nisFirstBoot: ");
        stringBuilder.append(this.mFirstBoot);
        stringBuilder.append("\nisUpgrade: ");
        stringBuilder.append(this.mPackageManagerService.isUpgrade());
        HwBootCheck.addBootInfo(stringBuilder.toString());
        HwBootCheck.bootSceneStart(101, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
        if (!(this.mOnlyCore || SystemProperties.getBoolean("config.disable_otadexopt", false))) {
            traceBeginAndSlog("StartOtaDexOptService");
            try {
                OtaDexoptService.main(this.mSystemContext, this.mPackageManagerService);
            } catch (Throwable th) {
                traceEnd();
            }
            traceEnd();
        }
        traceBeginAndSlog("StartUserManagerService");
        this.mSystemServiceManager.startService(LifeCycle.class);
        traceEnd();
        if (this.mFirstBoot && this.mPackageManagerService.isUpgrade()) {
            Jlog.d(26, "JL_FIRST_BOOT");
        }
        traceBeginAndSlog("InitAttributerCache");
        AttributeCache.init(this.mSystemContext);
        traceEnd();
        traceBeginAndSlog("SetSystemProcess");
        this.mActivityManagerService.setSystemProcess();
        traceEnd();
        this.mDisplayManagerService.setupSchedulerPolicies();
        traceBeginAndSlog("StartOverlayManagerService");
        this.mSystemServiceManager.startService(new OverlayManagerService(this.mSystemContext, installer));
        traceEnd();
        this.mSensorServiceStart = SystemServerInitThreadPool.get().submit(-$$Lambda$SystemServer$UyrPns7R814g-ZEylCbDKhe8It4.INSTANCE, START_SENSOR_SERVICE);
    }

    static /* synthetic */ void lambda$startBootstrapServices$0() {
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
        traceBeginAndSlog("StartBinderCallsStatsService");
        BinderCallsStatsService.start();
        traceEnd();
    }

    /* JADX WARNING: Removed duplicated region for block: B:56:0x0207  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x01d7 A:{SYNTHETIC, Splitter: B:45:0x01d7} */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x02af A:{Catch:{ RuntimeException -> 0x0414 }} */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x02ad A:{Catch:{ RuntimeException -> 0x0414 }} */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x031b A:{Catch:{ RuntimeException -> 0x03e2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x0353 A:{Catch:{ RuntimeException -> 0x03e2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:108:0x034b A:{Catch:{ RuntimeException -> 0x03e2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x03b1 A:{Catch:{ RuntimeException -> 0x03e2 }} */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x097c  */
    /* JADX WARNING: Removed duplicated region for block: B:366:0x09a0  */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x09e0  */
    /* JADX WARNING: Removed duplicated region for block: B:377:0x0a11  */
    /* JADX WARNING: Removed duplicated region for block: B:397:0x0a74  */
    /* JADX WARNING: Removed duplicated region for block: B:385:0x0a47  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x08e4 A:{SYNTHETIC, Splitter: B:323:0x08e4} */
    /* JADX WARNING: Removed duplicated region for block: B:328:0x08fa  */
    /* JADX WARNING: Removed duplicated region for block: B:338:0x093b A:{Catch:{ Throwable -> 0x094a }} */
    /* JADX WARNING: Removed duplicated region for block: B:337:0x0936 A:{Catch:{ Throwable -> 0x094a }} */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x097c  */
    /* JADX WARNING: Removed duplicated region for block: B:366:0x09a0  */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x09e0  */
    /* JADX WARNING: Removed duplicated region for block: B:377:0x0a11  */
    /* JADX WARNING: Removed duplicated region for block: B:385:0x0a47  */
    /* JADX WARNING: Removed duplicated region for block: B:397:0x0a74  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x08e4 A:{SYNTHETIC, Splitter: B:323:0x08e4} */
    /* JADX WARNING: Removed duplicated region for block: B:328:0x08fa  */
    /* JADX WARNING: Removed duplicated region for block: B:337:0x0936 A:{Catch:{ Throwable -> 0x094a }} */
    /* JADX WARNING: Removed duplicated region for block: B:338:0x093b A:{Catch:{ Throwable -> 0x094a }} */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x097c  */
    /* JADX WARNING: Removed duplicated region for block: B:366:0x09a0  */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x09e0  */
    /* JADX WARNING: Removed duplicated region for block: B:377:0x0a11  */
    /* JADX WARNING: Removed duplicated region for block: B:397:0x0a74  */
    /* JADX WARNING: Removed duplicated region for block: B:385:0x0a47  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x097c  */
    /* JADX WARNING: Removed duplicated region for block: B:366:0x09a0  */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x09e0  */
    /* JADX WARNING: Removed duplicated region for block: B:377:0x0a11  */
    /* JADX WARNING: Removed duplicated region for block: B:385:0x0a47  */
    /* JADX WARNING: Removed duplicated region for block: B:397:0x0a74  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x097c  */
    /* JADX WARNING: Removed duplicated region for block: B:366:0x09a0  */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x09e0  */
    /* JADX WARNING: Removed duplicated region for block: B:377:0x0a11  */
    /* JADX WARNING: Removed duplicated region for block: B:397:0x0a74  */
    /* JADX WARNING: Removed duplicated region for block: B:385:0x0a47  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:323:0x08e4 A:{SYNTHETIC, Splitter: B:323:0x08e4} */
    /* JADX WARNING: Removed duplicated region for block: B:328:0x08fa  */
    /* JADX WARNING: Removed duplicated region for block: B:338:0x093b A:{Catch:{ Throwable -> 0x094a }} */
    /* JADX WARNING: Removed duplicated region for block: B:337:0x0936 A:{Catch:{ Throwable -> 0x094a }} */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x097c  */
    /* JADX WARNING: Removed duplicated region for block: B:366:0x09a0  */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x09e0  */
    /* JADX WARNING: Removed duplicated region for block: B:377:0x0a11  */
    /* JADX WARNING: Removed duplicated region for block: B:385:0x0a47  */
    /* JADX WARNING: Removed duplicated region for block: B:397:0x0a74  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:411:0x0ab2  */
    /* JADX WARNING: Removed duplicated region for block: B:418:0x0af1  */
    /* JADX WARNING: Removed duplicated region for block: B:426:0x0b3f  */
    /* JADX WARNING: Removed duplicated region for block: B:434:0x0b8d  */
    /* JADX WARNING: Removed duplicated region for block: B:440:0x0ba0  */
    /* JADX WARNING: Removed duplicated region for block: B:444:0x0bbc A:{Catch:{ Throwable -> 0x0bcd }} */
    /* JADX WARNING: Removed duplicated region for block: B:449:0x0bca  */
    /* JADX WARNING: Removed duplicated region for block: B:454:0x0bde  */
    /* JADX WARNING: Removed duplicated region for block: B:482:0x0c68  */
    /* JADX WARNING: Removed duplicated region for block: B:485:0x0c84  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x0c9d  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x0ce3  */
    /* JADX WARNING: Removed duplicated region for block: B:499:0x0d1f  */
    /* JADX WARNING: Removed duplicated region for block: B:502:0x0d38  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:540:0x0ddb  */
    /* JADX WARNING: Removed duplicated region for block: B:516:0x0d76  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x04cd  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x05a8  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x05bc  */
    /* JADX WARNING: Removed duplicated region for block: B:552:0x0e4c  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x05fc  */
    /* JADX WARNING: Removed duplicated region for block: B:554:0x0e6c  */
    /* JADX WARNING: Removed duplicated region for block: B:557:0x0e80  */
    /* JADX WARNING: Removed duplicated region for block: B:562:0x0ed6  */
    /* JADX WARNING: Removed duplicated region for block: B:564:0x0ee7  */
    /* JADX WARNING: Removed duplicated region for block: B:567:0x0f02  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x0f40  */
    /* JADX WARNING: Removed duplicated region for block: B:570:0x0f2b  */
    /* JADX WARNING: Removed duplicated region for block: B:574:0x0f6d  */
    /* JADX WARNING: Removed duplicated region for block: B:577:0x0f8d A:{SYNTHETIC, Splitter: B:577:0x0f8d} */
    /* JADX WARNING: Removed duplicated region for block: B:582:0x0f9f  */
    /* JADX WARNING: Removed duplicated region for block: B:590:0x0fbd A:{SYNTHETIC, Splitter: B:590:0x0fbd} */
    /* JADX WARNING: Removed duplicated region for block: B:601:0x1000  */
    /* JADX WARNING: Removed duplicated region for block: B:604:0x1032  */
    /* JADX WARNING: Removed duplicated region for block: B:622:0x10a2  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void startOtherServices() {
        boolean z;
        RuntimeException e;
        InputManagerService inputManager;
        TelephonyRegistry telephonyRegistry;
        IStorageManager storageManager;
        ConnectivityService connectivity;
        VibratorService telephonyRegistry2;
        AlarmManagerService almService;
        boolean tuiEnable;
        int tuiEnable2;
        AlarmManagerService alarmManagerService;
        WindowManagerService wm;
        InputManagerService inputManager2;
        StatusBarManagerService statusBar;
        ILockSettings lockSettings;
        IStorageManager storageManager2;
        INotificationManager notification;
        boolean safeMode;
        MmsServiceBroker mmsService;
        ILockSettings lockSettings2;
        Configuration config;
        DisplayMetrics metrics;
        WindowManager w;
        Theme systemTheme;
        LocationManagerService location;
        String[] classes;
        int length;
        ILockSettings lockSettings3;
        int lockSettings4;
        Context context;
        WindowManagerService wm2;
        NetworkManagementService networkManagementF;
        NetworkManagementService networkManagement;
        NetworkStatsService networkStatsF;
        NetworkStatsService networkStats;
        NetworkPolicyManagerService networkPolicyF;
        NetworkPolicyManagerService networkPolicy;
        InputManagerService inputManager3;
        ConnectivityService connectivityF;
        LocationManagerService locationF;
        CountryDetectorService countryDetectorF;
        CountryDetectorService countryDetector;
        NetworkTimeUpdateService networkTimeUpdaterF;
        NetworkTimeUpdateService networkTimeUpdater;
        CommonTimeManagementService commonTimeMgmtServiceF;
        CommonTimeManagementService commonTimeMgmtService;
        InputManagerService inputManagerF;
        TelephonyRegistry telephonyRegistryF;
        MediaRouterService mediaRouterF;
        MediaRouterService mediaRouter;
        MmsServiceBroker mmsServiceF;
        IpSecService ipSecServiceF;
        IpSecService ipSecService;
        ActivityManagerService activityManagerService;
        -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe;
        -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
        TelephonyRegistry telephonyRegistry3;
        HwCustEmergDataManager hwCustEmergDataManager;
        InputManagerService inputManager4;
        AlarmManagerService almService2;
        Throwable e2;
        InputManagerService inputManagerService;
        StatusBarManagerService statusBarManagerService;
        NsdService serviceDiscovery;
        NsdService serviceDiscovery2;
        INotificationManager notification2;
        String str;
        StringBuilder stringBuilder;
        INotificationManager notification3;
        IHwLocationManagerService hwLocation;
        LocationManagerService location2;
        CountryDetectorService countryDetector2;
        boolean z2;
        HardwarePropertiesManagerService hardwarePropertiesService;
        IHwAttestationServiceFactory attestation;
        CertBlacklister certBlacklister;
        Binder statusBar2;
        SerialService serialService;
        HardwarePropertiesManagerService hardwarePropertiesManagerService;
        LocationManagerService location3;
        NsdService lockSettings5;
        SerialService serial;
        HardwarePropertiesManagerService hardwarePropertiesService2;
        Throwable th;
        Context context2 = this.mSystemContext;
        NetworkManagementService networkManagement2 = null;
        IpSecService ipSecService2 = null;
        NetworkStatsService networkStats2 = null;
        NetworkPolicyManagerService networkPolicy2 = null;
        WindowManagerService wm3 = null;
        SerialService serial2 = null;
        NetworkTimeUpdateService networkTimeUpdater2 = null;
        HwCustEmergDataManager emergDataManager = HwCustEmergDataManager.getDefault();
        if (!(emergDataManager == null || emergDataManager.isEmergencyState())) {
            HwServiceFactory.activePlaceFile();
        }
        boolean disableSystemTextClassifier = SystemProperties.getBoolean("config.disable_systemtextclassifier", false);
        boolean disableCameraService = SystemProperties.getBoolean("config.disable_cameraservice", false);
        boolean disableSlices = SystemProperties.getBoolean("config.disable_slices", false);
        boolean enableLeftyService = SystemProperties.getBoolean("config.enable_lefty", false);
        boolean isEmulator = SystemProperties.get("ro.kernel.qemu").equals(ENCRYPTED_STATE);
        boolean enableRms = SystemProperties.getBoolean("ro.config.enable_rms", false);
        boolean enableIaware = SystemProperties.getBoolean("ro.config.enable_iaware", false);
        boolean tuiEnable3 = SystemProperties.getBoolean("ro.vendor.tui.service", false);
        Binder binder = null;
        boolean vrDisplayEnable = SystemProperties.getBoolean("ro.vr_display.service", false);
        boolean dtvEnable = SystemProperties.getBoolean("ro.dtv.service", false);
        AlarmManagerService alarmManagerService2 = null;
        boolean isSupportedSecIme = "CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
        boolean isWatch = context2.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        boolean z3 = "3".equals(SystemProperties.get("ro.logsystem.usertype", "0")) && "true".equals(SystemProperties.get("ro.syssvccallrecord.enable", "false"));
        boolean isStartSysSvcCallRecord = z3;
        if (Build.IS_DEBUGGABLE) {
            z = false;
            if (SystemProperties.getBoolean("debug.crash_system", false)) {
                throw new RuntimeException();
            }
        }
        z = false;
        try {
            AlarmManagerService almService3;
            Watchdog watchdog;
            Watchdog watchdog2;
            VibratorService vibratorService;
            String SECONDARY_ZYGOTE_PRELOAD = "SecondaryZygotePreload";
            try {
                TelephonyRegistry telephonyRegistry4;
                this.mZygotePreload = SystemServerInitThreadPool.get().submit(-$$Lambda$SystemServer$VBGb9VpEls6bUcVBPwYLtX7qDTs.INSTANCE, "SecondaryZygotePreload");
                traceBeginAndSlog("StartKeyAttestationApplicationIdProviderService");
                ServiceManager.addService("sec_key_att_app_id_provider", new KeyAttestationApplicationIdProviderService(context2));
                traceEnd();
                traceBeginAndSlog("StartKeyChainSystemService");
                this.mSystemServiceManager.startService(KeyChainSystemService.class);
                traceEnd();
                traceBeginAndSlog("StartSchedulingPolicyService");
                ServiceManager.addService("scheduling_policy", new SchedulingPolicyService());
                traceEnd();
                traceBeginAndSlog("StartTelecomLoaderService");
                this.mSystemServiceManager.startService(TelecomLoaderService.class);
                traceEnd();
                traceBeginAndSlog("StartTelephonyRegistry");
                Slog.i(TAG, "Telephony Registry");
                if (HwSystemManager.mPermissionEnabled == 0) {
                    try {
                        telephonyRegistry4 = new TelephonyRegistry(context2);
                    } catch (RuntimeException e3) {
                        e = e3;
                        inputManager = null;
                        telephonyRegistry = null;
                        storageManager = null;
                        connectivity = null;
                        telephonyRegistry2 = binder;
                        almService = alarmManagerService2;
                        tuiEnable = tuiEnable3;
                        tuiEnable2 = 1;
                        Slog.e("System", "******************************************");
                        Slog.e("System", "************ Failure starting core service", e);
                        alarmManagerService = almService;
                        wm = wm3;
                        inputManager2 = inputManager;
                        statusBar = null;
                        lockSettings = null;
                        if (this.mFactoryTestMode != tuiEnable2) {
                        }
                        traceBeginAndSlog("MakeDisplayReady");
                        wm.displayReady();
                        traceEnd();
                        traceBeginAndSlog("StartStorageManagerService");
                        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                        storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                        traceEnd();
                        traceBeginAndSlog("StartStorageStatsService");
                        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                        traceEnd();
                        storageManager = storageManager2;
                        traceBeginAndSlog("StartUiModeManager");
                        this.mSystemServiceManager.startService(UiModeManagerService.class);
                        traceEnd();
                        HwBootCheck.bootSceneEnd(101);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                        notification = null;
                        if (!this.mRuntimeRestart) {
                        }
                        HwBootCheck.bootSceneStart(104, 900000);
                        if (!this.mOnlyCore) {
                        }
                        traceBeginAndSlog("PerformFstrimIfNeeded");
                        this.mPackageManagerService.performFstrimIfNeeded();
                        traceEnd();
                        HwBootCheck.bootSceneEnd(104);
                        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                        if (this.mFactoryTestMode == 1) {
                        }
                        if (!isWatch) {
                        }
                        MediaProjectionManagerService.sHasStartedInSystemserver = true;
                        if (isWatch) {
                        }
                        if (!disableSlices) {
                        }
                        if (!disableCameraService) {
                        }
                        if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        safeMode = wm.detectSafeMode();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                        }
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        telephonyRegistry2.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings2 != null) {
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
                        config = wm.computeNewConfiguration(0);
                        metrics = new DisplayMetrics();
                        w = (WindowManager) context2.getSystemService("window");
                        w.getDefaultDisplay().getMetrics(metrics);
                        context2.getResources().updateConfiguration(config, metrics);
                        systemTheme = context2.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        classes = this.mSystemContext.getResources().getStringArray(17236002);
                        length = classes.length;
                        lockSettings3 = lockSettings2;
                        lockSettings4 = 0;
                        while (lockSettings4 < length) {
                        }
                        context = context2;
                        wm2 = wm;
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        networkManagementF = networkManagement;
                        networkStatsF = networkStats;
                        networkPolicyF = networkPolicy;
                        inputManager3 = inputManager2;
                        connectivityF = connectivity;
                        locationF = location;
                        countryDetectorF = countryDetector;
                        networkTimeUpdaterF = networkTimeUpdater;
                        commonTimeMgmtServiceF = commonTimeMgmtService;
                        inputManagerF = inputManager3;
                        telephonyRegistryF = telephonyRegistry;
                        mediaRouterF = mediaRouter;
                        mmsServiceF = mmsService;
                        ipSecServiceF = ipSecService;
                        wm = wm2;
                        activityManagerService = this.mActivityManagerService;
                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                        activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                    }
                }
                IHwTelephonyRegistry itr = HwServiceFactory.getHwTelephonyRegistry();
                if (itr != null) {
                    telephonyRegistry3 = itr.getInstance(context2);
                    ServiceManager.addService("telephony.registry", telephonyRegistry3);
                    traceEnd();
                    traceBeginAndSlog("StartEntropyMixer");
                    this.mEntropyMixer = new EntropyMixer(context2);
                    traceEnd();
                    this.mContentResolver = context2.getContentResolver();
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
                    telephonyRegistry2 = new VibratorService(context2);
                    ServiceManager.addService("vibrator", telephonyRegistry2);
                    traceEnd();
                    if (isWatch) {
                        try {
                            traceBeginAndSlog("StartConsumerIrService");
                            try {
                                inputManager = null;
                                try {
                                    SystemServerInitThreadPool.get().submit(new -$$Lambda$SystemServer$cH_FacXLboZKirnIdTLWPzV_1Gc(context2), "StartConsumerIrService");
                                    traceEnd();
                                } catch (RuntimeException e4) {
                                    e = e4;
                                }
                            } catch (RuntimeException e5) {
                                e = e5;
                                inputManager = null;
                            }
                        } catch (RuntimeException e6) {
                            e = e6;
                            inputManager = null;
                            telephonyRegistry = telephonyRegistry3;
                            hwCustEmergDataManager = emergDataManager;
                            storageManager = null;
                            connectivity = null;
                            almService = alarmManagerService2;
                            tuiEnable = tuiEnable3;
                            tuiEnable2 = 1;
                            Slog.e("System", "******************************************");
                            Slog.e("System", "************ Failure starting core service", e);
                            alarmManagerService = almService;
                            wm = wm3;
                            inputManager2 = inputManager;
                            statusBar = null;
                            lockSettings = null;
                            if (this.mFactoryTestMode != tuiEnable2) {
                            }
                            traceBeginAndSlog("MakeDisplayReady");
                            wm.displayReady();
                            traceEnd();
                            traceBeginAndSlog("StartStorageManagerService");
                            this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                            storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                            traceEnd();
                            traceBeginAndSlog("StartStorageStatsService");
                            this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                            traceEnd();
                            storageManager = storageManager2;
                            traceBeginAndSlog("StartUiModeManager");
                            this.mSystemServiceManager.startService(UiModeManagerService.class);
                            traceEnd();
                            HwBootCheck.bootSceneEnd(101);
                            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                            notification = null;
                            if (this.mRuntimeRestart) {
                            }
                            HwBootCheck.bootSceneStart(104, 900000);
                            if (this.mOnlyCore) {
                            }
                            traceBeginAndSlog("PerformFstrimIfNeeded");
                            this.mPackageManagerService.performFstrimIfNeeded();
                            traceEnd();
                            HwBootCheck.bootSceneEnd(104);
                            HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                            if (this.mFactoryTestMode == 1) {
                            }
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                    }
                    inputManager = null;
                    traceBeginAndSlog("StartAlarmManagerService");
                    try {
                        almService3 = (AlarmManagerService) this.mSystemServiceManager.startService("com.android.server.HwAlarmManagerService");
                    } catch (Exception e7) {
                        this.mSystemServiceManager.startService(AlarmManagerService.class);
                        almService3 = null;
                    }
                    almService = almService3;
                    try {
                        traceEnd();
                        this.mActivityManagerService.setAlarmManager(almService);
                        traceBeginAndSlog("Init Watchdog");
                        watchdog = Watchdog.getInstance();
                        watchdog.init(context2, this.mActivityManagerService);
                        traceEnd();
                        traceBeginAndSlog("StartInputManagerService");
                        watchdog2 = watchdog;
                        Slog.i(TAG, "Input Manager");
                        inputManager4 = HwServiceFactory.getHwInputManagerService().getInstance(context2, null);
                        try {
                            traceEnd();
                            traceBeginAndSlog("StartHwSysResManagerService");
                            if (enableRms || enableIaware) {
                                try {
                                    this.mSystemServiceManager.startService("com.android.server.rms.HwSysResManagerService");
                                } catch (RuntimeException e8) {
                                    e = e8;
                                    telephonyRegistry = telephonyRegistry3;
                                    hwCustEmergDataManager = emergDataManager;
                                    storageManager = null;
                                    connectivity = null;
                                    inputManager = inputManager4;
                                    tuiEnable = tuiEnable3;
                                    tuiEnable2 = 1;
                                    Slog.e("System", "******************************************");
                                    Slog.e("System", "************ Failure starting core service", e);
                                    alarmManagerService = almService;
                                    wm = wm3;
                                    inputManager2 = inputManager;
                                    statusBar = null;
                                    lockSettings = null;
                                    if (this.mFactoryTestMode != tuiEnable2) {
                                    }
                                    traceBeginAndSlog("MakeDisplayReady");
                                    wm.displayReady();
                                    traceEnd();
                                    traceBeginAndSlog("StartStorageManagerService");
                                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                                    storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                                    traceEnd();
                                    traceBeginAndSlog("StartStorageStatsService");
                                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                                    traceEnd();
                                    storageManager = storageManager2;
                                    traceBeginAndSlog("StartUiModeManager");
                                    this.mSystemServiceManager.startService(UiModeManagerService.class);
                                    traceEnd();
                                    HwBootCheck.bootSceneEnd(101);
                                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                                    notification = null;
                                    if (this.mRuntimeRestart) {
                                    }
                                    HwBootCheck.bootSceneStart(104, 900000);
                                    if (this.mOnlyCore) {
                                    }
                                    traceBeginAndSlog("PerformFstrimIfNeeded");
                                    this.mPackageManagerService.performFstrimIfNeeded();
                                    traceEnd();
                                    HwBootCheck.bootSceneEnd(104);
                                    HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                                    if (this.mFactoryTestMode == 1) {
                                    }
                                    if (isWatch) {
                                    }
                                    MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                    if (isWatch) {
                                    }
                                    if (disableSlices) {
                                    }
                                    if (disableCameraService) {
                                    }
                                    if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                    }
                                    traceBeginAndSlog("StartStatsCompanionService");
                                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                    traceEnd();
                                    safeMode = wm.detectSafeMode();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    if (safeMode) {
                                    }
                                    traceBeginAndSlog("StartMmsService");
                                    mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                    }
                                    if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                    }
                                    if (isStartSysSvcCallRecord) {
                                    }
                                    traceBeginAndSlog("MakeVibratorServiceReady");
                                    telephonyRegistry2.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                                    if (lockSettings2 != null) {
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
                                    config = wm.computeNewConfiguration(0);
                                    metrics = new DisplayMetrics();
                                    w = (WindowManager) context2.getSystemService("window");
                                    w.getDefaultDisplay().getMetrics(metrics);
                                    context2.getResources().updateConfiguration(config, metrics);
                                    systemTheme = context2.getTheme();
                                    if (systemTheme.getChangingConfigurations() != 0) {
                                    }
                                    traceBeginAndSlog("MakePowerManagerServiceReady");
                                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                    traceEnd();
                                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                    traceBeginAndSlog("MakePackageManagerServiceReady");
                                    this.mPackageManagerService.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                    traceEnd();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    traceBeginAndSlog("StartDeviceSpecificServices");
                                    classes = this.mSystemContext.getResources().getStringArray(17236002);
                                    length = classes.length;
                                    lockSettings3 = lockSettings2;
                                    lockSettings4 = 0;
                                    while (lockSettings4 < length) {
                                    }
                                    context = context2;
                                    wm2 = wm;
                                    traceEnd();
                                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                    traceEnd();
                                    networkManagementF = networkManagement;
                                    networkStatsF = networkStats;
                                    networkPolicyF = networkPolicy;
                                    inputManager3 = inputManager2;
                                    connectivityF = connectivity;
                                    locationF = location;
                                    countryDetectorF = countryDetector;
                                    networkTimeUpdaterF = networkTimeUpdater;
                                    commonTimeMgmtServiceF = commonTimeMgmtService;
                                    inputManagerF = inputManager3;
                                    telephonyRegistryF = telephonyRegistry;
                                    mediaRouterF = mediaRouter;
                                    mmsServiceF = mmsService;
                                    ipSecServiceF = ipSecService;
                                    wm = wm2;
                                    activityManagerService = this.mActivityManagerService;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                    activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                } catch (Throwable e22) {
                                    almService2 = almService;
                                    try {
                                        Slog.e(TAG, e22.toString());
                                    } catch (RuntimeException e9) {
                                        e = e9;
                                        telephonyRegistry = telephonyRegistry3;
                                        hwCustEmergDataManager = emergDataManager;
                                        storageManager = null;
                                        connectivity = null;
                                        inputManagerService = inputManager4;
                                        alarmManagerService = almService2;
                                        tuiEnable = tuiEnable3;
                                        tuiEnable2 = 1;
                                        inputManager = inputManagerService;
                                        almService = alarmManagerService;
                                        Slog.e("System", "******************************************");
                                        Slog.e("System", "************ Failure starting core service", e);
                                        alarmManagerService = almService;
                                        wm = wm3;
                                        inputManager2 = inputManager;
                                        statusBar = null;
                                        lockSettings = null;
                                        if (this.mFactoryTestMode != tuiEnable2) {
                                        }
                                        traceBeginAndSlog("MakeDisplayReady");
                                        wm.displayReady();
                                        traceEnd();
                                        traceBeginAndSlog("StartStorageManagerService");
                                        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                                        storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                                        traceEnd();
                                        traceBeginAndSlog("StartStorageStatsService");
                                        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                                        traceEnd();
                                        storageManager = storageManager2;
                                        traceBeginAndSlog("StartUiModeManager");
                                        this.mSystemServiceManager.startService(UiModeManagerService.class);
                                        traceEnd();
                                        HwBootCheck.bootSceneEnd(101);
                                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                                        notification = null;
                                        if (this.mRuntimeRestart) {
                                        }
                                        HwBootCheck.bootSceneStart(104, 900000);
                                        if (this.mOnlyCore) {
                                        }
                                        traceBeginAndSlog("PerformFstrimIfNeeded");
                                        this.mPackageManagerService.performFstrimIfNeeded();
                                        traceEnd();
                                        HwBootCheck.bootSceneEnd(104);
                                        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                                        if (this.mFactoryTestMode == 1) {
                                        }
                                        if (isWatch) {
                                        }
                                        MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                        if (isWatch) {
                                        }
                                        if (disableSlices) {
                                        }
                                        if (disableCameraService) {
                                        }
                                        if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                        }
                                        traceBeginAndSlog("StartStatsCompanionService");
                                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                        traceEnd();
                                        safeMode = wm.detectSafeMode();
                                        this.mSystemServiceManager.setSafeMode(safeMode);
                                        if (safeMode) {
                                        }
                                        traceBeginAndSlog("StartMmsService");
                                        mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                        traceEnd();
                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                        }
                                        if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                        }
                                        if (isStartSysSvcCallRecord) {
                                        }
                                        traceBeginAndSlog("MakeVibratorServiceReady");
                                        telephonyRegistry2.systemReady();
                                        traceEnd();
                                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                                        if (lockSettings2 != null) {
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
                                        config = wm.computeNewConfiguration(0);
                                        metrics = new DisplayMetrics();
                                        w = (WindowManager) context2.getSystemService("window");
                                        w.getDefaultDisplay().getMetrics(metrics);
                                        context2.getResources().updateConfiguration(config, metrics);
                                        systemTheme = context2.getTheme();
                                        if (systemTheme.getChangingConfigurations() != 0) {
                                        }
                                        traceBeginAndSlog("MakePowerManagerServiceReady");
                                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                        traceEnd();
                                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                        traceBeginAndSlog("MakePackageManagerServiceReady");
                                        this.mPackageManagerService.systemReady();
                                        traceEnd();
                                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                        traceEnd();
                                        this.mSystemServiceManager.setSafeMode(safeMode);
                                        traceBeginAndSlog("StartDeviceSpecificServices");
                                        classes = this.mSystemContext.getResources().getStringArray(17236002);
                                        length = classes.length;
                                        lockSettings3 = lockSettings2;
                                        lockSettings4 = 0;
                                        while (lockSettings4 < length) {
                                        }
                                        context = context2;
                                        wm2 = wm;
                                        traceEnd();
                                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                        traceEnd();
                                        networkManagementF = networkManagement;
                                        networkStatsF = networkStats;
                                        networkPolicyF = networkPolicy;
                                        inputManager3 = inputManager2;
                                        connectivityF = connectivity;
                                        locationF = location;
                                        countryDetectorF = countryDetector;
                                        networkTimeUpdaterF = networkTimeUpdater;
                                        commonTimeMgmtServiceF = commonTimeMgmtService;
                                        inputManagerF = inputManager3;
                                        telephonyRegistryF = telephonyRegistry;
                                        mediaRouterF = mediaRouter;
                                        mmsServiceF = mmsService;
                                        ipSecServiceF = ipSecService;
                                        wm = wm2;
                                        activityManagerService = this.mActivityManagerService;
                                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                        activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                    }
                                }
                            }
                            almService2 = almService;
                            traceEnd();
                            traceBeginAndSlog("StartWindowManagerService");
                            ConcurrentUtils.waitForFutureNoInterrupt(this.mSensorServiceStart, START_SENSOR_SERVICE);
                            this.mSensorServiceStart = null;
                        } catch (RuntimeException e10) {
                            e = e10;
                            vibratorService = telephonyRegistry2;
                            telephonyRegistry = telephonyRegistry3;
                            hwCustEmergDataManager = emergDataManager;
                            storageManager = null;
                            connectivity = null;
                            tuiEnable = tuiEnable3;
                            tuiEnable2 = 1;
                            inputManager = inputManager4;
                            almService = almService;
                            Slog.e("System", "******************************************");
                            Slog.e("System", "************ Failure starting core service", e);
                            alarmManagerService = almService;
                            wm = wm3;
                            inputManager2 = inputManager;
                            statusBar = null;
                            lockSettings = null;
                            if (this.mFactoryTestMode != tuiEnable2) {
                            }
                            traceBeginAndSlog("MakeDisplayReady");
                            wm.displayReady();
                            traceEnd();
                            traceBeginAndSlog("StartStorageManagerService");
                            this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                            storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                            traceEnd();
                            traceBeginAndSlog("StartStorageStatsService");
                            this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                            traceEnd();
                            storageManager = storageManager2;
                            traceBeginAndSlog("StartUiModeManager");
                            this.mSystemServiceManager.startService(UiModeManagerService.class);
                            traceEnd();
                            HwBootCheck.bootSceneEnd(101);
                            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                            notification = null;
                            if (this.mRuntimeRestart) {
                            }
                            HwBootCheck.bootSceneStart(104, 900000);
                            if (this.mOnlyCore) {
                            }
                            traceBeginAndSlog("PerformFstrimIfNeeded");
                            this.mPackageManagerService.performFstrimIfNeeded();
                            traceEnd();
                            HwBootCheck.bootSceneEnd(104);
                            HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                            if (this.mFactoryTestMode == 1) {
                            }
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        try {
                            alarmManagerService = almService2;
                            vibratorService = telephonyRegistry2;
                            connectivity = null;
                            tuiEnable = tuiEnable3;
                            telephonyRegistry = telephonyRegistry3;
                            storageManager = null;
                            hwCustEmergDataManager = emergDataManager;
                            try {
                                wm = WindowManagerService.main(context2, inputManager4, this.mFactoryTestMode == 1, this.mFirstBoot ^ 1, this.mOnlyCore, HwPolicyFactory.getHwPhoneWindowManager());
                                try {
                                    initRogMode(wm, context2);
                                    processMultiDPI(wm);
                                    ServiceManager.addService("window", wm, false, 17);
                                    inputManagerService = inputManager4;
                                    tuiEnable2 = 1;
                                    try {
                                        ServiceManager.addService("input", inputManagerService, false, 1);
                                        traceEnd();
                                        traceBeginAndSlog("SetWindowManagerService");
                                        this.mActivityManagerService.setWindowManager(wm);
                                        traceEnd();
                                        traceBeginAndSlog("WindowManagerServiceOnInitReady");
                                        wm.onInitReady();
                                        traceEnd();
                                        SystemServerInitThreadPool.get().submit(-$$Lambda$SystemServer$JQH6ND0PqyyiRiz7lXLvUmRhwRM.INSTANCE, START_HIDL_SERVICES);
                                        if (!isWatch) {
                                            traceBeginAndSlog("StartVrManagerService");
                                            this.mSystemServiceManager.startService(VrManagerService.class);
                                            traceEnd();
                                        }
                                        traceBeginAndSlog("StartInputManager");
                                        inputManagerService.setWindowManagerCallbacks(wm.getInputMonitor());
                                        inputManagerService.start();
                                        traceEnd();
                                        traceBeginAndSlog("DisplayManagerWindowManagerAndInputReady");
                                        this.mDisplayManagerService.windowManagerAndInputReady();
                                        traceEnd();
                                        if (!isEmulator) {
                                            Slog.i(TAG, "No Bluetooth Service (emulator)");
                                        } else if (this.mFactoryTestMode == 1) {
                                            Slog.i(TAG, "No Bluetooth Service (factory test)");
                                        } else if (context2.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
                                            traceBeginAndSlog("StartBluetoothService");
                                            this.mSystemServiceManager.startService(BluetoothService.class);
                                            traceEnd();
                                        } else {
                                            Slog.i(TAG, "No Bluetooth Service (Bluetooth Hardware Not Present)");
                                        }
                                        traceBeginAndSlog("IpConnectivityMetrics");
                                        this.mSystemServiceManager.startService(IpConnectivityMetrics.class);
                                        traceEnd();
                                        traceBeginAndSlog("NetworkWatchlistService");
                                        this.mSystemServiceManager.startService(NetworkWatchlistService.Lifecycle.class);
                                        traceEnd();
                                        traceBeginAndSlog("PinnerService");
                                        this.mSystemServiceManager.startService(PinnerService.class);
                                        traceEnd();
                                        if (dtvEnable) {
                                            Slog.i(TAG, "To add DTVService");
                                            ServiceManager.addService("dtvservice", new DTVService());
                                        }
                                        traceBeginAndSlog("ZrHungService");
                                        try {
                                            this.mSystemServiceManager.startService("com.android.server.zrhung.ZRHungService");
                                        } catch (Throwable e222) {
                                            Slog.e(TAG, e222.toString());
                                        }
                                        traceEnd();
                                        inputManager2 = inputManagerService;
                                        telephonyRegistry2 = vibratorService;
                                    } catch (RuntimeException e11) {
                                        e = e11;
                                        wm3 = wm;
                                        inputManager = inputManagerService;
                                        almService = alarmManagerService;
                                        telephonyRegistry2 = vibratorService;
                                        Slog.e("System", "******************************************");
                                        Slog.e("System", "************ Failure starting core service", e);
                                        alarmManagerService = almService;
                                        wm = wm3;
                                        inputManager2 = inputManager;
                                        statusBar = null;
                                        lockSettings = null;
                                        if (this.mFactoryTestMode != tuiEnable2) {
                                        }
                                        traceBeginAndSlog("MakeDisplayReady");
                                        wm.displayReady();
                                        traceEnd();
                                        traceBeginAndSlog("StartStorageManagerService");
                                        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                                        storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                                        traceEnd();
                                        traceBeginAndSlog("StartStorageStatsService");
                                        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                                        traceEnd();
                                        storageManager = storageManager2;
                                        traceBeginAndSlog("StartUiModeManager");
                                        this.mSystemServiceManager.startService(UiModeManagerService.class);
                                        traceEnd();
                                        HwBootCheck.bootSceneEnd(101);
                                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                                        notification = null;
                                        if (this.mRuntimeRestart) {
                                        }
                                        HwBootCheck.bootSceneStart(104, 900000);
                                        if (this.mOnlyCore) {
                                        }
                                        traceBeginAndSlog("PerformFstrimIfNeeded");
                                        this.mPackageManagerService.performFstrimIfNeeded();
                                        traceEnd();
                                        HwBootCheck.bootSceneEnd(104);
                                        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                                        if (this.mFactoryTestMode == 1) {
                                        }
                                        if (isWatch) {
                                        }
                                        MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                        if (isWatch) {
                                        }
                                        if (disableSlices) {
                                        }
                                        if (disableCameraService) {
                                        }
                                        if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                        }
                                        traceBeginAndSlog("StartStatsCompanionService");
                                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                        traceEnd();
                                        safeMode = wm.detectSafeMode();
                                        this.mSystemServiceManager.setSafeMode(safeMode);
                                        if (safeMode) {
                                        }
                                        traceBeginAndSlog("StartMmsService");
                                        mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                        traceEnd();
                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                        }
                                        if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                        }
                                        if (isStartSysSvcCallRecord) {
                                        }
                                        traceBeginAndSlog("MakeVibratorServiceReady");
                                        telephonyRegistry2.systemReady();
                                        traceEnd();
                                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                                        if (lockSettings2 != null) {
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
                                        config = wm.computeNewConfiguration(0);
                                        metrics = new DisplayMetrics();
                                        w = (WindowManager) context2.getSystemService("window");
                                        w.getDefaultDisplay().getMetrics(metrics);
                                        context2.getResources().updateConfiguration(config, metrics);
                                        systemTheme = context2.getTheme();
                                        if (systemTheme.getChangingConfigurations() != 0) {
                                        }
                                        traceBeginAndSlog("MakePowerManagerServiceReady");
                                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                        traceEnd();
                                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                        traceBeginAndSlog("MakePackageManagerServiceReady");
                                        this.mPackageManagerService.systemReady();
                                        traceEnd();
                                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                        traceEnd();
                                        this.mSystemServiceManager.setSafeMode(safeMode);
                                        traceBeginAndSlog("StartDeviceSpecificServices");
                                        classes = this.mSystemContext.getResources().getStringArray(17236002);
                                        length = classes.length;
                                        lockSettings3 = lockSettings2;
                                        lockSettings4 = 0;
                                        while (lockSettings4 < length) {
                                        }
                                        context = context2;
                                        wm2 = wm;
                                        traceEnd();
                                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                        traceEnd();
                                        networkManagementF = networkManagement;
                                        networkStatsF = networkStats;
                                        networkPolicyF = networkPolicy;
                                        inputManager3 = inputManager2;
                                        connectivityF = connectivity;
                                        locationF = location;
                                        countryDetectorF = countryDetector;
                                        networkTimeUpdaterF = networkTimeUpdater;
                                        commonTimeMgmtServiceF = commonTimeMgmtService;
                                        inputManagerF = inputManager3;
                                        telephonyRegistryF = telephonyRegistry;
                                        mediaRouterF = mediaRouter;
                                        mmsServiceF = mmsService;
                                        ipSecServiceF = ipSecService;
                                        wm = wm2;
                                        activityManagerService = this.mActivityManagerService;
                                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                        activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                    }
                                } catch (RuntimeException e12) {
                                    e = e12;
                                    inputManagerService = inputManager4;
                                    tuiEnable2 = 1;
                                    wm3 = wm;
                                    inputManager = inputManagerService;
                                    almService = alarmManagerService;
                                    telephonyRegistry2 = vibratorService;
                                    Slog.e("System", "******************************************");
                                    Slog.e("System", "************ Failure starting core service", e);
                                    alarmManagerService = almService;
                                    wm = wm3;
                                    inputManager2 = inputManager;
                                    statusBar = null;
                                    lockSettings = null;
                                    if (this.mFactoryTestMode != tuiEnable2) {
                                    }
                                    traceBeginAndSlog("MakeDisplayReady");
                                    wm.displayReady();
                                    traceEnd();
                                    traceBeginAndSlog("StartStorageManagerService");
                                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                                    storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                                    traceEnd();
                                    traceBeginAndSlog("StartStorageStatsService");
                                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                                    traceEnd();
                                    storageManager = storageManager2;
                                    traceBeginAndSlog("StartUiModeManager");
                                    this.mSystemServiceManager.startService(UiModeManagerService.class);
                                    traceEnd();
                                    HwBootCheck.bootSceneEnd(101);
                                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                                    notification = null;
                                    if (this.mRuntimeRestart) {
                                    }
                                    HwBootCheck.bootSceneStart(104, 900000);
                                    if (this.mOnlyCore) {
                                    }
                                    traceBeginAndSlog("PerformFstrimIfNeeded");
                                    this.mPackageManagerService.performFstrimIfNeeded();
                                    traceEnd();
                                    HwBootCheck.bootSceneEnd(104);
                                    HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                                    if (this.mFactoryTestMode == 1) {
                                    }
                                    if (isWatch) {
                                    }
                                    MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                    if (isWatch) {
                                    }
                                    if (disableSlices) {
                                    }
                                    if (disableCameraService) {
                                    }
                                    if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                    }
                                    traceBeginAndSlog("StartStatsCompanionService");
                                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                    traceEnd();
                                    safeMode = wm.detectSafeMode();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    if (safeMode) {
                                    }
                                    traceBeginAndSlog("StartMmsService");
                                    mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                    }
                                    if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                    }
                                    if (isStartSysSvcCallRecord) {
                                    }
                                    traceBeginAndSlog("MakeVibratorServiceReady");
                                    telephonyRegistry2.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                                    if (lockSettings2 != null) {
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
                                    config = wm.computeNewConfiguration(0);
                                    metrics = new DisplayMetrics();
                                    w = (WindowManager) context2.getSystemService("window");
                                    w.getDefaultDisplay().getMetrics(metrics);
                                    context2.getResources().updateConfiguration(config, metrics);
                                    systemTheme = context2.getTheme();
                                    if (systemTheme.getChangingConfigurations() != 0) {
                                    }
                                    traceBeginAndSlog("MakePowerManagerServiceReady");
                                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                    traceEnd();
                                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                    traceBeginAndSlog("MakePackageManagerServiceReady");
                                    this.mPackageManagerService.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                    traceEnd();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    traceBeginAndSlog("StartDeviceSpecificServices");
                                    classes = this.mSystemContext.getResources().getStringArray(17236002);
                                    length = classes.length;
                                    lockSettings3 = lockSettings2;
                                    lockSettings4 = 0;
                                    while (lockSettings4 < length) {
                                    }
                                    context = context2;
                                    wm2 = wm;
                                    traceEnd();
                                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                    traceEnd();
                                    networkManagementF = networkManagement;
                                    networkStatsF = networkStats;
                                    networkPolicyF = networkPolicy;
                                    inputManager3 = inputManager2;
                                    connectivityF = connectivity;
                                    locationF = location;
                                    countryDetectorF = countryDetector;
                                    networkTimeUpdaterF = networkTimeUpdater;
                                    commonTimeMgmtServiceF = commonTimeMgmtService;
                                    inputManagerF = inputManager3;
                                    telephonyRegistryF = telephonyRegistry;
                                    mediaRouterF = mediaRouter;
                                    mmsServiceF = mmsService;
                                    ipSecServiceF = ipSecService;
                                    wm = wm2;
                                    activityManagerService = this.mActivityManagerService;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                    activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                }
                            } catch (RuntimeException e13) {
                                e = e13;
                                inputManagerService = inputManager4;
                                tuiEnable2 = 1;
                                inputManager = inputManagerService;
                                almService = alarmManagerService;
                                telephonyRegistry2 = vibratorService;
                                Slog.e("System", "******************************************");
                                Slog.e("System", "************ Failure starting core service", e);
                                alarmManagerService = almService;
                                wm = wm3;
                                inputManager2 = inputManager;
                                statusBar = null;
                                lockSettings = null;
                                if (this.mFactoryTestMode != tuiEnable2) {
                                }
                                traceBeginAndSlog("MakeDisplayReady");
                                wm.displayReady();
                                traceEnd();
                                traceBeginAndSlog("StartStorageManagerService");
                                this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                                storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                                traceEnd();
                                traceBeginAndSlog("StartStorageStatsService");
                                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                                traceEnd();
                                storageManager = storageManager2;
                                traceBeginAndSlog("StartUiModeManager");
                                this.mSystemServiceManager.startService(UiModeManagerService.class);
                                traceEnd();
                                HwBootCheck.bootSceneEnd(101);
                                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                                notification = null;
                                if (this.mRuntimeRestart) {
                                }
                                HwBootCheck.bootSceneStart(104, 900000);
                                if (this.mOnlyCore) {
                                }
                                traceBeginAndSlog("PerformFstrimIfNeeded");
                                this.mPackageManagerService.performFstrimIfNeeded();
                                traceEnd();
                                HwBootCheck.bootSceneEnd(104);
                                HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                                if (this.mFactoryTestMode == 1) {
                                }
                                if (isWatch) {
                                }
                                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                if (isWatch) {
                                }
                                if (disableSlices) {
                                }
                                if (disableCameraService) {
                                }
                                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                }
                                traceBeginAndSlog("StartStatsCompanionService");
                                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                traceEnd();
                                safeMode = wm.detectSafeMode();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                if (safeMode) {
                                }
                                traceBeginAndSlog("StartMmsService");
                                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                }
                                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                }
                                if (isStartSysSvcCallRecord) {
                                }
                                traceBeginAndSlog("MakeVibratorServiceReady");
                                telephonyRegistry2.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeLockSettingsServiceReady");
                                if (lockSettings2 != null) {
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
                                config = wm.computeNewConfiguration(0);
                                metrics = new DisplayMetrics();
                                w = (WindowManager) context2.getSystemService("window");
                                w.getDefaultDisplay().getMetrics(metrics);
                                context2.getResources().updateConfiguration(config, metrics);
                                systemTheme = context2.getTheme();
                                if (systemTheme.getChangingConfigurations() != 0) {
                                }
                                traceBeginAndSlog("MakePowerManagerServiceReady");
                                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                traceEnd();
                                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                traceBeginAndSlog("MakePackageManagerServiceReady");
                                this.mPackageManagerService.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                traceEnd();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                traceBeginAndSlog("StartDeviceSpecificServices");
                                classes = this.mSystemContext.getResources().getStringArray(17236002);
                                length = classes.length;
                                lockSettings3 = lockSettings2;
                                lockSettings4 = 0;
                                while (lockSettings4 < length) {
                                }
                                context = context2;
                                wm2 = wm;
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                traceEnd();
                                networkManagementF = networkManagement;
                                networkStatsF = networkStats;
                                networkPolicyF = networkPolicy;
                                inputManager3 = inputManager2;
                                connectivityF = connectivity;
                                locationF = location;
                                countryDetectorF = countryDetector;
                                networkTimeUpdaterF = networkTimeUpdater;
                                commonTimeMgmtServiceF = commonTimeMgmtService;
                                inputManagerF = inputManager3;
                                telephonyRegistryF = telephonyRegistry;
                                mediaRouterF = mediaRouter;
                                mmsServiceF = mmsService;
                                ipSecServiceF = ipSecService;
                                wm = wm2;
                                activityManagerService = this.mActivityManagerService;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                            }
                        } catch (RuntimeException e14) {
                            e = e14;
                            telephonyRegistry = telephonyRegistry3;
                            hwCustEmergDataManager = emergDataManager;
                            storageManager = null;
                            connectivity = null;
                            inputManagerService = inputManager4;
                            alarmManagerService = almService2;
                            vibratorService = telephonyRegistry2;
                            tuiEnable = tuiEnable3;
                            tuiEnable2 = 1;
                            inputManager = inputManagerService;
                            almService = alarmManagerService;
                            Slog.e("System", "******************************************");
                            Slog.e("System", "************ Failure starting core service", e);
                            alarmManagerService = almService;
                            wm = wm3;
                            inputManager2 = inputManager;
                            statusBar = null;
                            lockSettings = null;
                            if (this.mFactoryTestMode != tuiEnable2) {
                            }
                            traceBeginAndSlog("MakeDisplayReady");
                            wm.displayReady();
                            traceEnd();
                            traceBeginAndSlog("StartStorageManagerService");
                            this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                            storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                            traceEnd();
                            traceBeginAndSlog("StartStorageStatsService");
                            this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                            traceEnd();
                            storageManager = storageManager2;
                            traceBeginAndSlog("StartUiModeManager");
                            this.mSystemServiceManager.startService(UiModeManagerService.class);
                            traceEnd();
                            HwBootCheck.bootSceneEnd(101);
                            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                            notification = null;
                            if (this.mRuntimeRestart) {
                            }
                            HwBootCheck.bootSceneStart(104, 900000);
                            if (this.mOnlyCore) {
                            }
                            traceBeginAndSlog("PerformFstrimIfNeeded");
                            this.mPackageManagerService.performFstrimIfNeeded();
                            traceEnd();
                            HwBootCheck.bootSceneEnd(104);
                            HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                            if (this.mFactoryTestMode == 1) {
                            }
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                    } catch (RuntimeException e15) {
                        e = e15;
                        alarmManagerService = almService;
                        vibratorService = telephonyRegistry2;
                        telephonyRegistry = telephonyRegistry3;
                        hwCustEmergDataManager = emergDataManager;
                        storageManager = null;
                        connectivity = null;
                        tuiEnable = tuiEnable3;
                        tuiEnable2 = 1;
                        Slog.e("System", "******************************************");
                        Slog.e("System", "************ Failure starting core service", e);
                        alarmManagerService = almService;
                        wm = wm3;
                        inputManager2 = inputManager;
                        statusBar = null;
                        lockSettings = null;
                        if (this.mFactoryTestMode != tuiEnable2) {
                        }
                        traceBeginAndSlog("MakeDisplayReady");
                        wm.displayReady();
                        traceEnd();
                        traceBeginAndSlog("StartStorageManagerService");
                        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                        storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                        traceEnd();
                        traceBeginAndSlog("StartStorageStatsService");
                        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                        traceEnd();
                        storageManager = storageManager2;
                        traceBeginAndSlog("StartUiModeManager");
                        this.mSystemServiceManager.startService(UiModeManagerService.class);
                        traceEnd();
                        HwBootCheck.bootSceneEnd(101);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                        notification = null;
                        if (this.mRuntimeRestart) {
                        }
                        HwBootCheck.bootSceneStart(104, 900000);
                        if (this.mOnlyCore) {
                        }
                        traceBeginAndSlog("PerformFstrimIfNeeded");
                        this.mPackageManagerService.performFstrimIfNeeded();
                        traceEnd();
                        HwBootCheck.bootSceneEnd(104);
                        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                        if (this.mFactoryTestMode == 1) {
                        }
                        if (isWatch) {
                        }
                        MediaProjectionManagerService.sHasStartedInSystemserver = true;
                        if (isWatch) {
                        }
                        if (disableSlices) {
                        }
                        if (disableCameraService) {
                        }
                        if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        }
                        traceBeginAndSlog("StartStatsCompanionService");
                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                        traceEnd();
                        safeMode = wm.detectSafeMode();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        if (safeMode) {
                        }
                        traceBeginAndSlog("StartMmsService");
                        mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        }
                        if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                        }
                        if (isStartSysSvcCallRecord) {
                        }
                        traceBeginAndSlog("MakeVibratorServiceReady");
                        telephonyRegistry2.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                        if (lockSettings2 != null) {
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
                        config = wm.computeNewConfiguration(0);
                        metrics = new DisplayMetrics();
                        w = (WindowManager) context2.getSystemService("window");
                        w.getDefaultDisplay().getMetrics(metrics);
                        context2.getResources().updateConfiguration(config, metrics);
                        systemTheme = context2.getTheme();
                        if (systemTheme.getChangingConfigurations() != 0) {
                        }
                        traceBeginAndSlog("MakePowerManagerServiceReady");
                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                        traceEnd();
                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                        traceBeginAndSlog("MakePackageManagerServiceReady");
                        this.mPackageManagerService.systemReady();
                        traceEnd();
                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                        traceEnd();
                        this.mSystemServiceManager.setSafeMode(safeMode);
                        traceBeginAndSlog("StartDeviceSpecificServices");
                        classes = this.mSystemContext.getResources().getStringArray(17236002);
                        length = classes.length;
                        lockSettings3 = lockSettings2;
                        lockSettings4 = 0;
                        while (lockSettings4 < length) {
                        }
                        context = context2;
                        wm2 = wm;
                        traceEnd();
                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                        traceEnd();
                        networkManagementF = networkManagement;
                        networkStatsF = networkStats;
                        networkPolicyF = networkPolicy;
                        inputManager3 = inputManager2;
                        connectivityF = connectivity;
                        locationF = location;
                        countryDetectorF = countryDetector;
                        networkTimeUpdaterF = networkTimeUpdater;
                        commonTimeMgmtServiceF = commonTimeMgmtService;
                        inputManagerF = inputManager3;
                        telephonyRegistryF = telephonyRegistry;
                        mediaRouterF = mediaRouter;
                        mmsServiceF = mmsService;
                        ipSecServiceF = ipSecService;
                        wm = wm2;
                        activityManagerService = this.mActivityManagerService;
                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                        activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                    }
                    statusBar = null;
                    lockSettings = null;
                    if (this.mFactoryTestMode != tuiEnable2) {
                        traceBeginAndSlog("StartInputMethodManagerLifecycle");
                        try {
                            Slog.i(TAG, "Input Method Service");
                            this.mSystemServiceManager.startService(InputMethodManagerService.Lifecycle.class);
                        } catch (Throwable e2222) {
                            reportWtf("starting Input Manager Service", e2222);
                        }
                        if (isSupportedSecIme) {
                            try {
                                Slog.i(TAG, "Secure Input Method Service");
                                this.mSystemServiceManager.startService("com.android.server.HwSecureInputMethodManagerService$MyLifecycle");
                            } catch (Throwable e22222) {
                                reportWtf("starting Secure Input Manager Service", e22222);
                            }
                        }
                        traceEnd();
                        traceBeginAndSlog("StartAccessibilityManagerService");
                        try {
                            ServiceManager.addService("accessibility", new AccessibilityManagerService(context2));
                        } catch (Throwable e222222) {
                            reportWtf("starting Accessibility Manager", e222222);
                        }
                        traceEnd();
                    }
                    traceBeginAndSlog("MakeDisplayReady");
                    wm.displayReady();
                    traceEnd();
                    if (!(this.mFactoryTestMode == tuiEnable2 || "0".equals(SystemProperties.get("system_init.startmountservice")))) {
                        traceBeginAndSlog("StartStorageManagerService");
                        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                        storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                        traceEnd();
                        traceBeginAndSlog("StartStorageStatsService");
                        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                        traceEnd();
                        storageManager = storageManager2;
                    }
                    traceBeginAndSlog("StartUiModeManager");
                    this.mSystemServiceManager.startService(UiModeManagerService.class);
                    traceEnd();
                    HwBootCheck.bootSceneEnd(101);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                    notification = null;
                    if (this.mRuntimeRestart || isFirstBootOrUpgrade()) {
                        HwBootCheck.bootSceneStart(104, 900000);
                    } else {
                        HwBootCheck.bootSceneStart(104, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                    }
                    if (this.mOnlyCore) {
                        traceBeginAndSlog("UpdatePackagesIfNeeded");
                        try {
                            this.mPackageManagerService.updatePackagesIfNeeded();
                        } catch (Throwable e2222222) {
                            reportWtf("update packages", e2222222);
                        }
                        traceEnd();
                    }
                    traceBeginAndSlog("PerformFstrimIfNeeded");
                    this.mPackageManagerService.performFstrimIfNeeded();
                    traceEnd();
                    HwBootCheck.bootSceneEnd(104);
                    HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                    if (this.mFactoryTestMode == 1) {
                        ConnectivityService connectivity2;
                        startForceRotation(context2);
                        traceBeginAndSlog("StartLockSettingsService");
                        try {
                            this.mSystemServiceManager.startService(LOCK_SETTINGS_SERVICE_CLASS);
                            lockSettings = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
                        } catch (Throwable e22222222) {
                            reportWtf("starting LockSettingsService service", e22222222);
                        }
                        traceEnd();
                        tuiEnable3 = SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) ^ 1;
                        if (tuiEnable3) {
                            traceBeginAndSlog("StartPersistentDataBlock");
                            this.mSystemServiceManager.startService(PersistentDataBlockService.class);
                            traceEnd();
                        }
                        if (tuiEnable3 || OemLockService.isHalPresent()) {
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
                                Slog.i(TAG, "Status Bar");
                                statusBar = HwServiceFactory.createHwStatusBarManagerService(context2, wm);
                                ServiceManager.addService("statusbar", statusBar);
                            } catch (Throwable e222222222) {
                                reportWtf("starting StatusBarManagerService", e222222222);
                            }
                            traceEnd();
                        }
                        traceBeginAndSlog("StartClipboardService");
                        this.mSystemServiceManager.startService(ClipboardService.class);
                        traceEnd();
                        traceBeginAndSlog("StartNetworkManagementService");
                        try {
                            networkManagement2 = NetworkManagementService.create(context2);
                            ServiceManager.addService("network_management", networkManagement2);
                        } catch (Throwable e2222222222) {
                            reportWtf("starting NetworkManagement Service", e2222222222);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartIpSecService");
                        try {
                            ipSecService2 = IpSecService.create(context2);
                            ServiceManager.addService(INetd.IPSEC_INTERFACE_PREFIX, ipSecService2);
                        } catch (Throwable e22222222222) {
                            reportWtf("starting IpSec Service", e22222222222);
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
                            networkStats2 = NetworkStatsService.create(context2, networkManagement2);
                            ServiceManager.addService("netstats", networkStats2);
                        } catch (Throwable e222222222222) {
                            reportWtf("starting NetworkStats Service", e222222222222);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartNetworkPolicyManagerService");
                        try {
                            networkPolicy2 = HwServiceFactory.getHwNetworkPolicyManagerService().getInstance(context2, this.mActivityManagerService, networkManagement2);
                            ServiceManager.addService("netpolicy", networkPolicy2);
                        } catch (Throwable e2222222222222) {
                            reportWtf("starting NetworkPolicy Service", e2222222222222);
                        }
                        traceEnd();
                        if (!this.mOnlyCore) {
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                                traceBeginAndSlog("StartWifi");
                                this.mSystemServiceManager.startService(WIFI_SERVICE_CLASS);
                                traceEnd();
                                traceBeginAndSlog("StartWifiScanning");
                                this.mSystemServiceManager.startService("com.android.server.wifi.scanner.WifiScanningService");
                                traceEnd();
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                                traceBeginAndSlog("StartRttService");
                                this.mSystemServiceManager.startService("com.android.server.wifi.rtt.RttService");
                                traceEnd();
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                                traceBeginAndSlog("StartWifiAware");
                                this.mSystemServiceManager.startService(WIFI_AWARE_SERVICE_CLASS);
                                traceEnd();
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                                traceBeginAndSlog("StartWifiP2P");
                                this.mSystemServiceManager.startService(WIFI_P2P_SERVICE_CLASS);
                                traceEnd();
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                                traceBeginAndSlog("StartLowpan");
                                this.mSystemServiceManager.startService(LOWPAN_SERVICE_CLASS);
                                traceEnd();
                            }
                        }
                        if (this.mPackageManager.hasSystemFeature("android.hardware.ethernet") || this.mPackageManager.hasSystemFeature("android.hardware.usb.host")) {
                            traceBeginAndSlog("StartEthernet");
                            this.mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
                            traceEnd();
                        }
                        traceBeginAndSlog("StartConnectivityService");
                        try {
                            connectivity2 = HwServiceFactory.getHwConnectivityManager().createHwConnectivityService(context2, networkManagement2, networkStats2, networkPolicy2);
                            try {
                                try {
                                    ServiceManager.addService("connectivity", connectivity2, null, 6);
                                    networkStats2.bindConnectivityManager(connectivity2);
                                    networkPolicy2.bindConnectivityManager(connectivity2);
                                } catch (Throwable th2) {
                                    e2222222222222 = th2;
                                }
                            } catch (Throwable th3) {
                                e2222222222222 = th3;
                                statusBarManagerService = statusBar;
                                reportWtf("starting Connectivity Service", e2222222222222);
                                traceEnd();
                                traceBeginAndSlog("StartNsdService");
                                serviceDiscovery = NsdService.create(context2);
                                try {
                                    ServiceManager.addService("servicediscovery", serviceDiscovery);
                                } catch (Throwable th4) {
                                    e2222222222222 = th4;
                                }
                                serviceDiscovery2 = serviceDiscovery;
                                traceEnd();
                                traceBeginAndSlog("StartSystemUpdateManagerService");
                                ServiceManager.addService("system_update", new SystemUpdateManagerService(context2));
                                traceEnd();
                                traceBeginAndSlog("StartUpdateLockService");
                                ServiceManager.addService("updatelock", new UpdateLockService(context2));
                                traceEnd();
                                traceBeginAndSlog("StartNotificationManager");
                                this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                                SystemNotificationChannels.createAll(context2);
                                notification2 = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                                traceEnd();
                                traceBeginAndSlog("StartDeviceMonitor");
                                this.mSystemServiceManager.startService(HwServiceFactory.getDeviceStorageMonitorServiceClassName());
                                traceEnd();
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                notification3 = notification2;
                                stringBuilder.append("TUI Connect enable ");
                                stringBuilder.append(tuiEnable);
                                Slog.i(str, stringBuilder.toString());
                                if (tuiEnable) {
                                }
                                if (vrDisplayEnable) {
                                }
                                traceBeginAndSlog("StartLocationManagerService");
                                Slog.i(TAG, "Location Manager");
                                hwLocation = HwServiceFactory.getHwLocationManagerService();
                                if (hwLocation == null) {
                                }
                                try {
                                    ServiceManager.addService("location", location2);
                                } catch (Throwable th5) {
                                    e2222222222222 = th5;
                                }
                                traceEnd();
                                traceBeginAndSlog("StartCountryDetectorService");
                                countryDetector2 = new CountryDetectorService(context2);
                                try {
                                    ServiceManager.addService("country_detector", countryDetector2);
                                    z2 = vrDisplayEnable;
                                } catch (Throwable th6) {
                                    e2222222222222 = th6;
                                    reportWtf("starting Country Detector", e2222222222222);
                                    traceEnd();
                                    if (!isWatch) {
                                    }
                                    if (context2.getResources().getBoolean(true)) {
                                    }
                                    traceBeginAndSlog("StartTrustManager");
                                    this.mSystemServiceManager.startService(TrustManagerService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartAudioService");
                                    this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartDockObserver");
                                    this.mSystemServiceManager.startService(DockObserver.class);
                                    traceEnd();
                                    if (isWatch) {
                                    }
                                    traceBeginAndSlog("StartWiredAccessoryManager");
                                    inputManager2.setWiredAccessoryCallbacks(new WiredAccessoryManager(context2, inputManager2));
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                                    }
                                    traceBeginAndSlog("StartUsbService");
                                    this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                                    traceEnd();
                                    if (isWatch) {
                                    }
                                    traceBeginAndSlog("StartHardwarePropertiesManagerService");
                                    vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                                    ServiceManager.addService("hardware_properties", vrDisplayEnable);
                                    hardwarePropertiesService = vrDisplayEnable;
                                    traceEnd();
                                    traceBeginAndSlog("StartTwilightService");
                                    this.mSystemServiceManager.startService(TwilightService.class);
                                    traceEnd();
                                    if (ColorDisplayController.isAvailable(context2)) {
                                    }
                                    this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                                    traceBeginAndSlog("StartSoundTrigger");
                                    this.mSystemServiceManager.startService(SoundTriggerService.class);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                    }
                                    traceBeginAndSlog("StartAppWidgerService");
                                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                    traceEnd();
                                    traceBeginAndSlog("StartVoiceRecognitionManager");
                                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                    traceEnd();
                                    if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                                    }
                                    traceBeginAndSlog("StartSensorNotification");
                                    this.mSystemServiceManager.startService(SensorNotificationService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartContextHubSystemService");
                                    this.mSystemServiceManager.startService(ContextHubSystemService.class);
                                    traceEnd();
                                    HwServiceFactory.setupHwServices(context2);
                                    traceBeginAndSlog("StartDiskStatsService");
                                    ServiceManager.addService("diskstats", new DiskStatsService(context2));
                                    traceEnd();
                                    if (this.mOnlyCore) {
                                    }
                                    vrDisplayEnable = z3;
                                    if (vrDisplayEnable) {
                                    }
                                    Slog.i(TAG, "attestation Service");
                                    attestation = HwServiceFactory.getHwAttestationService();
                                    if (attestation != null) {
                                    }
                                    if (!isWatch) {
                                    }
                                    traceBeginAndSlog("StartCommonTimeManagementService");
                                    vrDisplayEnable = new CommonTimeManagementService(context2);
                                    ServiceManager.addService("commontime_management", vrDisplayEnable);
                                    traceEnd();
                                    traceBeginAndSlog("CertBlacklister");
                                    certBlacklister = new CertBlacklister(context2);
                                    traceEnd();
                                    traceBeginAndSlog("StartEmergencyAffordanceService");
                                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartDreamManager");
                                    this.mSystemServiceManager.startService(DreamManagerService.class);
                                    traceEnd();
                                    traceBeginAndSlog("AddGraphicsStatsService");
                                    ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                                    traceBeginAndSlog("StartMediaUpdateService");
                                    this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                                    statusBar2 = new MediaRouterService(context2);
                                    ServiceManager.addService("media_router", statusBar2);
                                    commonTimeMgmtService = vrDisplayEnable;
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                                    }
                                    traceBeginAndSlog("StartBackgroundDexOptService");
                                    BackgroundDexOptService.schedule(context2);
                                    traceEnd();
                                    traceBeginAndSlog("StartPruneInstantAppsJobService");
                                    PruneInstantAppsJobService.schedule(context2);
                                    traceEnd();
                                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartLauncherAppsService");
                                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartCrossProfileAppsService");
                                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                    traceEnd();
                                    connectivity = connectivity2;
                                    countryDetector = countryDetector2;
                                    ipSecService = ipSecService2;
                                    networkPolicy = networkPolicy2;
                                    serialService = serial2;
                                    networkTimeUpdater = networkTimeUpdater2;
                                    hardwarePropertiesManagerService = hardwarePropertiesService;
                                    lockSettings2 = lockSettings;
                                    notification = notification3;
                                    location = location3;
                                    networkManagement = networkManagement2;
                                    networkStats = networkStats2;
                                    lockSettings5 = serviceDiscovery2;
                                    if (isWatch) {
                                    }
                                    MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                    if (isWatch) {
                                    }
                                    if (disableSlices) {
                                    }
                                    if (disableCameraService) {
                                    }
                                    if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                    }
                                    traceBeginAndSlog("StartStatsCompanionService");
                                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                    traceEnd();
                                    safeMode = wm.detectSafeMode();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    if (safeMode) {
                                    }
                                    traceBeginAndSlog("StartMmsService");
                                    mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                    }
                                    if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                    }
                                    if (isStartSysSvcCallRecord) {
                                    }
                                    traceBeginAndSlog("MakeVibratorServiceReady");
                                    telephonyRegistry2.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                                    if (lockSettings2 != null) {
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
                                    config = wm.computeNewConfiguration(0);
                                    metrics = new DisplayMetrics();
                                    w = (WindowManager) context2.getSystemService("window");
                                    w.getDefaultDisplay().getMetrics(metrics);
                                    context2.getResources().updateConfiguration(config, metrics);
                                    systemTheme = context2.getTheme();
                                    if (systemTheme.getChangingConfigurations() != 0) {
                                    }
                                    traceBeginAndSlog("MakePowerManagerServiceReady");
                                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                    traceEnd();
                                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                    traceBeginAndSlog("MakePackageManagerServiceReady");
                                    this.mPackageManagerService.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                    traceEnd();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    traceBeginAndSlog("StartDeviceSpecificServices");
                                    classes = this.mSystemContext.getResources().getStringArray(17236002);
                                    length = classes.length;
                                    lockSettings3 = lockSettings2;
                                    lockSettings4 = 0;
                                    while (lockSettings4 < length) {
                                    }
                                    context = context2;
                                    wm2 = wm;
                                    traceEnd();
                                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                    traceEnd();
                                    networkManagementF = networkManagement;
                                    networkStatsF = networkStats;
                                    networkPolicyF = networkPolicy;
                                    inputManager3 = inputManager2;
                                    connectivityF = connectivity;
                                    locationF = location;
                                    countryDetectorF = countryDetector;
                                    networkTimeUpdaterF = networkTimeUpdater;
                                    commonTimeMgmtServiceF = commonTimeMgmtService;
                                    inputManagerF = inputManager3;
                                    telephonyRegistryF = telephonyRegistry;
                                    mediaRouterF = mediaRouter;
                                    mmsServiceF = mmsService;
                                    ipSecServiceF = ipSecService;
                                    wm = wm2;
                                    activityManagerService = this.mActivityManagerService;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                    activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                }
                                traceEnd();
                                if (isWatch) {
                                }
                                if (context2.getResources().getBoolean(true)) {
                                }
                                traceBeginAndSlog("StartTrustManager");
                                this.mSystemServiceManager.startService(TrustManagerService.class);
                                traceEnd();
                                traceBeginAndSlog("StartAudioService");
                                this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                                traceEnd();
                                traceBeginAndSlog("StartDockObserver");
                                this.mSystemServiceManager.startService(DockObserver.class);
                                traceEnd();
                                if (isWatch) {
                                }
                                traceBeginAndSlog("StartWiredAccessoryManager");
                                inputManager2.setWiredAccessoryCallbacks(new WiredAccessoryManager(context2, inputManager2));
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                                }
                                traceBeginAndSlog("StartUsbService");
                                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                                traceEnd();
                                if (isWatch) {
                                }
                                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                                vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                                ServiceManager.addService("hardware_properties", vrDisplayEnable);
                                hardwarePropertiesService = vrDisplayEnable;
                                traceEnd();
                                traceBeginAndSlog("StartTwilightService");
                                this.mSystemServiceManager.startService(TwilightService.class);
                                traceEnd();
                                if (ColorDisplayController.isAvailable(context2)) {
                                }
                                this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                                traceBeginAndSlog("StartSoundTrigger");
                                this.mSystemServiceManager.startService(SoundTriggerService.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                }
                                traceBeginAndSlog("StartAppWidgerService");
                                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                traceEnd();
                                traceBeginAndSlog("StartVoiceRecognitionManager");
                                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                traceEnd();
                                if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                                }
                                traceBeginAndSlog("StartSensorNotification");
                                this.mSystemServiceManager.startService(SensorNotificationService.class);
                                traceEnd();
                                traceBeginAndSlog("StartContextHubSystemService");
                                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                                traceEnd();
                                HwServiceFactory.setupHwServices(context2);
                                traceBeginAndSlog("StartDiskStatsService");
                                ServiceManager.addService("diskstats", new DiskStatsService(context2));
                                traceEnd();
                                if (this.mOnlyCore) {
                                }
                                vrDisplayEnable = z3;
                                if (vrDisplayEnable) {
                                }
                                Slog.i(TAG, "attestation Service");
                                attestation = HwServiceFactory.getHwAttestationService();
                                if (attestation != null) {
                                }
                                if (isWatch) {
                                }
                                traceBeginAndSlog("StartCommonTimeManagementService");
                                vrDisplayEnable = new CommonTimeManagementService(context2);
                                try {
                                    ServiceManager.addService("commontime_management", vrDisplayEnable);
                                } catch (Throwable th7) {
                                    e2222222222222 = th7;
                                }
                                traceEnd();
                                traceBeginAndSlog("CertBlacklister");
                                certBlacklister = new CertBlacklister(context2);
                                traceEnd();
                                traceBeginAndSlog("StartEmergencyAffordanceService");
                                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                traceEnd();
                                traceBeginAndSlog("StartDreamManager");
                                this.mSystemServiceManager.startService(DreamManagerService.class);
                                traceEnd();
                                traceBeginAndSlog("AddGraphicsStatsService");
                                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                                traceBeginAndSlog("StartMediaUpdateService");
                                this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                                statusBar2 = new MediaRouterService(context2);
                                try {
                                    ServiceManager.addService("media_router", statusBar2);
                                    commonTimeMgmtService = vrDisplayEnable;
                                } catch (Throwable th8) {
                                    e2222222222222 = th8;
                                    commonTimeMgmtService = vrDisplayEnable;
                                    reportWtf("starting MediaRouterService", e2222222222222);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                                    }
                                    traceBeginAndSlog("StartBackgroundDexOptService");
                                    BackgroundDexOptService.schedule(context2);
                                    traceEnd();
                                    traceBeginAndSlog("StartPruneInstantAppsJobService");
                                    PruneInstantAppsJobService.schedule(context2);
                                    traceEnd();
                                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartLauncherAppsService");
                                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartCrossProfileAppsService");
                                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                    traceEnd();
                                    connectivity = connectivity2;
                                    countryDetector = countryDetector2;
                                    ipSecService = ipSecService2;
                                    networkPolicy = networkPolicy2;
                                    serialService = serial2;
                                    networkTimeUpdater = networkTimeUpdater2;
                                    hardwarePropertiesManagerService = hardwarePropertiesService;
                                    lockSettings2 = lockSettings;
                                    notification = notification3;
                                    location = location3;
                                    networkManagement = networkManagement2;
                                    networkStats = networkStats2;
                                    lockSettings5 = serviceDiscovery2;
                                    if (isWatch) {
                                    }
                                    MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                    if (isWatch) {
                                    }
                                    if (disableSlices) {
                                    }
                                    if (disableCameraService) {
                                    }
                                    if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                    }
                                    traceBeginAndSlog("StartStatsCompanionService");
                                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                    traceEnd();
                                    safeMode = wm.detectSafeMode();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    if (safeMode) {
                                    }
                                    traceBeginAndSlog("StartMmsService");
                                    mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                    }
                                    if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                    }
                                    if (isStartSysSvcCallRecord) {
                                    }
                                    traceBeginAndSlog("MakeVibratorServiceReady");
                                    telephonyRegistry2.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                                    if (lockSettings2 != null) {
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
                                    config = wm.computeNewConfiguration(0);
                                    metrics = new DisplayMetrics();
                                    w = (WindowManager) context2.getSystemService("window");
                                    w.getDefaultDisplay().getMetrics(metrics);
                                    context2.getResources().updateConfiguration(config, metrics);
                                    systemTheme = context2.getTheme();
                                    if (systemTheme.getChangingConfigurations() != 0) {
                                    }
                                    traceBeginAndSlog("MakePowerManagerServiceReady");
                                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                    traceEnd();
                                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                    traceBeginAndSlog("MakePackageManagerServiceReady");
                                    this.mPackageManagerService.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                    traceEnd();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    traceBeginAndSlog("StartDeviceSpecificServices");
                                    classes = this.mSystemContext.getResources().getStringArray(17236002);
                                    length = classes.length;
                                    lockSettings3 = lockSettings2;
                                    lockSettings4 = 0;
                                    while (lockSettings4 < length) {
                                    }
                                    context = context2;
                                    wm2 = wm;
                                    traceEnd();
                                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                    traceEnd();
                                    networkManagementF = networkManagement;
                                    networkStatsF = networkStats;
                                    networkPolicyF = networkPolicy;
                                    inputManager3 = inputManager2;
                                    connectivityF = connectivity;
                                    locationF = location;
                                    countryDetectorF = countryDetector;
                                    networkTimeUpdaterF = networkTimeUpdater;
                                    commonTimeMgmtServiceF = commonTimeMgmtService;
                                    inputManagerF = inputManager3;
                                    telephonyRegistryF = telephonyRegistry;
                                    mediaRouterF = mediaRouter;
                                    mmsServiceF = mmsService;
                                    ipSecServiceF = ipSecService;
                                    wm = wm2;
                                    activityManagerService = this.mActivityManagerService;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                    activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                }
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                                }
                                traceBeginAndSlog("StartBackgroundDexOptService");
                                BackgroundDexOptService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartPruneInstantAppsJobService");
                                PruneInstantAppsJobService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartShortcutServiceLifecycle");
                                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                traceEnd();
                                traceBeginAndSlog("StartLauncherAppsService");
                                this.mSystemServiceManager.startService(LauncherAppsService.class);
                                traceEnd();
                                traceBeginAndSlog("StartCrossProfileAppsService");
                                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                traceEnd();
                                connectivity = connectivity2;
                                countryDetector = countryDetector2;
                                ipSecService = ipSecService2;
                                networkPolicy = networkPolicy2;
                                serialService = serial2;
                                networkTimeUpdater = networkTimeUpdater2;
                                hardwarePropertiesManagerService = hardwarePropertiesService;
                                lockSettings2 = lockSettings;
                                notification = notification3;
                                location = location3;
                                networkManagement = networkManagement2;
                                networkStats = networkStats2;
                                lockSettings5 = serviceDiscovery2;
                                if (isWatch) {
                                }
                                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                if (isWatch) {
                                }
                                if (disableSlices) {
                                }
                                if (disableCameraService) {
                                }
                                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                }
                                traceBeginAndSlog("StartStatsCompanionService");
                                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                traceEnd();
                                safeMode = wm.detectSafeMode();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                if (safeMode) {
                                }
                                traceBeginAndSlog("StartMmsService");
                                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                }
                                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                }
                                if (isStartSysSvcCallRecord) {
                                }
                                traceBeginAndSlog("MakeVibratorServiceReady");
                                telephonyRegistry2.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeLockSettingsServiceReady");
                                if (lockSettings2 != null) {
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
                                config = wm.computeNewConfiguration(0);
                                metrics = new DisplayMetrics();
                                w = (WindowManager) context2.getSystemService("window");
                                w.getDefaultDisplay().getMetrics(metrics);
                                context2.getResources().updateConfiguration(config, metrics);
                                systemTheme = context2.getTheme();
                                if (systemTheme.getChangingConfigurations() != 0) {
                                }
                                traceBeginAndSlog("MakePowerManagerServiceReady");
                                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                traceEnd();
                                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                traceBeginAndSlog("MakePackageManagerServiceReady");
                                this.mPackageManagerService.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                traceEnd();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                traceBeginAndSlog("StartDeviceSpecificServices");
                                classes = this.mSystemContext.getResources().getStringArray(17236002);
                                length = classes.length;
                                lockSettings3 = lockSettings2;
                                lockSettings4 = 0;
                                while (lockSettings4 < length) {
                                }
                                context = context2;
                                wm2 = wm;
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                traceEnd();
                                networkManagementF = networkManagement;
                                networkStatsF = networkStats;
                                networkPolicyF = networkPolicy;
                                inputManager3 = inputManager2;
                                connectivityF = connectivity;
                                locationF = location;
                                countryDetectorF = countryDetector;
                                networkTimeUpdaterF = networkTimeUpdater;
                                commonTimeMgmtServiceF = commonTimeMgmtService;
                                inputManagerF = inputManager3;
                                telephonyRegistryF = telephonyRegistry;
                                mediaRouterF = mediaRouter;
                                mmsServiceF = mmsService;
                                ipSecServiceF = ipSecService;
                                wm = wm2;
                                activityManagerService = this.mActivityManagerService;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                            }
                        } catch (Throwable th9) {
                            e2222222222222 = th9;
                            statusBarManagerService = statusBar;
                            connectivity2 = connectivity;
                            reportWtf("starting Connectivity Service", e2222222222222);
                            traceEnd();
                            traceBeginAndSlog("StartNsdService");
                            serviceDiscovery = NsdService.create(context2);
                            ServiceManager.addService("servicediscovery", serviceDiscovery);
                            serviceDiscovery2 = serviceDiscovery;
                            traceEnd();
                            traceBeginAndSlog("StartSystemUpdateManagerService");
                            ServiceManager.addService("system_update", new SystemUpdateManagerService(context2));
                            traceEnd();
                            traceBeginAndSlog("StartUpdateLockService");
                            ServiceManager.addService("updatelock", new UpdateLockService(context2));
                            traceEnd();
                            traceBeginAndSlog("StartNotificationManager");
                            this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                            SystemNotificationChannels.createAll(context2);
                            notification2 = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                            traceEnd();
                            traceBeginAndSlog("StartDeviceMonitor");
                            this.mSystemServiceManager.startService(HwServiceFactory.getDeviceStorageMonitorServiceClassName());
                            traceEnd();
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            notification3 = notification2;
                            stringBuilder.append("TUI Connect enable ");
                            stringBuilder.append(tuiEnable);
                            Slog.i(str, stringBuilder.toString());
                            if (tuiEnable) {
                            }
                            if (vrDisplayEnable) {
                            }
                            traceBeginAndSlog("StartLocationManagerService");
                            Slog.i(TAG, "Location Manager");
                            hwLocation = HwServiceFactory.getHwLocationManagerService();
                            if (hwLocation == null) {
                            }
                            ServiceManager.addService("location", location2);
                            traceEnd();
                            traceBeginAndSlog("StartCountryDetectorService");
                            countryDetector2 = new CountryDetectorService(context2);
                            ServiceManager.addService("country_detector", countryDetector2);
                            z2 = vrDisplayEnable;
                            traceEnd();
                            if (isWatch) {
                            }
                            if (context2.getResources().getBoolean(true)) {
                            }
                            traceBeginAndSlog("StartTrustManager");
                            this.mSystemServiceManager.startService(TrustManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("StartAudioService");
                            this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartDockObserver");
                            this.mSystemServiceManager.startService(DockObserver.class);
                            traceEnd();
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartWiredAccessoryManager");
                            inputManager2.setWiredAccessoryCallbacks(new WiredAccessoryManager(context2, inputManager2));
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                            }
                            traceBeginAndSlog("StartUsbService");
                            this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                            traceEnd();
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartHardwarePropertiesManagerService");
                            vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                            ServiceManager.addService("hardware_properties", vrDisplayEnable);
                            hardwarePropertiesService = vrDisplayEnable;
                            traceEnd();
                            traceBeginAndSlog("StartTwilightService");
                            this.mSystemServiceManager.startService(TwilightService.class);
                            traceEnd();
                            if (ColorDisplayController.isAvailable(context2)) {
                            }
                            this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                            traceBeginAndSlog("StartSoundTrigger");
                            this.mSystemServiceManager.startService(SoundTriggerService.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                            }
                            traceBeginAndSlog("StartAppWidgerService");
                            this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                            traceEnd();
                            traceBeginAndSlog("StartVoiceRecognitionManager");
                            this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                            traceEnd();
                            if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                            }
                            traceBeginAndSlog("StartSensorNotification");
                            this.mSystemServiceManager.startService(SensorNotificationService.class);
                            traceEnd();
                            traceBeginAndSlog("StartContextHubSystemService");
                            this.mSystemServiceManager.startService(ContextHubSystemService.class);
                            traceEnd();
                            HwServiceFactory.setupHwServices(context2);
                            traceBeginAndSlog("StartDiskStatsService");
                            ServiceManager.addService("diskstats", new DiskStatsService(context2));
                            traceEnd();
                            if (this.mOnlyCore) {
                            }
                            vrDisplayEnable = z3;
                            if (vrDisplayEnable) {
                            }
                            Slog.i(TAG, "attestation Service");
                            attestation = HwServiceFactory.getHwAttestationService();
                            if (attestation != null) {
                            }
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartCommonTimeManagementService");
                            vrDisplayEnable = new CommonTimeManagementService(context2);
                            ServiceManager.addService("commontime_management", vrDisplayEnable);
                            traceEnd();
                            traceBeginAndSlog("CertBlacklister");
                            certBlacklister = new CertBlacklister(context2);
                            traceEnd();
                            traceBeginAndSlog("StartEmergencyAffordanceService");
                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDreamManager");
                            this.mSystemServiceManager.startService(DreamManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("AddGraphicsStatsService");
                            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                            traceBeginAndSlog("StartMediaUpdateService");
                            this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                            statusBar2 = new MediaRouterService(context2);
                            ServiceManager.addService("media_router", statusBar2);
                            commonTimeMgmtService = vrDisplayEnable;
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            }
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartPruneInstantAppsJobService");
                            PruneInstantAppsJobService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            connectivity = connectivity2;
                            countryDetector = countryDetector2;
                            ipSecService = ipSecService2;
                            networkPolicy = networkPolicy2;
                            serialService = serial2;
                            networkTimeUpdater = networkTimeUpdater2;
                            hardwarePropertiesManagerService = hardwarePropertiesService;
                            lockSettings2 = lockSettings;
                            notification = notification3;
                            location = location3;
                            networkManagement = networkManagement2;
                            networkStats = networkStats2;
                            lockSettings5 = serviceDiscovery2;
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartNsdService");
                        try {
                            serviceDiscovery = NsdService.create(context2);
                            ServiceManager.addService("servicediscovery", serviceDiscovery);
                        } catch (Throwable th10) {
                            e2222222222222 = th10;
                            serviceDiscovery = null;
                            reportWtf("starting Service Discovery Service", e2222222222222);
                            serviceDiscovery2 = serviceDiscovery;
                            traceEnd();
                            traceBeginAndSlog("StartSystemUpdateManagerService");
                            ServiceManager.addService("system_update", new SystemUpdateManagerService(context2));
                            traceEnd();
                            traceBeginAndSlog("StartUpdateLockService");
                            ServiceManager.addService("updatelock", new UpdateLockService(context2));
                            traceEnd();
                            traceBeginAndSlog("StartNotificationManager");
                            this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                            SystemNotificationChannels.createAll(context2);
                            notification2 = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                            traceEnd();
                            traceBeginAndSlog("StartDeviceMonitor");
                            this.mSystemServiceManager.startService(HwServiceFactory.getDeviceStorageMonitorServiceClassName());
                            traceEnd();
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            notification3 = notification2;
                            stringBuilder.append("TUI Connect enable ");
                            stringBuilder.append(tuiEnable);
                            Slog.i(str, stringBuilder.toString());
                            if (tuiEnable) {
                            }
                            if (vrDisplayEnable) {
                            }
                            traceBeginAndSlog("StartLocationManagerService");
                            Slog.i(TAG, "Location Manager");
                            hwLocation = HwServiceFactory.getHwLocationManagerService();
                            if (hwLocation == null) {
                            }
                            ServiceManager.addService("location", location2);
                            traceEnd();
                            traceBeginAndSlog("StartCountryDetectorService");
                            countryDetector2 = new CountryDetectorService(context2);
                            ServiceManager.addService("country_detector", countryDetector2);
                            z2 = vrDisplayEnable;
                            traceEnd();
                            if (isWatch) {
                            }
                            if (context2.getResources().getBoolean(true)) {
                            }
                            traceBeginAndSlog("StartTrustManager");
                            this.mSystemServiceManager.startService(TrustManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("StartAudioService");
                            this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartDockObserver");
                            this.mSystemServiceManager.startService(DockObserver.class);
                            traceEnd();
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartWiredAccessoryManager");
                            inputManager2.setWiredAccessoryCallbacks(new WiredAccessoryManager(context2, inputManager2));
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                            }
                            traceBeginAndSlog("StartUsbService");
                            this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                            traceEnd();
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartHardwarePropertiesManagerService");
                            vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                            ServiceManager.addService("hardware_properties", vrDisplayEnable);
                            hardwarePropertiesService = vrDisplayEnable;
                            traceEnd();
                            traceBeginAndSlog("StartTwilightService");
                            this.mSystemServiceManager.startService(TwilightService.class);
                            traceEnd();
                            if (ColorDisplayController.isAvailable(context2)) {
                            }
                            this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                            traceBeginAndSlog("StartSoundTrigger");
                            this.mSystemServiceManager.startService(SoundTriggerService.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                            }
                            traceBeginAndSlog("StartAppWidgerService");
                            this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                            traceEnd();
                            traceBeginAndSlog("StartVoiceRecognitionManager");
                            this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                            traceEnd();
                            if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                            }
                            traceBeginAndSlog("StartSensorNotification");
                            this.mSystemServiceManager.startService(SensorNotificationService.class);
                            traceEnd();
                            traceBeginAndSlog("StartContextHubSystemService");
                            this.mSystemServiceManager.startService(ContextHubSystemService.class);
                            traceEnd();
                            HwServiceFactory.setupHwServices(context2);
                            traceBeginAndSlog("StartDiskStatsService");
                            ServiceManager.addService("diskstats", new DiskStatsService(context2));
                            traceEnd();
                            if (this.mOnlyCore) {
                            }
                            vrDisplayEnable = z3;
                            if (vrDisplayEnable) {
                            }
                            Slog.i(TAG, "attestation Service");
                            attestation = HwServiceFactory.getHwAttestationService();
                            if (attestation != null) {
                            }
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartCommonTimeManagementService");
                            vrDisplayEnable = new CommonTimeManagementService(context2);
                            ServiceManager.addService("commontime_management", vrDisplayEnable);
                            traceEnd();
                            traceBeginAndSlog("CertBlacklister");
                            certBlacklister = new CertBlacklister(context2);
                            traceEnd();
                            traceBeginAndSlog("StartEmergencyAffordanceService");
                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDreamManager");
                            this.mSystemServiceManager.startService(DreamManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("AddGraphicsStatsService");
                            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                            traceBeginAndSlog("StartMediaUpdateService");
                            this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                            statusBar2 = new MediaRouterService(context2);
                            ServiceManager.addService("media_router", statusBar2);
                            commonTimeMgmtService = vrDisplayEnable;
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            }
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartPruneInstantAppsJobService");
                            PruneInstantAppsJobService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            connectivity = connectivity2;
                            countryDetector = countryDetector2;
                            ipSecService = ipSecService2;
                            networkPolicy = networkPolicy2;
                            serialService = serial2;
                            networkTimeUpdater = networkTimeUpdater2;
                            hardwarePropertiesManagerService = hardwarePropertiesService;
                            lockSettings2 = lockSettings;
                            notification = notification3;
                            location = location3;
                            networkManagement = networkManagement2;
                            networkStats = networkStats2;
                            lockSettings5 = serviceDiscovery2;
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        serviceDiscovery2 = serviceDiscovery;
                        traceEnd();
                        traceBeginAndSlog("StartSystemUpdateManagerService");
                        try {
                            ServiceManager.addService("system_update", new SystemUpdateManagerService(context2));
                        } catch (Throwable e22222222222222) {
                            reportWtf("starting SystemUpdateManagerService", e22222222222222);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartUpdateLockService");
                        try {
                            ServiceManager.addService("updatelock", new UpdateLockService(context2));
                        } catch (Throwable e222222222222222) {
                            reportWtf("starting UpdateLockService", e222222222222222);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartNotificationManager");
                        try {
                            this.mSystemServiceManager.startService("com.android.server.notification.HwNotificationManagerService");
                        } catch (RuntimeException e16) {
                            this.mSystemServiceManager.startService(NotificationManagerService.class);
                        }
                        SystemNotificationChannels.createAll(context2);
                        notification2 = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                        traceEnd();
                        traceBeginAndSlog("StartDeviceMonitor");
                        this.mSystemServiceManager.startService(HwServiceFactory.getDeviceStorageMonitorServiceClassName());
                        traceEnd();
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        notification3 = notification2;
                        stringBuilder.append("TUI Connect enable ");
                        stringBuilder.append(tuiEnable);
                        Slog.i(str, stringBuilder.toString());
                        if (tuiEnable) {
                            try {
                                ServiceManager.addService("tui", new TrustedUIService(context2));
                            } catch (Throwable e2222222222222222) {
                                Slog.e(TAG, "Failure starting TUI Service ", e2222222222222222);
                            }
                        }
                        if (vrDisplayEnable) {
                            str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("VR Display enable ");
                            stringBuilder2.append(vrDisplayEnable);
                            Slog.i(str, stringBuilder2.toString());
                            try {
                                ServiceManager.addService("vr_display", new VRManagerService(context2));
                            } catch (Throwable e22222222222222222) {
                                Slog.e(TAG, "Failure starting VR Service ", e22222222222222222);
                            }
                        }
                        traceBeginAndSlog("StartLocationManagerService");
                        try {
                            Slog.i(TAG, "Location Manager");
                            hwLocation = HwServiceFactory.getHwLocationManagerService();
                            if (hwLocation == null) {
                                location2 = hwLocation.getInstance(context2);
                            } else {
                                location2 = new LocationManagerService(context2);
                            }
                            ServiceManager.addService("location", location2);
                        } catch (Throwable th11) {
                            e22222222222222222 = th11;
                            location2 = null;
                            reportWtf("starting Location Manager", e22222222222222222);
                            traceEnd();
                            traceBeginAndSlog("StartCountryDetectorService");
                            countryDetector2 = new CountryDetectorService(context2);
                            ServiceManager.addService("country_detector", countryDetector2);
                            z2 = vrDisplayEnable;
                            traceEnd();
                            if (isWatch) {
                            }
                            if (context2.getResources().getBoolean(true)) {
                            }
                            traceBeginAndSlog("StartTrustManager");
                            this.mSystemServiceManager.startService(TrustManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("StartAudioService");
                            this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartDockObserver");
                            this.mSystemServiceManager.startService(DockObserver.class);
                            traceEnd();
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartWiredAccessoryManager");
                            inputManager2.setWiredAccessoryCallbacks(new WiredAccessoryManager(context2, inputManager2));
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                            }
                            traceBeginAndSlog("StartUsbService");
                            this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                            traceEnd();
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartHardwarePropertiesManagerService");
                            vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                            ServiceManager.addService("hardware_properties", vrDisplayEnable);
                            hardwarePropertiesService = vrDisplayEnable;
                            traceEnd();
                            traceBeginAndSlog("StartTwilightService");
                            this.mSystemServiceManager.startService(TwilightService.class);
                            traceEnd();
                            if (ColorDisplayController.isAvailable(context2)) {
                            }
                            this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                            traceBeginAndSlog("StartSoundTrigger");
                            this.mSystemServiceManager.startService(SoundTriggerService.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                            }
                            traceBeginAndSlog("StartAppWidgerService");
                            this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                            traceEnd();
                            traceBeginAndSlog("StartVoiceRecognitionManager");
                            this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                            traceEnd();
                            if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                            }
                            traceBeginAndSlog("StartSensorNotification");
                            this.mSystemServiceManager.startService(SensorNotificationService.class);
                            traceEnd();
                            traceBeginAndSlog("StartContextHubSystemService");
                            this.mSystemServiceManager.startService(ContextHubSystemService.class);
                            traceEnd();
                            HwServiceFactory.setupHwServices(context2);
                            traceBeginAndSlog("StartDiskStatsService");
                            ServiceManager.addService("diskstats", new DiskStatsService(context2));
                            traceEnd();
                            if (this.mOnlyCore) {
                            }
                            vrDisplayEnable = z3;
                            if (vrDisplayEnable) {
                            }
                            Slog.i(TAG, "attestation Service");
                            attestation = HwServiceFactory.getHwAttestationService();
                            if (attestation != null) {
                            }
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartCommonTimeManagementService");
                            vrDisplayEnable = new CommonTimeManagementService(context2);
                            ServiceManager.addService("commontime_management", vrDisplayEnable);
                            traceEnd();
                            traceBeginAndSlog("CertBlacklister");
                            certBlacklister = new CertBlacklister(context2);
                            traceEnd();
                            traceBeginAndSlog("StartEmergencyAffordanceService");
                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDreamManager");
                            this.mSystemServiceManager.startService(DreamManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("AddGraphicsStatsService");
                            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                            traceBeginAndSlog("StartMediaUpdateService");
                            this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                            statusBar2 = new MediaRouterService(context2);
                            ServiceManager.addService("media_router", statusBar2);
                            commonTimeMgmtService = vrDisplayEnable;
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            }
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartPruneInstantAppsJobService");
                            PruneInstantAppsJobService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            connectivity = connectivity2;
                            countryDetector = countryDetector2;
                            ipSecService = ipSecService2;
                            networkPolicy = networkPolicy2;
                            serialService = serial2;
                            networkTimeUpdater = networkTimeUpdater2;
                            hardwarePropertiesManagerService = hardwarePropertiesService;
                            lockSettings2 = lockSettings;
                            notification = notification3;
                            location = location3;
                            networkManagement = networkManagement2;
                            networkStats = networkStats2;
                            lockSettings5 = serviceDiscovery2;
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartCountryDetectorService");
                        try {
                            countryDetector2 = new CountryDetectorService(context2);
                            ServiceManager.addService("country_detector", countryDetector2);
                            z2 = vrDisplayEnable;
                        } catch (Throwable th12) {
                            e22222222222222222 = th12;
                            countryDetector2 = null;
                            reportWtf("starting Country Detector", e22222222222222222);
                            traceEnd();
                            if (isWatch) {
                            }
                            if (context2.getResources().getBoolean(true)) {
                            }
                            traceBeginAndSlog("StartTrustManager");
                            this.mSystemServiceManager.startService(TrustManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("StartAudioService");
                            this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartDockObserver");
                            this.mSystemServiceManager.startService(DockObserver.class);
                            traceEnd();
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartWiredAccessoryManager");
                            inputManager2.setWiredAccessoryCallbacks(new WiredAccessoryManager(context2, inputManager2));
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                            }
                            traceBeginAndSlog("StartUsbService");
                            this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                            traceEnd();
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartHardwarePropertiesManagerService");
                            vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                            ServiceManager.addService("hardware_properties", vrDisplayEnable);
                            hardwarePropertiesService = vrDisplayEnable;
                            traceEnd();
                            traceBeginAndSlog("StartTwilightService");
                            this.mSystemServiceManager.startService(TwilightService.class);
                            traceEnd();
                            if (ColorDisplayController.isAvailable(context2)) {
                            }
                            this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                            traceBeginAndSlog("StartSoundTrigger");
                            this.mSystemServiceManager.startService(SoundTriggerService.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                            }
                            traceBeginAndSlog("StartAppWidgerService");
                            this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                            traceEnd();
                            traceBeginAndSlog("StartVoiceRecognitionManager");
                            this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                            traceEnd();
                            if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                            }
                            traceBeginAndSlog("StartSensorNotification");
                            this.mSystemServiceManager.startService(SensorNotificationService.class);
                            traceEnd();
                            traceBeginAndSlog("StartContextHubSystemService");
                            this.mSystemServiceManager.startService(ContextHubSystemService.class);
                            traceEnd();
                            HwServiceFactory.setupHwServices(context2);
                            traceBeginAndSlog("StartDiskStatsService");
                            ServiceManager.addService("diskstats", new DiskStatsService(context2));
                            traceEnd();
                            if (this.mOnlyCore) {
                            }
                            vrDisplayEnable = z3;
                            if (vrDisplayEnable) {
                            }
                            Slog.i(TAG, "attestation Service");
                            attestation = HwServiceFactory.getHwAttestationService();
                            if (attestation != null) {
                            }
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartCommonTimeManagementService");
                            vrDisplayEnable = new CommonTimeManagementService(context2);
                            ServiceManager.addService("commontime_management", vrDisplayEnable);
                            traceEnd();
                            traceBeginAndSlog("CertBlacklister");
                            certBlacklister = new CertBlacklister(context2);
                            traceEnd();
                            traceBeginAndSlog("StartEmergencyAffordanceService");
                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDreamManager");
                            this.mSystemServiceManager.startService(DreamManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("AddGraphicsStatsService");
                            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                            traceBeginAndSlog("StartMediaUpdateService");
                            this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                            statusBar2 = new MediaRouterService(context2);
                            ServiceManager.addService("media_router", statusBar2);
                            commonTimeMgmtService = vrDisplayEnable;
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            }
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartPruneInstantAppsJobService");
                            PruneInstantAppsJobService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            connectivity = connectivity2;
                            countryDetector = countryDetector2;
                            ipSecService = ipSecService2;
                            networkPolicy = networkPolicy2;
                            serialService = serial2;
                            networkTimeUpdater = networkTimeUpdater2;
                            hardwarePropertiesManagerService = hardwarePropertiesService;
                            lockSettings2 = lockSettings;
                            notification = notification3;
                            location = location3;
                            networkManagement = networkManagement2;
                            networkStats = networkStats2;
                            lockSettings5 = serviceDiscovery2;
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        traceEnd();
                        if (isWatch) {
                            traceBeginAndSlog("StartSearchManagerService");
                            try {
                                this.mSystemServiceManager.startService(SEARCH_MANAGER_SERVICE_CLASS);
                            } catch (Throwable e222222222222222222) {
                                reportWtf("starting Search Service", e222222222222222222);
                            }
                            traceEnd();
                        }
                        if (context2.getResources().getBoolean(true)) {
                            traceBeginAndSlog("StartWallpaperManagerService");
                            this.mSystemServiceManager.startService(HwServiceFactory.getWallpaperManagerServiceClassName());
                            traceEnd();
                        }
                        traceBeginAndSlog("StartTrustManager");
                        this.mSystemServiceManager.startService(TrustManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("StartAudioService");
                        this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                        traceEnd();
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
                            inputManager2.setWiredAccessoryCallbacks(new WiredAccessoryManager(context2, inputManager2));
                        } catch (Throwable e2222222222222222222) {
                            reportWtf("starting WiredAccessoryManager", e2222222222222222222);
                        }
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                            traceBeginAndSlog("StartMidiManager");
                            this.mSystemServiceManager.startService(MIDI_SERVICE_CLASS);
                            traceEnd();
                        }
                        if (this.mPackageManager.hasSystemFeature("android.hardware.usb.host") || this.mPackageManager.hasSystemFeature("android.hardware.usb.accessory") || isEmulator) {
                            traceBeginAndSlog("StartUsbService");
                            this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                            traceEnd();
                        }
                        if (isWatch) {
                            traceBeginAndSlog("StartSerialService");
                            try {
                                vrDisplayEnable = new SerialService(context2);
                                try {
                                    ServiceManager.addService("serial", vrDisplayEnable);
                                    location3 = location2;
                                } catch (Throwable th13) {
                                    e2222222222222222222 = th13;
                                    serial = vrDisplayEnable;
                                    location3 = location2;
                                    Slog.e(TAG, "Failure starting SerialService", e2222222222222222222);
                                    vrDisplayEnable = serial;
                                    traceEnd();
                                    serial2 = vrDisplayEnable;
                                    traceBeginAndSlog("StartHardwarePropertiesManagerService");
                                    vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                                    ServiceManager.addService("hardware_properties", vrDisplayEnable);
                                    hardwarePropertiesService = vrDisplayEnable;
                                    traceEnd();
                                    traceBeginAndSlog("StartTwilightService");
                                    this.mSystemServiceManager.startService(TwilightService.class);
                                    traceEnd();
                                    if (ColorDisplayController.isAvailable(context2)) {
                                    }
                                    this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                                    traceBeginAndSlog("StartSoundTrigger");
                                    this.mSystemServiceManager.startService(SoundTriggerService.class);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                    }
                                    traceBeginAndSlog("StartAppWidgerService");
                                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                    traceEnd();
                                    traceBeginAndSlog("StartVoiceRecognitionManager");
                                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                    traceEnd();
                                    if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                                    }
                                    traceBeginAndSlog("StartSensorNotification");
                                    this.mSystemServiceManager.startService(SensorNotificationService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartContextHubSystemService");
                                    this.mSystemServiceManager.startService(ContextHubSystemService.class);
                                    traceEnd();
                                    HwServiceFactory.setupHwServices(context2);
                                    traceBeginAndSlog("StartDiskStatsService");
                                    ServiceManager.addService("diskstats", new DiskStatsService(context2));
                                    traceEnd();
                                    if (this.mOnlyCore) {
                                    }
                                    vrDisplayEnable = z3;
                                    if (vrDisplayEnable) {
                                    }
                                    Slog.i(TAG, "attestation Service");
                                    attestation = HwServiceFactory.getHwAttestationService();
                                    if (attestation != null) {
                                    }
                                    if (isWatch) {
                                    }
                                    traceBeginAndSlog("StartCommonTimeManagementService");
                                    vrDisplayEnable = new CommonTimeManagementService(context2);
                                    ServiceManager.addService("commontime_management", vrDisplayEnable);
                                    traceEnd();
                                    traceBeginAndSlog("CertBlacklister");
                                    certBlacklister = new CertBlacklister(context2);
                                    traceEnd();
                                    traceBeginAndSlog("StartEmergencyAffordanceService");
                                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartDreamManager");
                                    this.mSystemServiceManager.startService(DreamManagerService.class);
                                    traceEnd();
                                    traceBeginAndSlog("AddGraphicsStatsService");
                                    ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                                    traceBeginAndSlog("StartMediaUpdateService");
                                    this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                                    statusBar2 = new MediaRouterService(context2);
                                    ServiceManager.addService("media_router", statusBar2);
                                    commonTimeMgmtService = vrDisplayEnable;
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                                    }
                                    traceBeginAndSlog("StartBackgroundDexOptService");
                                    BackgroundDexOptService.schedule(context2);
                                    traceEnd();
                                    traceBeginAndSlog("StartPruneInstantAppsJobService");
                                    PruneInstantAppsJobService.schedule(context2);
                                    traceEnd();
                                    traceBeginAndSlog("StartShortcutServiceLifecycle");
                                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartLauncherAppsService");
                                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                                    traceEnd();
                                    traceBeginAndSlog("StartCrossProfileAppsService");
                                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                    traceEnd();
                                    connectivity = connectivity2;
                                    countryDetector = countryDetector2;
                                    ipSecService = ipSecService2;
                                    networkPolicy = networkPolicy2;
                                    serialService = serial2;
                                    networkTimeUpdater = networkTimeUpdater2;
                                    hardwarePropertiesManagerService = hardwarePropertiesService;
                                    lockSettings2 = lockSettings;
                                    notification = notification3;
                                    location = location3;
                                    networkManagement = networkManagement2;
                                    networkStats = networkStats2;
                                    lockSettings5 = serviceDiscovery2;
                                    if (isWatch) {
                                    }
                                    MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                    if (isWatch) {
                                    }
                                    if (disableSlices) {
                                    }
                                    if (disableCameraService) {
                                    }
                                    if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                    }
                                    traceBeginAndSlog("StartStatsCompanionService");
                                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                    traceEnd();
                                    safeMode = wm.detectSafeMode();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    if (safeMode) {
                                    }
                                    traceBeginAndSlog("StartMmsService");
                                    mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                    traceEnd();
                                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                    }
                                    if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                    }
                                    if (isStartSysSvcCallRecord) {
                                    }
                                    traceBeginAndSlog("MakeVibratorServiceReady");
                                    telephonyRegistry2.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                                    if (lockSettings2 != null) {
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
                                    config = wm.computeNewConfiguration(0);
                                    metrics = new DisplayMetrics();
                                    w = (WindowManager) context2.getSystemService("window");
                                    w.getDefaultDisplay().getMetrics(metrics);
                                    context2.getResources().updateConfiguration(config, metrics);
                                    systemTheme = context2.getTheme();
                                    if (systemTheme.getChangingConfigurations() != 0) {
                                    }
                                    traceBeginAndSlog("MakePowerManagerServiceReady");
                                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                    traceEnd();
                                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                    traceBeginAndSlog("MakePackageManagerServiceReady");
                                    this.mPackageManagerService.systemReady();
                                    traceEnd();
                                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                    traceEnd();
                                    this.mSystemServiceManager.setSafeMode(safeMode);
                                    traceBeginAndSlog("StartDeviceSpecificServices");
                                    classes = this.mSystemContext.getResources().getStringArray(17236002);
                                    length = classes.length;
                                    lockSettings3 = lockSettings2;
                                    lockSettings4 = 0;
                                    while (lockSettings4 < length) {
                                    }
                                    context = context2;
                                    wm2 = wm;
                                    traceEnd();
                                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                    traceEnd();
                                    networkManagementF = networkManagement;
                                    networkStatsF = networkStats;
                                    networkPolicyF = networkPolicy;
                                    inputManager3 = inputManager2;
                                    connectivityF = connectivity;
                                    locationF = location;
                                    countryDetectorF = countryDetector;
                                    networkTimeUpdaterF = networkTimeUpdater;
                                    commonTimeMgmtServiceF = commonTimeMgmtService;
                                    inputManagerF = inputManager3;
                                    telephonyRegistryF = telephonyRegistry;
                                    mediaRouterF = mediaRouter;
                                    mmsServiceF = mmsService;
                                    ipSecServiceF = ipSecService;
                                    wm = wm2;
                                    activityManagerService = this.mActivityManagerService;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                    activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                }
                            } catch (Throwable th14) {
                                e2222222222222222222 = th14;
                                vrDisplayEnable = false;
                                serial = vrDisplayEnable;
                                location3 = location2;
                                Slog.e(TAG, "Failure starting SerialService", e2222222222222222222);
                                vrDisplayEnable = serial;
                                traceEnd();
                                serial2 = vrDisplayEnable;
                                traceBeginAndSlog("StartHardwarePropertiesManagerService");
                                vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                                ServiceManager.addService("hardware_properties", vrDisplayEnable);
                                hardwarePropertiesService = vrDisplayEnable;
                                traceEnd();
                                traceBeginAndSlog("StartTwilightService");
                                this.mSystemServiceManager.startService(TwilightService.class);
                                traceEnd();
                                if (ColorDisplayController.isAvailable(context2)) {
                                }
                                this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                                traceBeginAndSlog("StartSoundTrigger");
                                this.mSystemServiceManager.startService(SoundTriggerService.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                }
                                traceBeginAndSlog("StartAppWidgerService");
                                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                traceEnd();
                                traceBeginAndSlog("StartVoiceRecognitionManager");
                                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                traceEnd();
                                if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                                }
                                traceBeginAndSlog("StartSensorNotification");
                                this.mSystemServiceManager.startService(SensorNotificationService.class);
                                traceEnd();
                                traceBeginAndSlog("StartContextHubSystemService");
                                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                                traceEnd();
                                HwServiceFactory.setupHwServices(context2);
                                traceBeginAndSlog("StartDiskStatsService");
                                ServiceManager.addService("diskstats", new DiskStatsService(context2));
                                traceEnd();
                                if (this.mOnlyCore) {
                                }
                                vrDisplayEnable = z3;
                                if (vrDisplayEnable) {
                                }
                                Slog.i(TAG, "attestation Service");
                                attestation = HwServiceFactory.getHwAttestationService();
                                if (attestation != null) {
                                }
                                if (isWatch) {
                                }
                                traceBeginAndSlog("StartCommonTimeManagementService");
                                vrDisplayEnable = new CommonTimeManagementService(context2);
                                ServiceManager.addService("commontime_management", vrDisplayEnable);
                                traceEnd();
                                traceBeginAndSlog("CertBlacklister");
                                certBlacklister = new CertBlacklister(context2);
                                traceEnd();
                                traceBeginAndSlog("StartEmergencyAffordanceService");
                                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                traceEnd();
                                traceBeginAndSlog("StartDreamManager");
                                this.mSystemServiceManager.startService(DreamManagerService.class);
                                traceEnd();
                                traceBeginAndSlog("AddGraphicsStatsService");
                                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                                traceBeginAndSlog("StartMediaUpdateService");
                                this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                                statusBar2 = new MediaRouterService(context2);
                                ServiceManager.addService("media_router", statusBar2);
                                commonTimeMgmtService = vrDisplayEnable;
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                                }
                                traceBeginAndSlog("StartBackgroundDexOptService");
                                BackgroundDexOptService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartPruneInstantAppsJobService");
                                PruneInstantAppsJobService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartShortcutServiceLifecycle");
                                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                traceEnd();
                                traceBeginAndSlog("StartLauncherAppsService");
                                this.mSystemServiceManager.startService(LauncherAppsService.class);
                                traceEnd();
                                traceBeginAndSlog("StartCrossProfileAppsService");
                                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                traceEnd();
                                connectivity = connectivity2;
                                countryDetector = countryDetector2;
                                ipSecService = ipSecService2;
                                networkPolicy = networkPolicy2;
                                serialService = serial2;
                                networkTimeUpdater = networkTimeUpdater2;
                                hardwarePropertiesManagerService = hardwarePropertiesService;
                                lockSettings2 = lockSettings;
                                notification = notification3;
                                location = location3;
                                networkManagement = networkManagement2;
                                networkStats = networkStats2;
                                lockSettings5 = serviceDiscovery2;
                                if (isWatch) {
                                }
                                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                if (isWatch) {
                                }
                                if (disableSlices) {
                                }
                                if (disableCameraService) {
                                }
                                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                }
                                traceBeginAndSlog("StartStatsCompanionService");
                                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                traceEnd();
                                safeMode = wm.detectSafeMode();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                if (safeMode) {
                                }
                                traceBeginAndSlog("StartMmsService");
                                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                }
                                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                }
                                if (isStartSysSvcCallRecord) {
                                }
                                traceBeginAndSlog("MakeVibratorServiceReady");
                                telephonyRegistry2.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeLockSettingsServiceReady");
                                if (lockSettings2 != null) {
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
                                config = wm.computeNewConfiguration(0);
                                metrics = new DisplayMetrics();
                                w = (WindowManager) context2.getSystemService("window");
                                w.getDefaultDisplay().getMetrics(metrics);
                                context2.getResources().updateConfiguration(config, metrics);
                                systemTheme = context2.getTheme();
                                if (systemTheme.getChangingConfigurations() != 0) {
                                }
                                traceBeginAndSlog("MakePowerManagerServiceReady");
                                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                traceEnd();
                                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                traceBeginAndSlog("MakePackageManagerServiceReady");
                                this.mPackageManagerService.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                traceEnd();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                traceBeginAndSlog("StartDeviceSpecificServices");
                                classes = this.mSystemContext.getResources().getStringArray(17236002);
                                length = classes.length;
                                lockSettings3 = lockSettings2;
                                lockSettings4 = 0;
                                while (lockSettings4 < length) {
                                }
                                context = context2;
                                wm2 = wm;
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                traceEnd();
                                networkManagementF = networkManagement;
                                networkStatsF = networkStats;
                                networkPolicyF = networkPolicy;
                                inputManager3 = inputManager2;
                                connectivityF = connectivity;
                                locationF = location;
                                countryDetectorF = countryDetector;
                                networkTimeUpdaterF = networkTimeUpdater;
                                commonTimeMgmtServiceF = commonTimeMgmtService;
                                inputManagerF = inputManager3;
                                telephonyRegistryF = telephonyRegistry;
                                mediaRouterF = mediaRouter;
                                mmsServiceF = mmsService;
                                ipSecServiceF = ipSecService;
                                wm = wm2;
                                activityManagerService = this.mActivityManagerService;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                            }
                            traceEnd();
                            serial2 = vrDisplayEnable;
                        } else {
                            location3 = location2;
                        }
                        traceBeginAndSlog("StartHardwarePropertiesManagerService");
                        try {
                            vrDisplayEnable = new HardwarePropertiesManagerService(context2);
                            try {
                                ServiceManager.addService("hardware_properties", vrDisplayEnable);
                                hardwarePropertiesService = vrDisplayEnable;
                            } catch (Throwable th15) {
                                e2222222222222222222 = th15;
                                hardwarePropertiesService2 = vrDisplayEnable;
                                Slog.e(TAG, "Failure starting HardwarePropertiesManagerService", e2222222222222222222);
                                hardwarePropertiesService = hardwarePropertiesService2;
                                traceEnd();
                                traceBeginAndSlog("StartTwilightService");
                                this.mSystemServiceManager.startService(TwilightService.class);
                                traceEnd();
                                if (ColorDisplayController.isAvailable(context2)) {
                                }
                                this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                                traceBeginAndSlog("StartSoundTrigger");
                                this.mSystemServiceManager.startService(SoundTriggerService.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                }
                                traceBeginAndSlog("StartAppWidgerService");
                                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                traceEnd();
                                traceBeginAndSlog("StartVoiceRecognitionManager");
                                this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                traceEnd();
                                if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                                }
                                traceBeginAndSlog("StartSensorNotification");
                                this.mSystemServiceManager.startService(SensorNotificationService.class);
                                traceEnd();
                                traceBeginAndSlog("StartContextHubSystemService");
                                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                                traceEnd();
                                HwServiceFactory.setupHwServices(context2);
                                traceBeginAndSlog("StartDiskStatsService");
                                ServiceManager.addService("diskstats", new DiskStatsService(context2));
                                traceEnd();
                                if (this.mOnlyCore) {
                                }
                                vrDisplayEnable = z3;
                                if (vrDisplayEnable) {
                                }
                                Slog.i(TAG, "attestation Service");
                                attestation = HwServiceFactory.getHwAttestationService();
                                if (attestation != null) {
                                }
                                if (isWatch) {
                                }
                                traceBeginAndSlog("StartCommonTimeManagementService");
                                vrDisplayEnable = new CommonTimeManagementService(context2);
                                ServiceManager.addService("commontime_management", vrDisplayEnable);
                                traceEnd();
                                traceBeginAndSlog("CertBlacklister");
                                certBlacklister = new CertBlacklister(context2);
                                traceEnd();
                                traceBeginAndSlog("StartEmergencyAffordanceService");
                                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                traceEnd();
                                traceBeginAndSlog("StartDreamManager");
                                this.mSystemServiceManager.startService(DreamManagerService.class);
                                traceEnd();
                                traceBeginAndSlog("AddGraphicsStatsService");
                                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                                traceBeginAndSlog("StartMediaUpdateService");
                                this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                                statusBar2 = new MediaRouterService(context2);
                                ServiceManager.addService("media_router", statusBar2);
                                commonTimeMgmtService = vrDisplayEnable;
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                                }
                                traceBeginAndSlog("StartBackgroundDexOptService");
                                BackgroundDexOptService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartPruneInstantAppsJobService");
                                PruneInstantAppsJobService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartShortcutServiceLifecycle");
                                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                traceEnd();
                                traceBeginAndSlog("StartLauncherAppsService");
                                this.mSystemServiceManager.startService(LauncherAppsService.class);
                                traceEnd();
                                traceBeginAndSlog("StartCrossProfileAppsService");
                                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                traceEnd();
                                connectivity = connectivity2;
                                countryDetector = countryDetector2;
                                ipSecService = ipSecService2;
                                networkPolicy = networkPolicy2;
                                serialService = serial2;
                                networkTimeUpdater = networkTimeUpdater2;
                                hardwarePropertiesManagerService = hardwarePropertiesService;
                                lockSettings2 = lockSettings;
                                notification = notification3;
                                location = location3;
                                networkManagement = networkManagement2;
                                networkStats = networkStats2;
                                lockSettings5 = serviceDiscovery2;
                                if (isWatch) {
                                }
                                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                if (isWatch) {
                                }
                                if (disableSlices) {
                                }
                                if (disableCameraService) {
                                }
                                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                }
                                traceBeginAndSlog("StartStatsCompanionService");
                                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                traceEnd();
                                safeMode = wm.detectSafeMode();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                if (safeMode) {
                                }
                                traceBeginAndSlog("StartMmsService");
                                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                }
                                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                }
                                if (isStartSysSvcCallRecord) {
                                }
                                traceBeginAndSlog("MakeVibratorServiceReady");
                                telephonyRegistry2.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeLockSettingsServiceReady");
                                if (lockSettings2 != null) {
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
                                config = wm.computeNewConfiguration(0);
                                metrics = new DisplayMetrics();
                                w = (WindowManager) context2.getSystemService("window");
                                w.getDefaultDisplay().getMetrics(metrics);
                                context2.getResources().updateConfiguration(config, metrics);
                                systemTheme = context2.getTheme();
                                if (systemTheme.getChangingConfigurations() != 0) {
                                }
                                traceBeginAndSlog("MakePowerManagerServiceReady");
                                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                traceEnd();
                                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                traceBeginAndSlog("MakePackageManagerServiceReady");
                                this.mPackageManagerService.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                traceEnd();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                traceBeginAndSlog("StartDeviceSpecificServices");
                                classes = this.mSystemContext.getResources().getStringArray(17236002);
                                length = classes.length;
                                lockSettings3 = lockSettings2;
                                lockSettings4 = 0;
                                while (lockSettings4 < length) {
                                }
                                context = context2;
                                wm2 = wm;
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                traceEnd();
                                networkManagementF = networkManagement;
                                networkStatsF = networkStats;
                                networkPolicyF = networkPolicy;
                                inputManager3 = inputManager2;
                                connectivityF = connectivity;
                                locationF = location;
                                countryDetectorF = countryDetector;
                                networkTimeUpdaterF = networkTimeUpdater;
                                commonTimeMgmtServiceF = commonTimeMgmtService;
                                inputManagerF = inputManager3;
                                telephonyRegistryF = telephonyRegistry;
                                mediaRouterF = mediaRouter;
                                mmsServiceF = mmsService;
                                ipSecServiceF = ipSecService;
                                wm = wm2;
                                activityManagerService = this.mActivityManagerService;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                            }
                        } catch (Throwable th16) {
                            e2222222222222222222 = th16;
                            vrDisplayEnable = false;
                            hardwarePropertiesService2 = vrDisplayEnable;
                            Slog.e(TAG, "Failure starting HardwarePropertiesManagerService", e2222222222222222222);
                            hardwarePropertiesService = hardwarePropertiesService2;
                            traceEnd();
                            traceBeginAndSlog("StartTwilightService");
                            this.mSystemServiceManager.startService(TwilightService.class);
                            traceEnd();
                            if (ColorDisplayController.isAvailable(context2)) {
                            }
                            this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                            traceBeginAndSlog("StartSoundTrigger");
                            this.mSystemServiceManager.startService(SoundTriggerService.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                            }
                            traceBeginAndSlog("StartAppWidgerService");
                            this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                            traceEnd();
                            traceBeginAndSlog("StartVoiceRecognitionManager");
                            this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                            traceEnd();
                            if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
                            }
                            traceBeginAndSlog("StartSensorNotification");
                            this.mSystemServiceManager.startService(SensorNotificationService.class);
                            traceEnd();
                            traceBeginAndSlog("StartContextHubSystemService");
                            this.mSystemServiceManager.startService(ContextHubSystemService.class);
                            traceEnd();
                            HwServiceFactory.setupHwServices(context2);
                            traceBeginAndSlog("StartDiskStatsService");
                            ServiceManager.addService("diskstats", new DiskStatsService(context2));
                            traceEnd();
                            if (this.mOnlyCore) {
                            }
                            vrDisplayEnable = z3;
                            if (vrDisplayEnable) {
                            }
                            Slog.i(TAG, "attestation Service");
                            attestation = HwServiceFactory.getHwAttestationService();
                            if (attestation != null) {
                            }
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartCommonTimeManagementService");
                            vrDisplayEnable = new CommonTimeManagementService(context2);
                            ServiceManager.addService("commontime_management", vrDisplayEnable);
                            traceEnd();
                            traceBeginAndSlog("CertBlacklister");
                            certBlacklister = new CertBlacklister(context2);
                            traceEnd();
                            traceBeginAndSlog("StartEmergencyAffordanceService");
                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDreamManager");
                            this.mSystemServiceManager.startService(DreamManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("AddGraphicsStatsService");
                            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                            traceBeginAndSlog("StartMediaUpdateService");
                            this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                            statusBar2 = new MediaRouterService(context2);
                            ServiceManager.addService("media_router", statusBar2);
                            commonTimeMgmtService = vrDisplayEnable;
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            }
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartPruneInstantAppsJobService");
                            PruneInstantAppsJobService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            connectivity = connectivity2;
                            countryDetector = countryDetector2;
                            ipSecService = ipSecService2;
                            networkPolicy = networkPolicy2;
                            serialService = serial2;
                            networkTimeUpdater = networkTimeUpdater2;
                            hardwarePropertiesManagerService = hardwarePropertiesService;
                            lockSettings2 = lockSettings;
                            notification = notification3;
                            location = location3;
                            networkManagement = networkManagement2;
                            networkStats = networkStats2;
                            lockSettings5 = serviceDiscovery2;
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartTwilightService");
                        this.mSystemServiceManager.startService(TwilightService.class);
                        traceEnd();
                        if (ColorDisplayController.isAvailable(context2)) {
                            traceBeginAndSlog("StartNightDisplay");
                            this.mSystemServiceManager.startService(ColorDisplayService.class);
                            traceEnd();
                        }
                        try {
                            this.mSystemServiceManager.startService("com.android.server.job.HwJobSchedulerService");
                        } catch (RuntimeException e17) {
                            Slog.w(TAG, "create HwJobSchedulerService failed");
                            this.mSystemServiceManager.startService(JobSchedulerService.class);
                        }
                        traceBeginAndSlog("StartSoundTrigger");
                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                            traceBeginAndSlog("StartBackupManager");
                            this.mSystemServiceManager.startService(BACKUP_MANAGER_SERVICE_CLASS);
                            traceEnd();
                        }
                        if (this.mPackageManager.hasSystemFeature("android.software.app_widgets") || context2.getResources().getBoolean(true)) {
                            traceBeginAndSlog("StartAppWidgerService");
                            this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                            traceEnd();
                        }
                        traceBeginAndSlog("StartVoiceRecognitionManager");
                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                        traceEnd();
                        if (GestureLauncherService.isGestureLauncherEnabled(context2.getResources())) {
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
                        HwServiceFactory.setupHwServices(context2);
                        traceBeginAndSlog("StartDiskStatsService");
                        try {
                            ServiceManager.addService("diskstats", new DiskStatsService(context2));
                        } catch (Throwable e22222222222222222222) {
                            reportWtf("starting DiskStats Service", e22222222222222222222);
                        }
                        traceEnd();
                        z3 = this.mOnlyCore && context2.getResources().getBoolean(true);
                        vrDisplayEnable = z3;
                        if (vrDisplayEnable) {
                            traceBeginAndSlog("StartTimeZoneRulesManagerService");
                            this.mSystemServiceManager.startService(TIME_ZONE_RULES_MANAGER_SERVICE_CLASS);
                            traceEnd();
                        }
                        boolean startRulesManagerService;
                        try {
                            Slog.i(TAG, "attestation Service");
                            attestation = HwServiceFactory.getHwAttestationService();
                            if (attestation != null) {
                                startRulesManagerService = vrDisplayEnable;
                                try {
                                    ServiceManager.addService("attestation_service", attestation.getInstance(context2));
                                } catch (Throwable th17) {
                                    e22222222222222222222 = th17;
                                }
                            } else {
                                startRulesManagerService = vrDisplayEnable;
                            }
                        } catch (Throwable th18) {
                            e22222222222222222222 = th18;
                            startRulesManagerService = vrDisplayEnable;
                            Slog.i(TAG, "attestation_service failed");
                            reportWtf("attestation Service", e22222222222222222222);
                            if (isWatch) {
                            }
                            traceBeginAndSlog("StartCommonTimeManagementService");
                            vrDisplayEnable = new CommonTimeManagementService(context2);
                            ServiceManager.addService("commontime_management", vrDisplayEnable);
                            traceEnd();
                            traceBeginAndSlog("CertBlacklister");
                            certBlacklister = new CertBlacklister(context2);
                            traceEnd();
                            traceBeginAndSlog("StartEmergencyAffordanceService");
                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDreamManager");
                            this.mSystemServiceManager.startService(DreamManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("AddGraphicsStatsService");
                            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                            traceBeginAndSlog("StartMediaUpdateService");
                            this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                            statusBar2 = new MediaRouterService(context2);
                            ServiceManager.addService("media_router", statusBar2);
                            commonTimeMgmtService = vrDisplayEnable;
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            }
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartPruneInstantAppsJobService");
                            PruneInstantAppsJobService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            connectivity = connectivity2;
                            countryDetector = countryDetector2;
                            ipSecService = ipSecService2;
                            networkPolicy = networkPolicy2;
                            serialService = serial2;
                            networkTimeUpdater = networkTimeUpdater2;
                            hardwarePropertiesManagerService = hardwarePropertiesService;
                            lockSettings2 = lockSettings;
                            notification = notification3;
                            location = location3;
                            networkManagement = networkManagement2;
                            networkStats = networkStats2;
                            lockSettings5 = serviceDiscovery2;
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        if (isWatch) {
                            traceBeginAndSlog("StartNetworkTimeUpdateService");
                            try {
                                vrDisplayEnable = new NetworkTimeUpdateService(context2);
                                try {
                                    ServiceManager.addService("network_time_update_service", vrDisplayEnable);
                                } catch (Throwable th19) {
                                    e22222222222222222222 = th19;
                                }
                            } catch (Throwable th20) {
                                e22222222222222222222 = th20;
                                vrDisplayEnable = false;
                                reportWtf("starting NetworkTimeUpdate service", e22222222222222222222);
                                networkTimeUpdater2 = vrDisplayEnable;
                                traceEnd();
                                traceBeginAndSlog("StartCommonTimeManagementService");
                                vrDisplayEnable = new CommonTimeManagementService(context2);
                                ServiceManager.addService("commontime_management", vrDisplayEnable);
                                traceEnd();
                                traceBeginAndSlog("CertBlacklister");
                                certBlacklister = new CertBlacklister(context2);
                                traceEnd();
                                traceBeginAndSlog("StartEmergencyAffordanceService");
                                this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                traceEnd();
                                traceBeginAndSlog("StartDreamManager");
                                this.mSystemServiceManager.startService(DreamManagerService.class);
                                traceEnd();
                                traceBeginAndSlog("AddGraphicsStatsService");
                                ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                                traceBeginAndSlog("StartMediaUpdateService");
                                this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                                statusBar2 = new MediaRouterService(context2);
                                ServiceManager.addService("media_router", statusBar2);
                                commonTimeMgmtService = vrDisplayEnable;
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                                }
                                traceBeginAndSlog("StartBackgroundDexOptService");
                                BackgroundDexOptService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartPruneInstantAppsJobService");
                                PruneInstantAppsJobService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartShortcutServiceLifecycle");
                                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                traceEnd();
                                traceBeginAndSlog("StartLauncherAppsService");
                                this.mSystemServiceManager.startService(LauncherAppsService.class);
                                traceEnd();
                                traceBeginAndSlog("StartCrossProfileAppsService");
                                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                traceEnd();
                                connectivity = connectivity2;
                                countryDetector = countryDetector2;
                                ipSecService = ipSecService2;
                                networkPolicy = networkPolicy2;
                                serialService = serial2;
                                networkTimeUpdater = networkTimeUpdater2;
                                hardwarePropertiesManagerService = hardwarePropertiesService;
                                lockSettings2 = lockSettings;
                                notification = notification3;
                                location = location3;
                                networkManagement = networkManagement2;
                                networkStats = networkStats2;
                                lockSettings5 = serviceDiscovery2;
                                if (isWatch) {
                                }
                                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                if (isWatch) {
                                }
                                if (disableSlices) {
                                }
                                if (disableCameraService) {
                                }
                                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                }
                                traceBeginAndSlog("StartStatsCompanionService");
                                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                traceEnd();
                                safeMode = wm.detectSafeMode();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                if (safeMode) {
                                }
                                traceBeginAndSlog("StartMmsService");
                                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                }
                                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                }
                                if (isStartSysSvcCallRecord) {
                                }
                                traceBeginAndSlog("MakeVibratorServiceReady");
                                telephonyRegistry2.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeLockSettingsServiceReady");
                                if (lockSettings2 != null) {
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
                                config = wm.computeNewConfiguration(0);
                                metrics = new DisplayMetrics();
                                w = (WindowManager) context2.getSystemService("window");
                                w.getDefaultDisplay().getMetrics(metrics);
                                context2.getResources().updateConfiguration(config, metrics);
                                systemTheme = context2.getTheme();
                                if (systemTheme.getChangingConfigurations() != 0) {
                                }
                                traceBeginAndSlog("MakePowerManagerServiceReady");
                                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                traceEnd();
                                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                traceBeginAndSlog("MakePackageManagerServiceReady");
                                this.mPackageManagerService.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                traceEnd();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                traceBeginAndSlog("StartDeviceSpecificServices");
                                classes = this.mSystemContext.getResources().getStringArray(17236002);
                                length = classes.length;
                                lockSettings3 = lockSettings2;
                                lockSettings4 = 0;
                                while (lockSettings4 < length) {
                                }
                                context = context2;
                                wm2 = wm;
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                traceEnd();
                                networkManagementF = networkManagement;
                                networkStatsF = networkStats;
                                networkPolicyF = networkPolicy;
                                inputManager3 = inputManager2;
                                connectivityF = connectivity;
                                locationF = location;
                                countryDetectorF = countryDetector;
                                networkTimeUpdaterF = networkTimeUpdater;
                                commonTimeMgmtServiceF = commonTimeMgmtService;
                                inputManagerF = inputManager3;
                                telephonyRegistryF = telephonyRegistry;
                                mediaRouterF = mediaRouter;
                                mmsServiceF = mmsService;
                                ipSecServiceF = ipSecService;
                                wm = wm2;
                                activityManagerService = this.mActivityManagerService;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                            }
                            networkTimeUpdater2 = vrDisplayEnable;
                            traceEnd();
                        }
                        traceBeginAndSlog("StartCommonTimeManagementService");
                        try {
                            vrDisplayEnable = new CommonTimeManagementService(context2);
                            ServiceManager.addService("commontime_management", vrDisplayEnable);
                        } catch (Throwable th21) {
                            e22222222222222222222 = th21;
                            vrDisplayEnable = false;
                            reportWtf("starting CommonTimeManagementService service", e22222222222222222222);
                            traceEnd();
                            traceBeginAndSlog("CertBlacklister");
                            certBlacklister = new CertBlacklister(context2);
                            traceEnd();
                            traceBeginAndSlog("StartEmergencyAffordanceService");
                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                            traceEnd();
                            traceBeginAndSlog("StartDreamManager");
                            this.mSystemServiceManager.startService(DreamManagerService.class);
                            traceEnd();
                            traceBeginAndSlog("AddGraphicsStatsService");
                            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                            traceBeginAndSlog("StartMediaUpdateService");
                            this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                            statusBar2 = new MediaRouterService(context2);
                            ServiceManager.addService("media_router", statusBar2);
                            commonTimeMgmtService = vrDisplayEnable;
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            }
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartPruneInstantAppsJobService");
                            PruneInstantAppsJobService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            connectivity = connectivity2;
                            countryDetector = countryDetector2;
                            ipSecService = ipSecService2;
                            networkPolicy = networkPolicy2;
                            serialService = serial2;
                            networkTimeUpdater = networkTimeUpdater2;
                            hardwarePropertiesManagerService = hardwarePropertiesService;
                            lockSettings2 = lockSettings;
                            notification = notification3;
                            location = location3;
                            networkManagement = networkManagement2;
                            networkStats = networkStats2;
                            lockSettings5 = serviceDiscovery2;
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        traceEnd();
                        traceBeginAndSlog("CertBlacklister");
                        try {
                            certBlacklister = new CertBlacklister(context2);
                        } catch (Throwable e222222222222222222222) {
                            reportWtf("starting CertBlacklister", e222222222222222222222);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartEmergencyAffordanceService");
                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                        traceEnd();
                        traceBeginAndSlog("StartDreamManager");
                        this.mSystemServiceManager.startService(DreamManagerService.class);
                        traceEnd();
                        traceBeginAndSlog("AddGraphicsStatsService");
                        ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE, new GraphicsStatsService(context2));
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
                        traceBeginAndSlog("StartMediaUpdateService");
                        this.mSystemServiceManager.startService(MediaUpdateService.class);
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
                            statusBar2 = new MediaRouterService(context2);
                            ServiceManager.addService("media_router", statusBar2);
                            commonTimeMgmtService = vrDisplayEnable;
                        } catch (Throwable th22) {
                            e222222222222222222222 = th22;
                            statusBar2 = null;
                            commonTimeMgmtService = vrDisplayEnable;
                            reportWtf("starting MediaRouterService", e222222222222222222222);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            }
                            traceBeginAndSlog("StartBackgroundDexOptService");
                            BackgroundDexOptService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartPruneInstantAppsJobService");
                            PruneInstantAppsJobService.schedule(context2);
                            traceEnd();
                            traceBeginAndSlog("StartShortcutServiceLifecycle");
                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                            traceEnd();
                            traceBeginAndSlog("StartLauncherAppsService");
                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                            traceEnd();
                            traceBeginAndSlog("StartCrossProfileAppsService");
                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                            traceEnd();
                            connectivity = connectivity2;
                            countryDetector = countryDetector2;
                            ipSecService = ipSecService2;
                            networkPolicy = networkPolicy2;
                            serialService = serial2;
                            networkTimeUpdater = networkTimeUpdater2;
                            hardwarePropertiesManagerService = hardwarePropertiesService;
                            lockSettings2 = lockSettings;
                            notification = notification3;
                            location = location3;
                            networkManagement = networkManagement2;
                            networkStats = networkStats2;
                            lockSettings5 = serviceDiscovery2;
                            if (isWatch) {
                            }
                            MediaProjectionManagerService.sHasStartedInSystemserver = true;
                            if (isWatch) {
                            }
                            if (disableSlices) {
                            }
                            if (disableCameraService) {
                            }
                            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                            }
                            traceBeginAndSlog("StartStatsCompanionService");
                            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                            traceEnd();
                            safeMode = wm.detectSafeMode();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            if (safeMode) {
                            }
                            traceBeginAndSlog("StartMmsService");
                            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                            traceEnd();
                            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                            }
                            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                            }
                            if (isStartSysSvcCallRecord) {
                            }
                            traceBeginAndSlog("MakeVibratorServiceReady");
                            telephonyRegistry2.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeLockSettingsServiceReady");
                            if (lockSettings2 != null) {
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
                            config = wm.computeNewConfiguration(0);
                            metrics = new DisplayMetrics();
                            w = (WindowManager) context2.getSystemService("window");
                            w.getDefaultDisplay().getMetrics(metrics);
                            context2.getResources().updateConfiguration(config, metrics);
                            systemTheme = context2.getTheme();
                            if (systemTheme.getChangingConfigurations() != 0) {
                            }
                            traceBeginAndSlog("MakePowerManagerServiceReady");
                            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                            traceEnd();
                            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                            traceBeginAndSlog("MakePackageManagerServiceReady");
                            this.mPackageManagerService.systemReady();
                            traceEnd();
                            traceBeginAndSlog("MakeDisplayManagerServiceReady");
                            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                            traceEnd();
                            this.mSystemServiceManager.setSafeMode(safeMode);
                            traceBeginAndSlog("StartDeviceSpecificServices");
                            classes = this.mSystemContext.getResources().getStringArray(17236002);
                            length = classes.length;
                            lockSettings3 = lockSettings2;
                            lockSettings4 = 0;
                            while (lockSettings4 < length) {
                            }
                            context = context2;
                            wm2 = wm;
                            traceEnd();
                            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                            traceEnd();
                            networkManagementF = networkManagement;
                            networkStatsF = networkStats;
                            networkPolicyF = networkPolicy;
                            inputManager3 = inputManager2;
                            connectivityF = connectivity;
                            locationF = location;
                            countryDetectorF = countryDetector;
                            networkTimeUpdaterF = networkTimeUpdater;
                            commonTimeMgmtServiceF = commonTimeMgmtService;
                            inputManagerF = inputManager3;
                            telephonyRegistryF = telephonyRegistry;
                            mediaRouterF = mediaRouter;
                            mmsServiceF = mmsService;
                            ipSecServiceF = ipSecService;
                            wm = wm2;
                            activityManagerService = this.mActivityManagerService;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                        }
                        traceEnd();
                        if (this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                            traceBeginAndSlog("StartFingerprintSensor");
                            try {
                                Class serviceClass;
                                vrDisplayEnable = HwServiceFactory.getHwFingerprintService();
                                if (vrDisplayEnable) {
                                    try {
                                        Class<SystemService> serviceClass2 = vrDisplayEnable.createServiceClass();
                                        IHwFingerprintService ifs = vrDisplayEnable;
                                        Slog.i(TAG, "serviceClass doesn't null");
                                        serviceClass = serviceClass2;
                                    } catch (Throwable th23) {
                                        e222222222222222222222 = th23;
                                        mediaRouter = statusBar2;
                                        Slog.e(TAG, "Start fingerprintservice error", e222222222222222222222);
                                        traceEnd();
                                        traceBeginAndSlog("StartBackgroundDexOptService");
                                        BackgroundDexOptService.schedule(context2);
                                        traceEnd();
                                        traceBeginAndSlog("StartPruneInstantAppsJobService");
                                        PruneInstantAppsJobService.schedule(context2);
                                        traceEnd();
                                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                        traceEnd();
                                        traceBeginAndSlog("StartLauncherAppsService");
                                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                                        traceEnd();
                                        traceBeginAndSlog("StartCrossProfileAppsService");
                                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                        traceEnd();
                                        connectivity = connectivity2;
                                        countryDetector = countryDetector2;
                                        ipSecService = ipSecService2;
                                        networkPolicy = networkPolicy2;
                                        serialService = serial2;
                                        networkTimeUpdater = networkTimeUpdater2;
                                        hardwarePropertiesManagerService = hardwarePropertiesService;
                                        lockSettings2 = lockSettings;
                                        notification = notification3;
                                        location = location3;
                                        networkManagement = networkManagement2;
                                        networkStats = networkStats2;
                                        lockSettings5 = serviceDiscovery2;
                                        if (isWatch) {
                                        }
                                        MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                        if (isWatch) {
                                        }
                                        if (disableSlices) {
                                        }
                                        if (disableCameraService) {
                                        }
                                        if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                        }
                                        traceBeginAndSlog("StartStatsCompanionService");
                                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                        traceEnd();
                                        safeMode = wm.detectSafeMode();
                                        this.mSystemServiceManager.setSafeMode(safeMode);
                                        if (safeMode) {
                                        }
                                        traceBeginAndSlog("StartMmsService");
                                        mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                        traceEnd();
                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                        }
                                        if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                        }
                                        if (isStartSysSvcCallRecord) {
                                        }
                                        traceBeginAndSlog("MakeVibratorServiceReady");
                                        telephonyRegistry2.systemReady();
                                        traceEnd();
                                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                                        if (lockSettings2 != null) {
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
                                        config = wm.computeNewConfiguration(0);
                                        metrics = new DisplayMetrics();
                                        w = (WindowManager) context2.getSystemService("window");
                                        w.getDefaultDisplay().getMetrics(metrics);
                                        context2.getResources().updateConfiguration(config, metrics);
                                        systemTheme = context2.getTheme();
                                        if (systemTheme.getChangingConfigurations() != 0) {
                                        }
                                        traceBeginAndSlog("MakePowerManagerServiceReady");
                                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                        traceEnd();
                                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                        traceBeginAndSlog("MakePackageManagerServiceReady");
                                        this.mPackageManagerService.systemReady();
                                        traceEnd();
                                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                        traceEnd();
                                        this.mSystemServiceManager.setSafeMode(safeMode);
                                        traceBeginAndSlog("StartDeviceSpecificServices");
                                        classes = this.mSystemContext.getResources().getStringArray(17236002);
                                        length = classes.length;
                                        lockSettings3 = lockSettings2;
                                        lockSettings4 = 0;
                                        while (lockSettings4 < length) {
                                        }
                                        context = context2;
                                        wm2 = wm;
                                        traceEnd();
                                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                        traceEnd();
                                        networkManagementF = networkManagement;
                                        networkStatsF = networkStats;
                                        networkPolicyF = networkPolicy;
                                        inputManager3 = inputManager2;
                                        connectivityF = connectivity;
                                        locationF = location;
                                        countryDetectorF = countryDetector;
                                        networkTimeUpdaterF = networkTimeUpdater;
                                        commonTimeMgmtServiceF = commonTimeMgmtService;
                                        inputManagerF = inputManager3;
                                        telephonyRegistryF = telephonyRegistry;
                                        mediaRouterF = mediaRouter;
                                        mmsServiceF = mmsService;
                                        ipSecServiceF = ipSecService;
                                        wm = wm2;
                                        activityManagerService = this.mActivityManagerService;
                                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                        activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                    }
                                }
                                Object obj = vrDisplayEnable;
                                Class<SystemService> serviceClass3 = null;
                                Slog.e(TAG, "HwFingerPrintService is null!");
                                serviceClass = serviceClass3;
                                if (serviceClass != null) {
                                    mediaRouter = statusBar2;
                                    try {
                                        Slog.i(TAG, "start HwFingerPrintService");
                                        this.mSystemServiceManager.startService(serviceClass);
                                    } catch (Throwable th24) {
                                        e222222222222222222222 = th24;
                                        Slog.e(TAG, "Start fingerprintservice error", e222222222222222222222);
                                        traceEnd();
                                        traceBeginAndSlog("StartBackgroundDexOptService");
                                        BackgroundDexOptService.schedule(context2);
                                        traceEnd();
                                        traceBeginAndSlog("StartPruneInstantAppsJobService");
                                        PruneInstantAppsJobService.schedule(context2);
                                        traceEnd();
                                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                        traceEnd();
                                        traceBeginAndSlog("StartLauncherAppsService");
                                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                                        traceEnd();
                                        traceBeginAndSlog("StartCrossProfileAppsService");
                                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                        traceEnd();
                                        connectivity = connectivity2;
                                        countryDetector = countryDetector2;
                                        ipSecService = ipSecService2;
                                        networkPolicy = networkPolicy2;
                                        serialService = serial2;
                                        networkTimeUpdater = networkTimeUpdater2;
                                        hardwarePropertiesManagerService = hardwarePropertiesService;
                                        lockSettings2 = lockSettings;
                                        notification = notification3;
                                        location = location3;
                                        networkManagement = networkManagement2;
                                        networkStats = networkStats2;
                                        lockSettings5 = serviceDiscovery2;
                                        if (isWatch) {
                                        }
                                        MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                        if (isWatch) {
                                        }
                                        if (disableSlices) {
                                        }
                                        if (disableCameraService) {
                                        }
                                        if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                        }
                                        traceBeginAndSlog("StartStatsCompanionService");
                                        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                        traceEnd();
                                        safeMode = wm.detectSafeMode();
                                        this.mSystemServiceManager.setSafeMode(safeMode);
                                        if (safeMode) {
                                        }
                                        traceBeginAndSlog("StartMmsService");
                                        mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                        traceEnd();
                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                        }
                                        if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                        }
                                        if (isStartSysSvcCallRecord) {
                                        }
                                        traceBeginAndSlog("MakeVibratorServiceReady");
                                        telephonyRegistry2.systemReady();
                                        traceEnd();
                                        traceBeginAndSlog("MakeLockSettingsServiceReady");
                                        if (lockSettings2 != null) {
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
                                        config = wm.computeNewConfiguration(0);
                                        metrics = new DisplayMetrics();
                                        w = (WindowManager) context2.getSystemService("window");
                                        w.getDefaultDisplay().getMetrics(metrics);
                                        context2.getResources().updateConfiguration(config, metrics);
                                        systemTheme = context2.getTheme();
                                        if (systemTheme.getChangingConfigurations() != 0) {
                                        }
                                        traceBeginAndSlog("MakePowerManagerServiceReady");
                                        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                        traceEnd();
                                        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                        traceBeginAndSlog("MakePackageManagerServiceReady");
                                        this.mPackageManagerService.systemReady();
                                        traceEnd();
                                        traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                        traceEnd();
                                        this.mSystemServiceManager.setSafeMode(safeMode);
                                        traceBeginAndSlog("StartDeviceSpecificServices");
                                        classes = this.mSystemContext.getResources().getStringArray(17236002);
                                        length = classes.length;
                                        lockSettings3 = lockSettings2;
                                        lockSettings4 = 0;
                                        while (lockSettings4 < length) {
                                        }
                                        context = context2;
                                        wm2 = wm;
                                        traceEnd();
                                        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                        traceEnd();
                                        networkManagementF = networkManagement;
                                        networkStatsF = networkStats;
                                        networkPolicyF = networkPolicy;
                                        inputManager3 = inputManager2;
                                        connectivityF = connectivity;
                                        locationF = location;
                                        countryDetectorF = countryDetector;
                                        networkTimeUpdaterF = networkTimeUpdater;
                                        commonTimeMgmtServiceF = commonTimeMgmtService;
                                        inputManagerF = inputManager3;
                                        telephonyRegistryF = telephonyRegistry;
                                        mediaRouterF = mediaRouter;
                                        mmsServiceF = mmsService;
                                        ipSecServiceF = ipSecService;
                                        wm = wm2;
                                        activityManagerService = this.mActivityManagerService;
                                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                        activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                                    }
                                }
                                mediaRouter = statusBar2;
                                this.mSystemServiceManager.startService(FingerprintService.class);
                                Slog.i(TAG, "FingerPrintService ready");
                            } catch (Throwable th25) {
                                e222222222222222222222 = th25;
                                mediaRouter = statusBar2;
                                Slog.e(TAG, "Start fingerprintservice error", e222222222222222222222);
                                traceEnd();
                                traceBeginAndSlog("StartBackgroundDexOptService");
                                BackgroundDexOptService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartPruneInstantAppsJobService");
                                PruneInstantAppsJobService.schedule(context2);
                                traceEnd();
                                traceBeginAndSlog("StartShortcutServiceLifecycle");
                                this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                traceEnd();
                                traceBeginAndSlog("StartLauncherAppsService");
                                this.mSystemServiceManager.startService(LauncherAppsService.class);
                                traceEnd();
                                traceBeginAndSlog("StartCrossProfileAppsService");
                                this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                traceEnd();
                                connectivity = connectivity2;
                                countryDetector = countryDetector2;
                                ipSecService = ipSecService2;
                                networkPolicy = networkPolicy2;
                                serialService = serial2;
                                networkTimeUpdater = networkTimeUpdater2;
                                hardwarePropertiesManagerService = hardwarePropertiesService;
                                lockSettings2 = lockSettings;
                                notification = notification3;
                                location = location3;
                                networkManagement = networkManagement2;
                                networkStats = networkStats2;
                                lockSettings5 = serviceDiscovery2;
                                if (isWatch) {
                                }
                                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                                if (isWatch) {
                                }
                                if (disableSlices) {
                                }
                                if (disableCameraService) {
                                }
                                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                }
                                traceBeginAndSlog("StartStatsCompanionService");
                                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                                traceEnd();
                                safeMode = wm.detectSafeMode();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                if (safeMode) {
                                }
                                traceBeginAndSlog("StartMmsService");
                                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                traceEnd();
                                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                }
                                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                                }
                                if (isStartSysSvcCallRecord) {
                                }
                                traceBeginAndSlog("MakeVibratorServiceReady");
                                telephonyRegistry2.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeLockSettingsServiceReady");
                                if (lockSettings2 != null) {
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
                                config = wm.computeNewConfiguration(0);
                                metrics = new DisplayMetrics();
                                w = (WindowManager) context2.getSystemService("window");
                                w.getDefaultDisplay().getMetrics(metrics);
                                context2.getResources().updateConfiguration(config, metrics);
                                systemTheme = context2.getTheme();
                                if (systemTheme.getChangingConfigurations() != 0) {
                                }
                                traceBeginAndSlog("MakePowerManagerServiceReady");
                                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                                traceEnd();
                                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                                traceBeginAndSlog("MakePackageManagerServiceReady");
                                this.mPackageManagerService.systemReady();
                                traceEnd();
                                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                traceEnd();
                                this.mSystemServiceManager.setSafeMode(safeMode);
                                traceBeginAndSlog("StartDeviceSpecificServices");
                                classes = this.mSystemContext.getResources().getStringArray(17236002);
                                length = classes.length;
                                lockSettings3 = lockSettings2;
                                lockSettings4 = 0;
                                while (lockSettings4 < length) {
                                }
                                context = context2;
                                wm2 = wm;
                                traceEnd();
                                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                traceEnd();
                                networkManagementF = networkManagement;
                                networkStatsF = networkStats;
                                networkPolicyF = networkPolicy;
                                inputManager3 = inputManager2;
                                connectivityF = connectivity;
                                locationF = location;
                                countryDetectorF = countryDetector;
                                networkTimeUpdaterF = networkTimeUpdater;
                                commonTimeMgmtServiceF = commonTimeMgmtService;
                                inputManagerF = inputManager3;
                                telephonyRegistryF = telephonyRegistry;
                                mediaRouterF = mediaRouter;
                                mmsServiceF = mmsService;
                                ipSecServiceF = ipSecService;
                                wm = wm2;
                                activityManagerService = this.mActivityManagerService;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                            }
                            traceEnd();
                        } else {
                            mediaRouter = statusBar2;
                        }
                        traceBeginAndSlog("StartBackgroundDexOptService");
                        try {
                            BackgroundDexOptService.schedule(context2);
                        } catch (Throwable e2222222222222222222222) {
                            vrDisplayEnable = e2222222222222222222222;
                            reportWtf("starting StartBackgroundDexOptService", e2222222222222222222222);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartPruneInstantAppsJobService");
                        try {
                            PruneInstantAppsJobService.schedule(context2);
                        } catch (Throwable e22222222222222222222222) {
                            vrDisplayEnable = e22222222222222222222222;
                            reportWtf("StartPruneInstantAppsJobService", e22222222222222222222222);
                        }
                        traceEnd();
                        traceBeginAndSlog("StartShortcutServiceLifecycle");
                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                        traceEnd();
                        traceBeginAndSlog("StartLauncherAppsService");
                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                        traceEnd();
                        traceBeginAndSlog("StartCrossProfileAppsService");
                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                        traceEnd();
                        connectivity = connectivity2;
                        countryDetector = countryDetector2;
                        ipSecService = ipSecService2;
                        networkPolicy = networkPolicy2;
                        serialService = serial2;
                        networkTimeUpdater = networkTimeUpdater2;
                        hardwarePropertiesManagerService = hardwarePropertiesService;
                        lockSettings2 = lockSettings;
                        notification = notification3;
                        location = location3;
                        networkManagement = networkManagement2;
                        networkStats = networkStats2;
                        lockSettings5 = serviceDiscovery2;
                    } else {
                        statusBarManagerService = null;
                        location = null;
                        networkTimeUpdater = null;
                        commonTimeMgmtService = null;
                        hardwarePropertiesManagerService = null;
                        countryDetector = null;
                        lockSettings2 = null;
                        mediaRouter = null;
                        networkManagement = null;
                        ipSecService = null;
                        networkStats = null;
                        networkPolicy = null;
                        lockSettings5 = null;
                        serialService = null;
                    }
                    if (isWatch) {
                        traceBeginAndSlog("StartMediaProjectionManager");
                        this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                        traceEnd();
                    }
                    MediaProjectionManagerService.sHasStartedInSystemserver = true;
                    if (isWatch) {
                        traceBeginAndSlog("StartWearConfigService");
                        this.mSystemServiceManager.startService(WEAR_CONFIG_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartWearConnectivityService");
                        this.mSystemServiceManager.startService(WEAR_CONNECTIVITY_SERVICE_CLASS);
                        traceEnd();
                        traceBeginAndSlog("StartWearTimeService");
                        this.mSystemServiceManager.startService(WEAR_DISPLAY_SERVICE_CLASS);
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
                    if (disableSlices) {
                        traceBeginAndSlog("StartSliceManagerService");
                        this.mSystemServiceManager.startService(SLICE_MANAGER_SERVICE_CLASS);
                        traceEnd();
                    }
                    if (disableCameraService) {
                        traceBeginAndSlog("StartCameraServiceProxy");
                        this.mSystemServiceManager.startService(CameraServiceProxy.class);
                        traceEnd();
                    }
                    if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                        traceBeginAndSlog("StartIoTSystemService");
                        this.mSystemServiceManager.startService(IOT_SERVICE_CLASS);
                        traceEnd();
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    safeMode = wm.detectSafeMode();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    if (safeMode) {
                        traceBeginAndSlog("EnterSafeModeAndDisableJitCompilation");
                        this.mActivityManagerService.enterSafeMode();
                        VMRuntime.getRuntime().disableJitCompilation();
                        traceEnd();
                    } else {
                        traceBeginAndSlog("StartJitCompilation");
                        VMRuntime.getRuntime().startJitCompilation();
                        traceEnd();
                    }
                    traceBeginAndSlog("StartMmsService");
                    mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                        traceBeginAndSlog("StartAutoFillService");
                        this.mSystemServiceManager.startService(AUTO_FILL_MANAGER_SERVICE_CLASS);
                        traceEnd();
                    }
                    if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                        try {
                            this.mSystemServiceManager.startService("com.android.server.HwBastetService");
                        } catch (Exception e18) {
                            Slog.w(TAG, "HwBastetService not exists.");
                        }
                    }
                    if (isStartSysSvcCallRecord) {
                        startSysSvcCallRecordService();
                    }
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    telephonyRegistry2.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings2 != null) {
                        try {
                            lockSettings2.systemReady();
                        } catch (Throwable e222222222222222222222222) {
                            th = e222222222222222222222222;
                            reportWtf("making Lock Settings Service ready", e222222222222222222222222);
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
                    wm.systemReady();
                    traceEnd();
                    if (safeMode) {
                        this.mActivityManagerService.showSafeModeOverlay();
                    }
                    config = wm.computeNewConfiguration(0);
                    metrics = new DisplayMetrics();
                    w = (WindowManager) context2.getSystemService("window");
                    w.getDefaultDisplay().getMetrics(metrics);
                    context2.getResources().updateConfiguration(config, metrics);
                    systemTheme = context2.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                        systemTheme.rebase();
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    classes = this.mSystemContext.getResources().getStringArray(17236002);
                    length = classes.length;
                    lockSettings3 = lockSettings2;
                    lockSettings4 = 0;
                    while (lockSettings4 < length) {
                        int i;
                        context = context2;
                        String context3 = classes[lockSettings4];
                        StringBuilder stringBuilder3 = new StringBuilder();
                        wm2 = wm;
                        stringBuilder3.append("StartDeviceSpecificServices ");
                        stringBuilder3.append(context3);
                        traceBeginAndSlog(stringBuilder3.toString());
                        try {
                            this.mSystemServiceManager.startService(context3);
                            i = length;
                        } catch (Throwable e2222222222222222222222222) {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            i = length;
                            stringBuilder4.append("starting ");
                            stringBuilder4.append(context3);
                            reportWtf(stringBuilder4.toString(), e2222222222222222222222222);
                        }
                        traceEnd();
                        lockSettings4++;
                        context2 = context;
                        wm = wm2;
                        length = i;
                    }
                    context = context2;
                    wm2 = wm;
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    networkManagementF = networkManagement;
                    networkStatsF = networkStats;
                    networkPolicyF = networkPolicy;
                    inputManager3 = inputManager2;
                    connectivityF = connectivity;
                    locationF = location;
                    countryDetectorF = countryDetector;
                    networkTimeUpdaterF = networkTimeUpdater;
                    commonTimeMgmtServiceF = commonTimeMgmtService;
                    inputManagerF = inputManager3;
                    telephonyRegistryF = telephonyRegistry;
                    mediaRouterF = mediaRouter;
                    mmsServiceF = mmsService;
                    ipSecServiceF = ipSecService;
                    wm = wm2;
                    activityManagerService = this.mActivityManagerService;
                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                    activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                }
                telephonyRegistry4 = new TelephonyRegistry(context2);
                telephonyRegistry3 = telephonyRegistry4;
                try {
                    ServiceManager.addService("telephony.registry", telephonyRegistry3);
                    traceEnd();
                    traceBeginAndSlog("StartEntropyMixer");
                    this.mEntropyMixer = new EntropyMixer(context2);
                    traceEnd();
                    this.mContentResolver = context2.getContentResolver();
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
                    telephonyRegistry2 = new VibratorService(context2);
                } catch (RuntimeException e19) {
                    e = e19;
                    inputManager = null;
                    telephonyRegistry = telephonyRegistry3;
                    hwCustEmergDataManager = emergDataManager;
                    storageManager = null;
                    connectivity = null;
                    tuiEnable = tuiEnable3;
                    tuiEnable2 = 1;
                    telephonyRegistry2 = binder;
                    almService = alarmManagerService2;
                    Slog.e("System", "******************************************");
                    Slog.e("System", "************ Failure starting core service", e);
                    alarmManagerService = almService;
                    wm = wm3;
                    inputManager2 = inputManager;
                    statusBar = null;
                    lockSettings = null;
                    if (this.mFactoryTestMode != tuiEnable2) {
                    }
                    traceBeginAndSlog("MakeDisplayReady");
                    wm.displayReady();
                    traceEnd();
                    traceBeginAndSlog("StartStorageManagerService");
                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                    storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                    traceEnd();
                    traceBeginAndSlog("StartStorageStatsService");
                    this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                    traceEnd();
                    storageManager = storageManager2;
                    traceBeginAndSlog("StartUiModeManager");
                    this.mSystemServiceManager.startService(UiModeManagerService.class);
                    traceEnd();
                    HwBootCheck.bootSceneEnd(101);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                    notification = null;
                    if (this.mRuntimeRestart) {
                    }
                    HwBootCheck.bootSceneStart(104, 900000);
                    if (this.mOnlyCore) {
                    }
                    traceBeginAndSlog("PerformFstrimIfNeeded");
                    this.mPackageManagerService.performFstrimIfNeeded();
                    traceEnd();
                    HwBootCheck.bootSceneEnd(104);
                    HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                    HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                    if (this.mFactoryTestMode == 1) {
                    }
                    if (isWatch) {
                    }
                    MediaProjectionManagerService.sHasStartedInSystemserver = true;
                    if (isWatch) {
                    }
                    if (disableSlices) {
                    }
                    if (disableCameraService) {
                    }
                    if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                    }
                    traceBeginAndSlog("StartStatsCompanionService");
                    this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                    traceEnd();
                    safeMode = wm.detectSafeMode();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    if (safeMode) {
                    }
                    traceBeginAndSlog("StartMmsService");
                    mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                    traceEnd();
                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                    }
                    if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                    }
                    if (isStartSysSvcCallRecord) {
                    }
                    traceBeginAndSlog("MakeVibratorServiceReady");
                    telephonyRegistry2.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeLockSettingsServiceReady");
                    if (lockSettings2 != null) {
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
                    config = wm.computeNewConfiguration(0);
                    metrics = new DisplayMetrics();
                    w = (WindowManager) context2.getSystemService("window");
                    w.getDefaultDisplay().getMetrics(metrics);
                    context2.getResources().updateConfiguration(config, metrics);
                    systemTheme = context2.getTheme();
                    if (systemTheme.getChangingConfigurations() != 0) {
                    }
                    traceBeginAndSlog("MakePowerManagerServiceReady");
                    this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                    traceEnd();
                    this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                    traceBeginAndSlog("MakePackageManagerServiceReady");
                    this.mPackageManagerService.systemReady();
                    traceEnd();
                    traceBeginAndSlog("MakeDisplayManagerServiceReady");
                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                    traceEnd();
                    this.mSystemServiceManager.setSafeMode(safeMode);
                    traceBeginAndSlog("StartDeviceSpecificServices");
                    classes = this.mSystemContext.getResources().getStringArray(17236002);
                    length = classes.length;
                    lockSettings3 = lockSettings2;
                    lockSettings4 = 0;
                    while (lockSettings4 < length) {
                    }
                    context = context2;
                    wm2 = wm;
                    traceEnd();
                    traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                    this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                    traceEnd();
                    networkManagementF = networkManagement;
                    networkStatsF = networkStats;
                    networkPolicyF = networkPolicy;
                    inputManager3 = inputManager2;
                    connectivityF = connectivity;
                    locationF = location;
                    countryDetectorF = countryDetector;
                    networkTimeUpdaterF = networkTimeUpdater;
                    commonTimeMgmtServiceF = commonTimeMgmtService;
                    inputManagerF = inputManager3;
                    telephonyRegistryF = telephonyRegistry;
                    mediaRouterF = mediaRouter;
                    mmsServiceF = mmsService;
                    ipSecServiceF = ipSecService;
                    wm = wm2;
                    activityManagerService = this.mActivityManagerService;
                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                    -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                    activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
                }
            } catch (RuntimeException e20) {
                e = e20;
                inputManager = null;
                hwCustEmergDataManager = emergDataManager;
                storageManager = null;
                connectivity = null;
                tuiEnable = tuiEnable3;
                tuiEnable2 = 1;
                telephonyRegistry = null;
                telephonyRegistry2 = binder;
                almService = alarmManagerService2;
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting core service", e);
                alarmManagerService = almService;
                wm = wm3;
                inputManager2 = inputManager;
                statusBar = null;
                lockSettings = null;
                if (this.mFactoryTestMode != tuiEnable2) {
                }
                traceBeginAndSlog("MakeDisplayReady");
                wm.displayReady();
                traceEnd();
                traceBeginAndSlog("StartStorageManagerService");
                this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                traceEnd();
                traceBeginAndSlog("StartStorageStatsService");
                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                traceEnd();
                storageManager = storageManager2;
                traceBeginAndSlog("StartUiModeManager");
                this.mSystemServiceManager.startService(UiModeManagerService.class);
                traceEnd();
                HwBootCheck.bootSceneEnd(101);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                notification = null;
                if (this.mRuntimeRestart) {
                }
                HwBootCheck.bootSceneStart(104, 900000);
                if (this.mOnlyCore) {
                }
                traceBeginAndSlog("PerformFstrimIfNeeded");
                this.mPackageManagerService.performFstrimIfNeeded();
                traceEnd();
                HwBootCheck.bootSceneEnd(104);
                HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                if (this.mFactoryTestMode == 1) {
                }
                if (isWatch) {
                }
                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                if (isWatch) {
                }
                if (disableSlices) {
                }
                if (disableCameraService) {
                }
                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                safeMode = wm.detectSafeMode();
                this.mSystemServiceManager.setSafeMode(safeMode);
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                }
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("MakeVibratorServiceReady");
                telephonyRegistry2.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings2 != null) {
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
                config = wm.computeNewConfiguration(0);
                metrics = new DisplayMetrics();
                w = (WindowManager) context2.getSystemService("window");
                w.getDefaultDisplay().getMetrics(metrics);
                context2.getResources().updateConfiguration(config, metrics);
                systemTheme = context2.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                classes = this.mSystemContext.getResources().getStringArray(17236002);
                length = classes.length;
                lockSettings3 = lockSettings2;
                lockSettings4 = 0;
                while (lockSettings4 < length) {
                }
                context = context2;
                wm2 = wm;
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                networkManagementF = networkManagement;
                networkStatsF = networkStats;
                networkPolicyF = networkPolicy;
                inputManager3 = inputManager2;
                connectivityF = connectivity;
                locationF = location;
                countryDetectorF = countryDetector;
                networkTimeUpdaterF = networkTimeUpdater;
                commonTimeMgmtServiceF = commonTimeMgmtService;
                inputManagerF = inputManager3;
                telephonyRegistryF = telephonyRegistry;
                mediaRouterF = mediaRouter;
                mmsServiceF = mmsService;
                ipSecServiceF = ipSecService;
                wm = wm2;
                activityManagerService = this.mActivityManagerService;
                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
            }
            try {
                ServiceManager.addService("vibrator", telephonyRegistry2);
                traceEnd();
                if (isWatch) {
                }
            } catch (RuntimeException e21) {
                e = e21;
                inputManager = null;
                vibratorService = telephonyRegistry2;
                telephonyRegistry = telephonyRegistry3;
                hwCustEmergDataManager = emergDataManager;
                storageManager = null;
                connectivity = null;
                tuiEnable = tuiEnable3;
                tuiEnable2 = 1;
                almService = alarmManagerService2;
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting core service", e);
                alarmManagerService = almService;
                wm = wm3;
                inputManager2 = inputManager;
                statusBar = null;
                lockSettings = null;
                if (this.mFactoryTestMode != tuiEnable2) {
                }
                traceBeginAndSlog("MakeDisplayReady");
                wm.displayReady();
                traceEnd();
                traceBeginAndSlog("StartStorageManagerService");
                this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                traceEnd();
                traceBeginAndSlog("StartStorageStatsService");
                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                traceEnd();
                storageManager = storageManager2;
                traceBeginAndSlog("StartUiModeManager");
                this.mSystemServiceManager.startService(UiModeManagerService.class);
                traceEnd();
                HwBootCheck.bootSceneEnd(101);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                notification = null;
                if (this.mRuntimeRestart) {
                }
                HwBootCheck.bootSceneStart(104, 900000);
                if (this.mOnlyCore) {
                }
                traceBeginAndSlog("PerformFstrimIfNeeded");
                this.mPackageManagerService.performFstrimIfNeeded();
                traceEnd();
                HwBootCheck.bootSceneEnd(104);
                HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                if (this.mFactoryTestMode == 1) {
                }
                if (isWatch) {
                }
                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                if (isWatch) {
                }
                if (disableSlices) {
                }
                if (disableCameraService) {
                }
                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                safeMode = wm.detectSafeMode();
                this.mSystemServiceManager.setSafeMode(safeMode);
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                }
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("MakeVibratorServiceReady");
                telephonyRegistry2.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings2 != null) {
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
                config = wm.computeNewConfiguration(0);
                metrics = new DisplayMetrics();
                w = (WindowManager) context2.getSystemService("window");
                w.getDefaultDisplay().getMetrics(metrics);
                context2.getResources().updateConfiguration(config, metrics);
                systemTheme = context2.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                classes = this.mSystemContext.getResources().getStringArray(17236002);
                length = classes.length;
                lockSettings3 = lockSettings2;
                lockSettings4 = 0;
                while (lockSettings4 < length) {
                }
                context = context2;
                wm2 = wm;
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                networkManagementF = networkManagement;
                networkStatsF = networkStats;
                networkPolicyF = networkPolicy;
                inputManager3 = inputManager2;
                connectivityF = connectivity;
                locationF = location;
                countryDetectorF = countryDetector;
                networkTimeUpdaterF = networkTimeUpdater;
                commonTimeMgmtServiceF = commonTimeMgmtService;
                inputManagerF = inputManager3;
                telephonyRegistryF = telephonyRegistry;
                mediaRouterF = mediaRouter;
                mmsServiceF = mmsService;
                ipSecServiceF = ipSecService;
                wm = wm2;
                activityManagerService = this.mActivityManagerService;
                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
            }
            try {
                traceBeginAndSlog("StartAlarmManagerService");
                almService3 = (AlarmManagerService) this.mSystemServiceManager.startService("com.android.server.HwAlarmManagerService");
                almService = almService3;
                traceEnd();
                this.mActivityManagerService.setAlarmManager(almService);
                traceBeginAndSlog("Init Watchdog");
                watchdog = Watchdog.getInstance();
                watchdog.init(context2, this.mActivityManagerService);
                traceEnd();
                traceBeginAndSlog("StartInputManagerService");
                watchdog2 = watchdog;
                Slog.i(TAG, "Input Manager");
                inputManager4 = HwServiceFactory.getHwInputManagerService().getInstance(context2, null);
                traceEnd();
                traceBeginAndSlog("StartHwSysResManagerService");
                this.mSystemServiceManager.startService("com.android.server.rms.HwSysResManagerService");
                almService2 = almService;
                traceEnd();
                traceBeginAndSlog("StartWindowManagerService");
                ConcurrentUtils.waitForFutureNoInterrupt(this.mSensorServiceStart, START_SENSOR_SERVICE);
                this.mSensorServiceStart = null;
                if (this.mFactoryTestMode == 1) {
                }
                alarmManagerService = almService2;
                vibratorService = telephonyRegistry2;
                connectivity = null;
                tuiEnable = tuiEnable3;
                telephonyRegistry = telephonyRegistry3;
                storageManager = null;
                hwCustEmergDataManager = emergDataManager;
                wm = WindowManagerService.main(context2, inputManager4, this.mFactoryTestMode == 1, this.mFirstBoot ^ 1, this.mOnlyCore, HwPolicyFactory.getHwPhoneWindowManager());
                initRogMode(wm, context2);
                processMultiDPI(wm);
                ServiceManager.addService("window", wm, false, 17);
                inputManagerService = inputManager4;
                tuiEnable2 = 1;
                ServiceManager.addService("input", inputManagerService, false, 1);
                traceEnd();
                traceBeginAndSlog("SetWindowManagerService");
                this.mActivityManagerService.setWindowManager(wm);
                traceEnd();
                traceBeginAndSlog("WindowManagerServiceOnInitReady");
                wm.onInitReady();
                traceEnd();
                SystemServerInitThreadPool.get().submit(-$$Lambda$SystemServer$JQH6ND0PqyyiRiz7lXLvUmRhwRM.INSTANCE, START_HIDL_SERVICES);
                if (isWatch) {
                }
                traceBeginAndSlog("StartInputManager");
                inputManagerService.setWindowManagerCallbacks(wm.getInputMonitor());
                inputManagerService.start();
                traceEnd();
                traceBeginAndSlog("DisplayManagerWindowManagerAndInputReady");
                this.mDisplayManagerService.windowManagerAndInputReady();
                traceEnd();
                if (!isEmulator) {
                }
                traceBeginAndSlog("IpConnectivityMetrics");
                this.mSystemServiceManager.startService(IpConnectivityMetrics.class);
                traceEnd();
                traceBeginAndSlog("NetworkWatchlistService");
                this.mSystemServiceManager.startService(NetworkWatchlistService.Lifecycle.class);
                traceEnd();
                traceBeginAndSlog("PinnerService");
                this.mSystemServiceManager.startService(PinnerService.class);
                traceEnd();
                if (dtvEnable) {
                }
                traceBeginAndSlog("ZrHungService");
                this.mSystemServiceManager.startService("com.android.server.zrhung.ZRHungService");
                traceEnd();
                inputManager2 = inputManagerService;
                telephonyRegistry2 = vibratorService;
            } catch (RuntimeException e23) {
                e = e23;
                vibratorService = telephonyRegistry2;
                telephonyRegistry = telephonyRegistry3;
                hwCustEmergDataManager = emergDataManager;
                storageManager = null;
                connectivity = null;
                tuiEnable = tuiEnable3;
                tuiEnable2 = 1;
                almService = alarmManagerService2;
                Slog.e("System", "******************************************");
                Slog.e("System", "************ Failure starting core service", e);
                alarmManagerService = almService;
                wm = wm3;
                inputManager2 = inputManager;
                statusBar = null;
                lockSettings = null;
                if (this.mFactoryTestMode != tuiEnable2) {
                }
                traceBeginAndSlog("MakeDisplayReady");
                wm.displayReady();
                traceEnd();
                traceBeginAndSlog("StartStorageManagerService");
                this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
                traceEnd();
                traceBeginAndSlog("StartStorageStatsService");
                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                traceEnd();
                storageManager = storageManager2;
                traceBeginAndSlog("StartUiModeManager");
                this.mSystemServiceManager.startService(UiModeManagerService.class);
                traceEnd();
                HwBootCheck.bootSceneEnd(101);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
                notification = null;
                if (this.mRuntimeRestart) {
                }
                HwBootCheck.bootSceneStart(104, 900000);
                if (this.mOnlyCore) {
                }
                traceBeginAndSlog("PerformFstrimIfNeeded");
                this.mPackageManagerService.performFstrimIfNeeded();
                traceEnd();
                HwBootCheck.bootSceneEnd(104);
                HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
                HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
                if (this.mFactoryTestMode == 1) {
                }
                if (isWatch) {
                }
                MediaProjectionManagerService.sHasStartedInSystemserver = true;
                if (isWatch) {
                }
                if (disableSlices) {
                }
                if (disableCameraService) {
                }
                if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                }
                traceBeginAndSlog("StartStatsCompanionService");
                this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
                traceEnd();
                safeMode = wm.detectSafeMode();
                this.mSystemServiceManager.setSafeMode(safeMode);
                if (safeMode) {
                }
                traceBeginAndSlog("StartMmsService");
                mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                traceEnd();
                if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                }
                if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
                }
                if (isStartSysSvcCallRecord) {
                }
                traceBeginAndSlog("MakeVibratorServiceReady");
                telephonyRegistry2.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeLockSettingsServiceReady");
                if (lockSettings2 != null) {
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
                config = wm.computeNewConfiguration(0);
                metrics = new DisplayMetrics();
                w = (WindowManager) context2.getSystemService("window");
                w.getDefaultDisplay().getMetrics(metrics);
                context2.getResources().updateConfiguration(config, metrics);
                systemTheme = context2.getTheme();
                if (systemTheme.getChangingConfigurations() != 0) {
                }
                traceBeginAndSlog("MakePowerManagerServiceReady");
                this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
                traceEnd();
                this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
                traceBeginAndSlog("MakePackageManagerServiceReady");
                this.mPackageManagerService.systemReady();
                traceEnd();
                traceBeginAndSlog("MakeDisplayManagerServiceReady");
                this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                traceEnd();
                this.mSystemServiceManager.setSafeMode(safeMode);
                traceBeginAndSlog("StartDeviceSpecificServices");
                classes = this.mSystemContext.getResources().getStringArray(17236002);
                length = classes.length;
                lockSettings3 = lockSettings2;
                lockSettings4 = 0;
                while (lockSettings4 < length) {
                }
                context = context2;
                wm2 = wm;
                traceEnd();
                traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
                this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                traceEnd();
                networkManagementF = networkManagement;
                networkStatsF = networkStats;
                networkPolicyF = networkPolicy;
                inputManager3 = inputManager2;
                connectivityF = connectivity;
                locationF = location;
                countryDetectorF = countryDetector;
                networkTimeUpdaterF = networkTimeUpdater;
                commonTimeMgmtServiceF = commonTimeMgmtService;
                inputManagerF = inputManager3;
                telephonyRegistryF = telephonyRegistry;
                mediaRouterF = mediaRouter;
                mmsServiceF = mmsService;
                ipSecServiceF = ipSecService;
                wm = wm2;
                activityManagerService = this.mActivityManagerService;
                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
                -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
                activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
            }
        } catch (RuntimeException e24) {
            e = e24;
            inputManager = null;
            hwCustEmergDataManager = emergDataManager;
            storageManager = null;
            connectivity = null;
            tuiEnable = tuiEnable3;
            storageManager2 = z;
            tuiEnable2 = 1;
            telephonyRegistry = null;
            telephonyRegistry2 = binder;
            almService = alarmManagerService2;
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting core service", e);
            alarmManagerService = almService;
            wm = wm3;
            inputManager2 = inputManager;
            statusBar = null;
            lockSettings = null;
            if (this.mFactoryTestMode != tuiEnable2) {
            }
            traceBeginAndSlog("MakeDisplayReady");
            wm.displayReady();
            traceEnd();
            traceBeginAndSlog("StartStorageManagerService");
            this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
            storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
            traceEnd();
            traceBeginAndSlog("StartStorageStatsService");
            this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
            traceEnd();
            storageManager = storageManager2;
            traceBeginAndSlog("StartUiModeManager");
            this.mSystemServiceManager.startService(UiModeManagerService.class);
            traceEnd();
            HwBootCheck.bootSceneEnd(101);
            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
            notification = null;
            if (this.mRuntimeRestart) {
            }
            HwBootCheck.bootSceneStart(104, 900000);
            if (this.mOnlyCore) {
            }
            traceBeginAndSlog("PerformFstrimIfNeeded");
            this.mPackageManagerService.performFstrimIfNeeded();
            traceEnd();
            HwBootCheck.bootSceneEnd(104);
            HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
            HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
            if (this.mFactoryTestMode == 1) {
            }
            if (isWatch) {
            }
            MediaProjectionManagerService.sHasStartedInSystemserver = true;
            if (isWatch) {
            }
            if (disableSlices) {
            }
            if (disableCameraService) {
            }
            if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
            }
            traceBeginAndSlog("StartStatsCompanionService");
            this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
            traceEnd();
            safeMode = wm.detectSafeMode();
            this.mSystemServiceManager.setSafeMode(safeMode);
            if (safeMode) {
            }
            traceBeginAndSlog("StartMmsService");
            mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
            traceEnd();
            if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
            }
            if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
            }
            if (isStartSysSvcCallRecord) {
            }
            traceBeginAndSlog("MakeVibratorServiceReady");
            telephonyRegistry2.systemReady();
            traceEnd();
            traceBeginAndSlog("MakeLockSettingsServiceReady");
            if (lockSettings2 != null) {
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
            config = wm.computeNewConfiguration(0);
            metrics = new DisplayMetrics();
            w = (WindowManager) context2.getSystemService("window");
            w.getDefaultDisplay().getMetrics(metrics);
            context2.getResources().updateConfiguration(config, metrics);
            systemTheme = context2.getTheme();
            if (systemTheme.getChangingConfigurations() != 0) {
            }
            traceBeginAndSlog("MakePowerManagerServiceReady");
            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
            traceEnd();
            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
            traceBeginAndSlog("MakePackageManagerServiceReady");
            this.mPackageManagerService.systemReady();
            traceEnd();
            traceBeginAndSlog("MakeDisplayManagerServiceReady");
            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
            traceEnd();
            this.mSystemServiceManager.setSafeMode(safeMode);
            traceBeginAndSlog("StartDeviceSpecificServices");
            classes = this.mSystemContext.getResources().getStringArray(17236002);
            length = classes.length;
            lockSettings3 = lockSettings2;
            lockSettings4 = 0;
            while (lockSettings4 < length) {
            }
            context = context2;
            wm2 = wm;
            traceEnd();
            traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
            this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
            traceEnd();
            networkManagementF = networkManagement;
            networkStatsF = networkStats;
            networkPolicyF = networkPolicy;
            inputManager3 = inputManager2;
            connectivityF = connectivity;
            locationF = location;
            countryDetectorF = countryDetector;
            networkTimeUpdaterF = networkTimeUpdater;
            commonTimeMgmtServiceF = commonTimeMgmtService;
            inputManagerF = inputManager3;
            telephonyRegistryF = telephonyRegistry;
            mediaRouterF = mediaRouter;
            mmsServiceF = mmsService;
            ipSecServiceF = ipSecService;
            wm = wm2;
            activityManagerService = this.mActivityManagerService;
            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
            -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
            activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
        }
        statusBar = null;
        lockSettings = null;
        if (this.mFactoryTestMode != tuiEnable2) {
        }
        traceBeginAndSlog("MakeDisplayReady");
        try {
            wm.displayReady();
        } catch (Throwable e22222222222222222222222222) {
            Throwable th26 = e22222222222222222222222222;
            reportWtf("making display ready", e22222222222222222222222222);
        }
        traceEnd();
        traceBeginAndSlog("StartStorageManagerService");
        try {
            this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
            storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
        } catch (Throwable e222222222222222222222222222) {
            reportWtf("starting StorageManagerService", e222222222222222222222222222);
            storageManager2 = storageManager;
        }
        traceEnd();
        traceBeginAndSlog("StartStorageStatsService");
        try {
            this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
        } catch (Throwable e2222222222222222222222222222) {
            reportWtf("starting StorageStatsService", e2222222222222222222222222222);
        }
        traceEnd();
        storageManager = storageManager2;
        traceBeginAndSlog("StartUiModeManager");
        this.mSystemServiceManager.startService(UiModeManagerService.class);
        traceEnd();
        HwBootCheck.bootSceneEnd(101);
        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
        notification = null;
        if (this.mRuntimeRestart) {
        }
        HwBootCheck.bootSceneStart(104, 900000);
        if (this.mOnlyCore) {
        }
        traceBeginAndSlog("PerformFstrimIfNeeded");
        try {
            this.mPackageManagerService.performFstrimIfNeeded();
        } catch (Throwable e22222222222222222222222222222) {
            reportWtf("performing fstrim", e22222222222222222222222222222);
        }
        traceEnd();
        HwBootCheck.bootSceneEnd(104);
        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
        if (this.mFactoryTestMode == 1) {
        }
        if (isWatch) {
        }
        MediaProjectionManagerService.sHasStartedInSystemserver = true;
        if (isWatch) {
        }
        if (disableSlices) {
        }
        if (disableCameraService) {
        }
        if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
        }
        traceBeginAndSlog("StartStatsCompanionService");
        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
        traceEnd();
        safeMode = wm.detectSafeMode();
        this.mSystemServiceManager.setSafeMode(safeMode);
        if (safeMode) {
        }
        traceBeginAndSlog("StartMmsService");
        mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
        traceEnd();
        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
        }
        if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
        }
        if (isStartSysSvcCallRecord) {
        }
        traceBeginAndSlog("MakeVibratorServiceReady");
        try {
            telephonyRegistry2.systemReady();
        } catch (Throwable e222222222222222222222222222222) {
            th = e222222222222222222222222222222;
            reportWtf("making Vibrator Service ready", e222222222222222222222222222222);
        }
        traceEnd();
        traceBeginAndSlog("MakeLockSettingsServiceReady");
        if (lockSettings2 != null) {
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
        } catch (Throwable e2222222222222222222222222222222) {
            th = e2222222222222222222222222222222;
            reportWtf("making Window Manager Service ready", e2222222222222222222222222222222);
        }
        traceEnd();
        if (safeMode) {
        }
        config = wm.computeNewConfiguration(0);
        metrics = new DisplayMetrics();
        w = (WindowManager) context2.getSystemService("window");
        w.getDefaultDisplay().getMetrics(metrics);
        context2.getResources().updateConfiguration(config, metrics);
        systemTheme = context2.getTheme();
        if (systemTheme.getChangingConfigurations() != 0) {
        }
        traceBeginAndSlog("MakePowerManagerServiceReady");
        try {
            this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
        } catch (Throwable e22222222222222222222222222222222) {
            reportWtf("making Power Manager Service ready", e22222222222222222222222222222222);
        }
        traceEnd();
        try {
            this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
        } catch (Throwable e222222222222222222222222222222222) {
            reportWtf("making PG Manager Service ready", e222222222222222222222222222222222);
        }
        traceBeginAndSlog("MakePackageManagerServiceReady");
        this.mPackageManagerService.systemReady();
        traceEnd();
        traceBeginAndSlog("MakeDisplayManagerServiceReady");
        try {
            this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
        } catch (Throwable e2222222222222222222222222222222222) {
            reportWtf("making Display Manager Service ready", e2222222222222222222222222222222222);
        }
        traceEnd();
        this.mSystemServiceManager.setSafeMode(safeMode);
        traceBeginAndSlog("StartDeviceSpecificServices");
        classes = this.mSystemContext.getResources().getStringArray(17236002);
        length = classes.length;
        lockSettings3 = lockSettings2;
        lockSettings4 = 0;
        while (lockSettings4 < length) {
        }
        context = context2;
        wm2 = wm;
        traceEnd();
        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
        traceEnd();
        networkManagementF = networkManagement;
        networkStatsF = networkStats;
        networkPolicyF = networkPolicy;
        inputManager3 = inputManager2;
        connectivityF = connectivity;
        locationF = location;
        countryDetectorF = countryDetector;
        networkTimeUpdaterF = networkTimeUpdater;
        commonTimeMgmtServiceF = commonTimeMgmtService;
        inputManagerF = inputManager3;
        telephonyRegistryF = telephonyRegistry;
        mediaRouterF = mediaRouter;
        mmsServiceF = mmsService;
        ipSecServiceF = ipSecService;
        wm = wm2;
        activityManagerService = this.mActivityManagerService;
        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
        activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
        telephonyRegistry = telephonyRegistry3;
        storageManager = null;
        connectivity = null;
        almService = alarmManagerService2;
        tuiEnable = tuiEnable3;
        tuiEnable2 = 1;
        Slog.e("System", "******************************************");
        Slog.e("System", "************ Failure starting core service", e);
        alarmManagerService = almService;
        wm = wm3;
        inputManager2 = inputManager;
        statusBar = null;
        lockSettings = null;
        if (this.mFactoryTestMode != tuiEnable2) {
        }
        traceBeginAndSlog("MakeDisplayReady");
        wm.displayReady();
        traceEnd();
        traceBeginAndSlog("StartStorageManagerService");
        this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
        storageManager2 = Stub.asInterface(ServiceManager.getService("mount"));
        traceEnd();
        traceBeginAndSlog("StartStorageStatsService");
        this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
        traceEnd();
        storageManager = storageManager2;
        traceBeginAndSlog("StartUiModeManager");
        this.mSystemServiceManager.startService(UiModeManagerService.class);
        traceEnd();
        HwBootCheck.bootSceneEnd(101);
        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_START);
        notification = null;
        if (this.mRuntimeRestart) {
        }
        HwBootCheck.bootSceneStart(104, 900000);
        if (this.mOnlyCore) {
        }
        traceBeginAndSlog("PerformFstrimIfNeeded");
        this.mPackageManagerService.performFstrimIfNeeded();
        traceEnd();
        HwBootCheck.bootSceneEnd(104);
        HwBootCheck.bootSceneStart(102, JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
        HwBootFail.setBootStage(HwBootFail.STAGE_APP_DEXOPT_END);
        if (this.mFactoryTestMode == 1) {
        }
        if (isWatch) {
        }
        MediaProjectionManagerService.sHasStartedInSystemserver = true;
        if (isWatch) {
        }
        if (disableSlices) {
        }
        if (disableCameraService) {
        }
        if (context2.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
        }
        traceBeginAndSlog("StartStatsCompanionService");
        this.mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
        traceEnd();
        safeMode = wm.detectSafeMode();
        this.mSystemServiceManager.setSafeMode(safeMode);
        if (safeMode) {
        }
        traceBeginAndSlog("StartMmsService");
        mmsService = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
        traceEnd();
        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
        }
        if ("true".equals(SystemProperties.get("bastet.service.enable", "false"))) {
        }
        if (isStartSysSvcCallRecord) {
        }
        traceBeginAndSlog("MakeVibratorServiceReady");
        telephonyRegistry2.systemReady();
        traceEnd();
        traceBeginAndSlog("MakeLockSettingsServiceReady");
        if (lockSettings2 != null) {
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
        config = wm.computeNewConfiguration(0);
        metrics = new DisplayMetrics();
        w = (WindowManager) context2.getSystemService("window");
        w.getDefaultDisplay().getMetrics(metrics);
        context2.getResources().updateConfiguration(config, metrics);
        systemTheme = context2.getTheme();
        if (systemTheme.getChangingConfigurations() != 0) {
        }
        traceBeginAndSlog("MakePowerManagerServiceReady");
        this.mPowerManagerService.systemReady(this.mActivityManagerService.getAppOpsService());
        traceEnd();
        this.mPGManagerService.systemReady(this.mActivityManagerService, this.mPowerManagerService, location);
        traceBeginAndSlog("MakePackageManagerServiceReady");
        this.mPackageManagerService.systemReady();
        traceEnd();
        traceBeginAndSlog("MakeDisplayManagerServiceReady");
        this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
        traceEnd();
        this.mSystemServiceManager.setSafeMode(safeMode);
        traceBeginAndSlog("StartDeviceSpecificServices");
        classes = this.mSystemContext.getResources().getStringArray(17236002);
        length = classes.length;
        lockSettings3 = lockSettings2;
        lockSettings4 = 0;
        while (lockSettings4 < length) {
        }
        context = context2;
        wm2 = wm;
        traceEnd();
        traceBeginAndSlog("StartBootPhaseDeviceSpecificServicesReady");
        this.mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
        traceEnd();
        networkManagementF = networkManagement;
        networkStatsF = networkStats;
        networkPolicyF = networkPolicy;
        inputManager3 = inputManager2;
        connectivityF = connectivity;
        locationF = location;
        countryDetectorF = countryDetector;
        networkTimeUpdaterF = networkTimeUpdater;
        commonTimeMgmtServiceF = commonTimeMgmtService;
        inputManagerF = inputManager3;
        telephonyRegistryF = telephonyRegistry;
        mediaRouterF = mediaRouter;
        mmsServiceF = mmsService;
        ipSecServiceF = ipSecService;
        wm = wm2;
        activityManagerService = this.mActivityManagerService;
        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe = -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2;
        -__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe2 = new -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(this, context, wm, networkManagementF, networkPolicyF, ipSecServiceF, networkStatsF, connectivityF, locationF, countryDetectorF, networkTimeUpdaterF, commonTimeMgmtServiceF, inputManagerF, telephonyRegistryF, mediaRouterF, mmsServiceF, enableIaware);
        activityManagerService.systemReady(-__lambda_systemserver__k2zklzcdixiyhahbqhiid56pxe, BOOT_TIMINGS_TRACE_LOG);
    }

    static /* synthetic */ void lambda$startOtherServices$1() {
        try {
            Slog.i(TAG, "SecondaryZygotePreload");
            TimingsTraceLog traceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288);
            traceLog.traceBegin("SecondaryZygotePreload");
            if (!Process.zygoteProcess.preloadDefault(Build.SUPPORTED_32_BIT_ABIS[0])) {
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

    /* JADX WARNING: Removed duplicated region for block: B:123:0x0228  */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0228  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$startOtherServices$5(SystemServer systemServer, Context context, WindowManagerService windowManagerF, NetworkManagementService networkManagementF, NetworkPolicyManagerService networkPolicyF, IpSecService ipSecServiceF, NetworkStatsService networkStatsF, ConnectivityService connectivityF, LocationManagerService locationF, CountryDetectorService countryDetectorF, NetworkTimeUpdateService networkTimeUpdaterF, CommonTimeManagementService commonTimeMgmtServiceF, InputManagerService inputManagerF, TelephonyRegistry telephonyRegistryF, MediaRouterService mediaRouterF, MmsServiceBroker mmsServiceF, boolean enableIaware) {
        Throwable e;
        Throwable th;
        Throwable th2;
        SystemServer systemServer2 = systemServer;
        Context context2 = context;
        NetworkPolicyManagerService networkPolicyManagerService = networkPolicyF;
        Slog.i(TAG, "Making services ready");
        traceBeginAndSlog("StartActivityManagerReadyPhase");
        systemServer2.mSystemServiceManager.startBootPhase(550);
        traceEnd();
        traceBeginAndSlog("StartObservingNativeCrashes");
        try {
            systemServer2.mActivityManagerService.startObservingNativeCrashes();
        } catch (Throwable e2) {
            systemServer2.reportWtf("observing native crashes", e2);
        }
        traceEnd();
        String WEBVIEW_PREPARATION = "WebViewFactoryPreparation";
        Future<?> webviewPrep = null;
        if (!(systemServer2.mOnlyCore || systemServer2.mWebViewUpdateService == null)) {
            webviewPrep = SystemServerInitThreadPool.get().submit(new -$$Lambda$SystemServer$72PvntN28skIthlRYR9w5EhsdX8(systemServer2), "WebViewFactoryPreparation");
        }
        Future<?> webviewPrep2 = webviewPrep;
        if (systemServer2.mPackageManager.hasSystemFeature("android.hardware.type.automotive")) {
            traceBeginAndSlog("StartCarServiceHelperService");
            systemServer2.mSystemServiceManager.startService(CAR_SERVICE_HELPER_SERVICE_CLASS);
            traceEnd();
        }
        traceBeginAndSlog("StartSystemUI");
        try {
            startSystemUi(context, windowManagerF);
        } catch (Throwable e22) {
            Throwable th3 = e22;
            systemServer2.reportWtf("starting System UI", e22);
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkManagementServiceReady");
        if (networkManagementF != null) {
            try {
                networkManagementF.systemReady();
            } catch (Throwable e222) {
                Throwable th4 = e222;
                systemServer2.reportWtf("making Network Managment Service ready", e222);
            }
        }
        CountDownLatch networkPolicyInitReadySignal = null;
        if (networkPolicyManagerService != null) {
            networkPolicyInitReadySignal = networkPolicyF.networkScoreAndNetworkManagementServiceReady();
        }
        CountDownLatch networkPolicyInitReadySignal2 = networkPolicyInitReadySignal;
        traceEnd();
        traceBeginAndSlog("MakeIpSecServiceReady");
        if (ipSecServiceF != null) {
            try {
                ipSecServiceF.systemReady();
            } catch (Throwable e2222) {
                Throwable th5 = e2222;
                systemServer2.reportWtf("making IpSec Service ready", e2222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkStatsServiceReady");
        if (networkStatsF != null) {
            try {
                networkStatsF.systemReady();
            } catch (Throwable e22222) {
                Throwable th6 = e22222;
                systemServer2.reportWtf("making Network Stats Service ready", e22222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeConnectivityServiceReady");
        if (connectivityF != null) {
            try {
                connectivityF.systemReady();
            } catch (Throwable e222222) {
                th = e222222;
                systemServer2.reportWtf("making Connectivity Service ready", e222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkPolicyServiceReady");
        if (networkPolicyManagerService != null) {
            try {
                networkPolicyManagerService.systemReady(networkPolicyInitReadySignal2);
            } catch (Throwable e2222222) {
                th = e2222222;
                systemServer2.reportWtf("making Network Policy Service ready", e2222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("StartWatchdog");
        Watchdog.getInstance().start();
        traceEnd();
        systemServer2.mPackageManagerService.waitForAppDataPrepared();
        traceBeginAndSlog("PhaseThirdPartyAppsCanStart");
        if (webviewPrep2 != null) {
            ConcurrentUtils.waitForFutureNoInterrupt(webviewPrep2, "WebViewFactoryPreparation");
        }
        systemServer2.mSystemServiceManager.startBootPhase(600);
        traceEnd();
        traceBeginAndSlog("MakeLocationServiceReady");
        if (locationF != null) {
            try {
                locationF.systemRunning();
            } catch (Throwable e22222222) {
                Throwable th7 = e22222222;
                systemServer2.reportWtf("Notifying Location Service running", e22222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeCountryDetectionServiceReady");
        if (countryDetectorF != null) {
            try {
                countryDetectorF.systemRunning();
            } catch (Throwable e222222222) {
                Throwable th8 = e222222222;
                systemServer2.reportWtf("Notifying CountryDetectorService running", e222222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeNetworkTimeUpdateReady");
        if (networkTimeUpdaterF != null) {
            try {
                networkTimeUpdaterF.systemRunning();
            } catch (Throwable e2222222222) {
                Throwable th9 = e2222222222;
                systemServer2.reportWtf("Notifying NetworkTimeService running", e2222222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeCommonTimeManagementServiceReady");
        if (commonTimeMgmtServiceF != null) {
            try {
                commonTimeMgmtServiceF.systemRunning();
            } catch (Throwable e22222222222) {
                Throwable th10 = e22222222222;
                systemServer2.reportWtf("Notifying CommonTimeManagementService running", e22222222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeInputManagerServiceReady");
        if (inputManagerF != null) {
            try {
                inputManagerF.systemRunning();
            } catch (Throwable e222222222222) {
                th2 = e222222222222;
                systemServer2.reportWtf("Notifying InputManagerService running", e222222222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeTelephonyRegistryReady");
        if (telephonyRegistryF != null) {
            try {
                telephonyRegistryF.systemRunning();
            } catch (Throwable e2222222222222) {
                th2 = e2222222222222;
                systemServer2.reportWtf("Notifying TelephonyRegistry running", e2222222222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeMediaRouterServiceReady");
        if (mediaRouterF != null) {
            try {
                mediaRouterF.systemRunning();
            } catch (Throwable e22222222222222) {
                th2 = e22222222222222;
                systemServer2.reportWtf("Notifying MediaRouterService running", e22222222222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("MakeMmsServiceReady");
        if (mmsServiceF != null) {
            try {
                mmsServiceF.systemRunning();
            } catch (Throwable e222222222222222) {
                th2 = e222222222222222;
                systemServer2.reportWtf("Notifying MmsService running", e222222222222222);
            }
        }
        traceEnd();
        traceBeginAndSlog("IncidentDaemonReady");
        try {
            IIncidentManager incident = IIncidentManager.Stub.asInterface(ServiceManager.getService("incident"));
            if (incident != null) {
                incident.systemRunning();
            }
        } catch (Throwable e2222222222222222) {
            systemServer2.reportWtf("Notifying incident daemon running", e2222222222222222);
        }
        traceEnd();
        if (enableIaware) {
            try {
                ServiceManager.addService("multi_task", HwServiceFactory.getMultiTaskManagerService().getInstance(context2));
            } catch (Throwable th11) {
                e2222222222222222 = th11;
                systemServer2.reportWtf("starting MultiTaskManagerService", e2222222222222222);
                if (HwPCUtils.enabled()) {
                }
                HwServiceFactory.addHwFmService(context);
                HwServiceFactory.updateLocalesWhenOTAEX(context2, systemServer2.mPackageManagerService.getSdkVersion());
            }
        }
        Slog.e(TAG, "can not start multitask because the prop is false");
        if (HwPCUtils.enabled()) {
            traceBeginAndSlog("StartPCManagerService");
            HwServiceFactory.addHwPCManagerService(context2, systemServer2.mActivityManagerService);
            traceEnd();
        }
        HwServiceFactory.addHwFmService(context);
        HwServiceFactory.updateLocalesWhenOTAEX(context2, systemServer2.mPackageManagerService.getSdkVersion());
    }

    public static /* synthetic */ void lambda$startOtherServices$4(SystemServer systemServer) {
        Slog.i(TAG, "WebViewFactoryPreparation");
        TimingsTraceLog traceLog = new TimingsTraceLog(SYSTEM_SERVER_TIMING_ASYNC_TAG, 524288);
        traceLog.traceBegin("WebViewFactoryPreparation");
        ConcurrentUtils.waitForFutureNoInterrupt(systemServer.mZygotePreload, "Zygote preload");
        systemServer.mZygotePreload = null;
        systemServer.mWebViewUpdateService.prepareWebViewInSystemServer();
        traceLog.traceEnd();
    }

    static final void startSystemUi(Context context, WindowManagerService windowManager) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService"));
        intent.addFlags(256);
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
        windowManager.onSystemUiStarted();
    }

    private static void traceBeginAndSlog(String name) {
        Slog.i(TAG, name);
        BOOT_TIMINGS_TRACE_LOG.traceBegin(name);
    }

    private static void traceEnd() {
        BOOT_TIMINGS_TRACE_LOG.traceEnd();
    }

    private void processMultiDPI(WindowManagerService wm) {
        int dpi = SystemProperties.getInt("persist.sys.dpi", 0);
        if (SystemProperties.getInt("persist.sys.rog.width", 0) > 0) {
            dpi = SystemProperties.getInt("persist.sys.realdpi", SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0))));
        }
        if (dpi > 0) {
            wm.setForcedDisplayDensityForUser(0, dpi, UserHandle.myUserId());
        }
    }

    private void initRogMode(WindowManagerService wm, Context context) {
        if (!(wm == null || SystemProperties.get("ro.runmode", "normal").equals("factory") || (SystemProperties.getInt("persist.sys.aps.firstboot", 1) <= 0 && SystemProperties.getInt("persist.sys.rog.width", 0) != 0))) {
            int initWidth = SystemProperties.getInt("sys.rog.width", 0);
            int initHeight = SystemProperties.getInt("sys.rog.height", 0);
            int initDensity = SystemProperties.getInt("sys.rog.density", 0);
            if (!(initWidth == 0 || initHeight == 0 || initDensity == 0)) {
                int density = getRealDpiBasedRog(initDensity);
                SystemProperties.set("persist.sys.realdpi", Integer.toString(density));
                SystemProperties.set("persist.sys.rog.width", Integer.toString(initWidth));
                SystemProperties.set("persist.sys.rog.height", Integer.toString(initHeight));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(initWidth);
                stringBuilder.append(",");
                stringBuilder.append(initHeight);
                Global.putString(context.getContentResolver(), "display_size_forced", stringBuilder.toString());
                wm.setForcedDisplaySize(0, initWidth, initHeight);
                wm.setForcedDisplayDensityForUser(0, density, UserHandle.myUserId());
                SystemProperties.set("persist.sys.rog.configmode", ENCRYPTED_STATE);
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("initRogMode and setForcedDisplaySize, initWidth = ");
                stringBuilder2.append(initWidth);
                stringBuilder2.append("; initHeight = ");
                stringBuilder2.append(initHeight);
                stringBuilder2.append("; density = ");
                stringBuilder2.append(density);
                Slog.d(str, stringBuilder2.toString());
                Global.putInt(context.getContentResolver(), "aps_display_resolution", 2);
                Global.putInt(context.getContentResolver(), "low_resolution_switch", 1);
            }
        }
    }

    private int getRealDpiBasedRog(int rogDpi) {
        int originLcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0));
        return (SystemProperties.getInt("persist.sys.dpi", originLcdDpi) * rogDpi) / originLcdDpi;
    }

    private void restoreRogMode(WindowManagerService wm, Context context) {
        if (wm != null) {
            if (SystemProperties.getInt("persist.sys.rog.configmode", 0) == 1) {
                SystemProperties.set("persist.sys.realdpi", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                SystemProperties.set("persist.sys.rog.width", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                SystemProperties.set("persist.sys.rog.height", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                Global.putString(context.getContentResolver(), "display_size_forced", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                wm.setForcedDisplayDensityForUser(0, SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)), UserHandle.myUserId());
                SystemProperties.set("persist.sys.rog.configmode", "0");
            } else if (SystemProperties.getInt("persist.sys.rog.width", 0) != 0) {
                SystemProperties.set("persist.sys.rog.width", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                SystemProperties.set("persist.sys.rog.height", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
        }
    }

    private void startForceRotation(Context context) {
        if (HwFrameworkFactory.getForceRotationManager().isForceRotationSupported()) {
            try {
                Slog.i(TAG, "Force rotation Service, name = forceRotationService");
                IHwForceRotationManagerServiceWrapper ifrsw = HwServiceFactory.getForceRotationManagerServiceWrapper();
                if (ifrsw != null) {
                    ServiceManager.addService("forceRotationService", ifrsw.getServiceInstance(context, UiThread.getHandler()));
                }
            } catch (Throwable e) {
                reportWtf("starting Force rotation service", e);
            }
        }
    }
}
