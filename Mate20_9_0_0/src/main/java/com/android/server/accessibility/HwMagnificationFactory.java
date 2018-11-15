package com.android.server.accessibility;

import android.content.Context;
import android.util.Log;

public class HwMagnificationFactory {
    private static final String TAG = "HwMagnificationFactory";
    private static final Object mLock = new Object();
    private static volatile Factory obj = null;

    public interface Factory {
        IMagnificationGestureHandler getHwMagnificationGestureHandler();
    }

    public interface IMagnificationGestureHandler {
        MagnificationGestureHandler getInstance(Context context, MagnificationController magnificationController, boolean z, boolean z2);
    }

    private static Factory getImplObject() {
        if (obj == null) {
            synchronized (mLock) {
                if (obj == null) {
                    try {
                        obj = (Factory) Class.forName("com.android.server.accessibility.HwMagnificationFactoryImpl").newInstance();
                    } catch (Exception e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(": reflection exception is ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("get allimpl object = ");
            stringBuilder2.append(obj);
            Log.v(str2, stringBuilder2.toString());
        }
        return obj;
    }

    public static IMagnificationGestureHandler getHwMagnificationGestureHandler() {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwMagnificationGestureHandler();
        }
        return null;
    }
}
