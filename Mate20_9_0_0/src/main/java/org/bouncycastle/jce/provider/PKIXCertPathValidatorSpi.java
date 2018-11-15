package org.bouncycastle.jce.provider;

import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertPathValidatorSpi;
import java.security.cert.CertificateEncodingException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.PolicyNode;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.jcajce.PKIXExtendedBuilderParameters;
import org.bouncycastle.jcajce.PKIXExtendedParameters;
import org.bouncycastle.jcajce.PKIXExtendedParameters.Builder;
import org.bouncycastle.jcajce.util.BCJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.exception.ExtCertPathValidatorException;
import org.bouncycastle.x509.ExtendedPKIXParameters;

public class PKIXCertPathValidatorSpi extends CertPathValidatorSpi {
    private final JcaJceHelper helper = new BCJcaJceHelper();

    static void checkCertificate(X509Certificate x509Certificate) throws AnnotatedException {
        try {
            TBSCertificate.getInstance(x509Certificate.getTBSCertificate());
        } catch (CertificateEncodingException e) {
            throw new AnnotatedException("unable to process TBSCertificate");
        } catch (IllegalArgumentException e2) {
            throw new AnnotatedException(e2.getMessage());
        }
    }

    public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters certPathParameters) throws CertPathValidatorException, InvalidAlgorithmParameterException {
        PKIXExtendedParameters build;
        AnnotatedException e;
        PKIXCertPathValidatorSpi pKIXCertPathValidatorSpi = this;
        CertPath certPath2 = certPath;
        CertPathParameters certPathParameters2 = certPathParameters;
        if (certPathParameters2 instanceof PKIXParameters) {
            Builder builder = new Builder((PKIXParameters) certPathParameters2);
            if (certPathParameters2 instanceof ExtendedPKIXParameters) {
                ExtendedPKIXParameters extendedPKIXParameters = (ExtendedPKIXParameters) certPathParameters2;
                builder.setUseDeltasEnabled(extendedPKIXParameters.isUseDeltasEnabled());
                builder.setValidityModel(extendedPKIXParameters.getValidityModel());
            }
            build = builder.build();
        } else if (certPathParameters2 instanceof PKIXExtendedBuilderParameters) {
            build = ((PKIXExtendedBuilderParameters) certPathParameters2).getBaseParameters();
        } else if (certPathParameters2 instanceof PKIXExtendedParameters) {
            build = (PKIXExtendedParameters) certPathParameters2;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Parameters must be a ");
            stringBuilder.append(PKIXParameters.class.getName());
            stringBuilder.append(" instance.");
            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
        }
        if (build.getTrustAnchors() != null) {
            List certificates = certPath.getCertificates();
            int size = certificates.size();
            if (certificates.isEmpty()) {
                throw new CertPathValidatorException("Certification path is empty.", null, certPath2, -1);
            }
            Set initialPolicies = build.getInitialPolicies();
            List list;
            try {
                TrustAnchor findTrustAnchor = CertPathValidatorUtilities.findTrustAnchor((X509Certificate) certificates.get(certificates.size() - 1), build.getTrustAnchors(), build.getSigProvider());
                if (findTrustAnchor != null) {
                    int i;
                    X500Name subjectPrincipal;
                    PublicKey publicKey;
                    checkCertificate(findTrustAnchor.getTrustedCert());
                    PKIXExtendedParameters build2 = new Builder(build).setTrustAnchor(findTrustAnchor).build();
                    int i2 = size + 1;
                    ArrayList[] arrayListArr = new ArrayList[i2];
                    for (i = 0; i < arrayListArr.length; i++) {
                        arrayListArr[i] = new ArrayList();
                    }
                    Set hashSet = new HashSet();
                    hashSet.add(RFC3280CertPathUtilities.ANY_POLICY);
                    PKIXPolicyNode pKIXPolicyNode = new PKIXPolicyNode(new ArrayList(), 0, hashSet, null, new HashSet(), RFC3280CertPathUtilities.ANY_POLICY, false);
                    arrayListArr[0].add(pKIXPolicyNode);
                    PKIXNameConstraintValidator pKIXNameConstraintValidator = new PKIXNameConstraintValidator();
                    HashSet hashSet2 = new HashSet();
                    i = build2.isExplicitPolicyRequired() ? 0 : i2;
                    int i3 = build2.isAnyPolicyInhibited() ? 0 : i2;
                    if (build2.isPolicyMappingInhibited()) {
                        i2 = 0;
                    }
                    X509Certificate trustedCert = findTrustAnchor.getTrustedCert();
                    if (trustedCert != null) {
                        try {
                            subjectPrincipal = PrincipalUtils.getSubjectPrincipal(trustedCert);
                            publicKey = trustedCert.getPublicKey();
                        } catch (Throwable e2) {
                            throw new ExtCertPathValidatorException("Subject of trust anchor could not be (re)encoded.", e2, certPath2, -1);
                        }
                    }
                    subjectPrincipal = PrincipalUtils.getCA(findTrustAnchor);
                    publicKey = findTrustAnchor.getCAPublicKey();
                    PublicKey publicKey2 = publicKey;
                    try {
                        ArrayList[] arrayListArr2;
                        int i4;
                        X509Certificate x509Certificate;
                        HashSet hashSet3;
                        PKIXExtendedParameters pKIXExtendedParameters;
                        int i5;
                        TrustAnchor trustAnchor;
                        List list2;
                        Set hashSet4;
                        AlgorithmIdentifier algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(publicKey2);
                        algorithmIdentifier.getAlgorithm();
                        algorithmIdentifier.getParameters();
                        if (build2.getTargetConstraints() != null) {
                            arrayListArr2 = arrayListArr;
                            if (!build2.getTargetConstraints().match((X509Certificate) certificates.get(0))) {
                                throw new ExtCertPathValidatorException("Target certificate in certification path does not match targetConstraints.", null, certPath2, 0);
                            }
                        }
                        arrayListArr2 = arrayListArr;
                        List certPathCheckers = build2.getCertPathCheckers();
                        Iterator it = certPathCheckers.iterator();
                        while (it.hasNext()) {
                            i4 = i2;
                            Iterator it2 = it;
                            ((PKIXCertPathChecker) it.next()).init(false);
                            i2 = i4;
                            it = it2;
                        }
                        Set set = initialPolicies;
                        X509Certificate x509Certificate2 = trustedCert;
                        X500Name x500Name = subjectPrincipal;
                        int i6 = i;
                        int i7 = size;
                        PublicKey publicKey3 = publicKey2;
                        i = i3;
                        int size2 = certificates.size() - 1;
                        pKIXPolicyNode = pKIXPolicyNode;
                        i2 = i2;
                        X509Certificate x509Certificate3 = null;
                        while (size2 >= 0) {
                            int i8 = size - size2;
                            int i9 = i7;
                            x509Certificate = (X509Certificate) certificates.get(size2);
                            boolean z = size2 == certificates.size() + -1;
                            try {
                                List[] listArr;
                                PKIXCertPathValidatorSpi pKIXCertPathValidatorSpi2;
                                List[] listArr2;
                                Object obj;
                                X509Certificate x509Certificate4;
                                checkCertificate(x509Certificate);
                                list = certificates;
                                JcaJceHelper jcaJceHelper = pKIXCertPathValidatorSpi.helper;
                                int i10 = i2;
                                CertPath certPath3 = certPath2;
                                int i11 = i;
                                hashSet3 = hashSet2;
                                int i12 = i10;
                                i10 = i8;
                                TrustAnchor trustAnchor2 = findTrustAnchor;
                                PKIXNameConstraintValidator pKIXNameConstraintValidator2 = pKIXNameConstraintValidator;
                                List list3 = certPathCheckers;
                                ArrayList[] arrayListArr3 = arrayListArr2;
                                pKIXExtendedParameters = build2;
                                RFC3280CertPathUtilities.processCertA(certPath3, build2, size2, publicKey3, z, x500Name, x509Certificate2, jcaJceHelper);
                                RFC3280CertPathUtilities.processCertBC(certPath2, size2, pKIXNameConstraintValidator2);
                                PKIXPolicyNode processCertE = RFC3280CertPathUtilities.processCertE(certPath2, size2, RFC3280CertPathUtilities.processCertD(certPath3, size2, hashSet3, pKIXPolicyNode, arrayListArr3, i11));
                                RFC3280CertPathUtilities.processCertF(certPath2, size2, processCertE, i6);
                                if (i10 == size) {
                                    i8 = i9;
                                    i5 = i11;
                                    i4 = i12;
                                    trustAnchor = trustAnchor2;
                                } else if (x509Certificate == null || x509Certificate.getVersion() != 1) {
                                    Set hashSet5;
                                    trustAnchor = trustAnchor2;
                                    RFC3280CertPathUtilities.prepareNextCertA(certPath2, size2);
                                    i10 = i12;
                                    listArr = arrayListArr3;
                                    processCertE = RFC3280CertPathUtilities.prepareCertB(certPath2, size2, listArr, processCertE, i10);
                                    RFC3280CertPathUtilities.prepareNextCertG(certPath2, size2, pKIXNameConstraintValidator2);
                                    i = RFC3280CertPathUtilities.prepareNextCertH1(certPath2, size2, i6);
                                    i10 = RFC3280CertPathUtilities.prepareNextCertH2(certPath2, size2, i10);
                                    i5 = RFC3280CertPathUtilities.prepareNextCertH3(certPath2, size2, i11);
                                    i = RFC3280CertPathUtilities.prepareNextCertI1(certPath2, size2, i);
                                    i10 = RFC3280CertPathUtilities.prepareNextCertI2(certPath2, size2, i10);
                                    i5 = RFC3280CertPathUtilities.prepareNextCertJ(certPath2, size2, i5);
                                    RFC3280CertPathUtilities.prepareNextCertK(certPath2, size2);
                                    i8 = RFC3280CertPathUtilities.prepareNextCertM(certPath2, size2, RFC3280CertPathUtilities.prepareNextCertL(certPath2, size2, i9));
                                    RFC3280CertPathUtilities.prepareNextCertN(certPath2, size2);
                                    Collection criticalExtensionOIDs = x509Certificate.getCriticalExtensionOIDs();
                                    if (criticalExtensionOIDs != null) {
                                        hashSet5 = new HashSet(criticalExtensionOIDs);
                                        hashSet5.remove(RFC3280CertPathUtilities.KEY_USAGE);
                                        hashSet5.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                                        hashSet5.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                                        hashSet5.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                                        hashSet5.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                                        hashSet5.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                                        hashSet5.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                                        hashSet5.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                                        hashSet5.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                                        hashSet5.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                                    } else {
                                        hashSet5 = new HashSet();
                                    }
                                    list2 = list3;
                                    RFC3280CertPathUtilities.prepareNextCertO(certPath2, size2, hashSet5, list2);
                                    x500Name = PrincipalUtils.getSubjectPrincipal(x509Certificate);
                                    try {
                                        pKIXCertPathValidatorSpi2 = this;
                                        PublicKey nextWorkingKey = CertPathValidatorUtilities.getNextWorkingKey(certPath.getCertificates(), size2, pKIXCertPathValidatorSpi2.helper);
                                        AlgorithmIdentifier algorithmIdentifier2 = CertPathValidatorUtilities.getAlgorithmIdentifier(nextWorkingKey);
                                        algorithmIdentifier2.getAlgorithm();
                                        algorithmIdentifier2.getParameters();
                                        pKIXPolicyNode = processCertE;
                                        i6 = i;
                                        i = i5;
                                        publicKey3 = nextWorkingKey;
                                        x509Certificate2 = x509Certificate;
                                        i2 = i10;
                                        size2--;
                                        listArr2 = listArr;
                                        pKIXCertPathValidatorSpi = pKIXCertPathValidatorSpi2;
                                        pKIXNameConstraintValidator = pKIXNameConstraintValidator2;
                                        hashSet2 = hashSet3;
                                        build2 = pKIXExtendedParameters;
                                        certificates = list;
                                        findTrustAnchor = trustAnchor;
                                        certPathCheckers = list2;
                                        obj = null;
                                        x509Certificate4 = x509Certificate;
                                        i7 = i8;
                                        x509Certificate3 = x509Certificate4;
                                    } catch (Throwable e22) {
                                        throw new CertPathValidatorException("Next working key could not be retrieved.", e22, certPath2, size2);
                                    }
                                } else {
                                    if (i10 == 1) {
                                        trustAnchor = trustAnchor2;
                                        if (x509Certificate.equals(trustAnchor.getTrustedCert())) {
                                            i8 = i9;
                                            i5 = i11;
                                            i4 = i12;
                                        }
                                    }
                                    throw new CertPathValidatorException("Version 1 certificates can't be used as CA ones.", null, certPath2, size2);
                                }
                                listArr = arrayListArr3;
                                list2 = list3;
                                pKIXCertPathValidatorSpi2 = this;
                                pKIXPolicyNode = processCertE;
                                i = i5;
                                i2 = i4;
                                size2--;
                                listArr2 = listArr;
                                pKIXCertPathValidatorSpi = pKIXCertPathValidatorSpi2;
                                pKIXNameConstraintValidator = pKIXNameConstraintValidator2;
                                hashSet2 = hashSet3;
                                build2 = pKIXExtendedParameters;
                                certificates = list;
                                findTrustAnchor = trustAnchor;
                                certPathCheckers = list2;
                                obj = null;
                                x509Certificate4 = x509Certificate;
                                i7 = i8;
                                x509Certificate3 = x509Certificate4;
                            } catch (AnnotatedException e3) {
                                AnnotatedException annotatedException = e3;
                                throw new CertPathValidatorException(annotatedException.getMessage(), annotatedException.getUnderlyingException(), certPath2, size2);
                            }
                        }
                        hashSet3 = hashSet2;
                        list2 = certPathCheckers;
                        pKIXExtendedParameters = build2;
                        trustAnchor = findTrustAnchor;
                        ArrayList[] arrayListArr4 = arrayListArr2;
                        i5 = size2 + 1;
                        int wrapupCertB = RFC3280CertPathUtilities.wrapupCertB(certPath2, i5, RFC3280CertPathUtilities.wrapupCertA(i6, x509Certificate3));
                        Collection criticalExtensionOIDs2 = x509Certificate3.getCriticalExtensionOIDs();
                        if (criticalExtensionOIDs2 != null) {
                            hashSet4 = new HashSet(criticalExtensionOIDs2);
                            hashSet4.remove(RFC3280CertPathUtilities.KEY_USAGE);
                            hashSet4.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                            hashSet4.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                            hashSet4.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                            hashSet4.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                            hashSet4.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                            hashSet4.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                            hashSet4.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                            hashSet4.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                            hashSet4.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                            hashSet4.remove(RFC3280CertPathUtilities.CRL_DISTRIBUTION_POINTS);
                            hashSet4.remove(Extension.extendedKeyUsage.getId());
                        } else {
                            hashSet4 = new HashSet();
                        }
                        RFC3280CertPathUtilities.wrapupCertF(certPath2, i5, list2, hashSet4);
                        x509Certificate = x509Certificate3;
                        PolicyNode wrapupCertG = RFC3280CertPathUtilities.wrapupCertG(certPath2, pKIXExtendedParameters, set, i5, arrayListArr4, pKIXPolicyNode, hashSet3);
                        if (wrapupCertB > 0 || wrapupCertG != null) {
                            return new PKIXCertPathValidatorResult(trustAnchor, wrapupCertG, x509Certificate.getPublicKey());
                        }
                        throw new CertPathValidatorException("Path processing failed on policy.", null, certPath2, size2);
                    } catch (Throwable e222) {
                        throw new ExtCertPathValidatorException("Algorithm identifier of public key of trust anchor could not be read.", e222, certPath2, -1);
                    }
                }
                list = certificates;
                try {
                    throw new CertPathValidatorException("Trust anchor for certification path not found.", null, certPath2, -1);
                } catch (AnnotatedException e4) {
                    e3 = e4;
                    throw new CertPathValidatorException(e3.getMessage(), e3.getUnderlyingException(), certPath2, list.size() - 1);
                }
            } catch (AnnotatedException e5) {
                e3 = e5;
                list = certificates;
                throw new CertPathValidatorException(e3.getMessage(), e3.getUnderlyingException(), certPath2, list.size() - 1);
            }
        }
        throw new InvalidAlgorithmParameterException("trustAnchors is null, this is not allowed for certification path validation.");
    }
}
