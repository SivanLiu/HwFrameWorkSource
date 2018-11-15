package com.android.internal.telephony;

import java.util.ArrayList;
import java.util.List;

public class HwIccIdUtil {
    private static final List<String> CMCC_ICCID_ARRAY = new ArrayList();
    private static final List<String> CMCC_MCCMNC_ARRAY = new ArrayList();
    private static final List<String> CT_ICCID_ARRAY = new ArrayList();
    private static final List<String> CU_ICCID_ARRAY = new ArrayList();
    private static final List<String> CU_MCCMNC_ARRAY = new ArrayList();
    public static final int ICCID_LEN_MINIMUM = 7;
    public static final int ICCID_LEN_SIX = 6;
    public static final int MCCMNC_LEN_MINIMUM = 5;
    private static final String PREFIX_LOCAL_ICCID = "8986";
    private static final String PREFIX_LOCAL_MCC = "460";

    static {
        CMCC_ICCID_ARRAY.clear();
        CMCC_ICCID_ARRAY.add("898600");
        CMCC_ICCID_ARRAY.add("898602");
        CMCC_ICCID_ARRAY.add("898607");
        CMCC_ICCID_ARRAY.add("898212");
        CU_ICCID_ARRAY.clear();
        CU_ICCID_ARRAY.add("898601");
        CU_ICCID_ARRAY.add("898609");
        CT_ICCID_ARRAY.clear();
        CT_ICCID_ARRAY.add("898603");
        CT_ICCID_ARRAY.add("898611");
        CT_ICCID_ARRAY.add("898606");
        CT_ICCID_ARRAY.add("8985302");
        CT_ICCID_ARRAY.add("8985307");
        CMCC_MCCMNC_ARRAY.clear();
        CMCC_MCCMNC_ARRAY.add("46000");
        CMCC_MCCMNC_ARRAY.add("46002");
        CMCC_MCCMNC_ARRAY.add("46007");
        CMCC_MCCMNC_ARRAY.add("46008");
        CU_MCCMNC_ARRAY.clear();
        CU_MCCMNC_ARRAY.add("46001");
        CU_MCCMNC_ARRAY.add("46006");
        CU_MCCMNC_ARRAY.add("46009");
    }

    public static boolean isCMCC(String inn) {
        if (inn != null && inn.length() >= 7) {
            inn = inn.substring(0, 6);
        }
        return CMCC_ICCID_ARRAY.contains(inn);
    }

    public static boolean isCT(String inn) {
        if (inn != null && inn.startsWith(PREFIX_LOCAL_ICCID) && inn.length() >= 7) {
            inn = inn.substring(0, 6);
        }
        return CT_ICCID_ARRAY.contains(inn);
    }

    public static boolean isCU(String inn) {
        if (inn != null && inn.length() >= 7) {
            inn = inn.substring(0, 6);
        }
        return CU_ICCID_ARRAY.contains(inn);
    }

    public static boolean isCMCCByMccMnc(String mccMnc) {
        if (mccMnc != null && mccMnc.length() > 5) {
            mccMnc = mccMnc.substring(0, 5);
        }
        return CMCC_MCCMNC_ARRAY.contains(mccMnc);
    }

    public static boolean isCUByMccMnc(String mccMnc) {
        if (mccMnc != null && mccMnc.length() > 5) {
            mccMnc = mccMnc.substring(0, 5);
        }
        return CU_MCCMNC_ARRAY.contains(mccMnc);
    }
}
