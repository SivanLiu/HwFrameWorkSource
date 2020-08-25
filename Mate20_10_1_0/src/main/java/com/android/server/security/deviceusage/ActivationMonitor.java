package com.android.server.security.deviceusage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.util.List;

public class ActivationMonitor {
    private static final String ACTION_CHECK_POINT = "com.android.server.security.deviceusage.ACTION_CHECK_POINT";
    public static final long ACTIVATION_TIME = 21600000;
    private static final long CHECK_POINT_GAP = 3600000;
    private static final long CONNECTION_TIME_DEFAULT = 0;
    /* access modifiers changed from: private */
    public static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    /* access modifiers changed from: private */
    public static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", MemoryConstant.MEM_SCENE_DEFAULT));
    private static final int MINIMUM_SSID_LENGTH = 2;
    private static final int MSG_START_MONITOR = 10;
    private static final int NETWORK_NONE = -1;
    private static final int NETWORK_WIFI = 1;
    private static final String PERMISSION = "com.huawei.permission.MANAGE_USE_SECURITY";
    private static final String SETTINGS_KEY_CONNECTION_TIME = "connection_time_for_activation";
    private static final long START_DELAY = 60000;
    private static final String TAG = "ActivationMonitor";
    private long mActivationDuration = ACTIVATION_TIME;
    /* access modifiers changed from: private */
    public BroadcastReceiver mCheckPointReceiver = new BroadcastReceiver() {
        /* class com.android.server.security.deviceusage.ActivationMonitor.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                Slog.e(ActivationMonitor.TAG, "mCheckPointReceiver intent or action is null!");
            } else if (ActivationMonitor.ACTION_CHECK_POINT.equals(intent.getAction())) {
                if (ActivationMonitor.IS_HW_DEBUG) {
                    Slog.i(ActivationMonitor.TAG, "mCheckPointReceiver onReceive onNetStatusChanged.");
                }
                ActivationMonitor.this.onNetStatusChanged(ActivationMonitor.getNetWorkState(context));
            }
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mHasSimCard = false;
    /* access modifiers changed from: private */
    public IntegrateReceiver mIntegrateReceiver = new IntegrateReceiver();
    /* access modifiers changed from: private */
    public boolean mIsBroadcastRegistered = false;
    private boolean mIsNetworkConnected;
    private boolean mIsWifiOnly = false;
    private long mLastCheckTime;
    private AlarmManager.OnAlarmListener mNetChangeListener = new AlarmManager.OnAlarmListener() {
        /* class com.android.server.security.deviceusage.ActivationMonitor.AnonymousClass3 */

        public void onAlarm() {
            if (ActivationMonitor.IS_HW_DEBUG) {
                Slog.i(ActivationMonitor.TAG, "mNetChangeListener onAlarm Report Activation.");
            }
            ActivationRecorder.record();
            ActivationRecorder.report(ActivationMonitor.this.mContext);
            if (ActivationMonitor.this.mIsBroadcastRegistered) {
                if (ActivationMonitor.IS_TABLET) {
                    ActivationMonitor.this.mContext.unregisterReceiver(ActivationMonitor.this.mCheckPointReceiver);
                }
                ActivationMonitor.this.mContext.unregisterReceiver(ActivationMonitor.this.mIntegrateReceiver);
                boolean unused = ActivationMonitor.this.mIsBroadcastRegistered = false;
            }
            if (ActivationMonitor.IS_TABLET) {
                ActivationMonitor.this.cancelCheckPerHour();
            }
            ActivationMonitor.this.mHandler.removeMessages(10);
        }
    };
    private PendingIntent mPendingIntent;
    private AlarmManager.OnAlarmListener mSimChangeListener = new AlarmManager.OnAlarmListener() {
        /* class com.android.server.security.deviceusage.ActivationMonitor.AnonymousClass2 */

        public void onAlarm() {
            if (ActivationMonitor.IS_HW_DEBUG) {
                Slog.i(ActivationMonitor.TAG, "mSimChangeListener onAlarm Report Activation.");
            }
            ActivationRecorder.record();
            ActivationRecorder.report(ActivationMonitor.this.mContext);
            if (ActivationMonitor.this.mIsBroadcastRegistered) {
                if (ActivationMonitor.IS_TABLET) {
                    ActivationMonitor.this.mContext.unregisterReceiver(ActivationMonitor.this.mCheckPointReceiver);
                }
                ActivationMonitor.this.mContext.unregisterReceiver(ActivationMonitor.this.mIntegrateReceiver);
                boolean unused = ActivationMonitor.this.mIsBroadcastRegistered = false;
            }
            if (ActivationMonitor.IS_TABLET) {
                ActivationMonitor.this.cancelCheckPerHour();
            }
            ActivationMonitor.this.mHandler.removeMessages(10);
        }
    };

    public ActivationMonitor(Context context) {
        this.mContext = context;
    }

    public void start() {
        if (!ActivationRecorder.isActivated()) {
            if (this.mHandlerThread == null) {
                this.mHandlerThread = new HandlerThread(TAG);
                this.mHandlerThread.start();
            }
            if (this.mHandler == null) {
                this.mHandler = new MyHandler(this.mHandlerThread.getLooper());
            }
            this.mHandler.sendEmptyMessageDelayed(10, 60000);
            this.mIsWifiOnly = isWifiOnly();
        } else if (IS_HW_DEBUG) {
            Slog.i(TAG, "Device already activated.");
        }
    }

    public void restart(long duration) {
        if (duration <= ACTIVATION_TIME && duration >= 0) {
            this.mActivationDuration = duration;
            if (IS_HW_DEBUG) {
                Slog.i(TAG, "reStart : " + this.mActivationDuration);
            }
            if (this.mHandlerThread == null) {
                this.mHandlerThread = new HandlerThread(TAG);
                this.mHandlerThread.start();
            }
            if (this.mHandler == null) {
                this.mHandler = new MyHandler(this.mHandlerThread.getLooper());
            }
            this.mHandler.sendEmptyMessage(10);
            this.mIsWifiOnly = isWifiOnly();
        }
    }

    public boolean isActivated() {
        return ActivationRecorder.isActivated();
    }

    public void resetActivation() {
        HwOemInfoUtil.resetActivation();
    }

    private void saveConnectionTime(long time) {
        if (!Settings.Global.putLong(this.mContext.getContentResolver(), SETTINGS_KEY_CONNECTION_TIME, time)) {
            Slog.e(TAG, "write connection time to settings failed!");
        } else if (IS_HW_DEBUG) {
            Slog.i(TAG, "mConnection Time Saved: " + time);
        }
    }

    private long getConnectionTime() {
        return Settings.Global.getLong(this.mContext.getContentResolver(), SETTINGS_KEY_CONNECTION_TIME, 0);
    }

    /* access modifiers changed from: private */
    public void countDownIfHasSim() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (telephonyManager == null || alarmManager == null) {
            Slog.e(TAG, "SIM state cannot be detected");
            return;
        }
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "SIM state is " + telephonyManager.getSimState());
        }
        if (telephonyManager.getSimState() == 1 || this.mIsWifiOnly) {
            alarmManager.cancel(this.mSimChangeListener);
            this.mHasSimCard = false;
            if (IS_HW_DEBUG) {
                Slog.i(TAG, "SIM removed, count down stop");
            }
        } else if (!this.mHasSimCard) {
            alarmManager.set(2, this.mActivationDuration + SystemClock.elapsedRealtime(), "ACTIVATION_COUNTING", this.mSimChangeListener, this.mHandler);
            this.mHasSimCard = true;
            if (IS_HW_DEBUG) {
                Slog.i(TAG, "SIM inserted, count down start");
            }
        }
    }

    /* access modifiers changed from: private */
    public void checkNetworkPerHour() {
        if (((ConnectivityManager) this.mContext.getSystemService("connectivity")) == null) {
            Slog.e(TAG, "Network state cannot be detected");
            return;
        }
        int currentNetState = getNetWorkState(this.mContext);
        this.mIsNetworkConnected = currentNetState != -1;
        long currentTimePoint = SystemClock.elapsedRealtime();
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "checkNetworkPerHour currentNetState is " + currentNetState + ", currentTimePoint is " + currentTimePoint);
        }
        Intent intent = new Intent(ACTION_CHECK_POINT);
        intent.setPackage(this.mContext.getPackageName());
        this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
        ((AlarmManager) this.mContext.getSystemService("alarm")).setInexactRepeating(3, currentTimePoint, 3600000, this.mPendingIntent);
    }

    /* access modifiers changed from: private */
    public void cancelCheckPerHour() {
        if (this.mPendingIntent != null) {
            ((AlarmManager) this.mContext.getSystemService("alarm")).cancel(this.mPendingIntent);
        }
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "cancelCheckPerHour complete");
        }
    }

    private class MyHandler extends Handler {
        private MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 10) {
                if (!ActivationMonitor.this.mIsBroadcastRegistered) {
                    if (ActivationMonitor.IS_HW_DEBUG) {
                        Slog.i(ActivationMonitor.TAG, "start monitor.");
                    }
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED);
                    if (ActivationMonitor.IS_TABLET) {
                        intentFilter.addAction(SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE);
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(ActivationMonitor.ACTION_CHECK_POINT);
                        ActivationMonitor.this.mContext.registerReceiver(ActivationMonitor.this.mCheckPointReceiver, filter, ActivationMonitor.PERMISSION, null);
                    }
                    ActivationMonitor.this.mContext.registerReceiver(ActivationMonitor.this.mIntegrateReceiver, intentFilter);
                    boolean unused = ActivationMonitor.this.mIsBroadcastRegistered = true;
                }
                ActivationMonitor.this.countDownIfHasSim();
                if (ActivationMonitor.IS_TABLET) {
                    ActivationMonitor.this.checkNetworkPerHour();
                }
                removeMessages(10);
            }
        }
    }

    /* access modifiers changed from: private */
    public class IntegrateReceiver extends BroadcastReceiver {
        static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";

        private IntegrateReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                Slog.e(ActivationMonitor.TAG, "Intent or Action is null");
                return;
            }
            String action = intent.getAction();
            char c = 65535;
            int hashCode = action.hashCode();
            if (hashCode != -1172645946) {
                if (hashCode == -229777127 && action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    c = 0;
                }
            } else if (action.equals(SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE)) {
                c = 1;
            }
            if (c == 0) {
                ActivationMonitor.this.countDownIfHasSim();
            } else if (c != 1) {
                Slog.w(ActivationMonitor.TAG, "Unidentified broadcast!");
            } else {
                ActivationMonitor.this.onNetStatusChanged(ActivationMonitor.getNetWorkState(context));
            }
        }
    }

    private static class ActivationRecorder {
        private static final String ACTION_WARRANTY_DETECTED = "com.huawei.action.ACTION_ACTIVATION_DETECTED";
        private static final String RECEIVER_CLASS_NAME = "com.huawei.android.hwouc.biz.impl.reveiver.DeviceActivatedReceiver";
        private static final String RECEIVER_PACKAGE_NAME = "com.huawei.android.hwouc";
        private static final String RECEIVE_WARRANTY_REPORT = "com.huawei.permission.RECEIVE_ACTIVATION_REPORT";

        private ActivationRecorder() {
        }

        static void record() {
            HwOemInfoUtil.setActivated();
        }

        static boolean isActivated() {
            return HwOemInfoUtil.getActivationStatus();
        }

        static void report(Context context) {
            Intent intent = new Intent(ACTION_WARRANTY_DETECTED);
            intent.setClassName(RECEIVER_PACKAGE_NAME, RECEIVER_CLASS_NAME);
            context.sendBroadcast(intent, RECEIVE_WARRANTY_REPORT);
        }
    }

    private void checkIfActivateByTime(long totalTime) {
        if (totalTime >= this.mActivationDuration) {
            if (IS_HW_DEBUG) {
                Slog.i(TAG, "checkIfActivateByTime Report Activation.");
            }
            ActivationRecorder.record();
            ActivationRecorder.report(this.mContext);
            cancelCheckPerHour();
            this.mHandler.post(new Runnable() {
                /* class com.android.server.security.deviceusage.ActivationMonitor.AnonymousClass4 */

                public void run() {
                    if (ActivationMonitor.this.mIsBroadcastRegistered) {
                        if (ActivationMonitor.IS_TABLET) {
                            ActivationMonitor.this.mContext.unregisterReceiver(ActivationMonitor.this.mCheckPointReceiver);
                        }
                        ActivationMonitor.this.mContext.unregisterReceiver(ActivationMonitor.this.mIntegrateReceiver);
                        boolean unused = ActivationMonitor.this.mIsBroadcastRegistered = false;
                    }
                }
            });
            this.mHandler.removeMessages(10);
        }
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "checkIfActivateByTime complete.");
        }
    }

    /* access modifiers changed from: private */
    public void onNetStatusChanged(int networkState) {
        long totalConnectionTime = getConnectionTime();
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "onNetStatusChanged totalConnectionTime is " + totalConnectionTime);
        }
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (alarmManager == null) {
            Slog.e(TAG, "onNetStatusChanged alarmManager is null!");
        } else if (networkState == 1 && !checkIfCurrentWifiHasPassword()) {
            if (IS_HW_DEBUG) {
                Slog.i(TAG, "onNetStatusChanged Free Wifi Connected!");
            }
            if (!this.mIsNetworkConnected) {
                checkIfActivateByTime(totalConnectionTime);
                return;
            }
            onNetDisconnected(totalConnectionTime, this.mLastCheckTime);
            this.mIsNetworkConnected = false;
            alarmManager.cancel(this.mNetChangeListener);
        } else if (handleNetworkState(alarmManager, networkState, totalConnectionTime)) {
            checkIfActivateByTime(getConnectionTime());
        }
    }

    private boolean handleNetworkState(AlarmManager alarmManager, int networkState, long totalConnectionTime) {
        if (!this.mIsNetworkConnected) {
            if (networkState != -1) {
                this.mLastCheckTime = SystemClock.elapsedRealtime();
                this.mIsNetworkConnected = true;
                onNetConnected(alarmManager, totalConnectionTime, this.mLastCheckTime);
                return false;
            }
        } else if (networkState == -1) {
            onNetDisconnected(totalConnectionTime, this.mLastCheckTime);
            this.mIsNetworkConnected = false;
            alarmManager.cancel(this.mNetChangeListener);
        } else {
            long currentCheckTime = SystemClock.elapsedRealtime();
            onNetStable(totalConnectionTime, currentCheckTime, this.mLastCheckTime);
            this.mLastCheckTime = currentCheckTime;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public static int getNetWorkState(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager == null) {
            Slog.e(TAG, "getNetWorkState connectivityManager is null!");
            return -1;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            int type = activeNetworkInfo.getType();
            if (type == 0) {
                return 0;
            }
            if (type != 1) {
                return -1;
            }
            return 1;
        }
        return -1;
    }

    private boolean checkIfCurrentWifiHasPassword() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
        if (wifiManager == null || wifiManager.getConnectionInfo() == null) {
            Slog.e(TAG, "checkIfCurrentWifiHasPassword wifiManager is null!");
            return true;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        List<WifiConfiguration> wifiConfigurations = wifiManager.getConfiguredNetworks();
        String currentSsid = wifiInfo.getSSID();
        if (currentSsid == null || currentSsid.length() <= 2) {
            return true;
        }
        String currentSsid2 = currentSsid.replace("\"", "");
        if (wifiConfigurations == null || wifiConfigurations.size() == 0) {
            return true;
        }
        int size = wifiConfigurations.size();
        for (int i = 0; i < size; i++) {
            WifiConfiguration configuration = wifiConfigurations.get(i);
            if (configuration != null && configuration.status == 0) {
                String ssid = null;
                if (!TextUtils.isEmpty(configuration.SSID)) {
                    ssid = configuration.SSID.replace("\"", "");
                }
                if (currentSsid2.equalsIgnoreCase(ssid)) {
                    return true ^ configuration.allowedKeyManagement.get(0);
                }
            }
        }
        return true;
    }

    private void onNetConnected(AlarmManager alarmManager, long totalConnectionTime, long lastCheckTime) {
        long j = this.mActivationDuration;
        long timeRemaining = j - totalConnectionTime;
        if (timeRemaining <= 0) {
            checkIfActivateByTime(j);
            return;
        }
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "onNetStatusChanged Connection established! mLastCheckTime is " + lastCheckTime + ", timeRemaining is " + timeRemaining);
        }
        alarmManager.set(2, SystemClock.elapsedRealtime() + timeRemaining, "ACTIVATION_COUNTING", this.mNetChangeListener, this.mHandler);
    }

    private void onNetDisconnected(long totalConnectionTime, long lastCheckTime) {
        long currentCheckTime = SystemClock.elapsedRealtime();
        long connectionTime = (currentCheckTime - lastCheckTime) + totalConnectionTime;
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "onNetStatusChanged Connection down! currentCheckTime is " + currentCheckTime + ", totalConnectionTime is " + connectionTime);
        }
        saveConnectionTime(connectionTime);
    }

    private void onNetStable(long totalConnectionTime, long currentCheckTime, long lastCheckTime) {
        long connectionTime = (currentCheckTime - lastCheckTime) + totalConnectionTime;
        if (IS_HW_DEBUG) {
            Slog.i(TAG, "onNetStatusChanged Connection steady, currentCheckTime is " + currentCheckTime + ", totalConnectionTime is " + connectionTime);
        }
        saveConnectionTime(connectionTime);
    }

    private boolean isWifiOnly() {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (cm != null) {
            return !cm.isNetworkSupported(0);
        }
        return false;
    }
}
