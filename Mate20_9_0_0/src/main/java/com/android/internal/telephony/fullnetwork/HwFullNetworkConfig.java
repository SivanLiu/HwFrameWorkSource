package com.android.internal.telephony.fullnetwork;

import android.os.SystemProperties;
import com.android.internal.telephony.HwModemCapability;

public class HwFullNetworkConfig {
    public static final int DEFAULT_NETWORK_MODE = SystemProperties.getInt("ro.telephony.default_network", -1);
    public static final boolean IS_4G_SWITCH_SUPPORTED = SystemProperties.getBoolean("persist.sys.dualcards", false);
    public static final boolean IS_CARD2_CDMA_SUPPORTED = SystemProperties.getBoolean("ro.hwpp.card2_cdma_support", false);
    public static final boolean IS_CHINA_TELECOM;
    public static final boolean IS_CMCC_4GSWITCH_DISABLE;
    public static final boolean IS_CMCC_4G_DSDX_ENABLE = SystemProperties.getBoolean("ro.hwpp.cmcc_4G_dsdx_enable", false);
    public static final boolean IS_CMCC_CU_DSDX_ENABLE = SystemProperties.getBoolean("ro.hwpp.cmcc_cu_dsdx_enable", false);
    public static final boolean IS_CT_4GSWITCH_DISABLE = "ct".equalsIgnoreCase(SystemProperties.get("ro.hwpp.dualsim_swap_solution", ""));
    public static final boolean IS_DUAL_4G_SUPPORTED = HwModemCapability.isCapabilitySupport(21);
    public static final boolean IS_FAST_SWITCH_SIMSLOT = SystemProperties.getBoolean("ro.config.fast_switch_simslot", false);
    public static final boolean IS_FULL_NETWORK_SUPPORTED = SystemProperties.getBoolean(PROPERTY_FULL_NETWORK_SUPPORT, false);
    public static final boolean IS_FULL_NETWORK_SUPPORTED_IN_HISI;
    public static final boolean IS_HISI_CDMA_SUPPORTED = SystemProperties.getBoolean("ro.config.hisi_cdma_supported", false);
    public static final boolean IS_HISI_DSDS_AUTO_SWITCH_4G_SLOT;
    public static final boolean IS_HISI_DSDX;
    public static final boolean IS_OVERSEA_USIM_SUPPORT;
    public static final boolean IS_QCOM_DUAL_LTE_STACK = HwModemCapability.isCapabilitySupport(27);
    public static final boolean IS_QCRIL_CROSS_MAPPING = SystemProperties.getBoolean("ro.hwpp.qcril_cross_mapping", false);
    public static final boolean IS_SINGLE_CARD_TRAY = SystemProperties.getBoolean("persist.radio.single_card_tray", true);
    public static final boolean IS_TUNERIC_LOW_PERF = SystemProperties.getBoolean("ro.hwpp.is_tuneric_low_perf", false);
    public static final boolean IS_VICE_WCDMA = SystemProperties.getBoolean("ro.config.support_wcdma_modem1", false);
    public static final String PROPERTY_CG_STANDBY_MODE = "persist.radio.cg_standby_mode";
    public static final String PROPERTY_COMMRIL_MODE = "persist.radio.commril_mode";
    public static final String PROPERTY_FULL_NETWORK_SUPPORT = "ro.config.full_network_support";
    private static final String PROPERTY_HW_OPTA_TELECOM = "92";
    private static final String PROPERTY_HW_OPTB_CHINA = "156";
    public static final boolean RESET_PROFILE = SystemProperties.getBoolean("ro.hwpp_reset_profile", false);

    static {
        boolean z = true;
        boolean z2 = SystemProperties.get("ro.config.hw_opta", "0").equals(PROPERTY_HW_OPTA_TELECOM) && SystemProperties.get("ro.config.hw_optb", "0").equals(PROPERTY_HW_OPTB_CHINA);
        IS_CHINA_TELECOM = z2;
        z2 = IS_CMCC_4G_DSDX_ENABLE && "cmcc".equalsIgnoreCase(SystemProperties.get("ro.hwpp.dualsim_swap_solution", ""));
        IS_CMCC_4GSWITCH_DISABLE = z2;
        z2 = IS_FULL_NETWORK_SUPPORTED && !HwModemCapability.isCapabilitySupport(9) && SystemProperties.getBoolean("persist.hisi.fullnetwork", true) && "normal".equals(SystemProperties.get("ro.runmode", "normal"));
        IS_FULL_NETWORK_SUPPORTED_IN_HISI = z2;
        z2 = (HwModemCapability.isCapabilitySupport(9) || !"normal".equals(SystemProperties.get("ro.runmode", "normal")) || IS_CHINA_TELECOM) ? false : true;
        IS_HISI_DSDX = z2;
        z2 = !HwModemCapability.isCapabilitySupport(9) && "normal".equals(SystemProperties.get("ro.runmode", "normal")) && SystemProperties.getBoolean("ro.config.hw_switchdata_4G", false);
        IS_HISI_DSDS_AUTO_SWITCH_4G_SLOT = z2;
        if (!(IS_CHINA_TELECOM && SystemProperties.getBoolean("persist.radio.supp_oversea_usim", false))) {
            z = false;
        }
        IS_OVERSEA_USIM_SUPPORT = z;
    }

    public static boolean isHisiPlatform() {
        if (HwModemCapability.isCapabilitySupport(9)) {
            return false;
        }
        return true;
    }
}
