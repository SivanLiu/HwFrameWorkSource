package org.bouncycastle.pqc.crypto.xmss;

import java.util.ArrayList;
import java.util.List;

final class WOTSPlus {
    private final KeyedHashFunctions khf;
    private final WOTSPlusParameters params;
    private byte[] publicSeed;
    private byte[] secretKeySeed;

    protected WOTSPlus(WOTSPlusParameters wOTSPlusParameters) {
        if (wOTSPlusParameters != null) {
            this.params = wOTSPlusParameters;
            int digestSize = wOTSPlusParameters.getDigestSize();
            this.khf = new KeyedHashFunctions(wOTSPlusParameters.getDigest(), digestSize);
            this.secretKeySeed = new byte[digestSize];
            this.publicSeed = new byte[digestSize];
            return;
        }
        throw new NullPointerException("params == null");
    }

    private byte[] chain(byte[] bArr, int i, int i2, OTSHashAddress oTSHashAddress) {
        int digestSize = this.params.getDigestSize();
        if (bArr == null) {
            throw new NullPointerException("startHash == null");
        } else if (bArr.length != digestSize) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startHash needs to be ");
            stringBuilder.append(digestSize);
            stringBuilder.append("bytes");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (oTSHashAddress == null) {
            throw new NullPointerException("otsHashAddress == null");
        } else if (oTSHashAddress.toByteArray() != null) {
            int i3 = i + i2;
            if (i3 > this.params.getWinternitzParameter() - 1) {
                throw new IllegalArgumentException("max chain length must not be greater than w");
            } else if (i2 == 0) {
                return bArr;
            } else {
                bArr = chain(bArr, i, i2 - 1, oTSHashAddress);
                i2 = 0;
                OTSHashAddress oTSHashAddress2 = (OTSHashAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withOTSAddress(oTSHashAddress.getOTSAddress()).withChainAddress(oTSHashAddress.getChainAddress()).withHashAddress(i3 - 1).withKeyAndMask(0)).build();
                byte[] PRF = this.khf.PRF(this.publicSeed, oTSHashAddress2.toByteArray());
                byte[] PRF2 = this.khf.PRF(this.publicSeed, ((OTSHashAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress2.getLayerAddress())).withTreeAddress(oTSHashAddress2.getTreeAddress())).withOTSAddress(oTSHashAddress2.getOTSAddress()).withChainAddress(oTSHashAddress2.getChainAddress()).withHashAddress(oTSHashAddress2.getHashAddress()).withKeyAndMask(1)).build()).toByteArray());
                byte[] bArr2 = new byte[digestSize];
                while (i2 < digestSize) {
                    bArr2[i2] = (byte) (bArr[i2] ^ PRF2[i2]);
                    i2++;
                }
                return this.khf.F(PRF, bArr2);
            }
        } else {
            throw new NullPointerException("otsHashAddress byte array == null");
        }
    }

    private List<Integer> convertToBaseW(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new NullPointerException("msg == null");
        } else if (i == 4 || i == 16) {
            int log2 = XMSSUtil.log2(i);
            if (i2 <= (bArr.length * 8) / log2) {
                ArrayList arrayList = new ArrayList();
                for (byte b : bArr) {
                    for (int i3 = 8 - log2; i3 >= 0; i3 -= log2) {
                        arrayList.add(Integer.valueOf((b >> i3) & (i - 1)));
                        if (arrayList.size() == i2) {
                            return arrayList;
                        }
                    }
                }
                return arrayList;
            }
            throw new IllegalArgumentException("outLength too big");
        } else {
            throw new IllegalArgumentException("w needs to be 4 or 16");
        }
    }

    private byte[] expandSecretKeySeed(int i) {
        if (i >= 0 && i < this.params.getLen()) {
            return this.khf.PRF(this.secretKeySeed, XMSSUtil.toBytesBigEndian((long) i, 32));
        }
        throw new IllegalArgumentException("index out of bounds");
    }

    protected KeyedHashFunctions getKhf() {
        return this.khf;
    }

    protected WOTSPlusParameters getParams() {
        return this.params;
    }

    protected WOTSPlusPrivateKeyParameters getPrivateKey() {
        byte[][] bArr = new byte[this.params.getLen()][];
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = expandSecretKeySeed(i);
        }
        return new WOTSPlusPrivateKeyParameters(this.params, bArr);
    }

    protected WOTSPlusPublicKeyParameters getPublicKey(OTSHashAddress oTSHashAddress) {
        if (oTSHashAddress != null) {
            byte[][] bArr = new byte[this.params.getLen()][];
            OTSHashAddress oTSHashAddress2 = oTSHashAddress;
            for (int i = 0; i < this.params.getLen(); i++) {
                oTSHashAddress2 = (OTSHashAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress2.getLayerAddress())).withTreeAddress(oTSHashAddress2.getTreeAddress())).withOTSAddress(oTSHashAddress2.getOTSAddress()).withChainAddress(i).withHashAddress(oTSHashAddress2.getHashAddress()).withKeyAndMask(oTSHashAddress2.getKeyAndMask())).build();
                bArr[i] = chain(expandSecretKeySeed(i), 0, this.params.getWinternitzParameter() - 1, oTSHashAddress2);
            }
            return new WOTSPlusPublicKeyParameters(this.params, bArr);
        }
        throw new NullPointerException("otsHashAddress == null");
    }

    protected WOTSPlusPublicKeyParameters getPublicKeyFromSignature(byte[] bArr, WOTSPlusSignature wOTSPlusSignature, OTSHashAddress oTSHashAddress) {
        if (bArr == null) {
            throw new NullPointerException("messageDigest == null");
        } else if (bArr.length != this.params.getDigestSize()) {
            throw new IllegalArgumentException("size of messageDigest needs to be equal to size of digest");
        } else if (wOTSPlusSignature == null) {
            throw new NullPointerException("signature == null");
        } else if (oTSHashAddress != null) {
            List convertToBaseW = convertToBaseW(bArr, this.params.getWinternitzParameter(), this.params.getLen1());
            int i = 0;
            int i2 = 0;
            int i3 = i2;
            while (i2 < this.params.getLen1()) {
                i3 += (this.params.getWinternitzParameter() - 1) - ((Integer) convertToBaseW.get(i2)).intValue();
                i2++;
            }
            convertToBaseW.addAll(convertToBaseW(XMSSUtil.toBytesBigEndian((long) (i3 << (8 - ((this.params.getLen2() * XMSSUtil.log2(this.params.getWinternitzParameter())) % 8))), (int) Math.ceil(((double) (this.params.getLen2() * XMSSUtil.log2(this.params.getWinternitzParameter()))) / 8.0d)), this.params.getWinternitzParameter(), this.params.getLen2()));
            byte[][] bArr2 = new byte[this.params.getLen()][];
            while (i < this.params.getLen()) {
                oTSHashAddress = (OTSHashAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withOTSAddress(oTSHashAddress.getOTSAddress()).withChainAddress(i).withHashAddress(oTSHashAddress.getHashAddress()).withKeyAndMask(oTSHashAddress.getKeyAndMask())).build();
                bArr2[i] = chain(wOTSPlusSignature.toByteArray()[i], ((Integer) convertToBaseW.get(i)).intValue(), (this.params.getWinternitzParameter() - 1) - ((Integer) convertToBaseW.get(i)).intValue(), oTSHashAddress);
                i++;
            }
            return new WOTSPlusPublicKeyParameters(this.params, bArr2);
        } else {
            throw new NullPointerException("otsHashAddress == null");
        }
    }

    protected byte[] getPublicSeed() {
        return XMSSUtil.cloneArray(this.publicSeed);
    }

    protected byte[] getSecretKeySeed() {
        return XMSSUtil.cloneArray(getSecretKeySeed());
    }

    protected byte[] getWOTSPlusSecretKey(byte[] bArr, OTSHashAddress oTSHashAddress) {
        return this.khf.PRF(bArr, ((OTSHashAddress) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress.getLayerAddress())).withTreeAddress(oTSHashAddress.getTreeAddress())).withOTSAddress(oTSHashAddress.getOTSAddress()).build()).toByteArray());
    }

    void importKeys(byte[] bArr, byte[] bArr2) {
        if (bArr == null) {
            throw new NullPointerException("secretKeySeed == null");
        } else if (bArr.length != this.params.getDigestSize()) {
            throw new IllegalArgumentException("size of secretKeySeed needs to be equal to size of digest");
        } else if (bArr2 == null) {
            throw new NullPointerException("publicSeed == null");
        } else if (bArr2.length == this.params.getDigestSize()) {
            this.secretKeySeed = bArr;
            this.publicSeed = bArr2;
        } else {
            throw new IllegalArgumentException("size of publicSeed needs to be equal to size of digest");
        }
    }

    protected WOTSPlusSignature sign(byte[] bArr, OTSHashAddress oTSHashAddress) {
        if (bArr == null) {
            throw new NullPointerException("messageDigest == null");
        } else if (bArr.length != this.params.getDigestSize()) {
            throw new IllegalArgumentException("size of messageDigest needs to be equal to size of digest");
        } else if (oTSHashAddress != null) {
            List convertToBaseW = convertToBaseW(bArr, this.params.getWinternitzParameter(), this.params.getLen1());
            int i = 0;
            int i2 = i;
            while (i < this.params.getLen1()) {
                i2 += (this.params.getWinternitzParameter() - 1) - ((Integer) convertToBaseW.get(i)).intValue();
                i++;
            }
            convertToBaseW.addAll(convertToBaseW(XMSSUtil.toBytesBigEndian((long) (i2 << (8 - ((this.params.getLen2() * XMSSUtil.log2(this.params.getWinternitzParameter())) % 8))), (int) Math.ceil(((double) (this.params.getLen2() * XMSSUtil.log2(this.params.getWinternitzParameter()))) / 8.0d)), this.params.getWinternitzParameter(), this.params.getLen2()));
            byte[][] bArr2 = new byte[this.params.getLen()][];
            OTSHashAddress oTSHashAddress2 = oTSHashAddress;
            for (int i3 = 0; i3 < this.params.getLen(); i3++) {
                oTSHashAddress2 = (OTSHashAddress) ((Builder) ((Builder) ((Builder) new Builder().withLayerAddress(oTSHashAddress2.getLayerAddress())).withTreeAddress(oTSHashAddress2.getTreeAddress())).withOTSAddress(oTSHashAddress2.getOTSAddress()).withChainAddress(i3).withHashAddress(oTSHashAddress2.getHashAddress()).withKeyAndMask(oTSHashAddress2.getKeyAndMask())).build();
                bArr2[i3] = chain(expandSecretKeySeed(i3), 0, ((Integer) convertToBaseW.get(i3)).intValue(), oTSHashAddress2);
            }
            return new WOTSPlusSignature(this.params, bArr2);
        } else {
            throw new NullPointerException("otsHashAddress == null");
        }
    }

    protected boolean verifySignature(byte[] bArr, WOTSPlusSignature wOTSPlusSignature, OTSHashAddress oTSHashAddress) {
        if (bArr == null) {
            throw new NullPointerException("messageDigest == null");
        } else if (bArr.length != this.params.getDigestSize()) {
            throw new IllegalArgumentException("size of messageDigest needs to be equal to size of digest");
        } else if (wOTSPlusSignature == null) {
            throw new NullPointerException("signature == null");
        } else if (oTSHashAddress != null) {
            return XMSSUtil.areEqual(getPublicKeyFromSignature(bArr, wOTSPlusSignature, oTSHashAddress).toByteArray(), getPublicKey(oTSHashAddress).toByteArray());
        } else {
            throw new NullPointerException("otsHashAddress == null");
        }
    }
}
