package com.android.server.hidata.histream;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.android.server.hidata.HwHidataJniAdapter;
import com.android.server.hidata.appqoe.HwAPPQoEAPKConfig;
import com.android.server.hidata.appqoe.HwAPPQoEResourceManger;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.appqoe.HwAPPStateInfo;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;

class HwHiStreamQoeMonitor implements IHwHiStreamJniCallback {
    private static final int AI_DETECT_START_DELAY = 1000;
    private static final int ALGO_PARA_100MS_40 = 0;
    private static final int ALGO_PARA_100MS_80 = 1;
    private static final int ALGO_PARA_2S = 3;
    private static final int AUDIO_STALL_REPORT_MIN_INTERVAL = 10000;
    private static final int CMD_START_STALL_MONITOR = 24;
    private static final int CMD_STOP_AI_ALGO = 26;
    private static final int CMD_STOP_STALL_MONITOR = 25;
    private static final int DETECT_ALGO_FRAME_DETECT = 1;
    private static final int DETECT_ALGO_UDP_AI = 0;
    private static final int DETECT_RESULT_NO_DATA = 1;
    private static final int DOUYIN_DEFAULT_THRESHOLD = 286339651;
    private static final int FRAME_DETECT_DEFAULT_THRESHOLD = 1800;
    private static final int FRAME_DETECT_START_DELAY = 6100;
    private static final int HICURE_REPORT_NO_RX_COUNT = 3;
    private static final int MSG_APP_STATE_START = 1;
    private static final int MSG_APP_STATE_STOP = 2;
    private static final int MSG_DETECT_STALL = 3;
    private static final int MSG_QUERY_TRAFFIC = 5;
    private static final int MSG_START_AI_DETECT = 6;
    private static final int MSG_START_FRAME_DETECT = 4;
    private static final int QUERY_TRAFFIC_INTERVAL = 2000;
    private static final int STALL_DETECT_AFTER_HANDOVER_DELAY = 20000;
    private static final int VIDEO_STALL_REPORT_MIN_INTERVAL = 6000;
    private static HwHiStreamQoeMonitor mHwHiStreamQoeMonitor;
    private long lastFrameReportTime = 0;
    private int mAlgoPara = 3;
    private int mDetectAlgo = 0;
    private int mFrameDetectGeneralTH = FRAME_DETECT_DEFAULT_THRESHOLD;
    private int mFrameDetectThreshold = FRAME_DETECT_DEFAULT_THRESHOLD;
    private HwHidataJniAdapter mHwHidataJniAdapter;
    private long mLastAudioStallTime = 0;
    private long[] mLastCellularTraffic;
    private long[] mLastWifiTraffic;
    private Handler mManagerHandler;
    private Handler mQoeMonitorHandler;
    private long mStartTime = 0;
    private int monitoringSceneId = -1;
    private int monitoringUid = -1;

    private HwHiStreamQoeMonitor(Context context, Handler handler) {
        this.mManagerHandler = handler;
        this.mHwHidataJniAdapter = HwHidataJniAdapter.getInstance();
        initQoeMonitorHandler();
        if (this.mHwHidataJniAdapter != null) {
            this.mHwHidataJniAdapter.registerHiStreamJniCallback(this);
        }
    }

    public static HwHiStreamQoeMonitor createInstance(Context context, Handler handler) {
        if (mHwHiStreamQoeMonitor == null) {
            mHwHiStreamQoeMonitor = new HwHiStreamQoeMonitor(context, handler);
        }
        return mHwHiStreamQoeMonitor;
    }

    public static HwHiStreamQoeMonitor getInstance() {
        return mHwHiStreamQoeMonitor;
    }

    private void initQoeMonitorHandler() {
        HandlerThread handlerThread = new HandlerThread("HwHiStreamQoeMonitor_handler_thread");
        handlerThread.start();
        this.mQoeMonitorHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        HwHiStreamQoeMonitor.this.handleAppStateStart(msg);
                        return;
                    case 2:
                        HwHiStreamQoeMonitor.this.handleAppStateStop(msg);
                        return;
                    case 3:
                        HwHiStreamQoeMonitor.this.handleStallDetect(msg);
                        return;
                    case 4:
                        HwHiStreamQoeMonitor.this.handleStartFrameDetect();
                        return;
                    case 5:
                        HwHiStreamQoeMonitor.this.handleQueryTraffic();
                        return;
                    case 6:
                        HwHiStreamQoeMonitor.this.handleStartAIDetect(msg);
                        return;
                    default:
                        return;
                }
            }
        };
    }

    private int getAppScene(int SceneId) {
        if (HwAPPQoEUtils.SCENE_AUDIO == SceneId) {
            return 1;
        }
        if (HwAPPQoEUtils.SCENE_VIDEO == SceneId) {
            return 2;
        }
        if (HwAPPQoEUtils.SCENE_DOUYIN == SceneId) {
            return 3;
        }
        return -1;
    }

    private void handleQueryTraffic() {
        if (-1 != this.monitoringSceneId && this.mLastWifiTraffic != null && this.mLastCellularTraffic != null && 2 <= this.mLastWifiTraffic.length && 2 <= this.mLastCellularTraffic.length) {
            long[] curWifiTraffic = getCurTraffic(800);
            long[] curCellularTraffic = getCurTraffic(801);
            if (curWifiTraffic != null && curCellularTraffic != null && 2 <= curWifiTraffic.length && 2 <= curCellularTraffic.length && this.mLastWifiTraffic != null && this.mLastCellularTraffic != null && 2 <= this.mLastWifiTraffic.length && 2 <= this.mLastCellularTraffic.length) {
                long celluarTxTraffic = 0;
                long wifiRxTraffic = (0 == curWifiTraffic[0] || 0 == this.mLastWifiTraffic[0]) ? 0 : curWifiTraffic[0] - this.mLastWifiTraffic[0];
                long cellularRxTraffic = (0 == curCellularTraffic[0] || 0 == this.mLastCellularTraffic[0]) ? 0 : curCellularTraffic[0] - this.mLastCellularTraffic[0];
                long wifiTxTraffic = (0 == curWifiTraffic[1] || 0 == this.mLastWifiTraffic[1]) ? 0 : curWifiTraffic[1] - this.mLastWifiTraffic[1];
                if (!(0 == curCellularTraffic[1] || 0 == this.mLastCellularTraffic[1])) {
                    celluarTxTraffic = curCellularTraffic[1] - this.mLastCellularTraffic[1];
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleQueryTraffic: RxWifi= ");
                stringBuilder.append(wifiRxTraffic);
                stringBuilder.append(",Rxcellular=");
                stringBuilder.append(cellularRxTraffic);
                stringBuilder.append(",TXwifi= ");
                stringBuilder.append(wifiTxTraffic);
                stringBuilder.append(",Txcellular=");
                stringBuilder.append(celluarTxTraffic);
                HwHiStreamUtils.logD(stringBuilder.toString());
                Bundle bundle = new Bundle();
                bundle.putInt("wifiRxTraffic", (int) wifiRxTraffic);
                bundle.putInt("cellularRxTraffic", (int) cellularRxTraffic);
                bundle.putInt("wifiTxTraffic", (int) wifiTxTraffic);
                bundle.putInt("celluarTxTraffic", (int) celluarTxTraffic);
                bundle.putInt("monitoringUid", this.monitoringUid);
                this.mManagerHandler.sendMessage(this.mManagerHandler.obtainMessage(11, bundle));
                this.mLastWifiTraffic = curWifiTraffic;
                this.mLastCellularTraffic = curCellularTraffic;
                this.mQoeMonitorHandler.removeMessages(5);
                this.mQoeMonitorHandler.sendEmptyMessageDelayed(5, 2000);
            }
        }
    }

    private void handleStartFrameDetect() {
        if (HwAPPQoEUtils.SCENE_VIDEO == this.monitoringSceneId) {
            int appScene = getAppScene(this.monitoringSceneId);
            this.mDetectAlgo = 1;
            this.mHwHidataJniAdapter.startFrameDetect(appScene);
        }
    }

    private void handleStartAIDetect(Message msg) {
        if (-1 != this.monitoringSceneId) {
            int userType = msg.obj.getInt("userType");
            this.mHwHidataJniAdapter.sendStallDetectCmd(24, this.monitoringUid, getAppScene(this.monitoringSceneId), this.mAlgoPara, userType);
        }
    }

    private void handleAppStateStart(Message msg) {
        if (msg != null && msg.obj != null) {
            Bundle bundle = msg.obj;
            int uid = bundle.getInt("uid");
            int appSceneId = bundle.getInt("appSceneId");
            int stallThreshold = bundle.getInt("stallThreshold");
            int userType = bundle.getInt("userType");
            int appScene = getAppScene(appSceneId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleAppStateStart: appSceneId = ");
            stringBuilder.append(appSceneId);
            stringBuilder.append(",stallThreshold = ");
            stringBuilder.append(stallThreshold);
            HwHiStreamUtils.logD(stringBuilder.toString());
            this.monitoringSceneId = appSceneId;
            this.monitoringUid = uid;
            this.mDetectAlgo = 0;
            this.mStartTime = System.currentTimeMillis();
            if (HwAPPQoEUtils.SCENE_VIDEO == this.monitoringSceneId) {
                this.mQoeMonitorHandler.removeMessages(4);
                this.mQoeMonitorHandler.sendEmptyMessageDelayed(4, 6100);
                this.mFrameDetectThreshold = stallThreshold;
                this.mFrameDetectGeneralTH = getAppStallThreshold(1, this.monitoringSceneId);
                this.mAlgoPara = 3;
            } else {
                this.mAlgoPara = stallThreshold;
            }
            if (-1 == this.mHwHidataJniAdapter.sendStallDetectCmd(24, uid, appScene, this.mAlgoPara, userType)) {
                this.mQoeMonitorHandler.sendMessageDelayed(this.mQoeMonitorHandler.obtainMessage(6, bundle), 1000);
            }
            this.lastFrameReportTime = 0;
            this.mLastAudioStallTime = 0;
            this.mLastWifiTraffic = getCurTraffic(800);
            this.mLastCellularTraffic = getCurTraffic(801);
            this.mQoeMonitorHandler.removeMessages(5);
            this.mQoeMonitorHandler.sendEmptyMessageDelayed(5, 2000);
        }
    }

    private void handleAppStateStop(Message msg) {
        if (msg != null && msg.obj != null) {
            Bundle bundle = msg.obj;
            int uid = bundle.getInt("uid");
            int appSceneId = bundle.getInt("appSceneId");
            int appScene = getAppScene(appSceneId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleAppStateStop: appSceneId = ");
            stringBuilder.append(appSceneId);
            HwHiStreamUtils.logD(stringBuilder.toString());
            this.monitoringSceneId = -1;
            this.monitoringUid = -1;
            this.mStartTime = 0;
            if (1 == this.mDetectAlgo) {
                this.mHwHidataJniAdapter.stopFrameDetect();
            }
            this.mHwHidataJniAdapter.sendStallDetectCmd(25, uid, appScene, this.mAlgoPara, 0);
            this.mQoeMonitorHandler.removeMessages(5);
            this.mLastWifiTraffic = null;
            this.mLastCellularTraffic = null;
        }
    }

    public void handleStallDetect(Message msg) {
        Message message = msg;
        if (message != null && message.obj != null) {
            Bundle bundle = message.obj;
            int stallTime = bundle.getInt("stallTime");
            int appScene = bundle.getInt("appScene");
            int algo = bundle.getInt("algo");
            int curAppScene = getAppScene(this.monitoringSceneId);
            int frameDetectTH = -1;
            HwHiStreamNetworkMonitor mHwHiStreamNetworkMonitor = HwHiStreamNetworkMonitor.getInstance();
            Bundle bundle2;
            if (curAppScene != appScene || this.mDetectAlgo != algo) {
                bundle2 = bundle;
            } else if (mHwHiStreamNetworkMonitor == null) {
                bundle2 = bundle;
            } else if ((2 == appScene || 1 == appScene) && 1 == stallTime) {
                this.mManagerHandler.sendEmptyMessage(10);
            } else {
                StringBuilder stringBuilder;
                long curTime = System.currentTimeMillis();
                long handoverDelay = curTime - mHwHiStreamNetworkMonitor.mLastHandoverTime;
                if (1 == algo) {
                    frameDetectTH = 800 == mHwHiStreamNetworkMonitor.getCurrNetworkType(this.monitoringUid) ? this.mFrameDetectThreshold : this.mFrameDetectGeneralTH;
                    if (stallTime >= frameDetectTH) {
                        if (6000 <= curTime - this.lastFrameReportTime) {
                            this.lastFrameReportTime = curTime;
                        } else {
                            return;
                        }
                    }
                    return;
                }
                long curTime2;
                if (1 == appScene) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("AUDIO stall interval = ");
                    curTime2 = curTime;
                    stringBuilder.append(System.currentTimeMillis() - this.mLastAudioStallTime);
                    HwHiStreamUtils.logD(stringBuilder.toString());
                    if (MemoryConstant.MIN_INTERVAL_OP_TIMEOUT < System.currentTimeMillis() - this.mLastAudioStallTime) {
                        this.mLastAudioStallTime = System.currentTimeMillis();
                        return;
                    }
                    this.mLastAudioStallTime = System.currentTimeMillis();
                } else {
                    curTime2 = curTime;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleStallDetect: detect stall, appScene = ");
                stringBuilder.append(appScene);
                stringBuilder.append(",curAppScene=");
                stringBuilder.append(curAppScene);
                stringBuilder.append(",algo=");
                stringBuilder.append(algo);
                stringBuilder.append(",detectResult =");
                stringBuilder.append(stallTime);
                stringBuilder.append(",frameDetectTH=");
                stringBuilder.append(frameDetectTH);
                HwHiStreamUtils.logD(stringBuilder.toString());
                if (HwRecentsTaskUtils.MAX_REMOVE_TASK_TIME <= handoverDelay || curTime2 - this.mStartTime <= handoverDelay || !(2 == appScene || 1 == appScene)) {
                    Bundle bundle3 = new Bundle();
                    bundle3.putInt("appSceneId", this.monitoringSceneId);
                    bundle3.putInt("detectResult", (stallTime * 100) / VIDEO_STALL_REPORT_MIN_INTERVAL);
                    this.mManagerHandler.sendMessage(this.mManagerHandler.obtainMessage(9, bundle3));
                }
            }
        }
    }

    private int getAppStallThreshold(int userType, int sceneceId) {
        HwAPPQoEResourceManger mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();
        int threshold = -1;
        if (mHwAPPQoEResourceManger != null) {
            HwAPPQoEAPKConfig aPKScenceConfig = mHwAPPQoEResourceManger.getAPKScenceConfig(sceneceId);
            HwAPPQoEAPKConfig apkConfig = aPKScenceConfig;
            if (aPKScenceConfig != null) {
                threshold = 1 == userType ? apkConfig.mGeneralStallTH : apkConfig.mAggressiveStallTH;
            }
        }
        if (HwAPPQoEUtils.SCENE_VIDEO == sceneceId) {
            return -1 == threshold ? FRAME_DETECT_DEFAULT_THRESHOLD : (threshold * VIDEO_STALL_REPORT_MIN_INTERVAL) / 100;
        } else if (HwAPPQoEUtils.SCENE_DOUYIN != sceneceId) {
            return -1;
        } else {
            return -1 == threshold ? DOUYIN_DEFAULT_THRESHOLD : threshold;
        }
    }

    public void onAPPStateChange(HwAPPStateInfo stateInfo, int appState) {
        if (stateInfo == null) {
            HwHiStreamUtils.logE("onAPPStateChange:stateInfo is null");
            return;
        }
        Bundle bundle = new Bundle();
        int appSceneId = stateInfo.mScenceId;
        if (100 == appState || 103 == appState) {
            int stallThreshold = getAppStallThreshold(stateInfo.mUserType, appSceneId);
            if (-1 == this.monitoringSceneId) {
                bundle.putInt("uid", stateInfo.mAppUID);
                bundle.putInt("appSceneId", appSceneId);
                bundle.putInt("stallThreshold", stallThreshold);
                bundle.putInt("userType", stateInfo.mUserType);
                this.mQoeMonitorHandler.sendMessage(this.mQoeMonitorHandler.obtainMessage(1, bundle));
            } else if (appSceneId != this.monitoringSceneId) {
                bundle.putInt("uid", this.monitoringUid);
                bundle.putInt("appSceneId", this.monitoringSceneId);
                this.mQoeMonitorHandler.sendMessage(this.mQoeMonitorHandler.obtainMessage(2, bundle));
                Bundle bundle1 = new Bundle();
                bundle1.putInt("uid", stateInfo.mAppUID);
                bundle1.putInt("appSceneId", appSceneId);
                this.mQoeMonitorHandler.sendMessageDelayed(this.mQoeMonitorHandler.obtainMessage(1, bundle1), 500);
            }
        } else if ((101 == appState || (104 == appState && HwAPPQoEUtils.SCENE_VIDEO == appSceneId)) && appSceneId == this.monitoringSceneId) {
            bundle.putInt("uid", stateInfo.mAppUID);
            bundle.putInt("appSceneId", appSceneId);
            this.mQoeMonitorHandler.sendMessage(this.mQoeMonitorHandler.obtainMessage(2, bundle));
        }
    }

    private long[] getCurTraffic(int network) {
        long[] traffic = new long[2];
        if (HwAPPQoEUtils.SCENE_VIDEO != this.monitoringSceneId && HwAPPQoEUtils.SCENE_AUDIO != this.monitoringSceneId) {
            return traffic;
        }
        HwHiStreamNetworkMonitor mHwHiStreamNetworkMonitor = HwHiStreamNetworkMonitor.getInstance();
        if (mHwHiStreamNetworkMonitor == null || network != mHwHiStreamNetworkMonitor.getCurrNetworkType(this.monitoringUid)) {
            return traffic;
        }
        return this.mHwHidataJniAdapter.getCurrTotalTraffic();
    }

    public void onStallInfoReportCallback(int stallTime, int appScene, int algo) {
        Bundle bundle = new Bundle();
        bundle.putInt("stallTime", stallTime);
        bundle.putInt("appScene", appScene);
        bundle.putInt("algo", algo);
        this.mQoeMonitorHandler.sendMessage(this.mQoeMonitorHandler.obtainMessage(3, bundle));
    }
}
