package tmsdkobf;

import android.os.SystemClock;
import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.intelli_sms.NativeBumblebee;
import tmsdk.common.module.intelli_sms.SmsCheckInput;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.module.update.IUpdateObserver;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.module.update.UpdateInfo;
import tmsdk.common.module.update.UpdateManager;
import tmsdk.common.utils.f;

public class mn {
    private long Al;
    private HashMap<SmsCheckInput, Queue<b>> Am;
    private NativeBumblebee An;
    private IUpdateObserver vD = new IUpdateObserver(this) {
        final /* synthetic */ mn Ap;

        {
            this.Ap = r1;
        }

        public void onChanged(UpdateInfo updateInfo) {
            this.Ap.reload();
        }
    };

    private static class a implements mo {
        private SmsCheckResult Aq;
        private CountDownLatch Ar;

        public a(CountDownLatch countDownLatch) {
            this.Ar = countDownLatch;
        }

        private SmsCheckResult eY() {
            return this.Aq;
        }

        public int a(SmsCheckResult smsCheckResult) {
            f.d("BUMBLEBEE", "checkSmsCloudSync onResult " + (smsCheckResult == null ? "null" : smsCheckResult.toString()));
            this.Aq = smsCheckResult;
            f.d("BUMBLEBEE", "checkSmsCloudSync onResult countDown");
            this.Ar.countDown();
            f.d("BUMBLEBEE", "checkSmsCloudSync onResult countDown finish");
            return 0;
        }
    }

    private static class b {
        public mo As;
        public SmsCheckInput At;
        public cg Au;
        public ArrayList<a> Av;

        public static class a {
            public long Aw;
            public String name;
            public long time;
        }

        private b() {
            this.Av = new ArrayList();
        }
    }

    public mn() {
        if (NativeBumblebee.isLoadNative()) {
            this.An = new NativeBumblebee();
            int -l_1_I = this.An.nativeInitSmsChecker_c(0, eW());
            ((UpdateManager) ManagerCreatorC.getManager(UpdateManager.class)).addObserver(UpdateConfig.UPDATE_FLAG_POSEIDONV2, this.vD);
            f.d("BUMBLEBEE", "initSmsChecker " + -l_1_I);
        }
        u(3000);
        this.Am = new HashMap();
    }

    private void a(SmsCheckInput smsCheckInput, cg cgVar) {
        cgVar.sender = smsCheckInput.sender;
        cgVar.sms = smsCheckInput.sms;
        cgVar.uiCheckFlag = smsCheckInput.uiCheckFlag;
        cgVar.uiSmsInOut = smsCheckInput.uiSmsInOut;
        cgVar.uiSmsType = smsCheckInput.uiSmsType;
        cgVar.uiCheckType = smsCheckInput.uiCheckType;
        cgVar.eS = 0;
        cgVar.eT = null;
    }

    private void a(ch chVar, SmsCheckResult smsCheckResult) {
        smsCheckResult.uiFinalAction = chVar.uiFinalAction;
        smsCheckResult.uiContentType = chVar.uiContentType;
        smsCheckResult.uiMatchCnt = chVar.uiMatchCnt;
        smsCheckResult.fScore = chVar.fScore;
        smsCheckResult.uiActionReason = chVar.uiActionReason;
        if (chVar.stRuleTypeID != null) {
            Object -l_3_R = chVar.stRuleTypeID.iterator();
            while (-l_3_R.hasNext()) {
                ci -l_4_R = (ci) -l_3_R.next();
                smsCheckResult.addRuleTypeID(-l_4_R.uiRuleType, -l_4_R.uiRuleTypeId);
            }
        }
        smsCheckResult.sRule = chVar.sRule;
        smsCheckResult.uiShowRiskName = chVar.uiShowRiskName;
        smsCheckResult.sRiskClassify = chVar.sRiskClassify;
        smsCheckResult.sRiskUrl = chVar.sRiskUrl;
        smsCheckResult.sRiskName = chVar.sRiskName;
        smsCheckResult.sRiskReach = chVar.sRiskReach;
    }

    private void a(final b bVar) {
        f.d("BUMBLEBEE", "sendSmsToCloud");
        a(bVar, "add");
        im.bK().a(807, bVar.Au, new ch(), 0, new jy(this) {
            final /* synthetic */ mn Ap;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                f.d("BUMBLEBEE", "sendSmsToCloud onFinish");
                ch -l_6_R = (ch) jceStruct;
                f.d("BUMBLEBEE", "reqNO: " + i + " cmdId: " + i2 + " retCode: " + i3 + " result " + (jceStruct != null ? jceStruct.toString() : "null"));
                switch (i3) {
                    case 0:
                        this.Ap.a(bVar, -l_6_R);
                        return;
                    default:
                        return;
                }
            }
        }, 10000);
    }

    private void a(b bVar, String str) {
        a(bVar, str, 0);
    }

    private synchronized void a(b bVar, String str, long j) {
        Object -l_5_R = new a();
        -l_5_R.name = str;
        -l_5_R.time = SystemClock.elapsedRealtime();
        -l_5_R.Aw = j;
        bVar.Av.add(-l_5_R);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void a(b bVar, ch chVar) {
        synchronized (this.Am) {
            if (this.Am.containsKey(bVar.At)) {
                Queue -l_3_R = (Queue) this.Am.remove(bVar.At);
            }
        }
    }

    private synchronized void b(b bVar) {
        f.d("BUMBLEBEE", "printLog ==================================================");
        f.d("BUMBLEBEE", "printLog |sender: " + bVar.Au.sender + " |sms: " + bVar.Au.sms + "|");
        long -l_2_J = ((a) bVar.Av.get(0)).time;
        long -l_4_J = --l_2_J;
        Object -l_6_R = "printLog %-20s[%d] duration[%06d] timeout[%b]";
        Object -l_7_R = bVar.Av.iterator();
        while (-l_7_R.hasNext()) {
            boolean z;
            a -l_8_R = (a) -l_7_R.next();
            Object[] objArr = new Object[4];
            objArr[0] = -l_8_R.name;
            objArr[1] = Long.valueOf(-l_8_R.time);
            objArr[2] = Long.valueOf(-l_8_R.time - -l_2_J);
            if (-l_8_R.Aw != 0) {
                if ((-l_8_R.time - -l_2_J <= -l_8_R.Aw ? 1 : null) == null) {
                    z = true;
                    objArr[3] = Boolean.valueOf(z);
                    f.d("BUMBLEBEE", String.format(-l_6_R, objArr));
                    -l_2_J = -l_8_R.time;
                }
            }
            z = false;
            objArr[3] = Boolean.valueOf(z);
            f.d("BUMBLEBEE", String.format(-l_6_R, objArr));
            -l_2_J = -l_8_R.time;
        }
        f.d("BUMBLEBEE", "total[" + (-l_4_J + ((a) bVar.Av.get(bVar.Av.size() - 1)).time) + "]");
    }

    private String eW() {
        Object -l_1_R = UpdateConfig.POSEIDONV2;
        lu.b(TMSDKContext.getApplicaionContext(), -l_1_R, null);
        return ((UpdateManager) ManagerCreatorC.getManager(UpdateManager.class)).getFileSavePath() + File.separator + -l_1_R;
    }

    public int a(SmsCheckInput smsCheckInput, SmsCheckResult smsCheckResult) {
        if (!NativeBumblebee.isLoadNative()) {
            return -4;
        }
        if (smsCheckInput == null || smsCheckResult == null) {
            return -5;
        }
        f.d("BUMBLEBEE", "checkSmsLocal IN Java: " + smsCheckInput.toString());
        int -l_3_I = this.An.nativeCheckSms_c(smsCheckInput, smsCheckResult);
        f.d("BUMBLEBEE", "checkSmsLocal IN Java: ret = " + -l_3_I);
        return -l_3_I;
    }

    public SmsCheckResult a(SmsCheckInput smsCheckInput) {
        Object -l_2_R = null;
        Object -l_3_R = new CountDownLatch(1);
        a -l_4_R = new a(-l_3_R);
        a(smsCheckInput, (mo) -l_4_R);
        try {
            f.d("BUMBLEBEE", "checkSmsCloudSync latch.await()");
            if (-l_3_R.await(this.Al, TimeUnit.MILLISECONDS)) {
                f.d("BUMBLEBEE", "checkSmsCloudSync await true");
                -l_2_R = -l_4_R.eY();
            } else {
                f.d("BUMBLEBEE", "checkSmsCloudSync await false");
            }
        } catch (Object -l_5_R) {
            -l_5_R.printStackTrace();
        }
        return -l_2_R;
    }

    public void a(SmsCheckInput smsCheckInput, mo moVar) {
        int -l_4_I;
        b -l_3_R = new b();
        -l_3_R.As = moVar;
        -l_3_R.At = smsCheckInput;
        -l_3_R.Au = new cg();
        a(smsCheckInput, -l_3_R.Au);
        if (NativeBumblebee.isLoadNative() && this.An.nativeIsPrivateSms_c(smsCheckInput.sender, smsCheckInput.sms) == 1) {
            Object -l_5_R = this.An.nativeGetSmsInfo_c(smsCheckInput.sender, smsCheckInput.sms);
            if (-l_5_R == null) {
                moVar.a(null);
                return;
            } else {
                -l_3_R.Au.eS = 1;
                -l_3_R.Au.sms = -l_5_R;
            }
        }
        synchronized (this.Am) {
            if (this.Am.containsKey(-l_3_R.At)) {
                Object -l_6_R = (Queue) this.Am.get(-l_3_R.At);
                if (-l_6_R == null) {
                    -l_6_R = new LinkedList();
                }
                -l_6_R.add(-l_3_R);
                this.Am.put(-l_3_R.At, -l_6_R);
                -l_4_I = 0;
            } else {
                this.Am.put(-l_3_R.At, null);
                -l_4_I = 1;
            }
        }
        if (-l_4_I != 0) {
            a(-l_3_R);
        }
    }

    public void eX() {
        f.d("BUMBLEBEE", "Bumblebee stop()");
        if (NativeBumblebee.isLoadNative()) {
            ((UpdateManager) ManagerCreatorC.getManager(UpdateManager.class)).removeObserver(UpdateConfig.UPDATE_FLAG_POSEIDONV2);
            this.An.nativeFinishSmsChecker_c();
        }
    }

    public int nativeCalcMap(int i) {
        return NativeBumblebee.isLoadNative() ? this.An.nativeCalcMap_c(i) : 0;
    }

    public void reload() {
        if (NativeBumblebee.isLoadNative()) {
            this.An.nativeFinishSmsChecker_c();
            this.An.nativeInitSmsChecker_c(0, eW());
        }
    }

    public SmsCheckResult u(String str, String str2) {
        SmsCheckInput -l_3_R = new SmsCheckInput();
        -l_3_R.sender = str;
        -l_3_R.sms = str2;
        -l_3_R.uiCheckType = 1;
        SmsCheckResult -l_4_R = new SmsCheckResult();
        return (a(-l_3_R, -l_4_R) == 0 && -l_4_R.uiFinalAction == 2 && -l_4_R.uiContentType == SmsCheckResult.ESCT_PAY) ? -l_4_R : null;
    }

    public void u(long j) {
        if ((j >= 0 ? 1 : null) == null) {
            throw new IllegalArgumentException("CloudTimeout must >= 0 ");
        }
        this.Al = j;
    }
}
