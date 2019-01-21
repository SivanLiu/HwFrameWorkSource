package org.ifaa.android.manager;

import android.content.Context;
import android.util.Log;

public class IFAAManagerFactory {
    private static final String TAG = "IFAAManagerFactory";
    private static IFAAManagerV4Impl mImplv4;

    public static IFAAManager getIFAAManager(Context context, int authType) {
        Log.i(TAG, "IFAAManager getIFAAManager");
        Log.i(TAG, "returning v4");
        mImplv4 = new IFAAManagerV4Impl(context);
        return mImplv4;
    }
}
