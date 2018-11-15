package org.bouncycastle.cert.crmf.jcajce;

import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.Provider;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.crmf.CRMFException;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.operator.DefaultSecretKeySizeProvider;
import org.bouncycastle.operator.GenericKey;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.SecretKeySizeProvider;
import org.bouncycastle.operator.jcajce.JceGenericKey;

public class JceCRMFEncryptorBuilder {
    private static final SecretKeySizeProvider KEY_SIZE_PROVIDER = DefaultSecretKeySizeProvider.INSTANCE;
    private final ASN1ObjectIdentifier encryptionOID;
    private CRMFHelper helper;
    private final int keySize;
    private SecureRandom random;

    private class CRMFOutputEncryptor implements OutputEncryptor {
        private AlgorithmIdentifier algorithmIdentifier;
        private Cipher cipher;
        private SecretKey encKey;

        CRMFOutputEncryptor(ASN1ObjectIdentifier aSN1ObjectIdentifier, int i, SecureRandom secureRandom) throws CRMFException {
            KeyGenerator createKeyGenerator = JceCRMFEncryptorBuilder.this.helper.createKeyGenerator(aSN1ObjectIdentifier);
            if (secureRandom == null) {
                secureRandom = new SecureRandom();
            }
            if (i < 0) {
                i = JceCRMFEncryptorBuilder.KEY_SIZE_PROVIDER.getKeySize(aSN1ObjectIdentifier);
            }
            if (i < 0) {
                createKeyGenerator.init(secureRandom);
            } else {
                createKeyGenerator.init(i, secureRandom);
            }
            this.cipher = JceCRMFEncryptorBuilder.this.helper.createCipher(aSN1ObjectIdentifier);
            this.encKey = createKeyGenerator.generateKey();
            AlgorithmParameters generateParameters = JceCRMFEncryptorBuilder.this.helper.generateParameters(aSN1ObjectIdentifier, this.encKey, secureRandom);
            try {
                this.cipher.init(1, this.encKey, generateParameters, secureRandom);
                if (generateParameters == null) {
                    generateParameters = this.cipher.getParameters();
                }
                this.algorithmIdentifier = JceCRMFEncryptorBuilder.this.helper.getAlgorithmIdentifier(aSN1ObjectIdentifier, generateParameters);
            } catch (Throwable e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to initialize cipher: ");
                stringBuilder.append(e.getMessage());
                throw new CRMFException(stringBuilder.toString(), e);
            }
        }

        public AlgorithmIdentifier getAlgorithmIdentifier() {
            return this.algorithmIdentifier;
        }

        public GenericKey getKey() {
            return new JceGenericKey(this.algorithmIdentifier, this.encKey);
        }

        public OutputStream getOutputStream(OutputStream outputStream) {
            return new CipherOutputStream(outputStream, this.cipher);
        }
    }

    public JceCRMFEncryptorBuilder(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        this(aSN1ObjectIdentifier, -1);
    }

    public JceCRMFEncryptorBuilder(ASN1ObjectIdentifier aSN1ObjectIdentifier, int i) {
        this.helper = new CRMFHelper(new DefaultJcaJceHelper());
        this.encryptionOID = aSN1ObjectIdentifier;
        this.keySize = i;
    }

    public OutputEncryptor build() throws CRMFException {
        return new CRMFOutputEncryptor(this.encryptionOID, this.keySize, this.random);
    }

    public JceCRMFEncryptorBuilder setProvider(String str) {
        this.helper = new CRMFHelper(new NamedJcaJceHelper(str));
        return this;
    }

    public JceCRMFEncryptorBuilder setProvider(Provider provider) {
        this.helper = new CRMFHelper(new ProviderJcaJceHelper(provider));
        return this;
    }

    public JceCRMFEncryptorBuilder setSecureRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
        return this;
    }
}
