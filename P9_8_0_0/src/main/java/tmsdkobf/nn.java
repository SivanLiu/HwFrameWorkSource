package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import tmsdk.common.utils.f;

public class nn {
    public static <T extends JceStruct> T a(byte[] bArr, T -l_3_R, boolean z) {
        if (bArr == null || -l_3_R == null) {
            return null;
        }
        if (z) {
            -l_3_R = -l_3_R.newInit();
        }
        try {
            -l_3_R.recyle();
            -l_3_R.readFrom(s(bArr));
            return -l_3_R;
        } catch (Object -l_4_R) {
            f.e("JceStructUtil", "getJceStruct exception: " + -l_4_R);
            return null;
        }
    }

    public static byte[] d(JceStruct jceStruct) {
        Object -l_1_R = new JceOutputStream();
        -l_1_R.setServerEncoding("UTF-8");
        jceStruct.writeTo(-l_1_R);
        return -l_1_R.toByteArray();
    }

    public static bx fR() {
        return new bx();
    }

    public static cf r(byte[] bArr) {
        Object -l_2_R = a(bArr, new cf(), false);
        return -l_2_R != null ? (cf) -l_2_R : null;
    }

    private static JceInputStream s(byte[] bArr) {
        Object -l_1_R = new JceInputStream(bArr);
        -l_1_R.setServerEncoding("UTF-8");
        return -l_1_R;
    }
}
