package tmsdkobf;

import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.Buffalo;
import tmsdk.common.module.intelli_sms.DecomposeResult;
import tmsdk.common.utils.f;

public class ml {
    private static ml Ah = null;
    private Buffalo Ag = null;
    private int mRefCount = 0;

    private ml() {
        f.h("QQPimSecure", "BuffaloImpl 00");
    }

    public static ml eS() {
        if (Ah == null) {
            Object -l_0_R = ml.class;
            synchronized (ml.class) {
                if (Ah == null) {
                    Ah = new ml();
                }
            }
        }
        return Ah;
    }

    public String b(String str, String str2, int i) {
        Object -l_5_R = mm.class;
        synchronized (mm.class) {
            if (this.mRefCount > 0) {
                Object -l_4_R = this.Ag;
                -l_5_R = new DecomposeResult();
                if (-l_4_R.nativeCheckSmsHash_c(str, str2, i, -l_5_R) != 0 || -l_5_R.strResult == null) {
                    return null;
                }
                ku.bt(-l_5_R.strResult);
                return -l_5_R.strResult;
            }
            f.d("QQPimSecure", "BumbleBeeImpl checkSms mRefCount==0");
            return null;
        }
    }

    public void eT() {
        f.h("QQPimSecure", "BuffaloImpl 01");
        Object -l_1_R = lu.b(TMSDKContext.getApplicaionContext(), "rule.dat", null);
        if (-l_1_R != null) {
            Object -l_2_R = mm.class;
            synchronized (mm.class) {
                if (this.mRefCount <= 0) {
                    this.Ag = new Buffalo();
                    this.Ag.nativeInitHashChecker_c(-l_1_R);
                    this.mRefCount = 1;
                    f.h("QQPimSecure", "BuffaloImpl 02");
                    return;
                }
                this.mRefCount++;
            }
        }
    }

    public void eU() {
        f.h("QQPimSecure", "BuffaloImpl 03");
        Object -l_1_R = mm.class;
        synchronized (mm.class) {
            if (this.mRefCount > 0) {
                this.mRefCount--;
                if (this.mRefCount <= 0) {
                    if (this.Ag != null) {
                        this.Ag.nativeFinishHashChecker_c();
                    }
                    Ah = null;
                }
                f.h("QQPimSecure", "BuffaloImpl 04");
                return;
            }
        }
    }
}
