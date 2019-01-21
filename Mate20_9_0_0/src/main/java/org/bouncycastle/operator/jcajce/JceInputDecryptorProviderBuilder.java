package org.bouncycastle.operator.jcajce;

import java.io.InputStream;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cryptopro.GOST28147Parameters;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jcajce.spec.GOST28147ParameterSpec;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.operator.InputDecryptor;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Arrays;

public class JceInputDecryptorProviderBuilder {
    private JcaJceHelper helper = new DefaultJcaJceHelper();

    public InputDecryptorProvider build(byte[] bArr) {
        bArr = Arrays.clone(bArr);
        return new InputDecryptorProvider() {
            private Cipher cipher;
            private AlgorithmIdentifier encryptionAlg;

            public InputDecryptor get(AlgorithmIdentifier algorithmIdentifier) throws OperatorCreationException {
                this.encryptionAlg = algorithmIdentifier;
                ASN1ObjectIdentifier algorithm = algorithmIdentifier.getAlgorithm();
                try {
                    Cipher cipher;
                    AlgorithmParameterSpec ivParameterSpec;
                    this.cipher = JceInputDecryptorProviderBuilder.this.helper.createCipher(algorithm.getId());
                    SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, algorithm.getId());
                    ASN1Encodable parameters = algorithmIdentifier.getParameters();
                    if (parameters instanceof ASN1OctetString) {
                        cipher = this.cipher;
                        ivParameterSpec = new IvParameterSpec(ASN1OctetString.getInstance(parameters).getOctets());
                    } else {
                        GOST28147Parameters instance = GOST28147Parameters.getInstance(parameters);
                        cipher = this.cipher;
                        ivParameterSpec = new GOST28147ParameterSpec(instance.getEncryptionParamSet(), instance.getIV());
                    }
                    cipher.init(2, secretKeySpec, ivParameterSpec);
                    return new InputDecryptor() {
                        public AlgorithmIdentifier getAlgorithmIdentifier() {
                            return AnonymousClass1.this.encryptionAlg;
                        }

                        public InputStream getInputStream(InputStream inputStream) {
                            return new CipherInputStream(inputStream, AnonymousClass1.this.cipher);
                        }
                    };
                } catch (Exception e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to create InputDecryptor: ");
                    stringBuilder.append(e.getMessage());
                    throw new OperatorCreationException(stringBuilder.toString(), e);
                }
            }
        };
    }

    public JceInputDecryptorProviderBuilder setProvider(String str) {
        this.helper = new NamedJcaJceHelper(str);
        return this;
    }

    public JceInputDecryptorProviderBuilder setProvider(Provider provider) {
        this.helper = new ProviderJcaJceHelper(provider);
        return this;
    }
}
