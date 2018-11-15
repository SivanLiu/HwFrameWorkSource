package org.bouncycastle.cert.ocsp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ocsp.OCSPRequest;
import org.bouncycastle.asn1.ocsp.Request;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;

public class OCSPReq {
    private static final X509CertificateHolder[] EMPTY_CERTS = new X509CertificateHolder[0];
    private Extensions extensions;
    private OCSPRequest req;

    private OCSPReq(ASN1InputStream aSN1InputStream) throws IOException {
        StringBuilder stringBuilder;
        try {
            this.req = OCSPRequest.getInstance(aSN1InputStream.readObject());
            if (this.req != null) {
                this.extensions = this.req.getTbsRequest().getRequestExtensions();
                return;
            }
            throw new CertIOException("malformed request: no request data found");
        } catch (Throwable e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed request: ");
            stringBuilder.append(e.getMessage());
            throw new CertIOException(stringBuilder.toString(), e);
        } catch (Throwable e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed request: ");
            stringBuilder.append(e2.getMessage());
            throw new CertIOException(stringBuilder.toString(), e2);
        } catch (Throwable e22) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed request: ");
            stringBuilder.append(e22.getMessage());
            throw new CertIOException(stringBuilder.toString(), e22);
        }
    }

    public OCSPReq(OCSPRequest oCSPRequest) {
        this.req = oCSPRequest;
        this.extensions = oCSPRequest.getTbsRequest().getRequestExtensions();
    }

    public OCSPReq(byte[] bArr) throws IOException {
        this(new ASN1InputStream(bArr));
    }

    public X509CertificateHolder[] getCerts() {
        if (this.req.getOptionalSignature() == null) {
            return EMPTY_CERTS;
        }
        ASN1Sequence certs = this.req.getOptionalSignature().getCerts();
        if (certs == null) {
            return EMPTY_CERTS;
        }
        X509CertificateHolder[] x509CertificateHolderArr = new X509CertificateHolder[certs.size()];
        for (int i = 0; i != x509CertificateHolderArr.length; i++) {
            x509CertificateHolderArr[i] = new X509CertificateHolder(Certificate.getInstance(certs.getObjectAt(i)));
        }
        return x509CertificateHolderArr;
    }

    public Set getCriticalExtensionOIDs() {
        return OCSPUtils.getCriticalExtensionOIDs(this.extensions);
    }

    public byte[] getEncoded() throws IOException {
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        new ASN1OutputStream(byteArrayOutputStream).writeObject(this.req);
        return byteArrayOutputStream.toByteArray();
    }

    public Extension getExtension(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return this.extensions != null ? this.extensions.getExtension(aSN1ObjectIdentifier) : null;
    }

    public List getExtensionOIDs() {
        return OCSPUtils.getExtensionOIDs(this.extensions);
    }

    public Set getNonCriticalExtensionOIDs() {
        return OCSPUtils.getNonCriticalExtensionOIDs(this.extensions);
    }

    public Req[] getRequestList() {
        ASN1Sequence requestList = this.req.getTbsRequest().getRequestList();
        Req[] reqArr = new Req[requestList.size()];
        for (int i = 0; i != reqArr.length; i++) {
            reqArr[i] = new Req(Request.getInstance(requestList.getObjectAt(i)));
        }
        return reqArr;
    }

    public GeneralName getRequestorName() {
        return GeneralName.getInstance(this.req.getTbsRequest().getRequestorName());
    }

    public byte[] getSignature() {
        return !isSigned() ? null : this.req.getOptionalSignature().getSignature().getOctets();
    }

    public ASN1ObjectIdentifier getSignatureAlgOID() {
        return !isSigned() ? null : this.req.getOptionalSignature().getSignatureAlgorithm().getAlgorithm();
    }

    public int getVersionNumber() {
        return this.req.getTbsRequest().getVersion().getValue().intValue() + 1;
    }

    public boolean hasExtensions() {
        return this.extensions != null;
    }

    public boolean isSignatureValid(ContentVerifierProvider contentVerifierProvider) throws OCSPException {
        if (isSigned()) {
            try {
                ContentVerifier contentVerifier = contentVerifierProvider.get(this.req.getOptionalSignature().getSignatureAlgorithm());
                contentVerifier.getOutputStream().write(this.req.getTbsRequest().getEncoded(ASN1Encoding.DER));
                return contentVerifier.verify(getSignature());
            } catch (Throwable e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception processing signature: ");
                stringBuilder.append(e);
                throw new OCSPException(stringBuilder.toString(), e);
            }
        }
        throw new OCSPException("attempt to verify signature on unsigned object");
    }

    public boolean isSigned() {
        return this.req.getOptionalSignature() != null;
    }
}
