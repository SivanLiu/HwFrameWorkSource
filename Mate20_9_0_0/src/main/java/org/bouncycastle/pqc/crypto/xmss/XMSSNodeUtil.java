package org.bouncycastle.pqc.crypto.xmss;

class XMSSNodeUtil {
    XMSSNodeUtil() {
    }

    static XMSSNode lTree(WOTSPlus wOTSPlus, WOTSPlusPublicKeyParameters wOTSPlusPublicKeyParameters, LTreeAddress lTreeAddress) {
        if (wOTSPlusPublicKeyParameters == null) {
            throw new NullPointerException("publicKey == null");
        } else if (lTreeAddress != null) {
            int len = wOTSPlus.getParams().getLen();
            byte[][] toByteArray = wOTSPlusPublicKeyParameters.toByteArray();
            XMSSNode[] xMSSNodeArr = new XMSSNode[toByteArray.length];
            for (int i = 0; i < toByteArray.length; i++) {
                xMSSNodeArr[i] = new XMSSNode(0, toByteArray[i]);
            }
            Builder withTreeIndex = ((Builder) ((Builder) new Builder().withLayerAddress(lTreeAddress.getLayerAddress())).withTreeAddress(lTreeAddress.getTreeAddress())).withLTreeAddress(lTreeAddress.getLTreeAddress()).withTreeHeight(0).withTreeIndex(lTreeAddress.getTreeIndex());
            int keyAndMask = lTreeAddress.getKeyAndMask();
            while (true) {
                LTreeAddress lTreeAddress2 = (LTreeAddress) ((Builder) withTreeIndex.withKeyAndMask(keyAndMask)).build();
                if (len <= 1) {
                    return xMSSNodeArr[0];
                }
                double d;
                LTreeAddress lTreeAddress3 = lTreeAddress2;
                int i2 = 0;
                while (true) {
                    d = (double) (len / 2);
                    if (i2 >= ((int) Math.floor(d))) {
                        break;
                    }
                    lTreeAddress3 = (LTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(lTreeAddress3.getLayerAddress())).withTreeAddress(lTreeAddress3.getTreeAddress())).withLTreeAddress(lTreeAddress3.getLTreeAddress()).withTreeHeight(lTreeAddress3.getTreeHeight()).withTreeIndex(i2).withKeyAndMask(lTreeAddress3.getKeyAndMask())).build();
                    int i3 = 2 * i2;
                    xMSSNodeArr[i2] = randomizeHash(wOTSPlus, xMSSNodeArr[i3], xMSSNodeArr[i3 + 1], lTreeAddress3);
                    i2++;
                }
                if (len % 2 == 1) {
                    xMSSNodeArr[(int) Math.floor(d)] = xMSSNodeArr[len - 1];
                }
                len = (int) Math.ceil(((double) len) / 2.0d);
                withTreeIndex = ((Builder) ((Builder) new Builder().withLayerAddress(lTreeAddress3.getLayerAddress())).withTreeAddress(lTreeAddress3.getTreeAddress())).withLTreeAddress(lTreeAddress3.getLTreeAddress()).withTreeHeight(lTreeAddress3.getTreeHeight() + 1).withTreeIndex(lTreeAddress3.getTreeIndex());
                keyAndMask = lTreeAddress3.getKeyAndMask();
            }
        } else {
            throw new NullPointerException("address == null");
        }
    }

    static XMSSNode randomizeHash(WOTSPlus wOTSPlus, XMSSNode xMSSNode, XMSSNode xMSSNode2, XMSSAddress xMSSAddress) {
        if (xMSSNode == null) {
            throw new NullPointerException("left == null");
        } else if (xMSSNode2 == null) {
            throw new NullPointerException("right == null");
        } else if (xMSSNode.getHeight() != xMSSNode2.getHeight()) {
            throw new IllegalStateException("height of both nodes must be equal");
        } else if (xMSSAddress != null) {
            LTreeAddress lTreeAddress;
            HashTreeAddress hashTreeAddress;
            byte[] publicSeed = wOTSPlus.getPublicSeed();
            int i = 0;
            if (xMSSAddress instanceof LTreeAddress) {
                lTreeAddress = (LTreeAddress) xMSSAddress;
                xMSSAddress = (LTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(lTreeAddress.getLayerAddress())).withTreeAddress(lTreeAddress.getTreeAddress())).withLTreeAddress(lTreeAddress.getLTreeAddress()).withTreeHeight(lTreeAddress.getTreeHeight()).withTreeIndex(lTreeAddress.getTreeIndex()).withKeyAndMask(0)).build();
            } else if (xMSSAddress instanceof HashTreeAddress) {
                hashTreeAddress = (HashTreeAddress) xMSSAddress;
                xMSSAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight()).withTreeIndex(hashTreeAddress.getTreeIndex()).withKeyAndMask(0)).build();
            }
            byte[] PRF = wOTSPlus.getKhf().PRF(publicSeed, xMSSAddress.toByteArray());
            if (xMSSAddress instanceof LTreeAddress) {
                lTreeAddress = (LTreeAddress) xMSSAddress;
                xMSSAddress = (LTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(lTreeAddress.getLayerAddress())).withTreeAddress(lTreeAddress.getTreeAddress())).withLTreeAddress(lTreeAddress.getLTreeAddress()).withTreeHeight(lTreeAddress.getTreeHeight()).withTreeIndex(lTreeAddress.getTreeIndex()).withKeyAndMask(1)).build();
            } else if (xMSSAddress instanceof HashTreeAddress) {
                hashTreeAddress = (HashTreeAddress) xMSSAddress;
                xMSSAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight()).withTreeIndex(hashTreeAddress.getTreeIndex()).withKeyAndMask(1)).build();
            }
            byte[] PRF2 = wOTSPlus.getKhf().PRF(publicSeed, xMSSAddress.toByteArray());
            if (xMSSAddress instanceof LTreeAddress) {
                lTreeAddress = (LTreeAddress) xMSSAddress;
                xMSSAddress = (LTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(lTreeAddress.getLayerAddress())).withTreeAddress(lTreeAddress.getTreeAddress())).withLTreeAddress(lTreeAddress.getLTreeAddress()).withTreeHeight(lTreeAddress.getTreeHeight()).withTreeIndex(lTreeAddress.getTreeIndex()).withKeyAndMask(2)).build();
            } else if (xMSSAddress instanceof HashTreeAddress) {
                hashTreeAddress = (HashTreeAddress) xMSSAddress;
                xMSSAddress = (HashTreeAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(hashTreeAddress.getLayerAddress())).withTreeAddress(hashTreeAddress.getTreeAddress())).withTreeHeight(hashTreeAddress.getTreeHeight()).withTreeIndex(hashTreeAddress.getTreeIndex()).withKeyAndMask(2)).build();
            }
            byte[] PRF3 = wOTSPlus.getKhf().PRF(publicSeed, xMSSAddress.toByteArray());
            int digestSize = wOTSPlus.getParams().getDigestSize();
            byte[] bArr = new byte[(2 * digestSize)];
            for (int i2 = 0; i2 < digestSize; i2++) {
                bArr[i2] = (byte) (xMSSNode.getValue()[i2] ^ PRF2[i2]);
            }
            while (i < digestSize) {
                bArr[i + digestSize] = (byte) (xMSSNode2.getValue()[i] ^ PRF3[i]);
                i++;
            }
            return new XMSSNode(xMSSNode.getHeight(), wOTSPlus.getKhf().H(PRF, bArr));
        } else {
            throw new NullPointerException("address == null");
        }
    }
}
