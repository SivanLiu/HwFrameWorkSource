package com.android.internal.telephony.uicc;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.HwGsmAlphabet;
import com.android.internal.telephony.HwSubscriptionManager;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HwIccUtils {
    static final int ADN_BCD_NUMBER_LENGTH = 0;
    static final int ADN_CAPABILITY_ID = 12;
    static final int ADN_DIALING_NUMBER_END = 11;
    static final int ADN_DIALING_NUMBER_START = 2;
    static final int ADN_EXTENSION_ID = 13;
    static final int ADN_TON_AND_NPI = 1;
    static final int EXT_RECORD_LENGTH_BYTES = 13;
    static final int EXT_RECORD_TYPE_ADDITIONAL_DATA = 2;
    static final int EXT_RECORD_TYPE_MASK = 3;
    static final int FOOTER_SIZE_BYTES = 14;
    private static final String LOG_TAG = "HwIccUtils";
    static final int MAX_EXT_CALLED_PARTY_LENGTH = 10;
    static final int MAX_NUMBER_SIZE_BYTES = 11;
    static final int MDN_BYTE_LENGTH = 11;

    public static String adnStringFieldToStringForSTK(byte[] data, int offset, int length) {
        if (length == 0) {
            return "";
        }
        if (length >= 1 && data[offset] == Byte.MIN_VALUE) {
            String ret = null;
            try {
                ret = new String(data, offset + 1, ((length - 1) / 2) * 2, "utf-16be");
            } catch (UnsupportedEncodingException ex) {
                Rlog.e(LOG_TAG, "implausible UnsupportedEncodingException", ex);
            }
            if (ret != null) {
                int ucslen = ret.length();
                while (ucslen > 0 && ret.charAt(ucslen - 1) == 65535) {
                    ucslen--;
                }
                return ret.substring(0, ucslen);
            }
        }
        boolean isucs2 = false;
        char base = 0;
        int len = 0;
        if (length >= 3 && data[offset] == (byte) -127) {
            len = data[offset + 1] & HwSubscriptionManager.SUB_INIT_STATE;
            if (len > length - 3) {
                len = length - 3;
            }
            base = (char) ((data[offset + 2] & HwSubscriptionManager.SUB_INIT_STATE) << 7);
            offset += 3;
            isucs2 = true;
        } else if (length >= 4 && data[offset] == (byte) -126) {
            len = data[offset + 1] & HwSubscriptionManager.SUB_INIT_STATE;
            if (len > length - 4) {
                len = length - 4;
            }
            base = (char) (((data[offset + 2] & HwSubscriptionManager.SUB_INIT_STATE) << 8) | (data[offset + 3] & HwSubscriptionManager.SUB_INIT_STATE));
            offset += 4;
            isucs2 = true;
        }
        if (isucs2) {
            StringBuilder ret2 = new StringBuilder();
            while (len > 0) {
                if (data[offset] < (byte) 0) {
                    ret2.append((char) ((data[offset] & 127) + base));
                    offset++;
                    len--;
                }
                int count = 0;
                while (count < len && data[offset + count] >= (byte) 0) {
                    count++;
                }
                ret2.append(HwGsmAlphabet.gsm8BitUnpackedToString(data, offset, count));
                offset += count;
                len -= count;
            }
            return ret2.toString();
        }
        String defaultCharset = "";
        try {
            defaultCharset = Resources.getSystem().getString(17040159);
        } catch (NotFoundException e) {
        }
        return HwGsmAlphabet.gsm8BitUnpackedToString(data, offset, length, defaultCharset.trim(), true);
    }

    public static int hexCharToInt(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 65) + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 97) + 10;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid hex char '");
        stringBuilder.append(c);
        stringBuilder.append("'");
        throw new RuntimeException(stringBuilder.toString());
    }

    public static byte[] hexStringToBcd(String s) {
        int i = 0;
        if (s == null) {
            return new byte[0];
        }
        int sz = s.length();
        byte[] ret = new byte[(sz / 2)];
        while (i < sz) {
            ret[i / 2] = (byte) ((hexCharToInt(s.charAt(i + 1)) << 4) | hexCharToInt(s.charAt(i)));
            i += 2;
        }
        return ret;
    }

    public static String bcdIccidToString(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);
        char[] cnum = new char[]{'A', 'B', 'C', 'D', 'E', 'F'};
        for (int i = offset; i < offset + length; i++) {
            int v = data[i] & 15;
            if (v > 9) {
                ret.append(cnum[v - 10]);
            } else {
                ret.append((char) (48 + v));
            }
            v = (data[i] >> 4) & 15;
            if (v > 9) {
                ret.append(cnum[v - 10]);
            } else {
                ret.append((char) (48 + v));
            }
        }
        return ret.toString();
    }

    public static byte[] buildAdnStringHw(int recordSize, String mAlphaTag, String mNumber) {
        int gsm7Converts;
        int i = recordSize;
        String str = mAlphaTag;
        String mNumber2 = mNumber;
        int footerOffset = i - 14;
        int ucs80Len = 1;
        int ucs80Converts = 0;
        int ucs81Len = 3;
        int ucs81Converts = 0;
        int ucs82Len = 4;
        int ucs82Converts = 0;
        char baser81 = ' ';
        char baser82Low = ' ';
        char baser82High = ' ';
        boolean useGsm7 = true;
        boolean usePattern81 = true;
        boolean setPattern81 = false;
        boolean usePattern82 = true;
        boolean setPattern82 = false;
        int gsm7Len = 0;
        int gsm7Converts2;
        if (mNumber2 == null) {
            gsm7Converts2 = 0;
        } else if (str == null) {
            gsm7Converts2 = 0;
        } else {
            String mNumber3;
            int i2;
            int i3;
            String mAlphaTag2;
            byte[] bcdNumber;
            gsm7Converts2 = 0;
            if (mNumber.length() > 20) {
                Rlog.w(LOG_TAG, "[buildAdnString] Max length of dialing number is 20");
                mNumber2 = mNumber2.substring(0, 20);
            }
            int lenTag = mAlphaTag.length();
            gsm7Converts = 0;
            while (gsm7Converts < lenTag) {
                if (!useGsm7 && !usePattern81 && !usePattern82 && ucs80Len > footerOffset) {
                    mNumber3 = mNumber2;
                    i2 = lenTag;
                    i3 = gsm7Converts;
                    break;
                }
                i2 = lenTag;
                char c = str.charAt(gsm7Converts);
                mNumber3 = mNumber2;
                mNumber2 = HwGsmAlphabet.UCStoGsm7(c);
                if (mNumber2 == -1) {
                    useGsm7 = false;
                } else if (useGsm7) {
                    gsm7Len += mNumber2;
                    gsm7Converts2++;
                }
                i = gsm7Len;
                if (!usePattern81 || ucs81Len >= footerOffset) {
                    i3 = gsm7Converts;
                } else {
                    if (-1 == mNumber2) {
                        i3 = gsm7Converts;
                        if ((c & 32768) == 32768) {
                            usePattern81 = false;
                        } else if (!setPattern81) {
                            setPattern81 = true;
                            baser81 = (char) (c & 32640);
                        } else if (baser81 != ((char) (c & 32640))) {
                            usePattern81 = false;
                        }
                    } else {
                        i3 = gsm7Converts;
                    }
                    if (usePattern81) {
                        ucs81Converts++;
                        if (-1 == mNumber2) {
                            ucs81Len++;
                        } else {
                            ucs81Len += mNumber2;
                        }
                    }
                }
                if (usePattern82 && ucs82Len < footerOffset) {
                    if (-1 == mNumber2) {
                        if (setPattern82) {
                            if (baser82Low > c) {
                                baser82Low = c;
                            } else if (baser82High < c) {
                                baser82High = c;
                            }
                            if (baser82High - baser82Low > 127) {
                                usePattern82 = false;
                            }
                        } else {
                            setPattern82 = true;
                            baser82Low = c;
                            baser82High = c;
                        }
                    }
                    if (usePattern82) {
                        ucs82Converts++;
                        if (-1 == mNumber2) {
                            ucs82Len++;
                        } else {
                            ucs82Len += mNumber2;
                        }
                    }
                }
                if (ucs80Len < footerOffset) {
                    ucs80Len += 2;
                    ucs80Converts++;
                }
                if (useGsm7) {
                    if (i >= footerOffset) {
                        break;
                    }
                } else if (usePattern81) {
                    if (ucs81Len >= footerOffset) {
                        break;
                    }
                } else if (usePattern82) {
                    if (ucs82Len >= footerOffset) {
                        break;
                    }
                } else if (ucs80Len >= footerOffset) {
                    break;
                }
                gsm7Converts = i3 + 1;
                gsm7Len = i;
                lenTag = i2;
                mNumber2 = mNumber3;
                str = mAlphaTag;
                i = recordSize;
            }
            mNumber3 = mNumber2;
            i2 = lenTag;
            i3 = gsm7Converts;
            i = gsm7Len;
            int bestConverts = gsm7Converts2;
            int bestMode = 0;
            lenTag = i;
            if (bestConverts < ucs81Converts) {
                bestConverts = ucs81Converts;
                bestMode = 129;
                lenTag = ucs81Len;
            }
            if (bestConverts < ucs82Converts) {
                bestConverts = ucs82Converts;
                bestMode = 130;
                lenTag = ucs82Len;
            }
            if (bestConverts < ucs80Converts) {
                bestConverts = ucs80Converts;
                bestMode = 128;
                lenTag = ucs80Len;
            }
            gsm7Converts = bestMode;
            bestMode = bestConverts;
            str = mAlphaTag.substring(0, bestMode);
            if (lenTag > footerOffset) {
                lenTag -= 2;
                mAlphaTag2 = str.substring(0, bestMode - 1);
            } else {
                mAlphaTag2 = str;
            }
            ucs80Len = recordSize;
            byte[] adnString = new byte[ucs80Len];
            for (bestConverts = 0; bestConverts < ucs80Len; bestConverts++) {
                adnString[bestConverts] = (byte) -1;
            }
            String mNumber4 = mNumber3;
            if (mNumber4.length() > 0) {
                bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(mNumber4);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("buildAdnString bcdNumber.length = ");
                stringBuilder.append(bcdNumber.length);
                Rlog.e("AdnRecord", stringBuilder.toString());
                System.arraycopy(bcdNumber, 0, adnString, footerOffset + 1, bcdNumber.length);
                adnString[footerOffset + 0] = (byte) bcdNumber.length;
            } else {
                int i4 = ucs80Converts;
                int i5 = ucs81Len;
            }
            adnString[footerOffset + 12] = (byte) -1;
            adnString[footerOffset + 13] = (byte) -1;
            if (gsm7Converts != 0) {
                byte[] byteTag;
                switch (gsm7Converts) {
                    case 128:
                        try {
                            bcdNumber = mAlphaTag2.getBytes("UTF-16BE");
                            ucs80Len = 0;
                        } catch (UnsupportedEncodingException e) {
                            ucs80Len = 0;
                            bcdNumber = new byte[0];
                        }
                        adnString[ucs80Len] = Byte.MIN_VALUE;
                        System.arraycopy(bcdNumber, ucs80Len, adnString, 1, bcdNumber.length);
                        break;
                    case 129:
                        byteTag = HwGsmAlphabet.stringToUCS81Packed(mAlphaTag2, baser81, lenTag - 2);
                        adnString[0] = (byte) -127;
                        adnString[1] = (byte) (lenTag - 3);
                        System.arraycopy(byteTag, 0, adnString, 2, byteTag.length);
                        break;
                    case 130:
                        byteTag = HwGsmAlphabet.stringToUCS82Packed(mAlphaTag2, baser82Low, lenTag - 2);
                        adnString[0] = (byte) -126;
                        adnString[1] = (byte) (lenTag - 4);
                        System.arraycopy(byteTag, 0, adnString, 2, byteTag.length);
                        break;
                }
            }
            bcdNumber = HwGsmAlphabet.stringToGsm8BitPacked(mAlphaTag2);
            System.arraycopy(bcdNumber, 0, adnString, 0, bcdNumber.length);
            return adnString;
        }
        Rlog.w(LOG_TAG, "[buildAdnString] Empty alpha tag or number");
        byte[] adnString2 = new byte[i];
        int i6 = 0;
        while (true) {
            gsm7Converts = i6;
            if (gsm7Converts >= i) {
                return adnString2;
            }
            adnString2[gsm7Converts] = (byte) -1;
            i6 = gsm7Converts + 1;
        }
    }

    public static int getAlphaTagEncodingLength(String alphaTag) {
        String currGsm7Length = alphaTag;
        int ucs80Len = 1;
        int ucs80Converts = 0;
        int ucs81Len = 3;
        int ucs81Converts = 0;
        int ucs82Len = 4;
        int ucs82Converts = 0;
        char baser81 = ' ';
        char baser82Low = ' ';
        char baser82High = ' ';
        boolean useGsm7 = true;
        boolean usePattern81 = true;
        boolean setPattern81 = false;
        boolean usePattern82 = true;
        boolean setPattern82 = false;
        int index = 0;
        int gsm7Len;
        int gsm7Converts;
        if (currGsm7Length == null) {
            gsm7Len = 0;
            gsm7Converts = 0;
            Rlog.w(LOG_TAG, "[getAlphaTagEncodingLength] Empty alpha tag");
            return 0;
        }
        int index2;
        int currGsm7Length2;
        int index3;
        gsm7Len = 0;
        gsm7Converts = 0;
        int lenTag = alphaTag.length();
        while (true) {
            index2 = index;
            if (index2 >= lenTag) {
                break;
            }
            boolean useGsm72;
            int lenTag2 = lenTag;
            char lenTag3 = currGsm7Length.charAt(index2);
            currGsm7Length2 = HwGsmAlphabet.UCStoGsm7(lenTag3);
            index3 = index2;
            if (currGsm7Length2 == -1) {
                useGsm7 = false;
            } else if (useGsm7) {
                gsm7Len += currGsm7Length2;
                gsm7Converts++;
            }
            if (usePattern81) {
                if (-1 == currGsm7Length2) {
                    useGsm72 = useGsm7;
                    if (lenTag3 & true) {
                        usePattern81 = false;
                    } else if (!setPattern81) {
                        setPattern81 = true;
                        baser81 = (char) (lenTag3 & 32640);
                    } else if (baser81 != ((char) (lenTag3 & 32640))) {
                        usePattern81 = false;
                    }
                } else {
                    useGsm72 = useGsm7;
                }
                if (usePattern81) {
                    ucs81Converts++;
                    if (-1 == currGsm7Length2) {
                        ucs81Len++;
                    } else {
                        ucs81Len += currGsm7Length2;
                    }
                }
            } else {
                useGsm72 = useGsm7;
            }
            if (usePattern82) {
                if (-1 == currGsm7Length2) {
                    if (setPattern82) {
                        if (baser82Low > lenTag3) {
                            baser82Low = lenTag3;
                        } else if (baser82High < lenTag3) {
                            baser82High = lenTag3;
                        }
                        if (baser82High - baser82Low > 127) {
                            usePattern82 = false;
                        }
                    } else {
                        setPattern82 = true;
                        baser82Low = lenTag3;
                        baser82High = lenTag3;
                    }
                }
                if (usePattern82) {
                    ucs82Converts++;
                    if (-1 == currGsm7Length2) {
                        ucs82Len++;
                    } else {
                        ucs82Len += currGsm7Length2;
                    }
                }
            }
            ucs80Len += 2;
            ucs80Converts++;
            index = index3 + 1;
            lenTag = lenTag2;
            useGsm7 = useGsm72;
            currGsm7Length = alphaTag;
        }
        index3 = index2;
        currGsm7Length2 = gsm7Converts;
        index2 = gsm7Len;
        if (currGsm7Length2 < ucs81Converts) {
            currGsm7Length2 = ucs81Converts;
            index2 = ucs81Len;
        }
        if (currGsm7Length2 < ucs82Converts) {
            currGsm7Length2 = ucs82Converts;
            index2 = ucs82Len;
        }
        if (currGsm7Length2 < ucs80Converts) {
            index2 = ucs80Len;
        }
        return index2;
    }

    public static boolean equalAdn(AdnRecord first, AdnRecord second) {
        return first.mEfid == second.mEfid && first.mRecordNumber == second.mRecordNumber;
    }

    public static boolean isContainZeros(byte[] data, int length, int totalLength, int curIndex) {
        int startIndex = totalLength + curIndex;
        int endIndex = data.length;
        int tempTotalLength = totalLength;
        if (totalLength >= length || startIndex > endIndex) {
            return false;
        }
        for (int valueIndex = startIndex; valueIndex < endIndex; valueIndex++) {
            if (data[valueIndex] == (byte) 0) {
                tempTotalLength++;
            }
        }
        if (tempTotalLength == length) {
            return true;
        }
        return false;
    }

    public static boolean arrayCompareNullEqualsEmpty(String[] s1, String[] s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            s1 = new String[]{""};
        }
        if (s2 == null) {
            s2 = new String[]{""};
        }
        for (String str : s1) {
            if (!TextUtils.isEmpty(str) && !Arrays.asList(s2).contains(str)) {
                return false;
            }
        }
        for (String str2 : s2) {
            if (!TextUtils.isEmpty(str2) && !Arrays.asList(s1).contains(str2)) {
                return false;
            }
        }
        return true;
    }

    public static String[] updateAnrEmailArrayHelper(String[] dest, String[] src, int fileCount) {
        if (fileCount == 0) {
            return null;
        }
        if (dest == null || src == null) {
            return dest;
        }
        int i;
        int j;
        String[] ref = new String[fileCount];
        for (i = 0; i < fileCount; i++) {
            ref[i] = "";
        }
        for (i = 0; i < src.length; i++) {
            if (!TextUtils.isEmpty(src[i])) {
                for (Object equals : dest) {
                    if (src[i].equals(equals)) {
                        ref[i] = src[i];
                        break;
                    }
                }
            }
        }
        for (i = 0; i < dest.length; i++) {
            if (!Arrays.asList(ref).contains(dest[i])) {
                for (j = 0; j < ref.length; j++) {
                    if (TextUtils.isEmpty(ref[j])) {
                        ref[j] = dest[i];
                        break;
                    }
                }
            }
        }
        return ref;
    }

    public static String cdmaDTMFToString(byte[] data, int offset, int length) {
        if (data == null) {
            return null;
        }
        if (data.length < (length + 1) / 2) {
            Rlog.w(LOG_TAG, "cdmaDTMFToString data.length < length");
            length = data.length * 2;
        }
        StringBuilder ret = new StringBuilder();
        if (11 == data.length && 1 == (data[9] & 1)) {
            ret.append('+');
        }
        int count = 0;
        int i = offset;
        while (count < length) {
            char c = intToCdmaDTMFChar(data[i] & 15);
            if ('-' != c) {
                ret.append(c);
            }
            count++;
            if (count == length) {
                break;
            }
            c = intToCdmaDTMFChar((data[i] >> 4) & 15);
            if ('-' != c) {
                ret.append(c);
            }
            count++;
            i++;
        }
        return ret.toString();
    }

    public static char intToCdmaDTMFChar(int c) {
        if (c >= 0 && c <= 9) {
            return (char) (c + 48);
        }
        if (c == 10) {
            return '0';
        }
        if (c == 11) {
            return '*';
        }
        if (c == 12) {
            return '#';
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("intToCdmaDTMFChar invalid char ");
        stringBuilder.append((char) (c + 48));
        Rlog.w(str, stringBuilder.toString());
        return '-';
    }

    public static int cdmaDTMFCharToint(char c) {
        if (c > '0' && c <= '9') {
            return c - 48;
        }
        if (c == '0') {
            return 10;
        }
        if (c == '*') {
            return 11;
        }
        if (c == '#') {
            return 12;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid char for BCD ");
        stringBuilder.append(c);
        throw new RuntimeException(stringBuilder.toString());
    }

    public static byte[] stringToCdmaDTMF(String number) {
        int numberLenReal = number.length();
        int numberLenEffective = numberLenReal;
        if (numberLenEffective == 0) {
            return new byte[0];
        }
        byte[] result = new byte[((numberLenEffective + 1) / 2)];
        int digitCount = 0;
        for (int i = 0; i < numberLenReal; i++) {
            int i2 = digitCount >> 1;
            result[i2] = (byte) (result[i2] | ((byte) ((cdmaDTMFCharToint(number.charAt(i)) & 15) << ((digitCount & 1) == 1 ? 4 : 0))));
            digitCount++;
        }
        if ((digitCount & 1) == 1) {
            int i3 = digitCount >> 1;
            result[i3] = (byte) (result[i3] | 240);
        }
        return result;
    }

    public static String prependPlusInLongAdnNumber(String Number) {
        if (Number == null || Number.length() == 0) {
            return Number;
        }
        int i = 0;
        if (!(Number.indexOf(43) != -1)) {
            return Number;
        }
        StringBuilder ret;
        String[] str = Number.split("\\+");
        StringBuilder ret2 = new StringBuilder();
        while (i < str.length) {
            ret2.append(str[i]);
            i++;
        }
        String retString = ret2.toString();
        Matcher m = Pattern.compile("(^[#*])(.*)([#*])(.*)([*]{2})(.*)(#)$").matcher(retString);
        if (!m.matches()) {
            m = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$").matcher(retString);
            if (!m.matches()) {
                m = Pattern.compile("(^[#*])(.*)([#*])(.*)").matcher(retString);
                if (m.matches()) {
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                } else {
                    StringBuilder ret3 = new StringBuilder();
                    ret3.append('+');
                    ret3.append(retString);
                    ret = ret3;
                }
            } else if ("".equals(m.group(2))) {
                ret = new StringBuilder();
                ret.append(m.group(1));
                ret.append(m.group(3));
                ret.append(m.group(4));
                ret.append(m.group(5));
                ret.append("+");
            } else {
                ret = new StringBuilder();
                ret.append(m.group(1));
                ret.append(m.group(2));
                ret.append(m.group(3));
                ret.append("+");
                ret.append(m.group(4));
                ret.append(m.group(5));
            }
        } else if ("".equals(m.group(2))) {
            ret = new StringBuilder();
            ret.append(m.group(1));
            ret.append(m.group(3));
            ret.append(m.group(4));
            ret.append(m.group(5));
            ret.append(m.group(6));
            ret.append(m.group(7));
            ret.append("+");
        } else {
            ret = new StringBuilder();
            ret.append(m.group(1));
            ret.append(m.group(2));
            ret.append(m.group(3));
            ret.append("+");
            ret.append(m.group(4));
            ret.append(m.group(5));
            ret.append(m.group(6));
            ret.append(m.group(7));
        }
        return ret.toString();
    }
}
