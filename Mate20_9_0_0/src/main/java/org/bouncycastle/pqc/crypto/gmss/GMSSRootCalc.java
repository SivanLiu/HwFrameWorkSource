package org.bouncycastle.pqc.crypto.gmss;

import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Vector;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Integers;
import org.bouncycastle.util.encoders.Hex;

public class GMSSRootCalc {
    private byte[][] AuthPath;
    private int K;
    private GMSSDigestProvider digestProvider;
    private int heightOfNextSeed;
    private Vector heightOfNodes;
    private int heightOfTree;
    private int[] index;
    private int indexForNextSeed;
    private boolean isFinished;
    private boolean isInitialized;
    private int mdLength;
    private Digest messDigestTree;
    private Vector[] retain;
    private byte[] root;
    private Vector tailStack;
    private Treehash[] treehash;

    public GMSSRootCalc(int i, int i2, GMSSDigestProvider gMSSDigestProvider) {
        this.heightOfTree = i;
        this.digestProvider = gMSSDigestProvider;
        this.messDigestTree = gMSSDigestProvider.get();
        this.mdLength = this.messDigestTree.getDigestSize();
        this.K = i2;
        this.index = new int[i];
        this.AuthPath = (byte[][]) Array.newInstance(byte.class, new int[]{i, this.mdLength});
        this.root = new byte[this.mdLength];
        this.retain = new Vector[(this.K - 1)];
        for (i = 0; i < i2 - 1; i++) {
            this.retain[i] = new Vector();
        }
    }

    public GMSSRootCalc(Digest digest, byte[][] bArr, int[] iArr, Treehash[] treehashArr, Vector[] vectorArr) {
        int i;
        this.messDigestTree = this.digestProvider.get();
        this.digestProvider = this.digestProvider;
        int i2 = 0;
        this.heightOfTree = iArr[0];
        this.mdLength = iArr[1];
        this.K = iArr[2];
        this.indexForNextSeed = iArr[3];
        this.heightOfNextSeed = iArr[4];
        if (iArr[5] == 1) {
            this.isFinished = true;
        } else {
            this.isFinished = false;
        }
        if (iArr[6] == 1) {
            this.isInitialized = true;
        } else {
            this.isInitialized = false;
        }
        int i3 = iArr[7];
        this.index = new int[this.heightOfTree];
        for (i = 0; i < this.heightOfTree; i++) {
            this.index[i] = iArr[8 + i];
        }
        this.heightOfNodes = new Vector();
        for (i = 0; i < i3; i++) {
            this.heightOfNodes.addElement(Integers.valueOf(iArr[(this.heightOfTree + 8) + i]));
        }
        this.root = bArr[0];
        this.AuthPath = (byte[][]) Array.newInstance(byte.class, new int[]{this.heightOfTree, this.mdLength});
        int i4 = 0;
        while (i4 < this.heightOfTree) {
            int i5 = 1 + i4;
            this.AuthPath[i4] = bArr[i5];
            i4 = i5;
        }
        this.tailStack = new Vector();
        while (i2 < i3) {
            this.tailStack.addElement(bArr[(this.heightOfTree + 1) + i2]);
            i2++;
        }
        this.treehash = GMSSUtils.clone(treehashArr);
        this.retain = GMSSUtils.clone(vectorArr);
    }

    public byte[][] getAuthPath() {
        return GMSSUtils.clone(this.AuthPath);
    }

    public Vector[] getRetain() {
        return GMSSUtils.clone(this.retain);
    }

    public byte[] getRoot() {
        return Arrays.clone(this.root);
    }

    public Vector getStack() {
        Vector vector = new Vector();
        Enumeration elements = this.tailStack.elements();
        while (elements.hasMoreElements()) {
            vector.addElement(elements.nextElement());
        }
        return vector;
    }

    public byte[][] getStatByte() {
        int i = 0;
        int size = this.tailStack == null ? 0 : this.tailStack.size();
        byte[][] bArr = (byte[][]) Array.newInstance(byte.class, new int[]{(this.heightOfTree + 1) + size, 64});
        bArr[0] = this.root;
        int i2 = 0;
        while (i2 < this.heightOfTree) {
            int i3 = 1 + i2;
            bArr[i3] = this.AuthPath[i2];
            i2 = i3;
        }
        while (i < size) {
            bArr[(this.heightOfTree + 1) + i] = (byte[]) this.tailStack.elementAt(i);
            i++;
        }
        return bArr;
    }

    public int[] getStatInt() {
        int i = 0;
        int size = this.tailStack == null ? 0 : this.tailStack.size();
        int[] iArr = new int[((this.heightOfTree + 8) + size)];
        iArr[0] = this.heightOfTree;
        iArr[1] = this.mdLength;
        iArr[2] = this.K;
        iArr[3] = this.indexForNextSeed;
        iArr[4] = this.heightOfNextSeed;
        if (this.isFinished) {
            iArr[5] = 1;
        } else {
            iArr[5] = 0;
        }
        if (this.isInitialized) {
            iArr[6] = 1;
        } else {
            iArr[6] = 0;
        }
        iArr[7] = size;
        for (int i2 = 0; i2 < this.heightOfTree; i2++) {
            iArr[8 + i2] = this.index[i2];
        }
        while (i < size) {
            iArr[(this.heightOfTree + 8) + i] = ((Integer) this.heightOfNodes.elementAt(i)).intValue();
            i++;
        }
        return iArr;
    }

    public Treehash[] getTreehash() {
        return GMSSUtils.clone(this.treehash);
    }

    public void initialize(Vector vector) {
        int i;
        this.treehash = new Treehash[(this.heightOfTree - this.K)];
        for (i = 0; i < this.heightOfTree - this.K; i++) {
            this.treehash[i] = new Treehash(vector, i, this.digestProvider.get());
        }
        this.index = new int[this.heightOfTree];
        this.AuthPath = (byte[][]) Array.newInstance(byte.class, new int[]{this.heightOfTree, this.mdLength});
        this.root = new byte[this.mdLength];
        this.tailStack = new Vector();
        this.heightOfNodes = new Vector();
        this.isInitialized = true;
        this.isFinished = false;
        for (i = 0; i < this.heightOfTree; i++) {
            this.index[i] = -1;
        }
        this.retain = new Vector[(this.K - 1)];
        for (i = 0; i < this.K - 1; i++) {
            this.retain[i] = new Vector();
        }
        this.indexForNextSeed = 3;
        this.heightOfNextSeed = 0;
    }

    public void initializeTreehashSeed(byte[] bArr, int i) {
        this.treehash[i].initializeSeed(bArr);
    }

    public String toString() {
        StringBuilder stringBuilder;
        String str = "";
        int i = 0;
        int size = this.tailStack == null ? 0 : this.tailStack.size();
        String str2 = str;
        for (int i2 = 0; i2 < (8 + this.heightOfTree) + size; i2++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str2);
            stringBuilder2.append(getStatInt()[i2]);
            stringBuilder2.append(" ");
            str2 = stringBuilder2.toString();
        }
        while (i < (1 + this.heightOfTree) + size) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(str2);
            stringBuilder.append(new String(Hex.encode(getStatByte()[i])));
            stringBuilder.append(" ");
            str2 = stringBuilder.toString();
            i++;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(str2);
        stringBuilder.append("  ");
        stringBuilder.append(this.digestProvider.get().getDigestSize());
        return stringBuilder.toString();
    }

    public void update(byte[] bArr) {
        if (this.isFinished) {
            System.out.print("Too much updates for Tree!!");
        } else if (this.isInitialized) {
            int[] iArr = this.index;
            iArr[0] = iArr[0] + 1;
            if (this.index[0] == 1) {
                System.arraycopy(bArr, 0, this.AuthPath[0], 0, this.mdLength);
            } else if (this.index[0] == 3 && this.heightOfTree > this.K) {
                this.treehash[0].setFirstNode(bArr);
            }
            if ((this.index[0] - 3) % 2 == 0 && this.index[0] >= 3 && this.heightOfTree == this.K) {
                this.retain[0].insertElementAt(bArr, 0);
            }
            if (this.index[0] == 0) {
                this.tailStack.addElement(bArr);
                this.heightOfNodes.addElement(Integers.valueOf(0));
                return;
            }
            Object obj = new byte[this.mdLength];
            Object obj2 = new byte[(this.mdLength << 1)];
            System.arraycopy(bArr, 0, obj, 0, this.mdLength);
            int i = 0;
            while (this.tailStack.size() > 0 && i == ((Integer) this.heightOfNodes.lastElement()).intValue()) {
                System.arraycopy(this.tailStack.lastElement(), 0, obj2, 0, this.mdLength);
                this.tailStack.removeElementAt(this.tailStack.size() - 1);
                this.heightOfNodes.removeElementAt(this.heightOfNodes.size() - 1);
                System.arraycopy(obj, 0, obj2, this.mdLength, this.mdLength);
                this.messDigestTree.update(obj2, 0, obj2.length);
                obj = new byte[this.messDigestTree.getDigestSize()];
                this.messDigestTree.doFinal(obj, 0);
                i++;
                if (i < this.heightOfTree) {
                    int[] iArr2 = this.index;
                    iArr2[i] = iArr2[i] + 1;
                    if (this.index[i] == 1) {
                        System.arraycopy(obj, 0, this.AuthPath[i], 0, this.mdLength);
                    }
                    if (i >= this.heightOfTree - this.K) {
                        if (i == 0) {
                            System.out.println("M���P");
                        }
                        if ((this.index[i] - 3) % 2 == 0 && this.index[i] >= 3) {
                            this.retain[i - (this.heightOfTree - this.K)].insertElementAt(obj, 0);
                        }
                    } else if (this.index[i] == 3) {
                        this.treehash[i].setFirstNode(obj);
                    }
                }
            }
            this.tailStack.addElement(obj);
            this.heightOfNodes.addElement(Integers.valueOf(i));
            if (i == this.heightOfTree) {
                this.isFinished = true;
                this.isInitialized = false;
                this.root = (byte[]) this.tailStack.lastElement();
            }
        } else {
            System.err.println("GMSSRootCalc not initialized!");
        }
    }

    public void update(byte[] bArr, byte[] bArr2) {
        if (this.heightOfNextSeed < this.heightOfTree - this.K && this.indexForNextSeed - 2 == this.index[0]) {
            initializeTreehashSeed(bArr, this.heightOfNextSeed);
            this.heightOfNextSeed++;
            this.indexForNextSeed *= 2;
        }
        update(bArr2);
    }

    public boolean wasFinished() {
        return this.isFinished;
    }

    public boolean wasInitialized() {
        return this.isInitialized;
    }
}
