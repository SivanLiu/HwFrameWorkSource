package tmsdkobf;

import android.content.Intent;
import java.util.Arrays;
import tmsdk.common.module.aresengine.SmsEntity;

public final class hs {
    private hn qx;

    private boolean a(String str, String... strArr) {
        return strArr.length <= 1 ? strArr[0].equals(str) : Arrays.asList(strArr).contains(str);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void a(Intent intent) {
        Object -l_2_R = intent.getAction();
        Object -l_3_R = intent.getType();
        if (-l_2_R != null) {
            if (a(-l_2_R, "android.provider.Telephony.SMS_RECEIVED", "android.provider.Telephony.SMS_RECEIVED2", "android.provider.Telephony.GSM_SMS_RECEIVED", "android.provider.Telephony.SMS_DELIVER")) {
                this.qx = new hx(intent);
            } else if (-l_3_R != null) {
                if (a(-l_2_R, "android.provider.Telephony.WAP_PUSH_RECEIVED", "android.provider.Telephony.WAP_PUSH_GSM_RECEIVED", "android.provider.Telephony.WAP_PUSH_DELIVER")) {
                    if (a(-l_3_R, "application/vnd.wap.sic", "application/vnd.wap.slc", "application/vnd.wap.coc")) {
                        this.qx = new ia(intent);
                    } else {
                        if (a(-l_3_R, "application/vnd.wap.mms-message")) {
                            this.qx = new hr(intent);
                        }
                    }
                } else {
                    this.qx = null;
                }
            } else {
                this.qx = null;
            }
        }
    }

    public synchronized SmsEntity bt() {
        SmsEntity -l_1_R;
        -l_1_R = null;
        if (this.qx != null) {
            -l_1_R = this.qx.bt();
            this.qx = null;
        }
        return -l_1_R;
    }

    public synchronized boolean bv() {
        return this.qx != null;
    }
}
