package com.android.server.hidata.histream;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.contentsensor.IActivityObserver;
import android.contentsensor.IActivityObserver.Stub;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.AvailabilityCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import com.android.server.am.HwActivityManagerService;
import com.android.server.hidata.appqoe.HwAPPQoEAPKConfig;
import com.android.server.hidata.appqoe.HwAPPQoEResourceManger;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.appqoe.HwAPPStateInfo;
import com.android.server.hidata.mplink.MpLinkQuickSwitchConfiguration;

public class HwHiStreamContentAware {
    private static final String ACTION_AUDIO_RECORD_STATE_CHANGED = "huawei.media.AUDIO_RECORD_STATE_CHANGED_ACTION";
    private static final int APP_MONITOR_INTERVAL = 1500;
    private static final int AUDIO_RECORD_STATE_RECORDING = 3;
    private static final int AUDIO_RECORD_STATE_STOPPED = 1;
    private static final String INTENT_PACKAGENAME = "packagename";
    private static final String INTENT_STATE = "state";
    private static final int TOP_ACTIVITY_DOUYIN = 2;
    private static final int TOP_ACTIVITY_OTHERS = -1;
    private static final int TOP_ACTIVITY_WECHAT_CALL = 1;
    private static final int WECHAT_CALL_ACTIVITY_PAUSE = 2;
    private static final int WECHAT_CALL_ACTIVITY_RESUME = 1;
    private static final String WECHAT_NAME = "com.tencent.mm";
    private static HwHiStreamContentAware mHwHiStreamContentAware;
    private HwActivityManagerService mActivityManagerService;
    private IActivityObserver mActivityObserver = new Stub() {
        public void activityResumed(int pid, int uid, ComponentName componentName) throws RemoteException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("activityResumed:pid=");
            stringBuilder.append(pid);
            stringBuilder.append(", uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", component=");
            stringBuilder.append(componentName);
            HwHiStreamUtils.logD(stringBuilder.toString());
            String className = componentName != null ? componentName.getClassName() : "";
            String packageName = componentName != null ? componentName.getPackageName() : "";
            HwAPPQoEAPKConfig mHwAPPQoEAPKConfig = null;
            if (HwHiStreamContentAware.this.mHwAPPQoEResourceManger != null) {
                mHwAPPQoEAPKConfig = HwHiStreamContentAware.this.mHwAPPQoEResourceManger.checkIsMonitorVideoScence(packageName, className);
            }
            if (mHwAPPQoEAPKConfig == null) {
                HwHiStreamUtils.logD("mHwAPPQoEAPKConfig is null");
                HwHiStreamContentAware.this.mTopActivity = -1;
                if ((101 != HwHiStreamContentAware.this.mCurrWeChatState || true == HwHiStreamContentAware.this.mIsDouYinMonitering) && !HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                    HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                }
                return;
            }
            if (HwAPPQoEUtils.SCENE_AUDIO == mHwAPPQoEAPKConfig.mScenceId) {
                HwHiStreamContentAware.this.mTopActivity = 1;
                HwHiStreamContentAware.this.mWechatAudioStateInfo = HwHiStreamContentAware.this.getAPPStateInfo(mHwAPPQoEAPKConfig, uid);
                HwHiStreamContentAware.this.mWechatVideoStateInfo = HwHiStreamContentAware.this.getWechatVideoStateInfo(HwHiStreamContentAware.this.mWechatAudioStateInfo);
                if (!HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                    HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                }
            } else if (HwAPPQoEUtils.SCENE_DOUYIN == mHwAPPQoEAPKConfig.mScenceId) {
                HwHiStreamContentAware.this.mTopActivity = 2;
                HwHiStreamContentAware.this.mDouyinStateInfo = HwHiStreamContentAware.this.getAPPStateInfo(mHwAPPQoEAPKConfig, uid);
                if (!HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                    HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                }
            } else if (101 != HwHiStreamContentAware.this.mCurrWeChatState || true == HwHiStreamContentAware.this.mIsDouYinMonitering) {
                HwHiStreamContentAware.this.mTopActivity = -1;
                if (!HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                    HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                }
            }
        }

        public void activityPaused(int pid, int uid, ComponentName componentName) throws RemoteException {
        }
    };
    private int mAudioRecordState = 1;
    private AvailabilityCallback mAvailabilityCallback = new AvailabilityCallback() {
        public void onCameraAvailable(String cameraId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCameraAvailable cameraId = ");
            stringBuilder.append(cameraId);
            HwHiStreamUtils.logD(stringBuilder.toString());
            if (cameraId != null && cameraId.equals(HwHiStreamContentAware.this.mCameraId)) {
                HwHiStreamContentAware.this.mCameraId = "none";
                HwHiStreamContentAware.this.mIsUpgradeVideo = false;
                if (101 != HwHiStreamContentAware.this.mCurrWeChatState && 2 == HwHiStreamContentAware.this.mCurrWeChatType && !HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                    HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessageDelayed(1, 500);
                }
            }
        }

        public void onCameraUnavailable(String cameraId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCameraUnavailable cameraId = ");
            stringBuilder.append(cameraId);
            HwHiStreamUtils.logD(stringBuilder.toString());
            if (cameraId != null) {
                HwHiStreamContentAware.this.mCameraId = cameraId;
                if (1 == HwHiStreamContentAware.this.mTopActivity && 1 == HwHiStreamContentAware.this.mCurrWeChatType && true == HwHiStreamContentAware.this.mIsScreenOn) {
                    HwHiStreamContentAware.this.mIsUpgradeVideo = true;
                    if (!HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                        HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                    }
                }
            }
        }
    };
    private String mCameraId = "none";
    private CameraManager mCameraManager;
    private Handler mContentAwareHandler;
    private Context mContext;
    private int mCurrWeChatState = 101;
    private int mCurrWeChatType = -1;
    private HwAPPStateInfo mDouyinStateInfo = null;
    private IHwHiStreamCallback mHiStreamCallback;
    private HwAPPQoEResourceManger mHwAPPQoEResourceManger;
    private boolean mIsDouYinMonitering = false;
    private boolean mIsScreenOn = true;
    private boolean mIsUpgradeVideo = false;
    private long mLastNotityTime = 0;
    private DynamicReceiver mReceiver;
    private int mTopActivity = -1;
    private HwAPPStateInfo mWechatAudioStateInfo = null;
    private HwAPPStateInfo mWechatVideoStateInfo = null;

    class DynamicReceiver extends BroadcastReceiver {
        DynamicReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                String packageName = intent.getStringExtra(HwHiStreamContentAware.INTENT_PACKAGENAME);
                int state = intent.getIntExtra(HwHiStreamContentAware.INTENT_STATE, 0);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("--->Intent received, action=");
                stringBuilder.append(action);
                stringBuilder.append(", packageName ");
                stringBuilder.append(packageName);
                stringBuilder.append(",state");
                stringBuilder.append(state);
                stringBuilder.append(", mCurrWeChatState");
                stringBuilder.append(HwHiStreamContentAware.this.mCurrWeChatState);
                HwHiStreamUtils.logD(stringBuilder.toString());
                if (HwHiStreamContentAware.ACTION_AUDIO_RECORD_STATE_CHANGED.equals(action) && packageName != null && HwHiStreamContentAware.WECHAT_NAME.equals(packageName)) {
                    if (1 == state && 3 == HwHiStreamContentAware.this.mAudioRecordState) {
                        HwHiStreamContentAware.this.mAudioRecordState = 1;
                        if (!HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                            HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                        }
                    } else if (3 == state) {
                        HwHiStreamContentAware.this.mAudioRecordState = 3;
                        if (!HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                            HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                        }
                    }
                } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                    HwHiStreamContentAware.this.mIsScreenOn = true;
                    if (1 == HwHiStreamContentAware.this.mTopActivity && !HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                        HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                    }
                } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    HwHiStreamContentAware.this.mIsScreenOn = false;
                    if (1 == HwHiStreamContentAware.this.mTopActivity && !HwHiStreamContentAware.this.mContentAwareHandler.hasMessages(1)) {
                        HwHiStreamContentAware.this.mContentAwareHandler.sendEmptyMessage(1);
                    }
                }
            }
        }
    }

    private HwHiStreamContentAware(Context context, IHwHiStreamCallback callback, Handler handler) {
        this.mContext = context;
        this.mHiStreamCallback = callback;
        this.mContentAwareHandler = handler;
        this.mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mCameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, null);
        this.mActivityManagerService = HwActivityManagerService.self();
        if (this.mActivityManagerService != null) {
            this.mActivityManagerService.registerActivityObserver(this.mActivityObserver);
        }
        this.mReceiver = new DynamicReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_AUDIO_RECORD_STATE_CHANGED);
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    public static HwHiStreamContentAware createInstance(Context context, IHwHiStreamCallback callback, Handler handler) {
        if (mHwHiStreamContentAware == null) {
            mHwHiStreamContentAware = new HwHiStreamContentAware(context, callback, handler);
        }
        return mHwHiStreamContentAware;
    }

    public static HwHiStreamContentAware getInstance() {
        return mHwHiStreamContentAware;
    }

    private boolean isWechatCallEndOrDegrade() {
        if (101 == this.mCurrWeChatState || ((isCameraOn() || 2 != this.mCurrWeChatType || 1 != this.mTopActivity || true != this.mIsScreenOn) && 1 != this.mAudioRecordState)) {
            return false;
        }
        return true;
    }

    private boolean isWechatCallUpgrade() {
        if ((100 != this.mCurrWeChatState && 103 != this.mCurrWeChatState) || 1 != this.mCurrWeChatType || true != this.mIsScreenOn || true != this.mIsUpgradeVideo) {
            return false;
        }
        this.mIsUpgradeVideo = false;
        return true;
    }

    public void handleAppMonotor() {
        HwHiStreamUtils.logD("handleAppMonotor enter");
        Bundle bundle = new Bundle();
        StringBuilder stringBuilder;
        if (isWechatCallEndOrDegrade() || isWechatCallUpgrade()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("-----notify wechat call end  mCurrWeChatType= ");
            stringBuilder.append(this.mCurrWeChatType);
            HwHiStreamUtils.logD(stringBuilder.toString());
            bundle.putInt("appScene", this.mCurrWeChatType);
            bundle.putInt("appState", 101);
            this.mContentAwareHandler.sendMessage(this.mContentAwareHandler.obtainMessage(2, bundle));
            if (!this.mContentAwareHandler.hasMessages(1)) {
                this.mContentAwareHandler.sendEmptyMessageDelayed(1, 1500);
            }
            return;
        }
        if (2 == this.mTopActivity) {
            if (!this.mIsDouYinMonitering) {
                HwHiStreamUtils.logD("-----notify DOUYIN START ");
                bundle.putInt("appScene", 3);
                bundle.putInt("appState", 100);
                this.mContentAwareHandler.sendMessage(this.mContentAwareHandler.obtainMessage(2, bundle));
            }
        } else if (1 == this.mTopActivity) {
            if (true == this.mIsDouYinMonitering) {
                HwHiStreamUtils.logD("-----notify DOUYIN END ");
                bundle.putInt("appScene", 3);
                bundle.putInt("appState", 101);
                this.mContentAwareHandler.sendMessage(this.mContentAwareHandler.obtainMessage(2, bundle));
                if (!this.mContentAwareHandler.hasMessages(1)) {
                    this.mContentAwareHandler.sendEmptyMessageDelayed(1, 1500);
                }
            } else if (101 == this.mCurrWeChatState && 3 == this.mAudioRecordState) {
                int appScene = 1;
                if (isCameraOn()) {
                    appScene = 2;
                    if (!this.mIsScreenOn) {
                        return;
                    }
                }
                this.mCurrWeChatType = appScene;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("-----notify wechat call start appscene = ");
                stringBuilder2.append(appScene);
                stringBuilder2.append("-----");
                HwHiStreamUtils.logD(stringBuilder2.toString());
                bundle.putInt("appScene", appScene);
                bundle.putInt("appState", 100);
                this.mContentAwareHandler.sendMessage(this.mContentAwareHandler.obtainMessage(2, bundle));
            } else if (104 == this.mCurrWeChatState) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("-----notify wechat forground ,mCurrWeChatType= ");
                stringBuilder.append(this.mCurrWeChatType);
                stringBuilder.append("-----");
                HwHiStreamUtils.logD(stringBuilder.toString());
                bundle.putInt("appScene", this.mCurrWeChatType);
                bundle.putInt("appState", 103);
                this.mContentAwareHandler.sendMessage(this.mContentAwareHandler.obtainMessage(2, bundle));
            }
        }
        if (-1 == this.mTopActivity || !this.mIsScreenOn) {
            if (true == this.mIsDouYinMonitering && -1 == this.mTopActivity) {
                HwHiStreamUtils.logD("-----notify DOUYIN END ");
                bundle.putInt("appScene", 3);
                bundle.putInt("appState", 101);
                this.mContentAwareHandler.sendMessage(this.mContentAwareHandler.obtainMessage(2, bundle));
                if (!this.mContentAwareHandler.hasMessages(1)) {
                    this.mContentAwareHandler.sendEmptyMessageDelayed(1, 1500);
                }
            } else if ((103 == this.mCurrWeChatState || 100 == this.mCurrWeChatState) && 3 == this.mAudioRecordState && !(1 == this.mCurrWeChatType && 1 == this.mTopActivity)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("-----notify wechat background ,mCurrWeChatType=");
                stringBuilder.append(this.mCurrWeChatType);
                stringBuilder.append("-----");
                HwHiStreamUtils.logD(stringBuilder.toString());
                bundle.putInt("appScene", this.mCurrWeChatType);
                bundle.putInt("appState", 104);
                this.mContentAwareHandler.sendMessage(this.mContentAwareHandler.obtainMessage(2, bundle));
            }
        }
    }

    public void handleNotifyAppStateChange(Message msg) {
        if (msg != null && msg.obj != null) {
            Bundle bundle = msg.obj;
            int appScene = bundle.getInt("appScene");
            int appState = bundle.getInt("appState");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleNotifyAppStateChange appScene=");
            stringBuilder.append(appScene);
            stringBuilder.append(",appState");
            stringBuilder.append(appState);
            HwHiStreamUtils.logD(stringBuilder.toString());
            HwAPPStateInfo curStateInfo = null;
            if (-1 != appScene) {
                long currTime = System.currentTimeMillis();
                if (1500 > currTime - this.mLastNotityTime) {
                    HwHiStreamUtils.logD("NOTIFY interval is too short,delay 1s");
                    if (!this.mContentAwareHandler.hasMessages(1)) {
                        this.mContentAwareHandler.sendEmptyMessageDelayed(1, 1500 - (currTime - this.mLastNotityTime));
                    }
                } else {
                    if (1 == appScene || 2 == appScene) {
                        this.mCurrWeChatState = appState;
                        curStateInfo = 1 == appScene ? this.mWechatAudioStateInfo : this.mWechatVideoStateInfo;
                    } else if (3 == appScene) {
                        curStateInfo = this.mDouyinStateInfo;
                        if (101 == appState) {
                            this.mIsDouYinMonitering = false;
                        } else {
                            this.mIsDouYinMonitering = true;
                        }
                    }
                    if (this.mHiStreamCallback != null) {
                        this.mHiStreamCallback.onAPPStateChangeCallback(curStateInfo, appState);
                    }
                    this.mLastNotityTime = System.currentTimeMillis();
                }
            }
        }
    }

    public HwAPPStateInfo getCurAPPStateInfo(int appSceneId) {
        if (HwAPPQoEUtils.SCENE_AUDIO == appSceneId) {
            return this.mWechatAudioStateInfo;
        }
        if (HwAPPQoEUtils.SCENE_VIDEO == appSceneId) {
            return this.mWechatVideoStateInfo;
        }
        if (HwAPPQoEUtils.SCENE_DOUYIN == appSceneId) {
            return this.mDouyinStateInfo;
        }
        return null;
    }

    public void onNodataDetected() {
        HwHiStreamUtils.logD("No data,call end");
        this.mAudioRecordState = 1;
        if (!this.mContentAwareHandler.hasMessages(1)) {
            this.mContentAwareHandler.sendEmptyMessage(1);
        }
    }

    private HwAPPStateInfo getAPPStateInfo(HwAPPQoEAPKConfig mHwAPPQoEAPKConfig, int uid) {
        if (mHwAPPQoEAPKConfig == null) {
            return null;
        }
        HwAPPStateInfo mAPPStateInfo = new HwAPPStateInfo();
        mAPPStateInfo.mAppId = mHwAPPQoEAPKConfig.mAppId;
        mAPPStateInfo.mScenceId = mHwAPPQoEAPKConfig.mScenceId;
        mAPPStateInfo.mAppType = HwAPPQoEUtils.APP_TYPE_STREAMING;
        mAPPStateInfo.mAppUID = uid;
        MpLinkQuickSwitchConfiguration switchConfiguration = new MpLinkQuickSwitchConfiguration();
        if (HwAPPQoEUtils.SCENE_DOUYIN == mHwAPPQoEAPKConfig.mScenceId) {
            switchConfiguration.setNetworkStrategy(0);
            switchConfiguration.setSocketStrategy(3);
        } else {
            switchConfiguration.setNetworkStrategy(0);
            switchConfiguration.setSocketStrategy(3);
        }
        mAPPStateInfo.setMpLinkQuickSwitchConfiguration(switchConfiguration);
        return mAPPStateInfo;
    }

    private HwAPPStateInfo getWechatVideoStateInfo(HwAPPStateInfo mStateInfo) {
        if (mStateInfo == null) {
            return null;
        }
        HwAPPStateInfo videoStateInfo = new HwAPPStateInfo();
        videoStateInfo.mAppId = mStateInfo.mAppId;
        videoStateInfo.mScenceId = mStateInfo.mScenceId + 1;
        videoStateInfo.mAppType = HwAPPQoEUtils.APP_TYPE_STREAMING;
        videoStateInfo.mAppUID = mStateInfo.mAppUID;
        videoStateInfo.setMpLinkQuickSwitchConfiguration(mStateInfo.getQuickSwitchConfiguration());
        return videoStateInfo;
    }

    private boolean isCameraOn() {
        if (this.mCameraId == null || this.mCameraId.equals("none")) {
            return false;
        }
        return true;
    }

    public HwAPPStateInfo getCurStreamAppInfo() {
        if (this.mIsDouYinMonitering) {
            return this.mDouyinStateInfo;
        }
        if (101 == this.mCurrWeChatState) {
            return null;
        }
        if (2 == this.mCurrWeChatType) {
            return this.mWechatVideoStateInfo;
        }
        if (1 == this.mCurrWeChatType) {
            return this.mWechatAudioStateInfo;
        }
        return null;
    }
}
