package org.bouncycastle.pkcs.bc;

import java.io.IOException;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;

public class BcPKCS10CertificationRequest extends PKCS10CertificationRequest {
    public BcPKCS10CertificationRequest(CertificationRequest certificationRequest) {
        super(certificationRequest);
    }

    public BcPKCS10CertificationRequest(PKCS10CertificationRequest pKCS10CertificationRequest) {
        super(pKCS10CertificationRequest.toASN1Structure());
    }

    public BcPKCS10CertificationRequest(byte[] bArr) throws IOException {
        super(bArr);
    }

    public AsymmetricKeyParameter getPublicKey() throws PKCSException {
        try {
            return PublicKeyFactory.createKey(getSubjectPublicKeyInfo());
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error extracting key encoding: ");
            stringBuilder.append(e.getMessage());
            throw new PKCSException(stringBuilder.toString(), e);
        }
    }
}
