package com.android.server.hidata.appqoe;

import com.android.server.hidata.mplink.MpLinkQuickSwitchConfiguration;

public class HwAPPStateInfo {
    public int mAction = -1;
    public int mAppId = -1;
    public int mAppPeriod = -1;
    public int mAppRTT = -1;
    public int mAppState = -1;
    public int mAppType = -1;
    public int mAppUID = -1;
    private MpLinkQuickSwitchConfiguration mMpLinkQuickSwitchConfiguration = new MpLinkQuickSwitchConfiguration();
    private int mNetworkStrategy = -1;
    public int mNetworkType = -1;
    public int mScenceId = -1;
    public int mScenceType = 0;
    private int mSocketStrategy = -1;
    public int mUserType = -1;

    public void setSocketStrategy(int mSocketStrategy) {
        this.mSocketStrategy = mSocketStrategy;
        this.mMpLinkQuickSwitchConfiguration.setSocketStrategy(mSocketStrategy);
    }

    public void setNetworkStrategy(int mNetworkStrategy) {
        this.mNetworkStrategy = mNetworkStrategy;
        this.mMpLinkQuickSwitchConfiguration.setNetworkStrategy(mNetworkStrategy);
    }

    public void setMpLinkQuickSwitchConfiguration(MpLinkQuickSwitchConfiguration configuration) {
        this.mMpLinkQuickSwitchConfiguration.copyObjectValue(configuration);
        if (configuration != null) {
            this.mSocketStrategy = configuration.getSocketStrategy();
            this.mNetworkStrategy = configuration.getNetworkStrategy();
        }
    }

    public int getSocketStrategy() {
        return this.mSocketStrategy;
    }

    public int getNetworkStrategy() {
        return this.mNetworkStrategy;
    }

    public MpLinkQuickSwitchConfiguration getQuickSwitchConfiguration() {
        return this.mMpLinkQuickSwitchConfiguration;
    }

    public boolean isObjectValueEqual(HwAPPStateInfo tempAPPState) {
        if (tempAPPState != null && this.mAppId == tempAPPState.mAppId && this.mScenceId == tempAPPState.mScenceId && this.mAppUID == tempAPPState.mAppUID && this.mAppType == tempAPPState.mAppType && this.mAppState == tempAPPState.mAppState && this.mAppRTT == tempAPPState.mAppRTT && this.mNetworkType == tempAPPState.mNetworkType && this.mAction == tempAPPState.mAction && this.mUserType == tempAPPState.mUserType && this.mScenceType == tempAPPState.mScenceType && this.mSocketStrategy == tempAPPState.getSocketStrategy() && this.mNetworkStrategy == tempAPPState.getNetworkStrategy()) {
            return true;
        }
        return false;
    }

    public void copyObjectValue(HwAPPStateInfo tempAPPState) {
        if (tempAPPState != null) {
            this.mAppId = tempAPPState.mAppId;
            this.mScenceId = tempAPPState.mScenceId;
            this.mScenceType = tempAPPState.mScenceType;
            this.mAction = tempAPPState.mAction;
            this.mAppUID = tempAPPState.mAppUID;
            this.mAppPeriod = tempAPPState.mAppPeriod;
            this.mAppType = tempAPPState.mAppType;
            this.mAppState = tempAPPState.mAppState;
            this.mAppRTT = tempAPPState.mAppRTT;
            this.mNetworkType = tempAPPState.mNetworkType;
            this.mUserType = tempAPPState.mUserType;
            this.mSocketStrategy = tempAPPState.getSocketStrategy();
            this.mNetworkStrategy = tempAPPState.getNetworkStrategy();
            this.mMpLinkQuickSwitchConfiguration.copyObjectValue(tempAPPState.getQuickSwitchConfiguration());
            this.mMpLinkQuickSwitchConfiguration.setSocketStrategy(this.mSocketStrategy);
            this.mMpLinkQuickSwitchConfiguration.setNetworkStrategy(this.mNetworkStrategy);
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwAPPStateInfo  mAppId:");
        stringBuilder.append(this.mAppId);
        stringBuilder.append(", mScenceId:");
        stringBuilder.append(this.mScenceId);
        stringBuilder.append(", mScenceType:");
        stringBuilder.append(this.mScenceType);
        stringBuilder.append(", mAction");
        stringBuilder.append(this.mAction);
        stringBuilder.append(", mAppUID:");
        stringBuilder.append(this.mAppUID);
        stringBuilder.append(", mAppType:");
        stringBuilder.append(this.mAppType);
        stringBuilder.append(", mAppState:");
        stringBuilder.append(this.mAppState);
        stringBuilder.append(", mAppRTT:");
        stringBuilder.append(this.mAppRTT);
        stringBuilder.append(" ,mNetworkType: ");
        stringBuilder.append(this.mNetworkType);
        stringBuilder.append(",mUserType");
        stringBuilder.append(this.mUserType);
        stringBuilder.append(", SocketStrategy:");
        stringBuilder.append(this.mSocketStrategy);
        stringBuilder.append(" ,NetworkStrategy: ");
        stringBuilder.append(this.mNetworkStrategy);
        stringBuilder.append(" ,mAppPeriod:");
        stringBuilder.append(this.mAppPeriod);
        return stringBuilder.toString();
    }
}
