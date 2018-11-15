package com.android.server.hidata.channelqoe;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class HwCHQciManager {
    private static final String TAG = "HiDATA_ChannelQoE_QciManager";
    private static HwCHQciManager mCHQciManager;
    private List<HwCHQciConfig> mConfigList = new ArrayList();

    private HwCHQciManager() {
    }

    public static void log(String info) {
        Log.e(TAG, info);
    }

    public static HwCHQciManager getInstance() {
        if (mCHQciManager == null) {
            mCHQciManager = new HwCHQciManager();
        }
        return mCHQciManager;
    }

    public void addConfig(HwCHQciConfig config) {
        this.mConfigList.add(config);
    }

    public HwCHQciConfig getChQciConfig(int qci) {
        StringBuilder stringBuilder;
        for (HwCHQciConfig config : this.mConfigList) {
            if (config.mQci == qci) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("find QCI, RTT is ");
                stringBuilder.append(config.mRtt);
                stringBuilder.append(" CHLOAD is ");
                stringBuilder.append(config.mChload);
                log(stringBuilder.toString());
                return config;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Couldn't find QCI ");
        stringBuilder.append(qci);
        log(stringBuilder.toString());
        return HwCHQciConfig.getDefalultQci();
    }
}
