package com.android.org.bouncycastle.jcajce.provider.asymmetric.x509;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Null;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.RSASSAPSSparams;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.android.org.bouncycastle.jcajce.util.MessageDigestUtils;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
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

class X509SignatureUtil {
    private static final ASN1Null derNull = DERNull.INSTANCE;

    X509SignatureUtil() {
    }

    static void setSignatureParameters(Signature signature, ASN1Encodable params) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        StringBuilder stringBuilder;
        if (params != null && !derNull.equals(params)) {
            AlgorithmParameters sigParams = AlgorithmParameters.getInstance(signature.getAlgorithm(), signature.getProvider());
            try {
                sigParams.init(params.toASN1Primitive().getEncoded());
                if (signature.getAlgorithm().endsWith("MGF1")) {
                    try {
                        signature.setParameter(sigParams.getParameterSpec(PSSParameterSpec.class));
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

    static String getSignatureName(AlgorithmIdentifier sigAlgId) {
        StringBuilder stringBuilder;
        ASN1Encodable params = sigAlgId.getParameters();
        int i = 0;
        if (!(params == null || derNull.equals(params))) {
            if (sigAlgId.getAlgorithm().equals(PKCSObjectIdentifiers.id_RSASSA_PSS)) {
                RSASSAPSSparams rsaParams = RSASSAPSSparams.getInstance(params);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(getDigestAlgName(rsaParams.getHashAlgorithm().getAlgorithm()));
                stringBuilder2.append("withRSAandMGF1");
                return stringBuilder2.toString();
            } else if (sigAlgId.getAlgorithm().equals(X9ObjectIdentifiers.ecdsa_with_SHA2)) {
                ASN1Sequence ecDsaParams = ASN1Sequence.getInstance(params);
                stringBuilder = new StringBuilder();
                stringBuilder.append(getDigestAlgName((ASN1ObjectIdentifier) ecDsaParams.getObjectAt(0)));
                stringBuilder.append("withECDSA");
                return stringBuilder.toString();
            }
        }
        Provider prov = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (prov != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(sigAlgId.getAlgorithm().getId());
            String algName = prov.getProperty(stringBuilder.toString());
            if (algName != null) {
                return algName;
            }
        }
        Provider[] provs = Security.getProviders();
        while (i != provs.length) {
            String algName2 = provs[i];
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.Signature.");
            stringBuilder3.append(sigAlgId.getAlgorithm().getId());
            algName2 = algName2.getProperty(stringBuilder3.toString());
            if (algName2 != null) {
                return algName2;
            }
            i++;
        }
        return sigAlgId.getAlgorithm().getId();
    }

    private static String getDigestAlgName(ASN1ObjectIdentifier digestAlgOID) {
        String name = MessageDigestUtils.getDigestName(digestAlgOID);
        int dIndex = name.indexOf(45);
        if (dIndex <= 0 || name.startsWith("SHA3")) {
            return MessageDigestUtils.getDigestName(digestAlgOID);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name.substring(0, dIndex));
        stringBuilder.append(name.substring(dIndex + 1));
        return stringBuilder.toString();
    }
}
