package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.crypto.Digest;

public final class XMSSMTParameters {
    private final int height;
    private final int layers;
    private final XMSSOid oid;
    private final XMSSParameters xmssParams;

    public XMSSMTParameters(int i, int i2, Digest digest) {
        this.height = i;
        this.layers = i2;
        this.xmssParams = new XMSSParameters(xmssTreeHeight(i, i2), digest);
        this.oid = DefaultXMSSMTOid.lookup(getDigest().getAlgorithmName(), getDigestSize(), getWinternitzParameter(), getLen(), getHeight(), i2);
    }

    private static int xmssTreeHeight(int i, int i2) throws IllegalArgumentException {
        if (i < 2) {
            throw new IllegalArgumentException("totalHeight must be > 1");
        } else if (i % i2 == 0) {
            i /= i2;
            if (i != 1) {
                return i;
            }
            throw new IllegalArgumentException("height / layers must be greater than 1");
        } else {
            throw new IllegalArgumentException("layers must divide totalHeight without remainder");
        }
    }

    protected Digest getDigest() {
        return this.xmssParams.getDigest();
    }

    public int getDigestSize() {
        return this.xmssParams.getDigestSize();
    }

    public int getHeight() {
        return this.height;
    }

    public int getLayers() {
        return this.layers;
    }

    protected int getLen() {
        return this.xmssParams.getWOTSPlus().getParams().getLen();
    }

    protected WOTSPlus getWOTSPlus() {
        return this.xmssParams.getWOTSPlus();
    }

    public int getWinternitzParameter() {
        return this.xmssParams.getWinternitzParameter();
    }

    protected XMSSParameters getXMSSParameters() {
        return this.xmssParams;
    }
}
