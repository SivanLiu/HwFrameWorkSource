package com.android.contacts.hap.numbermark.utils;

import android.content.Context;
import android.text.TextUtils;
import com.android.contacts.util.HwLog;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptFileUtils {
    private static final int BUF_LEN = 4096;
    private static final int INDEX_0 = 3;
    private static final int INDEX_1 = 4;
    private static final int INDEX_2 = 4;
    private static final int INDEX_3 = 3;
    private static final int INDEX_LEAVE = 14;
    private static final int INDEX_MIN_LEN = 16;
    private static final String SHA = "SHA-1";
    private static final String TAG = "ContactsEncryptFileUtils";
    private static final String UTF = "UTF-8";

    public static String getSecretKeyFromAssets(Context context, String suffix, String subDir) {
        if (context == null || TextUtils.isEmpty(suffix)) {
            return "";
        }
        return getStrBySuffix(context, suffix, true, subDir);
    }

    public static void saveStrWithSuffix(Context context, String srcStr, String suffix) {
        if (!TextUtils.isEmpty(srcStr) && !TextUtils.isEmpty(suffix)) {
            int[] indexes = getDividerCounts(srcStr, suffix);
            saveIndexFile(context, indexes, suffix);
            saveStrsToFile(context, getDividerStr(srcStr, indexes), suffix);
        }
    }

    public static String getStrBySuffix(Context context, String suffix, boolean fromAssets, String subDir) {
        StringBuilder desStr = new StringBuilder("");
        int[] indexes = getIndexFromFile(context, suffix, fromAssets, subDir);
        if (indexes.length <= 0) {
            return desStr.toString();
        }
        int len = indexes.length;
        for (int i = 0; i < len; i++) {
            String cacheFileName = hashSHAKey(suffix + i);
            if (fromAssets && !TextUtils.isEmpty(subDir)) {
                cacheFileName = subDir + File.separator + cacheFileName;
            }
            String content = readStrsFromFile(getInputStream(context, cacheFileName, fromAssets));
            if (TextUtils.isEmpty(content)) {
                return "";
            }
            if (i == len / 2) {
                desStr.append(String.copyValueOf(mixChars(content.toCharArray(), false)));
            } else {
                desStr.append(content);
            }
        }
        return desStr.toString();
    }

    private static String[] getDividerStr(String srcStr, int[] indexes) {
        String[] result = new String[indexes.length];
        if (!TextUtils.isEmpty(srcStr) && indexes.length > 0) {
            int pos = 0;
            int i = 0;
            while (i < indexes.length) {
                try {
                    if (i == indexes.length / 2) {
                        result[i] = String.copyValueOf(mixChars(srcStr.substring(pos, indexes[i] + pos).toCharArray(), true));
                    } else {
                        result[i] = srcStr.substring(pos, indexes[i] + pos);
                    }
                    pos += indexes[i];
                    i++;
                } catch (IndexOutOfBoundsException e) {
                    HwLog.e(TAG, "get divider string error. ", e);
                }
            }
        }
        return result;
    }

    private static char[] mixChars(char[] src, boolean mix) {
        int i = 0;
        char[] newChar = new char[src.length];
        int i2;
        int length;
        if (mix) {
            i2 = 0;
            length = src.length;
            while (i < length) {
                newChar[i2] = (char) (src[i] + 2);
                i2++;
                i++;
            }
        } else {
            i2 = 0;
            length = src.length;
            while (i < length) {
                newChar[i2] = (char) (src[i] - 2);
                i2++;
                i++;
            }
        }
        return newChar;
    }

    private static void saveIndexFile(Context context, int[] indexes, String suffix) {
        if (indexes != null && indexes.length > 0 && !TextUtils.isEmpty(suffix)) {
            StringBuilder saveStr = new StringBuilder("");
            for (int append : indexes) {
                saveStr.append(append).append("/");
            }
            try {
                stringToFile(getFile(context, hashSHAKey(suffix)), saveStr.substring(0, saveStr.length() - 1));
            } catch (IOException e) {
                HwLog.e(TAG, "save index to file error. ", e);
            }
        }
    }

    private static int[] getIndexFromFile(Context context, String suffix, boolean fromAssets, String subDir) {
        if (TextUtils.isEmpty(suffix)) {
            return new int[0];
        }
        String cacheFileName = hashSHAKey(suffix);
        if (fromAssets && !TextUtils.isEmpty(subDir)) {
            cacheFileName = subDir + File.separator + cacheFileName;
        }
        String[] pieces = readStrsFromFile(getInputStream(context, cacheFileName, fromAssets)).split("/");
        if (pieces.length <= 0) {
            return new int[0];
        }
        try {
            int[] counts = new int[pieces.length];
            for (int i = 0; i < pieces.length; i++) {
                counts[i] = Integer.parseInt(pieces[i]);
            }
            return counts;
        } catch (NumberFormatException e) {
            HwLog.e(TAG, "get index from file error. ", e);
            return new int[0];
        }
    }

    private static String readStrsFromFile(InputStream is) {
        FileNotFoundException e;
        IOException e2;
        Throwable th;
        if (is == null) {
            return "";
        }
        InputStreamReader isr = null;
        BufferedReader br = null;
        StringBuilder buffer = new StringBuilder("");
        try {
            InputStreamReader isr2 = new InputStreamReader(is, UTF);
            try {
                BufferedReader br2 = new BufferedReader(isr2);
                try {
                    char[] bytes = new char[BUF_LEN];
                    while (true) {
                        int bytesRead = br2.read(bytes);
                        if (bytesRead == -1) {
                            break;
                        }
                        buffer.append(bytes, 0, bytesRead);
                    }
                    closeQuietly(isr2);
                    closeQuietly(br2);
                    br = br2;
                    isr = isr2;
                } catch (FileNotFoundException e3) {
                    e = e3;
                    br = br2;
                    isr = isr2;
                } catch (IOException e4) {
                    e2 = e4;
                    br = br2;
                    isr = isr2;
                } catch (Throwable th2) {
                    th = th2;
                    br = br2;
                    isr = isr2;
                }
            } catch (FileNotFoundException e5) {
                e = e5;
                isr = isr2;
                try {
                    HwLog.e(TAG, "read str from file error. ", e);
                    closeQuietly(isr);
                    closeQuietly(br);
                    return buffer.toString();
                } catch (Throwable th3) {
                    th = th3;
                    closeQuietly(isr);
                    closeQuietly(br);
                    throw th;
                }
            } catch (IOException e6) {
                e2 = e6;
                isr = isr2;
                HwLog.e(TAG, "read str from file error. ", e2);
                closeQuietly(isr);
                closeQuietly(br);
                return buffer.toString();
            } catch (Throwable th4) {
                th = th4;
                isr = isr2;
                closeQuietly(isr);
                closeQuietly(br);
                throw th;
            }
        } catch (FileNotFoundException e7) {
            e = e7;
            HwLog.e(TAG, "read str from file error. ", e);
            closeQuietly(isr);
            closeQuietly(br);
            return buffer.toString();
        } catch (IOException e8) {
            e2 = e8;
            HwLog.e(TAG, "read str from file error. ", e2);
            closeQuietly(isr);
            closeQuietly(br);
            return buffer.toString();
        }
        return buffer.toString();
    }

    private static void saveStrsToFile(Context context, String[] strs, String suffix) {
        if (context != null && strs != null && strs.length > 0 && !TextUtils.isEmpty(suffix)) {
            for (int i = 0; i < strs.length; i++) {
                try {
                    stringToFile(getFile(context, hashSHAKey(suffix + i)), strs[i]);
                } catch (IOException e) {
                    HwLog.e(TAG, "save str to file error. ", e);
                }
            }
        }
    }

    private static String getFile(Context context, String filename) {
        if (context == null || TextUtils.isEmpty(filename)) {
            return "";
        }
        return context.getFilesDir() + File.separator + filename;
    }

    private static InputStream getInputStream(Context context, String filename, boolean fromAssets) {
        if (context == null || TextUtils.isEmpty(filename)) {
            return null;
        }
        if (fromAssets) {
            try {
                return context.getAssets().open(filename);
            } catch (IOException e) {
                HwLog.e(TAG, "get input stream error. ", e);
            }
        } else {
            File f = new File(context.getFilesDir() + File.separator + filename);
            if (f.exists()) {
                return new FileInputStream(f);
            }
            return null;
        }
    }

    public static int[] getDividerCounts(String srcStr, String suffix) {
        if (TextUtils.isEmpty(srcStr) || srcStr.length() < INDEX_MIN_LEN || TextUtils.isEmpty(suffix)) {
            return new int[0];
        }
        return new int[]{3, 4, 4, 3, srcStr.length() - 14};
    }

    private static String hashSHAKey(String filename) {
        return hashKeyForFile(filename, SHA);
    }

    public static String hashKeyForFile(String key, String algorithm) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance(algorithm);
            mDigest.update(key.getBytes(UTF));
            return bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            HwLog.e(TAG, "hash for file error. ", e);
            return String.valueOf(key.hashCode());
        } catch (Exception e2) {
            HwLog.e(TAG, "hash for file error. ", e2);
            return "";
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 255);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private static void stringToFile(String filename, String string) throws IOException {
        Throwable th;
        OutputStreamWriter osw = null;
        try {
            OutputStreamWriter osw2 = new OutputStreamWriter(new FileOutputStream(filename, true), UTF);
            try {
                osw2.write(string);
                closeQuietly(osw2);
            } catch (Throwable th2) {
                th = th2;
                osw = osw2;
                closeQuietly(osw);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            closeQuietly(osw);
            throw th;
        }
    }

    private static void closeQuietly(Closeable f) {
        if (f != null) {
            try {
                f.close();
            } catch (IOException e) {
                HwLog.w(TAG, "close file error. ", e);
            }
        }
    }
}
