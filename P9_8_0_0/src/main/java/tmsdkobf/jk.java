package tmsdkobf;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.SparseArray;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import tmsdk.common.utils.f;
import tmsdkobf.ji.a;
import tmsdkobf.ji.b;

public class jk {
    private static Object lock = new Object();
    private static jk tf;
    Handler handler;
    private SparseArray<ji> td;
    HandlerThread te;

    private jk() {
        this.td = new SparseArray();
        this.te = null;
        this.handler = null;
        this.te = ((ki) fj.D(4)).newFreeHandlerThread("ProfileServiceManager");
        this.te.start();
        this.handler = new Handler(this, this.te.getLooper()) {
            final /* synthetic */ jk tg;

            public void handleMessage(Message message) {
                f.h("demo", "handler 000 : " + System.currentTimeMillis() + "  msg.what = " + message.what);
                final ji -l_3_R;
                switch (message.what) {
                    case 1:
                        jj -l_2_R = (jj) message.obj;
                        Object -l_3_R2 = -l_2_R.tc;
                        Object -l_4_R = -l_2_R.tb;
                        if (!(-l_3_R2 == null || -l_3_R2.size() <= 0 || -l_4_R == null)) {
                            -l_4_R.i(-l_3_R2);
                            break;
                        }
                    case 2:
                        Object -l_2_R2 = message.obj;
                        if (-l_2_R2 != null && (-l_2_R2 instanceof ji)) {
                            -l_3_R = (ji) -l_2_R2;
                            this.tg.td.remove(-l_3_R.cs());
                            this.tg.td.append(-l_3_R.cs(), -l_3_R);
                            -l_3_R.a(new gt(this) {
                                final /* synthetic */ AnonymousClass1 ti;

                                public void a(int i, ArrayList<JceStruct> arrayList, int i2, int i3) {
                                    if (i2 == 0) {
                                        -l_3_R.h(this.ti.tg.a(i, (ArrayList) arrayList));
                                        if (i == 0) {
                                            -l_3_R.ag(i3);
                                        }
                                    }
                                }
                            });
                            break;
                        }
                        return;
                        break;
                    case 3:
                        Integer -l_2_R3 = (Integer) message.obj;
                        if (-l_2_R3 != null) {
                            -l_3_R = (ji) this.tg.td.get(-l_2_R3.intValue());
                            if (-l_3_R != null) {
                                -l_3_R.ct();
                                break;
                            }
                            return;
                        }
                        return;
                    case 4:
                        this.tg.b((ji) this.tg.td.get(((Integer) message.obj).intValue()));
                        break;
                }
                f.h("demo", "handler 001 : " + System.currentTimeMillis());
            }
        };
    }

    private void b(final ji jiVar) {
        if (jiVar != null) {
            jiVar.a(new b(this) {
                final /* synthetic */ jk tg;

                public void j(ArrayList<a> arrayList) {
                    if (arrayList != null && arrayList.size() > 0) {
                        Message.obtain(this.tg.handler, 1, new jj(jiVar, arrayList)).sendToTarget();
                    }
                }
            });
        }
    }

    public static jk cv() {
        if (tf == null) {
            synchronized (lock) {
                if (tf == null) {
                    tf = new jk();
                }
            }
        }
        return tf;
    }

    protected ArrayList<a> a(int i, ArrayList<JceStruct> arrayList) {
        Object -l_3_R = new ArrayList();
        if (arrayList == null || arrayList.size() == 0) {
            return -l_3_R;
        }
        Object -l_4_R = arrayList.iterator();
        while (-l_4_R.hasNext()) {
            JceStruct -l_5_R = (JceStruct) -l_4_R.next();
            if (-l_5_R != null) {
                Object -l_6_R = new a();
                -l_6_R.ta = -l_5_R;
                -l_6_R.action = i;
                -l_3_R.add(-l_6_R);
            }
        }
        return -l_3_R;
    }

    public void a(ji jiVar) {
        if (jiVar != null && this.handler != null) {
            Message.obtain(this.handler, 2, jiVar).sendToTarget();
        }
    }

    public void ah(int i) {
        Message.obtain(this.handler, 4, Integer.valueOf(i)).sendToTarget();
    }
}
