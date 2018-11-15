package org.bouncycastle.pkcs.jcajce;

import java.io.InputStream;
import java.security.Key;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cryptopro.GOST28147Parameters;
import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.ScryptParams;
import org.bouncycastle.asn1.pkcs.PBEParameter;
import org.bouncycastle.asn1.pkcs.PBES2Parameters;
import org.bouncycastle.asn1.pkcs.PBKDF2Params;
import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.PasswordConverter;
import org.bouncycastle.jcajce.PBKDF1Key;
import org.bouncycastle.jcajce.PKCS12KeyWithParameters;
import org.bouncycastle.jcajce.spec.GOST28147ParameterSpec;
import org.bouncycastle.jcajce.spec.PBKDF2KeySpec;
import org.bouncycastle.jcajce.spec.ScryptKeySpec;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.operator.DefaultSecretKeySizeProvider;
import org.bouncycastle.operator.InputDecryptor;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.SecretKeySizeProvider;

public class JcePKCSPBEInputDecryptorProviderBuilder {
    private JcaJceHelper helper = new DefaultJcaJceHelper();
    private SecretKeySizeProvider keySizeProvider = DefaultSecretKeySizeProvider.INSTANCE;
    private boolean wrongPKCS12Zero = false;

    public InputDecryptorProvider build(final char[] cArr) {
        return new InputDecryptorProvider() {
            private Cipher cipher;
            private AlgorithmIdentifier encryptionAlg;

            public InputDecryptor get(AlgorithmIdentifier algorithmIdentifier) throws OperatorCreationException {
                ASN1ObjectIdentifier algorithm = algorithmIdentifier.getAlgorithm();
                StringBuilder stringBuilder;
                try {
                    if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
                        PKCS12PBEParams instance = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());
                        this.cipher = JcePKCSPBEInputDecryptorProviderBuilder.this.helper.createCipher(algorithm.getId());
                        this.cipher.init(2, new PKCS12KeyWithParameters(cArr, JcePKCSPBEInputDecryptorProviderBuilder.this.wrongPKCS12Zero, instance.getIV(), instance.getIterations().intValue()));
                        this.encryptionAlg = algorithmIdentifier;
                    } else if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
                        Key generateSecret;
                        Cipher cipher;
                        AlgorithmParameterSpec ivParameterSpec;
                        PBES2Parameters instance2 = PBES2Parameters.getInstance(algorithmIdentifier.getParameters());
                        if (MiscObjectIdentifiers.id_scrypt.equals(instance2.getKeyDerivationFunc().getAlgorithm())) {
                            ScryptParams instance3 = ScryptParams.getInstance(instance2.getKeyDerivationFunc().getParameters());
                            generateSecret = JcePKCSPBEInputDecryptorProviderBuilder.this.helper.createSecretKeyFactory("SCRYPT").generateSecret(new ScryptKeySpec(cArr, instance3.getSalt(), instance3.getCostParameter().intValue(), instance3.getBlockSize().intValue(), instance3.getParallelizationParameter().intValue(), JcePKCSPBEInputDecryptorProviderBuilder.this.keySizeProvider.getKeySize(AlgorithmIdentifier.getInstance(instance2.getEncryptionScheme()))));
                        } else {
                            SecretKeyFactory createSecretKeyFactory = JcePKCSPBEInputDecryptorProviderBuilder.this.helper.createSecretKeyFactory(instance2.getKeyDerivationFunc().getAlgorithm().getId());
                            PBKDF2Params instance4 = PBKDF2Params.getInstance(instance2.getKeyDerivationFunc().getParameters());
                            AlgorithmIdentifier instance5 = AlgorithmIdentifier.getInstance(instance2.getEncryptionScheme());
                            generateSecret = instance4.isDefaultPrf() ? createSecretKeyFactory.generateSecret(new PBEKeySpec(cArr, instance4.getSalt(), instance4.getIterationCount().intValue(), JcePKCSPBEInputDecryptorProviderBuilder.this.keySizeProvider.getKeySize(instance5))) : createSecretKeyFactory.generateSecret(new PBKDF2KeySpec(cArr, instance4.getSalt(), instance4.getIterationCount().intValue(), JcePKCSPBEInputDecryptorProviderBuilder.this.keySizeProvider.getKeySize(instance5), instance4.getPrf()));
                        }
                        this.cipher = JcePKCSPBEInputDecryptorProviderBuilder.this.helper.createCipher(instance2.getEncryptionScheme().getAlgorithm().getId());
                        this.encryptionAlg = AlgorithmIdentifier.getInstance(instance2.getEncryptionScheme());
                        ASN1Encodable parameters = instance2.getEncryptionScheme().getParameters();
                        if (parameters instanceof ASN1OctetString) {
                            cipher = this.cipher;
                            ivParameterSpec = new IvParameterSpec(ASN1OctetString.getInstance(parameters).getOctets());
                        } else {
                            GOST28147Parameters instance6 = GOST28147Parameters.getInstance(parameters);
                            cipher = this.cipher;
                            ivParameterSpec = new GOST28147ParameterSpec(instance6.getEncryptionParamSet(), instance6.getIV());
                        }
                        cipher.init(2, generateSecret, ivParameterSpec);
                    } else if (algorithm.equals(PKCSObjectIdentifiers.pbeWithMD5AndDES_CBC) || algorithm.equals(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC)) {
                        PBEParameter instance7 = PBEParameter.getInstance(algorithmIdentifier.getParameters());
                        this.cipher = JcePKCSPBEInputDecryptorProviderBuilder.this.helper.createCipher(algorithm.getId());
                        this.cipher.init(2, new PBKDF1Key(cArr, PasswordConverter.ASCII), new PBEParameterSpec(instance7.getSalt(), instance7.getIterationCount().intValue()));
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("unable to create InputDecryptor: algorithm ");
                        stringBuilder.append(algorithm);
                        stringBuilder.append(" unknown.");
                        throw new OperatorCreationException(stringBuilder.toString());
                    }
                    return new InputDecryptor() {
                        public AlgorithmIdentifier getAlgorithmIdentifier() {
                            return AnonymousClass1.this.encryptionAlg;
                        }

                        public InputStream getInputStream(InputStream inputStream) {
                            return new CipherInputStream(inputStream, AnonymousClass1.this.cipher);
                        }
                    };
                } catch (Throwable e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to create InputDecryptor: ");
                    stringBuilder.append(e.getMessage());
                    throw new OperatorCreationException(stringBuilder.toString(), e);
                }
            }
        };
    }

    public JcePKCSPBEInputDecryptorProviderBuilder setKeySizeProvider(SecretKeySizeProvider secretKeySizeProvider) {
        this.keySizeProvider = secretKeySizeProvider;
        return this;
    }

    public JcePKCSPBEInputDecryptorProviderBuilder setProvider(String str) {
        this.helper = new NamedJcaJceHelper(str);
        return this;
    }

    public JcePKCSPBEInputDecryptorProviderBuilder setProvider(Provider provider) {
        this.helper = new ProviderJcaJceHelper(provider);
        return this;
    }

    public JcePKCSPBEInputDecryptorProviderBuilder setTryWrongPKCS12Zero(boolean z) {
        this.wrongPKCS12Zero = z;
        return this;
    }
}
