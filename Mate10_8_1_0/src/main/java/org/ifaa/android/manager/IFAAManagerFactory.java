package org.ifaa.android.manager;

import android.content.Context;
import android.util.Log;
import huawei.android.hardware.fingerprint.FingerprintManagerEx;

public class IFAAManagerFactory {
    private static final String TAG = "IFAAManagerFactory";
    private static IFAAManagerV2Impl mImplv2 = new IFAAManagerV2Impl();
    private static IFAAManagerV3Impl mImplv3;

    public static IFAAManager getIFAAManager(Context context, int authType) {
        Log.i(TAG, "IFAAManager getIFAAManager");
        if (FingerprintManagerEx.hasFingerprintInScreen()) {
            Log.i(TAG, "returning v3");
            mImplv3 = new IFAAManagerV3Impl(context);
            return mImplv3;
        }
        Log.i(TAG, "returning v2");
        return mImplv2;
    }
}
