package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build.VERSION;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import tmsdk.common.TMSDKContext;

public class fs {
    private static volatile fs ne = null;
    private Context mContext;
    public long nd = 0;

    private fs(Context context) {
        this.mContext = context;
    }

    private ft H(String str) {
        Object -l_2_R = null;
        try {
            if (TextUtils.isEmpty(str)) {
                return null;
            }
            Object -l_3_R = str.split(";");
            if (-l_3_R != null && -l_3_R.length >= 9) {
                Object -l_2_R2 = new ft();
                try {
                    -l_2_R2.I(-l_3_R[0]);
                    -l_2_R2.J(-l_3_R[1]);
                    -l_2_R2.K(-l_3_R[2]);
                    -l_2_R2.F(Integer.valueOf(-l_3_R[3]).intValue());
                    -l_2_R2.L(-l_3_R[4]);
                    -l_2_R2.M(-l_3_R[5]);
                    -l_2_R2.N(-l_3_R[6]);
                    -l_2_R2.G(Integer.valueOf(-l_3_R[7]).intValue());
                    -l_2_R2.a((float) Integer.valueOf(-l_3_R[8]).intValue());
                    -l_2_R = -l_2_R2;
                } catch (Exception e) {
                    -l_2_R = -l_2_R2;
                    return -l_2_R;
                }
            }
            return -l_2_R;
        } catch (Exception e2) {
            return -l_2_R;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void a(Context context, ft ftVar) {
        Object -l_3_R = new StringBuilder();
        -l_3_R.append(ftVar.y()).append(";");
        if (fy.c(context, ftVar) != 0) {
            PackageInfo -l_6_R = context.getPackageManager().getPackageInfo(ftVar.y(), 4);
            String -l_7_R = -l_6_R == null ? "0" : -l_6_R.versionName.trim();
            -l_3_R.append("100").append(";").append(-l_7_R);
            int -l_8_I = fy.a(context, ftVar.D());
            -l_3_R.append(";").append(-l_8_I == 0 ? "120" : "102");
            int -l_9_I = Integer.valueOf(ftVar.B()).intValue();
            if (-l_8_I == 0 && 1 != -l_9_I) {
                if (fy.a(-l_7_R, ftVar.A().trim()) != 0) {
                    -l_3_R.append(";").append("105");
                    int -l_12_I = 0;
                    if (2 == -l_9_I) {
                        -l_12_I = fy.a(context, ftVar.y(), ftVar.C());
                        if (-l_12_I != 0) {
                            -l_3_R.append(";").append("103");
                        }
                    }
                    if (-l_12_I == 0) {
                        if ((ga.Q() / 1000 < ((long) ftVar.F()) ? 1 : null) == null) {
                            -l_3_R.append(";").append("106");
                            int -l_17_I = b(context, ftVar);
                            if (-l_17_I <= 0) {
                                -l_3_R.append(";").append("124");
                            } else {
                                -l_3_R.append(";").append("107");
                                if (-l_17_I >= 2) {
                                    -l_3_R.append(";").append("123");
                                }
                            }
                            int -l_18_I = 0;
                            int -l_19_I = 30;
                            do {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                }
                                -l_19_I--;
                            } while (-l_19_I > 0);
                            if (fy.a(context, ftVar.D())) {
                                -l_18_I = 1;
                                -l_3_R.append(";").append("108");
                            }
                            -l_19_I = 120;
                            do {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e2) {
                                }
                                -l_19_I--;
                            } while (-l_19_I > 0);
                            if (fy.a(context, ftVar.D())) {
                                -l_18_I = 1;
                                -l_3_R.append(";").append("109");
                            }
                            if (-l_18_I == 0) {
                                -l_3_R.append(";").append("125");
                            }
                        } else {
                            -l_3_R.append(";").append("122");
                        }
                    }
                } else {
                    -l_3_R.append(";").append("121");
                }
            }
        } else {
            -l_3_R.append("101");
        }
        fr.r().a(1320063, -l_3_R.toString());
    }

    private int b(Context context, ft ftVar) {
        int -l_8_I = 0;
        kv.d("WakeupUtil", "wakeUpApp-cmd:[" + ftVar.C() + "][" + ftVar.y() + "][" + ftVar.z() + "]");
        if (TextUtils.isEmpty(ftVar.C())) {
            return -1;
        }
        try {
            Object -l_4_R = "catfish" + "." + TMSDKContext.getApplicaionContext().getPackageName() + ".0.0";
            Object -l_6_R;
            switch (Integer.valueOf(ftVar.B()).intValue()) {
                case 2:
                    -l_6_R = new Intent();
                    -l_6_R.setClassName(ftVar.y(), ftVar.C());
                    -l_6_R.putExtra("platform_id", -l_4_R);
                    -l_6_R.putExtra("channel_id", im.bQ());
                    -l_6_R.setPackage(ftVar.y());
                    -l_6_R.addFlags(32);
                    kv.d("WakeupUtil", "startService-intent:" + -l_6_R + "]");
                    if ((context.startService(-l_6_R) == null ? 0 : 1) == 0) {
                        return 1;
                    }
                    break;
                case 3:
                    -l_6_R = ftVar.C();
                    if (VERSION.SDK_INT >= 17) {
                        -l_6_R = -l_6_R + " --user 0";
                    }
                    -l_6_R = (-l_6_R + " --include-stopped-packages") + " -e platform_id " + -l_4_R + " -e channel_id " + im.bQ();
                    kv.d("WakeupUtil", "AM-cmd:" + -l_6_R + "]");
                    if (Runtime.getRuntime().exec(-l_6_R) != null) {
                        -l_8_I = 1;
                    }
                    if (-l_8_I == 0) {
                        return 1;
                    }
                    break;
                case 4:
                    -l_6_R = ftVar.C() + " -e platform_id " + -l_4_R + " -e channel_id " + im.bQ();
                    kv.d("WakeupUtil", "AM_TO_INTENT-cmd:" + -l_6_R + "]");
                    Object -l_7_R = new fx(-l_6_R);
                    kv.d("WakeupUtil", "AM_TO_INTENT-intent:" + -l_7_R.getIntent() + "]");
                    if (-l_7_R.d(context) == 0) {
                        return 1;
                    }
                    break;
                default:
                    return -1;
            }
            return 2;
        } catch (Exception e) {
            return -1;
        }
    }

    public static fs c(Context context) {
        if (ne == null) {
            Object -l_1_R = fs.class;
            synchronized (fs.class) {
                if (ne == null) {
                    ne = new fs(context);
                }
            }
        }
        return ne;
    }

    private int w() {
        int -l_1_I = 0;
        Object<ft> -l_2_R = x();
        if (-l_2_R != null && -l_2_R.size() > 0) {
            Object -l_3_R = fr.r().v();
            for (ft -l_5_R : -l_2_R) {
                if (!(-l_5_R == null || -l_5_R.y().equals(this.mContext.getPackageName()))) {
                    long -l_6_J = System.currentTimeMillis();
                    long -l_8_J = 0;
                    String -l_10_R = (String) -l_3_R.get(-l_5_R.y());
                    if (!TextUtils.isEmpty(-l_10_R)) {
                        -l_8_J = Long.valueOf(-l_10_R).longValue();
                    }
                    if (((float) ((-l_6_J - -l_8_J) / 3600000)) >= Float.valueOf(-l_5_R.G()).floatValue()) {
                        -l_1_I++;
                        Object -l_14_R = -l_5_R.E();
                        int -l_15_I = 0;
                        if (!TextUtils.isEmpty(-l_14_R)) {
                            try {
                                if ((new Date().getTime() > new SimpleDateFormat("yyyyMMdd").parse(-l_14_R).getTime() ? 1 : null) == null) {
                                    -l_15_I = 1;
                                }
                            } catch (Exception e) {
                            }
                        }
                        if (-l_15_I == 0) {
                            Object -l_16_R = new StringBuilder();
                            -l_16_R.append(-l_5_R.y()).append(";").append("119");
                            fr.r().a(1320063, -l_16_R.toString());
                        } else {
                            a(this.mContext, -l_5_R);
                        }
                        fr.r().v().put(-l_5_R.y(), "" + System.currentTimeMillis());
                    }
                }
            }
            fr.r().b(this.mContext);
        }
        return -l_1_I;
    }

    private List<ft> x() {
        Object -l_1_R = new ArrayList();
        Object -l_2_R = fr.r().u();
        if (-l_2_R == null) {
            return null;
        }
        Object -l_4_R = -l_2_R.H().iterator();
        while (-l_4_R.hasNext()) {
            Object -l_6_R = H((String) -l_4_R.next());
            if (-l_6_R != null) {
                -l_1_R.add(-l_6_R);
            }
        }
        return -l_1_R;
    }

    public synchronized void c(boolean z) {
        Object obj = 1;
        synchronized (this) {
            try {
                long -l_2_J = System.currentTimeMillis();
                fr.r().a(this.mContext);
                Object -l_4_R = fr.r().u();
                if (-l_4_R != null) {
                    if (-l_4_R.no) {
                        if ((-l_2_J / 1000 > ((long) -l_4_R.R) ? 1 : null) == null) {
                            w();
                        }
                        if (!z) {
                            if (-l_2_J / 1000 > ((long) -l_4_R.R)) {
                                obj = null;
                            }
                            if (obj == null) {
                            }
                            fr.s();
                        }
                        fr.r().t();
                        gf.S().K(0);
                        fr.s();
                    }
                }
                gf.S().K(0);
                kt.saveActionData(1320054);
            } catch (Throwable th) {
            }
            fr.s();
        }
    }
}
