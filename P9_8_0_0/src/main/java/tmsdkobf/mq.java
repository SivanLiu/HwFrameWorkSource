package tmsdkobf;

import android.content.Context;
import com.qq.taf.jce.JceStruct;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.module.optimus.BsFakeType;
import tmsdk.common.module.optimus.IFakeBaseStationListener;
import tmsdk.common.module.optimus.Optimus;
import tmsdk.common.module.optimus.SMSCheckerResult;
import tmsdk.common.module.optimus.impl.bean.BsCloudResult;
import tmsdk.common.module.optimus.impl.bean.BsInput;
import tmsdk.common.module.optimus.impl.bean.BsResult;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;

public class mq implements tmsdkobf.mu.a {
    private static mq AM = null;
    private String AN;
    public Optimus AO = new Optimus();
    private mu AP;
    private mt AQ = new mt();
    private IFakeBaseStationListener AR;
    private Context mContext;

    public abstract class a {
        final /* synthetic */ mq AS;
        public BsCloudResult AU;
        protected CountDownLatch AV;

        public a(mq mqVar, CountDownLatch countDownLatch) {
            this.AS = mqVar;
            this.AV = countDownLatch;
        }

        public abstract void a(BsCloudResult bsCloudResult);
    }

    class b {
        final /* synthetic */ mq AS;
        BsInput AW;
        int AX = 0;

        b(mq mqVar) {
            this.AS = mqVar;
        }
    }

    private mq(Context context) {
        this.mContext = context.getApplicationContext();
        this.AN = lu.b(context, "fake_bs.dat", null);
    }

    private BsCloudResult a(b bVar) {
        Object -l_2_R = new CountDownLatch(1);
        Object -l_3_R = new a(this, -l_2_R) {
            final /* synthetic */ mq AS;

            public void a(BsCloudResult bsCloudResult) {
                f.d("QQPimSecure", "[Optimus]:checkFakeBsWithCloudSync has result =" + bsCloudResult);
                this.AU = bsCloudResult;
                this.AV.countDown();
            }
        };
        f.d("QQPimSecure", "[Optimus]:checkFakeBsWithCloudSync start");
        a(bVar.AW, -l_3_R);
        try {
            -l_2_R.await(3000, TimeUnit.MILLISECONDS);
        } catch (Object -l_4_R) {
            -l_4_R.printStackTrace();
        }
        f.d("QQPimSecure", "[Optimus]:checkFakeBsWithCloudSync timeout or notifyed");
        return -l_3_R.AU;
    }

    private void a(BsInput bsInput, final a aVar) {
        Object -l_3_R = new mx();
        -l_3_R.Bw = bsInput.sms;
        -l_3_R.Bv = bsInput.sender;
        -l_3_R.Bu = mw.l(this.AO.getBsInfos(bsInput));
        im.bK().a(812, -l_3_R, new nc(), 0, new jy(this) {
            final /* synthetic */ mq AS;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                try {
                    nc -l_6_R = (nc) jceStruct;
                    if (-l_6_R == null || -l_6_R.BF == null) {
                        aVar.a(null);
                        return;
                    }
                    Object -l_7_R = mw.a(-l_6_R.BF);
                    this.AS.AO.setBlackWhiteItems(mw.m(-l_6_R.BG), mw.m(-l_6_R.BH));
                    aVar.a(-l_7_R);
                } catch (Throwable th) {
                    aVar.a(null);
                }
            }
        }, 3000);
    }

    public static mq t(Context context) {
        if (AM == null) {
            Object -l_1_R = mq.class;
            synchronized (mq.class) {
                if (AM == null) {
                    AM = new mq(context);
                }
            }
        }
        return AM;
    }

    public void a(BsInput bsInput) {
        this.AQ.bZ("基站信息发生了变化");
        Object -l_2_R = new BsResult();
        this.AO.check(bsInput, -l_2_R);
        if (BsFakeType.FAKE == -l_2_R.fakeType) {
            f.f("Optimus", "type is fake");
            mv.fj().w(System.currentTimeMillis());
            this.AQ.a("", "", "", bsInput.neighbors == null ? "" : bsInput.neighbors.toString(), this.AO.getUploadInfo(), true, false);
            if (this.AR != null) {
                f.f("Optimus", "onFakeNotify");
                this.AR.onFakeNotify(-l_2_R.fakeType);
            }
        } else if (BsFakeType.RIST == -l_2_R.fakeType) {
            f.f("Optimus", "type is risk");
            if (this.AR != null) {
                f.f("Optimus", "onFakeNotify");
                this.AR.onFakeNotify(-l_2_R.fakeType);
            }
        }
    }

    public SMSCheckerResult b(SmsEntity smsEntity, boolean z) {
        Object -l_3_R = new SMSCheckerResult();
        if (smsEntity == null) {
            return -l_3_R;
        }
        Object -l_4_R = this.AP == null ? new BsInput() : this.AP.fi();
        -l_4_R.sender = smsEntity.phonenum;
        -l_4_R.sms = smsEntity.body;
        Object -l_5_R = new BsResult();
        f.f("Optimus", "check local");
        this.AO.check(-l_4_R, -l_5_R);
        -l_3_R.mType = -l_5_R.fakeType;
        if (z && i.iE() && !i.iF()) {
            f.f("Optimus", "check cloud");
            b -l_6_R = new b(this);
            -l_6_R.AW = -l_4_R;
            this.AQ.fe();
            Object -l_7_R = a(-l_6_R);
            if (-l_7_R != null) {
                this.AO.checkWithCloud(-l_4_R, -l_7_R, -l_5_R);
                -l_3_R.isCloudCheck = true;
            }
        }
        f.f("Optimus", "final result is " + -l_5_R.toString());
        this.AQ.bZ("|最终的检测结果=" + -l_5_R.toString());
        if (BsFakeType.FAKE == -l_5_R.fakeType) {
            mv.fj().v(System.currentTimeMillis());
            this.AQ.a("", smsEntity.phonenum, smsEntity.body, -l_4_R.neighbors == null ? "" : -l_4_R.neighbors.toString(), this.AO.getUploadInfo(), false, -l_3_R.isCloudCheck);
        }
        -l_3_R.mType = -l_5_R.fakeType;
        return -l_3_R;
    }

    public void setFakeBsListener(IFakeBaseStationListener iFakeBaseStationListener) {
        this.AR = iFakeBaseStationListener;
    }

    public boolean start() {
        if (!this.AO.init(this.AN, null)) {
            return false;
        }
        this.AQ.init();
        this.AP = new mu(this.AQ);
        this.AP.a((tmsdkobf.mu.a) this);
        this.AP.u(this.mContext);
        return true;
    }

    public void stop() {
        this.AO.finish();
        if (this.AP != null) {
            this.AP.v(this.mContext);
            this.AP.a(null);
        }
        if (this.AQ != null) {
            this.AQ.destroy();
        }
        Object -l_1_R = mq.class;
        synchronized (mq.class) {
            AM = null;
        }
    }
}
