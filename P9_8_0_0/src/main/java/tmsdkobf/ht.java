package tmsdkobf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.utils.f;
import tmsdk.common.utils.n;

public class ht extends if implements tmsdkobf.im.a {
    private static ht qE = null;
    private static final String[] qF = new String[]{"android.provider.Telephony.SMS_RECEIVED", "android.provider.Telephony.SMS_RECEIVED2", "android.provider.Telephony.GSM_SMS_RECEIVED"};
    private static final String[] qG = new String[]{"android.provider.Telephony.WAP_PUSH_RECEIVED", "android.provider.Telephony.WAP_PUSH_GSM_RECEIVED"};
    private Handler handler;
    private Context mContext;
    private hu qA;
    private Queue<SmsEntity> qB;
    private a qC;
    private boolean qD;
    private hs qy;
    private boolean qz;

    private class a implements Runnable {
        final /* synthetic */ ht qH;

        private a(ht htVar) {
            this.qH = htVar;
        }

        public void run() {
            if (this.qH.qA != null) {
                while (true) {
                    SmsEntity -l_1_R = (SmsEntity) this.qH.qB.poll();
                    if (-l_1_R == null) {
                        break;
                    }
                    this.qH.qA.a(-l_1_R, this.qH);
                }
            }
            this.qH.qD = false;
        }
    }

    public ht() {
        this.qy = new hs();
        this.qD = false;
        this.handler = new Handler(this) {
            final /* synthetic */ ht qH;

            {
                this.qH = r1;
            }

            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        if (this.qH.qC == null) {
                            this.qH.qC = new a();
                        }
                        if (!this.qH.qD) {
                            this.qH.qD = true;
                            im.bJ().addTask(this.qH.qC, "filterSms");
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mContext = TMSDKContext.getApplicaionContext();
        this.qB = new ConcurrentLinkedQueue();
    }

    private ht(Context context) {
        this.qy = new hs();
        this.qD = false;
        this.handler = /* anonymous class already generated */;
        this.mContext = context;
        this.qB = new ConcurrentLinkedQueue();
    }

    public static synchronized ht h(Context context) {
        ht htVar;
        synchronized (ht.class) {
            if (qE == null) {
                qE = new ht(context);
            }
            htVar = qE;
        }
        return htVar;
    }

    public static boolean i(Context context) {
        return false;
    }

    public static boolean j(Context context) {
        return false;
    }

    public void a(Context context, Intent intent, BroadcastReceiver broadcastReceiver) {
        Object -l_4_R = intent.getAction();
        f.f("MessageReceiver", this + " action " + -l_4_R + "   getInstance" + h(context));
        if (n.iX() <= 18 || !j(context) || "android.provider.Telephony.SMS_DELIVER".equals(-l_4_R) || "android.provider.Telephony.WAP_PUSH_DELIVER".equals(-l_4_R)) {
            SmsEntity -l_5_R = null;
            this.qy.a(intent);
            if (this.qy.bv()) {
                -l_5_R = this.qy.bt();
            }
            if (!(-l_5_R == null || TextUtils.isEmpty(-l_5_R.phonenum) || TextUtils.isEmpty(-l_5_R.body))) {
                if (i(context)) {
                    this.qB.add(-l_5_R);
                    this.handler.sendEmptyMessage(1);
                    if (broadcastReceiver == null) {
                        try {
                            abortBroadcast();
                        } catch (Object -l_6_R) {
                            f.e("MessageReceiver", -l_6_R);
                        }
                    } else {
                        broadcastReceiver.abortBroadcast();
                    }
                } else if (n.iX() > 4 && this.qA != null) {
                    this.qA.a(-l_5_R, this);
                }
            }
        }
    }

    public void a(hu huVar) {
        this.qA = huVar;
        if (this.qA != null && this.qB.size() > 0) {
            this.handler.sendEmptyMessage(1);
        }
    }

    public void a(qc qcVar) {
        Object -l_5_R;
        int -l_6_I;
        int -l_7_I;
        Object -l_2_R = null;
        Object -l_3_R = null;
        if (qcVar != null) {
            try {
                -l_5_R = qcVar.im();
                if (-l_5_R != null) {
                    -l_6_I = 0;
                    for (Object -l_10_R : qF) {
                        if (-l_10_R.equalsIgnoreCase(-l_5_R)) {
                            -l_6_I = 1;
                            break;
                        }
                    }
                    if (-l_6_I == 0) {
                        -l_2_R = new String[]{-l_5_R};
                    }
                }
                -l_5_R = qcVar.in();
                if (-l_5_R != null) {
                    -l_6_I = 0;
                    for (Object -l_10_R2 : qG) {
                        if (-l_10_R2.equalsIgnoreCase(-l_5_R)) {
                            -l_6_I = 1;
                            break;
                        }
                    }
                    if (-l_6_I == 0) {
                        -l_3_R = new String[]{-l_5_R};
                    }
                }
            } catch (Object -l_4_R) {
                f.b("MessageReceiver", "register", -l_4_R);
            }
        } else if (!this.qz) {
            this.qz = true;
            -l_2_R = qF;
            -l_3_R = qG;
        } else {
            return;
        }
        if (-l_2_R != null) {
            -l_5_R = -l_2_R;
            -l_6_I = -l_2_R.length;
            for (-l_7_I = 0; -l_7_I < -l_6_I; -l_7_I++) {
                Object -l_4_R2 = new IntentFilter(-l_5_R[-l_7_I]);
                -l_4_R2.addCategory("android.intent.category.DEFAULT");
                -l_4_R2.setPriority(Integer.MAX_VALUE);
                this.mContext.registerReceiver(this, -l_4_R2, "android.permission.BROADCAST_SMS", null);
            }
        }
        if (-l_3_R != null) {
            -l_5_R = -l_3_R;
            -l_6_I = -l_3_R.length;
            for (-l_7_I = 0; -l_7_I < -l_6_I; -l_7_I++) {
                -l_4_R2 = new IntentFilter(-l_5_R[-l_7_I]);
                -l_4_R2.addDataType("application/vnd.wap.sic");
                -l_4_R2.addDataType("application/vnd.wap.slc");
                -l_4_R2.addDataType("application/vnd.wap.coc");
                -l_4_R2.addCategory("android.intent.category.DEFAULT");
                -l_4_R2.setPriority(Integer.MAX_VALUE);
                this.mContext.registerReceiver(this, -l_4_R2, "android.permission.BROADCAST_SMS", null);
            }
        }
    }

    public boolean bw() {
        return this.qz;
    }

    public void doOnRecv(Context context, Intent intent) {
        a(context, intent, null);
    }

    public void unregister() {
        Object -l_1_R = TMSDKContext.getApplicaionContext();
        if (this.qz) {
            -l_1_R.unregisterReceiver(this);
            this.qz = false;
        }
    }
}
