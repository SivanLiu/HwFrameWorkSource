package tmsdkobf;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.utils.u;

public class np implements tmsdkobf.nx.a {
    private static np DM = null;
    private static Object mLock = new Object();
    private int DN;
    private long DO;
    private boolean DP;
    private long DQ;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private class a extends Handler {
        final /* synthetic */ np DR;

        public a(np npVar, Looper looper) {
            this.DR = npVar;
            super(looper);
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.DR.fV();
                    return;
                default:
                    return;
            }
        }
    }

    private np() {
        this.DN = -6;
        this.DO = 0;
        this.DP = false;
        this.DQ = 0;
        this.mHandlerThread = null;
        this.mHandler = null;
        this.mHandlerThread = ((ki) fj.D(4)).newFreeHandlerThread("Shark-Network-Detect-HandlerThread");
        this.mHandlerThread.start();
        this.mHandler = new a(this, this.mHandlerThread.getLooper());
        mb.n("NetworkDetector", "[detect_conn]init, register & start detect");
        nx.gs().a((tmsdkobf.nx.a) this);
        this.mHandler.sendEmptyMessageDelayed(1, 5000);
    }

    public static String bn(int i) {
        return "" + i;
    }

    public static np fS() {
        np npVar;
        synchronized (mLock) {
            if (DM == null) {
                DM = new np();
            }
            npVar = DM;
        }
        return npVar;
    }

    private boolean fU() {
        Object -l_1_R = null;
        try {
            -l_1_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
        } catch (Object -l_2_R) {
            mb.s("NetworkDetector", " NullPointerException: " + -l_2_R.getMessage());
        }
        return -l_1_R == null || !-l_1_R.isConnected();
    }

    private boolean fV() {
        int -l_2_I = 0;
        mb.n("NetworkDetector", "[detect_conn]detectSync()");
        this.DP = true;
        Object -l_1_R = null;
        try {
            -l_1_R = u.a(new tmsdk.common.utils.u.a(this) {
                final /* synthetic */ np DR;

                {
                    this.DR = r1;
                }

                public void d(boolean z, boolean z2) {
                    mb.n("NetworkDetector", "[detect_conn]detectSync(), network error? " + z2);
                    if (z2) {
                        this.DR.DN = -3;
                    } else if (z) {
                        this.DR.DN = -2;
                    } else {
                        this.DR.DN = 0;
                    }
                }
            });
        } catch (Object -l_2_R) {
            this.DN = -3;
            mb.o("NetworkDetector", "[detect_conn]detectSync(), exception: " + -l_2_R.toString());
        }
        this.DP = false;
        this.DQ = System.currentTimeMillis();
        if (!TextUtils.isEmpty(-l_1_R)) {
            -l_2_I = 1;
        }
        mb.n("NetworkDetector", "[detect_conn]detectSync(),  isNeed wifi approve? " + -l_2_I + " url: " + -l_1_R + " state: " + bn(this.DN));
        return -l_2_I;
    }

    public int c(boolean z, boolean z2) {
        int i = 0;
        if (fU()) {
            this.DN = -1;
        } else {
            int -l_3_I;
            if ((this.DQ <= 0 ? 1 : 0) == 0) {
                if ((Math.abs(System.currentTimeMillis() - this.DQ) > 300000 ? 1 : 0) == 0) {
                    -l_3_I = 1;
                    if (z) {
                        if (z2 && -l_3_I == 0) {
                            if (Math.abs(System.currentTimeMillis() - this.DQ) <= 60000) {
                                i = 1;
                            }
                            if (i == 0) {
                                this.mHandler.removeMessages(1);
                                this.mHandler.sendEmptyMessage(1);
                            }
                        }
                        if (this.DN == 0 && -l_3_I == 0) {
                            this.DN = -5;
                        }
                    } else {
                        fV();
                    }
                }
            }
            -l_3_I = 0;
            if (z) {
                fV();
            } else {
                if (Math.abs(System.currentTimeMillis() - this.DQ) <= 60000) {
                    i = 1;
                }
                if (i == 0) {
                    this.mHandler.removeMessages(1);
                    this.mHandler.sendEmptyMessage(1);
                }
                this.DN = -5;
            }
        }
        mb.n("NetworkDetector", "[detect_conn]getNetworkState(), mNetworkState: " + bn(this.DN));
        return this.DN;
    }

    public void fT() {
        mb.n("NetworkDetector", "[detect_conn] onNetworkingchanging");
        this.DN = -4;
        this.DO = System.currentTimeMillis();
    }

    public void onConnected() {
        int -l_1_I = 0;
        fT();
        if ((this.DQ <= 0 ? 1 : 0) == 0) {
            if ((Math.abs(System.currentTimeMillis() - this.DQ) >= 60000 ? 1 : 0) == 0) {
                -l_1_I = 1;
            }
        }
        if (-l_1_I == 0 && !this.DP) {
            mb.n("NetworkDetector", "[detect_conn]onConnected(), trigger detect in 5s");
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessageDelayed(1, 5000);
            return;
        }
        mb.n("NetworkDetector", "[detect_conn]onConnected(), trigger detect in 60000");
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 60000);
    }

    public void onDisconnected() {
        mb.n("NetworkDetector", "[detect_conn]onDisconnected()");
        fT();
        this.mHandler.removeMessages(1);
        this.DN = -1;
    }

    public boolean x(long j) {
        if (this.DN == -4) {
            if (!(Math.abs(System.currentTimeMillis() - this.DO) >= j)) {
                return true;
            }
        }
        return false;
    }
}
