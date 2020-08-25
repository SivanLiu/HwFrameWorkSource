package com.android.server.mtm.iaware.appmng.appcpulimit;

import android.app.IProcessObserver;
import android.app.mtm.iaware.appmng.AppMngConstant;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IMWThirdpartyCallback;
import android.os.Message;
import android.os.RemoteException;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IAwareCMSManager;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.utils.InnerUtils;
import com.android.server.rms.iaware.AwareCallback;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareSceneRecognize;
import com.huawei.android.app.HwActivityTaskManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AwareAppCpuLimitMng {
    private static final int CPULIMIT_OPT_VERSION = 5;
    private static final int DEFAULT_UNCPULIMIT_DURATION = 3000;
    private static final String FEATURE_NAME = "appmng_feature";
    private static final String ITEM_CONFIG_NAME = "cpulimit";
    private static final String ITEM_NAME = "cpulimit_duration";
    private static final String KEY_BUNDLE_PACKAGENAME = "pkgName";
    private static final String KEY_BUNDLE_PID = "pid";
    private static final String KEY_BUNDLE_UID = "uid";
    private static final int MSG_ACTIVITY_STARTING = 1;
    private static final int MSG_APP_SHOW_INPUTMETHOD = 6;
    private static final int MSG_APP_SLIPPING = 4;
    private static final int MSG_APP_SLIP_END = 5;
    private static final int MSG_FG_ACTIVITIES_CHANGED = 2;
    private static final int MSG_UNCPULIMIT_DURATION = 3;
    private static final int SLIP_UNCPULIMIT_DURATION = 1000;
    private static final String TAG = "AwareAppCpuLimitMng";
    private static AwareAppCpuLimitMng mAwareAppCpuLimitMng = null;
    private static AtomicBoolean mCpuLimitEnabled = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public static boolean mEnabled = false;
    private static int mRealVersion = 0;
    /* access modifiers changed from: private */
    public AtomicBoolean isCpuLimited;
    private AppCpuLimitCallBackHandler mCallBackHandler;
    private int mCpuLimitDuration;
    private AppCpuLimitObserver mCpuLimitObserver;
    private CpuLimitSceneRecognize mCpuLimitSceneRecognize;
    /* access modifiers changed from: private */
    public String mCurPkgName;
    /* access modifiers changed from: private */
    public AtomicBoolean mEnableCpuLimitOptSwitch;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private AtomicBoolean mIsInitialized;
    /* access modifiers changed from: private */
    public AtomicBoolean mIsMultiWin;

    private AwareAppCpuLimitMng() {
        this.mCpuLimitObserver = new AppCpuLimitObserver();
        this.isCpuLimited = new AtomicBoolean(false);
        this.mIsMultiWin = new AtomicBoolean(false);
        this.mHandler = null;
        this.mCurPkgName = "";
        this.mCpuLimitDuration = 3000;
        this.mIsInitialized = new AtomicBoolean(false);
        this.mEnableCpuLimitOptSwitch = new AtomicBoolean(false);
        this.mCpuLimitSceneRecognize = new CpuLimitSceneRecognize();
        this.mCallBackHandler = new AppCpuLimitCallBackHandler();
        this.mHandler = new AwareAppCpuLimitMngHanlder();
    }

    public static synchronized AwareAppCpuLimitMng getInstance() {
        AwareAppCpuLimitMng awareAppCpuLimitMng;
        synchronized (AwareAppCpuLimitMng.class) {
            if (mAwareAppCpuLimitMng == null) {
                mAwareAppCpuLimitMng = new AwareAppCpuLimitMng();
            }
            awareAppCpuLimitMng = mAwareAppCpuLimitMng;
        }
        return awareAppCpuLimitMng;
    }

    public static boolean isCpuLimitEnabled() {
        return mCpuLimitEnabled.get();
    }

    private AwareConfig getAwareCustConfig(String featureName, String configName) {
        if (featureName == null || featureName.isEmpty() || configName == null || configName.isEmpty()) {
            return null;
        }
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice != null) {
                return IAwareCMSManager.getCustConfig(awareservice, featureName, configName);
            }
            AwareLog.e(TAG, "can not find service awareservice!");
            return null;
        } catch (RemoteException e) {
            AwareLog.e(TAG, "CpuFeature getAwareCustConfig RemoteException");
            return null;
        }
    }

    public static void setVersion(int realVersion) {
        mRealVersion = realVersion;
    }

    private void initialize() {
        List<AwareConfig.SubItem> subItemList;
        if (!this.mIsInitialized.get()) {
            registerAwareSceneRecognize();
            MultiTaskManagerService mtmService = MultiTaskManagerService.self();
            if (mtmService != null) {
                DecisionMaker.getInstance().updateRule(AppMngConstant.AppMngFeature.APP_CPULIMIT, mtmService.context());
                AwareAppDefaultCpuLimit.getInstance().init(mtmService.context());
            }
            registerObserver();
            HwActivityTaskManager.registerThirdPartyCallBack(this.mCallBackHandler);
            AwareConfig configList = getAwareCustConfig(FEATURE_NAME, ITEM_CONFIG_NAME);
            if (configList != null) {
                mCpuLimitEnabled.set(true);
                for (AwareConfig.Item item : configList.getConfigList()) {
                    if (!(item == null || (subItemList = item.getSubItemList()) == null)) {
                        for (AwareConfig.SubItem subItem : subItemList) {
                            if (subItem != null && ITEM_NAME.equals(subItem.getName())) {
                                try {
                                    this.mCpuLimitDuration = Integer.parseInt(subItem.getValue());
                                } catch (NumberFormatException e) {
                                    AwareLog.e(TAG, "mCpuLimitDuration is not an Integer!");
                                }
                            }
                        }
                    }
                }
            }
            if (mRealVersion >= 5) {
                AwareAppDefaultCpuLimit.getInstance().initCpuLimitOpt();
                if (AwareAppDefaultCpuLimit.getInstance().getInitResult()) {
                    this.mEnableCpuLimitOptSwitch.set(true);
                }
            }
            this.mIsInitialized.set(true);
        }
    }

    private synchronized void deInitialize() {
        if (this.mIsInitialized.get()) {
            unregisterAwareSceneRecognize();
            AwareAppDefaultCpuLimit.getInstance().deInitDefaultFree();
            HwActivityTaskManager.unregisterThirdPartyCallBack(this.mCallBackHandler);
            unregisterObserver();
            this.mCpuLimitDuration = 3000;
            this.mEnableCpuLimitOptSwitch.set(false);
            AwareAppDefaultCpuLimit.getInstance().setCpuLimitOptEnable(false);
            this.mIsInitialized.set(false);
        }
    }

    private void registerAwareSceneRecognize() {
        AwareSceneRecognize recognize = AwareSceneRecognize.getInstance();
        if (recognize != null) {
            recognize.registerStateCallback(this.mCpuLimitSceneRecognize, 1);
        }
    }

    private void unregisterAwareSceneRecognize() {
        AwareSceneRecognize recognize = AwareSceneRecognize.getInstance();
        if (recognize != null) {
            recognize.unregisterStateCallback(this.mCpuLimitSceneRecognize);
        }
    }

    public static void enable() {
        mEnabled = true;
        AwareAppCpuLimitMng awareAppCpuLimitMng = mAwareAppCpuLimitMng;
        if (awareAppCpuLimitMng != null) {
            awareAppCpuLimitMng.initialize();
            if (!isCpuLimitEnabled()) {
                mEnabled = false;
                mAwareAppCpuLimitMng.deInitialize();
            }
        }
    }

    public static void disable() {
        mEnabled = false;
        AwareAppCpuLimitMng awareAppCpuLimitMng = mAwareAppCpuLimitMng;
        if (awareAppCpuLimitMng != null) {
            awareAppCpuLimitMng.deInitialize();
        }
    }

    private class AwareAppCpuLimitMngHanlder extends Handler {
        private AwareAppCpuLimitMngHanlder() {
        }

        public void handleMessage(Message msg) {
            if (AwareAppCpuLimitMng.mEnabled) {
                int i = msg.what;
                if (i == 1) {
                    String unused = AwareAppCpuLimitMng.this.mCurPkgName = msg.getData().getString("pkgName");
                    AwareAppCpuLimitMng.this.handleToCpuLimit(1);
                } else if (i == 2) {
                    Bundle data = msg.getData();
                    int uid = data.getInt("uid");
                    AwareAppCpuLimitMng.this.handleToRemovePids(uid, data.getInt("pid"));
                    String unused2 = AwareAppCpuLimitMng.this.mCurPkgName = InnerUtils.getPackageNameByUid(uid);
                } else if (i == 3) {
                    AwareAppCpuLimitMng.this.handleToUnCpuLimit();
                } else if (i != 4) {
                    if (i == 5 && AwareAppCpuLimitMng.this.isCpuLimited.get()) {
                        AwareAppCpuLimitMng.this.mHandler.removeMessages(3);
                        AwareAppCpuLimitMng.this.sendDelayMsg(3, 1000);
                    }
                } else if (!AwareAppCpuLimitMng.this.mIsMultiWin.get()) {
                    AwareAppCpuLimitMng.this.handleToCpuLimit(4);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendDelayMsg(int msgType, int duration) {
        Message msg = this.mHandler.obtainMessage();
        msg.what = msgType;
        this.mHandler.sendMessageDelayed(msg, (long) duration);
    }

    /* access modifiers changed from: private */
    public void handleToCpuLimit(int msgType) {
        AwareAppDefaultCpuLimit.getInstance().doLimitCpu(this.mCurPkgName, AppMngConstant.AppCpuLimitSource.CPULIMIT);
        if (!this.isCpuLimited.get()) {
            this.isCpuLimited.set(true);
        } else {
            this.mHandler.removeMessages(3);
        }
        if (msgType != 4) {
            sendDelayMsg(3, this.mCpuLimitDuration);
        }
    }

    /* access modifiers changed from: private */
    public void handleToUnCpuLimit() {
        if (this.isCpuLimited.get()) {
            AwareAppDefaultCpuLimit.getInstance().doUnLimitCPU();
            this.isCpuLimited.set(false);
        }
    }

    /* access modifiers changed from: private */
    public void handleToRemovePids(int uid, int pid) {
        if (this.isCpuLimited.get()) {
            AwareAppDefaultCpuLimit.getInstance().doRemoveCpuPids(uid, pid, true);
        }
    }

    private class CpuLimitSceneRecognize implements AwareSceneRecognize.IAwareSceneRecCallback {
        private CpuLimitSceneRecognize() {
        }

        @Override // com.android.server.rms.iaware.appmng.AwareSceneRecognize.IAwareSceneRecCallback
        public void onStateChanged(int sceneType, int eventType, String pkgName) {
            Message msg = AwareAppCpuLimitMng.this.mHandler.obtainMessage();
            msg.what = 0;
            if (eventType == 1) {
                if (sceneType == 2) {
                    msg.what = 4;
                    if (AwareAppCpuLimitMng.this.mEnableCpuLimitOptSwitch.get() && !AwareAppCpuLimitMng.this.mIsMultiWin.get()) {
                        AwareAppCpuLimitMng.this.handleToCpuLimit(4);
                        return;
                    }
                } else if (sceneType == 4) {
                    msg.what = 1;
                    if (AwareAppCpuLimitMng.this.mEnableCpuLimitOptSwitch.get()) {
                        String unused = AwareAppCpuLimitMng.this.mCurPkgName = pkgName;
                        AwareAppCpuLimitMng.this.handleToCpuLimit(1);
                        return;
                    }
                }
            } else if (sceneType == 2) {
                msg.what = 5;
            }
            if (msg.what != 0) {
                Bundle data = new Bundle();
                data.putString("pkgName", pkgName);
                msg.setData(data);
                AwareAppCpuLimitMng.this.mHandler.sendMessage(msg);
            }
        }
    }

    public void report(int eventId, Bundle bundleArgs) {
        if (mEnabled && bundleArgs != null) {
            if (!this.mIsInitialized.get()) {
                initialize();
            }
            if (eventId == 1 || eventId == 2) {
                int callerPid = bundleArgs.getInt("callPid");
                int targetUid = bundleArgs.getInt("tgtUid");
                if (targetUid >= 0 && !AwareAppDefaultCpuLimit.getInstance().isPidLimited(callerPid)) {
                    if (targetUid != 1000) {
                        handleToRemovePids(targetUid, 0);
                        return;
                    }
                    handleToRemovePids(targetUid, AwareAppAssociate.getInstance().getPidByNameAndUid(bundleArgs.getString("tgtProcName"), targetUid));
                }
            } else if (eventId == 34) {
                handleToCpuLimit(6);
            }
        }
    }

    private void registerObserver() {
        AwareCallback.getInstance().registerProcessObserver(this.mCpuLimitObserver);
    }

    private void unregisterObserver() {
        AwareCallback.getInstance().unregisterProcessObserver(this.mCpuLimitObserver);
    }

    class AppCpuLimitObserver extends IProcessObserver.Stub {
        AppCpuLimitObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities && AwareAppCpuLimitMng.mEnabled) {
                Message msg = AwareAppCpuLimitMng.this.mHandler.obtainMessage();
                msg.what = 2;
                Bundle data = msg.getData();
                data.putInt("uid", uid);
                data.putInt("pid", pid);
                AwareAppCpuLimitMng.this.mHandler.sendMessage(msg);
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
            if (AwareAppCpuLimitMng.mEnabled && AwareAppCpuLimitMng.this.isCpuLimited.get()) {
                AwareAppDefaultCpuLimit.getInstance().doRemoveCpuPids(uid, pid, false);
            }
        }
    }

    private class AppCpuLimitCallBackHandler extends IMWThirdpartyCallback.Stub {
        private AppCpuLimitCallBackHandler() {
        }

        public void onModeChanged(boolean aMWStatus) {
            AwareAppCpuLimitMng.this.mIsMultiWin.set(aMWStatus);
            if (!AwareAppCpuLimitMng.this.mIsMultiWin.get()) {
                String unused = AwareAppCpuLimitMng.this.mCurPkgName = "";
            }
            AwareLog.d(AwareAppCpuLimitMng.TAG, "mIsMultiWin:" + AwareAppCpuLimitMng.this.mIsMultiWin.get());
        }

        public void onZoneChanged() {
        }

        public void onSizeChanged() {
        }
    }
}
