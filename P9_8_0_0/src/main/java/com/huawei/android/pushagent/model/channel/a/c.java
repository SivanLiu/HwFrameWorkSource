package com.huawei.android.pushagent.model.channel.a;

import java.io.InputStream;

class c extends InputStream {
    private byte[] bt = null;
    private int bu = 0;
    private InputStream bv;
    final /* synthetic */ b bw;

    public c(b bVar, InputStream inputStream) {
        this.bw = bVar;
        this.bv = inputStream;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read() {
        synchronized (this.bw) {
            if (!this.bw.bn) {
                com.huawei.android.pushagent.utils.d.c.sf("PushLog2951", "secure socket is not initialized, can not read any data");
                return -1;
            }
        }
    }
}
