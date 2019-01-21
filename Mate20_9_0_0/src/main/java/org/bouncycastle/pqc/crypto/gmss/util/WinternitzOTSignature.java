package org.bouncycastle.pqc.crypto.gmss.util;

import java.lang.reflect.Array;
import org.bouncycastle.crypto.Digest;

public class WinternitzOTSignature {
    private int checksumsize;
    private GMSSRandom gmssRandom = new GMSSRandom(this.messDigestOTS);
    private int keysize;
    private int mdsize = this.messDigestOTS.getDigestSize();
    private Digest messDigestOTS;
    private int messagesize;
    private byte[][] privateKeyOTS;
    private int w;

    public WinternitzOTSignature(byte[] bArr, Digest digest, int i) {
        this.w = i;
        this.messDigestOTS = digest;
        double d = (double) i;
        this.messagesize = (int) Math.ceil(((double) (this.mdsize << 3)) / d);
        this.checksumsize = getLog((this.messagesize << i) + 1);
        this.keysize = this.messagesize + ((int) Math.ceil(((double) this.checksumsize) / d));
        this.privateKeyOTS = (byte[][]) Array.newInstance(byte.class, new int[]{this.keysize, this.mdsize});
        byte[] bArr2 = new byte[this.mdsize];
        int i2 = 0;
        System.arraycopy(bArr, 0, bArr2, 0, bArr2.length);
        while (i2 < this.keysize) {
            this.privateKeyOTS[i2] = this.gmssRandom.nextSeed(bArr2);
            i2++;
        }
    }

    public int getLog(int i) {
        int i2 = 1;
        int i3 = 2;
        while (i3 < i) {
            i3 <<= 1;
            i2++;
        }
        return i2;
    }

    public byte[][] getPrivateKey() {
        return this.privateKeyOTS;
    }

    public byte[] getPublicKey() {
        byte[] bArr = new byte[(this.keysize * this.mdsize)];
        byte[] bArr2 = new byte[this.mdsize];
        int i = 1 << this.w;
        for (int i2 = 0; i2 < this.keysize; i2++) {
            this.messDigestOTS.update(this.privateKeyOTS[i2], 0, this.privateKeyOTS[i2].length);
            byte[] bArr3 = new byte[this.messDigestOTS.getDigestSize()];
            this.messDigestOTS.doFinal(bArr3, 0);
            for (int i3 = 2; i3 < i; i3++) {
                this.messDigestOTS.update(bArr3, 0, bArr3.length);
                bArr3 = new byte[this.messDigestOTS.getDigestSize()];
                this.messDigestOTS.doFinal(bArr3, 0);
            }
            System.arraycopy(bArr3, 0, bArr, this.mdsize * i2, this.mdsize);
        }
        this.messDigestOTS.update(bArr, 0, bArr.length);
        bArr = new byte[this.messDigestOTS.getDigestSize()];
        this.messDigestOTS.doFinal(bArr, 0);
        return bArr;
    }

    public byte[] getSignature(byte[] bArr) {
        byte[] bArr2 = bArr;
        byte[] bArr3 = new byte[(this.keysize * this.mdsize)];
        byte[] bArr4 = new byte[this.mdsize];
        this.messDigestOTS.update(bArr2, 0, bArr2.length);
        bArr2 = new byte[this.messDigestOTS.getDigestSize()];
        this.messDigestOTS.doFinal(bArr2, 0);
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        Object obj;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        if (8 % this.w == 0) {
            i = 8 / this.w;
            i2 = (1 << this.w) - 1;
            i3 = 0;
            i4 = i3;
            Object obj2 = new byte[this.mdsize];
            i5 = i4;
            while (i5 < bArr2.length) {
                obj = obj2;
                i6 = i4;
                i4 = i3;
                for (i3 = 0; i3 < i; i3++) {
                    i7 = bArr2[i5] & i2;
                    i4 += i7;
                    System.arraycopy(this.privateKeyOTS[i6], 0, obj, 0, this.mdsize);
                    while (i7 > 0) {
                        this.messDigestOTS.update(obj, 0, obj.length);
                        obj = new byte[this.messDigestOTS.getDigestSize()];
                        this.messDigestOTS.doFinal(obj, 0);
                        i7--;
                    }
                    System.arraycopy(obj, 0, bArr3, this.mdsize * i6, this.mdsize);
                    bArr2[i5] = (byte) (bArr2[i5] >>> this.w);
                    i6++;
                }
                i5++;
                i3 = i4;
                i4 = i6;
                obj2 = obj;
            }
            i = (this.messagesize << this.w) - i3;
            for (i8 = 0; i8 < this.checksumsize; i8 += this.w) {
                System.arraycopy(this.privateKeyOTS[i4], 0, obj2, 0, this.mdsize);
                for (i5 = i & i2; i5 > 0; i5--) {
                    this.messDigestOTS.update(obj2, 0, obj2.length);
                    obj2 = new byte[this.messDigestOTS.getDigestSize()];
                    this.messDigestOTS.doFinal(obj2, 0);
                }
                System.arraycopy(obj2, 0, bArr3, this.mdsize * i4, this.mdsize);
                i >>>= this.w;
                i4++;
            }
        } else if (this.w < 8) {
            i2 = this.mdsize / this.w;
            i6 = (1 << this.w) - 1;
            i7 = 0;
            int i11 = i7;
            i9 = i11;
            Object obj3 = new byte[this.mdsize];
            int i12 = i9;
            while (i12 < i2) {
                int i13 = i7;
                long j = 0;
                for (i7 = 0; i7 < this.w; i7++) {
                    j ^= (long) ((bArr2[i13] & 255) << (i7 << 3));
                    i13++;
                }
                i3 = 0;
                while (i3 < 8) {
                    i10 = i3;
                    i5 = (int) (((long) i6) & j);
                    i9 += i5;
                    System.arraycopy(this.privateKeyOTS[i11], 0, obj3, 0, this.mdsize);
                    while (i5 > 0) {
                        this.messDigestOTS.update(obj3, 0, obj3.length);
                        obj3 = new byte[this.messDigestOTS.getDigestSize()];
                        this.messDigestOTS.doFinal(obj3, 0);
                        i5--;
                    }
                    System.arraycopy(obj3, 0, bArr3, this.mdsize * i11, this.mdsize);
                    j >>>= this.w;
                    i11++;
                    i3 = i10 + 1;
                }
                i12++;
                i7 = i13;
            }
            i2 = this.mdsize % this.w;
            long j2 = 0;
            for (i = 0; i < i2; i++) {
                j2 ^= (long) ((bArr2[i7] & 255) << (i << 3));
                i7++;
            }
            i8 = i2 << 3;
            for (i2 = 0; i2 < i8; i2 += this.w) {
                i = (int) (j2 & ((long) i6));
                i9 += i;
                System.arraycopy(this.privateKeyOTS[i11], 0, obj3, 0, this.mdsize);
                while (i > 0) {
                    this.messDigestOTS.update(obj3, 0, obj3.length);
                    obj3 = new byte[this.messDigestOTS.getDigestSize()];
                    this.messDigestOTS.doFinal(obj3, 0);
                    i--;
                }
                System.arraycopy(obj3, 0, bArr3, this.mdsize * i11, this.mdsize);
                j2 >>>= this.w;
                i11++;
            }
            i2 = (this.messagesize << this.w) - i9;
            for (i8 = 0; i8 < this.checksumsize; i8 += this.w) {
                System.arraycopy(this.privateKeyOTS[i11], 0, obj3, 0, this.mdsize);
                for (i = i2 & i6; i > 0; i--) {
                    this.messDigestOTS.update(obj3, 0, obj3.length);
                    obj3 = new byte[this.messDigestOTS.getDigestSize()];
                    this.messDigestOTS.doFinal(obj3, 0);
                }
                System.arraycopy(obj3, 0, bArr3, this.mdsize * i11, this.mdsize);
                i2 >>>= this.w;
                i11++;
            }
        } else if (this.w < 57) {
            long j3;
            i2 = (this.mdsize << 3) - this.w;
            i5 = (1 << this.w) - 1;
            i4 = 0;
            i6 = i4;
            obj = new byte[this.mdsize];
            i3 = i6;
            while (i3 <= i2) {
                int i14 = i3 % 8;
                i3 += this.w;
                i10 = 0;
                long j4 = 0;
                for (i9 = i3 >>> 3; i9 < ((i3 + 7) >>> 3); i9++) {
                    j4 ^= (long) ((bArr2[i9] & 255) << (i10 << 3));
                    i10++;
                }
                j3 = (j4 >>> i14) & ((long) i5);
                i4 = (int) (((long) i4) + j3);
                System.arraycopy(this.privateKeyOTS[i6], 0, obj, 0, this.mdsize);
                while (j3 > 0) {
                    this.messDigestOTS.update(obj, 0, obj.length);
                    obj = new byte[this.messDigestOTS.getDigestSize()];
                    this.messDigestOTS.doFinal(obj, 0);
                    j3--;
                }
                System.arraycopy(obj, 0, bArr3, this.mdsize * i6, this.mdsize);
                i6++;
            }
            i2 = i3 >>> 3;
            if (i2 < this.mdsize) {
                i3 %= 8;
                i = 0;
                j3 = 0;
                while (i2 < this.mdsize) {
                    j3 ^= (long) ((bArr2[i2] & 255) << (i << 3));
                    i++;
                    i2++;
                }
                long j5 = (j3 >>> i3) & ((long) i5);
                i4 = (int) (((long) i4) + j5);
                System.arraycopy(this.privateKeyOTS[i6], 0, obj, 0, this.mdsize);
                while (j5 > 0) {
                    this.messDigestOTS.update(obj, 0, obj.length);
                    obj = new byte[this.messDigestOTS.getDigestSize()];
                    this.messDigestOTS.doFinal(obj, 0);
                    j5--;
                }
                System.arraycopy(obj, 0, bArr3, this.mdsize * i6, this.mdsize);
                i6++;
            }
            i2 = (this.messagesize << this.w) - i4;
            for (i8 = 0; i8 < this.checksumsize; i8 += this.w) {
                System.arraycopy(this.privateKeyOTS[i6], 0, obj, 0, this.mdsize);
                for (long j6 = (long) (i2 & i5); j6 > 0; j6--) {
                    this.messDigestOTS.update(obj, 0, obj.length);
                    obj = new byte[this.messDigestOTS.getDigestSize()];
                    this.messDigestOTS.doFinal(obj, 0);
                }
                System.arraycopy(obj, 0, bArr3, this.mdsize * i6, this.mdsize);
                i2 >>>= this.w;
                i6++;
            }
        }
        return bArr3;
    }
}
