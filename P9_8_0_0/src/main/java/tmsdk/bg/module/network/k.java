package tmsdk.bg.module.network;

import android.util.Log;
import java.io.File;
import java.lang.reflect.Method;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdkobf.im;
import tmsdkobf.lu;
import tmsdkobf.ov;
import tmsdkobf.oz;

final class k {
    private final String TAG = "TrafficStats";
    private Method wF;
    private Method wG;
    private boolean wH;
    private boolean wI;
    private boolean wJ = false;

    public k() {
        Object -l_1_R;
        try {
            -l_1_R = Class.forName("android.net.TrafficStats");
            this.wF = -l_1_R.getDeclaredMethod("getUidRxBytes", new Class[]{Integer.TYPE});
            this.wG = -l_1_R.getDeclaredMethod("getUidTxBytes", new Class[]{Integer.TYPE});
            this.wH = true;
        } catch (Object -l_1_R2) {
            this.wH = false;
            -l_1_R2.printStackTrace();
        }
        if (this.wH) {
            im.bJ().addTask(new Runnable(this) {
                final /* synthetic */ k wK;

                {
                    this.wK = r1;
                }

                public void run() {
                    int -l_1_I = 1;
                    try {
                        -l_1_I = this.wK.dv();
                    } catch (Object -l_2_R) {
                        Log.w("TrafficStats", -l_2_R.getMessage());
                    }
                    if (-l_1_I == 0) {
                        this.wK.wI = new File("/proc/uid_stat").exists();
                        if (this.wK.wI) {
                            this.wK.wH = false;
                        } else {
                            this.wK.wH = true;
                        }
                    }
                    Log.i("TrafficStats", "^^ mAPISupported" + this.wK.wH + " mFileSupported " + this.wK.wI);
                }
            }, "checkAPIAvaliable");
            return;
        }
        this.wI = new File("/proc/uid_stat").exists();
        Log.i("TrafficStats", "^^ mAPISupported" + this.wH + " mFileSupported " + this.wI);
    }

    private boolean a(ov ovVar) {
        if (ovVar == null || ovVar.hA() == null) {
            return false;
        }
        int -l_2_I = 0;
        for (Object -l_6_R : ovVar.hA()) {
            if ("android.permission.INTERNET".equals(-l_6_R)) {
                -l_2_I = 1;
                break;
            }
        }
        return -l_2_I;
    }

    private long c(int i, String str, String str2) {
        Object -l_8_R;
        long -l_4_J = -1;
        Object -l_6_R = new File("/proc/uid_stat/" + i + "/" + str);
        Object -l_7_R = new File("/proc/uid_stat/" + i + "/" + str2);
        if (-l_6_R.exists()) {
            -l_8_R = lu.e(-l_6_R);
            if (-l_8_R != null && -l_8_R.length > 0) {
                -l_4_J = Long.parseLong(-l_8_R[0]);
            }
        }
        if (-l_7_R.exists()) {
            -l_8_R = lu.e(-l_7_R);
            if (-l_8_R != null && -l_8_R.length > 0) {
                -l_4_J = !-l_6_R.exists() ? Long.parseLong(-l_8_R[0]) : -l_4_J + Long.parseLong(-l_8_R[0]);
            }
        }
        return -l_4_J;
    }

    private boolean dv() {
        long -l_1_J = System.currentTimeMillis();
        Object -l_3_R = ((oz) ManagerCreatorC.getManager(oz.class)).f(34, 0);
        if (-l_3_R == null || -l_3_R.size() == 0) {
            -l_3_R = ((oz) ManagerCreatorC.getManager(oz.class)).f(34, 1);
        }
        if (-l_3_R == null || -l_3_R.size() == 0) {
            return false;
        }
        int -l_4_I = -l_3_R.size();
        Object -l_5_R = TMSDKContext.getApplicaionContext().getPackageName();
        int -l_6_I = 0;
        int -l_7_I = 0;
        while (-l_7_I < -l_4_I) {
            if (a((ov) -l_3_R.get(-l_7_I))) {
                int i;
                if (getUidRxBytes(((ov) -l_3_R.get(-l_7_I)).getUid()) <= 0) {
                    i = 1;
                } else {
                    boolean z = false;
                }
                if (i == 0 && !((ov) -l_3_R.get(-l_7_I)).getPackageName().equals(-l_5_R)) {
                    Log.i("TrafficStats", "^^ check traffic api avaliable count " + -l_7_I);
                    -l_6_I = 1;
                    break;
                }
            }
            -l_7_I++;
        }
        Log.i("TrafficStats", "^^ check time " + (System.currentTimeMillis() - -l_1_J) + " isAvaliable " + -l_6_I);
        return -l_6_I;
    }

    public long getUidRxBytes(int i) {
        long -l_2_J = -1;
        if (this.wH) {
            try {
                -l_2_J = Long.valueOf(this.wF.invoke(null, new Object[]{Integer.valueOf(i)}).toString()).longValue();
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
            }
        } else if (this.wI) {
            -l_2_J = c(i, "tcp_rcv", "udp_rcv");
        }
        return -l_2_J;
    }

    public long getUidTxBytes(int i) {
        long -l_2_J = -1;
        if (this.wH) {
            try {
                -l_2_J = Long.valueOf(this.wG.invoke(null, new Object[]{Integer.valueOf(i)}).toString()).longValue();
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
            }
        } else if (this.wI) {
            -l_2_J = c(i, "tcp_snd", "udp_snd");
        }
        return -l_2_J;
    }

    public boolean isSupportTrafficState() {
        return this.wI || this.wH;
    }
}
