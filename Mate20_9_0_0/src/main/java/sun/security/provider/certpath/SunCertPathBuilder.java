package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathBuilderSpi;
import java.security.cert.CertPathChecker;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertSelector;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PolicyNode;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.x509.PKIXExtensions;

public final class SunCertPathBuilder extends CertPathBuilderSpi {
    private static final Debug debug = Debug.getInstance("certpath");
    private BuilderParams buildParams;
    private CertificateFactory cf;
    private PublicKey finalPublicKey;
    private boolean pathCompleted = false;
    private PolicyNode policyTreeResult;
    private TrustAnchor trustAnchor;

    public SunCertPathBuilder() throws CertPathBuilderException {
        try {
            this.cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new CertPathBuilderException(e);
        }
    }

    public CertPathChecker engineGetRevocationChecker() {
        return new RevocationChecker();
    }

    public CertPathBuilderResult engineBuild(CertPathParameters params) throws CertPathBuilderException, InvalidAlgorithmParameterException {
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SunCertPathBuilder.engineBuild(");
            stringBuilder.append((Object) params);
            stringBuilder.append(")");
            debug.println(stringBuilder.toString());
        }
        this.buildParams = PKIX.checkBuilderParams(params);
        return build();
    }

    private PKIXCertPathBuilderResult build() throws CertPathBuilderException {
        List<List<Vertex>> adjList = new ArrayList();
        PKIXCertPathBuilderResult result = buildCertPath(null, adjList);
        if (result == null) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild: 2nd pass; try building again searching all certstores");
            }
            adjList.clear();
            result = buildCertPath(true, adjList);
            if (result == null) {
                throw new SunCertPathBuilderException("unable to find valid certification path to requested target", new AdjacencyList(adjList));
            }
        }
        return result;
    }

    private PKIXCertPathBuilderResult buildCertPath(boolean searchAllCertStores, List<List<Vertex>> adjList) throws CertPathBuilderException {
        this.pathCompleted = false;
        this.trustAnchor = null;
        this.finalPublicKey = null;
        this.policyTreeResult = null;
        List certPathList = new LinkedList();
        try {
            buildForward(adjList, certPathList, searchAllCertStores);
            try {
                if (!this.pathCompleted) {
                    return null;
                }
                if (debug != null) {
                    debug.println("SunCertPathBuilder.engineBuild() pathCompleted");
                }
                Collections.reverse(certPathList);
                return new SunCertPathBuilderResult(this.cf.generateCertPath(certPathList), this.trustAnchor, this.policyTreeResult, this.finalPublicKey, new AdjacencyList(adjList));
            } catch (CertificateException e) {
                if (debug != null) {
                    debug.println("SunCertPathBuilder.engineBuild() exception in wrap-up");
                    e.printStackTrace();
                }
                throw new SunCertPathBuilderException("unable to find valid certification path to requested target", e, new AdjacencyList(adjList));
            }
        } catch (IOException | GeneralSecurityException e2) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild() exception in build");
                e2.printStackTrace();
            }
            throw new SunCertPathBuilderException("unable to find valid certification path to requested target", e2, new AdjacencyList(adjList));
        }
    }

    private void buildForward(List<List<Vertex>> adjacencyList, LinkedList<X509Certificate> certPathList, boolean searchAllCertStores) throws GeneralSecurityException, IOException {
        if (debug != null) {
            debug.println("SunCertPathBuilder.buildForward()...");
        }
        ForwardState currentState = new ForwardState();
        currentState.initState(this.buildParams.certPathCheckers());
        adjacencyList.clear();
        adjacencyList.add(new LinkedList());
        depthFirstSearchForward(this.buildParams.targetSubject(), currentState, new ForwardBuilder(this.buildParams, searchAllCertStores), adjacencyList, certPathList);
    }

    private void depthFirstSearchForward(X500Principal dN, ForwardState currentState, ForwardBuilder builder, List<List<Vertex>> adjList, LinkedList<X509Certificate> cpList) throws GeneralSecurityException, IOException {
        Debug debug;
        StringBuilder stringBuilder;
        X500Principal x500Principal;
        Collection<X509Certificate> certs;
        List<Vertex> vertices;
        Vertex certs2;
        ForwardBuilder forwardBuilder = builder;
        List<List<Vertex>> list = adjList;
        Collection collection = cpList;
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SunCertPathBuilder.depthFirstSearchForward(");
            stringBuilder.append((Object) dN);
            stringBuilder.append(", ");
            stringBuilder.append(currentState.toString());
            stringBuilder.append(")");
            debug.println(stringBuilder.toString());
        } else {
            x500Principal = dN;
        }
        Collection<X509Certificate> certs3 = forwardBuilder.getMatchingCerts(currentState, this.buildParams.certStores());
        List<Vertex> vertices2 = addVertices(certs3, list);
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SunCertPathBuilder.depthFirstSearchForward(): certs.size=");
            stringBuilder.append(vertices2.size());
            debug.println(stringBuilder.toString());
        }
        Iterator it = vertices2.iterator();
        while (it.hasNext()) {
            Vertex vertex = (Vertex) it.next();
            ForwardState nextState = (ForwardState) currentState.clone();
            X509Certificate cert = vertex.getCertificate();
            StringBuilder stringBuilder2;
            Iterator it2;
            ForwardState forwardState;
            try {
                forwardBuilder.verifyCert(cert, nextState, collection);
                if (forwardBuilder.isPathCompleted(cert)) {
                    StringBuilder stringBuilder3;
                    BasicChecker basicChecker;
                    if (debug != null) {
                        debug.println("SunCertPathBuilder.depthFirstSearchForward(): commencing final verification");
                    }
                    List<X509Certificate> appendedCerts = new ArrayList(collection);
                    if (forwardBuilder.trustAnchor.getTrustedCert() == null) {
                        appendedCerts.add(0, cert);
                    }
                    PolicyNodeImpl policyNodeImpl = new PolicyNodeImpl(null, "2.5.29.32.0", null, false, Collections.singleton("2.5.29.32.0"), false);
                    List<PKIXCertPathChecker> checkers = new ArrayList();
                    PolicyChecker policyChecker = new PolicyChecker(this.buildParams.initialPolicies(), appendedCerts.size(), this.buildParams.explicitPolicyRequired(), this.buildParams.policyMappingInhibited(), this.buildParams.anyPolicyInhibited(), this.buildParams.policyQualifiersRejected(), policyNodeImpl);
                    List<PKIXCertPathChecker> checkers2 = checkers;
                    checkers2.add(policyChecker);
                    checkers2.add(new AlgorithmChecker(forwardBuilder.trustAnchor));
                    if (nextState.keyParamsNeeded()) {
                        PublicKey rootKey = cert.getPublicKey();
                        BasicChecker basicChecker2 = null;
                        if (forwardBuilder.trustAnchor.getTrustedCert() == null) {
                            rootKey = forwardBuilder.trustAnchor.getCAPublicKey();
                            if (debug != null) {
                                BasicChecker basicChecker3 = debug;
                                stringBuilder3 = new StringBuilder();
                                certs = certs3;
                                stringBuilder3.append("SunCertPathBuilder.depthFirstSearchForward using buildParams public key: ");
                                stringBuilder3.append(rootKey.toString());
                                basicChecker3.println(stringBuilder3.toString());
                                vertices = vertices2;
                                basicChecker = new BasicChecker(new TrustAnchor(cert.getSubjectX500Principal(), rootKey, (byte[]) null), this.buildParams.date(), this.buildParams.sigProvider(), true);
                                checkers2.add(basicChecker);
                            }
                        }
                        certs = certs3;
                        vertices = vertices2;
                        basicChecker = new BasicChecker(new TrustAnchor(cert.getSubjectX500Principal(), rootKey, (byte[]) null), this.buildParams.date(), this.buildParams.sigProvider(), true);
                        checkers2.add(basicChecker);
                    } else {
                        certs = certs3;
                        vertices = vertices2;
                        basicChecker = null;
                    }
                    this.buildParams.setCertPath(this.cf.generateCertPath((List) appendedCerts));
                    List<PKIXCertPathChecker> ckrs = this.buildParams.certPathCheckers();
                    certs3 = ckrs.iterator();
                    boolean revCheckerAdded = false;
                    while (certs3.hasNext()) {
                        PKIXCertPathChecker ckr = (PKIXCertPathChecker) certs3.next();
                        Collection<X509Certificate> collection2 = certs3;
                        if ((ckr instanceof PKIXRevocationChecker) != null) {
                            if (revCheckerAdded) {
                                PKIXCertPathChecker pKIXCertPathChecker = ckr;
                                throw new CertPathValidatorException("Only one PKIXRevocationChecker can be specified");
                            }
                            boolean revCheckerAdded2;
                            if (ckr instanceof RevocationChecker) {
                                revCheckerAdded2 = true;
                                ((RevocationChecker) ckr).init(forwardBuilder.trustAnchor, this.buildParams);
                            } else {
                                revCheckerAdded2 = true;
                            }
                            revCheckerAdded = revCheckerAdded2;
                        }
                        certs3 = collection2;
                    }
                    if (!this.buildParams.revocationEnabled() || revCheckerAdded) {
                    } else {
                        checkers2.add(new RevocationChecker(forwardBuilder.trustAnchor, this.buildParams));
                    }
                    checkers2.addAll(ckrs);
                    int i = 0;
                    while (true) {
                        certs3 = i;
                        List<X509Certificate> appendedCerts2;
                        List<PKIXCertPathChecker> ckrs2;
                        if (certs3 < appendedCerts.size()) {
                            List<PKIXCertPathChecker> checkers3;
                            X509Certificate vertices3 = (X509Certificate) appendedCerts.get(certs3);
                            if (debug != null) {
                                debug = debug;
                                appendedCerts2 = appendedCerts;
                                stringBuilder2 = new StringBuilder();
                                ckrs2 = ckrs;
                                stringBuilder2.append("current subject = ");
                                stringBuilder2.append(vertices3.getSubjectX500Principal());
                                debug.println(stringBuilder2.toString());
                            } else {
                                appendedCerts2 = appendedCerts;
                                ckrs2 = ckrs;
                            }
                            Set<String> unresCritExts = vertices3.getCriticalExtensionOIDs();
                            if (unresCritExts == null) {
                                unresCritExts = Collections.emptySet();
                            }
                            Set<String> unresCritExts2 = unresCritExts;
                            CertPathValidatorException cpve = checkers2.iterator();
                            while (cpve.hasNext()) {
                                CertPathValidatorException certPathValidatorException;
                                PKIXCertPathChecker currChecker = (PKIXCertPathChecker) cpve.next();
                                if (currChecker.isForwardCheckingSupported()) {
                                    certPathValidatorException = cpve;
                                    checkers3 = checkers2;
                                } else {
                                    if (certs3 == null) {
                                        certPathValidatorException = cpve;
                                        currChecker.init(false);
                                        if (currChecker instanceof AlgorithmChecker) {
                                            checkers3 = checkers2;
                                            ((AlgorithmChecker) currChecker).trySetTrustAnchor(forwardBuilder.trustAnchor);
                                            currChecker.check(vertices3, unresCritExts2);
                                        }
                                    } else {
                                        certPathValidatorException = cpve;
                                    }
                                    checkers3 = checkers2;
                                    try {
                                        currChecker.check(vertices3, unresCritExts2);
                                    } catch (CertPathValidatorException cpve2) {
                                        Object checkers4 = cpve2;
                                        if (debug != null) {
                                            checkers2 = debug;
                                            stringBuilder3 = new StringBuilder();
                                            it2 = it;
                                            stringBuilder3.append("SunCertPathBuilder.depthFirstSearchForward(): final verification failed: ");
                                            stringBuilder3.append(cpve2);
                                            checkers2.println(stringBuilder3.toString());
                                        } else {
                                            it2 = it;
                                        }
                                        if (this.buildParams.targetCertConstraints().match(vertices3) == null || cpve2.getReason() != BasicReason.REVOKED) {
                                            vertex.setThrowable(cpve2);
                                        } else {
                                            throw cpve2;
                                        }
                                    }
                                }
                                cpve = certPathValidatorException;
                                checkers2 = checkers3;
                                it = it;
                            }
                            checkers3 = checkers2;
                            it2 = it;
                            for (PKIXCertPathChecker checkers5 : this.buildParams.certPathCheckers()) {
                                if (checkers5.isForwardCheckingSupported()) {
                                    Set<String> suppExts = checkers5.getSupportedExtensions();
                                    if (suppExts != null) {
                                        unresCritExts2.removeAll(suppExts);
                                    }
                                }
                            }
                            if (!unresCritExts2.isEmpty()) {
                                unresCritExts2.remove(PKIXExtensions.BasicConstraints_Id.toString());
                                unresCritExts2.remove(PKIXExtensions.NameConstraints_Id.toString());
                                unresCritExts2.remove(PKIXExtensions.CertificatePolicies_Id.toString());
                                unresCritExts2.remove(PKIXExtensions.PolicyMappings_Id.toString());
                                unresCritExts2.remove(PKIXExtensions.PolicyConstraints_Id.toString());
                                unresCritExts2.remove(PKIXExtensions.InhibitAnyPolicy_Id.toString());
                                unresCritExts2.remove(PKIXExtensions.SubjectAlternativeName_Id.toString());
                                unresCritExts2.remove(PKIXExtensions.KeyUsage_Id.toString());
                                unresCritExts2.remove(PKIXExtensions.ExtendedKeyUsage_Id.toString());
                                if (!unresCritExts2.isEmpty()) {
                                    throw new CertPathValidatorException("unrecognized critical extension(s)", null, null, -1, PKIXReason.UNRECOGNIZED_CRIT_EXT);
                                }
                            }
                            i = certs3 + 1;
                            appendedCerts = appendedCerts2;
                            ckrs = ckrs2;
                            checkers2 = checkers3;
                            it = it2;
                        } else {
                            appendedCerts2 = appendedCerts;
                            ckrs2 = ckrs;
                            if (debug != null) {
                                debug.println("SunCertPathBuilder.depthFirstSearchForward(): final verification succeeded - path completed!");
                            }
                            this.pathCompleted = true;
                            if (forwardBuilder.trustAnchor.getTrustedCert() == null) {
                                forwardBuilder.addCertToPath(cert, collection);
                            }
                            this.trustAnchor = forwardBuilder.trustAnchor;
                            if (basicChecker != null) {
                                this.finalPublicKey = basicChecker.getPublicKey();
                            } else {
                                Certificate finalCert;
                                if (cpList.isEmpty()) {
                                    finalCert = forwardBuilder.trustAnchor.getTrustedCert();
                                } else {
                                    finalCert = (Certificate) cpList.getLast();
                                }
                                this.finalPublicKey = finalCert.getPublicKey();
                            }
                            this.policyTreeResult = policyChecker.getPolicyTree();
                            return;
                        }
                    }
                }
                certs = certs3;
                vertices = vertices2;
                it2 = it;
                forwardBuilder.addCertToPath(cert, collection);
                nextState.updateState(cert);
                list.add(new LinkedList());
                vertex.setIndex(adjList.size() - 1);
                certs2 = vertex;
                depthFirstSearchForward(cert.getIssuerX500Principal(), nextState, forwardBuilder, list, collection);
                if (!this.pathCompleted) {
                    if (debug != null) {
                        debug.println("SunCertPathBuilder.depthFirstSearchForward(): backtracking");
                    }
                    forwardBuilder.removeFinalCertFromPath(collection);
                    certs3 = certs;
                    vertices2 = vertices;
                    it = it2;
                    x500Principal = dN;
                    forwardState = currentState;
                } else {
                    return;
                }
            } catch (GeneralSecurityException cpve22) {
                X509Certificate x509Certificate = cert;
                forwardState = nextState;
                certs = certs3;
                vertices = vertices2;
                it2 = it;
                certs2 = vertex;
                GeneralSecurityException generalSecurityException = cpve22;
                if (debug != null) {
                    Debug debug2 = debug;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SunCertPathBuilder.depthFirstSearchForward(): validation failed: ");
                    stringBuilder2.append(cpve22);
                    debug2.println(stringBuilder2.toString());
                    cpve22.printStackTrace();
                }
                certs2.setThrowable(cpve22);
            }
        }
        certs = certs3;
        vertices = vertices2;
    }

    private static List<Vertex> addVertices(Collection<X509Certificate> certs, List<List<Vertex>> adjList) {
        List<Vertex> l = (List) adjList.get(adjList.size() - 1);
        for (X509Certificate cert : certs) {
            l.add(new Vertex(cert));
        }
        return l;
    }

    private static boolean anchorIsTarget(TrustAnchor anchor, CertSelector sel) {
        X509Certificate anchorCert = anchor.getTrustedCert();
        if (anchorCert != null) {
            return sel.match(anchorCert);
        }
        return false;
    }
}
