package com.android.server.mtm.iaware.srms;

import android.app.usage.UsageStatsManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.ActionFilterEntry;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.LocalServices;
import com.android.server.am.HwBroadcastRecord;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.policy.BrFilterPolicy;
import com.android.server.mtm.iaware.brjob.controller.KeyWordController;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import com.android.server.pm.PackageManagerService;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.IAwareStateCallback;
import com.android.server.rms.iaware.appmng.AwareSceneRecognize;
import com.android.server.rms.iaware.appmng.AwareSceneRecognize.IAwareSceneRecCallback;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.srms.BroadcastExFeature;
import com.android.server.rms.iaware.srms.BroadcastFeature;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

public class AwareBroadcastPolicy {
    private static final int APP_STATE_IDLE = 1;
    private static final int APP_STATE_NOT_IDLE = 0;
    private static final int BATTERY_DATA_FILTER = 1;
    private static final int BATTERY_DATA_NO_FILTER = 0;
    public static final String BATTERY_LEVEL_NAME = "BatteryLevel";
    public static final String BATTERY_STATUS_CHANGE_VALUE = "CHARGING_CHANGE";
    public static final String BATTERY_STATUS_CHARGINGOFF_VALUE = "CHARGING_OFF";
    public static final String BATTERY_STATUS_CHARGINGON_VALUE = "CHARGING_ON";
    public static final String BATTERY_STATUS_NAME = "BatteryStatus";
    private static final int BROADCAST_PROXY_SPEED_INDEX = 2;
    private static final int BROADCAST_PROXY_SPEED_INTERVAL = 100;
    private static final long BROADCAST_PROXY_SPEED_INTERVAL_LONG = 100;
    private static final int BROADCAST_PROXY_SPEED_NUMBER = 10;
    private static final int BROADCAST_PROXY_SPEED_NUMBER_INDEX_MAX = 9;
    public static final int CHARGING_OFF = 0;
    public static final int CHARGING_ON = 1;
    public static final String CONNECTIVITY_NAME = "ConnectStatus";
    public static final String CONNECT_STATUS_MOBILE_CON_VALUE = "MOBILEDATACON";
    public static final String CONNECT_STATUS_MOBILE_DSCON_VALUE = "MOBILEDATADSCON";
    public static final String CONNECT_STATUS_WIFI_CON_VALUE = "WIFIDATACON";
    public static final String CONNECT_STATUS_WIFI_DSCON_VALUE = "WIFIDATADSCON";
    public static final String DROP = "DROP";
    public static final String DROP_TYPE = "DropType";
    public static final String EXTRA_NAME = "Extra";
    private static final int GOOGLE_STATE_CONNECTED = 1;
    private static final int GOOGLE_STATE_DISCONNECTED = -1;
    private static final int IAWARE_APP_INSTALL_DELAY_TIME = 5000;
    private static final int IAWARE_DWONLOAD_DELAY_TIME = 3000;
    private static final int IAWARE_START_ACTIVITY_DELAY_TIME = 3000;
    public static final int INVALUED_DATA = -1;
    private static final int LOOPER_CHECK_TIME = 1000;
    private static final int LOOPER_CHECK_TIME_RECOUNT = 2000;
    public static final int MOBILE_CON = 1;
    public static final int MOBILE_DSCON = 2;
    private static final int MSG_APP_IDLE_STAT_CHANGE = 211;
    private static final int MSG_GOOGLE_CONN_STAT_CHANGE = 210;
    private static final int MSG_INSTALL_APP_TIMEOUT = 208;
    private static final int MSG_NOTIFY_OVERFLOW = 212;
    private static final int MSG_POLICY_DL_END = 202;
    private static final int MSG_POLICY_DL_START = 201;
    private static final int MSG_POLICY_END_CHECK = 206;
    private static final int MSG_POLICY_SCENE_ACTIVITY = 205;
    private static final int MSG_POLICY_SCENE_SLIP = 203;
    private static final int MSG_START_ACTIVITY_TIMEOUT = 207;
    private static final int MSG_UPDATE_BR_POLICY = 209;
    public static final String SCREEN_NAME = "ScreenStatus";
    public static final String SCREEN_STATUS_OFF = "SCREENOFF";
    public static final String SCREEN_STATUS_ON = "SCREENON";
    private static final String TAG = "AwareBroadcastPolicy";
    public static final int UNKNOW_CHARGING = -1;
    public static final int UNKNOW_CON = -1;
    public static final int WIFI_CON = 3;
    private static final int WIFI_DATA_DISCARD = 2;
    private static final int WIFI_DATA_FILTER = 1;
    private static final int WIFI_DATA_NO_FILTER = 0;
    public static final int WIFI_DSCON = 4;
    public static final String WIFI_NET_STATUS_NAME = "WifiNetStatus";
    public static final String WIFI_RSSI_STATUS_NAME = "WifiRssi";
    public static final String WIFI_STATUS_CONNECTING_VALUE = "WIFICONTING";
    public static final String WIFI_STATUS_CONNECT_VALUE = "WIFICON";
    public static final String WIFI_STATUS_DISABLED_VALUE = "WIFIDISABLED";
    public static final String WIFI_STATUS_DISCONNECT_VALUE = "WIFIDSCON";
    public static final String WIFI_STATUS_ENABLED_VALUE = "WIFIENABLED";
    public static final String WIFI_STATUS_NAME = "WifiStatus";
    public static final String WIFI_SUP_STATUS_COMPLET_VALUE = "WIFISUPCOMPLE";
    public static final String WIFI_SUP_STATUS_DISCONNECT_VALUE = "WIFISUPDSCON";
    public static final String WIFI_SUP_STATUS_NAME = "WifiSupStatus";
    private static boolean mGoogleConnStat = false;
    private static ArrayMap<String, AwareBroadcastCache> mIawareBrCaches = new ArrayMap();
    private AwareBroadcastConfig mAwareBroadcastConfig;
    private AwareSceneStateCallback mAwareSceneStateCallback;
    private AwareStateCallback mAwareStateCallback;
    private int mBatteryLevel = -1;
    private AwareBroadcastProcess mBgIawareBr = null;
    private int mCharging = -1;
    private int mConnectStatus = -1;
    private Context mContext = null;
    private long mCountCheck = 0;
    private AwareBroadcastProcess mFgIawareBr = null;
    private int mForegroundAppLevel = 2;
    private final IawareBroadcastPolicyHandler mHandler;
    private ArraySet<Integer> mIawareDownloadingUid = new ArraySet();
    private ArrayMap<String, FilterStatus> mIawareFilterStatus = new ArrayMap();
    private boolean mIawareInstallApp = false;
    private ArraySet<String> mIawareNoProxyActions = new ArraySet();
    private ArraySet<String> mIawareNoProxyPkgs = new ArraySet();
    private boolean mIawareProxyActivitStart = false;
    private boolean mIawareProxySlip = false;
    private boolean mIawareScreenOn = true;
    private ArraySet<String> mIawareTrimActions = new ArraySet();
    private ArrayMap<String, ArraySet<String>> mIawareUnProxySys = new ArrayMap();
    private long mLastParallelBrTime = 0;
    private Object mLockNoProxyActions = new Object();
    private Object mLockNoProxyPkgs = new Object();
    private Object mLockTrimActions = new Object();
    private Object mLockUnProxySys = new Object();
    private int mNoTouchCheckCount = 200;
    private int mPlugedtype = -1;
    private int mPrePlugedtype = -1;
    private final long[][] mProxyCount = ((long[][]) Array.newInstance(long.class, new int[]{10, 2}));
    private boolean mScreenOn = true;
    private boolean mSpeedParallelStartProxy = false;
    private long mStartParallelBrTime = 0;
    private int mTouchCheckCount = 60;
    private final UsageStatsManagerInternal mUsageStatsInternal;
    private State mWifiNetStatue = State.UNKNOWN;
    private int mWifiRssi = -127;
    private int mWifiStatue = 4;
    private SupplicantState mWifiSupStatue = SupplicantState.INVALID;

    /* renamed from: com.android.server.mtm.iaware.srms.AwareBroadcastPolicy$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$net$NetworkInfo$State = new int[State.values().length];
        static final /* synthetic */ int[] $SwitchMap$android$net$wifi$SupplicantState = new int[SupplicantState.values().length];

        static {
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.COMPLETED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.DISCONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INVALID.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$State[State.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$State[State.DISCONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$State[State.CONNECTING.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$State[State.UNKNOWN.ordinal()] = 4;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    private final class AppIdleStateChangeListener extends android.app.usage.UsageStatsManagerInternal.AppIdleStateChangeListener {
        private AppIdleStateChangeListener() {
        }

        /* synthetic */ AppIdleStateChangeListener(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            Message msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
            msg.what = 211;
            if (idle) {
                msg.arg1 = 1;
            } else {
                msg.arg1 = 0;
            }
            msg.arg2 = userId;
            msg.obj = packageName;
            AwareBroadcastPolicy.this.mHandler.sendMessage(msg);
        }

        public void onParoleStateChanged(boolean isParoleOn) {
        }
    }

    public enum BrCtrlType {
        NONE("do-nothing"),
        CACHEBR("cachebr"),
        DISCARDBR("discardbr"),
        DATADEFAULTBR("datadefaultbr");
        
        String mDescription;

        private BrCtrlType(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    private abstract class FilterStatus {
        public abstract boolean filter(Intent intent, String str);

        private FilterStatus() {
        }

        /* synthetic */ FilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }
    }

    private final class IawareBroadcastPolicyHandler extends Handler {
        public IawareBroadcastPolicyHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 201:
                    synchronized (AwareBroadcastPolicy.this.mIawareDownloadingUid) {
                        AwareBroadcastPolicy.this.mIawareDownloadingUid.add(Integer.valueOf(msg.arg1));
                    }
                    return;
                case 202:
                    synchronized (AwareBroadcastPolicy.this.mIawareDownloadingUid) {
                        AwareBroadcastPolicy.this.mIawareDownloadingUid.remove(Integer.valueOf(msg.arg1));
                    }
                    return;
                case 203:
                    AwareBroadcastPolicy.this.setIawarePolicy(2, msg.arg1);
                    return;
                case 205:
                    AwareBroadcastPolicy.this.setIawarePolicy(4, msg.arg1);
                    return;
                case 206:
                    AwareBroadcastPolicy.this.startUnproxyBroadcast();
                    return;
                case 207:
                    AwareBroadcastPolicy.this.mIawareProxyActivitStart = false;
                    return;
                case 208:
                    AwareBroadcastPolicy.this.mIawareInstallApp = false;
                    return;
                case 209:
                    AwareBroadcastPolicy.this.updateBrPolicy(msg.arg1, msg.obj);
                    return;
                case 210:
                    AwareBroadcastPolicy.this.resetGoogleConnStat(msg.arg1);
                    return;
                case 211:
                    AwareBroadcastPolicy.this.updateAppIdleStat(msg.arg1, msg.arg2, (String) msg.obj);
                    return;
                case 212:
                    AwareBroadcastPolicy.this.unproxyCacheBr(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    private class AwareSceneStateCallback implements IAwareSceneRecCallback {
        private AwareSceneStateCallback() {
        }

        /* synthetic */ AwareSceneStateCallback(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public void onStateChanged(int sceneType, int eventType, String pkg) {
            if (BroadcastFeature.isFeatureEnabled(10)) {
                Message msg;
                if (sceneType == 2) {
                    msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
                    msg.what = 203;
                    msg.arg1 = eventType;
                    AwareBroadcastPolicy.this.mHandler.sendMessage(msg);
                } else if (sceneType == 4) {
                    msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
                    msg.what = 205;
                    msg.arg1 = eventType;
                    AwareBroadcastPolicy.this.mHandler.sendMessage(msg);
                    if (AwareBroadcastPolicy.this.mHandler.hasMessages(207)) {
                        AwareBroadcastPolicy.this.mHandler.removeMessages(207);
                    }
                    if (eventType == 1) {
                        AwareBroadcastPolicy.this.mHandler.sendEmptyMessageDelayed(207, HwArbitrationDEFS.NotificationMonitorPeriodMillis);
                    }
                } else if (AwareBroadcastDebug.getDebugDetail()) {
                    String str = AwareBroadcastPolicy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("don't process scene type ");
                    stringBuilder.append(sceneType);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private class AwareStateCallback implements IAwareStateCallback {
        private AwareStateCallback() {
        }

        /* synthetic */ AwareStateCallback(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public void onStateChanged(int stateType, int eventType, int pid, int uid) {
            AwareBroadcastPolicy.this.updateProcessPolicy(stateType, eventType, pid, uid);
            if (BroadcastFeature.isFeatureEnabled(10) && stateType == 5 && uid >= 0) {
                Message msg;
                if (eventType == 1) {
                    msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
                    msg.what = 201;
                    msg.arg1 = uid;
                    AwareBroadcastPolicy.this.mHandler.sendMessage(msg);
                } else if (eventType == 2) {
                    msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
                    msg.what = 202;
                    msg.arg1 = uid;
                    AwareBroadcastPolicy.this.mHandler.sendMessageDelayed(msg, HwArbitrationDEFS.NotificationMonitorPeriodMillis);
                } else if (AwareBroadcastDebug.getDebugDetail()) {
                    String str = AwareBroadcastPolicy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("don't process type ");
                    stringBuilder.append(eventType);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private class BatteryFilterStatus extends FilterStatus {
        private BatteryFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ BatteryFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x0039 A:{RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:30:0x0060  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x004d  */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x003a  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x0039 A:{RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:30:0x0060  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x004d  */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x003a  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x0039 A:{RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:30:0x0060  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x004d  */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x003a  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean filter(Intent intent, String filterValue) {
            boolean z;
            int hashCode = filterValue.hashCode();
            if (hashCode == -992200083) {
                if (filterValue.equals(AwareBroadcastPolicy.BATTERY_STATUS_CHARGINGON_VALUE)) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == -693431679) {
                if (filterValue.equals(AwareBroadcastPolicy.BATTERY_STATUS_CHARGINGOFF_VALUE)) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 427770174 && filterValue.equals(AwareBroadcastPolicy.BATTERY_STATUS_CHANGE_VALUE)) {
                z = false;
                switch (z) {
                    case false:
                        return AwareBroadcastPolicy.this.mPrePlugedtype == AwareBroadcastPolicy.this.mPlugedtype && AwareBroadcastPolicy.this.mPrePlugedtype != -1;
                    case true:
                        return (AwareBroadcastPolicy.this.mCharging == 1 || AwareBroadcastPolicy.this.mCharging == -1) ? false : true;
                    case true:
                        return (AwareBroadcastPolicy.this.mCharging == 0 || AwareBroadcastPolicy.this.mCharging == -1) ? false : true;
                    default:
                        return true;
                }
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
        }
    }

    private class BatteryLevelFilterStatus extends FilterStatus {
        private BatteryLevelFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ BatteryLevelFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            try {
                String[] values = filterValue.split(":");
                if (values.length != 2) {
                    return false;
                }
                int minLevel = Integer.parseInt(values[0]);
                int maxLevel = Integer.parseInt(values[1]);
                if (minLevel >= maxLevel) {
                    return false;
                }
                if (AwareBroadcastPolicy.this.mBatteryLevel < minLevel || AwareBroadcastPolicy.this.mBatteryLevel > maxLevel) {
                    return true;
                }
                return false;
            } catch (NumberFormatException e) {
                AwareLog.e(AwareBroadcastPolicy.TAG, "iaware_br level value format is error");
                return false;
            }
        }
    }

    private class ConnectivityFilterStatus extends FilterStatus {
        private ConnectivityFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ ConnectivityFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            int access$3300 = AwareBroadcastPolicy.this.mConnectStatus;
            if (access$3300 == -1) {
                return false;
            }
            switch (access$3300) {
                case 1:
                    return "MOBILEDATACON".equals(filterValue) ^ 1;
                case 2:
                    return "MOBILEDATADSCON".equals(filterValue) ^ 1;
                case 3:
                    return AwareBroadcastPolicy.CONNECT_STATUS_WIFI_CON_VALUE.equals(filterValue) ^ 1;
                case 4:
                    return AwareBroadcastPolicy.CONNECT_STATUS_WIFI_DSCON_VALUE.equals(filterValue) ^ 1;
                default:
                    return true;
            }
        }
    }

    private class DropFilterStatus extends FilterStatus {
        private DropFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ DropFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            if (AwareBroadcastPolicy.DROP.equals(filterValue)) {
                return true;
            }
            return false;
        }
    }

    private class ExtraFilterStatus extends FilterStatus {
        private ExtraFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ ExtraFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            String[] extras = filterValue.split("[\\[\\]]");
            int i = 0;
            while (i < extras.length) {
                if (!(extras[i] == null || extras[i].trim().length() == 0 || ":".equals(extras[i].trim()))) {
                    if (AwareBroadcastDebug.getDebug()) {
                        String str = AwareBroadcastPolicy.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("iaware_br compare extra: ");
                        stringBuilder.append(extras[i]);
                        AwareLog.i(str, stringBuilder.toString());
                    }
                    String[] values = extras[i].split("[:@]");
                    if (values.length != 3) {
                        AwareLog.e(AwareBroadcastPolicy.TAG, "iaware_br extra value length is wrong.");
                        return false;
                    } else if (KeyWordController.matchReg(values[0], values[1], values[2], intent)) {
                        return false;
                    }
                }
                i++;
            }
            return true;
        }
    }

    private class ScreenFilterStatus extends FilterStatus {
        private ScreenFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ ScreenFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            if (AwareBroadcastPolicy.SCREEN_STATUS_ON.equals(filterValue)) {
                return AwareBroadcastPolicy.this.mScreenOn ^ 1;
            }
            if (AwareBroadcastPolicy.SCREEN_STATUS_OFF.equals(filterValue)) {
                return AwareBroadcastPolicy.this.mScreenOn;
            }
            return true;
        }
    }

    private class WifiFilterStatus extends FilterStatus {
        private WifiFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ WifiFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            int access$2600 = AwareBroadcastPolicy.this.mWifiStatue;
            if (access$2600 == 1) {
                return AwareBroadcastPolicy.WIFI_STATUS_DISABLED_VALUE.equals(filterValue) ^ 1;
            }
            switch (access$2600) {
                case 3:
                    return AwareBroadcastPolicy.WIFI_STATUS_ENABLED_VALUE.equals(filterValue) ^ 1;
                case 4:
                    return false;
                default:
                    return true;
            }
        }
    }

    private class WifiNetFilterStatus extends FilterStatus {
        private WifiNetFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ WifiNetFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            switch (AnonymousClass1.$SwitchMap$android$net$NetworkInfo$State[AwareBroadcastPolicy.this.mWifiNetStatue.ordinal()]) {
                case 1:
                    return "WIFICON".equals(filterValue) ^ 1;
                case 2:
                    return "WIFIDSCON".equals(filterValue) ^ 1;
                case 3:
                    return AwareBroadcastPolicy.WIFI_STATUS_CONNECTING_VALUE.equals(filterValue) ^ 1;
                case 4:
                    return false;
                default:
                    return true;
            }
        }
    }

    private class WifiRssiFilterStatus extends FilterStatus {
        private WifiRssiFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ WifiRssiFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            try {
                String[] values = filterValue.split(":");
                if (values.length != 2) {
                    return false;
                }
                int minRssi = Integer.parseInt(values[0]);
                int maxRssi = Integer.parseInt(values[1]);
                if (minRssi >= maxRssi) {
                    return false;
                }
                if (AwareBroadcastPolicy.this.mWifiRssi > maxRssi || AwareBroadcastPolicy.this.mWifiRssi < minRssi) {
                    return true;
                }
                return false;
            } catch (NumberFormatException e) {
                AwareLog.e(AwareBroadcastPolicy.TAG, "iaware_br rssi value format is error");
                return false;
            }
        }
    }

    private class WifiSupFilterStatus extends FilterStatus {
        private WifiSupFilterStatus() {
            super(AwareBroadcastPolicy.this, null);
        }

        /* synthetic */ WifiSupFilterStatus(AwareBroadcastPolicy x0, AnonymousClass1 x1) {
            this();
        }

        public boolean filter(Intent intent, String filterValue) {
            switch (AnonymousClass1.$SwitchMap$android$net$wifi$SupplicantState[AwareBroadcastPolicy.this.mWifiSupStatue.ordinal()]) {
                case 1:
                    return AwareBroadcastPolicy.WIFI_SUP_STATUS_COMPLET_VALUE.equals(filterValue) ^ 1;
                case 2:
                    return AwareBroadcastPolicy.WIFI_SUP_STATUS_DISCONNECT_VALUE.equals(filterValue) ^ 1;
                case 3:
                    return false;
                default:
                    return true;
            }
        }
    }

    public AwareBroadcastPolicy(Handler handler, Context context) {
        this.mContext = context;
        this.mBgIawareBr = new AwareBroadcastProcess(this, handler, "iawarebackground");
        this.mFgIawareBr = new AwareBroadcastProcess(this, handler, "iawareforeground");
        this.mHandler = new IawareBroadcastPolicyHandler(handler.getLooper());
        this.mAwareBroadcastConfig = AwareBroadcastConfig.getInstance();
        this.mIawareFilterStatus.put(WIFI_NET_STATUS_NAME, new WifiNetFilterStatus(this, null));
        this.mIawareFilterStatus.put("WifiStatus", new WifiFilterStatus(this, null));
        this.mIawareFilterStatus.put(WIFI_SUP_STATUS_NAME, new WifiSupFilterStatus(this, null));
        this.mIawareFilterStatus.put(WIFI_RSSI_STATUS_NAME, new WifiRssiFilterStatus(this, null));
        this.mIawareFilterStatus.put(BATTERY_STATUS_NAME, new BatteryFilterStatus(this, null));
        this.mIawareFilterStatus.put(BATTERY_LEVEL_NAME, new BatteryLevelFilterStatus(this, null));
        this.mIawareFilterStatus.put(CONNECTIVITY_NAME, new ConnectivityFilterStatus(this, null));
        this.mIawareFilterStatus.put("Extra", new ExtraFilterStatus(this, null));
        this.mIawareFilterStatus.put(SCREEN_NAME, new ScreenFilterStatus(this, null));
        this.mIawareFilterStatus.put(DROP_TYPE, new DropFilterStatus(this, null));
        initState();
        this.mUsageStatsInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        this.mUsageStatsInternal.addAppIdleStateChangeListener(new AppIdleStateChangeListener(this, null));
    }

    public void init() {
        this.mAwareStateCallback = new AwareStateCallback(this, null);
        AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 5);
        AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 1);
        AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 2);
        AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 3);
        AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 4);
        this.mAwareSceneStateCallback = new AwareSceneStateCallback(this, null);
        AwareSceneRecognize.getInstance().registerStateCallback(this.mAwareSceneStateCallback, 1);
        this.mAwareBroadcastConfig.doinit();
    }

    public static void initBrCache(String name, Object ams) {
        if (!mIawareBrCaches.containsKey(name)) {
            mIawareBrCaches.put(name, new AwareBroadcastCache(name, ams));
        }
    }

    public boolean enqueueIawareProxyBroacast(boolean isParallel, HwBroadcastRecord r) {
        if (r == null) {
            return false;
        }
        if (r.isBg()) {
            this.mBgIawareBr.enqueueIawareProxyBroacast(isParallel, r);
        } else {
            this.mFgIawareBr.enqueueIawareProxyBroacast(isParallel, r);
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:9:0x0035, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:12:0x003b, code skipped:
            if (isForbidProxy(r6, r10) == false) goto L_0x003e;
     */
    /* JADX WARNING: Missing block: B:13:0x003d, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:15:0x0042, code skipped:
            if (isIawarePrepared() != false) goto L_0x0045;
     */
    /* JADX WARNING: Missing block: B:16:0x0044, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:18:0x0046, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean shouldIawareProxyBroadcast(String brAction, int callingPid, int receiverUid, int receiverPid, String recevierPkg) {
        synchronized (this.mIawareDownloadingUid) {
            if (this.mIawareDownloadingUid.contains(Integer.valueOf(receiverUid))) {
                if (AwareBroadcastDebug.getDebugDetail()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("iaware_br : downloading, don't proxy : ");
                    stringBuilder.append(recevierPkg);
                    stringBuilder.append(": action : ");
                    stringBuilder.append(brAction);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private boolean isIawarePrepared() {
        if (this.mFgIawareBr == null || this.mBgIawareBr == null) {
            return false;
        }
        return true;
    }

    private boolean isForbidProxy(String action, String pkg) {
        boolean noProxyAction;
        synchronized (this.mLockNoProxyActions) {
            noProxyAction = this.mIawareNoProxyActions.contains(action);
        }
        if (noProxyAction) {
            return true;
        }
        boolean noProxyPkg;
        synchronized (this.mLockNoProxyPkgs) {
            noProxyPkg = this.mIawareNoProxyPkgs.contains(pkg);
        }
        return noProxyPkg;
    }

    public boolean isProxyedAllowedCondition() {
        return this.mIawareScreenOn && this.mSpeedParallelStartProxy;
    }

    public boolean isNotProxySysPkg(String pkg, String action) {
        synchronized (this.mLockUnProxySys) {
            ArraySet<String> actions = (ArraySet) this.mIawareUnProxySys.get(pkg);
            if (actions != null) {
                boolean contains = actions.contains(action);
                return contains;
            }
            return false;
        }
    }

    public boolean isTrimAction(String action) {
        boolean contains;
        synchronized (this.mLockTrimActions) {
            contains = this.mIawareTrimActions.contains(action);
        }
        return contains;
    }

    public void updateXmlConfig() {
        if (AwareBroadcastDebug.getDebugDetail()) {
            AwareLog.i(TAG, "updateXmlConfig begin");
        }
        if (isIawarePrepared()) {
            synchronized (this.mLockNoProxyActions) {
                this.mIawareNoProxyActions = this.mAwareBroadcastConfig.getUnProxyActionList();
            }
            synchronized (this.mLockNoProxyPkgs) {
                this.mIawareNoProxyPkgs = this.mAwareBroadcastConfig.getUnProxyPkgList();
            }
            synchronized (this.mLockUnProxySys) {
                this.mIawareUnProxySys = this.mAwareBroadcastConfig.getUnProxySysList();
            }
            synchronized (this.mLockTrimActions) {
                this.mIawareTrimActions = this.mAwareBroadcastConfig.getTrimActionList();
            }
            this.mForegroundAppLevel = this.mAwareBroadcastConfig.getFGAppLevel();
            this.mNoTouchCheckCount = this.mAwareBroadcastConfig.getNoTouchCheckCount();
            this.mTouchCheckCount = this.mAwareBroadcastConfig.getTouchCheckCount();
            this.mBgIawareBr.setUnProxyMaxDuration(this.mAwareBroadcastConfig.getUnProxyMaxDuration());
            this.mBgIawareBr.setUnProxyMaxSpeed(this.mAwareBroadcastConfig.getUnProxyMaxSpeed());
            this.mBgIawareBr.setUnProxyMinSpeed(this.mAwareBroadcastConfig.getUnProxyMinSpeed());
            this.mBgIawareBr.setUnProxyMiddleSpeed(this.mAwareBroadcastConfig.getUnProxyMiddleSpeed());
            this.mBgIawareBr.setUnProxyHighSpeed(this.mAwareBroadcastConfig.getUnProxyHighSpeed());
            this.mFgIawareBr.setUnProxyMaxDuration(this.mAwareBroadcastConfig.getUnProxyMaxDuration());
            this.mFgIawareBr.setUnProxyMaxSpeed(this.mAwareBroadcastConfig.getUnProxyMaxSpeed());
            this.mFgIawareBr.setUnProxyMinSpeed(this.mAwareBroadcastConfig.getUnProxyMinSpeed());
            this.mFgIawareBr.setUnProxyMiddleSpeed(this.mAwareBroadcastConfig.getUnProxyMiddleSpeed());
            this.mFgIawareBr.setUnProxyHighSpeed(this.mAwareBroadcastConfig.getUnProxyHighSpeed());
            return;
        }
        AwareLog.e(TAG, "iaware process broacast don't prepared.");
    }

    public void iawareStartCountBroadcastSpeed(boolean isParallel, long dispatchClockTime, int size) {
        if (this.mIawareScreenOn && !this.mSpeedParallelStartProxy && isIawarePrepared() && isParallel) {
            checkParallCount(dispatchClockTime, size);
        }
    }

    public void endCheckCount() {
        if (isIawarePrepared()) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = 206;
            this.mHandler.sendMessage(msg);
        }
    }

    private void startUnproxyBroadcast() {
        if (isEmptyIawareBrList()) {
            this.mSpeedParallelStartProxy = false;
            return;
        }
        this.mBgIawareBr.starUnproxyBroadcast();
        this.mFgIawareBr.starUnproxyBroadcast();
    }

    private void checkParallCount(long dispatchClockTime, int size) {
        long j = dispatchClockTime;
        int i = size;
        this.mCountCheck = 0;
        int i2 = (this.mStartParallelBrTime > 0 ? 1 : (this.mStartParallelBrTime == 0 ? 0 : -1));
        int i3 = 10;
        long j2 = BROADCAST_PROXY_SPEED_INTERVAL_LONG;
        int tempIndex = 0;
        if (i2 == 0) {
            long[] jArr = this.mProxyCount[0];
            this.mStartParallelBrTime = j;
            jArr[0] = j;
            jArr = this.mProxyCount[0];
            long j3 = (long) i;
            this.mCountCheck = j3;
            jArr[1] = j3;
            for (i2 = 1; i2 < 10; i2++) {
                this.mProxyCount[i2][0] = this.mStartParallelBrTime + (((long) i2) * BROADCAST_PROXY_SPEED_INTERVAL_LONG);
                this.mProxyCount[i2][1] = 0;
            }
            setProxyCount();
            return;
        }
        this.mLastParallelBrTime = j;
        long tempPeriod = this.mLastParallelBrTime - this.mStartParallelBrTime;
        if (tempPeriod < 0) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.d(TAG, "iaware_br checkcount <0");
            }
            this.mStartParallelBrTime = 0;
            checkParallCount(dispatchClockTime, size);
        }
        if (tempPeriod >= 2000) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.d(TAG, "iaware_br checkcount >2000");
            }
            this.mStartParallelBrTime = 0;
            checkParallCount(dispatchClockTime, size);
        } else if (tempPeriod >= 0 && tempPeriod < 1000) {
            i2 = (int) (tempPeriod / BROADCAST_PROXY_SPEED_INTERVAL_LONG);
            this.mProxyCount[i2][1] = this.mProxyCount[i2][1] + ((long) i);
            while (true) {
                i3 = tempIndex;
                if (i3 <= i2) {
                    this.mCountCheck += this.mProxyCount[i3][1];
                    tempIndex = i3 + 1;
                } else {
                    setProxyCount();
                    return;
                }
            }
        } else if (tempPeriod >= 1000) {
            i2 = (int) ((tempPeriod - 1000) / BROADCAST_PROXY_SPEED_INTERVAL_LONG);
            int i4 = 9;
            if (i2 == 9) {
                this.mStartParallelBrTime = 0;
                checkParallCount(dispatchClockTime, size);
            } else if (i2 < 9) {
                this.mStartParallelBrTime = this.mProxyCount[i2 + 1][0];
                int rIndex = i2;
                int tempIndex2 = 0;
                while (tempIndex2 < i3) {
                    rIndex++;
                    if (rIndex < i3) {
                        this.mProxyCount[tempIndex2][0] = this.mProxyCount[rIndex][0];
                        this.mProxyCount[tempIndex2][1] = this.mProxyCount[rIndex][1];
                    } else if (tempIndex2 < i4) {
                        this.mProxyCount[tempIndex2][0] = this.mProxyCount[tempIndex2 - 1][0] + j2;
                        this.mProxyCount[tempIndex2][1] = 0;
                    } else {
                        this.mProxyCount[tempIndex2][0] = this.mProxyCount[tempIndex2 - 1][0] + j2;
                        this.mProxyCount[tempIndex2][1] = (long) i;
                    }
                    tempIndex2++;
                    i3 = 10;
                    j2 = BROADCAST_PROXY_SPEED_INTERVAL_LONG;
                    i4 = 9;
                }
                while (true) {
                    i3 = tempIndex;
                    if (i3 < 10) {
                        this.mCountCheck += this.mProxyCount[i3][1];
                        tempIndex = i3 + 1;
                    } else {
                        setProxyCount();
                        return;
                    }
                }
            }
        }
    }

    private void setProxyCount() {
        if (isStrictCondition()) {
            if (this.mCountCheck > ((long) this.mTouchCheckCount)) {
                this.mSpeedParallelStartProxy = true;
            }
        } else if (this.mCountCheck > ((long) this.mNoTouchCheckCount)) {
            this.mSpeedParallelStartProxy = true;
        }
    }

    public void reportSysEvent(int event, int eventType) {
        if (event == 15016) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_br install app: ");
                stringBuilder.append(eventType);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (eventType == 0) {
                this.mIawareInstallApp = true;
            } else if (eventType == 1) {
                this.mIawareInstallApp = false;
            } else {
                return;
            }
            if (this.mHandler.hasMessages(208)) {
                this.mHandler.removeMessages(208);
            }
            if (eventType == 0) {
                this.mHandler.sendEmptyMessageDelayed(208, 5000);
            }
        } else if (event == 20011) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.d(TAG, "iaware_br dev status event screen on");
            }
            this.mIawareScreenOn = true;
        } else if (event == 90011) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.d(TAG, "iaware_br dev status event screen off");
            }
            this.mIawareScreenOn = false;
            resetUnproxySpeedScreenOff();
        }
    }

    private void resetUnproxySpeedScreenOff() {
        if (isIawarePrepared()) {
            this.mBgIawareBr.setUnProxySpeedScreenOff();
            this.mFgIawareBr.setUnProxySpeedScreenOff();
        }
    }

    private void setIawarePolicy(int type, int event) {
        String str;
        StringBuilder stringBuilder;
        if (type != 2) {
            if (type == 4) {
                if (event == 1) {
                    this.mIawareProxyActivitStart = true;
                } else if (event == 0) {
                    this.mIawareProxyActivitStart = false;
                } else if (AwareBroadcastDebug.getDebugDetail()) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("don't process event ");
                    stringBuilder.append(event);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
        } else if (event == 1) {
            this.mIawareProxySlip = true;
        } else if (event == 0) {
            this.mIawareProxySlip = false;
        } else if (AwareBroadcastDebug.getDebugDetail()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("don't process event ");
            stringBuilder.append(event);
            AwareLog.d(str, stringBuilder.toString());
        }
    }

    public boolean isSpeedNoCtrol() {
        return isStrictCondition() ^ 1;
    }

    public boolean isScreenOff() {
        return this.mIawareScreenOn ^ 1;
    }

    private boolean isStrictCondition() {
        if (this.mIawareProxySlip || this.mIawareProxyActivitStart) {
            return true;
        }
        return false;
    }

    public boolean isEmptyIawareBrList() {
        return this.mBgIawareBr.getIawareBrSize() == 0 && this.mFgIawareBr.getIawareBrSize() == 0;
    }

    public void setStartProxy(boolean startProxy) {
        this.mSpeedParallelStartProxy = startProxy;
    }

    public boolean getStartProxy() {
        return this.mSpeedParallelStartProxy;
    }

    public void dumpIawareBr(PrintWriter pw) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("    feature enable :");
        stringBuilder2.append(BroadcastFeature.isFeatureEnabled(10));
        pw.println(stringBuilder2.toString());
        synchronized (this.mLockNoProxyActions) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("    Default no proxy actions :");
            stringBuilder.append(this.mIawareNoProxyActions);
            pw.println(stringBuilder.toString());
        }
        synchronized (this.mLockNoProxyPkgs) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("    Default no proxy pkgs :");
            stringBuilder2.append(this.mIawareNoProxyPkgs);
            pw.println(stringBuilder2.toString());
        }
        synchronized (this.mLockUnProxySys) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("    Default unproxy sys :");
            stringBuilder.append(this.mIawareUnProxySys);
            pw.println(stringBuilder.toString());
        }
        synchronized (this.mLockTrimActions) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("    Default trim action :");
            stringBuilder2.append(this.mIawareTrimActions);
            pw.println(stringBuilder2.toString());
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("    fg app level :");
        stringBuilder2.append(this.mForegroundAppLevel);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("    The receiver speed :");
        stringBuilder2.append(this.mCountCheck);
        pw.println(stringBuilder2.toString());
        ArraySet<Integer> iawareDownloadingUid = new ArraySet();
        ArrayList<String> iawareDownloadingPkgs = new ArrayList();
        synchronized (this.mIawareDownloadingUid) {
            iawareDownloadingUid.addAll(this.mIawareDownloadingUid);
        }
        if (iawareDownloadingUid.size() > 0) {
            PackageManagerService pms = (PackageManagerService) ServiceManager.getService("package");
            Iterator it = iawareDownloadingUid.iterator();
            while (it.hasNext()) {
                String name = pms.getNameForUid(((Integer) it.next()).intValue());
                if (name != null) {
                    iawareDownloadingPkgs.add(name);
                }
            }
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("    App Downloading:");
        stringBuilder3.append(iawareDownloadingPkgs);
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("    Screen:");
        stringBuilder3.append(this.mIawareScreenOn ? PreciseIgnore.COMP_SCREEN_ON_VALUE_ : "off");
        pw.println(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("    Operation: [");
        stringBuilder3.append(this.mIawareProxySlip ? "slip" : "");
        stringBuilder3.append(" ");
        stringBuilder3.append(this.mIawareProxyActivitStart ? "activityStart" : "");
        stringBuilder3.append("]");
        pw.println(stringBuilder3.toString());
        pw.println("    Proxy info:");
        this.mBgIawareBr.dump(pw);
        this.mFgIawareBr.dump(pw);
    }

    public void notifyIawareUnproxyBr(int pid, int uid) {
        if (isIawarePrepared()) {
            this.mBgIawareBr.startUnproxyFgAppBroadcast(pid, uid);
            this.mFgIawareBr.startUnproxyFgAppBroadcast(pid, uid);
        }
    }

    public int getForegroundAppLevel() {
        return this.mForegroundAppLevel;
    }

    public boolean isInstallApp() {
        return this.mIawareInstallApp;
    }

    public boolean assemFilterBr(Intent intent, IntentFilter filter) {
        if (intent == null || filter == null) {
            return false;
        }
        boolean realFilter = false;
        Iterator<ActionFilterEntry> it = filter.actionFilterIterator();
        if (it != null) {
            while (it.hasNext()) {
                ActionFilterEntry actionFilter = (ActionFilterEntry) it.next();
                if (actionFilter.getAction() != null && actionFilter.getAction().equals(intent.getAction())) {
                    String filterName = actionFilter.getFilterName();
                    String filterValue = actionFilter.getFilterValue();
                    if (filterName == null) {
                        continue;
                    } else if (filterValue != null) {
                        FilterStatus filterStatus = (FilterStatus) this.mIawareFilterStatus.get(filterName);
                        if (filterStatus != null) {
                            realFilter = filterStatus.filter(intent, filterValue);
                            if (AwareBroadcastDebug.getFilterDebug()) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("iaware_br, filterValue: ");
                                stringBuilder.append(filterValue);
                                stringBuilder.append(", realFilter : ");
                                stringBuilder.append(realFilter);
                                stringBuilder.append(", id:");
                                stringBuilder.append(filter);
                                AwareLog.i(str, stringBuilder.toString());
                            }
                            if (!realFilter) {
                                return realFilter;
                            }
                        }
                    }
                } else if (AwareBroadcastDebug.getFilterDebug()) {
                    AwareLog.w(TAG, "iaware_br, action not match. ");
                }
            }
        }
        return realFilter;
    }

    private State getWifiNetworkStatus(Intent intent) {
        NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
        if (info != null) {
            return info.getState();
        }
        return State.UNKNOWN;
    }

    private int getConnectNetworkStatus(Intent intent) {
        int type = intent.getIntExtra("networkType", -1);
        if (type != 0 && type != 1) {
            return -1;
        }
        NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
        if (info == null) {
            return -1;
        }
        if (type == 0) {
            if (info.isConnected()) {
                return 1;
            }
            return 2;
        } else if (info.isConnected()) {
            return 3;
        } else {
            return 4;
        }
    }

    private int getWifiStatus(Intent intent) {
        return intent.getIntExtra("wifi_state", 4);
    }

    private int getWifiRssi(Intent intent) {
        return intent.getIntExtra("newRssi", -127);
    }

    private SupplicantState getWifiSupStatus(Intent intent) {
        SupplicantState state = (SupplicantState) intent.getParcelableExtra("newState");
        if (state != null) {
            return state;
        }
        return SupplicantState.INVALID;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void getStateFromSendBr(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                boolean z;
                switch (action.hashCode()) {
                    case -2128145023:
                        if (action.equals("android.intent.action.SCREEN_OFF")) {
                            z = true;
                            break;
                        }
                    case -1886648615:
                        if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                            z = true;
                            break;
                        }
                    case -1875733435:
                        if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                            z = true;
                            break;
                        }
                    case -1538406691:
                        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                            z = false;
                            break;
                        }
                    case -1454123155:
                        if (action.equals("android.intent.action.SCREEN_ON")) {
                            z = true;
                            break;
                        }
                    case -1172645946:
                        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                            z = true;
                            break;
                        }
                    case -385684331:
                        if (action.equals("android.net.wifi.RSSI_CHANGED")) {
                            z = true;
                            break;
                        }
                    case -343630553:
                        if (action.equals("android.net.wifi.STATE_CHANGE")) {
                            z = true;
                            break;
                        }
                    case 233521600:
                        if (action.equals("android.net.wifi.supplicant.STATE_CHANGE")) {
                            z = true;
                            break;
                        }
                    case 1019184907:
                        if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
                            z = true;
                            break;
                        }
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        this.mBatteryLevel = intent.getIntExtra(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL, -1);
                        this.mPrePlugedtype = this.mPlugedtype;
                        this.mPlugedtype = intent.getIntExtra("plugged", -1);
                        break;
                    case true:
                        this.mCharging = 1;
                        break;
                    case true:
                        this.mCharging = 0;
                        break;
                    case true:
                        this.mWifiNetStatue = getWifiNetworkStatus(intent);
                        break;
                    case true:
                        this.mConnectStatus = getConnectNetworkStatus(intent);
                        break;
                    case true:
                        this.mWifiStatue = getWifiStatus(intent);
                        break;
                    case true:
                        this.mWifiSupStatue = getWifiSupStatus(intent);
                        break;
                    case true:
                        this.mWifiRssi = getWifiRssi(intent);
                        break;
                    case true:
                        this.mScreenOn = true;
                        break;
                    case true:
                        this.mScreenOn = false;
                        break;
                }
            }
        }
    }

    private void initState() {
        if (this.mContext != null) {
            BatteryManager batteryManager = (BatteryManager) this.mContext.getSystemService("batterymanager");
            Context context = this.mContext;
            Context context2 = this.mContext;
            WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
            ConnectivityManager connectManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            if (batteryManager != null) {
                if (batteryManager.isCharging()) {
                    this.mCharging = 1;
                } else {
                    this.mCharging = 0;
                }
            }
            if (wifiManager != null) {
                this.mWifiStatue = wifiManager.getWifiState();
            }
            if (connectManager != null) {
                NetworkInfo connectInfo = connectManager.getActiveNetworkInfo();
                if (connectInfo != null && connectInfo.isConnected()) {
                    if (connectInfo.getType() == 1) {
                        this.mWifiNetStatue = State.CONNECTED;
                        this.mConnectStatus = 3;
                    } else if (connectInfo.getType() == 0) {
                        this.mConnectStatus = 1;
                    }
                }
            }
        }
    }

    public int filterBr(Intent intent, AwareProcessInfo pInfo) {
        if (intent == null || pInfo == null) {
            return BrCtrlType.NONE.ordinal();
        }
        String action = intent.getAction();
        if (action == null) {
            return BrCtrlType.NONE.ordinal();
        }
        BrFilterPolicy brfilterPolicy = pInfo.getBrPolicy(intent);
        if (brfilterPolicy == null) {
            return BrCtrlType.NONE.ordinal();
        }
        if (AwareBroadcastDebug.getFilterDebug()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brFilter brfilterPolicy.getPolicy() : ");
            stringBuilder.append(brfilterPolicy.getPolicy());
            AwareLog.i(str, stringBuilder.toString());
        }
        if (brfilterPolicy.getPolicy() == BrCtrlType.DATADEFAULTBR.ordinal()) {
            Object obj = -1;
            int hashCode = action.hashCode();
            if (hashCode != -1875733435) {
                if (hashCode != -1538406691) {
                    if (hashCode != -343630553) {
                        if (hashCode == 233521600 && action.equals("android.net.wifi.supplicant.STATE_CHANGE")) {
                            obj = 2;
                        }
                    } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                        obj = null;
                    }
                } else if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                    obj = 3;
                }
            } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                obj = 1;
            }
            switch (obj) {
                case null:
                    return getWifiNetPolicy(action);
                case 1:
                    return getWifiPolicy(action);
                case 2:
                    return getWifiSupPolicy(action);
                case 3:
                    return getBatteryPolicy(action);
            }
        }
        return brfilterPolicy.getPolicy();
    }

    private int getWifiNetPolicy(String action) {
        int policy = BrCtrlType.NONE.ordinal();
        int dataPolicy = BroadcastExFeature.getBrFilterPolicy(action);
        if (dataPolicy == 1) {
            if (this.mWifiNetStatue == State.CONNECTED || this.mWifiNetStatue == State.DISCONNECTED) {
                return policy;
            }
            return BrCtrlType.DISCARDBR.ordinal();
        } else if (dataPolicy == 2) {
            return BrCtrlType.DISCARDBR.ordinal();
        } else {
            return policy;
        }
    }

    private int getWifiPolicy(String action) {
        int policy = BrCtrlType.NONE.ordinal();
        int dataPolicy = BroadcastExFeature.getBrFilterPolicy(action);
        if (dataPolicy == 1) {
            if (this.mWifiStatue == 3 || this.mWifiStatue == 1) {
                return policy;
            }
            return BrCtrlType.DISCARDBR.ordinal();
        } else if (dataPolicy == 2) {
            return BrCtrlType.DISCARDBR.ordinal();
        } else {
            return policy;
        }
    }

    private int getWifiSupPolicy(String action) {
        int policy = BrCtrlType.NONE.ordinal();
        int dataPolicy = BroadcastExFeature.getBrFilterPolicy(action);
        if (dataPolicy == 1) {
            if (this.mWifiSupStatue == SupplicantState.COMPLETED || this.mWifiSupStatue == SupplicantState.DISCONNECTED) {
                return policy;
            }
            return BrCtrlType.DISCARDBR.ordinal();
        } else if (dataPolicy == 2) {
            return BrCtrlType.DISCARDBR.ordinal();
        } else {
            return policy;
        }
    }

    private int getBatteryPolicy(String action) {
        int policy = BrCtrlType.NONE.ordinal();
        if (BroadcastExFeature.getBrFilterPolicy(action) == 1 && this.mPrePlugedtype == this.mPlugedtype) {
            return BrCtrlType.DISCARDBR.ordinal();
        }
        return policy;
    }

    public boolean awareTrimAndEnqueueBr(boolean isParallel, HwBroadcastRecord r, boolean notify, int pid, String pkgName) {
        if (r == null) {
            return false;
        }
        AwareBroadcastCache brCache = (AwareBroadcastCache) mIawareBrCaches.get(r.getBrQueueName());
        if (brCache != null) {
            return brCache.awareTrimAndEnqueueBr(isParallel, r, notify, pid, pkgName, this);
        }
        return false;
    }

    private void unproxyCacheBr(int pid) {
        for (Entry<String, AwareBroadcastCache> ent : mIawareBrCaches.entrySet()) {
            ((AwareBroadcastCache) ent.getValue()).unproxyCacheBr(pid);
        }
    }

    public void clearCacheBr(int pid) {
        for (Entry<String, AwareBroadcastCache> ent : mIawareBrCaches.entrySet()) {
            ((AwareBroadcastCache) ent.getValue()).clearCacheBr(pid);
        }
    }

    public void updateProcessBrPolicy(AwareProcessInfo info, int state) {
        if (info != null) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = 209;
            msg.arg1 = state;
            msg.obj = info;
            this.mHandler.sendMessage(msg);
        }
    }

    private void updateAppIdleStat(int idleStat, int userId, String packageName) {
        ArrayList<AwareProcessInfo> procList = ProcessInfoCollector.getInstance().getAwareProcessInfosFromPackage(packageName, userId);
        if (AwareBroadcastDebug.getFilterDebug()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brFilter appidle:");
            stringBuilder.append(idleStat);
            stringBuilder.append(", packageName:");
            stringBuilder.append(packageName);
            stringBuilder.append(", userId:");
            stringBuilder.append(userId);
            AwareLog.i(str, stringBuilder.toString());
        }
        int count = procList.size();
        for (int i = 0; i < count; i++) {
            AwareProcessInfo info = (AwareProcessInfo) procList.get(i);
            if (idleStat == 1) {
                ProcessInfoCollector.getInstance().setAwareProcessState(info.mPid, info.mProcInfo.mUid, 3);
            } else {
                ProcessInfoCollector.getInstance().setAwareProcessState(info.mPid, info.mProcInfo.mUid, 4);
            }
        }
    }

    private void updateProcessPolicy(int stateType, int eventType, int pid, int uid) {
        if (stateType != 5) {
            if (AwareBroadcastDebug.getFilterDebug()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brFilter process status:");
                stringBuilder.append(stateType);
                stringBuilder.append(", eventType:");
                stringBuilder.append(eventType);
                stringBuilder.append(", pid:");
                stringBuilder.append(pid);
                stringBuilder.append(", uid:");
                stringBuilder.append(uid);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (pid > 0) {
                ProcessInfoCollector.getInstance().setAwareProcessState(pid, uid, -1);
            } else {
                ProcessInfoCollector.getInstance().setAwareProcessStateByUid(pid, uid, -1);
            }
        }
    }

    public void reportGoogleConn(boolean conn) {
        Message msg = this.mHandler.obtainMessage();
        msg.what = 210;
        if (conn) {
            msg.arg1 = 1;
        } else {
            msg.arg1 = -1;
        }
        this.mHandler.sendMessage(msg);
    }

    private void resetGoogleConnStat(int connStat) {
        boolean conn = false;
        if (connStat == 1) {
            conn = true;
        }
        if (mGoogleConnStat != conn) {
            mGoogleConnStat = conn;
            if (AwareBroadcastDebug.getFilterDebug()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brFilter google conn stat change:");
                stringBuilder.append(mGoogleConnStat);
                AwareLog.i(str, stringBuilder.toString());
            }
            ArraySet<String> googleAppPkgs = BroadcastExFeature.getBrGoogleAppList();
            int count = googleAppPkgs.size();
            for (int i = 0; i < count; i++) {
                ArrayList<AwareProcessInfo> procList = ProcessInfoCollector.getInstance().getAwareProcessInfosFromPackage((String) googleAppPkgs.valueAt(i), -1);
                int countProc = procList.size();
                for (int j = 0; j < countProc; j++) {
                    AwareProcessInfo info = (AwareProcessInfo) procList.get(j);
                    ProcessInfoCollector.getInstance().setAwareProcessState(info.mPid, info.mProcInfo.mUid, -1);
                }
            }
        }
    }

    private void updateBrPolicy(int state, Object obj) {
        if (obj instanceof AwareProcessInfo) {
            AwareProcessInfo info = (AwareProcessInfo) obj;
            info.updateBrPolicy();
            if (state == 2 || state == 4 || state == 8 || state == 6) {
                int currentState = info.getState();
                if (currentState == 10 || currentState == 9) {
                    unproxyCacheBr(info.mPid);
                }
            }
        }
    }

    public static int getGoogleConnStat() {
        if (mGoogleConnStat) {
            return 1;
        }
        return -1;
    }

    public void notifyOverFlow(int pid) {
        if (AwareBroadcastDebug.getFilterDebug()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brFilter notifyOverFlow: ");
            stringBuilder.append(pid);
            AwareLog.i(str, stringBuilder.toString());
        }
        Message msg = this.mHandler.obtainMessage();
        msg.what = 212;
        msg.arg1 = pid;
        this.mHandler.sendMessage(msg);
    }

    public void dumpIawareFilterBr(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("    BrFilterfeature enable :");
        stringBuilder.append(BroadcastExFeature.isFeatureEnabled(1));
        pw.println(stringBuilder.toString());
        pw.println("    process policy :");
        ArrayList<AwareProcessInfo> listProcess = ProcessInfoCollector.getInstance().getAwareProcessInfoList();
        int size = listProcess.size();
        for (int i = 0; i < size; i++) {
            AwareProcessInfo info = (AwareProcessInfo) listProcess.get(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("      process pid : ");
            stringBuilder2.append(info.mPid);
            stringBuilder2.append(", process name : ");
            stringBuilder2.append(info.mProcInfo.mProcessName);
            stringBuilder2.append(", process adj:");
            stringBuilder2.append(info.mProcInfo.mCurAdj);
            pw.println(stringBuilder2.toString());
            for (Entry<String, BrFilterPolicy> ent : info.getBrFilterPolicyMap().entrySet()) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("         policy action : ");
                stringBuilder3.append((String) ent.getKey());
                stringBuilder3.append(", value : ");
                stringBuilder3.append(((BrFilterPolicy) ent.getValue()).getPolicy());
                stringBuilder3.append(", state : ");
                stringBuilder3.append(((BrFilterPolicy) ent.getValue()).getProcessState());
                pw.println(stringBuilder3.toString());
            }
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    br before filter count :");
        stringBuilder4.append(AwareBroadcastDumpRadar.getBrBeforeCount());
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    br after filter count :");
        stringBuilder4.append(AwareBroadcastDumpRadar.getBrAfterCount());
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    br noprocess filter count :");
        stringBuilder4.append(AwareBroadcastDumpRadar.getBrNoProcessCount());
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    br system_server nodrop count :");
        stringBuilder4.append(AwareBroadcastDumpRadar.getSsNoDropCount());
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    br persistent app nodrop count :");
        stringBuilder4.append(AwareBroadcastDumpRadar.getPerAppNoDropCount());
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    Default white list :");
        stringBuilder4.append(BroadcastExFeature.getBrFilterWhiteList());
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    Default white actionAPP :");
        stringBuilder4.append(BroadcastExFeature.getBrFilterWhiteApp());
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    Default black actionAPP :");
        stringBuilder4.append(BroadcastExFeature.getBrFilterBlackApp());
        pw.println(stringBuilder4.toString());
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("    google app list :");
        stringBuilder4.append(BroadcastExFeature.getBrGoogleAppList());
        pw.println(stringBuilder4.toString());
        AwareBroadcastDumpRadar radar = MultiTaskManagerService.self().getIawareBrRadar();
        if (radar != null) {
            pw.println("    brfilter detail :");
            for (Entry<String, Integer> ent2 : radar.getBrFilterDetail().entrySet()) {
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("         ");
                stringBuilder5.append((String) ent2.getKey());
                stringBuilder5.append(", count , ");
                stringBuilder5.append(ent2.getValue());
                pw.println(stringBuilder5.toString());
            }
        }
    }
}
