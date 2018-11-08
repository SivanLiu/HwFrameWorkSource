package tmsdkobf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import tmsdk.common.TMSDKContext;

public class qr implements qq {
    final boolean NZ;
    final boolean Oa;
    final ra Ob;
    public Set<String> Oc;
    public Map<String, qv> Od;
    List<qt> Oe = new ArrayList();
    qt Of;
    List<qt> Og;
    List<qt> Oh;

    public qr(ra raVar) {
        this.Ob = raVar;
        this.NZ = this.Ob.jS();
        this.Oa = this.Ob.jR();
    }

    private String a(qv qvVar, boolean z, boolean z2) {
        if (qvVar == null) {
            return null;
        }
        Object -l_4_R = new StringBuilder();
        -l_4_R.append(!z ? "0;" : "1;");
        int -l_5_I = 0;
        if (qvVar.Ot != null) {
            for (qu -l_7_R : qvVar.Ot) {
                if (z2) {
                    if (z) {
                        if (-l_7_R.Nt == 3) {
                            qu.a(-l_4_R, -l_7_R, z, -l_5_I);
                            -l_5_I = 0;
                        }
                    } else if (-l_7_R.Nt != 1) {
                        qu.a(-l_4_R, -l_7_R, z, -l_5_I);
                        -l_5_I = 0;
                    }
                } else if (!(z || -l_7_R.Nt == 1 || -l_7_R.Nt == 2)) {
                    qu.a(-l_4_R, -l_7_R, z, -l_5_I);
                    -l_5_I = 0;
                }
                -l_5_I = 1;
                qu.a(-l_4_R, -l_7_R, z, -l_5_I);
                -l_5_I = 0;
            }
        }
        return -l_4_R.toString();
    }

    public void A(List<qt> list) {
        this.Oh = list;
    }

    public void B(List<qt> list) {
        this.Og = list;
    }

    public String a(qv -l_3_R, Map<String, ov> map) {
        int -l_4_I = 1;
        for (String -l_6_R : -l_3_R.Oz.keySet()) {
            if (map.containsKey(-l_6_R)) {
                -l_4_I = 0;
                break;
            }
        }
        return a(-l_3_R, -l_4_I, this.Oa);
    }

    public void a(qt qtVar) {
        this.Of = qtVar;
    }

    public Map<String, qv> cX(String str) {
        jD();
        if (this.Od == null || this.Od.size() == 0) {
            return null;
        }
        Object -l_2_R = new HashMap();
        for (Entry -l_4_R : this.Od.entrySet()) {
            qv -l_5_R = (qv) -l_4_R.getValue();
            if (-l_5_R != null && -l_5_R.Oz.containsKey(str)) {
                -l_2_R.put(-l_4_R.getKey(), -l_5_R);
            }
        }
        return -l_2_R;
    }

    public void cY(String str) {
        if (this.Oc == null) {
            this.Oc = new HashSet();
        }
        this.Oc.add(str);
    }

    public qv cZ(String str) {
        return (qv) this.Od.get(str);
    }

    public qt da(String str) {
        for (qt -l_3_R : this.Oe) {
            if (str.equals(-l_3_R.Oj)) {
                return -l_3_R;
            }
        }
        return null;
    }

    public boolean jD() {
        if (this.Od == null) {
            Object -l_1_R = new qs();
            if (-l_1_R.a(this.NZ, this) == 0 || !-l_1_R.T(TMSDKContext.getApplicaionContext())) {
                return false;
            }
            this.Od = -l_1_R.Od;
        }
        return true;
    }

    public String[] jE() {
        if (this.Oc == null) {
            return null;
        }
        return (String[]) this.Oc.toArray(new String[0]);
    }

    public String[] jF() {
        if (this.Od == null) {
            return null;
        }
        return (String[]) this.Od.keySet().toArray(new String[0]);
    }

    public List<qt> jG() {
        if (!this.Oa) {
            return this.Oe;
        }
        Object -l_1_R = new ArrayList();
        for (qt -l_3_R : this.Oe) {
            if (!-l_3_R.Or) {
                -l_1_R.add(-l_3_R);
            }
        }
        return -l_1_R;
    }

    public List<qt> jH() {
        if (!this.Oa) {
            return this.Oh;
        }
        Object -l_1_R = new ArrayList();
        for (qt -l_3_R : this.Oh) {
            if (!-l_3_R.Or) {
                -l_1_R.add(-l_3_R);
            }
        }
        return -l_1_R;
    }

    public List<qt> jI() {
        return this.Og;
    }

    public qt jJ() {
        return this.Of;
    }

    public void z(List<qt> list) {
        this.Oe = list;
    }
}
