package com.android.server.location;

import android.content.Context;
import android.provider.Settings;

public class HwGpsLocationCustFeature implements IHwGpsLocationCustFeature {
    private static final String FORBIDDEN_MSA_SWTICH = "forbidden_msa_switch";
    private static final int GPS_POSITION_MODE_MS_ASSISTED = 2;
    private static final int GPS_POSITION_MODE_MS_BASED = 1;

    public int setPostionMode(Context context, int oldPositionMode, boolean agpsEnabled) {
        if ("true".equalsIgnoreCase(Settings.Global.getString(context.getContentResolver(), "hw_device_agps")) && !agpsEnabled) {
            return 2;
        }
        if (!"true".equals(Settings.Global.getString(context.getContentResolver(), FORBIDDEN_MSA_SWTICH)) || oldPositionMode != 2) {
            return oldPositionMode;
        }
        return 1;
    }
}
