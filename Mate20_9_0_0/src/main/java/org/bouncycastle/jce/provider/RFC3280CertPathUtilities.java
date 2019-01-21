package org.bouncycastle.jce.provider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralSubtree;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.asn1.x509.NameConstraints;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.jcajce.PKIXCRLStore;
import org.bouncycastle.jcajce.PKIXCRLStoreSelector;
import org.bouncycastle.jcajce.PKIXCertStoreSelector;
import org.bouncycastle.jcajce.PKIXExtendedBuilderParameters;
import org.bouncycastle.jcajce.PKIXExtendedParameters;
import org.bouncycastle.jcajce.PKIXExtendedParameters.Builder;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.exception.ExtCertPathValidatorException;
import org.bouncycastle.util.Arrays;

class RFC3280CertPathUtilities {
    public static final String ANY_POLICY = "2.5.29.32.0";
    public static final String AUTHORITY_KEY_IDENTIFIER = Extension.authorityKeyIdentifier.getId();
    public static final String BASIC_CONSTRAINTS = Extension.basicConstraints.getId();
    public static final String CERTIFICATE_POLICIES = Extension.certificatePolicies.getId();
    public static final String CRL_DISTRIBUTION_POINTS = Extension.cRLDistributionPoints.getId();
    public static final String CRL_NUMBER = Extension.cRLNumber.getId();
    protected static final int CRL_SIGN = 6;
    private static final PKIXCRLUtil CRL_UTIL = new PKIXCRLUtil();
    public static final String DELTA_CRL_INDICATOR = Extension.deltaCRLIndicator.getId();
    public static final String FRESHEST_CRL = Extension.freshestCRL.getId();
    public static final String INHIBIT_ANY_POLICY = Extension.inhibitAnyPolicy.getId();
    public static final String ISSUING_DISTRIBUTION_POINT = Extension.issuingDistributionPoint.getId();
    protected static final int KEY_CERT_SIGN = 5;
    public static final String KEY_USAGE = Extension.keyUsage.getId();
    public static final String NAME_CONSTRAINTS = Extension.nameConstraints.getId();
    public static final String POLICY_CONSTRAINTS = Extension.policyConstraints.getId();
    public static final String POLICY_MAPPINGS = Extension.policyMappings.getId();
    public static final String SUBJECT_ALTERNATIVE_NAME = Extension.subjectAlternativeName.getId();
    protected static final String[] crlReasons = new String[]{"unspecified", "keyCompromise", "cACompromise", "affiliationChanged", "superseded", "cessationOfOperation", "certificateHold", "unknown", "removeFromCRL", "privilegeWithdrawn", "aACompromise"};

    RFC3280CertPathUtilities() {
    }

    private static void checkCRL(DistributionPoint distributionPoint, PKIXExtendedParameters pKIXExtendedParameters, X509Certificate x509Certificate, Date date, X509Certificate x509Certificate2, PublicKey publicKey, CertStatus certStatus, ReasonsMask reasonsMask, List list, JcaJceHelper jcaJceHelper) throws AnnotatedException {
        DistributionPoint distributionPoint2 = distributionPoint;
        PKIXExtendedParameters pKIXExtendedParameters2 = pKIXExtendedParameters;
        X509Certificate x509Certificate3 = x509Certificate;
        Date date2 = date;
        CertStatus certStatus2 = certStatus;
        ReasonsMask reasonsMask2 = reasonsMask;
        Date date3 = new Date(System.currentTimeMillis());
        if (date.getTime() <= date3.getTime()) {
            AnnotatedException annotatedException;
            Iterator it = CertPathValidatorUtilities.getCompleteCRLs(distributionPoint2, x509Certificate3, date3, pKIXExtendedParameters2).iterator();
            int i = 1;
            int i2 = 0;
            AnnotatedException annotatedException2 = null;
            while (it.hasNext() && certStatus.getCertStatus() == 11 && !reasonsMask.isAllReasons()) {
                Date date4;
                Iterator it2;
                int i3;
                ReasonsMask reasonsMask3;
                try {
                    X509CRL x509crl = (X509CRL) it.next();
                    ReasonsMask processCRLD = processCRLD(x509crl, distributionPoint2);
                    if (processCRLD.hasNewReasons(reasonsMask2)) {
                        date4 = date3;
                        ReasonsMask reasonsMask4 = processCRLD;
                        it2 = it;
                        X509CRL x509crl2 = x509crl;
                        annotatedException = annotatedException2;
                        int i4 = 11;
                        int i5 = i;
                        try {
                            X509CRL processCRLH = pKIXExtendedParameters.isUseDeltasEnabled() ? processCRLH(CertPathValidatorUtilities.getDeltaCRLs(pKIXExtendedParameters.getDate() != null ? pKIXExtendedParameters.getDate() : date4, x509crl2, pKIXExtendedParameters.getCertStores(), pKIXExtendedParameters.getCRLStores()), processCRLG(x509crl2, processCRLF(x509crl, x509Certificate3, x509Certificate2, publicKey, pKIXExtendedParameters2, list, jcaJceHelper))) : null;
                            if (pKIXExtendedParameters.getValidityModel() != i5) {
                                if (x509Certificate.getNotAfter().getTime() < x509crl2.getThisUpdate().getTime()) {
                                    throw new AnnotatedException("No valid CRL for current time found.");
                                }
                            }
                            processCRLB1(distributionPoint2, x509Certificate3, x509crl2);
                            processCRLB2(distributionPoint2, x509Certificate3, x509crl2);
                            processCRLC(processCRLH, x509crl2, pKIXExtendedParameters2);
                            processCRLI(date2, processCRLH, x509Certificate3, certStatus2, pKIXExtendedParameters2);
                            processCRLJ(date2, x509crl2, x509Certificate3, certStatus2);
                            if (certStatus.getCertStatus() == 8) {
                                certStatus2.setCertStatus(i4);
                            }
                            i3 = i5;
                            reasonsMask3 = reasonsMask;
                            try {
                                reasonsMask3.addReasons(reasonsMask4);
                                Set criticalExtensionOIDs = x509crl2.getCriticalExtensionOIDs();
                                if (criticalExtensionOIDs != null) {
                                    HashSet hashSet = new HashSet(criticalExtensionOIDs);
                                    hashSet.remove(Extension.issuingDistributionPoint.getId());
                                    hashSet.remove(Extension.deltaCRLIndicator.getId());
                                    if (!hashSet.isEmpty()) {
                                        throw new AnnotatedException("CRL contains unsupported critical extensions.");
                                    }
                                }
                                if (processCRLH != null) {
                                    criticalExtensionOIDs = processCRLH.getCriticalExtensionOIDs();
                                    if (criticalExtensionOIDs != null) {
                                        HashSet hashSet2 = new HashSet(criticalExtensionOIDs);
                                        hashSet2.remove(Extension.issuingDistributionPoint.getId());
                                        hashSet2.remove(Extension.deltaCRLIndicator.getId());
                                        if (!hashSet2.isEmpty()) {
                                            throw new AnnotatedException("Delta CRL contains unsupported critical extension.");
                                        }
                                    }
                                }
                                reasonsMask2 = reasonsMask3;
                                i = i3;
                                i2 = i;
                                date3 = date4;
                                it = it2;
                                annotatedException2 = annotatedException;
                            } catch (AnnotatedException e) {
                                annotatedException2 = e;
                                reasonsMask2 = reasonsMask3;
                                i = i3;
                                date3 = date4;
                                it = it2;
                            }
                        } catch (AnnotatedException e2) {
                            annotatedException2 = e2;
                            i3 = i5;
                            reasonsMask3 = reasonsMask;
                            reasonsMask2 = reasonsMask3;
                            i = i3;
                            date3 = date4;
                            it = it2;
                        }
                    }
                } catch (AnnotatedException e3) {
                    annotatedException2 = e3;
                    i3 = i;
                    reasonsMask3 = reasonsMask2;
                    date4 = date3;
                    it2 = it;
                    reasonsMask2 = reasonsMask3;
                    i = i3;
                    date3 = date4;
                    it = it2;
                }
            }
            annotatedException = annotatedException2;
            if (i2 == 0) {
                throw annotatedException;
            }
            return;
        }
        throw new AnnotatedException("Validation time is in future.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:55:0x0109  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x00fc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected static void checkCRLs(PKIXExtendedParameters pKIXExtendedParameters, X509Certificate x509Certificate, Date date, X509Certificate x509Certificate2, PublicKey publicKey, List list, JcaJceHelper jcaJceHelper) throws AnnotatedException {
        try {
            X509Certificate x509Certificate3 = x509Certificate;
            CRLDistPoint instance = CRLDistPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate3, CRL_DISTRIBUTION_POINTS));
            Builder builder = new Builder(pKIXExtendedParameters);
            try {
                Throwable e;
                int i;
                int i2;
                for (PKIXCRLStore addCRLStore : CertPathValidatorUtilities.getAdditionalStoresFromCRLDistributionPoint(instance, pKIXExtendedParameters.getNamedCRLStoreMap())) {
                    builder.addCRLStore(addCRLStore);
                }
                CertStatus certStatus = new CertStatus();
                ReasonsMask reasonsMask = new ReasonsMask();
                PKIXExtendedParameters build = builder.build();
                int i3 = 11;
                if (instance != null) {
                    try {
                        DistributionPoint[] distributionPoints = instance.getDistributionPoints();
                        if (distributionPoints != null) {
                            e = null;
                            int i4 = 0;
                            i = i4;
                            while (i4 < distributionPoints.length && certStatus.getCertStatus() == i3 && !reasonsMask.isAllReasons()) {
                                int i5;
                                DistributionPoint[] distributionPointArr;
                                PKIXExtendedParameters pKIXExtendedParameters2;
                                try {
                                    PKIXExtendedParameters pKIXExtendedParameters3 = build;
                                    i5 = i4;
                                    distributionPointArr = distributionPoints;
                                    pKIXExtendedParameters2 = build;
                                    i2 = i3;
                                    try {
                                        checkCRL(distributionPoints[i4], pKIXExtendedParameters3, x509Certificate3, date, x509Certificate2, publicKey, certStatus, reasonsMask, list, jcaJceHelper);
                                        i = 1;
                                    } catch (AnnotatedException e2) {
                                        e = e2;
                                    }
                                } catch (AnnotatedException e3) {
                                    e = e3;
                                    i5 = i4;
                                    distributionPointArr = distributionPoints;
                                    pKIXExtendedParameters2 = build;
                                    i2 = i3;
                                }
                                i4 = i5 + 1;
                                i3 = i2;
                                distributionPoints = distributionPointArr;
                                build = pKIXExtendedParameters2;
                            }
                            i2 = i3;
                            if (certStatus.getCertStatus() == i2 && !reasonsMask.isAllReasons()) {
                                checkCRL(new DistributionPoint(new DistributionPointName(0, new GeneralNames(new GeneralName(4, new ASN1InputStream(PrincipalUtils.getEncodedIssuerPrincipal(x509Certificate).getEncoded()).readObject()))), null, null), (PKIXExtendedParameters) pKIXExtendedParameters.clone(), x509Certificate3, date, x509Certificate2, publicKey, certStatus, reasonsMask, list, jcaJceHelper);
                                i = 1;
                            }
                            if (i != 0) {
                                if (e instanceof AnnotatedException) {
                                    throw e;
                                }
                                throw new AnnotatedException("No valid CRL found.", e);
                            } else if (certStatus.getCertStatus() == i2) {
                                if (!reasonsMask.isAllReasons() && certStatus.getCertStatus() == i2) {
                                    certStatus.setCertStatus(12);
                                }
                                if (certStatus.getCertStatus() == 12) {
                                    throw new AnnotatedException("Certificate status could not be determined.");
                                }
                                return;
                            } else {
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Certificate revocation after ");
                                stringBuilder.append(simpleDateFormat.format(certStatus.getRevocationDate()));
                                String stringBuilder2 = stringBuilder.toString();
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(stringBuilder2);
                                stringBuilder.append(", reason: ");
                                stringBuilder.append(crlReasons[certStatus.getCertStatus()]);
                                throw new AnnotatedException(stringBuilder.toString());
                            }
                        }
                    } catch (Exception e4) {
                        throw new AnnotatedException("Distribution points could not be read.", e4);
                    }
                }
                i2 = 11;
                e4 = null;
                i = 0;
                try {
                    checkCRL(new DistributionPoint(new DistributionPointName(0, new GeneralNames(new GeneralName(4, new ASN1InputStream(PrincipalUtils.getEncodedIssuerPrincipal(x509Certificate).getEncoded()).readObject()))), null, null), (PKIXExtendedParameters) pKIXExtendedParameters.clone(), x509Certificate3, date, x509Certificate2, publicKey, certStatus, reasonsMask, list, jcaJceHelper);
                    i = 1;
                } catch (Exception e5) {
                    throw new AnnotatedException("Issuer from certificate for CRL could not be reencoded.", e5);
                } catch (AnnotatedException e6) {
                    e4 = e6;
                }
                if (i != 0) {
                }
            } catch (AnnotatedException e7) {
                throw new AnnotatedException("No additional CRL locations could be decoded from CRL distribution point extension.", e7);
            }
        } catch (Exception e52) {
            throw new AnnotatedException("CRL distribution point extension could not be read.", e52);
        }
    }

    protected static PKIXPolicyNode prepareCertB(CertPath certPath, int i, List[] listArr, PKIXPolicyNode pKIXPolicyNode, int i2) throws CertPathValidatorException {
        CertPath certPath2 = certPath;
        int i3 = i;
        List[] listArr2 = listArr;
        List certificates = certPath.getCertificates();
        X509Certificate x509Certificate = (X509Certificate) certificates.get(i3);
        int size = certificates.size() - i3;
        try {
            ASN1Sequence instance = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate, POLICY_MAPPINGS));
            if (instance == null) {
                return pKIXPolicyNode;
            }
            int i4;
            String id;
            HashMap hashMap = new HashMap();
            HashSet hashSet = new HashSet();
            boolean z = false;
            int i5 = 0;
            while (true) {
                i4 = 1;
                if (i5 >= instance.size()) {
                    break;
                }
                ASN1Sequence aSN1Sequence = (ASN1Sequence) instance.getObjectAt(i5);
                String id2 = ((ASN1ObjectIdentifier) aSN1Sequence.getObjectAt(0)).getId();
                id = ((ASN1ObjectIdentifier) aSN1Sequence.getObjectAt(1)).getId();
                if (hashMap.containsKey(id2)) {
                    ((Set) hashMap.get(id2)).add(id);
                } else {
                    HashSet hashSet2 = new HashSet();
                    hashSet2.add(id);
                    hashMap.put(id2, hashSet2);
                    hashSet.add(id2);
                }
                i5++;
            }
            Iterator it = hashSet.iterator();
            PKIXPolicyNode pKIXPolicyNode2 = pKIXPolicyNode;
            while (it.hasNext()) {
                Iterator it2;
                int i6;
                String str = (String) it.next();
                Iterator it3;
                PKIXPolicyNode pKIXPolicyNode3;
                String str2;
                if (i2 > 0) {
                    int i7;
                    for (PKIXPolicyNode pKIXPolicyNode32 : listArr2[size]) {
                        if (pKIXPolicyNode32.getValidPolicy().equals(str)) {
                            pKIXPolicyNode32.expectedPolicies = (Set) hashMap.get(str);
                            i7 = i4;
                            break;
                        }
                    }
                    i7 = z;
                    if (i7 == 0) {
                        for (PKIXPolicyNode pKIXPolicyNode322 : listArr2[size]) {
                            if (ANY_POLICY.equals(pKIXPolicyNode322.getValidPolicy())) {
                                try {
                                    Set qualifierSet;
                                    Enumeration objects = ((ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(x509Certificate, CERTIFICATE_POLICIES)).getObjects();
                                    while (objects.hasMoreElements()) {
                                        try {
                                            PolicyInformation instance2 = PolicyInformation.getInstance(objects.nextElement());
                                            if (ANY_POLICY.equals(instance2.getPolicyIdentifier().getId())) {
                                                try {
                                                    qualifierSet = CertPathValidatorUtilities.getQualifierSet(instance2.getPolicyQualifiers());
                                                    break;
                                                } catch (CertPathValidatorException e) {
                                                    throw new ExtCertPathValidatorException("Policy qualifier info set could not be decoded.", e, certPath2, i3);
                                                }
                                            }
                                        } catch (Exception e2) {
                                            throw new CertPathValidatorException("Policy information could not be decoded.", e2, certPath2, i3);
                                        }
                                    }
                                    qualifierSet = null;
                                    boolean contains = x509Certificate.getCriticalExtensionOIDs() != null ? x509Certificate.getCriticalExtensionOIDs().contains(CERTIFICATE_POLICIES) : z;
                                    PKIXPolicyNode pKIXPolicyNode4 = (PKIXPolicyNode) pKIXPolicyNode322.getParent();
                                    if (ANY_POLICY.equals(pKIXPolicyNode4.getValidPolicy())) {
                                        PKIXPolicyNode pKIXPolicyNode5 = r6;
                                        PKIXPolicyNode pKIXPolicyNode6 = pKIXPolicyNode4;
                                        str2 = str;
                                        Set set = qualifierSet;
                                        it2 = it;
                                        i6 = i4;
                                        PKIXPolicyNode pKIXPolicyNode7 = new PKIXPolicyNode(new ArrayList(), size, (Set) hashMap.get(str), pKIXPolicyNode4, set, str2, contains);
                                        pKIXPolicyNode6.addChild(pKIXPolicyNode5);
                                        listArr2[size].add(pKIXPolicyNode5);
                                    }
                                } catch (AnnotatedException e3) {
                                    throw new ExtCertPathValidatorException("Certificate policies extension could not be decoded.", e3, certPath2, i3);
                                }
                            }
                        }
                    }
                    it2 = it;
                    i6 = i4;
                } else {
                    str2 = str;
                    it2 = it;
                    i6 = i4;
                    if (i2 <= 0) {
                        it3 = listArr2[size].iterator();
                        while (it3.hasNext()) {
                            pKIXPolicyNode322 = (PKIXPolicyNode) it3.next();
                            id = str2;
                            if (pKIXPolicyNode322.getValidPolicy().equals(id)) {
                                ((PKIXPolicyNode) pKIXPolicyNode322.getParent()).removeChild(pKIXPolicyNode322);
                                it3.remove();
                                for (int i8 = size - 1; i8 >= 0; i8--) {
                                    List list = listArr2[i8];
                                    PKIXPolicyNode pKIXPolicyNode8 = pKIXPolicyNode2;
                                    for (int i9 = 0; i9 < list.size(); i9++) {
                                        PKIXPolicyNode pKIXPolicyNode9 = (PKIXPolicyNode) list.get(i9);
                                        if (!pKIXPolicyNode9.hasChildren()) {
                                            pKIXPolicyNode8 = CertPathValidatorUtilities.removePolicyNode(pKIXPolicyNode8, listArr2, pKIXPolicyNode9);
                                            if (pKIXPolicyNode8 == null) {
                                                break;
                                            }
                                        }
                                    }
                                    pKIXPolicyNode2 = pKIXPolicyNode8;
                                }
                            }
                            str2 = id;
                        }
                    }
                }
                it = it2;
                i4 = i6;
                z = false;
            }
            return pKIXPolicyNode2;
        } catch (AnnotatedException e32) {
            throw new ExtCertPathValidatorException("Policy mappings extension could not be decoded.", e32, certPath2, i3);
        }
    }

    protected static void prepareNextCertA(CertPath certPath, int i) throws CertPathValidatorException {
        try {
            ASN1Sequence instance = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), POLICY_MAPPINGS));
            if (instance != null) {
                int i2 = 0;
                while (i2 < instance.size()) {
                    try {
                        ASN1Sequence instance2 = ASN1Sequence.getInstance(instance.getObjectAt(i2));
                        ASN1ObjectIdentifier instance3 = ASN1ObjectIdentifier.getInstance(instance2.getObjectAt(0));
                        ASN1ObjectIdentifier instance4 = ASN1ObjectIdentifier.getInstance(instance2.getObjectAt(1));
                        if (ANY_POLICY.equals(instance3.getId())) {
                            throw new CertPathValidatorException("IssuerDomainPolicy is anyPolicy", null, certPath, i);
                        } else if (ANY_POLICY.equals(instance4.getId())) {
                            throw new CertPathValidatorException("SubjectDomainPolicy is anyPolicy,", null, certPath, i);
                        } else {
                            i2++;
                        }
                    } catch (Exception e) {
                        throw new ExtCertPathValidatorException("Policy mappings extension contents could not be decoded.", e, certPath, i);
                    }
                }
            }
        } catch (AnnotatedException e2) {
            throw new ExtCertPathValidatorException("Policy mappings extension could not be decoded.", e2, certPath, i);
        }
    }

    protected static void prepareNextCertG(CertPath certPath, int i, PKIXNameConstraintValidator pKIXNameConstraintValidator) throws CertPathValidatorException {
        try {
            ASN1Sequence instance = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), NAME_CONSTRAINTS));
            NameConstraints instance2 = instance != null ? NameConstraints.getInstance(instance) : null;
            if (instance2 != null) {
                GeneralSubtree[] permittedSubtrees = instance2.getPermittedSubtrees();
                if (permittedSubtrees != null) {
                    try {
                        pKIXNameConstraintValidator.intersectPermittedSubtree(permittedSubtrees);
                    } catch (Exception e) {
                        throw new ExtCertPathValidatorException("Permitted subtrees cannot be build from name constraints extension.", e, certPath, i);
                    }
                }
                GeneralSubtree[] excludedSubtrees = instance2.getExcludedSubtrees();
                if (excludedSubtrees != null) {
                    int i2 = 0;
                    while (i2 != excludedSubtrees.length) {
                        try {
                            pKIXNameConstraintValidator.addExcludedSubtree(excludedSubtrees[i2]);
                            i2++;
                        } catch (Exception e2) {
                            throw new ExtCertPathValidatorException("Excluded subtrees cannot be build from name constraints extension.", e2, certPath, i);
                        }
                    }
                }
            }
        } catch (Exception e22) {
            throw new ExtCertPathValidatorException("Name constraints extension could not be decoded.", e22, certPath, i);
        }
    }

    protected static int prepareNextCertH1(CertPath certPath, int i, int i2) {
        return (CertPathValidatorUtilities.isSelfIssued((X509Certificate) certPath.getCertificates().get(i)) || i2 == 0) ? i2 : i2 - 1;
    }

    protected static int prepareNextCertH2(CertPath certPath, int i, int i2) {
        return (CertPathValidatorUtilities.isSelfIssued((X509Certificate) certPath.getCertificates().get(i)) || i2 == 0) ? i2 : i2 - 1;
    }

    protected static int prepareNextCertH3(CertPath certPath, int i, int i2) {
        return (CertPathValidatorUtilities.isSelfIssued((X509Certificate) certPath.getCertificates().get(i)) || i2 == 0) ? i2 : i2 - 1;
    }

    protected static int prepareNextCertI1(CertPath certPath, int i, int i2) throws CertPathValidatorException {
        try {
            ASN1Sequence instance = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), POLICY_CONSTRAINTS));
            if (instance != null) {
                Enumeration objects = instance.getObjects();
                while (objects.hasMoreElements()) {
                    try {
                        ASN1TaggedObject instance2 = ASN1TaggedObject.getInstance(objects.nextElement());
                        if (instance2.getTagNo() == 0) {
                            int intValue = ASN1Integer.getInstance(instance2, false).getValue().intValue();
                            if (intValue < i2) {
                                return intValue;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        throw new ExtCertPathValidatorException("Policy constraints extension contents cannot be decoded.", e, certPath, i);
                    }
                }
            }
            return i2;
        } catch (Exception e2) {
            throw new ExtCertPathValidatorException("Policy constraints extension cannot be decoded.", e2, certPath, i);
        }
    }

    protected static int prepareNextCertI2(CertPath certPath, int i, int i2) throws CertPathValidatorException {
        try {
            ASN1Sequence instance = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), POLICY_CONSTRAINTS));
            if (instance != null) {
                Enumeration objects = instance.getObjects();
                while (objects.hasMoreElements()) {
                    try {
                        ASN1TaggedObject instance2 = ASN1TaggedObject.getInstance(objects.nextElement());
                        if (instance2.getTagNo() == 1) {
                            int intValue = ASN1Integer.getInstance(instance2, false).getValue().intValue();
                            if (intValue < i2) {
                                return intValue;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        throw new ExtCertPathValidatorException("Policy constraints extension contents cannot be decoded.", e, certPath, i);
                    }
                }
            }
            return i2;
        } catch (Exception e2) {
            throw new ExtCertPathValidatorException("Policy constraints extension cannot be decoded.", e2, certPath, i);
        }
    }

    protected static int prepareNextCertJ(CertPath certPath, int i, int i2) throws CertPathValidatorException {
        try {
            ASN1Integer instance = ASN1Integer.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), INHIBIT_ANY_POLICY));
            if (instance != null) {
                int intValue = instance.getValue().intValue();
                if (intValue < i2) {
                    return intValue;
                }
            }
            return i2;
        } catch (Exception e) {
            throw new ExtCertPathValidatorException("Inhibit any-policy extension cannot be decoded.", e, certPath, i);
        }
    }

    protected static void prepareNextCertK(CertPath certPath, int i) throws CertPathValidatorException {
        try {
            BasicConstraints instance = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), BASIC_CONSTRAINTS));
            if (instance == null) {
                throw new CertPathValidatorException("Intermediate certificate lacks BasicConstraints");
            } else if (!instance.isCA()) {
                throw new CertPathValidatorException("Not a CA certificate");
            }
        } catch (Exception e) {
            throw new ExtCertPathValidatorException("Basic constraints extension cannot be decoded.", e, certPath, i);
        }
    }

    protected static int prepareNextCertL(CertPath certPath, int i, int i2) throws CertPathValidatorException {
        if (CertPathValidatorUtilities.isSelfIssued((X509Certificate) certPath.getCertificates().get(i))) {
            return i2;
        }
        if (i2 > 0) {
            return i2 - 1;
        }
        throw new ExtCertPathValidatorException("Max path length not greater than zero", null, certPath, i);
    }

    protected static int prepareNextCertM(CertPath certPath, int i, int i2) throws CertPathValidatorException {
        try {
            BasicConstraints instance = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), BASIC_CONSTRAINTS));
            if (instance != null) {
                BigInteger pathLenConstraint = instance.getPathLenConstraint();
                if (pathLenConstraint != null) {
                    int intValue = pathLenConstraint.intValue();
                    if (intValue < i2) {
                        return intValue;
                    }
                }
            }
            return i2;
        } catch (Exception e) {
            throw new ExtCertPathValidatorException("Basic constraints extension cannot be decoded.", e, certPath, i);
        }
    }

    protected static void prepareNextCertN(CertPath certPath, int i) throws CertPathValidatorException {
        boolean[] keyUsage = ((X509Certificate) certPath.getCertificates().get(i)).getKeyUsage();
        if (keyUsage != null && !keyUsage[5]) {
            throw new ExtCertPathValidatorException("Issuer certificate keyusage extension is critical and does not permit key signing.", null, certPath, i);
        }
    }

    protected static void prepareNextCertO(CertPath certPath, int i, Set set, List list) throws CertPathValidatorException {
        X509Certificate x509Certificate = (X509Certificate) certPath.getCertificates().get(i);
        for (PKIXCertPathChecker check : list) {
            try {
                check.check(x509Certificate, set);
            } catch (CertPathValidatorException e) {
                throw new CertPathValidatorException(e.getMessage(), e.getCause(), certPath, i);
            }
        }
        if (!set.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Certificate has unsupported critical extension: ");
            stringBuilder.append(set);
            throw new ExtCertPathValidatorException(stringBuilder.toString(), null, certPath, i);
        }
    }

    protected static Set processCRLA1i(Date date, PKIXExtendedParameters pKIXExtendedParameters, X509Certificate x509Certificate, X509CRL x509crl) throws AnnotatedException {
        HashSet hashSet = new HashSet();
        if (pKIXExtendedParameters.isUseDeltasEnabled()) {
            try {
                CRLDistPoint instance = CRLDistPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate, FRESHEST_CRL));
                if (instance == null) {
                    try {
                        instance = CRLDistPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(x509crl, FRESHEST_CRL));
                    } catch (AnnotatedException e) {
                        throw new AnnotatedException("Freshest CRL extension could not be decoded from CRL.", e);
                    }
                }
                if (instance != null) {
                    ArrayList arrayList = new ArrayList();
                    arrayList.addAll(pKIXExtendedParameters.getCRLStores());
                    try {
                        arrayList.addAll(CertPathValidatorUtilities.getAdditionalStoresFromCRLDistributionPoint(instance, pKIXExtendedParameters.getNamedCRLStoreMap()));
                        try {
                            hashSet.addAll(CertPathValidatorUtilities.getDeltaCRLs(date, x509crl, pKIXExtendedParameters.getCertStores(), arrayList));
                            return hashSet;
                        } catch (AnnotatedException e2) {
                            throw new AnnotatedException("Exception obtaining delta CRLs.", e2);
                        }
                    } catch (AnnotatedException e22) {
                        throw new AnnotatedException("No new delta CRL locations could be added from Freshest CRL extension.", e22);
                    }
                }
            } catch (AnnotatedException e222) {
                throw new AnnotatedException("Freshest CRL extension could not be decoded from certificate.", e222);
            }
        }
        return hashSet;
    }

    protected static Set[] processCRLA1ii(Date date, PKIXExtendedParameters pKIXExtendedParameters, X509Certificate x509Certificate, X509CRL x509crl) throws AnnotatedException {
        HashSet hashSet = new HashSet();
        X509CRLSelector x509CRLSelector = new X509CRLSelector();
        x509CRLSelector.setCertificateChecking(x509Certificate);
        try {
            x509CRLSelector.addIssuerName(PrincipalUtils.getIssuerPrincipal(x509crl).getEncoded());
            PKIXCRLStoreSelector build = new PKIXCRLStoreSelector.Builder(x509CRLSelector).setCompleteCRLEnabled(true).build();
            if (pKIXExtendedParameters.getDate() != null) {
                date = pKIXExtendedParameters.getDate();
            }
            Set findCRLs = CRL_UTIL.findCRLs(build, date, pKIXExtendedParameters.getCertStores(), pKIXExtendedParameters.getCRLStores());
            if (pKIXExtendedParameters.isUseDeltasEnabled()) {
                try {
                    hashSet.addAll(CertPathValidatorUtilities.getDeltaCRLs(date, x509crl, pKIXExtendedParameters.getCertStores(), pKIXExtendedParameters.getCRLStores()));
                } catch (AnnotatedException e) {
                    throw new AnnotatedException("Exception obtaining delta CRLs.", e);
                }
            }
            return new Set[]{findCRLs, hashSet};
        } catch (IOException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot extract issuer from CRL.");
            stringBuilder.append(e2);
            throw new AnnotatedException(stringBuilder.toString(), e2);
        }
    }

    protected static void processCRLB1(DistributionPoint distributionPoint, Object obj, X509CRL x509crl) throws AnnotatedException {
        ASN1Primitive extensionValue = CertPathValidatorUtilities.getExtensionValue(x509crl, ISSUING_DISTRIBUTION_POINT);
        int i = 0;
        int i2 = (extensionValue == null || !IssuingDistributionPoint.getInstance(extensionValue).isIndirectCRL()) ? 0 : 1;
        try {
            int i3;
            byte[] encoded = PrincipalUtils.getIssuerPrincipal(x509crl).getEncoded();
            if (distributionPoint.getCRLIssuer() != null) {
                GeneralName[] names = distributionPoint.getCRLIssuer().getNames();
                i3 = 0;
                while (i < names.length) {
                    if (names[i].getTagNo() == 4) {
                        try {
                            if (Arrays.areEqual(names[i].getName().toASN1Primitive().getEncoded(), encoded)) {
                                i3 = 1;
                            }
                        } catch (IOException e) {
                            throw new AnnotatedException("CRL issuer information from distribution point cannot be decoded.", e);
                        }
                    }
                    i++;
                }
                if (i3 != 0 && i2 == 0) {
                    throw new AnnotatedException("Distribution point contains cRLIssuer field but CRL is not indirect.");
                } else if (i3 == 0) {
                    throw new AnnotatedException("CRL issuer of CRL does not match CRL issuer of distribution point.");
                }
            }
            i3 = PrincipalUtils.getIssuerPrincipal(x509crl).equals(PrincipalUtils.getEncodedIssuerPrincipal(obj)) ? 1 : 0;
            if (i3 == 0) {
                throw new AnnotatedException("Cannot find matching CRL issuer for certificate.");
            }
        } catch (IOException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception encoding CRL issuer: ");
            stringBuilder.append(e2.getMessage());
            throw new AnnotatedException(stringBuilder.toString(), e2);
        }
    }

    protected static void processCRLB2(DistributionPoint distributionPoint, Object obj, X509CRL x509crl) throws AnnotatedException {
        try {
            IssuingDistributionPoint instance = IssuingDistributionPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(x509crl, ISSUING_DISTRIBUTION_POINT));
            if (instance != null) {
                if (instance.getDistributionPoint() != null) {
                    DistributionPointName distributionPoint2 = IssuingDistributionPoint.getInstance(instance).getDistributionPoint();
                    ArrayList arrayList = new ArrayList();
                    int i = 0;
                    if (distributionPoint2.getType() == 0) {
                        GeneralName[] names = GeneralNames.getInstance(distributionPoint2.getName()).getNames();
                        for (Object add : names) {
                            arrayList.add(add);
                        }
                    }
                    if (distributionPoint2.getType() == 1) {
                        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
                        try {
                            Enumeration objects = ASN1Sequence.getInstance(PrincipalUtils.getIssuerPrincipal(x509crl)).getObjects();
                            while (objects.hasMoreElements()) {
                                aSN1EncodableVector.add((ASN1Encodable) objects.nextElement());
                            }
                            aSN1EncodableVector.add(distributionPoint2.getName());
                            arrayList.add(new GeneralName(X500Name.getInstance(new DERSequence(aSN1EncodableVector))));
                        } catch (Exception e) {
                            throw new AnnotatedException("Could not read CRL issuer.", e);
                        }
                    }
                    GeneralName[] names2;
                    if (distributionPoint.getDistributionPoint() != null) {
                        int i2;
                        DistributionPointName distributionPoint3 = distributionPoint.getDistributionPoint();
                        GeneralName[] generalNameArr = null;
                        if (distributionPoint3.getType() == 0) {
                            generalNameArr = GeneralNames.getInstance(distributionPoint3.getName()).getNames();
                        }
                        if (distributionPoint3.getType() == 1) {
                            if (distributionPoint.getCRLIssuer() != null) {
                                names2 = distributionPoint.getCRLIssuer().getNames();
                            } else {
                                names2 = new GeneralName[1];
                                try {
                                    names2[0] = new GeneralName(X500Name.getInstance(PrincipalUtils.getEncodedIssuerPrincipal(obj).getEncoded()));
                                } catch (Exception e2) {
                                    throw new AnnotatedException("Could not read certificate issuer.", e2);
                                }
                            }
                            generalNameArr = names2;
                            for (i2 = 0; i2 < generalNameArr.length; i2++) {
                                Enumeration objects2 = ASN1Sequence.getInstance(generalNameArr[i2].getName().toASN1Primitive()).getObjects();
                                ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
                                while (objects2.hasMoreElements()) {
                                    aSN1EncodableVector2.add((ASN1Encodable) objects2.nextElement());
                                }
                                aSN1EncodableVector2.add(distributionPoint3.getName());
                                generalNameArr[i2] = new GeneralName(X500Name.getInstance(new DERSequence(aSN1EncodableVector2)));
                            }
                        }
                        if (generalNameArr != null) {
                            for (Object contains : generalNameArr) {
                                if (arrayList.contains(contains)) {
                                    i = 1;
                                    break;
                                }
                            }
                        }
                        if (i == 0) {
                            throw new AnnotatedException("No match for certificate CRL issuing distribution point name to cRLIssuer CRL distribution point.");
                        }
                    } else if (distributionPoint.getCRLIssuer() != null) {
                        names2 = distributionPoint.getCRLIssuer().getNames();
                        for (Object contains2 : names2) {
                            if (arrayList.contains(contains2)) {
                                i = 1;
                                break;
                            }
                        }
                        if (i == 0) {
                            throw new AnnotatedException("No match for certificate CRL issuing distribution point name to cRLIssuer CRL distribution point.");
                        }
                    } else {
                        throw new AnnotatedException("Either the cRLIssuer or the distributionPoint field must be contained in DistributionPoint.");
                    }
                }
                try {
                    BasicConstraints instance2 = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Extension) obj, BASIC_CONSTRAINTS));
                    if (obj instanceof X509Certificate) {
                        if (instance.onlyContainsUserCerts() && instance2 != null && instance2.isCA()) {
                            throw new AnnotatedException("CA Cert CRL only contains user certificates.");
                        } else if (instance.onlyContainsCACerts() && (instance2 == null || !instance2.isCA())) {
                            throw new AnnotatedException("End CRL only contains CA certificates.");
                        }
                    }
                    if (instance.onlyContainsAttributeCerts()) {
                        throw new AnnotatedException("onlyContainsAttributeCerts boolean is asserted.");
                    }
                } catch (Exception e22) {
                    throw new AnnotatedException("Basic constraints extension could not be decoded.", e22);
                }
            }
        } catch (Exception e222) {
            throw new AnnotatedException("Issuing distribution point extension could not be decoded.", e222);
        }
    }

    protected static void processCRLC(X509CRL x509crl, X509CRL x509crl2, PKIXExtendedParameters pKIXExtendedParameters) throws AnnotatedException {
        if (x509crl != null) {
            try {
                IssuingDistributionPoint instance = IssuingDistributionPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(x509crl2, ISSUING_DISTRIBUTION_POINT));
                if (!pKIXExtendedParameters.isUseDeltasEnabled()) {
                    return;
                }
                if (PrincipalUtils.getIssuerPrincipal(x509crl).equals(PrincipalUtils.getIssuerPrincipal(x509crl2))) {
                    try {
                        IssuingDistributionPoint instance2 = IssuingDistributionPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(x509crl, ISSUING_DISTRIBUTION_POINT));
                        Object obj = 1;
                        if (instance != null ? !instance.equals(instance2) : instance2 != null) {
                            obj = null;
                        }
                        if (obj != null) {
                            try {
                                ASN1Primitive extensionValue = CertPathValidatorUtilities.getExtensionValue(x509crl2, AUTHORITY_KEY_IDENTIFIER);
                                try {
                                    ASN1Primitive extensionValue2 = CertPathValidatorUtilities.getExtensionValue(x509crl, AUTHORITY_KEY_IDENTIFIER);
                                    if (extensionValue == null) {
                                        throw new AnnotatedException("CRL authority key identifier is null.");
                                    } else if (extensionValue2 == null) {
                                        throw new AnnotatedException("Delta CRL authority key identifier is null.");
                                    } else if (!extensionValue.equals(extensionValue2)) {
                                        throw new AnnotatedException("Delta CRL authority key identifier does not match complete CRL authority key identifier.");
                                    } else {
                                        return;
                                    }
                                } catch (AnnotatedException e) {
                                    throw new AnnotatedException("Authority key identifier extension could not be extracted from delta CRL.", e);
                                }
                            } catch (AnnotatedException e2) {
                                throw new AnnotatedException("Authority key identifier extension could not be extracted from complete CRL.", e2);
                            }
                        }
                        throw new AnnotatedException("Issuing distribution point extension from delta CRL and complete CRL does not match.");
                    } catch (Exception e3) {
                        throw new AnnotatedException("Issuing distribution point extension from delta CRL could not be decoded.", e3);
                    }
                }
                throw new AnnotatedException("Complete CRL issuer does not match delta CRL issuer.");
            } catch (Exception e32) {
                throw new AnnotatedException("Issuing distribution point extension could not be decoded.", e32);
            }
        }
    }

    protected static ReasonsMask processCRLD(X509CRL x509crl, DistributionPoint distributionPoint) throws AnnotatedException {
        try {
            IssuingDistributionPoint instance = IssuingDistributionPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(x509crl, ISSUING_DISTRIBUTION_POINT));
            if (instance != null && instance.getOnlySomeReasons() != null && distributionPoint.getReasons() != null) {
                return new ReasonsMask(distributionPoint.getReasons()).intersect(new ReasonsMask(instance.getOnlySomeReasons()));
            }
            if ((instance == null || instance.getOnlySomeReasons() == null) && distributionPoint.getReasons() == null) {
                return ReasonsMask.allReasons;
            }
            return (distributionPoint.getReasons() == null ? ReasonsMask.allReasons : new ReasonsMask(distributionPoint.getReasons())).intersect(instance == null ? ReasonsMask.allReasons : new ReasonsMask(instance.getOnlySomeReasons()));
        } catch (Exception e) {
            throw new AnnotatedException("Issuing distribution point extension could not be decoded.", e);
        }
    }

    protected static Set processCRLF(X509CRL x509crl, Object obj, X509Certificate x509Certificate, PublicKey publicKey, PKIXExtendedParameters pKIXExtendedParameters, List list, JcaJceHelper jcaJceHelper) throws AnnotatedException {
        X509CertSelector x509CertSelector = new X509CertSelector();
        try {
            x509CertSelector.setSubject(PrincipalUtils.getIssuerPrincipal(x509crl).getEncoded());
            PKIXCertStoreSelector build = new PKIXCertStoreSelector.Builder(x509CertSelector).build();
            try {
                int i;
                Collection findCertificates = CertPathValidatorUtilities.findCertificates(build, pKIXExtendedParameters.getCertificateStores());
                findCertificates.addAll(CertPathValidatorUtilities.findCertificates(build, pKIXExtendedParameters.getCertStores()));
                findCertificates.add(x509Certificate);
                Iterator it = findCertificates.iterator();
                ArrayList arrayList = new ArrayList();
                ArrayList arrayList2 = new ArrayList();
                while (true) {
                    i = 0;
                    if (!it.hasNext()) {
                        break;
                    }
                    X509Certificate x509Certificate2 = (X509Certificate) it.next();
                    if (x509Certificate2.equals(x509Certificate)) {
                        arrayList.add(x509Certificate2);
                        arrayList2.add(publicKey);
                    } else {
                        try {
                            PKIXCertPathBuilderSpi pKIXCertPathBuilderSpi = new PKIXCertPathBuilderSpi();
                            X509CertSelector x509CertSelector2 = new X509CertSelector();
                            x509CertSelector2.setCertificate(x509Certificate2);
                            Builder targetConstraints = new Builder(pKIXExtendedParameters).setTargetConstraints(new PKIXCertStoreSelector.Builder(x509CertSelector2).build());
                            if (list.contains(x509Certificate2)) {
                                targetConstraints.setRevocationEnabled(false);
                            } else {
                                targetConstraints.setRevocationEnabled(true);
                            }
                            List certificates = pKIXCertPathBuilderSpi.engineBuild(new PKIXExtendedBuilderParameters.Builder(targetConstraints.build()).build()).getCertPath().getCertificates();
                            arrayList.add(x509Certificate2);
                            arrayList2.add(CertPathValidatorUtilities.getNextWorkingKey(certificates, 0, jcaJceHelper));
                        } catch (CertPathBuilderException e) {
                            throw new AnnotatedException("CertPath for CRL signer failed to validate.", e);
                        } catch (CertPathValidatorException e2) {
                            throw new AnnotatedException("Public key of issuer certificate of CRL could not be retrieved.", e2);
                        } catch (Exception e3) {
                            throw new AnnotatedException(e3.getMessage());
                        }
                    }
                }
                HashSet hashSet = new HashSet();
                AnnotatedException annotatedException = null;
                while (i < arrayList.size()) {
                    boolean[] keyUsage = ((X509Certificate) arrayList.get(i)).getKeyUsage();
                    if (keyUsage == null || (keyUsage.length >= 7 && keyUsage[6])) {
                        hashSet.add(arrayList2.get(i));
                    } else {
                        annotatedException = new AnnotatedException("Issuer certificate key usage extension does not permit CRL signing.");
                    }
                    i++;
                }
                if (hashSet.isEmpty() && annotatedException == null) {
                    throw new AnnotatedException("Cannot find a valid issuer certificate.");
                } else if (!hashSet.isEmpty() || annotatedException == null) {
                    return hashSet;
                } else {
                    throw annotatedException;
                }
            } catch (AnnotatedException e4) {
                throw new AnnotatedException("Issuer certificate for CRL cannot be searched.", e4);
            }
        } catch (IOException e5) {
            throw new AnnotatedException("Subject criteria for certificate selector to find issuer certificate for CRL could not be set.", e5);
        }
    }

    protected static PublicKey processCRLG(X509CRL x509crl, Set set) throws AnnotatedException {
        Throwable e = null;
        for (PublicKey publicKey : set) {
            try {
                x509crl.verify(publicKey);
                return publicKey;
            } catch (Exception e2) {
                e = e2;
            }
        }
        throw new AnnotatedException("Cannot verify CRL.", e);
    }

    protected static X509CRL processCRLH(Set set, PublicKey publicKey) throws AnnotatedException {
        Throwable e = null;
        for (X509CRL x509crl : set) {
            try {
                x509crl.verify(publicKey);
                return x509crl;
            } catch (Exception e2) {
                e = e2;
            }
        }
        if (e == null) {
            return null;
        }
        throw new AnnotatedException("Cannot verify delta CRL.", e);
    }

    protected static void processCRLI(Date date, X509CRL x509crl, Object obj, CertStatus certStatus, PKIXExtendedParameters pKIXExtendedParameters) throws AnnotatedException {
        if (pKIXExtendedParameters.isUseDeltasEnabled() && x509crl != null) {
            CertPathValidatorUtilities.getCertStatus(date, x509crl, obj, certStatus);
        }
    }

    protected static void processCRLJ(Date date, X509CRL x509crl, Object obj, CertStatus certStatus) throws AnnotatedException {
        if (certStatus.getCertStatus() == 11) {
            CertPathValidatorUtilities.getCertStatus(date, x509crl, obj, certStatus);
        }
    }

    protected static void processCertA(CertPath certPath, PKIXExtendedParameters pKIXExtendedParameters, int i, PublicKey publicKey, boolean z, X500Name x500Name, X509Certificate x509Certificate, JcaJceHelper jcaJceHelper) throws ExtCertPathValidatorException {
        StringBuilder stringBuilder;
        List certificates = certPath.getCertificates();
        X509Certificate x509Certificate2 = (X509Certificate) certificates.get(i);
        if (!z) {
            try {
                CertPathValidatorUtilities.verifyX509Certificate(x509Certificate2, publicKey, pKIXExtendedParameters.getSigProvider());
            } catch (GeneralSecurityException e) {
                throw new ExtCertPathValidatorException("Could not validate certificate signature.", e, certPath, i);
            }
        }
        try {
            x509Certificate2.checkValidity(CertPathValidatorUtilities.getValidCertDateFromValidityModel(pKIXExtendedParameters, certPath, i));
            if (pKIXExtendedParameters.isRevocationEnabled()) {
                try {
                    checkCRLs(pKIXExtendedParameters, x509Certificate2, CertPathValidatorUtilities.getValidCertDateFromValidityModel(pKIXExtendedParameters, certPath, i), x509Certificate, publicKey, certificates, jcaJceHelper);
                } catch (AnnotatedException e2) {
                    throw new ExtCertPathValidatorException(e2.getMessage(), e2.getCause() != null ? e2.getCause() : e2, certPath, i);
                }
            }
            if (!PrincipalUtils.getEncodedIssuerPrincipal(x509Certificate2).equals(x500Name)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IssuerName(");
                stringBuilder2.append(PrincipalUtils.getEncodedIssuerPrincipal(x509Certificate2));
                stringBuilder2.append(") does not match SubjectName(");
                stringBuilder2.append(x500Name);
                stringBuilder2.append(") of signing certificate.");
                throw new ExtCertPathValidatorException(stringBuilder2.toString(), null, certPath, i);
            }
        } catch (CertificateExpiredException e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not validate certificate: ");
            stringBuilder.append(e3.getMessage());
            throw new ExtCertPathValidatorException(stringBuilder.toString(), e3, certPath, i);
        } catch (CertificateNotYetValidException e4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not validate certificate: ");
            stringBuilder.append(e4.getMessage());
            throw new ExtCertPathValidatorException(stringBuilder.toString(), e4, certPath, i);
        } catch (AnnotatedException e5) {
            throw new ExtCertPathValidatorException("Could not validate time of certificate.", e5, certPath, i);
        }
    }

    protected static void processCertBC(CertPath certPath, int i, PKIXNameConstraintValidator pKIXNameConstraintValidator) throws CertPathValidatorException {
        List certificates = certPath.getCertificates();
        X509Certificate x509Certificate = (X509Certificate) certificates.get(i);
        int size = certificates.size();
        int i2 = size - i;
        if (!CertPathValidatorUtilities.isSelfIssued(x509Certificate) || i2 >= size) {
            try {
                ASN1Sequence instance = ASN1Sequence.getInstance(PrincipalUtils.getSubjectPrincipal(x509Certificate).getEncoded());
                try {
                    pKIXNameConstraintValidator.checkPermittedDN(instance);
                    pKIXNameConstraintValidator.checkExcludedDN(instance);
                    try {
                        GeneralNames instance2 = GeneralNames.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate, SUBJECT_ALTERNATIVE_NAME));
                        RDN[] rDNs = X500Name.getInstance(instance).getRDNs(BCStyle.EmailAddress);
                        i2 = 0;
                        int i3 = 0;
                        while (i3 != rDNs.length) {
                            GeneralName generalName = new GeneralName(1, ((ASN1String) rDNs[i3].getFirst().getValue()).getString());
                            try {
                                pKIXNameConstraintValidator.checkPermitted(generalName);
                                pKIXNameConstraintValidator.checkExcluded(generalName);
                                i3++;
                            } catch (PKIXNameConstraintValidatorException e) {
                                throw new CertPathValidatorException("Subtree check for certificate subject alternative email failed.", e, certPath, i);
                            }
                        }
                        if (instance2 != null) {
                            try {
                                GeneralName[] names = instance2.getNames();
                                while (i2 < names.length) {
                                    try {
                                        pKIXNameConstraintValidator.checkPermitted(names[i2]);
                                        pKIXNameConstraintValidator.checkExcluded(names[i2]);
                                        i2++;
                                    } catch (PKIXNameConstraintValidatorException e2) {
                                        throw new CertPathValidatorException("Subtree check for certificate subject alternative name failed.", e2, certPath, i);
                                    }
                                }
                            } catch (Exception e3) {
                                throw new CertPathValidatorException("Subject alternative name contents could not be decoded.", e3, certPath, i);
                            }
                        }
                    } catch (Exception e32) {
                        throw new CertPathValidatorException("Subject alternative name extension could not be decoded.", e32, certPath, i);
                    }
                } catch (PKIXNameConstraintValidatorException e22) {
                    throw new CertPathValidatorException("Subtree check for certificate subject failed.", e22, certPath, i);
                }
            } catch (Exception e322) {
                throw new CertPathValidatorException("Exception extracting subject name when checking subtrees.", e322, certPath, i);
            }
        }
    }

    protected static PKIXPolicyNode processCertD(CertPath certPath, int i, Set set, PKIXPolicyNode pKIXPolicyNode, List[] listArr, int i2) throws CertPathValidatorException {
        CertPath certPath2 = certPath;
        int i3 = i;
        Set set2 = set;
        List[] listArr2 = listArr;
        List certificates = certPath.getCertificates();
        X509Certificate x509Certificate = (X509Certificate) certificates.get(i3);
        int size = certificates.size();
        int i4 = size - i3;
        try {
            ASN1Sequence instance = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate, CERTIFICATE_POLICIES));
            if (instance == null || pKIXPolicyNode == null) {
                return null;
            }
            Object obj;
            List list;
            Enumeration objects = instance.getObjects();
            HashSet hashSet = new HashSet();
            while (objects.hasMoreElements()) {
                PolicyInformation instance2 = PolicyInformation.getInstance(objects.nextElement());
                ASN1ObjectIdentifier policyIdentifier = instance2.getPolicyIdentifier();
                hashSet.add(policyIdentifier.getId());
                if (!ANY_POLICY.equals(policyIdentifier.getId())) {
                    try {
                        Set qualifierSet = CertPathValidatorUtilities.getQualifierSet(instance2.getPolicyQualifiers());
                        if (!CertPathValidatorUtilities.processCertD1i(i4, listArr2, policyIdentifier, qualifierSet)) {
                            CertPathValidatorUtilities.processCertD1ii(i4, listArr2, policyIdentifier, qualifierSet);
                        }
                    } catch (CertPathValidatorException e) {
                        throw new ExtCertPathValidatorException("Policy qualifier info set could not be build.", e, certPath2, i3);
                    }
                }
            }
            if (set.isEmpty() || set2.contains(ANY_POLICY)) {
                set.clear();
                set2.addAll(hashSet);
            } else {
                HashSet hashSet2 = new HashSet();
                for (Object obj2 : set) {
                    if (hashSet.contains(obj2)) {
                        hashSet2.add(obj2);
                    }
                }
                set.clear();
                set2.addAll(hashSet2);
            }
            if (i2 > 0 || (i4 < size && CertPathValidatorUtilities.isSelfIssued(x509Certificate))) {
                Enumeration objects2 = instance.getObjects();
                while (objects2.hasMoreElements()) {
                    PolicyInformation instance3 = PolicyInformation.getInstance(objects2.nextElement());
                    if (ANY_POLICY.equals(instance3.getPolicyIdentifier().getId())) {
                        Set qualifierSet2 = CertPathValidatorUtilities.getQualifierSet(instance3.getPolicyQualifiers());
                        List list2 = listArr2[i4 - 1];
                        for (size = 0; size < list2.size(); size++) {
                            Set set3;
                            PKIXPolicyNode pKIXPolicyNode2 = (PKIXPolicyNode) list2.get(size);
                            Iterator it = pKIXPolicyNode2.getExpectedPolicies().iterator();
                            while (it.hasNext()) {
                                String str;
                                Iterator it2;
                                PKIXPolicyNode pKIXPolicyNode3;
                                Object next = it.next();
                                if (next instanceof String) {
                                    str = (String) next;
                                } else if (next instanceof ASN1ObjectIdentifier) {
                                    str = ((ASN1ObjectIdentifier) next).getId();
                                } else {
                                    set3 = qualifierSet2;
                                }
                                String str2 = str;
                                Iterator children = pKIXPolicyNode2.getChildren();
                                obj2 = null;
                                while (children.hasNext()) {
                                    if (str2.equals(((PKIXPolicyNode) children.next()).getValidPolicy())) {
                                        obj2 = 1;
                                    }
                                }
                                if (obj2 == null) {
                                    HashSet hashSet3 = new HashSet();
                                    hashSet3.add(str2);
                                    PKIXPolicyNode pKIXPolicyNode4 = r6;
                                    it2 = it;
                                    set3 = qualifierSet2;
                                    pKIXPolicyNode3 = pKIXPolicyNode2;
                                    PKIXPolicyNode pKIXPolicyNode5 = new PKIXPolicyNode(new ArrayList(), i4, hashSet3, pKIXPolicyNode2, qualifierSet2, str2, false);
                                    pKIXPolicyNode3.addChild(pKIXPolicyNode4);
                                    listArr2[i4].add(pKIXPolicyNode4);
                                } else {
                                    set3 = qualifierSet2;
                                    it2 = it;
                                    pKIXPolicyNode3 = pKIXPolicyNode2;
                                }
                                pKIXPolicyNode2 = pKIXPolicyNode3;
                                it = it2;
                                qualifierSet2 = set3;
                            }
                            set3 = qualifierSet2;
                        }
                    }
                }
            }
            PKIXPolicyNode pKIXPolicyNode6 = pKIXPolicyNode;
            for (int i5 = i4 - 1; i5 >= 0; i5--) {
                list = listArr2[i5];
                for (i3 = 0; i3 < list.size(); i3++) {
                    PKIXPolicyNode pKIXPolicyNode7 = (PKIXPolicyNode) list.get(i3);
                    if (!pKIXPolicyNode7.hasChildren()) {
                        pKIXPolicyNode7 = CertPathValidatorUtilities.removePolicyNode(pKIXPolicyNode6, listArr2, pKIXPolicyNode7);
                        if (pKIXPolicyNode7 == null) {
                            pKIXPolicyNode6 = pKIXPolicyNode7;
                            break;
                        }
                        pKIXPolicyNode6 = pKIXPolicyNode7;
                    }
                }
            }
            set2 = x509Certificate.getCriticalExtensionOIDs();
            if (set2 != null) {
                boolean contains = set2.contains(CERTIFICATE_POLICIES);
                list = listArr2[i4];
                for (i3 = 0; i3 < list.size(); i3++) {
                    ((PKIXPolicyNode) list.get(i3)).setCritical(contains);
                }
            }
            return pKIXPolicyNode6;
        } catch (AnnotatedException e2) {
            throw new ExtCertPathValidatorException("Could not read certificate policies extension from certificate.", e2, certPath2, i3);
        }
    }

    protected static PKIXPolicyNode processCertE(CertPath certPath, int i, PKIXPolicyNode pKIXPolicyNode) throws CertPathValidatorException {
        try {
            return ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), CERTIFICATE_POLICIES)) == null ? null : pKIXPolicyNode;
        } catch (AnnotatedException e) {
            throw new ExtCertPathValidatorException("Could not read certificate policies extension from certificate.", e, certPath, i);
        }
    }

    protected static void processCertF(CertPath certPath, int i, PKIXPolicyNode pKIXPolicyNode, int i2) throws CertPathValidatorException {
        if (i2 <= 0 && pKIXPolicyNode == null) {
            throw new ExtCertPathValidatorException("No valid policy tree found when one expected.", null, certPath, i);
        }
    }

    protected static int wrapupCertA(int i, X509Certificate x509Certificate) {
        return (CertPathValidatorUtilities.isSelfIssued(x509Certificate) || i == 0) ? i : i - 1;
    }

    protected static int wrapupCertB(CertPath certPath, int i, int i2) throws CertPathValidatorException {
        try {
            ASN1Sequence instance = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(i), POLICY_CONSTRAINTS));
            if (instance != null) {
                Enumeration objects = instance.getObjects();
                while (objects.hasMoreElements()) {
                    ASN1TaggedObject aSN1TaggedObject = (ASN1TaggedObject) objects.nextElement();
                    if (aSN1TaggedObject.getTagNo() == 0) {
                        try {
                            if (ASN1Integer.getInstance(aSN1TaggedObject, false).getValue().intValue() == 0) {
                                return 0;
                            }
                        } catch (Exception e) {
                            throw new ExtCertPathValidatorException("Policy constraints requireExplicitPolicy field could not be decoded.", e, certPath, i);
                        }
                    }
                }
            }
            return i2;
        } catch (AnnotatedException e2) {
            throw new ExtCertPathValidatorException("Policy constraints could not be decoded.", e2, certPath, i);
        }
    }

    protected static void wrapupCertF(CertPath certPath, int i, List list, Set set) throws CertPathValidatorException {
        X509Certificate x509Certificate = (X509Certificate) certPath.getCertificates().get(i);
        for (PKIXCertPathChecker check : list) {
            try {
                check.check(x509Certificate, set);
            } catch (CertPathValidatorException e) {
                throw new ExtCertPathValidatorException("Additional certificate path checker failed.", e, certPath, i);
            }
        }
        if (!set.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Certificate has unsupported critical extension: ");
            stringBuilder.append(set);
            throw new ExtCertPathValidatorException(stringBuilder.toString(), null, certPath, i);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:79:0x0140 in {4, 6, 24, 25, 26, 30, 39, 40, 41, 43, 45, 59, 60, 61, 67, 76, 77, 78} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    protected static org.bouncycastle.jce.provider.PKIXPolicyNode wrapupCertG(java.security.cert.CertPath r6, org.bouncycastle.jcajce.PKIXExtendedParameters r7, java.util.Set r8, int r9, java.util.List[] r10, org.bouncycastle.jce.provider.PKIXPolicyNode r11, java.util.Set r12) throws java.security.cert.CertPathValidatorException {
        /*
        r0 = r6.getCertificates();
        r0 = r0.size();
        r1 = 0;
        if (r11 != 0) goto L_0x001a;
        r7 = r7.isExplicitPolicyRequired();
        if (r7 != 0) goto L_0x0012;
        return r1;
        r7 = new org.bouncycastle.jce.exception.ExtCertPathValidatorException;
        r8 = "Explicit policy requested but none available.";
        r7.<init>(r8, r1, r6, r9);
        throw r7;
        r2 = org.bouncycastle.jce.provider.CertPathValidatorUtilities.isAnyPolicy(r8);
        r3 = 0;
        if (r2 == 0) goto L_0x00b1;
        r7 = r7.isExplicitPolicyRequired();
        if (r7 == 0) goto L_0x00af;
        r7 = r12.isEmpty();
        if (r7 != 0) goto L_0x00a7;
        r6 = new java.util.HashSet;
        r6.<init>();
        r7 = r3;
        r8 = r10.length;
        if (r7 >= r8) goto L_0x0069;
        r8 = r10[r7];
        r9 = r3;
        r1 = r8.size();
        if (r9 >= r1) goto L_0x0066;
        r1 = r8.get(r9);
        r1 = (org.bouncycastle.jce.provider.PKIXPolicyNode) r1;
        r2 = "2.5.29.32.0";
        r4 = r1.getValidPolicy();
        r2 = r2.equals(r4);
        if (r2 == 0) goto L_0x0063;
        r1 = r1.getChildren();
        r2 = r1.hasNext();
        if (r2 == 0) goto L_0x0063;
        r2 = r1.next();
        r6.add(r2);
        goto L_0x0055;
        r9 = r9 + 1;
        goto L_0x0039;
        r7 = r7 + 1;
        goto L_0x0033;
        r6 = r6.iterator();
        r7 = r6.hasNext();
        if (r7 == 0) goto L_0x0081;
        r7 = r6.next();
        r7 = (org.bouncycastle.jce.provider.PKIXPolicyNode) r7;
        r7 = r7.getValidPolicy();
        r12.contains(r7);
        goto L_0x006d;
        if (r11 == 0) goto L_0x00af;
        r0 = r0 + -1;
        if (r0 < 0) goto L_0x00af;
        r6 = r10[r0];
        r7 = r3;
        r8 = r6.size();
        if (r7 >= r8) goto L_0x00a4;
        r8 = r6.get(r7);
        r8 = (org.bouncycastle.jce.provider.PKIXPolicyNode) r8;
        r9 = r8.hasChildren();
        if (r9 != 0) goto L_0x00a1;
        r8 = org.bouncycastle.jce.provider.CertPathValidatorUtilities.removePolicyNode(r11, r10, r8);
        r11 = r8;
        r7 = r7 + 1;
        goto L_0x008a;
        r0 = r0 + -1;
        goto L_0x0085;
        r7 = new org.bouncycastle.jce.exception.ExtCertPathValidatorException;
        r8 = "Explicit policy requested but none available.";
        r7.<init>(r8, r1, r6, r9);
        throw r7;
        r1 = r11;
        return r1;
        r6 = new java.util.HashSet;
        r6.<init>();
        r7 = r3;
        r9 = r10.length;
        if (r7 >= r9) goto L_0x00fb;
        r9 = r10[r7];
        r12 = r3;
        r1 = r9.size();
        if (r12 >= r1) goto L_0x00f8;
        r1 = r9.get(r12);
        r1 = (org.bouncycastle.jce.provider.PKIXPolicyNode) r1;
        r2 = "2.5.29.32.0";
        r4 = r1.getValidPolicy();
        r2 = r2.equals(r4);
        if (r2 == 0) goto L_0x00f5;
        r1 = r1.getChildren();
        r2 = r1.hasNext();
        if (r2 == 0) goto L_0x00f5;
        r2 = r1.next();
        r2 = (org.bouncycastle.jce.provider.PKIXPolicyNode) r2;
        r4 = "2.5.29.32.0";
        r5 = r2.getValidPolicy();
        r4 = r4.equals(r5);
        if (r4 != 0) goto L_0x00d9;
        r6.add(r2);
        goto L_0x00d9;
        r12 = r12 + 1;
        goto L_0x00bd;
        r7 = r7 + 1;
        goto L_0x00b7;
        r6 = r6.iterator();
        r7 = r6.hasNext();
        if (r7 == 0) goto L_0x011b;
        r7 = r6.next();
        r7 = (org.bouncycastle.jce.provider.PKIXPolicyNode) r7;
        r9 = r7.getValidPolicy();
        r9 = r8.contains(r9);
        if (r9 != 0) goto L_0x00ff;
        r7 = org.bouncycastle.jce.provider.CertPathValidatorUtilities.removePolicyNode(r11, r10, r7);
        r11 = r7;
        goto L_0x00ff;
        if (r11 == 0) goto L_0x00af;
        r0 = r0 + -1;
        if (r0 < 0) goto L_0x00af;
        r6 = r10[r0];
        r7 = r3;
        r8 = r6.size();
        if (r7 >= r8) goto L_0x013d;
        r8 = r6.get(r7);
        r8 = (org.bouncycastle.jce.provider.PKIXPolicyNode) r8;
        r9 = r8.hasChildren();
        if (r9 != 0) goto L_0x013a;
        r11 = org.bouncycastle.jce.provider.CertPathValidatorUtilities.removePolicyNode(r11, r10, r8);
        r7 = r7 + 1;
        goto L_0x0124;
        r0 = r0 + -1;
        goto L_0x011f;
        return r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.RFC3280CertPathUtilities.wrapupCertG(java.security.cert.CertPath, org.bouncycastle.jcajce.PKIXExtendedParameters, java.util.Set, int, java.util.List[], org.bouncycastle.jce.provider.PKIXPolicyNode, java.util.Set):org.bouncycastle.jce.provider.PKIXPolicyNode");
    }
}
