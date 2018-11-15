package org.bouncycastle.cert.crmf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.crmf.EncryptedValue;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.KeyWrapper;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfoBuilder;
import org.bouncycastle.util.Strings;

public class EncryptedValueBuilder {
    private OutputEncryptor encryptor;
    private EncryptedValuePadder padder;
    private KeyWrapper wrapper;

    public EncryptedValueBuilder(KeyWrapper keyWrapper, OutputEncryptor outputEncryptor) {
        this(keyWrapper, outputEncryptor, null);
    }

    public EncryptedValueBuilder(KeyWrapper keyWrapper, OutputEncryptor outputEncryptor, EncryptedValuePadder encryptedValuePadder) {
        this.wrapper = keyWrapper;
        this.encryptor = outputEncryptor;
        this.padder = encryptedValuePadder;
    }

    private EncryptedValue encryptData(byte[] bArr) throws CRMFException {
        StringBuilder stringBuilder;
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = this.encryptor.getOutputStream(byteArrayOutputStream);
        try {
            outputStream.write(bArr);
            outputStream.close();
            AlgorithmIdentifier algorithmIdentifier = this.encryptor.getAlgorithmIdentifier();
            try {
                this.wrapper.generateWrappedKey(this.encryptor.getKey());
                return new EncryptedValue(null, algorithmIdentifier, new DERBitString(this.wrapper.generateWrappedKey(this.encryptor.getKey())), this.wrapper.getAlgorithmIdentifier(), null, new DERBitString(byteArrayOutputStream.toByteArray()));
            } catch (Throwable e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("cannot wrap key: ");
                stringBuilder.append(e.getMessage());
                throw new CRMFException(stringBuilder.toString(), e);
            }
        } catch (Throwable e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("cannot process data: ");
            stringBuilder.append(e2.getMessage());
            throw new CRMFException(stringBuilder.toString(), e2);
        }
    }

    private byte[] padData(byte[] bArr) {
        return this.padder != null ? this.padder.getPaddedData(bArr) : bArr;
    }

    public EncryptedValue build(PrivateKeyInfo privateKeyInfo) throws CRMFException {
        StringBuilder stringBuilder;
        PKCS8EncryptedPrivateKeyInfoBuilder pKCS8EncryptedPrivateKeyInfoBuilder = new PKCS8EncryptedPrivateKeyInfoBuilder(privateKeyInfo);
        AlgorithmIdentifier privateKeyAlgorithm = privateKeyInfo.getPrivateKeyAlgorithm();
        AlgorithmIdentifier algorithmIdentifier = this.encryptor.getAlgorithmIdentifier();
        try {
            PKCS8EncryptedPrivateKeyInfo build = pKCS8EncryptedPrivateKeyInfoBuilder.build(this.encryptor);
            this.wrapper.generateWrappedKey(this.encryptor.getKey());
            return new EncryptedValue(privateKeyAlgorithm, algorithmIdentifier, new DERBitString(this.wrapper.generateWrappedKey(this.encryptor.getKey())), this.wrapper.getAlgorithmIdentifier(), null, new DERBitString(build.getEncoded()));
        } catch (Throwable e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("cannot encode encrypted private key: ");
            stringBuilder.append(e.getMessage());
            throw new CRMFException(stringBuilder.toString(), e);
        } catch (Throwable e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("cannot encode key: ");
            stringBuilder.append(e2.getMessage());
            throw new CRMFException(stringBuilder.toString(), e2);
        } catch (Throwable e22) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("cannot wrap key: ");
            stringBuilder.append(e22.getMessage());
            throw new CRMFException(stringBuilder.toString(), e22);
        }
    }

    public EncryptedValue build(X509CertificateHolder x509CertificateHolder) throws CRMFException {
        try {
            return encryptData(padData(x509CertificateHolder.getEncoded()));
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cannot encode certificate: ");
            stringBuilder.append(e.getMessage());
            throw new CRMFException(stringBuilder.toString(), e);
        }
    }

    public EncryptedValue build(char[] cArr) throws CRMFException {
        return encryptData(padData(Strings.toUTF8ByteArray(cArr)));
    }
}
