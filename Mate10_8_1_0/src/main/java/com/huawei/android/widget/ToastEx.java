package com.huawei.android.widget;

import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

public class ToastEx {
    private static final LayoutParams DEFAULT_VALUE = new LayoutParams();

    public static LayoutParams getWindowParams(Toast toast) {
        if (toast == null) {
            return DEFAULT_VALUE;
        }
        return toast.getWindowParams();
    }
}
