package tmsdkobf;

import android.content.Context;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.BaseManagerC;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.module.optimus.BsFakeType;
import tmsdk.common.module.optimus.IFakeBaseStationListener;
import tmsdk.common.module.optimus.SMSCheckerResult;
import tmsdk.common.utils.f;
import tmsdk.common.utils.s;

public class ms extends BaseManagerC {
    private mq AZ = null;
    private volatile boolean Ac = false;
    private mm pG = null;

    private SMSCheckerResult c(SmsEntity smsEntity, Boolean bool) {
        if (smsEntity.protocolType < 0 || smsEntity.protocolType > 2) {
            smsEntity.protocolType = 0;
        }
        Object -l_3_R = this.pG.b(smsEntity, bool);
        return (-l_3_R != null && -l_3_R.uiContentType == SmsCheckResult.ESCT_326) ? new SMSCheckerResult(BsFakeType.FAKE, -l_3_R.sIsCloudResult) : null;
    }

    public SMSCheckerResult checkSms(tmsdk.common.SmsEntity smsEntity, boolean z) {
        if (this.Ac) {
            if (z) {
                kt.aE(1320001);
            }
            Object -l_3_R = new SmsEntity();
            -l_3_R.phonenum = smsEntity.phonenum;
            -l_3_R.body = smsEntity.body;
            Object -l_4_R = c(-l_3_R, Boolean.valueOf(z));
            if (-l_4_R == null) {
                return this.AZ.b(-l_3_R, z);
            }
            f.f("Optimus", "intelli_fake, return");
            return -l_4_R;
        }
        f.f("Optimus", "not inited");
        return null;
    }

    public long getFakeBSLastTime() {
        if (!this.Ac) {
            return -1;
        }
        kt.saveActionData(1320002);
        return mv.fj().fk();
    }

    public void onCreate(Context context) {
    }

    public void setFakeBsListener(IFakeBaseStationListener iFakeBaseStationListener) {
        if (this.AZ != null) {
            this.AZ.setFakeBsListener(iFakeBaseStationListener);
        }
    }

    public synchronized boolean start() {
        s.bW(IncomingSmsFilterConsts.PAY_SMS);
        if (this.Ac) {
            stop();
        }
        if (this.AZ == null) {
            this.AZ = mq.t(TMSDKContext.getApplicaionContext());
            this.AZ.start();
            f.d("jiejie-optimus", "isInited 2 is " + this.Ac);
        }
        if (this.pG == null) {
            this.pG = mm.eV();
            this.pG.eT();
        }
        this.Ac = true;
        return true;
    }

    public synchronized void stop() {
        if (this.Ac) {
            if (this.AZ != null) {
                this.AZ.stop();
                this.AZ = null;
            }
            if (this.pG != null) {
                this.pG.eU();
                this.pG = null;
            }
        }
        mv.stop();
        this.Ac = false;
    }
}
