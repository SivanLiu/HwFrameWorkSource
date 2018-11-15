package com.android.server.wifi;

import android.content.Context;

public class HwCustWifiNetworkSelector {
    public boolean isShouldRegisterBroadcast(Context context) {
        return false;
    }

    public boolean isShouldDisableHs20BySimState(Context context) {
        return false;
    }

    public void registerSimCardStateReceiver(Context context) {
    }
}
