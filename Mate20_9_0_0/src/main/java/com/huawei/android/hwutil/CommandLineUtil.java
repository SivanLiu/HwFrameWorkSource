package com.huawei.android.hwutil;

import android.os.FileUtils;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

public class CommandLineUtil {
    public static final int FILE_RULE = 509;
    private static final String TAG = "CommandLineUtil";

    public static String addQuoteMark(String param) {
        if (TextUtils.isEmpty(param) || param.charAt(0) == '\"' || param.contains(PhoneConstants.APN_TYPE_ALL)) {
            return param;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"");
        stringBuilder.append(param);
        stringBuilder.append("\"");
        return stringBuilder.toString();
    }

    public static Boolean echo(String role, String wrrule, String path) {
        String str;
        StringBuilder stringBuilder;
        Boolean valueOf;
        if (path == null) {
            return Boolean.valueOf(false);
        }
        File file = new File(path);
        if (file.exists()) {
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(file);
                os.write(wrrule.getBytes());
                try {
                    os.close();
                } catch (Exception e) {
                }
                return Boolean.valueOf(true);
            } catch (IOException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Write failed to file ");
                stringBuilder.append(path);
                Log.e(str, stringBuilder.toString());
                valueOf = Boolean.valueOf(false);
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception e3) {
                    }
                }
                return valueOf;
            } catch (Exception e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Write failed to file ");
                stringBuilder.append(path);
                Log.e(str, stringBuilder.toString());
                valueOf = Boolean.valueOf(false);
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception e5) {
                    }
                }
                return valueOf;
            } catch (Throwable th) {
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception e6) {
                    }
                }
            }
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("file is not exists ");
            stringBuilder2.append(path);
            Log.e(str2, stringBuilder2.toString());
            return Boolean.valueOf(false);
        }
    }

    private static boolean chmodAllFiles(File file) {
        int i = 0;
        if (file == null || !file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return false;
            }
            int length = files.length;
            while (i < length) {
                File sfile = files[i];
                if (sfile.isDirectory()) {
                    chmodAllFiles(sfile);
                }
                FileUtils.setPermissions(sfile, 509, 1000, 1023);
                i++;
            }
            if (file.exists()) {
                FileUtils.setPermissions(file, 509, 1000, 1023);
            }
            return true;
        }
        FileUtils.setPermissions(file, 509, 1000, 1023);
        return false;
    }

    public static Boolean chmod(String rule, String chrule, String path) {
        if (path == null) {
            return Boolean.valueOf(false);
        }
        return Boolean.valueOf(chmodAllFiles(new File(path)));
    }

    public static boolean chown(String rule, String owner, String group, String path) {
        if (path == null) {
            return false;
        }
        return chmodAllFiles(new File(path));
    }

    public static boolean mkdir(String rule, String path) {
        if (path == null) {
            return false;
        }
        try {
            if (new File(path).mkdir()) {
                return true;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mkdir failed path = ");
            stringBuilder.append(path);
            Log.e(str, stringBuilder.toString());
            return false;
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("create folder exception: ");
            stringBuilder2.append(ex.getMessage());
            Log.e(str2, stringBuilder2.toString());
            return true;
        }
    }

    public static boolean mv(String rule, String opath, String npath) {
        if (opath == null || npath == null) {
            return false;
        }
        return renameFile(new File(opath), new File(npath));
    }

    private static boolean renameFile(File fileSrc, File fileTarget) {
        try {
            if (fileTarget.delete()) {
                Log.i(TAG, "renamefile, delete target success");
            }
            if (fileSrc.renameTo(fileTarget)) {
                return true;
            }
            Log.e(TAG, "rename file failed.");
            return false;
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rename file exception:");
            stringBuilder.append(ex.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public static boolean rm(String rule, String path) {
        if (path == null) {
            return false;
        }
        return deleteAll(new File(path));
    }

    private static boolean deleteAll(File file) {
        int i = 0;
        if (file == null || !file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return false;
            }
            int length = files.length;
            while (i < length) {
                File sfile = files[i];
                if (sfile.isDirectory()) {
                    deleteAll(sfile);
                }
                if (sfile.delete()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(": delete file success :");
                    stringBuilder.append(sfile);
                    Log.i(str, stringBuilder.toString());
                }
                i++;
            }
            if (file.exists() && !file.delete()) {
                Log.e(TAG, "FatherFile delete false");
            }
            return true;
        }
        if (file.delete()) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(": delete file success :");
            stringBuilder2.append(file);
            Log.i(str2, stringBuilder2.toString());
        }
        return false;
    }

    public static boolean link(String role, String oldpath, String newpath) {
        try {
            Files.createSymbolicLink(Paths.get(newpath, new String[0]), Paths.get(oldpath, new String[0]), new FileAttribute[0]);
            return true;
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("link error = ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public static boolean unlink(String path) {
        try {
            Os.unlink(path);
            return true;
        } catch (ErrnoException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unlink error = ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }
}
