package tmsdkobf;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;

public class og implements tmsdkobf.nx.a, tmsdkobf.oc.a, tmsdkobf.ol.b, tmsdkobf.ol.c, tmsdkobf.oq.a {
    private final Object Ba;
    private nl CT;
    private boolean CX;
    private nw Dm;
    protected tmsdkobf.od.a EW;
    private or HQ;
    private nm HR;
    private ka HS;
    private d HT;
    private oc HU;
    private ol HV;
    private int HW;
    private long HX;
    private long HY;
    private boolean HZ;
    private LinkedList<f> Ia;
    private byte Ib;
    private Handler Ic;
    private boolean Id;
    private op<f> Ie;
    private Context mContext;
    private HandlerThread te;

    public interface d {
        void a(tmsdkobf.nw.f fVar);
    }

    private abstract class c implements tmsdkobf.nw.b {
        final /* synthetic */ og If;
        int Ij = 0;
        int ey = 0;

        public c(og ogVar, int i, int i2) {
            this.If = ogVar;
            this.Ij = i;
            this.ey = i2;
        }

        public void a(boolean z, int i, int i2, ArrayList<ce> arrayList) {
            if (i != 0) {
                w(i, -1);
            } else if (this.Ij == 10999 && i == 0) {
                e(null);
            } else if (arrayList == null || arrayList.size() == 0) {
                w(-41250000, -1);
            } else {
                Object -l_5_R = arrayList.iterator();
                while (-l_5_R.hasNext()) {
                    ce -l_6_R = (ce) -l_5_R.next();
                    if (-l_6_R != null && -l_6_R.bz == this.Ij) {
                        if (-l_6_R.eB == 0 && -l_6_R.eC == 0) {
                            e(-l_6_R);
                            return;
                        } else {
                            w(-l_6_R.eB, -l_6_R.eC);
                            return;
                        }
                    }
                }
                w(-41250000, -1);
            }
        }

        protected abstract void e(ce ceVar);

        protected abstract void w(int i, int i2);
    }

    private class a extends c {
        final /* synthetic */ og If;
        private int Ig = 0;
        private String Ih = "";

        public a(og ogVar, int i, int i2, String str) {
            this.If = ogVar;
            super(ogVar, 10997, i);
            this.Ig = i2;
            this.Ih = str;
        }

        protected void e(ce ceVar) {
            mb.n("TmsTcpManager", "[tcp_control][f_p]fp success, mRetryTimes: " + this.Ig);
            this.If.Id = false;
            this.If.Ib = (byte) (byte) 1;
            this.If.HZ = false;
            nt.ga().a("TmsTcpManager", 997, this.ey, ceVar, 30, 0);
            nt.ga().bq(this.ey);
            this.If.a(2, null, 0, 0, true);
        }

        protected void w(int i, int i2) {
            int bj = ne.bj(i);
            int -l_3_I = (this.Ig < 1 && ne.bk(bj) && this.If.HW < 3 && this.If.HU.gG() > 0) ? 1 : 0;
            mb.s("TmsTcpManager", "[tcp_control][f_p]fp fail, retCode: " + bj + " dataRetCode: " + i2 + " mRetryTimes: " + this.Ig + " need retry? " + -l_3_I);
            this.If.Id = true;
            this.If.Ib = (byte) (byte) 0;
            this.If.HZ = true;
            nt.ga().a("TmsTcpManager", 997, this.ey, (ce) null, 30, bj);
            nt.ga().b(this.ey, -l_3_I == 0);
            this.If.a(3, null, bj, 0, true);
            if (-l_3_I == 0) {
                mb.n("TmsTcpManager", "[tcp_control][f_p]fp fail, should not retry, retCode: " + bj);
                return;
            }
            mb.n("TmsTcpManager", "[tcp_control][f_p]fp fail, ref count: " + this.If.HU.gG() + ", retry send fp in " + this.If.HU.az().E + "s");
            this.If.HW = this.If.HW + 1;
            og ogVar = this.If;
            String str = "delay_fp_retry:" + this.Ih + ":" + bj;
            int i3 = this.Ig + 1;
            this.Ig = i3;
            ogVar.a(11, str, i3, ((long) this.If.HU.az().E) * 1000, true);
        }
    }

    private class b extends c {
        final /* synthetic */ og If;
        private int Ig = 0;
        private String Ih = "";
        private byte Ii = (byte) 0;

        public b(og ogVar, int i, int i2, String str, byte b) {
            this.If = ogVar;
            super(ogVar, 10999, i);
            this.Ig = i2;
            this.Ih = str;
            this.Ii = (byte) b;
        }

        protected void e(ce ceVar) {
            mb.n("TmsTcpManager", "[tcp_control][h_b]hb success, helloSeq: " + this.Ii + " mRetryTimes: " + this.Ig + " reason: " + this.Ih);
            nt.ga().a("TmsTcpManager", 999, this.ey, ceVar, 30, 0);
            nt.ga().bq(this.ey);
            nt.ga().b(this.Ii);
        }

        protected void w(int i, int i2) {
            boolean z = false;
            mb.n("TmsTcpManager", "[tcp_control][h_b]hb fail, retCode: " + i + " dataRetCode: " + i2 + " helloSeq: " + this.Ii + " mRetryTimes: " + this.Ig + " reason: " + this.Ih);
            if (this.Ig >= 1) {
                mb.r("TmsTcpManager", "[tcp_control][h_b]hb fail again, mark disconnect not handled for reconnect");
                this.If.Id = true;
                this.If.Ib = (byte) (byte) 0;
            }
            int -l_3_I = (this.Ig < 1 && ne.bk(i) && this.If.HW < 3 && this.If.HU.gG() > 0) ? 1 : 0;
            nt.ga().a("TmsTcpManager", 999, this.ey, (ce) null, 30, i);
            nt ga = nt.ga();
            int i3 = this.ey;
            if (-l_3_I == 0) {
                z = true;
            }
            ga.b(i3, z);
            nt.ga().b(this.Ii);
            if (-l_3_I != 0) {
                mb.n("TmsTcpManager", "[tcp_control][h_b]hb fail, retry");
                og ogVar = this.If;
                String str = "hb_retry:" + this.Ih + ":" + i;
                int i4 = this.Ig + 1;
                this.Ig = i4;
                ogVar.a(13, str, i4, 2000, true);
            }
        }
    }

    private class e extends Handler {
        final /* synthetic */ og If;

        public e(og ogVar, Looper looper) {
            this.If = ogVar;
            super(looper);
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message message) {
            LinkedList -l_2_R;
            int -l_2_I;
            String -l_3_R;
            switch (message.what) {
                case 0:
                    mb.n("TmsTcpManager", "[tcp_control]handle MSG_SEND_TASK");
                    synchronized (this.If.Ba) {
                        f -l_2_R2 = (f) this.If.Ie.poll();
                        if (-l_2_R2 == null || -l_2_R2.Dh == null) {
                            mb.s("TmsTcpManager", "[tcp_control]no task for send");
                            return;
                        }
                    }
                    break;
                case 2:
                    -l_2_R = null;
                    synchronized (this.If.Ba) {
                        if (this.If.Ia != null) {
                            if (this.If.Ia.size() > 0) {
                                -l_2_R = (LinkedList) this.If.Ia.clone();
                                this.If.Ia.clear();
                            }
                        }
                    }
                    if (-l_2_R != null && -l_2_R.size() > 0) {
                        mb.n("TmsTcpManager", "[tcp_control]fp success. send waiting for fp tasks: " + -l_2_R.size());
                        synchronized (this.If.Ba) {
                            Object -l_4_R = -l_2_R.iterator();
                            while (-l_4_R.hasNext()) {
                                f -l_5_R = (f) -l_4_R.next();
                                if (-l_5_R != null) {
                                    this.If.Ie.add(-l_5_R);
                                }
                            }
                        }
                        this.If.a(0, null, 0, 0, true);
                    } else {
                        mb.n("TmsTcpManager", "[tcp_control]fp success, no task waiting for fp");
                    }
                    mb.n("TmsTcpManager", "[tcp_control][h_b]restartHeartBeat after fp success");
                    this.If.gV();
                    break;
                case 3:
                    this.If.bD(3);
                    synchronized (this.If.Ba) {
                        if (this.If.Ia != null && this.If.Ia.size() > 0) {
                            -l_2_R = (LinkedList) this.If.Ia.clone();
                            this.If.Ia.clear();
                            break;
                        }
                    }
                    break;
                case 4:
                    this.If.gT();
                    break;
                case 9:
                    mb.d("TmsTcpManager", "[tcp_control][f_p] handle: MSG_ON_CHANGE_TO_CONNECTED");
                    if (this.If.HU.gG() > 0) {
                        if (this.If.HW < 3) {
                            mb.d("TmsTcpManager", "[tcp_control][f_p] handle connected msg, ref count: " + this.If.HU.gG() + ", wait for network become stable and send fp in: " + this.If.HU.az().F + "s");
                            this.If.HW = this.If.HW + 1;
                            this.If.a(11, "delay_fp_network_connected", 0, 1000 * ((long) this.If.HU.az().F), true);
                            break;
                        }
                        mb.s("TmsTcpManager", "[tcp_control][f_p] handle connected msg, ref count: " + this.If.HU.gG() + ", mReconnectTimes over limit: " + this.If.HW);
                        break;
                    }
                    mb.d("TmsTcpManager", "[tcp_control][f_p] handle connected msg: ref connt <= 0, no need to reconnect");
                    return;
                case 11:
                    -l_2_I = message.arg1;
                    -l_3_R = "" + message.obj;
                    mb.n("TmsTcpManager", "[tcp_control] handle msg: MSG_DELAY_SEND_FIRST_PKG, retryTimes: " + -l_2_I + " reason: " + -l_3_R);
                    this.If.l(-l_2_I, -l_3_R);
                    break;
                case 12:
                    synchronized (this.If.Ba) {
                        if (this.If.Ia != null && this.If.Ia.size() > 0) {
                            -l_2_R = (LinkedList) this.If.Ia.clone();
                            this.If.Ia.clear();
                            break;
                        }
                    }
                    break;
                case 13:
                    if (this.If.Ib == (byte) 1) {
                        -l_2_I = message.arg1;
                        -l_3_R = "" + message.obj;
                        mb.n("TmsTcpManager", "[tcp_control] handle msg: MSG_SEND_HB, retryTimes: " + -l_2_I + " reason: " + -l_3_R);
                        this.If.j(-l_2_I, -l_3_R);
                        break;
                    }
                    mb.s("TmsTcpManager", "[tcp_control][f_p][h_b]handle msg: MSG_SEND_HB, fp not sent, donnot send hb!");
                    break;
            }
        }
    }

    private class f {
        public tmsdkobf.nw.f Dh = null;
        final /* synthetic */ og If;
        public kd Ik = null;
        public int eE = 0;

        public f(og ogVar, int i, kd kdVar, tmsdkobf.nw.f fVar) {
            this.If = ogVar;
            this.eE = i;
            this.Ik = kdVar;
            this.Dh = fVar;
        }
    }

    public og(nl nlVar, om omVar, tmsdkobf.od.a aVar, d dVar, nm nmVar, tmsdkobf.nw.d dVar2, nw nwVar) {
        this.mContext = null;
        this.HQ = null;
        this.CX = false;
        this.HW = 0;
        this.HX = 15000;
        this.HY = 0;
        this.HZ = false;
        this.Ia = new LinkedList();
        this.Ba = new Object();
        this.Ib = (byte) 0;
        this.te = null;
        this.Ic = null;
        this.Id = false;
        this.Ie = new op(new Comparator<f>(this) {
            final /* synthetic */ og If;

            {
                this.If = r1;
            }

            public int a(f fVar, f fVar2) {
                return kc.am(fVar2.eE) - kc.am(fVar.eE);
            }

            public /* synthetic */ int compare(Object obj, Object obj2) {
                return a((f) obj, (f) obj2);
            }
        });
        this.mContext = TMSDKContext.getApplicaionContext();
        this.HQ = new or(this.mContext, this, omVar);
        O(omVar.ax());
        this.te = ((ki) fj.D(4)).newFreeHandlerThread("sendHandlerThread");
        this.te.start();
        this.Ic = new e(this, this.te.getLooper());
        this.CT = nlVar;
        this.EW = aVar;
        this.HR = nmVar;
        this.HT = dVar;
        this.Dm = nwVar;
        this.HV = new ol(this.mContext, this, this);
        this.HU = new oc(nlVar, this);
        b(dVar2);
        nx.gs().a((tmsdkobf.nx.a) this);
    }

    private void A(long j) {
        mb.n("TmsTcpManager", "[tcp_control] checkKeepAliveAndResetHeartBeat()");
        gY();
        this.HU.z(j);
    }

    private void O(boolean z) {
        this.CX = z;
        if (z) {
            this.HX = 15000;
        }
    }

    private oh<Long, Integer, JceStruct> a(long j, h hVar) {
        if (hVar != null) {
            this.HU.c(hVar);
            Object -l_4_R = new d();
            -l_4_R.hash = hVar.hash;
            -l_4_R.j = hVar.j;
            return new oh(Long.valueOf(j), Integer.valueOf(1101), -l_4_R);
        }
        mb.s("TmsTcpManager", "[shark_push][shark_conf]handleSharkConfPush(), scSharkConf == null");
        return null;
    }

    private final void a(int i, Object obj, int i2, long j, boolean z) {
        if (this.Ic != null) {
            if (z) {
                this.Ic.removeMessages(i);
            }
            this.Ic.sendMessageDelayed(Message.obtain(this.Ic, i, i2, 0, obj), j);
        }
    }

    private void a(f fVar, int i) {
        if (fVar != null) {
            mb.n("TmsTcpManager", "[send_control] tcp fail, notify up level: " + i);
            if (this.HR != null) {
                this.HR.a(fVar.Dh, i);
            }
        }
    }

    private void b(tmsdkobf.nw.d dVar) {
        this.HS = new ka(this) {
            final /* synthetic */ og If;

            {
                this.If = r1;
            }

            public oh<Long, Integer, JceStruct> a(int i, long j, int i2, JceStruct jceStruct) {
                if (jceStruct != null) {
                    switch (i2) {
                        case 11101:
                            return this.If.a(j, (h) jceStruct);
                        default:
                            return null;
                    }
                }
                mb.s("TmsTcpManager", "[shark_push][shark_conf]onRecvPush() null == push");
                return null;
            }
        };
        mb.n("TmsTcpManager", "[shark_push][shark_conf]registerSharkPush()");
        dVar.a(0, 11101, new h(), 0, this.HS, false);
    }

    private final void bD(int i) {
        if (this.Ic != null) {
            this.Ic.removeMessages(i);
        }
    }

    private int cl(String str) {
        mb.n("TmsTcpManager", "[tcp_control]reconnect(), reason: " + str);
        int -l_2_I = this.HQ.hs();
        mb.n("TmsTcpManager", "[tcp_control]reconnect(), ret: " + -l_2_I);
        return -l_2_I;
    }

    private void g(tmsdkobf.nw.f fVar) {
        if (fVar != null && fVar.Ft != null && fVar.Ft.size() > 0) {
            Object -l_2_R = fVar.Ft.iterator();
            while (-l_2_R.hasNext()) {
                bw -l_3_R = (bw) -l_2_R.next();
                if (-l_3_R != null) {
                    nt.ga().a("TmsTcpManager", -l_3_R.bz, -l_3_R.ey, -l_3_R, 6);
                }
            }
        }
    }

    private int gS() {
        mb.n("TmsTcpManager", "[tcp_control]connectIfNeed()");
        int -l_1_I = 0;
        if (this.HQ.hl()) {
            mb.n("TmsTcpManager", "[tcp_control]connectIfNeed(), already connected");
        } else {
            -l_1_I = this.HQ.hm() ? this.HQ.hr() : -220000;
        }
        mb.n("TmsTcpManager", "[tcp_control]connectIfNeed(), ret: " + -l_1_I);
        return -l_1_I;
    }

    private void gT() {
        mb.n("TmsTcpManager", "[tcp_control]tryCloseConnectionSync()");
        if (this.HU.gG() <= 0) {
            this.HU.gH();
            mb.n("TmsTcpManager", "[tcp_control]tryCloseConnectionSync(), update: fp not send");
            this.Ib = (byte) 0;
            this.HW = 0;
            gX();
            synchronized (this.Ba) {
                this.Ie.clear();
            }
            long -l_1_J = System.currentTimeMillis();
            int -l_3_I = this.HQ.hq();
            long -l_4_J = System.currentTimeMillis() - -l_1_J;
            Object -l_6_R = this.HQ.gQ();
            if (!(-l_6_R == null || -l_6_R.B(true) == null)) {
                Object -l_7_R = new oe();
                Object -l_8_R = -l_6_R.B(true);
                -l_7_R.HB = -l_8_R.hd();
                -l_7_R.HC = String.valueOf(-l_8_R.getPort());
                -l_7_R.HE = String.valueOf(nh.w(this.mContext));
                -l_7_R.HH = this.HQ.hf();
                -l_7_R.errorCode = -l_3_I;
                -l_7_R.HG = -l_4_J;
                -l_7_R.e(this.CT);
            }
            return;
        }
        mb.d("TmsTcpManager", "[tcp_control]tryCloseConnectionSync(), not allow, ref connt: " + this.HU.gG());
    }

    private synchronized void gV() {
        gX();
        gW();
    }

    private synchronized void gW() {
        mb.n("TmsTcpManager", "[h_b]startHeartBeat");
        if (this.HV != null) {
            this.HV.start();
        }
    }

    private synchronized void gX() {
        mb.n("TmsTcpManager", "[h_b]stopHeartBeat");
        if (this.HV != null) {
            this.HV.stop();
        }
    }

    private synchronized void gY() {
        mb.n("TmsTcpManager", "[h_b]resetHeartBeat");
        if (this.HV != null) {
            this.HV.reset();
        }
    }

    private void h(tmsdkobf.nw.f fVar) {
        if (fVar != null && fVar.Ft != null && fVar.Ft.size() > 0) {
            Object -l_2_R = fVar.Ft.iterator();
            while (-l_2_R.hasNext()) {
                bw -l_3_R = (bw) -l_2_R.next();
                if (-l_3_R != null) {
                    nt.ga().a("TmsTcpManager", -l_3_R.bz, -l_3_R.ey, -l_3_R, 8);
                }
            }
        }
    }

    private void i(tmsdkobf.nw.f fVar) {
        if (fVar != null && fVar.Ft != null && fVar.Ft.size() > 0) {
            Object -l_2_R = fVar.Ft.iterator();
            while (-l_2_R.hasNext()) {
                bw -l_3_R = (bw) -l_2_R.next();
                if (-l_3_R != null) {
                    nt.ga().a("TmsTcpManager", -l_3_R.bz, -l_3_R.ey, -l_3_R, 7);
                }
            }
        }
    }

    private void j(int i, String str) {
        mb.n("TmsTcpManager", "[tcp_control][h_b][shark_conf]sendHeartBeat(), retryTimes: " + i + " reason: " + str);
        ArrayList -l_3_R = new ArrayList();
        Object -l_4_R = new bw();
        -l_4_R.bz = 999;
        -l_4_R.ey = ns.fW().fP();
        -l_3_R.add(-l_4_R);
        int -l_5_I = ns.fY().fZ();
        long -l_6_J = i >= 1 ? 60 : 30;
        Object -l_8_R = new tmsdkobf.nw.f(IncomingSmsFilterConsts.PAY_SMS, false, true, false, 0, -l_3_R, new b(this, -l_4_R.ey, i, str, -l_5_I), 1000 * -l_6_J);
        -l_8_R.Fx = (byte) -l_5_I;
        nt.ga().a(-l_4_R.ey, 1000 * -l_6_J, str);
        nt.ga().a(-l_5_I, -l_8_R.Fq);
        this.HT.a(-l_8_R);
    }

    private void l(int i, String str) {
        if (this.Ib == (byte) 1 || this.Ib == (byte) 2) {
            mb.n("TmsTcpManager", "[tcp_control][f_p]sending or received fp, no more send, is received ? " + (this.Ib == (byte) 1));
            return;
        }
        long -l_3_J = System.currentTimeMillis();
        if ((Math.abs(-l_3_J - this.HY) >= this.HX ? 1 : null) == null) {
            mb.s("TmsTcpManager", "[tcp_control][f_p]first pkg too frequency, send delay");
            a(12, null, 0, 0, true);
            a(11, "delay_too_freq:" + str, i, this.HX, true);
            return;
        }
        int -l_5_I = this.HU.az().F;
        if (np.fS().x(((long) -l_5_I) * 1000)) {
            mb.s("TmsTcpManager", "[tcp_control][f_p]net state changing, send fp delay(s): " + -l_5_I);
            a(11, "delay_waitfor_stable:" + str, i, ((long) -l_5_I) * 1000, true);
            return;
        }
        this.HY = -l_3_J;
        this.Ib = (byte) 2;
        bD(11);
        mb.s("TmsTcpManager", "[tcp_control][f_p]send first pkg, reason: " + str + " retryTimes: " + i);
        bw -l_6_R = new bw();
        -l_6_R.bz = 997;
        -l_6_R.ey = ns.fW().fP();
        -l_6_R.data = nh.a(this.mContext, null, -l_6_R.bz, -l_6_R);
        Object -l_7_R = new ArrayList();
        -l_7_R.add(-l_6_R);
        nt.ga().a(-l_6_R.ey, -1, str);
        this.HT.a(new tmsdkobf.nw.f(IncomingSmsFilterConsts.PAY_SMS, false, false, true, 0, -l_7_R, new a(this, -l_6_R.ey, i, str), 0));
    }

    public void a(int i, Object obj) {
        mb.d("TmsTcpManager", "[tcp_control]onTcpError(), errCode: " + i + " msg: " + obj);
        switch (i) {
            case 10:
            case 11:
            case 12:
                this.Id = true;
                this.Ib = (byte) 0;
                if (this.HU.gG() <= 0) {
                    return;
                }
                if (this.HW >= 3) {
                    mb.s("TmsTcpManager", "[tcp_control][f_p]tcp_connect_broken, ref count: " + this.HU.gG() + ", mReconnectTimes over limit: " + this.HW);
                    return;
                }
                mb.s("TmsTcpManager", "[tcp_control][f_p]tcp_connect_broken, ref count: " + this.HU.gG() + ", delay send fp in " + this.HU.az().E + "s");
                this.HW++;
                a(11, "delay_fp_for_connect_broken" + i, 0, 1000 * ((long) this.HU.az().E), true);
                return;
            default:
                return;
        }
    }

    public void bE(int i) {
        mb.d("TmsTcpManager", "[tcp_control]onTcpEvent(), eventCode: " + i);
    }

    public void d(int i, byte[] bArr) {
        mb.d("TmsTcpManager", "[tcp_control]onReceiveData()");
        this.HW = 0;
        if (this.CX || qg.bV(65539)) {
            if (!(bArr == null || nu.t(bArr))) {
                nv.a("TmsTcpManager", bArr);
            }
        }
        this.EW.a(true, 0, bArr, null);
    }

    void e(tmsdkobf.nw.f fVar) {
        mb.n("TmsTcpManager", "[tcp_control] sendCheckFirst()");
        if (this.HQ.hm() == 0) {
            mb.s("TmsTcpManager", "[tcp_control] sendCheckFirst(), no connect");
            this.EW.b(true, -40220000, fVar);
        } else if (lw.eJ()) {
            mb.s("TmsTcpManager", "[tcp_control] sendCheckFirst(), cmd could not connect");
            this.EW.b(true, -40230000, fVar);
        } else {
            Object -l_3_R = new f(this, 32, null, fVar);
            if (this.Ib == (byte) 1) {
                f(fVar);
            } else if (this.Ib != (byte) 2) {
                if (this.Ib == (byte) 0) {
                    if (fVar.Fl) {
                        mb.s("TmsTcpManager", "[tcp_control][f_p][h_b]sendCheckFirst(),fp is not sent ignore heartbeat");
                        return;
                    }
                    mb.n("TmsTcpManager", "[tcp_control] fp is not sent, send fp & enqueue this task");
                    h(fVar);
                    synchronized (this.Ba) {
                        this.Ia.add(-l_3_R);
                    }
                    a(11, "delay_send_for_others", 0, 0, true);
                }
            } else if (fVar.Fl) {
                mb.s("TmsTcpManager", "[tcp_control][f_p][h_b]sendCheckFirst(),sending fp ignore heartbeat");
            } else {
                mb.n("TmsTcpManager", "[tcp_control] sending fp, enqueue this task");
                i(fVar);
                synchronized (this.Ba) {
                    this.Ia.add(-l_3_R);
                }
            }
        }
    }

    void f(tmsdkobf.nw.f fVar) {
        mb.n("TmsTcpManager", "[tcp_control] send(), isFP: " + fVar.Fm + ", isHB: " + fVar.Fl);
        if (this.HQ.hm() == 0) {
            mb.s("TmsTcpManager", "[tcp_control] send(), no connect");
            this.EW.b(true, -40220000, fVar);
        } else if (lw.eJ()) {
            mb.s("TmsTcpManager", "[tcp_control] send(), cmd could not connect");
            this.EW.b(true, -40230000, fVar);
        } else {
            bD(4);
            g(fVar);
            if (!fVar.Fl) {
                A(fVar.Fs);
            }
            Object -l_3_R = new f(this, 32, null, fVar);
            synchronized (this.Ba) {
                this.Ie.add(-l_3_R);
            }
            a(0, null, 0, 0, true);
        }
    }

    synchronized void gA() {
        mb.n("TmsTcpManager", "get couldNotConnect cmd");
        if (lw.eJ()) {
            mb.n("TmsTcpManager", "could not connect");
            this.HU.gI();
        }
    }

    public void gC() {
        this.HU.gC();
    }

    public void gD() {
        this.HU.gD();
    }

    public void gP() {
        k(0, "tcp_control");
    }

    boolean gU() {
        if (this.Ib == (byte) 1) {
            mb.n("TmsTcpManager", "[tcp_control]guessTcpWillSucc(), fp succ, prefer tcp");
            return true;
        } else if (this.Ib != (byte) 2) {
            if (this.HY > 0) {
                -l_1_J = Math.abs(System.currentTimeMillis() - this.HY);
                if (!(-l_1_J <= 1800000)) {
                    mb.n("TmsTcpManager", "[tcp_control]guessTcpWillSucc(), over 30 mins since last fp, try again, prefer tcp: " + -l_1_J);
                    return true;
                } else if (this.HZ) {
                    mb.s("TmsTcpManager", "[tcp_control]guessTcpWillSucc(), fp failed within 30 mins, network not reconnected, prefer http: " + -l_1_J);
                    return false;
                } else {
                    mb.n("TmsTcpManager", "[tcp_control]guessTcpWillSucc(), no fp fail record or network reconnected within 30 mins, prefer tcp: " + -l_1_J);
                    return true;
                }
            }
            mb.n("TmsTcpManager", "[tcp_control]guessTcpWillSucc(), fp first time, prefer tcp");
            return true;
        } else {
            -l_1_J = Math.abs(System.currentTimeMillis() - this.HY);
            if (!(this.HY <= 0)) {
                if (!(-l_1_J >= 10000)) {
                    mb.n("TmsTcpManager", "[tcp_control]guessTcpWillSucc(), fp sending within 10s, prefer tcp: " + -l_1_J);
                    return true;
                }
            }
            mb.s("TmsTcpManager", "[tcp_control]guessTcpWillSucc(), fp sending over 10s, prefer http: " + -l_1_J);
            return false;
        }
    }

    public void gZ() {
        if (this.Ib != (byte) 1) {
            mb.s("TmsTcpManager", "[tcp_control][f_p][h_b]onHeartBeat(), fp not sent, donnot send hb!");
        } else {
            a(13, "onHeartBeat", 0, 0, true);
        }
    }

    public int ha() {
        return this.HU.az().interval;
    }

    void k(int i, String str) {
        a(11, "" + str, i, 0, true);
    }

    public void onClose() {
        a(4, null, 0, 0, true);
    }

    public void onConnected() {
        this.HZ = false;
        int -l_1_I = this.HU.gG();
        if (-l_1_I > 0) {
            mb.n("TmsTcpManager", "[tcp_control]onConnected(), with tcp ref, send MSG_ON_CHANGE_TO_CONNECTED, refCount: " + -l_1_I);
            a(9, null, 0, 0, true);
            return;
        }
        mb.n("TmsTcpManager", "[tcp_control]onConnected(), no tcp ref, ignore, refCount: " + -l_1_I);
    }

    public void onDisconnected() {
        String str = "TmsTcpManager";
        mb.n(str, "[tcp_control]onDisconnected(), update: disconnected & fp not send, refCount: " + this.HU.gG());
        this.Id = true;
        this.Ib = (byte) 0;
        bD(9);
    }
}
