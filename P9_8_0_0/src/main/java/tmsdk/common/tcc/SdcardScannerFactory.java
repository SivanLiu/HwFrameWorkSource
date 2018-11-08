package tmsdk.common.tcc;

import tmsdk.common.TMSDKContext;
import tmsdk.common.tcc.DeepCleanEngine.Callback;
import tmsdkobf.ma;
import tmsdkobf.py;
import tmsdkobf.py.a;
import tmsdkobf.qa;

public class SdcardScannerFactory {
    public static final long FLAG_GET_ALL_FILE = 8;
    public static final long FLAG_NEED_BASIC_INFO = 2;
    public static final long FLAG_NEED_EXTRA_INFO = 4;
    public static final long FLAG_SCAN_WIDE = 16;
    public static final int TYPE_QSCANNER = 1;
    public static boolean isLoadNativeOK;

    static {
        isLoadNativeOK = false;
        isLoadNativeOK = ma.f(TMSDKContext.getApplicaionContext(), "dce-1.1.17-mfr");
    }

    public static DeepCleanEngine getDeepCleanEngine(Callback callback) {
        return getDeepCleanEngine(callback, 0);
    }

    public static DeepCleanEngine getDeepCleanEngine(Callback callback, int i) {
        if (!isLoadNativeOK) {
            return null;
        }
        Object -l_2_R = new DeepCleanEngine(callback);
        return !-l_2_R.init(i) ? null : -l_2_R;
    }

    public static QSdcardScanner getQSdcardScanner(long j, a aVar, qa qaVar) {
        QSdcardScanner -l_4_R = (QSdcardScanner) getScanner(1, j, qaVar);
        if (-l_4_R == null) {
            return null;
        }
        -l_4_R.setListener(aVar);
        return -l_4_R;
    }

    private static py getScanner(int i, long j, Object obj) {
        switch (i) {
            case 1:
                long -l_4_J = nativeAllocate(i, j);
                return -l_4_J == 0 ? null : new QSdcardScanner(-l_4_J, i, j, obj);
            default:
                return null;
        }
    }

    private static native long nativeAllocate(int i, long j);
}
