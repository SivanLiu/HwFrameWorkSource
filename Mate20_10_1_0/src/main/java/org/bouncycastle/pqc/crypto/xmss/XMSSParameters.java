package org.bouncycastle.pqc.crypto.xmss;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.crypto.Digest;

public final class XMSSParameters {
    private final int height;
    private final int k;
    private final XMSSOid oid;
    private final String treeDigest;
    private final ASN1ObjectIdentifier treeDigestOID;
    private final int treeDigestSize;
    private final int winternitzParameter;
    private final WOTSPlusParameters wotsPlusParams;

    public XMSSParameters(int i, Digest digest) {
        if (i < 2) {
            throw new IllegalArgumentException("height must be >= 2");
        } else if (digest != null) {
            this.height = i;
            this.k = determineMinK();
            this.treeDigest = digest.getAlgorithmName();
            this.treeDigestOID = DigestUtil.getDigestOID(digest.getAlgorithmName());
            this.wotsPlusParams = new WOTSPlusParameters(this.treeDigestOID);
            this.treeDigestSize = this.wotsPlusParams.getTreeDigestSize();
            this.winternitzParameter = this.wotsPlusParams.getWinternitzParameter();
            this.oid = DefaultXMSSOid.lookup(this.treeDigest, this.treeDigestSize, this.winternitzParameter, this.wotsPlusParams.getLen(), i);
        } else {
            throw new NullPointerException("digest == null");
        }
    }

    private int determineMinK() {
        int i = 2;
        while (true) {
            int i2 = this.height;
            if (i > i2) {
                throw new IllegalStateException("should never happen...");
            } else if ((i2 - i) % 2 == 0) {
                return i;
            } else {
                i++;
            }
        }
    }

    public int getHeight() {
        return this.height;
    }

    /* access modifiers changed from: package-private */
    public int getK() {
        return this.k;
    }

    /* access modifiers changed from: package-private */
    public int getLen() {
        return this.wotsPlusParams.getLen();
    }

    /* access modifiers changed from: package-private */
    public XMSSOid getOid() {
        return this.oid;
    }

    /* access modifiers changed from: package-private */
    public String getTreeDigest() {
        return this.treeDigest;
    }

    /* access modifiers changed from: package-private */
    public ASN1ObjectIdentifier getTreeDigestOID() {
        return this.treeDigestOID;
    }

    public int getTreeDigestSize() {
        return this.treeDigestSize;
    }

    /* access modifiers changed from: package-private */
    public WOTSPlus getWOTSPlus() {
        return new WOTSPlus(this.wotsPlusParams);
    }

    /* access modifiers changed from: package-private */
    public int getWinternitzParameter() {
        return this.winternitzParameter;
    }
}
