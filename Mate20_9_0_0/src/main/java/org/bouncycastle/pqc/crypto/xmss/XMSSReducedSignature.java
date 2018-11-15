package org.bouncycastle.pqc.crypto.xmss;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class XMSSReducedSignature implements XMSSStoreableObjectInterface {
    private final List<XMSSNode> authPath;
    private final XMSSParameters params;
    private final WOTSPlusSignature wotsPlusSignature;

    public static class Builder {
        private List<XMSSNode> authPath = null;
        private final XMSSParameters params;
        private byte[] reducedSignature = null;
        private WOTSPlusSignature wotsPlusSignature = null;

        public Builder(XMSSParameters xMSSParameters) {
            this.params = xMSSParameters;
        }

        public XMSSReducedSignature build() {
            return new XMSSReducedSignature(this);
        }

        public Builder withAuthPath(List<XMSSNode> list) {
            this.authPath = list;
            return this;
        }

        public Builder withReducedSignature(byte[] bArr) {
            this.reducedSignature = XMSSUtil.cloneArray(bArr);
            return this;
        }

        public Builder withWOTSPlusSignature(WOTSPlusSignature wOTSPlusSignature) {
            this.wotsPlusSignature = wOTSPlusSignature;
            return this;
        }
    }

    protected XMSSReducedSignature(Builder builder) {
        this.params = builder.params;
        if (this.params != null) {
            List arrayList;
            int digestSize = this.params.getDigestSize();
            int len = this.params.getWOTSPlus().getParams().getLen();
            int height = this.params.getHeight();
            byte[] access$100 = builder.reducedSignature;
            if (access$100 != null) {
                if (access$100.length == (len * digestSize) + (height * digestSize)) {
                    byte[][] bArr = new byte[len][];
                    len = 0;
                    int i = 0;
                    int i2 = i;
                    while (i < bArr.length) {
                        bArr[i] = XMSSUtil.extractBytesAtOffset(access$100, i2, digestSize);
                        i2 += digestSize;
                        i++;
                    }
                    this.wotsPlusSignature = new WOTSPlusSignature(this.params.getWOTSPlus().getParams(), bArr);
                    arrayList = new ArrayList();
                    while (len < height) {
                        arrayList.add(new XMSSNode(len, XMSSUtil.extractBytesAtOffset(access$100, i2, digestSize)));
                        i2 += digestSize;
                        len++;
                    }
                } else {
                    throw new IllegalArgumentException("signature has wrong size");
                }
            }
            WOTSPlusSignature access$200 = builder.wotsPlusSignature;
            if (access$200 == null) {
                access$200 = new WOTSPlusSignature(this.params.getWOTSPlus().getParams(), (byte[][]) Array.newInstance(byte.class, new int[]{len, digestSize}));
            }
            this.wotsPlusSignature = access$200;
            arrayList = builder.authPath;
            if (arrayList == null) {
                arrayList = new ArrayList();
            } else if (arrayList.size() != height) {
                throw new IllegalArgumentException("size of authPath needs to be equal to height of tree");
            }
            this.authPath = arrayList;
            return;
        }
        throw new NullPointerException("params == null");
    }

    public List<XMSSNode> getAuthPath() {
        return this.authPath;
    }

    public XMSSParameters getParams() {
        return this.params;
    }

    public WOTSPlusSignature getWOTSPlusSignature() {
        return this.wotsPlusSignature;
    }

    public byte[] toByteArray() {
        int digestSize = this.params.getDigestSize();
        byte[] bArr = new byte[((this.params.getWOTSPlus().getParams().getLen() * digestSize) + (this.params.getHeight() * digestSize))];
        byte[][] toByteArray = this.wotsPlusSignature.toByteArray();
        int i = 0;
        int i2 = 0;
        int i3 = i2;
        while (i2 < toByteArray.length) {
            XMSSUtil.copyBytesAtOffset(bArr, toByteArray[i2], i3);
            i3 += digestSize;
            i2++;
        }
        while (i < this.authPath.size()) {
            XMSSUtil.copyBytesAtOffset(bArr, ((XMSSNode) this.authPath.get(i)).getValue(), i3);
            i3 += digestSize;
            i++;
        }
        return bArr;
    }
}
