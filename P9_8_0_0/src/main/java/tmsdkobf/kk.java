package tmsdkobf;

import android.content.Context;
import android.text.TextUtils;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.b;
import tmsdk.common.utils.f;

public class kk {
    private static String TAG = "CryptorUtils";

    public static String c(Context context, String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        try {
            Object -l_3_R = TccCryptor.encrypt(context, str.getBytes("gbk"), null);
            if (-l_3_R != null) {
                return b.encodeToString(-l_3_R, 0);
            }
        } catch (Object -l_2_R) {
            f.e(TAG, "getEncodeString, UnsupportedEncodingException: " + -l_2_R);
        } catch (Object -l_2_R2) {
            f.e(TAG, "getEncodeString, Exception: " + -l_2_R2);
        }
        return null;
    }

    public static String d(Context context, String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        try {
            Object -l_3_R = TccCryptor.decrypt(context, b.decode(str, 0), null);
            if (-l_3_R != null) {
                return new String(-l_3_R, "gbk");
            }
        } catch (Object -l_2_R) {
            f.e(TAG, "getDecodeString, UnsupportedEncodingException: " + -l_2_R);
        } catch (Object -l_2_R2) {
            f.e(TAG, "getDecodeString, Exception: " + -l_2_R2);
        }
        return null;
    }
}
