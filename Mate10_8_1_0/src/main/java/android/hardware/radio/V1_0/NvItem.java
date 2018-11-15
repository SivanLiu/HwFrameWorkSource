package android.hardware.radio.V1_0;

public final class NvItem {
    public static final int CDMA_1X_ADVANCED_ENABLED = 57;
    public static final int CDMA_ACCOLC = 4;
    public static final int CDMA_BC10 = 52;
    public static final int CDMA_BC14 = 53;
    public static final int CDMA_EHRPD_ENABLED = 58;
    public static final int CDMA_EHRPD_FORCED = 59;
    public static final int CDMA_MDN = 3;
    public static final int CDMA_MEID = 1;
    public static final int CDMA_MIN = 2;
    public static final int CDMA_PRL_VERSION = 51;
    public static final int CDMA_SO68 = 54;
    public static final int CDMA_SO73_COP0 = 55;
    public static final int CDMA_SO73_COP1TO7 = 56;
    public static final int DEVICE_MSL = 11;
    public static final int LTE_BAND_ENABLE_25 = 71;
    public static final int LTE_BAND_ENABLE_26 = 72;
    public static final int LTE_BAND_ENABLE_41 = 73;
    public static final int LTE_HIDDEN_BAND_PRIORITY_25 = 77;
    public static final int LTE_HIDDEN_BAND_PRIORITY_26 = 78;
    public static final int LTE_HIDDEN_BAND_PRIORITY_41 = 79;
    public static final int LTE_SCAN_PRIORITY_25 = 74;
    public static final int LTE_SCAN_PRIORITY_26 = 75;
    public static final int LTE_SCAN_PRIORITY_41 = 76;
    public static final int MIP_PROFILE_AAA_AUTH = 33;
    public static final int MIP_PROFILE_AAA_SPI = 39;
    public static final int MIP_PROFILE_HA_AUTH = 34;
    public static final int MIP_PROFILE_HA_SPI = 38;
    public static final int MIP_PROFILE_HOME_ADDRESS = 32;
    public static final int MIP_PROFILE_MN_AAA_SS = 41;
    public static final int MIP_PROFILE_MN_HA_SS = 40;
    public static final int MIP_PROFILE_NAI = 31;
    public static final int MIP_PROFILE_PRI_HA_ADDR = 35;
    public static final int MIP_PROFILE_REV_TUN_PREF = 37;
    public static final int MIP_PROFILE_SEC_HA_ADDR = 36;
    public static final int OMADM_HFA_LEVEL = 18;
    public static final int RTN_ACTIVATION_DATE = 13;
    public static final int RTN_LIFE_CALLS = 15;
    public static final int RTN_LIFE_DATA_RX = 17;
    public static final int RTN_LIFE_DATA_TX = 16;
    public static final int RTN_LIFE_TIMER = 14;
    public static final int RTN_RECONDITIONED_STATUS = 12;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.NvItem.dumpBitfield(int):java.lang.String
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.NvItem.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "CDMA_MEID";
        }
        if (o == 2) {
            return "CDMA_MIN";
        }
        if (o == 3) {
            return "CDMA_MDN";
        }
        if (o == 4) {
            return "CDMA_ACCOLC";
        }
        if (o == 11) {
            return "DEVICE_MSL";
        }
        if (o == 12) {
            return "RTN_RECONDITIONED_STATUS";
        }
        if (o == 13) {
            return "RTN_ACTIVATION_DATE";
        }
        if (o == 14) {
            return "RTN_LIFE_TIMER";
        }
        if (o == 15) {
            return "RTN_LIFE_CALLS";
        }
        if (o == 16) {
            return "RTN_LIFE_DATA_TX";
        }
        if (o == 17) {
            return "RTN_LIFE_DATA_RX";
        }
        if (o == 18) {
            return "OMADM_HFA_LEVEL";
        }
        if (o == 31) {
            return "MIP_PROFILE_NAI";
        }
        if (o == 32) {
            return "MIP_PROFILE_HOME_ADDRESS";
        }
        if (o == 33) {
            return "MIP_PROFILE_AAA_AUTH";
        }
        if (o == 34) {
            return "MIP_PROFILE_HA_AUTH";
        }
        if (o == 35) {
            return "MIP_PROFILE_PRI_HA_ADDR";
        }
        if (o == 36) {
            return "MIP_PROFILE_SEC_HA_ADDR";
        }
        if (o == 37) {
            return "MIP_PROFILE_REV_TUN_PREF";
        }
        if (o == 38) {
            return "MIP_PROFILE_HA_SPI";
        }
        if (o == 39) {
            return "MIP_PROFILE_AAA_SPI";
        }
        if (o == 40) {
            return "MIP_PROFILE_MN_HA_SS";
        }
        if (o == 41) {
            return "MIP_PROFILE_MN_AAA_SS";
        }
        if (o == 51) {
            return "CDMA_PRL_VERSION";
        }
        if (o == 52) {
            return "CDMA_BC10";
        }
        if (o == 53) {
            return "CDMA_BC14";
        }
        if (o == 54) {
            return "CDMA_SO68";
        }
        if (o == 55) {
            return "CDMA_SO73_COP0";
        }
        if (o == 56) {
            return "CDMA_SO73_COP1TO7";
        }
        if (o == 57) {
            return "CDMA_1X_ADVANCED_ENABLED";
        }
        if (o == 58) {
            return "CDMA_EHRPD_ENABLED";
        }
        if (o == 59) {
            return "CDMA_EHRPD_FORCED";
        }
        if (o == 71) {
            return "LTE_BAND_ENABLE_25";
        }
        if (o == 72) {
            return "LTE_BAND_ENABLE_26";
        }
        if (o == 73) {
            return "LTE_BAND_ENABLE_41";
        }
        if (o == 74) {
            return "LTE_SCAN_PRIORITY_25";
        }
        if (o == 75) {
            return "LTE_SCAN_PRIORITY_26";
        }
        if (o == 76) {
            return "LTE_SCAN_PRIORITY_41";
        }
        if (o == 77) {
            return "LTE_HIDDEN_BAND_PRIORITY_25";
        }
        if (o == 78) {
            return "LTE_HIDDEN_BAND_PRIORITY_26";
        }
        if (o == 79) {
            return "LTE_HIDDEN_BAND_PRIORITY_41";
        }
        return "0x" + Integer.toHexString(o);
    }
}
