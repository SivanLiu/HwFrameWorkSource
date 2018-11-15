package com.android.server.hidata.hiradio;

import android.content.Context;
import android.net.wifi.RssiPacketCountInfo;
import android.os.Binder;
import android.os.Bundle;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.util.SparseArray;
import com.android.server.hidata.HwHidataJniAdapter;
import com.android.server.hidata.IHidataCallback;
import com.android.server.hidata.arbitration.HwArbitrationFunction;
import com.android.server.rms.shrinker.ProcessStopShrinker;
import huawei.android.net.hwmplink.HwHiDataCommonUtils;

public class HwWifiBoost {
    private static final String TAG = "HiData_HwWifiBoost";
    private static HwWifiBoost mHwWifiBoost = null;
    private SparseArray<Integer> mBGLimitModeRecords;
    private Context mContext;
    private boolean mGameBoosting = false;
    private IHidataCallback mHidataCallback;
    private HwHidataJniAdapter mHwHidataJniAdapter;
    private boolean mStreamingBoosting = false;

    private HwWifiBoost(Context context) {
        this.mContext = context;
    }

    public static synchronized HwWifiBoost getInstance(Context context) {
        HwWifiBoost hwWifiBoost;
        synchronized (HwWifiBoost.class) {
            if (mHwWifiBoost == null) {
                mHwWifiBoost = new HwWifiBoost(context);
            }
            hwWifiBoost = mHwWifiBoost;
        }
        return hwWifiBoost;
    }

    public synchronized void initialBGLimitModeRecords() {
        this.mBGLimitModeRecords = new SparseArray();
        this.mBGLimitModeRecords.append(1, Integer.valueOf(0));
        this.mBGLimitModeRecords.append(2, Integer.valueOf(0));
        this.mBGLimitModeRecords.append(3, Integer.valueOf(0));
        this.mBGLimitModeRecords.append(4, Integer.valueOf(0));
    }

    private synchronized int getBGLimitMaxMode() {
        if (this.mBGLimitModeRecords == null || this.mBGLimitModeRecords.size() == 0) {
            HwHiDataCommonUtils.logD(TAG, " mBGLimitModeRecords is null");
            return -1;
        }
        int size = this.mBGLimitModeRecords.size();
        int max_mode = 0;
        for (int i = 0; i < size; i++) {
            int key = this.mBGLimitModeRecords.keyAt(i);
            if (((Integer) this.mBGLimitModeRecords.get(key)).intValue() > max_mode) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("BG Limit id=");
                stringBuilder.append(key);
                stringBuilder.append(" mode=");
                stringBuilder.append(this.mBGLimitModeRecords.get(key));
                stringBuilder.append(" > max mode=");
                stringBuilder.append(max_mode);
                HwHiDataCommonUtils.logD(str, stringBuilder.toString());
                max_mode = ((Integer) this.mBGLimitModeRecords.get(key)).intValue();
            }
        }
        return max_mode;
    }

    private synchronized void dumpModeTable() {
        if (this.mBGLimitModeRecords == null || this.mBGLimitModeRecords.size() == 0) {
            HwHiDataCommonUtils.logD(TAG, " mBGLimitModeRecords is null");
            return;
        }
        int size = this.mBGLimitModeRecords.size();
        for (int i = 0; i < size; i++) {
            int key = this.mBGLimitModeRecords.keyAt(i);
        }
    }

    public synchronized void limitedSpeed(int controlId, int enable, int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LimitedSpeed: ");
        stringBuilder.append(enable);
        stringBuilder.append(" mode=");
        stringBuilder.append(mode);
        HwHiDataCommonUtils.logD(str, stringBuilder.toString());
        if (HwArbitrationFunction.isInVPNMode(this.mContext) && enable == 1) {
            HwHiDataCommonUtils.logD(TAG, "Vpn Connected,can not limit speed!");
            return;
        }
        this.mBGLimitModeRecords.put(controlId, Integer.valueOf(enable == 0 ? 0 : mode));
        dumpModeTable();
        int cmd_mode = getBGLimitMaxMode();
        Bundle args = new Bundle();
        args.putInt("enbale", enable);
        args.putInt(ProcessStopShrinker.MODE_KEY, cmd_mode);
        CollectData data = new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_NET_MANAGE), System.currentTimeMillis(), args);
        long id = Binder.clearCallingIdentity();
        HwSysResManager.getInstance().reportData(data);
        Binder.restoreCallingIdentity(id);
    }

    public synchronized void highPriorityTransmit(int uid, int type, int enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("highPriorityTransmit uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(" enable: ");
        stringBuilder.append(enable);
        HwHiDataCommonUtils.logD(str, stringBuilder.toString());
        this.mHwHidataJniAdapter = HwHidataJniAdapter.getInstance();
        this.mHwHidataJniAdapter.setDpiMarkRule(uid, type, enable);
    }

    public synchronized void setPMMode(int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPMMode:  mode: ");
        stringBuilder.append(mode);
        HwHiDataCommonUtils.logD(str, stringBuilder.toString());
        this.mHidataCallback.onSetPMMode(mode);
    }

    public synchronized void setGameBoostMode(int enable, int uid, int type, int limitMode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setGameBoostMode:  enable: ");
        stringBuilder.append(enable);
        stringBuilder.append(" uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(" type: ");
        stringBuilder.append(type);
        HwHiDataCommonUtils.logD(str, stringBuilder.toString());
        this.mHidataCallback.onSetTXPower(enable);
        this.mHidataCallback.onSetPMMode(enable == 1 ? 4 : 3);
        highPriorityTransmit(uid, type, enable);
        limitedSpeed(1, enable, limitMode);
    }

    public synchronized void setStreamingBoostMode(int enable, int uid, int type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setStreamingBoostMode:  enable: ");
        stringBuilder.append(enable);
        stringBuilder.append(" uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(" type: ");
        stringBuilder.append(type);
        HwHiDataCommonUtils.logD(str, stringBuilder.toString());
        this.mHidataCallback.onSetTXPower(enable);
        highPriorityTransmit(uid, type, enable);
    }

    public synchronized void startGameBoost(int uid) {
        if (!isGameBoosting()) {
            setGameBoostMode(1, uid, 17, 1);
            this.mGameBoosting = true;
        }
    }

    public synchronized void stopGameBoost(int uid) {
        if (isGameBoosting()) {
            setGameBoostMode(0, uid, 17, 0);
            this.mGameBoosting = false;
        }
    }

    public synchronized void startStreamingBoost(int uid) {
        if (!isStreamingBoosting()) {
            setStreamingBoostMode(1, uid, 17);
            this.mStreamingBoosting = true;
        }
    }

    public synchronized void stopStreamingBoost(int uid) {
        if (isStreamingBoosting()) {
            setStreamingBoostMode(0, uid, 17);
            this.mStreamingBoosting = false;
        }
    }

    public synchronized void pauseABSHandover() {
        this.mHidataCallback.onPauseABSHandover();
    }

    public synchronized void restartABSHandover() {
        this.mHidataCallback.onRestartABSHandover();
    }

    public synchronized boolean isGameBoosting() {
        return this.mGameBoosting;
    }

    public synchronized boolean isStreamingBoosting() {
        return this.mStreamingBoosting;
    }

    public synchronized void stopLimitSpeed() {
        limitedSpeed(1, 0, 0);
        initialBGLimitModeRecords();
    }

    public synchronized void stopAllBoost() {
        highPriorityTransmit(-1, 17, 0);
        this.mHidataCallback.onSetTXPower(0);
        this.mHidataCallback.onSetPMMode(3);
        restartABSHandover();
        stopLimitSpeed();
    }

    public synchronized void registWifiBoostCallback(IHidataCallback callback) {
        if (this.mHidataCallback == null) {
            this.mHidataCallback = callback;
        }
    }

    public synchronized RssiPacketCountInfo getOTAInfo() {
        return this.mHidataCallback.onGetOTAInfo();
    }
}
