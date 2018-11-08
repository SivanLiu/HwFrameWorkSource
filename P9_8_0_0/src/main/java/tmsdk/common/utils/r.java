package tmsdk.common.utils;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.numbermarker.NumMarkerManager;
import tmsdk.common.module.qscanner.impl.AmScannerV2;
import tmsdk.common.module.update.UpdateConfig;
import tmsdkobf.ad;
import tmsdkobf.fd;
import tmsdkobf.lq;
import tmsdkobf.lu;

public class r {
    public static String a(Context context, long j) {
        Object -l_3_R = context.getFilesDir().getAbsolutePath();
        Object -l_4_R = UpdateConfig.getFileNameIdByFlag(j);
        if (-l_4_R == null) {
            return null;
        }
        Object -l_5_R = -l_3_R + File.separator + -l_4_R;
        if (!new File(-l_5_R).exists()) {
            lu.b(context, -l_4_R, -l_3_R);
        }
        return -l_5_R;
    }

    public static String b(Context context, int i, String str) {
        if (str == null || context == null) {
            return null;
        }
        Object -l_3_R = context.getFilesDir().getAbsolutePath();
        Object -l_4_R = Integer.toString(i) + str;
        Object -l_5_R = -l_3_R + File.separator + -l_4_R;
        if (!new File(-l_5_R).exists()) {
            lu.b(context, -l_4_R, -l_3_R);
        }
        return -l_5_R;
    }

    public static ad b(Context context, long j) {
        if (j == UpdateConfig.UPDATE_FLAG_VIRUS_BASE) {
            return i(context, a(context, j));
        }
        if (j == 2) {
            return m(50001, b(context, 50001, ".sdb"));
        }
        if (j != UpdateConfig.UPDATE_FLAG_YELLOW_PAGEV2_Large) {
            return cM(a(context, j));
        }
        Object -l_3_R = n(40461, b(context, 40461, ".sdb"));
        if (-l_3_R == null) {
            return -l_3_R;
        }
        f.f("gjj", "fileId:" + -l_3_R.aE + " timestamp:" + -l_3_R.timestamp + " pfutimestamp:" + -l_3_R.aG + " version:" + -l_3_R.version);
        return -l_3_R;
    }

    private static byte[] b(String str, int i, int i2) throws IOException {
        Object -l_3_R = new byte[i2];
        Object -l_4_R = new RandomAccessFile(str, "r");
        try {
            -l_4_R.skipBytes(i);
            -l_4_R.read(-l_3_R);
            return -l_3_R;
        } finally {
            -l_4_R.close();
        }
    }

    public static ad cM(String str) {
        Object -l_2_R;
        try {
            Object -l_1_R = b(str, 0, 24);
            -l_2_R = new ad();
            Object -l_3_R = new byte[4];
            System.arraycopy(-l_1_R, 4, -l_3_R, 0, 4);
            -l_2_R.timestamp = lq.k(-l_3_R);
            Object -l_4_R = new byte[16];
            System.arraycopy(-l_1_R, 8, -l_4_R, 0, 16);
            -l_2_R.aF = -l_4_R;
            -l_2_R.aE = cN(new File(str).getName());
            return -l_2_R;
        } catch (Object -l_2_R2) {
            f.f("UpdateManager", -l_2_R2.getMessage());
            return null;
        }
    }

    private static int cN(String str) {
        try {
            return Integer.parseInt(str.substring(0, str.lastIndexOf(".")));
        } catch (Object -l_1_R) {
            f.e("UpdateManager", "fileName: " + str + " e: " + -l_1_R.getMessage());
            return 0;
        }
    }

    public static ad i(Context context, String str) {
        Object -l_3_R = new File(str);
        if (!-l_3_R.exists()) {
            return null;
        }
        Object -l_4_R = AmScannerV2.g(context, str);
        if (-l_4_R == null) {
            return null;
        }
        Object -l_2_R = new ad();
        -l_2_R.aE = UpdateConfig.getFileIdByFileName(-l_3_R.getName());
        -l_2_R.timestamp = -l_4_R.timestamp;
        -l_2_R.version = -l_4_R.version;
        return -l_2_R;
    }

    public static fd j(Context context, String str) {
        return !new File(str).exists() ? null : AmScannerV2.g(context, str);
    }

    public static String k(Context context, String str) {
        if (str == null || context == null) {
            return null;
        }
        Object -l_3_R = context.getFilesDir().getAbsolutePath() + File.separator + str;
        return new File(-l_3_R).exists() ? -l_3_R : null;
    }

    public static ad m(int i, String str) {
        Object -l_3_R;
        try {
            Object -l_2_R = b(str, 0, 48);
            -l_3_R = new ad();
            -l_3_R.version = -l_2_R[0];
            Object -l_4_R = new byte[4];
            System.arraycopy(-l_2_R, 4, -l_4_R, 0, 4);
            -l_3_R.timestamp = lq.k(-l_4_R);
            Object -l_5_R = new byte[16];
            System.arraycopy(-l_2_R, 8, -l_5_R, 0, 16);
            -l_3_R.aF = -l_5_R;
            Object -l_6_R = new byte[4];
            System.arraycopy(-l_2_R, 44, -l_6_R, 0, 4);
            -l_3_R.aG = lq.k(-l_6_R);
            -l_3_R.aE = i;
            return -l_3_R;
        } catch (Object -l_3_R2) {
            f.f("UpdateManager", -l_3_R2.getMessage());
            return null;
        }
    }

    public static ad n(int i, String str) {
        Object -l_2_R = new ad();
        Object -l_4_R;
        try {
            Object -l_3_R = b(str, 0, 24);
            -l_2_R.version = -l_3_R[0];
            -l_4_R = new byte[4];
            System.arraycopy(-l_3_R, 4, -l_4_R, 0, 4);
            -l_2_R.timestamp = lq.k(-l_4_R);
            Object -l_5_R = new byte[16];
            System.arraycopy(-l_3_R, 8, -l_5_R, 0, 16);
            -l_2_R.aF = -l_5_R;
            -l_2_R.aG = -l_2_R.timestamp;
            -l_2_R.aE = i;
            return -l_2_R;
        } catch (Object -l_4_R2) {
            f.f("UpdateManager", -l_4_R2.getMessage());
            -l_2_R.aE = i;
            -l_2_R.aF = new byte[0];
            -l_2_R.timestamp = 0;
            if (-l_2_R.aF == null) {
                -l_2_R.aF = lq.at("");
            }
            return -l_2_R;
        }
    }

    public static ad o(int i, String str) {
        Object -l_3_R = ((NumMarkerManager) ManagerCreatorC.getManager(NumMarkerManager.class)).getMarkFileInfo(i, str);
        if (-l_3_R == null) {
            return null;
        }
        Object -l_4_R = new ad();
        -l_4_R.version = -l_3_R.version;
        -l_4_R.timestamp = -l_3_R.timeStampSecondWhole;
        -l_4_R.aG = -l_3_R.timeStampSecondLastDiff == 0 ? -l_3_R.timeStampSecondWhole : -l_3_R.timeStampSecondLastDiff;
        -l_4_R.aF = lq.bJ(-l_3_R.md5 == null ? "" : -l_3_R.md5);
        -l_4_R.aE = i;
        return -l_4_R;
    }
}
