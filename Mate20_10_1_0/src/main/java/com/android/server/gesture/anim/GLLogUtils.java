package com.android.server.gesture.anim;

import android.util.Log;
import com.android.server.gesture.GestureNavConst;
import com.huawei.hiai.awareness.AwarenessInnerConstants;

public class GLLogUtils {
    private static final String TAG = "GestureBackAnimation";

    private GLLogUtils() {
    }

    public static void logD(String tag, String content) {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, tag + AwarenessInnerConstants.DASH_KEY + content);
        }
    }

    public static void logV(String tag, String content) {
        if (GestureNavConst.DEBUG) {
            Log.v(TAG, tag + AwarenessInnerConstants.DASH_KEY + content);
        }
    }

    public static void logW(String tag, String content) {
        Log.w(TAG, tag + AwarenessInnerConstants.DASH_KEY + content);
    }

    public static void logE(String tag, String content) {
        Log.e(TAG, tag + AwarenessInnerConstants.DASH_KEY + content);
    }
}
