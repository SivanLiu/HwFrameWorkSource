package com.huawei.android.pushagent.datatype.a;

public class d {
    private byte[] m;

    public d(byte[] bArr) {
        if (bArr != null && bArr.length > 0) {
            this.m = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.m, 0, bArr.length);
        }
    }

    public byte[] ay(int i, int i2) {
        if (i > this.m.length || i + i2 > this.m.length) {
            throw new IndexOutOfBoundsException("out of index");
        }
        byte[] bArr = new byte[i2];
        System.arraycopy(this.m, i, bArr, 0, i2);
        byte[] bArr2 = new byte[(this.m.length - i2)];
        System.arraycopy(this.m, 0, bArr2, 0, i);
        System.arraycopy(this.m, i + i2, bArr2, i, (this.m.length - i) - i2);
        this.m = bArr2;
        return bArr;
    }

    public byte[] ax(int i) {
        if (i == 0) {
            return new byte[0];
        }
        return ay(0, i);
    }
}
