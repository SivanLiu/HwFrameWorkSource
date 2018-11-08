package tmsdkobf;

import android.os.SystemClock;
import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.i;

public class la {
    public static synchronized void a(String str, String str2, int i) {
        synchronized (la.class) {
            try {
                if (TextUtils.isEmpty(str)) {
                    return;
                }
                Object -l_3_R = bE(str2);
                if (-l_3_R.size() < 500) {
                    Object -l_4_R = new File(str2);
                    if (!-l_4_R.exists()) {
                        -l_4_R.createNewFile();
                    }
                    if (i == 90 || i == SmsCheckResult.ESCT_163) {
                        -l_3_R = new ArrayList();
                    }
                    -l_3_R.add(TccCryptor.encrypt(str.getBytes(), null));
                    Object -l_6_R = new ObjectOutputStream(new FileOutputStream(-l_4_R));
                    -l_6_R.writeObject(-l_3_R);
                    -l_6_R.flush();
                    -l_6_R.close();
                } else {
                    return;
                }
            } catch (Throwable th) {
            }
        }
    }

    public static synchronized void b(int i, int i2, String str) {
        synchronized (la.class) {
            if (str == null) {
                str = "";
            }
            try {
                JceStruct -l_4_R = new ao(SmsCheckResult.ESCT_184, new ArrayList());
                Object -l_5_R = new ap(new HashMap());
                -l_5_R.bG.put(Integer.valueOf(1), String.valueOf(i));
                -l_5_R.bG.put(Integer.valueOf(2), String.valueOf(i2));
                -l_5_R.bG.put(Integer.valueOf(3), str);
                -l_4_R.bD.add(-l_5_R);
                Object -l_6_R = im.bK();
                if (-l_4_R.bD.size() > 0 && -l_6_R != null) {
                    -l_6_R.a(4060, -l_4_R, null, 0, new jy() {
                        public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                        }
                    });
                }
            } catch (Throwable th) {
            }
        }
    }

    public static synchronized ArrayList<String> bD(String str) {
        Object -l_1_R;
        synchronized (la.class) {
            -l_1_R = new ArrayList();
            try {
                Object -l_2_R = bE(str);
                if (-l_2_R.size() > 0) {
                    Object -l_3_R = -l_2_R.iterator();
                    while (-l_3_R.hasNext()) {
                        -l_1_R.add(new String(TccCryptor.decrypt((byte[]) -l_3_R.next(), null)));
                    }
                }
            } catch (Throwable th) {
            }
        }
        return -l_1_R;
    }

    static synchronized ArrayList<byte[]> bE(String str) {
        synchronized (la.class) {
            Object -l_1_R = new ArrayList();
            try {
                if (new File(str).exists()) {
                    Object -l_3_R = new ObjectInputStream(new FileInputStream(str));
                    ArrayList -l_4_R = (ArrayList) -l_3_R.readObject();
                    if (-l_4_R != null) {
                        if (-l_4_R.size() > 0) {
                            -l_1_R.addAll(-l_4_R);
                        }
                    }
                    -l_3_R.close();
                } else {
                    return -l_1_R;
                }
            } catch (Throwable th) {
            }
        }
        return -l_1_R;
    }

    public static synchronized void bF(String str) {
        synchronized (la.class) {
            try {
                Object -l_1_R = new File(str);
                if (-l_1_R.exists()) {
                    -l_1_R.delete();
                }
            } catch (Throwable th) {
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized void el() {
        synchronized (la.class) {
            int -l_6_I;
            int -l_7_I;
            long -l_0_J = System.currentTimeMillis() / 1000;
            Object -l_2_R = TMSDKContext.getApplicaionContext();
            int -l_3_I = i.hm();
            int -l_4_I = i.K(-l_2_R);
            Object -l_5_R = kz.aJ(141);
            if (-l_5_R != null) {
                if (-l_5_R.xZ && -l_3_I != 0) {
                    -l_6_I = 0;
                    try {
                        -l_6_I = Integer.valueOf(-l_5_R.yb).intValue();
                    } catch (Throwable th) {
                    }
                    if (-l_6_I <= 0) {
                        -l_6_I = SmsCheckResult.ESCT_168;
                    }
                    if ((-l_0_J - kz.dV() <= ((long) -l_6_I) * 3600 ? 1 : null) == null) {
                        lb.en();
                    }
                }
            }
            -l_5_R = kz.aJ(SmsCheckResult.ESCT_146);
            if (-l_5_R != null) {
                if (-l_5_R.xZ) {
                    -l_6_I = 0;
                    try {
                        -l_6_I = Integer.valueOf(-l_5_R.yb).intValue();
                    } catch (Throwable th2) {
                    }
                    if (-l_6_I <= 0) {
                        -l_6_I = 24;
                    }
                    if ((kz.dX() > 0 ? 1 : null) == null) {
                        kz.l(-l_0_J);
                    } else {
                        if (-l_4_I != 0) {
                            if ((-l_0_J - kz.dX() > ((long) -l_6_I) * 3600 ? 1 : null) == null) {
                            }
                            lh.eq();
                        }
                        if (-l_3_I != 0) {
                        }
                    }
                }
            }
            -l_5_R = kz.aJ(150);
            if (-l_5_R != null && -l_5_R.xZ) {
                -l_6_I = 0;
                -l_7_I = 0;
                try {
                    -l_6_I = -l_5_R.ya;
                    -l_7_I = Integer.valueOf(-l_5_R.yb).intValue();
                } catch (Throwable th3) {
                }
                if (-l_6_I <= 0) {
                    -l_6_I = 24;
                }
                if (-l_7_I <= 0) {
                    -l_7_I = 24;
                }
                if ((kz.eb() > 0 ? 1 : null) == null) {
                    kz.m((-l_0_J - 86400) - 1);
                }
                if ((-l_0_J - kz.eb() < ((long) -l_6_I) * 3600 ? 1 : null) == null) {
                    le.ep();
                }
                if ((kz.ec() > 0 ? 1 : null) == null) {
                    kz.n(-l_0_J);
                } else {
                    if (-l_4_I != 0) {
                        if ((-l_0_J - kz.ec() > ((long) -l_7_I) * 3600 ? 1 : null) == null) {
                        }
                        le.eq();
                    }
                    if (-l_3_I != 0) {
                    }
                }
            }
            -l_5_R = kz.aJ(151);
            if (-l_5_R != null && -l_5_R.xZ) {
                -l_6_I = 0;
                -l_7_I = 0;
                try {
                    -l_6_I = -l_5_R.ya;
                    -l_7_I = Integer.valueOf(-l_5_R.yb).intValue();
                } catch (Throwable th4) {
                }
                if (-l_6_I <= 0) {
                    -l_6_I = 24;
                }
                if (-l_7_I <= 0) {
                    -l_7_I = 24;
                }
                if ((kz.ee() <= 0 ? 1 : null) == null) {
                }
                ld.ep();
                if ((kz.ef() > 0 ? 1 : null) == null) {
                    kz.p(-l_0_J);
                } else {
                    if (-l_4_I != 0) {
                        if ((-l_0_J - kz.ef() > ((long) -l_7_I) * 3600 ? 1 : null) == null) {
                        }
                        ld.et();
                        ld.eq();
                    }
                    if (-l_3_I != 0) {
                    }
                }
            }
            -l_5_R = kz.aJ(SmsCheckResult.ESCT_163);
            if (-l_5_R != null && -l_5_R.xZ) {
                -l_6_I = 0;
                -l_7_I = 0;
                try {
                    -l_6_I = -l_5_R.ya;
                    -l_7_I = Integer.valueOf(-l_5_R.yb).intValue();
                } catch (Throwable th5) {
                }
                if (-l_6_I <= 0) {
                    -l_6_I = 4;
                }
                if (-l_7_I <= 0) {
                    -l_7_I = 24;
                }
                if ((kz.eh() <= 0 ? 1 : null) == null) {
                }
                lc.ep();
                if ((kz.ei() > 0 ? 1 : null) == null) {
                    kz.r(-l_0_J);
                } else if (-l_3_I != 0) {
                    if ((-l_0_J - kz.ei() <= ((long) -l_7_I) * 3600 ? 1 : null) == null) {
                        lc.eq();
                    }
                }
            }
        }
    }

    public static boolean em() {
        long -l_2_J = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        if (!(Math.abs(-l_2_J - kz.ej()) >= 2000)) {
            return false;
        }
        kz.s(-l_2_J);
        return true;
    }

    public static synchronized void j(List<String> list) {
        synchronized (la.class) {
            kz.i(list);
        }
    }
}
