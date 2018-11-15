package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.util.Pack;

final class OTSHashAddress extends XMSSAddress {
    private static final int TYPE = 0;
    private final int chainAddress;
    private final int hashAddress;
    private final int otsAddress;

    protected static class Builder extends Builder<Builder> {
        private int chainAddress = 0;
        private int hashAddress = 0;
        private int otsAddress = 0;

        protected Builder() {
            super(0);
        }

        protected XMSSAddress build() {
            return new OTSHashAddress(this);
        }

        protected Builder getThis() {
            return this;
        }

        protected Builder withChainAddress(int i) {
            this.chainAddress = i;
            return this;
        }

        protected Builder withHashAddress(int i) {
            this.hashAddress = i;
            return this;
        }

        protected Builder withOTSAddress(int i) {
            this.otsAddress = i;
            return this;
        }
    }

    private OTSHashAddress(Builder builder) {
        super(builder);
        this.otsAddress = builder.otsAddress;
        this.chainAddress = builder.chainAddress;
        this.hashAddress = builder.hashAddress;
    }

    protected int getChainAddress() {
        return this.chainAddress;
    }

    protected int getHashAddress() {
        return this.hashAddress;
    }

    protected int getOTSAddress() {
        return this.otsAddress;
    }

    protected byte[] toByteArray() {
        byte[] toByteArray = super.toByteArray();
        Pack.intToBigEndian(this.otsAddress, toByteArray, 16);
        Pack.intToBigEndian(this.chainAddress, toByteArray, 20);
        Pack.intToBigEndian(this.hashAddress, toByteArray, 24);
        return toByteArray;
    }
}
