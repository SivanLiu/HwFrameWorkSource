package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import tmsdk.common.TMSDKContext;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;
import tmsdk.common.utils.l;
import tmsdk.common.utils.s;

public class kr {
    static a xA = null;
    static volatile boolean xB = false;
    static volatile long xC = -1;
    static Object xy = new Object();
    static Object xz = new Object();

    static class a extends if implements tmsdkobf.oo.a {
        public static boolean xF = false;

        a() {
        }

        public void dC() {
            if (im.bG()) {
                im.bP();
                if (!kr.br("FirstCheck")) {
                    if (gf.S().ai().booleanValue()) {
                        if (!i.K(TMSDKContext.getApplicaionContext())) {
                        }
                    }
                    kr.dy();
                }
                kr.dB();
                kr.dy();
            }
            ll.aM(2);
            s.bW(1);
        }

        public void dD() {
        }

        public void doOnRecv(Context context, Intent intent) {
            if (intent != null) {
                int -l_3_I = -1;
                Object -l_4_R = intent.getAction();
                f.d("cccccc", "check");
                if ("android.intent.action.TIME_SET".equals(-l_4_R) || "android.intent.action.TIMEZONE_CHANGED".equals(-l_4_R)) {
                    -l_3_I = 4;
                } else if ("android.intent.action.USER_PRESENT".equals(-l_4_R)) {
                    -l_3_I = 3;
                } else if ("tmsdk.common.ccrreport".equals(-l_4_R)) {
                    -l_3_I = 1;
                }
                if (-l_3_I != -1) {
                    if (im.bG()) {
                        kr.dz();
                    }
                    ll.aM(-l_3_I);
                    s.bW(1);
                }
            }
        }

        public synchronized void k(Context context) {
            if (!xF) {
                Object -l_2_R = new IntentFilter();
                -l_2_R.addAction("android.intent.action.USER_PRESENT");
                -l_2_R.setPriority(Integer.MAX_VALUE);
                context.registerReceiver(this, -l_2_R);
                Object -l_3_R = oo.A(context);
                if (-l_3_R != null) {
                    -l_3_R.a((tmsdkobf.oo.a) this);
                }
                xF = true;
            }
        }
    }

    public static void aD(int i) {
        gf.S().I(i);
    }

    static boolean br(String str) {
        long -l_2_J = System.currentTimeMillis();
        long -l_4_J = gf.S().W();
        if ((-l_2_J <= -l_4_J ? 1 : null) == null) {
            if ((-l_2_J - -l_4_J < 86400000 ? 1 : null) != null) {
                Object -l_6_R = Calendar.getInstance();
                -l_6_R.set(11, 0);
                -l_6_R.set(12, 0);
                -l_6_R.set(13, 0);
                long -l_8_J = -l_6_R.getTimeInMillis() + (((long) dA()) * 1000);
                if ((-l_8_J <= -l_2_J ? 1 : null) == null) {
                    -l_8_J -= 86400000;
                }
                if ((-l_4_J > -l_8_J ? 1 : null) != null) {
                    return false;
                }
            }
        } else {
            if ((Math.abs(-l_2_J - -l_4_J) < 86400000 ? 1 : null) != null) {
                return false;
            }
        }
        return true;
    }

    static int dA() {
        int -l_1_I = gf.S().V();
        if (-l_1_I > 0) {
            return -l_1_I;
        }
        -l_1_I = p(1, 20);
        aD(-l_1_I);
        return -l_1_I;
    }

    private static void dB() {
        synchronized (xz) {
            if ((System.currentTimeMillis() - xC >= 600000 ? 1 : null) == null) {
                return;
            }
            xC = System.currentTimeMillis();
            ((ki) fj.D(4)).addTask(new Runnable() {
                public void run() {
                    if (!ll.n(TMSDKContext.getApplicaionContext())) {
                        kt.aE(1320026);
                    } else if (ll.o(TMSDKContext.getApplicaionContext())) {
                        kt.aE(1320024);
                    } else {
                        kt.aE(1320025);
                    }
                    if (gf.S().aj().booleanValue()) {
                        li.ey().C(-1);
                    }
                    kr.p(false);
                    kw.dP();
                    ku.dO();
                    kx.dR().dS();
                }
            }, "xxx");
        }
    }

    private static void dy() {
        if (!xB) {
            xB = true;
            ((ki) fj.D(4)).addTask(new Runnable() {
                public void run() {
                    try {
                        la.el();
                    } catch (Throwable th) {
                    }
                    kr.xB = false;
                }
            }, "bd");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void dz() {
        synchronized (xy) {
            dy();
            if (br("FirstCheck") && i.iE()) {
                dB();
                gf.S().f(Boolean.valueOf(true));
            }
        }
    }

    public static void init() {
        li.ey();
        if (xA == null) {
            xA = new a();
            xA.k(TMSDKContext.getApplicaionContext());
        }
        s.bW(2);
    }

    static int p(int i, int i2) {
        int -l_2_I = i2 - i;
        if (-l_2_I < 0) {
            return -1;
        }
        long -l_3_J = 0;
        try {
            -l_3_J = Long.parseLong(l.L(TMSDKContext.getApplicaionContext()));
        } catch (Throwable th) {
        }
        Object -l_5_R = new Random();
        -l_5_R.setSeed(((System.currentTimeMillis() + ((long) System.identityHashCode(-l_5_R))) + ((long) System.identityHashCode(gf.S()))) + -l_3_J);
        return (((((int) (-l_5_R.nextDouble() * ((double) -l_2_I))) + i) * 3600) + (((int) (-l_5_R.nextDouble() * 60.0d)) * 60)) + ((int) (-l_5_R.nextDouble() * 60.0d));
    }

    public static synchronized void p(boolean z) {
        synchronized (kr.class) {
            try {
                q(z);
            } catch (Throwable th) {
            }
        }
    }

    private static void q(final boolean z) {
        final Object -l_1_R = kt.dE();
        if (gf.S().Y().booleanValue()) {
            Object -l_6_R;
            b -l_7_R;
            int -l_8_I;
            int -l_2_I = 0;
            JceStruct -l_3_R = new a();
            -l_3_R.a = new ArrayList();
            int -l_4_I = 0;
            Object -l_5_R = -l_1_R.dL();
            if (-l_5_R != null) {
                -l_6_R = -l_5_R.iterator();
                while (-l_6_R.hasNext()) {
                    -l_7_R = (b) -l_6_R.next();
                    -l_8_I = -l_7_R.toByteArray().length;
                    if (-l_8_I > 1024000) {
                        kt.aH(-l_7_R.c);
                    }
                    if (-l_2_I + -l_8_I >= 1024000) {
                        -l_6_R.remove();
                    } else {
                        -l_3_R.a.add(-l_7_R);
                        -l_2_I += -l_8_I;
                        -l_4_I++;
                    }
                }
            }
            -l_5_R = -l_1_R.dN();
            if (-l_5_R != null) {
                -l_6_R = -l_5_R.iterator();
                while (-l_6_R.hasNext()) {
                    -l_7_R = (b) -l_6_R.next();
                    -l_8_I = -l_7_R.toByteArray().length;
                    if (-l_8_I > 1024000) {
                        kt.aI(-l_7_R.c);
                    }
                    if (-l_2_I + -l_8_I >= 1024000) {
                        -l_6_R.remove();
                    } else {
                        -l_3_R.a.add(-l_7_R);
                        -l_2_I += -l_8_I;
                        -l_4_I++;
                    }
                }
            }
            -l_5_R = -l_1_R.dM();
            if (-l_5_R != null) {
                -l_6_R = -l_5_R.iterator();
                while (-l_6_R.hasNext()) {
                    -l_7_R = (b) -l_6_R.next();
                    -l_8_I = -l_7_R.toByteArray().length;
                    if (-l_8_I > 1024000) {
                        kt.aF(-l_7_R.c);
                    }
                    if (-l_2_I + -l_8_I >= 1024000) {
                        -l_6_R.remove();
                    } else {
                        -l_3_R.a.add(-l_7_R);
                        -l_2_I += -l_8_I;
                        -l_4_I++;
                    }
                }
            }
            Object -l_7_R2 = im.bK().a(3651, -l_3_R, null, 2, new jy() {
                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    if (i3 == 0) {
                        -l_1_R.dI();
                        -l_1_R.dK();
                        -l_1_R.dJ();
                        if (!z) {
                            gf.S().a(System.currentTimeMillis());
                        }
                    }
                }
            });
            return;
        }
        -l_1_R.dI();
        -l_1_R.dK();
        -l_1_R.dJ();
    }
}
