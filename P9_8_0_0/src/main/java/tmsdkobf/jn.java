package tmsdkobf;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import tmsdk.common.CallerIdent;
import tmsdk.common.utils.f;
import tmsdkobf.ji.a;
import tmsdkobf.ji.b;
import tmsdkobf.ji.c;

public class jn implements ji {
    private static Object lock = new Object();
    private static jn tl;
    Handler handler;
    private HashSet<jl> oW;
    private ConcurrentLinkedQueue<a> tk;
    b tm;
    pe<Integer, as> tn;

    private jn() {
        this.tk = new ConcurrentLinkedQueue();
        this.tm = null;
        this.tn = new pe(20);
        this.handler = null;
        this.oW = new HashSet();
        this.handler = new Handler(this, Looper.getMainLooper()) {
            final /* synthetic */ jn to;

            public void handleMessage(Message message) {
                switch (message.what) {
                    case 0:
                        this.to.handler.removeMessages(0);
                        if (this.to.tm != null && this.to.tk.size() > 0) {
                            ArrayList -l_2_R = new ArrayList();
                            Object -l_3_R = this.to.tk.iterator();
                            while (-l_3_R.hasNext()) {
                                a -l_4_R = (a) -l_3_R.next();
                                -l_3_R.remove();
                                if (-l_4_R != null) {
                                    -l_2_R.add(-l_4_R);
                                }
                            }
                            this.to.tm.j(this.to.k(-l_2_R));
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    private void a(int i, int i2, boolean z, int i3, long j, String str, byte[] bArr, short s) {
        as -l_10_R = new as();
        switch (i) {
            case 1:
                -l_10_R.bR = i2;
                -l_10_R.valueType = i;
                -l_10_R.i = i3;
                break;
            case 2:
                -l_10_R.bR = i2;
                -l_10_R.valueType = i;
                -l_10_R.bS = j;
                break;
            case 3:
                -l_10_R.bR = i2;
                -l_10_R.valueType = i;
                -l_10_R.bT = str;
                break;
            case 4:
                -l_10_R.bR = i2;
                -l_10_R.valueType = i;
                -l_10_R.bU = bArr;
                break;
            case 5:
                -l_10_R.bR = i2;
                -l_10_R.valueType = i;
                -l_10_R.bV = z;
                break;
            case 6:
                -l_10_R.bR = i2;
                -l_10_R.valueType = i;
                -l_10_R.bW = (short) s;
                break;
            default:
                return;
        }
        b(-l_10_R);
        Object -l_11_R = new a();
        -l_11_R.ta = -l_10_R;
        f.d("KeyValueProfileService", "[profile上报][" + -l_10_R.bR + "]");
        this.tk.add(-l_11_R);
        this.handler.sendEmptyMessageDelayed(0, 1000);
    }

    public static void a(String str, as asVar, String str2) {
        if (asVar != null) {
            Object -l_3_R = new StringBuilder();
            -l_3_R.append("keyid|" + asVar.bR);
            switch (asVar.valueType) {
                case 1:
                    -l_3_R.append("|int|" + asVar.i);
                    break;
                case 2:
                    -l_3_R.append("|long|" + asVar.bS);
                    break;
                case 3:
                    -l_3_R.append("|str|" + asVar.bT);
                    break;
                case 4:
                    -l_3_R.append("|byte[]|" + asVar.bU.length);
                    break;
                case 5:
                    -l_3_R.append("|bool|" + asVar.bV);
                    break;
                case 6:
                    -l_3_R.append("|short|" + asVar.bW);
                    break;
                default:
                    return;
            }
            if (str2 != null) {
                -l_3_R.append(str2);
            }
            gr.f(str, -l_3_R.toString());
        }
    }

    private void b(as asVar) {
        this.tn.put(Integer.valueOf(asVar.bR), asVar);
    }

    public static jn cx() {
        if (tl == null) {
            synchronized (lock) {
                if (tl == null) {
                    tl = new jn();
                }
            }
        }
        return tl;
    }

    private ArrayList<a> k(ArrayList<a> -l_2_R) {
        if (-l_2_R == null || -l_2_R.size() <= 1) {
            return -l_2_R;
        }
        Object -l_2_R2 = new ArrayList();
        Object -l_3_R = -l_2_R.iterator();
        while (-l_3_R.hasNext()) {
            a -l_4_R = (a) -l_3_R.next();
            if (-l_4_R != null) {
                -l_2_R2.add(-l_4_R);
            }
        }
        -l_3_R = -l_2_R2;
        -l_2_R.clear();
        Collections.sort(-l_3_R, new Comparator<a>(this) {
            final /* synthetic */ jn to;

            {
                this.to = r1;
            }

            public int a(a aVar, a aVar2) {
                int -l_3_I = aVar.ta != null ? aVar.ta instanceof as : 0;
                int -l_4_I = aVar2.ta != null ? aVar2.ta instanceof as : 0;
                if (-l_3_I == 0 && -l_4_I == 0) {
                    return 0;
                }
                if (-l_3_I == 0 && -l_4_I != 0) {
                    return -1;
                }
                if (-l_3_I != 0 && -l_4_I == 0) {
                    return 1;
                }
                if (aVar.action != aVar2.action) {
                    return aVar.action - aVar2.action;
                }
                return ((as) aVar.ta).bR - ((as) aVar2.ta).bR;
            }

            public /* synthetic */ int compare(Object obj, Object obj2) {
                return a((a) obj, (a) obj2);
            }
        });
        int -l_4_I = -l_3_R.size() - 1;
        for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
            a -l_6_R = (a) -l_3_R.get(-l_5_I);
            a -l_7_R = (a) -l_3_R.get(-l_5_I + 1);
            if (-l_6_R.action == -l_7_R.action) {
                as -l_8_R = null;
                as -l_9_R = null;
                if (-l_6_R.ta != null && (-l_6_R.ta instanceof as)) {
                    -l_8_R = (as) -l_6_R.ta;
                }
                if (-l_7_R.ta != null && (-l_7_R.ta instanceof as)) {
                    -l_9_R = (as) -l_7_R.ta;
                }
                if (-l_8_R == null) {
                    if (-l_9_R == null) {
                    }
                }
                if (-l_8_R != null && -l_9_R == null) {
                    -l_2_R.add(-l_6_R);
                } else {
                    if (-l_8_R == null) {
                        if (-l_9_R != null) {
                        }
                    }
                    if (-l_8_R.bR != -l_9_R.bR) {
                        -l_2_R.add(-l_6_R);
                    }
                }
            } else {
                -l_2_R.add(-l_6_R);
            }
        }
        if (-l_4_I >= 0) {
            -l_2_R.add(-l_3_R.get(-l_4_I));
        }
        return -l_2_R;
    }

    public void a(int i, boolean z) {
        a(5, i, z, 0, 0, null, null, (short) 0);
    }

    public void a(gt gtVar) {
        gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 4, new c(this) {
            final /* synthetic */ jn to;

            {
                this.to = r1;
            }

            public ArrayList<JceStruct> cu() {
                return jm.cw().getAll();
            }
        }, gtVar, jo.cy().cz());
    }

    public void a(b bVar) {
        this.tm = bVar;
    }

    public void a(jl jlVar) {
        this.oW.add(jlVar);
        gs.bc().a(jlVar);
    }

    public void ag(int i) {
    }

    public void b(jl jlVar) {
        this.oW.remove(jlVar);
        gs.bc().b(jlVar);
    }

    public int cs() {
        return 4;
    }

    public void ct() {
        Object -l_1_R = jm.cw().getAll();
        if (-l_1_R != null && -l_1_R.size() > 0) {
            Object -l_3_R = -l_1_R.iterator();
            while (-l_3_R.hasNext()) {
                JceStruct -l_4_R = (JceStruct) -l_3_R.next();
                if (-l_4_R instanceof as) {
                    as -l_2_R = (as) -l_4_R;
                    Object -l_5_R = new StringBuilder();
                    -l_5_R.append("key|");
                    -l_5_R.append(-l_2_R.bR);
                    -l_5_R.append("|valueType|");
                    -l_5_R.append(-l_2_R.valueType);
                    -l_5_R.append("|value|");
                    switch (-l_2_R.valueType) {
                        case 1:
                            -l_5_R.append(-l_2_R.i);
                            break;
                        case 2:
                            -l_5_R.append(-l_2_R.bS);
                            break;
                        case 3:
                            -l_5_R.append(-l_2_R.bT);
                            break;
                        case 4:
                            -l_5_R.append(Arrays.toString(-l_2_R.bU));
                            break;
                    }
                    f.d("KeyValueProfileService", -l_5_R.toString());
                }
            }
        }
    }

    public boolean h(ArrayList<a> arrayList) {
        if (arrayList == null || arrayList.size() <= 0) {
            return false;
        }
        Object -l_2_R = new ArrayList();
        int -l_3_I = jm.cw().a(arrayList, -l_2_R);
        Object -l_4_R = arrayList.iterator();
        while (-l_4_R.hasNext()) {
            a -l_5_R = (a) -l_4_R.next();
            if (!(-l_5_R == null || -l_5_R.ta == null || !(-l_5_R.ta instanceof as))) {
                a("KeyValueProfileService", (as) -l_5_R.ta, "|ret|" + -l_3_I);
            }
        }
        if (-l_3_I != 0 && -l_2_R.size() > 0) {
            int -l_4_I = 0;
            while (-l_4_I < -l_2_R.size()) {
                Boolean -l_5_R2 = (Boolean) -l_2_R.get(-l_4_I);
                if (-l_5_R2 != null && !-l_5_R2.booleanValue() && arrayList.size() > -l_4_I && (((a) arrayList.get(-l_4_I)).ta instanceof as)) {
                    Object -l_7_R = gr.a(4, 0, (as) ((a) arrayList.get(-l_4_I)).ta);
                    if (-l_7_R != null) {
                        jo.cy().aj(-l_7_R.length);
                        gs.bc().j(4, jo.cy().cz());
                    }
                }
                -l_4_I++;
            }
        }
        return -l_3_I;
    }

    public void i(ArrayList<a> arrayList) {
        if (arrayList != null && arrayList.size() > 0) {
            ArrayList arrayList2 = null;
            ArrayList arrayList3 = null;
            ArrayList arrayList4 = null;
            Object -l_5_R;
            a -l_6_R;
            if (jo.cy().cA()) {
                jo.cy().j(false);
                -l_5_R = arrayList.iterator();
                while (-l_5_R.hasNext()) {
                    -l_6_R = (a) -l_5_R.next();
                    if (!(-l_6_R == null || -l_6_R.ta == null || !(-l_6_R.ta instanceof as))) {
                        if (arrayList4 == null) {
                            arrayList4 = new ArrayList();
                        }
                        arrayList4.add((as) -l_6_R.ta);
                    }
                }
            } else {
                -l_5_R = arrayList.iterator();
                while (-l_5_R.hasNext()) {
                    -l_6_R = (a) -l_5_R.next();
                    if (!(-l_6_R == null || -l_6_R.ta == null || !(-l_6_R.ta instanceof as))) {
                        as -l_7_R = (as) -l_6_R.ta;
                        if (jm.cw().ai(-l_7_R.bR) <= 0) {
                            if (arrayList3 == null) {
                                arrayList3 = new ArrayList();
                            }
                            arrayList3.add(-l_7_R);
                        } else {
                            if (arrayList2 == null) {
                                arrayList2 = new ArrayList();
                            }
                            arrayList2.add(-l_7_R);
                        }
                    }
                }
            }
            if (arrayList4 != null && arrayList4.size() > 0) {
                gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 4, 0, arrayList4);
            }
            if (arrayList2 != null && arrayList2.size() > 0) {
                gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 4, 3, arrayList2);
            }
            if (arrayList3 != null && arrayList3.size() > 0) {
                gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 4, 1, arrayList3);
            }
        }
    }

    public void l(int i, int i2) {
        a(1, i, false, i2, 0, null, null, (short) 0);
    }

    public void onImsiChanged() {
        f.d("ImsiChecker", "KV-setFirstReport:[true]");
        jo.cy().j(true);
    }
}
