package huawei.android.telephony;

import android.database.Cursor;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.i18n.phonenumbers.CountryCodeToRegionCodeMapUtils;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.internal.telephony.HwTelephonyProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CallerInfoHW implements TelephonyInterfacesHW {
    private static final String CHINA_AREACODE = "0";
    private static final String CHINA_OPERATOR_MCC = "460";
    private static final int CN_FIXED_NUMBER_WITH_AREA_CODE_MIN_LEN = 9;
    private static final String CN_MPN_PATTERN = "^(1)\\d{10}$";
    private static final int CN_NUM_MATCH = 11;
    protected static final boolean DBG = false;
    private static final String FIXED_NUMBER_TOP2_TOKEN1 = "01";
    private static final String FIXED_NUMBER_TOP2_TOKEN2 = "02";
    private static final String[] INTERNATIONAL_PREFIX = new String[]{"+00", "+", "00"};
    private static final String[] IPHEAD = new String[]{"10193", "11808", "12593", "17900", "17901", "17908", "17909", "17910", "17911", "17931", "17950", "17951", "17960", "17968", "17969", "96435"};
    private static final int IPHEAD_LENTH = 5;
    private static final boolean IS_SUPPORT_DUAL_NUMBER = SystemProperties.getBoolean("ro.config.hw_dual_number", false);
    public static final int MIN_MATCH = 7;
    private static final String[] NORMAL_PREFIX_MCC = new String[]{"602", "722"};
    private static final String TAG = "CallerInfo";
    private static CallerInfoHW sCallerInfoHwInstance = null;
    private static PhoneNumberUtil sInstance = PhoneNumberUtil.getInstance();
    private boolean IS_CHINA_TELECOM;
    private boolean IS_MIIT_NUM_MATCH;
    private final int NUM_LONG_CUST;
    private final int NUM_SHORT_CUST;
    private final Map<Integer, ArrayList<String>> chineseFixNumberAreaCodeMap;
    private int configMatchNum = SystemProperties.getInt("ro.config.hwft_MatchNum", 7);
    private int configMatchNumShort;
    private final Map<Integer, List<String>> countryCallingCodeToRegionCodeMap;
    private int countryCodeforCN;
    private String mNetworkOperator;
    private int mSimNumLong;
    private int mSimNumShort;

    public CallerInfoHW() {
        int i = 7;
        if (this.configMatchNum >= 7) {
            i = this.configMatchNum;
        }
        this.NUM_LONG_CUST = i;
        this.configMatchNumShort = SystemProperties.getInt("ro.config.hwft_MatchNumShort", this.NUM_LONG_CUST);
        this.NUM_SHORT_CUST = this.configMatchNumShort >= this.NUM_LONG_CUST ? this.NUM_LONG_CUST : this.configMatchNumShort;
        this.mSimNumLong = this.NUM_LONG_CUST;
        this.mSimNumShort = this.NUM_SHORT_CUST;
        boolean z = SystemProperties.get("ro.config.hw_opta", "0").equals("92") && SystemProperties.get("ro.config.hw_optb", "0").equals("156");
        this.IS_CHINA_TELECOM = z;
        this.IS_MIIT_NUM_MATCH = SystemProperties.getBoolean("ro.config.miit_number_match", false);
        this.mNetworkOperator = null;
        this.countryCodeforCN = sInstance.getCountryCodeForRegion("CN");
        this.countryCallingCodeToRegionCodeMap = CountryCodeToRegionCodeMapUtils.getCountryCodeToRegionCodeMap();
        this.chineseFixNumberAreaCodeMap = ChineseFixNumberAreaCodeMap.getChineseFixNumberAreaCodeMap();
    }

    public static synchronized CallerInfoHW getInstance() {
        CallerInfoHW callerInfoHW;
        synchronized (CallerInfoHW.class) {
            if (sCallerInfoHwInstance == null) {
                sCallerInfoHwInstance = new CallerInfoHW();
            }
            callerInfoHW = sCallerInfoHwInstance;
        }
        return callerInfoHW;
    }

    public String getCountryIsoFromDbNumber(String number) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCountryIsoFromDbNumber(), number: ");
        stringBuilder.append(number);
        logd(stringBuilder.toString());
        if (TextUtils.isEmpty(number)) {
            return null;
        }
        int len = getIntlPrefixLength(number);
        if (len > 0) {
            String tmpNumber = number.substring(len);
            for (Integer countrycode : this.countryCallingCodeToRegionCodeMap.keySet()) {
                int countrycode2 = countrycode.intValue();
                if (tmpNumber.startsWith(Integer.toString(countrycode2))) {
                    String countryIso = sInstance.getRegionCodeForCountryCode(countrycode2);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getCountryIsoFromDbNumber(), find matched country code: ");
                    stringBuilder2.append(countrycode2);
                    stringBuilder2.append(", and country iso: ");
                    stringBuilder2.append(countryIso);
                    logd(stringBuilder2.toString());
                    return countryIso;
                }
            }
            logd("getCountryIsoFromDbNumber(), no matched country code, returns null");
        }
        return null;
    }

    public boolean compareNums(String num1, String netIso1, String num2, String netIso2) {
        String num12 = num1;
        String str = netIso1;
        String num22 = num2;
        String str2 = netIso2;
        String num1Prefix = null;
        String num2Prefix = null;
        String num1AreaCode = null;
        String num2AreaCode = null;
        int NUM_LONG = this.NUM_LONG_CUST;
        int NUM_SHORT = this.NUM_SHORT_CUST;
        int num1Len = 0;
        int num2Len;
        boolean z;
        String str3;
        String str4;
        if (num12 == null) {
            num2Len = 0;
            z = false;
            str3 = null;
            str4 = null;
        } else if (num22 == null) {
            num2Len = 0;
            z = false;
            str3 = null;
            str4 = null;
        } else {
            int num2Len2;
            StringBuilder stringBuilder = new StringBuilder();
            num2Len = 0;
            stringBuilder.append("compareNums, num1 = ");
            stringBuilder.append(num12);
            stringBuilder.append(", netIso1 = ");
            stringBuilder.append(str);
            stringBuilder.append(", num2 = ");
            stringBuilder.append(num22);
            stringBuilder.append(", netIso2 = ");
            stringBuilder.append(str2);
            logd(stringBuilder.toString());
            if (SystemProperties.getInt("ro.config.hwft_MatchNum", 0) == 0) {
                boolean numMatch = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH, 7);
                num2Len2 = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_SHORT, numMatch);
                z = false;
                int i = numMatch < true ? 7 : numMatch;
                this.mSimNumLong = i;
                NUM_LONG = i;
                i = num2Len2 >= NUM_LONG ? NUM_LONG : num2Len2;
                this.mSimNumShort = i;
                NUM_SHORT = i;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("compareNums, after setprop NUM_LONG = ");
                stringBuilder2.append(NUM_LONG);
                stringBuilder2.append(", NUM_SHORT = ");
                stringBuilder2.append(NUM_SHORT);
                logd(stringBuilder2.toString());
            } else {
                z = false;
            }
            if (num12.indexOf(64) < 0) {
                num12 = PhoneNumberUtils.stripSeparators(num1);
            }
            if (num22.indexOf(64) < 0) {
                num22 = PhoneNumberUtils.stripSeparators(num2);
            }
            num12 = formatedForDualNumber(num12);
            num22 = formatedForDualNumber(num22);
            if (this.IS_CHINA_TELECOM && num12.startsWith("**133") && num12.endsWith("#")) {
                num12 = num12.substring(0, num12.length() - 1);
                logd("compareNums, num1 startsWith **133 && endsWith #");
            }
            if (this.IS_CHINA_TELECOM && num22.startsWith("**133") && num22.endsWith("#")) {
                num22 = num22.substring(0, num22.length() - 1);
                logd("compareNums, num2 startsWith **133 && endsWith #");
            }
            if (num12.equals(num22)) {
                logd("compareNums, full compare returns true.");
                return true;
            }
            String formattedNum1;
            StringBuilder stringBuilder3;
            StringBuilder stringBuilder4;
            String origNum1 = num12;
            boolean ret = num22;
            if (!TextUtils.isEmpty(netIso1)) {
                formattedNum1 = PhoneNumberUtils.formatNumberToE164(num12, str.toUpperCase(Locale.US));
                if (formattedNum1 != null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("compareNums, formattedNum1: ");
                    stringBuilder3.append(formattedNum1);
                    stringBuilder3.append(", with netIso1: ");
                    stringBuilder3.append(str);
                    logd(stringBuilder3.toString());
                    num12 = formattedNum1;
                }
            }
            if (TextUtils.isEmpty(netIso2) == 0) {
                num2Len2 = PhoneNumberUtils.formatNumberToE164(num22, str2.toUpperCase(Locale.US));
                if (num2Len2 != 0) {
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("compareNums, formattedNum2: ");
                    stringBuilder4.append(num2Len2);
                    stringBuilder4.append(", with netIso2: ");
                    stringBuilder4.append(str2);
                    logd(stringBuilder4.toString());
                    num22 = num2Len2;
                }
            }
            if (num12.equals(num22) != 0) {
                logd("compareNums, full compare for formatted number returns true.");
                return true;
            }
            int numMatchShort;
            num2Len2 = getIntlPrefixAndCCLen(num12);
            if (num2Len2 > 0) {
                num1Prefix = num12.substring(0, num2Len2);
                num12 = num12.substring(num2Len2);
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("compareNums, num1 after remove prefix: ");
                stringBuilder4.append(num12);
                stringBuilder4.append(", num1Prefix: ");
                stringBuilder4.append(num1Prefix);
                logd(stringBuilder4.toString());
            }
            int countryCodeLen2 = getIntlPrefixAndCCLen(num22);
            int countryCodeLen1;
            if (countryCodeLen2 > 0) {
                num2Prefix = num22.substring(0, countryCodeLen2);
                num22 = num22.substring(countryCodeLen2);
                stringBuilder3 = new StringBuilder();
                countryCodeLen1 = num2Len2;
                stringBuilder3.append("compareNums, num2 after remove prefix: ");
                stringBuilder3.append(num22);
                stringBuilder3.append(", num2Prefix: ");
                stringBuilder3.append(num2Prefix);
                logd(stringBuilder3.toString());
            } else {
                countryCodeLen1 = num2Len2;
            }
            if (isRoamingCountryNumberByPrefix(num1Prefix, str) == 0 && isRoamingCountryNumberByPrefix(num2Prefix, str2) == 0) {
                int i2 = countryCodeLen2;
            } else {
                logd("compareNums, num1 or num2 belong to roaming country");
                num2Len2 = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_ROAMING, 7);
                numMatchShort = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_SHORT_ROAMING, num2Len2);
                NUM_LONG = num2Len2 < 7 ? 7 : num2Len2;
                NUM_SHORT = numMatchShort >= NUM_LONG ? NUM_LONG : numMatchShort;
                stringBuilder4 = new StringBuilder();
                int numMatch2 = num2Len2;
                stringBuilder4.append("compareNums, roaming prop NUM_LONG = ");
                stringBuilder4.append(NUM_LONG);
                stringBuilder4.append(", NUM_SHORT = ");
                stringBuilder4.append(NUM_SHORT);
                logd(stringBuilder4.toString());
            }
            String str5;
            if (isEqualCountryCodePrefix(num1Prefix, str, num2Prefix, str2) != 0) {
                boolean isNum1CnMPN;
                boolean isNum1CnMPN2;
                boolean isNum2CnMPN;
                num2Len2 = isChineseNumberByPrefix(num1Prefix, str);
                String str6;
                if (num2Len2 != 0) {
                    NUM_LONG = 11;
                    num12 = deleteIPHead(num12);
                    isNum1CnMPN = isChineseMobilePhoneNumber(num12);
                    if (isNum1CnMPN) {
                        isNum1CnMPN2 = isNum1CnMPN;
                        str6 = num1Prefix;
                    } else {
                        numMatchShort = getChineseFixNumberAreaCodeLength(num12);
                        if (numMatchShort > 0) {
                            isNum1CnMPN2 = isNum1CnMPN;
                            num1AreaCode = num12.substring(false, numMatchShort);
                            num12 = num12.substring(numMatchShort);
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("compareNums, CN num1 after remove area code: ");
                            stringBuilder4.append(num12);
                            stringBuilder4.append(", num1AreaCode: ");
                            stringBuilder4.append(num1AreaCode);
                            logd(stringBuilder4.toString());
                        } else {
                            isNum1CnMPN2 = isNum1CnMPN;
                        }
                        str6 = num1Prefix;
                    }
                } else {
                    boolean isNum1CnMPN3;
                    if ("PE".equalsIgnoreCase(str)) {
                        logd("compareNums, PE num1 start with 0 not remove it");
                        isNum1CnMPN3 = false;
                        str6 = num1Prefix;
                        str3 = null;
                    } else {
                        isNum1CnMPN3 = false;
                        if (num12.length() >= true) {
                            str3 = null;
                            if ("0".equals(num12.substring(0, 1)) && !"0".equals(num12.substring(1, 2))) {
                                num12 = num12.substring(1);
                                logd("compareNums, num1 remove 0 at beginning");
                                isNum1CnMPN2 = isNum1CnMPN3;
                                num1AreaCode = str3;
                            }
                        } else {
                            str3 = null;
                        }
                    }
                    isNum1CnMPN2 = isNum1CnMPN3;
                    num1AreaCode = str3;
                }
                isNum1CnMPN = isChineseNumberByPrefix(num2Prefix, str2);
                boolean isNum2CnMPN2;
                if (isNum1CnMPN) {
                    NUM_LONG = 11;
                    num22 = deleteIPHead(num22);
                    isNum2CnMPN = isChineseMobilePhoneNumber(num22);
                    if (isNum2CnMPN) {
                        isNum2CnMPN2 = isNum2CnMPN;
                        str5 = num2Prefix;
                    } else {
                        num1Prefix = getChineseFixNumberAreaCodeLength(num22);
                        if (num1Prefix > null) {
                            isNum2CnMPN2 = isNum2CnMPN;
                            num2AreaCode = num22.substring(false, num1Prefix);
                            num22 = num22.substring(num1Prefix);
                            stringBuilder3 = new StringBuilder();
                            int areaCodeLen = num1Prefix;
                            stringBuilder3.append("compareNums, CN num2 after remove area code: ");
                            stringBuilder3.append(num22);
                            stringBuilder3.append(", num2AreaCode: ");
                            stringBuilder3.append(num2AreaCode);
                            logd(stringBuilder3.toString());
                        } else {
                            isNum2CnMPN2 = isNum2CnMPN;
                        }
                        str5 = num2Prefix;
                    }
                } else {
                    boolean isNum2CnMPN3;
                    if ("PE".equalsIgnoreCase(str2) != null) {
                        logd("compareNums, PE num2 start with 0 not remove it");
                        isNum2CnMPN3 = false;
                        str5 = num2Prefix;
                        str4 = null;
                    } else {
                        isNum2CnMPN3 = false;
                        if (num22.length() >= true) {
                            str4 = null;
                            if ("0".equals(num22.substring(null, 1)) && !"0".equals(num22.substring(1, 2))) {
                                num22 = num22.substring(1);
                                logd("compareNums, num2 remove 0 at beginning");
                                isNum2CnMPN2 = isNum2CnMPN3;
                                num2AreaCode = str4;
                            }
                        } else {
                            str4 = null;
                        }
                    }
                    isNum2CnMPN2 = isNum2CnMPN3;
                    num2AreaCode = str4;
                }
                if ((!isNum1CnMPN2 || isNum2CnMPN2) && (isNum1CnMPN2 || !isNum2CnMPN2)) {
                    if (isNum1CnMPN2 && isNum2CnMPN2) {
                        logd("compareNums, num1 and num2 are both MPN, continue to compare");
                    } else if (!(num2Len2 == 0 || !isNum1CnMPN || isEqualChineseFixNumberAreaCode(num1AreaCode, num2AreaCode))) {
                        logd("compareNums, areacode prefix not same, return false");
                        return false;
                    }
                    return compareNumsInternal(num12, num22, NUM_LONG, NUM_SHORT);
                }
                if (shouldDoNumberMatchAgainBySimMccmnc(origNum1, str) || shouldDoNumberMatchAgainBySimMccmnc(ret, str2)) {
                    isNum2CnMPN = compareNumsInternal(origNum1, ret, this.mSimNumLong, this.mSimNumShort);
                } else {
                    isNum2CnMPN = z;
                }
                num1Prefix = new StringBuilder();
                num1Prefix.append("compareNums, num1 and num2 not both MPN, return ");
                num1Prefix.append(isNum2CnMPN);
                logd(num1Prefix.toString());
                return isNum2CnMPN;
            }
            str5 = num2Prefix;
            str3 = null;
            str4 = null;
            if (shouldDoNumberMatchAgainBySimMccmnc(origNum1, str) == 0 && shouldDoNumberMatchAgainBySimMccmnc(ret, str2) == 0) {
                num2Len2 = z;
            } else {
                num2Len2 = compareNumsInternal(origNum1, ret, this.mSimNumLong, this.mSimNumShort);
            }
            formattedNum1 = new StringBuilder();
            formattedNum1.append("compareNums, countrycode prefix not same, return ");
            formattedNum1.append(num2Len2);
            logd(formattedNum1.toString());
            return num2Len2;
        }
        return false;
    }

    public boolean compareNums(String num1, String num2) {
        int NUM_LONG = this.NUM_LONG_CUST;
        int NUM_SHORT = this.NUM_SHORT_CUST;
        if (num1 == null || num2 == null) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("compareNums, num1 = ");
        stringBuilder.append(num1);
        stringBuilder.append(", num2 = ");
        stringBuilder.append(num2);
        logd(stringBuilder.toString());
        if (SystemProperties.getInt("ro.config.hwft_MatchNum", 0) == 0) {
            int i = 7;
            int numMatch = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH, 7);
            int numMatchShort = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_SHORT, numMatch);
            if (numMatch >= 7) {
                i = numMatch;
            }
            NUM_LONG = i;
            NUM_SHORT = numMatchShort >= NUM_LONG ? NUM_LONG : numMatchShort;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("compareNums, after setprop NUM_LONG = ");
            stringBuilder2.append(NUM_LONG);
            stringBuilder2.append(", NUM_SHORT = ");
            stringBuilder2.append(NUM_SHORT);
            logd(stringBuilder2.toString());
        }
        if (num1.indexOf(64) < 0) {
            num1 = PhoneNumberUtils.stripSeparators(num1);
        }
        if (num2.indexOf(64) < 0) {
            num2 = PhoneNumberUtils.stripSeparators(num2);
        }
        num1 = formatedForDualNumber(num1);
        num2 = formatedForDualNumber(num2);
        if (this.IS_CHINA_TELECOM && num1.startsWith("**133") && num1.endsWith("#")) {
            num1 = num1.substring(0, num1.length() - 1);
            logd("compareNums, num1 startsWith **133 && endsWith #");
        }
        if (this.IS_CHINA_TELECOM && num2.startsWith("**133") && num2.endsWith("#")) {
            num2 = num2.substring(0, num2.length() - 1);
            logd("compareNums, num2 startsWith **133 && endsWith #");
        }
        if (NUM_SHORT < NUM_LONG) {
            logd("compareNums, NUM_SHORT have been set! Only do full compare.");
            return num1.equals(num2);
        }
        int num1Len = num1.length();
        int num2Len = num2.length();
        if (num1Len > NUM_LONG) {
            num1 = num1.substring(num1Len - NUM_LONG);
        }
        if (num2Len > NUM_LONG) {
            num2 = num2.substring(num2Len - NUM_LONG);
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("compareNums, new num1 = ");
        stringBuilder3.append(num1);
        stringBuilder3.append(", new num2 = ");
        stringBuilder3.append(num2);
        logd(stringBuilder3.toString());
        return num1.equals(num2);
    }

    public int getCallerIndex(Cursor cursor, String compNum) {
        return getCallerIndex(cursor, compNum, "number");
    }

    public int getCallerIndex(Cursor cursor, String compNum, String columnName) {
        return getCallerIndex(cursor, compNum, columnName, SystemProperties.get(HwTelephonyProperties.PROPERTY_NETWORK_COUNTRY_ISO, ""));
    }

    /* JADX WARNING: Removed duplicated region for block: B:154:0x0587  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x053b  */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x061a A:{LOOP_END, LOOP:0: B:91:0x034a->B:176:0x061a} */
    /* JADX WARNING: Removed duplicated region for block: B:376:0x05e4 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:376:0x05e4 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x061a A:{LOOP_END, LOOP:0: B:91:0x034a->B:176:0x061a} */
    /* JADX WARNING: Removed duplicated region for block: B:273:0x0990 A:{LOOP_END, LOOP:1: B:190:0x06e4->B:273:0x0990} */
    /* JADX WARNING: Removed duplicated region for block: B:379:0x0958 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:379:0x0958 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:273:0x0990 A:{LOOP_END, LOOP:1: B:190:0x06e4->B:273:0x0990} */
    /* JADX WARNING: Removed duplicated region for block: B:350:0x0c36  */
    /* JADX WARNING: Removed duplicated region for block: B:344:0x0c0d  */
    /* JADX WARNING: Removed duplicated region for block: B:357:0x0c7d A:{LOOP_END, LOOP:2: B:285:0x0a2e->B:357:0x0c7d} */
    /* JADX WARNING: Removed duplicated region for block: B:385:0x0c58 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x01b4  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x017e  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x01c0  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x01be  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x0214  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x01c8  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x0280  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x021f  */
    /* JADX WARNING: Removed duplicated region for block: B:183:0x0675  */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x02d7  */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x0d01  */
    /* JADX WARNING: Removed duplicated region for block: B:365:0x0ccf  */
    /* JADX WARNING: Missing block: B:179:0x063f, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:266:0x095d, code skipped:
            r5 = new java.lang.StringBuilder();
            r5.append("7: numShortID = ");
            r5.append(r1);
            r5.append(",numLongID = ");
            r5.append(r15);
            logd(r5.toString());
     */
    /* JADX WARNING: Missing block: B:267:0x097a, code skipped:
            if (-1 == r1) goto L_0x098a;
     */
    /* JADX WARNING: Missing block: B:268:0x097c, code skipped:
            r5 = r1;
     */
    /* JADX WARNING: Missing block: B:269:0x097d, code skipped:
            r16 = r1;
            r17 = r15;
            r12 = r41;
            r14 = r42;
            r15 = r70;
            r1 = r72;
     */
    /* JADX WARNING: Missing block: B:270:0x098a, code skipped:
            if (-1 == r15) goto L_0x098e;
     */
    /* JADX WARNING: Missing block: B:271:0x098c, code skipped:
            r5 = r15;
     */
    /* JADX WARNING: Missing block: B:272:0x098e, code skipped:
            r5 = -1;
     */
    /* JADX WARNING: Missing block: B:359:0x0ca0, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getCallerIndex(Cursor cursor, String compNum, String columnName, String countryIso) {
        Cursor cursor2 = cursor;
        String str = columnName;
        String str2 = countryIso;
        String compNumPrefix = null;
        String compNumAreaCode = null;
        String compNumShort = null;
        int fixedIndex = -1;
        String compNumLong = null;
        int NUM_LONG = this.NUM_LONG_CUST;
        String tmpNum = null;
        int NUM_SHORT = this.NUM_SHORT_CUST;
        String tmpNumShort;
        String str3;
        String tmpNumLong;
        StringBuilder stringBuilder;
        String origCompNum;
        if (TextUtils.isEmpty(compNum)) {
            if (cursor2 != null && cursor.getCount() > 0) {
                fixedIndex = 0;
            }
            tmpNumShort = null;
            str3 = TAG;
            tmpNumLong = null;
            stringBuilder = new StringBuilder();
            origCompNum = null;
            stringBuilder.append("CallerInfoHW(),null == compNum! fixedIndex = ");
            stringBuilder.append(fixedIndex);
            Log.e(str3, stringBuilder.toString());
            return fixedIndex;
        }
        tmpNumShort = null;
        tmpNumLong = null;
        origCompNum = null;
        String origTmpNum;
        String str4;
        String str5;
        String str6;
        if (cursor2 == null) {
            origTmpNum = null;
            str4 = null;
            str5 = null;
            str6 = null;
        } else if (cursor.getCount() <= 0) {
            origTmpNum = null;
            str4 = null;
            str5 = null;
            str6 = null;
        } else {
            fixedIndex = getFullMatchIndex(cursor, compNum, columnName);
            if (IS_SUPPORT_DUAL_NUMBER && -1 == fixedIndex) {
                fixedIndex = getFullMatchIndex(cursor2, formatedForDualNumber(PhoneNumberUtils.stripSeparators(compNum)), str);
            }
            if (-1 != fixedIndex) {
                return fixedIndex;
            }
            int numMatch;
            int numMatchShort;
            int fixedIndex2;
            String formattedCompNum;
            String tmpCompNum;
            boolean isCnNumber;
            String origTmpNum2;
            StringBuilder stringBuilder2;
            logd("getCallerIndex(), not full match proceed to check..");
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getCallerIndex(), NUM_LONG = ");
            stringBuilder3.append(NUM_LONG);
            stringBuilder3.append(",NUM_SHORT = ");
            stringBuilder3.append(NUM_SHORT);
            logd(stringBuilder3.toString());
            int i = 7;
            if (SystemProperties.getInt("ro.config.hwft_MatchNum", 0) == 0) {
                numMatch = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH, 7);
                numMatchShort = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_SHORT, numMatch);
                if (numMatch >= 7) {
                    i = numMatch;
                }
                this.mSimNumLong = i;
                NUM_LONG = i;
                i = numMatchShort >= NUM_LONG ? NUM_LONG : numMatchShort;
                this.mSimNumShort = i;
                NUM_SHORT = i;
                stringBuilder = new StringBuilder();
                fixedIndex2 = fixedIndex;
                stringBuilder.append("getCallerIndex(), after setprop NUM_LONG = ");
                stringBuilder.append(NUM_LONG);
                stringBuilder.append(", NUM_SHORT = ");
                stringBuilder.append(NUM_SHORT);
                logd(stringBuilder.toString());
            } else {
                fixedIndex2 = fixedIndex;
            }
            String compNum2 = formatedForDualNumber(PhoneNumberUtils.stripSeparators(compNum));
            numMatch = compNum2.length();
            stringBuilder = new StringBuilder();
            stringBuilder.append("compNum: ");
            stringBuilder.append(compNum2);
            stringBuilder.append(", countryIso: ");
            stringBuilder.append(str2);
            logd(stringBuilder.toString());
            if (this.IS_CHINA_TELECOM && compNum2.startsWith("**133") && compNum2.endsWith("#")) {
                compNum2 = compNum2.substring(0, compNum2.length() - 1);
                logd("compNum startsWith **133 && endsWith #");
            }
            String origCompNum2 = compNum2;
            int NUM_LONG2 = NUM_LONG;
            this.mNetworkOperator = SystemProperties.get(HwTelephonyProperties.PROPERTY_NETWORK_OPERATOR, "");
            String formattedCompNum2 = null;
            if (!TextUtils.isEmpty(countryIso)) {
                formattedCompNum2 = PhoneNumberUtils.formatNumberToE164(compNum2, str2.toUpperCase(Locale.US));
                if (formattedCompNum2 != null) {
                    int NUM_SHORT2;
                    int countryCodeLen;
                    String formattedCompNum3;
                    StringBuilder stringBuilder4;
                    boolean isCompNumCnMPN;
                    String origCompNum3;
                    String origTmpNum3;
                    String tmpCompNum2;
                    String compNumAreaCode2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("formattedCompNum: ");
                    stringBuilder.append(formattedCompNum2);
                    stringBuilder.append(", with countryIso: ");
                    stringBuilder.append(str2);
                    logd(stringBuilder.toString());
                    compNum2 = formattedCompNum2;
                    formattedCompNum = formattedCompNum2;
                    NUM_LONG = getIntlPrefixAndCCLen(compNum2);
                    if (NUM_LONG <= 0) {
                        NUM_SHORT2 = NUM_SHORT;
                        compNumPrefix = compNum2.substring(0, NUM_LONG);
                        compNum2 = compNum2.substring(NUM_LONG);
                        StringBuilder stringBuilder5 = new StringBuilder();
                        countryCodeLen = NUM_LONG;
                        stringBuilder5.append("compNum after remove prefix: ");
                        stringBuilder5.append(compNum2);
                        stringBuilder5.append(", compNumLen: ");
                        stringBuilder5.append(compNum2.length());
                        stringBuilder5.append(", compNumPrefix: ");
                        stringBuilder5.append(compNumPrefix);
                        logd(stringBuilder5.toString());
                    } else {
                        countryCodeLen = NUM_LONG;
                        NUM_SHORT2 = NUM_SHORT;
                    }
                    tmpCompNum = TextUtils.isEmpty(formattedCompNum) ? formattedCompNum : origCompNum2;
                    if (isRoamingCountryNumberByPrefix(compNumPrefix, str2)) {
                        formattedCompNum3 = formattedCompNum;
                    } else {
                        logd("compNum belongs to roaming country");
                        NUM_LONG = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_ROAMING, 7);
                        numMatch = SystemProperties.getInt(HwTelephonyProperties.PROPERTY_GLOBAL_VERSION_NUM_MATCH_SHORT_ROAMING, NUM_LONG);
                        formattedCompNum3 = formattedCompNum;
                        formattedCompNum = NUM_LONG < 7 ? 7 : NUM_LONG;
                        int NUM_SHORT3 = numMatch >= formattedCompNum ? formattedCompNum : numMatch;
                        stringBuilder4 = new StringBuilder();
                        int numMatchShort2 = numMatch;
                        stringBuilder4.append("getCallerIndex(), roaming prop NUM_LONG = ");
                        stringBuilder4.append(formattedCompNum);
                        stringBuilder4.append(", NUM_SHORT = ");
                        numMatch = NUM_SHORT3;
                        stringBuilder4.append(numMatch);
                        logd(stringBuilder4.toString());
                        NUM_SHORT2 = numMatch;
                        NUM_LONG2 = formattedCompNum;
                    }
                    isCnNumber = isChineseNumberByPrefix(compNumPrefix, str2);
                    if (isCnNumber) {
                        boolean z;
                        if ("PE".equalsIgnoreCase(str2)) {
                            logd("PE compNum start with 0 not remove it");
                            z = false;
                            origTmpNum = null;
                        } else if (compNum2.length() >= 7) {
                            z = false;
                            origTmpNum = null;
                            if ("0".equals(compNum2.substring(0, 1)) && !"0".equals(compNum2.substring(1, 2))) {
                                origTmpNum2 = compNum2.substring(1);
                                str3 = null;
                                compNumAreaCode = NUM_LONG2;
                                i = NUM_SHORT2;
                                isCompNumCnMPN = z;
                            }
                        } else {
                            z = false;
                            origTmpNum = null;
                        }
                        origTmpNum2 = compNum2;
                        str3 = null;
                        compNumAreaCode = NUM_LONG2;
                        i = NUM_SHORT2;
                        isCompNumCnMPN = z;
                    } else {
                        NUM_SHORT2 = 11;
                        NUM_LONG2 = 11;
                        compNum2 = deleteIPHead(compNum2);
                        boolean isCompNumCnMPN2 = isChineseMobilePhoneNumber(compNum2);
                        if (isCompNumCnMPN2) {
                            isCompNumCnMPN = isCompNumCnMPN2;
                            origTmpNum = null;
                            str3 = null;
                            compNumAreaCode = NUM_LONG2;
                            i = NUM_SHORT2;
                            origTmpNum2 = compNum2;
                        } else {
                            i = getChineseFixNumberAreaCodeLength(compNum2);
                            if (i > 0) {
                                compNumAreaCode = compNum2.substring(0, i);
                                compNum2 = compNum2.substring(i);
                                stringBuilder3 = new StringBuilder();
                                isCompNumCnMPN = isCompNumCnMPN2;
                                stringBuilder3.append("CN compNum after remove area code: ");
                                stringBuilder3.append(compNum2);
                                stringBuilder3.append(", compNumLen: ");
                                stringBuilder3.append(compNum2.length());
                                stringBuilder3.append(", compNumAreaCode: ");
                                stringBuilder3.append(compNumAreaCode);
                                logd(stringBuilder3.toString());
                            } else {
                                isCompNumCnMPN = isCompNumCnMPN2;
                            }
                            origTmpNum = null;
                            str3 = compNumAreaCode;
                            compNumAreaCode = NUM_LONG2;
                            i = NUM_SHORT2;
                            origTmpNum2 = compNum2;
                        }
                    }
                    NUM_LONG = origTmpNum2.length();
                    String compNumShort2;
                    int columnIndex;
                    int formatColumnIndex;
                    StringBuilder stringBuilder6;
                    String compNum3;
                    int numShortID;
                    String tmpNumFormat;
                    if (NUM_LONG < compNumAreaCode) {
                        compNum2 = origTmpNum2.substring(NUM_LONG - compNumAreaCode);
                        str4 = null;
                        compNumShort2 = origTmpNum2.substring(NUM_LONG - i);
                        str5 = null;
                        StringBuilder stringBuilder7 = new StringBuilder();
                        str6 = null;
                        stringBuilder7.append("11:, compNumLong = ");
                        stringBuilder7.append(compNum2);
                        stringBuilder7.append(",compNumShort = ");
                        stringBuilder7.append(compNumShort2);
                        logd(stringBuilder7.toString());
                        if (cursor.moveToFirst()) {
                            int i2;
                            String compNumLong2;
                            columnIndex = cursor2.getColumnIndex(str);
                            formatColumnIndex = cursor2.getColumnIndex("normalized_number");
                            origCompNum3 = origCompNum2;
                            origCompNum2 = cursor2.getColumnIndex("data4");
                            stringBuilder6 = new StringBuilder();
                            compNum3 = origTmpNum2;
                            stringBuilder6.append("11: columnIndex: ");
                            stringBuilder6.append(columnIndex);
                            stringBuilder6.append(", formatColumnIndex: ");
                            stringBuilder6.append(formatColumnIndex);
                            stringBuilder6.append(", data4ColumnIndex: ");
                            stringBuilder6.append(origCompNum2);
                            logd(stringBuilder6.toString());
                            if (columnIndex != -1) {
                                String origTmpNum4;
                                String tmpNumFormat2;
                                int countryCodeLen2;
                                numShortID = -1;
                                origTmpNum2 = -1;
                                while (true) {
                                    int columnIndex2 = columnIndex;
                                    origTmpNum3 = cursor2.getString(columnIndex);
                                    String str7;
                                    int i3;
                                    if (origTmpNum3 == null) {
                                        i2 = NUM_LONG;
                                        str7 = origCompNum2;
                                        i3 = formatColumnIndex;
                                        compNum2 = -1;
                                        break;
                                    }
                                    i2 = NUM_LONG;
                                    if (origTmpNum3.indexOf(64) >= 0) {
                                        str7 = origCompNum2;
                                        i3 = formatColumnIndex;
                                        compNum2 = -1;
                                        break;
                                    }
                                    formattedCompNum2 = PhoneNumberUtils.stripSeparators(origTmpNum3);
                                    stringBuilder7 = new StringBuilder();
                                    String compNumShort3 = compNumShort2;
                                    stringBuilder7.append("origTmpNum: ");
                                    stringBuilder7.append(formattedCompNum2);
                                    logd(stringBuilder7.toString());
                                    if (-1 != formatColumnIndex) {
                                        compNumShort2 = cursor2.getString(formatColumnIndex);
                                        origTmpNum3 = isValidData4Number(formattedCompNum2, compNumShort2) ? compNumShort2 : formattedCompNum2;
                                    } else if (-1 != origCompNum2) {
                                        compNumShort2 = cursor2.getString(origCompNum2);
                                        origTmpNum3 = isValidData4Number(formattedCompNum2, compNumShort2) ? compNumShort2 : formattedCompNum2;
                                    } else {
                                        origTmpNum3 = formattedCompNum2;
                                        compNumShort2 = str6;
                                    }
                                    origTmpNum4 = formattedCompNum2;
                                    stringBuilder4 = new StringBuilder();
                                    int data4ColumnIndex = origCompNum2;
                                    stringBuilder4.append("11: tmpNumFormat: ");
                                    stringBuilder4.append(compNumShort2);
                                    logd(stringBuilder4.toString());
                                    NUM_LONG = origTmpNum3.length();
                                    origCompNum2 = new StringBuilder();
                                    origCompNum2.append("11: tmpNum = ");
                                    origCompNum2.append(origTmpNum3);
                                    origCompNum2.append(", tmpNum.length11: ");
                                    origCompNum2.append(origTmpNum3.length());
                                    origCompNum2.append(",ID = ");
                                    origCompNum2.append(cursor.getPosition());
                                    logd(origCompNum2.toString());
                                    if (origTmpNum3.equals(tmpCompNum)) {
                                        NUM_LONG = cursor.getPosition();
                                        origCompNum2 = new StringBuilder();
                                        origCompNum2.append("11: > NUM_LONG numLongID = ");
                                        origCompNum2.append(NUM_LONG);
                                        origCompNum2.append(", formattedNum full match!");
                                        logd(origCompNum2.toString());
                                        compNumLong2 = compNum2;
                                        origTmpNum2 = NUM_LONG;
                                        tmpNumFormat2 = compNumShort2;
                                        i3 = formatColumnIndex;
                                        countryCodeLen2 = countryCodeLen;
                                        compNumShort2 = compNumShort3;
                                        break;
                                    }
                                    String tmpNumAreaCode;
                                    String tmpNumPrefix = null;
                                    NUM_LONG = getIntlPrefixAndCCLen(origTmpNum3);
                                    if (NUM_LONG > 0) {
                                        tmpNumAreaCode = null;
                                        tmpNumFormat2 = compNumShort2;
                                        compNumShort2 = origTmpNum3.substring(null, NUM_LONG);
                                        origTmpNum3 = origTmpNum3.substring(NUM_LONG);
                                        origCompNum2 = new StringBuilder();
                                        countryCodeLen2 = NUM_LONG;
                                        origCompNum2.append("11: tmpNum after remove prefix: ");
                                        origCompNum2.append(origTmpNum3);
                                        origCompNum2.append(", tmpNum.length11: ");
                                        origCompNum2.append(origTmpNum3.length());
                                        origCompNum2.append(", tmpNumPrefix: ");
                                        origCompNum2.append(compNumShort2);
                                        logd(origCompNum2.toString());
                                    } else {
                                        countryCodeLen2 = NUM_LONG;
                                        tmpNumAreaCode = null;
                                        tmpNumFormat2 = compNumShort2;
                                        compNumShort2 = tmpNumPrefix;
                                    }
                                    String tmpNumPrefix2;
                                    if (isEqualCountryCodePrefix(compNumPrefix, str2, compNumShort2, null) != null) {
                                        if (isCnNumber) {
                                            formattedCompNum2 = deleteIPHead(origTmpNum3);
                                            origCompNum2 = isChineseMobilePhoneNumber(formattedCompNum2);
                                            if (!(isCompNumCnMPN && origCompNum2 == null) && (isCompNumCnMPN || origCompNum2 == null)) {
                                                if (!isCompNumCnMPN || origCompNum2 == null) {
                                                    columnIndex = getChineseFixNumberAreaCodeLength(formattedCompNum2);
                                                    boolean isTmpNumCnMPN;
                                                    if (columnIndex > 0) {
                                                        isTmpNumCnMPN = origCompNum2;
                                                        tmpNumPrefix2 = compNumShort2;
                                                        compNumShort2 = formattedCompNum2.substring(null, columnIndex);
                                                        formattedCompNum2 = formattedCompNum2.substring(columnIndex);
                                                        origCompNum2 = new StringBuilder();
                                                        origCompNum2.append("11: CN tmpNum after remove area code: ");
                                                        origCompNum2.append(formattedCompNum2);
                                                        origCompNum2.append(", tmpNum.length11: ");
                                                        origCompNum2.append(formattedCompNum2.length());
                                                        origCompNum2.append(", tmpNumAreaCode: ");
                                                        origCompNum2.append(compNumShort2);
                                                        logd(origCompNum2.toString());
                                                    } else {
                                                        isTmpNumCnMPN = origCompNum2;
                                                        tmpNumPrefix2 = compNumShort2;
                                                        int i4 = columnIndex;
                                                        compNumShort2 = tmpNumAreaCode;
                                                    }
                                                    if (isEqualChineseFixNumberAreaCode(str3, compNumShort2) == null) {
                                                        logd("11: areacode prefix not same, continue");
                                                        compNumLong2 = compNum2;
                                                        tmpNum = formattedCompNum2;
                                                        str5 = compNumShort2;
                                                        i3 = formatColumnIndex;
                                                        compNumShort2 = compNumShort3;
                                                        if (cursor.moveToNext() == null) {
                                                            origTmpNum3 = tmpNum;
                                                            str4 = tmpNumPrefix2;
                                                            break;
                                                        }
                                                        columnIndex = columnIndex2;
                                                        NUM_LONG = i2;
                                                        origTmpNum = origTmpNum4;
                                                        origCompNum2 = data4ColumnIndex;
                                                        str6 = tmpNumFormat2;
                                                        countryCodeLen = countryCodeLen2;
                                                        str4 = tmpNumPrefix2;
                                                        formatColumnIndex = i3;
                                                        compNum2 = compNumLong2;
                                                    } else {
                                                        origTmpNum3 = formattedCompNum2;
                                                        origCompNum2 = compNumShort2;
                                                    }
                                                } else {
                                                    logd("11: compNum and tmpNum are both MPN, continue to match by mccmnc");
                                                    origTmpNum3 = formattedCompNum2;
                                                    tmpNumPrefix2 = compNumShort2;
                                                    origCompNum2 = tmpNumAreaCode;
                                                }
                                                tmpNumAreaCode = origCompNum2;
                                                i3 = formatColumnIndex;
                                                NUM_LONG = origTmpNum3.length();
                                                if (NUM_LONG < compNumAreaCode) {
                                                    origCompNum2 = origTmpNum3.substring(NUM_LONG - compNumAreaCode);
                                                    if (-1 == origTmpNum2 && compNum2.compareTo(origCompNum2) == 0) {
                                                        origTmpNum2 = cursor.getPosition();
                                                        StringBuilder stringBuilder8 = new StringBuilder();
                                                        stringBuilder8.append("11: > NUM_LONG numLongID = ");
                                                        stringBuilder8.append(origTmpNum2);
                                                        logd(stringBuilder8.toString());
                                                    } else {
                                                        compNumShort2 = new StringBuilder();
                                                        compNumShort2.append("11: >=NUM_LONG, and !=,  tmpNumLong = ");
                                                        compNumShort2.append(origCompNum2);
                                                        compNumShort2.append(", numLongID:");
                                                        compNumShort2.append(origTmpNum2);
                                                        logd(compNumShort2.toString());
                                                    }
                                                    compNumLong2 = compNum2;
                                                    tmpNumLong = origCompNum2;
                                                    tmpNum = origTmpNum3;
                                                } else if (NUM_LONG >= i) {
                                                    origCompNum2 = origTmpNum3.substring(NUM_LONG - i);
                                                    if (-1 == numShortID) {
                                                        compNumShort2 = compNumShort3;
                                                        if (compNumShort2.compareTo(origCompNum2) == null) {
                                                            numShortID = cursor.getPosition();
                                                        }
                                                    } else {
                                                        compNumShort2 = compNumShort3;
                                                    }
                                                    tmpNumFormat = new StringBuilder();
                                                    compNumLong2 = compNum2;
                                                    tmpNumFormat.append("11: >=NUM_SHORT, tmpNumShort = ");
                                                    tmpNumFormat.append(origCompNum2);
                                                    tmpNumFormat.append(", numShortID:");
                                                    tmpNumFormat.append(numShortID);
                                                    logd(tmpNumFormat.toString());
                                                    tmpNumShort = origCompNum2;
                                                } else {
                                                    compNumLong2 = compNum2;
                                                    compNumShort2 = compNumShort3;
                                                    logd("tmpNum11, continue");
                                                }
                                            } else {
                                                logd("11: compNum and tmpNum not both MPN, continue");
                                                compNumLong2 = compNum2;
                                                tmpNum = formattedCompNum2;
                                                tmpNumPrefix2 = compNumShort2;
                                                i3 = formatColumnIndex;
                                            }
                                        } else {
                                            tmpNumPrefix2 = compNumShort2;
                                            if (origTmpNum3.length() >= 7) {
                                                i3 = formatColumnIndex;
                                                if ("0".equals(origTmpNum3.substring(null, 1)) && !"0".equals(origTmpNum3.substring(1, 2))) {
                                                    origTmpNum3 = origTmpNum3.substring(1);
                                                    logd("11: tmpNum remove 0 at beginning");
                                                }
                                            } else {
                                                i3 = formatColumnIndex;
                                            }
                                            NUM_LONG = origTmpNum3.length();
                                            if (NUM_LONG < compNumAreaCode) {
                                            }
                                        }
                                        compNumShort2 = compNumShort3;
                                        if (cursor.moveToNext() == null) {
                                        }
                                    } else {
                                        compNumLong2 = compNum2;
                                        tmpNumPrefix2 = compNumShort2;
                                        i3 = formatColumnIndex;
                                        compNumShort2 = compNumShort3;
                                        logd("11: countrycode prefix not same, continue");
                                    }
                                    tmpNum = origTmpNum3;
                                    if (cursor.moveToNext() == null) {
                                    }
                                }
                                compNum2 = new StringBuilder();
                                compNum2.append("11:  numLongID = ");
                                compNum2.append(origTmpNum2);
                                compNum2.append(",numShortID = ");
                                compNum2.append(numShortID);
                                logd(compNum2.toString());
                                if (-1 != origTmpNum2) {
                                    NUM_LONG = origTmpNum2;
                                } else if (-1 != numShortID) {
                                    NUM_LONG = numShortID;
                                } else {
                                    NUM_LONG = -1;
                                }
                                compNum2 = NUM_LONG;
                                int numLongID = origTmpNum2;
                                origTmpNum2 = origTmpNum4;
                                tmpNumFormat = tmpNumFormat2;
                                NUM_LONG = countryCodeLen2;
                            } else {
                                compNumLong2 = compNum2;
                                i2 = NUM_LONG;
                                origTmpNum3 = tmpNum;
                                compNum2 = fixedIndex2;
                                NUM_LONG = countryCodeLen;
                                origTmpNum2 = origTmpNum;
                                tmpNumFormat = str6;
                            }
                            tmpCompNum2 = tmpCompNum;
                            compNumAreaCode2 = str3;
                            origTmpNum = origTmpNum2;
                            tmpNum = origTmpNum3;
                            str6 = tmpNumFormat;
                            NUM_LONG = compNum3;
                            NUM_SHORT = i2;
                            compNumLong = compNumLong2;
                            str = columnName;
                            origTmpNum2 = compNum2;
                        } else {
                            origCompNum3 = origCompNum2;
                            tmpCompNum2 = tmpCompNum;
                            compNumAreaCode2 = str3;
                            compNumLong = compNum2;
                            NUM_SHORT = NUM_LONG;
                            NUM_LONG = origTmpNum2;
                            origTmpNum2 = fixedIndex2;
                        }
                    } else {
                        origCompNum3 = origCompNum2;
                        compNum3 = origTmpNum2;
                        str4 = null;
                        str5 = null;
                        str6 = null;
                        StringBuilder stringBuilder9;
                        String compNum4;
                        int compNumLen;
                        if (NUM_LONG >= i) {
                            str = compNum3;
                            compNum2 = str.substring(NUM_LONG - i);
                            StringBuilder stringBuilder10 = new StringBuilder();
                            stringBuilder10.append("7:  compNumShort = ");
                            stringBuilder10.append(compNum2);
                            logd(stringBuilder10.toString());
                            if (cursor.moveToFirst()) {
                                String origTmpNum5;
                                numMatchShort = cursor2.getColumnIndex(columnName);
                                int formatColumnIndex2 = cursor2.getColumnIndex("normalized_number");
                                columnIndex = cursor2.getColumnIndex("data4");
                                stringBuilder9 = new StringBuilder();
                                compNum4 = str;
                                stringBuilder9.append("7: columnIndex: ");
                                stringBuilder9.append(numMatchShort);
                                stringBuilder9.append(", formatColumnIndex: ");
                                stringBuilder9.append(formatColumnIndex2);
                                stringBuilder9.append(", data4ColumnIndex: ");
                                stringBuilder9.append(columnIndex);
                                logd(stringBuilder9.toString());
                                if (numMatchShort != -1) {
                                    numShortID = -1;
                                    formatColumnIndex = -1;
                                    while (true) {
                                        int columnIndex3 = numMatchShort;
                                        origCompNum2 = cursor2.getString(numMatchShort);
                                        int i5;
                                        int i6;
                                        if (origCompNum2 == null) {
                                            i5 = formatColumnIndex2;
                                            i6 = columnIndex;
                                            formatColumnIndex2 = -1;
                                            break;
                                        }
                                        compNumLen = NUM_LONG;
                                        if (origCompNum2.indexOf(64) >= 0) {
                                            i6 = columnIndex;
                                            formatColumnIndex2 = -1;
                                            break;
                                        }
                                        NUM_LONG = PhoneNumberUtils.stripSeparators(origCompNum2);
                                        stringBuilder10 = new StringBuilder();
                                        stringBuilder10.append("origTmpNum: ");
                                        stringBuilder10.append(NUM_LONG);
                                        logd(stringBuilder10.toString());
                                        if (-1 != formatColumnIndex2) {
                                            origCompNum2 = cursor2.getString(formatColumnIndex2);
                                            origTmpNum2 = isValidData4Number(NUM_LONG, origCompNum2) != null ? origCompNum2 : NUM_LONG;
                                        } else if (-1 != columnIndex) {
                                            origCompNum2 = cursor2.getString(columnIndex);
                                            origTmpNum2 = isValidData4Number(NUM_LONG, origCompNum2) != null ? origCompNum2 : NUM_LONG;
                                        } else {
                                            origTmpNum2 = NUM_LONG;
                                            origCompNum2 = str6;
                                        }
                                        origTmpNum5 = NUM_LONG;
                                        NUM_LONG = new StringBuilder();
                                        i5 = formatColumnIndex2;
                                        NUM_LONG.append("7: tmpNumFormat: ");
                                        NUM_LONG.append(origCompNum2);
                                        logd(NUM_LONG.toString());
                                        NUM_LONG = origTmpNum2.length();
                                        formatColumnIndex2 = new StringBuilder();
                                        int tmpNumLen = NUM_LONG;
                                        formatColumnIndex2.append("7: tmpNum = ");
                                        formatColumnIndex2.append(origTmpNum2);
                                        formatColumnIndex2.append(", tmpNum.length7: ");
                                        formatColumnIndex2.append(origTmpNum2.length());
                                        formatColumnIndex2.append(",ID = ");
                                        formatColumnIndex2.append(cursor.getPosition());
                                        logd(formatColumnIndex2.toString());
                                        String tmpNumFormat3;
                                        int countryCodeLen3;
                                        if (origTmpNum2.equals(tmpCompNum) != 0) {
                                            NUM_LONG = cursor.getPosition();
                                            stringBuilder6 = new StringBuilder();
                                            stringBuilder6.append("7: >= NUM_SHORT numShortID = ");
                                            stringBuilder6.append(NUM_LONG);
                                            stringBuilder6.append(", formattedNum full match!");
                                            logd(stringBuilder6.toString());
                                            tmpNumFormat3 = origCompNum2;
                                            i6 = columnIndex;
                                            countryCodeLen3 = countryCodeLen;
                                            break;
                                        }
                                        String tmpNumAreaCode2;
                                        String tmpNumPrefix3 = 0;
                                        NUM_LONG = getIntlPrefixAndCCLen(origTmpNum2);
                                        if (NUM_LONG > 0) {
                                            tmpNumFormat3 = origCompNum2;
                                            tmpNumAreaCode2 = 0;
                                            formatColumnIndex2 = origTmpNum2.substring(null, NUM_LONG);
                                            origTmpNum2 = origTmpNum2.substring(NUM_LONG);
                                            stringBuilder10 = new StringBuilder();
                                            countryCodeLen3 = NUM_LONG;
                                            stringBuilder10.append("7: tmpNum after remove prefix: ");
                                            stringBuilder10.append(origTmpNum2);
                                            stringBuilder10.append(", tmpNum.length7: ");
                                            stringBuilder10.append(origTmpNum2.length());
                                            stringBuilder10.append(", tmpNumPrefix: ");
                                            stringBuilder10.append(formatColumnIndex2);
                                            logd(stringBuilder10.toString());
                                        } else {
                                            countryCodeLen3 = NUM_LONG;
                                            tmpNumFormat3 = origCompNum2;
                                            tmpNumAreaCode2 = 0;
                                            formatColumnIndex2 = tmpNumPrefix3;
                                        }
                                        String tmpNumPrefix4;
                                        if (isEqualCountryCodePrefix(compNumPrefix, str2, formatColumnIndex2, 0)) {
                                            if (isCnNumber) {
                                                NUM_LONG = deleteIPHead(origTmpNum2);
                                                boolean isTmpNumCnMPN2 = isChineseMobilePhoneNumber(NUM_LONG);
                                                if ((!isCompNumCnMPN || isTmpNumCnMPN2) && (isCompNumCnMPN || !isTmpNumCnMPN2)) {
                                                    if (isCompNumCnMPN && isTmpNumCnMPN2) {
                                                        logd("7: compNum and tmpNum are both MPN, continue to match by mccmnc");
                                                        origTmpNum2 = NUM_LONG;
                                                        tmpNumPrefix4 = formatColumnIndex2;
                                                        formatColumnIndex2 = tmpNumAreaCode2;
                                                    } else {
                                                        origTmpNum2 = getChineseFixNumberAreaCodeLength(NUM_LONG);
                                                        if (origTmpNum2 > null) {
                                                            tmpNumPrefix4 = formatColumnIndex2;
                                                            formatColumnIndex2 = NUM_LONG.substring(false, origTmpNum2);
                                                            NUM_LONG = NUM_LONG.substring(origTmpNum2);
                                                            stringBuilder10 = new StringBuilder();
                                                            int areaCodeLen = origTmpNum2;
                                                            stringBuilder10.append("7: CN tmpNum after remove area code: ");
                                                            stringBuilder10.append(NUM_LONG);
                                                            stringBuilder10.append(", tmpNum.length7: ");
                                                            stringBuilder10.append(NUM_LONG.length());
                                                            stringBuilder10.append(", tmpNumAreaCode: ");
                                                            stringBuilder10.append(formatColumnIndex2);
                                                            logd(stringBuilder10.toString());
                                                        } else {
                                                            String str8 = origTmpNum2;
                                                            tmpNumPrefix4 = formatColumnIndex2;
                                                            formatColumnIndex2 = tmpNumAreaCode2;
                                                        }
                                                        if (isEqualChineseFixNumberAreaCode(str3, formatColumnIndex2)) {
                                                            origTmpNum2 = NUM_LONG;
                                                        } else {
                                                            logd("7: areacode prefix not same, continue");
                                                            tmpNum = NUM_LONG;
                                                            str5 = formatColumnIndex2;
                                                            i6 = columnIndex;
                                                            if (cursor.moveToNext() == 0) {
                                                                NUM_LONG = numShortID;
                                                                origTmpNum2 = tmpNum;
                                                                str4 = tmpNumPrefix4;
                                                                break;
                                                            }
                                                            numMatchShort = columnIndex3;
                                                            NUM_LONG = compNumLen;
                                                            origTmpNum = origTmpNum5;
                                                            formatColumnIndex2 = i5;
                                                            str6 = tmpNumFormat3;
                                                            countryCodeLen = countryCodeLen3;
                                                            str4 = tmpNumPrefix4;
                                                            columnIndex = i6;
                                                            origTmpNum2 = columnName;
                                                        }
                                                    }
                                                    tmpNumAreaCode2 = formatColumnIndex2;
                                                    i6 = columnIndex;
                                                } else {
                                                    logd("7: compNum and tmpNum not both MPN, continue");
                                                    tmpNum = NUM_LONG;
                                                    tmpNumPrefix4 = formatColumnIndex2;
                                                    i6 = columnIndex;
                                                    str5 = tmpNumAreaCode2;
                                                    if (cursor.moveToNext() == 0) {
                                                    }
                                                }
                                            } else {
                                                tmpNumPrefix4 = formatColumnIndex2;
                                                if (origTmpNum2.length() >= 7) {
                                                    i6 = columnIndex;
                                                    if ("0".equals(origTmpNum2.substring(0, 1)) != 0 && "0".equals(origTmpNum2.substring(1, 2)) == 0) {
                                                        origTmpNum2 = origTmpNum2.substring(1);
                                                        logd("7: tmpNum remove 0 at beginning");
                                                    }
                                                } else {
                                                    i6 = columnIndex;
                                                }
                                            }
                                            NUM_LONG = origTmpNum2.length();
                                            if (NUM_LONG >= compNumAreaCode) {
                                                origCompNum2 = origTmpNum2.substring(NUM_LONG - i);
                                                if (-1 == formatColumnIndex && compNum2.compareTo(origCompNum2) == 0) {
                                                    formatColumnIndex = cursor.getPosition();
                                                }
                                                formatColumnIndex2 = new StringBuilder();
                                                formatColumnIndex2.append("7: >=NUM_LONG, tmpNumShort = ");
                                                formatColumnIndex2.append(origCompNum2);
                                                formatColumnIndex2.append(", numLongID:");
                                                formatColumnIndex2.append(formatColumnIndex);
                                                logd(formatColumnIndex2.toString());
                                            } else if (NUM_LONG >= i) {
                                                origCompNum2 = origTmpNum2.substring(NUM_LONG - i);
                                                if (-1 == numShortID && compNum2.compareTo(origCompNum2) == 0) {
                                                    numShortID = cursor.getPosition();
                                                    formatColumnIndex2 = new StringBuilder();
                                                    formatColumnIndex2.append("7: >= NUM_SHORT numShortID = ");
                                                    formatColumnIndex2.append(numShortID);
                                                    logd(formatColumnIndex2.toString());
                                                } else {
                                                    formatColumnIndex2 = new StringBuilder();
                                                    formatColumnIndex2.append("7: >=NUM_SHORT, and !=, tmpNumShort = ");
                                                    formatColumnIndex2.append(origCompNum2);
                                                    formatColumnIndex2.append(", numShortID:");
                                                    formatColumnIndex2.append(numShortID);
                                                    logd(formatColumnIndex2.toString());
                                                }
                                            } else {
                                                logd("7: continue");
                                            }
                                        } else {
                                            tmpNumPrefix4 = formatColumnIndex2;
                                            i6 = columnIndex;
                                            logd("7: countrycode prefix not same, continue");
                                        }
                                        tmpNum = origTmpNum2;
                                        str5 = tmpNumAreaCode2;
                                        if (cursor.moveToNext() == 0) {
                                        }
                                    }
                                    return formatColumnIndex2;
                                }
                                compNumLen = NUM_LONG;
                                origTmpNum2 = tmpNum;
                                numMatchShort = fixedIndex2;
                                NUM_LONG = countryCodeLen;
                                origTmpNum5 = origTmpNum;
                                compNumShort2 = str4;
                                origTmpNum3 = str5;
                                tmpNumFormat = str6;
                                tmpCompNum2 = tmpCompNum;
                                compNumAreaCode2 = str3;
                                tmpNum = origTmpNum2;
                                str4 = compNumShort2;
                                str5 = origTmpNum3;
                                str6 = tmpNumFormat;
                                NUM_LONG = compNum4;
                                NUM_SHORT = compNumLen;
                                origTmpNum = origTmpNum5;
                                str = columnName;
                                compNumShort2 = compNum2;
                                origTmpNum2 = numMatchShort;
                            } else {
                                compNumShort2 = compNum2;
                                tmpCompNum2 = tmpCompNum;
                                compNumAreaCode2 = str3;
                                origTmpNum2 = fixedIndex2;
                                NUM_SHORT = NUM_LONG;
                                NUM_LONG = str;
                                str = columnName;
                            }
                        } else {
                            compNumLen = NUM_LONG;
                            compNum4 = compNum3;
                            if (cursor.moveToFirst()) {
                                str = columnName;
                                fixedIndex = cursor2.getColumnIndex(str);
                                NUM_LONG = cursor2.getColumnIndex("normalized_number");
                                numMatchShort = cursor2.getColumnIndex("data4");
                                origTmpNum2 = new StringBuilder();
                                origTmpNum2.append("5: columnIndex: ");
                                origTmpNum2.append(fixedIndex);
                                origTmpNum2.append(", formatColumnIndex: ");
                                origTmpNum2.append(NUM_LONG);
                                origTmpNum2.append(", data4ColumnIndex: ");
                                origTmpNum2.append(numMatchShort);
                                logd(origTmpNum2.toString());
                                if (fixedIndex != -1) {
                                    String tmpNumFormat4;
                                    int countryCodeLen4;
                                    String tmpNumPrefix5;
                                    origTmpNum2 = fixedIndex2;
                                    while (true) {
                                        origTmpNum3 = cursor2.getString(fixedIndex);
                                        int i7;
                                        if (origTmpNum3 == null || origTmpNum3.indexOf(64) >= 0) {
                                            i7 = NUM_LONG;
                                            tmpCompNum2 = tmpCompNum;
                                            compNumAreaCode2 = str3;
                                            tmpCompNum = compNumLen;
                                        } else {
                                            origTmpNum3 = PhoneNumberUtils.stripSeparators(origTmpNum3);
                                            stringBuilder9 = new StringBuilder();
                                            int columnIndex4 = fixedIndex;
                                            stringBuilder9.append("origTmpNum: ");
                                            stringBuilder9.append(origTmpNum3);
                                            logd(stringBuilder9.toString());
                                            if (-1 != NUM_LONG) {
                                                fixedIndex = cursor2.getString(NUM_LONG);
                                                compNumShort2 = isValidData4Number(origTmpNum3, fixedIndex) ? fixedIndex : origTmpNum3;
                                            } else if (-1 != numMatchShort) {
                                                fixedIndex = cursor2.getString(numMatchShort);
                                                compNumShort2 = isValidData4Number(origTmpNum3, fixedIndex) ? fixedIndex : origTmpNum3;
                                            } else {
                                                compNumShort2 = origTmpNum3;
                                                fixedIndex = str6;
                                            }
                                            stringBuilder9 = new StringBuilder();
                                            i7 = NUM_LONG;
                                            stringBuilder9.append("5: tmpNumFormat: ");
                                            stringBuilder9.append(fixedIndex);
                                            logd(stringBuilder9.toString());
                                            NUM_LONG = compNumShort2.length();
                                            stringBuilder9 = new StringBuilder();
                                            tmpNumFormat4 = fixedIndex;
                                            stringBuilder9.append("5: tmpNum = ");
                                            stringBuilder9.append(compNumShort2);
                                            stringBuilder9.append(", tmpNum.length: ");
                                            stringBuilder9.append(compNumShort2.length());
                                            stringBuilder9.append(",ID = ");
                                            stringBuilder9.append(cursor.getPosition());
                                            logd(stringBuilder9.toString());
                                            if (compNumShort2.equals(tmpCompNum) != 0) {
                                                fixedIndex = cursor.getPosition();
                                                origTmpNum2 = new StringBuilder();
                                                origTmpNum2.append("5: break! numLongID = ");
                                                origTmpNum2.append(fixedIndex);
                                                origTmpNum2.append(", formattedNum full match!");
                                                logd(origTmpNum2.toString());
                                                tmpCompNum2 = tmpCompNum;
                                                compNumAreaCode2 = str3;
                                                countryCodeLen4 = countryCodeLen;
                                                tmpNumPrefix5 = str4;
                                                NUM_LONG = compNum4;
                                                NUM_SHORT = compNumLen;
                                                break;
                                            }
                                            tmpNumFormat = null;
                                            String tmpNumPrefix6 = 0;
                                            fixedIndex = getIntlPrefixAndCCLen(compNumShort2);
                                            if (fixedIndex > 0) {
                                                tmpCompNum2 = tmpCompNum;
                                                tmpCompNum = compNumShort2.substring(0, fixedIndex);
                                                compNumShort2 = compNumShort2.substring(fixedIndex);
                                                stringBuilder4 = new StringBuilder();
                                                countryCodeLen4 = fixedIndex;
                                                stringBuilder4.append("5: tmpNum after remove prefix: ");
                                                stringBuilder4.append(compNumShort2);
                                                stringBuilder4.append(", tmpNum.length5: ");
                                                stringBuilder4.append(compNumShort2.length());
                                                stringBuilder4.append(", tmpNumPrefix: ");
                                                stringBuilder4.append(tmpCompNum);
                                                logd(stringBuilder4.toString());
                                            } else {
                                                countryCodeLen4 = fixedIndex;
                                                int i8 = NUM_LONG;
                                                tmpCompNum2 = tmpCompNum;
                                                tmpCompNum = tmpNumPrefix6;
                                            }
                                            if (isEqualCountryCodePrefix(compNumPrefix, str2, tmpCompNum, 0)) {
                                                if (isCnNumber) {
                                                    formattedCompNum2 = deleteIPHead(compNumShort2);
                                                    boolean isTmpNumCnMPN3 = isChineseMobilePhoneNumber(formattedCompNum2);
                                                    if ((!isCompNumCnMPN || isTmpNumCnMPN3) && (isCompNumCnMPN || !isTmpNumCnMPN3)) {
                                                        if (isCompNumCnMPN && isTmpNumCnMPN3) {
                                                            logd("5: compNum and tmpNum are both MPN, continue to match by mccmnc");
                                                            compNumShort2 = formattedCompNum2;
                                                            tmpNumPrefix5 = tmpCompNum;
                                                        } else {
                                                            fixedIndex = getChineseFixNumberAreaCodeLength(formattedCompNum2);
                                                            int areaCodeLen2;
                                                            if (fixedIndex > 0) {
                                                                tmpNumPrefix5 = tmpCompNum;
                                                                tmpNumFormat = formattedCompNum2.substring(null, fixedIndex);
                                                                formattedCompNum2 = formattedCompNum2.substring(fixedIndex);
                                                                tmpCompNum = new StringBuilder();
                                                                areaCodeLen2 = fixedIndex;
                                                                tmpCompNum.append("5: CN tmpNum after remove area code: ");
                                                                tmpCompNum.append(formattedCompNum2);
                                                                tmpCompNum.append(", tmpNum.length5: ");
                                                                tmpCompNum.append(formattedCompNum2.length());
                                                                tmpCompNum.append(", tmpNumAreaCode: ");
                                                                tmpCompNum.append(tmpNumFormat);
                                                                logd(tmpCompNum.toString());
                                                            } else {
                                                                areaCodeLen2 = fixedIndex;
                                                                tmpNumPrefix5 = tmpCompNum;
                                                            }
                                                            if (isEqualChineseFixNumberAreaCode(str3, tmpNumFormat) == 0) {
                                                                logd("5: areacode prefix not same, continue");
                                                                tmpNum = formattedCompNum2;
                                                            } else {
                                                                compNumShort2 = formattedCompNum2;
                                                            }
                                                        }
                                                        compNumAreaCode2 = str3;
                                                    } else {
                                                        logd("5: compNum and tmpNum not both MPN, continue");
                                                        tmpNum = formattedCompNum2;
                                                        tmpNumPrefix5 = tmpCompNum;
                                                    }
                                                    compNumAreaCode2 = str3;
                                                    NUM_LONG = compNum4;
                                                    NUM_SHORT = compNumLen;
                                                    if (cursor.moveToNext() != 0) {
                                                        fixedIndex = origTmpNum2;
                                                        compNumShort2 = tmpNum;
                                                        break;
                                                    }
                                                    compNum4 = NUM_LONG;
                                                    compNumLen = NUM_SHORT;
                                                    origTmpNum = origTmpNum3;
                                                    fixedIndex = columnIndex4;
                                                    NUM_LONG = i7;
                                                    str6 = tmpNumFormat4;
                                                    tmpCompNum = tmpCompNum2;
                                                    countryCodeLen = countryCodeLen4;
                                                    str4 = tmpNumPrefix5;
                                                    str3 = compNumAreaCode2;
                                                } else {
                                                    tmpNumPrefix5 = tmpCompNum;
                                                    if (compNumShort2.length() >= 7) {
                                                        compNumAreaCode2 = str3;
                                                        if ("0".equals(compNumShort2.substring(null, 1)) != 0) {
                                                            if ("0".equals(compNumShort2.substring(1, 2)) == 0) {
                                                                compNumShort2 = compNumShort2.substring(1);
                                                                logd("5: tmpNum remove 0 at beginning");
                                                            }
                                                        }
                                                    } else {
                                                        compNumAreaCode2 = str3;
                                                    }
                                                    fixedIndex = compNumShort2.length();
                                                    NUM_SHORT = compNumLen;
                                                    int i9;
                                                    if (fixedIndex == NUM_SHORT) {
                                                        i9 = fixedIndex;
                                                        NUM_LONG = compNum4;
                                                        logd("5: continue");
                                                    } else if (-1 == origTmpNum2) {
                                                        NUM_LONG = compNum4;
                                                        if (NUM_LONG.compareTo(compNumShort2) == 0) {
                                                            origTmpNum2 = cursor.getPosition();
                                                            stringBuilder3 = new StringBuilder();
                                                            i9 = fixedIndex;
                                                            stringBuilder3.append("5: break! numLongID = ");
                                                            stringBuilder3.append(origTmpNum2);
                                                            logd(stringBuilder3.toString());
                                                        }
                                                    } else {
                                                        NUM_LONG = compNum4;
                                                    }
                                                }
                                                fixedIndex = compNumShort2.length();
                                                NUM_SHORT = compNumLen;
                                                if (fixedIndex == NUM_SHORT) {
                                                }
                                            } else {
                                                tmpNumPrefix5 = tmpCompNum;
                                                compNumAreaCode2 = str3;
                                                NUM_LONG = compNum4;
                                                NUM_SHORT = compNumLen;
                                                logd("5: countrycode prefix not same, continue");
                                            }
                                            tmpNum = compNumShort2;
                                            if (cursor.moveToNext() != 0) {
                                            }
                                        }
                                    }
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("5: fixedIndex = ");
                                    stringBuilder3.append(fixedIndex);
                                    logd(stringBuilder3.toString());
                                    origTmpNum2 = fixedIndex;
                                    tmpNum = compNumShort2;
                                    origTmpNum = origTmpNum3;
                                    compNumShort2 = compNumShort;
                                    str6 = tmpNumFormat4;
                                    countryCodeLen = countryCodeLen4;
                                    str4 = tmpNumPrefix5;
                                } else {
                                    tmpCompNum2 = tmpCompNum;
                                    compNumAreaCode2 = str3;
                                    NUM_LONG = compNum4;
                                    NUM_SHORT = compNumLen;
                                }
                            } else {
                                tmpCompNum2 = tmpCompNum;
                                compNumAreaCode2 = str3;
                                NUM_LONG = compNum4;
                                NUM_SHORT = compNumLen;
                                str = columnName;
                            }
                            compNumShort2 = compNumShort;
                            origTmpNum2 = fixedIndex2;
                        }
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("fixedIndex: ");
                    stringBuilder2.append(origTmpNum2);
                    logd(stringBuilder2.toString());
                    String str9;
                    String str10;
                    String str11;
                    if (-1 != origTmpNum2) {
                        origCompNum2 = origCompNum3;
                        int compNumLen2;
                        if (shouldDoNumberMatchAgainBySimMccmnc(origCompNum2, str2)) {
                            compNumShort = NUM_LONG;
                            compNumLen2 = NUM_SHORT;
                            NUM_SHORT2 = i;
                            origTmpNum2 = getCallerIndexInternal(cursor2, origCompNum2, str, this.mSimNumLong, this.mSimNumShort);
                        } else {
                            compNumShort = NUM_LONG;
                            compNumLen2 = NUM_SHORT;
                            NUM_SHORT2 = i;
                            origTmpNum3 = origCompNum2;
                            str9 = formattedCompNum3;
                            str10 = tmpCompNum2;
                            str11 = compNumAreaCode2;
                        }
                    } else {
                        compNumShort = NUM_LONG;
                        origCompNum = NUM_SHORT;
                        NUM_SHORT2 = i;
                        str9 = formattedCompNum3;
                        origTmpNum3 = origCompNum3;
                        str10 = tmpCompNum2;
                        str11 = compNumAreaCode2;
                    }
                    return origTmpNum2;
                }
            }
            String str12 = compNum2;
            formattedCompNum = formattedCompNum2;
            NUM_LONG = getIntlPrefixAndCCLen(compNum2);
            if (NUM_LONG <= 0) {
            }
            if (TextUtils.isEmpty(formattedCompNum)) {
            }
            tmpCompNum = TextUtils.isEmpty(formattedCompNum) ? formattedCompNum : origCompNum2;
            if (isRoamingCountryNumberByPrefix(compNumPrefix, str2)) {
            }
            isCnNumber = isChineseNumberByPrefix(compNumPrefix, str2);
            if (isCnNumber) {
            }
            NUM_LONG = origTmpNum2.length();
            if (NUM_LONG < compNumAreaCode) {
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("fixedIndex: ");
            stringBuilder2.append(origTmpNum2);
            logd(stringBuilder2.toString());
            if (-1 != origTmpNum2) {
            }
            return origTmpNum2;
        }
        str3 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("CallerInfoHW(), cursor is empty! fixedIndex = ");
        stringBuilder.append(-1);
        Log.e(str3, stringBuilder.toString());
        return -1;
    }

    public static boolean isfixedIndexValid(String cookie, Cursor cursor) {
        int fixedIndex = new CallerInfoHW().getCallerIndex(cursor, cookie, "number");
        return fixedIndex != -1 && cursor.moveToPosition(fixedIndex);
    }

    private static void logd(String msg) {
    }

    private int getIntlPrefixLength(String number) {
        if (TextUtils.isEmpty(number) || isNormalPrefix(number)) {
            return 0;
        }
        int len = INTERNATIONAL_PREFIX.length;
        for (int i = 0; i < len; i++) {
            if (number.startsWith(INTERNATIONAL_PREFIX[i])) {
                return INTERNATIONAL_PREFIX[i].length();
            }
        }
        return 0;
    }

    public int getIntlPrefixAndCCLen(String number) {
        if (TextUtils.isEmpty(number)) {
            return 0;
        }
        int len = getIntlPrefixLength(number);
        if (len > 0) {
            String tmpNumber = number.substring(len);
            for (Integer countrycode : this.countryCallingCodeToRegionCodeMap.keySet()) {
                int countrycode2 = countrycode.intValue();
                if (tmpNumber.startsWith(Integer.toString(countrycode2))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("extractCountryCodeFromNumber(), find matched country code: ");
                    stringBuilder.append(countrycode2);
                    logd(stringBuilder.toString());
                    return len + Integer.toString(countrycode2).length();
                }
            }
            logd("extractCountryCodeFromNumber(), no matched country code");
            len = 0;
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("extractCountryCodeFromNumber(), no valid prefix in number: ");
            stringBuilder2.append(number);
            logd(stringBuilder2.toString());
        }
        return len;
    }

    private boolean isChineseMobilePhoneNumber(String number) {
        if (TextUtils.isEmpty(number) || number.length() < 11 || !number.substring(number.length() - 11).matches(CN_MPN_PATTERN)) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isChineseMobilePhoneNumber(), return true for number: ");
        stringBuilder.append(number);
        logd(stringBuilder.toString());
        return true;
    }

    private int getChineseFixNumberAreaCodeLength(String number) {
        int len = 0;
        String tmpNumber = number;
        int i = 0;
        if (TextUtils.isEmpty(tmpNumber) || tmpNumber.length() < 9) {
            return 0;
        }
        StringBuilder stringBuilder;
        if (!tmpNumber.startsWith("0")) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("0");
            stringBuilder.append(tmpNumber);
            tmpNumber = stringBuilder.toString();
        }
        int i2 = 2;
        String top2String = tmpNumber.substring(0, 2);
        String areaCodeString;
        ArrayList<String> areaCodeArray;
        int areaCodeArraySize;
        if (top2String.equals(FIXED_NUMBER_TOP2_TOKEN1) || top2String.equals(FIXED_NUMBER_TOP2_TOKEN2)) {
            areaCodeString = tmpNumber.substring(0, 3);
            areaCodeArray = (ArrayList) this.chineseFixNumberAreaCodeMap.get(Integer.valueOf(1));
            areaCodeArraySize = areaCodeArray.size();
            while (i < areaCodeArraySize) {
                if (areaCodeString.equals(areaCodeArray.get(i))) {
                    if (tmpNumber.equals(number)) {
                        i2 = 3;
                    }
                    len = i2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getChineseFixNumberAreaCodeLength(), matched area code len: ");
                    stringBuilder.append(len);
                    stringBuilder.append(", number: ");
                    stringBuilder.append(number);
                    logd(stringBuilder.toString());
                } else {
                    i++;
                }
            }
        } else {
            areaCodeArraySize = 4;
            areaCodeString = tmpNumber.substring(0, 4);
            areaCodeArray = (ArrayList) this.chineseFixNumberAreaCodeMap.get(Integer.valueOf(2));
            i2 = areaCodeArray.size();
            while (i < i2) {
                if (areaCodeString.equals(areaCodeArray.get(i))) {
                    if (!tmpNumber.equals(number)) {
                        areaCodeArraySize = 3;
                    }
                    len = areaCodeArraySize;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getChineseFixNumberAreaCodeLength(), matched area code len: ");
                    stringBuilder2.append(len);
                    stringBuilder2.append(", number: ");
                    stringBuilder2.append(number);
                    logd(stringBuilder2.toString());
                } else {
                    i++;
                }
            }
        }
        return len;
    }

    private boolean isEqualChineseFixNumberAreaCode(String compNumAreaCode, String dbNumAreaCode) {
        if (TextUtils.isEmpty(compNumAreaCode) && TextUtils.isEmpty(dbNumAreaCode)) {
            return true;
        }
        if (!TextUtils.isEmpty(compNumAreaCode) || TextUtils.isEmpty(dbNumAreaCode)) {
            if (TextUtils.isEmpty(compNumAreaCode) || !TextUtils.isEmpty(dbNumAreaCode)) {
                StringBuilder stringBuilder;
                Object dbNumAreaCode2;
                if (!compNumAreaCode.startsWith("0")) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("0");
                    stringBuilder.append(compNumAreaCode);
                    compNumAreaCode = stringBuilder.toString();
                }
                if (!dbNumAreaCode2.startsWith("0")) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("0");
                    stringBuilder.append(dbNumAreaCode2);
                    dbNumAreaCode2 = stringBuilder.toString();
                }
                return compNumAreaCode.equals(dbNumAreaCode2);
            } else if (this.IS_MIIT_NUM_MATCH) {
                return false;
            } else {
                return true;
            }
        } else if (this.IS_MIIT_NUM_MATCH) {
            return false;
        } else {
            return true;
        }
    }

    private String deleteIPHead(String number) {
        if (TextUtils.isEmpty(number)) {
            return number;
        }
        int numberLen = number.length();
        if (numberLen < 5) {
            logd("deleteIPHead() numberLen is short than 5!");
            return number;
        }
        if (Arrays.binarySearch(IPHEAD, number.substring(null, 5)) >= 0) {
            number = number.substring(5, numberLen);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("deleteIPHead() new Number: ");
        stringBuilder.append(number);
        logd(stringBuilder.toString());
        return number;
    }

    private boolean isChineseNumberByPrefix(String numberPrefix, String netIso) {
        if (TextUtils.isEmpty(numberPrefix)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isChineseNumberByPrefix(), networkCountryIso: ");
            stringBuilder.append(netIso);
            logd(stringBuilder.toString());
            if (netIso == null || !"CN".equals(netIso.toUpperCase())) {
                return false;
            }
            return true;
        }
        return Integer.toString(this.countryCodeforCN).equals(numberPrefix.substring(getIntlPrefixLength(numberPrefix)));
    }

    private boolean isEqualCountryCodePrefix(String num1Prefix, String netIso1, String num2Prefix, String netIso2) {
        boolean isBothPrefixEmpty = TextUtils.isEmpty(num1Prefix) && TextUtils.isEmpty(num2Prefix);
        if (isBothPrefixEmpty) {
            logd("isEqualCountryCodePrefix(), both have no country code, return true");
            return true;
        }
        boolean ret;
        StringBuilder stringBuilder;
        String netIso;
        int countryCode;
        if (TextUtils.isEmpty(num1Prefix) && !TextUtils.isEmpty(num2Prefix)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("isEqualCountryCodePrefix(), netIso1: ");
            stringBuilder.append(netIso1);
            stringBuilder.append(", netIso2: ");
            stringBuilder.append(netIso2);
            logd(stringBuilder.toString());
            if (TextUtils.isEmpty(netIso1)) {
                ret = true;
            } else {
                netIso = netIso1.toUpperCase();
                if ("CN".equals(netIso)) {
                    countryCode = this.countryCodeforCN;
                } else {
                    countryCode = sInstance.getCountryCodeForRegion(netIso);
                }
                ret = num2Prefix.substring(getIntlPrefixLength(num2Prefix)).equals(Integer.toString(countryCode));
            }
        } else if (TextUtils.isEmpty(num1Prefix) || !TextUtils.isEmpty(num2Prefix)) {
            ret = num1Prefix.substring(getIntlPrefixLength(num1Prefix)).equals(num2Prefix.substring(getIntlPrefixLength(num2Prefix)));
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("isEqualCountryCodePrefix(), netIso1: ");
            stringBuilder.append(netIso1);
            stringBuilder.append(", netIso2: ");
            stringBuilder.append(netIso2);
            logd(stringBuilder.toString());
            if (TextUtils.isEmpty(netIso2)) {
                ret = true;
            } else {
                netIso = netIso2.toUpperCase();
                if ("CN".equals(netIso)) {
                    countryCode = this.countryCodeforCN;
                } else {
                    countryCode = sInstance.getCountryCodeForRegion(netIso);
                }
                ret = num1Prefix.substring(getIntlPrefixLength(num1Prefix)).equals(Integer.toString(countryCode));
            }
        }
        return ret;
    }

    private int getFullMatchIndex(Cursor cursor, String compNum, String columnName) {
        compNum = PhoneNumberUtils.stripSeparators(compNum);
        if (this.IS_CHINA_TELECOM && compNum.startsWith("**133") && compNum.endsWith("#")) {
            compNum = compNum.substring(0, compNum.length() - 1);
            logd("full match check, compNum startsWith **133 && endsWith #");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("full match check, compNum: ");
        stringBuilder.append(compNum);
        logd(stringBuilder.toString());
        if (cursor == null || !cursor.moveToFirst()) {
            return -1;
        }
        int columnIndex = cursor.getColumnIndex(columnName);
        if (-1 == columnIndex) {
            return -1;
        }
        do {
            String tmpNum = cursor.getString(columnIndex);
            if (tmpNum != null && tmpNum.indexOf(64) < 0) {
                tmpNum = PhoneNumberUtils.stripSeparators(tmpNum);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("full match check, tmpNum: ");
            stringBuilder2.append(tmpNum);
            logd(stringBuilder2.toString());
            if (compNum.equals(tmpNum)) {
                int fixedIndex = cursor.getPosition();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("exact match: break! fixedIndex = ");
                stringBuilder2.append(fixedIndex);
                logd(stringBuilder2.toString());
                return fixedIndex;
            }
        } while (cursor.moveToNext());
        return -1;
    }

    private boolean shouldDoNumberMatchAgainBySimMccmnc(String number, String countryIso) {
        if (SystemProperties.getBoolean(HwTelephonyProperties.PROPERTY_NETWORK_ISROAMING, false) && getFormatNumberByCountryISO(number, countryIso) == null) {
            return true;
        }
        return false;
    }

    private String getFormatNumberByCountryISO(String number, String countryIso) {
        if (TextUtils.isEmpty(number) || TextUtils.isEmpty(countryIso)) {
            return null;
        }
        return PhoneNumberUtils.formatNumberToE164(number, countryIso.toUpperCase(Locale.US));
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x01dd A:{LOOP_END, LOOP:0: B:21:0x00bf->B:53:0x01dd} */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x01b5 A:{SYNTHETIC, EDGE_INSN: B:126:0x01b5->B:46:0x01b5 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x032a A:{LOOP_END, LOOP:1: B:62:0x0215->B:94:0x032a} */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x0302 A:{SYNTHETIC, EDGE_INSN: B:127:0x0302->B:87:0x0302 ?: BREAK  } */
    /* JADX WARNING: Missing block: B:55:0x01e6, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:87:0x0302, code skipped:
            r3 = new java.lang.StringBuilder();
            r3.append("7: numShortID = ");
            r3.append(r10);
            r3.append(", numLongID = ");
            r3.append(r11);
            logd(r3.toString());
     */
    /* JADX WARNING: Missing block: B:88:0x031f, code skipped:
            if (-1 == r10) goto L_0x0324;
     */
    /* JADX WARNING: Missing block: B:89:0x0321, code skipped:
            r3 = r10;
     */
    /* JADX WARNING: Missing block: B:90:0x0322, code skipped:
            r12 = r3;
     */
    /* JADX WARNING: Missing block: B:91:0x0324, code skipped:
            if (-1 == r11) goto L_0x0328;
     */
    /* JADX WARNING: Missing block: B:92:0x0326, code skipped:
            r3 = r11;
     */
    /* JADX WARNING: Missing block: B:93:0x0328, code skipped:
            r3 = -1;
     */
    /* JADX WARNING: Missing block: B:120:0x03d9, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getCallerIndexInternal(Cursor cursor, String compNum, String columnName, int numLong, int numShort) {
        Cursor cursor2 = cursor;
        String str = compNum;
        String str2 = columnName;
        int i = numLong;
        int i2 = numShort;
        String compNumLong = null;
        String tmpNumShort = null;
        int numShortID = -1;
        int numLongID = -1;
        int fixedIndex = -1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCallerIndexInternal, compNum: ");
        stringBuilder.append(str);
        stringBuilder.append(", numLong: ");
        stringBuilder.append(i);
        stringBuilder.append(", numShort: ");
        stringBuilder.append(i2);
        logd(stringBuilder.toString());
        if (TextUtils.isEmpty(compNum)) {
            if (cursor2 != null && cursor.getCount() > 0) {
                fixedIndex = 0;
            }
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCallerIndexInternal(),null == compNum! fixedIndex = ");
            stringBuilder2.append(fixedIndex);
            Log.e(str3, stringBuilder2.toString());
            return fixedIndex;
        }
        String str4;
        StringBuilder stringBuilder3;
        int compNumLen = compNum.length();
        int NUM_LONG = 7;
        if (i >= 7) {
            NUM_LONG = i;
        }
        int NUM_SHORT = i2 >= NUM_LONG ? NUM_LONG : i2;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("getCallerIndexInternal, after check NUM_LONG: ");
        stringBuilder4.append(NUM_LONG);
        stringBuilder4.append(", NUM_SHORT: ");
        stringBuilder4.append(NUM_SHORT);
        logd(stringBuilder4.toString());
        if (cursor2 != null) {
            String compNumShort;
            String tmpNum;
            String tmpNum2;
            StringBuilder stringBuilder5;
            if (compNumLen >= NUM_LONG) {
                compNumLong = str.substring(compNumLen - NUM_LONG);
                compNumShort = str.substring(compNumLen - NUM_SHORT);
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("11: compNumLong = ");
                stringBuilder4.append(compNumLong);
                stringBuilder4.append(", compNumShort = ");
                stringBuilder4.append(compNumShort);
                logd(stringBuilder4.toString());
                if (cursor.moveToFirst()) {
                    i = cursor2.getColumnIndex(str2);
                    if (i != -1) {
                        while (true) {
                            tmpNum = cursor2.getString(i);
                            String str5;
                            if (tmpNum == null || tmpNum.indexOf(64) >= 0) {
                                str5 = tmpNumShort;
                            } else {
                                tmpNum2 = PhoneNumberUtils.stripSeparators(tmpNum);
                                i2 = tmpNum2.length();
                                int columnIndex = i;
                                i = new StringBuilder();
                                str5 = tmpNumShort;
                                i.append("11: tmpNum = ");
                                i.append(tmpNum2);
                                i.append(", tmpNum.length11: ");
                                i.append(tmpNum2.length());
                                i.append(", ID = ");
                                i.append(cursor.getPosition());
                                logd(i.toString());
                                StringBuilder stringBuilder6;
                                if (str.equals(tmpNum2) != 0) {
                                    i = cursor.getPosition();
                                    stringBuilder6 = new StringBuilder();
                                    stringBuilder6.append("exact match: break! numLongID = ");
                                    stringBuilder6.append(i);
                                    logd(stringBuilder6.toString());
                                    numLongID = i;
                                    tmpNumShort = str5;
                                    break;
                                }
                                if (i2 >= NUM_LONG) {
                                    i = tmpNum2.substring(i2 - NUM_LONG);
                                    if (-1 == numLongID && compNumLong.compareTo(i) == 0) {
                                        numLongID = cursor.getPosition();
                                        stringBuilder6 = new StringBuilder();
                                        stringBuilder6.append("11: > NUM_LONG numLongID = ");
                                        stringBuilder6.append(numLongID);
                                        logd(stringBuilder6.toString());
                                    }
                                    stringBuilder6 = new StringBuilder();
                                    stringBuilder6.append("11: >= NUM_LONG, and !=,  tmpNumLong = ");
                                    stringBuilder6.append(i);
                                    stringBuilder6.append(", numLongID: ");
                                    stringBuilder6.append(numLongID);
                                    logd(stringBuilder6.toString());
                                    Object obj = i;
                                } else if (i2 >= NUM_SHORT) {
                                    i = tmpNum2.substring(i2 - NUM_SHORT);
                                    if (-1 == numShortID && compNumShort.compareTo(i) == 0) {
                                        numShortID = cursor.getPosition();
                                    }
                                    stringBuilder6 = new StringBuilder();
                                    stringBuilder6.append("11: >= NUM_SHORT, tmpNumShort = ");
                                    stringBuilder6.append(i);
                                    stringBuilder6.append(", numShortID:");
                                    stringBuilder6.append(numShortID);
                                    logd(stringBuilder6.toString());
                                    tmpNumShort = i;
                                    if (cursor.moveToNext() != 0) {
                                        break;
                                    }
                                    i = columnIndex;
                                } else {
                                    logd("tmpNum11, continue");
                                }
                                tmpNumShort = str5;
                                if (cursor.moveToNext() != 0) {
                                }
                            }
                        }
                        i = new StringBuilder();
                        i.append("11: numLongID = ");
                        i.append(numLongID);
                        i.append(", numShortID = ");
                        i.append(numShortID);
                        logd(i.toString());
                        if (-1 != numLongID) {
                            i = numLongID;
                        } else if (-1 != numShortID) {
                            i = numShortID;
                        } else {
                            i = -1;
                        }
                        fixedIndex = i;
                    }
                }
            } else if (compNumLen >= NUM_SHORT) {
                compNumShort = str.substring(compNumLen - NUM_SHORT);
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("7: compNumShort = ");
                stringBuilder4.append(compNumShort);
                logd(stringBuilder4.toString());
                if (cursor.moveToFirst()) {
                    i = cursor2.getColumnIndex(str2);
                    if (i != -1) {
                        while (true) {
                            tmpNum = cursor2.getString(i);
                            if (tmpNum == null || tmpNum.indexOf(64) >= 0) {
                                str4 = compNumLong;
                            } else {
                                tmpNum2 = PhoneNumberUtils.stripSeparators(tmpNum);
                                i2 = tmpNum2.length();
                                int columnIndex2 = i;
                                i = new StringBuilder();
                                str4 = compNumLong;
                                i.append("7: tmpNum = ");
                                i.append(tmpNum2);
                                i.append(", tmpNum.length7: ");
                                i.append(tmpNum2.length());
                                i.append(", ID = ");
                                i.append(cursor.getPosition());
                                logd(i.toString());
                                if (str.equals(tmpNum2) != 0) {
                                    i = cursor.getPosition();
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("exact match numShortID = ");
                                    stringBuilder5.append(i);
                                    logd(stringBuilder5.toString());
                                    numShortID = i;
                                    break;
                                }
                                if (i2 >= NUM_LONG) {
                                    i = tmpNum2.substring(i2 - NUM_SHORT);
                                    if (-1 == numLongID && compNumShort.compareTo(i) == 0) {
                                        numLongID = cursor.getPosition();
                                    }
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("7: >= NUM_LONG, tmpNumShort = ");
                                    stringBuilder5.append(i);
                                    stringBuilder5.append(", numLongID:");
                                    stringBuilder5.append(numLongID);
                                    logd(stringBuilder5.toString());
                                } else if (i2 >= NUM_SHORT) {
                                    i = tmpNum2.substring(i2 - NUM_SHORT);
                                    if (-1 == numShortID && compNumShort.compareTo(i) == 0) {
                                        numShortID = cursor.getPosition();
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("7: >= NUM_SHORT numShortID = ");
                                        stringBuilder5.append(numShortID);
                                        logd(stringBuilder5.toString());
                                    }
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("7: >= NUM_SHORT, and !=, tmpNumShort = ");
                                    stringBuilder5.append(i);
                                    stringBuilder5.append(", numShortID:");
                                    stringBuilder5.append(numShortID);
                                    logd(stringBuilder5.toString());
                                } else {
                                    logd("7: continue");
                                    if (cursor.moveToNext() != 0) {
                                        break;
                                    }
                                    i = columnIndex2;
                                    compNumLong = str4;
                                }
                                tmpNumShort = i;
                                if (cursor.moveToNext() != 0) {
                                }
                            }
                        }
                        str4 = compNumLong;
                        return -1;
                    }
                    str4 = null;
                }
            } else {
                str4 = null;
                if (cursor.moveToFirst()) {
                    i2 = cursor2.getColumnIndex(str2);
                    if (i2 != -1) {
                        while (true) {
                            String tmpNum3 = cursor2.getString(i2);
                            if (tmpNum3 != null && tmpNum3.indexOf(64) < 0) {
                                tmpNum2 = PhoneNumberUtils.stripSeparators(tmpNum3);
                                i = tmpNum2.length();
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("5: tmpNum = ");
                                stringBuilder5.append(tmpNum2);
                                stringBuilder5.append(", tmpNum.length: ");
                                stringBuilder5.append(tmpNum2.length());
                                stringBuilder5.append(", ID = ");
                                stringBuilder5.append(cursor.getPosition());
                                logd(stringBuilder5.toString());
                                if (i == compNumLen) {
                                    if (-1 == -1 && str.compareTo(tmpNum2) == 0) {
                                        fixedIndex = cursor.getPosition();
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("5: break! numLongID = ");
                                        stringBuilder3.append(fixedIndex);
                                        logd(stringBuilder3.toString());
                                        break;
                                    }
                                }
                                logd("5: continue");
                                if (!cursor.moveToNext()) {
                                    break;
                                }
                                str2 = columnName;
                            }
                        }
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("5: fixedIndex = ");
                        stringBuilder3.append(fixedIndex);
                        logd(stringBuilder3.toString());
                    }
                }
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getCallerIndexInternal, fixedIndex: ");
            stringBuilder3.append(fixedIndex);
            logd(stringBuilder3.toString());
            return fixedIndex;
        }
        str4 = null;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("getCallerIndexInternal, fixedIndex: ");
        stringBuilder3.append(fixedIndex);
        logd(stringBuilder3.toString());
        return fixedIndex;
    }

    private boolean compareNumsInternal(String num1, String num2, int numLong, int numShort) {
        String str = num1;
        String str2 = num2;
        int i = numLong;
        int i2 = numShort;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("compareNumsInternal, num1: ");
        stringBuilder.append(str);
        stringBuilder.append(", num2: ");
        stringBuilder.append(str2);
        stringBuilder.append(", numLong: ");
        stringBuilder.append(i);
        stringBuilder.append(", numShort: ");
        stringBuilder.append(i2);
        logd(stringBuilder.toString());
        if (TextUtils.isEmpty(num1) || TextUtils.isEmpty(num2)) {
            return false;
        }
        int num1Len = num1.length();
        int num2Len = num2.length();
        int NUM_LONG = 7;
        if (i >= 7) {
            NUM_LONG = i;
        }
        int NUM_SHORT = i2 >= NUM_LONG ? NUM_LONG : i2;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("compareNumsInternal, after check NUM_LONG: ");
        stringBuilder2.append(NUM_LONG);
        stringBuilder2.append(", NUM_SHORT: ");
        stringBuilder2.append(NUM_SHORT);
        logd(stringBuilder2.toString());
        String num1Short;
        StringBuilder stringBuilder3;
        if (num1Len >= NUM_LONG) {
            String num1Long = str.substring(num1Len - NUM_LONG);
            num1Short = str.substring(num1Len - NUM_SHORT);
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("compareNumsInternal, 11: num1Long = ");
            stringBuilder3.append(num1Long);
            stringBuilder3.append(", num1Short = ");
            stringBuilder3.append(num1Short);
            logd(stringBuilder3.toString());
            if (num2Len >= NUM_LONG) {
                if (num1Long.compareTo(str2.substring(num2Len - NUM_LONG)) == 0) {
                    logd("compareNumsInternal, 11: >= NUM_LONG return true");
                    return true;
                }
            } else if (num2Len >= NUM_SHORT && num1Short.compareTo(str2.substring(num2Len - NUM_SHORT)) == 0) {
                logd("compareNumsInternal, 11: >= NUM_SHORT return true");
                return true;
            }
        } else if (num1Len >= NUM_SHORT) {
            num1Short = str.substring(num1Len - NUM_SHORT);
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("compareNumsInternal, 7: num1Short = ");
            stringBuilder3.append(num1Short);
            logd(stringBuilder3.toString());
            if (num2Len >= NUM_SHORT && num1Short.compareTo(str2.substring(num2Len - NUM_SHORT)) == 0) {
                logd("compareNumsInternal, 7: >= NUM_SHORT return true");
                return true;
            }
        } else {
            logd("compareNumsInternal, 5: do full compare");
            return num1.equals(num2);
        }
        return false;
    }

    private boolean isRoamingCountryNumberByPrefix(String numberPrefix, String netIso) {
        if (SystemProperties.getBoolean(HwTelephonyProperties.PROPERTY_NETWORK_ISROAMING, false)) {
            if (TextUtils.isEmpty(numberPrefix)) {
                return true;
            }
            numberPrefix = numberPrefix.substring(getIntlPrefixLength(numberPrefix));
            if (!(TextUtils.isEmpty(numberPrefix) || TextUtils.isEmpty(netIso))) {
                return numberPrefix.equals(Integer.toString(sInstance.getCountryCodeForRegion(netIso.toUpperCase(Locale.US))));
            }
        }
        return false;
    }

    private boolean isValidData4Number(String data1Num, String data4Num) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isValidData4Number, data1Num: ");
        stringBuilder.append(data1Num);
        stringBuilder.append(", data4Num: ");
        stringBuilder.append(data4Num);
        logd(stringBuilder.toString());
        if (!(TextUtils.isEmpty(data1Num) || TextUtils.isEmpty(data4Num) || !data4Num.startsWith("+"))) {
            int countryCodeLen = getIntlPrefixAndCCLen(data4Num);
            if (countryCodeLen > 0) {
                data4Num = data4Num.substring(countryCodeLen);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isValidData4Number, data4Num after remove prefix: ");
            stringBuilder2.append(data4Num);
            logd(stringBuilder2.toString());
            if (data4Num.length() <= data1Num.length() && data4Num.equals(data1Num.substring(data1Num.length() - data4Num.length()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isNormalPrefix(String number) {
        String sMcc = " ";
        if (this.mNetworkOperator != null && this.mNetworkOperator.length() > 3) {
            sMcc = this.mNetworkOperator.substring(0, 3);
        }
        if (number.startsWith("011")) {
            for (Object equals : NORMAL_PREFIX_MCC) {
                if (sMcc.equals(equals) && number.length() == 11) {
                    logd("those operator 011 are normal prefix");
                    return true;
                }
            }
        }
        return false;
    }

    private static String formatedForDualNumber(String compNum) {
        if (!IS_SUPPORT_DUAL_NUMBER) {
            return compNum;
        }
        if (isVirtualNum(compNum)) {
            compNum = compNum.substring(0, compNum.length() - 1);
        }
        if (compNum.startsWith("*230#")) {
            compNum = compNum.substring(5, compNum.length());
        } else if (compNum.startsWith("*23#")) {
            compNum = compNum.substring(4, compNum.length());
        }
        return compNum;
    }

    private static boolean isVirtualNum(String dialString) {
        if (!dialString.endsWith("#")) {
            return false;
        }
        String tempstring = dialString.substring(0, dialString.length() - 1).replace(" ", "").replace("+", "").replace("-", "");
        if (tempstring.startsWith("*230#")) {
            tempstring = tempstring.substring(5, tempstring.length());
        } else if (tempstring.startsWith("*23#")) {
            tempstring = tempstring.substring(4, tempstring.length());
        }
        if (tempstring.matches("[0-9]+")) {
            return true;
        }
        return false;
    }
}
