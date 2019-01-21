package com.android.server.wifi.LAA;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.IProcessObserver.Stub;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import com.android.server.hidata.HwQoEUdpNetWorkInfo;
import com.android.server.wifi.HwQoE.HwQoEJNIAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HwLaaContentAware {
    private static final int MSG_APP_UDP_MONITOR = 1;
    private static final String TAG = "LAA_HwLaaContentAware";
    private static final long UDP_ACCESS_MONITOR_INTERVAL = 3000;
    private boolean isLaaContentAwareEnabled;
    private boolean isRequestLaaDisEnabled;
    private boolean isSensitiveApp;
    private ActivityManager mActivityManager;
    private int mAppSensitivityScore;
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private int mCurrMoniorUid;
    private HwQoEUdpNetWorkInfo mCurrUdpInfoForMonitor;
    private String mForegroundName;
    private Handler mHwLaaContentAwareHandler;
    private Handler mHwLaaControllerHandler;
    private HwProcessObserver mHwProcessObserver;
    private HwQoEJNIAdapter mHwQoEJNIAdapter;
    private IntentFilter mIntentFilter;
    private HwQoEUdpNetWorkInfo mLastUdpInfoForMonitor;
    private PackageManager mPackageManager = this.mContext.getPackageManager();
    private Map<String, Integer> mSensitiveAppHashMap;

    public class AppInstallReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String packageName;
            String str;
            StringBuilder stringBuilder;
            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED") || intent.getAction().equals("android.intent.action.PACKAGE_REPLACED")) {
                packageName = intent.getData().getSchemeSpecificPart();
                str = HwLaaContentAware.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" add_packageName = ");
                stringBuilder.append(packageName);
                HwLaaUtils.logD(str, stringBuilder.toString());
                if (!TextUtils.isEmpty(packageName) && HwLaaUtils.matchSensitiveApp(packageName)) {
                    int add_uid = HwLaaContentAware.this.getAppUid(packageName);
                    if (add_uid > 0) {
                        HwLaaContentAware.this.mSensitiveAppHashMap.put(packageName, Integer.valueOf(add_uid));
                    }
                }
            } else if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
                packageName = intent.getData().getSchemeSpecificPart();
                str = HwLaaContentAware.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" removed_packageName = ");
                stringBuilder.append(packageName);
                HwLaaUtils.logD(str, stringBuilder.toString());
                if (!TextUtils.isEmpty(packageName) && HwLaaUtils.matchSensitiveApp(packageName) && HwLaaContentAware.this.mSensitiveAppHashMap.containsKey(packageName)) {
                    HwLaaContentAware.this.mSensitiveAppHashMap.remove(packageName);
                }
            }
        }
    }

    private class HwProcessObserver extends Stub {
        private HwProcessObserver() {
        }

        /* synthetic */ HwProcessObserver(HwLaaContentAware x0, AnonymousClass1 x1) {
            this();
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities) {
                String str = HwLaaContentAware.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("uid name :");
                stringBuilder.append(HwLaaContentAware.this.getAppNameUid(uid));
                stringBuilder.append(", uid:");
                stringBuilder.append(uid);
                stringBuilder.append(",moniorUid:");
                stringBuilder.append(HwLaaContentAware.this.mCurrMoniorUid);
                HwLaaUtils.logD(str, stringBuilder.toString());
            }
            String str2;
            StringBuilder stringBuilder2;
            if (foregroundActivities && HwLaaContentAware.this.mSensitiveAppHashMap.containsValue(Integer.valueOf(uid))) {
                str2 = HwLaaContentAware.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SensitiveApp is foregroundActivities, isLaaContentAwareEnabled = ");
                stringBuilder2.append(HwLaaContentAware.this.isLaaContentAwareEnabled);
                HwLaaUtils.logD(str2, stringBuilder2.toString());
                HwLaaContentAware.this.isSensitiveApp = true;
                HwLaaContentAware.this.mCurrMoniorUid = uid;
                if (HwLaaContentAware.this.isLaaContentAwareEnabled && !HwLaaContentAware.this.mHwLaaContentAwareHandler.hasMessages(1)) {
                    HwLaaContentAware.this.mHwLaaContentAwareHandler.sendEmptyMessage(1);
                }
            } else if (HwLaaContentAware.this.isSensitiveApp && !foregroundActivities && HwLaaContentAware.this.mSensitiveAppHashMap.containsValue(Integer.valueOf(uid))) {
                HwLaaContentAware.this.mForegroundName = HwLaaContentAware.this.getForegroundActivity();
                str2 = HwLaaContentAware.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SensitiveApp is BackgroundActivities,isRequestLaaDisEnabled:");
                stringBuilder2.append(HwLaaContentAware.this.isRequestLaaDisEnabled);
                stringBuilder2.append(", foregroundName:");
                stringBuilder2.append(HwLaaContentAware.this.mForegroundName);
                HwLaaUtils.logD(str2, stringBuilder2.toString());
                if (TextUtils.isEmpty(HwLaaContentAware.this.mForegroundName) || !HwLaaContentAware.this.mForegroundName.equals(HwLaaContentAware.this.getAppNameUid(HwLaaContentAware.this.mCurrMoniorUid))) {
                    HwLaaContentAware.this.isSensitiveApp = false;
                    if (HwLaaContentAware.this.mHwLaaContentAwareHandler.hasMessages(1)) {
                        HwLaaUtils.logD(HwLaaContentAware.TAG, "BackgroundActivities,removeMessages: MSG_APP_UDP_MONITOR");
                        HwLaaContentAware.this.mHwLaaContentAwareHandler.removeMessages(1);
                    }
                    if (HwLaaContentAware.this.isRequestLaaDisEnabled) {
                        HwLaaContentAware.this.requestSendLaaCmd(1);
                    }
                } else {
                    HwLaaUtils.logD(HwLaaContentAware.TAG, "SensitiveApp is Not BackgroundActivities");
                }
            }
        }

        public void onProcessDied(int pid, int uid) {
            if (uid == HwLaaContentAware.this.mCurrMoniorUid && HwLaaContentAware.this.mSensitiveAppHashMap.containsValue(Integer.valueOf(uid))) {
                HwLaaContentAware.this.mForegroundName = HwLaaContentAware.this.getForegroundActivity();
                String str = HwLaaContentAware.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onProcessDied,  foregroundName:");
                stringBuilder.append(HwLaaContentAware.this.mForegroundName);
                HwLaaUtils.logD(str, stringBuilder.toString());
            }
        }
    }

    public HwLaaContentAware(Context context, Handler handler) {
        this.mContext = context;
        this.mHwLaaControllerHandler = handler;
        this.mHwQoEJNIAdapter = HwQoEJNIAdapter.getInstance();
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        initialSensitiveAppHashMap();
        initHwLaaContentAwareHandler();
        registerProcessObserver();
    }

    public synchronized void setLaaContentAwareEnabled(boolean enabled) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setLaaContentAwareEnabled: ");
        stringBuilder.append(enabled);
        stringBuilder.append(", isSensitiveApp:");
        stringBuilder.append(this.isSensitiveApp);
        HwLaaUtils.logD(str, stringBuilder.toString());
        this.isLaaContentAwareEnabled = enabled;
        if (enabled) {
            if (this.isSensitiveApp && !this.mHwLaaContentAwareHandler.hasMessages(1)) {
                this.mHwLaaContentAwareHandler.sendEmptyMessage(1);
            }
        } else if (this.mHwLaaContentAwareHandler.hasMessages(1)) {
            HwLaaUtils.logD(TAG, "removeMessages : MSG_APP_UDP_MONITOR");
            this.mHwLaaContentAwareHandler.removeMessages(1);
        }
    }

    private void initHwLaaContentAwareHandler() {
        HandlerThread handlerThread = new HandlerThread("hw_laa_plus_handler_thread");
        handlerThread.start();
        this.mHwLaaContentAwareHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    if (HwLaaContentAware.this.isLaaContentAwareEnabled) {
                        HwLaaContentAware.this.mCurrUdpInfoForMonitor = HwLaaContentAware.this.mHwQoEJNIAdapter.getUdpNetworkStatsDetail(HwLaaContentAware.this.mCurrMoniorUid, 0);
                        HwLaaContentAware.this.calculateNewUdpAccessScore(HwLaaContentAware.this.mCurrUdpInfoForMonitor, HwLaaContentAware.this.mLastUdpInfoForMonitor);
                        if (HwLaaContentAware.this.mLastUdpInfoForMonitor == null) {
                            HwLaaContentAware.this.mLastUdpInfoForMonitor = new HwQoEUdpNetWorkInfo();
                        }
                        HwLaaContentAware.this.mLastUdpInfoForMonitor.setUdpNetWorkInfo(HwLaaContentAware.this.mCurrUdpInfoForMonitor);
                        String str = HwLaaContentAware.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("AppSensitivityScore = ");
                        stringBuilder.append(HwLaaContentAware.this.mAppSensitivityScore);
                        stringBuilder.append(" , RequestLaaDisEnabled = ");
                        stringBuilder.append(HwLaaContentAware.this.isRequestLaaDisEnabled);
                        stringBuilder.append(" ,isSensitiveApp = ");
                        stringBuilder.append(HwLaaContentAware.this.isSensitiveApp);
                        HwLaaUtils.logD(str, stringBuilder.toString());
                        if (!HwLaaContentAware.this.isRequestLaaDisEnabled && HwLaaContentAware.this.mAppSensitivityScore >= 2) {
                            HwLaaContentAware.this.requestSendLaaCmd(0);
                        } else if (HwLaaContentAware.this.isRequestLaaDisEnabled && HwLaaContentAware.this.mAppSensitivityScore == 0) {
                            HwLaaContentAware.this.requestSendLaaCmd(1);
                        }
                        if (HwLaaContentAware.this.isSensitiveApp) {
                            if (HwLaaContentAware.this.mHwLaaContentAwareHandler.hasMessages(1)) {
                                HwLaaUtils.logD(HwLaaContentAware.TAG, "the message already exists,remove it");
                                HwLaaContentAware.this.mHwLaaContentAwareHandler.removeMessages(1);
                            }
                            HwLaaContentAware.this.mHwLaaContentAwareHandler.sendEmptyMessageDelayed(1, HwLaaContentAware.UDP_ACCESS_MONITOR_INTERVAL);
                            return;
                        }
                        return;
                    }
                    HwLaaUtils.logD(HwLaaContentAware.TAG, "ContentAware is disenable");
                }
            }
        };
    }

    private void initialSensitiveAppHashMap() {
        this.mSensitiveAppHashMap = new HashMap();
        for (int i = 0; i < HwLaaUtils.DELAY_SENSITIVE_APPS.length; i++) {
            int uid = getAppUid(HwLaaUtils.DELAY_SENSITIVE_APPS[i]);
            if (uid > 0) {
                this.mSensitiveAppHashMap.put(HwLaaUtils.DELAY_SENSITIVE_APPS[i], Integer.valueOf(uid));
            }
        }
    }

    private int getAppUid(String processName) {
        int uid = -1;
        if (TextUtils.isEmpty(processName)) {
            return -1;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(processName, 1);
            if (ai != null) {
                uid = ai.uid;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("packageName = ");
                stringBuilder.append(processName);
                stringBuilder.append(", uid = ");
                stringBuilder.append(uid);
                HwLaaUtils.logD(str, stringBuilder.toString());
            }
        } catch (NameNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NameNotFoundException: ");
            stringBuilder.append(e.getMessage());
            HwLaaUtils.logD(str, stringBuilder.toString());
        }
        return uid;
    }

    private String getAppName(int pID) {
        String processName = "";
        List<RunningAppProcessInfo> appProcessList = this.mActivityManager.getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pID) {
                return appProcess.processName;
            }
        }
        return null;
    }

    private String getAppNameUid(int uid) {
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
        if (runningTaskInfos != null) {
            if (!runningTaskInfos.isEmpty()) {
                RunningTaskInfo mRunningTask = (RunningTaskInfo) runningTaskInfos.get(0);
                if (mRunningTask == null) {
                    HwLaaUtils.logD(TAG, "failed to get RunningTaskInfo");
                    return null;
                }
                return mRunningTask.topActivity.getPackageName();
            }
        }
        HwLaaUtils.logD(TAG, "running task is null, ams is abnormal!!!");
        return null;
    }

    private void registerProcessObserver() {
        this.mHwProcessObserver = new HwProcessObserver(this, null);
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mHwProcessObserver);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("register process observer failed,");
            stringBuilder.append(e.getMessage());
            HwLaaUtils.logD(str, stringBuilder.toString());
        }
        this.mBroadcastReceiver = new AppInstallReceiver();
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        this.mIntentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        this.mIntentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        this.mIntentFilter.addDataScheme("package");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    private void calculateNewUdpAccessScore(HwQoEUdpNetWorkInfo currUdpInfo, HwQoEUdpNetWorkInfo lastUdpInfo) {
        if (currUdpInfo == null || lastUdpInfo == null) {
            this.mAppSensitivityScore = 0;
        } else if (currUdpInfo.getUid() != lastUdpInfo.getUid()) {
            HwLaaUtils.logD(TAG, "uid is error,ignore calculate score");
            this.mAppSensitivityScore = 0;
        } else {
            long timestamp = currUdpInfo.getTimestamp() - lastUdpInfo.getTimestamp();
            if (timestamp <= 0 || timestamp > 10000) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[timestamp]: ");
                stringBuilder.append(timestamp);
                HwLaaUtils.logD(str, stringBuilder.toString());
                this.mAppSensitivityScore = 0;
                return;
            }
            long txUdpPackets = currUdpInfo.getTxUdpPackets() - lastUdpInfo.getTxUdpPackets();
            long rxUdpPackets = currUdpInfo.getRxUdpPackets() - lastUdpInfo.getRxUdpPackets();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[timestamp]: ");
            stringBuilder2.append(timestamp);
            stringBuilder2.append(" , [txUdpPackets]: ");
            stringBuilder2.append(txUdpPackets);
            stringBuilder2.append(" , [rxUdpPackets]: ");
            stringBuilder2.append(rxUdpPackets);
            HwLaaUtils.logD(str2, stringBuilder2.toString());
            long stamp = timestamp / 1000;
            if (stamp == 0) {
                stamp = 1;
            }
            if ((rxUdpPackets + txUdpPackets) / stamp > 10) {
                if (this.mAppSensitivityScore < 2) {
                    this.mAppSensitivityScore++;
                }
            } else if (this.mAppSensitivityScore > 0) {
                this.mAppSensitivityScore--;
            }
        }
    }

    private boolean requestSendLaaCmd(int cmd) {
        this.mHwLaaControllerHandler.sendMessage(this.mHwLaaControllerHandler.obtainMessage(1, cmd, 5));
        if (cmd == 0) {
            this.isRequestLaaDisEnabled = true;
        } else if (1 == cmd) {
            this.isRequestLaaDisEnabled = false;
        }
        return true;
    }
}
