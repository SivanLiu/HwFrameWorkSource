package com.android.server.hidata.wavemapping.dataprovider;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.android.server.hidata.appqoe.HwAPPStateInfo;
import com.android.server.hidata.appqoe.IHwAPPQoECallback;
import com.android.server.hidata.wavemapping.entity.HwWmpAppInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;

public class HwWmpCallbackImpl implements IHwAPPQoECallback {
    private static final String TAG = "HwWmpCallbackImpl";
    private static HwWmpCallbackImpl mHwWmpCallbackImpl;
    private HwAPPStateInfo appInfoSsaved = new HwAPPStateInfo();
    private Handler mStateMachineHandler;

    private HwWmpCallbackImpl(Handler handler) {
        this.mStateMachineHandler = handler;
        LogUtil.i("init HwWmpCallbackImpl completed!");
    }

    public static HwWmpCallbackImpl getInstance(Handler handler) {
        if (mHwWmpCallbackImpl == null) {
            mHwWmpCallbackImpl = new HwWmpCallbackImpl(handler);
        }
        return mHwWmpCallbackImpl;
    }

    public void onAPPStateCallBack(HwAPPStateInfo appInfo, int state) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onAppStateCallBack appinfo:");
        stringBuilder.append(appInfo.toString());
        stringBuilder.append(", state:");
        stringBuilder.append(state);
        LogUtil.i(stringBuilder.toString());
        HwWmpAppInfo mAppInfo = new HwWmpAppInfo(appInfo.mAppId, appInfo.mScenceId, appInfo.mAppUID, appInfo.mAppType, appInfo.mAppState, appInfo.mNetworkType);
        StringBuilder stringBuilder2;
        if (mAppInfo.isMonitorApp()) {
            StringBuilder stringBuilder3;
            Message msg;
            switch (state) {
                case 100:
                case 102:
                case 103:
                    if (this.appInfoSsaved.mAppId != appInfo.mAppId || this.appInfoSsaved.mScenceId != appInfo.mScenceId || this.appInfoSsaved.mAppUID != appInfo.mAppUID || this.appInfoSsaved.mAppType != appInfo.mAppType || this.appInfoSsaved.mAppState != appInfo.mAppState || this.appInfoSsaved.mNetworkType == appInfo.mNetworkType) {
                        if (!((this.appInfoSsaved.mAppId == appInfo.mAppId && this.appInfoSsaved.mScenceId == appInfo.mScenceId && this.appInfoSsaved.mAppUID == appInfo.mAppUID) || appInfo.mScenceId <= 0 || -1 == this.appInfoSsaved.mAppUID)) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(" App or Scence changed, code=");
                            stringBuilder3.append(this.appInfoSsaved.mScenceId);
                            stringBuilder3.append(", net=");
                            stringBuilder3.append(this.appInfoSsaved.mNetworkType);
                            LogUtil.i(stringBuilder3.toString());
                            HwWmpAppInfo mSavedAppInfo = new HwWmpAppInfo(this.appInfoSsaved.mAppId, this.appInfoSsaved.mScenceId, this.appInfoSsaved.mAppUID, this.appInfoSsaved.mAppType, this.appInfoSsaved.mAppState, this.appInfoSsaved.mNetworkType);
                            if (mSavedAppInfo.isMonitorApp()) {
                                msg = Message.obtain(this.mStateMachineHandler, 201);
                                msg.obj = mSavedAppInfo;
                                this.mStateMachineHandler.sendMessage(msg);
                            }
                        }
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(" App or Scence Start, code=");
                        stringBuilder2.append(appInfo.mScenceId);
                        stringBuilder2.append(", net=");
                        stringBuilder2.append(appInfo.mNetworkType);
                        LogUtil.i(stringBuilder2.toString());
                        msg = Message.obtain(this.mStateMachineHandler, 200);
                        msg.obj = mAppInfo;
                        this.mStateMachineHandler.sendMessage(msg);
                        this.appInfoSsaved.copyObjectValue(appInfo);
                        break;
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" only Network changed, code=");
                    stringBuilder2.append(appInfo.mScenceId);
                    stringBuilder2.append(", net=");
                    stringBuilder2.append(appInfo.mNetworkType);
                    LogUtil.i(stringBuilder2.toString());
                    msg = Message.obtain(this.mStateMachineHandler, 202);
                    msg.obj = mAppInfo;
                    this.mStateMachineHandler.sendMessage(msg);
                    break;
                    break;
                default:
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" App or Scence closed, code=");
                    stringBuilder3.append(appInfo.mScenceId);
                    stringBuilder3.append(", net=");
                    stringBuilder3.append(appInfo.mNetworkType);
                    LogUtil.i(stringBuilder3.toString());
                    msg = Message.obtain(this.mStateMachineHandler, 201);
                    msg.obj = mAppInfo;
                    this.mStateMachineHandler.sendMessage(msg);
                    this.appInfoSsaved = new HwAPPStateInfo();
                    break;
            }
            return;
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" app is not in checking list, ScenceId=");
        stringBuilder2.append(appInfo.mScenceId);
        LogUtil.d(stringBuilder2.toString());
    }

    public void onAPPQualityCallBack(HwAPPStateInfo appInfo, int experience) {
        LogUtil.i("onAPPQualityCallBack");
        HwWmpAppInfo mAppInfo = new HwWmpAppInfo(appInfo.mAppId, appInfo.mScenceId, appInfo.mAppUID, appInfo.mAppType, appInfo.mAppState, appInfo.mNetworkType);
        if (mAppInfo.isMonitorApp()) {
            int qoe;
            Message msg;
            Bundle bundle;
            StringBuilder stringBuilder;
            String appName = mAppInfo.getAppName();
            switch (experience) {
                case 106:
                    qoe = 2;
                    break;
                case 107:
                    qoe = 1;
                    msg = Message.obtain(this.mStateMachineHandler, 211);
                    bundle = new Bundle();
                    bundle.putString("APPNAME", appName);
                    msg.setData(bundle);
                    this.mStateMachineHandler.sendMessageDelayed(msg, 15000);
                    break;
                default:
                    qoe = -1;
                    break;
            }
            if (this.appInfoSsaved.mAppId == appInfo.mAppId && this.appInfoSsaved.mScenceId == appInfo.mScenceId && this.appInfoSsaved.mAppUID == appInfo.mAppUID && this.appInfoSsaved.mAppType == appInfo.mAppType && this.appInfoSsaved.mAppState == appInfo.mAppState && this.appInfoSsaved.mNetworkType != appInfo.mNetworkType) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" only Network changed, code=");
                stringBuilder.append(appInfo.mScenceId);
                LogUtil.i(stringBuilder.toString());
                msg = Message.obtain(this.mStateMachineHandler, 202);
                msg.obj = mAppInfo;
                this.mStateMachineHandler.sendMessage(msg);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(" QoE report, code=");
            stringBuilder.append(appInfo.mScenceId);
            stringBuilder.append(" qoe=");
            stringBuilder.append(qoe);
            LogUtil.i(stringBuilder.toString());
            msg = Message.obtain(this.mStateMachineHandler, 210);
            bundle = new Bundle();
            bundle.putInt("QOE", qoe);
            bundle.putString("APPNAME", appName);
            msg.setData(bundle);
            this.mStateMachineHandler.sendMessage(msg);
            this.appInfoSsaved.copyObjectValue(appInfo);
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" app is not in checking list, ScenceId=");
        stringBuilder2.append(appInfo.mScenceId);
        LogUtil.d(stringBuilder2.toString());
    }

    public void onNetworkQualityCallBack(int UID, int sense, int network, boolean isSatisfy) {
        LogUtil.d("onNetworkQualityCallBack, not used");
    }

    public void onAPPRttInfoCallBack(HwAPPStateInfo info) {
        LogUtil.d("onAPPRttInfoCallBack, not used");
    }

    public void onWifiLinkQuality(int UID, int scence, boolean isSatisfy) {
        LogUtil.i("onWifiLinkQuality, not used");
    }
}
