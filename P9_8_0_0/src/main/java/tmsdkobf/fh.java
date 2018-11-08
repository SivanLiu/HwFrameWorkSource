package tmsdkobf;

import android.util.SparseArray;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.ErrorCode;
import tmsdk.common.utils.q;
import tmsdkobf.ju.a;
import tmsdkobf.ju.b;

public class fh {
    private static fh mf;
    private byte[] mg;
    private SparseArray<b> mh;
    private List<r> mi;
    private long mj;
    private ob mk;
    private ka ml;

    private fh() {
        this.mg = new byte[0];
        this.mi = new ArrayList();
        this.mj = -1;
        this.ml = new ka(this) {
            final /* synthetic */ fh mo;

            {
                this.mo = r1;
            }

            public oh<Long, Integer, JceStruct> a(int i, long j, int i2, JceStruct jceStruct) {
                kv.d("WakeupUtil", "[" + Thread.currentThread().getId() + "][shark]onRecvPush-pushId:[" + j + "]cmdId:[" + i2 + "]");
                if (i2 == 10010 && jceStruct != null) {
                    ArrayList<s> -l_7_R = ((x) jceStruct).at;
                    if (-l_7_R == null || -l_7_R.size() == 0) {
                        kv.n("WakeupUtil", "onRecvPush|scPushConchs is not null but conchTasks is null!");
                        return null;
                    }
                    l -l_8_R = new l();
                    -l_8_R.V = new ArrayList();
                    StringBuilder -l_9_R = new StringBuilder();
                    this.mo.mj = ((s) -l_7_R.get(0)).al;
                    for (s -l_11_R : -l_7_R) {
                        kv.d("WakeupUtil", "conchTask-taskId:" + -l_11_R.al + "|taskSeqno:" + -l_11_R.am);
                        if (-l_11_R.ap == null || -l_11_R.ap.size() == 0) {
                            kv.n("WakeupUtil", "onRecvPush|(conchTask.conchList == null) || (conchTask.conchList.size() == 0)|conchTask.taskId:" + -l_11_R.al);
                            -l_8_R.V.add(this.mo.a(-l_11_R.al, -l_11_R.am, null, 3));
                        } else {
                            Object -l_12_R = -l_11_R.ap.iterator();
                            while (-l_12_R.hasNext()) {
                                p -l_13_R = (p) -l_12_R.next();
                                kv.d("WakeupUtil", "conch-cmdId:" + -l_13_R.Y);
                                a -l_14_R = new a(-l_11_R.al, -l_11_R.am, -l_13_R);
                                -l_9_R.append(-l_13_R.Y + ";");
                                if (((b) this.mo.mh.get(-l_14_R.tB != null ? -l_14_R.tB.tD : -l_13_R.Y)) != null) {
                                    kv.d("WakeupUtil", "cmdId:[" + -l_13_R.Y + "]mLocalConchPushListener is not null");
                                    this.mo.a(-l_14_R);
                                    -l_8_R.V.add(this.mo.a(-l_11_R.al, -l_11_R.am, -l_13_R, 1));
                                }
                            }
                        }
                    }
                    if (q.cJ(-l_9_R.toString())) {
                        kt.e(1320064, -l_9_R.toString());
                    }
                    return new oh(Long.valueOf(j), Integer.valueOf(i2), -l_8_R);
                }
                kv.n("WakeupUtil", "onRecvPush|cmdId != ECmd.Cmd_SCPushConch : " + i2);
                return null;
            }
        };
        this.mj = -1;
        this.mh = new SparseArray();
        this.mk = im.bK();
        if (!gf.S().aj().booleanValue()) {
            h();
        }
    }

    private q a(long j, long j2, p pVar, int i) {
        kv.n("WakeupUtil", "createConchPushResult :taskId" + j);
        Object -l_7_R = new q();
        -l_7_R.al = j;
        -l_7_R.am = j2;
        if (pVar != null) {
            kv.n("WakeupUtil", "createConchPushResult :taskId" + j + " conch:" + pVar.Y);
            -l_7_R.Y = pVar.Y;
            -l_7_R.af = pVar.af;
        }
        -l_7_R.result = i;
        return -l_7_R;
    }

    private synchronized void a(final a aVar) {
        im.bJ().addTask(new Runnable(this) {
            final /* synthetic */ fh mo;

            public void run() {
                Object -l_1_R = aVar;
                int -l_2_I = -l_1_R.tA.Y;
                SparseArray b = this.mo.mh;
                if (!(-l_1_R == null || -l_1_R.tB == null)) {
                    -l_2_I = -l_1_R.tB.tD;
                }
                b -l_3_R = (b) b.get(-l_2_I);
                if (-l_3_R != null) {
                    try {
                        -l_3_R.b(-l_1_R);
                    } catch (Throwable th) {
                    }
                }
            }
        }, "conchP");
    }

    public static fh g() {
        if (mf == null) {
            Object -l_0_R = fh.class;
            synchronized (fh.class) {
                if (mf == null) {
                    mf = new fh();
                }
            }
        }
        return mf;
    }

    private synchronized void j() {
        im.bJ().addTask(new Runnable(this) {
            final /* synthetic */ fh mo;

            {
                this.mo = r1;
            }

            /* JADX WARNING: inconsistent code. */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                Object -l_3_R;
                JceStruct -l_1_R = null;
                synchronized (this.mo.mg) {
                    try {
                        if (this.mo.mi.size() > 0) {
                            JceStruct -l_1_R2 = new m();
                            try {
                                -l_1_R2.V = new ArrayList(this.mo.mi);
                                this.mo.mi.clear();
                                -l_1_R = -l_1_R2;
                            } catch (Throwable th) {
                                -l_3_R = th;
                                -l_1_R = -l_1_R2;
                                throw -l_3_R;
                            }
                        }
                    } catch (Throwable th2) {
                        -l_3_R = th2;
                        throw -l_3_R;
                    }
                }
            }
        }, "conchRet");
    }

    public void a(long j, final int i) {
        kv.n("WakeupUtil", "pullConch : mIdent:" + j + " conchCmdId:" + i);
        final b -l_4_R = (b) this.mh.get(i);
        if (-l_4_R != null) {
            JceStruct -l_5_R = new n();
            -l_5_R.Y = i;
            kv.d("WakeupUtil", "ECmd.Cmd_CSPullConch");
            kt.aE(1320060);
            this.mk.a(11, -l_5_R, new w(), 2, new jy(this) {
                final /* synthetic */ fh mo;

                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    kv.d("WakeupUtil", "Cmd_CSPullConch-onFinish, retCode:[" + i3 + "]dataRetCode:[" + i4 + "]");
                    if (i3 != 0) {
                        -l_4_R.tC = i3 - 65;
                    } else if (i4 != 0) {
                        -l_4_R.tC = i4 - 65;
                    } else if (i2 != 10011) {
                        -l_4_R.tC = ErrorCode.ERR_RESPONSE;
                    }
                    if (-l_4_R.tC != 0) {
                        kt.e(1320061, "" + -l_4_R.tC);
                    } else if (jceStruct != null && ((w) jceStruct).at != null && ((w) jceStruct).at.size() != 0) {
                        JceStruct -l_6_R = new l();
                        -l_6_R.V = new ArrayList();
                        Object -l_7_R = new StringBuilder();
                        if (this.mo.mj == ((s) ((w) jceStruct).at.get(0)).al) {
                            kv.n("WakeupUtil", "Pull receiveCmding conchTaskIDTemp");
                            return;
                        }
                        Object -l_8_R = ((w) jceStruct).at.iterator();
                        while (-l_8_R.hasNext()) {
                            s -l_9_R = (s) -l_8_R.next();
                            kv.n("WakeupUtil", "pullConch conchTask.taskId:" + -l_9_R.al + "|taskSeqno:" + -l_9_R.am);
                            if (-l_9_R.ap == null || -l_9_R.ap.size() == 0) {
                                kv.d("WakeupUtil", "ER_Invalid");
                                -l_7_R.append("0-;");
                                -l_6_R.V.add(this.mo.a(-l_9_R.al, -l_9_R.am, null, 3));
                            } else {
                                Object -l_10_R = -l_9_R.ap.iterator();
                                while (-l_10_R.hasNext()) {
                                    p -l_11_R = (p) -l_10_R.next();
                                    if (-l_11_R.Y != i && this.mo.mh.get(-l_11_R.Y) == null) {
                                        -l_7_R.append("2-" + -l_11_R.Y + ";");
                                        -l_6_R.V.add(this.mo.a(-l_9_R.al, -l_9_R.am, -l_11_R, 5));
                                    } else {
                                        this.mo.a(new a(-l_9_R.al, -l_9_R.am, -l_11_R));
                                        -l_6_R.V.add(this.mo.a(-l_9_R.al, -l_9_R.am, -l_11_R, 1));
                                        -l_7_R.append("1-" + -l_11_R.Y + ";");
                                    }
                                }
                            }
                        }
                        if (-l_6_R.V.size() > 0) {
                            kv.d("WakeupUtil", "Cmd_CSConchPushResult");
                            kt.e(1320061, -l_7_R.toString());
                            this.mo.mk.a(13, -l_6_R, new u(), 2, null);
                        }
                    }
                }
            });
        }
    }

    public void a(long j, int i, b bVar) {
        if (bVar != null) {
            synchronized (this.mg) {
                if (this.mh.get(i) == null) {
                    this.mh.put(i, bVar);
                }
            }
        }
    }

    public void a(long j, long j2, long j3, int i, int i2, int i3, int i4) {
        Object -l_11_R = new r();
        -l_11_R.al = j2;
        -l_11_R.am = j3;
        -l_11_R.Y = i;
        -l_11_R.af = i2;
        -l_11_R.an = i3;
        switch (i3) {
            case 1:
                -l_11_R.action = i4;
                break;
            case 2:
                -l_11_R.ao = i4;
                break;
            default:
                -l_11_R.result = i4;
                break;
        }
        synchronized (this.mg) {
            this.mi.add(-l_11_R);
        }
        j();
    }

    public void h() {
        kv.d("WakeupUtil", "registerSharkPush, ECmd.Cmd_SCPushConch");
        this.mk.a(10010, new x(), 2, this.ml);
    }

    public void i() {
        kv.d("WakeupUtil", "unRegisterSharkPush, ECmd.Cmd_SCPushConch");
        this.mk.v(10010, 2);
    }
}
