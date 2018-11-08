package tmsdk.common;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephony.Stub;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import tmsdk.common.utils.f;
import tmsdkobf.im;
import tmsdkobf.mi;

public class DualSimTelephonyManager implements tmsdkobf.im.a {
    private static DualSimTelephonyManager wZ;
    private static final String[] xa = new String[]{"phone1", "phone2", "phoneEX"};
    private ArrayList<a> xb = new ArrayList(2);

    static class a {
        public WeakReference<PhoneStateListener> xc;
        public int xd;
        public boolean xe;
        public TelephonyManager xf;

        public a(PhoneStateListener phoneStateListener, int i, boolean z, TelephonyManager telephonyManager) {
            this.xc = new WeakReference(phoneStateListener);
            this.xd = i;
            this.xe = z;
            this.xf = telephonyManager;
        }
    }

    private DualSimTelephonyManager() {
        im.a(this);
    }

    private a a(PhoneStateListener phoneStateListener, int i, int i2) {
        Object -l_4_R;
        Object -l_5_R;
        boolean z = false;
        switch (i2) {
            case -1:
            case 0:
                -l_4_R = (TelephonyManager) TMSDKContext.getApplicaionContext().getSystemService("phone");
                break;
            case 1:
                -l_5_R = im.rE;
                if (-l_5_R == null || !-l_5_R.iu()) {
                    -l_4_R = getSecondTelephonyManager();
                    break;
                }
                TelephonyManager -l_4_R2 = (TelephonyManager) TMSDKContext.getApplicaionContext().getSystemService("phone");
                break;
                break;
            default:
                return null;
        }
        if (-l_4_R != null) {
            try {
                -l_4_R.listen(phoneStateListener, i);
            } catch (Object -l_5_R2) {
                -l_5_R2.printStackTrace();
            }
        }
        if (i2 == 1) {
            z = true;
        }
        return new a(phoneStateListener, i, z, -l_4_R);
    }

    public static ITelephony getDefaultTelephony() {
        ITelephony -l_0_R = null;
        Object -l_1_R = im.rE;
        if (-l_1_R != null) {
            -l_0_R = -l_1_R.bS(0);
            if (-l_0_R != null) {
                return -l_0_R;
            }
        }
        try {
            TelephonyManager -l_3_R = (TelephonyManager) TMSDKContext.getApplicaionContext().getSystemService("phone");
            if (-l_3_R == null) {
                return null;
            }
            Object -l_4_R = TelephonyManager.class.getDeclaredMethod("getITelephony", (Class[]) null);
            if (-l_4_R == null) {
                return null;
            }
            -l_4_R.setAccessible(true);
            -l_0_R = (ITelephony) -l_4_R.invoke(-l_3_R, (Object[]) null);
            return -l_0_R;
        } catch (Object -l_3_R2) {
            f.b("DualSimTelephonyManager", "getDefaultTelephony", -l_3_R2);
        } catch (Object -l_3_R22) {
            f.b("DualSimTelephonyManager", "getDefaultTelephony", -l_3_R22);
        } catch (Object -l_3_R222) {
            f.b("DualSimTelephonyManager", "getDefaultTelephony", -l_3_R222);
        } catch (Object -l_3_R2222) {
            f.b("DualSimTelephonyManager", "getDefaultTelephony", -l_3_R2222);
        } catch (Object -l_3_R22222) {
            f.b("DualSimTelephonyManager", "getDefaultTelephony", -l_3_R22222);
        }
    }

    public static synchronized DualSimTelephonyManager getInstance() {
        DualSimTelephonyManager dualSimTelephonyManager;
        synchronized (DualSimTelephonyManager.class) {
            if (wZ == null) {
                wZ = new DualSimTelephonyManager();
            }
            dualSimTelephonyManager = wZ;
        }
        return dualSimTelephonyManager;
    }

    public static ITelephony getSecondTelephony() {
        Object -l_1_R = im.rE;
        if (-l_1_R != null) {
            Object -l_0_R = -l_1_R.bS(1);
            if (-l_0_R != null) {
                return -l_0_R;
            }
        }
        for (Object -l_5_R : xa) {
            if (mi.checkService(-l_5_R) != null) {
                Object -l_6_R = mi.getService(-l_5_R);
                if (-l_6_R != null) {
                    return Stub.asInterface(-l_6_R);
                }
            }
        }
        return null;
    }

    public TelephonyManager getSecondTelephonyManager() {
        Object -l_2_R;
        Object -l_1_R = im.rE;
        if (-l_1_R != null) {
            -l_2_R = -l_1_R.iq();
            if (!(-l_2_R == null || mi.checkService(-l_2_R) == null)) {
                return (TelephonyManager) TMSDKContext.getApplicaionContext().getSystemService(-l_2_R);
            }
        }
        try {
            for (Object -l_5_R : xa) {
                if (mi.checkService(-l_5_R) != null) {
                    return (TelephonyManager) TMSDKContext.getApplicaionContext().getSystemService(-l_5_R);
                }
            }
        } catch (Object -l_2_R2) {
            f.f("DualSimTelephonyManager", -l_2_R2);
        }
        try {
            for (Object -l_5_R2 : xa) {
                if (mi.checkService(-l_5_R2) != null) {
                    return (TelephonyManager) TMSDKContext.getApplicaionContext().getSystemService(-l_5_R2);
                }
            }
        } catch (Object -l_2_R3) {
            f.e("DualSimTelephonyManager", -l_2_R3);
            -l_2_R2 = -l_2_R3;
        }
        return null;
    }

    public void handleSdkContextEvent(int i) {
        if (i == 1) {
            reListenPhoneState();
        }
    }

    public boolean listenPhonesState(int i, PhoneStateListener phoneStateListener, int i2) {
        Object -l_4_R = a(phoneStateListener, i2, i);
        if (-l_4_R == null) {
            return false;
        }
        Object -l_5_R = this.xb.iterator();
        int -l_6_I = 0;
        a aVar = null;
        while (-l_6_I == 0 && -l_5_R.hasNext()) {
            aVar = (a) -l_5_R.next();
            if (aVar.xe == (i == 1) && aVar.xc.get() == phoneStateListener) {
                -l_6_I = 1;
            }
        }
        if (-l_6_I == 0) {
            if (i2 != 0) {
                this.xb.add(-l_4_R);
            }
        } else if (i2 != 0) {
            aVar.xd = Integer.valueOf(i2).intValue();
        } else {
            -l_5_R.remove();
        }
        return true;
    }

    public void reListenPhoneState() {
        Object -l_1_R = this.xb.iterator();
        while (-l_1_R.hasNext()) {
            a -l_2_R = (a) -l_1_R.next();
            PhoneStateListener -l_3_R = (PhoneStateListener) -l_2_R.xc.get();
            if (-l_3_R != null) {
                if (-l_2_R.xf != null) {
                    -l_2_R.xf.listen(-l_3_R, 0);
                }
                Object -l_4_R = a(-l_3_R, -l_2_R.xd, !-l_2_R.xe ? 1 : 0);
                if (-l_4_R != null) {
                    -l_2_R.xf = -l_4_R.xf;
                }
            }
        }
    }
}
