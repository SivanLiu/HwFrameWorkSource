package tmsdkobf;

import android.content.Context;
import android.os.Bundle;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;

public class ll {
    public static void aM(int i) {
        if (gf.S().getStartCount() > 0 && eA()) {
            gf.S().c(System.currentTimeMillis());
            ((ki) fj.D(4)).addTask(new Runnable() {
                public void run() {
                    try {
                        int -l_1_I = ll.n(TMSDKContext.getApplicaionContext());
                        int -l_2_I = ll.o(TMSDKContext.getApplicaionContext());
                        if (-l_1_I == 0) {
                            kt.saveActionData(1320009);
                        } else if (-l_2_I == 0) {
                            kt.saveActionData(29986);
                        } else {
                            kt.saveActionData(29985);
                        }
                        if (-l_1_I != 0 && -l_2_I == 0) {
                            ll.p(TMSDKContext.getApplicaionContext());
                            int -l_3_I = gf.S().getStartCount();
                            if (-l_3_I > 0) {
                                gf.S().J(-l_3_I - 1);
                            }
                            Thread.sleep(15000);
                            if (ll.o(TMSDKContext.getApplicaionContext())) {
                                kt.saveActionData(29984);
                            } else {
                                kt.saveActionData(29983);
                            }
                        }
                    } catch (InterruptedException e) {
                    } catch (Throwable th) {
                        return;
                    }
                    kr.p(true);
                }
            }, "checkStartTMSecure");
        }
    }

    static boolean eA() {
        int -l_7_I;
        boolean z = true;
        int -l_0_I = 0;
        long -l_1_J = System.currentTimeMillis();
        long -l_3_J = gf.S().ae();
        long -l_5_J = gf.S().af();
        if (-l_5_J != 0) {
            if (-l_1_J / 1000 >= -l_5_J) {
                -l_7_I = 0;
                if (-l_7_I != 0) {
                    return false;
                }
                if (!(-l_1_J > -l_3_J)) {
                    if (-l_1_J - -l_3_J >= 86400000) {
                        z = false;
                    }
                    if (!z) {
                        -l_0_I = 1;
                    }
                }
                return -l_0_I;
            }
        }
        -l_7_I = 1;
        if (-l_7_I != 0) {
            return false;
        }
        if (-l_1_J > -l_3_J) {
        }
        if (-l_1_J > -l_3_J) {
            if (-l_1_J - -l_3_J >= 86400000) {
                z = false;
            }
            if (z) {
                -l_0_I = 1;
            }
        }
        return -l_0_I;
    }

    public static boolean n(Context context) {
        int -l_1_I = 0;
        try {
            -l_1_I = TMServiceFactory.getSystemInfoService().ai(ir.rV);
        } catch (Throwable th) {
        }
        return -l_1_I;
    }

    public static boolean o(Context context) {
        return fy.a(context, ir.rV);
    }

    public static void p(Context context) {
        kv.n("cccccc", "startByActivity:");
        Object -l_1_R = null;
        try {
            -l_1_R = context.getPackageManager().getPackageInfo(ir.rV, 16384);
        } catch (Object -l_2_R) {
            Object -l_2_R2;
            -l_2_R2.printStackTrace();
        }
        if (-l_1_R != null && -l_1_R.versionCode >= 1066) {
            -l_2_R2 = "EP_Secure_SDK_6.1.0";
            Object -l_3_R = "{'dest_view':65537,'show_id':'show_001','show_channel':'" + im.bQ() + "'}";
            kv.n("cccccc", "jumpParams:" + -l_3_R + "   appToken:" + -l_2_R2);
            Object -l_4_R;
            try {
                -l_4_R = context.getPackageManager().getLaunchIntentForPackage(ir.rV);
                Object -l_5_R = new Bundle();
                -l_5_R.putString("platform_id", -l_2_R2);
                -l_5_R.putString("launch_param", -l_3_R);
                -l_4_R.putExtras(-l_5_R);
                -l_4_R.setFlags(402653184);
                context.startActivity(-l_4_R);
            } catch (Object -l_4_R2) {
                -l_4_R2.printStackTrace();
            }
        }
    }
}
