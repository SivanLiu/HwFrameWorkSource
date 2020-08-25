package com.android.server.location;

import android.content.Context;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationRequest;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.ArraySet;
import com.android.server.HwServiceFactory;
import com.android.server.LocationManagerService;
import com.android.server.LocationManagerServiceUtil;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.huawei.utils.reflect.EasyInvokeFactory;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import libcore.io.IoUtils;

public class HwQuickTTFFMonitor implements IHwQuickTTFFMonitor {
    private static final float ACC_IN_WHITE_LIST = 50.0f;
    private static final long BLACKLIST_READ_INTERVAL = 600000;
    private static final int CN0S = 30;
    private static final boolean DEBUG = true;
    private static final int DEFAULT_SIZE = 16;
    private static final int FIRST_SV = 0;
    private static final boolean IS_DEBUG_VERSION;
    public static final int LIST_TYPE_DISABLE = 4;
    public static final int LIST_TYPE_WHITE = 3;
    private static final int LOCATION_HAS_HWQUICKFIX = 256;
    private static final int METER_PER_SECOND = -700;
    private static final int QUICK_ENABLE_ONLY = 2;
    private static final float QUICK_FAKE_CARRIER_FREQUENCY = 1.56109798E9f;
    private static final String QUICK_GPS = "quickgps";
    private static final int QUICK_GPS_LOCATION = 2;
    private static final int QUICK_GPS_REQUEST_START = 1;
    private static final int QUICK_GPS_REQUEST_STOP = 3;
    private static final int QUICK_GPS_SESSION_START = 0;
    private static final int QUICK_GPS_SESSION_STOP = 4;
    private static final int QUICK_IS_ENABLE = 1;
    private static final int QUICK_LOCATION_START = 1;
    private static final int QUICK_LOCATION_STOP = 0;
    private static final int QUICK_REQUEST_START = 1;
    private static final int QUICK_REQUEST_STOP = 0;
    private static final String QUICK_START = "request_quick_ttff";
    private static final String QUICK_STOP = "stop_quick_ttff";
    private static final int QUICK_TTFF = 128;
    private static final String REQUEST_QUICK_TTFF = "request_quick_ttff";
    private static final int STATE_QTTFF = 17;
    private static final String STOP_QUICK_TTFF = "stop_quick_ttff";
    private static final int STOP_TIME = 50;
    private static final int SVID = 24;
    private static final int SVID_WITH_FLAGS = 24;
    private static final int SV_FAKE_DATA = 1;
    private static final int SV_FAKE_DATA_ZERO = 0;
    private static final String TAG = "HwQuickTTFFMonitor-V2018.8.27";
    private static final String VERSION = "V2018.8.27";
    private static final int WAIT_TIME = 30000;
    private static GnssClock mGnssClock;
    private static GnssMeasurement mGnssMeasurement;
    private static GnssMeasurementsEvent mGnssMeasurementsEvent;
    private static HwQuickTTFFMonitor mHwQuickTTFFMonitorManager;
    private static final Object mLock = new Object();
    private long disableListUpdateTimetamp = 0;
    private final Object listLock = new Object();
    private Looper looper;
    private ArraySet<String> mAccWhiteList = new ArraySet<>();
    private HashMap<String, String> mAppMonitorMap;
    private Context mContext;
    private String mDebugPropertiesFile;
    private String mEnableQuickttff;
    private GnssLocationProvider mGnssProvider;
    private Handler mHandler;
    private HwLbsConfigManager mHwLbsConfigManager;
    private IHwGpsLogServices mHwLocationGpsLogServices;
    private boolean mIsNavigating = false;
    private boolean mIsPermission = true;
    private boolean mIsQuickLocation = false;
    private boolean mIsQuickTTFFEnable = false;
    /* access modifiers changed from: private */
    public boolean mIsReceive = false;
    private volatile boolean mIsSatisfiedRequest = false;
    private boolean mIsSendStartCommand = false;
    private Properties mProperties;
    private Timer mQuickTTFFtimer;
    private ArraySet<String> mQuickttffDisableList = new ArraySet<>();
    private int mQuickttffEnableState;
    private ConcurrentHashMap<String, Long> mRemoveTimeMap;
    /* access modifiers changed from: private */
    public boolean mRunning = false;
    private GpsLocationProviderUtils mUtils;
    private List<String> powerQuickttffDisableLists = new ArrayList(16);

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) != 3) {
            z = false;
        }
        IS_DEBUG_VERSION = z;
    }

    private HwQuickTTFFMonitor(Context context, GnssLocationProvider gnssLocationProvider) {
        this.mContext = context;
        this.mGnssProvider = gnssLocationProvider;
        this.mHwLocationGpsLogServices = HwServiceFactory.getHwGpsLogServices(this.mContext);
        this.mHwLbsConfigManager = HwLbsConfigManager.getInstance(context);
        startMonitor();
        this.mUtils = EasyInvokeFactory.getInvokeUtils(GpsLocationProviderUtils.class);
        GpsLocationProviderUtils gpsLocationProviderUtils = this.mUtils;
        if (gpsLocationProviderUtils != null) {
            this.looper = gpsLocationProviderUtils.getGnssLooper(this.mGnssProvider);
        }
        Looper looper2 = this.looper;
        if (looper2 != null) {
            this.mHandler = new QuickHandler(looper2);
        }
    }

    public static HwQuickTTFFMonitor getInstance(Context context, GnssLocationProvider provider) {
        synchronized (mLock) {
            if (mHwQuickTTFFMonitorManager == null) {
                LBSLog.i(TAG, false, "mHwQuickTTFFMonitorManager create.", new Object[0]);
                mHwQuickTTFFMonitorManager = new HwQuickTTFFMonitor(context, provider);
            }
        }
        return mHwQuickTTFFMonitorManager;
    }

    public static HwQuickTTFFMonitor getMonitor() {
        return mHwQuickTTFFMonitorManager;
    }

    private void startMonitor() {
        quickTTFFEnable(configPath());
        if (!this.mIsQuickTTFFEnable) {
            LBSLog.e(TAG, false, " QuickTTFF is  not enable %{public}s", this.mEnableQuickttff);
            return;
        }
        LBSLog.i(TAG, false, "startMonitor", new Object[0]);
        this.mAppMonitorMap = new HashMap<>();
        this.mRemoveTimeMap = new ConcurrentHashMap<>();
        this.mRunning = false;
        this.mIsSendStartCommand = false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00e5, code lost:
        r17.mIsSatisfiedRequest = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00e9, code lost:
        if (r17.mRunning != false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00ed, code lost:
        if (r17.mIsNavigating == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00ef, code lost:
        com.android.server.location.LBSLog.i(com.android.server.location.HwQuickTTFFMonitor.TAG, false, "start HwQuickTTFF  when isNavigating", new java.lang.Object[0]);
        sendStartCommand();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:?, code lost:
        return;
     */
    public void requestHwQuickTTFF(LocationRequest request, String packageName, String requestProvider, String id) {
        String requestAccuracy;
        if (this.mIsQuickTTFFEnable) {
            if (!"gps".equals(request.getProvider())) {
                return;
            }
            if (!"gps".equals(requestProvider)) {
                return;
            }
            if (this.mIsPermission) {
                boolean isQuickttffDisable = !this.mQuickttffDisableList.contains(packageName);
                if (this.mAccWhiteList.contains(packageName)) {
                    requestAccuracy = String.valueOf((float) ACC_IN_WHITE_LIST);
                } else {
                    requestAccuracy = "null";
                }
                this.mHwLocationGpsLogServices.setQuickGpsParam(1, packageName + "," + isQuickttffDisable + "," + requestAccuracy);
                if (isQuickttffDisableListApp(packageName)) {
                    LBSLog.i(TAG, false, "%{public}s is in black list can not start Quickttff", packageName);
                    return;
                }
                if (this.mRemoveTimeMap.size() > 0) {
                    for (Map.Entry<String, Long> entry : this.mRemoveTimeMap.entrySet()) {
                        String packageNameRemove = entry.getKey();
                        if (System.currentTimeMillis() - entry.getValue().longValue() > 2000) {
                            this.mRemoveTimeMap.remove(packageNameRemove);
                        }
                    }
                    if (this.mRemoveTimeMap.containsKey(packageName)) {
                        LBSLog.i(TAG, false, "%{public}s can not start quickttff in 2s", packageName);
                        return;
                    }
                }
                synchronized (mLock) {
                    try {
                        try {
                            this.mAppMonitorMap.put(id, packageName);
                            LBSLog.i(TAG, false, "requestHwQuickTTFF:%{public}s", packageName);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            }
        }
    }

    public void removeHwQuickTTFF(String packageName, String id, boolean isGps) {
        if (this.mIsQuickTTFFEnable) {
            if (isGps) {
                this.mRemoveTimeMap.put(packageName, Long.valueOf(System.currentTimeMillis()));
            }
            synchronized (mLock) {
                if (this.mAppMonitorMap.containsKey(id)) {
                    this.mAppMonitorMap.remove(id);
                    this.mHwLocationGpsLogServices.setQuickGpsParam(3, packageName);
                    LBSLog.i(TAG, false, "removeHwQuickTTFF:%{public}s", packageName);
                    if (this.mAppMonitorMap.size() <= 0) {
                        if (this.mRunning) {
                            LBSLog.i(TAG, false, "removeHwQuickTTFF HwQuickTTFF STOP", new Object[0]);
                            sendStopCommand();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0021, code lost:
        com.android.server.location.LBSLog.i(com.android.server.location.HwQuickTTFFMonitor.TAG, false, "removeAllHwQuickTTFF HwQuickTTFF STOP", new java.lang.Object[0]);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0031, code lost:
        if (r6.mAppMonitorMap.size() <= 0) goto L_0x005d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0033, code lost:
        r0 = r6.mAppMonitorMap.entrySet().iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0041, code lost:
        if (r0.hasNext() == false) goto L_0x005d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0043, code lost:
        r6.mRemoveTimeMap.put(r0.next().getValue(), java.lang.Long.valueOf(java.lang.System.currentTimeMillis()));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x005d, code lost:
        r6.mAppMonitorMap.clear();
        sendStopCommand();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0065, code lost:
        return;
     */
    private void removeAllHwQuickTTFF(String provider) {
        if (this.mIsQuickTTFFEnable && !this.mIsQuickLocation && "gps".equals(provider)) {
            synchronized (mLock) {
                if (this.mRunning) {
                    this.mRunning = false;
                    this.mIsSendStartCommand = false;
                }
            }
        }
    }

    public boolean isLocationReportToApp(String packageName, String provider, Location location) {
        boolean isReportLocation = false;
        float acc = location.getAccuracy();
        if (!this.mIsQuickLocation || !"gps".equals(provider)) {
            return true;
        }
        if (this.mAppMonitorMap.containsValue(packageName)) {
            if (isAccSatisfied(packageName, acc)) {
                isReportLocation = true;
            } else {
                isReportLocation = false;
            }
        }
        LBSLog.i(TAG, false, "isReportQuickTTFFLocation:%{public}b, pkgname=%{public}s", Boolean.valueOf(isReportLocation), packageName);
        return isReportLocation;
    }

    private void setQuickTTFFLocation(Location location, boolean passive) {
        if (this.mIsQuickTTFFEnable && !passive) {
            if ((QUICK_GPS.equals(location.getProvider()) || "gps".equals(location.getProvider())) && this.mRunning) {
                this.mHwLocationGpsLogServices.setQuickGpsParam(2, String.valueOf(location.getAccuracy()));
            }
            if (QUICK_GPS.equals(location.getProvider())) {
                this.mIsQuickLocation = true;
                location.setProvider("gps");
                Bundle extras = location.getExtras();
                if (extras == null) {
                    extras = new Bundle();
                    location.setExtras(extras);
                }
                LBSLog.i(TAG, false, "SourceType %{public}d", Integer.valueOf(location.getExtras().getInt("SourceType")));
                extras.putBoolean("QUICKGPS", true);
                location.setExtras(extras);
                LBSLog.i(TAG, false, "is a QuickTTFFLocation", new Object[0]);
                if (!this.mIsReceive) {
                    this.mIsReceive = true;
                    LBSLog.i(TAG, false, "have receive QuickTTFFLocation", new Object[0]);
                    cancelLocalTimerTask();
                }
            } else if ("gps".equals(location.getProvider())) {
                this.mIsQuickLocation = false;
            }
        }
    }

    public boolean isQuickLocation(Location location) {
        int sourceType = 0;
        if (location.getExtras() != null) {
            sourceType = location.getExtras().getInt("SourceType");
        }
        if ((sourceType & 128) == 128) {
            return true;
        }
        return false;
    }

    private boolean isRunning() {
        return this.mRunning;
    }

    public void setPermission(boolean permission) {
        this.mIsPermission = permission;
    }

    private void setNavigating(boolean Navigating) {
        this.mIsNavigating = Navigating;
    }

    private boolean isQuickttffDisableListApp(String appName) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.disableListUpdateTimetamp > 600000) {
            this.disableListUpdateTimetamp = currentTime;
            updateBlackList(new ArrayList(16));
        }
        if (this.mQuickttffDisableList.contains(appName)) {
            LBSLog.i(TAG, false, "%{public}s is not WhiteApp", appName);
            return true;
        }
        LBSLog.i(TAG, false, "%{public}s is WhiteApp", appName);
        return false;
    }

    private void updateBlackList(List<String> whiteList) {
        synchronized (this.listLock) {
            this.mQuickttffDisableList.clear();
            this.mQuickttffDisableList.addAll(this.mHwLbsConfigManager.getListForFeature(LbsConfigContent.CONFIG_QTTFF_DISABLE_BLACKLIST));
            if (whiteList != null && whiteList.size() > 0) {
                this.powerQuickttffDisableLists.clear();
                this.powerQuickttffDisableLists.addAll(whiteList);
            }
            for (String packageName : this.powerQuickttffDisableLists) {
                if (!this.mQuickttffDisableList.contains(packageName)) {
                    this.mQuickttffDisableList.add(packageName);
                }
            }
            LBSLog.i(TAG, false, "updateDisableList quickttffDisableList %{public}s", this.powerQuickttffDisableLists);
        }
    }

    private boolean isAccSatisfied(String appName, float acc) {
        if (!this.mAccWhiteList.contains(appName)) {
            return true;
        }
        LBSLog.i(TAG, false, "%{public}s is inAccList ", appName);
        if (ACC_IN_WHITE_LIST >= acc) {
            LBSLog.i(TAG, false, "Satisfied with Acc %{public}d >= %{public}d ", Float.valueOf((float) ACC_IN_WHITE_LIST), Float.valueOf(acc));
            return true;
        }
        LBSLog.i(TAG, false, " Not Satisfied with Acc %{public}d < %{public}d", Float.valueOf((float) ACC_IN_WHITE_LIST), Float.valueOf(acc));
        return false;
    }

    public void updateWhiteList(int type, List<String> whiteList) {
        if (type == 3) {
            this.mAccWhiteList.clear();
            this.mAccWhiteList.addAll(whiteList);
            LBSLog.i(TAG, false, "updateAccWhiteList accWhiteList %{public}s", whiteList);
        } else if (type == 4) {
            updateBlackList(whiteList);
        }
    }

    private void quickTTFFEnable(String filename) {
        boolean isLocalQuickTtffEnable;
        this.mProperties = new Properties();
        try {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(new File(filename));
                this.mProperties.load(stream);
            } finally {
                IoUtils.closeQuietly(stream);
            }
        } catch (IOException e) {
            LBSLog.i(TAG, false, "Could not open higeo configuration file %{private}s", filename);
        }
        this.mEnableQuickttff = this.mProperties.getProperty("higeo_enable_quickttff");
        String str = this.mEnableQuickttff;
        if (str != null && !"".equals(str)) {
            try {
                this.mQuickttffEnableState = Integer.parseInt(this.mEnableQuickttff);
            } catch (NumberFormatException e2) {
                LBSLog.e(TAG, false, "unable to parse Enable_Quickttff: %{public}s", this.mEnableQuickttff);
            }
        }
        int i = this.mQuickttffEnableState;
        if (i == 1) {
            isLocalQuickTtffEnable = true;
            LBSLog.e(TAG, false, " QuickTTFF is enable %{public}s", this.mEnableQuickttff);
        } else if (i == 2) {
            isLocalQuickTtffEnable = true;
            LBSLog.e(TAG, false, " QuickTTFF only is enable %{public}s", this.mEnableQuickttff);
        } else {
            isLocalQuickTtffEnable = false;
            LBSLog.e(TAG, false, " QuickTTFF is  not enable %{public}s", this.mEnableQuickttff);
        }
        if (this.mHwLbsConfigManager.isParamAlreadySetup(LbsConfigContent.CONFIG_QTTFF_ENABLE)) {
            this.mIsQuickTTFFEnable = this.mHwLbsConfigManager.isEnableForParam(LbsConfigContent.CONFIG_QTTFF_ENABLE);
        } else {
            this.mIsQuickTTFFEnable = isLocalQuickTtffEnable;
        }
    }

    private boolean isBetaUser() {
        return IS_DEBUG_VERSION;
    }

    private String configPath() {
        if (isBetaUser()) {
            LBSLog.i(TAG, false, "This is beta user", new Object[0]);
            File file = HwCfgFilePolicy.getCfgFile("xml/higeo_beta.conf", 0);
            if (file != null) {
                this.mDebugPropertiesFile = file.getPath();
                LBSLog.i(TAG, false, "configPath is %{private}s", this.mDebugPropertiesFile);
            } else {
                LBSLog.i(TAG, false, "configPath is not /cust_spec/xml/  ", new Object[0]);
                this.mDebugPropertiesFile = "/odm/etc/higeo_beta.conf";
            }
        } else {
            LBSLog.i(TAG, false, "This is not beta user", new Object[0]);
            File file2 = HwCfgFilePolicy.getCfgFile("xml/higeo.conf", 0);
            if (file2 != null) {
                this.mDebugPropertiesFile = file2.getPath();
                LBSLog.i(TAG, false, "configPath is %{private}s", this.mDebugPropertiesFile);
            } else {
                LBSLog.i(TAG, false, "configPath is not /cust_spec/xml/  ", new Object[0]);
                this.mDebugPropertiesFile = "/odm/etc/higeo.conf";
            }
        }
        LBSLog.i(TAG, false, "configPath is %{private}s", this.mDebugPropertiesFile);
        return this.mDebugPropertiesFile;
    }

    private boolean isReport(Location location) {
        if (this.mRunning || !QUICK_GPS.equals(location.getProvider())) {
            return true;
        }
        return false;
    }

    private void clearAppMonitorMap() {
        this.mAppMonitorMap.clear();
        LBSLog.i(TAG, false, "clearAppMonitorMap", new Object[0]);
    }

    private void sendStartCommand() {
        if (this.mRunning) {
            LBSLog.i(TAG, false, "HwQuickTTFF has be started before.", new Object[0]);
        } else if (!this.mIsSendStartCommand && this.mIsSatisfiedRequest) {
            this.mIsReceive = false;
            timerTask();
            this.mRunning = true;
            this.mIsSendStartCommand = true;
            LBSLog.i(TAG, false, "HwQuickTTFF Start", new Object[0]);
            this.mHwLocationGpsLogServices.setQuickGpsParam(0, "request_quick_ttff");
            sendMessage(1);
        }
    }

    private void sendMessage(int message) {
        Handler handler = this.mHandler;
        if (handler == null) {
            LBSLog.i(TAG, false, "Handler is null", new Object[0]);
        } else {
            handler.obtainMessage(message).sendToTarget();
        }
    }

    private final class QuickHandler extends Handler {
        public QuickHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int message = msg.what;
            if (message == 0) {
                HwQuickTTFFMonitor.this.sendTtffQuickCommand("stop_quick_ttff");
            } else if (message == 1) {
                HwQuickTTFFMonitor.this.sendTtffQuickCommand("request_quick_ttff");
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendTtffQuickCommand(String quickCommand) {
        long identity = Binder.clearCallingIdentity();
        LocationManagerServiceUtil locationManagerServiceUtil = LocationManagerServiceUtil.getDefault();
        if (locationManagerServiceUtil == null) {
            LBSLog.i(TAG, false, "locationManagerServiceUtil is null", new Object[0]);
            return;
        }
        try {
            if ("request_quick_ttff".equals(quickCommand)) {
                locationManagerServiceUtil.handleQuickLocation(1);
            } else if ("stop_quick_ttff".equals(quickCommand)) {
                locationManagerServiceUtil.handleQuickLocation(0);
            } else {
                LBSLog.w(TAG, false, "sendExtraCommand: unknown command " + quickCommand, new Object[0]);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* access modifiers changed from: private */
    public void sendStopCommand() {
        LBSLog.i(TAG, false, "HwQuickTTFF STOP", new Object[0]);
        this.mHwLocationGpsLogServices.setQuickGpsParam(4, AwareJobSchedulerConstants.NETWORK_STATUS_ALL);
        sendMessage(0);
        cancelLocalTimerTask();
        this.mIsSendStartCommand = false;
        this.mIsQuickLocation = false;
        this.mIsReceive = false;
        this.mIsSatisfiedRequest = false;
        this.mRunning = false;
    }

    private synchronized void timerTask() {
        this.mQuickTTFFtimer = new Timer();
        this.mQuickTTFFtimer.schedule(new LocalTimerTask(), HwArbitrationDEFS.DelayTimeMillisA);
    }

    private GnssMeasurementsEvent getGnssMeasurements() {
        LBSLog.i(TAG, false, "quickgps event start. ", new Object[0]);
        mGnssMeasurement = new GnssMeasurement();
        mGnssClock = new GnssClock();
        mGnssMeasurement.setSvid(24);
        mGnssMeasurement.setConstellationType(1);
        mGnssMeasurement.setReceivedSvTimeNanos(1);
        mGnssMeasurement.setReceivedSvTimeUncertaintyNanos(1);
        mGnssMeasurement.setTimeOffsetNanos(0.0d);
        mGnssMeasurement.setCn0DbHz(30.0d);
        mGnssMeasurement.setPseudorangeRateMetersPerSecond(-700.0d);
        mGnssMeasurement.setPseudorangeRateUncertaintyMetersPerSecond(1.0d);
        mGnssMeasurement.setAccumulatedDeltaRangeState(1);
        mGnssMeasurement.setAccumulatedDeltaRangeMeters(1.0d);
        mGnssMeasurement.setAccumulatedDeltaRangeUncertaintyMeters(1.0d);
        mGnssMeasurement.setMultipathIndicator(0);
        mGnssMeasurement.setState(17);
        mGnssMeasurement.setCarrierFrequencyHz(QUICK_FAKE_CARRIER_FREQUENCY);
        GnssMeasurement[] mArray = {mGnssMeasurement};
        mGnssClock.setTimeNanos(1);
        mGnssClock.setHardwareClockDiscontinuityCount(0);
        mGnssMeasurementsEvent = new GnssMeasurementsEvent(mGnssClock, mArray);
        LBSLog.i(TAG, false, "quickgps event %{public}d", Integer.valueOf(mGnssMeasurementsEvent.getMeasurements().size()));
        return mGnssMeasurementsEvent;
    }

    private void pauseTask() {
        LBSLog.i(TAG, false, "quickgps event stop.", new Object[0]);
        try {
            Thread.currentThread();
            Thread.sleep(50);
        } catch (Exception e) {
            LBSLog.i(TAG, false, "quickgps sleep Exception", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public synchronized void cancelLocalTimerTask() {
        if (this.mQuickTTFFtimer != null) {
            this.mQuickTTFFtimer.cancel();
            this.mQuickTTFFtimer = null;
            LBSLog.i(TAG, false, "mQuickTTFFtimer.cancel", new Object[0]);
        }
    }

    public void onStartNavigating() {
        sendStartCommand();
        setNavigating(true);
    }

    public void onStopNavigating() {
        if (isRunning()) {
            sendStopCommand();
        }
        setNavigating(false);
    }

    public int reportLocationEx(Location location) {
        int sourceType = 0;
        if (location.getProvider() != null && !location.getProvider().equals("gps")) {
            try {
                sourceType = Integer.parseInt(location.getProvider());
            } catch (NumberFormatException e) {
                LBSLog.e(TAG, false, "unable to parse  mSourceType %{public}s", location.getProvider());
            }
            if ((sourceType & 128) == 128) {
                this.mUtils.reportMeasurementData(this.mGnssProvider, getGnssMeasurements());
                this.mUtils.reportMeasurementData(this.mGnssProvider, getGnssMeasurements());
                pauseTask();
                location.setProvider(QUICK_GPS);
            } else {
                location.setProvider("gps");
            }
        }
        return sourceType;
    }

    public boolean checkLocationChanged(Location location, LocationManagerService.LocationProvider provider) {
        if (!isReport(location)) {
            LBSLog.i(TAG, false, "QuickTTFFMonitor don't allow report location, return", new Object[0]);
            return false;
        }
        boolean isPassive = provider.isPassiveLocked();
        setQuickTTFFLocation(location, isPassive);
        removeAllHwQuickTTFF(isPassive ? "passive" : provider.getName());
        return true;
    }

    class LocalTimerTask extends TimerTask {
        LocalTimerTask() {
        }

        public void run() {
            HwQuickTTFFMonitor.this.cancelLocalTimerTask();
            if (!HwQuickTTFFMonitor.this.mIsReceive && HwQuickTTFFMonitor.this.mRunning) {
                LBSLog.e(HwQuickTTFFMonitor.TAG, false, " Not receive location:stop quickttff", new Object[0]);
                HwQuickTTFFMonitor.this.sendStopCommand();
            }
        }
    }
}
