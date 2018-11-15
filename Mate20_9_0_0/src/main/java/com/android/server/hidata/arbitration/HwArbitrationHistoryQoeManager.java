package com.android.server.hidata.arbitration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.android.server.hidata.wavemapping.IWaveMappingCallback;
import com.android.server.hidata.wavemapping.cons.Constant;

public class HwArbitrationHistoryQoeManager {
    private static final String TAG = "HiDATA_HwArbitrationHistoryQoeManager";
    private static final float THRESHOLD_GOOD_RATIO = 0.2f;
    private static HwArbitrationHistoryQoeManager instance = null;
    private Handler mWmpStateMachineHandler;

    private HwArbitrationHistoryQoeManager(Handler handler) {
        this.mWmpStateMachineHandler = handler;
        HwArbitrationCommonUtils.logI(TAG, "HwArbitrationHistoryQoeManager init finish.");
    }

    public static synchronized HwArbitrationHistoryQoeManager getInstance(Handler handler) {
        HwArbitrationHistoryQoeManager hwArbitrationHistoryQoeManager;
        synchronized (HwArbitrationHistoryQoeManager.class) {
            if (instance == null) {
                instance = new HwArbitrationHistoryQoeManager(handler);
            }
            hwArbitrationHistoryQoeManager = instance;
        }
        return hwArbitrationHistoryQoeManager;
    }

    public void queryHistoryQoE(int UID, int appId, int scence, int networkType, IWaveMappingCallback callback) {
        int net;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryHistoryQoE enter, UID=");
        stringBuilder.append(UID);
        stringBuilder.append(", appId=");
        stringBuilder.append(appId);
        stringBuilder.append(", scence=");
        stringBuilder.append(scence);
        stringBuilder.append(", nw=");
        stringBuilder.append(networkType);
        HwArbitrationCommonUtils.logI(str, stringBuilder.toString());
        Bundle data = new Bundle();
        int fullAppId = scence;
        if (200000 <= scence) {
            fullAppId = Constant.transferGameId2FullId(appId, scence);
        }
        if (800 == networkType) {
            net = 1;
        } else if (801 == networkType) {
            net = 0;
        } else {
            net = 8;
        }
        data.putInt("FULLID", fullAppId);
        data.putInt("UID", UID);
        data.putInt("NW", net);
        data.putInt("ArbNW", networkType);
        Message msg = Message.obtain(this.mWmpStateMachineHandler, 120);
        msg.setData(data);
        msg.obj = callback;
        this.mWmpStateMachineHandler.sendMessage(msg);
    }
}
