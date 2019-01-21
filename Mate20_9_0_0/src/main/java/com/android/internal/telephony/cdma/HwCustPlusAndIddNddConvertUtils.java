package com.android.internal.telephony.cdma;

import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.TelephonyProperties;
import java.util.ArrayList;

public class HwCustPlusAndIddNddConvertUtils {
    private static final boolean DBG_NUM = false;
    static final String LOG_TAG = "HwCustPlusAndIddNddConvertUtils";
    public static final String PLUS_PREFIX = "+";
    private static HwCustPlusCountryListTable plusCountryList = HwCustPlusCountryListTable.getInstance();

    private static HwCustMccIddNddSid getNetworkMccIddList() {
        String findMcc;
        HwCustMccIddNddSid mccIddNddSid = null;
        if (plusCountryList != null) {
            findMcc = SystemProperties.get(TelephonyProperties.PROPERTY_CDMA_OPERATOR_MCC, "");
            if (findMcc == null || TextUtils.isEmpty(findMcc)) {
                Rlog.e(LOG_TAG, "plus: getNetworkMccIddList could not find mcc in ril.curMcc");
                return null;
            }
            HwCustPlusCountryListTable hwCustPlusCountryListTable = plusCountryList;
            mccIddNddSid = HwCustPlusCountryListTable.getItemFromCountryListByMcc(findMcc);
        }
        findMcc = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plus: getNetworkMccIddList mccIddNddSid = ");
        stringBuilder.append(mccIddNddSid);
        Rlog.d(findMcc, stringBuilder.toString());
        return mccIddNddSid;
    }

    public static String getCurMccBySidLtmoff(String sSid) {
        String sMcc = SystemProperties.get(TelephonyProperties.PROPERTY_CDMA_OPERATOR_MCC_FROM_NW, "");
        String sLtmoff = SystemProperties.get(TelephonyProperties.PROPERTY_CDMA_TIME_LTMOFFSET, "");
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plus: getCurMccBySidLtmoff Mcc = ");
        stringBuilder.append(sMcc);
        stringBuilder.append(",Sid = ");
        stringBuilder.append(sSid);
        stringBuilder.append(",Ltmoff = ");
        stringBuilder.append(sLtmoff);
        Rlog.d(str, stringBuilder.toString());
        HwCustPlusCountryListTable hwCustPlusCountryListTable = plusCountryList;
        ArrayList<HwCustMccSidLtmOff> itemList = HwCustPlusCountryListTable.getItemsFromSidConflictTableBySid(sSid);
        if (itemList == null || itemList.size() == 0) {
            Rlog.d(LOG_TAG, "plus: no mcc_array found in ConflictTable, try to get mcc in Main Table");
            HwCustPlusCountryListTable hwCustPlusCountryListTable2 = plusCountryList;
            str = HwCustPlusCountryListTable.getMccFromMainTableBySid(sSid);
        } else {
            Rlog.d(LOG_TAG, "plus: more than 2 mcc found in ConflictTable");
            str = plusCountryList.getCcFromConflictTableByLTM(itemList, sLtmoff);
        }
        if (str == null) {
            Rlog.e(LOG_TAG, "plus: could not find mcc by sid and ltmoff, use Network Mcc anyway");
            str = sMcc;
        }
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("plus: getCurMccBySidLtmoff, mcc = ");
        stringBuilder2.append(str);
        Rlog.d(str2, stringBuilder2.toString());
        return str;
    }

    private static String removePlusAddIdd(String number, HwCustMccIddNddSid mccIddNddSid) {
        if (number == null || mccIddNddSid == null || !number.startsWith(PLUS_PREFIX)) {
            Rlog.e(LOG_TAG, "plus: removePlusAddIdd input param invalid");
            return number;
        }
        String formatNum = number;
        String sCC = mccIddNddSid.Cc;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plus: number auto format correctly, mccIddNddSid = ");
        stringBuilder.append(mccIddNddSid.toString());
        Rlog.d(str, stringBuilder.toString());
        StringBuilder stringBuilder2;
        if (formatNum.startsWith(sCC, 1)) {
            formatNum = formatNum.substring(1, formatNum.length());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(mccIddNddSid.Idd);
            stringBuilder2.append(formatNum);
            formatNum = stringBuilder2.toString();
        } else {
            formatNum = formatNum.substring(1, formatNum.length());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(mccIddNddSid.Idd);
            stringBuilder2.append(formatNum);
            formatNum = stringBuilder2.toString();
        }
        return formatNum;
    }

    public static String replacePlusCodeWithIddNdd(String number) {
        if (number == null || number.length() == 0 || !number.startsWith(PLUS_PREFIX)) {
            Rlog.w(LOG_TAG, "plus: replacePlusCodeWithIddNdd invalid number, no need to replacePlusCode");
            return number;
        }
        HwCustMccIddNddSid mccIddNddSid = getNetworkMccIddList();
        if (mccIddNddSid != null) {
            return removePlusAddIdd(number, mccIddNddSid);
        }
        Rlog.e(LOG_TAG, "plus: replacePlusCodeWithIddNdd find no operator that match the MCC");
        return number;
    }

    public static String replaceIddNddWithPlus(String number, int toa) {
        if (number == null || number.length() == 0) {
            Rlog.e(LOG_TAG, "plus: replaceIddNddWithPlus please check the param ");
            return number;
        }
        HwCustMccIddNddSid mccIddNddSid = getNetworkMccIddList();
        if (mccIddNddSid == null) {
            Rlog.e(LOG_TAG, "plus: replaceIddNddWithPlus find no operator that match the MCC ");
            return number;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plus: replaceIddNddWithPlus mccIddNddSid =");
        stringBuilder.append(mccIddNddSid);
        Rlog.d(str, stringBuilder.toString());
        int ccIndex = 0;
        if (number.startsWith(PLUS_PREFIX)) {
            ccIndex = 1;
        }
        if (number.startsWith(mccIddNddSid.Idd, ccIndex)) {
            ccIndex += mccIddNddSid.Idd.length();
            toa = 145;
        }
        return PhoneNumberUtils.stringFromStringAndTOA(number.substring(ccIndex, number.length()), toa);
    }

    private static HwCustMccIddNddSid getMccIddListForSms() {
        String mcc = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_CDMA_OPERATOR_MCC, "");
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plus: getMccIddListForSms Mcc = ");
        stringBuilder.append(mcc);
        Rlog.d(str, stringBuilder.toString());
        if (plusCountryList == null) {
            return null;
        }
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("plus: [canFormatPlusCodeForSms] Mcc = ");
        stringBuilder2.append(mcc);
        Rlog.d(str2, stringBuilder2.toString());
        if (mcc == null || mcc.length() == 0) {
            return null;
        }
        HwCustPlusCountryListTable hwCustPlusCountryListTable = plusCountryList;
        HwCustMccIddNddSid mccIddNddSid = HwCustPlusCountryListTable.getItemFromCountryListByMcc(mcc);
        str2 = LOG_TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("plus: getMccIddListForSms getItemFromCountryListByMcc mccIddNddSid = ");
        stringBuilder2.append(mccIddNddSid);
        Rlog.d(str2, stringBuilder2.toString());
        return mccIddNddSid;
    }

    public static String replacePlusCodeWithIddNddForSms(String number) {
        if (number == null || number.length() == 0 || !number.startsWith(PLUS_PREFIX)) {
            Rlog.e(LOG_TAG, "plus: replacePlusCodeWithIddNddForSms faild ,invalid param");
            return number;
        }
        HwCustMccIddNddSid mccIddNddSid = getMccIddListForSms();
        if (mccIddNddSid != null) {
            return removePlusAddIdd(number, mccIddNddSid);
        }
        Rlog.e(LOG_TAG, "plus: replacePlusCodeWithIddNddForSms faild ,mccIddNddSid is null");
        return number;
    }

    public static String replaceIddNddWithPlusForSms(String number) {
        if (number == null || number.length() == 0) {
            Rlog.d(LOG_TAG, "plus: [replaceIddNddWithPlusForSms] please check the param ");
            return number;
        }
        String formatNumber = number;
        if (!number.startsWith(PLUS_PREFIX)) {
            HwCustMccIddNddSid mccIddNddSid = getMccIddListForSms();
            if (mccIddNddSid == null) {
                Rlog.d(LOG_TAG, "plus: [replaceIddNddWithPlusForSms] find no operator that match the MCC ");
                return formatNumber;
            }
            String Idd = mccIddNddSid.Idd;
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("plus: [replaceIddNddWithPlusForSms] find match the cc, Idd = ");
            stringBuilder.append(Idd);
            Rlog.d(str, stringBuilder.toString());
            if (number.startsWith(Idd) && number.length() > Idd.length()) {
                number = number.substring(Idd.length(), number.length());
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PLUS_PREFIX);
                stringBuilder2.append(number);
                formatNumber = stringBuilder2.toString();
            }
        }
        return formatNumber;
    }
}
