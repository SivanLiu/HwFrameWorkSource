package com.huawei.android.pushagent.utils.c;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.b;

public class c {
    public static byte[] bf(byte[] bArr, byte[] bArr2) {
        return bg(bArr, bArr2, null);
    }

    public static byte[] bg(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        try {
            d bi = d.bi(bArr2);
            if (bArr3 == null) {
                return bi.bj(bArr);
            }
            return bi.bk(bArr, bArr3);
        } catch (Throwable e) {
            b.aa("PushLog2976", "InvalidKeyException:" + e.getMessage(), e);
            return null;
        } catch (Throwable e2) {
            b.aa("PushLog2976", "aesEncrypter get BadPaddingException:" + e2.getMessage(), e2);
            return null;
        } catch (Throwable e22) {
            b.aa("PushLog2976", "IllegalBlockSizeException:" + e22.getMessage(), e22);
            return null;
        } catch (Throwable e222) {
            b.aa("PushLog2976", "NoSuchAlgorithmException:" + e222.getMessage(), e222);
            return null;
        } catch (Throwable e2222) {
            b.aa("PushLog2976", "NoSuchPaddingException:" + e2222.getMessage(), e2222);
            return null;
        } catch (Throwable e22222) {
            b.aa("PushLog2976", "Exception:" + e22222.getMessage(), e22222);
            return null;
        }
    }

    public static byte[] be(byte[] bArr, byte[] bArr2) {
        try {
            return d.bi(bArr2).bl(bArr);
        } catch (Throwable e) {
            b.aa("PushLog2976", "InvalidKeyException:" + e.getMessage(), e);
            return null;
        } catch (Throwable e2) {
            b.aa("PushLog2976", "BadPaddingException:" + e2.getMessage(), e2);
            return null;
        } catch (Throwable e22) {
            b.aa("PushLog2976", "IllegalBlockSizeException:" + e22.getMessage(), e22);
            return null;
        } catch (Throwable e222) {
            b.aa("PushLog2976", "NoSuchAlgorithmException:" + e222.getMessage(), e222);
            return null;
        } catch (Throwable e2222) {
            b.aa("PushLog2976", "NoSuchPaddingException:" + e2222.getMessage(), e2222);
            return null;
        } catch (Throwable e22222) {
            b.aa("PushLog2976", "Exception:" + e22222.getMessage(), e22222);
            return null;
        }
    }

    public static byte[] bh(byte[] bArr, String str) {
        byte[] bArr2 = null;
        try {
            bArr2 = b.az(bArr, str);
        } catch (Throwable e) {
            b.aa("PushLog2976", "rsa encrypt data error ", e);
        }
        return bArr2;
    }

    public static String bc(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return e.bp(str);
    }

    public static String bb(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return e.bq(str);
    }

    public static String bd(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return e.br(str, bArr);
    }
}
