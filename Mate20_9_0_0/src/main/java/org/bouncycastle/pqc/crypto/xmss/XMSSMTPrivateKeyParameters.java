package org.bouncycastle.pqc.crypto.xmss;

import java.io.IOException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.util.Arrays;

public final class XMSSMTPrivateKeyParameters extends AsymmetricKeyParameter implements XMSSStoreableObjectInterface {
    private final BDSStateMap bdsState;
    private final long index;
    private final XMSSMTParameters params;
    private final byte[] publicSeed;
    private final byte[] root;
    private final byte[] secretKeyPRF;
    private final byte[] secretKeySeed;

    public static class Builder {
        private BDSStateMap bdsState = null;
        private long index = 0;
        private final XMSSMTParameters params;
        private byte[] privateKey = null;
        private byte[] publicSeed = null;
        private byte[] root = null;
        private byte[] secretKeyPRF = null;
        private byte[] secretKeySeed = null;
        private XMSSParameters xmss = null;

        public Builder(XMSSMTParameters xMSSMTParameters) {
            this.params = xMSSMTParameters;
        }

        public XMSSMTPrivateKeyParameters build() {
            return new XMSSMTPrivateKeyParameters(this);
        }

        public Builder withBDSState(BDSStateMap bDSStateMap) {
            this.bdsState = bDSStateMap;
            return this;
        }

        public Builder withIndex(long j) {
            this.index = j;
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

    private XMSSMTPrivateKeyParameters(Builder builder) {
        BDSStateMap access$800;
        super(true);
        this.params = builder.params;
        if (this.params != null) {
            int digestSize = this.params.getDigestSize();
            byte[] access$100 = builder.privateKey;
            if (access$100 == null) {
                this.index = builder.index;
                byte[] access$400 = builder.secretKeySeed;
                if (access$400 == null) {
                    this.secretKeySeed = new byte[digestSize];
                } else if (access$400.length == digestSize) {
                    this.secretKeySeed = access$400;
                } else {
                    throw new IllegalArgumentException("size of secretKeySeed needs to be equal size of digest");
                }
                access$100 = builder.secretKeyPRF;
                if (access$100 == null) {
                    access$100 = new byte[digestSize];
                } else if (access$100.length != digestSize) {
                    throw new IllegalArgumentException("size of secretKeyPRF needs to be equal size of digest");
                }
                this.secretKeyPRF = access$100;
                byte[] access$600 = builder.publicSeed;
                if (access$600 == null) {
                    this.publicSeed = new byte[digestSize];
                } else if (access$600.length == digestSize) {
                    this.publicSeed = access$600;
                } else {
                    throw new IllegalArgumentException("size of publicSeed needs to be equal size of digest");
                }
                access$100 = builder.root;
                if (access$100 == null) {
                    this.root = new byte[digestSize];
                } else if (access$100.length == digestSize) {
                    this.root = access$100;
                } else {
                    throw new IllegalArgumentException("size of root needs to be equal size of digest");
                }
                access$800 = builder.bdsState;
                if (access$800 == null) {
                    if (!XMSSUtil.isIndexValid(this.params.getHeight(), builder.index) || access$600 == null || access$400 == null) {
                        this.bdsState = new BDSStateMap();
                        return;
                    }
                    BDSStateMap bDSStateMap = new BDSStateMap(this.params, builder.index, access$600, access$400);
                }
            } else if (builder.xmss != null) {
                int height = this.params.getHeight();
                int i = (height + 7) / 8;
                this.index = XMSSUtil.bytesToXBigEndian(access$100, 0, i);
                if (XMSSUtil.isIndexValid(height, this.index)) {
                    int i2 = 0 + i;
                    this.secretKeySeed = XMSSUtil.extractBytesAtOffset(access$100, i2, digestSize);
                    i2 += digestSize;
                    this.secretKeyPRF = XMSSUtil.extractBytesAtOffset(access$100, i2, digestSize);
                    i2 += digestSize;
                    this.publicSeed = XMSSUtil.extractBytesAtOffset(access$100, i2, digestSize);
                    i2 += digestSize;
                    this.root = XMSSUtil.extractBytesAtOffset(access$100, i2, digestSize);
                    i2 += digestSize;
                    try {
                        access$800 = (BDSStateMap) XMSSUtil.deserialize(XMSSUtil.extractBytesAtOffset(access$100, i2, access$100.length - i2));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e2) {
                        e2.printStackTrace();
                    }
                    access$800.setXMSS(builder.xmss);
                } else {
                    throw new IllegalArgumentException("index out of bounds");
                }
            } else {
                throw new NullPointerException("xmss == null");
            }
            this.bdsState = access$800;
        }
        throw new NullPointerException("params == null");
        access$800 = null;
        access$800.setXMSS(builder.xmss);
        this.bdsState = access$800;
    }

    BDSStateMap getBDSState() {
        return this.bdsState;
    }

    public long getIndex() {
        return this.index;
    }

    public XMSSMTPrivateKeyParameters getNextKey() {
        return new Builder(this.params).withIndex(this.index + 1).withSecretKeySeed(this.secretKeySeed).withSecretKeyPRF(this.secretKeyPRF).withPublicSeed(this.publicSeed).withRoot(this.root).withBDSState(new BDSStateMap(this.bdsState, this.params, getIndex(), this.publicSeed, this.secretKeySeed)).build();
    }

    public XMSSMTParameters getParameters() {
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
        int height = (this.params.getHeight() + 7) / 8;
        byte[] bArr = new byte[((((height + digestSize) + digestSize) + digestSize) + digestSize)];
        XMSSUtil.copyBytesAtOffset(bArr, XMSSUtil.toBytesBigEndian(this.index, height), 0);
        int i = 0 + height;
        XMSSUtil.copyBytesAtOffset(bArr, this.secretKeySeed, i);
        i += digestSize;
        XMSSUtil.copyBytesAtOffset(bArr, this.secretKeyPRF, i);
        i += digestSize;
        XMSSUtil.copyBytesAtOffset(bArr, this.publicSeed, i);
        XMSSUtil.copyBytesAtOffset(bArr, this.root, i + digestSize);
        try {
            return Arrays.concatenate(bArr, XMSSUtil.serialize(this.bdsState));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("error serializing bds state");
        }
    }
}
