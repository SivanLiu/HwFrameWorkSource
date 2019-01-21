package android.encrypt;

import huawei.android.provider.HwSettings.System;

public class HEX {
    public static String encode(byte[] btData, int iLen) {
        StringBuffer l_stHex = new StringBuffer();
        if (btData == null) {
            return null;
        }
        if (iLen <= 0 || iLen > btData.length) {
            iLen = btData.length;
        }
        for (int ii = 0; ii < iLen; ii++) {
            String l_stTmp = Integer.toHexString(btData[ii] & 255);
            if (l_stTmp.length() == 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(System.FINGERSENSE_KNUCKLE_GESTURE_OFF);
                stringBuilder.append(l_stTmp);
                l_stTmp = stringBuilder.toString();
            }
            l_stHex.append(l_stTmp.toUpperCase());
        }
        return l_stHex.toString();
    }

    public static byte[] decode(String stData) {
        if (stData == null) {
            return null;
        }
        int l_iLen = stData.length();
        if (l_iLen % 2 != 0) {
            return null;
        }
        int ii;
        String l_stData = stData.toUpperCase();
        for (ii = 0; ii < l_iLen; ii++) {
            char l_cTmp = l_stData.charAt(ii);
            if (('0' > l_cTmp || l_cTmp > '9') && ('A' > l_cTmp || l_cTmp > 'F')) {
                return null;
            }
        }
        l_iLen /= 2;
        byte[] l_btData = new byte[l_iLen];
        byte[] l_btTmp = new byte[2];
        int jj = 0;
        ii = 0;
        for (int ii2 = 0; ii2 < l_iLen; ii2++) {
            int jj2 = jj + 1;
            l_btTmp[0] = (byte) l_stData.charAt(jj);
            jj = jj2 + 1;
            l_btTmp[1] = (byte) l_stData.charAt(jj2);
            ii = 0;
            while (ii < 2) {
                if ((byte) 65 > l_btTmp[ii] || l_btTmp[ii] > (byte) 70) {
                    l_btTmp[ii] = (byte) (l_btTmp[ii] - 48);
                } else {
                    l_btTmp[ii] = (byte) (l_btTmp[ii] - 55);
                }
                ii++;
            }
            l_btData[ii2] = (byte) ((l_btTmp[0] << 4) | l_btTmp[1]);
        }
        return l_btData;
    }

    private HEX() {
    }
}
