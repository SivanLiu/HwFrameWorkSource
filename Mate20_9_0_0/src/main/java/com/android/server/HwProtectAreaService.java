package com.android.server;

import android.content.Context;
import android.util.Slog;
import huawei.android.os.HwProtectArea;

public class HwProtectAreaService {
    static final String TAG = "HwProtectAreaService";
    private static volatile HwProtectAreaService mInstance = null;
    private Context mContext;
    private final Object mLock = new Object();

    private static native void nativeProtectAreaClassInit();

    private static native int nativeReadProtectArea(String str, HwProtectArea hwProtectArea, int i);

    private static native int nativeWriteProtectArea(String str, HwProtectArea hwProtectArea, int i, String str2);

    public HwProtectAreaService(Context context) {
        String str;
        StringBuilder stringBuilder;
        this.mContext = context;
        try {
            synchronized (this.mLock) {
                nativeProtectAreaClassInit();
            }
        } catch (UnsatisfiedLinkError e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("libarary ClassInit failed >>>>>");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("libarary ClassInit failed >>>>>");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
        }
    }

    public static synchronized HwProtectAreaService getInstance(Context context) {
        HwProtectAreaService hwProtectAreaService;
        synchronized (HwProtectAreaService.class) {
            if (mInstance == null) {
                mInstance = new HwProtectAreaService(context);
            }
            hwProtectAreaService = mInstance;
        }
        return hwProtectAreaService;
    }

    /* JADX WARNING: Missing block: B:24:0x004b, code:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int nativeReadProtectAreaJava(String optItem, int readBufLen, String[] readBuf, int[] errno) {
        String str;
        StringBuilder stringBuilder;
        int errorNum = -1;
        try {
            synchronized (this.mLock) {
                if (optItem == null || readBufLen < 0 || readBuf == null || errno == null) {
                    Slog.e(TAG, "nativeReadProtectAreaJava:parameter error !");
                    if (readBuf != null) {
                        readBuf[0] = "error";
                    }
                    if (errno != null) {
                        errno[0] = errorNum;
                    }
                } else {
                    HwProtectArea protectArea = new HwProtectArea(optItem);
                    int ret = nativeReadProtectArea(optItem, protectArea, readBufLen);
                    if (-1 == ret) {
                        Slog.e(TAG, "nativeReadProtectAreaJava:error ret is -1 !");
                        readBuf[0] = "error";
                        errno[0] = errorNum;
                        return ret;
                    }
                    readBuf[0] = protectArea.getReadBuf();
                    errno[0] = protectArea.getErrno();
                    return ret;
                }
            }
        } catch (UnsatisfiedLinkError e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("libarary ReadProtectArea failed >>>>>");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("libarary ReadProtectArea failed >>>>>");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
        }
        return -1;
    }

    /* JADX WARNING: Missing block: B:21:0x0039, code:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int nativeWriteProtectAreaJava(String optItem, int writeLen, String writeBuf, int[] errno) {
        String str;
        StringBuilder stringBuilder;
        int errorNum = -1;
        try {
            synchronized (this.mLock) {
                if (optItem == null || writeBuf == null || errno == null) {
                    Slog.e(TAG, "nativeWriteProtectAreaJava:parameter error !");
                    if (errno != null) {
                        errno[0] = errorNum;
                    }
                } else {
                    HwProtectArea protectArea = new HwProtectArea(optItem);
                    int ret = nativeWriteProtectArea(optItem, protectArea, writeLen, writeBuf);
                    if (-1 == ret) {
                        Slog.e(TAG, "nativeWriteProtectArea:error ret is -1 !");
                        errno[0] = errorNum;
                        return ret;
                    }
                    errno[0] = protectArea.getErrno();
                    return ret;
                }
            }
        } catch (UnsatisfiedLinkError e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("libarary WriteProtectArea failed >>>>>");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("libarary WriteProtectArea failed >>>>>");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
        }
        return -1;
    }

    public int readProtectArea(String optItem, int readBufLen, String[] readBuf, int[] errorNum) {
        this.mContext.enforceCallingOrSelfPermission("com.huawei.android.permission.PROTECTAREA", null);
        return nativeReadProtectAreaJava(optItem, readBufLen, readBuf, errorNum);
    }

    public int writeProtectArea(String optItem, int writeLen, String writeBuf, int[] errorNum) {
        if (writeBuf == null || writeBuf.length() <= writeLen) {
            this.mContext.enforceCallingOrSelfPermission("com.huawei.android.permission.PROTECTAREA", null);
            return nativeWriteProtectAreaJava(optItem, writeLen, writeBuf, errorNum);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("writeProtectArea:writeBuf.length():");
        stringBuilder.append(writeBuf.length());
        stringBuilder.append(", writeLen:");
        stringBuilder.append(writeLen);
        Slog.d(str, stringBuilder.toString());
        return -1;
    }
}
