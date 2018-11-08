package tmsdkobf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import tmsdk.fg.module.cleanV2.RubbishEntity;
import tmsdk.fg.module.cleanV2.RubbishHolder;
import tmsdkobf.qi.b;

public class ql implements Comparable<ql> {
    private List<ql> NA;
    private ql NB;
    private ql NC;
    private Set<qj> ND;
    private int NE;
    private int[] NF;
    private String NG;
    private boolean[] NH;
    private ExecutorService NI;
    private qj NJ;
    private tmsdkobf.qi.a NK;
    private List<File> Nn;
    private String Ny;
    private List<qj> Nz;
    private String mAppName;
    private int mLevel;
    private String mPackageName;

    private class a implements Runnable {
        private AtomicBoolean MK;
        private File NL;
        final /* synthetic */ ql NM;

        a(ql qlVar, File file, AtomicBoolean atomicBoolean) {
            this.NM = qlVar;
            this.NL = file;
            this.MK = atomicBoolean;
        }

        public void run() {
            this.NM.a(this.NL, this.MK);
        }
    }

    public ql(String str, String str2, String str3, boolean z) {
        this.mLevel = 0;
        this.Nz = new LinkedList();
        this.NA = new LinkedList();
        this.NC = null;
        this.Nn = new LinkedList();
        this.ND = null;
        this.NE = 0;
        this.NF = null;
        this.mAppName = null;
        this.mPackageName = null;
        this.NH = null;
        this.NI = null;
        this.NJ = null;
        this.Ny = "/";
        this.ND = new LinkedHashSet();
        this.NF = new int[]{1};
        this.NG = str3;
        this.mAppName = str2;
        this.mPackageName = str;
        this.NH = new boolean[]{z};
    }

    private ql(String str, ql qlVar) {
        this.mLevel = 0;
        this.Nz = new LinkedList();
        this.NA = new LinkedList();
        this.NC = null;
        this.Nn = new LinkedList();
        this.ND = null;
        this.NE = 0;
        this.NF = null;
        this.mAppName = null;
        this.mPackageName = null;
        this.NH = null;
        this.NI = null;
        this.NJ = null;
        this.Ny = str;
        this.mLevel = qlVar.mLevel + 1;
        this.NC = qlVar;
        this.ND = qlVar.ND;
        this.NG = qlVar.NG;
        this.NF = qlVar.NF;
        int[] iArr = this.NF;
        iArr[0] = iArr[0] + 1;
        this.NI = qlVar.NI;
        this.mAppName = qlVar.mAppName;
        this.mPackageName = qlVar.mPackageName;
        this.NH = qlVar.NH;
    }

    private static boolean D(String str, String str2) {
        int -l_2_I = str.charAt(0);
        if (-l_2_I != 42) {
            return -l_2_I != 47 ? str.equalsIgnoreCase(str2) : qk.cT(str.substring(1)).matcher(str2).find();
        } else {
            return true;
        }
    }

    static List<RubbishEntity> a(qj qjVar, boolean z, String str, String str2) {
        Object -l_4_R = new LinkedList();
        int -l_5_I = !z ? 4 : 0;
        boolean -l_6_I = !qjVar.V(z);
        String -l_7_R = qjVar.Nv;
        List -l_8_R = null;
        if (-l_7_R != null) {
            -l_8_R = rg.dh(-l_7_R);
        }
        Object -l_9_R = new RubbishEntity(-l_5_I, qjVar.Nn, -l_6_I, qjVar.No, str2, str, qjVar.mDescription);
        -l_9_R.setExtendData(qjVar.Nu, qjVar.Ne, -l_8_R);
        -l_4_R.add(-l_9_R);
        if (qjVar.Nl == 3 && qjVar.Nq.size() > 0) {
            Object -l_10_R = new RubbishEntity(-l_5_I, qjVar.Nq, true, qjVar.Nr, str2, str, qjVar.Np);
            -l_10_R.setExtendData(qjVar.Nu, qjVar.Ne, -l_8_R);
            -l_4_R.add(-l_10_R);
        }
        return -l_4_R;
    }

    private void a(File file, AtomicBoolean atomicBoolean) {
        if (!atomicBoolean.get()) {
            if (file.isDirectory()) {
                Object -l_3_R = file.listFiles();
                if (-l_3_R != null) {
                    Object -l_4_R = -l_3_R;
                    for (File -l_7_R : -l_3_R) {
                        a(-l_7_R, atomicBoolean);
                    }
                }
            } else {
                i(file);
            }
        }
    }

    private void a(List<String> list, qj qjVar) {
        int -l_3_I = this.mLevel;
        if (-l_3_I < list.size()) {
            String -l_4_R = (String) list.get(-l_3_I);
            if ("*".equalsIgnoreCase(-l_4_R)) {
                if (this.NB == null) {
                    this.NB = new ql(-l_4_R, this);
                }
                this.NB.a((List) list, qjVar);
                return;
            }
            for (ql -l_6_R : this.NA) {
                if (-l_4_R.equalsIgnoreCase(-l_6_R.Ny)) {
                    -l_6_R.a((List) list, qjVar);
                    return;
                }
            }
            Object -l_5_R = new ql(-l_4_R, this);
            this.NA.add(-l_5_R);
            -l_5_R.a((List) list, qjVar);
            return;
        }
        this.Nz.add(qjVar);
    }

    private static List<String> cU(String str) {
        Object -l_1_R = new ArrayList();
        int -l_2_I = 1;
        while (true) {
            int -l_3_I = str.indexOf("/", -l_2_I + 1);
            if (-1 != -l_3_I) {
                -l_1_R.add(str.substring(-l_2_I, -l_3_I));
                -l_2_I = -l_3_I + 1;
            } else {
                -l_1_R.add(str.substring(-l_2_I));
                return -l_1_R;
            }
        }
    }

    private void i(File file) {
        for (qj -l_3_R : this.Nz) {
            if (-l_3_R.h(file)) {
                if (this.NH[0]) {
                    if (3 == -l_3_R.Nt || 4 == -l_3_R.Nt) {
                        return;
                    }
                }
                this.ND.add(-l_3_R);
                this.NK.a(file, -l_3_R);
                this.NJ = -l_3_R;
                return;
            }
        }
        if (this.NC != null) {
            this.NC.i(file);
        }
    }

    public int a(ql qlVar) {
        return this.NG.compareTo(qlVar.NG);
    }

    public void a(File file, ExecutorService executorService) {
        this.Nn.add(file);
        this.NI = executorService;
    }

    public void a(String str, b bVar, tmsdkobf.qi.a aVar, AtomicBoolean atomicBoolean) {
        this.NK = aVar;
        Object<ql> -l_5_R = new ArrayList(this.NA);
        if (!(this.NC == null || this.NC.NB == null)) {
            -l_5_R.addAll(this.NC.NB.NA);
        }
        for (File -l_7_R : this.Nn) {
            Object -l_8_R = -l_7_R.listFiles();
            if (-l_8_R != null && !atomicBoolean.get()) {
                Object -l_9_R = -l_8_R;
                for (Object -l_12_R : -l_8_R) {
                    if (-l_12_R.isDirectory()) {
                        Object -l_13_R = -l_12_R.getName();
                        if (!atomicBoolean.get()) {
                            aVar.a(-l_12_R, null);
                            int -l_14_I = 0;
                            for (ql -l_16_R : -l_5_R) {
                                if (D(-l_16_R.Ny, -l_13_R)) {
                                    -l_16_R.Nn.add(-l_12_R);
                                    -l_14_I = 1;
                                    break;
                                }
                            }
                            if (-l_14_I == 0) {
                                if (this.NB != null) {
                                    this.NB.Nn.add(-l_12_R);
                                } else {
                                    this.NI.execute(new a(this, -l_12_R, atomicBoolean));
                                }
                            }
                        } else {
                            return;
                        }
                    }
                    i(-l_12_R);
                }
            } else {
                return;
            }
        }
        for (ql -l_7_R2 : this.NA) {
            -l_7_R2.a(str + "\t", bVar, aVar, atomicBoolean);
        }
        if (this.NB != null) {
            this.NB.a(str + "\t", bVar, aVar, atomicBoolean);
        }
    }

    public void a(RubbishHolder rubbishHolder) {
        for (qj -l_3_R : this.ND) {
            Object -l_4_R = a(-l_3_R, this.NH[0], this.mPackageName, this.mAppName);
            rubbishHolder.addRubbish((RubbishEntity) -l_4_R.get(0));
            if (-l_4_R.size() > 1) {
                rubbishHolder.addRubbish((RubbishEntity) -l_4_R.get(1));
            }
        }
    }

    public void b(qj qjVar) {
        Object -l_2_R = qjVar.Nd;
        this.NE++;
        if ("/".equalsIgnoreCase(-l_2_R)) {
            this.Nz.add(qjVar);
        } else {
            a(cU(-l_2_R), qjVar);
        }
    }

    public /* synthetic */ int compareTo(Object obj) {
        return a((ql) obj);
    }

    public boolean jr() {
        return this.NH[0];
    }

    public boolean js() {
        for (qj -l_2_R : this.Nz) {
            if (-l_2_R.Nf == 0) {
                return true;
            }
        }
        return false;
    }

    public void jt() {
        Collections.sort(this.Nz);
        for (ql -l_2_R : this.NA) {
            -l_2_R.jt();
        }
    }
}
