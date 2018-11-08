package tmsdkobf;

import android.content.Context;
import com.qq.taf.jce.JceStruct;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.e;
import tmsdk.common.utils.l;
import tmsdk.common.utils.l.a;
import tmsdk.common.utils.n;
import tmsdk.common.utils.q;
import tmsdkobf.nq.b;

public class gh extends nl {
    private static gh og = null;
    private boolean oc = true;
    private boolean od = false;
    private boolean oe = false;
    private boolean of = false;

    private gh() {
    }

    public static gh aA() {
        if (og == null) {
            Object -l_0_R = gh.class;
            synchronized (gh.class) {
                if (og == null) {
                    og = new gh();
                }
            }
        }
        return og;
    }

    public WeakReference<kd> a(int i, int i2, int i3, long j, long j2, int i4, JceStruct jceStruct, byte[] bArr, JceStruct jceStruct2, int i5, jy jyVar, jz jzVar, long j3, long j4) {
        return go.aU().b(i, i2, i3, j, j2, i4, jceStruct, bArr, jceStruct2, i5, jyVar, jzVar, j3, j4);
    }

    public void a(int i, long j, int i2, int i3, long j2, int i4, byte[] bArr, int i5, long j3, long j4, long j5) {
    }

    public void a(long j, int i, int i2) {
    }

    public void a(String str, boolean z) {
        if (str != null) {
            mb.n("SharkOutlet", "onSaveGuidToPhone() guid: " + str);
            gg.al().Z(str);
        }
    }

    public void a(HashMap<String, String> hashMap) {
    }

    public void a(boolean z, boolean z2, boolean z3) {
        this.oc = z;
        this.od = z2;
        this.oe = z3;
    }

    public boolean aB() {
        return this.oc;
    }

    public boolean aC() {
        return this.od;
    }

    public boolean aD() {
        return this.oe;
    }

    public b aE() {
        return gg.al().ap();
    }

    public String aF() {
        Object -l_1_R = gg.al().as();
        mb.n("SharkOutlet", "onGetGuidFromPhone() guid: " + -l_1_R);
        return -l_1_R;
    }

    public String aG() {
        return null;
    }

    public boolean aH() {
        return true;
    }

    public void aI() {
    }

    public String aJ() {
        return gg.al().aq();
    }

    public String aK() {
        return gg.al().ar();
    }

    public br aL() {
        mb.n("SharkOutlet", "onGetInfoSavedOfGuid()");
        return gg.al().av();
    }

    public long aM() {
        return -1;
    }

    public br aN() {
        Context -l_1_R = TMSDKContext.getApplicaionContext();
        Object -l_2_R = e.D(-l_1_R);
        long -l_3_J = l.iV();
        Object -l_5_R = new a();
        l.a(-l_5_R);
        long -l_6_J = -l_5_R.LN;
        Object -l_8_R = new a();
        l.b(-l_8_R);
        long -l_9_J = -l_8_R.LN;
        Object -l_11_R = l.N(-l_1_R);
        int -l_12_I = 1;
        Object -l_13_R = "";
        if (im.bO() != null) {
            -l_13_R = im.bO().getIMSI(1);
            -l_12_I = 2;
        }
        Object -l_14_R = "";
        -l_14_R = im.bO() == null ? l.M(-l_1_R) : im.bO().getIMSI(0);
        int -l_15_I = l.Q(-l_1_R);
        int -l_16_I = l.R(-l_1_R);
        if (-l_15_I < -l_16_I) {
            int -l_17_I = -l_15_I;
            -l_15_I = -l_16_I;
            -l_16_I = -l_17_I;
        }
        Object -l_17_R = new br();
        -l_17_R.dl = l.L(-l_1_R);
        -l_17_R.imsi = -l_14_R;
        -l_17_R.dU = -l_13_R;
        if (-l_11_R == null) {
            -l_11_R = "";
        }
        -l_17_R.dm = -l_11_R;
        -l_17_R.dn = "0";
        -l_17_R.do = "0";
        -l_17_R.dp = 13;
        -l_17_R.dq = q.cI("19B7C7417A1AB190");
        -l_17_R.L = 3059;
        -l_17_R.dr = q.cI(im.bQ());
        -l_17_R.ds = 2;
        -l_17_R.dt = SmsCheckResult.ESCT_201;
        -l_17_R.du = e.F(-l_1_R);
        try {
            -l_17_R.dv = TMSDKContext.getApplicaionContext().getPackageName();
        } catch (Throwable th) {
        }
        -l_17_R.dw = q.cI(l.iL());
        -l_17_R.dx = n.iX();
        -l_17_R.dy = q.cI(l.P(-l_1_R));
        -l_17_R.dz = (short) 2052;
        -l_17_R.dA = -l_12_I;
        -l_17_R.dB = -l_2_R[2];
        -l_17_R.ed = l.cE("ro.product.cpu.abi2");
        -l_17_R.dC = e.iz();
        -l_17_R.dD = e.iC();
        -l_17_R.dE = -l_15_I + "*" + -l_16_I;
        -l_17_R.dF = -l_3_J;
        -l_17_R.dG = e.iD();
        -l_17_R.dH = -l_6_J;
        -l_17_R.ei = -l_9_J;
        -l_17_R.dI = q.cI(l.iP());
        -l_17_R.dJ = q.cI(l.iN());
        -l_17_R.dK = q.cI(l.iO());
        -l_17_R.version = "6.1.0";
        -l_17_R.dY = 1;
        -l_17_R.dZ = "";
        -l_17_R.dN = l.iT();
        -l_17_R.dQ = 0;
        -l_17_R.dR = 0;
        -l_17_R.ea = l.iQ();
        -l_17_R.eb = l.iR();
        -l_17_R.ec = l.cE("ro.build.product");
        -l_17_R.ee = l.cE("ro.build.fingerprint");
        -l_17_R.ef = l.cE("ro.product.locale.language");
        -l_17_R.eg = l.cE("ro.product.locale.region");
        -l_17_R.eh = l.getRadioVersion();
        -l_17_R.dO = l.cE("ro.board.platform");
        -l_17_R.ej = l.cE("ro.mediatek.platform");
        -l_17_R.dP = l.cE("ro.sf.lcd_density");
        -l_17_R.dL = l.cE("ro.product.name");
        -l_17_R.dM = l.cE("ro.build.version.release");
        -l_17_R.ek = l.iS();
        -l_17_R.dS = false;
        -l_17_R.el = 0;
        -l_17_R.em = l.iU();
        -l_17_R.en = l.S(true);
        -l_17_R.eo = l.S(false);
        return -l_17_R;
    }

    public long aO() {
        long -l_1_J = gg.al().au();
        mb.n("SharkOutlet", "onGetGuidUpdateCheckTimeMillis() tm: " + -l_1_J);
        return -l_1_J;
    }

    public boolean aP() {
        return gg.al().ax();
    }

    public h aQ() {
        return gg.al().az();
    }

    public int aR() {
        return -1;
    }

    public String aS() {
        return gg.al().at();
    }

    public void aT() {
    }

    public nj.a ah(String str) {
        return gg.al().ag(str);
    }

    public void b(int i, int i2) {
    }

    public void b(String str, long j, List<String> list) {
        gg.al().a(str, j, list);
    }

    public void b(String str, boolean z) {
    }

    public void b(br brVar) {
        mb.n("SharkOutlet", "onSaveInfoOfGuid()");
        gg.al().a(brVar);
    }

    public void b(h hVar) {
        gg.al().a(hVar);
    }

    public void b(b bVar) {
        if (bVar != null) {
            gg.al().a(bVar);
        }
    }

    public void c(int i, int i2) {
    }

    public void c(String str, boolean z) {
        if (str != null) {
            gg.al().aa(str);
        }
    }

    public void d(int i, int i2) {
    }

    public void d(String str, boolean z) {
        if (str != null) {
            gg.al().ab(str);
        }
    }

    public void e(int i, int i2) {
    }

    public void f(boolean z) {
        gg.al().e(z);
    }

    public void g(long j) {
        mb.n("SharkOutlet", "onSaveGuidUpdateCheckTimeMillis() timeMillis: " + j);
        gg.al().f(j);
    }
}
