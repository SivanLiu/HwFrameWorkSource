package com.android.server.hidata.histream;

import com.android.server.hidata.appqoe.HwAPPQoEUserAction;

class HwHiStreamTrafficInfo {
    public int mCallType = -1;
    public long mCurrDay = 0;
    public int mNetType = -1;
    public String mSubId = HwAPPQoEUserAction.DEFAULT_CHIP_TYPE;
    public long mTraffic = 0;
}
