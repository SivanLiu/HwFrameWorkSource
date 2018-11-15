package com.android.server.hidata.wavemapping.service;

import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.appqoe.HwAPPQoEAPKConfig;
import com.android.server.hidata.appqoe.HwAPPQoEGameConfig;
import com.android.server.hidata.appqoe.HwAPPQoEResourceManger;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.HashMap;
import java.util.List;

public class HwHistoryQoEResourceBuilder {
    private static String TAG;
    private static HwHistoryQoEResourceBuilder mHwHistoryQoEResourceBuilder;
    private HwAPPQoEResourceManger mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();
    private HashMap<Integer, Float> mMonitorAppList = new HashMap();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(HwHistoryQoEResourceBuilder.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public HwHistoryQoEResourceBuilder() {
        buildHistoryQoEAppList();
    }

    public static synchronized HwHistoryQoEResourceBuilder getInstance() {
        HwHistoryQoEResourceBuilder hwHistoryQoEResourceBuilder;
        synchronized (HwHistoryQoEResourceBuilder.class) {
            if (mHwHistoryQoEResourceBuilder == null) {
                mHwHistoryQoEResourceBuilder = new HwHistoryQoEResourceBuilder();
            }
            hwHistoryQoEResourceBuilder = mHwHistoryQoEResourceBuilder;
        }
        return hwHistoryQoEResourceBuilder;
    }

    private void buildHistoryQoEAppList() {
        int i;
        int appId;
        StringBuilder stringBuilder;
        LogUtil.i("buildHistoryQoEAppList");
        List<HwAPPQoEAPKConfig> mAPPconfigList = this.mHwAPPQoEResourceManger.getAPKConfigList();
        List<HwAPPQoEGameConfig> mGameconfigList = this.mHwAPPQoEResourceManger.getGameConfigList();
        int i2 = 0;
        for (i = 0; i < mAPPconfigList.size(); i++) {
            appId = ((HwAPPQoEAPKConfig) mAPPconfigList.get(i)).mAppId;
            int scenesId = ((HwAPPQoEAPKConfig) mAPPconfigList.get(i)).mScenceId;
            float threshold = ((HwAPPQoEAPKConfig) mAPPconfigList.get(i)).mHistoryQoeBadTH;
            if (threshold > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                int fullId = scenesId;
                if (!this.mMonitorAppList.containsKey(Integer.valueOf(fullId))) {
                    this.mMonitorAppList.put(Integer.valueOf(fullId), Float.valueOf(threshold));
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" add Common APP:");
                    stringBuilder.append(fullId);
                    stringBuilder.append(", AppId=");
                    stringBuilder.append(appId);
                    stringBuilder.append(", ScenceId= ");
                    stringBuilder.append(scenesId);
                    stringBuilder.append(", BadTH=");
                    stringBuilder.append(threshold);
                    LogUtil.d(stringBuilder.toString());
                }
            }
        }
        while (i2 < mGameconfigList.size()) {
            i = ((HwAPPQoEGameConfig) mGameconfigList.get(i2)).mGameId;
            appId = ((HwAPPQoEGameConfig) mGameconfigList.get(i2)).mScenceId;
            float threshold2 = ((HwAPPQoEGameConfig) mGameconfigList.get(i2)).mHistoryQoeBadTH;
            if (threshold2 > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                int fullId2 = Constant.transferGameId2FullId(i, appId);
                if (!this.mMonitorAppList.containsKey(Integer.valueOf(fullId2))) {
                    this.mMonitorAppList.put(Integer.valueOf(fullId2), Float.valueOf(threshold2));
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" add Game APP:");
                    stringBuilder.append(fullId2);
                    stringBuilder.append(", GameId=");
                    stringBuilder.append(i);
                    stringBuilder.append(", ScenceId=");
                    stringBuilder.append(appId);
                    stringBuilder.append(", BadTH=");
                    stringBuilder.append(threshold2);
                    LogUtil.d(stringBuilder.toString());
                }
            }
            i2++;
        }
        Constant.setSavedQoeAppList(this.mMonitorAppList);
    }

    public HashMap<Integer, Float> getQoEAppList() {
        return (HashMap) this.mMonitorAppList.clone();
    }
}
