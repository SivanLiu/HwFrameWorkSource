package com.android.internal.telephony.uicc;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.telephony.Rlog;
import com.android.internal.colorextraction.types.Tonal;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.midi.MidiConstants;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.util.AsyncService;
import java.io.UnsupportedEncodingException;

public class IccUtils {
    private static final char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static final String LOG_TAG = "IccUtils";
    private static final int MDN_NUMBER_TYPE_INDEX = 9;

    public static String bcdToString(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length; i++) {
            int v = data[i] & 15;
            if (v > 9) {
                break;
            }
            ret.append((char) (48 + v));
            v = (data[i] >> 4) & 15;
            if (v != 15) {
                if (v > 9) {
                    break;
                }
                ret.append((char) (48 + v));
            }
        }
        return ret.toString();
    }

    public static String bcdToString(byte[] data) {
        return bcdToString(data, 0, data.length);
    }

    public static byte[] bcdToBytes(String bcd) {
        byte[] output = new byte[((bcd.length() + 1) / 2)];
        bcdToBytes(bcd, output);
        return output;
    }

    public static void bcdToBytes(String bcd, byte[] bytes) {
        if (bcd.length() % 2 != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(bcd);
            stringBuilder.append("0");
            bcd = stringBuilder.toString();
        }
        int size = Math.min(bytes.length * 2, bcd.length());
        int i = 0;
        int j = 0;
        while (i + 1 < size) {
            bytes[j] = (byte) ((charToByte(bcd.charAt(i + 1)) << 4) | charToByte(bcd.charAt(i)));
            i += 2;
            j++;
        }
    }

    public static String bcdPlmnToString(byte[] data, int offset) {
        if (offset + 3 > data.length) {
            return null;
        }
        byte[] trans = new byte[3];
        trans[0] = (byte) ((data[0 + offset] << 4) | ((data[0 + offset] >> 4) & 15));
        trans[1] = (byte) ((data[1 + offset] << 4) | (data[2 + offset] & 15));
        trans[2] = (byte) (((data[1 + offset] >> 4) & 15) | (data[2 + offset] & MetricsEvent.FINGERPRINT_ENROLLING));
        String ret = bytesToHexString(trans);
        if (ret.contains("f") || ret.contains("F")) {
            ret = ret.replaceAll("(?i)f", "");
        }
        return ret;
    }

    public static String bchToString(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length; i++) {
            ret.append(HEX_CHARS[data[i] & 15]);
            ret.append(HEX_CHARS[(data[i] >> 4) & 15]);
        }
        return ret.toString();
    }

    public static String cdmaBcdToString(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder(length);
        int count = 0;
        int i = offset;
        while (count < length) {
            int v = data[i] & 15;
            if (v > 9) {
                v = 0;
            }
            ret.append((char) (48 + v));
            count++;
            if (count == length) {
                break;
            }
            v = (data[i] >> 4) & 15;
            if (v > 9) {
                v = 0;
            }
            ret.append((char) (48 + v));
            count++;
            i++;
        }
        return ret.toString();
    }

    public static String cdmaBcdToStringHw(byte[] data, int offset, int length) {
        StringBuilder ret = new StringBuilder();
        boolean prependPlus = false;
        if (length > 0 && data.length > 9) {
            prependPlus = data[9] == (byte) 9;
        }
        if (prependPlus) {
            ret.append('+');
        }
        int count = 0;
        int i = offset;
        while (count < length) {
            int v = data[i] & 15;
            if (v == 11) {
                ret.append('*');
            } else if (v == 12) {
                ret.append('#');
            } else {
                if (v > 9) {
                    v = 0;
                }
                ret.append((char) (48 + v));
            }
            count++;
            if (count == length) {
                break;
            }
            v = (data[i] >> 4) & 15;
            if (v == 11) {
                ret.append('*');
            } else if (v == 12) {
                ret.append('#');
            } else {
                if (v > 9) {
                    v = 0;
                }
                ret.append((char) (48 + v));
            }
            count++;
            i++;
        }
        return ret.toString();
    }

    public static int gsmBcdByteToInt(byte b) {
        int ret = 0;
        if ((b & MetricsEvent.FINGERPRINT_ENROLLING) <= 144) {
            ret = (b >> 4) & 15;
        }
        if ((b & 15) <= 9) {
            return ret + ((b & 15) * 10);
        }
        return ret;
    }

    public static int cdmaBcdByteToInt(byte b) {
        int ret = 0;
        if ((b & MetricsEvent.FINGERPRINT_ENROLLING) <= 144) {
            ret = ((b >> 4) & 15) * 10;
        }
        if ((b & 15) <= 9) {
            return ret + (b & 15);
        }
        return ret;
    }

    public static String adnStringFieldToString(byte[] data, int offset, int length) {
        if (length == 0) {
            return "";
        }
        if (length >= 1 && data[offset] == MidiConstants.STATUS_NOTE_OFF) {
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
            len = data[offset + 1] & 255;
            if (len > length - 3) {
                len = length - 3;
            }
            base = (char) ((data[offset + 2] & 255) << 7);
            offset += 3;
            isucs2 = true;
        } else if (length >= 4 && data[offset] == (byte) -126) {
            len = data[offset + 1] & 255;
            if (len > length - 4) {
                len = length - 4;
            }
            base = (char) (((data[offset + 2] & 255) << 8) | (data[offset + 3] & 255));
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
                ret2.append(GsmAlphabet.gsm8BitUnpackedToString(data, offset, count));
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
        return GsmAlphabet.gsm8BitUnpackedToString(data, offset, length, defaultCharset.trim());
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

    public static byte[] hexStringToBytes(String s) {
        if (s == null) {
            return null;
        }
        int sz = s.length();
        byte[] ret = new byte[(sz / 2)];
        for (int i = 0; i < sz; i += 2) {
            ret[i / 2] = (byte) ((hexCharToInt(s.charAt(i)) << 4) | hexCharToInt(s.charAt(i + 1)));
        }
        return ret;
    }

    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder ret = new StringBuilder(2 * bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            ret.append(HEX_CHARS[(bytes[i] >> 4) & 15]);
            ret.append(HEX_CHARS[15 & bytes[i]]);
        }
        return ret.toString();
    }

    public static String networkNameToString(byte[] data, int offset, int length) {
        if (data.length <= offset || (data[offset] & 128) != 128 || length < 1) {
            return "";
        }
        String ret;
        switch ((data[offset] >>> 4) & 7) {
            case 0:
                String ret2 = offset + 1;
                ret = GsmAlphabet.gsm7BitPackedToString(data, ret2, (((length - 1) * 8) - (data[offset] & 7)) / 7);
                break;
            case 1:
                try {
                    ret = new String(data, offset + 1, length - 1, "utf-16");
                    break;
                } catch (UnsupportedEncodingException ex) {
                    Rlog.e(LOG_TAG, "implausible UnsupportedEncodingException", ex);
                    ret = "";
                    break;
                }
            default:
                ret = "";
                break;
        }
        int countSeptets = data[offset];
        return ret;
    }

    public static Bitmap parseToBnW(byte[] data, int length) {
        int valueIndex = 0 + 1;
        int width = data[0] & 255;
        byte currentByte = valueIndex + 1;
        valueIndex = data[valueIndex] & 255;
        int numOfPixels = width * valueIndex;
        int[] pixels = new int[numOfPixels];
        int pixelIndex = 0;
        int bitIndex = 7;
        byte currentByte2 = (byte) 0;
        while (pixelIndex < numOfPixels) {
            if (pixelIndex % 8 == 0) {
                byte valueIndex2 = currentByte + 1;
                bitIndex = 7;
                currentByte2 = data[currentByte];
                currentByte = valueIndex2;
            }
            int pixelIndex2 = pixelIndex + 1;
            int bitIndex2 = bitIndex - 1;
            pixels[pixelIndex] = bitToRGB((currentByte2 >> bitIndex) & 1);
            pixelIndex = pixelIndex2;
            bitIndex = bitIndex2;
        }
        if (pixelIndex != numOfPixels) {
            Rlog.e(LOG_TAG, "parse end and size error");
        }
        return Bitmap.createBitmap(pixels, width, valueIndex, Config.ARGB_8888);
    }

    private static int bitToRGB(int bit) {
        if (bit == 1) {
            return -1;
        }
        return Tonal.MAIN_COLOR_DARK;
    }

    public static Bitmap parseToRGB(byte[] data, int length, boolean transparency) {
        int[] resultArray;
        int valueIndex = 0 + 1;
        int width = data[0] & 255;
        int valueIndex2 = valueIndex + 1;
        valueIndex = data[valueIndex] & 255;
        int valueIndex3 = valueIndex2 + 1;
        valueIndex2 = data[valueIndex2] & 255;
        int valueIndex4 = valueIndex3 + 1;
        valueIndex3 = data[valueIndex3] & 255;
        int valueIndex5 = valueIndex4 + 1;
        int valueIndex6 = valueIndex5 + 1;
        int[] colorIndexArray = getCLUT(data, ((data[valueIndex4] & 255) << 8) | (data[valueIndex5] & 255), valueIndex3);
        if (true == transparency) {
            colorIndexArray[valueIndex3 - 1] = 0;
        }
        if (8 % valueIndex2 == 0) {
            resultArray = mapTo2OrderBitColor(data, valueIndex6, width * valueIndex, colorIndexArray, valueIndex2);
        } else {
            resultArray = mapToNon2OrderBitColor(data, valueIndex6, width * valueIndex, colorIndexArray, valueIndex2);
        }
        return Bitmap.createBitmap(resultArray, width, valueIndex, Config.RGB_565);
    }

    private static int[] mapTo2OrderBitColor(byte[] data, int tempByte, int length, int[] colorArray, int bits) {
        if (8 % bits != 0) {
            Rlog.e(LOG_TAG, "not event number of color");
            return mapToNon2OrderBitColor(data, tempByte, length, colorArray, bits);
        }
        int mask = 1;
        if (bits == 4) {
            mask = 15;
        } else if (bits != 8) {
            switch (bits) {
                case 1:
                    mask = 1;
                    break;
                case 2:
                    mask = 3;
                    break;
            }
        } else {
            mask = 255;
        }
        int[] resultArray = new int[length];
        int resultIndex = 0;
        int run = 8 / bits;
        while (resultIndex < length) {
            byte valueIndex = tempByte + 1;
            byte tempByte2 = data[tempByte2];
            int runIndex = 0;
            while (runIndex < run) {
                int resultIndex2 = resultIndex + 1;
                resultArray[resultIndex] = colorArray[(tempByte2 >> (((run - runIndex) - 1) * bits)) & mask];
                runIndex++;
                resultIndex = resultIndex2;
            }
            tempByte2 = valueIndex;
        }
        return resultArray;
    }

    private static int[] mapToNon2OrderBitColor(byte[] data, int valueIndex, int length, int[] colorArray, int bits) {
        if (8 % bits != 0) {
            return new int[length];
        }
        Rlog.e(LOG_TAG, "not odd number of color");
        return mapTo2OrderBitColor(data, valueIndex, length, colorArray, bits);
    }

    private static int[] getCLUT(byte[] rawData, int offset, int number) {
        if (rawData == null) {
            return null;
        }
        int[] result = new int[number];
        int endIndex = (number * 3) + offset;
        int valueIndex = offset;
        int colorIndex = 0;
        while (true) {
            int colorIndex2 = colorIndex + 1;
            int valueIndex2 = valueIndex + 1;
            int valueIndex3 = valueIndex2 + 1;
            valueIndex = (((rawData[valueIndex] & 255) << 16) | Tonal.MAIN_COLOR_DARK) | ((rawData[valueIndex2] & 255) << 8);
            valueIndex2 = valueIndex3 + 1;
            result[colorIndex] = valueIndex | (rawData[valueIndex3] & 255);
            if (valueIndex2 >= endIndex) {
                return result;
            }
            colorIndex = colorIndex2;
            valueIndex = valueIndex2;
        }
    }

    public static String getDecimalSubstring(String iccId) {
        if (iccId == null) {
            return null;
        }
        int position = 0;
        while (position < iccId.length() && Character.isDigit(iccId.charAt(position))) {
            position++;
        }
        return iccId.substring(0, position);
    }

    public static int bytesToInt(byte[] src, int offset, int length) {
        StringBuilder stringBuilder;
        if (length > 4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("length must be <= 4 (only 32-bit integer supported): ");
            stringBuilder.append(length);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (offset < 0 || length < 0 || offset + length > src.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Out of the bounds: src=[");
            stringBuilder.append(src.length);
            stringBuilder.append("], offset=");
            stringBuilder.append(offset);
            stringBuilder.append(", length=");
            stringBuilder.append(length);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else {
            int result = 0;
            for (int i = 0; i < length; i++) {
                result = (result << 8) | (src[offset + i] & 255);
            }
            if (result >= 0) {
                return result;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("src cannot be parsed as a positive integer: ");
            stringBuilder2.append(result);
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    public static long bytesToRawLong(byte[] src, int offset, int length) {
        StringBuilder stringBuilder;
        if (length > 8) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("length must be <= 8 (only 64-bit long supported): ");
            stringBuilder.append(length);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (offset < 0 || length < 0 || offset + length > src.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Out of the bounds: src=[");
            stringBuilder.append(src.length);
            stringBuilder.append("], offset=");
            stringBuilder.append(offset);
            stringBuilder.append(", length=");
            stringBuilder.append(length);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else {
            long result = 0;
            for (int i = 0; i < length; i++) {
                result = (result << 8) | ((long) (src[offset + i] & 255));
            }
            return result;
        }
    }

    public static byte[] unsignedIntToBytes(int value) {
        if (value >= 0) {
            byte[] bytes = new byte[byteNumForUnsignedInt(value)];
            unsignedIntToBytes(value, bytes, 0);
            return bytes;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("value must be 0 or positive: ");
        stringBuilder.append(value);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static byte[] signedIntToBytes(int value) {
        if (value >= 0) {
            byte[] bytes = new byte[byteNumForSignedInt(value)];
            signedIntToBytes(value, bytes, 0);
            return bytes;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("value must be 0 or positive: ");
        stringBuilder.append(value);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static int unsignedIntToBytes(int value, byte[] dest, int offset) {
        return intToBytes(value, dest, offset, false);
    }

    public static int signedIntToBytes(int value, byte[] dest, int offset) {
        return intToBytes(value, dest, offset, true);
    }

    public static int byteNumForUnsignedInt(int value) {
        return byteNumForInt(value, false);
    }

    public static int byteNumForSignedInt(int value) {
        return byteNumForInt(value, true);
    }

    private static int intToBytes(int value, byte[] dest, int offset, boolean signed) {
        int l = byteNumForInt(value, signed);
        if (offset < 0 || offset + l > dest.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not enough space to write. Required bytes: ");
            stringBuilder.append(l);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
        int i = l - 1;
        int v = value;
        while (i >= 0) {
            dest[offset + i] = (byte) (v & 255);
            i--;
            v >>>= 8;
        }
        return l;
    }

    private static int byteNumForInt(int value, boolean signed) {
        if (value >= 0) {
            if (signed) {
                if (value <= 127) {
                    return 1;
                }
                if (value <= 32767) {
                    return 2;
                }
                if (value <= 8388607) {
                    return 3;
                }
            } else if (value <= 255) {
                return 1;
            } else {
                if (value <= 65535) {
                    return 2;
                }
                if (value <= AsyncService.CMD_ASYNC_SERVICE_ON_START_INTENT) {
                    return 3;
                }
            }
            return 4;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("value must be 0 or positive: ");
        stringBuilder.append(value);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static byte countTrailingZeros(byte b) {
        if (b == (byte) 0) {
            return (byte) 8;
        }
        int v = b & 255;
        byte c = (byte) 7;
        if ((v & 15) != 0) {
            c = (byte) (7 - 4);
        }
        if ((v & 51) != 0) {
            c = (byte) (c - 2);
        }
        if ((v & 85) != 0) {
            c = (byte) (c - 1);
        }
        return c;
    }

    public static String byteToHex(byte b) {
        return new String(new char[]{HEX_CHARS[(b & 255) >>> 4], HEX_CHARS[b & 15]});
    }

    public static String stripTrailingFs(String s) {
        return s == null ? null : s.replaceAll("(?i)f*$", "");
    }

    private static byte charToByte(char c) {
        if (c >= '0' && c <= '9') {
            return (byte) (c - 48);
        }
        if (c >= 'A' && c <= 'F') {
            return (byte) (c - 55);
        }
        if (c < 'a' || c > 'f') {
            return (byte) 0;
        }
        return (byte) (c - 87);
    }

    public static String iccIdBcdToString(byte[] data, int offset, int length) {
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
}
