package com.huawei.server;

import android.os.Looper;
import com.android.server.ServiceThread;

public class ServiceThreadExt {
    private ServiceThread mServiceThread;

    public ServiceThreadExt(String tag, int priority, boolean allowIo) {
        this.mServiceThread = new ServiceThread(tag, priority, allowIo);
    }

    public void start() {
        this.mServiceThread.start();
    }

    public Looper getLooper() {
        return this.mServiceThread.getLooper();
    }
}
