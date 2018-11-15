package org.bouncycastle.cert.ocsp;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.asn1.ocsp.CertStatus;
import org.bouncycastle.asn1.ocsp.ResponseData;
import org.bouncycastle.asn1.ocsp.RevokedInfo;
import org.bouncycastle.asn1.ocsp.SingleResponse;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;

public class BasicOCSPRespBuilder {
    private List list = new ArrayList();
    private RespID responderID;
    private Extensions responseExtensions = null;

    private class ResponseObject {
        CertificateID certId;
        CertStatus certStatus;
        Extensions extensions;
        ASN1GeneralizedTime nextUpdate;
        ASN1GeneralizedTime thisUpdate;

        public ResponseObject(CertificateID certificateID, CertificateStatus certificateStatus, Date date, Date date2, Extensions extensions) {
            CertStatus certStatus;
            this.certId = certificateID;
            ASN1GeneralizedTime aSN1GeneralizedTime = null;
            if (certificateStatus == null) {
                certStatus = new CertStatus();
            } else if (certificateStatus instanceof UnknownStatus) {
                certStatus = new CertStatus(2, DERNull.INSTANCE);
            } else {
                RevokedStatus revokedStatus = (RevokedStatus) certificateStatus;
                certStatus = revokedStatus.hasRevocationReason() ? new CertStatus(new RevokedInfo(new ASN1GeneralizedTime(revokedStatus.getRevocationTime()), CRLReason.lookup(revokedStatus.getRevocationReason()))) : new CertStatus(new RevokedInfo(new ASN1GeneralizedTime(revokedStatus.getRevocationTime()), null));
            }
            this.certStatus = certStatus;
            this.thisUpdate = new DERGeneralizedTime(date);
            if (date2 != null) {
                aSN1GeneralizedTime = new DERGeneralizedTime(date2);
            }
            this.nextUpdate = aSN1GeneralizedTime;
            this.extensions = extensions;
        }

        public SingleResponse toResponse() throws Exception {
            return new SingleResponse(this.certId.toASN1Primitive(), this.certStatus, this.thisUpdate, this.nextUpdate, this.extensions);
        }
    }

    public BasicOCSPRespBuilder(SubjectPublicKeyInfo subjectPublicKeyInfo, DigestCalculator digestCalculator) throws OCSPException {
        this.responderID = new RespID(subjectPublicKeyInfo, digestCalculator);
    }

    public BasicOCSPRespBuilder(RespID respID) {
        this.responderID = respID;
    }

    public BasicOCSPRespBuilder addResponse(CertificateID certificateID, CertificateStatus certificateStatus) {
        addResponse(certificateID, certificateStatus, new Date(), null, null);
        return this;
    }

    public BasicOCSPRespBuilder addResponse(CertificateID certificateID, CertificateStatus certificateStatus, Date date, Date date2) {
        addResponse(certificateID, certificateStatus, date, date2, null);
        return this;
    }

    public BasicOCSPRespBuilder addResponse(CertificateID certificateID, CertificateStatus certificateStatus, Date date, Date date2, Extensions extensions) {
        this.list.add(new ResponseObject(certificateID, certificateStatus, date, date2, extensions));
        return this;
    }

    public BasicOCSPRespBuilder addResponse(CertificateID certificateID, CertificateStatus certificateStatus, Date date, Extensions extensions) {
        addResponse(certificateID, certificateStatus, new Date(), date, extensions);
        return this;
    }

    public BasicOCSPRespBuilder addResponse(CertificateID certificateID, CertificateStatus certificateStatus, Extensions extensions) {
        addResponse(certificateID, certificateStatus, new Date(), null, extensions);
        return this;
    }

    public BasicOCSPResp build(ContentSigner contentSigner, X509CertificateHolder[] x509CertificateHolderArr, Date date) throws OCSPException {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        for (ResponseObject toResponse : this.list) {
            try {
                aSN1EncodableVector.add(toResponse.toResponse());
            } catch (Throwable e) {
                throw new OCSPException("exception creating Request", e);
            }
        }
        ResponseData responseData = new ResponseData(this.responderID.toASN1Primitive(), new ASN1GeneralizedTime(date), new DERSequence(aSN1EncodableVector), this.responseExtensions);
        try {
            ASN1Sequence aSN1Sequence;
            OutputStream outputStream = contentSigner.getOutputStream();
            outputStream.write(responseData.getEncoded(ASN1Encoding.DER));
            outputStream.close();
            DERBitString dERBitString = new DERBitString(contentSigner.getSignature());
            AlgorithmIdentifier algorithmIdentifier = contentSigner.getAlgorithmIdentifier();
            if (x509CertificateHolderArr == null || x509CertificateHolderArr.length <= 0) {
                aSN1Sequence = null;
            } else {
                aSN1EncodableVector = new ASN1EncodableVector();
                for (int i = 0; i != x509CertificateHolderArr.length; i++) {
                    aSN1EncodableVector.add(x509CertificateHolderArr[i].toASN1Structure());
                }
                aSN1Sequence = new DERSequence(aSN1EncodableVector);
            }
            return new BasicOCSPResp(new BasicOCSPResponse(responseData, algorithmIdentifier, dERBitString, aSN1Sequence));
        } catch (Throwable e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception processing TBSRequest: ");
            stringBuilder.append(e2.getMessage());
            throw new OCSPException(stringBuilder.toString(), e2);
        }
    }

    public BasicOCSPRespBuilder setResponseExtensions(Extensions extensions) {
        this.responseExtensions = extensions;
        return this;
    }
}
