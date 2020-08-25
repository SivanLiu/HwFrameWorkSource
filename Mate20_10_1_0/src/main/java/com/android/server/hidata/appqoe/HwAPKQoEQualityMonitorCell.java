package com.android.server.hidata.appqoe;

import android.common.HwFrameworkFactory;
import android.net.booster.IHwCommBoosterCallback;
import android.net.booster.IHwCommBoosterServiceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;

public class HwAPKQoEQualityMonitorCell {
    public static final int DATA_SEND_TO_HIDATA_APP_QOE_CHR_PARAMS = 2;
    public static final int DATA_SEND_TO_HIDATA_APP_QOE_EXPERIENCE_LEVEL = 1;
    public static final int HIDATA_APP_QOE_EXPERIENCE_LEVEL_BAD = 1;
    public static final int HIDATA_APP_QOE_EXPERIENCE_LEVEL_GOOD = 2;
    public static final int HIDATA_APP_QOE_EXPERIENCE_LEVEL_UNKNOW = 3;
    public static final int HIDATA_GET_CELLULAR_APP_QOE_CHR_PARAMS = 305;
    public static final int HIDATA_START_CELLULAR_APP_QOE_MONITOR = 301;
    public static final int HIDATA_STOP_CELLULAR_APP_QOE_MONITOR = 302;
    /* access modifiers changed from: private */
    public static String TAG = "HiData_HwAPKQoEQualityMonitorCell";
    private static HwAPKQoEQualityMonitorCell mHwAPKQoEQualityMonitorCell = null;
    private IHwCommBoosterServiceManager bm = null;
    /* access modifiers changed from: private */
    public HwAPPStateInfo curAppStateInfo = new HwAPPStateInfo();
    /* access modifiers changed from: private */
    public HwAPPChrExcpInfo mAPPQoEInfo = new HwAPPChrExcpInfo();
    private IHwCommBoosterCallback mIHwCommBoosterCallback = new IHwCommBoosterCallback.Stub() {
        /* class com.android.server.hidata.appqoe.HwAPKQoEQualityMonitorCell.AnonymousClass1 */

        public void callBack(int type, Bundle b) throws RemoteException {
            if (b != null) {
                if (type == 1) {
                    int level = b.getInt("cellularAppQoeExpLevel");
                    HwAPPQoEUtils.logD(HwAPKQoEQualityMonitorCell.TAG, false, "Call Back, level:%{public}d", Integer.valueOf(level));
                    if (1 == level) {
                        HwAPKQoEQualityMonitorCell.this.stmHandler.sendMessage(HwAPKQoEQualityMonitorCell.this.stmHandler.obtainMessage(107, HwAPKQoEQualityMonitorCell.this.curAppStateInfo));
                    } else if (2 == level) {
                        HwAPKQoEQualityMonitorCell.this.stmHandler.sendMessage(HwAPKQoEQualityMonitorCell.this.stmHandler.obtainMessage(106, HwAPKQoEQualityMonitorCell.this.curAppStateInfo));
                    }
                } else if (type == 2) {
                    synchronized (HwAPKQoEQualityMonitorCell.this.mLock) {
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.netType = 801;
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.rtt = b.getInt("rtt", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.txPacket = b.getInt("txPacket", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.txByte = b.getInt("txByte", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.rxPacket = b.getInt("rxPacket", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.rxByte = b.getInt("rxByte", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.rsPacket = b.getInt("rtsPacket", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.para1 = b.getInt("para1", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.para2 = b.getInt("para2", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.para3 = b.getInt("para3", -1);
                        HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.para4 = b.getInt("para4", -1);
                        HwAPPQoEUtils.logD(HwAPKQoEQualityMonitorCell.TAG, false, "DATA_SEND_TO_HIDATA_APP_QOE_CHR_PARAMS, mAPPQoEInfo:%{public}s", HwAPKQoEQualityMonitorCell.this.mAPPQoEInfo.toString());
                    }
                    HwAPKQoEQualityMonitorCell.this.stmHandler.sendEmptyMessage(204);
                }
            }
        }
    };
    private HwAPPQoEUserLearning mLearningManager;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    /* access modifiers changed from: private */
    public Handler stmHandler = null;

    private HwAPKQoEQualityMonitorCell(Handler handler) {
        this.stmHandler = handler;
        initCommBoosterManager();
    }

    protected static HwAPKQoEQualityMonitorCell createQualityMonitorCell(Handler handler) {
        if (mHwAPKQoEQualityMonitorCell == null) {
            mHwAPKQoEQualityMonitorCell = new HwAPKQoEQualityMonitorCell(handler);
        }
        return mHwAPKQoEQualityMonitorCell;
    }

    public static HwAPKQoEQualityMonitorCell getInstance() {
        return mHwAPKQoEQualityMonitorCell;
    }

    public void startMonitor(HwAPPStateInfo newAPPStateInfo) {
        if (newAPPStateInfo == null) {
            HwAPPQoEUtils.logD(TAG, false, "startMonitor, input null", new Object[0]);
            return;
        }
        HwAPPQoEUtils.logD(TAG, false, "startAPPMonitor -- newAPPStateInfo:%{public}s", newAPPStateInfo.toString());
        if (newAPPStateInfo.mAppId != this.curAppStateInfo.mAppId) {
            sendScenceStartToCellMonitor(newAPPStateInfo);
        } else if (newAPPStateInfo.mAppPeriod != this.curAppStateInfo.mAppPeriod) {
            sendScenceEndToCellMonitor(this.curAppStateInfo);
            sendScenceStartToCellMonitor(newAPPStateInfo);
        }
        this.curAppStateInfo.copyObjectValue(newAPPStateInfo);
    }

    public void stopMonitor() {
        HwAPPQoEUtils.logD(TAG, false, "stopAPPMonitor -- curAppStateInfo:%{public}s", this.curAppStateInfo.toString());
        sendScenceEndToCellMonitor(this.curAppStateInfo);
        this.curAppStateInfo = new HwAPPStateInfo();
    }

    public HwAPPChrExcpInfo getAPPQoEInfo() {
        HwAPPChrExcpInfo hwAPPChrExcpInfo;
        synchronized (this.mLock) {
            HwAPPQoEUtils.logD(TAG, false, "curAPPQoEInfo:%{public}s", this.mAPPQoEInfo.toString());
            hwAPPChrExcpInfo = this.mAPPQoEInfo;
        }
        return hwAPPChrExcpInfo;
    }

    public void sendScenceStartToCellMonitor(HwAPPStateInfo newAPPStateInfo) {
        int appPeriod;
        HwAPPQoEUtils.logD(TAG, false, "sendScenceStartToCellMonitor enter", new Object[0]);
        if (this.bm == null) {
            HwAPPQoEUtils.logD(TAG, false, "sendScenceStartToCellMonitor:null HwCommBoosterServiceManager", new Object[0]);
        } else if (-1 != newAPPStateInfo.mAppPeriod) {
            HwAPPQoEUserLearning hwAPPQoEUserLearning = this.mLearningManager;
            if (hwAPPQoEUserLearning == null) {
                appPeriod = newAPPStateInfo.mAppPeriod * 1000;
            } else if (hwAPPQoEUserLearning.getUserTypeByAppId(newAPPStateInfo.mAppId) == 1) {
                HwAPPQoEUtils.logD(TAG, false, " sendScenceStartToCellMonitor is a COMMON user", new Object[0]);
                appPeriod = newAPPStateInfo.mAppPeriod * 2 * 1000;
            } else {
                appPeriod = newAPPStateInfo.mAppPeriod * 1000;
                HwAPPQoEUtils.logD(TAG, false, " sendScenceStartToCellMonitor is a USER_TYPE_RADICAL user", new Object[0]);
            }
            HwAPPQoEUtils.logD(TAG, false, "sendScenceStartToCellMonitor:appPeriod is %{public}d", Integer.valueOf(appPeriod));
            Bundle data = new Bundle();
            data.putInt("appUid", newAPPStateInfo.mAppUID);
            data.putInt("qoeInfoReportPeriod", appPeriod);
            int ret = this.bm.reportBoosterPara("com.android.server.hidata.appqoe", 301, data);
            if (ret != 0) {
                HwAPPQoEUtils.logD(TAG, false, "reportBoosterPara failed, ret=%{public}d", Integer.valueOf(ret));
            }
        } else {
            HwAPPQoEUtils.logD(TAG, false, "sendScenceStartToCellMonitor:invalid appPeriod", new Object[0]);
        }
    }

    public void sendScenceEndToCellMonitor(HwAPPStateInfo newAPPStateInfo) {
        HwAPPQoEUtils.logD(TAG, false, "sendScenceEndToCellMonitor enter", new Object[0]);
        if (this.bm == null) {
            HwAPPQoEUtils.logD(TAG, false, "sendScenceEndToCellMonitor:null HwCommBoosterServiceManager", new Object[0]);
            return;
        }
        Bundle data = new Bundle();
        data.putInt("appUid", newAPPStateInfo.mAppUID);
        data.putInt("qoeInfoReportPeriod", -1);
        int ret = this.bm.reportBoosterPara("com.android.server.hidata.appqoe", 302, data);
        if (ret != 0) {
            HwAPPQoEUtils.logD(TAG, false, "reportBoosterPara failed, ret=%{public}d", Integer.valueOf(ret));
        }
    }

    public void sendGetAppQoeChrMsg() {
        HwAPPQoEUtils.logD(TAG, false, "sendGetAppQoeChrMsg enter", new Object[0]);
        if (this.bm == null) {
            HwAPPQoEUtils.logD(TAG, false, "sendGetAppQoeChrMsg:null HwCommBoosterServiceManager", new Object[0]);
            return;
        }
        Bundle data = new Bundle();
        data.putInt("getChr", 1);
        int ret = this.bm.reportBoosterPara("com.android.server.hidata.appqoe", 305, data);
        if (ret != 0) {
            HwAPPQoEUtils.logD(TAG, false, "reportBoosterPara failed, ret=%{public}d", Integer.valueOf(ret));
        }
    }

    private void registerBoosterCallback() {
        HwAPPQoEUtils.logD(TAG, false, "registerBoosterCallback enter", new Object[0]);
        IHwCommBoosterServiceManager iHwCommBoosterServiceManager = this.bm;
        if (iHwCommBoosterServiceManager != null) {
            int ret = iHwCommBoosterServiceManager.registerCallBack("com.android.server.hidata.appqoe", this.mIHwCommBoosterCallback);
            if (ret != 0) {
                HwAPPQoEUtils.logD(TAG, false, "registerBoosterCallback:registerCallBack failed, ret=%{public}d", Integer.valueOf(ret));
                return;
            }
            return;
        }
        HwAPPQoEUtils.logD(TAG, false, "registerBoosterCallback:null HwCommBoosterServiceManager", new Object[0]);
    }

    private void initCommBoosterManager() {
        this.bm = HwFrameworkFactory.getHwCommBoosterServiceManager();
        this.mLearningManager = HwAPPQoEUserLearning.getInstance();
        registerBoosterCallback();
    }
}
