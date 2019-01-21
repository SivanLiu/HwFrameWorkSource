package com.android.internal.telephony.gsm;

import android.content.Context;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.SettingsEx.Systemex;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import com.android.internal.telephony.CsgSearch;
import com.android.internal.telephony.CsgSearchFactory;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.uicc.IccRecords;
import huawei.cust.HwCfgFilePolicy;
import java.util.ArrayList;

public class HwCustHwGSMPhoneReferenceImpl extends HwCustHwGSMPhoneReference {
    private static final String LOG_TAG = "HwCustHwGSMPhoneReferenceImpl";
    private static final int format_Length = 2;
    private static final int search_Length = 4;
    private CsgSearch mCsgSrch;

    public HwCustHwGSMPhoneReferenceImpl(GsmCdmaPhone mGSMPhone) {
        super(mGSMPhone);
        if (CsgSearch.isSupportCsgSearch()) {
            this.mCsgSrch = CsgSearchFactory.createCsgSearch(mGSMPhone);
        } else {
            this.mCsgSrch = null;
        }
    }

    public String getCustOperatorNameBySpn(String rplmn, String tempName) {
        String mImsi = null;
        Context mContext = this.mPhone != null ? this.mPhone.getContext() : null;
        String custOperatorName = null;
        IccRecords r = (this.mPhone == null || this.mPhone.mIccRecords == null) ? null : (IccRecords) this.mPhone.mIccRecords.get();
        String spnName = r != null ? r.getServiceProviderName() : null;
        if (r != null) {
            mImsi = r.getIMSI();
        }
        if (mContext != null) {
            try {
                custOperatorName = Systemex.getString(mContext.getContentResolver(), "hw_cust_custOperatorName");
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hw_cust_custOperatorName = ");
                stringBuilder.append(custOperatorName);
                Rlog.d(str, stringBuilder.toString());
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "Exception when got hw_cust_custOperatorName value", e);
            }
        }
        if (TextUtils.isEmpty(rplmn) || TextUtils.isEmpty(custOperatorName) || TextUtils.isEmpty(mImsi)) {
            return tempName;
        }
        String tempName2 = tempName;
        for (String item : custOperatorName.split(";")) {
            String[] plmns = item.split(",");
            if (4 == plmns.length && rplmn.equals(plmns[1]) && mImsi.startsWith(plmns[0])) {
                if (TextUtils.isEmpty(spnName)) {
                    tempName2 = plmns[3];
                } else {
                    tempName2 = spnName.concat(plmns[2]);
                }
            }
        }
        return tempName2;
    }

    public String modifyTheFormatName(String rplmn, String tempName, String radioTechStr) {
        String str = tempName;
        String operatorName = null;
        IccRecords iccRecords = (this.mPhone == null || this.mPhone.mIccRecords == null) ? null : (IccRecords) this.mPhone.mIccRecords.get();
        IccRecords r = iccRecords;
        String mImsi = r != null ? r.getIMSI() : null;
        String modifyTheFormat = null;
        Context mContext = this.mPhone != null ? this.mPhone.getContext() : null;
        if (str != null) {
            operatorName = tempName.concat(radioTechStr);
        }
        if (mContext != null) {
            try {
                modifyTheFormat = Systemex.getString(mContext.getContentResolver(), "hw_cust_modifytheformat");
                String str2 = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hw_cust_modifytheformat = ");
                stringBuilder.append(modifyTheFormat);
                Rlog.d(str2, stringBuilder.toString());
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "Exception when got hw_cust_modifytheformat value", e);
            }
        }
        String str3;
        String str4;
        if (TextUtils.isEmpty(rplmn) || TextUtils.isEmpty(modifyTheFormat) || TextUtils.isEmpty(mImsi)) {
            str3 = rplmn;
            str4 = radioTechStr;
            return operatorName;
        }
        String operatorName2 = operatorName;
        for (String item : modifyTheFormat.split(";")) {
            String[] plmns = item.split(",");
            if (2 == plmns.length) {
                if (rplmn.equals(plmns[1]) && mImsi.startsWith(plmns[0])) {
                    if (" 4G".equals(radioTechStr)) {
                        operatorName2 = str.concat(" LTE");
                    }
                }
            } else {
                str3 = rplmn;
            }
            str4 = radioTechStr;
        }
        str3 = rplmn;
        str4 = radioTechStr;
        return operatorName2;
    }

    public ArrayList<OperatorInfo> filterActAndRepeatedItems(ArrayList<OperatorInfo> searchResult) {
        ArrayList arrayList = searchResult;
        boolean removeActState = false;
        boolean hasHwCfgConfig = false;
        try {
            if (this.mPhone != null) {
                Boolean removeAct = (Boolean) HwCfgFilePolicy.getValue("remove_act", SubscriptionManager.getSlotIndex(this.mPhone.getPhoneId()), Boolean.class);
                if (removeAct != null) {
                    removeActState = removeAct.booleanValue();
                    hasHwCfgConfig = true;
                }
            }
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Exception when got remove_act value error:", e);
        }
        if (hasHwCfgConfig && !removeActState) {
            return arrayList;
        }
        if (!SystemProperties.getBoolean("ro.config.hw_not_show_act", false) && !removeActState) {
            return arrayList;
        }
        String radioTechStr = "";
        ArrayList<OperatorInfo> custResults = new ArrayList();
        if (arrayList == null) {
            return custResults;
        }
        int j = 0;
        int list_size = searchResult.size();
        while (j < list_size) {
            String radioTechStr2;
            boolean removeActState2;
            ArrayList<OperatorInfo> arrayList2;
            OperatorInfo operatorInfo = (OperatorInfo) arrayList.get(j);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("filterActAndRepeatedItems: operatorInfo = ");
            stringBuilder.append(operatorInfo);
            Rlog.d(str, stringBuilder.toString());
            str = operatorInfo.getOperatorAlphaLong();
            int lastSpaceIndexInLongName = operatorInfo.getOperatorAlphaLong().lastIndexOf(32);
            if (-1 != lastSpaceIndexInLongName) {
                radioTechStr = operatorInfo.getOperatorAlphaLong().substring(lastSpaceIndexInLongName);
                if (" 2G".equals(radioTechStr) || " 3G".equals(radioTechStr) || " 4G".equals(radioTechStr)) {
                    str = operatorInfo.getOperatorAlphaLong().replace(radioTechStr, "");
                }
            }
            OperatorInfo rsltInfo = null;
            String NumericRslt = operatorInfo.getOperatorNumeric();
            String NumericRsltWithoutAct = operatorInfo.getOperatorNumericWithoutAct();
            boolean isFound = false;
            int length = custResults.size();
            int i = 0;
            while (true) {
                radioTechStr2 = radioTechStr;
                int i2 = i;
                int i3;
                if (i2 >= length) {
                    removeActState2 = removeActState;
                    i3 = length;
                    break;
                }
                OperatorInfo info = (OperatorInfo) custResults.get(i2);
                String custNumeric = info.getOperatorNumeric();
                removeActState2 = removeActState;
                removeActState = info.getOperatorNumericWithoutAct();
                if (str != null) {
                    i3 = length;
                    if (str.equals(info.getOperatorAlphaLong()) && removeActState && removeActState.equals(NumericRsltWithoutAct)) {
                        if (custNumeric == null || custNumeric.compareTo(NumericRslt) >= 0) {
                            String str2 = custNumeric;
                            Object obj = removeActState;
                        } else {
                            String str3 = LOG_TAG;
                            info = new StringBuilder();
                            String custNumericWithoutAct = removeActState;
                            info.append("filterActAndRepeatedItems: custNumeric=");
                            info.append(custNumeric);
                            info.append(" NumericRslt=");
                            info.append(NumericRslt);
                            Rlog.d(str3, info.toString());
                            custResults.remove(i2);
                            rsltInfo = new OperatorInfo(str, operatorInfo.getOperatorAlphaShort(), operatorInfo.getOperatorNumeric(), operatorInfo.getState());
                        }
                        isFound = true;
                    }
                } else {
                    i3 = length;
                }
                i = i2 + 1;
                radioTechStr = radioTechStr2;
                removeActState = removeActState2;
                length = i3;
                arrayList2 = searchResult;
            }
            if (!isFound) {
                rsltInfo = new OperatorInfo(str, operatorInfo.getOperatorAlphaShort(), operatorInfo.getOperatorNumeric(), operatorInfo.getState());
            }
            radioTechStr = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("filterActAndRepeatedItems: rsltInfo = ");
            stringBuilder2.append(rsltInfo);
            Rlog.d(radioTechStr, stringBuilder2.toString());
            if (rsltInfo != null) {
                custResults.add(rsltInfo);
            }
            j++;
            radioTechStr = radioTechStr2;
            removeActState = removeActState2;
            arrayList2 = searchResult;
        }
        return custResults;
    }

    public void selectCsgNetworkManually(Message response) {
        if (this.mCsgSrch != null) {
            this.mCsgSrch.selectCsgNetworkManually(response);
        }
    }

    public void judgeToLaunchCsgPeriodicSearchTimer() {
        if (this.mCsgSrch != null) {
            this.mCsgSrch.judgeToLaunchCsgPeriodicSearchTimer();
        }
    }

    public void registerForCsgRecordsLoadedEvent() {
        if (this.mCsgSrch != null) {
            this.mCsgSrch.registerForCsgRecordsLoadedEvent();
        }
    }

    public void unregisterForCsgRecordsLoadedEvent() {
        if (this.mCsgSrch != null) {
            this.mCsgSrch.unregisterForCsgRecordsLoadedEvent();
        }
    }
}
