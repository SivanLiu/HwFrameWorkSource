package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.os.Handler;
import com.android.internal.telephony.CommandsInterface;

public class HwFullNetworkCheckStateFactory {
    private static HwFullNetworkCheckStateBase mCheckStateBaseInstance;
    private static final Object mLock = new Object();

    public static HwFullNetworkCheckStateBase getHwFullNetworkCheckState(Context c, CommandsInterface[] ci, Handler h) {
        HwFullNetworkCheckStateBase hwFullNetworkCheckStateBase;
        synchronized (mLock) {
            if (mCheckStateBaseInstance == null) {
                if (!HwFullNetworkConfig.isHisiPlatform()) {
                    mCheckStateBaseInstance = new HwFullNetworkCheckStateQcom2_0(c, ci, h);
                } else if (HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT) {
                    mCheckStateBaseInstance = new HwFullNetworkCheckStateHisi2_0(c, ci, h);
                } else if (HwFullNetworkConfig.IS_FULL_NETWORK_SUPPORTED_IN_HISI) {
                    mCheckStateBaseInstance = new HwFullNetworkCheckStateHisi1_0(c, ci, h);
                }
            }
            hwFullNetworkCheckStateBase = mCheckStateBaseInstance;
        }
        return hwFullNetworkCheckStateBase;
    }
}
