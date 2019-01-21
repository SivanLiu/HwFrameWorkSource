package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.x509.AccessDescription;
import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;

class ForwardBuilder extends Builder {
    private static final Debug debug = Debug.getInstance("certpath");
    private AdaptableX509CertSelector caSelector;
    private X509CertSelector caTargetSelector;
    private X509CertSelector eeSelector;
    private boolean searchAllCertStores = true;
    TrustAnchor trustAnchor;
    private final Set<TrustAnchor> trustAnchors;
    private final Set<X509Certificate> trustedCerts;
    private final Set<X500Principal> trustedSubjectDNs;

    static class PKIXCertComparator implements Comparator<X509Certificate> {
        static final String METHOD_NME = "PKIXCertComparator.compare()";
        private final X509CertSelector certSkidSelector;
        private final Set<X500Principal> trustedSubjectDNs;

        PKIXCertComparator(Set<X500Principal> trustedSubjectDNs, X509CertImpl previousCert) throws IOException {
            this.trustedSubjectDNs = trustedSubjectDNs;
            this.certSkidSelector = getSelector(previousCert);
        }

        private X509CertSelector getSelector(X509CertImpl previousCert) throws IOException {
            if (previousCert != null) {
                AuthorityKeyIdentifierExtension akidExt = previousCert.getAuthorityKeyIdentifierExtension();
                if (akidExt != null) {
                    byte[] skid = akidExt.getEncodedKeyIdentifier();
                    if (skid != null) {
                        X509CertSelector selector = new X509CertSelector();
                        selector.setSubjectKeyIdentifier(skid);
                        return selector;
                    }
                }
            }
            return null;
        }

        public int compare(X509Certificate oCert1, X509Certificate oCert2) {
            if (oCert1.equals(oCert2)) {
                return 0;
            }
            Debug access$000;
            StringBuilder stringBuilder;
            int i = -1;
            if (this.certSkidSelector == null) {
                X509Certificate x509Certificate = oCert1;
                X509Certificate x509Certificate2 = oCert2;
            } else if (this.certSkidSelector.match(oCert1)) {
                return -1;
            } else {
                if (this.certSkidSelector.match(oCert2)) {
                    return 1;
                }
            }
            X500Principal cIssuer1 = oCert1.getIssuerX500Principal();
            Object cIssuer2 = oCert2.getIssuerX500Principal();
            X500Name cIssuer1Name = X500Name.asX500Name(cIssuer1);
            X500Name cIssuer2Name = X500Name.asX500Name(cIssuer2);
            if (ForwardBuilder.debug != null) {
                Debug access$0002 = ForwardBuilder.debug;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PKIXCertComparator.compare() o1 Issuer:  ");
                stringBuilder2.append((Object) cIssuer1);
                access$0002.println(stringBuilder2.toString());
                access$0002 = ForwardBuilder.debug;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PKIXCertComparator.compare() o2 Issuer:  ");
                stringBuilder2.append(cIssuer2);
                access$0002.println(stringBuilder2.toString());
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() MATCH TRUSTED SUBJECT TEST...");
            }
            boolean m1 = this.trustedSubjectDNs.contains(cIssuer1);
            boolean m2 = this.trustedSubjectDNs.contains(cIssuer2);
            if (ForwardBuilder.debug != null) {
                access$000 = ForwardBuilder.debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PKIXCertComparator.compare() m1: ");
                stringBuilder.append(m1);
                access$000.println(stringBuilder.toString());
                access$000 = ForwardBuilder.debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PKIXCertComparator.compare() m2: ");
                stringBuilder.append(m2);
                access$000.println(stringBuilder.toString());
            }
            if ((m1 && m2) || m1) {
                return -1;
            }
            if (m2) {
                return 1;
            }
            int distanceTto1;
            int distanceTto2;
            X500Name tSubjectName;
            int distanceTto12;
            Debug access$0003;
            StringBuilder stringBuilder3;
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() NAMING DESCENDANT TEST...");
            }
            for (X500Principal tSubject : this.trustedSubjectDNs) {
                X500Principal cIssuer12;
                X500Name tSubjectName2 = X500Name.asX500Name(tSubject);
                distanceTto1 = Builder.distance(tSubjectName2, cIssuer1Name, i);
                distanceTto2 = Builder.distance(tSubjectName2, cIssuer2Name, i);
                if (ForwardBuilder.debug != null) {
                    Debug access$0004 = ForwardBuilder.debug;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    cIssuer12 = cIssuer1;
                    stringBuilder4.append("PKIXCertComparator.compare() distanceTto1: ");
                    stringBuilder4.append(distanceTto1);
                    access$0004.println(stringBuilder4.toString());
                    Debug access$0005 = ForwardBuilder.debug;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("PKIXCertComparator.compare() distanceTto2: ");
                    stringBuilder5.append(distanceTto2);
                    access$0005.println(stringBuilder5.toString());
                } else {
                    cIssuer12 = cIssuer1;
                }
                if (distanceTto1 <= 0 && distanceTto2 <= 0) {
                    cIssuer1 = cIssuer12;
                    i = -1;
                } else if (distanceTto1 == distanceTto2) {
                    return -1;
                } else {
                    if (distanceTto1 > 0 && distanceTto2 <= 0) {
                        return -1;
                    }
                    if (distanceTto1 <= 0 && distanceTto2 > 0) {
                        return 1;
                    }
                    if (distanceTto1 < distanceTto2) {
                        return -1;
                    }
                    return 1;
                }
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() NAMING ANCESTOR TEST...");
            }
            for (X500Principal tSubject2 : this.trustedSubjectDNs) {
                tSubjectName = X500Name.asX500Name(tSubject2);
                distanceTto12 = Builder.distance(tSubjectName, cIssuer1Name, Integer.MAX_VALUE);
                i = Builder.distance(tSubjectName, cIssuer2Name, Integer.MAX_VALUE);
                if (ForwardBuilder.debug != null) {
                    access$0003 = ForwardBuilder.debug;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("PKIXCertComparator.compare() distanceTto1: ");
                    stringBuilder3.append(distanceTto12);
                    access$0003.println(stringBuilder3.toString());
                    access$0003 = ForwardBuilder.debug;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("PKIXCertComparator.compare() distanceTto2: ");
                    stringBuilder3.append(i);
                    access$0003.println(stringBuilder3.toString());
                }
                if (distanceTto12 < 0 || i < 0) {
                    if (distanceTto12 == i) {
                        return -1;
                    }
                    if (distanceTto12 < 0 && i >= 0) {
                        return -1;
                    }
                    if (distanceTto12 >= 0 && i < 0) {
                        return 1;
                    }
                    if (distanceTto12 > i) {
                        return -1;
                    }
                    return 1;
                }
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() SAME NAMESPACE AS TRUSTED TEST...");
            }
            Iterator it = this.trustedSubjectDNs.iterator();
            while (it.hasNext()) {
                Debug access$0006;
                Iterator it2;
                tSubjectName = X500Name.asX500Name((X500Principal) it.next());
                Object tAo1 = tSubjectName.commonAncestor(cIssuer1Name);
                Object tAo2 = tSubjectName.commonAncestor(cIssuer2Name);
                if (ForwardBuilder.debug != null) {
                    Debug access$0007 = ForwardBuilder.debug;
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("PKIXCertComparator.compare() tAo1: ");
                    stringBuilder6.append(String.valueOf(tAo1));
                    access$0007.println(stringBuilder6.toString());
                    access$0006 = ForwardBuilder.debug;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("PKIXCertComparator.compare() tAo2: ");
                    stringBuilder3.append(String.valueOf(tAo2));
                    access$0006.println(stringBuilder3.toString());
                }
                if (tAo1 == null && tAo2 == null) {
                    it2 = it;
                } else if (tAo1 == null || tAo2 == null) {
                    return tAo1 == null ? 1 : -1;
                } else {
                    distanceTto1 = Builder.hops(tSubjectName, cIssuer1Name, Integer.MAX_VALUE);
                    distanceTto2 = Builder.hops(tSubjectName, cIssuer2Name, Integer.MAX_VALUE);
                    if (ForwardBuilder.debug != null) {
                        access$0006 = ForwardBuilder.debug;
                        StringBuilder stringBuilder7 = new StringBuilder();
                        it2 = it;
                        stringBuilder7.append("PKIXCertComparator.compare() hopsTto1: ");
                        stringBuilder7.append(distanceTto1);
                        access$0006.println(stringBuilder7.toString());
                        Debug access$0008 = ForwardBuilder.debug;
                        StringBuilder stringBuilder8 = new StringBuilder();
                        stringBuilder8.append("PKIXCertComparator.compare() hopsTto2: ");
                        stringBuilder8.append(distanceTto2);
                        access$0008.println(stringBuilder8.toString());
                    } else {
                        it2 = it;
                    }
                    if (distanceTto1 != distanceTto2) {
                        if (distanceTto1 > distanceTto2) {
                            return 1;
                        }
                        return -1;
                    }
                }
                it = it2;
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() CERT ISSUER/SUBJECT COMPARISON TEST...");
            }
            Object cSubject1 = oCert1.getSubjectX500Principal();
            Object cSubject2 = oCert2.getSubjectX500Principal();
            X500Name cSubject1Name = X500Name.asX500Name(cSubject1);
            X500Name cSubject2Name = X500Name.asX500Name(cSubject2);
            if (ForwardBuilder.debug != null) {
                access$000 = ForwardBuilder.debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PKIXCertComparator.compare() o1 Subject: ");
                stringBuilder.append(cSubject1);
                access$000.println(stringBuilder.toString());
                access$000 = ForwardBuilder.debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PKIXCertComparator.compare() o2 Subject: ");
                stringBuilder.append(cSubject2);
                access$000.println(stringBuilder.toString());
            }
            distanceTto12 = Builder.distance(cSubject1Name, cIssuer1Name, Integer.MAX_VALUE);
            int distanceStoI2 = Builder.distance(cSubject2Name, cIssuer2Name, Integer.MAX_VALUE);
            if (ForwardBuilder.debug != null) {
                access$0003 = ForwardBuilder.debug;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("PKIXCertComparator.compare() distanceStoI1: ");
                stringBuilder3.append(distanceTto12);
                access$0003.println(stringBuilder3.toString());
                access$0003 = ForwardBuilder.debug;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("PKIXCertComparator.compare() distanceStoI2: ");
                stringBuilder3.append(distanceStoI2);
                access$0003.println(stringBuilder3.toString());
            }
            if (distanceStoI2 > distanceTto12) {
                return -1;
            }
            if (distanceStoI2 < distanceTto12) {
                return 1;
            }
            if (ForwardBuilder.debug != null) {
                ForwardBuilder.debug.println("PKIXCertComparator.compare() no tests matched; RETURN 0");
            }
            return -1;
        }
    }

    ForwardBuilder(BuilderParams buildParams, boolean searchAllCertStores) {
        super(buildParams);
        this.trustAnchors = buildParams.trustAnchors();
        this.trustedCerts = new HashSet(this.trustAnchors.size());
        this.trustedSubjectDNs = new HashSet(this.trustAnchors.size());
        for (TrustAnchor anchor : this.trustAnchors) {
            X509Certificate trustedCert = anchor.getTrustedCert();
            if (trustedCert != null) {
                this.trustedCerts.add(trustedCert);
                this.trustedSubjectDNs.add(trustedCert.getSubjectX500Principal());
            } else {
                this.trustedSubjectDNs.add(anchor.getCA());
            }
        }
        this.searchAllCertStores = searchAllCertStores;
    }

    Collection<X509Certificate> getMatchingCerts(State currentState, List<CertStore> certStores) throws CertStoreException, CertificateException, IOException {
        if (debug != null) {
            debug.println("ForwardBuilder.getMatchingCerts()...");
        }
        ForwardState currState = (ForwardState) currentState;
        Set<X509Certificate> certs = new TreeSet(new PKIXCertComparator(this.trustedSubjectDNs, currState.cert));
        if (currState.isInitial()) {
            getMatchingEECerts(currState, certStores, certs);
        }
        getMatchingCACerts(currState, certStores, certs);
        return certs;
    }

    private void getMatchingEECerts(ForwardState currentState, List<CertStore> certStores, Collection<X509Certificate> eeCerts) throws IOException {
        if (debug != null) {
            debug.println("ForwardBuilder.getMatchingEECerts()...");
        }
        if (this.eeSelector == null) {
            this.eeSelector = (X509CertSelector) this.targetCertConstraints.clone();
            this.eeSelector.setCertificateValid(this.buildParams.date());
            if (this.buildParams.explicitPolicyRequired()) {
                this.eeSelector.setPolicy(getMatchingPolicies());
            }
            this.eeSelector.setBasicConstraints(-2);
        }
        addMatchingCerts(this.eeSelector, certStores, eeCerts, this.searchAllCertStores);
    }

    private void getMatchingCACerts(ForwardState currentState, List<CertStore> certStores, Collection<X509Certificate> caCerts) throws IOException {
        X509CertSelector sel;
        if (debug != null) {
            debug.println("ForwardBuilder.getMatchingCACerts()...");
        }
        int initialSize = caCerts.size();
        if (!currentState.isInitial()) {
            if (this.caSelector == null) {
                this.caSelector = new AdaptableX509CertSelector();
                if (this.buildParams.explicitPolicyRequired()) {
                    this.caSelector.setPolicy(getMatchingPolicies());
                }
            }
            this.caSelector.setSubject(currentState.issuerDN);
            CertPathHelper.setPathToNames(this.caSelector, currentState.subjectNamesTraversed);
            this.caSelector.setValidityPeriod(currentState.cert.getNotBefore(), currentState.cert.getNotAfter());
            sel = this.caSelector;
        } else if (this.targetCertConstraints.getBasicConstraints() != -2) {
            if (debug != null) {
                debug.println("ForwardBuilder.getMatchingCACerts(): the target is a CA");
            }
            if (this.caTargetSelector == null) {
                this.caTargetSelector = (X509CertSelector) this.targetCertConstraints.clone();
                if (this.buildParams.explicitPolicyRequired()) {
                    this.caTargetSelector.setPolicy(getMatchingPolicies());
                }
            }
            sel = this.caTargetSelector;
        } else {
            return;
        }
        sel.setBasicConstraints(-1);
        for (X509Certificate trustedCert : this.trustedCerts) {
            if (sel.match(trustedCert)) {
                if (debug != null) {
                    Debug debug = debug;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ForwardBuilder.getMatchingCACerts: found matching trust anchor.\n  SN: ");
                    stringBuilder.append(Debug.toHexString(trustedCert.getSerialNumber()));
                    stringBuilder.append("\n  Subject: ");
                    stringBuilder.append(trustedCert.getSubjectX500Principal());
                    stringBuilder.append("\n  Issuer: ");
                    stringBuilder.append(trustedCert.getIssuerX500Principal());
                    debug.println(stringBuilder.toString());
                }
                if (caCerts.add(trustedCert) && !this.searchAllCertStores) {
                    return;
                }
            }
        }
        sel.setCertificateValid(this.buildParams.date());
        sel.setBasicConstraints(currentState.traversedCACerts);
        if ((!currentState.isInitial() && this.buildParams.maxPathLength() != -1 && this.buildParams.maxPathLength() <= currentState.traversedCACerts) || !addMatchingCerts(sel, certStores, caCerts, this.searchAllCertStores) || this.searchAllCertStores) {
            if (!currentState.isInitial() && Builder.USE_AIA) {
                AuthorityInfoAccessExtension aiaExt = currentState.cert.getAuthorityInfoAccessExtension();
                if (aiaExt != null) {
                    getCerts(aiaExt, caCerts);
                }
            }
            if (debug != null) {
                int numCerts = caCerts.size() - initialSize;
                Debug debug2 = debug;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ForwardBuilder.getMatchingCACerts: found ");
                stringBuilder2.append(numCerts);
                stringBuilder2.append(" CA certs");
                debug2.println(stringBuilder2.toString());
            }
        }
    }

    private boolean getCerts(AuthorityInfoAccessExtension aiaExt, Collection<X509Certificate> certs) {
        if (!Builder.USE_AIA) {
            return false;
        }
        List<AccessDescription> adList = aiaExt.getAccessDescriptions();
        if (adList == null || adList.isEmpty()) {
            return false;
        }
        boolean add = false;
        for (AccessDescription ad : adList) {
            CertStore cs = URICertStore.getInstance(ad);
            if (cs != null) {
                try {
                    if (certs.addAll(cs.getCertificates(this.caSelector))) {
                        add = true;
                        if (!this.searchAllCertStores) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                } catch (CertStoreException cse) {
                    if (debug != null) {
                        debug.println("exception getting certs from CertStore:");
                        cse.printStackTrace();
                    }
                }
            }
        }
        return add;
    }

    void verifyCert(X509Certificate cert, State currentState, List<X509Certificate> certPathList) throws GeneralSecurityException {
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ForwardBuilder.verifyCert(SN: ");
            stringBuilder.append(Debug.toHexString(cert.getSerialNumber()));
            stringBuilder.append("\n  Issuer: ");
            stringBuilder.append(cert.getIssuerX500Principal());
            stringBuilder.append(")\n  Subject: ");
            stringBuilder.append(cert.getSubjectX500Principal());
            stringBuilder.append(")");
            debug.println(stringBuilder.toString());
        }
        ForwardState currState = (ForwardState) currentState;
        if (certPathList != null) {
            for (X509Certificate cpListCert : certPathList) {
                if (cert.equals(cpListCert)) {
                    if (debug != null) {
                        debug.println("loop detected!!");
                    }
                    throw new CertPathValidatorException("loop detected");
                }
            }
        }
        boolean isTrustedCert = this.trustedCerts.contains(cert);
        if (!isTrustedCert) {
            Set<String> unresCritExts = cert.getCriticalExtensionOIDs();
            if (unresCritExts == null) {
                unresCritExts = Collections.emptySet();
            }
            Iterator it = currState.forwardCheckers.iterator();
            while (it.hasNext()) {
                ((PKIXCertPathChecker) it.next()).check(cert, unresCritExts);
            }
            for (PKIXCertPathChecker checker : this.buildParams.certPathCheckers()) {
                if (!checker.isForwardCheckingSupported()) {
                    Set<String> supportedExts = checker.getSupportedExtensions();
                    if (supportedExts != null) {
                        unresCritExts.removeAll(supportedExts);
                    }
                }
            }
            if (!unresCritExts.isEmpty()) {
                unresCritExts.remove(PKIXExtensions.BasicConstraints_Id.toString());
                unresCritExts.remove(PKIXExtensions.NameConstraints_Id.toString());
                unresCritExts.remove(PKIXExtensions.CertificatePolicies_Id.toString());
                unresCritExts.remove(PKIXExtensions.PolicyMappings_Id.toString());
                unresCritExts.remove(PKIXExtensions.PolicyConstraints_Id.toString());
                unresCritExts.remove(PKIXExtensions.InhibitAnyPolicy_Id.toString());
                unresCritExts.remove(PKIXExtensions.SubjectAlternativeName_Id.toString());
                unresCritExts.remove(PKIXExtensions.KeyUsage_Id.toString());
                unresCritExts.remove(PKIXExtensions.ExtendedKeyUsage_Id.toString());
                if (!unresCritExts.isEmpty()) {
                    throw new CertPathValidatorException("Unrecognized critical extension(s)", null, null, -1, PKIXReason.UNRECOGNIZED_CRIT_EXT);
                }
            }
        }
        if (!currState.isInitial()) {
            if (!isTrustedCert) {
                if (cert.getBasicConstraints() != -1) {
                    KeyChecker.verifyCAKeyUsage(cert);
                } else {
                    throw new CertificateException("cert is NOT a CA cert");
                }
            }
            if (!currState.keyParamsNeeded()) {
                if (this.buildParams.sigProvider() != null) {
                    currState.cert.verify(cert.getPublicKey(), this.buildParams.sigProvider());
                } else {
                    currState.cert.verify(cert.getPublicKey());
                }
            }
        }
    }

    boolean isPathCompleted(X509Certificate cert) {
        X500Principal principal;
        PublicKey publicKey;
        List<TrustAnchor> otherAnchors = new ArrayList();
        for (TrustAnchor anchor : this.trustAnchors) {
            if (anchor.getTrustedCert() == null) {
                principal = anchor.getCA();
                publicKey = anchor.getCAPublicKey();
                if (principal == null || publicKey == null || !principal.equals(cert.getSubjectX500Principal()) || !publicKey.equals(cert.getPublicKey())) {
                    otherAnchors.add(anchor);
                } else {
                    this.trustAnchor = anchor;
                    return true;
                }
            } else if (cert.equals(anchor.getTrustedCert())) {
                this.trustAnchor = anchor;
                return true;
            }
        }
        for (TrustAnchor anchor2 : otherAnchors) {
            principal = anchor2.getCA();
            publicKey = anchor2.getCAPublicKey();
            if (principal != null) {
                if (principal.equals(cert.getIssuerX500Principal())) {
                    if (!PKIX.isDSAPublicKeyWithoutParams(publicKey)) {
                        try {
                            if (this.buildParams.sigProvider() != null) {
                                cert.verify(publicKey, this.buildParams.sigProvider());
                            } else {
                                cert.verify(publicKey);
                            }
                            this.trustAnchor = anchor2;
                            return true;
                        } catch (InvalidKeyException e) {
                            if (debug != null) {
                                debug.println("ForwardBuilder.isPathCompleted() invalid DSA key found");
                            }
                        } catch (GeneralSecurityException e2) {
                            if (debug != null) {
                                debug.println("ForwardBuilder.isPathCompleted() unexpected exception");
                                e2.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    void addCertToPath(X509Certificate cert, LinkedList<X509Certificate> certPathList) {
        certPathList.addFirst(cert);
    }

    void removeFinalCertFromPath(LinkedList<X509Certificate> certPathList) {
        certPathList.removeFirst();
    }
}
