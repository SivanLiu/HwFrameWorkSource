package tmsdkobf;

import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class lb {

    static class a {
        public String id;
        public long time;

        a() {
        }
    }

    private static long c(File file) {
        Object -l_1_R = file.listFiles();
        long -l_2_J = 0;
        Object -l_4_R = -l_1_R;
        int -l_5_I = -l_1_R.length;
        for (int -l_6_I = 0; -l_6_I < -l_5_I; -l_6_I++) {
            long -l_8_J = -l_4_R[-l_6_I].lastModified();
            if ((-l_8_J <= -l_2_J ? 1 : null) == null) {
                -l_2_J = -l_8_J;
            }
        }
        return -l_2_J;
    }

    public static void en() {
        try {
            Object -l_0_R = eo();
            if (-l_0_R != null && -l_0_R.size() > 0) {
                JceStruct -l_1_R = new ao(141, new ArrayList());
                Object -l_2_R = -l_0_R.iterator();
                while (-l_2_R.hasNext()) {
                    a -l_3_R = (a) -l_2_R.next();
                    Object -l_4_R = new ap(new HashMap());
                    -l_4_R.bG.put(Integer.valueOf(1), -l_3_R.id);
                    -l_4_R.bG.put(Integer.valueOf(7), String.valueOf(-l_3_R.time));
                    -l_1_R.bD.add(-l_4_R);
                }
                -l_2_R = im.bK();
                if (-l_1_R.bD.size() > 0 && -l_2_R != null) {
                    -l_2_R.a(4060, -l_1_R, null, 0, new jy() {
                        public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                            if (i3 == 0 && i4 == 0) {
                                kz.k(System.currentTimeMillis() / 1000);
                            }
                        }
                    });
                }
                return;
            }
            la.b(141, 1001, "");
        } catch (Throwable th) {
        }
    }

    private static ArrayList<a> eo() {
        Object -l_0_R = new ArrayList();
        try {
            Object -l_2_R = new File(lu.eG() + "/tencent/MicroMsg/");
            if (!-l_2_R.exists()) {
                return -l_0_R;
            }
            Object -l_3_R = -l_2_R.listFiles();
            Object -l_4_R = -l_3_R;
            int -l_5_I = -l_3_R.length;
            for (int -l_6_I = 0; -l_6_I < -l_5_I; -l_6_I++) {
                Object -l_7_R = -l_4_R[-l_6_I];
                if (-l_7_R.getName().length() == 32) {
                    Object -l_8_R = new a();
                    -l_8_R.id = -l_7_R.getName();
                    -l_8_R.time = c(-l_7_R) / 1000;
                    -l_0_R.add(-l_8_R);
                }
            }
            return -l_0_R;
        } catch (Throwable th) {
        }
    }
}
