package org.bouncycastle.operator.jcajce;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.PSource.PSpecified;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAESOAEPparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;

public class JcaAlgorithmParametersConverter {
    public AlgorithmIdentifier getAlgorithmIdentifier(ASN1ObjectIdentifier aSN1ObjectIdentifier, AlgorithmParameters algorithmParameters) throws InvalidAlgorithmParameterException {
        try {
            return new AlgorithmIdentifier(aSN1ObjectIdentifier, ASN1Primitive.fromByteArray(algorithmParameters.getEncoded()));
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unable to encode parameters object: ");
            stringBuilder.append(e.getMessage());
            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
        }
    }

    public AlgorithmIdentifier getAlgorithmIdentifier(ASN1ObjectIdentifier aSN1ObjectIdentifier, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
        if (!(algorithmParameterSpec instanceof OAEPParameterSpec)) {
            throw new InvalidAlgorithmParameterException("unknown parameter spec passed.");
        } else if (algorithmParameterSpec.equals(OAEPParameterSpec.DEFAULT)) {
            return new AlgorithmIdentifier(aSN1ObjectIdentifier, new RSAESOAEPparams(RSAESOAEPparams.DEFAULT_HASH_ALGORITHM, RSAESOAEPparams.DEFAULT_MASK_GEN_FUNCTION, RSAESOAEPparams.DEFAULT_P_SOURCE_ALGORITHM));
        } else {
            OAEPParameterSpec oAEPParameterSpec = (OAEPParameterSpec) algorithmParameterSpec;
            PSource pSource = oAEPParameterSpec.getPSource();
            if (oAEPParameterSpec.getMGFAlgorithm().equals(OAEPParameterSpec.DEFAULT.getMGFAlgorithm())) {
                return new AlgorithmIdentifier(aSN1ObjectIdentifier, new RSAESOAEPparams(new DefaultDigestAlgorithmIdentifierFinder().find(oAEPParameterSpec.getDigestAlgorithm()), new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, new DefaultDigestAlgorithmIdentifierFinder().find(((MGF1ParameterSpec) oAEPParameterSpec.getMGFParameters()).getDigestAlgorithm())), new AlgorithmIdentifier(PKCSObjectIdentifiers.id_pSpecified, new DEROctetString(((PSpecified) pSource).getValue()))));
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("only ");
            stringBuilder.append(OAEPParameterSpec.DEFAULT.getMGFAlgorithm());
            stringBuilder.append(" mask generator supported.");
            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
        }
    }
}
