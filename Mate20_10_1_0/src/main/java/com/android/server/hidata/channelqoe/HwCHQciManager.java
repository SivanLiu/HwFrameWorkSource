package com.android.server.hidata.channelqoe;

import android.util.wifi.HwHiLog;
import java.util.ArrayList;
import java.util.List;

public class HwCHQciManager {
    private static final String TAG = "HiDATA_ChannelQoE_QciManager";
    private static HwCHQciManager mCHQciManager;
    private List<HwCHQciConfig> mConfigList = new ArrayList();

    private HwCHQciManager() {
    }

    public static void log(boolean isFmtStrPrivate, String info, Object... args) {
        HwHiLog.e(TAG, isFmtStrPrivate, info, args);
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
        for (HwCHQciConfig config : this.mConfigList) {
            if (config.mQci == qci) {
                log(false, "find QCI, RTT is %{public}d CHLOAD is %{public}d", Integer.valueOf(config.mRtt), Integer.valueOf(config.mChload));
                return config;
            }
        }
        log(false, "Couldn't find QCI %{public}d", Integer.valueOf(qci));
        return HwCHQciConfig.getDefalultQci();
    }
}
