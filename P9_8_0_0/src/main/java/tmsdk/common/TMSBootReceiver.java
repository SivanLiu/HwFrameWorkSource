package tmsdk.common;

import android.content.Context;
import android.content.Intent;
import tmsdkobf.if;
import tmsdkobf.im;
import tmsdkobf.lp;

public abstract class TMSBootReceiver extends if {

    private static final class a {
        private static final short[] xr = new short[]{(short) 64, (short) 75, (short) 72, (short) 8, (short) 86, (short) 65, (short) 65, (short) 69, (short) 68, (short) 31, (short) 27, (short) 30, (short) 1, (short) 93, (short) 94, (short) 80, (short) 90, (short) 88, (short) 80, (short) 69, (short) 86, (short) 94, (short) 92};

        private a() {
        }

        private String a(short[] sArr) {
            Object -l_2_R = new StringBuffer();
            Object -l_3_R = b(sArr);
            for (short s : -l_3_R) {
                -l_2_R.append((char) s);
            }
            return -l_2_R.toString();
        }

        private short[] b(short[] sArr) {
            Object -l_2_R = new short[sArr.length];
            int -l_3_I = 35;
            int -l_4_I = 0;
            while (-l_4_I < sArr.length) {
                -l_2_R[-l_4_I] = (short) ((short) (sArr[-l_4_I] ^ -l_3_I));
                -l_4_I++;
                -l_3_I = (char) (-l_3_I + 1);
            }
            return -l_2_R;
        }

        public boolean m(Context context) {
            return TMServiceFactory.getSystemInfoService().ai(a(xr));
        }
    }

    public void doOnRecv(final Context context, Intent intent) {
        im.bJ().newFreeThread(new Runnable(this) {
            final /* synthetic */ TMSBootReceiver xq;

            public void run() {
                int i = 0;
                Object -l_1_R = new lp();
                -l_1_R.t(0, (int) (System.currentTimeMillis() / 1000));
                if (!new a().m(context)) {
                    i = 1;
                }
                -l_1_R.t(1, i);
                -l_1_R.commit();
            }
        }, "TMSBootReceiveThread").start();
    }
}
