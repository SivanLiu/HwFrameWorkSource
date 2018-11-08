package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Build.VERSION;
import android.os.PowerManager;
import java.util.LinkedList;
import tmsdk.common.TMSDKContext;
import tmsdk.common.utils.f;
import tmsdk.common.utils.q;

public class oo extends if {
    private static oo IA;
    private static Object lock = new Object();
    private State Iy = State.DISCONNECTED;
    private LinkedList<a> Iz = new LinkedList();
    private boolean xF;

    public interface a {
        void dC();

        void dD();
    }

    private oo() {
    }

    public static oo A(Context context) {
        if (IA == null) {
            synchronized (lock) {
                if (IA == null) {
                    if (context != null) {
                        IA = new oo();
                        IA.init(context);
                    } else {
                        return null;
                    }
                }
            }
        }
        return IA;
    }

    private synchronized void B(Context context) {
        if (!this.xF) {
            Object -l_3_R;
            try {
                -l_3_R = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
                if (-l_3_R == null) {
                    this.Iy = State.DISCONNECTED;
                } else {
                    this.Iy = -l_3_R.getState();
                }
            } catch (Object -l_2_R) {
                -l_2_R.printStackTrace();
            }
            Object -l_2_R2 = new IntentFilter();
            -l_2_R2.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            -l_2_R2.setPriority(Integer.MAX_VALUE);
            try {
                context.registerReceiver(this, -l_2_R2);
                this.xF = true;
            } catch (Object -l_3_R2) {
                f.e("NetworkBroadcastReceiver", -l_3_R2);
            }
        }
    }

    private void init(Context context) {
        B(context);
    }

    public static boolean isScreenOn() {
        Object -l_0_R = q.cI(im.bQ());
        if (!"888748".equals(-l_0_R) && !"799005".equals(-l_0_R)) {
            return true;
        }
        int -l_2_I = 1;
        try {
            PowerManager -l_3_R = (PowerManager) TMSDKContext.getApplicaionContext().getSystemService("power");
            if (VERSION.SDK_INT < 20) {
                -l_2_I = ((Boolean) PowerManager.class.getMethod("isScreenOn", new Class[0]).invoke(-l_3_R, new Object[0])).booleanValue();
            } else {
                -l_2_I = ((Boolean) PowerManager.class.getMethod("isInteractive", new Class[0]).invoke(-l_3_R, new Object[0])).booleanValue();
            }
        } catch (Exception e) {
        }
        return -l_2_I;
    }

    public void a(a aVar) {
        synchronized (this.Iz) {
            this.Iz.add(aVar);
        }
    }

    public void b(a aVar) {
        synchronized (this.Iz) {
            this.Iz.remove(aVar);
        }
    }

    public void doOnRecv(Context context, Intent intent) {
        Object -l_3_R = intent.getAction();
        Object -l_4_R = intent.getExtras();
        f.f("NetworkBroadcastReceiver", -l_3_R);
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(-l_3_R)) {
            State -l_6_R = ((NetworkInfo) -l_4_R.getParcelable("networkInfo")).getState();
            if (-l_6_R != State.CONNECTED) {
                if (-l_6_R != State.DISCONNECTED) {
                    return;
                }
                if (this.Iy.compareTo(State.CONNECTED) == 0) {
                    im.bJ().a(new Runnable(this) {
                        final /* synthetic */ oo IB;

                        {
                            this.IB = r1;
                        }

                        public void run() {
                            synchronized (this.IB.Iz) {
                                LinkedList -l_1_R = (LinkedList) this.IB.Iz.clone();
                            }
                            if (-l_1_R != null) {
                                Object -l_2_R = -l_1_R.iterator();
                                while (-l_2_R.hasNext()) {
                                    ((a) -l_2_R.next()).dD();
                                }
                            }
                        }
                    }, "monitor_toDisconnected");
                }
            } else if (this.Iy.compareTo(State.DISCONNECTED) == 0 && isScreenOn()) {
                im.bJ().a(new Runnable(this) {
                    final /* synthetic */ oo IB;

                    {
                        this.IB = r1;
                    }

                    public void run() {
                        LinkedList -l_1_R;
                        synchronized (this.IB.Iz) {
                            -l_1_R = (LinkedList) this.IB.Iz.clone();
                        }
                        f.f("NetworkBroadcastReceiver", "copy != null ? " + (-l_1_R != null));
                        if (-l_1_R != null) {
                            f.f("NetworkBroadcastReceiver", "copy.size() : " + -l_1_R.size());
                            Object -l_2_R = -l_1_R.iterator();
                            while (-l_2_R.hasNext()) {
                                ((a) -l_2_R.next()).dC();
                            }
                        }
                    }
                }, "monitor_toConnected");
            }
            this.Iy = -l_6_R;
        }
    }
}
