package com.android.internal.telephony;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Binder;
import android.os.Build;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.HbpcdLookup.MccIdd;
import com.android.internal.telephony.HbpcdLookup.MccLookup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SmsNumberUtils {
    private static int[] ALL_COUNTRY_CODES = null;
    private static final int CDMA_HOME_NETWORK = 1;
    private static final int CDMA_ROAMING_NETWORK = 2;
    private static final boolean DBG = Build.IS_DEBUGGABLE;
    private static final int GSM_UMTS_NETWORK = 0;
    private static HashMap<String, ArrayList<String>> IDDS_MAPS = new HashMap();
    private static int MAX_COUNTRY_CODES_LENGTH = 0;
    private static final int MIN_COUNTRY_AREA_LOCAL_LENGTH = 10;
    private static final int NANP_CC = 1;
    private static final String NANP_IDD = "011";
    private static final int NANP_LONG_LENGTH = 11;
    private static final int NANP_MEDIUM_LENGTH = 10;
    private static final String NANP_NDD = "1";
    private static final int NANP_SHORT_LENGTH = 7;
    private static final int NP_CC_AREA_LOCAL = 104;
    private static final int NP_HOMEIDD_CC_AREA_LOCAL = 101;
    private static final int NP_INTERNATIONAL_BEGIN = 100;
    private static final int NP_LOCALIDD_CC_AREA_LOCAL = 103;
    private static final int NP_NANP_AREA_LOCAL = 2;
    private static final int NP_NANP_BEGIN = 1;
    private static final int NP_NANP_LOCAL = 1;
    private static final int NP_NANP_LOCALIDD_CC_AREA_LOCAL = 5;
    private static final int NP_NANP_NBPCD_CC_AREA_LOCAL = 4;
    private static final int NP_NANP_NBPCD_HOMEIDD_CC_AREA_LOCAL = 6;
    private static final int NP_NANP_NDD_AREA_LOCAL = 3;
    private static final int NP_NBPCD_CC_AREA_LOCAL = 102;
    private static final int NP_NBPCD_HOMEIDD_CC_AREA_LOCAL = 100;
    private static final int NP_NONE = 0;
    private static final String PLUS_SIGN = "+";
    private static final String TAG = "SmsNumberUtils";

    private static class NumberEntry {
        public String IDD;
        public int countryCode;
        public String number;

        public NumberEntry(String number) {
            this.number = number;
        }
    }

    private static String formatNumber(Context context, String number, String activeMcc, int networkType) {
        if (number == null) {
            throw new IllegalArgumentException("number is null");
        } else if (activeMcc == null || activeMcc.trim().length() == 0) {
            throw new IllegalArgumentException("activeMcc is null or empty!");
        } else {
            String networkPortionNumber = PhoneNumberUtils.extractNetworkPortion(number);
            if (networkPortionNumber == null || networkPortionNumber.length() == 0) {
                throw new IllegalArgumentException("Number is invalid!");
            }
            StringBuilder stringBuilder;
            NumberEntry numberEntry = new NumberEntry(networkPortionNumber);
            ArrayList<String> allIDDs = getAllIDDs(context, activeMcc);
            int nanpState = checkNANP(numberEntry, allIDDs);
            if (DBG) {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("NANP type: ");
                stringBuilder.append(getNumberPlanType(nanpState));
                Rlog.d(str, stringBuilder.toString());
            }
            if (nanpState == 1 || nanpState == 2 || nanpState == 3) {
                return networkPortionNumber;
            }
            if (nanpState != 4) {
                int iddLength;
                String str2;
                int i = 0;
                if (nanpState == 5) {
                    if (networkType == 1) {
                        return networkPortionNumber;
                    }
                    if (networkType == 0) {
                        if (numberEntry.IDD != null) {
                            i = numberEntry.IDD.length();
                        }
                        iddLength = i;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(PLUS_SIGN);
                        stringBuilder.append(networkPortionNumber.substring(iddLength));
                        return stringBuilder.toString();
                    } else if (networkType == 2) {
                        if (numberEntry.IDD != null) {
                            i = numberEntry.IDD.length();
                        }
                        return networkPortionNumber.substring(i);
                    }
                }
                int internationalState = checkInternationalNumberPlan(context, numberEntry, allIDDs, NANP_IDD);
                if (DBG) {
                    str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("International type: ");
                    stringBuilder2.append(getNumberPlanType(internationalState));
                    Rlog.d(str2, stringBuilder2.toString());
                }
                str2 = null;
                switch (internationalState) {
                    case 100:
                        if (networkType == 0) {
                            str2 = networkPortionNumber.substring(1);
                            break;
                        }
                        break;
                    case 101:
                        str2 = networkPortionNumber;
                        break;
                    case 102:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(NANP_IDD);
                        stringBuilder.append(networkPortionNumber.substring(1));
                        str2 = stringBuilder.toString();
                        break;
                    case NP_LOCALIDD_CC_AREA_LOCAL /*103*/:
                        if (networkType == 0 || networkType == 2) {
                            if (numberEntry.IDD != null) {
                                i = numberEntry.IDD.length();
                            }
                            iddLength = i;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(NANP_IDD);
                            stringBuilder.append(networkPortionNumber.substring(iddLength));
                            str2 = stringBuilder.toString();
                            break;
                        }
                    case 104:
                        int countryCode = numberEntry.countryCode;
                        if (!(inExceptionListForNpCcAreaLocal(numberEntry) || networkPortionNumber.length() < 11 || countryCode == 1)) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(NANP_IDD);
                            stringBuilder3.append(networkPortionNumber);
                            str2 = stringBuilder3.toString();
                            break;
                        }
                    default:
                        if (networkPortionNumber.startsWith(PLUS_SIGN) && (networkType == 1 || networkType == 2)) {
                            if (!networkPortionNumber.startsWith("+011")) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(NANP_IDD);
                                stringBuilder.append(networkPortionNumber.substring(1));
                                str2 = stringBuilder.toString();
                                break;
                            }
                            str2 = networkPortionNumber.substring(1);
                            break;
                        }
                }
                if (str2 == null) {
                    str2 = networkPortionNumber;
                }
                return str2;
            } else if (networkType == 1 || networkType == 2) {
                return networkPortionNumber.substring(1);
            } else {
                return networkPortionNumber;
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0050, code skipped:
            if (r10 != null) goto L_0x0052;
     */
    /* JADX WARNING: Missing block: B:17:0x0052, code skipped:
            r10.close();
     */
    /* JADX WARNING: Missing block: B:22:0x0060, code skipped:
            if (r10 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:23:0x0063, code skipped:
            IDDS_MAPS.put(r12, r0);
     */
    /* JADX WARNING: Missing block: B:24:0x006a, code skipped:
            if (DBG == false) goto L_0x008a;
     */
    /* JADX WARNING: Missing block: B:25:0x006c, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("MCC = ");
            r4.append(r12);
            r4.append(", all IDDs = ");
            r4.append(r0);
            android.telephony.Rlog.d(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:26:0x008a, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static ArrayList<String> getAllIDDs(Context context, String mcc) {
        ArrayList<String> allIDDs = (ArrayList) IDDS_MAPS.get(mcc);
        if (allIDDs != null) {
            return allIDDs;
        }
        allIDDs = new ArrayList();
        String[] projection = new String[]{MccIdd.IDD, "MCC"};
        String where = null;
        String[] selectionArgs = null;
        if (mcc != null) {
            where = "MCC=?";
            selectionArgs = new String[]{mcc};
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(MccIdd.CONTENT_URI, projection, where, selectionArgs, null);
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String idd = cursor.getString(0);
                    if (!allIDDs.contains(idd)) {
                        allIDDs.add(idd);
                    }
                }
            }
        } catch (SQLException e) {
            Rlog.e(TAG, "Can't access HbpcdLookup database", e);
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static int checkNANP(NumberEntry numberEntry, ArrayList<String> allIDDs) {
        boolean isNANP = false;
        String number = numberEntry.number;
        if (number.length() == 7) {
            char firstChar = number.charAt(0);
            if (firstChar >= '2' && firstChar <= '9') {
                isNANP = true;
                for (int i = 1; i < 7; i++) {
                    if (!PhoneNumberUtils.isISODigit(number.charAt(i))) {
                        isNANP = false;
                        break;
                    }
                }
            }
            if (isNANP) {
                return 1;
            }
        } else if (number.length() == 10) {
            if (isNANP(number)) {
                return 2;
            }
        } else if (number.length() == 11) {
            if (isNANP(number)) {
                return 3;
            }
        } else if (number.startsWith(PLUS_SIGN)) {
            number = number.substring(1);
            if (number.length() == 11) {
                if (isNANP(number)) {
                    return 4;
                }
            } else if (number.startsWith(NANP_IDD) && number.length() == 14 && isNANP(number.substring(3))) {
                return 6;
            }
        } else {
            Iterator it = allIDDs.iterator();
            while (it.hasNext()) {
                String idd = (String) it.next();
                if (number.startsWith(idd)) {
                    String number2 = number.substring(idd.length());
                    if (number2 != null && number2.startsWith(String.valueOf(1)) && isNANP(number2)) {
                        numberEntry.IDD = idd;
                        return 5;
                    }
                }
            }
        }
        return 0;
    }

    private static boolean isNANP(String number) {
        if (number.length() != 10 && (number.length() != 11 || !number.startsWith("1"))) {
            return false;
        }
        if (number.length() == 11) {
            number = number.substring(1);
        }
        return PhoneNumberUtils.isNanp(number);
    }

    private static int checkInternationalNumberPlan(Context context, NumberEntry numberEntry, ArrayList<String> allIDDs, String homeIDD) {
        String number = numberEntry.number;
        int countryCode;
        int countryCode2;
        if (number.startsWith(PLUS_SIGN)) {
            String numberNoNBPCD = number.substring(1);
            if (numberNoNBPCD.startsWith(homeIDD)) {
                int countryCode3 = getCountryCode(context, numberNoNBPCD.substring(homeIDD.length()));
                countryCode = countryCode3;
                if (countryCode3 > 0) {
                    numberEntry.countryCode = countryCode;
                    return 100;
                }
            }
            countryCode2 = getCountryCode(context, numberNoNBPCD);
            countryCode = countryCode2;
            if (countryCode2 > 0) {
                numberEntry.countryCode = countryCode;
                return 102;
            }
        } else if (number.startsWith(homeIDD)) {
            countryCode2 = getCountryCode(context, number.substring(homeIDD.length()));
            countryCode = countryCode2;
            if (countryCode2 > 0) {
                numberEntry.countryCode = countryCode;
                return 101;
            }
        } else {
            Iterator it = allIDDs.iterator();
            while (it.hasNext()) {
                String exitCode = (String) it.next();
                if (number.startsWith(exitCode)) {
                    int countryCode4 = getCountryCode(context, number.substring(exitCode.length()));
                    countryCode = countryCode4;
                    if (countryCode4 > 0) {
                        numberEntry.countryCode = countryCode;
                        numberEntry.IDD = exitCode;
                        return NP_LOCALIDD_CC_AREA_LOCAL;
                    }
                }
            }
            if (!number.startsWith(ProxyController.MODEM_0)) {
                int countryCode5 = getCountryCode(context, number);
                countryCode = countryCode5;
                if (countryCode5 > 0) {
                    numberEntry.countryCode = countryCode;
                    return 104;
                }
            }
        }
        return 0;
    }

    private static int getCountryCode(Context context, String number) {
        if (number.length() >= 10) {
            int[] allCCs = getAllCountryCodes(context);
            if (allCCs == null) {
                return -1;
            }
            int i;
            int[] ccArray = new int[MAX_COUNTRY_CODES_LENGTH];
            for (i = 0; i < MAX_COUNTRY_CODES_LENGTH; i++) {
                ccArray[i] = Integer.parseInt(number.substring(0, i + 1));
            }
            for (int tempCC : allCCs) {
                for (int j = 0; j < MAX_COUNTRY_CODES_LENGTH; j++) {
                    if (tempCC == ccArray[j]) {
                        if (DBG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Country code = ");
                            stringBuilder.append(tempCC);
                            Rlog.d(str, stringBuilder.toString());
                        }
                        return tempCC;
                    }
                }
            }
        }
        return -1;
    }

    /* JADX WARNING: Missing block: B:15:0x0051, code skipped:
            if (r0 != null) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:16:0x0053, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:21:0x0061, code skipped:
            if (r0 == null) goto L_0x0064;
     */
    /* JADX WARNING: Missing block: B:23:0x0066, code skipped:
            return ALL_COUNTRY_CODES;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int[] getAllCountryCodes(Context context) {
        if (ALL_COUNTRY_CODES != null) {
            return ALL_COUNTRY_CODES;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(MccLookup.CONTENT_URI, new String[]{MccLookup.COUNTRY_CODE}, null, null, null);
            if (cursor.getCount() > 0) {
                ALL_COUNTRY_CODES = new int[cursor.getCount()];
                int i = 0;
                while (cursor.moveToNext()) {
                    int countryCode = cursor.getInt(0);
                    int i2 = i + 1;
                    ALL_COUNTRY_CODES[i] = countryCode;
                    i = String.valueOf(countryCode).trim().length();
                    if (i > MAX_COUNTRY_CODES_LENGTH) {
                        MAX_COUNTRY_CODES_LENGTH = i;
                    }
                    i = i2;
                }
            }
        } catch (SQLException e) {
            Rlog.e(TAG, "Can't access HbpcdLookup database", e);
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static boolean inExceptionListForNpCcAreaLocal(NumberEntry numberEntry) {
        int countryCode = numberEntry.countryCode;
        return numberEntry.number.length() == 12 && (countryCode == 7 || countryCode == 20 || countryCode == 65 || countryCode == 90);
    }

    private static String getNumberPlanType(int state) {
        String numberPlanType = new StringBuilder();
        numberPlanType.append("Number Plan type (");
        numberPlanType.append(state);
        numberPlanType.append("): ");
        numberPlanType = numberPlanType.toString();
        if (state == 1) {
            return "NP_NANP_LOCAL";
        }
        if (state == 2) {
            return "NP_NANP_AREA_LOCAL";
        }
        if (state == 3) {
            return "NP_NANP_NDD_AREA_LOCAL";
        }
        if (state == 4) {
            return "NP_NANP_NBPCD_CC_AREA_LOCAL";
        }
        if (state == 5) {
            return "NP_NANP_LOCALIDD_CC_AREA_LOCAL";
        }
        if (state == 6) {
            return "NP_NANP_NBPCD_HOMEIDD_CC_AREA_LOCAL";
        }
        if (state == 100) {
            return "NP_NBPCD_HOMEIDD_CC_AREA_LOCAL";
        }
        if (state == 101) {
            return "NP_HOMEIDD_CC_AREA_LOCAL";
        }
        if (state == 102) {
            return "NP_NBPCD_CC_AREA_LOCAL";
        }
        if (state == NP_LOCALIDD_CC_AREA_LOCAL) {
            return "NP_LOCALIDD_CC_AREA_LOCAL";
        }
        if (state == 104) {
            return "NP_CC_AREA_LOCAL";
        }
        return "Unknown type";
    }

    public static String filterDestAddr(Phone phone, String destAddr) {
        if (DBG) {
            Rlog.d(TAG, "enter filterDestAddr. destAddr=\"xxxx\"");
        }
        if (destAddr == null || !PhoneNumberUtils.isGlobalPhoneNumber(destAddr)) {
            Rlog.w(TAG, "destAddr xxxx is not a global phone number! Nothing changed.");
            return destAddr;
        }
        String networkOperator = TelephonyManager.from(phone.getContext()).getNetworkOperator(phone.getSubId());
        String result = null;
        if (needToConvert(phone)) {
            int networkType = getNetworkType(phone);
            if (!(networkType == -1 || TextUtils.isEmpty(networkOperator))) {
                String networkMcc = networkOperator.substring(null, 3);
                if (networkMcc != null && networkMcc.trim().length() > 0) {
                    result = formatNumber(phone.getContext(), destAddr, networkMcc, networkType);
                }
            }
        }
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("destAddr is ");
            stringBuilder.append(result != null ? "formatted." : "not formatted.");
            Rlog.d(str, stringBuilder.toString());
            Rlog.d(TAG, "leave filterDestAddr, new destAddr=\"xxxx\"");
        }
        return result != null ? result : destAddr;
    }

    private static int getNetworkType(Phone phone) {
        int phoneType = phone.getPhoneType();
        if (phoneType == 1) {
            return 0;
        }
        if (phoneType == 2) {
            if (isInternationalRoaming(phone)) {
                return 2;
            }
            return 1;
        } else if (!DBG) {
            return -1;
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("warning! unknown mPhoneType value=");
            stringBuilder.append(phoneType);
            Rlog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    private static boolean isInternationalRoaming(Phone phone) {
        String operatorIsoCountry = TelephonyManager.from(phone.getContext()).getNetworkCountryIsoForPhone(phone.getPhoneId());
        String simIsoCountry = TelephonyManager.from(phone.getContext()).getSimCountryIsoForPhone(phone.getPhoneId());
        boolean internationalRoaming = (TextUtils.isEmpty(operatorIsoCountry) || TextUtils.isEmpty(simIsoCountry) || simIsoCountry.equals(operatorIsoCountry)) ? false : true;
        if (!internationalRoaming) {
            return internationalRoaming;
        }
        if ("us".equals(simIsoCountry)) {
            return 1 ^ "vi".equals(operatorIsoCountry);
        }
        if ("vi".equals(simIsoCountry)) {
            return 1 ^ "us".equals(operatorIsoCountry);
        }
        return internationalRoaming;
    }

    private static boolean needToConvert(Phone phone) {
        long identity = Binder.clearCallingIdentity();
        try {
            CarrierConfigManager configManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
            if (configManager != null) {
                PersistableBundle bundle = configManager.getConfig();
                if (bundle != null) {
                    boolean z = bundle.getBoolean("sms_requires_destination_number_conversion_bool");
                    return z;
                }
            }
            Binder.restoreCallingIdentity(identity);
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static boolean compareGid1(Phone phone, String serviceGid1) {
        String gid1 = phone.getGroupIdLevel1();
        boolean ret = true;
        if (TextUtils.isEmpty(serviceGid1)) {
            if (DBG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("compareGid1 serviceGid is empty, return ");
                stringBuilder.append(true);
                Rlog.d(str, stringBuilder.toString());
            }
            return true;
        }
        String str2;
        StringBuilder stringBuilder2;
        int gid_length = serviceGid1.length();
        if (gid1 == null || gid1.length() < gid_length || !gid1.substring(0, gid_length).equalsIgnoreCase(serviceGid1)) {
            if (DBG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" gid1 ");
                stringBuilder2.append(gid1);
                stringBuilder2.append(" serviceGid1 ");
                stringBuilder2.append(serviceGid1);
                Rlog.d(str2, stringBuilder2.toString());
            }
            ret = false;
        }
        if (DBG) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("compareGid1 is ");
            stringBuilder2.append(ret ? "Same" : "Different");
            Rlog.d(str2, stringBuilder2.toString());
        }
        return ret;
    }
}
