package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.os.Message;
import java.util.LinkedList;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.utils.f;

public class nx extends if {
    private static Object FG = new Object();
    private static nx FI = null;
    private boolean FA = false;
    private State FB = State.UNKNOWN;
    private String FC = null;
    private String FD = null;
    private LinkedList<a> FE = new LinkedList();
    private LinkedList<b> FF = new LinkedList();
    private Object FH = new Object();
    private long Fz = 0;
    private Handler mHandler = new Handler(this, nu.getLooper()) {
        final /* synthetic */ nx FJ;

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.FJ.fF();
                    return;
                default:
                    return;
            }
        }
    };

    public interface a {
        void onConnected();

        void onDisconnected();
    }

    public interface b {
        void gb();
    }

    private void e(Intent intent) {
        Object obj = 1;
        synchronized (this.FH) {
            Object -l_3_R;
            NetworkInfo -l_4_R;
            State -l_5_R;
            if ((this.Fz <= 0 ? 1 : null) == null) {
                if (System.currentTimeMillis() - this.Fz <= 2000) {
                    obj = null;
                }
                if (obj == null) {
                    mb.d("SharkNetworkReceiver", "[conn_monitor]doOnRecv(), ignore for just register: " + (System.currentTimeMillis() - this.Fz));
                    -l_3_R = intent.getExtras();
                    if (-l_3_R == null) {
                        -l_4_R = (NetworkInfo) -l_3_R.getParcelable("networkInfo");
                        if (-l_4_R == null) {
                            -l_5_R = -l_4_R.getState();
                            Object -l_6_R = -l_4_R.getTypeName();
                            Object -l_7_R = -l_4_R.getSubtypeName();
                            mb.n("SharkNetworkReceiver", "[conn_monitor]doOnRecv(), Sate: " + this.FB + " -> " + -l_5_R);
                            mb.n("SharkNetworkReceiver", "[conn_monitor]doOnRecv(), type: " + this.FC + " -> " + -l_6_R);
                            mb.n("SharkNetworkReceiver", "[conn_monitor]doOnRecv(), subType: " + this.FD + " -> " + -l_7_R);
                            if (-l_5_R != State.CONNECTED) {
                                if (-l_5_R == State.DISCONNECTED && this.FB != State.DISCONNECTED) {
                                    gt();
                                }
                            } else if (this.FB != State.CONNECTED) {
                                gu();
                            }
                            this.FB = -l_5_R;
                            this.FC = -l_6_R;
                            this.FD = -l_7_R;
                        }
                        return;
                    }
                    return;
                }
            }
            np.fS().fT();
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessageDelayed(1, 5000);
            -l_3_R = intent.getExtras();
            if (-l_3_R == null) {
                return;
            }
            -l_4_R = (NetworkInfo) -l_3_R.getParcelable("networkInfo");
            if (-l_4_R == null) {
                return;
            }
            -l_5_R = -l_4_R.getState();
            Object -l_6_R2 = -l_4_R.getTypeName();
            Object -l_7_R2 = -l_4_R.getSubtypeName();
            mb.n("SharkNetworkReceiver", "[conn_monitor]doOnRecv(), Sate: " + this.FB + " -> " + -l_5_R);
            mb.n("SharkNetworkReceiver", "[conn_monitor]doOnRecv(), type: " + this.FC + " -> " + -l_6_R2);
            mb.n("SharkNetworkReceiver", "[conn_monitor]doOnRecv(), subType: " + this.FD + " -> " + -l_7_R2);
            if (-l_5_R != State.CONNECTED) {
                gt();
            } else if (this.FB != State.CONNECTED) {
                gu();
            }
            this.FB = -l_5_R;
            this.FC = -l_6_R2;
            this.FD = -l_7_R2;
        }
    }

    private void eZ() {
        Object -l_1_R;
        try {
            -l_1_R = TMSDKContext.getApplicaionContext();
            if (-l_1_R != null) {
                z(-l_1_R);
            }
        } catch (Object -l_1_R2) {
            mb.o("SharkNetworkReceiver", "[conn_monitor]checkInit(), registerConnectivityIfNeed() failed: " + -l_1_R2);
        }
    }

    private void fF() {
        ((ki) fj.D(4)).a(new Runnable(this) {
            final /* synthetic */ nx FJ;

            {
                this.FJ = r1;
            }

            public void run() {
                LinkedList -l_1_R;
                mb.d("SharkNetworkReceiver", "[conn_monitor]handleNetworkChange()");
                synchronized (this.FJ.FF) {
                    -l_1_R = (LinkedList) this.FJ.FF.clone();
                }
                Object -l_2_R = -l_1_R.iterator();
                while (-l_2_R.hasNext()) {
                    b -l_3_R = (b) -l_2_R.next();
                    if (-l_3_R != null) {
                        -l_3_R.gb();
                    }
                }
            }
        }, "network_change");
    }

    public static nx gs() {
        if (FI == null) {
            synchronized (FG) {
                if (FI == null) {
                    FI = new nx();
                }
            }
        }
        FI.eZ();
        return FI;
    }

    private void gt() {
        ((ki) fj.D(4)).a(new Runnable(this) {
            final /* synthetic */ nx FJ;

            {
                this.FJ = r1;
            }

            public void run() {
                LinkedList -l_1_R;
                mb.d("SharkNetworkReceiver", "[conn_monitor]handleChange2DisConnected(), 有网络 -> 无网络");
                synchronized (this.FJ.FE) {
                    -l_1_R = (LinkedList) this.FJ.FE.clone();
                }
                Object -l_2_R = -l_1_R.iterator();
                while (-l_2_R.hasNext()) {
                    a -l_3_R = (a) -l_2_R.next();
                    if (-l_3_R != null) {
                        -l_3_R.onDisconnected();
                    }
                }
            }
        }, "network_disconnected");
    }

    private void gu() {
        ((ki) fj.D(4)).a(new Runnable(this) {
            final /* synthetic */ nx FJ;

            {
                this.FJ = r1;
            }

            public void run() {
                LinkedList -l_2_R;
                boolean z = false;
                mb.d("SharkNetworkReceiver", "[conn_monitor]handleChange2Connected(), 无网络 -> 有网络");
                Object -l_1_R = nj.fE();
                StringBuilder append = new StringBuilder().append("[conn_monitor][ip_list]handleChange2Connected(), notify hiplist first: ");
                String str = "SharkNetworkReceiver";
                if (-l_1_R != null) {
                    z = true;
                }
                mb.d(str, append.append(z).toString());
                if (-l_1_R != null) {
                    -l_1_R.fF();
                }
                synchronized (this.FJ.FE) {
                    -l_2_R = (LinkedList) this.FJ.FE.clone();
                }
                Object -l_3_R = -l_2_R.iterator();
                while (-l_3_R.hasNext()) {
                    a -l_4_R = (a) -l_3_R.next();
                    if (-l_4_R != null) {
                        -l_4_R.onConnected();
                    }
                }
            }
        }, "network_connected");
    }

    private synchronized void z(Context context) {
        Object -l_2_R;
        Object -l_2_R2;
        if (!this.FA) {
            try {
                -l_2_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
                if (-l_2_R == null) {
                    this.FB = State.DISCONNECTED;
                    mb.d("SharkNetworkReceiver", "[conn_monitor]registerConnectivRityIfNeed(), not got, set mLastState: " + this.FB);
                } else {
                    this.FB = -l_2_R.getState();
                    this.FC = -l_2_R.getTypeName();
                    this.FD = -l_2_R.getSubtypeName();
                    mb.d("SharkNetworkReceiver", "[conn_monitor]registerConnectivRityIfNeed(), got mLastState: " + this.FB);
                }
                -l_2_R2 = -l_2_R;
            } catch (Object -l_2_R3) {
                mb.o("SharkNetworkReceiver", "[conn_monitor]getActiveNetworkInfo() failed: " + -l_2_R3);
                -l_2_R2 = -l_2_R3;
            }
            try {
                -l_2_R3 = new IntentFilter();
                try {
                    -l_2_R3.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                    -l_2_R3.setPriority(Integer.MAX_VALUE);
                    context.registerReceiver(this, -l_2_R3);
                    this.Fz = System.currentTimeMillis();
                    this.FA = true;
                    mb.n("SharkNetworkReceiver", "[conn_monitor]registerConnectivityIfNeed() succ");
                } catch (Throwable th) {
                    -l_2_R2 = th;
                    mb.o("SharkNetworkReceiver", "[conn_monitor]registerConnectivityIfNeed() failed: " + -l_2_R2);
                }
            } catch (Throwable th2) {
                -l_2_R3 = -l_2_R2;
                Throwable -l_2_R4 = th2;
                mb.o("SharkNetworkReceiver", "[conn_monitor]registerConnectivityIfNeed() failed: " + -l_2_R2);
            }
        }
    }

    public void a(a aVar) {
        if (aVar != null) {
            synchronized (this.FE) {
                if (!this.FE.contains(aVar)) {
                    this.FE.add(aVar);
                }
            }
        }
    }

    public void a(b bVar) {
        if (bVar != null) {
            synchronized (this.FF) {
                if (!this.FF.contains(bVar)) {
                    this.FF.add(bVar);
                }
            }
        }
    }

    public void doOnRecv(Context context, final Intent -l_4_R) {
        if (-l_4_R != null && -l_4_R.getAction() != null) {
            Object -l_3_R = -l_4_R.getAction();
            f.d("SharkNetworkReceiver", "[conn_monitor]doOnRecv(), action: " + -l_3_R);
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(-l_3_R)) {
                this.mHandler.post(new Runnable(this) {
                    final /* synthetic */ nx FJ;

                    public void run() {
                        if (im.bG() && oo.isScreenOn()) {
                            this.FJ.e(-l_4_R);
                        }
                    }
                });
            }
        }
    }
}
