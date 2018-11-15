package com.android.server.hidata.appqoe;

import android.content.ComponentName;
import android.content.Context;
import android.contentsensor.IActivityObserver;
import android.contentsensor.IActivityObserver.Stub;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.rms.iaware.AppTypeRecoManager;
import android.text.TextUtils;
import com.android.server.am.HwActivityManagerService;
import com.huawei.android.app.ActivityManagerEx;

public class HwAPPQoEActivityMonitor {
    private static final String[] DEFAULT_GAME_NAMES = new String[]{"com.huawei.gamebox"};
    private static final int EVENT_ACTIVITY_STATE_CHANGE = 2;
    private static final int EVENT_DELAY_TIMER_EXPIRE = 1;
    private static String TAG = "HiData_HwAPPQoEActivityMonitor";
    private static HwAPPStateInfo lastAPPStateInfo = new HwAPPStateInfo();
    private static HwAPPQoEActivityMonitor mHwAPPQoEActivityMonitor = null;
    private int APP_USER_LEARNING_MONITOR;
    private long apkStartTime;
    private HwActivityManagerService hwActivityManger;
    private HwAPPQoEResourceManger hwResourceManger;
    private HwAPPQoEUserLearning hwUserLearning;
    private IActivityObserver mActivityObserver;
    private AppTypeRecoManager mAppTypeRecoManager;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private int monitorAPPUID;

    public static class CachedAppInfo {
        public int mAppUID;
        public String mClassName;
        public String mPackageName;
    }

    private class MainHandler extends Handler {
        MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg == null || msg.obj == null) {
                HwAPPQoEUtils.logD(HwAPPQoEActivityMonitor.TAG, "handleMessage -- invalid input");
                return;
            }
            CachedAppInfo cachedAppInfo;
            switch (msg.what) {
                case 1:
                    cachedAppInfo = msg.obj;
                    HwAPPQoEActivityMonitor.this.handleActivityChange(cachedAppInfo.mPackageName, cachedAppInfo.mClassName, cachedAppInfo.mAppUID, true);
                    break;
                case 2:
                    cachedAppInfo = msg.obj;
                    HwAPPQoEActivityMonitor.this.handleActivityChange(cachedAppInfo.mPackageName, cachedAppInfo.mClassName, cachedAppInfo.mAppUID, false);
                    break;
            }
        }
    }

    private HwAPPQoEActivityMonitor(Context context) {
        this.hwResourceManger = null;
        this.hwActivityManger = null;
        this.hwUserLearning = null;
        this.mHandler = null;
        this.mHandlerThread = null;
        this.apkStartTime = System.currentTimeMillis();
        this.monitorAPPUID = -1;
        this.APP_USER_LEARNING_MONITOR = 1;
        this.mActivityObserver = new Stub() {
            public void activityResumed(int pid, int uid, ComponentName componentName) throws RemoteException {
                try {
                    CachedAppInfo mCachedAppInfo = new CachedAppInfo();
                    mCachedAppInfo.mAppUID = uid;
                    mCachedAppInfo.mClassName = componentName.getClassName();
                    mCachedAppInfo.mPackageName = componentName.getPackageName();
                    HwAPPQoEActivityMonitor.this.mHandler.removeMessages(1);
                    HwAPPQoEActivityMonitor.this.mHandler.sendMessage(HwAPPQoEActivityMonitor.this.mHandler.obtainMessage(2, mCachedAppInfo));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void activityPaused(int pid, int uid, ComponentName componentName) throws RemoteException {
            }
        };
        this.hwResourceManger = HwAPPQoEResourceManger.getInstance();
        this.hwUserLearning = HwAPPQoEUserLearning.createHwAPPQoEUserLearning(context);
        initProcess();
    }

    private void initProcess() {
        this.hwActivityManger = HwActivityManagerService.self();
        if (this.hwActivityManger != null) {
            this.hwActivityManger.registerActivityObserver(this.mActivityObserver);
        }
        this.mAppTypeRecoManager = AppTypeRecoManager.getInstance();
        this.mHandlerThread = new HandlerThread("HwAPPQoEActivityMonitor Thread");
        this.mHandlerThread.start();
        this.mHandler = new MainHandler(this.mHandlerThread.getLooper());
    }

    protected static HwAPPQoEActivityMonitor createHwAPPQoEActivityMonitor(Context context) {
        if (mHwAPPQoEActivityMonitor == null) {
            mHwAPPQoEActivityMonitor = new HwAPPQoEActivityMonitor(context);
        }
        return mHwAPPQoEActivityMonitor;
    }

    public void handleActivityChange(String curPackage, String curClass, int curUid, boolean isCalledByTimerExpired) {
        if (curPackage == null || curClass == null) {
            HwAPPQoEUtils.logD(TAG, "handleActivityChange,  error input");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleActivityChange,  curPackage:");
        stringBuilder.append(curPackage);
        stringBuilder.append(", curClass:");
        stringBuilder.append(curClass);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        HwAPPStateInfo curAPPStateInfo = new HwAPPStateInfo();
        curAPPStateInfo.mAppUID = curUid;
        HwAPPQoEAPKConfig tempAPKScence = this.hwResourceManger.checkIsMonitorAPKScence(curPackage, curClass);
        notifyAPPStateChange(curPackage, curUid, tempAPKScence);
        if (tempAPKScence != null) {
            curAPPStateInfo.mAppId = tempAPKScence.mAppId;
            curAPPStateInfo.mScenceId = tempAPKScence.mScenceId;
            curAPPStateInfo.mScenceType = tempAPKScence.mScenceType;
            curAPPStateInfo.mAppPeriod = tempAPKScence.mAppPeriod;
            curAPPStateInfo.mAppType = 1000;
            curAPPStateInfo.mAction = tempAPKScence.mAction;
        } else if (isGeneralGameApp(curPackage)) {
            HwAPPQoEUtils.logD(TAG, "handleActivityChange, it is a general game");
            curAPPStateInfo.mAppType = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
            curAPPStateInfo.mScenceId = HwAPPQoEUtils.GAME_SCENCE_NOT_IN_WAR;
        } else {
            HwAPPQoEUtils.logD(TAG, "handleActivityChange, it is not a care app or scence");
        }
        if (1 == lastAPPStateInfo.mScenceType) {
            if (isCalledByTimerExpired) {
                HwAPPQoEUtils.logD(TAG, "handleActivityChange, delay timer expired");
            } else if (this.hwResourceManger.checkIsMonitorGameScence(curPackage) != null) {
                HwAPPQoEUtils.logD(TAG, "handleActivityChange, app type is game");
                this.mHandler.removeMessages(1);
            } else if (1 != curAPPStateInfo.mScenceType) {
                CachedAppInfo mCachedAppInfo = new CachedAppInfo();
                mCachedAppInfo.mAppUID = curUid;
                mCachedAppInfo.mClassName = curClass;
                mCachedAppInfo.mPackageName = curPackage;
                this.mHandler.removeMessages(1);
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, mCachedAppInfo), 1000);
                HwAPPQoEUtils.logD(TAG, "handleActivityChange, delay 1s to update scence during pay scence");
                return;
            } else {
                this.mHandler.removeMessages(1);
            }
        }
        String str2;
        StringBuilder stringBuilder2;
        if (tempAPKScence != null && tempAPKScence.mAppId == lastAPPStateInfo.mAppId && 255 == tempAPKScence.mScenceType) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleActivityChange, ignore current scence:");
            stringBuilder2.append(tempAPKScence.mScenceType);
            HwAPPQoEUtils.logD(str2, stringBuilder2.toString());
            return;
        }
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("handleActivityChange,  curr mAppType:");
        stringBuilder2.append(curAPPStateInfo.mAppType);
        stringBuilder2.append(", lastType:");
        stringBuilder2.append(lastAPPStateInfo.mAppType);
        HwAPPQoEUtils.logD(str2, stringBuilder2.toString());
        if (-1 != curAPPStateInfo.mAppType && -1 == lastAPPStateInfo.mAppType) {
            HwAPPQoEContentAware.sentNotificationToSTM(curAPPStateInfo, 100);
        } else if (-1 == curAPPStateInfo.mAppType && -1 != lastAPPStateInfo.mAppType) {
            HwAPPQoEContentAware.sentNotificationToSTM(lastAPPStateInfo, 101);
        } else if (-1 != curAPPStateInfo.mAppType || -1 != lastAPPStateInfo.mAppType) {
            str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("handleActivityChange,  curr Scence:");
            stringBuilder3.append(curAPPStateInfo.mScenceId);
            stringBuilder3.append(", last Scence:");
            stringBuilder3.append(lastAPPStateInfo.mScenceId);
            HwAPPQoEUtils.logD(str2, stringBuilder3.toString());
            if (lastAPPStateInfo.mAppUID != curAPPStateInfo.mAppUID || lastAPPStateInfo.mAppId != curAPPStateInfo.mAppId) {
                HwAPPQoEContentAware.sentNotificationToSTM(lastAPPStateInfo, 101);
                HwAPPQoEContentAware.sentNotificationToSTM(curAPPStateInfo, 100);
            } else if (lastAPPStateInfo.mScenceId != curAPPStateInfo.mScenceId) {
                HwAPPQoEContentAware.sentNotificationToSTM(curAPPStateInfo, 102);
            }
        } else {
            return;
        }
        lastAPPStateInfo.copyObjectValue(curAPPStateInfo);
    }

    private void notifyAPPStateChange(String packageName, int uid, HwAPPQoEAPKConfig tempAPKConfig) {
        if (tempAPKConfig != null) {
            this.hwUserLearning.setLatestAPPScenceId(tempAPKConfig.mScenceId);
        }
        if (-1 == this.monitorAPPUID) {
            HwAPPQoEAPKConfig tempAPKScence = this.hwResourceManger.checkIsMonitorAPKScence(packageName, null);
            if (tempAPKScence != null && this.APP_USER_LEARNING_MONITOR == tempAPKScence.monitorUserLearning) {
                HwAPPQoEUtils.logD(TAG, "notifyAPPStateChange, init app state");
                this.monitorAPPUID = uid;
                this.apkStartTime = System.currentTimeMillis();
            }
        } else if (uid != this.monitorAPPUID) {
            HwAPPQoEUtils.logD(TAG, "notifyAPPStateChange, prepare to notify userlearning");
            this.hwUserLearning.notifyAPPStateChange(this.apkStartTime, System.currentTimeMillis(), lastAPPStateInfo.mAppId);
            this.monitorAPPUID = -1;
        }
    }

    public boolean isGeneralGameApp(String packageName) {
        if (this.hwResourceManger.checkIsMonitorGameScence(packageName) != null) {
            HwAPPQoEUtils.logD(TAG, "it is a monitor game");
            return false;
        } else if (isGameTypeForRecoManager(this.mAppTypeRecoManager.getAppType(packageName))) {
            return true;
        } else {
            if (TextUtils.isEmpty(packageName)) {
                return false;
            }
            if (isDefaultGameType(packageName)) {
                return true;
            }
            if (packageName.contains(":") && isGameTypeForRecoManager(this.mAppTypeRecoManager.getAppType(getRealAppName(packageName)))) {
                return true;
            }
            if (HwAPPQoEUtils.GAME_ASSISIT_ENABLE) {
                return ActivityManagerEx.isInGameSpace(packageName);
            }
            return false;
        }
    }

    private boolean isGameTypeForRecoManager(int type) {
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

    private String getRealAppName(String appName) {
        String realName = "";
        if (TextUtils.isEmpty(appName) || !appName.contains(":")) {
            return realName;
        }
        String[] appNames = appName.split(":", 2);
        if (appNames.length > 0) {
            return appNames[0];
        }
        return realName;
    }
}
