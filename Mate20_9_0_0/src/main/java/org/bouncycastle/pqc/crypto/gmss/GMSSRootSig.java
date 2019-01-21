package org.bouncycastle.pqc.crypto.gmss;

import java.lang.reflect.Array;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.pqc.crypto.gmss.util.GMSSRandom;
import org.bouncycastle.util.encoders.Hex;

public class GMSSRootSig {
    private long big8;
    private int checksum;
    private int counter;
    private GMSSRandom gmssRandom;
    private byte[] hash;
    private int height;
    private int ii;
    private int k;
    private int keysize;
    private int mdsize;
    private Digest messDigestOTS;
    private int messagesize;
    private byte[] privateKeyOTS;
    private int r;
    private byte[] seed;
    private byte[] sign;
    private int steps;
    private int test;
    private long test8;
    private int w;

    public GMSSRootSig(Digest digest, int i, int i2) {
        this.messDigestOTS = digest;
        this.gmssRandom = new GMSSRandom(this.messDigestOTS);
        this.mdsize = this.messDigestOTS.getDigestSize();
        this.w = i;
        this.height = i2;
        this.k = (1 << i) - 1;
        this.messagesize = (int) Math.ceil(((double) (this.mdsize << 3)) / ((double) i));
    }

    public GMSSRootSig(Digest digest, byte[][] bArr, int[] iArr) {
        this.messDigestOTS = digest;
        this.gmssRandom = new GMSSRandom(this.messDigestOTS);
        this.counter = iArr[0];
        this.test = iArr[1];
        this.ii = iArr[2];
        this.r = iArr[3];
        this.steps = iArr[4];
        this.keysize = iArr[5];
        this.height = iArr[6];
        this.w = iArr[7];
        this.checksum = iArr[8];
        this.mdsize = this.messDigestOTS.getDigestSize();
        this.k = (1 << this.w) - 1;
        this.messagesize = (int) Math.ceil(((double) (this.mdsize << 3)) / ((double) this.w));
        this.privateKeyOTS = bArr[0];
        this.seed = bArr[1];
        this.hash = bArr[2];
        this.sign = bArr[3];
        this.test8 = ((((((((long) (bArr[4][2] & 255)) << 16) | (((long) (bArr[4][0] & 255)) | (((long) (bArr[4][1] & 255)) << 8))) | (((long) (bArr[4][3] & 255)) << 24)) | (((long) (bArr[4][4] & 255)) << 32)) | (((long) (bArr[4][5] & 255)) << 40)) | (((long) (bArr[4][6] & 255)) << 48)) | (((long) (bArr[4][7] & 255)) << 56);
        this.big8 = (((long) (bArr[4][15] & 255)) << 56) | ((((((((long) (bArr[4][8] & 255)) | (((long) (bArr[4][9] & 255)) << 8)) | (((long) (bArr[4][10] & 255)) << 16)) | (((long) (bArr[4][11] & 255)) << 24)) | (((long) (bArr[4][12] & 255)) << 32)) | (((long) (bArr[4][13] & 255)) << 40)) | (((long) (bArr[4][14] & 255)) << 48));
    }

    private void oneStep() {
        if (8 % this.w == 0) {
            if (this.test == 0) {
                this.privateKeyOTS = this.gmssRandom.nextSeed(this.seed);
                if (this.ii < this.mdsize) {
                    this.test = this.hash[this.ii] & this.k;
                    this.hash[this.ii] = (byte) (this.hash[this.ii] >>> this.w);
                } else {
                    this.test = this.checksum & this.k;
                    this.checksum >>>= this.w;
                }
            } else if (this.test > 0) {
                this.messDigestOTS.update(this.privateKeyOTS, 0, this.privateKeyOTS.length);
                this.privateKeyOTS = new byte[this.messDigestOTS.getDigestSize()];
                this.messDigestOTS.doFinal(this.privateKeyOTS, 0);
                this.test--;
            }
            if (this.test == 0) {
                System.arraycopy(this.privateKeyOTS, 0, this.sign, this.counter * this.mdsize, this.mdsize);
                this.counter++;
                if (this.counter % (8 / this.w) == 0) {
                    this.ii++;
                    return;
                }
            }
        }
        int i;
        if (this.w < 8) {
            if (this.test == 0) {
                if (this.counter % 8 == 0 && this.ii < this.mdsize) {
                    this.big8 = 0;
                    if (this.counter < ((this.mdsize / this.w) << 3)) {
                        for (i = 0; i < this.w; i++) {
                            this.big8 ^= (long) ((this.hash[this.ii] & 255) << (i << 3));
                            this.ii++;
                        }
                    } else {
                        for (i = 0; i < this.mdsize % this.w; i++) {
                            this.big8 ^= (long) ((this.hash[this.ii] & 255) << (i << 3));
                            this.ii++;
                        }
                    }
                }
                if (this.counter == this.messagesize) {
                    this.big8 = (long) this.checksum;
                }
                this.test = (int) (this.big8 & ((long) this.k));
                this.privateKeyOTS = this.gmssRandom.nextSeed(this.seed);
            } else if (this.test > 0) {
                this.messDigestOTS.update(this.privateKeyOTS, 0, this.privateKeyOTS.length);
                this.privateKeyOTS = new byte[this.messDigestOTS.getDigestSize()];
                this.messDigestOTS.doFinal(this.privateKeyOTS, 0);
                this.test--;
            }
            if (this.test == 0) {
                System.arraycopy(this.privateKeyOTS, 0, this.sign, this.counter * this.mdsize, this.mdsize);
                this.big8 >>>= this.w;
            }
        } else if (this.w < 57) {
            if (this.test8 == 0) {
                this.big8 = 0;
                this.ii = 0;
                i = this.r % 8;
                int i2 = this.r >>> 3;
                if (i2 < this.mdsize) {
                    int i3;
                    if (this.r <= (this.mdsize << 3) - this.w) {
                        this.r += this.w;
                        i3 = (this.r + 7) >>> 3;
                    } else {
                        i3 = this.mdsize;
                        this.r += this.w;
                    }
                    while (i2 < i3) {
                        this.big8 ^= (long) ((this.hash[i2] & 255) << (this.ii << 3));
                        this.ii++;
                        i2++;
                    }
                    this.big8 >>>= i;
                    this.test8 = this.big8 & ((long) this.k);
                } else {
                    this.test8 = (long) (this.checksum & this.k);
                    this.checksum >>>= this.w;
                }
                this.privateKeyOTS = this.gmssRandom.nextSeed(this.seed);
            } else if (this.test8 > 0) {
                this.messDigestOTS.update(this.privateKeyOTS, 0, this.privateKeyOTS.length);
                this.privateKeyOTS = new byte[this.messDigestOTS.getDigestSize()];
                this.messDigestOTS.doFinal(this.privateKeyOTS, 0);
                this.test8--;
            }
            if (this.test8 == 0) {
                System.arraycopy(this.privateKeyOTS, 0, this.sign, this.counter * this.mdsize, this.mdsize);
            }
        }
        this.counter++;
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

    public byte[] getSig() {
        return this.sign;
    }

    public byte[][] getStatByte() {
        byte[][] bArr = (byte[][]) Array.newInstance(byte.class, new int[]{5, this.mdsize});
        bArr[0] = this.privateKeyOTS;
        bArr[1] = this.seed;
        bArr[2] = this.hash;
        bArr[3] = this.sign;
        bArr[4] = getStatLong();
        return bArr;
    }

    public int[] getStatInt() {
        return new int[]{this.counter, this.test, this.ii, this.r, this.steps, this.keysize, this.height, this.w, this.checksum};
    }

    public byte[] getStatLong() {
        return new byte[]{(byte) ((int) (this.test8 & 255)), (byte) ((int) ((this.test8 >> 8) & 255)), (byte) ((int) ((this.test8 >> 16) & 255)), (byte) ((int) ((this.test8 >> 24) & 255)), (byte) ((int) ((this.test8 >> 32) & 255)), (byte) ((int) ((this.test8 >> 40) & 255)), (byte) ((int) ((this.test8 >> 48) & 255)), (byte) ((int) ((this.test8 >> 56) & 255)), (byte) ((int) (this.big8 & 255)), (byte) ((int) ((this.big8 >> 8) & 255)), (byte) ((int) ((this.big8 >> 16) & 255)), (byte) ((int) ((this.big8 >> 24) & 255)), (byte) ((int) ((this.big8 >> 32) & 255)), (byte) ((int) ((this.big8 >> 40) & 255)), (byte) ((int) ((this.big8 >> 48) & 255)), (byte) ((int) ((this.big8 >> 56) & 255))};
    }

    public void initSign(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = bArr2;
        this.hash = new byte[this.mdsize];
        int i = 0;
        this.messDigestOTS.update(bArr3, 0, bArr3.length);
        this.hash = new byte[this.messDigestOTS.getDigestSize()];
        this.messDigestOTS.doFinal(this.hash, 0);
        bArr3 = new byte[this.mdsize];
        System.arraycopy(this.hash, 0, bArr3, 0, this.mdsize);
        int log = getLog((this.messagesize << this.w) + 1);
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        if (8 % this.w == 0) {
            i2 = 8 / this.w;
            i3 = 0;
            i4 = i3;
            while (i3 < this.mdsize) {
                i5 = i4;
                for (i4 = 0; i4 < i2; i4++) {
                    i5 += bArr3[i3] & this.k;
                    bArr3[i3] = (byte) (bArr3[i3] >>> this.w);
                }
                i3++;
                i4 = i5;
            }
            this.checksum = (this.messagesize << this.w) - i4;
            i3 = this.checksum;
            for (i6 = 0; i6 < log; i6 += this.w) {
                i4 += this.k & i3;
                i3 >>>= this.w;
            }
            i = i4;
        } else if (this.w < 8) {
            i3 = this.mdsize / this.w;
            i4 = 0;
            i5 = i4;
            int i8 = i5;
            while (i4 < i3) {
                int i9 = i5;
                long j = 0;
                for (i5 = i; i5 < this.w; i5++) {
                    j ^= (long) ((bArr3[i9] & 255) << (i5 << 3));
                    i9++;
                }
                i7 = i;
                while (i7 < 8) {
                    i8 += (int) (((long) this.k) & j);
                    j >>>= this.w;
                    i7++;
                    i3 = i3;
                }
                int i10 = i3;
                i4++;
                i5 = i9;
                i = 0;
            }
            i = this.mdsize % this.w;
            long j2 = 0;
            for (i3 = 0; i3 < i; i3++) {
                j2 ^= (long) ((bArr3[i5] & 255) << (i3 << 3));
                i5++;
            }
            i6 = i << 3;
            for (i = 0; i < i6; i += this.w) {
                i8 += (int) (((long) this.k) & j2);
                j2 >>>= this.w;
            }
            this.checksum = (this.messagesize << this.w) - i8;
            i3 = this.checksum;
            i = i8;
            for (i6 = 0; i6 < log; i6 += this.w) {
                i += this.k & i3;
                i3 >>>= this.w;
            }
        } else if (this.w < 57) {
            i = 0;
            i3 = 0;
            while (i <= (this.mdsize << 3) - this.w) {
                int i11 = i % 8;
                i += this.w;
                long j3 = 0;
                int i12 = 0;
                for (i7 = i >>> 3; i7 < ((i + 7) >>> 3); i7++) {
                    j3 ^= (long) ((bArr3[i7] & 255) << (i12 << 3));
                    i12++;
                }
                i3 = (int) (((long) i3) + ((j3 >>> i11) & ((long) this.k)));
            }
            i7 = i >>> 3;
            if (i7 < this.mdsize) {
                i %= 8;
                i2 = 0;
                long j4 = 0;
                while (i7 < this.mdsize) {
                    j4 ^= (long) ((bArr3[i7] & 255) << (i2 << 3));
                    i2++;
                    i7++;
                }
                i3 = (int) (((long) i3) + ((j4 >>> i) & ((long) this.k)));
            }
            this.checksum = (this.messagesize << this.w) - i3;
            i = i3;
            i3 = this.checksum;
            for (i6 = 0; i6 < log; i6 += this.w) {
                i += this.k & i3;
                i3 >>>= this.w;
            }
        } else {
            i = 0;
        }
        this.keysize = this.messagesize + ((int) Math.ceil(((double) log) / ((double) this.w)));
        this.steps = (int) Math.ceil(((double) (this.keysize + i)) / ((double) (1 << this.height)));
        this.sign = new byte[(this.keysize * this.mdsize)];
        this.counter = 0;
        this.test = 0;
        this.ii = 0;
        this.test8 = 0;
        this.r = 0;
        this.privateKeyOTS = new byte[this.mdsize];
        this.seed = new byte[this.mdsize];
        System.arraycopy(bArr, 0, this.seed, 0, this.mdsize);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.big8);
        stringBuilder.append("  ");
        String stringBuilder2 = stringBuilder.toString();
        int[] iArr = new int[9];
        iArr = getStatInt();
        byte[][] bArr = (byte[][]) Array.newInstance(byte.class, new int[]{5, this.mdsize});
        bArr = getStatByte();
        int i = 0;
        String str = stringBuilder2;
        for (int i2 = 0; i2 < 9; i2++) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append(iArr[i2]);
            stringBuilder3.append(" ");
            str = stringBuilder3.toString();
        }
        while (i < 5) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(new String(Hex.encode(bArr[i])));
            stringBuilder.append(" ");
            str = stringBuilder.toString();
            i++;
        }
        return str;
    }

    public boolean updateSign() {
        for (int i = 0; i < this.steps; i++) {
            if (this.counter < this.keysize) {
                oneStep();
            }
            if (this.counter == this.keysize) {
                return true;
            }
        }
        return false;
    }
}
