package tmsdkobf;

import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.module.intelli_sms.MMatchSysResult;
import tmsdk.common.module.intelli_sms.SmsCheckInput;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;
import tmsdk.common.utils.s;

public class mm {
    public static final int[][] Ai;
    private static mm Ak = null;
    private mn Aj = null;
    private int mRefCount = 0;

    static {
        r0 = new int[3][];
        r0[0] = new int[]{0, 0};
        r0[1] = new int[]{1, 1};
        r0[2] = new int[]{2, 2};
        Ai = r0;
    }

    private mm() {
        f.h("QQPimSecure", "BumbleBeeImpl 00");
    }

    public static mm eV() {
        if (Ak == null) {
            Object -l_0_R = mm.class;
            synchronized (mm.class) {
                if (Ak == null) {
                    Ak = new mm();
                }
            }
        }
        return Ak;
    }

    public MMatchSysResult a(SmsEntity smsEntity, Boolean bool) {
        if (smsEntity.protocolType < 0 || smsEntity.protocolType > 2) {
            smsEntity.protocolType = 0;
        }
        SmsCheckResult -l_3_R = b(smsEntity, bool);
        if (-l_3_R == null) {
            return new MMatchSysResult(1, 1, 0, 0, 1, null);
        }
        Object -l_4_R = new MMatchSysResult(-l_3_R);
        -l_4_R.contentType = aU(-l_4_R.contentType);
        return -l_4_R;
    }

    public int aU(int -l_2_I) {
        Object -l_4_R = mm.class;
        synchronized (mm.class) {
            if (this.mRefCount > 0) {
                Object -l_3_R = this.Aj;
                int -l_4_I = 0;
                try {
                    -l_4_I = -l_3_R.nativeCalcMap(-l_2_I);
                } catch (Throwable th) {
                }
                if (-l_4_I == 3) {
                    -l_2_I = 3;
                } else if (-l_4_I == 4) {
                    -l_2_I = 4;
                }
                return -l_2_I;
            }
            f.d("QQPimSecure", "BumbleBeeImpl filterResult mRefCount==0");
            return -l_2_I;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SmsCheckResult b(SmsEntity smsEntity, Boolean bool) {
        s.bW(512);
        kt.aE(1320031);
        if (bool.booleanValue()) {
            kt.aE(1320004);
        }
        Object -l_4_R = mm.class;
        synchronized (mm.class) {
            if (this.mRefCount > 0) {
                Object -l_3_R = this.Aj;
                if (smsEntity == null) {
                    return null;
                }
                SmsCheckResult -l_5_R;
                SmsCheckInput -l_4_R2 = new SmsCheckInput(smsEntity.phonenum, smsEntity.body, Ai[smsEntity.protocolType][0], 0, 0, 0);
                if (bool.booleanValue() && i.iE()) {
                    -l_5_R = -l_3_R.a(-l_4_R2);
                    if (-l_5_R != null) {
                        f.d("BUMBLEBEE", "Bumble cloud scan success! ");
                        -l_5_R.sIsCloudResult = true;
                    } else {
                        -l_5_R = new SmsCheckResult();
                    }
                } else {
                    -l_5_R = new SmsCheckResult();
                }
                if (bool.booleanValue()) {
                    kr.dz();
                }
                f.d("BUMBLEBEE", "SmsCheckResult = " + (-l_5_R == null ? "null" : -l_5_R.toString()));
                return -l_5_R;
            }
            f.d("QQPimSecure", "BumbleBeeImpl checkSms mRefCount==0");
            return null;
        }
    }

    public void eT() {
        f.h("QQPimSecure", "BumbleBeeImpl 01");
        Object -l_1_R = mm.class;
        synchronized (mm.class) {
            if (this.mRefCount <= 0) {
                this.mRefCount = 1;
                this.Aj = new mn();
                f.h("QQPimSecure", "BumbleBeeImpl 02");
                return;
            }
            this.mRefCount++;
        }
    }

    public void eU() {
        f.h("QQPimSecure", "BumbleBeeImpl 03");
        Object -l_1_R = mm.class;
        synchronized (mm.class) {
            if (this.mRefCount > 0) {
                this.mRefCount--;
                if (this.mRefCount <= 0) {
                    this.Aj.eX();
                    this.Aj = null;
                    Ak = null;
                }
                f.h("QQPimSecure", "BumbleBeeImpl 04");
                return;
            }
        }
    }

    public SmsCheckResult t(String str, String str2) {
        Object -l_4_R = mm.class;
        synchronized (mm.class) {
            if (this.mRefCount > 0) {
                Object -l_3_R = this.Aj;
                if (str == null || str2 == null) {
                    return null;
                }
                -l_4_R = -l_3_R.u(str, str2);
                f.d("QQPimSecure", "SmsCheckResult = " + (-l_4_R == null ? "null" : -l_4_R.toString()));
                return -l_4_R;
            }
            f.d("QQPimSecure", "BumbleBeeImpl isPaySms mRefCount==0");
            return null;
        }
    }
}
