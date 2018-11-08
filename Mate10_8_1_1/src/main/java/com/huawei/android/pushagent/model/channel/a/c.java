package com.huawei.android.pushagent.model.channel.a;

import com.huawei.android.pushagent.utils.a.b;
import java.io.InputStream;

class c extends InputStream {
    private byte[] gu = null;
    private int gv = 0;
    private InputStream gw;
    final /* synthetic */ b gx;

    public c(b bVar, InputStream inputStream) {
        this.gx = bVar;
        this.gw = inputStream;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read() {
        synchronized (this.gx) {
            if (!this.gx.go) {
                b.y("PushLog2976", "secure socket is not initialized, can not read any data");
                return -1;
            }
        }
    }
}
