package org.bouncycastle.cert.ocsp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.ocsp.OCSPResponse;
import org.bouncycastle.asn1.ocsp.ResponseBytes;
import org.bouncycastle.cert.CertIOException;

public class OCSPResp {
    public static final int INTERNAL_ERROR = 2;
    public static final int MALFORMED_REQUEST = 1;
    public static final int SIG_REQUIRED = 5;
    public static final int SUCCESSFUL = 0;
    public static final int TRY_LATER = 3;
    public static final int UNAUTHORIZED = 6;
    private OCSPResponse resp;

    public OCSPResp(InputStream inputStream) throws IOException {
        this(new ASN1InputStream(inputStream));
    }

    private OCSPResp(ASN1InputStream aSN1InputStream) throws IOException {
        StringBuilder stringBuilder;
        try {
            this.resp = OCSPResponse.getInstance(aSN1InputStream.readObject());
            if (this.resp == null) {
                throw new CertIOException("malformed response: no response data found");
            }
        } catch (Throwable e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed response: ");
            stringBuilder.append(e.getMessage());
            throw new CertIOException(stringBuilder.toString(), e);
        } catch (Throwable e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed response: ");
            stringBuilder.append(e2.getMessage());
            throw new CertIOException(stringBuilder.toString(), e2);
        } catch (Throwable e22) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("malformed response: ");
            stringBuilder.append(e22.getMessage());
            throw new CertIOException(stringBuilder.toString(), e22);
        }
    }

    public OCSPResp(OCSPResponse oCSPResponse) {
        this.resp = oCSPResponse;
    }

    public OCSPResp(byte[] bArr) throws IOException {
        this(new ByteArrayInputStream(bArr));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof OCSPResp)) {
            return false;
        }
        return this.resp.equals(((OCSPResp) obj).resp);
    }

    public byte[] getEncoded() throws IOException {
        return this.resp.getEncoded();
    }

    public Object getResponseObject() throws OCSPException {
        ResponseBytes responseBytes = this.resp.getResponseBytes();
        if (responseBytes == null) {
            return null;
        }
        if (!responseBytes.getResponseType().equals(OCSPObjectIdentifiers.id_pkix_ocsp_basic)) {
            return responseBytes.getResponse();
        }
        try {
            return new BasicOCSPResp(BasicOCSPResponse.getInstance(ASN1Primitive.fromByteArray(responseBytes.getResponse().getOctets())));
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("problem decoding object: ");
            stringBuilder.append(e);
            throw new OCSPException(stringBuilder.toString(), e);
        }
    }

    public int getStatus() {
        return this.resp.getResponseStatus().getValue().intValue();
    }

    public int hashCode() {
        return this.resp.hashCode();
    }

    public OCSPResponse toASN1Structure() {
        return this.resp;
    }
}
