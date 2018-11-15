package com.android.server.hidata.arbitration;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.telephony.SubscriptionManager;
import android.widget.Toast;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.hidata.appqoe.HwAPPChrManager;
import com.android.server.hidata.appqoe.HwAPPQoEManager;
import com.android.server.hidata.appqoe.HwAPPQoEResourceManger;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.appqoe.HwAPPStateInfo;
import com.android.server.hidata.channelqoe.HwChannelQoEManager;
import com.android.server.hidata.hiradio.HwHiRadioBoost;
import com.android.server.hidata.hiradio.HwWifiBoost;
import com.android.server.hidata.histream.HwHiStreamManager;
import com.android.server.hidata.mplink.HwMplinkManager;
import com.android.server.hidata.mplink.MpLinkQuickSwitchConfiguration;
import com.android.server.hidata.mplink.MplinkBindResultInfo;
import com.android.server.hidata.wavemapping.HwWaveMappingManager;
import com.android.server.net.HwNetworkStatsService;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.android.net.hwmplink.HwHiDataCommonUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class HwArbitrationStateMachine extends StateMachine {
    private static final long QueryWaitTime = 20000;
    private static final String TAG = "HiData_HwArbitrationStateMachine";
    private static final int hiCureDelayTime = 2000;
    private static HwArbitrationStateMachine mArbitrationStateMachine;
    private static HashMap<Integer, HwArbitrationAppBoostInfo> mHwArbitrationAppBoostMap;
    private long hiCureDefaultOvertime = HwNetworkStatsService.UPLOAD_INTERVAL;
    private long hiCureDefaultOvertimeReset = 180000;
    private long hiCureInformBlockTime = 86400000;
    private long hiCureWifiDisconnectedDelayTime = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
    private boolean isAllowHiCure = true;
    private boolean isAllowQueryChQoE = true;
    private int isStallAfterCure = 0;
    private long lastReceiveResultTime = 0;
    private State mCellMonitorState = new CellMonitorState();
    private int mCoexCount = 0;
    private Context mContext;
    private int mCurrentActiveNetwork = 802;
    private int mCurrentServiceState = 1;
    private HwAPPStateInfo mCurrentStreamAppInfo;
    private State mDefaultState = new DefaultState();
    private boolean mDenyByNotification = false;
    private boolean mDeviceBootCommpleted = false;
    private boolean mHiStreamTriggerOrStopMplink = true;
    HwAPPQoEManager mHwAPPQoEManager = null;
    private HwAPPQoEResourceManger mHwAPPQoEResourceManger;
    HwArbitrationChrImpl mHwArbitrationChrImpl;
    private HwHiRadioBoost mHwHiRadioBoost;
    HwHiStreamManager mHwHiStreamManager = null;
    private HwWifiBoost mHwWifiBoost = null;
    private State mInitialState = new InitialState();
    private boolean mIsMpLinkBinding = false;
    private boolean mIsMpLinkError = false;
    private State mMPLinkStartedState = new MPLinkStartedState();
    private State mMPLinkStartingState = new MPLinkStartingState();
    private State mMPLinkStoppingState = new MPLinkStoppingState();
    private int mMpLinkCount = 0;
    private HashMap<Integer, Long> mQueryTime;
    private State mWifiMonitorState = new WifiMonitorState();
    private boolean monitorStallAfterCure = false;
    private long pingPongTMCell_Bad = 0;
    private boolean trgPingPongCell_Bad = false;
    private long triggerMPlinkInternal = 20000;

    class CellMonitorState extends State {
        private int actions = 0;
        private int appUID = -1;
        private HwAPPStateInfo mAppInfo = null;

        CellMonitorState() {
        }

        public void enter() {
            String str;
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Enter CellMonitorState");
            HwArbitrationStateMachine.this.mHwAPPQoEManager = HwAPPQoEManager.getInstance();
            this.mAppInfo = HwArbitrationStateMachine.this.mHwAPPQoEManager == null ? null : HwArbitrationStateMachine.this.mHwAPPQoEManager.getCurAPPStateInfo();
            String str2 = HwArbitrationStateMachine.TAG;
            if (this.mAppInfo == null) {
                str = "mAppInfo is null ";
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mAppInfo:");
                stringBuilder.append(this.mAppInfo.toString());
                str = stringBuilder.toString();
            }
            HwArbitrationCommonUtils.logD(str2, str);
            if (this.mAppInfo != null && HwArbitrationFunction.isDataTechSuitable()) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "begin HiRadio  Cellular boost");
                handleCurrentScene(this.mAppInfo);
                handleHiStreamScene();
            }
        }

        private void handleCurrentScene(HwAPPStateInfo appInfo) {
            if (appInfo != null) {
                int i = appInfo.mAppType;
                if (i == 1000) {
                    HwArbitrationStateMachine.this.sendMessage(109, appInfo);
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "CurrentScene:MSG_INSTANT_APP_START");
                } else if (i != 2000) {
                    if (i != HwAPPQoEUtils.APP_TYPE_STREAMING) {
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "CurrentScene:no AppQoE monitored scenes at foreground");
                    } else {
                        HwArbitrationStateMachine.this.sendMessage(106, appInfo);
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "CurrentScene:MSG_STREAM_APP_START");
                    }
                } else if (appInfo.mScenceId == HwAPPQoEUtils.GAME_SCENCE_IN_WAR) {
                    HwArbitrationStateMachine.this.sendMessage(104, appInfo);
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "CurrentScene:MSG_GAME_ENTER_PVP_BATTLE");
                } else if (appInfo.mScenceId == HwAPPQoEUtils.GAME_SCENCE_NOT_IN_WAR) {
                    HwArbitrationStateMachine.this.sendMessage(100, appInfo);
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "CurrentScene:MSG_GAME_STATE_START");
                }
            }
        }

        private void handleHiStreamScene() {
            if (HwArbitrationStateMachine.this.mCurrentStreamAppInfo != null) {
                int actions = getActionsConfig(HwArbitrationStateMachine.this.mCurrentStreamAppInfo);
                if (actions < 0) {
                    actions = 1;
                }
                actions = findActionsNeedStart(HwArbitrationStateMachine.this.mCurrentStreamAppInfo, actions);
                String str = HwArbitrationStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Action to start:");
                stringBuilder.append(actions);
                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                if (actions > 0) {
                    HwArbitrationStateMachine.this.mHwHiRadioBoost.startOptimizeActionsForApp(HwArbitrationStateMachine.this.mCurrentStreamAppInfo, actions);
                    HwArbitrationStateMachine.this.setStateMachineHashMap(HwArbitrationStateMachine.this.mCurrentStreamAppInfo, 801, false, false, actions);
                }
            }
        }

        private void stopAllCellOptimize() {
            if (HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.isEmpty()) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "stopAllCellOptimize: no Optimize to recover ");
                return;
            }
            for (Integer appUid : HwArbitrationStateMachine.mHwArbitrationAppBoostMap.keySet()) {
                HwAPPStateInfo tempData = new HwAPPStateInfo();
                tempData.mAppUID = appUid.intValue();
                stopUidOptimiztion(tempData);
            }
            HwArbitrationStateMachine.mHwArbitrationAppBoostMap.clear();
        }

        private void stopUidOptimiztion(HwAPPStateInfo appInfo) {
            if (appInfo != null && HwArbitrationStateMachine.mHwArbitrationAppBoostMap != null && !HwArbitrationStateMachine.mHwArbitrationAppBoostMap.isEmpty()) {
                HwArbitrationAppBoostInfo boostInfo = (HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(appInfo.mAppUID));
                if (boostInfo != null) {
                    String str = HwArbitrationStateMachine.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("stopUidOptimiztion enter: boostInfo is ");
                    stringBuilder.append(boostInfo.toString());
                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    HwAPPStateInfo tempData = new HwAPPStateInfo();
                    tempData.copyObjectValue(appInfo);
                    tempData.mAppId = boostInfo.getAppID();
                    tempData.mAppUID = boostInfo.getBoostUID();
                    tempData.mScenceId = boostInfo.getSceneId();
                    int solution = boostInfo.getSolution();
                    String str2 = HwArbitrationStateMachine.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("stopUidOptimiztion: for ");
                    stringBuilder2.append(tempData.mAppId);
                    stringBuilder2.append(", solution to stop is ");
                    stringBuilder2.append(solution);
                    HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                    HwArbitrationStateMachine.this.mHwHiRadioBoost.stopOptimizedActionsForApp(tempData, false, solution);
                }
            }
        }

        private int findActionsNeedStart(HwAPPStateInfo appInfo, int actions) {
            if (appInfo == null) {
                return -1;
            }
            if (actions <= 0) {
                return actions;
            }
            if (!(HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.isEmpty())) {
                HwArbitrationAppBoostInfo boostInfo = (HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(appInfo.mAppUID));
                if (boostInfo != null) {
                    int lastActions = (~boostInfo.getSolution()) & actions;
                    String str = HwArbitrationStateMachine.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("findActions: actions started is ");
                    stringBuilder.append(boostInfo.getSolution());
                    stringBuilder.append(", actions to judge is ");
                    stringBuilder.append(actions);
                    stringBuilder.append(", actions going to start is ");
                    stringBuilder.append(lastActions);
                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    return lastActions;
                }
            }
            String str2 = HwArbitrationStateMachine.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("findActions: actions to start is ");
            stringBuilder2.append(actions);
            HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
            return actions;
        }

        private int findActionsNeedStop(HwAPPStateInfo appInfo, int configActions) {
            if (configActions <= 0 || appInfo == null) {
                return configActions;
            }
            int actionsStop = -1;
            if (HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.isEmpty()) {
                String str = HwArbitrationStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("findActionsNeedStop: no started actions for uid: ");
                stringBuilder.append(appInfo.mAppUID);
                HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
            } else {
                HwArbitrationAppBoostInfo boostInfo = (HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(appInfo.mAppUID));
                if (boostInfo != null) {
                    actionsStop = configActions & boostInfo.getSolution();
                    String str2 = HwArbitrationStateMachine.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("findActionsNeedStop: actions started is ");
                    stringBuilder2.append(boostInfo.getSolution());
                    stringBuilder2.append(", actions to judge is ");
                    stringBuilder2.append(configActions);
                    stringBuilder2.append(", actions going to remain is ");
                    stringBuilder2.append(actionsStop);
                    HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                    actionsStop = (~actionsStop) & boostInfo.getSolution();
                }
            }
            return actionsStop;
        }

        private int getActionsToStart(HwAPPStateInfo appInfo) {
            return findActionsNeedStart(appInfo, getActionsConfig(appInfo));
        }

        private int getActionsConfig(HwAPPStateInfo appInfo) {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Enter getActionsConfig");
            HwArbitrationStateMachine.this.mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();
            int actions = 0;
            if (!(HwArbitrationStateMachine.this.mHwAPPQoEResourceManger == null || appInfo == null)) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, appInfo.toString());
                actions = HwArbitrationStateMachine.this.mHwAPPQoEResourceManger.getScenceAction(appInfo.mAppType, appInfo.mAppId, appInfo.mScenceId);
                if (actions < 0) {
                    HwArbitrationCommonUtils.logE(HwArbitrationStateMachine.TAG, "get SceneAction from AppQoE xmlConfg failed");
                }
            }
            return actions;
        }

        public void exit() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Exit CellMonitorState");
            stopAllCellOptimize();
            if (HwArbitrationFunction.isPvpScene()) {
                HwArbitrationStateMachine.this.mHwWifiBoost.limitedSpeed(1, 0, 0);
            }
        }

        public boolean processMessage(Message message) {
            String str;
            StringBuilder stringBuilder;
            int newActions;
            String str2;
            StringBuilder stringBuilder2;
            switch (message.what) {
                case 100:
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    str = HwArbitrationStateMachine.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("In CellMonitorState MSG_GAME_STATE_START, mCurrentRAT is ");
                    stringBuilder.append(HwArbitrationFunction.getDataTech());
                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    if (this.mAppInfo != null && HwArbitrationFunction.isDataTechSuitable()) {
                        this.actions = getActionsConfig(this.mAppInfo);
                        str = HwArbitrationStateMachine.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ENTER GAME: getActionConfig,actions = ");
                        stringBuilder.append(this.actions);
                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                        this.actions = findActionsNeedStart(this.mAppInfo, this.actions);
                        HwArbitrationStateMachine.this.mHwHiRadioBoost.startOptimizeActionsForApp(this.mAppInfo, this.actions);
                        HwArbitrationStateMachine.this.setStateMachineHashMap(this.mAppInfo, 801, false, false, this.actions);
                        break;
                    }
                case 101:
                case 103:
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (!(this.mAppInfo == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null)) {
                        stopUidOptimiztion(this.mAppInfo);
                        HwArbitrationStateMachine.mHwArbitrationAppBoostMap.remove(Integer.valueOf(this.mAppInfo.mAppUID));
                    }
                    HwArbitrationStateMachine.this.mHwWifiBoost.limitedSpeed(1, 0, 0);
                    break;
                case 104:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "In CellMonitorState MSG_GAME_ENTER_PVP_BATTLE");
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (this.mAppInfo != null && HwArbitrationFunction.isDataTechSuitable()) {
                        this.actions = getActionsConfig(this.mAppInfo) | 1;
                        str = HwArbitrationStateMachine.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ENTER PVP: getActionConfig:");
                        stringBuilder.append(getActionsConfig(this.mAppInfo));
                        stringBuilder.append(", actions = ");
                        stringBuilder.append(this.actions);
                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                        this.actions = findActionsNeedStart(this.mAppInfo, this.actions);
                        HwArbitrationStateMachine.this.mHwHiRadioBoost.startOptimizeActionsForApp(this.mAppInfo, this.actions);
                        HwArbitrationStateMachine.this.updateStateMachineHashMap(this.mAppInfo, 801, false, false, this.actions);
                        HwArbitrationStateMachine.this.mHwWifiBoost.limitedSpeed(1, 1, 1);
                        break;
                    }
                case 105:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "In CellMonitorState MSG_GAME_EXIT_PVP_BATTLE");
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (!(this.mAppInfo == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.mAppInfo.mAppUID)) == null)) {
                        this.actions = getActionsConfig(this.mAppInfo);
                        str = HwArbitrationStateMachine.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("EXIT PVP:getActionConfig:");
                        stringBuilder.append(this.actions);
                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                        newActions = findActionsNeedStop(this.mAppInfo, this.actions);
                        str2 = HwArbitrationStateMachine.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("findActionsNeedStop:StopAction:");
                        stringBuilder3.append(newActions);
                        HwArbitrationCommonUtils.logD(str2, stringBuilder3.toString());
                        HwArbitrationStateMachine.this.mHwHiRadioBoost.stopOptimizedActionsForApp(this.mAppInfo, true, newActions);
                        HwArbitrationStateMachine.this.setStateMachineHashMap(this.mAppInfo, 801, false, false, this.actions);
                        HwArbitrationStateMachine.this.mHwWifiBoost.limitedSpeed(1, 0, 0);
                        break;
                    }
                case 106:
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (this.mAppInfo != null && HwArbitrationFunction.isDataTechSuitable()) {
                        newActions = getActionsToStart(this.mAppInfo);
                        if (newActions < 0) {
                            newActions = 1;
                        }
                        str2 = HwArbitrationStateMachine.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("histream actions:");
                        stringBuilder2.append(newActions);
                        HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                        HwArbitrationStateMachine.this.mHwHiRadioBoost.startOptimizeActionsForApp(this.mAppInfo, newActions);
                        HwArbitrationStateMachine.this.setStateMachineHashMap(this.mAppInfo, 801, false, false, newActions);
                        HwArbitrationStateMachine.this.printMap(this.mAppInfo.mAppUID);
                        break;
                    }
                case 107:
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (!(this.mAppInfo == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null)) {
                        stopUidOptimiztion(this.mAppInfo);
                        HwArbitrationStateMachine.mHwArbitrationAppBoostMap.remove(Integer.valueOf(this.mAppInfo.mAppUID));
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Stop HiStream actions");
                        break;
                    }
                case 109:
                case HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START /*111*/:
                case 113:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "In CellMonitorState need startOptimizeActionsForApp");
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (this.mAppInfo != null) {
                        this.actions = getActionsToStart(this.mAppInfo);
                        if (this.actions > 0 && HwArbitrationFunction.isDataTechSuitable()) {
                            str = HwArbitrationStateMachine.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("begin HiRadioBoost, UID = ");
                            stringBuilder.append(this.mAppInfo.mAppUID);
                            stringBuilder.append(", actions =");
                            stringBuilder.append(this.actions);
                            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                            HwArbitrationStateMachine.this.mHwHiRadioBoost.startOptimizeActionsForApp(this.mAppInfo, this.actions);
                            HwArbitrationStateMachine.this.setStateMachineHashMap(this.mAppInfo, 801, false, false, this.actions);
                            HwArbitrationStateMachine.this.printMap(this.mAppInfo.mAppUID);
                            break;
                        }
                    }
                    break;
                case 110:
                case HwArbitrationDEFS.MSG_INSTANT_PAY_APP_END /*112*/:
                case 114:
                    str = HwArbitrationStateMachine.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("In CellMonitorState need stopOptimizedActionsForApp , message is ");
                    stringBuilder.append(message.what);
                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (!(this.mAppInfo == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null)) {
                        HwArbitrationAppBoostInfo boostInfo = (HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.mAppInfo.mAppUID));
                        if (boostInfo != null && boostInfo.getSceneId() == HwAPPQoEUtils.SCENE_AUDIO) {
                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "HiStream Audio is still at background");
                            break;
                        }
                        stopUidOptimiztion(this.mAppInfo);
                        HwArbitrationStateMachine.mHwArbitrationAppBoostMap.remove(Integer.valueOf(this.mAppInfo.mAppUID));
                        break;
                    }
                    break;
                case 115:
                case HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD /*1106*/:
                case HwArbitrationDEFS.MSG_STREAMING_AUDIO_BAD /*1108*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "In CellMonitorState process MSG_GAME/APP_STATE_BAD");
                    break;
                case 120:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "in CellMonitor: MSG_STREAMING_APP_FOREGROUND");
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (this.mAppInfo != null && this.mAppInfo.mScenceId == HwAPPQoEUtils.SCENE_VIDEO && HwArbitrationFunction.isDataTechSuitable()) {
                        newActions = getActionsToStart(this.mAppInfo);
                        if (newActions < 0) {
                            newActions = 1;
                        }
                        str2 = HwArbitrationStateMachine.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Start HiStream actions:");
                        stringBuilder2.append(newActions);
                        HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                        HwArbitrationStateMachine.this.mHwHiRadioBoost.startOptimizeActionsForApp(this.mAppInfo, newActions);
                        HwArbitrationStateMachine.this.setStateMachineHashMap(this.mAppInfo, 801, false, false, newActions);
                        break;
                    }
                case 121:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "in CellMonitor: MSG_STREAMING_APP_BACKGROUND");
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    if (!(this.mAppInfo == null || this.mAppInfo.mScenceId != HwAPPQoEUtils.SCENE_VIDEO || HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null)) {
                        stopUidOptimiztion(this.mAppInfo);
                        HwArbitrationStateMachine.mHwArbitrationAppBoostMap.remove(this.mAppInfo);
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Stop HiStream actions");
                        break;
                    }
                case 1005:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "CellMonitorState transitionTo WifiMonitorState");
                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mWifiMonitorState);
                    break;
                case 1009:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "In CellMonitorState don't process MSG_CELL_STATE_CONNECTED");
                    break;
                case 1010:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "CellMonitorState:MSG_CELL_STATE_DISCONNECT");
                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                    break;
                case 1012:
                case 1023:
                    stopAllCellOptimize();
                    if (1012 == message.what && HwArbitrationFunction.isPvpScene()) {
                        HwArbitrationStateMachine.this.mHwWifiBoost.limitedSpeed(1, 0, 0);
                        break;
                    }
                case 1017:
                    HwArbitrationStateMachine hwArbitrationStateMachine = HwArbitrationStateMachine.this;
                    HwAPPQoEManager instance = HwAPPQoEManager.getInstance();
                    hwArbitrationStateMachine.mHwAPPQoEManager = instance;
                    if (instance != null) {
                        HwAPPStateInfo curAPPStateInfo = HwArbitrationStateMachine.this.mHwAPPQoEManager.getCurAPPStateInfo();
                        this.mAppInfo = curAPPStateInfo;
                        if (curAPPStateInfo != null && HwArbitrationFunction.isDataTechSuitable()) {
                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, this.mAppInfo.toString());
                            handleCurrentScene(this.mAppInfo);
                        }
                    }
                    handleHiStreamScene();
                    if (HwArbitrationFunction.isPvpScene()) {
                        HwArbitrationStateMachine.this.mHwWifiBoost.limitedSpeed(1, 1, 1);
                        break;
                    }
                    break;
                case 1022:
                    if (HwArbitrationStateMachine.this.mHwAPPQoEManager != null) {
                        this.mAppInfo = HwArbitrationStateMachine.this.mHwAPPQoEManager.getCurAPPStateInfo();
                        if (this.mAppInfo != null) {
                            str = HwArbitrationStateMachine.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("AppInfo:");
                            stringBuilder.append(this.mAppInfo.toString());
                            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                            this.actions = getActionsToStart(this.mAppInfo);
                            HwArbitrationStateMachine.this.mHwHiRadioBoost.startOptimizeActionsForApp(this.mAppInfo, this.actions);
                            HwArbitrationStateMachine.this.setStateMachineHashMap(this.mAppInfo, 801, false, false, this.actions);
                            str = HwArbitrationStateMachine.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Back to RAT suitable, start actions = ");
                            stringBuilder.append(this.actions);
                            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                            handleHiStreamScene();
                            break;
                        }
                    }
                    break;
                case HwArbitrationDEFS.MSG_CHANNELQOE_NOTIFY_RESULT_GOOD /*1122*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "ChannelQoE Notify Result:MSG_CHANNEL_NOTIFY_RESULT_GOOD");
                    break;
                case HwArbitrationDEFS.MSG_CHANNELQOE_NOTIFY_RESULT_BAD /*1123*/:
                    str = HwArbitrationStateMachine.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ChannelQoE Notify Result:MSG_CHANNEL_NOTIFY_RESULT_BAD, Reason:");
                    stringBuilder.append(message.arg1);
                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    if (HwArbitrationStateMachine.this.isAllowHiCure && -1 != this.appUID) {
                        HwArbitrationStateMachine.this.startHiCure(HwArbitrationStateMachine.this.mContext, this.appUID, 1, 0);
                        break;
                    }
                case HwArbitrationDEFS.MSG_APP_STATE_BAD_NO_RX /*1128*/:
                    this.mAppInfo = (HwAPPStateInfo) message.obj;
                    HwArbitrationCallbackImpl hwArbitrationCallbackImpl = HwArbitrationCallbackImpl.getInstance(HwArbitrationStateMachine.this.mContext);
                    if (this.mAppInfo != null && HwArbitrationStateMachine.this.isAllowHiCure && HwArbitrationStateMachine.this.isAllowQueryChQoE && hwArbitrationCallbackImpl != null) {
                        HwArbitrationStateMachine.this.isAllowQueryChQoE = false;
                        this.appUID = this.mAppInfo.mAppUID;
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Receive MSG_APP_STATE_BAD_NO_RX, start query ChannelQoE");
                        HwChannelQoEManager.createInstance(HwArbitrationStateMachine.this.mContext).queryCellPSAvailable(hwArbitrationCallbackImpl);
                    }
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "In CellMonitorState process MSG_APP_STATE_BAD_NO_RX");
                    break;
                case HwArbitrationDEFS.MSG_AIRPLANE_MODE_ON /*2020*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "In CellMonitorState MSG_AIRPLANE_MODE_ON");
                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Enter DefaultState");
            HwArbitrationStateMachine.this.mHwWifiBoost.initialBGLimitModeRecords();
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "HwWifiBoost:initialBGLimitModeRecords complete");
        }

        public void exit() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "exit DefaultState");
        }

        public boolean processMessage(Message message) {
            String str;
            StringBuilder stringBuilder;
            if (HwArbitrationStateMachine.this.getCurrentState() != null) {
                str = HwArbitrationStateMachine.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DefaultState Msg: ");
                stringBuilder.append(message.what);
                stringBuilder.append(", received in ");
                stringBuilder.append(HwArbitrationStateMachine.this.getCurrentState().getName());
                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            }
            switch (message.what) {
                case 100:
                case 104:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState unhandled MSG_GAME_ENTER_PVP_BATTLE or MSG_GAME_STATE_START");
                    break;
                case 105:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState unhandled MSG_GAME_EXIT_PVP_BATTLE");
                    break;
                case 115:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState unhandled MSG_GAME_WAR_STATE_BAD");
                    break;
                case 1005:
                    HwArbitrationStateMachine.this.deferMessage(message);
                    break;
                case 1006:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState MSG_WIFI_STATE_DISCONNECT");
                    if (!HwArbitrationStateMachine.this.deliverErrorMPLinkCase()) {
                        if (801 != HwArbitrationCommonUtils.getActiveConnectType(HwArbitrationStateMachine.this.mContext) || HwArbitrationStateMachine.this.mCellMonitorState == HwArbitrationStateMachine.this.getCurrentState()) {
                            if (HwArbitrationStateMachine.this.getCurrentState() != HwArbitrationStateMachine.this.mInitialState && 802 == HwArbitrationCommonUtils.getActiveConnectType(HwArbitrationStateMachine.this.mContext)) {
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Not handle:MSG_WIFI_STATE_DISCONNECT in default state");
                                HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                                break;
                            }
                        }
                        HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mCellMonitorState);
                        break;
                    }
                    break;
                case 1010:
                case HwArbitrationDEFS.MSG_CLOSE_4G_OR_WCDMA /*2022*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState: MSG_CELL_STATE_DISCONNECTED");
                    if (!HwArbitrationStateMachine.this.deliverErrorMPLinkCase()) {
                        if (800 != HwArbitrationCommonUtils.getActiveConnectType(HwArbitrationStateMachine.this.mContext)) {
                            if (802 == HwArbitrationCommonUtils.getActiveConnectType(HwArbitrationStateMachine.this.mContext) && HwArbitrationStateMachine.this.getCurrentState() != HwArbitrationStateMachine.this.mInitialState) {
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Not handle:ACTIVE_CONNECT_IS_NONE in default state");
                                HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                                break;
                            }
                        }
                        HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mWifiMonitorState);
                        break;
                    }
                    break;
                case 1012:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_SCREEN_IS_TURNOFF");
                    HwArbitrationStateMachine.this.deliverErrorMPLinkCase();
                    break;
                case 1015:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "update  mDataRoamingState:true");
                    break;
                case 1017:
                    break;
                case 1019:
                    HwArbitrationStateMachine.this.mCurrentServiceState = 0;
                    break;
                case 1020:
                    HwArbitrationStateMachine.this.mCurrentServiceState = 1;
                    break;
                case 1021:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "update  mDataRoamingState:false");
                    break;
                case HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK /*1030*/:
                    HwArbitrationStateMachine.this.mCurrentActiveNetwork = message.arg1;
                    str = HwArbitrationStateMachine.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("update active network: ");
                    stringBuilder.append(HwArbitrationStateMachine.this.mCurrentActiveNetwork);
                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    break;
                case HwArbitrationDEFS.MSG_RECEIVED_HICURE_RESULT /*1119*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "has received HiCure result");
                    break;
                case HwArbitrationDEFS.MSG_ALLOW_HICURE /*1121*/:
                    HwArbitrationStateMachine.this.isAllowHiCure = true;
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "isAllowHicure:true");
                    HwArbitrationStateMachine.this.hiCureToast("isAllowHiCure:True");
                    break;
                case HwArbitrationDEFS.MSG_NOT_RECEIVE_HICURE_RESULT /*1124*/:
                    HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_ALLOW_HICURE, HwArbitrationStateMachine.this.hiCureDefaultOvertime);
                    HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateHiCureResultChr(-1, -1, -1);
                    break;
                case HwArbitrationDEFS.MSG_WIFI_DISCONNECTED_FOR_HICURE /*1125*/:
                    str = HwArbitrationStateMachine.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("receive MSG_WIFI_DISCONNECTED_FOR_HICURE in ");
                    stringBuilder.append(HwArbitrationStateMachine.this.getCurrentState().getName());
                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    if (HwArbitrationCommonUtils.isWifiEnabled(HwArbitrationStateMachine.this.mContext) && HwArbitrationFunction.isScreenOn()) {
                        if (!HwArbitrationFunction.isDataRoaming()) {
                            if (!HwArbitrationCommonUtils.isCellEnable(HwArbitrationStateMachine.this.mContext)) {
                                HwArbitrationStateMachine.this.wifiDisconnectedTriggerHiCure();
                                break;
                            }
                        } else if (!(HwArbitrationCommonUtils.isCellEnable(HwArbitrationStateMachine.this.mContext) && HwArbitrationCommonUtils.isDataRoamingEnable(HwArbitrationStateMachine.this.mContext))) {
                            HwArbitrationStateMachine.this.wifiDisconnectedTriggerHiCure();
                            break;
                        }
                    }
                    break;
                case HwArbitrationDEFS.MSG_DEVICE_BOOT_COMPLETED /*1127*/:
                    HwArbitrationStateMachine.this.mDeviceBootCommpleted = true;
                    break;
                case HwArbitrationDEFS.MSG_APPQOE_REPORT_BAD /*1129*/:
                    if (message.obj != null) {
                        HwArbitrationStateMachine.this.isStallAfterCure = 1;
                        break;
                    }
                    break;
                case HwArbitrationDEFS.MSG_CURED_IS_STALL /*1130*/:
                    HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateIsStallAfterCure(HwArbitrationStateMachine.this.isStallAfterCure);
                    HwArbitrationStateMachine.this.monitorStallAfterCure = false;
                    break;
                case 2001:
                    HwArbitrationCommonUtils.logE(HwArbitrationStateMachine.TAG, "mIsMpLinkError is false");
                    HwArbitrationStateMachine.this.mIsMpLinkError = false;
                    break;
                case HwArbitrationDEFS.MSG_MPLINK_NONCOEX_MODE /*2004*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState start COEX error");
                    HwArbitrationStateMachine.this.deliverErrorMPLinkCase();
                    break;
                case HwArbitrationDEFS.MSG_WIFI_PLUS_ENABLE /*2016*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState MSG_WIFI_PLUS_ENABLE");
                    break;
                case HwArbitrationDEFS.MSG_WIFI_PLUS_DISABLE /*2018*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState MSG_WIFI_PLUS_DISABLE");
                    HwArbitrationStateMachine.this.deliverErrorMPLinkCase();
                    break;
                case HwArbitrationDEFS.MSG_AIRPLANE_MODE_ON /*2020*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "DefaultState MSG_AIRPLANE_MODE_ON ");
                    if (!HwArbitrationStateMachine.this.deliverErrorMPLinkCase()) {
                        switch (HwArbitrationCommonUtils.getActiveConnectType(HwArbitrationStateMachine.this.mContext)) {
                            case 800:
                                if (HwArbitrationStateMachine.this.mWifiMonitorState != HwArbitrationStateMachine.this.getCurrentState()) {
                                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mWifiMonitorState);
                                    break;
                                }
                                break;
                            case 801:
                                if (HwArbitrationStateMachine.this.mCellMonitorState != HwArbitrationStateMachine.this.getCurrentState()) {
                                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mCellMonitorState);
                                    break;
                                }
                                break;
                            default:
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "cannot distinguish state, Enter InitialState");
                                if (HwArbitrationStateMachine.this.mInitialState != HwArbitrationStateMachine.this.getCurrentState()) {
                                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                                    break;
                                }
                                break;
                        }
                    }
                    break;
                case HwArbitrationDEFS.MSG_MPLINK_AI_DEVICE_COEX_MODE /*2023*/:
                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStartedState);
                    break;
                case HwArbitrationDEFS.MSG_VPN_STATE_OPEN /*2024*/:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_VPN_STATE_OPEN");
                    HwArbitrationStateMachine.this.deliverErrorMPLinkCase();
                    break;
                case HwArbitrationDEFS.MSG_HISTREAM_TRIGGER_MPPLINK_INTERNAL /*2026*/:
                    HwArbitrationStateMachine.this.mHiStreamTriggerOrStopMplink = true;
                    break;
                case HwArbitrationDEFS.MSG_Stop_MPLink_By_Notification /*2032*/:
                    HwArbitrationCommonUtils.logE(HwArbitrationStateMachine.TAG, "MSG_Stop_MPLink_By_Notification");
                    break;
                case HwArbitrationDEFS.MSG_Recovery_Flag_By_Notification /*2034*/:
                    HwArbitrationCommonUtils.logE(HwArbitrationStateMachine.TAG, "MSG_Recovery_Flag_By_Notification");
                    HwArbitrationStateMachine.this.mDenyByNotification = false;
                    break;
                default:
                    if (HwArbitrationStateMachine.this.getCurrentState() != null) {
                        str = HwArbitrationStateMachine.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unhandled Message: ");
                        stringBuilder.append(message.what);
                        stringBuilder.append(" in State: ");
                        stringBuilder.append(HwArbitrationStateMachine.this.getCurrentState().getName());
                        HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
                        break;
                    }
                    break;
            }
            return true;
        }
    }

    class InitialState extends State {
        InitialState() {
        }

        public void enter() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Enter InitialState");
            if (HwArbitrationStateMachine.mHwArbitrationAppBoostMap != null) {
                HwArbitrationStateMachine.mHwArbitrationAppBoostMap.clear();
            }
        }

        public void exit() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Exit InitialState");
            if (HwArbitrationStateMachine.this.hasMessages(HwArbitrationDEFS.MSG_TRANSITION_TO_HICURE)) {
                HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_TRANSITION_TO_HICURE);
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_TRANSITION_TO_HICURE moved in InitialState");
            }
        }

        public boolean processMessage(Message message) {
            String str = HwArbitrationStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("InitialState Msg: ");
            stringBuilder.append(message.what);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            int i = message.what;
            if (i == 1005) {
                HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mWifiMonitorState);
            } else if (i != 1009) {
                if (i != 1012) {
                    if (i == HwArbitrationDEFS.MSG_TRANSITION_TO_HICURE) {
                        i = message.arg1;
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "has received MSG_TRANSITION_TO_HICURE in InitialState");
                        if (HwArbitrationStateMachine.this.isAllowHiCure && 802 == HwArbitrationCommonUtils.getActiveConnectType(HwArbitrationStateMachine.this.mContext)) {
                            HwArbitrationStateMachine.this.startHiCure(HwArbitrationStateMachine.this.mContext, i, 1, -1);
                        } else {
                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "not allow HiCure");
                        }
                    } else if (i == HwArbitrationDEFS.MSG_MPLINK_STOP_COEX_SUCC || i == HwArbitrationDEFS.MSG_MPLINK_ERROR_HANDLER) {
                        if (HwArbitrationDEFS.MSG_MPLINK_ERROR_HANDLER == message.what) {
                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_ERROR_HANDLER");
                        } else {
                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_STOP_COEX_SUCC");
                        }
                        switch (HwArbitrationCommonUtils.getActiveConnectType(HwArbitrationStateMachine.this.mContext)) {
                            case 800:
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "InitialState WIFI_NETWORK");
                                HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mWifiMonitorState);
                                break;
                            case 801:
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "InitialState CELL_NETWORK");
                                HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mCellMonitorState);
                                break;
                            default:
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "cannot distinguish state");
                                break;
                        }
                    } else {
                        switch (i) {
                            case HwArbitrationDEFS.MSG_APP_STATE_FOREGROUND /*1117*/:
                                HwAPPStateInfo appInfo = message.obj;
                                if (appInfo != null && HwArbitrationStateMachine.this.mCurrentServiceState == 0) {
                                    HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_TRANSITION_TO_HICURE, appInfo.mAppUID, 2000);
                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "send DelayMessage MSG_TRANSITION_TO_HICURE when APP_STATE_FOREFROUND");
                                    break;
                                }
                            case HwArbitrationDEFS.MSG_APP_STATE_BACKGROUND /*1118*/:
                                break;
                            default:
                                str = HwArbitrationStateMachine.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Message:");
                                stringBuilder.append(message.what);
                                stringBuilder.append(" did't process in InitialState, go to parent state");
                                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                return false;
                        }
                    }
                }
                if (HwArbitrationStateMachine.this.hasMessages(HwArbitrationDEFS.MSG_TRANSITION_TO_HICURE)) {
                    HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_TRANSITION_TO_HICURE);
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "has removed MSG_TRANSITION_TO_HICURE in InitialState");
                }
            } else {
                HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mCellMonitorState);
            }
            return true;
        }
    }

    class MPLinkStartedState extends State {
        private static final int InstantAPP = 2;
        private static final int StreamingAPP = 3;
        private HwAPPStateInfo appInfo;
        private MplinkBindResultInfo mplinkBindResultInfo;
        private int mplinkErrorCode = -1;
        private long pingPongTMWiFi_Good = 0;
        private int punishWiFiGoodCount = 0;
        private int stopMplinkReason = -1;
        private boolean trgPingPongWiFi_Good;
        private int uid;
        private boolean wifiGoodFlag = true;

        MPLinkStartedState() {
        }

        public void enter() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Enter MPLinkStartedState");
            this.trgPingPongWiFi_Good = false;
        }

        public void exit() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Exit MPLinkStartedState");
            HwArbitrationStateMachine.this.mIsMpLinkBinding = false;
            HwArbitrationStateMachine.this.mCoexCount = 0;
            HwArbitrationStateMachine.this.mMpLinkCount = 0;
            HwArbitrationStateMachine.mHwArbitrationAppBoostMap = null;
            HwArbitrationStateMachine.this.mIsMpLinkError = false;
            this.wifiGoodFlag = true;
            this.stopMplinkReason = -1;
            this.mplinkErrorCode = -1;
            HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_SET_PingPong_WiFi_Good_FALSE);
            HwArbitrationDisplay.getInstance(HwArbitrationStateMachine.this.mContext).requestDataMonitor(false, 1);
            if (HwAPPQoEManager.getInstance() != null) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "stopWifiLinkMonitor all");
                HwAPPQoEManager.getInstance().stopWifiLinkMonitor(-1, true);
            }
        }

        public boolean processMessage(Message message) {
            Message message2 = message;
            int i = message2.what;
            switch (i) {
                case 100:
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_GAME_STATE_START");
                    HwArbitrationStateMachine.this.printMap(this.uid);
                    this.appInfo = (HwAPPStateInfo) message2.obj;
                    HwArbitrationStateMachine.this.handleGeneralGameStart(this.appInfo);
                    break;
                case 101:
                    this.appInfo = (HwAPPStateInfo) message2.obj;
                    if (this.appInfo != null) {
                        HwArbitrationStateMachine.this.printMap(this.appInfo.mAppUID);
                        HwArbitrationStateMachine.this.handleGeneralGameEnd(this.appInfo);
                        HwArbitrationStateMachine.this.handleGamePvpEnd(this.appInfo);
                        break;
                    }
                    break;
                default:
                    int i2 = 0;
                    switch (i) {
                        case 103:
                            HwArbitrationStateMachine.this.printMap(this.uid);
                            this.appInfo = (HwAPPStateInfo) message2.obj;
                            if (this.appInfo != null) {
                                HwArbitrationStateMachine.this.handleGeneralGameEnd(this.appInfo);
                                break;
                            }
                            break;
                        case 104:
                            this.appInfo = (HwAPPStateInfo) message2.obj;
                            if (this.appInfo != null) {
                                HwArbitrationStateMachine.this.handleGeneralGameStart(this.appInfo);
                                HwArbitrationStateMachine.this.handleGamePvpStart(this.appInfo);
                                updateMapAppInfo(this.appInfo, false);
                                HwArbitrationStateMachine.this.printMap(this.appInfo.mAppUID);
                                break;
                            }
                            break;
                        case 105:
                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_GAME_EXIT_PVP_BATTLE");
                            this.appInfo = (HwAPPStateInfo) message2.obj;
                            if (this.appInfo != null) {
                                HwArbitrationStateMachine.this.handleGamePvpEnd(this.appInfo);
                                break;
                            }
                            break;
                        case 106:
                            this.appInfo = (HwAPPStateInfo) message2.obj;
                            HwArbitrationStateMachine.this.handleStreamingStart(this.appInfo);
                            break;
                        case 107:
                            this.appInfo = (HwAPPStateInfo) message2.obj;
                            HwArbitrationStateMachine.this.handleStreamingEnd(this.appInfo);
                            if (HwArbitrationStateMachine.this.isInMPLink(this.appInfo.mAppUID)) {
                                this.stopMplinkReason = 6;
                                stopMPLinkAppBind(this.appInfo.mAppUID);
                                break;
                            }
                            break;
                        default:
                            switch (i) {
                                case 109:
                                case HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START /*111*/:
                                case 113:
                                    this.appInfo = (HwAPPStateInfo) message2.obj;
                                    if (!(this.appInfo == null || (HwArbitrationStateMachine.this.isInMPLink(this.appInfo.mAppUID) && HwArbitrationStateMachine.this.isStreamScene(this.appInfo.mAppUID)))) {
                                        updateMapAppInfo(this.appInfo, true);
                                        break;
                                    }
                                case 110:
                                    this.appInfo = (HwAPPStateInfo) message2.obj;
                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_INSTANT_APP_END");
                                    if (this.appInfo != null) {
                                        if (!HwArbitrationStateMachine.this.isInMPLink(this.appInfo.mAppUID) || HwArbitrationStateMachine.this.isStreamScene(this.appInfo.mAppUID)) {
                                            if (!HwArbitrationStateMachine.this.isInMPLink(this.appInfo.mAppUID)) {
                                                stopMPLinkCoex(this.appInfo.mAppUID);
                                                break;
                                            }
                                        }
                                        this.stopMplinkReason = 6;
                                        stopMPLinkAppBind(this.appInfo.mAppUID);
                                        break;
                                    }
                                    break;
                                case HwArbitrationDEFS.MSG_INSTANT_PAY_APP_END /*112*/:
                                case 114:
                                    this.appInfo = (HwAPPStateInfo) message2.obj;
                                    if (!(this.appInfo == null || HwArbitrationStateMachine.this.isInMPLink(this.appInfo.mAppUID))) {
                                        stopMPLinkCoex(this.appInfo.mAppUID);
                                        break;
                                    }
                                case 115:
                                    this.appInfo = (HwAPPStateInfo) message2.obj;
                                    handleAppExpBad(this.appInfo, 2);
                                    break;
                                default:
                                    String str;
                                    StringBuilder stringBuilder;
                                    StringBuilder stringBuilder2;
                                    switch (i) {
                                        case HwArbitrationDEFS.MSG_MPLINK_BIND_CHECK_OK_NOTIFY /*2005*/:
                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_BIND_CHECK_OK_NOTIFY");
                                            HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_QUERY_QOE_WM_TIMEOUT);
                                            this.uid = message2.arg1;
                                            if (!HwArbitrationStateMachine.this.isInMPLink(this.uid)) {
                                                startMPLinkAppBind(this.uid);
                                                break;
                                            }
                                            this.stopMplinkReason = 3;
                                            stopMPLinkAppBind(this.uid);
                                            break;
                                        case HwArbitrationDEFS.MSG_MPLINK_BIND_CHECK_FAIL_NOTIFY /*2006*/:
                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_BIND_CHECK_FAIL_NOTIFY");
                                            HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_QUERY_QOE_WM_TIMEOUT);
                                            this.uid = message2.arg1;
                                            HwArbitrationStateMachine.this.setQueryTime(this.uid);
                                            if (HwArbitrationStateMachine.this.noAPPInMPLink()) {
                                                stopMPLinkCoex(this.uid);
                                            }
                                            HwArbitrationStateMachine.this.mIsMpLinkBinding = false;
                                            HwArbitrationStateMachine.this.updateMplinkCHRExceptionEvent(message2.arg1, 8, message2.arg2);
                                            HwAPPChrManager.getInstance().updateStatisInfo(this.appInfo, 11);
                                            break;
                                        case HwArbitrationDEFS.MSG_MPLINK_BIND_SUCCESS /*2007*/:
                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_BIND_SUCCESS");
                                            this.mplinkBindResultInfo = (MplinkBindResultInfo) message2.obj;
                                            HwArbitrationStateMachine.this.mIsMpLinkBinding = false;
                                            if (this.mplinkBindResultInfo != null) {
                                                this.uid = this.mplinkBindResultInfo.getUid();
                                                if (!(HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid)) == null)) {
                                                    i = ((HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid))).getAppID();
                                                    if (!(1004 == i || 1002 == i)) {
                                                        HwArbitrationDisplay.setToast(HwArbitrationStateMachine.this.mContext, HwArbitrationStateMachine.this.mContext.getString(33686081));
                                                    }
                                                }
                                                HwArbitrationStateMachine.this.printMap(this.uid);
                                                if (HwArbitrationStateMachine.this.isStreamScene(this.uid)) {
                                                    HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_HISTREAM_TRIGGER_MPPLINK_INTERNAL);
                                                    HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_HISTREAM_TRIGGER_MPPLINK_INTERNAL, HwArbitrationStateMachine.this.triggerMPlinkInternal);
                                                    HwArbitrationStateMachine.this.mHiStreamTriggerOrStopMplink = false;
                                                }
                                                if (!(HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid)) == null)) {
                                                    if (((HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid))).getIsMPlink()) {
                                                        str = HwArbitrationStateMachine.TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("MSG_MPLINK_BIND_SUCCESS, ");
                                                        stringBuilder.append(this.uid);
                                                        stringBuilder.append("is already in mMpLink!");
                                                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                    } else {
                                                        HwArbitrationStateMachine.this.mMpLinkCount = HwArbitrationStateMachine.this.mMpLinkCount + 1;
                                                        str = HwArbitrationStateMachine.TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("MSG_MPLINK_BIND_SUCCESS, mMpLinkCount is ");
                                                        stringBuilder.append(HwArbitrationStateMachine.this.mMpLinkCount);
                                                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                    }
                                                }
                                                if (System.currentTimeMillis() - this.pingPongTMWiFi_Good < HwArbitrationDEFS.DelayTimeMillisA) {
                                                    this.punishWiFiGoodCount++;
                                                    str = HwArbitrationStateMachine.TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("MSG_MPLINK_BIND_SUCCESS, punishWiFiGoodCount: ");
                                                    stringBuilder.append(this.punishWiFiGoodCount);
                                                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                    if (1 <= this.punishWiFiGoodCount) {
                                                        this.trgPingPongWiFi_Good = true;
                                                        HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_SET_PingPong_WiFi_Good_FALSE, this.uid, HwArbitrationDEFS.DelayTimeMillisB);
                                                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_BIND_SUCCESS, trigger the Ping-Pong ================================");
                                                        this.punishWiFiGoodCount = 0;
                                                    }
                                                } else {
                                                    this.punishWiFiGoodCount = 0;
                                                }
                                                HwArbitrationStateMachine.this.pingPongTMCell_Bad = System.currentTimeMillis();
                                                if (!(HwAPPQoEManager.getInstance() == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid)) == null || this.trgPingPongWiFi_Good)) {
                                                    str = HwArbitrationStateMachine.TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("MSG_MPLINK_BIND_SUCCESS, startWifiLinkMonitor: ");
                                                    stringBuilder.append(this.uid);
                                                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                    HwAPPQoEManager.getInstance().startWifiLinkMonitor(this.uid, ((HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid))).getSceneId());
                                                }
                                                if (HwArbitrationStateMachine.this.updateStateInfoMap(this.mplinkBindResultInfo, HwArbitrationFunction.getNetwork(HwArbitrationStateMachine.this.mContext, this.mplinkBindResultInfo.getNetwork()), true)) {
                                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "start MPLink success and update table success");
                                                    HwArbitrationStateMachine.this.sendMPLinkBroadcast(this.uid);
                                                    HwArbitrationStateMachine.this.printMap(this.uid);
                                                }
                                                if (801 == HwArbitrationStateMachine.this.getCurrentNetwork(HwArbitrationStateMachine.this.mContext, this.uid) && HwArbitrationStateMachine.this.isInMPLink(this.uid)) {
                                                    HwArbitrationStateMachine.this.mHwWifiBoost.stopGameBoost(this.uid);
                                                    HwArbitrationStateMachine.this.mHwWifiBoost.stopStreamingBoost(this.uid);
                                                }
                                                HwArbitrationDisplay.getInstance(HwArbitrationStateMachine.this.mContext).requestDataMonitor(true, 1);
                                                HwArbitrationStateMachine.this.updateMplinkCHRExceptionEvent(this.uid, 5, 0);
                                                break;
                                            }
                                            break;
                                        case HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL /*2008*/:
                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_BIND_FAIL");
                                            this.mplinkBindResultInfo = (MplinkBindResultInfo) message2.obj;
                                            HwArbitrationStateMachine.this.mIsMpLinkBinding = false;
                                            if (this.mplinkBindResultInfo != null) {
                                                this.uid = this.mplinkBindResultInfo.getUid();
                                                HwArbitrationStateMachine.this.printMap(this.uid);
                                                HwArbitrationStateMachine.this.setQueryTime(this.uid);
                                                HwArbitrationStateMachine.this.updateMplinkCHRExceptionEvent(this.uid, 5, 8);
                                            }
                                            HwAPPChrManager.getInstance().updateStatisInfo(this.appInfo, 12);
                                            break;
                                        case HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS /*2009*/:
                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_UNBIND_SUCCESS");
                                            this.mplinkBindResultInfo = (MplinkBindResultInfo) message2.obj;
                                            this.wifiGoodFlag = true;
                                            if (this.mplinkBindResultInfo != null) {
                                                this.uid = this.mplinkBindResultInfo.getUid();
                                                if (HwArbitrationStateMachine.this.isStreamScene(this.uid)) {
                                                    HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_HISTREAM_TRIGGER_MPPLINK_INTERNAL);
                                                    HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_HISTREAM_TRIGGER_MPPLINK_INTERNAL, HwArbitrationStateMachine.this.triggerMPlinkInternal);
                                                    HwArbitrationStateMachine.this.mHiStreamTriggerOrStopMplink = false;
                                                }
                                                HwArbitrationStateMachine.this.printMap(this.uid);
                                                HwArbitrationStateMachine.this.mMpLinkCount = HwArbitrationStateMachine.this.mMpLinkCount - 1;
                                                str = HwArbitrationStateMachine.TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("MSG_MPLINK_UNBIND_SUCCESS, mMpLinkCount is ");
                                                stringBuilder.append(HwArbitrationStateMachine.this.mMpLinkCount);
                                                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                i = getTargetNetwork(this.mplinkBindResultInfo.getNetwork(), false);
                                                if (HwArbitrationStateMachine.this.mIsMpLinkError) {
                                                    i = HwArbitrationCommonUtils.getActiveConnectType(HwArbitrationStateMachine.this.mContext);
                                                }
                                                String str2 = HwArbitrationStateMachine.TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("MSG_MPLINK_UNBIND_SUCCESS, network is ");
                                                stringBuilder2.append(i);
                                                HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                                                if (HwArbitrationStateMachine.this.updateStateInfoMap(this.mplinkBindResultInfo, i, false)) {
                                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "stop MPLink success and update table success");
                                                }
                                                HwArbitrationStateMachine.this.sendMPLinkBroadcast(this.uid);
                                                HwArbitrationStateMachine.this.updateMplinkCHRExceptionEvent(this.uid, this.stopMplinkReason, 0);
                                                if (!(HwAPPQoEManager.getInstance() == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid)) == null)) {
                                                    str2 = HwArbitrationStateMachine.TAG;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("MSG_MPLINK_UNBIND_SUCCESS, stopWifiLinkMonitor ");
                                                    stringBuilder2.append(this.uid);
                                                    HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                                                    HwAPPQoEManager.getInstance().stopWifiLinkMonitor(this.uid, false);
                                                }
                                                stopMPLinkCoex(this.uid);
                                                break;
                                            }
                                            break;
                                        case HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL /*2010*/:
                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_UNBIND_FAIL");
                                            this.wifiGoodFlag = true;
                                            this.mplinkBindResultInfo = (MplinkBindResultInfo) message2.obj;
                                            if (this.mplinkBindResultInfo != null) {
                                                HwArbitrationStateMachine.this.setQueryTime(this.mplinkBindResultInfo.getUid());
                                                HwArbitrationStateMachine.this.updateMplinkCHRExceptionEvent(this.mplinkBindResultInfo.getUid(), this.stopMplinkReason, 9);
                                                break;
                                            }
                                            break;
                                        default:
                                            switch (i) {
                                                case HwArbitrationDEFS.MSG_QUERY_QOE_WM_TIMEOUT /*2029*/:
                                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "check ChannelQoE and Wavemapping timeout");
                                                    if (HwArbitrationStateMachine.this.noAPPInMPLink()) {
                                                        stopMPLinkCoex(message2.arg1);
                                                    }
                                                    HwArbitrationStateMachine.this.updateMplinkCHRExceptionEvent(message2.arg1, 8, 7);
                                                    break;
                                                case HwArbitrationDEFS.MSG_APPQoE_WIFI_GOOD /*2030*/:
                                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_APPQoE_WIFI_GOOD");
                                                    if (!HwArbitrationStateMachine.this.noAPPInMPLink()) {
                                                        if (!this.wifiGoodFlag) {
                                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "processing MSG_APPQoE_WIFI_GOOD");
                                                            break;
                                                        }
                                                        this.uid = message2.arg1;
                                                        str = HwArbitrationStateMachine.TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("trgPingPongWiFi_Good: ");
                                                        stringBuilder.append(this.trgPingPongWiFi_Good);
                                                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                        if (HwArbitrationStateMachine.mHwArbitrationAppBoostMap != null && HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid)) != null && HwArbitrationStateMachine.this.isInMPLink(this.uid) && !this.trgPingPongWiFi_Good) {
                                                            this.wifiGoodFlag = false;
                                                            HwArbitrationStateMachine.this.printMap(this.uid);
                                                            this.stopMplinkReason = 4;
                                                            this.pingPongTMWiFi_Good = System.currentTimeMillis();
                                                            str = HwArbitrationStateMachine.TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("MSG_APPQoE_WIFI_GOOD time: ");
                                                            stringBuilder.append(this.pingPongTMWiFi_Good);
                                                            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                            stopMPLinkAppBind(this.uid);
                                                            break;
                                                        }
                                                        str = HwArbitrationStateMachine.TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("MSG_APPQoE_WIFI_GOOD not allow: ");
                                                        stringBuilder.append(this.trgPingPongWiFi_Good);
                                                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                        break;
                                                    }
                                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "there's no app in MPLink");
                                                    break;
                                                    break;
                                                case HwArbitrationDEFS.MSG_SET_PingPong_WiFi_Good_FALSE /*2031*/:
                                                    if (this.trgPingPongWiFi_Good) {
                                                        HwArbitrationCommonUtils.logE(HwArbitrationStateMachine.TAG, "MSG_SET_PingPong_WiFi_Good_FALSE");
                                                        this.trgPingPongWiFi_Good = false;
                                                        this.uid = message2.arg1;
                                                        if (!(HwAPPQoEManager.getInstance() == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid)) == null || !HwArbitrationStateMachine.this.isInMPLink(this.uid) || ((HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid))).getNetwork() != 801)) {
                                                            str = HwArbitrationStateMachine.TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("MSG_SET_PingPong_WiFi_Good_FALSE, startWifiLinkMonitor: ");
                                                            stringBuilder.append(this.uid);
                                                            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                            HwAPPQoEManager.getInstance().startWifiLinkMonitor(this.uid, ((HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(this.uid))).getSceneId());
                                                            break;
                                                        }
                                                    }
                                                    break;
                                                case HwArbitrationDEFS.MSG_Stop_MPLink_By_Notification /*2032*/:
                                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_Stop_MPLink_By_Notification");
                                                    HwArbitrationStateMachine.this.mDenyByNotification = true;
                                                    HwArbitrationStateMachine.this.sendMessage(HwArbitrationDEFS.MSG_MPLINK_ERROR);
                                                    this.mplinkErrorCode = 0;
                                                    HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_Recovery_Flag_By_Notification, 86400000);
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case 8:
                                                            i = message2.arg1;
                                                            i2 = SubscriptionManager.getDefaultSubId();
                                                            if (HwArbitrationCommonUtils.isSlotIdValid(i) && HwArbitrationCommonUtils.isSlotIdValid(i2) && i != i2) {
                                                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Vice SIM  is starting Calling");
                                                                HwArbitrationStateMachine.this.sendMessage(HwArbitrationDEFS.MSG_MPLINK_ERROR);
                                                                break;
                                                            }
                                                        case 1005:
                                                            break;
                                                        case 1009:
                                                        case HwArbitrationDEFS.MSG_MPLINK_ERROR /*2019*/:
                                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_ERROR");
                                                            HwArbitrationStateMachine.this.mIsMpLinkBinding = true;
                                                            HwArbitrationStateMachine.this.mCoexCount = 0;
                                                            List<Integer> uidList = HwArbitrationStateMachine.this.getAppUIDInMPLink();
                                                            if (uidList != null && uidList.size() != 0) {
                                                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_ERROR: app in MPLink");
                                                                if (this.mplinkErrorCode == 0) {
                                                                    this.stopMplinkReason = 9;
                                                                    this.mplinkErrorCode = -1;
                                                                } else {
                                                                    this.stopMplinkReason = 7;
                                                                }
                                                                int NList = uidList.size();
                                                                while (i2 < NList) {
                                                                    stopMPLinkAppBind(((Integer) uidList.get(i2)).intValue());
                                                                    i2++;
                                                                }
                                                                break;
                                                            }
                                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_ERROR: no app in MPLink");
                                                            HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStoppingState);
                                                            break;
                                                        case 1012:
                                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_SCREEN_IS_TURNOFF");
                                                            if (!(HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.size() == 0)) {
                                                                for (HwArbitrationAppBoostInfo boostInfo : HwArbitrationStateMachine.mHwArbitrationAppBoostMap.values()) {
                                                                    if (boostInfo != null && boostInfo.getIsMPlink()) {
                                                                        String str3;
                                                                        if (HwAPPQoEUtils.SCENE_AUDIO != boostInfo.getSceneId()) {
                                                                            str3 = HwArbitrationStateMachine.TAG;
                                                                            stringBuilder2 = new StringBuilder();
                                                                            stringBuilder2.append("stop MpLinkBind, uid:");
                                                                            stringBuilder2.append(boostInfo.getBoostUID());
                                                                            HwArbitrationCommonUtils.logD(str3, stringBuilder2.toString());
                                                                            stopMPLinkAppBind(boostInfo.getBoostUID());
                                                                        } else {
                                                                            str3 = HwArbitrationStateMachine.TAG;
                                                                            stringBuilder2 = new StringBuilder();
                                                                            stringBuilder2.append("HiStream Audio : not stop MpLinkBind, uid:");
                                                                            stringBuilder2.append(boostInfo.getBoostUID());
                                                                            HwArbitrationCommonUtils.logD(str3, stringBuilder2.toString());
                                                                        }
                                                                    }
                                                                }
                                                                break;
                                                            }
                                                        case HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD /*1106*/:
                                                        case HwArbitrationDEFS.MSG_STREAMING_AUDIO_BAD /*1108*/:
                                                            this.appInfo = (HwAPPStateInfo) message2.obj;
                                                            if (this.appInfo != null) {
                                                                if (!HwArbitrationStateMachine.this.isInMPLink(this.appInfo.mAppUID)) {
                                                                    if (HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD == message2.what && !HwArbitrationFunction.isInLTE(HwArbitrationStateMachine.this.mContext)) {
                                                                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Phone is not in LTE");
                                                                        HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr((HwAPPStateInfo) message2.obj, 1);
                                                                        break;
                                                                    }
                                                                    handleAppExpBad(this.appInfo, 3);
                                                                    break;
                                                                }
                                                                str = HwArbitrationStateMachine.TAG;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("isStreamScene:");
                                                                stringBuilder.append(HwArbitrationStateMachine.this.isStreamScene(this.appInfo.mAppUID));
                                                                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                                                                handleAppExpBad(this.appInfo, 3);
                                                                break;
                                                            }
                                                            break;
                                                        case HwArbitrationDEFS.MSG_QUERY_QOE_WM_INFO /*2013*/:
                                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_QUERY_QOE_WM_INFO");
                                                            this.uid = message2.arg1;
                                                            if (HwArbitrationStateMachine.this.isInMPLink(this.uid) && System.currentTimeMillis() - HwArbitrationStateMachine.this.pingPongTMCell_Bad < HwArbitrationDEFS.DelayTimeMillisA) {
                                                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "trigger ping pong in wifi monitor");
                                                                HwArbitrationStateMachine.this.trgPingPongCell_Bad = true;
                                                                HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_SET_PingPong_Cell_Bad_FALSE, HwArbitrationDEFS.DelayTimeMillisB);
                                                            }
                                                            startMPLinkBindCheck(this.uid);
                                                            break;
                                                        default:
                                                            return false;
                                                    }
                                                    break;
                                            }
                                    }
                            }
                            break;
                    }
            }
            return true;
        }

        private void handleAppExpBad(HwAPPStateInfo appInfo, int type) {
            if (appInfo != null) {
                updateMapAppInfo(appInfo, true);
                HwArbitrationStateMachine.this.printMap(appInfo.mAppUID);
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, appInfo.toString());
                if (!HwArbitrationStateMachine.this.mIsMpLinkBinding) {
                    switch (type) {
                        case 2:
                            HwArbitrationStateMachine.this.sendMessage(HwArbitrationDEFS.MSG_QUERY_QOE_WM_INFO, appInfo.mAppUID);
                            break;
                        case 3:
                            if (HwArbitrationFunction.isStreamingScene(appInfo)) {
                                HwArbitrationStateMachine.this.sendMessage(HwArbitrationDEFS.MSG_QUERY_QOE_WM_INFO, appInfo.mAppUID);
                                break;
                            }
                            break;
                    }
                }
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MPLinkStarted is querying");
                HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr(appInfo, 6);
            }
        }

        private void updateMapAppInfo(HwAPPStateInfo appInfo, boolean needCoexAdd) {
            if (appInfo != null) {
                if (HwArbitrationStateMachine.mHwArbitrationAppBoostMap != null && HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(appInfo.mAppUID)) != null) {
                    HwArbitrationAppBoostInfo mHwAAInfo = (HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(appInfo.mAppUID));
                    if (!mHwAAInfo.getIsCoex() && needCoexAdd) {
                        mHwAAInfo.setIsCoex(true);
                        HwArbitrationStateMachine.this.mCoexCount = HwArbitrationStateMachine.this.mCoexCount + 1;
                        String str = HwArbitrationStateMachine.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("updateMapAppInfo, mCoexCount is: ");
                        stringBuilder.append(HwArbitrationStateMachine.this.mCoexCount);
                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    }
                    HwArbitrationStateMachine.this.setStateMachineHashMap(appInfo, mHwAAInfo.getNetwork(), mHwAAInfo.getIsCoex(), mHwAAInfo.getIsMPlink(), mHwAAInfo.getSolution());
                } else if (HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(appInfo.mAppUID)) == null) {
                    HwArbitrationStateMachine.this.setStateMachineHashMap(appInfo, HwArbitrationStateMachine.this.getCurrentNetwork(HwArbitrationStateMachine.this.mContext, appInfo.mAppUID), true, false, 0);
                    HwArbitrationStateMachine.this.mCoexCount = HwArbitrationStateMachine.this.mCoexCount + 1;
                    String str2 = HwArbitrationStateMachine.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateMapAppInfo, mCoexCount is: ");
                    stringBuilder2.append(HwArbitrationStateMachine.this.mCoexCount);
                    HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                }
            }
        }

        private void startMPLinkBindCheck(int uid) {
            String str = HwArbitrationStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startMPLinkBindCheck,uid = ");
            stringBuilder.append(uid);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            HwArbitrationStateMachine.this.printMap(uid);
            if (HwArbitrationStateMachine.mHwArbitrationAppBoostMap != null && HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(uid)) != null) {
                HwArbitrationAppBoostInfo myAABInfo = (HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(uid));
                if (!HwArbitrationStateMachine.this.isNeedChQoEquery(uid)) {
                    HwArbitrationStateMachine.this.updateMplinkCHRExceptionEvent(uid, 8, 3);
                } else if (HwAPPQoEManager.createHwAPPQoEManager(HwArbitrationStateMachine.this.mContext) != null && HwWaveMappingManager.getInstance(HwArbitrationStateMachine.this.mContext) != null) {
                    HwArbitrationStateMachine.this.mIsMpLinkBinding = true;
                    HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_QUERY_QOE_WM_TIMEOUT, uid, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
                    HwAPPQoEManager.createHwAPPQoEManager(HwArbitrationStateMachine.this.mContext).queryNetworkQuality(myAABInfo.mUID, myAABInfo.mSceneId, getTargetNetwork(myAABInfo.mNetwork, true), true);
                    HwWaveMappingManager.getInstance(HwArbitrationStateMachine.this.mContext).queryWaveMappingInfo(myAABInfo.mUID, myAABInfo.mAppID, myAABInfo.mSceneId, getTargetNetwork(myAABInfo.mNetwork, true));
                }
            }
        }

        private void startMPLinkAppBind(int uid) {
            String str = HwArbitrationStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startMPLinkAppBind:");
            stringBuilder.append(uid);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            HwArbitrationStateMachine.this.printMap(uid);
            HwMplinkManager.createInstance(HwArbitrationStateMachine.this.mContext).requestBindProcessToNetwork(HwArbitrationStateMachine.this.getTargetNetID(HwArbitrationStateMachine.this.getCurrentNetwork(HwArbitrationStateMachine.this.mContext, uid)), uid, HwArbitrationStateMachine.this.getMpLinkQuickSwitchConfiguration(uid));
        }

        private void stopMPLinkAppBind(int uid) {
            String str = HwArbitrationStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopMPLinkAppBind:");
            stringBuilder.append(uid);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            HwMplinkManager.createInstance(HwArbitrationStateMachine.this.mContext).requestClearBindProcessToNetwork(HwArbitrationFunction.getNetworkID(HwArbitrationStateMachine.this.mContext, HwArbitrationStateMachine.this.getCurrentNetwork(HwArbitrationStateMachine.this.mContext, uid)), uid);
        }

        private void stopMPLinkCoex(int uid) {
            if (!(HwArbitrationStateMachine.mHwArbitrationAppBoostMap == null || HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(uid)) == null || !((HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(uid))).getIsCoex())) {
                ((HwArbitrationAppBoostInfo) HwArbitrationStateMachine.mHwArbitrationAppBoostMap.get(Integer.valueOf(uid))).setIsCoex(false);
                HwArbitrationStateMachine.this.mCoexCount = HwArbitrationStateMachine.this.mCoexCount - 1;
                String str = HwArbitrationStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stopMPLinkCoex, mCoexCount is: ");
                stringBuilder.append(HwArbitrationStateMachine.this.mCoexCount);
                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            }
            if (HwArbitrationStateMachine.this.mMpLinkCount > 0 || HwArbitrationStateMachine.this.mCoexCount > 0) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "other app in MPLink, keep COEX");
                return;
            }
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "no app in MPLink, stop COEX");
            HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStoppingState);
        }

        private int getTargetNetwork(int network, boolean flag) {
            if (!flag) {
                network = HwArbitrationFunction.getNetwork(HwArbitrationStateMachine.this.mContext, network);
            }
            if (800 == network) {
                return 801;
            }
            if (801 == network) {
                return 800;
            }
            return 802;
        }
    }

    class MPLinkStartingState extends State {
        HwAPPStateInfo appStateInfo;

        MPLinkStartingState() {
        }

        public void enter() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Enter MPLinkStartingState");
            HwMplinkManager.createInstance(HwArbitrationStateMachine.this.mContext).requestWiFiAndCellCoexist(true);
            HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_MPLINK_COEXIST_TIMEOUT, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
        }

        public void exit() {
            HwArbitrationStateMachine.this.mIsMpLinkError = false;
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "exit MPLinkStartingState");
            HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_MPLINK_COEXIST_TIMEOUT);
            this.appStateInfo = null;
        }

        public boolean processMessage(Message message) {
            String str = HwArbitrationStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MPLinkStartingState Msg: ");
            stringBuilder.append(message.what);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            int i = message.what;
            if (i == 6) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "WiFi and Celluar coexist successful");
            } else if (i == 107 || i == 110) {
                HwAPPStateInfo appEndInfo = message.obj;
                if (!(appEndInfo == null || this.appStateInfo == null || appEndInfo.mAppUID != this.appStateInfo.mAppUID)) {
                    if (!HwArbitrationFunction.isStreamingScene(this.appStateInfo) || this.appStateInfo.mScenceId == appEndInfo.mScenceId) {
                        HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr(this.appStateInfo, 10);
                        HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStoppingState);
                    } else {
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "appEnd SceneID is not the same as appState");
                    }
                }
            } else {
                if (i != 115) {
                    if (i == 1009) {
                        HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStoppingState);
                    } else if (!(i == HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD || i == HwArbitrationDEFS.MSG_STREAMING_AUDIO_BAD)) {
                        if (i != HwArbitrationDEFS.MSG_MPLINK_COEXIST_TIMEOUT) {
                            switch (i) {
                                case 2003:
                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "start coex successfully, MSG_MPLINK_COEX_MODE");
                                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStartedState);
                                    break;
                                case HwArbitrationDEFS.MSG_MPLINK_NONCOEX_MODE /*2004*/:
                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MPLinkStartingState start COEX error");
                                    HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr(this.appStateInfo, 2);
                                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStoppingState);
                                    break;
                                default:
                                    return false;
                            }
                        }
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Coexist timeout");
                        HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr(this.appStateInfo, 2);
                        HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStoppingState);
                    }
                }
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "defer BAD MSG");
                this.appStateInfo = (HwAPPStateInfo) message.obj;
                HwArbitrationStateMachine.this.deferMessage(message);
            }
            return true;
        }
    }

    class MPLinkStoppingState extends State {
        MPLinkStoppingState() {
        }

        public void enter() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Enter MPLinkStoppingState");
            HwMplinkManager.createInstance(HwArbitrationStateMachine.this.mContext).requestWiFiAndCellCoexist(false);
            HwArbitrationStateMachine.this.sendMessageDelayed(HwArbitrationDEFS.MSG_MPLINK_COEXIST_TIMEOUT, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
        }

        public void exit() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Exit MPLinkStoppingState");
            HwArbitrationStateMachine.this.mIsMpLinkError = false;
            HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_MPLINK_COEXIST_TIMEOUT);
        }

        public boolean processMessage(Message message) {
            String str = HwArbitrationStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MPLinkStoppingState Msg: ");
            stringBuilder.append(message.what);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            int i = message.what;
            if (i != HwArbitrationDEFS.MSG_ARBITRATION_REQUEST_MPLINK) {
                if (i != HwArbitrationDEFS.MSG_MPLINK_COEXIST_TIMEOUT) {
                    switch (i) {
                        case 2003:
                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "MSG_MPLINK_COEX_MODE: stop COEX error ");
                            HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                            HwArbitrationStateMachine.this.deferMessage(HwArbitrationStateMachine.this.obtainMessage(HwArbitrationDEFS.MSG_MPLINK_STOP_COEX_SUCC));
                            break;
                        case HwArbitrationDEFS.MSG_MPLINK_NONCOEX_MODE /*2004*/:
                            HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                            HwArbitrationStateMachine.this.deferMessage(HwArbitrationStateMachine.this.obtainMessage(HwArbitrationDEFS.MSG_MPLINK_STOP_COEX_SUCC));
                            break;
                        default:
                            return false;
                    }
                }
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "clear Coexist timeout");
                HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                HwArbitrationStateMachine.this.deferMessage(HwArbitrationStateMachine.this.obtainMessage(HwArbitrationDEFS.MSG_MPLINK_ERROR_HANDLER));
            }
            return true;
        }
    }

    class WifiMonitorState extends State {
        private HwAPPStateInfo appInfo;

        WifiMonitorState() {
        }

        public void enter() {
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Enter WifiMonitorState");
        }

        public void exit() {
            resetPingPong();
            HwArbitrationStateMachine.this.removeMessages(HwArbitrationDEFS.MSG_SET_PingPong_Cell_Bad_FALSE);
            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Exit WifiMonitorState");
        }

        public boolean processMessage(Message message) {
            String str = HwArbitrationStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WifiMonitorState Msg: ");
            stringBuilder.append(message.what);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            int i = message.what;
            switch (i) {
                case 100:
                    this.appInfo = (HwAPPStateInfo) message.obj;
                    HwArbitrationStateMachine.this.handleGeneralGameStart(this.appInfo);
                    break;
                case 101:
                    this.appInfo = (HwAPPStateInfo) message.obj;
                    HwArbitrationStateMachine.this.handleGamePvpEnd(this.appInfo);
                    HwArbitrationStateMachine.this.handleGeneralGameEnd(this.appInfo);
                    break;
                default:
                    switch (i) {
                        case 103:
                            break;
                        case 104:
                            this.appInfo = (HwAPPStateInfo) message.obj;
                            if (this.appInfo != null) {
                                HwArbitrationStateMachine.this.handleGeneralGameStart(this.appInfo);
                                HwArbitrationStateMachine.this.handleGamePvpStart(this.appInfo);
                                break;
                            }
                            break;
                        case 105:
                            this.appInfo = (HwAPPStateInfo) message.obj;
                            HwArbitrationStateMachine.this.handleGamePvpEnd(this.appInfo);
                            break;
                        case 106:
                            this.appInfo = (HwAPPStateInfo) message.obj;
                            HwArbitrationStateMachine.this.handleStreamingStart(this.appInfo);
                            break;
                        case 107:
                            this.appInfo = (HwAPPStateInfo) message.obj;
                            HwArbitrationStateMachine.this.handleStreamingEnd(this.appInfo);
                            resetPingPong();
                            break;
                        default:
                            switch (i) {
                                case 109:
                                    break;
                                case 110:
                                    resetPingPong();
                                    break;
                                case HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START /*111*/:
                                case 113:
                                    startMPLinkCoex(message, false);
                                    break;
                                case HwArbitrationDEFS.MSG_INSTANT_PAY_APP_END /*112*/:
                                case 114:
                                    resetPingPong();
                                    break;
                                case 115:
                                    startMPLinkCoex(message, true);
                                    break;
                                default:
                                    switch (i) {
                                        case 1005:
                                            break;
                                        case 1006:
                                            HwArbitrationStateMachine.this.mHwWifiBoost.stopAllBoost();
                                            HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mInitialState);
                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "WifiMonitor transitionTo InitialState");
                                            break;
                                        default:
                                            switch (i) {
                                                case 1009:
                                                    HwArbitrationStateMachine.this.mHwWifiBoost.stopAllBoost();
                                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "WifiMonitorState transitionTo CellmonitorState");
                                                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mCellMonitorState);
                                                    break;
                                                case 1010:
                                                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "in WifiMonitorState don't process MSG_CELL_STATE_DISCONNECTED");
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case 1012:
                                                            if (HwArbitrationFunction.isPvpScene()) {
                                                                HwArbitrationStateMachine.this.mHwWifiBoost.setPMMode(3);
                                                                HwArbitrationStateMachine.this.mHwWifiBoost.setPMMode(7);
                                                                HwArbitrationStateMachine.this.mHwWifiBoost.limitedSpeed(1, 0, 0);
                                                                break;
                                                            }
                                                            break;
                                                        case 1017:
                                                            if (HwArbitrationFunction.isPvpScene()) {
                                                                HwArbitrationStateMachine.this.mHwWifiBoost.setPMMode(6);
                                                                HwArbitrationStateMachine.this.mHwWifiBoost.setPMMode(4);
                                                                HwArbitrationStateMachine.this.mHwWifiBoost.limitedSpeed(1, 1, 1);
                                                                break;
                                                            }
                                                            break;
                                                        case HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD /*1106*/:
                                                            if (!HwArbitrationFunction.isInLTE(HwArbitrationStateMachine.this.mContext)) {
                                                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "Phone is not in LTE");
                                                                HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr((HwAPPStateInfo) message.obj, 1);
                                                                break;
                                                            }
                                                        case HwArbitrationDEFS.MSG_STREAMING_AUDIO_BAD /*1108*/:
                                                            if (!HwArbitrationStateMachine.this.mHiStreamTriggerOrStopMplink) {
                                                                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "avoid Ping-pong switching,not trigger mplink");
                                                                HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr((HwAPPStateInfo) message.obj, 3);
                                                                break;
                                                            }
                                                            startMPLinkCoex(message, true);
                                                            break;
                                                        case 2003:
                                                            HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "mplink duplicate callback response");
                                                            break;
                                                        case HwArbitrationDEFS.MSG_SET_PingPong_Cell_Bad_FALSE /*2035*/:
                                                            HwArbitrationCommonUtils.logE(HwArbitrationStateMachine.TAG, "MSG_SET_PingPong_Cell_Bad_FALSE in wifi monitor");
                                                            resetPingPong();
                                                            break;
                                                        default:
                                                            return false;
                                                    }
                                                    break;
                                            }
                                    }
                            }
                    }
                    this.appInfo = (HwAPPStateInfo) message.obj;
                    HwArbitrationStateMachine.this.handleGamePvpEnd(this.appInfo);
                    HwArbitrationStateMachine.this.handleGeneralGameEnd(this.appInfo);
                    break;
            }
            return true;
        }

        private void resetPingPong() {
            HwArbitrationCommonUtils.logE(HwArbitrationStateMachine.TAG, "resetPingPong in wifi monitor");
            HwArbitrationStateMachine.this.trgPingPongCell_Bad = false;
            HwArbitrationStateMachine.this.pingPongTMCell_Bad = 0;
        }

        private void startMPLinkCoex(Message message, boolean needDefer) {
            HwAPPStateInfo appInfo = message.obj;
            int uid = Integer.MIN_VALUE;
            if (appInfo != null) {
                uid = appInfo.mAppUID;
                if (!HwArbitrationStateMachine.this.isNeedChQoEquery(uid)) {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "not allow ChannelQoE and WM query in WiFiMonitor");
                    HwAPPChrManager.getInstance().updateStatisInfo(appInfo, 7);
                    HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr((HwAPPStateInfo) message.obj, 3);
                    return;
                }
            }
            if (HwArbitrationStateMachine.this.trgPingPongCell_Bad) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "not start MPLink, due to trgPingPong in Cell Bad =============");
            } else if (HwArbitrationStateMachine.this.mDenyByNotification) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "not start MPLink, due to deny by Notification in next 24h");
                HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr((HwAPPStateInfo) message.obj, 20);
            } else {
                if (HwArbitrationFunction.isAllowMpLink(HwArbitrationStateMachine.this.mContext, uid)) {
                    if (needDefer && message.obj != null) {
                        HwArbitrationStateMachine.this.deferMessage(message);
                    }
                    HwArbitrationStateMachine.this.transitionTo(HwArbitrationStateMachine.this.mMPLinkStartingState);
                } else {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMachine.TAG, "not allow MpLink");
                    HwAPPChrManager.getInstance().updateStatisInfo(appInfo, 7);
                    HwArbitrationStateMachine.this.mHwArbitrationChrImpl.updateRequestMplinkChr((HwAPPStateInfo) message.obj, 1);
                }
            }
        }
    }

    private void setStateMachineHashMap(HwAPPStateInfo appInfo, int network, boolean isCoex, boolean isMPLink, int solution) {
        if (appInfo != null) {
            setStateMachineHashMap(appInfo.mAppId, appInfo.mAppUID, appInfo.mScenceId, network, isCoex, isMPLink, solution, appInfo);
        }
    }

    private void setStateMachineHashMap(int AppID, int UID, int sceneId, int network, boolean isCoex, boolean isMPLink, int solution, HwAPPStateInfo appInfo) {
        if (mHwArbitrationAppBoostMap == null) {
            mHwArbitrationAppBoostMap = new HashMap();
        }
        HwArbitrationAppBoostInfo hwArbitrationAppBoostInfo = new HwArbitrationAppBoostInfo(AppID, UID, sceneId, network, isCoex, isMPLink, solution);
        hwArbitrationAppBoostInfo.setHwAPPStateInfo(appInfo);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set Map, UID is ");
        int i = UID;
        stringBuilder.append(i);
        stringBuilder.append(" BoostInfo is ");
        stringBuilder.append(hwArbitrationAppBoostInfo.toString());
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        mHwArbitrationAppBoostMap.put(Integer.valueOf(i), hwArbitrationAppBoostInfo);
    }

    private void updateStateMachineHashMap(HwAPPStateInfo appInfo, int network, boolean isCoex, boolean isMPLink, int solution) {
        if (mHwArbitrationAppBoostMap == null || mHwArbitrationAppBoostMap.get(Integer.valueOf(appInfo.mAppUID)) == null) {
            setStateMachineHashMap(appInfo, network, isCoex, isMPLink, solution);
            return;
        }
        HwArbitrationAppBoostInfo boostInfo = (HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(appInfo.mAppUID));
        int newSolution = boostInfo.getSolution() | solution;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setStateMachineHashMap: old solution is    ");
        stringBuilder.append(boostInfo.getSolution());
        stringBuilder.append(",New solution is  ");
        stringBuilder.append(newSolution);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        boostInfo.setSolution(newSolution);
        mHwArbitrationAppBoostMap.put(Integer.valueOf(appInfo.mAppUID), boostInfo);
    }

    private void printMap(int uid) {
        String str;
        StringBuilder stringBuilder;
        if (mHwArbitrationAppBoostMap == null) {
            HwArbitrationCommonUtils.logD(TAG, "Map is null");
        } else if (mHwArbitrationAppBoostMap.get(Integer.valueOf(uid)) == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("MapInfo is null, uid is: ");
            stringBuilder.append(uid);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("uid is ");
            stringBuilder.append(uid);
            stringBuilder.append(" BoostInfo is ");
            stringBuilder.append(((HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(uid))).toString());
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        }
    }

    public static HwArbitrationStateMachine getInstance(Context context) {
        if (mArbitrationStateMachine == null) {
            mArbitrationStateMachine = new HwArbitrationStateMachine(context);
        }
        return mArbitrationStateMachine;
    }

    private HwArbitrationStateMachine(Context context) {
        super("HwArbitrationStateMachine");
        this.mContext = context;
        this.mHwWifiBoost = HwWifiBoost.getInstance(this.mContext);
        this.mHwHiRadioBoost = HwHiRadioBoost.createInstance();
        addState(this.mDefaultState);
        addState(this.mInitialState, this.mDefaultState);
        addState(this.mWifiMonitorState, this.mDefaultState);
        addState(this.mCellMonitorState, this.mDefaultState);
        addState(this.mMPLinkStartingState, this.mDefaultState);
        addState(this.mMPLinkStoppingState, this.mDefaultState);
        addState(this.mMPLinkStartedState, this.mDefaultState);
        setInitialState(this.mInitialState);
        start();
        if (HwArbitrationCommonUtils.isHiCureShow()) {
            this.hiCureDefaultOvertimeReset = 5000;
            this.hiCureDefaultOvertime = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
            this.hiCureInformBlockTime = AppHibernateCst.DELAY_ONE_MINS;
            HwArbitrationCommonUtils.logD(TAG, "HICURE_TEST_MODE");
        }
        this.mHwArbitrationChrImpl = HwArbitrationChrImpl.createInstance();
    }

    private void handleGeneralGameStart(HwAPPStateInfo appInfo) {
        if (appInfo != null && 800 == getCurrentNetwork(this.mContext, appInfo.mAppUID)) {
            this.mHwWifiBoost.setPMMode(6);
            this.mHwWifiBoost.pauseABSHandover();
        }
    }

    private void handleGeneralGameEnd(HwAPPStateInfo appInfo) {
        if (appInfo != null && 800 == getCurrentNetwork(this.mContext, appInfo.mAppUID)) {
            this.mHwWifiBoost.setPMMode(7);
            this.mHwWifiBoost.restartABSHandover();
        }
    }

    private void handleGamePvpStart(HwAPPStateInfo appInfo) {
        if (appInfo != null && 800 == getCurrentNetwork(this.mContext, appInfo.mAppUID)) {
            this.mHwWifiBoost.startGameBoost(appInfo.mAppUID);
        }
    }

    private void handleGamePvpEnd(HwAPPStateInfo appInfo) {
        if (appInfo != null && 800 == getCurrentNetwork(this.mContext, appInfo.mAppUID)) {
            this.mHwWifiBoost.stopGameBoost(appInfo.mAppUID);
        }
    }

    private void handleStreamingStart(HwAPPStateInfo appInfo) {
        if (appInfo != null && 800 == getCurrentNetwork(this.mContext, appInfo.mAppUID)) {
            this.mHwWifiBoost.startStreamingBoost(appInfo.mAppUID);
        }
    }

    private void handleStreamingEnd(HwAPPStateInfo appInfo) {
        if (appInfo != null && 800 == getCurrentNetwork(this.mContext, appInfo.mAppUID)) {
            this.mHwWifiBoost.stopStreamingBoost(appInfo.mAppUID);
        }
    }

    private boolean updateStateInfoMap(MplinkBindResultInfo result, int network, boolean isMPLink) {
        int uid = result.getUid();
        if (mHwArbitrationAppBoostMap == null || mHwArbitrationAppBoostMap.get(Integer.valueOf(uid)) == null) {
            return false;
        }
        HwArbitrationAppBoostInfo gmsInfo = (HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(uid));
        gmsInfo.setIsMPLink(isMPLink);
        gmsInfo.setNetwork(network);
        mHwArbitrationAppBoostMap.put(Integer.valueOf(uid), gmsInfo);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MplinkBindResultInfo ");
        stringBuilder.append(result.toString());
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        HwArbitrationCommonUtils.logD(TAG, "updateStateInfoMap Map");
        printMap(uid);
        return true;
    }

    private void hiCureToast(String string) {
        if (HwArbitrationCommonUtils.isHiCureShow() && this.mDeviceBootCommpleted) {
            Toast.makeText(this.mContext, string, 0).show();
        }
    }

    public boolean isInMPLink(int uid) {
        if (mHwArbitrationAppBoostMap == null || mHwArbitrationAppBoostMap.get(Integer.valueOf(uid)) == null) {
            return false;
        }
        return ((HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(uid))).getIsMPlink();
    }

    private boolean isStreamScene(int uid) {
        boolean z = false;
        if (mHwArbitrationAppBoostMap == null || mHwArbitrationAppBoostMap.get(Integer.valueOf(uid)) == null) {
            return false;
        }
        int scene = ((HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(uid))).getSceneId();
        if (scene == HwAPPQoEUtils.SCENE_DOUYIN || scene == HwAPPQoEUtils.SCENE_VIDEO || scene == HwAPPQoEUtils.SCENE_AUDIO) {
            z = true;
        }
        return z;
    }

    public void sendMPLinkBroadcast(int uid) {
        if (mHwArbitrationAppBoostMap != null && mHwArbitrationAppBoostMap.get(Integer.valueOf(uid)) != null) {
            Intent MPLinkIntent = new Intent("com.android.server.hidata.arbitration.HwArbitrationStateMachine");
            MPLinkIntent.putExtra("MPLinkSuccessUIDKey", uid);
            MPLinkIntent.putExtra("MPLinkSuccessNetworkKey", ((HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(uid))).getNetwork());
            this.mContext.sendBroadcastAsUser(MPLinkIntent, UserHandle.ALL, "com.huawei.hidata.permission.MPLINK_START_CHECK");
        }
    }

    public int getTargetNetID(int network) {
        if (network == 800) {
            return HwArbitrationFunction.getNetworkID(this.mContext, 801);
        }
        if (network == 801) {
            return HwArbitrationFunction.getNetworkID(this.mContext, 800);
        }
        return -1;
    }

    public int getCurrentNetwork(Context mContext, int uid) {
        if (mHwArbitrationAppBoostMap != null) {
            HwArbitrationAppBoostInfo hwArbitrationAppBoostInfo = (HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(uid));
            HwArbitrationAppBoostInfo boostInfo = hwArbitrationAppBoostInfo;
            if (hwArbitrationAppBoostInfo != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("uid: ");
                stringBuilder.append(uid);
                stringBuilder.append(", CurrentNetwork: ");
                stringBuilder.append(boostInfo.getNetwork());
                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                return boostInfo.getNetwork();
            }
        }
        return HwArbitrationCommonUtils.getActiveConnectType(mContext);
    }

    private boolean isNeedChQoEquery(int uid) {
        if (this.mQueryTime == null || !this.mQueryTime.containsKey(Integer.valueOf(uid))) {
            return true;
        }
        long queryTime = ((Long) this.mQueryTime.get(Integer.valueOf(uid))).longValue();
        long nowTime = SystemClock.elapsedRealtime();
        if (nowTime - queryTime > 20000) {
            HwArbitrationCommonUtils.logD(TAG, "isNeedChQoEquery: allow ChannelQoE and WM query");
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isNeedChQoEquery: not allow ChannelQoE and WM query, waiting ");
        stringBuilder.append((20000 + queryTime) - nowTime);
        stringBuilder.append(" Milliseconds");
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        return false;
    }

    private MpLinkQuickSwitchConfiguration getMpLinkQuickSwitchConfiguration(int uid) {
        if (mHwArbitrationAppBoostMap == null || mHwArbitrationAppBoostMap.get(Integer.valueOf(uid)) == null || ((HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(uid))).getHwAPPStateInfo() == null) {
            return null;
        }
        return ((HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(uid))).getHwAPPStateInfo().getQuickSwitchConfiguration();
    }

    public boolean deliverErrorMPLinkCase() {
        if (this.mIsMpLinkError) {
            HwArbitrationCommonUtils.logD(TAG, "denyCoexStatus is processing the MPLink Error");
            return false;
        } else if (getCurrentState() == this.mMPLinkStartingState) {
            setFlagAtTimer(30000);
            HwArbitrationCommonUtils.logD(TAG, "deliverErrorMPLinkCase, goto InitialState");
            transitionTo(this.mMPLinkStoppingState);
            return true;
        } else if (getCurrentState() == this.mMPLinkStartedState) {
            setFlagAtTimer(30000);
            HwArbitrationCommonUtils.logD(TAG, "deliverErrorMPLinkCase in MPLinkStartedState");
            sendMessage(HwArbitrationDEFS.MSG_MPLINK_ERROR);
            return true;
        } else if (getCurrentState() != this.mMPLinkStoppingState) {
            return false;
        } else {
            setFlagAtTimer(30000);
            HwArbitrationCommonUtils.logD(TAG, "deliverErrorMPLinkCase in MPLinkStoppingState");
            return true;
        }
    }

    private boolean noAPPInMPLink() {
        List<Integer> result = getAppUIDInMPLink();
        return result == null || result.isEmpty();
    }

    private List<Integer> getAppUIDInMPLink() {
        if (mHwArbitrationAppBoostMap == null) {
            return null;
        }
        List<Integer> result = new ArrayList();
        for (Entry entry : mHwArbitrationAppBoostMap.entrySet()) {
            HwArbitrationAppBoostInfo val = (HwArbitrationAppBoostInfo) entry.getValue();
            if (val != null && val.getIsMPlink()) {
                result.add((Integer) entry.getKey());
            }
        }
        return result;
    }

    public void setIsAllowQueryCHQoE() {
        HwArbitrationCommonUtils.logD(TAG, "isAllowQueryChQoE:True");
        this.isAllowQueryChQoE = true;
    }

    private void wifiDisconnectedTriggerHiCure() {
        HwArbitrationCommonUtils.logD(TAG, "onWiFiDisconnectedTriggerHiCure");
        this.mHwAPPQoEManager = HwAPPQoEManager.getInstance();
        HwAPPStateInfo hwAPPStateInfo = null;
        HwAPPStateInfo appInfo = this.mHwAPPQoEManager == null ? null : this.mHwAPPQoEManager.getCurAPPStateInfo();
        String str;
        StringBuilder stringBuilder;
        if (appInfo == null || -1 == appInfo.mAppUID) {
            this.mHwHiStreamManager = HwHiStreamManager.getInstance();
            if (this.mHwHiStreamManager != null) {
                hwAPPStateInfo = this.mHwHiStreamManager.getCurStreamAppInfo();
            }
            appInfo = hwAPPStateInfo;
            if (!(appInfo == null || -1 == appInfo.mAppUID)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("AppStreamInfo:");
                stringBuilder.append(appInfo.toString());
                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                sendMessageDelayed(HwArbitrationDEFS.MSG_TRANSITION_TO_HICURE, appInfo.mAppUID, this.hiCureWifiDisconnectedDelayTime);
                HwArbitrationCommonUtils.logD(TAG, "send DelayMessage MSG_TRANSITION_TO_HICURE When WiFi Disconnected");
            }
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("AppInfo:");
        stringBuilder.append(appInfo.toString());
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        sendMessageDelayed(HwArbitrationDEFS.MSG_TRANSITION_TO_HICURE, appInfo.mAppUID, this.hiCureWifiDisconnectedDelayTime);
        HwArbitrationCommonUtils.logD(TAG, "send DelayMessage MSG_TRANSITION_TO_HICURE When WiFi Disconnected");
    }

    private void startHiCure(Context context, int appUID, int blockType, int network) {
        int i = appUID;
        if (this.mCurrentServiceState == 0) {
            int mDefaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            boolean wifiSwitch = HwArbitrationCommonUtils.isWifiEnabled(context);
            boolean wifiLink = HwArbitrationCommonUtils.isWifiConnected(context);
            boolean dataSwitch = HwArbitrationCommonUtils.isCellEnable(context);
            boolean dataLink = HwArbitrationCommonUtils.isCellConnected(context);
            Context context2 = context;
            boolean dataRoamingStatus = HwArbitrationCommonUtils.isDataRoamingEnabled(context2, mDefaultDataSubId);
            String apkName = HwArbitrationCommonUtils.getAppNameUid(context, appUID);
            if (apkName == null || "".equals(apkName)) {
                apkName = Integer.toString(appUID);
            }
            String apkName2 = apkName;
            apkName = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("apkName of appuid:");
            stringBuilder.append(i);
            stringBuilder.append(" is ");
            stringBuilder.append(apkName2);
            HwArbitrationCommonUtils.logD(apkName, stringBuilder.toString());
            StringBuilder stringBuilder2;
            int i2;
            if (HwArbitrationCommonUtils.isCellEnable(context)) {
                this.isAllowHiCure = false;
                HwArbitrationCommonUtils.logD(TAG, "isAllowHiCure is set to be false");
                HwArbitrationCommonUtils.logD(TAG, "HiCure:celluar is not activiated or is blocked");
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("celluar is not activiated or is blocked,Start HiCure, appUID= ");
                stringBuilder2.append(i);
                hiCureToast(stringBuilder2.toString());
                HwHiDataCommonUtils.startHiCure(context2, i, blockType, network, wifiSwitch, wifiLink, dataSwitch, dataLink);
                i2 = HwArbitrationDEFS.MSG_NOT_RECEIVE_HICURE_RESULT;
                this.mHwArbitrationChrImpl.updateHiCureRequestChr(apkName2, blockType, network, dataSwitch, dataRoamingStatus, dataLink, wifiSwitch, wifiLink, 0);
                sendMessageDelayed(i2, this.hiCureDefaultOvertimeReset);
            } else {
                i2 = 1124;
                String apkName3 = apkName2;
                HwArbitrationCommonUtils.logD(TAG, "HiCure:No Connection, need inform user");
                long startCureTime = System.currentTimeMillis();
                long lastCureTime = getLastCureTime(this.mContext);
                long cureTimeDiff = startCureTime - lastCureTime;
                if (cureTimeDiff < 0) {
                    HwArbitrationCommonUtils.logD(TAG, "SystemTime has been changed");
                    System.putLong(this.mContext.getContentResolver(), HwArbitrationDEFS.SETTING_HICURE_LAST_INFROM_TIME, startCureTime);
                    return;
                }
                apkName = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("startCureTime:");
                stringBuilder3.append(startCureTime);
                stringBuilder3.append(", lastCureTime");
                stringBuilder3.append(lastCureTime);
                stringBuilder3.append(", diff = ");
                stringBuilder3.append(cureTimeDiff / 1000);
                stringBuilder3.append(" seconds");
                HwArbitrationCommonUtils.logD(apkName, stringBuilder3.toString());
                long testcure = SystemClock.elapsedRealtime();
                apkName = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("elapseRealtime:");
                stringBuilder3.append(testcure);
                HwArbitrationCommonUtils.logD(apkName, stringBuilder3.toString());
                long startCureTime2;
                if (cureTimeDiff > this.hiCureInformBlockTime) {
                    HwArbitrationCommonUtils.logD(TAG, "HiCure:inform user is allowed");
                    this.isAllowHiCure = false;
                    HwArbitrationCommonUtils.logD(TAG, "isAllowHiCure:false");
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("switch is close ,Start HiCure, appUID= ");
                    stringBuilder2.append(i);
                    hiCureToast(stringBuilder2.toString());
                    int i3 = i2;
                    HwHiDataCommonUtils.startHiCure(context, i, blockType, network, wifiSwitch, wifiLink, dataSwitch, dataLink);
                    int i4 = i3;
                    startCureTime2 = startCureTime;
                    this.mHwArbitrationChrImpl.updateHiCureRequestChr(apkName3, blockType, network, dataSwitch, dataRoamingStatus, dataLink, wifiSwitch, wifiLink, 0);
                    sendMessageDelayed(i4, this.hiCureDefaultOvertimeReset);
                    System.putLong(this.mContext.getContentResolver(), HwArbitrationDEFS.SETTING_HICURE_LAST_INFROM_TIME, startCureTime2);
                } else {
                    long j = lastCureTime;
                    startCureTime2 = startCureTime;
                    apkName = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("not allow inform user, cureTimeDiff:");
                    stringBuilder.append(cureTimeDiff / AppHibernateCst.DELAY_ONE_MINS);
                    stringBuilder.append("minutes");
                    HwArbitrationCommonUtils.logD(apkName, stringBuilder.toString());
                }
            }
        }
    }

    public void receiveCureResult(int result, int overTime, int diagnoseResult, int method) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HiCure result:");
        stringBuilder.append(result);
        stringBuilder.append(" ,overTime:");
        stringBuilder.append(overTime);
        stringBuilder.append(", diagnoseResult:");
        stringBuilder.append(diagnoseResult);
        stringBuilder.append(", method:");
        stringBuilder.append(method);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        long querytime = SystemClock.elapsedRealtime();
        if (0 == this.lastReceiveResultTime || querytime - this.lastReceiveResultTime >= 2000) {
            this.lastReceiveResultTime = querytime;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Receive HiCure result= ");
            stringBuilder2.append(result);
            stringBuilder2.append(", overTime= ");
            stringBuilder2.append(overTime);
            hiCureToast(stringBuilder2.toString());
            sendMessage(HwArbitrationDEFS.MSG_RECEIVED_HICURE_RESULT);
            this.monitorStallAfterCure = true;
            if (overTime <= 0) {
                this.isAllowHiCure = true;
                return;
            }
            this.mHwArbitrationChrImpl.updateHiCureResultChr(result, diagnoseResult, method);
            this.isStallAfterCure = 0;
            sendMessageDelayed(HwArbitrationDEFS.MSG_CURED_IS_STALL, 5000);
            removeMessages(HwArbitrationDEFS.MSG_NOT_RECEIVE_HICURE_RESULT);
            removeMessages(HwArbitrationDEFS.MSG_ALLOW_HICURE);
            sendMessageDelayed(HwArbitrationDEFS.MSG_ALLOW_HICURE, ((long) (overTime * 60)) * 1000);
            return;
        }
        HwArbitrationCommonUtils.logD(TAG, "duplicate broadcast");
        this.lastReceiveResultTime = querytime;
    }

    private long getLastCureTime(Context context) {
        return System.getLong(context.getContentResolver(), HwArbitrationDEFS.SETTING_HICURE_LAST_INFROM_TIME, -1);
    }

    public boolean startMonitorQuality() {
        return this.monitorStallAfterCure;
    }

    private void setFlagAtTimer(int milliseconds) {
        this.mIsMpLinkError = true;
        if (milliseconds < 1) {
            milliseconds = 30000;
        }
        HwArbitrationCommonUtils.logD(TAG, "setFlagAtTimer 30S");
        removeMessages(2001);
        sendMessageDelayed(2001, (long) milliseconds);
    }

    public void updateCurrentStreamAppState(HwAPPStateInfo appInfo, int state) {
        if (appInfo != null) {
            if (state == 100 || state == 103) {
                this.mCurrentStreamAppInfo = appInfo;
            } else if (state == 101) {
                this.mCurrentStreamAppInfo = null;
            } else if (state == 104) {
                if (appInfo.mScenceId == HwAPPQoEUtils.SCENE_VIDEO) {
                    this.mCurrentStreamAppInfo = null;
                } else if (appInfo.mScenceId == HwAPPQoEUtils.SCENE_AUDIO) {
                    this.mCurrentStreamAppInfo = appInfo;
                }
            }
        }
    }

    private synchronized void updateMplinkCHRExceptionEvent(int appUid, int event, int reason) {
        if (mHwArbitrationAppBoostMap != null) {
            HwArbitrationAppBoostInfo hwArbitrationAppBoostInfo = (HwArbitrationAppBoostInfo) mHwArbitrationAppBoostMap.get(Integer.valueOf(appUid));
            HwArbitrationAppBoostInfo boostInfo = hwArbitrationAppBoostInfo;
            if (hwArbitrationAppBoostInfo != null) {
                if (8 == event) {
                    if (800 == boostInfo.getNetwork()) {
                        event = 5;
                    } else if (801 == boostInfo.getNetwork()) {
                        event = 3;
                    }
                }
                if (this.mHwArbitrationChrImpl != null) {
                    this.mHwArbitrationChrImpl.updateMplinkActionChr(boostInfo.getHwAPPStateInfo(), event, reason);
                }
            }
        }
    }

    private void setQueryTime(int uid) {
        if (this.mQueryTime == null) {
            this.mQueryTime = new HashMap(10);
        }
        this.mQueryTime.put(Integer.valueOf(uid), Long.valueOf(SystemClock.elapsedRealtime()));
    }
}
