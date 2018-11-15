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

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] readFile(File file, int valueType) {
        IOException e;
        Throwable th;
        Object fis;
        if (!file.exists()) {
            Log.e(TAG, "The file doesn't exist");
        }
        AutoCloseable autoCloseable = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            FileInputStream fis2 = new FileInputStream(file);
            try {
                BufferedInputStream bis2 = new BufferedInputStream(fis2);
                try {
                    byte[] buf = new byte[4096];
                    int len;
                    switch (valueType) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                            while (bis2.read(buf) != -1) {
                                baos.write(buf, valueType * 16, 16);
                            }
                            break;
                        case 4:
                            if (!file.getName().equals("E_SK2")) {
                                if (!file.getName().equals("E_PIN2")) {
                                    while (true) {
                                        len = bis2.read(buf);
                                        if (len == -1) {
                                            break;
                                        }
                                        baos.write(buf, 16, (len - 32) - 16);
                                    }
                                } else {
                                    while (true) {
                                        len = bis2.read(buf);
                                        if (len == -1) {
                                            break;
                                        }
                                        baos.write(buf, 16, ((len - 32) - 16) - 294);
                                    }
                                }
                            } else {
                                while (true) {
                                    len = bis2.read(buf);
                                    if (len == -1) {
                                        break;
                                    }
                                    baos.write(buf, 64, (len - 32) - 64);
                                }
                            }
                        case 5:
                            while (true) {
                                len = bis2.read(buf);
                                baos.write(buf, len - 32, 32);
                                break;
                            }
                        case 7:
                            while (true) {
                                len = bis2.read(buf);
                                baos.write(buf, (len - 32) - 294, PKLENGTH);
                                break;
                            }
                        case 8:
                            while (true) {
                                len = bis2.read(buf);
                                baos.write(buf, 0, len - 32);
                                break;
                            }
                    }
                    byte[] buffer = baos.toByteArray();
                    IoUtils.closeQuietly(fis2);
                    IoUtils.closeQuietly(bis2);
                    IoUtils.closeQuietly(baos);
                    return buffer;
                } catch (IOException e2) {
                    e = e2;
                    bis = bis2;
                    autoCloseable = fis2;
                } catch (Throwable th2) {
                    th = th2;
                    bis = bis2;
                    autoCloseable = fis2;
                }
            } catch (IOException e3) {
                e = e3;
                fis = fis2;
                try {
                    Log.e(TAG, "read file exception!" + e.getMessage());
                    IoUtils.closeQuietly(autoCloseable);
                    IoUtils.closeQuietly(bis);
                    IoUtils.closeQuietly(baos);
                    return new byte[0];
                } catch (Throwable th3) {
                    th = th3;
                    IoUtils.closeQuietly(autoCloseable);
                    IoUtils.closeQuietly(bis);
                    IoUtils.closeQuietly(baos);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                fis = fis2;
                IoUtils.closeQuietly(autoCloseable);
                IoUtils.closeQuietly(bis);
                IoUtils.closeQuietly(baos);
                throw th;
            }
        } catch (IOException e4) {
            e = e4;
            Log.e(TAG, "read file exception!" + e.getMessage());
            IoUtils.closeQuietly(autoCloseable);
            IoUtils.closeQuietly(bis);
            IoUtils.closeQuietly(baos);
            return new byte[0];
        }
    }

    public static boolean writeFile(byte[] values, File fileName) {
        IOException e;
        Object obj;
        Throwable th;
        mkdirHwSecurity();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        AutoCloseable fileOutputStream = null;
        try {
            byteArrayOutputStream.write(values);
            FileOutputStream fileOutputStream2 = new FileOutputStream(fileName);
            try {
                byteArrayOutputStream.writeTo(fileOutputStream2);
                fileOutputStream2.flush();
                IoUtils.closeQuietly(byteArrayOutputStream);
                IoUtils.closeQuietly(fileOutputStream2);
                return true;
            } catch (IOException e2) {
                e = e2;
                obj = fileOutputStream2;
                try {
                    Log.e(TAG, "write file exception! " + fileName.getName() + e.getMessage());
                    IoUtils.closeQuietly(byteArrayOutputStream);
                    IoUtils.closeQuietly(fileOutputStream);
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(byteArrayOutputStream);
                    IoUtils.closeQuietly(fileOutputStream);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                obj = fileOutputStream2;
                IoUtils.closeQuietly(byteArrayOutputStream);
                IoUtils.closeQuietly(fileOutputStream);
                throw th;
            }
        } catch (IOException e3) {
            e = e3;
            Log.e(TAG, "write file exception! " + fileName.getName() + e.getMessage());
            IoUtils.closeQuietly(byteArrayOutputStream);
            IoUtils.closeQuietly(fileOutputStream);
            return false;
        }
    }

    public static File newFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        File screenFile = new File(fileName);
        try {
            Log.i(TAG, "new file " + screenFile.getName() + " result is :" + screenFile.createNewFile());
            return screenFile;
        } catch (Exception e) {
            Log.e(TAG, "new file exception!" + e.getMessage());
            return null;
        }
    }

    private static Boolean verifyFile(File file) {
        if (Arrays.equals(readHashs(file), DeviceEncryptUtils.hmacSign(readNoHashs(file)))) {
            Log.i(TAG, "verify File " + file.getName() + " result is true");
            return Boolean.valueOf(true);
        }
        Log.e(TAG, "verify File " + file.getName() + " result is false");
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
