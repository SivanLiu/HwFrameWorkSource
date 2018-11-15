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
        if (this.mGameConfig == null) {
            HwAPPQoEUtils.logD(TAG, "updateGameRTT mGameConfig == null");
            return;
        }
        int kqi = this.mGameConfig.mGameKQI * 1000;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateGameRTT rtt = ");
        stringBuilder.append(rtt);
        stringBuilder.append(" mGameConfig.mGameRtt = ");
        stringBuilder.append(this.mGameConfig.mGameRtt);
        stringBuilder.append(" kqi = ");
        stringBuilder.append(kqi);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        if (rtt > this.mGameConfig.mGameRtt) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(107, this.mGameStateInfo), (long) kqi);
        } else if (this.mHandler.hasMessages(107, this.mGameStateInfo)) {
            this.mHandler.removeMessages(107);
        }
    }

    public HwAPPStateInfo getCurrentAPPState() {
        return this.mGameStateInfo;
    }
}
