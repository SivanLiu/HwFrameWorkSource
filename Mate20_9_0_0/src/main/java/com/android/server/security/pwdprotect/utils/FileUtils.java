package com.android.server.security.pwdprotect.utils;

import android.text.TextUtils;
import android.util.Log;
import com.android.server.security.pwdprotect.model.PasswordIvsCache;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import libcore.io.IoUtils;

public class FileUtils {
    private static final int HASHLENGTH = 32;
    private static final int HASHVALUES = 5;
    private static final int IV_FIRST = 0;
    private static final int IV_FOUR = 3;
    private static final int IV_SECOND = 1;
    private static final int IV_THIRD = 2;
    private static final int KEYVALUES = 4;
    private static final int NOHASHVALUES = 8;
    private static final int PKLENGTH = 294;
    private static final int PUBLICKEY = 7;
    private static final String TAG = "PwdProtectService";

    public static byte[] readKeys(File file) {
        return readFile(file, 4);
    }

    private static byte[] readHashs(File file) {
        return readFile(file, 5);
    }

    public static byte[] readIvs(File file, int ivNo) {
        return readFile(file, ivNo);
    }

    private static byte[] readNoHashs(File file) {
        return readFile(file, 8);
    }

    public static byte[] readPublicKey() {
        return readFile(PasswordIvsCache.FILE_E_PIN2, 7);
    }

    /* JADX WARNING: Missing block: B:8:0x0034, code:
            if (r6 == -1) goto L_0x00ba;
     */
    /* JADX WARNING: Missing block: B:11:0x0041, code:
            if (r8 == -1) goto L_0x00ba;
     */
    /* JADX WARNING: Missing block: B:14:0x004f, code:
            if (r6 == -1) goto L_0x00ba;
     */
    /* JADX WARNING: Missing block: B:33:0x00b2, code:
            if (r6 == -1) goto L_0x00ba;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] readFile(File file, int valueType) {
        if (!file.exists()) {
            Log.e(TAG, "The file doesn't exist");
        }
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int buffer = 0;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            byte[] buf = new byte[4096];
            int read;
            int i;
            int len;
            switch (valueType) {
                case 0:
                case 1:
                case 2:
                case 3:
                    while (true) {
                        read = bis.read(buf);
                        i = read;
                        baos.write(buf, 16 * valueType, 16);
                        break;
                    }
                case 4:
                    if (!file.getName().equals("E_SK2")) {
                        if (!file.getName().equals("E_PIN2")) {
                            while (true) {
                                read = bis.read(buf);
                                i = read;
                                if (read == -1) {
                                    break;
                                }
                                baos.write(buf, 16 * 1, (i - 32) - (16 * 1));
                            }
                        } else {
                            while (true) {
                                i = bis.read(buf);
                                len = i;
                                if (i == -1) {
                                    break;
                                }
                                baos.write(buf, 16 * 1, ((len - 32) - (16 * 1)) - PKLENGTH);
                            }
                        }
                    } else {
                        while (true) {
                            read = bis.read(buf);
                            i = read;
                            if (read == -1) {
                                break;
                            }
                            baos.write(buf, 16 * 4, (i - 32) - (16 * 4));
                        }
                    }
                case 5:
                    while (true) {
                        read = bis.read(buf);
                        i = read;
                        baos.write(buf, i - 32, 32);
                        break;
                    }
                case 7:
                    while (true) {
                        i = bis.read(buf);
                        len = i;
                        baos.write(buf, (len - 32) - PKLENGTH, PKLENGTH);
                        break;
                    }
                case 8:
                    while (true) {
                        read = bis.read(buf);
                        i = read;
                        baos.write(buf, 0, i - 32);
                        break;
                    }
            }
            byte[] buffer2 = baos.toByteArray();
            return buffer2;
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("read file exception!");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return new byte[buffer];
        } finally {
            IoUtils.closeQuietly(fis);
            IoUtils.closeQuietly(bis);
            IoUtils.closeQuietly(baos);
        }
    }

    public static boolean writeFile(byte[] values, File fileName) {
        mkdirHwSecurity();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileOutputStream fileOutputStream = null;
        boolean e;
        try {
            byteArrayOutputStream.write(values);
            fileOutputStream = new FileOutputStream(fileName);
            byteArrayOutputStream.writeTo(fileOutputStream);
            fileOutputStream.flush();
            e = true;
            return e;
        } catch (IOException e2) {
            e = e2;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("write file exception! ");
            stringBuilder.append(fileName.getName());
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        } finally {
            IoUtils.closeQuietly(byteArrayOutputStream);
            IoUtils.closeQuietly(fileOutputStream);
        }
    }

    public static File newFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        File screenFile = new File(fileName);
        String str;
        StringBuilder stringBuilder;
        try {
            boolean result = screenFile.createNewFile();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("new file ");
            stringBuilder.append(screenFile.getName());
            stringBuilder.append(" result is :");
            stringBuilder.append(result);
            Log.i(str, stringBuilder.toString());
            return screenFile;
        } catch (Exception e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("new file exception!");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    private static Boolean verifyFile(File file) {
        String str;
        StringBuilder stringBuilder;
        if (Arrays.equals(readHashs(file), DeviceEncryptUtils.hmacSign(readNoHashs(file)))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verify File ");
            stringBuilder.append(file.getName());
            stringBuilder.append(" result is true");
            Log.i(str, stringBuilder.toString());
            return Boolean.valueOf(true);
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("verify File ");
        stringBuilder.append(file.getName());
        stringBuilder.append(" result is false");
        Log.e(str, stringBuilder.toString());
        return Boolean.valueOf(false);
    }

    public static Boolean verifyFile() {
        if (verifyFile(PasswordIvsCache.FILE_E_PWDQANSWER).booleanValue() && verifyFile(PasswordIvsCache.FILE_E_PIN2).booleanValue() && verifyFile(PasswordIvsCache.FILE_E_SK2).booleanValue() && verifyFile(PasswordIvsCache.FILE_E_PWDQ).booleanValue()) {
            Log.i(TAG, "verify File is true");
            return Boolean.valueOf(true);
        }
        Log.e(TAG, "verify File is false");
        return Boolean.valueOf(false);
    }

    private static void mkdirHwSecurity() {
        File file = PasswordIvsCache.PWDPROTECT_DIR_PATH;
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "mkdirs file failed");
        }
    }
}
