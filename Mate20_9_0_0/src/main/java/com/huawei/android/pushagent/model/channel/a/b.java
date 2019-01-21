package com.huawei.android.pushagent.model.channel.a;

import com.huawei.android.pushagent.utils.b.a;
import java.io.InputStream;

class b extends InputStream {
    private byte[] bu = null;
    private int bv = 0;
    private InputStream bw;
    final /* synthetic */ a bx;

    public b(a aVar, InputStream inputStream) {
        this.bx = aVar;
        this.bw = inputStream;
    }

    /* JADX WARNING: Missing block: B:10:0x001c, code skipped:
            if (r5.bu == null) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:12:0x0021, code skipped:
            if (r5.bu.length <= 0) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:14:0x0028, code skipped:
            if (r5.bv >= r5.bu.length) goto L_0x003a;
     */
    /* JADX WARNING: Missing block: B:15:0x002a, code skipped:
            r0 = r5.bu;
            r1 = r5.bv;
            r5.bv = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:16:0x0036, code skipped:
            return r0[r1] & 255;
     */
    /* JADX WARNING: Missing block: B:20:0x003a, code skipped:
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "bufferByte has read end , need read bytes from socket");
     */
    /* JADX WARNING: Missing block: B:21:0x0043, code skipped:
            r5.bu = null;
            r5.bv = 0;
     */
    /* JADX WARNING: Missing block: B:22:0x0049, code skipped:
            if (r5.bw == null) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:23:0x004b, code skipped:
            r0 = r5.bw.read();
     */
    /* JADX WARNING: Missing block: B:24:0x0051, code skipped:
            if (-1 != r0) goto L_0x005d;
     */
    /* JADX WARNING: Missing block: B:25:0x0053, code skipped:
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "read -1 from inputstream");
     */
    /* JADX WARNING: Missing block: B:26:0x005c, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:28:0x005f, code skipped:
            if (48 != r0) goto L_0x0096;
     */
    /* JADX WARNING: Missing block: B:29:0x0061, code skipped:
            r5.bu = com.huawei.android.pushagent.utils.e.a.vw(com.huawei.android.pushagent.model.channel.a.a.dm(r5.bw), com.huawei.android.pushagent.model.channel.a.a.bt);
     */
    /* JADX WARNING: Missing block: B:30:0x0073, code skipped:
            if (r5.bu == null) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:32:0x0078, code skipped:
            if (r5.bu.length != 0) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:33:0x007a, code skipped:
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "ase decrypt serverkey error");
            com.huawei.android.pushagent.b.a.abd(87);
     */
    /* JADX WARNING: Missing block: B:34:0x0088, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:35:0x0089, code skipped:
            r0 = r5.bu;
            r1 = r5.bv;
            r5.bv = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:36:0x0095, code skipped:
            return r0[r1] & 255;
     */
    /* JADX WARNING: Missing block: B:37:0x0096, code skipped:
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "read secure message error, return -1, cmdId: " + com.huawei.android.pushagent.utils.b.c.ts((byte) r0));
            com.huawei.android.pushagent.b.a.abd(88);
     */
    /* JADX WARNING: Missing block: B:38:0x00ba, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:39:0x00bb, code skipped:
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "secureInputStream is null, return -1");
     */
    /* JADX WARNING: Missing block: B:40:0x00c4, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read() {
        synchronized (this.bx) {
            if (!this.bx.bo) {
                a.su("PushLog3414", "secure socket is not initialized, can not read any data");
                return -1;
            }
        }
    }
}
