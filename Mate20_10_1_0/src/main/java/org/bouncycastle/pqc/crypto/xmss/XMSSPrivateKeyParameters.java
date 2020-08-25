package org.bouncycastle.pqc.crypto.xmss;

import java.io.IOException;
import org.bouncycastle.pqc.crypto.xmss.OTSHashAddress;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Encodable;
import org.bouncycastle.util.Pack;

public final class XMSSPrivateKeyParameters extends XMSSKeyParameters implements XMSSStoreableObjectInterface, Encodable {
    private volatile BDS bdsState;
    private final XMSSParameters params;
    private final byte[] publicSeed;
    private final byte[] root;
    private final byte[] secretKeyPRF;
    private final byte[] secretKeySeed;

    public static class Builder {
        /* access modifiers changed from: private */
        public BDS bdsState = null;
        /* access modifiers changed from: private */
        public int index = 0;
        /* access modifiers changed from: private */
        public int maxIndex = -1;
        /* access modifiers changed from: private */
        public final XMSSParameters params;
        /* access modifiers changed from: private */
        public byte[] privateKey = null;
        /* access modifiers changed from: private */
        public byte[] publicSeed = null;
        /* access modifiers changed from: private */
        public byte[] root = null;
        /* access modifiers changed from: private */
        public byte[] secretKeyPRF = null;
        /* access modifiers changed from: private */
        public byte[] secretKeySeed = null;

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

        public Builder withMaxIndex(int i) {
            this.maxIndex = i;
            return this;
        }

        public Builder withPrivateKey(byte[] bArr) {
            this.privateKey = XMSSUtil.cloneArray(bArr);
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

    private XMSSPrivateKeyParameters(Builder builder) {
        super(true, builder.params.getTreeDigest());
        this.params = builder.params;
        XMSSParameters xMSSParameters = this.params;
        if (xMSSParameters != null) {
            int treeDigestSize = xMSSParameters.getTreeDigestSize();
            byte[] access$100 = builder.privateKey;
            if (access$100 != null) {
                int height = this.params.getHeight();
                int bigEndianToInt = Pack.bigEndianToInt(access$100, 0);
                if (XMSSUtil.isIndexValid(height, (long) bigEndianToInt)) {
                    this.secretKeySeed = XMSSUtil.extractBytesAtOffset(access$100, 4, treeDigestSize);
                    int i = 4 + treeDigestSize;
                    this.secretKeyPRF = XMSSUtil.extractBytesAtOffset(access$100, i, treeDigestSize);
                    int i2 = i + treeDigestSize;
                    this.publicSeed = XMSSUtil.extractBytesAtOffset(access$100, i2, treeDigestSize);
                    int i3 = i2 + treeDigestSize;
                    this.root = XMSSUtil.extractBytesAtOffset(access$100, i3, treeDigestSize);
                    int i4 = i3 + treeDigestSize;
                    try {
                        BDS bds = (BDS) XMSSUtil.deserialize(XMSSUtil.extractBytesAtOffset(access$100, i4, access$100.length - i4), BDS.class);
                        if (bds.getIndex() == bigEndianToInt) {
                            this.bdsState = bds.withWOTSDigest(builder.params.getTreeDigestOID());
                            return;
                        }
                        throw new IllegalStateException("serialized BDS has wrong index");
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e.getMessage(), e);
                    } catch (ClassNotFoundException e2) {
                        throw new IllegalArgumentException(e2.getMessage(), e2);
                    }
                } else {
                    throw new IllegalArgumentException("index out of bounds");
                }
            } else {
                byte[] access$200 = builder.secretKeySeed;
                if (access$200 == null) {
                    this.secretKeySeed = new byte[treeDigestSize];
                } else if (access$200.length == treeDigestSize) {
                    this.secretKeySeed = access$200;
                } else {
                    throw new IllegalArgumentException("size of secretKeySeed needs to be equal size of digest");
                }
                byte[] access$300 = builder.secretKeyPRF;
                if (access$300 == null) {
                    access$300 = new byte[treeDigestSize];
                } else if (access$300.length != treeDigestSize) {
                    throw new IllegalArgumentException("size of secretKeyPRF needs to be equal size of digest");
                }
                this.secretKeyPRF = access$300;
                byte[] access$400 = builder.publicSeed;
                if (access$400 == null) {
                    this.publicSeed = new byte[treeDigestSize];
                } else if (access$400.length == treeDigestSize) {
                    this.publicSeed = access$400;
                } else {
                    throw new IllegalArgumentException("size of publicSeed needs to be equal size of digest");
                }
                byte[] access$500 = builder.root;
                if (access$500 == null) {
                    this.root = new byte[treeDigestSize];
                } else if (access$500.length == treeDigestSize) {
                    this.root = access$500;
                } else {
                    throw new IllegalArgumentException("size of root needs to be equal size of digest");
                }
                BDS access$600 = builder.bdsState;
                if (access$600 == null) {
                    if (builder.index >= (1 << this.params.getHeight()) - 2 || access$400 == null || access$200 == null) {
                        XMSSParameters xMSSParameters2 = this.params;
                        access$600 = new BDS(xMSSParameters2, (1 << xMSSParameters2.getHeight()) - 1, builder.index);
                    } else {
                        access$600 = new BDS(this.params, access$400, access$200, (OTSHashAddress) new OTSHashAddress.Builder().build(), builder.index);
                    }
                }
                this.bdsState = access$600;
                if (builder.maxIndex >= 0 && builder.maxIndex != this.bdsState.getMaxIndex()) {
                    throw new IllegalArgumentException("maxIndex set but not reflected in state");
                }
            }
        } else {
            throw new NullPointerException("params == null");
        }
    }

    public XMSSPrivateKeyParameters extractKeyShard(int i) {
        XMSSPrivateKeyParameters build;
        if (i >= 1) {
            synchronized (this) {
                long j = (long) i;
                if (j <= getUsagesRemaining()) {
                    build = new Builder(this.params).withSecretKeySeed(this.secretKeySeed).withSecretKeyPRF(this.secretKeyPRF).withPublicSeed(this.publicSeed).withRoot(this.root).withIndex(getIndex()).withBDSState(this.bdsState.withMaxIndex((this.bdsState.getIndex() + i) - 1, this.params.getTreeDigestOID())).build();
                    if (j == getUsagesRemaining()) {
                        this.bdsState = new BDS(this.params, this.bdsState.getMaxIndex(), getIndex() + i);
                    } else {
                        OTSHashAddress oTSHashAddress = (OTSHashAddress) new OTSHashAddress.Builder().build();
                        for (int i2 = 0; i2 != i; i2++) {
                            this.bdsState = this.bdsState.getNextState(this.publicSeed, this.secretKeySeed, oTSHashAddress);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("usageCount exceeds usages remaining");
                }
            }
            return build;
        }
        throw new IllegalArgumentException("cannot ask for a shard with 0 keys");
    }

    /* access modifiers changed from: package-private */
    public BDS getBDSState() {
        return this.bdsState;
    }

    @Override // org.bouncycastle.util.Encodable
    public byte[] getEncoded() throws IOException {
        byte[] byteArray;
        synchronized (this) {
            byteArray = toByteArray();
        }
        return byteArray;
    }

    public int getIndex() {
        return this.bdsState.getIndex();
    }

    public XMSSPrivateKeyParameters getNextKey() {
        XMSSPrivateKeyParameters extractKeyShard;
        synchronized (this) {
            extractKeyShard = extractKeyShard(1);
        }
        return extractKeyShard;
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

    public long getUsagesRemaining() {
        long maxIndex;
        synchronized (this) {
            maxIndex = (long) ((this.bdsState.getMaxIndex() - getIndex()) + 1);
        }
        return maxIndex;
    }

    /* access modifiers changed from: package-private */
    public XMSSPrivateKeyParameters rollKey() {
        synchronized (this) {
            this.bdsState = this.bdsState.getIndex() < this.bdsState.getMaxIndex() ? this.bdsState.getNextState(this.publicSeed, this.secretKeySeed, (OTSHashAddress) new OTSHashAddress.Builder().build()) : new BDS(this.params, this.bdsState.getMaxIndex(), this.bdsState.getMaxIndex() + 1);
        }
        return this;
    }

    @Override // org.bouncycastle.pqc.crypto.xmss.XMSSStoreableObjectInterface
    public byte[] toByteArray() {
        byte[] concatenate;
        synchronized (this) {
            int treeDigestSize = this.params.getTreeDigestSize();
            byte[] bArr = new byte[(treeDigestSize + 4 + treeDigestSize + treeDigestSize + treeDigestSize)];
            Pack.intToBigEndian(this.bdsState.getIndex(), bArr, 0);
            XMSSUtil.copyBytesAtOffset(bArr, this.secretKeySeed, 4);
            int i = 4 + treeDigestSize;
            XMSSUtil.copyBytesAtOffset(bArr, this.secretKeyPRF, i);
            int i2 = i + treeDigestSize;
            XMSSUtil.copyBytesAtOffset(bArr, this.publicSeed, i2);
            XMSSUtil.copyBytesAtOffset(bArr, this.root, i2 + treeDigestSize);
            try {
                concatenate = Arrays.concatenate(bArr, XMSSUtil.serialize(this.bdsState));
            } catch (IOException e) {
                throw new RuntimeException("error serializing bds state: " + e.getMessage());
            }
        }
        return concatenate;
    }
}
