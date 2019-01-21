package com.android.internal.telephony.cdma;

import android.telephony.Rlog;
import android.util.Log;
import android.util.SparseIntArray;
import java.util.ArrayList;
import java.util.Iterator;

public class HwCustPlusCountryListTable {
    private static final boolean DBG = true;
    private static SparseIntArray FindOutSidMap = new SparseIntArray();
    static final String LOG_TAG = "CDMA-HwCustPlusCountryListTable";
    protected static final HwCustMccIddNddSid[] MccIddNddSidMap = HwCustTelephonyPlusCode.MccIddNddSidMap_support;
    protected static final HwCustMccSidLtmOff[] MccSidLtmOffMap = HwCustTelephonyPlusCode.MccSidLtmOffMap_support;
    static final int PARAM_FOR_OFFSET = 2;
    static final Object sInstSync = new Object();
    private static final HwCustPlusCountryListTable sInstance = new HwCustPlusCountryListTable();

    public static HwCustPlusCountryListTable getInstance() {
        return sInstance;
    }

    private HwCustPlusCountryListTable() {
    }

    public static HwCustMccIddNddSid getItemFromCountryListByMcc(String sMcc) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plus: getItemFromCountryListByMcc mcc = ");
        stringBuilder.append(sMcc);
        Rlog.d(str, stringBuilder.toString());
        int mcc = getIntFromString(sMcc);
        for (HwCustMccIddNddSid item : MccIddNddSidMap) {
            if (mcc == item.Mcc) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("plus: Now find mccIddNddSid = ");
                stringBuilder2.append(item);
                Rlog.d(str2, stringBuilder2.toString());
                return item;
            }
        }
        Rlog.e(LOG_TAG, "plus: can't find one that match the Mcc");
        return null;
    }

    public static int getIntFromString(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Rlog.e(LOG_TAG, Log.getStackTraceString(e));
            return -1;
        }
    }

    public static ArrayList<HwCustMccSidLtmOff> getItemsFromSidConflictTableBySid(String sSid) {
        int sid = getIntFromString(sSid);
        ArrayList<HwCustMccSidLtmOff> itemList = new ArrayList();
        for (HwCustMccSidLtmOff item : MccSidLtmOffMap) {
            if (sid == item.Sid) {
                itemList.add(item);
            }
        }
        return itemList;
    }

    public static String getMccFromMainTableBySid(String sSid) {
        int sid = getIntFromString(sSid);
        String mcc = null;
        for (HwCustMccIddNddSid item : MccIddNddSidMap) {
            if (sid <= item.SidMax && sid >= item.SidMin) {
                mcc = Integer.toString(item.Mcc);
                break;
            }
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plus: getMccFromMainTableBySid mcc = ");
        stringBuilder.append(mcc);
        Rlog.d(str, stringBuilder.toString());
        return mcc;
    }

    public String getCcFromConflictTableByLTM(ArrayList<HwCustMccSidLtmOff> itemList, String sLtm_off) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plus:  getCcFromConflictTableByLTM sLtm_off = ");
        stringBuilder.append(sLtm_off);
        Rlog.d(str, stringBuilder.toString());
        if (itemList == null || itemList.size() == 0) {
            Rlog.e(LOG_TAG, "plus: [getCcFromConflictTableByLTM] please check the param ");
            return null;
        }
        String FindMcc = null;
        try {
            int ltm_off = Integer.parseInt(sLtm_off);
            Iterator it = itemList.iterator();
            while (it.hasNext()) {
                HwCustMccSidLtmOff item = (HwCustMccSidLtmOff) it.next();
                int min = item.LtmOffMin * 2;
                if (ltm_off <= item.LtmOffMax * 2 && ltm_off >= min) {
                    FindMcc = Integer.toString(item.Mcc);
                    break;
                }
            }
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("plus: find one that match the ltm_off mcc = ");
            stringBuilder2.append(FindMcc);
            Rlog.d(str2, stringBuilder2.toString());
            return FindMcc;
        } catch (NumberFormatException e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            return null;
        }
    }
}
