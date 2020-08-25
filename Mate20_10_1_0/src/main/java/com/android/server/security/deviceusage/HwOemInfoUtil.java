package com.android.server.security.deviceusage;

import android.util.Slog;
import java.lang.reflect.InvocationTargetException;

public class HwOemInfoUtil {
    private static final String CHR_OEM_INFO_CLASS = "com.huawei.android.os.HwOemInfoCustEx";
    private static final int HEX_MASK = 15;
    private static final int OEM_INFO_ACTIVATION_STATUS_ID = 136;
    private static final int OEM_INFO_ACTIVATION_STATUS_SIZE = 1;
    private static final byte STATUS_ACTIVATED = -84;
    private static final String TAG = "HwCCOEM";
    private static final int WRITE_FAILED = -1;

    private HwOemInfoUtil() {
    }

    public static boolean getActivationStatus() {
        byte[] oemInfoBytes = getByteArrayFromOemInfo(136, 1);
        if (oemInfoBytes == null || oemInfoBytes.length < 1) {
            Slog.e(TAG, "getActivationStatus error!");
            return false;
        } else if (oemInfoBytes[0] == -84) {
            return true;
        } else {
            return false;
        }
    }

    public static void setActivated() {
        writeByteArrayToOemInfo(136, 1, new byte[]{STATUS_ACTIVATED});
    }

    public static void resetActivation() {
        writeByteArrayToOemInfo(136, 1, new byte[]{-69});
    }

    private static byte[] getByteArrayFromOemInfo(int type, int sizeOf) {
        byte[] bytes = new byte[0];
        try {
            Object obj = Class.forName(CHR_OEM_INFO_CLASS).getMethod("getByteArrayFromOeminfo", Integer.TYPE, Integer.TYPE).invoke(null, Integer.valueOf(type), Integer.valueOf(sizeOf));
            if (obj == null || !(obj instanceof byte[])) {
                return bytes;
            }
            return (byte[]) obj;
        } catch (ClassNotFoundException e) {
            Slog.e(TAG, "getByteArrayFromOemInfo unable to find class!");
            return bytes;
        } catch (NoSuchMethodException e2) {
            Slog.e(TAG, "Method getByteArrayFromOemInfo not found!");
            return bytes;
        } catch (IllegalAccessException e3) {
            Slog.e(TAG, "Method getByteArrayFromOemInfo access illegally!");
            return bytes;
        } catch (InvocationTargetException e4) {
            Slog.e(TAG, "Method getByteArrayFromOemInfo exception!");
            return bytes;
        }
    }

    private static int writeByteArrayToOemInfo(int type, int sizeOf, byte[] bytes) {
        try {
            Object obj = Class.forName(CHR_OEM_INFO_CLASS).getMethod("writeByteArrayToOeminfo", Integer.TYPE, Integer.TYPE, byte[].class).invoke(null, Integer.valueOf(type), Integer.valueOf(sizeOf), bytes);
            if (obj == null || !(obj instanceof Integer)) {
                return -1;
            }
            return ((Integer) obj).intValue();
        } catch (ClassNotFoundException e) {
            Slog.e(TAG, "writeByteArrayToOemInfo unable to find class!");
            return -1;
        } catch (NoSuchMethodException e2) {
            Slog.e(TAG, "Method writeByteArrayToOemInfo not found!");
            return -1;
        } catch (IllegalAccessException e3) {
            Slog.e(TAG, "Method writeByteArrayToOemInfo access illegally!");
            return -1;
        } catch (InvocationTargetException e4) {
            Slog.e(TAG, "Method writeByteArrayToOemInfo exception!");
            return -1;
        }
    }
}
