package tmsdkobf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import java.util.concurrent.ConcurrentHashMap;
import tmsdk.common.TMSDKContext;
import tmsdk.common.utils.f;

public class lm {
    private static Object lock = new Object();
    private static lm yC = null;
    private static Object yD = new Object();
    private Context context = TMSDKContext.getApplicaionContext();
    ConcurrentHashMap<String, a> yB = new ConcurrentHashMap();

    class a extends if {
        public String action = null;
        public Runnable yE = null;
        final /* synthetic */ lm yF;

        a(lm lmVar) {
            this.yF = lmVar;
        }

        public void doOnRecv(Context context, Intent intent) {
            final Object -l_3_R = intent.getAction();
            if (-l_3_R != null) {
                f.f("cccccc", "action...");
                if (this.action.equals(-l_3_R) && this.yE != null) {
                    im.bJ().addTask(new Runnable(this) {
                        final /* synthetic */ a yH;

                        public void run() {
                            this.yH.yE.run();
                            this.yH.yF.bH(-l_3_R);
                        }
                    }, "AlarmerTaskReceiver");
                }
            }
        }
    }

    private lm() {
    }

    public static lm eC() {
        if (yC == null) {
            synchronized (lock) {
                if (yC == null) {
                    yC = new lm();
                }
            }
        }
        return yC;
    }

    public void a(String str, long j, Runnable runnable) {
        try {
            synchronized (yD) {
                Object -l_6_R = new a(this);
                this.context.registerReceiver(-l_6_R, new IntentFilter(str));
                -l_6_R.yE = runnable;
                -l_6_R.action = str;
                Object -l_8_R = PendingIntent.getBroadcast(this.context, 0, new Intent(str), 0);
                AlarmManager -l_9_R = (AlarmManager) this.context.getSystemService("alarm");
                this.yB.put(str, -l_6_R);
                -l_9_R.set(0, System.currentTimeMillis() + j, -l_8_R);
            }
        } catch (Throwable th) {
        }
    }

    public void bH(String str) {
        synchronized (yD) {
            a -l_3_R = (a) this.yB.remove(str);
            if (-l_3_R != null) {
                oj.h(this.context, str);
                this.context.unregisterReceiver(-l_3_R);
            }
        }
    }
}
