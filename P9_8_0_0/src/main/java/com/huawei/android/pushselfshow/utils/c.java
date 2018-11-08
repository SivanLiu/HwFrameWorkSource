package com.huawei.android.pushselfshow.utils;

import android.os.Handler;
import android.os.Message;
import java.lang.ref.WeakReference;

public class c extends Handler {
    private WeakReference a;

    public interface a {
        void handleMessage(Message message);
    }

    public c(a aVar) {
        this.a = new WeakReference(aVar);
    }

    public void handleMessage(Message message) {
        super.handleMessage(message);
        a -l_2_R = (a) this.a.get();
        if (-l_2_R != null) {
            -l_2_R.handleMessage(message);
        }
    }
}
