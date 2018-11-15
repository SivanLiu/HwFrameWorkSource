package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.util.Pack;

public abstract class XMSSAddress {
    private final int keyAndMask;
    private final int layerAddress;
    private final long treeAddress;
    private final int type;

    protected static abstract class Builder<T extends Builder> {
        private int keyAndMask = 0;
        private int layerAddress = 0;
        private long treeAddress = 0;
        private final int type;

        protected Builder(int i) {
            this.type = i;
        }

        protected abstract XMSSAddress build();

        protected abstract T getThis();

        protected T withKeyAndMask(int i) {
            this.keyAndMask = i;
            return getThis();
        }

        protected T withLayerAddress(int i) {
            this.layerAddress = i;
            return getThis();
        }

        protected T withTreeAddress(long j) {
            this.treeAddress = j;
            return getThis();
        }
    }

    protected XMSSAddress(Builder builder) {
        this.layerAddress = builder.layerAddress;
        this.treeAddress = builder.treeAddress;
        this.type = builder.type;
        this.keyAndMask = builder.keyAndMask;
    }

    public final int getKeyAndMask() {
        return this.keyAndMask;
    }

    protected final int getLayerAddress() {
        return this.layerAddress;
    }

    protected final long getTreeAddress() {
        return this.treeAddress;
    }

    public final int getType() {
        return this.type;
    }

    protected byte[] toByteArray() {
        byte[] bArr = new byte[32];
        Pack.intToBigEndian(this.layerAddress, bArr, 0);
        Pack.longToBigEndian(this.treeAddress, bArr, 4);
        Pack.intToBigEndian(this.type, bArr, 12);
        Pack.intToBigEndian(this.keyAndMask, bArr, 28);
        return bArr;
    }
}
