package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.util.Pack;

public final class XMSSSignature extends XMSSReducedSignature implements XMSSStoreableObjectInterface {
    private final int index;
    private final byte[] random;

    public static class Builder extends org.bouncycastle.pqc.crypto.xmss.XMSSReducedSignature.Builder {
        private int index = 0;
        private final XMSSParameters params;
        private byte[] random = null;

        public Builder(XMSSParameters xMSSParameters) {
            super(xMSSParameters);
            this.params = xMSSParameters;
        }

        public XMSSSignature build() {
            return new XMSSSignature(this);
        }

        public Builder withIndex(int i) {
            this.index = i;
            return this;
        }

        public Builder withRandom(byte[] bArr) {
            this.random = XMSSUtil.cloneArray(bArr);
            return this;
        }

        public Builder withSignature(byte[] bArr) {
            if (bArr != null) {
                int digestSize = this.params.getDigestSize();
                int len = this.params.getWOTSPlus().getParams().getLen() * digestSize;
                int height = this.params.getHeight() * digestSize;
                this.index = Pack.bigEndianToInt(bArr, 0);
                this.random = XMSSUtil.extractBytesAtOffset(bArr, 4, digestSize);
                withReducedSignature(XMSSUtil.extractBytesAtOffset(bArr, 4 + digestSize, len + height));
                return this;
            }
            throw new NullPointerException("signature == null");
        }
    }

    private XMSSSignature(Builder builder) {
        super(builder);
        this.index = builder.index;
        int digestSize = getParams().getDigestSize();
        byte[] access$100 = builder.random;
        if (access$100 == null) {
            access$100 = new byte[digestSize];
        } else if (access$100.length != digestSize) {
            throw new IllegalArgumentException("size of random needs to be equal to size of digest");
        }
        this.random = access$100;
    }

    public int getIndex() {
        return this.index;
    }

    public byte[] getRandom() {
        return XMSSUtil.cloneArray(this.random);
    }

    public byte[] toByteArray() {
        int digestSize = getParams().getDigestSize();
        byte[] bArr = new byte[(((4 + digestSize) + (getParams().getWOTSPlus().getParams().getLen() * digestSize)) + (getParams().getHeight() * digestSize))];
        int i = 0;
        Pack.intToBigEndian(this.index, bArr, 0);
        XMSSUtil.copyBytesAtOffset(bArr, this.random, 4);
        int i2 = 4 + digestSize;
        byte[][] toByteArray = getWOTSPlusSignature().toByteArray();
        int i3 = i2;
        for (byte[] copyBytesAtOffset : toByteArray) {
            XMSSUtil.copyBytesAtOffset(bArr, copyBytesAtOffset, i3);
            i3 += digestSize;
        }
        while (i < getAuthPath().size()) {
            XMSSUtil.copyBytesAtOffset(bArr, ((XMSSNode) getAuthPath().get(i)).getValue(), i3);
            i3 += digestSize;
            i++;
        }
        return bArr;
    }
}
