package org.bouncycastle.pqc.jcajce.provider.xmss;

import java.io.IOException;
import java.security.PrivateKey;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.pqc.asn1.PQCObjectIdentifiers;
import org.bouncycastle.pqc.asn1.XMSSMTKeyParams;
import org.bouncycastle.pqc.asn1.XMSSMTPrivateKey;
import org.bouncycastle.pqc.asn1.XMSSPrivateKey;
import org.bouncycastle.pqc.crypto.xmss.BDSStateMap;
import org.bouncycastle.pqc.crypto.xmss.XMSSMTParameters;
import org.bouncycastle.pqc.crypto.xmss.XMSSMTPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.xmss.XMSSMTPrivateKeyParameters.Builder;
import org.bouncycastle.pqc.crypto.xmss.XMSSUtil;
import org.bouncycastle.pqc.jcajce.interfaces.XMSSMTKey;
import org.bouncycastle.util.Arrays;

public class BCXMSSMTPrivateKey implements PrivateKey, XMSSMTKey {
    private final XMSSMTPrivateKeyParameters keyParams;
    private final ASN1ObjectIdentifier treeDigest;

    public BCXMSSMTPrivateKey(ASN1ObjectIdentifier aSN1ObjectIdentifier, XMSSMTPrivateKeyParameters xMSSMTPrivateKeyParameters) {
        this.treeDigest = aSN1ObjectIdentifier;
        this.keyParams = xMSSMTPrivateKeyParameters;
    }

    public BCXMSSMTPrivateKey(PrivateKeyInfo privateKeyInfo) throws IOException {
        XMSSMTKeyParams instance = XMSSMTKeyParams.getInstance(privateKeyInfo.getPrivateKeyAlgorithm().getParameters());
        this.treeDigest = instance.getTreeDigest().getAlgorithm();
        XMSSPrivateKey instance2 = XMSSPrivateKey.getInstance(privateKeyInfo.parsePrivateKey());
        try {
            Builder withRoot = new Builder(new XMSSMTParameters(instance.getHeight(), instance.getLayers(), DigestUtil.getDigest(this.treeDigest))).withIndex((long) instance2.getIndex()).withSecretKeySeed(instance2.getSecretKeySeed()).withSecretKeyPRF(instance2.getSecretKeyPRF()).withPublicSeed(instance2.getPublicSeed()).withRoot(instance2.getRoot());
            if (instance2.getBdsState() != null) {
                withRoot.withBDSState((BDSStateMap) XMSSUtil.deserialize(instance2.getBdsState()));
            }
            this.keyParams = withRoot.build();
        } catch (ClassNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ClassNotFoundException processing BDS state: ");
            stringBuilder.append(e.getMessage());
            throw new IOException(stringBuilder.toString());
        }
    }

    private XMSSMTPrivateKey createKeyStructure() {
        byte[] toByteArray = this.keyParams.toByteArray();
        int digestSize = this.keyParams.getParameters().getDigestSize();
        int height = this.keyParams.getParameters().getHeight();
        int i = (height + 7) / 8;
        int bytesToXBigEndian = (int) XMSSUtil.bytesToXBigEndian(toByteArray, 0, i);
        if (XMSSUtil.isIndexValid(height, (long) bytesToXBigEndian)) {
            int i2 = 0 + i;
            byte[] extractBytesAtOffset = XMSSUtil.extractBytesAtOffset(toByteArray, i2, digestSize);
            i2 += digestSize;
            byte[] extractBytesAtOffset2 = XMSSUtil.extractBytesAtOffset(toByteArray, i2, digestSize);
            i2 += digestSize;
            byte[] extractBytesAtOffset3 = XMSSUtil.extractBytesAtOffset(toByteArray, i2, digestSize);
            i2 += digestSize;
            byte[] extractBytesAtOffset4 = XMSSUtil.extractBytesAtOffset(toByteArray, i2, digestSize);
            i2 += digestSize;
            return new XMSSMTPrivateKey(bytesToXBigEndian, extractBytesAtOffset, extractBytesAtOffset2, extractBytesAtOffset3, extractBytesAtOffset4, XMSSUtil.extractBytesAtOffset(toByteArray, i2, toByteArray.length - i2));
        }
        throw new IllegalArgumentException("index out of bounds");
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BCXMSSMTPrivateKey)) {
            return false;
        }
        BCXMSSMTPrivateKey bCXMSSMTPrivateKey = (BCXMSSMTPrivateKey) obj;
        return this.treeDigest.equals(bCXMSSMTPrivateKey.treeDigest) && Arrays.areEqual(this.keyParams.toByteArray(), bCXMSSMTPrivateKey.keyParams.toByteArray());
    }

    public String getAlgorithm() {
        return "XMSSMT";
    }

    public byte[] getEncoded() {
        try {
            return new PrivateKeyInfo(new AlgorithmIdentifier(PQCObjectIdentifiers.xmss_mt, new XMSSMTKeyParams(this.keyParams.getParameters().getHeight(), this.keyParams.getParameters().getLayers(), new AlgorithmIdentifier(this.treeDigest))), createKeyStructure()).getEncoded();
        } catch (IOException e) {
            return null;
        }
    }

    public String getFormat() {
        return "PKCS#8";
    }

    public int getHeight() {
        return this.keyParams.getParameters().getHeight();
    }

    CipherParameters getKeyParams() {
        return this.keyParams;
    }

    public int getLayers() {
        return this.keyParams.getParameters().getLayers();
    }

    public String getTreeDigest() {
        return DigestUtil.getXMSSDigestName(this.treeDigest);
    }

    ASN1ObjectIdentifier getTreeDigestOID() {
        return this.treeDigest;
    }

    public int hashCode() {
        return this.treeDigest.hashCode() + (37 * Arrays.hashCode(this.keyParams.toByteArray()));
    }
}
