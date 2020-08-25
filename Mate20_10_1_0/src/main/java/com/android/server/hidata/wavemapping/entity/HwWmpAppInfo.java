package com.android.server.hidata.wavemapping.entity;

import com.android.server.hidata.appqoe.HwAPPStateInfo;
import com.android.server.hidata.wavemapping.cons.Constant;

public class HwWmpAppInfo extends HwAPPStateInfo {
    private boolean isMonitorApp;
    private String mAppName;
    private int mBadThreshold;
    private int mFullId;
    private long mStartTime;

    public HwWmpAppInfo(int appId, int scenesId, int appUid, int appType, int appState, int networkType) {
        ((HwAPPStateInfo) this).mAppId = appId;
        ((HwAPPStateInfo) this).mScenceId = scenesId;
        ((HwAPPStateInfo) this).mAppUID = appUid;
        ((HwAPPStateInfo) this).mAppType = appType;
        ((HwAPPStateInfo) this).mAppState = appState;
        ((HwAPPStateInfo) this).mNetworkType = networkType;
        if (networkType == 2000) {
            this.mFullId = Constant.transferGameId2FullId(appId, scenesId);
        } else {
            this.mFullId = scenesId;
        }
        if (Constant.getSavedQoeAppList().containsKey(Integer.valueOf(this.mFullId))) {
            this.mAppName = Constant.USERDB_APP_NAME_PREFIX + this.mFullId;
            this.isMonitorApp = true;
        } else {
            this.mAppName = Constant.USERDB_APP_NAME_NONE;
            this.isMonitorApp = false;
        }
        this.mStartTime = 0;
    }

    public HwWmpAppInfo(int networkFakeApp) {
        if (networkFakeApp == 1) {
            this.mAppName = Constant.USERDB_APP_NAME_WIFI;
            ((HwAPPStateInfo) this).mNetworkType = 800;
            this.isMonitorApp = true;
        } else if (networkFakeApp == 0) {
            this.mAppName = Constant.USERDB_APP_NAME_MOBILE;
            ((HwAPPStateInfo) this).mNetworkType = 801;
            this.isMonitorApp = true;
        } else {
            this.mAppName = Constant.USERDB_APP_NAME_NONE;
            ((HwAPPStateInfo) this).mNetworkType = 802;
            this.isMonitorApp = false;
        }
        this.mStartTime = 0;
    }

    public void setStartTime(long time) {
        this.mStartTime = time;
    }

    public long getStartTime() {
        return this.mStartTime;
    }

    public int getScenceId() {
        return ((HwAPPStateInfo) this).mScenceId;
    }

    public int getAppUid() {
        return ((HwAPPStateInfo) this).mAppUID;
    }

    public boolean isNormalApp() {
        if (((HwAPPStateInfo) this).mAppId == -1) {
            return false;
        }
        return true;
    }

    public boolean isMonitorApp() {
        return this.isMonitorApp;
    }

    public String getAppName() {
        return this.mAppName;
    }

    public int getAppFullId() {
        return this.mFullId;
    }

    public int getConMgrNetworkType() {
        if (((HwAPPStateInfo) this).mNetworkType == 800) {
            return 1;
        }
        if (((HwAPPStateInfo) this).mNetworkType == 801) {
            return 0;
        }
        return 8;
    }

    public void setConMgrNetworkType(int net) {
        if (net == 1) {
            ((HwAPPStateInfo) this).mNetworkType = 800;
        } else if (net == 0) {
            ((HwAPPStateInfo) this).mNetworkType = 801;
        } else {
            ((HwAPPStateInfo) this).mNetworkType = 802;
        }
    }

    public void copyObjectValue(HwWmpAppInfo tempAppState) {
        if (tempAppState != null) {
            ((HwAPPStateInfo) this).mAppId = tempAppState.mAppId;
            ((HwAPPStateInfo) this).mScenceId = tempAppState.mScenceId;
            ((HwAPPStateInfo) this).mAction = tempAppState.mAction;
            ((HwAPPStateInfo) this).mAppUID = tempAppState.mAppUID;
            ((HwAPPStateInfo) this).mAppType = tempAppState.mAppType;
            ((HwAPPStateInfo) this).mAppState = tempAppState.mAppState;
            ((HwAPPStateInfo) this).mAppRTT = tempAppState.mAppRTT;
            ((HwAPPStateInfo) this).mNetworkType = tempAppState.mNetworkType;
            ((HwAPPStateInfo) this).mUserType = tempAppState.mUserType;
            this.mAppName = tempAppState.mAppName;
            this.mStartTime = tempAppState.mStartTime;
        }
    }

    @Override // com.android.server.hidata.appqoe.HwAPPStateInfo
    public String toString() {
        return "HwWmpAppInfo - mAppName:" + this.mAppName + " mAppId:" + ((HwAPPStateInfo) this).mAppId + ", mScenceId:" + ((HwAPPStateInfo) this).mScenceId + ", mAppUID:" + ((HwAPPStateInfo) this).mAppUID + ", mAppType:" + ((HwAPPStateInfo) this).mAppType + ", mAppState:" + ((HwAPPStateInfo) this).mAppState + ", mNetworkType:" + ((HwAPPStateInfo) this).mNetworkType + ", mStartTime:" + this.mStartTime;
    }
}
