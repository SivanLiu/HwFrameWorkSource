package sun.security.provider.certpath;

import java.io.IOException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRLReason;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.Extension;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import sun.misc.HexDumpEncoder;
import sun.security.action.GetIntegerAction;
import sun.security.provider.certpath.OCSP.RevocationStatus;
import sun.security.provider.certpath.OCSP.RevocationStatus.CertStatus;
import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X509CertImpl;

public final class OCSPResponse {
    private static final int CERT_STATUS_GOOD = 0;
    private static final int CERT_STATUS_REVOKED = 1;
    private static final int CERT_STATUS_UNKNOWN = 2;
    private static final int DEFAULT_MAX_CLOCK_SKEW = 900000;
    private static final int KEY_TAG = 2;
    private static final String KP_OCSP_SIGNING_OID = "1.3.6.1.5.5.7.3.9";
    private static final int MAX_CLOCK_SKEW = initializeClockSkew();
    private static final int NAME_TAG = 1;
    private static final ObjectIdentifier OCSP_BASIC_RESPONSE_OID = ObjectIdentifier.newInternal(new int[]{1, 3, 6, 1, 5, 5, 7, 48, 1, 1});
    private static final Debug debug = Debug.getInstance("certpath");
    private static final boolean dump;
    private static ResponseStatus[] rsvalues = ResponseStatus.values();
    private static CRLReason[] values = CRLReason.values();
    private List<X509CertImpl> certs;
    private KeyIdentifier responderKeyId = null;
    private X500Principal responderName = null;
    private final byte[] responseNonce;
    private final ResponseStatus responseStatus;
    private final AlgorithmId sigAlgId;
    private final byte[] signature;
    private X509CertImpl signerCert = null;
    private final Map<CertId, SingleResponse> singleResponseMap;
    private final byte[] tbsResponseData;

    static final class SingleResponse implements RevocationStatus {
        private final CertId certId;
        private final CertStatus certStatus;
        private final Date nextUpdate;
        private final CRLReason revocationReason;
        private final Date revocationTime;
        private final Map<String, Extension> singleExtensions;
        private final Date thisUpdate;

        private SingleResponse(DerValue der) throws IOException {
            if (der.tag == (byte) 48) {
                DerInputStream tmp = der.data;
                this.certId = new CertId(tmp.getDerValue().data);
                DerValue derVal = tmp.getDerValue();
                short tag = (short) ((byte) (derVal.tag & 31));
                if (tag == (short) 1) {
                    this.certStatus = CertStatus.REVOKED;
                    this.revocationTime = derVal.data.getGeneralizedTime();
                    if (derVal.data.available() != 0) {
                        DerValue dv = derVal.data.getDerValue();
                        if (((short) ((byte) (dv.tag & 31))) == (short) 0) {
                            int reason = dv.data.getEnumerated();
                            if (reason < 0 || reason >= OCSPResponse.values.length) {
                                this.revocationReason = CRLReason.UNSPECIFIED;
                            } else {
                                this.revocationReason = OCSPResponse.values[reason];
                            }
                        } else {
                            this.revocationReason = CRLReason.UNSPECIFIED;
                        }
                    } else {
                        this.revocationReason = CRLReason.UNSPECIFIED;
                    }
                    if (OCSPResponse.debug != null) {
                        Debug access$500 = OCSPResponse.debug;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Revocation time: ");
                        stringBuilder.append(this.revocationTime);
                        access$500.println(stringBuilder.toString());
                        access$500 = OCSPResponse.debug;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Revocation reason: ");
                        stringBuilder.append(this.revocationReason);
                        access$500.println(stringBuilder.toString());
                    }
                } else {
                    this.revocationTime = null;
                    this.revocationReason = CRLReason.UNSPECIFIED;
                    if (tag == (short) 0) {
                        this.certStatus = CertStatus.GOOD;
                    } else if (tag == (short) 2) {
                        this.certStatus = CertStatus.UNKNOWN;
                    } else {
                        throw new IOException("Invalid certificate status");
                    }
                }
                this.thisUpdate = tmp.getGeneralizedTime();
                if (tmp.available() == 0) {
                    this.nextUpdate = null;
                } else {
                    derVal = tmp.getDerValue();
                    if (((short) ((byte) (derVal.tag & 31))) == (short) 0) {
                        this.nextUpdate = derVal.data.getGeneralizedTime();
                        if (tmp.available() != 0) {
                            tag = (short) ((byte) (tmp.getDerValue().tag & 31));
                        }
                    } else {
                        this.nextUpdate = null;
                    }
                }
                if (tmp.available() > 0) {
                    derVal = tmp.getDerValue();
                    if (derVal.isContextSpecific((byte) 1)) {
                        DerValue[] singleExtDer = derVal.data.getSequence(3);
                        this.singleExtensions = new HashMap(singleExtDer.length);
                        for (DerValue extension : singleExtDer) {
                            StringBuilder stringBuilder2;
                            Object ext = new sun.security.x509.Extension(extension);
                            if (OCSPResponse.debug != null) {
                                Debug access$5002 = OCSPResponse.debug;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("OCSP single extension: ");
                                stringBuilder2.append(ext);
                                access$5002.println(stringBuilder2.toString());
                            }
                            if (ext.isCritical()) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unsupported OCSP critical extension: ");
                                stringBuilder2.append(ext.getExtensionId());
                                throw new IOException(stringBuilder2.toString());
                            }
                            this.singleExtensions.put(ext.getId(), ext);
                        }
                        return;
                    }
                    this.singleExtensions = Collections.emptyMap();
                    return;
                }
                this.singleExtensions = Collections.emptyMap();
                return;
            }
            throw new IOException("Bad ASN.1 encoding in SingleResponse");
        }

        public CertStatus getCertStatus() {
            return this.certStatus;
        }

        private CertId getCertId() {
            return this.certId;
        }

        public Date getRevocationTime() {
            return (Date) this.revocationTime.clone();
        }

        public CRLReason getRevocationReason() {
            return this.revocationReason;
        }

        public Map<String, Extension> getSingleExtensions() {
            return Collections.unmodifiableMap(this.singleExtensions);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SingleResponse:  \n");
            sb.append(this.certId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\nCertStatus: ");
            stringBuilder.append(this.certStatus);
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
            if (this.certStatus == CertStatus.REVOKED) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("revocationTime is ");
                stringBuilder.append(this.revocationTime);
                stringBuilder.append("\n");
                sb.append(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("revocationReason is ");
                stringBuilder.append(this.revocationReason);
                stringBuilder.append("\n");
                sb.append(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("thisUpdate is ");
            stringBuilder.append(this.thisUpdate);
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
            if (this.nextUpdate != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("nextUpdate is ");
                stringBuilder.append(this.nextUpdate);
                stringBuilder.append("\n");
                sb.append(stringBuilder.toString());
            }
            return sb.toString();
        }
    }

    public enum ResponseStatus {
        SUCCESSFUL,
        MALFORMED_REQUEST,
        INTERNAL_ERROR,
        TRY_LATER,
        UNUSED,
        SIG_REQUIRED,
        UNAUTHORIZED
    }

    static {
        boolean z = debug != null && Debug.isOn("ocsp");
        dump = z;
    }

    private static int initializeClockSkew() {
        Integer tmp = (Integer) AccessController.doPrivileged(new GetIntegerAction("com.sun.security.ocsp.clockSkew"));
        if (tmp == null || tmp.intValue() < 0) {
            return DEFAULT_MAX_CLOCK_SKEW;
        }
        return tmp.intValue() * 1000;
    }

    OCSPResponse(byte[] bytes) throws IOException {
        CertificateException ce;
        byte[] bArr = bytes;
        if (dump) {
            HexDumpEncoder hexEnc = new HexDumpEncoder();
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("OCSPResponse bytes...\n\n");
            stringBuilder.append(hexEnc.encode(bArr));
            stringBuilder.append("\n");
            debug.println(stringBuilder.toString());
        }
        DerValue der = new DerValue(bArr);
        if (der.tag == (byte) 48) {
            DerInputStream derIn = der.getData();
            int status = derIn.getEnumerated();
            StringBuilder stringBuilder2;
            if (status < 0 || status >= rsvalues.length) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknown OCSPResponse status: ");
                stringBuilder2.append(status);
                throw new IOException(stringBuilder2.toString());
            }
            this.responseStatus = rsvalues[status];
            if (debug != null) {
                Debug debug2 = debug;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("OCSP response status: ");
                stringBuilder3.append(this.responseStatus);
                debug2.println(stringBuilder3.toString());
            }
            if (this.responseStatus != ResponseStatus.SUCCESSFUL) {
                this.singleResponseMap = Collections.emptyMap();
                this.certs = new ArrayList();
                this.sigAlgId = null;
                this.signature = null;
                this.tbsResponseData = null;
                this.responseNonce = null;
                return;
            }
            der = derIn.getDerValue();
            if (der.isContextSpecific((byte) 0)) {
                DerValue tmp = der.data.getDerValue();
                if (tmp.tag == (byte) 48) {
                    derIn = tmp.data;
                    Object responseType = derIn.getOID();
                    Debug debug3;
                    DerInputStream derInputStream;
                    if (responseType.equals(OCSP_BASIC_RESPONSE_OID)) {
                        if (debug != null) {
                            debug.println("OCSP response type: basic");
                        }
                        DerValue[] seqTmp = new DerInputStream(derIn.getOctetString()).getSequence(2);
                        if (seqTmp.length >= 3) {
                            DerValue responseData = seqTmp[0];
                            this.tbsResponseData = seqTmp[0].toByteArray();
                            if (responseData.tag == (byte) 48) {
                                StringBuilder stringBuilder4;
                                DerInputStream derInputStream2;
                                DerValue seq;
                                DerInputStream seqDerIn = responseData.data;
                                DerValue seq2 = seqDerIn.getDerValue();
                                if (seq2.isContextSpecific((byte) 0) && seq2.isConstructed() && seq2.isContextSpecific()) {
                                    DerValue seq3 = seq2.data.getDerValue();
                                    int version = seq3.getInteger();
                                    if (seq3.data.available() == 0) {
                                        seq2 = seqDerIn.getDerValue();
                                    } else {
                                        throw new IOException("Bad encoding in version  element of OCSP response: bad format");
                                    }
                                }
                                short tag = (short) ((byte) (seq2.tag & 31));
                                if (tag == (short) 1) {
                                    this.responderName = new X500Principal(seq2.getData().toByteArray());
                                    if (debug != null) {
                                        debug3 = debug;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Responder's name: ");
                                        stringBuilder4.append(this.responderName);
                                        debug3.println(stringBuilder4.toString());
                                    }
                                } else if (tag == (short) 2) {
                                    this.responderKeyId = new KeyIdentifier(seq2.getData().getOctetString());
                                    if (debug != null) {
                                        debug3 = debug;
                                        StringBuilder stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("Responder's key ID: ");
                                        stringBuilder5.append(Debug.toString(this.responderKeyId.getIdentifier()));
                                        debug3.println(stringBuilder5.toString());
                                    }
                                } else {
                                    derInputStream = derIn;
                                    derInputStream2 = seqDerIn;
                                    throw new IOException("Bad encoding in responderID element of OCSP response: expected ASN.1 context specific tag 0 or 1");
                                }
                                DerValue seq4 = seqDerIn.getDerValue();
                                if (debug != null) {
                                    Object producedAtDate = seq4.getGeneralizedTime();
                                    Debug debug4 = debug;
                                    StringBuilder stringBuilder6 = new StringBuilder();
                                    seq = seq4;
                                    stringBuilder6.append("OCSP response produced at: ");
                                    stringBuilder6.append(producedAtDate);
                                    debug4.println(stringBuilder6.toString());
                                } else {
                                    seq = seq4;
                                }
                                DerValue[] singleResponseDer = seqDerIn.getSequence(1);
                                this.singleResponseMap = new HashMap(singleResponseDer.length);
                                if (debug != null) {
                                    debug3 = debug;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("OCSP number of SingleResponses: ");
                                    stringBuilder4.append(singleResponseDer.length);
                                    debug3.println(stringBuilder4.toString());
                                }
                                int i = 0;
                                while (i < singleResponseDer.length) {
                                    SingleResponse singleResponse = new SingleResponse(singleResponseDer[i]);
                                    this.singleResponseMap.put(singleResponse.getCertId(), singleResponse);
                                    i++;
                                    bArr = bytes;
                                }
                                DerValue seq5;
                                if (seqDerIn.available() > 0) {
                                    DerValue seq6 = seqDerIn.getDerValue();
                                    DerValue der2;
                                    if (seq6.isContextSpecific((byte) 1)) {
                                        DerValue[] responseExtDer = seq6.data.getSequence(3);
                                        seq2 = null;
                                        byte[] nonce = null;
                                        while (true) {
                                            seq5 = seq6;
                                            if (nonce >= responseExtDer.length) {
                                                derInputStream = derIn;
                                                derInputStream2 = seqDerIn;
                                                break;
                                            }
                                            StringBuilder stringBuilder7;
                                            der2 = der;
                                            Object seq7 = new sun.security.x509.Extension(responseExtDer[nonce]);
                                            if (debug != null) {
                                                der = debug;
                                                derInputStream = derIn;
                                                stringBuilder7 = new StringBuilder();
                                                derInputStream2 = seqDerIn;
                                                stringBuilder7.append("OCSP extension: ");
                                                stringBuilder7.append(seq7);
                                                der.println(stringBuilder7.toString());
                                            } else {
                                                derInputStream = derIn;
                                                derInputStream2 = seqDerIn;
                                            }
                                            if (seq7.getExtensionId().equals(OCSP.NONCE_EXTENSION_OID) != null) {
                                                seq2 = seq7.getExtensionValue();
                                            } else if (seq7.isCritical() != null) {
                                                stringBuilder7 = new StringBuilder();
                                                stringBuilder7.append("Unsupported OCSP critical extension: ");
                                                stringBuilder7.append(seq7.getExtensionId());
                                                throw new IOException(stringBuilder7.toString());
                                            }
                                            nonce++;
                                            seq6 = seq5;
                                            der = der2;
                                            derIn = derInputStream;
                                            seqDerIn = derInputStream2;
                                        }
                                    } else {
                                        der2 = der;
                                        derInputStream = derIn;
                                        derInputStream2 = seqDerIn;
                                        seq2 = null;
                                    }
                                } else {
                                    derInputStream = derIn;
                                    derInputStream2 = seqDerIn;
                                    seq2 = null;
                                    seq5 = seq;
                                }
                                this.responseNonce = seq2;
                                this.sigAlgId = AlgorithmId.parse(seqTmp[1]);
                                this.signature = seqTmp[2].getBitString();
                                if (seqTmp.length > 3) {
                                    der = seqTmp[3];
                                    i = 0;
                                    if (der.isContextSpecific((byte) 0)) {
                                        DerValue[] derCerts = der.getData().getSequence(3);
                                        this.certs = new ArrayList(derCerts.length);
                                        while (i < derCerts.length) {
                                            DerValue[] derCerts2;
                                            try {
                                                X509CertImpl cert = new X509CertImpl(derCerts[i].toByteArray());
                                                this.certs.add(cert);
                                                if (debug != null) {
                                                    Debug debug5 = debug;
                                                    stringBuilder4 = new StringBuilder();
                                                    derCerts2 = derCerts;
                                                    try {
                                                        stringBuilder4.append("OCSP response cert #");
                                                        stringBuilder4.append(i + 1);
                                                        stringBuilder4.append(": ");
                                                        stringBuilder4.append(cert.getSubjectX500Principal());
                                                        debug5.println(stringBuilder4.toString());
                                                    } catch (CertificateException e) {
                                                        ce = e;
                                                    }
                                                } else {
                                                    derCerts2 = derCerts;
                                                }
                                                i++;
                                                derCerts = derCerts2;
                                            } catch (CertificateException e2) {
                                                ce = e2;
                                                derCerts2 = derCerts;
                                                throw new IOException("Bad encoding in X509 Certificate", ce);
                                            }
                                        }
                                    } else {
                                        throw new IOException("Bad encoding in certs element of OCSP response: expected ASN.1 context specific tag 0.");
                                    }
                                }
                                this.certs = new ArrayList();
                                return;
                            }
                            derInputStream = derIn;
                            throw new IOException("Bad encoding in tbsResponseData element of OCSP response: expected ASN.1 SEQUENCE tag.");
                        }
                        derInputStream = derIn;
                        throw new IOException("Unexpected BasicOCSPResponse value");
                    }
                    derInputStream = derIn;
                    if (debug != null) {
                        debug3 = debug;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("OCSP response type: ");
                        stringBuilder2.append(responseType);
                        debug3.println(stringBuilder2.toString());
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unsupported OCSP response type: ");
                    stringBuilder2.append(responseType);
                    throw new IOException(stringBuilder2.toString());
                }
                throw new IOException("Bad encoding in responseBytes element of OCSP response: expected ASN.1 SEQUENCE tag.");
            }
            throw new IOException("Bad encoding in responseBytes element of OCSP response: expected ASN.1 context specific tag 0.");
        }
        throw new IOException("Bad encoding in OCSP response: expected ASN.1 SEQUENCE tag.");
    }

    void verify(List<CertId> certIds, X509Certificate issuerCert, X509Certificate responderCert, Date date, byte[] nonce) throws CertPathValidatorException {
        StringBuilder stringBuilder;
        switch (this.responseStatus) {
            case SUCCESSFUL:
                for (Object certId : certIds) {
                    SingleResponse sr = getSingleResponse(certId);
                    if (sr == null) {
                        if (debug != null) {
                            Debug debug = debug;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("No response found for CertId: ");
                            stringBuilder2.append(certId);
                            debug.println(stringBuilder2.toString());
                        }
                        throw new CertPathValidatorException("OCSP response does not include a response for a certificate supplied in the OCSP request");
                    } else if (debug != null) {
                        Debug debug2 = debug;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Status of certificate (with serial number ");
                        stringBuilder3.append(certId.getSerialNumber());
                        stringBuilder3.append(") is: ");
                        stringBuilder3.append(sr.getCertStatus());
                        debug2.println(stringBuilder3.toString());
                    }
                }
                if (this.signerCert == null) {
                    try {
                        this.certs.add(X509CertImpl.toImpl(issuerCert));
                        if (responderCert != null) {
                            this.certs.add(X509CertImpl.toImpl(responderCert));
                        }
                        if (this.responderName != null) {
                            for (X509CertImpl cert : this.certs) {
                                if (cert.getSubjectX500Principal().equals(this.responderName)) {
                                    this.signerCert = cert;
                                }
                            }
                        } else if (this.responderKeyId != null) {
                            for (X509CertImpl cert2 : this.certs) {
                                KeyIdentifier certKeyId = cert2.getSubjectKeyId();
                                if (certKeyId == null || !this.responderKeyId.equals(certKeyId)) {
                                    try {
                                        certKeyId = new KeyIdentifier(cert2.getPublicKey());
                                    } catch (IOException e) {
                                    }
                                    if (this.responderKeyId.equals(certKeyId)) {
                                        this.signerCert = cert2;
                                    }
                                } else {
                                    this.signerCert = cert2;
                                }
                            }
                        }
                    } catch (CertificateException ce) {
                        throw new CertPathValidatorException("Invalid issuer or trusted responder certificate", ce);
                    }
                }
                if (this.signerCert != null) {
                    if (this.signerCert.equals(issuerCert)) {
                        if (debug != null) {
                            debug.println("OCSP response is signed by the target's Issuing CA");
                        }
                    } else if (this.signerCert.equals(responderCert)) {
                        if (debug != null) {
                            debug.println("OCSP response is signed by a Trusted Responder");
                        }
                    } else if (this.signerCert.getIssuerX500Principal().equals(issuerCert.getSubjectX500Principal())) {
                        try {
                            List<String> keyPurposes = this.signerCert.getExtendedKeyUsage();
                            if (keyPurposes == null || !keyPurposes.contains(KP_OCSP_SIGNING_OID)) {
                                throw new CertPathValidatorException("Responder's certificate not valid for signing OCSP responses");
                            }
                            keyPurposes = new AlgorithmChecker(new TrustAnchor(issuerCert, null));
                            keyPurposes.init(false);
                            keyPurposes.check(this.signerCert, Collections.emptySet());
                            if (date == null) {
                                try {
                                    this.signerCert.checkValidity();
                                } catch (CertificateException e2) {
                                    throw new CertPathValidatorException("Responder's certificate not within the validity period", e2);
                                }
                            }
                            this.signerCert.checkValidity(date);
                            if (!(this.signerCert.getExtension(PKIXExtensions.OCSPNoCheck_Id) == null || debug == null)) {
                                debug.println("Responder's certificate includes the extension id-pkix-ocsp-nocheck.");
                            }
                            try {
                                this.signerCert.verify(issuerCert.getPublicKey());
                                if (debug != null) {
                                    debug.println("OCSP response is signed by an Authorized Responder");
                                }
                            } catch (GeneralSecurityException e3) {
                                this.signerCert = null;
                            }
                        } catch (CertificateParsingException cpe) {
                            throw new CertPathValidatorException("Responder's certificate not valid for signing OCSP responses", cpe);
                        }
                    } else {
                        throw new CertPathValidatorException("Responder's certificate is not authorized to sign OCSP responses");
                    }
                }
                if (this.signerCert != null) {
                    AlgorithmChecker.check(this.signerCert.getPublicKey(), this.sigAlgId);
                    if (!verifySignature(this.signerCert)) {
                        throw new CertPathValidatorException("Error verifying OCSP Response's signature");
                    } else if (nonce == null || this.responseNonce == null || Arrays.equals(nonce, this.responseNonce)) {
                        long now = date == null ? System.currentTimeMillis() : date.getTime();
                        Date nowPlusSkew = new Date(((long) MAX_CLOCK_SKEW) + now);
                        Date nowMinusSkew = new Date(now - ((long) MAX_CLOCK_SKEW));
                        for (SingleResponse sr2 : this.singleResponseMap.values()) {
                            if (debug != null) {
                                String until = "";
                                if (sr2.nextUpdate != null) {
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append(" until ");
                                    stringBuilder4.append(sr2.nextUpdate);
                                    until = stringBuilder4.toString();
                                }
                                Debug debug3 = debug;
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("OCSP response validity interval is from ");
                                stringBuilder5.append(sr2.thisUpdate);
                                stringBuilder5.append(until);
                                debug3.println(stringBuilder5.toString());
                                debug3 = debug;
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("Checking validity of OCSP response on: ");
                                stringBuilder5.append(new Date(now));
                                debug3.println(stringBuilder5.toString());
                            }
                            if (!nowPlusSkew.before(sr2.thisUpdate)) {
                                if (nowMinusSkew.after(sr2.nextUpdate != null ? sr2.nextUpdate : sr2.thisUpdate)) {
                                }
                            }
                            throw new CertPathValidatorException("Response is unreliable: its validity interval is out-of-date");
                        }
                        return;
                    } else {
                        throw new CertPathValidatorException("Nonces don't match");
                    }
                }
                throw new CertPathValidatorException("Unable to verify OCSP Response's signature");
            case TRY_LATER:
            case INTERNAL_ERROR:
                stringBuilder = new StringBuilder();
                stringBuilder.append("OCSP response error: ");
                stringBuilder.append(this.responseStatus);
                throw new CertPathValidatorException(stringBuilder.toString(), null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("OCSP response error: ");
                stringBuilder.append(this.responseStatus);
                throw new CertPathValidatorException(stringBuilder.toString());
        }
    }

    ResponseStatus getResponseStatus() {
        return this.responseStatus;
    }

    private boolean verifySignature(X509Certificate cert) throws CertPathValidatorException {
        try {
            Signature respSignature = Signature.getInstance(this.sigAlgId.getName());
            respSignature.initVerify(cert.getPublicKey());
            respSignature.update(this.tbsResponseData);
            if (respSignature.verify(this.signature)) {
                if (debug != null) {
                    debug.println("Verified signature of OCSP Response");
                }
                return true;
            }
            if (debug != null) {
                debug.println("Error verifying signature of OCSP Response");
            }
            return false;
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            throw new CertPathValidatorException(e);
        }
    }

    SingleResponse getSingleResponse(CertId certId) {
        return (SingleResponse) this.singleResponseMap.get(certId);
    }

    X509Certificate getSignerCertificate() {
        return this.signerCert;
    }
}
