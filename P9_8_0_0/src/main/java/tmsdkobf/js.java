package tmsdkobf;

import com.qq.taf.jce.JceStruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;
import tmsdk.common.CallerIdent;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.utils.f;
import tmsdkobf.ji.a;
import tmsdkobf.ji.b;
import tmsdkobf.ji.c;

public class js implements ji, pg {
    private static js tu = null;
    b tm;
    kh tv;

    private js() {
        this.tm = null;
        this.tv = null;
        this.tv = (kh) fj.D(12);
    }

    private static av a(ov ovVar, String str) {
        Object -l_2_R = new av();
        if (ovVar != null) {
            -l_2_R.ca = ovVar.getSize();
            -l_2_R.bZ = ovVar.hz();
            -l_2_R.cb = ovVar.hx();
            -l_2_R.packageName = ovVar.getPackageName();
            -l_2_R.softName = ovVar.getAppName();
            -l_2_R.version = ovVar.getVersion();
            -l_2_R.cd = (long) ovVar.getVersionCode();
            -l_2_R.ce = ovVar.hy() / 1000;
            Object -l_3_R = ovVar.hB();
            -l_2_R.aS = "";
            -l_2_R.cc = aU(-l_3_R);
            return -l_2_R;
        }
        -l_2_R.packageName = str;
        return -l_2_R;
    }

    private void a(List<a> list, Collection<JceStruct> collection, int i) {
        for (JceStruct -l_5_R : collection) {
            if (-l_5_R != null) {
                Object -l_6_R = new a();
                -l_6_R.action = i;
                -l_6_R.ta = -l_5_R;
                list.add(-l_6_R);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean a(av avVar, av avVar2) {
        if (avVar == null && avVar2 == null) {
            return true;
        }
        return avVar != null && avVar2 != null && avVar.ca == avVar2.ca && avVar.cd == avVar2.cd && k(avVar.packageName, avVar2.packageName) && k(avVar.softName, avVar2.softName) && k(avVar.bZ, avVar2.bZ) && k(avVar.aS, avVar2.aS) && k(avVar.version, avVar2.version) && k(avVar.cc, avVar2.cc) && avVar.cb == avVar2.cb;
    }

    private av aT(String str) {
        return a(this.tv.a(str, 89), str);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String aU(String str) {
        ZipFile -l_2_R;
        Object -l_9_R;
        if (str == null) {
            return "";
        }
        BufferedReader bufferedReader = null;
        ZipFile zipFile = null;
        try {
            -l_2_R = new ZipFile(str);
            try {
                Object -l_5_R;
                if (new File(str).exists()) {
                    Object -l_3_R = -l_2_R.getEntry("META-INF/MANIFEST.MF");
                    if (-l_3_R != null) {
                        BufferedReader -l_1_R = new BufferedReader(new InputStreamReader(-l_2_R.getInputStream(-l_3_R)));
                        while (true) {
                            try {
                                -l_5_R = -l_1_R.readLine();
                                if (-l_5_R == null) {
                                    break;
                                } else if (-l_5_R.contains("classes.dex")) {
                                    -l_5_R = -l_1_R.readLine();
                                    if (-l_5_R != null && -l_5_R.contains("SHA1-Digest")) {
                                        int -l_6_I = -l_5_R.indexOf(":");
                                        if (-l_6_I > 0) {
                                            break;
                                        }
                                    }
                                }
                            } catch (Throwable th) {
                                -l_9_R = th;
                                zipFile = -l_2_R;
                                bufferedReader = -l_1_R;
                            }
                        }
                        bufferedReader = -l_1_R;
                    }
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Object -l_4_R) {
                            f.e("SoftListProfileService", -l_4_R);
                        }
                    }
                    if (-l_2_R != null) {
                        try {
                            -l_2_R.close();
                        } catch (Object -l_4_R2) {
                            f.e("SoftListProfileService", -l_4_R2);
                        }
                    }
                    zipFile = -l_2_R;
                    return "";
                }
                -l_5_R = "";
                if (null != null) {
                    try {
                        bufferedReader.close();
                    } catch (Object -l_6_R) {
                        f.e("SoftListProfileService", -l_6_R);
                    }
                }
                if (-l_2_R != null) {
                    try {
                        -l_2_R.close();
                    } catch (Object -l_6_R2) {
                        f.e("SoftListProfileService", -l_6_R2);
                    }
                }
                return -l_5_R;
            } catch (Throwable th2) {
                -l_9_R = th2;
                zipFile = -l_2_R;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Object -l_10_R) {
                        f.e("SoftListProfileService", -l_10_R);
                    }
                }
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (Object -l_10_R2) {
                        f.e("SoftListProfileService", -l_10_R2);
                    }
                }
                throw -l_9_R;
            }
        } catch (Throwable th3) {
            -l_9_R = th3;
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (zipFile != null) {
                zipFile.close();
            }
            throw -l_9_R;
        }
        return -l_7_R;
        if (-l_2_R != null) {
            try {
                -l_2_R.close();
            } catch (Object -l_8_R) {
                f.e("SoftListProfileService", -l_8_R);
            }
        }
        return -l_7_R;
    }

    private void b(String str, int i) {
        if (this.tm != null) {
            Object -l_3_R = aT(str);
            if ((-l_3_R.ce > 0 ? 1 : null) == null) {
                -l_3_R.ce = System.currentTimeMillis() / 1000;
            }
            Object -l_4_R = new ArrayList();
            Object -l_5_R = new a();
            -l_5_R.action = i;
            -l_5_R.ta = -l_3_R;
            -l_4_R.add(-l_5_R);
            this.tm.j(-l_4_R);
        }
    }

    public static js cE() {
        if (tu == null) {
            Object -l_0_R = js.class;
            synchronized (js.class) {
                if (tu == null) {
                    tu = new js();
                }
            }
        }
        return tu;
    }

    private static final boolean k(String str, String str2) {
        return str != str2 ? str != null ? str.equals(str2) : false : true;
    }

    public void a(gt gtVar) {
        int -l_2_I = jt.cH().cz();
        if (-l_2_I <= 0) {
            -l_2_I = 500000;
        }
        f.d("SoftListProfileService", "softListFullUploadQuantity : " + -l_2_I);
        gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 2, new c(this) {
            final /* synthetic */ js tw;

            {
                this.tw = r1;
            }

            public ArrayList<JceStruct> cu() {
                return this.tw.cG();
            }
        }, gtVar, -l_2_I);
    }

    public void a(b bVar) {
        this.tv.b(this);
        this.tv.a(this);
        this.tm = bVar;
    }

    public void aQ(String str) {
        f.d("SoftListProfileService", "onPackageAdded : " + str);
        b(str, 1);
    }

    public void aR(String str) {
        f.d("SoftListProfileService", "onPackageReinstall : " + str);
        b(str, 3);
    }

    public void aS(String str) {
        f.d("SoftListProfileService", "onPackageRemoved : " + str);
        b(str, 2);
    }

    public void ag(int i) {
        jt.cH().ag(i);
    }

    public void cF() {
        jt.cH().k(true);
    }

    public ArrayList<JceStruct> cG() {
        Object -l_1_R = new ArrayList();
        Object -l_3_R = TMServiceFactory.getSystemInfoService().f(89, 2);
        if (-l_3_R == null || -l_3_R.size() <= 0) {
            return -l_1_R;
        }
        Object -l_4_R = -l_3_R.iterator();
        while (-l_4_R.hasNext()) {
            ov -l_5_R = (ov) -l_4_R.next();
            if (-l_5_R != null) {
                -l_1_R.add(a(-l_5_R, -l_5_R.getPackageName()));
            }
        }
        return -l_1_R;
    }

    public int cs() {
        return 2;
    }

    public void ct() {
        Object -l_2_R = ((kh) fj.D(12)).f(89, 2);
        Object -l_3_R = new LinkedList();
        Object -l_4_R = -l_2_R.iterator();
        while (-l_4_R.hasNext()) {
            ov -l_5_R = (ov) -l_4_R.next();
            if (-l_5_R != null) {
                Object -l_6_R = a(-l_5_R, -l_5_R.getPackageName());
                if (!(-l_6_R == null || -l_6_R.packageName == null)) {
                    if (!"".equals(-l_6_R.packageName)) {
                        -l_3_R.add(-l_6_R);
                    }
                }
            }
        }
    }

    public boolean h(ArrayList<a> arrayList) {
        int -l_2_I = jr.cB().l(arrayList);
        f.f("SoftListProfileService", "SoftListProfile UpdateImage : " + -l_2_I);
        return -l_2_I;
    }

    public void i(ArrayList<a> arrayList) {
        if (arrayList != null && arrayList.size() != 0) {
            ArrayList -l_2_R = null;
            ArrayList -l_3_R = null;
            ArrayList -l_4_R = null;
            ArrayList -l_5_R = null;
            Object -l_6_R = arrayList.iterator();
            while (-l_6_R.hasNext()) {
                a -l_7_R = (a) -l_6_R.next();
                if (!(-l_7_R == null || -l_7_R.ta == null)) {
                    switch (-l_7_R.action) {
                        case 0:
                            if (-l_5_R == null) {
                                -l_5_R = new ArrayList();
                            }
                            -l_5_R.add(-l_7_R.ta);
                            break;
                        case 1:
                            if (-l_4_R == null) {
                                -l_4_R = new ArrayList();
                            }
                            -l_4_R.add(-l_7_R.ta);
                            break;
                        case 2:
                            if (-l_3_R == null) {
                                -l_3_R = new ArrayList();
                            }
                            -l_3_R.add(-l_7_R.ta);
                            break;
                        case 3:
                            if (-l_2_R == null) {
                                -l_2_R = new ArrayList();
                            }
                            -l_2_R.add(-l_7_R.ta);
                            break;
                        default:
                            break;
                    }
                }
            }
            if (-l_5_R != null && -l_5_R.size() > 0) {
                gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 2, 0, -l_5_R);
            }
            if (-l_2_R != null && -l_2_R.size() > 0) {
                gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 2, 3, -l_2_R);
            }
            if (-l_3_R != null && -l_3_R.size() > 0) {
                gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 2, 2, -l_3_R);
            }
            if (-l_4_R != null && -l_4_R.size() > 0) {
                gs.bc().a(CallerIdent.getIdent(1, 4294967296L), 2, 1, -l_4_R);
            }
        }
    }

    public ArrayList<a> m(ArrayList<JceStruct> arrayList) {
        Object -l_2_R = new ArrayList();
        if (arrayList == null || arrayList.size() == 0) {
            return -l_2_R;
        }
        Object -l_3_R = jr.cB().cC();
        if (-l_3_R == null || -l_3_R.size() == 0 || jt.cH().cI()) {
            f.f("SoftListProfileService", "fullCheck|AllReport size:" + arrayList.size());
            a(-l_2_R, arrayList, 0);
            if (jt.cH().cI()) {
                jt.cH().k(false);
            }
            return -l_2_R;
        }
        f.f("SoftListProfileService", "not !!! fullCheck|AllReport size:" + arrayList.size());
        Object -l_4_R = new LinkedList();
        Object -l_5_R = new LinkedList();
        Object -l_6_R = -l_3_R.iterator();
        while (-l_6_R.hasNext()) {
            av -l_7_R = (av) -l_6_R.next();
            if (!(-l_7_R == null || -l_7_R.packageName == null || -l_7_R.packageName.trim().equals(""))) {
                Object -l_8_R = null;
                Object -l_9_R = arrayList.iterator();
                while (-l_9_R.hasNext()) {
                    av -l_11_R = (av) ((JceStruct) -l_9_R.next());
                    if (-l_11_R.packageName.equals(-l_7_R.packageName)) {
                        av -l_8_R2 = -l_11_R;
                        if (!a(-l_11_R, -l_7_R)) {
                            -l_4_R.add(-l_11_R);
                        }
                        if (-l_8_R == null) {
                            arrayList.remove(-l_8_R);
                        } else {
                            -l_7_R.ce = 0;
                            -l_5_R.add(-l_7_R);
                        }
                    }
                }
                if (-l_8_R == null) {
                    -l_7_R.ce = 0;
                    -l_5_R.add(-l_7_R);
                } else {
                    arrayList.remove(-l_8_R);
                }
            }
        }
        a(-l_2_R, -l_4_R, 3);
        a(-l_2_R, -l_5_R, 2);
        a(-l_2_R, arrayList, 1);
        return -l_2_R;
    }
}
