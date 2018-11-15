package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.hidata.HwQoEUdpNetWorkInfo;

public class HwQoEUdpServiceImpl {
    private static final String HwQoEUdpNetWorkInfo = null;
    private static final int KILO_BYTE = 1024;
    private static final int MAX_UDP_NO_ACCESS_SCORE = 2;
    private static final int MSG_UDP_ACCESS_MONITOR_ENABLE = 101;
    private static final int MSG_UDP_BASE = 100;
    private static final int MSG_UDP_TRAFFIC_SPEED = 102;
    private static final int NO_ACCESS_STATISTICS_UDP_SOCKETS_SUM = 5;
    private static final int NO_ACCESS_STATISTICS_UDP_UID_SUM = 3;
    private static final String TAG = "HwQoEUdpServiceImpl";
    private static final long UDP_ACCESS_MONITOR_INTERVAL = 2000;
    private static final int UID_ALL = 0;
    private int mAddScore;
    HwQoEUdpNetWorkInfo mCurrUdpInfoForMonitor;
    HwQoEUdpNetWorkInfo mCurrUdpInfoForUID;
    UdpNetworkSpeed mCurrUdpSpeed;
    private HwQoEJNIAdapter mHwQoEJNIAdapter = HwQoEJNIAdapter.getInstance();
    private IHwQoEMonitorCallback mHwQoEMonitorCallback;
    private Handler mHwQoEUdpServiceImplHandler;
    private boolean mIsUDPAccessMonitorEnabled;
    HwQoEUdpNetWorkInfo mLastUdpInfoForMonitor;
    private int mUdpNoAccessScore;

    public HwQoEUdpServiceImpl(Context context, IHwQoEMonitorCallback callback) {
        this.mHwQoEMonitorCallback = callback;
        initHwQoEUdpServiceImplHandler();
    }

    public synchronized HwQoEUdpNetWorkInfo getUdpNetworkStatsDetail(int uid) {
        return null;
    }

    public synchronized void setUDPTrafficStatisticEnabled(boolean enabled, int uid) {
    }

    public void setUDPInternetAccessMonitorEnabled(boolean enabled) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setUDPInternetAccessMonitorEnabled:");
        stringBuilder.append(enabled);
        Log.d(str, stringBuilder.toString());
        this.mIsUDPAccessMonitorEnabled = enabled;
        if (!this.mIsUDPAccessMonitorEnabled) {
            this.mUdpNoAccessScore = 0;
            this.mAddScore = 0;
            this.mCurrUdpInfoForMonitor = null;
            this.mLastUdpInfoForMonitor = null;
            if (this.mHwQoEUdpServiceImplHandler.hasMessages(101)) {
                this.mHwQoEUdpServiceImplHandler.removeMessages(101);
            }
        } else if (!this.mHwQoEUdpServiceImplHandler.hasMessages(101)) {
            this.mHwQoEUdpServiceImplHandler.sendEmptyMessage(101);
        }
    }

    private void initHwQoEUdpServiceImplHandler() {
        HandlerThread handlerThread = new HandlerThread("hw_udpqoe_handler_thread");
        handlerThread.start();
        this.mHwQoEUdpServiceImplHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 101:
                        HwQoEUdpServiceImpl.this.mCurrUdpInfoForMonitor = HwQoEUdpServiceImpl.this.mHwQoEJNIAdapter.getUdpNetworkStatsDetail(0, 1);
                        HwQoEUdpServiceImpl.this.mAddScore = HwQoEUdpServiceImpl.this.calculateNewUdpAccessScore(HwQoEUdpServiceImpl.this.mCurrUdpInfoForMonitor, HwQoEUdpServiceImpl.this.mLastUdpInfoForMonitor);
                        HwQoEUdpServiceImpl.this.mUdpNoAccessScore = HwQoEUdpServiceImpl.this.mUdpNoAccessScore + HwQoEUdpServiceImpl.this.mAddScore;
                        HwQoEUdpServiceImpl.this.mUdpNoAccessScore = Math.max(0, HwQoEUdpServiceImpl.this.mUdpNoAccessScore);
                        String str = HwQoEUdpServiceImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mUdpNoAccessScore: ");
                        stringBuilder.append(HwQoEUdpServiceImpl.this.mUdpNoAccessScore);
                        stringBuilder.append(", mAddScore: ");
                        stringBuilder.append(HwQoEUdpServiceImpl.this.mAddScore);
                        Log.d(str, stringBuilder.toString());
                        if (HwQoEUdpServiceImpl.this.mLastUdpInfoForMonitor == null) {
                            HwQoEUdpServiceImpl.this.mLastUdpInfoForMonitor = new HwQoEUdpNetWorkInfo();
                        }
                        HwQoEUdpServiceImpl.this.mLastUdpInfoForMonitor.setUdpNetWorkInfo(HwQoEUdpServiceImpl.this.mCurrUdpInfoForMonitor);
                        if (HwQoEUdpServiceImpl.this.mIsUDPAccessMonitorEnabled) {
                            if (HwQoEUdpServiceImpl.this.mUdpNoAccessScore >= 2 && HwQoEUdpServiceImpl.this.mHwQoEMonitorCallback != null && HwQoEUdpServiceImpl.this.mAddScore > 0) {
                                HwQoEUdpServiceImpl.this.mHwQoEMonitorCallback.onUDPInternetAccessStatusChange(true);
                            }
                            HwQoEUdpServiceImpl.this.mHwQoEUdpServiceImplHandler.sendEmptyMessageDelayed(101, HwQoEUdpServiceImpl.UDP_ACCESS_MONITOR_INTERVAL);
                            return;
                        }
                        return;
                    case 102:
                        HwQoEUdpServiceImpl.this.mCurrUdpInfoForUID = HwQoEUdpServiceImpl.this.mHwQoEJNIAdapter.getUdpNetworkStatsDetail(1000, 1);
                        if (HwQoEUdpServiceImpl.this.mLastUdpInfoForMonitor != null) {
                            HwQoEUdpServiceImpl.this.mCurrUdpSpeed = HwQoEUdpServiceImpl.this.calculateUDPSpeed(HwQoEUdpServiceImpl.this.mCurrUdpInfoForUID, HwQoEUdpServiceImpl.this.mLastUdpInfoForMonitor);
                        } else {
                            HwQoEUdpServiceImpl.this.mLastUdpInfoForMonitor = new HwQoEUdpNetWorkInfo();
                            HwQoEUdpServiceImpl.this.mLastUdpInfoForMonitor.setUdpNetWorkInfo(HwQoEUdpServiceImpl.this.mCurrUdpInfoForMonitor);
                        }
                        if (HwQoEUdpServiceImpl.this.mIsUDPAccessMonitorEnabled) {
                            HwQoEUdpServiceImpl.this.mHwQoEUdpServiceImplHandler.sendEmptyMessageDelayed(101, 1000);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    private int calculateNewUdpAccessScore(HwQoEUdpNetWorkInfo currUdpInfo, HwQoEUdpNetWorkInfo lastUdpInfo) {
        int score = 0;
        if (currUdpInfo == null || lastUdpInfo == null) {
            return 0;
        }
        if (currUdpInfo.getUid() != lastUdpInfo.getUid()) {
            Log.d(TAG, "uid is error,ignore calculate score");
            return 0;
        }
        long timestamp = currUdpInfo.getTimestamp() - lastUdpInfo.getTimestamp();
        if (timestamp <= 0 || timestamp > 30000) {
            return 0;
        }
        long rxUdpBytes = currUdpInfo.getRxUdpBytes() - lastUdpInfo.getRxUdpBytes();
        long rxUdpPackets = currUdpInfo.getRxUdpPackets() - lastUdpInfo.getRxUdpPackets();
        long rxSumPackets = currUdpInfo.getSumRxPackets() - lastUdpInfo.getSumRxPackets();
        int sumUdpSockets = (currUdpInfo.getSumUdpSockets() + lastUdpInfo.getSumUdpSockets()) / 2;
        int sumUdpUids = (currUdpInfo.getSumUdpUids() + lastUdpInfo.getSumUdpUids()) / 2;
        if (rxUdpBytes > 0 || rxUdpPackets > 0 || rxSumPackets > 0) {
            if (this.mUdpNoAccessScore >= 2 && this.mHwQoEMonitorCallback != null) {
                this.mHwQoEMonitorCallback.onUDPInternetAccessStatusChange(false);
            }
            score = 0 - this.mUdpNoAccessScore;
        } else if (sumUdpSockets > 5 || sumUdpUids > 3) {
            score = 2;
        } else if (sumUdpSockets > 1 || sumUdpUids > 1) {
            score = 1;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("add: score:");
        stringBuilder.append(score);
        stringBuilder.append(", rxUdpBytes:");
        stringBuilder.append(rxUdpBytes);
        stringBuilder.append(", rxUdpPackets:");
        stringBuilder.append(rxUdpPackets);
        stringBuilder.append(", sumUdpSockets:");
        stringBuilder.append(sumUdpSockets);
        stringBuilder.append(", sumUdpUids:");
        stringBuilder.append(sumUdpUids);
        stringBuilder.append(",rxSumPackets:");
        stringBuilder.append(rxSumPackets);
        Log.d(str, stringBuilder.toString());
        return score;
    }

    private UdpNetworkSpeed calculateUDPSpeed(HwQoEUdpNetWorkInfo currUdpInfo, HwQoEUdpNetWorkInfo lastUdpInfo) {
        Log.d(TAG, "calculateUDPSpeed:");
        if (currUdpInfo == null || lastUdpInfo == null) {
            return null;
        }
        if (currUdpInfo.getUid() != lastUdpInfo.getUid()) {
            Log.d(TAG, "uid is error,ignore calculate speed");
            return null;
        }
        long timestamp = currUdpInfo.getTimestamp() - lastUdpInfo.getTimestamp();
        long rxSpeedKbs = ((currUdpInfo.getRxUdpBytes() - lastUdpInfo.getRxUdpBytes()) / 1024) / (timestamp / 1000);
        long txSpeedKbs = ((currUdpInfo.getTxUdpBytes() - lastUdpInfo.getTxUdpBytes()) / 1024) / (timestamp / 1000);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("txSpeedKbs:");
        stringBuilder.append(txSpeedKbs);
        stringBuilder.append(" Kb/s, rxSpeedKbs:");
        stringBuilder.append(rxSpeedKbs);
        stringBuilder.append(" Kb/s, timestamp:");
        stringBuilder.append(timestamp);
        Log.d(str, stringBuilder.toString());
        UdpNetworkSpeed networkSpeed = new UdpNetworkSpeed();
        networkSpeed.updateUdpSpeed(txSpeedKbs, rxSpeedKbs, currUdpInfo.getUid());
        return networkSpeed;
    }

    public void release() {
        if (this.mHwQoEUdpServiceImplHandler != null) {
            Looper looper = this.mHwQoEUdpServiceImplHandler.getLooper();
            if (looper != null && looper != Looper.getMainLooper()) {
                looper.quitSafely();
                HwQoEUtils.logD("HwQoEUdpServiceImpl$HandlerThread::Release");
            }
        }
    }
}
