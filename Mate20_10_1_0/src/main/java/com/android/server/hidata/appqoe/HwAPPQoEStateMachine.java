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
import com.android.server.hidata.wavemapping.HwWaveMappingManager;
import com.android.server.hidata.wavemapping.IWaveMappingCallback;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.wifipro.WifiProCommonUtils;

public class HwAPPQoEStateMachine extends StateMachine implements IWaveMappingCallback {
    public static final int MPLINK_CHECK_TIME = 60000;
    public static final String STATE_MACHINE_NAME_CELL = "CellMonitorState";
    public static final String STATE_MACHINE_NAME_DATA_OFF = "DataOffMonitorState";
    public static final String STATE_MACHINE_NAME_IDLE = "IdleState";
    public static final String STATE_MACHINE_NAME_WIFI = "WiFiMonitorState";
    /* access modifiers changed from: private */
    public static String TAG = "HiData_HwAPPQoEStateMachine";
    private static HwAPPQoEStateMachine mHwAPPQoEStateMachine = null;
    /* access modifiers changed from: private */
    public HwAPPStateInfo curAPPStateInfo = new HwAPPStateInfo();
    /* access modifiers changed from: private */
    public boolean isMPLinkSuccess = false;
    /* access modifiers changed from: private */
    public int lastAPPStateSent = -1;
    /* access modifiers changed from: private */
    public State mCellMonitorState = new CellMonitorState();
    /* access modifiers changed from: private */
    public HwAPPChrExcpReport mChrExcpReport = null;
    /* access modifiers changed from: private */
    public Context mContext = null;
    /* access modifiers changed from: private */
    public State mDataOffMonitorState = new DataOffMonitorState();
    /* access modifiers changed from: private */
    public HwAPKQoEQualityMonitor mHwAPKQoEQualityMonitor = null;
    private HwAPKQoEQualityMonitorCell mHwAPKQoEQualityMonitorCell = null;
    /* access modifiers changed from: private */
    public HwAPPChrManager mHwAPPChrManager;
    /* access modifiers changed from: private */
    public HwAPPQoEContentAware mHwAPPQoEContentAware = null;
    /* access modifiers changed from: private */
    public HwAPPQoESystemStateMonitor mHwAPPQoESystemStateMonitor = null;
    /* access modifiers changed from: private */
    public HwAPPQoEResourceManger mHwAPPQoeResourceManger = null;
    private HwGameQoEManager mHwGameQoEManager = null;
    /* access modifiers changed from: private */
    public State mIdleState = new IdleState();
    /* access modifiers changed from: private */
    public State mWiFiMonitorState = new WiFiMonitorState();
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;

    private HwAPPQoEStateMachine(Context context) {
        super("HwAPPQoEStateMachine");
        this.mContext = context;
        this.mHwAPPQoeResourceManger = HwAPPQoEResourceManger.createHwAPPQoEResourceManger();
        this.mHwAPPQoESystemStateMonitor = new HwAPPQoESystemStateMonitor(context, getHandler());
        HwAPPQoEUserLearning.createHwAPPQoEUserLearning(this.mContext);
        this.mHwAPPChrManager = HwAPPChrManager.getInstance();
        this.mWifiManager = (WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
        addState(this.mIdleState);
        addState(this.mWiFiMonitorState, this.mIdleState);
        addState(this.mCellMonitorState, this.mIdleState);
        addState(this.mDataOffMonitorState, this.mIdleState);
        setInitialState(this.mIdleState);
        start();
    }

    public HwAPPStateInfo getCurAPPStateInfo() {
        HwAPPQoEUtils.logD(TAG, false, "APP QOE State machine Enter getCurAPPStateInfo ", new Object[0]);
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
            HwAPPQoEUtils.logD(TAG, false, "initAPPQoEModules, invalid context.", new Object[0]);
            return;
        }
        HwAPPQoEUtils.logD(TAG, false, "initAPPQoEModules, process.", new Object[0]);
        this.mChrExcpReport = HwAPPChrExcpReport.getInstance();
        this.mHwAPPQoEContentAware = HwAPPQoEContentAware.createHwAPPQoEContentAware(this.mContext, getHandler());
        this.mHwGameQoEManager = new HwGameQoEManager(this.mContext, getHandler());
        this.mHwAPKQoEQualityMonitor = new HwAPKQoEQualityMonitor(getHandler(), this.mContext);
        this.mHwAPKQoEQualityMonitorCell = HwAPKQoEQualityMonitorCell.createQualityMonitorCell(getHandler());
        this.mChrExcpReport.setMonitorInstance(this.mHwAPKQoEQualityMonitor, this.mHwAPKQoEQualityMonitorCell);
        HwWaveMappingManager mWaveMappingManager = HwWaveMappingManager.getInstance();
        if (mWaveMappingManager != null) {
            mWaveMappingManager.registerWaveMappingCallback(this, 0);
        }
    }

    public class IdleState extends State {
        public IdleState() {
        }

        public void enter() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "enter idle state", new Object[0]);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 100) {
                HwAPPStateInfo infoData = (HwAPPStateInfo) message.obj;
                HwAPPQoEStateMachine.this.updateCurAPPStateInfo(infoData);
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "IdleState, msg.what:%{public}d,%{public}s", Integer.valueOf(message.what), infoData.toString());
                int networkType = HwAPPQoEStateMachine.this.quryCurrentNetwork(infoData.mAppUID);
                if (800 == networkType) {
                    HwAPPQoEStateMachine hwAPPQoEStateMachine = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine.transitionTo(hwAPPQoEStateMachine.mWiFiMonitorState);
                } else if (801 == networkType) {
                    HwAPPQoEStateMachine hwAPPQoEStateMachine2 = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine2.transitionTo(hwAPPQoEStateMachine2.mCellMonitorState);
                } else if (802 == networkType) {
                    HwAPPQoEStateMachine hwAPPQoEStateMachine3 = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine3.transitionTo(hwAPPQoEStateMachine3.mDataOffMonitorState);
                    HwAPPQoEStateMachine hwAPPQoEStateMachine4 = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine4.sendMessage(Message.obtain(hwAPPQoEStateMachine4.getHandler(), 100, infoData));
                }
            } else if (i == 111) {
                HwAPPQoEStateMachine.this.notifyAPPQualityCallback((HwAPPStateInfo) message.obj, message.what, false);
            } else if (i == 200) {
                HwAPPQoEStateMachine.this.initAPPQoEModules();
            } else if (i != 207) {
                if (i == 202) {
                    HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.reRegisterAllGameCallbacks();
                } else if (i != 203) {
                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "IdleState:default case", new Object[0]);
                } else {
                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "MSG_INTERNAL_CHR_EXCP_TRIGGER", new Object[0]);
                    if (HwAPPQoEStateMachine.this.mHwAPPQoESystemStateMonitor.getMpLinkState()) {
                        HwAPPQoEStateMachine.this.mChrExcpReport.reportAPPQoExcpInfo(message.arg1, HwAPPQoEStateMachine.this.curAPPStateInfo.mScenceId);
                    }
                }
            } else if (HwAPPQoEStateMachine.this.mHwAPPQoEContentAware != null) {
                HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.registerFIHiComCallback();
            }
            return true;
        }
    }

    public class DataOffMonitorState extends State {
        public DataOffMonitorState() {
        }

        public void enter() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "Enter DataOffMonitorState", new Object[0]);
        }

        public boolean processMessage(Message message) {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "dataoff monitor state:%{public}d", Integer.valueOf(message.what));
            int i = message.what;
            if (i == 3) {
                HwAPPQoEStateMachine hwAPPQoEStateMachine = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine.transitionTo(hwAPPQoEStateMachine.mWiFiMonitorState);
            } else if (i != 7) {
                if (i != 104) {
                    if (i == 201) {
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "DataOffMonitorState MSG_INTERNAL_NETWORK_STATE_CHANGE", new Object[0]);
                        if (((Integer) message.obj).intValue() == 801) {
                            HwAPPQoEStateMachine hwAPPQoEStateMachine2 = HwAPPQoEStateMachine.this;
                            hwAPPQoEStateMachine2.transitionTo(hwAPPQoEStateMachine2.mCellMonitorState);
                        } else {
                            HwAPPQoEStateMachine hwAPPQoEStateMachine3 = HwAPPQoEStateMachine.this;
                            hwAPPQoEStateMachine3.transitionTo(hwAPPQoEStateMachine3.mWiFiMonitorState);
                        }
                    } else if (i != 202) {
                        switch (i) {
                            case 100:
                            case 102:
                                HwAPPStateInfo infoData = (HwAPPStateInfo) message.obj;
                                infoData.mNetworkType = 802;
                                HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                                HwAPPQoEStateMachine.this.updateCurAPPStateInfo(infoData);
                                break;
                            case 101:
                                break;
                            default:
                                return false;
                        }
                    } else {
                        HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.reRegisterAllGameCallbacks();
                    }
                }
                HwAPPStateInfo infoData2 = (HwAPPStateInfo) message.obj;
                infoData2.mNetworkType = 802;
                HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData2, message.what);
                if (true == HwAPPQoEStateMachine.this.isStopInfoMatchStartInfo(infoData2)) {
                    HwAPPQoEStateMachine hwAPPQoEStateMachine4 = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine4.transitionTo(hwAPPQoEStateMachine4.mIdleState);
                }
            } else {
                HwAPPQoEStateMachine hwAPPQoEStateMachine5 = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine5.transitionTo(hwAPPQoEStateMachine5.mCellMonitorState);
            }
            return true;
        }
    }

    public class WiFiMonitorState extends State {
        long mAppKQITimer = 0;
        boolean mIsAppStall = false;

        public WiFiMonitorState() {
        }

        public void enter() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "Enter WiFiMonitorState", new Object[0]);
            int event = 100;
            if (102 == HwAPPQoEStateMachine.this.lastAPPStateSent) {
                event = 102;
            }
            HwAPPQoEStateMachine hwAPPQoEStateMachine = HwAPPQoEStateMachine.this;
            hwAPPQoEStateMachine.sendMessage(Message.obtain(hwAPPQoEStateMachine.getHandler(), event, HwAPPQoEStateMachine.this.curAPPStateInfo));
            initChrState();
        }

        public void exit() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState is Exit", new Object[0]);
            stopWeakNetworkMonitor();
        }

        public boolean processMessage(Message message) {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "wifi monitor state:%{public}d", Integer.valueOf(message.what));
            int i = message.what;
            if (i == 4) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_WIFI_STATE_DISCONNECT", new Object[0]);
                HwAPPQoEStateMachine hwAPPQoEStateMachine = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine.stopAPPMonitor(hwAPPQoEStateMachine.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                HwAPPQoEStateMachine hwAPPQoEStateMachine2 = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine2.transitionTo(hwAPPQoEStateMachine2.mDataOffMonitorState);
            } else if (i == 7) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_CELL_STATE_CONNECTED", new Object[0]);
                HwAPPQoEStateMachine hwAPPQoEStateMachine3 = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine3.stopAPPMonitor(hwAPPQoEStateMachine3.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                HwAPPQoEStateMachine hwAPPQoEStateMachine4 = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine4.transitionTo(hwAPPQoEStateMachine4.mCellMonitorState);
            } else if (i == 109) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_APP_STATE_WEAK", new Object[0]);
                if (HwAPPQoEStateMachine.this.curAPPStateInfo.mAppType == 2000 || HwAPPQoEStateMachine.this.curAPPStateInfo.mScenceType == 4) {
                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "donot send game bad event when wifi signal is weak", new Object[0]);
                } else {
                    HwAPPQoEStateMachine hwAPPQoEStateMachine5 = HwAPPQoEStateMachine.this;
                    HwAPPQoEStateMachine.this.sendMessage(hwAPPQoEStateMachine5.obtainMessage(107, hwAPPQoEStateMachine5.curAPPStateInfo));
                }
            } else if (i == 112) {
                HwAPPQoEStateMachine.this.sendMessage(107, (HwAPPStateInfo) message.obj);
            } else if (i == 201) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_INTERNAL_NETWORK_STATE_CHANGE", new Object[0]);
                if (((Integer) message.obj).intValue() == 801) {
                    HwAPPQoEStateMachine hwAPPQoEStateMachine6 = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine6.stopAPPMonitor(hwAPPQoEStateMachine6.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                    handleChrMpLinkSuccess();
                    HwAPPQoEStateMachine hwAPPQoEStateMachine7 = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine7.transitionTo(hwAPPQoEStateMachine7.mCellMonitorState);
                }
            } else if (i != 202) {
                switch (i) {
                    case 100:
                    case 102:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_APP_STATE_START/UPDATE", new Object[0]);
                        HwAPPStateInfo infoData = (HwAPPStateInfo) message.obj;
                        if (infoData.mAppType == 2000 && infoData.mScenceId == 200001 && HwAPPQoEGameCallback.isHasGameCacheWarInfo(infoData.mAppUID)) {
                            infoData.mScenceId = 200002;
                            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "has the game cashe war info", new Object[0]);
                        }
                        infoData.mNetworkType = 800;
                        if (!HwAPPQoEStateMachine.this.curAPPStateInfo.isObjectValueEqual(infoData) && message.what == 100 && HwAPPQoEStateMachine.this.curAPPStateInfo.mAppType == 2000) {
                            if (HwAPPQoEStateMachine.this.curAPPStateInfo.mScenceId == 200002) {
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "alread in war", new Object[0]);
                                break;
                            } else if (infoData.mScenceId == 200002) {
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "update in war", new Object[0]);
                                message.what = 102;
                            }
                        }
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                        HwAPPQoEStateMachine.this.updateCurAPPStateInfo(infoData);
                        HwAPPQoEStateMachine.this.startAPPMonitor(infoData, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                        startChrState();
                        startWeakNetworkMonitor();
                        break;
                    default:
                        switch (i) {
                            case 104:
                                break;
                            case 105:
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_APP_STATE_MONITOR", new Object[0]);
                                HwAPPStateInfo infoData2 = (HwAPPStateInfo) message.obj;
                                HwAPPQoEStateMachine.this.updateGameRTT(infoData2.mAppRTT, infoData2.mAppId);
                                infoData2.mNetworkType = 800;
                                HwAPPQoEStateMachine.this.notifyAPPRttInfoCallback(infoData2);
                                break;
                            case 106:
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_APP_STATE_GOOD", new Object[0]);
                                HwAPPStateInfo infoData3 = (HwAPPStateInfo) message.obj;
                                infoData3.mNetworkType = 800;
                                HwAPPQoEStateMachine.this.notifyAPPQualityCallback(infoData3, message.what, true);
                                break;
                            case 107:
                                if (HwAPPQoEStateMachine.this.isScreenOn()) {
                                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_APP_STATE_BAD", new Object[0]);
                                    HwAPPStateInfo infoData4 = (HwAPPStateInfo) message.obj;
                                    infoData4.mNetworkType = 800;
                                    HwAPPQoEStateMachine.this.notifyAPPQualityCallback(infoData4, message.what, false);
                                    handleChrAppBad();
                                    break;
                                } else {
                                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState app is bad but screen is off", new Object[0]);
                                    break;
                                }
                            default:
                                return false;
                        }
                    case 101:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState MSG_APP_STATE_BACKGROUND/END", new Object[0]);
                        HwAPPStateInfo infoData5 = (HwAPPStateInfo) message.obj;
                        infoData5.mNetworkType = 800;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData5, message.what);
                        if (true == HwAPPQoEStateMachine.this.isStopInfoMatchStartInfo(infoData5)) {
                            HwAPPQoEStateMachine.this.stopAPPMonitor(infoData5, HwAPPQoEStateMachine.STATE_MACHINE_NAME_WIFI);
                            HwAPPQoEStateMachine hwAPPQoEStateMachine8 = HwAPPQoEStateMachine.this;
                            hwAPPQoEStateMachine8.transitionTo(hwAPPQoEStateMachine8.mIdleState);
                            break;
                        }
                        break;
                }
            } else {
                HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.reRegisterAllGameCallbacks();
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
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState startWeakNetworkMonitor curAPPStateInfo.mAppId = %{public}d", Integer.valueOf(HwAPPQoEStateMachine.this.curAPPStateInfo.mAppId));
                WifiInfo info = ((WifiManager) HwAPPQoEStateMachine.this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE)).getConnectionInfo();
                if (info != null) {
                    int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
                    if (rssiLevel <= 1 && !HwAPPQoEStateMachine.this.hasMessages(109)) {
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "send weak network delay message", new Object[0]);
                        HwAPPQoEStateMachine.this.sendMessageDelayed(109, 4000);
                    } else if (rssiLevel >= 2 && HwAPPQoEStateMachine.this.hasMessages(109)) {
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "remove weak network delay message", new Object[0]);
                        HwAPPQoEStateMachine.this.removeMessages(109);
                    }
                }
            }
        }

        private void stopWeakNetworkMonitor() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "WiFiMonitorState stopWeakNetworkMonitor", new Object[0]);
            HwAPPQoEStateMachine.this.removeMessages(109);
        }

        private void handleChrMpLinkSuccess() {
            long endAppKQITime = System.currentTimeMillis() - this.mAppKQITimer;
            HwAPPQoEAPKConfig config = HwAPPQoEStateMachine.this.mHwAPPQoeResourceManger.getAPKScenceConfig(HwAPPQoEStateMachine.this.curAPPStateInfo.mScenceId);
            HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 13);
            boolean unused = HwAPPQoEStateMachine.this.isMPLinkSuccess = true;
            if (config != null) {
                if (endAppKQITime <= 1000) {
                    HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 16);
                } else {
                    HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 17);
                }
                WifiConfiguration mConnectedConfig = WifiProCommonUtils.getCurrentWifiConfig(HwAPPQoEStateMachine.this.mWifiManager);
                HwAPPChrExcpInfo info = HwAPPQoEStateMachine.this.mHwAPKQoEQualityMonitor.getAPPQoEInfo();
                if (!(info == null || info.rssi == -1)) {
                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "handleChrMpLinkSuccess info.rssi = %{public}d", Integer.valueOf(info.rssi));
                    if (info.rssi >= 3) {
                        HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 20);
                    } else {
                        HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 21);
                    }
                }
                if (mConnectedConfig != null) {
                    if (WifiProCommonUtils.isWpaOrWpa2(mConnectedConfig)) {
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "handleChrMpLinkSuccess is a home ap", new Object[0]);
                        HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 9);
                    } else {
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "handleChrMpLinkSuccess is a portal or open ap", new Object[0]);
                        HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 10);
                    }
                }
                HwAPPQoEStateMachine.this.sendMessage(203, 4);
            }
        }
    }

    public class CellMonitorState extends State {
        boolean isBadAfterMPLink = false;
        boolean mIsAppStall = false;
        long mMPLinkCheckTimer = 0;
        long mMPLinkStartTraffic = 0;

        public CellMonitorState() {
        }

        public void enter() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "Enter CellMonitorState", new Object[0]);
            int event = 100;
            if (102 == HwAPPQoEStateMachine.this.lastAPPStateSent) {
                event = 102;
            }
            initChrState();
            HwAPPQoEStateMachine hwAPPQoEStateMachine = HwAPPQoEStateMachine.this;
            hwAPPQoEStateMachine.sendMessage(Message.obtain(hwAPPQoEStateMachine.getHandler(), event, HwAPPQoEStateMachine.this.curAPPStateInfo));
        }

        public void exit() {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState is Exit", new Object[0]);
            HwAPPQoEStateMachine.this.removeMessages(107);
        }

        public boolean processMessage(Message message) {
            HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState, msg.what:%{public}d", Integer.valueOf(message.what));
            int i = message.what;
            if (i == 3) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState MSG_WIFI_STATE_CONNECTED", new Object[0]);
                HwAPPQoEStateMachine hwAPPQoEStateMachine = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine.stopAPPMonitor(hwAPPQoEStateMachine.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                endChrState();
                HwAPPQoEStateMachine hwAPPQoEStateMachine2 = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine2.transitionTo(hwAPPQoEStateMachine2.mWiFiMonitorState);
            } else if (i == 8) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState MSG_CELL_STATE_DISCONNECT", new Object[0]);
                HwAPPQoEStateMachine hwAPPQoEStateMachine3 = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine3.stopAPPMonitor(hwAPPQoEStateMachine3.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                endChrState();
                HwAPPQoEStateMachine hwAPPQoEStateMachine4 = HwAPPQoEStateMachine.this;
                hwAPPQoEStateMachine4.transitionTo(hwAPPQoEStateMachine4.mDataOffMonitorState);
            } else if (i == 205) {
                HwAPPQoEStateMachine.this.mHwAPPChrManager.updateStatisInfo(HwAPPQoEStateMachine.this.curAPPStateInfo, 14);
            } else if (i == 201) {
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState MSG_INTERNAL_NETWORK_STATE_CHANGE", new Object[0]);
                int currentNetworkState = ((Integer) message.obj).intValue();
                endChrState();
                if (currentNetworkState == 800) {
                    HwAPPQoEStateMachine hwAPPQoEStateMachine5 = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine5.stopAPPMonitor(hwAPPQoEStateMachine5.curAPPStateInfo, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                    HwAPPQoEStateMachine hwAPPQoEStateMachine6 = HwAPPQoEStateMachine.this;
                    hwAPPQoEStateMachine6.transitionTo(hwAPPQoEStateMachine6.mWiFiMonitorState);
                }
            } else if (i != 202) {
                switch (i) {
                    case 100:
                    case 102:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState MSG_APP_STATE_START/UPDATE", new Object[0]);
                        HwAPPStateInfo infoData = (HwAPPStateInfo) message.obj;
                        infoData.mNetworkType = 801;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData, message.what);
                        HwAPPQoEStateMachine.this.updateCurAPPStateInfo(infoData);
                        HwAPPQoEStateMachine.this.startAPPMonitor(infoData, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                        startChrState();
                        HwAPPQoEStateMachine.this.removeMessages(107);
                        break;
                    default:
                        switch (i) {
                            case 104:
                                break;
                            case 105:
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState MSG_APP_STATE_MONITOR", new Object[0]);
                                HwAPPStateInfo infoData2 = (HwAPPStateInfo) message.obj;
                                HwAPPQoEStateMachine.this.updateGameRTT(infoData2.mAppRTT, infoData2.mAppId);
                                infoData2.mNetworkType = 801;
                                HwAPPQoEStateMachine.this.notifyAPPRttInfoCallback(infoData2);
                                break;
                            case 106:
                                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState MSG_APP_STATE_GOOD", new Object[0]);
                                HwAPPStateInfo infoData3 = (HwAPPStateInfo) message.obj;
                                infoData3.mNetworkType = 801;
                                HwAPPQoEStateMachine.this.notifyAPPQualityCallback(infoData3, message.what, false);
                                HwAPPQoEStateMachine.this.removeMessages(107);
                                break;
                            case 107:
                                if (HwAPPQoEStateMachine.this.isScreenOn()) {
                                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState MSG_APP_STATE_BAD", new Object[0]);
                                    HwAPPStateInfo infoData4 = (HwAPPStateInfo) message.obj;
                                    infoData4.mNetworkType = 801;
                                    HwAPPQoEStateMachine.this.notifyAPPQualityCallback(infoData4, message.what, false);
                                    HwAPPQoEAPKConfig config = HwAPPQoEStateMachine.this.mHwAPPQoeResourceManger.getAPKScenceConfig(infoData4.mScenceId);
                                    if (config != null && !HwAPPQoEStateMachine.this.hasMessages(107)) {
                                        Message msg = Message.obtain();
                                        msg.what = 107;
                                        msg.obj = infoData4;
                                        HwAPPQoEStateMachine.this.sendMessageDelayed(msg, (long) (config.mAppPeriod * 1000));
                                    }
                                    handleChrAppBad();
                                    break;
                                } else {
                                    HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState app is bad but screen is off", new Object[0]);
                                    break;
                                }
                            default:
                                return false;
                        }
                    case 101:
                        HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "CellMonitorState MSG_APP_STATE_BACKGROUND/END", new Object[0]);
                        HwAPPStateInfo infoData5 = (HwAPPStateInfo) message.obj;
                        infoData5.mNetworkType = 801;
                        HwAPPQoEStateMachine.this.notifyAPPStateCallback(infoData5, message.what);
                        endChrState();
                        if (true == HwAPPQoEStateMachine.this.isStopInfoMatchStartInfo(infoData5)) {
                            HwAPPQoEStateMachine.this.stopAPPMonitor(infoData5, HwAPPQoEStateMachine.STATE_MACHINE_NAME_CELL);
                            HwAPPQoEStateMachine hwAPPQoEStateMachine7 = HwAPPQoEStateMachine.this;
                            hwAPPQoEStateMachine7.transitionTo(hwAPPQoEStateMachine7.mIdleState);
                            break;
                        }
                        break;
                }
            } else {
                HwAPPQoEStateMachine.this.mHwAPPQoEContentAware.reRegisterAllGameCallbacks();
            }
            return true;
        }

        private void endChrState() {
            if (HwAPPQoEStateMachine.this.isMPLinkSuccess) {
                long deltaTraffic = ((TrafficStats.getMobileTxBytes() + TrafficStats.getMobileRxBytes()) - this.mMPLinkStartTraffic) / 1024;
                HwAPPQoEUtils.logD(HwAPPQoEStateMachine.TAG, false, "endChrState deltaTraffic = %{public}s KB", String.valueOf(deltaTraffic));
                if (deltaTraffic > 0 && this.mMPLinkStartTraffic != 0) {
                    HwAPPQoEStateMachine.this.mHwAPPChrManager.updateTraffic(HwAPPQoEStateMachine.this.curAPPStateInfo, deltaTraffic);
                }
                this.mMPLinkStartTraffic = 0;
                boolean unused = HwAPPQoEStateMachine.this.isMPLinkSuccess = false;
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

    /* access modifiers changed from: private */
    public void notifyAPPStateCallback(HwAPPStateInfo data, int state) {
        HwAPPQoEUtils.logD(TAG, false, "notifyAPPStateCallback, state:%{public}d, app:%{public}d, scence:%{public}d, action:%{public}d", Integer.valueOf(state), Integer.valueOf(data.mAppId), Integer.valueOf(data.mScenceId), Integer.valueOf(data.mAction));
        if (!this.curAPPStateInfo.isObjectValueEqual(data) || state != this.lastAPPStateSent) {
            HwAPPQoEManager hwAPPQoEManager = HwAPPQoEManager.getInstance();
            IHwAPPQoECallback brainCallback = hwAPPQoEManager.getAPPQoECallback(true);
            data.mAppState = state;
            HwAPPStateInfo tempData = new HwAPPStateInfo();
            tempData.copyObjectValue(data);
            if (brainCallback != null) {
                brainCallback.onAPPStateCallBack(tempData, state);
            }
            IHwAPPQoECallback wmCallback = hwAPPQoEManager.getAPPQoECallback(false);
            if (wmCallback != null) {
                wmCallback.onAPPStateCallBack(tempData, state);
            }
            if (tempData.mAppType == 2000 || tempData.mAppType == 3000 || (HwAPPQoEUtils.IS_TABLET && (tempData.mAction & 2) == 2)) {
                hwAPPQoEManager.notifyGameQoeCallback(tempData, state);
            }
            this.lastAPPStateSent = state;
            return;
        }
        HwAPPQoEUtils.logD(TAG, false, "notifyAPPStateCallback: info sent was same as before", new Object[0]);
    }

    /* access modifiers changed from: private */
    public void notifyAPPQualityCallback(HwAPPStateInfo data, int state, boolean isWmOnly) {
        HwAPPQoEUtils.logD(TAG, false, "notifyAPPQualityCallback:%{public}d,%{public}d", Integer.valueOf(state), Integer.valueOf(data.mAppId));
        HwAPPQoEManager hwAPPQoEManager = HwAPPQoEManager.getInstance();
        IHwAPPQoECallback brainCallback = hwAPPQoEManager.getAPPQoECallback(true);
        data.setExperience(state);
        HwAPPStateInfo tempData = new HwAPPStateInfo();
        tempData.copyObjectValue(data);
        MpLinkQuickSwitchConfiguration switchConfiguration = tempData.getQuickSwitchConfiguration();
        if (switchConfiguration != null) {
            switchConfiguration.setSocketStrategy(3);
            switchConfiguration.setNetworkStrategy(0);
        }
        if (brainCallback != null && !isWmOnly) {
            brainCallback.onAPPQualityCallBack(tempData, state);
        }
        IHwAPPQoECallback wmCallback = hwAPPQoEManager.getAPPQoECallback(false);
        if (wmCallback != null) {
            wmCallback.onAPPQualityCallBack(tempData, state);
        }
    }

    /* access modifiers changed from: private */
    public void notifyAPPRttInfoCallback(HwAPPStateInfo data) {
        HwAPPQoEUtils.logD(TAG, false, "notifyAPPRttInfoCallback:%{public}s", data.toString());
        HwAPPQoEManager hwAPPQoEManager = HwAPPQoEManager.getInstance();
        IHwAPPQoECallback brainCallback = hwAPPQoEManager.getAPPQoECallback(true);
        HwAPPStateInfo tempData = new HwAPPStateInfo();
        tempData.copyObjectValue(data);
        if (brainCallback != null) {
            brainCallback.onAPPRttInfoCallBack(tempData);
        }
        IHwAPPQoECallback wmCallback = hwAPPQoEManager.getAPPQoECallback(false);
        if (wmCallback != null) {
            wmCallback.onAPPRttInfoCallBack(tempData);
        }
    }

    /* access modifiers changed from: private */
    public int quryCurrentNetwork(int uid) {
        return HwArbitrationFunction.getCurrentNetwork(this.mContext, uid);
    }

    /* access modifiers changed from: private */
    public void startAPPMonitor(HwAPPStateInfo appStateInfo, String stateName) {
        HwAPPQoEUtils.logD(TAG, false, "startAPPMonitor -- appStateInfo:%{public}s", appStateInfo.toString());
        if (1000 == appStateInfo.mAppType) {
            if (!HwAPPQoEManager.isAppStartMonitor(appStateInfo, this.mContext)) {
                HwAPPQoEUtils.logD(TAG, false, "isAppStartMonitor false", new Object[0]);
            } else if (appStateInfo.mScenceType == 4 || appStateInfo.mScenceType == 5) {
                HwAPPQoEUtils.logD(TAG, false, "not startAPPMonitor", new Object[0]);
            } else {
                HwAPPQoEUtils.logD(TAG, false, "startAPPMonitor -- stateName:%{public}s", stateName);
                if (STATE_MACHINE_NAME_WIFI.equals(stateName)) {
                    this.mHwAPKQoEQualityMonitor.startMonitor(this.curAPPStateInfo);
                } else if (STATE_MACHINE_NAME_CELL.equals(stateName)) {
                    this.mHwAPKQoEQualityMonitorCell.startMonitor(this.curAPPStateInfo);
                }
            }
        } else if (2000 == appStateInfo.mAppType) {
            HwAPPQoEUtils.logD(TAG, false, "startAPPMonitor for game", new Object[0]);
            this.mHwGameQoEManager.startMonitor(appStateInfo);
        }
    }

    /* access modifiers changed from: private */
    public void stopAPPMonitor(HwAPPStateInfo appStateInfo, String stateName) {
        HwAPPQoEUtils.logD(TAG, false, "stopAPPMonitor -- appStateInfo:%{public}s", appStateInfo.toString());
        if (1000 == appStateInfo.mAppType) {
            if (STATE_MACHINE_NAME_WIFI.equals(stateName)) {
                this.mHwAPKQoEQualityMonitor.stopMonitor();
            } else if (STATE_MACHINE_NAME_CELL.equals(stateName)) {
                this.mHwAPKQoEQualityMonitorCell.stopMonitor();
            }
        } else if (2000 == appStateInfo.mAppType) {
            HwAPPQoEUtils.logD(TAG, false, "stopAPPMonitor for game", new Object[0]);
            this.mHwGameQoEManager.stopMonitor();
        }
    }

    /* access modifiers changed from: private */
    public void updateGameRTT(int rtt, int gameId) {
        HwGameQoEManager hwGameQoEManager = this.mHwGameQoEManager;
        if (hwGameQoEManager == null || hwGameQoEManager.mGameId != gameId) {
            HwAPPQoEUtils.logD(TAG, false, "updateGameRTT, game not found in game list, internal error", new Object[0]);
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

    /* access modifiers changed from: private */
    public void updateCurAPPStateInfo(HwAPPStateInfo infoData) {
        this.curAPPStateInfo.copyObjectValue(infoData);
        this.mHwAPPQoESystemStateMonitor.curAPPStateInfo.copyObjectValue(infoData);
    }

    /* access modifiers changed from: private */
    public boolean isScreenOn() {
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (pm == null || !pm.isScreenOn()) {
            return false;
        }
        return true;
    }

    @Override // com.android.server.hidata.wavemapping.IWaveMappingCallback
    public void onWaveMappingReportCallback(int reportType, String networkName, int networkType) {
        HwAPPQoEUtils.logD(TAG, false, "onWaveMappingReportCallback", new Object[0]);
        HwAPPStateInfo curAppInfo = getCurAPPStateInfo();
        if (curAppInfo != null) {
            HwAPPQoEUtils.logD(TAG, false, "onWaveMappingReportCallback, app: %{public}d", Integer.valueOf(curAppInfo.mAppId));
            IHwAPPQoECallback brainCallback = HwAPPQoEManager.getInstance().getAPPQoECallback(true);
            if (brainCallback != null) {
                brainCallback.onAPPQualityCallBack(curAppInfo, 107);
            }
        }
    }

    @Override // com.android.server.hidata.wavemapping.IWaveMappingCallback
    public void onWaveMappingRespondCallback(int UID, int prefer, int network, boolean isGood, boolean found) {
    }

    @Override // com.android.server.hidata.wavemapping.IWaveMappingCallback
    public void onWaveMappingRespond4BackCallback(int UID, int prefer, int network, boolean isGood, boolean found) {
    }
}
