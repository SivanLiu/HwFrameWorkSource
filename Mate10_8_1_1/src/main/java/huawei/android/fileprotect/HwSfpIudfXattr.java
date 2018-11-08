package huawei.android.fileprotect;

import android.util.Log;

public class HwSfpIudfXattr {
    private static final int LINK_ERROR = -2;
    private static final String TAG = "HwSfpIudfXattr";

    public static final native int getFileXattr(String str, int i);

    public static final native int setFileXattr(String str, String str2, int i, int i2);

    static {
        try {
            System.loadLibrary("iudf_xattr");
        } catch (Throwable e) {
            Log.e(TAG, "jni, loadLibrary error" + e.getMessage());
            Log.e(TAG, "error, load libisecurity failed");
        }
    }

    public static final int setFileXattrEx(String path, String desc, int storageType, int fileType) {
        int result = -2;
        try {
            result = setFileXattr(path, desc, storageType, fileType);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "error, nativeSetFileXattr failed:" + e.getMessage());
        }
        return result;
    }

    public static final int getFileXattrEx(String path, int type) {
        int result = -2;
        try {
            result = getFileXattr(path, type);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "error, nativeGetFileXattr failed:" + e.getMessage());
        }
        return result;
    }
}
