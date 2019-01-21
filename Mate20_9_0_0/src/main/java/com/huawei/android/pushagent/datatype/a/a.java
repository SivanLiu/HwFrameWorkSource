package com.huawei.android.pushagent.datatype.a;

public class a {
    private byte[] b;
    private String c;
    private int d;
    private int e;
    private String f;
    private byte[] g;

    /* JADX WARNING: Missing block: B:4:0x0009, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public a(int i, String str, byte[] bArr, byte[] bArr2, int i2, String str2) {
        if (bArr != null && bArr.length != 0 && bArr2 != null && bArr2.length != 0) {
            this.d = i;
            this.f = str;
            this.g = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.g, 0, bArr.length);
            this.b = new byte[bArr2.length];
            System.arraycopy(bArr2, 0, this.b, 0, bArr2.length);
            this.e = i2;
            this.c = str2;
        }
    }

    public String am() {
        return this.f;
    }

    public byte[] getToken() {
        return this.g;
    }

    public byte[] ai() {
        return this.b;
    }

    public int al() {
        return this.e;
    }

    public String aj() {
        return this.c;
    }

    public int ak() {
        return this.d;
    }
}
