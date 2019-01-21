package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.x500.X500Name;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.Extension;
import com.android.org.bouncycastle.jcajce.PKIXExtendedBuilderParameters;
import com.android.org.bouncycastle.jcajce.PKIXExtendedParameters;
import com.android.org.bouncycastle.jcajce.PKIXExtendedParameters.Builder;
import com.android.org.bouncycastle.jcajce.util.BCJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.exception.ExtCertPathValidatorException;
import com.android.org.bouncycastle.x509.ExtendedPKIXParameters;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertPathValidatorSpi;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PKIXCertPathValidatorSpi extends CertPathValidatorSpi {
    private final JcaJceHelper helper = new BCJcaJceHelper();

    private static class NoPreloadHolder {
        private static final CertBlacklist blacklist = new CertBlacklist();

        private NoPreloadHolder() {
        }
    }

    public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters params) throws CertPathValidatorException, InvalidAlgorithmParameterException {
        PKIXExtendedParameters paramsPKIX;
        PKIXCertPathValidatorSpi pKIXCertPathValidatorSpi;
        CertPath certPath2;
        IllegalArgumentException ex;
        int i;
        int validPolicyTree;
        TrustAnchor nameConstraintValidator;
        List list;
        PKIXCertPathValidatorSpi pKIXCertPathValidatorSpi2 = this;
        CertPath certPath3 = certPath;
        CertPathParameters certPathParameters = params;
        if (certPathParameters instanceof PKIXParameters) {
            paramsPKIX = new Builder((PKIXParameters) certPathParameters);
            if (certPathParameters instanceof ExtendedPKIXParameters) {
                ExtendedPKIXParameters extPKIX = (ExtendedPKIXParameters) certPathParameters;
                paramsPKIX.setUseDeltasEnabled(extPKIX.isUseDeltasEnabled());
                paramsPKIX.setValidityModel(extPKIX.getValidityModel());
            }
            paramsPKIX = paramsPKIX.build();
        } else if (certPathParameters instanceof PKIXExtendedBuilderParameters) {
            paramsPKIX = ((PKIXExtendedBuilderParameters) certPathParameters).getBaseParameters();
        } else if (certPathParameters instanceof PKIXExtendedParameters) {
            paramsPKIX = (PKIXExtendedParameters) certPathParameters;
        } else {
            pKIXCertPathValidatorSpi = pKIXCertPathValidatorSpi2;
            certPath2 = certPath3;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Parameters must be a ");
            stringBuilder.append(PKIXParameters.class.getName());
            stringBuilder.append(" instance.");
            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
        }
        PKIXExtendedParameters paramsPKIX2 = paramsPKIX;
        if (paramsPKIX2.getTrustAnchors() != null) {
            List certs = certPath.getCertificates();
            int n = certs.size();
            List list2;
            if (certs.isEmpty()) {
                list2 = certs;
                n = pKIXCertPathValidatorSpi2;
                throw new CertPathValidatorException("Certification path is empty.", null, certPath3, -1);
            }
            String message;
            X509Certificate cert = (X509Certificate) certs.get(0);
            if (cert != null) {
                BigInteger serial = cert.getSerialNumber();
                if (NoPreloadHolder.blacklist.isSerialNumberBlackListed(serial)) {
                    message = new StringBuilder();
                    message.append("Certificate revocation of serial 0x");
                    message.append(serial.toString(16));
                    message = message.toString();
                    System.out.println(message);
                    AnnotatedException e = new AnnotatedException(message);
                    throw new CertPathValidatorException(e.getMessage(), e, certPath3, 0);
                }
            }
            Set userInitialPolicySet = paramsPKIX2.getInitialPolicies();
            int i2;
            try {
                TrustAnchor trust = CertPathValidatorUtilities.findTrustAnchor((X509Certificate) certs.get(certs.size() - 1), paramsPKIX2.getTrustAnchors(), paramsPKIX2.getSigProvider());
                if (trust != null) {
                    int explicitPolicy;
                    int inhibitAnyPolicy;
                    int policyMapping;
                    X500Name workingIssuerName;
                    PublicKey workingPublicKey;
                    PKIXPolicyNode pKIXPolicyNode;
                    Set set;
                    PKIXExtendedParameters pKIXExtendedParameters;
                    PKIXNameConstraintValidator paramsPKIX3;
                    PKIXExtendedParameters paramsPKIX4 = new Builder(paramsPKIX2).setTrustAnchor(trust).build();
                    List[] policyNodes = new ArrayList[(n + 1)];
                    for (int j = 0; j < policyNodes.length; j++) {
                        policyNodes[j] = new ArrayList();
                    }
                    Set policySet = new HashSet();
                    policySet.add(RFC3280CertPathUtilities.ANY_POLICY);
                    PKIXPolicyNode validPolicyTree2 = new PKIXPolicyNode(new ArrayList(), 0, policySet, null, new HashSet(), RFC3280CertPathUtilities.ANY_POLICY, false);
                    policyNodes[0].add(validPolicyTree2);
                    PKIXNameConstraintValidator nameConstraintValidator2 = new PKIXNameConstraintValidator();
                    Set acceptablePolicies = new HashSet();
                    if (paramsPKIX4.isExplicitPolicyRequired()) {
                        explicitPolicy = 0;
                    } else {
                        explicitPolicy = n + 1;
                    }
                    if (paramsPKIX4.isAnyPolicyInhibited()) {
                        inhibitAnyPolicy = 0;
                    } else {
                        inhibitAnyPolicy = n + 1;
                    }
                    if (paramsPKIX4.isPolicyMappingInhibited()) {
                        policyMapping = 0;
                    } else {
                        policyMapping = n + 1;
                    }
                    X509Certificate sign = trust.getTrustedCert();
                    if (sign != null) {
                        try {
                            workingIssuerName = PrincipalUtils.getSubjectPrincipal(sign);
                            workingPublicKey = sign.getPublicKey();
                        } catch (IllegalArgumentException e2) {
                            ex = e2;
                            i = 0;
                            pKIXPolicyNode = validPolicyTree2;
                            set = policySet;
                            pKIXExtendedParameters = paramsPKIX4;
                            i2 = n;
                            validPolicyTree = -1;
                            n = pKIXCertPathValidatorSpi2;
                            paramsPKIX3 = nameConstraintValidator2;
                            nameConstraintValidator = trust;
                            certPath2 = certPath3;
                            list = certs;
                            certs = policyNodes;
                            policyNodes = list;
                        }
                    } else {
                        try {
                            workingIssuerName = PrincipalUtils.getCA(trust);
                            workingPublicKey = trust.getCAPublicKey();
                        } catch (IllegalArgumentException e3) {
                            ex = e3;
                            i = 0;
                            pKIXPolicyNode = validPolicyTree2;
                            set = policySet;
                            pKIXExtendedParameters = paramsPKIX4;
                            i2 = n;
                            validPolicyTree = -1;
                            n = pKIXCertPathValidatorSpi2;
                            certPath2 = certPath3;
                            list = certs;
                            certs = policyNodes;
                            list2 = list;
                            throw new ExtCertPathValidatorException("Subject of trust anchor could not be (re)encoded.", ex, certPath2, validPolicyTree);
                        }
                    }
                    PublicKey workingPublicKey2 = workingPublicKey;
                    AlgorithmIdentifier workingAlgId = null;
                    PublicKey workingPublicKey3 = workingPublicKey2;
                    PublicKey workingPublicKey4;
                    try {
                        Iterator certIter;
                        Set acceptablePolicies2;
                        TrustAnchor trust2;
                        int index;
                        PublicKey workingPublicKey5;
                        int policyMapping2;
                        int inhibitAnyPolicy2;
                        List policySet2;
                        int maxPathLength;
                        AlgorithmIdentifier workingAlgId2 = CertPathValidatorUtilities.getAlgorithmIdentifier(workingPublicKey3);
                        ASN1ObjectIdentifier workingPublicKeyAlgorithm = workingAlgId2.getAlgorithm();
                        ASN1Encodable workingPublicKeyParameters = workingAlgId2.getParameters();
                        int maxPathLength2 = n;
                        if (paramsPKIX4.getTargetConstraints() != null) {
                            i = 0;
                            if (paramsPKIX4.getTargetConstraints().match((X509Certificate) certs.get(0))) {
                                pKIXPolicyNode = validPolicyTree2;
                                workingPublicKey4 = workingPublicKey3;
                            } else {
                                throw new ExtCertPathValidatorException("Target certificate in certification path does not match targetConstraints.", null, certPath3, null);
                            }
                        }
                        i = 0;
                        pKIXPolicyNode = validPolicyTree2;
                        workingPublicKey4 = workingPublicKey3;
                        List pathCheckers = paramsPKIX4.getCertPathCheckers();
                        Iterator certIter2 = pathCheckers.iterator();
                        while (true) {
                            certIter = certIter2;
                            if (!certIter.hasNext()) {
                                break;
                            }
                            acceptablePolicies2 = acceptablePolicies;
                            ((PKIXCertPathChecker) certIter.next()).init(null);
                            certIter2 = certIter;
                            acceptablePolicies = acceptablePolicies2;
                        }
                        acceptablePolicies2 = acceptablePolicies;
                        workingPublicKey3 = null;
                        int explicitPolicy2 = explicitPolicy;
                        int inhibitAnyPolicy3 = inhibitAnyPolicy;
                        int policyMapping3 = policyMapping;
                        int maxPathLength3 = maxPathLength2;
                        PublicKey workingPublicKey6 = workingPublicKey4;
                        X509Certificate sign2 = sign;
                        int index2 = certs.size() - 1;
                        X500Name workingIssuerName2 = workingIssuerName;
                        while (index2 >= 0) {
                            Iterator certIter3 = certIter;
                            Iterator certIter4;
                            if (NoPreloadHolder.blacklist.isPublicKeyBlackListed(workingPublicKey6) == null) {
                                List[] policyNodes2;
                                X509Certificate cert2;
                                PublicKey workingPublicKey7;
                                X509Certificate workingPublicKey8 = (X509Certificate) certs.get(index2);
                                int i3 = n - index2;
                                PKIXNameConstraintValidator nameConstraintValidator3 = nameConstraintValidator2;
                                boolean verificationAlreadyPerformed = index2 == certs.size() + -1 ? 1 : null;
                                Iterator it = pKIXCertPathValidatorSpi2.helper;
                                certIter4 = certIter3;
                                List pathCheckers2 = pathCheckers;
                                set = policySet;
                                List[] policyNodes3 = policyNodes;
                                pKIXExtendedParameters = paramsPKIX4;
                                trust2 = trust;
                                int i4 = i3;
                                index = index2;
                                RFC3280CertPathUtilities.processCertA(certPath3, paramsPKIX4, index2, workingPublicKey6, verificationAlreadyPerformed, workingIssuerName2, sign2, it);
                                RFC3280CertPathUtilities.processCertBC(certPath3, index, nameConstraintValidator3);
                                X509Certificate certIter5 = workingPublicKey8;
                                policySet = n;
                                list2 = certs;
                                paramsPKIX3 = nameConstraintValidator3;
                                certPath2 = certPath3;
                                workingPublicKey5 = workingPublicKey6;
                                acceptablePolicies = acceptablePolicies2;
                                workingPublicKey3 = RFC3280CertPathUtilities.processCertE(certPath2, index, RFC3280CertPathUtilities.processCertD(certPath3, index, acceptablePolicies, pKIXPolicyNode, policyNodes3, inhibitAnyPolicy3));
                                RFC3280CertPathUtilities.processCertF(certPath2, index, workingPublicKey3, explicitPolicy2);
                                n = i4;
                                if (n != policySet) {
                                    Set criticalExtensions;
                                    if (certIter5 != null) {
                                        if (certIter5.getVersion() == 1) {
                                            throw new CertPathValidatorException("Version 1 certificates can't be used as CA ones.", null, certPath2, index);
                                        }
                                    }
                                    RFC3280CertPathUtilities.prepareNextCertA(certPath2, index);
                                    policyMapping2 = policyMapping3;
                                    policyNodes2 = policyNodes3;
                                    workingPublicKey3 = RFC3280CertPathUtilities.prepareCertB(certPath2, index, policyNodes2, workingPublicKey3, policyMapping2);
                                    RFC3280CertPathUtilities.prepareNextCertG(certPath2, index, paramsPKIX3);
                                    explicitPolicy2 = RFC3280CertPathUtilities.prepareNextCertH1(certPath2, index, explicitPolicy2);
                                    policyMapping2 = RFC3280CertPathUtilities.prepareNextCertH2(certPath2, index, policyMapping2);
                                    inhibitAnyPolicy2 = RFC3280CertPathUtilities.prepareNextCertH3(certPath2, index, inhibitAnyPolicy3);
                                    inhibitAnyPolicy = RFC3280CertPathUtilities.prepareNextCertI1(certPath2, index, explicitPolicy2);
                                    policyMapping2 = RFC3280CertPathUtilities.prepareNextCertI2(certPath2, index, policyMapping2);
                                    inhibitAnyPolicy3 = RFC3280CertPathUtilities.prepareNextCertJ(certPath2, index, inhibitAnyPolicy2);
                                    RFC3280CertPathUtilities.prepareNextCertK(certPath2, index);
                                    inhibitAnyPolicy2 = RFC3280CertPathUtilities.prepareNextCertM(certPath2, index, RFC3280CertPathUtilities.prepareNextCertL(certPath2, index, maxPathLength3));
                                    RFC3280CertPathUtilities.prepareNextCertN(certPath2, index);
                                    Set criticalExtensions2 = certIter5.getCriticalExtensionOIDs();
                                    if (criticalExtensions2 != null) {
                                        criticalExtensions2 = new HashSet(criticalExtensions2);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.KEY_USAGE);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                                        criticalExtensions2.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                                        criticalExtensions = criticalExtensions2;
                                    } else {
                                        criticalExtensions = new HashSet();
                                    }
                                    i2 = policySet;
                                    policySet = pathCheckers2;
                                    RFC3280CertPathUtilities.prepareNextCertO(certPath2, index, criticalExtensions, policySet);
                                    X509Certificate sign3 = certIter5;
                                    cert2 = certIter5;
                                    workingIssuerName2 = PrincipalUtils.getSubjectPrincipal(certIter5);
                                    X509Certificate sign4;
                                    try {
                                        sign4 = certIter5;
                                        pKIXCertPathValidatorSpi = this;
                                        try {
                                            workingPublicKey7 = CertPathValidatorUtilities.getNextWorkingKey(certPath.getCertificates(), index, pKIXCertPathValidatorSpi.helper);
                                            certIter = CertPathValidatorUtilities.getAlgorithmIdentifier(workingPublicKey7);
                                            workingPublicKey5 = certIter.getAlgorithm();
                                            Iterator it2 = certIter;
                                            Set parameters = certIter.getParameters();
                                            PublicKey publicKey = workingPublicKey5;
                                            pKIXPolicyNode = workingPublicKey3;
                                            maxPathLength3 = inhibitAnyPolicy2;
                                            policyMapping3 = policyMapping2;
                                            sign2 = sign4;
                                        } catch (CertPathValidatorException e4) {
                                            explicitPolicy2 = e4;
                                            throw new CertPathValidatorException("Next working key could not be retrieved.", explicitPolicy2, certPath2, index);
                                        }
                                    } catch (CertPathValidatorException e5) {
                                        explicitPolicy2 = e5;
                                        sign4 = certIter5;
                                        policyMapping = n;
                                        throw new CertPathValidatorException("Next working key could not be retrieved.", explicitPolicy2, certPath2, index);
                                    }
                                }
                                cert2 = certIter5;
                                i2 = policySet;
                                policyMapping = n;
                                inhibitAnyPolicy2 = inhibitAnyPolicy3;
                                policyMapping2 = policyMapping3;
                                certIter = maxPathLength3;
                                policySet = pathCheckers2;
                                policyNodes2 = policyNodes3;
                                pKIXCertPathValidatorSpi = this;
                                inhibitAnyPolicy = explicitPolicy2;
                                workingPublicKey7 = workingPublicKey5;
                                pKIXPolicyNode = workingPublicKey3;
                                index2 = index - 1;
                                certPathParameters = params;
                                Object pathCheckers3 = policySet;
                                nameConstraintValidator2 = paramsPKIX3;
                                certPath3 = certPath2;
                                pKIXCertPathValidatorSpi2 = pKIXCertPathValidatorSpi;
                                acceptablePolicies2 = acceptablePolicies;
                                certIter = certIter4;
                                policySet = set;
                                paramsPKIX4 = pKIXExtendedParameters;
                                trust = trust2;
                                n = i2;
                                Object workingPublicKey9 = cert2;
                                workingPublicKey6 = workingPublicKey7;
                                explicitPolicy2 = inhibitAnyPolicy;
                                List[] listArr = policyNodes2;
                                certs = list2;
                                policyNodes = listArr;
                            } else {
                                pKIXExtendedParameters = paramsPKIX4;
                                trust2 = trust;
                                i2 = n;
                                certPath2 = certPath3;
                                policyMapping2 = policyMapping3;
                                certIter = maxPathLength3;
                                certIter4 = certIter3;
                                n = pKIXCertPathValidatorSpi2;
                                policySet2 = pathCheckers3;
                                paramsPKIX3 = nameConstraintValidator2;
                                index = index2;
                                index2 = workingPublicKey6;
                                acceptablePolicies = acceptablePolicies2;
                                list = certs;
                                certs = policyNodes;
                                policyNodes = list;
                                message = new StringBuilder();
                                message.append("Certificate revocation of public key ");
                                message.append(index2);
                                message = message.toString();
                                System.out.println(message);
                                nameConstraintValidator2 = new AnnotatedException(message);
                                maxPathLength = certIter;
                                throw new CertPathValidatorException(nameConstraintValidator2.getMessage(), nameConstraintValidator2, certPath2, index);
                            }
                        }
                        set = policySet;
                        pKIXExtendedParameters = paramsPKIX4;
                        trust2 = trust;
                        i2 = n;
                        certPath2 = certPath3;
                        inhibitAnyPolicy2 = inhibitAnyPolicy3;
                        policyMapping2 = policyMapping3;
                        maxPathLength = maxPathLength3;
                        n = pKIXCertPathValidatorSpi2;
                        policySet2 = pathCheckers3;
                        paramsPKIX3 = nameConstraintValidator2;
                        index = index2;
                        workingPublicKey5 = workingPublicKey6;
                        acceptablePolicies = acceptablePolicies2;
                        list = certs;
                        certs = policyNodes;
                        policyNodes = list;
                        explicitPolicy2 = RFC3280CertPathUtilities.wrapupCertB(certPath2, index + 1, RFC3280CertPathUtilities.wrapupCertA(explicitPolicy2, workingPublicKey3));
                        Set criticalExtensions3 = workingPublicKey3.getCriticalExtensionOIDs();
                        if (criticalExtensions3 != null) {
                            criticalExtensions3 = new HashSet(criticalExtensions3);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.KEY_USAGE);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.CERTIFICATE_POLICIES);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.POLICY_MAPPINGS);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.INHIBIT_ANY_POLICY);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.POLICY_CONSTRAINTS);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.BASIC_CONSTRAINTS);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.SUBJECT_ALTERNATIVE_NAME);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.NAME_CONSTRAINTS);
                            criticalExtensions3.remove(RFC3280CertPathUtilities.CRL_DISTRIBUTION_POINTS);
                            criticalExtensions3.remove(Extension.extendedKeyUsage.getId());
                        } else {
                            criticalExtensions3 = new HashSet();
                        }
                        RFC3280CertPathUtilities.wrapupCertF(certPath2, index + 1, policySet2, criticalExtensions3);
                        validPolicyTree2 = RFC3280CertPathUtilities.wrapupCertG(certPath2, pKIXExtendedParameters, userInitialPolicySet, index + 1, certs, pKIXPolicyNode, acceptablePolicies);
                        Set set2;
                        if (explicitPolicy2 > 0) {
                            set2 = criticalExtensions3;
                        } else if (validPolicyTree2 != null) {
                            int i5 = explicitPolicy2;
                            set2 = criticalExtensions3;
                        } else {
                            throw new CertPathValidatorException("Path processing failed on policy.", null, certPath2, index);
                        }
                        return new PKIXCertPathValidatorResult(trust2, validPolicyTree2, workingPublicKey3.getPublicKey());
                    } catch (CertPathValidatorException e6) {
                        i = 0;
                        pKIXPolicyNode = validPolicyTree2;
                        set = policySet;
                        pKIXExtendedParameters = paramsPKIX4;
                        workingPublicKey4 = workingPublicKey3;
                        i2 = n;
                        n = pKIXCertPathValidatorSpi2;
                        paramsPKIX3 = nameConstraintValidator2;
                        nameConstraintValidator = trust;
                        list = certs;
                        certs = policyNodes;
                        policyNodes = list;
                        CertPathValidatorException certPathValidatorException = e6;
                        throw new ExtCertPathValidatorException("Algorithm identifier of public key of trust anchor could not be read.", e6, certPath3, -1);
                    }
                }
                i2 = n;
                list2 = certs;
                n = pKIXCertPathValidatorSpi2;
                throw new CertPathValidatorException("Trust anchor for certification path not found.", null, certPath3, -1);
            } catch (AnnotatedException e7) {
                i2 = n;
                n = pKIXCertPathValidatorSpi2;
                throw new CertPathValidatorException(e7.getMessage(), e7, certPath3, certs.size() - 1);
            }
        }
        pKIXCertPathValidatorSpi = pKIXCertPathValidatorSpi2;
        certPath2 = certPath3;
        throw new InvalidAlgorithmParameterException("trustAnchors is null, this is not allowed for certification path validation.");
    }
}
