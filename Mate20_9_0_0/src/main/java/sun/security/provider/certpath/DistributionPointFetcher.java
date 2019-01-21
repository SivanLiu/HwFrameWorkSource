package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
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
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.DistributionPoint;
import sun.security.x509.DistributionPointName;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.IssuingDistributionPointExtension;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.RDN;
import sun.security.x509.ReasonFlags;
import sun.security.x509.SerialNumber;
import sun.security.x509.URIName;
import sun.security.x509.X500Name;
import sun.security.x509.X509CRLImpl;
import sun.security.x509.X509CertImpl;

public class DistributionPointFetcher {
    private static final boolean[] ALL_REASONS = new boolean[]{true, true, true, true, true, true, true, true, true};
    private static final Debug debug = Debug.getInstance("certpath");

    private DistributionPointFetcher() {
    }

    public static Collection<X509CRL> getCRLs(X509CRLSelector selector, boolean signFlag, PublicKey prevKey, String provider, List<CertStore> certStores, boolean[] reasonsMask, Set<TrustAnchor> trustAnchors, Date validity) throws CertStoreException {
        return getCRLs(selector, signFlag, prevKey, null, provider, certStores, reasonsMask, trustAnchors, validity);
    }

    public static Collection<X509CRL> getCRLs(X509CRLSelector selector, boolean signFlag, PublicKey prevKey, X509Certificate prevCert, String provider, List<CertStore> certStores, boolean[] reasonsMask, Set<TrustAnchor> trustAnchors, Date validity) throws CertStoreException {
        X509Certificate cert = selector.getCertificateChecking();
        if (cert == null) {
            return Collections.emptySet();
        }
        try {
            Debug debug;
            StringBuilder stringBuilder;
            X509CertImpl certImpl = X509CertImpl.toImpl(cert);
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DistributionPointFetcher.getCRLs: Checking CRLDPs for ");
                stringBuilder.append(certImpl.getSubjectX500Principal());
                debug.println(stringBuilder.toString());
            }
            CRLDistributionPointsExtension ext = certImpl.getCRLDistributionPointsExtension();
            if (ext == null) {
                if (debug != null) {
                    debug.println("No CRLDP ext");
                }
                return Collections.emptySet();
            }
            List<DistributionPoint> points = ext.get(CRLDistributionPointsExtension.POINTS);
            HashSet results = new HashSet();
            Iterator<DistributionPoint> t = points.iterator();
            while (true) {
                Iterator<DistributionPoint> t2 = t;
                if (!t2.hasNext()) {
                    break;
                }
                boolean[] zArr = reasonsMask;
                if (Arrays.equals(zArr, ALL_REASONS)) {
                    break;
                }
                Iterator<DistributionPoint> t3 = t2;
                results.addAll(getCRLs(selector, certImpl, (DistributionPoint) t2.next(), zArr, signFlag, prevKey, prevCert, provider, certStores, trustAnchors, validity));
                t = t3;
            }
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Returning ");
                stringBuilder.append(results.size());
                stringBuilder.append(" CRLs");
                debug.println(stringBuilder.toString());
            }
            return results;
        } catch (IOException | CertificateException e) {
            return Collections.emptySet();
        }
    }

    private static Collection<X509CRL> getCRLs(X509CRLSelector selector, X509CertImpl certImpl, DistributionPoint point, boolean[] reasonsMask, boolean signFlag, PublicKey prevKey, X509Certificate prevCert, String provider, List<CertStore> certStores, Set<TrustAnchor> trustAnchors, Date validity) throws CertStoreException {
        CertStoreException cse;
        List<CertStore> list;
        X509CRL crl;
        X509CRLSelector x509CRLSelector = selector;
        GeneralNames fullName = point.getFullName();
        if (fullName == null) {
            RDN relativeName = point.getRelativeName();
            if (relativeName == null) {
                return Collections.emptySet();
            }
            try {
                GeneralNames crlIssuers = point.getCRLIssuer();
                if (crlIssuers == null) {
                    fullName = getFullNames((X500Name) certImpl.getIssuerDN(), relativeName);
                } else if (crlIssuers.size() != 1) {
                    return Collections.emptySet();
                } else {
                    fullName = getFullNames((X500Name) crlIssuers.get(0).getName(), relativeName);
                }
            } catch (IOException e) {
                return Collections.emptySet();
            }
        }
        ArrayList<X509CRL> possibleCRLs = new ArrayList();
        Iterator<GeneralName> t = fullName.iterator();
        CertStoreException savedCSE = null;
        while (t.hasNext()) {
            try {
                GeneralName name = (GeneralName) t.next();
                if (name.getType() == 4) {
                    try {
                        possibleCRLs.addAll(getCRLs((X500Name) name.getName(), certImpl.getIssuerX500Principal(), certStores));
                    } catch (CertStoreException e2) {
                        cse = e2;
                        savedCSE = cse;
                    }
                } else {
                    list = certStores;
                    if (name.getType() == 6) {
                        crl = getCRL((URIName) name.getName());
                        if (crl != null) {
                            possibleCRLs.add(crl);
                        }
                    }
                }
            } catch (CertStoreException e3) {
                cse = e3;
                list = certStores;
                savedCSE = cse;
            }
        }
        list = certStores;
        if (!possibleCRLs.isEmpty() || savedCSE == null) {
            ArrayList crls = new ArrayList(2);
            for (X509CRL crl2 : possibleCRLs) {
                try {
                    x509CRLSelector.setIssuerNames(null);
                    if (x509CRLSelector.match(crl2) && verifyCRL(certImpl, point, crl2, reasonsMask, signFlag, prevKey, prevCert, provider, trustAnchors, certStores, validity)) {
                        crls.add(crl2);
                    }
                } catch (IOException | CRLException e4) {
                    if (debug != null) {
                        Debug debug = debug;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Exception verifying CRL: ");
                        stringBuilder.append(e4.getMessage());
                        debug.println(stringBuilder.toString());
                        e4.printStackTrace();
                    }
                }
                list = certStores;
            }
            return crls;
        }
        throw savedCSE;
    }

    private static X509CRL getCRL(URIName name) throws CertStoreException {
        Object uri = name.getURI();
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to fetch CRL from DP ");
            stringBuilder.append(uri);
            debug.println(stringBuilder.toString());
        }
        CertStore ucs = null;
        try {
            Collection<? extends CRL> crls = URICertStore.getInstance(new URICertStoreParameters(uri)).getCRLs(null);
            if (crls.isEmpty()) {
                return null;
            }
            return (X509CRL) crls.iterator().next();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            if (debug != null) {
                Debug debug2 = debug;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Can't create URICertStore: ");
                stringBuilder2.append(e.getMessage());
                debug2.println(stringBuilder2.toString());
            }
            return null;
        }
    }

    private static Collection<X509CRL> getCRLs(X500Name name, X500Principal certIssuer, List<CertStore> certStores) throws CertStoreException {
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to fetch CRL from DP ");
            stringBuilder.append((Object) name);
            debug.println(stringBuilder.toString());
        }
        X509CRLSelector xcs = new X509CRLSelector();
        xcs.addIssuer(name.asX500Principal());
        xcs.addIssuer(certIssuer);
        Collection<X509CRL> crls = new ArrayList();
        CertStoreException savedCSE = null;
        for (CertStore store : certStores) {
            try {
                for (CRL crl : store.getCRLs(xcs)) {
                    crls.add((X509CRL) crl);
                }
            } catch (CertStoreException cse) {
                if (debug != null) {
                    Debug debug2 = debug;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Exception while retrieving CRLs: ");
                    stringBuilder2.append(cse);
                    debug2.println(stringBuilder2.toString());
                    cse.printStackTrace();
                }
                savedCSE = new CertStoreTypeException(store.getType(), cse);
            }
        }
        if (!crls.isEmpty() || savedCSE == null) {
            return crls;
        }
        throw savedCSE;
    }

    /* JADX WARNING: Removed duplicated region for block: B:226:0x0420  */
    /* JADX WARNING: Removed duplicated region for block: B:222:0x0408  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static boolean verifyCRL(X509CertImpl certImpl, DistributionPoint point, X509CRL crl, boolean[] reasonsMask, boolean signFlag, PublicKey prevKey, X509Certificate prevCert, String provider, Set<TrustAnchor> trustAnchors, List<CertStore> certStores, Date validity) throws CRLException, IOException {
        Debug debug;
        boolean indirectCRL;
        GeneralNameInterface name;
        PublicKey prevKey2;
        Object idpPoint;
        Date reasons;
        X509CertImpl x509CertImpl = certImpl;
        X509CRL x509crl = crl;
        boolean[] zArr = reasonsMask;
        X509Certificate x509Certificate = prevCert;
        String str = provider;
        if (debug != null) {
            debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DistributionPointFetcher.verifyCRL: checking revocation status for\n  SN: ");
            stringBuilder.append(Debug.toHexString(certImpl.getSerialNumber()));
            stringBuilder.append("\n  Subject: ");
            stringBuilder.append(certImpl.getSubjectX500Principal());
            stringBuilder.append("\n  Issuer: ");
            stringBuilder.append(certImpl.getIssuerX500Principal());
            debug.println(stringBuilder.toString());
        }
        boolean indirectCRL2 = false;
        X509CRLImpl crlImpl = X509CRLImpl.toImpl(crl);
        IssuingDistributionPointExtension idpExt = crlImpl.getIssuingDistributionPointExtension();
        X500Name certIssuer = (X500Name) certImpl.getIssuerDN();
        Object crlIssuer = (X500Name) crlImpl.getIssuerDN();
        GeneralNames pointCrlIssuers = point.getCRLIssuer();
        X500Name pointCrlIssuer = null;
        if (pointCrlIssuers != null) {
            if (idpExt == null) {
                indirectCRL = false;
            } else if (((Boolean) idpExt.get(IssuingDistributionPointExtension.INDIRECT_CRL)).equals(Boolean.FALSE)) {
                indirectCRL = false;
            } else {
                boolean match = false;
                Iterator<GeneralName> t = pointCrlIssuers.iterator();
                while (!match && t.hasNext()) {
                    name = ((GeneralName) t.next()).getName();
                    indirectCRL = indirectCRL2;
                    if (crlIssuer.equals(name)) {
                        match = true;
                        pointCrlIssuer = (X500Name) name;
                    }
                    indirectCRL2 = indirectCRL;
                }
                indirectCRL = indirectCRL2;
                if (!match) {
                    return false;
                }
                if (issues(x509CertImpl, crlImpl, str)) {
                    indirectCRL2 = certImpl.getPublicKey();
                } else {
                    indirectCRL = true;
                    indirectCRL2 = prevKey;
                }
                prevKey2 = indirectCRL2;
            }
            return false;
        }
        indirectCRL = false;
        if (crlIssuer.equals(certIssuer)) {
            KeyIdentifier certAKID = certImpl.getAuthKeyId();
            KeyIdentifier crlAKID = crlImpl.getAuthKeyId();
            if (certAKID == null || crlAKID == null) {
                if (issues(x509CertImpl, crlImpl, str)) {
                    prevKey2 = certImpl.getPublicKey();
                }
            } else if (!certAKID.equals(crlAKID)) {
                if (issues(x509CertImpl, crlImpl, str)) {
                    prevKey2 = certImpl.getPublicKey();
                } else {
                    prevKey2 = prevKey;
                    indirectCRL = true;
                }
            }
            prevKey2 = prevKey;
        } else {
            if (debug != null) {
                debug = debug;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("crl issuer does not equal cert issuer.\ncrl issuer: ");
                stringBuilder2.append(crlIssuer);
                stringBuilder2.append("\ncert issuer: ");
                stringBuilder2.append((Object) certIssuer);
                debug.println(stringBuilder2.toString());
            }
            return false;
        }
        if (!indirectCRL && !signFlag) {
            return false;
        }
        StringBuilder stringBuilder3;
        boolean[] idpReasonFlags;
        GeneralNames generalNames;
        X500Name x500Name;
        if (idpExt != null) {
            DistributionPointName idpPoint2 = (DistributionPointName) idpExt.get(IssuingDistributionPointExtension.POINT);
            X500Name certIssuer2;
            if (idpPoint2 != null) {
                Object relativeName;
                GeneralNames idpNames = idpPoint2.getFullName();
                if (idpNames == null) {
                    relativeName = idpPoint2.getRelativeName();
                    if (relativeName == null) {
                        if (debug != null) {
                            debug.println("IDP must be relative or full DN");
                        }
                        return false;
                    }
                    if (debug != null) {
                        idpPoint2 = debug;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("IDP relativeName:");
                        stringBuilder3.append(relativeName);
                        idpPoint2.println(stringBuilder3.toString());
                    }
                    idpNames = getFullNames(crlIssuer, relativeName);
                } else {
                    GeneralNames generalNames2 = idpNames;
                }
                Iterator<GeneralName> t2;
                if (point.getFullName() == null && point.getRelativeName() == null) {
                    idpPoint2 = null;
                    t2 = pointCrlIssuers.iterator();
                    while (idpPoint2 == null && t2.hasNext()) {
                        boolean match2;
                        name = ((GeneralName) t2.next()).getName();
                        Iterator<GeneralName> i = idpNames.iterator();
                        while (true) {
                            Iterator<GeneralName> i2 = i;
                            if (idpPoint2 != null) {
                                match2 = idpPoint2;
                                break;
                            }
                            match2 = idpPoint2;
                            Iterator<GeneralName> idpPoint3 = i2;
                            if (!idpPoint3.hasNext()) {
                                break;
                            }
                            Iterator<GeneralName> i3 = idpPoint3;
                            idpPoint2 = name.equals(((GeneralName) idpPoint3.next()).getName());
                            i = i3;
                        }
                        idpPoint2 = match2;
                    }
                    if (idpPoint2 == null) {
                        return false;
                    }
                    Object obj = certIssuer;
                    generalNames = pointCrlIssuers;
                    x500Name = pointCrlIssuer;
                } else {
                    idpPoint2 = point.getFullName();
                    DistributionPointName pointNames;
                    if (idpPoint2 == null) {
                        relativeName = point.getRelativeName();
                        if (relativeName == null) {
                            if (debug != null) {
                                pointNames = idpPoint2;
                                debug.println("DP must be relative or full DN");
                            } else {
                                pointNames = idpPoint2;
                            }
                            return false;
                        }
                        pointNames = idpPoint2;
                        if (debug != null) {
                            idpPoint2 = debug;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("DP relativeName:");
                            stringBuilder3.append(relativeName);
                            idpPoint2.println(stringBuilder3.toString());
                        }
                        if (!indirectCRL) {
                            idpPoint2 = getFullNames(certIssuer, relativeName);
                        } else if (pointCrlIssuers.size() != 1) {
                            if (debug != null) {
                                debug.println("must only be one CRL issuer when relative name present");
                            }
                            return false;
                        } else {
                            idpPoint2 = getFullNames(pointCrlIssuer, relativeName);
                        }
                    } else {
                        pointNames = idpPoint2;
                    }
                    boolean match3 = false;
                    t2 = idpNames.iterator();
                    while (!match3 && t2.hasNext()) {
                        Object idpName = ((GeneralName) t2.next()).getName();
                        if (debug != null) {
                            certIssuer2 = certIssuer;
                            Debug debug2 = debug;
                            generalNames = pointCrlIssuers;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            x500Name = pointCrlIssuer;
                            stringBuilder4.append("idpName: ");
                            stringBuilder4.append(idpName);
                            debug2.println(stringBuilder4.toString());
                        } else {
                            certIssuer2 = certIssuer;
                            generalNames = pointCrlIssuers;
                            x500Name = pointCrlIssuer;
                        }
                        Iterator<GeneralName> p = idpPoint2.iterator();
                        while (!match3 && p.hasNext()) {
                            GeneralNames pointNames2;
                            Object pointName = ((GeneralName) p.next()).getName();
                            if (debug != null) {
                                Debug debug3 = debug;
                                pointNames2 = idpPoint2;
                                idpPoint2 = new StringBuilder();
                                idpPoint2.append("pointName: ");
                                idpPoint2.append(pointName);
                                debug3.println(idpPoint2.toString());
                            } else {
                                pointNames2 = idpPoint2;
                                boolean z = match3;
                            }
                            match3 = idpName.equals(pointName);
                            idpPoint = pointNames2;
                        }
                        certIssuer = certIssuer2;
                        pointCrlIssuers = generalNames;
                        pointCrlIssuer = x500Name;
                        idpPoint2 = idpPoint2;
                        match3 = match3;
                    }
                    DistributionPointName pointNames3 = idpPoint2;
                    certIssuer2 = certIssuer;
                    generalNames = pointCrlIssuers;
                    x500Name = pointCrlIssuer;
                    if (!match3) {
                        if (debug != null) {
                            debug.println("IDP name does not match DP name");
                        }
                        return false;
                    }
                }
            }
            certIssuer2 = certIssuer;
            generalNames = pointCrlIssuers;
            x500Name = pointCrlIssuer;
            if (((Boolean) idpExt.get(IssuingDistributionPointExtension.ONLY_USER_CERTS)).equals(Boolean.TRUE) && certImpl.getBasicConstraints() != -1) {
                if (debug != null) {
                    debug.println("cert must be a EE cert");
                }
                return false;
            } else if (((Boolean) idpExt.get(IssuingDistributionPointExtension.ONLY_CA_CERTS)).equals(Boolean.TRUE) && certImpl.getBasicConstraints() == -1) {
                if (debug != null) {
                    debug.println("cert must be a CA cert");
                }
                return false;
            } else if (((Boolean) idpExt.get(IssuingDistributionPointExtension.ONLY_ATTRIBUTE_CERTS)).equals(Boolean.TRUE)) {
                if (debug != null) {
                    debug.println("cert must not be an AA cert");
                }
                return false;
            }
        }
        generalNames = pointCrlIssuers;
        x500Name = pointCrlIssuer;
        boolean[] interimReasonsMask = new boolean[9];
        ReasonFlags reasons2 = null;
        if (idpExt != null) {
            reasons2 = (ReasonFlags) idpExt.get(IssuingDistributionPointExtension.REASONS);
        }
        boolean[] pointReasonFlags = point.getReasonFlags();
        if (reasons2 != null) {
            if (pointReasonFlags != null) {
                idpReasonFlags = reasons2.getFlags();
                int i4 = 0;
                while (i4 < interimReasonsMask.length) {
                    boolean z2 = i4 < idpReasonFlags.length && idpReasonFlags[i4] && i4 < pointReasonFlags.length && pointReasonFlags[i4];
                    interimReasonsMask[i4] = z2;
                    i4++;
                }
            } else {
                interimReasonsMask = (boolean[]) reasons2.getFlags().clone();
            }
        } else if (idpExt == null || reasons2 == null) {
            if (pointReasonFlags != null) {
                interimReasonsMask = (boolean[]) pointReasonFlags.clone();
            } else {
                Arrays.fill(interimReasonsMask, true);
            }
        }
        idpReasonFlags = interimReasonsMask;
        boolean oneOrMore = false;
        int i5 = 0;
        while (i5 < idpReasonFlags.length && !oneOrMore) {
            if (idpReasonFlags[i5] && (i5 >= zArr.length || !zArr[i5])) {
                oneOrMore = true;
            }
            i5++;
        }
        if (!oneOrMore) {
            return false;
        }
        if (indirectCRL) {
            Set<TrustAnchor> newTrustAnchors;
            Set<TrustAnchor> reasons3;
            PKIXBuilderParameters params;
            Set<TrustAnchor> newTrustAnchors2;
            CertPathBuilder builder;
            CertSelector certSel = new X509CertSelector();
            certSel.setSubject(crlIssuer.asX500Principal());
            certSel.setKeyUsage(new boolean[]{false, false, false, false, false, false, true});
            AuthorityKeyIdentifierExtension akidext = crlImpl.getAuthKeyIdExtension();
            if (akidext != null) {
                byte[] kid = akidext.getEncodedKeyIdentifier();
                if (kid != null) {
                    certSel.setSubjectKeyIdentifier(kid);
                }
                SerialNumber asn = (SerialNumber) akidext.get(AuthorityKeyIdentifierExtension.SERIAL_NUMBER);
                if (asn != null) {
                    certSel.setSerialNumber(asn.getNumber());
                    newTrustAnchors = new HashSet((Collection) trustAnchors);
                    if (prevKey2 == null) {
                        TrustAnchor temporary;
                        if (x509Certificate != null) {
                            temporary = new TrustAnchor(x509Certificate, null);
                        } else {
                            temporary = new TrustAnchor(certImpl.getIssuerX500Principal(), prevKey2, null);
                        }
                        reasons3 = newTrustAnchors;
                        reasons3.add(temporary);
                    } else {
                        reasons3 = newTrustAnchors;
                    }
                    params = null;
                    params = new PKIXBuilderParameters((Set) reasons3, certSel);
                    newTrustAnchors2 = reasons3;
                    params.setCertStores(certStores);
                    params.setSigProvider(str);
                    params.setDate(validity);
                    builder = CertPathBuilder.getInstance("PKIX");
                    prevKey2 = ((PKIXCertPathBuilderResult) builder.build(params)).getPublicKey();
                }
            }
            newTrustAnchors = new HashSet((Collection) trustAnchors);
            if (prevKey2 == null) {
            }
            params = null;
            try {
                params = new PKIXBuilderParameters((Set) reasons3, certSel);
                newTrustAnchors2 = reasons3;
                params.setCertStores(certStores);
                params.setSigProvider(str);
                params.setDate(validity);
                try {
                    builder = CertPathBuilder.getInstance("PKIX");
                    prevKey2 = ((PKIXCertPathBuilderResult) builder.build(params)).getPublicKey();
                } catch (GeneralSecurityException e) {
                    throw new CRLException(e);
                }
            } catch (InvalidAlgorithmParameterException e2) {
                newTrustAnchors2 = reasons3;
                reasons = validity;
                throw new CRLException(e2);
            }
        }
        reasons = validity;
        X509CRL x509crl2 = crl;
        Debug debug4;
        try {
            AlgorithmChecker.check(prevKey2, x509crl2);
            try {
                x509crl2.verify(prevKey2, str);
                Object<String> unresCritExts = crl.getCriticalExtensionOIDs();
                if (unresCritExts != null) {
                    unresCritExts.remove(PKIXExtensions.IssuingDistributionPoint_Id.toString());
                    if (!unresCritExts.isEmpty()) {
                        if (debug != null) {
                            debug4 = debug;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Unrecognized critical extension(s) in CRL: ");
                            stringBuilder3.append((Object) unresCritExts);
                            debug4.println(stringBuilder3.toString());
                            for (String ext : unresCritExts) {
                                debug.println(ext);
                            }
                        }
                        return false;
                    }
                }
                int i6 = 0;
                while (i6 < zArr.length) {
                    boolean z3 = zArr[i6] || (i6 < idpReasonFlags.length && idpReasonFlags[i6]);
                    zArr[i6] = z3;
                    i6++;
                }
                return true;
            } catch (GeneralSecurityException e3) {
                GeneralSecurityException generalSecurityException = e3;
                if (debug != null) {
                    debug.println("CRL signature failed to verify");
                }
                return false;
            }
        } catch (CertPathValidatorException idpPoint4) {
            CertPathValidatorException certPathValidatorException = idpPoint4;
            if (debug != null) {
                debug4 = debug;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("CRL signature algorithm check failed: ");
                stringBuilder3.append(idpPoint4);
                debug4.println(stringBuilder3.toString());
            }
            return false;
        }
    }

    private static GeneralNames getFullNames(X500Name issuer, RDN rdn) throws IOException {
        List<RDN> rdns = new ArrayList(issuer.rdns());
        rdns.add(rdn);
        GeneralNameInterface fullName = new X500Name((RDN[]) rdns.toArray(new RDN[0]));
        GeneralNames fullNames = new GeneralNames();
        fullNames.add(new GeneralName(fullName));
        return fullNames;
    }

    private static boolean issues(X509CertImpl cert, X509CRLImpl crl, String provider) throws IOException {
        AdaptableX509CertSelector issuerSelector = new AdaptableX509CertSelector();
        boolean[] usages = cert.getKeyUsage();
        if (usages != null) {
            usages[6] = true;
            issuerSelector.setKeyUsage(usages);
        }
        issuerSelector.setSubject(crl.getIssuerX500Principal());
        AuthorityKeyIdentifierExtension crlAKID = crl.getAuthKeyIdExtension();
        issuerSelector.setSkiAndSerialNumber(crlAKID);
        boolean matched = issuerSelector.match(cert);
        if (!matched) {
            return matched;
        }
        if (crlAKID != null && cert.getAuthorityKeyIdentifierExtension() != null) {
            return matched;
        }
        try {
            crl.verify(cert.getPublicKey(), provider);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
