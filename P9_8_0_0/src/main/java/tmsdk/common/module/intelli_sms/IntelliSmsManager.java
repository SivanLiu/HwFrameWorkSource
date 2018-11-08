package tmsdk.common.module.intelli_sms;

import android.content.Context;
import tmsdk.common.SmsEntity;
import tmsdk.common.creator.BaseManagerC;
import tmsdk.common.utils.f;
import tmsdkobf.ic;
import tmsdkobf.kt;
import tmsdkobf.mm;

public class IntelliSmsManager extends BaseManagerC {
    public static final String TAG = "TMSDK_IntelliSmsManager";
    private mm Ae;

    public IntelliSmsCheckResult checkSms(SmsEntity smsEntity, Boolean bool) {
        int -l_5_I = 1;
        f.f(TAG, "checkSms");
        if (this.Ae == null) {
            return null;
        }
        Object -l_3_R = new tmsdk.common.module.aresengine.SmsEntity();
        -l_3_R.phonenum = smsEntity.phonenum;
        -l_3_R.body = smsEntity.body;
        Object -l_4_R = this.Ae.a(-l_3_R, bool);
        if (-l_3_R.protocolType != 1) {
            -l_5_I = MMatchSysResult.getSuggestion(-l_4_R);
        }
        return new IntelliSmsCheckResult(-l_5_I, -l_4_R);
    }

    public synchronized void destroy() {
        if (this.Ae != null) {
            this.Ae.eU();
            this.Ae = null;
        }
    }

    public synchronized void init() {
        if (!ic.bE()) {
            if (this.Ae == null) {
                this.Ae = mm.eV();
            }
            this.Ae.eT();
        }
    }

    public boolean isPaySms(SmsEntity smsEntity) {
        boolean z = false;
        f.f(TAG, "isPaySms");
        if (this.Ae == null) {
            return false;
        }
        Object -l_2_R = new tmsdk.common.module.aresengine.SmsEntity();
        -l_2_R.phonenum = smsEntity.phonenum;
        -l_2_R.body = smsEntity.body;
        if (this.Ae.t(-l_2_R.phonenum, -l_2_R.body) != null) {
            z = true;
        }
        return z;
    }

    public void onCreate(Context context) {
        this.Ae = null;
        kt.saveActionData(1320031);
    }
}
