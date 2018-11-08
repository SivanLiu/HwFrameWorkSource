package tmsdkobf;

import android.content.Context;
import com.tencent.tcuser.util.a;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import tmsdk.common.TMSDKContext;

public class fr {
    private static volatile fr nc = null;
    private fv mZ;
    private fw na;
    private Map<String, String> nb = new HashMap();

    private void G(String str) {
        Object -l_2_R = new File(str);
        if (-l_2_R.exists() && -l_2_R.isFile()) {
            -l_2_R.delete();
        }
    }

    private void a(Context context, Object obj, String str, String str2) {
        try {
            Object -l_7_R = gb.a(context, str, obj, gb.e(context) + File.separator + str2);
            if (-l_7_R != null) {
                if (-l_7_R instanceof fv) {
                    this.mZ = (fv) -l_7_R;
                } else if (-l_7_R instanceof fw) {
                    this.na = (fw) -l_7_R;
                }
            }
        } catch (Exception e) {
        }
    }

    public static fr r() {
        if (nc == null) {
            Object -l_0_R = fr.class;
            synchronized (fr.class) {
                if (nc == null) {
                    nc = new fr();
                }
            }
        }
        return nc;
    }

    public static void s() {
        nc = null;
    }

    public void a(int i, String str) {
        if (str != null) {
            kt.e(i, str);
        }
    }

    public void a(Context context) {
        a(context, new fv(), "cloudcmd", "tms_config.dat");
        if (this.mZ != null) {
            a(context, new fw(), "localrecord", "tms_record.dat");
            if (this.na == null) {
                this.na = new fw();
            } else {
                Object -l_2_R = this.na.I();
                if (-l_2_R != null && -l_2_R.size() > 0) {
                    Object -l_3_R = -l_2_R.iterator();
                    while (-l_3_R.hasNext()) {
                        fu -l_4_R = (fu) -l_3_R.next();
                        this.nb.put(-l_4_R.nf, -l_4_R.nn);
                    }
                }
            }
        }
    }

    public boolean a(t tVar, int i) {
        Object -l_3_R = new fv();
        t();
        int -l_5_I = a.av((String) tVar.ar.get(0)) == 0 ? 0 : 1;
        if (-l_5_I == 0) {
            return true;
        }
        if ((System.currentTimeMillis() / 1000 <= ((long) i) ? 1 : null) == null) {
            return false;
        }
        -l_3_R.d(-l_5_I);
        -l_3_R.H(i);
        -l_3_R.O((String) tVar.ar.get(1));
        -l_3_R.P((String) tVar.ar.get(2));
        -l_3_R.Q((String) tVar.ar.get(3));
        -l_3_R.R((String) tVar.ar.get(4));
        -l_3_R.S((String) tVar.ar.get(5));
        -l_3_R.T((String) tVar.ar.get(6));
        -l_3_R.U((String) tVar.ar.get(7));
        this.mZ = -l_3_R;
        int -l_8_I = b(TMSDKContext.getApplicaionContext(), this.mZ, "cloudcmd", "tms_config.dat");
        if (-l_8_I == 0) {
            kt.e(1320067, "0");
        } else {
            kt.e(1320067, "1");
        }
        return -l_8_I;
    }

    public void b(Context context) {
        if (this.nb != null) {
            Object -l_2_R = new ArrayList();
            for (String -l_4_R : this.nb.keySet()) {
                Object -l_5_R = new fu();
                -l_5_R.nf = -l_4_R;
                -l_5_R.nn = (String) this.nb.get(-l_4_R);
                -l_2_R.add(-l_5_R);
            }
            this.na.g(-l_2_R);
            b(context, this.na, "localrecord", "tms_record.dat");
            this.nb.clear();
        }
    }

    public boolean b(Context context, Object obj, String str, String str2) {
        try {
            Object -l_6_R = gb.e(context) + File.separator + str2;
            if (obj != null && gb.c(context, obj, str, -l_6_R) == 0) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void t() {
        Object -l_1_R = gb.e(TMSDKContext.getApplicaionContext());
        G(-l_1_R + File.separator + "tms_config.dat");
        G(-l_1_R + File.separator + "tms_record.dat");
    }

    public fv u() {
        return this.mZ;
    }

    public Map<String, String> v() {
        return this.nb;
    }
}
