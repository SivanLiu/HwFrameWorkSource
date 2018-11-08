package tmsdkobf;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tmsdk.common.TMSDKContext;
import tmsdk.common.tcc.DeepCleanEngine.Callback;
import tmsdk.common.tcc.QFile;
import tmsdk.common.utils.f;
import tmsdk.common.utils.m;
import tmsdk.fg.module.cleanV2.IScanTaskCallBack;
import tmsdk.fg.module.cleanV2.RubbishEntity;
import tmsdk.fg.module.cleanV2.RubbishHolder;

public class ra implements Callback {
    List<String> ME;
    private RubbishHolder Mt;
    private final boolean NZ;
    Map<String, ov> OK;
    Map<String, List<Integer>> OL;
    private qq OM;
    IScanTaskCallBack ON;
    qv OO;
    a OP;
    String OQ;
    private int OR = 0;
    private int OS = 0;
    private final long OT;
    qu OU = null;
    long OV = 0;
    long OW = 0;
    StringBuffer OX = new StringBuffer();
    private final boolean Oa;

    public class a {
        public String NQ;
        public boolean OY;
        final /* synthetic */ ra OZ;
        public String nf;

        public a(ra raVar) {
            this.OZ = raVar;
        }
    }

    public ra(boolean z, boolean z2, IScanTaskCallBack iScanTaskCallBack) {
        this.NZ = z;
        this.Oa = z2;
        this.OT = System.currentTimeMillis();
        this.OK = jY();
        this.ON = iScanTaskCallBack;
        this.OL = new HashMap();
        this.Mt = new RubbishHolder();
        Object -l_5_R = !new qn().jv() ? new qr(this) : !qo.jz().jC() ? new qr(this) : new qp(this);
        this.OM = -l_5_R;
        this.ME = rh.jZ();
    }

    private a a(boolean z, qv qvVar) {
        Object -l_4_R = new a(this);
        if (qvVar == null) {
            return null;
        }
        Object -l_5_R = qvVar.Oz;
        if (-l_5_R == null || -l_5_R.size() < 1) {
            return null;
        }
        -l_4_R.nf = this.OQ;
        ov -l_3_R = (ov) this.OK.get(this.OQ);
        if (-l_3_R != null) {
            -l_4_R.nf = -l_3_R.getPackageName();
            if (-l_3_R.getAppName() == null) {
                Object -l_8_R;
                try {
                    -l_8_R = TMSDKContext.getApplicaionContext().getPackageManager();
                    -l_3_R.setAppName(-l_8_R.getApplicationLabel(-l_8_R.getApplicationInfo(-l_3_R.getPackageName(), 0)).toString());
                } catch (Object -l_8_R2) {
                    -l_8_R2.printStackTrace();
                }
            }
            -l_4_R.NQ = -l_3_R.getAppName();
            -l_4_R.OY = false;
        } else {
            -l_4_R.NQ = (String) -l_5_R.get(this.OQ);
            Object -l_6_R = null;
            for (String -l_8_R3 : -l_5_R.keySet()) {
                ov -l_6_R2 = (ov) this.OK.get(-l_8_R3);
                if (-l_6_R2 != null) {
                    break;
                }
            }
            -l_4_R.OY = -l_6_R == null;
        }
        return -l_4_R;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void a(PackageInfo packageInfo, ov ovVar, int i, PackageManager packageManager) {
        boolean z = true;
        int i2 = -1;
        if (packageInfo != null && ovVar != null && packageInfo.applicationInfo != null) {
            if ((i & 1) != 0) {
                ovVar.put("pkgName", packageInfo.applicationInfo.packageName);
                ovVar.put("isSystem", Boolean.valueOf((packageInfo.applicationInfo.flags & 1) != 0));
                ovVar.put("uid", Integer.valueOf(packageInfo.applicationInfo == null ? -1 : packageInfo.applicationInfo.uid));
            }
            if ((i & 2) != 0) {
                ovVar.put("pkgName", packageInfo.applicationInfo.packageName);
                String str = "isSystem";
                if ((packageInfo.applicationInfo.flags & 1) == 0) {
                    z = false;
                }
                ovVar.put(str, Boolean.valueOf(z));
                String str2 = "uid";
                if (packageInfo.applicationInfo != null) {
                    i2 = packageInfo.applicationInfo.uid;
                }
                ovVar.put(str2, Integer.valueOf(i2));
            }
            if ((i & 8) != 0) {
                ovVar.put("version", packageInfo.versionName);
                ovVar.put("versionCode", Integer.valueOf(packageInfo.versionCode));
            }
            if ((i & 64) != 0) {
                ovVar.put("apkPath", packageInfo.applicationInfo.sourceDir);
                ovVar.put("isApk", Boolean.valueOf(false));
            }
        }
    }

    private a b(boolean z, qv qvVar) {
        Object -l_8_R;
        ov -l_3_R = null;
        Object -l_4_R = new a(this);
        if (qvVar == null) {
            return null;
        }
        Object -l_5_R = qvVar.Oz;
        if (-l_5_R == null || -l_5_R.size() < 1) {
            return null;
        }
        for (String -l_7_R : -l_5_R.keySet()) {
            -l_3_R = (ov) this.OK.get(-l_7_R);
            if (-l_3_R != null) {
                break;
            }
        }
        if (-l_3_R != null) {
            -l_4_R.nf = -l_3_R.getPackageName();
            if (-l_3_R.getAppName() == null) {
                try {
                    -l_8_R = TMSDKContext.getApplicaionContext().getPackageManager();
                    -l_3_R.setAppName(-l_8_R.getApplicationLabel(-l_8_R.getApplicationInfo(-l_3_R.getPackageName(), 0)).toString());
                } catch (Object -l_8_R2) {
                    -l_8_R2.printStackTrace();
                }
            }
            -l_4_R.NQ = -l_3_R.getAppName();
            -l_4_R.OY = false;
        } else {
            long -l_8_J = 0;
            String str = null;
            Object -l_11_R = qo.jz().jB();
            if (-l_11_R.size() > 0) {
                for (String -l_13_R : -l_5_R.keySet()) {
                    if (-l_11_R.get(-l_13_R) != null) {
                        long -l_6_J = ((Long) -l_11_R.get(-l_13_R)).longValue();
                        if ((-l_6_J <= -l_8_J ? 1 : null) == null) {
                            -l_8_J = -l_6_J;
                            str = -l_13_R;
                        }
                    }
                }
                -l_4_R.NQ = (String) -l_5_R.get(str);
            }
            if (str == null) {
                str = (String) -l_5_R.keySet().toArray()[0];
                -l_4_R.NQ = !z ? "疑似" + ((String) -l_5_R.get(str)) : (String) -l_5_R.get(str);
            }
            -l_4_R.nf = str;
            -l_4_R.OY = true;
        }
        return -l_4_R;
    }

    private int bF(int i) {
        int -l_2_I = 0;
        if ((i & 16) != 0) {
            -l_2_I = 64;
        }
        return (i & 32) == 0 ? -l_2_I : -l_2_I | 4096;
    }

    private boolean de(String str) {
        Object<String> -l_2_R = jZ();
        Object -l_3_R = this.OM.jE();
        if (-l_3_R == null) {
            return false;
        }
        for (String -l_6_R : -l_2_R) {
            if (str.toLowerCase().contains(-l_6_R.toLowerCase())) {
                Object -l_4_R = str.toLowerCase().substring(-l_6_R.length());
                Object -l_7_R = -l_3_R;
                for (Object -l_10_R : -l_3_R) {
                    if (-l_10_R.toLowerCase().startsWith(-l_4_R)) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    private ArrayList<ov> x(int i, int i2) {
        Object -l_3_R = null;
        Object -l_4_R = TMSDKContext.getApplicaionContext().getPackageManager();
        try {
            -l_3_R = -l_4_R.getInstalledPackages(bF(i));
        } catch (Object -l_5_R) {
            -l_5_R.printStackTrace();
        }
        Object -l_5_R2 = new ArrayList();
        if (-l_3_R != null) {
            for (PackageInfo -l_7_R : -l_3_R) {
                int -l_8_I = (-l_7_R.applicationInfo.flags & 1) == 0 ? 0 : 1;
                if (-l_8_I != 0 || i2 != 1) {
                    if (-l_8_I != 0) {
                        if (i2 != 0) {
                        }
                    }
                    Object -l_9_R = new ov();
                    a(-l_7_R, -l_9_R, i, -l_4_R);
                    -l_5_R2.add(-l_9_R);
                }
            }
        }
        return -l_5_R2;
    }

    public boolean a(Set<String> set) {
        if (set != null) {
            for (String -l_3_R : set) {
                this.OM.cY(-l_3_R);
            }
        }
        Object -l_2_R = this.OM.jE();
        if (-l_2_R != null) {
            Object -l_3_R2 = -l_2_R;
            for (Object -l_6_R : -l_2_R) {
                f.e("ZhongSi", "setWhitePath: " + -l_6_R);
            }
        }
        return true;
    }

    public void dd(String str) {
        this.OQ = str;
        Object -l_2_R = this.OM.cX(str);
        if (!(this.OM instanceof qr)) {
            if (-l_2_R == null || -l_2_R.size() == 0) {
                this.OM = new qr(this);
            } else {
                Object -l_4_R = new qr(this);
                Object -l_5_R = -l_4_R.cX(str);
                if (-l_5_R != null && -l_5_R.size() > -l_2_R.size()) {
                    this.OM = -l_4_R;
                }
            }
        }
    }

    public String getDetailRule(String str) {
        if (this.OM == null) {
            return null;
        }
        Object -l_2_R = null;
        try {
            this.OO = this.OM.cZ(str.toLowerCase());
            if (this.OO == null) {
                return null;
            }
            if (this.OQ == null) {
                this.OP = b(this.NZ, this.OO);
                if (this.OP == null) {
                    return null;
                }
            }
            this.OP = a(this.NZ, this.OO);
            if (this.OP == null) {
                return null;
            }
            -l_2_R = this.OM.a(this.OO, this.OK);
            return -l_2_R;
        } catch (Object -l_3_R) {
            -l_3_R.printStackTrace();
            this.OO = null;
            this.OP = null;
        }
    }

    public boolean jD() {
        return this.OM == null ? false : this.OM.jD();
    }

    public boolean jR() {
        return this.Oa;
    }

    public boolean jS() {
        return this.NZ;
    }

    protected IScanTaskCallBack jT() {
        return this.ON;
    }

    protected String jU() {
        return this.OQ;
    }

    protected qq jV() {
        return this.OM;
    }

    public void jW() {
        if (this.ON != null) {
            this.ON.onScanCanceled(this.Mt);
        }
    }

    public void jX() {
        if (this.ON != null) {
            this.ON.onScanFinished(this.Mt);
            this.ON = null;
        }
    }

    protected Map<String, ov> jY() {
        Object -l_2_R = x(73, 2);
        Object -l_3_R = new HashMap();
        Object -l_4_R = -l_2_R.iterator();
        while (-l_4_R.hasNext()) {
            ov -l_5_R = (ov) -l_4_R.next();
            -l_3_R.put(-l_5_R.getPackageName(), -l_5_R);
        }
        return -l_3_R;
    }

    public List<String> jZ() {
        if (this.ME == null || this.ME.size() < 1) {
            this.ME = rh.jZ();
        }
        return this.ME;
    }

    public void onDirectoryChange(String str, int i) {
        if (i == 0) {
            this.OR = this.OS;
        }
        this.OS = this.OR + i;
        if (this.ON != null) {
            this.ON.onDirectoryChange(str, this.OS);
        }
    }

    public void onFoundComRubbish(String str, String -l_8_R, long j) {
        qt -l_5_R = this.OM.da(str);
        if (-l_5_R != null) {
            Object -l_6_R;
            Object -l_7_R = new qy();
            if (-l_5_R != this.OM.jJ()) {
                -l_6_R = new RubbishEntity(1, -l_8_R, !-l_5_R.Or, j, null, null, -l_5_R.mDescription);
            } else {
                -l_6_R = -l_7_R.a(this, this.Oa, -l_8_R, j, this.OL, this.OK, this.OM.jI());
                if (-l_6_R == null) {
                    return;
                }
                if (-l_6_R.getPackageName() != null) {
                    if (this.OL.get(-l_6_R.getPackageName()) == null) {
                        -l_8_R = new ArrayList();
                        -l_8_R.add(Integer.valueOf(-l_6_R.getVersionCode()));
                        this.OL.put(-l_6_R.getPackageName(), -l_8_R);
                    } else {
                        ((List) this.OL.get(-l_6_R.getPackageName())).add(Integer.valueOf(-l_6_R.getVersionCode()));
                    }
                }
            }
            if (-l_6_R != null) {
                this.Mt.addRubbish(-l_6_R);
                if (this.ON != null) {
                    this.ON.onRubbishFound(-l_6_R);
                }
            }
        }
    }

    public void onFoundEmptyDir(String -l_5_R, long j) {
        if (!de(-l_5_R)) {
            Object -l_6_R = new RubbishEntity(1, -l_5_R, true, j, null, null, m.cF("scan_item_empty_folders"));
            if (-l_6_R != null) {
                this.Mt.addRubbish(-l_6_R);
                if (this.ON != null) {
                    this.ON.onRubbishFound(-l_6_R);
                }
            }
        }
    }

    public void onFoundKeySoftRubbish(String str, String[] strArr, long j) {
        if (this.OO != null && this.OP != null && strArr != null) {
            try {
                int -l_5_I = Integer.parseInt(str);
                long -l_6_J = j / 1000;
                List -l_10_R = Arrays.asList(strArr);
                Object -l_9_R = this.OU != null ? this.OU.mID != -l_5_I ? this.OO.bY(-l_5_I) : this.OU : this.OO.bY(-l_5_I);
                if (-l_9_R != null) {
                    Object -l_8_R;
                    this.OU = -l_9_R;
                    if (this.OP.OY) {
                        -l_8_R = new RubbishEntity(4, -l_10_R, (-l_9_R.Nt != 3 ? 0 : 1) == 0, -l_6_J, this.OP.NQ, this.OP.nf, -l_9_R.mDescription);
                    } else {
                        int -l_11_I;
                        if (-l_9_R.Nt == 1) {
                            -l_11_I = 0;
                        } else if (-l_9_R.Nt == 2) {
                            -l_11_I = 1;
                        } else {
                            return;
                        }
                        -l_8_R = new RubbishEntity(0, -l_10_R, -l_11_I == 0, -l_6_J, this.OP.NQ, this.OP.nf, -l_9_R.mDescription);
                    }
                    if (-l_8_R != null) {
                        -l_8_R.setExtendData(-l_9_R.Nu, -l_9_R.Ne, -l_9_R.Nw);
                        if (this.ON != null) {
                            this.ON.onRubbishFound(-l_8_R);
                        }
                        this.Mt.addRubbish(-l_8_R);
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public void onFoundSoftRubbish(String str, String -l_11_R, String str2, long j) {
        if (this.OO != null && this.OP != null) {
            try {
                int -l_6_I = Integer.parseInt(str);
                long -l_7_J = j / 1000;
                if (str2 != null) {
                    -l_11_R = -l_11_R + str2;
                }
                Object -l_10_R = this.OU != null ? this.OU.mID != -l_6_I ? this.OO.bY(-l_6_I) : this.OU : this.OO.bY(-l_6_I);
                if (-l_10_R != null) {
                    Object -l_9_R;
                    this.OU = -l_10_R;
                    if (this.OP.OY) {
                        -l_9_R = new RubbishEntity(4, -l_11_R, (-l_10_R.Nt != 3 ? 0 : 1) == 0, -l_7_J, this.OP.NQ, this.OP.nf, -l_10_R.mDescription);
                    } else {
                        int -l_12_I;
                        if (-l_10_R.Nt == 1) {
                            -l_12_I = 0;
                        } else if (-l_10_R.Nt == 2) {
                            -l_12_I = 1;
                        } else {
                            return;
                        }
                        -l_9_R = new RubbishEntity(0, -l_11_R, -l_12_I == 0, -l_7_J, this.OP.NQ, this.OP.nf, -l_10_R.mDescription);
                    }
                    if (-l_9_R != null) {
                        -l_9_R.setExtendData(-l_10_R.Nu, -l_10_R.Ne, -l_10_R.Nw);
                        this.Mt.addRubbish(-l_9_R);
                        if (this.ON != null) {
                            this.ON.onRubbishFound(-l_9_R);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public void onProcessChange(int i) {
    }

    public void onScanError(int i) {
        if (this.ON != null) {
            this.ON.onScanError(i, this.Mt);
        }
    }

    public void onScanStarted() {
        if (this.ON != null) {
            this.ON.onScanStarted();
        }
    }

    public void onVisit(QFile qFile) {
    }

    public void release() {
        this.ON = null;
        this.OM = null;
        this.Mt = null;
        this.OK = null;
        this.OL = null;
        this.OO = null;
        this.OP = null;
        this.OQ = null;
        this.OU = null;
        this.ME = null;
    }
}
