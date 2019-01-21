package org.ifaa.android.manager.face;

import android.content.Context;
import android.util.Log;

public class IFAAFaceManagerFactory {
    private static final String TAG = "IFAAFaceManagerFactory";
    private static IFAAFaceManagerV1Impl mFaceImplV1;

    public static IFAAFaceManager getIFAAFaceManager(Context context) {
        Log.i(TAG, "IFAAManager getIFAAFaceManager");
        if (mFaceImplV1 == null) {
            mFaceImplV1 = new IFAAFaceManagerV1Impl(context);
        }
        return mFaceImplV1;
    }
}
