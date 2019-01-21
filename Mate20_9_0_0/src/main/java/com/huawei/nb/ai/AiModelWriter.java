package com.huawei.nb.ai;

import com.huawei.nb.efs.EfsException;
import com.huawei.nb.efs.EfsRwChannel;
import com.huawei.nb.utils.logger.DSLog;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class AiModelWriter {
    public static final int TO_ENCRYPTED = 1;
    public static final int TO_NORMAL = 0;

    /* JADX WARNING: Removed duplicated region for block: B:19:0x005f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean writeAiModel(String filePath, byte[] data, byte[] key) {
        Throwable e;
        if (filePath == null || data == null) {
            String str;
            StringBuilder append = new StringBuilder().append("Error: Invalid input");
            if (data == null) {
                str = ", data is null.";
            } else {
                str = ".";
            }
            DSLog.e(append.append(str).toString(), new Object[0]);
            return false;
        }
        EfsRwChannel channel = null;
        try {
            channel = EfsRwChannel.open(filePath, 6, key);
            channel.startTransaction(1, 1);
            channel.truncateFile(0);
            channel.write(0, data, 0, data.length);
            channel.endTransaction(true);
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            closeEfsRwChange(channel);
            return true;
        } catch (UnsatisfiedLinkError e2) {
            e = e2;
            try {
                DSLog.e("Failed to write AI model %s, error: %s.", filePath, e.getMessage());
                if (key != null) {
                    Arrays.fill(key, (byte) 0);
                }
                closeEfsRwChange(channel);
                return false;
            } catch (Throwable th) {
                if (key != null) {
                    Arrays.fill(key, (byte) 0);
                }
                closeEfsRwChange(channel);
            }
        } catch (EfsException e22) {
            e = e22;
            DSLog.e("Failed to write AI model %s, error: %s.", filePath, e.getMessage());
            if (key != null) {
            }
            closeEfsRwChange(channel);
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0075 A:{SYNTHETIC, Splitter:B:28:0x0075} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0062 A:{Catch:{ all -> 0x0078 }} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0062 A:{Catch:{ all -> 0x0078 }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0075 A:{SYNTHETIC, Splitter:B:28:0x0075} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean changeStorageMode(String filePath, String key, int type) {
        Throwable e;
        String str;
        Object[] objArr;
        if ((type != 1 && type != 0) || key == null || filePath == null) {
            String str2;
            StringBuilder append = new StringBuilder().append("Error: Invalid input, type is ").append(type);
            if (key == null) {
                str2 = ",key is null.";
            } else {
                str2 = ".";
            }
            DSLog.e(append.append(str2).toString(), new Object[0]);
            return false;
        }
        try {
            byte[] bArr;
            byte[] keyByte = key.getBytes("UTF-8");
            if (type == 1) {
                bArr = null;
            } else {
                bArr = keyByte;
            }
            EfsRwChannel channel = EfsRwChannel.open(filePath, 6, bArr);
            channel.startTransaction(1, 1);
            if (type != 1) {
                keyByte = null;
            }
            channel.setKey(keyByte);
            channel.endTransaction(true);
            closeEfsRwChange(channel);
            return true;
        } catch (UnsupportedEncodingException e2) {
            e = e2;
            try {
                str = "Failed to change AI model %s to %s mode, error: %s.";
                objArr = new Object[3];
                objArr[0] = filePath;
                objArr[1] = type == 1 ? "encrypted" : "";
                objArr[2] = e.getMessage();
                DSLog.e(str, objArr);
                return false;
            } finally {
                closeEfsRwChange(null);
            }
        } catch (UnsatisfiedLinkError e22) {
            e = e22;
            str = "Failed to change AI model %s to %s mode, error: %s.";
            objArr = new Object[3];
            objArr[0] = filePath;
            if (type == 1) {
            }
            objArr[1] = type == 1 ? "encrypted" : "";
            objArr[2] = e.getMessage();
            DSLog.e(str, objArr);
            return false;
        } catch (EfsException e222) {
            e = e222;
            str = "Failed to change AI model %s to %s mode, error: %s.";
            objArr = new Object[3];
            objArr[0] = filePath;
            if (type == 1) {
            }
            objArr[1] = type == 1 ? "encrypted" : "";
            objArr[2] = e.getMessage();
            DSLog.e(str, objArr);
            return false;
        }
    }

    private static void closeEfsRwChange(EfsRwChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (EfsException e) {
                DSLog.e("Failed to close EfsRwChannel.", new Object[0]);
            }
        }
    }
}
