package com.huawei.hwwifiproservice;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.util.ArrayList;

public class HwDualBandManager {
    private static HwDualBandManager mHwDualBandManager = null;
    private static HwDualBandStateMachine mHwDualBandStateMachine = null;

    private HwDualBandManager(Context context, IDualBandManagerCallback dbCallBack) {
        mHwDualBandStateMachine = new HwDualBandStateMachine(context, dbCallBack);
    }

    public static HwDualBandStateMachine getHwDualBandStateMachine() {
        return mHwDualBandStateMachine;
    }

    public static HwDualBandManager createInstance(Context context, IDualBandManagerCallback dbCallBack) {
        if (mHwDualBandManager == null) {
            mHwDualBandManager = new HwDualBandManager(context, dbCallBack);
        }
        Log.i(HwDualBandMessageUtil.TAG, "HwDualBandManager init Complete!");
        return mHwDualBandManager;
    }

    public static HwDualBandManager getInstance() {
        return mHwDualBandManager;
    }

    public boolean startDualBandManger() {
        mHwDualBandStateMachine.onStart();
        return true;
    }

    public boolean stopDualBandManger() {
        mHwDualBandStateMachine.onStop();
        return true;
    }

    public boolean isDualbandScanning() {
        HwDualBandStateMachine hwDualBandStateMachine = mHwDualBandStateMachine;
        if (hwDualBandStateMachine == null) {
            return false;
        }
        return hwDualBandStateMachine.isDualbandScanning();
    }

    public boolean startMonitor(ArrayList<HwDualBandMonitorInfo> apList) {
        if (apList.size() == 0) {
            Log.e(HwDualBandMessageUtil.TAG, "startMonitor apList.size() == 0");
            return false;
        }
        Handler mHandler = mHwDualBandStateMachine.getStateMachineHandler();
        Bundle data = new Bundle();
        data.putParcelableArrayList(HwDualBandMessageUtil.MSG_KEY_APLIST, (ArrayList) apList.clone());
        Message msg = Message.obtain();
        msg.what = 102;
        msg.setData(data);
        mHandler.sendMessage(msg);
        return true;
    }

    public boolean stopMonitor() {
        mHwDualBandStateMachine.getStateMachineHandler().sendEmptyMessage(103);
        return true;
    }

    public void updateCurrentRssi(int rssi) {
        Bundle data = new Bundle();
        data.putInt(HwDualBandMessageUtil.MSG_KEY_RSSI, rssi);
        Message msg = Message.obtain();
        msg.what = 18;
        msg.setData(data);
        mHwDualBandStateMachine.getStateMachineHandler().sendMessage(msg);
    }
}
