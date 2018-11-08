package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import com.qq.taf.jce.JceStruct;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.BaseManagerC;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;

public class nz extends BaseManagerC implements tmsdkobf.nw.c, tmsdkobf.nw.d, tmsdkobf.nw.e {
    private static int GG = 0;
    private static final long[] GH = new long[]{100, 60000, 120000};
    private nl CT;
    private nw Dm;
    private ExecutorService EO;
    private TreeMap<Integer, oh<JceStruct, ka, c>> FQ = new TreeMap();
    private Handler FR = new Handler(this, Looper.getMainLooper()) {
        final /* synthetic */ nz GM;

        public void handleMessage(Message message) {
            switch (message.what) {
                case 11:
                    Object[] -l_2_R = (Object[]) ((Object[]) message.obj);
                    d -l_3_R = (d) -l_2_R[0];
                    if (-l_3_R.Gc <= 0) {
                        -l_3_R.Gk.onFinish(-l_3_R.Ec, ((Integer) -l_2_R[1]).intValue(), ((Integer) -l_2_R[2]).intValue(), ((Integer) -l_2_R[3]).intValue(), -l_3_R.Gi);
                        return;
                    } else if (-l_3_R.Ha != null) {
                        -l_3_R.Ha.a(-l_3_R.FM, -l_3_R.Gc, -l_3_R.Ec, ((Integer) -l_2_R[1]).intValue(), ((Integer) -l_2_R[2]).intValue(), ((Integer) -l_2_R[3]).intValue(), -l_3_R.GZ);
                        return;
                    } else {
                        return;
                    }
                default:
                    return;
            }
        }
    };
    private boolean GA = false;
    private LinkedList<no> GB = null;
    private boolean GC = false;
    private boolean GD = false;
    private boolean GE = false;
    private boolean GF = false;
    private List<jw> GI = new ArrayList();
    private List<jw> GJ = new ArrayList();
    private List<a> GK = new ArrayList();
    private a GL = null;
    private List<b> Go = new ArrayList();
    private ArrayList<d> Gp = new ArrayList();
    private ok<Long> Gq = new ok(CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY);
    private kj Gr;
    private boolean Gs = false;
    private boolean Gt = false;
    private boolean Gu = false;
    private boolean Gv = false;
    private boolean Gw = false;
    private boolean Gx = false;
    private boolean Gy = false;
    private boolean Gz = false;
    private final String TAG = "SharkProtocolQueue";
    private Context mContext;
    private Handler vH = new Handler(this, nu.getLooper()) {
        final /* synthetic */ nz GM;

        public void handleMessage(Message message) {
            int -l_2_I;
            Object -l_7_R;
            Object -l_3_R;
            List<b> -l_2_R;
            switch (message.what) {
                case 1:
                    this.GM.vH.removeMessages(1);
                    -l_2_I = 0;
                    e eVar = new e();
                    ArrayList -l_5_R = new ArrayList();
                    synchronized (this.GM.Gp) {
                        -l_7_R = this.GM.Gp.iterator();
                        while (-l_7_R.hasNext()) {
                            d -l_8_R = (d) -l_7_R.next();
                            int -l_3_I = 1;
                            if (this.GM.Gr != null) {
                                -l_3_I = this.GM.Gr.d(-l_8_R.Gf, -l_8_R.Gg);
                            }
                            if ((-l_8_R.Gj & 1073741824) != 0) {
                                if (-l_3_I == 0) {
                                    -l_5_R.add(-l_8_R);
                                } else {
                                    eVar.He.add(-l_8_R);
                                }
                            } else if (-l_8_R.Hc.cJ()) {
                                nt.ga().bp(-l_8_R.Ec);
                            } else if (-l_3_I == 0) {
                                -l_5_R.add(-l_8_R);
                            } else {
                                eVar.a(Integer.valueOf(-l_8_R.Ec), -l_8_R);
                            }
                            -l_2_I++;
                        }
                        this.GM.Gp.clear();
                        if (-l_5_R.size() > 0) {
                            this.GM.Gp.addAll(-l_5_R);
                        }
                    }
                    if (-l_2_I > 0) {
                        this.GM.EO.submit(eVar);
                        return;
                    }
                    return;
                case 2:
                    nu.Ev = true;
                    mb.d("SharkProtocolQueue", "[shark_init]=========== MSG_INIT_FINISH ==========");
                    synchronized (this.GM.Gp) {
                        -l_2_I = this.GM.Gp.size();
                    }
                    if (-l_2_I > 0) {
                        this.GM.vH.sendEmptyMessage(1);
                    }
                    if (this.GM.Gs) {
                        this.GM.N(false);
                    }
                    if (this.GM.Gt) {
                        this.GM.N(true);
                    }
                    if (this.GM.Gu) {
                        this.GM.gz();
                    }
                    if (this.GM.Gv) {
                        this.GM.gh();
                    }
                    if (this.GM.Gw) {
                        this.GM.onReady();
                    }
                    if (this.GM.Gx) {
                        this.GM.gA();
                    }
                    if (this.GM.Gy) {
                        this.GM.gm();
                    }
                    if (this.GM.Gz) {
                        this.GM.gn();
                    }
                    if (this.GM.GA) {
                        this.GM.gB();
                    }
                    if (this.GM.GC) {
                        this.GM.gx();
                    }
                    if (this.GM.GB != null) {
                        -l_3_R = this.GM.GB.iterator();
                        while (-l_3_R.hasNext()) {
                            no -l_4_R = (no) -l_3_R.next();
                            if (-l_4_R != null) {
                                this.GM.b(-l_4_R.ii, -l_4_R.DK, -l_4_R.DL);
                            }
                        }
                        this.GM.GB = null;
                    }
                    if (this.GM.GD) {
                        this.GM.GD = false;
                        String -l_3_R2 = this.GM.b();
                        if (!TextUtils.isEmpty(-l_3_R2)) {
                            mb.n("SharkProtocolQueue", "[cu_guid] notifyGuidGot on init finished");
                            this.GM.i(0, -l_3_R2);
                        }
                    }
                    if (this.GM.GE) {
                        this.GM.gC();
                    }
                    if (this.GM.GF) {
                        this.GM.gD();
                        return;
                    }
                    return;
                case 3:
                    mb.d("SharkProtocolQueue", "[shark_push]handle MSG_CLEAR_EXPIRED_PUSH");
                    -l_2_R = new ArrayList();
                    Collection -l_3_R3 = new ArrayList();
                    synchronized (this.GM.Go) {
                        if (this.GM.Go.size() > 0) {
                            long -l_5_J = System.currentTimeMillis();
                            for (b -l_8_R2 : this.GM.Go) {
                                if ((-l_5_J - -l_8_R2.GT < 600000 ? 1 : null) == null) {
                                    -l_2_R.add(-l_8_R2);
                                } else {
                                    -l_3_R3.add(-l_8_R2);
                                }
                            }
                            this.GM.Go.clear();
                            this.GM.Go.addAll(-l_3_R3);
                        }
                    }
                    mb.d("SharkProtocolQueue", "[shark_push]handle MSG_CLEAR_EXPIRED_PUSH, expired: " + -l_2_R.size() + " remain: " + -l_3_R3.size());
                    if (-l_2_R.size() > 0) {
                        for (b -l_5_R2 : -l_2_R) {
                            if (-l_5_R2.GW != 0) {
                                mb.d("SharkProtocolQueue", "[shark_push]no need to sendPushResp() for expired gift, cmd: " + -l_5_R2.GU.bz + " pushId: " + -l_5_R2.ex);
                            } else {
                                mb.d("SharkProtocolQueue", "[shark_push]sendPushResp() for expired push, cmd: " + -l_5_R2.GU.bz + " pushId: " + -l_5_R2.ex);
                                this.GM.a(-l_5_R2.GU.ey, -l_5_R2.ex, -l_5_R2.GU.bz, null, null, -2, -1000000001);
                            }
                        }
                        return;
                    }
                    return;
                case 4:
                    mb.d("SharkProtocolQueue", "[shark_push]handle MSG_CLEAR_PUSH_CACHE");
                    -l_2_R = new ArrayList();
                    synchronized (this.GM.Go) {
                        if (this.GM.Go.size() > 0) {
                            -l_2_R.addAll(this.GM.Go);
                            this.GM.Go.clear();
                        }
                    }
                    mb.d("SharkProtocolQueue", "[shark_push]handle MSG_CLEAR_PUSH_CACHE, " + -l_2_R.size() + " -> 0");
                    if (-l_2_R.size() > 0) {
                        for (b -l_4_R2 : -l_2_R) {
                            if (-l_4_R2.GW != 0) {
                                mb.d("SharkProtocolQueue", "[shark_push]no need to sendPushResp() on gift cleared, cmd: " + -l_4_R2.GU.bz + " pushId: " + -l_4_R2.ex);
                            } else {
                                mb.d("SharkProtocolQueue", "[shark_push]sendPushResp() on push cleared, cmd: " + -l_4_R2.GU.bz + " pushId: " + -l_4_R2.ex);
                                this.GM.a(-l_4_R2.GU.ey, -l_4_R2.ex, -l_4_R2.GU.bz, null, null, -2, -1000000001);
                            }
                        }
                        return;
                    }
                    return;
                case 5:
                    oh -l_3_R4;
                    -l_2_I = message.arg1;
                    mb.d("SharkProtocolQueue", "[shark_push]handle MSG_CHECK_CACHED_PUSH for cmd: " + -l_2_I);
                    synchronized (this.GM.FQ) {
                        -l_3_R4 = (oh) this.GM.FQ.get(Integer.valueOf(-l_2_I));
                    }
                    List -l_4_R3 = new ArrayList();
                    Collection -l_5_R3 = new ArrayList();
                    synchronized (this.GM.Go) {
                        if (this.GM.Go.size() > 0) {
                            for (b -l_8_R22 : this.GM.Go) {
                                if (-l_8_R22.GU.bz != -l_2_I) {
                                    -l_5_R3.add(-l_8_R22);
                                } else {
                                    -l_4_R3.add(-l_8_R22);
                                }
                            }
                            this.GM.Go.clear();
                            this.GM.Go.addAll(-l_5_R3);
                        }
                    }
                    mb.d("SharkProtocolQueue", "[shark_push]handle MSG_CHECK_CACHED_PUSH, fixed: " + -l_4_R3.size() + " remain: " + -l_5_R3.size());
                    if (-l_3_R4 != null && -l_4_R3 != null && -l_4_R3.size() > 0) {
                        oh -l_6_R = -l_3_R4;
                        List list = -l_4_R3;
                        final List list2 = -l_4_R3;
                        final int i = -l_2_I;
                        final oh ohVar = -l_3_R4;
                        ((ki) fj.D(4)).addTask(new Runnable(this) {
                            final /* synthetic */ AnonymousClass2 GQ;

                            public void run() {
                                for (b -l_2_R : list2) {
                                    if (-l_2_R.GW != 0) {
                                        mb.d("SharkProtocolQueue", "[shark_push]handle cached gift, cmd: " + i + " pushId: " + -l_2_R.ex);
                                        this.GQ.GM.b(-l_2_R.ex, -l_2_R.GU, -l_2_R.GV, ohVar);
                                    } else {
                                        mb.d("SharkProtocolQueue", "[shark_push]handle cached push, cmd: " + i + " pushId: " + -l_2_R.ex);
                                        this.GQ.GM.a(-l_2_R.ex, -l_2_R.GU, -l_2_R.GV, ohVar);
                                    }
                                }
                            }
                        }, "shark callback: check cached push");
                        return;
                    }
                    return;
                case 6:
                    mb.n("SharkProtocolQueue", "[shark_vip] handle: MSG_RESET_VIP_RULE, expired VipRule: " + this.GM.Gr);
                    this.GM.Gr = null;
                    mb.n("SharkProtocolQueue", "[shark_vip] triggle MSG_SEND_SHARK on VipRule expired ");
                    if (nu.Ev) {
                        this.GM.vH.sendEmptyMessage(1);
                        return;
                    }
                    return;
                case 7:
                    mb.n("SharkProtocolQueue", "[cu_guid]handle: MSG_REQUEST_REG_GUID");
                    ni.x(TMSDKContext.getApplicaionContext());
                    return;
                default:
                    return;
            }
        }
    };

    private final class a extends if {
        final /* synthetic */ nz GM;
        private boolean vL;

        private a(nz nzVar) {
            this.GM = nzVar;
        }

        private void k(Context context) {
            if (!this.vL) {
                Object -l_2_R;
                try {
                    -l_2_R = context.getPackageName();
                    Object -l_3_R = new IntentFilter();
                    -l_3_R.addAction(String.format("action.guid.got:%s", new Object[]{-l_2_R}));
                    -l_3_R.addAction(String.format("action.rsa.got:%s", new Object[]{-l_2_R}));
                    -l_3_R.addAction(String.format("action.reg.guid:%s", new Object[]{-l_2_R}));
                    -l_3_R.addAction(String.format("action.up.rsa:%s", new Object[]{-l_2_R}));
                    -l_3_R.addAction(String.format("action.d.a:%s", new Object[]{-l_2_R}));
                    context.registerReceiver(this, -l_3_R);
                    this.vL = true;
                } catch (Object -l_2_R2) {
                    mb.b("SharkProtocolQueue", "[cu_guid] register: " + -l_2_R2, -l_2_R2);
                    -l_2_R2.printStackTrace();
                }
            }
        }

        public void doOnRecv(final Context context, final Intent intent) {
            final Object -l_3_R = intent.getAction();
            ((ki) fj.D(4)).addTask(new Runnable(this) {
                final /* synthetic */ a GS;

                public void run() {
                    Object -l_1_R = context.getPackageName();
                    Object -l_2_R = String.format("action.guid.got:%s", new Object[]{-l_1_R});
                    Object -l_3_R = String.format("action.rsa.got:%s", new Object[]{-l_1_R});
                    Object -l_4_R = String.format("action.reg.guid:%s", new Object[]{-l_1_R});
                    Object -l_5_R = String.format("action.up.rsa:%s", new Object[]{-l_1_R});
                    Object -l_6_R = String.format("action.d.a:%s", new Object[]{-l_1_R});
                    int -l_7_I;
                    if (-l_2_R.equals(-l_3_R)) {
                        this.GS.GM.vH.removeMessages(7);
                        -l_7_I = intent.getIntExtra("k.rc", -1);
                        String -l_8_R = intent.getStringExtra("k.g");
                        if (-l_7_I == 0 && !nu.aB()) {
                            mb.n("SharkProtocolQueue", "[cu_guid] doOnRecv(), !sendProcess, refreshGuid on recv broadcast");
                            this.GS.GM.N(true);
                        }
                        mb.n("SharkProtocolQueue", "[cu_guid] doOnRecv(), notifyGuidGot on recv broadcast: " + -l_3_R);
                        this.GS.GM.i(-l_7_I, -l_8_R);
                    } else if (-l_3_R.equals(-l_3_R)) {
                        -l_7_I = intent.getIntExtra("k.rc", -1);
                        tmsdkobf.nq.b -l_8_R2 = null;
                        if (-l_7_I == 0) {
                            -l_8_R2 = new tmsdkobf.nq.b();
                            -l_8_R2.DX = intent.getStringExtra("k.r.k");
                            -l_8_R2.DW = intent.getStringExtra("k.r.s");
                            if (!nu.aB()) {
                                mb.n("SharkProtocolQueue", "[rsa_key] doOnRecv(), !sendProcess, refreshRsaKey on recv broadcast");
                                this.GS.GM.N(false);
                            }
                        }
                        mb.n("SharkProtocolQueue", "[rsa_key] doOnRecv(), notifyRsaKeyGot on recv broadcast: " + -l_3_R);
                        this.GS.GM.b(-l_7_I, -l_8_R2);
                    } else if (-l_4_R.equals(-l_3_R)) {
                        if (nu.aB()) {
                            mb.n("SharkProtocolQueue", "[rsa_key] doOnRecv(), triggerRegGuid on recv broadcast: " + -l_3_R);
                            this.GS.GM.gh();
                        }
                    } else if (-l_5_R.equals(-l_3_R)) {
                        if (nu.aB()) {
                            mb.n("SharkProtocolQueue", "[rsa_key] doOnRecv(), triggerUpdateRsaKey on recv broadcast: " + -l_3_R);
                            this.GS.GM.gz();
                        }
                    } else if (-l_6_R.equals(-l_3_R) && nu.aB()) {
                        try {
                            -l_7_I = intent.getIntExtra("k.sa", 0);
                            if (-l_7_I == 1) {
                                Object -l_8_R3 = intent.getExtras();
                                this.GS.GM.a((kj) -l_8_R3.getSerializable("v.r"), -l_8_R3.getLong("vt.m", 35000));
                            } else if (-l_7_I == 2) {
                                this.GS.GM.gx();
                            }
                        } catch (Object -l_7_R) {
                            mb.b("SharkProtocolQueue", "[shark_vip] doOnRecv(), setVipRule: " + -l_7_R, -l_7_R);
                        }
                    }
                }
            }, "GuidOrRsaKeyGotReceiver onRecv");
        }
    }

    private class b {
        final /* synthetic */ nz GM;
        long GT;
        ce GU;
        byte[] GV;
        int GW = 0;
        long ex;

        public b(nz nzVar, int i, long j, long j2, ce ceVar, byte[] bArr) {
            this.GM = nzVar;
            this.GW = i;
            this.GT = j;
            this.ex = j2;
            this.GU = ceVar;
            this.GV = bArr;
        }
    }

    public static class c {
        public boolean GX;
        public long mr;

        public c(boolean z, long j) {
            this.GX = z;
            this.mr = j;
        }
    }

    private class d {
        public int Ec;
        public int FM;
        public long Fv = System.currentTimeMillis();
        final /* synthetic */ nz GM;
        public byte[] GY;
        public byte[] GZ;
        public int Gc;
        public int Gf;
        public long Gg;
        public JceStruct Gh;
        public JceStruct Gi;
        public int Gj;
        public jy Gk;
        public jz Ha;
        public int Hb;
        public kd Hc;
        public int eB;
        public long ex;
        public long ov = -1;
        public long ow = 0;

        d(nz nzVar, int i, int i2, long j, int i3, JceStruct jceStruct, byte[] bArr, JceStruct jceStruct2, int i4, jy jyVar, jz jzVar) {
            this.GM = nzVar;
            this.FM = i;
            this.Gc = i2;
            this.Gg = j;
            this.Gf = i3;
            this.Gh = jceStruct;
            this.GY = bArr;
            this.Gi = jceStruct2;
            this.Gj = i4;
            this.Gk = jyVar;
            this.Ha = jzVar;
            this.Hc = new kd();
        }

        public boolean gp() {
            int -l_5_I = 1;
            long -l_1_J = Math.abs(System.currentTimeMillis() - this.Fv);
            if ((-l_1_J < (((this.ov > 0 ? 1 : (this.ov == 0 ? 0 : -1)) <= 0 ? 1 : 0) == 0 ? this.ov : 180000) ? 1 : 0) != 0) {
                -l_5_I = 0;
            }
            if (-l_5_I != 0) {
                Object -l_6_R = new StringBuilder();
                -l_6_R.append("cmdId|").append(this.Gf);
                -l_6_R.append("|mIpcSeqNo|").append(this.Gc);
                -l_6_R.append("|mSeqNo|").append(this.Ec);
                -l_6_R.append("|pushId|").append(this.ex);
                -l_6_R.append("|mCallerIdent|").append(this.Gg);
                -l_6_R.append("|callBackTimeout|").append(this.ov);
                -l_6_R.append("|time(s)|").append(-l_1_J / 1000);
                nv.c("ocean", "[ocean][time_out]SharkProtocolQueue.SharkSendTask.isTimeOut(), " + -l_6_R.toString(), null, null);
            }
            return -l_5_I;
        }
    }

    private class e implements Runnable {
        final /* synthetic */ nz GM;
        private TreeMap<Integer, d> Hd;
        private ArrayList<d> He;
        private Handler Hf;
        private Handler Hg;

        private e(nz nzVar) {
            this.GM = nzVar;
            this.Hd = new TreeMap();
            this.He = new ArrayList();
            this.Hf = new Handler(this, nu.getLooper()) {
                final /* synthetic */ e Hh;

                public void handleMessage(Message message) {
                    of -l_2_R = (of) message.obj;
                    ce -l_3_R = new ce();
                    -l_3_R.eB = -11050000;
                    -l_3_R.ez = message.what;
                    if (-l_2_R != null) {
                        -l_3_R.bz = -l_2_R.ii;
                    }
                    mb.o("SharkProtocolQueue", "接收超时：seq: " + -l_3_R.ez + " cmdId: " + -l_3_R.bz);
                    this.Hh.d(-l_3_R);
                }
            };
            this.Hg = new Handler(this, nu.getLooper()) {
                final /* synthetic */ e Hh;

                public void handleMessage(Message message) {
                    switch (message.what) {
                        case 1:
                            ce -l_2_R = new ce();
                            -l_2_R.eB = -10000017;
                            -l_2_R.ez = message.arg1;
                            -l_2_R.bz = message.arg2;
                            mb.o("SharkProtocolQueue", "[time_out]发送请求超时： seq: " + -l_2_R.ez + " cmdId: " + -l_2_R.bz);
                            this.Hh.d(-l_2_R);
                            return;
                        default:
                            return;
                    }
                }
            };
        }

        private void a(ce ceVar, d dVar, Integer num, Integer num2, Integer num3) {
            dVar.Hc.setState(2);
            final int -l_6_I = ne.bj(num2.intValue());
            if (ceVar != null) {
                nt.ga().a("SharkProtocolQueue", num.intValue(), ceVar.ez, ceVar, 30, -l_6_I);
                nt.ga().bq(ceVar.ez);
            } else {
                nt.ga().a("SharkProtocolQueue", num.intValue(), dVar.Ec, ceVar, 30, -l_6_I);
                nt.ga().bq(dVar.Ec);
            }
            if (dVar.Gk != null || dVar.Ha != null) {
                switch (kc.al(dVar.Gj)) {
                    case 8:
                        this.GM.FR.sendMessage(this.GM.FR.obtainMessage(11, new Object[]{dVar, num, Integer.valueOf(-l_6_I), num3}));
                        break;
                    case 16:
                        if (dVar.Ha == null || dVar.Gc <= 0) {
                            dVar.Gk.onFinish(dVar.Ec, num.intValue(), -l_6_I, num3.intValue(), dVar.Gi);
                            break;
                        } else {
                            dVar.Ha.a(dVar.FM, dVar.Gc, dVar.Ec, num.intValue(), -l_6_I, num3.intValue(), dVar.GZ);
                            break;
                        }
                        break;
                    default:
                        final d dVar2 = dVar;
                        final Integer num4 = num;
                        final Integer num5 = num3;
                        Object -l_8_R = new Runnable(this) {
                            final /* synthetic */ e Hh;

                            public void run() {
                                if (dVar2.Ha != null && dVar2.Gc > 0) {
                                    dVar2.Ha.a(dVar2.FM, dVar2.Gc, dVar2.Ec, num4.intValue(), -l_6_I, num5.intValue(), dVar2.GZ);
                                } else {
                                    dVar2.Gk.onFinish(dVar2.Ec, num4.intValue(), -l_6_I, num5.intValue(), dVar2.Gi);
                                }
                            }
                        };
                        if (num.intValue() != 2016 && num.intValue() != 12016) {
                            ((ki) fj.D(4)).addTask(-l_8_R, "shark callback");
                            break;
                        } else {
                            ((ki) fj.D(4)).a(-l_8_R, "shark callback(urgent)");
                            break;
                        }
                        break;
                }
            }
        }

        private void b(boolean z, int i, int i2, ArrayList<ce> arrayList) {
            if (i != 0) {
                by(i);
                return;
            }
            Object -l_5_R = arrayList.iterator();
            while (-l_5_R.hasNext()) {
                ce -l_6_R = (ce) -l_5_R.next();
                if (bx(-l_6_R.ez)) {
                    d(-l_6_R);
                } else if (nz.b(-l_6_R)) {
                    this.GM.a(z, i2, -l_6_R);
                } else if (nz.c(-l_6_R)) {
                    this.GM.b(z, i2, -l_6_R);
                } else {
                    mb.s("SharkProtocolQueue", "No callback xx: cmd : " + -l_6_R.bz + " seqNo : " + -l_6_R.ey + " refSeqNo : " + -l_6_R.ez);
                }
            }
        }

        private void by(int i) {
            Object<Entry> -l_2_R = gw();
            synchronized (this.Hd) {
                this.Hd.clear();
            }
            for (Entry -l_4_R : -l_2_R) {
                try {
                    a(null, (d) -l_4_R.getValue(), Integer.valueOf(((d) -l_4_R.getValue()).Gf), Integer.valueOf(i), Integer.valueOf(-1));
                } catch (Object -l_5_R) {
                    mb.a("SharkProtocolQueue", "callback crash", -l_5_R);
                }
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void d(ce ceVar) {
            JceStruct -l_2_R = null;
            Object -l_3_R = null;
            this.Hf.removeMessages(ceVar.ez);
            synchronized (this.Hd) {
                d -l_4_R = (d) this.Hd.get(Integer.valueOf(ceVar.ez));
                if (-l_4_R != null) {
                    this.Hd.remove(Integer.valueOf(ceVar.ez));
                } else {
                    return;
                }
            }
            if (-l_4_R.GZ != -l_3_R) {
                -l_4_R.GZ = -l_3_R;
            }
            if (-l_4_R.Gi != -l_2_R) {
                -l_4_R.Gi = -l_2_R;
            }
            try {
                a(ceVar, -l_4_R, Integer.valueOf(ceVar.bz), Integer.valueOf(ceVar.eB), Integer.valueOf(ceVar.eC));
            } catch (Object -l_5_R) {
                mb.b("SharkProtocolQueue", "callback crash", -l_5_R);
            }
        }

        public void a(Integer num, d dVar) {
            this.Hd.put(num, dVar);
        }

        public boolean bx(int i) {
            boolean containsKey;
            synchronized (this.Hd) {
                containsKey = this.Hd.containsKey(Integer.valueOf(i));
            }
            return containsKey;
        }

        public Set<Entry<Integer, d>> gw() {
            TreeMap -l_1_R;
            synchronized (this.Hd) {
                -l_1_R = (TreeMap) this.Hd.clone();
            }
            return -l_1_R.entrySet();
        }

        public void run() {
            long -l_1_J = 0;
            ArrayList -l_3_R = new ArrayList();
            ArrayList -l_4_R = new ArrayList();
            ArrayList -l_5_R = new ArrayList();
            ArrayList -l_6_R = new ArrayList();
            for (Entry -l_9_R : gw()) {
                bw -l_10_R;
                if (!((d) -l_9_R.getValue()).Hc.cJ()) {
                    if (((d) -l_9_R.getValue()).gp()) {
                        this.Hg.obtainMessage(1, ((d) -l_9_R.getValue()).Ec, ((d) -l_9_R.getValue()).Gf).sendToTarget();
                    } else {
                        ((d) -l_9_R.getValue()).Hc.setState(1);
                        -l_10_R = new bw();
                        -l_10_R.bz = ((d) -l_9_R.getValue()).Gf;
                        -l_10_R.ey = ((d) -l_9_R.getValue()).Ec;
                        -l_10_R.eA = ((d) -l_9_R.getValue()).Gg;
                        -l_10_R.ez = 0;
                        -l_10_R.data = null;
                        if (((d) -l_9_R.getValue()).GY == null) {
                            -l_10_R.data = nh.a(this.GM.mContext, ((d) -l_9_R.getValue()).Gh, -l_10_R.bz, -l_10_R);
                        } else {
                            try {
                                -l_10_R.data = nh.a(this.GM.mContext, (byte[]) ((d) -l_9_R.getValue()).GY, -l_10_R.bz, -l_10_R);
                            } catch (Throwable -l_1_R) {
                                mb.c("SharkProtocolQueue", "run shark task e: " + -l_1_R.toString(), -l_1_R);
                                by(-10001200);
                                return;
                            }
                        }
                        long -l_11_J = ((d) -l_9_R.getValue()).ov;
                        if ((-l_11_J > 0 ? 1 : null) == null) {
                            -l_11_J = 180000;
                        }
                        mb.r("SharkProtocolQueue", "[shark_timer]对seq: " + -l_10_R.ey + "计时(ms): " + -l_11_J);
                        this.Hf.sendMessageDelayed(Message.obtain(this.Hf, -l_10_R.ey, new of(-l_10_R.bz)), -l_11_J);
                        if ((((d) -l_9_R.getValue()).Gj & 2048) != 0) {
                            -l_3_R.add(-l_10_R);
                        } else if ((((d) -l_9_R.getValue()).Gj & 512) != 0) {
                            -l_4_R.add(-l_10_R);
                        } else if ((((d) -l_9_R.getValue()).Gj & IncomingSmsFilterConsts.PAY_SMS) == 0) {
                            -l_6_R.add(-l_10_R);
                        } else {
                            -l_5_R.add(-l_10_R);
                        }
                        nt.ga().a("SharkProtocolQueue", -l_10_R.bz, -l_10_R.ey, -l_10_R, 0);
                        if ((((d) -l_9_R.getValue()).ow <= -l_1_J ? 1 : null) == null) {
                            -l_1_J = ((d) -l_9_R.getValue()).ow;
                        }
                    }
                }
            }
            Object -l_8_R = this.He.iterator();
            while (-l_8_R.hasNext()) {
                d -l_9_R2 = (d) -l_8_R.next();
                if (!-l_9_R2.gp()) {
                    -l_10_R = new bw();
                    -l_10_R.bz = -l_9_R2.Gf;
                    -l_10_R.ey = ns.fW().fP();
                    -l_10_R.ez = -l_9_R2.Ec;
                    -l_10_R.data = null;
                    -l_10_R.eB = -l_9_R2.eB;
                    -l_10_R.eC = -l_9_R2.Hb;
                    bv -l_11_R = new bv();
                    -l_11_R.ex = -l_9_R2.ex;
                    -l_10_R.eD = -l_11_R;
                    mb.n("SharkProtocolQueue", "resp push, seqNo: " + -l_10_R.ey + " pushId: " + -l_9_R2.ex);
                    try {
                        if (-l_9_R2.GY == null) {
                            -l_10_R.data = nh.a(this.GM.mContext, -l_9_R2.Gh, -l_10_R.bz, -l_10_R);
                        } else {
                            -l_10_R.data = nh.a(this.GM.mContext, (byte[]) -l_9_R2.GY, -l_10_R.bz, -l_10_R);
                        }
                    } catch (Exception e) {
                    }
                    if ((-l_9_R2.Gj & 2048) != 0) {
                        -l_3_R.add(-l_10_R);
                    } else if ((-l_9_R2.Gj & 512) != 0) {
                        -l_4_R.add(-l_10_R);
                    } else if ((-l_9_R2.Gj & IncomingSmsFilterConsts.PAY_SMS) == 0) {
                        -l_6_R.add(-l_10_R);
                    } else {
                        -l_5_R.add(-l_10_R);
                    }
                    nt.ga().a("SharkProtocolQueue", -l_10_R.bz, -l_10_R.ey, -l_10_R, 0);
                } else if (-l_9_R2.Gf != 1103) {
                    mb.o("SharkProtocolQueue", "[time_out]发送push的自动回包超时： mSeqNo: " + -l_9_R2.Ec + " pushId: " + -l_9_R2.ex + " mCmdId: " + -l_9_R2.Gf);
                } else {
                    mb.o("SharkProtocolQueue", "[time_out]发送push的业务回包超时： mSeqNo: " + -l_9_R2.Ec + " pushId: " + -l_9_R2.ex);
                }
            }
            if (-l_3_R.size() > 0) {
                this.GM.Dm.a(2048, -l_1_J, true, -l_3_R, new tmsdkobf.nw.b(this) {
                    final /* synthetic */ e Hh;

                    {
                        this.Hh = r1;
                    }

                    public void a(boolean z, int i, int i2, ArrayList<ce> arrayList) {
                        this.Hh.b(z, i, i2, arrayList);
                    }
                });
            }
            if (-l_4_R.size() > 0) {
                this.GM.Dm.a(512, -l_1_J, true, -l_4_R, new tmsdkobf.nw.b(this) {
                    final /* synthetic */ e Hh;

                    {
                        this.Hh = r1;
                    }

                    public void a(boolean z, int i, int i2, ArrayList<ce> arrayList) {
                        this.Hh.b(z, i, i2, arrayList);
                    }
                });
            }
            if (-l_5_R.size() > 0) {
                this.GM.Dm.a((int) IncomingSmsFilterConsts.PAY_SMS, -l_1_J, true, -l_5_R, new tmsdkobf.nw.b(this) {
                    final /* synthetic */ e Hh;

                    {
                        this.Hh = r1;
                    }

                    public void a(boolean z, int i, int i2, ArrayList<ce> arrayList) {
                        this.Hh.b(z, i, i2, arrayList);
                    }
                });
            }
            if (-l_6_R.size() > 0) {
                this.GM.Dm.a(0, -l_1_J, true, -l_6_R, new tmsdkobf.nw.b(this) {
                    final /* synthetic */ e Hh;

                    {
                        this.Hh = r1;
                    }

                    public void a(boolean z, int i, int i2, ArrayList<ce> arrayList) {
                        this.Hh.b(z, i, i2, arrayList);
                    }
                });
            }
        }
    }

    private void a(long j, ce ceVar, byte[] bArr, oh<JceStruct, ka, c> ohVar) {
        Object -l_7_R;
        Throwable -l_8_R;
        oh -l_8_R2;
        JceStruct jceStruct = null;
        if (ceVar.data != null) {
            if (((c) ohVar.Il).GX) {
                try {
                    -l_7_R = nh.a(this.mContext, bArr, ceVar.data, ceVar.eE);
                } catch (Throwable -l_8_R3) {
                    mb.b("SharkProtocolQueue", "[shark_push]handleCallbackForPush(), dataForReceive2JceBytes exception: " + -l_8_R3, -l_8_R3);
                    a(ceVar.ey, j, ceVar.bz, null, null, -1);
                }
                if (((c) ohVar.Il).GX) {
                    -l_8_R2 = ((ka) ohVar.second).a(ceVar.ey, j, ceVar.bz, jceStruct);
                } else {
                    -l_8_R2 = ((kb) ohVar.second).a(ceVar.ey, j, ceVar.bz, -l_7_R);
                }
                if (-l_8_R2 != null) {
                    mb.n("SharkProtocolQueue", "[shark_push]handleCallbackForPush(), donot send PushStatus for user: |pushId|" + j + "|cmd|" + ceVar.bz);
                }
                mb.n("SharkProtocolQueue", "[shark_push]handleCallbackForPush(), send PushStatus for user: |pushId|" + j + "|cmd|" + -l_8_R2.second + "|JceStruct|" + -l_8_R2.Il);
                a(ceVar.ey, j, ((Integer) -l_8_R2.second).intValue(), (JceStruct) -l_8_R2.Il, null, 1);
                return;
            } else if (ohVar.first != null) {
                try {
                    jceStruct = nh.a(this.mContext, bArr, ceVar.data, (JceStruct) ohVar.first, true, ceVar.eE);
                } catch (Throwable -l_8_R32) {
                    mb.b("SharkProtocolQueue", "[shark_push]handleCallbackForPush(), dataForReceive2JceStruct exception: " + -l_8_R32, -l_8_R32);
                    a(ceVar.ey, j, ceVar.bz, null, null, -1);
                }
            }
        }
        -l_7_R = null;
        try {
            if (((c) ohVar.Il).GX) {
                -l_8_R2 = ((kb) ohVar.second).a(ceVar.ey, j, ceVar.bz, -l_7_R);
            } else {
                -l_8_R2 = ((ka) ohVar.second).a(ceVar.ey, j, ceVar.bz, jceStruct);
            }
            if (-l_8_R2 != null) {
                mb.n("SharkProtocolQueue", "[shark_push]handleCallbackForPush(), send PushStatus for user: |pushId|" + j + "|cmd|" + -l_8_R2.second + "|JceStruct|" + -l_8_R2.Il);
                a(ceVar.ey, j, ((Integer) -l_8_R2.second).intValue(), (JceStruct) -l_8_R2.Il, null, 1);
                return;
            }
            mb.n("SharkProtocolQueue", "[shark_push]handleCallbackForPush(), donot send PushStatus for user: |pushId|" + j + "|cmd|" + ceVar.bz);
        } catch (Throwable -l_8_R4) {
            mb.b("SharkProtocolQueue", "[shark_push]handleCallbackForPush(), callback exception: " + -l_8_R4, -l_8_R4);
            -l_8_R32 = -l_8_R4;
        }
    }

    public static boolean a(ce ceVar) {
        boolean z = false;
        if (ceVar == null) {
            return false;
        }
        if (ceVar.ez != 0) {
            z = true;
        }
        return z;
    }

    private void b(int i, tmsdkobf.nq.b bVar) {
        Object<a> -l_3_R = new ArrayList();
        synchronized (this.GK) {
            if (this.GK.size() > 0) {
                -l_3_R.addAll(this.GK);
            }
        }
        if (-l_3_R.size() > 0) {
            for (a -l_5_R : -l_3_R) {
                -l_5_R.a(i, bVar);
            }
        }
    }

    private void b(long j, ce ceVar, byte[] bArr, oh<JceStruct, ka, c> ohVar) {
        Object -l_7_R;
        Object -l_8_R;
        JceStruct jceStruct = null;
        if (ceVar.data != null) {
            if (((c) ohVar.Il).GX) {
                try {
                    -l_7_R = nh.a(this.mContext, this.Dm.ap().DX.getBytes(), ceVar.data, ceVar.eE);
                } catch (Object -l_8_R2) {
                    mb.b("SharkProtocolQueue", "[shark_push]handleCallbackForGift(), dataForReceive2JceBytes exception: " + -l_8_R2, -l_8_R2);
                }
                if (((c) ohVar.Il).GX) {
                    -l_8_R2 = ((ka) ohVar.second).a(ceVar.ey, j, ceVar.bz, jceStruct);
                } else {
                    -l_8_R2 = ((kb) ohVar.second).a(ceVar.ey, j, ceVar.bz, -l_7_R);
                }
                if (-l_8_R2 == null) {
                    c(ceVar.ey, ((Integer) -l_8_R2.second).intValue(), (JceStruct) -l_8_R2.Il);
                }
            } else if (ohVar.first != null) {
                try {
                    jceStruct = nh.a(this.mContext, this.Dm.ap().DX.getBytes(), ceVar.data, (JceStruct) ohVar.first, true, ceVar.eE);
                } catch (Object -l_8_R22) {
                    mb.b("SharkProtocolQueue", "[shark_push]handleCallbackForGift(), dataForReceive2JceStruct exception: " + -l_8_R22, -l_8_R22);
                }
            }
        }
        -l_7_R = null;
        try {
            if (((c) ohVar.Il).GX) {
                -l_8_R22 = ((kb) ohVar.second).a(ceVar.ey, j, ceVar.bz, -l_7_R);
            } else {
                -l_8_R22 = ((ka) ohVar.second).a(ceVar.ey, j, ceVar.bz, jceStruct);
            }
            if (-l_8_R22 == null) {
                c(ceVar.ey, ((Integer) -l_8_R22.second).intValue(), (JceStruct) -l_8_R22.Il);
            }
        } catch (Throwable -l_9_R) {
            mb.b("SharkProtocolQueue", "[shark_push]handleCallbackForGift(), callback exception: " + -l_9_R, -l_9_R);
        }
    }

    public static boolean b(ce ceVar) {
        return (ceVar == null || ceVar.ez != 0 || ceVar.eO == null || ceVar.eO.ex == 0) ? false : true;
    }

    public static boolean c(ce ceVar) {
        boolean z = false;
        if (ceVar == null) {
            return false;
        }
        if (!(a(ceVar) || b(ceVar))) {
            z = true;
        }
        return z;
    }

    private void i(int i, String str) {
        Object<jw> -l_3_R = new ArrayList();
        synchronized (this.GI) {
            if (this.GI.size() > 0) {
                -l_3_R.addAll(this.GI);
                this.GI.clear();
            }
        }
        synchronized (this.GJ) {
            if (this.GJ.size() > 0) {
                -l_3_R.addAll(this.GJ);
            }
        }
        if (-l_3_R.size() > 0) {
            for (jw -l_5_R : -l_3_R) {
                -l_5_R.c(i, str);
            }
        }
    }

    public void N(boolean z) {
        if (z) {
            if (nu.Ev) {
                this.Dm.fC();
            } else {
                this.Gt = true;
            }
        } else if (nu.Ev) {
            this.Dm.gk();
        } else {
            this.Gs = true;
        }
    }

    public long a(boolean z, int i, ce ceVar) {
        if (ceVar == null || !b(ceVar)) {
            return -1;
        }
        long -l_4_J = 0;
        if (ceVar.eO != null) {
            -l_4_J = ceVar.eO.ex;
        }
        mb.d("SharkProtocolQueue", "[shark_push]onPush(), ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey + " pushId: " + -l_4_J + " isTcpChannel: " + z);
        a(-l_4_J, ceVar.bz, i, ceVar.ey, -1000000001);
        if (ceVar.eB != 0) {
            mb.o("SharkProtocolQueue", "[shark_push]onPush(), push with error, drop it, ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey + " pushId: " + -l_4_J + " isTcpChannel: " + z + " retCode: " + ceVar.eB);
            return -1;
        } else if (this.Gq.d(Long.valueOf(-l_4_J))) {
            mb.s("SharkProtocolQueue", "[shark_push]onPush(), push duplicate, drop it, ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey + " pushId: " + -l_4_J);
            return -1;
        } else {
            oh -l_6_R;
            this.Gq.push(Long.valueOf(-l_4_J));
            synchronized (this.FQ) {
                -l_6_R = (oh) this.FQ.get(Integer.valueOf(ceVar.bz));
            }
            if (-l_6_R != null) {
                mb.d("SharkProtocolQueue", "[shark_push]onPush(), someone listen to it, callback now, ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey + " pushId: " + -l_4_J);
                a(-l_4_J, ceVar, this.Dm.ap().DX.getBytes(), -l_6_R);
                return -l_6_R.Il == null ? -1 : ((c) -l_6_R.Il).mr;
            } else {
                int -l_7_I;
                synchronized (this.Go) {
                    this.Go.add(new b(this, 0, System.currentTimeMillis(), -l_4_J, ceVar, this.Dm.ap().DX.getBytes()));
                    -l_7_I = this.Go.size();
                }
                mb.s("SharkProtocolQueue", "[shark_push]onPush(), nobody listen to it, ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey + " pushId: " + -l_4_J + " cache for " + 600 + "s" + " pushSize: " + -l_7_I);
                this.vH.removeMessages(3);
                if (-l_7_I < 20) {
                    this.vH.sendEmptyMessageDelayed(3, 600000);
                } else {
                    this.vH.sendEmptyMessageDelayed(3, 2000);
                    this.vH.sendEmptyMessageDelayed(3, 600000);
                }
                return -1;
            }
        }
    }

    public WeakReference<kd> a(int i, long j, int i2, JceStruct jceStruct, byte[] bArr, int i3) {
        return a(i, j, i2, jceStruct, bArr, i3, 0);
    }

    public WeakReference<kd> a(int i, long j, int i2, JceStruct jceStruct, byte[] bArr, int i3, int i4) {
        nv.z("SharkProtocolQueue", "[shark_push]sendPushResp(), pushSeqNo: " + i + " pushId: " + j + " cmdId: " + i2 + " result: " + i3 + " retCode: " + i4);
        an -l_9_R = new an();
        -l_9_R.bz = i2;
        -l_9_R.status = i3;
        if (bArr != null && bArr.length > 0) {
            -l_9_R.bA = bArr;
        } else if (jceStruct != null) {
            -l_9_R.bA = nn.d(jceStruct);
        }
        Object -l_11_R = new d(this, 0, 0, -1, 1103, jceStruct, nh.b(-l_9_R), null, 1073741824, null, null);
        -l_11_R.Ec = i;
        -l_11_R.ex = j;
        -l_11_R.eB = i4;
        synchronized (this.Gp) {
            this.Gp.add(-l_11_R);
        }
        if (nu.Ev) {
            this.vH.sendEmptyMessage(1);
        }
        return new WeakReference(-l_11_R.Hc);
    }

    public void a(long j, int i, int i2, int i3, int i4) {
        nv.z("SharkProtocolQueue", "autoReplyPush()  pushId: " + j + " cmdId: " + i + " serverSharkSeqNo: " + i2 + " serverSashimiSeqNo: " + i3 + " errCode: " + i4);
        Object -l_7_R = new d(this, Process.myPid(), 0, 0, i, null, new byte[0], null, 1073741824, null, null);
        -l_7_R.eB = i4;
        -l_7_R.Ec = i3;
        -l_7_R.ex = j;
        synchronized (this.Gp) {
            this.Gp.add(-l_7_R);
        }
        if (nu.Ev) {
            this.vH.sendEmptyMessage(1);
        }
    }

    public void a(long j, int i, JceStruct jceStruct, int i2, ka kaVar, boolean z) {
        if (kaVar != null) {
            synchronized (this.FQ) {
                this.FQ.put(Integer.valueOf(i), new oh(jceStruct, kaVar, new c(z, j)));
            }
            mb.d("SharkProtocolQueue", "[shark_push]registerSharkPush(), for cmd: " + i);
            if (nu.Ev) {
                this.vH.obtainMessage(5, i, 0).sendToTarget();
            }
        }
    }

    public void a(jw jwVar) {
        if (jwVar != null) {
            synchronized (this.GJ) {
                if (!this.GJ.contains(jwVar)) {
                    this.GJ.add(jwVar);
                }
            }
        }
    }

    public void a(kj kjVar, long j) {
        if (kjVar != null) {
            mb.n("SharkProtocolQueue", "[shark_vip] setVipRule(): " + kjVar + ", valid time(ms): " + j);
            this.Gr = kjVar;
            this.vH.removeMessages(6);
            if ((j <= 0 ? 1 : null) == null) {
                this.vH.sendEmptyMessageDelayed(6, j);
            }
        }
    }

    public void a(a aVar) {
        if (aVar != null) {
            synchronized (this.GK) {
                if (!this.GK.contains(aVar)) {
                    this.GK.add(aVar);
                }
            }
        }
    }

    public long b(boolean z, int i, ce ceVar) {
        if (ceVar == null || !c(ceVar)) {
            return -1;
        }
        mb.d("SharkProtocolQueue", "[shark_push]onGotGift(), ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey + " pushId: " + 0 + " isTcpChannel: " + z);
        if (ceVar.eB == 0) {
            oh -l_6_R;
            synchronized (this.FQ) {
                -l_6_R = (oh) this.FQ.get(Integer.valueOf(ceVar.bz));
            }
            if (-l_6_R != null) {
                mb.d("SharkProtocolQueue", "[shark_push]onGotGift(), someone listen to it, callback now, ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey);
                b(0, ceVar, this.Dm.ap().DX.getBytes(), -l_6_R);
                return -l_6_R.Il == null ? -1 : ((c) -l_6_R.Il).mr;
            } else {
                int -l_7_I;
                synchronized (this.Go) {
                    this.Go.add(new b(this, 1, System.currentTimeMillis(), 0, ceVar, this.Dm.ap().DX.getBytes()));
                    -l_7_I = this.Go.size();
                }
                mb.s("SharkProtocolQueue", "[shark_push]onGotGift(), nobody listen to it, ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey + " cache for " + 600 + "s" + " pushSize: " + -l_7_I);
                this.vH.removeMessages(3);
                if (-l_7_I < 20) {
                    this.vH.sendEmptyMessageDelayed(3, 600000);
                } else {
                    this.vH.sendEmptyMessageDelayed(3, 2000);
                    this.vH.sendEmptyMessageDelayed(3, 600000);
                }
                return -1;
            }
        }
        mb.o("SharkProtocolQueue", "[shark_push]onGotGift(), gift with error, drop it, ECmd: " + ceVar.bz + " seqNo: " + ceVar.ey + " pushId: " + 0 + " isTcpChannel: " + z + " retCode: " + ceVar.eB);
        return -1;
    }

    public String b() {
        return this.Dm != null ? this.Dm.b() : "";
    }

    public void b(int i, int i2, int i3) {
        if (nu.Ev) {
            this.Dm.b(i, i2, i3);
            return;
        }
        if (this.GB == null) {
            this.GB = new LinkedList();
        }
        this.GB.add(new no(i, i2, i3));
    }

    public void b(jw jwVar) {
        if (jwVar != null) {
            synchronized (this.GJ) {
                if (this.GJ.contains(jwVar)) {
                    this.GJ.remove(jwVar);
                }
            }
        }
    }

    public void b(a aVar) {
        if (aVar != null) {
            synchronized (this.GK) {
                if (this.GK.contains(aVar)) {
                    this.GK.remove(aVar);
                }
            }
        }
    }

    public WeakReference<kd> c(int i, int i2, int i3, long j, long j2, int i4, JceStruct jceStruct, byte[] bArr, JceStruct jceStruct2, int i5, jy jyVar, jz jzVar, long j3, long j4) {
        mb.d("SharkProtocolQueue", "sendShark() cmdId: " + i4 + " pushSeqNo: " + i3);
        if (i3 > 0) {
            return a(i3, j, i4, jceStruct, bArr, 1);
        }
        Object -l_19_R = new d(this, i, i2, j2, i4, jceStruct, bArr, jceStruct2, i5, jyVar, jzVar);
        -l_19_R.Ec = ns.fW().fP();
        -l_19_R.ov = j3;
        -l_19_R.ow = j4;
        synchronized (this.Gp) {
            this.Gp.add(-l_19_R);
        }
        nt.ga().a(-l_19_R.Ec, j3, null);
        if (nu.Ev) {
            this.vH.sendEmptyMessage(1);
        }
        return new WeakReference(-l_19_R.Hc);
    }

    public WeakReference<kd> c(int i, final int i2, JceStruct jceStruct) {
        mb.n("SharkProtocolQueue", "[shark_push]sendGiftResp(): giftSeqNo: " + i + " acmdId: " + i2 + " respStruct: " + jceStruct);
        if (i2 == 156) {
            mb.r("SharkProtocolQueue", "[ip_list]sendGiftResp(): giftSeqNo: " + i + " acmdId: " + i2 + " respStruct: " + jceStruct);
        }
        return nu.gf().a(i2, jceStruct, null, 0, new jy(this) {
            final /* synthetic */ nz GM;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                mb.n("SharkProtocolQueue", "[shark_push]sendGiftResp()-onFinish() seqNo: " + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4);
                if (i2 == 156) {
                    mb.r("SharkProtocolQueue", "[ip_list]sendGiftResp()-onFinish() seqNo: " + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4 + " resp: " + jceStruct);
                }
            }
        });
    }

    public void c(nl nlVar) {
        mb.d("SharkProtocolQueue", "[shark_init]initSync()");
        this.CT = nlVar;
        nz -l_2_R = this;
        synchronized (this) {
            if (this.GL == null) {
                this.GL = new a();
                this.GL.k(this.mContext);
                mb.d("SharkProtocolQueue", "[shark_init][cu_guid][rsa_key] initSync(), register guid & rsakey event");
            }
        }
    }

    public void gA() {
        if (nu.aB()) {
            if (nu.Ev) {
                this.Dm.gj().gA();
            } else {
                this.Gx = true;
            }
        }
    }

    public void gB() {
        if (nu.Ev) {
            this.Dm.go();
        } else {
            this.GA = true;
        }
    }

    public void gC() {
        if (nu.aB()) {
            if (nu.Ev) {
                this.Dm.gj().gC();
            } else {
                this.GE = true;
            }
        }
    }

    public void gD() {
        if (nu.aB()) {
            if (nu.Ev) {
                this.Dm.gj().gD();
            } else {
                this.GF = true;
            }
        }
    }

    public int getSingletonType() {
        return 1;
    }

    public void gh() {
        if (nu.Ev) {
            this.Dm.gh();
        } else {
            this.Gv = true;
        }
    }

    public nl gl() {
        return this.CT;
    }

    public void gm() {
        if (nu.Ev) {
            this.Dm.gm();
        } else {
            this.Gy = true;
        }
    }

    public void gn() {
        if (nu.Ev) {
            this.Dm.gn();
        } else {
            this.Gz = true;
        }
    }

    void gx() {
        if (nu.Ev) {
            this.vH.removeMessages(4);
            this.vH.sendEmptyMessage(4);
            return;
        }
        this.GC = true;
    }

    public void gy() {
        mb.n("SharkProtocolQueue", "[shark_init]initAsync()");
        this.Dm = new nw(TMSDKContext.getApplicaionContext(), this.CT, this, this, this, nu.gc(), nu.gd());
        this.EO = Executors.newSingleThreadExecutor();
        nt.ga().b(this.CT);
        this.vH.sendEmptyMessage(2);
    }

    public void gz() {
        if (nu.Ev) {
            this.Dm.y(1000);
        } else {
            this.Gu = true;
        }
    }

    public void onCreate(Context context) {
        mb.d("SharkProtocolQueue", "[shark_init]onCreate()");
        this.mContext = context;
    }

    public void onReady() {
        if (!nu.Ev) {
            this.Gw = true;
        } else if (this.Dm != null) {
            this.Dm.onReady();
        }
    }

    public ka v(int i, int i2) {
        ka -l_3_R = null;
        synchronized (this.FQ) {
            if (this.FQ.containsKey(Integer.valueOf(i))) {
                -l_3_R = (ka) ((oh) this.FQ.remove(Integer.valueOf(i))).second;
            }
        }
        return -l_3_R;
    }
}
