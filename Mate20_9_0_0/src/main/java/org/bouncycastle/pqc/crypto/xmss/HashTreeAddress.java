package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.util.Pack;

final class HashTreeAddress extends XMSSAddress {
    private static final int PADDING = 0;
    private static final int TYPE = 2;
    private final int padding;
    private final int treeHeight;
    private final int treeIndex;

    protected static class Builder extends Builder<Builder> {
        private int treeHeight = 0;
        private int treeIndex = 0;

        protected Builder() {
            super(2);
        }

        protected XMSSAddress build() {
            return new HashTreeAddress(this);
        }

        protected Builder getThis() {
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

    private HashTreeAddress(Builder builder) {
        super(builder);
        this.padding = 0;
        this.treeHeight = builder.treeHeight;
        this.treeIndex = builder.treeIndex;
    }

    protected int getPadding() {
        return this.padding;
    }

    protected int getTreeHeight() {
        return this.treeHeight;
    }

    protected int getTreeIndex() {
        return this.treeIndex;
    }

    protected byte[] toByteArray() {
        byte[] toByteArray = super.toByteArray();
        Pack.intToBigEndian(this.padding, toByteArray, 16);
        Pack.intToBigEndian(this.treeHeight, toByteArray, 20);
        Pack.intToBigEndian(this.treeIndex, toByteArray, 24);
        return toByteArray;
    }
}
