package com.huawei.android.pushagent.model.channel.a;

import com.huawei.android.pushagent.utils.f.c;
import java.io.InputStream;

class b extends InputStream {
    private byte[] ep = null;
    private int eq = 0;
    private InputStream er;
    final /* synthetic */ a es;

    public b(a aVar, InputStream inputStream) {
        this.es = aVar;
        this.er = inputStream;
    }

    /* JADX WARNING: Missing block: B:10:0x001c, code:
            if (r5.ep == null) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:12:0x0021, code:
            if (r5.ep.length <= 0) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:14:0x0028, code:
            if (r5.eq >= r5.ep.length) goto L_0x003a;
     */
    /* JADX WARNING: Missing block: B:15:0x002a, code:
            r0 = r5.ep;
            r1 = r5.eq;
            r5.eq = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:16:0x0036, code:
            return r0[r1] & 255;
     */
    /* JADX WARNING: Missing block: B:20:0x003a, code:
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "bufferByte has read end , need read bytes from socket");
     */
    /* JADX WARNING: Missing block: B:21:0x0043, code:
            r5.ep = null;
            r5.eq = 0;
     */
    /* JADX WARNING: Missing block: B:22:0x0049, code:
            if (r5.er == null) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:23:0x004b, code:
            r0 = r5.er.read();
     */
    /* JADX WARNING: Missing block: B:24:0x0051, code:
            if (-1 != r0) goto L_0x005d;
     */
    /* JADX WARNING: Missing block: B:25:0x0053, code:
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "read -1 from inputstream");
     */
    /* JADX WARNING: Missing block: B:26:0x005c, code:
            return -1;
     */
    /* JADX WARNING: Missing block: B:28:0x005f, code:
            if (48 != r0) goto L_0x0096;
     */
    /* JADX WARNING: Missing block: B:29:0x0061, code:
            r5.ep = com.huawei.android.pushagent.utils.a.c.l(com.huawei.android.pushagent.model.channel.a.a.nr(r5.er), com.huawei.android.pushagent.model.channel.a.a.nq());
     */
    /* JADX WARNING: Missing block: B:30:0x0073, code:
            if (r5.ep == null) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:32:0x0078, code:
            if (r5.ep.length != 0) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:33:0x007a, code:
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "ase decrypt serverkey error");
            com.huawei.android.pushagent.a.a.hx(87);
     */
    /* JADX WARNING: Missing block: B:34:0x0088, code:
            return -1;
     */
    /* JADX WARNING: Missing block: B:35:0x0089, code:
            r0 = r5.ep;
            r1 = r5.eq;
            r5.eq = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:36:0x0095, code:
            return r0[r1] & 255;
     */
    /* JADX WARNING: Missing block: B:37:0x0096, code:
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "read secure message error, return -1, cmdId: " + com.huawei.android.pushagent.utils.f.b.em((byte) r0));
            com.huawei.android.pushagent.a.a.hx(88);
     */
    /* JADX WARNING: Missing block: B:38:0x00ba, code:
            return -1;
     */
    /* JADX WARNING: Missing block: B:39:0x00bb, code:
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "secureInputStream is null, return -1");
     */
    /* JADX WARNING: Missing block: B:40:0x00c4, code:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read() {
        synchronized (this.es) {
            if (!this.es.ej) {
                c.eq("PushLog3413", "secure socket is not initialized, can not read any data");
                return -1;
            }
        }
    }
}
