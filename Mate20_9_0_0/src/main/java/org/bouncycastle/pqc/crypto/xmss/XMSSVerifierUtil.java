package org.bouncycastle.pqc.crypto.xmss;

class XMSSVerifierUtil {
    XMSSVerifierUtil() {
    }

    static XMSSNode getRootNodeFromSignature(WOTSPlus wOTSPlus, int i, byte[] bArr, XMSSReducedSignature xMSSReducedSignature, OTSHashAddress oTSHashAddress, int i2) {
        if (bArr.length != wOTSPlus.getParams().getDigestSize()) {
            throw new IllegalArgumentException("size of messageDigest needs to be equal to size of digest");
        } else if (xMSSReducedSignature == null) {
            throw new NullPointerException("signature == null");
        } else if (oTSHashAddress != null) {
            LTreeAddress lTreeAddress = (LTreeAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withLTreeAddress(oTSHashAddress.getOTSAddress()).build();
            HashTreeAddress hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withTreeIndex(oTSHashAddress.getOTSAddress()).build();
            XMSSNode[] xMSSNodeArr = new XMSSNode[2];
            xMSSNodeArr[0] = XMSSNodeUtil.lTree(wOTSPlus, wOTSPlus.getPublicKeyFromSignature(bArr, xMSSReducedSignature.getWOTSPlusSignature(), oTSHashAddress), lTreeAddress);
            for (int i3 = 0; i3 < i; i3++) {
                hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(i3).withTreeIndex(hashTreeAddress.getTreeIndex()).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build();
                if (Math.floor((double) (i2 / (1 << i3))) % 2.0d == 0.0d) {
                    hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight()).withTreeIndex(hashTreeAddress.getTreeIndex() / 2).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build();
                    xMSSNodeArr[1] = XMSSNodeUtil.randomizeHash(wOTSPlus, xMSSNodeArr[0], (XMSSNode) xMSSReducedSignature.getAuthPath().get(i3), hashTreeAddress);
                    xMSSNodeArr[1] = new XMSSNode(xMSSNodeArr[1].getHeight() + 1, xMSSNodeArr[1].getValue());
                } else {
                    hashTreeAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight()).withTreeIndex((hashTreeAddress.getTreeIndex() - 1) / 2).withKeyAndMask(hashTreeAddress.getKeyAndMask())).build();
                    xMSSNodeArr[1] = XMSSNodeUtil.randomizeHash(wOTSPlus, (XMSSNode) xMSSReducedSignature.getAuthPath().get(i3), xMSSNodeArr[0], hashTreeAddress);
                    xMSSNodeArr[1] = new XMSSNode(xMSSNodeArr[1].getHeight() + 1, xMSSNodeArr[1].getValue());
                }
                xMSSNodeArr[0] = xMSSNodeArr[1];
            }
            return xMSSNodeArr[0];
        } else {
            throw new NullPointerException("otsHashAddress == null");
        }
    }
}
