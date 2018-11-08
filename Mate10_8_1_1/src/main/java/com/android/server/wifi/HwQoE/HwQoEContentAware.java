package com.android.server.wifi.HwQoE;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.IProcessObserver.Stub;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.rms.iaware.AppTypeRecoManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.HwWifiConnectivityMonitor;
import com.huawei.android.app.ActivityManagerEx;
import java.util.List;

public class HwQoEContentAware {
    private static final String[] DEFAULT_GAME_NAMES = new String[]{"com.huawei.gamebox"};
    private static final int MSG_APP_UDP_MONITOR = 1;
    private static final int MSG_FOREGROUND_APP_CHANGED = 2;
    private static final String TAG = "HiDATA_ContentAware";
    private static final long UDP_ACCESS_MONITOR_INTERVAL = 2000;
    private static HwQoEContentAware mHwQoEContentAware;
    private boolean isBroadcastRegisted;
    private boolean isCallbackNotified;
    private boolean isSensitiveApp;
    private ActivityManager mActivityManager = ((ActivityManager) this.mContext.getSystemService("activity"));
    private int mAppSensitivityScore;
    private AppTypeRecoManager mAppTypeRecoManager = AppTypeRecoManager.getInstance();
    private IHwQoEContentAwareCallback mCallback;
    private Context mContext;
    private int mCurrMoniorUid;
    private int mCurrMoniorscore;
    private HwQoEUdpNetWorkInfo mCurrUdpInfoForMonitor;
    private String mForegroundAppPackageName;
    private String mForegroundName;
    private HiDataTrafficManager mHiDataTrafficManager;
    private HwProcessObserver mHwProcessObserver;
    private Handler mHwQoEContentAwareHandler;
    private HwQoEJNIAdapter mHwQoEJNIAdapter = HwQoEJNIAdapter.getInstance();
    private HwQoEWifiPolicyConfigManager mHwQoEWifiPolicyConfigManager = HwQoEWifiPolicyConfigManager.getInstance(this.mContext);
    private HwQoEUdpNetWorkInfo mLastUdpInfoForMonitor;
    private int mMonitorNetwork = -1;
    private PackageManager mPackageManager = this.mContext.getPackageManager();
    private String mPackageName;

    private class HwProcessObserver extends Stub {
        private HwProcessObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities) {
                if (HwQoEContentAware.this.mHwQoEContentAwareHandler.hasMessages(2)) {
                    HwQoEContentAware.this.mHwQoEContentAwareHandler.removeMessages(2);
                }
                HwQoEContentAware.this.mForegroundAppPackageName = HwQoEContentAware.this.getAppNameUid(uid);
                HwQoEContentAware.this.logD("mForegroundAppPackageName: " + HwQoEContentAware.this.mForegroundAppPackageName + " ,score: " + HwQoEContentAware.this.mAppSensitivityScore);
                HwQoEContentAware.this.handleForegroundAppWifiSleepChange(HwQoEContentAware.this.mForegroundAppPackageName, true);
                if (!TextUtils.isEmpty(HwQoEContentAware.this.mForegroundAppPackageName)) {
                    HwWifiConnectivityMonitor.getInstance().notifyForegroundAppChanged(HwQoEContentAware.this.mForegroundAppPackageName);
                }
            }
            if (uid > 0 && !TextUtils.isEmpty(HwQoEContentAware.this.mPackageName)) {
                if (foregroundActivities && HwQoEContentAware.this.mPackageName.equals(HwQoEContentAware.this.getAppNameUid(uid))) {
                    HwQoEContentAware.this.logD("SensitiveApp is foregroundActivities,  ,currMoniorUid:" + HwQoEContentAware.this.mCurrMoniorUid);
                    HwQoEContentAware.this.isSensitiveApp = true;
                    HwQoEContentAware.this.mCurrMoniorUid = uid;
                    if (!HwQoEContentAware.this.mHwQoEContentAwareHandler.hasMessages(1)) {
                        HwQoEContentAware.this.mHwQoEContentAwareHandler.sendEmptyMessage(1);
                    }
                    if (HwQoEContentAware.this.isCallbackNotified) {
                        HwQoEContentAware.this.mCallback.onSensitiveAppStateChange(HwQoEContentAware.this.mCurrMoniorUid, 2, false);
                    }
                } else if (HwQoEContentAware.this.isSensitiveApp && (foregroundActivities ^ 1) != 0 && HwQoEContentAware.this.mPackageName.equals(HwQoEContentAware.this.getAppNameUid(uid))) {
                    HwQoEContentAware.this.mForegroundName = HwQoEContentAware.this.getForegroundActivity();
                    HwQoEContentAware.this.logD("SensitiveApp is BackgroundActivities,isCallbackNotified:" + HwQoEContentAware.this.isCallbackNotified + ", mForegroundName:" + HwQoEContentAware.this.mForegroundName);
                    if (TextUtils.isEmpty(HwQoEContentAware.this.mForegroundName) || !HwQoEContentAware.this.mForegroundName.equals(HwQoEContentAware.this.getAppNameUid(HwQoEContentAware.this.mCurrMoniorUid))) {
                        HwQoEContentAware.this.isSensitiveApp = false;
                        if (HwQoEContentAware.this.isCallbackNotified) {
                            HwQoEContentAware.this.mCallback.onSensitiveAppStateChange(HwQoEContentAware.this.mCurrMoniorUid, 2, true);
                        } else if (HwQoEContentAware.this.mHwQoEContentAwareHandler.hasMessages(1)) {
                            HwQoEContentAware.this.mHwQoEContentAwareHandler.removeMessages(1);
                        }
                    } else {
                        HwQoEContentAware.this.logD("SensitiveApp is Not BackgroundActivities");
                    }
                }
            }
        }

        public void onProcessDied(int pid, int uid) {
            if (HwQoEContentAware.this.isSensitiveApp && uid == HwQoEContentAware.this.mCurrMoniorUid) {
                HwQoEContentAware.this.mForegroundName = HwQoEContentAware.this.getForegroundActivity();
            }
        }
    }

    public static HwQoEContentAware createInstance(Context context, IHwQoEContentAwareCallback callback) {
        if (mHwQoEContentAware == null) {
            mHwQoEContentAware = new HwQoEContentAware(context, callback);
        }
        return mHwQoEContentAware;
    }

    public static HwQoEContentAware getInstance() {
        return mHwQoEContentAware;
    }

    public void setAppStateMonitorEnabled(boolean enabled, String packageName, int network) {
        logD("setAppStateMonitorEnabled, enabled: " + enabled + ", packageName: " + packageName + ",network: " + network + " ,mMonitorNetwork: " + this.mMonitorNetwork);
        if (!enabled) {
            if (this.isCallbackNotified) {
                this.isCallbackNotified = false;
                this.mCallback.onSensitiveAppStateChange(this.mCurrMoniorUid, 0, true);
            }
            this.mPackageName = null;
            this.mCurrMoniorUid = 0;
            this.mAppSensitivityScore = 0;
            this.mMonitorNetwork = -1;
            this.mHwQoEContentAwareHandler.removeMessages(1);
        } else if (this.mMonitorNetwork != network) {
            this.mPackageName = packageName;
            this.mCurrMoniorUid = getAppUid(this.mPackageName);
            this.mAppSensitivityScore = 0;
            this.mMonitorNetwork = network;
            this.mHwQoEContentAwareHandler.removeMessages(1);
            this.mHwQoEContentAwareHandler.sendEmptyMessageDelayed(1, UDP_ACCESS_MONITOR_INTERVAL);
        }
    }

    private HwQoEContentAware(Context context, IHwQoEContentAwareCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
        initHwQoEContentAwareHandler();
        this.mHiDataTrafficManager = new HiDataTrafficManager(context);
    }

    public void updateWifiSleepWhiteList(int type, List<String> packageWhiteList) {
        if (7 == type) {
            this.mHwQoEWifiPolicyConfigManager.updateWifiSleepWhiteList(packageWhiteList);
        }
    }

    public boolean isLiveStreamApp(int uid) {
        int noBgLimit = this.mHwQoEWifiPolicyConfigManager.queryWifiSleepTime(getAppNameUid(uid));
        if (21 != this.mAppTypeRecoManager.getAppType(getAppNameUid(uid)) && 1000 != noBgLimit) {
            return false;
        }
        logD("isLiveStreamApp,uid:" + uid);
        return true;
    }

    private int calculateNewUdpAccessScore(HwQoEUdpNetWorkInfo currUdpInfo, HwQoEUdpNetWorkInfo lastUdpInfo) {
        if (currUdpInfo == null) {
            logD("currUdpInfo is null");
            return 0;
        } else if (lastUdpInfo == null) {
            logD("lastUdpInfo is null");
            return 0;
        } else {
            if (currUdpInfo.getUid() != lastUdpInfo.getUid()) {
                logD("uid is error,ignore calculate score");
                return 0;
            }
            if (currUdpInfo.getNetwork() != lastUdpInfo.getNetwork()) {
                logD("Network is error,ignore calculate score");
                return 0;
            }
            long timestamp = currUdpInfo.getTimestamp() - lastUdpInfo.getTimestamp();
            if (timestamp <= 0 || timestamp > 10000) {
                logD("[timestamp]: " + timestamp);
                return 0;
            }
            int score;
            long txUdpPackets = currUdpInfo.getTxUdpPackets() - lastUdpInfo.getTxUdpPackets();
            long rxUdpPackets = currUdpInfo.getRxUdpPackets() - lastUdpInfo.getRxUdpPackets();
            long rxUdpBytes = currUdpInfo.getRxUdpBytes() - lastUdpInfo.getRxUdpBytes();
            long txUdpBytes = currUdpInfo.getTxUdpBytes() - lastUdpInfo.getTxUdpBytes();
            long socketNum = (long) currUdpInfo.getUidUdpSockets();
            long rxTcpBytes = currUdpInfo.mRxTcpBytes - lastUdpInfo.mRxTcpBytes;
            long rxTcpPackets = currUdpInfo.mRxTcpPackets - lastUdpInfo.mRxTcpPackets;
            long txTcpBytes = currUdpInfo.mTxTcpBytes - lastUdpInfo.mTxTcpBytes;
            long txTcpPackets = currUdpInfo.mTxTcpPackets - lastUdpInfo.mTxTcpPackets;
            long stamp = timestamp / 1000;
            if (stamp == 0) {
                stamp = 1;
            }
            long outSpeed = ((txUdpBytes / stamp) / 1024) * 8;
            long inSpeed = ((rxUdpBytes / stamp) / 1024) * 8;
            long outTcpSpeed = ((txTcpBytes / stamp) / 1024) * 8;
            long inTcpSpeed = ((rxTcpBytes / stamp) / 1024) * 8;
            if ((rxUdpPackets + txUdpPackets) / stamp > 10 || (outTcpSpeed + inTcpSpeed) / stamp > 10) {
                score = 1;
            } else {
                score = -1;
            }
            if (socketNum == 0 && rxUdpPackets + txUdpPackets == 0 && txTcpPackets + rxTcpPackets == 0) {
                score = 0 - this.mAppSensitivityScore;
            }
            if (this.mCallback != null && isWechartCalling()) {
                if (inSpeed != 0 || inTcpSpeed == 0) {
                    this.mCallback.onPeriodSpeed(outSpeed, inSpeed);
                } else {
                    this.mCallback.onPeriodSpeed(outTcpSpeed, inTcpSpeed);
                }
            }
            return score;
        }
    }

    private void initHwQoEContentAwareHandler() {
        HandlerThread handlerThread = new HandlerThread("hw_qoe_contentaware_thread");
        handlerThread.start();
        this.mHwQoEContentAwareHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if (HwQoEContentAware.this.isSensitiveApp || HwQoEContentAware.this.isWechartCalling()) {
                            if (HwQoEContentAware.this.mMonitorNetwork == 1 || HwQoEContentAware.this.mMonitorNetwork == 0) {
                                HwQoEContentAware.this.mCurrUdpInfoForMonitor = HwQoEContentAware.this.mHwQoEJNIAdapter.getUdpNetworkStatsDetail(HwQoEContentAware.this.mCurrMoniorUid, HwQoEContentAware.this.mMonitorNetwork);
                            }
                            if (HwQoEContentAware.this.mLastUdpInfoForMonitor == null) {
                                HwQoEContentAware.this.mLastUdpInfoForMonitor = new HwQoEUdpNetWorkInfo(HwQoEContentAware.this.mCurrUdpInfoForMonitor);
                            } else {
                                HwQoEContentAware.this.handleWechatUdpInfoChange();
                                HwQoEContentAware.this.mLastUdpInfoForMonitor.setUdpNetWorkInfo(HwQoEContentAware.this.mCurrUdpInfoForMonitor);
                            }
                            if (!HwQoEContentAware.this.mHwQoEContentAwareHandler.hasMessages(1)) {
                                HwQoEContentAware.this.mHwQoEContentAwareHandler.sendEmptyMessageDelayed(1, HwQoEContentAware.UDP_ACCESS_MONITOR_INTERVAL);
                                return;
                            }
                            return;
                        } else if (HwQoEContentAware.this.isCallbackNotified) {
                            HwQoEContentAware.this.isCallbackNotified = false;
                            HwQoEContentAware.this.mCallback.onSensitiveAppStateChange(HwQoEContentAware.this.mCurrMoniorUid, 0, true);
                            return;
                        } else {
                            return;
                        }
                    case 2:
                        HwQoEContentAware.this.handleForegroundAppWifiSleepChange(HwQoEContentAware.this.mForegroundAppPackageName, false);
                        return;
                    default:
                        return;
                }
            }
        };
    }

    private void registerProcessObserver() {
        if (!this.isBroadcastRegisted) {
            this.isBroadcastRegisted = true;
            this.mHwProcessObserver = new HwProcessObserver();
            logD("registerProcessObserver");
            try {
                ActivityManagerNative.getDefault().registerProcessObserver(this.mHwProcessObserver);
            } catch (RemoteException e) {
                logD("register process observer failed," + e.getMessage());
            }
        }
    }

    public int getForegroundAppUid() {
        return getAppUid(getForegroundActivity());
    }

    public int getAppUid(String processName) {
        int uid = -1;
        if (TextUtils.isEmpty(processName)) {
            return -1;
        }
        try {
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(processName, 1);
            if (ai != null) {
                uid = ai.uid;
                logD("packageName = " + processName + ", uid = " + uid);
            }
        } catch (NameNotFoundException e) {
            logD("NameNotFoundException: " + e.getMessage());
        }
        return uid;
    }

    public String getAppNameUid(int uid) {
        String processName = "";
        List<RunningAppProcessInfo> appProcessList = this.mActivityManager.getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.uid == uid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    private synchronized String getForegroundActivity() {
        List<RunningTaskInfo> runningTaskInfos = this.mActivityManager.getRunningTasks(1);
        if (runningTaskInfos == null || runningTaskInfos.isEmpty()) {
            logD("running task is null, ams is abnormal!!!");
            return null;
        }
        RunningTaskInfo mRunningTask = (RunningTaskInfo) runningTaskInfos.get(0);
        if (mRunningTask == null) {
            logD("failed to get RunningTaskInfo");
            return null;
        }
        return mRunningTask.topActivity.getPackageName();
    }

    public void queryForegroundAppType() {
        handleForegroundAppWifiSleepChange(getForegroundActivity(), true);
    }

    public void systemBootCompled() {
        registerProcessObserver();
        this.mHiDataTrafficManager.systemBootCompled();
    }

    private void handleWechatUdpInfoChange() {
        this.mCurrMoniorscore = calculateNewUdpAccessScore(this.mCurrUdpInfoForMonitor, this.mLastUdpInfoForMonitor);
        this.mAppSensitivityScore += this.mCurrMoniorscore;
        if (this.mAppSensitivityScore < 0 || (this.mAppSensitivityScore >= 2 && this.mCurrMoniorscore <= 0)) {
            this.mAppSensitivityScore = 0;
        }
        if (!this.isCallbackNotified && this.mAppSensitivityScore >= 2 && isWechartCalling()) {
            this.isCallbackNotified = true;
            this.mCallback.onSensitiveAppStateChange(this.mCurrMoniorUid, 1, false);
        } else if (this.isCallbackNotified && this.mAppSensitivityScore == 0 && (isWechartCalling() ^ 1) != 0) {
            this.isCallbackNotified = false;
            this.mCallback.onSensitiveAppStateChange(this.mCurrMoniorUid, 0, false);
            if (!this.isSensitiveApp && this.mHwQoEContentAwareHandler.hasMessages(1)) {
                this.mHwQoEContentAwareHandler.removeMessages(1);
            }
        }
    }

    private void handleForegroundAppWifiSleepChange(String packageName, boolean retry) {
        if (!TextUtils.isEmpty(packageName)) {
            int sleepTime = this.mHwQoEWifiPolicyConfigManager.queryWifiSleepTime(packageName);
            int type = this.mAppTypeRecoManager.getAppType(packageName);
            if (-1 != sleepTime) {
                this.mCallback.onForegroundAppWifiSleepChange(true, sleepTime, type, packageName);
            } else if (-1 != type || !retry) {
                this.mCallback.onForegroundAppWifiSleepChange(false, sleepTime, type, packageName);
            } else if (!this.mHwQoEContentAwareHandler.hasMessages(2)) {
                this.mHwQoEContentAwareHandler.sendEmptyMessageDelayed(2, UDP_ACCESS_MONITOR_INTERVAL);
            }
            this.mCallback.onForegroundAppTypeChange(type, packageName);
        }
    }

    public boolean isGameType(int type, String packageName) {
        if (isGameTypeForRecoManager(type)) {
            return true;
        }
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (isDefaultGameType(packageName)) {
            return true;
        }
        if (packageName.contains(":") && isGameTypeForRecoManager(this.mAppTypeRecoManager.getAppType(getRealAppName(packageName)))) {
            return true;
        }
        if (HwQoEUtils.GAME_ASSISIT_ENABLE) {
            return ActivityManagerEx.isInGameSpace(packageName);
        }
        return false;
    }

    public String getRealAppName(String appName) {
        String realName = "";
        if (!TextUtils.isEmpty(appName) && appName.contains(":")) {
            String[] appNames = appName.split(":", 2);
            if (appNames.length > 0) {
                realName = appNames[0];
            }
        }
        logD("RealAppName:" + realName);
        return realName;
    }

    public boolean isGameTypeForRecoManager(int type) {
        if (305 == type || 9 == type) {
            return true;
        }
        return false;
    }

    private boolean isDefaultGameType(String appName) {
        if (!TextUtils.isEmpty(appName)) {
            for (String startsWith : DEFAULT_GAME_NAMES) {
                if (appName.startsWith(startsWith)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void logD(String log) {
        Log.d(TAG, log);
    }

    private boolean isWechartCalling() {
        if (((AudioManager) this.mContext.getSystemService("audio")).getMode() == 3) {
            return true;
        }
        return false;
    }
}
