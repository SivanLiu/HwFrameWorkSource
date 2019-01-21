package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1String;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.x500.RDN;
import com.android.org.bouncycastle.asn1.x500.X500Name;
import com.android.org.bouncycastle.asn1.x500.style.BCStyle;
import com.android.org.bouncycastle.asn1.x509.BasicConstraints;
import com.android.org.bouncycastle.asn1.x509.CRLDistPoint;
import com.android.org.bouncycastle.asn1.x509.DistributionPoint;
import com.android.org.bouncycastle.asn1.x509.DistributionPointName;
import com.android.org.bouncycastle.asn1.x509.Extension;
import com.android.org.bouncycastle.asn1.x509.GeneralName;
import com.android.org.bouncycastle.asn1.x509.GeneralNames;
import com.android.org.bouncycastle.asn1.x509.GeneralSubtree;
import com.android.org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import com.android.org.bouncycastle.asn1.x509.NameConstraints;
import com.android.org.bouncycastle.asn1.x509.PolicyInformation;
import com.android.org.bouncycastle.jcajce.PKIXCRLStore;
import com.android.org.bouncycastle.jcajce.PKIXCRLStoreSelector;
import com.android.org.bouncycastle.jcajce.PKIXCertStoreSelector;
import com.android.org.bouncycastle.jcajce.PKIXCertStoreSelector.Builder;
import com.android.org.bouncycastle.jcajce.PKIXExtendedBuilderParameters;
import com.android.org.bouncycastle.jcajce.PKIXExtendedParameters;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.exception.ExtCertPathValidatorException;
import com.android.org.bouncycastle.util.Arrays;
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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

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

    protected static void processCRLB2(DistributionPoint dp, Object cert, X509CRL crl) throws AnnotatedException {
        IssuingDistributionPoint idp = null;
        try {
            idp = IssuingDistributionPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(crl, ISSUING_DISTRIBUTION_POINT));
            if (idp != null) {
                if (idp.getDistributionPoint() != null) {
                    DistributionPointName dpName = IssuingDistributionPoint.getInstance(idp).getDistributionPoint();
                    List names = new ArrayList();
                    int j = 0;
                    if (dpName.getType() == 0) {
                        GeneralName[] genNames = GeneralNames.getInstance(dpName.getName()).getNames();
                        for (Object add : genNames) {
                            names.add(add);
                        }
                    }
                    if (dpName.getType() == 1) {
                        ASN1EncodableVector vec = new ASN1EncodableVector();
                        try {
                            Enumeration e = ASN1Sequence.getInstance(PrincipalUtils.getIssuerPrincipal(crl)).getObjects();
                            while (e.hasMoreElements()) {
                                vec.add((ASN1Encodable) e.nextElement());
                            }
                            vec.add(dpName.getName());
                            names.add(new GeneralName(X500Name.getInstance(new DERSequence(vec))));
                        } catch (Exception e2) {
                            throw new AnnotatedException("Could not read CRL issuer.", e2);
                        }
                    }
                    boolean matches = false;
                    GeneralName[] genNames2;
                    if (dp.getDistributionPoint() != null) {
                        dpName = dp.getDistributionPoint();
                        GeneralName[] genNames3 = null;
                        if (dpName.getType() == 0) {
                            genNames3 = GeneralNames.getInstance(dpName.getName()).getNames();
                        }
                        if (dpName.getType() == 1) {
                            if (dp.getCRLIssuer() != null) {
                                genNames2 = dp.getCRLIssuer().getNames();
                            } else {
                                genNames2 = new GeneralName[1];
                                try {
                                    genNames2[0] = new GeneralName(X500Name.getInstance(PrincipalUtils.getEncodedIssuerPrincipal(cert).getEncoded()));
                                } catch (Exception e22) {
                                    throw new AnnotatedException("Could not read certificate issuer.", e22);
                                }
                            }
                            genNames3 = genNames2;
                            for (genNames2 = null; genNames2 < genNames3.length; genNames2++) {
                                Enumeration e3 = ASN1Sequence.getInstance(genNames3[genNames2].getName().toASN1Primitive()).getObjects();
                                ASN1EncodableVector vec2 = new ASN1EncodableVector();
                                while (e3.hasMoreElements()) {
                                    vec2.add((ASN1Encodable) e3.nextElement());
                                }
                                vec2.add(dpName.getName());
                                genNames3[genNames2] = new GeneralName(X500Name.getInstance(new DERSequence(vec2)));
                            }
                        }
                        if (genNames3 != null) {
                            while (j < genNames3.length) {
                                if (names.contains(genNames3[j])) {
                                    matches = true;
                                    break;
                                }
                                j++;
                            }
                        }
                        if (!matches) {
                            throw new AnnotatedException("No match for certificate CRL issuing distribution point name to cRLIssuer CRL distribution point.");
                        }
                    } else if (dp.getCRLIssuer() != null) {
                        genNames2 = dp.getCRLIssuer().getNames();
                        while (j < genNames2.length) {
                            if (names.contains(genNames2[j])) {
                                matches = true;
                                break;
                            }
                            j++;
                        }
                        if (!matches) {
                            throw new AnnotatedException("No match for certificate CRL issuing distribution point name to cRLIssuer CRL distribution point.");
                        }
                    } else {
                        throw new AnnotatedException("Either the cRLIssuer or the distributionPoint field must be contained in DistributionPoint.");
                    }
                }
                try {
                    BasicConstraints bc = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Extension) cert, BASIC_CONSTRAINTS));
                    if (cert instanceof X509Certificate) {
                        if (idp.onlyContainsUserCerts() && bc != null && bc.isCA()) {
                            throw new AnnotatedException("CA Cert CRL only contains user certificates.");
                        } else if (idp.onlyContainsCACerts() && (bc == null || !bc.isCA())) {
                            throw new AnnotatedException("End CRL only contains CA certificates.");
                        }
                    }
                    if (idp.onlyContainsAttributeCerts()) {
                        throw new AnnotatedException("onlyContainsAttributeCerts boolean is asserted.");
                    }
                } catch (Exception e4) {
                    throw new AnnotatedException("Basic constraints extension could not be decoded.", e4);
                }
            }
        } catch (Exception e222) {
            throw new AnnotatedException("Issuing distribution point extension could not be decoded.", e222);
        }
    }

    protected static void processCRLB1(DistributionPoint dp, Object cert, X509CRL crl) throws AnnotatedException {
        ASN1Primitive idp = CertPathValidatorUtilities.getExtensionValue(crl, ISSUING_DISTRIBUTION_POINT);
        boolean isIndirect = false;
        if (idp != null && IssuingDistributionPoint.getInstance(idp).isIndirectCRL()) {
            isIndirect = true;
        }
        try {
            byte[] issuerBytes = PrincipalUtils.getIssuerPrincipal(crl).getEncoded();
            boolean matchIssuer = false;
            if (dp.getCRLIssuer() != null) {
                GeneralName[] genNames = dp.getCRLIssuer().getNames();
                for (int j = 0; j < genNames.length; j++) {
                    if (genNames[j].getTagNo() == 4) {
                        try {
                            if (Arrays.areEqual(genNames[j].getName().toASN1Primitive().getEncoded(), issuerBytes)) {
                                matchIssuer = true;
                            }
                        } catch (IOException e) {
                            throw new AnnotatedException("CRL issuer information from distribution point cannot be decoded.", e);
                        }
                    }
                }
                if (matchIssuer && !isIndirect) {
                    throw new AnnotatedException("Distribution point contains cRLIssuer field but CRL is not indirect.");
                } else if (!matchIssuer) {
                    throw new AnnotatedException("CRL issuer of CRL does not match CRL issuer of distribution point.");
                }
            } else if (PrincipalUtils.getIssuerPrincipal(crl).equals(PrincipalUtils.getEncodedIssuerPrincipal(cert))) {
                matchIssuer = true;
            }
            if (!matchIssuer) {
                throw new AnnotatedException("Cannot find matching CRL issuer for certificate.");
            }
        } catch (IOException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception encoding CRL issuer: ");
            stringBuilder.append(e2.getMessage());
            throw new AnnotatedException(stringBuilder.toString(), e2);
        }
    }

    protected static ReasonsMask processCRLD(X509CRL crl, DistributionPoint dp) throws AnnotatedException {
        try {
            IssuingDistributionPoint idp = IssuingDistributionPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(crl, ISSUING_DISTRIBUTION_POINT));
            if (idp != null && idp.getOnlySomeReasons() != null && dp.getReasons() != null) {
                return new ReasonsMask(dp.getReasons()).intersect(new ReasonsMask(idp.getOnlySomeReasons()));
            }
            if ((idp == null || idp.getOnlySomeReasons() == null) && dp.getReasons() == null) {
                return ReasonsMask.allReasons;
            }
            ReasonsMask reasonsMask;
            ReasonsMask reasonsMask2;
            if (dp.getReasons() == null) {
                reasonsMask = ReasonsMask.allReasons;
            } else {
                reasonsMask = new ReasonsMask(dp.getReasons());
            }
            if (idp == null) {
                reasonsMask2 = ReasonsMask.allReasons;
            } else {
                reasonsMask2 = new ReasonsMask(idp.getOnlySomeReasons());
            }
            return reasonsMask.intersect(reasonsMask2);
        } catch (Exception e) {
            throw new AnnotatedException("Issuing distribution point extension could not be decoded.", e);
        }
    }

    protected static Set processCRLF(X509CRL crl, Object cert, X509Certificate defaultCRLSignCert, PublicKey defaultCRLSignKey, PKIXExtendedParameters paramsPKIX, List certPathCerts, JcaJceHelper helper) throws AnnotatedException {
        CertPathBuilderException e;
        CertPathValidatorException e2;
        Exception e3;
        Object obj = defaultCRLSignCert;
        X509CertSelector certSelector = new X509CertSelector();
        PKIXExtendedParameters pKIXExtendedParameters;
        List list;
        PublicKey publicKey;
        try {
            certSelector.setSubject(PrincipalUtils.getIssuerPrincipal(crl).getEncoded());
            PKIXCertStoreSelector selector = new Builder(certSelector).build();
            try {
                List validKeys;
                JcaJceHelper jcaJceHelper;
                Collection coll = CertPathValidatorUtilities.findCertificates(selector, paramsPKIX.getCertificateStores());
                coll.addAll(CertPathValidatorUtilities.findCertificates(selector, paramsPKIX.getCertStores()));
                coll.add(obj);
                Iterator cert_it = coll.iterator();
                ArrayList validCerts = new ArrayList();
                List validKeys2 = new ArrayList();
                while (true) {
                    validKeys = validKeys2;
                    if (!cert_it.hasNext()) {
                        break;
                    }
                    X509Certificate signingCert = (X509Certificate) cert_it.next();
                    if (signingCert.equals(obj)) {
                        validCerts.add(signingCert);
                        validKeys.add(defaultCRLSignKey);
                        pKIXExtendedParameters = paramsPKIX;
                        list = certPathCerts;
                        jcaJceHelper = helper;
                    } else {
                        publicKey = defaultCRLSignKey;
                        try {
                            PKIXExtendedParameters.Builder paramsBuilder;
                            PKIXCertPathBuilderSpi builder = new PKIXCertPathBuilderSpi();
                            X509CertSelector tmpCertSelector = new X509CertSelector();
                            tmpCertSelector.setCertificate(signingCert);
                            try {
                                paramsBuilder = new PKIXExtendedParameters.Builder(paramsPKIX).setTargetConstraints(new Builder(tmpCertSelector).build());
                            } catch (CertPathBuilderException e4) {
                                e = e4;
                                list = certPathCerts;
                                jcaJceHelper = helper;
                                throw new AnnotatedException("CertPath for CRL signer failed to validate.", e);
                            } catch (CertPathValidatorException e5) {
                                e2 = e5;
                                list = certPathCerts;
                                jcaJceHelper = helper;
                                throw new AnnotatedException("Public key of issuer certificate of CRL could not be retrieved.", e2);
                            } catch (Exception e6) {
                                e3 = e6;
                                list = certPathCerts;
                                jcaJceHelper = helper;
                                throw new AnnotatedException(e3.getMessage());
                            }
                            try {
                                if (certPathCerts.contains(signingCert)) {
                                    paramsBuilder.setRevocationEnabled(false);
                                } else {
                                    paramsBuilder.setRevocationEnabled(true);
                                }
                                List certs = builder.engineBuild(new PKIXExtendedBuilderParameters.Builder(paramsBuilder.build()).build()).getCertPath().getCertificates();
                                validCerts.add(signingCert);
                                try {
                                    validKeys.add(CertPathValidatorUtilities.getNextWorkingKey(certs, null, helper));
                                } catch (CertPathBuilderException e7) {
                                    e = e7;
                                } catch (CertPathValidatorException e8) {
                                    e2 = e8;
                                    throw new AnnotatedException("Public key of issuer certificate of CRL could not be retrieved.", e2);
                                } catch (Exception e9) {
                                    e3 = e9;
                                    throw new AnnotatedException(e3.getMessage());
                                }
                            } catch (CertPathBuilderException e10) {
                                e = e10;
                                jcaJceHelper = helper;
                                throw new AnnotatedException("CertPath for CRL signer failed to validate.", e);
                            } catch (CertPathValidatorException e11) {
                                e2 = e11;
                                jcaJceHelper = helper;
                                throw new AnnotatedException("Public key of issuer certificate of CRL could not be retrieved.", e2);
                            } catch (Exception e12) {
                                e3 = e12;
                                jcaJceHelper = helper;
                                throw new AnnotatedException(e3.getMessage());
                            }
                        } catch (CertPathBuilderException e13) {
                            e = e13;
                            pKIXExtendedParameters = paramsPKIX;
                            list = certPathCerts;
                            jcaJceHelper = helper;
                            throw new AnnotatedException("CertPath for CRL signer failed to validate.", e);
                        } catch (CertPathValidatorException e14) {
                            e2 = e14;
                            pKIXExtendedParameters = paramsPKIX;
                            list = certPathCerts;
                            jcaJceHelper = helper;
                            throw new AnnotatedException("Public key of issuer certificate of CRL could not be retrieved.", e2);
                        } catch (Exception e15) {
                            e3 = e15;
                            pKIXExtendedParameters = paramsPKIX;
                            list = certPathCerts;
                            jcaJceHelper = helper;
                            throw new AnnotatedException(e3.getMessage());
                        }
                    }
                    validKeys2 = validKeys;
                    X509Certificate obj2 = defaultCRLSignCert;
                }
                publicKey = defaultCRLSignKey;
                pKIXExtendedParameters = paramsPKIX;
                list = certPathCerts;
                jcaJceHelper = helper;
                int i = 0;
                Set checkKeys = new HashSet();
                AnnotatedException lastException = null;
                while (i < validCerts.size()) {
                    boolean[] keyusage = ((X509Certificate) validCerts.get(i)).getKeyUsage();
                    if (keyusage == null || (keyusage.length >= 7 && keyusage[6])) {
                        checkKeys.add(validKeys.get(i));
                    } else {
                        lastException = new AnnotatedException("Issuer certificate key usage extension does not permit CRL signing.");
                    }
                    i++;
                    jcaJceHelper = helper;
                }
                if (checkKeys.isEmpty() && lastException == null) {
                    throw new AnnotatedException("Cannot find a valid issuer certificate.");
                } else if (!checkKeys.isEmpty() || lastException == null) {
                    return checkKeys;
                } else {
                    throw lastException;
                }
            } catch (AnnotatedException e16) {
                publicKey = defaultCRLSignKey;
                pKIXExtendedParameters = paramsPKIX;
                list = certPathCerts;
                throw new AnnotatedException("Issuer certificate for CRL cannot be searched.", e16);
            }
        } catch (IOException e17) {
            publicKey = defaultCRLSignKey;
            pKIXExtendedParameters = paramsPKIX;
            list = certPathCerts;
            throw new AnnotatedException("Subject criteria for certificate selector to find issuer certificate for CRL could not be set.", e17);
        }
    }

    protected static PublicKey processCRLG(X509CRL crl, Set keys) throws AnnotatedException {
        Exception lastException = null;
        for (PublicKey key : keys) {
            try {
                crl.verify(key);
                return key;
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new AnnotatedException("Cannot verify CRL.", lastException);
    }

    protected static X509CRL processCRLH(Set deltacrls, PublicKey key) throws AnnotatedException {
        Exception lastException = null;
        for (X509CRL crl : deltacrls) {
            try {
                crl.verify(key);
                return crl;
            } catch (Exception e) {
                lastException = e;
            }
        }
        if (lastException == null) {
            return null;
        }
        throw new AnnotatedException("Cannot verify delta CRL.", lastException);
    }

    protected static Set processCRLA1i(Date currentDate, PKIXExtendedParameters paramsPKIX, X509Certificate cert, X509CRL crl) throws AnnotatedException {
        Set set = new HashSet();
        if (paramsPKIX.isUseDeltasEnabled()) {
            try {
                CRLDistPoint freshestCRL = CRLDistPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(cert, FRESHEST_CRL));
                if (freshestCRL == null) {
                    try {
                        freshestCRL = CRLDistPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(crl, FRESHEST_CRL));
                    } catch (AnnotatedException e) {
                        throw new AnnotatedException("Freshest CRL extension could not be decoded from CRL.", e);
                    }
                }
                if (freshestCRL != null) {
                    List crlStores = new ArrayList();
                    crlStores.addAll(paramsPKIX.getCRLStores());
                    try {
                        crlStores.addAll(CertPathValidatorUtilities.getAdditionalStoresFromCRLDistributionPoint(freshestCRL, paramsPKIX.getNamedCRLStoreMap()));
                        try {
                            set.addAll(CertPathValidatorUtilities.getDeltaCRLs(currentDate, crl, paramsPKIX.getCertStores(), crlStores));
                        } catch (AnnotatedException e2) {
                            throw new AnnotatedException("Exception obtaining delta CRLs.", e2);
                        }
                    } catch (AnnotatedException e22) {
                        throw new AnnotatedException("No new delta CRL locations could be added from Freshest CRL extension.", e22);
                    }
                }
            } catch (AnnotatedException e3) {
                throw new AnnotatedException("Freshest CRL extension could not be decoded from certificate.", e3);
            }
        }
        return set;
    }

    protected static Set[] processCRLA1ii(Date currentDate, PKIXExtendedParameters paramsPKIX, X509Certificate cert, X509CRL crl) throws AnnotatedException {
        Set deltaSet = new HashSet();
        X509CRLSelector crlselect = new X509CRLSelector();
        crlselect.setCertificateChecking(cert);
        try {
            crlselect.addIssuerName(PrincipalUtils.getIssuerPrincipal(crl).getEncoded());
            PKIXCRLStoreSelector extSelect = new PKIXCRLStoreSelector.Builder(crlselect).setCompleteCRLEnabled(true).build();
            Date validityDate = currentDate;
            if (paramsPKIX.getDate() != null) {
                validityDate = paramsPKIX.getDate();
            }
            Set completeSet = CRL_UTIL.findCRLs(extSelect, validityDate, paramsPKIX.getCertStores(), paramsPKIX.getCRLStores());
            if (paramsPKIX.isUseDeltasEnabled()) {
                try {
                    deltaSet.addAll(CertPathValidatorUtilities.getDeltaCRLs(validityDate, crl, paramsPKIX.getCertStores(), paramsPKIX.getCRLStores()));
                } catch (AnnotatedException e) {
                    throw new AnnotatedException("Exception obtaining delta CRLs.", e);
                }
            }
            return new Set[]{completeSet, deltaSet};
        } catch (IOException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot extract issuer from CRL.");
            stringBuilder.append(e2);
            throw new AnnotatedException(stringBuilder.toString(), e2);
        }
    }

    protected static void processCRLC(X509CRL deltaCRL, X509CRL completeCRL, PKIXExtendedParameters pkixParams) throws AnnotatedException {
        if (deltaCRL != null) {
            IssuingDistributionPoint completeidp = null;
            try {
                completeidp = IssuingDistributionPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(completeCRL, ISSUING_DISTRIBUTION_POINT));
                if (pkixParams.isUseDeltasEnabled()) {
                    if (PrincipalUtils.getIssuerPrincipal(deltaCRL).equals(PrincipalUtils.getIssuerPrincipal(completeCRL))) {
                        IssuingDistributionPoint deltaidp = null;
                        try {
                            deltaidp = IssuingDistributionPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(deltaCRL, ISSUING_DISTRIBUTION_POINT));
                            boolean match = false;
                            if (completeidp == null) {
                                if (deltaidp == null) {
                                    match = true;
                                }
                            } else if (completeidp.equals(deltaidp)) {
                                match = true;
                            }
                            if (match) {
                                ASN1Primitive completeKeyIdentifier = null;
                                try {
                                    completeKeyIdentifier = CertPathValidatorUtilities.getExtensionValue(completeCRL, AUTHORITY_KEY_IDENTIFIER);
                                    try {
                                        ASN1Primitive deltaKeyIdentifier = CertPathValidatorUtilities.getExtensionValue(deltaCRL, AUTHORITY_KEY_IDENTIFIER);
                                        if (completeKeyIdentifier == null) {
                                            throw new AnnotatedException("CRL authority key identifier is null.");
                                        } else if (deltaKeyIdentifier == null) {
                                            throw new AnnotatedException("Delta CRL authority key identifier is null.");
                                        } else if (!completeKeyIdentifier.equals(deltaKeyIdentifier)) {
                                            throw new AnnotatedException("Delta CRL authority key identifier does not match complete CRL authority key identifier.");
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
                }
            } catch (Exception e32) {
                throw new AnnotatedException("Issuing distribution point extension could not be decoded.", e32);
            }
        }
    }

    protected static void processCRLI(Date validDate, X509CRL deltacrl, Object cert, CertStatus certStatus, PKIXExtendedParameters pkixParams) throws AnnotatedException {
        if (pkixParams.isUseDeltasEnabled() && deltacrl != null) {
            CertPathValidatorUtilities.getCertStatus(validDate, deltacrl, cert, certStatus);
        }
    }

    protected static void processCRLJ(Date validDate, X509CRL completecrl, Object cert, CertStatus certStatus) throws AnnotatedException {
        if (certStatus.getCertStatus() == 11) {
            CertPathValidatorUtilities.getCertStatus(validDate, completecrl, cert, certStatus);
        }
    }

    protected static PKIXPolicyNode prepareCertB(CertPath certPath, int index, List[] policyNodes, PKIXPolicyNode validPolicyTree, int policyMapping) throws CertPathValidatorException {
        Iterator it;
        CertPath certPath2 = certPath;
        int i = index;
        List[] listArr = policyNodes;
        List certs = certPath.getCertificates();
        X509Certificate cert = (X509Certificate) certs.get(i);
        int n = certs.size();
        int i2 = n - i;
        ASN1Sequence pm = null;
        List certs2;
        int n2;
        X509Certificate cert2;
        try {
            ASN1Sequence pm2 = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue(cert, POLICY_MAPPINGS));
            PKIXPolicyNode pm3 = validPolicyTree;
            if (pm2 != null) {
                String id_p;
                Set tmp;
                ASN1Sequence mappings = pm2;
                Map m_idp = new HashMap();
                Set s_idp = new HashSet();
                int i3 = 0;
                int j = 0;
                while (j < mappings.size()) {
                    ASN1Sequence mapping = (ASN1Sequence) mappings.getObjectAt(j);
                    id_p = ((ASN1ObjectIdentifier) mapping.getObjectAt(i3)).getId();
                    String sd_p = ((ASN1ObjectIdentifier) mapping.getObjectAt(1)).getId();
                    if (m_idp.containsKey(id_p)) {
                        ((Set) m_idp.get(id_p)).add(sd_p);
                    } else {
                        tmp = new HashSet();
                        tmp.add(sd_p);
                        m_idp.put(id_p, tmp);
                        s_idp.add(id_p);
                    }
                    j++;
                    i3 = 0;
                }
                Iterator it_idp = s_idp.iterator();
                PKIXPolicyNode _validPolicyTree = pm3;
                while (true) {
                    Iterator it_idp2 = it_idp;
                    if (it_idp2.hasNext()) {
                        Iterator it_idp3;
                        Set s_idp2;
                        Map m_idp2;
                        ASN1Sequence mappings2;
                        id_p = (String) it_idp2.next();
                        String str;
                        if (policyMapping > 0) {
                            PKIXPolicyNode node;
                            boolean idp_found = false;
                            for (PKIXPolicyNode node2 : listArr[i2]) {
                                if (node2.getValidPolicy().equals(id_p)) {
                                    idp_found = true;
                                    node2.expectedPolicies = (Set) m_idp.get(id_p);
                                    break;
                                }
                            }
                            if (!idp_found) {
                                Iterator nodes_i = listArr[i2].iterator();
                                while (nodes_i.hasNext()) {
                                    node2 = (PKIXPolicyNode) nodes_i.next();
                                    if (ANY_POLICY.equals(node2.getValidPolicy())) {
                                        ASN1Sequence policies = null;
                                        try {
                                            certs2 = certs;
                                            certs = (ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(cert, CERTIFICATE_POLICIES);
                                            Enumeration e = certs.getObjects();
                                            while (true) {
                                                List policies2 = certs;
                                                Enumeration certs3 = e;
                                                Enumeration e2;
                                                if (!certs3.hasMoreElements()) {
                                                    e2 = certs3;
                                                    n2 = n;
                                                    tmp = null;
                                                    break;
                                                }
                                                PolicyInformation pinfo = null;
                                                try {
                                                    e2 = certs3;
                                                    n2 = n;
                                                    PolicyInformation certs4 = PolicyInformation.getInstance(certs3.nextElement());
                                                    if (ANY_POLICY.equals(certs4.getPolicyIdentifier().getId())) {
                                                        try {
                                                            tmp = CertPathValidatorUtilities.getQualifierSet(certs4.getPolicyQualifiers());
                                                            break;
                                                        } catch (CertPathValidatorException ex) {
                                                            PolicyInformation pinfo2 = certs4;
                                                            throw new ExtCertPathValidatorException("Policy qualifier info set could not be decoded.", ex, certPath2, i);
                                                        }
                                                    }
                                                    certs = policies2;
                                                    e = e2;
                                                    n = n2;
                                                } catch (Exception ex2) {
                                                    e2 = certs3;
                                                    n2 = n;
                                                    throw new CertPathValidatorException("Policy information could not be decoded.", ex2, certPath2, i);
                                                }
                                            }
                                            certs = null;
                                            if (cert.getCriticalExtensionOIDs() != null) {
                                                certs = cert.getCriticalExtensionOIDs().contains(CERTIFICATE_POLICIES);
                                            }
                                            PKIXPolicyNode n3 = (PKIXPolicyNode) node2.getParent();
                                            cert2 = cert;
                                            if (ANY_POLICY.equals(n3.getValidPolicy())) {
                                                it_idp3 = it_idp2;
                                                s_idp2 = s_idp;
                                                m_idp2 = m_idp;
                                                mappings2 = mappings;
                                                X509Certificate pm4 = new PKIXPolicyNode(new ArrayList(), i2, (Set) m_idp.get(id_p), n3, tmp, id_p, certs);
                                                n3.addChild(pm4);
                                                listArr[i2].add(pm4);
                                            } else {
                                                it_idp3 = it_idp2;
                                                s_idp2 = s_idp;
                                                m_idp2 = m_idp;
                                                mappings2 = mappings;
                                            }
                                        } catch (AnnotatedException e3) {
                                            certs2 = certs;
                                            cert2 = cert;
                                            n2 = n;
                                            PKIXPolicyNode pKIXPolicyNode = node2;
                                            it = nodes_i;
                                            str = id_p;
                                            it_idp3 = it_idp2;
                                            s_idp2 = s_idp;
                                            m_idp2 = m_idp;
                                            mappings2 = mappings;
                                            throw new ExtCertPathValidatorException("Certificate policies extension could not be decoded.", e3, certPath2, i);
                                        }
                                    }
                                    cert2 = cert;
                                    n2 = n;
                                    it = nodes_i;
                                    str = id_p;
                                    it_idp3 = it_idp2;
                                    s_idp2 = s_idp;
                                    m_idp2 = m_idp;
                                    mappings2 = mappings;
                                }
                            }
                            certs2 = certs;
                            cert2 = cert;
                            n2 = n;
                            str = id_p;
                            it_idp3 = it_idp2;
                            s_idp2 = s_idp;
                            m_idp2 = m_idp;
                            mappings2 = mappings;
                        } else {
                            certs2 = certs;
                            cert2 = cert;
                            n2 = n;
                            str = id_p;
                            it_idp3 = it_idp2;
                            s_idp2 = s_idp;
                            m_idp2 = m_idp;
                            mappings2 = mappings;
                            if (policyMapping <= 0) {
                                it_idp = listArr[i2].iterator();
                                while (it_idp.hasNext() != null) {
                                    PKIXPolicyNode certs5 = (PKIXPolicyNode) it_idp.next();
                                    String id_p2 = str;
                                    if (certs5.getValidPolicy().equals(id_p2)) {
                                        ((PKIXPolicyNode) certs5.getParent()).removeChild(certs5);
                                        it_idp.remove();
                                        for (pm = i2 - 1; pm >= null; pm--) {
                                            List nodes = listArr[pm];
                                            PKIXPolicyNode _validPolicyTree2 = _validPolicyTree;
                                            for (int l = 0; l < nodes.size(); l++) {
                                                PKIXPolicyNode node22 = (PKIXPolicyNode) nodes.get(l);
                                                if (!node22.hasChildren()) {
                                                    _validPolicyTree2 = CertPathValidatorUtilities.removePolicyNode(_validPolicyTree2, listArr, node22);
                                                    if (_validPolicyTree2 == null) {
                                                        break;
                                                    }
                                                }
                                            }
                                            _validPolicyTree = _validPolicyTree2;
                                        }
                                    }
                                    str = id_p2;
                                }
                            }
                        }
                        it_idp = it_idp3;
                        certs = certs2;
                        s_idp = s_idp2;
                        n = n2;
                        m_idp = m_idp2;
                        cert = cert2;
                        mappings = mappings2;
                    } else {
                        cert2 = cert;
                        n2 = n;
                        return _validPolicyTree;
                    }
                }
            }
            certs2 = certs;
            cert2 = cert;
            n2 = n;
            return pm3;
        } catch (AnnotatedException e32) {
            certs2 = certs;
            cert2 = cert;
            n2 = n;
            throw new ExtCertPathValidatorException("Policy mappings extension could not be decoded.", e32, certPath2, i);
        }
    }

    protected static void prepareNextCertA(CertPath certPath, int index) throws CertPathValidatorException {
        ASN1Sequence pm = null;
        try {
            pm = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), POLICY_MAPPINGS));
            if (pm != null) {
                ASN1Sequence mappings = pm;
                int j = 0;
                while (j < mappings.size()) {
                    ASN1ObjectIdentifier subjectDomainPolicy = null;
                    try {
                        ASN1Sequence mapping = ASN1Sequence.getInstance(mappings.getObjectAt(j));
                        ASN1ObjectIdentifier issuerDomainPolicy = ASN1ObjectIdentifier.getInstance(mapping.getObjectAt(0));
                        subjectDomainPolicy = ASN1ObjectIdentifier.getInstance(mapping.getObjectAt(1));
                        if (ANY_POLICY.equals(issuerDomainPolicy.getId())) {
                            throw new CertPathValidatorException("IssuerDomainPolicy is anyPolicy", null, certPath, index);
                        } else if (ANY_POLICY.equals(subjectDomainPolicy.getId())) {
                            throw new CertPathValidatorException("SubjectDomainPolicy is anyPolicy,", null, certPath, index);
                        } else {
                            j++;
                        }
                    } catch (Exception e) {
                        throw new ExtCertPathValidatorException("Policy mappings extension contents could not be decoded.", e, certPath, index);
                    }
                }
            }
        } catch (AnnotatedException ex) {
            throw new ExtCertPathValidatorException("Policy mappings extension could not be decoded.", ex, certPath, index);
        }
    }

    protected static void processCertF(CertPath certPath, int index, PKIXPolicyNode validPolicyTree, int explicitPolicy) throws CertPathValidatorException {
        if (explicitPolicy <= 0 && validPolicyTree == null) {
            throw new ExtCertPathValidatorException("No valid policy tree found when one expected.", null, certPath, index);
        }
    }

    protected static PKIXPolicyNode processCertE(CertPath certPath, int index, PKIXPolicyNode validPolicyTree) throws CertPathValidatorException {
        try {
            if (ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), CERTIFICATE_POLICIES)) == null) {
                return null;
            }
            return validPolicyTree;
        } catch (AnnotatedException e) {
            throw new ExtCertPathValidatorException("Could not read certificate policies extension from certificate.", e, certPath, index);
        }
    }

    protected static void processCertBC(CertPath certPath, int index, PKIXNameConstraintValidator nameConstraintValidator) throws CertPathValidatorException {
        List list;
        CertPath certPath2 = certPath;
        int i = index;
        PKIXNameConstraintValidator pKIXNameConstraintValidator = nameConstraintValidator;
        List certs = certPath.getCertificates();
        X509Certificate cert = (X509Certificate) certs.get(i);
        int n = certs.size();
        int i2 = n - i;
        if (!CertPathValidatorUtilities.isSelfIssued(cert) || i2 >= n) {
            try {
                ASN1Sequence dns = ASN1Sequence.getInstance(PrincipalUtils.getSubjectPrincipal(cert).getEncoded());
                try {
                    pKIXNameConstraintValidator.checkPermittedDN(dns);
                    pKIXNameConstraintValidator.checkExcludedDN(dns);
                    GeneralNames altName = null;
                    try {
                        altName = GeneralNames.getInstance(CertPathValidatorUtilities.getExtensionValue(cert, SUBJECT_ALTERNATIVE_NAME));
                        RDN[] emails = X500Name.getInstance(dns).getRDNs(BCStyle.EmailAddress);
                        int eI = 0;
                        while (eI != emails.length) {
                            GeneralName emailAsGeneralName = new GeneralName(1, ((ASN1String) emails[eI].getFirst().getValue()).getString());
                            try {
                                pKIXNameConstraintValidator.checkPermitted(emailAsGeneralName);
                                pKIXNameConstraintValidator.checkExcluded(emailAsGeneralName);
                                eI++;
                            } catch (PKIXNameConstraintValidatorException ex) {
                                throw new CertPathValidatorException("Subtree check for certificate subject alternative email failed.", ex, certPath2, i);
                            }
                        }
                        if (altName != null) {
                            GeneralName[] genNames = null;
                            try {
                                certs = altName.getNames();
                                int j = 0;
                                while (true) {
                                    int j2 = j;
                                    if (j2 < certs.length) {
                                        try {
                                            pKIXNameConstraintValidator.checkPermitted(certs[j2]);
                                            pKIXNameConstraintValidator.checkExcluded(certs[j2]);
                                            j = j2 + 1;
                                        } catch (PKIXNameConstraintValidatorException ex2) {
                                            throw new CertPathValidatorException("Subtree check for certificate subject alternative name failed.", ex2, certPath2, i);
                                        }
                                    }
                                    return;
                                }
                            } catch (Exception e) {
                                Exception exception = e;
                                throw new CertPathValidatorException("Subject alternative name contents could not be decoded.", e, certPath2, i);
                            }
                        }
                        return;
                    } catch (Exception e2) {
                        list = certs;
                        throw new CertPathValidatorException("Subject alternative name extension could not be decoded.", e2, certPath2, i);
                    }
                } catch (PKIXNameConstraintValidatorException ex22) {
                    list = certs;
                    throw new CertPathValidatorException("Subtree check for certificate subject failed.", ex22, certPath2, i);
                }
            } catch (Exception e22) {
                list = certs;
                throw new CertPathValidatorException("Exception extracting subject name when checking subtrees.", e22, certPath2, i);
            }
        }
        list = certs;
    }

    protected static PKIXPolicyNode processCertD(CertPath certPath, int index, Set acceptablePolicies, PKIXPolicyNode validPolicyTree, List[] policyNodes, int inhibitAnyPolicy) throws CertPathValidatorException {
        List list;
        CertPath certPath2 = certPath;
        int i = index;
        Set set = acceptablePolicies;
        List[] listArr = policyNodes;
        List certs = certPath.getCertificates();
        X509Certificate cert = (X509Certificate) certs.get(i);
        int n = certs.size();
        int i2 = n - i;
        ASN1Sequence certPolicies = null;
        int i3;
        int i4;
        try {
            ASN1Sequence certPolicies2 = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue(cert, CERTIFICATE_POLICIES));
            ASN1Sequence aSN1Sequence;
            if (certPolicies2 == null || validPolicyTree == null) {
                i3 = n;
                aSN1Sequence = certPolicies2;
                i4 = i2;
                return null;
            }
            Set pols;
            Iterator it;
            int k;
            Enumeration e = certPolicies2.getObjects();
            Set pols2 = new HashSet();
            while (true) {
                pols = pols2;
                if (!e.hasMoreElements()) {
                    break;
                }
                PolicyInformation pInfo = PolicyInformation.getInstance(e.nextElement());
                ASN1ObjectIdentifier pOid = pInfo.getPolicyIdentifier();
                pols.add(pOid.getId());
                if (!ANY_POLICY.equals(pOid.getId())) {
                    Set pq = null;
                    try {
                        pq = CertPathValidatorUtilities.getQualifierSet(pInfo.getPolicyQualifiers());
                        if (!CertPathValidatorUtilities.processCertD1i(i2, listArr, pOid, pq)) {
                            CertPathValidatorUtilities.processCertD1ii(i2, listArr, pOid, pq);
                        }
                    } catch (CertPathValidatorException ex) {
                        list = certs;
                        throw new ExtCertPathValidatorException("Policy qualifier info set could not be build.", ex, certPath2, i);
                    }
                }
                pols2 = pols;
                certs = certs;
            }
            if (acceptablePolicies.isEmpty() || set.contains(ANY_POLICY)) {
                acceptablePolicies.clear();
                set.addAll(pols);
            } else {
                certs = new HashSet();
                for (Object o : acceptablePolicies) {
                    if (pols.contains(o)) {
                        certs.add(o);
                    }
                }
                acceptablePolicies.clear();
                set.addAll(certs);
            }
            Set pols3;
            if (inhibitAnyPolicy > 0 || (i2 < n && CertPathValidatorUtilities.isSelfIssued(cert))) {
                Enumeration e2;
                Enumeration e3 = certPolicies2.getObjects();
                while (e3.hasMoreElements()) {
                    PolicyInformation pInfo2 = PolicyInformation.getInstance(e3.nextElement());
                    if (ANY_POLICY.equals(pInfo2.getPolicyIdentifier().getId())) {
                        PolicyInformation pInfo3;
                        Set _apq = CertPathValidatorUtilities.getQualifierSet(pInfo2.getPolicyQualifiers());
                        List _nodes = listArr[i2 - 1];
                        int k2 = 0;
                        while (true) {
                            k = k2;
                            if (k >= _nodes.size()) {
                                break;
                            }
                            PKIXPolicyNode _node = (PKIXPolicyNode) _nodes.get(k);
                            it = _node.getExpectedPolicies().iterator();
                            while (it.hasNext()) {
                                String _policy;
                                int k3;
                                Object _tmp = it.next();
                                Iterator _policySetIter = it;
                                if ((_tmp instanceof String) != null) {
                                    _policy = (String) _tmp;
                                } else if (_tmp instanceof ASN1ObjectIdentifier) {
                                    _policy = ((ASN1ObjectIdentifier) _tmp).getId();
                                } else {
                                    i3 = n;
                                    k3 = k;
                                    n = _nodes;
                                    pInfo3 = pInfo2;
                                    e2 = e3;
                                    pols3 = pols;
                                    aSN1Sequence = certPolicies2;
                                    i4 = i2;
                                    _nodes = n;
                                    it = _policySetIter;
                                    k = k3;
                                    n = i3;
                                    pInfo2 = pInfo3;
                                    e3 = e2;
                                    pols = pols3;
                                    certPolicies2 = aSN1Sequence;
                                    i2 = i4;
                                    set = acceptablePolicies;
                                }
                                boolean _found = false;
                                Iterator _childrenIter = _node.getChildren();
                                while (true) {
                                    Object _tmp2 = _tmp;
                                    Iterator _childrenIter2 = _childrenIter;
                                    if (!_childrenIter2.hasNext()) {
                                        break;
                                    }
                                    Iterator _childrenIter3 = _childrenIter2;
                                    if (_policy.equals(((PKIXPolicyNode) _childrenIter2.next()).getValidPolicy()) != null) {
                                        _found = true;
                                    }
                                    _tmp = _tmp2;
                                    _childrenIter = _childrenIter3;
                                }
                                if (_found) {
                                    i3 = n;
                                    k3 = k;
                                    n = _nodes;
                                    pInfo3 = pInfo2;
                                    e2 = e3;
                                    pols3 = pols;
                                    aSN1Sequence = certPolicies2;
                                    i4 = i2;
                                } else {
                                    set = new HashSet();
                                    set.add(_policy);
                                    i3 = n;
                                    PKIXPolicyNode _node2 = _node;
                                    k3 = k;
                                    List arrayList = new ArrayList();
                                    n = _nodes;
                                    pInfo3 = pInfo2;
                                    e2 = e3;
                                    pols3 = pols;
                                    aSN1Sequence = certPolicies2;
                                    i4 = i2;
                                    List pKIXPolicyNode = new PKIXPolicyNode(arrayList, i2, set, _node2, _apq, _policy, 0);
                                    _node = _node2;
                                    _node.addChild(pKIXPolicyNode);
                                    listArr[i4].add(pKIXPolicyNode);
                                }
                                _nodes = n;
                                it = _policySetIter;
                                k = k3;
                                n = i3;
                                pInfo2 = pInfo3;
                                e3 = e2;
                                pols = pols3;
                                certPolicies2 = aSN1Sequence;
                                i2 = i4;
                                set = acceptablePolicies;
                            }
                            i3 = n;
                            n = _nodes;
                            pInfo3 = pInfo2;
                            e2 = e3;
                            pols3 = pols;
                            aSN1Sequence = certPolicies2;
                            i4 = i2;
                            k2 = k + 1;
                            n = i3;
                            set = acceptablePolicies;
                        }
                        n = _nodes;
                        pInfo3 = pInfo2;
                        e2 = e3;
                        pols3 = pols;
                        aSN1Sequence = certPolicies2;
                        i4 = i2;
                        e = e2;
                    } else {
                        e2 = e3;
                        pols3 = pols;
                        aSN1Sequence = certPolicies2;
                        i4 = i2;
                        set = acceptablePolicies;
                    }
                }
                e2 = e3;
                pols3 = pols;
                aSN1Sequence = certPolicies2;
                i4 = i2;
                e = e2;
            } else {
                i3 = n;
                pols3 = pols;
                aSN1Sequence = certPolicies2;
                i4 = i2;
            }
            PKIXPolicyNode _validPolicyTree = validPolicyTree;
            i2 = i4 - 1;
            while (true) {
                int j = i2;
                if (j < 0) {
                    break;
                }
                certs = listArr[j];
                PKIXPolicyNode _validPolicyTree2 = _validPolicyTree;
                for (int k4 = 0; k4 < certs.size(); k4++) {
                    PKIXPolicyNode node = (PKIXPolicyNode) certs.get(k4);
                    if (!node.hasChildren()) {
                        _validPolicyTree2 = CertPathValidatorUtilities.removePolicyNode(_validPolicyTree2, listArr, node);
                        if (_validPolicyTree2 == null) {
                            break;
                        }
                    }
                }
                _validPolicyTree = _validPolicyTree2;
                i2 = j - 1;
            }
            set = cert.getCriticalExtensionOIDs();
            if (set != null) {
                certs = set.contains(CERTIFICATE_POLICIES);
                n = listArr[i4];
                int j2 = 0;
                while (true) {
                    k = j2;
                    if (k >= n.size()) {
                        break;
                    }
                    ((PKIXPolicyNode) n.get(k)).setCritical(certs);
                    j2 = k + 1;
                }
            }
            return _validPolicyTree;
        } catch (AnnotatedException e4) {
            list = certs;
            i3 = n;
            i4 = i2;
            throw new ExtCertPathValidatorException("Could not read certificate policies extension from certificate.", e4, certPath2, i);
        }
    }

    protected static void processCertA(CertPath certPath, PKIXExtendedParameters paramsPKIX, int index, PublicKey workingPublicKey, boolean verificationAlreadyPerformed, X500Name workingIssuerName, X509Certificate sign, JcaJceHelper helper) throws ExtCertPathValidatorException {
        PublicKey publicKey;
        GeneralSecurityException e;
        StringBuilder stringBuilder;
        CertPath certPath2 = certPath;
        PKIXExtendedParameters pKIXExtendedParameters = paramsPKIX;
        int i = index;
        X500Name x500Name = workingIssuerName;
        List certs = certPath2.getCertificates();
        X509Certificate cert = (X509Certificate) certs.get(i);
        if (verificationAlreadyPerformed) {
            publicKey = workingPublicKey;
        } else {
            try {
                publicKey = workingPublicKey;
                try {
                    CertPathValidatorUtilities.verifyX509Certificate(cert, publicKey, paramsPKIX.getSigProvider());
                } catch (GeneralSecurityException e2) {
                    e = e2;
                }
            } catch (GeneralSecurityException e3) {
                e = e3;
                publicKey = workingPublicKey;
                throw new ExtCertPathValidatorException("Could not validate certificate signature.", e, certPath2, i);
            }
        }
        try {
            cert.checkValidity(CertPathValidatorUtilities.getValidCertDateFromValidityModel(pKIXExtendedParameters, certPath2, i));
            if (paramsPKIX.isRevocationEnabled()) {
                try {
                    checkCRLs(pKIXExtendedParameters, cert, CertPathValidatorUtilities.getValidCertDateFromValidityModel(pKIXExtendedParameters, certPath2, i), sign, publicKey, certs, helper);
                } catch (AnnotatedException e4) {
                    Throwable cause = e4;
                    if (e4.getCause() != null) {
                        cause = e4.getCause();
                    }
                    throw new ExtCertPathValidatorException(e4.getMessage(), cause, certPath2, i);
                }
            }
            if (!PrincipalUtils.getEncodedIssuerPrincipal(cert).equals(x500Name)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IssuerName(");
                stringBuilder2.append(PrincipalUtils.getEncodedIssuerPrincipal(cert));
                stringBuilder2.append(") does not match SubjectName(");
                stringBuilder2.append(x500Name);
                stringBuilder2.append(") of signing certificate.");
                throw new ExtCertPathValidatorException(stringBuilder2.toString(), null, certPath2, i);
            }
        } catch (CertificateExpiredException e5) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not validate certificate: ");
            stringBuilder.append(e5.getMessage());
            throw new ExtCertPathValidatorException(stringBuilder.toString(), e5, certPath2, i);
        } catch (CertificateNotYetValidException e6) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not validate certificate: ");
            stringBuilder.append(e6.getMessage());
            throw new ExtCertPathValidatorException(stringBuilder.toString(), e6, certPath2, i);
        } catch (AnnotatedException e7) {
            throw new ExtCertPathValidatorException("Could not validate time of certificate.", e7, certPath2, i);
        }
    }

    protected static int prepareNextCertI1(CertPath certPath, int index, int explicitPolicy) throws CertPathValidatorException {
        try {
            ASN1Sequence pc = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), POLICY_CONSTRAINTS));
            if (pc != null) {
                Enumeration policyConstraints = pc.getObjects();
                while (policyConstraints.hasMoreElements()) {
                    try {
                        ASN1TaggedObject constraint = ASN1TaggedObject.getInstance(policyConstraints.nextElement());
                        if (constraint.getTagNo() == 0) {
                            int tmpInt = ASN1Integer.getInstance(constraint, false).getValue().intValue();
                            if (tmpInt < explicitPolicy) {
                                return tmpInt;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        throw new ExtCertPathValidatorException("Policy constraints extension contents cannot be decoded.", e, certPath, index);
                    }
                }
            }
            return explicitPolicy;
        } catch (Exception e2) {
            throw new ExtCertPathValidatorException("Policy constraints extension cannot be decoded.", e2, certPath, index);
        }
    }

    protected static int prepareNextCertI2(CertPath certPath, int index, int policyMapping) throws CertPathValidatorException {
        try {
            ASN1Sequence pc = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), POLICY_CONSTRAINTS));
            if (pc != null) {
                Enumeration policyConstraints = pc.getObjects();
                while (policyConstraints.hasMoreElements()) {
                    try {
                        ASN1TaggedObject constraint = ASN1TaggedObject.getInstance(policyConstraints.nextElement());
                        if (constraint.getTagNo() == 1) {
                            int tmpInt = ASN1Integer.getInstance(constraint, false).getValue().intValue();
                            if (tmpInt < policyMapping) {
                                return tmpInt;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        throw new ExtCertPathValidatorException("Policy constraints extension contents cannot be decoded.", e, certPath, index);
                    }
                }
            }
            return policyMapping;
        } catch (Exception e2) {
            throw new ExtCertPathValidatorException("Policy constraints extension cannot be decoded.", e2, certPath, index);
        }
    }

    protected static void prepareNextCertG(CertPath certPath, int index, PKIXNameConstraintValidator nameConstraintValidator) throws CertPathValidatorException {
        NameConstraints nc = null;
        try {
            ASN1Sequence ncSeq = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), NAME_CONSTRAINTS));
            if (ncSeq != null) {
                nc = NameConstraints.getInstance(ncSeq);
            }
            if (nc != null) {
                GeneralSubtree[] permitted = nc.getPermittedSubtrees();
                if (permitted != null) {
                    try {
                        nameConstraintValidator.intersectPermittedSubtree(permitted);
                    } catch (Exception ex) {
                        throw new ExtCertPathValidatorException("Permitted subtrees cannot be build from name constraints extension.", ex, certPath, index);
                    }
                }
                GeneralSubtree[] excluded = nc.getExcludedSubtrees();
                if (excluded != null) {
                    int i = 0;
                    while (i != excluded.length) {
                        try {
                            nameConstraintValidator.addExcludedSubtree(excluded[i]);
                            i++;
                        } catch (Exception ex2) {
                            throw new ExtCertPathValidatorException("Excluded subtrees cannot be build from name constraints extension.", ex2, certPath, index);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ExtCertPathValidatorException("Name constraints extension could not be decoded.", e, certPath, index);
        }
    }

    private static void checkCRL(DistributionPoint dp, PKIXExtendedParameters paramsPKIX, X509Certificate cert, Date validDate, X509Certificate defaultCRLSignCert, PublicKey defaultCRLSignKey, CertStatus certStatus, ReasonsMask reasonMask, List certPathCerts, JcaJceHelper helper) throws AnnotatedException {
        Set crls;
        Iterator crl_iter;
        AnnotatedException e;
        DistributionPoint distributionPoint = dp;
        PKIXExtendedParameters pKIXExtendedParameters = paramsPKIX;
        X509Certificate x509Certificate = cert;
        Date date = validDate;
        CertStatus certStatus2 = certStatus;
        ReasonsMask reasonsMask = reasonMask;
        Date currentDate = new Date(System.currentTimeMillis());
        ReasonsMask interimReasonsMask;
        if (validDate.getTime() <= currentDate.getTime()) {
            Iterator crl_iter2;
            Set crls2 = CertPathValidatorUtilities.getCompleteCRLs(distributionPoint, x509Certificate, currentDate, pKIXExtendedParameters);
            Iterator crl_iter3 = crls2.iterator();
            boolean validCrlFound = false;
            AnnotatedException lastException = null;
            while (true) {
                crl_iter2 = crl_iter3;
                if (crl_iter2.hasNext() && certStatus.getCertStatus() == 11 && !reasonMask.isAllReasons()) {
                    try {
                        X509CRL crl = (X509CRL) crl_iter2.next();
                        interimReasonsMask = processCRLD(crl, distributionPoint);
                        if (interimReasonsMask.hasNewReasons(reasonsMask)) {
                            crls = crls2;
                            ReasonsMask crls3 = interimReasonsMask;
                            int i = 11;
                            crl_iter = crl_iter2;
                            try {
                                crl_iter3 = processCRLG(crl, processCRLF(crl, x509Certificate, defaultCRLSignCert, defaultCRLSignKey, pKIXExtendedParameters, certPathCerts, helper));
                                X509CRL deltaCRL = null;
                                Date validityDate = currentDate;
                                if (paramsPKIX.getDate() != null) {
                                    validityDate = paramsPKIX.getDate();
                                }
                                if (paramsPKIX.isUseDeltasEnabled()) {
                                    deltaCRL = processCRLH(CertPathValidatorUtilities.getDeltaCRLs(validityDate, crl, paramsPKIX.getCertStores(), paramsPKIX.getCRLStores()), crl_iter3);
                                }
                                if (paramsPKIX.getValidityModel() != 1) {
                                    if (cert.getNotAfter().getTime() < crl.getThisUpdate().getTime()) {
                                        throw new AnnotatedException("No valid CRL for current time found.");
                                    }
                                }
                                processCRLB1(distributionPoint, x509Certificate, crl);
                                processCRLB2(distributionPoint, x509Certificate, crl);
                                processCRLC(deltaCRL, crl, pKIXExtendedParameters);
                                processCRLI(date, deltaCRL, x509Certificate, certStatus2, pKIXExtendedParameters);
                                processCRLJ(date, crl, x509Certificate, certStatus2);
                                if (certStatus.getCertStatus() == 8) {
                                    certStatus2.setCertStatus(i);
                                }
                                interimReasonsMask = reasonMask;
                                try {
                                    HashSet criticalExtensions;
                                    interimReasonsMask.addReasons(crls3);
                                    Set criticalExtensions2 = crl.getCriticalExtensionOIDs();
                                    if (criticalExtensions2 != null) {
                                        criticalExtensions = new HashSet(criticalExtensions2);
                                        criticalExtensions.remove(Extension.issuingDistributionPoint.getId());
                                        criticalExtensions.remove(Extension.deltaCRLIndicator.getId());
                                        if (!criticalExtensions.isEmpty()) {
                                            throw new AnnotatedException("CRL contains unsupported critical extensions.");
                                        }
                                    }
                                    if (deltaCRL != null) {
                                        criticalExtensions2 = deltaCRL.getCriticalExtensionOIDs();
                                        if (criticalExtensions2 != null) {
                                            criticalExtensions = new HashSet(criticalExtensions2);
                                            criticalExtensions.remove(Extension.issuingDistributionPoint.getId());
                                            criticalExtensions.remove(Extension.deltaCRLIndicator.getId());
                                            if (!criticalExtensions.isEmpty()) {
                                                throw new AnnotatedException("Delta CRL contains unsupported critical extension.");
                                            }
                                        }
                                    }
                                    validCrlFound = true;
                                } catch (AnnotatedException e2) {
                                    e = e2;
                                    lastException = e;
                                    reasonsMask = interimReasonsMask;
                                    crls2 = crls;
                                    crl_iter3 = crl_iter;
                                }
                            } catch (AnnotatedException e3) {
                                e = e3;
                                interimReasonsMask = reasonMask;
                                lastException = e;
                                reasonsMask = interimReasonsMask;
                                crls2 = crls;
                                crl_iter3 = crl_iter;
                            }
                            reasonsMask = interimReasonsMask;
                            crls2 = crls;
                            crl_iter3 = crl_iter;
                        } else {
                            crl_iter3 = crl_iter2;
                        }
                    } catch (AnnotatedException e4) {
                        e = e4;
                        crl_iter = crl_iter2;
                        interimReasonsMask = reasonsMask;
                        crls = crls2;
                        lastException = e;
                        reasonsMask = interimReasonsMask;
                        crls2 = crls;
                        crl_iter3 = crl_iter;
                    }
                } else {
                    crl_iter = crl_iter2;
                    interimReasonsMask = reasonsMask;
                    crls = crls2;
                }
            }
            crl_iter = crl_iter2;
            interimReasonsMask = reasonsMask;
            crls = crls2;
            if (!validCrlFound) {
                throw lastException;
            }
            return;
        }
        interimReasonsMask = reasonsMask;
        throw new AnnotatedException("Validation time is in future.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:65:0x0136  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x00cf  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x014b  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x013e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected static void checkCRLs(PKIXExtendedParameters paramsPKIX, X509Certificate cert, Date validDate, X509Certificate sign, PublicKey workingPublicKey, List certPathCerts, JcaJceHelper helper) throws AnnotatedException {
        AnnotatedException e;
        PKIXExtendedParameters.Builder builder;
        ReasonsMask reasonsMask;
        CertStatus certStatus;
        int i;
        DistributionPoint[] dps;
        Exception e2;
        PKIXExtendedParameters pKIXExtendedParameters;
        AnnotatedException lastException = null;
        CRLDistPoint crldp = null;
        X509Certificate x509Certificate;
        try {
            x509Certificate = cert;
            try {
                crldp = CRLDistPoint.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate, CRL_DISTRIBUTION_POINTS));
                PKIXExtendedParameters.Builder paramsBldr = new PKIXExtendedParameters.Builder(paramsPKIX);
                try {
                    boolean validCrlFound;
                    AnnotatedException annotatedException;
                    for (PKIXCRLStore addCRLStore : CertPathValidatorUtilities.getAdditionalStoresFromCRLDistributionPoint(crldp, paramsPKIX.getNamedCRLStoreMap())) {
                        try {
                            paramsBldr.addCRLStore(addCRLStore);
                        } catch (AnnotatedException e3) {
                            e = e3;
                            builder = paramsBldr;
                            throw new AnnotatedException("No additional CRL locations could be decoded from CRL distribution point extension.", e);
                        }
                    }
                    CertStatus certStatus2 = new CertStatus();
                    ReasonsMask reasonsMask2 = new ReasonsMask();
                    PKIXExtendedParameters finalParams = paramsBldr.build();
                    int i2 = 11;
                    if (crldp != null) {
                        DistributionPoint[] dps2 = null;
                        try {
                            DistributionPoint[] dps3 = crldp.getDistributionPoints();
                            if (dps3 != null) {
                                validCrlFound = false;
                                int i3 = 0;
                                while (true) {
                                    int i4 = i3;
                                    if (i4 >= dps3.length || certStatus2.getCertStatus() != i2 || reasonsMask2.isAllReasons()) {
                                        reasonsMask = reasonsMask2;
                                        certStatus = certStatus2;
                                        builder = paramsBldr;
                                    } else {
                                        try {
                                            i = i4;
                                            dps = dps3;
                                            int i5 = i2;
                                            reasonsMask = reasonsMask2;
                                            certStatus = certStatus2;
                                            builder = paramsBldr;
                                            try {
                                                checkCRL(dps3[i4], finalParams, x509Certificate, validDate, sign, workingPublicKey, certStatus2, reasonsMask2, certPathCerts, helper);
                                                validCrlFound = true;
                                            } catch (AnnotatedException e4) {
                                                e = e4;
                                                lastException = e;
                                                i3 = i + 1;
                                                certStatus2 = certStatus;
                                                dps3 = dps;
                                                reasonsMask2 = reasonsMask;
                                                paramsBldr = builder;
                                                i2 = 11;
                                            }
                                        } catch (AnnotatedException e5) {
                                            e = e5;
                                            i = i4;
                                            dps = dps3;
                                            reasonsMask = reasonsMask2;
                                            certStatus = certStatus2;
                                            builder = paramsBldr;
                                            lastException = e;
                                            i3 = i + 1;
                                            certStatus2 = certStatus;
                                            dps3 = dps;
                                            reasonsMask2 = reasonsMask;
                                            paramsBldr = builder;
                                            i2 = 11;
                                        }
                                        i3 = i + 1;
                                        certStatus2 = certStatus;
                                        dps3 = dps;
                                        reasonsMask2 = reasonsMask;
                                        paramsBldr = builder;
                                        i2 = 11;
                                    }
                                }
                                reasonsMask = reasonsMask2;
                                certStatus = certStatus2;
                                builder = paramsBldr;
                                if (certStatus.getCertStatus() != 11) {
                                    ReasonsMask paramsBldr2 = reasonsMask;
                                    if (paramsBldr2.isAllReasons()) {
                                        annotatedException = lastException;
                                        lastException = paramsBldr2;
                                    } else {
                                        ASN1Primitive issuer = null;
                                        try {
                                            try {
                                                annotatedException = lastException;
                                                lastException = paramsBldr2;
                                                checkCRL(new DistributionPoint(new DistributionPointName(0, new GeneralNames(new GeneralName(4, new ASN1InputStream(PrincipalUtils.getEncodedIssuerPrincipal(cert).getEncoded()).readObject()))), null, null), (PKIXExtendedParameters) paramsPKIX.clone(), x509Certificate, validDate, sign, workingPublicKey, certStatus, paramsBldr2, certPathCerts, helper);
                                                validCrlFound = true;
                                            } catch (AnnotatedException e6) {
                                                e = e6;
                                                annotatedException = lastException;
                                                lastException = paramsBldr2;
                                            }
                                        } catch (Exception e22) {
                                            annotatedException = lastException;
                                            ReasonsMask lastException2 = paramsBldr2;
                                            throw new AnnotatedException("Issuer from certificate for CRL could not be reencoded.", e22);
                                        } catch (AnnotatedException e7) {
                                            e = e7;
                                        }
                                    }
                                } else {
                                    annotatedException = lastException;
                                    lastException = reasonsMask;
                                }
                                e = annotatedException;
                                if (validCrlFound) {
                                    if (e instanceof AnnotatedException) {
                                        throw e;
                                    }
                                    throw new AnnotatedException("No valid CRL found.", e);
                                } else if (certStatus.getCertStatus() == 11) {
                                    if (!lastException.isAllReasons() && certStatus.getCertStatus() == 11) {
                                        certStatus.setCertStatus(12);
                                    }
                                    if (certStatus.getCertStatus() == 12) {
                                        throw new AnnotatedException("Certificate status could not be determined.");
                                    }
                                    return;
                                } else {
                                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    String message = new StringBuilder();
                                    message.append("Certificate revocation after ");
                                    message.append(df.format(certStatus.getRevocationDate()));
                                    message = message.toString();
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(message);
                                    stringBuilder.append(", reason: ");
                                    stringBuilder.append(crlReasons[certStatus.getCertStatus()]);
                                    throw new AnnotatedException(stringBuilder.toString());
                                }
                            }
                        } catch (Exception e222) {
                            reasonsMask = reasonsMask2;
                            certStatus = certStatus2;
                            builder = paramsBldr;
                            Exception exception = e222;
                            throw new AnnotatedException("Distribution points could not be read.", e222);
                        }
                    }
                    reasonsMask = reasonsMask2;
                    certStatus = certStatus2;
                    builder = paramsBldr;
                    validCrlFound = false;
                    if (certStatus.getCertStatus() != 11) {
                    }
                    e = annotatedException;
                    if (validCrlFound) {
                    }
                } catch (AnnotatedException e8) {
                    e = e8;
                    builder = paramsBldr;
                    throw new AnnotatedException("No additional CRL locations could be decoded from CRL distribution point extension.", e);
                }
            } catch (Exception e9) {
                e222 = e9;
                pKIXExtendedParameters = paramsPKIX;
                throw new AnnotatedException("CRL distribution point extension could not be read.", e222);
            }
        } catch (Exception e10) {
            e222 = e10;
            pKIXExtendedParameters = paramsPKIX;
            x509Certificate = cert;
            throw new AnnotatedException("CRL distribution point extension could not be read.", e222);
        }
    }

    protected static int prepareNextCertJ(CertPath certPath, int index, int inhibitAnyPolicy) throws CertPathValidatorException {
        try {
            ASN1Integer iap = ASN1Integer.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), INHIBIT_ANY_POLICY));
            if (iap != null) {
                int _inhibitAnyPolicy = iap.getValue().intValue();
                if (_inhibitAnyPolicy < inhibitAnyPolicy) {
                    return _inhibitAnyPolicy;
                }
            }
            return inhibitAnyPolicy;
        } catch (Exception e) {
            throw new ExtCertPathValidatorException("Inhibit any-policy extension cannot be decoded.", e, certPath, index);
        }
    }

    protected static void prepareNextCertK(CertPath certPath, int index) throws CertPathValidatorException {
        try {
            BasicConstraints bc = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), BASIC_CONSTRAINTS));
            if (bc == null) {
                throw new CertPathValidatorException("Intermediate certificate lacks BasicConstraints");
            } else if (!bc.isCA()) {
                throw new CertPathValidatorException("Not a CA certificate");
            }
        } catch (Exception e) {
            throw new ExtCertPathValidatorException("Basic constraints extension cannot be decoded.", e, certPath, index);
        }
    }

    protected static int prepareNextCertL(CertPath certPath, int index, int maxPathLength) throws CertPathValidatorException {
        if (CertPathValidatorUtilities.isSelfIssued((X509Certificate) certPath.getCertificates().get(index))) {
            return maxPathLength;
        }
        if (maxPathLength > 0) {
            return maxPathLength - 1;
        }
        throw new ExtCertPathValidatorException("Max path length not greater than zero", null, certPath, index);
    }

    protected static int prepareNextCertM(CertPath certPath, int index, int maxPathLength) throws CertPathValidatorException {
        try {
            BasicConstraints bc = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), BASIC_CONSTRAINTS));
            if (bc != null) {
                BigInteger _pathLengthConstraint = bc.getPathLenConstraint();
                if (_pathLengthConstraint != null) {
                    int _plc = _pathLengthConstraint.intValue();
                    if (_plc < maxPathLength) {
                        return _plc;
                    }
                }
            }
            return maxPathLength;
        } catch (Exception e) {
            throw new ExtCertPathValidatorException("Basic constraints extension cannot be decoded.", e, certPath, index);
        }
    }

    protected static void prepareNextCertN(CertPath certPath, int index) throws CertPathValidatorException {
        boolean[] _usage = ((X509Certificate) certPath.getCertificates().get(index)).getKeyUsage();
        if (_usage != null && !_usage[5]) {
            throw new ExtCertPathValidatorException("Issuer certificate keyusage extension is critical and does not permit key signing.", null, certPath, index);
        }
    }

    protected static void prepareNextCertO(CertPath certPath, int index, Set criticalExtensions, List pathCheckers) throws CertPathValidatorException {
        X509Certificate cert = (X509Certificate) certPath.getCertificates().get(index);
        for (PKIXCertPathChecker check : pathCheckers) {
            try {
                check.check(cert, criticalExtensions);
            } catch (CertPathValidatorException e) {
                throw new CertPathValidatorException(e.getMessage(), e.getCause(), certPath, index);
            }
        }
        if (!criticalExtensions.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Certificate has unsupported critical extension: ");
            stringBuilder.append(criticalExtensions);
            throw new ExtCertPathValidatorException(stringBuilder.toString(), null, certPath, index);
        }
    }

    protected static int prepareNextCertH1(CertPath certPath, int index, int explicitPolicy) {
        if (CertPathValidatorUtilities.isSelfIssued((X509Certificate) certPath.getCertificates().get(index)) || explicitPolicy == 0) {
            return explicitPolicy;
        }
        return explicitPolicy - 1;
    }

    protected static int prepareNextCertH2(CertPath certPath, int index, int policyMapping) {
        if (CertPathValidatorUtilities.isSelfIssued((X509Certificate) certPath.getCertificates().get(index)) || policyMapping == 0) {
            return policyMapping;
        }
        return policyMapping - 1;
    }

    protected static int prepareNextCertH3(CertPath certPath, int index, int inhibitAnyPolicy) {
        if (CertPathValidatorUtilities.isSelfIssued((X509Certificate) certPath.getCertificates().get(index)) || inhibitAnyPolicy == 0) {
            return inhibitAnyPolicy;
        }
        return inhibitAnyPolicy - 1;
    }

    protected static int wrapupCertA(int explicitPolicy, X509Certificate cert) {
        if (CertPathValidatorUtilities.isSelfIssued(cert) || explicitPolicy == 0) {
            return explicitPolicy;
        }
        return explicitPolicy - 1;
    }

    protected static int wrapupCertB(CertPath certPath, int index, int explicitPolicy) throws CertPathValidatorException {
        try {
            ASN1Sequence pc = ASN1Sequence.getInstance(CertPathValidatorUtilities.getExtensionValue((X509Certificate) certPath.getCertificates().get(index), POLICY_CONSTRAINTS));
            if (pc != null) {
                Enumeration policyConstraints = pc.getObjects();
                while (policyConstraints.hasMoreElements()) {
                    ASN1TaggedObject constraint = (ASN1TaggedObject) policyConstraints.nextElement();
                    if (constraint.getTagNo() == 0) {
                        try {
                            if (ASN1Integer.getInstance(constraint, false).getValue().intValue() == 0) {
                                return 0;
                            }
                        } catch (Exception e) {
                            throw new ExtCertPathValidatorException("Policy constraints requireExplicitPolicy field could not be decoded.", e, certPath, index);
                        }
                    }
                }
            }
            return explicitPolicy;
        } catch (AnnotatedException e2) {
            throw new ExtCertPathValidatorException("Policy constraints could not be decoded.", e2, certPath, index);
        }
    }

    protected static void wrapupCertF(CertPath certPath, int index, List pathCheckers, Set criticalExtensions) throws CertPathValidatorException {
        X509Certificate cert = (X509Certificate) certPath.getCertificates().get(index);
        for (PKIXCertPathChecker check : pathCheckers) {
            try {
                check.check(cert, criticalExtensions);
            } catch (CertPathValidatorException e) {
                throw new ExtCertPathValidatorException("Additional certificate path checker failed.", e, certPath, index);
            }
        }
        if (!criticalExtensions.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Certificate has unsupported critical extension: ");
            stringBuilder.append(criticalExtensions);
            throw new ExtCertPathValidatorException(stringBuilder.toString(), null, certPath, index);
        }
    }

    protected static PKIXPolicyNode wrapupCertG(CertPath certPath, PKIXExtendedParameters paramsPKIX, Set userInitialPolicySet, int index, List[] policyNodes, PKIXPolicyNode validPolicyTree, Set acceptablePolicies) throws CertPathValidatorException {
        CertPath certPath2 = certPath;
        int i = index;
        List[] listArr = policyNodes;
        int n = certPath.getCertificates().size();
        Set set;
        PKIXPolicyNode pKIXPolicyNode;
        Set set2;
        Set<PKIXPolicyNode> _validPolicyNodeSet;
        int k;
        int j;
        List nodes;
        PKIXPolicyNode validPolicyTree2;
        int k2;
        PKIXPolicyNode node;
        if (validPolicyTree == null) {
            if (paramsPKIX.isExplicitPolicyRequired()) {
                throw new ExtCertPathValidatorException("Explicit policy requested but none available.", null, certPath2, i);
            }
            set = userInitialPolicySet;
            pKIXPolicyNode = validPolicyTree;
            set2 = acceptablePolicies;
            return null;
        } else if (CertPathValidatorUtilities.isAnyPolicy(userInitialPolicySet)) {
            if (!paramsPKIX.isExplicitPolicyRequired()) {
                set2 = acceptablePolicies;
            } else if (acceptablePolicies.isEmpty()) {
                set2 = acceptablePolicies;
                throw new ExtCertPathValidatorException("Explicit policy requested but none available.", null, certPath2, i);
            } else {
                _validPolicyNodeSet = new HashSet();
                for (List _nodeDepth : listArr) {
                    for (k = 0; k < _nodeDepth.size(); k++) {
                        PKIXPolicyNode _node = (PKIXPolicyNode) _nodeDepth.get(k);
                        if (ANY_POLICY.equals(_node.getValidPolicy())) {
                            Iterator _iter = _node.getChildren();
                            while (_iter.hasNext()) {
                                _validPolicyNodeSet.add(_iter.next());
                            }
                        }
                    }
                }
                for (PKIXPolicyNode _node2 : _validPolicyNodeSet) {
                    acceptablePolicies.contains(_node2.getValidPolicy());
                }
                set2 = acceptablePolicies;
                if (validPolicyTree != null) {
                    j = n - 1;
                    pKIXPolicyNode = validPolicyTree;
                    while (j >= 0) {
                        nodes = listArr[j];
                        validPolicyTree2 = pKIXPolicyNode;
                        for (k2 = 0; k2 < nodes.size(); k2++) {
                            node = (PKIXPolicyNode) nodes.get(k2);
                            if (!node.hasChildren()) {
                                validPolicyTree2 = CertPathValidatorUtilities.removePolicyNode(validPolicyTree2, listArr, node);
                            }
                        }
                        j--;
                        pKIXPolicyNode = validPolicyTree2;
                    }
                    set = userInitialPolicySet;
                    return pKIXPolicyNode;
                }
            }
            pKIXPolicyNode = validPolicyTree;
            set = userInitialPolicySet;
            return pKIXPolicyNode;
        } else {
            PKIXPolicyNode _c_node;
            set2 = acceptablePolicies;
            _validPolicyNodeSet = new HashSet();
            for (List _nodeDepth2 : listArr) {
                for (k = 0; k < _nodeDepth2.size(); k++) {
                    validPolicyTree2 = (PKIXPolicyNode) _nodeDepth2.get(k);
                    if (ANY_POLICY.equals(validPolicyTree2.getValidPolicy())) {
                        Iterator _iter2 = validPolicyTree2.getChildren();
                        while (_iter2.hasNext()) {
                            _c_node = (PKIXPolicyNode) _iter2.next();
                            if (!ANY_POLICY.equals(_c_node.getValidPolicy())) {
                                _validPolicyNodeSet.add(_c_node);
                            }
                        }
                    }
                }
            }
            pKIXPolicyNode = validPolicyTree;
            for (PKIXPolicyNode _node22 : _validPolicyNodeSet) {
                if (!userInitialPolicySet.contains(_node22.getValidPolicy())) {
                    pKIXPolicyNode = CertPathValidatorUtilities.removePolicyNode(pKIXPolicyNode, listArr, _node22);
                }
            }
            set = userInitialPolicySet;
            if (pKIXPolicyNode != null) {
                j = n - 1;
                while (j >= 0) {
                    nodes = listArr[j];
                    node = pKIXPolicyNode;
                    for (k2 = 0; k2 < nodes.size(); k2++) {
                        _c_node = (PKIXPolicyNode) nodes.get(k2);
                        if (!_c_node.hasChildren()) {
                            node = CertPathValidatorUtilities.removePolicyNode(node, listArr, _c_node);
                        }
                    }
                    j--;
                    pKIXPolicyNode = node;
                }
            }
            return pKIXPolicyNode;
        }
    }
}
