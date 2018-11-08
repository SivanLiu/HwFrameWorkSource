package tmsdk.common.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.Build.VERSION;
import android.os.Process;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdkobf.if;
import tmsdkobf.lq;
import tmsdkobf.lu;
import tmsdkobf.qd;

public final class ScriptHelper {
    private static final String LW = (TMSDKContext.getApplicaionContext().getPackageName() + "_" + "athena_v4_2-mfr.dat" + "_" + Process.myUid());
    private static final boolean LX = new File("/dev/socket/script_socket").exists();
    private static int LY = 2;
    private static boolean LZ = LX;
    private static Object Ma = new Object();
    private static a Mb = null;
    private static int Mc = Process.myPid();
    private static BroadcastReceiver Md = new if() {
        public void doOnRecv(Context context, Intent intent) {
        }
    };
    private static qd Me = null;
    public static final int ROOT_GOT = 0;
    public static final int ROOT_NOT_GOT = 2;
    public static final int ROOT_NOT_SUPPORT = 1;
    public static final int ROOT_NO_RESPOND = -1;
    public static final String ROOT_STATE_KEY = "rtstky";
    public static boolean isSuExist;

    public interface a {
        int H(long j);

        boolean cG(String str);

        int jf();

        void jg();
    }

    static final class b {
        byte[] data;
        int size;
        int time;
        int type;

        b() {
        }

        void writeToStream(OutputStream outputStream) throws IOException {
            this.size = this.data == null ? 0 : this.data.length;
            Object -l_2_R = new byte[12];
            System.arraycopy(lq.aO(this.type), 0, -l_2_R, 0, 4);
            System.arraycopy(lq.aO(this.time), 0, -l_2_R, 4, 4);
            System.arraycopy(lq.aO(this.size), 0, -l_2_R, 8, 4);
            outputStream.write(-l_2_R);
            if (this.data != null && this.data.length > 0) {
                outputStream.write(this.data);
            }
            outputStream.flush();
        }
    }

    static final class c {
        byte[] data;
        int size;

        c() {
        }

        void e(InputStream inputStream) throws IOException {
            Object -l_2_R = new byte[4];
            if (inputStream.read(-l_2_R) == 4) {
                this.size = lq.k(-l_2_R);
                if (this.size <= 0) {
                    this.data = new byte[0];
                    return;
                }
                Object -l_3_R = new byte[this.size];
                int -l_4_I = 0;
                while (true) {
                    int -l_5_I = inputStream.read(-l_3_R, -l_4_I, this.size - -l_4_I);
                    if (-l_5_I <= 0) {
                        break;
                    }
                    -l_4_I += -l_5_I;
                }
                if (-l_4_I == this.size) {
                    this.data = -l_3_R;
                    return;
                }
                throw new IOException("respond data is invalid");
            }
            throw new IOException("respond data is invalid");
        }
    }

    static {
        boolean z = (lu.bM("/system/bin/su") || lu.bM("/system/xbin/su") || lu.bM("/sbin/su")) ? true : LX;
        isSuExist = z;
    }

    private static synchronized c a(b bVar, boolean z) {
        Object -l_7_R;
        Object -l_8_R;
        synchronized (ScriptHelper.class) {
            Object -l_2_R = new LocalSocket();
            OutputStream outputStream = null;
            InputStream inputStream = null;
            Object -l_6_R = new c();
            try {
                Object -l_3_R;
                if (LX) {
                    -l_3_R = new LocalSocketAddress("/dev/socket/script_socket", Namespace.FILESYSTEM);
                } else {
                    -l_3_R = new LocalSocketAddress(LW, Namespace.ABSTRACT);
                }
                try {
                    f.d("Root-ScriptHelper", "connect:[" + -l_3_R + "]");
                    -l_2_R.connect(-l_3_R);
                } catch (Object -l_7_R2) {
                    f.d("Root-ScriptHelper", "connect IOException:[" + -l_7_R2 + "]");
                    if (!LX && z) {
                        jd();
                        try {
                            Thread.sleep(1000);
                        } catch (Object -l_8_R2) {
                            -l_8_R2.printStackTrace();
                        }
                        -l_8_R2 = a(bVar, LX);
                        if (null != null) {
                            try {
                                outputStream.close();
                            } catch (Object -l_9_R) {
                                -l_9_R.printStackTrace();
                            }
                        }
                        if (null != null) {
                            try {
                                inputStream.close();
                            } catch (Object -l_9_R2) {
                                -l_9_R2.printStackTrace();
                            }
                        }
                        try {
                            -l_2_R.close();
                        } catch (Object -l_9_R22) {
                            -l_9_R22.printStackTrace();
                        }
                        return -l_8_R2;
                    }
                }
                try {
                    inputStream = -l_2_R.getInputStream();
                    outputStream = -l_2_R.getOutputStream();
                    bVar.writeToStream(outputStream);
                    -l_6_R.e(inputStream);
                    -l_7_R2 = -l_6_R;
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Object -l_8_R22) {
                            -l_8_R22.printStackTrace();
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Object -l_8_R222) {
                            -l_8_R222.printStackTrace();
                        }
                    }
                    try {
                        -l_2_R.close();
                    } catch (Object -l_8_R2222) {
                        -l_8_R2222.printStackTrace();
                    }
                    return -l_6_R;
                } catch (Object -l_7_R22) {
                    -l_7_R22.printStackTrace();
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Object -l_7_R222) {
                            -l_7_R222.printStackTrace();
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Object -l_7_R2222) {
                            -l_7_R2222.printStackTrace();
                        }
                    }
                    try {
                        -l_2_R.close();
                    } catch (Object -l_7_R22222) {
                        -l_7_R22222.printStackTrace();
                    }
                    return null;
                }
            } catch (Object -l_7_R222222) {
                -l_7_R222222.printStackTrace();
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Object -l_7_R2222222) {
                        -l_7_R2222222.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Object -l_7_R22222222) {
                        -l_7_R22222222.printStackTrace();
                    }
                }
                try {
                    -l_2_R.close();
                } catch (Object -l_7_R222222222) {
                    -l_7_R222222222.printStackTrace();
                }
                return null;
            } catch (Object -l_7_R2222222222) {
                -l_7_R2222222222.printStackTrace();
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Object -l_7_R22222222222) {
                        -l_7_R22222222222.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Object -l_7_R222222222222) {
                        -l_7_R222222222222.printStackTrace();
                    }
                }
                try {
                    -l_2_R.close();
                } catch (Object -l_7_R2222222222222) {
                    -l_7_R2222222222222.printStackTrace();
                }
                return null;
            } catch (Throwable th) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Object -l_11_R) {
                        -l_11_R.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Object -l_11_R2) {
                        -l_11_R2.printStackTrace();
                    }
                }
                try {
                    -l_2_R.close();
                } catch (Object -l_11_R22) {
                    -l_11_R22.printStackTrace();
                }
            }
        }
    }

    public static int acquireRoot() {
        int -l_0_I;
        if (LY == 0) {
            -l_0_I = je();
            f.f("Root-ScriptHelper", "acquireRoot(), sCurrRootState = ROOT_GOT; isReallyGot ? " + -l_0_I);
            if (-l_0_I != 0) {
                return LY;
            }
        }
        if (Mb == null) {
            -l_0_I = doAcquireRoot();
            f.f("Root-ScriptHelper", "do acquire root locally, root state=" + -l_0_I);
        } else {
            -l_0_I = Mb.H(4294967299L);
            f.f("Root-ScriptHelper", "do acquire root by proxy-RootService, root state=" + -l_0_I);
        }
        return -l_0_I;
    }

    public static String acquireRootAndRunScript(int i, List<String> list) {
        return acquireRoot() == 0 ? runScript(i, (List) list) : null;
    }

    public static String acquireRootAndRunScript(int i, String... strArr) {
        return acquireRootAndRunScript(i, new ArrayList(Arrays.asList(strArr)));
    }

    public static void actualStartDaemon() {
        int -l_0_I = LY;
        LY = 2;
        if (-l_0_I != LY) {
            jb();
        }
        f.f("Root-ScriptHelper", "[beg]startDaemon @ " + Process.myPid());
        Object -l_2_R = lu.b(TMSDKContext.getApplicaionContext(), "athena_v4_2-mfr.dat", null);
        Object -l_4_R = "chmod 755 " + -l_2_R + "\n" + String.format(Locale.US, "%s %s %d", new Object[]{-l_2_R, LW, Integer.valueOf(Process.myUid())}) + "\n";
        if (Mb == null || !Mb.cG(-l_4_R)) {
            Object -l_5_R = new ProcessBuilder(new String[0]);
            -l_5_R.command(new String[]{"sh"});
            OutputStream outputStream = null;
            try {
                -l_5_R.redirectErrorStream(true);
                outputStream = -l_5_R.start().getOutputStream();
                outputStream.write(-l_4_R.getBytes());
                outputStream.flush();
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Object -l_7_R) {
                        -l_7_R.printStackTrace();
                    }
                }
            } catch (Object -l_7_R2) {
                -l_7_R2.printStackTrace();
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Object -l_7_R22) {
                        -l_7_R22.printStackTrace();
                    }
                }
            } catch (Error e) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Object -l_7_R222) {
                        -l_7_R222.printStackTrace();
                    }
                }
            } catch (Throwable th) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Object -l_9_R) {
                        -l_9_R.printStackTrace();
                    }
                }
            }
            f.f("Root-ScriptHelper", "[end]startDaemon @ " + Process.myPid());
        }
    }

    public static boolean checkIfSuExist() {
        boolean z = LX;
        if (lu.bM("/system/bin/su") || lu.bM("/system/xbin/su") || lu.bM("/sbin/su")) {
            z = true;
        }
        isSuExist = z;
        f.d("Root-ScriptHelper", "checkIfSuExist:[" + isSuExist + "]");
        return isSuExist;
    }

    public static int doAcquireRoot() {
        int -l_0_I = LY;
        checkIfSuExist();
        if (LX) {
            LY = 0;
        } else if (isSuExist) {
            synchronized (Ma) {
                int -l_3_I = 2;
                int -l_4_I = 0;
                while (-l_4_I < 1) {
                    -l_3_I = jc();
                    if (-l_3_I == -1) {
                        -l_4_I++;
                    }
                }
                LY = -l_3_I;
            }
        } else {
            LY = 1;
        }
        if (!LZ) {
            int i = LY;
        }
        if (-l_0_I != LY) {
            jb();
        }
        return LY;
    }

    public static String[] exec(File file, String... strArr) {
        Object -l_3_R;
        try {
            -l_3_R = new StringBuffer();
            Object -l_4_R = new ProcessBuilder(strArr);
            if (file != null) {
                -l_4_R.directory(file);
            }
            -l_4_R.redirectErrorStream(LX);
            Object -l_5_R = -l_4_R.start();
            Object -l_6_R = -l_5_R.getInputStream();
            Object -l_7_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
            while (true) {
                int -l_8_I = -l_6_R.read(-l_7_R);
                if (-l_8_I <= 0) {
                    -l_6_R.close();
                    -l_5_R.destroy();
                    return -l_3_R.toString().split("\n");
                }
                -l_3_R.append(new String(-l_7_R, 0, -l_8_I));
            }
        } catch (Object -l_3_R2) {
            -l_3_R2.printStackTrace();
            return null;
        } catch (Error e) {
            return null;
        }
    }

    public static String[] exec(String... strArr) {
        return exec(null, strArr);
    }

    public static int getRootState() {
        return Mb == null ? LY : Mb.jf();
    }

    public static int getRootStateActual() {
        return LY;
    }

    public static void initForeMultiProcessUse() {
        ja();
    }

    public static boolean isRootGot() {
        return getRootState() != 0 ? LX : true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isRootUid() {
        f.d("Root-ScriptHelper", "isRootUid");
        synchronized (Ma) {
            Object -l_1_R = runScript(-1, "id");
            f.d("Root-ScriptHelper", "isRootUid res=" + -l_1_R);
            if (-l_1_R != null && -l_1_R.contains("uid=0")) {
                return true;
            } else if (LY == 0) {
                LY = 2;
                jb();
            }
        }
    }

    public static boolean isSystemUid() {
        return Process.myUid() != CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY ? LX : true;
    }

    private static void ja() {
    }

    private static void jb() {
    }

    private static int jc() {
        Object -l_1_R = runScript(-1, "id");
        f.d("Root-ScriptHelper", "run (id):[" + -l_1_R + "]");
        if (-l_1_R != null) {
            if (-l_1_R.contains("uid=0")) {
                return 0;
            }
            -l_1_R = runScript(-1, "su");
            f.d("Root-ScriptHelper", "run (su):[" + -l_1_R + "]");
            if (-l_1_R != null) {
                if (-l_1_R.contains("Kill") || -l_1_R.contains("kill")) {
                    return -1;
                }
                -l_1_R = runScript(-1, "id");
                f.d("Root-ScriptHelper", "run (su--id):[" + -l_1_R + "]");
                if (-l_1_R != null) {
                    if (!-l_1_R.contains("uid=0")) {
                        return 2;
                    }
                    List -l_2_R = new ArrayList();
                    v(-l_2_R);
                    runScript(-1, -l_2_R);
                    return 0;
                }
            }
        }
        return 2;
    }

    private static void jd() {
        if (Mb != null) {
            Mb.jg();
        } else {
            actualStartDaemon();
        }
    }

    private static boolean je() {
        Object -l_0_R = new b();
        -l_0_R.time = CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY;
        -l_0_R.data = "id\n".getBytes();
        Object -l_1_R = a(-l_0_R, LX);
        return (-l_1_R != null && new String(-l_1_R.data).contains("uid=0")) ? true : LX;
    }

    public static qd provider() {
        return Me;
    }

    public static boolean providerSupportCancelMissCall() {
        return (Me != null && Me.bU(2)) ? true : LX;
    }

    public static boolean providerSupportCpuRelative() {
        return (Me != null && Me.bU(3)) ? true : LX;
    }

    public static boolean providerSupportGetAllApkFiles() {
        if (Me != null) {
            if (Me.bU(1)) {
                return true;
            }
        }
        return LX;
    }

    public static boolean providerSupportPmRelative() {
        return (Me != null && Me.bU(4)) ? true : LX;
    }

    public static String runScript(int i, List<String> list) {
        Object -l_4_R;
        if (i < 0) {
            i = 30000;
        }
        v(list);
        Object -l_2_R = new StringBuilder();
        for (Object -l_4_R2 : list) {
            if (VERSION.SDK_INT >= 21 && -l_4_R2 != null) {
                if (-l_4_R2.indexOf("pm ") == 0 || -l_4_R2.indexOf("am ") == 0 || -l_4_R2.indexOf("service ") == 0) {
                    -l_4_R2 = "su -cn u:r:shell:s0 -c " + -l_4_R2 + " < /dev/null";
                } else if (-l_4_R2.indexOf("dumpsys ") == 0) {
                    -l_4_R2 = "su -cn u:r:system:s0 -c " + -l_4_R2 + " < /dev/null";
                }
            }
            -l_2_R.append(-l_4_R2).append("\n");
        }
        Object -l_3_R = new b();
        -l_3_R.type = 0;
        -l_3_R.time = i;
        -l_3_R.data = -l_2_R.toString().getBytes();
        -l_4_R2 = a(-l_3_R, true);
        if (-l_4_R2 != null) {
            try {
                if (-l_4_R2.data != null) {
                    return new String(-l_4_R2.data).trim();
                }
            } catch (Object -l_5_R) {
                -l_5_R.printStackTrace();
            } catch (Error e) {
            }
        }
        return null;
    }

    public static String runScript(int i, String... strArr) {
        return runScript(i, new ArrayList(Arrays.asList(strArr)));
    }

    public static void setProvider(qd qdVar) {
        int -l_1_I = 0;
        for (Object -l_5_R : Thread.currentThread().getStackTrace()) {
            if (-l_5_R.getClass().equals(TMSDKContext.class) && -l_5_R.getMethodName().indexOf("init") >= 0) {
                -l_1_I = 1;
                break;
            }
        }
        if (-l_1_I != 0) {
            Me = qdVar;
        } else {
            f.e("ScriptHelper", "Unauthorized caller");
        }
    }

    public static void setRootService(a aVar) {
        Mb = aVar;
    }

    public static boolean stopDaemon() {
        Object -l_0_R = new b();
        -l_0_R.type = 1;
        -l_0_R.data = "echo old".getBytes();
        Object -l_1_R = a(-l_0_R, LX);
        return (-l_1_R == null || new String(-l_1_R.data).trim().contains("old")) ? LX : true;
    }

    private static void v(List<String> list) {
        for (Entry -l_4_R : new ProcessBuilder(new String[0]).environment().entrySet()) {
            list.add("export " + ((String) -l_4_R.getKey()) + "=" + ((String) -l_4_R.getValue()));
        }
    }
}
