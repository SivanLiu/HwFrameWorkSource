package org.bouncycastle.pqc.crypto.gmss;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.pqc.crypto.gmss.util.GMSSRandom;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

public class GMSSLeaf {
    private byte[] concHashs;
    private GMSSRandom gmssRandom;
    private int i;
    private int j;
    private int keysize;
    private byte[] leaf;
    private int mdsize;
    private Digest messDigestOTS;
    byte[] privateKeyOTS;
    private byte[] seed;
    private int steps;
    private int two_power_w;
    private int w;

    GMSSLeaf(Digest digest, int i, int i2) {
        this.w = i;
        this.messDigestOTS = digest;
        this.gmssRandom = new GMSSRandom(this.messDigestOTS);
        this.mdsize = this.messDigestOTS.getDigestSize();
        double d = (double) i;
        int ceil = (int) Math.ceil(((double) (this.mdsize << 3)) / d);
        this.keysize = ceil + ((int) Math.ceil(((double) getLog((ceil << i) + 1)) / d));
        ceil = 1 << i;
        this.two_power_w = ceil;
        this.steps = (int) Math.ceil(((double) ((((ceil - 1) * this.keysize) + 1) + this.keysize)) / ((double) i2));
        this.seed = new byte[this.mdsize];
        this.leaf = new byte[this.mdsize];
        this.privateKeyOTS = new byte[this.mdsize];
        this.concHashs = new byte[(this.mdsize * this.keysize)];
    }

    public GMSSLeaf(Digest digest, int i, int i2, byte[] bArr) {
        this.w = i;
        this.messDigestOTS = digest;
        this.gmssRandom = new GMSSRandom(this.messDigestOTS);
        this.mdsize = this.messDigestOTS.getDigestSize();
        double d = (double) i;
        int ceil = (int) Math.ceil(((double) (this.mdsize << 3)) / d);
        this.keysize = ceil + ((int) Math.ceil(((double) getLog((ceil << i) + 1)) / d));
        ceil = 1 << i;
        this.two_power_w = ceil;
        this.steps = (int) Math.ceil(((double) ((((ceil - 1) * this.keysize) + 1) + this.keysize)) / ((double) i2));
        this.seed = new byte[this.mdsize];
        this.leaf = new byte[this.mdsize];
        this.privateKeyOTS = new byte[this.mdsize];
        this.concHashs = new byte[(this.mdsize * this.keysize)];
        initLeafCalc(bArr);
    }

    public GMSSLeaf(Digest digest, byte[][] bArr, int[] iArr) {
        this.i = iArr[0];
        this.j = iArr[1];
        this.steps = iArr[2];
        this.w = iArr[3];
        this.messDigestOTS = digest;
        this.gmssRandom = new GMSSRandom(this.messDigestOTS);
        this.mdsize = this.messDigestOTS.getDigestSize();
        int ceil = (int) Math.ceil(((double) (this.mdsize << 3)) / ((double) this.w));
        this.keysize = ceil + ((int) Math.ceil(((double) getLog((ceil << this.w) + 1)) / ((double) this.w)));
        this.two_power_w = 1 << this.w;
        this.privateKeyOTS = bArr[0];
        this.seed = bArr[1];
        this.concHashs = bArr[2];
        this.leaf = bArr[3];
    }

    private GMSSLeaf(GMSSLeaf gMSSLeaf) {
        this.messDigestOTS = gMSSLeaf.messDigestOTS;
        this.mdsize = gMSSLeaf.mdsize;
        this.keysize = gMSSLeaf.keysize;
        this.gmssRandom = gMSSLeaf.gmssRandom;
        this.leaf = Arrays.clone(gMSSLeaf.leaf);
        this.concHashs = Arrays.clone(gMSSLeaf.concHashs);
        this.i = gMSSLeaf.i;
        this.j = gMSSLeaf.j;
        this.two_power_w = gMSSLeaf.two_power_w;
        this.w = gMSSLeaf.w;
        this.steps = gMSSLeaf.steps;
        this.seed = Arrays.clone(gMSSLeaf.seed);
        this.privateKeyOTS = Arrays.clone(gMSSLeaf.privateKeyOTS);
    }

    private int getLog(int i) {
        int i2 = 1;
        int i3 = 2;
        while (i3 < i) {
            i3 <<= 1;
            i2++;
        }
        return i2;
    }

    private void updateLeafCalc() {
        byte[] bArr = new byte[this.messDigestOTS.getDigestSize()];
        for (int i = 0; i < this.steps + 10000; i++) {
            if (this.i == this.keysize && this.j == this.two_power_w - 1) {
                this.messDigestOTS.update(this.concHashs, 0, this.concHashs.length);
                this.leaf = new byte[this.messDigestOTS.getDigestSize()];
                this.messDigestOTS.doFinal(this.leaf, 0);
                return;
            }
            if (this.i == 0 || this.j == this.two_power_w - 1) {
                this.i++;
                this.j = 0;
                this.privateKeyOTS = this.gmssRandom.nextSeed(this.seed);
            } else {
                this.messDigestOTS.update(this.privateKeyOTS, 0, this.privateKeyOTS.length);
                this.privateKeyOTS = bArr;
                this.messDigestOTS.doFinal(this.privateKeyOTS, 0);
                this.j++;
                if (this.j == this.two_power_w - 1) {
                    System.arraycopy(this.privateKeyOTS, 0, this.concHashs, this.mdsize * (this.i - 1), this.mdsize);
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unable to updateLeaf in steps: ");
        stringBuilder.append(this.steps);
        stringBuilder.append(" ");
        stringBuilder.append(this.i);
        stringBuilder.append(" ");
        stringBuilder.append(this.j);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public byte[] getLeaf() {
        return Arrays.clone(this.leaf);
    }

    public byte[][] getStatByte() {
        byte[][] bArr = new byte[][]{new byte[this.mdsize], new byte[this.mdsize], new byte[(this.mdsize * this.keysize)], new byte[this.mdsize]};
        bArr[0] = this.privateKeyOTS;
        bArr[1] = this.seed;
        bArr[2] = this.concHashs;
        bArr[3] = this.leaf;
        return bArr;
    }

    public int[] getStatInt() {
        return new int[]{this.i, this.j, this.steps, this.w};
    }

    void initLeafCalc(byte[] bArr) {
        this.i = 0;
        this.j = 0;
        byte[] bArr2 = new byte[this.mdsize];
        System.arraycopy(bArr, 0, bArr2, 0, this.seed.length);
        this.seed = this.gmssRandom.nextSeed(bArr2);
    }

    GMSSLeaf nextLeaf() {
        GMSSLeaf gMSSLeaf = new GMSSLeaf(this);
        gMSSLeaf.updateLeafCalc();
        return gMSSLeaf;
    }

    public String toString() {
        int i = 0;
        String str = "";
        for (int i2 = 0; i2 < 4; i2++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(getStatInt()[i2]);
            stringBuilder.append(" ");
            str = stringBuilder.toString();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append(" ");
        stringBuilder2.append(this.mdsize);
        stringBuilder2.append(" ");
        stringBuilder2.append(this.keysize);
        stringBuilder2.append(" ");
        stringBuilder2.append(this.two_power_w);
        stringBuilder2.append(" ");
        String stringBuilder3 = stringBuilder2.toString();
        byte[][] statByte = getStatByte();
        while (i < 4) {
            StringBuilder stringBuilder4;
            if (statByte[i] != null) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(stringBuilder3);
                stringBuilder4.append(new String(Hex.encode(statByte[i])));
                stringBuilder3 = " ";
            } else {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(stringBuilder3);
                stringBuilder3 = "null ";
            }
            stringBuilder4.append(stringBuilder3);
            stringBuilder3 = stringBuilder4.toString();
            i++;
        }
        return stringBuilder3;
    }
}
