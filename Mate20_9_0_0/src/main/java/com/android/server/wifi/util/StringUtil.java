package com.android.server.wifi.util;

public class StringUtil {
    static final byte ASCII_PRINTABLE_MAX = (byte) 126;
    static final byte ASCII_PRINTABLE_MIN = (byte) 32;

    public static boolean isAsciiPrintable(byte[] byteArray) {
        if (byteArray == null) {
            return true;
        }
        for (byte b : byteArray) {
            if (b != (byte) 7) {
                switch (b) {
                    case (byte) 9:
                    case (byte) 10:
                    case (byte) 11:
                    case (byte) 12:
                        continue;
                    default:
                        if (b >= ASCII_PRINTABLE_MIN && b <= ASCII_PRINTABLE_MAX) {
                            break;
                        }
                        return false;
                        break;
                }
            }
        }
        return true;
    }

    public static String safeDisplayBssid(String srcBssid) {
        if (srcBssid == null) {
            return "null";
        }
        int len = srcBssid.length();
        if (len < 12) {
            return "Can not display bssid";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(srcBssid.substring(0, 6));
        stringBuilder.append("**:**");
        stringBuilder.append(srcBssid.substring(len - 6, len));
        return stringBuilder.toString();
    }
}
