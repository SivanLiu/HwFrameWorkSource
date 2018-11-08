package tmsdkobf;

import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.util.SparseIntArray;
import com.qq.taf.jce.JceStruct;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import tmsdk.common.CallerIdent;
import tmsdk.common.TMSDKContext;
import tmsdkobf.ji.c;

public class gs {
    private static Object lock = new Object();
    private static int[] oJ;
    private static gs oN;
    private HandlerThread mHandlerThread = ((ki) fj.D(4)).newFreeHandlerThread("profile upload task queue");
    private gw oG;
    private gq oH = gq.aZ();
    private HashMap<Integer, gn> oI = new HashMap();
    private Set<Integer> oK = new HashSet();
    private pf oL = new pf("profile4", 43200000, 24);
    private pf oM = new pf("profile2", 43200000, 12);
    private State oO = State.UNKNOWN;
    private State oP = State.UNKNOWN;
    private tmsdkobf.kh.a oQ = new tmsdkobf.kh.a(this) {
        final /* synthetic */ gs oX;

        {
            this.oX = r1;
        }
    };
    private ka oR = new ka(this) {
        final /* synthetic */ gs oX;

        {
            this.oX = r1;
        }

        public oh<Long, Integer, JceStruct> a(int i, long j, int i2, JceStruct jceStruct) {
            if (i2 == 11052) {
                nv.r("ProfileUpload", "recv profile full upload push");
                at -l_6_R = (at) jceStruct;
                if (-l_6_R != null) {
                    nv.r("ProfileUpload", "profilePush.profileID : " + -l_6_R.bK + " profilePush.profileCmd : " + -l_6_R.bY);
                    switch (-l_6_R.bY) {
                        case 1:
                            this.oX.c(-l_6_R.bK, 0);
                            if (-l_6_R.bK == 2) {
                                this.oX.oH.g(true);
                                jk.cv().ah(-l_6_R.bK);
                                break;
                            }
                            break;
                        case 2:
                            if (-l_6_R.bK == 2) {
                                this.oX.oH.g(false);
                                break;
                            }
                            break;
                    }
                    return null;
                }
                nv.r("ProfileUpload", "profilePush == null");
                return null;
            }
            mb.o("ProfileUpload", "cmdId != ECmd.Cmd_SCProfilePushCmd : " + i2);
            return null;
        }
    };
    private ConcurrentHashMap<Integer, a> oS = new ConcurrentHashMap();
    private SparseIntArray oT = new SparseIntArray();
    private Handler oU;
    private Callback oV = new Callback(this) {
        final /* synthetic */ gs oX;

        {
            this.oX = r1;
        }

        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.oX.Y(message.arg1);
                    break;
                case 2:
                    int -l_2_I = message.arg1;
                    ArrayList -l_4_R = this.oX.X(-l_2_I);
                    if (-l_4_R != null && -l_4_R.size() > 0) {
                        Object -l_5_R = gr.a(gr.a(-l_2_I, 0, -l_4_R));
                        if (-l_5_R != null) {
                            this.oX.oT.put(-l_2_I, -l_5_R.length);
                            break;
                        }
                    }
                    gr.f("ProfileUpload", "get full upload jce");
                    return true;
                    break;
                case 3:
                    this.oX.oT.delete(message.arg1);
                    break;
                case 4:
                    new Bundle().putInt("profile.id", message.arg1);
                    break;
                case 5:
                    this.oX.a((gu) message.obj);
                    break;
                case 6:
                    this.oX.b((tmsdkobf.gp.a) message.obj);
                    break;
                case 7:
                    this.oX.a((gn) message.obj);
                    break;
                case 8:
                    this.oX.bd();
                    break;
                case 9:
                    this.oX.a((gx) message.obj);
                    break;
                case 10:
                    this.oX.oK.clear();
                    break;
            }
            return true;
        }
    };
    private HashSet<jl> oW = new HashSet();

    static class a {
        public c oY;
        public gt oZ;

        public a(c cVar, gt gtVar) {
            this.oY = cVar;
            this.oZ = gtVar;
        }
    }

    class b implements jy {
        final /* synthetic */ gs oX;
        tmsdkobf.gp.a pa;

        public b(gs gsVar, tmsdkobf.gp.a aVar) {
            this.oX = gsVar;
            this.pa = aVar;
        }

        public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
            Message.obtain(this.oX.oU, 9, new gx(i, i2, i3, i4, jceStruct, this.pa)).sendToTarget();
        }
    }

    private gs() {
        State state;
        this.mHandlerThread.start();
        this.oU = new Handler(this.mHandlerThread.getLooper(), this.oV);
        this.oG = (gw) fj.D(5);
        this.oG.a(11052, new at(), 2, this.oR);
        Object -l_3_R = ((ConnectivityManager) TMSDKContext.getApplicaionContext().getSystemService("connectivity")).getActiveNetworkInfo();
        if (-l_3_R == null) {
            state = State.DISCONNECTED;
            this.oO = state;
        } else {
            state = -l_3_R.getState();
            this.oO = state;
        }
        this.oP = state;
        h(30000);
        oJ = new int[6];
        for (int -l_4_I = 1; -l_4_I < 5; -l_4_I++) {
            oJ[-l_4_I] = 0;
        }
    }

    private void Y(int i) {
        a -l_2_R = (a) this.oS.get(Integer.valueOf(i));
        if (-l_2_R == null || -l_2_R.oY == null) {
            gr.f("ProfileUpload", "profileID " + i + " callback null,can't full upload");
            return;
        }
        Object -l_3_R = -l_2_R.oY.cu();
        if (-l_3_R != null) {
            a(CallerIdent.getIdent(1, 4294967296L), i, 0, -l_3_R);
        } else {
            gr.f("ProfileUpload", "get full upload profile null,can't full upload");
        }
    }

    private pf Z(int i) {
        switch (i) {
            case 2:
                return this.oM;
            case 4:
                return this.oL;
            default:
                return null;
        }
    }

    private void a(gn gnVar) {
        if (gnVar != null) {
            int -l_2_I = gp.aV().aW();
            nv.r("ProfileUpload", "enqueue force push taskID : " + -l_2_I);
            if (gnVar.oy == null || !gnVar.oy.cJ()) {
                this.oI.put(Integer.valueOf(-l_2_I), gnVar);
                bd();
            }
        }
    }

    private void a(tmsdkobf.gp.a aVar) {
        if (aVar != null) {
            gr.f("ProfileUpload", "---onSharkUnknown");
            if (aVar.bK > 0 && aVar.bK < 5) {
                Object -l_2_R = Z(aVar.bK);
                if (-l_2_R == null) {
                    c(aVar.bK, 0);
                } else if (-l_2_R.hI()) {
                    -l_2_R.hJ();
                    c(aVar.bK, 0);
                    gr.f("ProfileUpload", "unknown err,full upload");
                } else {
                    gr.f("ProfileUpload", "unknown err,full upload,freq ctrl,ignore this");
                }
            }
        }
    }

    private void a(gu guVar) {
        Object obj = null;
        if (guVar != null) {
            int -l_2_I;
            int -l_3_I = 0;
            Object -l_5_R = gr.a(guVar.bK, guVar.pb, guVar.pc);
            gr.f("ProfileUpload", "profileEnqueue");
            int -l_4_I = -l_5_R.bK;
            int -l_6_I = -l_5_R.bO;
            Object -l_7_R = gp.aV();
            if (this.oH.b(-l_5_R) && -l_6_I != 0) {
                gr.f("ProfileUpload", "ingnore this, the same as the last upload task");
                -l_2_I = -1;
            } else {
                Object -l_8_R = gr.a(-l_5_R);
                if (-l_8_R == null || -l_8_R.length == 0) {
                    -l_2_I = -2;
                } else {
                    -l_3_I = -l_8_R.length;
                    if (-l_6_I == 0) {
                        -l_7_R.O(-l_4_I);
                        this.oH.T(-l_4_I);
                    }
                    long -l_9_J = (long) -l_7_R.a(-l_8_R, -l_4_I);
                    gr.f("ProfileUpload", "profileEnqueue taskID : " + -l_9_J);
                    if (-l_9_J >= 0) {
                        obj = 1;
                    }
                    if (obj == null) {
                        gr.f("ProfileUpload", "pushLast fail!!!");
                        -l_2_I = -3;
                    } else {
                        -l_2_I = 0;
                        this.oH.h(-l_4_I, -l_8_R.length);
                        if (-l_6_I == 0) {
                            this.oT.put(-l_4_I, -l_8_R.length);
                        }
                        this.oH.a(-l_5_R);
                    }
                }
            }
            a -l_5_R2 = (a) this.oS.get(Integer.valueOf(-l_4_I));
            if (!(-l_5_R2 == null || -l_5_R2.oZ == null)) {
                -l_5_R2.oZ.a(guVar.pb, guVar.pc, -l_2_I, -l_3_I);
            }
            bd();
        }
    }

    private void a(gx gxVar) {
        if (gxVar != null) {
            tmsdkobf.gp.a -l_2_R = gxVar.pa;
            int -l_3_I = gxVar.eC;
            int -l_4_I = gxVar.eB;
            if (-l_2_R != null) {
                Object -l_8_R;
                Object -l_5_R = new ArrayList();
                Object -l_6_R = -l_2_R.aY();
                if (!(-l_6_R == null || -l_6_R.bN == null)) {
                    Object -l_7_R = -l_6_R.bN.iterator();
                    while (-l_7_R.hasNext()) {
                        byte[] -l_8_R2 = (byte[]) -l_7_R.next();
                        if (-l_6_R.bK == 4) {
                            -l_5_R.add(nn.a(-l_8_R2, new as(), false));
                        }
                    }
                }
                mb.d("TrafficCorrection", "ProfileUpload-retCode:[" + -l_4_I + "]dataRetCode[" + -l_3_I + "]");
                int -l_7_I = 0;
                if (-l_4_I != 0 || -l_3_I != 0) {
                    -l_7_I = -1;
                }
                synchronized (this.oW) {
                    -l_8_R = new HashSet(this.oW);
                }
                Object -l_9_R = -l_8_R.iterator();
                while (-l_9_R.hasNext()) {
                    ((jl) -l_9_R.next()).a(-l_5_R, -l_7_I);
                }
                this.oK.remove(Integer.valueOf(-l_2_R.ox));
                if (-l_2_R.aY() == null) {
                    gr.f("ProfileUpload", "recv profile resp retCode : " + -l_4_I + " dataRetCode : " + -l_3_I);
                } else {
                    gr.f("ProfileUpload", "recv profile resp retCode : " + -l_4_I + " dataRetCode : " + -l_3_I + " profileID : " + -l_2_R.aY().bK + " actionID : " + -l_2_R.aY().bO + " lastVerifyKey " + -l_2_R.aY().bL + " presentVerifyKey " + -l_2_R.aY().bM + " taskID " + -l_2_R.ox);
                }
                if (-l_4_I == 0) {
                    switch (-l_3_I) {
                        case -1:
                            c(-l_2_R);
                            break;
                        case 0:
                            b(-l_2_R);
                            break;
                        default:
                            a(-l_2_R);
                            break;
                    }
                } else if (-l_4_I > 0) {
                    d(-l_2_R);
                }
            }
        }
    }

    private void b(tmsdkobf.gp.a aVar) {
        if (aVar != null) {
            Object -l_2_R = gp.aV();
            switch (aVar.oD) {
                case 0:
                    if (aVar.aY() != null) {
                        int -l_3_I = aVar.aY().bK;
                        gr.f("ProfileUpload", "+++onSharkSuccess");
                        oJ[-l_3_I] = 0;
                        Object -l_4_R = -l_2_R.P(aVar.ox);
                        if (-l_4_R != null) {
                            this.oH.i(-l_3_I, -l_4_R.length);
                            gr.f("ProfileUpload", "popFirst success! taskID : " + aVar.ox);
                            break;
                        }
                        String str = "ProfileUpload";
                        gr.f(str, "popFirst fail! queueQuantity : " + this.oH.S(-l_3_I) + " taskID : " + aVar.ox);
                        break;
                    }
                    return;
                case 1:
                    -l_2_R.P(aVar.ox);
                    gr.f("ProfileUpload", "popFirst success! taskID : " + aVar.ox);
                    break;
            }
        }
    }

    public static gs bc() {
        if (oN == null) {
            synchronized (lock) {
                if (oN == null) {
                    oN = new gs();
                }
            }
        }
        return oN;
    }

    private void bd() {
        gr.f("ProfileUpload", "uploadTask");
        Object<tmsdkobf.gp.a> -l_2_R = gp.aV().aX();
        if (-l_2_R == null || -l_2_R.size() == 0) {
            mb.s("ProfileUpload", "uploadTask no more task");
            return;
        }
        Object -l_3_R = -l_2_R.iterator();
        while (-l_3_R.hasNext()) {
            if (this.oK.contains(Integer.valueOf(((tmsdkobf.gp.a) -l_3_R.next()).ox))) {
                -l_3_R.remove();
            }
        }
        if (-l_2_R == null || -l_2_R.size() == 0) {
            mb.s("ProfileUpload", "all task is uploading");
            return;
        }
        int -l_4_I = 0;
        for (tmsdkobf.gp.a -l_6_R : -l_2_R) {
            if (!(-l_6_R == null || -l_6_R.aY() == null)) {
                int -l_7_I = -l_6_R.aY().bK;
                if (-l_6_R.aY().bO != 0) {
                    int -l_8_I = this.oT.get(-l_7_I);
                    int -l_9_I = this.oH.S(-l_7_I);
                    if (-l_9_I > -l_8_I && -l_8_I > 0) {
                        gr.f("ProfileUpload", "queue more than full,then full upload. queue : " + -l_9_I + " quantity : " + -l_8_I);
                        c(-l_7_I, 0);
                        -l_4_I = 1;
                    }
                }
            }
        }
        if (-l_4_I == 0) {
            for (tmsdkobf.gp.a -l_6_R2 : -l_2_R) {
                if (-l_6_R2.aY() != null) {
                    this.oK.add(Integer.valueOf(-l_6_R2.ox));
                    Object -l_7_R = -l_6_R2.aY();
                    gr.f("ProfileUpload", "send : profileID " + -l_7_R.bK + " actionID " + -l_7_R.bO + " lastVerifyKey " + -l_7_R.bL + " presentVerifyKey " + -l_7_R.bM + " taskID " + -l_6_R2.ox);
                    this.oG.a(1051, (JceStruct) -l_7_R, new au(), 18, new b(this, -l_6_R2), 90000);
                } else if (-l_6_R2.oD != 1) {
                    gr.f("ProfileUpload", "ProfileQueueTask neither force push nor upload profile");
                } else {
                    gn -l_7_R2 = (gn) this.oI.remove(Integer.valueOf(-l_6_R2.ox));
                    if (-l_7_R2 != null) {
                        if (!(-l_7_R2 == null || -l_7_R2.oy == null)) {
                            if (!-l_7_R2.oy.cJ()) {
                            }
                        }
                        this.oK.add(Integer.valueOf(-l_6_R2.ox));
                        gr.f("ProfileUpload", "send : cmdid : " + -l_7_R2.Y + " taskID : " + -l_7_R2.ox);
                        this.oG.c(-l_7_R2.pid, -l_7_R2.on, -l_7_R2.oo, -l_7_R2.ex, -l_7_R2.op, -l_7_R2.Y, -l_7_R2.oq, -l_7_R2.or, -l_7_R2.os, -l_7_R2.eE, -l_7_R2.ot, -l_7_R2.ou, -l_7_R2.ov, -l_7_R2.ow);
                        b(-l_6_R2);
                    }
                    b(-l_6_R2);
                }
            }
        }
    }

    private void c(int i, long j) {
        Object -l_4_R = Message.obtain(this.oU, 1, i, 0);
        this.oU.removeMessages(1);
        this.oU.sendMessageDelayed(-l_4_R, j);
    }

    private void c(tmsdkobf.gp.a aVar) {
        if (aVar != null) {
            gr.f("ProfileUpload", "---onSharkFail");
            int[] iArr = oJ;
            int i = aVar.bK;
            int i2 = iArr[i] + 1;
            iArr[i] = i2;
            if (i2 <= 2) {
                gr.f("ProfileUpload", "resend");
                h((long) (oJ[aVar.bK] * 30000));
                return;
            }
            oJ[aVar.bK] = 0;
            if (aVar.bK > 0 && aVar.bK < 5) {
                Object -l_2_R = Z(aVar.bK);
                if (-l_2_R == null) {
                    c(aVar.bK, 0);
                } else if (-l_2_R.hI()) {
                    -l_2_R.hJ();
                    c(aVar.bK, 0);
                    gr.f("ProfileUpload", "err more than 2,full upload");
                } else {
                    gr.f("ProfileUpload", "err more than 2,full upload,freq ctrl,ignore this");
                }
            }
        }
    }

    private void d(tmsdkobf.gp.a aVar) {
        if (aVar != null) {
            gr.f("ProfileUpload", "---onSharkFail");
            int[] iArr = oJ;
            int i = aVar.bK;
            int i2 = iArr[i] + 1;
            iArr[i] = i2;
            if (i2 <= 1) {
                gr.f("ProfileUpload", "resend");
                h((long) (oJ[aVar.bK] * 30000));
                return;
            }
            oJ[aVar.bK] = 0;
            gr.f("ProfileUpload", "err more than 1,wait next upload task");
        }
    }

    private void h(long j) {
        if (this.oP.compareTo(State.CONNECTED) != 0) {
            gr.f("ProfileUpload", "no network");
            return;
        }
        this.oU.removeMessages(8);
        this.oU.sendEmptyMessageDelayed(8, j);
    }

    public ArrayList<JceStruct> X(int i) {
        a -l_2_R = (a) this.oS.get(Integer.valueOf(i));
        return (-l_2_R == null || -l_2_R.oY == null) ? null : -l_2_R.oY.cu();
    }

    public void a(long j, int i, c cVar, gt gtVar, int i2) {
        if (((a) this.oS.put(Integer.valueOf(i), new a(cVar, gtVar))) == null) {
            this.oT.put(i, i2);
        }
    }

    public void a(jl jlVar) {
        synchronized (this.oW) {
            this.oW.add(jlVar);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean a(long j, int i, int i2, ArrayList<JceStruct> arrayList) {
        if (i <= 0 || i >= 5 || this.oS.get(Integer.valueOf(i)) == null) {
            return false;
        }
        gr.f("ProfileUpload", "profileUpload  profileID : " + i + " profileActionID : " + i2);
        Message.obtain(this.oU, 5, new gu(i, i2, arrayList, null)).sendToTarget();
        return true;
    }

    public WeakReference<kd> b(int i, int i2, int i3, long j, long j2, int i4, JceStruct jceStruct, byte[] bArr, JceStruct jceStruct2, int i5, jy jyVar, jz jzVar, long j3, long j4) {
        Object -l_19_R = new kd();
        Message.obtain(this.oU, 7, new gn(i, i2, i3, j, j2, i4, jceStruct, bArr, jceStruct2, i5, jyVar, jzVar, j3, j4, -l_19_R)).sendToTarget();
        return new WeakReference(-l_19_R);
    }

    public void b(jl jlVar) {
        synchronized (this.oW) {
            this.oW.remove(jlVar);
        }
    }

    public void j(int i, int i2) {
        this.oT.put(i, i2);
    }
}
