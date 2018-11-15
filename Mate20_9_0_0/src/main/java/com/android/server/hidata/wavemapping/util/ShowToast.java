package com.android.server.hidata.wavemapping.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.android.server.hidata.wavemapping.cons.ContextManager;

public class ShowToast {
    private static final String TAG;
    private static boolean isShow = false;
    private Context mCtx;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(ShowToast.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public static void showToast(String info) {
        try {
            if (isShow) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("showToast:");
                stringBuilder.append(info);
                LogUtil.i(stringBuilder.toString());
                Toast.makeText(ContextManager.getInstance().getContext(), info, 1).show();
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("showToast,e:");
            stringBuilder2.append(e);
            Log.e(str, stringBuilder2.toString());
        }
    }

    public static boolean isIsShow() {
        return isShow;
    }

    public static void setIsShow(boolean isShow) {
        isShow = isShow;
    }
}
