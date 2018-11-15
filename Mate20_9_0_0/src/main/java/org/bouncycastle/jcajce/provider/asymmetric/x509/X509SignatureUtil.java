package org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PSSParameterSpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Null;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSASSAPSSparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jcajce.util.MessageDigestUtils;

class X509SignatureUtil {
    private static final ASN1Null derNull = DERNull.INSTANCE;

    X509SignatureUtil() {
    }

    private static String getDigestAlgName(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        String digestName = MessageDigestUtils.getDigestName(aSN1ObjectIdentifier);
        int indexOf = digestName.indexOf(45);
        if (indexOf <= 0 || digestName.startsWith("SHA3")) {
            return MessageDigestUtils.getDigestName(aSN1ObjectIdentifier);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(digestName.substring(0, indexOf));
        stringBuilder.append(digestName.substring(indexOf + 1));
        return stringBuilder.toString();
    }

    static String getSignatureName(AlgorithmIdentifier algorithmIdentifier) {
        ASN1Encodable parameters = algorithmIdentifier.getParameters();
        int i = 0;
        if (!(parameters == null || derNull.equals(parameters))) {
            StringBuilder stringBuilder;
            if (algorithmIdentifier.getAlgorithm().equals(PKCSObjectIdentifiers.id_RSASSA_PSS)) {
                RSASSAPSSparams instance = RSASSAPSSparams.getInstance(parameters);
                stringBuilder = new StringBuilder();
                stringBuilder.append(getDigestAlgName(instance.getHashAlgorithm().getAlgorithm()));
                stringBuilder.append("withRSAandMGF1");
                return stringBuilder.toString();
            } else if (algorithmIdentifier.getAlgorithm().equals(X9ObjectIdentifiers.ecdsa_with_SHA2)) {
                ASN1Sequence instance2 = ASN1Sequence.getInstance(parameters);
                stringBuilder = new StringBuilder();
                stringBuilder.append(getDigestAlgName((ASN1ObjectIdentifier) instance2.getObjectAt(0)));
                stringBuilder.append("withECDSA");
                return stringBuilder.toString();
            }
        }
        Provider provider = Security.getProvider("BC");
        if (provider != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.Signature.");
            stringBuilder2.append(algorithmIdentifier.getAlgorithm().getId());
            String property = provider.getProperty(stringBuilder2.toString());
            if (property != null) {
                return property;
            }
        }
        Provider[] providers = Security.getProviders();
        while (i != providers.length) {
            Provider provider2 = providers[i];
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.Signature.");
            stringBuilder3.append(algorithmIdentifier.getAlgorithm().getId());
            String property2 = provider2.getProperty(stringBuilder3.toString());
            if (property2 != null) {
                return property2;
            }
            i++;
        }
        return algorithmIdentifier.getAlgorithm().getId();
    }

    static void setSignatureParameters(Signature signature, ASN1Encodable aSN1Encodable) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        StringBuilder stringBuilder;
        if (!(aSN1Encodable == null || derNull.equals(aSN1Encodable))) {
            AlgorithmParameters instance = AlgorithmParameters.getInstance(signature.getAlgorithm(), signature.getProvider());
            try {
                instance.init(aSN1Encodable.toASN1Primitive().getEncoded());
                if (signature.getAlgorithm().endsWith("MGF1")) {
                    try {
                        signature.setParameter(instance.getParameterSpec(PSSParameterSpec.class));
                    } catch (GeneralSecurityException e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Exception extracting parameters: ");
                        stringBuilder.append(e.getMessage());
                        throw new SignatureException(stringBuilder.toString());
                    }
                }
            } catch (IOException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("IOException decoding parameters: ");
                stringBuilder.append(e2.getMessage());
                throw new SignatureException(stringBuilder.toString());
            }
        }
    }
}
