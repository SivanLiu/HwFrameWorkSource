package tmsdkobf;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.utils.f;
import tmsdk.fg.module.cleanV2.IUpdateCallBack;

public class qw {
    private static qw OB;
    static final Object OI = new Object();
    private qn OC = new qn();
    IUpdateCallBack OD;
    private ArrayList<String> OE;
    private ArrayList<String> OF;
    jy OG = new jy(this) {
        final /* synthetic */ qw OJ;

        {
            this.OJ = r1;
        }

        public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
            f.f("ListNetService", "onFinish() seqNo: " + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4);
            if (i3 != 0) {
                synchronized (qw.OI) {
                    this.OJ.OF = null;
                    this.OJ.OE = null;
                    this.OJ.OH = false;
                }
                synchronized (this.OJ.mLock) {
                    if (this.OJ.OD != null) {
                        this.OJ.OD.updateEnd(0);
                        this.OJ.OD = null;
                    }
                }
                return;
            }
            f.f("ListNetService", "upload onFinish()");
            synchronized (this.OJ.mLock) {
                if (this.OJ.OD != null) {
                    this.OJ.OD.updateEnd(0);
                    this.OJ.OD = null;
                }
            }
            this.OJ.OC.K(System.currentTimeMillis());
            Object -l_6_R = new qx();
            Object -l_7_R = this.OJ.mHandler.obtainMessage();
            -l_6_R.Y = i2;
            -l_6_R.ey = i;
            -l_6_R.wL = jceStruct;
            -l_7_R.obj = -l_6_R;
            -l_7_R.what = 1;
            this.OJ.mHandler.sendMessage(-l_7_R);
            if (this.OJ.OE == null || this.OJ.OE.size() == 0) {
                this.OJ.OC.X(true);
                this.OJ.OH = false;
                return;
            }
            this.OJ.mHandler.sendEmptyMessage(2);
        }
    };
    private boolean OH = false;
    private Handler mHandler;
    private Object mLock = new Object();
    private ob mk = im.bK();
    private HandlerThread vG = im.bJ().newFreeHandlerThread("networkSharkThread");
    ka wb = new ka(this) {
        final /* synthetic */ qw OJ;

        {
            this.OJ = r1;
        }

        public oh<Long, Integer, JceStruct> a(int i, long j, int i2, JceStruct jceStruct) {
            switch (i2) {
                case 13652:
                    synchronized (this.OJ.mLock) {
                        if (this.OJ.OD != null) {
                            this.OJ.OD.updateEnd(0);
                            this.OJ.OD = null;
                        }
                        if (jceStruct != null) {
                            f.f("ListNetService", "listener push");
                            this.OJ.OC.J(System.currentTimeMillis());
                            Object -l_7_R = new qx();
                            Object -l_8_R = this.OJ.mHandler.obtainMessage();
                            -l_7_R.Y = i2;
                            -l_7_R.ey = i;
                            -l_7_R.ex = j;
                            -l_7_R.wL = jceStruct;
                            -l_8_R.obj = -l_7_R;
                            -l_8_R.what = 1;
                            this.OJ.mHandler.sendMessage(-l_8_R);
                            break;
                        }
                        f.f("ListNetService", "push == null");
                        return null;
                    }
            }
            return null;
        }
    };

    private qw() {
        this.vG.start();
        this.mHandler = new Handler(this, this.vG.getLooper()) {
            final /* synthetic */ qw OJ;

            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        if (message.obj != null) {
                            qx -l_2_R = (qx) message.obj;
                            ah -l_3_R = (ah) -l_2_R.wL;
                            f.f("ListNetService", "收到云端push");
                            f.f("ListNetService", "onRecvPush + info.seqNo :" + -l_2_R.ey + "  info.pushId :" + -l_2_R.ex);
                            f.f("ListNetService", "size:" + -l_3_R.aU.size());
                            Object<String> -l_4_R = this.OJ.D(-l_3_R.aU);
                            if (!this.OJ.OC.jv()) {
                                this.OJ.OC.W(true);
                            }
                            Object -l_5_R = new af();
                            -l_5_R.aQ = new ArrayList();
                            for (String -l_8_R : -l_4_R) {
                                Object -l_6_R = new ag();
                                -l_6_R.fileName = -l_8_R;
                                -l_6_R.aT = 0;
                                -l_5_R.aQ.add(-l_6_R);
                            }
                            this.OJ.mk.b(-l_2_R.ey, -l_2_R.ex, -l_2_R.Y, -l_5_R);
                            return;
                        }
                        return;
                    case 2:
                        this.OJ.jO();
                        return;
                    case 3:
                        this.OJ.OE = this.OJ.jQ();
                        this.OJ.jN();
                        return;
                    case 4:
                        Object -l_2_R2 = js.cE().m(js.cE().cG());
                        if (-l_2_R2.size() >= 1) {
                            js.cE().i(-l_2_R2);
                        } else {
                            f.f("ListNetService", "it doesn't need update");
                            this.OJ.OE = this.OJ.jQ();
                            this.OJ.jN();
                        }
                        synchronized (this.OJ.mLock) {
                            if (this.OJ.OD != null) {
                                this.OJ.OD.updateEnd(0);
                                this.OJ.OD = null;
                            }
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    private List<String> D(List<am> list) {
        return qo.jz().y(list);
    }

    private ArrayList<String> a(String str, File -l_5_R, int i) {
        if (3 == i) {
            return null;
        }
        Object -l_4_R = new ArrayList();
        Object -l_6_R = -l_5_R.listFiles();
        if (-l_6_R == null) {
            return null;
        }
        Object -l_7_R = -l_6_R;
        for (Object -l_10_R : -l_6_R) {
            if (-l_10_R.isDirectory()) {
                -l_4_R.add(-l_10_R.getAbsolutePath().substring(str.length()).toLowerCase());
                Object -l_11_R = a(str, -l_10_R, i + 1);
                if (-l_11_R != null) {
                    -l_4_R.addAll(-l_11_R);
                }
            }
        }
        return -l_4_R;
    }

    public static qw jM() {
        if (OB == null) {
            OB = new qw();
        }
        return OB;
    }

    private void jN() {
        if (!this.OH) {
            this.OH = true;
            this.mHandler.sendEmptyMessage(2);
        }
    }

    private void jO() {
        this.OF = null;
        if (this.OE != null) {
            int -l_1_I = this.OE.size();
            f.f("ListNetService", "report sd card !!: ");
            if (-l_1_I > 50) {
                this.OF = new ArrayList();
                for (int -l_2_I = 0; -l_2_I < 50; -l_2_I++) {
                    this.OF.add(this.OE.get(-l_2_I));
                }
                this.OE.removeAll(this.OF);
            } else if (-l_1_I <= 0) {
                this.OH = false;
                return;
            } else {
                this.OF = (ArrayList) this.OE.clone();
                this.OE.removeAll(this.OF);
            }
        }
        synchronized (OI) {
            if (this.OF == null) {
            } else if (this.OF.size() >= 1) {
                JceStruct -l_1_R = new ac();
                -l_1_R.aC = this.OF;
                this.mk.a(3652, -l_1_R, new ah(), 0, this.OG);
            }
        }
    }

    private ArrayList<String> jP() {
        if (Environment.getExternalStorageState() == "unmounted") {
            return null;
        }
        Object -l_1_R = Environment.getExternalStorageDirectory();
        Object -l_2_R = -l_1_R.getAbsolutePath();
        Object -l_3_R = -l_1_R.listFiles();
        Object -l_4_R = new ArrayList();
        if (-l_3_R == null) {
            return null;
        }
        Object -l_5_R = -l_3_R;
        for (Object -l_8_R : -l_3_R) {
            if (-l_8_R.isDirectory()) {
                -l_4_R.add(-l_8_R.getAbsolutePath().substring(-l_2_R.length()).toLowerCase());
                Object -l_9_R = a(-l_2_R, -l_8_R, 1);
                if (-l_9_R != null) {
                    -l_4_R.addAll(-l_9_R);
                }
            }
        }
        return -l_4_R;
    }

    private ArrayList<String> jQ() {
        f.f("ListNetService", "get sd card !!: : ");
        return jP();
    }

    public synchronized boolean b(IUpdateCallBack iUpdateCallBack) {
        this.OD = iUpdateCallBack;
        if (qo.jz().jC()) {
            long -l_6_J = System.currentTimeMillis() - this.OC.jx();
            if ((System.currentTimeMillis() - this.OC.jy() <= 604800000 ? 1 : null) == null) {
                this.OC.X(false);
            }
            if ((-l_6_J <= 604800000 ? 1 : null) == null) {
                js.cE().cF();
            }
            if (0 == this.OC.jy() && 0 == this.OC.jx()) {
                this.OC.W(false);
            }
        } else {
            this.OC.X(false);
            js.cE().cF();
            this.OC.W(false);
        }
        this.mHandler.sendEmptyMessage(4);
        if (!this.OC.jw()) {
            this.mHandler.sendEmptyMessage(3);
        }
        kr.dz();
        f.f("ListNetService", "uploadSoftwareAndDirs guid:" + this.mk.b());
        return true;
    }

    public synchronized void de() {
        if (this.mk == null) {
            this.mk = im.bK();
        }
        if (this.mk == null) {
            try {
                throw new Exception("registerSharkePush failed");
            } catch (Object -l_1_R) {
                -l_1_R.printStackTrace();
            }
        } else {
            this.mk.v(13652, 2);
            this.mk.a(13652, new ah(), 2, this.wb);
        }
        return;
    }
}
