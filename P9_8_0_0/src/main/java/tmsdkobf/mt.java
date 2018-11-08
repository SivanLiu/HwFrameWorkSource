package tmsdkobf;

import com.qq.taf.jce.JceStruct;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import tmsdk.common.TMSDKContext;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;
import tmsdkobf.kx.a;

public class mt {
    private Object Ba = new Object();
    private mr<String> Bb;
    private mr<String> Bc;
    private mr<String> Bd = new mr(5);
    private mr<String> Be = new mr(5);
    private mr<String> Bf = new mr(5);
    private mr<String> Bg = new mr(5);
    private mr<String> Bh = new mr(5);
    private mr<String> Bi = new mr(5);
    private a Bj = new a(this) {
        final /* synthetic */ mt Bk;

        {
            this.Bk = r1;
        }

        public void dT() {
            this.Bk.ff();
        }
    };
    private volatile boolean xT = false;

    private void a(final aq aqVar, final CountDownLatch countDownLatch) {
        if (aqVar != null) {
            im.bK().a(3122, aqVar, null, 0, new jy(this) {
                final /* synthetic */ mt Bk;

                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    switch (ne.bg(i3)) {
                        case 0:
                            synchronized (this.Bk.Ba) {
                                this.Bk.Bd.removeAll((Collection) aqVar.bI.get(Integer.valueOf(1)));
                                this.Bk.Be.removeAll((Collection) aqVar.bI.get(Integer.valueOf(2)));
                                this.Bk.Bf.removeAll((Collection) aqVar.bI.get(Integer.valueOf(3)));
                                this.Bk.Bg.removeAll((Collection) aqVar.bI.get(Integer.valueOf(4)));
                                this.Bk.Bh.removeAll((Collection) aqVar.bI.get(Integer.valueOf(5)));
                                this.Bk.Bi.removeAll((Collection) aqVar.bI.get(Integer.valueOf(6)));
                            }
                            break;
                    }
                    countDownLatch.countDown();
                }
            }, 5000);
            return;
        }
        countDownLatch.countDown();
    }

    private void a(mr<String> mrVar, boolean z) {
        int -l_3_I;
        Object -l_4_R;
        if (z) {
            -l_3_I = this.Bb.size();
            -l_4_R = this.Bb.fc();
        } else {
            -l_3_I = this.Bc.size();
            -l_4_R = this.Bc.fc();
            if (-l_3_I == 0) {
                -l_3_I = this.Bb.size();
                -l_4_R = this.Bb.fc();
            }
        }
        if (-l_3_I != 0) {
            Object -l_5_R = new StringBuilder();
            if (-l_3_I <= 40) {
                for (String -l_7_R : -l_4_R) {
                    -l_5_R.append(-l_7_R + "``");
                }
            } else {
                int -l_6_I = 0;
                int -l_7_I = -l_3_I - 20;
                for (String -l_9_R : -l_4_R) {
                    if (-l_6_I < 20) {
                        -l_5_R.append(-l_9_R + "``");
                    }
                    if (-l_6_I > -l_7_I) {
                        -l_5_R.append(-l_9_R + "``");
                    }
                    -l_6_I++;
                }
            }
            if (z) {
                this.Bb.clear();
            } else {
                this.Bc.clear();
            }
            mrVar.offer(-l_5_R.toString());
        }
    }

    private void ff() {
        if (gf.S().aa().booleanValue() && this.Bf.size() != 0 && i.K(TMSDKContext.getApplicaionContext()) && !this.xT) {
            this.xT = true;
            im.bJ().addTask(new Runnable(this) {
                final /* synthetic */ mt Bk;

                {
                    this.Bk = r1;
                }

                public void run() {
                    f.h("QQPimSecure", "OptimusReport sendOptimusData 01");
                    Object -l_1_R = this.Bk.fg();
                    if (-l_1_R != null) {
                        Object -l_2_R = new CountDownLatch(1);
                        this.Bk.a((aq) -l_1_R, (CountDownLatch) -l_2_R);
                        try {
                            -l_2_R.await(5000, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            f.h("QQPimSecure", "OptimusReport sendOptimusData InterruptedException!");
                        }
                    }
                    f.h("QQPimSecure", "OptimusReport sendOptimusData 10");
                    this.Bk.xT = false;
                }
            }, "OptimusReport");
        }
    }

    private aq fg() {
        if (this.Bf.size() == 0) {
            return null;
        }
        Object -l_1_R = new aq();
        -l_1_R.bC = 26;
        -l_1_R.bI = new HashMap();
        synchronized (this.Ba) {
            -l_1_R.bI.put(Integer.valueOf(1), this.Bd.fd());
            -l_1_R.bI.put(Integer.valueOf(2), this.Be.fd());
            -l_1_R.bI.put(Integer.valueOf(3), this.Bf.fd());
            -l_1_R.bI.put(Integer.valueOf(4), this.Bg.fd());
            -l_1_R.bI.put(Integer.valueOf(5), this.Bh.fd());
            -l_1_R.bI.put(Integer.valueOf(6), this.Bi.fd());
        }
        return -l_1_R;
    }

    public synchronized void a(String str, String str2, String str3, String str4, String str5, boolean z, boolean z2) {
        synchronized (this.Ba) {
            a(this.Bi, z);
            this.Bd.offer(str);
            this.Be.offer(str2);
            this.Bf.offer(str3);
            this.Bg.offer(str4);
            this.Bh.offer(str5);
        }
        if (z2) {
            ff();
        }
    }

    public void bZ(String str) {
        Object -l_2_R = new Date();
        Object -l_4_R = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(-l_2_R) + "``" + str).replace("\n", "``").replace("|", ":");
        synchronized (this.Ba) {
            if (this.Bb == null) {
                this.Bb = new mr(80);
                this.Bc = new mr(80);
            }
            this.Bb.offer(-l_4_R);
        }
    }

    public void destroy() {
        kx.dR().b(this.Bj);
    }

    public void fe() {
        synchronized (this.Ba) {
            if (this.Bb != null) {
                if (this.Bc == null) {
                    this.Bc = new mr(80);
                }
                this.Bc.clear();
                this.Bc.addAll(this.Bb.fc());
                return;
            }
        }
    }

    public void init() {
        kx.dR().a(this.Bj);
    }
}
