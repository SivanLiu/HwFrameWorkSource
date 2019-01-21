package com.huawei.android.telephony;

import android.telephony.SignalStrength;

public class SignalStrengthEx {
    public static int getLteRsrp(SignalStrength signal) {
        if (signal != null) {
            return signal.getLteRsrp();
        }
        return Integer.MAX_VALUE;
    }

    public static int getWcdmaRscp(SignalStrength signal) {
        if (signal != null) {
            return signal.getWcdmaRscp();
        }
        return Integer.MAX_VALUE;
    }

    public static int getCdmaLevel(SignalStrength signal) {
        if (signal != null) {
            signal.getCdmaLevel();
        }
        return 0;
    }
}
