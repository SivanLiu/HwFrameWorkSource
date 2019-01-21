package com.android.internal.telephony;

import android.common.HwCfgKey;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import huawei.cust.HwGetCfgFileConfig;
import java.util.regex.Pattern;

public class CustPlmnMember {
    private static final String LOG_TAG = "CustPlmnMember";
    private static final String NETMORK_TYPE = "network_type";
    private static final String NETWORK_MCCMNC = "network_mccmnc";
    private static final String PLMN = "plmn";
    private static final String REGEX = "(\\d:([^:,;]{5,14},){2}[^:,;]{1,20},[^:,;]{1,20}(,[^:,;]{1,5})?;)*(\\d:([^:,;]{5,14},){2}[^:,;]{1,20},[^:,;]{1,20}(,[^:,;]{1,5})?;?)$";
    private static final String RULE = "rule";
    private static final String SIM_MCCMNC = "sim_mccmnc";
    private static final String SPN = "spn";
    protected static final int SPN_RULE_SHOW_BOTH = 3;
    protected static final int SPN_RULE_SHOW_PLMN_ONLY = 2;
    protected static final int SPN_RULE_SHOW_PNN_PRIOR = 4;
    protected static final int SPN_RULE_SHOW_SPN_ONLY = 1;
    private static CustPlmnMember instance;
    public String plmn;
    public int rule;
    public boolean showPlmn;
    public boolean showSpn;
    public String spn;

    private CustPlmnMember() {
    }

    public static CustPlmnMember getInstance() {
        if (instance == null) {
            instance = new CustPlmnMember();
        }
        return instance;
    }

    private boolean isAvail(String str) {
        return (str == null || "".equals(str)) ? false : true;
    }

    public boolean acquireFromCust(String hplmn, ServiceState currentState, String custSpn) {
        String str = hplmn;
        String str2 = custSpn;
        String regplmn = currentState.getOperatorNumeric();
        int netType = currentState.getVoiceNetworkType();
        TelephonyManager.getDefault();
        int netClass = TelephonyManager.getNetworkClass(netType);
        int i = 0;
        boolean z = true;
        boolean isAllAvail = isAvail(str2) && isAvail(hplmn) && isAvail(regplmn);
        if (!isAllAvail) {
            Rlog.d(LOG_TAG, "acquireFromCust() failed, custSpn or hplmm or regplmn is null or empty string");
            return false;
        } else if (Pattern.matches(REGEX, str2)) {
            boolean isAllAvail2;
            String[] rules = str2.split(";");
            int length = rules.length;
            boolean match = false;
            int match2 = 0;
            while (match2 < length) {
                String[] rule_plmns = rules[match2].split(":");
                int rule_prop = Integer.parseInt(rule_plmns[i]);
                boolean custShowSpn = (rule_prop & 1) == z ? z : false;
                z = (rule_prop & 2) == 2;
                int netType2 = netType;
                String[] plmns = rule_plmns[1].split(",");
                isAllAvail2 = isAllAvail;
                if (plmns[0].equals(str) == 0) {
                    netType = 1;
                } else if (plmns[1].equals(regplmn) != 0) {
                    if (4 == plmns.length || (5 == plmns.length && plmns[4].contains(String.valueOf(netClass + 1)) != 0)) {
                        this.showSpn = custShowSpn;
                        this.showPlmn = z;
                        this.rule = rule_prop;
                        this.plmn = plmns[2];
                        this.spn = plmns[3];
                        return true;
                    }
                    match2++;
                    netType = netType2;
                    isAllAvail = isAllAvail2;
                    str2 = custSpn;
                    i = 0;
                    z = true;
                }
                if (plmns[0].equals(str) == 0 || plmns[1].equals("00000") == 0) {
                    if (!(plmns[0].equals("00000") == 0 || plmns[1].equals(regplmn) == 0)) {
                        this.rule = rule_prop;
                        this.showSpn = custShowSpn;
                        this.showPlmn = z;
                        this.plmn = plmns[2];
                        this.spn = plmns[3];
                        match = true;
                    }
                    match2++;
                    netType = netType2;
                    isAllAvail = isAllAvail2;
                    str2 = custSpn;
                    i = 0;
                    z = true;
                } else {
                    this.rule = rule_prop;
                    this.showSpn = custShowSpn;
                    this.showPlmn = z;
                    this.plmn = plmns[2];
                    this.spn = plmns[3];
                    match = true;
                    match2++;
                    netType = netType2;
                    isAllAvail = isAllAvail2;
                    str2 = custSpn;
                    i = 0;
                    z = true;
                }
            }
            isAllAvail2 = isAllAvail;
            if (match) {
                return true;
            }
            return false;
        } else {
            Rlog.d(LOG_TAG, "acquireFromCust() failed, custSpn does not match with regex");
            return false;
        }
    }

    public boolean judgeShowSpn(boolean showSpn) {
        return this.rule == 0 ? showSpn : this.showSpn;
    }

    public String judgeSpn(String spn) {
        return "####".equals(this.spn) ? spn : this.spn;
    }

    public String judgePlmn(String plmn) {
        return "####".equals(this.plmn) ? plmn : this.plmn;
    }

    /* JADX WARNING: Removed duplicated region for block: B:54:0x00d8 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00d7 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00d7 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00d8 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getCfgCustDisplayParams(String hplmn, ServiceState currentState, String custSpn, int slotid) {
        boolean odpCfgParams;
        boolean match = false;
        boolean cfgrule = false;
        int netType = currentState.getVoiceNetworkType();
        TelephonyManager.getDefault();
        OnsDisplayParams odpCfgParams2;
        try {
            HwCfgKey hwCfgKey = hwCfgKey;
            odpCfgParams2 = null;
            odpCfgParams = true;
            try {
                HwCfgKey keyCollection = new HwCfgKey(custSpn, NETWORK_MCCMNC, SIM_MCCMNC, NETMORK_TYPE, PLMN, currentState.getOperatorNumeric(), hplmn, String.valueOf(TelephonyManager.getNetworkClass(netType)), slotid);
                String custplmn = (String) HwGetCfgFileConfig.getCfgFileData(keyCollection, String.class);
                keyCollection.rkey = "rule";
                Integer custrule = (Integer) HwGetCfgFileConfig.getCfgFileData(keyCollection, Integer.class);
                keyCollection.rkey = "spn";
                String custspn = (String) HwGetCfgFileConfig.getCfgFileData(keyCollection, String.class);
                if (custrule != null) {
                    cfgrule = custrule.intValue();
                    this.rule = cfgrule;
                    match = true;
                    if (!(custspn == null || custplmn == null)) {
                        this.spn = custspn;
                        this.plmn = custplmn;
                    }
                }
                if (odpCfgParams == cfgrule && custspn != null) {
                    this.showSpn = (cfgrule & 1) == odpCfgParams ? odpCfgParams : false;
                    this.showPlmn = (cfgrule & 2) == 2 ? odpCfgParams : false;
                    this.rule = cfgrule;
                    this.spn = custspn;
                    match = true;
                }
                if (true == cfgrule && custplmn != null) {
                    this.showPlmn = (cfgrule & 2) == 2 ? odpCfgParams : false;
                    this.showSpn = (cfgrule & 1) == odpCfgParams ? odpCfgParams : false;
                    this.rule = cfgrule;
                    this.plmn = custplmn;
                    match = true;
                }
                if (!(true != cfgrule || custplmn == null || custspn == null)) {
                    this.showPlmn = (cfgrule & 2) == 2 ? odpCfgParams : false;
                    this.showSpn = (cfgrule & 1) == odpCfgParams ? odpCfgParams : false;
                    this.rule = cfgrule;
                    this.spn = custspn;
                    this.plmn = custplmn;
                    match = true;
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG, "Exception: read net_sim_ue_pri error");
                if (match) {
                }
            }
        } catch (Exception e2) {
            odpCfgParams2 = null;
            odpCfgParams = 1;
            Rlog.d(LOG_TAG, "Exception: read net_sim_ue_pri error");
            if (match) {
            }
        }
        if (match) {
            return odpCfgParams;
        }
        return false;
    }
}
