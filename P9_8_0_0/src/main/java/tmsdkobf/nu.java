package tmsdkobf;

import android.os.Looper;
import android.text.TextUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import tmsdk.common.CallerIdent;
import tmsdk.common.utils.i;
import tmsdk.common.utils.u;
import tmsdk.common.utils.u.a;

public class nu {
    private static nl CT = null;
    private static boolean EA = false;
    private static boolean EB = false;
    private static boolean EC = false;
    private static oa ED = null;
    public static boolean Eu = false;
    public static boolean Ev = false;
    private static Looper Ew = null;
    private static boolean Ex = false;
    private static String Ey = null;
    private static boolean Ez = false;
    private static Looper sLooper = null;

    public static void H(boolean z) {
        Ex = z;
    }

    public static void I(boolean z) {
        Ez = z;
    }

    public static void J(boolean z) {
        EA = z;
    }

    public static void K(boolean z) {
        EB = z;
    }

    public static void L(boolean z) {
        EC = z;
    }

    public static void a(oa oaVar) {
        mb.d("SharkHelper", "[shark_init]initSharkQueueInstance(), sharkQueue: " + oaVar);
        ED = oaVar;
    }

    public static boolean aB() {
        return Ez;
    }

    public static boolean aC() {
        return EA;
    }

    public static boolean aD() {
        return EB;
    }

    private static long bL() {
        return CallerIdent.getIdent(3, 4294967296L);
    }

    public static boolean br(int i) {
        return i == 152 || i == 1;
    }

    public static boolean bs(int i) {
        return i == 997 || i == 999;
    }

    public static boolean ch(String str) {
        int -l_2_I = 0;
        if (eb.iJ != i.iG()) {
            return false;
        }
        mb.n("SharkHelper", "[detect_conn]needWifiApprove(), from: " + str);
        Object -l_1_R = null;
        try {
            -l_1_R = u.a(new a() {
                public void d(boolean z, boolean z2) {
                    mb.n("SharkHelper", "[detect_conn]needWifiApprove() callback,  need: " + z + " receivedError: " + z2);
                }
            });
        } catch (Object -l_2_R) {
            mb.o("SharkHelper", "[detect_conn]needWifiApprove(), exception: " + -l_2_R.toString());
        }
        if (!TextUtils.isEmpty(-l_1_R)) {
            -l_2_I = 1;
        }
        mb.n("SharkHelper", "[detect_conn]needWifiApprove(),  need approve: " + -l_2_I + " approve url: " + -l_1_R);
        return -l_2_I;
    }

    public static boolean ci(String str) {
        Object -l_6_R;
        Object -l_8_R;
        int -l_1_I = 0;
        Object -l_2_R = "www.qq.com";
        mb.n("SharkHelper", "[detect_conn]detectConnection, host: " + -l_2_R + " from: " + str);
        long -l_3_J = System.currentTimeMillis();
        Socket socket = null;
        try {
            Object -l_7_R = new InetSocketAddress(InetAddress.getByName(-l_2_R), 80);
            Socket -l_5_R = new Socket();
            try {
                -l_5_R.setSoLinger(false, 0);
                -l_5_R.connect(-l_7_R, 5000);
                -l_1_I = 1;
                if (-l_5_R != null) {
                    try {
                        if (-l_5_R.isConnected()) {
                            -l_5_R.close();
                        }
                    } catch (Object -l_6_R2) {
                        -l_6_R2.printStackTrace();
                    }
                }
                socket = -l_5_R;
            } catch (IOException e) {
                -l_6_R2 = e;
                socket = -l_5_R;
                try {
                    mb.c("SharkHelper", "[detect_conn]detectConnection, exception: " + -l_6_R2.getMessage(), -l_6_R2);
                    if (socket != null) {
                        try {
                            if (socket.isConnected()) {
                                socket.close();
                            }
                        } catch (Object -l_6_R22) {
                            -l_6_R22.printStackTrace();
                        }
                    }
                    mb.n("SharkHelper", "[detect_conn]detectConnection end, isConnect: " + -l_1_I + " time cost: " + (System.currentTimeMillis() - -l_3_J));
                    return -l_1_I;
                } catch (Throwable th) {
                    -l_8_R = th;
                    if (socket != null) {
                        try {
                            if (socket.isConnected()) {
                                socket.close();
                            }
                        } catch (Object -l_9_R) {
                            -l_9_R.printStackTrace();
                        }
                    }
                    throw -l_8_R;
                }
            } catch (Throwable th2) {
                -l_8_R = th2;
                socket = -l_5_R;
                if (socket != null) {
                    if (socket.isConnected()) {
                        socket.close();
                    }
                }
                throw -l_8_R;
            }
        } catch (IOException e2) {
            -l_6_R22 = e2;
            mb.c("SharkHelper", "[detect_conn]detectConnection, exception: " + -l_6_R22.getMessage(), -l_6_R22);
            if (socket != null) {
                if (socket.isConnected()) {
                    socket.close();
                }
            }
            mb.n("SharkHelper", "[detect_conn]detectConnection end, isConnect: " + -l_1_I + " time cost: " + (System.currentTimeMillis() - -l_3_J));
            return -l_1_I;
        } catch (Throwable th3) {
            -l_6_R22 = th3;
            mb.c("SharkHelper", "[detect_conn]detectConnection, Throwable: " + -l_6_R22.getMessage(), -l_6_R22);
            if (socket != null) {
                if (socket.isConnected()) {
                    socket.close();
                }
            }
            mb.n("SharkHelper", "[detect_conn]detectConnection end, isConnect: " + -l_1_I + " time cost: " + (System.currentTimeMillis() - -l_3_J));
            return -l_1_I;
        }
        mb.n("SharkHelper", "[detect_conn]detectConnection end, isConnect: " + -l_1_I + " time cost: " + (System.currentTimeMillis() - -l_3_J));
        return -l_1_I;
    }

    public static void cj(String str) {
        Ey = str;
    }

    public static boolean gc() {
        return Ex;
    }

    public static String gd() {
        return Ey;
    }

    public static boolean ge() {
        return EC;
    }

    public static Looper getLooper() {
        if (sLooper == null) {
            Object -l_0_R = nu.class;
            synchronized (nu.class) {
                if (sLooper == null) {
                    Object -l_2_R = ((ki) fj.D(4)).newFreeHandlerThread("Shark-Looper");
                    -l_2_R.start();
                    sLooper = -l_2_R.getLooper();
                }
            }
        }
        return sLooper;
    }

    public static oa gf() {
        if (ED == null) {
            Object -l_0_R = ob.class;
            synchronized (ob.class) {
                if (ED == null) {
                    ED = new ob(bL());
                }
            }
        }
        return ED;
    }

    public static boolean t(byte[] bArr) {
        if (bArr != null) {
            if (bArr.length == 1) {
                return true;
            }
        }
        return false;
    }
}
