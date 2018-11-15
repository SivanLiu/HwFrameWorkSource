package org.bouncycastle.eac;

import java.io.IOException;
import java.io.OutputStream;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.eac.CVCertificate;
import org.bouncycastle.asn1.eac.PublicKeyDataObject;
import org.bouncycastle.eac.operator.EACSignatureVerifier;

public class EACCertificateHolder {
    private CVCertificate cvCertificate;

    public EACCertificateHolder(CVCertificate cVCertificate) {
        this.cvCertificate = cVCertificate;
    }

    public EACCertificateHolder(byte[] bArr) throws IOException {
        this(parseBytes(bArr));
    }

    private static CVCertificate parseBytes(byte[] bArr) throws IOException {
        StringBuilder stringBuilder;
        try {
            return CVCertificate.getInstance(bArr);
        } catch (Throwable e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed data: ");
            stringBuilder.append(e.getMessage());
            throw new EACIOException(stringBuilder.toString(), e);
        } catch (Throwable e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed data: ");
            stringBuilder.append(e2.getMessage());
            throw new EACIOException(stringBuilder.toString(), e2);
        } catch (Throwable e22) {
            if (e22.getCause() instanceof IOException) {
                throw ((IOException) e22.getCause());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed data: ");
            stringBuilder.append(e22.getMessage());
            throw new EACIOException(stringBuilder.toString(), e22);
        }
    }

    public PublicKeyDataObject getPublicKeyDataObject() {
        return this.cvCertificate.getBody().getPublicKey();
    }

    public boolean isSignatureValid(EACSignatureVerifier eACSignatureVerifier) throws EACException {
        try {
            OutputStream outputStream = eACSignatureVerifier.getOutputStream();
            outputStream.write(this.cvCertificate.getBody().getEncoded(ASN1Encoding.DER));
            outputStream.close();
            return eACSignatureVerifier.verify(this.cvCertificate.getSignature());
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unable to process signature: ");
            stringBuilder.append(e.getMessage());
            throw new EACException(stringBuilder.toString(), e);
        }
    }

    public CVCertificate toASN1Structure() {
        return this.cvCertificate;
    }
}
