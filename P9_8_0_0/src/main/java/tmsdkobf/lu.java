package tmsdkobf;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.utils.f;
import tmsdk.common.utils.l;
import tmsdk.common.utils.l.a;

public final class lu {
    private static final String[][] yV;

    static {
        String[][] strArr = new String[69][];
        strArr[0] = new String[]{"3gp", "video/3gpp"};
        strArr[1] = new String[]{"apk", "application/vnd.android.package-archive"};
        strArr[2] = new String[]{"asf", "video/x-ms-asf"};
        strArr[3] = new String[]{"avi", "video/x-msvideo"};
        strArr[4] = new String[]{"bin", "application/octet-stream"};
        strArr[5] = new String[]{"bmp", "image/bmp"};
        strArr[6] = new String[]{"c", "text/plain"};
        strArr[7] = new String[]{"class", "application/octet-stream"};
        strArr[8] = new String[]{"conf", "text/plain"};
        strArr[9] = new String[]{"cpp", "text/plain"};
        strArr[10] = new String[]{"doc", "application/msword"};
        strArr[11] = new String[]{"docx", "application/msword"};
        strArr[12] = new String[]{"exe", "application/octet-stream"};
        strArr[13] = new String[]{"gif", "image/gif"};
        strArr[14] = new String[]{"gtar", "application/x-gtar"};
        strArr[15] = new String[]{"gz", "application/x-gzip"};
        strArr[16] = new String[]{"h", "text/plain"};
        strArr[17] = new String[]{"htm", "text/html"};
        strArr[18] = new String[]{"html", "text/html"};
        strArr[19] = new String[]{"jar", "application/java-archive"};
        strArr[20] = new String[]{"java", "text/plain"};
        strArr[21] = new String[]{"jpeg", "image/jpeg"};
        strArr[22] = new String[]{"jpg", "image/jpeg"};
        strArr[23] = new String[]{"js", "application/x-javascript"};
        strArr[24] = new String[]{"log", "text/plain"};
        strArr[25] = new String[]{"m3u", "audio/x-mpegurl"};
        strArr[26] = new String[]{"m4a", "audio/mp4a-latm"};
        strArr[27] = new String[]{"m4b", "audio/mp4a-latm"};
        strArr[28] = new String[]{"m4p", "audio/mp4a-latm"};
        strArr[29] = new String[]{"m4u", "video/vnd.mpegurl"};
        strArr[30] = new String[]{"m4v", "video/x-m4v"};
        strArr[31] = new String[]{"mov", "video/quicktime"};
        strArr[32] = new String[]{"mp2", "audio/x-mpeg"};
        strArr[33] = new String[]{"mp3", "audio/x-mpeg"};
        strArr[34] = new String[]{"mp4", "video/mp4"};
        strArr[35] = new String[]{"mpc", "application/vnd.mpohn.certificate"};
        strArr[36] = new String[]{"mpe", "video/mpeg"};
        strArr[37] = new String[]{"mpeg", "video/mpeg"};
        strArr[38] = new String[]{"mpg", "video/mpeg"};
        strArr[39] = new String[]{"mpg4", "video/mp4"};
        strArr[40] = new String[]{"mpga", "audio/mpeg"};
        strArr[41] = new String[]{"msg", "application/vnd.ms-outlook"};
        strArr[42] = new String[]{"ogg", "audio/ogg"};
        strArr[43] = new String[]{"pdf", "application/pdf"};
        strArr[44] = new String[]{"png", "image/png"};
        strArr[45] = new String[]{"pps", "application/vnd.ms-powerpoint"};
        strArr[46] = new String[]{"ppsx", "application/vnd.ms-powerpoint"};
        strArr[47] = new String[]{"ppt", "application/vnd.ms-powerpoint"};
        strArr[48] = new String[]{"pptx", "application/vnd.ms-powerpoint"};
        strArr[49] = new String[]{"xls", "application/vnd.ms-excel"};
        strArr[50] = new String[]{"xlsx", "application/vnd.ms-excel"};
        strArr[51] = new String[]{"prop", "text/plain"};
        strArr[52] = new String[]{"rar", "application/x-rar-compressed"};
        strArr[53] = new String[]{"rc", "text/plain"};
        strArr[54] = new String[]{"rmvb", "audio/x-pn-realaudio"};
        strArr[55] = new String[]{"rtf", "application/rtf"};
        strArr[56] = new String[]{"sh", "text/plain"};
        strArr[57] = new String[]{"tar", "application/x-tar"};
        strArr[58] = new String[]{"tgz", "application/x-compressed"};
        strArr[59] = new String[]{"txt", "text/plain"};
        strArr[60] = new String[]{"wav", "audio/x-wav"};
        strArr[61] = new String[]{"wma", "audio/x-ms-wma"};
        strArr[62] = new String[]{"wmv", "audio/x-ms-wmv"};
        strArr[63] = new String[]{"wps", "application/vnd.ms-works"};
        strArr[64] = new String[]{"xml", "text/plain"};
        strArr[65] = new String[]{"z", "application/x-compress"};
        strArr[66] = new String[]{"zip", "application/zip"};
        strArr[67] = new String[]{"epub", "application/epub+zip"};
        strArr[68] = new String[]{"", "*/*"};
        yV = strArr;
    }

    private static boolean a(Context -l_3_R, String str, boolean z) {
        int -l_9_I;
        int -l_10_I;
        InputStream -l_5_R;
        Object -l_11_R;
        boolean z2;
        if (z) {
            return true;
        }
        Context -l_3_R2 = TMSDKContext.getCurrentContext();
        if (-l_3_R2 != null) {
            -l_3_R = -l_3_R2;
        }
        InputStream inputStream = null;
        int i = 0;
        int -l_7_I = 0;
        Object -l_8_R;
        try {
            inputStream = -l_3_R.getAssets().open(str);
            -l_8_R = new byte[28];
            inputStream.read(-l_8_R);
            i = (((-l_8_R[4] & 255) | ((-l_8_R[5] & 255) << 8)) | ((-l_8_R[6] & 255) << 16)) | ((-l_8_R[7] & 255) << 24);
            -l_7_I = (((-l_8_R[24] & 255) | ((-l_8_R[25] & 255) << 8)) | ((-l_8_R[26] & 255) << 16)) | ((-l_8_R[27] & 255) << 24);
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    -l_9_I = 0;
                    -l_10_I = 0;
                    -l_5_R = new FileInputStream(-l_3_R.getFilesDir().toString() + File.separator + str);
                    -l_11_R = new byte[28];
                    try {
                        -l_5_R.read(-l_11_R);
                        -l_9_I = (((-l_11_R[4] & 255) | ((-l_11_R[5] & 255) << 8)) | ((-l_11_R[6] & 255) << 16)) | ((-l_11_R[7] & 255) << 24);
                        -l_10_I = (((-l_11_R[24] & 255) | ((-l_11_R[25] & 255) << 8)) | ((-l_11_R[26] & 255) << 16)) | ((-l_11_R[27] & 255) << 24);
                        if (-l_5_R != null) {
                            try {
                                -l_5_R.close();
                            } catch (IOException e2) {
                            }
                        }
                        inputStream = -l_5_R;
                    } catch (Exception e3) {
                        -l_11_R = e3;
                        inputStream = -l_5_R;
                        try {
                            -l_11_R.printStackTrace();
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e4) {
                                }
                            }
                            if (i != -l_9_I) {
                                return z2;
                            }
                            return z2;
                        } catch (Throwable th) {
                            Object -l_12_R = th;
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e5) {
                                }
                            }
                            throw -l_12_R;
                        }
                    } catch (Throwable th2) {
                        -l_12_R = th2;
                        inputStream = -l_5_R;
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        throw -l_12_R;
                    }
                    if (i != -l_9_I) {
                        return z2;
                    }
                    return z2;
                }
            }
        } catch (Object -l_8_R2) {
            -l_8_R2.printStackTrace();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e6) {
                    -l_9_I = 0;
                    -l_10_I = 0;
                    -l_5_R = new FileInputStream(-l_3_R.getFilesDir().toString() + File.separator + str);
                    -l_11_R = new byte[28];
                    -l_5_R.read(-l_11_R);
                    -l_9_I = (((-l_11_R[4] & 255) | ((-l_11_R[5] & 255) << 8)) | ((-l_11_R[6] & 255) << 16)) | ((-l_11_R[7] & 255) << 24);
                    -l_10_I = (((-l_11_R[24] & 255) | ((-l_11_R[25] & 255) << 8)) | ((-l_11_R[26] & 255) << 16)) | ((-l_11_R[27] & 255) << 24);
                    if (-l_5_R != null) {
                        -l_5_R.close();
                    }
                    inputStream = -l_5_R;
                    if (i != -l_9_I) {
                        return z2;
                    }
                    return z2;
                }
            }
        } catch (Throwable th3) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e7) {
                }
            }
        }
        -l_9_I = 0;
        -l_10_I = 0;
        try {
            -l_5_R = new FileInputStream(-l_3_R.getFilesDir().toString() + File.separator + str);
            try {
                -l_11_R = new byte[28];
                -l_5_R.read(-l_11_R);
                -l_9_I = (((-l_11_R[4] & 255) | ((-l_11_R[5] & 255) << 8)) | ((-l_11_R[6] & 255) << 16)) | ((-l_11_R[7] & 255) << 24);
                -l_10_I = (((-l_11_R[24] & 255) | ((-l_11_R[25] & 255) << 8)) | ((-l_11_R[26] & 255) << 16)) | ((-l_11_R[27] & 255) << 24);
                if (-l_5_R != null) {
                    -l_5_R.close();
                }
                inputStream = -l_5_R;
            } catch (Exception e8) {
                -l_11_R = e8;
                inputStream = -l_5_R;
                -l_11_R.printStackTrace();
                if (inputStream != null) {
                    inputStream.close();
                }
                if (i != -l_9_I) {
                    return z2;
                }
                return z2;
            } catch (Throwable th4) {
                -l_12_R = th4;
                inputStream = -l_5_R;
                if (inputStream != null) {
                    inputStream.close();
                }
                throw -l_12_R;
            }
        } catch (Exception e9) {
            -l_11_R = e9;
            -l_11_R.printStackTrace();
            if (inputStream != null) {
                inputStream.close();
            }
            if (i != -l_9_I) {
                return z2;
            }
            return z2;
        }
        z2 = i != -l_9_I || -l_7_I > -l_10_I;
        return z2;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized String b(Context -l_3_R, String str, String str2) {
        Object -l_4_R;
        synchronized (lu.class) {
            String str3;
            InputStream inputStream;
            FileOutputStream fileOutputStream;
            Object -l_7_R;
            int -l_8_I;
            int -l_9_I;
            int -l_10_I;
            String -l_11_R;
            Object -l_12_R;
            Object -l_13_R;
            Object -l_14_R;
            FileOutputStream -l_6_R;
            Object -l_11_R2;
            int -l_12_I;
            Context -l_3_R2 = TMSDKContext.getCurrentContext();
            if (-l_3_R2 != null) {
                -l_3_R = -l_3_R2;
            }
            if (-l_3_R == null) {
                try {
                    throw new Exception("TMSDKContext is null");
                } catch (Object -l_4_R2) {
                    -l_4_R2.printStackTrace();
                }
            }
            if (str2 != null) {
                if (!str2.equals("")) {
                    -l_4_R2 = new File(str2);
                    if (!-l_4_R2.exists() || !-l_4_R2.isDirectory()) {
                        -l_4_R2.mkdirs();
                    }
                    str3 = str2 + File.separator + str;
                    inputStream = null;
                    fileOutputStream = null;
                    -l_7_R = new File(str3);
                    -l_8_I = -l_7_R.exists();
                    -l_9_I = UpdateConfig.isUpdatableAssetFile(str);
                    -l_10_I = 0;
                    if (!str.equals(UpdateConfig.VIRUS_BASE_NAME)) {
                        if (!str.equals(UpdateConfig.VIRUS_BASE_EN_NAME)) {
                            if (-l_8_I == 0 && -l_9_I != 0) {
                                -l_11_R = (String) UpdateConfig.sDeprecatedNameMap.get(str);
                                if (-l_11_R != null) {
                                    -l_12_R = str3.substring(0, str3.lastIndexOf(File.separator) + 1) + -l_11_R;
                                    -l_13_R = new File(-l_12_R);
                                    -l_14_R = new File(-l_12_R + UpdateConfig.PATCH_SUFIX);
                                    if (-l_13_R.exists()) {
                                        -l_13_R.delete();
                                    }
                                    if (-l_14_R.exists()) {
                                        -l_14_R.delete();
                                    }
                                }
                            }
                            if (-l_8_I != 0) {
                                if (!str.equals("MToken.zip")) {
                                    if (str.equals(UpdateConfig.VIRUS_BASE_NAME) || str.equals(UpdateConfig.VIRUS_BASE_EN_NAME)) {
                                    }
                                    if (!str.equals(UpdateConfig.LOCATION_NAME) || !r(-l_3_R)) {
                                        if (-l_9_I == 0 || str.equals(UpdateConfig.VIRUS_BASE_NAME) || str.equals(UpdateConfig.VIRUS_BASE_EN_NAME) || str.equals(UpdateConfig.LOCATION_NAME) || !e(-l_3_R, str)) {
                                            if (-l_9_I != 0) {
                                                if (-l_5_R != null) {
                                                    try {
                                                        -l_5_R.close();
                                                    } catch (IOException e) {
                                                    }
                                                }
                                                if (fileOutputStream != null) {
                                                    try {
                                                        fileOutputStream.close();
                                                    } catch (IOException e2) {
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            d(-l_7_R);
                            inputStream = -l_3_R.getResources().getAssets().open(str, 1);
                            -l_6_R = new FileOutputStream(-l_7_R);
                            -l_11_R2 = new byte[8192];
                            while (true) {
                                -l_12_I = inputStream.read(-l_11_R2);
                                if (-l_12_I <= 0) {
                                    break;
                                }
                                -l_6_R.write(-l_11_R2, 0, -l_12_I);
                            }
                            -l_6_R.getChannel().force(true);
                            -l_6_R.flush();
                            fileOutputStream = -l_6_R;
                            if (-l_5_R != null) {
                                -l_5_R.close();
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.close();
                            }
                        }
                    }
                    if (eE()) {
                        -l_10_I = 1;
                    }
                    -l_11_R = (String) UpdateConfig.sDeprecatedNameMap.get(str);
                    if (-l_11_R != null) {
                        -l_12_R = str3.substring(0, str3.lastIndexOf(File.separator) + 1) + -l_11_R;
                        -l_13_R = new File(-l_12_R);
                        -l_14_R = new File(-l_12_R + UpdateConfig.PATCH_SUFIX);
                        if (-l_13_R.exists()) {
                            -l_13_R.delete();
                        }
                        if (-l_14_R.exists()) {
                            -l_14_R.delete();
                        }
                    }
                    if (-l_8_I != 0) {
                        if (str.equals("MToken.zip")) {
                            if (str.equals(UpdateConfig.VIRUS_BASE_NAME)) {
                                if (!str.equals(UpdateConfig.LOCATION_NAME)) {
                                }
                                if (-l_9_I == 0) {
                                }
                                if (-l_9_I != 0) {
                                    if (-l_5_R != null) {
                                        -l_5_R.close();
                                    }
                                    if (fileOutputStream != null) {
                                        fileOutputStream.close();
                                    }
                                }
                            }
                        }
                    }
                    d(-l_7_R);
                    inputStream = -l_3_R.getResources().getAssets().open(str, 1);
                    -l_6_R = new FileOutputStream(-l_7_R);
                    -l_11_R2 = new byte[8192];
                    while (true) {
                        -l_12_I = inputStream.read(-l_11_R2);
                        if (-l_12_I <= 0) {
                            break;
                        }
                        -l_6_R.write(-l_11_R2, 0, -l_12_I);
                    }
                    -l_6_R.getChannel().force(true);
                    -l_6_R.flush();
                    fileOutputStream = -l_6_R;
                    if (-l_5_R != null) {
                        -l_5_R.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                }
            }
            str2 = -l_3_R.getFilesDir().toString();
            -l_4_R2 = new File(str2);
            if (!-l_4_R2.exists()) {
                str3 = str2 + File.separator + str;
                inputStream = null;
                fileOutputStream = null;
                -l_7_R = new File(str3);
                -l_8_I = -l_7_R.exists();
                -l_9_I = UpdateConfig.isUpdatableAssetFile(str);
                -l_10_I = 0;
                if (str.equals(UpdateConfig.VIRUS_BASE_NAME)) {
                    if (str.equals(UpdateConfig.VIRUS_BASE_EN_NAME)) {
                        -l_11_R = (String) UpdateConfig.sDeprecatedNameMap.get(str);
                        if (-l_11_R != null) {
                            -l_12_R = str3.substring(0, str3.lastIndexOf(File.separator) + 1) + -l_11_R;
                            -l_13_R = new File(-l_12_R);
                            -l_14_R = new File(-l_12_R + UpdateConfig.PATCH_SUFIX);
                            if (-l_13_R.exists()) {
                                -l_13_R.delete();
                            }
                            if (-l_14_R.exists()) {
                                -l_14_R.delete();
                            }
                        }
                        if (-l_8_I != 0) {
                            if (str.equals("MToken.zip")) {
                                if (str.equals(UpdateConfig.VIRUS_BASE_NAME)) {
                                    if (!str.equals(UpdateConfig.LOCATION_NAME)) {
                                    }
                                    if (-l_9_I == 0) {
                                    }
                                    if (-l_9_I != 0) {
                                        if (-l_5_R != null) {
                                            -l_5_R.close();
                                        }
                                        if (fileOutputStream != null) {
                                            fileOutputStream.close();
                                        }
                                    }
                                }
                            }
                        }
                        d(-l_7_R);
                        inputStream = -l_3_R.getResources().getAssets().open(str, 1);
                        -l_6_R = new FileOutputStream(-l_7_R);
                        -l_11_R2 = new byte[8192];
                        while (true) {
                            -l_12_I = inputStream.read(-l_11_R2);
                            if (-l_12_I <= 0) {
                                break;
                            }
                            -l_6_R.write(-l_11_R2, 0, -l_12_I);
                        }
                        -l_6_R.getChannel().force(true);
                        -l_6_R.flush();
                        fileOutputStream = -l_6_R;
                        if (-l_5_R != null) {
                            -l_5_R.close();
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                    }
                }
                if (eE()) {
                    -l_10_I = 1;
                }
                -l_11_R = (String) UpdateConfig.sDeprecatedNameMap.get(str);
                if (-l_11_R != null) {
                    -l_12_R = str3.substring(0, str3.lastIndexOf(File.separator) + 1) + -l_11_R;
                    -l_13_R = new File(-l_12_R);
                    -l_14_R = new File(-l_12_R + UpdateConfig.PATCH_SUFIX);
                    if (-l_13_R.exists()) {
                        -l_13_R.delete();
                    }
                    if (-l_14_R.exists()) {
                        -l_14_R.delete();
                    }
                }
                if (-l_8_I != 0) {
                    if (str.equals("MToken.zip")) {
                        if (str.equals(UpdateConfig.VIRUS_BASE_NAME)) {
                            if (!str.equals(UpdateConfig.LOCATION_NAME)) {
                            }
                            if (-l_9_I == 0) {
                            }
                            if (-l_9_I != 0) {
                                if (-l_5_R != null) {
                                    -l_5_R.close();
                                }
                                if (fileOutputStream != null) {
                                    fileOutputStream.close();
                                }
                            }
                        }
                    }
                }
                d(-l_7_R);
                inputStream = -l_3_R.getResources().getAssets().open(str, 1);
                -l_6_R = new FileOutputStream(-l_7_R);
                -l_11_R2 = new byte[8192];
                while (true) {
                    -l_12_I = inputStream.read(-l_11_R2);
                    if (-l_12_I <= 0) {
                        break;
                    }
                    -l_6_R.write(-l_11_R2, 0, -l_12_I);
                }
                -l_6_R.getChannel().force(true);
                -l_6_R.flush();
                fileOutputStream = -l_6_R;
                if (-l_5_R != null) {
                    -l_5_R.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
            -l_4_R2.mkdirs();
            str3 = str2 + File.separator + str;
            inputStream = null;
            fileOutputStream = null;
            try {
                -l_7_R = new File(str3);
                -l_8_I = -l_7_R.exists();
                -l_9_I = UpdateConfig.isUpdatableAssetFile(str);
                -l_10_I = 0;
                if (str.equals(UpdateConfig.VIRUS_BASE_NAME)) {
                    if (str.equals(UpdateConfig.VIRUS_BASE_EN_NAME)) {
                        -l_11_R = (String) UpdateConfig.sDeprecatedNameMap.get(str);
                        if (-l_11_R != null) {
                            -l_12_R = str3.substring(0, str3.lastIndexOf(File.separator) + 1) + -l_11_R;
                            -l_13_R = new File(-l_12_R);
                            -l_14_R = new File(-l_12_R + UpdateConfig.PATCH_SUFIX);
                            if (-l_13_R.exists()) {
                                -l_13_R.delete();
                            }
                            if (-l_14_R.exists()) {
                                -l_14_R.delete();
                            }
                        }
                        if (-l_8_I != 0) {
                            if (str.equals("MToken.zip")) {
                                if (str.equals(UpdateConfig.VIRUS_BASE_NAME)) {
                                    if (!str.equals(UpdateConfig.LOCATION_NAME)) {
                                    }
                                    if (-l_9_I == 0) {
                                    }
                                    if (-l_9_I != 0) {
                                        if (-l_5_R != null) {
                                            -l_5_R.close();
                                        }
                                        if (fileOutputStream != null) {
                                            fileOutputStream.close();
                                        }
                                    }
                                }
                            }
                        }
                        d(-l_7_R);
                        inputStream = -l_3_R.getResources().getAssets().open(str, 1);
                        -l_6_R = new FileOutputStream(-l_7_R);
                        -l_11_R2 = new byte[8192];
                        while (true) {
                            -l_12_I = inputStream.read(-l_11_R2);
                            if (-l_12_I <= 0) {
                                break;
                            }
                            -l_6_R.write(-l_11_R2, 0, -l_12_I);
                        }
                        -l_6_R.getChannel().force(true);
                        -l_6_R.flush();
                        fileOutputStream = -l_6_R;
                        if (-l_5_R != null) {
                            -l_5_R.close();
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                    }
                }
                if (eE()) {
                    -l_10_I = 1;
                }
                -l_11_R = (String) UpdateConfig.sDeprecatedNameMap.get(str);
                if (-l_11_R != null) {
                    -l_12_R = str3.substring(0, str3.lastIndexOf(File.separator) + 1) + -l_11_R;
                    -l_13_R = new File(-l_12_R);
                    -l_14_R = new File(-l_12_R + UpdateConfig.PATCH_SUFIX);
                    if (-l_13_R.exists()) {
                        -l_13_R.delete();
                    }
                    if (-l_14_R.exists()) {
                        -l_14_R.delete();
                    }
                }
                if (-l_8_I != 0) {
                    if (str.equals("MToken.zip")) {
                        if (str.equals(UpdateConfig.VIRUS_BASE_NAME)) {
                            if (!str.equals(UpdateConfig.LOCATION_NAME)) {
                            }
                            if (-l_9_I == 0) {
                            }
                            if (-l_9_I != 0) {
                                if (-l_5_R != null) {
                                    -l_5_R.close();
                                }
                                if (fileOutputStream != null) {
                                    fileOutputStream.close();
                                }
                            }
                        }
                    }
                }
                d(-l_7_R);
                inputStream = -l_3_R.getResources().getAssets().open(str, 1);
                -l_6_R = new FileOutputStream(-l_7_R);
                try {
                    -l_11_R2 = new byte[8192];
                    while (true) {
                        -l_12_I = inputStream.read(-l_11_R2);
                        if (-l_12_I <= 0) {
                            break;
                        }
                        -l_6_R.write(-l_11_R2, 0, -l_12_I);
                    }
                    -l_6_R.getChannel().force(true);
                    -l_6_R.flush();
                    fileOutputStream = -l_6_R;
                    if (-l_5_R != null) {
                        -l_5_R.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e3) {
                    fileOutputStream = -l_6_R;
                } catch (Throwable th) {
                    -l_15_R = th;
                    fileOutputStream = -l_6_R;
                }
            } catch (IOException e4) {
                try {
                    f.e("getCommonFilePath", "getCommonFilePath error");
                    Object -l_8_R = "";
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e5) {
                        }
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e6) {
                        }
                    }
                    return -l_8_R;
                } catch (Throwable th2) {
                    Object -l_15_R;
                    -l_15_R = th2;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e7) {
                        }
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e8) {
                        }
                    }
                    throw -l_15_R;
                }
            }
        }
        return str3;
    }

    public static boolean bK(String str) {
        Object -l_1_R = new File(str);
        if (-l_1_R.exists()) {
            return !-l_1_R.isFile() ? bL(str) : deleteFile(str);
        } else {
            return false;
        }
    }

    public static boolean bL(String str) {
        if (!str.endsWith(File.separator)) {
            str = str + File.separator;
        }
        Object -l_1_R = new File(str);
        if (!-l_1_R.exists() || !-l_1_R.isDirectory()) {
            return false;
        }
        int -l_2_I = 1;
        Object -l_3_R = -l_1_R.listFiles();
        for (int -l_4_I = 0; -l_4_I < -l_3_R.length; -l_4_I++) {
            if (!-l_3_R[-l_4_I].isFile()) {
                -l_2_I = bL(-l_3_R[-l_4_I].getAbsolutePath());
                if (-l_2_I == 0) {
                    break;
                }
            } else {
                -l_2_I = deleteFile(-l_3_R[-l_4_I].getAbsolutePath());
                if (-l_2_I == 0) {
                    break;
                }
            }
        }
        return -l_2_I != 0 && -l_1_R.delete();
    }

    public static boolean bM(String str) {
        return new File(str).exists();
    }

    public static String bN(String str) {
        Object -l_8_R;
        BufferedInputStream bufferedInputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        Object -l_3_R;
        try {
            ByteArrayOutputStream -l_2_R;
            BufferedInputStream -l_1_R = new BufferedInputStream(new FileInputStream(str));
            try {
                -l_2_R = new ByteArrayOutputStream();
            } catch (FileNotFoundException e) {
                -l_3_R = e;
                bufferedInputStream = -l_1_R;
                try {
                    -l_3_R.printStackTrace();
                    if (byteArrayOutputStream != null) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Object -l_3_R2) {
                            -l_3_R2.printStackTrace();
                        }
                    }
                    if (bufferedInputStream != null) {
                        try {
                            bufferedInputStream.close();
                        } catch (Object -l_3_R22) {
                            -l_3_R22.printStackTrace();
                        }
                    }
                    return "";
                } catch (Throwable th) {
                    -l_8_R = th;
                    if (byteArrayOutputStream != null) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Object -l_9_R) {
                            -l_9_R.printStackTrace();
                        }
                    }
                    if (bufferedInputStream != null) {
                        try {
                            bufferedInputStream.close();
                        } catch (Object -l_9_R2) {
                            -l_9_R2.printStackTrace();
                        }
                    }
                    throw -l_8_R;
                }
            } catch (IOException e2) {
                -l_3_R22 = e2;
                bufferedInputStream = -l_1_R;
                -l_3_R22.printStackTrace();
                if (byteArrayOutputStream != null) {
                    try {
                        byteArrayOutputStream.close();
                    } catch (Object -l_3_R222) {
                        -l_3_R222.printStackTrace();
                    }
                }
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (Object -l_3_R2222) {
                        -l_3_R2222.printStackTrace();
                    }
                }
                return "";
            } catch (Throwable th2) {
                -l_8_R = th2;
                bufferedInputStream = -l_1_R;
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                throw -l_8_R;
            }
            try {
                -l_3_R2222 = new byte[IncomingSmsFilterConsts.PAY_SMS];
                while (true) {
                    int -l_4_I = -l_1_R.read(-l_3_R2222);
                    if (-l_4_I == -1) {
                        break;
                    }
                    -l_2_R.write(-l_3_R2222, 0, -l_4_I);
                }
                Object -l_6_R = new String(-l_2_R.toByteArray());
                if (-l_2_R == null) {
                    byteArrayOutputStream = -l_2_R;
                } else {
                    try {
                        -l_2_R.close();
                    } catch (Object -l_7_R) {
                        -l_7_R.printStackTrace();
                    }
                }
                if (-l_1_R == null) {
                    bufferedInputStream = -l_1_R;
                } else {
                    try {
                        -l_1_R.close();
                    } catch (Object -l_7_R2) {
                        -l_7_R2.printStackTrace();
                    }
                }
                return -l_6_R;
            } catch (FileNotFoundException e3) {
                -l_3_R2222 = e3;
                byteArrayOutputStream = -l_2_R;
                bufferedInputStream = -l_1_R;
                -l_3_R2222.printStackTrace();
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                return "";
            } catch (IOException e4) {
                -l_3_R2222 = e4;
                byteArrayOutputStream = -l_2_R;
                bufferedInputStream = -l_1_R;
                -l_3_R2222.printStackTrace();
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                return "";
            } catch (Throwable th3) {
                -l_8_R = th3;
                byteArrayOutputStream = -l_2_R;
                bufferedInputStream = -l_1_R;
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                throw -l_8_R;
            }
        } catch (FileNotFoundException e5) {
            -l_3_R2222 = e5;
            -l_3_R2222.printStackTrace();
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            return "";
        } catch (IOException e6) {
            -l_3_R2222 = e6;
            -l_3_R2222.printStackTrace();
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            return "";
        }
    }

    public static boolean cL() {
        Object -l_0_R = Environment.getExternalStorageDirectory().toString() + "/DCIM";
        Object -l_1_R = new File(-l_0_R);
        if (!-l_1_R.isDirectory() && !-l_1_R.mkdirs()) {
            return false;
        }
        Object -l_2_R = new File(-l_0_R, ".probe");
        try {
            if (-l_2_R.exists()) {
                -l_2_R.delete();
            }
            if (!-l_2_R.createNewFile()) {
                return false;
            }
            -l_2_R.delete();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void d(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean deleteFile(String str) {
        try {
            Object -l_1_R = new File(str);
            return -l_1_R.isFile() ? -l_1_R.delete() : false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean e(Context -l_7_R, String str) {
        Object -l_9_R;
        Object -l_10_R;
        Object -l_3_R = new File(TMSDKContext.getApplicaionContext().getFilesDir() + File.separator + str);
        if (!-l_3_R.exists()) {
            return true;
        }
        InputStream inputStream = null;
        int -l_6_I = 0;
        try {
            Context -l_7_R2 = TMSDKContext.getCurrentContext();
            if (-l_7_R2 != null) {
                -l_7_R = -l_7_R2;
            }
            Object -l_4_R = -l_7_R.getAssets().open(str, 1);
            -l_6_I = lt.c(-l_4_R).yT;
            if (-l_4_R != null) {
                try {
                    -l_4_R.close();
                } catch (Context -l_7_R3) {
                    -l_7_R3.printStackTrace();
                }
            }
        } catch (Context -l_7_R32) {
            -l_7_R32.printStackTrace();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Context -l_7_R322) {
                    -l_7_R322.printStackTrace();
                }
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Object -l_9_R2) {
                    -l_9_R2.printStackTrace();
                }
            }
        }
        if (-l_6_I == 0) {
            return false;
        }
        FileInputStream fileInputStream = null;
        int -l_8_I = 0;
        try {
            FileInputStream -l_7_R4 = new FileInputStream(-l_3_R);
            try {
                -l_8_I = lt.c(-l_7_R4).yT;
                if (-l_7_R4 == null) {
                    fileInputStream = -l_7_R4;
                    return -l_6_I <= -l_8_I;
                }
                try {
                    -l_7_R4.close();
                } catch (Object -l_9_R22) {
                    -l_9_R22.printStackTrace();
                }
                if (-l_6_I <= -l_8_I) {
                }
                return -l_6_I <= -l_8_I;
            } catch (Exception e) {
                -l_9_R22 = e;
                fileInputStream = -l_7_R4;
                try {
                    -l_9_R22.printStackTrace();
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_9_R222) {
                            -l_9_R222.printStackTrace();
                        }
                    }
                    if (-l_6_I <= -l_8_I) {
                    }
                    return -l_6_I <= -l_8_I;
                } catch (Throwable th2) {
                    -l_10_R = th2;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_11_R) {
                            -l_11_R.printStackTrace();
                        }
                    }
                    throw -l_10_R;
                }
            } catch (Throwable th3) {
                -l_10_R = th3;
                fileInputStream = -l_7_R4;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw -l_10_R;
            }
        } catch (Exception e2) {
            -l_9_R222 = e2;
            -l_9_R222.printStackTrace();
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (-l_6_I <= -l_8_I) {
            }
            return -l_6_I <= -l_8_I;
        }
    }

    public static String[] e(File file) {
        Object -l_3_R;
        Object -l_7_R;
        BufferedInputStream bufferedInputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            ByteArrayOutputStream -l_2_R;
            BufferedInputStream -l_1_R = new BufferedInputStream(new FileInputStream(file));
            try {
                -l_2_R = new ByteArrayOutputStream();
            } catch (FileNotFoundException e) {
                -l_3_R = e;
                bufferedInputStream = -l_1_R;
                try {
                    -l_3_R.printStackTrace();
                    if (bufferedInputStream != null) {
                        try {
                            bufferedInputStream.close();
                        } catch (Object -l_3_R2) {
                            -l_3_R2.printStackTrace();
                        }
                    }
                    if (byteArrayOutputStream != null) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Object -l_3_R22) {
                            -l_3_R22.printStackTrace();
                        }
                    }
                    return null;
                } catch (Throwable th) {
                    -l_7_R = th;
                    if (bufferedInputStream != null) {
                        try {
                            bufferedInputStream.close();
                        } catch (Object -l_8_R) {
                            -l_8_R.printStackTrace();
                        }
                    }
                    if (byteArrayOutputStream != null) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Object -l_8_R2) {
                            -l_8_R2.printStackTrace();
                        }
                    }
                    throw -l_7_R;
                }
            } catch (IOException e2) {
                -l_3_R22 = e2;
                bufferedInputStream = -l_1_R;
                -l_3_R22.printStackTrace();
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (Object -l_3_R222) {
                        -l_3_R222.printStackTrace();
                    }
                }
                if (byteArrayOutputStream != null) {
                    try {
                        byteArrayOutputStream.close();
                    } catch (Object -l_3_R2222) {
                        -l_3_R2222.printStackTrace();
                    }
                }
                return null;
            } catch (Throwable th2) {
                -l_7_R = th2;
                bufferedInputStream = -l_1_R;
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                throw -l_7_R;
            }
            try {
                -l_3_R2222 = new byte[IncomingSmsFilterConsts.PAY_SMS];
                while (true) {
                    int -l_4_I = -l_1_R.read(-l_3_R2222);
                    if (-l_4_I == -1) {
                        break;
                    }
                    -l_2_R.write(-l_3_R2222, 0, -l_4_I);
                }
                Object -l_5_R = new String(-l_2_R.toByteArray()).split("\\n");
                if (-l_1_R == null) {
                    bufferedInputStream = -l_1_R;
                } else {
                    try {
                        -l_1_R.close();
                    } catch (Object -l_6_R) {
                        -l_6_R.printStackTrace();
                    }
                }
                if (-l_2_R == null) {
                    byteArrayOutputStream = -l_2_R;
                } else {
                    try {
                        -l_2_R.close();
                    } catch (Object -l_6_R2) {
                        -l_6_R2.printStackTrace();
                    }
                }
                return -l_5_R;
            } catch (FileNotFoundException e3) {
                -l_3_R2222 = e3;
                byteArrayOutputStream = -l_2_R;
                bufferedInputStream = -l_1_R;
                -l_3_R2222.printStackTrace();
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                return null;
            } catch (IOException e4) {
                -l_3_R2222 = e4;
                byteArrayOutputStream = -l_2_R;
                bufferedInputStream = -l_1_R;
                -l_3_R2222.printStackTrace();
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                return null;
            } catch (Throwable th3) {
                -l_7_R = th3;
                byteArrayOutputStream = -l_2_R;
                bufferedInputStream = -l_1_R;
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                throw -l_7_R;
            }
        } catch (FileNotFoundException e5) {
            -l_3_R2222 = e5;
            -l_3_R2222.printStackTrace();
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            return null;
        } catch (IOException e6) {
            -l_3_R2222 = e6;
            -l_3_R2222.printStackTrace();
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            return null;
        }
    }

    private static boolean eE() {
        Object -l_1_R = new md("tms");
        String -l_3_R = "6.1.0";
        if (-l_1_R.getString("soft_version", "").equals(-l_3_R)) {
            return false;
        }
        -l_1_R.a("soft_version", -l_3_R, true);
        return true;
    }

    public static boolean eF() {
        try {
            Object -l_0_R = Environment.getExternalStorageState();
            return -l_0_R != null ? -l_0_R.equals("mounted") : false;
        } catch (Exception e) {
            return false;
        }
    }

    public static String eG() {
        return !Environment.getExternalStorageState().equals("mounted") ? "/sdcard" : Environment.getExternalStorageDirectory().getPath();
    }

    public static final String p(String str, String -l_2_R) {
        String -l_2_R2 = null;
        if (null == null) {
            Object -l_3_R = Uri.decode(str);
            if (-l_3_R != null) {
                int -l_4_I = -l_3_R.indexOf(63);
                if (-l_4_I > 0) {
                    -l_3_R = -l_3_R.substring(0, -l_4_I);
                }
                if (!-l_3_R.endsWith("/")) {
                    int -l_5_I = -l_3_R.lastIndexOf(47) + 1;
                    if (-l_5_I > 0) {
                        -l_2_R2 = -l_3_R.substring(-l_5_I);
                    }
                }
            }
        }
        if (-l_2_R2 != null) {
            -l_2_R = -l_2_R2;
        }
        return -l_2_R != null ? -l_2_R : "downloadfile";
    }

    public static boolean q(String str, String str2) {
        FileOutputStream -l_4_R;
        Object -l_8_R;
        if (str == null || str.length() == 0) {
            return false;
        }
        Object -l_2_R = new File(str);
        if (!-l_2_R.exists() || !-l_2_R.canRead()) {
            return false;
        }
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        Object -l_5_R;
        try {
            -l_5_R = new File(str2);
            d(-l_5_R);
            InputStream -l_3_R = new FileInputStream(-l_2_R);
            try {
                -l_4_R = new FileOutputStream(-l_5_R);
            } catch (IOException e) {
                -l_5_R = e;
                inputStream = -l_3_R;
                try {
                    -l_5_R.printStackTrace();
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e2) {
                        }
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e3) {
                        }
                    }
                    return false;
                } catch (Throwable th) {
                    -l_8_R = th;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e4) {
                        }
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw -l_8_R;
                }
            } catch (Throwable th2) {
                -l_8_R = th2;
                inputStream = -l_3_R;
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                throw -l_8_R;
            }
            try {
                Object -l_6_R = new byte[8192];
                while (true) {
                    try {
                        int -l_7_I = -l_3_R.read(-l_6_R);
                        if (-l_7_I <= 0) {
                            break;
                        }
                        -l_4_R.write(-l_6_R, 0, -l_7_I);
                    } catch (IOException e6) {
                        -l_5_R = e6;
                        fileOutputStream = -l_4_R;
                        inputStream = -l_3_R;
                    } catch (Throwable th3) {
                        -l_8_R = th3;
                        fileOutputStream = -l_4_R;
                        inputStream = -l_3_R;
                    }
                }
                -l_4_R.flush();
                if (-l_3_R != null) {
                    try {
                        -l_3_R.close();
                    } catch (IOException e7) {
                    }
                }
                if (-l_4_R != null) {
                    try {
                        -l_4_R.close();
                    } catch (IOException e8) {
                    }
                }
                return true;
            } catch (IOException e9) {
                -l_5_R = e9;
                fileOutputStream = -l_4_R;
                inputStream = -l_3_R;
                -l_5_R.printStackTrace();
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                return false;
            } catch (Throwable th4) {
                -l_8_R = th4;
                fileOutputStream = -l_4_R;
                inputStream = -l_3_R;
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                throw -l_8_R;
            }
        } catch (IOException e10) {
            -l_5_R = e10;
            -l_5_R.printStackTrace();
            if (inputStream != null) {
                inputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            return false;
        }
    }

    private static boolean r(Context -l_1_R) {
        Object -l_5_R;
        int -l_6_I;
        InputStream -l_3_R;
        Object -l_7_R;
        Context -l_1_R2 = TMSDKContext.getCurrentContext();
        if (-l_1_R2 != null) {
            -l_1_R = -l_1_R2;
        }
        InputStream inputStream = null;
        int -l_4_I = 0;
        try {
            inputStream = -l_1_R.getAssets().open(UpdateConfig.LOCATION_NAME, 1);
            -l_5_R = new byte[8];
            inputStream.read(-l_5_R);
            -l_4_I = (((-l_5_R[4] & 255) | ((-l_5_R[5] & 255) << 8)) | ((-l_5_R[6] & 255) << 16)) | ((-l_5_R[7] & 255) << 24);
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    -l_6_I = 0;
                    -l_3_R = new FileInputStream(-l_1_R.getFilesDir().toString() + File.separator + UpdateConfig.LOCATION_NAME);
                    -l_7_R = new byte[8];
                    try {
                        -l_3_R.read(-l_7_R);
                        -l_6_I = (((-l_7_R[4] & 255) | ((-l_7_R[5] & 255) << 8)) | ((-l_7_R[6] & 255) << 16)) | ((-l_7_R[7] & 255) << 24);
                        if (-l_3_R != null) {
                            try {
                                -l_3_R.close();
                            } catch (IOException e2) {
                            }
                        }
                        inputStream = -l_3_R;
                    } catch (Exception e3) {
                        -l_7_R = e3;
                        inputStream = -l_3_R;
                        try {
                            -l_7_R.printStackTrace();
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e4) {
                                }
                            }
                            return -l_4_I > -l_6_I;
                        } catch (Throwable th) {
                            Object -l_8_R = th;
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e5) {
                                }
                            }
                            throw -l_8_R;
                        }
                    } catch (Throwable th2) {
                        -l_8_R = th2;
                        inputStream = -l_3_R;
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        throw -l_8_R;
                    }
                    if (-l_4_I > -l_6_I) {
                    }
                }
            }
        } catch (Object -l_5_R2) {
            -l_5_R2.printStackTrace();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e6) {
                    -l_6_I = 0;
                    -l_3_R = new FileInputStream(-l_1_R.getFilesDir().toString() + File.separator + UpdateConfig.LOCATION_NAME);
                    -l_7_R = new byte[8];
                    -l_3_R.read(-l_7_R);
                    -l_6_I = (((-l_7_R[4] & 255) | ((-l_7_R[5] & 255) << 8)) | ((-l_7_R[6] & 255) << 16)) | ((-l_7_R[7] & 255) << 24);
                    if (-l_3_R != null) {
                        -l_3_R.close();
                    }
                    inputStream = -l_3_R;
                    if (-l_4_I > -l_6_I) {
                    }
                }
            }
        } catch (Throwable th3) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e7) {
                }
            }
        }
        -l_6_I = 0;
        try {
            -l_3_R = new FileInputStream(-l_1_R.getFilesDir().toString() + File.separator + UpdateConfig.LOCATION_NAME);
            try {
                -l_7_R = new byte[8];
                -l_3_R.read(-l_7_R);
                -l_6_I = (((-l_7_R[4] & 255) | ((-l_7_R[5] & 255) << 8)) | ((-l_7_R[6] & 255) << 16)) | ((-l_7_R[7] & 255) << 24);
                if (-l_3_R != null) {
                    -l_3_R.close();
                }
                inputStream = -l_3_R;
            } catch (Exception e8) {
                -l_7_R = e8;
                inputStream = -l_3_R;
                -l_7_R.printStackTrace();
                if (inputStream != null) {
                    inputStream.close();
                }
                if (-l_4_I > -l_6_I) {
                }
            } catch (Throwable th4) {
                -l_8_R = th4;
                inputStream = -l_3_R;
                if (inputStream != null) {
                    inputStream.close();
                }
                throw -l_8_R;
            }
        } catch (Exception e9) {
            -l_7_R = e9;
            -l_7_R.printStackTrace();
            if (inputStream != null) {
                inputStream.close();
            }
            if (-l_4_I > -l_6_I) {
            }
        }
        if (-l_4_I > -l_6_I) {
        }
    }

    public static List<String> s(Context context) {
        StorageManager -l_1_R = (StorageManager) context.getSystemService("storage");
        Object -l_2_R = new ArrayList();
        Object -l_3_R;
        try {
            -l_3_R = (Object[]) -l_1_R.getClass().getMethod("getVolumeList", new Class[0]).invoke(-l_1_R, new Object[0]);
            if (-l_3_R != null && -l_3_R.length > 0) {
                Object -l_4_R = -l_3_R[0].getClass().getDeclaredMethod("getPath", new Class[0]);
                Object -l_5_R = -l_1_R.getClass().getMethod("getVolumeState", new Class[]{String.class});
                Object -l_6_R = -l_3_R;
                for (Object -l_9_R : -l_3_R) {
                    String -l_10_R = (String) -l_4_R.invoke(-l_9_R, new Object[0]);
                    if (-l_10_R != null) {
                        if ("mounted".equals(-l_5_R.invoke(-l_1_R, new Object[]{-l_10_R}))) {
                            -l_2_R.add(-l_10_R);
                        }
                    }
                }
            }
        } catch (Object -l_3_R2) {
            -l_3_R2.printStackTrace();
        }
        return -l_2_R;
    }

    public static int t(long j) {
        int i = 1;
        if (!eF()) {
            return 1;
        }
        if (!cL()) {
            return 2;
        }
        Object -l_2_R = new a();
        l.a(-l_2_R);
        if (-l_2_R.LM < j) {
            i = 0;
        }
        return i == 0 ? 3 : 0;
    }
}
