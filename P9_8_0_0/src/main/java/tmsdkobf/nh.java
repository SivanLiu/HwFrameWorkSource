package tmsdkobf;

import android.content.Context;
import android.os.Process;
import com.qq.taf.jce.JceStruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import tmsdk.common.TMSDKContext;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.e;
import tmsdk.common.utils.i;
import tmsdkobf.nq.b;
import tmsdkobf.nw.f;

public class nh {
    private static i Dj;
    private static String Dk;
    private static boolean Dl = false;

    public static JceStruct a(Context context, byte[] bArr, byte[] bArr2, JceStruct jceStruct, boolean z) {
        if (bArr2 == null || bArr2.length == 0) {
            return null;
        }
        Object -l_5_R = a(context, bArr, bArr2);
        return -l_5_R != null ? nn.a(-l_5_R, jceStruct, z) : null;
    }

    public static JceStruct a(Context context, byte[] bArr, byte[] bArr2, JceStruct jceStruct, boolean z, int i) {
        if (bArr2 == null || bArr2.length == 0) {
            return null;
        }
        Object -l_6_R = null;
        Object -l_7_R = a(context, bArr, bArr2, i);
        if (!(-l_7_R == null || -l_7_R.length <= 0 || jceStruct == null)) {
            -l_6_R = nn.a(-l_7_R, jceStruct, z);
            if (-l_6_R == null) {
                mb.s("ConverterUtil", "[shark_v4][shark_cmd]dataForReceive2JceStruct(), getJceStruct() return null! jceData: " + Arrays.toString(-l_7_R));
            }
        }
        return -l_6_R;
    }

    public static JceStruct a(byte[] bArr, byte[] bArr2, JceStruct jceStruct) {
        return a(null, bArr, bArr2, jceStruct, false);
    }

    private static bx a(Context context, boolean z, f fVar, b bVar, ArrayList<bw> arrayList, String str, nl nlVar) {
        Object -l_7_R = nn.fR();
        -l_7_R.ey = fVar.Fq;
        -l_7_R.eH = 4;
        -l_7_R.eJ = arrayList;
        Object -l_9_R;
        if (!z || fVar.Fj || fVar.Fk || fVar.Fm) {
            mb.d("ConverterUtil", "[shark_v4][shark_fin] must take sharkfin: !isTcpChannel: " + (!z) + " isRsa: " + fVar.Fj + " isGuid: " + fVar.Fk + " isFP: " + fVar.Fm);
            -l_9_R = a(context, fVar.Fj, bVar, str, nlVar);
            -l_7_R.eI = -l_9_R;
            Dj = -l_9_R;
        } else {
            if (!fVar.Fl) {
                -l_9_R = a(context, false, bVar, str, nlVar);
                if (!a(-l_9_R, Dj)) {
                    -l_7_R.eI = -l_9_R;
                    Dj = -l_9_R;
                } else if (Dl) {
                    -l_7_R.eI = -l_9_R;
                    Dj = -l_9_R;
                } else {
                    mb.n("ConverterUtil", "[shark_v4][shark_fin] sharkfin unchanged, no need to take sharkfin");
                }
            }
            return -l_7_R;
        }
        Dl = false;
        return -l_7_R;
    }

    private static i a(Context context, boolean z, b bVar, String str, nl nlVar) {
        boolean z2 = false;
        if (nlVar == null) {
            return null;
        }
        Object -l_5_R = new i();
        Object -l_6_R = bVar == null ? "" : bVar.DW;
        if (z) {
            -l_6_R = "";
        }
        -l_5_R.K = -l_6_R;
        -l_5_R.L = 3059;
        -l_5_R.s = w(context);
        -l_5_R.M = i.J(context);
        -l_5_R.authType = fz();
        StringBuilder append = new StringBuilder().append("[ip_list][conn_monitor]checkSharkfin(), apn=").append(-l_5_R.s).append(" isWifi=");
        String str2 = "ConverterUtil";
        if (ln.yO == (byte) 3) {
            z2 = true;
        }
        mb.n(str2, append.append(z2).append(" authType=").append(-l_5_R.authType).toString());
        -l_5_R.I = str;
        -l_5_R.N = nlVar.aM();
        -l_5_R.O = nlVar.aR();
        -l_5_R.P = nlVar.aS();
        if (Dk == null) {
            int -l_7_I = Process.myPid();
            Object -l_8_R = new StringBuilder();
            -l_8_R.append(!nlVar.aB() ? "f" : "b");
            -l_8_R.append(-l_7_I);
            Dk = -l_8_R.toString();
        }
        -l_5_R.J = Dk;
        return -l_5_R;
    }

    private static boolean a(i iVar, i iVar2) {
        int -l_2_I = 1;
        if (iVar == null && iVar2 == null) {
            return true;
        }
        if (iVar == null || iVar2 == null) {
            return false;
        }
        if (iVar.s == iVar2.s && iVar.authType == iVar2.authType && v(iVar.I, iVar2.I) && v(iVar.J, iVar2.J) && v(iVar.K, iVar2.K) && iVar.L == iVar2.L && iVar.M == iVar2.M && iVar.N == iVar2.N && iVar.O == iVar2.O) {
            if (!v(iVar.P, iVar2.P)) {
            }
            return -l_2_I;
        }
        -l_2_I = 0;
        return -l_2_I;
    }

    public static byte[] a(Context context, JceStruct jceStruct, int i, bw bwVar) {
        byte[] -l_4_R = null;
        if (jceStruct != null) {
            -l_4_R = nn.d(jceStruct);
        }
        return a(context, -l_4_R, i, bwVar);
    }

    public static byte[] a(Context context, byte[] -l_5_R, int i, bw bwVar) {
        int -l_6_I;
        if (-l_5_R != null && -l_5_R.length > 50) {
            Object obj;
            Object -l_5_R2 = p(-l_5_R);
            if (bwVar != null) {
                try {
                    -l_6_I = bwVar.eE;
                    if (-l_5_R2 != null) {
                        if (-l_5_R2.length < -l_5_R.length) {
                            bwVar.eE &= -2;
                            mb.n("ConverterUtil", "[shark_compress]compressed, length: " + -l_5_R.length + " -> " + -l_5_R2.length + " cmdId: " + i + " flag: " + Integer.toBinaryString(-l_6_I) + " -> " + Integer.toBinaryString(bwVar.eE));
                        }
                    }
                    int -l_7_I = -l_5_R2 != null ? -l_5_R2.length : -1;
                    try {
                        bwVar.eE |= 1;
                        mb.n("ConverterUtil", "[shark_compress]donnot compress, length: " + -l_5_R.length + " (if compress)|-> " + -l_7_I + " cmdId: " + i + " flag: " + Integer.toBinaryString(-l_6_I) + " -> " + Integer.toBinaryString(bwVar.eE));
                    } catch (Exception e) {
                        -l_5_R2 = e;
                        mb.o("ConverterUtil", "jceStruct2DataForSend(), exception: " + -l_5_R2);
                        obj = -l_5_R2;
                        return null;
                    }
                } catch (Exception e2) {
                    obj = -l_5_R2;
                    Exception -l_5_R3 = e2;
                    mb.o("ConverterUtil", "jceStruct2DataForSend(), exception: " + -l_5_R2);
                    obj = -l_5_R2;
                    return null;
                }
            }
            obj = -l_5_R2;
        } else if (bwVar != null) {
            -l_6_I = bwVar.eE;
            bwVar.eE |= 1;
            mb.n("ConverterUtil", "[shark_compress]without compress, length: " + (-l_5_R == null ? "null" : "" + -l_5_R.length) + " cmdId: " + i + " flag: " + Integer.toBinaryString(-l_6_I) + " -> " + Integer.toBinaryString(bwVar.eE));
        }
        return c(i, -l_5_R);
    }

    @Deprecated
    public static byte[] a(Context context, byte[] bArr, JceStruct jceStruct) {
        if (jceStruct == null) {
            return null;
        }
        byte[] -l_3_R = c(jceStruct);
        return -l_3_R != null ? TccCryptor.encrypt(-l_3_R, bArr) : null;
    }

    public static byte[] a(Context context, byte[] bArr, byte[] bArr2) {
        if (bArr2 == null || bArr2.length == 0) {
            return null;
        }
        try {
            Object -l_3_R = TccCryptor.decrypt(bArr2, bArr);
            return -l_3_R != null ? q(-l_3_R) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] a(Context context, byte[] bArr, byte[] -l_4_R, int i) {
        boolean z = false;
        if (-l_4_R == null || -l_4_R.length == 0) {
            StringBuilder append = new StringBuilder().append("[shark_v4]dataForReceive2JceBytes(), null or empty data, null? ");
            String str = "ConverterUtil";
            if (-l_4_R == null) {
                z = true;
            }
            mb.s(str, append.append(z).toString());
            return null;
        }
        if ((i & 2) == 0) {
            try {
                -l_4_R = TccCryptor.decrypt(-l_4_R, bArr);
            } catch (Object -l_5_R) {
                Object -l_5_R2;
                mb.s("ConverterUtil", "[shark_v4]dataForReceive2JceBytes(), decrypt exception: " + -l_5_R2);
                Object -l_4_R2 = null;
            }
        }
        if (-l_4_R != null && -l_4_R.length >= 4) {
            -l_5_R2 = o(-l_4_R);
            if (-l_5_R2 != null && -l_5_R2.length > 0) {
                Object -l_6_R = (i & 1) != 0 ? -l_5_R2 : q(-l_5_R2);
                if (-l_6_R == null) {
                    mb.s("ConverterUtil", "[shark_v4]dataForReceive2JceBytes(), decompress failed!");
                }
                return -l_6_R;
            }
        }
        mb.s("ConverterUtil", "[shark_v4]dataForReceive2JceBytes(), data should be at least 4 bytes: " + (-l_4_R == null ? -1 : -l_4_R.length));
        return null;
    }

    public static byte[] a(f fVar, boolean z, String str, nl nlVar) {
        if (fVar == null) {
            return null;
        }
        Object -l_4_R = !fVar.Fl ? nn.d(a(TMSDKContext.getApplicaionContext(), z, fVar, fVar.Fr, fVar.Ft, str, nlVar)) : new byte[]{(byte) fVar.Fx};
        mb.d("ConverterUtil", "createSendBytes(), isHello? " + fVar.Fl + " sendData.length: " + (-l_4_R == null ? -1 : -l_4_R.length));
        return -l_4_R;
    }

    public static JceStruct b(byte[] bArr, JceStruct jceStruct) {
        return nn.a(bArr, jceStruct, false);
    }

    public static byte[] b(JceStruct jceStruct) {
        return jceStruct != null ? nn.d(jceStruct) : null;
    }

    private static byte[] c(int i, byte[] bArr) {
        Object -l_3_R;
        try {
            -l_3_R = new ByteArrayOutputStream();
            Object -l_4_R = new DataOutputStream(-l_3_R);
            -l_4_R.writeInt(i);
            if (bArr != null && bArr.length > 0) {
                -l_4_R.write(bArr);
            }
            return -l_3_R.toByteArray();
        } catch (Object -l_3_R2) {
            mb.s("ConverterUtil", "[shark_v4]appendIntHeader(), exception: " + -l_3_R2);
            return null;
        }
    }

    public static byte[] c(JceStruct jceStruct) {
        if (jceStruct == null) {
            return null;
        }
        Object -l_2_R = p(nn.d(jceStruct));
        return -l_2_R != null ? -l_2_R : null;
    }

    public static byte[] c(byte[] bArr, JceStruct jceStruct) {
        return a(null, bArr, jceStruct);
    }

    public static byte[] decrypt(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr.length == 0) {
            return bArr;
        }
        try {
            return TccCryptor.decrypt(bArr, bArr2);
        } catch (Object -l_2_R) {
            mb.o("ConverterUtil", "decrypt(), exception: " + -l_2_R.toString());
            return null;
        }
    }

    public static byte[] encrypt(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr.length == 0) {
            return bArr;
        }
        try {
            return TccCryptor.encrypt(bArr, bArr2);
        } catch (Object -l_2_R) {
            mb.o("ConverterUtil", "encrypt(), exception: " + -l_2_R.toString());
            return null;
        }
    }

    public static void fy() {
        Dl = true;
    }

    private static int fz() {
        switch (e.iB()) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }

    private static byte[] o(byte[] bArr) {
        if (bArr != null && bArr.length >= 4) {
            Object -l_1_R = new byte[(bArr.length - 4)];
            System.arraycopy(bArr, 4, -l_1_R, 0, bArr.length - 4);
            return -l_1_R;
        }
        mb.s("ConverterUtil", "[shark_v4]deleteIntHeader(), mixData is not valid, len: " + (bArr == null ? -1 : bArr.length));
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static byte[] p(byte[] bArr) {
        Object -l_2_R = new ByteArrayOutputStream();
        Object -l_3_R = new DeflaterOutputStream(-l_2_R);
        try {
            -l_3_R.write(bArr);
            -l_3_R.finish();
            Object -l_1_R = -l_2_R.toByteArray();
            try {
                -l_2_R.close();
                -l_3_R.close();
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
            }
            return -l_1_R;
        } catch (Throwable th) {
            try {
                -l_2_R.close();
                -l_3_R.close();
            } catch (Object -l_8_R) {
                -l_8_R.printStackTrace();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static byte[] q(byte[] bArr) {
        Object -l_2_R = new ByteArrayInputStream(bArr);
        Object -l_3_R = new InflaterInputStream(-l_2_R);
        Object -l_4_R = new ByteArrayOutputStream();
        while (true) {
            try {
                int -l_5_I = -l_3_R.read();
                if (-l_5_I == -1) {
                    break;
                }
                -l_4_R.write(-l_5_I);
            } catch (Object -l_5_R) {
                mb.s("ConverterUtil", "inflater(), exception: " + -l_5_R);
                return null;
            } catch (Throwable th) {
                try {
                    -l_2_R.close();
                    -l_3_R.close();
                    -l_4_R.close();
                } catch (Object -l_9_R) {
                    -l_9_R.printStackTrace();
                }
            }
        }
        Object -l_1_R = -l_4_R.toByteArray();
        try {
            -l_2_R.close();
            -l_3_R.close();
            -l_4_R.close();
        } catch (Object -l_5_R2) {
            -l_5_R2.printStackTrace();
        }
        return -l_1_R;
    }

    private static boolean v(String str, String str2) {
        return (str == null && str2 == null) ? true : (str == null || str2 == null) ? false : str.equals(str2);
    }

    public static int w(Context context) {
        ln.yN = false;
        ln.q(context);
        if ((byte) 3 == ln.yO) {
            return 1;
        }
        switch (ln.yQ) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 4;
            case 3:
                return 5;
            case 4:
                return 6;
            case 5:
                return 7;
            case 6:
                return 8;
            case 7:
                return 9;
            case 8:
                return 10;
            default:
                return 0;
        }
    }
}
