package com.huawei.android.pushagent.datatype.a;

public class d {
    private byte[] cl;

    public d(byte[] bArr) {
        if (bArr != null && bArr.length > 0) {
            this.cl = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.cl, 0, bArr.length);
        }
    }

    public byte[] kq(int i, int i2) {
        if (i > this.cl.length || i + i2 > this.cl.length) {
            throw new IndexOutOfBoundsException("out of index");
        }
        byte[] bArr = new byte[i2];
        System.arraycopy(this.cl, i, bArr, 0, i2);
        byte[] bArr2 = new byte[(this.cl.length - i2)];
        System.arraycopy(this.cl, 0, bArr2, 0, i);
        System.arraycopy(this.cl, i + i2, bArr2, i, (this.cl.length - i) - i2);
        this.cl = bArr2;
        return bArr;
    }

    public byte[] kp(int i) {
        if (i == 0) {
            return new byte[0];
        }
        return kq(0, i);
    }
}
