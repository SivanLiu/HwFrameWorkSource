package tmsdkobf;

import android.content.Context;
import android.net.NetworkInfo;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdkobf.nw.f;
import tmsdkobf.on.b;

public class oq {
    protected om CU;
    private byte ID;
    private boolean IE;
    private String IF;
    private volatile boolean IG;
    private Thread IH;
    private final Object II;
    private Socket IJ;
    private DataOutputStream IK;
    private DataInputStream IL;
    private a IM;
    private boolean IN;
    private Context mContext;

    public interface a {
        void a(int i, Object obj);

        void bE(int i);

        void d(int i, byte[] bArr);
    }

    public oq(Context context, byte b, boolean z, a aVar, om omVar) {
        this.ID = (byte) 0;
        this.IE = true;
        this.IF = "";
        this.IG = true;
        this.II = new Object();
        this.IN = false;
        this.mContext = context;
        this.ID = (byte) b;
        this.IE = z;
        this.IM = aVar;
        this.CU = omVar;
    }

    public oq(Context context, a aVar, om omVar) {
        this(context, (byte) 0, false, aVar, omVar);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized int a(Context context, boolean z) {
        mb.d("TcpNetwork", "[tcp_control]start() isRestart " + z);
        if (isStarted()) {
            mb.d("TcpNetwork", "start() already started");
            return 0;
        } else if (hm()) {
            if (this.IM != null) {
                this.IM.bE(3);
            }
            int -l_3_I = a(this.CU);
            if (-l_3_I == 0) {
                this.IG = false;
                if (this.ID == (byte) 0) {
                    mb.d("TcpNetwork", "[tcp_control]connect succ, startRcvThread()");
                    hh();
                }
                if (this.IM != null) {
                    if (z) {
                        this.IM.bE(5);
                    } else {
                        this.IM.bE(4);
                    }
                }
            } else {
                mb.s("TcpNetwork", "[tcp_control]connect failed, donot startRcvThread()");
                return -l_3_I;
            }
        } else {
            mb.d("TcpNetwork", "start(), no connect");
            return -220000;
        }
    }

    private int a(om omVar) {
        mb.d("TcpNetwork", "[tcp_control] checkSocketWithRetry()");
        long -l_2_J = System.currentTimeMillis();
        int -l_4_I = 0;
        omVar.D(true);
        int -l_5_I = omVar.G(true);
        b -l_6_R = null;
        long -l_7_J = 0;
        int -l_9_I = 0;
        while (-l_9_I < -l_5_I) {
            -l_6_R = omVar.B(true);
            if (-l_6_R != null) {
                long -l_10_J = System.currentTimeMillis();
                -l_4_I = b(-l_6_R);
                -l_7_J = System.currentTimeMillis() - -l_10_J;
                mb.n("TcpNetwork", "checkSocketWithRetry(), ipPoint " + -l_6_R.toString() + " localIp " + ho() + " localPort " + hp() + " ret: " + -l_4_I);
                if (-l_4_I == 0 || !ne.bk(-l_4_I)) {
                    break;
                } else if (-l_9_I == 0 && nu.ch("tcp connect")) {
                    -l_4_I = -160000;
                    break;
                } else {
                    omVar.C(true);
                }
            }
            -l_9_I++;
        }
        omVar.E(-l_4_I == 0);
        if (-l_6_R != null) {
            final Object -l_10_R = new oe();
            -l_10_R.HB = -l_6_R.hd();
            -l_10_R.HC = String.valueOf(-l_6_R.getPort());
            -l_10_R.HE = String.valueOf(nh.w(this.mContext));
            -l_10_R.HG = -l_7_J;
            -l_10_R.errorCode = -l_4_I;
            -l_10_R.HH = this.IF;
            -l_10_R.HD = -l_9_I >= -l_5_I ? -l_5_I : -l_9_I + 1;
            -l_10_R.u(omVar.F(true));
            if ((-l_9_I != -l_5_I ? 0 : 1) == 0) {
                -l_10_R.HL = false;
                -l_10_R.HI = "false";
                nz -l_12_R = (nz) ManagerCreatorC.getManager(nz.class);
                if (-l_12_R != null) {
                    -l_10_R.d(-l_12_R.gl());
                }
            } else {
                ((ki) fj.D(4)).addTask(new Runnable(this) {
                    final /* synthetic */ oq IO;

                    public void run() {
                        -l_10_R.HL = true;
                        -l_10_R.HM = nu.ci("tcp connect");
                        -l_10_R.HI = "true";
                        nz -l_1_R = (nz) ManagerCreatorC.getManager(nz.class);
                        if (-l_1_R != null) {
                            -l_10_R.d(-l_1_R.gl());
                        }
                    }
                }, "uploadConnectInfo");
            }
        }
        mb.n("TcpNetwork", "[tcp_control] checkSocketWithRetry(), ret: " + -l_4_I + " time: " + (System.currentTimeMillis() - -l_2_J));
        return -l_4_I;
    }

    private Socket a(InetAddress inetAddress, int i) throws IOException {
        mb.n("TcpNetwork", "acquireSocketWithTimeOut, addr: " + inetAddress + ", port: " + i);
        Object -l_3_R = new Socket();
        -l_3_R.setSoLinger(false, 0);
        -l_3_R.connect(new InetSocketAddress(inetAddress, i), 15000);
        mb.n("TcpNetwork", "acquireSocketWithTimeOut end");
        return -l_3_R;
    }

    private boolean a(b bVar) throws IOException {
        mb.d("TcpNetwork", "[tcp_control]startSocket()");
        if (!hk()) {
            mb.n("TcpNetwork", "startSocket() 1");
            hj();
        }
        mb.n("TcpNetwork", "startSocket() 2");
        InetAddress -l_2_R = InetAddress.getByName(bVar.hd());
        mb.n("TcpNetwork", "startSocket() 3");
        this.IJ = a(-l_2_R, bVar.getPort());
        mb.n("TcpNetwork", "startSocket() 4");
        switch (this.ID) {
            case (byte) 0:
                this.IK = new DataOutputStream(this.IJ.getOutputStream());
                mb.n("TcpNetwork", "startSocket() 5");
                this.IL = new DataInputStream(this.IJ.getInputStream());
                break;
            case (byte) 1:
                this.IJ.setSoTimeout(15000);
                break;
        }
        mb.n("TcpNetwork", "startSocket() 6");
        return hl();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int b(f fVar, byte[] bArr) {
        try {
            synchronized (this.IJ) {
                if (hl()) {
                    Object -l_4_R = new ByteArrayOutputStream();
                    Object -l_5_R = new DataOutputStream(-l_4_R);
                    -l_5_R.writeInt(bArr.length);
                    -l_5_R.write(bArr);
                    Object -l_6_R = -l_4_R.toByteArray();
                    mb.n("TcpNetwork", "[tcp_control]sendDataInAsync(), bf [tcp send] bytes: " + -l_6_R.length);
                    this.IK.write(-l_6_R);
                    mb.d("TcpNetwork", "[flow_control][tcp_control]sendDataInAsync(), [tcp send] bytes: " + -l_6_R.length);
                    if (!(fVar == null || fVar.Ft == null || fVar.Ft.size() <= 0)) {
                        int -l_7_I = fVar.Ft.size();
                        Object -l_8_R = fVar.Ft.iterator();
                        while (-l_8_R.hasNext()) {
                            bw -l_9_R = (bw) -l_8_R.next();
                            if (-l_9_R != null) {
                                nt.ga().a("TcpNetwork", -l_9_R.bz, -l_9_R.ey, -l_9_R, 12, 0, String.format("%d/%d", new Object[]{Integer.valueOf(-l_6_R.length), Integer.valueOf(-l_7_I)}));
                                if (-l_9_R.ez == 0) {
                                    oe.a(new oe(), -l_9_R.ey);
                                }
                            }
                        }
                    }
                } else {
                    return -180000;
                }
            }
        } catch (Object -l_3_R) {
            this.IF = -l_3_R.toString();
            mb.o("TcpNetwork", "sendDataInAsync() SocketException: " + -l_3_R.toString());
            return -330000;
        } catch (Object -l_3_R2) {
            this.IF = -l_3_R2.toString();
            mb.o("TcpNetwork", "sendDataInAsync() Throwable: " + -l_3_R2.toString());
            return -320000;
        }
    }

    private int b(b bVar) {
        int -l_2_I;
        String unknownHostException;
        mb.d("TcpNetwork", "[tcp_control]checkSocket()");
        if (bVar == null) {
            return -10;
        }
        if (hl()) {
            mb.s("TcpNetwork", "[tcp_control]checkSocket(), already contected");
            return 0;
        }
        try {
            if (a(bVar)) {
                -l_2_I = 0;
                mb.r("TcpNetwork", "[tcp_control]checkSocket(), startSocket succ, set: mIsIgnoreStopExption = false");
                this.IN = false;
            } else {
                -l_2_I = -340000;
            }
            this.IF = "";
        } catch (Object -l_3_R) {
            -l_2_I = -70000;
            mb.b("TcpNetwork", "checkSocket(), UnknownHostException: ", -l_3_R);
            if (this.IM != null) {
                this.IM.a(7, bVar);
            }
            unknownHostException = -l_3_R.toString();
            this.IF = unknownHostException;
            return -l_2_I;
        } catch (Object -l_3_R2) {
            -l_2_I = -130000;
            mb.b("TcpNetwork", "checkSocket(), SocketTimeoutException: ", -l_3_R2);
            if (this.IM != null) {
                this.IM.a(8, bVar);
            }
            unknownHostException = -l_3_R2.toString();
            this.IF = unknownHostException;
            return -l_2_I;
        } catch (Object -l_3_R22) {
            -l_2_I = ne.f(-l_3_R22.toString(), -500000);
            mb.b("TcpNetwork", "checkSocket(), ConnectException: ", -l_3_R22);
            if (this.IM != null) {
                this.IM.a(9, bVar);
            }
            unknownHostException = -l_3_R22.toString();
            this.IF = unknownHostException;
            return -l_2_I;
        } catch (Object -l_3_R222) {
            -l_2_I = ne.f(-l_3_R222.toString(), -420000);
            mb.b("TcpNetwork", "checkSocket(), SocketException: ", -l_3_R222);
            if (this.IM != null) {
                this.IM.a(9, bVar);
            }
            unknownHostException = -l_3_R222.toString();
            this.IF = unknownHostException;
            return -l_2_I;
        } catch (Object -l_3_R2222) {
            -l_2_I = ne.f(-l_3_R2222.toString(), -440000);
            mb.b("TcpNetwork", "checkSocket(), SecurityException: ", -l_3_R2222);
            if (this.IM != null) {
                this.IM.a(9, bVar);
            }
            unknownHostException = -l_3_R2222.toString();
            this.IF = unknownHostException;
            return -l_2_I;
        } catch (Object -l_3_R22222) {
            -l_2_I = -900000;
            mb.b("TcpNetwork", "checkSocket(), Throwable: ", -l_3_R22222);
            if (this.IM != null) {
                this.IM.a(9, bVar);
            }
            unknownHostException = -l_3_R22222.toString();
            this.IF = unknownHostException;
            return -l_2_I;
        }
        return -l_2_I;
    }

    private synchronized int e(boolean z, boolean z2) {
        int -l_3_I;
        mb.n("TcpNetwork", "[tcp_control]stop(),  bySvr: " + z + " isRestart: " + z2);
        if (!z) {
            mb.d("TcpNetwork", "[tcp_control]stop(), !bySvr, set: mIsIgnoreStopExption = true");
            this.IN = true;
        }
        this.IG = true;
        -l_3_I = hj();
        if (-l_3_I == 0) {
            if (this.IM != null) {
                if (z) {
                    this.IM.bE(0);
                } else if (z2) {
                    this.IM.bE(2);
                } else {
                    this.IM.bE(1);
                }
            }
        } else if (this.IM != null) {
            this.IM.a(6, "stop socket failed: " + this.IF);
        }
        return -l_3_I;
    }

    private void e(final int i, final byte[] bArr) {
        if (this.IM != null) {
            ((ki) fj.D(4)).addTask(new Runnable(this) {
                final /* synthetic */ oq IO;

                public void run() {
                    try {
                        this.IO.IM.d(i, bArr);
                    } catch (Object -l_1_R) {
                        mb.e("TcpNetwork", -l_1_R);
                    }
                }
            }, "shark-onreceive-callback");
        }
    }

    private void hh() {
        this.IH = new Thread(this, "RcvThread") {
            final /* synthetic */ oq IO;

            public void run() {
                mb.d("TcpNetwork", "[tcp_control]RcvThread start...");
                this.IO.hi();
                mb.d("TcpNetwork", "[tcp_control]RcvThread end!");
            }
        };
        this.IH.setPriority(10);
        this.IH.start();
    }

    private void hi() {
        mb.d("TcpNetwork", "[tcp_control]recv()...");
        while (!this.IG) {
            int -l_1_I = 0;
            try {
                if (this.IE) {
                    -l_1_I = this.IL.readInt();
                }
                int -l_2_I = this.IL.readInt();
                if (-l_2_I < 1000000) {
                    mb.d("TcpNetwork", "[flow_control][tcp_control]recv(), [tcp receive] bytes: " + (-l_2_I + 4));
                    byte[] -l_3_R = on.a(this.IL, 0, -l_2_I, null);
                    if (-l_3_R != null) {
                        mb.d("TcpNetwork", "[tcp_control]notifyOnReceiveData(), respData.length(): " + -l_3_R.length);
                        e(-l_1_I, -l_3_R);
                    } else {
                        mb.o("TcpNetwork", "[tcp_control]recv(), respData == null");
                    }
                } else {
                    mb.o("TcpNetwork", "[flow_control][tcp_control]包有误，数据过大，size >= 1000000, [tcp receive] bytes: " + -l_2_I);
                    return;
                }
            } catch (Object -l_1_R) {
                mb.c("TcpNetwork", "[tcp_control]recv(), SocketException: " + -l_1_R, -l_1_R);
                if (this.IN) {
                    mb.d("TcpNetwork", "[tcp_control]ignore stop exption");
                    this.IG = true;
                } else {
                    e(true, false);
                    if (this.IM != null) {
                        this.IM.a(10, -l_1_R);
                    }
                }
            } catch (Object -l_1_R2) {
                mb.c("TcpNetwork", "[tcp_control]recv() EOFException: " + -l_1_R2, -l_1_R2);
                if (this.IN) {
                    mb.d("TcpNetwork", "[tcp_control]ignore stop exption");
                    this.IG = true;
                } else {
                    e(true, false);
                    if (this.IM != null) {
                        this.IM.a(11, -l_1_R2);
                    }
                }
            } catch (Object -l_1_R22) {
                mb.c("TcpNetwork", "[tcp_control]recv() Throwable: " + -l_1_R22, -l_1_R22);
                if (this.IN) {
                    mb.d("TcpNetwork", "[tcp_control]ignore stop exption");
                    this.IG = true;
                } else {
                    e(true, false);
                    if (this.IM != null) {
                        this.IM.a(12, -l_1_R22);
                    }
                }
            }
        }
        if (!this.IN) {
            stop();
        }
        mb.d("TcpNetwork", "[tcp_control]recv(), recv thread is stopped, set: mIsIgnoreStopExption = false");
        this.IN = false;
        mb.d("TcpNetwork", "[tcp_control]recv(), end!!!");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int hj() {
        mb.n("TcpNetwork", "[tcp_control]stopSocket()");
        long -l_1_J = System.currentTimeMillis();
        synchronized (this.II) {
            if (this.IJ != null) {
            } else {
                mb.s("TcpNetwork", "[tcp_control]stopSocket(), mSocket is null");
                return 0;
            }
        }
        mb.n("TcpNetwork", "stopSocket() 5");
        try {
            if (!this.IJ.isOutputShutdown()) {
                this.IJ.shutdownOutput();
            }
        } catch (Object -l_4_R) {
            mb.d("TcpNetwork", "stopSocket(), mSocket.shutdownOutput() " + -l_4_R);
        }
        mb.n("TcpNetwork", "stopSocket() 6");
        try {
            this.IK.close();
        } catch (Object -l_4_R2) {
            mb.d("TcpNetwork", "stopSocket(), mSocketWriter.close() " + -l_4_R2);
        }
        int -l_3_I = 0;
        try {
            mb.n("TcpNetwork", "stopSocket() 7");
            synchronized (this.II) {
                mb.n("TcpNetwork", "stopSocket() 8");
                this.IJ.close();
                this.IJ = null;
                mb.n("TcpNetwork", "stopSocket() 9");
            }
            Thread.sleep(2000);
            this.IF = "";
        } catch (Object -l_4_R22) {
            -l_3_I = -270000;
            mb.d("TcpNetwork", "stopSocket(), InterruptedException: " + -l_4_R22);
            r7 = -l_4_R22.toString();
        } catch (Object -l_4_R222) {
            -l_3_I = -140000;
            mb.d("TcpNetwork", "stopSocket(), IOException: " + -l_4_R222);
            r7 = -l_4_R222.toString();
        } catch (Object -l_4_R2222) {
            -l_3_I = -900000;
            mb.d("TcpNetwork", "stopSocket(), Throwable: " + -l_4_R2222);
            r7 = -l_4_R2222.toString();
        }
        mb.n("TcpNetwork", "[tcp_control]stopSocket(), ret: " + -l_3_I + " stop action use(ms): " + (System.currentTimeMillis() - -l_1_J));
        return -l_3_I;
        mb.n("TcpNetwork", "stopSocket() 6");
        this.IK.close();
        int -l_3_I2 = 0;
        mb.n("TcpNetwork", "stopSocket() 7");
        synchronized (this.II) {
            mb.n("TcpNetwork", "stopSocket() 8");
            this.IJ.close();
            this.IJ = null;
            mb.n("TcpNetwork", "stopSocket() 9");
        }
        Thread.sleep(2000);
        this.IF = "";
        mb.n("TcpNetwork", "[tcp_control]stopSocket(), ret: " + -l_3_I2 + " stop action use(ms): " + (System.currentTimeMillis() - -l_1_J));
        return -l_3_I2;
        mb.n("TcpNetwork", "stopSocket() 4");
        try {
            this.IL.close();
        } catch (Object -l_4_R22222) {
            mb.d("TcpNetwork", -l_4_R22222);
        }
        mb.n("TcpNetwork", "stopSocket() 5");
        if (this.IJ.isOutputShutdown()) {
            this.IJ.shutdownOutput();
        }
        mb.n("TcpNetwork", "stopSocket() 6");
        this.IK.close();
        int -l_3_I22 = 0;
        mb.n("TcpNetwork", "stopSocket() 7");
        synchronized (this.II) {
            mb.n("TcpNetwork", "stopSocket() 8");
            this.IJ.close();
            this.IJ = null;
            mb.n("TcpNetwork", "stopSocket() 9");
        }
        Thread.sleep(2000);
        this.IF = "";
        mb.n("TcpNetwork", "[tcp_control]stopSocket(), ret: " + -l_3_I22 + " stop action use(ms): " + (System.currentTimeMillis() - -l_1_J));
        return -l_3_I22;
        String interruptedException;
        this.IF = interruptedException;
        mb.n("TcpNetwork", "[tcp_control]stopSocket(), ret: " + -l_3_I22 + " stop action use(ms): " + (System.currentTimeMillis() - -l_1_J));
        return -l_3_I22;
        int -l_3_I222 = 0;
        mb.n("TcpNetwork", "stopSocket() 7");
        synchronized (this.II) {
            mb.n("TcpNetwork", "stopSocket() 8");
            this.IJ.close();
            this.IJ = null;
            mb.n("TcpNetwork", "stopSocket() 9");
        }
        Thread.sleep(2000);
        this.IF = "";
        mb.n("TcpNetwork", "[tcp_control]stopSocket(), ret: " + -l_3_I222 + " stop action use(ms): " + (System.currentTimeMillis() - -l_1_J));
        return -l_3_I222;
    }

    private boolean hk() {
        mb.n("TcpNetwork", "isSocketClosed()");
        synchronized (this.II) {
            mb.n("TcpNetwork", "isSocketClosed() 1");
            if (this.IJ != null) {
                int -l_2_I = this.IJ.isClosed();
                mb.n("TcpNetwork", "isSocketClosed() 2");
                return -l_2_I;
            }
            return true;
        }
    }

    private NetworkInfo hn() {
        Object -l_1_R = null;
        try {
            -l_1_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
        } catch (Object -l_2_R) {
            mb.s("TcpNetwork", " getActiveNetworkInfo NullPointerException--- \n" + -l_2_R.getMessage());
        }
        return -l_1_R;
    }

    private String ho() {
        synchronized (this.II) {
            if (this.IJ != null) {
                String inetAddress = this.IJ.getLocalAddress().toString();
                return inetAddress;
            }
            return "null";
        }
    }

    private int hp() {
        synchronized (this.II) {
            if (this.IJ != null) {
                int localPort = this.IJ.getLocalPort();
                return localPort;
            }
            return 0;
        }
    }

    private boolean isStarted() {
        return !this.IG;
    }

    private int u(byte[] bArr) {
        try {
            this.IK.writeInt(bArr.length);
            this.IK.write(bArr);
            return 0;
        } catch (Object -l_2_R) {
            mb.o("TcpNetwork", "sendDataInSync() Throwable: " + -l_2_R.toString());
            return -310000;
        }
    }

    public int C(Context context) {
        return a(context, false);
    }

    public int a(f fVar, byte[] bArr) {
        if (hk()) {
            return -190000;
        }
        if (!hl()) {
            return -180000;
        }
        if (fVar.gp()) {
            mb.o("TcpNetwork", "[time_out]sendDataAsync(), send time out");
            return -17;
        }
        int -l_3_I = -1;
        switch (this.ID) {
            case (byte) 0:
                -l_3_I = b(fVar, bArr);
                break;
            case (byte) 1:
                -l_3_I = u(bArr);
                break;
            default:
                return -l_3_I;
        }
        return -l_3_I;
    }

    public om gQ() {
        return this.CU;
    }

    public String hf() {
        return this.IF;
    }

    protected int hg() {
        return e(false, true) == 0 ? a(this.mContext, true) : -210000;
    }

    protected boolean hl() {
        int -l_2_I = 0;
        mb.n("TcpNetwork", "isSocketConnected()");
        synchronized (this.II) {
            mb.n("TcpNetwork", "isSocketConnected() 1");
            if (this.IJ != null) {
                mb.n("TcpNetwork", "isSocketConnected() 2");
                if (!hk() && this.IJ.isConnected()) {
                    -l_2_I = 1;
                }
                mb.n("TcpNetwork", "isSocketConnected() 3");
                return -l_2_I;
            }
            return false;
        }
    }

    public boolean hm() {
        Object -l_1_R = hn();
        return -l_1_R != null ? -l_1_R.isConnected() : false;
    }

    public int stop() {
        return e(false, false);
    }
}
