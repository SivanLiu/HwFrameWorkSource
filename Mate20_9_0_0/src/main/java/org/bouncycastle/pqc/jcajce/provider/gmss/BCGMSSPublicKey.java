package org.bouncycastle.pqc.jcajce.provider.gmss;

import java.security.PublicKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.pqc.asn1.GMSSPublicKey;
import org.bouncycastle.pqc.asn1.PQCObjectIdentifiers;
import org.bouncycastle.pqc.asn1.ParSet;
import org.bouncycastle.pqc.crypto.gmss.GMSSParameters;
import org.bouncycastle.pqc.crypto.gmss.GMSSPublicKeyParameters;
import org.bouncycastle.pqc.jcajce.provider.util.KeyUtil;
import org.bouncycastle.util.encoders.Hex;

public class BCGMSSPublicKey implements CipherParameters, PublicKey {
    private static final long serialVersionUID = 1;
    private GMSSParameters gmssParameterSet;
    private GMSSParameters gmssParams;
    private byte[] publicKeyBytes;

    public BCGMSSPublicKey(GMSSPublicKeyParameters gMSSPublicKeyParameters) {
        this(gMSSPublicKeyParameters.getPublicKey(), gMSSPublicKeyParameters.getParameters());
    }

    public BCGMSSPublicKey(byte[] bArr, GMSSParameters gMSSParameters) {
        this.gmssParameterSet = gMSSParameters;
        this.publicKeyBytes = bArr;
    }

    public String getAlgorithm() {
        return "GMSS";
    }

    public byte[] getEncoded() {
        return KeyUtil.getEncodedSubjectPublicKeyInfo(new AlgorithmIdentifier(PQCObjectIdentifiers.gmss, new ParSet(this.gmssParameterSet.getNumOfLayers(), this.gmssParameterSet.getHeightOfTrees(), this.gmssParameterSet.getWinternitzParameter(), this.gmssParameterSet.getK()).toASN1Primitive()), new GMSSPublicKey(this.publicKeyBytes));
    }

    public String getFormat() {
        return "X.509";
    }

    public GMSSParameters getParameterSet() {
        return this.gmssParameterSet;
    }

    public byte[] getPublicKeyBytes() {
        return this.publicKeyBytes;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("GMSS public key : ");
        stringBuilder.append(new String(Hex.encode(this.publicKeyBytes)));
        stringBuilder.append("\nHeight of Trees: \n");
        String stringBuilder2 = stringBuilder.toString();
        for (int i = 0; i < this.gmssParameterSet.getHeightOfTrees().length; i++) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(stringBuilder2);
            stringBuilder3.append("Layer ");
            stringBuilder3.append(i);
            stringBuilder3.append(" : ");
            stringBuilder3.append(this.gmssParameterSet.getHeightOfTrees()[i]);
            stringBuilder3.append(" WinternitzParameter: ");
            stringBuilder3.append(this.gmssParameterSet.getWinternitzParameter()[i]);
            stringBuilder3.append(" K: ");
            stringBuilder3.append(this.gmssParameterSet.getK()[i]);
            stringBuilder3.append("\n");
            stringBuilder2 = stringBuilder3.toString();
        }
        return stringBuilder2;
    }
}
