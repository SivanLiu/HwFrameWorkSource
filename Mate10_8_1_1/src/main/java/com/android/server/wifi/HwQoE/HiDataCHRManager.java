package com.android.server.wifi.HwQoE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import com.android.server.wifi.HwWifiCHRStateManager;
import com.android.server.wifi.HwWifiCHRStateManagerImpl;

public class HiDataCHRManager {
    private static final String TAG = "HiDATA_CHR";
    private static final int UPLOAD_TIME_INTERVAL = 86400000;
    private static final int WIFI_CONNECTED_MESSAGE = 1;
    private static HiDataCHRManager mHiDataCHRManager = null;
    private IntentFilter intentFilter = new IntentFilter();
    private boolean isBootComplete = false;
    private BroadcastReceiver mBroadcastReceiver = new WifiBroadcastReceiver();
    private Context mContext;
    private HiDataCHRHandoverInfo mHandoverException = null;
    private HiDataCHRMachineInfo mHiDataCHRMachineInfo = null;
    private HwQoEQualityManager mHwQoEQualityManager = null;
    private boolean mIsAudioNeedReset = false;
    private boolean mIsHandoverExceptionUpload = false;
    private boolean mIsMachineInfoUpload = false;
    private boolean mIsStallExceptionUpload = false;
    private boolean mIsVideoNeedReset = false;
    private HiDataCHRStallInfo mStallException = null;

    private class WifiBroadcastReceiver extends BroadcastReceiver {
        private WifiBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (netInfo != null && netInfo.getState() == State.CONNECTED) {
                    HiDataCHRManager.this.uploadHidataStatistics();
                }
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                HiDataCHRManager.this.isBootComplete = true;
            }
        }
    }

    private HiDataCHRManager(Context context) {
        this.mContext = context;
        this.mHwQoEQualityManager = HwQoEQualityManager.getInstance(context);
        HiDataCHRStatisticsInfo statistics = this.mHwQoEQualityManager.getWeChatStatistics(1);
        registerBroadcastReceiver();
        if (statistics == null) {
            statistics = new HiDataCHRStatisticsInfo(1);
            statistics.mLastUploadTime = System.currentTimeMillis();
            this.mHwQoEQualityManager.initWeChatStatistics(statistics);
            statistics = new HiDataCHRStatisticsInfo(2);
            statistics.mLastUploadTime = System.currentTimeMillis();
            this.mHwQoEQualityManager.initWeChatStatistics(statistics);
        }
    }

    private void registerBroadcastReceiver() {
        this.intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.intentFilter);
    }

    private void uploadHidataStatistics() {
        if (isNeedToUpload(getWeChatVideoStatistics())) {
            HwWifiCHRStateManager mHwWifiCHRStateManager = HwWifiCHRStateManagerImpl.getDefault();
            mHwWifiCHRStateManager.uploadDFTEvent(909002040, null);
            this.mIsVideoNeedReset = true;
            mHwWifiCHRStateManager.uploadDFTEvent(909002041, null);
            this.mIsAudioNeedReset = true;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isNeedToUpload(HiDataCHRStatisticsInfo record) {
        if (record == null || (this.isBootComplete ^ 1) != 0 || System.currentTimeMillis() - record.mLastUploadTime <= 86400000) {
            return false;
        }
        return true;
    }

    public static HiDataCHRManager getInstance(Context context) {
        if (mHiDataCHRManager == null) {
            mHiDataCHRManager = new HiDataCHRManager(context);
        }
        return mHiDataCHRManager;
    }

    public void updateWeChatStatistics(HiDataCHRStatisticsInfo statistics) {
        this.mHwQoEQualityManager.updateWeChatStatistics(statistics);
    }

    public HiDataCHRStatisticsInfo getWeChatVideoStatistics() {
        HwQoEUtils.logD("getWeChatVideoStatistics mIsVideoNeedReset = " + this.mIsVideoNeedReset);
        HiDataCHRStatisticsInfo info = this.mHwQoEQualityManager.getWeChatStatistics(1);
        if (this.mIsVideoNeedReset) {
            HiDataCHRStatisticsInfo resetStatistics = new HiDataCHRStatisticsInfo(1);
            resetStatistics.mLastUploadTime = System.currentTimeMillis();
            this.mHwQoEQualityManager.updateWeChatStatistics(resetStatistics);
            this.mIsVideoNeedReset = false;
        }
        return info;
    }

    public HiDataCHRStatisticsInfo getWeChatAudioStatistics() {
        HwQoEUtils.logD("getWeChatAudioStatistics mIsAudioNeedReset = " + this.mIsAudioNeedReset);
        HiDataCHRStatisticsInfo info = this.mHwQoEQualityManager.getWeChatStatistics(2);
        if (this.mIsAudioNeedReset) {
            HiDataCHRStatisticsInfo resetStatistics = new HiDataCHRStatisticsInfo(2);
            resetStatistics.mLastUploadTime = System.currentTimeMillis();
            this.mHwQoEQualityManager.updateWeChatStatistics(resetStatistics);
            this.mIsAudioNeedReset = false;
        }
        return info;
    }

    public void uploadWeChatStallInfo(HiDataCHRStallInfo exception) {
        HwWifiCHRStateManagerImpl.getDefault().uploadDFTEvent(909002025, "STREAMING_SLOWLY_BAD_QOE");
        this.mStallException = exception;
        this.mIsStallExceptionUpload = true;
    }

    public HiDataCHRStallInfo getWeChatStallInfo() {
        HwQoEUtils.logD("getWeChatStallInfo = " + this.mIsStallExceptionUpload);
        if (!this.mIsStallExceptionUpload) {
            return null;
        }
        this.mIsStallExceptionUpload = false;
        return this.mStallException;
    }

    public void uploadWeChatHandoverInfo(HiDataCHRHandoverInfo exception) {
        HwWifiCHRStateManagerImpl.getDefault().uploadDFTEvent(909002039, null);
        this.mHandoverException = exception;
        this.mIsHandoverExceptionUpload = true;
    }

    public HiDataCHRHandoverInfo getWeChatHandoverInfo() {
        HwQoEUtils.logD("getWeChatHandoverInfo mIsHandoverExceptionUpload = " + this.mIsHandoverExceptionUpload);
        if (!this.mIsHandoverExceptionUpload) {
            return null;
        }
        this.mIsHandoverExceptionUpload = false;
        return this.mHandoverException;
    }

    public void uploadWeChatMachineInfo(HiDataCHRMachineInfo info) {
        HwWifiCHRStateManagerImpl.getDefault().uploadDFTEvent(909002042, null);
        this.mHiDataCHRMachineInfo = info;
        this.mIsMachineInfoUpload = true;
    }

    public HiDataCHRMachineInfo getWeChatMachineInfo() {
        HwQoEUtils.logD("getWeChatMachineInfo mIsMachineInfoUpload = " + this.mIsMachineInfoUpload);
        if (!this.mIsMachineInfoUpload) {
            return null;
        }
        this.mIsMachineInfoUpload = false;
        return this.mHiDataCHRMachineInfo;
    }
}
