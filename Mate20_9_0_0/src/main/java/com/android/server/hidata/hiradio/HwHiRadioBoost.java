package com.android.server.hidata.hiradio;

import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.server.hidata.appqoe.HwAPPStateInfo;
import com.android.server.hidata.arbitration.HwArbitrationCommonUtils;
import com.huawei.android.bastet.IBastetManager;
import com.huawei.android.bastet.IBastetManager.Stub;

public class HwHiRadioBoost {
    private static final String BASTET_SERVICE = "BastetService";
    public static final int BSR_ACCELERATE_ENABLED = 2;
    public static final int DATA_ACCELERATE_ENABLED = 1;
    public static final int HIREADIO_BOOST_ACTION_BG_OR_FG = 512;
    public static final int HIREADIO_BOOST_ACTION_ENABLE = 256;
    public static final int NO_ACTION_ENABLED = 0;
    private static final String TAG = "HwHiRadioBoost";
    private static HwHiRadioBoost mHwHiRadioBoost;
    private IBinder mBastetService;
    private DeathRecipient mDeathRecipient = new DeathRecipient() {
        public void binderDied() {
            HwArbitrationCommonUtils.logE(HwHiRadioBoost.TAG, "Bastet service has died!");
            synchronized (HwHiRadioBoost.this) {
                if (HwHiRadioBoost.this.mBastetService != null) {
                    HwHiRadioBoost.this.mBastetService.unlinkToDeath(this, 0);
                    HwHiRadioBoost.this.mBastetService = null;
                    HwHiRadioBoost.this.mIBastetManager = null;
                }
            }
        }
    };
    private IBastetManager mIBastetManager;

    public static HwHiRadioBoost createInstance() {
        if (mHwHiRadioBoost == null) {
            mHwHiRadioBoost = new HwHiRadioBoost();
        }
        return mHwHiRadioBoost;
    }

    private HwHiRadioBoost() {
        boolean isConnected = getBastetService();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("init mHwHiRadioBoost completed, isConnected = ");
        stringBuilder.append(isConnected);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
    }

    private boolean getBastetService() {
        synchronized (this) {
            if (this.mBastetService == null) {
                this.mBastetService = ServiceManager.getService(BASTET_SERVICE);
                if (this.mBastetService == null) {
                    HwArbitrationCommonUtils.logE(TAG, "Failed to get bastet service!");
                    return false;
                }
                try {
                    this.mBastetService.linkToDeath(this.mDeathRecipient, 0);
                    this.mIBastetManager = Stub.asInterface(this.mBastetService);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private boolean isDataAccelerateEnabled(int actions) {
        return (actions & 1) == 1;
    }

    private boolean isBSRAccelerateEnabled(int actions) {
        return (actions & 2) == 2;
    }

    public void startOptimizeActionsForApp(HwAPPStateInfo AppInfo, int actions) {
        String str;
        StringBuilder stringBuilder;
        if (AppInfo == null || actions <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Enter startOptimizeActionsForApp: AppInfo is null, actions is ");
            stringBuilder.append(actions);
            HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("startOptimizeActionsForApp Enter: AppInfo.uid is ");
        stringBuilder.append(AppInfo.mAppUID);
        stringBuilder.append("AppInfo.mAppState is ");
        stringBuilder.append(AppInfo.mAppState);
        stringBuilder.append("actions is ");
        stringBuilder.append(actions);
        HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
        if (isDataAccelerateEnabled(actions) || isBSRAccelerateEnabled(actions)) {
            configDataAccelerate(AppInfo.mAppUID, true, true, (actions & 1) | (actions & 2));
        }
    }

    public void stopOptimizedActionsForApp(HwAPPStateInfo AppInfo, boolean fgBgState, int actions) {
        String str;
        StringBuilder stringBuilder;
        if (AppInfo == null || actions < 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Enter stopOptimizedActionsForApp: AppInfo is null, actions is ");
            stringBuilder.append(actions);
            HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("stopOptimizedActionsForApp Enter: AppInfo.uid is ");
        stringBuilder.append(AppInfo.mAppUID);
        stringBuilder.append("AppInfo.mAppState is ");
        stringBuilder.append(AppInfo.mAppState);
        stringBuilder.append("actions is ");
        stringBuilder.append(actions);
        HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
        if (isDataAccelerateEnabled(actions) || isBSRAccelerateEnabled(actions)) {
            configDataAccelerate(AppInfo.mAppUID, fgBgState, false, (actions & 1) | (actions & 2));
        }
    }

    private void configDataAccelerate(int uid, boolean fgBgState, boolean enable, int action) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("configDataAccelerate Enter, uid is ");
        stringBuilder.append(uid);
        stringBuilder.append("action is ");
        stringBuilder.append(action);
        HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
        if (enable) {
            action += 256;
        }
        if (!fgBgState) {
            action += 512;
        }
        try {
            getBastetService();
            synchronized (this) {
                if (this.mIBastetManager != null && this.mIBastetManager.configDataAccelerate(uid, action) == 0) {
                    HwArbitrationCommonUtils.logD(TAG, "configDataAccelerate success");
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
