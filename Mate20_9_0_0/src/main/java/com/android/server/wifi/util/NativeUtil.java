package com.android.server.wifi.util;

import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import libcore.util.HexEncoding;

public class NativeUtil {
    public static final byte[] ANY_MAC_BYTES = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private static final String ANY_MAC_STR = "any";
    private static final int MAC_LENGTH = 6;
    private static final int MAC_OUI_LENGTH = 3;
    private static final int MAC_STR_LENGTH = 17;
    private static final String TAG = "NativeUtil";

    public static ArrayList<Byte> stringToByteArrayList(String str) {
        if (str != null) {
            try {
                ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(str));
                byte[] byteArray = new byte[encoded.remaining()];
                encoded.get(byteArray);
                return byteArrayToArrayList(byteArray);
            } catch (CharacterCodingException cce) {
                throw new IllegalArgumentException("cannot be utf-8 encoded", cce);
            }
        }
        throw new IllegalArgumentException("null string");
    }

    public static String stringFromByteArrayList(ArrayList<Byte> byteArrayList) {
        if (byteArrayList != null) {
            byte[] byteArray = new byte[byteArrayList.size()];
            int i = 0;
            Iterator it = byteArrayList.iterator();
            while (it.hasNext()) {
                byteArray[i] = ((Byte) it.next()).byteValue();
                i++;
            }
            return new String(byteArray, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("null byte array list");
    }

    public static byte[] stringToByteArray(String str) {
        if (str != null) {
            return str.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("null string");
    }

    public static String stringFromByteArray(byte[] byteArray) {
        if (byteArray != null) {
            return new String(byteArray);
        }
        throw new IllegalArgumentException("null byte array");
    }

    public static byte[] macAddressToByteArray(String macStr) {
        if (TextUtils.isEmpty(macStr) || "any".equals(macStr)) {
            return ANY_MAC_BYTES;
        }
        String cleanMac = macStr.replace(":", "");
        if (cleanMac.length() == 12) {
            return HexEncoding.decode(cleanMac.toCharArray(), false);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid mac string length: ");
        stringBuilder.append(cleanMac);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static String macAddressFromByteArray(byte[] macArray) {
        if (macArray == null) {
            throw new IllegalArgumentException("null mac bytes");
        } else if (macArray.length == 6) {
            StringBuilder sb = new StringBuilder(17);
            for (int i = 0; i < macArray.length; i++) {
                if (i != 0) {
                    sb.append(":");
                }
                sb.append(new String(HexEncoding.encode(macArray, i, 1)));
            }
            return sb.toString().toLowerCase();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid macArray length: ");
            stringBuilder.append(macArray.length);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static byte[] macAddressOuiToByteArray(String macStr) {
        if (macStr != null) {
            String cleanMac = macStr.replace(":", "");
            if (cleanMac.length() == 6) {
                return HexEncoding.decode(cleanMac.toCharArray(), false);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid mac oui string length: ");
            stringBuilder.append(cleanMac);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        throw new IllegalArgumentException("null mac string");
    }

    /* JADX WARNING: Removed duplicated region for block: B:6:0x0017 A:{Splitter: B:3:0x0007, ExcHandler: java.nio.BufferUnderflowException (e java.nio.BufferUnderflowException)} */
    /* JADX WARNING: Missing block: B:8:0x001f, code:
            throw new java.lang.IllegalArgumentException("invalid macArray");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static Long macAddressToLong(byte[] macArray) {
        if (macArray == null) {
            throw new IllegalArgumentException("null mac bytes");
        } else if (macArray.length == 6) {
            try {
                return Long.valueOf(ByteBufferReader.readInteger(ByteBuffer.wrap(macArray), ByteOrder.BIG_ENDIAN, macArray.length));
            } catch (BufferUnderflowException e) {
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid macArray length: ");
            stringBuilder.append(macArray.length);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static String removeEnclosingQuotes(String quotedStr) {
        int length = quotedStr.length();
        if (length >= 2 && quotedStr.charAt(0) == '\"' && quotedStr.charAt(length - 1) == '\"') {
            return quotedStr.substring(1, length - 1);
        }
        return quotedStr;
    }

    public static String addEnclosingQuotes(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"");
        stringBuilder.append(str);
        stringBuilder.append("\"");
        return stringBuilder.toString();
    }

    public static ArrayList<Byte> hexOrQuotedStringToBytes(String str) {
        if (str != null) {
            int length = str.length();
            if (length > 1 && str.charAt(0) == '\"' && str.charAt(length - 1) == '\"') {
                return stringToByteArrayList(str.substring(1, str.length() - 1));
            }
            return byteArrayToArrayList(hexStringToByteArray(str));
        }
        throw new IllegalArgumentException("null string");
    }

    public static String bytesToHexOrQuotedString(ArrayList<Byte> bytes) {
        if (bytes != null) {
            byte[] byteArray = byteArrayFromArrayList(bytes);
            if (!bytes.contains(Byte.valueOf((byte) 0))) {
                try {
                    CharBuffer decoded = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(byteArray));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("\"");
                    stringBuilder.append(decoded.toString());
                    stringBuilder.append("\"");
                    return stringBuilder.toString();
                } catch (CharacterCodingException e) {
                }
            }
            return hexStringFromByteArray(byteArray);
        }
        throw new IllegalArgumentException("null ssid bytes");
    }

    public static String quotedAsciiStringToHex(String ascii, String charsetName) {
        if (TextUtils.isEmpty(ascii)) {
            Log.d(TAG, "quotedAsciiStringToHex: Invalid param.");
            return null;
        }
        try {
            return hexStringFromByteArray(removeEnclosingQuotes(ascii).getBytes(charsetName));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "quotedAsciiStringToHex: Unsupported encoding exception.");
            return null;
        }
    }

    public static ArrayList<Byte> decodeSsid(String ssidStr) {
        return hexOrQuotedStringToBytes(ssidStr);
    }

    public static String encodeSsid(ArrayList<Byte> ssidBytes) {
        return bytesToHexOrQuotedString(ssidBytes);
    }

    public static ArrayList<Byte> byteArrayToArrayList(byte[] bytes) {
        ArrayList<Byte> byteList = new ArrayList();
        for (Byte b : bytes) {
            byteList.add(Byte.valueOf(b));
        }
        return byteList;
    }

    public static byte[] byteArrayFromArrayList(ArrayList<Byte> bytes) {
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        Iterator it = bytes.iterator();
        while (it.hasNext()) {
            int i2 = i + 1;
            byteArray[i] = ((Byte) it.next()).byteValue();
            i = i2;
        }
        return byteArray;
    }

    public static byte[] hexStringToByteArray(String hexStr) {
        if (hexStr != null) {
            return HexEncoding.decode(hexStr.toCharArray(), false);
        }
        throw new IllegalArgumentException("null hex string");
    }

    public static String hexStringFromByteArray(byte[] bytes) {
        if (bytes != null) {
            return new String(HexEncoding.encode(bytes)).toLowerCase();
        }
        throw new IllegalArgumentException("null hex bytes");
    }

    public static String wpsDevTypeStringFromByteArray(byte[] devType) {
        byte[] a = devType;
        int x = ((a[0] & Constants.BYTE_MASK) << 8) | (a[1] & Constants.BYTE_MASK);
        String y = new String(HexEncoding.encode(Arrays.copyOfRange(devType, 2, 6)));
        int z = ((a[6] & Constants.BYTE_MASK) << 8) | (a[7] & Constants.BYTE_MASK);
        return String.format("%d-%s-%d", new Object[]{Integer.valueOf(x), y, Integer.valueOf(z)});
    }
}
