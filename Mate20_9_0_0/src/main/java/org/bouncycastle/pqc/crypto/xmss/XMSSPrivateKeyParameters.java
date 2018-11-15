package org.bouncycastle.pqc.crypto.xmss;

import java.io.IOException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public final class XMSSPrivateKeyParameters extends AsymmetricKeyParameter implements XMSSStoreableObjectInterface {
    private final BDS bdsState;
    private final XMSSParameters params;
    private final byte[] publicSeed;
    private final byte[] root;
    private final byte[] secretKeyPRF;
    private final byte[] secretKeySeed;

    public static class Builder {
        private BDS bdsState = null;
        private int index = 0;
        private final XMSSParameters params;
        private byte[] privateKey = null;
        private byte[] publicSeed = null;
        private byte[] root = null;
        private byte[] secretKeyPRF = null;
        private byte[] secretKeySeed = null;
        private XMSSParameters xmss = null;

        public Builder(XMSSParameters xMSSParameters) {
            this.params = xMSSParameters;
        }

        public XMSSPrivateKeyParameters build() {
            return new XMSSPrivateKeyParameters(this);
        }

        public Builder withBDSState(BDS bds) {
            this.bdsState = bds;
            return this;
        }

        public Builder withIndex(int i) {
            this.index = i;
            return this;
        }

        public Builder withPrivateKey(byte[] bArr, XMSSParameters xMSSParameters) {
            this.privateKey = XMSSUtil.cloneArray(bArr);
            this.xmss = xMSSParameters;
            return this;
        }

        public Builder withPublicSeed(byte[] bArr) {
            this.publicSeed = XMSSUtil.cloneArray(bArr);
            return this;
        }

        public Builder withRoot(byte[] bArr) {
            this.root = XMSSUtil.cloneArray(bArr);
            return this;
        }

        public Builder withSecretKeyPRF(byte[] bArr) {
            this.secretKeyPRF = XMSSUtil.cloneArray(bArr);
            return this;
        }

        public Builder withSecretKeySeed(byte[] bArr) {
            this.secretKeySeed = XMSSUtil.cloneArray(bArr);
            return this;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:69:0x0133 in {11, 13, 15, 16, 20, 22, 24, 26, 31, 33, 34, 39, 41, 42, 47, 49, 50, 55, 57, 58, 62, 67, 68, 71} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private XMSSPrivateKeyParameters(org.bouncycastle.pqc.crypto.xmss.XMSSPrivateKeyParameters.Builder r8) {
        /*
        r7 = this;
        r0 = 1;
        r7.<init>(r0);
        r1 = r8.params;
        r7.params = r1;
        r1 = r7.params;
        if (r1 == 0) goto L_0x0134;
    L_0x000e:
        r1 = r7.params;
        r1 = r1.getDigestSize();
        r2 = r8.privateKey;
        if (r2 == 0) goto L_0x0092;
    L_0x001a:
        r0 = r8.xmss;
        if (r0 == 0) goto L_0x008a;
    L_0x0020:
        r0 = r7.params;
        r0 = r0.getHeight();
        r3 = 0;
        r3 = org.bouncycastle.util.Pack.bigEndianToInt(r2, r3);
        r4 = (long) r3;
        r0 = org.bouncycastle.pqc.crypto.xmss.XMSSUtil.isIndexValid(r0, r4);
        if (r0 == 0) goto L_0x0082;
    L_0x0032:
        r0 = 4;
        r4 = org.bouncycastle.pqc.crypto.xmss.XMSSUtil.extractBytesAtOffset(r2, r0, r1);
        r7.secretKeySeed = r4;
        r0 = r0 + r1;
        r4 = org.bouncycastle.pqc.crypto.xmss.XMSSUtil.extractBytesAtOffset(r2, r0, r1);
        r7.secretKeyPRF = r4;
        r0 = r0 + r1;
        r4 = org.bouncycastle.pqc.crypto.xmss.XMSSUtil.extractBytesAtOffset(r2, r0, r1);
        r7.publicSeed = r4;
        r0 = r0 + r1;
        r4 = org.bouncycastle.pqc.crypto.xmss.XMSSUtil.extractBytesAtOffset(r2, r0, r1);
        r7.root = r4;
        r0 = r0 + r1;
        r1 = r2.length;
        r1 = r1 - r0;
        r0 = org.bouncycastle.pqc.crypto.xmss.XMSSUtil.extractBytesAtOffset(r2, r0, r1);
        r1 = 0;
        r0 = org.bouncycastle.pqc.crypto.xmss.XMSSUtil.deserialize(r0);	 Catch:{ IOException -> 0x0062, ClassNotFoundException -> 0x005d }
        r0 = (org.bouncycastle.pqc.crypto.xmss.BDS) r0;	 Catch:{ IOException -> 0x0062, ClassNotFoundException -> 0x005d }
        goto L_0x0067;
    L_0x005d:
        r0 = move-exception;
        r0.printStackTrace();
        goto L_0x0066;
    L_0x0062:
        r0 = move-exception;
        r0.printStackTrace();
    L_0x0066:
        r0 = r1;
    L_0x0067:
        r8 = r8.xmss;
        r0.setXMSS(r8);
        r0.validate();
        r8 = r0.getIndex();
        if (r8 != r3) goto L_0x007a;
    L_0x0077:
        r7.bdsState = r0;
        return;
    L_0x007a:
        r8 = new java.lang.IllegalStateException;
        r0 = "serialized BDS has wrong index";
        r8.<init>(r0);
        throw r8;
    L_0x0082:
        r8 = new java.lang.IllegalArgumentException;
        r0 = "index out of bounds";
        r8.<init>(r0);
        throw r8;
    L_0x008a:
        r8 = new java.lang.NullPointerException;
        r0 = "xmss == null";
        r8.<init>(r0);
        throw r8;
    L_0x0092:
        r4 = r8.secretKeySeed;
        if (r4 == 0) goto L_0x00a6;
    L_0x0098:
        r2 = r4.length;
        if (r2 != r1) goto L_0x009e;
    L_0x009b:
        r7.secretKeySeed = r4;
        goto L_0x00aa;
    L_0x009e:
        r8 = new java.lang.IllegalArgumentException;
        r0 = "size of secretKeySeed needs to be equal size of digest";
        r8.<init>(r0);
        throw r8;
    L_0x00a6:
        r2 = new byte[r1];
        r7.secretKeySeed = r2;
    L_0x00aa:
        r2 = r8.secretKeyPRF;
        if (r2 == 0) goto L_0x00bc;
    L_0x00b0:
        r3 = r2.length;
        if (r3 != r1) goto L_0x00b4;
    L_0x00b3:
        goto L_0x00be;
    L_0x00b4:
        r8 = new java.lang.IllegalArgumentException;
        r0 = "size of secretKeyPRF needs to be equal size of digest";
        r8.<init>(r0);
        throw r8;
    L_0x00bc:
        r2 = new byte[r1];
    L_0x00be:
        r7.secretKeyPRF = r2;
        r3 = r8.publicSeed;
        if (r3 == 0) goto L_0x00d4;
    L_0x00c6:
        r2 = r3.length;
        if (r2 != r1) goto L_0x00cc;
    L_0x00c9:
        r7.publicSeed = r3;
        goto L_0x00d8;
    L_0x00cc:
        r8 = new java.lang.IllegalArgumentException;
        r0 = "size of publicSeed needs to be equal size of digest";
        r8.<init>(r0);
        throw r8;
    L_0x00d4:
        r2 = new byte[r1];
        r7.publicSeed = r2;
    L_0x00d8:
        r2 = r8.root;
        if (r2 == 0) goto L_0x00ec;
    L_0x00de:
        r5 = r2.length;
        if (r5 != r1) goto L_0x00e4;
    L_0x00e1:
        r7.root = r2;
        goto L_0x00f0;
    L_0x00e4:
        r8 = new java.lang.IllegalArgumentException;
        r0 = "size of root needs to be equal size of digest";
        r8.<init>(r0);
        throw r8;
    L_0x00ec:
        r1 = new byte[r1];
        r7.root = r1;
    L_0x00f0:
        r1 = r8.bdsState;
        if (r1 == 0) goto L_0x00f9;
    L_0x00f6:
        r7.bdsState = r1;
        return;
    L_0x00f9:
        r1 = r8.index;
        r2 = r7.params;
        r2 = r2.getHeight();
        r0 = r0 << r2;
        r0 = r0 + -2;
        if (r1 >= r0) goto L_0x0126;
    L_0x0108:
        if (r3 == 0) goto L_0x0126;
    L_0x010a:
        if (r4 == 0) goto L_0x0126;
    L_0x010c:
        r0 = new org.bouncycastle.pqc.crypto.xmss.BDS;
        r2 = r7.params;
        r1 = new org.bouncycastle.pqc.crypto.xmss.OTSHashAddress$Builder;
        r1.<init>();
        r1 = r1.build();
        r5 = r1;
        r5 = (org.bouncycastle.pqc.crypto.xmss.OTSHashAddress) r5;
        r6 = r8.index;
        r1 = r0;
        r1.<init>(r2, r3, r4, r5, r6);
        goto L_0x0077;
    L_0x0126:
        r0 = new org.bouncycastle.pqc.crypto.xmss.BDS;
        r1 = r7.params;
        r8 = r8.index;
        r0.<init>(r1, r8);
        goto L_0x0077;
        return;
    L_0x0134:
        r8 = new java.lang.NullPointerException;
        r0 = "params == null";
        r8.<init>(r0);
        throw r8;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.pqc.crypto.xmss.XMSSPrivateKeyParameters.<init>(org.bouncycastle.pqc.crypto.xmss.XMSSPrivateKeyParameters$Builder):void");
    }

    BDS getBDSState() {
        return this.bdsState;
    }

    public int getIndex() {
        return this.bdsState.getIndex();
    }

    public XMSSPrivateKeyParameters getNextKey() {
        Builder withRoot;
        BDS nextState;
        if (getIndex() < (1 << this.params.getHeight()) - 1) {
            withRoot = new Builder(this.params).withSecretKeySeed(this.secretKeySeed).withSecretKeyPRF(this.secretKeyPRF).withPublicSeed(this.publicSeed).withRoot(this.root);
            nextState = this.bdsState.getNextState(this.publicSeed, this.secretKeySeed, (OTSHashAddress) new Builder().build());
        } else {
            withRoot = new Builder(this.params).withSecretKeySeed(this.secretKeySeed).withSecretKeyPRF(this.secretKeyPRF).withPublicSeed(this.publicSeed).withRoot(this.root);
            nextState = new BDS(this.params, getIndex() + 1);
        }
        return withRoot.withBDSState(nextState).build();
    }

    public XMSSParameters getParameters() {
        return this.params;
    }

    public byte[] getPublicSeed() {
        return XMSSUtil.cloneArray(this.publicSeed);
    }

    public byte[] getRoot() {
        return XMSSUtil.cloneArray(this.root);
    }

    public byte[] getSecretKeyPRF() {
        return XMSSUtil.cloneArray(this.secretKeyPRF);
    }

    public byte[] getSecretKeySeed() {
        return XMSSUtil.cloneArray(this.secretKeySeed);
    }

    public byte[] toByteArray() {
        int digestSize = this.params.getDigestSize();
        byte[] bArr = new byte[((((4 + digestSize) + digestSize) + digestSize) + digestSize)];
        Pack.intToBigEndian(this.bdsState.getIndex(), bArr, 0);
        XMSSUtil.copyBytesAtOffset(bArr, this.secretKeySeed, 4);
        int i = 4 + digestSize;
        XMSSUtil.copyBytesAtOffset(bArr, this.secretKeyPRF, i);
        i += digestSize;
        XMSSUtil.copyBytesAtOffset(bArr, this.publicSeed, i);
        XMSSUtil.copyBytesAtOffset(bArr, this.root, i + digestSize);
        try {
            return Arrays.concatenate(bArr, XMSSUtil.serialize(this.bdsState));
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error serializing bds state: ");
            stringBuilder.append(e.getMessage());
            throw new RuntimeException(stringBuilder.toString());
        }
    }
}
