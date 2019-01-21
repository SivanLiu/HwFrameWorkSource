package sun.net.util;

public class IPAddressUtil {
    private static final int INADDR16SZ = 16;
    private static final int INADDR4SZ = 4;
    private static final int INT16SZ = 2;

    public static byte[] textToNumericFormatV4(String src) {
        byte[] res = new byte[4];
        boolean newOctet = true;
        int len = src.length();
        String str;
        if (len == 0 || len > 15) {
            str = src;
            return null;
        }
        int currByte = 0;
        long tmpValue = 0;
        for (long tmpValue2 = null; tmpValue2 < len; tmpValue2++) {
            char c = src.charAt(tmpValue2);
            int digit;
            if (c != '.') {
                digit = Character.digit(c, 10);
                if (digit < 0) {
                    return null;
                }
                tmpValue = (tmpValue * 10) + ((long) digit);
                newOctet = false;
            } else if (newOctet || tmpValue < 0 || tmpValue > 255 || currByte == 3) {
                return null;
            } else {
                digit = currByte + 1;
                res[currByte] = (byte) ((int) (tmpValue & 255));
                tmpValue = 0;
                newOctet = true;
                currByte = digit;
            }
        }
        str = src;
        if (newOctet || tmpValue < 0 || tmpValue >= (1 << ((4 - currByte) * 8))) {
            return null;
        }
        switch (currByte) {
            case 0:
            case 1:
            case 2:
                return null;
            case 3:
                res[3] = (byte) ((int) ((tmpValue >> null) & 255));
                break;
        }
        return res;
    }

    /* JADX WARNING: Removed duplicated region for block: B:60:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00f3  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x011b  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0119  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] textToNumericFormatV6(String src) {
        String str = src;
        if (src.length() < 2) {
            return null;
        }
        char[] srcb = src.toCharArray();
        int i = 16;
        byte[] dst = new byte[16];
        int srcb_length = srcb.length;
        int pc = str.indexOf("%");
        if (pc == srcb_length - 1) {
            return null;
        }
        int i2;
        byte[] bArr;
        int i3 = -1;
        if (pc != -1) {
            srcb_length = pc;
        }
        int i4 = 0;
        if (srcb[0] == ':') {
            i4 = 0 + 1;
            if (srcb[i4] != ':') {
                return null;
            }
        }
        boolean saw_xdigit = false;
        int curtok = i4;
        int j = 0;
        int colonp = -1;
        int val = 0;
        while (i4 < srcb_length) {
            i2 = i4 + 1;
            char i5 = srcb[i4];
            int chval = Character.digit(i5, i);
            if (chval != i3) {
                val = (val << 4) | chval;
                if (val > 65535) {
                    return null;
                }
                saw_xdigit = true;
                i4 = i2;
                i = 16;
            } else if (i5 == ':') {
                curtok = i2;
                if (saw_xdigit) {
                    if (i2 == srcb_length || j + 2 > 16) {
                        return null;
                    }
                    i = j + 1;
                    dst[j] = (byte) ((val >> 8) & 255);
                    j = i + 1;
                    dst[i] = (byte) (val & 255);
                    saw_xdigit = false;
                    val = 0;
                    i4 = i2;
                    i = 16;
                    i3 = -1;
                } else if (colonp != i3) {
                    return null;
                } else {
                    colonp = j;
                    chval = 58;
                    i4 = i2;
                    i = 16;
                }
            } else {
                i = 46;
                if (i5 != '.' || j + 4 > 16) {
                    return null;
                }
                int indexOf;
                String ia4 = str.substring(curtok, srcb_length);
                int dot_count = 0;
                i3 = 0;
                while (true) {
                    indexOf = ia4.indexOf(i, i3);
                    i3 = indexOf;
                    if (indexOf == -1) {
                        break;
                    }
                    dot_count++;
                    i3++;
                    str = src;
                    i = 46;
                }
                indexOf = dot_count;
                if (indexOf != 3) {
                    return null;
                }
                byte[] v4addr = textToNumericFormatV4(ia4);
                if (v4addr == null) {
                    return null;
                }
                i = 0;
                while (true) {
                    int dot_count2 = indexOf;
                    if (i >= 4) {
                        break;
                    }
                    indexOf = j + 1;
                    dst[j] = v4addr[i];
                    i++;
                    j = indexOf;
                    indexOf = dot_count2;
                }
                saw_xdigit = false;
                bArr = null;
                if (saw_xdigit) {
                    if (j + 2 > 16) {
                        return bArr;
                    }
                    indexOf = j + 1;
                    dst[j] = (byte) ((val >> 8) & 255);
                    j = indexOf + 1;
                    dst[indexOf] = (byte) (val & 255);
                }
                if (colonp != -1) {
                    indexOf = j - colonp;
                    if (j == 16) {
                        return null;
                    }
                    for (i2 = 1; i2 <= indexOf; i2++) {
                        dst[16 - i2] = dst[(colonp + indexOf) - i2];
                        dst[(colonp + indexOf) - i2] = (byte) 0;
                    }
                    j = 16;
                }
                if (j == 16) {
                    return null;
                }
                bArr = convertFromIPv4MappedAddress(dst);
                if (bArr != null) {
                    return bArr;
                }
                return dst;
            }
        }
        bArr = null;
        i2 = i4;
        if (saw_xdigit) {
        }
        if (colonp != -1) {
        }
        if (j == 16) {
        }
    }

    public static boolean isIPv4LiteralAddress(String src) {
        return textToNumericFormatV4(src) != null;
    }

    public static boolean isIPv6LiteralAddress(String src) {
        return textToNumericFormatV6(src) != null;
    }

    public static byte[] convertFromIPv4MappedAddress(byte[] addr) {
        if (!isIPv4MappedAddress(addr)) {
            return null;
        }
        byte[] newAddr = new byte[4];
        System.arraycopy(addr, 12, newAddr, 0, 4);
        return newAddr;
    }

    private static boolean isIPv4MappedAddress(byte[] addr) {
        if (addr.length >= 16 && addr[0] == (byte) 0 && addr[1] == (byte) 0 && addr[2] == (byte) 0 && addr[3] == (byte) 0 && addr[4] == (byte) 0 && addr[5] == (byte) 0 && addr[6] == (byte) 0 && addr[7] == (byte) 0 && addr[8] == (byte) 0 && addr[9] == (byte) 0 && addr[10] == (byte) -1 && addr[11] == (byte) -1) {
            return true;
        }
        return false;
    }
}
