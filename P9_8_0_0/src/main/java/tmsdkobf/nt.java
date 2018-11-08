package tmsdkobf;

import android.os.PowerManager;
import android.text.TextUtils;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdkobf.nx.b;

public class nt {
    private static nt Ef = null;
    private nl CT = null;
    private pe<Integer, a> Ee = new pe(SmsCheckResult.ESCT_200);
    private PowerManager Eg = null;
    public Map<Byte, Integer> Eh = new HashMap();

    public static class a {
        public BitSet Ej = new BitSet();
        public String Ek = "";
        public int El = 0;
        public boolean Em = false;
        public boolean En = false;
        public long Eo = 0;
        public int Ep = 0;
        public int Eq = 0;
        public long Er = System.currentTimeMillis();
        public String Es = "";
        public String Et = "";
        public int bz = 0;
        public long createTime = System.currentTimeMillis();
        public int eB = 0;
    }

    public static nt ga() {
        Object -l_0_R = nt.class;
        synchronized (nt.class) {
            if (Ef == null) {
                Ef = new nt();
            }
            return Ef;
        }
    }

    public void a(byte b, int i) {
        synchronized (this.Eh) {
            this.Eh.put(Byte.valueOf(b), Integer.valueOf(i));
        }
    }

    public synchronized void a(int i, long j, String str) {
        Object -l_5_R = new a();
        -l_5_R.Ek = str;
        -l_5_R.Eo = j;
        -l_5_R.El = np.fS().c(false, false);
        if (this.Eg != null) {
            try {
                -l_5_R.Em = this.Eg.isScreenOn();
            } catch (Throwable th) {
            }
        }
        this.Ee.put(Integer.valueOf(i), -l_5_R);
    }

    public synchronized void a(String str, int i, int i2, bw bwVar, int i3) {
        a(str, i, i2, bwVar, i3, 0, null);
    }

    public synchronized void a(String str, int i, int i2, bw bwVar, int i3, int i4, String str2) {
        a -l_8_R = (a) this.Ee.get(Integer.valueOf(i2));
        if (-l_8_R != null) {
            nv.r("" + str, "[ocean][shark_funnel]|seqNo|seq_" + i2 + "|step|" + i3 + "|cmdId|cmd_" + i + "|stepTime|" + (System.currentTimeMillis() - -l_8_R.Er) + "|retCode|" + i4 + "|flow|" + str2);
            if (i == 21) {
                qg.a(65542, "|step|" + i3 + "|cmdId|" + i + "|retCode|" + i4);
            }
            -l_8_R.bz = i;
            -l_8_R.Ej.set(i3, true);
            if (str2 != null) {
                -l_8_R.Es = str2;
            }
            if (i3 == 14 || i3 == 9 || i3 == 10) {
                -l_8_R.Ep = i4;
                if (this.CT != null) {
                    this.CT.c(i, i4);
                }
            } else if (i3 != 16) {
                -l_8_R.eB = i4;
            } else {
                -l_8_R.Eq = i4;
                if (this.CT != null) {
                    this.CT.d(i, i4);
                }
            }
            -l_8_R.Er = System.currentTimeMillis();
        }
    }

    public synchronized void a(String str, int i, int i2, ce ceVar, int i3, int i4) {
        a(str, i, i2, ceVar, i3, i4, null);
    }

    public synchronized void a(String str, int i, int i2, ce ceVar, int i3, int i4, String str2) {
        a -l_8_R = (a) this.Ee.get(Integer.valueOf(i2));
        if (-l_8_R != null) {
            nv.r("" + str, "[ocean][shark_funnel]|seqNo|seq_" + i2 + "|step|" + i3 + "|cmdId|cmd_" + i + "|stepTime|" + (System.currentTimeMillis() - -l_8_R.Er) + "|retCode|" + i4 + "|flow|" + str2);
            if (i == 10021) {
                qg.a(65542, "|step|" + i3 + "|cmdId|" + i + "|retCode|" + i4);
            }
            -l_8_R.bz = i;
            if (str2 != null) {
                -l_8_R.Et = str2;
            }
            -l_8_R.Ej.set(i3, true);
            if (i3 == 14) {
                -l_8_R.Ep = i4;
            } else if (i3 != 16) {
                -l_8_R.eB = i4;
            } else {
                -l_8_R.Eq = i4;
            }
            -l_8_R.Er = System.currentTimeMillis();
        }
    }

    public void b(byte b) {
        synchronized (this.Eh) {
            this.Eh.remove(Byte.valueOf(b));
        }
    }

    public synchronized void b(nl nlVar) {
        this.CT = nlVar;
        try {
            this.Eg = (PowerManager) TMSDKContext.getApplicaionContext().getSystemService("power");
        } catch (Throwable th) {
        }
        nx.gs().a(new b(this) {
            final /* synthetic */ nt Ei;

            {
                this.Ei = r1;
            }

            public void gb() {
                synchronized (this.Ei) {
                    if (this.Ei.Ee.size() > 0) {
                        mb.n("SharkFunnelModel", "[tcp_control]mark network changed for every running task, seqNos: " + this.Ei.Ee.hH().keySet());
                        for (Entry -l_4_R : this.Ei.Ee.hH().entrySet()) {
                            ((a) -l_4_R.getValue()).En = true;
                        }
                    }
                }
            }
        });
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean b(int i, boolean z) {
        a -l_3_R = (a) this.Ee.get(Integer.valueOf(i));
        if (-l_3_R == null) {
            return false;
        }
        int -l_7_I;
        this.Ee.f(Integer.valueOf(i));
        int -l_4_I = -l_3_R.Ej.get(15);
        int -l_5_I = -l_3_R.eB;
        if (-l_5_I != 0) {
            int -l_6_I = ne.bi(-l_5_I);
            -l_7_I = -l_3_R.El;
            if (-l_7_I == -2) {
                -l_5_I = (-l_5_I - -l_6_I) - 160000;
            } else if (-l_6_I == -50000) {
                int -l_8_I = -l_6_I;
                if (-l_3_R.En) {
                    -l_8_I = -550000;
                } else if (-l_7_I == -4) {
                    -l_8_I = -530000;
                } else if (-l_7_I == -1) {
                    -l_8_I = -220000;
                } else if (-l_7_I == -3) {
                    -l_8_I = -540000;
                }
                -l_5_I = (-l_5_I - -l_6_I) + -l_8_I;
            }
        }
        Object -l_6_R = new StringBuilder();
        -l_6_R.append("|cmd|cmd_");
        -l_6_R.append(-l_3_R.bz);
        -l_6_R.append("|seqNo|seq_");
        -l_6_R.append(i);
        if (!TextUtils.isEmpty(-l_3_R.Ek)) {
            -l_6_R.append("|reason|");
            -l_6_R.append(-l_3_R.Ek);
        }
        -l_6_R.append("|channel|");
        -l_6_R.append(-l_4_I == 0 ? "tcp" : "http");
        -l_6_R.append("|step|");
        -l_6_R.append(-l_3_R.Ej.toString());
        -l_6_R.append("|netState|");
        -l_6_R.append(np.bn(-l_3_R.El));
        -l_6_R.append("|isScreenOn|");
        -l_6_R.append(-l_3_R.Em);
        -l_6_R.append("|isNetworkChanged|");
        -l_6_R.append(-l_3_R.En);
        -l_6_R.append("|tcpRetCode|");
        -l_6_R.append(-l_3_R.Ep);
        -l_6_R.append("|httpRecCode|");
        -l_6_R.append(-l_3_R.Eq);
        -l_6_R.append("|retCode|");
        if (-l_5_I != -l_3_R.eB) {
            -l_6_R.append(-l_3_R.eB).append("->").append(-l_5_I);
        } else {
            -l_6_R.append(-l_3_R.eB);
        }
        -l_6_R.append("|timeOut|");
        -l_6_R.append(-l_3_R.Eo);
        -l_6_R.append("|totalTime|");
        -l_6_R.append(System.currentTimeMillis() - -l_3_R.createTime);
        -l_6_R.append("|sendFlow|");
        -l_6_R.append(-l_3_R.Es);
        -l_6_R.append("|recFlow|");
        -l_6_R.append(-l_3_R.Et);
        if (-l_3_R.eB == 0) {
            nv.z("SharkFunnelModel", "[shark_funnel]" + -l_6_R.toString());
        } else if (z) {
            nv.A("SharkFunnelModel", "xxxxxxxxxxxx [shark_funnel]" + -l_6_R.toString());
        } else {
            nv.A("SharkFunnelModel", "tttt [shark_funnel]" + -l_6_R.toString());
        }
        if (this.CT != null) {
            -l_7_I = -l_3_R.bz <= 10000 ? -l_3_R.bz : -l_3_R.bz - 10000;
            if (!(-l_7_I == 999 || -l_7_I == 794 || -l_7_I == 797 || -l_7_I == 782)) {
                if (-l_5_I == 0) {
                    this.CT.e(-l_7_I, -l_5_I);
                } else if (z) {
                    this.CT.e(-l_7_I, -l_5_I);
                }
            }
        }
    }

    public synchronized void bp(int i) {
        this.Ee.f(Integer.valueOf(i));
    }

    public synchronized boolean bq(int i) {
        return b(i, true);
    }

    public int c(byte b) {
        synchronized (this.Eh) {
            Integer -l_3_R = (Integer) this.Eh.get(Byte.valueOf(b));
            if (-l_3_R == null) {
                return -1;
            }
            int intValue = -l_3_R.intValue();
            return intValue;
        }
    }
}
