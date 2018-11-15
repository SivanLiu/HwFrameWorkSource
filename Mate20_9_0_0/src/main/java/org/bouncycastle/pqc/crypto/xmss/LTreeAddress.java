package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.util.Pack;

final class LTreeAddress extends XMSSAddress {
    private static final int TYPE = 1;
    private final int lTreeAddress;
    private final int treeHeight;
    private final int treeIndex;

    protected static class Builder extends Builder<Builder> {
        private int lTreeAddress = 0;
        private int treeHeight = 0;
        private int treeIndex = 0;

        protected Builder() {
            super(1);
        }

        protected XMSSAddress build() {
            return new LTreeAddress(this);
        }

        protected Builder getThis() {
            return this;
        }

        protected Builder withLTreeAddress(int i) {
            this.lTreeAddress = i;
            return this;
        }

        protected Builder withTreeHeight(int i) {
            this.treeHeight = i;
            return this;
        }

        protected Builder withTreeIndex(int i) {
            this.treeIndex = i;
            return this;
        }
    }

    private LTreeAddress(Builder builder) {
        super(builder);
        this.lTreeAddress = builder.lTreeAddress;
        this.treeHeight = builder.treeHeight;
        this.treeIndex = builder.treeIndex;
    }

    protected int getLTreeAddress() {
        return this.lTreeAddress;
    }

    protected int getTreeHeight() {
        return this.treeHeight;
    }

    protected int getTreeIndex() {
        return this.treeIndex;
    }

    protected byte[] toByteArray() {
        byte[] toByteArray = super.toByteArray();
        Pack.intToBigEndian(this.lTreeAddress, toByteArray, 16);
        Pack.intToBigEndian(this.treeHeight, toByteArray, 20);
        Pack.intToBigEndian(this.treeIndex, toByteArray, 24);
        return toByteArray;
    }
}
