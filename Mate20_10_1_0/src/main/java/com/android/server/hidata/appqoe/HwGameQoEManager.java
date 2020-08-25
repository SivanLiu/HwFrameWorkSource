package com.android.server.hidata.appqoe;

import android.content.Context;
import android.os.Handler;

public class HwGameQoEManager {
    private static String TAG = "HiData_HwGameQoEManager";
    private HwAPPQoEGameConfig mGameConfig;
    public int mGameId;
    public HwAPPStateInfo mGameStateInfo = new HwAPPStateInfo();
    private Handler mHandler;
    private HwAPPQoEResourceManger mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();

    public HwGameQoEManager(Context context, Handler handler) {
        this.mHandler = handler;
    }

    public void startMonitor(HwAPPStateInfo appStateInfo) {
        if (this.mHandler.hasMessages(107, this.mGameStateInfo) && this.mGameStateInfo.mScenceId != appStateInfo.mScenceId) {
            this.mHandler.removeMessages(107);
        }
        this.mGameStateInfo.copyObjectValue(appStateInfo);
        this.mGameId = appStateInfo.mAppId;
        this.mGameConfig = this.mHwAPPQoEResourceManger.getGameScenceConfig(appStateInfo.mAppId);
    }

    public void stopMonitor() {
        if (this.mHandler.hasMessages(107, this.mGameStateInfo)) {
            this.mHandler.removeMessages(107);
        }
    }

    public void updateGameRTT(int rtt) {
        HwAPPQoEGameConfig hwAPPQoEGameConfig = this.mGameConfig;
        if (hwAPPQoEGameConfig == null) {
            HwAPPQoEUtils.logD(TAG, false, "updateGameRTT mGameConfig == null", new Object[0]);
            return;
        }
        int kqi = hwAPPQoEGameConfig.mGameKQI * 1000;
        HwAPPQoEUtils.logD(TAG, false, "updateGameRTT rtt = %{public}d mGameConfig.mGameRtt = %{public}d kqi = %{public}d", Integer.valueOf(rtt), Integer.valueOf(this.mGameConfig.mGameRtt), Integer.valueOf(kqi));
        HwAPPStateInfo hwAPPStateInfo = this.mGameStateInfo;
        hwAPPStateInfo.mScenceId = 200002;
        if (hwAPPStateInfo.mAppId == 2001) {
            HwAPPQoEUtils.logD(TAG, false, "SGAME not report BAD", new Object[0]);
        } else if (rtt > this.mGameConfig.mGameRtt) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(107, this.mGameStateInfo), (long) kqi);
        } else if (this.mHandler.hasMessages(107, this.mGameStateInfo)) {
            this.mHandler.removeMessages(107);
        }
    }

    public HwAPPStateInfo getCurrentAPPState() {
        return this.mGameStateInfo;
    }
}
