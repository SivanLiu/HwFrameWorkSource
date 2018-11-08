package tmsdk.common.module.qscanner.impl;

import android.content.Context;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.utils.f;
import tmsdkobf.fd;
import tmsdkobf.ff;
import tmsdkobf.fg;
import tmsdkobf.fn;
import tmsdkobf.lu;
import tmsdkobf.ma;

public class AmScannerV2 {
    private static boolean BP;
    private long object = 0;

    static {
        BP = false;
        try {
            BP = ma.f(TMSDKContext.getApplicaionContext(), "ams-1.2.9-mfr");
            if (!BP) {
                f.g("QScannerMgr-AmScannerV2", "load ams so failed");
            }
        } catch (Object -l_0_R) {
            f.b("QScannerMgr-AmScannerV2", "load ams so exception: " + -l_0_R, -l_0_R);
        }
    }

    protected AmScannerV2(Context context, String str) {
        this.object = newObject(context, str);
        f.d("QScannerMgr-AmScannerV2", "[native]newObject:[" + this.object + "]");
        if (this.object == 0) {
            throw new OutOfMemoryError();
        }
    }

    public static synchronized int a(Context context, String str, fg fgVar, List<ff> list) {
        synchronized (AmScannerV2.class) {
            if (fgVar == null || list == null) {
                f.g("QScannerMgr-AmScannerV2", "updateBase, virusServerInfo == null || virusInfoList == null");
                return -6;
            }
            Object -l_4_R = new fn();
            -l_4_R.B("UTF-8");
            -l_4_R.m();
            -l_4_R.put("vsi", fgVar);
            Object -l_5_R = -l_4_R.l();
            -l_4_R.k();
            -l_4_R.put("vil", list);
            Object -l_6_R = -l_4_R.l();
            f.d("QScannerMgr-AmScannerV2", "[native]nativeUpdateMalwareInfoBytes, amfFile:[" + str + "]");
            int nativeUpdateMalwareInfoBytes = nativeUpdateMalwareInfoBytes(context, str, -l_5_R, -l_6_R);
            return nativeUpdateMalwareInfoBytes;
        }
    }

    public static synchronized d b(a aVar) {
        synchronized (AmScannerV2.class) {
            if (aVar != null) {
                Object -l_1_R = new fn();
                -l_1_R.B("UTF-8");
                -l_1_R.m();
                -l_1_R.put("ak", aVar);
                Object -l_2_R = -l_1_R.l();
                Object -l_3_R = new AtomicReference();
                f.d("QScannerMgr-AmScannerV2", "[native]extractApkInfo");
                int -l_4_I = extractApkInfo(-l_2_R, -l_3_R);
                if (-l_4_I == 0) {
                    byte[] -l_5_R = (byte[]) -l_3_R.get();
                    if (-l_5_R != null) {
                        -l_1_R.k();
                        -l_1_R.b(-l_5_R);
                        d -l_6_R = (d) -l_1_R.a("qsr", new d());
                        return -l_6_R;
                    }
                    f.g("QScannerMgr-AmScannerV2", "extractApkInfo(), return null");
                    return null;
                }
                f.g("QScannerMgr-AmScannerV2", "extractApkInfo(), err: " + -l_4_I);
                return null;
            }
            f.g("QScannerMgr-AmScannerV2", "extractApkInfo, apkKey == null!");
            return null;
        }
    }

    private static native void deleteObject(long j);

    private static native int extractApkInfo(byte[] bArr, AtomicReference<byte[]> atomicReference);

    public static synchronized fd g(Context context, String str) {
        synchronized (AmScannerV2.class) {
            if (str != null) {
                Object -l_2_R = new fn();
                -l_2_R.B("UTF-8");
                -l_2_R.m();
                Object -l_3_R = new AtomicReference();
                try {
                    f.d("QScannerMgr-AmScannerV2", "[native]nativeLoadAmfHeaderBytes, amfFile:[" + str + "]");
                    if (nativeLoadAmfHeaderBytes(context, str, -l_3_R) == 0) {
                        byte[] -l_4_R = (byte[]) -l_3_R.get();
                        if (-l_4_R != null) {
                            -l_2_R.b(-l_4_R);
                            fd fdVar = (fd) -l_2_R.a("vci", new fd());
                            return fdVar;
                        }
                    }
                } catch (Object -l_4_R2) {
                    f.e("QScannerMgr-AmScannerV2", "loadAmfHeader, e:[" + -l_4_R2 + "]");
                }
            }
        }
        return null;
    }

    public static native int getOpcode(byte[] bArr, AtomicReference<byte[]> atomicReference);

    private static native int initScanner(long j);

    public static boolean isSupported() {
        return BP;
    }

    private static native int nativeLoadAmfHeaderBytes(Context context, String str, AtomicReference<byte[]> atomicReference);

    private static native int nativeUpdateMalwareInfoBytes(Context context, String str, byte[] bArr, byte[] bArr2);

    private static native long newObject(Context context, String str);

    private static native int scanApkBytes(long j, byte[] bArr, AtomicReference<byte[]> atomicReference);

    public synchronized d a(a aVar) {
        if (aVar != null) {
            f.d("QScannerMgr-AmScannerV2", "scanApk, [" + aVar.nf + "][" + aVar.softName + "][" + aVar.bZ + "][" + aVar.path + "]");
            Object -l_2_R = new fn();
            -l_2_R.B("UTF-8");
            -l_2_R.m();
            -l_2_R.put("ak", aVar);
            Object -l_3_R = new AtomicReference();
            f.d("QScannerMgr-AmScannerV2", "[native]scanApkBytes, object:[" + this.object + "]");
            int -l_4_I = scanApkBytes(this.object, -l_2_R.l(), -l_3_R);
            if (-l_4_I == 0) {
                byte[] -l_5_R = (byte[]) -l_3_R.get();
                if (-l_5_R != null) {
                    -l_2_R.k();
                    -l_2_R.b(-l_5_R);
                    d -l_6_R = null;
                    try {
                        -l_6_R = (d) -l_2_R.a("qsr", new d());
                    } catch (Object -l_7_R) {
                        -l_7_R.printStackTrace();
                    }
                } else {
                    f.g("QScannerMgr-AmScannerV2", "scanApk, scanApkBytes() return null");
                    return null;
                }
            }
            f.g("QScannerMgr-AmScannerV2", "scanApk, native scanApkBytes() err: " + -l_4_I);
            return null;
        }
        f.g("QScannerMgr-AmScannerV2", "scanApk, apkKey == null!");
        return null;
        return -l_6_R;
    }

    protected synchronized void exit() {
        if (this.object != 0) {
            f.d("QScannerMgr-AmScannerV2", "[native]deleteObject, object:[" + this.object + "]");
            deleteObject(this.object);
            this.object = 0;
        }
    }

    protected synchronized boolean fl() {
        int -l_1_I = initScanner(this.object);
        f.d("QScannerMgr-AmScannerV2", "[native]initScanner:[" + -l_1_I + "]");
        if (-l_1_I == 0) {
            return true;
        }
        Object -l_2_R = lu.b(TMSDKContext.getApplicaionContext(), UpdateConfig.VIRUS_BASE_NAME, null);
        f.g("QScannerMgr-AmScannerV2", "amf file error, delete:[" + -l_2_R + "]");
        lu.bK(-l_2_R);
        return false;
    }
}
