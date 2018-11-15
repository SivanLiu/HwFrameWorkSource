package com.huawei.android.pushagent.datatype.b;

public class a {
    private byte[] hk;

    public a(byte[] bArr) {
        if (bArr != null && bArr.length > 0) {
            this.hk = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.hk, 0, bArr.length);
        }
    }

    public byte[] xy(int i, int i2) {
        if (i > this.hk.length || i + i2 > this.hk.length) {
            throw new IndexOutOfBoundsException("out of index");
        }
        byte[] bArr = new byte[i2];
        System.arraycopy(this.hk, i, bArr, 0, i2);
        byte[] bArr2 = new byte[(this.hk.length - i2)];
        System.arraycopy(this.hk, 0, bArr2, 0, i);
        System.arraycopy(this.hk, i + i2, bArr2, i, (this.hk.length - i) - i2);
        this.hk = bArr2;
        return bArr;
    }

    public byte[] xx(int i) {
        if (i == 0) {
            return new byte[0];
        }
        return xy(0, i);
    }
}
