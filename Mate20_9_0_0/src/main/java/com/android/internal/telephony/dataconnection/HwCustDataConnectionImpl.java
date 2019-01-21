package com.android.internal.telephony.dataconnection;

import android.common.HwCfgKey;
import android.content.ContentResolver;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccRecords;
import huawei.cust.HwGetCfgFileConfig;
import java.util.ArrayList;

public class HwCustDataConnectionImpl extends HwCustDataConnection {
    private static final String DEFAULT_PCO_DATA = "-2;-2";
    private static final String DEFAULT_PCO_VALUE = "-2";
    private static final String FOTA_APN = "open-dm2.dcm-dm.ne.jp";
    private static final boolean HWDBG = true;
    private static final boolean HW_SIM_ACTIVATION = SystemProperties.getBoolean("ro.config.hw_sim_activation", false);
    private static final int INTERNET_PCO_TYPE = 3;
    private static final boolean IS_DOCOMO;
    private static final String PCO_DATA = "pco_data";
    private static final String SPLIT = ";";
    private static final String TAG = "HwCustDataConnectionImpl";

    static {
        boolean z = (SystemProperties.get("ro.config.hw_opta", "").equals("341") && SystemProperties.get("ro.config.hw_optb", "").equals("392")) ? HWDBG : false;
        IS_DOCOMO = z;
    }

    private void log(String message) {
        Rlog.d(TAG, message);
    }

    public boolean setMtuIfNeeded(LinkProperties lp, Phone phone) {
        LinkProperties linkProperties = lp;
        Phone phone2 = phone;
        if (phone2 == null || phone2.mIccRecords == null || phone2.mIccRecords.get() == null) {
            return false;
        }
        int mtu;
        int i = 1;
        try {
            Integer custMtu = (Integer) HwGetCfgFileConfig.getCfgFileData(new HwCfgKey("set_mtu", "ip", null, null, "mtu", getIPType(lp), null, null, SubscriptionManager.getSlotIndex(phone.getPhoneId())), Integer.class);
            if (custMtu != null) {
                mtu = custMtu.intValue();
                linkProperties.setMtu(mtu);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HwCfgFile:set MTU by cust to ");
                stringBuilder.append(mtu);
                log(stringBuilder.toString());
                return HWDBG;
            }
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception: read set_mtu error ");
            stringBuilder2.append(e.toString());
            log(stringBuilder2.toString());
        }
        String mccmnc = ((IccRecords) phone2.mIccRecords.get()).getOperatorNumeric();
        String plmnsConfig = System.getString(phone.getContext().getContentResolver(), "hw_set_mtu_by_mccmnc");
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("mccmnc = ");
        stringBuilder3.append(mccmnc);
        stringBuilder3.append(" plmnsConfig = ");
        stringBuilder3.append(plmnsConfig);
        log(stringBuilder3.toString());
        if (TextUtils.isEmpty(plmnsConfig) || TextUtils.isEmpty(mccmnc) || linkProperties == null) {
            return false;
        }
        String[] plmns = plmnsConfig.split(SPLIT);
        mtu = plmns.length;
        int i2 = 0;
        while (i2 < mtu) {
            String[] mcc = plmns[i2].split(",");
            if (mcc.length != 2 || TextUtils.isEmpty(mcc[0]) || TextUtils.isEmpty(mcc[i])) {
                return false;
            }
            String IPType = "";
            int pos = mcc[0].indexOf(":");
            if (pos != -1) {
                IPType = mcc[0].substring(pos + 1);
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("IPType: ");
                stringBuilder4.append(IPType);
                log(stringBuilder4.toString());
            }
            int mtuValue = Integer.parseInt(mcc[i]);
            if ("all".equals(mcc[0]) || (mcc[0].startsWith(mccmnc) && (TextUtils.isEmpty(IPType) || IPType.equals(getIPType(lp))))) {
                linkProperties.setMtu(mtuValue);
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("set MTU by cust to ");
                stringBuilder5.append(mtuValue);
                log(stringBuilder5.toString());
                return HWDBG;
            }
            i2++;
            i = 1;
        }
        return false;
    }

    private String getIPType(LinkProperties lp) {
        String result = "";
        if (lp == null) {
            return result;
        }
        if (lp.hasIPv4Address() && lp.hasGlobalIPv6Address()) {
            result = "IPV4V6";
        } else if (lp.hasIPv4Address()) {
            result = "IPV4";
        } else if (lp.hasGlobalIPv6Address()) {
            result = "IPV6";
        }
        return result;
    }

    public boolean whetherSetApnByCust(Phone phone) {
        if (phone == null || phone.mIccRecords == null || phone.mIccRecords.get() == null) {
            return false;
        }
        String mccmnc = ((IccRecords) phone.mIccRecords.get()).getOperatorNumeric();
        String plmnsConfig = System.getString(phone.getContext().getContentResolver(), "hw_set_apn_by_mccmnc");
        int dataRadioTech = phone.getServiceState().getRilDataRadioTechnology();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mccmnc = ");
        stringBuilder.append(mccmnc);
        stringBuilder.append(" plmnsConfig = ");
        stringBuilder.append(plmnsConfig);
        stringBuilder.append(" dataRadioTech = ");
        stringBuilder.append(dataRadioTech);
        log(stringBuilder.toString());
        if (TextUtils.isEmpty(plmnsConfig) || TextUtils.isEmpty(mccmnc) || dataRadioTech != 14) {
            return false;
        }
        for (String plmn : plmnsConfig.split(",")) {
            if (plmn.equals(mccmnc)) {
                return HWDBG;
            }
        }
        return false;
    }

    public boolean isNeedReMakeCapability() {
        return IS_DOCOMO;
    }

    public NetworkCapabilities getNetworkCapabilities(String[] types, NetworkCapabilities result, ApnSetting apnSetting, DcTracker dct) {
        if (apnSetting == null || result == null || types == null || dct == null) {
            return result;
        }
        boolean isDcmFotaApn = FOTA_APN.equals(apnSetting.apn);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getNetworkCapabilities isDcmFotaApn = ");
        stringBuilder.append(isDcmFotaApn);
        log(stringBuilder.toString());
        for (String type : types) {
            Object obj = -1;
            int hashCode = type.hashCode();
            if (hashCode != 42) {
                if (hashCode != 99837) {
                    if (hashCode == 3149046 && type.equals("fota")) {
                        obj = 1;
                    }
                } else if (type.equals("dun")) {
                    obj = 2;
                }
            } else if (type.equals("*")) {
                obj = null;
            }
            switch (obj) {
                case null:
                    if (IS_DOCOMO && !isDcmFotaApn) {
                        result.removeCapability(INTERNET_PCO_TYPE);
                        break;
                    }
                case 1:
                    if (IS_DOCOMO) {
                        if (!isDcmFotaApn) {
                            result.removeCapability(INTERNET_PCO_TYPE);
                            break;
                        }
                        result.addCapability(12);
                        break;
                    }
                    break;
                case 2:
                    if (!IS_DOCOMO) {
                        break;
                    }
                    ArrayList<ApnSetting> securedDunApns = dct.fetchDunApns();
                    if (securedDunApns != null && securedDunApns.size() != 0) {
                        hashCode = securedDunApns.size();
                        for (int i = 0; i < hashCode; i++) {
                            if (apnSetting.equals(securedDunApns.get(i))) {
                                result.addCapability(12);
                                break;
                            }
                        }
                        break;
                    }
                    result.addCapability(12);
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    public void clearInternetPcoValue(int profileId, Phone phone) {
        if (HW_SIM_ACTIVATION) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("profileId is : ");
            stringBuilder.append(profileId);
            log(stringBuilder.toString());
            if (profileId == INTERNET_PCO_TYPE) {
                ContentResolver mResolver = phone.getContext().getContentResolver();
                String pcoValue = getPcoValueFromSetting(mResolver)[0].concat(SPLIT).concat(DEFAULT_PCO_VALUE);
                Global.putString(mResolver, PCO_DATA, pcoValue);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setting pco values is : ");
                stringBuilder2.append(pcoValue);
                log(stringBuilder2.toString());
            }
        }
    }

    private String[] getPcoValueFromSetting(ContentResolver mResolver) {
        String[] pcoValues = Global.getString(mResolver, PCO_DATA).split(SPLIT);
        if (pcoValues == null || pcoValues.length != 2) {
            return new String[]{DEFAULT_PCO_VALUE, DEFAULT_PCO_VALUE};
        }
        return pcoValues;
    }

    public boolean isEmergencyApnSetting(ApnSetting sApnSetting) {
        if (!SystemProperties.getBoolean("ro.config.emergency_apn_handle", false)) {
            return false;
        }
        String[] types = sApnSetting.types;
        if (types != null) {
            for (String s : types) {
                if ("emergency".equals(s)) {
                    return HWDBG;
                }
            }
        }
        return false;
    }
}
