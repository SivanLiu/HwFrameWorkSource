package org.bouncycastle.pqc.jcajce.provider.xmss;

import java.io.IOException;
import java.security.PrivateKey;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.pqc.asn1.PQCObjectIdentifiers;
import org.bouncycastle.pqc.asn1.XMSSKeyParams;
import org.bouncycastle.pqc.asn1.XMSSPrivateKey;
import org.bouncycastle.pqc.crypto.xmss.BDS;
import org.bouncycastle.pqc.crypto.xmss.XMSSParameters;
import org.bouncycastle.pqc.crypto.xmss.XMSSPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.xmss.XMSSPrivateKeyParameters.Builder;
import org.bouncycastle.pqc.crypto.xmss.XMSSUtil;
import org.bouncycastle.pqc.jcajce.interfaces.XMSSKey;
import org.bouncycastle.util.Arrays;

public class BCXMSSPrivateKey implements PrivateKey, XMSSKey {
    private final XMSSPrivateKeyParameters keyParams;
    private final ASN1ObjectIdentifier treeDigest;

    public BCXMSSPrivateKey(ASN1ObjectIdentifier aSN1ObjectIdentifier, XMSSPrivateKeyParameters xMSSPrivateKeyParameters) {
        this.treeDigest = aSN1ObjectIdentifier;
        this.keyParams = xMSSPrivateKeyParameters;
    }

    public BCXMSSPrivateKey(PrivateKeyInfo privateKeyInfo) throws IOException {
        XMSSKeyParams instance = XMSSKeyParams.getInstance(privateKeyInfo.getPrivateKeyAlgorithm().getParameters());
        this.treeDigest = instance.getTreeDigest().getAlgorithm();
        XMSSPrivateKey instance2 = XMSSPrivateKey.getInstance(privateKeyInfo.parsePrivateKey());
        try {
            Builder withRoot = new Builder(new XMSSParameters(instance.getHeight(), DigestUtil.getDigest(this.treeDigest))).withIndex(instance2.getIndex()).withSecretKeySeed(instance2.getSecretKeySeed()).withSecretKeyPRF(instance2.getSecretKeyPRF()).withPublicSeed(instance2.getPublicSeed()).withRoot(instance2.getRoot());
            if (instance2.getBdsState() != null) {
                withRoot.withBDSState((BDS) XMSSUtil.deserialize(instance2.getBdsState()));
            }
            this.keyParams = withRoot.build();
        } catch (ClassNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ClassNotFoundException processing BDS state: ");
            stringBuilder.append(e.getMessage());
            throw new IOException(stringBuilder.toString());
        }
    }

    private XMSSPrivateKey createKeyStructure() {
        byte[] toByteArray = this.keyParams.toByteArray();
        int digestSize = this.keyParams.getParameters().getDigestSize();
        int bytesToXBigEndian = (int) XMSSUtil.bytesToXBigEndian(toByteArray, 0, 4);
        if (XMSSUtil.isIndexValid(this.keyParams.getParameters().getHeight(), (long) bytesToXBigEndian)) {
            byte[] extractBytesAtOffset = XMSSUtil.extractBytesAtOffset(toByteArray, 4, digestSize);
            int i = 4 + digestSize;
            byte[] extractBytesAtOffset2 = XMSSUtil.extractBytesAtOffset(toByteArray, i, digestSize);
            i += digestSize;
            byte[] extractBytesAtOffset3 = XMSSUtil.extractBytesAtOffset(toByteArray, i, digestSize);
            i += digestSize;
            byte[] extractBytesAtOffset4 = XMSSUtil.extractBytesAtOffset(toByteArray, i, digestSize);
            i += digestSize;
            return new XMSSPrivateKey(bytesToXBigEndian, extractBytesAtOffset, extractBytesAtOffset2, extractBytesAtOffset3, extractBytesAtOffset4, XMSSUtil.extractBytesAtOffset(toByteArray, i, toByteArray.length - i));
        }
        throw new IllegalArgumentException("index out of bounds");
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BCXMSSPrivateKey)) {
            return false;
        }
        BCXMSSPrivateKey bCXMSSPrivateKey = (BCXMSSPrivateKey) obj;
        return this.treeDigest.equals(bCXMSSPrivateKey.treeDigest) && Arrays.areEqual(this.keyParams.toByteArray(), bCXMSSPrivateKey.keyParams.toByteArray());
    }

    public String getAlgorithm() {
        return "XMSS";
    }

    public byte[] getEncoded() {
        try {
            return new PrivateKeyInfo(new AlgorithmIdentifier(PQCObjectIdentifiers.xmss, new XMSSKeyParams(this.keyParams.getParameters().getHeight(), new AlgorithmIdentifier(this.treeDigest))), createKeyStructure()).getEncoded();
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
