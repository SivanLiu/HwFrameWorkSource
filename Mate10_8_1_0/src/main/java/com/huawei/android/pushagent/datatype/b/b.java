package com.huawei.android.pushagent.datatype.b;

public class b {
    private byte[] hl;
    private String hm;
    private int hn;
    private int ho;
    private String hp;
    private byte[] hq;

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public b(int i, String str, byte[] bArr, byte[] bArr2, int i2, String str2) {
        if (bArr != null && bArr.length != 0 && bArr2 != null && bArr2.length != 0) {
            this.hn = i;
            this.hp = str;
            this.hq = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.hq, 0, bArr.length);
            this.hl = new byte[bArr2.length];
            System.arraycopy(bArr2, 0, this.hl, 0, bArr2.length);
            this.ho = i2;
            this.hm = str2;
        }
    }

    public String yb() {
        return this.hp;
    }

    public byte[] getToken() {
        return this.hq;
    }

    public byte[] xz() {
        return this.hl;
    }

    public int yc() {
        return this.ho;
    }

    public String yd() {
        return this.hm;
    }

    public int ya() {
        return this.hn;
    }
}
