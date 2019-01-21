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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("jni, loadLibrary error");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            Log.e(TAG, "error, load libisecurity failed");
        }
    }

    public static final int setFileXattrEx(String path, String desc, int storageType, int fileType) {
        try {
            return setFileXattr(path, desc, storageType, fileType);
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error, nativeSetFileXattr failed:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return -2;
        }
    }

    public static final int getFileXattrEx(String path, int type) {
        try {
            return getFileXattr(path, type);
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error, nativeGetFileXattr failed:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return -2;
        }
    }
}
