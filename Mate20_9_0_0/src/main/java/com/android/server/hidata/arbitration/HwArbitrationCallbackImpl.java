package com.android.server.hidata.arbitration;

import android.content.Context;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.appqoe.HwAPPStateInfo;
import com.android.server.hidata.appqoe.IHwAPPQoECallback;
import com.android.server.hidata.channelqoe.IChannelQoECallback;
import com.android.server.hidata.histream.HwHistreamCHRQoeInfo;
import com.android.server.hidata.histream.IHwHistreamQoeCallback;
import com.android.server.hidata.mplink.IMpLinkCallback;
import com.android.server.hidata.mplink.MplinkBindResultInfo;
import com.android.server.hidata.mplink.MplinkNetworkResultInfo;
import com.android.server.hidata.wavemapping.IWaveMappingCallback;
import java.util.HashMap;

public class HwArbitrationCallbackImpl implements IMpLinkCallback, IHwAPPQoECallback, IWaveMappingCallback, IChannelQoECallback, IHwHistreamQoeCallback {
    private static final int MSG_APP_STATE_BAD_NO_RX = 109;
    private static final String TAG = "HiData_HwArbitrationCallbackImpl";
    private static HwArbitrationCallbackImpl mHwArbitrationCallbackImpl;
    private int UID;
    private int checkFailReason = -1;
    private int isFullInfo = 0;
    private boolean isTargetNetworkGood = true;
    private Context mContext;
    private HashMap<Integer, HwAPPStateInfo> mDelayMsgMap;
    private IGameCHRCallback mGameCHRCallback;
    private int mGameState;
    private HwArbitrationStateMachine mHwArbitrationStateMachine;
    private HwAPPStateInfo mPreviousAppInfo;
    private boolean mWifiPlusFromBrain = false;
    private int network;

    public static HwArbitrationCallbackImpl getInstance(Context context) {
        if (mHwArbitrationCallbackImpl == null) {
            mHwArbitrationCallbackImpl = new HwArbitrationCallbackImpl(context);
        }
        return mHwArbitrationCallbackImpl;
    }

    public static HwArbitrationCallbackImpl getInstanceForChr() {
        return mHwArbitrationCallbackImpl;
    }

    public void regisGameCHR(IGameCHRCallback callback) {
        this.mGameCHRCallback = callback;
    }

    private HwArbitrationCallbackImpl(Context context) {
        this.mContext = context;
        this.mHwArbitrationStateMachine = HwArbitrationStateMachine.getInstance(this.mContext);
        HwArbitrationCommonUtils.logD(TAG, "init HwArbitrationCallbackImpl completed!");
    }

    public void onBindProcessToNetworkResult(MplinkBindResultInfo result) {
        if (result != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onMpLinkBindResult:result = ");
            stringBuilder.append(result.getResult());
            stringBuilder.append(", failReason = ");
            stringBuilder.append(result.getFailReason());
            stringBuilder.append(", uid = ");
            stringBuilder.append(result.getUid());
            stringBuilder.append(", network = ");
            stringBuilder.append(result.getNetwork());
            stringBuilder.append(", type = ");
            stringBuilder.append(result.getType());
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            switch (result.getResult()) {
                case 1:
                    this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_MPLINK_BIND_SUCCESS, result);
                    return;
                case 2:
                    this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL, result);
                    return;
                case 3:
                    this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS, result);
                    return;
                case 4:
                    this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL, result);
                    return;
                default:
                    return;
            }
        }
    }

    public void onWiFiAndCellCoexistResult(MplinkNetworkResultInfo result) {
        HwArbitrationCommonUtils.logD(TAG, "onWiFiAndCellCoexistResult");
        if (result != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mResult = ");
            stringBuilder.append(result.getResult());
            stringBuilder.append(", mFailReason = ");
            stringBuilder.append(result.getFailReason());
            stringBuilder.append(", mApType = ");
            stringBuilder.append(result.getAPType());
            stringBuilder.append(", mActivteNetwork =");
            stringBuilder.append(result.getActiveNetwork());
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            if (100 == result.getResult()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("enter wifi cell coex mode,type:");
                stringBuilder.append(result.getAPType());
                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                if (1 == result.getAPType()) {
                    this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_MPLINK_AI_DEVICE_COEX_MODE, result);
                } else {
                    this.mHwArbitrationStateMachine.sendMessage(2003, result);
                }
            } else if (101 == result.getResult()) {
                HwArbitrationCommonUtils.logD(TAG, "leave wifi cell coex mode");
                this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_MPLINK_NONCOEX_MODE, result);
            }
        }
    }

    public void onWifiLinkQuality(int uid, int scene, boolean isSatisfy) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(" scene: ");
        stringBuilder.append(scene);
        stringBuilder.append(" isSatisfy: ");
        stringBuilder.append(isSatisfy);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        if (isSatisfy) {
            HwArbitrationCommonUtils.logD(TAG, "APPQoE detect that wifi is good");
            this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_APPQoE_WIFI_GOOD, uid);
        }
    }

    public void onAPPStateCallBack(HwAPPStateInfo appInfo, int state) {
        if (appInfo == null) {
            HwArbitrationCommonUtils.logD(TAG, "appInfo is null");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onAppStateCallBack: appInfo:");
        stringBuilder.append(appInfo.toString());
        stringBuilder.append(" state:");
        stringBuilder.append(state);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        int i = appInfo.mAppType;
        if (i != 1000) {
            if (i != 2000 && i != HwAPPQoEUtils.APP_TYPE_GENERAL_GAME) {
                if (i == HwAPPQoEUtils.APP_TYPE_STREAMING) {
                    this.mHwArbitrationStateMachine.updateCurrentStreamAppState(appInfo, state);
                    switch (state) {
                        case 100:
                            if (HwArbitrationFunction.isStreamingScene(appInfo)) {
                                this.mHwArbitrationStateMachine.sendMessage(106, appInfo);
                                HwArbitrationCommonUtils.logD(TAG, "MSG_STREAMING_APP_START");
                            }
                            this.mWifiPlusFromBrain = true;
                            break;
                        case 101:
                            if (HwArbitrationFunction.isStreamingScene(appInfo)) {
                                this.mHwArbitrationStateMachine.sendMessage(107, appInfo);
                                HwArbitrationCommonUtils.logD(TAG, "MSG_STREAM_APP_END");
                            }
                            this.mWifiPlusFromBrain = false;
                            break;
                        case 103:
                            if (HwArbitrationFunction.isStreamingScene(appInfo)) {
                                this.mHwArbitrationStateMachine.sendMessage(120, appInfo);
                                HwArbitrationCommonUtils.logD(TAG, "Callback:MSG_STREAMING_APP_FOREGROUND");
                                break;
                            }
                            break;
                        case 104:
                            if (HwArbitrationFunction.isStreamingScene(appInfo)) {
                                HwArbitrationCommonUtils.logD(TAG, "Callback:MSG_STREAMING_APP_BACKGROUND");
                                this.mHwArbitrationStateMachine.sendMessage(121, appInfo);
                                break;
                            }
                            break;
                    }
                }
            }
            switch (state) {
                case 100:
                    if (appInfo.mScenceId != HwAPPQoEUtils.GAME_SCENCE_IN_WAR) {
                        if (appInfo.mScenceId == HwAPPQoEUtils.GAME_SCENCE_NOT_IN_WAR) {
                            this.mHwArbitrationStateMachine.sendMessage(100, appInfo);
                            HwArbitrationCommonUtils.logD(TAG, "MSG_GAME_STATE_START");
                            break;
                        }
                    }
                    this.mHwArbitrationStateMachine.sendMessage(104, appInfo);
                    HwArbitrationCommonUtils.logD(TAG, "MSG_GAME_ENTER_PVP_BATTLE");
                    HwArbitrationFunction.setPvpScene(true);
                    this.mGameState = 4;
                    updateGameState(appInfo, this.mGameState);
                    break;
                    break;
                case 101:
                    this.mHwArbitrationStateMachine.sendMessage(101, appInfo);
                    HwArbitrationCommonUtils.logD(TAG, "MSG_GAME_STATE_END");
                    HwArbitrationFunction.setPvpScene(false);
                    this.mGameState = 5;
                    updateGameState(appInfo, this.mGameState);
                    break;
                case 102:
                    if (appInfo.mScenceId != HwAPPQoEUtils.GAME_SCENCE_IN_WAR) {
                        if (appInfo.mScenceId == HwAPPQoEUtils.GAME_SCENCE_NOT_IN_WAR) {
                            this.mHwArbitrationStateMachine.sendMessage(105, appInfo);
                            HwArbitrationCommonUtils.logD(TAG, "MSG_GAME_EXIT_PVP_BATTLE");
                            HwArbitrationFunction.setPvpScene(false);
                            this.mGameState = 2;
                            updateGameState(appInfo, this.mGameState);
                            break;
                        }
                    }
                    this.mHwArbitrationStateMachine.sendMessage(104, appInfo);
                    HwArbitrationCommonUtils.logD(TAG, "MSG_GAME_ENTER_PVP_BATTLE");
                    HwArbitrationFunction.setPvpScene(true);
                    this.mGameState = 1;
                    updateGameState(appInfo, this.mGameState);
                    break;
                    break;
                case 104:
                    this.mHwArbitrationStateMachine.sendMessage(103, appInfo);
                    HwArbitrationCommonUtils.logD(TAG, "MSG_GAME_STATE_BACKGROUND");
                    if (appInfo.mScenceId == HwAPPQoEUtils.GAME_SCENCE_IN_WAR) {
                        this.mGameState = 3;
                        updateGameState(appInfo, this.mGameState);
                    }
                    HwArbitrationFunction.setPvpScene(false);
                    break;
            }
        }
        switch (state) {
            case 100:
            case 102:
                if (isPayScene(appInfo)) {
                    this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START, appInfo);
                    HwArbitrationCommonUtils.logD(TAG, "MSG_INSTANT_PAY_APP_START");
                } else if (isTravelScene(appInfo)) {
                    this.mHwArbitrationStateMachine.sendMessage(113, appInfo);
                    HwArbitrationCommonUtils.logD(TAG, "MSG_INSTANT_TRAVEL_APP_START");
                } else if (isPayEnd(appInfo, this.mPreviousAppInfo)) {
                    this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_INSTANT_PAY_APP_END, appInfo);
                    HwArbitrationCommonUtils.logD(TAG, "MSG_INSTANT_PAY_APP_END");
                } else if (isTravelEnd(appInfo, this.mPreviousAppInfo)) {
                    this.mHwArbitrationStateMachine.sendMessage(114, appInfo);
                    HwArbitrationCommonUtils.logD(TAG, "MSG_INSTANT_TRAVEL_APP_END");
                } else {
                    this.mHwArbitrationStateMachine.sendMessage(109, appInfo);
                    if (100 == state) {
                        HwArbitrationCommonUtils.logD(TAG, "MSG_INSTANT_APP_START");
                    } else {
                        HwArbitrationCommonUtils.logD(TAG, "MSG_INSTANT_APP_UPDATE");
                    }
                }
                this.mPreviousAppInfo = appInfo;
                this.mWifiPlusFromBrain = true;
                break;
            case 101:
                this.mHwArbitrationStateMachine.sendMessage(110, appInfo);
                HwArbitrationCommonUtils.logD(TAG, "MSG_INSTANT_APP_END");
                this.mPreviousAppInfo = null;
                this.mWifiPlusFromBrain = false;
                break;
        }
        if (802 == HwArbitrationCommonUtils.getActiveConnectType(this.mContext)) {
            if (state == 100) {
                this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_APP_STATE_FOREGROUND, appInfo);
            } else if (state == 101) {
                this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_APP_STATE_BACKGROUND, appInfo);
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0058, code:
            if (r0 != com.android.server.hidata.appqoe.HwAPPQoEUtils.APP_TYPE_STREAMING) goto L_0x00a8;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onAPPQualityCallBack(HwAPPStateInfo appInfo, int experience) {
        if (appInfo == null) {
            HwArbitrationCommonUtils.logD(TAG, "appInfo is null");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(appInfo.toString());
        stringBuilder.append(",experience:");
        stringBuilder.append(experience);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        if (109 == experience) {
            this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_APP_STATE_BAD_NO_RX, appInfo);
            HwArbitrationCommonUtils.logD(TAG, "MSG_APP_STATE_BAD_NO_RX");
        }
        if (this.mHwArbitrationStateMachine.startMonitorQuality() && 107 == experience) {
            this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_APPQOE_REPORT_BAD, appInfo);
        }
        int i = appInfo.mAppType;
        if (i != 1000) {
            if (i == 2000) {
                if (appInfo.mScenceId == HwAPPQoEUtils.GAME_SCENCE_IN_WAR && experience == 107) {
                    HwArbitrationCommonUtils.logD(TAG, "MSG_GAME_WAR_STATE_BAD, ignore it");
                }
            }
        }
        if (experience == 107) {
            if (appInfo.mScenceId == HwAPPQoEUtils.SCENE_VIDEO) {
                this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_STREAMING_VIDEO_BAD, appInfo);
                HwArbitrationCommonUtils.logD(TAG, "MSG_STREAMING_VIDEO_BAD");
            } else if (appInfo.mScenceId == HwAPPQoEUtils.SCENE_AUDIO) {
                this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_STREAMING_AUDIO_BAD, appInfo);
                HwArbitrationCommonUtils.logD(TAG, "MSG_STREAMING_AUDIO_BAD");
            } else {
                this.mHwArbitrationStateMachine.sendMessage(115, appInfo);
                HwArbitrationCommonUtils.logD(TAG, "MSG_INSTANT_BAD");
            }
        }
    }

    public void onHistreamBadQoedetect(HwHistreamCHRQoeInfo qoeInfo) {
        if (this.mGameCHRCallback != null && qoeInfo != null) {
            this.mGameCHRCallback.updateHistreamExperience(qoeInfo);
        }
    }

    public void onNetworkQualityCallBack(int UID, int sense, int network, boolean isSatisfy) {
        HwArbitrationCommonUtils.logD(TAG, "onChannelQoENetworkQualityCallBack");
        judgeBothCQEAndWM(UID, network, isSatisfy, true);
    }

    public void onAPPRttInfoCallBack(HwAPPStateInfo info) {
        updataGameExperience(info);
    }

    public void onWaveMappingRespondCallback(int UID, int prefer, int network, boolean isGood, boolean found) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onWaveMappingRespondCallback, UID: ");
        stringBuilder.append(UID);
        stringBuilder.append(", prefer: ");
        stringBuilder.append(prefer);
        stringBuilder.append(", network: ");
        stringBuilder.append(network);
        stringBuilder.append(", isGood: ");
        stringBuilder.append(isGood);
        stringBuilder.append(", found: ");
        stringBuilder.append(found);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        if (!found) {
            HwArbitrationCommonUtils.logD(TAG, "WaveMapping cannot find the history record");
            isGood = true;
        }
        if (!(network == 800 || network == 801)) {
            HwArbitrationCommonUtils.logD(TAG, "WaveMapping network error");
            isGood = true;
        }
        judgeBothCQEAndWM(UID, network, isGood, false);
    }

    public void onWaveMappingReportCallback(int reportType, String networkName, int networkType) {
        HwArbitrationCommonUtils.logD(TAG, "onWaveMappingReportCallback");
    }

    public void onCellPSAvailable(boolean isOK, int reason) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onCellPSAvailable, isOk:");
        stringBuilder.append(isOK);
        stringBuilder.append(", reason:");
        stringBuilder.append(reason);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        this.mHwArbitrationStateMachine.setIsAllowQueryCHQoE();
        if (isOK) {
            this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_CHANNELQOE_NOTIFY_RESULT_GOOD, reason);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("receive MSG_CHANNEL_NOTIFY_RESULT_GOOD,Reason: ");
            stringBuilder.append(reason);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        } else if (2 != reason) {
            this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_CHANNELQOE_NOTIFY_RESULT_BAD, reason);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("receive MSG_CHANNEL_NOTIFY_RESULT_BAD,Reason: ");
            stringBuilder.append(reason);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        } else {
            HwArbitrationCommonUtils.logD(TAG, "current  cell signal quality is poor");
        }
    }

    public void onChannelQuality(int UID, int sense, int network, int label) {
    }

    public void onWifiLinkQuality(int UID, int sense, int label) {
    }

    public void onCurrentRtt(int rtt) {
    }

    private synchronized void judgeBothCQEAndWM(int UID, int network, boolean isGood, boolean isChannelQoE) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" judgeBothCQEAndWM, UID: ");
        stringBuilder.append(UID);
        stringBuilder.append(", network: ");
        stringBuilder.append(network);
        stringBuilder.append(", isGood: ");
        stringBuilder.append(isGood);
        stringBuilder.append(", isChannelQoE: ");
        stringBuilder.append(isChannelQoE);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        if (!isGood) {
            int i;
            if (isChannelQoE) {
                i = 5;
            } else {
                i = 4;
            }
            this.checkFailReason = i;
        }
        boolean z = true;
        if (1 == this.isFullInfo) {
            if (!(this.isTargetNetworkGood && isGood)) {
                z = false;
            }
            this.isTargetNetworkGood = z;
            if (this.isTargetNetworkGood) {
                this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_MPLINK_BIND_CHECK_OK_NOTIFY, UID);
            } else {
                this.mHwArbitrationStateMachine.sendMessage(HwArbitrationDEFS.MSG_MPLINK_BIND_CHECK_FAIL_NOTIFY, UID, this.checkFailReason);
            }
            this.isFullInfo = 0;
            this.UID = -1;
            this.network = -1;
            this.checkFailReason = -1;
        } else {
            this.isFullInfo++;
            this.isTargetNetworkGood = isGood;
            this.UID = UID;
            this.network = network;
        }
    }

    private void updateGameState(HwAPPStateInfo appInfo, int state) {
        if (this.mGameCHRCallback != null && appInfo != null) {
            this.mGameCHRCallback.updateGameState(appInfo, state);
        }
    }

    public void updataGameExperience(HwAPPStateInfo appInfo) {
        if (this.mGameCHRCallback != null && appInfo != null) {
            this.mGameCHRCallback.updataGameExperience(appInfo);
        }
    }

    private boolean isPayScene(HwAPPStateInfo appInfo) {
        return false;
    }

    private boolean isTravelScene(HwAPPStateInfo appInfo) {
        return false;
    }

    private boolean isPayEnd(HwAPPStateInfo currentAppInfo, HwAPPStateInfo previousAppInfo) {
        return false;
    }

    private boolean isTravelEnd(HwAPPStateInfo currentAppInfo, HwAPPStateInfo previousAppInfo) {
        return false;
    }

    public boolean getWifiPlusFlagFromHiData() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mWifiPlusFromBrain is ");
        stringBuilder.append(this.mWifiPlusFromBrain);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        return this.mWifiPlusFromBrain;
    }
}
