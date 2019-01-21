package com.android.server.hidata.appqoe;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.TrafficStats;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.os.PowerManager;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.hidata.arbitration.HwArbitrationFunction;
import com.android.server.hidata.mplink.MpLinkQuickSwitchConfiguration;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.wifipro.WifiProCommonUtils;

public class HwAPPQoEStateMachine extends StateMachine {
    public static final int MPLINK_CHECK_TIME = 60000;
    public static final String STATE_MACHINE_NAME_CELL = "CellMonitorState";
    public static final String STATE_MACHINE_NAME_DATA_OFF = "DataOffMonitorState";
    public static final String STATE_MACHINE_NAME_IDLE = "IdleState";
    public static final String STATE_MACHINE_NAME_WIFI = "WiFiMonitorState";
    private static String TAG = "HiData_HwAPPQoEStateMachine";
    private static HwAPPQoEStateMachine mHwAPPQoEStateMachine = null;
    private HwAPPStateInfo curAPPStateInfo = new HwAPPStateInfo();
    private boolean isMPLinkSuccess = false;
    private int lastAPPStateSent = -1;
    private State mCellMonitorState = new CellMonitorState();
    private HwAPPChrExcpReport mChrExcpReport = null;
    private Context mContext = null;
    private State mDataOffMonitorState = new DataOffMonitorState();
    private HwAPKQoEQualityMonitor mHwAPKQoEQualityMonitor = null;
    private HwAPKQoEQualityMonitorCell mHwAPKQoEQualityMonitorCell = null;
    private HwAPPChrManager mHwAPPChrManager;
    private HwAPPQoEContentAware mHwAPPQoEContentAware = null;
    private HwAPPQoESystemStateMonitor mHwAPPQoESystemStateMonitor = null;
    private HwAPPQoEResourceManger mHwAPPQoeResourceManger = null;
    private HwGameQoEManager mHwGameQoEManager = null;
    private State mIdleState = new IdleState();
    private State mWiFiMonitorState = new WiFiMonitorState();
    private WifiManager mWifiManager;

    public class CellMonitorState extends State {
        boolean isBadAfterMPLink = false;
        boolean mIsAppStall = false;
        long mMPLinkCheckTimer = 0;
        long mMPLinkStartTraffic = 0;

        public void enter() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "Enter CellMonitorState");
            int event = 100;
            if (102 == HwAPPQoEStateMachine.this.lastAPPStateSent) {
                event = 102;
            }
            HwAPPQoEStateMachine.this.sendMessage(Message.obtain(HwAPPQoEStateMachine.this.getHandler(), event, HwAPPQoEStateMachine.this.curAPPStateInfo));
            initChrState();
        }

        public void exit() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState is Exit");
            HwAPPQoEStateMachine.this.removeMessages(107);
        }

        /* JADX WARNING: Removed duplicated region for block: B:29:0x0128  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(Message message) {
            String access$000 = HwAPPQoEStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CellMonitorState, msg.what:");
            stringBuilder.append(message.what);
            HwAPPQoEUtils.logD(access$000, stringBuilder.toString());
            int i = message.what;
            if (i == 3) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_WIFI_STATE_CONNECTED");
                HwAPPQoEStateMachine.this.stopAPPMonitor(HwAPPQoEStateMachine.this.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                endChrState();
                HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mWiFiMonitorState);
            } else if (i == 8) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_CELL_STATE_DISCONNECT");
                HwAPPQoEStateMachine.this.stopAPPMonitor(HwAPPQoEStateMachine.this.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                endChrState();
                HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mDataOffMonitorState);
            } else if (i != 205) {
                HwAPPStateInfo infoData;
                switch (i) {
                    case 100:
                    case 102:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_APP_STATE_START/UPDATE");
                        infoData = (HwAPPStateInfo) message.obj;
                        infoData.mNetworkType = 801;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                        HwAPPQoEStateMachine.this.updateCurAPPStateInfo(infoData);
                        HwAPPQoEStateMachine.this.startAPPMonitor(infoData, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                        startChrState();
                        HwAPPQoEStateMachine.this.removeMessages(107);
                        break;
                    case 101:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_APP_STATE_BACKGROUND/END");
                        infoData = (HwAPPStateInfo) message.obj;
                        infoData.mNetworkType = 801;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                        endChrState();
                        if (true == HwAPPQoEStateMachine.this.isStopInfoMatchStartInfo(infoData)) {
                        }
                        break;
                    default:
                        switch (i) {
                            case 104:
                                break;
                            case 105:
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_APP_STATE_MONITOR");
                                infoData = (HwAPPStateInfo) message.obj;
                                HwAPPQoEStateMachine.this.updateGameRTT(infoData.mAppRTT, infoData.mAppId);
                                infoData.mNetworkType = 801;
                                HwAPPQoEStateMachine.this.notifyAPPRttInfoCallback(infoData);
                                break;
                            case 106:
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_APP_STATE_GOOD");
                                infoData = (HwAPPStateInfo) message.obj;
                                infoData.mNetworkType = 801;
                                HwAPPQoEStateMachine.this.notifyAPPQualityCallback(infoData, message.what, false);
                                HwAPPQoEStateMachine.this.removeMessages(107);
                                break;
                            case 107:
                                if (!HwAPPQoEStateMachine.this.isScreenOn()) {
                                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState app is bad but screen is off");
                                    break;
                                }
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_APP_STATE_BAD");
                                infoData = message.obj;
                                infoData.mNetworkType = 801;
                                HwAPPQoEStateMachine.this.notifyAPPQualityCallback(infoData, message.what, false);
                                HwAPPQoEAPKConfig config = HwAPPQoEStateMachine.this.mHwAPPQoeResourceManger.getAPKScenceConfig(infoData.mScenceId);
                                if (!(config == null || HwAPPQoEStateMachine.this.hasMessages(107))) {
                                    int appPeriod = config.mAppPeriod * 1000;
                                    Message msg = new Message();
                                    msg.what = 107;
                                    msg.obj = infoData;
                                    HwAPPQoEStateMachine.this.sendMessageDelayed(msg, (long) appPeriod);
                                }
                                handleChrAppBad();
                                break;
                            default:
                                switch (i) {
                                    case 201:
                                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_INTERNAL_NETWORK_STATE_CHANGE");
                                        i = ((Integer) message.obj).intValue();
                                        endChrState();
                                        if (i == 800) {
                                            HwAPPQoEStateMachine.this.stopAPPMonitor(HwAPPQoEStateMachine.this.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                                            HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mWiFiMonitorState);
                                            break;
                                        }
                                        break;
                                    case 202:
                                        HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.reRegisterAllGameCallbacks();
                                        break;
                                    default:
                                        return false;
                                }
                        }
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "CellMonitorState MSG_APP_STATE_BACKGROUND/END");
                        infoData = (HwAPPStateInfo) message.obj;
                        infoData.mNetworkType = 801;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                        endChrState();
                        if (true == HwAPPQoEStateMachine.this.isStopInfoMatchStartInfo(infoData)) {
                            HwAPPQoEStateMachine.this.stopAPPMonitor(infoData, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                            HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mIdleState);
                            break;
                        }
                        break;
                }
            } else {
                HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 14);
            }
            return true;
        }

        private void endChrState() {
            if (HwAPPQoEStateMachine.this.isMPLinkSuccess) {
                long deltaTraffic = ((TrafficStats.getMobileTxBytes() + TrafficStats.getMobileRxBytes()) - this.mMPLinkStartTraffic) / 1024;
                String access$000 = HwAPPQoEStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("endChrState deltaTraffic = ");
                stringBuilder.append(deltaTraffic);
                stringBuilder.append("KB");
                HwAPPQoEUtils.logD(access$000, stringBuilder.toString());
                HwAPPQoEStateMachine.this.mHwAPPChrManager.updateTraffic(HwAPPQoEStateMachine.this.curAPPStateInfo, deltaTraffic);
                this.mMPLinkStartTraffic = 0;
                HwAPPQoEStateMachine.this.isMPLinkSuccess = false;
                this.isBadAfterMPLink = false;
            }
        }

        private void initChrState() {
            this.mIsAppStall = false;
            this.isBadAfterMPLink = false;
            this.mMPLinkStartTraffic = 0;
            this.mMPLinkCheckTimer = 0;
        }

        private void startChrState() {
            if (HwAPPQoEStateMachine.this.isMPLinkSuccess) {
                this.isBadAfterMPLink = false;
                this.mMPLinkStartTraffic = TrafficStats.getMobileTxBytes() + TrafficStats.getMobileRxBytes();
                this.mMPLinkCheckTimer = System.currentTimeMillis();
                HwAPPQoEStateMachine.this.sendMessageDelayed(205, AppHibernateCst.DELAY_ONE_MINS);
                return;
            }
            this.mIsAppStall = false;
            HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 2);
        }

        private void handleChrAppBad() {
            if (HwAPPQoEStateMachine.this.isMPLinkSuccess) {
                if (!this.isBadAfterMPLink) {
                    HwAPPQoEStateMachine.this.removeMessages(205);
                    if (System.currentTimeMillis() - this.mMPLinkCheckTimer < AppHibernateCst.DELAY_ONE_MINS) {
                        HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 15);
                        HwAPPQoEStateMachine.this.sendMessage(203, 3);
                    }
                    this.isBadAfterMPLink = true;
                }
            } else if (!this.mIsAppStall) {
                HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 4);
                this.mIsAppStall = true;
            }
            HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 6);
        }
    }

    public class DataOffMonitorState extends State {
        public void enter() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "Enter DataOffMonitorState");
        }

        public boolean processMessage(Message message) {
            String access$000 = HwAPPQoEStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dataoff monitor state:");
            stringBuilder.append(message.what);
            HwAPPQoEUtils.logD(access$000, stringBuilder.toString());
            int i = message.what;
            if (i == 3) {
                HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mWiFiMonitorState);
            } else if (i != 7) {
                HwAPPStateInfo infoData;
                if (i != 104) {
                    switch (i) {
                        case 100:
                        case 102:
                            infoData = message.obj;
                            infoData.mNetworkType = 802;
                            HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                            HwAPPQoEStateMachine.this.updateCurAPPStateInfo(infoData);
                            break;
                        case 101:
                            break;
                        default:
                            switch (i) {
                                case 201:
                                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "DataOffMonitorState MSG_INTERNAL_NETWORK_STATE_CHANGE");
                                    if (((Integer) message.obj).intValue() != 801) {
                                        HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mWiFiMonitorState);
                                        break;
                                    }
                                    HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mCellMonitorState);
                                    break;
                                case 202:
                                    HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.reRegisterAllGameCallbacks();
                                    break;
                                default:
                                    return false;
                            }
                    }
                }
                infoData = (HwAPPStateInfo) message.obj;
                infoData.mNetworkType = 802;
                HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                if (true == HwAPPQoEStateMachine.this.isStopInfoMatchStartInfo(infoData)) {
                    HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mIdleState);
                }
            } else {
                HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mCellMonitorState);
            }
            return true;
        }
    }

    public class IdleState extends State {
        public void enter() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "enter idle state");
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 100) {
                HwAPPStateInfo infoData = message.obj;
                HwAPPQoEStateMachine.this.updateCurAPPStateInfo(infoData);
                String access$000 = HwAPPQoEStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IdleState, msg.what:");
                stringBuilder.append(message.what);
                stringBuilder.append(",");
                stringBuilder.append(infoData.toString());
                HwAPPQoEUtils.logD(access$000, stringBuilder.toString());
                int networkType = HwAPPQoEStateMachine.this.quryCurrentNetwork(infoData.mAppUID);
                if (800 == networkType) {
                    HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mWiFiMonitorState);
                } else if (801 == networkType) {
                    HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mCellMonitorState);
                } else if (802 == networkType) {
                    HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mDataOffMonitorState);
                    HwAPPQoEStateMachine.this.sendMessage(Message.obtain(HwAPPQoEStateMachine.this.getHandler(), 100, infoData));
                }
            } else if (i != 200) {
                switch (i) {
                    case 202:
                        HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.reRegisterAllGameCallbacks();
                        break;
                    case 203:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "MSG_INTERNAL_CHR_EXCP_TRIGGER");
                        if (HwAPPQoEStateMachine.this.mHwAPPQoESystemStateMonitor.getMpLinkState() && HwAPPQoEStateMachine.this.mChrExcpReport.isReportPermmitted()) {
                            HwAPPQoEStateMachine.this.mChrExcpReport.reportAPPQoExcpInfo(message.arg1, HwAPPQoEStateMachine.this.curAPPStateInfo.mScenceId);
                            break;
                        }
                    default:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "IdleState:default case");
                        break;
                }
            } else {
                HwAPPQoEStateMachine.this.initAPPQoEModules();
            }
            return true;
        }
    }

    public class WiFiMonitorState extends State {
        long mAppKQITimer = 0;
        boolean mIsAppStall = false;

        public void enter() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "Enter WiFiMonitorState");
            int event = 100;
            if (102 == HwAPPQoEStateMachine.this.lastAPPStateSent) {
                event = 102;
            }
            HwAPPQoEStateMachine.this.sendMessage(Message.obtain(HwAPPQoEStateMachine.this.getHandler(), event, HwAPPQoEStateMachine.this.curAPPStateInfo));
            initChrState();
        }

        public void exit() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState is Exit");
            stopWeakNetworkMonitor();
        }

        /* JADX WARNING: Removed duplicated region for block: B:24:0x00f5  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(Message message) {
            String access$000 = HwAPPQoEStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("wifi monitor state:");
            stringBuilder.append(message.what);
            HwAPPQoEUtils.logD(access$000, stringBuilder.toString());
            int i = message.what;
            if (i == 4) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_WIFI_STATE_DISCONNECT");
                HwAPPQoEStateMachine.this.stopAPPMonitor(HwAPPQoEStateMachine.this.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mDataOffMonitorState);
            } else if (i == 7) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_CELL_STATE_CONNECTED");
                HwAPPQoEStateMachine.this.stopAPPMonitor(HwAPPQoEStateMachine.this.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mCellMonitorState);
            } else if (i != 109) {
                HwAPPStateInfo infoData;
                switch (i) {
                    case 100:
                    case 102:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_APP_STATE_START/UPDATE");
                        infoData = (HwAPPStateInfo) message.obj;
                        infoData.mNetworkType = 800;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                        HwAPPQoEStateMachine.this.updateCurAPPStateInfo(infoData);
                        HwAPPQoEStateMachine.this.startAPPMonitor(infoData, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                        startChrState();
                        startWeakNetworkMonitor();
                        break;
                    case 101:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_APP_STATE_BACKGROUND/END");
                        infoData = (HwAPPStateInfo) message.obj;
                        infoData.mNetworkType = 800;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                        if (true == HwAPPQoEStateMachine.this.isStopInfoMatchStartInfo(infoData)) {
                        }
                        break;
                    default:
                        switch (i) {
                            case 104:
                                break;
                            case 105:
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_APP_STATE_MONITOR");
                                infoData = (HwAPPStateInfo) message.obj;
                                HwAPPQoEStateMachine.this.updateGameRTT(infoData.mAppRTT, infoData.mAppId);
                                infoData.mNetworkType = 800;
                                HwAPPQoEStateMachine.this.notifyAPPRttInfoCallback(infoData);
                                break;
                            case 106:
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_APP_STATE_GOOD");
                                infoData = (HwAPPStateInfo) message.obj;
                                infoData.mNetworkType = 800;
                                HwAPPQoEStateMachine.this.notifyAPPQualityCallback(infoData, message.what, true);
                                break;
                            case 107:
                                if (!HwAPPQoEStateMachine.this.isScreenOn()) {
                                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState app is bad but screen is off");
                                    break;
                                }
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_APP_STATE_BAD");
                                infoData = message.obj;
                                infoData.mNetworkType = 800;
                                HwAPPQoEStateMachine.this.notifyAPPQualityCallback(infoData, message.what, false);
                                handleChrAppBad();
                                break;
                            default:
                                switch (i) {
                                    case 201:
                                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_INTERNAL_NETWORK_STATE_CHANGE");
                                        if (((Integer) message.obj).intValue() == 801) {
                                            HwAPPQoEStateMachine.this.stopAPPMonitor(HwAPPQoEStateMachine.this.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                                            handleChrMpLinkSuccess();
                                            HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mCellMonitorState);
                                            break;
                                        }
                                        break;
                                    case 202:
                                        HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.reRegisterAllGameCallbacks();
                                        break;
                                    default:
                                        return false;
                                }
                        }
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_APP_STATE_BACKGROUND/END");
                        infoData = (HwAPPStateInfo) message.obj;
                        infoData.mNetworkType = 800;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                        if (true == HwAPPQoEStateMachine.this.isStopInfoMatchStartInfo(infoData)) {
                            HwAPPQoEStateMachine.this.stopAPPMonitor(infoData, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                            HwAPPQoEStateMachine.this.transitionTo(HwAPPQoEStateMachine.this.mIdleState);
                            break;
                        }
                        break;
                }
            } else {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState MSG_APP_STATE_WEAK");
                HwAPPQoEStateMachine.this.sendMessage(HwAPPQoEStateMachine.this.obtainMessage(107, HwAPPQoEStateMachine.this.curAPPStateInfo));
            }
            return true;
        }

        private void initChrState() {
            this.mIsAppStall = false;
            this.mAppKQITimer = 0;
        }

        private void startChrState() {
            this.mIsAppStall = false;
            this.mAppKQITimer = 0;
            HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 1);
        }

        private void handleChrAppBad() {
            HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 5);
            if (!this.mIsAppStall) {
                HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 3);
                this.mAppKQITimer = System.currentTimeMillis();
                this.mIsAppStall = true;
            }
        }

        private void startWeakNetworkMonitor() {
            if (HwAPPQoEStateMachine.this.curAPPStateInfo != null && HwAPPQoEStateMachine.this.curAPPStateInfo.mAppId != -1) {
                String access$000 = HwAPPQoEStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("WiFiMonitorState startWeakNetworkMonitor curAPPStateInfo.mAppId = ");
                stringBuilder.append(HwAPPQoEStateMachine.this.curAPPStateInfo.mAppId);
                HwAPPQoEUtils.logD(access$000, stringBuilder.toString());
                WifiInfo info = ((WifiManager) HwAPPQoEStateMachine.this.mContext.getSystemService("wifi")).getConnectionInfo();
                if (info != null) {
                    int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
                    if (rssiLevel <= 1 && !HwAPPQoEStateMachine.this.hasMessages(109)) {
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "send weak network delay message");
                        HwAPPQoEStateMachine.this.sendMessageDelayed(109, 4000);
                    } else if (rssiLevel >= 2 && HwAPPQoEStateMachine.this.hasMessages(109)) {
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "remove weak network delay message");
                        HwAPPQoEStateMachine.this.removeMessages(109);
                    }
                }
            }
        }

        private void stopWeakNetworkMonitor() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "WiFiMonitorState stopWeakNetworkMonitor");
            HwAPPQoEStateMachine.this.removeMessages(109);
        }

        private void handleChrMpLinkSuccess() {
            long endAppKQITime = System.currentTimeMillis() - this.mAppKQITimer;
            HwAPPQoEAPKConfig config = HwAPPQoEStateMachine.this.mHwAPPQoeResourceManger.getAPKScenceConfig(HwAPPQoEStateMachine.this.curAPPStateInfo.mScenceId);
            HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 13);
            HwAPPQoEStateMachine.this.isMPLinkSuccess = true;
            if (config != null) {
                if (endAppKQITime <= 1000) {
                    HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 16);
                } else {
                    HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 17);
                }
                WifiConfiguration mConnectedConfig = WifiProCommonUtils.getCurrentWifiConfig(HwAPPQoEStateMachine.this.mWifiManager);
                HwAPPChrExcpInfo info = HwAPPQoEStateMachine.this.mHwAPKQoEQualityMonitor.getAPPQoEInfo();
                if (!(info == null || info.rssi == -1)) {
                    String access$000 = HwAPPQoEStateMachine.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleChrMpLinkSuccess info.rssi = ");
                    stringBuilder.append(info.rssi);
                    HwAPPQoEUtils.logD(access$000, stringBuilder.toString());
                    if (info.rssi >= 3) {
                        HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 20);
                    } else {
                        HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 21);
                    }
                }
                if (mConnectedConfig == null) {
                    return;
                }
                if (WifiProCommonUtils.isWpaOrWpa2(mConnectedConfig)) {
                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "handleChrMpLinkSuccess is a home ap");
                    HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 9);
                    return;
                }
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, "handleChrMpLinkSuccess is a portal or open ap");
                HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 10);
            }
        }
    }

    private HwAPPQoEStateMachine(Context context) {
        super("HwAPPQoEStateMachine");
        this.mContext = context;
        this.mHwAPPQoeResourceManger = HwAPPQoEResourceManger.createHwAPPQoEResourceManger();
        this.mHwAPPQoESystemStateMonitor = new HwAPPQoESystemStateMonitor(context, getHandler());
        HwAPPQoEUserLearning.createHwAPPQoEUserLearning(this.mContext);
        this.mHwAPPChrManager = HwAPPChrManager.getInstance();
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        addState(this.mIdleState);
        addState(this.mWiFiMonitorState, this.mIdleState);
        addState(this.mCellMonitorState, this.mIdleState);
        addState(this.mDataOffMonitorState, this.mIdleState);
        setInitialState(this.mIdleState);
        start();
    }

    public HwAPPStateInfo getCurAPPStateInfo() {
        HwAPPQoEUtils.logD(TAG, "APP QOE State machine Enter getCurAPPStateInfo ");
        return this.curAPPStateInfo;
    }

    public static HwAPPQoEStateMachine createHwAPPQoEStateMachine(Context context) {
        if (mHwAPPQoEStateMachine == null) {
            mHwAPPQoEStateMachine = new HwAPPQoEStateMachine(context);
        }
        return mHwAPPQoEStateMachine;
    }

    public void initAPPQoEModules() {
        if (this.mContext == null) {
            HwAPPQoEUtils.logD(TAG, "initAPPQoEModules, invalid context.");
            return;
        }
        HwAPPQoEUtils.logD(TAG, "initAPPQoEModules, process.");
        this.mChrExcpReport = HwAPPChrExcpReport.getInstance();
        this.mHwAPPQoEContentAware = HwAPPQoEContentAware.createHwAPPQoEContentAware(this.mContext, getHandler());
        this.mHwGameQoEManager = new HwGameQoEManager(this.mContext, getHandler());
        this.mHwAPKQoEQualityMonitor = new HwAPKQoEQualityMonitor(getHandler(), this.mContext);
        this.mHwAPKQoEQualityMonitorCell = HwAPKQoEQualityMonitorCell.createQualityMonitorCell(getHandler());
        this.mChrExcpReport.setMonitorInstance(this.mHwAPKQoEQualityMonitor, this.mHwAPKQoEQualityMonitorCell);
    }

    private void notifyAPPStateCallback(HwAPPStateInfo data, int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyAPPStateCallback, state:");
        stringBuilder.append(state);
        stringBuilder.append(", app:");
        stringBuilder.append(data.mAppId);
        stringBuilder.append(", scence:");
        stringBuilder.append(data.mScenceId);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        if (this.curAPPStateInfo.isObjectValueEqual(data) && state == this.lastAPPStateSent) {
            HwAPPQoEUtils.logD(TAG, "notifyAPPStateCallback: info sent was same as before");
            return;
        }
        HwAPPQoEManager hwAPPQoEManager = HwAPPQoEManager.getInstance();
        IHwAPPQoECallback brainCallback = hwAPPQoEManager.getAPPQoECallback(true);
        HwAPPStateInfo tempData = new HwAPPStateInfo();
        tempData.copyObjectValue(data);
        if (brainCallback != null) {
            brainCallback.onAPPStateCallBack(tempData, state);
        }
        IHwAPPQoECallback wmCallback = hwAPPQoEManager.getAPPQoECallback(null);
        if (wmCallback != null) {
            wmCallback.onAPPStateCallBack(tempData, state);
        }
        this.lastAPPStateSent = state;
    }

    private void notifyAPPQualityCallback(HwAPPStateInfo data, int state, boolean isWmOnly) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyAPPQualityCallback:");
        stringBuilder.append(state);
        stringBuilder.append(",");
        stringBuilder.append(data.mAppId);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        HwAPPQoEManager hwAPPQoEManager = HwAPPQoEManager.getInstance();
        IHwAPPQoECallback brainCallback = hwAPPQoEManager.getAPPQoECallback(true);
        HwAPPStateInfo tempData = new HwAPPStateInfo();
        tempData.copyObjectValue(data);
        MpLinkQuickSwitchConfiguration switchConfiguration = tempData.getQuickSwitchConfiguration();
        if (switchConfiguration != null) {
            switchConfiguration.setSocketStrategy(3);
            switchConfiguration.setNetworkStrategy(0);
        }
        if (!(brainCallback == null || isWmOnly)) {
            brainCallback.onAPPQualityCallBack(tempData, state);
        }
        IHwAPPQoECallback wmCallback = hwAPPQoEManager.getAPPQoECallback(false);
        if (wmCallback != null) {
            wmCallback.onAPPQualityCallBack(tempData, state);
        }
    }

    private void notifyAPPRttInfoCallback(HwAPPStateInfo data) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyAPPRttInfoCallback:");
        stringBuilder.append(data.toString());
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        HwAPPQoEManager hwAPPQoEManager = HwAPPQoEManager.getInstance();
        IHwAPPQoECallback brainCallback = hwAPPQoEManager.getAPPQoECallback(true);
        HwAPPStateInfo tempData = new HwAPPStateInfo();
        tempData.copyObjectValue(data);
        if (brainCallback != null) {
            brainCallback.onAPPRttInfoCallBack(tempData);
        }
        IHwAPPQoECallback wmCallback = hwAPPQoEManager.getAPPQoECallback(null);
        if (wmCallback != null) {
            wmCallback.onAPPRttInfoCallBack(tempData);
        }
    }

    private int quryCurrentNetwork(int uid) {
        return HwArbitrationFunction.getCurrentNetwork(this.mContext, uid);
    }

    private void startAPPMonitor(HwAPPStateInfo appStateInfo, String stateName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startAPPMonitor -- appStateInfo:");
        stringBuilder.append(appStateInfo.toString());
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        if (1000 == appStateInfo.mAppType) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startAPPMonitor -- stateName:");
            stringBuilder.append(stateName);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            if (STATE_MACHINE_NAME_WIFI.equals(stateName)) {
                this.mHwAPKQoEQualityMonitor.startMonitor(this.curAPPStateInfo);
            } else if (STATE_MACHINE_NAME_CELL.equals(stateName)) {
                this.mHwAPKQoEQualityMonitorCell.startMonitor(this.curAPPStateInfo);
            }
            return;
        }
        if (2000 == appStateInfo.mAppType) {
            HwAPPQoEUtils.logD(TAG, "startAPPMonitor for game");
            this.mHwGameQoEManager.startMonitor(appStateInfo);
        }
    }

    private void stopAPPMonitor(HwAPPStateInfo appStateInfo, String stateName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopAPPMonitor -- appStateInfo:");
        stringBuilder.append(appStateInfo.toString());
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        if (1000 == appStateInfo.mAppType) {
            if (STATE_MACHINE_NAME_WIFI.equals(stateName)) {
                this.mHwAPKQoEQualityMonitor.stopMonitor();
            } else if (STATE_MACHINE_NAME_CELL.equals(stateName)) {
                this.mHwAPKQoEQualityMonitorCell.stopMonitor();
            }
            return;
        }
        if (2000 == appStateInfo.mAppType) {
            HwAPPQoEUtils.logD(TAG, "stopAPPMonitor for game");
            this.mHwGameQoEManager.stopMonitor();
        }
    }

    private void updateGameRTT(int rtt, int gameId) {
        if (this.mHwGameQoEManager == null || this.mHwGameQoEManager.mGameId != gameId) {
            HwAPPQoEUtils.logD(TAG, "updateGameRTT, game not found in game list, internal error");
        } else {
            this.mHwGameQoEManager.updateGameRTT(rtt);
        }
    }

    public boolean isStopInfoMatchStartInfo(HwAPPStateInfo infoData) {
        if (infoData.mAppUID != this.curAPPStateInfo.mAppUID) {
            return false;
        }
        this.curAPPStateInfo = new HwAPPStateInfo();
        updateCurAPPStateInfo(this.curAPPStateInfo);
        return true;
    }

    private void updateCurAPPStateInfo(HwAPPStateInfo infoData) {
        this.curAPPStateInfo.copyObjectValue(infoData);
        this.mHwAPPQoESystemStateMonitor.curAPPStateInfo.copyObjectValue(infoData);
    }

    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (pm == null || !pm.isScreenOn()) {
            return false;
        }
        return true;
    }
}
