package tmsdkobf;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import tmsdkobf.nw.f;

public class ng {
    private static String TAG = "HttpNetworkManager";
    private nl CT;
    private om CU;
    private boolean CX = false;
    private int CY = 0;
    private LinkedList<a> CZ = new LinkedList();
    private Context mContext;
    private Handler mHandler = new Handler(this, nu.getLooper()) {
        final /* synthetic */ ng Da;

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    synchronized (this.Da.mLock) {
                        if (this.Da.CY >= 4) {
                            mb.s(ng.TAG, "[http_control]handleMessage(), not allow start, running tasks(>=4): " + this.Da.CY);
                        } else {
                            a -l_2_R = (a) this.Da.CZ.poll();
                            if (-l_2_R == null) {
                                mb.d(ng.TAG, "[http_control]handleMessage(), allow start but no data to send, running tasks: " + this.Da.CY);
                            } else {
                                mb.n(ng.TAG, "[http_control]handleMessage(), allow start, running tasks: " + this.Da.CY);
                                this.Da.CY = this.Da.CY + 1;
                                this.Da.b(-l_2_R.Dh, -l_2_R.data, -l_2_R.Di);
                            }
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private final Object mLock = new Object();

    private class a {
        final /* synthetic */ ng Da;
        public f Dh = null;
        public tmsdkobf.nf.a Di = null;
        public byte[] data = null;

        public a(ng ngVar, byte[] bArr, f fVar, tmsdkobf.nf.a aVar) {
            this.Da = ngVar;
            this.data = bArr;
            this.Dh = fVar;
            this.Di = aVar;
        }
    }

    public ng(Context context, nl nlVar, om omVar, boolean z) {
        this.mContext = context;
        this.CT = nlVar;
        this.CU = omVar;
        this.CX = z;
    }

    private void b(final f fVar, final byte[] bArr, final tmsdkobf.nf.a aVar) {
        Object -l_4_R = new Runnable(this) {
            final /* synthetic */ ng Da;

            public void run() {
                int -l_1_I;
                Object -l_2_R = new AtomicReference();
                try {
                    -l_1_I = new nf(this.Da.mContext, this.Da.CT, this.Da.CU, this.Da.CX).a(fVar, bArr, -l_2_R);
                } catch (Object -l_3_R) {
                    mb.c(ng.TAG, "sendDataAsyn(), exception:", -l_3_R);
                    -l_1_I = -1200;
                }
                final int -l_3_I = -l_1_I;
                final byte[] -l_4_R = (byte[]) -l_2_R.get();
                Object -l_5_R = new Runnable(this) {
                    final /* synthetic */ AnonymousClass2 Dg;

                    public void run() {
                        if (aVar != null) {
                            aVar.b(-l_3_I, -l_4_R);
                        }
                    }
                };
                ki -l_6_R = (ki) fj.D(4);
                if (nu.aC()) {
                    -l_6_R.a(-l_5_R, "shark-http-callback");
                } else {
                    -l_6_R.addTask(-l_5_R, "shark-http-callback");
                }
                synchronized (this.Da.mLock) {
                    this.Da.CY = this.Da.CY - 1;
                    if (this.Da.CZ.size() > 0) {
                        this.Da.mHandler.sendEmptyMessage(1);
                    }
                    mb.d(ng.TAG, "[http_control]-------- send finish, running tasks: " + this.Da.CY + ", waiting tasks: " + this.Da.CZ.size());
                }
            }
        };
        ki -l_5_R = (ki) fj.D(4);
        if (nu.aC()) {
            -l_5_R.a(-l_4_R, "shark-http-send");
        } else {
            -l_5_R.addTask(-l_4_R, "shark-http-send");
        }
    }

    public void a(f fVar, byte[] bArr, tmsdkobf.nf.a aVar) {
        synchronized (this.mLock) {
            this.CZ.add(new a(this, bArr, fVar, aVar));
            mb.r(TAG, "[http_control]sendDataAsyn(), waiting tasks: " + this.CZ.size());
        }
        this.mHandler.sendEmptyMessage(1);
    }
}
