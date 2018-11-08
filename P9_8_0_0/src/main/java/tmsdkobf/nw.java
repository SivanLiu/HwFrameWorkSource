package tmsdkobf;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.SparseArray;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class nw implements tmsdkobf.og.d {
    private final Object DA = new Object();
    private nl Dz;
    private od EE;
    private nq EF;
    private ni EG;
    private oi EH;
    private e EI;
    private SparseArray<pf> EJ = null;
    private f EK = null;
    private f EL = null;
    private ArrayList<f> EM = new ArrayList();
    private LinkedHashMap<Integer, f> EN = new LinkedHashMap();
    private ExecutorService EO;
    private boolean EP;
    private long EQ = 0;
    private boolean ER = false;
    private boolean ES = false;
    private long ET = 0;
    private long EU = 0;
    private c EV;
    private tmsdkobf.od.a EW = new tmsdkobf.od.a(this) {
        final /* synthetic */ nw Fb;

        {
            this.Fb = r1;
        }

        public void a(boolean z, int i, byte[] bArr, f fVar) {
            mb.n("SharkNetwork", "onFinish(), retCode: " + i);
            if (i != 0) {
                this.Fb.a(z, i, fVar);
            } else if (bArr != null) {
                mb.d("SharkNetwork", "onFinish() retData.length: " + bArr.length);
                int -l_5_I;
                if (nu.t(bArr)) {
                    -l_5_I = nt.ga().c(bArr[0]);
                    if (-l_5_I >= 0) {
                        f -l_7_R;
                        synchronized (this.Fb.EN) {
                            -l_7_R = (f) this.Fb.EN.get(Integer.valueOf(-l_5_I));
                        }
                        if (-l_7_R != null) {
                            this.Fb.a(z, -l_7_R, 0, 0, null);
                        }
                    }
                } else {
                    try {
                        Object -l_8_R = nn.r(bArr);
                        if (-l_8_R != null) {
                            f -l_9_R;
                            int -l_10_I;
                            cf -l_6_R = (cf) -l_8_R;
                            ArrayList -l_7_R2 = -l_6_R.eQ;
                            -l_5_I = -l_6_R.ez;
                            if (this.Fb.EK != null && this.Fb.EK.Fq == -l_5_I) {
                                -l_9_R = this.Fb.EK;
                            } else if (this.Fb.EL != null && this.Fb.EL.Fq == -l_5_I) {
                                -l_9_R = this.Fb.EL;
                            } else {
                                synchronized (this.Fb.EN) {
                                    -l_9_R = (f) this.Fb.EN.get(Integer.valueOf(-l_5_I));
                                }
                            }
                            if (-l_7_R2 != null) {
                                -l_10_I = 0;
                                Object -l_11_R = -l_7_R2.iterator();
                                while (-l_11_R.hasNext()) {
                                    ce -l_12_R = (ce) -l_11_R.next();
                                    mb.n("SharkNetwork_CMDID", "[" + -l_10_I + "]收包：cmd id:[" + -l_12_R.bz + "]seqNo:[" + -l_12_R.ey + "]refSeqNo:[" + -l_12_R.ez + "]retCode:[" + -l_12_R.eB + "]dataRetCode:[" + -l_12_R.eC + "]");
                                    -l_10_I++;
                                }
                            }
                            if (-l_7_R2 != null) {
                                nw.r(-l_7_R2);
                                mb.d("SharkNetwork", "onFinish() sharkSeq: " + -l_5_I + " ssTag: " + -l_9_R + " shark回包或push个数: " + -l_7_R2.size());
                                -l_10_I = this.Fb.s(-l_7_R2);
                                mb.n("SharkNetwork", "[rsa_key]onFinish() 密钥是否过期：" + (-l_10_I == 0 ? "否" : "是"));
                                if (-l_10_I == 0) {
                                    ArrayList -l_11_R2 = this.Fb.a(-l_9_R, z, -l_6_R, -l_7_R2);
                                    if (-l_11_R2 != null && -l_11_R2.size() > 0) {
                                        Object -l_12_R2 = -l_11_R2.iterator();
                                        while (-l_12_R2.hasNext()) {
                                            ce -l_13_R = (ce) -l_12_R2.next();
                                            if (-l_13_R != null) {
                                                nt.ga().a("SharkNetwork", -l_13_R.bz, -l_13_R.ez, -l_13_R, 17, i, bArr == null ? null : String.format("%d/%d", new Object[]{Integer.valueOf(bArr.length + 4), Integer.valueOf(-l_7_R2.size())}));
                                                oe -l_14_R = oe.bC(-l_13_R.ez);
                                                if (-l_14_R != null) {
                                                    -l_14_R.HE = String.valueOf(nh.w(this.Fb.mContext));
                                                    -l_14_R.errorCode = -l_13_R.eB;
                                                    -l_14_R.bB(-l_13_R.bz);
                                                    -l_14_R.f(this.Fb.Dz);
                                                }
                                            }
                                        }
                                    }
                                    this.Fb.a(z, -l_9_R, 0, -l_6_R.ey, -l_11_R2);
                                    this.Fb.gi();
                                } else {
                                    this.Fb.EP = true;
                                    this.Fb.vH.removeMessages(1);
                                    this.Fb.vH.sendEmptyMessageDelayed(1, 100);
                                    return;
                                }
                            }
                            mb.o("SharkNetwork", "onFinish() null == respSashimiList");
                            this.Fb.a(z, -l_9_R, -21000005, -l_6_R.ey, null);
                            return;
                        }
                        mb.o("SharkNetwork", "onFinish() null == obj");
                        this.Fb.a(z, -21000400, fVar);
                    } catch (Object -l_9_R2) {
                        mb.o("SharkNetwork", "onFinish() e: " + -l_9_R2.toString());
                        this.Fb.a(z, -21000400, fVar);
                    }
                }
            } else {
                mb.o("SharkNetwork", "onFinish() null == retData");
                this.Fb.a(z, -21000005, fVar);
            }
        }

        public void b(boolean z, int i, f fVar) {
            if (fVar == null) {
                mb.o("SharkNetwork", "onSendFailed(), isTcpChannel: " + z + " retCode: " + i);
            } else {
                mb.o("SharkNetwork", "onSendFailed(), isTcpChannel: " + z + " retCode: " + i + " seqNo: " + fVar.Fq);
            }
            if (i != 0) {
                this.Fb.a(z, i, fVar);
            }
        }
    };
    private boolean EX = true;
    private boolean EY = true;
    private long EZ = 0;
    private Handler Fa = new Handler(this, nu.getLooper()) {
        final /* synthetic */ nw Fb;

        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case 1:
                    this.Fb.b((f) message.obj);
                    return;
                default:
                    return;
            }
        }
    };
    private Context mContext;
    private Handler vH = new Handler(this, nu.getLooper()) {
        final /* synthetic */ nw Fb;

        private void b(final boolean z, final f fVar) {
            this.Fb.EO.submit(new Runnable(this) {
                final /* synthetic */ AnonymousClass4 Fe;

                public void run() {
                    Object -l_1_R;
                    Object -l_2_R;
                    if (this.Fe.Fb.EJ != null) {
                        if (!fVar.Fm) {
                            synchronized (this.Fe.Fb.EJ) {
                                pf -l_2_R2 = (pf) this.Fe.Fb.EJ.get(997);
                                if (-l_2_R2 != null) {
                                    if (!-l_2_R2.hI()) {
                                        mb.s("SharkNetwork", "[network_control] cloud cmd: fp donot connect, use http channel");
                                        fVar.Fo = true;
                                    }
                                }
                            }
                        }
                        -l_1_R = fVar.Ft;
                        if (-l_1_R != null && -l_1_R.size() > 0) {
                            mb.n("SharkNetwork", "[network_control] before control, sashimis.size(): " + -l_1_R.size());
                            -l_2_R = -l_1_R.iterator();
                            while (-l_2_R.hasNext()) {
                                bw -l_3_R = (bw) -l_2_R.next();
                                if (-l_3_R != null) {
                                    pf -l_4_R;
                                    synchronized (this.Fe.Fb.EJ) {
                                        -l_4_R = (pf) this.Fe.Fb.EJ.get(-l_3_R.bz);
                                    }
                                    if (-l_4_R != null) {
                                        if (-l_4_R.hI()) {
                                            -l_4_R.hJ();
                                        } else {
                                            -l_2_R.remove();
                                            nv.b("SharkNetwork", "network ctrl donot connect, cmdid : " + -l_3_R.bz, null, null);
                                            mb.s("SharkNetwork", "[network_control] cloud cmd: donot connect, cmdid : " + -l_3_R.bz);
                                            Object -l_5_R = new ce();
                                            -l_5_R.bz = -l_3_R.bz;
                                            -l_5_R.eB = -7;
                                            this.Fe.Fb.a(true, false, fVar, -20000007, 0, (ce) -l_5_R);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    -l_1_R = fVar.Ft;
                    if (-l_1_R != null && -l_1_R.size() > 0) {
                        mb.n("SharkNetwork", "[network_control] after control, sashimis.size(): " + -l_1_R.size());
                        try {
                            this.Fe.Fb.a(z, fVar);
                            return;
                        } catch (Object -l_2_R3) {
                            mb.e("SharkNetwork", -l_2_R3);
                            return;
                        }
                    }
                    mb.s("SharkNetwork", "[network_control] no sashimi can connect, control by cloud cmd!");
                }
            });
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    this.Fb.vH.removeMessages(0);
                    if (this.Fb.EK == null || message.arg1 != 1) {
                        if (this.Fb.EL == null || message.arg1 != 2) {
                            mb.o("SharkNetwork", "MSG_SHARK_SEND_VIP null");
                            break;
                        }
                        mb.n("SharkNetwork", "MSG_SHARK_SEND_VIP mSharkSendGuid");
                        b(true, this.Fb.EL);
                        break;
                    }
                    mb.n("SharkNetwork", "MSG_SHARK_SEND_VIP mSharkSendRsa");
                    b(false, this.Fb.EK);
                    break;
                    break;
                case 1:
                    mb.n("SharkNetwork", "MSG_SHARK_SEND");
                    this.Fb.vH.removeMessages(1);
                    Object -l_2_R = this.Fb.EF.ap();
                    if (TextUtils.isEmpty(-l_2_R.DW) || TextUtils.isEmpty(-l_2_R.DX)) {
                        mb.s("SharkNetwork", "[rsa_key] MSG_SHARK_SEND, without rsakey, handleOnNeedRsaKey()");
                        this.Fb.M(false);
                        return;
                    } else if (this.Fb.EP) {
                        mb.s("SharkNetwork", "[rsa_key] MSG_SHARK_SEND, rsakey expired, handleOnNeedRsaKey()");
                        this.Fb.M(true);
                        return;
                    } else if (this.Fb.EG.fB()) {
                        mb.s("SharkNetwork", "[cu_guid] MSG_SHARK_SEND, without guid, handleOnNeedGuid()");
                        this.Fb.gg();
                        return;
                    } else {
                        synchronized (this.Fb.EM) {
                            if (this.Fb.EM.size() > 0) {
                                ArrayList -l_3_R = (ArrayList) this.Fb.EM.clone();
                                this.Fb.EM.clear();
                                break;
                            }
                            return;
                        }
                    }
                    break;
                case 2:
                    this.Fb.vH.removeMessages(2);
                    mb.n("SharkNetwork", "[rsa_key]msg: MSG_SHARK_UPDATE_RSAKEY");
                    this.Fb.EO.submit(new Runnable(this) {
                        final /* synthetic */ AnonymousClass4 Fe;

                        {
                            this.Fe = r1;
                        }

                        public void run() {
                            this.Fe.Fb.vH.removeMessages(2);
                            if (this.Fe.Fb.EQ <= 0) {
                                int i = 1;
                            } else {
                                boolean z = false;
                            }
                            if (i == 0) {
                                if (Math.abs(System.currentTimeMillis() - this.Fe.Fb.EQ) > 60000) {
                                    i = 1;
                                } else {
                                    z = false;
                                }
                                if (i == 0) {
                                    mb.n("SharkNetwork", "[rsa_key]update rsa succ in 60s, no need to update now");
                                    synchronized (this.Fe.Fb.DA) {
                                        this.Fe.Fb.ER = false;
                                    }
                                    this.Fe.Fb.EP = false;
                                    this.Fe.Fb.vH.sendEmptyMessage(1);
                                    mb.n("SharkNetwork", "[rsa_key]update rsa succ in 60s, no need to update now, broadcast after 5s");
                                    this.Fe.Fb.vH.removeMessages(5);
                                    this.Fe.Fb.vH.sendEmptyMessageDelayed(5, 5000);
                                    return;
                                }
                            }
                            try {
                                this.Fe.Fb.EF.a(new tmsdkobf.nq.a(this) {
                                    final /* synthetic */ AnonymousClass2 Ff;

                                    {
                                        this.Ff = r1;
                                    }

                                    public void a(int i, int i2, int i3) {
                                        int bj = ne.bj(i3);
                                        mb.d("SharkNetwork", "[rsa_key]onUpdateFinish(), ret: " + bj);
                                        synchronized (this.Ff.Fe.Fb.DA) {
                                            this.Ff.Fe.Fb.ER = false;
                                        }
                                        nt.ga().a("SharkNetwork", i2, i, (ce) null, 30, bj);
                                        nt.ga().bq(i);
                                        if (bj != 0) {
                                            nq.a(this.Ff.Fe.Fb.mContext, bj, null);
                                        } else {
                                            nq.a(this.Ff.Fe.Fb.mContext, bj, this.Ff.Fe.Fb.ap());
                                        }
                                        this.Ff.Fe.Fb.bw(bj);
                                    }
                                });
                            } catch (Object -l_1_R) {
                                mb.o("SharkNetwork", "[rsa_key] MSG_SHARK_UPDATE_RSAKEY e: " + -l_1_R.toString());
                                synchronized (this.Fe.Fb.DA) {
                                    this.Fe.Fb.ER = false;
                                    this.Fe.Fb.bw(-20000014);
                                }
                            }
                        }
                    });
                    break;
                case 3:
                    mb.n("SharkNetwork", "[cu_guid]MSG_SHARK_GET_GUID");
                    this.Fb.vH.removeMessages(3);
                    this.Fb.EO.submit(new Runnable(this) {
                        final /* synthetic */ AnonymousClass4 Fe;

                        {
                            this.Fe = r1;
                        }

                        public void run() {
                            this.Fe.Fb.vH.removeMessages(3);
                            try {
                                this.Fe.Fb.EG.a(new tmsdkobf.ni.a(this) {
                                    final /* synthetic */ AnonymousClass3 Fg;

                                    {
                                        this.Fg = r1;
                                    }

                                    public void a(int i, int i2, int i3, String str) {
                                        int bj = ne.bj(i3);
                                        mb.d("SharkNetwork", "[cu_guid]onGuidFinish(), send broadcast, ret: " + bj);
                                        synchronized (this.Fg.Fe.Fb.DA) {
                                            this.Fg.Fe.Fb.ES = false;
                                        }
                                        nt.ga().a("SharkNetwork", i2, i, (ce) null, 30, bj);
                                        nt.ga().bq(i);
                                        ni.a(this.Fg.Fe.Fb.mContext, bj, str);
                                        this.Fg.Fe.Fb.bv(bj);
                                    }
                                });
                            } catch (Object -l_1_R) {
                                mb.o("SharkNetwork", "[cu_guid]register guid exception: " + -l_1_R.toString());
                                synchronized (this.Fe.Fb.DA) {
                                    this.Fe.Fb.ES = false;
                                    this.Fe.Fb.bv(-20000014);
                                }
                            }
                        }
                    });
                    break;
                case 4:
                    this.Fb.EO.submit(new Runnable(this) {
                        final /* synthetic */ AnonymousClass4 Fe;

                        {
                            this.Fe = r1;
                        }

                        public void run() {
                            if (this.Fe.Fb.EG != null) {
                                mb.r("SharkNetwork", "[cu_guid]deal msg: guid info changed, check update guid");
                                this.Fe.Fb.EG.a(true, null);
                            }
                        }
                    });
                    break;
                case 5:
                    nq.a(this.Fb.mContext, 0, this.Fb.ap());
                    break;
                case 6:
                    mb.n("SharkNetwork", "[cu_guid]handle: MSG_REQUEST_SENDPROCESS_GET_GUID");
                    ni.x(this.Fb.mContext);
                    break;
                case 7:
                    mb.n("SharkNetwork", "[rsa_key]handle: MSG_REQUEST_SENDPROCESS_UPDATE_RSAKEY");
                    nq.y(this.Fb.mContext);
                    break;
                case 8:
                    mb.n("SharkNetwork", "[cu_vid]deal msg: MSG_REGISTER_VID_IFNEED");
                    this.Fb.EH.gB();
                    break;
                case 9:
                    mb.n("SharkNetwork", "[cu_vid]deal msg: MSG_UPDATE_VID_IFNEED");
                    this.Fb.EH.c(0, false);
                    break;
            }
        }
    };

    public interface b {
        void a(boolean z, int i, int i2, ArrayList<ce> arrayList);
    }

    interface a {
        void a(int i, tmsdkobf.nq.b bVar);
    }

    public interface c {
        long a(boolean z, int i, ce ceVar);

        long b(boolean z, int i, ce ceVar);
    }

    public interface d {
        void a(long j, int i, JceStruct jceStruct, int i2, ka kaVar, boolean z);
    }

    public interface e {
        void a(jw jwVar);

        void a(a aVar);

        void b(jw jwVar);

        void b(a aVar);
    }

    public static class f {
        public int Fh = 0;
        public boolean Fi = false;
        public boolean Fj = false;
        public boolean Fk = false;
        public boolean Fl = false;
        public boolean Fm = false;
        public boolean Fn = false;
        public boolean Fo = false;
        public boolean Fp = false;
        public int Fq;
        public tmsdkobf.nq.b Fr;
        public long Fs;
        public ArrayList<bw> Ft;
        public b Fu;
        public long Fv = System.currentTimeMillis();
        public boolean Fw = false;
        public byte Fx = (byte) 0;
        public long Fy = -1;

        public f(int i, boolean z, boolean z2, boolean z3, long j, ArrayList<bw> arrayList, b bVar, long j2) {
            this.Fh = i;
            this.Fi = z;
            this.Fl = z2;
            this.Fm = z3;
            this.Fs = j;
            this.Ft = arrayList;
            this.Fu = bVar;
            this.Fq = ns.fX().fP();
            this.Fy = j2;
        }

        public boolean gp() {
            int -l_3_I = 1;
            long -l_1_J = Math.abs(System.currentTimeMillis() - this.Fv);
            if ((-l_1_J < 180000 ? 1 : 0) != 0) {
                -l_3_I = 0;
            }
            if (-l_3_I != 0) {
                nv.c("ocean", "[ocean][time_out]SharkNetwork.SharkSend.isTimeOut(), SharkSend.seqNoTag: " + this.Fq + " time(s): " + (-l_1_J / 1000), null, null);
                if (this.Ft != null) {
                    int -l_4_I = this.Ft.size();
                    for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
                        bw -l_6_R = (bw) this.Ft.get(-l_5_I);
                        if (-l_6_R != null) {
                            nv.c("ocean", "[ocean][time_out]SharkNetwork.SharkSend.isTimeOut(), cmdId|" + -l_6_R.bz + "|seqNo|" + -l_6_R.ey, null, null);
                        }
                    }
                }
            }
            return -l_3_I;
        }

        public boolean gq() {
            return this.Fl || this.Fm;
        }

        public boolean gr() {
            return this.Fi;
        }
    }

    public nw(Context context, nl nlVar, e eVar, c cVar, d dVar, boolean z, String str) {
        mb.n("SharkNetwork", "[shark_init]SharkNetwork() isTest: " + z + " serverAdd: " + str);
        this.mContext = context;
        this.Dz = nlVar;
        this.EI = eVar;
        this.EV = cVar;
        this.EF = new nq(context, this);
        this.EG = new ni(context, this, z);
        this.EH = new oi(context, this, z);
        this.EO = Executors.newSingleThreadExecutor();
        this.EE = new od(this.Dz.aB(), context, nlVar, z, this.EW, this, dVar, this, str);
        if (this.Dz.aB()) {
            a(dVar);
            this.EH.c(dVar);
            if (im.bG()) {
                gi();
            }
        }
    }

    private void M(boolean z) {
        int i = 0;
        if (z) {
            bt(3);
        } else {
            bt(2);
        }
        if (nu.aC()) {
            mb.r("SharkNetwork", "[rsa_key] handleOnNeedRsaKey(), isSemiSendProcess, regRsaKeyListener() & requestSendProcessUpdateRsaKey()");
            a -l_2_R = new a(this) {
                final /* synthetic */ nw Fb;

                {
                    this.Fb = r1;
                }

                public void a(int i, tmsdkobf.nq.b bVar) {
                    mb.n("SharkNetwork", "[rsa_key] IRsaKeyListener.onCallback(), isSemiSendProcess, unregRsaKeyListener(this) and call onRsaKeyUpdated(errCode)");
                    if (this.Fb.EI != null) {
                        this.Fb.EI.b((a) this);
                    }
                    this.Fb.bw(i);
                }
            };
            if (this.EI != null) {
                this.EI.a(-l_2_R);
            }
            long -l_3_J = 2000;
            if (this.EY) {
                this.EY = false;
                -l_3_J = 0;
            }
            this.vH.removeMessages(7);
            this.vH.sendEmptyMessageDelayed(7, -l_3_J);
            return;
        }
        StringBuilder append = new StringBuilder().append("[rsa_key] handleOnNeedRsaKey(), isSendProcess, triggerUpdateRsaKey() in(ms) ");
        String str = "SharkNetwork";
        if (z) {
            i = 2000;
        }
        mb.r(str, append.append(i).toString());
        y(!z ? 0 : 2000);
    }

    private final ArrayList<ce> a(f fVar, boolean z, cf cfVar, ArrayList<ce> arrayList) {
        if (arrayList == null) {
            return null;
        }
        Object -l_5_R = new ArrayList();
        int -l_7_I = arrayList.size();
        for (int -l_8_I = 0; -l_8_I < -l_7_I; -l_8_I++) {
            ce -l_6_R = (ce) arrayList.get(-l_8_I);
            if (-l_6_R != null) {
                mb.n("SharkNetwork", "checkFilterList(), rs.refSeqNo: " + -l_6_R.ez + " rs.cmd: " + -l_6_R.bz + " rs.retCode: " + -l_6_R.eB + " rs.dataRetCode: " + -l_6_R.eC + " rs.data.length: " + (-l_6_R.data == null ? 0 : -l_6_R.data.length));
                if (!a(z, cfVar, -l_6_R)) {
                    -l_5_R.add(-l_6_R);
                }
            }
        }
        return -l_5_R;
    }

    private oh<Long, Integer, JceStruct> a(long j, int i, by byVar) {
        if (byVar != null) {
            mb.r("SharkNetwork", "[cu_guid_p]handlePushRefreshGuid(), |pushId=" + j + "|serverShasimiSeqNo=" + i);
            this.EG.a(true, byVar.et);
            return null;
        }
        mb.s("SharkNetwork", "[cu_guid_p]handlePushRefreshGuid(), scPushRefreshGuid == null");
        return null;
    }

    private oh<Long, Integer, JceStruct> a(long j, int i, g gVar) {
        if (gVar != null) {
            Object -l_5_R = this.EE.gQ();
            if (-l_5_R != null) {
                -l_5_R.a(j, i, gVar);
            }
            mb.d("SharkNetwork", "[ip_list]report push status, |pushId=" + j);
            Object -l_6_R = new c();
            -l_6_R.hash = gVar.hash;
            return new oh(Long.valueOf(j), Integer.valueOf(156), -l_6_R);
        }
        mb.s("SharkNetwork", "[ip_list]handleHIPList(), scHIPList == null");
        return null;
    }

    private void a(d dVar) {
        Object -l_2_R = new ka(this) {
            final /* synthetic */ nw Fb;

            {
                this.Fb = r1;
            }

            public oh<Long, Integer, JceStruct> a(int i, long j, int i2, JceStruct jceStruct) {
                if (jceStruct != null) {
                    switch (i2) {
                        case 10155:
                            return this.Fb.a(j, i, (g) jceStruct);
                        case 15081:
                            return this.Fb.a(j, i, (by) jceStruct);
                        default:
                            return null;
                    }
                }
                mb.o("SharkNetwork", "[ip_list][cu_guid_p]onRecvPush() null == push");
                return null;
            }
        };
        dVar.a(0, 10155, new g(), 0, -l_2_R, false);
        dVar.a(0, 15081, new by(), 0, -l_2_R, false);
        mb.n("SharkNetwork", "[ip_list][cu_guid_p]registerSharkPush, Cmd_SCHIPList,Cmd_SCPushRefreshGuid: 10155,15081");
    }

    private void a(boolean z, int i, f fVar) {
        mb.n("SharkNetwork", "runError(), ret = " + i + " isTcpChannel: " + z);
        if (fVar != null) {
            Object -l_4_R = new ArrayList();
            if (this.EK != null && this.EK.Fq == fVar.Fq) {
                mb.n("SharkNetwork", "runError(), updating rsa, only callback rsa");
                -l_4_R.add(this.EK);
                bu(this.EK.Fq);
            } else if (this.EL != null && this.EL.Fq == fVar.Fq) {
                mb.n("SharkNetwork", "runError(), updating guid, only callback guid");
                -l_4_R.add(this.EL);
                bu(this.EL.Fq);
            } else {
                mb.n("SharkNetwork", "runError(), call back failed for this seqNo: " + fVar.Fq);
                -l_4_R.add(fVar);
                bu(fVar.Fq);
                synchronized (this.EM) {
                    mb.d("SharkNetwork", "runError(), callback failed for mSharkQueueWaiting, size(): " + this.EM.size());
                    -l_4_R.addAll(this.EM);
                    this.EM.clear();
                }
            }
            mb.n("SharkNetwork", "runError(), callback error, ret: " + i + " values.size(): " + -l_4_R.size());
            Object -l_5_R = -l_4_R.iterator();
            while (-l_5_R.hasNext()) {
                a(z, (f) -l_5_R.next(), i, 0, null);
            }
            return;
        }
        mb.s("SharkNetwork", "runError(), failedSharkSend == null");
    }

    private void a(boolean z, f fVar, int i, int i2, ArrayList<ce> arrayList) {
        a(false, z, fVar, i, i2, (ArrayList) arrayList);
    }

    private void a(boolean z, boolean z2, int i) {
        mb.d("SharkNetwork", "onSharkVipError(), retCode = " + i + " 事件： " + (!z2 ? "注册guid" : "交换密钥"));
        Object -l_4_R = new ArrayList();
        synchronized (this.EN) {
            mb.d("SharkNetwork", "onSharkVipError(), callback failed for all sending: " + this.EN.keySet());
            -l_4_R.addAll(this.EN.values());
            this.EN.clear();
        }
        synchronized (this.EM) {
            mb.d("SharkNetwork", "onSharkVipError(), callback failed for mSharkQueueWaiting, size(): " + this.EM.size());
            -l_4_R.addAll(this.EM);
            this.EM.clear();
        }
        Object -l_5_R = -l_4_R.iterator();
        while (-l_5_R.hasNext()) {
            a(z, (f) -l_5_R.next(), i, 0, null);
        }
    }

    private void a(boolean z, boolean z2, f fVar, int i, int i2, ArrayList<ce> arrayList) {
        if (fVar != null) {
            if (z) {
                if (fVar.Ft != null && fVar.Ft.size() > 0) {
                    fVar.Fu.a(z2, i, i2, arrayList);
                }
            }
            try {
                bu(fVar.Fq);
                fVar.Fu.a(z2, i, i2, arrayList);
            } catch (Object -l_7_R) {
                mb.c("SharkNetwork", "runError() callback crash", -l_7_R);
            }
        }
    }

    private void a(boolean z, boolean z2, f fVar, int i, int i2, ce ceVar) {
        ArrayList -l_7_R = new ArrayList();
        -l_7_R.add(ceVar);
        a(z, z2, fVar, i, i2, -l_7_R);
    }

    private boolean a(boolean z, cf cfVar, ce ceVar) {
        if (ceVar == null) {
            return false;
        }
        int -l_4_I = 0;
        if (nz.b(ceVar)) {
            this.EV.a(z, cfVar.ey, ceVar);
            nv.b("ocean", "[ocean]guid|" + this.EG.b() + "|push|" + "通道|" + (!z ? "http|" : "tcp|") + "sharkSeqNo|" + cfVar.ey + "|ECmd|" + ceVar.bz + "|seqNo|" + ceVar.ey + "|refSeqNo|" + ceVar.ez + "|ret|" + 0 + (ceVar.eO == null ? "" : "|pushId|" + ceVar.eO.ex), null, ceVar);
            qg.d(65541, -l_5_R);
            -l_4_I = 1;
        } else if (nz.c(ceVar)) {
            this.EV.b(z, cfVar.ey, ceVar);
            nv.b("ocean", "[ocean]guid|" + this.EG.b() + "|gift|" + "通道|" + (!z ? "http|" : "tcp|") + "sharkSeqNo|" + cfVar.ey + "|ECmd|" + ceVar.bz + "|seqNo|" + ceVar.ey + "|refSeqNo|" + ceVar.ez + "|ret|" + 0 + (ceVar.eO == null ? "" : "|pushId|" + ceVar.eO.ex), null, ceVar);
            qg.d(65541, -l_5_R);
            -l_4_I = 1;
        }
        return -l_4_I;
    }

    private void b(final f fVar) {
        if (fVar != null) {
            mb.d("SharkNetwork", "runTimeout(), will check timeout for sharkSend with seqNoTag: " + fVar.Fq);
            this.Fa.removeMessages(1, fVar);
            ((ki) fj.D(4)).addTask(new Runnable(this) {
                final /* synthetic */ nw Fb;

                public void run() {
                    f -l_1_R = this.Fb.bu(fVar.Fq);
                    if (-l_1_R != null) {
                        mb.n("SharkNetwork", "runTimeout(), sharkSend.seqNoTag: " + fVar.Fq + " isSent: " + fVar.Fw);
                        this.Fb.a(fVar.Fn, -l_1_R, !-l_1_R.Fw ? -21000020 : -21050000, 0, null);
                    }
                }
            }, "runTimeout");
        }
    }

    private void bt(int i) {
        synchronized (this.EM) {
            ArrayList -l_2_R = (ArrayList) this.EM.clone();
        }
        if (-l_2_R != null && -l_2_R.size() > 0) {
            Object -l_3_R = -l_2_R.iterator();
            while (-l_3_R.hasNext()) {
                f -l_4_R = (f) -l_3_R.next();
                if (!(-l_4_R == null || -l_4_R.Ft == null || -l_4_R.Ft.size() <= 0)) {
                    Object -l_5_R = -l_4_R.Ft.iterator();
                    while (-l_5_R.hasNext()) {
                        bw -l_6_R = (bw) -l_5_R.next();
                        if (-l_6_R != null) {
                            nt.ga().a("SharkNetwork", -l_6_R.bz, -l_6_R.ey, -l_6_R, i);
                        }
                    }
                }
            }
        }
    }

    private f bu(int i) {
        f fVar;
        mb.d("SharkNetwork", "removeSendingBySeqNoTag() seqNoTag: " + i);
        synchronized (this.EN) {
            fVar = (f) this.EN.remove(Integer.valueOf(i));
        }
        return fVar;
    }

    private void bv(int i) {
        if (i != 0) {
            int -l_2_I = i <= 0 ? -800000000 + i : Math.abs(-800000000) + i;
            mb.n("SharkNetwork", "[cu_guid] onGuidRegisterResult(), guid failed, call onSharkVipError(), " + -l_2_I);
            a(false, false, -l_2_I);
            return;
        }
        this.vH.sendEmptyMessage(1);
    }

    private void bw(int i) {
        if (i != 0) {
            int -l_2_I = i <= 0 ? -900000000 + i : Math.abs(-900000000) + i;
            mb.n("SharkNetwork", "[cu_guid] onRsaKeyUpdateResult(), rsa failed, call onSharkVipError(), " + -l_2_I);
            a(false, true, -l_2_I);
            return;
        }
        this.EQ = System.currentTimeMillis();
        this.EP = false;
        synchronized (this.DA) {
            if (this.ES) {
                mb.s("SharkNetwork", "[cu_guid] onRsaKeyUpdateResult(), update rsa succ, allow register guid!");
                this.ES = false;
            }
        }
        Object<f> -l_2_R = new ArrayList();
        synchronized (this.EN) {
            if (this.EN.size() > 0) {
                for (f -l_5_R : this.EN.values()) {
                    if (-l_5_R.Fj || -l_5_R.Fk) {
                        mb.n("SharkNetwork", "[cu_guid][cu_guid] onRsaKeyUpdateResult(), rsa or guid, should not revert and resend after rsa updated, rsa?" + -l_5_R.Fj + " guid?" + -l_5_R.Fk);
                    } else {
                        -l_2_R.add(-l_5_R);
                    }
                }
                this.EN.clear();
            }
        }
        if (-l_2_R.size() <= 0) {
            mb.n("SharkNetwork", "[cu_guid] onRsaKeyUpdateResult(), rsa succ, no need to revert and resend data");
        } else {
            mb.n("SharkNetwork", "[cu_guid] onRsaKeyUpdateResult(), rsa succ, revert and resend data, size: " + -l_2_R.size());
            for (f -l_4_R : -l_2_R) {
                c(-l_4_R);
            }
            synchronized (this.EM) {
                this.EM.addAll(-l_2_R);
            }
        }
        mb.n("SharkNetwork", "[cu_guid] onRsaKeyUpdateResult(), rsa succ, send MSG_SHARK_SEND");
        this.vH.sendEmptyMessage(1);
    }

    private void c(f fVar) {
        if (fVar == null || fVar.Ft == null || fVar.Fr == null || fVar.Fr.DX == null) {
            mb.o("SharkNetwork", "[rsa_key]revertClientSashimiData() something null");
            return;
        }
        Object -l_2_R = fVar.Ft.iterator();
        while (-l_2_R.hasNext()) {
            bw -l_3_R = (bw) -l_2_R.next();
            if (!(-l_3_R == null || -l_3_R.data == null)) {
                if ((-l_3_R.eE & 2) == 0) {
                    -l_3_R.data = nh.decrypt(-l_3_R.data, fVar.Fr.DX.getBytes());
                }
                if (-l_3_R.data == null) {
                    mb.o("SharkNetwork", "[rsa_key]revertClientSashimiData(), revert failed, cmd: " + -l_3_R.bz);
                } else {
                    mb.d("SharkNetwork", "[rsa_key]revertClientSashimiData(), revert succ, cmd: " + -l_3_R.bz + " len: " + -l_3_R.data.length);
                }
                nt.ga().a("SharkNetwork", -l_3_R.bz, -l_3_R.ey, -l_3_R, 13);
            }
        }
    }

    private void gg() {
        bt(4);
        if (nu.aC()) {
            mb.n("SharkNetwork", "[cu_guid] handleOnNeedGuid(), isSemiSendProcess, no guid, regGuidListener() & requestSendProcessRegisterGuid()");
            jw -l_1_R = new jw(this) {
                final /* synthetic */ nw Fb;

                {
                    this.Fb = r1;
                }

                public void c(int i, String str) {
                    mb.n("SharkNetwork", "[cu_guid] IGuidCallback.onCallback(), unregGuidListener(this) and call onGuidRegisterResult(errCode)");
                    if (this.Fb.EI != null) {
                        this.Fb.EI.b((jw) this);
                    }
                    this.Fb.bv(i);
                }
            };
            if (this.EI != null) {
                this.EI.a(-l_1_R);
            }
            long -l_2_J = 2000;
            if (this.EX) {
                this.EX = false;
                -l_2_J = 0;
            }
            this.vH.removeMessages(6);
            this.vH.sendEmptyMessageDelayed(6, -l_2_J);
            return;
        }
        gh();
    }

    private synchronized void gi() {
        Object obj = null;
        synchronized (this) {
            if (this.Dz.aB()) {
                if (this.EZ != 0) {
                    if (System.currentTimeMillis() - this.EZ > 300000) {
                        obj = 1;
                    }
                    if (obj == null) {
                        return;
                    }
                }
                this.EZ = System.currentTimeMillis();
                this.vH.removeMessages(8);
                this.vH.sendEmptyMessage(8);
                mb.n("SharkNetwork", "[cu_vid]triggerRegVidIfNeed(), send msg: MSG_REGISTER_VID_IFNEED in 5s");
                return;
            }
        }
    }

    private static void r(ArrayList<ce> arrayList) {
        if (arrayList != null && arrayList.size() > 0) {
            Object -l_1_R = arrayList.iterator();
            while (-l_1_R.hasNext()) {
                ce -l_2_R = (ce) -l_1_R.next();
                if (-l_2_R != null && -l_2_R.eB == 3) {
                    mb.n("SharkNetwork", "[shark_v4][shark_fin]mazu said need sharkfin, cmdId: " + -l_2_R.bz + " ClientSashimi.seqNo: " + -l_2_R.ez + " ServerSashimi.seqNo: " + -l_2_R.ey);
                    nh.fy();
                    break;
                }
            }
        }
    }

    private boolean s(ArrayList<ce> arrayList) {
        if (arrayList == null || arrayList.size() != 1) {
            return false;
        }
        ce -l_2_R = (ce) arrayList.get(0);
        if (-l_2_R == null) {
            return false;
        }
        return (2 != -l_2_R.eB ? 0 : 1) != 0;
    }

    protected tmsdkobf.nq.b a(boolean z, f fVar) {
        if (fVar == null) {
            return null;
        }
        Object -l_4_R;
        bw -l_5_R;
        tmsdkobf.nq.b -l_3_R = null;
        if (z) {
            -l_3_R = this.EF.ap();
            fVar.Fr = -l_3_R;
            -l_4_R = fVar.Ft.iterator();
            while (-l_4_R.hasNext()) {
                -l_5_R = (bw) -l_4_R.next();
                if (-l_5_R != null && -l_5_R.data != null && -l_5_R.data.length > 0 && (-l_5_R.eE & 2) == 0) {
                    -l_5_R.data = nh.encrypt(-l_5_R.data, -l_3_R.DX.getBytes());
                    if (-l_5_R.data == null) {
                        mb.o("SharkNetwork", "[ocean][rsa_key]encrypt failed, cmdId: " + -l_5_R.bz);
                    }
                }
            }
        }
        if (fVar.Ft != null && fVar.Ft.size() > 0) {
            -l_4_R = fVar.Ft.iterator();
            while (-l_4_R.hasNext()) {
                -l_5_R = (bw) -l_4_R.next();
                if (-l_5_R != null) {
                    if (-l_5_R.ez == 0) {
                        fVar.Fp = true;
                    }
                    nt.ga().a("SharkNetwork", -l_5_R.bz, -l_5_R.ey, -l_5_R, 5);
                }
            }
        }
        synchronized (this.EN) {
            mb.d("SharkNetwork", "spSend() sharkSend.seqNoTag: " + fVar.Fq);
            this.EN.put(Integer.valueOf(fVar.Fq), fVar);
        }
        this.Fa.sendMessageDelayed(Message.obtain(this.Fa, 1, fVar), !((fVar.Fy > 0 ? 1 : (fVar.Fy == 0 ? 0 : -1)) <= 0) ? fVar.Fy : 180000);
        this.EE.d(fVar);
        return -l_3_R;
    }

    public void a(int i, long j, boolean z, ArrayList<bw> arrayList, b bVar) {
        a(new f(i, false, false, false, j, arrayList, bVar, 0));
        if (z) {
            this.EG.a(false, null);
        }
    }

    protected void a(ArrayList<bw> arrayList, b bVar) {
        int -l_3_I = 0;
        Object -l_4_R = arrayList.iterator();
        while (-l_4_R.hasNext()) {
            bw -l_5_R = (bw) -l_4_R.next();
            mb.n("SharkNetwork_CMDID", "[" + -l_3_I + "]Rsa发包请求：cmd id:[" + -l_5_R.bz + "]seqNo:[" + -l_5_R.ey + "]refSeqNo:[" + -l_5_R.ez + "]retCode:[" + -l_5_R.eB + "]dataRetCode:[" + -l_5_R.eC + "]");
            -l_3_I++;
        }
        this.EK = new f(0, true, false, false, 0, arrayList, bVar, 0);
        this.EK.Fj = true;
        this.vH.obtainMessage(0, 1, 0).sendToTarget();
    }

    public void a(f fVar) {
        if (fVar != null && fVar.Fu != null && fVar.Ft != null && fVar.Ft.size() > 0) {
            synchronized (this.EM) {
                this.EM.add(fVar);
                mb.n("SharkNetwork", "asyncSendShark() mSharkQueueWaiting.size(): " + this.EM.size());
            }
            Object -l_3_R = fVar.Ft.iterator();
            while (-l_3_R.hasNext()) {
                bw -l_4_R = (bw) -l_3_R.next();
                if (-l_4_R != null) {
                    mb.n("SharkNetwork_CMDID", "[" + 0 + "]发包请求：cmd id:[" + -l_4_R.bz + "]seqNo:[" + -l_4_R.ey + "]refSeqNo:[" + -l_4_R.ez + "]retCode:[" + -l_4_R.eB + "]dataRetCode:[" + -l_4_R.eC + "]");
                    nt.ga().a("SharkNetwork", -l_4_R.bz, -l_4_R.ey, -l_4_R, 1);
                }
            }
            this.vH.sendEmptyMessage(1);
        }
    }

    protected tmsdkobf.nq.b ap() {
        return this.EF.ap();
    }

    public String b() {
        return this.EG.b();
    }

    public void b(int i, int i2, int i3) {
        if (i2 > 0) {
            Object -l_4_R;
            if (this.EJ == null) {
                -l_4_R = nw.class;
                synchronized (nw.class) {
                    if (this.EJ == null) {
                        this.EJ = new SparseArray();
                    }
                }
            }
            -l_4_R = new pf("network_control_" + i, (long) (i2 * CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY), i3);
            synchronized (this.EJ) {
                this.EJ.append(i, -l_4_R);
                mb.d("SharkNetwork", "[network_control]handleNetworkControl : cmdid|" + i + "|timeSpan|" + i2 + "|maxTimes|" + i3 + " size: " + this.EJ.size());
            }
        }
    }

    protected void b(ArrayList<bw> arrayList, b bVar) {
        int -l_3_I = 0;
        Object -l_4_R = arrayList.iterator();
        while (-l_4_R.hasNext()) {
            bw -l_5_R = (bw) -l_4_R.next();
            mb.n("SharkNetwork_CMDID", "[" + -l_3_I + "]Guid发包请求：cmd id:[" + -l_5_R.bz + "]seqNo:[" + -l_5_R.ey + "]refSeqNo:[" + -l_5_R.ez + "]retCode:[" + -l_5_R.eB + "]dataRetCode:[" + -l_5_R.eC + "]");
            -l_3_I++;
        }
        this.EL = new f(0, true, false, false, 0, arrayList, bVar, 0);
        this.EL.Fk = true;
        this.vH.obtainMessage(0, 2, 0).sendToTarget();
    }

    void fC() {
        mb.n("SharkNetwork", "[cu_guid]refreshGuid()");
        this.EG.fC();
    }

    void gh() {
        synchronized (this.DA) {
            if (this.ES) {
                if (!lr.a(System.currentTimeMillis(), this.EU, 3)) {
                    mb.s("SharkNetwork", "[cu_guid]registering guid, ignore");
                    return;
                }
            }
            this.ES = true;
            this.EU = System.currentTimeMillis();
            this.vH.removeMessages(3);
            this.vH.sendEmptyMessageDelayed(3, 1000);
        }
    }

    public og gj() {
        return this.EE.gj();
    }

    void gk() {
        mb.n("SharkNetwork", "[rsa_key]refreshRsaKey()");
        this.EF.refresh();
    }

    protected nl gl() {
        return this.Dz;
    }

    public void gm() {
        if (this.vH != null) {
            mb.r("SharkNetwork", "[cu_guid]send msg: guid info changed, check update guid in 15s");
            this.vH.removeMessages(4);
            this.vH.sendEmptyMessage(4);
        }
    }

    public void gn() {
        if (this.vH != null) {
            mb.n("SharkNetwork", "[cu_vid] updateVidIfNeed(), send MSG_UPDATE_VID_IFNEED in 2s");
            this.vH.removeMessages(9);
            this.vH.sendEmptyMessageDelayed(9, 2000);
        }
    }

    public void go() {
        gi();
    }

    public void onReady() {
        mb.r("SharkNetwork", "[cu_guid]onReady(), check update guid");
        this.EG.a(true, null);
    }

    void y(long j) {
        synchronized (this.DA) {
            if (this.ER) {
                if (!lr.a(System.currentTimeMillis(), this.ET, 3)) {
                    return;
                }
            }
            this.ER = true;
            this.ET = System.currentTimeMillis();
            this.vH.removeMessages(2);
            this.vH.sendEmptyMessageDelayed(2, j);
        }
    }
}
