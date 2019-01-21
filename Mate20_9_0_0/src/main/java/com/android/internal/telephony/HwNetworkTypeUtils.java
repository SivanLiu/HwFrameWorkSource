package com.android.internal.telephony;

import android.os.SystemProperties;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConfig;
import java.util.HashMap;
import java.util.Map.Entry;

public class HwNetworkTypeUtils {
    public static final int INVALID_NETWORK_MODE = -1;
    public static final boolean IS_MODEM_FULL_PREFMODE_SUPPORTED = HwModemCapability.isCapabilitySupport(3);
    public static final int LTE_SERVICE_OFF = 0;
    public static final int LTE_SERVICE_ON = 1;
    private static HashMap<Integer, Integer> LteOnOffMapping = new HashMap();
    public static int lteOffMappingMode;
    public static int lteOnMappingMode;

    static {
        lteOnMappingMode = -1;
        lteOffMappingMode = -1;
        LteOnOffMapping.put(Integer.valueOf(8), Integer.valueOf(4));
        LteOnOffMapping.put(Integer.valueOf(9), Integer.valueOf(3));
        LteOnOffMapping.put(Integer.valueOf(10), Integer.valueOf(7));
        LteOnOffMapping.put(Integer.valueOf(11), Integer.valueOf(7));
        LteOnOffMapping.put(Integer.valueOf(12), Integer.valueOf(2));
        LteOnOffMapping.put(Integer.valueOf(15), Integer.valueOf(13));
        LteOnOffMapping.put(Integer.valueOf(17), Integer.valueOf(16));
        LteOnOffMapping.put(Integer.valueOf(19), Integer.valueOf(14));
        LteOnOffMapping.put(Integer.valueOf(20), Integer.valueOf(18));
        LteOnOffMapping.put(Integer.valueOf(22), Integer.valueOf(21));
        LteOnOffMapping.put(Integer.valueOf(25), Integer.valueOf(24));
        LteOnOffMapping.put(Integer.valueOf(26), Integer.valueOf(1));
        LteOnOffMapping.put(Integer.valueOf(61), Integer.valueOf(52));
        lteOnMappingMode = SystemProperties.getInt("ro.telephony.default_network", -1);
        if (LteOnOffMapping.containsKey(Integer.valueOf(lteOnMappingMode))) {
            lteOffMappingMode = ((Integer) LteOnOffMapping.get(Integer.valueOf(lteOnMappingMode))).intValue();
        } else {
            lteOnMappingMode = -1;
        }
        String[] lteOnOffMapings = SystemProperties.get("ro.hwpp.lteonoff_mapping", "0,0").split(",");
        if (lteOnOffMapings.length == 2 && Integer.parseInt(lteOnOffMapings[0]) != 0) {
            lteOnMappingMode = Integer.parseInt(lteOnOffMapings[0]);
            lteOffMappingMode = Integer.parseInt(lteOnOffMapings[1]);
        }
    }

    public static int getOnModeFromMapping(int curPrefMode) {
        int onKey = -1;
        for (Entry<Integer, Integer> entry : LteOnOffMapping.entrySet()) {
            if (curPrefMode == ((Integer) entry.getValue()).intValue()) {
                onKey = ((Integer) entry.getKey()).intValue();
                if (7 != curPrefMode || 11 != onKey) {
                    break;
                }
            }
        }
        if (HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE || onKey != 26) {
            return onKey;
        }
        return 9;
    }

    public static int getOffModeFromMapping(int curPrefMode) {
        if (LteOnOffMapping.containsKey(Integer.valueOf(curPrefMode))) {
            return ((Integer) LteOnOffMapping.get(Integer.valueOf(curPrefMode))).intValue();
        }
        return -1;
    }

    public static boolean isLteServiceOn(int curPrefMode) {
        return LteOnOffMapping.containsKey(Integer.valueOf(curPrefMode)) || curPrefMode == 23 || curPrefMode == 30 || curPrefMode == 31 || curPrefMode == 63;
    }
}
