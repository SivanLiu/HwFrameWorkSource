package tmsdk.common.utils;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import tmsdk.common.tcc.TccCryptor;
import tmsdkobf.fn;
import tmsdkobf.lq;
import tmsdkobf.ls;
import tmsdkobf.lt;
import tmsdkobf.mc;

public abstract class c {
    protected final String LA;
    private final String LB;
    private final String LC;
    public ls Lz;
    private Context mContext;

    public c(Context context, String str) {
        this.LA = str;
        this.LC = context.getFilesDir().getAbsolutePath();
        this.LB = context.getCacheDir().getAbsolutePath();
        this.mContext = context;
    }

    protected fn a(Context context, String str, String str2, boolean z) {
        FileInputStream fileInputStream;
        Object -l_12_R;
        Object -l_5_R = new fn();
        Object -l_6_R = new File(str);
        if (!-l_6_R.exists()) {
            return -l_5_R;
        }
        fileInputStream = null;
        try {
            FileInputStream -l_7_R = new FileInputStream(-l_6_R);
            try {
                -l_8_R = lt.c(-l_7_R);
                this.Lz = -l_8_R;
                Object -l_9_R = new byte[-l_7_R.available()];
                -l_7_R.read(-l_9_R);
                Object obj;
                if (z && !lq.bytesToHexString(mc.l(-l_9_R)).equals(lq.bytesToHexString(-l_8_R.yU))) {
                    obj = -l_5_R;
                    if (-l_7_R == null) {
                        fileInputStream = -l_7_R;
                    } else {
                        try {
                            -l_7_R.close();
                        } catch (Object -l_11_R) {
                            -l_11_R.printStackTrace();
                        }
                    }
                    return -l_5_R;
                }
                obj = TccCryptor.decrypt(-l_9_R, null);
                if (str2 != null) {
                    if (str2.length() > 0) {
                        -l_5_R.B(str2);
                    }
                }
                -l_5_R.b(obj);
                if (-l_7_R == null) {
                    fileInputStream = -l_7_R;
                    return -l_5_R;
                }
                try {
                    -l_7_R.close();
                } catch (Object -l_8_R) {
                    -l_8_R.printStackTrace();
                }
                return -l_5_R;
            } catch (Exception e) {
                -l_8_R = e;
                fileInputStream = -l_7_R;
                try {
                    Object -l_8_R2;
                    -l_8_R2.printStackTrace();
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_8_R22) {
                            -l_8_R22.printStackTrace();
                        }
                    }
                    return -l_5_R;
                } catch (Throwable th) {
                    -l_12_R = th;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_13_R) {
                            -l_13_R.printStackTrace();
                        }
                    }
                    throw -l_12_R;
                }
            } catch (Throwable th2) {
                -l_12_R = th2;
                fileInputStream = -l_7_R;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw -l_12_R;
            }
        } catch (Exception e2) {
            -l_8_R22 = e2;
            -l_8_R22.printStackTrace();
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return -l_5_R;
        }
    }

    public String iy() {
        Object -l_1_R = new StringBuffer();
        -l_1_R.append(this.LC).append(File.separator).append(this.LA).append(".dat");
        return -l_1_R.toString();
    }
}
