package org.bouncycastle.pqc.crypto.gmss;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Vector;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.pqc.crypto.gmss.util.GMSSRandom;
import org.bouncycastle.pqc.crypto.gmss.util.WinternitzOTSignature;
import org.bouncycastle.util.Arrays;

public class GMSSPrivateKeyParameters extends GMSSKeyParameters {
    private int[] K;
    private byte[][][] currentAuthPaths;
    private Vector[][] currentRetain;
    private byte[][] currentRootSig;
    private byte[][] currentSeeds;
    private Vector[] currentStack;
    private Treehash[][] currentTreehash;
    private GMSSDigestProvider digestProvider;
    private GMSSParameters gmssPS;
    private GMSSRandom gmssRandom;
    private int[] heightOfTrees;
    private int[] index;
    private byte[][][] keep;
    private int mdLength;
    private Digest messDigestTrees;
    private int[] minTreehash;
    private byte[][][] nextAuthPaths;
    private GMSSLeaf[] nextNextLeaf;
    private GMSSRootCalc[] nextNextRoot;
    private byte[][] nextNextSeeds;
    private Vector[][] nextRetain;
    private byte[][] nextRoot;
    private GMSSRootSig[] nextRootSig;
    private Vector[] nextStack;
    private Treehash[][] nextTreehash;
    private int numLayer;
    private int[] numLeafs;
    private int[] otsIndex;
    private GMSSLeaf[] upperLeaf;
    private GMSSLeaf[] upperTreehashLeaf;
    private boolean used;

    private GMSSPrivateKeyParameters(GMSSPrivateKeyParameters gMSSPrivateKeyParameters) {
        super(true, gMSSPrivateKeyParameters.getParameters());
        this.used = false;
        this.index = Arrays.clone(gMSSPrivateKeyParameters.index);
        this.currentSeeds = Arrays.clone(gMSSPrivateKeyParameters.currentSeeds);
        this.nextNextSeeds = Arrays.clone(gMSSPrivateKeyParameters.nextNextSeeds);
        this.currentAuthPaths = Arrays.clone(gMSSPrivateKeyParameters.currentAuthPaths);
        this.nextAuthPaths = Arrays.clone(gMSSPrivateKeyParameters.nextAuthPaths);
        this.currentTreehash = gMSSPrivateKeyParameters.currentTreehash;
        this.nextTreehash = gMSSPrivateKeyParameters.nextTreehash;
        this.currentStack = gMSSPrivateKeyParameters.currentStack;
        this.nextStack = gMSSPrivateKeyParameters.nextStack;
        this.currentRetain = gMSSPrivateKeyParameters.currentRetain;
        this.nextRetain = gMSSPrivateKeyParameters.nextRetain;
        this.keep = Arrays.clone(gMSSPrivateKeyParameters.keep);
        this.nextNextLeaf = gMSSPrivateKeyParameters.nextNextLeaf;
        this.upperLeaf = gMSSPrivateKeyParameters.upperLeaf;
        this.upperTreehashLeaf = gMSSPrivateKeyParameters.upperTreehashLeaf;
        this.minTreehash = gMSSPrivateKeyParameters.minTreehash;
        this.gmssPS = gMSSPrivateKeyParameters.gmssPS;
        this.nextRoot = Arrays.clone(gMSSPrivateKeyParameters.nextRoot);
        this.nextNextRoot = gMSSPrivateKeyParameters.nextNextRoot;
        this.currentRootSig = gMSSPrivateKeyParameters.currentRootSig;
        this.nextRootSig = gMSSPrivateKeyParameters.nextRootSig;
        this.digestProvider = gMSSPrivateKeyParameters.digestProvider;
        this.heightOfTrees = gMSSPrivateKeyParameters.heightOfTrees;
        this.otsIndex = gMSSPrivateKeyParameters.otsIndex;
        this.K = gMSSPrivateKeyParameters.K;
        this.numLayer = gMSSPrivateKeyParameters.numLayer;
        this.messDigestTrees = gMSSPrivateKeyParameters.messDigestTrees;
        this.mdLength = gMSSPrivateKeyParameters.mdLength;
        this.gmssRandom = gMSSPrivateKeyParameters.gmssRandom;
        this.numLeafs = gMSSPrivateKeyParameters.numLeafs;
    }

    public GMSSPrivateKeyParameters(int[] iArr, byte[][] bArr, byte[][] bArr2, byte[][][] bArr3, byte[][][] bArr4, byte[][][] bArr5, Treehash[][] treehashArr, Treehash[][] treehashArr2, Vector[] vectorArr, Vector[] vectorArr2, Vector[][] vectorArr3, Vector[][] vectorArr4, GMSSLeaf[] gMSSLeafArr, GMSSLeaf[] gMSSLeafArr2, GMSSLeaf[] gMSSLeafArr3, int[] iArr2, byte[][] bArr6, GMSSRootCalc[] gMSSRootCalcArr, byte[][] bArr7, GMSSRootSig[] gMSSRootSigArr, GMSSParameters gMSSParameters, GMSSDigestProvider gMSSDigestProvider) {
        int i;
        int i2;
        int i3;
        int i4;
        int[] iArr3 = iArr;
        byte[][] bArr8 = bArr;
        byte[][][] bArr9 = bArr5;
        Vector[] vectorArr5 = vectorArr;
        Vector[] vectorArr6 = vectorArr2;
        GMSSLeaf[] gMSSLeafArr4 = gMSSLeafArr;
        GMSSLeaf[] gMSSLeafArr5 = gMSSLeafArr2;
        GMSSLeaf[] gMSSLeafArr6 = gMSSLeafArr3;
        int[] iArr4 = iArr2;
        byte[][] bArr10 = bArr6;
        GMSSRootCalc[] gMSSRootCalcArr2 = gMSSRootCalcArr;
        GMSSRootSig[] gMSSRootSigArr2 = gMSSRootSigArr;
        GMSSParameters gMSSParameters2 = gMSSParameters;
        super(true, gMSSParameters2);
        this.used = false;
        this.messDigestTrees = gMSSDigestProvider.get();
        this.mdLength = this.messDigestTrees.getDigestSize();
        this.gmssPS = gMSSParameters2;
        this.otsIndex = gMSSParameters.getWinternitzParameter();
        this.K = gMSSParameters.getK();
        this.heightOfTrees = gMSSParameters.getHeightOfTrees();
        this.numLayer = this.gmssPS.getNumOfLayers();
        if (iArr3 == null) {
            this.index = new int[this.numLayer];
            for (i = 0; i < this.numLayer; i++) {
                this.index[i] = 0;
            }
        } else {
            this.index = iArr3;
        }
        this.currentSeeds = bArr8;
        this.nextNextSeeds = bArr2;
        this.currentAuthPaths = bArr3;
        this.nextAuthPaths = bArr4;
        if (bArr9 == null) {
            this.keep = new byte[this.numLayer][][];
            for (i = 0; i < this.numLayer; i++) {
                this.keep[i] = (byte[][]) Array.newInstance(byte.class, new int[]{(int) Math.floor((double) (this.heightOfTrees[i] / 2)), this.mdLength});
            }
        } else {
            this.keep = bArr9;
        }
        if (vectorArr5 == null) {
            this.currentStack = new Vector[this.numLayer];
            for (i = 0; i < this.numLayer; i++) {
                this.currentStack[i] = new Vector();
            }
        } else {
            this.currentStack = vectorArr5;
        }
        if (vectorArr6 == null) {
            i2 = 1;
            this.nextStack = new Vector[(this.numLayer - 1)];
            i = 0;
            while (i < this.numLayer - i2) {
                this.nextStack[i] = new Vector();
                i++;
                i2 = 1;
            }
        } else {
            this.nextStack = vectorArr6;
        }
        this.currentTreehash = treehashArr;
        this.nextTreehash = treehashArr2;
        this.currentRetain = vectorArr3;
        this.nextRetain = vectorArr4;
        this.nextRoot = bArr10;
        this.digestProvider = gMSSDigestProvider;
        if (gMSSRootCalcArr2 == null) {
            i3 = 1;
            this.nextNextRoot = new GMSSRootCalc[(this.numLayer - 1)];
            i2 = 0;
            while (i2 < this.numLayer - i3) {
                i4 = i2 + 1;
                this.nextNextRoot[i2] = new GMSSRootCalc(this.heightOfTrees[i4], this.K[i4], this.digestProvider);
                i2 = i4;
                i3 = 1;
            }
        } else {
            this.nextNextRoot = gMSSRootCalcArr2;
        }
        this.currentRootSig = bArr7;
        this.numLeafs = new int[this.numLayer];
        for (i2 = 0; i2 < this.numLayer; i2++) {
            this.numLeafs[i2] = 1 << this.heightOfTrees[i2];
        }
        this.gmssRandom = new GMSSRandom(this.messDigestTrees);
        if (this.numLayer <= 1) {
            this.nextNextLeaf = new GMSSLeaf[0];
        } else if (gMSSLeafArr4 == null) {
            this.nextNextLeaf = new GMSSLeaf[(this.numLayer - 2)];
            i2 = 0;
            while (i2 < this.numLayer - 2) {
                i4 = i2 + 1;
                this.nextNextLeaf[i2] = new GMSSLeaf(gMSSDigestProvider.get(), this.otsIndex[i4], this.numLeafs[i2 + 2], this.nextNextSeeds[i2]);
                i2 = i4;
            }
        } else {
            this.nextNextLeaf = gMSSLeafArr4;
        }
        if (gMSSLeafArr5 == null) {
            i3 = 1;
            this.upperLeaf = new GMSSLeaf[(this.numLayer - 1)];
            i2 = 0;
            while (i2 < this.numLayer - i3) {
                i4 = i2 + 1;
                this.upperLeaf[i2] = new GMSSLeaf(gMSSDigestProvider.get(), this.otsIndex[i2], this.numLeafs[i4], this.currentSeeds[i2]);
                i2 = i4;
                i3 = 1;
            }
        } else {
            this.upperLeaf = gMSSLeafArr5;
        }
        if (gMSSLeafArr6 == null) {
            i3 = 1;
            this.upperTreehashLeaf = new GMSSLeaf[(this.numLayer - 1)];
            i2 = 0;
            while (i2 < this.numLayer - i3) {
                int i5 = i2 + 1;
                this.upperTreehashLeaf[i2] = new GMSSLeaf(gMSSDigestProvider.get(), this.otsIndex[i2], this.numLeafs[i5]);
                i2 = i5;
                i3 = 1;
            }
        } else {
            this.upperTreehashLeaf = gMSSLeafArr6;
        }
        if (iArr4 == null) {
            i3 = 1;
            this.minTreehash = new int[(this.numLayer - 1)];
            i2 = 0;
            while (i2 < this.numLayer - i3) {
                this.minTreehash[i2] = -1;
                i2++;
                i3 = 1;
            }
        } else {
            this.minTreehash = iArr4;
        }
        byte[] bArr11 = new byte[this.mdLength];
        byte[] bArr12 = new byte[this.mdLength];
        if (gMSSRootSigArr2 == null) {
            this.nextRootSig = new GMSSRootSig[(this.numLayer - 1)];
            i3 = 0;
            while (i3 < this.numLayer - 1) {
                System.arraycopy(bArr8[i3], 0, bArr11, 0, this.mdLength);
                this.gmssRandom.nextSeed(bArr11);
                byte[] nextSeed = this.gmssRandom.nextSeed(bArr11);
                int i6 = i3 + 1;
                this.nextRootSig[i3] = new GMSSRootSig(gMSSDigestProvider.get(), this.otsIndex[i3], this.heightOfTrees[i6]);
                this.nextRootSig[i3].initSign(nextSeed, bArr10[i3]);
                i3 = i6;
            }
            return;
        }
        this.nextRootSig = gMSSRootSigArr2;
    }

    public GMSSPrivateKeyParameters(byte[][] bArr, byte[][] bArr2, byte[][][] bArr3, byte[][][] bArr4, Treehash[][] treehashArr, Treehash[][] treehashArr2, Vector[] vectorArr, Vector[] vectorArr2, Vector[][] vectorArr3, Vector[][] vectorArr4, byte[][] bArr5, byte[][] bArr6, GMSSParameters gMSSParameters, GMSSDigestProvider gMSSDigestProvider) {
        this(null, bArr, bArr2, bArr3, bArr4, (byte[][][]) null, treehashArr, treehashArr2, vectorArr, vectorArr2, vectorArr3, vectorArr4, null, null, null, null, bArr5, null, bArr6, null, gMSSParameters, gMSSDigestProvider);
    }

    private void computeAuthPaths(int i) {
        int i2;
        int i3 = this.index[i];
        int i4 = this.heightOfTrees[i];
        int i5 = this.K[i];
        int i6 = 0;
        while (true) {
            i2 = i4 - i5;
            if (i6 >= i2) {
                break;
            }
            this.currentTreehash[i][i6].updateNextSeed(this.gmssRandom);
            i6++;
        }
        i5 = heightOfPhi(i3);
        byte[] bArr = new byte[this.mdLength];
        bArr = this.gmssRandom.nextSeed(this.currentSeeds[i]);
        int i7 = 1;
        int i8 = (i3 >>> (i5 + 1)) & 1;
        byte[] bArr2 = new byte[this.mdLength];
        i4--;
        if (i5 < i4 && i8 == 0) {
            System.arraycopy(this.currentAuthPaths[i][i5], 0, bArr2, 0, this.mdLength);
        }
        byte[] bArr3 = new byte[this.mdLength];
        if (i5 == 0) {
            Object publicKey;
            if (i == this.numLayer - 1) {
                publicKey = new WinternitzOTSignature(bArr, this.digestProvider.get(), this.otsIndex[i]).getPublicKey();
            } else {
                byte[] bArr4 = new byte[this.mdLength];
                System.arraycopy(this.currentSeeds[i], 0, bArr4, 0, this.mdLength);
                this.gmssRandom.nextSeed(bArr4);
                Object leaf = this.upperLeaf[i].getLeaf();
                this.upperLeaf[i].initLeafCalc(bArr4);
                publicKey = leaf;
            }
            System.arraycopy(publicKey, 0, this.currentAuthPaths[i][0], 0, this.mdLength);
        } else {
            bArr = new byte[(this.mdLength << 1)];
            int i9 = i5 - 1;
            System.arraycopy(this.currentAuthPaths[i][i9], 0, bArr, 0, this.mdLength);
            System.arraycopy(this.keep[i][(int) Math.floor((double) (i9 / 2))], 0, bArr, this.mdLength, this.mdLength);
            this.messDigestTrees.update(bArr, 0, bArr.length);
            this.currentAuthPaths[i][i5] = new byte[this.messDigestTrees.getDigestSize()];
            this.messDigestTrees.doFinal(this.currentAuthPaths[i][i5], 0);
            i6 = 0;
            while (i6 < i5) {
                if (i6 < i2) {
                    if (this.currentTreehash[i][i6].wasFinished()) {
                        System.arraycopy(this.currentTreehash[i][i6].getFirstNode(), 0, this.currentAuthPaths[i][i6], 0, this.mdLength);
                        this.currentTreehash[i][i6].destroy();
                    } else {
                        PrintStream printStream = System.err;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Treehash (");
                        stringBuilder.append(i);
                        stringBuilder.append(",");
                        stringBuilder.append(i6);
                        stringBuilder.append(") not finished when needed in AuthPathComputation");
                        printStream.println(stringBuilder.toString());
                    }
                }
                if (i6 < i4 && i6 >= i2) {
                    i9 = i6 - i2;
                    if (this.currentRetain[i][i9].size() > 0) {
                        System.arraycopy(this.currentRetain[i][i9].lastElement(), 0, this.currentAuthPaths[i][i6], 0, this.mdLength);
                        this.currentRetain[i][i9].removeElementAt(this.currentRetain[i][i9].size() - 1);
                    }
                }
                if (i6 < i2 && (3 * (1 << i6)) + i3 < this.numLeafs[i]) {
                    this.currentTreehash[i][i6].initialize();
                }
                i6++;
            }
        }
        if (i5 < i4 && i8 == 0) {
            System.arraycopy(bArr2, 0, this.keep[i][(int) Math.floor((double) (i5 / 2))], 0, this.mdLength);
        }
        if (i == this.numLayer - 1) {
            while (i7 <= i2 / 2) {
                i3 = getMinTreehashIndex(i);
                if (i3 >= 0) {
                    try {
                        byte[] bArr5 = new byte[this.mdLength];
                        System.arraycopy(this.currentTreehash[i][i3].getSeedActive(), 0, bArr5, 0, this.mdLength);
                        this.currentTreehash[i][i3].update(this.gmssRandom, new WinternitzOTSignature(this.gmssRandom.nextSeed(bArr5), this.digestProvider.get(), this.otsIndex[i]).getPublicKey());
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                i7++;
            }
            return;
        }
        this.minTreehash[i] = getMinTreehashIndex(i);
    }

    private int getMinTreehashIndex(int i) {
        int i2 = 0;
        int i3 = -1;
        while (i2 < this.heightOfTrees[i] - this.K[i]) {
            if (this.currentTreehash[i][i2].wasInitialized() && !this.currentTreehash[i][i2].wasFinished() && (i3 == -1 || this.currentTreehash[i][i2].getLowestNodeHeight() < this.currentTreehash[i][i3].getLowestNodeHeight())) {
                i3 = i2;
            }
            i2++;
        }
        return i3;
    }

    private int heightOfPhi(int i) {
        if (i == 0) {
            return -1;
        }
        int i2 = 0;
        int i3 = 1;
        while (i % i3 == 0) {
            i3 *= 2;
            i2++;
        }
        return i2 - 1;
    }

    private void nextKey(int i) {
        if (i == this.numLayer - 1) {
            int[] iArr = this.index;
            iArr[i] = iArr[i] + 1;
        }
        if (this.index[i] != this.numLeafs[i]) {
            updateKey(i);
        } else if (this.numLayer != 1) {
            nextTree(i);
            this.index[i] = 0;
        }
    }

    private void nextTree(int i) {
        if (i > 0) {
            int[] iArr = this.index;
            int i2 = i - 1;
            iArr[i2] = iArr[i2] + 1;
            int i3 = i;
            int i4 = 1;
            do {
                i3--;
                if (this.index[i3] < this.numLeafs[i3]) {
                    i4 = 0;
                }
                if (i4 == 0) {
                    break;
                }
            } while (i3 > 0);
            if (i4 == 0) {
                this.gmssRandom.nextSeed(this.currentSeeds[i]);
                this.nextRootSig[i2].updateSign();
                if (i > 1) {
                    i4 = i2 - 1;
                    this.nextNextLeaf[i4] = this.nextNextLeaf[i4].nextLeaf();
                }
                this.upperLeaf[i2] = this.upperLeaf[i2].nextLeaf();
                if (this.minTreehash[i2] >= 0) {
                    this.upperTreehashLeaf[i2] = this.upperTreehashLeaf[i2].nextLeaf();
                    try {
                        this.currentTreehash[i2][this.minTreehash[i2]].update(this.gmssRandom, this.upperTreehashLeaf[i2].getLeaf());
                        this.currentTreehash[i2][this.minTreehash[i2]].wasFinished();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                updateNextNextAuthRoot(i);
                this.currentRootSig[i2] = this.nextRootSig[i2].getSig();
                for (i3 = 0; i3 < this.heightOfTrees[i] - this.K[i]; i3++) {
                    this.currentTreehash[i][i3] = this.nextTreehash[i2][i3];
                    this.nextTreehash[i2][i3] = this.nextNextRoot[i2].getTreehash()[i3];
                }
                for (i3 = 0; i3 < this.heightOfTrees[i]; i3++) {
                    System.arraycopy(this.nextAuthPaths[i2][i3], 0, this.currentAuthPaths[i][i3], 0, this.mdLength);
                    System.arraycopy(this.nextNextRoot[i2].getAuthPath()[i3], 0, this.nextAuthPaths[i2][i3], 0, this.mdLength);
                }
                for (i3 = 0; i3 < this.K[i] - 1; i3++) {
                    this.currentRetain[i][i3] = this.nextRetain[i2][i3];
                    this.nextRetain[i2][i3] = this.nextNextRoot[i2].getRetain()[i3];
                }
                this.currentStack[i] = this.nextStack[i2];
                this.nextStack[i2] = this.nextNextRoot[i2].getStack();
                this.nextRoot[i2] = this.nextNextRoot[i2].getRoot();
                byte[] bArr = new byte[this.mdLength];
                bArr = new byte[this.mdLength];
                System.arraycopy(this.currentSeeds[i2], 0, bArr, 0, this.mdLength);
                this.gmssRandom.nextSeed(bArr);
                this.gmssRandom.nextSeed(bArr);
                this.nextRootSig[i2].initSign(this.gmssRandom.nextSeed(bArr), this.nextRoot[i2]);
                nextKey(i2);
            }
        }
    }

    private void updateKey(int i) {
        computeAuthPaths(i);
        if (i > 0) {
            int i2;
            if (i > 1) {
                i2 = (i - 1) - 1;
                this.nextNextLeaf[i2] = this.nextNextLeaf[i2].nextLeaf();
            }
            i2 = i - 1;
            this.upperLeaf[i2] = this.upperLeaf[i2].nextLeaf();
            int floor = (int) Math.floor(((double) (getNumLeafs(i) * 2)) / ((double) (this.heightOfTrees[i2] - this.K[i2])));
            if (this.index[i] % floor == 1) {
                if (this.index[i] > 1 && this.minTreehash[i2] >= 0) {
                    try {
                        this.currentTreehash[i2][this.minTreehash[i2]].update(this.gmssRandom, this.upperTreehashLeaf[i2].getLeaf());
                        this.currentTreehash[i2][this.minTreehash[i2]].wasFinished();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                this.minTreehash[i2] = getMinTreehashIndex(i2);
                if (this.minTreehash[i2] >= 0) {
                    this.upperTreehashLeaf[i2] = new GMSSLeaf(this.digestProvider.get(), this.otsIndex[i2], floor, this.currentTreehash[i2][this.minTreehash[i2]].getSeedActive());
                    this.upperTreehashLeaf[i2] = this.upperTreehashLeaf[i2].nextLeaf();
                }
            } else if (this.minTreehash[i2] >= 0) {
                this.upperTreehashLeaf[i2] = this.upperTreehashLeaf[i2].nextLeaf();
            }
            this.nextRootSig[i2].updateSign();
            if (this.index[i] == 1) {
                this.nextNextRoot[i2].initialize(new Vector());
            }
            updateNextNextAuthRoot(i);
        }
    }

    private void updateNextNextAuthRoot(int i) {
        byte[] bArr = new byte[this.mdLength];
        int i2 = i - 1;
        bArr = this.gmssRandom.nextSeed(this.nextNextSeeds[i2]);
        if (i == this.numLayer - 1) {
            this.nextNextRoot[i2].update(this.nextNextSeeds[i2], new WinternitzOTSignature(bArr, this.digestProvider.get(), this.otsIndex[i]).getPublicKey());
            return;
        }
        this.nextNextRoot[i2].update(this.nextNextSeeds[i2], this.nextNextLeaf[i2].getLeaf());
        this.nextNextLeaf[i2].initLeafCalc(this.nextNextSeeds[i2]);
    }

    public byte[][][] getCurrentAuthPaths() {
        return Arrays.clone(this.currentAuthPaths);
    }

    public byte[][] getCurrentSeeds() {
        return Arrays.clone(this.currentSeeds);
    }

    public int getIndex(int i) {
        return this.index[i];
    }

    public int[] getIndex() {
        return this.index;
    }

    public GMSSDigestProvider getName() {
        return this.digestProvider;
    }

    public int getNumLeafs(int i) {
        return this.numLeafs[i];
    }

    public byte[] getSubtreeRootSig(int i) {
        return this.currentRootSig[i];
    }

    public boolean isUsed() {
        return this.used;
    }

    public void markUsed() {
        this.used = true;
    }

    public GMSSPrivateKeyParameters nextKey() {
        GMSSPrivateKeyParameters gMSSPrivateKeyParameters = new GMSSPrivateKeyParameters(this);
        gMSSPrivateKeyParameters.nextKey(this.gmssPS.getNumOfLayers() - 1);
        return gMSSPrivateKeyParameters;
    }
}
