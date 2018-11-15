package org.bouncycastle.openssl.jcajce;

import java.io.InputStream;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.Provider;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PBEParameter;
import org.bouncycastle.asn1.pkcs.PBES2Parameters;
import org.bouncycastle.asn1.pkcs.PBKDF2Params;
import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CharToByteConverter;
import org.bouncycastle.jcajce.PBKDF1KeyWithParameters;
import org.bouncycastle.jcajce.PKCS12KeyWithParameters;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.operator.InputDecryptor;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Strings;

public class JceOpenSSLPKCS8DecryptorProviderBuilder {
    private JcaJceHelper helper;

    public JceOpenSSLPKCS8DecryptorProviderBuilder() {
        this.helper = new DefaultJcaJceHelper();
        this.helper = new DefaultJcaJceHelper();
    }

    public InputDecryptorProvider build(final char[] cArr) throws OperatorCreationException {
        return new InputDecryptorProvider() {
            public InputDecryptor get(final AlgorithmIdentifier algorithmIdentifier) throws OperatorCreationException {
                StringBuilder stringBuilder;
                try {
                    Cipher createCipher;
                    Key generateSecretKeyForPKCS5Scheme2;
                    if (PEMUtilities.isPKCS5Scheme2(algorithmIdentifier.getAlgorithm())) {
                        PBES2Parameters instance = PBES2Parameters.getInstance(algorithmIdentifier.getParameters());
                        KeyDerivationFunc keyDerivationFunc = instance.getKeyDerivationFunc();
                        EncryptionScheme encryptionScheme = instance.getEncryptionScheme();
                        PBKDF2Params pBKDF2Params = (PBKDF2Params) keyDerivationFunc.getParameters();
                        int intValue = pBKDF2Params.getIterationCount().intValue();
                        byte[] salt = pBKDF2Params.getSalt();
                        String id = encryptionScheme.getAlgorithm().getId();
                        if (PEMUtilities.isHmacSHA1(pBKDF2Params.getPrf())) {
                            generateSecretKeyForPKCS5Scheme2 = PEMUtilities.generateSecretKeyForPKCS5Scheme2(JceOpenSSLPKCS8DecryptorProviderBuilder.this.helper, id, cArr, salt, intValue);
                        } else {
                            generateSecretKeyForPKCS5Scheme2 = PEMUtilities.generateSecretKeyForPKCS5Scheme2(JceOpenSSLPKCS8DecryptorProviderBuilder.this.helper, id, cArr, salt, intValue, pBKDF2Params.getPrf());
                        }
                        createCipher = JceOpenSSLPKCS8DecryptorProviderBuilder.this.helper.createCipher(id);
                        AlgorithmParameters createAlgorithmParameters = JceOpenSSLPKCS8DecryptorProviderBuilder.this.helper.createAlgorithmParameters(id);
                        createAlgorithmParameters.init(encryptionScheme.getParameters().toASN1Primitive().getEncoded());
                        createCipher.init(2, generateSecretKeyForPKCS5Scheme2, createAlgorithmParameters);
                    } else {
                        if (PEMUtilities.isPKCS12(algorithmIdentifier.getAlgorithm())) {
                            PKCS12PBEParams instance2 = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());
                            createCipher = JceOpenSSLPKCS8DecryptorProviderBuilder.this.helper.createCipher(algorithmIdentifier.getAlgorithm().getId());
                            generateSecretKeyForPKCS5Scheme2 = new PKCS12KeyWithParameters(cArr, instance2.getIV(), instance2.getIterations().intValue());
                        } else if (PEMUtilities.isPKCS5Scheme1(algorithmIdentifier.getAlgorithm())) {
                            PBEParameter instance3 = PBEParameter.getInstance(algorithmIdentifier.getParameters());
                            createCipher = JceOpenSSLPKCS8DecryptorProviderBuilder.this.helper.createCipher(algorithmIdentifier.getAlgorithm().getId());
                            generateSecretKeyForPKCS5Scheme2 = new PBKDF1KeyWithParameters(cArr, new CharToByteConverter() {
                                public byte[] convert(char[] cArr) {
                                    return Strings.toByteArray(cArr);
                                }

                                public String getType() {
                                    return "ASCII";
                                }
                            }, instance3.getSalt(), instance3.getIterationCount().intValue());
                        } else {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Unknown algorithm: ");
                            stringBuilder2.append(algorithmIdentifier.getAlgorithm());
                            throw new PEMException(stringBuilder2.toString());
                        }
                        createCipher.init(2, generateSecretKeyForPKCS5Scheme2);
                    }
                    return new InputDecryptor() {
                        public AlgorithmIdentifier getAlgorithmIdentifier() {
                            return algorithmIdentifier;
                        }

                        public InputStream getInputStream(InputStream inputStream) {
                            return new CipherInputStream(inputStream, createCipher);
                        }
                    };
                } catch (Throwable e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(algorithmIdentifier.getAlgorithm());
                    stringBuilder.append(" not available: ");
                    stringBuilder.append(e.getMessage());
                    throw new OperatorCreationException(stringBuilder.toString(), e);
                } catch (Throwable e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(algorithmIdentifier.getAlgorithm());
                    stringBuilder.append(" not available: ");
                    stringBuilder.append(e2.getMessage());
                    throw new OperatorCreationException(stringBuilder.toString(), e2);
                }
            }
        };
    }

    public JceOpenSSLPKCS8DecryptorProviderBuilder setProvider(String str) {
        this.helper = new NamedJcaJceHelper(str);
        return this;
    }

    public JceOpenSSLPKCS8DecryptorProviderBuilder setProvider(Provider provider) {
        this.helper = new ProviderJcaJceHelper(provider);
        return this;
    }
}
