package com.huawei.zxing.common;

import android.telephony.HwCarrierConfigManager;
import com.huawei.android.app.AppOpsManagerEx;
import com.huawei.android.hishow.AlarmInfoEx;
import com.huawei.internal.telephony.SmsConstantsEx;
import com.huawei.zxing.DecodeHintType;
import java.util.Map;

public final class StringUtils {
    private static final boolean ASSUME_SHIFT_JIS;
    private static final String EUC_JP = "EUC_JP";
    public static final String GB2312 = "GB2312";
    private static final String ISO88591 = "ISO8859_1";
    private static final String PLATFORM_DEFAULT_ENCODING = System.getProperty("file.encoding");
    public static final String SHIFT_JIS = "SJIS";
    private static final String UTF8 = "UTF8";

    static {
        boolean z = SHIFT_JIS.equalsIgnoreCase(PLATFORM_DEFAULT_ENCODING) || EUC_JP.equalsIgnoreCase(PLATFORM_DEFAULT_ENCODING);
        ASSUME_SHIFT_JIS = z;
    }

    private StringUtils() {
    }

    private static final boolean canBeGB2312(byte[] rawtext) {
        int rawtextlen = rawtext.length;
        int dbchars = 1;
        int gbchars = 1;
        int i = 0;
        while (i < rawtextlen - 1) {
            if (rawtext[i] < (byte) 0) {
                dbchars++;
                int value = rawtext[i] + 256;
                int next_value = rawtext[i + 1] + 256;
                if (value >= 161 && value <= 247 && next_value >= 161 && next_value <= 254) {
                    gbchars++;
                }
                i++;
            }
            i++;
        }
        return gbchars > 1 && gbchars * 100 > dbchars * 80;
    }

    public static String guessEncoding(byte[] bytes, Map<DecodeHintType, ?> hints) {
        int length;
        byte[] bArr = bytes;
        Map<DecodeHintType, ?> map = hints;
        if (map != null) {
            String characterSet = (String) map.get(DecodeHintType.CHARACTER_SET);
            if (characterSet != null) {
                return characterSet;
            }
        }
        int length2 = bArr.length;
        boolean canBeShiftJIS = true;
        boolean canBeUTF8 = true;
        int utf8BytesLeft = 0;
        int utf2BytesChars = 0;
        int utf3BytesChars = 0;
        int utf4BytesChars = 0;
        int sjisBytesLeft = 0;
        int sjisKatakanaChars = 0;
        int sjisCurKatakanaWordLength = 0;
        int sjisCurDoubleBytesWordLength = 0;
        int sjisMaxKatakanaWordLength = 0;
        int sjisMaxDoubleBytesWordLength = 0;
        int isoHighOther = 0;
        boolean canBeISO88591 = true;
        int i = 0;
        boolean z = true;
        if (!(bArr.length > 3 && bArr[0] == (byte) -17 && bArr[1] == (byte) -69 && bArr[2] == (byte) -65)) {
            z = false;
        }
        boolean utf8bom = z;
        while (true) {
            int i2 = i;
            if (i2 < length2) {
                if (!canBeISO88591 && !canBeShiftJIS && !canBeUTF8) {
                    length = length2;
                    break;
                }
                boolean canBeUTF82;
                length = length2;
                length2 = bArr[i2] & 255;
                if (canBeUTF8) {
                    if (utf8BytesLeft > 0) {
                        if ((length2 & AppOpsManagerEx.TYPE_MICROPHONE) == 0) {
                            canBeUTF82 = false;
                        } else {
                            utf8BytesLeft--;
                        }
                    } else if ((length2 & AppOpsManagerEx.TYPE_MICROPHONE) != 0) {
                        if ((length2 & 64) == 0) {
                            canBeUTF82 = false;
                        } else {
                            utf8BytesLeft++;
                            if ((length2 & 32) == 0) {
                                utf2BytesChars++;
                            } else {
                                utf8BytesLeft++;
                                if ((length2 & 16) == 0) {
                                    utf3BytesChars++;
                                } else {
                                    utf8BytesLeft++;
                                    if ((length2 & 8) == 0) {
                                        utf4BytesChars++;
                                    } else {
                                        canBeUTF82 = false;
                                    }
                                }
                            }
                        }
                    }
                    canBeUTF8 = canBeUTF82;
                }
                if (canBeISO88591) {
                    if (length2 > AlarmInfoEx.EVERYDAY_CODE && length2 < SmsConstantsEx.MAX_USER_DATA_SEPTETS) {
                        canBeISO88591 = false;
                    } else if (length2 > 159 && (length2 < HwCarrierConfigManager.HD_ICON_MASK_DIALER || length2 == 215 || length2 == 247)) {
                        isoHighOther++;
                    }
                }
                if (canBeShiftJIS) {
                    if (sjisBytesLeft > 0) {
                        if (length2 < 64 || length2 == AlarmInfoEx.EVERYDAY_CODE || length2 > 252) {
                            canBeUTF82 = false;
                        } else {
                            sjisBytesLeft--;
                        }
                    } else if (length2 == AppOpsManagerEx.TYPE_MICROPHONE || length2 == SmsConstantsEx.MAX_USER_DATA_SEPTETS || length2 > 239) {
                        canBeUTF82 = false;
                    } else if (length2 > SmsConstantsEx.MAX_USER_DATA_SEPTETS && length2 < 224) {
                        sjisKatakanaChars++;
                        sjisCurKatakanaWordLength++;
                        if (sjisCurKatakanaWordLength > sjisMaxKatakanaWordLength) {
                            sjisMaxKatakanaWordLength = sjisCurKatakanaWordLength;
                        }
                        sjisCurDoubleBytesWordLength = 0;
                    } else if (length2 > AlarmInfoEx.EVERYDAY_CODE) {
                        sjisBytesLeft++;
                        sjisCurDoubleBytesWordLength++;
                        if (sjisCurDoubleBytesWordLength > sjisMaxDoubleBytesWordLength) {
                            sjisMaxDoubleBytesWordLength = sjisCurDoubleBytesWordLength;
                        }
                        sjisCurKatakanaWordLength = 0;
                    } else {
                        sjisCurKatakanaWordLength = 0;
                        sjisCurDoubleBytesWordLength = 0;
                    }
                    canBeShiftJIS = canBeUTF82;
                }
                i = i2 + 1;
                length2 = length;
                bArr = bytes;
            } else {
                length = length2;
                break;
            }
        }
        if (canBeUTF8 && utf8BytesLeft > 0) {
            canBeUTF8 = false;
        }
        if (canBeShiftJIS && sjisBytesLeft > 0) {
            canBeShiftJIS = false;
        }
        if (canBeUTF8 && (utf8bom || (utf2BytesChars + utf3BytesChars) + utf4BytesChars > 0)) {
            return UTF8;
        }
        if (canBeGB2312(bytes)) {
            return GB2312;
        }
        if (canBeShiftJIS && (ASSUME_SHIFT_JIS || sjisMaxKatakanaWordLength >= 3 || sjisMaxDoubleBytesWordLength >= 3)) {
            return SHIFT_JIS;
        }
        if (canBeISO88591 && canBeShiftJIS) {
            String str;
            if (sjisMaxKatakanaWordLength == 2 && sjisKatakanaChars == 2) {
                length2 = length;
            } else if (isoHighOther * 10 < length) {
                str = ISO88591;
                return str;
            }
            str = SHIFT_JIS;
            return str;
        }
        if (canBeUTF8) {
            return UTF8;
        }
        if (canBeISO88591) {
            return ISO88591;
        }
        if (canBeShiftJIS) {
            return SHIFT_JIS;
        }
        return PLATFORM_DEFAULT_ENCODING;
    }
}
