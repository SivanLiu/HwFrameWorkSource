package com.huawei.android.pushagent.utils.a;

import android.text.TextUtils;

public class c {
    public static byte[] m(byte[] bArr, byte[] bArr2) {
        return n(bArr, bArr2, null);
    }

    public static byte[] n(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        try {
            b g = b.g(bArr2);
            if (bArr3 == null) {
                return g.e(bArr);
            }
            return g.f(bArr, bArr3);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "InvalidKeyException:" + e.getMessage(), e);
            return null;
        } catch (Throwable e2) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "aesEncrypter get BadPaddingException:" + e2.getMessage(), e2);
            return null;
        } catch (Throwable e22) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "IllegalBlockSizeException:" + e22.getMessage(), e22);
            return null;
        } catch (Throwable e222) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "NoSuchAlgorithmException:" + e222.getMessage(), e222);
            return null;
        } catch (Throwable e2222) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "NoSuchPaddingException:" + e2222.getMessage(), e2222);
            return null;
        } catch (Throwable e22222) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "Exception:" + e22222.getMessage(), e22222);
            return null;
        }
    }

    public static byte[] l(byte[] bArr, byte[] bArr2) {
        try {
            return b.g(bArr2).d(bArr);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "InvalidKeyException:" + e.getMessage(), e);
            return null;
        } catch (Throwable e2) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "BadPaddingException:" + e2.getMessage(), e2);
            return null;
        } catch (Throwable e22) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "IllegalBlockSizeException:" + e22.getMessage(), e22);
            return null;
        } catch (Throwable e222) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "NoSuchAlgorithmException:" + e222.getMessage(), e222);
            return null;
        } catch (Throwable e2222) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "NoSuchPaddingException:" + e2222.getMessage(), e2222);
            return null;
        } catch (Throwable e22222) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "Exception:" + e22222.getMessage(), e22222);
            return null;
        }
    }

    public static byte[] p(byte[] bArr, String str) {
        byte[] bArr2 = null;
        try {
            return a.a(bArr, str);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "rsa encrypt data error ", e);
            return bArr2;
        }
    }

    public static String k(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return g.w(str);
    }

    public static String j(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return g.x(str);
    }

    public static String o(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return g.y(str, bArr);
    }
}
