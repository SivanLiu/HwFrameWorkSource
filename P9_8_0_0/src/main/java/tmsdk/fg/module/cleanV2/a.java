package tmsdk.fg.module.cleanV2;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build.VERSION;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.lang.MultiLangManager;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.tcc.QFile;
import tmsdk.common.utils.f;
import tmsdk.common.utils.m;
import tmsdk.common.utils.s;
import tmsdk.fg.creator.BaseManagerF;
import tmsdkobf.al;
import tmsdkobf.ic;
import tmsdkobf.im;
import tmsdkobf.jk;
import tmsdkobf.js;
import tmsdkobf.mk;
import tmsdkobf.qi;
import tmsdkobf.qn;
import tmsdkobf.qo;
import tmsdkobf.qw;
import tmsdkobf.qz;
import tmsdkobf.ra;
import tmsdkobf.rc;
import tmsdkobf.rd;
import tmsdkobf.re;
import tmsdkobf.rf;

class a extends BaseManagerF implements qz {
    rc Mk;
    rc Ml;
    rd Mm;
    HashMap<Integer, AppGroupDesc> Mn;
    private Object Mo = new Object();
    private Object Mp = new Object();
    private Object Mq = new Object();
    a Mr;
    qi Ms = null;

    class a {
        RubbishHolder Mt;
        ICleanTaskCallBack Mu;
        boolean Mv;
        int Mw;
        final boolean Mx;
        final /* synthetic */ a My;

        protected a(a aVar, RubbishHolder rubbishHolder, ICleanTaskCallBack iCleanTaskCallBack, boolean z) {
            this.My = aVar;
            this.Mt = rubbishHolder;
            this.Mu = iCleanTaskCallBack;
            this.Mx = z;
        }

        private int a(ContentResolver contentResolver, File file) {
            int -l_3_I = 0;
            if (file.getName().startsWith(".")) {
                if (file.isDirectory()) {
                    new QFile(file.getPath()).deleteAllChildren();
                }
                file.delete();
                return 1;
            }
            if (file.isDirectory()) {
                for (File a : file.listFiles()) {
                    -l_3_I += a(contentResolver, a);
                }
                if (file.delete()) {
                    -l_3_I++;
                }
            } else {
                -l_3_I = b(contentResolver, file) + 0;
            }
            return -l_3_I;
        }

        private void a(File file, int i) {
            if (file != null) {
                int -l_3_I = i + 1;
                Object -l_4_R = file.listFiles();
                if (-l_4_R != null) {
                    int -l_5_I = -l_4_R.length;
                    for (int -l_6_I = 0; -l_6_I < -l_5_I; -l_6_I++) {
                        if (-l_4_R[-l_6_I].isDirectory()) {
                            a(-l_4_R[-l_6_I], -l_3_I);
                        }
                        -l_4_R[-l_6_I].delete();
                    }
                    file.delete();
                    return;
                }
                file.delete();
            }
        }

        private void a(RubbishEntity -l_4_R, int i, boolean z) {
            try {
                Object -l_5_R = -l_4_R.getRubbishKey();
                if (-l_5_R != null) {
                    int -l_6_I = -l_5_R.size();
                    if (-l_6_I > 0) {
                        String -l_7_R = (String) -l_5_R.get(0);
                        if (z) {
                            x(-l_5_R);
                        } else {
                            w(-l_5_R);
                        }
                        this.Mw += -l_6_I;
                        this.Mu.onCleanProcessChange((this.Mw * 100) / i, -l_7_R);
                        -l_4_R.ju();
                    }
                }
            } catch (Exception e) {
            }
        }

        private int b(ContentResolver contentResolver, File file) {
            if (file == null) {
                return 0;
            }
            try {
                return (new rf(contentResolver, file).delete() || file.delete()) ? 1 : 0;
            } catch (Object -l_3_R) {
                -l_3_R.printStackTrace();
            }
        }

        private void b(RubbishEntity -l_4_R, int i, boolean z) {
            try {
                Object<String> -l_6_R = -l_4_R.getRubbishKey();
                if (-l_6_R != null && -l_6_R.size() > 0) {
                    Object -l_5_R;
                    String -l_7_R = (String) -l_6_R.get(0);
                    Object -l_5_R2 = null;
                    for (String -l_9_R : -l_6_R) {
                        try {
                            if (-l_9_R != null) {
                                -l_5_R = new File(-l_9_R);
                                if (z) {
                                    -l_5_R.delete();
                                } else if (VERSION.SDK_INT < 11) {
                                    -l_5_R.delete();
                                } else {
                                    b(TMSDKContext.getApplicaionContext().getContentResolver(), -l_5_R);
                                }
                                this.Mw++;
                                -l_5_R2 = -l_5_R;
                            }
                        } catch (Exception e) {
                            -l_5_R = -l_5_R2;
                        }
                    }
                    this.Mu.onCleanProcessChange((this.Mw * 100) / i, -l_7_R);
                    -l_4_R.ju();
                    -l_5_R = -l_5_R2;
                }
            } catch (Exception e2) {
            }
        }

        private void w(List<String> list) {
            ContentResolver -l_2_R = null;
            if (VERSION.SDK_INT < 11) {
                x(list);
                return;
            }
            if (null == null) {
                -l_2_R = TMSDKContext.getApplicaionContext().getContentResolver();
            }
            int -l_3_I = list.size();
            for (int -l_5_I = 0; -l_5_I < -l_3_I; -l_5_I++) {
                String -l_4_R = (String) list.get(-l_5_I);
                if (-l_4_R != null) {
                    a(-l_2_R, new File(-l_4_R.trim()));
                }
            }
        }

        private void x(List<String> list) {
            for (String -l_3_R : list) {
                if (-l_3_R != null) {
                    File -l_4_R = new File(-l_3_R);
                    if (-l_4_R.isDirectory()) {
                        a(-l_4_R, 0);
                    } else {
                        -l_4_R.delete();
                    }
                }
            }
        }

        protected void jm() {
            this.Mv = true;
        }

        protected boolean jn() {
            if (this.Mu == null) {
                return false;
            }
            im.bJ().addTask(new Runnable(this) {
                final /* synthetic */ a Mz;

                {
                    this.Mz = r1;
                }

                private int jo() {
                    int -l_1_I = 0;
                    if (this.Mz.Mt.getmApkRubbishes() != null) {
                        for (RubbishEntity -l_3_R : this.Mz.Mt.getmApkRubbishes()) {
                            if (-l_3_R.getStatus() == 1) {
                                -l_1_I += -l_3_R.getRubbishKey().size();
                            }
                        }
                    }
                    if (this.Mz.Mt.getmSystemRubbishes() != null) {
                        for (Entry -l_3_R2 : this.Mz.Mt.getmSystemRubbishes().entrySet()) {
                            if (((RubbishEntity) -l_3_R2.getValue()).getStatus() == 1) {
                                -l_1_I += ((RubbishEntity) -l_3_R2.getValue()).getRubbishKey().size();
                            }
                        }
                    }
                    if (this.Mz.Mt.getmInstallRubbishes() != null) {
                        for (Entry -l_3_R22 : this.Mz.Mt.getmInstallRubbishes().entrySet()) {
                            if (((RubbishEntity) -l_3_R22.getValue()).getStatus() == 1) {
                                -l_1_I += ((RubbishEntity) -l_3_R22.getValue()).getRubbishKey().size();
                            }
                        }
                    }
                    if (this.Mz.Mt.getmUnInstallRubbishes() != null) {
                        for (Entry -l_3_R222 : this.Mz.Mt.getmUnInstallRubbishes().entrySet()) {
                            if (((RubbishEntity) -l_3_R222.getValue()).getStatus() == 1) {
                                -l_1_I += ((RubbishEntity) -l_3_R222.getValue()).getRubbishKey().size();
                            }
                        }
                    }
                    return -l_1_I;
                }

                public void run() {
                    this.Mz.Mw = 0;
                    this.Mz.Mu.onCleanStarted();
                    int -l_1_I = jo();
                    if (-l_1_I > 0) {
                        if (this.Mz.Mt.getmApkRubbishes() != null) {
                            for (RubbishEntity -l_3_R : this.Mz.Mt.getmApkRubbishes()) {
                                if (this.Mz.Mv) {
                                    this.Mz.Mu.onCleanCanceled();
                                    this.Mz.release();
                                    return;
                                } else if (-l_3_R.getStatus() == 1) {
                                    this.Mz.b(-l_3_R, -l_1_I, this.Mz.Mx);
                                }
                            }
                        }
                        if (this.Mz.Mt.getmSystemRubbishes() != null) {
                            for (Entry -l_3_R2 : this.Mz.Mt.getmSystemRubbishes().entrySet()) {
                                if (this.Mz.Mv) {
                                    this.Mz.Mu.onCleanCanceled();
                                    this.Mz.release();
                                    return;
                                } else if (((RubbishEntity) -l_3_R2.getValue()).getStatus() == 1) {
                                    this.Mz.a((RubbishEntity) -l_3_R2.getValue(), -l_1_I, this.Mz.Mx);
                                }
                            }
                        }
                        if (this.Mz.Mt.getmInstallRubbishes() != null) {
                            for (Entry -l_3_R22 : this.Mz.Mt.getmInstallRubbishes().entrySet()) {
                                if (this.Mz.Mv) {
                                    this.Mz.Mu.onCleanCanceled();
                                    this.Mz.release();
                                    return;
                                } else if (((RubbishEntity) -l_3_R22.getValue()).getStatus() == 1) {
                                    this.Mz.a((RubbishEntity) -l_3_R22.getValue(), -l_1_I, this.Mz.Mx);
                                }
                            }
                        }
                        if (this.Mz.Mt.getmUnInstallRubbishes() != null) {
                            for (Entry -l_3_R222 : this.Mz.Mt.getmUnInstallRubbishes().entrySet()) {
                                if (this.Mz.Mv) {
                                    this.Mz.Mu.onCleanCanceled();
                                    this.Mz.release();
                                    return;
                                } else if (((RubbishEntity) -l_3_R222.getValue()).getStatus() == 1) {
                                    this.Mz.a((RubbishEntity) -l_3_R222.getValue(), -l_1_I, this.Mz.Mx);
                                }
                            }
                        }
                    }
                    this.Mz.Mu.onCleanFinished();
                    this.Mz.release();
                }
            }, null);
            return true;
        }

        protected void release() {
            this.Mt = null;
            this.Mu = null;
            this.My.Mr = null;
        }
    }

    protected a() {
    }

    private boolean jj() {
        boolean z = false;
        al -l_1_R = (al) mk.a(TMSDKContext.getApplicaionContext(), UpdateConfig.DEEPCLEAN_SDCARD_SCAN_RULE_NAME_V2_SDK, UpdateConfig.intToString(40415), new al(), "UTF-8");
        if (-l_1_R == null || -l_1_R.bt == null) {
            return false;
        }
        if (qo.jz().y(-l_1_R.bt).size() > 10) {
            z = true;
        }
        return z;
    }

    private void jk() {
        Object -l_1_R = new qn();
        if (!-l_1_R.jv()) {
            -l_1_R.W(jj());
        }
    }

    protected boolean SlowCleanRubbish(RubbishHolder rubbishHolder, ICleanTaskCallBack iCleanTaskCallBack) {
        if (ic.bE() || this.Mr != null || rubbishHolder == null) {
            return false;
        }
        this.Mr = new a(this, rubbishHolder, iCleanTaskCallBack, false);
        return this.Mr.jn();
    }

    public void a(IScanTaskCallBack iScanTaskCallBack, String str) {
        jk();
        if (iScanTaskCallBack == null) {
            f.e("ZhongSi", "aListener is null!!!");
        } else if (str != null) {
            int -l_4_I = ((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG();
            m.T(-l_4_I);
            if (this.Ms == null) {
                this.Ms = new qi();
            }
            this.Ms.a(iScanTaskCallBack, str, -l_4_I);
        } else {
            f.e("ZhongSi", "packageName is null!!!");
            iScanTaskCallBack.onScanError(-1, null);
        }
    }

    protected boolean a(IUpdateCallBack iUpdateCallBack) {
        return !ic.bE() ? qw.jM().b(iUpdateCallBack) : false;
    }

    protected boolean addUninstallPkg(String str) {
        return !ic.bE() ? qo.jz().addUninstallPkg(str) : false;
    }

    public void bX(int i) {
        switch (i) {
            case 0:
                synchronized (this.Mq) {
                    this.Mk = null;
                }
            case 1:
                synchronized (this.Mo) {
                    this.Ml = null;
                }
                break;
            case 2:
                synchronized (this.Mp) {
                    this.Mm = null;
                }
                break;
            default:
                return;
        }
    }

    protected boolean cancelClean() {
        if (ic.bE() || this.Mr == null) {
            return false;
        }
        this.Mr.jm();
        this.Mr = null;
        return true;
    }

    protected boolean cancelScan(int i) {
        switch (i) {
            case 0:
                if (this.Mk != null) {
                    this.Mk.cancel();
                    break;
                }
                return false;
            case 1:
                if (this.Ml != null) {
                    this.Ml.cancel();
                    break;
                }
                return false;
            case 2:
                if (this.Mm != null) {
                    this.Mm.cancel();
                    break;
                }
                return false;
            default:
                return false;
        }
        return true;
    }

    protected boolean cleanRubbish(RubbishHolder rubbishHolder, ICleanTaskCallBack iCleanTaskCallBack) {
        if (ic.bE() || this.Mr != null || rubbishHolder == null) {
            return false;
        }
        this.Mr = new a(this, rubbishHolder, iCleanTaskCallBack, true);
        return this.Mr.jn();
    }

    protected boolean delUninstallPkg(String str) {
        return !ic.bE() ? qo.jz().delUninstallPkg(str) : false;
    }

    protected boolean easyScan(IScanTaskCallBack iScanTaskCallBack, Set<String> set) {
        if (ic.bE() || this.Ml != null) {
            return false;
        }
        boolean a;
        synchronized (this.Mo) {
            this.Ml = new rc(this);
            jk();
            int -l_5_I = ((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG();
            m.T(-l_5_I);
            Object -l_6_R = new ra(-l_5_I, true, iScanTaskCallBack);
            -l_6_R.a(set);
            a = this.Ml.a((ra) -l_6_R);
        }
        return a;
    }

    protected AppGroupDesc getGroupInfo(int i) {
        if (this.Mn == null) {
            this.Mn = re.kd();
        }
        if (this.Mn == null) {
            return null;
        }
        return (AppGroupDesc) this.Mn.get(Integer.valueOf(i));
    }

    public int getSingletonType() {
        return 1;
    }

    public void jl() {
        if (this.Ms != null) {
            this.Ms.cancel();
        }
    }

    public void onCreate(Context context) {
        s.bW(16);
        qw.jM().de();
        jk.cv().a(js.cE());
    }

    public void onDestroy() {
        synchronized (this.Mq) {
            if (this.Mk != null) {
                this.Mk.cancel();
                this.Mk.release();
                this.Mk = null;
            }
        }
        synchronized (this.Mo) {
            if (this.Ml != null) {
                this.Ml.cancel();
                this.Ml.release();
                this.Ml = null;
            }
        }
        synchronized (this.Mp) {
            if (this.Mm != null) {
                this.Mm.cancel();
                this.Mm.release();
                this.Mm = null;
            }
        }
        this.Mn = null;
    }

    protected boolean scan4app(String str, IScanTaskCallBack iScanTaskCallBack) {
        if (ic.bE() || this.Mm != null) {
            return false;
        }
        int -l_8_I;
        synchronized (this.Mp) {
            Object -l_4_R = new rd(this);
            jk();
            int -l_6_I = ((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG();
            m.T(-l_6_I);
            Object -l_7_R = new ra(-l_6_I, false, iScanTaskCallBack);
            -l_7_R.dd(str);
            -l_8_I = -l_4_R.a((ra) -l_7_R);
            this.Mm = -l_4_R;
        }
        return -l_8_I;
    }

    protected boolean scanDisk(IScanTaskCallBack iScanTaskCallBack, Set<String> set) {
        if (ic.bE()) {
            f.e("ZhongSi", "scanDisk: isExpired");
            return false;
        } else if (this.Mk == null) {
            int -l_8_I;
            if (set != null) {
                for (String -l_4_R : set) {
                    String str = "ZhongSi";
                    f.e(str, "scanDisk whitePath: " + -l_4_R);
                }
            }
            synchronized (this.Mq) {
                Object -l_4_R2 = new rc(this);
                jk();
                int -l_6_I = ((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG();
                m.T(-l_6_I);
                Object -l_7_R = new ra(-l_6_I, false, iScanTaskCallBack);
                -l_7_R.a(set);
                -l_8_I = -l_4_R2.a((ra) -l_7_R);
                this.Mk = -l_4_R2;
            }
            return -l_8_I;
        } else {
            f.e("ZhongSi", "scanDisk: null!=mScanTaskDisk");
            return false;
        }
    }
}
