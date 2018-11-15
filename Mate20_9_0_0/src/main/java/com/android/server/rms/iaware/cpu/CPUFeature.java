package com.android.server.rms.iaware.cpu;

import android.content.Context;
import android.iawareperf.UniPerf;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.DumpData;
import android.rms.iaware.IAwaredConnection;
import android.rms.iaware.StatisticsData;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.service.vr.IVrStateCallbacks.Stub;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.feature.RFeature;
import com.huawei.android.app.HwActivityManager;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import vendor.huawei.hardware.iawareperfpolicy.V1_0.IPerfPolicy;

public class CPUFeature extends RFeature {
    private static final int BASE_VERSION = 1;
    private static final int BIGDATA_RECORD_PERIOD = 600;
    private static final int CONNECT_VR_DELAYED = 3000;
    private static final int CYCLE_MAX_NUM = 5;
    private static final int MSG_BASE_VALUE = 100;
    public static final int MSG_BINDER_THREAD_CREATE = 151;
    public static final int MSG_BOOST_KILL_SWITCH = 107;
    public static final int MSG_CPUCTL_SUBSWITCH = 140;
    public static final int MSG_CPUFEATURE_OFF = 114;
    public static final int MSG_ENABLE_EAS = 145;
    public static final int MSG_ENTER_GAME_SCENE = 147;
    public static final int MSG_EXIT_GAME_SCENE = 148;
    public static final int MSG_GAME_SCENE_LEVEL = 157;
    public static final int MSG_MOVETO_BACKGROUND = 105;
    public static final int MSG_MOVETO_K_BACKGROUND = 104;
    public static final int MSG_PROCESS_GROUP_CHANGE = 106;
    public static final int MSG_RESET_BOOST_CPUS = 136;
    public static final int MSG_RESET_ON_FIRE = 154;
    public static final int MSG_RESET_TOP_APP_CPUSET = 117;
    public static final int MSG_RESET_VIP_THREAD = 144;
    public static final int MSG_SCREEN_OFF = 102;
    public static final int MSG_SCREEN_ON = 101;
    public static final int MSG_SET_BG_UIDS = 142;
    public static final int MSG_SET_BOOST_CPUS = 135;
    public static final int MSG_SET_CPUCONFIG = 124;
    public static final int MSG_SET_CPUSETCONFIG_SCREENOFF = 127;
    public static final int MSG_SET_CPUSETCONFIG_SCREENON = 126;
    public static final int MSG_SET_CPUSETCONFIG_VR = 125;
    public static final int MSG_SET_FG_CGROUP = 139;
    public static final int MSG_SET_FG_UIDS = 141;
    public static final int MSG_SET_LIMIT_CGROUP = 137;
    public static final int MSG_SET_THREAD_TO_TA = 146;
    public static final int MSG_SET_TOP_APP_CPUSET = 116;
    public static final int MSG_SET_VIP_THREAD = 143;
    public static final int MSG_SET_VIP_THREAD_PARAMS = 150;
    public static final int MSG_THREAD_BOOST = 115;
    public static final int MSG_UNIPERF_BOOST_OFF = 119;
    public static final int MSG_UNIPERF_BOOST_OFF_SEND = 120;
    public static final int MSG_UNIPERF_BOOST_ON = 118;
    public static final int MSG_VIP_THREAD_DYM_GRAN = 2;
    public static final int MSG_VIP_THREAD_MIGRATION = 3;
    public static final int MSG_VIP_THREAD_SCHED_DELAY = 1;
    private static final int MSG_VR_CONNECT = 134;
    public static final int MSG_VR_OFF = 123;
    public static final int MSG_VR_ON = 122;
    private static final int SCREEN_OFF_DELAYED = 5000;
    public static final int SEND_BYTE_NULL = -1;
    public static final int SEND_RETRY_TIME = 2;
    public static final int SEND_RETRY_TIME_OUT = -2;
    public static final int SEND_SUCCESS = 1;
    private static final int SWITCH_APP_START_ON_FIRE = 131072;
    private static final int SWITCH_APP_WARMCOLD_START = 524288;
    public static final int SWITCH_CPUCTL_FG_DETECT = 1024;
    private static final int SWITCH_EAS_MODE = 8192;
    private static final int SWITCH_GAME_SCENE = 16384;
    public static final int SWITCH_NET_MANAGE = 512;
    private static final int SWITCH_ON_DEMAND_BOOST = 262144;
    public static final int SWITCH_THREAD_BOOST = 16;
    public static final int SWITCH_TOPAPP_BOOST = 32;
    public static final int SWITCH_VIP_MODE = 2048;
    public static final int SWITCH_VR_MODE = 256;
    private static final String TAG = "CPUFeature";
    private static boolean mCPUSetEnable;
    private CPUFeatureHandler mCPUFeatureHandler = new CPUFeatureHandler();
    private CPUFreqInteractive mCPUFreqInteractive;
    private CPUGameFreq mCPUGameFreq;
    private CPUNetLink mCPUNetLink;
    private CPUProcessInherit mCPUProcessInherit = null;
    private CPUXmlConfiguration mCPUXmlConfiguration;
    private int mCycleNum = 0;
    private int mFeatureFlag = 0;
    private IVrManager mVRManager = null;
    private final IVrStateCallbacks mVrStateCallback = new Stub() {
        public void onVrStateChanged(boolean enable) throws RemoteException {
            String str = CPUFeature.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("vr state change:");
            stringBuilder.append(enable);
            AwareLog.d(str, stringBuilder.toString());
            CPUFeature.this.processVRConnected(enable);
        }
    };

    public class CPUFeatureHandler extends Handler {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 101:
                    CPUFeature.this.doScreenOn();
                    SchedLevelBoost.getInstance().onScreenStateChanged(true);
                    return;
                case 102:
                    SchedLevelBoost.getInstance().onScreenStateChanged(false);
                    CPUFeature.this.doScreenOff();
                    CPUGameScene.getInstance().setScreenOffScene();
                    return;
                case CPUFeature.MSG_VR_CONNECT /*134*/:
                    CPUFeature.this.connectVRListner();
                    return;
                case CPUFeature.MSG_SET_BOOST_CPUS /*135*/:
                    CPUFeature.this.sendPacketByMsgCode(CPUFeature.MSG_SET_BOOST_CPUS);
                    return;
                case CPUFeature.MSG_RESET_BOOST_CPUS /*136*/:
                    CPUFeature.this.sendPacketByMsgCode(CPUFeature.MSG_RESET_BOOST_CPUS);
                    return;
                case 140:
                    CPUFeature.this.doSendCpuCtlSwitchMsg();
                    return;
                case CPUFeature.MSG_SET_VIP_THREAD /*143*/:
                    CPUFeature.this.doVipThreadSet(msg);
                    return;
                case CPUFeature.MSG_RESET_VIP_THREAD /*144*/:
                    CPUFeature.this.doVipThreadSet(msg);
                    return;
                case CPUFeature.MSG_ENTER_GAME_SCENE /*147*/:
                    CPUGameScene.getInstance().setGameScene();
                    return;
                case CPUFeature.MSG_EXIT_GAME_SCENE /*148*/:
                    CPUGameScene.getInstance().resetGameScene();
                    return;
                case CPUFeature.MSG_RESET_ON_FIRE /*154*/:
                    CPUAppStartOnFire.getInstance().resetOnFire();
                    return;
                default:
                    String str = CPUFeature.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("error msg what = ");
                    stringBuilder.append(msg.what);
                    AwareLog.e(str, stringBuilder.toString());
                    return;
            }
        }
    }

    private boolean isFeatureEnable(int f) {
        return mCPUSetEnable && (this.mFeatureFlag & f) != 0;
    }

    public CPUFeature(Context context, FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
        this.mCPUFreqInteractive = new CPUFreqInteractive(context);
        this.mCPUGameFreq = new CPUGameFreq();
    }

    public ArrayList<DumpData> getDumpData(int time) {
        return null;
    }

    public ArrayList<StatisticsData> getStatisticsData() {
        return null;
    }

    public boolean custConfigUpdate() {
        if (UniPerf.getInstance().uniPerfEvent(20230, "", new int[0]) == 0) {
            return true;
        }
        AwareLog.d(TAG, "fail to update uniperf config.");
        return false;
    }

    public boolean reportData(CollectData data) {
        if (!mCPUSetEnable) {
            return false;
        }
        if (data == null) {
            AwareLog.e(TAG, "reportData: data is null!");
            return false;
        }
        int resId = data.getResId();
        if (resId == ResourceType.RESOURCE_SCREEN_ON.ordinal()) {
            UniPerf.getInstance().uniPerfEvent(20226, "", new int[]{1});
            removeScreenMessages();
            this.mCPUFeatureHandler.sendEmptyMessageDelayed(101, 0);
        } else if (resId == ResourceType.RESOURCE_SCREEN_OFF.ordinal()) {
            UniPerf.getInstance().uniPerfEvent(20226, "", new int[]{0});
            removeScreenMessages();
            this.mCPUFeatureHandler.sendEmptyMessageDelayed(102, 5000);
        } else if (resId == ResourceType.RESOURCE_GAME_BOOST.ordinal()) {
            this.mCPUGameFreq.gameFreq(data.getBundle(), isFeatureEnable(8192));
        } else if (resId == ResourceType.RESOURCE_NET_MANAGE.ordinal()) {
            Bundle bundle = data.getBundle();
            if (bundle != null) {
                int enable = bundle.getInt("enbale", 0);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("reportData: RESOURCE_NET_MANAGE = ");
                stringBuilder.append(enable);
                AwareLog.d(str, stringBuilder.toString());
                NetManager.getInstance().sendMsgToNetMng(enable, 0, 12);
            }
        }
        return true;
    }

    private void subscribeResourceTypes() {
        this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
        this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
        this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_GAME_BOOST, this.mFeatureType);
        this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_NET_MANAGE, this.mFeatureType);
    }

    private void unsubscribeResourceTypes() {
        this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
        this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
        this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_GAME_BOOST, this.mFeatureType);
        this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_NET_MANAGE, this.mFeatureType);
    }

    private void enableVRMode() {
        if (isFeatureEnable(256)) {
            connectVRListner();
        }
    }

    private void enableGameScene() {
        if (isFeatureEnable(SWITCH_GAME_SCENE)) {
            CPUGameScene.getInstance().enable(this.mCPUFeatureHandler);
        }
    }

    private void enableVipThread() {
        if (isFeatureEnable(2048)) {
            CPUVipThread.getInstance().start(this);
            CPUVipThread.getInstance().setHandler(this.mCPUFeatureHandler);
            enableVipCgroupControl();
        }
    }

    private void notifyCpusetSwitch(boolean enable) {
        HwActivityManager.setCpusetSwitch(enable);
    }

    private void notifyWarmColdSwitch(boolean enable) {
        HwActivityManager.setWarmColdSwitch(enable);
    }

    private void enableCPUHighFgControl() {
        if (isFeatureEnable(1024)) {
            CPUHighFgControl.getInstance().start(this);
            handleSendMsg(140);
        }
    }

    private void enableVipCgroupControl() {
        VipCgroupControl.getInstance().enable(this);
    }

    private void enableCPUNetLink() {
        if ((isFeatureEnable(1024) || isFeatureEnable(2048) || isFeatureEnable(16)) && this.mCPUNetLink == null) {
            this.mCPUNetLink = new CPUNetLink();
            this.mCPUNetLink.start();
        }
    }

    private void getEasSwitch() {
        if (isFeatureEnable(8192)) {
            sendPacketByMsgCode(MSG_ENABLE_EAS);
        }
    }

    private void enableNetManager() {
        if (isFeatureEnable(512)) {
            NetManager.getInstance().enable(this);
        }
    }

    private void sendSubSwitchToUniperf() {
        UniPerf.getInstance().uniPerfEvent(20225, "", new int[]{this.mFeatureFlag});
    }

    private void enableAppStartOnFire() {
        if (isFeatureEnable(131072)) {
            CPUAppStartOnFire.getInstance().enable(this.mCPUFeatureHandler);
        }
    }

    public boolean enable() {
        if (mCPUSetEnable) {
            AwareLog.e(TAG, "cpuset has already enabled!");
            return true;
        }
        if (this.mCPUXmlConfiguration == null) {
            this.mCPUXmlConfiguration = new CPUXmlConfiguration();
        }
        this.mCPUXmlConfiguration.startSetProperty(this);
        setEnable(true);
        notifyCpusetSwitch(true);
        CPUKeyBackground.getInstance().start(this);
        setBoostKillSwitch(true);
        CPUPowerMode.getInstance().enable(this.mContext);
        try {
            this.mFeatureFlag = Integer.parseInt(SystemProperties.get("persist.sys.cpuset.subswitch", "0"));
        } catch (IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enable parseInt catch exception msg = ");
            stringBuilder.append(e.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        }
        notifyWarmColdSwitch(isFeatureEnable(SWITCH_APP_WARMCOLD_START));
        if (isFeatureEnable(16)) {
            CpuThreadBoost.getInstance().start(this, this.mCPUFeatureHandler);
        }
        if (this.mCPUProcessInherit == null) {
            this.mCPUProcessInherit = new CPUProcessInherit();
        }
        this.mCPUProcessInherit.registerPorcessObserver();
        subscribeResourceTypes();
        if (isScreenOn()) {
            doScreenOn();
        }
        if (isFeatureEnable(16) || isFeatureEnable(SWITCH_GAME_SCENE)) {
            this.mCPUFreqInteractive.startGameStateMoniter();
        }
        CPUResourceConfigControl.getInstance().enable();
        CPUFeatureAMSCommunicator.getInstance().start(this);
        CPUFeatureAMSCommunicator.getInstance().setTopAppBoostEnable(isFeatureEnable(32));
        CPUFeatureAMSCommunicator.getInstance().setOnDemandBoostEnable(isFeatureEnable(262144));
        if (isFeatureEnable(262144)) {
            sendOnDemandBoostParams();
        }
        enableVRMode();
        AwareLog.d(TAG, "CPUFeature enabled");
        return true;
    }

    public boolean enableFeatureEx(int realVersion) {
        if (realVersion >= 1) {
            enable();
        }
        if (realVersion >= 2) {
            getEasSwitch();
            enableCPUNetLink();
            enableCPUHighFgControl();
            enableVipThread();
            enableNetManager();
            enableGameScene();
            sendSubSwitchToUniperf();
            enableAppStartOnFire();
        }
        return true;
    }

    private void disableCPUHighFgControl() {
        if (isFeatureEnable(1024)) {
            CPUHighFgControl.getInstance().stop();
            handleSendMsg(140);
        }
    }

    private void disableVipThread() {
        if (isFeatureEnable(2048)) {
            CPUVipThread.getInstance().stop();
            disableVipCgroupControl();
        }
    }

    private void disableVipCgroupControl() {
        VipCgroupControl.getInstance().disable();
    }

    private void disableCPUNetLink() {
        if (this.mCPUNetLink != null) {
            this.mCPUNetLink.stop();
            this.mCPUNetLink = null;
        }
    }

    private void disableNetManager() {
        if (isFeatureEnable(512)) {
            NetManager.getInstance().disable();
        }
    }

    private void disableGameScene() {
        if (isFeatureEnable(SWITCH_GAME_SCENE)) {
            CPUGameScene.getInstance().disable();
        }
    }

    private void disableAppStartOnFire() {
        if (isFeatureEnable(131072)) {
            CPUAppStartOnFire.getInstance().disable();
        }
    }

    public boolean disable() {
        if (mCPUSetEnable) {
            if (isFeatureEnable(16) || isFeatureEnable(SWITCH_GAME_SCENE)) {
                this.mCPUFreqInteractive.stopGameStateMoniter();
            }
            CPUFeatureAMSCommunicator.getInstance().setTopAppBoostEnable(false);
            CPUFeatureAMSCommunicator.getInstance().setOnDemandBoostEnable(false);
            CPUFeatureAMSCommunicator.getInstance().stop();
            AwareLog.d(TAG, "CPUFeature destroyed");
            CPUKeyBackground.getInstance().destroy();
            if (isFeatureEnable(16)) {
                CpuThreadBoost.getInstance().stop();
            }
            disableCPUHighFgControl();
            disableNetManager();
            CPUPowerMode.getInstance().disable();
            disableVipThread();
            disableCPUNetLink();
            setBoostKillSwitch(false);
            if (this.mCPUProcessInherit != null) {
                this.mCPUProcessInherit.unregisterPorcessObserver();
            }
            doFeatureOff();
            disableAppStartOnFire();
            setEnable(false);
            notifyCpusetSwitch(false);
            notifyWarmColdSwitch(false);
            unsubscribeResourceTypes();
            if (isFeatureEnable(256)) {
                callUnRegisterVRListener();
                this.mCPUFeatureHandler.removeMessages(MSG_VR_CONNECT);
            }
            CPUResourceConfigControl.getInstance().disable();
            disableGameScene();
            return true;
        }
        AwareLog.e(TAG, "cpuset has already disabled!");
        return true;
    }

    private void setEnable(boolean enable) {
        try {
            SystemProperties.set("persist.sys.cpuset.enable", enable ? "1" : "0");
            mCPUSetEnable = enable;
        } catch (Exception e) {
            AwareLog.e(TAG, "setEnable catch exception!");
        }
    }

    public static boolean isCpusetEnable() {
        return mCPUSetEnable;
    }

    private int doScreenOn() {
        AwareLog.d(TAG, "get screen on event");
        sendPacketByMsgCode(101);
        return 0;
    }

    private int doScreenOff() {
        AwareLog.d(TAG, "get screen off event");
        sendPacketByMsgCode(102);
        return 0;
    }

    private int doFeatureOff() {
        sendPacketByMsgCode(114);
        return 0;
    }

    public synchronized int sendPacket(ByteBuffer buffer) {
        if (buffer == null) {
            return -1;
        }
        int retry = 2;
        do {
            if (IAwaredConnection.getInstance().sendPacket(buffer.array(), 0, buffer.position())) {
                return 1;
            }
            retry--;
        } while (retry > 0);
        return -2;
    }

    private boolean isScreenOn() {
        return ((PowerManager) this.mContext.getSystemService("power")).isInteractive();
    }

    private void removeScreenMessages() {
        this.mCPUFeatureHandler.removeMessages(101);
        this.mCPUFeatureHandler.removeMessages(102);
    }

    private int setBoostKillSwitch(boolean isEnable) {
        if (!isCpusetEnable()) {
            return 0;
        }
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(107);
        buffer.putInt(isEnable);
        int resCode = sendPacket(buffer);
        if (resCode != 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setBoostKillSwitch sendPacket failed, isEnable:");
            stringBuilder.append(isEnable);
            stringBuilder.append(",send error code:");
            stringBuilder.append(resCode);
            AwareLog.e(str, stringBuilder.toString());
        }
        return 0;
    }

    private void sendPacketByMsgCode(int msg) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(msg);
        int resCode = sendPacket(buffer);
        if (resCode != 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendPacketByMsgCode sendPacket failed, msg:");
            stringBuilder.append(msg);
            stringBuilder.append(",send error code:");
            stringBuilder.append(resCode);
            AwareLog.e(str, stringBuilder.toString());
        }
    }

    private void processVRConnected(boolean enable) {
        if (enable) {
            if (isFeatureEnable(32)) {
                CPUFeatureAMSCommunicator.getInstance().stop();
            }
            sendPacketByMsgCode(122);
            return;
        }
        if (isFeatureEnable(32)) {
            CPUFeatureAMSCommunicator.getInstance().start(this);
        }
        if (isScreenOn()) {
            sendPacketByMsgCode(123);
        }
    }

    private void connectVRListner() {
        this.mVRManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        if (this.mVRManager != null || this.mCycleNum >= 5) {
            this.mCycleNum = 0;
            callRegisterVRListener();
            return;
        }
        this.mCycleNum++;
        this.mCPUFeatureHandler.removeMessages(MSG_VR_CONNECT);
        this.mCPUFeatureHandler.sendEmptyMessageDelayed(MSG_VR_CONNECT, HwArbitrationDEFS.NotificationMonitorPeriodMillis);
    }

    private void callRegisterVRListener() {
        if (this.mVRManager != null) {
            try {
                this.mVRManager.registerListener(this.mVrStateCallback);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("failed to register vr mode state listner:");
                stringBuilder.append(e);
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }

    private void callUnRegisterVRListener() {
        if (this.mVRManager != null) {
            try {
                this.mVRManager.unregisterListener(this.mVrStateCallback);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("failed to unregister vr mode state listner:");
                stringBuilder.append(e);
                AwareLog.e(str, stringBuilder.toString());
            }
        }
    }

    private void doSendCpuCtlSwitchMsg() {
        sendPacketByMsgValueCode(140, isFeatureEnable(1024));
    }

    private void handleSendMsg(int msg) {
        this.mCPUFeatureHandler.removeMessages(msg);
        this.mCPUFeatureHandler.sendEmptyMessage(msg);
    }

    private void sendPacketByMsgValueCode(int msg, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(msg);
        buffer.putInt(value);
        int resCode = sendPacket(buffer);
        if (resCode != 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendPacketByMsgPidCode sendPacket failed, msg, value:");
            stringBuilder.append(msg);
            stringBuilder.append(value);
            stringBuilder.append(", send error code:");
            stringBuilder.append(resCode);
            AwareLog.e(str, stringBuilder.toString());
        }
    }

    private void doVipThreadSet(Message msg) {
        int msgId = msg.what;
        int pid = msg.arg1;
        if (msg.obj != null && pid > 0) {
            CPUVipThread.getInstance().sendPacket(pid, msg.obj, msgId);
        }
    }

    private void sendOnDemandBoostParams() {
        String str;
        StringBuilder stringBuilder;
        try {
            IPerfPolicy perfPolicy = IPerfPolicy.getService("perfpolicy");
            if (perfPolicy != null) {
                int coldstartDuration = OnDemandBoost.getInstance().getColdStartDuration();
                int winswitchDuration = OnDemandBoost.getInstance().getWindowSwitchDuration();
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sendOnDemandBoostParams coldstartDuration =  ");
                stringBuilder2.append(coldstartDuration);
                stringBuilder2.append(" winswitchDuration = ");
                stringBuilder2.append(winswitchDuration);
                AwareLog.d(str2, stringBuilder2.toString());
                perfPolicy.setOnDemandBoostPolicy("cold", coldstartDuration);
                perfPolicy.setOnDemandBoostPolicy("winswitch", winswitchDuration);
            }
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sendOnDemandBoostParams catch RemoteException = ");
            stringBuilder.append(e.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        } catch (NoSuchElementException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sendOnDemandBoostParams catch NoSuchElementException = ");
            stringBuilder.append(e2.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        }
    }
}
