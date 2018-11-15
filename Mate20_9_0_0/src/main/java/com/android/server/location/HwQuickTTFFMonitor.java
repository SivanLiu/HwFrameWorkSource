package com.android.server.location;

import android.content.Context;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationRequest;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.HwServiceFactory;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import libcore.io.IoUtils;

public class HwQuickTTFFMonitor {
    private static final float ACC_IN_WHITE_LIST = 50.0f;
    private static final int CN0S = 30;
    private static final boolean DBG = true;
    private static final boolean DEBUG = true;
    private static final int FIRST_SV = 0;
    private static final boolean IS_DEBUG_VERSION;
    public static final int LIST_TYPE_DISABLE = 4;
    public static final int LIST_TYPE_WHITE = 3;
    private static final int LOCATION_HAS_HWQUICKFIX = 256;
    private static final int METER_PER_SECOND = -700;
    private static final int QUICK_ENABLE_ONLY = 2;
    private static final String QUICK_GPS = "quickgps";
    private static final int QUICK_GPS_LOCATION = 2;
    private static final int QUICK_GPS_REQUEST_START = 1;
    private static final int QUICK_GPS_REQUEST_STOP = 3;
    private static final int QUICK_GPS_SESSION_START = 0;
    private static final int QUICK_GPS_SESSION_STOP = 4;
    private static final int QUICK_IS_ENABLE = 1;
    private static final String QUICK_START = "request_quick_ttff";
    private static final String QUICK_STOP = "stop_quick_ttff";
    private static final int QUICK_TTFF = 128;
    private static final int STATE_QTTFF = 17;
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
    private static ArrayList<String> mLocalQttffDisableList = new ArrayList(Arrays.asList(new String[]{"com.huawei.msdp", "com.huawei.HwOPServer"}));
    private static final Object mLock = new Object();
    private String DEBUG_PROPERTIES_FILE;
    private String Enable_Quickttff;
    private boolean isNavigating = false;
    private boolean isPermission = true;
    private boolean isQuickLocation = false;
    private boolean isQuickTTFFEnable = false;
    private boolean isReceive = false;
    private volatile boolean isSatisfiedRequest = false;
    private boolean isSendStartCommand = false;
    private HashMap<String, String> mAppMonitorMap;
    private Context mContext;
    private int mEnableQuickttff;
    LocationProviderInterface mGnssProvider;
    private IHwGpsLogServices mHwLocationGpsLogServices;
    private Properties mProperties;
    private Timer mQuickTTFFtimer;
    private ConcurrentHashMap<String, Long> mRemoveTimeMap;
    private ArraySet<String> m_QuickttffDisableList = new ArraySet();
    private ArraySet<String> m_accWhiteList = new ArraySet();
    private boolean m_running = false;

    class LocalTimerTask extends TimerTask {
        LocalTimerTask() {
        }

        public void run() {
            HwQuickTTFFMonitor.this.cancelLocalTimerTask();
            if (!HwQuickTTFFMonitor.this.isReceive && HwQuickTTFFMonitor.this.m_running) {
                Log.e(HwQuickTTFFMonitor.TAG, " Not receive location :stop quickttff");
                HwQuickTTFFMonitor.this.sendStopCommand();
            }
        }
    }

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) != 3) {
            z = false;
        }
        IS_DEBUG_VERSION = z;
    }

    private HwQuickTTFFMonitor(Context context, LocationProviderInterface provider) {
        this.mContext = context;
        this.mGnssProvider = provider;
        this.mHwLocationGpsLogServices = HwServiceFactory.getHwGpsLogServices(this.mContext);
    }

    public static HwQuickTTFFMonitor getInstance(Context context, LocationProviderInterface provider) {
        synchronized (mLock) {
            if (mHwQuickTTFFMonitorManager == null) {
                Log.i(TAG, "mHwQuickTTFFMonitorManager create.");
                mHwQuickTTFFMonitorManager = new HwQuickTTFFMonitor(context, provider);
            }
        }
        return mHwQuickTTFFMonitorManager;
    }

    public static HwQuickTTFFMonitor getMonitor() {
        return mHwQuickTTFFMonitorManager;
    }

    public void startMonitor() {
        quickTTFFEnable(configPath());
        if (this.isQuickTTFFEnable) {
            Log.i(TAG, "startMonitor");
            this.mAppMonitorMap = new HashMap();
            this.mRemoveTimeMap = new ConcurrentHashMap();
            this.m_running = false;
            this.isSendStartCommand = false;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" QuickTTFF is  not enable");
        stringBuilder.append(this.Enable_Quickttff);
        Log.e(str, stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:39:0x0106, code:
            r1.isSatisfiedRequest = true;
     */
    /* JADX WARNING: Missing block: B:40:0x010a, code:
            if (r1.m_running != false) goto L_0x011b;
     */
    /* JADX WARNING: Missing block: B:42:0x010e, code:
            if (r1.isNavigating == false) goto L_0x011b;
     */
    /* JADX WARNING: Missing block: B:43:0x0110, code:
            android.util.Log.i(TAG, "start HwQuickTTFF  when isNavigating");
            sendStartCommand();
     */
    /* JADX WARNING: Missing block: B:44:0x011b, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void requestHwQuickTTFF(LocationRequest request, String packageName, String requestProvider, String id) {
        Throwable th;
        String str = packageName;
        if (this.isQuickTTFFEnable) {
            String packageNameRemove;
            if (!"gps".equals(request.getProvider())) {
                String str2 = requestProvider;
            } else if ("gps".equals(requestProvider) && this.isPermission) {
                String permissionStatus = String.valueOf(this.m_QuickttffDisableList.contains(str) ^ true);
                String requestAccuracy = "null";
                if (this.m_accWhiteList.contains(str)) {
                    requestAccuracy = String.valueOf(ACC_IN_WHITE_LIST);
                } else {
                    requestAccuracy = "null";
                }
                String requestAccuracy2 = requestAccuracy;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(",");
                stringBuilder.append(permissionStatus);
                stringBuilder.append(",");
                stringBuilder.append(requestAccuracy2);
                this.mHwLocationGpsLogServices.setQuickGpsParam(1, stringBuilder.toString());
                if (isQuickttffDisableListApp(str)) {
                    requestAccuracy = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(str);
                    stringBuilder2.append(" is in black list  can not start Quickttff");
                    Log.i(requestAccuracy, stringBuilder2.toString());
                    return;
                }
                if (this.mRemoveTimeMap.size() > 0) {
                    for (Entry<String, Long> entry : this.mRemoveTimeMap.entrySet()) {
                        packageNameRemove = (String) entry.getKey();
                        if (System.currentTimeMillis() - ((Long) entry.getValue()).longValue() > 2000) {
                            this.mRemoveTimeMap.remove(packageNameRemove);
                        }
                    }
                    if (this.mRemoveTimeMap.containsKey(str)) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(str);
                        stringBuilder3.append(" can not start quickttff in 2s");
                        Log.i(str3, stringBuilder3.toString());
                        return;
                    }
                }
                synchronized (mLock) {
                    try {
                        try {
                            this.mAppMonitorMap.put(id, str);
                            requestAccuracy = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("requestHwQuickTTFF:");
                            stringBuilder4.append(str);
                            Log.i(requestAccuracy, stringBuilder4.toString());
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        packageNameRemove = id;
                        throw th;
                    }
                }
            }
            packageNameRemove = id;
        }
    }

    public void removeHwQuickTTFF(String packageName, String id) {
        if (this.isQuickTTFFEnable && this.mAppMonitorMap.containsKey(id)) {
            synchronized (mLock) {
                this.mAppMonitorMap.remove(id);
                this.mRemoveTimeMap.put(packageName, Long.valueOf(System.currentTimeMillis()));
                this.mHwLocationGpsLogServices.setQuickGpsParam(3, packageName);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeHwQuickTTFF:");
                stringBuilder.append(packageName);
                Log.i(str, stringBuilder.toString());
                if (this.mAppMonitorMap.size() > 0 || !this.m_running) {
                    return;
                }
                Log.i(TAG, "removeHwQuickTTFF HwQuickTTFF STOP");
                sendStopCommand();
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0021, code:
            android.util.Log.i(TAG, "removeAllHwQuickTTFF HwQuickTTFF STOP");
     */
    /* JADX WARNING: Missing block: B:17:0x002f, code:
            if (r6.mAppMonitorMap.size() <= 0) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:18:0x0031, code:
            r0 = r6.mAppMonitorMap.entrySet().iterator();
     */
    /* JADX WARNING: Missing block: B:20:0x003f, code:
            if (r0.hasNext() == false) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:21:0x0041, code:
            r6.mRemoveTimeMap.put((java.lang.String) ((java.util.Map.Entry) r0.next()).getValue(), java.lang.Long.valueOf(java.lang.System.currentTimeMillis()));
     */
    /* JADX WARNING: Missing block: B:22:0x005b, code:
            r6.mAppMonitorMap.clear();
            sendStopCommand();
     */
    /* JADX WARNING: Missing block: B:23:0x0063, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeAllHwQuickTTFF(String provider) {
        if (this.isQuickTTFFEnable && !this.isQuickLocation && "gps".equals(provider)) {
            synchronized (mLock) {
                if (this.m_running) {
                    this.m_running = false;
                    this.isSendStartCommand = false;
                }
            }
        }
    }

    public boolean isLocationReportToApp(String packageName, String provider, Location location) {
        boolean isReportLocation = false;
        float acc = location.getAccuracy();
        if (!this.isQuickLocation || !"gps".equals(provider)) {
            return true;
        }
        if (this.mAppMonitorMap.containsValue(packageName)) {
            if (isAccSatisfied(packageName, acc)) {
                isReportLocation = true;
            } else {
                isReportLocation = false;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isReportQuickTTFFLocation:");
        stringBuilder.append(isReportLocation);
        stringBuilder.append(", pkgname=");
        stringBuilder.append(packageName);
        Log.i(str, stringBuilder.toString());
        return isReportLocation;
    }

    public void setQuickTTFFLocation(Location location, boolean passive) {
        if (this.isQuickTTFFEnable && !passive) {
            if ((QUICK_GPS.equals(location.getProvider()) || "gps".equals(location.getProvider())) && this.m_running) {
                this.mHwLocationGpsLogServices.setQuickGpsParam(2, String.valueOf(location.getAccuracy()));
            }
            if (QUICK_GPS.equals(location.getProvider())) {
                this.isQuickLocation = true;
                location.setProvider("gps");
                Bundle extras = location.getExtras();
                if (extras == null) {
                    extras = new Bundle();
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SourceType");
                stringBuilder.append(location.getExtras().getInt("SourceType"));
                Log.i(str, stringBuilder.toString());
                extras.putBoolean("QUICKGPS", true);
                location.setExtras(extras);
                Log.i(TAG, "is a QuickTTFFLocation");
                if (!this.isReceive) {
                    this.isReceive = true;
                    Log.i(TAG, "have receive QuickTTFFLocation");
                    cancelLocalTimerTask();
                }
            } else if ("gps".equals(location.getProvider())) {
                this.isQuickLocation = false;
            }
        }
    }

    public boolean isQuickLocation(Location location) {
        int mSourceType = 0;
        if (location.getExtras() != null) {
            mSourceType = location.getExtras().getInt("SourceType");
        }
        if ((mSourceType & 128) == 128) {
            return true;
        }
        return false;
    }

    public boolean isRunning() {
        return this.m_running;
    }

    public void setPermission(boolean permission) {
        this.isPermission = permission;
    }

    public void setNavigating(boolean Navigating) {
        this.isNavigating = Navigating;
    }

    private boolean isQuickttffDisableListApp(String appName) {
        if (this.m_QuickttffDisableList == null) {
            return false;
        }
        String str;
        if (this.m_QuickttffDisableList.contains(appName)) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(appName);
            stringBuilder.append("is not WhiteApp");
            Log.i(str, stringBuilder.toString());
            return true;
        }
        str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(appName);
        stringBuilder2.append("is  WhiteApp");
        Log.i(str, stringBuilder2.toString());
        return false;
    }

    private boolean isAccSatisfied(String appName, float acc) {
        if (this.m_QuickttffDisableList == null || !this.m_accWhiteList.contains(appName)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(appName);
        stringBuilder.append("is inAccList ");
        Log.i(str, stringBuilder.toString());
        if (ACC_IN_WHITE_LIST >= acc) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Satisfied with Acc50.0");
            stringBuilder.append(acc);
            Log.d(str, stringBuilder.toString());
            return true;
        }
        str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" Not Satisfied with Acc50.0");
        stringBuilder2.append(acc);
        Log.d(str, stringBuilder2.toString());
        return false;
    }

    public void updateAccWhiteList(List<String> accWhiteList) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AccWhiteList ");
        stringBuilder.append(accWhiteList.size());
        Log.d(str, stringBuilder.toString());
        for (String packageName : accWhiteList) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("AccWhiteList ");
            stringBuilder2.append(packageName);
            Log.d(str2, stringBuilder2.toString());
        }
        this.m_accWhiteList.clear();
        this.m_accWhiteList.addAll(accWhiteList);
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("accWhiteList");
        stringBuilder.append(accWhiteList);
        Log.d(str, stringBuilder.toString());
    }

    public void updateDisableList(List<String> QuickttffDisableList) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DisableList ");
        stringBuilder.append(QuickttffDisableList.size());
        Log.d(str, stringBuilder.toString());
        for (String packageName : QuickttffDisableList) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DisableList ");
            stringBuilder2.append(packageName);
            Log.d(str2, stringBuilder2.toString());
        }
        this.m_QuickttffDisableList.clear();
        this.m_QuickttffDisableList.addAll(mLocalQttffDisableList);
        this.m_QuickttffDisableList.addAll(QuickttffDisableList);
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append(" QuickttffDisableList ");
        stringBuilder.append(QuickttffDisableList);
        Log.d(str, stringBuilder.toString());
    }

    public void quickTTFFEnable(String filename) {
        String str;
        StringBuilder stringBuilder;
        this.mProperties = new Properties();
        FileInputStream stream;
        try {
            stream = null;
            stream = new FileInputStream(new File(filename));
            this.mProperties.load(stream);
            IoUtils.closeQuietly(stream);
        } catch (IOException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not open higeo configuration file ");
            stringBuilder.append(filename);
            Log.d(str, stringBuilder.toString());
            this.isQuickTTFFEnable = false;
        } catch (Throwable th) {
            IoUtils.closeQuietly(stream);
        }
        this.Enable_Quickttff = this.mProperties.getProperty("higeo_enable_quickttff");
        if (!(this.Enable_Quickttff == null || BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(this.Enable_Quickttff))) {
            try {
                this.mEnableQuickttff = Integer.parseInt(this.Enable_Quickttff);
            } catch (NumberFormatException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unable to parse Enable_Quickttff: ");
                stringBuilder.append(this.Enable_Quickttff);
                Log.e(str, stringBuilder.toString());
                this.isQuickTTFFEnable = false;
            }
        }
        String str2;
        StringBuilder stringBuilder2;
        if (this.mEnableQuickttff == 1) {
            this.isQuickTTFFEnable = true;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" QuickTTFF is enable");
            stringBuilder2.append(this.Enable_Quickttff);
            Log.e(str2, stringBuilder2.toString());
        } else if (this.mEnableQuickttff == 2) {
            this.isQuickTTFFEnable = true;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" QuickTTFF only is enable");
            stringBuilder2.append(this.Enable_Quickttff);
            Log.e(str2, stringBuilder2.toString());
        } else {
            this.isQuickTTFFEnable = false;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" QuickTTFF is  not enable");
            stringBuilder2.append(this.Enable_Quickttff);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    private boolean isBetaUser() {
        return IS_DEBUG_VERSION;
    }

    private String configPath() {
        File file;
        String str;
        StringBuilder stringBuilder;
        if (isBetaUser()) {
            Log.i(TAG, "This is beta user");
            file = HwCfgFilePolicy.getCfgFile("xml/higeo_beta.conf", 0);
            if (file != null) {
                this.DEBUG_PROPERTIES_FILE = file.getPath();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("configPath is");
                stringBuilder.append(this.DEBUG_PROPERTIES_FILE);
                Log.d(str, stringBuilder.toString());
            } else {
                Log.d(TAG, "configPath is not /cust_spec/xml/  ");
                this.DEBUG_PROPERTIES_FILE = "/odm/etc/higeo_beta.conf";
            }
        } else {
            Log.i(TAG, "This is not beta user");
            file = HwCfgFilePolicy.getCfgFile("xml/higeo.conf", 0);
            if (file != null) {
                this.DEBUG_PROPERTIES_FILE = file.getPath();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("configPath is");
                stringBuilder.append(this.DEBUG_PROPERTIES_FILE);
                Log.d(str, stringBuilder.toString());
            } else {
                Log.d(TAG, "configPath is not /cust_spec/xml/  ");
                this.DEBUG_PROPERTIES_FILE = "/odm/etc/higeo.conf";
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("configPath is");
        stringBuilder2.append(this.DEBUG_PROPERTIES_FILE);
        Log.d(str2, stringBuilder2.toString());
        return this.DEBUG_PROPERTIES_FILE;
    }

    public boolean isReport(Location location) {
        if (this.m_running || !QUICK_GPS.equals(location.getProvider())) {
            return true;
        }
        return false;
    }

    public void clearAppMonitorMap() {
        this.mAppMonitorMap.clear();
        Log.i(TAG, "clearAppMonitorMap");
    }

    public void sendStartCommand() {
        if (this.m_running) {
            Log.i(TAG, "HwQuickTTFF has be started before.");
            return;
        }
        if (!this.isSendStartCommand && this.isSatisfiedRequest) {
            this.isReceive = false;
            timerTask();
            this.m_running = true;
            this.isSendStartCommand = true;
            Log.i(TAG, "HwQuickTTFF Start");
            this.mHwLocationGpsLogServices.setQuickGpsParam(0, QUICK_START);
            this.mGnssProvider.sendExtraCommand(QUICK_START, null);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isSendStartCommand");
            stringBuilder.append(this.isSendStartCommand);
            Log.e(str, stringBuilder.toString());
        }
    }

    public void sendStopCommand() {
        this.m_running = false;
        this.isSendStartCommand = false;
        this.isQuickLocation = false;
        this.isReceive = false;
        this.isSatisfiedRequest = false;
        Log.i(TAG, "HwQuickTTFF STOP");
        this.mHwLocationGpsLogServices.setQuickGpsParam(4, "ALL");
        this.mGnssProvider.sendExtraCommand(QUICK_STOP, null);
        cancelLocalTimerTask();
    }

    public void timerTask() {
        this.mQuickTTFFtimer = new Timer();
        this.mQuickTTFFtimer.schedule(new LocalTimerTask(), 30000);
    }

    public static GnssMeasurementsEvent getGnssMeasurements() {
        Log.i(TAG, "quickgps event start. ");
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
        GnssMeasurement[] mArray = new GnssMeasurement[]{mGnssMeasurement};
        mGnssClock.setTimeNanos(1);
        mGnssClock.setHardwareClockDiscontinuityCount(0);
        mGnssMeasurementsEvent = new GnssMeasurementsEvent(mGnssClock, mArray);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("quickgps event ");
        stringBuilder.append(mGnssMeasurementsEvent.getMeasurements().size());
        Log.i(str, stringBuilder.toString());
        return mGnssMeasurementsEvent;
    }

    public static void pauseTask() {
        Log.i(TAG, "quickgps event stop. ");
        try {
            Thread.currentThread();
            Thread.sleep(50);
        } catch (Exception e) {
            Log.i(TAG, "quickgps sleep Exception");
        }
    }

    private synchronized void cancelLocalTimerTask() {
        if (this.mQuickTTFFtimer != null) {
            this.mQuickTTFFtimer.cancel();
            this.mQuickTTFFtimer = null;
            Log.i(TAG, "mQuickTTFFtimer.cancel");
        }
    }
}
