package com.huawei.android.pushagent.utils.e;

import android.text.TextUtils;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class a {
    public static byte[] vx(byte[] bArr, byte[] bArr2) {
        return vy(bArr, bArr2, null);
    }

    public static byte[] vy(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        try {
            b wa = b.wa(bArr2);
            if (bArr3 == null) {
                return wa.wb(bArr);
            }
            return wa.wc(bArr, bArr3);
        } catch (InvalidKeyException e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "InvalidKeyException:" + e.getMessage(), e);
            return null;
        } catch (BadPaddingException e2) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "aesEncrypter get BadPaddingException:" + e2.getMessage(), e2);
            return null;
        } catch (IllegalBlockSizeException e3) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "IllegalBlockSizeException:" + e3.getMessage(), e3);
            return null;
        } catch (NoSuchAlgorithmException e4) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "NoSuchAlgorithmException:" + e4.getMessage(), e4);
            return null;
        } catch (NoSuchPaddingException e5) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "NoSuchPaddingException:" + e5.getMessage(), e5);
            return null;
        } catch (Exception e6) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "Exception:" + e6.getMessage(), e6);
            return null;
        }
    }

    public static byte[] vw(byte[] bArr, byte[] bArr2) {
        try {
            return b.wa(bArr2).wd(bArr);
        } catch (InvalidKeyException e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "InvalidKeyException:" + e.getMessage(), e);
            return null;
        } catch (BadPaddingException e2) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "BadPaddingException:" + e2.getMessage(), e2);
            return null;
        } catch (IllegalBlockSizeException e3) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "IllegalBlockSizeException:" + e3.getMessage(), e3);
            return null;
        } catch (NoSuchAlgorithmException e4) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "NoSuchAlgorithmException:" + e4.getMessage(), e4);
            return null;
        } catch (NoSuchPaddingException e5) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "NoSuchPaddingException:" + e5.getMessage(), e5);
            return null;
        } catch (Exception e6) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "Exception:" + e6.getMessage(), e6);
            return null;
        }
    }

    public static byte[] vz(byte[] bArr, String str) {
        byte[] bArr2 = null;
        try {
            return g.wx(bArr, str);
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "rsa encrypt data error ", e);
            return bArr2;
        }
    }

    public static String vu(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return e.wl(str);
    }

    public static String vt(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return e.wm(str);
    }

    public static String vv(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return e.wn(str, bArr);
    }
}
