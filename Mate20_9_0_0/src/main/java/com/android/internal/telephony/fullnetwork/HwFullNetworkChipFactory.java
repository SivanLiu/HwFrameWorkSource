package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import com.android.internal.telephony.CommandsInterface;

public class HwFullNetworkChipFactory {
    private static HwFullNetworkChipCommon mChipCommon;
    private static HwFullNetworkChipHisi mChipHisi;
    private static HwFullNetworkChipQcom mChipQcom;
    private static final Object mLock = new Object();

    static void make(Context c, CommandsInterface[] ci) {
        synchronized (mLock) {
            if (mChipCommon == null) {
                mChipCommon = HwFullNetworkChipCommon.make(c, ci);
            }
            if (HwFullNetworkConfig.isHisiPlatform()) {
                if (mChipHisi == null) {
                    mChipHisi = HwFullNetworkChipHisi.make(c, ci);
                }
            } else if (mChipQcom == null) {
                mChipQcom = HwFullNetworkChipQcom.make(c, ci);
            }
        }
    }

    static HwFullNetworkChipCommon getChipCommon() {
        HwFullNetworkChipCommon hwFullNetworkChipCommon;
        synchronized (mLock) {
            if (mChipCommon != null) {
                hwFullNetworkChipCommon = mChipCommon;
            } else {
                throw new RuntimeException("HwFullNetworkChipCommon Instance can't be called before make()");
            }
        }
        return hwFullNetworkChipCommon;
    }
}
