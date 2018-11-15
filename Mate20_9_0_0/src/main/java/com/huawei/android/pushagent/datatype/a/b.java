package com.huawei.android.pushagent.datatype.a;

public class b {
    private byte[] cd;
    private String ce;
    private int cf;
    private int cg;
    private String ch;
    private byte[] ci;

    /* JADX WARNING: Missing block: B:4:0x0009, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public b(int i, String str, byte[] bArr, byte[] bArr2, int i2, String str2) {
        if (bArr != null && bArr.length != 0 && bArr2 != null && bArr2.length != 0) {
            this.cf = i;
            this.ch = str;
            this.ci = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.ci, 0, bArr.length);
            this.cd = new byte[bArr2.length];
            System.arraycopy(bArr2, 0, this.cd, 0, bArr2.length);
            this.cg = i2;
            this.ce = str2;
        }
    }

    public String kj() {
        return this.ch;
    }

    public byte[] getToken() {
        return this.ci;
    }

    public byte[] kf() {
        return this.cd;
    }

    public int ki() {
        return this.cg;
    }

    public String kg() {
        return this.ce;
    }

    public int kh() {
        return this.cf;
    }
}
