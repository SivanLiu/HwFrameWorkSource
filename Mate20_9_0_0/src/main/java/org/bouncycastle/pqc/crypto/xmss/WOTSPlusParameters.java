package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.crypto.Digest;

final class WOTSPlusParameters {
    private final Digest digest;
    private final int digestSize;
    private final int len;
    private final int len1;
    private final int len2;
    private final XMSSOid oid;
    private final int winternitzParameter;

    protected WOTSPlusParameters(Digest digest) {
        if (digest != null) {
            this.digest = digest;
            this.digestSize = XMSSUtil.getDigestSize(digest);
            this.winternitzParameter = 16;
            this.len1 = (int) Math.ceil(((double) (8 * this.digestSize)) / ((double) XMSSUtil.log2(this.winternitzParameter)));
            this.len2 = ((int) Math.floor((double) (XMSSUtil.log2(this.len1 * (this.winternitzParameter - 1)) / XMSSUtil.log2(this.winternitzParameter)))) + 1;
            this.len = this.len1 + this.len2;
            this.oid = WOTSPlusOid.lookup(digest.getAlgorithmName(), this.digestSize, this.winternitzParameter, this.len);
            if (this.oid == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cannot find OID for digest algorithm: ");
                stringBuilder.append(digest.getAlgorithmName());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            return;
        }
        throw new NullPointerException("digest == null");
    }

    protected Digest getDigest() {
        return this.digest;
    }

    protected int getDigestSize() {
        return this.digestSize;
    }

    protected int getLen() {
        return this.len;
    }

    protected int getLen1() {
        return this.len1;
    }

    protected int getLen2() {
        return this.len2;
    }

    protected XMSSOid getOid() {
        return this.oid;
    }

    protected int getWinternitzParameter() {
        return this.winternitzParameter;
    }
}
