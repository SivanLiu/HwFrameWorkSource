package sun.security.provider.certpath;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CRLReason;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateRevokedException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.Extension;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PKIXRevocationChecker.Option;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.provider.certpath.OCSP.RevocationStatus.CertStatus;
import sun.security.util.Debug;
import sun.security.x509.AccessDescription;
import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.DistributionPoint;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X500Name;
import sun.security.x509.X509CRLEntryImpl;
import sun.security.x509.X509CertImpl;

class RevocationChecker extends PKIXRevocationChecker {
    private static final boolean[] ALL_REASONS = new boolean[]{true, true, true, true, true, true, true, true, true};
    private static final boolean[] CRL_SIGN_USAGE = new boolean[]{false, false, false, false, false, false, true};
    private static final String HEX_DIGITS = "0123456789ABCDEFabcdef";
    private static final long MAX_CLOCK_SKEW = 900000;
    private static final Debug debug = Debug.getInstance("certpath");
    private TrustAnchor anchor;
    private int certIndex;
    private List<CertStore> certStores;
    private boolean crlDP;
    private boolean crlSignFlag;
    private X509Certificate issuerCert;
    private boolean legacy = false;
    private Mode mode = Mode.PREFER_OCSP;
    private List<Extension> ocspExtensions;
    private Map<X509Certificate, byte[]> ocspResponses;
    private boolean onlyEE;
    private ValidatorParams params;
    private PublicKey prevPubKey;
    private X509Certificate responderCert;
    private URI responderURI;
    private boolean softFail;
    private LinkedList<CertPathValidatorException> softFailExceptions = new LinkedList();

    private static class RevocationProperties {
        boolean crlDPEnabled;
        boolean ocspEnabled;
        String ocspIssuer;
        String ocspSerial;
        String ocspSubject;
        String ocspUrl;
        boolean onlyEE;

        private RevocationProperties() {
        }

        /* synthetic */ RevocationProperties(AnonymousClass1 x0) {
            this();
        }
    }

    private enum Mode {
        PREFER_OCSP,
        PREFER_CRLS,
        ONLY_CRLS,
        ONLY_OCSP
    }

    private static class RejectKeySelector extends X509CertSelector {
        private final Set<PublicKey> badKeySet;

        RejectKeySelector(Set<PublicKey> badPublicKeys) {
            this.badKeySet = badPublicKeys;
        }

        public boolean match(Certificate cert) {
            if (!super.match(cert)) {
                return false;
            }
            if (this.badKeySet.contains(cert.getPublicKey())) {
                if (RevocationChecker.debug != null) {
                    RevocationChecker.debug.println("RejectKeySelector.match: bad key");
                }
                return false;
            }
            if (RevocationChecker.debug != null) {
                RevocationChecker.debug.println("RejectKeySelector.match: returning true");
            }
            return true;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("RejectKeySelector: [\n");
            sb.append(super.toString());
            sb.append(this.badKeySet);
            sb.append("]");
            return sb.toString();
        }
    }

    RevocationChecker() {
    }

    RevocationChecker(TrustAnchor anchor, ValidatorParams params) throws CertPathValidatorException {
        init(anchor, params);
    }

    void init(TrustAnchor anchor, ValidatorParams params) throws CertPathValidatorException {
        X509Certificate responderCert;
        StringBuilder stringBuilder;
        RevocationProperties rp = getRevocationProperties();
        URI uri = getOcspResponder();
        this.responderURI = uri == null ? toURI(rp.ocspUrl) : uri;
        X509Certificate cert = getOcspResponderCert();
        if (cert == null) {
            responderCert = getResponderCert(rp, params.trustAnchors(), params.certStores());
        } else {
            responderCert = cert;
        }
        this.responderCert = responderCert;
        Set<Option> options = getOptions();
        for (Object option : options) {
            switch (option) {
                case ONLY_END_ENTITY:
                case PREFER_CRLS:
                case SOFT_FAIL:
                case NO_FALLBACK:
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unrecognized revocation parameter option: ");
                    stringBuilder.append(option);
                    throw new CertPathValidatorException(stringBuilder.toString());
            }
        }
        this.softFail = options.contains(Option.SOFT_FAIL);
        if (this.legacy) {
            this.mode = rp.ocspEnabled ? Mode.PREFER_OCSP : Mode.ONLY_CRLS;
            this.onlyEE = rp.onlyEE;
        } else {
            if (options.contains(Option.NO_FALLBACK)) {
                if (options.contains(Option.PREFER_CRLS)) {
                    this.mode = Mode.ONLY_CRLS;
                } else {
                    this.mode = Mode.ONLY_OCSP;
                }
            } else if (options.contains(Option.PREFER_CRLS)) {
                this.mode = Mode.PREFER_CRLS;
            }
            this.onlyEE = options.contains(Option.ONLY_END_ENTITY);
        }
        if (this.legacy) {
            this.crlDP = rp.crlDPEnabled;
        } else {
            this.crlDP = true;
        }
        this.ocspResponses = getOcspResponses();
        this.ocspExtensions = getOcspExtensions();
        this.anchor = anchor;
        this.params = params;
        this.certStores = new ArrayList(params.certStores());
        try {
            this.certStores.add(CertStore.getInstance("Collection", new CollectionCertStoreParameters(params.certificates())));
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            if (debug != null) {
                Debug debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("RevocationChecker: error creating Collection CertStore: ");
                stringBuilder.append(e);
                debug.println(stringBuilder.toString());
            }
        }
    }

    private static URI toURI(String uriString) throws CertPathValidatorException {
        if (uriString == null) {
            return null;
        }
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new CertPathValidatorException("cannot parse ocsp.responderURL property", e);
        }
    }

    private static RevocationProperties getRevocationProperties() {
        return (RevocationProperties) AccessController.doPrivileged(new PrivilegedAction<RevocationProperties>() {
            public RevocationProperties run() {
                RevocationProperties rp = new RevocationProperties();
                String onlyEE = Security.getProperty("com.sun.security.onlyCheckRevocationOfEECert");
                boolean z = false;
                boolean z2 = onlyEE != null && onlyEE.equalsIgnoreCase("true");
                rp.onlyEE = z2;
                String ocspEnabled = Security.getProperty("ocsp.enable");
                if (ocspEnabled != null && ocspEnabled.equalsIgnoreCase("true")) {
                    z = true;
                }
                rp.ocspEnabled = z;
                rp.ocspUrl = Security.getProperty("ocsp.responderURL");
                rp.ocspSubject = Security.getProperty("ocsp.responderCertSubjectName");
                rp.ocspIssuer = Security.getProperty("ocsp.responderCertIssuerName");
                rp.ocspSerial = Security.getProperty("ocsp.responderCertSerialNumber");
                rp.crlDPEnabled = Boolean.getBoolean("com.sun.security.enableCRLDP");
                return rp;
            }
        });
    }

    private static X509Certificate getResponderCert(RevocationProperties rp, Set<TrustAnchor> anchors, List<CertStore> stores) throws CertPathValidatorException {
        if (rp.ocspSubject != null) {
            return getResponderCert(rp.ocspSubject, (Set) anchors, (List) stores);
        }
        if (rp.ocspIssuer != null && rp.ocspSerial != null) {
            return getResponderCert(rp.ocspIssuer, rp.ocspSerial, anchors, stores);
        }
        if (rp.ocspIssuer == null && rp.ocspSerial == null) {
            return null;
        }
        throw new CertPathValidatorException("Must specify both ocsp.responderCertIssuerName and ocsp.responderCertSerialNumber properties");
    }

    private static X509Certificate getResponderCert(String subject, Set<TrustAnchor> anchors, List<CertStore> stores) throws CertPathValidatorException {
        X509CertSelector sel = new X509CertSelector();
        try {
            sel.setSubject(new X500Principal(subject));
            return getResponderCert(sel, (Set) anchors, (List) stores);
        } catch (IllegalArgumentException e) {
            throw new CertPathValidatorException("cannot parse ocsp.responderCertSubjectName property", e);
        }
    }

    private static X509Certificate getResponderCert(String issuer, String serial, Set<TrustAnchor> anchors, List<CertStore> stores) throws CertPathValidatorException {
        X509CertSelector sel = new X509CertSelector();
        try {
            sel.setIssuer(new X500Principal(issuer));
            try {
                sel.setSerialNumber(new BigInteger(stripOutSeparators(serial), 16));
                return getResponderCert(sel, (Set) anchors, (List) stores);
            } catch (NumberFormatException e) {
                throw new CertPathValidatorException("cannot parse ocsp.responderCertSerialNumber property", e);
            }
        } catch (IllegalArgumentException e2) {
            throw new CertPathValidatorException("cannot parse ocsp.responderCertIssuerName property", e2);
        }
    }

    private static X509Certificate getResponderCert(X509CertSelector sel, Set<TrustAnchor> anchors, List<CertStore> stores) throws CertPathValidatorException {
        for (TrustAnchor anchor : anchors) {
            X509Certificate cert = anchor.getTrustedCert();
            if (cert != null) {
                if (sel.match(cert)) {
                    return cert;
                }
            }
        }
        for (CertStore store : stores) {
            try {
                Collection<? extends Certificate> certs = store.getCertificates(sel);
                if (!certs.isEmpty()) {
                    return (X509Certificate) certs.iterator().next();
                }
            } catch (CertStoreException e) {
                if (debug != null) {
                    Debug debug = debug;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("CertStore exception:");
                    stringBuilder.append(e);
                    debug.println(stringBuilder.toString());
                }
            }
        }
        throw new CertPathValidatorException("Cannot find the responder's certificate (set using the OCSP security properties).");
    }

    public void init(boolean forward) throws CertPathValidatorException {
        if (forward) {
            throw new CertPathValidatorException("forward checking not supported");
        }
        if (this.anchor != null) {
            PublicKey publicKey;
            this.issuerCert = this.anchor.getTrustedCert();
            if (this.issuerCert != null) {
                publicKey = this.issuerCert.getPublicKey();
            } else {
                publicKey = this.anchor.getCAPublicKey();
            }
            this.prevPubKey = publicKey;
        }
        this.crlSignFlag = true;
        if (this.params == null || this.params.certPath() == null) {
            this.certIndex = -1;
        } else {
            this.certIndex = this.params.certPath().getCertificates().size() - 1;
        }
        this.softFailExceptions.clear();
    }

    public boolean isForwardCheckingSupported() {
        return false;
    }

    public Set<String> getSupportedExtensions() {
        return null;
    }

    public List<CertPathValidatorException> getSoftFailExceptions() {
        return Collections.unmodifiableList(this.softFailExceptions);
    }

    public void check(Certificate cert, Collection<String> unresolvedCritExts) throws CertPathValidatorException {
        check((X509Certificate) cert, unresolvedCritExts, this.prevPubKey, this.crlSignFlag);
    }

    private void check(X509Certificate xcert, Collection<String> unresolvedCritExts, PublicKey pubKey, boolean crlSignFlag) throws CertPathValidatorException {
        CertPathValidatorException cause;
        boolean eSoftFail;
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RevocationChecker.check: checking cert\n  SN: ");
            stringBuilder.append(Debug.toHexString(xcert.getSerialNumber()));
            stringBuilder.append("\n  Subject: ");
            stringBuilder.append(xcert.getSubjectX500Principal());
            stringBuilder.append("\n  Issuer: ");
            stringBuilder.append(xcert.getIssuerX500Principal());
            debug.println(stringBuilder.toString());
        }
        try {
            if (!this.onlyEE || xcert.getBasicConstraints() == -1) {
                switch (this.mode) {
                    case PREFER_OCSP:
                    case ONLY_OCSP:
                        checkOCSP(xcert, unresolvedCritExts);
                        break;
                    case PREFER_CRLS:
                    case ONLY_CRLS:
                        checkCRLs(xcert, unresolvedCritExts, null, pubKey, crlSignFlag);
                        break;
                    default:
                        break;
                }
                updateState(xcert);
                return;
            }
            if (debug != null) {
                debug.println("Skipping revocation check; cert is not an end entity cert");
            }
            updateState(xcert);
        } catch (CertPathValidatorException x) {
            if (debug != null) {
                debug.println("RevocationChecker.check() failover failed");
                Debug debug2 = debug;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("RevocationChecker.check() ");
                stringBuilder2.append(x.getMessage());
                debug2.println(stringBuilder2.toString());
            }
            if (x.getReason() == BasicReason.REVOKED) {
                throw x;
            } else if (!isSoftFailException(x)) {
                cause.addSuppressed(x);
                throw cause;
            } else if (!eSoftFail) {
                throw cause;
            }
        } catch (CertPathValidatorException e) {
            if (e.getReason() != BasicReason.REVOKED) {
                eSoftFail = isSoftFailException(e);
                if (eSoftFail) {
                    if (this.mode == Mode.ONLY_OCSP || this.mode == Mode.ONLY_CRLS) {
                        updateState(xcert);
                        return;
                    }
                } else if (this.mode == Mode.ONLY_OCSP || this.mode == Mode.ONLY_CRLS) {
                    throw e;
                }
                cause = e;
                if (debug != null) {
                    Debug debug3 = debug;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("RevocationChecker.check() ");
                    stringBuilder3.append(e.getMessage());
                    debug3.println(stringBuilder3.toString());
                    debug.println("RevocationChecker.check() preparing to failover");
                }
                int i = AnonymousClass2.$SwitchMap$sun$security$provider$certpath$RevocationChecker$Mode[this.mode.ordinal()];
                if (i == 1) {
                    checkCRLs(xcert, unresolvedCritExts, null, pubKey, crlSignFlag);
                } else if (i == 3) {
                    checkOCSP(xcert, unresolvedCritExts);
                }
            } else {
                throw e;
            }
        } catch (Throwable th) {
            updateState(xcert);
        }
    }

    private boolean isSoftFailException(CertPathValidatorException e) {
        if (!this.softFail || e.getReason() != BasicReason.UNDETERMINED_REVOCATION_STATUS) {
            return false;
        }
        this.softFailExceptions.addFirst(new CertPathValidatorException(e.getMessage(), e.getCause(), this.params.certPath(), this.certIndex, e.getReason()));
        return true;
    }

    private void updateState(X509Certificate cert) throws CertPathValidatorException {
        this.issuerCert = cert;
        PublicKey pubKey = cert.getPublicKey();
        if (PKIX.isDSAPublicKeyWithoutParams(pubKey)) {
            pubKey = BasicChecker.makeInheritedParamsKey(pubKey, this.prevPubKey);
        }
        this.prevPubKey = pubKey;
        this.crlSignFlag = certCanSignCrl(cert);
        if (this.certIndex > 0) {
            this.certIndex--;
        }
    }

    private void checkCRLs(X509Certificate cert, Collection<String> collection, Set<X509Certificate> stackedCerts, PublicKey pubKey, boolean signFlag) throws CertPathValidatorException {
        checkCRLs(cert, pubKey, null, signFlag, true, stackedCerts, this.params.trustAnchors());
    }

    private void checkCRLs(X509Certificate cert, PublicKey prevKey, X509Certificate prevCert, boolean signFlag, boolean allowSeparateKey, Set<X509Certificate> stackedCerts, Set<TrustAnchor> anchors) throws CertPathValidatorException {
        Throwable e;
        PublicKey publicKey;
        boolean z;
        X509CRLSelector x509CRLSelector;
        X509Certificate x509Certificate = cert;
        Set<X509Certificate> set = stackedCerts;
        if (debug != null) {
            debug.println("RevocationChecker.checkCRLs() ---checking revocation status ...");
        }
        if (set == null || !set.contains(x509Certificate)) {
            Debug debug;
            StringBuilder stringBuilder;
            Set possibleCRLs = new HashSet();
            HashSet approvedCRLs = new HashSet();
            X509CRLSelector sel = new X509CRLSelector();
            sel.setCertificateChecking(x509Certificate);
            CertPathHelper.setDateAndTime(sel, this.params.date(), MAX_CLOCK_SKEW);
            CertPathValidatorException networkFailureException = null;
            for (CertStore store : this.certStores) {
                try {
                    for (CRL crl : store.getCRLs(sel)) {
                        possibleCRLs.add((X509CRL) crl);
                    }
                } catch (CertStoreException e2) {
                    if (debug != null) {
                        Debug debug2 = debug;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("RevocationChecker.checkCRLs() CertStoreException: ");
                        stringBuilder2.append(e2.getMessage());
                        debug2.println(stringBuilder2.toString());
                    }
                    if (networkFailureException == null && CertStoreHelper.isCausedByNetworkIssue(store.getType(), e2)) {
                        networkFailureException = new CertPathValidatorException("Unable to determine revocation status due to network error", e2, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                    }
                }
            }
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("RevocationChecker.checkCRLs() possible crls.size() = ");
                stringBuilder.append(possibleCRLs.size());
                debug.println(stringBuilder.toString());
            }
            boolean[] reasonsMask = new boolean[9];
            if (!possibleCRLs.isEmpty()) {
                approvedCRLs.addAll(verifyPossibleCRLs(possibleCRLs, x509Certificate, prevKey, signFlag, reasonsMask, anchors));
            }
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("RevocationChecker.checkCRLs() approved crls.size() = ");
                stringBuilder.append(approvedCRLs.size());
                debug.println(stringBuilder.toString());
            }
            boolean[] reasonsMask2;
            if (approvedCRLs.isEmpty() || !Arrays.equals(reasonsMask, ALL_REASONS)) {
                try {
                    if (this.crlDP) {
                        try {
                            reasonsMask2 = reasonsMask;
                            try {
                                approvedCRLs.addAll(DistributionPointFetcher.getCRLs(sel, signFlag, prevKey, prevCert, this.params.sigProvider(), this.certStores, reasonsMask2, anchors, null));
                            } catch (CertStoreException e3) {
                                e2 = e3;
                                publicKey = prevKey;
                                z = signFlag;
                            }
                        } catch (CertStoreException e4) {
                            e2 = e4;
                            reasonsMask2 = reasonsMask;
                            x509CRLSelector = sel;
                            publicKey = prevKey;
                            z = signFlag;
                            if (e2 instanceof CertStoreTypeException) {
                            }
                            throw new CertPathValidatorException(e2);
                        }
                    }
                    reasonsMask2 = reasonsMask;
                    x509CRLSelector = sel;
                    if (!approvedCRLs.isEmpty() && Arrays.equals(reasonsMask2, ALL_REASONS)) {
                        checkApprovedCRLs(x509Certificate, approvedCRLs);
                    } else if (allowSeparateKey) {
                        try {
                            verifyWithSeparateSigningKey(x509Certificate, prevKey, signFlag, set);
                            return;
                        } catch (CertPathValidatorException cpve) {
                            CertPathValidatorException certPathValidatorException = cpve;
                            if (networkFailureException != null) {
                                throw networkFailureException;
                            }
                            throw cpve;
                        }
                    } else {
                        publicKey = prevKey;
                        z = signFlag;
                        if (networkFailureException != null) {
                            throw networkFailureException;
                        }
                        throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                    }
                } catch (CertStoreException e5) {
                    e2 = e5;
                    publicKey = prevKey;
                    z = signFlag;
                    reasonsMask2 = reasonsMask;
                    x509CRLSelector = sel;
                    if ((e2 instanceof CertStoreTypeException) || !CertStoreHelper.isCausedByNetworkIssue(((CertStoreTypeException) e2).getType(), e2)) {
                        throw new CertPathValidatorException(e2);
                    }
                    throw new CertPathValidatorException("Unable to determine revocation status due to network error", e2, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                }
            }
            checkApprovedCRLs(x509Certificate, approvedCRLs);
            reasonsMask2 = reasonsMask;
            x509CRLSelector = sel;
            return;
        }
        if (debug != null) {
            debug.println("RevocationChecker.checkCRLs() circular dependency");
        }
        throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
    }

    private void checkApprovedCRLs(X509Certificate cert, Set<X509CRL> approvedCRLs) throws CertPathValidatorException {
        if (debug != null) {
            BigInteger sn = cert.getSerialNumber();
            debug.println("RevocationChecker.checkApprovedCRLs() starting the final sweep...");
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RevocationChecker.checkApprovedCRLs() cert SN: ");
            stringBuilder.append(sn.toString());
            debug.println(stringBuilder.toString());
        }
        CRLReason reasonCode = CRLReason.UNSPECIFIED;
        X509CRLEntryImpl entry = null;
        for (X509CRL crl : approvedCRLs) {
            X509CRLEntry e = crl.getRevokedCertificate(cert);
            if (e != null) {
                Throwable t;
                try {
                    X509CRLEntryImpl entry2 = X509CRLEntryImpl.toImpl(e);
                    if (debug != null) {
                        Debug debug2 = debug;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("RevocationChecker.checkApprovedCRLs() CRL entry: ");
                        stringBuilder2.append(entry2.toString());
                        debug2.println(stringBuilder2.toString());
                    }
                    Set<String> unresCritExts = entry2.getCriticalExtensionOIDs();
                    if (!(unresCritExts == null || unresCritExts.isEmpty())) {
                        unresCritExts.remove(PKIXExtensions.ReasonCode_Id.toString());
                        unresCritExts.remove(PKIXExtensions.CertificateIssuer_Id.toString());
                        if (!unresCritExts.isEmpty()) {
                            throw new CertPathValidatorException("Unrecognized critical extension(s) in revoked CRL entry");
                        }
                    }
                    CRLReason reasonCode2 = entry2.getRevocationReason();
                    if (reasonCode2 == null) {
                        reasonCode2 = CRLReason.UNSPECIFIED;
                    }
                    Date revocationDate = entry2.getRevocationDate();
                    if (revocationDate.before(this.params.date())) {
                        t = new CertificateRevokedException(revocationDate, reasonCode2, crl.getIssuerX500Principal(), entry2.getExtensions());
                        throw new CertPathValidatorException(t.getMessage(), t, null, -1, BasicReason.REVOKED);
                    }
                } catch (CRLException ce) {
                    t = ce;
                    throw new CertPathValidatorException(ce);
                }
            }
        }
        X509Certificate x509Certificate = cert;
    }

    private void checkOCSP(X509Certificate cert, Collection<String> collection) throws CertPathValidatorException {
        IOException e;
        IOException e2;
        X509Certificate x509Certificate;
        X509CertImpl currCert = null;
        try {
            currCert = X509CertImpl.toImpl(cert);
            CertId certId = null;
            try {
                if (this.issuerCert != null) {
                    certId = new CertId(this.issuerCert, currCert.getSerialNumberObject());
                } else {
                    certId = new CertId(this.anchor.getCA(), this.anchor.getCAPublicKey(), currCert.getSerialNumberObject());
                }
                try {
                    OCSPResponse response;
                    OCSPResponse oCSPResponse;
                    byte[] responseBytes = (byte[]) this.ocspResponses.get(cert);
                    if (responseBytes != null) {
                        if (debug != null) {
                            debug.println("Found cached OCSP response");
                        }
                        OCSPResponse response2 = new OCSPResponse(responseBytes);
                        byte[] nonce = null;
                        try {
                            for (Extension ext : this.ocspExtensions) {
                                if (ext.getId().equals("1.3.6.1.5.5.7.48.1.2")) {
                                    nonce = ext.getValue();
                                }
                            }
                            response2.verify(Collections.singletonList(certId), this.issuerCert, this.responderCert, this.params.date(), nonce);
                            response = response2;
                        } catch (IOException e22) {
                            e = e22;
                            oCSPResponse = response2;
                            throw new CertPathValidatorException("Unable to determine revocation status due to network error", e, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                        }
                    }
                    URI responderURI;
                    if (this.responderURI != null) {
                        responderURI = this.responderURI;
                    } else {
                        responderURI = OCSP.getResponderURI(currCert);
                    }
                    if (responderURI != null) {
                        response = OCSP.check(Collections.singletonList(certId), responderURI, this.issuerCert, this.responderCert, null, this.ocspExtensions);
                    } else {
                        throw new CertPathValidatorException("Certificate does not specify OCSP responder", null, null, -1);
                    }
                    oCSPResponse = response.getSingleResponse(certId);
                    responseBytes = oCSPResponse.getCertStatus();
                    if (responseBytes == CertStatus.REVOKED) {
                        Date revocationTime = oCSPResponse.getRevocationTime();
                        if (revocationTime.before(this.params.date())) {
                            Throwable t = new CertificateRevokedException(revocationTime, oCSPResponse.getRevocationReason(), response.getSignerCertificate().getSubjectX500Principal(), oCSPResponse.getSingleExtensions());
                            throw new CertPathValidatorException(t.getMessage(), t, null, -1, BasicReason.REVOKED);
                        }
                    } else if (responseBytes == CertStatus.UNKNOWN) {
                        throw new CertPathValidatorException("Certificate's revocation status is unknown", null, this.params.certPath(), -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                    }
                } catch (IOException e3) {
                    e22 = e3;
                    e = e22;
                    throw new CertPathValidatorException("Unable to determine revocation status due to network error", e, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                }
            } catch (IOException e4) {
                e22 = e4;
                x509Certificate = cert;
                e = e22;
                throw new CertPathValidatorException("Unable to determine revocation status due to network error", e, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
            }
        } catch (CertificateException ce) {
            x509Certificate = cert;
            Throwable th = ce;
            throw new CertPathValidatorException(ce);
        }
    }

    private static String stripOutSeparators(String value) {
        char[] chars = value.toCharArray();
        StringBuilder hexNumber = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (HEX_DIGITS.indexOf(chars[i]) != -1) {
                hexNumber.append(chars[i]);
            }
        }
        return hexNumber.toString();
    }

    static boolean certCanSignCrl(X509Certificate cert) {
        boolean[] keyUsage = cert.getKeyUsage();
        if (keyUsage != null) {
            return keyUsage[6];
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:32:0x00d0  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Collection<X509CRL> verifyPossibleCRLs(Set<X509CRL> crls, X509Certificate cert, PublicKey prevKey, boolean signFlag, boolean[] reasonsMask, Set<TrustAnchor> anchors) throws CertPathValidatorException {
        Exception e;
        Debug debug;
        StringBuilder stringBuilder;
        boolean[] zArr;
        try {
            List<DistributionPoint> points;
            X509CertImpl certImpl = X509CertImpl.toImpl(cert);
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("RevocationChecker.verifyPossibleCRLs: Checking CRLDPs for ");
                stringBuilder.append(certImpl.getSubjectX500Principal());
                debug.println(stringBuilder.toString());
            }
            CRLDistributionPointsExtension ext = certImpl.getCRLDistributionPointsExtension();
            if (ext == null) {
                points = Collections.singletonList(new DistributionPoint(new GeneralNames().add(new GeneralName((X500Name) certImpl.getIssuerDN())), null, null));
            } else {
                points = ext.get(CRLDistributionPointsExtension.POINTS);
            }
            List<DistributionPoint> points2 = points;
            HashSet results = new HashSet();
            Iterator it = points2.iterator();
            while (it.hasNext()) {
                Iterator it2;
                DistributionPoint point = (DistributionPoint) it.next();
                Iterator it3 = crls.iterator();
                while (it3.hasNext()) {
                    X509CRL crl = (X509CRL) it3.next();
                    String sigProvider = this.params.sigProvider();
                    List list = this.certStores;
                    X509CRL crl2 = crl;
                    Iterator it4 = it3;
                    it2 = it;
                    if (DistributionPointFetcher.verifyCRL(certImpl, point, crl, reasonsMask, signFlag, prevKey, null, sigProvider, anchors, list, this.params.date())) {
                        results.add(crl2);
                    }
                    it = it2;
                    it3 = it4;
                }
                it2 = it;
                try {
                    if (Arrays.equals(reasonsMask, ALL_REASONS)) {
                        break;
                    }
                    it = it2;
                } catch (IOException | CRLException | CertificateException e2) {
                    e = e2;
                    if (debug != null) {
                    }
                    return Collections.emptySet();
                }
            }
            zArr = reasonsMask;
            return results;
        } catch (IOException | CRLException | CertificateException e3) {
            e = e3;
            zArr = reasonsMask;
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while verifying CRL: ");
                stringBuilder.append(e.getMessage());
                debug.println(stringBuilder.toString());
                e.printStackTrace();
            }
            return Collections.emptySet();
        }
    }

    private void verifyWithSeparateSigningKey(X509Certificate cert, PublicKey prevKey, boolean signFlag, Set<X509Certificate> stackedCerts) throws CertPathValidatorException {
        String msg = "revocation status";
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RevocationChecker.verifyWithSeparateSigningKey() ---checking ");
            stringBuilder.append(msg);
            stringBuilder.append("...");
            debug.println(stringBuilder.toString());
        }
        if (stackedCerts != null && stackedCerts.contains(cert)) {
            if (debug != null) {
                debug.println("RevocationChecker.verifyWithSeparateSigningKey() circular dependency");
            }
            throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
        } else if (signFlag) {
            buildToNewKey(cert, prevKey, stackedCerts);
        } else {
            buildToNewKey(cert, null, stackedCerts);
        }
    }

    private void buildToNewKey(X509Certificate currCert, PublicKey prevKey, Set<X509Certificate> stackedCerts) throws CertPathValidatorException {
        Set<TrustAnchor> trustAnchors;
        Throwable iape;
        Set<X509Certificate> set;
        PKIXCertPathBuilderResult cpbr;
        CertPathBuilder builder;
        int i;
        int i2;
        int i3;
        PublicKey newKey;
        CertPathValidatorException cpve;
        RevocationChecker revocationChecker = this;
        PublicKey publicKey = prevKey;
        if (debug != null) {
            debug.println("RevocationChecker.buildToNewKey() starting work");
        }
        HashSet badKeys = new HashSet();
        if (publicKey != null) {
            badKeys.add(publicKey);
        }
        CertSelector certSel = new RejectKeySelector(badKeys);
        certSel.setSubject(currCert.getIssuerX500Principal());
        certSel.setKeyUsage(CRL_SIGN_USAGE);
        if (revocationChecker.anchor == null) {
            trustAnchors = revocationChecker.params.trustAnchors();
        } else {
            trustAnchors = Collections.singleton(revocationChecker.anchor);
        }
        Set newAnchors = trustAnchors;
        try {
            PKIXBuilderParameters builderParams = new PKIXBuilderParameters(newAnchors, certSel);
            builderParams.setInitialPolicies(revocationChecker.params.initialPolicies());
            builderParams.setCertStores(revocationChecker.certStores);
            builderParams.setExplicitPolicyRequired(revocationChecker.params.explicitPolicyRequired());
            builderParams.setPolicyMappingInhibited(revocationChecker.params.policyMappingInhibited());
            builderParams.setAnyPolicyInhibited(revocationChecker.params.anyPolicyInhibited());
            builderParams.setDate(revocationChecker.params.date());
            builderParams.setCertPathCheckers(revocationChecker.params.getPKIXParameters().getCertPathCheckers());
            builderParams.setSigProvider(revocationChecker.params.sigProvider());
            builderParams.setRevocationEnabled(false);
            int i4 = 1;
            if (Builder.USE_AIA) {
                X509CertImpl currCertImpl = null;
                try {
                    currCertImpl = X509CertImpl.toImpl(currCert);
                } catch (CertificateException ce) {
                    CertificateException certificateException = ce;
                    if (debug != null) {
                        Debug debug = debug;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("RevocationChecker.buildToNewKey: error decoding cert: ");
                        stringBuilder.append(ce);
                        debug.println(stringBuilder.toString());
                    }
                }
                AuthorityInfoAccessExtension aiaExt = null;
                if (currCertImpl != null) {
                    aiaExt = currCertImpl.getAuthorityInfoAccessExtension();
                }
                if (aiaExt != null) {
                    List<AccessDescription> adList = aiaExt.getAccessDescriptions();
                    if (adList != null) {
                        for (AccessDescription ad : adList) {
                            CertStore cs = URICertStore.getInstance(ad);
                            if (cs != null) {
                                if (debug != null) {
                                    debug.println("adding AIAext CertStore");
                                }
                                builderParams.addCertStore(cs);
                            }
                        }
                    }
                }
            }
            CertPathBuilder builder2 = null;
            try {
                CertPathBuilder builder3 = CertPathBuilder.getInstance("PKIX");
                Set<X509Certificate> stackedCerts2 = stackedCerts;
                while (true) {
                    try {
                        Set<X509Certificate> stackedCerts3;
                        if (debug != null) {
                            try {
                                debug.println("RevocationChecker.buildToNewKey() about to try build ...");
                            } catch (InvalidAlgorithmParameterException e) {
                                iape = e;
                                set = stackedCerts2;
                            } catch (CertPathBuilderException e2) {
                                throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                            }
                        }
                        PKIXCertPathBuilderResult cpbr2 = (PKIXCertPathBuilderResult) builder3.build(builderParams);
                        if (debug != null) {
                            debug.println("RevocationChecker.buildToNewKey() about to check revocation ...");
                        }
                        if (stackedCerts2 == null) {
                            stackedCerts3 = new HashSet();
                        } else {
                            stackedCerts3 = stackedCerts2;
                        }
                        try {
                            PublicKey prevKey2;
                            stackedCerts3.add(currCert);
                            TrustAnchor ta = cpbr2.getTrustAnchor();
                            PublicKey prevKey22 = ta.getCAPublicKey();
                            if (prevKey22 == null) {
                                try {
                                    prevKey2 = ta.getTrustedCert().getPublicKey();
                                } catch (InvalidAlgorithmParameterException e3) {
                                    iape = e3;
                                    set = stackedCerts3;
                                    throw new CertPathValidatorException(iape);
                                } catch (CertPathBuilderException e4) {
                                    set = stackedCerts3;
                                    throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                                }
                            }
                            prevKey2 = prevKey22;
                            List<? extends Certificate> cpList = cpbr2.getCertPath().getCertificates();
                            PublicKey prevKey23;
                            boolean signFlag;
                            TrustAnchor ta2;
                            try {
                                X509Certificate x509Certificate;
                                PublicKey newKey2;
                                int i5;
                                int i6 = cpList.size() - i4;
                                prevKey23 = prevKey2;
                                signFlag = true;
                                while (i6 >= 0) {
                                    try {
                                        X509Certificate x509Certificate2 = (X509Certificate) cpList.get(i6);
                                        if (debug != null) {
                                            try {
                                                Debug debug2 = debug;
                                                try {
                                                    StringBuilder stringBuilder2 = new StringBuilder();
                                                    ta2 = ta;
                                                    try {
                                                        stringBuilder2.append("RevocationChecker.buildToNewKey() index ");
                                                        stringBuilder2.append(i6);
                                                        stringBuilder2.append(" checking ");
                                                        stringBuilder2.append((Object) x509Certificate2);
                                                        debug2.println(stringBuilder2.toString());
                                                    } catch (CertPathValidatorException e5) {
                                                        set = stackedCerts3;
                                                        cpbr = cpbr2;
                                                        builder = builder3;
                                                        i = 1;
                                                    }
                                                } catch (CertPathValidatorException e6) {
                                                    ta2 = ta;
                                                    set = stackedCerts3;
                                                    cpbr = cpbr2;
                                                    builder = builder3;
                                                    i = 1;
                                                    i2 = 0;
                                                    badKeys.add(cpbr.getPublicKey());
                                                    i4 = i;
                                                    builder3 = builder;
                                                    i3 = i2;
                                                    stackedCerts2 = set;
                                                    revocationChecker = this;
                                                    publicKey = prevKey;
                                                }
                                            } catch (CertPathValidatorException e7) {
                                                ta2 = ta;
                                                set = stackedCerts3;
                                                cpbr = cpbr2;
                                                builder = builder3;
                                                i = i4;
                                                i2 = 0;
                                                badKeys.add(cpbr.getPublicKey());
                                                i4 = i;
                                                builder3 = builder;
                                                i3 = i2;
                                                stackedCerts2 = set;
                                                revocationChecker = this;
                                                publicKey = prevKey;
                                            }
                                        } else {
                                            ta2 = ta;
                                        }
                                        X509Certificate cert = x509Certificate2;
                                        set = stackedCerts3;
                                        builder = builder3;
                                        cpbr = cpbr2;
                                        i = 1;
                                        try {
                                            revocationChecker.checkCRLs(x509Certificate2, prevKey23, null, signFlag, true, set, newAnchors);
                                            X509Certificate cert2 = cert;
                                            signFlag = certCanSignCrl(cert2);
                                            prevKey23 = cert2.getPublicKey();
                                            i6--;
                                            x509Certificate = currCert;
                                            cpbr2 = cpbr;
                                            i4 = 1;
                                            ta = ta2;
                                            builder3 = builder;
                                            stackedCerts3 = set;
                                            publicKey = prevKey;
                                        } catch (CertPathValidatorException e8) {
                                            i2 = 0;
                                            badKeys.add(cpbr.getPublicKey());
                                            i4 = i;
                                            builder3 = builder;
                                            i3 = i2;
                                            stackedCerts2 = set;
                                            revocationChecker = this;
                                            publicKey = prevKey;
                                        }
                                    } catch (CertPathValidatorException e9) {
                                        ta2 = ta;
                                        set = stackedCerts3;
                                        cpbr = cpbr2;
                                        builder = builder3;
                                        i = i4;
                                        i2 = 0;
                                        badKeys.add(cpbr.getPublicKey());
                                        i4 = i;
                                        builder3 = builder;
                                        i3 = i2;
                                        stackedCerts2 = set;
                                        revocationChecker = this;
                                        publicKey = prevKey;
                                    }
                                }
                                ta2 = ta;
                                set = stackedCerts3;
                                cpbr = cpbr2;
                                builder = builder3;
                                i = i4;
                                try {
                                    if (debug != null) {
                                        Debug debug3 = debug;
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("RevocationChecker.buildToNewKey() got key ");
                                        stringBuilder3.append(cpbr.getPublicKey());
                                        debug3.println(stringBuilder3.toString());
                                    }
                                    newKey2 = cpbr.getPublicKey();
                                    if (cpList.isEmpty()) {
                                        x509Certificate = null;
                                        i5 = 0;
                                    } else {
                                        i5 = 0;
                                        x509Certificate = (X509Certificate) cpList.get(0);
                                    }
                                } catch (InvalidAlgorithmParameterException e10) {
                                    iape = e10;
                                    throw new CertPathValidatorException(iape);
                                } catch (CertPathBuilderException e11) {
                                    throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                                }
                                try {
                                    Set trustAnchors2 = revocationChecker.params.trustAnchors();
                                    RevocationChecker revocationChecker2 = revocationChecker;
                                    i2 = i5;
                                    newKey = newKey2;
                                    try {
                                        revocationChecker2.checkCRLs(currCert, newKey2, x509Certificate, true, false, null, trustAnchors2);
                                        return;
                                    } catch (CertPathValidatorException e12) {
                                        cpve = e12;
                                    }
                                } catch (CertPathValidatorException e13) {
                                    cpve = e13;
                                    i2 = i5;
                                    newKey = newKey2;
                                    if (cpve.getReason() != BasicReason.REVOKED) {
                                        badKeys.add(newKey);
                                        i4 = i;
                                        builder3 = builder;
                                        i3 = i2;
                                        stackedCerts2 = set;
                                        revocationChecker = this;
                                        publicKey = prevKey;
                                    } else {
                                        throw cpve;
                                    }
                                }
                            } catch (CertPathValidatorException e14) {
                                ta2 = ta;
                                set = stackedCerts3;
                                cpbr = cpbr2;
                                builder = builder3;
                                i = i4;
                                i2 = 0;
                                prevKey23 = prevKey2;
                                signFlag = true;
                                badKeys.add(cpbr.getPublicKey());
                                i4 = i;
                                builder3 = builder;
                                i3 = i2;
                                stackedCerts2 = set;
                                revocationChecker = this;
                                publicKey = prevKey;
                            }
                            i4 = i;
                            builder3 = builder;
                            i3 = i2;
                            stackedCerts2 = set;
                            revocationChecker = this;
                            publicKey = prevKey;
                        } catch (InvalidAlgorithmParameterException e15) {
                            iape = e15;
                            set = stackedCerts3;
                            builder = builder3;
                            throw new CertPathValidatorException(iape);
                        } catch (CertPathBuilderException e16) {
                            set = stackedCerts3;
                            builder = builder3;
                            throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                        }
                    } catch (InvalidAlgorithmParameterException e17) {
                        iape = e17;
                        builder = builder3;
                        set = stackedCerts2;
                        throw new CertPathValidatorException(iape);
                    } catch (CertPathBuilderException e18) {
                        builder = builder3;
                        set = stackedCerts2;
                        throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                    }
                }
            } catch (NoSuchAlgorithmException iape2) {
                throw new CertPathValidatorException(iape2);
            }
        } catch (InvalidAlgorithmParameterException iape22) {
            throw new RuntimeException(iape22);
        }
    }

    public RevocationChecker clone() {
        RevocationChecker copy = (RevocationChecker) super.clone();
        copy.softFailExceptions = new LinkedList(this.softFailExceptions);
        return copy;
    }
}
