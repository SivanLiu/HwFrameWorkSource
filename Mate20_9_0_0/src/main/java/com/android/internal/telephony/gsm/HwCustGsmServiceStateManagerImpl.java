package com.android.internal.telephony.gsm;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.provider.SettingsEx.Systemex;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.text.TextUtils;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.OnsDisplayParams;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.VirtualNet;
import com.android.internal.telephony.uicc.IccRecords;
import java.util.HashSet;

public class HwCustGsmServiceStateManagerImpl extends HwCustGsmServiceStateManager {
    private static final String ACTION_RAC_CHANGED = "com.huawei.android.intent.action.RAC_CHANGED";
    private static final boolean HWDBG = true;
    private static final boolean IS_VIDEOTRON;
    private static final String LOG_TAG = "HwCustGsmServiceStateManagerImpl";
    private static final String[] NetWorkPlmnListATT = new String[]{"310170", "310410", "310560", "311180", "310380", "310030", "310280", "310950", "310150"};
    private static final String[] NetWorkPlmnListTMO = new String[]{"310160", "310200", "310210", "310220", "310230", "310240", "310250", "310260", "310270", "310300", "310310", "310490", "310530", "310580", "310590", "310640", "310660", "310800"};
    private static final boolean PS_CLEARCODE = SystemProperties.getBoolean("ro.config.hw_clearcode_pdp", false);
    private static final String RogersMccmnc = "302720";
    private static final boolean UseUSA_gsmroaming_cmp = SystemProperties.get("ro.config.USA_gsmroaming_cmp", "false").equals("true");
    private static final String VideotronMccmnc = "302500,302510,302520";
    private static final String[] roamingPlmnListATT = new String[]{"310110", "310140", "310400", "310470", "311170"};
    private static final String[] roamingPlmnListTMO = new String[]{"310470", "310370", "310032", "310140", "310250", "310400", "311170"};
    boolean USARoamingRuleAffect = false;
    private boolean mIsActualRoaming = false;
    private int mOldRac = -1;
    private int mRac = -1;
    private HashSet<String> mShowNetnameMcc = new HashSet();

    static {
        boolean z = ("119".equals(SystemProperties.get("ro.config.hw_opta", "0")) && "124".equals(SystemProperties.get("ro.config.hw_optb", "0"))) ? HWDBG : false;
        IS_VIDEOTRON = z;
    }

    public HwCustGsmServiceStateManagerImpl(ServiceStateTracker sst, GsmCdmaPhone gsmPhone) {
        super(sst, gsmPhone);
    }

    public boolean setRoamingStateForOperatorCustomization(ServiceState currentState, boolean ParaRoamingState) {
        if (currentState == null || currentState.getState() != 0) {
            return ParaRoamingState;
        }
        String hplmn = null;
        if (this.mGsmPhone.mIccRecords.get() != null) {
            hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
        }
        String regplmn = currentState.getOperatorNumeric();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Roaming customaziton for vendor hplmn = ");
        stringBuilder.append(hplmn);
        stringBuilder.append("  regplmn=");
        stringBuilder.append(regplmn);
        log(stringBuilder.toString());
        if (hplmn == null || regplmn == null || 5 > regplmn.length()) {
            return ParaRoamingState;
        }
        if (("302490".equals(hplmn) || "22288".equals(hplmn) || "22201".equals(hplmn)) && "302".equals(regplmn.substring(0, 3))) {
            return false;
        }
        return ParaRoamingState;
    }

    private void log(String message) {
        Rlog.d(LOG_TAG, message);
    }

    public OnsDisplayParams setOnsDisplayCustomization(OnsDisplayParams odp, ServiceState currentState) {
        OnsDisplayParams ons = odp;
        if (currentState != null && currentState.getState() == 0) {
            StringBuilder stringBuilder;
            String hplmn = null;
            String regplmn = currentState.getOperatorNumeric();
            String spnRes = ons.mSpn;
            IccRecords iccRecords = (IccRecords) this.mGsmPhone.mIccRecords.get();
            if (iccRecords != null) {
                hplmn = iccRecords.getOperatorNumeric();
                if (!TextUtils.isEmpty(iccRecords.getServiceProviderName())) {
                    spnRes = iccRecords.getServiceProviderName();
                }
            }
            String str = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SetOnsDisplayCustomization for vendor hplmn = ");
            stringBuilder2.append(hplmn);
            stringBuilder2.append("  regplmn=");
            stringBuilder2.append(regplmn);
            stringBuilder2.append("  spn=");
            stringBuilder2.append(spnRes);
            Rlog.d(str, stringBuilder2.toString());
            if ("20404".equals(hplmn) && !TextUtils.isEmpty(spnRes) && "ziggo.dataxs.mob".equalsIgnoreCase(spnRes.trim())) {
                ons.mSpn = "Ziggo";
            }
            if (isShowNetnameByMcc(hplmn, regplmn) && !TextUtils.isEmpty(currentState.getOperatorAlphaLong())) {
                ons.mPlmn = currentState.getOperatorAlphaLong();
                ons.mShowPlmn = HWDBG;
                ons.mShowSpn = false;
            }
            if (!TextUtils.isEmpty(regplmn) && regplmn.equals(hplmn) && isShowNetOnly(hplmn)) {
                ons.mShowPlmn = HWDBG;
                ons.mShowSpn = false;
            }
            if (isEplmnShowSpnPlus(hplmn, regplmn, spnRes)) {
                if (!spnRes.contains("+")) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(spnRes);
                    stringBuilder.append("+");
                    ons.mSpn = stringBuilder.toString();
                }
                ons.mShowPlmn = false;
                ons.mShowSpn = HWDBG;
            }
            if (isDualLinePlmn(hplmn, regplmn, spnRes)) {
                if (hplmn.equals(regplmn)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("o2 - de ");
                    stringBuilder.append(spnRes);
                    ons.mSpn = stringBuilder.toString();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("o2 - de+ ");
                    stringBuilder.append(spnRes);
                    ons.mSpn = stringBuilder.toString();
                }
                ons.mShowPlmn = false;
                ons.mShowSpn = HWDBG;
            }
        }
        return ons;
    }

    /* JADX WARNING: Missing block: B:16:0x0034, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isShowNetnameByMcc(String hplmn, String regplmn) {
        if (TextUtils.isEmpty(hplmn) || hplmn.length() < 3 || TextUtils.isEmpty(regplmn) || regplmn.length() < 3) {
            return false;
        }
        String hmcc = hplmn.substring(0, 3);
        String rmcc = regplmn.substring(0, 3);
        if (isMccForShowNetname(hmcc) && hmcc.equals(rmcc)) {
            return HWDBG;
        }
        return false;
    }

    private boolean isMccForShowNetname(String currentMccmnc) {
        if (this.mShowNetnameMcc.size() == 0) {
            String strMcc = Systemex.getString(this.mContext.getContentResolver(), "hw_mcc_show_netname");
            if (strMcc != null) {
                String[] mcc = strMcc.split(",");
                for (String trim : mcc) {
                    this.mShowNetnameMcc.add(trim.trim());
                }
            }
        }
        return this.mShowNetnameMcc.contains(currentMccmnc);
    }

    public boolean notUseVirtualName(String mImsi) {
        if (TextUtils.isEmpty(mImsi)) {
            return false;
        }
        String string;
        String custPlmnsString = System.getString(this.mContext.getContentResolver(), "hw_notUseVirtualNetCust");
        if (!TextUtils.isEmpty(custPlmnsString)) {
            String[] custPlmns = custPlmnsString.split(";");
            for (String string2 : custPlmns) {
                if (mImsi.startsWith(string2)) {
                    Rlog.d(LOG_TAG, "Imsi matched,did not use the virtualnets.xml name");
                    return HWDBG;
                }
            }
        }
        String hplmn = null;
        int i = this.mGsmPhone.getPhoneId();
        if (this.mGsmPhone.mIccRecords.get() != null) {
            hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
        }
        if (hplmn != null && VirtualNet.isVirtualNet(i) && VirtualNet.getCurrentVirtualNet(i).isRealNetwork) {
            string2 = System.getString(this.mContext.getContentResolver(), "hw_real_net_not_use_vn_ons");
            if (!TextUtils.isEmpty(string2)) {
                String[] custmccmnc = string2.split(";");
                for (Object equals : custmccmnc) {
                    if (hplmn.equals(equals)) {
                        Rlog.d(LOG_TAG, "hplmn equels hw_real_net_not_use_vn_ons config");
                        return HWDBG;
                    }
                }
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:24:0x0075, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isEplmnShowSpnPlus(String hplmn, String rplmn, String spn) {
        if (TextUtils.isEmpty(hplmn) || TextUtils.isEmpty(rplmn) || TextUtils.isEmpty(spn) || isDualLinePlmn(hplmn, rplmn, spn)) {
            return false;
        }
        String custPlmnString = System.getString(this.mContext.getContentResolver(), "hw_eplmn_show_spnplus");
        if (!TextUtils.isEmpty(custPlmnString)) {
            String[] custEplmnArray = custPlmnString.split(";");
            for (String[] custEplmnArrayPlmns : custEplmnArray) {
                String[] custEplmnArrayPlmns2 = custEplmnArrayPlmns2.split(",");
                if (!hplmn.equals(rplmn) && isContained(hplmn, custEplmnArrayPlmns2) && isContained(rplmn, custEplmnArrayPlmns2)) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("isEplmnShowSpnPlus hplmn:");
                    stringBuilder.append(hplmn);
                    stringBuilder.append("|rplmn:");
                    stringBuilder.append(rplmn);
                    Rlog.d(str, stringBuilder.toString());
                    return HWDBG;
                }
            }
        }
        return false;
    }

    private boolean isDualLinePlmn(String hplmn, String rplmn, String spn) {
        if (!"26207".equals(hplmn) || ((!"26207".equals(rplmn) && !"26203".equals(rplmn)) || (!"Private".equals(spn) && !"Business".equals(spn)))) {
            return false;
        }
        Rlog.d(LOG_TAG, "isDualLinePlmn");
        return HWDBG;
    }

    private boolean isContained(String plmn, String[] plmnArray) {
        if (TextUtils.isEmpty(plmn) || plmnArray == null) {
            return false;
        }
        for (String s : plmnArray) {
            if (plmn.equals(s)) {
                return HWDBG;
            }
        }
        return false;
    }

    private boolean isShowNetOnly(String hplmn) {
        String custPlmnString = System.getString(this.mContext.getContentResolver(), "hw_show_netname_only");
        if (TextUtils.isEmpty(custPlmnString) || TextUtils.isEmpty(hplmn) || !isContained(hplmn, custPlmnString.split(","))) {
            return false;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isShowNetOnly hplmn:");
        stringBuilder.append(hplmn);
        Rlog.d(str, stringBuilder.toString());
        return HWDBG;
    }

    public void storeModemRoamingStatus(boolean roaming) {
        if (IS_VIDEOTRON) {
            this.mIsActualRoaming = roaming;
        }
    }

    public OnsDisplayParams getGsmOnsDisplayParamsForVideotron(boolean showSpn, boolean showPlmn, int rule, String plmn, String spn) {
        OnsDisplayParams odpCustForVideotron = null;
        if (!IS_VIDEOTRON) {
            return null;
        }
        boolean z;
        int i;
        String str;
        if (this.mGsmPhone.mIccRecords.get() == null || this.mGsst.mSS == null) {
            z = showPlmn;
            i = rule;
            str = spn;
        } else {
            boolean showSpn2;
            String hplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumericEx(this.mCr, "hw_ons_hplmn_ex");
            String regplmn = this.mGsst.mSS.getOperatorNumeric();
            String str2 = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getGsmOnsDisplayParamsForVideotron hplmn:");
            stringBuilder.append(hplmn);
            stringBuilder.append("|regplmn:");
            stringBuilder.append(regplmn);
            Rlog.d(str2, stringBuilder.toString());
            str2 = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getGsmOnsDisplayParamsForVideotron  mIsActualRoaming: ");
            stringBuilder.append(this.mIsActualRoaming);
            stringBuilder.append(" Roaming: ");
            stringBuilder.append(this.mGsst.mSS.getRoaming());
            Rlog.d(str2, stringBuilder.toString());
            if (TextUtils.isEmpty(hplmn) || TextUtils.isEmpty(regplmn) || VideotronMccmnc.indexOf(hplmn) == -1 || !RogersMccmnc.equals(regplmn)) {
                showSpn2 = showSpn;
                z = showPlmn;
                i = rule;
                str = spn;
            } else {
                showSpn2 = HWDBG;
                z = false;
                i = 1;
                if (this.mIsActualRoaming || this.mGsst.mSS.getRoaming()) {
                    str = "Videotron PRTNR1";
                } else {
                    str = "Videotron";
                }
                String str3 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getGsmOnsDisplayParamsForVideotron spn:");
                stringBuilder2.append(str);
                stringBuilder2.append(" showSpn :");
                stringBuilder2.append(HWDBG);
                stringBuilder2.append(" rule:");
                stringBuilder2.append(1);
                Rlog.d(str3, stringBuilder2.toString());
            }
            odpCustForVideotron = new OnsDisplayParams(showSpn2, z, i, plmn, str);
        }
        return odpCustForVideotron;
    }

    public boolean checkIsInternationalRoaming(boolean roaming, ServiceState currentState) {
        boolean GsmRoaming = roaming;
        String SimNumeric = null;
        if (UseUSA_gsmroaming_cmp) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Before checkIsInternationalRoaming, roaming is ");
            stringBuilder.append(roaming);
            Rlog.d("GsmServiceStateTracker", stringBuilder.toString());
            if (currentState != null && currentState.getState() == 0 && HWDBG == roaming) {
                if (this.mGsmPhone.mIccRecords.get() != null) {
                    SimNumeric = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getOperatorNumeric();
                }
                if (currentState.getOperatorNumeric() == null || SimNumeric == null) {
                    Rlog.d("GsmServiceStateTracker", "Invalid operatorNumeric or SimNumeric");
                    return GsmRoaming;
                }
                if (isPlmnOfOperator(SimNumeric, NetWorkPlmnListATT)) {
                    String actingHplmn = ((IccRecords) this.mGsmPhone.mIccRecords.get()).getActingHplmn();
                    if (!(actingHplmn == null || "".equals(actingHplmn))) {
                        Rlog.d("GsmServiceStateTracker", "Invalid operatorNumeric or SimNumeric");
                        SimNumeric = actingHplmn;
                    }
                    if (isPlmnOfOperator(SimNumeric, NetWorkPlmnListATT) && isAmericanNetwork(currentState.getOperatorNumeric(), roamingPlmnListATT)) {
                        GsmRoaming = false;
                    }
                } else if (isPlmnOfOperator(SimNumeric, NetWorkPlmnListTMO) && isAmericanNetwork(currentState.getOperatorNumeric(), roamingPlmnListTMO)) {
                    GsmRoaming = false;
                }
                if (!GsmRoaming) {
                    this.USARoamingRuleAffect = HWDBG;
                }
            } else {
                this.USARoamingRuleAffect = false;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("After checkIsInternationalRoaming, roaming is ");
            stringBuilder.append(GsmRoaming);
            Rlog.d("GsmServiceStateTracker", stringBuilder.toString());
        }
        return GsmRoaming;
    }

    public boolean isPlmnOfOperator(String plmn, String[] operatorPlmnList) {
        boolean isPlmnOfOperator = false;
        if (plmn == null || operatorPlmnList == null) {
            return false;
        }
        for (String opreatorPlmn : operatorPlmnList) {
            if (plmn.equals(opreatorPlmn)) {
                isPlmnOfOperator = HWDBG;
                break;
            }
        }
        return isPlmnOfOperator;
    }

    private boolean isAmericanNetwork(String plmn, String[] roamingPlmnList) {
        boolean isAmericanNetwork = false;
        if (plmn == null || roamingPlmnList == null) {
            return false;
        }
        int AmericanMCC;
        int i = 0;
        if (5 <= plmn.length()) {
            AmericanMCC = Integer.parseInt(plmn.substring(0, 3));
            if (AmericanMCC < 310 || AmericanMCC > 316) {
                isAmericanNetwork = false;
            } else {
                isAmericanNetwork = HWDBG;
            }
        }
        if (HWDBG == isAmericanNetwork) {
            AmericanMCC = roamingPlmnList.length;
            while (i < AmericanMCC) {
                if (plmn.equals(roamingPlmnList[i])) {
                    isAmericanNetwork = false;
                    break;
                }
                i++;
            }
        }
        return isAmericanNetwork;
    }

    public boolean iscustRoamingRuleAffect(boolean roaming) {
        boolean isMatchRoamingRule = false;
        if (UseUSA_gsmroaming_cmp && !roaming) {
            isMatchRoamingRule = this.USARoamingRuleAffect;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isMatchRoamingRule: ");
        stringBuilder.append(isMatchRoamingRule);
        Rlog.d("GsmServiceStateTracker", stringBuilder.toString());
        return isMatchRoamingRule;
    }

    public IntentFilter getCustIntentFilter(IntentFilter filter) {
        if (PS_CLEARCODE) {
            filter.addAction(ACTION_RAC_CHANGED);
        }
        return filter;
    }

    public int handleBroadcastReceived(Context context, Intent intent, int rac) {
        if (!PS_CLEARCODE || intent == null || !intent.getAction().equals(ACTION_RAC_CHANGED)) {
            return rac;
        }
        this.mRac = ((Integer) intent.getExtra("rac", Integer.valueOf(-1))).intValue();
        if (this.mRac == -1 || this.mOldRac == this.mRac) {
            return rac;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CLEARCODE mRac = ");
        stringBuilder.append(this.mRac);
        stringBuilder.append(" , mOldRac = ");
        stringBuilder.append(this.mOldRac);
        log(stringBuilder.toString());
        this.mOldRac = this.mRac;
        return this.mRac;
    }

    public boolean skipPlmnUpdateFromCust() {
        return SystemProperties.getBoolean("ro.config.display_pnn_name", false);
    }
}
