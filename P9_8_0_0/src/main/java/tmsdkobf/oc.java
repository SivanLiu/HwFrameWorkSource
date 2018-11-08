package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;

public class oc {
    private nl CT;
    private PowerManager Eg;
    private a Ho;
    private b Hp;
    private h Hq;
    private AtomicInteger Hr = new AtomicInteger(0);
    private boolean Hs = false;
    private Runnable Ht = new Runnable(this) {
        final /* synthetic */ oc Hv;

        {
            this.Hv = r1;
        }

        public void run() {
            this.Hv.mHandler.postDelayed(new Runnable(this) {
                final /* synthetic */ AnonymousClass2 Hw;

                {
                    this.Hw = r1;
                }

                public void run() {
                    synchronized (this.Hw.Hv) {
                        if (this.Hw.Hv.Hs) {
                            nv.z("SharkTcpControler", "[tcp_control][shark_conf][shark_alarm] keep after send timeout, tryCloseConnectionAsyn()");
                            this.Hw.Hv.gI();
                            this.Hw.Hv.Hs = false;
                        }
                    }
                }
            }, 5000);
            mb.d("SharkTcpControler", "[tcp_control][shark_conf][shark_alarm] keep after send timeout(by alarm), delay 5s by handler");
        }
    };
    private boolean Hu = false;
    private Context mContext = TMSDKContext.getApplicaionContext();
    private Handler mHandler = new Handler(this, nu.getLooper()) {
        final /* synthetic */ oc Hv;

        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    mb.n("SharkTcpControler", "[tcp_control][shark_conf] MSG_EXE_RULE_OPEN");
                    this.Hv.gJ();
                    this.Hv.Ho.gP();
                    return;
                case 1:
                    mb.n("SharkTcpControler", "[tcp_control][shark_conf] MSG_EXE_RULE_CLOSE");
                    this.Hv.gI();
                    return;
                case 3:
                    mb.n("SharkTcpControler", "[tcp_control][shark_conf] MSG_EXE_RULE_CYCLE");
                    this.Hv.gL();
                    return;
                default:
                    return;
            }
        }
    };

    public interface a {
        void gP();

        void onClose();
    }

    private class b extends if {
        final /* synthetic */ oc Hv;

        private b(oc ocVar) {
            this.Hv = ocVar;
        }

        public void doOnRecv(Context context, Intent intent) {
            mb.d("SharkTcpControler", "[tcp_control][shark_conf]doOnRecv()");
            Object -l_3_R = intent.getAction();
            Object -l_4_R = intent.getPackage();
            if (-l_3_R == null || -l_4_R == null || !-l_4_R.equals(TMSDKContext.getApplicaionContext().getPackageName())) {
                mb.d("SharkTcpControler", "[tcp_control][shark_conf]TcpControlReceiver.onReceive(), null action or from other pkg, ignore");
                return;
            }
            if (-l_3_R.equals("action_keep_alive_cycle")) {
                this.Hv.mHandler.sendEmptyMessage(3);
            } else if (-l_3_R.equals("action_keep_alive_close")) {
                this.Hv.mHandler.sendEmptyMessage(1);
            }
        }
    }

    public oc(nl nlVar, a aVar) {
        this.CT = nlVar;
        this.Ho = aVar;
        try {
            this.Eg = (PowerManager) this.mContext.getSystemService("power");
        } catch (Throwable th) {
        }
    }

    private static final int bA(int i) {
        return bz(i * 60);
    }

    private static final int bz(int i) {
        return i * 60;
    }

    private static void d(h hVar) {
        if (hVar != null) {
            if (hVar.A != null && hVar.A.size() > 0) {
                t(hVar.A);
            } else {
                hVar.A = gK();
            }
            if (hVar.interval <= 30) {
                hVar.interval = 30;
            }
            if (hVar.B <= 0) {
                hVar.B = SmsCheckResult.ESCT_300;
            }
            if (hVar.E <= 0) {
                hVar.E = 120;
            }
            if (hVar.F <= 0) {
                hVar.F = 10;
            }
        }
    }

    private void gJ() {
        if (this.Hr.get() < 0) {
            this.Hr.set(0);
        }
        String str = "SharkTcpControler";
        mb.n(str, "[tcp_control][shark_conf]markKeepAlive(), refCount: " + this.Hr.incrementAndGet());
    }

    private static ArrayList<f> gK() {
        Object -l_0_R = new ArrayList();
        Object -l_1_R = new f();
        -l_1_R.start = bA(0);
        -l_1_R.n = bz(10);
        -l_1_R.o = bz(60);
        -l_0_R.add(-l_1_R);
        -l_1_R = new f();
        -l_1_R.start = bA(8);
        -l_1_R.n = bz(15);
        -l_1_R.o = bz(15);
        -l_0_R.add(-l_1_R);
        -l_1_R = new f();
        -l_1_R.start = bA(15);
        -l_1_R.n = bz(10);
        -l_1_R.o = bz(20);
        -l_0_R.add(-l_1_R);
        return -l_0_R;
    }

    private void gL() {
        Object -l_1_R = gN();
        if (-l_1_R != null) {
            gM();
            if (ck("execRule")) {
                this.mHandler.sendEmptyMessage(0);
                oj.a(this.mContext, "action_keep_alive_close", (long) (-l_1_R.n * CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY));
                mb.r("SharkTcpControler", "[tcp_control][shark_conf]now open connection, after " + -l_1_R.n + "s close connection");
            } else {
                mb.s("SharkTcpControler", "[tcp_control][f_p][h_b][shark_conf]execRule(), scSharkConf: donnot keepAlive!");
            }
            oj.a(this.mContext, "action_keep_alive_cycle", (long) ((-l_1_R.n + -l_1_R.o) * CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY));
            mb.r("SharkTcpControler", "[tcp_control][shark_conf]execRule(), next cycle in " + (-l_1_R.n + -l_1_R.o) + "s");
            return;
        }
        mb.s("SharkTcpControler", "[tcp_control][shark_conf]no KeepAlivePolicy for current time!");
    }

    private void gM() {
        mb.d("SharkTcpControler", "[tcp_control][shark_conf]cancelOldAction()");
        oj.h(this.mContext, "action_keep_alive_close");
        oj.h(this.mContext, "action_keep_alive_cycle");
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(0);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private f gN() {
        oc -l_1_R = this;
        synchronized (this) {
            Object -l_2_R = az();
            if (!(-l_2_R == null || -l_2_R.A == null || -l_2_R.A.size() <= 0)) {
                int -l_3_I = gO();
                int -l_4_I = -l_2_R.A.size() - 1;
                while (-l_4_I >= 0) {
                    f -l_5_R = (f) -l_2_R.A.get(-l_4_I);
                    if (-l_5_R.start > -l_3_I) {
                        -l_4_I--;
                    } else {
                        mb.n("SharkTcpControler", "[tcp_control][shark_conf]getRuleAtNow(), fixed policy: start hour: " + (-l_5_R.start / 3600) + " start: " + -l_5_R.start + " keep: " + -l_5_R.n + " close: " + -l_5_R.o);
                        return -l_5_R;
                    }
                }
            }
        }
    }

    private int gO() {
        Object -l_1_R = Calendar.getInstance();
        return -l_1_R != null ? ((-l_1_R.get(11) * 3600) + (-l_1_R.get(12) * 60)) + -l_1_R.get(13) : 0;
    }

    private static void t(List<f> list) {
        if (list != null && list.size() != 0) {
            if (((f) list.get(0)).start > 0) {
                f -l_1_R = (f) list.get(list.size() - 1);
                Object -l_2_R = new f();
                -l_2_R.start = bA(0);
                -l_2_R.n = -l_1_R.n;
                -l_2_R.o = -l_1_R.o;
                list.add(0, -l_2_R);
            }
            try {
                Collections.sort(list, new Comparator<f>() {
                    public int a(f fVar, f fVar2) {
                        return fVar.start - fVar2.start;
                    }

                    public /* synthetic */ int compare(Object obj, Object obj2) {
                        return a((f) obj, (f) obj2);
                    }
                });
            } catch (Object -l_1_R2) {
                mb.b("SharkTcpControler", "[tcp_control][shark_conf]checkAndSort() exception: " + -l_1_R2, -l_1_R2);
            }
        }
    }

    public h az() {
        oc -l_1_R = this;
        synchronized (this) {
            if (this.Hq == null) {
                this.Hq = this.CT.aQ();
                if (this.Hq == null) {
                    this.Hq = new h();
                    if (nu.gc()) {
                        this.Hq.interval = 30;
                        this.Hq.B = 60;
                    } else {
                        this.Hq.interval = SmsCheckResult.ESCT_270;
                        this.Hq.B = SmsCheckResult.ESCT_300;
                    }
                    this.Hq.z = new ArrayList();
                    this.Hq.A = gK();
                    this.Hq.C = true;
                    this.Hq.D = true;
                    this.Hq.E = 120;
                    this.Hq.F = 10;
                } else {
                    d(this.Hq);
                }
            }
        }
        return this.Hq;
    }

    public void c(h hVar) {
        if (hVar != null) {
            oc -l_2_R = this;
            synchronized (this) {
                this.Hq = hVar;
                this.CT.b(this.Hq);
                d(this.Hq);
            }
            return;
        }
        mb.s("SharkTcpControler", "[tcp_control][shark_conf]onSharkConfPush(), scSharkConf == null");
    }

    boolean ck(String str) {
        Object -l_2_R = az();
        if (-l_2_R == null) {
            return true;
        }
        int -l_3_I = 1;
        if (!(-l_2_R.C || eb.iJ == i.iG())) {
            -l_3_I = 0;
            mb.r("SharkTcpControler", "[tcp_control][shark_conf] shouldKeepAlive(), not allow in none wifi! timing: " + str);
        }
        if (!(-l_3_I == 0 || -l_2_R.D)) {
            int -l_4_I = 0;
            if (this.Eg != null) {
                try {
                    -l_4_I = this.Eg.isScreenOn() ? 0 : 1;
                } catch (Throwable th) {
                }
            }
            if (-l_4_I != 0) {
                mb.r("SharkTcpControler", "[tcp_control][shark_conf] shouldKeepAlive(), not allow on screen off! timing: " + str);
                -l_3_I = 0;
            }
        }
        return -l_3_I;
    }

    public synchronized void gC() {
        if (!this.Hu) {
            f.d("SharkTcpControler", "[tcp_control][shark_conf]startTcpControl()");
            if (this.Hp == null) {
                this.Hp = new b();
                Object -l_1_R = new IntentFilter();
                -l_1_R.addAction("action_keep_alive_close");
                -l_1_R.addAction("action_keep_alive_cycle");
                try {
                    this.mContext.registerReceiver(this.Hp, -l_1_R);
                } catch (Object -l_2_R) {
                    mb.s("SharkTcpControler", "[tcp_control][shark_conf]registerReceiver exception: " + -l_2_R);
                }
            }
            this.mHandler.sendEmptyMessage(3);
            this.Hu = true;
        }
    }

    public synchronized void gD() {
        if (this.Hu) {
            f.d("SharkTcpControler", "[tcp_control][shark_conf]stopTcpControl()");
            gM();
            if (this.Hp != null) {
                try {
                    this.mContext.unregisterReceiver(this.Hp);
                    this.Hp = null;
                } catch (Object -l_1_R) {
                    mb.s("SharkTcpControler", "[tcp_control][shark_conf]unregisterReceiver exception: " + -l_1_R);
                }
            }
            gI();
            this.Hu = false;
        }
    }

    public int gG() {
        return this.Hr.get();
    }

    public void gH() {
        this.Hr.set(0);
    }

    void gI() {
        int -l_1_I = this.Hr.decrementAndGet();
        mb.n("SharkTcpControler", "[tcp_control][shark_conf]tryCloseConnectionAsyn, refCount: " + -l_1_I);
        if (-l_1_I <= 0) {
            this.Hr.set(0);
            this.Ho.onClose();
        }
    }

    void z(long -l_3_J) {
        Object obj = 1;
        long -l_3_J2 = 1000 * ((long) az().B);
        if (-l_3_J2 < -l_3_J) {
            obj = null;
        }
        if (obj != null) {
            -l_3_J = -l_3_J2;
        }
        oc -l_5_R = this;
        synchronized (this) {
            if (!this.Hs) {
                mb.n("SharkTcpControler", "[tcp_control][shark_conf] extendConnectOnSend(), markKeepConnection()");
                gJ();
                this.Hs = true;
            }
        }
        mb.n("SharkTcpControler", "[tcp_control][shark_conf] " + (-l_3_J / 1000));
        lm.eC().bH("action_keep_alive_after_send_end");
        lm.eC().a("action_keep_alive_after_send_end", -l_3_J, this.Ht);
    }
}
