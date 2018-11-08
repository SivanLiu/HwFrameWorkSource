package tmsdkobf;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Pair;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.utils.i;

public final class ny {
    private static ny FS = null;
    private final Object DA = new Object();
    private nl Dz;
    private tmsdkobf.ns.a FL;
    private int FM = Process.myPid();
    private ExecutorService FN;
    private ArrayList<a> FO = new ArrayList();
    private TreeMap<Integer, a> FP = new TreeMap();
    private TreeMap<Integer, Pair<JceStruct, ka>> FQ = new TreeMap();
    private Handler FR = new Handler(this, Looper.getMainLooper()) {
        final /* synthetic */ ny FT;

        public void handleMessage(Message message) {
            switch (message.what) {
                case 11:
                    Object[] -l_2_R = (Object[]) ((Object[]) message.obj);
                    a -l_3_R = (a) -l_2_R[0];
                    if (-l_3_R.Gk != null) {
                        -l_3_R.Gk.onFinish(((Integer) -l_2_R[1]).intValue(), -l_3_R.Gf, ((Integer) -l_2_R[2]).intValue(), ((Integer) -l_2_R[3]).intValue(), -l_3_R.Gi);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private Handler Fa = new Handler(this, nu.getLooper()) {
        final /* synthetic */ ny FT;

        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case 0:
                    this.FT.a((a) message.obj);
                    return;
                default:
                    return;
            }
        }
    };
    private Handler vH = new Handler(this, nu.getLooper()) {
        final /* synthetic */ ny FT;

        public void handleMessage(Message message) {
            Object -l_2_R;
            switch (message.what) {
                case 1:
                    try {
                        this.FT.vH.removeMessages(1);
                        -l_2_R = new b();
                        synchronized (this.FT.DA) {
                            Object -l_4_R = this.FT.FO.iterator();
                            while (-l_4_R.hasNext()) {
                                a -l_5_R = (a) -l_4_R.next();
                                -l_2_R.a(Integer.valueOf(-l_5_R.Gc), -l_5_R);
                                if ((-l_5_R.Gj & 1073741824) == 0) {
                                    this.FT.FP.put(Integer.valueOf(-l_5_R.Gc), -l_5_R);
                                }
                                mb.d("SharkProcessProxy", this.FT.FM + " sendShark() MSG_SEND_PROXY_TASK task.mIpcSeqNo: " + -l_5_R.Gc);
                            }
                            this.FT.FO.clear();
                        }
                        this.FT.FN.submit(-l_2_R);
                        return;
                    } catch (Object -l_2_R2) {
                        mb.o("SharkProcessProxy", "exception: " + -l_2_R2);
                        return;
                    }
                default:
                    return;
            }
        }
    };

    public class a {
        public int FM;
        final /* synthetic */ ny FT;
        public int Gc;
        public int Gd;
        public long Ge;
        public int Gf;
        public long Gg;
        public JceStruct Gh;
        public JceStruct Gi;
        public int Gj;
        public jy Gk;
        public long Gl = -1;
        public long Gm = System.currentTimeMillis();
        public long mTimeout = -1;

        a(ny nyVar, int i, int i2, int i3, long j, long j2, int i4, JceStruct jceStruct, JceStruct jceStruct2, int i5, jy jyVar, long j3, long j4) {
            this.FT = nyVar;
            this.FM = i;
            this.Gc = i2;
            this.Gd = i3;
            this.Ge = j;
            this.Gf = i4;
            this.Gg = j2;
            this.Gh = jceStruct;
            this.Gi = jceStruct2;
            this.Gj = i5;
            this.Gk = jyVar;
            this.mTimeout = j3;
            this.Gl = j4;
        }

        public boolean gp() {
            int -l_5_I = 1;
            long -l_1_J = Math.abs(System.currentTimeMillis() - this.Gm);
            if ((-l_1_J < (((this.mTimeout > 0 ? 1 : (this.mTimeout == 0 ? 0 : -1)) <= 0 ? 1 : 0) == 0 ? this.mTimeout : 185000) ? 1 : 0) != 0) {
                -l_5_I = 0;
            }
            if (-l_5_I != 0) {
                Object -l_6_R = new StringBuilder();
                -l_6_R.append("cmdId|").append(this.Gf);
                -l_6_R.append("|mIpcSeqNo|").append(this.Gc);
                -l_6_R.append("|mPushSeqNo|").append(this.Gd);
                -l_6_R.append("|mPushId|").append(this.Ge);
                -l_6_R.append("|mCallerIdent|").append(this.Gg);
                -l_6_R.append("|mTimeout|").append(this.mTimeout);
                -l_6_R.append("|time(s)|").append(-l_1_J / 1000);
                nv.c("ocean", "[ocean][time_out]SharkProcessProxy.SharkProxyTask.isTimeOut(), " + -l_6_R.toString(), null, null);
            }
            return -l_5_I;
        }
    }

    private class b implements Runnable {
        final /* synthetic */ ny FT;
        private TreeMap<Integer, a> Gn;

        private b(ny nyVar) {
            this.FT = nyVar;
            this.Gn = new TreeMap();
        }

        public void a(Integer num, a aVar) {
            this.Gn.put(num, aVar);
        }

        public Set<Entry<Integer, a>> gw() {
            TreeMap -l_1_R;
            synchronized (this.Gn) {
                -l_1_R = (TreeMap) this.Gn.clone();
            }
            return -l_1_R.entrySet();
        }

        public void run() {
            int -l_1_I = i.hm();
            for (Entry -l_4_R : gw()) {
                if (-l_1_I == 0) {
                    mb.n("SharkProcessProxy", this.FT.FM + " run, 无物理网络");
                    this.FT.a(Process.myPid(), ((a) -l_4_R.getValue()).Gc, 0, ((a) -l_4_R.getValue()).Gf, null, -1000002, 0);
                    mb.s("SharkProcessProxy", "[ocean]SharkProxyTaskRunnable.run(), no network: cmdId: " + ((a) -l_4_R.getValue()).Gf + " retCode: " + -1000002);
                    this.FT.Dz.e(((a) -l_4_R.getValue()).Gf, -1000002);
                } else if (((a) -l_4_R.getValue()).gp()) {
                    this.FT.a(Process.myPid(), ((a) -l_4_R.getValue()).Gc, 0, ((a) -l_4_R.getValue()).Gf, null, -1000017, 0);
                    mb.s("SharkProcessProxy", "[ocean][time_out]SharkProxyTaskRunnable.run(), send time out, stats by onConnnect(): cmdId: " + ((a) -l_4_R.getValue()).Gf + " retCode: " + -1000017);
                    this.FT.Dz.e(((a) -l_4_R.getValue()).Gf, -1000017);
                } else {
                    mb.n("SharkProcessProxy", this.FT.FM + " onPostToSendingProcess() mPid: " + ((a) -l_4_R.getValue()).FM + " mCallerIdent: " + ((a) -l_4_R.getValue()).Gg + " mIpcSeqNo: " + ((a) -l_4_R.getValue()).Gc + " mPushSeqNo: " + ((a) -l_4_R.getValue()).Gd + " mPushId: " + ((a) -l_4_R.getValue()).Ge + " mCmdId: " + ((a) -l_4_R.getValue()).Gf + " mFlag: " + ((a) -l_4_R.getValue()).Gj + " mTimeout: " + ((a) -l_4_R.getValue()).mTimeout);
                    this.FT.Fa.sendMessageDelayed(Message.obtain(this.FT.Fa, 0, -l_4_R.getValue()), 185000);
                    this.FT.Dz.a(((a) -l_4_R.getValue()).FM, ((a) -l_4_R.getValue()).Gg, ((a) -l_4_R.getValue()).Gc, ((a) -l_4_R.getValue()).Gd, ((a) -l_4_R.getValue()).Ge, ((a) -l_4_R.getValue()).Gf, nh.b(((a) -l_4_R.getValue()).Gh), ((a) -l_4_R.getValue()).Gj, ((a) -l_4_R.getValue()).mTimeout, ((a) -l_4_R.getValue()).Gl, ((a) -l_4_R.getValue()).Gm);
                }
            }
        }
    }

    private ny(nl nlVar) {
        this.Dz = nlVar;
        this.FL = new tmsdkobf.ns.a();
        this.FN = Executors.newSingleThreadExecutor();
    }

    private void a(final a aVar) {
        mb.d("SharkProcessProxy", "runTimeout() sharkProxyTask: " + aVar.Gc);
        this.Fa.removeMessages(0, aVar);
        synchronized (this.DA) {
            if (this.FP.containsKey(Integer.valueOf(aVar.Gc)) != 0) {
                ((ki) fj.D(4)).addTask(new Runnable(this) {
                    final /* synthetic */ ny FT;

                    public void run() {
                        this.FT.a(Process.myPid(), aVar.Gc, 0, aVar.Gf, null, ne.bj(-2050000), 0);
                    }
                }, "sharkProcessProxyTimeout");
                return;
            }
        }
    }

    public static synchronized ny gv() {
        ny nyVar;
        synchronized (ny.class) {
            if (FS == null) {
                FS = new ny(((nz) ManagerCreatorC.getManager(nz.class)).gl());
            }
            nyVar = FS;
        }
        return nyVar;
    }

    public void a(int i, int i2, int i3, int i4, byte[] bArr, int i5, int i6) {
        if (this.FM == i) {
            final int i7 = i2;
            final byte[] bArr2 = bArr;
            final int i8 = i4;
            final int i9 = i3;
            final int i10 = i5;
            final int i11 = i6;
            ((ki) fj.D(4)).addTask(new Runnable(this) {
                final /* synthetic */ ny FT;

                public void run() {
                    try {
                        a -l_1_R;
                        synchronized (this.FT.DA) {
                            -l_1_R = (a) this.FT.FP.remove(Integer.valueOf(i7));
                        }
                        if (-l_1_R != null) {
                            this.FT.Fa.removeMessages(0, -l_1_R);
                            JceStruct -l_2_R = nh.b(bArr2, -l_1_R.Gi);
                            if (-l_1_R.Gi != -l_2_R) {
                                -l_1_R.Gi = -l_2_R;
                            }
                            -l_1_R.Gf = i8;
                            mb.n("SharkProcessProxy", this.FT.FM + " callBack() ipcSeqNo: " + i7 + " seqNo: " + i9 + " cmdId: " + i8 + " retCode: " + i10 + " dataRetCode: " + i11);
                            this.FT.a(-l_1_R, Integer.valueOf(i9), Integer.valueOf(i10), Integer.valueOf(i11));
                            return;
                        }
                        mb.o("SharkProcessProxy", this.FT.FM + " callBack(), no callback for ipcSeqNo: " + i7);
                    } catch (Object -l_1_R2) {
                        mb.o("SharkProcessProxy", "exception: " + -l_1_R2);
                        Object obj = -l_1_R2;
                    }
                }
            }, "shark callback");
            return;
        }
        mb.s("SharkProcessProxy", this.FM + " callBack() not my pid's response, its pid is: " + i);
    }

    public void a(int i, long j, int i2, long j2, int i3, JceStruct jceStruct, JceStruct jceStruct2, int i4, jy jyVar, long j3, long j4) {
        mb.d("SharkProcessProxy", this.FM + " sendShark()");
        Object -l_16_R = new a(this, i, this.FL.fP(), i2, j2, j, i3, jceStruct, jceStruct2, i4, jyVar, j3, j4);
        synchronized (this.DA) {
            this.FO.add(-l_16_R);
        }
        this.vH.sendEmptyMessage(1);
    }

    public void a(long j, int i, JceStruct jceStruct, int i2, ka kaVar) {
        synchronized (this.FQ) {
            mb.d("SharkProcessProxy", this.FM + " registerSharkPush() callIdent: " + j + " cmdId: " + i + " flag: " + i2);
            if (this.FQ.containsKey(Integer.valueOf(i))) {
                Object -l_8_R = "[shark_push]registerSharkPush(), only one listener is allowed for current version! callIdent: " + j + " cmdId: " + i + " flag: " + i2;
                if (nu.ge()) {
                    throw new RuntimeException(-l_8_R);
                }
                mb.o("SharkProcessProxy", -l_8_R);
            } else {
                this.FQ.put(Integer.valueOf(i), new Pair(jceStruct, kaVar));
                final long j2 = j;
                final int i3 = i;
                final int i4 = i2;
                ((ki) fj.D(4)).addTask(new Runnable(this) {
                    final /* synthetic */ ny FT;

                    public void run() {
                        if (this.FT.Dz == null) {
                            mb.o("SharkProcessProxy", "shark register push failed");
                        } else {
                            this.FT.Dz.a(j2, i3, i4);
                        }
                    }
                }, "shark register push");
            }
        }
    }

    protected void a(a aVar, Integer num, Integer num2, Integer num3) {
        if (aVar.Gk != null) {
            nv.a("ocean", "[ocean]procallback: ECmd|" + aVar.Gf + "|ipcSeqNo|" + aVar.Gc + "|seqNo|" + num + "|ret|" + num2 + "|dataRetCode|" + num3 + "|ident|" + aVar.Gg, null, null);
            switch (kc.al(aVar.Gj)) {
                case 8:
                    this.FR.sendMessage(this.FR.obtainMessage(11, new Object[]{aVar, num, num2, num3}));
                    break;
                case 16:
                    aVar.Gk.onFinish(num.intValue(), aVar.Gf, num2.intValue(), num3.intValue(), aVar.Gi);
                    break;
                default:
                    aVar.Gk.onFinish(num.intValue(), aVar.Gf, num2.intValue(), num3.intValue(), aVar.Gi);
                    break;
            }
        }
    }

    public ka v(final int i, final int i2) {
        ka -l_3_R = null;
        synchronized (this.FQ) {
            mb.d("SharkProcessProxy", this.FM + "unregisterSharkPush() cmdId: " + i + " flag: " + i2);
            if (this.FQ.containsKey(Integer.valueOf(i))) {
                -l_3_R = (ka) ((Pair) this.FQ.remove(Integer.valueOf(i))).second;
                ((ki) fj.D(4)).addTask(new Runnable(this) {
                    final /* synthetic */ ny FT;

                    public void run() {
                        this.FT.Dz.b(i, i2);
                    }
                }, "shark unregist push");
            }
        }
        return -l_3_R;
    }
}
